package viet.iot.project25.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates daemon-threads with {@link Thread#NORM_PRIORITY} and a sensible
 * name (prefix plus incrementing number) in the security manager's thread group
 * or if there is no security manager, in the calling thread's group.
 */
public class ContainerThreadFactory implements ThreadFactory {

    static protected final AtomicInteger threadNumber = new AtomicInteger(1);

    protected final String name;
    protected final ThreadGroup group;

    public ContainerThreadFactory(String name) {
        this.name = name;
        group = Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,  name + "-" + threadNumber.getAndIncrement(), 0);
        t.setDaemon(true);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
