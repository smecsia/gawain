package ru.qatools.gawain.impl

import groovy.transform.CompileStatic
import org.nustaq.serialization.FSTConfiguration
import ru.qatools.gawain.Serializer

/**
 * @author Ilya Sadykov
 */
@CompileStatic
class FSTSerializer<T> implements Serializer<T> {
    static final FSTConfiguration serializer = FSTConfiguration.createDefaultConfiguration()

    @Override
    byte[] toBytes(T object) {
        (object != null) ? serializer.asByteArray(object) : null
    }

    @Override
    T fromBytes(byte[] bytes) {
        serializer.asObject(bytes) as T
    }
}
