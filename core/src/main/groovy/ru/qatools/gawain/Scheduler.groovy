package ru.qatools.gawain

import groovy.transform.Canonical

import java.util.concurrent.TimeUnit

import static java.util.concurrent.Executors.newSingleThreadExecutor

/**
 * @author Ilya Sadykov
 */
@Canonical
class Scheduler {
    public static final String LOCK_KEY = 'GawainScheduler'
    List<Job> globalJobs = []
    List<Job> localJobs = []
    List<Timer> startedTimers = []
    final Repository timerRepo

    def addJob(int frequency, TimeUnit unit, Closure task, Opts opts = new Opts()) {
        def job = new Job(frequency: frequency, unit: unit, task: task)
        if (opts.global) {
            globalJobs << job
        } else {
            localJobs << job
        }
    }

    def start() {
        newSingleThreadExecutor().submit({
            while (true) {
                timerRepo.lockAndGet(LOCK_KEY)
            }

        })
    }

    def suspend() {
        startedTimers.each { it.cancel() }
        startedTimers.clear()
    }

    class Job {
        int frequency
        TimeUnit unit
        Closure task

        def invoke() {
            task.run()
        }
    }

}
