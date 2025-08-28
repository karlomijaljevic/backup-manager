/**
 * Copyright (C) 2025 Karlo Mijaljević
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package xyz.mijaljevic.backup_manager.utilities;

import xyz.mijaljevic.backup_manager.Defaults;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A simple task scheduler that uses a fixed thread pool to execute tasks
 * concurrently. The number of threads in the pool is configurable.
 *
 * @param <T> The type of the input to the task.
 */
public final class TaskScheduler<T> {
    /**
     * Executor service to manage the thread pool.
     */
    private final ExecutorService executorService;

    /**
     * Create a new task scheduler with a fixed number of threads.
     *
     * @param threads The number of threads to use for executing tasks.
     *                Must be at least 1.
     * @throws IllegalArgumentException If the number of threads is less than 1.
     */
    public TaskScheduler(Integer threads) {
        threads = sanitizeThreadNumber(threads);

        Logger.info("Creating task scheduler with "
                + threads
                + " thread"
                + (threads == 1 ? "." : "s.")
        );

        this.executorService = Executors.newFixedThreadPool(
                sanitizeThreadNumber(threads),
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(
                                runnable,
                                "TaskScheduler-" + count.getAndIncrement()
                        );

                        thread.setDaemon(false);
                        thread.setPriority(Thread.NORM_PRIORITY);

                        return thread;
                    }
                }
        );
    }

    /**
     * Schedule a new task to be executed. The task is defined by a callback
     * function that takes an input of type T.
     *
     * @param callback The callback function to execute.
     * @param input    The input to the callback function.
     */
    public void schedule(Consumer<T> callback, T input) {
        executorService.submit(() -> callback.accept(input));
    }

    /**
     * Shutdown the task scheduler. This will stop accepting new tasks and
     * will wait for all running tasks to complete. No new tasks will be
     * accepted after this method is called.
     *
     * <p>
     * <b>Note:</b> This method blocks until all tasks have completed
     * execution. If the current thread is interrupted while waiting, it
     * will re-interrupt itself and proceed to shut down the executor
     * service immediately.
     * </p>
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void shutdown() {
        try {
            executorService.shutdown();

            executorService.awaitTermination(
                    Long.MAX_VALUE,
                    TimeUnit.SECONDS
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executorService.shutdownNow();
        }
    }

    /**
     * Sanitizes the number of threads to use for file operations. If the
     * provided number of threads is greater than the number of available
     * processors, it will be set to the number of available processors.
     * If the provided number of threads is less than 1, it will be set to
     * the default number of threads defined in {@link Defaults#THREAD_NUMBER}.
     *
     * @param threads The requested number of threads to utilize.
     * @return The sanitized number of threads.
     */
    private static int sanitizeThreadNumber(int threads) {
        int max = Runtime.getRuntime().availableProcessors();

        if (threads > max) {
            return max;
        } else if (threads < 1) {
            return Defaults.THREAD_NUMBER;
        } else {
            return threads;
        }
    }
}
