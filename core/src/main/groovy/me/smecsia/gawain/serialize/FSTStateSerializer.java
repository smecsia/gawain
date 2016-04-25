package me.smecsia.gawain.serialize;

import org.nustaq.serialization.FSTConfiguration;

import java.util.Map;

/**
 * @author Ilya Sadykov
 */
@SuppressWarnings("unchecked")
public class FSTStateSerializer implements ToBytesStateSerializer {
    private static final FSTConfiguration serializer = FSTConfiguration.createDefaultConfiguration();

    @Override
    public byte[] serialize(Map object) {
        return (object != null) ? serializer.asByteArray(object) : null;
    }

    @Override
    public Map deserialize(byte[] bytes) {
        return (Map) serializer.asObject(bytes);
    }
}
