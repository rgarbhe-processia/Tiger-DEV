import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.DSCExpandObjectWithSelect;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleObjectExpander;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.utils.MCADProfiler;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.db.RelationshipWithSelect;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;

public class SLWFolderMigration_mxJPO {
    private static final String SLW_GCO_TYPE = "SolidWorksInteg-GlobalConfig";

    private static final String SLW_GCO_NAME = "SWNewArch";

    private static final String DRAWING_LIKE_CADTYPES = "drawing"; // Enter comma-separated list here if custom drawing types are used.

    private static final boolean PROFILER_ENABLED = false;

    private MCADGlobalConfigObject _gco = null;

    private MCADMxUtil _mxUtil = null;

    private MCADServerResourceBundle _res = null;

    private IEFGlobalCache _cache = null;

    private String localeLanguage;

    private MCADServerResourceBundle serverResourceBundle;

    private IEFGlobalCache cache;

    private MCADServerGeneralUtil _generalUtil;

    private SLWFolderMigrationExpander_mxJPO migrationExpander = null;

    private HashMap programMap = null;

    private Map<String, List<String>> firstMajorRevIdAllVersionsMap = null;

    private Map<String, IEFFamilyRevTreeNode> firstMajorIdTreeNodeMap = null;

    private Map<String, IEFFamilyRevTreeNode> latestFamilyIdFamilyTreeNodeMap = null;

    private Map<String, String> firstMajorIdStatusMap = null;

    private Map<String, IEFFamilyRevTreeNode> expandedIdsFamilyTreeNodeCache = null;

    private Set<String> expandedIdsCache = null;

    private Set<String> familiesForVersionProcessing = null;

    private Set<String> familiesProcessedForVersion = null;

    private String folderObjectId = null;

    private Map<String, List<Set<String>>> folderIdFolderDetailsMap = null;

    private Map<String, String[]> busIdLatestBusDetailsMap = null;

    private Map<String, Set<Map>> latestFamilyIdRelBusDataMap = null;

    private Set<String> duplicateFileNameIds = null;

    private Map<String, StringList[]> lastFamilyIdConnectedFoldersMap = null;

    private Map<String, String> propertyKeyValueMap = null;

    private static final String SELECT_EXPR_REVISIONS = "revisions";

    private static final String SELECT_EXPR_REVISION_IDS = "revisions.id";

    private static String SELECT_EXPR_ACTIVE_VERSION = "";

    private static String SELECT_EXPR_VERSIONOF = "";

    private static String SELECT_EXPR_MAJOR_ACTIVE = "";

    private static String SELECT_EXPR_MAJOR_VERSIONOF = "";

    private static String SELECT_EXPR_VERSIONS = "";

    private static String SELECT_EXPR_VERSION_IDS = "";

    private static String SELECT_EXPR_MAJOR_REVISIONS_ACTIVE = "";

    private static String SELECT_EXPR_MAJOR_REVISION_IDS_ACTIVE = "";

    private static String SELECT_EXPR_MAJOR_REVISION_MINORS_ACTIVE = "";

    private static String SELECT_EXPR_MAJOR_REVISION_MINORS_ACTIVE_IDS = "";

    private static String SELECT_EXPR_MAJOR_REVISION_MINORS_VERSIONOF = "";

    private static String RELATIONSHIP_ACTIVE_VERSION = "";

    private static String RELATIONSHIP_VERSIONOF = "";

    private static String RELATIONSHIP_INSTANCEOF = "";

    private static String RELATIONSHIP_VAULTEDOBJECTS = "";

    private static String SELECT_EXPR_MAJOR_REVISIONS_VERSIONOF = "";

    private static String SELECT_EXPR_MAJOR_REVISION_IDS_VERSIONOF = "";

    private static String TYPE_CADDRAWING = "";

    private static final String ERROR_DUPLICATE_FILE = "Error:Duplicate file name";

    private static final String ERROR_REFERENCE_DUPLICATE_FILE = "Error:Reference has duplicate file name";

    public SLWFolderMigration_mxJPO(Context context, String[] args) throws Exception {
        RELATIONSHIP_ACTIVE_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
        RELATIONSHIP_VERSIONOF = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
        RELATIONSHIP_INSTANCEOF = MCADMxUtil.getActualNameForAEFData(context, "relationship_InstanceOf");
        RELATIONSHIP_VAULTEDOBJECTS = MCADMxUtil.getActualNameForAEFData(context, "relationship_VaultedDocuments");
        TYPE_CADDRAWING = MCADMxUtil.getActualNameForAEFData(context, "type_CADDrawing");

        SELECT_EXPR_ACTIVE_VERSION = "from[" + RELATIONSHIP_ACTIVE_VERSION + "].to";
        SELECT_EXPR_VERSIONOF = "to[" + RELATIONSHIP_VERSIONOF + "].from";
        SELECT_EXPR_MAJOR_ACTIVE = "to[" + RELATIONSHIP_ACTIVE_VERSION + "].from";
        SELECT_EXPR_MAJOR_VERSIONOF = "from[" + RELATIONSHIP_VERSIONOF + "].to";
        SELECT_EXPR_VERSIONS = SELECT_EXPR_REVISIONS + "." + SELECT_EXPR_ACTIVE_VERSION + "." + SELECT_EXPR_REVISIONS;
        SELECT_EXPR_VERSION_IDS = SELECT_EXPR_VERSIONS + "." + DomainConstants.SELECT_ID;
        SELECT_EXPR_MAJOR_REVISIONS_ACTIVE = SELECT_EXPR_MAJOR_ACTIVE + "." + SELECT_EXPR_REVISIONS;
        SELECT_EXPR_MAJOR_REVISIONS_VERSIONOF = SELECT_EXPR_MAJOR_VERSIONOF + "." + SELECT_EXPR_REVISIONS;
        SELECT_EXPR_MAJOR_REVISION_IDS_ACTIVE = SELECT_EXPR_MAJOR_REVISIONS_ACTIVE + "." + DomainConstants.SELECT_ID;
        SELECT_EXPR_MAJOR_REVISION_IDS_VERSIONOF = SELECT_EXPR_MAJOR_REVISIONS_VERSIONOF + "." + DomainConstants.SELECT_ID;
        SELECT_EXPR_MAJOR_REVISION_MINORS_ACTIVE = SELECT_EXPR_MAJOR_REVISIONS_ACTIVE + "." + SELECT_EXPR_ACTIVE_VERSION + "." + SELECT_EXPR_REVISIONS;
        SELECT_EXPR_MAJOR_REVISION_MINORS_ACTIVE_IDS = SELECT_EXPR_MAJOR_REVISION_MINORS_ACTIVE + "." + DomainConstants.SELECT_ID;

        migrationExpander = new SLWFolderMigrationExpander_mxJPO(context, null);

        initCache();
    }

    public void initCache() {
        expandedIdsCache = new HashSet<String>();
        expandedIdsFamilyTreeNodeCache = new HashMap<String, IEFFamilyRevTreeNode>();
        latestFamilyIdFamilyTreeNodeMap = new HashMap<String, IEFFamilyRevTreeNode>();
        familiesForVersionProcessing = new HashSet<String>();
        familiesProcessedForVersion = new HashSet<String>();
        folderIdFolderDetailsMap = new HashMap<String, List<Set<String>>>();
        busIdLatestBusDetailsMap = new HashMap<String, String[]>();
        duplicateFileNameIds = new HashSet<String>();
        latestFamilyIdRelBusDataMap = new HashMap<String, Set<Map>>();
        lastFamilyIdConnectedFoldersMap = new HashMap<String, StringList[]>();
        propertyKeyValueMap = new HashMap<String, String>();
    }

    public MapList addToFolder(Context context, String[] argv) throws Exception {
        startProfileTask("SLWFolderMigration:addToFolder");

        MapList rootObjectList = new MapList();

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(argv);

            HashMap requestValuesMap = (HashMap) programMap.get("RequestValuesMap");
            String[] objectIds = (String[]) requestValuesMap.get("selectedobjectids");
            String[] folderObjectIds = (String[]) requestValuesMap.get("folderObjectId");
            this.folderObjectId = folderObjectIds[0];

            initUtils(context, requestValuesMap);

            loadFolderDetails(context, folderObjectId);

            Set<String> rootLastFamilyIds = new HashSet<String>();

            StringTokenizer tokenizer = new StringTokenizer(objectIds[0], "|");

            while (tokenizer.hasMoreTokens()) {
                String objectId = (String) tokenizer.nextToken();
                String[] latestFamilyMajorDetails = getLatestMajorDetails(context, objectId);
                String[] documentStatus = getDocumentStatus(latestFamilyMajorDetails[0], latestFamilyMajorDetails[1]);

                Map objectDetails = getObjectDetailsMap(null, latestFamilyMajorDetails[0], documentStatus);
                objectDetails.put("rootnode", "true");

                rootLastFamilyIds.add(latestFamilyMajorDetails[0]);

                rootObjectList.add(objectDetails);
            }

            String[] rootFamilyIds = new String[rootLastFamilyIds.size()];
            rootLastFamilyIds.toArray(rootFamilyIds);

            getConnectedFoldersStatus(context, rootFamilyIds, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        endProfileTask("SLWFolderMigration:addToFolder");

        return rootObjectList;
    }

    private void initUtils(Context context, Map requestValuesMap) throws Exception {
        String language = "en-US";
        Locale locale = (Locale) requestValuesMap.get("localeObj");
        if (locale != null) {
            language = locale.getLanguage();
        }

        serverResourceBundle = new MCADServerResourceBundle(language);
        cache = new IEFGlobalCache();
        _mxUtil = new MCADMxUtil(context, serverResourceBundle, cache);
        _gco = getGlobalConfigObject(context);
    }

    public MapList getRelatedData(Context context, String[] argv) throws Exception {
        startProfileTask("SLWFolderMigration:getRelatedData");
        MapList relBusObjList = new MapList();

        HashMap programMap = (HashMap) JPO.unpackArgs(argv);
        localeLanguage = (String) programMap.get("LocaleLanguage");
        HashMap requestValuesMap = (HashMap) programMap.get("RequestValuesMap");
        String[] folderObjectIds = (String[]) requestValuesMap.get("folderObjectId");
        this.folderObjectId = folderObjectIds[0];
        _generalUtil = new MCADServerGeneralUtil(context, _gco, serverResourceBundle, cache);

        String inputId = (String) programMap.get("objectId");

        loadFolderDetails(context, folderObjectId);

        String[] inputFamilyId = new String[1];
        inputFamilyId[0] = inputId;
        String[] latestFamilyMajorDetails = getLatestMajorDetails(context, inputFamilyId[0]);
        String latestFamilyMajorId = latestFamilyMajorDetails[0];
        String latestFamilyMajorTitle = latestFamilyMajorDetails[1];
        boolean isDrawing = Boolean.valueOf(latestFamilyMajorDetails[2]);

        Map<String, Set<String>> rootFamilyIdInstanceTable = isDrawing ? null : new HashMap<String, Set<String>>();

        startProfileTask("SLWFolderMigration:getRelatedData::get structure for all root revs/vers");

        Map<String, Set<String>> rootFamilyIdAllRevVerIdsMap = getAllRevisionsVersions(context, inputFamilyId, false, rootFamilyIdInstanceTable);
        Set<String> ids = rootFamilyIdAllRevVerIdsMap.get(inputId);
        String[] allVersionFamilyIds = new String[ids.size()];
        ids.toArray(allVersionFamilyIds);

        IEFFamilyRevTreeNode rootFamilyNode = getFamilyRevTreeNode(null, latestFamilyMajorId, latestFamilyMajorTitle);

        for (int i = 0; i < allVersionFamilyIds.length; ++i) {
            if (rootFamilyIdInstanceTable != null) {
                Set<String> instances = rootFamilyIdInstanceTable.get(allVersionFamilyIds[i]);

                if (instances != null) {
                    Iterator<String> instanceIter = instances.iterator();

                    while (instanceIter.hasNext()) {
                        String[] idsToExpand = new String[1];
                        idsToExpand[0] = instanceIter.next();
                        getFamilyStructure(context, idsToExpand[0], rootFamilyNode); // expand instance, add structure to family tree
                    }
                } else {
                    // System.out.println("instanceTable null for: "+ allVersionFamilyIds[i]);
                }
            } else {
                getFamilyStructure(context, allVersionFamilyIds[i], rootFamilyNode); // this is a drawing - so expand itself
            }
        }

        endProfileTask("SLWFolderMigration:getRelatedData::get structure for all root revs/vers");
        startProfileTask("SLWFolderMigration:getRelatedData::get structure for all non-root revs/vers");

        processAllVersions(context, relBusObjList);

        endProfileTask("SLWFolderMigration:getRelatedData::get structure for all non-root revs/vers");

        startProfileTask("SLWFolderMigration:getRelatedData::get connected folders for all families");

        Set<String> allFamilies = latestFamilyIdFamilyTreeNodeMap.keySet();
        String[] allFamilyIds = new String[allFamilies.size()];
        allFamilies.toArray(allFamilyIds);

        getConnectedFoldersStatus(context, allFamilyIds, false);

        endProfileTask("SLWFolderMigration:getRelatedData::get connected folders for all families");

        try {
            List<String> alreadyExpandedNodesInPath = new ArrayList<String>();
            alreadyExpandedNodesInPath.add(rootFamilyNode.getNodeId());

            buildRelBusObjList(rootFamilyNode, relBusObjList, alreadyExpandedNodesInPath);
            alreadyExpandedNodesInPath.remove(rootFamilyNode.getNodeId());

            if (!duplicateFileNameIds.isEmpty()) {
                Iterator<String> iter = duplicateFileNameIds.iterator();

                while (iter.hasNext()) {
                    IEFFamilyRevTreeNode treeNode = latestFamilyIdFamilyTreeNodeMap.get(iter.next());

                    propagateDuplicateFileNameStatus(treeNode);
                }
            }

            // To avoid JPO call for each expansion
            HashMap hmTemp = new HashMap();
            hmTemp.put("expandMultiLevelsJPO", "true");

            relBusObjList.add(hmTemp);
        } catch (Exception ex) {
            System.out.println("\nSLWFolderMigration:getRelatedData : Error " + ex.getMessage());
            ex.printStackTrace();
        }

        endProfileTask("SLWFolderMigration:getRelatedData");

        return relBusObjList;
    }

    private void getConnectedFoldersStatus(Context context, String[] familyIds, boolean updateStatus) throws Exception {
        boolean isPushed = false;

        try {
            ContextUtil.pushContext(context);
            isPushed = true;

            StringList busSelectList = new StringList();
            busSelectList.add("id");
            busSelectList.add("to[" + RELATIONSHIP_VAULTEDOBJECTS + "].from.id");
            busSelectList.add("to[" + RELATIONSHIP_VAULTEDOBJECTS + "].from.name");

            BusinessObjectWithSelectList busWithSelectList = BusinessObjectWithSelect.getSelectBusinessObjectData(context, familyIds, busSelectList);

            for (int i = 0; i < busWithSelectList.size(); i++) {
                BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(i);
                String familyId = busWithSelect.getSelectData("id");

                StringList connectedFolderIds = busWithSelect.getSelectDataList("to[" + RELATIONSHIP_VAULTEDOBJECTS + "].from.id");
                StringList connectedFolderNames = busWithSelect.getSelectDataList("to[" + RELATIONSHIP_VAULTEDOBJECTS + "].from.name");
                StringList[] connectedFolderDetails = new StringList[2];
                connectedFolderDetails[0] = connectedFolderIds;
                connectedFolderDetails[1] = connectedFolderNames;

                if (connectedFolderIds != null && !connectedFolderIds.isEmpty()) {
                    lastFamilyIdConnectedFoldersMap.put(familyId, connectedFolderDetails);

                    if (updateStatus) {
                        Set<Map> relBusData = latestFamilyIdRelBusDataMap.get(familyId);

                        if (relBusData != null) {
                            String status = null;
                            String message = null;

                            if (connectedFolderIds.size() > 1 || !((String) connectedFolderIds.firstElement()).equals(folderObjectId)) {
                                status = "error";
                                message = getPropertyValue("mcadIntegration.Server.Message.RootAlreadyConnectedToFolder", "Error: Root node is already connected to other folder(s) named: ")
                                        + getFolderNamesForMessage(connectedFolderIds, connectedFolderNames)
                                        + getPropertyValue("mcadIntegration.Server.Message.DuplicateConnectionDisallowed", ".Duplicate connection not permitted.");
                            }

                            for (Map objectDetails : relBusData) {
                                if (status != null && message != null) {
                                    objectDetails.put("message", message);
                                    objectDetails.put("status", status);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            if (isPushed) {
                ContextUtil.popContext(context);
            }
        }
    }

    private String getFolderNamesForMessage(StringList connectedFolderIds, StringList connectedFolderNames) {
        String folderNames = null;

        int matchIndex = connectedFolderIds.indexOf(folderObjectId);

        if (matchIndex >= 0) {
            StringList connectedFolderNamesCopy = new StringList();
            connectedFolderNamesCopy.addAll(connectedFolderNames);
            connectedFolderNamesCopy.remove(matchIndex);

            folderNames = MCADUtil.getDelimitedStringFromCollection(connectedFolderNamesCopy, ",");
        } else {
            folderNames = MCADUtil.getDelimitedStringFromCollection(connectedFolderNames, ",");
        }

        return folderNames;
    }

    private void propagateDuplicateFileNameStatus(IEFFamilyRevTreeNode treeNode) {
        if (treeNode != null) {
            Set<String> parentNodeIds = treeNode.getParentNodeIds();

            if (!parentNodeIds.isEmpty()) {
                Iterator<String> iter = parentNodeIds.iterator();

                while (iter.hasNext()) {
                    IEFFamilyRevTreeNode parentTreeNode = latestFamilyIdFamilyTreeNodeMap.get(iter.next());

                    Set<Map> relBusDataList = latestFamilyIdRelBusDataMap.get(parentTreeNode.getNodeId());

                    if (relBusDataList != null) {
                        Iterator<Map> iter1 = relBusDataList.iterator();
                        while (iter1.hasNext()) {
                            Map objectDetails = iter1.next();
                            objectDetails.put("disableSelection", "true");
                            objectDetails.put("message", getPropertyValue("mcadIntegration.Server.Message.ErrorReferenceDuplicateFile", ERROR_REFERENCE_DUPLICATE_FILE));
                            objectDetails.put("error", "true");
                        }

                        propagateDuplicateFileNameStatus(parentTreeNode);
                    }
                }
            }
        }
    }

    private void loadFolderDetails(Context context, String folderId) throws Exception {
        if (folderIdFolderDetailsMap == null) {
            folderIdFolderDetailsMap = new HashMap<String, List<Set<String>>>();
        }

        if (folderIdFolderDetailsMap.get(folderId) == null) {
            List<Set<String>> folderDetails = new ArrayList<Set<String>>();
            Set<String> connectedDocumentIds = new HashSet<String>();
            Set<String> connectedDocumentTitles = new HashSet<String>();
            folderDetails.add(connectedDocumentIds);
            folderDetails.add(connectedDocumentTitles);
            folderIdFolderDetailsMap.put(folderId, folderDetails);

            BusinessObject folderObject = new BusinessObject(folderId);
            folderObject.open(context);
            StringList relSelects = new StringList();
            relSelects.add("to.id");
            relSelects.add("to.attribute[Title]");

            ExpansionIterator iterator = null;
            try {
                iterator = folderObject.getExpansionIterator(context, RELATIONSHIP_VAULTEDOBJECTS, getRelevantTypes(), new StringList(), relSelects, false, true, (short) 1, "", "", (short) 1000,
                        false, true, (short) 1000, false);

                while (iterator.hasNext()) {
                    RelationshipWithSelect relWithSelect = iterator.next();
                    String documentId = relWithSelect.getSelectData("to.id");
                    String documentTitle = relWithSelect.getSelectData("to.attribute[Title]");

                    connectedDocumentIds.add(documentId);
                    connectedDocumentTitles.add(documentTitle);
                }
            } finally {
                iterator.close();
            }

            folderObject.close(context);
        }
    }

    private String[] getDocumentStatus(String documentId, String documentTitle) {
        String[] documentStatus = new String[2];
        String status = "selectable";
        String message = "OK";

        if (documentId == null) {
            status = "error";
            message = getPropertyValue("mcadIntegration.Server.Message.MissingFamily", "Error:Missing family object");
        } else {
            List<Set<String>> folderDetails = folderIdFolderDetailsMap.get(folderObjectId);
            Set<String> connectedObjIds = folderDetails.get(0);
            Set<String> connectedObjTitles = folderDetails.get(1);

            if (connectedObjIds.contains(documentId)) {
                status = "unselectable";
                message = getPropertyValue("mcadIntegration.Server.Message.AlreadyPresentInThisFolder", "Already present in this folder");
                // status = "true";
            } else if (connectedObjTitles.contains(documentTitle)) // TBD: would evaluate to true even when some other revision/version is connected
            {
                status = "error";
                message = getPropertyValue("mcadIntegration.Server.Message.ErrorDuplicateFile", ERROR_DUPLICATE_FILE);
                // status = "false";
                duplicateFileNameIds.add(documentId);
            }
        }

        documentStatus[0] = status;
        documentStatus[1] = message;

        return documentStatus;
    }

    private Map getObjectDetailsMap(String level, String id, String[] documentStatus) {
        Map objectDetails = new HashMap<String, String>();

        objectDetails.put("id", id);
        objectDetails.put("expandLevel", "all");

        if (!documentStatus[0].equals("error")) {
            updateStatusForFolderConnection(id, documentStatus);
        }

        if (documentStatus[0].equals("false")) {
            objectDetails.put("disableSelection", "true");
        }

        objectDetails.put("message", documentStatus[1]);
        objectDetails.put("status", documentStatus[0]);

        if (level != null) {
            objectDetails.put("level", level);
        }

        Set<Map> relBusData = latestFamilyIdRelBusDataMap.get(id);

        if (relBusData == null) {
            relBusData = new HashSet<Map>();
            latestFamilyIdRelBusDataMap.put(id, relBusData);
        }

        relBusData.add(objectDetails);

        return objectDetails;
    }

    private void updateStatusForFolderConnection(String lastFamilyId, String[] documentStatus) {
        if (lastFamilyIdConnectedFoldersMap.containsKey(lastFamilyId)) {
            StringList[] connectedFolderDetails = lastFamilyIdConnectedFoldersMap.get(lastFamilyId);
            StringList connectedFolderIds = connectedFolderDetails[0];
            StringList connectedFolderNames = connectedFolderDetails[1];

            if (connectedFolderIds.size() > 1 || !((String) connectedFolderIds.firstElement()).equals(folderObjectId)) {
                documentStatus[0] = "unselectable";
                documentStatus[1] = getPropertyValue("mcadIntegration.Server.Message.AlreadyConnectedToFolder", "Already connected to other folder(s) named:")
                        + getFolderNamesForMessage(connectedFolderIds, connectedFolderNames) + getPropertyValue("mcadIntegration.Server.Message.WillBeSkipped", ". Will be skipped.");
            }
        }
    }

    private String getRelevantTypes() throws MCADException {
        Vector relevantENOVIATypes = new Vector();
        Vector familyCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_FAMILY_LIKE);
        for (int i = 0; i < familyCADTypes.size(); ++i) {
            relevantENOVIATypes.addAll(getMappedBusTypesForCADType((String) familyCADTypes.elementAt(i)));
        }

        Vector drawingCADTypes = MCADUtil.getVectorFromString(DRAWING_LIKE_CADTYPES, ",");

        for (int i = 0; i < drawingCADTypes.size(); ++i) {
            relevantENOVIATypes.addAll(getMappedBusTypesForCADType((String) drawingCADTypes.elementAt(i)));
        }

        return MCADUtil.getDelimitedStringFromCollection(relevantENOVIATypes, ",");
    }

    private void buildRelBusObjList(IEFFamilyRevTreeNode treeNode, MapList relBusObjList, List<String> alreadyExpandedNodesInPath) {
        /*
         * Map rootObjectDetails = getObjectDetailsMap("0", treeNode.getNodeId(), treeNode.getStatus()); relBusObjList.add(rootObjectDetails);
         */

        String level = String.valueOf(alreadyExpandedNodesInPath.size());
        List<IEFFamilyRevTreeNode> childNodes = treeNode.getChildNodes();

        for (int i = 0; i < childNodes.size(); ++i) {
            IEFFamilyRevTreeNode childNode = childNodes.get(i);
            String childFamilyId = childNode.getNodeId();
            /*
             * Map newMap = new HashMap(5); newMap.put("level", level); newMap.put("id", childFamilyId); newMap.put("expandLevel", "all");
             * 
             * String message = childNode.getMessage();
             * 
             * if(message != null) { newMap.put("message", message); } else { newMap.put("message", "OK"); }
             */

            String[] documentStatus = childNode.getStatus();

            Map objectDetails = getObjectDetailsMap(level, childFamilyId, documentStatus);

            relBusObjList.add(objectDetails);

            if (!alreadyExpandedNodesInPath.contains(childFamilyId)) {
                alreadyExpandedNodesInPath.add(childFamilyId);

                buildRelBusObjList(childNode, relBusObjList, alreadyExpandedNodesInPath);

                alreadyExpandedNodesInPath.remove(childFamilyId);
            }
        }
    }

    private void processAllVersions(Context context, MapList rootRelBusObjList) throws Exception {
        if (!familiesForVersionProcessing.isEmpty()) {
            String[] ids = new String[familiesForVersionProcessing.size()];
            familiesForVersionProcessing.toArray(ids);
            Map<String, Set<String>> familyIdInstanceTable = new HashMap<String, Set<String>>();

            startProfileTask("SLWFolderMigration:processAllVersions::get all non-root revs/vers");
            Map<String, Set<String>> idAllRevisionVersionIdsMap = getAllRevisionsVersions(context, ids, true, familyIdInstanceTable);
            endProfileTask("SLWFolderMigration:processAllVersions::get all non-root revs/vers");

            Iterator<String> majorIdsIter = idAllRevisionVersionIdsMap.keySet().iterator();

            while (majorIdsIter.hasNext()) {
                String familyMajorId = majorIdsIter.next();

                familiesForVersionProcessing.remove(familyMajorId);
                familiesProcessedForVersion.add(familyMajorId);

                IEFFamilyRevTreeNode treeNode = latestFamilyIdFamilyTreeNodeMap.get(familyMajorId);
                Set<String> allRevisionVersionFamilyIds = idAllRevisionVersionIdsMap.get(familyMajorId);
                Iterator<String> allFamilyVersIter = allRevisionVersionFamilyIds.iterator();

                while (allFamilyVersIter.hasNext()) {
                    String familyId = allFamilyVersIter.next();
                    Set<String> instanceIds = familyIdInstanceTable.get(familyId);

                    if (instanceIds != null) {
                        Iterator<String> instanceIter = instanceIds.iterator();

                        while (instanceIter.hasNext()) {
                            String[] idsToExpand = new String[1];
                            idsToExpand[0] = instanceIter.next();

                            getFamilyStructure(context, idsToExpand[0], treeNode);
                        }
                    }
                }
            }

            processAllVersions(context, rootRelBusObjList); // process any discovered families
        }
    }

    private void getFamilyStructure(Context context, String objectId, IEFFamilyRevTreeNode familyTreeNode) throws Exception {
        if (!expandedIdsCache.contains(objectId)) {
            expandedIdsCache.add(objectId);

            String[] objectIds = new String[1];
            objectIds[0] = objectId;

            migrationExpander.expandInputObject(context, objectIds, expandedIdsCache);

            ArrayList alreadyExpandedNodesInPath = new ArrayList();
            alreadyExpandedNodesInPath.add(objectId);

            buildObjectStructure(context, objectId, alreadyExpandedNodesInPath, familyTreeNode);

            alreadyExpandedNodesInPath.remove(objectId);
        }
    }

    private void addForAllVersionsProcessing(String lastFamilyMajorId) {
        if (!familiesProcessedForVersion.contains(lastFamilyMajorId)) {
            familiesForVersionProcessing.add(lastFamilyMajorId);
        }
    }

    private Map getAllRevisionsVersions(Context context, String[] oids, boolean inputIdsMajorOnly, Map<String, Set<String>> familyIdInstanceTable) throws MatrixException {
        Map<String, Set<String>> idAllRevisionVersionIdsMap = new HashMap<String, Set<String>>();

        boolean getInstances = (familyIdInstanceTable != null) ? true : false;

        StringList selects = new StringList();
        selects.add(DomainConstants.SELECT_ID);
        selects.add(DomainConstants.SELECT_TYPE);
        selects.add("revisions"); // All revisions, from a major
        selects.add("revisions.id");

        if (getInstances) {
            selects.add("revisions.from[" + RELATIONSHIP_INSTANCEOF + "].to.id"); // all revision instances, from a major
            selects.add("revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions.from[" + RELATIONSHIP_INSTANCEOF + "].to.id"); // all instances of all versions of all revisions
        }

        selects.add("revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions"); // All versions of all revisions, from a major
        selects.add("revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions.id");

        if (!inputIdsMajorOnly) {
            selects.add("from[" + RELATIONSHIP_VERSIONOF + "].to.revisions"); // All revisions, from a minor
            selects.add("from[" + RELATIONSHIP_VERSIONOF + "].to.revisions.id");

            selects.add("from[" + RELATIONSHIP_VERSIONOF + "].to.revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions"); // All versions of *all* revisions, from a minor
            selects.add("from[" + RELATIONSHIP_VERSIONOF + "].to.revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions.id");

            // to handle bulk-loaded data that is missing VersionOf relationship
            selects.add("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.revisions");// All revisions, from a bulk-loaded minor
            selects.add("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.revisions.id");

            selects.add("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions"); // All versions of *all* revisions, from a bulk-loaded minor
            selects.add("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions.id");

            if (getInstances) {
                selects.add("from[" + RELATIONSHIP_VERSIONOF + "].to.revisions.from[" + RELATIONSHIP_INSTANCEOF + "].to.id"); // All instances connected to all major revisions
                selects.add("from[" + RELATIONSHIP_VERSIONOF + "].to.revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions.from[" + RELATIONSHIP_INSTANCEOF + "].to.id"); // All instances
                                                                                                                                                                                     // connected to all
                                                                                                                                                                                     // minors

                // from bulk-loaded minor
                selects.add("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.revisions.from[" + RELATIONSHIP_INSTANCEOF + "].to.id"); // All instances connected to all major revisions
                selects.add("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.revisions.from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions.from[" + RELATIONSHIP_INSTANCEOF + "].to.id"); // All
                                                                                                                                                                                          // instances
                                                                                                                                                                                          // connected
                                                                                                                                                                                          // to all
                                                                                                                                                                                          // minors

            }
        }

        BusinessObjectWithSelectList busWithSelectList = BusinessObjectWithSelect.getSelectBusinessObjectData(context, oids, selects);

        for (int i = 0; i < busWithSelectList.size(); ++i) {
            BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) busWithSelectList.elementAt(i);
            String id = busObjectWithSelect.getSelectData(DomainConstants.SELECT_ID);
            String type = busObjectWithSelect.getSelectData(DomainConstants.SELECT_TYPE);

            Set<String> allRevisionVersionIds = idAllRevisionVersionIdsMap.get(id);
            if (allRevisionVersionIds == null) {
                allRevisionVersionIds = new HashSet<String>();
                idAllRevisionVersionIdsMap.put(id, allRevisionVersionIds);
            }

            StringList majorRevisions = null;
            boolean isMajor = false;

            if (_gco.isMajorType(type)) {
                majorRevisions = busObjectWithSelect.getSelectDataList("revisions");
                isMajor = true;
            } else {
                majorRevisions = busObjectWithSelect.getSelectDataList("from[" + RELATIONSHIP_VERSIONOF + "].to.revisions");

                if (majorRevisions == null || majorRevisions.isEmpty()) {
                    majorRevisions = busObjectWithSelect.getSelectDataList("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.revisions");
                }
            }

            // System.out.println("majorRevisions: " + majorRevisions);

            StringBuilder revSelectBuf = new StringBuilder();
            for (int j = 0; j < majorRevisions.size(); ++j) {
                revSelectBuf.setLength(0);
                String majorRevSelect = null;
                String majorRevisionId = null;

                if (isMajor) {
                    majorRevSelect = revSelectBuf.append("revisions[").append(majorRevisions.elementAt(j)).append("].").toString();
                    majorRevisionId = (String) busObjectWithSelect.getSelectData(new StringBuilder(majorRevSelect).append(DomainConstants.SELECT_ID).toString());
                } else {
                    // majorRevSelect = revSelectBuf.append(SELECT_EXPR_MAJOR_VERSIONOF).append(".revisions[").append(majorRevisions.elementAt(j)).append("].").toString();
                    majorRevSelect = revSelectBuf.append("from[" + RELATIONSHIP_VERSIONOF + "].to").append(".revisions[").append(majorRevisions.elementAt(j)).append("].").toString();
                    majorRevisionId = (String) busObjectWithSelect.getSelectData(new StringBuilder(majorRevSelect).append(DomainConstants.SELECT_ID).toString());

                    if (majorRevisionId == null || majorRevisionId.equals("")) {
                        majorRevSelect = revSelectBuf.append("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from").append(".revisions[").append(majorRevisions.elementAt(j)).append("].").toString();
                    }
                }

                allRevisionVersionIds.add(majorRevisionId);

                if (getInstances) {
                    StringList instanceIds = (StringList) busObjectWithSelect
                            .getSelectDataList(new StringBuilder(majorRevSelect).append("from[").append(RELATIONSHIP_INSTANCEOF).append("].to.id").toString());

                    if (instanceIds != null) {
                        Set<String> instances = new HashSet<String>(instanceIds.size());
                        instances.addAll(instanceIds);

                        familyIdInstanceTable.put(majorRevisionId, instances);
                    }
                }

                // if(isMajor)
                // {
                StringList minorRevisions = (StringList) busObjectWithSelect
                        .getSelectDataList(new StringBuilder(majorRevSelect).append("from[" + RELATIONSHIP_ACTIVE_VERSION + "].to.revisions").toString());
                // }
                // else if(isActiveMinor)
                // {
                // minorRevisions = (StringList)busObjectWithSelect.getSelectDataList(new StringBuilder(majorRevSelect).append(SELECT_EXPR_ACTIVE_VERSION).append(".revisions").toString());
                // }

                // System.out.println("minorRevisions: " + minorRevisions);
                if (minorRevisions != null && minorRevisions.size() > 0) {
                    StringBuilder minorRevSelectBuf = new StringBuilder();
                    for (int k = 0; k < minorRevisions.size(); ++k) {
                        minorRevSelectBuf.setLength(0);
                        String minorRevSelect = minorRevSelectBuf.append(majorRevSelect).append("from[" + RELATIONSHIP_ACTIVE_VERSION + "].to").append(".revisions[")
                                .append(minorRevisions.elementAt(k)).append("].").toString();
                        String minorId = (String) busObjectWithSelect.getSelectData(new StringBuilder(minorRevSelect).append(DomainConstants.SELECT_ID).toString());

                        if (getInstances) {
                            StringList instanceIds = (StringList) busObjectWithSelect
                                    .getSelectDataList(new StringBuilder(minorRevSelect).append("from[").append(RELATIONSHIP_INSTANCEOF).append("].to.id").toString());

                            if (instanceIds != null) {
                                Set<String> instances = new HashSet<String>(instanceIds.size());
                                instances.addAll(instanceIds);

                                familyIdInstanceTable.put(minorId, instances);
                            }
                        }

                        allRevisionVersionIds.add(minorId);

                        // System.out.println("minorId: " + minorId);
                    }
                }

                // System.out.println("revisionId: " + majorRevisionId);
            }
        }

        return idAllRevisionVersionIdsMap;
    }

    public String[] getLatestMajorDetails(Context context, String objectID) throws Exception {
        String[] latestMajorDetails = busIdLatestBusDetailsMap.get(objectID);

        if (latestMajorDetails == null) {
            latestMajorDetails = new String[3];

            StringList selectList = new StringList();
            selectList.add("last.id");
            selectList.add("last.attribute[Title]");
            selectList.add("type");
            selectList.add("type.kindof[" + TYPE_CADDRAWING + "]");
            selectList.add("from[" + RELATIONSHIP_VERSIONOF + "].to.last.id");
            selectList.add("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.last.id");
            selectList.add("from[" + RELATIONSHIP_VERSIONOF + "].to.last.attribute[Title]");
            selectList.add("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.last.attribute[Title]");

            BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, new String[] { objectID }, selectList);
            BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(0);

            String type = busWithSelect.getSelectData("type");

            if (_gco.isMajorType(type)) {
                latestMajorDetails[0] = busWithSelect.getSelectData("last.id");
                latestMajorDetails[1] = busWithSelect.getSelectData("last.attribute[Title]");
            } else {
                latestMajorDetails[0] = busWithSelect.getSelectData("from[" + RELATIONSHIP_VERSIONOF + "].to.last.id");
                latestMajorDetails[1] = busWithSelect.getSelectData("from[" + RELATIONSHIP_VERSIONOF + "].to.last.attribute[Title]");

                if (latestMajorDetails[0] == null) {
                    latestMajorDetails[0] = busWithSelect.getSelectData("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.last.id");
                    latestMajorDetails[1] = busWithSelect.getSelectData("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.last.attribute[Title]");
                }
            }

            latestMajorDetails[2] = busWithSelect.getSelectData("type.kindof[" + TYPE_CADDRAWING + "]");

            busIdLatestBusDetailsMap.put(objectID, latestMajorDetails);
        }

        return latestMajorDetails;
    }

    protected void buildObjectStructure(Context context, String inputObjectID, ArrayList alreadyExpandedNodesInPath, IEFFamilyRevTreeNode revTreeNode) throws Exception {
        HashMap relIdChildDataMap = migrationExpander.getRelidChildBusIdList(inputObjectID);

        if (relIdChildDataMap == null)
            return;

        Iterator relids = relIdChildDataMap.keySet().iterator();

        while (relids.hasNext()) {
            String relid = (String) relids.next();

            if (relid == null || relid.equals(""))
                continue;

            String[] childData = (String[]) relIdChildDataMap.get(relid);
            String childID = childData[0];

            RelationshipWithSelect relWithSelect = migrationExpander.getRelationshipWithSelect(relid);
            BusinessObjectWithSelect busWithSelect = migrationExpander.getBusinessObjectWithSelect(childID);

            /*
             * if(busWithSelect == null) { System.out.println("This should NOT print........................." + childID);
             * 
             * //Only as backup. Ideally, this should not run, it will hit performance.
             * 
             * StringList busSelectList = new StringList(); busSelectList.add("type"); busSelectList.add("name"); busSelectList.add("to[" + RELATIONSHIP_INSTANCEOF + "].from.from[" +
             * RELATIONSHIP_VERSIONOF + "].to.last.id"); //assumption: bulk-loaded data would use major in structure. busSelectList.add("to[" + RELATIONSHIP_INSTANCEOF + "].from.last.id");
             * 
             * busWithSelect = BusinessObject.getSelectBusinessObjectData(context, new String[]{childID}, busSelectList).getElement(0); }
             */

            String relName = (String) relWithSelect.getSelectData("name");

            StringList familyMajorIds = (StringList) busWithSelect.getSelectDataList("to[" + RELATIONSHIP_INSTANCEOF + "].from.from[" + RELATIONSHIP_VERSIONOF + "].to.last.id");
            StringList familyMajorTitles = (StringList) busWithSelect.getSelectDataList("to[" + RELATIONSHIP_INSTANCEOF + "].from.from[" + RELATIONSHIP_VERSIONOF + "].to.last.attribute[Title]");

            if (familyMajorIds == null || familyMajorIds.isEmpty()) {
                familyMajorIds = (StringList) busWithSelect.getSelectDataList("to[" + RELATIONSHIP_INSTANCEOF + "].from.last.id");

                if (familyMajorIds == null || familyMajorIds.isEmpty()) {
                    familyMajorIds = (StringList) busWithSelect
                            .getSelectDataList("to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.to[" + RELATIONSHIP_INSTANCEOF + "].from.to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.last.id");
                }
            }

            if (familyMajorTitles == null || familyMajorTitles.isEmpty()) {
                familyMajorTitles = (StringList) busWithSelect.getSelectDataList("to[" + RELATIONSHIP_INSTANCEOF + "].from.last.attribute[Title]");

                if (familyMajorTitles == null || familyMajorTitles.isEmpty()) {
                    familyMajorTitles = (StringList) busWithSelect.getSelectDataList(
                            "to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.to[" + RELATIONSHIP_INSTANCEOF + "].from.to[" + RELATIONSHIP_ACTIVE_VERSION + "].from.last.attribute[Title]");
                }
            }

            String lastFamilyMajorId = (familyMajorIds == null || familyMajorIds.isEmpty()) ? null : (String) familyMajorIds.elementAt(0);
            String lastFamilyMajorTitle = (familyMajorTitles == null || familyMajorTitles.isEmpty()) ? null : (String) familyMajorTitles.elementAt(0);

            // System.out.println("familyMajorId............" + familyMajorIds);
            // System.out.println("lastFamilyMajorTitle............" + lastFamilyMajorTitle);

            IEFFamilyRevTreeNode childRevTreeNode = getFamilyRevTreeNode(childID, lastFamilyMajorId, lastFamilyMajorTitle);
            revTreeNode.addChild(childRevTreeNode);

            if (lastFamilyMajorId != null) {
                addForAllVersionsProcessing(lastFamilyMajorId);

                String level = String.valueOf(alreadyExpandedNodesInPath.size());

                if (_gco.isRelationshipOfClass(relName, MCADServerSettings.CIRCULAR_EXTERNAL_REFERENCE_LIKE) || (!validateRelationship(context, relid)))
                    continue;

                if (!alreadyExpandedNodesInPath.contains(childID)) {
                    alreadyExpandedNodesInPath.add(childID);
                    buildObjectStructure(context, childID, alreadyExpandedNodesInPath, childRevTreeNode);
                    alreadyExpandedNodesInPath.remove(childID);
                }
            }
        }
    }

    protected StringList getRelSelectList(Hashtable relsAndEnds) {
        StringList relSelectionList = new StringList();

        relSelectionList.addElement("id");
        relSelectionList.addElement("name");

        return relSelectionList;
    }

    public String getTypesForTypeChooser(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        _gco = (MCADGlobalConfigObject) programMap.get("GCO");

        Vector familyLikeCADTypes = _gco.getTypeListForClass(MCADAppletServletProtocol.TYPE_FAMILY_LIKE);
        Set<String> types = new HashSet<String>();

        for (int i = 0; i < familyLikeCADTypes.size(); ++i) {
            String cadType = (String) familyLikeCADTypes.elementAt(i);
            Vector enoviaTypes = getMappedBusTypesForCADType(cadType);
            types.addAll(enoviaTypes);
        }

        Vector drawingCADTypes = MCADUtil.getVectorFromString(DRAWING_LIKE_CADTYPES, ",");

        for (int i = 0; i < drawingCADTypes.size(); ++i) {
            String drawingCADType = (String) drawingCADTypes.elementAt(i);
            Vector drawingEnoviaTypes = getMappedBusTypesForCADType(drawingCADType);
            types.addAll(drawingEnoviaTypes);
        }

        String typesForChooser = MCADUtil.getDelimitedStringFromCollection(types, ",");

        return typesForChooser;
    }

    private Vector getMappedBusTypesForCADType(String cadType) throws MCADException {
        Vector rawMappedTypes = _gco.getMappedBusTypes(cadType);

        if (rawMappedTypes == null) {
            String errorMessage = _res.getString("mcadIntegration.Server.Message.ProblemsWithGlobalConfigObject");
            MCADServerException.createException(errorMessage, null);
        }

        Vector mappedBusTypes = MCADUtil.getListOfActualTypes(rawMappedTypes);

        return mappedBusTypes;
    }

    protected boolean validateRelationship(Context _context, String relid) {
        boolean isMustInStructure = true;
        return isMustInStructure;

    }

    private MCADGlobalConfigObject getGlobalConfigObject(Context context) throws Exception {
        if (_gco == null) {
            MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader();
            _gco = configLoader.createGlobalConfigObject(context, _mxUtil, SLW_GCO_TYPE, SLW_GCO_NAME);
        }

        return _gco;
    }

    private IEFFamilyRevTreeNode getFamilyRevTreeNode(String objectId, String latestFamilyId, String title) {
        IEFFamilyRevTreeNode familyRevTreeNode = null;
        if (latestFamilyId != null) {
            familyRevTreeNode = latestFamilyIdFamilyTreeNodeMap.get(latestFamilyId);

            if (familyRevTreeNode == null) {
                familyRevTreeNode = new IEFFamilyRevTreeNode(latestFamilyId);
                familyRevTreeNode.setTitle(title);

                String[] status = getDocumentStatus(latestFamilyId, title);
                familyRevTreeNode.setStatus(status);

                latestFamilyIdFamilyTreeNodeMap.put(latestFamilyId, familyRevTreeNode);
            }
        } else {
            familyRevTreeNode = new IEFFamilyRevTreeNode(objectId);
            String[] status = getDocumentStatus(latestFamilyId, title);
            familyRevTreeNode.setStatus(status);
            familyRevTreeNode.setTitle("");
            latestFamilyIdFamilyTreeNodeMap.put(objectId, familyRevTreeNode);
        }

        if (objectId != null && !expandedIdsFamilyTreeNodeCache.containsKey(objectId)) // objectId can be null only for root node
        {
            expandedIdsFamilyTreeNodeCache.put(objectId, familyRevTreeNode);
        }

        return familyRevTreeNode;
    }

    public class IEFFamilyRevTreeNode {
        private String lastFamilyMajorId = null;

        private String lastFamilyMajorTitle = null;

        private List<IEFFamilyRevTreeNode> childNodes = null;

        private Set<String> childNodeIds = null;

        private Set<String> parentNodeIds = null;

        private String message = null;

        private boolean isSelectable = true;

        private String[] status = null;

        public IEFFamilyRevTreeNode(String lastFamilyId) {
            this.lastFamilyMajorId = lastFamilyId;
            this.childNodeIds = new HashSet<String>();
            this.childNodes = new ArrayList<IEFFamilyRevTreeNode>();
            this.parentNodeIds = new HashSet<String>();
            this.message = "OK";
        }

        public String getLastFamilyMajorId() {
            return lastFamilyMajorId;
        }

        public void setLastFamilyMajorId(String lastFamilyMajorId) {
            this.lastFamilyMajorId = lastFamilyMajorId;
        }

        public String getTitle() {
            return lastFamilyMajorTitle;
        }

        public void setTitle(String lastFamilyMajorTitle) {
            this.lastFamilyMajorTitle = lastFamilyMajorTitle;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setStatus(String[] status) {
            this.status = status;
        }

        public boolean isSelectable() {
            return isSelectable;
        }

        public void setSelectable(boolean isSelectable) {
            this.isSelectable = isSelectable;
        }

        public String getNodeId() {
            return lastFamilyMajorId;
        }

        public void addChild(IEFFamilyRevTreeNode inputNode) {
            String inputNodeId = inputNode.getNodeId();
            if (!childNodeIds.contains(inputNodeId)) {
                childNodeIds.add(inputNodeId);
                childNodes.add(inputNode);

                inputNode.addParent(this);
            }
        }

        public List<IEFFamilyRevTreeNode> getChildNodes() {
            return childNodes;
        }

        public void addParent(IEFFamilyRevTreeNode parentNode) {
            if (parentNode != null && parentNode.getNodeId() != null) {
                this.parentNodeIds.add(parentNode.getNodeId());
            }
        }

        public String[] getStatus() {
            return this.status;
        }

        public Set getParentNodeIds() {
            return this.parentNodeIds;
        }
    }

    public Object showStatus(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            String status = (String) objDetails.get("message") == null ? "OK" : (String) objDetails.get("message");
            String isRoot = (String) objDetails.get("rootnode");

            if (isRoot != null && isRoot.equals("true")) {
                MapList children = (MapList) objDetails.get("children");

                if (children != null) {
                    Iterator iter = children.iterator();

                    while (iter.hasNext()) {
                        Map childMap = (Map) iter.next();
                        String level = (String) childMap.get("level");

                        if (level.equals("1")) {
                            String message = (String) childMap.get("message");
                            if (message.equals(ERROR_DUPLICATE_FILE) || message.equals(getPropertyValue("mcadIntegration.Server.Message.ErrorDuplicateFile", ERROR_DUPLICATE_FILE))
                                    || message.equals(ERROR_REFERENCE_DUPLICATE_FILE)
                                    || message.equals(getPropertyValue("mcadIntegration.Server.Message.ErrorReferenceDuplicateFile", ERROR_REFERENCE_DUPLICATE_FILE))) {
                                status = getPropertyValue("mcadIntegration.Server.Message.ErrorReferenceDuplicateFile", ERROR_REFERENCE_DUPLICATE_FILE);
                                break;
                            }
                        }
                    }
                }
            }

            columnCellContentList.add(status);
            // System.out.println("objDetails.........................." + objDetails);
        }

        return columnCellContentList;
    }

    public Object setSelection(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            String isChecked = (String) objDetails.get("disableSelection") == null ? "true" : "false";
            String isRoot = (String) objDetails.get("rootnode");

            if (isRoot != null && isRoot.equals("true") && !isChecked.equals("false")) {
                // System.out.println("root objDetails.........................." + objDetails);

                MapList children = (MapList) objDetails.get("children");

                if (children != null) {
                    Iterator iter = children.iterator();

                    while (iter.hasNext()) {
                        Map childMap = (Map) iter.next();
                        String level = (String) childMap.get("level");

                        if (level.equals("1")) {
                            String message = (String) childMap.get("message");
                            if (message.equals(ERROR_DUPLICATE_FILE) || message.equals(getPropertyValue("mcadIntegration.Server.Message.ErrorDuplicateFile", ERROR_DUPLICATE_FILE))
                                    || message.equals(ERROR_REFERENCE_DUPLICATE_FILE)
                                    || message.equals(getPropertyValue("mcadIntegration.Server.Message.ErrorReferenceDuplicateFile", ERROR_REFERENCE_DUPLICATE_FILE))) {
                                isChecked = "false";
                                break;
                            }
                        }
                    }
                }
            }

            columnCellContentList.add(isChecked);
            // System.out.println("objDetails.........................." + objDetails);
        }

        return columnCellContentList;
    }

    private void startProfileTask(String taskname) {
        if (PROFILER_ENABLED) {
            MCADProfiler.taskStart(taskname);
        }
    }

    private void endProfileTask(String taskname) {
        if (PROFILER_ENABLED) {
            MCADProfiler.taskEnd(taskname);
            MCADProfiler.printProfileData();
        }
    }

    private String getPropertyValue(String propertyKey, String defaultValue) {
        String value = propertyKeyValueMap.get(propertyKey);

        if (value == null) {
            value = serverResourceBundle.getString(propertyKey);

            if (value == null || value.isEmpty() || value.equalsIgnoreCase(propertyKey))
                value = defaultValue;

            propertyKeyValueMap.put(propertyKey, value);
        }

        return value;
    }

}
