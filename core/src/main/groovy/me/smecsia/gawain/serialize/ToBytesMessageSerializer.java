package me.smecsia.gawain.serialize;

/**
 * @author Ilya Sadykov
 */
public interface ToBytesMessageSerializer<From> extends MessageSerializer<From, byte[]>{
    @Override
    byte[] serialize(From notSerializedObject);

    @Override
    From deserialize(byte[] serializedObject);
}
