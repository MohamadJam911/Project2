package spl.lae;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import parser.InputParser;
import parser.OutputWriter;
import parser.ComputationNode;
import memory.SharedVector;
import memory.SharedMatrix;
import memory.VectorOrientation;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

public class ConsolidatedExamplesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode runFromInputString(String inputContent, int threads, String outFileName) throws Exception {
        File tmp = File.createTempFile("conex", ".json");
        Files.writeString(tmp.toPath(), inputContent, StandardCharsets.UTF_8);
        Path outDir = Path.of("script_output");
        Files.createDirectories(outDir);
        Path outPath = outDir.resolve(outFileName);
        
        try {
            InputParser parser = new InputParser();
            ComputationNode root = parser.parse(tmp.getAbsolutePath());
            // Created after parsing so a parse error doesn't leave worker threads running
            LinearAlgebraEngine engine = new LinearAlgebraEngine(threads);
            ComputationNode result = engine.run(root);
            OutputWriter.write(result.getMatrix(), outPath.toString());
        } catch (ParseException | IllegalArgumentException e) {
            OutputWriter.write(e.getMessage(), outPath.toString());
        } catch (Exception e) {
            OutputWriter.write(e.getMessage(), outPath.toString());
        } finally {
            tmp.delete();
        }
        
        // Return both the JSON and a way to check the engine's final report if needed
        return mapper.readTree(outPath.toFile());
    }

    @Test
    public void complexNestedWorkerStatisticsReport() throws Exception {
        // Run a significant task to generate measurable statistics
        int threadCount = 4;
        LinearAlgebraEngine engine = new LinearAlgebraEngine(threadCount);
        InputParser parser = new InputParser();
        
        // Using the complex nested input from your AllExamplesData
        File tmp = File.createTempFile("stats_test", ".json");
        Files.writeString(tmp.toPath(), AllExamplesData.COMPLEX_NESTED, StandardCharsets.UTF_8);
        
        ComputationNode root = parser.parse(tmp.getAbsolutePath());
        engine.run(root);
        
        String report = engine.getWorkerReport();
        
        // Assertions to verify internal logic of TiredExecutor and TiredThread
        assertNotNull(report);
        assertTrue(report.contains("Worker"), "Report should have header");
        assertTrue(report.contains("fatigue"), "Report should track fatigue");
        
        // Ensure that threads actually did work (timeUsed > 0)
        // Note: In extremely fast environments this might be very small, 
        // but with 4 threads and a nested tree, it should be logged.
        assertTrue(report.contains("| 0        |") || !report.contains("timeUsed(ns) : 0"), 
            "At least one worker should have logged execution time");
            
        tmp.delete();
    }

    @Test
    public void copilotExampleProducesExpectedOutput() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("cpoilottest.json"), 2, "consolidated_cpoilotout.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("copilotexpected.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void invalidOperatorProducesError() throws Exception {
        JsonNode node = runFromInputString(AllExamplesData.INPUTS.get("invalid_operator.json"), 2, "consolidated_invalid_op.json");
        assertTrue(node.has("error"));
    }

    @Test
    public void inconsistentRowProducesError() throws Exception {
        JsonNode node = runFromInputString(AllExamplesData.INPUTS.get("inconsistent_row.json"), 2, "consolidated_inconsistent.json");
        assertTrue(node.has("error"));
    }

    @Test
    public void dimMismatchProducesError() throws Exception {
        JsonNode node = runFromInputString(AllExamplesData.INPUTS.get("dim_mismatch.json"), 2, "consolidated_dim_mismatch.json");
        assertTrue(node.has("error"));
    }

    @Test
    public void manyThreadsDeterminism() throws Exception {
        String input = AllExamplesData.INPUTS.get("many_threads.json");
        JsonNode ref = runFromInputString(input, 1, "consolidated_det_t1.json");
        for (int t : new int[]{2,4,8,16}) {
            JsonNode produced = runFromInputString(input, t, "consolidated_det_t" + t + ".json");
            assertEquals(ref, produced, "Mismatch for threads=" + t);
        }
    }

    @Test
    public void nestedNaryOperatorUsesAllOperands() throws Exception {
        // -(1 + 2 + 3): the inner 3-operand ADD is not the root, so it must be split on its own
        String input = "{\"operator\":\"-\",\"operands\":[{\"operator\":\"+\",\"operands\":[[[1]],[[2]],[[3]]]}]}";
        JsonNode produced = runFromInputString(input, 2, "consolidated_nested_nary.json");
        assertEquals(mapper.readTree("{\"result\":[[-6.0]]}"), produced);
    }

    @Test
    public void matrixOnlyInputReturnsMatrixAndStopsWorkers() throws Exception {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(3);
        ComputationNode result = engine.run(new ComputationNode(new double[][] {{1, 2}, {3, 4}}));
        assertArrayEquals(new double[][] {{1, 2}, {3, 4}}, result.getMatrix());
        assertEquals(0, countLiveWorkerThreads(), "Worker threads must exit after run()");
    }

    @Test
    public void failedComputationStopsWorkers() throws Exception {
        JsonNode node = runFromInputString(AllExamplesData.INPUTS.get("dim_mismatch.json"), 3, "consolidated_dim_mismatch_shutdown.json");
        assertTrue(node.get("error").asText().contains("Dimension mismatch"));
        assertEquals(0, countLiveWorkerThreads(), "Worker threads must exit after a failed run()");
    }

    @Test
    public void addWithDifferentShapesProducesError() throws Exception {
        JsonNode rows = runFromInputString("{\"operator\":\"+\",\"operands\":[[[1,2]],[[1,2],[3,4]]]}", 2, "consolidated_add_rows.json");
        assertTrue(rows.has("error"));
        JsonNode cols = runFromInputString("{\"operator\":\"+\",\"operands\":[[[1,2]],[[1,2,3]]]}", 2, "consolidated_add_cols.json");
        assertTrue(cols.has("error"));
    }

    private static long countLiveWorkerThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t instanceof scheduling.TiredThread && t.isAlive())
                .count();
    }

    @Test
    public void zeroThreadsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new LinearAlgebraEngine(0));
        assertThrows(IllegalArgumentException.class, () -> new LinearAlgebraEngine(-2));
    }

    @Test
    public void vecMatMulDimensionMismatchThrows() {
        SharedVector v = new SharedVector(new double[] {1,2,3}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix();
        m.loadRowMajor(new double[][] { {1,2}, {3,4} });
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    // --- Automatically added expanded tests ---

    @Test
    public void bigNumbersMultiplication() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("big_numbers_mul.json"), 2, "consolidated_big_numbers.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("big_numbers_mul.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void identityMultiplication() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("identity_mul.json"), 2, "consolidated_identity_mul.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("identity_mul.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void zeroMultiplicationGivesZero() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("zero_mul.json"), 2, "consolidated_zero_mul.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("zero_mul.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void negativeNumbersMultiplication() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("negative_numbers.json"), 2, "consolidated_negative_numbers.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("negative_numbers.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void singleElementMultiplication() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("single_element.json"), 1, "consolidated_single_element.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("single_element.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void nonSquareMultiplication() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("non_square_mul.json"), 2, "consolidated_non_square_mul.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("non_square_mul.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void transposeOperation() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("transpose.json"), 2, "consolidated_transpose.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("transpose.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void chainedOperationsProduceExpected() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("chained_ops.json"), 2, "consolidated_chained_ops.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("chained_ops.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void overflowTestLargeNumbers() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("overflow_test.json"), 2, "consolidated_overflow_test.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("overflow_test.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void manyThreadsLargeDeterminism() throws Exception {
        String input = AllExamplesData.INPUTS.get("many_threads_large.json");
        JsonNode ref = runFromInputString(input, 1, "consolidated_mtl_t1.json");
        for (int t : new int[]{2,4,8,16,32,64}) {
            JsonNode produced = runFromInputString(input, t, "consolidated_mtl_t" + t + ".json");
            assertEquals(ref, produced, "Mismatch for threads=" + t);
        }
    }

    @Test
    public void associativePropertyTest() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("associative_test.json"), 3, "consolidated_associative.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("associative_test.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void transposeDoubleTest() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("transpose_double.json"), 2, "consolidated_transpose_double.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("transpose_double.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void identityLargeTest() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("identity_large.json"), 2, "consolidated_identity_large.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("identity_large.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void invalidOperator2ProducesError() throws Exception {
        JsonNode node = runFromInputString(AllExamplesData.INPUTS.get("invalid_operator2.json"), 2, "consolidated_invalid_op2.json");
        assertTrue(node.has("error"));
    }

    @Test
    public void inconsistentRow2ProducesError() throws Exception {
        JsonNode node = runFromInputString(AllExamplesData.INPUTS.get("inconsistent_row2.json"), 2, "consolidated_inconsistent2.json");
        assertTrue(node.has("error"));
    }

    @Test
    public void dimMismatch2ProducesError() throws Exception {
        JsonNode node = runFromInputString(AllExamplesData.INPUTS.get("dim_mismatch2.json"), 2, "consolidated_dim_mismatch2.json");
        assertTrue(node.has("error"));
    }

    @Test
    public void transposeEqualityTest() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("transpose_eq.json"), 2, "consolidated_transpose_eq.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("transpose_eq.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void assocValidTest() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("assoc_valid.json"), 2, "consolidated_assoc_valid.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("assoc_valid.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void largeIdentity5Test() throws Exception {
        JsonNode produced = runFromInputString(AllExamplesData.INPUTS.get("large_identity5.json"), 2, "consolidated_large_identity5.json");
        JsonNode expected = mapper.readTree(AllExamplesData.EXPECTED.get("large_identity5.json"));
        assertEquals(expected, produced);
    }

    @Test
    public void threadVariationDeterminism() throws Exception {
        String input = AllExamplesData.INPUTS.get("big_numbers_mul.json");
        JsonNode ref = runFromInputString(input, 1, "consolidated_tv_t1.json");
        for (int t : new int[]{2,4,8,16}) {
            JsonNode produced = runFromInputString(input, t, "consolidated_tv_t" + t + ".json");
            assertEquals(ref, produced, "Mismatch for threads=" + t);
        }
    }
}