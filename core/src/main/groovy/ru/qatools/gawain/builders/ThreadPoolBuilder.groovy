package ru.qatools.gawain.builders

import groovy.transform.CompileStatic

import java.util.concurrent.ExecutorService

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface ThreadPoolBuilder {
    ExecutorService build(int size)
}