package pss.e2e.ui;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.User;
import matrix.db.UserItr;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class ProjectNotifications_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProjectNotifications_mxJPO.class);

    // Added by Suchit G. for TIGTK-3957 on Date: 01/02/2017: Start
    // For Fing bug issue : TIGTK-4401 : Priyanka Salunke : 09-Feb-2017
    // Fix for FindBugs issue Misuse of static fields: Suchit Gangurde: 28 Feb 2017
    String STR_SERVER_URL = DomainConstants.EMPTY_STRING;

    // Added by Suchit G. for TIGTK-3957 on Date: 01/02/2017: End

    /**
     * When the PSS_ProgramProject is created, this method will be invoked to populate the notification related fields on PSS_ProgramProject object by getting the values from related BG object.
     * @param context
     * @param args
     * @throws Exception
     */
    public void populateNotificationDatafromBG(Context context, String args[]) throws Exception {
        String strProjectId = args[0];
        DomainObject domProjObj = new DomainObject(strProjectId);
        // Get Organization name (BG name) of current user
        String strBusinessGroupOfCurrentUser = PersonUtil.getDefaultOrganization(context, context.getUser());
        StringList selectList = new StringList(DomainConstants.SELECT_ID);
        selectList.add(DomainConstants.SELECT_NAME);

        MapList mlBusObjList = domProjObj.findObjects(context, TigerConstants.TYPE_BUSINESSUNIT, TigerConstants.VAULT_ESERVICEPRODUCTION, null, selectList);

        // Get Frequency & Type and Rules in Days attribute names from property file
        StringList slAttrNames = FrameworkUtil.split(EnoviaResourceBundle.getProperty(context, "PSS_Components.E2ENotification.AttributeName"), "|");

        Map<String, String> mAttrMap = new HashMap<>();
        Iterator BusObjListItr = mlBusObjList.iterator();
        while (BusObjListItr.hasNext()) {
            Map mTempMap = (Map) BusObjListItr.next();
            String strBusinessUnitName = (String) mTempMap.get(DomainConstants.SELECT_NAME);
            String strBusinessUnitId = (String) mTempMap.get(DomainConstants.SELECT_ID);
            if (strBusinessGroupOfCurrentUser.equalsIgnoreCase(strBusinessUnitName)) {
                DomainObject domBusUnitObj = new DomainObject(strBusinessUnitId);
                for (int i = 0; i < slAttrNames.size(); i++) {
                    String strName = (String) slAttrNames.get(i);
                    String strAttrName = PropertyUtil.getSchemaProperty(context, strName);
                    String strAttrValue = domBusUnitObj.getAttributeValue(context, strAttrName);
                    mAttrMap.put(strAttrName, strAttrValue);
                }

                break;
            }
        }
        domProjObj.setAttributeValues(context, mAttrMap);

    }

    /**
     * @description: This method is used to set the values of attributes on the Program Project based.
     * @param context
     * @param args
     * @return nothing
     * @throws Exception
     */

    public void setNotificationValuesonProject(Context context, String args[]) throws Exception {
        String strBUId = "";
        String strBU = "";
        String strOpenedFrequency = "";
        String strDelayedFrequency = "";
        String strRemainderFrequency = "";
        String strcrduration = "";
        String strcnduration = "";
        String strcoduration = "";
        String strmcoduration = "";
        String stractivateNotification = "";
        DomainObject domBU = new DomainObject();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        try {
            Pattern typePat = new Pattern(DomainConstants.TYPE_COMPANY);
            typePat.addPattern(DomainConstants.TYPE_BUSINESS_UNIT);
            StringList slObjSelects = new StringList();
            slObjSelects.add(DomainConstants.SELECT_ID);
            slObjSelects.add(DomainConstants.SELECT_NAME);
            String strProjectObjId = (String) programMap.get("objectId");
            String strFunctionality = (String) programMap.get("Functionality");
            String strTypeOfChange = (String) programMap.get("typeOfChange");

            DomainObject domProjectObj = new DomainObject(strProjectObjId);
            if (UIUtil.isNotNullAndNotEmpty(strTypeOfChange) && "DefaultToBGValues".equalsIgnoreCase(strTypeOfChange)) {
                strBU = domProjectObj.getInfo(context, DomainConstants.SELECT_ORGANIZATION);
                String where = "name == " + strBU;
                if (UIUtil.isNotNullAndNotEmpty(strBU)) {
                    MapList mlBU = domProjectObj.findObjects(context, typePat.getPattern(), TigerConstants.VAULT_ESERVICEPRODUCTION, where, slObjSelects);
                    if (!mlBU.isEmpty()) {
                        Map mBU = (Map) mlBU.get(0);
                        strBUId = (String) mBU.get(DomainConstants.SELECT_ID);
                    }
                    domBU = DomainObject.newInstance(context, strBUId);
                } else {
                    MqlUtil.mqlCommand(context, "notice $1", "Organisation is not set on the Program Project");
                }
            }
            Map mAttributeMap = new HashMap();

            // Setting the values of attributes on the Program Project from the Escalation Tab.
            if (UIUtil.isNotNullAndNotEmpty(strFunctionality) && "Escalation".equalsIgnoreCase(strFunctionality)) {
                strOpenedFrequency = (String) programMap.get("OpenedFrequency");
                if (strOpenedFrequency != null) {
                    mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2EESCALATIONOPENEDCHGSFREQUENCY, strOpenedFrequency);
                }
                strDelayedFrequency = (String) programMap.get("DelayedFrequency");
                if (strDelayedFrequency != null) {
                    mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2EESCALATIONDELAYEDCHGSFREQUENCY, strDelayedFrequency);
                }
            }
            // Setting the values of attributes on the Program Project from the Notification Tab.
            else {
                // To set the Default BG Values
                if (UIUtil.isNotNullAndNotEmpty(strTypeOfChange) && "DefaultToBGValues".equalsIgnoreCase(strTypeOfChange) && UIUtil.isNotNullAndNotEmpty(strBU)) {
                    strOpenedFrequency = domBU.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2ENOTIFICATIONOPENEDCHGSFREQUENCY);
                }
                // To set the values from the Escalation Tab when activation mode is "Yes"
                else {
                    strOpenedFrequency = (String) programMap.get("OpenedFrequency");
                }
                if (strOpenedFrequency != null) {
                    mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2ENOTIFICATIONOPENEDCHGSFREQUENCY, strOpenedFrequency);
                }

                // To set the Default BG Values
                if (UIUtil.isNotNullAndNotEmpty(strTypeOfChange) && "DefaultToBGValues".equalsIgnoreCase(strTypeOfChange) && UIUtil.isNotNullAndNotEmpty(strBU)) {
                    strDelayedFrequency = domBU.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2ENOTIFICATIONDELAYEDCHGSFREQUENCY);
                }
                // To set the values from the Escalation Tab when activation mode is "Yes"
                else {
                    strDelayedFrequency = (String) programMap.get("DelayedFrequency");
                }
                if (strDelayedFrequency != null) {
                    mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2ENOTIFICATIONDELAYEDCHGSFREQUENCY, strDelayedFrequency);
                }

                // To set the Default BG Values
                if (UIUtil.isNotNullAndNotEmpty(strTypeOfChange) && "DefaultToBGValues".equalsIgnoreCase(strTypeOfChange) && UIUtil.isNotNullAndNotEmpty(strBU)) {
                    strRemainderFrequency = domBU.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2ENOTIFICATIONREMAINDERFREQUENCY);
                }
                // To set the values from the Escalation Tab when activation mode is "Yes"
                else {
                    strRemainderFrequency = (String) programMap.get("RemainderFrequency");
                }
                if (strRemainderFrequency != null) {
                    mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2ENOTIFICATIONREMAINDERFREQUENCY, strRemainderFrequency);
                }

                // To set the Default BG Values
                if (UIUtil.isNotNullAndNotEmpty(strTypeOfChange) && "DefaultToBGValues".equalsIgnoreCase(strTypeOfChange) && UIUtil.isNotNullAndNotEmpty(strBU)) {
                    strcrduration = domBU.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2ECRDURATIONATSUBMITSTATE);
                }
                // To set the values from the Escalation Tab when activation mode is "Yes"
                else {
                    strcrduration = (String) programMap.get("crduration");
                }
                if (strcrduration != null) {
                    mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2ECRDURATIONATSUBMITSTATE, strcrduration);
                }

                // To set the Default BG Values
                if (UIUtil.isNotNullAndNotEmpty(strTypeOfChange) && "DefaultToBGValues".equalsIgnoreCase(strTypeOfChange) && UIUtil.isNotNullAndNotEmpty(strBU)) {
                    strcnduration = domBU.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2EOPENEDCNDURATION);
                }
                // To set the values from the Escalation Tab when activation mode is "Yes"
                else {
                    strcnduration = (String) programMap.get("cnduration");
                }
                if (strcnduration != null) {
                    mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2EOPENEDCNDURATION, strcnduration);
                }

                // To set the Default BG Values
                if (UIUtil.isNotNullAndNotEmpty(strTypeOfChange) && "DefaultToBGValues".equalsIgnoreCase(strTypeOfChange) && UIUtil.isNotNullAndNotEmpty(strBU)) {
                    strcoduration = domBU.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2ECODURATIONATPREPARESTATE);
                }
                // To set the values from the Escalation Tab when activation mode is "Yes"
                else {
                    strcoduration = (String) programMap.get("coduration");
                }
                if (strcoduration != null) {
                    mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2ECODURATIONATPREPARESTATE, strcoduration);
                }

                // To set the Default BG Values
                if (UIUtil.isNotNullAndNotEmpty(strTypeOfChange) && "DefaultToBGValues".equalsIgnoreCase(strTypeOfChange) && UIUtil.isNotNullAndNotEmpty(strBU)) {
                    strmcoduration = domBU.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2EMCODURATIONATPREPARESTATE);
                }
                // To set the values from the Escalation Tab when activation mode is "Yes"
                else {
                    strmcoduration = (String) programMap.get("mcoduration");
                }
                if (strmcoduration != null) {
                    mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2EMCODURATIONATPREPARESTATE, strmcoduration);
                }

                // To set the Default BG Values
                if (UIUtil.isNotNullAndNotEmpty(strTypeOfChange) && "DefaultToBGValues".equalsIgnoreCase(strTypeOfChange) && UIUtil.isNotNullAndNotEmpty(strBU)) {
                    stractivateNotification = domBU.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2EPROJECTNOTIFICATIONACTIVATION);
                }
                // To set the values from the Escalation Tab when activation mode is "Yes"
                else {
                    stractivateNotification = (String) programMap.get("activateNotification");
                }
                if (stractivateNotification != null) {
                    mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2EPROJECTNOTIFICATIONACTIVATION, stractivateNotification);
                }

            }
            // Added by Suchit G. on 16/12/2016 for TIGTK-3803: Start
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
            // Added by Suchit G. on 16/12/2016 for TIGTK-3803: End
            domProjectObj.setAttributeValues(context, mAttributeMap);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setNotificationValuesonProject: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        // Added by Suchit G. on 16/12/2016 for TIGTK-3803: Start
        finally {
            ContextUtil.popContext(context);
        }
        // Added by Suchit G. on 16/12/2016 for TIGTK-3803: End

    }

    /**
     * This method is for getting Program Project Related all data
     * @param context
     * @param string
     *            = Program Project Id
     * @throws Exception
     * @author Priyanka Salunke
     */

    public Map getProgramProjectInfo(Context context, String strProgramProjectId) throws Exception {

        MapList mlCRInfoList = new MapList();
        MapList mlCOInfoList = new MapList();
        MapList mlCAInfoList = new MapList();
        MapList mlMCOInfoList = new MapList();
        MapList mlMCAInfoList = new MapList();
        MapList mlCNInfoList = new MapList();
        Map mPCMDataMap = new HashMap();

        DomainObject domProgramProjectObject = new DomainObject();
        // Program Project Object Id
        try {
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectId)) {
                domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectId);
            }

            // Relationship selects
            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.addElement(DomainConstants.SELECT_FROM_NAME);
            slSelectRelStmts.addElement(DomainConstants.SELECT_FROM_ID);

            // Relationship Pattern
            Pattern relationshipPCMPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);
            relationshipPCMPattern.addPattern(TigerConstants.RELATIONSHIP_CHANGEACTION);
            relationshipPCMPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION);
            relationshipPCMPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_RELATEDCN);
            // Type Pattern
            Pattern typePCMPattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            typePCMPattern.addPattern(TigerConstants.TYPE_PSS_CHANGEORDER);
            typePCMPattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
            typePCMPattern.addPattern(TigerConstants.TYPE_CHANGEACTION);
            typePCMPattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION);
            typePCMPattern.addPattern(TigerConstants.TYPE_PSS_CHANGENOTICE);
            // Object Where Expression
            // Modified for E2E issue TIGTK-3947 : Priyanka Salunke : 19-Jan-2017 : START
            StringBuffer sbWhereExpression = new StringBuffer();
            sbWhereExpression.append("(");
            sbWhereExpression.append("((type=='" + TigerConstants.TYPE_PSS_CHANGEREQUEST + "') ");
            sbWhereExpression.append("&& (current=='" + TigerConstants.STATE_SUBMIT_CR + "' || current==Const'Evaluate' || current=='" + TigerConstants.STATE_INREVIEW_CR + "' || current=='"
                    + TigerConstants.STATE_PSS_CR_INPROCESS + "'))");
            sbWhereExpression.append(" || ");
            sbWhereExpression.append("((type=='" + TigerConstants.TYPE_PSS_CHANGEORDER + "') && (current=='" + TigerConstants.STATE_PSS_CHANGEORDER_PREPARE + "' || current=='"
                    + TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL + "' || current=='" + TigerConstants.STATE_PSS_CHANGEORDER_INWORK + "'))");
            sbWhereExpression.append(" || ");
            sbWhereExpression.append("((type=='" + TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER + "') && (current=='" + TigerConstants.STATE_PSS_MCO_PREPARE + "' || current=='"
                    + TigerConstants.STATE_PSS_MCO_INWORK + "' || current=='" + TigerConstants.STATE_PSS_MCO_INREVIEW + "'))");
            sbWhereExpression.append(" || ");
            sbWhereExpression.append("((type=='" + TigerConstants.TYPE_CHANGEACTION + "') && (current=='" + TigerConstants.STATE_CHANGEACTION_INAPPROVAL + "' || current=='"
                    + TigerConstants.STATE_PSS_MCO_INWORK + "'))");
            sbWhereExpression.append(" || ");
            sbWhereExpression.append("((type=='" + TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION + "') && (current=='" + TigerConstants.STATE_PSS_MCA_INREVIEW + "' || current=='"
                    + TigerConstants.STATE_PSS_MCA_INWORK + "'))");
            sbWhereExpression.append(" || ");
            sbWhereExpression
                    .append("((type=='" + TigerConstants.TYPE_PSS_CHANGENOTICE + "') && (current=='" + TigerConstants.STATE_PREPARE_CN + "' || current=='" + TigerConstants.STATE_INREVIEW_CN + "'");
            sbWhereExpression.append(
                    " || current=='" + TigerConstants.STATE_INTRANSFER + "' || current=='" + TigerConstants.STATE_NOTFULLYINTEGRATED + "' || current=='" + TigerConstants.STATE_TRANSFERERROR + "'))");
            sbWhereExpression.append(")");
            // Modified for E2E issue TIGTK-3947 : Priyanka Salunke : 19-Jan-2017 : END
            /*
             * Get connected CR , CO and MCO of Program Project Get Connected CA of CO and MCA, CN of MCO
             */
            // Object selects
            // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017
            StringList slObjSelectStmts = getCommonObjectSelects();
            MapList mlConnectedPCMData = domProgramProjectObject.getRelatedObjects(context, // context
                    relationshipPCMPattern.getPattern(), // relationship
                    // pattern
                    typePCMPattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 3, // recursion level
                    sbWhereExpression.toString(), // object where clause
                    null, // relationship where clause
                    (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null);
            // Proceed PM data is not empty
            if (!(mlConnectedPCMData.isEmpty())) {
                // Get Program Project E2E attributes value
                // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017
                Map mProgramProjectMap = domProgramProjectObject.getAttributeMap(context);

                Iterator itrConnectedPCMData = mlConnectedPCMData.iterator();
                while (itrConnectedPCMData.hasNext()) {
                    Map mConnectedPCMData = (Map) itrConnectedPCMData.next();
                    String strPCMType = (String) mConnectedPCMData.get(DomainConstants.SELECT_TYPE);
                    String strPCMCurrent = (String) mConnectedPCMData.get(DomainConstants.SELECT_CURRENT);
                    // Basic Information which is pass to getInfo() method
                    // Type is CHANGE REQUEST
                    if (strPCMType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                        mConnectedPCMData.put("Change Info", (Map) getChangeInformationForCR(context, mConnectedPCMData, strProgramProjectId));
                        mConnectedPCMData.put("isOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));

                        // Changed by Suchit G. to isReminder from isRemainder on Date: 24/11/2016 for E2E2 Stream: TIGTK-3613
                        mConnectedPCMData.put("isReminder", isChangeRemainder(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mlCRInfoList.add(mConnectedPCMData);
                    } // CR type end
                      // Type is CHANGE ORDER
                    else if ((strPCMType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) && (strPCMCurrent.equalsIgnoreCase("Prepare"))) {
                        mConnectedPCMData.put("Change Info", (Map) getChangeInformationForCOandMCO(context, mConnectedPCMData, strProgramProjectId));
                        mConnectedPCMData.put("isOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));

                        // Changed by Suchit G. to isReminder from isRemainder on Date: 24/11/2016 for E2E2 Stream: TIGTK-3613
                        mConnectedPCMData.put("isReminder", isChangeRemainder(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mlCOInfoList.add(mConnectedPCMData);
                    } // CO type end
                      // Type is CHANGE ACTION
                    else if (strPCMType.equalsIgnoreCase(TigerConstants.TYPE_CHANGEACTION)) {
                        // Get CA Connected CO number
                        String strCONumber = (String) mConnectedPCMData.get(DomainConstants.SELECT_FROM_NAME);
                        String strCONumberId = (String) mConnectedPCMData.get(DomainConstants.SELECT_FROM_ID);
                        mConnectedPCMData.put("RelatedChangeNumber", strCONumber);
                        mConnectedPCMData.put("RelatedChangeId", strCONumberId);
                        mConnectedPCMData.put("Change Info", (Map) getChangeInformationForCAAndMCA(context, mConnectedPCMData, strProgramProjectId));
                        mConnectedPCMData.put("isOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));

                        // Changed by Suchit G. to isReminder from isRemainder on Date: 24/11/2016 for E2E2 Stream: TIGTK-3613
                        mConnectedPCMData.put("isReminder", isChangeRemainder(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mlCAInfoList.add(mConnectedPCMData);
                    } // CA type end
                      // Type is MFG CHANGE ORDER
                    else if ((strPCMType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) && (strPCMCurrent.equalsIgnoreCase("Prepare"))) {
                        mConnectedPCMData.put("Change Info", (Map) getChangeInformationForCOandMCO(context, mConnectedPCMData, strProgramProjectId));
                        mConnectedPCMData.put("isOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));

                        // Changed by Suchit G. to isReminder from isRemainder on Date: 24/11/2016 for E2E2 Stream: TIGTK-3613
                        mConnectedPCMData.put("isReminder", isChangeRemainder(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mlMCOInfoList.add(mConnectedPCMData);
                    } // MCO type end
                      // Type is MFG CHANGE ACTION
                    else if (strPCMType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {
                        // Get MCA Connected MCO number
                        String strMCONumber = (String) mConnectedPCMData.get(DomainConstants.SELECT_FROM_NAME);
                        String strCONumberId = (String) mConnectedPCMData.get(DomainConstants.SELECT_FROM_ID);
                        mConnectedPCMData.put("RelatedChangeNumber", strMCONumber);
                        mConnectedPCMData.put("RelatedChangeId", strCONumberId);
                        mConnectedPCMData.put("Change Info", (Map) getChangeInformationForCAAndMCA(context, mConnectedPCMData, strProgramProjectId));
                        mConnectedPCMData.put("isOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));

                        // Changed by Suchit G. to isReminder from isRemainder on Date: 24/11/2016 for E2E2 Stream: TIGTK-3613
                        mConnectedPCMData.put("isReminder", isChangeRemainder(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mlMCAInfoList.add(mConnectedPCMData);
                    } // MCA type end
                      // Type is CHANGE NOTICE
                    else if (strPCMType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGENOTICE)) {
                        mConnectedPCMData.put("Change Info", (Map) getChangeInformationForCN(context, mConnectedPCMData, strProgramProjectId));
                        mConnectedPCMData.put("isOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));

                        // Changed by Suchit G. to isReminder from isRemainder on Date: 24/11/2016 for E2E2 Stream: TIGTK-3613
                        mConnectedPCMData.put("isReminder", isChangeRemainder(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationOpened", isChangeOpened(context, mConnectedPCMData, mProgramProjectMap));
                        mConnectedPCMData.put("isEscalationDelayed", isChangeDelayed(context, mConnectedPCMData, mProgramProjectMap));
                        mlCNInfoList.add(mConnectedPCMData);
                    } // CN type end
                } // while end
                  // Add CR,CO,CA,MCO,MCA,CN map list in one map to return
                mPCMDataMap.put("CR", mlCRInfoList);
                mPCMDataMap.put("CO", mlCOInfoList);
                mPCMDataMap.put("CA", mlCAInfoList);
                mPCMDataMap.put("MCO", mlMCOInfoList);
                mPCMDataMap.put("MCA", mlMCAInfoList);
                mPCMDataMap.put("CN", mlCNInfoList);
            } // If mlConnectedPCMData not empty end

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgramProjectInfo: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return mPCMDataMap;
    }

    /**
     * This method is for getting Change Order and Mfg Change Order Related all data
     * @param context
     * @param string
     *            = Program Project Id , Map
     * @throws Exception
     * @author Priyanka Salunke
     */

    public Map getChangeInformationForCOandMCO(Context context, Map mChange, String strProgProjId) throws Exception {

        Map mChangeInfo = new HashMap();
        StringList slNotificationList = new StringList();
        DomainObject domProgramProjectObj = new DomainObject();
        DomainObject domObj = new DomainObject();
        try {
            String strPolicy = (String) mChange.get(DomainConstants.SELECT_POLICY);
            String strCRState = (String) mChange.get(DomainConstants.SELECT_CURRENT);
            String strCRStateAliasName = FrameworkUtil.reverseLookupStateName(context, strPolicy, strCRState);
            String strType = (String) mChange.get(DomainConstants.SELECT_TYPE);
            String strTypeAliasName = PropertyUtil.getAliasForAdmin(context, "Type", strType, false);
            String strKey = "emxFramework.E2E." + strTypeAliasName + "." + strCRStateAliasName;
            if (UIUtil.isNotNullAndNotEmpty(strProgProjId)) {
                domProgramProjectObj = DomainObject.newInstance(context, strProgProjId);
            }
            String strCurrentState = (String) mChange.get(DomainConstants.SELECT_CURRENT);
            ;
            String strCreator = (String) mChange.get(DomainConstants.SELECT_OWNER);
            // Notification list
            if (UIUtil.isNotNullAndNotEmpty(strCreator)) {
                if (!strCreator.startsWith("auto_")) {
                    slNotificationList.add(strCreator);
                }
            }
            String strId = (String) mChange.get(DomainConstants.SELECT_ID);
            if (UIUtil.isNotNullAndNotEmpty(strId)) {
                domObj = DomainObject.newInstance(context, strId);
            }
            Date dTodayDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            String strTodaysDate = dateFormat.format(dTodayDate);
            Date todaysDate = new Date();
            if (UIUtil.isNotNullAndNotEmpty(strTodaysDate)) {
                todaysDate = dateFormat.parse(strTodaysDate);
            }

            // Required Action

            String strRequiredAction = getPropertyValue(context, strKey);

            if (strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_MCO_PREPARE) || strCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_CHANGEORDER_PREPARE)) {
                String inStateSince = domObj.getInfo(context, "current.actual");
                Date dCOCurrentDate = new Date();
                if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                    dCOCurrentDate = dateFormat.parse(inStateSince);
                }
                long lDifference = todaysDate.getTime() - dCOCurrentDate.getTime();
                long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                long lDelay;
                long lLimit;
                // Find Bug Issue : TIGTK-3955 : Priyanka Salunke : 25-Jan-2017 : START
                // Method uses the same code for two branches
                String strCOOrMCODurationAtPrepareStateValue = "";
                int intCOOrMCOLimit;
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) {
                    // Limit = Get the value of attribute PSS_ E2ECODurationAtPrepareState from project
                    strCOOrMCODurationAtPrepareStateValue = domProgramProjectObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2ECODURATIONATPREPARESTATE);
                } else {
                    strCOOrMCODurationAtPrepareStateValue = domProgramProjectObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2EMCODURATIONATPREPARESTATE);
                }
                intCOOrMCOLimit = Integer.parseInt(strCOOrMCODurationAtPrepareStateValue);
                mChangeInfo.put("RequiredAction", strRequiredAction.concat(" (").concat(strCurrentState).concat(" state)"));
                lLimit = lInStateSince - intCOOrMCOLimit;
                lDelay = lLimit;
                // Find Bug Issue : TIGTK-3955 : Priyanka Salunke : 25-Jan-2017 :END
                // Method uses the same code for two branches
                if (lLimit <= 0) {
                    mChangeInfo.put("Delay", "N/A");
                } else {
                    mChangeInfo.put("Delay", "" + lDelay);
                }
                mChangeInfo.put("InStateSince", lInStateSince);
                mChangeInfo.put("DueDate", "N/A");
                mChangeInfo.put("NotificationList", slNotificationList);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getChangeInformationForCOandMCO: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return mChangeInfo;
    }

    /**
     * This method is for getting Change Notice Related all data
     * @param context
     * @param string
     *            = Program Project Id , Map
     * @throws Exception
     * @author Priyanka Salunke
     */

    public Map getChangeInformationForCN(Context context, Map mChange, String strProgProjId) throws Exception {

        DomainObject domProgramProjectObj = new DomainObject();
        DomainObject domCNObject = new DomainObject();
        Map mChangeInfo = new HashMap();
        StringList slNotificationList = new StringList();
        try {
            StringBuffer sbITDueDate = new StringBuffer();
            String strPolicy = (String) mChange.get(DomainConstants.SELECT_POLICY);
            String strState = (String) mChange.get(DomainConstants.SELECT_CURRENT);
            String strStateAliasName = FrameworkUtil.reverseLookupStateName(context, strPolicy, strState);
            String strType = (String) mChange.get(DomainConstants.SELECT_TYPE);
            String strTypeAliasName = PropertyUtil.getAliasForAdmin(context, "Type", strType, false);
            String strKey = "emxFramework.E2E." + strTypeAliasName + "." + strStateAliasName;
            String strRequiredAction = getPropertyValue(context, strKey);
            if (UIUtil.isNotNullAndNotEmpty(strProgProjId)) {
                domProgramProjectObj = DomainObject.newInstance(context, strProgProjId);
            }
            String strCNCurrentState = (String) mChange.get(DomainConstants.SELECT_CURRENT);
            String strCNCreator = (String) mChange.get(DomainConstants.SELECT_OWNER);
            String strId = (String) mChange.get(DomainConstants.SELECT_ID);
            if (UIUtil.isNotNullAndNotEmpty(strId)) {
                domCNObject = DomainObject.newInstance(context, strId);
            }
            Date dTodayDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            String strTodaysDate = dateFormat.format(dTodayDate);
            Date todaysDate = new Date();
            if (UIUtil.isNotNullAndNotEmpty(strTodaysDate)) {
                todaysDate = dateFormat.parse(strTodaysDate);
            }

            // CN State is IN REVIEW
            if (strCNCurrentState.equalsIgnoreCase(TigerConstants.STATE_INREVIEW_CN)) {
                StringBuffer sbTaskDueDate = new StringBuffer();
                String strTaskAssignee = "";
                // Getting the Required Action
                // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                mChangeInfo.put("RequiredAction", getPropertyValue(context, strKey).concat(" (").concat(strCNCurrentState).concat(" state)"));
                // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END

                Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_OBJECT_ROUTE);
                Pattern tPattern = new Pattern(DomainConstants.TYPE_ROUTE);
                String strObjRouteWhere = "current != Complete";
                String strTaskWhere = "current != 'Complete'";
                // Object selects
                // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017
                StringList slObjSelectStmts = getCommonObjectSelects();
                // Relationship selects
                StringList slSelectRelStmts = new StringList(1);
                slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                slSelectRelStmts.addElement(DomainConstants.SELECT_FROM_NAME);
                // Getting the CN ROutes
                MapList mlCNRoutes = domCNObject.getRelatedObjects(context, // context
                        relPattern.getPattern(), // relationship pattern
                        tPattern.getPattern(), // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        strObjRouteWhere, // object where clause
                        null, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null);

                if (!mlCNRoutes.isEmpty()) {
                    for (int itrRoute = 0; itrRoute < mlCNRoutes.size(); itrRoute++) {
                        Map mRouteMap = (Map) mlCNRoutes.get(itrRoute);
                        String strRouteId = (String) mRouteMap.get(DomainConstants.SELECT_ID);
                        DomainObject domRouteObj = new DomainObject();
                        ;
                        if (UIUtil.isNotNullAndNotEmpty(strRouteId)) {
                            domRouteObj = DomainObject.newInstance(context, strRouteId);
                        }
                        // Getting the Inbox Task connected to CR Impact Analysis route
                        MapList mlRouteTaskList = domRouteObj.getRelatedObjects(context, // context
                                DomainConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
                                DomainConstants.TYPE_INBOX_TASK, // object pattern
                                slObjSelectStmts, // object selects
                                slSelectRelStmts, // relationship selects
                                true, // to direction
                                false, // from direction
                                (short) 1, // recursion level
                                strTaskWhere, // object where clause
                                null, // relationship where clause
                                (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 0, // pageSize
                                null, null, null, null);
                        if (!mlRouteTaskList.isEmpty()) {
                            for (int itrRouteTask = 0; itrRouteTask < mlRouteTaskList.size(); itrRouteTask++) {
                                Map mRouteTask = (Map) mlRouteTaskList.get(itrRouteTask);
                                String strRouteTaskId = (String) mRouteTask.get(DomainConstants.SELECT_ID);
                                DomainObject domRouteTask = new DomainObject();
                                if (UIUtil.isNotNullAndNotEmpty(strRouteTaskId)) {
                                    domRouteTask = DomainObject.newInstance(context, strRouteTaskId);
                                }

                                String strInboxTaskDueDate = domRouteTask.getAttributeValue(context, TigerConstants.ATTRIBUTE_SCHEDULEDCOMPLETIONDATE);
                                Date dInboxTaskDueDate = new Date();
                                if (UIUtil.isNotNullAndNotEmpty(strInboxTaskDueDate)) {
                                    dInboxTaskDueDate = dateFormat.parse(strInboxTaskDueDate);
                                }
                                long lTaskDelay = todaysDate.getTime() - dInboxTaskDueDate.getTime();
                                long lTaskDelayInDay = TimeUnit.DAYS.convert(lTaskDelay, TimeUnit.MILLISECONDS);

                                String strMQLCommand = "print bus " + strId + " select current.actual dump;";
                                String inStateSince = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                                Date dCRCurrentDate = new Date();
                                if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                                    dCRCurrentDate = dateFormat.parse(inStateSince);
                                }
                                long lDifference = todaysDate.getTime() - dCRCurrentDate.getTime();
                                long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                                if (lTaskDelay > 0) {
                                    mChangeInfo.put("Delay", "" + lTaskDelayInDay);
                                } else {
                                    mChangeInfo.put("Delay", "N/A");
                                }
                                mChangeInfo.put("InStateSince", lInStateSince);
                                // Getting the Task Due Date
                                String strDueDate = domRouteTask.getInfo(context, "attribute[" + DomainConstants.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]");
                                sbITDueDate.append(strDueDate);
                                sbITDueDate.append(",");
                                // Getting the Task Name
                                String strTaskName = domRouteTask.getInfo(context, DomainConstants.SELECT_NAME);
                                String strTaskId = domRouteTask.getInfo(context, DomainConstants.SELECT_ID);
                                strTaskName = getObjectLinkHTML(context, strTaskName, strTaskId);
                                // Getting the Task Assignee
                                strTaskAssignee = domRouteTask.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_PROJECT_TASK + "].to.name");
                                sbTaskDueDate.append(" ");
                                sbTaskDueDate.append(strTaskName);
                                sbTaskDueDate.append(" - ");
                                sbTaskDueDate.append(strDueDate);
                                if (UIUtil.isNotNullAndNotEmpty(strTaskAssignee)) {
                                    if (!strTaskAssignee.startsWith("auto_")) {
                                        slNotificationList.add(strTaskAssignee);
                                    }
                                }
                            }
                        }
                    }
                }
                mChangeInfo.put("DueDate", sbTaskDueDate.toString());
                mChangeInfo.put("NotificationList", slNotificationList);
                mChangeInfo.put("TaskDueDate", sbITDueDate.toString());

            } // CN State is IN REVIEW end
              // CN not in Fully Integrated or Cancelled state
            if (((!(strCNCurrentState.equalsIgnoreCase(TigerConstants.STATE_FULLYINTEGRATED))) || (!(strCNCurrentState.equalsIgnoreCase(TigerConstants.STATE_CN_CANCELLED))))
                    && (!(strCNCurrentState.equalsIgnoreCase(TigerConstants.STATE_INREVIEW_CN)))) {
                // Notification List
                if (UIUtil.isNotNullAndNotEmpty(strCNCreator)) {
                    if (!strCNCreator.startsWith("auto_")) {
                        slNotificationList.add(strCNCreator);
                    }
                }

                // In State Since
                String inStateSince = domCNObject.getInfo(context, "current.actual");
                Date dCNCurrentDate = new Date();
                if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                    dCNCurrentDate = dateFormat.parse(inStateSince);
                }
                long lDifference = todaysDate.getTime() - dCNCurrentDate.getTime();
                long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                mChangeInfo.put("RequiredAction", strRequiredAction.concat(" (").concat(strCNCurrentState).concat(" state)"));
                // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END
                // Added for E2E stream issue TIGTK-3945 : Priyanka Salunke : 17-Jan-2017 : START
                String strDurationOfOpenCNValue = domProgramProjectObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2EOPENEDCNDURATION);
                int CNLimit = Integer.parseInt(strDurationOfOpenCNValue);
                long lDelay = lInStateSince - CNLimit;
                mChangeInfo.put("InStateSince", lInStateSince);
                if (lDelay > 0) {
                    mChangeInfo.put("Delay", "" + lDelay);
                } else {
                    mChangeInfo.put("Delay", "N/A");
                }
                // Added for E2E stream issue TIGTK-3945 : Priyanka Salunke : 17-Jan-2017 : END
                mChangeInfo.put("InStateSince", lInStateSince);
                mChangeInfo.put("DueDate", "N/A");
                mChangeInfo.put("NotificationList", slNotificationList);

            } // CN not in Fully Integrated or Cancelled state end
        } // try end
        catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getChangeInformationForCN: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return mChangeInfo;
    }

    /**
     * @description: Access Function on Table Column Manager and Manager Email Id in the Member Tab of the Program Project.
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */

    public boolean checkEscalationEditAccess(Context context, String args[]) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        boolean flag = false;
        String strRoleName = "";
        try {
            String strProgProjId = (String) programMap.get("objectId");
            DomainObject domProgProj = DomainObject.newInstance(context, strProgProjId);
            String strActivatioNotification = domProgProj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2EPROJECTNOTIFICATIONACTIVATION);
            String strProgProjCurState = domProgProj.getInfo(context, DomainConstants.SELECT_CURRENT);
            String strUser = context.getUser();
            matrix.db.Person person = new matrix.db.Person(strUser);
            person.open(context);
            UserItr userItr = new UserItr(person.getAssignments(context));
            while (userItr.next()) {
                User userObj = userItr.obj();
                if (userObj instanceof matrix.db.Role) {
                    strRoleName = userObj.getName();
                    if (strRoleName.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || (strRoleName.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM))
                            || (strRoleName.equalsIgnoreCase(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR))) {
                        if ("TRUE".equalsIgnoreCase(strActivatioNotification)) {
                            if (strProgProjCurState.equalsIgnoreCase("Active") || strProgProjCurState.equalsIgnoreCase("Phase 1") || strProgProjCurState.equalsIgnoreCase("Phase 2a")
                                    || strProgProjCurState.equalsIgnoreCase("Phase 2b") || strProgProjCurState.equalsIgnoreCase("Phase 3") || strProgProjCurState.equalsIgnoreCase("Phase 4")
                                    || strProgProjCurState.equalsIgnoreCase("Phase 5")) {
                                flag = true;
                            } else {
                                flag = false;
                            }
                        } else {
                            flag = false;
                        }
                    } else {
                        flag = false;
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkEscalationEditAccess: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        if (flag == true) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * @description: This method is used to get the Information of CR
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */

    public Map getChangeInformationForCR(Context context, Map mChange, String strProgProjId) throws Exception {
        Map mChangeInfo = new HashMap();
        StringList slNotificationList = new StringList();
        DomainObject domCR = new DomainObject();
        DomainObject domRouteTask = new DomainObject();
        DomainObject domProgramProject = new DomainObject();
        DomainObject domRouteObj = new DomainObject();
        try {
            StringBuffer sbITDueDate = new StringBuffer();
            Date dTodayDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            String strTodaysDate = dateFormat.format(dTodayDate);
            Date todaysDate = dateFormat.parse(strTodaysDate);
            String strPolicy = (String) mChange.get(DomainConstants.SELECT_POLICY);
            String strCRState = (String) mChange.get(DomainConstants.SELECT_CURRENT);
            String strCRStateAliasName = FrameworkUtil.reverseLookupStateName(context, strPolicy, strCRState);
            String strType = (String) mChange.get(DomainConstants.SELECT_TYPE);
            String strTypeAliasName = PropertyUtil.getAliasForAdmin(context, "Type", strType, false);
            String strKey = "emxFramework.E2E." + strTypeAliasName + "." + strCRStateAliasName;
            String strCRObjId = (String) mChange.get(DomainConstants.SELECT_ID);
            if (UIUtil.isNotNullAndNotEmpty(strCRObjId)) {
                domCR = DomainObject.newInstance(context, strCRObjId);
            }
            if (UIUtil.isNotNullAndNotEmpty(strProgProjId)) {
                domProgramProject = DomainObject.newInstance(context, strProgProjId);
            }

            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.add(DomainConstants.SELECT_ID);
            slObjSelectStmts.add(DomainConstants.SELECT_CURRENT);
            slObjSelectStmts.add(DomainConstants.SELECT_NAME);

            StringList slSelectRelStmts = new StringList();
            slSelectRelStmts.add(DomainRelationship.SELECT_ID);

            // If the CR in in "Submit" State
            if (TigerConstants.STATE_SUBMIT_CR.equals(strCRState)) {
                String strMQLCommand = "print bus " + strCRObjId + " select current.actual dump;";
                String inStateSince = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                Date dCRCurrentDate = new Date();
                if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                    dCRCurrentDate = dateFormat.parse(inStateSince);
                }
                long lDifference = todaysDate.getTime() - dCRCurrentDate.getTime();
                long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                String strCRDurationAtSubmitStateValue = domProgramProject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2ECRDURATIONATSUBMITSTATE);
                int CRLimit = Integer.parseInt(strCRDurationAtSubmitStateValue);
                long lDelay = lInStateSince - CRLimit;
                mChangeInfo.put("InStateSince", lInStateSince);
                if (lDelay > 0) {
                    mChangeInfo.put("Delay", "" + lDelay);
                } else {
                    mChangeInfo.put("Delay", "N/A");
                }

                mChangeInfo.put("DueDate", "N/A");
                // Getting the Required Action when CR state is "Submit"
                // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                mChangeInfo.put("RequiredAction", getPropertyValue(context, strKey).concat(" (").concat(strCRState).concat(" state)"));
                // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END
                // Getting the Change Manager of the CR
                String slCRChangeManager = domCR.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].to.name");
                if (UIUtil.isNotNullAndNotEmpty(slCRChangeManager)) {
                    if (!slCRChangeManager.startsWith("auto_")) {
                        slNotificationList.add(slCRChangeManager);
                    }

                }
                mChangeInfo.put("NotificationList", slNotificationList);
            }

            // If the CR in in "Evaluate" State
            else if ("Evaluate".equals(strCRState)) {
                StringBuffer sbTaskDueDate = new StringBuffer();
                String strTaskAssignee = "";
                // Getting the Required Action
                // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                mChangeInfo.put("RequiredAction", getPropertyValue(context, strKey).concat(" (").concat(strCRState).concat(" state)"));
                // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END
                Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_OBJECT_ROUTE);
                Pattern tPattern = new Pattern(DomainConstants.TYPE_ROUTE);
                String strObjRouteWhere = "current != Complete";
                String strRelRouteWhere = "attribute[Route Base State] == state_Evaluate";
                String strTaskWhere = "current != 'Complete'";
                slSelectRelStmts.add("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASESTATE + "]");
                // Getting the CR Impact Analysis route
                MapList mlCRRoutes = domCR.getRelatedObjects(context, // context
                        relPattern.getPattern(), // relationship pattern
                        tPattern.getPattern(), // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        strObjRouteWhere, // object where clause
                        strRelRouteWhere, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null);

                if (!mlCRRoutes.isEmpty()) {
                    for (int itrRoute = 0; itrRoute < mlCRRoutes.size(); itrRoute++) {
                        Map mRouteMap = (Map) mlCRRoutes.get(itrRoute);
                        String strRouteId = (String) mRouteMap.get(DomainConstants.SELECT_ID);
                        if (UIUtil.isNotNullAndNotEmpty(strRouteId)) {
                            domRouteObj = DomainObject.newInstance(context, strRouteId);
                        }
                        // Getting the Inbox Task connected to CR Impact Analysis route
                        MapList mlRouteTaskList = domRouteObj.getRelatedObjects(context, // context
                                DomainConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
                                DomainConstants.TYPE_INBOX_TASK, // object pattern
                                slObjSelectStmts, // object selects
                                slSelectRelStmts, // relationship selects
                                true, // to direction
                                false, // from direction
                                (short) 1, // recursion level
                                strTaskWhere, // object where clause
                                null, // relationship where clause
                                (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 0, // pageSize
                                null, null, null, null);
                        if (!mlRouteTaskList.isEmpty()) {
                            for (int itrRouteTask = 0; itrRouteTask < mlRouteTaskList.size(); itrRouteTask++) {
                                Map mRouteTask = (Map) mlRouteTaskList.get(itrRouteTask);
                                String strRouteTaskId = (String) mRouteTask.get(DomainConstants.SELECT_ID);
                                if (UIUtil.isNotNullAndNotEmpty(strRouteTaskId)) {
                                    domRouteTask = DomainObject.newInstance(context, strRouteTaskId);
                                }
                                String strInboxTaskDueDate = domRouteTask.getAttributeValue(context, TigerConstants.ATTRIBUTE_SCHEDULEDCOMPLETIONDATE);
                                Date dInboxTaskDueDate = new Date();
                                if (UIUtil.isNotNullAndNotEmpty(strInboxTaskDueDate)) {
                                    dInboxTaskDueDate = dateFormat.parse(strInboxTaskDueDate);
                                }
                                long lTaskDelay = todaysDate.getTime() - dInboxTaskDueDate.getTime();
                                long lTaskDelayInDay = TimeUnit.DAYS.convert(lTaskDelay, TimeUnit.MILLISECONDS);

                                String strMQLCommand = "print bus " + strCRObjId + " select current.actual dump;";
                                String inStateSince = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                                Date dCRCurrentDate = new Date();
                                if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                                    dCRCurrentDate = dateFormat.parse(inStateSince);
                                }
                                long lDifference = todaysDate.getTime() - dCRCurrentDate.getTime();
                                long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                                if (lTaskDelayInDay > 0) {
                                    mChangeInfo.put("Delay", "" + lTaskDelayInDay);
                                } else {
                                    mChangeInfo.put("Delay", "N/A");
                                }
                                mChangeInfo.put("InStateSince", lInStateSince);
                                // Getting the Task Due Date
                                String strDueDate = domRouteTask.getInfo(context, "attribute[" + DomainConstants.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]");
                                sbITDueDate.append(strDueDate);
                                sbITDueDate.append(",");
                                // Getting the Task Name
                                String strTaskName = domRouteTask.getInfo(context, DomainConstants.SELECT_NAME);
                                String strTaskId = domRouteTask.getInfo(context, DomainConstants.SELECT_ID);
                                strTaskName = getObjectLinkHTML(context, strTaskName, strTaskId);
                                // Getting the Task Assignee
                                strTaskAssignee = domRouteTask.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_PROJECT_TASK + "].to.name");
                                sbTaskDueDate.append(" ");
                                sbTaskDueDate.append(strTaskName);
                                sbTaskDueDate.append(" - ");
                                sbTaskDueDate.append(strDueDate);
                                if (UIUtil.isNotNullAndNotEmpty(strTaskAssignee)) {
                                    if (!strTaskAssignee.startsWith("auto_")) {
                                        slNotificationList.add(strTaskAssignee);
                                    }
                                }
                            }
                        }
                    }
                }
                mChangeInfo.put("DueDate", sbTaskDueDate.toString());
                mChangeInfo.put("NotificationList", slNotificationList);
                mChangeInfo.put("TaskDueDate", sbITDueDate.toString());
            }

            // If the CR in in "In Review" State
            else if (TigerConstants.STATE_INREVIEW_CR.equals(strCRState)) {
                StringBuffer sbTaskDueDate = new StringBuffer();
                String strTaskAssignee = "";
                // Getting the Required Action
                // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                mChangeInfo.put("RequiredAction", getPropertyValue(context, strKey).concat(" (").concat(strCRState).concat(" state)"));
                // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END
                Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_OBJECT_ROUTE);
                Pattern tPattern = new Pattern(DomainConstants.TYPE_ROUTE);
                String strObjRouteWhere = "current != Complete";
                String strRelRouteWhere = "attribute[Route Base State] == state_InReview";
                String strTaskWhere = "current != 'Complete'";
                slSelectRelStmts.add("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASESTATE + "]");
                // Getting the connected CR Approval route
                MapList mlCRRoutes = domCR.getRelatedObjects(context, // context
                        relPattern.getPattern(), // relationship pattern
                        tPattern.getPattern(), // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        strObjRouteWhere, // object where clause
                        strRelRouteWhere, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null);
                if (!mlCRRoutes.isEmpty()) {
                    for (int itrRoute = 0; itrRoute < mlCRRoutes.size(); itrRoute++) {
                        Map mRouteMap = (Map) mlCRRoutes.get(itrRoute);
                        String strRouteId = (String) mRouteMap.get(DomainConstants.SELECT_ID);
                        if (UIUtil.isNotNullAndNotEmpty(strRouteId)) {
                            domRouteObj = DomainObject.newInstance(context, strRouteId);
                        }
                        // Getting the Inbox Task connected to CR Approval route
                        MapList mlRouteTaskList = domRouteObj.getRelatedObjects(context, // context
                                DomainConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
                                DomainConstants.TYPE_INBOX_TASK, // object pattern
                                slObjSelectStmts, // object selects
                                slSelectRelStmts, // relationship selects
                                true, // to direction
                                false, // from direction
                                (short) 1, // recursion level
                                strTaskWhere, // object where clause
                                null, // relationship where clause
                                (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 0, // pageSize
                                null, null, null, null);
                        if (!mlRouteTaskList.isEmpty()) {
                            for (int itrRouteTask = 0; itrRouteTask < mlRouteTaskList.size(); itrRouteTask++) {
                                Map mRouteTask = (Map) mlRouteTaskList.get(itrRouteTask);
                                String strRouteTaskId = (String) mRouteTask.get(DomainConstants.SELECT_ID);
                                if (UIUtil.isNotNullAndNotEmpty(strRouteTaskId)) {
                                    domRouteTask = DomainObject.newInstance(context, strRouteTaskId);
                                }
                                String strInboxTaskDueDate = domRouteTask.getAttributeValue(context, TigerConstants.ATTRIBUTE_SCHEDULEDCOMPLETIONDATE);
                                Date dInboxTaskDueDate = new Date();
                                if (UIUtil.isNotNullAndNotEmpty(strInboxTaskDueDate)) {
                                    dInboxTaskDueDate = dateFormat.parse(strInboxTaskDueDate);
                                }
                                long lTaskDelay = todaysDate.getTime() - dInboxTaskDueDate.getTime();
                                long lTaskDelayInDay = TimeUnit.DAYS.convert(lTaskDelay, TimeUnit.MILLISECONDS);

                                String strMQLCommand = "print bus " + strCRObjId + " select current.actual dump;";
                                String inStateSince = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                                Date dCRCurrentDate = new Date();
                                if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                                    dCRCurrentDate = dateFormat.parse(inStateSince);
                                }
                                long lDifference = todaysDate.getTime() - dCRCurrentDate.getTime();
                                long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                                if (lTaskDelayInDay > 0) {
                                    mChangeInfo.put("Delay", "" + lTaskDelayInDay);
                                } else {
                                    mChangeInfo.put("Delay", "N/A");
                                }

                                mChangeInfo.put("InStateSince", lInStateSince);
                                // Getting the Task Due Date
                                String strDueDate = domRouteTask.getInfo(context, "attribute[" + DomainConstants.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]");
                                sbITDueDate.append(strDueDate);
                                sbITDueDate.append(",");
                                // Getting the Task Name
                                String strTaskName = domRouteTask.getInfo(context, DomainConstants.SELECT_NAME);
                                String strTaskId = domRouteTask.getInfo(context, DomainConstants.SELECT_ID);
                                strTaskName = getObjectLinkHTML(context, strTaskName, strTaskId);
                                // Getting the Task Assignee
                                strTaskAssignee = domRouteTask.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_PROJECT_TASK + "].to.name");
                                sbTaskDueDate.append(" ");
                                sbTaskDueDate.append(strTaskName);
                                sbTaskDueDate.append(" - ");
                                sbTaskDueDate.append(strDueDate);
                                if (!strTaskAssignee.startsWith("auto_")) {
                                    slNotificationList.add(strTaskAssignee);
                                }
                            }
                        }
                    }
                }
                mChangeInfo.put("DueDate", sbTaskDueDate.toString());
                mChangeInfo.put("NotificationList", slNotificationList);
                mChangeInfo.put("TaskDueDate", sbITDueDate.toString());
            }

            // If the CR in in "In Process" State
            else if (TigerConstants.STATE_PSS_CR_INPROCESS.equals(strCRState)) {
                Pattern relCRPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER);
                relCRPattern.addPattern(TigerConstants.RELATIONSHIP_CHANGEORDER);
                Pattern typeCRPattern = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
                typeCRPattern.addPattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
                typeCRPattern.addPattern(TigerConstants.TYPE_PSS_CHANGEORDER);

                Pattern typePersonPattern = new Pattern(DomainConstants.TYPE_PERSON);
                Pattern relPersonPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS);
                slObjSelectStmts.add(DomainConstants.SELECT_ID);
                slObjSelectStmts.add(DomainConstants.SELECT_CURRENT);
                // Getting the CO and MCO connected to the CR
                MapList mlCRConnectedCOandMCO = domCR.getRelatedObjects(context, // context
                        relCRPattern.getPattern(), // relationship pattern
                        typeCRPattern.getPattern(), // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 0, // pageSize
                        null, null, null, null);

                if (mlCRConnectedCOandMCO.isEmpty()) {
                    strKey = strKey + ".No";
                    mChangeInfo.put("DueDate", "N/A");
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                    mChangeInfo.put("RequiredAction", getPropertyValue(context, strKey).concat(" (").concat(strCRState).concat(" state)"));
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END
                    slSelectRelStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]");
                    // Modified for E2E issue : TIGTK-3948 : Priyanka Salunke : 01-Feb-2017 : START

                    // TIGTK-5890 : PTE : 4/7/2017 : START

                    String where = "attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]==" + TigerConstants.ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD + "|| " + "attribute["
                            + TigerConstants.ATTRIBUTE_PSS_ROLE + "]==" + TigerConstants.ROLE_PSS_PROGRAM_MANUFACTURING_LEADER;
                    // TIGTK-5890 : PTE : 4/7/2017 : END

                    // Modified for E2E issue : TIGTK-3948 : Priyanka Salunke : 01-Feb-2017 : END
                    String strMQLCommand = "print bus " + strCRObjId + " select current.actual dump;";
                    String inStateSince = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                    Date dCRCurrentDate = new Date();
                    if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                        dCRCurrentDate = dateFormat.parse(inStateSince);
                    }
                    long lDifference = todaysDate.getTime() - dCRCurrentDate.getTime();
                    long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                    mChangeInfo.put("InStateSince", lInStateSince);
                    mChangeInfo.put("Delay", "N/A");
                    // Getting the Members of the Program Project
                    MapList mlProgramProjectPersonsList = domProgramProject.getRelatedObjects(context, // context
                            relPersonPattern.getPattern(), // relationship pattern
                            typePersonPattern.getPattern(), // object pattern
                            slObjSelectStmts, // object selects
                            slSelectRelStmts, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            where, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 0, // pageSize
                            null, null, null, null);
                    if (!mlProgramProjectPersonsList.isEmpty()) {
                        for (int itrProgProjPer = 0; itrProgProjPer < mlProgramProjectPersonsList.size(); itrProgProjPer++) {
                            Map mProgProjPer = (Map) mlProgramProjectPersonsList.get(itrProgProjPer);
                            String strPerName = (String) mProgProjPer.get(DomainConstants.SELECT_NAME);
                            if (!strPerName.startsWith("auto_")) {
                                slNotificationList.add(strPerName);
                            }
                        }
                        mChangeInfo.put("NotificationList", slNotificationList);
                    }
                } else {

                    String strObjWhere = "current == Implemented";
                    String strMQLCommand = "print bus " + strCRObjId + " select current.actual dump;";
                    String inStateSince = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                    Date dCRCurrentDate = new Date();
                    if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                        dCRCurrentDate = dateFormat.parse(inStateSince);
                    }
                    long lDifference = todaysDate.getTime() - dCRCurrentDate.getTime();
                    long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                    mChangeInfo.put("InStateSince", lInStateSince);
                    mChangeInfo.put("Delay", "N/A");
                    // Getting the Implemented CO and MCO connected to the CR
                    MapList mlCRConnectedImplementedCOandMCO = domCR.getRelatedObjects(context, // context
                            relCRPattern.getPattern(), // relationship pattern
                            typeCRPattern.getPattern(), // object pattern
                            slObjSelectStmts, // object selects
                            slSelectRelStmts, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 0, // recursion level
                            strObjWhere, // object where clause
                            null, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 0, // pageSize
                            null, null, null, null);
                    if (!mlCRConnectedImplementedCOandMCO.isEmpty()) {
                        String slCRChangeManager = domCR.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].to.name");
                        if (!slCRChangeManager.startsWith("auto_")) {
                            slNotificationList.add(slCRChangeManager);
                        }
                        mChangeInfo.put("NotificationList", slNotificationList);
                    }
                    strKey = strKey + ".Yes";
                    mChangeInfo.put("DueDate", "N/A");
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                    mChangeInfo.put("RequiredAction", getPropertyValue(context, strKey).concat(" (").concat(strCRState).concat(" state)"));
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END
                    mChangeInfo.put("NotificationList", slNotificationList);
                    mChangeInfo.put("CRConnectedCOandMCOList", mlCRConnectedCOandMCO);
                    mChangeInfo.put("CRImplementedCOandMCOList", mlCRConnectedImplementedCOandMCO);
                }
            }
        } catch (Exception e) {
            logger.error("Error in getChangeInformationForCR: ", e);

        }
        return mChangeInfo;
    }

    /**
     * @description: this method is used to get the Information of CA and MCA
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */

    public Map getChangeInformationForCAAndMCA(Context context, Map mChange, String strProgProjId) throws Exception {
        Map mChangeInfo = new HashMap();
        StringList slNotificationlist = new StringList();
        StringList slObjSelectStmts = new StringList();
        StringList slSelectRelStmts = new StringList();
        try {
            Date dTodayDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            String strTodaysDate = dateFormat.format(dTodayDate);
            Date todaysDate = dateFormat.parse(strTodaysDate);
            DomainObject domObj = new DomainObject();
            DomainObject domRouteTask = new DomainObject();
            DomainObject domRouteObj = new DomainObject();
            String strTaskAssignee = "";
            StringBuffer sbTaskDueDate = new StringBuffer();
            StringBuffer sbITDueDate = new StringBuffer();
            slObjSelectStmts.add(DomainConstants.SELECT_ID);
            slObjSelectStmts.add(DomainConstants.SELECT_CURRENT);
            slObjSelectStmts.add(DomainConstants.SELECT_NAME);

            slSelectRelStmts.add(DomainRelationship.SELECT_ID);

            String strPolicy = (String) mChange.get(DomainConstants.SELECT_POLICY);
            String strState = (String) mChange.get(DomainConstants.SELECT_CURRENT);
            String strStateAliasName = FrameworkUtil.reverseLookupStateName(context, strPolicy, strState);
            String strType = (String) mChange.get(DomainConstants.SELECT_TYPE);
            String strTypeAliasName = PropertyUtil.getAliasForAdmin(context, "Type", strType, false);
            String strKey = "emxFramework.E2E." + strTypeAliasName + "." + strStateAliasName;
            String strObjId = (String) mChange.get(DomainConstants.SELECT_ID);
            if (UIUtil.isNotNullAndNotEmpty(strObjId)) {
                domObj = DomainObject.newInstance(context, strObjId);
            }
            // If State of CA or MCA is "In Work"
            if (TigerConstants.STATE_PSS_MCA_INWORK.equalsIgnoreCase(strState)) {
                // Getting the Planned Due date of the CA and MCA
                // Modified for E2E issue TIGTK-3944 : Priyanka Salunke : 20-Jan-2017 : START
                StringList slCOMCOOwner = new StringList();
                String strPlannedEndDate = domObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PLANNEDENDDATE);
                if (UIUtil.isNotNullAndNotEmpty(strPlannedEndDate)) {
                    mChangeInfo.put("DueDate", strPlannedEndDate);
                } else {
                    mChangeInfo.put("DueDate", " ");
                }
                // Modified for E2E issue TIGTK-3944 : Priyanka Salunke : 20-Jan-2017 : END
                // Getting the Assignee of the CA and MCA
                String strAssignee = domObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.name");
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_CHANGEACTION)) {
                    String strMQLCommand = "print bus " + strObjId + " select current.actual dump;";
                    String inStateSince = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                    Date dCACurrentDate = new Date();
                    if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                        dCACurrentDate = dateFormat.parse(inStateSince);
                    }
                    long lDifference = todaysDate.getTime() - dCACurrentDate.getTime();
                    long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                    mChangeInfo.put("InStateSince", lInStateSince);
                    // ATTRIBUTE_PSS_PLANNEDENDDATE = Due Date
                    Date dCADueDate = new Date();
                    if (UIUtil.isNotNullAndNotEmpty(strPlannedEndDate)) {
                        dCADueDate = dateFormat.parse(strPlannedEndDate);
                    }
                    long lCADelay = todaysDate.getTime() - dCADueDate.getTime();
                    long lCADelayInDay = TimeUnit.DAYS.convert(lCADelay, TimeUnit.MILLISECONDS);
                    if (lCADelayInDay > 0) {
                        mChangeInfo.put("Delay", "" + lCADelayInDay);
                    } else {
                        mChangeInfo.put("Delay", "N/A");
                    }
                    slCOMCOOwner = domObj.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                    // Getting the Required Action
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                    mChangeInfo.put("RequiredAction", getPropertyValue(context, strKey).concat("  (").concat(strState).concat(" state)"));
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END
                    if (!slCOMCOOwner.isEmpty()) {
                        slNotificationlist.addAll(slCOMCOOwner);
                    }
                }
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {
                    String strMQLCommand = "print bus " + strObjId + " select current.actual dump;";
                    String inStateSince = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                    Date dMCACurrentDate = new Date();
                    if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                        dMCACurrentDate = dateFormat.parse(inStateSince);
                    }
                    long lDifference = todaysDate.getTime() - dMCACurrentDate.getTime();
                    long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                    Date dMCADueDate = new Date();
                    if (UIUtil.isNotNullAndNotEmpty(strPlannedEndDate)) {
                        dMCADueDate = dateFormat.parse(strPlannedEndDate);
                    }
                    long lMCADelay = todaysDate.getTime() - dMCADueDate.getTime();
                    long lMCADelayInDay = TimeUnit.DAYS.convert(lMCADelay, TimeUnit.MILLISECONDS);
                    mChangeInfo.put("InStateSince", lInStateSince);
                    if (lMCADelayInDay > 0) {
                        mChangeInfo.put("Delay", "" + lMCADelayInDay);
                    } else {
                        mChangeInfo.put("Delay", "N/A");
                    }

                    slCOMCOOwner = domObj.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                    // Getting the Required Action
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                    mChangeInfo.put("RequiredAction", getPropertyValue(context, strKey).concat("  (").concat(strState).concat(" state)"));
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END
                    if (!slCOMCOOwner.isEmpty()) {
                        slNotificationlist.addAll(slCOMCOOwner);
                    }
                }
                if (UIUtil.isNotNullAndNotEmpty(strAssignee)) {
                    if (!strAssignee.startsWith("auto_")) {
                        slNotificationlist.add(strAssignee);
                    }
                }
                mChangeInfo.put("NotificationList", slNotificationlist);
            }

            // If State of CA or MCA is "In Review"
            else if (TigerConstants.STATE_PSS_MCA_INREVIEW.equalsIgnoreCase(strState) || TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL.equalsIgnoreCase(strState)) {

                Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_OBJECT_ROUTE);
                Pattern tPattern = new Pattern(DomainConstants.TYPE_ROUTE);
                String strObjRouteWhere = "current != Complete";
                String strRelRouteWhere = "attribute[Route Base State] == state_InReview || attribute[Route Base State] == state_InApproval";
                String strTaskWhere = "current != 'Complete'";
                slSelectRelStmts.add("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASESTATE + "]");

                if (strType.equalsIgnoreCase(TigerConstants.TYPE_CHANGEACTION)) {
                    // Getting the Required Action
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                    mChangeInfo.put("RequiredAction", getPropertyValue(context, strKey).concat("  (").concat(strState).concat(" state)"));
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END
                }
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {
                    // Getting the Required Action
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : START
                    mChangeInfo.put("RequiredAction", getPropertyValue(context, strKey).concat("  (").concat(strState).concat(" state)"));
                    // Modified for E2E Issue : TIGTK - 3947 : Priyanka Salunke : 19-Jan-2017 : END
                }

                // Getting the connected CA and MCA Approval route
                MapList mlCAMCARoutes = domObj.getRelatedObjects(context, // context
                        relPattern.getPattern(), // relationship pattern
                        tPattern.getPattern(), // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        strObjRouteWhere, // object where clause
                        strRelRouteWhere, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 0, // pageSize
                        null, null, null, null);
                if (!mlCAMCARoutes.isEmpty()) {
                    for (int itrRoute = 0; itrRoute < mlCAMCARoutes.size(); itrRoute++) {
                        Map mRouteMap = (Map) mlCAMCARoutes.get(itrRoute);
                        String strRouteId = (String) mRouteMap.get(DomainConstants.SELECT_ID);
                        if (UIUtil.isNotNullAndNotEmpty(strRouteId)) {
                            domRouteObj = DomainObject.newInstance(context, strRouteId);
                        }

                        // Getting the Inbox Task connected to CR Approval route
                        MapList mlRouteTaskList = domRouteObj.getRelatedObjects(context, // context
                                DomainConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
                                DomainConstants.TYPE_INBOX_TASK, // object pattern
                                slObjSelectStmts, // object selects
                                slSelectRelStmts, // relationship selects
                                true, // to direction
                                false, // from direction
                                (short) 1, // recursion level
                                strTaskWhere, // object where clause
                                null, // relationship where clause
                                (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 0, // pageSize
                                null, null, null, null);
                        if (!mlRouteTaskList.isEmpty()) {
                            for (int itrRouteTask = 0; itrRouteTask < mlRouteTaskList.size(); itrRouteTask++) {
                                Map mRouteTask = (Map) mlRouteTaskList.get(itrRouteTask);
                                String strRouteTaskId = (String) mRouteTask.get(DomainConstants.SELECT_ID);
                                if (UIUtil.isNotNullAndNotEmpty(strRouteTaskId)) {
                                    domRouteTask = DomainObject.newInstance(context, strRouteTaskId);
                                }
                                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {
                                    String strInboxTaskDueDate = domRouteTask.getAttributeValue(context, TigerConstants.ATTRIBUTE_SCHEDULEDCOMPLETIONDATE);
                                    Date dInboxTaskDueDate = new Date();
                                    if (UIUtil.isNotNullAndNotEmpty(strInboxTaskDueDate)) {
                                        dInboxTaskDueDate = dateFormat.parse(strInboxTaskDueDate);
                                    }
                                    String strMQLCommand = "print bus " + strObjId + " select current.actual dump;";
                                    String inStateSince = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                                    Date dMCACurrentDate = new Date();
                                    if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                                        dMCACurrentDate = dateFormat.parse(inStateSince);
                                    }
                                    long lDifference = todaysDate.getTime() - dMCACurrentDate.getTime();
                                    long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);
                                    long lMCADelay = todaysDate.getTime() - dInboxTaskDueDate.getTime();
                                    long lMCADelayInDay = TimeUnit.DAYS.convert(lMCADelay, TimeUnit.MILLISECONDS);
                                    mChangeInfo.put("InStateSince", lInStateSince);
                                    if (lMCADelayInDay > 0) {
                                        mChangeInfo.put("Delay", "" + lMCADelayInDay);
                                    } else {
                                        mChangeInfo.put("Delay", "N/A");
                                    }
                                }
                                if (strType.equalsIgnoreCase(TigerConstants.TYPE_CHANGEACTION)) {
                                    String strInboxTaskDueDate = domRouteTask.getAttributeValue(context, TigerConstants.ATTRIBUTE_SCHEDULEDCOMPLETIONDATE);
                                    Date dInboxTaskDueDate = new Date();
                                    if (UIUtil.isNotNullAndNotEmpty(strInboxTaskDueDate)) {
                                        dInboxTaskDueDate = dateFormat.parse(strInboxTaskDueDate);
                                    }
                                    String strMQLCommand = "print bus " + strObjId + " select current.actual dump;";
                                    String inStateSince = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                                    Date dCACurrentDate = new Date();
                                    if (UIUtil.isNotNullAndNotEmpty(inStateSince)) {
                                        dCACurrentDate = dateFormat.parse(inStateSince);
                                    }
                                    long lDifference = todaysDate.getTime() - dCACurrentDate.getTime();
                                    long lInStateSince = TimeUnit.DAYS.convert(lDifference, TimeUnit.MILLISECONDS);

                                    long lCADelay = todaysDate.getTime() - dInboxTaskDueDate.getTime();
                                    long lCADelayInDay = TimeUnit.DAYS.convert(lCADelay, TimeUnit.MILLISECONDS);
                                    mChangeInfo.put("InStateSince", lInStateSince);
                                    if (lCADelayInDay > 0) {
                                        mChangeInfo.put("Delay", "" + lCADelayInDay);
                                    } else {
                                        mChangeInfo.put("Delay", "N/A");
                                    }
                                }

                                // Getting the Task Due Date
                                String strDueDate = domRouteTask.getInfo(context, "attribute[" + DomainConstants.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]");
                                sbITDueDate.append(strDueDate);
                                sbITDueDate.append(",");
                                // Getting the Task Name
                                String strTaskName = domRouteTask.getInfo(context, DomainConstants.SELECT_NAME);
                                // Getting the Task Assignee
                                strTaskAssignee = domRouteTask.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_PROJECT_TASK + "].to.name");
                                sbTaskDueDate.append(" ");
                                sbTaskDueDate.append(strTaskName);
                                sbTaskDueDate.append(" - ");
                                sbTaskDueDate.append(strDueDate);
                                if (!strTaskAssignee.startsWith("auto_")) {
                                    slNotificationlist.add(strTaskAssignee);
                                }
                            }
                        }
                    }
                }
                mChangeInfo.put("DueDate", sbTaskDueDate.toString());
                mChangeInfo.put("NotificationList", slNotificationlist);
                mChangeInfo.put("TaskDueDate", sbITDueDate.toString());
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getChangeInformationForCAAndMCA: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }

        return mChangeInfo;
    }

    /**
     * This Method is for getting the common object selects
     * @param context
     * @param args
     * @throws Exception
     * @author Priyanka Salunke
     */
    public StringList getCommonObjectSelects() throws Exception {
        StringList slObjSelectStmts = new StringList();
        try {

            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            slObjSelectStmts.addElement(DomainConstants.SELECT_CURRENT);
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            slObjSelectStmts.addElement(DomainConstants.SELECT_DESCRIPTION);
            slObjSelectStmts.addElement(DomainConstants.SELECT_TYPE);
            slObjSelectStmts.addElement(DomainConstants.SELECT_OWNER);
            slObjSelectStmts.addElement(DomainConstants.SELECT_ORIGINATOR);
            slObjSelectStmts.addElement(DomainConstants.SELECT_POLICY);
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getCommonObjectSelects: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return slObjSelectStmts;
    } // getCommonObjectSelects() end

    // Get Email Id on memebr info table column
    public Vector<String> getEmailId(Context context, String args[]) throws Exception {

        Vector vEmailId = new Vector();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        DomainRelationship domMemberRel = new DomainRelationship();
        try {
            MapList objectList = (MapList) programMap.get("objectList");
            if (!(objectList.isEmpty())) {
                Iterator itrMemberList = objectList.iterator();
                while (itrMemberList.hasNext()) {
                    StringBuffer sbEmailId = new StringBuffer();
                    Map mMemberMap = (Map) itrMemberList.next();
                    String strConnectionId = (String) mMemberMap.get("id[connection]");
                    if (UIUtil.isNotNullAndNotEmpty(strConnectionId)) {
                        domMemberRel = DomainRelationship.newInstance(context, strConnectionId);
                    }
                    String strManagerNames = domMemberRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_E2EMANAGERNAME);
                    StringList slMemberNameList = FrameworkUtil.split(strManagerNames, "|");
                    if (!(slMemberNameList.isEmpty())) {
                        boolean flag = false;
                        for (int i = 0; i < slMemberNameList.size(); i++) {
                            String strMemberName = (String) slMemberNameList.get(i);
                            // Get Person Id
                            String strMQLCommand = "print  Person" + " " + strMemberName + " select email dump;";
                            String strEmailId = MqlUtil.mqlCommand(context, strMQLCommand, true, false);
                            if (!(strEmailId.isEmpty())) {

                                if (flag) {
                                    sbEmailId.append(";");
                                    sbEmailId.append("<br/>");
                                    sbEmailId.append(strEmailId);
                                } else {
                                    sbEmailId.append(strEmailId);
                                    flag = true;
                                }
                            }
                        } // for end
                    } // slMemberNameList not empty if end
                    vEmailId.add(sbEmailId.toString());
                } // while end
            } // if end
        } catch (Exception e) {
            throw e;
        }
        return vEmailId;
    }

    /**
     * @description: This Method is used to get the Mail Format of the Change
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */

    public Map getChangeEmailBodyRows(Context context, MapList changeResult, String header, boolean isEscalation) throws Exception {

        String strMail = "";
        String startTR_odd = "<tr class='odd'>";
        String startTR_even = "<tr class='even'>";
        String startTR = startTR_odd;
        String endTR = "</tr>";
        String startTD = "<td width='100' height='50'>";
        String endTD = "</td>";
        String newLine = "<br>";
        String strColspan = (isEscalation) ? "7" : "6";

        Map mDataMap = new HashMap();

        try {

            StringList objectIds = new StringList();

            if (changeResult != null && !changeResult.isEmpty()) {
                for (int itr = 0; itr < changeResult.size(); itr++) {
                    Map mChange = (Map) changeResult.get(itr);
                    HashSet toList = new HashSet();
                    // To remove the duplicate from the mail
                    if (objectIds.contains((String) mChange.get(DomainConstants.SELECT_ID)))
                        continue;
                    objectIds.add((String) mChange.get(DomainConstants.SELECT_ID));
                    Map mChangeInfo = (Map) mChange.get("Change Info");
                    StringList slNotificationList = (StringList) mChangeInfo.get("NotificationList");
                    if (slNotificationList.isEmpty()) {
                        continue;
                    }
                    for (Object object : slNotificationList)
                        toList.add((String) object);
                    // Added for TIGTK-3948 : PS&VP : 30-Jan-2017 : START
                    Iterator<String> itrToList = toList.iterator();
                    while (itrToList.hasNext()) {

                        StringBuffer sb = new StringBuffer();
                        String strTOObject = (String) itrToList.next();
                        // Added for TIGTK-3948 : PS&VP : 30-Jan-2017 : END
                        String strChangeType = (String) mChange.get(DomainConstants.SELECT_TYPE);
                        String strChangeId = (String) mChange.get(DomainConstants.SELECT_ID);
                        String strChangeName = (String) mChange.get(DomainConstants.SELECT_NAME);
                        String strDescription = (String) mChange.get(DomainConstants.SELECT_DESCRIPTION);
                        String strRequiredAction = (String) mChangeInfo.get("RequiredAction");
                        long strInStateSince = (long) mChangeInfo.get("InStateSince");
                        // Added for TIGTK-3967 : Rutuja Ektapure: 31-Jan-2017 : START
                        String strDueDate = (String) mChangeInfo.get("TaskDueDate");
                        String strDueDateNew = DomainConstants.EMPTY_STRING;
                        if (UIUtil.isNullOrEmpty(strDueDate)) {
                            strDueDateNew = (String) mChangeInfo.get("DueDate");
                            if (!strDueDateNew.equalsIgnoreCase("N/A") && UIUtil.isNotNullAndNotEmpty(strDueDateNew)) {
                                strDueDateNew = strDueDateNew.substring(0, strDueDateNew.indexOf(" "));
                            }
                        } else {
                            StringList sLDueDate = FrameworkUtil.split(strDueDate, ",");
                            if (sLDueDate.size() > 1) {
                                sLDueDate.sort();
                                strDueDateNew = (String) sLDueDate.get(1);
                                if (UIUtil.isNotNullAndNotEmpty(strDueDateNew)) {
                                    strDueDateNew = strDueDateNew.substring(0, strDueDateNew.indexOf(" "));
                                }
                            }
                        }
                        if (!strDueDateNew.equalsIgnoreCase("N/A") && UIUtil.isNotNullAndNotEmpty(strDueDateNew)) {
                            SimpleDateFormat format1 = new SimpleDateFormat("MM/dd/yyyy");
                            SimpleDateFormat format2 = new SimpleDateFormat("dd.MM.yyyy");
                            Date date = format1.parse(strDueDateNew);
                            strDueDateNew = format2.format(date);
                        }

                        // Added for TIGTK-3967 : Rutuja Ektapure: 31-Jan-2017 : END
                        String strDelay = (String) mChangeInfo.get("Delay");
                        strChangeName = getObjectLinkHTML(context, strChangeName, strChangeId);
                        if (strChangeType.equals(TigerConstants.TYPE_CHANGEACTION) || strChangeType.equals(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {
                            String relatedChangeName = (String) mChange.get("RelatedChangeNumber");
                            String relatedChangeId = (String) mChange.get("RelatedChangeId");
                            strChangeName += "<br>related to<br>&nbsp;&nbsp;" + getObjectLinkHTML(context, relatedChangeName, relatedChangeId);
                        }

                        sb.append(startTR);
                        sb.append(startTD);
                        sb.append(strChangeName);
                        sb.append(endTD);
                        sb.append(startTD);
                        sb.append(strDescription);
                        sb.append(endTD);
                        sb.append(startTD);
                        sb.append(strRequiredAction);
                        sb.append(endTD);
                        // Added for TIGTK-3948 : PS&VP : 30-Jan-2017 : START
                        if (isEscalation) {
                            sb.append(startTD);
                            sb.append(strTOObject);
                            sb.append(endTD);
                        }
                        // Added for TIGTK-3948 : PS&VP : 30-Jan-2017 : END
                        // Added for E2E issue TIGTK-3945 : Priyanka Salunke : 17-Jan-2017 : START
                        if (strInStateSince == 1 || strInStateSince == 0) {
                            sb.append(startTD);
                            sb.append(strInStateSince);
                            sb.append(" Day");
                            sb.append(endTD);
                        } else {
                            sb.append(startTD);
                            sb.append(strInStateSince);
                            sb.append(" Days");
                            sb.append(endTD);
                        }
                        // Added for E2E issue TIGTK-3945 : Priyanka Salunke : 17-Jan-2017 : END
                        sb.append(startTD);
                        sb.append(strDueDateNew);
                        sb.append(endTD);
                        // Added for E2E issue TIGTK-3945 : Priyanka Salunke : 17-Jan-2017 : START
                        if (strDelay.equalsIgnoreCase("N/A")) {
                            sb.append(startTD);
                            sb.append("" + strDelay);
                            sb.append(endTD);
                            sb.append(endTR);
                        } else if (strDelay.equalsIgnoreCase("1") || strDelay.equalsIgnoreCase("0")) {
                            sb.append(startTD);
                            sb.append("" + strDelay);
                            sb.append(" Day");
                            sb.append(endTD);
                            sb.append(endTR);
                        } else {
                            sb.append(startTD);
                            sb.append("" + strDelay);
                            sb.append(" Days");
                            sb.append(endTD);
                            sb.append(endTR);
                        }
                        // Added for E2E issue TIGTK-3945 : Priyanka Salunke : 17-Jan-2017 : END
                        startTR = (startTR.equals(startTR_odd)) ? startTR_even : startTR_odd;
                        // Added for TIGTK-3948 : PS&VP : 30-Jan-2017 : START
                        if (!mDataMap.containsKey(strTOObject)) {
                            StringBuffer sbStoredValue = new StringBuffer();
                            sbStoredValue.append("<tr>");
                            sbStoredValue.append("<th colspan='" + strColspan + "' class='changeType'>");
                            sbStoredValue.append(header);
                            sbStoredValue.append("</th>");
                            sbStoredValue.append(endTR);
                            sbStoredValue.append(sb);
                            mDataMap.put(strTOObject, sbStoredValue.toString());
                        } else {
                            StringBuffer sbStoredValue = new StringBuffer();
                            sbStoredValue.append((String) mDataMap.get(strTOObject));
                            sbStoredValue.append(sb);

                            mDataMap.put(strTOObject, sbStoredValue.toString());
                        }

                    } // For end
                      // Added for TIGTK-3948 : PS&VP : 30-Jan-2017 : END
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getChangeEmailBodyRows: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return mDataMap;
    }

    public String getObjectLinkHTML(Context context, String text, String objectId) throws Exception {

        // Modified by Suchit G. for TIGTK-3957 on Date: 07/02/2017: Start
        String link = XSSUtil.encodeForHTMLAttribute(context, getObjectLink(context, objectId));
        // Modified by Suchit G. for TIGTK-3957 on Date: 07/02/2017: End
        if (link != null && link.length() > 0) {
            // Modified by Suchit G. for TIGTK-3957 on Date: 07/02/2017: Start
            link = "<a href=\"" + link + "\" target = \"_blank\">" + text + "</a>";
            // Modified by Suchit G. for TIGTK-3957 on Date: 07/02/2017: End
        } else {
            link = text;
        }
        return (link);
        // return text;
    }

    public String getObjectLink(Context context, String objectId) throws Exception {

        String link = "";
        // Modified by Suchit G. for TIGTK-3957 on Date:01/02/2017: Start
        String baseURL = STR_SERVER_URL + "/common/emxNavigator.jsp";
        // Modified by Suchit G. for TIGTK-3957 on Date:01/02/2017: End

        if (baseURL != null && baseURL.length() > 0) {
            link = baseURL + "?objectId=" + objectId;
        }

        return (link);
    }

    /**
     * @description: This
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     * @author Priyanka Salunke
     */

    public MapList sendProgramProjectChangeStatus(Context context, String[] args, StringList slChangeType, String strE2E) throws Exception {
        String strProgramProjectId = args[0];
        boolean isEscalation = args[1].equalsIgnoreCase("true");
        // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017 : START

        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
        MapList mlResultList = new MapList();
        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END

        boolean isContextPushed = false;
        try {

            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

            isContextPushed = true;
            Map<String, String> resultMap = getProjectMailBody(context, strProgramProjectId, isEscalation, slChangeType);
            if (isContextPushed) {
                ContextUtil.popContext(context);
                isContextPushed = false;
            }
            // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017 : END
            // Added for TIGTK-3948 : PS&VP : 30-Jan-2017 : START
            Map mResultMap = null;

            for (java.util.Map.Entry<String, String> entrySet : resultMap.entrySet()) {
                // Finf bug issue : TIGTK-4401 : Priyanka Salunke : 10-Feb-2017
                mResultMap = new HashMap();
                String strEmailToUser = (String) entrySet.getKey();
                String strEmailToRowData = (String) entrySet.getValue();

                StringList toList = new StringList();
                toList.add(strEmailToUser);

                StringBuffer sbSubject = new StringBuffer();
                int Size = slChangeType.size();
                for (Object obj : slChangeType) {
                    String strSubject = (String) obj;
                    if (Size > 1) {
                        // Find Bug : Dodgy Code : PS : 21-March-2017
                        sbSubject.append(strSubject.substring(2, strSubject.length()));
                        sbSubject.append("/");
                    } else {
                        sbSubject.append(strSubject.substring(2, strSubject.length()));
                    }
                    Size--;
                }

                if (isEscalation && strE2E.equalsIgnoreCase("Escalation")) {
                    mResultMap.put("subject", "[TIGER] [" + sbSubject.toString() + "] Changes Escalation");
                    StringList slManagerList = getProjectManagerEscalationList(context, strProgramProjectId, toList);
                    mResultMap.put("toList", slManagerList);
                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                    mResultMap.put("messageType", "Escalation");
                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                } else if (strE2E.equalsIgnoreCase("Notification")) {
                    mResultMap.put("subject", "[TIGER] [" + sbSubject.toString() + "] Changes Notification");
                    mResultMap.put("toList", toList);
                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                    mResultMap.put("messageType", "Notification");
                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                }

                mResultMap.put("messageText", strEmailToRowData);
                mResultMap.put("messageHTML", strEmailToRowData);
                mResultMap.put("fromAgent", context.getUser());
                // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                mlResultList.add(mResultMap);
            }
            // Added for TIGTK-3948 : PS&VP : 30-Jan-2017 : END
            return mlResultList;
            // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in sendProgramProjectChangeStatus: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017 : START
        finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }
        // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017 : END
    }

    /**
     * @description: This Method is used to get the all change of the single program project
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     * @author Priyanka Salunke
     */
    public Map getProjectMailBody(Context context, String strProgramProjectId, boolean isEscalation, StringList ChangeType) throws Exception {

        Map<String, String> returnMap = new HashMap();

        try {
            DomainObject domProgramProjectObject = new DomainObject();
            Map resultMap = new HashMap();
            String strColspan = (isEscalation) ? "7" : "6";
            // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017
            Map mAllChangeMap = getProgramProjectInfo(context, strProgramProjectId);
            if (!mAllChangeMap.isEmpty() && !ChangeType.isEmpty()) {
                for (Object object : ChangeType) {
                    Map<String, MapList> mTempChange = getTypeOfChange(context, (String) object, mAllChangeMap);
                    // Added for Find Bug Issue TIGTK-3955 : Priyanka Salunke : 23-Jan-2017 : START
                    for (java.util.Map.Entry<String, MapList> entrySet : mTempChange.entrySet()) {
                        String strKey = (String) entrySet.getKey();
                        MapList mlResultChange = (MapList) resultMap.get(strKey);
                        MapList mlTempChange = (MapList) entrySet.getValue();
                        if (mlResultChange == null) {
                            mlResultChange = new MapList();
                        }
                        mlResultChange.addAll(mlTempChange);
                        resultMap.put(strKey, mlResultChange);
                    }
                    // Added for Find Bug Issue TIGTK-3955 : Priyanka Salunke : 23-Jan-2017 : END
                }
            }
            // Program PRoject Domain Object
            if (!(strProgramProjectId.isEmpty())) {
                domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectId);
            }
            // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017
            MapList mlCRList = (MapList) resultMap.get("CR");
            MapList mlCOList = (MapList) resultMap.get("CO");
            MapList mlCAList = (MapList) resultMap.get("CA");
            MapList mlMCOList = (MapList) resultMap.get("MCO");
            MapList mlMCAList = (MapList) resultMap.get("MCA");
            MapList mlCNList = (MapList) resultMap.get("CN");

            StringBuffer sbMailBody = new StringBuffer();
            // Modified For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017
            // Added for TIGTK-3948 : PS&VP : 30-Jan-2017 : START
            sbMailBody.append("<tr class='projectName'><th colspan='" + strColspan + "'>");
            sbMailBody.append(domProgramProjectObject.getInfo(context, DomainConstants.SELECT_NAME));
            sbMailBody.append("</th>");
            sbMailBody.append("</tr>");

            // Change Request
            if (mlCRList != null && !mlCRList.isEmpty()) {
                returnMap = getChangeEmailBodyRows(context, mlCRList, "Change Request", isEscalation);
            }

            // Change Order
            if (mlCOList != null && !mlCOList.isEmpty()) {
                Map<String, String> mChangeOrderBodyMap = getChangeEmailBodyRows(context, mlCOList, "Change Order", isEscalation);

                for (java.util.Map.Entry<String, String> entrySet : mChangeOrderBodyMap.entrySet()) {
                    String strEmailToUser = (String) entrySet.getKey();
                    String strEmailToRowData = (String) entrySet.getValue();
                    if (returnMap != null && !returnMap.isEmpty()) {
                        if (returnMap.containsKey(strEmailToUser)) {
                            StringBuffer sbStoredValue = new StringBuffer();
                            sbStoredValue.append((String) returnMap.get(strEmailToUser));
                            sbStoredValue.append(strEmailToRowData);

                            returnMap.put(strEmailToUser, sbStoredValue.toString());
                        } else {
                            returnMap.put(strEmailToUser, strEmailToRowData);
                        }
                    } else {
                        returnMap.putAll(mChangeOrderBodyMap);
                    }
                }
            }

            // Change Action
            if (mlCAList != null && !mlCAList.isEmpty()) {
                Map<String, String> mChangeActionBodyMap = getChangeEmailBodyRows(context, mlCAList, "Change Action", isEscalation);

                for (java.util.Map.Entry<String, String> entrySet : mChangeActionBodyMap.entrySet()) {
                    String strEmailToUser = (String) entrySet.getKey();
                    String strEmailToRowData = (String) entrySet.getValue();
                    if (returnMap != null && !returnMap.isEmpty()) {
                        if (returnMap.containsKey(strEmailToUser)) {
                            StringBuffer sbStoredValue = new StringBuffer();
                            sbStoredValue.append((String) returnMap.get(strEmailToUser));
                            sbStoredValue.append(strEmailToRowData);

                            returnMap.put(strEmailToUser, sbStoredValue.toString());
                        } else {
                            returnMap.put(strEmailToUser, strEmailToRowData);
                        }
                    } else {
                        returnMap.putAll(mChangeActionBodyMap);
                    }
                }
            }

            // Mfg Change Order
            if (mlMCOList != null && !mlMCOList.isEmpty()) {
                Map<String, String> mMfgChangeOrderBodyMap = getChangeEmailBodyRows(context, mlMCOList, "Mfg Change Order", isEscalation);
                for (java.util.Map.Entry<String, String> entrySet : mMfgChangeOrderBodyMap.entrySet()) {
                    String strEmailToUser = (String) entrySet.getKey();
                    String strEmailToRowData = (String) entrySet.getValue();
                    if (returnMap != null && !returnMap.isEmpty()) {
                        if (returnMap.containsKey(strEmailToUser)) {
                            StringBuffer sbStoredValue = new StringBuffer();
                            sbStoredValue.append((String) returnMap.get(strEmailToUser));
                            sbStoredValue.append(strEmailToRowData);

                            returnMap.put(strEmailToUser, sbStoredValue.toString());
                        } else {
                            returnMap.put(strEmailToUser, strEmailToRowData);
                        }
                    } else {
                        returnMap.putAll(mMfgChangeOrderBodyMap);
                    }
                }
            }

            // Mfg Change Action
            if (mlMCAList != null && !mlMCAList.isEmpty()) {
                Map<String, String> mMfgChangeActionBodyMap = getChangeEmailBodyRows(context, mlMCAList, "Mfg Change Action", isEscalation);

                for (java.util.Map.Entry<String, String> entrySet : mMfgChangeActionBodyMap.entrySet()) {
                    String strEmailToUser = (String) entrySet.getKey();
                    String strEmailToRowData = (String) entrySet.getValue();
                    if (returnMap != null && !returnMap.isEmpty()) {
                        if (returnMap.containsKey(strEmailToUser)) {
                            StringBuffer sbStoredValue = new StringBuffer();
                            sbStoredValue.append((String) returnMap.get(strEmailToUser));
                            sbStoredValue.append(strEmailToRowData);

                            returnMap.put(strEmailToUser, sbStoredValue.toString());
                        } else {
                            returnMap.put(strEmailToUser, strEmailToRowData);
                        }
                    } else {
                        returnMap.putAll(mMfgChangeActionBodyMap);
                    }
                }
            }

            // Change Notice
            if (mlCNList != null && !mlCNList.isEmpty()) {
                Map<String, String> mChangeNoticeBodyMap = getChangeEmailBodyRows(context, mlCNList, "Change Notice", isEscalation);
                for (java.util.Map.Entry<String, String> entrySet : mChangeNoticeBodyMap.entrySet()) {
                    String strEmailToUser = (String) entrySet.getKey();
                    String strEmailToRowData = (String) entrySet.getValue();
                    if (returnMap != null && !returnMap.isEmpty()) {
                        if (returnMap.containsKey(strEmailToUser)) {
                            StringBuffer sbStoredValue = new StringBuffer();
                            sbStoredValue.append((String) returnMap.get(strEmailToUser));
                            sbStoredValue.append(strEmailToRowData);

                            returnMap.put(strEmailToUser, sbStoredValue.toString());
                        } else {
                            returnMap.put(strEmailToUser, strEmailToRowData);
                        }
                    } else {
                        returnMap.putAll(mChangeNoticeBodyMap);
                    }
                }
            }

            for (java.util.Map.Entry<String, String> entrySet : returnMap.entrySet()) {
                String strEmailToUser = (String) entrySet.getKey();
                String strEmailToRowData = (String) entrySet.getValue();

                StringBuffer sbStoredValue = new StringBuffer();
                sbStoredValue.append((String) sbMailBody.toString());
                sbStoredValue.append(strEmailToRowData);
                // Modified For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017

                returnMap.put(strEmailToUser, sbStoredValue.toString());
            }
            // Added for TIGTK-3948 : PS&VP : 30-Jan-2017 : END
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProjectMailBody: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return returnMap;
    }

    public StringList getProjectManagerEscalationList(Context context, String strProgramProjectId, StringList slToList) throws Exception {
        StringList slManagerList = new StringList();
        DomainObject domProgProj = new DomainObject();
        StringList slObjSelectStmts = new StringList();
        StringList slSelectRelStmts = new StringList();
        try {
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectId)) {
                domProgProj = DomainObject.newInstance(context, strProgramProjectId);
            }
            slObjSelectStmts.add(DomainConstants.SELECT_NAME);
            slSelectRelStmts.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_E2EMANAGERNAME + "]");

            MapList mlConnectedMembers = domProgProj.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, // relationship pattern
                    DomainConstants.TYPE_PERSON, // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, // relationship where clause
                    (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    null, null, null, null);
            if (!mlConnectedMembers.isEmpty()) {
                for (int itr = 0; itr < mlConnectedMembers.size(); itr++) {
                    Map mConnectedMember = (Map) mlConnectedMembers.get(itr);
                    String strMemberName = (String) mConnectedMember.get(DomainConstants.SELECT_NAME);
                    if (slToList.contains(strMemberName)) {
                        String strManager = (String) mConnectedMember.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_E2EMANAGERNAME + "]");
                        StringList slManager = FrameworkUtil.split(strManager, "|");
                        slManagerList.addAll(slManager);
                    }
                }
            }
            HashSet managerSet = new HashSet();
            for (Object object : slManagerList) {
                managerSet.add((String) object);
            }
            slManagerList = new StringList();
            for (Object object : managerSet) {
                slManagerList.add((String) object);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProjectManagerEscalationList: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return slManagerList;
    }

    public String getMailCSS() {
        StringBuffer css = new StringBuffer();
        css.append("<style>");
        css.append("table{  width: 100%; font-size:15px; border-collapse: collapse; }");
        css.append("th, td { border: 1px solid black; }");
        css.append(".description, .ra{ width:25%; }");
        css.append(".name{ width:10%; }");
        css.append(".delay, .InStateSince{ width:5%; }");
        css.append(".projectName{ background-color:#81A0AD; color:#ffffff; font-family: 'Courier New'; font-size:20px; text-align:center;   }");
        css.append(".changeType{ background-color:#094C8C; color:#ffffff; font-family: 'Courier New'; font-size:15px; text-align:center;  }");

        css.append(".odd{ background-color:#D6EAF8; }");
        css.append(".even{ background-color:#FFFFFF;}");
        css.append(".mainHeader{ background-color:#CCD1D1;}");
        css.append("</style>");
        return css.toString();
    }

    /**
     * @description: This Method is used to check that the Change is OpenedChnage or not
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     * @author Priyanka Salunke
     */
    public boolean isChangeOpened(Context context, Map mConnectedPCMData, Map mProgramProjectMap) throws Exception {
        boolean isOpened = false;
        try {

            String strType = (String) mConnectedPCMData.get(DomainConstants.SELECT_TYPE);
            String strCurrentState = (String) mConnectedPCMData.get(DomainConstants.SELECT_CURRENT);

            // Change Request
            if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) && ((!(strCurrentState.equalsIgnoreCase("Complete"))) || (!(strCurrentState.equalsIgnoreCase("Rejected"))))) {
                isOpened = true;
            }
            // Change Order Or Mfg Change Order
            if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER))
                    && ((!(strCurrentState.equalsIgnoreCase("Implemented"))) || (!(strCurrentState.equalsIgnoreCase("Rejected"))) || (!(strCurrentState.equalsIgnoreCase("Cancelled"))))) {
                isOpened = true;
            }
            // Change Action Or Mfg Change Action
            if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION) || strType.equalsIgnoreCase(TigerConstants.TYPE_CHANGEACTION))
                    && ((!(strCurrentState.equalsIgnoreCase("Complete"))))) {
                isOpened = true;
            }
            // Change Notice
            if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGENOTICE)) && ((!(strCurrentState.equalsIgnoreCase("Fully Integrated"))) || (!(strCurrentState.equalsIgnoreCase("Cancelled"))))) {
                isOpened = true;
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in isChangeOpened: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return isOpened;
    } // isChangeOpened() end

    /**
     * @description: This Method is used to check that the Change is RemainderChnage or not
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     * @author Priyanka Salunke
     */
    public boolean isChangeRemainder(Context context, Map mConnectedPCMData, Map mProgramProjectMap) throws Exception {

        boolean isRemainder = false;
        try {

            String strType = (String) mConnectedPCMData.get(DomainConstants.SELECT_TYPE);
            String strCurrentState = (String) mConnectedPCMData.get(DomainConstants.SELECT_CURRENT);
            Map mChangeInfo = (Map) mConnectedPCMData.get("Change Info");

            // Change Request
            if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) && (strCurrentState.equalsIgnoreCase("In Process"))) {
                // CRImplementedCOandMCOList CRConnectedCOandMCOList
                MapList mlCRConnectedCOandMCOList = (MapList) mChangeInfo.get("CRConnectedCOandMCOList");
                MapList mlCRImplementedCOandMCOList = (MapList) mChangeInfo.get("CRImplementedCOandMCOList");
                // Check CO and MCO is connected to CR or not and Check CO and MCO of CR is in Implemented state
                if (((mlCRConnectedCOandMCOList == null) || (mlCRConnectedCOandMCOList.isEmpty())) || ((mlCRImplementedCOandMCOList != null) && (!(mlCRImplementedCOandMCOList.isEmpty())))) {
                    isRemainder = true;
                }
            } // Change Request End

            // Change Notice
            StringList slCNStatesList = new StringList();
            slCNStatesList.add("Prepare");
            slCNStatesList.add("In Review");
            slCNStatesList.add("In Transfer");
            slCNStatesList.add("Not Fully Integrated");
            slCNStatesList.add("Transfer Error");
            if ((strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGENOTICE)) && (slCNStatesList.contains(strCurrentState))) {
                // In State Since
                long lInStateSince = (long) mChangeInfo.get("InStateSince");
                // CN Limit
                String PSS_E2EOpenedCNDuration = (String) mProgramProjectMap.get("PSS_E2EOpenedCNDuration");
                Long lCNLimit = Long.parseLong(PSS_E2EOpenedCNDuration);
                // If CN is in current state more than limit define the Remainder = true
                if (null != lCNLimit) {
                    if (lInStateSince > lCNLimit) {
                        isRemainder = true;
                    }
                }

            } // Change Notice End
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in isChangeRemainder: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return isRemainder;
    } // isChangeRemainder() end

    /**
     * @description: This method is used to check the Change i.e.CR/CO/MCO/CA/MCA/CN is delayed or not.
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */

    public boolean isChangeDelayed(Context context, Map mConnectedPCMData, Map mProgProjAttrDetails) throws Exception {
        String strChangeId = "";
        String strChangeType = "";
        String strChangeState = "";
        boolean isDelayed = false;
        long lInStateSince = 0;
        try {
            if (!mConnectedPCMData.isEmpty() && !mProgProjAttrDetails.isEmpty()) {
                strChangeType = (String) mConnectedPCMData.get(DomainConstants.SELECT_TYPE);
                if (UIUtil.isNotNullAndNotEmpty(strChangeType)) {
                    Map mChangeInfo = new HashMap<>();
                    // Type is Change Request
                    if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strChangeType)) {
                        strChangeState = (String) mConnectedPCMData.get(DomainConstants.SELECT_CURRENT);
                        // State is Submit
                        if ("Submit".equalsIgnoreCase(strChangeState)) {
                            mChangeInfo = (Map) mConnectedPCMData.get("Change Info");
                            if (!mChangeInfo.isEmpty()) {
                                lInStateSince = (long) mChangeInfo.get("InStateSince");
                            }
                            String strCRDurationAtSubmitState = (String) mProgProjAttrDetails.get(TigerConstants.ATTRIBUTE_PSS_E2ECRDURATIONATSUBMITSTATE);
                            long lCRDurationAtSubmitState = Long.parseLong(strCRDurationAtSubmitState);
                            if (lInStateSince > lCRDurationAtSubmitState) {
                                isDelayed = true;
                            }
                        }
                        // State is Evaluate
                        if ("Evaluate".equalsIgnoreCase(strChangeState) || "In Review".equalsIgnoreCase(strChangeState)) {
                            // Date ITDate = new Date();
                            mChangeInfo = (Map) mConnectedPCMData.get("Change Info");
                            if (!mChangeInfo.isEmpty()) {
                                String strTaskDate = (String) mChangeInfo.get("TaskDueDate");
                                StringList slTaskDate = FrameworkUtil.split(strTaskDate, ",");
                                if (!slTaskDate.isEmpty()) {
                                    for (int itr = 0; itr < slTaskDate.size(); itr++) {
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                                        String strITDate = (String) slTaskDate.get(itr);
                                        if (UIUtil.isNotNullAndNotEmpty(strITDate)) {
                                            // Find Bug : Dodgy Code : PS :21-March-2017
                                            Date ITDate = dateFormat.parse(strITDate);
                                            Date dTodayDate = new Date();
                                            String strTodaysDate = dateFormat.format(dTodayDate);
                                            Date todaysDate = new Date();
                                            if (UIUtil.isNotNullAndNotEmpty(strTodaysDate)) {
                                                todaysDate = (Date) dateFormat.parse(strTodaysDate);
                                            }
                                            if (todaysDate.compareTo(ITDate) > 0) {
                                                isDelayed = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Type is Change Order/ Manufacturing Change Order
                    else if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(strChangeType) || TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER.equalsIgnoreCase(strChangeType)) {
                        long lDurationAtPrepareState = 0;
                        strChangeState = (String) mConnectedPCMData.get(DomainConstants.SELECT_CURRENT);
                        // State is Prepare
                        if ("Prepare".equalsIgnoreCase(strChangeState)) {
                            mChangeInfo = (Map) mConnectedPCMData.get("Change Info");
                            if (!mChangeInfo.isEmpty()) {
                                lInStateSince = (long) mChangeInfo.get("InStateSince");
                            }
                            String strCODurationAtPrepareState = (String) mProgProjAttrDetails.get(TigerConstants.ATTRIBUTE_PSS_E2ECODURATIONATPREPARESTATE);
                            String strMCODurationAtPrepareState = (String) mProgProjAttrDetails.get(TigerConstants.ATTRIBUTE_PSS_E2EMCODURATIONATPREPARESTATE);
                            if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(strChangeType)) {
                                lDurationAtPrepareState = Long.parseLong(strCODurationAtPrepareState);

                            }
                            if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER.equalsIgnoreCase(strChangeType)) {
                                lDurationAtPrepareState = Long.parseLong(strMCODurationAtPrepareState);
                            }
                            if (lInStateSince > lDurationAtPrepareState) {
                                isDelayed = true;
                            }
                        }
                    }

                    // Type is Change Action/ Manufacturing Change Action
                    else if (TigerConstants.TYPE_CHANGEACTION.equalsIgnoreCase(strChangeType) || TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strChangeType)) {
                        strChangeState = (String) mConnectedPCMData.get(DomainConstants.SELECT_CURRENT);
                        String strPlannedEndDate = "";
                        // Date PlannedEndDate = new Date();
                        // State in In Work
                        if ("In Work".equalsIgnoreCase(strChangeState)) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                            mChangeInfo = (Map) mConnectedPCMData.get("Change Info");
                            if (!mChangeInfo.isEmpty()) {
                                strPlannedEndDate = (String) mChangeInfo.get("DueDate");
                            }
                            if (UIUtil.isNotNullAndNotEmpty(strPlannedEndDate)) {
                                // Find Bug : Dodgy Code : PS :21-March-2017
                                Date PlannedEndDate = dateFormat.parse(strPlannedEndDate);
                                Date dTodayDate = new Date();
                                String strTodaysDate = dateFormat.format(dTodayDate);
                                Date todaysDate = new Date();
                                if (UIUtil.isNotNullAndNotEmpty(strTodaysDate)) {
                                    todaysDate = dateFormat.parse(strTodaysDate);
                                }
                                if (todaysDate.compareTo(PlannedEndDate) > 0) {
                                    isDelayed = true;
                                }
                            }
                        }

                        // // State in In Review/In Approval
                        if ("In Review".equalsIgnoreCase(strChangeState) || "In Approval".equalsIgnoreCase(strChangeState)) {
                            Date ITDate = new Date();
                            mChangeInfo = (Map) mConnectedPCMData.get("Change Info");
                            if (!mChangeInfo.isEmpty()) {
                                String strTaskDate = (String) mChangeInfo.get("TaskDueDate");
                                StringList slTaskDate = FrameworkUtil.split(strTaskDate, ",");
                                if (!slTaskDate.isEmpty()) {
                                    for (int itr = 0; itr < slTaskDate.size(); itr++) {
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                                        String strITDate = (String) slTaskDate.get(itr);
                                        if (UIUtil.isNotNullAndNotEmpty(strITDate)) {
                                            ITDate = dateFormat.parse(strITDate);
                                            Date dTodayDate = new Date();
                                            String strTodaysDate = dateFormat.format(dTodayDate);
                                            Date todaysDate = new Date();
                                            if (UIUtil.isNotNullAndNotEmpty(strTodaysDate)) {
                                                todaysDate = dateFormat.parse(strTodaysDate);
                                            }
                                            if (todaysDate.compareTo(ITDate) > 0) {
                                                isDelayed = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Type is Change Notice
                    else if (TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(strChangeType)) {
                        Date ITDate = new Date();
                        // State is In Review
                        strChangeState = (String) mConnectedPCMData.get(DomainConstants.SELECT_CURRENT);
                        if ("In Review".equalsIgnoreCase(strChangeState)) {
                            mChangeInfo = (Map) mConnectedPCMData.get("Change Info");
                            if (!mChangeInfo.isEmpty()) {
                                String strTaskDate = (String) mChangeInfo.get("TaskDueDate");
                                StringList slTaskDate = FrameworkUtil.split(strTaskDate, ",");
                                if (!slTaskDate.isEmpty()) {
                                    for (int itr = 0; itr < slTaskDate.size(); itr++) {
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                                        String strITDate = (String) slTaskDate.get(itr);
                                        if (UIUtil.isNotNullAndNotEmpty(strITDate)) {
                                            ITDate = dateFormat.parse(strITDate);
                                            Date dTodayDate = new Date();
                                            String strTodaysDate = dateFormat.format(dTodayDate);
                                            Date todaysDate = new Date();
                                            if (UIUtil.isNotNullAndNotEmpty(strTodaysDate)) {
                                                todaysDate = dateFormat.parse(strTodaysDate);
                                            }
                                            if (todaysDate.compareTo(ITDate) > 0) {
                                                isDelayed = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in isChangeDelayed: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in isChangeDelayed: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw new Exception(e);
        }
        // Find Bug Issue TIGTK-3955 : Priyanka Salunke : 25-Jan-2017 : START
        return isDelayed;
        // Find Bug Issue TIGTK-3955 : Priyanka Salunke : 25-Jan-2017 : END

    }

    /**
     * @description: This method is used to get the ProgramProject Configuration
     * @param context
     * @param args
     * @return void
     * @throws Exception
     * @author Priyanka Salunke
     */
    public Map getProgramProjectConfiguration(Context context, String strProgramProjectId, String strCroneType, String strE2E) throws Exception {

        Map mProgramProjectConfigurationMap = new HashMap();

        DomainObject domProgramProjectObject = new DomainObject();
        StringList slChangeList = new StringList();
        try {
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectId)) {
                domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectId);
            }
            // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017
            // Map mProgramProjectAttributeValueMap = new HashMap();
            Map mProgramProjectAttributeValueMap = domProgramProjectObject.getAttributeMap(context);
            if ("Escalation".equalsIgnoreCase(strE2E)) {
                String isEscalatioOpenedValue = (String) mProgramProjectAttributeValueMap.get("PSS_E2EEscalationOpenedChgsFrequency");
                String isEscalatioDelayedValue = (String) mProgramProjectAttributeValueMap.get("PSS_E2EEscalationDelayedChgsFrequency");
                if (strCroneType.equalsIgnoreCase(isEscalatioOpenedValue)) {
                    slChangeList.add("isEscalationOpened");
                }
                if (strCroneType.equalsIgnoreCase(isEscalatioDelayedValue)) {
                    slChangeList.add("isEscalationDelayed");
                }
            }
            if ("Notification".equalsIgnoreCase(strE2E)) {
                String isOpenedValue = (String) mProgramProjectAttributeValueMap.get("PSS_E2ENotificationOpenedChgsFrequency");
                String isDelayedValue = (String) mProgramProjectAttributeValueMap.get("PSS_E2ENotificationDelayedChgsFrequency");
                String isRemainderValue = (String) mProgramProjectAttributeValueMap.get("PSS_E2ENotificationRemainderFrequency");
                if (strCroneType.equalsIgnoreCase(isOpenedValue)) {
                    slChangeList.add("isOpened");
                }
                if (strCroneType.equalsIgnoreCase(isDelayedValue)) {
                    slChangeList.add("isDelayed");
                }
                if (strCroneType.equalsIgnoreCase(isRemainderValue)) {
                    // Changed by Suchit G. to isReminder from isRemainder on Date: 24/11/2016 for E2E2 Stream: TIGTK-3613
                    slChangeList.add("isReminder");
                }
            }

            // If StringList is not empty then put in the Map
            if (!slChangeList.isEmpty()) {
                mProgramProjectConfigurationMap.put("CroneType", slChangeList);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgramProjectConfiguration: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return mProgramProjectConfigurationMap;
    }// getProgramProjectConfiguration() end

    /**
     * @description: This method is used to get the Change Data for Escalation(based on the changeType)
     * @param context
     * @param ChangeType
     * @param PCMData
     * @return Map
     * @throws Exception
     */

    public Map getTypeOfChange(Context context, String strChangeType, Map<MapList, MapList> mPCMData) throws Exception {
        // strChangeType=isOpened/isRemainder/isDelayed/isEscalatioOpened/isEscalatioDelayed
        Map mTypeOfChange = new HashMap();
        try {
            if (mPCMData != null && !mPCMData.isEmpty()) {
                // Added for Find Bug Issue TIGTK-3955 : Priyanka Salunke : 23-Jan-2017 : START
                for (java.util.Map.Entry<MapList, MapList> entrySet : mPCMData.entrySet()) {
                    MapList mlChange = (MapList) entrySet.getValue();
                    MapList mlReturnChange = new MapList();
                    if (!mlChange.isEmpty()) {
                        for (int itr = 0; itr < mlChange.size(); itr++) {
                            Map mChange = (Map) mlChange.get(itr);
                            boolean changeType = (boolean) mChange.get(strChangeType);
                            if (changeType) {
                                mlReturnChange.add(mChange);
                            }
                        }
                    }
                    mTypeOfChange.put(entrySet.getKey(), mlReturnChange);
                }
                // Added for Find Bug Issue TIGTK-3955 : Priyanka Salunke : 23-Jan-2017 : END
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getTypeOfChange: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return mTypeOfChange;

    }

    /**
     * @description: This method is used to send the weekly notification of all program project
     * @param context
     * @param args
     * @return void
     * @throws Exception
     * @author Priyanka Salunke
     */
    public void sendWeeklyNotification(Context context, String[] strCroneType1) throws Exception {
        try {
            // Get Todays Day
            DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
            String dayNames[] = dateFormatSymbols.getWeekdays();
            Calendar calendar = Calendar.getInstance();
            String strTodaysDay = dayNames[calendar.get(Calendar.DAY_OF_WEEK)];
            // Get all Program Projects present in System
            StringList slObjectSelects = new StringList(DomainObject.SELECT_ID);
            MapList mlProgramProjects = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PROGRAMPROJECT, TigerConstants.VAULT_ESERVICEPRODUCTION, null, slObjectSelects);
            String strCroneType = strCroneType1[0];
            String strIsEscalation = "true";

            // Added by Suchit G. for TIGTK-3957 on Date: 01/02/2017: Start
            STR_SERVER_URL = strCroneType1[1];
            // Added by Suchit G. for TIGTK-3957 on Date: 01/02/2017: End

            // Proceed when Program Projects are present in system
            // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
            MapList mCombinedResultMapList = new MapList();
            // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END

            if ((mlProgramProjects != null) && (!mlProgramProjects.isEmpty())) {
                Iterator itrProgramProjects = mlProgramProjects.iterator();
                while (itrProgramProjects.hasNext()) {
                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                    MapList mlTemp = new MapList();
                    MapList mlProgramProjectMailList = new MapList();
                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                    Map mPRogramProjectMap = (Map) itrProgramProjects.next();
                    String strProgramProjectID = (String) mPRogramProjectMap.get(DomainConstants.SELECT_ID);
                    // Iteration 14
                    if (UIUtil.isNotNullAndNotEmpty(strProgramProjectID)) {
                        if (strIsEscalation.equalsIgnoreCase("true")) {
                            // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017
                            String[] args = new String[] { strProgramProjectID, "true" };
                            // Added for E2E issue : TIGTK-3948 : Priyanka Salunke : 02-Feb-2017 : END
                            Map mProgramProjectConfigurationMap = getProgramProjectConfiguration(context, strProgramProjectID, strCroneType, "Escalation");
                            StringList slEscalationChangeType = (StringList) mProgramProjectConfigurationMap.get("CroneType");
                            if (slEscalationChangeType != null && !slEscalationChangeType.isEmpty()) {
                                if (strTodaysDay.equalsIgnoreCase("Sunday")) {
                                    if (slEscalationChangeType.size() == 2) {
                                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                                        mlTemp = sendProgramProjectChangeStatus(context, args, slEscalationChangeType, "Escalation");
                                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                                    } else if (slEscalationChangeType.size() != 2) {
                                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                                        mlTemp = sendProgramProjectChangeStatus(context, args, slEscalationChangeType, "Escalation");
                                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                                    }
                                }
                            }
                        }
                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                        if (!mlTemp.isEmpty()) {
                            mlProgramProjectMailList.addAll(mlTemp);
                        }
                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                        // Added for TIGTK-3948 : PS&VP : 31-Jan-2017 : START
                        // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017
                        String[] args = new String[] { strProgramProjectID, "false" };
                        // Added for E2E issue : TIGTK-3948 : Priyanka Salunke : 02-Feb-2017 : END
                        Map mProgramProjectConfigurationMap = getProgramProjectConfiguration(context, strProgramProjectID, strCroneType, "Notification");
                        StringList slNotificationChangeType = (StringList) mProgramProjectConfigurationMap.get("CroneType");
                        if (slNotificationChangeType != null && !slNotificationChangeType.isEmpty()) {
                            if (strTodaysDay.equalsIgnoreCase("Sunday")) {
                                if (slNotificationChangeType.size() == 3) {
                                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                                    mlTemp = sendProgramProjectChangeStatus(context, args, slNotificationChangeType, "Notification");
                                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                                } else if (slNotificationChangeType.size() != 3) {

                                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                                    mlTemp = sendProgramProjectChangeStatus(context, args, slNotificationChangeType, "Notification");
                                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                                }
                            }
                        }
                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                        if (!mlTemp.isEmpty()) {
                            mlProgramProjectMailList.addAll(mlTemp);
                        }
                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                    } // If end
                      // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                    if (!mlProgramProjectMailList.isEmpty()) {
                        if (mCombinedResultMapList.isEmpty()) {
                            mCombinedResultMapList.addAll(mlProgramProjectMailList);
                        } else {
                            int iCombinedListSize = mCombinedResultMapList.size();
                            int iTempListSize = mlProgramProjectMailList.size();
                            for (int i = 0; i < iTempListSize; i++) {
                                boolean isMessageFound = false;
                                // Result of single Program-Project
                                Map mCurrentMap = (Map) mlProgramProjectMailList.get(i);
                                StringList slCurrentToObjectList = (StringList) mCurrentMap.get("toList");
                                String strCurrentMessageType = (String) mCurrentMap.get("messageType");
                                String strCurrentMessageBody = (String) mCurrentMap.get("messageText");
                                String strCurrentMessageBodyHTML = (String) mCurrentMap.get("messageHTML");

                                for (int j = 0; j < iCombinedListSize; j++) {
                                    // When same mail type and toList then combined the results of Program-Projects
                                    Map mCombinedMap = (Map) mCombinedResultMapList.get(j);
                                    StringList slToObjectList = (StringList) mCombinedMap.get("toList");
                                    String strMessageType = (String) mCombinedMap.get("messageType");
                                    if (slToObjectList.equals(slCurrentToObjectList) && strMessageType.equals(strCurrentMessageType)) {
                                        Map mFinalResultMap = new HashMap();
                                        String strMessageBody = (String) mCombinedMap.get("messageText");
                                        StringBuffer sbMessageText = new StringBuffer();
                                        sbMessageText.append(strMessageBody);
                                        sbMessageText.append(strCurrentMessageBody);

                                        String strSubject = (String) mCombinedMap.get("subject");
                                        String strMessageHTML = (String) mCombinedMap.get("messageHTML");
                                        StringBuffer sbMessageHTML = new StringBuffer();
                                        sbMessageHTML.append(strMessageHTML);
                                        sbMessageHTML.append(strCurrentMessageBodyHTML);

                                        mFinalResultMap.put("subject", strSubject);
                                        mFinalResultMap.put("toList", slToObjectList);
                                        mFinalResultMap.put("messageType", strMessageType);
                                        mFinalResultMap.put("messageText", sbMessageText.toString());
                                        mFinalResultMap.put("messageHTML", sbMessageHTML.toString());
                                        mFinalResultMap.put("fromAgent", context.getUser());
                                        isMessageFound = true;
                                        mCombinedResultMapList.remove(j);
                                        mCombinedResultMapList.add(mFinalResultMap);
                                        break;
                                    }
                                }
                                if (!isMessageFound) {
                                    mCombinedResultMapList.add(mCurrentMap);
                                }

                            }
                        }
                    }
                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                } // While end
            } // If end
              // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
              // Send Consolidated Mail for Program-Projects
            if (!mCombinedResultMapList.isEmpty()) {
                String messageCSS = getMailCSS();
                Iterator itrFinalIterator = mCombinedResultMapList.iterator();
                while (itrFinalIterator.hasNext()) {
                    Map mpFinalResultMap = (Map) itrFinalIterator.next();
                    String strMessageType = (String) mpFinalResultMap.get("messageType");
                    String strMessageHTML = (String) mpFinalResultMap.get("messageHTML");

                    StringBuilder sbMailBody = new StringBuilder();
                    sbMailBody.append("<table><tr class='mainHeader'><th class='name'>Change Number</th><th class='description'>Description</th><th class='ra'>Required Action</th>");
                    if (strMessageType.equals("Escalation"))
                        sbMailBody.append("<th>Assignee</th>");
                    sbMailBody.append("<th class='InStateSince'>In State Since</th><th>Due Date</th><th class='delay'>Delay</th></tr>");
                    sbMailBody.append(strMessageHTML);
                    sbMailBody.append((String) "</table>");

                    mpFinalResultMap.put("messageHTML", messageCSS + sbMailBody.toString());
                    mpFinalResultMap.put("messageText", messageCSS + sbMailBody.toString());
                    // Invoke to send mails
                    JPO.invoke(context, "PSS_emxPart", null, "sendJavaMail", JPO.packArgs(mpFinalResultMap));
                }
            }
            // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in sendWeeklyNotification: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }// sendWeeklyNotification() end

    /**
     * @description: This method is used to send the weekly notification of all program project
     * @param context
     * @param args
     * @return void
     * @throws Exception
     * @author Priyanka Salunke
     */
    public void sendDailyNotification(Context context, String[] strCroneType1) throws Exception {
        try {
            // Get Todays Day
            DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
            String dayNames[] = dateFormatSymbols.getWeekdays();
            Calendar calendar = Calendar.getInstance();
            String strTodaysDay = dayNames[calendar.get(Calendar.DAY_OF_WEEK)];
            // Get all Program Projects present in System
            StringList slObjectSelects = new StringList(DomainObject.SELECT_ID);
            MapList mlProgramProjects = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PROGRAMPROJECT, TigerConstants.VAULT_ESERVICEPRODUCTION, null, slObjectSelects);
            String strCroneType = strCroneType1[0];
            String strIsEscalation = "true";

            // Added by Suchit G. for TIGTK-3957 on Date: 01/02/2017: Start
            STR_SERVER_URL = strCroneType1[1];
            // Added by Suchit G. for TIGTK-3957 on Date: 01/02/2017: End

            // Proceed when Program Projects are present in system
            // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
            MapList mCombinedResultMapList = new MapList();
            // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END

            if ((mlProgramProjects != null) && (!mlProgramProjects.isEmpty())) {
                Iterator itrProgramProjects = mlProgramProjects.iterator();
                while (itrProgramProjects.hasNext()) {
                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                    MapList mlTemp = new MapList();
                    MapList mlProgramProjectMailList = new MapList();
                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                    Map mPRogramProjectMap = (Map) itrProgramProjects.next();
                    String strProgramProjectID = (String) mPRogramProjectMap.get(DomainConstants.SELECT_ID);
                    // Iteration 14
                    if (UIUtil.isNotNullAndNotEmpty(strProgramProjectID)) {
                        if (strIsEscalation.equalsIgnoreCase("true")) {
                            // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017
                            String[] args = new String[] { strProgramProjectID, "true" };
                            Map mProgramProjectConfigurationMap = getProgramProjectConfiguration(context, strProgramProjectID, strCroneType, "Escalation");
                            StringList slEscalationChangeType = (StringList) mProgramProjectConfigurationMap.get("CroneType");
                            if (slEscalationChangeType != null && !slEscalationChangeType.isEmpty()) {
                                if (slEscalationChangeType.size() == 2) {
                                    if (!strTodaysDay.equalsIgnoreCase("Friday") && !strTodaysDay.equalsIgnoreCase("Saturday")) {
                                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                                        mlTemp = sendProgramProjectChangeStatus(context, args, slEscalationChangeType, "Escalation");
                                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                                    }
                                } else if (slEscalationChangeType.size() != 2) {
                                    if (!strTodaysDay.equalsIgnoreCase("Friday") && !strTodaysDay.equalsIgnoreCase("Saturday") && !strTodaysDay.equalsIgnoreCase("Sunday")) {
                                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                                        mlTemp = sendProgramProjectChangeStatus(context, args, slEscalationChangeType, "Escalation");
                                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                                    }
                                }
                            }
                        }
                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                        if (!mlTemp.isEmpty()) {
                            mlProgramProjectMailList.addAll(mlTemp);
                        }
                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                        // Added for TIGTK-3948 : PS&VP : 31-Jan-2017 : START
                        // Fing Bug Issue : TIGTK-4401 : Priyanka Salunke : 08-Feb-2017
                        String[] args = new String[] { strProgramProjectID, "false" };
                        // Added for TIGTK-3948 : PS&VP : 31-Jan-2017 : END
                        Map mProgramProjectConfigurationMap = getProgramProjectConfiguration(context, strProgramProjectID, strCroneType, "Notification");
                        StringList slNotificationChangeType = (StringList) mProgramProjectConfigurationMap.get("CroneType");

                        if (slNotificationChangeType != null && !slNotificationChangeType.isEmpty()) {
                            // Send notification if all Frequencies are Daily
                            if (slNotificationChangeType.size() == 3) {
                                if (!strTodaysDay.equalsIgnoreCase("Friday") && !strTodaysDay.equalsIgnoreCase("Saturday")) {
                                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                                    mlTemp = sendProgramProjectChangeStatus(context, args, slNotificationChangeType, "Notification");
                                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                                }
                            }
                            // If all Frequencies are not Daily
                            else if (slNotificationChangeType.size() != 3) {
                                // Send notification on Monday , Tuesday , Wednesday , Thursday
                                if (!strTodaysDay.equalsIgnoreCase("Friday") && !strTodaysDay.equalsIgnoreCase("Saturday") && !strTodaysDay.equalsIgnoreCase("Sunday")) {
                                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                                    mlTemp = sendProgramProjectChangeStatus(context, args, slNotificationChangeType, "Notification");
                                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                                }
                            }
                        }
                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                        if (!mlTemp.isEmpty()) {
                            mlProgramProjectMailList.addAll(mlTemp);
                        }
                        // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                    } // If end
                      // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
                    if (!mlProgramProjectMailList.isEmpty()) {
                        if (mCombinedResultMapList.isEmpty()) {
                            mCombinedResultMapList.addAll(mlProgramProjectMailList);
                        } else {
                            int iCombinedListSize = mCombinedResultMapList.size();
                            int iTempListSize = mlProgramProjectMailList.size();
                            for (int i = 0; i < iTempListSize; i++) {
                                boolean isMessageFound = false;
                                // Result of single Program-Project
                                Map mCurrentMap = (Map) mlProgramProjectMailList.get(i);
                                StringList slCurrentToObjectList = (StringList) mCurrentMap.get("toList");
                                String strCurrentMessageType = (String) mCurrentMap.get("messageType");
                                String strCurrentMessageBody = (String) mCurrentMap.get("messageText");
                                String strCurrentMessageBodyHTML = (String) mCurrentMap.get("messageHTML");

                                for (int j = 0; j < iCombinedListSize; j++) {
                                    // When same mail type and toList then combined the results of Program-Projects
                                    Map mCombinedMap = (Map) mCombinedResultMapList.get(j);
                                    StringList slToObjectList = (StringList) mCombinedMap.get("toList");
                                    String strMessageType = (String) mCombinedMap.get("messageType");
                                    if (slToObjectList.equals(slCurrentToObjectList) && strMessageType.equals(strCurrentMessageType)) {
                                        Map mFinalResultMap = new HashMap();
                                        String strMessageBody = (String) mCombinedMap.get("messageText");
                                        StringBuffer sbMessageText = new StringBuffer();
                                        sbMessageText.append(strMessageBody);
                                        sbMessageText.append(strCurrentMessageBody);

                                        String strSubject = (String) mCombinedMap.get("subject");
                                        String strMessageHTML = (String) mCombinedMap.get("messageHTML");
                                        StringBuffer sbMessageHTML = new StringBuffer();
                                        sbMessageHTML.append(strMessageHTML);
                                        sbMessageHTML.append(strCurrentMessageBodyHTML);

                                        mFinalResultMap.put("subject", strSubject);
                                        mFinalResultMap.put("toList", slToObjectList);
                                        mFinalResultMap.put("messageType", strMessageType);
                                        mFinalResultMap.put("messageText", sbMessageText.toString());
                                        mFinalResultMap.put("messageHTML", sbMessageHTML.toString());
                                        mFinalResultMap.put("fromAgent", context.getUser());
                                        isMessageFound = true;
                                        mCombinedResultMapList.remove(j);
                                        mCombinedResultMapList.add(mFinalResultMap);
                                        break;
                                    }
                                }
                                if (!isMessageFound) {
                                    mCombinedResultMapList.add(mCurrentMap);
                                }

                            }
                        }
                    }
                    // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
                } // While end
            } // If end
              // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : START
              // Send Consolidated Mail for Program-Projects
            if (!mCombinedResultMapList.isEmpty()) {
                String messageCSS = getMailCSS();
                Iterator itrFinalIterator = mCombinedResultMapList.iterator();
                while (itrFinalIterator.hasNext()) {
                    Map mpFinalResultMap = (Map) itrFinalIterator.next();
                    String strMessageType = (String) mpFinalResultMap.get("messageType");
                    String strMessageHTML = (String) mpFinalResultMap.get("messageHTML");

                    StringBuilder sbMailBody = new StringBuilder();
                    sbMailBody.append("<table><tr class='mainHeader'><th class='name'>Change Number</th><th class='description'>Description</th><th class='ra'>Required Action</th>");
                    if (strMessageType.equals("Escalation"))
                        sbMailBody.append("<th>Assignee</th>");
                    sbMailBody.append("<th class='InStateSince'>In State Since</th><th>Due Date</th><th class='delay'>Delay</th></tr>");
                    sbMailBody.append(strMessageHTML);
                    sbMailBody.append((String) "</table>");

                    mpFinalResultMap.put("messageHTML", messageCSS + sbMailBody.toString());
                    mpFinalResultMap.put("messageText", messageCSS + sbMailBody.toString());
                    // Invoke to send mails
                    JPO.invoke(context, "PSS_emxPart", null, "sendJavaMail", JPO.packArgs(mpFinalResultMap));
                }
            }
            // Added For E2E Issue TIGTK-4383 : Priyanka Salunke : 15-Feb-2017 : END
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in sendDailyNotification: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }// sendDailyNotification() end

    /**
     * @description: This method is used added for TIGTK-3430
     * @param context
     * @param String
     * @return String
     * @throws Exception
     * @since 10/Nov/2016
     */
    private String getPropertyValue(Context context, String propertyKey) throws Exception {
        return EnoviaResourceBundle.getFrameworkStringResourceProperty(context, propertyKey, context.getLocale());
    }

}