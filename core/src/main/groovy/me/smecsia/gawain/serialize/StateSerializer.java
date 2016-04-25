package me.smecsia.gawain.serialize;

import java.util.Map;

/**
 * @author Ilya Sadykov
 */
public interface StateSerializer<To> extends Serializer<Map, To> {

    To serialize(Map state);

    Map deserialize(To serializedObject);
}
