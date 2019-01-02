
/*
 ** DSCShowRecentCheckinColumnLink
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display version label.
 */
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.util.MapList;

public class DSCShowRecentCheckinColumnLink_mxJPO extends emxCommonBaseComparator_mxJPO {
    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private String localeLanguage = null;

    private IEFGlobalCache cache = null;

    public static final String TNR_SEP = "~";

    public String MOVE_FILES_TO_VERSION = "";

    public String IS_VERSION_OBJECT = "";

    public DSCShowRecentCheckinColumnLink_mxJPO() {

    }

    public DSCShowRecentCheckinColumnLink_mxJPO(Context context, String[] args) throws Exception {
    }

    public Vector getVersions(Context context, String[] args) throws Exception {
        DSC_CommonUtil_mxJPO jpoUtil = new DSC_CommonUtil_mxJPO();

        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        localeLanguage = (String) paramMap.get("LocaleLanguage");

        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new MCADMxUtil(context, serverResourceBundle, cache);

        IS_VERSION_OBJECT = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
        MOVE_FILES_TO_VERSION = MCADMxUtil.getActualNameForAEFData(context, "attribute_MoveFilesToVersion");

        String jsTreeID = (String) paramMap.get("jsTreeID");
        if (jsTreeID == null) {
            Map paramList = (Map) paramMap.get("paramList");

            if (null != paramList) {
                jsTreeID = (String) paramList.get("jsTreeID");
            }

            if (jsTreeID == null)
                jsTreeID = "";
        }

        String suiteDirectory = (String) paramMap.get("emxSuiteDirectory");
        if (suiteDirectory == null) {
            Map paramList = (Map) paramMap.get("paramList");
            if (null != paramList) {
                suiteDirectory = (String) paramList.get("emxSuiteDirectory");
            }
            if (suiteDirectory == null)
                suiteDirectory = "";
        }

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            String version = "";

            try {
                Map objDetails = (Map) relBusObjPageList.get(i);
                String objectId = (String) objDetails.get("id");
                String integrationName = util.getIntegrationName(context, objectId);

                if (integrationName != null && integrationNameGCOTable.containsKey(integrationName)) {
                    MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);
                    if (null != globalConfigObject && false == globalConfigObject.isCreateVersionObjectsEnabled()) {
                        version = "--";
                    } else {
                        BusinessObject busObject = new BusinessObject(objectId);
                        busObject.open(context);

                        BusinessObject majorBusObject = util.getMajorObject(context, busObject);
                        if (majorBusObject != null) {
                            majorBusObject.open(context);
                            String majorRevision = majorBusObject.getRevision();
                            String minorRevision = busObject.getRevision();

                            version = MCADUtil.getVersionFromMinorRevision(majorRevision, minorRevision);
                            majorBusObject.close(context);
                        } else {
                            HashMap attrMap = jpoUtil.getCommonDocumentAttributes(context, objectId);
                            String retObjectId = objectId;

                            BusinessObject minorObject = busObject;

                            if (fileInMinor(attrMap) == false)
                                retObjectId = util.getActiveVersionObject(context, objectId);
                            else
                                retObjectId = util.getLatestMinorID(context, busObject);

                            if (retObjectId != null && !retObjectId.equals("")) {
                                minorObject = new BusinessObject(retObjectId);

                                minorObject.open(context);

                                String minorRevision = minorObject.getRevision();
                                int pos = minorRevision.indexOf('.');
                                if (pos > 0) {
                                    version = minorRevision.substring(pos + 1, minorRevision.length());
                                }

                                minorObject.close(context);
                            }
                        }

                        busObject.close(context);
                    }
                } else {
                    HashMap attrMap = jpoUtil.getCommonDocumentAttributes(context, objectId);
                    BusinessObject busObject = null;

                    if (isVersionObject(attrMap))
                        busObject = new BusinessObject(objectId);
                    else
                        busObject = new BusinessObject(util.getActiveVersionObject(context, objectId));

                    if (busObject != null) {
                        busObject.open(context);
                        String minorVersion = busObject.getRevision();

                        if (minorVersion != null) {
                            int pos = minorVersion.indexOf('.');
                            if (pos > 0)
                                version = minorVersion.substring(pos + 1);
                            else
                                version = minorVersion;
                        }

                        busObject.close(context);
                    }
                }
            } catch (Exception e) {

            }

            columnCellContentList.add(version);
        }
        return columnCellContentList;
    }

    public Vector getDesignCheckinDate(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        /*
         * localeLanguage = (String)paramMap.get("LocaleLanguage");
         * 
         * integrationNameGCOTable = (HashMap)paramMap.get("GCOTable");
         * 
         * serverResourceBundle = new MCADServerResourceBundle(localeLanguage); cache = new IEFGlobalCache(); util = new MCADMxUtil(context, serverResourceBundle, cache);
         */

        String checkinDate = null;

        try {
            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map objDetails = (Map) relBusObjPageList.get(i);
                checkinDate = (String) objDetails.get("Checkin Date");

                columnCellContentList.add(checkinDate);
            }
        } catch (Exception e) {
        }
        return columnCellContentList;
    }

    public Vector getRecentlyCheckinRevision(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        String relVersionOf = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
        String[] objIds = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("objectList");
        }

        StringList busSelectionList = new StringList();
        busSelectionList.addElement("id");
        busSelectionList.addElement("revision");
        busSelectionList.addElement("from[" + relVersionOf + "].to.revision");

        BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

        for (int i = 0; i < buslWithSelectionList.size(); i++) {
            BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(i);

            String revision = busObjectWithSelect.getSelectData("revision");
            String majorRevision = busObjectWithSelect.getSelectData("from[" + relVersionOf + "].to.revision");

            try {
                if (majorRevision == null || majorRevision.equals(""))
                    columnCellContentList.add(revision);
                else
                    columnCellContentList.add(majorRevision);
            } catch (Exception e) {
            }
        }
        return columnCellContentList;
    }

    public Vector getRecentlyCheckinVersion(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        String version = null;

        try {
            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map objDetails = (Map) relBusObjPageList.get(i);
                version = (String) objDetails.get("Version");

                columnCellContentList.add(version);
            }
        } catch (Exception e) {
        }
        return columnCellContentList;
    }

    public Vector getRecentlyCheckinState(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        String[] objIds = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("objectList");
        }
        String relVersionOf = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");

        StringList busSelectionList = new StringList();
        busSelectionList.addElement("id");
        busSelectionList.addElement("state");
        busSelectionList.addElement("from[" + relVersionOf + "].to.current");

        BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

        for (int i = 0; i < buslWithSelectionList.size(); i++) {
            BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(i);

            String state = busObjectWithSelect.getSelectData("state");
            String majorState = busObjectWithSelect.getSelectData("from[VersionOf].to.current");

            try {
                if (majorState == null || majorState.equals(""))
                    columnCellContentList.add(state);
                else
                    columnCellContentList.add(majorState);
            } catch (Exception e) {
            }
        }
        return columnCellContentList;
    }

    public Vector getRecentlyCheckinType(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        String relVersionOf = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");

        String[] objIds = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("objectList");
        }

        StringList busSelectionList = new StringList();
        busSelectionList.addElement("id");
        busSelectionList.addElement("type");
        busSelectionList.addElement("from[" + relVersionOf + "].to.type");

        BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

        for (int i = 0; i < buslWithSelectionList.size(); i++) {
            BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(i);

            String type = busObjectWithSelect.getSelectData("type");
            String majorType = busObjectWithSelect.getSelectData("from[VersionOf].to.type");

            try {
                if (majorType == null || majorType.equals(""))
                    columnCellContentList.add(type);
                else
                    columnCellContentList.add(majorType);
            } catch (Exception e) {
            }
        }
        return columnCellContentList;
    }

    private boolean isVersionObject(HashMap objectMap) {
        String result = (String) objectMap.get(IS_VERSION_OBJECT);

        if (result == null || result.length() < 0)
            return false;
        return result.equalsIgnoreCase("true");
    }

    private boolean fileInMinor(HashMap objectMap) {
        String result = (String) objectMap.get(MOVE_FILES_TO_VERSION);
        if (result == null || result.length() < 0)
            return false;
        return result.equalsIgnoreCase("true");
    }

    public int compare(Object object1, Object object2) {
        int returnValue = 0;

        Map sortKeys = getSortKeys();

        // Get column values from object1 and object2
        Map m1 = (Map) object1;
        Map m2 = (Map) object2;

        String columnKey = (String) sortKeys.get("name");
        String sortDirection = (String) sortKeys.get("dir");

        String columnValue1 = (String) m1.get(columnKey);
        String columnValue2 = (String) m2.get(columnKey);

        String timePattern = "MM/dd/yyyy hh:mm:ss a";
        java.text.SimpleDateFormat formatter = (java.text.SimpleDateFormat) java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.LONG, java.text.DateFormat.LONG, Locale.US);
        formatter.applyPattern(timePattern);

        try {
            Date checkinDate1 = formatter.parse(columnValue1);
            Date checkinDate2 = formatter.parse(columnValue2);

            returnValue = checkinDate1.compareTo(checkinDate2);

            if (sortDirection.equalsIgnoreCase("descending"))
                returnValue = (-1 * returnValue);
        } catch (Exception e) {
            // TODO: handle exception
        }

        return returnValue;
    }
}
