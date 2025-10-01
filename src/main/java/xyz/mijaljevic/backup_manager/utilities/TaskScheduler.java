/**
 * Copyright (C) 2025 Karlo Mijaljević
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 *
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p>
 *
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * </p>
 */
package xyz.mijaljevic.backup_manager.utilities;

import xyz.mijaljevic.backup_manager.Defaults;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A simple task scheduler that uses a fixed thread pool to execute tasks
 * concurrently. The number of concurrent tasks is limited by a semaphore
 * to prevent overwhelming the system. Tasks are defined by a callback
 * function that takes an input of type T.
 */
public final class TaskScheduler {
    /**
     * Executor service to manage the thread pool.
     */
    private static final ExecutorService EXECUTOR_SERVICE = Executors
            .newVirtualThreadPerTaskExecutor();

    /**
     * Semaphore to limit the number of concurrent tasks.
     */
    private static final Semaphore SEMAPHORE = new Semaphore(Defaults.MAX_CONCURRENT_TASKS);

    /**
     * Schedule a new task to be executed. The task is defined by a callback
     * function that takes an input of type T.
     *
     * <p>
     * <b>Note:</b> The callback function is executed in a separate thread
     * from the thread pool. The number of concurrent tasks is limited by
     * a semaphore to prevent overwhelming the system. If the semaphore
     * cannot be acquired, the task will wait until a permit is available.
     * </p>
     *
     * <p>
     * <b>Warning:</b> The {@link TaskScheduler} must be shut down
     * using the {@link #shutdown()} method to ensure that all tasks
     * are completed before the application exits. Failing to do so
     * may result in lost tasks or incomplete operations.
     * </p>
     *
     * @param callback The callback function to execute.
     * @param input    The input to the callback function.
     */
    public static <T> void schedule(
            final Consumer<T> callback,
            final T input
    ) {
        EXECUTOR_SERVICE.submit(() -> {
            try {
                SEMAPHORE.acquire();
                callback.accept(input);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                SEMAPHORE.release();
            }
        });
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
    public static void shutdown() {
        try {
            EXECUTOR_SERVICE.shutdown();

            final boolean success = EXECUTOR_SERVICE.awaitTermination(
                    Long.MAX_VALUE,
                    TimeUnit.SECONDS
            );

            if (!success) {
                EXECUTOR_SERVICE.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            EXECUTOR_SERVICE.shutdownNow();
        }
    }
}
