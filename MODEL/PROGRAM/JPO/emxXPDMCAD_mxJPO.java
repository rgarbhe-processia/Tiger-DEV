import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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

public class emxXPDMCAD_mxJPO {

    /**
     * Trigger method for Part Added Use Case (On Rel CAD SubComponent : Create: Action)
     * @param data
     * @return
     */
    public void p2vPartAdd(Context context, String[] args) throws Exception {
        try {
            System.out.println("[START] P2V Part add trigger...");
            String strInstanceId = args[0];
            String strInstanceType = args[1];
            String strParentType = args[3];
            String strParentName = args[4];
            String strParentRevision = args[5];
            String strChildType = args[6];
            String strChildName = args[7];
            String strChildRevision = args[8];
            if (!isValidRevision(strChildRevision, "P2V Part add trigger") || !isValidRevision(strParentRevision, "P2V Part add trigger") || !isValidObjectId(strInstanceId, "P2V Part add trigger")) {
                return;
            }
            String relLastModified = getLastModifiedForConnection(context, strInstanceId);
            Map<String, String> recordsToAdd = new HashMap<String, String>(12);
            recordsToAdd.put("parentType", strParentType);
            recordsToAdd.put("parentName", strParentName);
            recordsToAdd.put("parentRevision", strParentRevision);
            recordsToAdd.put("Type", strChildType);
            recordsToAdd.put("Name", strChildName);
            recordsToAdd.put("Revision", strChildRevision);
            recordsToAdd.put("modified", relLastModified);
            recordsToAdd.put("InstanceType", strInstanceType);
            recordsToAdd.put("InstanceName", strInstanceId);
            recordsToAdd.put("InstanceRevision", DomainConstants.EMPTY_STRING);

            Map<String, String> valuesToUpdate = buildMap("FALSE", relLastModified);
            valuesToUpdate.put(DBServices.ISEFFECTIVITYMODIFIED, "TRUE");

            DBOperation dbOperation = new DBOperation();
            dbOperation.createInstance(recordsToAdd, valuesToUpdate);
            System.out.println("[END] P2V Part add trigger...");
        } catch (Exception e) {
            System.out.println("[ERROR] P2V Part add trigger: " + e);
            e.printStackTrace();
        }
    }

    private Map<String, String> buildMap(String isUptoDate, String relLastModified) {
        Map<String, String> valuesToUpdate = new HashMap<String, String>(4);
        valuesToUpdate.put(DBServices.ISUPTODATE, isUptoDate);
        valuesToUpdate.put(DBServices.SOURCELASTMODIFDATE, relLastModified);
        return valuesToUpdate;
    }

    /**
     * Trigger method for Part Promote Use Case (On Policy EC Part)
     * @param data
     * @return
     */
    public void p2vPartPromote(Context context, String[] args) throws Exception {

        try {
            System.out.println("[START] P2V Part promote trigger...");
            String strType = args[0];
            String strName = args[1];
            String strRevision = args[2];
            String strObjectId = args[3];

            System.out.println(Arrays.toString(args));

            if (!UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                System.out.println("[WARN] P2V Part promote trigger: OBJECTID null or empty. Trigger method skip the further execution.");
                return;
            }

            String strLastModified = getLastModifiedForObject(context, strObjectId);

            Map<String, String> recordToUpdate = new HashMap<String, String>(5);
            recordToUpdate.put("Type", strType);
            recordToUpdate.put("Name", strName);
            recordToUpdate.put("Revision", strRevision);
            recordToUpdate.put("modified", strLastModified);

            Map<String, String> valuesToUpdate = new HashMap<String, String>(3);
            valuesToUpdate.put(DBServices.ISUPTODATE, "FALSE");
            valuesToUpdate.put(DBServices.SOURCELASTMODIFDATE, strLastModified);

            System.out.println("\t-> RecordToAdd: " + recordToUpdate);
            System.out.println("\t-> UpdateValues: " + valuesToUpdate);

            DBOperation dbOperation = new DBOperation();
            dbOperation.updateTablesOnReferenceModification(recordToUpdate, valuesToUpdate);

            System.out.println("[END] P2V Part promote trigger...");
        } catch (Exception e) {
            System.out.println("[ERROR] P2V Part promote trigger: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Trigger method for Part Remove Use Case (On Rel EBOM : Delete : Action)
     * @param data
     * @return
     */
    public void p2vPartRemove(Context context, String args[]) throws Exception {
        System.out.println("[START] P2V Part remove trigger...");
        try {
            String strInstanceId = args[0];
            String strParentType = args[1];
            String strParentName = args[2];
            String strParentRevision = args[3];
            String strChildType = args[4];
            String strChildName = args[5];
            String strChildRevision = args[6];
            if (!isValidRevision(strChildRevision, "P2V Part remove trigger") || !isValidRevision(strParentRevision, "P2V Part remove trigger")
                    || !isValidObjectId(strInstanceId, "P2V Part remove trigger")) {
                return;
            }
            String strLastModified = getLastModifiedForConnection(context, strInstanceId);
            Map<String, String> recordToRemove = new HashMap<String, String>(11);
            recordToRemove.put("parentType", strParentType);
            recordToRemove.put("parentName", strParentName);
            recordToRemove.put("parentRevision", strParentRevision);
            recordToRemove.put("Type", strChildType);
            recordToRemove.put("Name", strChildName);
            recordToRemove.put("Revision", strChildRevision);
            recordToRemove.put("InstanceType", DomainConstants.EMPTY_STRING);
            recordToRemove.put("InstanceName", strInstanceId);
            recordToRemove.put("InstanceRevision", DomainConstants.EMPTY_STRING);
            recordToRemove.put("modified", strLastModified);
            Map<String, String> valuesToUpdate = buildMap("FALSE", strLastModified);
            valuesToUpdate.put(DBServices.ISDELETED, "TRUE");
            DBOperation dbOperation = new DBOperation();
            dbOperation.removeInstanceOnPartRemove(recordToRemove, valuesToUpdate);
            System.out.println("[END] P2V Part remove trigger...");
        } catch (Exception e) {
            System.out.println("[ERROR] P2V Part remove trigger: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Trigger Method for EBOM Modify Attribute(Instance Renaming) Use Case (On Rel EBOM )
     * @param data
     * @return
     */

    public void p2vModifyRelationAttributes(Context context, String args[]) throws Exception {
        System.out.println("[START] P2V Relationship modify attributes trigger...");
        try {
            String strInstanceId = args[0];
            String strInstanceType = args[1];
            String strParentType = args[3];
            String strParentName = args[4];
            String strParentRevision = args[5];
            String strChildType = args[6];
            String strChildName = args[7];
            String strChildRevision = args[8];
            String strAttrName = args[9];

            if (!isUpdateRequired(strInstanceId, strChildRevision, strAttrName, "P2V Relationship modify attributes trigger")
                    || !isUpdateRequired(strInstanceId, strParentRevision, strAttrName, "P2V Relationship modify attributes trigger"))
                return;

            String strLastModified = getLastModifiedForConnection(context, strInstanceId);
            Map<String, String> valuesToUpdate = buildMap("FALSE", strLastModified);
            if (isEffectivityAttribute(strAttrName))
                valuesToUpdate.put(DBServices.ISEFFECTIVITYMODIFIED, "TRUE");

            Map<String, String> recordsToAdd = new HashMap<String, String>(11);
            recordsToAdd.put("parentType", strParentType);
            recordsToAdd.put("parentName", strParentName);
            recordsToAdd.put("parentRevision", strParentRevision);
            recordsToAdd.put("Type", strChildType);
            recordsToAdd.put("Name", strChildName);
            recordsToAdd.put("Revision", strChildRevision);
            recordsToAdd.put("modified", strLastModified);
            recordsToAdd.put("InstanceType", strInstanceType);
            recordsToAdd.put("InstanceName", strInstanceId);
            recordsToAdd.put("InstanceRevision", DomainConstants.EMPTY_STRING);

            DBOperation DBOper = new DBOperation();
            DBOper.updateInstance(recordsToAdd, valuesToUpdate);
            System.out.println("[END] P2V Relationship modify attributes trigger...");
        } catch (Exception e) {
            System.out.println("[ERROR] P2V Relationship modify attributes trigger: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Trigger for Part Modify Attributes (On Part )
     * @param context
     * @param args
     * @throws Exception
     */
    public void p2vModifyPartAttributes(Context context, String args[]) throws Exception {
        System.out.println("[START] P2V Part modify attributes trigger...");
        try {
            String strObjectId = args[0];
            String strType = args[1];
            String strName = args[2];
            String strRevision = args[3];
            String strAttName = args[4];
            if (!isUpdateRequired(strObjectId, strRevision, strAttName, "P2V Part modify attributes trigger")) {
                System.out.println("\t[INFO] Trigger method skip the further execution.");
                return;
            }

            String strLastModified = getLastModifiedForObject(context, strObjectId);

            Map<String, String> recordsToUpdate = new HashMap<String, String>(6);
            recordsToUpdate.put("Type", strType);
            recordsToUpdate.put("Id", strObjectId);
            recordsToUpdate.put("Name", strName);
            recordsToUpdate.put("Revision", strRevision);
            recordsToUpdate.put("modified", strLastModified);

            Map<String, String> valuesToUpdate = new HashMap<String, String>(3);
            valuesToUpdate.put("ISUPTODATE", "FALSE");
            valuesToUpdate.put("SOURCELASTMODIFDATE", strLastModified);

            System.out.println("\t-> RecordToAdd: " + recordsToUpdate);
            System.out.println("\t-> UpdateValues: " + valuesToUpdate);

            if (null != recordsToUpdate && !recordsToUpdate.equals("")) {
                DBOperation dbOperation = new DBOperation();
                dbOperation.updateTablesOnReferenceModification(recordsToUpdate, valuesToUpdate);
            }
            System.out.println("[END] P2V Part modify attributes trigger...");
        } catch (Exception e) {
            System.out.println("[ERROR] P2V modify attribute trigger on Part: " + e);
            e.printStackTrace();
        }

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

                    XPDMXMLGenerator gen = new XPDMXMLGenerator(context, true, "CGR".equalsIgnoreCase(isCGR), isDeleted, "TRUE".equalsIgnoreCase(isEffectivityModified));
                    gen.generate(strType, strName, strRevision);
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] P2V update cron: " + e);
            e.printStackTrace();
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

    private boolean validateDate(String relLastModified, String tblLastModified) {

        // Instance not modified since last extraction
        if (relLastModified != null && relLastModified.equals(tblLastModified)) {
            return false;
        } else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
                Date rDate = sdf.parse(relLastModified);
                Date tDate = sdf.parse(tblLastModified);
                return rDate.after(tDate);
            } catch (Exception e) {
                System.out.println("\n\t-> Error in parsing date: " + e.getMessage());
                return false;
            }

        }
    }

    private Map<String, String> getDetail(String strInstanceId) throws SQLException {
        DBServices services = new DBServices();
        String columnSelect[] = new String[] { DBServices.INSTANCENUMBER, DBServices.SOURCELASTMODIFDATE };
        List<Map<String, String>> list = services.fetchDetails("link", columnSelect, DBServices.INSTANCENUMBER + "='" + strInstanceId + "'");
        return list.isEmpty() ? null : list.get(0);
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
        } catch (Exception e) {
            System.out.println("[ERROR] getLastModifiedForConnection: " + e);
            e.printStackTrace();
        }
        return strLastModified;
    }

    public Map<String, String> getConnectionInfo(Context context, String strInstanceId) {
        try {
            StringList selectables = new StringList();
            selectables.addElement(DomainConstants.SELECT_MODIFIED);
            selectables.addElement(DomainConstants.SELECT_ORIGINATED);
            DomainRelationship dr = DomainRelationship.newInstance(context, strInstanceId);
            return dr.getAttributeDetails(context);
        } catch (Exception e) {
            System.out.println("[ERROR] getLastModifiedForConnection: " + e);
            e.printStackTrace();
        }
        return null;
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
        } catch (Exception e) {
            System.out.println("[ERROR] getLastModifiedForObject: " + e);
            e.printStackTrace();
        }
        return strLastModified;
    }

    private boolean isEffectivityAttribute(String attrName) {
        String[] effectivityAttrs = { "Effectivity Compiled Form", "Effectivity Expression", "Effectivity Expression Binary", "Effectivity Ordered Criteria", "Effectivity Ordered Criteria Dictionary",
                "Effectivity Ordered Impacting Criteria", "Effectivity Proposed Expression", "Effectivity Types", "Effectivity Variable Indexes" };

        for (String eAtt : effectivityAttrs) {
            if (eAtt != null && eAtt.equals(attrName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUpdateRequired(String strObjectId, String strRevision, String strAttName, String calledFrom) {
        return isValidRevision(strRevision, calledFrom) && isValidObjectId(strObjectId, calledFrom) && isValidAttributeName(strAttName, calledFrom);
    }

    private boolean isValidRevision(String strRevision, String calledFrom) {
        if (strRevision.contains(".")) {
            System.out.println("[WARN] " + calledFrom + ": Trigger called for iteration of object.");
            return false;
        }
        return true;
    }

    private boolean isValidAttributeName(String strAttName, String calledFrom) {
        if ("IEF-FileMessageDigest".equals(strAttName)) {
            System.out.println("[WARN] " + calledFrom + ": Modified attribute is " + strAttName + ".");
            return false;
        }
        return true;
    }

    private boolean isValidObjectId(String strObjectId, String calledFrom) {
        if (!UIUtil.isNotNullAndNotEmpty(strObjectId)) {
            System.out.println("[WARN] " + calledFrom + ": OBJECTID null or empty.");
            return false;
        }
        return true;
    }

}
