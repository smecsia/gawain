package ru.qatools.gawain
/**
 * @author Ilya Sadykov
 */
class Opts {
    private final Map<String, Object> opts = [:]
    int maxQueueSize = 0, processors = 10, consumers = 1, bcConsumers = 1
    int maxLockWaitSec = 30
    long maxQueueWaitSec = 0
    boolean global = false

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

    Opts with(Map opts = [:]) {
        setProps(opts); this
    }

    protected Map setProps(Map opts) {
        def self = this
        opts.each { k, v ->
            def key = k as String
            if (self.metaClass.properties.any {it.name == key}) {
                self."${key}" = v
            } else {
                self.opts[key] = v
            }
        }
    }
}
