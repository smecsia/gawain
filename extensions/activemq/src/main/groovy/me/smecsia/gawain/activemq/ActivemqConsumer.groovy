package me.smecsia.gawain.activemq

import groovy.transform.CompileStatic
import me.smecsia.gawain.GawainQueueConsumer
import me.smecsia.gawain.Opts
import me.smecsia.gawain.serialize.ToBytesMessageSerializer

import javax.jms.BytesMessage
import javax.jms.MessageConsumer

import static me.smecsia.gawain.activemq.ActivemqUtil.ensureBytesSerializer
/**
 * @author Ilya Sadykov
 */
@CompileStatic
class ActivemqConsumer<T> implements GawainQueueConsumer<T> {
    final MessageConsumer consumer
    final ToBytesMessageSerializer<T> serializer
    final Opts opts

    ActivemqConsumer(MessageConsumer consumer, Opts opts = new Opts()) {
        this.consumer = consumer
        this.opts = opts
        this.serializer = ensureBytesSerializer(opts.messageSerializer)
    }

    @Override
    T consume() {
        def message = ((opts.maxQueueWaitSec > 0) ?
                consumer.receive(opts.maxQueueWaitSec) : consumer.receive()) as BytesMessage
        def bytes = new byte[message.getBodyLength()]
        message.readBytes(bytes)
        serializer.deserialize(bytes) as T
    }
}
