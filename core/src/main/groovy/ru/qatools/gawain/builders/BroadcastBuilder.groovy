package ru.qatools.gawain.builders

import groovy.transform.CompileStatic
import ru.qatools.gawain.Broadcaster
import ru.qatools.gawain.Gawain
import ru.qatools.gawain.Opts

/**
 * @author Ilya Sadykov
 */
@CompileStatic
trait BroadcastBuilder {

    abstract Broadcaster build(String name, Gawain gawain, Opts opts);

}