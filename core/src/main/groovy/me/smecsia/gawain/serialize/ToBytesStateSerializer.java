package me.smecsia.gawain.serialize;

import java.util.Map;

/**
 * @author Ilya Sadykov
 */
public interface ToBytesStateSerializer extends StateSerializer<byte[]> {

    @Override
    byte[] serialize(Map state);

    @Override
    Map deserialize(byte[] serializedObject);
}