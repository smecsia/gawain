package ru.qatools.gawain.activemq
import groovy.transform.CompileStatic
import ru.qatools.gawain.GawainQueue
import ru.qatools.gawain.Opts

import javax.jms.Session
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ActivemqQueue<T extends Serializable> extends AbstractActivemqRouter<T> implements GawainQueue<T> {

    ActivemqQueue(String destName, Session session, Opts opts = new Opts()) {
        super(destName, session, session.createQueue(destName), opts)
    }

    @Override
    def add(T event) {
        produce(event)
    }

    @Override
    T take() {
        consume()
    }
}
