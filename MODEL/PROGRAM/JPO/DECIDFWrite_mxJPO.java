
/*
 ** ${CLASSNAME}
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** 
 */
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Enumeration;
import java.util.HashSet;

import matrix.db.Attribute;
import matrix.db.AttributeType;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.BusinessObject;
import matrix.db.AttributeList;
import matrix.db.BusinessObjectProxy;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.db.BusinessObjectWithSelectList;
import matrix.util.StringList;
import matrix.util.MatrixException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.fcs.mcs.Checkin;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.utils.xml.IEFXmlNode;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADXMLUtils;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;

public class DECIDFWrite_mxJPO {
    private MCADServerGeneralUtil generalUtil = null;

    private MCADMxUtil util = null;

    private static final boolean DEBUG = false;

    protected int QUERY_NAMES_LIMIT = 1000;

    protected String clientLang = "en";

    private static final String CADTYPE = "type";

    public DECIDFWrite_mxJPO(Context context, String[] args) {
        generalUtil = new MCADServerGeneralUtil(context, null, new MCADServerResourceBundle("en"), new IEFGlobalCache());
        util = new MCADMxUtil(context, new MCADServerResourceBundle("en"), new IEFGlobalCache());
    }

    public String createAndConnectIDF(Context context, String[] args) throws Exception {

        HashMap argsMap = (HashMap) JPO.unpackArgs(args);

        String idfcheckindetailsNodeXml = (String) argsMap.get("inputXml");
        String lang = (String) argsMap.get("lang");
        if (lang != null)
            clientLang = lang;
        IEFXmlNode idfcheckindetailsNode = MCADXMLUtils.parse(idfcheckindetailsNodeXml, "UTF8");
        String store = idfcheckindetailsNode.getAttribute("store");
        String receipt = idfcheckindetailsNode.getAttribute("reciept");

        return createAndConnectIDF(context, idfcheckindetailsNode, store, receipt);
    }

    private void printDebugTrace(Exception exception) {
        if (DEBUG) {
            exception.printStackTrace();
        }
    }

    protected String createAndConnectIDF(Context context, IEFXmlNode idfcheckindetailsNode, String store, String receipt) throws Exception {
        String retVal = null;
        try {
            util.startTransaction(context);

            MapList checkinList = createMetadata(context, idfcheckindetailsNode);
            commitCheckin(context, checkinList, receipt, store);
            // idfcheckindetailsNode.setAttributeValue("result", "success");
            // retXml = idfcheckindetailsNode.getXmlString();
            retVal = getSuccessMessage(idfcheckindetailsNode);
            util.commitTransaction(context);
        } catch (Exception e) {
            context.abort();
            printDebugTrace(e);
            String failureReason = e.getMessage();
            // retXml = getFailureXMLString(failureReason);
            retVal = getFailureMessage(failureReason);
        }
        return retVal;
    }

    public MapList createMetadata(Context context, String[] args) throws Exception {
        util.startTransaction(context);
        HashMap argsMap = (HashMap) JPO.unpackArgs(args);

        String idfcheckindetailsNodeXml = (String) argsMap.get("inputXml");
        String lang = (String) argsMap.get("lang");
        if (lang != null)
            clientLang = lang;
        IEFXmlNode idfcheckindetailsNode = MCADXMLUtils.parse(idfcheckindetailsNodeXml, "UTF8");
        MapList retList = createMetadata(context, idfcheckindetailsNode);
        util.commitTransaction(context);
        return retList;
    }

    public MapList createMetadata(Context context, IEFXmlNode idfcheckindetailsNode) throws Exception {
        MapList checkinList = new MapList();

        IEFXmlNode cadObjectListNode = MCADXMLUtils.getChildNodeWithName(idfcheckindetailsNode, "cadobjectlist");
        Enumeration cadObjectList = cadObjectListNode.elements();

        Map revIdMap = createLatestRevTable(context, idfcheckindetailsNode);
        while (cadObjectList.hasMoreElements()) {
            IEFXmlNode cadObjectNode = (IEFXmlNode) cadObjectList.nextElement();
            createObject(context, cadObjectNode, revIdMap, checkinList);
        }
        return checkinList;
    }

    private String getSuccessMessage(IEFXmlNode idfcheckindetailsNode) {
        String message = "S_OK";
        return message;
    }

    private String getFailureMessage(String failureReason) {
        // return "false|" + failureReason;
        return "E_FAIL";
    }

    private void commitCheckin(Context context, MapList checkinList, String receipt, String store) throws Exception {
        java.util.ArrayList arraylist = new java.util.ArrayList();
        ListIterator CheckinListEnum = checkinList.listIterator();
        while (CheckinListEnum.hasNext()) {
            Map checkinMap = (Map) CheckinListEnum.next();
            String id = (String) checkinMap.get("id");
            String format = (String) checkinMap.get("format");
            String filename = (String) checkinMap.get("filename");

            BusinessObjectProxy businessobjectproxy = new BusinessObjectProxy(id, format, filename, true, false);
            arraylist.add(businessobjectproxy);
        }
        try {
            Checkin.doIt(context, receipt, store, arraylist);
        } catch (MatrixException me) {
            // errorCode: FileCheckinCommit failed
            // Core
            String errCode = "IEF0233300289";// Failed committing file checkin
            String errMsg = i18nNow.getI18nString("mcadIntegration.Server.Message.IEF0233300289", "iefStringResource", clientLang);
            MCADServerException.createManagedException(errCode, errMsg, me);
        }

    }

    private void createObject(Context context, IEFXmlNode cadObjectNode, Map revIdMap, MapList checkinList) throws Exception {
        String busName = cadObjectNode.getAttribute("name");
        String busType = cadObjectNode.getAttribute("mxtype");
        String latestRevisionid = getLatestRevisionId(revIdMap, busType, busName);

        if (latestRevisionid == null)
            createNewObject(context, cadObjectNode, busName, checkinList);
        else {
            String IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
            String SELECT_ISVERSIONOBJ = "attribute[" + IS_VERSION_OBJ + "]";

            String relVersionOf = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
            String SELECT_MAJOR = "from[" + relVersionOf + "].to.id";

            StringList slSelectsForInputID = new StringList(2);
            slSelectsForInputID.addElement(SELECT_ISVERSIONOBJ);
            slSelectsForInputID.addElement(SELECT_MAJOR);

            StringList slOid = new StringList(1);
            slOid.addElement(latestRevisionid);

            String[] oidsTopLevel = new String[slOid.size()];
            slOid.toArray(oidsTopLevel);

            BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, oidsTopLevel, slSelectsForInputID);
            BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(0);
            String isThisVersionObj = (String) busObjectWithSelect.getSelectData(SELECT_ISVERSIONOBJ);
            boolean isVersion = Boolean.valueOf(isThisVersionObj).booleanValue();

            if (isVersion) {
                String sMajorId = (String) busObjectWithSelect.getSelectData(SELECT_MAJOR);
                latestRevisionid = sMajorId;
            }

            versionObject(context, cadObjectNode, latestRevisionid, checkinList);
        }
    }

    protected void versionObject(Context context, IEFXmlNode cadObjectNode, String targetId, MapList checkinList) throws Exception {
        try {
            BusinessObject busObject = null;
            BusinessObject thisMajorObject = new BusinessObject(targetId);

            busObject = reviseObjectWithinStream(context, cadObjectNode, thisMajorObject, null);
            String id = busObject.getObjectId();

            storeCreatedObjDetails(targetId, cadObjectNode, checkinList);
        } catch (MatrixException me) {
            String errCode = "IEF0233300290";// Failed getting target revision object
            String errMsg = i18nNow.getI18nString("mcadIntegration.Server.Message.IEF0233300290", "iefStringResource", clientLang);
            MCADServerException.createManagedException(errCode, errMsg, me);
        }
    }

    private BusinessObject reviseObjectWithinStream(Context context, IEFXmlNode cadObjectNode, BusinessObject majorObj, String targetRev) throws Exception {
        BusinessObject revisedObject = null;
        String latestMinorId = "";
        try {
            BusinessObject latestMinor = null;
            latestMinorId = util.getLatestMinorID(context, majorObj);
            BusinessObject thisLatestObject = new BusinessObject(latestMinorId);

            util.copyFilesFcsSupported(context, majorObj, thisLatestObject);

            if (!"".equals(latestMinorId) && latestMinorId.length() > 0) {
                latestMinor = new BusinessObject(latestMinorId);
                latestMinor.open(context);
                revisedObject = util.reviseBusinessObject(context, latestMinor, targetRev, false, false);
                latestMinor.close(context);
            }
        } catch (MatrixException me) {
            String errCode = "IEF0233300291";// Failed getting latest minor
            String errMsg = i18nNow.getI18nString("mcadIntegration.Server.Message.IEF0233300291", "iefStringResource", clientLang);
            MCADServerException.createManagedException(errCode, errMsg, me);
        } catch (MCADException me) {
            String errCode = "IEF0233200292";// Failed revising latest minor
            String errMsg = i18nNow.getI18nString("mcadIntegration.Server.Message.IEF0233200292", "iefStringResource", clientLang);
            MCADServerException.createManagedException(errCode, errMsg, me);
        }
        return revisedObject;
    }

    private void createNewObject(Context context, IEFXmlNode cadObjectNode, String busName, MapList checkinList) throws Exception {
        String targetRev = getRevisionString(context, cadObjectNode);
        String busRev = getVersionString(cadObjectNode, targetRev);

        String targetType = cadObjectNode.getAttribute("mxtype");

        BusinessObject majorObject = createNewMajorObjectFromCadobjectNode(context, cadObjectNode, targetType, busName, targetRev);
        String majorObjectId = majorObject.getObjectId();
        setAttributesOnMajor(context, majorObject, cadObjectNode);
        // TODO: update attributes on Major here...
        util.executeMQL(context, "set env global MCADINTEGRATION_CONTEXT true");
        BusinessObject minorObject = createBusObjectForCadObjectNode(context, cadObjectNode, targetType, busName, busRev);
        util.executeMQL(context, "unset env global MCADINTEGRATION_CONTEXT");
        String minorObjectId = minorObject.getObjectId();
        setAttributesOnMinor(context, minorObject, cadObjectNode);
        // TODO: update attributes on minor here

        generalUtil.connectMajorAndMinorObjects(context, minorObjectId, majorObjectId);
        storeCreatedObjDetails(majorObjectId, cadObjectNode, checkinList);
    }

    private void setAttributesOnMinor(Context context, BusinessObject minorObject, IEFXmlNode cadObjectNode) throws MCADException, MatrixException {
        Map attributesMap = getMinorAttributesNameAndValues(context, cadObjectNode);
        setAttributes(context, minorObject, attributesMap);
    }

    private Map getMinorAttributesNameAndValues(Context context, IEFXmlNode cadObjectNode) throws MCADException {
        return getAttributesForIDF(context, cadObjectNode, true);
    }

    private void setAttributesOnMajor(Context context, BusinessObject majorObject, IEFXmlNode cadObjectNode) throws MCADException, MatrixException {
        Map attributesMap = getMajorAttributesNameAndValues(context, cadObjectNode);
        setAttributes(context, majorObject, attributesMap);
    }

    private Map getMajorAttributesNameAndValues(Context context, IEFXmlNode cadObjectNode) throws MCADException {
        return getAttributesForIDF(context, cadObjectNode, false);
    }

    private Map getAttributesForIDF(Context context, IEFXmlNode cadObjectNode, boolean isVersionObject) throws MCADException {
        Map attrMap = new HashMap();
        final String ATTRIBUTE_CADTYPE = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");
        final String ATTRIBUTE_MOVE_FILES_TO_VERSION = MCADMxUtil.getActualNameForAEFData(context, "attribute_MoveFilesToVersion");
        final String ATTRIBUTE_IS_VERSION_OBJECT = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
        final String ATTRIBUTE_TITLE = MCADMxUtil.getActualNameForAEFData(context, "attribute_Title");

        String strCadType = cadObjectNode.getAttribute(CADTYPE);
        String title = cadObjectNode.getAttribute("name");
        String moveFilesToVersion = "True";
        String strVersionObject = "False";
        if (isVersionObject) {
            moveFilesToVersion = "False";
            strVersionObject = "True";
            title = cadObjectNode.getAttribute("filename");
        }

        attrMap.put(ATTRIBUTE_CADTYPE, strCadType);
        attrMap.put(ATTRIBUTE_MOVE_FILES_TO_VERSION, moveFilesToVersion);
        attrMap.put(ATTRIBUTE_IS_VERSION_OBJECT, strVersionObject);
        attrMap.put(ATTRIBUTE_TITLE, title);
        System.out.println("IDF Write.1: " + attrMap);
        return attrMap;
    }

    private void setAttributes(Context context, BusinessObject busObject, Map attributesMap) throws MatrixException {
        if (busObject != null && attributesMap != null) {
            AttributeList attributelist = new AttributeList();
            Iterator itr = attributesMap.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) itr.next();
                String attrValue = (String) attributesMap.get(attrName);
                // TODO:Check if the attribute exists on this bus type
                attributelist.addElement(new Attribute(new AttributeType(attrName), attrValue));
            }
            busObject.open(context);
            busObject.setAttributes(context, attributelist);
            busObject.close(context);
        }
    }

    private void storeCreatedObjDetails(String majorObjectId, IEFXmlNode cadObjectNode, MapList checkinList) {
        Map checkinMap = new HashMap();
        String fileName = cadObjectNode.getAttribute("filename");
        String format = cadObjectNode.getAttribute("format");
        checkinMap.put("filename", fileName);
        checkinMap.put("format", format);
        checkinMap.put("id", majorObjectId);
        checkinList.add(checkinMap);

        cadObjectNode.setAttributeValue("id", majorObjectId);
    }

    private BusinessObject createBusObjectForCadObjectNode(Context context, IEFXmlNode cadObjectNode, String majorType, String busName, String busRev) throws Exception {
        BusinessObject busObject = null;
        String busType = "";

        busType = util.getCorrespondingType(context, majorType);
        String policy = (cadObjectNode.getAttribute("versionpolicy").trim());

        try {
            busObject = new BusinessObject(busType, busName, busRev, "");
            busObject.create(context, policy);
        } catch (MatrixException me) {
            String errCode = "IEF0233300293";// Failed creating target version
            String errMsg = i18nNow.getI18nString("mcadIntegration.Server.Message.IEF0233300293", "iefStringResource", clientLang);
            MCADServerException.createManagedException(errCode, errMsg, me);
        }
        return busObject;

    }

    private BusinessObject createNewMajorObjectFromCadobjectNode(Context context, IEFXmlNode cadObjectNode, String busType, String busName, String busRev) throws Exception {
        BusinessObject busObject = null;
        String majorPolicy = cadObjectNode.getAttribute("designpolicy");
        busObject = getTargetRevBusObject(context, busType, busName, busRev, majorPolicy);

        return busObject;

    }

    private BusinessObject getTargetRevBusObject(Context context, String targetType, String busName, String targetRev, String majPolicy) throws Exception {
        BusinessObject targetBusObject = null;
        try {
            targetBusObject = new BusinessObject(targetType, busName, targetRev, "");
            targetBusObject.create(context, majPolicy);
        } catch (MatrixException me) {
            String errCode = "IEF0233300294";// Failed creating target Major
            String errMsg = i18nNow.getI18nString("mcadIntegration.Server.Message.IEF0233300294", "iefStringResource", clientLang);
            MCADServerException.createManagedException(errCode, errMsg, me);
        }
        return targetBusObject;
    }

    private String getRevisionString(Context context, IEFXmlNode cadObjectNode) throws Exception {
        String targetRevision = null;
        try {
            String policyName = (cadObjectNode.getAttribute("designpolicy")).trim();
            matrix.db.Policy policy = new matrix.db.Policy(policyName);
            if (policy.hasSequence(context)) {
                targetRevision = policy.getFirstInSequence(context);
            } else {
                // [huv]...
            }
        } catch (MatrixException me) {
            String errCode = "IEF0233300295";// Failed getting Policy Sequence on policy %s
            String errMsg = i18nNow.getI18nString("mcadIntegration.Server.Message.IEF0233300295", "iefStringResource", clientLang);
            MCADServerException.createManagedException(errCode, errMsg, me);

        }

        return targetRevision;
    }

    private String getVersionString(IEFXmlNode cadObjectNode, String targetRev) {
        String version = null;

        version = util.getFirstVersionStringForStream(targetRev);
        return version;
    }

    boolean getBoolean(String strFlag) {
        boolean bRet = false;
        if (strFlag != null && strFlag.equals("true"))
            bRet = true;
        return bRet;
    }

    Map createLatestRevTable(Context context, IEFXmlNode idfcheckindetailsNode) {
        Map revIdMap = new HashMap();
        IEFXmlNode cadObjectListNode = MCADXMLUtils.getChildNodeWithName(idfcheckindetailsNode, "cadobjectlist");
        Enumeration cadObjectList = cadObjectListNode.elements();
        StringBuffer typeList = new StringBuffer();

        StringList busSelects = new StringList();
        busSelects.add("id");
        busSelects.add("type");
        busSelects.add("name");
        busSelects.add("revision");

        while (cadObjectList.hasMoreElements()) {
            StringBuffer nameList = new StringBuffer();
            HashSet typeSet = new HashSet();
            for (int i = 0; i < QUERY_NAMES_LIMIT && cadObjectList.hasMoreElements(); i++) {
                IEFXmlNode cadObjectNode = (IEFXmlNode) cadObjectList.nextElement();
                String type = cadObjectNode.getAttribute("mxtype");
                String name = cadObjectNode.getAttribute("name");

                if (type != null)
                    typeSet.add(type);
                if (name != null)
                    nameList.append(name);

                if (cadObjectList.hasMoreElements()) {
                    nameList.append(",");
                }
            }
            String nameListForQuery = nameList.toString();
            String typeListForQuery = com.matrixone.MCADIntegration.utils.MCADUtil.getDelimitedStringFromCollection(typeSet, ",");

            Query query = new Query();
            query.setBusinessObjectType(typeListForQuery);
            query.setBusinessObjectName(nameListForQuery);
            query.setBusinessObjectRevision("*");
            query.setWhereExpression("id==last.id");
            try {

                QueryIterator queryIterator = query.getIterator(context, busSelects, (short) 1000);

                while (queryIterator.hasNext()) {
                    BusinessObjectWithSelect busWithSelect = queryIterator.next();
                    String id = busWithSelect.getSelectData("id");
                    String type = busWithSelect.getSelectData("type");
                    String name = busWithSelect.getSelectData("name");

                    StringBuffer nameTypeBuffer = new StringBuffer(name);
                    nameTypeBuffer.append("|").append(type);

                    revIdMap.put(nameTypeBuffer.toString(), id);
                }
                queryIterator.close();
            } catch (MatrixException me) {

            }
        }

        return revIdMap;
    }

    String getLatestRevisionId(Map revIdMap, String type, String name) {
        String LatestRevId = null;
        String key = new StringBuffer(name).append("|").append(type).toString();
        String id = (String) revIdMap.get(key);

        if (id != null)
            LatestRevId = id;
        return LatestRevId;
    }
}
