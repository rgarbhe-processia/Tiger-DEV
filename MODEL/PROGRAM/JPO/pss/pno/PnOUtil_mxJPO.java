package pss.pno;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADIntegrationSessionData;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.vplm.posbusinessmodel.PeopleConstants;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.User;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.common.utils.TigerUtils;
import pss.constants.TigerConstants;

public class PnOUtil_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PnOUtil_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    /**
     * Description : Include Object ID of CS which is Connected.
     * @author punamk
     * @args
     * @Date aug 24, 2016
     */
    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList includeProjectsForChangeOwner(Context context, String[] args) throws Exception {
        StringList listCSIDs = new StringList();
        String strProgramProjectId = "";
        String strProgramProjectName = "";
        String strProjectName = DomainConstants.EMPTY_STRING;
        String strCurrentOrganization = PersonUtil.getDefaultOrganization(context, context.getUser());
        String strWhereClause = "ownership.organization.ancestor =='" + strCurrentOrganization + "'";
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strParentOID = (String) programMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strParentOID)) {
                BusinessObject busObj = new BusinessObject(strParentOID);
                User usrProjectName = busObj.getProjectOwner(context);
                strProjectName = usrProjectName.toString();
            }

            String userName = context.getUser();
            String userID = PersonUtil.getPersonObjectID(context, userName);
            DomainObject domUserOID = new DomainObject(userID);
            MapList listCSId = null;
            // TIGTK-7606:rutuja Ekatpure:5/5/2017:start
            String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, userName);
            String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);
            // TIGTK-7606:rutuja Ekatpure:5/5/2017:End
            if (assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                StringList slObjectSle = new StringList(1);
                slObjectSle.addElement(DomainConstants.SELECT_ID);
                slObjectSle.addElement(DomainConstants.SELECT_NAME);
                listCSId = (MapList) DomainObject.findObjects(context, DomainConstants.TYPE_PNOPROJECT, TigerConstants.VAULT_ESERVICEPRODUCTION, strWhereClause, slObjectSle);
                if (listCSId.size() != 0) {
                    for (int j = 0; j < listCSId.size(); j++) {
                        Map<?, ?> mapProgramProject = (Map<?, ?>) listCSId.get(j);
                        strProgramProjectId = (String) mapProgramProject.get(DomainConstants.SELECT_ID);
                        strProgramProjectName = (String) mapProgramProject.get(DomainConstants.SELECT_NAME);
                        if (!strProgramProjectName.equals(strProjectName)) {
                            listCSIDs.add(strProgramProjectId);
                        }
                    }
                }

            } else {

                StringList slObjSelectStmts = new StringList();
                slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
                slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);

                StringList slRelSelectStmts = new StringList(1);
                slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                Pattern typePattern = new Pattern(DomainConstants.TYPE_PNOPROJECT);
                typePattern.addPattern(DomainConstants.TYPE_SECURITYCONTEXT);

                Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_ASSIGNED_SECURITY_CONTEXT);
                relPattern.addPattern(DomainConstants.RELATIONSHIP_SECURITYCONTEXT_PROJECT);

                Pattern postTypePattern = new Pattern(DomainConstants.TYPE_PNOPROJECT);

                listCSId = domUserOID.getRelatedObjects(context, relPattern.getPattern(), // Relationship
                        // Pattern
                        typePattern.getPattern(), // Object Pattern
                        slObjSelectStmts, // Object Selects
                        slRelSelectStmts, // Relationship Selects
                        false, // to direction
                        true, // from direction
                        (short) 2, // recursion level
                        strWhereClause, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        postTypePattern, // Post Type Pattern
                        null, null, null);

                if (listCSId.size() != 0) {
                    for (int j = 0; j < listCSId.size(); j++) {
                        Map<?, ?> mapProgramProject = (Map<?, ?>) listCSId.get(j);
                        strProgramProjectId = (String) mapProgramProject.get(DomainConstants.SELECT_ID);
                        strProgramProjectName = (String) mapProgramProject.get(DomainConstants.SELECT_NAME);

                        if (!strProgramProjectName.equals(strProjectName)) {
                            listCSIDs.add(strProgramProjectId);
                        }

                    }
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in includeProjectsForChangeOwner: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        // TIGTK-6721:Rutuja Ekatpure:17/5/2017:Start
        if (listCSIDs.isEmpty()) {
            listCSIDs.add(" ");
        }
        // TIGTK-6721:Rutuja Ekatpure:17/5/2017:End
        return listCSIDs;

    }

    /**
     * Method For Change Ownership from search result
     * @param context
     * @param args
     * @return void
     * @throws Exception
     * @Modified on : 26-07-2017
     * @Modified By: psalunke
     * @Modified for : TIGTK-9206
     */
    public void postProcessForMassChangeOwnership(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String[] emxTableRowIds = (String[]) programMap.get("emxTableRowId");
            StringList OIDList = (StringList) programMap.get("selectedIds");
            MCADIntegrationSessionData integSessionData = (MCADIntegrationSessionData) programMap.get("integSessionData");
            String projectId = "";
            for (int i = 0; i < emxTableRowIds.length; i++) {
                StringList strlObjectIdList = (StringList) FrameworkUtil.split(((String) emxTableRowIds[i]), "|");
                projectId = ((String) strlObjectIdList.get(0));

            }
            DomainObject domProjectObject = new DomainObject(projectId);
            String strProjectName = domProjectObject.getInfo(context, DomainObject.SELECT_NAME);
            int intSelectedItemListSize = OIDList.size();
            boolean isRPESet = false;
            for (int intIndex = 0; intIndex < intSelectedItemListSize; intIndex++) {
                String strSelectedItem = (String) OIDList.get(intIndex);
                String strSelectedId = strSelectedItem.split(",")[0];
                DomainObject domSourceObject = new DomainObject(strSelectedId);

                // Apply change ownership on CAD minor objects as well.
                try {
                    String strVersionObjectId = "";

                    if (domSourceObject.isKindOf(context, DomainConstants.TYPE_CAD_MODEL) || domSourceObject.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {

                        String localeLanguage = integSessionData.getLanguageName();

                        StringList selectStmts = new StringList(1);
                        selectStmts.addElement(DomainConstants.SELECT_ID);
                        selectStmts.addElement(DomainConstants.SELECT_TYPE);
                        StringList relStmts = new StringList(1);
                        relStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                        Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_VIEWABLE);
                        relPattern.addPattern(TigerConstants.RELATIONSHIP_ACTIVEVERSION);
                        relPattern.addPattern(TigerConstants.RELATIONSHIP_LATESTVERSION);
                        relPattern.addPattern(TigerConstants.RELATIONSHIP_IMAGEHOLDER);
                        relPattern.addPattern(TigerConstants.RELATIONSHIP_VERSIONOF);
                        relPattern.addPattern(TigerConstants.RELATIONSHIP_DERIVEDOUTPUT);

                        MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle(""), new IEFGlobalCache());
                        String[] arrMajors = new String[1];
                        arrMajors[0] = strSelectedId;

                        String[] allMinors = mxUtil.getAllVersionObjects(context, arrMajors, false);

                        for (int minorKey = 0; minorKey < allMinors.length; minorKey++) {
                            String strMinorObjId = (String) allMinors[minorKey];
                            DomainObject domMinorObject = DomainObject.newInstance(context, strMinorObjId);

                            MapList mlobjMapList = domMinorObject.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                                    "*", // object pattern
                                    selectStmts, // object selects
                                    relStmts, // relationship selects
                                    true, // to direction
                                    true, // from direction
                                    (short) 1, // recursion level
                                    null, // object where clause
                                    null, 0); // relationship where clause
                            if (mlobjMapList != null && !mlobjMapList.isEmpty()) {
                                for (int j = 0; j < mlobjMapList.size(); j++) {
                                    Map mapVersionObject = (Map) mlobjMapList.get(j);
                                    strVersionObjectId = (String) mapVersionObject.get(DomainConstants.SELECT_ID);
                                    BusinessObject busObj = new BusinessObject(strVersionObjectId);

                                    try {
                                        PropertyUtil.setRPEValue(context, "PSS_Update_Project_owner", "True", true);
                                        isRPESet = true;
                                        domMinorObject.open(context);
                                        busObj.open(context);

                                        domMinorObject.setProjectOwner(context, strProjectName);
                                        busObj.setProjectOwner(context, strProjectName);

                                        domMinorObject.update(context);
                                        domMinorObject.close(context);
                                        busObj.update(context);
                                        busObj.close(context);
                                        // Added for TIGTK-8606:PKH:End
                                    } catch (Exception e) {
                                        throw e;
                                    }
                                }
                            }
                        }
                    } else if (domSourceObject.isKindOf(context, TigerConstants.TYPE_PSS_DOCUMENT)) {
                        Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_ACTIVEVERSION);
                        MapList mlobjMapList = domSourceObject.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                                "*", // object pattern
                                new StringList(DomainConstants.SELECT_ID), // object selects
                                new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                                true, // to direction
                                true, // from direction
                                (short) 1, // recursion level
                                null, // object where clause
                                null, 0); // relationship where clause
                        if (mlobjMapList != null && !mlobjMapList.isEmpty()) {
                            for (int j = 0; j < mlobjMapList.size(); j++) {
                                Map mapVersionObject = (Map) mlobjMapList.get(j);
                                strVersionObjectId = (String) mapVersionObject.get(DomainConstants.SELECT_ID);
                                BusinessObject busObj = new BusinessObject(strVersionObjectId);
                                try {
                                    PropertyUtil.setRPEValue(context, "PSS_Update_Project_owner", "True", true);
                                    isRPESet = true;
                                    busObj.open(context);
                                    busObj.setProjectOwner(context, strProjectName);
                                    busObj.update(context);
                                    busObj.close(context);
                                } catch (Exception e) {
                                    throw e;
                                }
                            }
                        }
                    }
                    // apply change ownership on other objects
                    domSourceObject.open(context);
                    domSourceObject.setProjectOwner(context, strProjectName);
                    domSourceObject.update(context);
                    domSourceObject.close(context);
                } catch (Exception e) {
                    throw e;
                } finally {
                    if (isRPESet) {
                        PropertyUtil.setRPEValue(context, "PSS_Update_Project_owner", "False", true);
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in postProcessForMassChangeOwnership: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }

    }

    /**
     * Description : Filter on basis of FAS .
     * @author snehai
     * @args TIGTK-8593
     * @Date aug 11, 2017
     */
    public Map filterCollabSpaceBasedOnOwnership(Context context, String[] args) throws Exception {

        Map returnMap = new HashMap();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map paramMap = (Map) programMap.get("paramMap");

            String strCurrentOrganization = PersonUtil.getDefaultOrganization(context, context.getUser());

            String strMQLQuery = "temp query bus $1 $2 $3 where $4 select $5 dump $6";
            String mqlArgs[] = new String[6];
            mqlArgs[0] = PeopleConstants.TYPE_PNO_PROJECT;
            mqlArgs[1] = "*";
            mqlArgs[2] = "-";
            mqlArgs[3] = "ownership.organization.ancestor=='" + strCurrentOrganization + "'";
            mqlArgs[4] = "name";
            mqlArgs[5] = "|";

            String strResult = MqlUtil.mqlCommand(context, strMQLQuery, mqlArgs);
            StringList strList = FrameworkUtil.split(strResult, "|");

            Iterator<Map.Entry> it = paramMap.entrySet().iterator();
            int j = 0;
            while (it.hasNext()) {
                Entry e = it.next();
                Map value = (Map) e.getValue();

                String strPLMExternalId = (String) value.get("PLM_ExternalID");
                if (strList.contains(strPLMExternalId)) {
                    returnMap.put("project" + j, value);
                    j++;
                }
            }
        } catch (Exception e) {
            logger.error("Error in filterCollabSpaceBasedOnOwnership: ", e);
            throw e;
        }
        return returnMap;
    }

    /**
     * Description : Set Origanization on Ownership .
     * @author snehai
     * @args TIGTK-8593
     * @Date aug 11, 2017
     */
    public int addOwnership(Context context, String[] args) throws Exception {
        try {
            String strCollabSpaceId = args[0];
            String strCollabSpaceName = args[1];
            String strCurrentOrganization = PersonUtil.getDefaultOrganization(context, context.getUser());
            if (UIUtil.isNullOrEmpty(strCurrentOrganization) && context.getUser().equals(PropertyUtil.getSchemaProperty(context, "person_UserAgent"))) {
                String strLoginedPerson = PropertyUtil.getGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME);
                if (UIUtil.isNotNullAndNotEmpty(strLoginedPerson)) {
                    strCurrentOrganization = PersonUtil.getDefaultOrganization(context, strLoginedPerson);
                    ;
                }
            }

            MqlUtil.mqlCommand(context, "modify businessobject $1 organization $2 project $3 add ownership $4 $5 for $6",
                    new String[] { strCollabSpaceId, "", "", strCurrentOrganization, strCollabSpaceName, "P&O" });
        } catch (Exception e) {
            logger.error("Error in filterCollabSpaceBasedOnOwnership: ", e);
            throw e;
        }
        return 0;
    }

    /**
     * Methode to update the project(CS) of part TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @throws Exception
     */
    public void updateCollaborativeSpace(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: updateCollaborativeSpace ::::::::");
        boolean bSuperUser = false;
        try {
            HashMap hmArgumentMap = (HashMap) JPO.unpackArgs(args);
            HashMap hmParamMap = (HashMap) hmArgumentMap.get("paramMap");
            String strProjectOID = (String) hmParamMap.get("New OID");
            String strPartOID = (String) hmParamMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strProjectOID) && UIUtil.isNotNullAndNotEmpty(strPartOID)) {
                DomainObject doProject = DomainObject.newInstance(context, strProjectOID);
                String strProjectName = doProject.getInfo(context, DomainConstants.SELECT_NAME);
                if (UIUtil.isNotNullAndNotEmpty(strProjectName)) {
                    TigerUtils.pushContextToSuperUser(context);
                    bSuperUser = true;
                    BusinessObject boPart = new BusinessObject(strPartOID);
                    boPart.open(context);
                    boPart.setProjectOwner(context, strProjectName);
                    boPart.update(context);
                    boPart.close(context);
                    DomainObject doPart = DomainObject.newInstance(context, strPartOID);
                    HashMap hmSettingsMap = (HashMap) ((HashMap) hmArgumentMap.get("fieldMap")).get("settings");
                    String strChangeTo = (String) hmSettingsMap.get("ChangeTo");
                    if (UIUtil.isNotNullAndNotEmpty(strChangeTo)) {
                        String strNewPolicy = "Standard".equals(strChangeTo) ? TigerConstants.POLICY_STANDARDPART
                                : "EC".equals(strChangeTo) ? TigerConstants.POLICY_PSS_ECPART : DomainConstants.EMPTY_STRING;
                        doPart.setPolicy(context, strNewPolicy);
                        String strhistory = "modify bus $1 add history $2 comment $3;";
                        MqlUtil.mqlCommand(context, strhistory, new String[] { strPartOID, "change policy", "Action to change policy is requested by " + TigerUtils.getLoggedInUserName(context) });
                    }
                    MapList mlPartSpecification = doPart.getRelatedObjects(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, DomainConstants.QUERY_WILDCARD,
                            new StringList(DomainConstants.SELECT_ID), null, false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
                    int iPS = mlPartSpecification.size();
                    for (int i = 0; i < iPS; i++) {
                        Map mpPS = (Map) mlPartSpecification.get(i);
                        strPartOID = (String) mpPS.get(DomainConstants.SELECT_ID);
                        boPart = new BusinessObject(strPartOID);
                        boPart.open(context);
                        boPart.setProjectOwner(context, strProjectName);
                        boPart.update(context);
                        boPart.close(context);
                    }
                    ContextUtil.popContext(context);
                    bSuperUser = false;
                }
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        } finally {
            if (bSuperUser) {
                ContextUtil.popContext(context);
                bSuperUser = false;
            }
        }
        logger.debug(":::::::: EXIT :: updateCollaborativeSpace ::::::::");
    }

    /**
     * Method to get list of Collaborative Space already connected with the Business Unit as a Standard CS TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getConnectedStandardCSOfBU(Context context, String[] args) throws Exception {
        logger.debug(":::::::: ENTER :: getConnectedStandardCSOfBU ::::::::");
        MapList mlStdCS = null;
        try {
            HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
            String strOId = (String) hmParamMap.get("objectId");
            StringList slObjSelects = new StringList(2);
            slObjSelects.add(DomainConstants.SELECT_ID);
            slObjSelects.add(TigerConstants.SELECT_ATTRIBUTE_PNO_VISIBILITY_VALUE);

            DomainObject doBusinessUnit = DomainObject.newInstance(context, strOId);
            mlStdCS = doBusinessUnit.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_STANDARD_COLLABORATIVE_SPACE, DomainConstants.TYPE_PNOPROJECT, slObjSelects,
                    new StringList(DomainRelationship.SELECT_ID), false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
		if(null == mlStdCS)
			mlStdCS = new MapList();
		
        logger.debug(":::::::: EXIT :: getConnectedStandardCSOfBU ::::::::");
        return mlStdCS;
    }

    /**
     * Method to to list all eligibile CS in a search result to connect with Business Unit as a Standard CS TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList getAllowedStandardCSForBU(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: getAllowedStandardCSForBU ::::::::");
        StringList slStandardCS = new StringList();
        try {
            HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
            String strBUOid = (String) hmParamMap.get("objectId");
            DomainObject doBusinessUnit = DomainObject.newInstance(context, strBUOid);
            MapList mlSecContext = doBusinessUnit.getRelatedObjects(context, DomainRelationship.RELATIONSHIP_SECURITYCONTEXT_ORGANIZATION, DomainConstants.TYPE_SECURITYCONTEXT,
                    new StringList("from[" + DomainRelationship.RELATIONSHIP_SECURITYCONTEXT_PROJECT + "].to.id"), null, true, false, (short) 1, DomainConstants.EMPTY_STRING,
                    DomainConstants.EMPTY_STRING, 0);
            if (!mlSecContext.isEmpty()) {
                MapList mlStdCS = doBusinessUnit.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_STANDARD_COLLABORATIVE_SPACE, DomainConstants.TYPE_PNOPROJECT,
                        new StringList(DomainConstants.SELECT_ID), null, false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
                Iterator itrStdCS = mlStdCS.iterator();
                StringList slConnectedStdCS = new StringList(mlStdCS.size());
                while (itrStdCS.hasNext()) {
                    Map mpStdCS = (Map) itrStdCS.next();
                    String strPnOProject = (String) mpStdCS.get(DomainConstants.SELECT_ID);
                    slConnectedStdCS.add(strPnOProject);
                }
                Iterator itrSecContext = mlSecContext.iterator();
                while (itrSecContext.hasNext()) {
                    Map mpSecContext = (Map) itrSecContext.next();
                    String strPnOProject = (String) mpSecContext.get("from[" + DomainRelationship.RELATIONSHIP_SECURITYCONTEXT_PROJECT + "].to.id");
                    if (!slStandardCS.contains(strPnOProject) && !slConnectedStdCS.contains(strPnOProject)) {
                        slStandardCS.add(strPnOProject);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: getAllowedStandardCSForBU ::::::::");
        return slStandardCS;
    }

    /**
     * Method to list all eligible(Standard/Default to Bu) Collaborative Spaces in search result to perform transfer ownership from EC to STD TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList getStandardOrDefaultCSOfBU(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: getStandardOrDefaultCSOfBU ::::::::");
        StringList slCollaborativeSpace = new StringList();
        try {
            String strActiveOrganization = TigerUtils.getActiveSecurityContextOrganization(context);
            String strBUOid = DomainConstants.EMPTY_STRING;
            MapList mlBusinessUnit = DomainObject.findObjects(context, DomainConstants.TYPE_BUSINESS_UNIT, strActiveOrganization, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD,
                    TigerConstants.VAULT_ESERVICEPRODUCTION, DomainConstants.EMPTY_STRING, false, new StringList(DomainConstants.SELECT_ID));
            if (!mlBusinessUnit.isEmpty()) {
                strBUOid = (String) ((Map) mlBusinessUnit.get(0)).get(DomainConstants.SELECT_ID);
            }
            if (UIUtil.isNotNullAndNotEmpty(strBUOid)) {
                DomainObject doBusinessUnit = DomainObject.newInstance(context, strBUOid);
                MapList mlStdCS = doBusinessUnit.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_STANDARD_COLLABORATIVE_SPACE, DomainConstants.TYPE_PNOPROJECT,
                        new StringList(DomainConstants.SELECT_ID), null, false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
                String strPnOProject = DomainConstants.EMPTY_STRING;
                if (!mlStdCS.isEmpty()) {
                    Iterator itrStdCS = mlStdCS.iterator();
                    while (itrStdCS.hasNext()) {
                        Map mpStdCS = (Map) itrStdCS.next();
                        strPnOProject = (String) mpStdCS.get(DomainConstants.SELECT_ID);
                        slCollaborativeSpace.add(strPnOProject);
                    }
                } else {
                    MapList mlSecContext = doBusinessUnit.getRelatedObjects(context, DomainRelationship.RELATIONSHIP_SECURITYCONTEXT_ORGANIZATION, DomainConstants.TYPE_SECURITYCONTEXT,
                            new StringList("from[" + DomainRelationship.RELATIONSHIP_SECURITYCONTEXT_PROJECT + "].to.id"), null, true, false, (short) 1, DomainConstants.EMPTY_STRING,
                            DomainConstants.EMPTY_STRING, 0);
                    Iterator itrSecContext = mlSecContext.iterator();
                    while (itrSecContext.hasNext()) {
                        Map mpSecContext = (Map) itrSecContext.next();
                        strPnOProject = (String) mpSecContext.get("from[" + DomainRelationship.RELATIONSHIP_SECURITYCONTEXT_PROJECT + "].to.id");
                        slCollaborativeSpace.add(strPnOProject);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: getStandardOrDefaultCSOfBU ::::::::");
        return slCollaborativeSpace;
    }

    /**
     * Method to list all eligible Collaborative Spaces in search result to perform transfer ownership from STD to EC TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList getCSForTransferOwnershipFromStandardToECPart(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: getCSForTransferOwnershipFromStandardToECPart ::::::::");
        StringList slCollaborativeSpace = new StringList();
        try {
            HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
            String strPartOID = (String) hmParamMap.get("objectId");
            DomainObject doPart = DomainObject.newInstance(context, strPartOID);
            TigerUtils.pushContextToSuperUser(context);
            MapList mlParentParts = doPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, new StringList("project"), null, true, false, (short) 1,
                    DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
            ContextUtil.popContext(context);
            String strPnOProject = DomainConstants.EMPTY_STRING;
            StringBuilder sbProjects = new StringBuilder(1024);
            MapList mlPnOProjects = null;
            if (!mlParentParts.isEmpty()) {
                Iterator itrParts = mlParentParts.iterator();
                while (itrParts.hasNext()) {
                    Map mpParentPart = (Map) itrParts.next();
                    strPnOProject = (String) mpParentPart.get("project");
                    sbProjects.append(strPnOProject).append(TigerConstants.SEPERATOR_COMMA);
                }
                if (sbProjects.length() > 0) {
                    sbProjects.setLength(sbProjects.length() - 1);
                    mlPnOProjects = DomainObject.findObjects(context, DomainConstants.TYPE_PNOPROJECT, sbProjects.toString(), DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD,
                            TigerConstants.VAULT_ESERVICEPRODUCTION, DomainConstants.EMPTY_STRING, false, new StringList(DomainConstants.SELECT_ID));
                }
            } else {
                mlPnOProjects = DomainObject.findObjects(context, DomainConstants.TYPE_PNOPROJECT, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD,
                        TigerConstants.VAULT_ESERVICEPRODUCTION, DomainConstants.EMPTY_STRING, false, new StringList(DomainConstants.SELECT_ID));
            }
            if (null != mlPnOProjects) {
                Iterator itrPnOProjects = mlPnOProjects.iterator();
                while (itrPnOProjects.hasNext()) {
                    Map mpPnOProject = (Map) itrPnOProjects.next();
                    strPnOProject = (String) mpPnOProject.get(DomainConstants.SELECT_ID);
                    slCollaborativeSpace.add(strPnOProject);
                }
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: getCSForTransferOwnershipFromStandardToECPart ::::::::");
        return slCollaborativeSpace;
    }
}
