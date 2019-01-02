package fpdm.eai.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import matrix.db.Context;
import matrix.db.JPO;

public class WebServiceManager_mxJPO {

    private static Logger logger = LoggerFactory.getLogger(WebServiceManager_mxJPO.class);

    private static final String sPageName = "FPDM_EAI.properties";

    private static boolean INITIALIZED = false;

    private static String EAI_ADDRESS = "";

    private static void initialization(Context context) {
        if (!fpdm.eai.utils.WebServiceManager_mxJPO.INITIALIZED) {
            Properties _propertyResource;
            try {
                _propertyResource = fpdm.utils.Page_mxJPO.getPropertiesFromPage(context, sPageName);
                fpdm.eai.utils.WebServiceManager_mxJPO.EAI_ADDRESS = _propertyResource.getProperty("FPDM_EAI.URL");
                fpdm.eai.utils.WebServiceManager_mxJPO.INITIALIZED = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static NamespaceContext nsSoapContext = new NamespaceContext() {
        @Override
        public Iterator<?> getPrefixes(String namespaceURI) {
            return null;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return "soap";
        }

        @Override
        public String getNamespaceURI(String prefix) {
            return "http://schemas.xmlsoap.org/soap/envelope/";
        }
    };

    /**
     * Create a SOAPMessage correctly formatted to be sent to the EAI
     * @param mInputAll
     *            A Map containing keys/values to put in the message
     * @return SOAPMessage
     */
    /**
     * Create a SOAPMessage correctly formatted to be sent to the EAI
     * @param mInputAll
     *            A Map containing keys/values to put in the message
     * @return SOAPMessage
     */
    private static SOAPMessage createSOAPRequest(Map<String, ?> mInputAll) throws Exception {

        Iterator<?> i = mInputAll.entrySet().iterator();

        MessageFactory messageFactory = MessageFactory.newInstance();
        logger.debug("MessageFactory OK");
        SOAPMessage soapMessage = messageFactory.createMessage();
        logger.debug("SOAPMessage OK");
        SOAPPart soapPart = (SOAPPart) soapMessage.getSOAPPart();
        logger.debug("SOAPPart OK");

        String serverURI = "http://ws.faurecia.com/";
        logger.debug("serverURI : " + serverURI);

        String sMethod = (String) mInputAll.get("sMethod");
        logger.debug("sMethod : " + sMethod);

        // SOAP Envelope
        SOAPEnvelope envelope = (SOAPEnvelope) soapPart.getEnvelope();
        logger.debug("SOAPEnvelope OK");
        envelope.addNamespaceDeclaration("ws", serverURI);
        logger.debug("addNamespaceDeclaration OK");

        /*
         * Constructed SOAP Request Message: <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:example="http://ws.cdyne.com/"> <SOAP-ENV:Header/> <SOAP-ENV:Body>
         * <example:VerifyEmail> <example:email>mutantninja@gmail.com</example:email> <example:LicenseKey>123</example:LicenseKey> </example:VerifyEmail> </SOAP-ENV:Body> </SOAP-ENV:Envelope>
         */

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();
        logger.debug("SOAPBody OK");
        SOAPElement soapBodyElem = soapBody.addChildElement("executeFromWS", "ws");
        logger.debug("SOAPElement OK");
        SOAPElement soapMethodName = soapBodyElem.addChildElement("methodName");
        soapMethodName.setTextContent(sMethod);

        SOAPElement soapBodyArgs = soapBodyElem.addChildElement("args");
        SOAPElement soapBodyArgsMap = soapBodyArgs.addChildElement("map");

        while (i.hasNext()) {
            try {
                SOAPElement soapEntry = soapBodyArgsMap.addChildElement("entry");

                Entry<?, ?> entry = (Entry<?, ?>) i.next();

                SOAPElement soapKey = soapEntry.addChildElement("key");
                soapKey.setTextContent((String) entry.getKey());

                SOAPElement soapValue = soapEntry.addChildElement("value");
                soapValue.setTextContent((String) entry.getValue());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        soapMessage.saveChanges();

        /* Print the request message */
        logger.debug("Request SOAP Message = ");

        // Get the Envelope Source
        Source src = soapMessage.getSOAPPart().getContent();

        // Transform the Source into a StreamResult to get the XML
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(src, result);
        String xmlString = result.getWriter().toString();
        logger.debug(xmlString);
        logger.debug("");

        return soapMessage;
    }

    public static String callEAIWS(Context context, String[] args) throws Exception {
        HashMap<String, String> requestMap = (HashMap<String, String>) JPO.unpackArgs(args);
        logger.debug(requestMap.toString());

        String webService = (String) requestMap.get("webService");
        String sMethod = (String) requestMap.get("sMethod");

        if ((null != webService) && ("" != webService)) {
            if ((null != sMethod) && ("" != sMethod)) {
                return fpdm.eai.utils.WebServiceManager_mxJPO.callEAIWS(context, webService, sMethod, requestMap);
            }
        }

        return "";
    }

    public static String callEAIWS(Context context, String webService, String sMethod, Map<String, String> mMethodAgs) {

        if (!fpdm.eai.utils.WebServiceManager_mxJPO.INITIALIZED) {
            fpdm.eai.utils.WebServiceManager_mxJPO.initialization(context);
        }

        String sResult = null;

        String sEAIWebserviceURL = fpdm.eai.utils.WebServiceManager_mxJPO.EAI_ADDRESS + "/" + webService;
        logger.debug("sEAIWebserviceURL = " + sEAIWebserviceURL);

        try {
            logger.debug("init Service");
            // Create SOAP Connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            logger.debug("SOAPConnectionFactory OK");
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();
            logger.debug("SOAPConnection OK");

            mMethodAgs.put("sMethod", sMethod);

            // Send SOAP Message to SOAP Server
            SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(mMethodAgs), sEAIWebserviceURL);
            logger.debug("SOAPMessage OK");

            // Process the SOAP Response
            sResult = processSOAPResponse(soapResponse);
            logger.debug("SOAP Response : " + sResult);

            soapConnection.close();
        } catch (Exception e) {
            logger.error("Error occurred while sending SOAP Request to Server");
            logger.error(e.getMessage());
            e.printStackTrace();
        }

        return sResult;
    }

    /**
     * Method used to print the SOAP Response
     */
    private static String processSOAPResponse(SOAPMessage soapResponse) throws Exception {
        // Get the Envelope Source
        Source src = soapResponse.getSOAPPart().getContent();

        // Transform the Source into a StreamResult to get the XML
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(src, result);
        String xmlString = result.getWriter().toString();

        // logger.debug("Response SOAP Message = ");
        // logger.debug(xmlString);

        InputStream is = new ByteArrayInputStream(xmlString.getBytes());
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        org.w3c.dom.Document doc = builder.parse(is);
        XPathFactory xPathfactory = XPathFactory.newInstance();
        // logger.debug("Root element :" + doc.getDocumentElement().getNodeName());

        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("//return");

        // logger.debug("Doc type : " + doc.getXmlEncoding());

        NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        try {
            return nl.item(0).getTextContent();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return "";
        }
    }
}
