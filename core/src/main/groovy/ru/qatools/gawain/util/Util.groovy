package ru.qatools.gawain.util
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
/**
 * @author Ilya Sadykov
 */
abstract class Util {
    static final def JSON = new JsonSlurper()

    static <T> Optional<T> opt(T val) {
        (val != null) ? Optional.of(val) : Optional.empty()
    }

    static Map fromJson(String json) {
        try {
            JSON.parseText(json) as Map
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse json: \n ${json}", e)
        }
    }

    static toJson(object) {
        JsonOutput.toJson(object)
    }
}
