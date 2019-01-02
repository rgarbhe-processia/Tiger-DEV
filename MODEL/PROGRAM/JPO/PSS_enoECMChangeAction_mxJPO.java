
/*
 * * ${CLASS:PSS_enoECMChangeActionBase}** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 */
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import com.dassault_systemes.enovia.enterprisechangemgt.admin.ECMAdmin;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeAction;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeManagement;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class PSS_enoECMChangeAction_mxJPO extends emxDomainObject_mxJPO {

    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_enoECMChangeAction_mxJPO.class);
    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - END

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static final String ATTR_VALUE_MANDATORY = "Mandatory";

    protected static String RESOURCE_BUNDLE_COMPONENTS_STR = "emxEnterpriseChangeMgtStringResource";

    protected static final String SELECT_NEW_VALUE = "New Value";

    private ChangeManagement changeManagement = null;

    private static final String FORMAT_DATE = "date";

    private static final String FORMAT_NUMERIC = "numeric";

    private static final String FORMAT_INTEGER = "integer";

    private static final String FORMAT_BOOLEAN = "boolean";

    private static final String FORMAT_REAL = "real";

    private static final String FORMAT_TIMESTAMP = "timestamp";

    private static final String FORMAT_STRING = "string";

    protected static final String INPUT_TYPE_TEXTBOX = "textbox";

    private static final String INPUT_TYPE_TEXTAREA = "textarea";

    private static final String INPUT_TYPE_COMBOBOX = "combobox";

    protected static final String SETTING_INPUT_TYPE = "Input Type";

    private static final String SETTING_FORMAT = "format";

    /**
     * Constructor
     * @param context
     * @param args
     * @throws Exception
     */
    public PSS_enoECMChangeAction_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Method to return prerequisites
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getPrerequisites(Context context, String[] args) throws Exception {
        Map programMap = (HashMap) JPO.unpackArgs(args);
        String changeObjId = (String) programMap.get("objectId");
        ChangeManagement changeManagement = new ChangeManagement(changeObjId);
        MapList mlCMPrerequisites = changeManagement.getPrerequisites(context, ChangeConstants.TYPE_CHANGE_ACTION);
        return mlCMPrerequisites;

        // return new ChangeManagement(changeObjId).getPrerequisites(context, ChangeConstants.TYPE_CHANGE_ACTION);
    }

    /**
     * Method to return prerequisites
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getRelatedItems(Context context, String[] args) throws Exception {
        Map programMap = (HashMap) JPO.unpackArgs(args);
        String changeObjId = (String) programMap.get("objectId");
        ChangeAction changeAction = new ChangeAction(changeObjId);
        MapList mlRelatedItem = changeAction.getRelatedItems(context);
        return mlRelatedItem;
        // return new ChangeAction(changeObjId).getRelatedItems(context);
    }

    /**
     * Method to add Responsible Organizatoin for CA.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void connectResponsibleOrganization(Context context, String[] args) throws Exception {
        try {
            Map programMap = (HashMap) JPO.unpackArgs(args);
            HashMap hmParamMap = (HashMap) programMap.get("paramMap");
            String strChangeObjId = (String) hmParamMap.get("objectId");
            String strNewResponsibleOrgName = (String) hmParamMap.get(ChangeConstants.NEW_VALUE);
            this.setId(strChangeObjId);
            if (UIUtil.isNotNullAndNotEmpty(strNewResponsibleOrgName)) {
                this.setPrimaryOwnership(context, ChangeUtil.getDefaultProject(context), strNewResponsibleOrgName);
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectResponsibleOrganization: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * connectTechAssignee -
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void connectTechAssignee(Context context, String[] args) throws Exception {
        try {
            Map programMap = (HashMap) JPO.unpackArgs(args);
            HashMap hmParamMap = (HashMap) programMap.get("paramMap");
            String strChangeObjId = (String) hmParamMap.get("objectId");
            String strNewTechAssignee = (String) hmParamMap.get("New OID");
            String relTechAssignee = PropertyUtil.getSchemaProperty(context, "relationship_TechnicalAssignee");

            ChangeAction changeAction = new ChangeAction(strChangeObjId);
            changeAction.connectTechAssigneeToCA(context, strChangeObjId, strNewTechAssignee, relTechAssignee);
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectTechAssignee: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * connectSeniorTechAssignee - Connect new Senior Tech Assignee -Update Program
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void connectSeniorTechAssignee(Context context, String[] args) throws Exception {
        try {
            Map programMap = (HashMap) JPO.unpackArgs(args);
            HashMap hmParamMap = (HashMap) programMap.get("paramMap");
            String strChangeObjId = (String) hmParamMap.get("objectId");
            String strNewSeniorTechAssig = (String) hmParamMap.get("New OID");
            String relSeniorTechAssignee = PropertyUtil.getSchemaProperty(context, "relationship_SeniorTechnicalAssignee");
            this.setId(strChangeObjId);
            String strSrTechAssigneeRelId = getInfo(context, "from[" + relSeniorTechAssignee + "].id");

            if (!ChangeUtil.isNullOrEmpty(strSrTechAssigneeRelId)) {
                DomainRelationship.disconnect(context, strSrTechAssigneeRelId);
            }
            if (!ChangeUtil.isNullOrEmpty(strNewSeniorTechAssig)) {
                DomainRelationship.connect(context, new DomainObject(strChangeObjId), new RelationshipType(relSeniorTechAssignee), new DomainObject(strNewSeniorTechAssig));
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectSeniorTechAssignee: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * This is a check trigger method on (Pending --> InWork) to validate Estimated Completion Date and Technical Assignee on the Change Action
     * @param context
     * @param args
     *            Change Action Id and Notice
     * @return integer (0 = pass, 1= block with notice)
     * @throws Exception
     */
    public int validateCompletionDateAndTechAssignee(Context context, String args[]) throws Exception {
        int iReturn = 0;
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }
            String strChangeId = args[0];
            this.setId(strChangeId);

            // Getting the Tecchnical Assignee connected
            String strTechAssigneeId = getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.id");
            String strEstCompletionDateNotice = EnoviaResourceBundle.getProperty(context, args[3], context.getLocale(), args[1]);
            String strTechAssigneeNotice = EnoviaResourceBundle.getProperty(context, args[3], context.getLocale(), args[2]);
            // Getting the Estimated Completion Date Value
            String strCompletionDate = (String) getAttributeValue(context, ChangeConstants.ATTRIBUTE_ESTIMATED_COMPLETION_DATE);

            // Validating If both are not empty, if so accordingly sending the
            // notice.
            if (ChangeUtil.isNullOrEmpty(strCompletionDate)) {
                emxContextUtilBase_mxJPO.mqlNotice(context, strEstCompletionDateNotice);
                iReturn = 1;
            }

            if (ChangeUtil.isNullOrEmpty(strTechAssigneeId)) {
                emxContextUtilBase_mxJPO.mqlNotice(context, strTechAssigneeNotice);
                iReturn = 1;
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in validateCompletionDateAndTechAssignee: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
        return iReturn;
    }

    /**
     * This is a check trigger method on (Pending --> InWork) to validate whether the Impact Analysis connected to the Change Action are in Complete State
     * @param context
     * @param args
     *            (Change Action Id and Notice)
     * @return integer (0 = pass, 1= block with notice)
     * @throws Exception
     */
    public int impactAnalysisCompletion(Context context, String args[]) throws Exception {
        int iReturn = 0;
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }

            String strIAName = "";
            Map tempMap = null;
            String strChangeId = args[0];
            this.setId(strChangeId);
            StringBuffer sbMessage = new StringBuffer();
            String STATE_IA_COMPLETE = PropertyUtil.getSchemaProperty(context, "policy", ChangeConstants.POLICY_IMPACT, args[3]);
            StringList objectSelects = new StringList(SELECT_ID);
            objectSelects.add(SELECT_CURRENT);
            objectSelects.add(SELECT_NAME);

            String strNotice = EnoviaResourceBundle.getProperty(context, args[2], context.getLocale(), args[1]);

            String whereStr = SELECT_CURRENT + "!=" + STATE_IA_COMPLETE;

            // Getting all the Impact Analysis which are not in complete state.
            MapList mlIAOutput = getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPACT_ANALYSIS, TYPE_IMPACT_ANALYSIS, objectSelects, null, false, true, (short) 1, whereStr, EMPTY_STRING, 0);

            if (mlIAOutput != null && !mlIAOutput.isEmpty()) {
                for (Object var : mlIAOutput) {
                    tempMap = (Map) var;
                    strIAName = (String) tempMap.get(SELECT_NAME);
                    sbMessage = addToStringBuffer(context, sbMessage, strIAName);
                }
            }

            // If Message is not empty, send the notice with Impact Analysis
            // name, which are not in complete state.
            if (sbMessage.length() != 0) {
                emxContextUtilBase_mxJPO.mqlNotice(context, strNotice + sbMessage.toString());
                iReturn = 1;
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in impactAnalysisCompletion: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
        return iReturn;
    }

    /**
     * Subsidiary method to add the new String to the StringBuffer
     * @param context
     * @param sbOutput
     *            - StringBuffer Output
     * @param message
     *            - String need to be added
     * @return String Buffer
     * @throws Exception
     */
    private StringBuffer addToStringBuffer(Context context, StringBuffer sbOutput, String message) throws Exception {
        try {
            if (sbOutput != null && sbOutput.length() != 0) {
                sbOutput.append(", ");
                sbOutput.append(message);
            } else {
                // Find Bug Issue Fix : Priyanka Salunke
                sbOutput = new StringBuffer(message);
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in addToStringBuffer: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
        return sbOutput;
    }

    /**
     * Check Trigger on (Pending --> InWork) to check whether Prerequisites Parent CA (Hard Dependency) are all in Complete State, Hard Dependency - Parent Change Action Id will be having attribute
     * value "Type of Dependency" as "Hard".
     * @param context
     * @param args
     *            - Change Action Id
     * @return (0 = pass, 1= block with notice)
     * @throws Exception
     */
    public int checkForDependency(Context context, String args[]) throws Exception {
        int iReturn = 0;
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }

            String strCAName = "";
            Map tempMap = null;
            String strChangeId = args[0];
            this.setId(strChangeId);
            StringBuffer sBusWhere = new StringBuffer();
            StringBuffer sRelWhere = new StringBuffer();
            StringBuffer sbMessage = new StringBuffer();

            String strNotice = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Notice.HardDependency");

            sBusWhere.append("(" + SELECT_CURRENT + " != \"");
            sBusWhere.append(ChangeConstants.STATE_CHANGE_ACTION_COMPLETE);
            sBusWhere.append("\" && ");
            sBusWhere.append(SELECT_CURRENT + " != \"");
            sBusWhere.append(ChangeConstants.STATE_CHANGE_ACTION_CANCEL);
            sBusWhere.append("\" && ");
            sBusWhere.append(SELECT_CURRENT + " != \"");
            sBusWhere.append(ChangeConstants.STATE_CHANGE_ACTION_HOLD);
            sBusWhere.append("\")");
            sRelWhere.append("attribute[");
            sRelWhere.append(ChangeConstants.ATTRIBUTE_PREREQUISITE_TYPE);
            sRelWhere.append("] == ");
            sRelWhere.append(ATTR_VALUE_MANDATORY);

            // Get all the Prerequisites which are not in complete state and
            // that are Hard Dependency.
            // MapList mlPrerequisites = new ChangeAction(strChangeId).getPrerequisites(context, ChangeConstants.TYPE_CHANGE_ACTION, sBusWhere.toString(), sRelWhere.toString());
            ChangeAction changeAction = new ChangeAction(strChangeId);
            MapList mlPrerequisites = changeAction.getPrerequisites(context, ChangeConstants.TYPE_CHANGE_ACTION, sBusWhere.toString(), sRelWhere.toString());
            if (mlPrerequisites != null && !mlPrerequisites.isEmpty()) {
                for (Object var : mlPrerequisites) {
                    tempMap = (Map) var;
                    strCAName = (String) tempMap.get(SELECT_NAME);
                    sbMessage = addToStringBuffer(context, sbMessage, strCAName);
                }
            }

            // If Message is not empty, send the notice with Change Action which
            // are not completed.
            if (sbMessage.length() != 0) {
                emxContextUtilBase_mxJPO.mqlNotice(context, strNotice + "  " + sbMessage.toString());
                iReturn = 1;
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForDependency: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
        return iReturn;
    }

    /**
     * Check Trigger on (Pending --> InWork) to check whether parent CO is in In Work state,
     * @param context
     * @param args
     *            - Change Action Id
     * @return (0 = pass, 1= block with notice)
     * @throws Exception
     */
    public int checkCOState(Context context, String args[]) throws Exception {
        String strFunc = null;
        String strNotice = null;

        String strChangeId = args[0];
        String nextState = args[9]; // for custom change on cancel the check
        // trigger will be fired on promting to
        // cancel state.
        String type = args[8]; // for custom change on cancel the check trigger
        // will be fired on promting to cancel state.

        if (ChangeConstants.TYPE_CCA.equals(type)) {
            String policyConfiguredPart = PropertyUtil.getSchemaProperty(context, "policy_PUEECO");
            String stateCancelled = PropertyUtil.getSchemaProperty(context, "policy", policyConfiguredPart, "state_Cancelled");

            if (stateCancelled.equals(nextState)) {
                return 0;
            }
        }
        if (UIUtil.isNotNullAndNotEmpty(strChangeId)) {
            this.setId(strChangeId);

            String coObjIdSelect = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + ChangeConstants.TYPE_CHANGE_ORDER + "].id";
            String coPolicySelect = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + ChangeConstants.TYPE_CHANGE_ORDER + "].policy";
            String coCurrentState = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + ChangeConstants.TYPE_CHANGE_ORDER + "].current";
            StringList select = new StringList();
            select.add(coObjIdSelect);
            select.add(coPolicySelect);
            select.add(coCurrentState);

            Map resultList = getInfo(context, select);
            String coObjId = (String) resultList.get(coObjIdSelect);
            String copolicy = (String) resultList.get(coPolicySelect);

            if (UIUtil.isNullOrEmpty(coObjId)) {
                String crObjectId = getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + ChangeConstants.TYPE_CHANGE_REQUEST + "].id");

                if (UIUtil.isNotNullAndNotEmpty(crObjectId)) {
                    strNotice = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Notice.COIsNotConnectedToCA");
                }
            } else {
                strFunc = context.getCustomData("massFunctionality");

                if (ChangeConstants.POLICY_FASTTRACK_CHANGE.equals(copolicy)) {
                    if (ChangeConstants.TYPE_CCA.equals(type)) {
                        String coState = (String) resultList.get(coCurrentState);
                        String STATE_CO_ON_HOLD = PropertyUtil.getSchemaProperty(context, "policy", ChangeConstants.POLICY_FORMAL_CHANGE, "state_OnHold");
                        if (coState.equals(STATE_CO_ON_HOLD)) {
                            strNotice = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Notice.ConnectedCOInOnHoldState");
                        }
                    }
                } else {
                    String STATE_CO_INWORK = PropertyUtil.getSchemaProperty(context, "policy", ChangeConstants.POLICY_FORMAL_CHANGE, "state_InWork");
                    ChangeUtil changeUtil = new ChangeUtil();
                    if ((changeUtil.checkObjState(context, coObjId, STATE_CO_INWORK, ChangeConstants.NE) == 0)
                            && !(ChangeConstants.FOR_RELEASE.equals(strFunc) || ChangeConstants.FOR_OBSOLETE.equals(strFunc))) {
                        strNotice = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Notice.ConnectedCONotInInWorkState");
                    }
                }
            }
        }

        if (strNotice != null) {
            emxContextUtilBase_mxJPO.mqlNotice(context, strNotice);
        }

        if (strFunc != null) {
            context.clearCustomData();
        }

        return (strNotice == null) ? 0 : 1;
    }

    /**
     * The Action trigger method on (Pending --> In Work) to Revise and Connect object to implemented items of CA. This can be used for generic purpose.
     * @param context
     * @param args
     * @return void
     * @throws Exception
     */

    public int reviseAndConnectToImplementedItems(Context context, String[] args) throws Exception {
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }
            String strId = "";
            String strType = "";
            String strName = "";
            String strCurrent = "";
            String strRevision = "";
            String strAttrRC = "";
            String strWhere = "";
            String strPolicy = "";
            // String strLatestRevCurrent = "";
            String strLatestRevision = "";
            String strLatestRevisionId = "";
            String strObjectStateName = "";
            // RelationshipType relType = null;
            Map tempMap = null;
            Map mapTemp = null;
            boolean bSpec = true;
            String nonCDMTypes = EnoviaResourceBundle.getProperty(context, "EnterpriseChangeMgt.Integration.NonCDMTypes");
            StringList typeList = new StringList();
            String typeSpec = "";
            String LastestRevId = "";

            HashMap latestRevisionMap = new HashMap();
            String strObjId = args[0];
            DomainObject dObj = new DomainObject(strObjId);

            ChangeUtil changeUtil = new ChangeUtil();
            StringList slBusSelect = new StringList(4);
            slBusSelect.addElement(SELECT_ID);
            slBusSelect.addElement(SELECT_TYPE);
            slBusSelect.addElement(SELECT_NAME);
            slBusSelect.addElement(SELECT_CURRENT);
            slBusSelect.addElement(SELECT_REVISION);
            slBusSelect.addElement(SELECT_POLICY);

            ChangeAction changeAction = new ChangeAction(strObjId);

            StringList slRelSelect = new StringList();
            String strRequestedChange = ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE;
            slRelSelect.addElement(strRequestedChange);

            // Getting all the connected objects of context object
            MapList mlRelatedObjects = dObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, "*", slBusSelect, slRelSelect, false, true, (short) 1, "", "", 0);

            Iterator i = mlRelatedObjects.iterator();

            DomainObject domObj = new DomainObject();

            // Iterating all the Affected Items Objects
            while (i.hasNext()) {
                mapTemp = (Map) i.next();
                // Fetching all the values
                strId = (String) mapTemp.get(SELECT_ID);
                strType = (String) mapTemp.get(SELECT_TYPE);
                strName = (String) mapTemp.get(SELECT_NAME);
                strCurrent = (String) mapTemp.get(SELECT_CURRENT);
                strRevision = (String) mapTemp.get(SELECT_REVISION);
                strAttrRC = (String) mapTemp.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                strPolicy = (String) mapTemp.get(SELECT_POLICY);

                strObjectStateName = ECMAdmin.getReleaseStateValue(context, strType, strPolicy);

                // Checking if the object is "For Revise" and state is Release
                if (strAttrRC.equalsIgnoreCase(ChangeConstants.FOR_REVISE) && strCurrent.equalsIgnoreCase(strObjectStateName)) {
                    strWhere = "name == '" + strName + "' && revision == last";
                    setId(strId);

                    // Considering only one Latest Revision with that Name
                    LastestRevId = getInfo(context, "last.id");

                    setId(LastestRevId);

                    tempMap = getInfo(context, slBusSelect);
                    // Checking for the current state and revision of the object
                    // strLatestRevCurrent = (String) tempMap.get(SELECT_CURRENT);
                    strLatestRevision = (String) tempMap.get(SELECT_REVISION);

                    // Check if latest revision exists and which is not released
                    // in the system
                    if (!strRevision.equalsIgnoreCase(strLatestRevision) && changeUtil.checkObjState(context, LastestRevId, strObjectStateName, ChangeConstants.LT) == 0) {
                        // Getting the latest revision of object, if already
                        // connected in Implemented Items
                        MapList mlImplementedItems = dObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, "*", slBusSelect, null, false, true, (short) 1, strWhere,
                                EMPTY_STRING, 0);

                        if (mlImplementedItems.size() == 0) {
                            // Connecting to Change Action object if latest
                            // revision is not connected as Implemented Item
                            changeAction.connectImplementedItems(context, new StringList(LastestRevId));
                        }
                    } else {
                        domObj.setId(strId);

                        if (UIUtil.isNotNullAndNotEmpty(nonCDMTypes))
                            typeList = FrameworkUtil.split(nonCDMTypes, ",");
                        // Modified for IR-264331 start
                        if (domObj.isKindOf(context, CommonDocument.TYPE_DOCUMENTS)) {
                            Iterator itr = typeList.iterator();
                            while (itr.hasNext()) {
                                typeSpec = (String) itr.next();
                                typeSpec = PropertyUtil.getSchemaProperty(context, typeSpec);
                                if (domObj.isKindOf(context, typeSpec)) {
                                    bSpec = false;
                                    break;
                                }
                            }
                        }

                        if (domObj.isKindOf(context, CommonDocument.TYPE_DOCUMENTS) && bSpec) {
                            CommonDocument docItem = new CommonDocument(domObj);
                            // Fix for FindBugs issue Bad use of return value from method: Suchit Gangurde: 28 Feb 2017
                            docItem = docItem.revise(context, true);
                        } else {
                            domObj.reviseObject(context, false);
                        }

                        // Modified for IR-264331 end

                        // Selecting the latest revision business id
                        strLatestRevisionId = (String) domObj.getInfo(context, "last.id");
                        // relType = new RelationshipType(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);
                        // Adding the above latest revision object to
                        // Implemented Item of CA
                        changeAction.connectImplementedItems(context, new StringList(strLatestRevisionId));
                        latestRevisionMap.put(strId, strLatestRevisionId);
                    }
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in reviseAndConnectToImplementedItems: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

        return 0;
    }

    /**
     * The Action trigger method on (Pending --> In Work) to set current date as the Actual Start Date of Change Action
     * @param context
     * @param args
     *            (Change Action Id)
     * @throws Exception
     */
    public void setActualStartDate(Context context, String[] args) throws Exception {
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }
            String strObjId = args[0];
            this.setId(strObjId);
            SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
            String strActualStartDate = _mxDateFormat.format(new Date());

            // Setting the Current Date to the Actual Start Date.
            setAttributeValue(context, ATTRIBUTE_ACTUAL_START_DATE, strActualStartDate);
            // Below code is added to address use case where in CO has more than
            // one CAs and one of the CA is promoted to In Work state. In this
            // case the only Tech. Assginee of first CA is made owner of CA and
            // than the other CAs are promoted.
            // In this use case even after CA promotion to In Work the Tech.
            // Asignee is not made owner. IR-346050-3DEXPERIENCER2015x
            String strTechAssignee = getInfo(context, "from[" + PropertyUtil.getSchemaProperty(context, "relationship_TechnicalAssignee") + "].to.name");
            if (getOwner(context).toString().compareToIgnoreCase(strTechAssignee) != 0) {
                setOwner(context, strTechAssignee);
            }

        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setActualStartDate: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Check Trigger on (Pending --> In Work) to check whether the context change Action's implemented items are not connected to the other Change Actions which are in "In Work" State.
     * @param context
     * @param args
     *            (Change Action Id and Notice)
     * @return (0 = pass, 1= block with notice)
     * @throws Exception
     */
    public int validateImplementedItems(Context context, String[] args) throws Exception {
        int iReturn = 0;
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }
            Map tempMap = null;
            String strImplementedObjId = "";
            String strObjId = args[0];
            String strMessage = "";

            String strNotice = EnoviaResourceBundle.getProperty(context, args[2], context.getLocale(), args[1]);

            // Get All Implemented items from the Chnage Action
            // MapList mlImplementedItems = new ChangeAction(strObjId).getImplementedItems(context);
            ChangeAction objChangeAction = new ChangeAction(strObjId);
            MapList mlImplementedItems = objChangeAction.getImplementedItems(context);

            for (Object var : mlImplementedItems) {
                tempMap = (Map) var;
                strImplementedObjId = (String) tempMap.get(SELECT_ID);
                // Get Change Action which are in In Work State
                strMessage = getChangeAction(context, strImplementedObjId, strObjId);
            }

            // If Message Is not empty, send send a notice and block the
            // Promotion.
            if (strMessage != null && !strMessage.isEmpty()) {
                emxContextUtilBase_mxJPO.mqlNotice(context, strNotice + strMessage);
                iReturn = 1;
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in validateImplementedItems: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
        return iReturn;
    }

    /**
     * Subsidiary method to get the change action connected to the Item, which are "In Work" state.
     * @param context
     * @param strImplementedObjId
     * @param strObjId
     * @return String name of Change Actions which are in InWork State
     * @throws Exception
     */
    public String getChangeAction(Context context, String strImplementedObjId, String strObjId) throws Exception {
        StringBuffer sbOutput = new StringBuffer();
        try {
            String strChangeActionName = "";
            String strChangeActionId = "";
            Map tempMap = null;
            String STATE_CA_IN_WORK = PropertyUtil.getSchemaProperty(context, "policy", ChangeConstants.POLICY_CHANGE_ACTION, "state_InWork");
            this.setId(strImplementedObjId);
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(SELECT_NAME);
            slObjectSelects.add(SELECT_ID);
            slObjectSelects.add(SELECT_CURRENT);

            String strWhere = SELECT_CURRENT + " == \"" + STATE_CA_IN_WORK + "\"";

            MapList mlChangeActionList = getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, // relationship
                    // pattern
                    ChangeConstants.TYPE_CHANGE_ACTION, // object pattern
                    slObjectSelects, // object selects
                    new StringList(DomainRelationship.SELECT_ID), // relationship
                    // selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    strWhere, // object where clause
                    "", 0); // relationship where clause

            for (Object var : mlChangeActionList) {
                tempMap = (Map) var;
                strChangeActionName = (String) tempMap.get(SELECT_NAME);
                strChangeActionId = (String) tempMap.get(SELECT_ID);

                if (strChangeActionId != null && !strChangeActionId.equals(strObjId)) {
                    sbOutput = addToStringBuffer(context, sbOutput, strChangeActionName);
                }
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeAction: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
        return sbOutput.toString();
    }

    /**
     * Subsidiary method to get the Ids as per the selects and convrting to array.
     * @param context
     * @param strSelect
     * @param ObjectId
     * @return String array
     * @throws Exception
     */
    public String[] getConnectedObjects(Context context, String strSelect, String ObjectId) throws Exception {
        String[] ObjArr = null;
        try {
            setId(ObjectId);
            StringList slObjectRel = getInfoList(context, strSelect);
            ObjArr = (String[]) slObjectRel.toArray(new String[slObjectRel.size()]);
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getConnectedObjects: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
        return ObjArr;
    }

    /**
     * Check Trigger on (In Work -->> In Approval) to check whether the Route Template or the Senior technical Assignee is connected to Change Action.
     * @param context
     * @param args
     *            (Change Action ID and Notice)
     * @return (0 = pass, 1 = block the promotion)
     * @throws Exception
     */
    public int checkForSrTechnicalAssigneeAndRouteTemplate(Context context, String[] args) throws Exception {
        int iReturn = 0;
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String objectId = args[0];// Change Object Id
        String strReviewerRouteTemplate = args[1];

        try {
            // create change object with the context Object Id
            setId(objectId);
            StringList selectStmts = new StringList(1);
            selectStmts.addElement("attribute[" + ATTRIBUTE_ROUTE_BASE_PURPOSE + "]");

            String whrClause = "attribute[" + ATTRIBUTE_ROUTE_BASE_PURPOSE + "] match '" + strReviewerRouteTemplate + "' && current == Active";

            // get route template objects from change object
            // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
            MapList mlRouteTemplate = getRelatedObjects(context, RELATIONSHIP_OBJECT_ROUTE, TYPE_ROUTE_TEMPLATE, selectStmts, null, false, true, (short) 1, whrClause, null, 0);

            // get the Senior Technical Assignee connected
            String strResponsibleOrgRelId = getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_SENIOR_TECHNICAL_ASSIGNEE + "].id");

            // Send notice and block promotion if both are not connected.
            if ((mlRouteTemplate == null || mlRouteTemplate.isEmpty()) && ChangeUtil.isNullOrEmpty(strResponsibleOrgRelId)) {
                String strMsg = EnoviaResourceBundle.getProperty(context, args[3], context.getLocale(), args[2]);
                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                iReturn = 1;
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForSrTechnicalAssigneeAndRouteTemplate: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return iReturn;
    }

    /**
     * Action trigger on (InWork--> In Approval) to Promote all the Implemented/Affected Items connected to the Change Action to Approved State.
     * @param context
     * @param args
     *            (Change Action Id)
     * @throws Exception
     */
    public void promoteItemsToApproved(Context context, String[] args) throws Exception {
        try {
            String strItem = "";
            String strItemType = "";
            String strItemPolicy = "";
            String strChangeActionId = args[0];
            // String strCurrentState = args[1];
            String strTargetState = args[2];
            setId(strChangeActionId);
            // String STATE_CA_INAPPROVAL = PropertyUtil.getSchemaProperty(context, "policy", ChangeConstants.POLICY_CHANGE_ACTION, "state_InApproval");
            StringList objSelects = new StringList(SELECT_ID);
            objSelects.addElement(SELECT_TYPE);
            objSelects.addElement(SELECT_POLICY);

            MapList ImplementedItems = null;
            Map<String, String> implementedItemMap;
            String relWhereClause = "attribute[" + ATTRIBUTE_REQUESTED_CHANGE + "] == '" + ChangeConstants.FOR_RELEASE + "'";
            String stateApprovedMapping = "";
            boolean strAutoApproveValue = false;

            // if(!ChangeUtil.isNullOrEmpty(strTargetState) &&
            // STATE_CA_INAPPROVAL.equalsIgnoreCase(strTargetState))
            if (!ChangeUtil.isNullOrEmpty(strTargetState) && TigerConstants.STATE_CHANGEACTION_INAPPROVAL.equalsIgnoreCase(strTargetState)) {
                // Get the Implemented Items connected.
                ImplementedItems = getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, // relationship
                        // pattern
                        "*", // object pattern
                        objSelects, // object selects
                        new StringList(DomainRelationship.SELECT_ID), // relationship
                        // selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        EMPTY_STRING, // object where clause
                        relWhereClause, (short) 0); // relationship where clause

                Map relItemTypPolicyDtls = new HashMap();
                // Set the Approved State on the Implemented Items
                for (Object var : ImplementedItems) {
                    implementedItemMap = (Map<String, String>) var;
                    strItem = implementedItemMap.get(SELECT_ID);
                    strItemType = implementedItemMap.get(SELECT_TYPE);
                    strItemPolicy = implementedItemMap.get(SELECT_POLICY);
                    stateApprovedMapping = ECMAdmin.getApproveStateValue(context, strItemType, strItemPolicy);
                    strAutoApproveValue = ECMAdmin.getAutoApprovalValue(context, strItemType, strItemPolicy);
                    if (strAutoApproveValue && !ChangeUtil.isNullOrEmpty(stateApprovedMapping)) {
                        relItemTypPolicyDtls.put(strItem, strItemPolicy + "|" + strItemType);
                    }
                }

                if (!relItemTypPolicyDtls.isEmpty())
                    ECMAdmin.enforceApproveOrder(context, relItemTypPolicyDtls);
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteItemsToApproved: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Action Trigger on (InApproval-- > Approved) to Set the current date as the Actual Completion Date
     * @param context
     * @param args
     *            (Cahnge Action Id)
     * @throws Exception
     */
    public void setActualCompletionDate(Context context, String[] args) throws Exception {
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }
            String strObjId = args[0];
            this.setId(strObjId);
            SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
            String strActualCompletionDate = _mxDateFormat.format(new Date());
            // Set the Actual Completion Date
            setAttributeValue(context, ATTRIBUTE_ACTUAL_COMPLETION_DATE, strActualCompletionDate);
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setActualCompletionDate: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Action trigger on (Approved --> Complete) to Promote the Implemented Items as per the Requested Change attribute on the relationship.
     * @param context
     * @param args
     *            (Change Action Id)
     * @throws Exception
     */
    public void promoteImplementedItemsAsRequestedChange(Context context, String[] args) throws Exception {
        try {
            Map tempMap = null;
            String strRequestedChange = "";
            String strItem = "";
            String strItemPolicy = "";
            String strItemType = "";
            String targetStateName = "";
            String strRelType = "";
            String strChangeActionId = args[0];
            setId(strChangeActionId);
            StringList busSelects = new StringList(SELECT_ID);
            busSelects.add(SELECT_POLICY);
            busSelects.add(SELECT_TYPE);
            StringList relSelects = new StringList(SELECT_RELATIONSHIP_ID);
            relSelects.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
            relSelects.add(SELECT_RELATIONSHIP_TYPE);
            StringList obsoleteItems = new StringList();
            StringList releasedItems = new StringList();
            StringList updatedItems = new StringList();

            // Get the implemented items & Affected Items connected.
            MapList listItems = getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "," + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, "*", busSelects, relSelects, false,
                    true, (short) 1, EMPTY_STRING, EMPTY_STRING, (short) 0);

            Map relItemTypPolicyDtls = new HashMap();

            // promote Implemented items as per the Requested change attribute
            for (Object var : listItems) {
                targetStateName = null;
                tempMap = (Map) var;
                strRequestedChange = (String) tempMap.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                strItem = (String) tempMap.get(SELECT_ID);
                strItemPolicy = (String) tempMap.get(SELECT_POLICY);
                strItemType = (String) tempMap.get(SELECT_TYPE);
                strRelType = (String) tempMap.get(SELECT_RELATIONSHIP_TYPE);

                if (ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strRequestedChange)) {
                    targetStateName = ECMAdmin.getObsoleteStateValue(context, strItemType, strItemPolicy);

                    // Obsoleting items
                    setId(strItem);
                    setState(context, targetStateName);

                    if (ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM.equalsIgnoreCase(strRelType))
                        obsoleteItems.addElement(strItem);
                }

                if (ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strRequestedChange)) {

                    relItemTypPolicyDtls.put(strItem, strItemPolicy + "|" + strItemType);

                    targetStateName = ECMAdmin.getReleaseStateValue(context, strItemType, strItemPolicy);
                    if (ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM.equalsIgnoreCase(strRelType))
                        releasedItems.addElement(strItem);
                }

                if (ChangeConstants.FOR_UPDATE.equalsIgnoreCase(strRequestedChange) && ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM.equalsIgnoreCase(strRelType)) {
                    updatedItems.addElement(strItem);
                }

                /*
                 * if(!ChangeUtil.isNullOrEmpty(targetStateName)) { setId(strItem); setState(context, targetStateName); }
                 */
            }
            PropertyUtil.setRPEValue(context, "MX_SKIP_PART_PROMOTE_CHECK", "true", false);
            // Logic to RELEASE affecited/Implemented items in order as per the
            // admin settings
            ECMAdmin.enforceReleaseOrder(context, relItemTypPolicyDtls);

            ChangeAction changeAction = new ChangeAction(strChangeActionId);
            // Connects all the affected items
            changeAction.connectImplementedItems(context, obsoleteItems, ChangeConstants.FOR_OBSOLESCENCE);
            changeAction.connectImplementedItems(context, releasedItems, ChangeConstants.FOR_RELEASE);
            changeAction.connectImplementedItems(context, updatedItems, ChangeConstants.FOR_UPDATE);

        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteImplementedItemsAsRequestedChange: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Action Trigger (InWork --> In Approval) to check whether Route Template is connected to the Change Action, If not get the Senior Technical Assignee and set as the Owner.
     * @param context
     * @param args
     *            (Change Action Id)
     * @throws Exception
     */
    public void setOwner(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            String objectId = args[0];// Change Object Id
            String strReviewerRouteTemplate = args[1];

            setId(objectId);
            StringList selectStmts = new StringList(1);
            selectStmts.addElement("attribute[" + ATTRIBUTE_ROUTE_BASE_PURPOSE + "]");

            String whrClause = "attribute[" + ATTRIBUTE_ROUTE_BASE_PURPOSE + "] match '" + strReviewerRouteTemplate + "' && current == Active";

            // get route template objects from change object
            // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
            MapList mlRouteTemplate = getRelatedObjects(context, RELATIONSHIP_OBJECT_ROUTE, TYPE_ROUTE_TEMPLATE, selectStmts, null, false, true, (short) 1, whrClause, null, 0);

            // If not Route template is connected to the Change Action, the get
            // the Senior Technical Assignee and set as change Action Owner.
            if (mlRouteTemplate == null || mlRouteTemplate.isEmpty()) {

                String strSeniorTechAssignee = getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_SENIOR_TECHNICAL_ASSIGNEE + "].to.name");
                setOwner(context, strSeniorTechAssignee);
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setOwner: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Action Trigger on (InApproval --> Approved) to check whether the context Change Action is the ast Change Action to be Approved. If so then Promote the Change Order to "In Approval" state and
     * notify CO Owner.
     * @param context
     * @param args
     *            (Change Action Id and Notice)
     * @throws Exception
     */
    public void checkForLastCA(Context context, String args[]) throws Exception {
        try {
            String strCAId;
            String strCAState;
            String strCAPolicy;
            String strChangeOrderId = null;
            String strChangeOrderPolicy = null;
            String strRoutetemplate = null;
            String strCCAId = null;
            Map tempMap = null;

            StringList listChangeActionAllStates;
            boolean pendingChangeExists = false;
            String objectId = args[0];// Change Object Id
            setId(objectId);

            StringList slObjectSelect = new StringList(4);
            slObjectSelect.add(SELECT_ID);
            slObjectSelect.add(SELECT_POLICY);
            slObjectSelect.add(
                    "from[" + RELATIONSHIP_OBJECT_ROUTE + "|to.type=='" + TYPE_ROUTE_TEMPLATE + "' && to.revision == to.last &&  attribute[" + ATTRIBUTE_ROUTE_BASE_PURPOSE + "]==Approval].to.name");
            slObjectSelect.add("from[" + RELATIONSHIP_OBJECT_ROUTE + "|to.type=='" + TYPE_ROUTE + "' &&  attribute[" + ATTRIBUTE_ROUTE_BASE_STATE + "]=='"
                    + TigerConstants.STATE_CHANGEACTION_INAPPROVAL + "'].to.name");

            MapList resultList = getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ORDER, slObjectSelect, null, true, false, (short) 1, "",
                    EMPTY_STRING, 0);

            if (resultList != null && !resultList.isEmpty()) {
                for (Object var : resultList) {
                    tempMap = (Map) var;
                    strChangeOrderId = (String) tempMap.get(SELECT_ID);
                    strChangeOrderPolicy = (String) tempMap.get(SELECT_POLICY);
                    if (tempMap.get("from[" + RELATIONSHIP_OBJECT_ROUTE + "].to.name") instanceof StringList) {
                        // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                        StringList slRouteList = (StringList) tempMap.get("from[" + RELATIONSHIP_OBJECT_ROUTE + "].to.name");
                        strRoutetemplate = (String) slRouteList.get(0);
                    } else {
                        strRoutetemplate = (String) tempMap.get("from[" + RELATIONSHIP_OBJECT_ROUTE + "].to.name");
                    }

                }
            }

            if (UIUtil.isNotNullAndNotEmpty(strChangeOrderId)) {
                // Get Change Actions connected to Change Order
                MapList mlChangeActions = getChangeActions(context, strChangeOrderId);
                HashMap releaseStateMap = ChangeUtil.getReleasePolicyStates(context);

                Map mapTemp;
                for (Object var : mlChangeActions) {
                    mapTemp = (Map) var;
                    strCAId = (String) mapTemp.get(SELECT_ID);
                    if (!strCAId.equals(objectId)) {
                        strCAState = (String) mapTemp.get(SELECT_CURRENT);
                        strCAPolicy = (String) mapTemp.get(SELECT_POLICY);
                        listChangeActionAllStates = ChangeUtil.getAllStates(context, strCAPolicy);
                        ChangeUtil changeUtil = new ChangeUtil();
                        if (changeUtil.checkObjState(context, listChangeActionAllStates, strCAState, (String) releaseStateMap.get(strCAPolicy), ChangeConstants.LT) == 0) {

                            // if (new ChangeUtil().checkObjState(context, listChangeActionAllStates, strCAState, (String) releaseStateMap.get(strCAPolicy), ChangeConstants.LT) == 0) {
                            if (ChangeConstants.TYPE_CCA.equals((String) mapTemp.get(SELECT_TYPE))) {
                                String affectedItemExits = DomainObject.newInstance(context, strCAId).getInfo(context, "from[" + DomainConstants.RELATIONSHIP_AFFECTED_ITEM + "]");
                                if ("True".equalsIgnoreCase(affectedItemExits)) {
                                    pendingChangeExists = true;
                                    break;
                                } else {
                                    strCCAId = strCAId;
                                }
                            } else {
                                pendingChangeExists = true;
                                break;
                            }
                        }
                    }
                }

                // If flag is empty, then set the CO state and notify the owner.
                if (!pendingChangeExists) {
                    setId(strChangeOrderId);
                    if (UIUtil.isNotNullAndNotEmpty(strRoutetemplate)) {
                        setState(context, TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL);
                    } else {
                        setState(context, TigerConstants.STATE_PSS_CHANGEORDER_COMPLETE);
                    }

                    emxNotificationUtilBase_mxJPO.sendNotification(context, strChangeOrderId, new StringList(getOwner(context).getName()), new StringList(), new StringList(), args[1], args[2],
                            new StringList(), args[3], null, null, null);
                    if (strCCAId != null) {
                        DomainObject.deleteObjects(context, new String[] { strCCAId });
                    }
                }
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForLastCA: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Subsidiary method to get Change Actions connected to the Change Order
     * @param context
     * @param strChangeOrderId
     * @return
     * @throws Exception
     */
    public MapList getChangeActions(Context context, String strChangeOrderId) throws Exception {
        StringList slObjectSelect = new StringList(4);
        slObjectSelect.add(SELECT_ID);
        slObjectSelect.add(SELECT_NAME);
        slObjectSelect.add(SELECT_CURRENT);
        slObjectSelect.add(SELECT_TYPE);
        slObjectSelect.add(SELECT_POLICY);
        StringList slRelSelect = new StringList(SELECT_RELATIONSHIP_ID);
        setId(strChangeOrderId);
        return getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, DomainConstants.QUERY_WILDCARD, slObjectSelect, slRelSelect, false, true, (short) 1, "", EMPTY_STRING, 0);
    }

    /**
     * Method is called from TransferOwnerShip commands in Dashboard. It will identify the Person objects with specific roles from the XML depending on the affected Item's Type and Policy. It will
     * identify the person from Responsible Organization of the CA. If not present, it will fetch the RO from Change Order. Based on the functionality it will either be TechAssignee or SrTechAssignee
     * @param context
     * @param args
     * @return String - representing the Org ID and Role
     * @throws Exception
     */
    public String checkAssigneeRole(Context context, String[] args) throws Exception {
        try {
            // unpacking the Arguments from variable args
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);
            String strObjectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
            boolean isTechAssignee = true;
            boolean isSrTechAssignee = false;
            String sfunctionality = (String) requestMap.get("sfunctionality");
            if ("transferOwnershipToSrTechnicalAssignee".equals(sfunctionality)) {
                isTechAssignee = false;
                isSrTechAssignee = true;
            }
            return getRoleDynamicSearchQuery(context, strObjectId, isTechAssignee, isSrTechAssignee);
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkAssigneeRole: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Method is called from CATransferOwnerShip commands in Properties page. It will identify the Person objects with specific roles from the XML depending on the affected Item's Type and Policy. It
     * will identify the person from Responsible Organization of the CA. If not present, it will fetch the RO from Change Order.
     * @param context
     * @param args
     * @return String - representing the Org ID and Role
     * @throws Exception
     */
    public String getTechAssigneeandSrTechAssigneeRoleDynamicSearchQuery(Context context, String[] args) throws Exception {
        try {
            // unpacking the Arguments from variable args
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);
            String strObjectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
            return getRoleDynamicSearchQuery(context, strObjectId, true, true);
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getTechAssigneeandSrTechAssigneeRoleDynamicSearchQuery: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Method is called from Href of the Technical Assignee on the CA Property field. It will identify the Person objects with specific roles from the XML depending on the affected Item's Type and
     * Policy. It will identify the person from Responsible Organization of the CA. If not present, it will fetch the RO from Change Order.
     * @param context
     * @param args
     * @return String - representing the Org ID and Role
     * @throws Exception
     */
    public String getTechAssigneeRoleDynamicSearchQuery(Context context, String[] args) throws Exception {
        try {
            // unpacking the Arguments from variable args
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);
            HashMap fieldMap = (HashMap) programMap.get(ChangeConstants.FIELD_VALUES);
            HashMap typeAheadMap = (HashMap) programMap.get("typeAheadMap");
            String strObjectId = fieldMap != null ? (String) fieldMap.get(ChangeConstants.ROW_OBJECT_ID) : "";
            if (UIUtil.isNullOrEmpty(strObjectId) && typeAheadMap != null) {
                strObjectId = (String) typeAheadMap.get(ChangeConstants.ROW_OBJECT_ID);
            }
            if (UIUtil.isNullOrEmpty(strObjectId)) {
                strObjectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
            }
            return getRoleDynamicSearchQuery(context, strObjectId, true, false);
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getTechAssigneeRoleDynamicSearchQuery: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Method is called from Href of the Senior Technical Assignee on the CA Property field. It will identify the Person objects with specific roles from the XML depending on the affected Item's Type
     * and Policy. It will identify the person from Responsible Organization of the CA. If not present, it will fetch the RO from Change Order.
     * @param context
     * @param args
     * @return String - representing the Org ID and Role
     * @throws Exception
     */
    public String getSrTechAssigneeRoleDynamicSearchQuery(Context context, String[] args) throws Exception {
        try {
            // unpacking the Arguments from variable args
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);
            HashMap fieldMap = (HashMap) programMap.get(ChangeConstants.FIELD_VALUES);
            HashMap typeAheadMap = (HashMap) programMap.get("typeAheadMap");
            String strObjectId = fieldMap != null ? (String) fieldMap.get(ChangeConstants.ROW_OBJECT_ID) : "";
            if (UIUtil.isNullOrEmpty(strObjectId) && typeAheadMap != null) {
                strObjectId = (String) typeAheadMap.get(ChangeConstants.ROW_OBJECT_ID);
            }
            if (UIUtil.isNullOrEmpty(strObjectId)) {
                strObjectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
            }
            return getRoleDynamicSearchQuery(context, strObjectId, false, true);
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getSrTechAssigneeRoleDynamicSearchQuery: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Subsidiary method for the getTechAssigneeRoleDynamicSearchQuery & getSrTechAssigneeRoleDynamicSearchQuery
     * @param context
     * @param strObjectId
     *            - Change Action Id
     * @param isTechRole
     *            - boolean for TechAssignee or Senior TechAssignee
     * @return String
     * @throws Exception
     */
    public String getRoleDynamicSearchQuery(Context context, String strObjectId, boolean isTechRole, boolean isSrTechRole) throws Exception {
        try {
            setId(strObjectId);
            String strTechRole = "";
            String strSrTechRole = "";
            String strResponsibleOrg = "";
            String strIndiviAffectedItemType = "";
            String strIndiviAffectedItemPolicy = "";
            ChangeAction changeActionInstance = new ChangeAction();
            String strAffectedItemType = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.type";
            String strAffectedItemPolicy = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.policy";
            StringList slSelects = new StringList();
            slSelects.add(strAffectedItemType);
            slSelects.add(strAffectedItemPolicy);

            StringBuffer strRole = new StringBuffer();
            strResponsibleOrg = changeActionInstance.getResponsibleOrganization(context, strObjectId);

            // Get the Affected item connected to the Change Action.
            Map mapAffectedItemDetails = getInfo(context, slSelects);

            if (mapAffectedItemDetails != null && !mapAffectedItemDetails.isEmpty()) {
                strIndiviAffectedItemType = (String) mapAffectedItemDetails.get(strAffectedItemType);
                strIndiviAffectedItemPolicy = (String) mapAffectedItemDetails.get(strAffectedItemPolicy);

                // Get the Role from XML with the Type and Policy of Affected
                // Item.
                if (isTechRole) {
                    strTechRole = ECMAdmin.getTechAssigneeRole(context, strIndiviAffectedItemType, strIndiviAffectedItemPolicy);
                }
                if (isSrTechRole) {
                    strSrTechRole = ECMAdmin.getSrTechAssigneeRole(context, strIndiviAffectedItemType, strIndiviAffectedItemPolicy);
                }

            }
            if (UIUtil.isNotNullAndNotEmpty(strTechRole)) {
                strRole.append(strTechRole);
            }
            if (UIUtil.isNotNullAndNotEmpty(strSrTechRole)) {
                if (strRole.length() > 0) {
                    strRole.append(",");
                }
                strRole.append(strSrTechRole);
            }

            return "MEMBER_ID=" + strResponsibleOrg + ":USERROLE=" + strRole.toString();
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getRoleDynamicSearchQuery: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
    }

    /**
     * Displays the Range Values on Edit for Attribute Requested Change at COAffectedItemsTable/CAAffectedItemsTable..
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds a HashMap containing the following entries:
     * @param HashMap
     *            containing the following keys, "objectId"
     * @return HashMap contains actual and display values
     * @throws Exception
     *             if operation fails
     * @since ECM R211
     */
    public HashMap displayRequestedChangeRangeValues(Context context, String[] args) throws Exception {
        String strLanguage = context.getSession().getLanguage();
        StringList strListRequestedChange = FrameworkUtil.getRanges(context, ATTRIBUTE_REQUESTED_CHANGE);
        HashMap rangeMap = new HashMap();

        StringList listChoices = new StringList();
        StringList listDispChoices = new StringList();

        String attrValue = "";
        String dispValue = "";

        for (int i = 0; i < strListRequestedChange.size(); i++) {
            attrValue = (String) strListRequestedChange.get(i);
            // TIGTK-7850 - 17-05-2017 - PTE - START
            // Commented for TIGTK-17435 : Start
            // if (UIUtil.isNotNullAndNotEmpty(attrValue) && !(attrValue.equals("None") || attrValue.equals(TigerConstants.FOR_CLONE) || attrValue.equals(TigerConstants.FOR_REPLACE))) {
            // Commented for TIGTK-17435 : End

            dispValue = i18nNow.getRangeI18NString(ATTRIBUTE_REQUESTED_CHANGE, attrValue, strLanguage);
            listDispChoices.add(dispValue);
            listChoices.add(attrValue);

            // Commented for TIGTK-17435 : Start
            /*
             * }
             * 
             * 
             * if (UIUtil.isNotNullAndNotEmpty(attrValue) && !(attrValue.equals("None") || attrValue.equals(TigerConstants.FOR_CLONE) || attrValue.equals(TigerConstants.FOR_REPLACE))) { dispValue =
             * i18nNow.getRangeI18NString(ATTRIBUTE_REQUESTED_CHANGE, attrValue, strLanguage); listDispChoices.add(dispValue); listChoices.add(attrValue); }
             */
            // Commented for TIGTK-17435 : End

            // TIGTK-7850 - 17-05-2017 - PTE - END
        }

        rangeMap.put("field_choices", listChoices);
        rangeMap.put("field_display_choices", listDispChoices);
        return rangeMap;
    }

    /**
     * excludeAffectedItems() method returns OIDs of Affect Items which are already connected to context change object
     * @param context
     *            Context : User's Context.
     * @param args
     *            String array
     * @return The StringList value of OIDs
     * @throws Exception
     *             if searching Parts object fails.
     */
    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeAffectedItems(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strChangeId = (String) programMap.get("objectId");
        StringList strlAffItemList = new StringList();

        if (ChangeUtil.isNullOrEmpty(strChangeId))
            return strlAffItemList;

        try {
            setId(strChangeId);
            strlAffItemList.addAll(getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id"));

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in excludeAffectedItems: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return strlAffItemList;
    }

    /**
     * Updates the Range Values for Attribute RequestedChange Based on User Selection
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds a HashMap containing the following entries: paramMap - a HashMap containing the following keys, "relId","RequestedChange"
     * @return int
     * @throws Exception
     *             if operation fails
     * @since
     **/
    public int updateRequestedChangeValues(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get(ChangeConstants.PARAM_MAP);
            // HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);

            String changeActionId = (String) paramMap.get(ChangeConstants.OBJECT_ID);
            // String changeObjId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
            String affectedItemRelId = (String) paramMap.get(ChangeConstants.SELECT_REL_ID);
            String strNewRequestedChangeValue = (String) paramMap.get(ChangeConstants.NEW_VALUE);
            changeManagement = new ChangeManagement(changeActionId);
            String affectedItemObjId = MqlUtil.mqlCommand(context, "print connection $1 select $2 dump", affectedItemRelId, "to.id");
            String message = changeManagement.updateRequestedChangeValues(context, affectedItemObjId, affectedItemRelId, strNewRequestedChangeValue);
            if ("".equals(message)) {
                return 0;// operation success
            } else {
                MqlUtil.mqlCommand(context, "notice $1", message);
                return 1;// for failure
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in updateRequestedChangeValues: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // PCM TIGTK-3951: 15/02/2017 : KWagh : START : Performance Issue
    // Modified java code to remove method "getCustomAttributes".
    // PCM TIGTK-3951: 15/02/2017 : KWagh : End : Performance Issue

    /**
     * Method to get the proper Input Type/Format settings for interface attributes
     * @param context
     * @param columnMap
     * @param attrType
     * @param choicesList
     * @param sMultiLine
     * @throws MatrixException
     */
    private void setColumnSettings(Context context, Map columnMap, String attrType, StringList choicesList, String sMultiLine) throws MatrixException {
        String strFieldFormat = "";
        String strFieldIPType = INPUT_TYPE_TEXTBOX;

        if (FORMAT_STRING.equalsIgnoreCase(attrType)) {
            strFieldIPType = INPUT_TYPE_TEXTBOX;
            if (choicesList != null && choicesList.size() > 0) {
                strFieldIPType = INPUT_TYPE_COMBOBOX;
            } else if ("true".equalsIgnoreCase(sMultiLine)) {
                strFieldIPType = INPUT_TYPE_TEXTAREA;
            }
        } else if (FORMAT_BOOLEAN.equalsIgnoreCase(attrType)) {
            strFieldIPType = INPUT_TYPE_COMBOBOX;
        } else if (FORMAT_REAL.equalsIgnoreCase(attrType)) {
            if (choicesList != null && choicesList.size() > 0) {
                strFieldIPType = INPUT_TYPE_COMBOBOX;
            }
            strFieldFormat = FORMAT_NUMERIC;
        } else if (FORMAT_TIMESTAMP.equalsIgnoreCase(attrType)) {
            strFieldFormat = FORMAT_DATE;
        } else if (FORMAT_INTEGER.equalsIgnoreCase(attrType)) {
            if (choicesList != null && choicesList.size() > 0) {
                strFieldIPType = INPUT_TYPE_COMBOBOX;
            }
            strFieldFormat = FORMAT_INTEGER;
        }

        columnMap.put(SETTING_INPUT_TYPE, strFieldIPType);
        if (strFieldFormat.length() > 0)
            columnMap.put(SETTING_FORMAT, strFieldFormat);
    }

    /**
     * The Action trigger method on (Pending --> In Work) to Promote Connected CO to In Work State
     * @param context
     * @param args
     *            (Change Action Id)
     * @throws Exception
     */
    public void promoteConnectedCO(Context context, String[] args) throws Exception {
        ChangeAction changeAction = new ChangeAction();
        changeAction.promoteConnectedCO(context, args);
        // new ChangeAction().promoteConnectedCO(context, args);

    }

    /**
     * Reset Owner on demote of ChangeAction
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - String holding the object id. 1 - String to hold state.
     * @returns void.
     * @throws Exception
     *             if the operation fails
     * @since ECM R417
     */
    public void resetOwner(Context context, String[] args) throws Exception {
        try {
            String objectId = args[0]; // changeObject ID
            setObjectId(objectId);
            String strCurrentState = args[1]; // current state of ChangeObject

            StringList select = new StringList(SELECT_OWNER);
            select.add(SELECT_ORIGINATOR);
            select.add(SELECT_POLICY);
            select.add(ChangeConstants.SELECT_TECHNICAL_ASSIGNEE_NAME);
            Map resultList = getInfo(context, select);
            String currentOwner = (String) resultList.get(SELECT_OWNER);
            // String sOriginator = (String) resultList.get(SELECT_ORIGINATOR);
            String sPolicy = (String) resultList.get(SELECT_POLICY);
            String previousOwner = (String) resultList.get(ChangeConstants.SELECT_TECHNICAL_ASSIGNEE_NAME);

            if (ChangeConstants.POLICY_CHANGE_ACTION.equalsIgnoreCase(sPolicy) && ChangeConstants.STATE_CHANGE_ACTION_INAPPROVAL.equalsIgnoreCase(strCurrentState)
                    && !ChangeUtil.isNullOrEmpty(previousOwner) && !currentOwner.equalsIgnoreCase(previousOwner)) {
                setOwner(context, previousOwner);

            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in resetOwner: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * This method is used for Get the Change Affected Item of Change Action
     * @param args
     * @returns MapList of CA's affected Item.
     * @throws Exception
     *             if the operation fails
     */

    public MapList getAffectedItems(Context context, String[] args) throws Exception {
        try {
            MapList objectList = new MapList();
            Map programMap = (HashMap) JPO.unpackArgs(args);
            String strChangeActionObjId = (String) programMap.get("objectId");

            StringList objectSelects = new StringList(1);
            objectSelects.addElement(SELECT_ID);

            DomainObject domCAObject = new DomainObject(strChangeActionObjId);

            // Get The change Affected item of CA
            MapList listChangeAffectedId = domCAObject.getRelatedObjects(context, // context
                    ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, // relationship pattern
                    DomainConstants.QUERY_WILDCARD, // object pattern
                    objectSelects, // object selects
                    new StringList("id[connection]"), // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    ""); // relationship where clause

            for (int i = 0; i < listChangeAffectedId.size(); i++) {
                Map domChanegeAffectedItem = (Map) listChangeAffectedId.get(i);
                String strAffectedItemId = (String) domChanegeAffectedItem.get("id");
                String strAffectedItemIdConnaction = (String) domChanegeAffectedItem.get("id[connection]");
                DomainObject domAffectedItemObject = new DomainObject(strAffectedItemId);
                String strAffectedItemCurrentState = domAffectedItemObject.getInfo(context, SELECT_CURRENT);
                
                HashMap mapObject = new HashMap();
                // PCM : TIGTK-4276 : 09/02/2017 : AB : START
                // TIGTK-14264 - add one more state Part review
                if (TigerConstants.STATE_RELEASED_CAD_OBJECT.equalsIgnoreCase(strAffectedItemCurrentState) || TigerConstants.STATE_PART_RELEASE.equalsIgnoreCase(strAffectedItemCurrentState)
                        || strAffectedItemCurrentState.equalsIgnoreCase("In Work") || strAffectedItemCurrentState.equalsIgnoreCase("Preliminary") || TigerConstants.STATE_PART_REVIEW.equalsIgnoreCase(strAffectedItemCurrentState)) {
                    mapObject.put("id", strAffectedItemId);
                    mapObject.put("id[connection]", strAffectedItemIdConnaction);
                    mapObject.put("RowEditable", "show");
                } else {
                    mapObject.put("id", strAffectedItemId);
                    mapObject.put("id[connection]", strAffectedItemIdConnaction);
                    mapObject.put("RowEditable", "readonly");
                }
                // PCM : TIGTK-4276 : 09/02/2017 : AB : END

                objectList.add(mapObject);
            }
            return objectList;
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getAffectedItems: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * This method is used for Get the Change Implemented Item of Change Action
     * @param args
     * @returns MapList of CA's Implemented Item.
     * @throws Exception
     *             if the operation fails
     */
    // TIGTK-6388:Rutuja Ekatpure:Method modified for merging CA Affected Item and Implemented Item tab:25/4/2017:start
    public MapList getImplementedItems(Context context, String[] args) throws Exception {
        try {
            MapList objectList = new MapList();
            Map programMap = (HashMap) JPO.unpackArgs(args);
            String strChangeActionObjId = (String) programMap.get("objectId");
            DomainObject domCAObject = new DomainObject(strChangeActionObjId);

            StringList releasedState = new StringList();
            releasedState.add(TigerConstants.STATE_RELEASED_CAD_OBJECT);
            releasedState.add(TigerConstants.STATE_PART_RELEASE);

            StringList objectSelects = new StringList();
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_CURRENT);
            // START :: TIGTK-14892 :: ALM-4106 :: PSI
            objectSelects.addElement(DomainConstants.SELECT_NAME);
            objectSelects.addElement(DomainConstants.SELECT_POLICY);
            // END :: TIGTK-14892 :: ALM-4106 :: PSI

            StringList allowedRequestedChangeValuesForAffectedItems = new StringList();
            allowedRequestedChangeValuesForAffectedItems.add(ChangeConstants.FOR_OBSOLESCENCE);
            allowedRequestedChangeValuesForAffectedItems.add(ChangeConstants.FOR_RELEASE);

            StringList relSelects = new StringList();
            relSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            relSelects.addElement(DomainRelationship.SELECT_NAME);
            relSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_REQUESTED_CHANGE + "]");

            Pattern relPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
            relPattern.addPattern(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);

            MapList mlCAAffectedAndImplmentedItems = domCAObject.getRelatedObjects(context, // context
                    relPattern.getPattern(), // relationship pattern
                    DomainConstants.QUERY_WILDCARD, // object pattern
                    objectSelects, // object selects
                    relSelects, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    "", 0); // relationship where clause

            Iterator itrAffectedItem = mlCAAffectedAndImplmentedItems.iterator();

            while (itrAffectedItem.hasNext()) {
                Map mapCAAffectedAndImplementedItem = (Map) itrAffectedItem.next();
                String attrRequestedChange = (String) mapCAAffectedAndImplementedItem.get("attribute[" + DomainConstants.ATTRIBUTE_REQUESTED_CHANGE + "]");
                String relname = (String) mapCAAffectedAndImplementedItem.get(DomainRelationship.SELECT_NAME);
                String current = (String) mapCAAffectedAndImplementedItem.get(DomainConstants.SELECT_CURRENT);
                if (ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM.equals(relname) && allowedRequestedChangeValuesForAffectedItems.contains(attrRequestedChange)) {

                    if (releasedState.contains(current) || ChangeConstants.FOR_OBSOLESCENCE.equals(attrRequestedChange)) {
                        mapCAAffectedAndImplementedItem.put(TigerConstants.TABLE_SETTING_DISABLESELECTION, "true");
                    }
                    objectList.add(mapCAAffectedAndImplementedItem);
                } else if (ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM.equals(relname)) {
                    objectList.add(mapCAAffectedAndImplementedItem);
                }
            }

            return objectList;
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getImplementedItems: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // TIGTK-6388:Rutuja Ekatpure:Method modified for merging CA Affected Item and Implemented Item tab:25/4/2017:End
    /**
     * This method is used for The new Assignees to be sent email notification
     * @param args
     * @returns MapList of CA's Implemented Item.
     * @throws Exception
     *             if the operation fails
     */

    public void postAssigneeUpdate(Context context, String[] args) throws Exception {
        try {

            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }

            String strFromObjectId = args[0];
            String strToObjectId = args[1];
            String strReasonForChangeValue = "";

            // Creating Domain Object Instance of "Affected Item"
            DomainObject domAffectedItemObject = DomainObject.newInstance(context, strFromObjectId);

            // Creating Domain Object Instance of "Assignee"
            DomainObject domAssigneePersonObject = DomainObject.newInstance(context, strToObjectId);
            Map mapAffectedObjectInfo = null;
            String strLanguage = context.getSession().getLanguage();

            // Get Connected "PSS_ChangeOrder" Object Information
            StringList lstAffectedItemSelectList = new StringList();
            lstAffectedItemSelectList.add(DomainConstants.SELECT_ID);
            lstAffectedItemSelectList.add(DomainConstants.SELECT_TYPE);
            lstAffectedItemSelectList.add(DomainConstants.SELECT_NAME);
            lstAffectedItemSelectList.add(DomainConstants.SELECT_REVISION);
            lstAffectedItemSelectList.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]");
            lstAffectedItemSelectList.add("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "]");
            lstAffectedItemSelectList.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.id");
            lstAffectedItemSelectList.add("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.id");
            // lstAffectedItemSelectList.add("to[" + RELATIONSHIP_PSS_AFFECTEDITEM + "].attribute["+ChangeConstants.ATTRIBUTE_REASON_FOR_CHANGE+"].value");
            lstAffectedItemSelectList.add("to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREASONFORCHANGE + "].value");

            // Get "Assignee" Name
            String strAssigneeName = (String) domAssigneePersonObject.getInfo(context, DomainConstants.SELECT_NAME);

            // "Change Action" Object Id
            String strChangeActionObjectId = "";
            mapAffectedObjectInfo = domAffectedItemObject.getInfo(context, lstAffectedItemSelectList);

            String strAffectedObjectName = "";
            String strAffectedObjectId = "";
            String strAffectedObjectType = "";
            String strAffectedObjectRevision = "";
            if (mapAffectedObjectInfo != null) {
                String strIsChangeAffected = (String) mapAffectedObjectInfo.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]");
                String strIsImplementedAffected = (String) mapAffectedObjectInfo.get("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "]");
                strReasonForChangeValue = (String) mapAffectedObjectInfo
                        .get("to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREASONFORCHANGE + "].value");

                if (UIUtil.isNotNullAndNotEmpty(strIsChangeAffected) && "true".equalsIgnoreCase(strIsChangeAffected)) {
                    // Get Connected "Change Action" Object Id
                    strChangeActionObjectId = (String) mapAffectedObjectInfo.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.id");
                } else if (UIUtil.isNotNullAndNotEmpty(strIsImplementedAffected) && "true".equalsIgnoreCase(strIsImplementedAffected)) {
                    // Get Connected "Change Action" Object Id
                    strChangeActionObjectId = (String) mapAffectedObjectInfo.get("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.id");
                }

                // Get Affected Object Name
                strAffectedObjectName = (String) mapAffectedObjectInfo.get(DomainConstants.SELECT_NAME);
                strAffectedObjectId = (String) mapAffectedObjectInfo.get(DomainConstants.SELECT_ID);
                strAffectedObjectType = (String) mapAffectedObjectInfo.get(DomainConstants.SELECT_TYPE);
                strAffectedObjectRevision = (String) mapAffectedObjectInfo.get(DomainConstants.SELECT_REVISION);
            }

            // String strChangeActionObjectId = (String)mapAffectedObjectInfo.get("to["+ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM+"].from.id");

            // Creating Domain Object Instance for "Change Action" Object
            DomainObject domCAObject = DomainObject.newInstance(context, strChangeActionObjectId);

            // Get Connected "PSS_ChangeOrder" Object Information
            StringList lstObjectSelect = new StringList();
            lstObjectSelect.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
            lstObjectSelect.add(DomainConstants.SELECT_NAME);

            Map mapChangeActionInfo = domCAObject.getInfo(context, lstObjectSelect);
            String strConnectedCOId = (String) mapChangeActionInfo.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
            String strCAName = (String) mapChangeActionInfo.get(DomainConstants.SELECT_NAME);

            // Creating Domain Object Instance for "PSS_ChangeOrder" Object
            DomainObject domCOObject = DomainObject.newInstance(context, strConnectedCOId);

            // Get Information of "PSS_ChangeOrder" Object
            StringList lstSelectList = new StringList();
            lstSelectList.add(DomainConstants.SELECT_NAME);
            lstSelectList.add(DomainConstants.SELECT_DESCRIPTION);
            lstSelectList.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            lstSelectList.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE + "]");
            lstSelectList.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");

            Map mapChangeOrderInfo = domCOObject.getInfo(context, lstSelectList);

            String strCONumber = (String) mapChangeOrderInfo.get(DomainConstants.SELECT_NAME);
            String strCODescription = (String) mapChangeOrderInfo.get(DomainConstants.SELECT_DESCRIPTION);
            String strCOPurposeOfRelease = (String) mapChangeOrderInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            String strCOVirtualImplDate = (String) mapChangeOrderInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE + "]");
            String strProgramProjectName = (String) mapChangeOrderInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");

            // Reading Properties File Values
            String strMsgTigerKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Tiger");
            String strCAAffectedItemReassignKey1 = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.CAAffectedItemReassignKey1");
            String strCAAffectedItemReassignKey2 = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.CAAffectedItemReassignKey2");
            String strMsghasBeenAssignedForKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.hasBeenAssignedFor1");
            String strMsgAgainstStringKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.AgainstString");
            String strMsgCODescriptionKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.CODescription");
            String strMsgVirtualImplPlanDateKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage),
                    "emxFramework.Message.VirtualImplementationPlanDate");
            String strMsgCOPurposeOfReleaseKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.PurposeOfRelease");
            String strMsgReasonForChangeKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.ReasonForChange");
            String strMsgCOAffectedItemKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.AffectedItem");

            StringList toList = new StringList();
            toList.add(strAssigneeName);

            // "SubjectKey" StringBuffer
            StringBuffer subjectKey = new StringBuffer();
            subjectKey.append(strMsgTigerKey + " ");
            subjectKey.append(strCAAffectedItemReassignKey1 + " ");
            subjectKey.append(strAffectedObjectType + " ");
            subjectKey.append(strAffectedObjectName + " ");
            subjectKey.append(strAffectedObjectRevision + " ");
            subjectKey.append(strCAAffectedItemReassignKey2 + " ");
            subjectKey.append(strCAName + " ");
            subjectKey.append(strMsgAgainstStringKey + " ");
            subjectKey.append(strCONumber + " ");
            subjectKey.append(strMsghasBeenAssignedForKey + " ");
            subjectKey.append(strProgramProjectName);

            // "Message" StringBuffer
            StringBuffer msg = new StringBuffer();
            msg.append(strMsgCOAffectedItemKey + " ");

            String strBaseURL = MailUtil.getBaseURL(context);
            String strObjectURL = null;

            if (UIUtil.isNotNullAndNotEmpty(strBaseURL)) {
                int position = strBaseURL.lastIndexOf("/");
                strBaseURL = strBaseURL.substring(0, position);
                strObjectURL = strBaseURL + "/emxTree.jsp?objectId=" + strAffectedObjectId;
            }
            msg.append(strObjectURL);
            msg.append("\n");
            // msg.append(strAffectedObjectName);
            // msg.append("\n");

            msg.append(strMsgCODescriptionKey + " ");
            msg.append(strCODescription);
            msg.append("\n");
            msg.append(strMsgReasonForChangeKey + " ");
            msg.append(strReasonForChangeValue);
            msg.append("\n");
            msg.append(strMsgCOPurposeOfReleaseKey + " ");
            msg.append(strCOPurposeOfRelease);
            msg.append("\n");
            msg.append(strMsgVirtualImplPlanDateKey + " ");
            msg.append(strCOVirtualImplDate);
            msg.append("\n");

            // Send Notification to Change Manager of CR for reject Task.
            MailUtil.sendNotification(context, toList, // toList
                    null, // ccList
                    null, // bccList
                    subjectKey.toString(), // subjectKey
                    null, // subjectKeys
                    null, // subjectValues
                    msg.toString(), // messageKey
                    null, // messageKeys
                    null, // messageValues
                    new StringList(strChangeActionObjectId), // objectIdList
                    null); // companyName

        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in postAssigneeUpdate: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: END
        catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in postAssigneeUpdate: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    /**
     * Description : This method is used for Check whether CO is in 'In Work' state when CA promoted to 'In Work'
     * @author abhalani
     * @args
     * @Date Aug 4, 2016
     */

    public int checkCOInExpectedState(Context context, String args[]) throws Exception {
        int intReurn = 0;
        String strCAObjectID = args[0];
        String strExCOState = args[1];

        DomainObject domCA = new DomainObject(strCAObjectID);

        String isPromoteFromCOAction = context.getCustomData("isPromoteFromCOAction");
        if (UIUtil.isNotNullAndNotEmpty(isPromoteFromCOAction)) {
            intReurn = 0;
        } else {
            String strCOCurrent = domCA.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.current");

            // Check The state of Change Order
            if (!strExCOState.equalsIgnoreCase(strCOCurrent)) {
                intReurn = 1;
                String strMessage = "Related Change Order must be in \"" + strExCOState + "\" state.";
                emxContextUtil_mxJPO.mqlNotice(context, strMessage);
            }
        }
        return intReurn;
    }

    /**
     * @description: This Function is used to show the MCA related Affected Items in the Table.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */

    public MapList getMCAAffectedItems(Context context, String[] args) throws Exception {
        // Findbug Issue correction start
        // Date: 22/03/2017
        // By: Asha G.
        MapList mlMCAAffectedItems = null;
        // Findbug Issue correction end
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        try {
            String strMCAObjId = (String) programMap.get("objectId");
            StringList slObjSel = new StringList();
            slObjSel.add(SELECT_ID);
            DomainObject domMCAObject = new DomainObject(strMCAObjId);
            // Rutuja Ekatpure : 09/09/2016 :Start
            StringList relSelects = new StringList(6);
            relSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            // Rutuja Ekatpure : 09/09/2016 :End
            // Get The change Affected item of MCA
            mlMCAAffectedItems = domMCAObject.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, // relationship pattern
                    DomainConstants.QUERY_WILDCARD, // object pattern
                    slObjSel, // object selects
                    relSelects, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, 0); // relationship where clause
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMCAAffectedItems: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return mlMCAAffectedItems;
    }

    /**
     * This method is used for "Edit Access Function" on CA Affected item summary table for "Technical Assignee" column.
     * @param context
     * @param args
     * @throws Exception
     */
    public StringList hasEditAccessOnAffectedItemsAssignee(Context context, String args[]) throws Exception {
        StringList slReturn = null;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            slReturn = new StringList(objectList.size());
            Iterator objectItr = objectList.iterator();
            while (objectItr.hasNext()) {
                Map curObjectMap = (Map) objectItr.next();
                String strObjectID = (String) curObjectMap.get("id");

                // Get the current state of Affected Item
                DomainObject domItem = DomainObject.newInstance(context, strObjectID);
                String strItemCurrent = (String) domItem.getInfo(context, DomainConstants.SELECT_CURRENT);

                // If affected item is in released state then Don't allow Edit on Technical Assignee column for that Item
                if (strItemCurrent.contains(TigerConstants.STATE_PART_RELEASE) || strItemCurrent.contains(TigerConstants.STATE_RELEASED_CAD_OBJECT)) {
                    slReturn.addElement("false");
                } else {
                    slReturn.addElement("true");
                }
            }
        } catch (Exception ex) {
            logger.error("Error in hasEditAccessOnAffectedItemsAssignee: ", ex);
            throw ex;
        }
        return slReturn;
    }

    /**
     * This method is used in IncludeProgram for Transfer Ownership of Change ACtion PCM : TIGTK-8502 : 12/06/2017 : AB
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getContextProgramProjectMembersOfCA(Context context, String[] args) throws Exception {
        StringList slPersonList = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strChangeObjectId = (String) programMap.get("objectId");

            // Change Action Object ID
            DomainObject domCA = DomainObject.newInstance(context, strChangeObjectId);

            StringList busSelect = new StringList();
            busSelect.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from["
                    + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.id");
            busSelect.add(DomainConstants.SELECT_OWNER);

            DomainObject.MULTI_VALUE_LIST.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from["
                    + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.id");

            // Get related Program-Project Members of relavant CO and owner of the CA
            Map mapCAInfo = (Map) domCA.getInfo(context, busSelect);
            String strCAOwner = (String) mapCAInfo.get(DomainConstants.SELECT_OWNER);
            StringList slProgramProjectMembers = (StringList) mapCAInfo.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA
                    + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.id");

            if (!slProgramProjectMembers.isEmpty()) {
                int nCount = slProgramProjectMembers.size();
                busSelect.clear();
                busSelect.add(DomainConstants.SELECT_NAME);
                busSelect.add(DomainConstants.SELECT_CURRENT);

                for (int i = 0; i < nCount; i++) {
                    String strPersonID = (String) slProgramProjectMembers.get(i);
                    DomainObject domPersonObject = new DomainObject(strPersonID);
                    Map mapPersonInfo = domPersonObject.getInfo(context, busSelect);
                    String strPersonCurrent = (String) mapPersonInfo.get(DomainConstants.SELECT_CURRENT);
                    String strPersonName = (String) mapPersonInfo.get(DomainConstants.SELECT_NAME);

                    // Remove Program-Project member which is current owner of the Change Action
                    if (strPersonCurrent.equalsIgnoreCase("Active") && !strCAOwner.equalsIgnoreCase(strPersonName)) {
                        slPersonList.add(strPersonID);
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Error in getContextProgramProjectMembersOfCA: ", ex);
            throw ex;
        }
        return slPersonList;
    }

    /**
     * Description : This method is used for Transfer Ownership of Change Action Object PCM : TIGTK-8502 : 23/06/2017 : AB
     * @param context
     * @param args
     * @throws Exception
     */
    public void transferOwnership(Context context, String[] args) throws Exception {
        boolean isPushContext = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);

            String transferReason = (String) requestMap.get(ChangeConstants.TRANSFER_REASON);
            String objectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
            String newOwner = (String) requestMap.get(ChangeConstants.NEW_OWNER);

            // TransferOwnership of Change Action
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isPushContext = true;
            ChangeOrder changeOrder = new ChangeOrder(objectId);
            changeOrder.transferOwnership(context, transferReason, newOwner);
        } catch (Exception ex) {
            logger.error("Error in transferOwnership: ", ex);
            throw ex;
        } finally {
            if (isPushContext) {
                ContextUtil.popContext(context);
            }
        }

    }

    /**
     * This method is called to change Route owner when CA asignee get changed.
     * @author PTE
     * @param context
     * @param args
     * @throws Exception
     */
    public void modifyCARouteOwner(Context context, String args[]) throws Exception {
        try {
            String strAssigneeName = args[0];
            String strCAID = args[1];
            DomainObject domObj = DomainObject.newInstance(context, strCAID);
            String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            StringList relSelect = new StringList(1);
            relSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            // Get connected Route Objects

            MapList mlConnectedRoutes = domObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, slObjectSle, relSelect, false, true, (short) 1, null,
                    null, 0);
            Iterator i = mlConnectedRoutes.iterator();

            // Iterating all the Affected Items Objects
            while (i.hasNext()) {
                Map mapTemp = (Map) i.next();
                String strRouteId = (String) mapTemp.get(SELECT_ID);
                DomainObject domObjRoute = DomainObject.newInstance(context, strRouteId);

                domObjRoute.setOwner(context, strAssigneeName);
                if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strType)) {
                    StringList slRouteTaskUsers = domObjRoute.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_NODE + "].to.id");
                    if (!slRouteTaskUsers.isEmpty()) {
                        Iterator itrRouteTaskUsers = slRouteTaskUsers.iterator();
                        while (itrRouteTaskUsers.hasNext()) {
                            String strRouteTaskUserID = (String) itrRouteTaskUsers.next();

                            DomainObject domRouteTaskUser = DomainObject.newInstance(context, strRouteTaskUserID);
                            domRouteTaskUser.setOwner(context, strAssigneeName);
                        }
                    }

                }

            }
        } catch (Exception e) {

            logger.error("Error in modifyCARouteOwner: ", e);
            throw e;
        }
    }

    /**
     * Method to check and highlight Part in red on CA context if the Part gone under Policy change after adding into CO TIGTK-14892 :: ALM-4106 :: PSI
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector highlightPartsWithPolicyChange(Context context, String[] args) throws Exception {
        Vector vReturn = null;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get(ChangeConstants.OBJECT_LIST);
            String strCAcOId = (String) ((Map) programMap.get("paramList")).get("objectId");
            logger.debug(">>> programMap :: " + programMap);
            StringBuilder sbHref = null;
            vReturn = new Vector(objectList.size());
            String strAIOid = DomainConstants.EMPTY_STRING;
            String strAIPolicy = DomainConstants.EMPTY_STRING;
            String strAIName = DomainConstants.EMPTY_STRING;
            String strAIType = DomainConstants.EMPTY_STRING;
            String strRelatedCAOID = DomainConstants.EMPTY_STRING;
            String strTreeLink = DomainConstants.EMPTY_STRING;
            String strAITypeIcon = DomainConstants.EMPTY_STRING;

            DomainObject doAI = DomainObject.newInstance(context);
            DomainObject doCA = DomainObject.newInstance(context, strCAcOId);
            String strCAType = doCA.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "].value");
            StringList slObjeSelects = new StringList(3);
            slObjeSelects.add(DomainConstants.SELECT_NAME);
            slObjeSelects.add(DomainConstants.SELECT_POLICY);
            slObjeSelects.add(DomainConstants.SELECT_TYPE);
            Iterator itrAIs = objectList.iterator();
            while (itrAIs.hasNext()) {
                Map mpAI = (Map) itrAIs.next();
                strAIOid = (String) mpAI.get(DomainConstants.SELECT_ID);
                doAI.setId(strAIOid);
                mpAI = doAI.getInfo(context, slObjeSelects);
                strAIName = (String) mpAI.get(DomainConstants.SELECT_NAME);
                strAIType = (String) mpAI.get(DomainConstants.SELECT_TYPE);
                strAIPolicy = (String) mpAI.get(DomainConstants.SELECT_POLICY);
                strAITypeIcon = UINavigatorUtil.getTypeIconProperty(context, strAIType);
                sbHref = new StringBuilder(500);
                strTreeLink = "<a class=\"object\" href=\"JavaScript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=" + XSSUtil.encodeForHTMLAttribute(context, strAIOid)
                        + "', '800', '575','true','content')\"><img border='0' src='../common/images/" + XSSUtil.encodeForHTMLAttribute(context, strAITypeIcon) + "'/>"
                        + XSSUtil.encodeForHTML(context, strAIName) + "</a>";
                if (!ChangeUtil.isNullOrEmpty(strAIPolicy)
                        && ((TigerConstants.POLICY_PSS_ECPART.equals(strAIPolicy) || TigerConstants.POLICY_PSS_DEVELOPMENTPART.equals(strAIPolicy))
                                && !TigerConstants.ATTRIBUTE_PSS_CATYPE_PART.equals(strCAType))
                        || (TigerConstants.POLICY_STANDARDPART.equals(strAIPolicy) && !TigerConstants.ATTRIBUTE_PSS_CATYPE_STD.equals(strCAType))) {
                    strTreeLink = "<a class=\"object\" style=\"color:red;\" href=\"JavaScript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId="
                            + XSSUtil.encodeForHTMLAttribute(context, strAIOid) + "', '800', '575','true','content')\"><img border='0' src='../common/images/"
                            + XSSUtil.encodeForHTMLAttribute(context, strAITypeIcon) + "'/>" + XSSUtil.encodeForHTML(context, strAIName) + "</a>";
                }
                sbHref.append(strTreeLink);
                vReturn.addElement(sbHref.toString());
            }
        } catch (Exception e) {
            logger.debug(e.getLocalizedMessage(), e);
            throw new FrameworkException(e);
        }
        return vReturn;
    }

}