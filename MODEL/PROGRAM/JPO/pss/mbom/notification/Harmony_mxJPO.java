package pss.mbom.notification;

import java.util.HashMap;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Harmony_mxJPO {

    // TIGTK-5405 - 06-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Harmony_mxJPO.class);
    // TIGTK-5405 - 06-04-2017 - VB - END

    public Harmony_mxJPO() {
        super();
        // TODO Auto-generated constructor stub
    }

    public String getHarmonyRequestAcceptedMessageText(Context context, String[] args) throws Exception {

        final String strMessageText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Accepted");
        final String strProgramText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Program");
        final String strRequestText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Request");
        final String strDescriptionText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Description");
        final String strOriginatorText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Originator");
        StringBuffer strMsg = new StringBuffer();
        strMsg.append("<html><body>");
        try {
            StringList objectSelects = new StringList(DomainConstants.SELECT_NAME);
            HashMap params = (HashMap) JPO.unpackArgs(args);
            // String baseURL = (String) params.get("baseURL");
            // int position = baseURL.lastIndexOf("/");
            String strObjectId = (String) params.get(DomainConstants.SELECT_ID);
            DomainObject dom = DomainObject.newInstance(context, strObjectId);
            String strDescription = dom.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
            String strOriginator = dom.getInfo(context, DomainConstants.SELECT_ORIGINATOR);
            String strRequestNumber = dom.getInfo(context, DomainConstants.SELECT_NAME);
            String URL = "../common/emxTree.jsp?objectId=" + strObjectId;

            MapList mList = dom.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSSCONNECT_HARMONY_REQUEST, // relationship pattern
                    TigerConstants.TYPE_PSS_PROGRAMPROJECT, // object pattern
                    objectSelects, // object selects
                    null, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);

            if (!mList.isEmpty()) {
                for (int j = 0; j < mList.size(); j++) {
                    Map programMap = (Map) mList.get(j);
                    String strProgramName = (String) programMap.get(DomainConstants.SELECT_NAME);
                    strMsg.append(strMessageText + "</br>");
                    strMsg.append(strProgramText + "-" + strProgramName + "</br>");
                    strMsg.append(strRequestText + "- <a href =" + URL + ">" + strRequestNumber + "</a></br>");
                    strMsg.append(strDescriptionText + "-" + strDescription + "</br>");
                    strMsg.append(strOriginatorText + "-" + strOriginator + "</br></body></html>");
                }
            } else {

                // String check = "../common/emxTree.jsp?objectId=" + strObjectId;
                strMsg.append(strMessageText + "</br>");
                strMsg.append(strRequestText + "- <a href =" + URL + ">" + strRequestNumber + "</a></br>");
                strMsg.append(strDescriptionText + "-" + strDescription + "</br>");
                strMsg.append(strOriginatorText + "-" + strOriginator + "</br></body></html>");
            }

        } catch (Exception e) {
            // TIGTK-5405 - 06-04-2017 - VB - START
            logger.error("Error in getHarmonyRequestAcceptedMessageText: ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
        }
        return strMsg.toString();

    }

    public String getHarmonyRequestRejectedMessageText(Context context, String[] args) throws Exception {

        final String strMessageText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Rejected");
        final String strProgramText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Program");
        final String strRequestText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Request");
        final String strDescriptionText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Description");
        final String strOriginatorText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Originator");
        StringBuffer strMsg = new StringBuffer();
        try {

            StringList objectSelects = new StringList(DomainConstants.SELECT_NAME);
            HashMap params = (HashMap) JPO.unpackArgs(args);
            String strObjectId = (String) params.get(DomainConstants.SELECT_ID);
            DomainObject dom = DomainObject.newInstance(context, strObjectId);
            String strDescription = dom.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
            String strOriginator = dom.getInfo(context, DomainConstants.SELECT_ORIGINATOR);
            String strRequestNumber = dom.getInfo(context, DomainConstants.SELECT_NAME);
            String URL = "../common/emxTree.jsp?objectId=" + strObjectId;

            MapList mList = dom.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSSCONNECT_HARMONY_REQUEST, // relationship pattern
                    TigerConstants.TYPE_PSS_PROGRAMPROJECT, // object pattern
                    objectSelects, // object selects
                    null, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);

            if (!mList.isEmpty()) {
                for (int j = 0; j < mList.size(); j++) {
                    Map programMap = (Map) mList.get(j);
                    String strProgramName = (String) programMap.get(DomainConstants.SELECT_NAME);
                    strMsg.append(strMessageText + "</br>");
                    strMsg.append(strProgramText + "-" + strProgramName + "</br>");
                    strMsg.append(strRequestText + "- <a href =" + URL + ">" + strRequestNumber + "</a></br>");
                    strMsg.append(strDescriptionText + "-" + strDescription + "</br>");
                    strMsg.append(strOriginatorText + "-" + strOriginator + "</br></body></html>");

                }
            } else {
                strMsg.append(strMessageText + "</br>");
                strMsg.append(strRequestText + "- <a href =" + URL + ">" + strRequestNumber + "</a></br>");
                strMsg.append(strDescriptionText + "-" + strDescription + "</br>");
                strMsg.append(strOriginatorText + "-" + strOriginator + "</br></body></html>");
            }

        } catch (Exception e) {
            // TIGTK-5405 - 06-04-2017 - VB - START
            logger.error("Error in getHarmonyRequestRejectedMessageText: ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
        }
        return strMsg.toString();

    }

    public String getHarmonyRequestCompletedMessageText(Context context, String[] args) throws Exception {

        final String strMessageText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Completed");
        final String strProgramText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Program");
        final String strRequestText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Request");
        final String strDescriptionText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Description");
        final String strOriginatorText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Originator");
        StringBuffer strMsg = new StringBuffer();
        try {

            StringList objectSelects = new StringList(DomainConstants.SELECT_NAME);
            HashMap params = (HashMap) JPO.unpackArgs(args);
            String strObjectId = (String) params.get(DomainConstants.SELECT_ID);
            DomainObject dom = DomainObject.newInstance(context, strObjectId);
            String strDescription = dom.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
            String strOriginator = dom.getInfo(context, DomainConstants.SELECT_ORIGINATOR);
            String strRequestNumber = dom.getInfo(context, DomainConstants.SELECT_NAME);
            String URL = "../common/emxTree.jsp?objectId=" + strObjectId;

            MapList mList = dom.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSSCONNECT_HARMONY_REQUEST, // relationship pattern
                    TigerConstants.TYPE_PSS_PROGRAMPROJECT, // object pattern
                    objectSelects, // object selects
                    null, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);

            if (!mList.isEmpty()) {
                for (int j = 0; j < mList.size(); j++) {
                    Map programMap = (Map) mList.get(j);
                    String strProgramName = (String) programMap.get(DomainConstants.SELECT_NAME);
                    strMsg.append(strMessageText + "</br>");
                    strMsg.append(strProgramText + "-" + strProgramName + "</br>");
                    strMsg.append(strRequestText + "- <a href =" + URL + ">" + strRequestNumber + "</a></br>");
                    strMsg.append(strDescriptionText + "-" + strDescription + "</br>");
                    strMsg.append(strOriginatorText + "-" + strOriginator + "</br></body></html>");
                }
            } else {

                strMsg.append(strMessageText + "</br>");
                strMsg.append(strRequestText + "- <a href =" + URL + ">" + strRequestNumber + "</a></br>");
                strMsg.append(strDescriptionText + "-" + strDescription + "</br>");
                strMsg.append(strOriginatorText + "-" + strOriginator + "</br></body></html>");
            }

        } catch (Exception e) {
            // TIGTK-5405 - 06-04-2017 - VB - START
            logger.error("Error in getHarmonyRequestCompletedMessageText: ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
        }
        return strMsg.toString();

    }
}
