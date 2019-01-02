import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class PSS_emxBOMPartManagement_mxJPO extends emxBOMPartManagementBase_mxJPO {
    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @throws Exception
     *             if the operation fails.
     */
    public static final long serialVersionUID = 1L;

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxBOMPartManagement_mxJPO.class);

    public PSS_emxBOMPartManagement_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Checks if an object can be connected as an EBOM. - checks if the Parent is not in any state as indicated by the property key - emxBOMPartManagement.ECPart.AllowApply and
     * emxBOMPartManagement.DevelopmentPart.AllowApply - If the Parent part is in any of the above states then allows to be connected as EBOM else not. *
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @return int. 0 - for success, 1 - for Exception/failure.
     * @throws Exception
     *             if the operation fails.
     * @since 5/11/2017
     */
    public int checkIfParentIsReleased(Context context, String[] args) throws Exception {
        String sType = getInfo(context, SELECT_TYPE);
        String sName = getInfo(context, SELECT_NAME);
        String sRev = getInfo(context, SELECT_REVISION);
        String sPolicy = getInfo(context, SELECT_POLICY);
        String sCurrent = getInfo(context, DomainConstants.SELECT_CURRENT);
        String langStr = context.getSession().getLanguage();
        String policyClassification = getPolicyClassification(context, sPolicy);
        String propAllowLevel = "";
        String strRPEValue = PropertyUtil.getRPEValue(context, "PSS_ReplaceWithLatestRevision", true);
        if (UIUtil.isNotNullAndNotEmpty(strRPEValue) && strRPEValue.equals("True")) {
            return 0;
        }

        if ("Production".equalsIgnoreCase(policyClassification)) {
            propAllowLevel = DomainObject.STATE_PART_RELEASE;
        }
        // IR-049181 - Starts
        else if ("Unresolved".equalsIgnoreCase(policyClassification)) {
            return 0;
        }
        // IR-049181 - Ends

        if (UIUtil.isNotNullAndNotEmpty(sCurrent) && !sCurrent.equals(propAllowLevel)) {
            return 0;
        } else {

            String sErrorMsg = sType + " " + sName + " " + sRev + " "
                    + EnoviaResourceBundle.getProperty(context, "emxBOMPartManagementStringResource", context.getLocale(), "emxBOMPartManagement.EBOM.ParentInReleaseError1") + sCurrent + ". "
                    + EnoviaResourceBundle.getProperty(context, "emxBOMPartManagementStringResource", langStr, "emxBOMPartManagement.EBOM.ParentInReleaseError2");
            emxContextUtil_mxJPO.mqlNotice(context, sErrorMsg);
            return 1;
        }
    }

    // TIGTK-8594 - 2017-06-16 - VP - START
    /**
     * Floats the EBOM connections from previously released Part to the newly released Part. The following steps are performed: - Creates an "EBOM History" connection between this child and its
     * released parent assemblies. - Copies attributes from the "EBOM" connection to the "EBOM History" connection. - Floats the "EBOM" connections from parent assemblies to the new released child
     * component. - Sets the End Effectivity date on the "EBOM History" connection to the date the child is released (-1 second). - Sets the Start Effectivity date on "EBOM" connection to the date the
     * child is released.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds the following input arguments: 0 - the symbolic name of the relationship to float, either relationship_ManufacturerEquivalent or relationship_EBOM (default if none specified).
     * @throws Exception
     *             if the operation fails.
     * @since EC 9.5.JCI.0.
     * @trigger PolicyECPartStateReviewPromoteAction.
     */
    public void floatEBOMToEnd(Context context, String[] args) throws Exception {
        String strCompletingCOId = PropertyUtil.getGlobalRPEValue(context, "CompletingCOId");
        String strAffectedItemswithRequestedChange = PropertyUtil.getGlobalRPEValue(context, "sbAffectedItemswithRequestedChange");

        Map<String, String> mpReasonForChangeonFloatonRelease = new HashMap<String, String>();
        SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -1);
        Date date = cal.getTime();
        String endEffectivityDate = _mxDateFormat.format(date);
        String startEffectivityDate = _mxDateFormat.format(new Date());
        Pattern relPattern = new Pattern("");
        String RELATIONSHIP_GBOM_TO = PropertyUtil.getSchemaProperty(context, "relationship_GBOMTo");
        String sLatestRev = getInfo(context, "last.id");
        contextUtil.pushContext(context, null);
        try {

            DomainObject domCOObj = DomainObject.newInstance(context, strCompletingCOId);
            if (UIUtil.isNotNullAndNotEmpty(strAffectedItemswithRequestedChange)) {
                StringList slAffectedItemswithRequestedChange = FrameworkUtil.split(strAffectedItemswithRequestedChange, "|");
                if (!slAffectedItemswithRequestedChange.isEmpty()) {
                    for (int i = 0; i < slAffectedItemswithRequestedChange.size(); i++) {
                        StringList slReasonForChangeInfo = FrameworkUtil.split(((String) slAffectedItemswithRequestedChange.get(i)), "^");
                        if (!slReasonForChangeInfo.isEmpty()) {
                            String strRequestedChange = ((String) slReasonForChangeInfo.get(1)).replace("_", " ");
                            mpReasonForChangeonFloatonRelease.put((String) slReasonForChangeInfo.get(0), strRequestedChange);
                        }

                    }
                }
            }
            Map m = null;
            DomainRelationship historyRel = null;
            String rel_SymbolicName = args[0];
            if (!(rel_SymbolicName != null && rel_SymbolicName.equals("relationship_ManufacturerEquivalent"))) {
                relPattern.addPattern(DomainConstants.RELATIONSHIP_EBOM);
                relPattern.addPattern(RELATIONSHIP_GBOM_TO);
                relPattern.addPattern(RELATIONSHIP_GBOM);
                // TIGTK-8742 : MC : START
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_REFERENCE_EBOM);
                // TIGTK-8742 : MC : END
            }
            String strPreviousId = "";

            String checkPartVersion = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral.Check.PartVersion");
            if (checkPartVersion != null && "TRUE".equalsIgnoreCase(checkPartVersion)) {
                String sRelPartVersion = PropertyUtil.getSchemaProperty(context, "relationship_PartVersion");
                String objectSelect = "relationship[" + sRelPartVersion + "].from.id";
                strPreviousId = getInfo(context, objectSelect);
            } else {
                strPreviousId = getInfo(context, "previous.id");
            }
            if (strPreviousId == null || "".equals(strPreviousId)) {
                return;
            }

            DomainObject prevRevPart = new DomainObject(strPreviousId);
            // TIGTK-6843 : PKH : START
            String strPolicy = prevRevPart.getPolicy(context).getName();
            while (TigerConstants.POLICY_PSS_CANCELPART.equals(strPolicy)) {
                BusinessObject boObjectPart = prevRevPart.getPreviousRevision(context);
                String strObjId = boObjectPart.getObjectId(context);
                prevRevPart = DomainObject.newInstance(context, strObjId);
                strPreviousId = strObjId;
                strPolicy = prevRevPart.getPolicy(context).getName();
            }
            // TIGTK-6843 : PKH : END

            StringList selectStmts = new StringList(2);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            StringList selectRelStmts = new StringList(1);
            selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            String stateSymbolicName = "state_Release";
            String sRevQuery = "program[emxServiceUtils -method checkRevisions ${OBJECTID} " + stateSymbolicName + " HIGHEST_AND_PRESTATE_REVS] == true";

            MapList mapList = prevRevPart.getRelatedObjects(context, relPattern.getPattern(), "*", selectStmts, selectRelStmts, true, false, (short) 1, sRevQuery, null, 0);
            String strRelationName;
            Iterator i = mapList.iterator();
            ContextUtil.startTransaction(context, true);
            while (i.hasNext()) {
                // TIGTK-6340 : 17/04/2017 : AB : START
                boolean isGBOM = false;
                // TIGTK-6340 : 17/04/2017 : AB : END
                boolean floatonRelease = true;
                m = (Map) i.next();
                strRelationName = (String) m.get("relationship");
                String strRelationshipId = (String) m.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                if (strRelationName.equals(RELATIONSHIP_GBOM_TO) || strRelationName.equals(RELATIONSHIP_GBOM)) {
                    isGBOM = true;
                }

                // TIGTK-8742 : MC : START
                boolean isReferenceEBOM = false;
                if (strRelationName.equals(TigerConstants.RELATIONSHIP_PSS_REFERENCE_EBOM)) {
                    isReferenceEBOM = true;
                }
                // Added check for Reference EBOM link in below code, along with isGBOM.
                // TIGTK-8742 : MC : END
                if (!isGBOM && !isReferenceEBOM) {
                    DomainRelationship domRelationship = new DomainRelationship(strRelationshipId);
                    Map attributeMap = domRelationship.getAttributeMap(context, true);
                    historyRel = prevRevPart.addRelatedObject(context, _ebomHistory, true, (String) m.get(DomainConstants.SELECT_ID));
                    attributeMap.put(DomainConstants.ATTRIBUTE_END_EFFECTIVITY_DATE, endEffectivityDate);
                    attributeMap.remove("Effectivity Types");
                    attributeMap.remove("Effectivity Expression");
                    attributeMap.remove("Effectivity Expression Binary");
                    attributeMap.remove("Effectivity Variable Indexes");
                    attributeMap.remove("Effectivity Compiled Form");
                    attributeMap.remove("Effectivity Proposed Expression");
                    attributeMap.remove("Effectivity Ordered Criteria");
                    attributeMap.remove("Effectivity Ordered Criteria Dictionary");
                    attributeMap.remove("Effectivity Ordered Impacting Criteria");
                    // Modified for TIGTK-5354:15/03/2017:Start
                    attributeMap.remove("PSS_SimplerEffectivityExpression");
                    attributeMap.remove("PSS_CustomEffectivityExpression");
                    attributeMap.remove("PSS_ReplacedWithLatestRevision");
                    attributeMap.remove("PSS_ReferenceEBOMGenerated");
                    // Modified for TIGTK-5354:15/03/2017:End
                    historyRel.setAttributeValues(context, attributeMap);
                }

                // TIGTK-5064 : 07/03/2017 : AB : START

                DomainObject doLatestRev = new DomainObject(sLatestRev);
                String strNewPolicy = doLatestRev.getPolicy(context).getName();
                String strQuery = "from.id";
                String strCmd = "print connection \"" + strRelationshipId + "\"   select  \"" + strQuery + "\"  dump | ";
                String strRelResult = MqlUtil.mqlCommand(context, strCmd, true, true);
                DomainObject domParent = DomainObject.newInstance(context, strRelResult);
                String strParentState = domParent.getInfo(context, DomainConstants.SELECT_CURRENT);
                String strNewId = domParent.getInfo(context, "last.id");
                if (isGBOM) {
                    String sGBOMExists = doLatestRev.getInfo(context, "to[" + RELATIONSHIP_GBOM + "]");
                    if ("False".equalsIgnoreCase(sGBOMExists))
                        DomainRelationship.setToObject(context, (String) m.get(DomainConstants.SELECT_RELATIONSHIP_ID), this);
                } else {
                    if (mpReasonForChangeonFloatonRelease.containsKey(strPreviousId) && mpReasonForChangeonFloatonRelease.containsKey(strRelResult)) {
                        String strChildRequestedChange = (String) mpReasonForChangeonFloatonRelease.get(strPreviousId);
                        String strParentRequestedChange = (String) mpReasonForChangeonFloatonRelease.get(strRelResult);
                        if ((ChangeConstants.FOR_REVISE.equalsIgnoreCase(strChildRequestedChange) && ChangeConstants.FOR_REVISE.equalsIgnoreCase(strParentRequestedChange))
                                || ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strParentRequestedChange)) {
                            floatonRelease = false;
                            // DomainRelationship.setToObject(context, (String) m.get(DomainConstants.SELECT_RELATIONSHIP_ID), this);
                        }
                    }
                    if (!strNewPolicy.equals(strPolicy))
                        floatonRelease = false;
                    if (floatonRelease == true) {
                        DomainRelationship.setToObject(context, (String) m.get(DomainConstants.SELECT_RELATIONSHIP_ID), this);
                    }
                }
                // TIGTK-5064 : 07/03/2017 : AB : END

                if (!isGBOM && !isReferenceEBOM) {
                    DomainRelationship.setAttributeValue(context, (String) m.get(DomainConstants.SELECT_RELATIONSHIP_ID), DomainConstants.ATTRIBUTE_START_EFFECTIVITY_DATE, startEffectivityDate);
                }
            }

            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            logger.error("Error in Method - floatEBOMToEnd:  " + ex.toString());
            ContextUtil.abortTransaction(context);
            throw ex;
        } finally {
            contextUtil.popContext(context, null);
        }
    }

    // PCM TIGTK-3622 : 2/12/2016 : KWagh : End
    // TIGTK-8594 - 2017-06-16 - VP - END

    /**
     * This method checks if the children objects related to the parent with the specified relationships with the "to" direction have reached the target state given. The intent of this program is to
     * provide a function which checks the state of all objects of a named object type related to a parent object. The returned value will inform the parent if all the requested related objects are at
     * a given state so that the parent can be promoted to the next state.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            hold the following input arguments: args[0] - sRelationship -args[0] - Relationship to expand from, mutltiple relationships can be entered as a string delimited with spaces(" "), "~"
     *            or ",". (Optional) (default "*") Ex. relationship_PartSpecification,relationship_DrawingSpecification Passing in one of the following will expand on all relationships: * or ""
     *            (NULL). args[1] - sTargetObject - args[1] - Object to expand on, multiple objects can be entered as a string, delimited with spaces(" "), "~" or ",". (Optional) (default "*") Ex.
     *            type_Part,type_DrawingPrint Passing in one of the following will expand on all objects: * or "" (NULL). args[2] - sTargetStateProp - The state being checked for. Symbolic name must
     *            be used. (Optional) args[3] - sDirection - The direction to expand. Valid entries are "from" or "to". (Optional) (default both to and from). args[4] - sComparisonOperator - Operator
     *            to check state with. Valid entries are LT, GT, EQ, LE, GE, and NE. (Optional) (default - "EQ") args[5] - sObjectRequired - Set "required" flag if an object should be connected. Valid
     *            entries are Required and Optional. (Optional) (default - "Optional"). args[6] - sStateRequired - Set "required" flag if target state should be present. Valid entries are Required and
     *            Optional. (Optional) (default - "Required")
     * @return 0 if all children are in a valid state. 1 if any child is in an invalid state.
     * @throws Exception
     *             if the operation fails
     * @since EC 10.6.SP2
     */
    public int checkRelatedObjectState(Context context, String[] args) throws Exception {

        String strOutput = "";
        int intOutput = 0;

        try {

            // TIGTK-17240 : START
            String isPromoteFromCOAction = context.getCustomData("isPromoteFromCOAction");
            if (UIUtil.isNotNullAndNotEmpty(isPromoteFromCOAction) && isPromoteFromCOAction.equalsIgnoreCase("true")) {
                return intOutput;
            }
            // TIGTK-17240 : END
            // Create an instant of emxUtil JPO
            emxUtil_mxJPO utilityClass = new emxUtil_mxJPO(context, null);

            // Get Required Environment Variables
            String arguments[] = new String[1];
            arguments[0] = "get env OBJECTID";

            ArrayList cmdResults = utilityClass.executeMQLCommands(context, arguments);

            String sObjectId = (String) cmdResults.get(0);
            StringBuffer sBuffer = new StringBuffer();
            String sRel = "";
            String sTargObject = "";

            String sRelationship = args[0];
            String sTargetObject = args[1];
            String sTargetStateProp = args[2];
            String sDirection = args[3];
            String sComparisonOperator = args[4];
            String sObjectRequired = args[5];
            String sStateRequired = args[6];

            // If no value for operator set it to EQ
            if ("".equals(sComparisonOperator)) {
                sComparisonOperator = "EQ";
            }

            // If value for Object Required in not Required set it to Optional
            if (!"required".equalsIgnoreCase(sObjectRequired)) {
                sObjectRequired = "Optional";
            }

            // If value for State Required in not Required set it to Optional
            if (!"optional".equalsIgnoreCase(sStateRequired)) {
                sStateRequired = "Required";
            }

            StringTokenizer strToken = new StringTokenizer(sRelationship, " ,~");
            String strRel = "";
            String strRelRealName = "";
            while (strToken.hasMoreTokens()) {
                strRel = strToken.nextToken().trim();
                strRelRealName = PropertyUtil.getSchemaProperty(context, strRel);

                if ("".equals(strRelRealName)) {
                    // Error out if not registered
                    arguments = new String[5];
                    arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState_if.InvalidRel";
                    arguments[1] = "1";
                    arguments[2] = "Rel";
                    arguments[3] = strRel;
                    arguments[4] = "";

                    strOutput = emxMailUtil_mxJPO.getMessage(context, arguments);
                    intOutput = 1;
                    break;
                } else {
                    if (sBuffer.length() > 0) {
                        sBuffer.append(',');
                    }
                    sBuffer.append(strRelRealName);
                }
            }

            if (sBuffer.length() > 0) {
                sRel = sBuffer.toString();
            } else {
                // Set Relationship to * if one is not entered
                sRel = "*";
            }

            if (intOutput == 0) {
                sBuffer = new StringBuffer();
                strToken = new StringTokenizer(sTargetObject, " ,~");
                String sTypeResult = "";
                String sTypeRealName = "";
                while (strToken.hasMoreTokens()) {
                    sTypeResult = strToken.nextToken().trim();
                    sTypeRealName = PropertyUtil.getSchemaProperty(context, sTypeResult);
                    if ("".equals(sTypeRealName)) {
                        // Error out if not registered
                        arguments = new String[5];
                        arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState_if.InvalidType";
                        arguments[1] = "1";
                        arguments[2] = "Type";
                        arguments[3] = sTypeResult;
                        arguments[4] = "";

                        strOutput = emxMailUtil_mxJPO.getMessage(context, arguments);
                        intOutput = 1;
                        break;
                    } else {
                        if (sBuffer.length() > 0) {
                            sBuffer.append(',');
                        }
                        sBuffer.append(sTypeRealName);
                    }
                }

                if (sBuffer.length() > 0) {
                    sTargObject = sBuffer.toString();
                } else {
                    // Set Target Object to * if one is not entered
                    sTargObject = "*";
                }
            }

            if (intOutput == 0) {
                String sTargetState = "";

                DomainObject domObj = new DomainObject(sObjectId);
                boolean bParentState = true;
                // If no Target state is defined use current state of object
                if (sTargetStateProp == null || "".equals(sTargetStateProp)) {
                    sTargetState = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
                    bParentState = false;
                }

                // prepare getRelatedObjects parameters
                boolean getToRelationships = true;
                boolean getFromRelationships = true;

                String whereClause = DomainConstants.EMPTY_STRING;
                StringList strListObj = new StringList(6);
                strListObj.add(DomainConstants.SELECT_ID);
                strListObj.add(DomainConstants.SELECT_TYPE);
                strListObj.add(DomainConstants.SELECT_NAME);
                strListObj.add(DomainConstants.SELECT_REVISION);
                strListObj.add(DomainConstants.SELECT_CURRENT);
                strListObj.add(DomainConstants.SELECT_POLICY);

                MapList mapList = new MapList();

                if ("to".equalsIgnoreCase(sDirection)) {
                    mapList = domObj.getRelatedObjects(context, sRel, sTargObject, strListObj, null, getToRelationships, // getTo relationships
                            false, // getFrom relationships
                            (short) 1, whereClause, "", 0);
                } else if ("from".equalsIgnoreCase(sDirection)) {
                    mapList = domObj.getRelatedObjects(context, sRel, sTargObject, strListObj, null, false, // getTo relationships
                            getFromRelationships, // getFrom relationships
                            (short) 1, whereClause, "", 0);
                }

                int size = 0;

                if (mapList != null && (size = mapList.size()) > 0) {
                    // Create a list of all matching objects and check their state
                    Map map = null;
                    String sChildID = "";
                    String sChildType = "";
                    String sChildName = "";
                    String sChildRev = "";
                    String sChildCurrent = "";
                    String sChildPolicy = "";

                    StringList strListChildStates = null;
                    String languageStr = context.getSession().getLanguage();
                    String sDvlpPartPolicy = DomainConstants.POLICY_DEVELOPMENT_PART;
                    for (int i = 0; i < size; i++) {
                        map = (Map) mapList.get(i);
                        sChildID = (String) map.get(DomainConstants.SELECT_ID);
                        sChildType = (String) map.get(DomainConstants.SELECT_TYPE);
                        sChildName = (String) map.get(DomainConstants.SELECT_NAME);
                        sChildRev = (String) map.get(DomainConstants.SELECT_REVISION);
                        sChildCurrent = (String) map.get(DomainConstants.SELECT_CURRENT);
                        sChildPolicy = (String) map.get(DomainConstants.SELECT_POLICY);

                        sChildType = i18nNow.getAdminI18NString("Type", sChildType, languageStr);

                        /*
                         * If a dvlp part, then we need to equate "Approved" (Common Part State) to "Complete" (Dvlp part state) and "Review" (Common Part State) to "Peer Review" (Dvlp Part State)
                         */

                        if (sChildPolicy.equals(sDvlpPartPolicy)) {
                            if ("state_Approved".equals(sTargetStateProp)) {
                                sTargetStateProp = "state_Complete";
                            } else if ("state_Review".equals(sTargetStateProp)) {
                                sTargetStateProp = "state_PeerReview";
                            }
                        }

                        if (bParentState) {
                            sTargetState = PropertyUtil.getSchemaProperty(context, "policy", sChildPolicy, sTargetStateProp);
                            if (sTargetState == null || "".equals(sTargetState)) {
                                if ("required".equalsIgnoreCase(sStateRequired)) {
                                    // Error out if not registered
                                    arguments = new String[7];
                                    arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.InvalidState";
                                    arguments[1] = "2";
                                    arguments[2] = "State";
                                    arguments[3] = sTargetStateProp;
                                    arguments[4] = "Policy";
                                    arguments[5] = sChildPolicy;
                                    arguments[6] = "";

                                    intOutput = 1;
                                    strOutput = strOutput + emxMailUtil_mxJPO.getMessage(context, arguments);
                                    break;
                                } else {
                                    continue;
                                }
                            }
                        }

                        // Get all states for object
                        domObj = new DomainObject(sChildID);
                        strListChildStates = domObj.getInfoList(context, DomainConstants.SELECT_STATES);
                        int indexTargetState = strListChildStates.indexOf(sTargetState);

                        // TIGTK-12635 : VB: Start
                        String strPolicytoken = sChildPolicy.replace(' ', '_');
                        String strTargetStatetoken = sTargetState.replace(' ', '_');
                        StringBuffer strKeyValue = new StringBuffer("emxFramework.State.");
                        strKeyValue.append(strPolicytoken);
                        strKeyValue.append(".");
                        strKeyValue.append(strTargetStatetoken);

                        String stsTargetPropertyValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), strKeyValue.toString());
                        if (sTargetState.equals(TigerConstants.STATE_PSS_DEVELOPMENTPART_PEERREVIEW) || sTargetState.equals(TigerConstants.STATE_PART_REVIEW)) {
                            sTargetState = stsTargetPropertyValue;
                        } else if (sTargetState.equals(TigerConstants.STATE_PSS_DEVELOPMENTPART_COMPLETE) || sTargetState.equals(TigerConstants.STATE_PART_RELEASE)) {
                            sTargetState = stsTargetPropertyValue;
                        } else if (sTargetState.equals(TigerConstants.STATE_PSS_DEVELOPMENTPART_CREATE) || sTargetState.equals(TigerConstants.STATE_PSS_ECPART_PRELIMINARY)) {
                            sTargetState = stsTargetPropertyValue;
                        }
                        // TIGTK-12635 : VB: End

                        // check if target state is in object's policy
                        if (indexTargetState < 0) {
                            if ("required".equalsIgnoreCase(sStateRequired)) {
                                arguments = new String[13];
                                arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.InvalidTargetState";
                                arguments[1] = "5";
                                arguments[2] = "Type";
                                arguments[3] = sChildType;
                                arguments[4] = "Name";
                                arguments[5] = sChildName;
                                arguments[6] = "Rev";
                                arguments[7] = sChildRev;
                                arguments[8] = "Policy";
                                arguments[9] = sChildPolicy;
                                arguments[10] = "State";
                                arguments[11] = sTargetState;
                                arguments[12] = "";

                                intOutput = 1;
                                strOutput = strOutput + emxMailUtil_mxJPO.getMessage(context, arguments);
                            }

                            continue;
                        }

                        // Get index location for object
                        int index = strListChildStates.indexOf(sChildCurrent);

                        // TIGTK-12635 : VB: Start
                        String strChildStatetoken = sChildCurrent.replace(' ', '_');
                        StringBuffer strChildKeyValue = new StringBuffer("emxFramework.State.");
                        strChildKeyValue.append(strPolicytoken);
                        strChildKeyValue.append(".");
                        strChildKeyValue.append(strChildStatetoken);
                        String strChildPropertyValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), strChildKeyValue.toString());

                        if (sChildCurrent.equals(TigerConstants.STATE_PSS_DEVELOPMENTPART_CREATE) || sChildCurrent.equals(TigerConstants.STATE_PSS_ECPART_PRELIMINARY)) {
                            sChildCurrent = strChildPropertyValue;
                        } else if (sChildCurrent.equals(TigerConstants.STATE_PSS_DEVELOPMENTPART_PEERREVIEW) || sChildCurrent.equals(TigerConstants.STATE_PART_REVIEW)) {
                            sChildCurrent = strChildPropertyValue;
                        } else if (sChildCurrent.equals(TigerConstants.STATE_PSS_DEVELOPMENTPART_COMPLETE) || sChildCurrent.equals(TigerConstants.STATE_PART_RELEASE)) {
                            sChildCurrent = strChildPropertyValue;
                        }
                        // TIGTK-12635 : VB: End

                        // Check Target State index with object index location
                        if ("LT".equals(sComparisonOperator)) {
                            if (index >= indexTargetState) {
                                arguments = new String[13];
                                // TIGTK-14374 : 01-06-2018 : START
                                if (domObj.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING) || domObj.isKindOf(context, DomainConstants.TYPE_CAD_MODEL)) {
                                    arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.EqualOrAfterForCAD";
                                } else {
                                    arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.EqualOrAfter";
                                }
                                // TIGTK-14374 : 01-06-2018 : END

                                arguments[1] = "5";
                                arguments[2] = "Type";
                                arguments[3] = sChildType;
                                arguments[4] = "Name";
                                arguments[5] = sChildName;
                                arguments[6] = "Rev";
                                arguments[7] = sChildRev;
                                arguments[8] = "State";
                                arguments[9] = sChildCurrent;
                                arguments[10] = "TargetState";
                                arguments[11] = sTargetState;
                                arguments[12] = "";

                                intOutput = 1;
                                strOutput = strOutput + emxMailUtil_mxJPO.getMessage(context, arguments);
                            }
                        } else if ("GT".equals(sComparisonOperator)) {
                            if (index <= indexTargetState) {
                                arguments = new String[13];
                                arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.EqualOrBefore";
                                arguments[1] = "5";
                                arguments[2] = "Type";
                                arguments[3] = sChildType;
                                arguments[4] = "Name";
                                arguments[5] = sChildName;
                                arguments[6] = "Rev";
                                arguments[7] = sChildRev;
                                arguments[8] = "State";
                                arguments[9] = sChildCurrent;
                                arguments[10] = "TargetState";
                                arguments[11] = sTargetState;
                                arguments[12] = "";

                                intOutput = 1;
                                strOutput = strOutput + emxMailUtil_mxJPO.getMessage(context, arguments);
                            }
                        } else if ("EQ".equals(sComparisonOperator)) {
                            if (index != indexTargetState) {
                                arguments = new String[11];
                                arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.NotIn";
                                arguments[1] = "4";
                                arguments[2] = "Type";
                                arguments[3] = sChildType;
                                arguments[4] = "Name";
                                arguments[5] = sChildName;
                                arguments[6] = "Rev";
                                arguments[7] = sChildRev;
                                arguments[8] = "State";
                                arguments[9] = sTargetState;
                                arguments[10] = "";

                                intOutput = 1;
                                strOutput = strOutput + emxMailUtil_mxJPO.getMessage(context, arguments);
                            }
                        } else if ("LE".equals(sComparisonOperator)) {
                            if (index > indexTargetState) {
                                arguments = new String[13];
                                arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.After";
                                arguments[1] = "5";
                                arguments[2] = "Type";
                                arguments[3] = sChildType;
                                arguments[4] = "Name";
                                arguments[5] = sChildName;
                                arguments[6] = "Rev";
                                arguments[7] = sChildRev;
                                arguments[8] = "State";
                                arguments[9] = sChildCurrent;
                                arguments[10] = "TargetState";
                                arguments[11] = sTargetState;
                                arguments[12] = "";

                                intOutput = 1;
                                strOutput = strOutput + emxMailUtil_mxJPO.getMessage(context, arguments);
                            }
                        } else if ("GE".equals(sComparisonOperator)) {
                            if (index < indexTargetState) {
                                arguments = new String[13];
                                arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.Before";
                                arguments[1] = "5";
                                arguments[2] = "Type";
                                arguments[3] = sChildType;
                                arguments[4] = "Name";
                                arguments[5] = sChildName;
                                arguments[6] = "Rev";
                                arguments[7] = sChildRev;
                                arguments[8] = "State";
                                arguments[9] = sChildCurrent;
                                arguments[10] = "TargetState";
                                arguments[11] = sTargetState;
                                arguments[12] = "";

                                intOutput = 1;
                                strOutput = strOutput + emxMailUtil_mxJPO.getMessage(context, arguments);
                            }
                        } else if ("NE".equals(sComparisonOperator)) {
                            if (index == indexTargetState) {
                                arguments = new String[11];
                                arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.Equal";
                                arguments[1] = "4";
                                arguments[2] = "Type";
                                arguments[3] = sChildType;
                                arguments[4] = "Name";
                                arguments[5] = sChildName;
                                arguments[6] = "Rev";
                                arguments[7] = sChildRev;
                                arguments[8] = "State";
                                arguments[9] = sChildCurrent;
                                arguments[10] = "";

                                intOutput = 1;
                                strOutput = strOutput + emxMailUtil_mxJPO.getMessage(context, arguments);
                            }
                        } else {
                            arguments = new String[5];
                            arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.InvalidOperator";
                            arguments[1] = "1";
                            arguments[2] = "Operation";
                            arguments[3] = sComparisonOperator;
                            arguments[4] = "";

                            intOutput = 1;
                            strOutput = strOutput + emxMailUtil_mxJPO.getMessage(context, arguments);
                            break;
                        }
                    }

                } else if ("required".equalsIgnoreCase(sObjectRequired)) {
                    arguments = new String[7];
                    arguments[0] = "emxFramework.ProgramObject.eServicecommonCheckRelState.NoObject";
                    arguments[1] = "2";
                    arguments[2] = "Rel";
                    arguments[3] = sRel;
                    arguments[4] = "Object";
                    arguments[5] = sTargObject;
                    arguments[6] = "";

                    intOutput = 1;
                    strOutput = emxMailUtil_mxJPO.getMessage(context, arguments);
                }
            }

            if (intOutput != 0) {
                emxContextUtil_mxJPO.mqlNotice(context, strOutput);
            }
        } catch (Exception e) {
            logger.error("Error in checkRelatedObjectState: ", e);
        }
        return intOutput;
    }

}
