/**
 * @author Harika Varanasi : SteepGraph
 */
package pss.ecm.notification;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.util.StringList;
import pss.constants.TigerConstants;
import com.matrixone.apps.common.Person;

public class CommonNotificationUtil_mxJPO {

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CommonNotificationUtil_mxJPO.class);

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END
    /**
     * @param context
     * @throws Exception
     */
    public CommonNotificationUtil_mxJPO(Context context) throws Exception {
    }

    /**
     * @param context
     * @param strPageName
     * @return Properties
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    public Properties loadConfigProperties(Context context, String strPageName) throws Exception {
        Properties properties = new Properties();
        try {
            String strPageContent = MqlUtil.mqlCommand(context, "print page $1 select content dump", strPageName);
            InputStream input = new ByteArrayInputStream(strPageContent.getBytes(StandardCharsets.UTF_8));
            properties.load(input);
        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: START
        catch (RuntimeException rex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in loadConfigProperties: ", rex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw rex;
        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: End
        catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in loadConfigProperties: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return properties;
    }

    /**
     * @param context
     * @param strPageName
     * @return String
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("resource")
    public String getFormStyleSheet(Context context, String strPageName) throws Exception {
        String inputStreamString = "";
        try {
            String strPageContent = MqlUtil.mqlCommand(context, "print page $1 select content dump", strPageName);
            InputStream pageInputStream = new ByteArrayInputStream(strPageContent.getBytes(StandardCharsets.UTF_8));

            // Fix for FindBugs issue Method call on instantiation : Harika Varanasi : 21 March 2017: START
            Scanner scannerObj = new Scanner(pageInputStream, "UTF-8");
            inputStreamString = scannerObj.useDelimiter("\\A").next();
            // Fix for FindBugs issue Method call on instantiation : Harika Varanasi : 21 March 2017: END
            // inputStreamString = new Scanner(pageInputStream, "UTF-8").useDelimiter("\\A").next();

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getFormStyleSheet: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return inputStreamString;
    }

    /**
     * @param context
     * @param mlInfoList
     * @param strStyleSheet
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public String getHTMLTwoColumnTable(Context context, MapList mlInfoList, String strStyleSheet) throws Exception {
        // Fix for FindBugs issue Method call on instantiation: Harika Varanasi : 21 March 2017 : Start
        Properties properties;// = new Properties();
        // Fix for FindBugs issue Method call on instantiation : Harika Varanasi : 21 March 2017 : End
        StringBuffer sbHTMLResult = new StringBuffer();
        try {
            properties = loadConfigProperties(context, "PSS_NotificationConfig.properties");
            if (mlInfoList != null && (!mlInfoList.isEmpty())) {

                sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.HeadStyleStart"));
                sbHTMLResult.append(strStyleSheet);
                sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.HeadStyleEnd"));
                sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.FormStart"));
                int mlSize = mlInfoList.size();
                // Fix for FindBugs issue Method call on instantiation : Harika Varanasi : 21 March 2017 : Start
                Map rowMap;// = new HashMap();
                // Fix for FindBugs issue Method call on instantiation : Harika Varanasi : 21 March 2017 : End

                // String strHref = "";// Fix for FindBugs dead local storage issue : Harika Varanasi : 21 March 2017
                String strPurpose = "";
                String strValue = "";
                for (int i = 0; i < mlSize; i++) {
                    rowMap = (Map) mlInfoList.get(i);
                    strPurpose = (String) rowMap.get("purpose");
                    // strHref = (String) rowMap.get("Href");// Fix for FindBugs dead local storage issue : Harika Varanasi : 21 March 2017
                    strValue = (String) rowMap.get("value");
                    if (UIUtil.isNullOrEmpty(strValue)) {
                        strValue = "";
                    }
                    if (UIUtil.isNotNullAndNotEmpty(strPurpose) && "Row".equalsIgnoreCase(strPurpose)) {
                        sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.FieldRowStart"));
                        sbHTMLResult.append((String) rowMap.get("label"));
                        sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.FieldRowMiddle"));
                        sbHTMLResult.append(strValue);
                        sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.FieldRowEnd"));
                    } else if (UIUtil.isNotNullAndNotEmpty(strPurpose) && "SectionHeader".equalsIgnoreCase(strPurpose)) {
                        sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.SectionHeaderStart"));
                        sbHTMLResult.append((String) rowMap.get("label"));
                        sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.SectionHeaderEnd"));
                    } else if (UIUtil.isNotNullAndNotEmpty(strPurpose) && "SectionSubject".equalsIgnoreCase(strPurpose)) {

                        sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.SectionSubjectStart"));
                        sbHTMLResult.append(strValue);
                        sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.SectionSubjectEnd"));

                    }

                }
                sbHTMLResult.append((String) properties.getProperty("Notification.Stylesheet.FormEnd"));

            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getHTMLTwoColumnTable: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return sbHTMLResult.toString();
    }

    /**
     * @param context
     * @param objectIdList
     * @param objectNamesList
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public String getURLString(Context context, StringList objectIdList, StringList objectNamesList, String strBaseURL) throws Exception {
        StringBuffer urlsBuffer = new StringBuffer();
        try {

            if (UIUtil.isNullOrEmpty(strBaseURL)) {
                strBaseURL = MailUtil.getBaseURL(context);
            }

            String strBaseURLSubstring = "";
            if (UIUtil.isNotNullAndNotEmpty(strBaseURL)) {
                int position = strBaseURL.lastIndexOf("/");
                strBaseURLSubstring = strBaseURL.substring(0, position);

            }

            strBaseURLSubstring = strBaseURLSubstring + "/emxTree.jsp";

            if (objectIdList != null && (!objectIdList.isEmpty())) {
                Iterator iteratorIds = objectIdList.iterator();
                Iterator iteratorNames = objectNamesList.iterator();
                String strObjId = "";
                String strObjName = "";
                while (iteratorIds.hasNext()) {
                    strObjId = (String) iteratorIds.next();
                    strObjName = (String) iteratorNames.next();
                    // TIGTK-6074 | 03/04/2017 | Harika Varanasi-SteepGraph - Starts
                    strObjName = strObjName.replace(" ", "&nbsp;");
                    // TIGTK-6074 | 03/04/2017 | Harika Varanasi-SteepGraph - Ends

                    String strNotificationName = PropertyUtil.getGlobalRPEValue(context, "PSS_NOTIFICATION_NAME");

                    if (UIUtil.isNotNullAndNotEmpty(strObjId)) {
                        urlsBuffer.append("<a class=\"object\" href=\"");
                        urlsBuffer.append(strBaseURLSubstring);
                        urlsBuffer.append("?objectId=");
                        urlsBuffer.append(strObjId);
                        urlsBuffer.append("&targetLocation=popup\">");
                        urlsBuffer.append(strObjName);
                        urlsBuffer.append("</a>");
                        if (UIUtil.isNotNullAndNotEmpty(strNotificationName) && (strNotificationName.equals("PSS_COInWorkNotification") || strNotificationName.equals("PSS_MCOInWorkNotification"))) {
                            DomainObject domObj = DomainObject.newInstance(context, strObjId);
                            String strAsigneeName = domObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.name");
                            if (UIUtil.isNotNullAndNotEmpty(strAsigneeName)
                                    && (domObj.isKindOf(context, TigerConstants.TYPE_CHANGEACTION) || domObj.isKindOf(context, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION))) {
                                urlsBuffer.append(" : ");
                                Person person = Person.getPerson(context, strAsigneeName);
                                String strFirstName = person.getInfo(context, Person.SELECT_FIRST_NAME);
                                String strLastName = person.getInfo(context, Person.SELECT_LAST_NAME);
                                strAsigneeName = strFirstName + " " + strLastName;
                                urlsBuffer.append(strAsigneeName);
                            }

                        }
                        urlsBuffer.append("\n");
                    }

                }

            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getURLString: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return urlsBuffer.toString();
    }

    /**
     * @param context
     * @param objectMap
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList transformGenericMapToHTMLList(Context context, Map objectMap, String strBaseURL, LinkedHashMap<String, String> lhmSelectionStore, StringList slHyperLinkLabelKey,
            StringList slHyperLinkLabelKeyIds) throws Exception {
        MapList mlInfoList = new MapList();
        try {
            Map rowMap = null;
            StringList objectIdList = null;
            StringList objectNamesList = null;
            String strLabel = "";
            String strLabelKey = "";
            String strMapKey = "";
            boolean isHrefLink;
            int count = 0;
            for (Map.Entry<String, String> entry : lhmSelectionStore.entrySet()) {

                rowMap = new HashMap();
                isHrefLink = false;
                objectIdList = new StringList();
                objectNamesList = new StringList();
                strLabelKey = (String) entry.getKey();
                strMapKey = (String) entry.getValue();
                strLabel = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Label." + strLabelKey);
                rowMap.put("label", strLabel);

                if ("SectionHeader".equalsIgnoreCase(strMapKey)) {
                    rowMap.put("purpose", "SectionHeader");
                } else if ("SectionSubject".equalsIgnoreCase(strMapKey)) {
                    rowMap.put("purpose", "SectionSubject");
                    rowMap.put("value", "TIGER - " + objectMap.get("name") + " - " + objectMap.get("SectionSubject"));
                } else {
                    rowMap.put("purpose", "Row");
                    if (objectMap.containsKey(strMapKey)) {
                        isHrefLink = (slHyperLinkLabelKey != null && (!slHyperLinkLabelKey.isEmpty()) && slHyperLinkLabelKey.contains(strLabelKey));
                        try {
                            objectNamesList.addElement((String) objectMap.get(strMapKey));
                            if (isHrefLink) {
                                objectIdList.addElement((String) objectMap.get(slHyperLinkLabelKeyIds.get(slHyperLinkLabelKey.indexOf(strLabelKey))));
                            }
                        } catch (ClassCastException cse) {
                            objectNamesList = (StringList) objectMap.get(strMapKey);
                            if (isHrefLink) {
                                objectIdList = (StringList) objectMap.get(slHyperLinkLabelKeyIds.get(slHyperLinkLabelKey.indexOf(strLabelKey)));
                            }
                        }
                        if (isHrefLink) {
                            rowMap.put("value", getURLString(context, objectIdList, objectNamesList, strBaseURL));
                        } else {
                            String strValue = "";
                            if (objectNamesList != null && (!objectNamesList.isEmpty())) {
                                strValue = FrameworkUtil.join(objectNamesList, "\n");
                            }

                            // PCM : TIGTK-7745 : 21/08/2017 : AB : START
                            if ("Parallel_Track_Comment".equalsIgnoreCase(strLabelKey) || "Parallel_Track".equalsIgnoreCase(strLabelKey)) {
                                if (strValue.equalsIgnoreCase("No") || strValue.equalsIgnoreCase(DomainConstants.EMPTY_STRING)) {
                                    rowMap.remove("label", strLabel);
                                    rowMap.remove("purpose", "Row");
                                } else {
                                    rowMap.put("value", strValue);
                                }
                            } // TIGTK-7580:START
                            else if ("Comment".equalsIgnoreCase(strLabelKey)) {
                                if (strValue.equalsIgnoreCase(DomainConstants.EMPTY_STRING)) {
                                    rowMap.remove("label", strLabel);
                                    rowMap.remove("purpose", "Row");
                                } else {
                                    rowMap.put("value", strValue);
                                }
                            }
                            // PCM : TIGTK-10768 : 20/11/2017 : TS : START
                            else if ("MCA_Creator".equalsIgnoreCase(strLabelKey) && TigerConstants.PERSON_USER_AGENT.equals(strValue)) {

                                String strMCAId = (String) objectMap.get("id");
                                DomainObject domMCA = DomainObject.newInstance(context, strMCAId);
                                String strOwner = domMCA.getInfo(context, DomainConstants.SELECT_OWNER);

                                rowMap.put("value", strOwner);
                                // PCM : TIGTK-10768 : 20/11/2017 : TS : END
                            } else {
                                rowMap.put("value", strValue);
                            }
                            // TIGTK-7580 : END
                        }
                    }
                }

                if (!rowMap.isEmpty()) {
                    mlInfoList.add(count, rowMap);
                    count++;
                }
                // PCM : TIGTK-7745 : 21/08/2017 : AB : END
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in transformGenericMapToHTMLList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mlInfoList;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    /**
     * getStringListFromMap
     * @param context
     * @param objectMap
     * @param strSelects
     * @return StringList
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    public StringList getStringListFromMap(Context context, Map objectMap, String strSelects) throws Exception {
        StringList slList = new StringList();
        try {

            if (objectMap != null && objectMap.containsKey(strSelects)) {
                Object objSelects = objectMap.get(strSelects);
                if (objSelects != null) {
                    try {
                        slList.addElement((String) objSelects);
                    } catch (ClassCastException cse) {
                        slList.addAll((StringList) objSelects);
                    }
                }

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getStringListFromMap: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return slList;
    }
}