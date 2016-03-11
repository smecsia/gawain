package ru.qatools.gawain

import groovy.transform.CompileStatic
import ru.qatools.gawain.impl.FSTSerializer

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class Opts {
    private final Map<String, Object> opts = [:]
    int maxQueueSize = 0,           // max size for the queue
        processors = 10,            // max count of processor threads
        consumers = 1,              // max count of queue consumer threads
        bcConsumers = 1,            // max count of broadcaster queue consumer threads
        maxLockWaitMs = 30000       // max time to wait until lock is available
    long maxQueueWaitSec = 0,       // max time to wait until queue provides a message
         lockPollIntervalMs = 10    // interval to poll locks from db
    boolean global = false          // indicates global or local job
    Serializer<Map> serializer = new FSTSerializer()

    // Constants just for convinience
    public static final String GLOBAL = 'global'
    public static final String SERIALIZER = 'serializer'

    @Override
    Object getProperty(String name) {
        try {
            metaClass.getProperty(this, name)
        } catch (MissingPropertyException ignored) {
            opts[name]
        }
    }

    Opts(List opts) {
        setProps(opts)
    }

    Opts(Map opts = [:]) {
        setProps(opts)
    }

    Opts set(List opts) {
        setProps(opts)
    }

    Opts set(Map opts) {
        setProps(opts); this
    }

    public static final Opts opts(Object... opts) {
        this.opts(opts as List)
    }

    public static final Opts opts(List opts) {
        new Opts(opts)
    }

    public static final Opts opts(Map opts) {
        new Opts(opts)
    }

    protected Opts setProps(List opts) {
        if (opts.size() % 2 > 0) {
            throw new IllegalAccessException("")
        }
        for (int idx = 0; idx < opts.size() - 1; idx += 2) {
            def map = [:]
            map.put(opts[idx], opts[idx + 1])
            setProps(map)
        }
        this
    }

    protected Opts setProps(Map opts) {
        def self = this
        opts.each { k, v ->
            def key = k as String
            if (self.metaClass.properties.any { it.name == key }) {
                self.metaClass.setProperty(self, key, v)
            } else {
                self.opts.put(key, v)
            }
        }
        this
    }
}
