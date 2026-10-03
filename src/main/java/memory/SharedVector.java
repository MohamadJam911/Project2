package memory;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A vector of doubles guarded by a read-write lock.
 * <p>
 * {@link #get(int)} locks internally. The mutating and arithmetic operations
 * ({@link #add}, {@link #negate}, {@link #dot}, {@link #vecMatMul}, {@link #transpose})
 * do NOT lock this vector themselves: the caller must hold this vector's write lock
 * (or read lock, for {@link #dot}). Elements of the other operand are read through
 * {@link #get(int)}, so the other vector is read-locked per element.
 */
public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        readLock();
        try {
            return vector[index];
        } finally {
            readUnlock();
        }
    }

    public int length() {
        return vector.length;
    }

    public VectorOrientation getOrientation() {
        return orientation;
    }

    public void writeLock() {
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    public void readLock() {
        lock.readLock().lock();
    }

    public void readUnlock() {
        lock.readLock().unlock();
    }

    /**
     * Flips the orientation (row <-> column). The data itself is unchanged;
     * a row vector of length n simply becomes a column vector of length n.
     */
    public void transpose() {
        if (orientation == VectorOrientation.ROW_MAJOR) {
            orientation = VectorOrientation.COLUMN_MAJOR;
        } else {
            orientation = VectorOrientation.ROW_MAJOR;
        }
    }

    /** Element-wise addition: this = this + other. */
    public void add(SharedVector other) {
        if (this.length() != other.length()) {
            throw new IllegalArgumentException("Vector length mismatch: " + this.length() + " vs " + other.length());
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] + other.get(i);
        }
    }

    /** Element-wise negation: this = -this. */
    public void negate() {
        for (int i = 0; i < vector.length; i++) {
            vector[i] = -vector[i];
        }
    }

    /** Returns the dot product of this vector and other. */
    public double dot(SharedVector other) {
        if (this.length() != other.length()) {
            throw new IllegalArgumentException("Vector length mismatch: " + this.length() + " vs " + other.length());
        }
        double sum = 0.0;
        for (int i = 0; i < vector.length; i++) {
            sum += vector[i] * other.get(i);
        }
        return sum;
    }

    /**
     * Replaces this row vector with (this × matrix).
     * The matrix is expected to be stored column-major, so each stored vector is
     * one column and result[i] = this · column(i). The result is a row vector
     * whose length is the number of columns in the matrix.
     */
    public void vecMatMul(SharedMatrix matrix) {
        double[] result = new double[matrix.length()];
        for (int i = 0; i < matrix.length(); i++) {
            SharedVector column = matrix.get(i);
            if (column.length() != this.length()) {
                throw new IllegalArgumentException("Vector length " + this.length()
                        + " does not match matrix column " + i + " length " + column.length());
            }
            result[i] = this.dot(column);
        }
        vector = result;
        orientation = VectorOrientation.ROW_MAJOR;
    }
}
