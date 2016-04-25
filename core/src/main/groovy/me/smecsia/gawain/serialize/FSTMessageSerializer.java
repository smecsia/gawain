package me.smecsia.gawain.serialize;

import org.nustaq.serialization.FSTConfiguration;

/**
 * @author Ilya Sadykov
 */
@SuppressWarnings("unchecked")
public class FSTMessageSerializer<T> implements ToBytesMessageSerializer<T> {
    private static final FSTConfiguration serializer = FSTConfiguration.createDefaultConfiguration();

    @Override
    public byte[] serialize(T message) {
        return (message != null) ? serializer.asByteArray(message) : null;
    }

    @Override
    public T deserialize(byte[] serializedMessage) {
        return (T) serializer.asObject(serializedMessage);
    }
}
