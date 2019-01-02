import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matrix.db.Context;
import matrix.util.StringList;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import dsis.com.service.DBOperation;
import dsis.com.service.DBServices;
import dsis.com.xpdmapp.XPDMXMLGenerator;

public class emxXPDM_mxJPO {

    /**
     * Trigger method for Part Added Use Case (On Rel EBOM : Create: Action)
     * @param data
     * @return
     */
    public void updateCustomMappingTableOnPartAdd(Context context, String[] args) throws Exception {

        try {
            System.out.println("updateCustomMappingTable On Part Add..............START");
            String strInstanceId = args[0];
            String strInstanceType = args[1];
            String strParentType = args[3];
            String strParentName = args[4];
            String strParentRevision = args[5];
            String strChildType = args[6];
            String strChildName = args[7];
            String strChildRevision = args[8];

            if (UIUtil.isNotNullAndNotEmpty(strInstanceId)) {
                String strLastModified = getLastModifiedForConnection(context, strInstanceId);

                HashMap<String, String> mpRecordToAdd = new HashMap<String, String>(12);
                mpRecordToAdd.put("parentType", strParentType);
                mpRecordToAdd.put("parentName", strParentName);
                mpRecordToAdd.put("parentRevision", strParentRevision);
                mpRecordToAdd.put("Type", strChildType);
                mpRecordToAdd.put("Name", strChildName);
                mpRecordToAdd.put("Revision", strChildRevision);
                mpRecordToAdd.put("modified", strLastModified);
                mpRecordToAdd.put("InstanceType", strInstanceType);
                mpRecordToAdd.put("InstanceName", strInstanceId);
                mpRecordToAdd.put("InstanceRevision", DomainConstants.EMPTY_STRING);
                System.out.println("mpRecordToADD" + mpRecordToAdd);

                Map mpUpdateValues = new HashMap(4);
                mpUpdateValues.put("ISUPTODATE", "FALSE");
                mpUpdateValues.put("SOURCELASTMODIFDATE", strLastModified);
                mpUpdateValues.put(DBServices.ISEFFECTIVITYMODIFIED, "TRUE");

                System.out.println("mpUpdateValues" + mpUpdateValues);
                DBOperation DBOper = new DBOperation();
                DBOper.createInstance(mpRecordToAdd, mpUpdateValues);

                System.out.println("updateCustomMappingTable On Part Add..............END");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }
    }

    /**
     * Trigger method for Part Promote Use Case (On Policy EC Part)
     * @param data
     * @return
     */
    public void updateCustomMappingRecordOnPromote(Context context, String[] args) throws Exception {

        try {
            System.out.println("Updating custom mapping table on promotion.........Start.....");
            String strType = args[0];
            String strName = args[1];
            String strRevision = args[2];
            String strObjectId = args[3];

            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                String strLastModified = getLastModifiedForObject(context, strObjectId);

                HashMap<String, String> mpRecordToUpdate = new HashMap<String, String>(5);
                mpRecordToUpdate.put("Type", strType);
                mpRecordToUpdate.put("Name", strName);
                mpRecordToUpdate.put("Revision", strRevision);
                mpRecordToUpdate.put("modified", strLastModified);

                Map mpUpdateValues = new HashMap(3);
                mpUpdateValues.put("ISUPTODATE", "FALSE");
                mpUpdateValues.put("SOURCELASTMODIFDATE", strLastModified);

                System.out.println("mpRecordToUpdate" + mpRecordToUpdate);
                System.out.println("mpUpdateValues" + mpUpdateValues);

                DBOperation DBOper = new DBOperation();
                DBOper.updateTablesOnReferenceModification(mpRecordToUpdate, mpUpdateValues);

                System.out.println("Updating custom mapping table on promotion............END");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }
    }

    /**
     * Trigger method for Part Remove Use Case (On Rel EBOM : Delete : Action)
     * @param data
     * @return
     */
    public void updateCustomMappingRecodOnEBOMDisconnection(Context context, String args[]) throws Exception {
        System.out.println("Updating Custom Mapping table after removal of part.......");
        try {
            String strInstanceId = args[0];
            String strParentType = args[1];
            String strParentName = args[2];
            String strParentRevision = args[3];
            String strChildType = args[4];
            String strChildName = args[5];
            String strChildRevision = args[6];

            if (UIUtil.isNotNullAndNotEmpty(strInstanceId)) {
                String strLastModified = getLastModifiedForConnection(context, strInstanceId);

                HashMap<String, String> mpRecordToRemove = new HashMap<String, String>(11);
                mpRecordToRemove.put("parentType", strParentType);
                mpRecordToRemove.put("parentName", strParentName);
                mpRecordToRemove.put("parentRevision", strParentRevision);
                mpRecordToRemove.put("Type", strChildType);
                mpRecordToRemove.put("Name", strChildName);
                mpRecordToRemove.put("Revision", strChildRevision);
                mpRecordToRemove.put("InstanceType", DomainConstants.EMPTY_STRING);
                mpRecordToRemove.put("InstanceName", strInstanceId);
                mpRecordToRemove.put("InstanceRevision", DomainConstants.EMPTY_STRING);
                mpRecordToRemove.put("modified", strLastModified);
                System.out.println("mpRecordToRemove" + mpRecordToRemove);

                Map mpUpdateValues = new HashMap(4);
                mpUpdateValues.put("ISUPTODATE", "FALSE");
                mpUpdateValues.put("SOURCELASTMODIFDATE", strLastModified);
                mpUpdateValues.put(DBServices.ISDELETED, "TRUE");
                System.out.println("mpUpdateValues" + mpUpdateValues);

                DBOperation DBOper = new DBOperation();
                DBOper.removeInstanceOnPartRemove(mpRecordToRemove, mpUpdateValues);
                System.out.println("Updation of Custom Mapping table completed.....");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }
    }

    /**
     * Get LastModified Date for Connection
     * @param strInstanceId
     * @return String
     */

    public String getLastModifiedForConnection(Context context, String strInstanceId) {
        String strLastModified = DomainConstants.EMPTY_STRING;
        try {
            String[] arr = { strInstanceId };
            MapList mlRel = DomainRelationship.getInfo(context, arr, new StringList(DomainConstants.SELECT_MODIFIED));
            if (mlRel != null) {
                Map mpRel = (Map) mlRel.get(0);
                strLastModified = (String) mpRel.get(DomainConstants.SELECT_MODIFIED);
            }
            System.out.println("LastModified for EBOM connection : " + strLastModified);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strLastModified;
    }

    /**
     * Get LastModified Date for Object
     * @param strInstanceId
     * @return String
     */

    public String getLastModifiedForObject(Context context, String strObjectId) {
        String strLastModified = DomainConstants.EMPTY_STRING;
        try {
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domObj = DomainObject.newInstance(context, strObjectId);
                strLastModified = domObj.getInfo(context, DomainConstants.SELECT_MODIFIED);
            }
            System.out.println("LastModified for Object : " + strLastModified);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strLastModified;
    }

    /**
     * Trigger Method for EBOM Modify Attribute(Instance Renaming) Use Case (On Rel EBOM )
     * @param data
     * @return
     */

    public void updateCustomMappingRecodOnEBOMModifyAttribute(Context context, String args[]) throws Exception {
        System.out.println("Updating Custom Mapping table after EBOMModifyAttribute.....");
        try {
            String strInstanceId = args[0];
            String strInstanceType = args[1];
            String strParentType = args[3];
            String strParentName = args[4];
            String strParentRevision = args[5];
            String strChildType = args[6];
            String strChildName = args[7];
            String strChildRevision = args[8];
            String strAttrValue = args[9];
            Map mpUpdateValues = new HashMap(5);

            String strEvent = args[10];
            System.out.println("strAttrValue" + strAttrValue);
            System.out.println("strEvent" + strEvent);

            StringList slEffectivityAttr = getEffectivityAttributes();

            if (UIUtil.isNotNullAndNotEmpty(strAttrValue) && null != slEffectivityAttr && !slEffectivityAttr.isEmpty()) {
                if (slEffectivityAttr.contains(strAttrValue)) {
                    mpUpdateValues.put("ISEFFECTIVITYMODIFIED", "TRUE");
                }
            }
            if (UIUtil.isNotNullAndNotEmpty(strInstanceId)) {

                String strLastModified = getLastModifiedForConnection(context, strInstanceId);

                HashMap<String, String> mpRecordToAdd = new HashMap<String, String>(11);
                mpRecordToAdd.put("parentType", strParentType);
                mpRecordToAdd.put("parentName", strParentName);
                mpRecordToAdd.put("parentRevision", strParentRevision);
                mpRecordToAdd.put("Type", strChildType);
                mpRecordToAdd.put("Name", strChildName);
                mpRecordToAdd.put("Revision", strChildRevision);
                mpRecordToAdd.put("modified", strLastModified);
                mpRecordToAdd.put("InstanceType", strInstanceType);
                mpRecordToAdd.put("InstanceName", strInstanceId);
                mpRecordToAdd.put("InstanceRevision", DomainConstants.EMPTY_STRING);

                mpUpdateValues.put("ISUPTODATE", "FALSE");
                mpUpdateValues.put("SOURCELASTMODIFDATE", strLastModified);

                System.out.println("mpRecordToADD" + mpRecordToAdd);
                System.out.println("mpUpdateValues" + mpUpdateValues);

                DBOperation DBOper = new DBOperation();
                DBOper.updateInstance(mpRecordToAdd, mpUpdateValues);
                System.out.println("Updation of Custom Mapping table completed...EBOMModifyAttribute..");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }
    }

    /**
     * Method to get Effectivity Attributes
     * @return StringList
     */
    public StringList getEffectivityAttributes() {
        StringList slEffectivityAttr = new StringList(10);
        slEffectivityAttr.add("Effectivity Compiled Form");
        slEffectivityAttr.add("Effectivity Expression");
        slEffectivityAttr.add("Effectivity Expression Binary");
        slEffectivityAttr.add("Effectivity Ordered Criteria");
        slEffectivityAttr.add("Effectivity Ordered Criteria Dictionary");
        slEffectivityAttr.add("Effectivity Ordered Impacting Criteria");
        slEffectivityAttr.add("Effectivity Proposed Expression");
        slEffectivityAttr.add("Effectivity Types");
        slEffectivityAttr.add("Effectivity Variable Indexes");
        return slEffectivityAttr;
    }

    /**
     * Method for Extraction
     * @param data
     * @return
     */
    public void extractXPDM(Context context, String[] args) {

        try {
            System.out.println("\n-> Prereqs validation started.");
            if (!XPDMXMLGenerator.validateBeforeExtraction()) {
                System.out.println("\n\t-> Could not start extraction. Please check prereqs...");
                return;
            }
            System.out.println("\n-> Prereqs validation completed. All good!");

            DBOperation dbOp = new DBOperation();
            List<Map<String, String>> mlRecords = dbOp.getNonUpdatedRecords();

            if (!mlRecords.isEmpty()) {
                for (int i = 0; i < mlRecords.size(); i++) {
                    Map mpRecord = (Map) mlRecords.get(i);
                    String strType = (String) mpRecord.get(DBServices.TYPE);
                    String strName = (String) mpRecord.get(DBServices.PARTNUMBER);
                    String strRevision = (String) mpRecord.get(DBServices.VERSION);
                    String isEffectivityModified = (String) mpRecord.get(DBServices.ISEFFECTIVITYMODIFIED);
                    String isCGR = (String) mpRecord.get(DBServices.EXTRACTIONTYPE);
                    boolean isDeleted = Boolean.parseBoolean((String) mpRecord.get(DBServices.ISDELETED));

                    System.out.println("isDeleted Value :" + isDeleted);
                    XPDMXMLGenerator gen = new XPDMXMLGenerator(context, true, "CGR".equalsIgnoreCase(isCGR), isDeleted, "TRUE".equalsIgnoreCase(isEffectivityModified));
                    gen.generate(strType, strName, strRevision);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
            // throw e;
        }
    }

    /**
     * Trigger for Part 3D Design Update Use Case (On Relationship CAD SubComponent )
     * @param data
     * @return
     */
    // Rmoved Trigger on CAD SUBComponent Hence following method not required : Remove it later
    /*
     * public void modifyCustomMappingTableOnRelCADSubComponent(Context context, String args[]) throws Exception { try { System.out.println(
     * "Updating Custom Mapping Table After CAD Design change..START....");
     * 
     * String strInstanceId = args[0]; String strParentId = args[1]; String strChildId = args[2]; String strLastModified = DomainConstants.EMPTY_STRING; StringList slSelects = new StringList(2);
     * slSelects.add("to.attribute[Is Version Object]"); String isVersion = DomainConstants.EMPTY_STRING;
     * 
     * if (UIUtil.isNotNullAndNotEmpty(strInstanceId)) { String strArr[] = { strInstanceId }; MapList mlRelData = DomainRelationship.getInfo(context, strArr, slSelects); if (null != mlRelData &&
     * !mlRelData.isEmpty()) { Map mpObj = (Map) mlRelData.get(0); isVersion = (String) mpObj.get("to.attribute[Is Version Object]"); } if (("False").equalsIgnoreCase(isVersion)) { if
     * (UIUtil.isNotNullAndNotEmpty(strParentId) && UIUtil.isNotNullAndNotEmpty(strChildId)) { HashMap mpParent = getPartFromCATPart(context, strParentId); HashMap mpChild =
     * getPartFromCATPart(context, strChildId); strParentId = (String) mpParent.get("Id"); strChildId = (String) mpChild.get("Id"); if (UIUtil.isNotNullAndNotEmpty(strParentId) &&
     * UIUtil.isNotNullAndNotEmpty(strChildId)) { String strEBOMRelId = getEBOMId(context, strParentId, strChildId); strLastModified = (String) mpParent.get(DomainConstants.SELECT_MODIFIED); if
     * (UIUtil.isNotNullAndNotEmpty(strEBOMRelId)) { HashMap mpRecordToUpdate = new HashMap(3); mpRecordToUpdate.put("ParentData", mpParent); mpRecordToUpdate.put("ChildData", mpChild);
     * mpRecordToUpdate.put("EBOMId", strEBOMRelId);
     * 
     * Map mpUpdateValues = new HashMap(2); mpUpdateValues.put("ISUPTODATE", "FALSE"); mpUpdateValues.put("SOURCELASTMODIFDATE", strLastModified);
     * 
     * System.out.println("mpUpdateValues" + mpUpdateValues);
     * 
     * DBOperation DBOper = new DBOperation(); DBOper.updateTablesOnInstanceModification(mpRecordToUpdate, mpUpdateValues); } } } }
     * 
     * } System.out.println("Updating Custom Mapping Table After CAD Design change..END....");
     * 
     * } catch (Exception e) { e.printStackTrace(); // throw e; System.out.println("Error: " + e); } }
     */

    /**
     * Trigger for 3D Design Update Use Case & Part Modify Attributes (On Type CATPART, CATProduct, Part )
     * @param context
     * @param args
     * @throws Exception
     */
    // Split Part & CATPart Trigger hence no need of following method : Remove it later
    /*
     * public void modifyCustomMappingTableOnTypePart(Context context, String args[]) throws Exception { try { System.out.println("modifyCustomMappingTable On CAD object And Part.....START......");
     * String strType = args[1]; String strName = args[2]; String strRevision = args[3]; String strObjectId = args[0]; String strLastModified = DomainConstants.EMPTY_STRING; HashMap mpRecordToUpdate =
     * new HashMap(6); if (UIUtil.isNotNullAndNotEmpty(strType) && (strType.equals("CATPart") || strType.equals("CATProduct"))) { System.out.println("--------------CAD SIDE----------------------"); if
     * (UIUtil.isNotNullAndNotEmpty(strObjectId)) { mpRecordToUpdate = getPartFromCATPart(context, strObjectId); strLastModified = (String) mpRecordToUpdate.get(DomainConstants.SELECT_MODIFIED);
     * System.out.println("CAD To EBOM DATA ==-" + mpRecordToUpdate); } } else if (UIUtil.isNotNullAndNotEmpty(strType) && strType.equals(DomainObject.TYPE_PART)) {
     * 
     * if (UIUtil.isNotNullAndNotEmpty(strObjectId)) { DomainObject domObj = DomainObject.newInstance(context, strObjectId); strLastModified = domObj.getInfo(context, DomainConstants.SELECT_MODIFIED);
     * } mpRecordToUpdate.put("Type", strType); mpRecordToUpdate.put("Id", strObjectId); mpRecordToUpdate.put("Name", strName); mpRecordToUpdate.put("Revision", strRevision);
     * mpRecordToUpdate.put("modified", strLastModified); System.out.println("EBOM DATA ==-" + mpRecordToUpdate); } Map mpUpdateValues = new HashMap(2); mpUpdateValues.put("ISUPTODATE", "FALSE");
     * mpUpdateValues.put("SOURCELASTMODIFDATE", strLastModified);
     * 
     * String strPartId = (String) mpRecordToUpdate.get("Id");
     * 
     * System.out.println("mpRecordToUpdate.." + mpRecordToUpdate); System.out.println("mpUpdateValues.." + mpUpdateValues); System.out.println("strPartId.." + strPartId);
     * 
     * if (UIUtil.isNotNullAndNotEmpty(strPartId)) { DBOperation DBOper = new DBOperation(); System.out.println("DB Operation object created....");
     * DBOper.updateTablesOnReferenceModification(mpRecordToUpdate, mpUpdateValues); System.out.println("DB Operation updation done .."); }
     * 
     * System.out.println("modifyCustomMappingTable On CAD object And Part.....END......"); } catch (Exception e) { e.printStackTrace(); System.out.println("Error: " + e); }
     * 
     * }
     */

    /**
     * Trigger for Part Modify Attributes (On Part )
     * @param context
     * @param args
     * @throws Exception
     */
    public void modifyCustomMappingTableOnTypePart(Context context, String args[]) throws Exception {
        try {

            System.out.println("--------------------------------------------------------------------");
            System.out.println("modifyCustomMappingTable On CAD object And Part.....START......");
            String strType = args[1];
            String strName = args[2];
            String strRevision = args[3];
            String strObjectId = args[0];

            String strATTRNAMEs = args[4];
            System.out.println("strATTRNAMEs ==-" + strATTRNAMEs);
            System.out.println("strType ==-" + strType);
            System.out.println("strName ==-" + strName);
            System.out.println("strRevision ==-" + strRevision);
            System.out.println("strObjectId ==-" + strObjectId);

            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                String strLastModified = getLastModifiedForObject(context, strObjectId);

                HashMap mpRecordToUpdate = new HashMap(6);
                mpRecordToUpdate.put("Type", strType);
                mpRecordToUpdate.put("Id", strObjectId);
                mpRecordToUpdate.put("Name", strName);
                mpRecordToUpdate.put("Revision", strRevision);
                mpRecordToUpdate.put("modified", strLastModified);
                System.out.println("EBOM DATA ==-" + mpRecordToUpdate);

                Map mpUpdateValues = new HashMap(3);
                mpUpdateValues.put("ISUPTODATE", "FALSE");
                mpUpdateValues.put("SOURCELASTMODIFDATE", strLastModified);

                System.out.println("mpRecordToUpdate.." + mpRecordToUpdate);
                System.out.println("mpUpdateValues.." + mpUpdateValues);

                if (null != mpRecordToUpdate && !mpRecordToUpdate.equals("")) {
                    DBOperation DBOper = new DBOperation();
                    System.out.println("DB Operation object created....");
                    DBOper.updateTablesOnReferenceModification(mpRecordToUpdate, mpUpdateValues);
                    System.out.println("DB Operation updation done ..");
                }
            }
            System.out.println("modifyCustomMappingTable On CAD object And Part.....END......");
            System.out.println("--------------------------------------------------------------------");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }

    }

    /**
     * Trigger for Part 3D Design Update Use Case ( on CAD Part )
     * @param data
     * @return
     */
    public void modifyCustomMappingTableOnTypeCADPart(Context context, String args[]) throws Exception {
        try {
            System.out.println("modifyCustomMappingTable On CAD object And Part.....START......");
            System.out.println("--------------------------------------------------------------------");
            String strType = args[1];
            String strName = args[2];
            String strRevision = args[3];
            String strObjectId = args[0];
            String strATTRNAMEs = args[4];
            String strLastModified = DomainConstants.EMPTY_STRING;
            HashMap mpRecordToUpdate = new HashMap(6);
            System.out.println("strATTRNAMEs ==-" + strATTRNAMEs);
            System.out.println("strType ==-" + strType);
            System.out.println("strName ==-" + strName);
            System.out.println("strRevision ==-" + strRevision);
            System.out.println("strObjectId ==-" + strObjectId);
            DomainObject domObj = new DomainObject();
            String strPartId = DomainConstants.EMPTY_STRING;
            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                domObj = DomainObject.newInstance(context, strObjectId);

                System.out.println("--------------CAD SIDE----------------------");

                mpRecordToUpdate = getPartFromCATPart(context, strObjectId);
                if (null != mpRecordToUpdate && !mpRecordToUpdate.equals("")) {
                    strLastModified = (String) mpRecordToUpdate.get(DomainConstants.SELECT_MODIFIED);

                    Map mpUpdateValues = new HashMap(2);
                    mpUpdateValues.put("ISUPTODATE", "FALSE");
                    mpUpdateValues.put("SOURCELASTMODIFDATE", strLastModified);

                    strPartId = (String) mpRecordToUpdate.get("Id");
                    if (UIUtil.isNotNullAndNotEmpty(strPartId)) {
                        DBOperation DBOper = new DBOperation();
                        System.out.println("DB Operation object created....");
                        DBOper.updateTablesOnReferenceModification(mpRecordToUpdate, mpUpdateValues);
                        System.out.println("DB Operation updation done ..");
                    }
                    System.out.println("mpUpdateValues.." + mpUpdateValues);
                }
                System.out.println("CAD To EBOM DATA ==-" + mpRecordToUpdate);
            }
            System.out.println("mpRecordToUpdate.." + mpRecordToUpdate);
            System.out.println("strPartId.." + strPartId);
            System.out.println("--------------------------------------------------------------------");
            System.out.println("modifyCustomMappingTable On CAD object And Part.....END......");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }

    }

    /**
     * Trigger for Part Replaced with new Use Case (On Rel EBOM )
     * @param data
     * @return
     */
    public void updateCustomMappingTableOnReplaced(Context context, String[] args) throws Exception {

        try {
            System.out.println("modifyCustomMappingTable On Part replaced with new.....START......");

            String strObjectId = args[0];
            String strChildType = args[1];
            String strChildName = args[2];
            String strChildRevision = args[3];
            String strInstanceId = args[4];
            String strInstanceType = args[5];
            String strParentType = args[6];
            String strParentName = args[7];
            String strParentRevision = args[8];

            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {

                String strLastModified = getLastModifiedForObject(context, strObjectId);

                HashMap<String, String> mpRecordToAdd = new HashMap<String, String>(12);
                mpRecordToAdd.put("parentType", strParentType);
                mpRecordToAdd.put("parentName", strParentName);
                mpRecordToAdd.put("parentRevision", strParentRevision);
                mpRecordToAdd.put("Type", strChildType);
                mpRecordToAdd.put("Name", strChildName);
                mpRecordToAdd.put("Revision", strChildRevision);
                mpRecordToAdd.put("modified", strLastModified);
                mpRecordToAdd.put("InstanceType", strInstanceType);
                mpRecordToAdd.put("InstanceName", strInstanceId);
                mpRecordToAdd.put("InstanceRevision", DomainConstants.EMPTY_STRING);
                mpRecordToAdd.put(DBServices.ISEFFECTIVITYMODIFIED, "TRUE");
                System.out.println("mpRecordToADD" + mpRecordToAdd);

                DBOperation DBOper = new DBOperation();
                DBOper.createReferenceOnReplaced(mpRecordToAdd);
            }
            System.out.println("modifyCustomMappingTable On Part replaced with new.....END");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }
    }

    /**
     * method to prepare selectable to part retrival
     * @param data
     * @return
     */
    public String getSelectableForPart(String strKey) throws Exception {
        StringBuilder sbPartSel = new StringBuilder(5);
        try {
            sbPartSel.append("to[");
            sbPartSel.append(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
            sbPartSel.append("].from.").append(strKey);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }
        return sbPartSel.toString();
    }

    /**
     * method to get Part from CATPart
     * @param data
     * @return
     */

    public HashMap getPartFromCATPart(Context context, String strObjectId) throws Exception {

        HashMap<String, String> mpRecordToUpdate = new HashMap<String, String>(6);
        String strLastModified = DomainConstants.EMPTY_STRING;
        try {

            String strPartType = getSelectableForPart(DomainConstants.SELECT_TYPE);
            String strPartName = getSelectableForPart(DomainConstants.SELECT_NAME);
            String strPartRev = getSelectableForPart(DomainConstants.SELECT_REVISION);
            String strPartId = getSelectableForPart(DomainConstants.SELECT_ID);

            StringList slBusSelects = new StringList(5);
            slBusSelects.add(strPartType);
            slBusSelects.add(strPartName);
            slBusSelects.add(strPartRev);
            slBusSelects.add(strPartId);
            slBusSelects.add(DomainConstants.SELECT_MODIFIED);

            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domObj = DomainObject.newInstance(context, strObjectId);
                Map mpPartDetails = domObj.getInfo(context, slBusSelects);

                if (null != mpPartDetails && !mpPartDetails.isEmpty()) {
                    strLastModified = (String) mpPartDetails.get(DomainConstants.SELECT_MODIFIED);
                    mpRecordToUpdate.put("Type", (String) mpPartDetails.get(strPartType));
                    mpRecordToUpdate.put("Id", (String) mpPartDetails.get(strPartId));
                    mpRecordToUpdate.put("Name", (String) mpPartDetails.get(strPartName));
                    mpRecordToUpdate.put("Revision", (String) mpPartDetails.get(strPartRev));
                    mpRecordToUpdate.put("modified", strLastModified);
                }
            }
        } catch (FrameworkException e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }
        return mpRecordToUpdate;
    }

    /**
     * method to get EBOM Id
     * @param data
     * @return
     */
    public String getEBOMId(Context context, String strToObjectId, String strFromObjectId) throws FrameworkException {
        String strEBOMId = new String();
        try {
            strEBOMId = MqlUtil.mqlCommand(context, "query connection relationship EBOM where 'to.id==" + strFromObjectId + " && from.id==" + strToObjectId + "' select id dump");
            System.out.println("strEBOMId----" + strEBOMId);
            StringList objectIdList = FrameworkUtil.split(strEBOMId, "\n");
            System.out.println("objectIdList" + objectIdList);

            if (null != objectIdList && !objectIdList.isEmpty()) {
                for (int i = 0; i < objectIdList.size(); i++) {
                    strEBOMId = (String) objectIdList.get(i);
                    StringList objList = FrameworkUtil.split(strEBOMId, ",");
                    System.out.println("objList----" + objList);
                    strEBOMId = (String) objList.get(1);
                }
            }

            System.out.println("strEBOMId===" + strEBOMId);
        } catch (FrameworkException e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }
        return strEBOMId;

    }
}
