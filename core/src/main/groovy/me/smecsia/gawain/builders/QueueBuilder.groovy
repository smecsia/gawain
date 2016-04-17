package me.smecsia.gawain.builders
import groovy.transform.CompileStatic
import me.smecsia.gawain.GawainQueue
/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface QueueBuilder<Q extends GawainQueue> {

    Q build(String name, int maxSize)
}