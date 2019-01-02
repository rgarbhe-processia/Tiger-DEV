package pss.ecm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.domain.DomainAccess;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class MultiProgramChange_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MultiProgramChange_mxJPO.class);

    // TIGTK-6862 : Phase-2.0 : PKH : START
    public static final int GET_IMPACTED_ONLY = 1;

    public static final int GET_GOVERNING_ONLY = 2;

    public static final int GET_BOTH_PROJECTS_TYPES = 0;

    /**
     * Program to get the list of Governing or Impacted project for Part/CAD/stand part
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public MapList getConnectedGoverningOrImpactedProject(Context context, String args[]) throws Exception {
        MapList mlProgramProjects = new MapList();
        logger.debug("getConnectedGoverningOrImpactedProject : START");
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domObjPart = DomainObject.newInstance(context, strObjectId);

                mlProgramProjects = getConnectedProjectFromAffectedItem(context, domObjPart, DomainConstants.EMPTY_STRING, GET_BOTH_PROJECTS_TYPES);
                // PCM2.0 TIGTK-10418: 9/10/2017:START
                if (mlProgramProjects == null) {
                    mlProgramProjects = new MapList();

                }
                // PCM2.0 TIGTK-10418: 9/10/2017:END
            }
            logger.debug(" getConnectedGoverningOrImpactedProject : END");
        } catch (Exception e) {
            logger.error("getConnectedGoverningOrImpactedProject : ERROR ", e);
            throw e;
        }
        return mlProgramProjects;
    }

    /**
     * API to get the list of governing or impacted or all projects connected to AI
     * @param context
     * @param domObjAffectedItem
     * @param objectWhere
     * @return
     * @throws Exception
     */
    public MapList getConnectedProjectFromAffectedItem(Context context, DomainObject domObjAffectedItem, String objectWhere, int iImpactedOrGovering) throws Exception {

        logger.debug(" getConnectedProjectFromAffectedItem : START");
        boolean isPushed = false;
        try {

            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainObject.SELECT_ID);
            slObjectSelect.add(DomainObject.SELECT_TYPE);
            slObjectSelect.add(DomainObject.SELECT_REVISION);
            slObjectSelect.add(DomainObject.SELECT_CURRENT);

            StringList slRelSelect = new StringList();
            slRelSelect.add(DomainRelationship.SELECT_ID);

            String strRelPattern = DomainConstants.EMPTY_STRING;
            MapList mlProgramProjects = null;
            switch (iImpactedOrGovering) {
            case GET_BOTH_PROJECTS_TYPES:
                strRelPattern = TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "," + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT;
                break;
            case GET_IMPACTED_ONLY:
                strRelPattern = TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT;
                break;
            case GET_GOVERNING_ONLY:
                strRelPattern = TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT;
                break;
            default:
                // Optional
                break;

            }

            try {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isPushed = true;
                // TIGTK-13922 : Replaced getRelatedObjects with getExpansionIterator to improve ALC performance
                short sQueryLimit = 0;
                ContextUtil.startTransaction(context, false);
                ExpansionIterator expIter = domObjAffectedItem.getExpansionIterator(context, strRelPattern, TigerConstants.TYPE_PSS_PROGRAMPROJECT, slObjectSelect, slRelSelect, true, false, (short) 0,
                        objectWhere, "", sQueryLimit, true, false, (short) 100, false);
                mlProgramProjects = FrameworkUtil.toMapList(expIter, sQueryLimit, null, null, null, null);
                expIter.close();
                ContextUtil.commitTransaction(context);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("getConnectedProjectFromAffectedItem : ERROR ", e);
                throw e;
            } finally {
                if (isPushed) {
                    ContextUtil.popContext(context);
                }

            }

            logger.debug("getConnectedProjectFromAffectedItem : END");
            return mlProgramProjects;
        } catch (Exception e) {
            logger.error("getConnectedProjectFromAffectedItem : ERROR ", e);
            throw e;
        }

    }

    /**
     * @param context
     * @param args
     * @throws Exception
     *             This method is used to check part is connected to Governing Project or not. If part is not connected to Governing Project then connect Part to the project(which is connected to the
     *             connected CO)
     */
    public void connectGoveringProject(Context context, String args[]) throws Exception {

        logger.debug("connectGoveringProject : START");
        try {
            String strAffectedItemID = args[0];
            DomainObject domAffectedItem = DomainObject.newInstance(context, strAffectedItemID);
            String strGoveringPrjId = domAffectedItem.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");

            if (UIUtil.isNullOrEmpty(strGoveringPrjId)) {
                updateOrAddGoverningProject(context, domAffectedItem);

            }
            logger.debug("connectGoveringProject : END");
        } catch (FrameworkException ex) {

            logger.error("connectGoveringProject : ERROR ", ex);
            String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                    "PSS_EnterpriseChangeMgt.Notice.ConnectionOFGoveringProjectErrorMsg");
            ex.addMessage(strMessage);
            throw ex;
        }

    }

    /**
     * this method will add governing project to Part/CAd and if project is present as impacted project please update rel attribute to governing
     * @param context
     * @param domAffectedItem
     * @return
     * @throws Exception
     */
    public void updateOrAddGoverningProject(Context context, DomainObject domAffectedItem) throws Exception {

        String strConnectedProgramId = DomainConstants.EMPTY_STRING;
        logger.debug("addGoveringProject : START");
        try {
            String strCompleteCA = domAffectedItem.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.id");
            // PCM - TIGTK-10182 : Start : 29/09/2017
            if (UIUtil.isNullOrEmpty(strCompleteCA)) {

                strCompleteCA = domAffectedItem.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.id");
            }
            if (UIUtil.isNotNullAndNotEmpty(strCompleteCA)) {

                DomainObject domCompleteCA = DomainObject.newInstance(context, strCompleteCA);
                String strCOId = domCompleteCA.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
                if (UIUtil.isNotNullAndNotEmpty(strCOId)) {
                    DomainObject domObjCO = DomainObject.newInstance(context, strCOId);
                    strConnectedProgramId = domObjCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

                }
                DomainObject domObjProject = DomainObject.newInstance(context);
                if (UIUtil.isNotNullAndNotEmpty(strConnectedProgramId)) {
                    domObjProject.setId(strConnectedProgramId);
                }

                String strWhereClause = DomainObject.SELECT_ID + "==" + strConnectedProgramId;
                MapList mlImpactdProjects = getConnectedProjectFromAffectedItem(context, domAffectedItem, strWhereClause, GET_IMPACTED_ONLY);
                if (mlImpactdProjects != null && !mlImpactdProjects.isEmpty()) {
                    Map<?, ?> mProject = (Map<?, ?>) mlImpactdProjects.get(0);
                    String strRelID = (String) mProject.get(DomainRelationship.SELECT_ID);
                    DomainRelationship.disconnect(context, strRelID);
                }
                DomainRelationship.connect(context, domObjProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT, domAffectedItem);
                logger.debug("addGoveringProject : END");
            }
            // PCM - TIGTK-10182 : End : 29/09/2017
        } catch (FrameworkException ex) {
            logger.error("addGoveringProject : ERROR ", ex);
            String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                    "PSS_EnterpriseChangeMgt.Notice.ConnectionOFGoveringProjectErrorMsg");
            ex.addMessage(strMessage);
            throw ex;
        }

    }

    /**
     * @param context
     * @param domAI
     * @param strconnectedProgramid
     * @throws Exception
     *             This method will add Parent Parts/CADs governing project as Impacted Project to child Part/CAD and if project is not present as impacted project on child.
     */
    public void addImpactedProjectOnChild(Context context, String[] args) throws Exception {

        logger.debug(" addImpactedProjectOnChild : START");
        try {
            String strConnectedProgramId = args[0];
            String strParentAIId = args[1];

            DomainObject domAffectedItem = DomainObject.newInstance(context, strParentAIId);

            DomainObject domParentProject = DomainObject.newInstance(context, strConnectedProgramId);

            Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_CADSUBCOMPONENT);
            relPattern.addPattern(DomainConstants.RELATIONSHIP_EBOM);

            MapList mlImmediateChildOIDsList = domAffectedItem.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    DomainConstants.QUERY_WILDCARD, // object pattern
                    new StringList(DomainConstants.SELECT_ID), // object selects
                    DomainConstants.EMPTY_STRINGLIST, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    DomainConstants.EMPTY_STRING, // object where clause
                    null, 0); // relationship where clause

            Iterator<Map<?, ?>> itrChild = mlImmediateChildOIDsList.iterator();
            while (itrChild.hasNext()) {

                Map<?, ?> mChild = (Map<?, ?>) itrChild.next();
                String strChildID = (String) mChild.get(DomainConstants.SELECT_ID);
                DomainObject domObjChild = DomainObject.newInstance(context, strChildID);

                String strWhereClause = DomainObject.SELECT_ID + "==" + strConnectedProgramId;
                MapList mlChildProjectList = getConnectedProjectFromAffectedItem(context, domObjChild, strWhereClause, GET_BOTH_PROJECTS_TYPES);

                if (mlChildProjectList == null || mlChildProjectList.isEmpty()) {
                    DomainRelationship domObjRel = DomainRelationship.connect(context, domParentProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT, domObjChild);

                }
            }
            logger.debug("addImpactedProjectOnChild : END");
        } catch (FrameworkException e) {
            logger.error("addImpactedProjectOnChild : ERROR ", e);
            String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                    "PSS_EnterpriseChangeMgt.Notice.ConnectedImpactedProjectErrorMsg");
            e.addMessage(strMessage);
            throw e;
        }

    }

    /**
     * @param context
     * @param args
     * @throws Exception
     *             This method is called from Trigger on EBOM relationship to add parents Govering project as childs Impacted project
     */
    public void connectImpactedProjectToChild(Context context, String[] args) throws Exception {

        logger.debug("connectImpactedProjectToChild : START");
        try {

            String strParentObjectId = args[0];
            String strChildObjectId = args[1];

            DomainObject domObjParent = DomainObject.newInstance(context, strParentObjectId);

            String strParentGovPrjectId = domObjParent.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");

            if (UIUtil.isNotNullAndNotEmpty(strParentGovPrjectId)) {

                String strWhereClause = DomainObject.SELECT_ID + "==" + strParentGovPrjectId;
                DomainObject domObjChild = DomainObject.newInstance(context, strChildObjectId);
                MapList mlChildProjectList = getConnectedProjectFromAffectedItem(context, domObjChild, strWhereClause, GET_BOTH_PROJECTS_TYPES);

                if (mlChildProjectList == null || mlChildProjectList.isEmpty()) {

                    DomainObject domParentProject = DomainObject.newInstance(context, strParentGovPrjectId);
                    DomainRelationship.connect(context, domParentProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT, domObjChild);

                }
            }
            logger.debug("connectImpactedProjectToChild : END");
        } catch (FrameworkException e) {

            logger.error("connectImpactedProjectToChild : ERROR ", e);
            String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                    "PSS_EnterpriseChangeMgt.Notice.ConnectedImpactedProjectErrorMsg");
            e.addMessage(strMessage);
            throw e;
        }
    }

    /**
     * @param context
     * @param args
     * @throws Exception
     *             This method is called from Trigger on EBOM/ CAD SubComponent relationship to disconnect child Impacted project (which is parents Governing project)when EBOM/ CAD SubComponent rel is
     *             deleted
     */
    // PCM TIGTK-10720: 02/11/2017 : START
    public void disconnectImpactedProjectFromChild(Context context, String[] args) throws Exception {

        logger.debug("disconnectImpactedProjectFromChild : START");

        try {

            String strParentObjectId = args[0];
            String strChildObjectId = args[1];
            String strRelationshipName = args[2];

            String strOtherParentsGoveringProject = DomainConstants.EMPTY_STRING;
            ArrayList<String> lsArrOtherParentsProjectList = new ArrayList<String>();

            DomainObject domObjParent = DomainObject.newInstance(context, strParentObjectId);

            String strParentGovPrjectId = domObjParent.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");

            if (UIUtil.isNotNullAndNotEmpty(strParentGovPrjectId)) {

                StringList objectSelect = new StringList();
                objectSelect.add(DomainConstants.SELECT_ID);

                DomainObject domObjChild = DomainObject.newInstance(context, strChildObjectId);

                MapList mlAllParentList = domObjChild.getRelatedObjects(context, strRelationshipName, DomainConstants.QUERY_WILDCARD, objectSelect, DomainObject.EMPTY_STRINGLIST, true, false,
                        (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);

                if (mlAllParentList.size() == 0) {

                    String strQuery = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "|from.id" + "==" + strParentGovPrjectId + "].id";

                    String strCmd = "print bus \"" + strChildObjectId + "\"   select  \"" + strQuery + "\"  dump | ";

                    String strRelResult = MqlUtil.mqlCommand(context, strCmd, true, true);

                    if (UIUtil.isNotNullAndNotEmpty(strRelResult)) {

                        DomainRelationship.disconnect(context, strRelResult);
                    }
                    // PCM TIGTK-11042: 08/11/2017 : START
                } else {
                    Iterator<Map<?, ?>> itrAllParent = mlAllParentList.iterator();
                    StringList slOtherParentIdList = new StringList();
                    while (itrAllParent.hasNext()) {
                        Map<?, ?> mAllParent = (Map<?, ?>) itrAllParent.next();
                        String strOtherParentID = (String) mAllParent.get(DomainConstants.SELECT_ID);
                        slOtherParentIdList.add(strOtherParentID);

                    }
                    StringList slAllParent = removeRevisionOfCurrentParent(context, slOtherParentIdList, strParentObjectId);
                    if (!slAllParent.isEmpty()) {
                        Iterator itrSlAllParent = slAllParent.iterator();
                        while (itrSlAllParent.hasNext()) {

                            String strParentPartID = (String) itrSlAllParent.next();
                            DomainObject domObjOtherParent = DomainObject.newInstance(context, strParentPartID);

                            strOtherParentsGoveringProject = domObjOtherParent.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");

                            lsArrOtherParentsProjectList.add(strOtherParentsGoveringProject);

                        }
                    }
                    // PCM TIGTK-11042: 08/11/2017 : START
                    if (!lsArrOtherParentsProjectList.isEmpty()) {
                        if (!lsArrOtherParentsProjectList.contains(strParentGovPrjectId)) {

                            String strRelSelect = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "|from.id" + "==" + strParentGovPrjectId + "].id";

                            String strCommand = "print bus \"" + strChildObjectId + "\"   select  \"" + strRelSelect + "\"  dump | ";
                            String strChildImpProjectRelID = MqlUtil.mqlCommand(context, strCommand, true, true);

                            if (UIUtil.isNotNullAndNotEmpty(strChildImpProjectRelID)) {

                                DomainRelationship.disconnect(context, strChildImpProjectRelID);
                            }

                        }
                    }
                }
            }
            logger.debug("disconnectImpactedProjectFromChild : END");
        } catch (FrameworkException e) {
            logger.error("disconnectImpactedProjectFromChild : ERROR ", e);

            throw e;
        }
    }

    // PCM TIGTK-10720: 02/11/2017 : END
    // TIGTK-6862 : Phase-2.0 : PKH : END

    // PCM TIGTK-11042: 08/11/2017 : START
    public StringList removeRevisionOfCurrentParent(Context context, StringList slOtherParentIdList, String strParentObjectId) throws Exception {
        StringList slFinalPartIdList = new StringList();
        slFinalPartIdList.addAll(slOtherParentIdList);
        try {

            DomainObject domCurrentParentObj = DomainObject.newInstance(context, strParentObjectId);

            String strParentObjName = domCurrentParentObj.getInfo(context, DomainConstants.SELECT_NAME);

            String strParentObjPP = domCurrentParentObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");

            if (!slOtherParentIdList.isEmpty()) {
                Iterator itrOtherParents = slOtherParentIdList.iterator();

                while (itrOtherParents.hasNext()) {

                    String strFirstElement = (String) itrOtherParents.next();

                    DomainObject domFirstElement = DomainObject.newInstance(context, strFirstElement);

                    String strFirstElementName = domFirstElement.getInfo(context, DomainConstants.SELECT_NAME);

                    String strFirstElementPP = domFirstElement.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");

                    // TIGTK-15094 START
                    if ((UIUtil.isNotNullAndNotEmpty(strFirstElementName) && UIUtil.isNotNullAndNotEmpty(strFirstElementPP)) && (strFirstElementName.equalsIgnoreCase(strParentObjName))
                            && (strFirstElementPP.equalsIgnoreCase(strParentObjPP))) {
                        slFinalPartIdList.remove(strFirstElement);
                    }
                    // TIGTK-15094 END
                }
            }
        } catch (FrameworkException e) {
            logger.error("removeRevisionOfCurrentParent : ERROR ", e);

            throw e;
        }
        return slFinalPartIdList;

    }
    // PCM TIGTK-11042: 08/11/2017 : START

    // TIGTK-6866 : Phase-2.0 : PKH : START
    /**
     * Automatically CR creation from Submit To Evaluate State.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void autoCreatePrerequisiteCRs(Context context, String[] args) throws Exception {
        logger.debug("autoCreatePrerequisiteCRs : START");
        try {
            String strCRID = args[0];
            DomainObject domObjCR = DomainObject.newInstance(context, strCRID);
            String strDesc = domObjCR.getDescription(context);
            // PCM TIGTK-10720: 30/10/2017 : START
            Map<String, String> attributeMap = domObjCR.getAttributeMap(context, true);
            if (attributeMap.containsKey("PSS_ParallelTrack")) {
                String strParrelTrackComment = attributeMap.get("PSS_ParallelTrack");

                if (strParrelTrackComment.equalsIgnoreCase("Yes")) {
                    attributeMap.remove("PSS_ParallelTrack");
                    attributeMap.remove("PSS_ParallelTrackComment");
                }

            }
            // TIGTK-10757 - START
            if (attributeMap.containsKey("PSS_CRTitle")) {
                attributeMap.remove("PSS_CRTitle");
            }
            // TIGTK-10757 - END

            Map<String, List<String>> mapPPvsAI = getUniqueProgramProject(context, domObjCR);
            // TIGTK-17601,TIGTK-17757 :Start
            Map<String, DomainObject> mapGoverningProjectCR = new HashMap<>();

            if (null != mapPPvsAI) {
                Iterator<Entry<String, List<String>>> mapIterator = mapPPvsAI.entrySet().iterator();
                for (; mapIterator.hasNext();) {
                    Entry<String, List<String>> entry = mapIterator.next();
                    String programId = entry.getKey();
                    if (!(programId.contains(TigerConstants.STRING_PROGRAMPROJECT_TYPE_GOVERNING + "_") || programId.contains(TigerConstants.STRING_PROGRAMPROJECT_TYPE_IMPACTED + "_"))) {
                        mapIterator.remove();
                    } else if (entry.getKey().contains(TigerConstants.STRING_PROGRAMPROJECT_TYPE_IMPACTED + "_")) {
                        String[] infoProgramProject = programId.split("_");
                        if (infoProgramProject.length == 2) {
                            mapIterator.remove();
                        }
                    }
                }
            }
            // TIGTK-17601,TIGTK-17757 :End
            List<String> lstNewCRs = new ArrayList<String>();
            if (!mapPPvsAI.isEmpty()) {
                // PCM TIGTK-10720: 30/10/2017 : End
                for (Entry<String, List<String>> entry : mapPPvsAI.entrySet()) {
                    String strProgramProject = entry.getKey();
                    // TIGTK-17601,TIGTK-17757:Start
                    String strGovernigProject = DomainConstants.EMPTY_STRING;

                    String progType = DomainConstants.EMPTY_STRING;
                    String[] infoProgramProject = strProgramProject.split("_");

                    if (infoProgramProject.length == 2) {
                        strProgramProject = infoProgramProject[1];
                        progType = infoProgramProject[0];
                    }

                    if (infoProgramProject.length == 3) {
                        strProgramProject = infoProgramProject[1];
                        progType = infoProgramProject[0];
                        strGovernigProject = infoProgramProject[2];
                    }
                    // TIGTK-17601,TIGTK-17757 :End
                    List<String> lstTemp = new ArrayList<String>();
                    lstTemp.addAll(entry.getValue());
                    HashSet<String> hsActiveCRIDs = getActiveCRForProgramProject(context, domObjCR, strProgramProject, progType);
                    if (hsActiveCRIDs != null && hsActiveCRIDs.size() > 0) {
                        String strRelWhere = "attribute[" + TigerConstants.ATTRIBUTE_REQUESTED_CHANGE + "]== 'For Revise' || attribute[" + TigerConstants.ATTRIBUTE_REQUESTED_CHANGE + "]=='"
                                + TigerConstants.FOR_REPLACE + "' || attribute[" + TigerConstants.ATTRIBUTE_REQUESTED_CHANGE + "]=='For Obsolescence'";

                        for (String sCRId : hsActiveCRIDs) {
                            StringList slObjectSelect = new StringList();
                            slObjectSelect.add(DomainConstants.SELECT_ID);
                            DomainObject domObjActiveCR = DomainObject.newInstance(context, sCRId);
                            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            MapList mlAffectedItems = domObjActiveCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, DomainConstants.QUERY_WILDCARD, slObjectSelect,
                                    DomainConstants.EMPTY_STRINGLIST, false, true, (short) 0, DomainConstants.EMPTY_STRING, strRelWhere, 0);
                            ContextUtil.popContext(context);

                            Iterator<Map<?, ?>> itrAffectedItem = mlAffectedItems.iterator();
                            StringList slAIIds = new StringList();
                            while (itrAffectedItem.hasNext()) {
                                Map<?, ?> mapTemp = itrAffectedItem.next();
                                Object sAffectedItemID = (Object) mapTemp.get(DomainConstants.SELECT_ID);
                                if (sAffectedItemID instanceof String) {
                                    slAIIds.add((String) sAffectedItemID);
                                } else if (sAffectedItemID instanceof StringList) {
                                    slAIIds = (StringList) sAffectedItemID;
                                }
                                lstTemp.removeAll(slAIIds);
                            }
                            if (lstTemp.size() > 0) {
                                connectAffectedItemToCR(context, domObjActiveCR, lstTemp);
                            }
                        }
                    } else {
                        // PCM TIGTK-10939: 03/11/2017 : START
                        // TIGTK-17601,TIGTK-17757 :Start
                        if (progType.equalsIgnoreCase(TigerConstants.STRING_PROGRAMPROJECT_TYPE_GOVERNING)) {
                            lstTemp = getAffectedItemsToBeTransferedToNewCR(context, strCRID, lstTemp, strProgramProject, true);
                        } else {
                            lstTemp = getAffectedItemsToBeTransferedToNewCR(context, strCRID, lstTemp, strProgramProject, false);
                        }
                        // TIGTK-17601,TIGTK-17757 :End
                        if (!lstTemp.isEmpty()) {
                            // PCM TIGTK-10939: 03/11/2017 : START
                            DomainObject domObjPP = DomainObject.newInstance(context, strProgramProject);
                            StringList slPPSelects = new StringList();
                            slPPSelects.add(DomainAccess.SELECT_PROJECT);
                            slPPSelects.add(DomainConstants.SELECT_ORGANIZATION);
                            // TIGTK-17596 : 26/10/2018 : Prakash B - START
                            slPPSelects.add(DomainConstants.SELECT_CURRENT);
                            // TIGTK-17596 : 26/10/2018 : Prakash B - END
                            Map<String, String> mpPPinfo = domObjPP.getInfo(context, slPPSelects);

                            String strPPCurrent = mpPPinfo.get(DomainConstants.SELECT_CURRENT);
                            // TIGTK-17596 : 26/10/2018 : Prakash B - START
                            if (!(TigerConstants.STATE_ACTIVE.equalsIgnoreCase(strPPCurrent) || TigerConstants.STATE_OBSOLETE.equalsIgnoreCase(strPPCurrent)
                                    || TigerConstants.STATE_NONAWARDED.equalsIgnoreCase(strPPCurrent))) {
                                // TIGTK-17596 : 26/10/2018 : Prakash B - END
                                StringList objSelect = new StringList();

                                objSelect.add(DomainConstants.SELECT_NAME);
                                objSelect.add(DomainConstants.SELECT_ID);

                                StringBuffer relWhere = new StringBuffer();
                                relWhere.append("attribute[");
                                relWhere.append(TigerConstants.ATTRIBUTE_PSS_POSITION);
                                relWhere.append("]");
                                relWhere.append(" == ");
                                relWhere.append("Lead");
                                relWhere.append(" && ");
                                relWhere.append("attribute[");
                                relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
                                relWhere.append("]");
                                relWhere.append(" == '");
                                relWhere.append(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                                relWhere.append("'");

                                pss.ecm.enoECMChange_mxJPO enoECMChange = new pss.ecm.enoECMChange_mxJPO();
                                MapList mlConnectedChangeCoordMembersList = enoECMChange.getMembersFromProgram(context, domObjPP, objSelect, DomainConstants.EMPTY_STRINGLIST,
                                        DomainConstants.EMPTY_STRING, relWhere.toString());
                                String strChangeCoordName = DomainConstants.EMPTY_STRING;
                                String strChangeId = DomainConstants.EMPTY_STRING;
                                for (int i = 0; i < mlConnectedChangeCoordMembersList.size(); i++) {
                                    Map<?, ?> mapConnectedChangeCoordMember = (Map<?, ?>) mlConnectedChangeCoordMembersList.get(i);
                                    strChangeCoordName = (String) mapConnectedChangeCoordMember.get(DomainConstants.SELECT_NAME);
                                    strChangeId = (String) mapConnectedChangeCoordMember.get(DomainConstants.SELECT_ID);
                                }
                                String strObjectGeneratorName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, ChangeConstants.TYPE_CHANGE_REQUEST, true);
                                String strAutoName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, null);
                                // TIGTK-13649 :START

                                BusinessObject boObjNewCR = new BusinessObject(TigerConstants.TYPE_PSS_CHANGEREQUEST, strAutoName, "-", context.getVault().getName());
                                DomainObject domObjNewCR = new DomainObject(boObjNewCR);
                                MqlUtil.mqlCommand(context, "history off", true, false);
                                ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                domObjNewCR.create(context, TigerConstants.POLICY_PSS_CHANGEREQUEST, null, new AttributeList(), mpPPinfo.get(DomainConstants.SELECT_ORGANIZATION),
                                        mpPPinfo.get(DomainAccess.SELECT_PROJECT));
                                // TIGTK-10757 - START
                                String strNewCRName = domObjNewCR.getInfo(context, DomainConstants.SELECT_NAME);
                                String strCRId = domObjNewCR.getInfo(context, DomainConstants.SELECT_ID);

                                attributeMap.put("PSS_CRTitle", strNewCRName);
                                // TIGTK-10757 - END
                                PropertyUtil.setGlobalRPEValue(context, "SubmitChange", "YES");
                                domObjNewCR.setOwner(context, strChangeCoordName);
                                domObjNewCR.setAttributeValues(context, attributeMap);
                                domObjNewCR.setDescription(context, strDesc);
                                domObjNewCR.setAttributeValue(context, DomainConstants.ATTRIBUTE_ORIGINATOR, strChangeCoordName);
                                // TIGTK-17784 : START
                                domObjNewCR.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROJECT_PHASE_AT_CREATION, strPPCurrent);
                                // TIGTK-17784 : END
                                DomainRelationship.connect(context, domObjPP, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, domObjNewCR);
                                // TIGTK-17601,TIGTK-17757:Start
                                if (progType.equalsIgnoreCase(TigerConstants.STRING_PROGRAMPROJECT_TYPE_GOVERNING)) {
                                    DomainRelationship.connect(context, domObjCR, TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR, domObjNewCR);
                                    mapGoverningProjectCR.put(strProgramProject, domObjNewCR);
                                } else if (progType.equalsIgnoreCase(TigerConstants.STRING_PROGRAMPROJECT_TYPE_IMPACTED)) {
                                    if (null != mapGoverningProjectCR.get(strGovernigProject)) {
                                        domObjCR = mapGoverningProjectCR.get(strGovernigProject);
                                    }
                                    DomainRelationship.connect(context, domObjCR, TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR, domObjNewCR);
                                }
                                // TIGTK-17601,TIGTK-17757:End
                                DomainObject domObjChangeManager = DomainObject.newInstance(context, strChangeId);
                                DomainRelationship.connect(context, domObjNewCR, TigerConstants.RELATIONSHIP_CHANGECOORDINATOR, domObjChangeManager);

                                lstNewCRs.add(domObjNewCR.getId(context));

                                connectAffectedItemToCR(context, domObjNewCR, lstTemp);
                                ContextUtil.popContext(context);
                                MqlUtil.mqlCommand(context, "history on", true, false);

                                StringBuffer sbHistoryAction = new StringBuffer();
                                sbHistoryAction.delete(0, sbHistoryAction.length());
                                sbHistoryAction.append(" Create / Connect ");
                                sbHistoryAction.append(strNewCRName);
                                sbHistoryAction.append(" ");
                                sbHistoryAction.append(TigerConstants.TYPE_PSS_CHANGEREQUEST);
                                sbHistoryAction.append(" ");
                                sbHistoryAction.append("-");
                                modifyHistory(context, strCRId, sbHistoryAction.toString(), " ");
                                // TIGTK-13649 :END
                                // TIGTK-17596 : 26/10/2018 : Prakash B - START
                            }
                            // TIGTK-17596 : 26/10/2018 : Prakash B - END
                        }
                    }
                }
                sendAutoCreationCRNotification(context, lstNewCRs);
            }
            logger.debug("autoCreatePrerequisiteCRs : END");
        } catch (Exception e) {
            logger.error(" autoCreatePrerequisiteCRs : ERROR ", e);
            throw e;
        }
    }

    // PCM TIGTK-10939: 03/11/2017 : START
    // TIGTK-17601,TIGTK-17757
    public List<String> getAffectedItemsToBeTransferedToNewCR(Context context, String strCRID, List<String> lstTemp, String strNewCRPP, Boolean isGoverningProject) throws Exception {

        List<String> lstFinalAI = lstTemp;
        DomainObject domCR = DomainObject.newInstance(context, strCRID);

        StringList slFromPreRequisiteCR = domCR.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR + "].to.id");
        StringList slToPreRequisiteCR = domCR.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR + "].from.id");
        StringList slPreRequisiteCR = new StringList();
        StringList slAffectedItemsWithSameCRPP = new StringList();

        if (!slToPreRequisiteCR.isEmpty()) {
            slPreRequisiteCR.addAll(slToPreRequisiteCR);
        }
        if (!slFromPreRequisiteCR.isEmpty()) {
            slPreRequisiteCR.addAll(slFromPreRequisiteCR);
        }
        if (!slPreRequisiteCR.isEmpty()) {
            Iterator itrPreRequisteCR = slPreRequisiteCR.iterator();
            while (itrPreRequisteCR.hasNext()) {
                String strPreCRId = (String) itrPreRequisteCR.next();
                DomainObject domPreCR = DomainObject.newInstance(context, strPreCRId);
                ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                String strProgramProjectOfCR = domPreCR.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

                String strPreRequisiteCRsCurrent = domPreCR.getInfo(context, DomainConstants.SELECT_CURRENT);
                StringList slAffectedItem = domPreCR.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");
                ContextUtil.popContext(context);
                if (!slAffectedItem.isEmpty()) {
                    Iterator itrAI = slAffectedItem.iterator();
                    while (itrAI.hasNext()) {
                        String strAIId = (String) itrAI.next();
                        if (strProgramProjectOfCR.equalsIgnoreCase(strNewCRPP)
                                && !(TigerConstants.STATE_COMPLETE_CR.equals(strPreRequisiteCRsCurrent) || TigerConstants.STATE_REJECTED_CR.equals(strPreRequisiteCRsCurrent))) {
                            slAffectedItemsWithSameCRPP.add(strAIId);
                        }
                        // TIGTK-17601,TIGTK-17757
                        if (isGoverningProject && lstFinalAI.contains(strAIId)
                                && (TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strPreRequisiteCRsCurrent) || TigerConstants.STATE_PSS_CR_CREATE.equalsIgnoreCase(strPreRequisiteCRsCurrent))
                                || TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strPreRequisiteCRsCurrent)) {
                            lstFinalAI.remove(strAIId);
                        }
                    }
                }
            }
            Iterator itrFinalAI = lstFinalAI.iterator();
            boolean bFlag = false;
            while (itrFinalAI.hasNext()) {
                String strAffectedItemId = (String) itrFinalAI.next();
                if (!slAffectedItemsWithSameCRPP.contains(strAffectedItemId)) {
                    bFlag = true;
                    break;
                }
            }
            if (bFlag == false) {
                lstFinalAI.clear();
            }
        }
        return lstFinalAI;
    }
    // PCM TIGTK-10939: 03/11/2017 : START

    /**
     * get unique progarm project of affected item.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Map<String, List<String>> getUniqueProgramProject(Context context, DomainObject domCR) throws Exception {
        // TIGTK-17601,TIGTK-17757
        Map<String, List<String>> mapPPvsAI = new TreeMap<String, List<String>>();
        logger.debug("getUniqueProgramProject : START");
        try {
            String strProgramProjectOfCR = domCR.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            String strRelWhere = "attribute[" + TigerConstants.ATTRIBUTE_REQUESTED_CHANGE + "]=='For Revise' || attribute[" + TigerConstants.ATTRIBUTE_REQUESTED_CHANGE + "]=='"
                    + TigerConstants.FOR_REPLACE + "' || attribute[" + TigerConstants.ATTRIBUTE_REQUESTED_CHANGE + "]=='For Obsolescence'";

            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_ID);
            // TIGTK-17601,TIGTK-17757 :Start
            StringList slRelationShipSelect = new StringList();
            slRelationShipSelect.add(DomainRelationship.SELECT_NAME);

            MapList mlAffectedItems = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, DomainConstants.QUERY_WILDCARD, slObjectSelect, DomainConstants.EMPTY_STRINGLIST,
                    false, true, (short) 0, DomainConstants.EMPTY_STRING, strRelWhere, 0);

            Iterator<Map<?, ?>> itrAffectedItem = mlAffectedItems.iterator();
            String projectType = DomainConstants.EMPTY_STRING;
            while (itrAffectedItem.hasNext()) {
                Map<?, ?> mapTemp = itrAffectedItem.next();
                String sAffectedItemId = (String) mapTemp.get(DomainConstants.SELECT_ID);
                DomainObject domAffectedItem = DomainObject.newInstance(context, sAffectedItemId);
                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT);
                String strWhereClause = DomainObject.SELECT_ID + "!=" + strProgramProjectOfCR;
                MapList mlGoverningAndImpactedPPs = domAffectedItem.getRelatedObjects(context, relPattern.getPattern(), TigerConstants.TYPE_PSS_PROGRAMPROJECT, slObjectSelect, slRelationShipSelect,
                        true, false, (short) 1, strWhereClause, DomainConstants.EMPTY_STRING, 0);
                Iterator<Map<?, ?>> itrGoverningAndImpactedPPs = mlGoverningAndImpactedPPs.iterator();
                while (itrGoverningAndImpactedPPs.hasNext()) {
                    Map<?, ?> mpGPPandIPPInfo = itrGoverningAndImpactedPPs.next();
                    String strPPID = (String) mpGPPandIPPInfo.get(DomainConstants.SELECT_ID);
                    projectType = (String) mpGPPandIPPInfo.get(DomainRelationship.SELECT_NAME);
                    if (projectType.equalsIgnoreCase(TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT)) {
                        projectType = TigerConstants.STRING_PROGRAMPROJECT_TYPE_GOVERNING;
                    } else if (projectType.equalsIgnoreCase(TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT)) {
                        projectType = TigerConstants.STRING_PROGRAMPROJECT_TYPE_IMPACTED;
                    }
                    if (!mapPPvsAI.containsKey(strPPID)) {
                        mapPPvsAI.put(strPPID, new ArrayList<String>());
                    }
                    if (!mapPPvsAI.containsKey(projectType + "_" + strPPID)) {
                        mapPPvsAI.put(projectType + "_" + strPPID, new ArrayList<String>());
                    }
                    mapPPvsAI.get(strPPID).add(sAffectedItemId);
                    mapPPvsAI.get(projectType + "_" + strPPID).add(sAffectedItemId);
                }
            }

            if (!mapPPvsAI.isEmpty()) {

                Map<String, String> mapAItoGP = new HashMap<>();
                Map<String, List<String>> mapIPvsAI = new HashMap<>();

                // Code to create the Affected Item to Governing Project Mapping
                // Code to add the Impacted Project -> Affected Item -> Governing Project Mapping
                Iterator<Entry<String, List<String>>> mapIterator = mapPPvsAI.entrySet().iterator();
                for (; mapIterator.hasNext();) {
                    Entry<String, List<String>> programProjectToAI = mapIterator.next();
                    String strProgramProject = programProjectToAI.getKey();
                    String[] infoProgramProject = strProgramProject.split("_");
                    if (infoProgramProject.length == 2) {
                        String strProgramProjectId = infoProgramProject[1];
                        String progType = infoProgramProject[0];
                        if (TigerConstants.STRING_PROGRAMPROJECT_TYPE_GOVERNING.equals(progType)) {
                            List<String> listAI = programProjectToAI.getValue();
                            if (null != listAI && !listAI.isEmpty()) {
                                for (String affectedItem : listAI) {
                                    mapAItoGP.put(affectedItem, strProgramProjectId);
                                }
                            }
                        } else if (TigerConstants.STRING_PROGRAMPROJECT_TYPE_IMPACTED.equals(progType)) {
                            List<String> listAI = programProjectToAI.getValue();
                            if (null != listAI && !listAI.isEmpty()) {
                                for (String impactedItem : listAI) {
                                    String sGPid = mapAItoGP.get(impactedItem);
                                    if (UIUtil.isNullOrEmpty(sGPid)) {
                                        sGPid = strProgramProjectOfCR;
                                    }
                                    String strKey = strProgramProject + "_" + sGPid;
                                    if (mapIPvsAI.containsKey(strKey)) {
                                        mapIPvsAI.get(strKey).add(impactedItem);
                                    } else {
                                        List<String> listImpactedItem = new ArrayList<String>();
                                        listImpactedItem.add(impactedItem);
                                        mapIPvsAI.put(strKey, listImpactedItem);
                                    }
                                }
                            }
                        }
                    }
                }
                mapPPvsAI.putAll(mapIPvsAI);
                // TIGTK-17601,TIGTK-17757 :End
            }
            logger.debug("getUniqueProgramProject : END");
        } catch (Exception e) {
            logger.error("getUniqueProgramProject : ERROR ", e);
            throw e;
        }
        return mapPPvsAI;
    }

    /**
     * get Active CR from program project.
     * @param context
     * @param progType
     * @param args
     * @return
     * @throws Exception
     */
    public HashSet<String> getActiveCRForProgramProject(Context context, DomainObject domCR, String sProgramProjectID, String progType) throws Exception {

        HashSet<String> hsActiveCRIDs = new HashSet<String>();
        logger.debug("getActiveCRForProgramProject : START");
        try {
            StringList objectSelect = new StringList();
            objectSelect.add(DomainConstants.SELECT_ID);
            objectSelect.add(DomainConstants.SELECT_CURRENT);
            objectSelect.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            String sWhereClause = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id ==" + sProgramProjectID + " && (current ==" + TigerConstants.STATE_PSS_CR_CREATE
                    + " || current==" + TigerConstants.STATE_SUBMIT_CR + ")";
            // TIGTK-17601,TIGTK-17757 :Start
            String relWhereClause = "attribute[" + TigerConstants.ATTRIBUTE_PSS_MANDATORYCR + "]=='" + TigerConstants.ATTRIBUTE_PSS_MANDATORYCR_RANGE_MANDATORY + "'";
            if (TigerConstants.STRING_PROGRAMPROJECT_TYPE_IMPACTED.equalsIgnoreCase(progType)) {
                relWhereClause = "attribute[" + TigerConstants.ATTRIBUTE_PSS_MANDATORYCR + "]=='" + TigerConstants.ATTRIBUTE_PSS_MANDATORYCR_RANGE_OPTIONAL + "'";
            }
            // TIGTK-17601,TIGTK-17757 :End
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            // TIGTK-17601,TIGTK-17757 :Start
            MapList mlFromCRs = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR, TigerConstants.TYPE_PSS_CHANGEREQUEST, objectSelect, DomainConstants.EMPTY_STRINGLIST,
                    true, false, (short) 0, sWhereClause, relWhereClause, 0);
            MapList mlToCRs = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR, TigerConstants.TYPE_PSS_CHANGEREQUEST, objectSelect, DomainConstants.EMPTY_STRINGLIST,
                    false, true, (short) 0, sWhereClause, relWhereClause, 0);
            // TIGTK-17601,TIGTK-17757 :End
            ContextUtil.popContext(context);
            MapList mlMergedCRs = new MapList();
            if (!mlFromCRs.isEmpty()) {
                mlMergedCRs.addAll(mlFromCRs);
            }
            if (!mlToCRs.isEmpty()) {
                mlMergedCRs.addAll(mlToCRs);
            }

            if (mlMergedCRs.isEmpty()) {
                return null;
            }

            Iterator<Map<?, ?>> itrCRs = mlMergedCRs.iterator();
            while (itrCRs.hasNext()) {
                Map<?, ?> mpCR = itrCRs.next();
                hsActiveCRIDs.add((String) mpCR.get(DomainConstants.SELECT_ID));

            }
            logger.debug("getActiveCRForProgramProject : END");
        } catch (Exception e) {
            logger.error(" getActiveCRForProgramProject : ERROR ", e);
            throw e;
        }

        return hsActiveCRIDs;

    }

    /**
     * connect affected item to CR
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void connectAffectedItemToCR(Context context, DomainObject domCR, List<String> slAffectedItemIds) throws Exception {
        logger.debug("connectAffectedItemToCR : START");
        try {
            // PCM TIGTK-10720: 30/10/2017 : START
            // String[] sAIIds = slAffectedItemIds.toArray(new String[0]);
            PropertyUtil.setGlobalRPEValue(context, "PSS_AutoCRCreation", "True");
            Iterator<String> itrAI = slAffectedItemIds.iterator();
            while (itrAI.hasNext()) {

                String strAffectedItemID = itrAI.next();
                DomainObject domAffectedItem = DomainObject.newInstance(context, strAffectedItemID);
                String strAICurrent = domAffectedItem.getInfo(context, DomainConstants.SELECT_CURRENT);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                DomainRelationship domRElPSS_AffectedItem = DomainRelationship.connect(context, domCR, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, domAffectedItem);
                if (TigerConstants.STATE_PART_RELEASE.equalsIgnoreCase(strAICurrent) || TigerConstants.STATE_RELEASED_CAD_OBJECT.equalsIgnoreCase(strAICurrent)) {
                    domRElPSS_AffectedItem.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_REVISE);
                } else if (TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equalsIgnoreCase(strAICurrent) || TigerConstants.STATE_PART_REVIEW.equalsIgnoreCase(strAICurrent)
                        || TigerConstants.STATE_CAD_REVIEW.equalsIgnoreCase(strAICurrent) || TigerConstants.STATE_INWORK_CAD_OBJECT.equalsIgnoreCase(strAICurrent)) {
                    domRElPSS_AffectedItem.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_RELEASE);

                }
                ContextUtil.popContext(context);

            }
            logger.debug(" connectAffectedItemToCR : END");
        } catch (Exception e) {
            logger.error("connectAffectedItemToCR : ERROR ", e);
            throw e;
        }
    }
    // PCM TIGTK-10720: 30/10/2017 : E

    /**
     * Send notification for Automatic CR has been created.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void sendAutoCreationCRNotification(Context context, List<String> lstCRIds) throws Exception {
        logger.debug(" sendAutoCreationCRNotification : START");
        try {
            Iterator<String> itrCRIds = lstCRIds.iterator();
            while (itrCRIds.hasNext()) {

                String strCRId = (String) itrCRIds.next();
                String strnotifyargs[] = { strCRId, "PSS_AutoCRCreateNotification" };

                JPO.invoke(context, "emxNotificationUtil", null, "objectNotification", strnotifyargs, null);
                logger.debug(" sendAutoCreationCRNotification : END");
            }
            logger.debug(" sendAutoCreationCRNotification : END");
        } catch (Exception e) {
            logger.error(" sendAutoCreationCRNotification : ERROR ", e);
            throw e;
        }

    }

    /**
     * get Tolist for mail notification
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getAutoCreatedCRNotificationRecipients(Context context, String[] args) throws Exception {
        boolean isContextPushed = false;
        StringList slRole = new StringList();
        String strChangeManager = DomainConstants.EMPTY_STRING;
        logger.debug("getAutoCreatedCRNotificationRecipients : START");
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strCRObjId = (String) programMap.get(DomainConstants.SELECT_ID);
            String strContextUser = context.getUser();
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isContextPushed = true;
            DomainObject domObjCR = DomainObject.newInstance(context, strCRObjId);
            strChangeManager = domObjCR.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].to.name");
            slRole.add(strChangeManager);

            while (slRole.contains(strContextUser)) {
                slRole.remove(strContextUser);
            }

            logger.debug("getAutoCreatedCRNotificationRecipients : END");
        } catch (Exception e) {
            logger.error("getAutoCreatedCRNotificationRecipients : ERROR ", e);
            throw e;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
                isContextPushed = false;
            }
        }
        return slRole;

    }

    /**
     * This Method is used to get connected related Change Request from a Change request
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public MapList getPreRequisiteCRs(Context context, String[] args) throws Exception {

        logger.debug("getPreRequisiteCRs : START");
        try {

            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);

            String strChangeRequestObjectId = (String) programMap.get("objectId");
            DomainObject domChangeRequest = new DomainObject(strChangeRequestObjectId);

            StringList slObjectSelect = new StringList(1);
            slObjectSelect.addElement(DomainConstants.SELECT_ID);

            StringList slRelSelect = new StringList(1);
            slRelSelect.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            MapList mlCRList = domChangeRequest.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR, // Relationship Pattern
                    TigerConstants.TYPE_PSS_CHANGEREQUEST, // Type Pattern
                    slObjectSelect, // Object Select
                    slRelSelect, // Relationship Select
                    false, // To Side
                    true, // from Side
                    (short) 1, // Recursion Level
                    "", // Object Where clause
                    "", // Relationship Where clause
                    0, // Limit
                    null, // Post Relationship Patten
                    null, // Post Type Pattern
                    null); // Post Patterns
            logger.debug("getPreRequisiteCRs : END");
            return mlCRList;
        } catch (Exception e) {
            logger.error("getPreRequisiteCRs : ERROR ", e);
            throw e;
        }

    }

    // TIGTK-6866 : Phase-2.0 : PKH : END
    // TIGTK-6874 : Phase-2.0 : PKH : START
    /**
     * Mandatory CR column visibility
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static StringList isEditableMandatoryCR(Context context, String[] args) throws Exception {
        logger.debug("isEditableMandatoryCR : START");
        StringList slRole = new StringList();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

            MapList mlCRObjectList = (MapList) programMap.get("objectList");
            String userName = context.getUser();
            String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, userName);
            String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);
            // TIGTK-17656, TIGTK-17125 - START
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

            StringList slObjectSelect = new StringList(1);
            slObjectSelect.addElement(DomainConstants.SELECT_NAME);
            StringList slRelSelect = new StringList(2);
            slRelSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]");
            slRelSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_POSITION + "]");

            MapList mlObjList;
            String strProgramProjectId = "";
            // TIGTK-17656, TIGTK-17125 - END
            for (int i = 0; i < mlCRObjectList.size(); i++) {
                Map<?, ?> mpObjectData = (Map<?, ?>) mlCRObjectList.get(i);
                String strId = (String) mpObjectData.get("id");
                DomainObject domObjetCR = DomainObject.newInstance(context, strId);
                // TIGTK-14824 : 19-06-2018 - START
                // TIGTK-17656, TIGTK-17125 - START
                strProgramProjectId = domObjetCR.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                DomainObject domProgProjectObj = DomainObject.newInstance(context, strProgramProjectId);
                mlObjList = domProgProjectObj.getRelatedObjects(context, // context
                        TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, // Relationship Pattern
                        DomainConstants.TYPE_PERSON, // Type Pattern
                        slObjectSelect, // Object Select
                        slRelSelect, // Relationship Select
                        false, // To Side
                        true, // from Side
                        (short) 1, // Recursion Level
                        DomainConstants.SELECT_NAME + "==" + userName, // Object Where clause
                        "", // Relationship Where clause
                        0, // Limit
                        null, // Post Relationship Patten
                        null, // Post Type Pattern
                        null); // Post Patterns
                if (mlObjList.size() > 0) {
                    for (int j = 0; j < mlObjList.size(); j++) {
                        Map<?, ?> mpRoleData = (Map<?, ?>) mlObjList.get(j);
                        String strRole = (String) mpRoleData.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]");
                        String strPosition = (String) mpRoleData.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_POSITION + "]");
                        if ((strRole.equalsIgnoreCase(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR) && strPosition.equalsIgnoreCase("Lead"))
                                || (strRole.equals(TigerConstants.ROLE_PSS_PROGRAM_MANAGER) && strPosition.equalsIgnoreCase("Lead"))
                                || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                            slRole.addElement("true");
                        } else {
                            slRole.addElement("false");
                        }
                    }
                } else {
                    if (assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                        slRole.addElement("true");
                    } else {
                        slRole.addElement("false");
                    }
                }
                // TIGTK-17656, TIGTK-17125 - END
            }
            ContextUtil.popContext(context);
            logger.debug(" isEditableMandatoryCR : END");
        } catch (Exception e) {
            logger.error(" isEditableMandatoryCR : ERROR ", e);

            throw e;
        }

        return slRole;
    }

    // TIGTK-6874 : Phase-2.0 : PKH : END
    // TIGTK-6885: Phase-2.0 : Hiren : Start
    /****
     * This method is used to get list of Governing Program-projects on selected Affected Item
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList getListOfGoverningProgramprojects(Context context, String[] args) throws Exception {
        logger.debug("getListOfGoverningProgramprojects : Start");
        StringList slProjectList = new StringList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String slAIList[] = (String[]) programMap.get("sourceAffectedItemRowIds");

            for (String sAI : slAIList) {
                String strAIID = sAI;

                if (UIUtil.isNotNullAndNotEmpty(strAIID)) {
                    DomainObject domAI = DomainObject.newInstance(context, strAIID);
                    String strGoverningProjectID = domAI.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                    if (UIUtil.isNotNullAndNotEmpty(strGoverningProjectID)) {
                        if (!slProjectList.contains(strGoverningProjectID))
                            slProjectList.add(strGoverningProjectID);
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Error in getListOfGoverningProgramprojects: ", ex);
            throw ex;
        }
        logger.debug("getListOfGoverningProgramprojects : End");
        return slProjectList;
    }

    /****
     * This method is used to define search criteria of CO search for functionality "Add to Existing CO"
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList includeCOList(Context context, String args[]) throws Exception {
        logger.debug("includeCOList : Start");
        StringList includeCOList = new StringList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strAIProjectID = (String) programMap.get("strAIProjectID");
            DomainObject domProject = DomainObject.newInstance(context, strAIProjectID);
            // Get the Connected members of Program-Project
            StringList slMemberList = (StringList) domProject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");

            // Check if logged in User is member of Program - project or not
            if (slMemberList.contains(context.getUser())) {
                // Get the Connected Change Order Objects with "PSS_ConnectedPCMData" relationship
                StringList slObjectSle = new StringList(1);
                slObjectSle.addElement(DomainConstants.SELECT_ID);
                String swhrClause = "current == " + TigerConstants.STATE_PSS_CHANGEORDER_PREPARE;
                MapList mlConnectedChangeOrder = domProject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, TigerConstants.TYPE_PSS_CHANGEORDER, slObjectSle,
                        DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, swhrClause, null, 0);
                if (!mlConnectedChangeOrder.isEmpty()) {
                    Iterator itrCO = mlConnectedChangeOrder.iterator();
                    while (itrCO.hasNext()) {
                        Map mCO = (Map) itrCO.next();
                        String strCOID = (String) mCO.get(DomainConstants.SELECT_ID);
                        if (UIUtil.isNotNullAndNotEmpty(strCOID)) {
                            includeCOList.add(strCOID);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error in includeCOList in JPO MultiProgramChange : ", ex);
        }
        logger.debug("includeCOList : End");
        return includeCOList;
    }

    // TIGTK-6885 :Phase-2.0: Hiren:END
    /**
     * This method is used to modify the History of the object.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param strObjectId
     *            - The object id.
     * @return strAction - The action to be added in the history.
     * @return strComment - The comment to be added in the history
     * @throws Exception
     *             if the operation fails //TIGTK-13649 :
     */
    protected void modifyHistory(Context context, String strObjectId, String strAction, String strComment) throws Exception {
        logger.debug("modifyHistory : START");
        StringBuffer sbMqlCommand = new StringBuffer("modify bus ");
        sbMqlCommand.append(strObjectId);
        sbMqlCommand.append(" add history \"");
        sbMqlCommand.append("disconnect ");
        sbMqlCommand.append("\" comment \"");
        sbMqlCommand.append(strAction);

        MqlUtil.mqlCommand(context, sbMqlCommand.toString(), true);
        logger.debug("modifyHistory : END");
    }

} // End of Class