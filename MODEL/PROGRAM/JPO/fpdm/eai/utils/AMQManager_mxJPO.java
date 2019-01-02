package fpdm.eai.utils;

import java.util.HashMap;
import java.util.Properties;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import matrix.db.Context;

public class AMQManager_mxJPO {

    private final static Logger logger = LoggerFactory.getLogger("fpdm.eai.utils.AMQManager");

    private static AMQManager_mxJPO singleton = null;

    private static ActiveMQConnectionFactory connectionFactory;

    private static ActiveMQConnection connection;

    private static ActiveMQSession session;

    private static String sQueueName = null;

    private static String sSAPTransferCategoryName = null;

    private static String sSAPTransferMessageName = null;

    /**
     * Constructor
     * @param context
     *            the eMatrix <code>Context</code> object
     * @throws Exception
     */
    private AMQManager_mxJPO(Context context) throws Exception {
        loadConfiguration(context);
    }

    /**
     * Singleton
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return return an instance of object
     * @throws Exception
     */
    public final static AMQManager_mxJPO getInstance(Context context) throws Exception {
        if (singleton == null) {
            logger.debug("getInstance() - >>>> Start inializing singleton");
            singleton = new AMQManager_mxJPO(context);
        }
        if (!connection.isAlwaysSyncSend()) {
            connection = (ActiveMQConnection) connectionFactory.createQueueConnection();
            session = (ActiveMQSession) connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        } else if (session.isClosed()) {
            session = (ActiveMQSession) connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        }

        return singleton;
    }

    /**
     * Initialize singleton informations
     * @param context
     *            the eMatrix <code>Context</code> object
     * @throws Exception
     */
    private void loadConfiguration(Context context) throws Exception {
        try {
            logger.debug("AMQManager_mxJPO() - >>>> Start loading EAI Configuration");
            Properties _propertyResource = fpdm.utils.Page_mxJPO.getPropertiesFromPage(context, "FPDM_EAI.properties");
            String sConnectionFactoryNamesValue = _propertyResource.getProperty("FPDM_EAI.connectionFactoryNames");
            logger.debug("AMQManager_mxJPO() - sConnectionFactoryNamesValue = <" + sConnectionFactoryNamesValue + ">");
            String sEAI_URL = _propertyResource.getProperty("FPDM_EAI.AMQ.URL");
            logger.debug("AMQManager_mxJPO() - sEAI_URL = <" + sEAI_URL + ">");
            sQueueName = _propertyResource.getProperty("FPDM_EAI.QueueName");
            logger.debug("AMQManager_mxJPO() - sQueueName = <" + sQueueName + ">");
            sSAPTransferCategoryName = _propertyResource.getProperty("FPDM_EAI.SAP.CategoryName");
            sSAPTransferMessageName = _propertyResource.getProperty("FPDM_EAI.SAP.MessageName");

            connectionFactory = new ActiveMQConnectionFactory(sEAI_URL);
            connection = (ActiveMQConnection) connectionFactory.createQueueConnection();
            session = (ActiveMQSession) connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        } catch (JMSException e) {
            logger.error("Error in loadConfiguration()\n", e);
            throw e;
        }
    }

    /**
     * Add a new message on the EAI Queue
     * @param category
     *            Message category
     * @param messageName
     *            Message text
     * @param hmParams
     *            All message informations (example: object ID)
     * @throws JMSException
     */
    public void addMessage(String category, String messageName, HashMap<String, ?> hmParams) throws JMSException {
        try {
            logger.debug("addMessage() - category=<" + category + ">  messageName=<" + messageName + "> hmParams = <" + hmParams + ">");
            logger.debug("addMessage() - connection = <" + connection + ">");
            logger.debug("addMessage() - session.isClosed() = <" + session.isClosed() + ">");

            Destination destination = session.createQueue(sQueueName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            logger.debug("addMessage() - producer = <" + producer.getDestination().toString() + ">");

            TextMessage msg = session.createTextMessage(messageName);
            msg.setText(messageName);
            msg.setObjectProperty("args", hmParams);
            msg.setStringProperty("category", category);

            producer.send(msg);
            logger.debug("addMessage() - Message sent.");

        } catch (JMSException e) {
            logger.error("Error in addMessage()\n", e);
            throw e;
        }
    }

    /**
     * Add a new message on the EAI Queue
     * @param category
     *            Message category
     * @param messageName
     *            Message text
     * @param hmParams
     *            All message informations (example: object ID)
     * @throws JMSException
     */
    public void addSAPTransferMessage(HashMap<String, ?> hmParams) throws JMSException {
        try {
            logger.debug("addSAPTransferMessage() - category=<" + sSAPTransferCategoryName + ">  messageName=<" + sSAPTransferMessageName + "> hmParams = <" + hmParams + ">");
            logger.debug("addSAPTransferMessage() - session = <" + session + ">");
            logger.debug("addSAPTransferMessage() - session.isClosed() = <" + session.isClosed() + ">");
            logger.debug("addSAPTransferMessage() - session.isClosed() = <" + session.isClosed() + ">");
            logger.debug("addSAPTransferMessage() - session.isClosed() = <" + session.isClosed() + ">");

            Destination destination = session.createQueue(sQueueName);
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            logger.debug("addMessage() - producer = <" + producer.getDestination().toString() + ">");

            TextMessage msg = session.createTextMessage(sSAPTransferMessageName);
            msg.setText(sSAPTransferMessageName);
            msg.setObjectProperty("args", hmParams);
            msg.setStringProperty("category", sSAPTransferCategoryName);

            producer.send(msg);
            logger.debug("addSAPTransferMessage() - Message sent.");

        } catch (JMSException e) {
            logger.error("Error in addSAPTransferMessage()\n", e);
            throw e;
        }
    }

}
