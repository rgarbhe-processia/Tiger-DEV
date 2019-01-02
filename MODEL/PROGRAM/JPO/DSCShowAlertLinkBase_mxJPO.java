
/*
 ** DSCShowAlertLinkBase
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display Checkout Icon
 */
import java.util.HashMap;
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
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;

public class DSCShowAlertLinkBase_mxJPO {
    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private IEFGlobalCache cache = null;

    private String localeLanguage = null;

    protected String REL_VERSION_OF = "";

    protected String REL_ACTIVE_VERSION = "";

    protected String REL_MARKUP = "";

    protected String REL_PART_SPECIFICATION = "";

    protected String SELECT_ON_MAJOR = "";

    protected String SELECT_ON_ACTIVE_MINOR = "";

    protected String SELECT_ON_SPC_PART = "";

    protected String IS_MARKUPS_EXIST = "";

    protected String ATTR_SOURCE = "";

    protected String ATTR_IS_REPLACEMENTDONE = "";

    protected String ATTR_RENAMED_FROM = "";

    protected String REL_DECBASELINE = "";

    protected String SELECT_ON_BASELINE = "";

    protected String BASELINE_ID = "";

    protected String IS_VERSION_OBJ = "";

    protected String SELECT_ISVERSIONOBJ = "";

    // static String formatJTStr = PropertyUtil.getSchemaProperty("format_JT");

    public DSCShowAlertLinkBase_mxJPO(Context context, String[] args) throws Exception {
    }

    public boolean isObjectReplaced(Context context, BusinessObjectWithSelect busObjectWithSelect) throws Exception {
        boolean isReplaced = false;
        String replaced = "";
        // [NDM] Start Op6
        // String busType = busObjectWithSelect.getSelectData("type");
        String busId = busObjectWithSelect.getSelectData("id");
        String sIsVersion = busObjectWithSelect.getSelectData("SELECT_ISVERSIONOBJ");
        boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
        if (!isVersion)// gco.isMajorType(busType)) // [NDM] End Op6
        {
            replaced = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + ATTR_IS_REPLACEMENTDONE);
        } else {
            replaced = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + SELECT_ON_ACTIVE_MINOR + ATTR_IS_REPLACEMENTDONE);
        }

        if ((replaced == null) || (replaced.equals("")) || !(replaced.length() > 0))
            replaced = busObjectWithSelect.getSelectData(ATTR_IS_REPLACEMENTDONE);

        if (replaced.compareToIgnoreCase("true") == 0) {
            isReplaced = true;
        }

        return isReplaced;
    }

    public boolean isObjectRenamed(Context context, BusinessObjectWithSelect busObjectWithSelect) throws Exception {
        boolean isRenamed = false;
        // [NDM] Start Op6
        // String busType = busObjectWithSelect.getSelectData("type");
        String renamedFromAttribute = "";
        String busId = busObjectWithSelect.getSelectData("id");
        String sIsVersion = busObjectWithSelect.getSelectData("SELECT_ISVERSIONOBJ");
        boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();
        if (!isVersion)// gco.isMajorType(busType)) // [NDM] End Op6
        {
            renamedFromAttribute = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + ATTR_RENAMED_FROM);
        } else {
            renamedFromAttribute = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + SELECT_ON_ACTIVE_MINOR + ATTR_RENAMED_FROM);
        }

        if (renamedFromAttribute == null || renamedFromAttribute.equals("") || !(renamedFromAttribute.length() > 0))
            renamedFromAttribute = busObjectWithSelect.getSelectData(ATTR_RENAMED_FROM);

        if (renamedFromAttribute != null && !renamedFromAttribute.equals("") && renamedFromAttribute.length() > 0) {
            isRenamed = true;
        }

        return isRenamed;
    }

    public Object getHtmlString(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        localeLanguage = (String) paramMap.get("languageStr");
        String portalName = (String) paramMap.get("portCmdName");
        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        return getHtmlStringForTable(context, relBusObjPageList, portalName);
    }

    public Object getHtmlStringForFrameworkTable(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        localeLanguage = (String) paramMap.get("languageStr");
        String portalName = (String) paramMap.get("portCmdName");

        HashMap paramList = (HashMap) paramMap.get("paramList");
        integrationNameGCOTable = (HashMap) paramList.get("GCOTable");

        if (localeLanguage == null)
            localeLanguage = (String) paramList.get("languageStr");

        if (integrationNameGCOTable == null)
            integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        if (integrationNameGCOTable == null) {

        }

        return getHtmlStringForTable(context, relBusObjPageList, portalName);
    }

    private Object getHtmlStringForTable(Context context, MapList relBusObjPageList, String portalName) throws Exception {
        Vector columnCellContentList = new Vector();
        String sUseMinor = "";

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new MCADMxUtil(context, serverResourceBundle, cache);

        // HashMap ebomTNRebomConfigObjMap = new HashMap();

        String[] objIds = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            objIds[i] = (String) objDetails.get("id");

            // Designer Central 10.6.0.1
            sUseMinor = (String) objDetails.get("UseMinor");
        }

        try {
            String renameToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.AlertRename");
            String replaceToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.AlertReplace");
            String batchProcessorToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.AlertBatchRename");
            String markupToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.AlertMarkup");
            String partToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.AlertPart");
            String baselineToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.AlertBaseline");

            StringList busSelectionList = getBusSelectionList(context, util);

            BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);

            for (int i = 0; i < buslWithSelectionList.size(); i++) {
                BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(i);

                StringBuffer htmlBuffer = new StringBuffer();
                String integrationName = null;

                String objectId = busObjectWithSelect.getSelectData("id");
                String integrationSource = busObjectWithSelect.getSelectData(ATTR_SOURCE);
                boolean isConnectToBaseLine = false;

                if (integrationSource != null) {
                    StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

                    if (integrationSourceTokens.hasMoreTokens())
                        integrationName = integrationSourceTokens.nextToken();
                }

                if (integrationName != null) {
                    // MCADGlobalConfigObject gco = (MCADGlobalConfigObject)integrationNameGCOTable.get(integrationName);
                    if (null == cache) {
                        cache = new IEFGlobalCache();
                    }

                    // MCADServerGeneralUtil serverGeneralUtil1 = new MCADServerGeneralUtil(context, gco, serverResourceBundle, cache);

                    String checkoutHref = "javascript:checkoutWithValidation('" + integrationName + "', '" + objectId + "', '', '', '')";

                    boolean isReplacementDone = false;
                    isReplacementDone = isObjectReplaced(context, busObjectWithSelect);

                    if (sUseMinor != null && sUseMinor.equals("true"))
                        objectId = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + "last." + SELECT_ON_ACTIVE_MINOR + "id");

                    boolean isUpdate = false;
                    boolean isAssociatedDesignRenamed = false;
                    isUpdate = isObjectRenamed(context, busObjectWithSelect);

                    // if(gco.isBatchProcessorForRenameEnabled())
                    // isAssociatedDesignRenamed = isAssociatedDesignRenamed(context , busObjectWithSelect);

                    if (true == isUpdate) {
                        htmlBuffer.append(getFeatureIconContent(checkoutHref, "iconAlertChanged.gif", renameToolTip));
                    }
                    // else if(true == isAssociatedDesignRenamed)
                    // {
                    // htmlBuffer.append(getFeatureIconContent(checkoutHref, "iconAlertChanged.gif", batchProcessorToolTip));
                    // }
                    if (true == isReplacementDone) {
                        htmlBuffer.append(getFeatureIconContent(checkoutHref, "iconStatusChanged.gif", replaceToolTip));
                    }

                    // show the Markup alert
                    String hasMarkups = "false";
                    hasMarkups = busObjectWithSelect.getSelectData(IS_MARKUPS_EXIST);

                    if (hasMarkups.equalsIgnoreCase("false")) {
                        hasMarkups = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + IS_MARKUPS_EXIST);
                    }

                    if (hasMarkups.equalsIgnoreCase("true")) {
                        htmlBuffer.append(getFeatureIconContent(checkoutHref, "iconAlertMarkup.gif", markupToolTip));
                    }

                    // show the Part alert
                    // IEFEBOMConfigObject ebomConfigObject1 = null;

                    // String sEBOMRegistryTNR = gco.getEBOMRegistryTNR();

                    // if(!ebomTNRebomConfigObjMap.containsKey(sEBOMRegistryTNR))
                    // {
                    // StringTokenizer token = new StringTokenizer(sEBOMRegistryTNR, "|");
                    //
                    // if(token.countTokens() >= 3)
                    // {
                    // String sEBOMRConfigObjType = (String) token.nextElement();
                    // String sEBOMRConfigObjName = (String) token.nextElement();
                    // String sEBOMRConfigObjRev = (String) token.nextElement();
                    //
                    // ebomConfigObject = new IEFEBOMConfigObject(context, sEBOMRConfigObjType, sEBOMRConfigObjName, sEBOMRConfigObjRev);
                    //
                    // ebomTNRebomConfigObjMap.put(sEBOMRegistryTNR, ebomConfigObject);
                    // }
                    // }
                    // else
                    // ebomConfigObject = (IEFEBOMConfigObject) ebomTNRebomConfigObjMap.get(sEBOMRegistryTNR);

                    String assignPartToMajor = "true";// ebomConfigObject.getConfigAttributeValue(IEFEBOMConfigObject.ATTR_ASSIGN_PART_TO_MAJOR);[NDM]

                    String partObjId = busObjectWithSelect.getSelectData(SELECT_ON_SPC_PART + "id");

                    if (((partObjId.equals("")) || (partObjId == null)) && ("false".equalsIgnoreCase(assignPartToMajor))) {
                        partObjId = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + SELECT_ON_SPC_PART + "id");
                    }

                    if (partObjId != null && !partObjId.equals("")) {
                        htmlBuffer.append(getFeatureIconContent(checkoutHref, "iconAlertPart.gif", partToolTip));
                    }

                    isConnectToBaseLine = isDesignBaselined(context, busObjectWithSelect);
                    if (isConnectToBaseLine) {
                        htmlBuffer.append(getFeatureIconContent(checkoutHref, "iconSmallBaseline.gif", baselineToolTip));
                    }

                    columnCellContentList.add(htmlBuffer.toString());
                } else {
                    columnCellContentList.add(htmlBuffer.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        return columnCellContentList;
    }

    private String getFeatureIconContent(String href, String featureImage, String toolTop) {
        StringBuffer featureIconContent = new StringBuffer();
        featureIconContent.append("<a><img src=\"../iefdesigncenter/images/");
        featureIconContent.append(featureImage);
        featureIconContent.append("\" border=\"0\" title=\"");
        featureIconContent.append(toolTop);
        featureIconContent.append("\"/></a>");

        return featureIconContent.toString();
    }

    public boolean isAssociatedDesignRenamed(Context context, BusinessObjectWithSelect busObjectWithSelect) throws Exception {
        boolean isAssociatedDesignRenamed = false;
        // [NDM] Start Op6
        // String busType = busObjectWithSelect.getSelectData("type");
        String renamedFromAttribute = "";
        String busId = busObjectWithSelect.getSelectData("id");

        if (util.isMajorObject(context, busId))// gco.isMajorType(busType)) // [NDM] End Op6
        {
            renamedFromAttribute = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + ATTR_RENAMED_FROM);
        } else {
            renamedFromAttribute = busObjectWithSelect.getSelectData(SELECT_ON_MAJOR + SELECT_ON_ACTIVE_MINOR + ATTR_RENAMED_FROM);
        }

        if (renamedFromAttribute == null || renamedFromAttribute.equals("") || !(renamedFromAttribute.length() > 0))
            renamedFromAttribute = busObjectWithSelect.getSelectData(ATTR_RENAMED_FROM);

        if (renamedFromAttribute != null && !renamedFromAttribute.equals("") && renamedFromAttribute.length() > 0) {
            isAssociatedDesignRenamed = true;
        }

        return isAssociatedDesignRenamed;
    }

    // To be implemented in the overridden class
    protected StringList getBusSelectionList(Context context, MCADMxUtil util) throws Exception {
        REL_VERSION_OF = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
        REL_ACTIVE_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
        REL_MARKUP = MCADMxUtil.getActualNameForAEFData(context, "relationship_Markup");
        REL_PART_SPECIFICATION = MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification");
        REL_DECBASELINE = MCADMxUtil.getActualNameForAEFData(context, "relationship_DesignBaseline");

        SELECT_ON_MAJOR = "from[" + REL_VERSION_OF + "].to.";
        SELECT_ON_ACTIVE_MINOR = "from[" + REL_ACTIVE_VERSION + "].to.";
        SELECT_ON_SPC_PART = "to[" + REL_PART_SPECIFICATION + "].from.";
        SELECT_ON_BASELINE = "from[" + REL_DECBASELINE + "].to.";

        BASELINE_ID = SELECT_ON_BASELINE + "id";

        IS_MARKUPS_EXIST = "from[" + REL_MARKUP + "]";

        ATTR_SOURCE = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
        ATTR_IS_REPLACEMENTDONE = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_DSC-IsReplacementDone") + "]";
        ATTR_RENAMED_FROM = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_RenamedFrom") + "]";

        StringList busSelectionList = new StringList();

        busSelectionList.addElement("id");
        busSelectionList.addElement("type");

        busSelectionList.addElement(ATTR_SOURCE); // To get Integrations name
        busSelectionList.addElement(SELECT_ON_MAJOR + "last." + SELECT_ON_ACTIVE_MINOR + "id");

        busSelectionList.addElement(ATTR_IS_REPLACEMENTDONE); // IsReplacementDone default.
        busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + ATTR_IS_REPLACEMENTDONE); // IsReplacementDone from major.
        busSelectionList.addElement(SELECT_ON_MAJOR + SELECT_ON_ACTIVE_MINOR + ATTR_IS_REPLACEMENTDONE); // IsReplacementDone from minor.

        busSelectionList.addElement(BASELINE_ID);// baseline on Major
        busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + BASELINE_ID); // Baseline On Active Minor

        busSelectionList.addElement(ATTR_RENAMED_FROM); // IsRenamed default.
        busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + ATTR_RENAMED_FROM); // IsRenamed from major.
        busSelectionList.addElement(SELECT_ON_MAJOR + "last." + SELECT_ON_ACTIVE_MINOR + ATTR_RENAMED_FROM); // IsRenamed from minor.

        busSelectionList.addElement(IS_MARKUPS_EXIST); // Markup List.
        busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + IS_MARKUPS_EXIST); // Markup List.

        busSelectionList.addElement(SELECT_ON_ACTIVE_MINOR + SELECT_ON_SPC_PART + "id"); // Part Id.
        busSelectionList.addElement(SELECT_ON_SPC_PART + "id"); // Part Id.
        IS_VERSION_OBJ = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
        SELECT_ISVERSIONOBJ = "attribute[" + IS_VERSION_OBJ + "]";
        busSelectionList.addElement(SELECT_ISVERSIONOBJ);
        return busSelectionList;
    }

    public boolean isDesignBaselined(Context context, BusinessObjectWithSelect busObjectWithSelect) throws Exception {
        boolean isDesignBaselined = false;
        // [NDM] Start Op6
        // String busType = busObjectWithSelect.getSelectData("type");
        String baselineId = "";
        String busId = busObjectWithSelect.getSelectData("id");

        String sIsVersion = busObjectWithSelect.getSelectData("SELECT_ISVERSIONOBJ");
        boolean isVersion = Boolean.valueOf(sIsVersion).booleanValue();

        baselineId = busObjectWithSelect.getSelectData(BASELINE_ID);
        if (baselineId.equals("") || baselineId.length() == 0) {
            if (!isVersion)// gco.isMajorType(busType)) // [NDM] End Op6
            {
                baselineId = busObjectWithSelect.getSelectData(SELECT_ON_ACTIVE_MINOR + BASELINE_ID);
            }
        }

        if (!baselineId.equals("") && baselineId.length() > 0) {
            isDesignBaselined = true;
        }
        return isDesignBaselined;
    }
}
