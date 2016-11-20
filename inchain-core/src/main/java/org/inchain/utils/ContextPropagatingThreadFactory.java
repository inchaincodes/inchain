package org.inchain.utils;

import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextPropagatingThreadFactory implements ThreadFactory {
    private static final Logger log = LoggerFactory.getLogger(ContextPropagatingThreadFactory.class);
    private final String name;
    private final int priority;

    public ContextPropagatingThreadFactory(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public ContextPropagatingThreadFactory(String name) {
        this(name, Thread.NORM_PRIORITY);
    }

    @Override
    public Thread newThread(final Runnable r) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Exception e) {
                    log.error("Exception in thread", e);
                }
            }
        }, name);
        thread.setPriority(priority);
        thread.setDaemon(true);
        return thread;
    }
}
