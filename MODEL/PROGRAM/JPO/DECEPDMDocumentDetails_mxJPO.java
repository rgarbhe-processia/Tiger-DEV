import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADFolderUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;

public class DECEPDMDocumentDetails_mxJPO {
    private static HashMap CachedNLSMap = null; // IR492366 map for caching NLS information

    public Object getFileName(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "fileName", args);
    }

    public Object getFileSize(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "fileSize", args);
    }

    public Object getFileModifiedDate(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "modifiedDate", args);
    }

    public Object getComputerName(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "computerName", args);
    }

    public Object getLockedPath(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "lockedpath", args);
    }

    public Object getLockerName(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "locker", args);
    }

    public Object getObjectCreated(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "objectcreated", args);
    }

    public Object getObjectModified(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "objectmodified", args);
    }

    public Object getObjectUpdateStamp(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "updatestamp", args);
    }

    public Object getMinorObjectModified(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "minorobjectmodified", args);
    }

    public Object getInstanceObjectModified(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "instanceobjectmodified", args);
    }

    public Object getInstanceUpdateStamp(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "instanceupdatestamp", args);
    }

    public Object getInstanceObjectCreated(Context context, String args[]) {
        return getParameterVectorFromIdMap(context, "instanceobjectcreated", args);
    }

    public Object getType(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "type", args);
    }

    public Object getState(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "state", args);
    }

    public Object getRevision(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "revision", args);
    }

    public Object getOwner(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "owner", args);
    }

    public Object getPolicy(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "policy", args);
    }

    public Object getVault(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "vault", args);
    }

    public Object getLockedStatus(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "lockedStatus", args);
    }

    public Object getLatestVersion(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "latestVersion", args);
    }

    public Object getTemplate(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "workspaceTemplateName", args);
    }

    public Object getVersion(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "version", args);
    }

    public Object getCurrentStateId(Context context, String args[]) // IR492366
    {
        return getParameterVectorFromIdMap(context, "currentStateId", args);
    }

    public Object getConnectedFolderInformation(Context context, String args[]) throws Exception // IR492366
    {
        Map dataMap = (Map) JPO.unpackArgs(args);
        HashMap paramList = (HashMap) dataMap.get("paramList");
        String localeLanguage = (String) paramList.get("LocaleLanguage");
        MCADServerResourceBundle serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        IEFGlobalCache cache = new IEFGlobalCache();

        String REL_VAULTED_DOCUMENTS = MCADMxUtil.getActualNameForAEFData(context, "relationship_VaultedDocuments"); // IR492366
        String TYPE_WORKSPACE_VAULT = MCADMxUtil.getActualNameForAEFData(context, "type_ProjectVault"); // IR492366
        String versionOfRelActualName = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");

        String GET_VAULT_IDS = new StringBuffer("to[").append(REL_VAULTED_DOCUMENTS).append("].from[").append(TYPE_WORKSPACE_VAULT).append("].id").toString(); // IR492366
        String SELECT_ON_MAJOR = new StringBuffer("from[").append(versionOfRelActualName).append("].to.").toString();

        String paramKey1 = GET_VAULT_IDS;
        String paramKey2 = SELECT_ON_MAJOR + GET_VAULT_IDS;
        Vector columnCellContentList = new Vector();
        try {
            MCADFolderUtil mcadFolderUtil = new MCADFolderUtil(context, serverResourceBundle, cache);

            if (isValidForEvaluation(dataMap)) {
                List objectList = (List) dataMap.get("objectList");
                if (!checkForDataExistense(paramKey1, objectList) && !checkForDataExistense(paramKey2, objectList)) {
                    initialize(context, dataMap);
                }

                HashMap folderIDLogicalIDMap = new HashMap();
                HashMap folderIDfolderPathMap = new HashMap();

                for (int i = 0; i < objectList.size(); i++) {
                    Map mpEachObj = (Map) objectList.get(i);
                    // BusinessObjectWithSelect busWithSelect = (BusinessObjectWithSelect) busWithSelectList.elementAt(i);
                    String folderPathFolderId = mcadFolderUtil.getPathForBusObjectForCheckout(context, null, folderIDfolderPathMap, folderIDLogicalIDMap, mpEachObj, ",", "|", false);

                    if (folderPathFolderId != null && !folderPathFolderId.equals(""))
                        columnCellContentList.add(folderPathFolderId);
                    else
                        columnCellContentList.add("");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return columnCellContentList;
    }

    // IR492366
    private static String getNLSFromMap(Context context, String type, String name, String policyName, String localeLanguage) // IR492366
    {
        String nlsName = "";
        String tempNameKey = type + "_" + name;

        if (null != CachedNLSMap && CachedNLSMap.containsKey(tempNameKey)) {
            nlsName = (String) CachedNLSMap.get(type + "_" + name);
        } /*
           * else if(type.equals("Type") || type.equals("Policy") || type.equals("Person")) { try { nlsName = EnoviaResourceBundle.getAdminI18NString(context, type, name, localeLanguage); } catch
           * (Exception e) { nlsName = ""; e.printStackTrace(); } updateNLSNameInMap(tempNameKey, nlsName);
           * 
           * }
           */
        else if (type.equals("State")) {
            try {
                nlsName = EnoviaResourceBundle.getStateI18NString(context, policyName, name, localeLanguage);
            } catch (Exception e) {
                nlsName = "";
                e.printStackTrace();
            }
            updateNLSNameInMap(tempNameKey, nlsName);
        } else {
            try {
                nlsName = EnoviaResourceBundle.getAdminI18NString(context, type, name, localeLanguage);
            } catch (Exception e) {
                nlsName = "";
                e.printStackTrace();
            }
            updateNLSNameInMap(tempNameKey, nlsName);
        }
        return nlsName;
    }

    // IR-492366-3DEXPERIENCER2015x, Feb 07,2017
    private static void updateNLSNameInMap(String NLSNameKey, String NLSValue) // IR492366
    {
        if (null == CachedNLSMap)
            CachedNLSMap = new HashMap();

        if (null != CachedNLSMap && !CachedNLSMap.containsKey(NLSNameKey))
            CachedNLSMap.put(NLSNameKey, NLSValue);
    }

    private Vector getParameterVectorFromIdMap(Context context, String paramKey, String args[]) {

        Vector retunValues = new Vector();
        try {
            Map dataMap = (Map) JPO.unpackArgs(args);
            if (isValidForEvaluation(dataMap)) {
                List objectList = (List) dataMap.get("objectList");
                if (!checkForDataExistense(paramKey, objectList)) {
                    initialize(context, dataMap);
                }
                for (int i = 0; i < objectList.size(); i++) {
                    Map idsMap = (Map) objectList.get(i);
                    retunValues.add(idsMap.get(paramKey).toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retunValues;
    }

    private String getNativeFormat(String mxType, String cadType, String source, Map globalConfigObjectsMap) {
        String nativeFormat = null;
        String integrationName = "";
        StringTokenizer integrationSourceTokens = new StringTokenizer(source, "|");

        if (integrationSourceTokens.hasMoreTokens())
            integrationName = integrationSourceTokens.nextToken();

        MCADGlobalConfigObject gco = (MCADGlobalConfigObject) globalConfigObjectsMap.get(integrationName);
        if (gco != null) {
            nativeFormat = gco.getFormatsForType(mxType, cadType);
        }
        return nativeFormat;
    }

    private String[] getObjectsId(Map dataMap) {
        List objectList = (List) dataMap.get("objectList");
        String[] objIds = new String[objectList.size()];
        for (int i = 0; i < objectList.size(); i++) {
            Map idsMap = (Map) objectList.get(i);
            objIds[i] = idsMap.get("id").toString();
        }
        return objIds;
    }

    private boolean isValidForEvaluation(Map dataMap) {
        boolean isValid = true;
        Map paramList = (Map) dataMap.get("paramList");
        String evaluatesColumns = paramList != null ? (String) paramList.get("evaluatecolumns") : null;
        if (evaluatesColumns != null && evaluatesColumns.trim().equalsIgnoreCase("false")) {
            isValid = false;
        }
        return isValid;
    }

    // Assumption : Input is major
    private void initialize(Context context, Map dataMap) {
        ArrayList familyids = new ArrayList();

        try {
            Map paramList = (Map) dataMap.get("paramList");
            Map globalConfigObjectsMap = (Map) paramList.get("GCOTable");

            List filterList = (List) paramList.get("filters");
            List objectList = (List) dataMap.get("objectList");

            String[] objsId = getObjectsId(dataMap);

            String localeLanguage = (String) paramList.get("LocaleLanguage"); // IR492366 changed from (String)dataMap.get("LocaleLanguage")

            String activeVersion = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
            String versionOfRelActualName = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");

            String SELECT_ON_MINOR = new StringBuffer("from[").append(activeVersion).append("].to.").toString();
            String SELECT_HAS_FILE = "format.hasfile";

            String ALL_FORMAT_SELECT = "format.file.format";
            String ALL_FORMAT_SELECT_ON_MINOR = new StringBuffer(SELECT_ON_MINOR).append(ALL_FORMAT_SELECT).toString();

            String ALL_FILE_NAME_SELECT = "format.file.name";
            String ALL_FILE_SIZE_SELECT = "format.file.size";
            String ALL_FILE_MODIFIED_DATE_SELECT = "format.file.modified.generic";
            String SELECT_OBJECT_MODIFIED = "modified.generic";
            String SELECT_OBJECT_CREATED = "originated.generic";
            String SELECT_UPDATE_STAMP = "updatestamp";

            String ALL_FILE_NAME_SELECT_ON_MINOR = new StringBuffer(SELECT_ON_MINOR).append(ALL_FILE_NAME_SELECT).toString();
            String ALL_FILE_SIZE_SELECT_ON_MINOR = new StringBuffer(SELECT_ON_MINOR).append(ALL_FILE_SIZE_SELECT).toString();
            String ALL_FILE_MODIFIED_DATE_SELECT_ON_MINOR = new StringBuffer(SELECT_ON_MINOR).append(ALL_FILE_MODIFIED_DATE_SELECT).toString();
            String SELECT_OBJECT_MODIFIED_ON_MINOR = new StringBuffer(SELECT_ON_MINOR).append(SELECT_OBJECT_MODIFIED).toString();

            String SELECT_SOURCE_ATT = new StringBuffer("attribute[").append(MCADMxUtil.getActualNameForAEFData(context, "attribute_Source")).append("]").toString();
            String SELECT_CAD_TYPE_ATT = new StringBuffer("attribute[").append(MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType")).append("]").toString();
            String SELECT_LOCK_INFO_ATT = new StringBuffer("attribute[").append(MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-LockInformation")).append("]").toString();
            String SELECT_TITLE_ATTR = new StringBuffer("attribute[").append(MCADMxUtil.getActualNameForAEFData(context, "attribute_Title")).append("]").toString();

            String SELECT_ON_MAJOR = new StringBuffer("from[").append(versionOfRelActualName).append("].to.").toString();
            // String SELECT_ON_MAJOR_ID = new StringBuffer(SELECT_ON_MAJOR).append("id").toString(); //IR492366 - not needed
            String SELECT_ON_MAJOR_LOCK_INFORMATION = new StringBuffer(SELECT_ON_MAJOR).append(SELECT_LOCK_INFO_ATT).toString();
            // String SELECT_ON_MAJOR_MX_TYPE = new StringBuffer(SELECT_ON_MAJOR).append(DomainConstants.SELECT_TYPE).toString(); //IR492366 - not needed
            String SELECT_ON_MAJOR_LOCKER = new StringBuffer(SELECT_ON_MAJOR).append(DomainConstants.SELECT_LOCKER).toString();
            String IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject"); // IR492366
            String SELECT_ISVERSIONOBJ = "attribute[" + IS_VERSION_OBJ + "]"; // IR492366

            String relLatestVersion = MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion"); // IR492366
            String REL_WORKSPACE_TEMPLATE = MCADMxUtil.getActualNameForAEFData(context, "relationship_WorkspaceTemplate"); // IR492366
            String SELECT_WORKSPACE_TEMPLATE_NAME = new StringBuffer("to[").append(REL_WORKSPACE_TEMPLATE).append("].from.name").toString(); // IR492366
            String REL_VAULTED_DOCUMENTS = MCADMxUtil.getActualNameForAEFData(context, "relationship_VaultedDocuments"); // IR492366
            String TYPE_WORKSPACE_VAULT = MCADMxUtil.getActualNameForAEFData(context, "type_ProjectVault"); // IR492366
            String GET_VAULT_IDS = new StringBuffer("to[").append(REL_VAULTED_DOCUMENTS).append("].from[").append(TYPE_WORKSPACE_VAULT).append("].id").toString(); // IR492366
            String LATEST_VERSION_ACROSS_REVISION = new StringBuffer("last.from[").append(activeVersion).append("].to.").append(DomainConstants.SELECT_REVISION).toString(); // IR492366
            String SELECT_ON_CURRENT_STATE_ID = new StringBuffer("policy.state.id").toString(); // IR492366

            String reportFormat = (String) paramList.get("reportFormat"); // IR492366
            StringList busSelectList = new StringList();

            busSelectList.add(SELECT_HAS_FILE);
            busSelectList.add(SELECT_TITLE_ATTR);
            busSelectList.add(ALL_FORMAT_SELECT);
            busSelectList.add(ALL_FORMAT_SELECT_ON_MINOR);
            busSelectList.add(ALL_FILE_NAME_SELECT);
            busSelectList.add(ALL_FILE_SIZE_SELECT);
            busSelectList.add(ALL_FILE_MODIFIED_DATE_SELECT);
            busSelectList.add(ALL_FILE_NAME_SELECT_ON_MINOR);
            busSelectList.add(ALL_FILE_SIZE_SELECT_ON_MINOR);
            busSelectList.add(ALL_FILE_MODIFIED_DATE_SELECT_ON_MINOR);

            busSelectList.add(SELECT_SOURCE_ATT);
            busSelectList.add(SELECT_CAD_TYPE_ATT);
            busSelectList.add(SELECT_LOCK_INFO_ATT);
            busSelectList.add(DomainConstants.SELECT_TYPE);
            busSelectList.add(DomainConstants.SELECT_LOCKER);
            busSelectList.add(SELECT_OBJECT_MODIFIED);
            busSelectList.add(SELECT_OBJECT_CREATED);
            busSelectList.add(SELECT_OBJECT_MODIFIED_ON_MINOR);
            busSelectList.add(DomainConstants.SELECT_ID);
            busSelectList.add(SELECT_UPDATE_STAMP);

            // busSelectList.add(SELECT_ON_MAJOR_ID); // IR492366 - not needed
            busSelectList.add(SELECT_ON_MAJOR_LOCK_INFORMATION);
            // busSelectList.add(SELECT_ON_MAJOR_MX_TYPE); // IR492366 - not needed
            busSelectList.add(SELECT_ON_MAJOR_LOCKER);

            busSelectList.add(DomainConstants.SELECT_CURRENT); // IR492366
            busSelectList.add(SELECT_ON_MAJOR + DomainConstants.SELECT_CURRENT); // IR492366
            busSelectList.add(SELECT_ISVERSIONOBJ); // IR492366
            busSelectList.add(DomainConstants.SELECT_REVISION); // IR492366
            busSelectList.add(SELECT_ON_MAJOR + DomainConstants.SELECT_REVISION); // IR492366
            busSelectList.add(DomainConstants.SELECT_OWNER); // IR492366
            busSelectList.add(SELECT_ON_MAJOR + DomainConstants.SELECT_OWNER); // IR492366
            busSelectList.add(DomainConstants.SELECT_POLICY); // IR492366
            busSelectList.add(SELECT_ON_MAJOR + DomainConstants.SELECT_POLICY); // IR492366
            busSelectList.add(DomainConstants.SELECT_VAULT); // IR492366
            busSelectList.add(DomainConstants.SELECT_LOCKED); // IR492366
            busSelectList.add(SELECT_ON_MAJOR + DomainConstants.SELECT_LOCKED); // IR492366
            busSelectList.add(SELECT_WORKSPACE_TEMPLATE_NAME); // IR492366 //possible ML
            busSelectList.add(SELECT_ON_MINOR + DomainConstants.SELECT_REVISION); // IR492366
            busSelectList.add(GET_VAULT_IDS); // IR492366 //ML
            busSelectList.add(SELECT_ON_MAJOR + GET_VAULT_IDS); // IR492366 //ML
            busSelectList.add(LATEST_VERSION_ACROSS_REVISION); // IR492366
            busSelectList.add(SELECT_ON_MAJOR + LATEST_VERSION_ACROSS_REVISION); // IR492366
            busSelectList.add(SELECT_ON_CURRENT_STATE_ID); // IR492366
            busSelectList.add(SELECT_ON_MAJOR + SELECT_ON_CURRENT_STATE_ID); // IR492366

            BusinessObjectWithSelectList busSelectDataList = BusinessObject.getSelectBusinessObjectData(context, objsId, busSelectList);

            for (int i = 0; i < busSelectDataList.size(); i++) {
                BusinessObjectWithSelect busSelectData = busSelectDataList.getElement(i);

                String busid = busSelectData.getSelectData(DomainConstants.SELECT_ID);
                String cadType = busSelectData.getSelectData(SELECT_CAD_TYPE_ATT);
                String mxType = busSelectData.getSelectData(DomainConstants.SELECT_TYPE);
                String source = busSelectData.getSelectData(SELECT_SOURCE_ATT);
                String title = busSelectData.getSelectData(SELECT_TITLE_ATTR);
                // String majorId = busSelectData.getSelectData(SELECT_ON_MAJOR_ID); //IR492366 - not needed
                String lockInformation = busSelectData.getSelectData(SELECT_LOCK_INFO_ATT);
                String locker = busSelectData.getSelectData(DomainConstants.SELECT_LOCKER);
                String currentState = ""; // IR492366
                String revision = ""; // IR492366
                String owner = ""; // IR492366
                String policy = ""; // IR492366
                String vault = ""; // IR492366
                String lockedStatus = ""; // IR492366
                String latestVersion = ""; // IR492366
                String workspaceTemplateName = ""; // IR492366
                String version = ""; // IR492366
                String currentStateId = ""; // IR492366
                StringList vaultIds = new StringList(); // IR492366

                currentState = (String) busSelectData.getSelectData(DomainConstants.SELECT_CURRENT); // IR492366
                revision = (String) busSelectData.getSelectData(DomainConstants.SELECT_REVISION); // IR492366
                owner = (String) busSelectData.getSelectData(DomainConstants.SELECT_OWNER); // IR492366
                policy = (String) busSelectData.getSelectData(DomainConstants.SELECT_POLICY); // IR492366
                vault = (String) busSelectData.getSelectData(DomainConstants.SELECT_VAULT); // IR492366
                lockedStatus = (String) busSelectData.getSelectData(DomainConstants.SELECT_LOCKED); // IR492366
                latestVersion = (String) busSelectData.getSelectData(LATEST_VERSION_ACROSS_REVISION); // IR492366
                workspaceTemplateName = (String) busSelectData.getSelectData(SELECT_WORKSPACE_TEMPLATE_NAME); // IR492366
                vaultIds = (StringList) busSelectData.getSelectDataList(GET_VAULT_IDS); // IR492366
                currentStateId = (String) busSelectData.getSelectData("policy.state[" + currentState + "].id"); // IR492366 getting current state id

                String minorObjectModified = busSelectData.getSelectData(SELECT_OBJECT_MODIFIED_ON_MINOR);

                Map idsMap = (Map) objectList.get(i);

                // boolean isInputMinor = majorId != null && !majorId.equals(""); //IR492366 - replaced with SELECT_ISVERSIONOBJ

                String isThisVersionObj = (String) busSelectData.getSelectData(SELECT_ISVERSIONOBJ); // IR492366
                boolean isVersion = Boolean.valueOf(isThisVersionObj).booleanValue(); // IR492366

                if (isVersion) // IR492366 - replace isInputMinor with SELECT_ISVERSIONOBJ
                {
                    lockInformation = busSelectData.getSelectData(SELECT_ON_MAJOR_LOCK_INFORMATION);
                    // mxType = busSelectData.getSelectData(SELECT_ON_MAJOR_MX_TYPE); //IR492366 - not needed
                    locker = busSelectData.getSelectData(SELECT_ON_MAJOR_LOCKER);
                    minorObjectModified = "";

                    currentState = (String) busSelectData.getSelectData(SELECT_ON_MAJOR + DomainConstants.SELECT_CURRENT); // IR492366
                    revision = (String) busSelectData.getSelectData(SELECT_ON_MAJOR + DomainConstants.SELECT_REVISION); // IR492366
                    owner = (String) busSelectData.getSelectData(SELECT_ON_MAJOR + DomainConstants.SELECT_OWNER); // IR492366
                    policy = (String) busSelectData.getSelectData(SELECT_ON_MAJOR + DomainConstants.SELECT_POLICY); // IR492366
                    lockedStatus = (String) busSelectData.getSelectData(SELECT_ON_MAJOR + DomainConstants.SELECT_LOCKED); // IR492366
                    latestVersion = (String) busSelectData.getSelectData(SELECT_ON_MAJOR + LATEST_VERSION_ACROSS_REVISION); // IR492366
                    vaultIds = (StringList) busSelectData.getSelectDataList(SELECT_ON_MAJOR + GET_VAULT_IDS); // IR492366
                    currentStateId = (String) busSelectData.getSelectData(SELECT_ON_MAJOR + "policy.state[" + currentState + "].id"); // IR492366

                    idsMap.put(SELECT_ON_MAJOR + GET_VAULT_IDS, vaultIds); // IR492366 add vault ID with key SELECT_ON_MAJOR+GET_VAULT_IDS
                }

                StringTokenizer integrationSourceTokens = new StringTokenizer(source, "|");
                String integrationName = "";
                if (integrationSourceTokens.hasMoreTokens())
                    integrationName = integrationSourceTokens.nextToken();

                MCADGlobalConfigObject gco = (MCADGlobalConfigObject) globalConfigObjectsMap.get(integrationName);

                if (null != gco && gco.isTypeOfClass(cadType, MCADAppletServletProtocol.TYPE_FAMILY_LIKE)) {
                    familyids.add(busid);
                } else {
                    idsMap.put("instanceobjectcreated", "");
                    idsMap.put("instanceobjectmodified", "");
                    idsMap.put("instanceupdatestamp", "");
                }

                if (integrationName != null && globalConfigObjectsMap.containsKey(integrationName)) // IR492366 - whole if block to get version
                {
                    // MCADGlobalConfigObject gco = (MCADGlobalConfigObject)globalConfigObjectsMap.get(integrationName);

                    if (null != gco && false == gco.isCreateVersionObjectsEnabled()) {
                        version = "--";
                    } else {
                        String minorVersion = "";
                        if (!isVersion) // [NDM] is Major Object //replace with isVersion
                        {
                            minorVersion = busSelectData.getSelectData(SELECT_ON_MINOR + DomainConstants.SELECT_REVISION);
                        } else {
                            minorVersion = busSelectData.getSelectData(DomainConstants.SELECT_REVISION);
                        }

                        if (minorVersion != null) {
                            int pos = minorVersion.lastIndexOf('.');
                            if (pos > -1)
                                version = minorVersion.substring(pos + 1);
                            else
                                version = minorVersion;
                        }
                    }
                } else {
                    String minorVersion = busSelectData.getSelectData(DomainConstants.SELECT_REVISION);

                    if (minorVersion != null) {
                        int pos = minorVersion.lastIndexOf('.');
                        if (pos > -1)
                            version = minorVersion.substring(pos + 1);
                        else {
                            minorVersion = busSelectData.getSelectData(SELECT_ON_MINOR + DomainConstants.SELECT_REVISION);
                            pos = minorVersion.lastIndexOf('.');
                            if (pos > -1)
                                version = minorVersion.substring(pos + 1);
                            else
                                version = minorVersion;
                        }
                    }
                }

                String computerName = "";
                String lockPath = "";
                List fileNameList = null;
                List formatList = null;
                List modifiedDateList = null;
                List sizeList = null;

                String isMajorHasFile = (String) busSelectData.getSelectDataList(SELECT_HAS_FILE).elementAt(0);

                if (isMajorHasFile.equalsIgnoreCase(MCADAppletServletProtocol.TRUE)) {
                    fileNameList = busSelectData.getSelectDataList(ALL_FILE_NAME_SELECT);
                    formatList = busSelectData.getSelectDataList(ALL_FORMAT_SELECT);
                    sizeList = busSelectData.getSelectDataList(ALL_FILE_SIZE_SELECT);
                    modifiedDateList = busSelectData.getSelectDataList(ALL_FILE_MODIFIED_DATE_SELECT);
                } else {
                    fileNameList = busSelectData.getSelectDataList(ALL_FILE_NAME_SELECT_ON_MINOR);
                    formatList = busSelectData.getSelectDataList(ALL_FORMAT_SELECT_ON_MINOR);
                    sizeList = busSelectData.getSelectDataList(ALL_FILE_SIZE_SELECT_ON_MINOR);
                    modifiedDateList = busSelectData.getSelectDataList(ALL_FILE_MODIFIED_DATE_SELECT_ON_MINOR);
                }

                if (lockInformation != null && !"".equals(lockInformation)) {
                    StringTokenizer token = new StringTokenizer(lockInformation, "|");
                    computerName = token.hasMoreTokens() ? token.nextToken() : "";
                    lockPath = token.hasMoreTokens() ? token.nextToken() : "";
                }

                String nativeFormat = getNativeFormat(mxType, cadType, source, globalConfigObjectsMap); // IR-545838: defining nativeFormat here to pass mxType original value from DB & not NLS value

                // getting NLS names
                mxType = getNLSFromMap(context, "Type", mxType, "", localeLanguage); // IR492366
                // owner = getNLSFromMap(context, "Person", owner, "" , localeLanguage); //IR492366
                currentState = getNLSFromMap(context, "State", currentState, policy, localeLanguage); // IR492366
                policy = getNLSFromMap(context, "Policy", policy, "", localeLanguage); // IR492366
                // locker = getNLSFromMap(context, "Person", locker, "" , localeLanguage); //IR492366

                // values to map
                if ("CSV".equalsIgnoreCase(reportFormat)) // IR492366
                {
                    idsMap.put("revision", "=\"" + revision + "\"");
                } else {
                    idsMap.put("revision", revision);
                }

                idsMap.put("computerName", computerName); // will be set as blank if not present
                idsMap.put("lockedpath", lockPath); // will be set as blank if not present
                idsMap.put("locker", locker);
                idsMap.put("fileName", "");
                idsMap.put("fileSize", "");
                idsMap.put("modifiedDate", "");
                idsMap.put("objectcreated", busSelectData.getSelectData(SELECT_OBJECT_CREATED));
                idsMap.put("objectmodified", busSelectData.getSelectData(SELECT_OBJECT_MODIFIED));
                idsMap.put("minorobjectmodified", minorObjectModified);
                idsMap.put("updatestamp", busSelectData.getSelectData(SELECT_UPDATE_STAMP));
                idsMap.put("type", mxType); // IR492366
                idsMap.put("vault", vault); // IR492366
                idsMap.put("state", currentState); // IR492366
                idsMap.put("owner", owner); // IR492366
                idsMap.put("policy", policy); // IR492366
                idsMap.put("lockedStatus", lockedStatus); // IR492366
                idsMap.put("latestVersion", latestVersion); // IR492366
                idsMap.put("workspaceTemplateName", workspaceTemplateName); // IR492366
                idsMap.put("version", version); // IR492366
                idsMap.put("currentStateId", currentStateId); // IR492366
                // idsMap.put("vaultIds", vaultIds); //IR492366 - later

                if (!idsMap.containsKey(SELECT_ON_MAJOR + GET_VAULT_IDS))
                    idsMap.put(GET_VAULT_IDS, vaultIds); // IR492366 add vault ID with key GET_VAULT_IDS if SELECT_ON_MAJOR + GET_VAULT_IDS is not there in map TODO remove later

                // String nativeFormat = getNativeFormat(mxType, cadType, source, globalConfigObjectsMap); //IR-545838: mxType passed is NLS, which returns nativeFormat null. so commenting movinf
                // definition of nativeFormat before fetching NLS

                // adding file related info
                Map fileData = getFileData(formatList, fileNameList, sizeList, modifiedDateList, nativeFormat, filterList, title);
                if (fileData != null)
                    idsMap.putAll(fileData);
            }

            if (!familyids.isEmpty())
                getInstanceObjectDateAttributes(context, familyids, objectList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getInstanceObjectDateAttributes(Context context, ArrayList familyids, List objectList) throws Exception {
        HashMap familyidsActiveMinorIdsMap = new HashMap();
        HashMap incomingIdActiveMinorIdsMap = new HashMap();

        String OBJECT_CREATED = "originated.generic";
        String OBJECT_MODIFIED = "modified.generic";
        String OBJECT_NAME = "name";
        String SELECT_UPDATE_STAMP = "updatestamp";

        String REL_INSTANCE_OF = MCADMxUtil.getActualNameForAEFData(context, "relationship_InstanceOf");

        String REL_ACTIVE_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");

        String SELECT_ON_INSTANCE = "from[" + REL_INSTANCE_OF + "].to.";

        String SELECT_ACTIVE_MINOR_ID = "from[" + REL_ACTIVE_VERSION + "].to.id";

        String[] familyidArray = new String[familyids.size()];

        familyids.toArray(familyidArray);

        StringList familyBusSelectList = new StringList();
        familyBusSelectList.add(DomainConstants.SELECT_ID);
        familyBusSelectList.add(SELECT_ACTIVE_MINOR_ID);

        BusinessObjectWithSelectList familyBusSelectDataList = BusinessObject.getSelectBusinessObjectData(context, familyidArray, familyBusSelectList);

        for (int i = 0; i < familyBusSelectDataList.size(); i++) {
            BusinessObjectWithSelect familyBusSelectData = familyBusSelectDataList.getElement(i);

            String familyid = familyBusSelectData.getSelectData(DomainConstants.SELECT_ID);
            String activeMinorId = familyBusSelectData.getSelectData(SELECT_ACTIVE_MINOR_ID);

            // incase of input is minor
            if (activeMinorId == null || activeMinorId.equals(""))
                activeMinorId = familyid;

            familyidsActiveMinorIdsMap.put(activeMinorId, familyid);
            incomingIdActiveMinorIdsMap.put(familyid, activeMinorId);
        }

        HashMap familydIdInstanceInfoMap = new HashMap();

        String[] familyActiveMinorids = new String[familyidsActiveMinorIdsMap.size()];

        familyidsActiveMinorIdsMap.keySet().toArray(familyActiveMinorids);

        StringList busSelectList = new StringList();
        busSelectList.add(DomainConstants.SELECT_ID);
        busSelectList.add(SELECT_ON_INSTANCE + OBJECT_NAME);
        busSelectList.add(SELECT_ON_INSTANCE + OBJECT_CREATED);
        busSelectList.add(SELECT_ON_INSTANCE + OBJECT_MODIFIED);
        busSelectList.add(SELECT_ON_INSTANCE + SELECT_UPDATE_STAMP);

        BusinessObjectWithSelectList familyActiveMinorBusSelectDataList = BusinessObject.getSelectBusinessObjectData(context, familyActiveMinorids, busSelectList);

        for (int i = 0; i < familyActiveMinorBusSelectDataList.size(); i++) {
            BusinessObjectWithSelect familyActiveMinorBusSelectData = familyActiveMinorBusSelectDataList.getElement(i);

            String familyActiveMinorid = familyActiveMinorBusSelectData.getSelectData(DomainConstants.SELECT_ID);

            String familyid = (String) familyidsActiveMinorIdsMap.get(familyActiveMinorid);

            StringList instanceNames = familyActiveMinorBusSelectData.getSelectDataList(SELECT_ON_INSTANCE + OBJECT_NAME);
            StringList instanceCreatedTimes = familyActiveMinorBusSelectData.getSelectDataList(SELECT_ON_INSTANCE + OBJECT_CREATED);
            StringList instanceModifiedTimes = familyActiveMinorBusSelectData.getSelectDataList(SELECT_ON_INSTANCE + OBJECT_MODIFIED);

            StringList instanceUpdatestampTimes = familyActiveMinorBusSelectData.getSelectDataList(SELECT_ON_INSTANCE + SELECT_UPDATE_STAMP);

            StringBuffer instanceObjectCreated = new StringBuffer(45 * instanceNames.size());
            StringBuffer instanceObjectModified = new StringBuffer(45 * instanceNames.size());
            StringBuffer instanceupdatestamp = new StringBuffer(45 * instanceNames.size());

            for (int k = 0; k < instanceNames.size(); k++) {
                if (k > 0) {
                    instanceObjectCreated.append("|");
                    instanceObjectModified.append("|");
                    instanceupdatestamp.append("|");
                }

                String instanceName = (String) instanceNames.elementAt(k);
                String instanceCreated = (String) instanceCreatedTimes.elementAt(k);
                String instanceModified = (String) instanceModifiedTimes.elementAt(k);
                String updateStamp = (String) instanceUpdatestampTimes.elementAt(k);

                instanceObjectCreated.append(instanceName).append("##").append(instanceCreated);
                instanceObjectModified.append(instanceName).append("##").append(instanceModified);
                instanceupdatestamp.append(instanceName).append("##").append(updateStamp);

            }

            HashMap instanceInfoMap = new HashMap();

            instanceInfoMap.put("instanceobjectcreated", instanceObjectCreated.toString());
            instanceInfoMap.put("instanceobjectmodified", instanceObjectModified.toString());
            instanceInfoMap.put("instanceupdatestamp", instanceupdatestamp.toString());

            familydIdInstanceInfoMap.put(familyid, instanceInfoMap);
        }

        for (Iterator iterator = objectList.iterator(); iterator.hasNext();) {
            Map idsMap = (Map) iterator.next();

            String id = (String) idsMap.get("id");

            if (familydIdInstanceInfoMap.containsKey(id)) {
                Map instanceInfoMap = (Map) familydIdInstanceInfoMap.get(id);

                idsMap.putAll(instanceInfoMap);
            } else if (incomingIdActiveMinorIdsMap.containsKey(id))// added if incoming object is minor
            {
                String activeminorId = (String) incomingIdActiveMinorIdsMap.get(id);
                if (activeminorId != null && !activeminorId.equals("") && familyidsActiveMinorIdsMap.containsKey(id))
                    activeminorId = (String) familyidsActiveMinorIdsMap.get(id);

                Map instanceInfoMap = (Map) familydIdInstanceInfoMap.get(activeminorId);

                idsMap.putAll(instanceInfoMap);
            }
        }
    }

    private Map getFileData(List formatList, List fileNameList, List sizeList, List modifiedDateList, String nativeFormat, List filterList, String title) {
        Map fileData = null;

        List allNativeFileDataList = new ArrayList();
        if (null != formatList) {
            Iterator formatItr = formatList.iterator();
            int formatIndex = 0;

            while (formatItr.hasNext()) {
                String format = (String) formatItr.next();

                String fileName = (String) fileNameList.get(formatIndex);
                String fileSize = (String) sizeList.get(formatIndex);
                String modifiedDate = (String) modifiedDateList.get(formatIndex);
                if ((format.equals(nativeFormat) || nativeFormat == null) && fileName.equalsIgnoreCase(title)) {
                    Map fileInfoMap = new Hashtable();

                    fileInfoMap.put("fileName", fileName);
                    fileInfoMap.put("fileSize", fileSize);
                    fileInfoMap.put("modifiedDate", modifiedDate);

                    allNativeFileDataList.add(fileInfoMap);
                }

                formatIndex++;
            }

            if (!allNativeFileDataList.isEmpty())
                fileData = (Map) allNativeFileDataList.get(0);
        }
        return fileData;
    }

    private boolean checkForDataExistense(String key, List objectList) {
        boolean hasDataForKey = false;
        try {
            for (int i = 0; i < objectList.size(); i++) {
                Map idsMap = (Map) objectList.get(i);
                if (idsMap.containsKey(key)) {
                    hasDataForKey = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasDataForKey;
    }
}
