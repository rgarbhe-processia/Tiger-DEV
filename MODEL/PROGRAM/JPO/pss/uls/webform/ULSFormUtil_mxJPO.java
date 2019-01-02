package pss.uls.webform;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class ULSFormUtil_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ULSFormUtil_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    /**
     * Range Function for attribute PSS_ProgramProject Assign Ranges to the child according to parent's type
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    // ULS: Modified for TIGTK-5250 | 3/24/17 PTE
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map getValidProgramOrProjectRange(Context context, String args[]) throws Exception {

        final String PROGRAM = "Program";
        final String PROJECT = "Project";

        Map mRangeValues = new HashMap();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");
            String objectId = (String) requestMap.get("objectId");
            String openerFrame = (String) requestMap.get("openerFrame");
            String languageStr = (String) requestMap.get("languageStr");
            StringList slRangeList = FrameworkUtil.getRanges(context, TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT);
            StringList slFieldChoiceList = new StringList();
            StringList slFieldDisplayChoiceList = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(objectId)) {
                DomainObject domainObj = DomainObject.newInstance(context, objectId);
                String programORproject = domainObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT);

                StringList slObjSelectStmts = getULSRelationshipConstants();

                StringList slSelectRelStmts = new StringList(1);
                slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                Map mParentMap = domainObj.getRelatedObject(context, TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT, false, slObjSelectStmts, slSelectRelStmts);

                String programORprojectParent = null;
                String programORprojectParentLevelTwo = null;
                boolean isCreate = false;
                if (UIUtil.isNotNullAndNotEmpty(openerFrame)) {
                    isCreate = true;
                }
                if (mParentMap != null && !mParentMap.isEmpty()) {

                    String strObjId = (String) mParentMap.get(DomainConstants.SELECT_ID);
                    DomainObject domainObjParent = DomainObject.newInstance(context, strObjId);
                    programORprojectParent = domainObjParent.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT);

                    if (isCreate) {
                        programORprojectParentLevelTwo = programORprojectParent;
                        programORprojectParent = programORproject;
                    } else {

                        Map mParentMapLevelTwo = domainObjParent.getRelatedObject(context, TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT, false, slObjSelectStmts, slSelectRelStmts);
                        if (mParentMapLevelTwo != null && !mParentMapLevelTwo.isEmpty()) {
                            strObjId = (String) mParentMapLevelTwo.get(DomainConstants.SELECT_ID);
                            DomainObject domainObjParentLevelTwo = DomainObject.newInstance(context, strObjId);
                            programORprojectParentLevelTwo = domainObjParentLevelTwo.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT);
                        }
                    }
                } else if (isCreate) {
                    programORprojectParent = programORproject;
                }

                for (int i = 0; i < slRangeList.size(); i++) {
                    String attrValue = (String) slRangeList.get(i);

                    if (UIUtil.isNullOrEmpty(programORprojectParent)) {

                        slFieldChoiceList.add(attrValue);

                    } else if (UIUtil.isNotNullAndNotEmpty(programORprojectParent) && UIUtil.isNullOrEmpty(programORprojectParentLevelTwo)) {

                        if (programORprojectParent.equals(PROGRAM)) {

                            slFieldChoiceList.add(attrValue);
                        } else if (attrValue.equals(PROJECT)) {

                            slFieldChoiceList.add(attrValue);
                            break;
                        }
                    } else if (UIUtil.isNotNullAndNotEmpty(programORprojectParent) && UIUtil.isNotNullAndNotEmpty(programORprojectParentLevelTwo)) {

                        if (programORprojectParent.equals(PROGRAM)) {
                            if (programORprojectParentLevelTwo.equals(PROGRAM) && attrValue.equals(PROJECT)) {
                                slFieldChoiceList.add(attrValue);
                                break;
                            } else if (!programORprojectParentLevelTwo.equals(PROGRAM)) {
                                slFieldChoiceList.add(attrValue);
                            }
                        } else if (attrValue.equals(PROJECT)) {
                            slFieldChoiceList.add(attrValue);
                            break;
                        }
                    }

                }
            }
            /**
             * Ranges for Program-Project create in ULS
             */

            else {
                for (int i = 0; i < slRangeList.size(); i++) {
                    String attrValue = (String) slRangeList.get(i);
                    slFieldChoiceList.add(attrValue);
                }
            }

            for (int i = 0; i < slFieldChoiceList.size(); i++) {
                slFieldDisplayChoiceList.add(i18nNow.getRangeI18NString(TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT, (String) slFieldChoiceList.get(i), languageStr));
            }
            mRangeValues.put("field_choices", slFieldChoiceList);
            mRangeValues.put("field_display_choices", slFieldDisplayChoiceList);
            return mRangeValues;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getValidProgramOrProjectRange: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return mRangeValues;

    }

    /**
     * Update Function for connecting Route Template to Program-Project
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public void connectRTToProgProj(Context context, String[] args) throws Exception {

        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            HashMap fieldMap = (HashMap) programMap.get("fieldMap");
            HashMap settingsMap = (HashMap) fieldMap.get("settings");
            String strFromTemplate = (String) settingsMap.get("fromTemplate");
            String objectId = (String) paramMap.get("objectId");
            DomainObject domProgProj = new DomainObject(objectId);

            // Get all connected route template id of field PART APPROVAL MCO
            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_ROUTETEMPLATE);
            StringList slRelSelects = new StringList();
            slRelSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            slRelSelects.add("attribute[PSS_RouteTemplateType]");
            String strRelAttribute = "attribute[PSS_RouteTemplateType]";

            // TIGTK-3581 : START Modify:16/11/2016
            // PSS_RouteTemplateType attribute value of each field

            StringBuffer sbRelWhere = new StringBuffer();
            String strRouteTemplateType = getAppropriateAttrValue(strFromTemplate);
            if (!"".equals(strRouteTemplateType)) {
                sbRelWhere.append(strRelAttribute);
                sbRelWhere.append(" == '");
                sbRelWhere.append(strRouteTemplateType);
                sbRelWhere.append("'");

            }
            // TIGTK-3581 : END

            MapList mlRouteTemplate = domProgProj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    new StringList(DomainObject.SELECT_ID), // object selects
                    slRelSelects, // relationship selects
                    true, // to direction
                    true, // from direction;
                    (short) 1, // recursion level
                    null, // object where clause
                    sbRelWhere.toString(), (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null, null);

            if (mlRouteTemplate.size() > 0) {
                java.util.Iterator itrPartApprovalMCO = mlRouteTemplate.iterator();
                Map mPartApprovalMCO;
                while (itrPartApprovalMCO.hasNext()) {
                    mPartApprovalMCO = (Map) itrPartApprovalMCO.next();
                    String strConnectionId = (String) mPartApprovalMCO.get("id[connection]");
                    if (UIUtil.isNotNullAndNotEmpty(strConnectionId)) {
                        DomainRelationship.disconnect(context, strConnectionId);
                    }
                }
            }

            String strNewId = (String) paramMap.get("New Value");
            if (strNewId == null)
                strNewId = (String) paramMap.get("New OID");

            if (UIUtil.isNotNullAndNotEmpty(strNewId)) {
                DomainRelationship drTemplate = DomainRelationship.connect(context, domProgProj, TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES, new DomainObject(strNewId));
                drTemplate.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE, getAppropriateAttrValue(strFromTemplate));
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectRTToProgProj: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    /**
     * @param fromTemplate
     * @description this method will return the appropriate attribute value to be set on relationship PSS_CONNECTEDROUTETEMPLATE
     * @throws Exception
     */

    public String getAppropriateAttrValue(String fromTemplate) throws Exception {
        String strAttrValueToSet = DomainObject.EMPTY_STRING;

        if (fromTemplate.equals("ReviewTemplate")) {
            strAttrValueToSet = "Default Evaluation Review Route Template for CR";
        } else if (fromTemplate.equals("ImpactAnalysis")) {
            strAttrValueToSet = "Default Impact Analysis Route Template for CR";
        } else if (fromTemplate.equals("CommercialApprovalCO")) {
            strAttrValueToSet = "Approval List for Commercial update on CO";
        } else if (fromTemplate.equals("PrototypeApprovalCO")) {
            strAttrValueToSet = "Approval List for Prototype on CO";
        } else if (fromTemplate.equals("SerialApprovalCO")) {
            strAttrValueToSet = "Approval List for Serial Launch on CO";
        } else if (fromTemplate.equals("DesignApprovalCO")) {
            strAttrValueToSet = "Approval List for Design study on CO";
        } else if (fromTemplate.equals("CADApprovalCO")) {
            strAttrValueToSet = "Approval List for CAD on CO";
        } else if (fromTemplate.equals("PartApprovalCO")) {
            strAttrValueToSet = "Approval List for Other Parts on CO";
        } else if (fromTemplate.equals("StandardApprovalCO")) {
            strAttrValueToSet = "Approval List for Standard Parts on CO";
        } else if (fromTemplate.equals("CommercialApprovalMCO")) {
            strAttrValueToSet = "Approval List for Commercial update on MCO";
        } else if (fromTemplate.equals("PrototypeApprovalMCO")) {
            strAttrValueToSet = "Approval List for Prototype on MCO";
        } else if (fromTemplate.equals("SerialApprovalMCO")) {
            strAttrValueToSet = "Approval List for Serial Launch on MCO";
        } else if (fromTemplate.equals("DesignApprovalMCO")) {
            strAttrValueToSet = "Approval List for Design study on MCO";
        } else if (fromTemplate.equals("PartApprovalMCO")) {
            strAttrValueToSet = "Approval List for Other Parts on MCO";
        } else if (fromTemplate.equals("ReviewCN")) {
            strAttrValueToSet = "Default CN Reviewer on CN";
        }
        // TIGTK-11675 : 27/11/17 : TS : START
        else if (fromTemplate.equals("AcquisitionCO")) {
            strAttrValueToSet = "Approval List for Acquisition on CO";
        } else if (fromTemplate.equals("AcquisitionMCO")) {
            strAttrValueToSet = "Approval List for Acquisition on MCO";
        }

        // TIGTK-11675 : 27/11/17 : TS : END

        return strAttrValueToSet;
    }

    /**
     * To disconnect already connected Division, Platform of Program-Project and connect newly selected Division,Platform to Program-Project
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void updateConnectedPlatformOrDivision(Context context, String[] args) throws Exception {

        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");

            String strObjectId = (String) paramMap.get("objectId");
            DomainObject domProgProjObj = DomainObject.newInstance(context, strObjectId);

            String strRelPlatformId = domProgProjObj.getInfo(context, "to[PSS_ConnectedPlatform].id");
            String strRelDivisionId = domProgProjObj.getInfo(context, "from[PSS_ResponsibleDivision].id");
            String strOldId = (String) paramMap.get("Old OID");
            String strOldType = "";

            // Disconnect Platform and Division
            if (UIUtil.isNotNullAndNotEmpty(strOldId)) {
                DomainObject domOldObj = DomainObject.newInstance(context, strOldId);
                strOldType = domOldObj.getInfo(context, DomainConstants.SELECT_TYPE);
            }

            if (UIUtil.isNotNullAndNotEmpty(strOldId) && strOldType.equals(TigerConstants.TYPE_PSS_PLATFORM)) {
                DomainRelationship.disconnect(context, strRelPlatformId);
            } else if (UIUtil.isNotNullAndNotEmpty(strOldId) && strOldType.equals(TigerConstants.TYPE_PSS_DIVISION)) {
                DomainRelationship.disconnect(context, strRelDivisionId);
            }

            // Connect Platform and Division
            String strNewType = "";

            String strNewId = (String) paramMap.get("New OID");
            if (UIUtil.isNotNullAndNotEmpty(strNewId)) {
                DomainObject domNewObj = DomainObject.newInstance(context, strNewId);
                strNewType = domNewObj.getInfo(context, DomainConstants.SELECT_TYPE);
            }

            if (UIUtil.isNotNullAndNotEmpty(strNewType)) {
                if (UIUtil.isNotNullAndNotEmpty(strNewId) && strNewType.equals(TigerConstants.TYPE_PSS_PLATFORM)) {
                    DomainObject platformObj = DomainObject.newInstance(context, strNewId);
                    DomainRelationship.connect(context, platformObj, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM, domProgProjObj);
                }

                else if (UIUtil.isNotNullAndNotEmpty(strNewId) && strNewType.equals(TigerConstants.TYPE_PSS_DIVISION)) {
                    DomainObject divisionObj = DomainObject.newInstance(context, strNewId);
                    DomainRelationship.connect(context, domProgProjObj, TigerConstants.RELATIONSHIP_PSS_RESPONSIBLEDIVISION, divisionObj);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in updateConnectedPlatformOrDivision: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

    }

    /**
     * Post Process for create Program Project Connect the Devision and Platform to the recently created Program Project.
     * @param context
     * @param args
     * @throws Exception
     */
    public void createProgramProjectPostProcess(Context context, String[] args) throws Exception {

        try {
            // get object details from args
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");
            HashMap<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");
            // get object id from param map
            String strObjectId = (String) paramMap.get("objectId");
            DomainObject domProgProjObj = DomainObject.newInstance(context, strObjectId);

            // get Platform Id and Division Id from request map
            String strPlatformId = (String) requestMap.get("PlatformOID");
            String strDivisionId = (String) requestMap.get("DivisionOID");

            // connect division to Program Project
            if (UIUtil.isNotNullAndNotEmpty(strDivisionId)) {
                DomainObject domDivisionObj = DomainObject.newInstance(context, strDivisionId);
                DomainRelationship.connect(context, domProgProjObj, TigerConstants.RELATIONSHIP_PSS_RESPONSIBLEDIVISION, domDivisionObj);
            }

            // connect platform to Program Project
            if (UIUtil.isNotNullAndNotEmpty(strPlatformId)) {
                DomainObject domPlatformObj = DomainObject.newInstance(context, strPlatformId);
                DomainRelationship.connect(context, domPlatformObj, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM, domProgProjObj);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in createProgramProjectPostProcess: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    /**
     * This Method is for getting Common data for all tables
     * @param context
     * @param args
     * @throws Exception
     */

    public StringList getULSRelationshipConstants() throws Exception {
        try {
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            slObjSelectStmts.addElement(DomainConstants.SELECT_CURRENT);
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            slObjSelectStmts.addElement(DomainConstants.SELECT_DESCRIPTION);
            slObjSelectStmts.addElement(DomainConstants.SELECT_TYPE);
            return slObjSelectStmts;
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getULSRelationshipConstants: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }

    }

    /**
     * This method is used as Include OID program for Route Template search in Program Project. It fetches the Route Template based on the AutoStopOnRejection attribute value
     * @param context
     * @param args
     * @throws Exception
     * @return StringList
     */
    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList includeImmediateOrDeferredRouteTemplate(Context context, String args[]) throws Exception {
        StringList slRouteTemplateList = new StringList();
        final String ATTRIBUTE_AUTO_STOP_ON_REJECTION = PropertyUtil.getSchemaProperty(context, "attribute_AutoStopOnRejection");
        final String SELECT_ATTRIBUTE_AUTO_STOP_REJECTION = "attribute[" + ATTRIBUTE_AUTO_STOP_ON_REJECTION + "]";
        try {
            String strWhereClause = "";
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strFieldName = (String) programMap.get("fieldNameActual");
            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_ID);
            slObjectSelect.add(SELECT_ATTRIBUTE_AUTO_STOP_REJECTION);
            if (strFieldName.equals("PSS_ImpactTemplate")) {
                strWhereClause = SELECT_ATTRIBUTE_AUTO_STOP_REJECTION + "==Deferred";
            } else {
                strWhereClause = SELECT_ATTRIBUTE_AUTO_STOP_REJECTION + "==Immediate";
            }
            MapList mlRouteTemplates = DomainObject.findObjects(context, DomainConstants.TYPE_ROUTE_TEMPLATE, TigerConstants.VAULT_ESERVICEPRODUCTION, strWhereClause, slObjectSelect);
            for (int itr = 0; itr < mlRouteTemplates.size(); itr++) {
                Map mRouteTemplate = (Map) mlRouteTemplates.get(itr);
                slRouteTemplateList.add((String) mRouteTemplate.get(DomainConstants.SELECT_ID));
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in includeImmediateOrDeferredRouteTemplate: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }
        return slRouteTemplateList;
    }

    // PCM TIGTK-1961 | 01/12/16 :Pooja Mantri : Start
    /**
     * Displays the Range Values for attribute PSS_Role on PSS_MemberInfoTable.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds a HashMap containing the following entries:
     * @param HashMap
     *            containing the following keys, "objectId"
     * @return HashMap contains actual and display values
     * @throws Exception
     *             if operation fails
     */
    public HashMap displayPSSRoleValues(Context context, String[] args) throws Exception {
        HashMap rangeMap = new HashMap();
        try {
            Locale strLocale = new Locale(context.getSession().getLanguage());
            String strMQLCommand = "list role PSS*";
            
            // TIGTK-12983 - ssamel : START
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map mpParamMap = (Map) programMap.get("paramMap");

            String sPPId = (String) mpParamMap.get("objectId");
            DomainObject doPPObject = DomainObject.newInstance(context, sPPId); // Domain Object instantiated with the ParentOID
            String sPPOwnership = doPPObject.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_OWNERSHIP + "].value");

            if(UIUtil.isNotNullAndNotEmpty(sPPOwnership) && TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equals(sPPOwnership))
            {
                strMQLCommand = "list role PSS*_JV";
            }
            //String strMQLCommand = "list role PSS*";
            // TIGTK-12983 - ssamel : END
            
            StringList slRoleFieldChoices = new StringList();
            // Modify for TIGTK-5890 by PTE 3/31/2017 Starts
            StringList slRoleDisplayValue = new StringList();
            // Modify for TIGTK-5890 by PTE 3/31/2017 END
			// TIGTK-18252 : stembulkar : start
			String sContextRole = context.getRole();
			// TIGTK-18252 : stembulkar : end
            StringList slPSSRoleList = FrameworkUtil.split(MqlUtil.mqlCommand(context, true, strMQLCommand, true), System.getProperty("line.separator"));
			// TIGTK-18252 : stembulkar : start
			slPSSRoleList.remove( TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR );
			slPSSRoleList.remove( TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM );
			// TIGTK-18252 : stembulkar : end
            int intPSSRoleListSize = slPSSRoleList.size();

            for (int iCnt = 0; iCnt < intPSSRoleListSize; iCnt++) {
                String strRoleName = (String) slPSSRoleList.get(iCnt);
                if (UIUtil.isNotNullAndNotEmpty(strRoleName)) {
                    // Modify for TIGTK-5890 by PTE 3/31/2017
                    String strKey = "emxFramework.Role." + strRoleName;
                    String strPSSRoleValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", strLocale, strKey);
                    slRoleDisplayValue.addElement(strPSSRoleValue.trim());
                    slRoleFieldChoices.addElement(strRoleName.trim());
                    // Modify for TIGTK-5890 by PTE 3/31/2017 END
                }
            }
            // Modify for TIGTK-5890 by PTE 3/31/2017
            rangeMap.put("field_choices", slRoleFieldChoices);
            rangeMap.put("field_display_choices", slRoleDisplayValue);
            // Modify for TIGTK-5890 by PTE 3/31/2017 END
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in displayPSSRoleValues: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return rangeMap;
    }
    // PCM TIGTK-1961 | 01/12/16 :Pooja Mantri : End

    public Map getComplexityRanges(Context context, String args[]) throws Exception {
        Map mRangeValues = new HashMap();
        try {
            Locale strLocale = new Locale(context.getSession().getLanguage());
            String strKey = "emxFramework.RangeSequence.PSS_Complexity";
            String strPSSRangeSequence = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", strLocale, strKey);

            StringList slValidRangeSequence = FrameworkUtil.split(strPSSRangeSequence, ",");
            mRangeValues.put("field_choices", slValidRangeSequence);
            mRangeValues.put("field_display_choices", slValidRangeSequence);
        } catch (Exception e) {
            logger.error("Error in getComplexityRanges: ", e);
        }
        return mRangeValues;
    }
    
    //TIGTK-12983 - ssamel : START
    /**
     * Validate the Project Role for attribute PSS_Role on PSS_MemberInfoTable on JV Program-Project.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds a HashMap containing the following entries:
     * @param HashMap
     *            containing the keys as "objectId", "New Value"
     * @return int contains 0 or 1 for process continuation or discontinue
     * @throws Exception
     *             if operation fails
     */
    public int checkAndUpdatePSSRoleValues(Context context, String args[]) throws Exception {
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        
        HashMap<String, String> paramMap = (HashMap<String, String>) programMap.get("paramMap");
        String sObjectId = (String) paramMap.get("objectId");

        DomainObject doPerson = DomainObject.newInstance(context, sObjectId);
        
        StringList slSelect = new StringList(2);
        slSelect.addElement(DomainConstants.SELECT_NAME);
        slSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_USERTYPE + "].value");
        
        Map mpPersonDetails = doPerson.getInfo(context, slSelect);
        String sUserType = (String) mpPersonDetails.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_USERTYPE + "].value");
        String sRoleValue = (String) paramMap.get("New Value");
        if(TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_FAURECIA.equals(sUserType))
        {
            if(UIUtil.isNotNullAndNotEmpty(sRoleValue) && !(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV.equals(sRoleValue) || 
                    TigerConstants.ROLE_PSS_CHANGE_COORDINATOR_JV.equals(sRoleValue)))
            {
                String sUserName = (String) mpPersonDetails.get(DomainConstants.SELECT_NAME);
                Locale sLocale = new Locale(context.getSession().getLanguage());
                String sAlert = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", sLocale, "PSS_EnterpriseChangeMgt.Alert.ProgramProjectRoles");
                StringBuffer sbError = new StringBuffer(sUserName);
                sbError.append(" ");
                sbError.append(sAlert);
                sbError.append(" ");
                
                String sKey = "emxFramework.Role." + sRoleValue;
                String sRoleDisplayValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", sLocale, sKey);
                
                sbError.append(sRoleDisplayValue);                
                MqlUtil.mqlCommand(context, "error $1", sbError.toString());
                return 1;
            }
        }
        
        String sMemberRelId = (String) paramMap.get("relId");
        DomainRelationship doMember = new DomainRelationship(sMemberRelId);
        doMember.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_ROLE, sRoleValue);
        return 0;
    }
    //TIGTK-12983 - ssamel : END
}