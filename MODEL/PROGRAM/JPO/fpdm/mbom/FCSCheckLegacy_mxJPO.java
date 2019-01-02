package fpdm.mbom;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.rpc.ServiceException;
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

public class FCSCheckLegacy_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("fpdm.mbom.FCSCheckLegacy");

    private String sEAI_URL = "";

    private String sEAI_CheckLegacyWebService_Name = "";

    private String sEAI_CheckLegacyWebService_Method = "";

    private static final String sPageName = "FPDM_EAI.properties";

    private static NamespaceContext nsSoapContext = new NamespaceContext() {
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
     * Constructor
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            no arguments
     * @throws Exception
     */
    public FCSCheckLegacy_mxJPO(Context context, String[] args) throws Exception {
        Properties _propertyResource = fpdm.utils.Page_mxJPO.getPropertiesFromPage(context, sPageName);
        this.sEAI_URL = _propertyResource.getProperty("FPDM_EAI.URL");
        this.sEAI_CheckLegacyWebService_Name = _propertyResource.getProperty("FPDM_EAI.CheckLegacy.WebService.Name");
        this.sEAI_CheckLegacyWebService_Method = _propertyResource.getProperty("FPDM_EAI.CheckLegacy.WebService.Method");
    }

    /**
     * Calls the EAI web service which will call KMP241 WebService and returns a list of unmapped material master names.
     * @plm.usage Program : fpdm.mbom.MBOM100Management
     * @param mlMBOMPartToCheck
     *            Parts to check
     * @return
     * @throws Exception
     */
    public Vector<String> checkLegaciesMappedInSAP(ArrayList<Map<String, String>> mlMBOMPartToCheck) throws Exception {
        Vector<String> alUnmappedLegacies = null;
        try {

            String sEAIWebserviceURL = this.sEAI_URL + "/" + this.sEAI_CheckLegacyWebService_Name;
            logger.debug("checkLegaciesMappedInSAP() - sEAIWebserviceURL = <" + sEAIWebserviceURL + "> sEAI_CheckLegacyWebService_Method = <" + this.sEAI_CheckLegacyWebService_Method + ">");

            logger.debug("checkLegaciesMappedInSAP() - init Service");
            // Create SOAP Connection
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();

            // Send SOAP Message to SOAP Server
            SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(mlMBOMPartToCheck, this.sEAI_CheckLegacyWebService_Method), sEAIWebserviceURL);
            logger.debug("checkLegaciesMappedInSAP() - SOAPMessage OK");

            // Process the SOAP Response
            alUnmappedLegacies = processSOAPResponse(soapResponse);
            logger.debug("checkLegaciesMappedInSAP() - SOAP Response  = <" + alUnmappedLegacies + ">");

            // close soap connection
            soapConnection.close();

        } catch (Exception e) {
            logger.error("Error in checkLegaciesMappedInSAP()\n", e);
            throw e;
        }

        return alUnmappedLegacies;

    }

    /**
     * Construct SOAP message to send to EAI web service
     * @param alLegacies
     *            Parts to check
     * @param sMethod
     *            Web service method name
     * @return
     * @throws Exception
     */
    private SOAPMessage createSOAPRequest(ArrayList<Map<String, String>> alLegacies, String sMethod) throws Exception {

        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = (SOAPPart) soapMessage.getSOAPPart();

        String serverURI = "http://ws.faurecia.com/";
        logger.debug("serverURI : " + serverURI);

        // SOAP Envelope
        SOAPEnvelope envelope = (SOAPEnvelope) soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("ws", serverURI);

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapBodyElem = soapBody.addChildElement(sMethod, "ws");

        for (Map<String, String> map : alLegacies) {
            try {
                SOAPElement soapKey = soapBodyElem.addChildElement("arg0");
                soapKey.setTextContent(map.get("matrx_matnr") + "|" + map.get("werks"));
            } catch (Exception e) {
                logger.debug(e.getMessage());
            }
        }

        soapMessage.saveChanges();

        /* Print the request message */
        // Get the Envelope Source
        Source src = soapMessage.getSOAPPart().getContent();
        // Transform the Source into a StreamResult to get the XML
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(src, result);
        String xmlString = result.getWriter().toString();
        logger.debug("createSOAPRequest() - Request SOAP Message = <" + xmlString + ">");

        return soapMessage;
    }

    /**
     * Get result from web service SOAP message
     * @param soapResponse
     *            Web service message
     * @return
     * @throws Exception
     */
    private static Vector<String> processSOAPResponse(SOAPMessage soapResponse) throws Exception {
        Vector<String> vResult = new Vector<String>();
        try {
            // Get the Envelope Source
            Source src = soapResponse.getSOAPPart().getContent();

            // Transform the Source into a StreamResult to get the XML
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(src, result);
            String xmlString = result.getWriter().toString();
            logger.debug("processSOAPResponse() - Response SOAP Message = <" + xmlString + ">");

            InputStream is = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();

            org.w3c.dom.Document doc = builder.parse(is);
            logger.debug("processSOAPResponse() - doc type = <" + doc.getDoctype() + ">");
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            xpath.setNamespaceContext(nsSoapContext);

            // check error soap return error
            XPathExpression exprFault = xpath.compile("//soap:Fault/*");
            NodeList nodes = (NodeList) exprFault.evaluate(doc, XPathConstants.NODESET);
            logger.debug("processSOAPResponse() - soap:Fault nodes.getLength() = <" + nodes.getLength() + ">");
            if (nodes.getLength() > 0) {
                StringBuilder sbError = new StringBuilder();
                for (int i = 0; i < nodes.getLength(); i++) {
                    sbError.append(nodes.item(i).getNodeName());
                    sbError.append(" : <");
                    sbError.append(nodes.item(i).getTextContent());
                    sbError.append("> ");
                }
                logger.debug("processSOAPResponse() - sbError = " + sbError);
                vResult.add(sbError.toString());
            } else {
                XPathExpression expr = xpath.compile("//return/text()");
                nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                logger.debug("processSOAPResponse() - nodes.getLength() = <" + nodes.getLength() + ">");

                for (int i = 0; i < nodes.getLength(); i++) {
                    logger.debug("processSOAPResponse() - node value = " + nodes.item(i).getNodeValue());
                    vResult.add(nodes.item(i).getTextContent());
                }
            }
        } catch (RuntimeException e) {
            logger.error("Error in processSOAPResponse()\n", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in processSOAPResponse()\n", e);
            throw e;
        }

        return vResult;
    }

    // TODO: for test only. To remove
    public void sendToEAI_FCSCheckLegacyProcess(Context context, String[] args) throws Exception {
        try {
            logger.debug("sendToEAI_FCSCheckLegacyProcess() - Arrays.toString(args) = <" + Arrays.toString(args) + ">");

            ArrayList<Map<String, String>> alLegacies = new ArrayList<Map<String, String>>();
            Map<String, String> mLegacy = null;
            for (int i = 3000000; i < 3000010; i++) {
                mLegacy = new HashMap<String, String>();
                mLegacy.put("matrx_matnr", Integer.toString(i));
                mLegacy.put("werks", "1251");

                alLegacies.add(mLegacy);
            }
            logger.debug("sendToEAI_FCSCheckLegacyProcess() - alLegacies = <" + alLegacies + ">");

            Vector<String> vResult = checkLegaciesMappedInSAP(alLegacies);

            logger.info("vResult=" + vResult);

        } catch (ServiceException e) {
            logger.error("Error in sendToEAI_FCSCheckLegacyProcess()\n" + e.getMessage());
            throw e;
        } catch (MalformedURLException e) {
            logger.error("Error in sendToEAI_FCSCheckLegacyProcess()\n" + e.getMessage());
            throw e;
        }
    }

}
