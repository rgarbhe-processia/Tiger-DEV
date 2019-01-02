
/*
 ** ITIMxMCAD_ENOVIANewCustomJPO
 **
 ** JPO for retrieving ECPart Number from Part Pool and provide ECPart information as needed by the ENOVIA New functionality for various use cases.
 **
 ** In the Sections marked as TODO for the functions, Custom Code as per Customer requirements need to be implemented.
 **
 */
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.List;
import java.lang.StringBuffer;
import java.util.Date;
import matrix.db.Attribute;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.Expansion;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;
import matrix.db.AttributeList;
import matrix.db.AttributeItr;
import com.matrixone.apps.common.Part;
import matrix.db.*;
import matrix.db.MatrixLogWriter;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.ContextUtil;
import matrix.util.StringList;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;

public class ITIMxMCAD_ENOVIANewCustomJPO_mxJPO {
    private final String confObjTNR = "MCAD-EBOMSyncConfig|MCAD-EBOMSyncConfig-TEAM|-";

    private Context _context = null;

    protected IEFEBOMConfigObject ebomConfigObj = null;

    private String confAttrNewPartPolicy = "";

    private String confAttrNewPartRevision = "";

    // Define this variable to True for working in Customer Environment.
    private boolean bCustomerEnvironment = false;

    // Define this variable to false by default as the EC Part attribute is not allowed
    // if the EC Part number is already created in ENOVIA (i.e., not created through ENOVIA->New feature)
    private boolean bAllowECPartAttributeUpdate = false;

    // Attribute for reading the User initials.
    private String ATTR_CADSIGNATURE = "CAD Signature";

    // Policy name definition for reading Initial Revision and State names.
    private String POLICY_PROENGINEER_DESIGN = "Design TEAM Definition";

    private static final boolean DEBUG = true;

    private static final boolean WRITE_DEBUG_TO_RMI_LOGS = true;

    private MatrixLogWriter matrixLogger = null;

    private String DESIGN_RESPONSIBILITY_RELATIONSHIP = PropertyUtil.getSchemaProperty("relationship_DesignResponsibility");

    private String TYPE_RDO = PropertyUtil.getSchemaProperty("type_Department");

    public ITIMxMCAD_ENOVIANewCustomJPO_mxJPO(Context context, String[] args) throws Exception {
        _context = context;
    }

    public int main(String[] args) throws Exception {
        return 0;
    }

    private void init(Context _context) throws Exception {
        StringTokenizer token = new StringTokenizer(confObjTNR, "|");
        if (token.countTokens() < 3) {
            throw new Exception("MCAD-EBOMSyncConfig object cannot be obtained.");
        }

        String confObjType = (String) token.nextElement();
        String confObjName = (String) token.nextElement();
        String confObjRev = (String) token.nextElement();
        ebomConfigObj = new IEFEBOMConfigObject(_context, confObjType, confObjName, confObjRev);

        confAttrNewPartPolicy = ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_NEW_PART_POLICY);
        confAttrNewPartRevision = ebomConfigObj.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_NEW_PART_REVISION);
        // strOwner,strPolicy,strType,strRevision,strVault,attributesMap,strDesc
    }

    /**
     * Method to execute the MQL command.
     * @param context
     * @param mqlCmd
     * @return
     */
    private String executeMQLCommand(Context context, String mqlCmd) throws Exception {
        String mqlResult = "";
        logMessage("executeCommand", "Entered...");
        try {
            if (context != null) {
                MQLCommand mqlc = new MQLCommand();

                logMessage("executeCommand", "mqlCmd: " + mqlCmd);
                logMessage("executeCommand", "context user: " + context.getUser());
                boolean bRet = mqlc.executeCommand(context, mqlCmd);
                if (bRet) {
                    mqlResult = mqlc.getResult();
                    logMessage("executeCommand", "mqlResult: " + mqlResult);
                    if (mqlResult != null && mqlResult.length() > 0) {
                        mqlResult = "true|" + mqlResult;
                    } else if (mqlResult.length() == 0) {
                        mqlResult = "false|" + mqlResult;
                    }
                } else {
                    mqlResult = mqlc.getError();
                    mqlResult = "false|" + mqlResult;
                }

                logMessage("executeCommand", "mqlResult: " + mqlResult.length());
                if (mqlResult.endsWith("\n")) {
                    mqlResult = mqlResult.substring(0, (mqlResult.lastIndexOf("\n")));
                }
            } else {
                mqlResult = "false|" + "Invalid Context is passed for executing MQL command...";
            }
        } catch (Exception me) {
            mqlResult = "false|" + me.getMessage();
            throw new Exception("[executeMQLCommand]... Error occured: " + mqlResult);
        }

        logMessage("executeCommand", "returning mqlResult: " + mqlResult);
        return mqlResult;
    }

    /**
     * find if the ECPart exists in PLM input: ECPart Name output: the part info if it exists.
     **/
    public String findIfECPartExists(Context context, String[] inputArgs) throws Exception {
        String result = "";
        try {
            logMessage("findIfECPartExists", "Entered...");

            if (inputArgs == null || inputArgs.length < 1) {
                throw (new IllegalArgumentException());
            }

            boolean ecpartExists = false;

            String ecpartNumber = inputArgs[0];
            logMessage("findIfECPartExists", "ecPartNumber: " + ecpartNumber);

            String mqlCmd = "temp query bus Part \"" + ecpartNumber.trim() + "\" * select id dump |";
            logMessage("findIfECPartExists", "mqlCmd: " + mqlCmd);
            result = executeMQLCommand(context, mqlCmd);
            if (result.startsWith("true")) {
                ecpartExists = true;
            } else {
                if (result.equals("false|")) {
                    result = result + "EC Part does not exist.";
                }
                ecpartExists = false;
                return result;
            }

            logMessage("findIfECPartExists", "result: " + result);
        } catch (Exception e) {
            String msg = "Error occured while querying for EC Part:" + e.getMessage();
            logMessage("findIfECPartExists", msg);
            result = "false|" + msg;
        }
        return result;
    }

    /**
     * find if the ProE CAD Spec Object exists in Designer Central. input: ECPart Name output: the ProE CAD Spec info if it exists.
     **/
    public String findIfProESpecObjectExists(Context context, String[] inputArgs) throws Exception {
        String result = "";
        try {
            logMessage("findIfProESpecObjectExists", "Entered...");

            if (inputArgs == null || inputArgs.length < 1) {
                throw (new IllegalArgumentException());
            }

            boolean proeCadSpecObjExists = false;

            String ecpartNumber = inputArgs[0];
            logMessage("findIfProESpecObjectExists", "ecPartNumber: " + ecpartNumber);
            String templateCadType = "";
            if (inputArgs.length == 2)
                templateCadType = inputArgs[1];

            String proEType = "";
            if (templateCadType.equals("component")) {
                proEType = "ProE Part,ProE Part Instance";
            } else if (templateCadType.equals("assembly")) {
                proEType = "ProE Assembly,ProE Assembly Instance";
            }

            String mqlCmd = "temp query bus 'ProE*' \"" + ecpartNumber.trim() + "\" * select id dump |";
            logMessage("findIfProESpecObjectExists", "mqlCmd: " + mqlCmd);
            result = executeMQLCommand(context, mqlCmd);
            if (result.startsWith("true")) {
                proeCadSpecObjExists = true;
            } else {
                if (result.equals("false|")) {
                    result = result + "ProE CAD Specification Object does not exist.";
                }
                proeCadSpecObjExists = false;
                return result;
            }

            logMessage("findIfProESpecObjectExists", "result: " + result);
        } catch (Exception e) {
            String msg = "Error occured while querying for EC Part:" + e.getMessage();
            logMessage("findIfProESpecObjectExists", msg);
            result = "false|" + msg;
        }
        return result;
    }

    /**
     * check ifthe ECPart exists in PLM input: ECPart Name output: the part info if it exists.
     **/
    public String checkIfECPartExists(Context context, String[] inputArgs) throws Exception {
        String result = "";
        try {
            logMessage("checkIfECPartExists", "Entered...");

            if (inputArgs == null || inputArgs.length < 1) {
                throw (new IllegalArgumentException());
            }

            String partInfo = findIfECPartExists(context, inputArgs);

            if (partInfo.startsWith("true")) {
                // String [] partInfoArray = partInfo.split("|");
                int lastIdx = partInfo.lastIndexOf("|");
                String partId = partInfo.substring(lastIdx + 1);
                logMessage("checkIfECPartExists", "partId: " + partId);

                // Check if ProE Model Object exists in ENOVIA.
                // String [] args = new String[1];
                // args[0] = partId;
                String proeCadObjectInfo = findIfProESpecObjectExists(context, inputArgs);
                // String [] proeCadObjectInfoArray = proeCadObjectInfo.split("|");
                if (proeCadObjectInfo.startsWith("true")) {
                    lastIdx = proeCadObjectInfo.lastIndexOf("|");
                    String proeCadObjectId = proeCadObjectInfo.substring(lastIdx + 1);
                    logMessage("checkIfECPartExists", "proeCadObjectId: " + proeCadObjectId);

                    result = "true~usermessage|Part Number exists in ENOVIA, it is already used by the Creo integration and cannot be used for creating New Design file from template.";
                } else {
                    result = partInfo;

                    // Read the User initials and return.
                    String sCADSignature = getUserIntials(context);
                    if (sCADSignature == null)
                        sCADSignature = "";
                    if (!"".equals(sCADSignature)) {
                        result = result + "|" + sCADSignature;
                    }
                }
            } else {
                logMessage("checkIfECPartExists", "Part Number does not exist.");
                result = "false|EC Part does not exist for Part Number";
                // Read the User initials and return.
                String sCADSignature = getUserIntials(context);
                if (sCADSignature == null)
                    sCADSignature = "";
                if (!"".equals(sCADSignature)) {
                    result = result + "|" + sCADSignature;
                }
            }

            logMessage("checkIfECPartExists", "result: " + result);
        } catch (Exception e) {
            String msg = "Error occured while querying for EC Part:" + e.getMessage();
            logMessage("checkIfECPartExists", msg);
            result = "false|" + msg;
        }
        return result;
    }

    /**
     * get the Attributes for the ECPart Type, Name and Revision input: ECPart Type, Name and Revision output: the ECPart atribute "Name|Value" pairs delimited by '@@' if it exists.
     **/
    public String getPartAttributes(Context context, String[] inputArgs) throws Exception {
        StringBuffer attributeNameValues = new StringBuffer();
        String sAllowECPartUpdates = "false";
        String sPartAttributeNameValuePairs = "";
        try {
            logMessage("getPartAttributes", "Entered... ");

            if (inputArgs == null || inputArgs.length < 3) {
                throw (new IllegalArgumentException());
            }

            String ecpartType = inputArgs[0];
            String ecpartNumber = inputArgs[1];
            String ecpartRevision = inputArgs[2];
            logMessage("getPartAttributes", "ecPartType: " + ecpartType);
            logMessage("getPartAttributes", "ecPartNumber: " + ecpartNumber);
            logMessage("getPartAttributes", "ecpartRevision: " + ecpartRevision);

            String[] attributes = new String[inputArgs.length - 3];

            BusinessObject partBus = new BusinessObject(ecpartType, ecpartNumber, ecpartRevision, "");
            partBus.open(context);

            for (int i = 3; i < inputArgs.length; i++) {
                attributes[i - 3] = inputArgs[i];
                String attributeName = attributes[i - 3];
                logMessage("getPartAttributes", "attributeName: " + attributeName);

                //
                // TODO: Implement custom code as per the attributes required to be returned as per Customer requirements.
                // retrieve attribute value and return in the appropriate format from here.
                // attribute name, value pairs to be formed: attributeNameValues = attr1|attrvalue1@@attr2|attrvalue2@@attr3|attrvalu3
                //
                // attributeNameValues will be returned along with required prefix as needed depending on if values are allowed
                // to be edited in ENOVIA New Dialog or not.
                //
                if (attributeName.equals("$$Description$$") || attributeName.equals("Description")) {
                    String attributeValue = partBus.getDescription(context);
                    attributeNameValues.append(attributeName + "|" + attributeValue);
                    if (i <= inputArgs.length - 1) {
                        attributeNameValues.append("@@");
                    }
                } else if (attributeName.equals("$$Owner$$") || attributeName.equals("Owner")) {
                    String attributeValue = (partBus.getOwner(context)).toString();
                    attributeNameValues.append(attributeName + "|" + attributeValue);
                    if (i <= inputArgs.length - 1) {
                        attributeNameValues.append("@@");
                    }
                } else if (attributeName.equalsIgnoreCase("Policy")) {
                    String attributeValue = (partBus.getPolicy(context)).getName();
                    attributeNameValues.append(attributeName + "|" + attributeValue);
                    if (i <= inputArgs.length - 1) {
                        attributeNameValues.append("@@");
                    }
                } else if (attributeName.equalsIgnoreCase("MATERIAL")) {
                    String attrName = "Material";
                    // Attribute attrib = partBus.getAttributeValues(context, attrName);
                    StringList strAttrList = new StringList();
                    strAttrList.add(attrName);
                    boolean bAttributeHidden = true;
                    AttributeList allAttributeList = partBus.getAttributes(context, strAttrList, bAttributeHidden);
                    Attribute attrib = getAttributeObjectFromAttributeList(allAttributeList, attrName);
                    if (attrib != null) {
                        String attributeValue = attrib.getValue();
                        if (attributeValue == null)
                            attributeValue = "";
                        attributeNameValues.append(attributeName + "|" + attributeValue);
                        if (i <= inputArgs.length - 1) {
                            attributeNameValues.append("@@");
                        }
                    }
                } else if (attributeName.equalsIgnoreCase("Collect Material data for RoHS")) {
                    // The attribute 'Collect Material data for ROHS' is actually the attribute 'To ESMART'.
                    // So, translate that to 'To ESMART', read the value and send it back in response as
                    // 'Collect Material data for ROHS'.
                    String attrName = "To ESMART";
                    StringList strAttrList = new StringList();
                    strAttrList.add(attrName);
                    boolean bAttributeHidden = true;
                    AttributeList allAttributeList = partBus.getAttributes(context, strAttrList, bAttributeHidden);
                    Attribute attrib = getAttributeObjectFromAttributeList(allAttributeList, attrName);
                    String attributeValue = attrib.getValue();
                    if (attributeValue == null)
                        attributeValue = "";
                    attributeNameValues.append(attributeName + "|" + attributeValue);
                    if (i <= inputArgs.length - 1) {
                        attributeNameValues.append("@@");
                    }
                } else if (attributeName.equalsIgnoreCase("RDO") || attributeName.equalsIgnoreCase("Design Responsible Organization")) {
                    // attributeName = "Design Responsible Organization";
                    // Get the RDO Name by expanding the EC Part Object.
                    String partId = partBus.getObjectId(context);
                    MapList mpList = new MapList();
                    String strWhere = "";
                    mpList = getRelObjs(context, partId, DESIGN_RESPONSIBILITY_RELATIONSHIP, strWhere);
                    if (mpList.size() > 0) {
                        Hashtable resultTable = (Hashtable) mpList.get(0);
                        String rdoName = (String) resultTable.get("name");
                        attributeNameValues.append(attributeName + "|" + rdoName);
                        if (i <= inputArgs.length - 1) {
                            attributeNameValues.append("@@");
                        }
                    }
                } else {
                    // Attribute attrib = partBus.getAttributeValues(context, attributeName);
                    StringList strAttrList = new StringList();
                    strAttrList.add(attributeName);
                    boolean bAttributeHidden = true;
                    AttributeList allAttributeList = partBus.getAttributes(context, strAttrList, bAttributeHidden);
                    Attribute attrib = getAttributeObjectFromAttributeList(allAttributeList, attributeName);
                    if (attrib != null) {
                        String attributeValue = attrib.getValue();
                        if (attributeValue == null)
                            attributeValue = "";
                        attributeNameValues.append(attributeName + "|" + attributeValue);
                        if (i <= inputArgs.length - 1) {
                            attributeNameValues.append("@@");
                        }
                    }
                }
            }

            partBus.close(context);

            logMessage("getPartAttributes", "returning attributeNameValues: " + attributeNameValues);

            // This is to allow EC Part Attributes from ENOVIA->New when the EC Part is already exists in ENOVIA and
            // not created usign ENOVIA->New Auto Number feature.
            if (bAllowECPartAttributeUpdate) {
                sAllowECPartUpdates = "allow-part-updates|";
            } else {
                sAllowECPartUpdates = "disallow-part-updates|";
            }
            sPartAttributeNameValuePairs = sAllowECPartUpdates + attributeNameValues.toString();
        } catch (Exception e) {
            String msg = "Exception occurred while retrieving attribute values: " + e.getMessage();
            logMessage("getPartAttributes", msg);

            return "false|" + msg;
        }

        return "true~" + sPartAttributeNameValuePairs;
    }

    private Attribute getAttributeObjectFromAttributeList(AttributeList attribList, String attributeName) throws Exception {
        Attribute attr = null;

        AttributeItr attribListItr = new AttributeItr(attribList);
        while (attribListItr.next()) {
            attr = (Attribute) attribListItr.obj();
            String attrName = (String) attr.getName();
            if (attrName.equals(attributeName)) {
                break;
            }
        }

        return attr;
    }

    /*****************************************************************************
     * # getRelObjs # Arguments include: # context # ObjId - id of object for which the related objects to be obtained. # relName - name of the relationship. # strWhere - where condition for getting
     * related objects. # Returns: # Related Objects MapList.
     *****************************************************************************/
    private MapList getRelObjs(Context context, String ObjId, String relName, String strWhere) throws Exception {
        DomainObject domObjPart = new DomainObject(ObjId);
        MapList relObjs = new MapList();

        StringList strlSelects = new StringList();
        strlSelects.addElement("id");
        strlSelects.addElement("name");
        // strlSelects.addElement("attribute[Title]");

        relObjs = domObjPart.getRelatedObjects(context, relName, "*", strlSelects, null, true, false, (short) 0, strWhere, "", null, null, null);

        return relObjs;
    }

    /**
     * Update the Attributes for the EC Part input: ECPart Type and Name output: success/failure
     **/
    public String updateECPartAttributes(Context context, String[] inputArgs) throws Exception {
        logMessage("updateECPartAttributes", "Entered...");

        if (inputArgs == null || inputArgs.length < 2) {
            throw (new IllegalArgumentException());
        }

        try {
            String ecPartNumber = inputArgs[0];
            logMessage("updateECPartAttributes", "ecPartNumber: " + ecPartNumber);

            String[] partTypeNameArgs = new String[1];
            partTypeNameArgs[0] = ecPartNumber;

            String partInfo = findIfECPartExists(context, partTypeNameArgs);

            String[] partInfoArray = partInfo.split("|");
            int lastIdx = partInfo.lastIndexOf("|");
            String partId = partInfo.substring(lastIdx + 1);
            logMessage("updateECPartAttributes", "partId: " + partId);

            Hashtable partAttributes = new Hashtable();
            Hashtable systemAttributes = new Hashtable();
            for (int i = 1; i < inputArgs.length; i++) {
                String attributeNameValue = inputArgs[i];
                int idx = attributeNameValue.indexOf("|");
                String attributeName = attributeNameValue.substring(0, idx);
                String attributeValue = attributeNameValue.substring(idx + 1);
                logMessage("updateECPartAttributes", "attributeName: " + attributeName + " attributeValue: " + attributeValue);
                if (attributeName.startsWith("$$Description$$") || attributeName.equals("Description")) {
                    systemAttributes.put("$$Description$$", attributeValue);
                }
                if (attributeName.startsWith("$$Owner$$") || attributeName.equals("Owner")) {
                    systemAttributes.put("$$Owner$$", attributeValue);
                } else if (checkIfValueExistInRange(context, attributeName, attributeValue)) {
                    partAttributes.put(attributeName, attributeValue);
                }
            }

            BusinessObject busObj = new BusinessObject(partId);
            Part partBus = new Part(busObj);

            partBus.openObject(context);
            logMessage("updateECPartAttributes", "partAttributes: " + partAttributes.toString());
            partBus.setAttributeValues(context, partAttributes);
            setSystemAttributeValues(context, partBus, systemAttributes);

            partBus.closeObject(context, true);
            logMessage("updateECPartAttributes", "updated partAttributes seccessfully...");
        } catch (Exception e) {
            String msg = "Exception occurred while updating attributes...: " + e.getMessage();
            logMessage("updateECPartAttributes", msg);

            return "false|" + msg;
        }
        return "true|successfully updated attributes.";
    }

    private boolean checkIfValueExistInRange(Context context, String attributeName, String atrVal) throws Exception {
        boolean ret = true;
        matrix.util.StringList rangelist = null;

        // find if attribute contains range values.
        try {
            AttributeType attributeType = new AttributeType(attributeName);
            attributeType.open(context);
            rangelist = attributeType.getChoices();
            attributeType.close(context);
            // if range values exist, check if the value passed in present
            if (rangelist != null && rangelist.size() > 0 && !(rangelist.contains(atrVal))) {
                ret = false;
            }
        } catch (Exception e) {
            ret = false;
        }

        return ret;
    }

    private void setSystemAttributeValues(Context context, Part partObj, Hashtable sysAttr) throws Exception {
        if (sysAttr != null && sysAttr.containsKey("$$Description$$") || sysAttr.containsKey("Description")) {
            String descAttr = (String) sysAttr.get("$$Description$$");
            if (descAttr == null) {
                descAttr = (String) sysAttr.get("Description");
            }
            if (descAttr != null && descAttr.trim().length() > 0) {
                partObj.setDescription(context, descAttr);
                partObj.update(context);
            }
        }

        if (sysAttr != null && sysAttr.containsKey("$$Owner$$")) {
            String ownerAttr = (String) sysAttr.get("$$Owner$$");
            if (ownerAttr != null && ownerAttr.trim().length() > 0) {
                partObj.setOwner(context, ownerAttr);
                partObj.update(context);
            }
        }
    }

    /**
     * retrieve Default Part Number for RMB->AutoName functionality in SaveDlg. strOwner,strPolicy,strType,strRevision,strVault,attributesMap,strDesc
     **/
    public String retrieveDefaultTypePartNumberFromPool(Context context, String[] args) throws Exception {
        String result = "";
        try {
            String defaultPartType = args[0];
            String numAutoNumbersRqd = args[1];

            int nAutoNumbersNeeded = Integer.parseInt(numAutoNumbersRqd);

            for (int i = 0; i < nAutoNumbersNeeded; i++) {
                String tempResult = "";
                if (!bCustomerEnvironment) {
                    // String ecPartType = "Fabricated Item";
                    String ecPartType = "Part";
                    String strUnitOfMeasure = "Each";
                    String strPolicy = "EC Part";
                    String[] in_args = new String[1];
                    in_args[0] = ecPartType;
                    // in_args[1] = "Unit of Measure|" + strUnitOfMeasure;
                    // in_args[2] = "Policy|" + strPolicy;
                    tempResult = retrievePartNumberFromPoolInternal(context, in_args);
                } else {
                    //
                    // TODO: Customer specific implementation
                    //
                    // Customer can call own Auto Number Generation programs here.
                    // Refer to the sample OOTB code used in the function, retrievePartNumberFromPoolInternal(...)
                    // tempResult variable needs to be populated with the 'Auto Number' to be returned.
                    // The tempResult value needs to be formatted as: "true|AUTONUMBER".
                    //
                    // Custom code can be implemented in the function, retrievePartNumberFromPoolCustom
                    //
                    // tempResult = retrievePartNumberFromPoolCustom(context, in_args);
                    //
                    tempResult = "";
                }

                if (tempResult.startsWith("true|")) {
                    if (i > 0) {
                        result = result + ";";
                    }
                    result = result + tempResult.substring(5);
                }
            }

            result = "true|" + result;
        } catch (Exception e) {
            result = "false|Failed to retrieve Part Number.\n" + e.getMessage();
        }

        return result;
    }

    /**
     * retrieve Part Number and update Attributes strOwner,strPolicy,strType,strRevision,strVault,attributesMap,strDesc
     **/
    public String retrievePartNumberFromPool(Context context, String[] args) throws Exception {
        String result = "";
        try {
            if (!bCustomerEnvironment) {
                result = retrievePartNumberFromPoolInternal(context, args);
            } else {
                // result = retrievePartNumberFromPoolCustom(context, args);

                //
                // TODO: Customer specific implementation
                //
                // Customer can call own Auto Number Generation programs here.
                // Refer to the sample OOTB code used in the function, retrievePartNumberFromPoolInternal(...)
                // result variable needs to be populated with the 'Auto Number' to be returned.
                // The result value needs to be formatted as: "true|AUTONUMBER".
                //

                result = "false|Retrieve Auto Number custom code is not implemented. Please contact administrator.";
            }

            // Retrieve User initials from the Person Object and return.
            if (result.startsWith("true|")) {
                String sCADSignature = getUserIntials(context);
                if (sCADSignature == null)
                    sCADSignature = "";
                if (!"".equals(sCADSignature)) {
                    result = result + "|" + sCADSignature;
                }
            }
        } catch (Exception e) {
            result = "false|Failed to retrieve Part Number.\n" + e.getMessage();
        }

        return result;
    }

    /**
     * function: retrievePartNumberFromPoolInternal This is OOTB implementation to retrieve Auto Number for EC Parts.
     */
    public String retrievePartNumberFromPoolInternal(Context context, String[] args) throws Exception {
        String result = "";
        try {
            logMessage("retrievePartNumberFromPoolInternal", "Entered...");

            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }

            init(_context);

            String strPartRevision = this.confAttrNewPartRevision;
            String strPartPolicy = this.confAttrNewPartPolicy;
            String strPartType = args[0];
            String attrName = "";
            String attrValue = "";
            Map attrValueMap = new HashMap();
            for (int i = 1; i < args.length; i++) {
                attrName = args[i].substring(0, args[i].indexOf("|"));
                attrValue = args[i].substring(args[i].indexOf("|") + 1);
                attrValueMap.put(attrName, attrValue);
                System.out.println("attrValueMap:" + attrValueMap);
            }
            String strVault = "eService Production";
            String strPartDesc = "test part";
            String strPartOwner = context.getUser();

            String strAttrUnitOfMeasure = "inch";

            String partNumber = "";

            // String[] initArgs = new String[2];
            String[] initArgs = null;

            int numAutoNamesReqd = 1;

            Hashtable autoNameRqMap = new Hashtable();
            autoNameRqMap.put("type_CADDrawing|A Size", new Integer(numAutoNamesReqd));

            System.out.println("[retrievePartNumberFromPoolInternal]... calling AutoNumber Generator JPO...:");
            Hashtable resultsTable = (Hashtable) JPO.invoke(context, "DECNameGenerator", initArgs, "getNames", JPO.packArgs(autoNameRqMap), Hashtable.class);
            Vector partNumberVect = (Vector) resultsTable.get("type_CADDrawing|A Size");
            partNumber = (String) partNumberVect.elementAt(0);
            System.out.println("[retrievePartNumberFromPoolInternal]... return of AutoNumber Generator JPO...: partNumber: " + partNumber);

            // Create EC Part Object
            Policy policyObj = new Policy(strPartPolicy);
            String firstRevInSequence = policyObj.getFirstInSequence(context);
            strPartRevision = firstRevInSequence;

            System.out.println("[retrievePartNumberFromPoolInternal]... firstRevInSequence: " + firstRevInSequence);
            BusinessObject busObj = new BusinessObject(strPartType, partNumber, strPartRevision, strVault);
            Part partObj = new Part(busObj);
            System.out.println("[retrievePartNumberFromPoolInternal]... strPartPolicy: " + strPartPolicy);
            partObj.create(context, strPartPolicy);
            partObj.openObject(context);
            if (attrValueMap.size() > 0) {
                partObj.setAttributeValues(context, attrValueMap);
            }

            partNumber = partObj.getName();
            partObj.closeObject(context, true);
            String ecPartObjId = partObj.getObjectId(context);

            logMessage("retrievePartNumberFromPoolInternal", "returning the EC Part Number: " + partNumber);

            String strRDOName = "Company Name";
            createDesignResponsibilityRelnship(context, strRDOName, ecPartObjId);

            result = "true|" + partNumber;
        } catch (Exception e) {
            result = "false|Failed to retrieve EC Part Number.\n" + e.getMessage();
        }

        return result;
    }

    /**
     * This function is to create Design Responsibility relationship between EC Part and the Department / Organization.
     **/
    private void createDesignResponsibilityRelnship(Context context, String rdoName, String ecPartId) throws Exception {
        // Create connection between EC Part and RDO.
        StringList objectSelects = new StringList();
        objectSelects.add(DomainObject.SELECT_TYPE);
        objectSelects.add(DomainObject.SELECT_NAME);
        objectSelects.add(DomainObject.SELECT_REVISION);
        objectSelects.add(DomainObject.SELECT_ID);

        if (!bCustomerEnvironment) {
            rdoName = "Company Name";
            TYPE_RDO = "Company";
        }

        MapList objectList = DomainObject.findObjects(context, // Context
                TYPE_RDO, // Type
                rdoName, // Name
                DomainConstants.QUERY_WILDCARD, // Revision
                DomainConstants.QUERY_WILDCARD, // Owner
                DomainConstants.QUERY_WILDCARD, // Vault
                null, // Where expression
                true, // Expand Type
                objectSelects); // Selectable
        logMessage("retrievePartNumberFromPoolInternal", "objectList   " + objectList);
        if (objectList == null || objectList.size() == 0) {
            String tempMessage = "No RDO object found with type " + TYPE_RDO + " name :" + rdoName;
            logMessage("retrievePartNumberFromPoolInternal", tempMessage);
            throw new Exception(tempMessage);
        } else {
            String departmentObjectID = (String) ((HashMap) objectList.get(0)).get(DomainObject.SELECT_ID);

            DomainObject rdoObject = new DomainObject(departmentObjectID);
            DomainObject ecPartObj = new DomainObject(ecPartId);
            // Connect the Department object and EC Part object
            DomainRelationship.connect(context, rdoObject, DESIGN_RESPONSIBILITY_RELATIONSHIP, ecPartObj);
        }
    }

    /**
     * retrieve Part Number and update Attributes strOwner,strPolicy,strType,strRevision,strVault,attributesMap,strDesc
     **/
    public String retrievePartNumberFromPoolCustom(Context _context, String[] args) throws Exception {
        String result = "";
        try {
            logMessage("retrievePartNumberFromPoolCustom", "Entered...");

            String partNumber = "";
            //
            // TODO - Implement customer specific custom code here
            //
            // Logic to retreive Auto Number for EC Part that can be used to name the CAD Models.
            //

            result = "true|" + partNumber;
            logMessage("retrievePartNumberFromPoolCustom", "returning result: " + result);
        } catch (Exception e) {
            String msg = "Failed to retrieve Part Number.\n" + e.getMessage();
            return "false|" + msg;
        }

        return result;
    }

    /**
     * retrieve Drawing Number and send it to the ENOVIA New feature.
     **/
    public String retrieveDrawingNumberFromENOVIA(Context context, String[] args) throws Exception {
        String result = "";
        try {
            logMessage("retrieveDrawingNumberFromENOVIA", "Entered...");

            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }

            init(_context);

            String drawingNumber = "";

            // String[] initArgs = new String[2];
            String[] initArgs = null;

            int numAutoNamesReqd = 1;

            Hashtable autoNameRqMap = new Hashtable();
            autoNameRqMap.put("type_CADDrawing|A Size", new Integer(numAutoNamesReqd));

            Hashtable resultsTable = (Hashtable) JPO.invoke(context, "DECNameGenerator", initArgs, "getNames", JPO.packArgs(autoNameRqMap), Hashtable.class);
            Vector partNumberVect = (Vector) resultsTable.get("type_CADDrawing|A Size");
            drawingNumber = (String) partNumberVect.elementAt(0);

            logMessage("retrieveDrawingNumberFromENOVIA", "returning the Drawing Number: " + drawingNumber);

            result = "true|" + drawingNumber;

            // Retrieve User initials from the Person Object and return.
            if (result.startsWith("true|")) {
                String sCADSignature = getUserIntials(context);
                if (sCADSignature == null)
                    sCADSignature = "";
                if (!"".equals(sCADSignature)) {
                    result = result + "|" + sCADSignature;
                }
            }
        } catch (Exception e) {
            result = "false|Failed to retrieve Drawing Number.\n" + e.getMessage();
        }

        return result;
    }

    /**
     * getCommonPLMAttributes returns Common PLM attributes to be updated in the Design file. e.g., DESIGNED_BY, MODIFIED_BY,INITIALREVISION and INITIALSTATE. In addition, any other parameters can
     * also be returned as key-value pairs with the following syntax. PARAM1|param1Value@PARAM2|param2Value@...
     */
    public String getCommonPLMAttributes(Context context, String[] args) throws Exception {
        String sResult = "";

        logMessage("getCommonPLMAttributes", "Entered...");
        try {
            // Read User Initials from attribute 'CAD Signature' on the Person object of the Context User.
            String sUserCredentials = getUserIntials(context);
            String sDesignedBy = "DESIGNED_BY|" + sUserCredentials;
            String sModifiedBy = "MODIFIED_BY|" + sUserCredentials;

            // Read Initial Revision and State name from the Policy.
            String sPolicyName = POLICY_PROENGINEER_DESIGN;
            String sInitialRevAndState = getPolicyInitialRevAndInitialState(context, sPolicyName);

            sResult = sDesignedBy + "@" + sModifiedBy + "@" + sInitialRevAndState;
            logMessage("getCommonPLMAttributes", "Returning sResult: " + sResult);
        } catch (Exception e) {
            System.out.println("getCommonPLMAttributes: Exception occurred while retriveing common PLM attributes.");
        }

        return sResult;
    }

    /**
     * getPolicyInitialRevAndInitialState To Read Initial Revision and State Name from the Policy.
     **/
    private String getPolicyInitialRevAndInitialState(Context context, String policyName) throws Exception {
        String sResult = "";

        logMessage("getPolicyInitialRevAndInitialState", "Entered...");

        // Query 'INITIAL REVISION' in the revision sequence of the Policy.
        String sInitalRevision = "";
        String sInitialState = "";
        matrix.db.Policy policyObj = new matrix.db.Policy(policyName);
        policyObj.open(context);
        String sFirstRev = policyObj.getFirstInSequence();
        String sFirstRevision = policyObj.getFirstInMinorSequence();
        // System.out.println("firstRev: " + sFirstRev);
        // System.out.println("firstRevision: " + sFirstRevision);
        policyObj.close(context);

        sInitalRevision = "INITIALREVISION|" + sFirstRevision;
        logMessage("getPolicyInitialRevAndInitialState", "Initial Revision defined in Revision Sequence of the Policy, sInitalRevision: " + sInitalRevision);

        // Query 'name' of the First State in the Lifecycle.
        String sStatesQuery = "print policy '" + policyName + "' select state dump |";
        String sStateNamesResult = executeMQLCommand(context, sStatesQuery);
        if (sStateNamesResult.startsWith("true|")) {
            sStateNamesResult = sStateNamesResult.substring(5);
            int pipeIdx = sStateNamesResult.indexOf("|");
            if (pipeIdx > -1) {
                sInitialState = sStateNamesResult.substring(0, pipeIdx);
                sInitialState = "INITIALSTATE|" + sInitialState;
                logMessage("getPolicyInitialRevAndInitialState", "Initial State defined in Life Cycle of the Policy, sInitalState: " + sInitialState);
            }
        }

        sResult = sInitalRevision + "@" + sInitialState;
        logMessage("getPolicyInitialRevAndInitialState", "returning value: sResult: " + sResult);

        return sResult;
    }

    /**
     * getUserIntials returns the value of CAD Signature attribute from Person Object.
     **/
    private String getUserIntials(Context context) throws Exception {
        String sCADSignature = "";

        logMessage("getUserIntials", "Entered...");
        try {
            if (!bCustomerEnvironment) {
                // For OOTB, the 'CAD Signature' attribute won't be present.
                // So, return User Name.
                String sContextUserName = context.getUser();
                sCADSignature = sContextUserName;
            } else {
                //
                // TODO - In customer environment, if the CAD Signature is to be read from an attribute on the Person
                // Object, implement the code to read it from the Person Object and return it from here.
                // e.g., sCADSignature = "Test User"
                //
                sCADSignature = "";
            }

            logMessage("getUserIntials", "sCADSignature: " + sCADSignature);
        } catch (Exception e) {
        }

        return sCADSignature;
    }

    private void logMessage(String method, String message) {
        if (DEBUG) {
            String logMessage = new Date() + " ::: [ITIMxMCAD_ENOVIANewCustomJPO." + method + "] :::  " + message;
            if (WRITE_DEBUG_TO_RMI_LOGS && matrixLogger != null) {
                try {
                    matrixLogger.write(logMessage);
                    matrixLogger.flush();
                } catch (Exception e) {
                    System.out.println(logMessage);
                }
            } else {
                System.out.println(logMessage);
            }
        }
    }
}
