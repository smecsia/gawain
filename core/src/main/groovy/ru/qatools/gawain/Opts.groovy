package ru.qatools.gawain

import ru.qatools.gawain.impl.FSTSerializer

/**
 * @author Ilya Sadykov
 */
class Opts {
    private final Map<String, Object> opts = [:]
    int maxQueueSize = 0,           // max size for the queue
        processors = 10,            // max count of processor threads
        consumers = 1,              // max count of queue consumer threads
        bcConsumers = 1,            // max count of broadcaster queue consumer threads
        maxLockWaitMs = 1000        // max time to wait until lock is available
    long maxQueueWaitSec = 0,       // max time to wait until queue provides a message
         lockPollIntervalMs = 10    // interval to poll locks from db
    boolean global = false          // indicates global or local job
    Serializer<Map> serializer = new FSTSerializer()

    @Override
    Object getProperty(String name) {
        try {
            metaClass.getProperty(this, name)
        } catch (MissingPropertyException ignored) {
            opts[name]
        }
    }

    Opts(Map opts = [:]) {
        setProps(opts)
    }

    Opts set(Map opts) {
        setProps(opts); this
    }

    public static final Opts opts(Map opts) {
        new Opts(opts)
    }

    protected Map setProps(Map opts) {
        def self = this
        opts.each { k, v ->
            def key = k as String
            if (self.metaClass.properties.any { it.name == key }) {
                self."${key}" = v
            } else {
                self.opts[key] = v
            }
        }
    }
}
