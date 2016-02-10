package ru.qatools.gawain.activemq.util
import groovy.transform.CompileStatic
import org.apache.activemq.broker.BrokerService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.yandex.qatools.embed.service.AbstractEmbeddedService

import static java.util.UUID.randomUUID
import static jodd.io.FileUtil.deleteDir
/**
 * @author Ilya Sadykov
 */
@CompileStatic
public class ActivemqEmbeddedService extends AbstractEmbeddedService {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String brokerUrl;
    private final String brokerName;
    private BrokerService broker;

    public ActivemqEmbeddedService(String brokerUrl, String brokerName) throws IOException {
        super(null, true, 10000);
        this.brokerUrl = brokerUrl;
        this.brokerName = brokerName + randomUUID();
    }

    @Override
    public void doStart() {
        try {
            broker = new BrokerService();
            broker.setBrokerName(brokerName);
            broker.addConnector(brokerUrl);
            broker.setDataDirectory(dataDirectory);
            broker.start();
        } catch (Exception e) {
            logger.error("Failed to startup embedded ActiveMQ", e);
        }
    }

    @Override
    public void doStop() {
        if (broker != null) {
            try {
                broker.stop();
                if (removeDataDir) {
                    try {
                        deleteDir(new File(dataDirectory));
                    } catch (Exception e) {
                        logger.error("Failed to remove data dir", e);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to stop embedded ActiveMQ service", e);
            }
        }
    }
}
