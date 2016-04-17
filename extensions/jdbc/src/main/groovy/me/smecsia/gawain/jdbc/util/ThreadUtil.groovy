package me.smecsia.gawain.jdbc.util

import groovy.transform.CompileStatic

import static java.lang.Thread.currentThread
import static java.lang.management.ManagementFactory.getRuntimeMXBean
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ThreadUtil {
    private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<Long>();

    private ThreadUtil() {
    }

    /**
     * Get the thread id
     *
     * @return the thread id
     */
    public static String threadId() {
        final Long threadId = THREAD_LOCAL.get();
        if (threadId != null) {
            return getRuntimeMXBean().getName() + "-" + threadId;
        }
        return getRuntimeMXBean().getName() + "-" + currentThread().getId();
    }
}
