package me.smecsia.gawain

import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import me.smecsia.gawain.serialize.MessageSerializer
import me.smecsia.gawain.serialize.StateSerializer

import java.lang.reflect.Field
import java.lang.reflect.Modifier

import static me.smecsia.gawain.serialize.Serializer.DEFAULT_MSG_SERIALIZER
import static me.smecsia.gawain.serialize.Serializer.DEFAULT_STATE_SERIALIZER

/**
 * @author Ilya Sadykov
 */
@CompileStatic
@AutoClone
class Opts implements Serializable {
    private final Map<String, Object> opts = [:]
    int maxQueueSize = 0,               // max size for the queue
        consumers = 1,                  // max count of queue consumer threads
        bcConsumers = 1,                // max count of broadcaster queue consumer threads
        maxLockWaitMs = 30000           // max time to wait until lock is available
    long maxQueueWaitSec = 0,           // max time to wait until queue provides a message
         lockPollIntervalMs = 10        // interval to poll locks from db
    boolean global = false              // indicates global or local job
    transient StateSerializer stateSerializer = DEFAULT_STATE_SERIALIZER
    transient MessageSerializer messageSerializer = DEFAULT_MSG_SERIALIZER

    // Constants just for convinience
    public static final String GLOBAL = 'global'
    public static final String STATE_SERIALIZER = 'stateSerializer'
    public static final String MSG_SERIALIZER = 'messageSerializer'

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

    Opts(Opts other) {
        setProps(clone(other).opts)
        stateSerializer = other.stateSerializer
        getClass().getDeclaredFields()
                .findAll({ f -> (f.modifiers & Modifier.FINAL) != Modifier.FINAL })
                .each { Field f ->
            def value = other.getProperty(f.name)
            if (Cloneable.isAssignableFrom(f.getType())) {
                value = value.clone()
            } else if (Serializable.isAssignableFrom(f.getType())) {
                value = clone(value as Serializable)
            }
            metaClass.setProperty(this, f.name, value)
        }
    }

    protected static <T extends Serializable> T clone(T value) {
        DEFAULT_MSG_SERIALIZER.deserialize(DEFAULT_MSG_SERIALIZER.serialize(value))
    }

    Opts set(List opts) {
        setProps(opts)
    }

    Opts set(Map opts) {
        setProps(opts); this
    }

    Opts clone() {
        new Opts(this)
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
