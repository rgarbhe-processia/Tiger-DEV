package pss.mbom.publish;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.QueueViewMBean;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.jsystem.util.Encoding;

import dsis.com.activemq.utilities.JMXUtilMethods;
import dsis.com.jaxb.Message;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MatrixClassLoader;

public class PublishDetails_mxJPO {

    // TIGTK-5405 - 07-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PublishDetails_mxJPO.class);

    // TIGTK-5405 - 07-04-2017 - VB - END
    /**
     * This method is used to create map to populate the columns.
     * @param strPartReference
     * @param stXPDMFile
     * @param paramString
     * @param startTimeAndDate
     * @param strErrorMessage
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public Map getQueuesBasedOnQueueName(Context context, String strPartReference, String stXPDMFile, String paramString, String startTimeAndDate, String strErrorMessage, String strEndTime,
            String strUser) throws Exception {
        Map mTempMap = new HashMap();
        try {
            String strPublishSuccess = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Status.PublishSuccess");
            String strPublishFail = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Status.PublishFailed");
            String strPublishProcessing = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Status.PublishProcessing");
            String strXPDMFileNotFound = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.XPDMFileError");
            File localFile = new File(stXPDMFile);
            if (paramString.equals(strPublishSuccess)) {
                paramString = "Success";
            } else if (paramString.equals(strPublishProcessing)) {
                paramString = "In Process";
            } else if (paramString.equals(strPublishFail)) {
                paramString = "Failed";
            }
            if (localFile.exists()) {
                mTempMap.put("Name", strPartReference);
                mTempMap.put("TimeCreated", startTimeAndDate);
                mTempMap.put("TimeModified", strEndTime);
                mTempMap.put("Owner", strUser);
                mTempMap.put("Status", paramString);
                mTempMap.put("Error", strErrorMessage);
            } else {
                mTempMap.put("Name", strPartReference);
                mTempMap.put("TimeCreated", startTimeAndDate);
                mTempMap.put("TimeModified", strEndTime);
                mTempMap.put("Owner", strUser);
                mTempMap.put("Status", paramString);
                mTempMap.put("Error", strXPDMFileNotFound);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getQueuesBasedOnQueueName: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw e;
        }
        return mTempMap;
    }

    public Map getQueuesBasedOnQueueName(Context context, String strPartReference, String stXPDMFile, String paramString, String startTimeAndDate, String strErrorMessage, String strEndTime,
            String strUser, String sServerURL) throws Exception {

        try {
            Map tempMap = getQueuesBasedOnQueueName(context, strPartReference, stXPDMFile, paramString, startTimeAndDate, strErrorMessage, strEndTime, strUser);
            tempMap.put("ServerURL", sServerURL);
            return tempMap;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getQueuesBasedOnQueueName: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw e;
        }

    }

    /**
     * This method is to fetch all queues from active mq server.
     * @param context
     * @param paramString
     * @return
     */
    public MapList getNextMessageCompositeData(Context context, String paramString) throws Exception {

        MapList mQueueDetailsMapList = new MapList();
        JMXServiceURL url;
        JMXConnector jmxc;
        String strPublishFail = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Status.PublishFailed");
        String strXPDMFileName = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.File.Publish");
        String strXPDMLogFileName = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.File.Publish.error");
        String strErrorMessageMappingContext = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.PublishError1");
        String strErrorMessageConnection = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.PublishError2");
        String strPathOfXPDMDirectory = EnoviaResourceBundle.getProperty(context, "extractor", context.getLocale(), "EVENT_WRITE_LOCATION");
        String strDefaultErrorMessage = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.PublishErrorDefault");
        String strErrorMessageFailedTranfer = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.PublishError3");

        // TIGTK-9441:Rutuja Ekatpure:22/8/2017:Start
        // String strJMSURLs = EnoviaResourceBundle.getProperty(context, "extractor", context.getLocale(), "JMS_SERVICE_URL");
        String strJMSURLs = EnoviaResourceBundle.getProperty(context, "JMS_SERVICE_URL");
        String strServerError = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.ServerError");
        String strMessageUnknown = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.ErrorMessage.Unknown");
        List<String> listJMSURLs = Arrays.asList(strJMSURLs.split(","));
        Iterator<String> itJMSURL = listJMSURLs.iterator();

        while (itJMSURL.hasNext()) {
            String strJMSURL = itJMSURL.next();
            System.out.println("New server : " + strJMSURL);
            try {
                String strActiveMQService = "service:jmx:rmi:///jndi/rmi://" + strJMSURL + "/jmxrmi";
                url = new JMXServiceURL(strActiveMQService);
                jmxc = JMXConnectorFactory.connect(url);
                MBeanServerConnection localMBeanServerConnection = jmxc.getMBeanServerConnection();
                ObjectName localObjectName = new ObjectName("org.apache.activemq:brokerName=localhost,type=Broker");
                BrokerViewMBean localBrokerViewMBean = (BrokerViewMBean) MBeanServerInvocationHandler.newProxyInstance(localMBeanServerConnection, localObjectName, BrokerViewMBean.class, true);
                for (ObjectName localObjectName2 : localBrokerViewMBean.getQueues()) {
                    QueueViewMBean localQueueViewMBean = (QueueViewMBean) MBeanServerInvocationHandler.newProxyInstance(localMBeanServerConnection, localObjectName2, QueueViewMBean.class, true);
                    if (!localQueueViewMBean.getName().equals(paramString))
                        continue;
                    CompositeData[] arrayOfCompositeData = localQueueViewMBean.browse();
                    if (arrayOfCompositeData.length <= 0) {
                        continue;
                    }

                    for (int i = 0; i < arrayOfCompositeData.length; i++) {

                        String strText = (String) arrayOfCompositeData[i].get("Text");
                        Date startDateAndTime = (Date) arrayOfCompositeData[i].get("JMSTimestamp");
                        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                        String strStartTime = dateFormat.format(startDateAndTime);
                        Message localMessage = unmarshallMessage(strText);
                        String strPartReference = "";
                        String strErrorMessage = "";
                        String strUser = "";
                        if (strText.contains("<MailID>")) {
                            strUser = org.apache.commons.lang3.StringUtils.substringBetween(strText, "<MailID>", "</MailID>");
                        }
                        String strXPDMFileDirectory = localMessage.getXPGReportDir();
                        String strEndTime = "";
                        if (UIUtil.isNotNullAndNotEmpty(strXPDMFileDirectory)) {
                            String[] strSplitXPDMDirectoryPathBeforeXPDM = strXPDMFileDirectory.split("xpdm");
                            String strAppendToDirectoryPath = FrameworkUtil.Replace(strSplitXPDMDirectoryPathBeforeXPDM[1], "\\", "/");
                            StringBuilder sbXPDMHtmlFileDirectory = new StringBuilder();
                            sbXPDMHtmlFileDirectory.append(strPathOfXPDMDirectory);
                            sbXPDMHtmlFileDirectory.append(strAppendToDirectoryPath);
                            sbXPDMHtmlFileDirectory.append(strXPDMLogFileName);

                            File fXPDMHtmlFile = new File(sbXPDMHtmlFileDirectory.toString());

                            if (fXPDMHtmlFile.exists()) {
                                DateFormat dateFormatXPDMHtmlFile = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                                strEndTime = dateFormatXPDMHtmlFile.format(fXPDMHtmlFile.lastModified());
                            }

                            if (paramString.equalsIgnoreCase(strPublishFail)) {

                                StringBuilder sbContentBuilder = new StringBuilder();
                                FileInputStream fHtmlFileInputStream = null;
                                BufferedReader bfParseHTMLContent = null;
                                InputStreamReader inputStreamReader = null;
                                try {
                                    fHtmlFileInputStream = new FileInputStream(sbXPDMHtmlFileDirectory.toString());
                                    inputStreamReader = new InputStreamReader(fHtmlFileInputStream, "UTF-8");
                                    bfParseHTMLContent = new BufferedReader(inputStreamReader);
                                    String str;
                                    while ((str = bfParseHTMLContent.readLine()) != null) {
                                        sbContentBuilder.append(str);
                                    }
                                } catch (IOException e) {
                                    // TIGTK-5405 - 07-04-2017 - VB - START
                                    logger.error("Error in getNextMessageCompositeData: ", e);
                                    // TIGTK-5405 - 07-04-2017 - VB - END
                                } catch (Exception e) {
                                    // TIGTK-5405 - 07-04-2017 - VB - START
                                    logger.error("Error in getNextMessageCompositeData: ", e);
                                    // TIGTK-5405 - 07-04-2017 - VB - END
                                } finally {
                                    if (bfParseHTMLContent != null) {
                                        bfParseHTMLContent.close();
                                    }
                                    if (inputStreamReader != null) {
                                        inputStreamReader.close();
                                    }
                                    if (fHtmlFileInputStream != null) {
                                        fHtmlFileInputStream.close();
                                    }
                                }

                                String strContent = sbContentBuilder.toString();
                                if (strContent.contains("Unable to find a mapping context associated to this XPDM site ID")) {
                                    strErrorMessage = strErrorMessageMappingContext;
                                } else if (strContent.contains("The connection to 3DEXPERIENCE Platform has failed")) {
                                    strErrorMessage = strErrorMessageConnection;
                                } else if (strContent.contains("Failed to transfer the PLM Components to the 3DEXPERIENCE Platform server Process ends with Error ReturnCode")) {
                                    strErrorMessage = strErrorMessageFailedTranfer;
                                } else {
                                    strErrorMessage = strDefaultErrorMessage;
                                }
                            }
                        }
                        String strPartTypeNameRevision = "";
                        StringBuilder strXPDMFilePath = new StringBuilder();
                        strXPDMFilePath.append(strPathOfXPDMDirectory);
                        String stXPDMFile = localMessage.getXPDMXMLFile();
                        if (UIUtil.isNotNullAndNotEmpty(stXPDMFile)) {
                            String[] strSplitPathBeforeXPDM = stXPDMFile.split("xpdm");

                            if (strSplitPathBeforeXPDM[1].contains("Part")) {
                                strPartReference = strSplitPathBeforeXPDM[1].substring(1, 16);
                                strPartTypeNameRevision = strPartReference.replace('_', ' ');
                            }
                            String strAppendToPath = FrameworkUtil.Replace(strSplitPathBeforeXPDM[1], "\\", "/");
                            strXPDMFilePath.append(strAppendToPath);
                            strXPDMFilePath.append(strXPDMFileName);
                        }

                        Map mQueueDetailsTempMap = getQueuesBasedOnQueueName(context, strPartTypeNameRevision, strXPDMFilePath.toString(), paramString, strStartTime, strErrorMessage, strEndTime,
                                strUser, strJMSURL);
                        mQueueDetailsMapList.add(mQueueDetailsTempMap);
                    }
                }
            } catch (Exception e) {
                if ("IMPORT_SUCCESS".equals(paramString)) {
                    Map<String, String> mQueueDetailsTempMap = new HashMap<String, String>();
                    mQueueDetailsTempMap.put("Name", strMessageUnknown);
                    mQueueDetailsTempMap.put("TimeCreated", "");
                    mQueueDetailsTempMap.put("TimeModified", "");
                    mQueueDetailsTempMap.put("Owner", "");
                    mQueueDetailsTempMap.put("Status", "Server unreachable");
                    mQueueDetailsTempMap.put("Error", strServerError);
                    mQueueDetailsTempMap.put("ServerURL", strJMSURL);

                    mQueueDetailsMapList.add(0, mQueueDetailsTempMap);
                }

                // TIGTK-5405 - 07-04-2017 - VB - START
                logger.error("Error in getNextMessageCompositeData: ", e);
                // TIGTK-5405 - 07-04-2017 - VB - END
            }
        }

        return mQueueDetailsMapList;
    }

    /**
     * Get all queues which are success, failed or in processing state.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public MapList getPublishQueues(Context context, String[] args) throws Exception {
        MapList mlPartListWithPublishDetails = new MapList();
        try {
            String strPublishSuccess = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Status.PublishSuccess");
            String strPublishFail = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Status.PublishFailed");
            String strPublishProcessing = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.Status.PublishProcessing");

            MapList mpImportSuccess = getNextMessageCompositeData(context, strPublishSuccess);
            MapList mpImportFail = getNextMessageCompositeData(context, strPublishFail);
            MapList mpImportProcessing = getNextMessageCompositeData(context, strPublishProcessing);
            mlPartListWithPublishDetails.addAll(mpImportSuccess);
            mlPartListWithPublishDetails.addAll(mpImportFail);
            mlPartListWithPublishDetails.addAll(mpImportProcessing);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getPublishQueues: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw e;
        }
        return mlPartListWithPublishDetails;
    }

    /**
     * Populate column of User.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static Vector getUserDetailsForQueuesData(Context context, String[] args) throws Exception {
        Vector vecResult = new Vector();
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            for (int i = 0; i < objectList.size(); i++) {
                Map mObjectMap = (Map) objectList.get(i);
                String strResourceId = (String) mObjectMap.get("Owner");
                vecResult.add(strResourceId);

            }
            // vecResult.add(strResourceId);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getUserDetailsForQueuesData: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw e;
        }
        return vecResult;
    }

    /**
     * Populate column of Part Reference.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static Vector getPartReferenceForQueuesData(Context context, String[] args) throws Exception {
        Vector vecResult = new Vector();
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            for (int i = 0; i < objectList.size(); i++) {
                Map mObjectMap = (Map) objectList.get(i);
                String strResourceId = (String) mObjectMap.get("Name");
                vecResult.add(strResourceId);

            }
            // vecResult.add(strResourceId);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getPartReferenceForQueuesData: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw e;
        }
        return vecResult;
    }

    /**
     * Populate column of start time.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static Vector getPublishStartDateTimeForQueuesData(Context context, String[] args) throws Exception {
        Vector vecResult = new Vector();
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            for (int i = 0; i < objectList.size(); i++) {
                Map mObjectMap = (Map) objectList.get(i);
                String strResourceId = (String) mObjectMap.get("TimeCreated");
                vecResult.add(strResourceId);

            }
            // vecResult.add(strResourceId);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getPublishStartDateTimeForQueuesData: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw e;
        }
        return vecResult;
    }

    /**
     * Populate column of EndTime.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static Vector getPublishEndDateTimeForQueuesData(Context context, String[] args) throws Exception {
        Vector vecResult = new Vector();
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            for (int i = 0; i < objectList.size(); i++) {
                Map mObjectMap = (Map) objectList.get(i);
                String strResourceId = (String) mObjectMap.get("TimeModified");
                vecResult.add(strResourceId);

            }
            // vecResult.add(strResourceId);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getPublishEndDateTimeForQueuesData: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw e;
        }
        return vecResult;
    }

    /**
     * Populate column of Publish Status.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static Vector getPublishStatusQueuesData(Context context, String[] args) throws Exception {
        Vector vecResult = new Vector();
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            for (int i = 0; i < objectList.size(); i++) {
                Map mObjectMap = (Map) objectList.get(i);
                String strResourceId = (String) mObjectMap.get("Status");
                vecResult.add(strResourceId);

            }
            // vecResult.add(strResourceId);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getPublishStatusQueuesData: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw e;
        }
        return vecResult;
    }

    /**
     * Populate column of Error logs.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static Vector getPublishErrorLogs(Context context, String[] args) throws Exception {
        Vector vecResult = new Vector();
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            for (int i = 0; i < objectList.size(); i++) {
                Map mObjectMap = (Map) objectList.get(i);
                String strResourceId = (String) mObjectMap.get("Error");
                vecResult.add(strResourceId);

            }
            // vecResult.add(strResourceId);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getPublishErrorLogs: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw e;
        }
        return vecResult;
    }

    /**
     * Populate column of Server URLs.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public static Vector getPublishServerURL(Context context, String[] args) throws Exception {
        Vector vecResult = new Vector();
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            for (int i = 0; i < objectList.size(); i++) {
                Map mObjectMap = (Map) objectList.get(i);
                String strResourceId = (String) mObjectMap.get("ServerURL");
                vecResult.add(strResourceId);
            }
        } catch (Exception e) {
            logger.error("Error in getPublishServerURL: ", e);
            throw e;
        }
        return vecResult;
    }

    public Message unmarshallMessage(String paramString) {
        Message localMessage = null;
        try {
            JAXBContext localJAXBContext = JAXBContext.newInstance(new Class[] { Message.class });
            Unmarshaller localUnmarshaller = localJAXBContext.createUnmarshaller();
            ByteArrayInputStream localByteArrayInputStream = new ByteArrayInputStream(paramString.getBytes(StandardCharsets.UTF_8));
            localMessage = (Message) localUnmarshaller.unmarshal(localByteArrayInputStream);
        } catch (JAXBException localJAXBException) {
            localJAXBException.printStackTrace();
        }
        return localMessage;
    }
}