package me.smecsia.gawain

/**
 * @author Ilya Sadykov
 */
interface ProcessingStrategy<T> {
    T process(T event)
}
