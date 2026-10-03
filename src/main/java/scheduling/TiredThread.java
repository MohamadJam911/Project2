package scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * A worker thread that becomes more "tired" the more it works.
 * Fatigue = fatigueFactor * total time spent executing tasks (ns).
 * Workers are ordered by fatigue so the executor can always pick the least tired one.
 */
public class TiredThread extends Thread implements Comparable<TiredThread> {

    private static final Runnable POISON_PILL = () -> {}; // Special task to signal shutdown

    private final int id; // Worker index assigned by the executor
    private final double fatigueFactor; // Multiplier for fatigue calculation

    private final AtomicBoolean alive = new AtomicBoolean(true); // Indicates if the worker should keep running

    // Single-slot handoff queue; executor will put tasks here
    private final BlockingQueue<Runnable> handoff = new ArrayBlockingQueue<>(1);

    private final AtomicBoolean busy = new AtomicBoolean(false); // Indicates if the worker is currently executing a task

    private final AtomicLong timeUsed = new AtomicLong(0); // Total time spent executing tasks
    private final AtomicLong timeIdle = new AtomicLong(0); // Total time spent idle
    private final AtomicLong idleStartTime = new AtomicLong(0); // Timestamp when the worker became idle

    // Called after each task, once the statistics are updated and the worker is idle again
    private volatile Consumer<TiredThread> onTaskDone = worker -> {};

    public TiredThread(int id, double fatigueFactor) {
        this.id = id;
        this.fatigueFactor = fatigueFactor;
        this.idleStartTime.set(System.nanoTime());
        setName(String.format("FF=%.2f", fatigueFactor));
    }

    public int getWorkerId() {
        return id;
    }

    public double getFatigue() {
        return fatigueFactor * timeUsed.get();
    }

    public boolean isBusy() {
        return busy.get();
    }

    public long getTimeUsed() {
        return timeUsed.get();
    }

    public long getTimeIdle() {
        return timeIdle.get();
    }

    /**
     * Registers a callback invoked on this worker's thread after every task,
     * once timeUsed/fatigue are updated and the worker is no longer busy.
     */
    public void setOnTaskDone(Consumer<TiredThread> listener) {
        this.onTaskDone = listener;
    }

    /**
     * Assign a task to this worker.
     * This method is non-blocking: if the worker is not ready to accept a task,
     * it throws IllegalStateException.
     */
    public void newTask(Runnable task) {
        if (!alive.get()) {
            throw new IllegalStateException("Worker not alive");
        }
        // Atomically reserve the worker
        if (!busy.compareAndSet(false, true)) {
            throw new IllegalStateException("Worker already busy");
        }
        if (!handoff.offer(task)) {
            // Undo the reservation if the handoff slot is somehow occupied
            busy.set(false);
            throw new IllegalStateException("Handoff queue rejected task");
        }
    }

    /**
     * Request this worker to stop after finishing current task.
     * Inserts a poison pill so the worker wakes up and exits.
     */
    public void shutdown() {
        alive.set(false);
        handoff.offer(POISON_PILL);
        // Wake the thread in case it is blocked on take()
        this.interrupt();
    }

    @Override
    public void run() {
        try {
            while (alive.get()) {
                Runnable task;
                try {
                    task = handoff.take();
                } catch (InterruptedException e) {
                    // Interrupted by shutdown(): exit; otherwise keep waiting for work
                    if (!alive.get()) {
                        break;
                    }
                    continue;
                }

                if (task == POISON_PILL) {
                    alive.set(false);
                } else {
                    timeIdle.addAndGet(System.nanoTime() - idleStartTime.get());

                    long start = System.nanoTime();
                    try {
                        task.run();
                    } catch (Throwable t) {
                        // Report but keep the worker alive
                        t.printStackTrace();
                    } finally {
                        timeUsed.addAndGet(System.nanoTime() - start);
                        busy.set(false);
                        idleStartTime.set(System.nanoTime());
                    }
                    onTaskDone.accept(this);
                }
            }
        } finally {
            alive.set(false);
            busy.set(false);
        }
    }

    /** Orders workers by fatigue (lowest first), breaking ties by id. */
    @Override
    public int compareTo(TiredThread o) {
        int cmp = Double.compare(this.getFatigue(), o.getFatigue());
        if (cmp != 0) {
            return cmp;
        }
        return Integer.compare(this.id, o.id);
    }
}
