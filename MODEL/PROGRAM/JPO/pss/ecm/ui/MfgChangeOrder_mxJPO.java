package pss.ecm.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.DomainSymbolicConstants;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.lifecycle.CalculateSequenceNumber;
import com.matrixone.apps.framework.ui.UIUtil;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.db.Signature;
import matrix.db.SignatureList;
import matrix.util.DateFormatUtil;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;
import matrix.db.User;

/**
 * The <code>pss.ecm.ui.MfgChangeOrder</code> class contains code for the "Manufacturing Change Order" business type.
 */

public class MfgChangeOrder_mxJPO {

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MfgChangeOrder_mxJPO.class);
    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END

    // TGPSS_PCM_TS_155_ManufacturingChangeOrderNotifications_V3.1 | 28/02/2017 |Harika Varanasi : Starts
    // Findbug Issue correction start
    // Date: 21/03/2017
    // By: Asha G.

    public LinkedHashMap<String, String> lhmMCOSelectionStore = new LinkedHashMap<String, String>();

    // Findbug Issue correction End
    // TGPSS_PCM_TS_155_ManufacturingChangeOrderNotifications_V3.1 | 28/02/2017 |Harika Varanasi : Ends

    // TGPSS_PCM-TS156 Manufacturing Change Action Notifications V_2.2 | 20/03/2017 |Harika Varanasi : Starts
    public LinkedHashMap<String, String> lhmMCASelectionStore = new LinkedHashMap<String, String>();

    // TGPSS_PCM-TS156 Manufacturing Change Action Notifications V_2.2 | 20/03/2017 |Harika Varanasi : Ends

    // TIGTK-11455 : Sayali D--START | 22-Nov-2017
    public LinkedHashMap<String, String> lhmMCARejectSelectionStore = new LinkedHashMap<String, String>();

    public LinkedHashMap<String, String> lhmMCACommentsSelectionStore = new LinkedHashMap<String, String>();
    // TIGTK-11455 : Sayali D--END | 22-Nov-2017

    /**
     * This method is invoked via Config.xml which is used for filtering of MBOM Objects based on Plants. TIGTK-3060, TIGTK-3538
     * @param context
     * @param args
     * @returntype -- String --- Returns Object Id of MBOM Object
     * @throws Exception
     */
    public String filterMBOMBasedOnPlant(Context context, String[] args) throws Exception {
        DomainObject domObject = null;
        try {
            String mfgId = args[0];
            String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, mfgId);
            if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                domObject = DomainObject.newInstance(context, strAttachedPlant);
            }
            if (domObject == null) {
                return DomainConstants.EMPTY_STRING;
            } else {
                return domObject.getInfo(context, DomainConstants.SELECT_ID);
            }
        } catch (Exception exp) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in filterMBOMBasedOnPlant: ", exp);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw exp;
        }
    }

    /**
     * TIGTK-3060, TIGTK-3538
     * @param context
     * @param instPID
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List<String> getPlantsAttachedToMBOMReference(Context context, String instPID) throws Exception {
        List returnList = new ArrayList();

        StringList lRsc = new StringList();

        String sAssemblyPID = "";
        if ((null != instPID) && (!("".equals(instPID))))
            sAssemblyPID = MqlUtil.mqlCommand(context, "print bus " + instPID + " select physicalid dump |", false, false);
        if ((null != sAssemblyPID) && (!("".equals(sAssemblyPID)))) {
            String listPathIDStr = MqlUtil.mqlCommand(context, "query path type SemanticRelation containing " + sAssemblyPID + " select id dump |", false, false);
            if (!("".equals(listPathIDStr))) {
                String[] listPathID = listPathIDStr.split("\n");
                for (String pathDesc : listPathID) {
                    String[] aPathDesc = pathDesc.split("\\|");
                    // Fix for FindBugs issue Redundant Null check capture: Harika Varanasi : 21 March 2017: START
                    if (1 < aPathDesc.length) {
                        // Fix for FindBugs issue Redundant Null check capture: Harika Varanasi : 21 March 2017: End
                        String pathID = aPathDesc[1];

                        String pathSemantics = MqlUtil.mqlCommand(context, "print path " + pathID + " select attribute[RoleSemantics].value dump |", false, false);
                        if ("PLM_ImplementLink_TargetReference3".equals(pathSemantics)) {
                            String targetPhysId = MqlUtil.mqlCommand(context, "print path " + pathID + " select owner.to[" + "VPLMrel/PLMConnection/V_Owner" + "].from.physicalid dump |", false,
                                    false);
                            if ((null != targetPhysId) && (!("".equals(targetPhysId)))) {
                                lRsc.addElement(targetPhysId);
                            }
                        }
                    }
                }
            }
        }
        for (Iterator i$ = lRsc.iterator(); i$.hasNext();) {
            Object rscPIDObj = i$.next();
            returnList.add((String) rscPIDObj);
        }
        return returnList;
    }

    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList get150MBOMRelatedMfgParts(Context context, String[] args) throws Exception {
        StringList includeOIDList = new StringList();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strMCOId = (String) programMap.get("parentId");

        DomainObject domMCOObj = new DomainObject(strMCOId);
        StringList slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_ID);
        StringList slRelSelect = new StringList();

        // String TYPE_CREATEASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_CreateAssembly");

        // 13/09/16 : Ketaki Wagh : Start : MBOM Pattern
        Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
        typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
        typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
        typePattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
        typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

        MapList mList = domMCOObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM, typePattern.getPattern(), slObjectSelect, slRelSelect, false, true, (short) 1, null, null,
                (short) 0);

        if (mList.size() > 0) {

            mList = domMCOObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE, typePattern.getPattern(), slObjectSelect, slRelSelect, false, true, (short) 1,
                    null, null, (short) 0);
            // 13/09/16 : Ketaki Wagh : End : MBOM Pattern
            int cntCreateAssembly = mList.size();
            for (int cnt = 0; cnt < cntCreateAssembly; cnt++) {
                includeOIDList.add((String) ((Map) mList.get(cnt)).get(DomainObject.SELECT_ID));
            }
        }
        return includeOIDList;
    }

    /**
     * This method is invoked via
     * @param context
     * @param args
     * @throws Exception
     */
    public MapList getMfgChangeOrderItems(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            // HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strCRId = (String) programMap.get("objectId");
            DomainObject domCRId = new DomainObject(strCRId);
            MapList mlTableData = new MapList();

            StringList slObjectSelects = new StringList(6);
            slObjectSelects.addElement(DomainConstants.SELECT_ID);
            slObjectSelects.addElement(DomainConstants.SELECT_TYPE);
            slObjectSelects.addElement(DomainConstants.SELECT_NAME);
            slObjectSelects.addElement(DomainConstants.SELECT_POLICY);
            slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
            slObjectSelects.addElement(DomainConstants.SELECT_DESCRIPTION);

            // Get The Manufacturing Change Order connected with CO and Return map of MCO
            MapList mlManufacturingChangeOrder = domCRId.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER,
                    slObjectSelects, new StringList(0), false, true, (short) 1, "", "", (short) 0);

            for (int j = 0; j < mlManufacturingChangeOrder.size(); j++) {
                Map mMCOObj = (Map) mlManufacturingChangeOrder.get(j);
                mlTableData.add(mMCOObj);
            }
            // }
            return mlTableData;

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMfgChangeOrderItems: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    /**
     * This method is invoked for Check create access for MCO from CR based on CR 'state' & 'Parallel Track' attribute value
     * @param context
     * @param args
     * @throws Exception
     */
    public boolean checkCreateAccessForMCO(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjId = (String) programMap.get("objectId");
            DomainObject domObjId = new DomainObject(strObjId);
            String strObjType = (String) domObjId.getInfo(context, DomainConstants.SELECT_TYPE);

            // check if type is Change Request Then check Create MCO Access condition from CO
            if (strObjType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) {

                String strCOState = (String) domObjId.getInfo(context, DomainConstants.SELECT_CURRENT);
                if (strCOState.equalsIgnoreCase("In Work") || strCOState.equalsIgnoreCase("In Approval") || strCOState.equalsIgnoreCase("Complete")) {
                    return true;
                } else {
                    return false;
                }

                // check if type is Change Request Then check Create MCO Access condition from CR
            } else if (strObjType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {

                String strCRState = (String) domObjId.getInfo(context, DomainConstants.SELECT_CURRENT);
                String strParallelTrack = (String) domObjId.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");

                if (strParallelTrack.equalsIgnoreCase("YES")) {
                    if (strCRState.equalsIgnoreCase("Evaluate") || strCRState.equalsIgnoreCase("In Process") || strCRState.equalsIgnoreCase("In Review")) {
                        return true;
                    }
                } else if (strParallelTrack.equalsIgnoreCase("NO")) {
                    if (strCRState.equalsIgnoreCase("In Process")) {
                        return true;
                    }
                } else {
                    return false;
                }
            }

            return false;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkCreateAccessForMCO: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    public StringList getParentProgramProjectName(Context context, String[] args) throws Exception {

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            StringList slObjectSle = new StringList(1);

            String strProgramProjectId = DomainConstants.EMPTY_STRING;
            StringList slParentProgramProject = new StringList();

            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            String strId = (String) programMap.get("objectId");

            DomainObject domObj = new DomainObject(strId);

            MapList mlConnectedProgramProject = domObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, "*", slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);
            if (!mlConnectedProgramProject.isEmpty()) {
                for (int i = 0; i < mlConnectedProgramProject.size(); i++) {
                    Map mProgramProject = (Map) mlConnectedProgramProject.get(i);
                    strProgramProjectId = (String) mProgramProject.get(DomainConstants.SELECT_ID);
                    slParentProgramProject.add(strProgramProjectId);
                }
            }
            return slParentProgramProject;
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getParentProgramProjectName: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw new FrameworkException(e);
        }
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Create the relationship PSS_ConnectedPCMData with ProgramProject at FROM side and MCO at TO side.
     */
    public int connectRelPCMData(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");

        String strMCOId = (String) paramMap.get("objectId");
        String strProgramProjectID = (String) paramMap.get("New OID");
        // PCM:TIGTK-5779:Rutuja Ekatpure:23/3/2017:start
        DomainObject domMCO = new DomainObject(strMCOId);
        DomainObject domProgramProject = new DomainObject(strProgramProjectID);

        // PCM TIGTK-6088: 04/04/2017 : KWagh : START
        StringList slObjSel = new StringList(1);
        slObjSel.addElement(DomainConstants.SELECT_ID);
        StringList slRelSle = new StringList(1);
        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEORDER);
        typePattern.addPattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

        MapList mlMCAs = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, slObjSel, slRelSle, false,
                true, (short) 1, null, null, 0);

        MapList mlCRCOs = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, typePattern.getPattern(), slObjSel, slRelSle, true, false, (short) 1, null, null,
                0);

        if ((!mlMCAs.isEmpty()) || (!mlCRCOs.isEmpty())) {

            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ErrorInMCOEdit");
            ;

            MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
            return 1;
        }

        // PCM TIGTK-6088: 04/04/2017 : KWagh : End

        if (UIUtil.isNotNullAndNotEmpty(strProgramProjectID)) {

            // get program project connected with MCO
            MapList mlProgProjList = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, TigerConstants.TYPE_PSS_PROGRAMPROJECT, null, slRelSle, true, false, (short) 1,
                    null, null, 0);

            if (!mlProgProjList.isEmpty()) {
                int nCount = mlProgProjList.size();

                for (int j = 0; j < nCount; j++) {
                    Map mProgProj = (Map) mlProgProjList.get(j);
                    String strRelID = (String) mProgProj.get(DomainRelationship.SELECT_RELATIONSHIP_ID);
                    // disconnect program project
                    DomainRelationship.disconnect(context, strRelID);
                }
            }
            // connect new Program project to MCO
            DomainRelationship.connect(context, domProgramProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, domMCO);
        }
        // PCM:TIGTK-5779:Rutuja Ekatpure:23/3/2017:end
        return 0;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method retrieves the list of all Plant Items from the system
     */
    public Map getPlantList(Context context, String[] args) throws Exception {

        HashMap rangeMap = new HashMap();
        String strWhere = DomainObject.EMPTY_STRING;
        StringList slPlantId = new StringList();
        StringList slPlantName = new StringList();

        StringList objectSelects = new StringList();
        objectSelects.addElement(DomainConstants.SELECT_ID);
        objectSelects.addElement(DomainConstants.SELECT_NAME);

        MapList mlPlant = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PLANT, "*", "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION, strWhere, true, objectSelects);

        if (!mlPlant.isEmpty()) {
            for (int i = 0; i < mlPlant.size(); i++) {
                Map mPlant = (Map) mlPlant.get(i);
                String strPlantId = (String) mPlant.get(DomainConstants.SELECT_ID);
                String strPlantName = (String) mPlant.get(DomainConstants.SELECT_NAME);
                slPlantId.add(strPlantId);
                slPlantName.add(strPlantName);
            }

        }
        rangeMap.put("field_choices", slPlantId);
        rangeMap.put("field_display_choices", slPlantName);
        return rangeMap;
    }

    // PCM TS175 Manufacturing Change Order- Properties layout: KWagh : START
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method connects the MCO to the Plant.
     */
    public int connectRelPlant(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");

        String strMCOId = (String) paramMap.get("objectId");
        String strPlantID = (String) paramMap.get("New OID");
        // PCM:TIGTK-5747:Rutuja Ekatpure:start
        if (UIUtil.isNullOrEmpty(strPlantID)) {
            strPlantID = (String) paramMap.get("New Value");
        }
        // PCM:TIGTK-5747:Rutuja Ekatpure:End
        // PCM:TIGTK-5779:Rutuja Ekatpure:23/3/2017:start

        DomainObject domMCO = new DomainObject(strMCOId);
        DomainObject domPlant = new DomainObject(strPlantID);

        // PCM TIGTK-6088: 04/04/2017 : KWagh : START
        StringList slObjSel = new StringList(1);
        slObjSel.addElement(DomainConstants.SELECT_ID);
        StringList slRelSle = new StringList(1);
        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEORDER);
        typePattern.addPattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

        MapList mlMCAs = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, slObjSel, slRelSle, false,
                true, (short) 1, null, null, 0);

        MapList mlCRCOs = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, typePattern.getPattern(), slObjSel, slRelSle, true, false, (short) 1, null, null,
                0);

        if ((!mlMCAs.isEmpty()) || (!mlCRCOs.isEmpty())) {

            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ErrorInMCOEdit");
            ;

            MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
            return 1;
        }

        // PCM TIGTK-6088: 04/04/2017 : KWagh : End

        // get already connected plant
        MapList mlPlantList = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT, TigerConstants.TYPE_PSS_PLANT, null, slRelSle, false, true, (short) 1, null,
                null, 0);

        if (!mlPlantList.isEmpty()) {
            int nCount = mlPlantList.size();

            for (int j = 0; j < nCount; j++) {
                Map mPlant = (Map) mlPlantList.get(j);
                String strRelID = (String) mPlant.get(DomainRelationship.SELECT_RELATIONSHIP_ID);
                // disconnect plant
                DomainRelationship.disconnect(context, strRelID);
            }
        }
        // connect plant to MCO
        DomainRelationship.connect(context, domMCO, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT, domPlant);
        // PCM:TIGTK-5779:Rutuja Ekatpure:23/3/2017:start
        return 0;
    }

    // PCM TS175 Manufacturing Change Order- Properties layout: KWagh : End

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method connects the MCO to the CO CR.
     */

    public void connectMfgChangeOrderItems(Context context, String[] args) throws Exception {

        // MapList mlChangeRequest = new MapList();
        String strType;
        String strCRID;

        StringList slObjectSle = new StringList(1);
        slObjectSle.addElement(DomainConstants.SELECT_ID);
        slObjectSle.addElement(DomainConstants.SELECT_NAME);

        StringList slRelSle = new StringList(1);
        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        HashMap requestMap = (HashMap) programMap.get("requestMap");

        String strId = (String) requestMap.get("parentOID");
        String strMCOID = (String) paramMap.get("objectId");
        if ((strId.equalsIgnoreCase("") || strId.equalsIgnoreCase(null)) && (strMCOID.equalsIgnoreCase("") || strMCOID.equalsIgnoreCase(null))) {
            // Do nothing
        } else {

            DomainObject domObj = new DomainObject(strId);
            DomainObject domMCO = new DomainObject(strMCOID);

            // Check if parent object is Change request
            strType = domObj.getType(context);
            if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                DomainRelationship.connect(context, domObj, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, domMCO);

            } else if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) {

                DomainRelationship.connect(context, domObj, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, domMCO);

                MapList mlChangeRequest = domObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSle, slRelSle, true, false,
                        (short) 1, null, null, 0);

                if (!mlChangeRequest.isEmpty()) {
                    for (int j = 0; j < mlChangeRequest.size(); j++) {
                        Map mCRObj = (Map) mlChangeRequest.get(j);
                        strCRID = (String) mCRObj.get(DomainConstants.SELECT_ID);
                        DomainObject domobjCR = new DomainObject(strCRID);
                        DomainRelationship.connect(context, domobjCR, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, domMCO);

                    }
                }
            }
            // TIGTK-2985 :Rutuja Ekatpure :06/09/2016:Start
            // Get value of attribute Purpose of Release attribute from Parent object

            // Set the value of attribute Purpose of Release on MCO .

            // Set the value of attribute Transfer to SAP Expected = Yes.
            // domMCO.setAttributeValue(context, "PSS_Trasfer_To_SAP_Expected", "Yes");
            // TIGTK-2985 :Rutuja Ekatpure :06/09/2016:End
        }
    }

    /**
     * This method is to promote the MCO to Rejected state and also to promote associated MCA to Cancelled state and Stop the Route of MCA Date : 03/01/2017 : TIGTK-3869 : AB
     * @param context
     * @param args
     * @throws Exception
     */
    public void rejectMfgChangeOrder(Context context, String[] args) throws Exception {
        try {
            // Get the ObjectId of Manufacturing Change Order
            @SuppressWarnings("rawtypes")
            HashMap param = (HashMap) JPO.unpackArgs(args);
            @SuppressWarnings("rawtypes")
            Map requestMap = (Map) param.get("requestMap");
            String objectId = (String) requestMap.get("objectId");
            DomainObject domMCO = new DomainObject(objectId);

            StringList objectSelect = new StringList();
            objectSelect.add(DomainConstants.SELECT_ID);
            StringList relSelect = new StringList();
            relSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            // Promote Manufacturing Change Order to Rejected state
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

            // PCM TIGTK-3902 : 09/01/2017 : AB : START
            // MqlUtil.mqlCommand(context, "trigger off", true, true);
            domMCO.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, "Rejected");
            domMCO.setState(context, "Rejected");
            // MqlUtil.mqlCommand(context, "trigger on", true, true);
            // PCM TIGTK-3902 : 09/01/2017 : AB : END

            // Get the related Manufacturing CHange Action of Manufacturing Change Order
            StringList slMCAIds = domMCO.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.id");

            if (!slMCAIds.isEmpty()) {
                for (int itr = 0; itr < slMCAIds.size(); itr++) {
                    DomainObject domMCA = new DomainObject((String) slMCAIds.get(itr));
                    MapList mlMCAConnectedRoute = domMCA.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, objectSelect, relSelect, false, true,
                            (short) 1, null, null, 0);

                    if (mlMCAConnectedRoute.size() != 0) {
                        for (int i = 0; i < mlMCAConnectedRoute.size(); i++) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> mapRoute = (Map<String, Object>) mlMCAConnectedRoute.get(i);
                            String strRouteID = (String) mapRoute.get(DomainConstants.SELECT_ID);
                            DomainObject domRoute = new DomainObject(strRouteID);

                            // Cancel Manufacturing Change Action and complete all Related Route to MCA
                            domRoute.setAttributeValue(context, "Route Status", "Stopped");
                            MqlUtil.mqlCommand(context, "trigger off", true, true);
                            domMCA.setState(context, "Cancelled");
                            domRoute.setState(context, "Complete");
                            MqlUtil.mqlCommand(context, "trigger on", true, true);
                        }
                    }
                }
            }

            ContextUtil.popContext(context);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in rejectMfgChangeOrder: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    // Modified by KWagh -TIGTK-2805- Start
    /**
     * This method is used to check for MCO related to this MCA is in Rejected State or state
     * @param context
     * @param args
     * @throws Exception
     */
    public boolean checkMCARelatedMCOCurrentState(Context context, String[] args) throws Exception {
        HashMap param = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) param.get("objectId");
        DomainObject domObjMCA = new DomainObject(objectId);
        String strMCACurrent = domObjMCA.getInfo(context, DomainConstants.SELECT_CURRENT);
        // TIGTK-10768 SayaliD : 2 Nov 2017 start
        String strMCAOwner = domObjMCA.getInfo(context, DomainConstants.SELECT_OWNER);
        String strMCATechnicalAssignee = domObjMCA.getInfo(context, "from[Technical Assignee].to.name");
        String strUserName = context.getUser();
        String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strUserName);
        String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);
        boolean bolAdminUser = false;

        if (assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
            bolAdminUser = true;
        }
        // TIGTK-10768 SayaliD : 2 Nov 2017 end
        boolean bolReturn = false;
        // Get Connected PSS_ManufacturingChangeOrder Objects

        StringList slObjectSle = new StringList(1);
        slObjectSle.addElement(DomainConstants.SELECT_ID);
        slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
        slObjectSle.addElement(DomainConstants.SELECT_OWNER);

        String strContextUser = context.getUser();

        StringList slRelSle = new StringList(1);
        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
        // PCM TIGTK-10768 : 16/11/17 : TS : START
        String strMCOobjId = domObjMCA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.id");
        StringList slRolesList = new StringList();
        slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANUFACTURING_LEADER);
        pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
        StringList slToMCAImplementedList = commonObj.getProgramProjectTeamMembersForChange(context, strMCOobjId, slRolesList, true);
        // PCM TIGTK-10768 : 16/11/17 : TS : END

        MapList mlConnectedMCO = domObjMCA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, slObjectSle,
                slRelSle, true, false, (short) 1, null, null, 0);
        // TIGTK-10768 SayaliD : 2 Nov 2017
        // If below conditions are TRUE : MCA assignee can be changed in "In Work" state.
        // MCA: Prepare/In Work
        // Context user : MCO owner/MCA onwer/MCA Assignee/Admin users
        // MCO : Not rejected
        if (!mlConnectedMCO.isEmpty()) {
            for (int i = 0; i < mlConnectedMCO.size(); i++) {
                Map mMCOObj = (Map) mlConnectedMCO.get(i);
                String strMCOState = (String) mMCOObj.get(DomainConstants.SELECT_CURRENT);
                String strMCOOwner = (String) mMCOObj.get(DomainConstants.SELECT_OWNER);

                // Check the Current state of MCO is rejected or not if current state is Rejected then return false
                // PCM TIGTK-10768 : 16/11/17 : TS : START
                if (TigerConstants.STATE_PSS_MCA_INWORK.equalsIgnoreCase(strMCACurrent) && slToMCAImplementedList.contains(strContextUser)) {
                    bolReturn = true;
                }
                if (!bolReturn) {
                    if (((strMCACurrent.equalsIgnoreCase("Prepare") || strMCACurrent.equalsIgnoreCase("In Work")) && (strContextUser.equalsIgnoreCase(strMCOOwner)
                            || strContextUser.equalsIgnoreCase(strMCAOwner) || bolAdminUser || strContextUser.equalsIgnoreCase(strMCATechnicalAssignee)))
                            && (!strMCOState.equalsIgnoreCase("Rejected"))) {
                        bolReturn = true;
                    }
                }

                // PCM TIGTK-10768 : 16/11/17 : TS : END

            }
        }

        return bolReturn;
    }

    // Modified by KWagh -TIGTK-2805- End

    public MapList getMCORelatedCOs(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            // HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strMCOId = (String) programMap.get("objectId");
            DomainObject domMCO = new DomainObject(strMCOId);
            MapList mlTableData = new MapList();
            StringList objectSelects = new StringList(6);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_TYPE);

            // Get Change Order objects connected with MCO
            MapList mlChangeOrderList = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, TigerConstants.TYPE_PSS_CHANGEORDER, objectSelects,
                    new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), true, false, (short) 1, "", "", (short) 0);

            if (!mlChangeOrderList.isEmpty()) {
                for (int j = 0; j < mlChangeOrderList.size(); j++) {
                    Map mCOObj = (Map) mlChangeOrderList.get(j);
                    mlTableData.add(mCOObj);
                }
            }
            return mlTableData;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMCORelatedCOs: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * @param context
     * @param args
     * @throws Exception
     */
    public MapList getMCORelatedCRs(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            // HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strMCOId = (String) programMap.get("objectId");
            DomainObject domMCO = new DomainObject(strMCOId);
            MapList mlTableData = new MapList();
            StringList objectSelects = new StringList(6);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_TYPE);

            // Get Change Request objects connected with MCO
            MapList mlChangeRequestList = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, TigerConstants.TYPE_PSS_CHANGEREQUEST, objectSelects,
                    new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), true, false, (short) 1, "", "", (short) 0);
            if (!mlChangeRequestList.isEmpty()) {
                for (int j = 0; j < mlChangeRequestList.size(); j++) {
                    Map mCRObj = (Map) mlChangeRequestList.get(j);
                    mlTableData.add(mCRObj);
                }
            }
            return mlTableData;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMCORelatedCRs: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * @param context
     * @param personObjectId
     * @return
     * @throws Exception
     *             This method is called from notification object to get To list mail of when MCA is Promoted to "In Work" state
     */
    public StringList getMCAAssigneesList(Context context, String args[]) throws Exception {
        StringList mlAssignee = new StringList();
        HashSet<String> personSet = new HashSet<>();
        String strMCAAssignee = DomainConstants.EMPTY_STRING;
        String strMCOId = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strMCADId = (String) programMap.get("id");
            DomainObject domMCA = new DomainObject(strMCADId);
            StringList busSelects = new StringList();
            busSelects.addElement(DomainConstants.SELECT_ID);
            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            MapList mlConnectedMCOs = domMCA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, busSelects,
                    slRelSle, true, false, (short) 1, null, null, 0);
            for (int index = 0; index < mlConnectedMCOs.size(); index++) {
                Map mMCOobj = (Map) mlConnectedMCOs.get(index);
                strMCOId = (String) mMCOobj.get(DomainConstants.SELECT_ID);
            }
            DomainObject domMCO = new DomainObject(strMCOId);
            String strMCOOriginator = domMCO.getAttributeValue(context, DomainConstants.ATTRIBUTE_ORIGINATOR);
            String strMCAAssigneeId = domMCA.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.id");
            if ((strMCAAssigneeId == null) || (strMCAAssigneeId.equals(""))) {
            } else {
                DomainObject domAssignee = new DomainObject(strMCAAssigneeId);
                strMCAAssignee = domAssignee.getName(context);
            }
            if (UIUtil.isNotNullAndNotEmpty(strMCAAssignee)) {
                personSet.add(strMCAAssignee);
            }
            if (UIUtil.isNotNullAndNotEmpty(strMCOOriginator)) {
                personSet.add(strMCOOriginator);
            }
            mlAssignee.addAll(personSet);
            return mlAssignee;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMCAAssigneesList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * @param context
     * @param args
     * @throws Exception
     *             This method is called from trigger object on MCO policy to promote all related MCA(s) in In Work state.
     */
    public void promoteMCAsToInWorkState(Context context, String args[]) throws Exception {
        // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : Start
        boolean isContextPushed = false;
        // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : End
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : Start
            String contextUser = (String) context.getUser();
            context.setCustomData("contextUser", contextUser);

            if (contextUser != null && !"".equals(contextUser)) {
                ContextUtil.pushContext(context, contextUser, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isContextPushed = true;
            }
            // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : End
            String strMCODId = args[0];
            // String strSelectMCA = "from["+TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION+"].to.id";
            DomainObject domMCO = new DomainObject(strMCODId);
            // PCM : TIGTK-4437 : AB : START
            SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, -1);
            Date date = cal.getTime();
            String strDate = _mxDateFormat.format(date);
            domMCO.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MCO_START_DATE, strDate);
            // PCM : TIGTK-4437 : AB : END
            StringList busSelects = new StringList();
            busSelects.addElement(DomainConstants.SELECT_ID);
            busSelects.addElement(DomainConstants.SELECT_CURRENT);
            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            MapList mlConnectedMCAs = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, busSelects,
                    slRelSle, false, true, (short) 1, null, null, 0);
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            for (int index = 0; index < mlConnectedMCAs.size(); index++) {
                Map mMCAobj = (Map) mlConnectedMCAs.get(index);
                String strMCAID = (String) mMCAobj.get(DomainConstants.SELECT_ID);
                DomainObject domMCA = new DomainObject(strMCAID);
                // TIGTK-3627:check state of MCA before promote to inwork ,in case MCA is in inwork or above no need to promote:21/11/2016:start
                String strMCAState = (String) mMCAobj.get(DomainConstants.SELECT_CURRENT);
                if (strMCAState.equalsIgnoreCase(TigerConstants.STATE_PSS_MCA_PREPARE)) {
                    domMCA.setState(context, TigerConstants.STATE_PSS_MCA_INWORK);
                }
                // TIGTK-3627:check state of MCA before promote to inwork ,in case MCA is in inwork or above no need to promote:21/11/2016:End
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteMCAsToInWorkState: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        } finally {
            if (isContextPushed == true) {
                ContextUtil.popContext(context);
            }

        }
    }

    /**
     * @param context
     * @param args
     * @throws Exception
     *             This method is called from trigger object on Route policy to promote related MCA(s) in Complete state.
     */
    public void promoteMCAToCompleteState(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            // PCM TIGTK-3032 & TIGTK-2825 | 12/09/16 : Ketaki Wagh : Start
            String strRouteId = args[0];
            String strMCAObjID = DomainConstants.EMPTY_STRING;
            String strMCAObjCurrent = DomainConstants.EMPTY_STRING;

            DomainObject domRoute = new DomainObject(strRouteId);
            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ObjectRoute");
            // String relpatternAffectedItem = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingChangeAffectedItem");
            StringList slObjectSle = new StringList(2);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            // Get Connected MCA Object
            MapList mlConnectedMCA = domRoute.getRelatedObjects(context, relpattern, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

            if (mlConnectedMCA.size() != 0) {
                for (int index = 0; index < mlConnectedMCA.size(); index++) {

                    Map mMCAobj = (Map) mlConnectedMCA.get(index);
                    strMCAObjID = (String) mMCAobj.get(DomainConstants.SELECT_ID);
                    strMCAObjCurrent = (String) mMCAobj.get(DomainConstants.SELECT_CURRENT);
                    DomainObject domMCA = new DomainObject(strMCAObjID);

                    if (strMCAObjCurrent.equalsIgnoreCase("In Review")) {

                        // Promote MCA to Complete state
                        domMCA.setState(context, "Complete");
                        // PCM TIGTK-3032 & TIGTK-2825 | 12/09/16 : Ketaki Wagh : End
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteMCAToCompleteState: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    public String getSystemDate(Context context, String[] args) throws Exception {
        SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -1);
        cal.add(Calendar.DATE, 5);
        Date date = cal.getTime();
        String strDate = _mxDateFormat.format(date);
        return strDate;
    }

    /**
     * This method used for Cancel MCO and Related MCA
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public String cancelMCOAndRelatedMCAs(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strMCOID = (String) programMap.get("objectId");
        DomainObject domCoId = new DomainObject(strMCOID);
        boolean boolMCASuccess = true;
        String strSuccessStatus = "false";
        // Promote MCO to Cancelled State.
        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
        // MqlUtil.mqlCommand(context, "trigger off");
        domCoId.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, "Cancelled");
        int intReturnMCO = domCoId.setState(context, "Cancelled");

        // MqlUtil.mqlCommand(context, "trigger on");
        ContextUtil.popContext(context);

        StringList objectSelects = new StringList(1);
        objectSelects.addElement(DomainConstants.SELECT_ID);

        // Get the connected MCA of MCO
        MapList MfgchangeActionList = domCoId.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, objectSelects,
                new StringList(0), false, true, (short) 1, "", "", (short) 0);
        for (int i = 0; i < MfgchangeActionList.size(); i++) {
            Map mapMCAObj = (Map) MfgchangeActionList.get(i);
            String strMCAID = (String) mapMCAObj.get(DomainConstants.SELECT_ID);
            DomainObject domMCAId = new DomainObject(strMCAID);

            // Promote MCA to Cancelled State.
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            MqlUtil.mqlCommand(context, "trigger off");
            int intReturnMCA = domMCAId.setState(context, "Cancelled");
            MqlUtil.mqlCommand(context, "trigger on");
            ContextUtil.popContext(context);
            if (intReturnMCA != 4) {
                boolMCASuccess = false;
            }
        }

        if (intReturnMCO == 5 && boolMCASuccess == true) {
            strSuccessStatus = "Success";
        } else {
            String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ErrorInCancellingMCO");
            MqlUtil.mqlCommand(context, "notice $1", strMessage);

        }

        return strSuccessStatus;
    }

    // PCM TIGTK-3691 | 28/11/16 :Pooja Mantri : Start
    /**
     * This method used promote MCO to Implemented when MCO is being promoted to Complete state from In review
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int promoteMCOToImplemented(Context context, String[] args) throws Exception {
        int intRetStatus = 0;
        try {
            String strMCOID = args[0];
            StringList objectSelects = new StringList(2);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_CURRENT);
            DomainObject domMCOObject = new DomainObject(strMCOID);

            boolean boolPromoteMCO = false;
            StringList slObjSelect = new StringList();
            slObjSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED + "]");
            slObjSelect.addElement(DomainConstants.SELECT_OWNER);

            Map mChangeNoticeDetails = (Map) domMCOObject.getInfo(context, slObjSelect);

            String strTransferToSAP = (String) mChangeNoticeDetails.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED + "]");
            String strMCOOwner = (String) mChangeNoticeDetails.get(DomainConstants.SELECT_OWNER);
            MapList mlChangeNoticeList = domMCOObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATEDCN, TigerConstants.TYPE_PSS_CHANGENOTICE, objectSelects, new StringList(0),
                    false, true, (short) 1, "", "", (short) 0);

            int intChangeNoticeListSize = mlChangeNoticeList.size();
            // Check attribute 'PSS_TRANSFER_TO_SAP_EXPECTED' is No
            if (("No").equalsIgnoreCase(strTransferToSAP)) {
                if (intChangeNoticeListSize == 0) {
                    // promote MCO to Implemented state
                    boolPromoteMCO = true;
                }
            } else {
                if (intChangeNoticeListSize == 0) {
                    boolPromoteMCO = false;
                } else {
                    for (int i = 0; i < intChangeNoticeListSize; i++) {
                        Map mapCNObj = (Map) mlChangeNoticeList.get(i);
                        String strCNState = (String) mapCNObj.get(DomainConstants.SELECT_CURRENT);

                        // Check current state of Change Notice
                        if (strCNState.equalsIgnoreCase(TigerConstants.STATE_FULLYINTEGRATED) || strCNState.equalsIgnoreCase(TigerConstants.STATE_NOTFULLYINTEGRATED)
                                || strCNState.equalsIgnoreCase(TigerConstants.STATE_CN_CANCELLED)) {
                            boolPromoteMCO = true;
                            // intRetStatus =1;
                        } else {
                            // promote MCO to Implemented state
                            boolPromoteMCO = false;
                        }
                    }
                }
            }
            // Promote MCO to Implemented state
            if (boolPromoteMCO == true) {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                domMCOObject.setState(context, TigerConstants.STATE_PSS_MCO_IMPLEMENTED);
                ContextUtil.popContext(context);
                domMCOObject.setOwner(context, strMCOOwner);

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteMCOToImplemented: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return intRetStatus;
    }

    // PCM TIGTK-3691 | 28/11/16 :Pooja Mantri : End

    // PCM TIGTK-3691 | 28/11/16 :Pooja Mantri : Start
    /**
     * This method called by override trigger for MCO is being promoted from Complete to Implemented state.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int promoteMCOToImplementedOverride(Context context, String[] args) throws Exception {

        int retStatus = 0;
        try {
            String strMCOID = args[0];
            boolean boolPromoteMCO = false;

            StringList objectSelects = new StringList(2);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_CURRENT);
            DomainObject domMCOObject = new DomainObject(strMCOID);

            StringList slObjSelect = new StringList();
            slObjSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED + "]");
            slObjSelect.addElement(DomainConstants.SELECT_OWNER);

            Map mChangenoticeInfo = domMCOObject.getInfo(context, slObjSelect);

            String strTransferToSAP = (String) mChangenoticeInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED + "]");
            MapList mlChangeNoticeList = domMCOObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATEDCN, TigerConstants.TYPE_PSS_CHANGENOTICE, objectSelects, new StringList(0),
                    false, true, (short) 1, "", "", (short) 0);

            int ichangeNoticeListSize = mlChangeNoticeList.size();

            // Check attribute 'PSS_Transfer_To_SAP_Expected' is No
            if (("No").equalsIgnoreCase(strTransferToSAP)) {
                if (ichangeNoticeListSize == 0) {
                    // promote MCO to Implemented state
                    boolPromoteMCO = true;

                }
            }

            // Check attribute 'PSS_Transfer_To_SAP_Expected' is Yes
            if (("Yes").equalsIgnoreCase(strTransferToSAP)) {
                if (ichangeNoticeListSize == 0) {
                    StringBuffer processStr = new StringBuffer();
                    processStr.append("JSP:postProcess");
                    processStr.append("|");
                    processStr.append("commandName=");
                    processStr.append("PSS_PromoteMCOFromJSP");
                    processStr.append("|");
                    processStr.append("objectId=");
                    processStr.append(strMCOID);
                    MqlUtil.mqlCommand(context, "notice $1", processStr.toString());
                    retStatus = 1;

                } else {
                    for (int i = 0; i < ichangeNoticeListSize; i++) {
                        Map mapCNObj = (Map) mlChangeNoticeList.get(i);
                        String strCNState = (String) mapCNObj.get(DomainConstants.SELECT_CURRENT);
                        // Check current state of Change Notice
                        if (strCNState.equalsIgnoreCase(TigerConstants.STATE_FULLYINTEGRATED) || strCNState.equalsIgnoreCase(TigerConstants.STATE_NOTFULLYINTEGRATED)
                                || strCNState.equalsIgnoreCase(TigerConstants.STATE_CN_CANCELLED)) {
                            boolPromoteMCO = true;
                        }
                    }
                }

            }

            if (boolPromoteMCO == true) {
                retStatus = 0;

            } else {
                String strMsg = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Alert.CNNotFullyIntegratedCancelledState");

                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                retStatus = 1;
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteMCOToImplementedOverride: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return retStatus;

    }

    // PCM TIGTK-3691 | 28/11/16 :Pooja Mantri : End
    /**
     * This method used promote MCO to 'In Review' state when MCA promoted is Last
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void promoteMCOIfMCAPromotedIsLast(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            String strMfgChangeOrderID = "";
            Map mMCAObj;
            String sMCAID;
            String sMCACurrent;
            HashSet<String> stateSet = new HashSet<>();

            String strMCAId = args[0];
            String STATE_INREVIEW = PropertyUtil.getSchemaProperty("policy", TigerConstants.POLICY_PSS_MANUFACTURINGCHANGEORDER, "state_InReview");

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
            slObjectSle.addElement(DomainConstants.SELECT_POLICY);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            DomainObject domMCAObj = new DomainObject(strMCAId);
            MapList mlConnectedMCOs = domMCAObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, slObjectSle,
                    slRelSle, true, false, (short) 1, null, null);

            if (mlConnectedMCOs.size() != 0) {
                for (int index = 0; index < mlConnectedMCOs.size(); index++) {
                    Map mMCOObj = (Map) mlConnectedMCOs.get(index);
                    strMfgChangeOrderID = (String) mMCOObj.get(DomainConstants.SELECT_ID);
                }
                DomainObject domMfgChangeOrder = new DomainObject(strMfgChangeOrderID);

                // Get Connected ALl MCA with MCO
                MapList mlConnectedMCAs = domMfgChangeOrder.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION,
                        slObjectSle, slRelSle, false, true, (short) 1, null, null, 0);

                if (!mlConnectedMCAs.isEmpty()) {
                    if (mlConnectedMCAs.size() == 1) {

                        for (int j = 0; j < mlConnectedMCAs.size(); j++) {
                            mMCAObj = (Map) mlConnectedMCAs.get(j);
                            sMCAID = (String) mMCAObj.get(DomainConstants.SELECT_ID);
                            if (sMCAID.equalsIgnoreCase(strMCAId)) {
                                // Modified By KWagh - TIGTK-2883 -Start
                                // Promote Mfg Change Order to In Review state
                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                domMfgChangeOrder.setState(context, STATE_INREVIEW);
                                ContextUtil.popContext(context);
                                // Modified By KWagh - TIGTK-2883 -End
                            }
                        }
                    } else {

                        for (int k = 0; k < mlConnectedMCAs.size(); k++) {
                            mMCAObj = (Map) mlConnectedMCAs.get(k);
                            sMCAID = (String) mMCAObj.get(DomainConstants.SELECT_ID);
                            sMCACurrent = (String) mMCAObj.get(DomainConstants.SELECT_CURRENT);

                            if (!(sMCAID.equalsIgnoreCase(strMCAId))) {
                                stateSet.add(sMCACurrent);
                            }
                        }
                        // Check for All Related MCA of MCO is in 'In Review' State or not
                        if (stateSet.size() > 1) {
                        } else {
                            if (stateSet.iterator().hasNext()) {
                                String sState = stateSet.iterator().next();

                                // PCM TIGTK-3837 : 28/12/2016 : AB : START
                                if (STATE_INREVIEW.equalsIgnoreCase(sState) || TigerConstants.STATE_PSS_MCA_COMPLETE.equalsIgnoreCase(sState)) {
                                    // Modified By KWagh - TIGTK-2883 -Start
                                    // Promote Mfg Change Order to In Review state
                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    domMfgChangeOrder.setState(context, STATE_INREVIEW);
                                    ContextUtil.popContext(context);
                                    // Modified By KWagh - TIGTK-2883 -End
                                }
                                // PCM TIGTK-3837 : 28/12/2016 : AB : END
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteMCOIfMCAPromotedIsLast: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * This method used promote MCA If current affected Item promoted is last
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void promoteMCAIfAffectedItemPromotedIsLast(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            // TIGTK-6191 | 11/04/2017 | Harika varanasi : Starts
            String strLoginedUser = PropertyUtil.getGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME);
            // TIGTK-6191 | 11/04/2017 | Harika varanasi : Ends
            String strMCAID;
            String strPartID;
            String strPartCurrent;
            Map mPartObj;
            String strObjId = args[0];
            HashSet<String> stateSet = new HashSet<String>();

            // 13/09/16 : Ketaki Wagh : Start : MBOM Pattern
            // String strTypeCreateAssembly = PropertyUtil.getSchemaProperty(context, "type_CreateAssembly");
            Pattern typeMBOMPattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
            typeMBOMPattern.addPattern(TigerConstants.TYPE_CREATEKIT);
            typeMBOMPattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
            typeMBOMPattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
            typeMBOMPattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

            StringList slMBOMType = new StringList();
            slMBOMType.add(TigerConstants.TYPE_CREATEASSEMBLY);
            slMBOMType.add(TigerConstants.TYPE_CREATEKIT);
            slMBOMType.add(TigerConstants.TYPE_CREATEMATERIAL);
            // 13/09/16 : Ketaki Wagh : End : MBOM Pattern

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
            slObjectSle.addElement(DomainConstants.SELECT_POLICY);
            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            DomainObject domObj = new DomainObject(strObjId);
            String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
            // String strPolicy = domObj.getInfo(context, DomainConstants.SELECT_POLICY);
            // Rutuja Ekatpure :change added for MCA cancelled and same affected item added in another MCO :14/10/2016:start
            StringList slMCAID = domObj.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].from.id");
            for (int i = 0; i < slMCAID.size(); i++) {
                strMCAID = (String) slMCAID.get(i);
                if ((!strMCAID.equalsIgnoreCase(null)) || (!strMCAID.equalsIgnoreCase(""))) {
                    DomainObject domobjMCA = new DomainObject(strMCAID);
                    String strMCAState = domobjMCA.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if ((TigerConstants.LIST_TYPE_MATERIALS.contains(strType) || slMBOMType.contains(strType)) && strMCAState.equalsIgnoreCase(TigerConstants.STATE_PSS_MCA_INWORK)) {
                        MapList mlConnectedParts = domobjMCA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, typeMBOMPattern.getPattern(), slObjectSle,
                                slRelSle, false, true, (short) 1, null, null, 0);
                        if (!mlConnectedParts.isEmpty()) {
                            if (mlConnectedParts.size() == 1) {
                                try {
                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    // TIGTK-6191 | 11/04/2017 | Harika Varanasi : Starts
                                    if (UIUtil.isNotNullAndNotEmpty(strLoginedUser)) {
                                        PropertyUtil.setGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME, strLoginedUser);
                                    }
                                    // TIGTK-6191 | 11/04/2017 | Harika Varanasi : Ends
                                    for (int j = 0; j < mlConnectedParts.size(); j++) {
                                        mPartObj = (Map) mlConnectedParts.get(j);
                                        strPartID = (String) mPartObj.get(DomainConstants.SELECT_ID);
                                        if (strPartID.equalsIgnoreCase(strObjId)) {
                                            // Promote Manufacturing Change Action to In Review state
                                            domobjMCA.promote(context);
                                        }
                                    }
                                }
                                // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: Start
                                catch (RuntimeException e) {
                                    throw e;
                                }
                                // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: End
                                catch (Exception e) {
                                    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
                                    logger.error("Error in promoteMCAIfAffectedItemPromotedIsLast: ", e);
                                    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
                                    throw e;
                                } finally {
                                    ContextUtil.popContext(context);
                                }
                            } else {
                                for (int k = 0; k < mlConnectedParts.size(); k++) {
                                    mPartObj = (Map) mlConnectedParts.get(k);
                                    strPartID = (String) mPartObj.get(DomainConstants.SELECT_ID);
                                    strPartCurrent = (String) mPartObj.get(DomainConstants.SELECT_CURRENT);
                                    if (!(strPartID.equalsIgnoreCase(strObjId))) {
                                        stateSet.add(strPartCurrent);
                                    }
                                }
                                if (stateSet.size() > 1) {
                                    // do nothing
                                } else {
                                    if (stateSet.iterator().hasNext()) {
                                        String sState = stateSet.iterator().next();
                                        if (TigerConstants.STATE_PART_REVIEW.equalsIgnoreCase(sState)) {
                                            // Promote Manufacturing Change Action to In Review state
                                            try {
                                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                                // TIGTK-6191 | 11/04/2017 | Harika Varanasi : Starts
                                                if (UIUtil.isNotNullAndNotEmpty(strLoginedUser)) {
                                                    PropertyUtil.setGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME, strLoginedUser);
                                                }
                                                // TIGTK-6191 | 11/04/2017 | Harika Varanasi : Ends
                                                domobjMCA.promote(context);
                                            } catch (Exception e) {
                                                // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
                                                logger.error("Error in promoteMCAIfAffectedItemPromotedIsLast: ", e);
                                                // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
                                                throw e;
                                            } finally {
                                                ContextUtil.popContext(context);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Do Nothing
                }
            }
            // Rutuja Ekatpure :change added for MCA cancelled and same affected item added in another MCO :14/10/2016:End
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteMCAIfAffectedItemPromotedIsLast: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * This method used for Start Approval route for MCA
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void startApprovalRouteForMCA(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            // TIGTK-6191 | 11/04/2017 | Harika Varanasi : Starts
            String strLoginedUser = PropertyUtil.getGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME);
            // TIGTK-6191 | 11/04/2017 | Harika Varanasi : Ends
            String strMCAObjID = args[0];
            DomainObject domMCA = DomainObject.newInstance(context, strMCAObjID);

            // Connected Route Objects
            StringList slObjectSle = new StringList(DomainConstants.SELECT_ID);
            StringList slRelSle = new StringList(DomainRelationship.SELECT_RELATIONSHIP_ID);
            String strRouteID = DomainConstants.EMPTY_STRING;
            MapList mlObjConnected = domMCA.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, slObjectSle, slRelSle, false, true, (short) 1, null, null,
                    0);

            if (mlObjConnected.size() > 0) {
                for (int i = 0; i < mlObjConnected.size(); i++) {
                    Map mRouteObj = (Map) mlObjConnected.get(i);
                    strRouteID = (String) mRouteObj.get(DomainConstants.SELECT_ID);
                    DomainObject objRoute = new DomainObject(strRouteID);
                    String strRouteStatus = objRoute.getAttributeValue(context, "Route Status");
                    String sRouteBaseState = objRoute.getInfo(context, "to[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASESTATE + "]");

                    if ("state_InReview".equals(sRouteBaseState) && strRouteStatus.equalsIgnoreCase("Stopped")) {
                        Route route = new Route(strRouteID);
                        // Restarting the already connected Route
                        route.resume(context);
                        // set Route Due Date
                        this.setDueDateOnMCARoute(context, strRouteID);
                    } else {
                        try {
                            // set Route Due Date
                            this.setDueDateOnMCARoute(context, strRouteID);
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            // TIGTK-6191 | 11/04/2017 | Harika Varanasi : Starts
                            if (UIUtil.isNotNullAndNotEmpty(strLoginedUser)) {
                                PropertyUtil.setGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME, strLoginedUser);
                            }
                            // TIGTK-6191 | 11/04/2017 | Harika Varanasi : Ends
                            objRoute.setAttributeValue(context, "Route Status", "Started");
                            objRoute.setState(context, "In Process");
                        } catch (FrameworkException e) {
                            logger.error("Error in startApprovalRouteForMCA: ", e);
                            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                    "EnterpriseChangeMgt.Alert.RouteFailureMessage");
                            e.addMessage(strAlertMessage);
                            throw e;
                            // PCM : TIGTK-10010 : 21/09/2017 : AB : END
                        } finally {
                            ContextUtil.popContext(context);
                        }
                    }
                }
            } else {
                // PCM : TIGTK-10010 : 21/09/2017 : AB :START
                // Create New Route between State 'In Review' & 'Complete' state And After that Start The Route
                StringList slObjectSelectsMCA = new StringList(DomainConstants.SELECT_OWNER);
                slObjectSelectsMCA.add("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.id");
                String strMCOID = DomainConstants.EMPTY_STRING;
                String strMCAOwner = DomainConstants.EMPTY_STRING;

                Map mapMCAInfo = domMCA.getInfo(context, slObjectSelectsMCA);
                if (!mapMCAInfo.isEmpty()) {
                    strMCOID = (String) mapMCAInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.id");
                    strMCAOwner = (String) mapMCAInfo.get(DomainConstants.SELECT_OWNER);
                }

                // Create new Route on MCA
                if (UIUtil.isNotNullAndNotEmpty(strMCOID) && UIUtil.isNotNullAndNotEmpty(strMCAObjID)) {
                    String[] starArgs = new String[2];
                    starArgs[0] = strMCOID;
                    starArgs[1] = strMCAObjID;
                    this.createRouteOnPrepareState(context, starArgs);
                }

                // Now get Connected Route of MCA
                MapList mlConnectedRouteOfMCA = domMCA.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, slObjectSle, slRelSle, false, true, (short) 1,
                        null, null, 0);

                if (mlConnectedRouteOfMCA.size() > 0) {
                    for (int i = 0; i < mlConnectedRouteOfMCA.size(); i++) {
                        Map mapRoute = (Map) mlConnectedRouteOfMCA.get(i);
                        strRouteID = (String) mapRoute.get(DomainConstants.SELECT_ID);
                        DomainObject domRoute = new DomainObject(strRouteID);

                        // set Route Due Date
                        this.setDueDateOnMCARoute(context, strRouteID);

                        // Change the owner of Route to context user, so that user can start the Route
                        try {
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            String strRouteOwner = DomainConstants.EMPTY_STRING;

                            if (UIUtil.isNotNullAndNotEmpty(strLoginedUser)) {
                                strRouteOwner = strLoginedUser;
                            } else {
                                strRouteOwner = context.getUser();
                            }
                            domRoute.setOwner(context, strRouteOwner);
                        } catch (Exception e) {
                            logger.error("Error in startApprovalRouteForMCA: ", e);
                            throw e;
                        } finally {
                            ContextUtil.popContext(context);
                        }

                        try {
                            // Start newly created Route
                            if (UIUtil.isNotNullAndNotEmpty(strLoginedUser)) {
                                PropertyUtil.setGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME, strLoginedUser);
                            }
                            domRoute.setAttributeValue(context, "Route Status", "Started");
                            domRoute.setState(context, "In Process");
                        } catch (FrameworkException e) {
                            logger.error("Error in startApprovalRouteForMCA: ", e);
                            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                    "EnterpriseChangeMgt.Alert.RouteFailureMessage");
                            e.addMessage(strAlertMessage);
                            throw e;
                        }

                        // set the original owner of Route
                        try {
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            domRoute.setOwner(context, strMCAOwner);
                        } catch (Exception e) {
                            logger.error("Error in startApprovalRouteForMCA: ", e);
                            throw e;
                        } finally {
                            ContextUtil.popContext(context);
                        }

                    }
                }
            }
            // PCM : TIGTK-10010 : 21/09/2017 : AB : END
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in startApprovalRouteForMCA: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * This method is used to get Table data in MCO mFG Affected Item Tab
     * @param context
     * @param args
     * @throws Exception
     */
    public MapList getMCOAffectedItems(Context context, String[] args) throws Exception {
        try {
            MapList mlTableData = new MapList();
            String strMCAID;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            // HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strMCOId = (String) programMap.get("objectId");

            DomainObject domMCO = new DomainObject(strMCOId);

            StringList objectSelects = new StringList(6);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_TYPE);
            objectSelects.addElement(DomainConstants.SELECT_NAME);
            StringList relSelects = new StringList(6);
            relSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            // Get MCA Objects related to MCO
            MapList mlMCAList = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, "*", objectSelects, relSelects, false, true, (short) 0, "", "", (short) 0);

            if (!mlMCAList.isEmpty()) {

                for (int j = 0; j < mlMCAList.size(); j++) {
                    Map mMCA = (Map) mlMCAList.get(j);
                    strMCAID = (String) mMCA.get(DomainConstants.SELECT_ID);
                    DomainObject domMCA = new DomainObject(strMCAID);

                    // Get Affected Items related MCO
                    MapList mlAffectedItemList = domMCA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, "*", objectSelects, relSelects, false, true,
                            (short) 0, "", "", (short) 0);

                    if (!mlAffectedItemList.isEmpty()) {
                        for (int k = 0; k < mlAffectedItemList.size(); k++) {
                            Map mAffectedItemObj = (Map) mlAffectedItemList.get(k);
                            mAffectedItemObj.put("relatedCAId", (String) mMCA.get(DomainConstants.SELECT_ID));
                            mAffectedItemObj.put("relatedCAName", (String) mMCA.get(DomainConstants.SELECT_NAME));
                            mAffectedItemObj.put("relatedCAType", (String) mMCA.get(DomainConstants.SELECT_TYPE));
                            mlTableData.add(mAffectedItemObj);

                        }
                    }
                }
            }
            return mlTableData;

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMCOAffectedItems: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    /**
     * Method called on the post process functionality of "PSS_ManufacturingChangeOrder" edit command. It makes a check if "PSS_ManufacturingChangeOrder" object is connected to any "PSS_ChangeNotice"
     * object. If YES it sets " PSS_Transfer_To_SAP_Expected" attribute value to Yes. Modified For TIGTK-3703 by AB
     * @param context
     * @param args
     * @return void -- Nothing
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public void setTransferToSAPValue(Context context, String[] args) throws Exception {
        try {
            // PCM TIGTK-3041 | 08/09/Y16 : kwagh
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String strMCOObjectId = (String) paramMap.get("objectId");

            // Creating Domain Object Instance
            DomainObject domMCOObject = DomainObject.newInstance(context, strMCOObjectId);

            String strPSSTrasferToSAPExpectedValue = domMCOObject.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED + "]");

            // Get Connected "PSS Change Notice" objects
            StringList lstSelectList = new StringList();
            lstSelectList.add(DomainConstants.SELECT_ID);
            lstSelectList.add(DomainConstants.SELECT_CURRENT);
            String where = "current!=" + TigerConstants.STATE_CN_CANCELLED;
            MapList mlConnectedCNList = domMCOObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATEDCN, TigerConstants.TYPE_PSS_CHANGENOTICE, lstSelectList, null, false, true,
                    (short) 0, where, null, 0);

            // Change "Transfer To SAP Expected" to Yes if There was no CN connected with MCO and current value of attribute is NO
            if (!mlConnectedCNList.isEmpty() && mlConnectedCNList.size() > 0) {
                if (!strPSSTrasferToSAPExpectedValue.equalsIgnoreCase("Yes")) {
                    domMCOObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED, "Yes");
                }
            }
            // TIGTK-13112 : 07-02-2018 : START
            String strProgProjId = (String) paramMap.get("ProjectcodeOID");
            if (UIUtil.isNotNullAndNotEmpty(strProgProjId)) {
                BusinessObject busProgProjObj = new BusinessObject(strProgProjId);
                User usrProgProjProjectName = busProgProjObj.getProjectOwner(context);
                String strProgProjProjectName = usrProgProjProjectName.toString();
                User usrChangeProjectName = domMCOObject.getProjectOwner(context);
                String strChangeObjectProjectName = usrChangeProjectName.toString();
                if (!strProgProjProjectName.equalsIgnoreCase(strChangeObjectProjectName)) {
                    MqlUtil.mqlCommand(context, "history off", true, false);
                    boolean isContextPushed = false;
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    isContextPushed = true;
                    domMCOObject.open(context);
                    domMCOObject.setProjectOwner(context, strProgProjProjectName);
                    domMCOObject.update(context);
                    domMCOObject.close(context);
                    if (isContextPushed) {
                        ContextUtil.popContext(context);
                        isContextPushed = false;
                    }
                    MqlUtil.mqlCommand(context, "history on", true, false);

                    String strMqlHistory = "modify bus $1 add history $2 comment $3";

                    StringBuffer sbInfo = new StringBuffer();
                    sbInfo.append("project: ");
                    sbInfo.append(strProgProjProjectName);

                    MqlUtil.mqlCommand(context, strMqlHistory, strMCOObjectId, "change", sbInfo.toString() + " was " + strChangeObjectProjectName);

                }
            }
            // TIGTK-13112 : 07-02-2018 : END
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setTransferToSAPValue: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    /**
     * Method to get the "Route Template" name connected to "PSS Program Project" object
     * @param context
     * @param args
     *            - ObjectId of "Mfg Change Action"
     * @return -- String -- "Route Template" object Name
     * @throws Exception
     */
    public String getRouteTemplate(Context context, String objectId) throws Exception {
        String strRouteTemplateName = "";
        String strPSSRouteTemplateTypeValue = "";
        StringList busSelect = new StringList();
        try {
            // Range Constants for attribute "PSS_RouteTemplateType"
            final String RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATE = "Approval List for Commercial update";
            final String RANGE_APPROVAL_LIST_FORPROTOTYPE = "Approval List for Prototype";
            final String RANGE_APPROVAL_LIST_FORSERIALLAUNCH = "Approval List for Serial Launch";
            final String RANGE_APPROVAL_LIST_FORDESIGNSTUDY = "Approval List for Design study";
            final String RANGE_DESIGN_STUDY = "Design study";
            final String RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION = "Serial Tool Launch/Modification";
            final String RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION = "Prototype Tool Launch/Modification";
            final String RANGE_COMMERCIAL_UPDATE = "Commercial Update";

            // Domain Object Instance
            DomainObject domMCAObject = DomainObject.newInstance(context, objectId);

            // Get connected Manufacturing ChangeOrder objects with ChangeAction
            StringList objectSelects = new StringList(1);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            StringList relSelects = new StringList(1);
            relSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            MapList mfgchangeOrderList = domMCAObject.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, // relationship pattern
                    TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, // object pattern
                    objectSelects, // object selects
                    relSelects, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    "", // object where clause
                    "", // relationship where clause
                    (short) 0);

            // Get Manufacturing ChangeOrder's attribute(PSS_Purpose_Of_Release) value.
            if (mfgchangeOrderList.size() != 0) {
                for (int m = 0; m < mfgchangeOrderList.size(); m++) {
                    Map mMCOObj = (Map) mfgchangeOrderList.get(m);
                    String strMCOId = (String) mMCOObj.get(DomainConstants.SELECT_ID);
                    DomainObject domMCO = new DomainObject(strMCOId);
                    String strPurposeOfRelease = domMCO.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);

                    // Get the Connected "PSS Program Project" to "PSS Mfg Change Order" with "PSS_ConnectedPCMData" relationship
                    String strProgramProjectOID = (String) domMCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

                    // Create Mapping map for "PSS Program Project" object
                    Map programProjectMap = new HashMap<>();
                    programProjectMap.put(RANGE_COMMERCIAL_UPDATE, RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATE);
                    programProjectMap.put(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION, RANGE_APPROVAL_LIST_FORPROTOTYPE);
                    programProjectMap.put(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION, RANGE_APPROVAL_LIST_FORSERIALLAUNCH);
                    programProjectMap.put(RANGE_DESIGN_STUDY, RANGE_APPROVAL_LIST_FORDESIGNSTUDY);
                    // programProjectMap.put(RANGE_OTHER,RANGE_APPROVAL_LIST_FOROTHERITEMS);

                    if (strPurposeOfRelease.equals(RANGE_DESIGN_STUDY)) {
                        strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_DESIGN_STUDY);
                    } else if (strPurposeOfRelease.equals(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION)) {
                        strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION);
                    } else if (strPurposeOfRelease.equals(RANGE_COMMERCIAL_UPDATE)) {
                        strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_COMMERCIAL_UPDATE);
                    } else if (strPurposeOfRelease.equals(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION)) {
                        strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION);
                    } else {
                        strRouteTemplateName = "Approval List for Other Route Template";
                    }

                    // Creating "PSS Program Project" Object Instance
                    DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);

                    busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                            + strPSSRouteTemplateTypeValue + "'].to.name");
                    Map mapRouteTemplateDetails = domProgramProjectObject.getInfo(context, busSelect);
                    strRouteTemplateName = (String) mapRouteTemplateDetails.get("from[PSS_ConnectedRouteTemplates].to.name");

                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getRouteTemplate: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return strRouteTemplateName;
    }

    /**
     * Method called to update "PSS_PlannedEndDate" and "MCA Assignee". This method is called on relationship "PSS_ManufacturingChangeAction" trigger.
     * @param context
     * @param args
     *            -- args0 -- "PSS_ManufacturingChangeOrder" Object Id -- args1 -- "PSS_ManufacturingChangeAction" Object Id
     * @return void -- Nothing
     * @throws Exception
     */
    public void updateMCAProperties(Context context, String args[]) throws Exception {
        try {
            String strFromPSSMfgChangeOrderObjectId = args[0];
            String strToPSSMfgChangeActionObjectId = args[1];

            // Creating DomainObject Instance of "PSS_ManufacturingChangeOrder" Object
            DomainObject domPSSMfgChangeOrderObject = DomainObject.newInstance(context, strFromPSSMfgChangeOrderObjectId);

            // Creating DomainObject Instance of "PSS_ManufacturingChangeAction" Object
            DomainObject domPSSMfgChangeActionObject = DomainObject.newInstance(context, strToPSSMfgChangeActionObjectId);

            // Defining SelectList
            StringList lstSelectList = new StringList();
            lstSelectList.add(DomainConstants.SELECT_TYPE);
            lstSelectList.add(DomainConstants.SELECT_ORIGINATOR);
            lstSelectList.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PHYSICAL_IMPLEMENTATION_PLANNED_DATE + "]");

            // Get Type of "PSS_ManufacturingChangeOrder" Object
            Map mapPSSMfgChangeOrderInfo = domPSSMfgChangeOrderObject.getInfo(context, lstSelectList);
            String strPSSMfgChangeOrderType = (String) mapPSSMfgChangeOrderInfo.get(DomainConstants.SELECT_TYPE);
            String strPSSMfgChangeOrderCreator = (String) mapPSSMfgChangeOrderInfo.get(DomainConstants.SELECT_ORIGINATOR);
            String strPSSMfgChangeOrderPhyImplDate = (String) mapPSSMfgChangeOrderInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PHYSICAL_IMPLEMENTATION_PLANNED_DATE + "]");

            StringList busSelects = new StringList();
            busSelects.add(DomainConstants.SELECT_ID);

            Date date7DaysBefore = null;
            // String strPlannedEndDate = _mxDateFormat.format(date5DaysBefore);

            Date dtPSSMfgChangeOrderPhyImplDate = new Date(strPSSMfgChangeOrderPhyImplDate);

            long DAY_IN_MS = 1000 * 60 * 60 * 24;
            new Date(System.currentTimeMillis() - (7 * DAY_IN_MS));
            date7DaysBefore = new Date(dtPSSMfgChangeOrderPhyImplDate.getTime() - (7 * DAY_IN_MS));
            // TIGTK-14777 : START
            Date date = new Date();
            if (date7DaysBefore.before(date)) {
                date7DaysBefore = dtPSSMfgChangeOrderPhyImplDate;
            }
            // TIGTK-14777 : END

            String strPlannedEndDate = DateFormatUtil.formatDate(context, date7DaysBefore);

            // Get the Current Assignee for "PSS Mfg Change Action" Object
            String strPSSMfgChangeActionAssigneeName = domPSSMfgChangeOrderObject.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.name");

            if (strPSSMfgChangeOrderType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                domPSSMfgChangeActionObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PLANNEDENDDATE, strPlannedEndDate);
                ContextUtil.popContext(context);

                if (UIUtil.isNullOrEmpty(strPSSMfgChangeActionAssigneeName) || (!strPSSMfgChangeActionAssigneeName.equalsIgnoreCase(strPSSMfgChangeOrderCreator))) {
                    String whereExpression = "";
                    String strPersonId = "";
                    // Added for error found by find bug : 08/11/2016 : START
                    if (UIUtil.isNotNullAndNotEmpty(strPSSMfgChangeOrderCreator))
                        // Added for error found by find bug : 08/11/2016 : END
                        whereExpression = "name == '" + strPSSMfgChangeOrderCreator + "'";

                    String strTypePerson = PropertyUtil.getSchemaProperty(context, "type_Person");

                    MapList mpList = DomainObject.findObjects(context, strTypePerson, TigerConstants.VAULT_ESERVICEPRODUCTION, whereExpression, busSelects);
                    for (int index = 0; index < mpList.size(); index++) {
                        Map mPersonObj = (Map) mpList.get(index);
                        strPersonId = (String) mPersonObj.get(DomainConstants.SELECT_ID);

                        DomainObject objPerson = new DomainObject(strPersonId);
                        domPSSMfgChangeActionObject.connect(context, ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE, objPerson, false);
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in updateMCAProperties: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * owner: Sneha TS-122 This method is used to create route in between Prepare and In Work state when we add affected Item. Modified by :KWagh TIGTK-2839
     * @param context
     * @param args
     * @throws Exception
     */

    public int createRouteOnPrepareState(Context context, String[] args) throws Exception {
        String strRouteId = "";
        // String strRouteTemplateName = "";
        String strPSSRouteTemplateTypeValue = "";
        StringList busSelect = new StringList();
        try {

            // MCO Objects ID
            String strMCOID = args[0];
            // MCA Objects ID
            String strMCAID = args[1];

            DomainObject domMCOObject = DomainObject.newInstance(context, strMCOID);
            String strMCAOwner = domMCOObject.getInfo(context, DomainConstants.SELECT_OWNER);
            String strPurposeOfRelease = domMCOObject.getAttributeValue(context, "PSS_Purpose_Of_Release");
            // Get the Connected "PSS Program Project" to "PSS Mfg Change Order" with "PSS_ConnectedPCMData" relationship
            String strProgramProjectOID = (String) domMCOObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            // Create Mapping map for "PSS Program Project" object
            Map programProjectMap = new HashMap<>();
            programProjectMap.put(TigerConstants.RANGE_COMMERCIAL_UPDATE, TigerConstants.RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONMCO);
            programProjectMap.put(TigerConstants.RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION, TigerConstants.RANGE_APPROVAL_LIST_FORPROTOTYPEONMCO);
            programProjectMap.put(TigerConstants.RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION, TigerConstants.RANGE_APPROVAL_LIST_FORSERIALLAUNCHONMCO);
            programProjectMap.put(TigerConstants.RANGE_DESIGN_STUDY, TigerConstants.RANGE_APPROVAL_LIST_FORDESIGNSTUDYONMCO);
            programProjectMap.put(TigerConstants.RANGE_Acquisition, TigerConstants.RANGE_APPROVAL_LIST_FORAcquisitionONMCO);
            // PCM TIGTK-3482/1 | 10/11/16 : Pooja Mantri : Start
            programProjectMap.put(TigerConstants.RANGE_OTHER, TigerConstants.RANGE_APPROVAL_LIST_FOROTHERPARTSONMCO);

            if (strPurposeOfRelease.equals(TigerConstants.RANGE_DESIGN_STUDY)) {

                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_DESIGN_STUDY);
            } else if (strPurposeOfRelease.equals(TigerConstants.RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION)) {
                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION);
            } else if (strPurposeOfRelease.equals(TigerConstants.RANGE_COMMERCIAL_UPDATE)) {
                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_COMMERCIAL_UPDATE);
            } else if (strPurposeOfRelease.equals(TigerConstants.RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION)) {
                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION);
            } else if (strPurposeOfRelease.equals(TigerConstants.RANGE_Acquisition)) {
                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_Acquisition);
            } else {
                strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_OTHER);
            }

            // Creating "PSS Program Project" Object Instance
            DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);

            busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='" + strPSSRouteTemplateTypeValue
                    + "'].to.name");

            busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='" + strPSSRouteTemplateTypeValue
                    + "'].to.id");

            Map mapRouteTemplateDetails = domProgramProjectObject.getInfo(context, busSelect);

            String strRouteTemplateId = (String) mapRouteTemplateDetails.get("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.id");

            if (UIUtil.isNotNullAndNotEmpty(strRouteTemplateId)) {

                DomainObject RouteTemplate = new DomainObject(strRouteTemplateId);
                String strRouteBasePurpose = RouteTemplate.getAttributeValue(context, "Route Base Purpose");

                Route routeObject = (Route) DomainObject.newInstance(context, DomainConstants.TYPE_ROUTE);
                String strName = DomainObject.getAutoGeneratedName(context, DomainSymbolicConstants.SYMBOLIC_type_Route, "");

                routeObject.createObject(context, DomainConstants.TYPE_ROUTE, strName, "1", DomainConstants.POLICY_ROUTE, TigerConstants.VAULT_ESERVICEPRODUCTION);

                strRouteId = (String) routeObject.getInfo(context, DomainConstants.SELECT_ID);

                DomainRelationship dRel = routeObject.addRelatedObject(context, new RelationshipType(DomainConstants.RELATIONSHIP_OBJECT_ROUTE), true, strMCAID);

                Map<String, String> mapAttribute = new HashMap<String, String>();
                mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_POLICY, "policy_PSS_ManufacturingChangeAction");
                mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE, "state_InReview");
                mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE, strRouteBasePurpose);
                mapAttribute.put("Auto Stop On Rejection", "Immediate");
                dRel.setAttributeValues(context, mapAttribute);

                routeObject.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE, strRouteBasePurpose);
                // If the route template id is not null then connect the route to the route template
                if (strRouteTemplateId != null && !strRouteTemplateId.equalsIgnoreCase("null") && !strRouteTemplateId.equalsIgnoreCase(DomainConstants.EMPTY_STRING)) {
                    routeObject.connectTemplate(context, strRouteTemplateId);
                    routeObject.addMembersFromTemplate(context, strRouteTemplateId);
                }
                String strRelId = "";

                Pattern typePattern = new Pattern(DomainConstants.TYPE_ROUTE_TASK_USER);
                typePattern.addPattern(DomainConstants.TYPE_PERSON);

                StringList slBusSelect = new StringList();

                StringList slRelSelect = new StringList(1);
                slRelSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                DomainObject domObjRoute = new DomainObject(strRouteId);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                domObjRoute.setOwner(context, strMCAOwner);
                ContextUtil.popContext(context);

                MapList mapListRouteNodeRel = domObjRoute.getRelatedObjects(context, DomainObject.RELATIONSHIP_ROUTE_NODE, typePattern.getPattern(), slBusSelect, slRelSelect, false, true, (short) 1,
                        null, null);

                if (mapListRouteNodeRel != null && !mapListRouteNodeRel.isEmpty()) {
                    for (int i = 0; i < mapListRouteNodeRel.size(); i++) {

                        Map connectIdMap = (Map) mapListRouteNodeRel.get(i);
                        strRelId = (String) connectIdMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                        // Creating relationship object.
                        DomainRelationship domRelRouteNode = DomainRelationship.newInstance(context, strRelId);

                        // setting attibute (scheduled completion date) values for that relationship.
                        // TIGTK_3625:merge MCA create issue :Rutuja Ekatpure:start
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        domRelRouteNode.setAttributeValue(context, DomainConstants.ATTRIBUTE_DUEDATE_OFFSET, "7");
                        domRelRouteNode.setAttributeValue(context, DomainConstants.ATTRIBUTE_DATE_OFFSET_FROM, "Route Start Date");
                        domRelRouteNode.setAttributeValue(context, DomainConstants.ATTRIBUTE_ASSIGNEE_SET_DUEDATE, "No");

                        ContextUtil.popContext(context);
                        // TIGTK_3625:merge MCA create issue :Rutuja Ekatpure:End
                    }
                }
            }

            // PCM TIGTK-3482 | 08/11/16 : Pooja Mantri : End

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in createRouteOnPrepareState: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;

        }

        return 0;

    }

    /**
     * this method will check for connected affected item state for promotion of MCA OWNER : Krishna TS-081
     * @param context
     * @param args
     * @throws Exception
     */
    public void promoteMCAtoInReview(Context context, String[] args) throws Exception {

        // 13/09/16 : Ketaki Wagh : Start : MBOM Pattern
        // String TYPE_CREATEASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_CreateAssembly");

        Pattern typeMBOMPattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
        typeMBOMPattern.addPattern(TigerConstants.TYPE_CREATEKIT);
        typeMBOMPattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
        typeMBOMPattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
        typeMBOMPattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

        StringList slMBOMType = new StringList();
        slMBOMType.add(TigerConstants.TYPE_CREATEASSEMBLY);
        slMBOMType.add(TigerConstants.TYPE_CREATEKIT);
        slMBOMType.add(TigerConstants.TYPE_CREATEMATERIAL);

        try {
            StringList slObjectSle = new StringList();
            slObjectSle.add(DomainConstants.SELECT_ID);
            slObjectSle.add(DomainConstants.SELECT_CURRENT);
            String strObjId = args[0];
            DomainObject domObj = new DomainObject(strObjId);
            String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
            // String strPolicy = domObj.getInfo(context, DomainConstants.SELECT_POLICY);
            // Map mPartObj = new HashMap();
            String strPartID = "";
            String strPartCurrent = "";
            HashSet<String> stateSet = new HashSet<String>();
            // Rutuja Ekatpure :change added for MCA cancelled and same affected item added in another MCO :14/10/2016:start
            StringList slMCAID = domObj.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].from.id");
            String strCAIDChangeAffectedItem = "";
            for (int i = 0; i < slMCAID.size(); i++) {
                strCAIDChangeAffectedItem = (String) slMCAID.get(i);
                if (strCAIDChangeAffectedItem != null && !strCAIDChangeAffectedItem.isEmpty()) {

                    DomainObject domobjCA = new DomainObject(strCAIDChangeAffectedItem);
                    String strMCAState = domobjCA.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if ((TigerConstants.LIST_TYPE_MATERIALS.contains(strType) || slMBOMType.contains(strType)) && strMCAState.equalsIgnoreCase(TigerConstants.STATE_PSS_MCA_INWORK)) {
                        MapList mlConnectedParts = domobjCA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, typeMBOMPattern.getPattern(), slObjectSle,
                                new StringList(), false, true, (short) 1, null, null, 0);
                        // 13/09/16 : Ketaki Wagh : End : MBOM Pattern
                        if (!mlConnectedParts.isEmpty()) {
                            // Findbug Issue correction start
                            // Date: 21/03/2017
                            // By: Asha G.
                            Map mPartObj = null;
                            // Findbug Issue correction end
                            if (mlConnectedParts.size() == 1) {
                                for (int j = 0; j < mlConnectedParts.size(); j++) {
                                    mPartObj = (Map) mlConnectedParts.get(j);
                                    strPartID = (String) mPartObj.get(DomainConstants.SELECT_ID);
                                    if (strPartID.equalsIgnoreCase(strObjId)) {

                                        // Promote MFG Change Action to In Review state
                                        domobjCA.setState(context, TigerConstants.STATE_PSS_MCA_INREVIEW);
                                    }
                                }
                            } else {
                                for (int k = 0; k < mlConnectedParts.size(); k++) {
                                    mPartObj = (Map) mlConnectedParts.get(k);
                                    strPartID = (String) mPartObj.get(DomainConstants.SELECT_ID);
                                    strPartCurrent = (String) mPartObj.get(DomainConstants.SELECT_CURRENT);

                                    if (!(strPartID.equalsIgnoreCase(strObjId))) {

                                        stateSet.add(strPartCurrent);
                                    }
                                }

                                if (stateSet.size() > 1) {

                                    // do nothing

                                } else {
                                    if (stateSet.iterator().hasNext()) {
                                        String sState = stateSet.iterator().next();
                                        if (TigerConstants.STATE_PSS_MBOM_REVIEW.equalsIgnoreCase(sState)) {
                                            // Promote Change Action to In Review state
                                            domobjCA.setState(context, TigerConstants.STATE_PSS_MCA_INREVIEW);
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
            // Rutuja Ekatpure :change added for MCA cancelled and same affected item added in another MCO :14/10/2016:End

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteMCAtoInReview: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    /**
     * Added this function to get Connect The CO and CR to MCO
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @owner: Vishal B.
     */
    // TIGTK-9998| 20/09/17 : Start
    public void connectRelatedCO(Context context, String[] args) throws Exception {

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        // TIGTK-10055| 20/09/17 : Start
        String MCOobjectId = (String) paramMap.get("objectId");
        // TIGTK-10055| 20/09/17 : End
        DomainObject domMCO = new DomainObject(MCOobjectId);
        StringList slSelectedCOIds = (StringList) paramMap.get("slObjectIds");

        HashSet hsCRIDs = new HashSet<>();

        try {

            StringList slConnectedCRIDs = getConnectedChnageRequest(context, domMCO);

            RelationshipType relType = new RelationshipType(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER);

            if (!slSelectedCOIds.isEmpty()) {

                Iterator itrCOId = slSelectedCOIds.iterator();
                while (itrCOId.hasNext()) {

                    String strCOId = (String) itrCOId.next();
                    DomainObject domCO = new DomainObject(strCOId);
                    // TIGTK-10084: Start
                    StringList slCOConnectedToMCO = domMCO.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].from.id");
                    if (!slCOConnectedToMCO.contains(strCOId)) {
                        domMCO.addRelatedObject(context, relType, true, strCOId);
                    }
                    // TIGTK-10084 : End

                    StringList strCRConnectedToCO = domCO.getInfoList(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ORDER + "].from.id");

                    if (!strCRConnectedToCO.isEmpty()) {

                        Iterator itrCRIDs = strCRConnectedToCO.iterator();
                        while (itrCRIDs.hasNext()) {

                            String strCRID = (String) itrCRIDs.next();
                            if (!slConnectedCRIDs.contains(strCRID) && !hsCRIDs.contains(strCRID)) {
                                hsCRIDs.add(strCRID);
                                domMCO.addRelatedObject(context, relType, true, strCRID);
                            }

                        }

                    }

                }

            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectRelatedCO: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    // TIGTK-9998| 20/09/17 : End

    // TIGTK-9998| 20/09/17 : Start
    public StringList getConnectedChnageRequest(Context context, DomainObject domMCO) throws FrameworkException {

        StringList slConnectedCRIds = new StringList();
        try {
            StringList slObjSel = new StringList();
            slObjSel.addElement(DomainConstants.SELECT_ID);
            Pattern typepattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

            MapList mlChangeRequestList = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, typepattern.getPattern(), slObjSel,
                    DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, null, null, 0);

            Iterator itrCRIds = mlChangeRequestList.iterator();
            while (itrCRIds.hasNext()) {
                Map mCRID = (Map) itrCRIds.next();
                String strCRId = (String) mCRID.get(DomainConstants.SELECT_ID);
                slConnectedCRIds.add(strCRId);
            }
        } catch (Exception ex) {
            logger.error("Error in getConnectedChnageRequest: ", ex);
        }
        return slConnectedCRIds;
    }

    // TIGTK-9998| 20/09/17 : End
    // TIGTK-10184 : 27/6/17 :Start
    /**
     * Added this function to get Connect The CO and CR to existing MCO from CO
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void connectRelatedCRtoExistingMCO(Context context, String[] args) throws Exception {
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String COobjectId = (String) paramMap.get("objectId");
            StringList slObjectIds = (StringList) paramMap.get("slObjectIds");

            int listSize = slObjectIds.size();
            for (int i = 0; i < listSize; i++) {
                StringList slCOObjectIds = new StringList();
                slCOObjectIds.add(COobjectId);
                String MCOobjectId = (String) slObjectIds.get(i);
                Map paramMapForJPO = new HashMap();
                paramMapForJPO.put("objectId", MCOobjectId);
                paramMapForJPO.put("slObjectIds", slCOObjectIds);
                String[] arg = JPO.packArgs(paramMapForJPO);
                connectRelatedCO(context, arg);
            }
        } catch (Exception ex) {
            logger.error("Error in connectRelatedCRtoExistingMCO: ", ex);
            throw ex;
        }

    }

    /**
     * Added this function to get Connect The CO and CR to Newly Created MCO from CO
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void connectRelatedCRtoNewCreatedMCO(Context context, String[] args) throws Exception {
        try {
            HashMap param = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) param.get("paramMap");
            String objMCOId = (String) paramMap.get("newObjectId");
            DomainObject domMCO = new DomainObject(objMCOId);
            StringList slCOObjectIds = (StringList) domMCO.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].from.id");
            Map paramMapForJPO = new HashMap();
            paramMapForJPO.put("objectId", objMCOId);
            paramMapForJPO.put("slObjectIds", slCOObjectIds);
            String[] arg = JPO.packArgs(paramMapForJPO);
            connectRelatedCO(context, arg);
        } catch (Exception ex) {
            logger.error("Error in connectRelatedCRtoNewCreatedMCO: ", ex);
            throw ex;
        }

    }
    // TIGTK-10184 : 27/6/17 :END

    /**
     * Added this function to get ExcludedOIds. this StringList returns list for Exclude ids
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    public static Object excludeConnectedObjects(Context context, String[] args) throws Exception {
        StringList excludeOID = new StringList();
        try {

            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) paramMap.get("objectId");
            String strRelationship = (String) paramMap.get("srcDestRelName");
            String strFieldtype = (String) paramMap.get("field_actual");
            strFieldtype = strFieldtype.replace("TYPES=", "");
            strFieldtype = strFieldtype.trim();

            StringTokenizer strToken = new StringTokenizer(strFieldtype, ",");
            int i = 0;
            String strTypeArr[] = new String[strToken.countTokens()];
            StringBuffer strtype = new StringBuffer();
            int k = strToken.countTokens();
            while (strToken.hasMoreTokens()) {
                strTypeArr[i] = strToken.nextToken();
                String[] temp = strTypeArr[i].split(":");

                strtype.append(PropertyUtil.getSchemaProperty(context, temp[0]));
                if (k != ++i) {
                    strtype.append(",");
                }
            }
            DomainObject domainObject = new DomainObject(objectId);
            StringList slSelectList = new StringList();
            slSelectList.add(DomainConstants.SELECT_ID);
            domainObject.open(context);
            // boolean c = domainObject.isOpen();
            String strRel = PropertyUtil.getSchemaProperty(context, strRelationship);

            MapList mlList = domainObject.getRelatedObjects(context, strRel, strtype.toString(), slSelectList, null, true, false, (short) 2, null, null, 0);
            if (mlList.size() > 0) {
                Iterator itr = mlList.iterator();
                Map map;
                while (itr.hasNext()) {
                    map = (Map) itr.next();
                    excludeOID.add((String) map.get(DomainConstants.SELECT_ID));
                }
                excludeOID.add(objectId);
            }
            return excludeOID;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in excludeConnectedObjects: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // PCM RFC-117: 27/03/2017 : START
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method connects the MCO to the 150% MBOM
     */
    public int connectRelMBOM(Context context, String[] args) throws Exception {

        boolean isContextPushed = false;

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String strNewObjId = (String) paramMap.get("New OID");
            String strObjectId = (String) paramMap.get("objectId");

            DomainObject domObj = new DomainObject(strNewObjId);
            DomainObject domMCO = new DomainObject(strObjectId);

            // PCM TIGTK-6088: 04/04/2017 : KWagh : START
            StringList slObjSel = new StringList(1);
            slObjSel.addElement(DomainConstants.SELECT_ID);
            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEORDER);
            typePattern.addPattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

            MapList mlMCAs = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, slObjSel, slRelSle, false,
                    true, (short) 1, null, null, 0);

            MapList mlCRCOs = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, typePattern.getPattern(), slObjSel, slRelSle, true, false, (short) 1, null,
                    null, 0);

            if ((!mlMCAs.isEmpty()) || (!mlCRCOs.isEmpty())) {

                String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ErrorInMCOEdit");
                ;

                MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
                return 1;
            }
            // PCM TIGTK-6088: 04/04/2017 : KWagh : End
            // PCM:TIGTK-5779:Rutuja Ekatpure:23/3/2017:start
            // check if MCO having affected item connected
            String strMCOAffectedItem = domMCO.getInfo(context,
                    "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to");

            if (UIUtil.isNullOrEmpty(strMCOAffectedItem)) {

                // get already connected 150% MBOM
                MapList mlRel150MBOMList = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM, "*", null, slRelSle, false, true, (short) 1, null, null, 0);

                if (!mlRel150MBOMList.isEmpty()) {
                    int nCount = mlRel150MBOMList.size();

                    for (int j = 0; j < nCount; j++) {
                        Map mRel150MBOM = (Map) mlRel150MBOMList.get(j);
                        String strRelID = (String) mRel150MBOM.get(DomainRelationship.SELECT_RELATIONSHIP_ID);
                        // disconnect old 150% MBOM
                        DomainRelationship.disconnect(context, strRelID);
                    }
                }
                // connect new 150% MBOM
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isContextPushed = true;
                DomainRelationship.connect(context, domMCO, TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM, domObj);
            } else {
                // show alert that MCO having affected item connected,first remove it before changing 150% MBOM on MCO
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ErrorInEditMCOProperty");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                return 1;
            }
            // PCM:TIGTK-5779:Rutuja Ekatpure:23/3/2017:End

        } catch (RuntimeException e) {
            throw e;
        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: End
        catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectRelMBOM: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }
        return 0;
    }

    // PCM RFC-117: 27/03/2017 : End

    /**
     * Description :
     * @author abhalani
     * @args
     * @Date Oct 4, 2016
     */
    public void connectMfgChangeOrderItemsToMBOM(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        StringList slItemSelect = new StringList();
        slItemSelect.add(DomainConstants.SELECT_CURRENT);

        try {
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strIds = (String) requestMap.get("strSelectedObjectRowIds");
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String strParentId = (String) paramMap.get("objectId");

            // Added for US-130
            StringList slAffectedItemList = FrameworkUtil.split(strIds, "|");

            // PCM : TIGTK-4428 : 09/02/2017 : AB : START
            StringList slReleasedItem = new StringList();

            int intSize = slAffectedItemList.size();
            for (int i = 0; i < intSize; i++) {
                String strID = (String) slAffectedItemList.get(i);
                DomainObject domItem = new DomainObject(strID);
                Map itemMap = domItem.getInfo(context, slItemSelect);
                String strCurrent = (String) itemMap.get(DomainConstants.SELECT_CURRENT);

                if (TigerConstants.STATE_MBOM_RELEASED.equalsIgnoreCase(strCurrent)) {
                    slReleasedItem.add(strID);
                }

            }
            slAffectedItemList.removeAll(slReleasedItem);
            // added for unique
            // PCM : 27/03/2017 : JIRA :5837 //START
            slAffectedItemList = getUniqueIdList(slAffectedItemList);
            // PCM : 27/03/2017 : JIRA :5837 //END
            if (slAffectedItemList.size() != 0) {

                // PCM TIGTK-6561 | 14/04/2017 : AB : START
                // Remove Continuous Provided Material, Continuous Manufacturing Material, Line Data and Operations
                StringList slNotAllowedManufaturingType = new StringList();
                slNotAllowedManufaturingType.add(TigerConstants.TYPE_PSS_LINEDATA);
                slNotAllowedManufaturingType.add(TigerConstants.TYPE_PSS_OPERATION);
                slNotAllowedManufaturingType.add(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);
                slNotAllowedManufaturingType.add(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);

                StringList slFinalAffectedItemList = new StringList();

                if (slAffectedItemList.size() != 0) {
                    int intItemSize = slAffectedItemList.size();
                    for (int i = 0; i < intItemSize; i++) {
                        String strItemID = (String) slAffectedItemList.get(i);
                        DomainObject domItem = new DomainObject(strItemID);
                        String strItemType = (String) domItem.getInfo(context, DomainConstants.SELECT_TYPE);
                        if (!slNotAllowedManufaturingType.contains(strItemType)) {
                            slFinalAffectedItemList.add(strItemID);
                        }
                    }

                    Object[] objectArray = slFinalAffectedItemList.toArray();

                    // PCM TIGTK-6561 | 14/04/2017 : AB : END
                    String[] stringArray = Arrays.copyOf(objectArray, objectArray.length, String[].class);
                    DomainObject dummyObject = new DomainObject();
                    String strName = DomainObject.getAutoGeneratedName(context, "type_PSS_ManufacturingChangeAction", "");
                    dummyObject.createObject(context, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, strName, null, null, null);
                    String strMCAId = dummyObject.getObjectId();
                    DomainObject domMCA = new DomainObject(strMCAId);

                    DomainObject domParent = new DomainObject(strParentId);
                    // PCM TIGTK-4275 | 02/9/17 : PTE : Start
                    DomainRelationship.connect(context, domParent, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, domMCA);
                    // PCM TIGTK-4275 | 02/9/17 : PTE : Ends
                    DomainRelationship.connect(context, domMCA, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, true, stringArray);

                    // PCM TIGTK-4275 | 02/9/17 : PTE : Start
                    String[] strArgs = new String[2];
                    strArgs[0] = strParentId;
                    this.connectRouteTemplateToMfgChangeAction(context, strArgs);
                    // PCM TIGTK-4275 | 02/9/17 : PTE : Ends

                }
            }

            // PCM : TIGTK-4428 : 09/02/2017 : AB : END

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectMfgChangeOrderItemsToMBOM: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * Create the relationship PSS_ConnectedPCMData with ProgramProject at FROM side and MCO at TO side.
     * @param context
     * @param args
     * @throws Exception
     */
    public int connectParentProgramProject(Context context, String[] args) throws Exception {
        int iresult = 1;
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        // HashMap requestMap = (HashMap) programMap.get("requestMap");

        StringList slObjectSle = new StringList(1);
        slObjectSle.addElement(DomainConstants.SELECT_ID);
        slObjectSle.addElement(DomainConstants.SELECT_NAME);

        StringList slRelSle = new StringList(1);
        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

        String strMCOId = (String) paramMap.get("objectId");
        String strProgramProjectId = (String) paramMap.get("New OID");

        DomainObject domMCO = new DomainObject(strMCOId);

        if (strProgramProjectId.equalsIgnoreCase("") || strProgramProjectId.equalsIgnoreCase(null)) {
            // Do nothing
        } else {
            DomainObject domProgramProject = new DomainObject(strProgramProjectId);
            DomainRelationship.connect(context, domProgramProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, domMCO);
            iresult = 0;

        }

        return iresult;
    }

    /**
     * Description :
     * @author abhalani
     * @args
     * @Date Oct 4, 2016
     */
    public MapList getPlantFromCurrentProgramProject(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strProgObjectId = (String) programMap.get("progObjectId");

        DomainObject domObj = new DomainObject(strProgObjectId);
        StringList objectSelect = new StringList();
        objectSelect.add(DomainConstants.SELECT_ID);
        objectSelect.add(DomainConstants.SELECT_NAME);
        StringList relSelect = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);

        MapList mList = domObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY, TigerConstants.TYPE_PSS_PLANT, objectSelect, relSelect, false, true, (short) 1, null, null,
                (short) 0);
        return mList;
    }

    // PCM TIGTK-5780: 29/03/2017 : KWagh : START
    // PCM TIGTK-5781: 29/03/2017 : PMantri : START
    /**
     * Description :
     * @author abhalani
     * @args
     * @Date Oct 4, 2016
     */
    public MapList getProgramProjectsFromCurrentCS(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strMode = (String) programMap.get("strMode");

        MapList mlProgramProjectList;
        StringList objectSelect = new StringList();
        objectSelect.add(DomainObject.SELECT_ID);
        objectSelect.add(DomainObject.SELECT_NAME);
        objectSelect.add(DomainObject.SELECT_DESCRIPTION);
        objectSelect.add(DomainObject.SELECT_ORIGINATED);

        // PCM TIGTK-4461: 16/02/2017 : KWagh : START
        if (!strMode.equals("CRCO")) {
            String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, context.getUser());
            String strCollaborativeSpace = (strSecurityContext.split("[.]")[2]);
            String query = "(project == \"" + strCollaborativeSpace + "\" && (current!=" + TigerConstants.STATE_ACTIVE + ")&&(current!=" + TigerConstants.STATE_OBSOLETE + ")&&(current!='"
                    + TigerConstants.STATE_NONAWARDED + "') && from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]== False)";

            String queryLimit = "0";

            mlProgramProjectList = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PROGRAMPROJECT, // type keyed in or selected from type chooser
                    "*", "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION, query, "", false, objectSelect, Short.parseShort(queryLimit), "*", "");
            mlProgramProjectList.sort(DomainConstants.SELECT_ORIGINATED, "descending", "date");
        } else {
            String strCRObjectId = (String) programMap.get("objectId");
            if (UIUtil.isNullOrEmpty(strCRObjectId)) {
                strCRObjectId = (String) programMap.get("parentOID");
            }
            DomainObject domCRObj = new DomainObject(strCRObjectId);

            StringList relSelect = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);

            mlProgramProjectList = domCRObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, TigerConstants.TYPE_PSS_PROGRAMPROJECT, objectSelect, relSelect, true, false,
                    (short) 1, null, null, (short) 0);
        }

        // PCM TIGTK-5780: 29/03/2017 : KWagh : End
        // PCM TIGTK-5781: 29/03/2017 : PMantri : End
        // PCM TIGTK-4461: 16/02/2017 : KWagh : End
        return mlProgramProjectList;
    }

    /* End - Created By SGS Swapnil Patil */
    /**
     * Description :
     * @author abhalani
     * @args
     * @Date Oct 4, 2016
     */
    public MapList getMfgChangeAction(Context context, String[] args) throws Exception {

        try {

            MapList mlTableData = new MapList();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            // HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strMCOId = (String) programMap.get("objectId");

            DomainObject domMCO = new DomainObject(strMCOId);

            StringList objectSelects = new StringList(6);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_TYPE);

            // Get MCA Objects related to MCO
            MapList mlMCAList = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, "*", objectSelects, new StringList(0), false, true, (short) 0, "", "",
                    (short) 0);

            if (!mlMCAList.isEmpty()) {

                for (int k = 0; k < mlMCAList.size(); k++) {
                    Map mMCAItemObj = (Map) mlMCAList.get(k);
                    mlTableData.add(mMCAItemObj);
                }
            }
            return mlTableData;

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMfgChangeAction: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    /**
     * Description :This method is used for Connect Route template With change Action on Changes of 'Purpose of Release' on CO
     * @author abhalani
     * @args
     * @Date Jul 25, 2016
     */
    public void connectRouteTemplateToMfgChangeAction(Context context, String[] args) throws Exception {
        // TODO Auto-generated method stub
        try {
            String strObjectId = args[0]; // Object ID of MCO
            String strNewValuePurposeOfRelease = args[1];
            String strPSSRouteTemplateTypeValue = "";
            String strConnectedRouteTemplateOfMCA = "";
            // PCM TIGTK-4275 | 02/9/17 : PTE : Start
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {

                Map programProjectMap = new HashMap<>();
                programProjectMap.put(TigerConstants.RANGE_COMMERCIAL_UPDATE, TigerConstants.RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONMCO);
                programProjectMap.put(TigerConstants.RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION, TigerConstants.RANGE_APPROVAL_LIST_FORPROTOTYPEONMCO);
                programProjectMap.put(TigerConstants.RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION, TigerConstants.RANGE_APPROVAL_LIST_FORSERIALLAUNCHONMCO);
                programProjectMap.put(TigerConstants.RANGE_DESIGN_STUDY, TigerConstants.RANGE_APPROVAL_LIST_FORDESIGNSTUDYONMCO);
                programProjectMap.put(TigerConstants.RANGE_Acquisition, TigerConstants.RANGE_APPROVAL_LIST_FORAcquisitionONMCO);
                programProjectMap.put(TigerConstants.RANGE_OTHER, TigerConstants.RANGE_APPROVAL_LIST_FOROTHERPARTSONMCO);

                DomainObject domObject = new DomainObject(strObjectId);
                String strTypeObject = domObject.getInfo(context, DomainConstants.SELECT_TYPE);

                if (strTypeObject.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                    String strPurposeOfreleaseOfMCO = domObject.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
                    // Creating "PSS Program Project" Object Instance
                    String strProgramProjectOID = domObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                    // Added for JIRA TIGTK-2575 -- Pooja Mantri
                    DomainObject domProgramProjectObject = null;
                    if (UIUtil.isNotNullAndNotEmpty(strProgramProjectOID)) {
                        domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
                    }
                    // Added for JIRA TIGTK-2575 -- Pooja Mantri

                    StringList objectSelects = new StringList(1);
                    objectSelects.addElement(DomainConstants.SELECT_ID);
                    objectSelects.addElement(DomainConstants.SELECT_NAME);
                    objectSelects.addElement(DomainConstants.SELECT_TYPE);
                    objectSelects.addElement(DomainConstants.SELECT_POLICY);
                    objectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.name");

                    StringList relSelects = new StringList(1);

                    MapList MfgchangeActionList = domObject.getRelatedObjects(context, // context
                            TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, // relationship pattern
                            TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, // object pattern
                            objectSelects, // object selects
                            relSelects, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, // relationship where clause
                            (short) 0);

                    for (int i = 0; i < MfgchangeActionList.size(); i++) {
                        StringList busSelect = new StringList();
                        Map mMCAObj = (Map) MfgchangeActionList.get(i);
                        String strMCAId = (String) mMCAObj.get(DomainConstants.SELECT_ID);
                        DomainObject domMCAID = new DomainObject(strMCAId);
                        strConnectedRouteTemplateOfMCA = (String) mMCAObj.get("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.name");

                        if (strPurposeOfreleaseOfMCO.equals(TigerConstants.RANGE_DESIGN_STUDY)) {

                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_DESIGN_STUDY);
                        } else if (strPurposeOfreleaseOfMCO.equals(TigerConstants.RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION)) {
                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION);
                        } else if (strPurposeOfreleaseOfMCO.equals(TigerConstants.RANGE_COMMERCIAL_UPDATE)) {
                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_COMMERCIAL_UPDATE);
                        } else if (strPurposeOfreleaseOfMCO.equals(TigerConstants.RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION)) {
                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION);
                        } else if (strPurposeOfreleaseOfMCO.equals(TigerConstants.RANGE_Acquisition)) {
                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_Acquisition);
                        } else {
                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(TigerConstants.RANGE_OTHER);
                        }

                        busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                                + strPSSRouteTemplateTypeValue + "'].to.name");
                        busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                                + strPSSRouteTemplateTypeValue + "'].to.id");

                        Map mapRouteTemplateDetails = domProgramProjectObject.getInfo(context, busSelect);
                        String strRouteTemplateFromProgramProject = (String) mapRouteTemplateDetails.get("from[PSS_ConnectedRouteTemplates].to.name");
                        String strRouteTemplateFromProgramProjectID = (String) mapRouteTemplateDetails.get("from[PSS_ConnectedRouteTemplates].to.id");

                        if (UIUtil.isNullOrEmpty(strConnectedRouteTemplateOfMCA)) {
                            domMCAID.addToObject(context, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES), strRouteTemplateFromProgramProjectID);
                        } else if (!strConnectedRouteTemplateOfMCA.equalsIgnoreCase(strRouteTemplateFromProgramProject)) {
                            String strOldRelID = (String) domMCAID.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].id");
                            DomainRelationship.disconnect(context, strOldRelID);
                            domMCAID.addToObject(context, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES), strRouteTemplateFromProgramProjectID);
                        }

                        if (UIUtil.isNotNullAndNotEmpty(strNewValuePurposeOfRelease)) {
                            String[] strArgs = new String[2];
                            // PCM TIGTK-4275 | 02/9/17 : PTE : Start
                            /*
                             * strArgs[0] = strMCAId; strArgs[1] = null;
                             */
                            strArgs[0] = strObjectId;
                            strArgs[1] = strMCAId;
                            // PCM TIGTK-4275 | 02/9/17 : PTE : Ends
                            // Replaced as per Best Practice- AB

                            // PCM TIGTK-4979 | 01/03/17 : AB : START
                            DomainObject domMCA = new DomainObject(strMCAId);
                            StringList slObjectSle = new StringList(1);
                            slObjectSle.addElement(DomainConstants.SELECT_ID);
                            slObjectSle.addElement(DomainConstants.SELECT_NAME);

                            StringList slRelSle = new StringList(1);
                            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
                            MapList mlObjConnected = domMCA.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, slObjectSle, slRelSle, false, true,
                                    (short) 1, null, null, 0);

                            for (int k = 0; k < mlObjConnected.size(); k++) {
                                Map mRouteObj = (Map) mlObjConnected.get(k);
                                String strRouteID = (String) mRouteObj.get(DomainConstants.SELECT_ID);

                                // Delete old Route from MCA when Purpose of release update of MCO, and connect new Route based on new value
                                DomainObject objRoute = new DomainObject(strRouteID);
                                objRoute.deleteObject(context);
                            }
                            // PCM TIGTK-4979 | 01/03/17 : AB : END
                            this.createRouteOnPrepareState(context, strArgs);

                        }
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectRouteTemplateToMfgChangeAction: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // PCM TIGTK-3032 & TIGTK-2825 | 12/09/16 : Ketaki Wagh : Start
    // Added by Sabari for triggers development-Start
    // This Trigger is used to Promotion of MCO In Review to Complete
    // Release all Affected Item
    /**
     * Description :
     * @author abhalani
     * @args
     * @Date Oct 4, 2016
     */
    public void promoteConnectedAffectedItemToRelease(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String strMCOID = args[0];

        DomainObject domMCOObj = new DomainObject(strMCOID);
        StringList slSelectList = new StringList(2);
        slSelectList.addElement(DomainConstants.SELECT_ID);
        slSelectList.addElement(DomainConstants.SELECT_NAME);

        // Get connected MCA objects
        MapList mlConnectedMCA = (MapList) domMCOObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION,
                slSelectList, DomainObject.EMPTY_STRINGLIST, false, true, (short) 1, null, null, 0);
        int nMCASize = mlConnectedMCA.size();

        if (nMCASize > 0) {

            for (int cnt = 0; cnt < nMCASize; cnt++) {
                Map mMCA = (Map) mlConnectedMCA.get(cnt);
                String strMCAID = (String) mMCA.get(DomainObject.SELECT_ID);

                DomainObject domMCA = new DomainObject(strMCAID);

                // TIGTK-7057:Rutuja Ekatpure:15/6/2017:start
                // Get Related Affected Items
                StringList SlItemOID = domMCA.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.id");
                // order Affected Item list in sequence of child first and then parent
                MapList mlSortedOID = getOrderedParentChild(context, SlItemOID);

                for (Object objMBOM : mlSortedOID) {
                    Map mAffectedItem = (Map) objMBOM;
                    // TIGTK-7057:Rutuja Ekatpure:15/6/2017:End
                    String strAffectedItemID = (String) mAffectedItem.get(DomainObject.SELECT_ID);

                    DomainObject domAffectedItem = new DomainObject(strAffectedItemID);
                    String sCurrent = (String) domAffectedItem.getInfo(context, DomainObject.SELECT_CURRENT);

                    // Set Effectivity Date to System Date

                    String strV_ApplicabilityDate = PropertyUtil.getSchemaProperty(context, "attribute_PLMReference.V_ApplicabilityDate");
                    String strSystemDate = getSystemDate(context, args);

                    domAffectedItem.setAttributeValue(context, strV_ApplicabilityDate, strSystemDate);

                    if (TigerConstants.STATE_PART_APPROVED.equalsIgnoreCase(sCurrent)) {
                        // Approve the Signature between Approved and Release state
                        SignatureList sigList = domAffectedItem.getSignatures(context, sCurrent, TigerConstants.STATE_MBOM_RELEASED);
                        Iterator<Signature> sigItr = sigList.iterator();
                        while (sigItr.hasNext()) {
                            Signature sig = (Signature) sigItr.next();
                            String strComment = "Approved";
                            domAffectedItem.approveSignature(context, sig, strComment);
                        }
                        // Release Affected Item
                        domAffectedItem.setState(context, TigerConstants.STATE_MBOM_RELEASED);

                    }
                }
            }
        }
    }

    // Added by Sabari for triggers development-End
    // PCM TIGTK-3032 & TIGTK-2825 | 12/09/16 : Ketaki Wagh : End

    // PCM RFC-117: 27/03/2017 : START
    /**
     * Description : This method is used for get Manufacturing Part of Related 150% MBOM, for Add existing affected item in MCO
     * @author abhalani
     * @args
     * @Date Aug 8, 2016
     */
    public StringList getMfgPartRealatedTo150MBOM(Context context, String args[]) throws Exception {
        try {

            StringList slAffectedItem = new StringList();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String str150MBOMOfMCO = "";
            String strMCORelatedPlant = "";
            String firstPlant = "";
            /*
             * List lstNotallowedState = new ArrayList();
             * 
             * lstNotallowedState.add(TigerConstants.STATE_PREPARE); lstNotallowedState.add(TigerConstants.STATE_INWORK); lstNotallowedState.add(TigerConstants.STATE_INREVIEW);
             */
            StringList objectSelect = new StringList();
            objectSelect.addElement(DomainConstants.SELECT_ID);

            // Get the ObjectID of MCO
            String strObjId = (String) programMap.get("objectId");

            if (UIUtil.isNotNullAndNotEmpty(strObjId)) {
                DomainObject domObj = new DomainObject(strObjId);
                String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);

                // Get Related 150% MBOM of MCO
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                    str150MBOMOfMCO = domObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM + "].to.id");
                    strMCORelatedPlant = domObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.physicalid");

                }
                // Get Related 150% MBOM of MCA
                else {
                    str150MBOMOfMCO = domObj.getInfo(context,
                            "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM + "].to.id");
                    strMCORelatedPlant = domObj.getInfo(context,
                            "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.physicalid");
                }
                DomainObject dom150MBOM = new DomainObject(str150MBOMOfMCO);

                slAffectedItem.add(str150MBOMOfMCO);
                // Get Related Manufacturing Part of 150MBOM

                // PCM : TIGTK-3940 : 18/01/2017 : AB : START
                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PROCESS_INSTANCE_CONTINUOUS);

                MapList mlConnectedMfgPartObject = dom150MBOM.getRelatedObjects(context, relPattern.getPattern(), TigerConstants.TYPE_DELFMIFUNCTIONREFERENCE, objectSelect, new StringList(0), false,
                        true, (short) 0, null, null, 0);

                // add additional selectable
                objectSelect.addElement(DomainConstants.SELECT_CURRENT);
                objectSelect.addElement(
                        "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.current");

                // PCM : TIGTK-3940 : 18/01/2017 : AB : END
                // Add all related MfgPart of 150MBOM to StringList of Return Affected item
                if (mlConnectedMfgPartObject.size() != 0) {
                    for (int m = 0; m < mlConnectedMfgPartObject.size(); m++) {
                        Map mMfgAffectedItems = (Map) mlConnectedMfgPartObject.get(m);
                        String strMfgAffectedItemId = (String) mMfgAffectedItems.get(DomainConstants.SELECT_ID);

                        String latestRevisionPIDsStr = MqlUtil.mqlCommand(context, "print bus " + strMfgAffectedItemId + " select majorids.lastmajorid dump |", false, false);
                        String[] latestRevisionPIDs = latestRevisionPIDsStr.split("\\|");
                        String latestRevisionPID = latestRevisionPIDs[0];
                        if (UIUtil.isNotNullAndNotEmpty(latestRevisionPID)) {
                            DomainObject domLatestMBOMRev = DomainObject.newInstance(context, latestRevisionPID);

                            Map mLatestMBOMRevInfo = (Map) domLatestMBOMRev.getInfo(context, objectSelect);

                            // get connected MCO state

                            StringList slConnectedMCOCurrent = this.getStringListFromMap(context, mLatestMBOMRevInfo, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM
                                    + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.current");

                            String strNewRevId = (String) mLatestMBOMRevInfo.get(DomainConstants.SELECT_ID);
                            String strCurrent = (String) mLatestMBOMRevInfo.get(DomainConstants.SELECT_CURRENT);

                            if (!slConnectedMCOCurrent.isEmpty() || (!slConnectedMCOCurrent.contains(TigerConstants.STATE_PSS_MCO_PREPARE)
                                    && !slConnectedMCOCurrent.contains(TigerConstants.STATE_PSS_MCO_INWORK) && !slConnectedMCOCurrent.contains(TigerConstants.STATE_INREVIEW_CR))) {
                                if ((strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_MBOM_INWORK) || strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_MBOM_REVIEW))) {

                                    strMfgAffectedItemId = strNewRevId;
                                }
                            }

                        }
                        // PCM TIGTK-8443: 16/06/2017 : VB : START
                        String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strMfgAffectedItemId);
                        if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                            if (strAttachedPlant.equalsIgnoreCase(strMCORelatedPlant) && !slAffectedItem.contains(strMfgAffectedItemId)) {
                                slAffectedItem.add(strMfgAffectedItemId);
                            }
                        }

                        // PCM TIGTK-8443: 16/06/2017 : VB : END
                    }
                }
            }

            return slAffectedItem;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMfgPartRealatedTo150MBOM: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    // PCM RFC-117: 27/03/2017 : End

    // KWagh Modified for TIGTK-2673 - Start

    /**
     * Description: Access function for displaying the commands for Reference Document and Supporting Document
     * @author GChaudhari
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */

    // PCM TIGTK-3300 | 20/10/16 : GC : Start
    public boolean hasCreateNewDocumentAccess(Context context, String args[]) throws Exception {
        boolean bResult = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            // PCM TIGTK-3805 | 29/12/16 : AB : START
            HashMap settingsMap = (HashMap) programMap.get("SETTINGS");
            String strObjId = (String) programMap.get("objectId");
            String strToolbarName = (String) programMap.get("toolbar");
            String strFromRemove = (String) settingsMap.get("FromRemove");
            // PCM TIGTK-3805 | 29/12/16 : AB : END
            DomainObject domChange = new DomainObject(strObjId);

            // PCM TIGTK-3237 | 25/11/16 :Pooja Mantri : Start
            StringList slChangeInfo = new StringList();
            slChangeInfo.add(DomainConstants.SELECT_CURRENT);
            slChangeInfo.add(DomainConstants.SELECT_TYPE);
            Map mChangeInfo = domChange.getInfo(context, slChangeInfo);
            StringList slStateValues = new StringList();
            String strKey = DomainConstants.EMPTY_STRING;

            String strType = (String) mChangeInfo.get(DomainConstants.SELECT_TYPE);
            String strSymType = PropertyUtil.getAliasForAdmin(context, "Type", strType, false);
            String strCurrent = (String) mChangeInfo.get(DomainConstants.SELECT_CURRENT);
            // PCM TIGTK-3237 | 25/11/16 :Pooja Mantri : End
            if (!TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strType)) {
                // For Creating Supporting Documents
                if (strToolbarName.equalsIgnoreCase("PSS_ECMSummarySupportingDocsToolbar") && UIUtil.isNullOrEmpty(strFromRemove)) {
                    // PCM TIGTK-3237 | 25/11/16 :Pooja Mantri : Start
                    strKey = "PSS_EnterpriseChangeMgt.SupportingDocMenuAdd." + strSymType;
                    slStateValues = FrameworkUtil.split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strKey), "|");
                    // PCM TIGTK-3237 | 25/11/16 :Pooja Mantri : End

                }
                // For Creating Supporting Documents
                else if (!strToolbarName.equalsIgnoreCase("PSS_ECMSummarySupportingDocsToolbar") && UIUtil.isNullOrEmpty(strFromRemove)) {
                    // PCM TIGTK-3237 | 25/11/16 :Pooja Mantri : Start
                    strKey = "PSS_EnterpriseChangeMgt.ReferenceDocMenuAdd." + strSymType;
                    slStateValues = FrameworkUtil.split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strKey), "|");
                    // PCM TIGTK-3237 | 25/11/16 :Pooja Mantri : End

                }

                if (slStateValues.contains(strCurrent))
                    bResult = true;
            }
            // For Removing Supporting Documents and Reference Documents
            // PCM | TIGTK-4346 | 14/7/2017 | SIE: Start
            if (UIUtil.isNotNullAndNotEmpty(strFromRemove)) {
                // PCM TIGTK-3237 | 25/11/16 :Pooja Mantri : Start
                strKey = "PSS_EnterpriseChangeMgt.RefAndSupportingDocMenuRemove." + strSymType;
                slStateValues = FrameworkUtil.split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strKey), "|");
                // PCM TIGTK-3237 | 25/11/16 :Pooja Mantri : End
                if (slStateValues.contains(strCurrent))
                    bResult = true;
            }
            // TIGTK-7576 :START
            if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(strType) && bResult) {
                String RELATIONSHIP_PSS_CONNECTEDPCMDATA = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPCMData");
                String RELATIONSHIP_PSS_CONNECTEDMEMBERS = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedMembers");
                StringList slProjectList = domChange.getInfoList(context, "to[" + RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");
                String strContextUser = context.getUser();
                if (slProjectList.contains(strContextUser)) {
                    bResult = true;
                } else {
                    bResult = false;
                }

            }
            // TIGTK-7576 :END
            // PCM | TIGTK-4346 | 14/7/2017 | SIE: Start

        } catch (Exception e) {

            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in hasCreateNewDocumentAccess: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return bResult;
    }

    // PCM TIGTK-3300 | 20/10/16 : GC : End

    /**
     * @author KWagh
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Access function for displaying the Create New Document command other than CO,MCO and CN
     */
    public boolean showCreateDocumentCommand(Context context, String args[]) throws Exception {

        boolean bResult = true;
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            // Get the ObjectID of CN
            String strObjID = (String) programMap.get("objectId");

            DomainObject domObj = new DomainObject();
            if (UIUtil.isNotNullAndNotEmpty(strObjID)) {
                domObj.setId(strObjID);
            }
            String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);

            if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGENOTICE) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)
                    || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                bResult = false;

            }

        } catch (Exception e) {

            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in showCreateDocumentCommand: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return bResult;
    }

    // KWagh Modified for TIGTK-2673 - End

    // SteepGraph(Rutuja Ekatpure):29/08/2016:Start
    /****
     * method for get related plants for selected program project
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Map getRelatedPlant(Context context, String args[]) throws Exception {
        Map mParamForPlant = new HashMap();
        HashMap plantRangeMap = new HashMap();
        StringList fieldRangeValues = new StringList();
        StringList fieldDisplayRangeValues = new StringList();

        HashMap programMap = JPO.unpackArgs(args);
        HashMap fieldValuesMap = (HashMap) programMap.get("fieldValues");
        String strProjectId = "";
        if (null != fieldValuesMap && !fieldValuesMap.isEmpty())
            strProjectId = (String) fieldValuesMap.get("Project codeOID");
        // if field Values map does not contain project id then put from request map
        if (UIUtil.isNullOrEmpty(strProjectId)) {
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            strProjectId = (String) requestMap.get("strProjectId");
        }

        mParamForPlant.put("progObjectId", strProjectId);
        // get plant list from program project
        // Replaced as per Best Practice
        pss.ecm.ui.MfgChangeOrder_mxJPO MCOObj = new pss.ecm.ui.MfgChangeOrder_mxJPO();
        MapList mPlantList = MCOObj.getPlantFromCurrentProgramProject(context, JPO.packArgs(mParamForPlant));
        int cntMap = mPlantList.size();
        // PCM TIGTK-3034 | 12/09/16 : AB : START
        // fieldRangeValues.add("");
        // fieldDisplayRangeValues.add("");
        // PCM TIGTK-3034 | 12/09/16 : AB : END
        for (int cnt = 0; cnt < cntMap; cnt++) {
            Map mPlantObj = (Map) mPlantList.get(cnt);
            String strPlantName = (String) mPlantObj.get(DomainConstants.SELECT_NAME);
            String strPlantID = (String) mPlantObj.get(DomainConstants.SELECT_ID);
            fieldRangeValues.add(strPlantID);
            fieldDisplayRangeValues.add(strPlantName);
        }
        // construct plant range map
        plantRangeMap.put("RangeValues", fieldRangeValues);
        plantRangeMap.put("RangeDisplayValues", fieldDisplayRangeValues);
        return plantRangeMap;
    }

    // SteepGraph(Rutuja Ekatpure):29/08/2016:End

    /**
     * Description : Send MCA assignee mail notification on Promotion of MCA Prepare state to In Work state
     * @author abhalani
     * @args
     * @Date Oct 4, 2016
     */

    public void sendNotificationToMCAAssignee(Context context, String args[]) throws Exception {
        // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : Start
        boolean isContextPushed = false;
        String contextUser = (String) context.getCustomData("contextUser");
        // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : End
        try {
            // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : Start
            if (contextUser != null && !"".equals(contextUser)) {
                ContextUtil.pushContext(context, contextUser, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isContextPushed = true;
                context.setCustomData("contextUser", "");
            }
            // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : End
            DomainObject domMCA = new DomainObject(args[0]);

            String strMCAAssignee = domMCA.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.name");
            String strMCOCreator = domMCA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.attribute[Originator]");

            StringList slInfo = new StringList();
            slInfo.add(DomainConstants.SELECT_NAME);
            slInfo.add(DomainConstants.SELECT_ID);
            Map mInfoMap = domMCA.getInfo(context, slInfo);
            String strMCAName = (String) mInfoMap.get(DomainConstants.SELECT_NAME);
            String strMCAID = (String) mInfoMap.get(DomainConstants.SELECT_ID);
            // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : Start
            StringBuffer strMsgBodyBuffer = new StringBuffer();
            String strMessageBody = "";
            // DomainObject domMCAObject = DomainObject.newInstance(context, strMCAID);
            String strMCOId = domMCA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.id");
            // Create Link for Change Request & Program Project
            String strBaseURLSubstring = "";
            String strMCOLink = "";
            String strBaseURL = MailUtil.getBaseURL(context);

            if (UIUtil.isNotNullAndNotEmpty(strBaseURL)) {
                int position = strBaseURL.lastIndexOf("/");
                strBaseURLSubstring = strBaseURL.substring(0, position);
                strMCOLink = strBaseURLSubstring + "/emxTree.jsp?mode=edit&objectId=" + strMCOId;

            }
            strMessageBody = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Message.Body.MCAAssigned.StartKey")
                    + strMCAName + " " + EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Message.Body.MCAAssigned.EndKey");
            strMsgBodyBuffer.append(strMessageBody);
            strMsgBodyBuffer.append("\n");
            strMsgBodyBuffer.append(strMCOLink);
            // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : End

            String strSubject = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Message.Subject.MCAAssigned.StartKey")
                    + strMCAName + " "
                    + EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Message.Subject.MCAAssigned.EndKey");

            StringList objectIdList = new StringList();
            objectIdList.add(strMCAID);

            StringList mailToList = new StringList();

            if (!strMCOCreator.equals(strMCAAssignee)) {
                mailToList.add(strMCAAssignee);
                mailToList.add(strMCOCreator);
            } else {
                mailToList.add(strMCAAssignee);
            }
            // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : Start
            MailUtil.sendNotification(context, mailToList, // toList
                    null, // ccList
                    null, // bccList
                    strSubject, // subjectKey
                    null, // subjectKeys
                    null, // subjectValues
                    strMsgBodyBuffer.toString(), // messageKey
                    null, // messageKeys
                    null, // messageValues
                    objectIdList, // objectIdList
                    null); // companyName
            // PCM TIGTK-3299 | 30/09/16 : Pooja Mantri : End
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in sendNotificationToMCAAssignee: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        } finally {
            // PCM TIGTK-3602 : 17/11/2016 : KWagh : Start
            if (isContextPushed == true) {
                ContextUtil.popContext(context);
            }
            // PCM TIGTK-3602 : 17/11/2016 : KWagh : End
        }
    }

    /**
     * Description :
     * @author abhalani
     * @args
     * @Date Oct 4, 2016
     */
    public int checkAIConnectionToActiveMCO(Context context, String args[]) throws Exception {
        return 0;
    }

    /***
     * method merges selected MCA to new MCA :Rutuja Ekatpure(1/9/2016)
     * @param context
     * @param args
     * @throws Exception
     */
    public String mergeSelectedMCAtoNewMCA(Context context, String[] args) throws Exception {
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strMCOId = (String) paramMap.get("strMCOId");
            String[] emxTableRowIds = (String[]) paramMap.get("emxTableRowIds");

            // create new MCA
            DomainObject doMCAObj = new DomainObject();
            String strAutoName = doMCAObj.getAutoGeneratedName(context, "type_PSS_ManufacturingChangeAction", "");
            doMCAObj.createObject(context, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, strAutoName, "-", TigerConstants.POLICY_PSS_MANUFACTURINGCHANGEACTION, "eService Production");
            String strMCAId = doMCAObj.getInfo(context, DomainConstants.SELECT_ID);
            // get all id's of selected row
            Map mapObjIdRelId = (Map) ChangeUtil.getObjectIdsRelIdsMapFromTableRowID(emxTableRowIds);
            StringList slObjectIds = (StringList) mapObjIdRelId.get("ObjId");

            int objIdCount = slObjectIds.size();
            if (objIdCount < 2) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ErrorInMergeMCA");
                return strMessage;
            } else {
                for (int i = 0; i < objIdCount; i++) {
                    String strSelectedMCAId = (String) slObjectIds.get(i);
                    DomainObject domMCAObj = new DomainObject(strSelectedMCAId);

                    StringList objSelects = new StringList();
                    objSelects.addElement(DomainConstants.SELECT_ID);

                    // Findbug Issue correction start
                    // Date: 21/03/2017
                    // By: Asha G.
                    MapList mlRelatedAffectedItems;// = new MapList();// Fix for FindBugs issue Method call on instantiation : Harika Varanasi : 21 March 2017
                    // get affected item of selected MCA
                    mlRelatedAffectedItems = domMCAObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, // relationship pattern
                            "*", // object pattern
                            objSelects, // object selects
                            null, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, 0);
                    // Findbug Issue correction End

                    int nRelatedAICount = mlRelatedAffectedItems.size();
                    for (int j = 0; j < nRelatedAICount; j++) {
                        Map mapAI = (Map) mlRelatedAffectedItems.get(j);
                        String strConnectedAIID = (String) mapAI.get(DomainConstants.SELECT_ID);
                        // code changes by rutuja for removing MCA with no MCO connection issue:18/11/2016:start
                        // disconnect affected item from old MCA
                        MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", strSelectedMCAId, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, strConnectedAIID);
                        // connect affected item of selected MCA to new MCA
                        MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", strMCAId, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, strConnectedAIID);
                    }
                    // code changes by rutuja for removing MCA with no MCO connection issue:18/11/2016:End
                }
                // connect new MCA with MCO
                MqlUtil.mqlCommand(context, "connect bus $1 relationship $2 to $3", strMCOId, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, strMCAId);

            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in mergeSelectedMCAtoNewMCA: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return "Success";
    }

    /***
     * method used to get connected MCA to MCO :Rutuja Ekatpure (1/9/2016)
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public MapList getAffectedMCA(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strParentId = (String) programMap.get("parentOID");

            // PCM : TIGTK-10011 & TIGTK-10004 : 22/09/2017 : AB : START
            // Check parent object id is MCO or any other change object
            if (UIUtil.isNotNullAndNotEmpty(strParentId)) {
                DomainObject domParentObject = DomainObject.newInstance(context, strParentId);
                String strParentType = (String) domParentObject.getInfo(context, DomainConstants.SELECT_TYPE);

                if (!TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER.equalsIgnoreCase(strParentType)) {
                    strParentId = (String) programMap.get("objectId");
                }
            }

            // PCM : TIGTK-10011 & TIGTK-10004 : 22/09/2017 : AB : END

            DomainObject domMCO = DomainObject.newInstance(context, strParentId);
            StringList objSelects = new StringList(DomainConstants.SELECT_ID);

            Pattern relPatternMfgChangeAffectedItem = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION);
            Pattern typePatternMfgChangeAction = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION);

            // get related all MCA of MCO
            MapList mlRelatedMCA = domMCO.getRelatedObjects(context, relPatternMfgChangeAffectedItem.getPattern(), // relationship pattern
                    typePatternMfgChangeAction.getPattern(), // object pattern
                    objSelects, // object selects
                    null, // relationship selects
                    true, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, 0);

            return mlRelatedMCA;
        } catch (Exception e) {
            logger.error("Error in getAffectedMCA: ", e);
            throw e;
        }
    }

    /***
     * method used to exclude connected affected items from MCO affected item add existing:Rutuja Ekatpure (12/9/2016)
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList excludeConnectedAffectedItems(Context context, String args[]) throws Exception {
        try {
            StringList slAffectedItemOfMCA = new StringList();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String strId = (String) programMap.get("objectId");

            if (UIUtil.isNotNullAndNotEmpty(strId)) {
                DomainObject domObj = new DomainObject(strId);
                String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);

                // Get Related Affected Items of MCO
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                    slAffectedItemOfMCA = domObj.getInfoList(context,
                            "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.id");
                }
                // Get Related Affected Items of MCO
                else {
                    slAffectedItemOfMCA = domObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.id");
                }

            }
            return slAffectedItemOfMCA;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in excludeConnectedAffectedItems: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    // PCM TIGTK-3060 | 06/10/16 : Pooja Mantri : Start
    /**
     * This method is called for PLM Session Object closing.
     * @param context
     * @param args
     * @returntype --void --- Returns nothing
     * @throws Exception
     */
    // Fix for FindBugs issue related to throw : Harika Varanasi : 21 March 2017: Start
    public static void flushAndCloseSession(PLMCoreModelerSession plmSession) throws Exception {
        try {
            plmSession.flushSession();
        } catch (Exception e) {
            // PCM:TIGTK-5796:Rutuja Ekatpure:24/3/2017:start
            // throw e;
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in flushAndCloseSession: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            // PCM:TIGTK-5796:Rutuja Ekatpure:24/3/2017:End
        }

        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in flushAndCloseSession: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }
    // Fix for FindBugs issue related to throw : Harika Varanasi : 21 March 2017: END

    // PCM TIGTK-3060 | 06/10/16 : Pooja Mantri : Start

    // Added by Priyanka Salunke - TIGTK-3321 -Start
    /**
     * This method is invoked via a Promote check trigger when the MCA is promoted from In Review to Complete
     * @param context
     * @param args
     * @throws Exception
     */
    public int checkMCOInInReviewState(Context context, String[] args) throws Exception {
        String strMCAId = args[0];
        int intReturn = 0;
        DomainObject domMCAObject = new DomainObject();
        DomainObject domMCOObject = new DomainObject();
        try {
            if (UIUtil.isNotNullAndNotEmpty(strMCAId)) {
                domMCAObject = DomainObject.newInstance(context, strMCAId);
            }
            // Get connected MCO of MCA
            String strMCOId = (String) domMCAObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.id");
            if (UIUtil.isNotNullAndNotEmpty(strMCOId)) {
                domMCOObject = DomainObject.newInstance(context, strMCOId);
            }
            // Check MCO Current State
            String strMCOCurrentState = (String) domMCOObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            if (strMCOCurrentState.equalsIgnoreCase("In Review")) {
                intReturn = 0;
            } else {
                intReturn = 1;
            }
            // If MCO is not in In Review state then throw alert
            if (intReturn == 1) {
                // Throw error
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Alert.MfgChangeOrderNotInInReviewState");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkMCOInInReviewState: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return intReturn;

    } // End for TIGTK-3321 by Priyanka salunke

    // PCM TIGTK-3597 | 17/11/16 : Kwagh : Start
    /**
     * @Description: This Method is used on the Demote Trigger on MCO from In Work State to Prepare State which will demote the Connected MCAs if all the MCAs are in In Work State.
     * @param context
     * @param args
     * @return nothing
     * @throws Exception
     */

    public void demoteCAMCAToPrepareState(Context context, String[] args) throws Exception {
        boolean isContextPushed = false;
        String strObjId = args[0];
        try {
            DomainObject domMCO = DomainObject.newInstance(context, strObjId);
            // DomainObject domMCA = new DomainObject();

            // StringList slConnectedMCA = new StringList();
            // Findbug Issue correction start
            // Date: 21/03/2017
            // By: Asha G.

            StringList slConnectedMCACurrent = null;
            // Findbug Issue correction end
            boolean flag = true;

            StringList slConnectedMCA = domMCO.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.id");
            slConnectedMCACurrent = domMCO.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.current");

            for (Object obj : slConnectedMCACurrent) {
                String strCurrent = (String) obj;
                if (!strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_MCA_INWORK)) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                // Findbug Issue correction start
                // Date: 21/03/2017
                // By: Asha G.
                BusinessObject busObj = null;
                // Findbug Issue correction End
                int slSize = slConnectedMCA.size();
                for (int itr = 0; itr < slSize; itr++) {
                    // domMCA = DomainObject.newInstance(context,(String)slConnectedMCA.get(itr));
                    busObj = DomainObject.newInstance(context, (String) slConnectedMCA.get(itr));
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    isContextPushed = true;
                    // domMCA.demote(context);
                    busObj.demote(context);

                }
            }
        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: START
        catch (RuntimeException e) {
            throw e;
        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: End
        catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in demoteCAMCAToPrepareState: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }
    }

    // PCM TIGTK-3597 | 17/11/16 : Kwagh : End

    // PCM TIGTK-3560 | 11/11/16 : Pooja Mantri : Start
    /**
     * @Description: This Method is used on the Promote Trigger on MCO from Prepare State to In Work which will check for connected CR or CO Object.
     * @param context
     * @param args
     *            -- args 0 -- MCO Object Id
     * @return -- int -- Status of Check Trigger
     * @throws Exception
     */
    public int checkForCROrCOConnectedToMCO(Context context, String args[]) throws Exception {
        int retStatus = 0;
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            String strMCObjectId = args[0];
            DomainObject domMCOObject = DomainObject.newInstance(context, strMCObjectId);
            boolean result = domMCOObject.hasRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, false);
            if (result == true) {
                retStatus = 0;
            } else {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.NoCROrCOForMCO");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                retStatus = 1;
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForCROrCOConnectedToMCO: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return retStatus;
    }

    // PCM TIGTK-3560 | 11/11/16 : Pooja Mantri : End
    // PCM:TIGTK-3587:Check trigger on MCA in review promote check :Rutuja Ekatpure:15/11/2016:Start
    /***
     * this trigger added on MCA in review promote check ,this method checks content MBOM having connected EC part released or not
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkRelatedECPartStateOfConnectedMBOM(Context context, String[] args) throws Exception {
        int intResult = 0;
        PLMCoreModelerSession plmSession = null;
        StringList slPartName = new StringList();

        try {
            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                    "PSS_EnterpriseChangeMgt.Alert.ECPartConnectedNotInReleasedState");
            String strMCAID = args[0];
            DomainObject domMCAObj = new DomainObject(strMCAID);
            // get connected MBOM object id
            StringList slMBOMID = domMCAObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.id");

            ContextUtil.startTransaction(context, true);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // get reference physical ID connected to MBOM
            List<String> lRefPIDList = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, slMBOMID);
            // iterate list of Physical ID
            if (lRefPIDList != null && lRefPIDList.size() > 0) {
                for (int j = 0; j < lRefPIDList.size(); j++) {
                    String strRefPID = lRefPIDList.get(j);
                    if (strRefPID != null) {
                        // get Part from VPM reference
                        MapList mlPartId = pss.mbom.MBOMUtil_mxJPO.getPartFromVPMReference(context, strRefPID);
                        if (mlPartId != null && mlPartId.size() > 0) {
                            Map objMap = (Map) mlPartId.get(0);
                            DomainObject domPart = DomainObject.newInstance(context, (String) objMap.get(DomainConstants.SELECT_ID));
                            StringList slObjectSelect = new StringList(2);
                            slObjectSelect.add(DomainConstants.SELECT_CURRENT);
                            slObjectSelect.add(DomainConstants.SELECT_NAME);
                            Map mapObjInfo = domPart.getInfo(context, slObjectSelect);
                            String strName = (String) mapObjInfo.get(DomainConstants.SELECT_NAME);
                            String strCurrent = (String) mapObjInfo.get(DomainConstants.SELECT_CURRENT);
                            // check state of Part
                            // PCM : TIGTK-5268 : 14/03/2017 : AB : START
                            // PCM TIGTK-5268: 30/03/2017 : KWagh : START
                            if (!strCurrent.equalsIgnoreCase(TigerConstants.STATE_PART_RELEASE) && !strCurrent.equalsIgnoreCase(TigerConstants.STATE_DEVELOPMENTPART_COMPLETE)
                                    && !strCurrent.equalsIgnoreCase(TigerConstants.STATE_OBSOLETE)) {

                                slPartName.add(strName);
                                intResult = 1;
                            }
                            // PCM TIGTK-5268: 30/03/2017 : KWagh : End
                            // PCM : TIGTK-5268 : 14/03/2017 : AB : END
                        }
                    }
                }
                if (intResult == 1) {
                    String strMessage = (String) FrameworkUtil.join(slPartName, "\n");
                    strAlertMessage = strAlertMessage.replace("$<name>", strMessage);
                    MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
                }
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkRelatedECPartStateOfConnectedMBOM: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return intResult;
    }

    // PCM:TIGTK-3587:Check trigger on MCA in review promote check :Rutuja Ekatpure:15/11/2016:end

    /**
     * Description : This method is used get the calendar date for Route Due Date Modified for TIGTK-3766
     * @author abhalani
     * @args
     * @Date December 14, 2016
     */

    public String getRouteDueDate(Context context, String[] args) throws Exception {
        SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -1);
        cal.add(Calendar.DATE, 7);
        Date date = cal.getTime();
        String strDate = _mxDateFormat.format(date);
        return strDate;
    }

    /**
     * Description : This method is used set the Due date for Route on MCA Added for TIGTK-3766
     * @author abhalani
     * @throws Exception
     * @args
     * @Date December 14, 2016
     */

    public void setDueDateOnMCARoute(Context context, String strRouteID) throws Exception {
        try {
            String strRouteId = strRouteID;
            DomainObject domRoute = new DomainObject(strRouteId);

            // Get the RouteNode relation of Route on MCA
            StringList slNewRouteNodeRelId = domRoute.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_NODE + "].id");

            if (slNewRouteNodeRelId != null && !slNewRouteNodeRelId.isEmpty()) {
                for (int i = 0; i < slNewRouteNodeRelId.size(); i++) {

                    String strRelId = (String) slNewRouteNodeRelId.get(i);
                    DomainRelationship domRelRouteNode = DomainRelationship.newInstance(context, strRelId);

                    // setting attribute (scheduled completion date) values for that relationship.
                    domRelRouteNode.setAttributeValue(context, DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE, this.getRouteDueDate(context, null));
                }
            }

        } catch (FrameworkException e) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setDueDateOnMCARoute: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
    }

    // TGPSS_PCM_TS_155_ManufacturingChangeOrderNotifications_V3.1 | 28/02/2017 |Harika Varanasi : Starts

    /**
     * getManufacturingChangeOrderInformation method is used to get all information about PSS_ManufacturingChangeOrder As a Part of TGPSS_PCM_TS_155_ManufacturingChangeOrderNotifications_V3.1
     * @param context
     * @param strMCOId
     * @return Map
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public Map getManufacturingChangeOrderInformation(Context context, String strMCOId) throws Exception {
        Map mapMCO = new HashMap();
        try {
            if (UIUtil.isNotNullAndNotEmpty(strMCOId)) {
                DomainObject domMCOObj = DomainObject.newInstance(context);
                domMCOObj.setId(strMCOId);

                StringList slObjectSelects = new StringList(13);
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                slObjectSelects.addElement(DomainConstants.SELECT_NAME);
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
                slObjectSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PHYSICAL_IMPLEMENTATION_PLANNED_DATE + "]");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");
                slObjectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].from.name");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.id");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.name");

                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED + "]");

                // PCM : TIGTK-7745 : 01/09/2017 : AB : START
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.name");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.name");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.id");
                // PCM : TIGTK-7745 : 01/09/2017 : AB : END

                mapMCO = domMCOObj.getInfo(context, slObjectSelects);
            }

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getManufacturingChangeOrderInformation: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        } finally {
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.name");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.id");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.name");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.id");
        }

        return mapMCO;
    }

    /**
     * transformMCOMapToHTMLList As a Part of TGPSS_PCM_TS_155_MfgChangeOrderNotifications_V3.1
     * @param context
     * @param objectMap
     * @return MapList
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public MapList transformMCOMapToHTMLList(Context context, Map objectMap, String strBaseURL) throws Exception {
        MapList mlInfoList = new MapList();
        try {

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);

            StringList slHyperLinkLabelKey = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.MCONotification.HyperLinkLabelKey"), ",");
            StringList slHyperLinkLabelKeyIds = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.MCONotification.HyperLinkLabelKeyIds"), ",");
            initializeMCOLinkedHashMap();
            mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmMCOSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in transformMCOMapToHTMLList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mlInfoList;
    }

    /**
     * initializeMCOLinkedHashMap As a Part of TGPSS_PCM_TS_155_MfgChangeOrderNotifications_V3.1
     * @throws Exception
     *             if the operation fails
     * @author Harika Varanasi : SteepGraph
     */
    public void initializeMCOLinkedHashMap() throws Exception {
        try {

            if (lhmMCOSelectionStore != null && (lhmMCOSelectionStore.isEmpty())) {

                lhmMCOSelectionStore.put("Title", "SectionHeader");
                lhmMCOSelectionStore.put("Subject", "SectionSubject");
                lhmMCOSelectionStore.put("Main_Information", "SectionHeader");
                lhmMCOSelectionStore.put("Project_Code", "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                lhmMCOSelectionStore.put("Project_Description", "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                lhmMCOSelectionStore.put("MCO", DomainConstants.SELECT_NAME);
                lhmMCOSelectionStore.put("MCO_Description", DomainConstants.SELECT_DESCRIPTION);
                lhmMCOSelectionStore.put("State", DomainConstants.SELECT_CURRENT);
                lhmMCOSelectionStore.put("Physical_Implementation_Planned_Date", "attribute[" + TigerConstants.ATTRIBUTE_PSS_PHYSICAL_IMPLEMENTATION_PLANNED_DATE + "]");
                lhmMCOSelectionStore.put("MCO_Plant", "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");
                lhmMCOSelectionStore.put("MCO_Creator", "attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                // TIGTK-7580 : START
                lhmMCOSelectionStore.put("Comment", "TransferComments");
                // TIGTK-7580 : END
                lhmMCOSelectionStore.put("Useful_Links", "SectionHeader");
                lhmMCOSelectionStore.put("Related_Content", "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.name");
                lhmMCOSelectionStore.put("Related_Change", "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].from.name");
            }

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeMCOLinkedHashMap: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }

    /**
     * getMCONotificationBodyHTML method is used to MCO messageHTML in Notification Object As apart of TGPSS_PCM_TS_155_MfgChangeOrderNotifications_V3.1
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getMCONotificationBodyHTML(Context context, String[] args) throws Exception {
        String messageHTML = "";
        boolean bIsCOInWorkNotificaton = false;
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strMCOObjId = (String) programMap.get("id");
            String strBaseURL = (String) programMap.get("baseURL");
            String notificationObjName = (String) programMap.get("notificationName");
            String strAttrSubText = PropertyUtil.getSchemaProperty(context, "attribute_SubjectText");
            StringList busSelects = new StringList("attribute[" + strAttrSubText + "]");
            String strAttrTransferSAPValue = "Yes";
            if (UIUtil.isNotNullAndNotEmpty(strMCOObjId)) {
                DomainObject domMcoObject = DomainObject.newInstance(context, strMCOObjId);
                strAttrTransferSAPValue = (String) domMcoObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED);

            }

            Map issueMap = (Map) getManufacturingChangeOrderInformation(context, strMCOObjId);

            String strSubjectKey = "";
            MapList mpNotificationList = DomainObject.findObjects(context, "Notification", notificationObjName, "*", "*", TigerConstants.VAULT_ESERVICEADMINISTRATION, "", null, true, busSelects,
                    (short) 0);

            if (mpNotificationList != null && (!mpNotificationList.isEmpty())) {
                strSubjectKey = (String) ((Map) mpNotificationList.get(0)).get("attribute[Subject Text]");
            }
            if (UIUtil.isNotNullAndNotEmpty(strSubjectKey) && (!strSubjectKey.contains("JPO "))) {
                issueMap.put("SectionSubject", EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strSubjectKey));
            } else if (UIUtil.isNotNullAndNotEmpty(strSubjectKey) && strSubjectKey.contains("JPO")) {
                issueMap.put("SectionSubject", EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        String.format("EnterpriseChangeMgt.MCONotification.%s.SubjectKeyCompletePromote", strAttrTransferSAPValue)));
            } else {
                issueMap.put("SectionSubject", "");
            } // TIGTK-7580 : START
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_TransferOwnershipNotification".equalsIgnoreCase(notificationObjName)) {
                Map payLoadMap = (Map) programMap.get("payload");
                if (payLoadMap != null) {
                    if (payLoadMap.containsKey("TransferComments")) {
                        String strComments = (String) payLoadMap.get("TransferComments");
                        issueMap.put("TransferComments", strComments);
                    }
                }
                pss.ecm.enoECMChange_mxJPO enoECMChange = new pss.ecm.enoECMChange_mxJPO();
                strSubjectKey = enoECMChange.getTranferOwnershipSubject(context, args);
                issueMap.put("SectionSubject", strSubjectKey);
            } else {
                issueMap.put("TransferComments", "");
            } // TIGTK-7580:END
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_MCOInWorkNotification".equalsIgnoreCase(notificationObjName)) {
                PropertyUtil.setGlobalRPEValue(context, "PSS_NOTIFICATION_NAME", "PSS_MCOInWorkNotification");
                bIsCOInWorkNotificaton = true;
            }
            MapList mlInfoList = transformMCOMapToHTMLList(context, issueMap, strBaseURL);

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            String strStyleSheet = commonObj.getFormStyleSheet(context, "PSS_NotificationStyleSheet");
            messageHTML = commonObj.getHTMLTwoColumnTable(context, mlInfoList, strStyleSheet);
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMCONotificationBodyHTML: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        } finally {
            if (bIsCOInWorkNotificaton)
                PropertyUtil.setGlobalRPEValue(context, "PSS_NOTIFICATION_NAME", "");
        }
        return messageHTML;
    }

    /**
     * getMCOCompleteNotificationSubject method is used As apart of TGPSS_PCM_TS_155_MfgChangeOrderNotifications_V3.1
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    public String getMCOCompleteNotificationSubject(Context context, String[] args) throws Exception {
        String strMCOSubject = "";
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strMCOObjId = (String) programMap.get("id");

            if (UIUtil.isNotNullAndNotEmpty(strMCOObjId)) {
                DomainObject domMCOObj = DomainObject.newInstance(context);
                domMCOObj.setId(strMCOObjId);
                String strAttrTransferSAPValue = (String) domMCOObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED);
                strMCOSubject = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        String.format("EnterpriseChangeMgt.MCONotification.%s.SubjectKeyCompletePromote", strAttrTransferSAPValue));

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMCOCompleteNotificationSubject: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return strMCOSubject;

    }

    // TGPSS_PCM_TS_155_ManufacturingChangeOrderNotifications_V3.1 | 28/02/2017 |Harika Varanasi : Ends

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author PTE : SteepGraph
     */

    public int checkRouteTemplateOfProgramProject(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            String strObjectId = args[0]; // Object ID of MCO
            String strNewValuePurposeOfRelease = args[1];
            String strPSSRouteTemplateTypeValue = "";
            String strConnectedRouteTemplateOfMCA = "";

            if (DomainConstants.EMPTY_STRING.equals(strObjectId)) {
                return 0;
            }
            final String RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATE = "Approval List for Commercial update on MCO";
            final String RANGE_APPROVAL_LIST_FORPROTOTYPE = "Approval List for Prototype on MCO";
            final String RANGE_APPROVAL_LIST_FORSERIALLAUNCH = "Approval List for Serial Launch on MCO";
            final String RANGE_APPROVAL_LIST_FORDESIGNSTUDY = "Approval List for Design study on MCO";
            final String RANGE_APPROVAL_LIST_FOROTHERPARTS = "Approval List for Other Parts on MCO";
            // Range Constants for attribute "PSS_Purpose_Of_Release"
            final String RANGE_OTHER = "Other";
            final String RANGE_DESIGN_STUDY = "Design study";
            final String RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION = "Serial Tool Launch/Modification";
            final String RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION = "Prototype Tool Launch/Modification";
            final String RANGE_COMMERCIAL_UPDATE = "Commercial Update";

            Map programProjectMap = new HashMap<>();
            programProjectMap.put(RANGE_COMMERCIAL_UPDATE, RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATE);
            programProjectMap.put(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION, RANGE_APPROVAL_LIST_FORPROTOTYPE);
            programProjectMap.put(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION, RANGE_APPROVAL_LIST_FORSERIALLAUNCH);
            programProjectMap.put(RANGE_DESIGN_STUDY, RANGE_APPROVAL_LIST_FORDESIGNSTUDY);
            programProjectMap.put(RANGE_OTHER, RANGE_APPROVAL_LIST_FOROTHERPARTS);

            DomainObject domObject = new DomainObject(strObjectId);
            String strTypeObject = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
            if (strTypeObject.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                // Creating "PSS Program Project" Object Instance
                String strProgramProjectOID = domObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                DomainObject domProgramProjectObject = null;
                if (UIUtil.isNotNullAndNotEmpty(strProgramProjectOID)) {
                    domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
                }
                StringList objectSelects = new StringList(1);
                objectSelects.addElement(DomainConstants.SELECT_ID);
                objectSelects.addElement(DomainConstants.SELECT_NAME);
                objectSelects.addElement(DomainConstants.SELECT_TYPE);
                objectSelects.addElement(DomainConstants.SELECT_POLICY);
                objectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.name");

                StringList relSelects = new StringList(1);
                MapList MfgchangeActionList = domObject.getRelatedObjects(context, // context
                        TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, // relationship pattern
                        TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, // object pattern
                        objectSelects, // object selects
                        relSelects, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, // relationship where clause
                        (short) 0);
                boolean flag = false;
                for (int i = 0; i < MfgchangeActionList.size(); i++) {

                    StringList busSelect = new StringList();
                    Map mMCAObj = (Map) MfgchangeActionList.get(i);
                    String strMCAId = (String) mMCAObj.get(DomainConstants.SELECT_ID);
                    DomainObject domMCAID = new DomainObject(strMCAId);
                    strConnectedRouteTemplateOfMCA = (String) mMCAObj.get("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.name");

                    if (strNewValuePurposeOfRelease.equals(RANGE_DESIGN_STUDY)) {
                        strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_DESIGN_STUDY);
                    } else if (strNewValuePurposeOfRelease.equals(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION)) {
                        strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION);
                    } else if (strNewValuePurposeOfRelease.equals(RANGE_COMMERCIAL_UPDATE)) {
                        strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_COMMERCIAL_UPDATE);
                    } else if (strNewValuePurposeOfRelease.equals(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION)) {
                        strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION);
                    } else if (strNewValuePurposeOfRelease.equals(RANGE_OTHER)) {
                        strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_OTHER);
                    }
                    busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                            + strPSSRouteTemplateTypeValue + "'].to.name");
                    busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                            + strPSSRouteTemplateTypeValue + "'].to.id");

                    Map mapRouteTemplateDetails = domProgramProjectObject.getInfo(context, busSelect);
                    String strRouteTemplateFromProgramProject = (String) mapRouteTemplateDetails.get("from[PSS_ConnectedRouteTemplates].to.name");
                    String strRouteTemplateFromProgramProjectID = (String) mapRouteTemplateDetails.get("from[PSS_ConnectedRouteTemplates].to.id");
                    String strProgProjName = domProgramProjectObject.getInfo(context, DomainConstants.SELECT_NAME);
                    if (UIUtil.isNullOrEmpty(strConnectedRouteTemplateOfMCA)) {
                        domMCAID.addToObject(context, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES), strRouteTemplateFromProgramProjectID);
                    } else if (!strConnectedRouteTemplateOfMCA.equalsIgnoreCase(strRouteTemplateFromProgramProject)) {
                        String strOldRelID = (String) domMCAID.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].id");
                        DomainRelationship.disconnect(context, strOldRelID);
                        if (UIUtil.isNotNullAndNotEmpty(strRouteTemplateFromProgramProjectID)) {
                            domMCAID.addToObject(context, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES), strRouteTemplateFromProgramProjectID);
                        } else {
                            String strError = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                    "PSS_EnterpriseChangeMgt.Alert.RouteTemplateNotPresentOnProgramProject");
                            String strMessage = "Program-Project '" + strProgProjName + strError + strNewValuePurposeOfRelease + "'.";
                            MqlUtil.mqlCommand(context, "notice $1", strMessage);
                            return 1;
                        }

                    }
                }

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkRouteTemplateOfProgramProject: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return 0;

    }

    // PCM TS175 Manufacturing Change Order- Properties layout:17/03/2017: KWagh : START
    public String getPlantRelatedToProject(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map fieldValuesMap = (HashMap) programMap.get(ChangeConstants.FIELD_VALUES);
        StringList slPlantList = new StringList();

        DomainObject domProject = new DomainObject();
        String strProjectID = (String) fieldValuesMap.get("ProjectcodeOID");
        if (UIUtil.isNotNullAndNotEmpty(strProjectID)) {

            domProject.setId(strProjectID);
        }
        // get Plant List

        StringList slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_ID);
        slObjectSelect.add(DomainConstants.SELECT_NAME);
        StringList slRelSelect = new StringList();

        MapList mlPlantList = domProject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY, TigerConstants.TYPE_PSS_PLANT, slObjectSelect, slRelSelect, false, true,
                (short) 1, null, null, (short) 0);
        int ncnt = mlPlantList.size();
        for (int j = 0; j < ncnt; j++) {
            Map mPlant = (Map) mlPlantList.get(j);
            String strPlantName = (String) mPlant.get(DomainConstants.SELECT_NAME);
            slPlantList.add(strPlantName);
        }

        StringBuffer sb = new StringBuffer();
        if (!slPlantList.isEmpty()) {
            for (int i = 0; i < slPlantList.size(); i++) {
                sb.append(slPlantList.get(i));
                sb.append(",");
            }
        }
        return "TYPES=type_PSS_Plant:POLICY=policy_PSS_Plant:Name=" + sb.toString();
    }

    // PCM TS175 Manufacturing Change Order- Properties layout:17/03/2017: KWagh : End

    // PCM RFC-117: 27/03/2017 : START
    // PCM TS175 Manufacturing Change Order- Properties layout:17/03/2017: KWagh : START
    public String get150percentMBOM(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map fieldValuesMap = (HashMap) programMap.get(ChangeConstants.FIELD_VALUES);
        // Map requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);//Fix for FindBugs issue dead local storage : Harika Varanasi : 21 March 2017
        String strURL = DomainConstants.EMPTY_STRING;

        // DomainObject domPlant = new DomainObject();//Fix for FindBugs issue dead local storage : Harika Varanasi : 21 March 2017
        String strPlantID = (String) fieldValuesMap.get("PlantOID");
        if (UIUtil.isNotNullAndNotEmpty(strPlantID)) {

            strURL = "TYPES=type_CreateAssembly:POLICY=policy_PSS_MBOM:CURRENT!=policy_PSS_MBOM.state_Obsolete,policy_PSS_MBOM.state_Approved,policy_PSS_MBOM.state_Cancelled:MPLANT_ID=" + strPlantID;
        }

        return strURL;

    }
    // PCM RFC-117: 27/03/2017 : End
    // PCM TS175 Manufacturing Change Order- Properties layout:17/03/2017: KWagh : End

    // TGPSS_PCM-TS156 Manufacturing Change Action Notifications V_2.2 | 20/03/2017 |Harika Varanasi : Starts
    /**
     * getManufacturingChangeActionInformation method is used to get all information about Manufacturing Change Action As a Part of TGPSS_PCM-TS156 Manufacturing Change Action Notifications V_2.2
     * @param context
     * @param strMCAId
     * @return Map
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public Map getManufacturingChangeActionInformation(Context context, String strMCAId) throws Exception {
        Map mapMCA = new HashMap();
        try {

            if (UIUtil.isNotNullAndNotEmpty(strMCAId)) {
                DomainObject domMCAObj = DomainObject.newInstance(context, strMCAId);

                StringList slObjectSelects = new StringList(13);
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                slObjectSelects.addElement(DomainConstants.SELECT_NAME);
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
                slObjectSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
                slObjectSelects
                        .addElement("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_PHYSICAL_IMPLEMENTATION_PLANNED_DATE + "]");
                slObjectSelects
                        .addElement("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");
                slObjectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.id");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.name");

                // PCM : TIGTK-7745 : 01/09/2017 : AB : START
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.name");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.id");
                // PCM : TIGTK-7745 : 01/09/2017 : AB : END

                mapMCA = domMCAObj.getInfo(context, slObjectSelects);
            }

        } catch (RuntimeException re) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getManufacturingChangeActionInformation: ", re);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getManufacturingChangeActionInformation: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        } finally {
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.name");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.id");
        }

        return mapMCA;
    }

    /**
     * transformMCAMapToHTMLList As a Part of TGPSS_PCM-TS156 Manufacturing Change Action Notifications V_2.2
     * @param context
     * @param objectMap
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public MapList transformMCAMapToHTMLList(Context context, Map objectMap, String strBaseURL, boolean isTaskRejected, boolean isReassign) throws Exception {
        MapList mlInfoList = new MapList();
        try {

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);

            StringList slHyperLinkLabelKey = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.MCANotification.HyperLinkLabelKey"), ",");
            StringList slHyperLinkLabelKeyIds = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.MCANotification.HyperLinkLabelKeyIds"), ",");
            // TIGTK-11455 :START
            if (isReassign) {
                initializeMCALinkedHashMap(isReassign, isTaskRejected, lhmMCACommentsSelectionStore);
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmMCACommentsSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            } else if (isTaskRejected) {
                initializeMCALinkedHashMap(isReassign, isTaskRejected, lhmMCARejectSelectionStore);
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmMCARejectSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            } else {
                initializeMCALinkedHashMap(isReassign, isTaskRejected, lhmMCASelectionStore);
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmMCASelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            }
            // TIGTK-11455 :END
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in transformMCAMapToHTMLList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mlInfoList;
    }

    /**
     * initializeMCALinkedHashMap As a Part of TGPSS_PCM-TS156 Manufacturing Change Action Notifications V_2.2
     * @throws Exception
     *             if the operation fails
     * @author Harika Varanasi : SteepGraph
     */
    public void initializeMCALinkedHashMap(boolean isReassign, boolean isTaskRejected, LinkedHashMap<String, String> lhmMCASelectionStore) throws Exception {
        try {

            if (lhmMCASelectionStore != null && (lhmMCASelectionStore.isEmpty())) {

                lhmMCASelectionStore.put("Title", "SectionHeader");
                lhmMCASelectionStore.put("Subject", "SectionSubject");
                lhmMCASelectionStore.put("Main_Information", "SectionHeader");
                lhmMCASelectionStore.put("Project_Code",
                        "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                lhmMCASelectionStore.put("Project_Description",
                        "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                lhmMCASelectionStore.put("MCA", DomainConstants.SELECT_NAME);
                lhmMCASelectionStore.put("MCA_Description", DomainConstants.SELECT_DESCRIPTION);
                lhmMCASelectionStore.put("State", DomainConstants.SELECT_CURRENT);
                lhmMCASelectionStore.put("Physical_Implementation_Planned_Date",
                        "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_PHYSICAL_IMPLEMENTATION_PLANNED_DATE + "]");
                lhmMCASelectionStore.put("MCA_Plant",
                        "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");
                lhmMCASelectionStore.put("MCA_Creator", "attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                // TIGTK-11455 :START
                if (isReassign) {
                    lhmMCASelectionStore.put("Comments", "Comments");
                }
                if (isTaskRejected) {
                    lhmMCASelectionStore.put("RejectedTask_Comment", "RejectedTask_Comment");
                }
                // TIGTK-11455 :END
                lhmMCASelectionStore.put("Useful_Links", "SectionHeader");
                lhmMCASelectionStore.put("MCA_Content", "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.name");

            }

        } catch (RuntimeException re) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeMCALinkedHashMap: ", re);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeMCALinkedHashMap: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }

    /**
     * getMCANotificationBodyHTML method is used to MCA messageHTML in Notification Object As apart of TGPSS_PCM-TS156 Manufacturing Change Action Notifications V_2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getMCANotificationBodyHTML(Context context, String[] args) throws Exception {
        String messageHTML = "";
        // TIGTK-10709 -- START
        String strSectionSub = DomainConstants.EMPTY_STRING;
        // TIGTK-10709 -- END
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strMCAObjId = (String) programMap.get("id");
            String strBaseURL = (String) programMap.get("baseURL");
            String notificationObjName = (String) programMap.get("notificationName");
            String attrSubText = PropertyUtil.getSchemaProperty(context, "attribute_SubjectText");
            StringList busSelects = new StringList("attribute[" + attrSubText + "]");
            Map payLoadMap = (Map) programMap.get("payload");

            Map mapMCA = (Map) getManufacturingChangeActionInformation(context, strMCAObjId);
            // TIGTK-11455 :START
            if (payLoadMap != null) {
                if (payLoadMap.containsKey("RejectedTask_Comment")) {
                    String strComments = (String) payLoadMap.get("RejectedTask_Comment");
                    mapMCA.put("RejectedTask_Comment", strComments);
                }
                if (payLoadMap.containsKey("Comments")) {
                    String strComments = (String) payLoadMap.get("Comments");
                    mapMCA.put("Comments", strComments);
                }
            }
            // TIGTK-11455 :END
            String strSubjectKey = "";
            MapList mpNotificationList = DomainObject.findObjects(context, "Notification", notificationObjName, "*", "*", TigerConstants.VAULT_ESERVICEADMINISTRATION, "", null, true, busSelects,
                    (short) 0);

            if (mpNotificationList != null && (!mpNotificationList.isEmpty())) {
                strSubjectKey = (String) ((Map) mpNotificationList.get(0)).get("attribute[" + attrSubText + "]");

            }
            if (UIUtil.isNotNullAndNotEmpty(strSubjectKey)) {
                mapMCA.put("SectionSubject", EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strSubjectKey));
            } else {
                mapMCA.put("SectionSubject", "");
            }
            // TIGTK-10709 -- START
            boolean isReassign = false;

            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_MCATaskReassignNotification".equalsIgnoreCase(notificationObjName)) {
                pss.ecm.enoECMChange_mxJPO enoECMObj = new pss.ecm.enoECMChange_mxJPO();
                strSectionSub = enoECMObj.getTaskReassignmentSubject(context, args);
                mapMCA.put("SectionSubject", strSectionSub);
                // TIGTK-11455--START
                isReassign = true;
                // TIGTK-11455--END
            }
            // TIGTK-10709 -- END

            // TIGTK-10768 -- START
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_MCAAssigneeReassignNotification".equalsIgnoreCase(notificationObjName)) {
                pss.ecm.enoECMChange_mxJPO enoECMObj = new pss.ecm.enoECMChange_mxJPO();
                strSectionSub = enoECMObj.getChangeReassignmentSubject(context, args);
                mapMCA.put("SectionSubject", strSectionSub);
            }
            // TIGTK-10768 -- END
            // TIGTK-11455 :START
            boolean isTaskRejected = false;
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_MCATaskRejectionNotification".equalsIgnoreCase(notificationObjName)) {
                isTaskRejected = true;
            }
            MapList mlInfoList = transformMCAMapToHTMLList(context, mapMCA, strBaseURL, isTaskRejected, isReassign);
            // TIGTK-11455 :END
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            String strStyleSheet = commonObj.getFormStyleSheet(context, "PSS_NotificationStyleSheet");
            messageHTML = commonObj.getHTMLTwoColumnTable(context, mlInfoList, strStyleSheet);
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMCANotificationBodyHTML: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return messageHTML;
    }

    // TGPSS_PCM-TS156 Manufacturing Change Action Notifications V_2.2 | 20/03/2017 |Harika Varanasi : Ends

    /**
     * Description : This method is used for get StringList from Map.
     * @author abhalani
     * @args
     * @Date March 24, 2017
     */
    public StringList getStringListFromMap(Context context, Map inputMap, String selectable) throws Exception {
        StringList slOutput = new StringList();

        Object obj = (Object) inputMap.get(selectable);
        if (obj instanceof StringList) {
            slOutput = (StringList) obj;
        } else {
            String temp = (String) obj;
            slOutput.add(temp);
        }

        return slOutput;
        // TODO Auto-generated method stub

    }

    // PCM : 27/03/2017 : JIRA :5837 :PMantri //START
    /**
     * For getting unique stringList from input list
     * @param slInputList
     * @return
     * @throws Exception
     */
    public StringList getUniqueIdList(StringList slInputList) throws Exception {
        StringList slReturnList = new StringList();
        try {

            for (int i = 0; i < slInputList.size(); i++) {

                String objectId = (String) slInputList.get(i);
                if (!slReturnList.contains(objectId)) {
                    slReturnList.add(objectId);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getUniqueIdList: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return slReturnList;
    }
    // PCM : 27/03/2017 : JIRA : 5837:PMantri // END

    /**
     * This Method is used in Access Function for creating/Add existing Reference document from CR.Created by PCM : TIGTK-6102 : 11/04/2017 : AB
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean hasCreateNewDocumentAccessForCR(Context context, String args[]) throws Exception {
        boolean bResult = false;
        try {
            // Get the Toolbar name and other settings from Arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap settingsMap = (HashMap) programMap.get("SETTINGS");
            String strObjId = (String) programMap.get("objectId");
            String strToolbarName = (String) programMap.get("toolbar");
            String strFromRemove = (String) settingsMap.get("FromRemove");
            DomainObject domChange = new DomainObject(strObjId);

            // Get the Change objects state and type
            StringList slChangeInfo = new StringList();
            slChangeInfo.add(DomainConstants.SELECT_CURRENT);
            slChangeInfo.add(DomainConstants.SELECT_TYPE);
            Map mChangeInfo = domChange.getInfo(context, slChangeInfo);
            StringList slStateValues = new StringList();
            String strKey = DomainConstants.EMPTY_STRING;
            String strType = (String) mChangeInfo.get(DomainConstants.SELECT_TYPE);
            String strSymType = PropertyUtil.getAliasForAdmin(context, "Type", strType, false);
            String strCurrent = (String) mChangeInfo.get(DomainConstants.SELECT_CURRENT);

            // Check if the current Change object is Change Request and current state is create or Submit then allow for create/Add existing reference documents
            if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strType)) {
                if (!strToolbarName.equalsIgnoreCase("PSS_ECMSummarySupportingDocsToolbar") && UIUtil.isNullOrEmpty(strFromRemove)) {
                    strKey = "PSS_EnterpriseChangeMgt.ReferenceDocMenuAdd." + strSymType;
                    slStateValues = FrameworkUtil.split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strKey), "|");
                }
            }

            if (slStateValues.contains(strCurrent))
                bResult = true;

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in hasCreateNewDocumentAccessForCR: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return bResult;
    }

    // TIGTK-6191 | 11/04/2017 | Harika Varanasi : Starts
    /**
     * getGenericContextOrLoginUser
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getGenericContextOrLoginUser(Context context, String[] args) throws Exception {
        String strFromAgent = "";
        try {
            strFromAgent = context.getUser();
            if (UIUtil.isNotNullAndNotEmpty(strFromAgent) && strFromAgent.equals(PropertyUtil.getSchemaProperty(context, "person_UserAgent"))) {
                String strLoginedPerson = PropertyUtil.getGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME);
                if (UIUtil.isNotNullAndNotEmpty(strLoginedPerson)) {
                    strFromAgent = strLoginedPerson;
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getGenericContextOrLoginUser: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return strFromAgent;
    }
    // TIGTK-6191 | 11/04/2017 | Harika Varanasi : Starts

    /**
     * Method will be called from MBOM Creation page, As CMM CPM Line Data and Operations are not getting transfer on MCO as well on CR we must skip them for all checks PCM : TIGTK-6561 : 20/04/2017 :
     * AB
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList removeCMMCPMLineDateaOperation(Context context, String[] args) throws Exception {

        StringList slReturn = new StringList();
        try {
            List allowedTypeList = new ArrayList();
            allowedTypeList.add(TigerConstants.TYPE_CREATEASSEMBLY);
            allowedTypeList.add(TigerConstants.TYPE_CREATEKIT);
            allowedTypeList.add(TigerConstants.TYPE_CREATEMATERIAL);

            for (int i = 0; i < args.length; i++) {
                String objectId = args[i];
                DomainObject domObject = new DomainObject(objectId);
                String type = domObject.getInfo(context, DomainObject.SELECT_TYPE);

                if (allowedTypeList.contains(type)) {
                    slReturn.add(objectId);
                }
            }
        } catch (Exception e) {
            logger.error("Error in removeCMMCPMLineDateaOperation: ", e);
            throw e;
        }
        return slReturn;
    }

    /**
     * This method is used to Sort the Affected Items based on sequence number to get the order promotion/demotion
     * @param context
     * @param SlItemOID
     *            -- List of Affected Items
     * @return
     * @throws Exception
     */
    public MapList getOrderedParentChild(Context context, StringList SlItemOID) throws Exception {

        MapList finalMapList = new MapList();
        MapList valueMapList = new MapList();
        String sortdir = "descending";

        if (SlItemOID != null && SlItemOID.size() > 0) {
            CalculateSequenceNumber objCalculateSequenceNumber = new com.matrixone.apps.framework.lifecycle.CalculateSequenceNumber();

            finalMapList = objCalculateSequenceNumber.orderParentChild(context, SlItemOID);
        }
        // Sort the Map based on sequence number to get the order promotion/demotion

        if (finalMapList != null && finalMapList.size() > 0) {
            Iterator itr = finalMapList.iterator();
            while (itr.hasNext()) {
                Map valueMap = (Map) itr.next();
                String objectId = (String) valueMap.get("id");
                Integer compNo = (Integer) valueMap.get("sequence");
                HashMap hmp = new HashMap();
                hmp.put("sequence", compNo.toString());
                hmp.put("id", objectId);
                valueMapList.add(hmp);
            }
            valueMapList.sort("sequence", sortdir, "integer");

        }
        return valueMapList;

    }

    // PCM TIGTK-3509 : 09/11/2016 : KWagh : End

    // TIGTK-9716| 08/09/17 : Start
    /**
     * @author Kwagh
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is used to show Related MCO's Purpose of Release value on MBOM's properties page.
     */
    public String displayPurposeOfReleaseOnMfgPart(Context context, String[] args) throws Exception {
        String strPurposeOfRelease = DomainConstants.EMPTY_STRING;

        try {
            HashMap requestMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) requestMap.get("paramMap");

            String strMBOMID = (String) paramMap.get("objectId");

            DomainObject domMBOM = DomainObject.newInstance(context, strMBOMID);
            // Object Selects
            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_ID);

            // Object Where clause
            String strObjectWhere = "(current != '" + TigerConstants.STATE_PSS_MCA_CANCELLED + "')";

            // Get connected MCAs of MBOM
            MapList mlConnectedMCA = domMBOM.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION,
                    slObjectSelect, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, strObjectWhere, null, (short) 0);

            Iterator itrMCA = mlConnectedMCA.iterator();
            while (itrMCA.hasNext()) {
                Map mMCA = (Map) itrMCA.next();
                String strMCAID = (String) mMCA.get(DomainConstants.SELECT_ID);

                DomainObject domMCA = DomainObject.newInstance(context, strMCAID);

                // Get connected MCOs of MBOM
                MapList mlConnectedMCO = domMCA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, slObjectSelect,
                        DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, strObjectWhere, null, (short) 0);

                Iterator itrMCO = mlConnectedMCO.iterator();
                while (itrMCO.hasNext()) {
                    Map mMCO = (Map) itrMCO.next();
                    String strMCOID = (String) mMCO.get(DomainConstants.SELECT_ID);

                    DomainObject domMCO = DomainObject.newInstance(context, strMCOID);

                    strPurposeOfRelease = domMCO.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);

                }
            }

        } catch (Exception e) {

            logger.error("Error in displayPurposeOfReleaseOnMfgPart: ", e);

        }

        return strPurposeOfRelease;
    }
    // TIGTK-9716| 08/09/17 : End

    // KETAKI WAGH -TIGTK-10700 -Start
    public String getMasterPlantForMCO(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strMBOMID = (String) programMap.get("objectId");

        String strMasterPlantID = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strMBOMID);
        return strMasterPlantID;

    }

    // KETAKI WAGH -TIGTK-10700 -END
    // KETAKI WAGH -TIGTK-10700 -Start
    public boolean checkIfConnectedToDifferentplants(Context context, String[] args) throws Exception {
        boolean bResult = false;
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        StringList slMBOMIDList = (StringList) programMap.get("slList");

        HashSet<String> hsMasterPlantSet = new HashSet<>();
        if (!slMBOMIDList.isEmpty()) {
            Iterator itrMBOMID = slMBOMIDList.iterator();

            while (itrMBOMID.hasNext()) {
                String strMBOMID = (String) itrMBOMID.next();
                String strMasterPlantID = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strMBOMID);
                hsMasterPlantSet.add(strMasterPlantID);

            }
        }

        if (hsMasterPlantSet.size() == 1 && !hsMasterPlantSet.contains(DomainConstants.EMPTY_STRING)) {
            bResult = true;
        }

        return bResult;

    }

    // KETAKI WAGH -TIGTK-10700 -END

    // KETAKI WAGH -TIGTK-10700 -Start

    public StringList getMasterPlantInSearch(Context context, String[] args) throws Exception {
        try {
            String strMPLANT_ID = DomainConstants.EMPTY_STRING;
            String strplantID = DomainConstants.EMPTY_STRING;
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

            String strftsFilters = (String) programMap.get("ftsFilters");

            StringList slftsFilters = FrameworkUtil.split(strftsFilters, ",");

            for (int i = 0; i < slftsFilters.size(); i++) {
                String strftsFiltersElements = (String) slftsFilters.get(i);

                if (strftsFiltersElements.contains("MPLANT_ID")) {
                    strMPLANT_ID = strftsFiltersElements;

                }
            }
            StringList slPlant = FrameworkUtil.split(strMPLANT_ID, "|");
            if (!slPlant.isEmpty()) {
                String strplant = (String) slPlant.get(1);
                strplantID = (strplant.substring(0, strplant.length() - 3));
            }
            // Get all MBOM objects where this "plant" is connected as "Master"

            StringList slMasterPlantList = getConnectedMBOMFromMasterPlant(context, strplantID);
            return slMasterPlantList;
        } catch (Exception e) {
            logger.error("Error in getMasterPlantInSearch: ", e);
            throw e;
        }

    }

    // search all MBOM

    public StringList getConnectedMBOMFromMasterPlant(Context context, String strPlantId) throws Exception {
        StringList slMBOMList = new StringList();
        try {
            Pattern typePattern = new Pattern("MfgProductionPlanning");
            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement("physicalid");
            StringList slRelSelectStmts = new StringList(1);
            slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            String strWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_MANUFACTURINGPLANTEXTPSS_OWNERSHIP + "] == Master";
            DomainObject domPlant = DomainObject.newInstance(context, strPlantId);

            MapList mlMfgProduction = domPlant.getRelatedObjects(context, TigerConstants.RELATIONSHIP_VOWNER, // Relationship
                    // Pattern
                    typePattern.getPattern(), // Object Pattern
                    slObjSelectStmts, // Object Selects
                    slRelSelectStmts, // Relationship Selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    strWhere, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // Post Type Pattern
                    null, null, null);

            if (!mlMfgProduction.isEmpty()) {
                for (int i = 0; i < mlMfgProduction.size(); i++) {
                    Map mapMfgProduction = (Map) mlMfgProduction.get(i);
                    String strMfgProductionPlanningObjId = (String) mapMfgProduction.get("physicalid");
                    String strQuery = "print bus " + strMfgProductionPlanningObjId + " select paths.path.element[0].physicalid dump |;";
                    String strMqlResult = MqlUtil.mqlCommand(context, strQuery, false, false);
                    String strSplitArray[] = strMqlResult.split("\\|");
                    if (strSplitArray.length > 0) {
                        String strMqlResult1 = MqlUtil.mqlCommand(context, "print bus " + strSplitArray[1] + " select id dump |;", false, false);
                        slMBOMList.add(strMqlResult1);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in getConnectedMBOMFromMasterPlant: ", e);
        }
        logger.debug("getConnectedMBOMFromMasterPlant: Consumer Plant Id::" + slMBOMList);
        logger.debug("getConnectedMBOMFromMasterPlant: End ");
        return slMBOMList;
    }

    /**
     * Method called to update PSS_PLANNEDENDDATE of MCA Depending on "PhysicalImplementationPlannedDate" of MCO.
     * @param context
     * @param args
     *            -- args0 -- "PSS_ManufacturingChangeOrder" Object Id -- args1 -- "Physical Implementation Planned Date " updated Value
     * @return void -- Nothing
     * @throws Exception
     */
    public void modifyPlannedEndDateOnManufacturingChangeAction(Context context, String args[]) throws Exception {
        try {
            String strPSSMfgChangeOrderObjectId = args[0];
            String strNewPhysicalImplementationPlannedDate = args[1];

            Date date7DaysBefore = null;

            Date dtPSSMfgChangeOrderPhyImplDate = new Date(strNewPhysicalImplementationPlannedDate);

            long DAY_IN_MS = 1000 * 60 * 60 * 24;
            new Date(System.currentTimeMillis() - (7 * DAY_IN_MS));
            date7DaysBefore = new Date(dtPSSMfgChangeOrderPhyImplDate.getTime() - (7 * DAY_IN_MS));
            Date date = new Date();
            if (date7DaysBefore.before(date)) {
                date7DaysBefore = dtPSSMfgChangeOrderPhyImplDate;
            }

            String strPlannedEndDate = DateFormatUtil.formatDate(context, date7DaysBefore);

            DomainObject domPSSMfgChangeOrderObject = DomainObject.newInstance(context, strPSSMfgChangeOrderObjectId);
            StringList slConnectedMCA = domPSSMfgChangeOrderObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.id");
            if (!slConnectedMCA.isEmpty()) {
                for (int cnt = 0; cnt < slConnectedMCA.size(); cnt++) {
                    String strMCAId = (String) slConnectedMCA.get(cnt);
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    DomainObject domPSSMfgChangeActionObject = DomainObject.newInstance(context, strMCAId);
                    domPSSMfgChangeActionObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PLANNEDENDDATE, strPlannedEndDate);
                    ContextUtil.popContext(context);
                }
            }

        } catch (Exception e) {
            logger.error("Error in modifyPlannedEndDateOnManufacturingChangeAction: ", e);
        }
    }
}