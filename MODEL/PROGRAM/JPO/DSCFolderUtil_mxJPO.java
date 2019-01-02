
/*
 ** DSCFolderUtil
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Class defining basic infrastructure, contains common data members required for executing any IEF related actions.
 */

import java.util.HashMap;
import java.util.Map;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.SetItr;
import matrix.db.SetList;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;

public class DSCFolderUtil_mxJPO {
    private String localeLanguage = null;

    private static String REL_VAULTED_DOCUMENTS_REV2 = "Vaulted Documents Rev2";

    private static String TYPE_WORKSPACE = "Workspace";

    private static String TYPE_PROJECT = "Project";

    public DSCFolderUtil_mxJPO() {
    }

    public DSCFolderUtil_mxJPO(Context context, String[] args) throws Exception {
        if (!context.isConnected()) {
            MCADServerException.createException("not supported no desktop client", null);
        }

    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public MapList getAssignedCollections(Context context, String objectId) throws FrameworkException, Exception {
        // Get the list of sets(collections)
        SetList setList = matrix.db.Set.getSets(context);

        SetItr setItr = new SetItr(setList);

        MapList collectionList = new MapList(5);

        // Loop thru the sets(collections)
        int index = 0;
        while (setItr.next()) {
            matrix.db.Set set = setItr.obj();
            if (set.getName().startsWith("."))
                continue;
            HashMap map = new HashMap();
            // count the set elements
            set.open(context);
            BusinessObjectList busList = set.getBusinessObjects(context);
            // checks for the business object in the collection
            if (null != busList) {
                for (int i = 0; i < busList.size(); i++) {
                    BusinessObject bus = (BusinessObject) busList.get(i);
                    String busId = bus.getObjectId();
                    // add the collection name if object exists in the collection
                    if (null != bus && null != busId && busId.equals(objectId)) {
                        map.put(DomainObject.SELECT_TYPE, "Collection");
                        map.put(DomainObject.SELECT_ID, objectId);
                        map.put(DomainObject.SELECT_NAME, set.getName());
                        collectionList.add(map);
                        break;
                    }
                }
            }
            set.close(context);

            index++;
        }
        return collectionList;
    }

    public String projectFolderToPath(Context context, String folderId

    ) throws Exception {
        StringBuffer folderPath = new StringBuffer();
        try {
            StringList busSelects = new StringList(5);
            busSelects.add(DomainObject.SELECT_NAME);
            busSelects.add(DomainObject.SELECT_ID);
            Pattern wsRelPattern = new Pattern(DomainObject.RELATIONSHIP_WORKSPACE_VAULTS);
            wsRelPattern.addPattern(DomainObject.RELATIONSHIP_SUB_VAULTS);
            wsRelPattern.addPattern(REL_VAULTED_DOCUMENTS_REV2);
            Pattern typePattern = new Pattern(DomainObject.TYPE_WORKSPACE_VAULT);
            typePattern.addPattern(DomainObject.TYPE_PROJECT_SPACE);
            DomainObject domObj = DomainObject.newInstance(context, folderId);

            MapList folderList = domObj.getRelatedObjects(context, wsRelPattern.getPattern(), typePattern.getPattern(), busSelects, null, true, false, (short) 0, null, null);
            // folderList.sortStructure("level", "ascending", "String");
            if (folderList != null && folderList.size() > 0) {
                for (int k = 0; k < folderList.size(); k++) {
                    folderPath.insert(0, (String) ((Map) folderList.get(k)).get(DomainObject.SELECT_NAME) + "/");
                }
            }
            BusinessObject folder = new BusinessObject(folderId);
            folder.open(context);
            folderPath.append(folder.getName());
            folder.close(context);
            return folderPath.toString();
        } catch (Exception e) {
            System.out.println(e.toString());
            return folderPath.toString();
        }
    }

    public String workspaceFolderToPath(Context context, String folderId

    ) throws Exception {
        StringBuffer folderPath = new StringBuffer();
        try {
            StringList busSelects = new StringList(5);
            busSelects.add(DomainObject.SELECT_NAME);
            busSelects.add(DomainObject.SELECT_ID);
            Pattern wsRelPattern = new Pattern(DomainObject.RELATIONSHIP_WORKSPACE_VAULTS);
            wsRelPattern.addPattern(DomainObject.RELATIONSHIP_SUB_VAULTS);
            wsRelPattern.addPattern(DomainObject.RELATIONSHIP_VAULTED_OBJECTS);
            Pattern typePattern = new Pattern(DomainObject.TYPE_WORKSPACE_VAULT);
            typePattern.addPattern(DomainObject.TYPE_WORKSPACE);
            DomainObject domObj = DomainObject.newInstance(context, folderId);
            String objectWhere = "revision !~ \"" + "*auto*" + "\"";
            MapList folderList = domObj.getRelatedObjects(context, wsRelPattern.getPattern(), typePattern.getPattern(), busSelects, null, true, false, (short) 0, null, null);
            // folderList.sortStructure("level", "ascending", "String");
            if (folderList != null && folderList.size() > 0) {
                for (int k = 0; k < folderList.size(); k++) {
                    folderPath.insert(0, (String) ((Map) folderList.get(k)).get(DomainObject.SELECT_NAME) + "/");
                }
            }
            BusinessObject folder = new BusinessObject(folderId);
            folder.open(context);
            folderPath.append(folder.getName());
            folder.close(context);
            return folderPath.toString();
        } catch (Exception e) {
            System.out.println(e.toString());
            return folderPath.toString();
        }
    }

    public MapList getObjectAssignedFolders(Context context, String[] args) throws Exception {
        HashMap inputParamMap = (HashMap) JPO.unpackArgs(args);

        String objectId = (String) inputParamMap.get(DomainObject.SELECT_ID);
        localeLanguage = (String) inputParamMap.get("LocaleLanguage");
        MapList retList = new MapList();

        try {
            TYPE_PROJECT = PropertyUtil.getSchemaProperty(context, "type_ProjectSpace");
            TYPE_WORKSPACE = PropertyUtil.getSchemaProperty(context, "type_Project");
            REL_VAULTED_DOCUMENTS_REV2 = PropertyUtil.getSchemaProperty(context, "relationship_VaultedDocumentsRev2");

            // Get all the paths assigned to some Collection
            retList = getAssignedCollections(context, objectId);

            // Get all the Workspace folder paths
            StringList busSelects = new StringList(5);
            busSelects.add(DomainObject.SELECT_NAME);
            busSelects.add(DomainObject.SELECT_ID);
            Pattern wsRelPattern = new Pattern(DomainObject.RELATIONSHIP_WORKSPACE_VAULTS);

            wsRelPattern.addPattern(DomainObject.RELATIONSHIP_SUB_VAULTS);
            wsRelPattern.addPattern(DomainObject.RELATIONSHIP_VAULTED_OBJECTS);

            DomainObject workspace = DomainObject.newInstance(context, objectId);
            String objectWhere = null; // "where (Revision !~~ \"" + "*auto*" + "\")";
            MapList folderList = workspace.getRelatedObjects(context, wsRelPattern.getPattern(), DomainObject.TYPE_WORKSPACE_VAULT, busSelects, null, true, false, (short) 1, objectWhere, null);
            // folderList.sortStructure("level", "ascending", "String");
            for (int i = 0; i < folderList.size(); i++) {
                Map folderIdMap = (Map) folderList.get(i);
                String folderId = (String) folderIdMap.get(DomainObject.SELECT_ID);
                String path = workspaceFolderToPath(context, folderId);
                HashMap folderMap = new HashMap();
                folderMap.put(DomainObject.SELECT_TYPE, TYPE_WORKSPACE);
                folderMap.put(DomainObject.SELECT_NAME, path);
                folderMap.put(DomainObject.SELECT_ID, objectId);

                retList.add(folderMap);
            }

            // Get all the project folder paths
            DomainObject project = DomainObject.newInstance(context, objectId);
            busSelects = new StringList(5);
            busSelects.add(DomainObject.SELECT_NAME);
            busSelects.add(DomainObject.SELECT_ID);
            wsRelPattern = new Pattern(DomainObject.RELATIONSHIP_WORKSPACE_VAULTS);
            wsRelPattern.addPattern(DomainObject.RELATIONSHIP_SUB_VAULTS);
            wsRelPattern.addPattern(REL_VAULTED_DOCUMENTS_REV2);

            DomainObject domObj = DomainObject.newInstance(context, objectId);

            folderList = domObj.getRelatedObjects(context, wsRelPattern.getPattern(), DomainObject.TYPE_WORKSPACE_VAULT, busSelects, null, true, false, (short) 1, null, null);
            // folderList.sortStructure("level", "ascending", "String");
            for (int i = 0; i < folderList.size(); i++) {
                Map folderIdMap = (Map) folderList.get(i);
                String folderId = (String) folderIdMap.get(DomainObject.SELECT_ID);
                String path = projectFolderToPath(context, folderId);
                HashMap folderMap = new HashMap();
                folderMap.put(DomainObject.SELECT_TYPE, TYPE_PROJECT);
                folderMap.put(DomainObject.SELECT_NAME, path);
                folderMap.put(DomainObject.SELECT_ID, objectId);
                retList.add(folderMap);
            }

        } catch (Exception e) {
            System.out.println("DSCFolderUtil: getObjectsAssignedFolder: " + e.toString());
        }

        return retList;
    }

}
