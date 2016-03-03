package ru.qatools.gawain

/**
 * @author Ilya Sadykov
 */
interface Serializer<T> {
    byte[] toBytes(T object)

    T fromBytes(byte[] bytes)
}
