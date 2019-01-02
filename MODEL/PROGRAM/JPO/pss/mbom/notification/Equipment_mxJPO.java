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

public class Equipment_mxJPO {

    // TIGTK-5405 - 06-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Equipment_mxJPO.class);
    // TIGTK-5405 - 06-04-2017 - VB - END

    public Equipment_mxJPO() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * this method is used to send notification to the user,whenever the equipment request is accepted
     * @param context
     * @param args
     * @return message string
     * @throws Exception
     *             Exception appears, if error occurred
     */
    public String getEquipmentRequestAcceptedMessageText(Context context, String[] args) throws Exception {

        final String strMessageText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Accepted");
        final String strPlantText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Plant");
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
            String strObjectId = (String) params.get(DomainConstants.SELECT_ID);
            DomainObject domEquipmentRequest = DomainObject.newInstance(context, strObjectId);
            String strDescription = domEquipmentRequest.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
            String strOriginator = domEquipmentRequest.getInfo(context, DomainConstants.SELECT_ORIGINATOR);
            String strRequestNumber = domEquipmentRequest.getInfo(context, DomainConstants.SELECT_NAME);
            String URL = "../common/emxTree.jsp?objectId=" + strObjectId;
            MapList mList = domEquipmentRequest.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_EQUIPMENT_REQUEST, // relationship pattern
                    TigerConstants.TYPE_PSS_PLANT, // object pattern
                    objectSelects, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
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
                    String strPlantName = (String) programMap.get(DomainConstants.SELECT_NAME);
                    strMsg.append(strMessageText + "</br>");
                    strMsg.append(strPlantText + "-" + strPlantName + "</br>");
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
            logger.error("Error in getEquipmentRequestAcceptedMessageText: ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
        }
        return strMsg.toString();

    }

    /**
     * this method is used to send notification to the user,whenever the equipment request is rejected
     * @param context
     * @param args
     * @return message string
     * @throws Exception
     *             Exception appears, if error occurred
     */
    public String getEquipmentRequestRejectedMessageText(Context context, String[] args) throws Exception {

        final String strMessageText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Rejected");
        final String strPlantText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Plant");
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
            String strObjectId = (String) params.get(DomainConstants.SELECT_ID);
            DomainObject domEquipmentRequest = DomainObject.newInstance(context, strObjectId);
            String strDescription = domEquipmentRequest.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
            String strOriginator = domEquipmentRequest.getInfo(context, DomainConstants.SELECT_ORIGINATOR);
            String strRequestNumber = domEquipmentRequest.getInfo(context, DomainConstants.SELECT_NAME);
            String URL = "../common/emxTree.jsp?objectId=" + strObjectId;
            MapList mList = domEquipmentRequest.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_EQUIPMENT_REQUEST, // relationship pattern
                    TigerConstants.TYPE_PSS_PLANT, // object pattern
                    objectSelects, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
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
                    String strPlantName = (String) programMap.get(DomainConstants.SELECT_NAME);
                    strMsg.append(strMessageText + "</br>");
                    strMsg.append(strPlantText + "-" + strPlantName + "</br>");
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
            logger.error("Error in getEquipmentRequestRejectedMessageText : ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
        }
        return strMsg.toString();

    }

    /**
     * this method is used to send notification to the user,whenever the equipment request is completed
     * @param context
     * @param args
     * @return message string
     * @throws Exception
     *             Exception appears, if error occurred
     */
    public String getEquipmentRequestCompletedMessageText(Context context, String[] args) throws Exception {

        final String strMessageText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Completed");
        final String strPlantText = EnoviaResourceBundle.getProperty(context, TigerConstants.PROPERTIES_EMXFRCMBOMCENTRALSTRINGRESOURCE, context.getLocale(),
                "PSS_FRCMBOMCentral.Notification.Message.Plant");
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
            String strObjectId = (String) params.get(DomainConstants.SELECT_ID);
            DomainObject domEquipmentRequest = DomainObject.newInstance(context, strObjectId);
            String strDescription = domEquipmentRequest.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
            String strOriginator = domEquipmentRequest.getInfo(context, DomainConstants.SELECT_ORIGINATOR);
            String strRequestNumber = domEquipmentRequest.getInfo(context, DomainConstants.SELECT_NAME);
            String URL = "../common/emxTree.jsp?objectId=" + strObjectId;
            MapList mList = domEquipmentRequest.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_EQUIPMENT_REQUEST, // relationship pattern
                    TigerConstants.TYPE_PSS_PLANT, // object pattern
                    objectSelects, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
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
                    String strPlantName = (String) programMap.get(DomainConstants.SELECT_NAME);
                    strMsg.append(strMessageText + "</br>");
                    strMsg.append(strPlantText + "-" + strPlantName + "</br>");
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
            logger.error("Error in getEquipmentRequestCompletedMessageText : ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
        }
        return strMsg.toString();

    }

}
