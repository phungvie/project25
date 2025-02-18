package viet.iot.project25.concurrent;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread pool that adds logging for tasks that throw exceptions
 */
public class ContainerScheduledExecutor extends ScheduledThreadPoolExecutor {

    protected static final Logger LOG = Logger.getLogger(ContainerScheduledExecutor.class.getName());

    public ContainerScheduledExecutor(String name, int corePoolSize) {
        this(name, corePoolSize, new CallerRunsPolicy());
    }

    public ContainerScheduledExecutor(String name, int corePoolSize, RejectedExecutionHandler rejectedHandler) {
        super(
            corePoolSize,
            new ContainerThreadFactory(name),
            // Wrap rejected handler to add logging
            (r, executor) -> {
                // Log and discard
                LOG.info("Container scheduled thread pool '" + executor + "' rejected execution of " + r);
                rejectedHandler.rejectedExecution(r, executor);
            });
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        logExceptionCause(runnable, throwable);
    }

    protected static void logExceptionCause(Runnable runnable, Throwable throwable) {
        if (throwable != null) {
            Throwable cause = unwrap(throwable);
            if (cause instanceof InterruptedException) {
                // Ignore this, might happen when we shutdownNow() the executor. We can't
                // log at this point as the logging system might be stopped already.
                return;
            }
            LOG.log(Level.WARNING, "Thread terminated unexpectedly executing: " +runnable.getClass(), throwable);
        }
    }

    protected static Throwable unwrap(Throwable throwable) throws IllegalArgumentException {
        if (throwable == null) {
            throw new IllegalArgumentException("Cannot unwrap null throwable");
        }
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            throwable = current;
        }
        return throwable;
    }
}
