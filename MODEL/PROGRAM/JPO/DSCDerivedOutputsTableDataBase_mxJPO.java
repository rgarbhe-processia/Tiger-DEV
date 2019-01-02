
/*
 ** DSCDerivedOutputsTableDataBase
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 ** 
 ** This is a JPO which act as a data source for rendering data in to a custom table . Using this JPO program developer can create their own column definitions and can return tabledata in a
 * CustomMapList which stores each row of table as Map objects.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.db.Format;
import matrix.db.FormatList;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.Relationship;
import matrix.db.RelationshipItr;
import matrix.db.RelationshipList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.MCADIntegration.utils.customTable.ColumnDefinition;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.i18nNow;

public class DSCDerivedOutputsTableDataBase_mxJPO {
    MCADGlobalConfigObject _gco = null;

    MCADMxUtil _util = null;

    MCADServerGeneralUtil _generalUtil = null;

    String sIDDepDoc = "";

    MCADServerResourceBundle serverResourceBundle = null;

    IEFGlobalCache cache = null;

    final String REL_PARTOBJ_CADOBJ = "Part Specification";

    final String ATTRNAME_ON_PARTSPECS_REL = "CAD Object Name";

    protected HashMap integrationNameGCOTable = null;

    protected MCADMxUtil util = null;

    protected String localeLanguage = null;

    /**
     * This is constructor which intializes variable declared
     * @author GauravG
     * @since AEF 9.5.2.0
     */

    public DSCDerivedOutputsTableDataBase_mxJPO(Context context, String[] args) throws Exception {
    }

    protected void intializeClassMembers(Context context, MCADGlobalConfigObject gcoObject, String sLanguage) {
        serverResourceBundle = new MCADServerResourceBundle(sLanguage);
        cache = new IEFGlobalCache();
        _gco = gcoObject;
        _util = new MCADMxUtil(context, serverResourceBundle, cache);
        _generalUtil = new MCADServerGeneralUtil(context, _gco, serverResourceBundle, cache);

    }

    /**
     * This method retutns list of column definitions by setting their column properties. These columns definitions control the look & feel of the target table. Following methos controls look & feel
     * of table displayong toolsets.
     * @param Context
     *            context for user logged in
     * @param String
     *            array
     * @return Object as ArrayList
     */
    public Object getColumnDefinitions(Context context, String[] args) throws Exception {
        ArrayList columnDefs = new ArrayList();
        // Creating 3 columns : FileName, Format, Download
        ColumnDefinition column1 = new ColumnDefinition();
        ColumnDefinition column2 = new ColumnDefinition();
        ColumnDefinition column3 = new ColumnDefinition();
        ColumnDefinition column4 = new ColumnDefinition();
        ColumnDefinition column5 = new ColumnDefinition();

        // Initializing column for Component(program / method / wizard) name
        column1.setColumnTitle("mcadIntegration.Server.ColumnName.Name");
        column1.setColumnKey("ObjectName");
        column1.setColumnDataType("string");
        column1.setColumnType("href");
        column1.setColumnTarget("content");

        column2.setColumnTitle("mcadIntegration.Server.ColumnName.Revision");
        column2.setColumnKey("Revision");
        column2.setColumnDataType("string");
        column2.setColumnType("text");

        column3.setColumnTitle("mcadIntegration.Server.ColumnName.Type");
        column3.setColumnKey("Type");
        column3.setColumnDataType("string");
        column3.setColumnType("text");

        column4.setColumnTitle("mcadIntegration.Server.ColumnName.Details");
        column4.setColumnKey("ObjectDetails");
        column4.setColumnDataType("string");
        column4.setColumnTarget("popup");
        column4.setColumnType("icon");
        column4.setColumnIsSortable(false);

        column5.setColumnTitle("mcadIntegration.Server.ColumnName.FileName");
        column5.setColumnKey("FileName");
        column5.setColumnDataType("string");
        column5.setColumnType("text");

        columnDefs.add(column1);
        columnDefs.add(column2);
        columnDefs.add(column3);
        columnDefs.add(column4);
        columnDefs.add(column5);

        return columnDefs;
    }

    /**
     * This method returns Dependent documents table data using a CustomMapList. Each row data of table is stored in Map object which in turn is stored in CustomMaplist. Data Picking logic - Get CAD
     * object connected with selected PART object using "Part Specification" relationship. Store the "CAD Object Name" attribute value on this relationship. Now Find Dependent Document object(s)
     * connected to above CAD object using "Dependent Document Like" relationship such that attribute "CAD Object Name" on this relationship matched with that on Part Specification relationship
     * between PART and CAD object. Show files in picked Dependent Document objects. Limitations: Following method does not use icons specific toolsets but hardcodes a fixed image instead. Following
     * method does not display method-components belonging to a toolset.
     * @param Context
     *            context for user logged in
     * @param String
     *            array This method expects following parameters to be packed in string array sBusId : busId of PART type object langStr : language info for resource bundle jsTreeID : Tree node ID of
     *            above object gcoString : GCO object string
     * @return Object as CustomMapList
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getTableData(Context context, String[] args) throws Exception {
        String sType = "";
        String sName = "";
        String sRev = "";

        MapList attachmentList = new MapList();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String sBusId = (String) paramMap.get("objectId");

        if (null == sBusId || "".equals(sBusId))
            sBusId = (String) paramMap.get("inputObjId");

        String langStr = (String) paramMap.get("languageStr");
        String sInstanceName = (String) paramMap.get("instanceName");
        String suiteDir = (String) paramMap.get("emxSuiteDirectory");

        if (suiteDir == null || suiteDir.length() == 0 || suiteDir.equals("null"))
            suiteDir = "DesignerCentral";

        MCADGlobalConfigObject gcoObject = null;

        localeLanguage = (String) paramMap.get("languageStr");
        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        if (null == cache) {
            cache = new IEFGlobalCache();
        }

        util = new MCADMxUtil(context, serverResourceBundle, cache);
        String integrationName = util.getIntegrationName(context, sBusId);
        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        if (integrationName != null && integrationNameGCOTable != null && integrationNameGCOTable.containsKey(integrationName)) {
            gcoObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);
        }

        intializeClassMembers(context, gcoObject, langStr);

        if (sBusId == null || sBusId.length() == 0)
            return attachmentList;

        /*
         * [NDM] H68: No need to check the following condiition as derived object will be definitely connected to any of the given objcet BusinessObject busObj = new BusinessObject(sBusId);
         * busObj.open(context);
         * 
         * BusinessObject majorBusObj = null; BusinessObject activeMinor = null; String activeMinorId = null;
         * 
         * majorBusObj = _util.getMajorObject(context, busObj);
         * 
         * if(gcoObject != null && majorBusObj == null) //input object is major object { boolean isFinalized = _generalUtil.isBusObjectFinalized(context, busObj);
         * 
         * if(!isFinalized) { majorBusObj = busObj; activeMinor = _util.getActiveMinor(context, majorBusObj);
         * 
         * if(activeMinor != null){ activeMinorId = activeMinor.getObjectId(context); sBusId=activeMinorId; } }
         * 
         * }
         */

        BusinessObject partbus = new BusinessObject(sBusId);
        partbus.open(context);
        String type = partbus.getTypeName();
        String name = partbus.getName();
        partbus.close(context);

        String objectName = name;
        if (sInstanceName != null && !sInstanceName.trim().equals(""))
            objectName = MCADUrlUtil.hexDecode(sInstanceName);

        BusinessObjectList depDocList = null;
        Vector irrelevantFormats = new Vector();

        if (gcoObject != null) {
            if (gcoObject.isCreateDependentDocObj()) {
                depDocList = _generalUtil.getDependentDocObjects(context, partbus, ATTRNAME_ON_PARTSPECS_REL, objectName);
            } else {
                depDocList = new BusinessObjectList(1);
                depDocList.addElement(partbus);

                String cadType = _util.getAttributeForBO(context, sBusId, "CAD Type");
                String cadFormat = _generalUtil.getFormatsForType(context, type, cadType);
                irrelevantFormats.addElement(cadFormat);
            }
        } else {
            ResourceBundle iefProperties = ResourceBundle.getBundle("ief");
            String irrelevantFormatsList = iefProperties.getString("mcadIntegration.Server.DerivedOutputExclusionFormats");

            irrelevantFormats = MCADUtil.getVectorFromString(irrelevantFormatsList, ",");
            depDocList = new BusinessObjectList(1);
            depDocList.addElement(partbus);
        }

        BusinessObjectItr depDocItr = new BusinessObjectItr(depDocList);
        while (depDocItr.next()) {
            BusinessObject busDepDoc = depDocItr.obj();
            sIDDepDoc = busDepDoc.getObjectId();
            FormatList formatList = busDepDoc.getFormats(context);

            for (int j = 0; j < formatList.size(); j++) {
                Format format = (Format) formatList.elementAt(j);
                String formatName = format.getName();

                // if format is irrelevant, i.e., it is a cadFormat, skip it as we want
                // only those formats which have derived output files
                if (irrelevantFormats.contains(formatName)) {
                    continue;
                }

                FileList depdocfileList = busDepDoc.getFiles(context, formatName);
                for (int k = 0; k < depdocfileList.size(); k++) {
                    matrix.db.File depdocfile = (matrix.db.File) depdocfileList.elementAt(k);
                    String fileName = depdocfile.getName();

                    // ================================================================
                    // putting collected data in attachmentList to be returned, in format
                    // expected by CustomTable.
                    sType = busDepDoc.getTypeName();
                    sName = busDepDoc.getName();
                    sRev = busDepDoc.getRevision();

                    HashMap map = new HashMap();

                    map.put("Type", sType);
                    map.put("ObjectName", sName);
                    map.put("Revision", sRev);
                    map.put("FileName", fileName);
                    map.put("format", formatName);

                    map.put("id", sIDDepDoc);
                    map.put("rowid", fileName + "|" + format + "|" + sIDDepDoc);
                    map.put("isDerivedOutputPage", "true");
                    attachmentList.add(map);
                    // ================================================================
                }
            }
        }

        return attachmentList;
    }

    /**
     * This method finds out objects connected with "partbus" using "Part Specification relationship. Also find value of attribute "CAD Object Name" on this relationship. Places object and attribute
     * value in hashmap.
     */

    public Hashtable getPartSpecsRelatedObjects(Context context, BusinessObject partbus) {
        Hashtable hashCadObjectParentName = new Hashtable();
        String sPartSpecsRel = REL_PARTOBJ_CADOBJ;

        // TBD - change it to CAD Object Name
        String attName = ATTRNAME_ON_PARTSPECS_REL;

        try {
            RelationshipList relList = _util.getFromRelationship(context, partbus, (short) 0, false);
            RelationshipItr Itr = new RelationshipItr(relList);
            while (Itr.next()) {
                Relationship rel = Itr.obj();
                BusinessObject cadObject = rel.getTo();

                String thisRelName = rel.getTypeName();
                if (thisRelName.equals(sPartSpecsRel)) {
                    String attParentNameVal = _util.getRelationshipAttributeValue(context, rel, attName);
                    hashCadObjectParentName.put(cadObject, attParentNameVal);
                }
            }
        } catch (Exception e) {
        }
        return hashCadObjectParentName;
    }

    protected String getObjectDetailsPageName(HashMap argumentsTable) {
        String objectDetailsPageName = "../common/emxTree.jsp";
        String sSuiteDirectory = (String) argumentsTable.get("emxSuiteDirectory");

        if (sSuiteDirectory != null && sSuiteDirectory.indexOf("infocentral") > -1) {
            objectDetailsPageName = "../infocentral/emxInfoManagedMenuEmxTree.jsp";
        } else if (sSuiteDirectory != null && sSuiteDirectory.indexOf("iefdesigncenter") > -1) {
            objectDetailsPageName = "../common/emxTree.jsp";
        }

        return objectDetailsPageName;
    }

    public Vector getFileNames(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            String fileName = null;

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                fileName = (String) objDetails.get("FileName");
            } catch (Exception e) {
            }

            columnCellContentList.add(fileName);
        }

        return columnCellContentList;
    }

    public Vector getRevisions(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            String revText = null;

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                revText = (String) objDetails.get("Revision");

            } catch (Exception e) {
            }

            columnCellContentList.add(revText);
        }

        return columnCellContentList;
    }

    public Vector getTypes(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            String typeText = null;

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                typeText = (String) objDetails.get("Type");
            } catch (Exception e) {
            }

            columnCellContentList.add(typeText);
        }

        return columnCellContentList;
    }

    protected String getViewerURL(Context context, String objectId, String format, String fileName) {
        try {
            String lang = (String) context.getSession().getLanguage();
            String sTipView = i18nNow.getI18nString("emxTeamCentral.ContentSummary.ToolTipView", "emxTeamCentralStringResource", lang);
            StringBuffer htmlBuffer = new StringBuffer();

            // format and store all of them in a String seperated by comma
            MQLCommand prMQL = new MQLCommand();
            prMQL.open(context);
            prMQL.executeCommand(context, "execute program $1 $2", "eServicecommonGetViewers.tcl", format);

            String sResult = prMQL.getResult().trim();
            String error = prMQL.getError();
            String sViewerServletName = "";

            if (null != sResult && sResult.length() > 0) {
                StringTokenizer viewerTokenizer = new StringTokenizer(sResult, "|", false);
                String sErrorCode = "";

                if (viewerTokenizer.hasMoreTokens())
                    sErrorCode = viewerTokenizer.nextToken();

                if (sErrorCode.equals("0")) {
                    if (viewerTokenizer.hasMoreTokens())
                        sViewerServletName = viewerTokenizer.nextToken();
                    if (viewerTokenizer.hasMoreTokens())
                        sTipView = viewerTokenizer.nextToken();
                }
                if (sViewerServletName == null || sViewerServletName.length() == 0)
                    return "";

                String sFileViewerLink = "/servlet/" + sViewerServletName;
                String viewerURL = "../iefdesigncenter/emxInfoViewer.jsp?url=" + sFileViewerLink + "&amp;id=" + objectId + "&amp;format=" + format + "&amp;file=" + MCADUrlUtil.hexEncode(fileName);
                String viewerHref = viewerURL;

                viewerHref = "javascript:openWindow('" + viewerURL + "')";
                String url = getFeatureIconContent(viewerHref, "../../iefdesigncenter/images/iconActionViewer.gif", sTipView + " (" + format + ")");

                htmlBuffer.append(url);
                // htmlBuffer.append("&nbsp");
            }

            return htmlBuffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public Object getActionLinks(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();
        StringBuffer htmlBuffer = new StringBuffer();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        paramMap.put("displayCheckout", "true");
        paramMap.put("displayViewer", "true");

        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new MCADMxUtil(context, serverResourceBundle, cache);

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            String fileName = null;

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                String objectId = (String) objDetails.get("id");

                // String integrationName = util.getIntegrationName(objectId);
                htmlBuffer = new StringBuffer();
                fileName = (String) objDetails.get("FileName");

                String format = (String) objDetails.get("format");
                String hexfileName = MCADUrlUtil.hexEncode(fileName);
                String checkoutHref = "../iefdesigncenter/DSCComponentCheckoutWrapper.jsp?" + "objectId=" + objectId + "&amp;action=download" + "&amp;format=" + format + "&amp;fileName=" + hexfileName
                        + "&amp;refresh=false&amp;";

                String checkoutToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.Download");
                checkoutHref = "javascript:openWindow('" + checkoutHref + "')";

                htmlBuffer.append(getFeatureIconContent(checkoutHref, "../../common/images/iconActionDownload.gif", checkoutToolTip));
                htmlBuffer.append(getViewerURL(context, objectId, format, fileName));
            } catch (Exception e) {
            }
            columnCellContentList.add(htmlBuffer.toString());
        }
        return columnCellContentList;
    }

    protected String getFeatureIconContent(String href, String featureImage, String toolTop) {
        StringBuffer featureIconContent = new StringBuffer();

        featureIconContent.append("<a href=\"");
        featureIconContent.append(href);
        featureIconContent.append("\" ><img src=\"images/");
        featureIconContent.append(featureImage);
        featureIconContent.append("\" border=\"0\" title=\"");
        featureIconContent.append(toolTop);
        featureIconContent.append("\"/></a>");

        return featureIconContent.toString();
    }

    public static Boolean isObjectBased(Context context, String[] args) throws Exception {
        return new Boolean(false);
    }
}
