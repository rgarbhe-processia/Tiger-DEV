import matrix.db.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import com.dassault_systemes.enovia.enterprisechangemgt.admin.ECMAdmin;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeAction;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeRequest;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicClassItf;
import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicFactory;
import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicItf;
import com.dassault_systemes.vplm.ResourceAuthoring.interfaces.IVPLMResourceAuthoring;
import com.dassault_systemes.vplm.fctProcessNav.interfaces.IVPLMFctProcessImplementLinkNav;
import com.dassault_systemes.vplm.fctProcessNav.utility.MBOMILRelationInformation;
import com.dassault_systemes.vplm.interfaces.access.IPLMxCoreAccess;
import com.dassault_systemes.vplm.modeler.PLMCoreAbstractModeler;
import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.dassault_systemes.vplm.modeler.PLMxTemplateFactory;
import com.dassault_systemes.vplm.modeler.entity.PLMxRefInstanceEntity;
import com.dassault_systemes.vplm.modeler.entity.PLMxReferenceEntity;
import com.dassault_systemes.vplm.modeler.template.IPLMTemplateContext;
import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.configuration.ConfigurationUtil;
import com.matrixone.apps.configuration.ProductConfiguration;
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
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.effectivity.EffectivityExpression;
import com.matrixone.apps.effectivity.EffectivityFramework;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.plmql.cmd.PLMID;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.mbom.modeler.utility.*;

public class FRCMBOMProg_mxJPO extends FRCMBOMProgBase_mxJPO {
    public FRCMBOMProg_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    public static Vector getMBOMNameHTML(Context context, String[] args) throws Exception { // Called from table FRCMBOMTable (column FRCMBOMCentral.MBOMTableColumnTitle)
        long startTime = System.currentTimeMillis();
        try {
            ContextUtil.startTransaction(context, false);

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            // FRC START - HE5 : Added the part of code to fix the issue #267
            Map paramMap = (HashMap) programMap.get("paramList");
            boolean isexport = false;
            String export = (String) paramMap.get("exportFormat");
            if (export != null) {
                isexport = true;
            }
            // FRC END - HE5 : Added the part of code to fix the issue #267

            MapList objectList = (MapList) programMap.get("objectList");

            List<String> listIDs = new ArrayList<String>();

            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String objectID = (String) mapObjectInfo.get("id");
                listIDs.add(objectID);
            }

            StringList busSelect = new StringList();
            busSelect.add("attribute[PLMEntity.V_Name].value");
            MapList resultInfoML = DomainObject.getInfo(context, listIDs.toArray(new String[0]), busSelect);

            int index = 0;
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String objectID = (String) mapObjectInfo.get("id");

                Map<String, String> resultInfoMap = (Map<String, String>) resultInfoML.get(index);
                index++;

                String objectType = (String) mapObjectInfo.get("type");
                // String objectDisplayStr = MqlUtil.mqlCommand(context, "print bus " + objectID + " select attribute[PLMEntity.V_Name].value dump ' '", false, false);
                String objectDisplayStr = resultInfoMap.get("attribute[PLMEntity.V_Name].value");

                StringBuffer resultSB = new StringBuffer();

                resultSB.append(genObjHTML(context, objectID, objectType, objectDisplayStr, false, false));

                // FRC START - HE5 : Added the part of code to fix the issue #267
                if (isexport) {
                    vecResult.add(objectDisplayStr);
                } else {
                    vecResult.add(resultSB.toString());
                }
                // FRC END - HE5 : Added the part of code to fix the issue #267
            }

            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();
            System.out.println("FRC PERFOS : getMBOMNameHTML : " + (endTime - startTime));

            if (perfoTraces != null) {
                perfoTraces.write("Time spent in getMBOMNameHTML() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }

    }

    public static Vector getDiversityColumn(Context context, String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            // FRC START - HE5 : Added the part of code to fix the issue #267
            Map paramMap = (HashMap) programMap.get("paramList");
            boolean isexport = false;
            String export = (String) paramMap.get("exportFormat");
            if (export != null) {
                isexport = true;
            }
            // FRC END - HE5 : Added the part of code to fix the issue #267

            // Do for each object
            List<String> instanceIDList = new ArrayList<String>();
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String instanceID = (String) mapObjectInfo.get("id[connection]");
                if (instanceID != null && !"".equals(instanceID)) {
                    instanceIDList.add(instanceID);
                }
            }

            Map<String, String> effMap = FRCMBOMModelerUtility.getEffectivityOnInstanceList(context, plmSession, instanceIDList, true);

            String parentOID = (String) paramMap.get("objectId");
            String parentOIDIsManufItem = MqlUtil.mqlCommand(context, "print bus " + parentOID + " select type.kindof[DELFmiFunctionReference] dump |", false, false);

            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                // System.out.println("EPI : mapObjectInfo = " + mapObjectInfo);

                String displayStr = "";
                // boolean isEffUpToDate = true;

                String instanceID = (String) mapObjectInfo.get("id[connection]");
                if (instanceID != null && !"".equals(instanceID)) {
                    displayStr = effMap.get(instanceID);
                }

                StringBuffer cellContentSB = new StringBuffer("");
                cellContentSB.append("<span title=\"" + displayStr + "\"");

                cellContentSB.append(">");
                cellContentSB.append(displayStr);
                cellContentSB.append("</span>");

                // vecResult.add(cellContentSB.toString());
                // FRC START - HE5 : Added the part of code to fix the issue #267
                if (isexport) {
                    vecResult.add(displayStr);
                } else {
                    vecResult.add(cellContentSB.toString());
                }
                // FRC END - HE5 : Added the part of code to fix the issue #267
            }

            ContextUtil.commitTransaction(context);

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();

            if (perfoTraces != null) {
                perfoTraces.write("Time spent in getDiversityColumn() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static Vector get3DShowColumn(Context context, String[] args) throws Exception { // Called by table FRCMBOMTable (column Show3D)
        long startTime = System.currentTimeMillis();

        try {
            ContextUtil.startTransaction(context, false);

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            // FRC START - HE5 : Added the part of code to fix the issue #267
            Map paramMap = (HashMap) programMap.get("paramList");
            boolean isexport = false;
            String export = (String) paramMap.get("exportFormat");
            if (export != null) {
                isexport = true;
            }
            // FRC END - HE5 : Added the part of code to fix the issue #267

            MapList objectList = (MapList) programMap.get("objectList");

            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String level = (String) mapObjectInfo.get("id[level]");
                if (level == null || "".equals(level))
                    level = (String) mapObjectInfo.get("level");

                // FRC START - HE5 : Added the part of code to fix the issue #267
                if (isexport) {
                    vecResult.add(level);
                } else {
                    vecResult.add(
                            "<img border=\"0\" onclick=\"load3DFromMBOMFromRowID('" + level + "')\" src=\"../common/images/iconSmallShowHide3D.gif\" style=\"height: 16px; cursor: pointer\"></img>");
                }
                // FRC END - HE5 : Added the part of code to fix the issue #267
            }

            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();
            if (perfoTraces != null) {
                perfoTraces.write("Time spent in get3DShowColumn() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            return vecResult;
        } catch (Exception exp) {
            long endTime = System.currentTimeMillis();
            if (perfoTraces != null) {
                perfoTraces.write("Time spent in get3DShowColumn() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    // KYB Added method for Feature #247
    public static void createNewResourceItem(Context context, String[] args) throws Exception { // Called from command FRCMBOMCreateNewResourceItem
        PLMCoreModelerSession plmSession = null;

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map paramMap = (HashMap) programMap.get("paramMap");
            String resourceType = (String) paramMap.get("ResourceType");
            String resourceTitle = (String) paramMap.get("Title");
            String resourceTargetCost = (String) paramMap.get("TargetCost");
            String resourceSetUpTimeReduced = (String) paramMap.get("SetUpTimeReduced");
            String resourceSetUpTime = (String) paramMap.get("SetUpTime");
            String resourceTargetedCycleTime = (String) paramMap.get("TargetedCycleTime");
            String resourceCalculatedCycleTime = (String) paramMap.get("CalculatedCycleTime");
            String resourceCycleTime = (String) paramMap.get("CycleTime");
            String resourceMeanTimeToRepair = (String) paramMap.get("MeanTimeToRepair");
            String resourceMeanTimeBTWFailure = (String) paramMap.get("MeanTimeBTWFailure");
            String resourceConsumableCost = (String) paramMap.get("ConsumableCost");
            String resourceEnergyCost = (String) paramMap.get("EnergyCost");
            String resourceMaintenanceCost = (String) paramMap.get("MaintenanceCost");
            String resourceRepairCost = (String) paramMap.get("RepairCost");
            String resourcePurchaseCost = (String) paramMap.get("PurchaseCost");
            String resourceCategory = (String) paramMap.get("Category");
            String resourceManufacturer = (String) paramMap.get("Manufacturer");
            String resourceDescription = (String) paramMap.get("Description");

            // Example getting the context and starting the session
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");

            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            IVPLMResourceAuthoring resourceAuthoringModeler = (IVPLMResourceAuthoring) plmSession.getModeler("com.dassault_systemes.vplm.ResourceAuthoring.implementation.VPLMResourceAuthoring");

            IPLMDictionaryPublicItf dico = new IPLMDictionaryPublicFactory().getDictionary();
            IPLMTemplateContext brContext = PLMxTemplateFactory.newContext((PLMCoreAbstractModeler) resourceAuthoringModeler);
            Context ctx = (Context) brContext.getExecuteRuleCtx();

            IPLMDictionaryPublicClassItf iFuncCreateResourceType = null;
            if ("Worker".equals(resourceType)) {
                iFuncCreateResourceType = dico.getClass(ctx, "ErgoHuman");
            } else {
                iFuncCreateResourceType = dico.getClass(ctx, "VPMReference");
            }

            Hashtable attributes = new Hashtable<>();
            // FRC Start: Fixed Bug 638 'Capable resource Title empty'
            // attributes.put("PLM_ExternalID", resourceType);
            Date date = new Date();
            attributes.put("Ext(PSS_MBOM/PSS_PublishedPart).PSS_PSTime", System.currentTimeMillis() / 1000);
            attributes.put("V_Name", resourceTitle);
            // FRC End: Fixed Bug 638 'Capable resource Title empty'

            PLMxReferenceEntity rsc_Ref = resourceAuthoringModeler.createReferenceFromType(iFuncCreateResourceType, "DELRmiResourceModel/" + resourceType, attributes);

            if (rsc_Ref == null)
                throw new Exception("Create Resource Item failed : returned null.");

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);

            String refPLMIDStr = rsc_Ref.getPLMIdentifier();
            PLMID refPLMID = PLMID.buildFromString(refPLMIDStr);
            String rscObjId = refPLMID.getPid();
            setResourceItemAttributes(context, rscObjId, paramMap);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    private static void setResourceItemAttributes(Context context, String objectId, Map paramMap) throws Exception {
        try {
            HashMap rscAttributes = new HashMap();
            String attrTargetCost = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_TargetCost");
            String attrSetUpTimeReduced = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_SetUpTimeReduced");
            String attrSetUpTime = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_SetUpTime");
            String attrTargetedCycleTime = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_TargetedCycleTime");
            String attrCalculatedCycleTime = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_CalculatedCycleTime");
            String attrCycleTime = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_CycleTime");
            String attrMTTR = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_MTTR");
            String attrMTBF = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_MTBF");
            String attrConsumableCost = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_ConsumableCost");
            String attrEnergyCost = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_EnergyCost");
            String attrMaintenanceCost = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_MaintenanceCost");
            String attrRepairCost = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_RepairCost");
            String attrPurchaseCost = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_PurchaseCost");
            String attrCategory = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_Category");
            String attrManufacturer = PropertyUtil.getSchemaProperty(context, "attribute_Resource.V_Manufacturer");
            String attrDescription = PropertyUtil.getSchemaProperty(context, "attribute_PLMEntity.V_description");

            String resourceType = (String) paramMap.get("ResourceType");
            String resourceTitle = (String) paramMap.get("Title");
            String resourceTargetCost = (String) paramMap.get("TargetCost");
            String resourceSetUpTimeReduced = (String) paramMap.get("SetUpTimeReduced");
            String resourceSetUpTime = (String) paramMap.get("SetUpTime");
            String resourceTargetedCycleTime = (String) paramMap.get("TargetedCycleTime");
            String resourceCalculatedCycleTime = (String) paramMap.get("CalculatedCycleTime");
            String resourceCycleTime = (String) paramMap.get("CycleTime");
            String resourceMeanTimeToRepair = (String) paramMap.get("MeanTimeToRepair");
            String resourceMeanTimeBTWFailure = (String) paramMap.get("MeanTimeBTWFailure");
            String resourceConsumableCost = (String) paramMap.get("ConsumableCost");
            String resourceEnergyCost = (String) paramMap.get("EnergyCost");
            String resourceMaintenanceCost = (String) paramMap.get("MaintenanceCost");
            String resourceRepairCost = (String) paramMap.get("RepairCost");
            String resourcePurchaseCost = (String) paramMap.get("PurchaseCost");
            String resourceCategory = (String) paramMap.get("Category");
            String resourceManufacturer = (String) paramMap.get("Manufacturer");
            String resourceDescription = (String) paramMap.get("Description");

            rscAttributes.put(attrTargetCost, resourceTargetCost);
            rscAttributes.put(attrSetUpTimeReduced, resourceSetUpTimeReduced);
            rscAttributes.put(attrSetUpTime, resourceSetUpTime);
            rscAttributes.put(attrTargetedCycleTime, resourceTargetedCycleTime);
            rscAttributes.put(attrCalculatedCycleTime, resourceCalculatedCycleTime);
            rscAttributes.put(attrCycleTime, resourceCycleTime);
            rscAttributes.put(attrMTTR, resourceMeanTimeToRepair);
            rscAttributes.put(attrMTBF, resourceMeanTimeBTWFailure);
            rscAttributes.put(attrConsumableCost, resourceConsumableCost);
            rscAttributes.put(attrEnergyCost, resourceEnergyCost);
            rscAttributes.put(attrMaintenanceCost, resourceMaintenanceCost);
            rscAttributes.put(attrRepairCost, resourceRepairCost);
            rscAttributes.put(attrPurchaseCost, resourcePurchaseCost);
            rscAttributes.put(attrCategory, resourceCategory);
            rscAttributes.put(attrManufacturer, resourceManufacturer);
            rscAttributes.put(attrDescription, resourceDescription);

            DomainObject domainObject = new DomainObject(objectId);
            domainObject.setAttributeValues(context, rscAttributes);
        } catch (Exception exp) {
            exp.printStackTrace();
            throw exp;
        }
    }

    // FRC : Feature #425 : START
    public String getMBOMPropertiesFieldValues(Context context, String[] args) throws Exception {

        String returnValue = DomainConstants.EMPTY_STRING;

        Map programMap = (HashMap) JPO.unpackArgs(args);
        Map requestMap = (Map) programMap.get("requestMap");
        Map fieldMap = (Map) programMap.get("fieldMap");
        String fieldName = (String) fieldMap.get("name");

        String strObjectId = (String) requestMap.get("objectId");
        DomainObject dmoObject = new DomainObject(strObjectId);

        String typeRelatedAttribute = MqlUtil.mqlCommand(context, "print bus " + strObjectId + " select *");
        if ("Major Revision".equalsIgnoreCase(fieldName)) {
            String majorRev = dmoObject.getInfo(context, "majorrevision");
            returnValue = majorRev;
        } else if ("Project".equalsIgnoreCase(fieldName)) {
            String project = dmoObject.getInfo(context, "project");
            returnValue = project;
        } else if ("Locker".equalsIgnoreCase(fieldName)) {
            String locker = dmoObject.getInfo(context, "locker");
            returnValue = locker;
        } else if ("Is Published".equalsIgnoreCase(fieldName)) {
            String ispublished = dmoObject.getInfo(context, "ispublished");
            returnValue = ispublished;
        } else if ("Is Bestsofar".equalsIgnoreCase(fieldName)) {
            String isbestsofar = dmoObject.getInfo(context, "isbestsofar");
            returnValue = isbestsofar;
        } else if ("Reserved By".equalsIgnoreCase(fieldName)) {
            String reservedby = dmoObject.getInfo(context, "reservedby");
            returnValue = reservedby;
        } else if ("Locked".equalsIgnoreCase(fieldName)) {
            String locked = dmoObject.getInfo(context, "locked");
            returnValue = locked;
        } else if ("Minor Revision".equalsIgnoreCase(fieldName)) {
            String minorrevision = dmoObject.getInfo(context, "minorrevision");
            returnValue = minorrevision;
        } else if ("Reserved".equalsIgnoreCase(fieldName)) {
            String reserved = dmoObject.getInfo(context, "reserved");
            returnValue = reserved;
        } else if ("Organization".equalsIgnoreCase(fieldName)) {
            String organization = dmoObject.getInfo(context, "organization");
            returnValue = organization;
        } else if ("Revision Index".equalsIgnoreCase(fieldName)) {
            String revindex = dmoObject.getInfo(context, "revindex");
            returnValue = revindex;
        } else if ("Collaborative Space".equalsIgnoreCase(fieldName)) {
            String collabProject = dmoObject.getInfo(context, "project");
            returnValue = collabProject;
        } else {
            return "";
        }

        return returnValue;
    }
    // FRC : Feature #425 : END

}
