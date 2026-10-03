package spl.lae;

import java.io.IOException;

import parser.*;

public class Main {

    /**
     * Usage: <numThreads> <inputPath> <outputPath>
     * Reads a computation tree from the input JSON, evaluates it, and writes either
     * {"result": [[...]]} or {"error": "..."} to the output JSON.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: <numThreads> <inputPath> <outputPath>");
            return;
        }
        String inputPath = args[1];
        String outputPath = args[2];

        try {
            // Number of TiredThread workers
            int numThreads = Integer.parseInt(args[0]);

            InputParser parser = new InputParser();
            ComputationNode root = parser.parse(inputPath);

            LinearAlgebraEngine lae = new LinearAlgebraEngine(numThreads);
            ComputationNode resultNode = lae.run(root);
            System.err.println(lae.getWorkerReport());

            OutputWriter.write(resultNode.getMatrix(), outputPath);
        } catch (Exception exception) {
            // Invalid input (bad JSON, unknown operator, dimension mismatch, ...) is reported in the output file
            String message = exception.getMessage() != null ? exception.getMessage() : exception.toString();
            try {
                OutputWriter.write(message, outputPath);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}
