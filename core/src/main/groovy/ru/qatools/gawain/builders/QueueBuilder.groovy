package ru.qatools.gawain.builders

import ru.qatools.gawain.GawainQueue
/**
 * @author Ilya Sadykov
 */
interface QueueBuilder {

    GawainQueue build(String name, int maxSize);
}