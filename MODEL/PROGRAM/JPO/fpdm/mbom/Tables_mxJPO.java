package fpdm.mbom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Tables_mxJPO extends fpdm.mbom.Constants_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.mbom.MBOMTable");

    private static final StringList EXPD_MBOM100_BUS_SELECT = new StringList(new String[] { "id", "type", "name", "revision", "attribute.value" });

    private static final StringList EXPD_MBOM100_REL_SELECT = new StringList(new String[] { SELECT_RELATIONSHIP_ID, "attribute.value" });

    /**
     * Constructor
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @throws Exception
     */
    public Tables_mxJPO(Context context, String[] args) throws Exception {
    }

    /**
     * Expand MBOM 100% and return all the linked Plants objects<br>
     * @plm.usage Command: FPDM_MBOM100Plants and table FRCPlantsTable
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public MapList getLinkedPlants(Context context, String[] args) throws Exception {
        MapList resultList = null;

        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sObjectId = (String) programMap.get("objectId");
            DomainObject doMBOM150 = DomainObject.newInstance(context, sObjectId);

            // Call Expand
            String sRelationPattern = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_ScopeLink);
            String sTypePattern = getSchemaProperty(context, SYMBOLIC_type_Plant);
            resultList = doMBOM150.getRelatedObjects(context, sRelationPattern, sTypePattern, new StringList(SELECT_ID), null, false, true, (short) 1, null, null, 0);

        } catch (Exception e) {
            logger.error("Error in getLinkedPlants()\n", e);
            throw e;
        }
        return resultList;
    }

    /**
     * Expand MBOM 100% and return all the linked objects<br>
     * @plm.usage Table: FPDM_MBOM100Table
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public MapList getExpandMBOM100(Context context, String[] args) throws Exception {// Expand program called by the emxIndentedTable.jsp of the MBOM 100 table
        MapList mlMBOM100 = null;

        try {
            HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sObjectId = (String) paramMap.get("objectId");
            logger.debug("getExpandMBOM100() - sObjectId = <" + sObjectId + ">");

            DomainObject doMBOM100 = DomainObject.newInstance(context, sObjectId);

            // Call Expand
            String sRelationPattern = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_MBOMRel);
            String sTypePattern = getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMPart);
            mlMBOM100 = doMBOM100.getRelatedObjects(context, sRelationPattern, sTypePattern, EXPD_MBOM100_BUS_SELECT, EXPD_MBOM100_REL_SELECT, false, true, (short) 0, null, null, 0);

            mlMBOM100.sortStructure("attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_FPDM_FindNumber) + "].value", "ascending", "String");

        } catch (Exception e) {
            logger.error("Error in getExpandMBOM100()\n", e);
            throw e;
        }
        return mlMBOM100;
    }

    /**
     * Expand MBOM 150% and return all the linked MBOM 100% objects<br>
     * @plm.usage Table: FPDM_FRCEBOMMBOMTable
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public MapList getLinkedMBOM100(Context context, String[] args) throws Exception {
        MapList resultList = null;

        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            // logger.debug("getLinkedMBOM100 - programMap <" + programMap + ">");
            String sObjectId = (String) programMap.get("objectId");
            logger.debug("getLinkedMBOM100() - sObjectId = <" + sObjectId + ">");

            DomainObject doMBOM150 = DomainObject.newInstance(context, sObjectId);
            String sRelationPattern = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_GeneratedMBOM);
            String sTypePattern = getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMPart);
            resultList = doMBOM150.getRelatedObjects(context, sRelationPattern, sTypePattern, new StringList(SELECT_ID), new StringList(DomainRelationship.SELECT_ID), false, true, (short) 1, null,
                    null, 0);
        } catch (Exception e) {
            logger.error("Error in getLinkedMBOM100()\n", e);
            throw e;
        }
        return resultList;
    }

    /**
     * Get MBOM name<br>
     * @plm.usage Table: FPDM_FRCEBOMMBOMTable, FPDM_RelatedMBOM100Table Column: Title
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getTitleColumn(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            List<String> listIDs = new ArrayList<String>();
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map<?, ?> mapObjectInfo = (Map<?, ?>) itrObjects.next();

                String objectID = (String) mapObjectInfo.get("id");
                listIDs.add(objectID);
            }
            StringList busSelect = new StringList();
            busSelect.add("attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_FPDM_FCSReference) + "].value");
            busSelect.add("attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PLMEntity_V_Name) + "].value");
            ArrayList<Map<String, Object>> resultInfoML = fpdm.utils.SelectData_mxJPO.getSelectMapList(context, listIDs.toArray(new String[listIDs.size()]), busSelect);

            for (Iterator<Map<String, Object>> itrObjects = resultInfoML.iterator(); itrObjects.hasNext();) {
                Map<String, Object> resultInfoMap = itrObjects.next();

                String sMBOM100Name = (String) resultInfoMap.get("attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_FPDM_FCSReference) + "].value");
                String sMBOM150Name = (String) resultInfoMap.get("attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PLMEntity_V_Name) + "].value");

                if (sMBOM150Name != null && !"".equals(sMBOM150Name)) {
                    vecResult.add(sMBOM150Name);
                } else {
                    vecResult.add(sMBOM100Name);
                }
            }

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getTitleColumn()\n", e);
            throw e;
        }
    }

    /**
     * Get Plant name<br>
     * @plm.usage Table: FPDM_FRCEBOMMBOMTable Column: Plant
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getPlantColumn(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            DomainObject doMBOM = DomainObject.newInstance(context);
            String sObjectID = null;
            String sPlantName = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map<?, ?> mapObjectInfo = (Map<?, ?>) itrObjects.next();
                // get object ID
                sObjectID = getBasicAttributeValueFromMap(context, SELECT_ID, mapObjectInfo);

                doMBOM.setId(sObjectID);

                if (doMBOM.isKindOf(context, getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMPart))) {
                    sPlantName = doMBOM.getInfo(context, "from[" + getSchemaProperty(context, SYMBOLIC_relationship_FPDM_ScopeLink) + "].to.name");
                    if (sPlantName != null) {
                        vecResult.addElement(sPlantName);
                    } else {
                        vecResult.addElement("");
                    }
                } else {
                    sPlantName = getMBOM150LinkedPlant(context, sObjectID);
                    vecResult.addElement(sPlantName);
                }

            }

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getPlantColumn()\n", e);
            throw e;
        }
    }

    /**
     * Get MBOM Effectivity date<br>
     * @plm.usage Table: FPDM_FRCEBOMMBOMTable Column: EffectivityDate
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getEffectivityDateColumn(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            DomainObject doMBOM = DomainObject.newInstance(context);
            String sObjectID = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map<?, ?> mapObjectInfo = (Map<?, ?>) itrObjects.next();
                // get object ID
                sObjectID = getBasicAttributeValueFromMap(context, SELECT_ID, mapObjectInfo);

                doMBOM.setId(sObjectID);

                if (doMBOM.isKindOf(context, getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMPart))) {
                    vecResult.addElement(doMBOM.getInfo(context, "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_FPDM_EffectivityDate) + "].value"));
                } else {
                    vecResult.addElement(doMBOM.getInfo(context, "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PLMReference_V_ApplicabilityDate) + "].value"));
                }

            }

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getEffectivityDateColumn()\n", e);
            throw e;
        }
    }

    /**
     * Get MBOM 150 % linked Plant
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sMBOM150Id
     *            The Manufacturing 150% ID
     * @return
     * @throws Exception
     */
    private String getMBOM150LinkedPlant(Context context, String sMBOM150Id) throws Exception {
        StringBuilder sbPlantName = new StringBuilder();
        ArrayList<String> alMasterPlants = new ArrayList<String>();
        ArrayList<String> alConsumerPlants = new ArrayList<String>();

        if (sMBOM150Id != null && !"".equals(sMBOM150Id)) {
            String sPhysicalID = MqlUtil.mqlCommand(context, "print bus " + sMBOM150Id + " select physicalid dump |", false, false);
            StringBuilder sbQuery = new StringBuilder("query path type SemanticRelation containing ");
            sbQuery.append(sPhysicalID);
            sbQuery.append(" where owner.type=='").append(getSchemaProperty(context, SYMBOLIC_type_MfgProductionPlanning)).append("'");
            sbQuery.append(" select owner.attribute[").append("PSS_ManufacturingPlantExt.PSS_Ownership").append("].value");
            sbQuery.append(" owner.to[").append(getSchemaProperty(context, SYMBOLIC_relationship_VPLMrelPLMConnectionV_Owner)).append("].from.name");
            sbQuery.append(" owner.to[").append(getSchemaProperty(context, SYMBOLIC_relationship_VPLMrelPLMConnectionV_Owner)).append("].from.type dump |");

            String sResult = MqlUtil.mqlCommand(context, sbQuery.toString(), false, false);
            logger.debug("getMBOM150LinkedPlant() - sResult = <" + sResult + ">");

            if (sResult != null && !"".equals(sResult)) {
                String[] sResultArray = sResult.split("[\n]");
                for (int i = 0; i < sResultArray.length; i++) {
                    String[] sPlantInfos = sResultArray[i].split("[|]");
                    if (sPlantInfos.length > 3) {
                        if (TigerConstants.TYPE_PSS_PLANT.equals(sPlantInfos[3])) {
                            if ("Master".equals(sPlantInfos[1])) {
                                alMasterPlants.add(sPlantInfos[2]);
                            } else if ("Consumer".equals(sPlantInfos[1])) {
                                alConsumerPlants.add(sPlantInfos[2]);
                            }
                        }
                    }
                }
            }
        }
        logger.debug("getMBOM150LinkedPlant() - alMasterPlants = <" + alMasterPlants + "> alConsumerPlants = <" + alConsumerPlants + ">");
        sbPlantName.append("<b>");
        sbPlantName.append(StringUtils.join(alMasterPlants, ','));
        sbPlantName.append("</b>");
        if (alConsumerPlants.size() > 0) {
            sbPlantName.append(" (");
            sbPlantName.append(StringUtils.join(alConsumerPlants, ','));
            sbPlantName.append(")");
        }

        return sbPlantName.toString();

    }

    /**
     * Expand MBOM 100% and return all the linked Change Notice objects<br>
     * @plm.usage Command: FPDM_MBOMPartRelatedCNs and table PSS_RelatedCNsSummaryTable
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public MapList getLinkedCNs(Context context, String[] args) throws Exception {
        MapList resultList = null;

        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String sObjectId = (String) programMap.get("objectId");
            DomainObject doMBOM100 = DomainObject.newInstance(context, sObjectId);

            // Call Expand
            String sRelationPattern = getSchemaProperty(context, SYMBOLIC_relationship_FPDM_CNAffectedItems);
            String sTypePattern = getSchemaProperty(context, SYMBOLIC_type_PSS_ChangeNotice);
            resultList = doMBOM100.getRelatedObjects(context, sRelationPattern, sTypePattern, new StringList(SELECT_ID), null, true, false, (short) 1, null, null, 0);

        } catch (Exception e) {
            logger.error("Error in getLinkedCNs()\n", e);
            throw e;
        }
        return resultList;
    }

    /**
     * Get MBOM Revision<br>
     * @plm.usage Table: FPDM_MBOM100Table Column: Revision
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getRevisionColumn(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            String sRevision = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map<?, ?> mapObjectInfo = (Map<?, ?>) itrObjects.next();
                // get object ID
                sRevision = getBasicAttributeValueFromMap(context, SELECT_REVISION, mapObjectInfo);

                if (sRevision.contains(".")) {
                    vecResult.addElement(sRevision.substring(0, 2));
                } else {
                    vecResult.addElement(sRevision);
                }
            }

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getRevisionColumn()\n", e);
            throw e;
        }
    }

    /**
     * Get the engineering Part revision associated to the Manufacturing Part (MBOM 150%)
     * @plm.usage Column: PartRevision of the table FPDM_FRCEBOMMBOMTable
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public static Vector<String> getAssociatedPartRevision(Context context, String[] args) throws Exception { // Called from table FRCEBOMMBOMTable (column FRCMBOMCentral.MBOMTableColumnRevision)
        // Create result vector
        Vector<String> vecResult = new Vector<String>();
        com.dassault_systemes.vplm.modeler.PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = com.dassault_systemes.vplm.modeler.PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            List<String> slMBOMIds = new ArrayList<String>();
            Map<?, ?> mBOM = null;
            for (Object oMBOM : objectList) {
                mBOM = (Map<?, ?>) oMBOM;
                slMBOMIds.add((String) mBOM.get(SELECT_ID));
            }
            logger.debug("getAssociatedPartRevision() - slMBOMIds = <" + slMBOMIds + ">");
            if (!slMBOMIds.isEmpty()) {
                List<String> psRefPIDList = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, slMBOMIds);
                logger.debug("getAssociatedPartRevision() - psRefPIDList = <" + psRefPIDList + ">");
                if (psRefPIDList != null && !(psRefPIDList.isEmpty())) {
                    String sObjectID = null;
                    for (int i = 0; i < psRefPIDList.size(); i++) {
                        sObjectID = (String) psRefPIDList.get(i);
                        if (!"".equals(sObjectID)) {
                            DomainObject doVPMReference = DomainObject.newInstance(context, (String) psRefPIDList.get(i));
                            String strRevision = doVPMReference.getInfo(context, "majorrevision");
                            vecResult.add("<div>" + strRevision + "</div>");
                        } else {
                            vecResult.add("<div>" + DomainConstants.EMPTY_STRING + "</div>");
                        }
                    }
                }
            }

            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            logger.error("Error in getAssociatedPartRevision()\n", e);
            ContextUtil.abortTransaction(context);
            throw e;
        } finally {
            try {
                if (plmSession != null) {
                    plmSession.closeSession(true);
                }
            } catch (com.dassault_systemes.vplm.modeler.exception.PLMxModelerException e) {
            }
        }

        return vecResult;
    }

    /**
     * Get MBOM Product Configuration description<br>
     * @plm.usage Table: FPDM_FRCEBOMMBOMTable Column: PCDescription
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getPCDescriptionColumn(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            DomainObject doMBOM = DomainObject.newInstance(context);
            DomainObject doPC = DomainObject.newInstance(context);
            String sObjectID = null;
            String sPCID = null;
            String sPCDescription = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map<?, ?> mapObjectInfo = (Map<?, ?>) itrObjects.next();
                // get object ID
                sObjectID = getBasicAttributeValueFromMap(context, SELECT_ID, mapObjectInfo);

                doMBOM.setId(sObjectID);

                // get PC object ID
                sPCID = doMBOM.getInfo(context, "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_FPDM_ProductConfigurationId) + "].value");
                logger.warn("getPCDescriptionColumn() - sPCID = <" + sPCID + ">");

                sPCDescription = "";
                if (sPCID != null && !"".equals(sPCID)) {
                    doPC.setId(sPCID);
                    sPCDescription = doPC.getInfo(context, SELECT_DESCRIPTION);
                }

                vecResult.addElement(sPCDescription);
            }

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getPCDescriptionColumn()\n", e);
            throw e;
        }
    }

    /**
     * Get MBOM Faurecia Short Description<br>
     * @plm.usage Table: FPDM_FRCEBOMMBOMTable Column: Description
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getFaureciaShortDescriptionColumn(Context context, String[] args) throws Exception {
        try {
            // Create result vector
            Vector<String> vecResult = new Vector<String>();

            // Get object list information from packed arguments
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            DomainObject doMBOM = DomainObject.newInstance(context);
            String sObjectID = null;
            // Do for each object
            for (Iterator<?> itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map<?, ?> mapObjectInfo = (Map<?, ?>) itrObjects.next();
                // get object ID
                sObjectID = getBasicAttributeValueFromMap(context, SELECT_ID, mapObjectInfo);

                doMBOM.setId(sObjectID);

                if (doMBOM.isKindOf(context, getSchemaProperty(context, SYMBOLIC_type_FPDM_MBOMPart))) {
                    vecResult.addElement(doMBOM.getInfo(context, "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_FPDM_FaureciaShortLengthDescriptionFCS) + "].value"));
                } else {
                    vecResult.addElement(doMBOM.getInfo(context, "attribute[" + getSchemaProperty(context, SYMBOLIC_attribute_PLMEntity_V_description) + "].value"));
                }

            }

            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getFaureciaShortDescriptionColumn()\n", e);
            throw e;
        }
    }

}
