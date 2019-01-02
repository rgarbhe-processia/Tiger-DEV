
/*
 ** ${CLASS:enoECMChangeOrder}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.State;
import matrix.db.StateList;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

/**
 * The <code>enoECMChangeOrder</code> class contains code for the "Change Order" business type.
 * @version ECM R215 - # Copyright (c) 1992-2015 Dassault Systemes.
 */
public class enoECMChangeOrder_mxJPO extends enoECMChangeOrderBase_mxJPO {
    private ChangeUtil changeUtil = null;

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @throws Exception
     *             if the operation fails.
     * @since ECM R215.
     */

    public enoECMChangeOrder_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * PCM : TIGTK-7745 : 11/08/2017 : AB Trigger Method to send notification after functionality Change Owner.
     * @author
     * @param context
     *            - the eMatrix <code>Context</code> object
     * @param args
     *            - Object Id of Change
     * @return int - Returns integer status code 0 - in case the trigger is successful
     * @throws Exception
     *             if the operation fails
     * @since ECM R211
     */
    public int notifyOwner(Context context, String[] args) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            String strObjectId = args[0];
            String subjectKey = args[1];
            String messageKey = args[2];
            String propertyKey = args[3];
            String kindOfOwner = args[4];

            DomainObject domChange = newInstance(context, strObjectId);
            StringList objSelects = new StringList(SELECT_OWNER);
            ;
            objSelects.addElement(SELECT_ORIGINATOR);
            objSelects.addElement(SELECT_TYPE);

            Map objMap = domChange.getInfo(context, objSelects);
            String strOwner = (String) objMap.get(SELECT_OWNER);
            String strOriginator = (String) objMap.get(SELECT_ORIGINATOR);
            String strType = (String) objMap.get(SELECT_TYPE);

            // PCM : TIGTK-7745 : 11/08/2017 : AB : START
            // This is used to skip the notification (New Change Action has been Assigned)

            if (!(ChangeConstants.TYPE_CHANGE_ACTION.equalsIgnoreCase(strType) || TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(strType))) {
                String strLanguage = context.getSession().getLanguage();

                StringList ccList = new StringList();
                StringList bccList = new StringList();
                StringList toList = new StringList();
                StringList lstAttachments = new StringList();

                toList.add(strOwner);
                lstAttachments.add(strObjectId);

                // Do not send a message if the current owner is person_UserAgent
                if (ChangeConstants.USER_AGENT.equalsIgnoreCase(strOwner) || kindOfOwner.equalsIgnoreCase(DomainConstants.TYPE_ORGANIZATION) || kindOfOwner.equalsIgnoreCase("Project"))
                    return 0;

                // Sending mail to the owner
                emxNotificationUtilBase_mxJPO.sendNotification(context, strObjectId, toList, ccList, bccList, subjectKey, messageKey, lstAttachments, propertyKey, null, null,
                        "enoECMChangeOrder:getTransferComments");
            }
            // PCM : TIGTK-7745 : 11/08/2017 : AB : END

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            throw new FrameworkException((String) ex.getMessage());
        }
        return 0;
    }
    /*
     * // TIGTK-17751:29-10-2018 : START public int checkPrerequisitesBeforeComplete(Context context, String[] args) throws Exception {
     * 
     * String objectId = args[0]; String stateName = args[1]; String typeName = args[2]; String relName = args[3]; String propertyKey = args[4]; String suiteKey = args[5];
     * 
     * MapList mListPrerequisites = null; MapList mListPartPrerequisites = null; boolean hasPrerequisites = false; boolean hasPartobsolute = false; String currentStateOfPrereq = ""; String strPartObj
     * = null; String strRequestedChange = null; String relWhereClause = "(attribute[" + ChangeConstants.ATTRIBUTE_PREREQUISITE_TYPE + "]== Mandatory)";
     * 
     * try {
     * 
     * DomainObject domChangeOrder = newInstance(context, objectId); StateList stateResultList = domChangeOrder.getStates(context); StringList stateList = new StringList(); for (Object object :
     * stateResultList) { stateList.addElement(((State) object).getName()); } setId(objectId); StringList sList = new StringList(2); sList.addElement(SELECT_ID); sList.addElement(SELECT_CURRENT);
     * 
     * if (TigerConstants.STATE_PSS_CHANGEORDER_COMPLETE.equalsIgnoreCase(stateName)) { Map mapPart = null; StringBuffer relPattern = new
     * StringBuffer(ChangeConstants.RELATIONSHIP_CHANGE_ACTION).append(",").append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM); StringBuffer typePattern = new
     * StringBuffer(TigerConstants.TYPE_CHANGEACTION).append(",").append(TigerConstants.TYPE_PART); MapList list = getRelatedObjects(context, relPattern.toString(), typePattern.toString(), sList, new
     * StringList(new StringBuilder().append("attribute[").append(ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE).append("]").toString()), false, true, (short) 0, EMPTY_STRING, EMPTY_STRING, (short) 0,
     * new Pattern(TigerConstants.TYPE_PART), null, null); int coAffectedItemSize = list.size(); for (int i = 0; i < coAffectedItemSize; i++) { mapPart = (Map) list.get(i); strPartObj = (String)
     * mapPart.get(SELECT_ID); strRequestedChange = (String) mapPart.get(new StringBuilder().append("attribute[").append(ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE).append("]").toString()); if
     * (ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strRequestedChange)) { mListPartPrerequisites = new DomainObject(strPartObj).getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM,
     * TigerConstants.TYPE_PART, sList, null, true, false, (short) 1, EMPTY_STRING, EMPTY_STRING, (short) 0); Iterator partIterator = mListPartPrerequisites.iterator(); while (partIterator.hasNext())
     * { currentStateOfPrereq = ((Map) partIterator.next()).get(SELECT_CURRENT).toString(); if (!TigerConstants.STATE_PART_OBSOLETE.equalsIgnoreCase(currentStateOfPrereq)) { hasPartobsolute = true;
     * break; } } } if (hasPartobsolute) break; } } // get the related Prerequisites in the MapList mListPrerequisites = getRelatedObjects(context, PropertyUtil.getSchemaProperty(context, relName), //
     * IR-248902V6R2014x PropertyUtil.getSchemaProperty(context, typeName), sList, null, false, true, (short) 1, EMPTY_STRING, relWhereClause, (short) 0);
     * 
     * for (Iterator prereqIterator = mListPrerequisites.iterator(); prereqIterator.hasNext();) { currentStateOfPrereq = ((Map) prereqIterator.next()).get(SELECT_CURRENT).toString(); ; // If
     * prerequisite change state is less than current change state means raise notice if (changeUtil.checkObjState(context, stateList, currentStateOfPrereq, stateName, ChangeConstants.LT) == 0) {
     * hasPrerequisites = true; break; } }
     * 
     * } catch (Exception d) { d.printStackTrace(); }
     * 
     * if (hasPrerequisites) { String strAlertMessage = EnoviaResourceBundle.getProperty(context, suiteKey, context.getLocale(), propertyKey); emxContextUtilBase_mxJPO.mqlNotice(context,
     * strAlertMessage); return 1; } if (hasPartobsolute) { String strAlertMessage = EnoviaResourceBundle.getProperty(context, suiteKey, context.getLocale(),
     * "EnterpriseChangeMgt.Warning.PartObsoluteMessage"); emxContextUtilBase_mxJPO.mqlNotice(context, strAlertMessage); return 1; } else { return 0; }
     * 
     * }
     * 
     * // TIGTK-17751:29-10-2018 : END
     */
     
     
     // START : TIGTK-14264 : Sub TIGTK-18220
    /**
     * Get the list of all Objects(CAs) which are connected to the Change object and From CAs retrieve all the affected items and send the list
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - HashMap containing one String entry for key "objectId"
     * @return a eMatrix <code>MapList</code> object having the list of Affected
     * @throws Exception
     *             if the operation fails
     * @since ECM R211
     **/

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAffectedItems(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map paramMap = (HashMap) programMap.get(ChangeConstants.PARAM_MAP);
        String strParentId = (String) programMap.get("parentOID");

        ChangeOrder changeOrder = new ChangeOrder();
        changeOrder.setId(strParentId);
        MapList mlAffectedItems = changeOrder.getAffectedItems(context);
        // initializing iterator
        Iterator objectListItr = mlAffectedItems.iterator();
        while (objectListItr.hasNext()) {
            Map objectMap = (Map) objectListItr.next();
            objectMap.put("RowEditable", "show");
        }
        return mlAffectedItems;
    }
    // START : TIGTK-14264 : Sub TIGTK-18220
    
}
