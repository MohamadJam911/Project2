package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates a computation tree bottom-up. Each step picks a node whose operands
 * are all matrices, loads the operands into leftMatrix/rightMatrix, runs one task
 * per row on the executor, and replaces the node with the resulting matrix.
 */
public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        executor = new TiredExecutor(numThreads);
    }

    /**
     * Resolves the whole tree and returns the root, which is then a MATRIX node.
     * The executor is shut down when this returns or throws, so an engine
     * instance can run only one computation.
     */
    public ComputationNode run(ComputationNode computationRoot) {
        try {
            if (computationRoot == null) {
                return null;
            }
            ComputationNode resolvable;
            while ((resolvable = computationRoot.findResolvable()) != null) {
                if (resolvable.getChildren().size() > 2) {
                    // Split n-ary ADD/MULTIPLY into left-nested binary nodes, then search again
                    resolvable.associativeNesting();
                    continue;
                }
                loadAndCompute(resolvable);
            }
            return computationRoot;
        } finally {
            try {
                executor.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Computes a single node whose operands are all matrices and resolves it.
     * The result is always produced in leftMatrix.
     *
     * @throws IllegalArgumentException if the operand dimensions are incompatible
     */
    public void loadAndCompute(ComputationNode node) {
        List<ComputationNode> operands = node.getChildren();
        double[][] left = operands.get(0).getMatrix();
        List<Runnable> tasks;

        switch (node.getNodeType()) {
            case ADD -> {
                double[][] right = operands.get(1).getMatrix();
                if (left.length != right.length || cols(left) != cols(right)) {
                    throw new IllegalArgumentException("Dimension mismatch for addition: "
                            + dims(left) + " + " + dims(right));
                }
                leftMatrix.loadRowMajor(left);
                rightMatrix.loadRowMajor(right);
                tasks = createAddTasks();
            }
            case MULTIPLY -> {
                double[][] right = operands.get(1).getMatrix();
                if (cols(left) != right.length) {
                    throw new IllegalArgumentException("Dimension mismatch for multiplication: "
                            + dims(left) + " * " + dims(right));
                }
                leftMatrix.loadRowMajor(left);
                // Column-major so each stored vector of the right operand is a column
                rightMatrix.loadColumnMajor(right);
                tasks = createMultiplyTasks();
            }
            case NEGATE -> {
                leftMatrix.loadRowMajor(left);
                tasks = createNegateTasks();
            }
            case TRANSPOSE -> {
                leftMatrix.loadRowMajor(left);
                tasks = createTransposeTasks();
            }
            default -> throw new IllegalStateException("Node is not an operation: " + node.getNodeType());
        }

        // Blocks until every task has finished
        executor.submitAll(tasks);

        node.resolve(leftMatrix.readRowMajor());
    }

    private static int cols(double[][] m) {
        return m.length == 0 ? 0 : m[0].length;
    }

    private static String dims(double[][] m) {
        return m.length + "x" + cols(m);
    }

    /** One task per row: leftRow += rightRow. */
    public List<Runnable> createAddTasks() {
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int rowIndex = i;
            tasks.add(() -> {
                SharedVector v1 = leftMatrix.get(rowIndex);
                SharedVector v2 = rightMatrix.get(rowIndex);
                // v1 is modified, v2 is only read
                v1.writeLock();
                v2.readLock();
                try {
                    v1.add(v2);
                } finally {
                    v2.readUnlock();
                    v1.writeUnlock();
                }
            });
        }
        return tasks;
    }

    /** One task per row: leftRow = leftRow × rightMatrix. */
    public List<Runnable> createMultiplyTasks() {
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int rowIndex = i;
            tasks.add(() -> {
                SharedVector rowV = leftMatrix.get(rowIndex);
                rowV.writeLock();
                try {
                    // Replaces the row in place with its dot products against each column
                    rowV.vecMatMul(rightMatrix);
                } finally {
                    rowV.writeUnlock();
                }
            });
        }
        return tasks;
    }

    /** One task per row: leftRow = -leftRow. */
    public List<Runnable> createNegateTasks() {
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int rowIndex = i;
            tasks.add(() -> {
                SharedVector row = leftMatrix.get(rowIndex);
                row.writeLock();
                try {
                    row.negate();
                } finally {
                    row.writeUnlock();
                }
            });
        }
        return tasks;
    }

    /**
     * One task per row: flips each row vector to column orientation.
     * readRowMajor() then reads the vectors as columns, which yields the transpose.
     */
    public List<Runnable> createTransposeTasks() {
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int rowIndex = i;
            tasks.add(() -> {
                SharedVector v = leftMatrix.get(rowIndex);
                v.writeLock();
                try {
                    v.transpose();
                } finally {
                    v.writeUnlock();
                }
            });
        }
        return tasks;
    }

    public String getWorkerReport() {
        return executor.getWorkerReport();
    }
}
