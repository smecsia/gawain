package me.smecsia.gawain.activemq

import me.smecsia.gawain.error.InitializationException
import me.smecsia.gawain.serialize.Serializer
import me.smecsia.gawain.serialize.ToBytesMessageSerializer

/**
 * @author Ilya Sadykov
 */
abstract class ActivemqUtil {
    private ActivemqUtil() {
    }

    static <T> ToBytesMessageSerializer<T> ensureBytesSerializer(Serializer serializer) {
        if (!(serializer instanceof ToBytesMessageSerializer)) {
            throw new InitializationException("Cannot use ${serializer} message serializer! " +
                    "ActiveMQ supports only bytes serialization!")
        }
        serializer as ToBytesMessageSerializer
    }
}
