package me.smecsia.gawain.impl

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import me.smecsia.gawain.Opts
import me.smecsia.gawain.Repository
import me.smecsia.gawain.Scheduler
import me.smecsia.gawain.error.LockWaitTimeoutException

import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

import static java.lang.System.currentTimeMillis
import static java.util.concurrent.Executors.newSingleThreadExecutor

/**
 * @author Ilya Sadykov
 */
@Canonical
@CompileStatic
class SchedulerImpl implements Scheduler {
    static final Logger LOGGER = LoggerFactory.getLogger(SchedulerImpl)
    public static final String LOCK_KEY = 'GawainScheduler'
    public static final int HB_INTERVAL_MS = 5000
    public static final int MAX_NO_HB_INTERVAL_MS = 10000
    public static final int MAX_INITIAL_DELAY_MS = 100
    List<Job> globalJobs = []
    List<Job> localJobs = []
    List<Timer> startedTimers = []
    ExecutorService executor = newSingleThreadExecutor()
    volatile boolean terminated = false
    volatile boolean master = false
    final String name
    final Repository timerRepo
    final long maxNoHBMs
    final int hbIntervalMs
    final int maxInitialDelayMs

    SchedulerImpl(String name, Repository timerRepo, long maxNoHBMs = MAX_NO_HB_INTERVAL_MS,
                  int hbCheckIntervalMs = HB_INTERVAL_MS, int maxInitialDelay = MAX_INITIAL_DELAY_MS) {
        this.name = name
        this.timerRepo = timerRepo
        this.maxNoHBMs = maxNoHBMs
        this.hbIntervalMs = hbCheckIntervalMs
        this.maxInitialDelayMs = maxInitialDelay
    }

    @Override
    def addJob(int frequency, TimeUnit unit, Closure task, Opts opts = new Opts()) {
        LOGGER.debug("[${name}] Registering ${opts.global ? 'global' : 'local'} job occurring every ${frequency} ${unit}")
        def job = new Job(frequency, unit, task)
        if (opts.global) {
            globalJobs << job
        } else {
            localJobs << job
        }
    }

    @Override
    def start() {
        LOGGER.debug("[${name}] Starting scheduler")
        terminated = false
        executor.submit({
            LOGGER.debug("[${name}] Starting local jobs")
            startJobs(localJobs)
            LOGGER.debug("[${name}] Waiting for master lock")
            if (becomeMaster()) {
                LOGGER.info("[${name}] Now I am master scheduler!")
                startJobs(globalJobs)
                LOGGER.debug("[${name}] Starting master loop")
                masterLoop()
            } else {
                restart()
            }
        })
    }

    @Override
    def restart() {
        LOGGER.warn("[${name}] Restarting!")
        suspend()
        sleep(hbIntervalMs)
        start()
    }

    @Override
    def suspend() {
        LOGGER.warn("[${name}] Suspending all started jobs!")
        startedTimers.each { it.cancel() }
        startedTimers.clear()
    }

    @Override
    def terminate() {
        LOGGER.warn("[${name}] Terminating scheduler!")
        terminated = true
    }

    @Override
    boolean isMaster() {
        master
    }

    private startJobs(Collection<Job> jobs) {
        try {
            jobs.each { job ->
                def timer = new Timer()
                timer.schedule(job.task as TimerTask, new Random().nextInt(maxInitialDelayMs), job.unit.toMillis(job.frequency))
                startedTimers << timer
            }
        } catch (Exception e) {
            LOGGER.error("[${name}] Failed to start jobs", e)
        }
    }

    private boolean becomeMaster() {
        master = false
        while (!terminated) {
            try {
                timerRepo.lock(LOCK_KEY)
                return true
            } catch (LockWaitTimeoutException ignored) {
                LOGGER.debug("[${name}] Still waiting for master scheduler to release the lock...")
                if (lastHb() < currentTimeMillis() - maxNoHBMs) {
                    LOGGER.warn("[${name}] Last heartbeat is older than ${maxNoHBMs}ms, forcing unlock")
                    timerRepo.forceUnlock(LOCK_KEY)
                }
            } catch (Exception e) {
                LOGGER.error("[${name}] Error while waiting for master scheduler to release the lock", e)
                sleep(hbIntervalMs)
            }
        }
        return false
    }

    private masterLoop() {
        try {
            master = true
            while (!terminated && timerRepo.isLockedByMe(LOCK_KEY)) {
                LOGGER.debug("[${name}] Updating heartbeat...")
                timerRepo.put(LOCK_KEY, [lastUpdated: currentTimeMillis()])
                sleep(hbIntervalMs)
            }
        } catch (Exception e) {
            LOGGER.error("[${name}] Master loop has been terminated due to error", e)
        }
        LOGGER.warn("[${name}] Master loop has been ended, restarting...")
        if (timerRepo.isLockedByMe(LOCK_KEY)) {
            timerRepo.unlock(LOCK_KEY)
        }
        restart()
    }

    private long lastHb() {
        (timerRepo.get(LOCK_KEY)?.lastUpdated ?: 0) as long
    }

    @Canonical
    class Job {
        final int frequency
        final TimeUnit unit
        final Closure task

        Job(int frequency, TimeUnit unit, Closure task) {
            this.frequency = frequency
            this.unit = unit
            this.task = task
        }
    }

}
