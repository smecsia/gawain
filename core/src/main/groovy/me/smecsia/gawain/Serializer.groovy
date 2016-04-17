package me.smecsia.gawain

import groovy.transform.CompileStatic

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface Serializer<T> {
    byte[] toBytes(T object)

    T fromBytes(byte[] bytes)
}
