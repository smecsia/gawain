package me.smecsia.gawain.serialize;

/**
 * @author Ilya Sadykov
 */
public interface Serializer<From, To> {
    FSTStateSerializer DEFAULT_STATE_SERIALIZER = new FSTStateSerializer();
    FSTMessageSerializer DEFAULT_MSG_SERIALIZER = new FSTMessageSerializer();

    To serialize(From notSerializedObject);

    From deserialize(To serializedObject);
}
