package memory;

/**
 * A matrix stored as an array of {@link SharedVector}s.
 * When the orientation is ROW_MAJOR each stored vector is a row;
 * when it is COLUMN_MAJOR each stored vector is a column.
 */
public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    /** Creates an empty matrix (no vectors). */
    public SharedMatrix() {
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        loadRowMajor(matrix);
    }

    /**
     * Replaces the contents with the given matrix, stored row by row.
     * Rows are copied, so later in-place operations do not modify the caller's array.
     */
    public void loadRowMajor(double[][] matrix) {
        SharedVector[] newVectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            newVectors[i] = new SharedVector(matrix[i].clone(), VectorOrientation.ROW_MAJOR);
        }
        this.vectors = newVectors;
    }

    /** Replaces the contents with the given matrix, stored column by column. */
    public void loadColumnMajor(double[][] matrix) {
        int rows = matrix.length;
        int cols = rows == 0 ? 0 : matrix[0].length;
        SharedVector[] newVectors = new SharedVector[cols];
        for (int j = 0; j < cols; j++) {
            double[] columnData = new double[rows];
            for (int i = 0; i < rows; i++) {
                columnData[i] = matrix[i][j];
            }
            newVectors[j] = new SharedVector(columnData, VectorOrientation.COLUMN_MAJOR);
        }
        this.vectors = newVectors;
    }

    /**
     * Returns the contents as a row-major double[][].
     * If the vectors are column-oriented they are read as columns, so a matrix whose
     * rows were all transposed (see {@link SharedVector#transpose()}) reads back as
     * the transpose of the original.
     */
    public double[][] readRowMajor() {
        SharedVector[] vecs = vectors;
        if (vecs.length == 0) {
            return new double[0][0];
        }
        acquireAllVectorReadLocks(vecs);
        try {
            int vecNum = vecs.length;
            int vecLen = vecs[0].length();
            if (vecs[0].getOrientation() == VectorOrientation.ROW_MAJOR) {
                double[][] result = new double[vecNum][vecLen];
                for (int i = 0; i < vecNum; i++) {
                    for (int j = 0; j < vecLen; j++) {
                        result[i][j] = vecs[i].get(j);
                    }
                }
                return result;
            } else {
                double[][] result = new double[vecLen][vecNum];
                for (int j = 0; j < vecNum; j++) {
                    for (int i = 0; i < vecLen; i++) {
                        result[i][j] = vecs[j].get(i);
                    }
                }
                return result;
            }
        } finally {
            releaseAllVectorReadLocks(vecs);
        }
    }

    public SharedVector get(int index) {
        return vectors[index];
    }

    /** Returns the number of stored vectors (rows if ROW_MAJOR, columns if COLUMN_MAJOR). */
    public int length() {
        return vectors.length;
    }

    /** Returns the orientation of the stored vectors, or null if the matrix is empty. */
    public VectorOrientation getOrientation() {
        SharedVector[] vecs = vectors;
        if (vecs.length == 0) {
            return null;
        }
        return vecs[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        for (SharedVector v : vecs) {
            v.readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        for (SharedVector v : vecs) {
            v.readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        for (SharedVector v : vecs) {
            v.writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        for (SharedVector v : vecs) {
            v.writeUnlock();
        }
    }
}
