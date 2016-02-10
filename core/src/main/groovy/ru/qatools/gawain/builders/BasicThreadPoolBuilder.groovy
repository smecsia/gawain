package ru.qatools.gawain.builders
import groovy.transform.CompileStatic

import java.util.concurrent.ExecutorService

import static java.util.concurrent.Executors.newFixedThreadPool
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class BasicThreadPoolBuilder implements ThreadPoolBuilder {
    @Override
    ExecutorService build(int size){
        newFixedThreadPool(size)
    }
}
