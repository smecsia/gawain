package ru.qatools.gawain

/**
 * @author Ilya Sadykov
 */
interface ProcessingStrategy<T> {
    T process(T event)
}
