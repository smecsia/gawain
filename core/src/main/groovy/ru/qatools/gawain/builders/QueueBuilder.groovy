package ru.qatools.gawain.builders

import groovy.transform.CompileStatic
import ru.qatools.gawain.GawainQueue
/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface QueueBuilder {

    GawainQueue build(String name, int maxSize);
}