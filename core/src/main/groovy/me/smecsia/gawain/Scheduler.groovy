package me.smecsia.gawain

import groovy.transform.CompileStatic

import java.util.concurrent.TimeUnit

/**
 * @author Ilya Sadykov
 */
@CompileStatic
interface Scheduler {

    def addJob(int frequency, TimeUnit unit, Closure task, Opts opts)

    def addJob(int frequency, TimeUnit unit, Closure task)

    def start()

    def restart()

    def suspend()

    def terminate()

    boolean isMaster()

}