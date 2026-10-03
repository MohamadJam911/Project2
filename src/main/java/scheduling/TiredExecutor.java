package scheduling;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fixed pool of {@link TiredThread} workers. Each task is handed to the
 * least fatigued idle worker, taken from a min-heap ordered by fatigue.
 */
public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);
    // Exceptions thrown by tasks, reported by submitAll
    private final ConcurrentLinkedQueue<Throwable> taskExceptions = new ConcurrentLinkedQueue<>();

    public TiredExecutor(int numThreads) {
        if (numThreads < 1) {
            throw new IllegalArgumentException("numThreads must be >= 1");
        }
        workers = new TiredThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            // Random fatigue factor in [0.5, 1.5)
            double fatigueFactor = 0.5 + Math.random();
            TiredThread worker = new TiredThread(i, fatigueFactor);
            worker.setOnTaskDone(this::onTaskDone);
            workers[i] = worker;
            idleMinHeap.add(worker);
            worker.start();
        }
    }

    /**
     * Runs on the worker's thread after its statistics are updated, so the worker
     * re-enters the heap with its current fatigue.
     */
    private void onTaskDone(TiredThread worker) {
        idleMinHeap.add(worker);
        if (inFlight.decrementAndGet() == 0) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    /**
     * Hands a task to the least fatigued idle worker, blocking until one is available.
     * Does not wait for the task to finish.
     */
    public void submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        Runnable wrapped = () -> {
            try {
                task.run();
            } catch (Throwable t) {
                taskExceptions.add(t);
            }
        };

        try {
            while (true) {
                TiredThread worker = idleMinHeap.take();
                // Count the task before handing it off so a fast worker cannot finish it first
                inFlight.incrementAndGet();
                try {
                    worker.newTask(wrapped);
                    return;
                } catch (IllegalStateException e) {
                    // Worker could not accept the task; undo, put it back if alive, try another
                    inFlight.decrementAndGet();
                    if (worker.isAlive()) {
                        idleMinHeap.add(worker);
                    }
                    Thread.yield();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Submits all tasks and blocks until every in-flight task has finished.
     * If any task threw, the first exception is rethrown (RuntimeExceptions as-is,
     * anything else wrapped in a RuntimeException).
     */
    public void submitAll(Iterable<Runnable> tasks) {
        if (tasks == null) {
            throw new NullPointerException("tasks");
        }
        for (Runnable task : tasks) {
            submit(task);
        }
        synchronized (this) {
            while (inFlight.get() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        Throwable first = taskExceptions.peek();
        if (first != null) {
            taskExceptions.clear();
            if (first instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException("Task failed: " + first.getMessage(), first);
        }
    }

    /** Stops all workers and waits for their threads to terminate. */
    public void shutdown() throws InterruptedException {
        for (TiredThread worker : workers) {
            worker.shutdown();
        }
        for (TiredThread worker : workers) {
            worker.join();
        }
    }

    public synchronized String getWorkerReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== TiredExecutor Worker Report ===\n");
        report.append(String.format("%-8s | %-12s | %-12s | %-14s | %-6s\n",
                "Worker", "timeUsed(ns)", "timeIdle(ns)", "fatigue", "busy"));
        report.append("---------------------------------------------------------------\n");

        long totalUsed = 0;
        long totalIdle = 0;
        double minFat = Double.POSITIVE_INFINITY;
        double maxFat = Double.NEGATIVE_INFINITY;

        for (TiredThread w : workers) {
            long used = w.getTimeUsed();
            long idle = w.getTimeIdle();
            double fat = w.getFatigue();
            totalUsed += used;
            totalIdle += idle;
            minFat = Math.min(minFat, fat);
            maxFat = Math.max(maxFat, fat);
            report.append(String.format("%-8d | %-12d | %-12d | %-14.2f | %-6s\n",
                    w.getWorkerId(), used, idle, fat, w.isBusy()));
        }
        report.append("---------------------------------------------------------------\n");
        report.append(String.format("Totals: used=%d ns, idle=%d ns\n", totalUsed, totalIdle));
        report.append(String.format("Fatigue range: min=%.2f , max=%.2f\n", minFat, maxFat));
        report.append(String.format("In-flight tasks right now: %d\n", inFlight.get()));
        return report.toString();
    }
}
