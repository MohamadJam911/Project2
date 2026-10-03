package spl.lae;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import parser.InputParser;
import parser.ComputationNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Utility that collects every src/Examples/example*.json input into three files:
 * merged_inputs.json (the inputs), merged_expected.json (results computed with 1 thread)
 * and merged_outputs.json (results computed with the thread count given as the first
 * argument, default 1). Run it from the project root.
 */
public class MergeExamples {
    public static void main(String[] args) throws Exception {
        Path examplesDir = Path.of("src/Examples");
        if (!Files.isDirectory(examplesDir)) {
            System.err.println("Examples directory not found: " + examplesDir.toAbsolutePath());
            System.exit(1);
        }

        // Optional thread count argument for producing the 'outputs' file
        int threadCount = 1;
        if (args != null && args.length > 0) {
            try {
                threadCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid thread count provided, defaulting to 1");
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode inputs = mapper.createArrayNode();
        ObjectNode expected = mapper.createObjectNode();
        ObjectNode outputs = mapper.createObjectNode();

        final int runThreads = threadCount;

        Files.list(examplesDir)
                .filter(p -> p.getFileName().toString().matches("example.*\\.json"))
                .sorted(Comparator.comparing(Path::toString))
                .forEach(p -> {
                    try {
                        String name = p.getFileName().toString().replaceFirst("\\.json$", "");
                        JsonNode content = mapper.readTree(p.toFile());
                        ObjectNode entry = mapper.createObjectNode();
                        entry.put("name", name);
                        entry.set("input", content);
                        inputs.add(entry);

                        // Compute expected output or error by running the engine single-threaded
                        try {
                            InputParser parser = new InputParser();
                            ComputationNode root = parser.parse(p.toString());
                            LinearAlgebraEngine engine = new LinearAlgebraEngine(1);
                            ComputationNode result = engine.run(root);
                            ObjectNode outNode = mapper.createObjectNode();
                            outNode.set("result", mapper.valueToTree(result.getMatrix()));
                            expected.set(name, outNode);
                        } catch (Exception ex) {
                            ObjectNode err = mapper.createObjectNode();
                            err.put("error", ex.getMessage());
                            expected.set(name, err);
                        }

                        // Compute actual output using the requested thread count
                        try {
                            InputParser parser2 = new InputParser();
                            ComputationNode root2 = parser2.parse(p.toString());
                            LinearAlgebraEngine engine2 = new LinearAlgebraEngine(runThreads);
                            ComputationNode result2 = engine2.run(root2);
                            ObjectNode outNode2 = mapper.createObjectNode();
                            outNode2.set("result", mapper.valueToTree(result2.getMatrix()));
                            outputs.set(name, outNode2);
                        } catch (Exception ex) {
                            ObjectNode err = mapper.createObjectNode();
                            err.put("error", ex.getMessage());
                            outputs.set(name, err);
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        // Write merged files into the examples directory so they can be tracked
        Path mergedInputs = examplesDir.resolve("merged_inputs.json");
        Path mergedExpected = examplesDir.resolve("merged_expected.json");
        Path mergedOutputs = examplesDir.resolve("merged_outputs.json");

        Files.writeString(mergedInputs, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputs));
        Files.writeString(mergedExpected, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(expected));
        Files.writeString(mergedOutputs, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(outputs));

        System.out.println("Wrote: " + mergedInputs.toAbsolutePath());
        System.out.println("Wrote: " + mergedExpected.toAbsolutePath());
        System.out.println("Wrote: " + mergedOutputs.toAbsolutePath());
    }
}
