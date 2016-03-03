package ru.qatools.gawain.impl

import org.nustaq.serialization.FSTConfiguration
import ru.qatools.gawain.Serializer

/**
 * @author Ilya Sadykov
 */
class FSTSerializer implements Serializer<Map> {
    static final FSTConfiguration serializer = FSTConfiguration.createDefaultConfiguration()

    @Override
    byte[] toBytes(Map object) {
        (object != null) ? serializer.asByteArray(object) : null
    }

    @Override
    Map fromBytes(byte[] bytes) {
        serializer.asObject(bytes) as Map
    }
}
