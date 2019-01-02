package pss.mbom.command;

import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import matrix.db.Context;
import matrix.db.JPO;
import pss.constants.TigerConstants;
import matrix.util.StringList;

import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicClassItf;
import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicFactory;
import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicItf;
import com.dassault_systemes.vplm.ResourceAuthoring.interfaces.IVPLMResourceAuthoring;
import com.dassault_systemes.vplm.modeler.PLMCoreAbstractModeler;
import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.dassault_systemes.vplm.modeler.PLMxTemplateFactory;
import com.dassault_systemes.vplm.modeler.entity.PLMxReferenceEntity;
import com.dassault_systemes.vplm.modeler.template.IPLMTemplateContext;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.plmql.cmd.PLMID;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;

// TIGTK-6755 - 29/06/2017 - TS - START
public class Tooling_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Tooling_mxJPO.class);

    /**
     * Method to Create Tool with PSS_Tool Policy.
     * @param context
     * @param args
     * @return HashMap
     * @throws Exception
     */
    public HashMap createTool(Context context, String args[]) throws Exception {

        String TYPE_SYMBOLIC_VPMREFERENCE = FrameworkUtil.getAliasForAdmin(context, "type", TigerConstants.TYPE_VPMREFERENCE, true);
        String strToolObjId = DomainObject.EMPTY_STRING;
        String strToolObjName = DomainObject.EMPTY_STRING;
        PLMCoreModelerSession plmSession = null;
        boolean activeTransaction = false;

        try {
            HashMap programMap = JPO.unpackArgs(args);
            HashMap returnMap = new HashMap();
            strToolObjName = DomainObject.getAutoGeneratedName(context, TYPE_SYMBOLIC_VPMREFERENCE, TigerConstants.POLICY_PSS_TOOL);
            ContextUtil.startTransaction(context, true);
            activeTransaction = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            IVPLMResourceAuthoring resourceAuthoringModeler = (IVPLMResourceAuthoring) plmSession.getModeler("com.dassault_systemes.vplm.ResourceAuthoring.implementation.VPLMResourceAuthoring");
            IPLMDictionaryPublicFactory ipldmFactory = new IPLMDictionaryPublicFactory();
            IPLMDictionaryPublicItf dico = ipldmFactory.getDictionary();

            IPLMTemplateContext brContext = PLMxTemplateFactory.newContext((PLMCoreAbstractModeler) resourceAuthoringModeler);
            Context ctx = (Context) brContext.getExecuteRuleCtx();

            IPLMDictionaryPublicClassItf iFuncCreateResourceType = dico.getClass(ctx, "VPMReference");

            Hashtable attributes = new Hashtable<>();
            attributes.put("Ext(PSS_MBOM/PSS_PublishedPart).PSS_PSTime", System.currentTimeMillis() / 1000);
            PLMxReferenceEntity rsc_Ref = resourceAuthoringModeler.createReferenceFromType(iFuncCreateResourceType, "PSS_Resources/PSS_Tooling", attributes);

            if (rsc_Ref == null) {
                throw new Exception("Create Tool Item failed : returned null.");
            }
            flushAndCloseSession(plmSession);

            String refPLMIDStr = rsc_Ref.getPLMIdentifier();
            PLMID refPLMID = PLMID.buildFromString(refPLMIDStr);

            strToolObjId = refPLMID.getPid();

            DomainObject domToolObj = DomainObject.newInstance(context, strToolObjId);

            // TIGTK-9597 :START
            String strInterfaceList = MqlUtil.mqlCommand(context, "print bus $1 select interface dump;", strToolObjId);
            StringList slInterfaceList = FrameworkUtil.split(strInterfaceList, ",");
            if (slInterfaceList.contains("PSS_PublishedPart")) {
                MqlUtil.mqlCommand(context, "mod bus $1 remove interface $2;", strToolObjId, "PSS_PublishedPart");
            }
            // TIGTK-9597 :END
            domToolObj.setPolicy(context, TigerConstants.POLICY_PSS_TOOL);
            String strChangeString = "modify bus $1 revision $2 name $3;";
            MqlUtil.mqlCommand(context, strChangeString, strToolObjId, "01.1", strToolObjName);
            domToolObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, strToolObjName);
            domToolObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLM_EXTERNALID, strToolObjName);

            if (activeTransaction) {
                ContextUtil.commitTransaction(context);
            }

            returnMap.put(DomainConstants.SELECT_ID, strToolObjId);
            return returnMap;
        }

        catch (Exception e) {
            logger.error("Error in createTool: ", e);
            if (activeTransaction) {
                ContextUtil.abortTransaction(context);
            }
            throw e;
        }
    }

    /**
     * Method to Close session.
     * @param context
     * @param args
     * @return void
     * @throws Exception
     */

    public static void flushAndCloseSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.flushSession();
        } catch (Exception e) {
            logger.error("Error in flushAndCloseSession: ", e);
        }

        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
            logger.error("Error in flushAndCloseSession: ", e);
        }

    }

    /**
     * Method to connect Tool.
     * @param context
     * @param args
     * @return void
     * @throws Exception
     */
    public void connectToolAsCapableresource(Context context, String[] args) {
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            if (null != programMap) {
                HashMap paramMap = (HashMap) programMap.get("paramMap");
                HashMap requestMap = (HashMap) programMap.get("requestMap");
                String strMfgId = (String) requestMap.get("objectId");
                String strNewToolObjId = (String) paramMap.get("newObjectId");

                if (UIUtil.isNotNullAndNotEmpty(strNewToolObjId) && UIUtil.isNotNullAndNotEmpty(strMfgId)) {

                    List<String> lExistingRsc = FRCMBOMModelerUtility.getResourcesAttachedToMBOMReference(context, plmSession, strMfgId);
                    String lExistingRscStr = lExistingRsc.toString();

                    String sNewPID = "";

                    if (UIUtil.isNotNullAndNotEmpty(strNewToolObjId)) {
                        sNewPID = MqlUtil.mqlCommand(context, "print bus " + strNewToolObjId + " select physicalid dump |", false, false);
                    }

                    if (UIUtil.isNotNullAndNotEmpty(sNewPID) && (!lExistingRscStr.contains(sNewPID))) {
                        FRCMBOMModelerUtility.attachResourceToMBOMReference(context, plmSession, strMfgId, sNewPID);
                    }

                }

            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            logger.error("Error in connectToolAsCapableresource: ", exp);
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
        }
    }

    /**
     * Method to connect Part and Manufacturing Item to Tool.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    // TIGTK-7709 : TS : 29/06/2017 : Start
    public MapList getConnectedPartsAndMfgItems(Context context, String[] args) {
        MapList mlConnectedPartsList = null;
        Boolean bToSide = false;
        Boolean bFromSide = false;
        String FROM = "from";
        PLMCoreModelerSession plmSession = null;

        try {

            HashMap<String, String> programMap = (HashMap) JPO.unpackArgs(args);
            String strToolObjectId = (String) programMap.get("objectId");
            String strTypeName = (String) programMap.get("PSS_TypeName");
            String strRelName = (String) programMap.get("PSS_RelName");
            String strDirection = (String) programMap.get("PSS_Direction");
            String strShowMfgItems = (String) programMap.get("PSS_ShowMfgItems");
            final String RELATIONSHIP_PSS_PARTTOOL = PropertyUtil.getSchemaProperty(context, strRelName);
            StringList slSymbolicTypes = FrameworkUtil.split(strTypeName, ",");
            int slSize = slSymbolicTypes.size();
            StringList slOriginalTypes = new StringList();
            if (!slSymbolicTypes.isEmpty()) {
                for (int i = 0; i < slSize; i++) {
                    slOriginalTypes.addElement(PropertyUtil.getSchemaProperty(context, (String) slSymbolicTypes.get(i)));

                }
            }
            String TYPE_NAME = FrameworkUtil.join(slOriginalTypes, ",");

            DomainObject domToolObject = DomainObject.newInstance(context, strToolObjectId);
            StringList lstSelectStmts = new StringList();
            StringList lstRelStmts = new StringList();

            lstSelectStmts.add(DomainConstants.SELECT_ID);
            lstRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

            if (strDirection.equalsIgnoreCase(FROM)) {
                bFromSide = true;

            } else {
                bToSide = true;

            }

            mlConnectedPartsList = domToolObject.getRelatedObjects(context, RELATIONSHIP_PSS_PARTTOOL, TYPE_NAME, lstSelectStmts, lstRelStmts, bFromSide, bToSide, (short) 1, null, null, 0);

            if (UIUtil.isNotNullAndNotEmpty(strShowMfgItems) && "True".equalsIgnoreCase(strShowMfgItems)) {
                context.setApplication("VPLM");
                plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
                plmSession.openSession();
                String sToolPID = "";
                if (UIUtil.isNotNullAndNotEmpty(strToolObjectId)) {
                    sToolPID = MqlUtil.mqlCommand(context, "print bus " + strToolObjectId + " select physicalid dump |", false, false);
                }

                String strMfgItemId = "";
                if (UIUtil.isNotNullAndNotEmpty(sToolPID)) {
                    String listPathIDStr = MqlUtil.mqlCommand(context, "query path type SemanticRelation containing " + sToolPID + " select id dump |", false, false);

                    if (!"".equals(listPathIDStr)) {

                        String[] listPathID = listPathIDStr.split("\n");
                        Map tempObjMap;
                        for (String pathDesc : listPathID) {
                            String[] aPathDesc = pathDesc.split("\\|");
                            if (null != aPathDesc && 1 < aPathDesc.length) {
                                String pathID = aPathDesc[1];

                                String targetMfgId = MqlUtil.mqlCommand(context, "print path " + pathID + " select owner.to[" + FRCMBOMModelerUtility.REL_VOWNER + "].from.id dump |", false, false);
                                String targetMfgRelId = MqlUtil.mqlCommand(context, "print path " + pathID + " select owner.to[" + FRCMBOMModelerUtility.REL_VOWNER + "].id dump |", false, false);
                                if (null != targetMfgId && !"".equals(targetMfgId)) {
                                    if (mlConnectedPartsList == null) {
                                        mlConnectedPartsList = new MapList();
                                    }
                                    tempObjMap = new HashMap();
                                    tempObjMap.put("id", targetMfgId);
                                    tempObjMap.put("id[connection]", targetMfgRelId);
                                    tempObjMap.put("level", "1");
                                    mlConnectedPartsList.add(tempObjMap);
                                }
                            }
                        }
                    }

                }
                flushAndCloseSession(plmSession);
            }
        } catch (Exception e) {
            logger.error("Error in getConnectedPartsAndMfgItems: ", e);
            flushAndCloseSession(plmSession);
        }
        return mlConnectedPartsList;

    }
    // TIGTK-7709 : TS : 29/06/2017 : End
}
// TIGTK-6755 - 29/06/2017 - TS - END