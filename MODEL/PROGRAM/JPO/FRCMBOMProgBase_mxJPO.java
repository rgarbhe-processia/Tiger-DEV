import java.io.BufferedWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import com.dassault_systemes.enovia.enterprisechangemgt.admin.ECMAdmin;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeAction;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeRequest;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.dassault_systemes.i3dx.appsmodel.matrix.Relationship;
import com.dassault_systemes.vplm.ProductionSystemAuthoring.interfaces.IVPLMProductionSystemAuthoring;
import com.dassault_systemes.vplm.ProductionSystemNav.interfaces.IVPLMProductionSystemNav;
import com.dassault_systemes.vplm.fctProcessAuthoring.interfaces.IVPLMFctProcessImplementLinkAuthoring;
import com.dassault_systemes.vplm.fctProcessNav.utility.MBOMILImpactDiagnosis;
import com.dassault_systemes.vplm.fctProcessNav.utility.MBOMSLOutputInfo;
import com.dassault_systemes.vplm.interfaces.access.IPLMxCoreAccess;
import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.dassault_systemes.vplm.modeler.entity.PLMxRefInstanceEntity;
import com.dassault_systemes.vplm.modeler.entity.PLMxReferenceEntity;
import com.dassault_systemes.vplm.modeler.entity.PLMxSemanticRelation;
import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.configuration.ConfigurationUtil;
import com.matrixone.apps.configuration.ProductConfiguration;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.effectivity.EffectivityExpression;
import com.matrixone.apps.effectivity.EffectivityFramework;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.productline.ProductLineConstants;
import com.matrixone.plmql.cmd.PLMID;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Query;
import matrix.util.StringList;

import com.mbom.modeler.utility.*;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class FRCMBOMProgBase_mxJPO {
    protected static BufferedWriter perfoTraces = null;

    public FRCMBOMProgBase_mxJPO(Context context, String[] args) throws Exception {
    }

    public static final String ATT_PLMEXTID = "PLMEntity.PLM_ExternalID";

    public static final String REL_VOWNER = "VPLMrel/PLMConnection/V_Owner";

    public static final String TYP_PRODCONF = "VPMCfgConfiguration";

    public static final StringList EXPD_BUS_SELECT = new StringList(new String[] { "physicalid", "logicalid" });

    public static final StringList EXPD_REL_SELECT = new StringList(new String[] { "physicalid[connection]", "logicalid[connection]", "from.physicalid", "attribute[PLMInstance.V_TreeOrder].value" });

    public static Map<String, List<String>> derivedTypesList = new HashMap<String, List<String>>();

    // PSS : Customize type list
    // public static List<String> baseTypesForMBOMAssemblyNodes = new StringList(new String[] { "CreateAssembly", "CreateMaterial", "CreateKit", "Disassemble" });
    public static List<String> baseTypesForMBOMAssemblyNodes = new StringList(new String[] { "CreateAssembly", "CreateMaterial", "CreateKit" });

    // PSS : Customize type list
    // public static List<String> baseTypesForMBOMLeafNodes = new StringList(new String[] { "Provide", "ProcessContinuousProvide", "CreateMaterial", "ProcessContinuousCreateMaterial",
    // "ElementaryEndItem" });
    public static List<String> baseTypesForMBOMLeafNodes = new StringList(new String[] { "ProcessContinuousProvide", "CreateMaterial" });

    private static String[] listMagnitudeFieldKeys = { "FRCMBOMCentral.MBOMManufItemMagnitudeLength", "FRCMBOMCentral.MBOMManufItemMagnitudeMass", "FRCMBOMCentral.MBOMManufItemMagnitudeArea",
            "FRCMBOMCentral.MBOMManufItemMagnitudeVolume" };

    private static String[] listMakeBuyFieldKeys = { "FRCMBOMCentral.Undefined", "FRCMBOMCentral.Make", "FRCMBOMCentral.Buy" };

    private static String[] listYesNoFieldKeys = { "FRCMBOMCentral.Yes", "FRCMBOMCentral.No" };

    public static Map<String, String> ebomToPSTypeMapping;

    static {
        ebomToPSTypeMapping = new HashMap<String, String>();
        ebomToPSTypeMapping.put("Part", "VPMReference");

        try {
            // perfoTraces = Files.newBufferedWriter(Paths.get(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "FRCPerfoTraces.txt"), StandardCharsets.UTF_8);
        } catch (Exception e) {
        }
        ;
    }

    private static List<String> lModelListOnStructure = new ArrayList<String>();

    private static String valueEnvAttachModel;
    // =====================================================
    // Entry points
    // =====================================================

    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getExpandProductStructure(Context context, String[] args) throws Exception {// Expand program called by the emxIndentedTable.jsp of the PS table
        long startTime = System.currentTimeMillis();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        // Add configuration filter
        String filterExpression = (String) paramMap.get("FRCExpressionFilterInput_OID");// Filter from AFN
        String filterValue = (String) paramMap.get("FRCExpressionFilterInput_actualValue");// Filter from AFN
        String filterInput = (String) paramMap.get("FRCExpressionFilterInput");

        String objectId = (String) paramMap.get("objectId");

        String expandLevel = (String) paramMap.get("expandLevel");
        short stopLevel = 0;// If All then expand All so stop level=0

        // EPI : correction bug when applying a config filter : force to do an Expand All
        // if (!"All".equals(expandLevel))
        // stopLevel = Short.parseShort(expandLevel);

        MapList res;

        ContextUtil.startTransaction(context, false);
        try {
            res = getExpandPS(context, objectId, stopLevel, filterExpression, filterValue, filterInput, EXPD_REL_SELECT, EXPD_BUS_SELECT);

            // START UM5c06 : Build Paths and save theses in the return maps + add "hasChildren=false" where needed

            // Store path in a Map to be able to manage unsorted return MapList
            HashMap<String, String> mapPaths = new HashMap<String, String>();
            HashMap<String, String> mapPathsLogical = new HashMap<String, String>();

            DomainObject objectDOM = new DomainObject(objectId);

            String rootPID = objectDOM.getInfo(context, "physicalid");
            mapPaths.put(rootPID, rootPID);

            String rootLID = objectDOM.getInfo(context, "logicalid");
            mapPathsLogical.put(rootPID, rootLID);

            // Declare variable before to improve prefs
            Map<String, Object> mapObj;
            String objPID, objFromPID, objPIDConnection, objLID, objLIDConnection;
            String newPath = "";
            String newPathLogical = "";
            for (int i = 0; i < res.size(); i++) {
                mapObj = (Map) res.get(i);
                objPID = (String) mapObj.get("physicalid");
                objPIDConnection = (String) mapObj.get("physicalid[connection]");
                objLID = (String) mapObj.get("logicalid");
                objLIDConnection = (String) mapObj.get("logicalid[connection]");
                objFromPID = (String) mapObj.get("from.physicalid");
                newPath = mapPaths.get(objFromPID);
                newPathLogical = mapPathsLogical.get(objFromPID);
                if (newPath != null && !newPath.isEmpty()) {
                    newPath = newPath + "/" + objPIDConnection;
                    newPathLogical = newPathLogical + "/" + objLIDConnection;
                } else {
                    newPath = objPID;
                    newPathLogical = objLID;
                }
                mapPaths.put(objPID, newPath);
                mapPathsLogical.put(objPID, newPathLogical);

                mapObj.put("pathPID", newPath);
                mapObj.put("pathLID", newPathLogical);
                // set ids to use physicalids
                mapObj.put("id", objPID);
                mapObj.put("id[connection]", objPIDConnection);

                // Add hasChildren info here
                if ((i + 1) < res.size()) {
                    Map nextMapObj = (Map) res.get(i + 1);
                    String strLevelNow = (String) mapObj.get("level");
                    String strLevelNext = (String) nextMapObj.get("level");
                    int levelNow = Integer.parseInt(strLevelNow);
                    int levelNext = Integer.parseInt(strLevelNext);
                    if (levelNext > levelNow) {
                        mapObj.put("hasChildren", "true");
                    } else {
                        mapObj.put("hasChildren", "false");
                        mapObj.put("children", new MapList());
                    }
                } else {
                    // Last object so no childrens
                    mapObj.put("hasChildren", "false");
                    mapObj.put("children", new MapList());
                }
            }

            // END UM5c06 : Build Paths and save theses in the return maps

            // Sort by TreeOrder "attribute[PLMInstance.V_TreeOrder].value"
            res.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");

            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            ContextUtil.abortTransaction(context);
            throw ex;
        }
        long endTime = System.currentTimeMillis();
        System.out.println("FRC PERFOS : getExpandProductStructure (without getVPMStructure) : " + (endTime - startTime));

        if (perfoTraces != null) {
            perfoTraces.write("Time spent in getExpandProductStructure() : " + (endTime - startTime) + " milliseconds");
            perfoTraces.newLine();
            perfoTraces.flush();
        }

        return res;
    }

    public static HashMap getProductConfigurationValue(Context context, String[] args) {

        HashMap retMap = new HashMap();
        StringList slOIDValues = new StringList();
        StringList slDisplayValues = new StringList();
        String result = new String();
        HashMap paramMap = new HashMap();
        // String filterInput= new String();
        // try {
        // result = (String) PropertyUtil.getRPEValue(context, "AppliedProductConfiguration", true); //FRCEffExpr.getProductConfigurationValue(context);
        // } catch (FrameworkException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        try {
            paramMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) paramMap.get("requestMap");
            result = ((String) requestMap.get("productConfigurationApplied"));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (result == null || result.equals(""))
            result = "-none-";
        slOIDValues.add(0, result);
        slDisplayValues.add(0, result);
        retMap.put("field_choices", slOIDValues);
        retMap.put("field_display_choices", slDisplayValues);
        return retMap;

    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getExpandMBOM(Context context, String[] args) throws Exception {// Expand program called by the emxIndentedTable.jsp of the MBOM table
        long startTime = System.currentTimeMillis();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        MapList res = new MapList();

        ContextUtil.startTransaction(context, false);
        String objectId = null;
        try {
            objectId = (String) paramMap.get("objectId");
            String expandLevel = (String) paramMap.get("expandLevel");

            // Add configuration filter
            String filterExpression = (String) paramMap.get("FRCExpressionFilterInput_OID");
            String filterValue = (String) paramMap.get("FRCExpressionFilterInput_actualValue");
            String filterInput = (String) paramMap.get("FRCExpressionFilterInput");

            short expLvl = 0;// Default to Expand All = 0

            // EPI : correction bug when applying a config filter : force to do an Expand All
            // if (!"All".equals(expandLevel))
            // expLvl = Short.parseShort(expandLevel);

            // Call Expand
            res = getExpandMBOM(context, objectId, expLvl, filterExpression, filterValue, filterInput, EXPD_REL_SELECT, EXPD_BUS_SELECT);

            // START UM5c06 : Build Paths and save theses in the return maps
            HashMap<String, String> mapPaths = new HashMap<String, String>();// Store path in a Map to be able to manage unsorted return MapList

            DomainObject domObj = new DomainObject(objectId);
            String rootPID = domObj.getInfo(context, "physicalid");
            mapPaths.put(rootPID, rootPID);

            // Declare variable before to improve prefs
            Map<String, Object> mapObj;
            String objPID, objFromPID, objPIDConnection;
            String newPath = "";
            for (int i = 0; i < res.size(); i++) {
                mapObj = (Map) res.get(i);
                objPID = (String) mapObj.get("physicalid");
                objPIDConnection = (String) mapObj.get("physicalid[connection]");
                objFromPID = (String) mapObj.get("from.physicalid");
                newPath = mapPaths.get(objFromPID);
                if (newPath != null && !newPath.isEmpty()) {
                    newPath = newPath + "/" + objPIDConnection;
                } else {
                    newPath = objPID;
                }
                mapPaths.put(objPID, newPath);
                mapObj.put("pathPID", newPath);

                // set ids to use physicalids
                mapObj.put("id", objPID);
                mapObj.put("id[connection]", objPIDConnection);

                // Add hasChildren info here
                if ((i + 1) < res.size()) {
                    Map nextMapObj = (Map) res.get(i + 1);
                    String strLevelNow = (String) mapObj.get("level");
                    String strLevelNext = (String) nextMapObj.get("level");
                    int levelNow = Integer.parseInt(strLevelNow);
                    int levelNext = Integer.parseInt(strLevelNext);
                    if (levelNext > levelNow) {
                        mapObj.put("hasChildren", "true");
                    } else {
                        mapObj.put("hasChildren", "false");
                        mapObj.put("children", new MapList());
                    }
                } else {
                    // Last object so no childrens
                    mapObj.put("hasChildren", "false");
                    mapObj.put("children", new MapList());
                }
            }
            // END UM5c06 : Build Paths and save theses in the return maps

            // Sort by TreeOrder "attribute[PLMInstance.V_TreeOrder].value"
            res.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");

            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            ex.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw ex;
        }
        long endTime = System.currentTimeMillis();
        System.out.println("FRC PERFOS : getExpandMBOM (without getVPMStructure) : " + (endTime - startTime));

        if (perfoTraces != null) {
            perfoTraces.write("Time spent in getExpandMBOM() : " + (endTime - startTime) + " milliseconds");
            perfoTraces.newLine();
            perfoTraces.flush();
        }

        return res;
    }

    @com.matrixone.apps.framework.ui.CreateProcessCallable
    public static void removeManufItem(Context context, String[] args) throws Exception { // Called by FRCRemoveManufItemPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String instancePIDs = (String) programMap.get("relId");

            // START UM5 : Manage Multiple items
            String[] arrInstPID = instancePIDs.split("\\|", -2);

            for (String instancePID : arrInstPID) {
                FRCMBOMModelerUtility.deleteMBOMInstance(context, plmSession, instancePID);
            }
            // END UM5 : Manage Multiple items

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static void replaceByExistingRevision(Context context, String[] args) throws Exception { // Called by FRCReplaceByRevisionPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomPath = args[0];
            String newRefPID = args[1];

            String[] mbomPathList = mbomPath.split("/");

            FRCMBOMModelerUtility.replaceMBOMInstance(context, plmSession, mbomPathList[mbomPathList.length - 1], newRefPID);

            flushAndCloseSession(plmSession);
            // HE5 : Added fix for the error "Transaction not active" after calling the R&D API replaceMBOMInstance
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

    }

    public Vector getReorderColumn(Context context, String[] args) throws Exception { // Called from table FRCMBOMTable (column FRCMBOMCentral.MBOMTableColumnReorder)
        long startTime = System.currentTimeMillis();

        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        MapList objList = (MapList) programMap.get("objectList");

        Vector<String> columnVals = new Vector<String>();

        HashMap columnMap = (HashMap) programMap.get("columnMap");
        HashMap settings = (HashMap) columnMap.get("settings");

        String sReorderProgram = (String) settings.get("Reorder Program");
        String sReorderFunction = (String) settings.get("Reorder Function");

        // Write drag and drop functions that will be repeatedly used
        String jsFctDragStart = "var tile=event.target;" + "var info={" + "\"relId\":tile.parentElement.getAttribute(\"r\")," + "\"rowId\":tile.parentElement.getAttribute(\"rowId\"),"
                + "\"dndType\":\"Reorder\"" + "};" + "event.dataTransfer.setData(\"text\",JSON.stringify(info)); top.FRCdata=JSON.stringify(info);" // Modif AFN - Move/Reorder bug on chrome
                + "var relIdTile=tile.parentElement.getAttribute(\"r\");" + "setTimeout(function(){$(\".dragReorder\").css(\"display\",\"none\");"
                + "$(\".dropReorder[r!=\\\"\"+relIdTile+\"\\\"]\").css(\"display\",\"inline-block\");},10);";// Timeout used to work around a bug in google chrome where dragend is fired if DOM is
                                                                                                             // modified in dragstart

        String jsFctDragEnd = "$(\".dragReorder\").css(\"display\",\"inline-block\");" + "$(\".dropReorder\").css(\"display\",\"none\");top.FRCdata=\"\";";

        String jsFctDragEnterOver = "var tile=event.target;" + "var data=event.dataTransfer.getData(\"text\");" + "if (\"\"==data || null == data) data=top.FRCdata; var info=JSON.parse(data);" // Modif
                                                                                                                                                                                                 // AFN
                                                                                                                                                                                                 // -
                                                                                                                                                                                                 // Move/Reorder
                                                                                                                                                                                                 // bug
                                                                                                                                                                                                 // on
                                                                                                                                                                                                 // chrome
                + "if((info.dndType===\"Reorder\") &amp;&amp; (hasSameScope(info.rowId, tile.parentElement.getAttribute(\"rowId\"), tile.getAttribute(\"tiletype\")) || hasSameParent(info.rowId, tile.parentElement.getAttribute(\"rowId\"), tile.getAttribute(\"tiletype\"))) ){" // Modif
                                                                                                                                                                                                                                                                                    // AFN
                                                                                                                                                                                                                                                                                    // -
                                                                                                                                                                                                                                                                                    // Move/Reorder
                + "tile.style.backgroundColor=\"#9DE69D\";" + "event.preventDefault();" + "return true;" + "}else{" + "tile.style.backgroundColor=\"#FF7B59\";" // Modif AFN - Move/Reorder
                + "event.preventDefault();" + "return false;" + "}";

        String jsFctDragLeave = "var tile=event.target;tile.style.backgroundColor=\"\";";

        String jsFctDrop = "var tile=event.target;" + "tile.style.backgroundColor=\"\";" + "var data=event.dataTransfer.getData(\"text\");" + "var info=JSON.parse(data);"
                + "if((info.dndType===\"Reorder\") &amp;&amp; (hasSameScope(info.rowId, tile.parentElement.getAttribute(\"rowId\"), tile.getAttribute(\"tiletype\")) || hasSameParent(info.rowId, tile.parentElement.getAttribute(\"rowId\"), tile.getAttribute(\"tiletype\"))) ){" // Modif
                                                                                                                                                                                                                                                                                    // AFN
                                                                                                                                                                                                                                                                                    // -
                                                                                                                                                                                                                                                                                    // Move/Reorder
                + "window.open(\"../FRCMBOMCentral/FRCReorder.jsp?reorderProg=" + sReorderProgram + "&amp;reorderFct=" + sReorderFunction
                + "&amp;relIdSource=\"+info.relId+\"&amp;relIdTarget=\"+tile.parentElement.getAttribute(\"r\")+\"&amp;rowIdSource=\"+info.rowId+\"&amp;rowIdTarget=\"+tile.parentElement.getAttribute(\"rowId\")+\"&amp;mode=\"+tile.getAttribute(\"tiletype\"),\"listHidden\");"
                + "event.preventDefault();top.FRCdata=\"\";" + "return true;" + "}else{" + "event.preventDefault();top.FRCdata=\"\";" + "return false;" + "}"; // End Modif AFN - Move/Reorder

        for (Object obj : objList) {
            String oid = (String) ((Map) obj).get("id");
            String relId = (String) ((Map) obj).get("id[connection]");
            // String parentOid = (String) ((Map) obj).get("id[parent]");
            String rowId = (String) ((Map) obj).get("id[level]");

            boolean dragOk = true;
            boolean dropBellowAboveOk = true;
            boolean dropAsChildOk = true;

            StringBuilder htmlOut = new StringBuilder();
            htmlOut.append(
                    "<div class='placeHolderForSize' style='height:0px;width:65px;display:block;'></div><div class='placeHolderForHeight' style='height:18px;width:0px;display:inline-block;'></div>");

            if (relId != null && !relId.isEmpty() && dragOk) {
                htmlOut.append("<div class='dragReorder' r='");
                htmlOut.append(relId);
                htmlOut.append("' o='");
                htmlOut.append(oid);
                htmlOut.append("' rowId='");
                htmlOut.append(rowId);
                htmlOut.append("' style='display:inline-block;'>");
                // Put DnD drag Div
                htmlOut.append("<div id='TileMove_");
                htmlOut.append(relId);
                htmlOut.append(
                        "' style='display:inline-block;cursor:move;width:18px;height:18px;overflow:hidden;background-image:url(\"../FRCMBOMCentral/images/iconDragDrop.png\");background-size:18px 18px;'");
                htmlOut.append(" draggable='true'");
                htmlOut.append(" ondragstart='");
                htmlOut.append(jsFctDragStart);
                htmlOut.append("' ondragend='");
                htmlOut.append(jsFctDragEnd);
                htmlOut.append("'></div>");
                // Close div dragReorder
                htmlOut.append("</div>");
            }

            htmlOut.append("<div class='dropReorder' r='");
            htmlOut.append(relId);
            htmlOut.append("' o='");
            htmlOut.append(oid);
            htmlOut.append("' rowId='");
            htmlOut.append(rowId);
            htmlOut.append("' style='display:none;height:18px;'>");
            // Put Drop Divs (drop bellow, above or as child)
            if (relId != null && !relId.isEmpty()) {
                // Drop Above or drop bellow OK here (not a root object)
                if (dropBellowAboveOk) {
                    htmlOut.append("<div id='TileDropAbove_");
                    htmlOut.append(relId);
                    htmlOut.append(
                            "' tiletype='DropAbove' title='Attach Above' style='display:inline-block;height:18px;width:18px;margin-right:4px;background-image:url(\"../FRCMBOMCentral/images/iconDropAbove_48px.png\");background-size:18px 18px;' ");
                    htmlOut.append("ondragenter='");
                    htmlOut.append(jsFctDragEnterOver);
                    htmlOut.append("' ");
                    htmlOut.append("ondragover='");
                    htmlOut.append(jsFctDragEnterOver);
                    htmlOut.append("' ");
                    htmlOut.append("ondragleave='");
                    htmlOut.append(jsFctDragLeave);
                    htmlOut.append("' ");
                    htmlOut.append("ondrop='");
                    htmlOut.append(jsFctDrop);
                    htmlOut.append("'");
                    htmlOut.append("></div>");

                    htmlOut.append("<div id='TileDropBellow_");
                    htmlOut.append(relId);
                    htmlOut.append(
                            "' tiletype='DropBellow' title='Attach Bellow' style='display:inline-block;height:18px;width:18px;margin-right:4px;background-image:url(\"../FRCMBOMCentral/images/iconDropBellow_48px.png\");background-size:18px 18px;' ");
                    htmlOut.append("ondragenter='");
                    htmlOut.append(jsFctDragEnterOver);
                    htmlOut.append("' ");
                    htmlOut.append("ondragover='");
                    htmlOut.append(jsFctDragEnterOver);
                    htmlOut.append("' ");
                    htmlOut.append("ondragleave='");
                    htmlOut.append(jsFctDragLeave);
                    htmlOut.append("' ");
                    htmlOut.append("ondrop='");
                    htmlOut.append(jsFctDrop);
                    htmlOut.append("'");
                    htmlOut.append("></div>");
                }

                // Drop as child
                if (dropAsChildOk) {
                    htmlOut.append("<div id='TileDropAsChild_");
                    htmlOut.append(relId);
                    htmlOut.append(
                            "' tiletype='DropAsChild' title='Attach as Child' style='display:inline-block;height:18px;width:18px;background-image:url(\"../FRCMBOMCentral/images/iconDropAsChild_48px.png\");background-size:18px 18px;' ");
                    htmlOut.append("ondragenter='");
                    htmlOut.append(jsFctDragEnterOver);
                    htmlOut.append("' ");
                    htmlOut.append("ondragover='");
                    htmlOut.append(jsFctDragEnterOver);
                    htmlOut.append("' ");
                    htmlOut.append("ondragleave='");
                    htmlOut.append(jsFctDragLeave);
                    htmlOut.append("' ");
                    htmlOut.append("ondrop='");
                    htmlOut.append(jsFctDrop);
                    htmlOut.append("'");
                    htmlOut.append("></div>");
                }
            }

            // Close div dropReorder
            htmlOut.append("</div>");

            // Add Drop Zones to create MBOM Links
            htmlOut.append("<div class='dropMBOMLink' o='");
            htmlOut.append(oid);
            htmlOut.append("' rowId='");
            htmlOut.append(rowId);
            htmlOut.append("' style='display:none;height:18px;'>");
            // if (relId != null && !relId.isEmpty()) {
            DomainObject domMBOM = new DomainObject(oid);

            boolean canDropFromPS = false;
            for (String typeInList : baseTypesForMBOMLeafNodes) {
                if (domMBOM.isKindOf(context, typeInList))
                    canDropFromPS = true;
            }
            for (String typeInList : baseTypesForMBOMAssemblyNodes) {
                if (domMBOM.isKindOf(context, typeInList))
                    canDropFromPS = true;
            }
            boolean canDropFromMBOM = false;

            htmlOut.append("<div id='TileDropMBOMLink_");
            htmlOut.append(relId);
            htmlOut.append(
                    "' tiletype='DropMBOMLink' title='Attach as Child' style='display:inline-block;height:18px;width:18px;background-image:url(\"../FRCMBOMCentral/images/I_DELPPRAssignmentPanelCmd.png\");background-size:18px 18px;' ");
            htmlOut.append("ondragenter='FRCMBOMDragOver(event, " + canDropFromPS + ", " + canDropFromMBOM + ");' ");
            htmlOut.append("ondragover='FRCMBOMDragOver(event, " + canDropFromPS + ", " + canDropFromMBOM + ");' ");
            htmlOut.append("ondragleave='FRCMBOMDragLeave(event);' ");
            htmlOut.append("ondrop='FRCMBOMDrop(event, \"" + oid + "\", \"" + rowId + "\");'");
            htmlOut.append("></div>");
            // }
            // resultSB.append("<div draggable=\"true\" ondragstart=\"FRCDragStart(event, '" + rowID + "', 'MBOM')\" ondrop=\"FRCMBOMDrop(event, '" + objectID + "', '" + rowID + "')\"
            // ondragover=\"FRCMBOMDragOver(event, " + canDropFromPS + ", " + canDropFromMBOM + ")\">");

            htmlOut.append("</div>");

            // Add elements to the line
            columnVals.add(htmlOut.toString());
        }

        long endTime = System.currentTimeMillis();
        if (perfoTraces != null) {
            perfoTraces.write("Time spent in getReorderColumn() : " + (endTime - startTime) + " milliseconds");
            perfoTraces.newLine();
            perfoTraces.flush();
        }

        return columnVals;
    }

    public static String reorderMBOM(Context context, String[] args) throws Exception {// Called by FRCReorder.jsp
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sRet = "";

        String mode = (String) programMap.get("mode");
        String relIdSource = (String) programMap.get("relIdSource");
        String relIdTarget = (String) programMap.get("relIdTarget");

        // Code to reorder the structure
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Get Rels Infos
            DomainRelationship drSrc = DomainRelationship.newInstance(context, relIdSource);
            DomainRelationship drTrg = DomainRelationship.newInstance(context, relIdTarget);

            StringList relSel = new StringList();
            relSel.add("from.id");
            relSel.add("to.id");
            relSel.add("id[connection]");
            relSel.add("name[connection]");
            relSel.add("attribute[PLMInstance.V_TreeOrder].value"); // Not needed ????

            Map mapRelSrc = drSrc.getRelationshipData(context, relSel);
            Map mapRelTrg = drTrg.getRelationshipData(context, relSel);

            String idFromSrc = (String) ((StringList) mapRelSrc.get("from.id")).get(0);
            // String idToSrc = (String) ((StringList)mapRelSrc.get("to.id")).get(0);

            String idFromTrg = (String) ((StringList) mapRelTrg.get("from.id")).get(0);
            String idToTrg = (String) ((StringList) mapRelTrg.get("to.id")).get(0);

            String m1IdRelTrg = (String) ((StringList) mapRelTrg.get("id[connection]")).get(0);// To be sure to have a matrix Id in case we have a physicalid in input

            DomainObject domParent = null;
            DomainRelationship domRel = null;
            String instToReorderID = relIdSource;
            if ((idFromTrg.equals(idFromSrc) && (mode.equals("DropAbove") || mode.equals("DropBellow"))) || (idToTrg.equals(idFromSrc) && mode.equals("DropAsChild"))) {
                sRet = "reorder";

                // Same parent do a simple modification of V_TreeOrder
                domParent = new DomainObject(idFromSrc);
                domRel = drSrc;
            } else {
                // Call specific function of FRC for restructuration
                sRet = "move";
                String[] params = new String[2];
                params[0] = relIdSource;
                if (mode.equals("DropAsChild")) {
                    params[1] = idToTrg;
                    domParent = new DomainObject(idToTrg);
                } else {
                    params[1] = idFromTrg;
                    domParent = new DomainObject(idFromTrg);
                }
                String sNewInstPID = restructureInstance(context, plmSession, params);
                if (null != sNewInstPID && !"".equals(sNewInstPID)) {
                    domRel = DomainRelationship.newInstance(context, sNewInstPID);
                    instToReorderID = sNewInstPID;
                }
            }

            // Then compute V_TreeOrder
            if (null != domParent && null != domRel) {
                StringList slType = new StringList(new String[] { "id", "type" });
                StringList slRel = new StringList(new String[] { "id[connection]", "name[connection]", "attribute[PLMInstance.V_TreeOrder].value", "physicalid" });

                // Get childs 1 level down only
                MapList res = domParent.getRelatedObjects(context, "PLMInstance", "*", slType, slRel, false, true, (short) 1, "", "", 0);

                res.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");

                double newTreeOrder = -1.0;

                List<String> reoderInstPIDList = new ArrayList<String>();

                if (mode.equals("DropAsChild")) {
                } else {
                    for (int i = 0; i < res.size(); i++) {
                        Map mapChild = (Map) res.get(i);
                        String relChildId = (String) mapChild.get("id[connection]");
                        if (relChildId.equals(m1IdRelTrg)) {
                            if (mode.equals("DropBellow")) {
                                reoderInstPIDList.add(relChildId);
                                reoderInstPIDList.add(instToReorderID);
                                if (i < res.size() - 1)
                                    reoderInstPIDList.add((String) ((Map) res.get(i + 1)).get("id[connection]"));
                            } else { // Drop Above
                                if (i > 0) {
                                    if (!((String) ((Map) res.get(0)).get("physicalid")).equals(instToReorderID)) {
                                        reoderInstPIDList.add((String) ((Map) res.get(0)).get("id[connection]"));
                                    }
                                }
                                reoderInstPIDList.add(instToReorderID);
                                reoderInstPIDList.add(relChildId);
                            }
                            break;
                        }
                    }
                }

                // Construct the full Chil Instance List in the expected order
                List<String> reoderInstPIDFullList = new ArrayList<String>();

                for (Iterator itr = res.iterator(); itr.hasNext();) {
                    Map curMap = (Map) itr.next();

                    if (instToReorderID.equals((String) curMap.get("physicalid"))) {
                        reoderInstPIDFullList.add((String) curMap.get("physicalid"));
                    } else {
                        reoderInstPIDFullList.add((String) curMap.get("id[connection]"));
                    }
                }

                int pos = reoderInstPIDList.indexOf(instToReorderID);
                int sizelist = reoderInstPIDList.size();
                if (pos == 0) {
                    reoderInstPIDFullList.remove(instToReorderID);
                    reoderInstPIDFullList.add(0, instToReorderID);
                } else if (pos == (sizelist - 1)) {
                    reoderInstPIDFullList.remove(instToReorderID);
                    reoderInstPIDFullList.add(instToReorderID);
                } else {
                    reoderInstPIDFullList.remove(instToReorderID);
                    int posInsert = reoderInstPIDFullList.indexOf(reoderInstPIDList.get(0));
                    reoderInstPIDFullList.add(posInsert + 1, instToReorderID);
                }

                System.out.println("newTreeOrder=" + Double.toString(newTreeOrder));
                if (reoderInstPIDList.size() > 0) {
                    FRCMBOMModelerUtility.reorderMBOMInstance(context, plmSession, instToReorderID, reoderInstPIDFullList);
                } else if ("reorder".equals(sRet))
                    sRet = ""; // In case of reorder, nothing has been changed into the structure
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            sRet = "";
            e.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw e;
        }
        return sRet;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getMBOMFromEBOM(Context context, String[] args) { // Called from command FRCEBOMMBOMTableCmd
        MapList retList = new MapList();
        boolean bTrans = false;

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            bTrans = true;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");
                MapList mlPrd = getProductFromEBOM(context, sId);

                if (null != mlPrd) {
                    for (int i = 0; i < mlPrd.size(); i++) {
                        Map mObj = (Map) mlPrd.get(i);
                        String sPhysId = (String) mObj.get("physicalid");
                        List<String> lMBOM = FRCMBOMModelerUtility.getScopingReferencesFromList(context, plmSession, sPhysId);

                        // StringList lMBOM = getMBOMScopePIDListFromPS(context, sPhysId);
                        for (int j = 0; j < lMBOM.size(); j++) {
                            HashMap mNewObj = new HashMap();
                            mNewObj.put(DomainConstants.SELECT_ID, lMBOM.get(j));
                            retList.add(mNewObj);
                        }
                    }
                }
            }
            if (bTrans) {
                closeSession(plmSession);
                ContextUtil.commitTransaction(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (bTrans) {
                closeSession(plmSession);
                ContextUtil.abortTransaction(context);
            }
        }

        return retList;
    }

    public String mbomCreationMethodSelect(Context context, String[] args) throws Exception { // Called from form FRCCreateMBOM (field FRCMBOMManufItemCreateAndPropertiesForm)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map requestMap = (Map) programMap.get("requestMap");
        String strLanguage = (String) requestMap.get("languageStr");
        String sPartId = (String) requestMap.get("objectId");
        StringBuffer sb = new StringBuffer();
        Locale lLang = new Locale(strLanguage);
        String sSelectMBOM = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FRCMBOMCentral.CreateMBOM.SelectMBOM", lLang);

        sb.append("<input type=\"radio\" name=\"selmbomtype\" value=\"CopyMBOM\" ></input>");
        sb.append("<input type=\"text\" readonly=\"readonly\" name=\"txtMBOM\" value=\"");
        sb.append(XSSUtil.encodeForXML(context, sSelectMBOM));
        sb.append("\"></input>");
        sb.append("<input type=\"button\" name=\"btnMBOM\" value=\"...\" onclick= \"showCreateAssemblyChooser()\"></input>");
        sb.append("<input type=\"hidden\" name=\"rootMbom\" value=\"\" ></input>");
        sb.append("<input type=\"hidden\" name=\"rootMbomOID\" value=\"\" ></input>");
        sb.append("<br></br>");

        sb.append("<input type=\"radio\" name=\"selmbomtype\" value=\"FromEBOM\"  ></input>");
        String sFromEBOM = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FRCMBOMCentral.CreateMBOM.FromEBOM", lLang);
        sb.append(sFromEBOM);
        sb.append("<br></br>");

        sb.append("<input type=\"radio\" name=\"selmbomtype\" value=\"NewAssembly\" checked=\"checked\"> </input>");
        String sNewAssembly = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FRCMBOMCentral.CreateMBOM.NewAssembly", lLang);
        sb.append(sNewAssembly);
        sb.append("<br></br>");

        // Add validation method that will be launch on form submit button
        sb.append("<script language=\"javascript\">");
        sb.append("assignValidateMethod('rootMbomOID', 'validateCreationMethod');");

        // Initialize Title with V6 product Title
        MapList mlPrd = getProductFromEBOM(context, sPartId);
        String sTitle = "";
        if (null != mlPrd && 0 < mlPrd.size()) {
            Map mObj = (Map) mlPrd.get(0);
            sTitle = (String) mObj.get("attribute[PLMEntity.V_Name]");
        }
        if (!"".equals(sTitle)) {
            sb.append("document.forms[0].Title.value='");
            sb.append(sTitle);
            sb.append("'");
        }
        sb.append("</script>");

        return sb.toString();
    }

    public String mbomCreateChange(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (empty field after
                                                                                      // FRCMBOMCentral.Form.Label.ChangeObject) and form FRCCreateMBOM (empty field after
                                                                                      // FRCMBOMCentral.Form.Label.ChangeObject)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map requestMap = (Map) programMap.get("requestMap");
        String strLanguage = (String) requestMap.get("languageStr");
        String forceChange = (String) requestMap.get("forceSelectChange");
        StringBuffer sb = new StringBuffer();
        Locale lLang = new Locale(strLanguage);
        String sOpenChangeTitle = EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FRCMBOMCentral.MBOMOpenCA", lLang);
        String sCRTitle = EnoviaResourceBundle.getProperty(context, "EnterpriseChangeMgt", "EnterpriseChangeMgt.Command.CreateChangeRequest", lLang);
        String sCOTitle = EnoviaResourceBundle.getProperty(context, "EnterpriseChangeMgt", "EnterpriseChangeMgt.Command.CreateChange", lLang);

        sb.append("<button name=\"btnOpenChange\" title=\"" + sOpenChangeTitle + "\" onclick= \"showChangeinCreateAssembly()\">");
        sb.append("<img src='../FRCMBOMCentral/images/OpenChangeOrderLarge.png'/>");
        sb.append("</button>");

        if (null != forceChange)
            sb.append("<button name=\"btnCreateCR\" title=\"" + sCRTitle + "\" onclick= \"createCRinCreateMBOM()\">");
        else
            sb.append("<button name=\"btnCreateCR\" title=\"" + sCRTitle + "\" onclick= \"createCRinCreateAssembly()\">");
        sb.append("<img src='../FRCMBOMCentral/images/CreateNewChangeRequestLarge.png'/>");
        sb.append("</button>");

        if (null != forceChange)
            sb.append("<button name=\"btnCreateCO\" title=\"" + sCOTitle + "\" onclick= \"createCOinCreateMBOM()\">");
        else
            sb.append("<button name=\"btnCreateCO\" title=\"" + sCOTitle + "\" onclick= \"createCOinCreateAssembly()\">");
        sb.append("<img src='../FRCMBOMCentral/images/CreateNewChangeOrderLarge.png'/>");
        sb.append("</button>");

        return sb.toString();
    }

    public static Map createMBOMFromEBOM(Context context, String[] args) throws Exception {// Called from command FRCCreateMBOM
        Map returnMap = new HashMap();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        String sCreationMode = (String) programMap.get("selmbomtype"); // Possible values : CopyMBOM - FromEBOM - NewAssembly

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String newObjID = "";
            if ("NewAssembly".equals(sCreationMode)) {
                newObjID = createMBOMFromEBOMEmptyStructure(context, plmSession, args);
            } else if ("CopyMBOM".equals(sCreationMode)) {
                newObjID = createMBOMFromEBOMFromTemplate(context, plmSession, args);
            } else if ("FromEBOM".equals(sCreationMode)) {
                // newObjID = createMBOMFromEBOMLikePS(context, plmSession, args);
                newObjID = createMBOMFromEBOMLikePS_new(context, plmSession, args);
            }

            if (null != newObjID && !"".equals(newObjID))
                returnMap.put("id", newObjID);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context); if (true) throw new Exception("EPI END.");
        } catch (Exception e) {
            e.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw e;
        }

        return returnMap;
    }

    public static String createMBOMFromEBOMLikePS_new(Context context, PLMCoreModelerSession plmSession, String[] args) throws Exception {
        long startTime = System.currentTimeMillis();

        String rootRefPID = "";

        FRCMBOMModelerUtility.checkValidScenario(context);
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sPartId = (String) programMap.get("objectId");

        valueEnvAttachModel = System.getenv("DISABLE_ATTACH_MODEL_ON_SCOPE");

        // Get the change object
        String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
        // Modif AFN - Test if a value has been defined into the creation web form
        String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
        if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
            changeObjectName = changeObjectFromForm;

        MapList mlPrd = getProductFromEBOM(context, sPartId);
        if (null != mlPrd) {
            if (1 < mlPrd.size())
                throw new Exception("Several VPM Products have been found for the given EBOM part");
            if (mlPrd.size() == 0)
                throw new Exception("No VPM Products have been found for the given EBOM part. Please do a \"Collaborate with Physical\".");
            Map mPrd = (Map) mlPrd.get(0);
            String sPrdPhysId = (String) mPrd.get("physicalid");
            if (null != sPrdPhysId && !"".equals(sPrdPhysId)) {
                Map<String, List<String>> workingInfo = new HashMap<String, List<String>>();
                workingInfo.put("instanceToCreate_parentRefPLMID", new ArrayList<String>());
                workingInfo.put("instanceToCreate_childRefPLMID", new ArrayList<String>());
                workingInfo.put("mbomLeafInstancePIDList", new ArrayList<String>());
                workingInfo.put("psPathList", new ArrayList<String>());
                workingInfo.put("newRefPIDList", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_MBOMRefPIDs", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_MBOMRefPLMIDs", new ArrayList<String>());
                workingInfo.put("newScopesToCreate_PSRefPIDs", new ArrayList<String>());

                List<Map<String, String>> workingInfo_instanceAttributes = new ArrayList<Map<String, String>>();
                List<Integer> workingInfo_indexInstancesForImplement = new ArrayList<Integer>();
                Set<String> workingInfo_lModelListOnStructure = new HashSet<String>();
                Map<String, String> workingInfo_AppDateToValuate = new HashMap<String, String>();

                // Recursively process the PS root node and create the MBOM references
                String sRet = createMBOMFromEBOMLikePSRecursive_new(context, plmSession, null, sPrdPhysId, null, workingInfo, workingInfo_lModelListOnStructure, workingInfo_instanceAttributes,
                        workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate);// newRefPIDList, newScopesToCreate_MBOMRefPIDs, newScopesToCreate_PSRefPIDs);

                flushSession(plmSession);

                rootRefPID = PLMID.buildFromString(sRet).getPid();

                // Valuate the V_ApplicabilityDate attributes
                for (String refPID : workingInfo_AppDateToValuate.keySet()) {
                    MqlUtil.mqlCommand(context, "mod bus " + refPID + " PLMReference.V_ApplicabilityDate '" + workingInfo_AppDateToValuate.get(refPID) + "'", false, false);
                }

                // Create all the MBOM instances in one shot
                List<String> allCreatedInstancesPIDList = new ArrayList<String>();
                workingInfo.put("mbomLeafInstancePIDList", createInstanceBulk(context, plmSession, workingInfo.get("instanceToCreate_parentRefPLMID"),
                        workingInfo.get("instanceToCreate_childRefPLMID"), workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, allCreatedInstancesPIDList));

                flushSession(plmSession);

                // MBO-164-MBOM performance issue:START-H65 15/11/2017
                for (String instancePID : allCreatedInstancesPIDList) {
                    MqlUtil.mqlCommand(context, "mod connection " + instancePID + " add interface FRCCustoExtension1", false, false);
                }
                // MBO-164-Below MBOM performance issue:END-H65 15/11/2017

                // In the list of models to attach to the root, add the models attached to the root PS
                workingInfo_lModelListOnStructure.addAll(FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId));

                // Attach all Models to the Root
                if (workingInfo_lModelListOnStructure.size() > 0) {
                    List lBOMRef = new ArrayList<List>();
                    lBOMRef.add(rootRefPID);
                    FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, lBOMRef, new ArrayList<String>(workingInfo_lModelListOnStructure));
                }

                // Create all the scope links in one shot
                FRCMBOMModelerUtility.createScopeLinkBulk(context, plmSession, workingInfo.get("newScopesToCreate_MBOMRefPIDs"), workingInfo.get("newScopesToCreate_PSRefPIDs"));

                flushSession(plmSession);

                // Create all the implement links in one shot
                String retStr = FRCMBOMModelerUtility.setImplementLinkBulk(context, plmSession, workingInfo.get("mbomLeafInstancePIDList"), workingInfo.get("psPathList"), true);

                if (!"".equals(retStr))
                    throw new Exception(retStr);

                // Attach all created references to change object.
                attachListObjectsToChange(context, plmSession, changeObjectName, workingInfo.get("newRefPIDList"));

            }
        } else {
            throw new Exception("No VPM Product found for the given EBOM part");
        }

        long endTime = System.currentTimeMillis();
        System.out.println("EPI PERFOS : createMBOMFromEBOMLikePS_new = " + (endTime - startTime));

        return rootRefPID;
    }

    public static String createMBOMFromEBOMLikePSRecursive_new(Context context, PLMCoreModelerSession plmSession, String mbomParentRefPLMID, String psRefID, String psCompletePath,
            Map<String, List<String>> workingInfo, Set<String> workingInfo_lModelListOnStructure, List<Map<String, String>> workingInfo_instanceAttributes,
            List<Integer> workingInfo_indexInstancesForImplement, Map<String, String> workingInfo_AppDateToValuate) throws Exception {
        String newMBOMRefPLMID = null;
        String newMBOMRefPID = null;

        // Get all the first level instances of the PS reference
        StringList busSelect = new StringList();
        busSelect.add("physicalid");
        StringList relSelect = new StringList();
        relSelect.add("physicalid[connection]");

        // Bug #231 - DCP - START
        relSelect.add("attribute[PLMInstance.V_TreeOrder].value");

        MapList psInstList = FRCMBOMModelerUtility.getVPMStructure(context, plmSession, psRefID, busSelect, relSelect, (short) 1, null, null); // Expand first level

        psInstList.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");
        // Bug #231 - DCP - END
        if (psInstList.size() == 0 && mbomParentRefPLMID != null && !"".equals(mbomParentRefPLMID)) {
            // This is a leaf node of the PS (and it is not the root) : create a Provide under the MBOM path, with implement link and effectivity

            setImplementLinkProcess_new(context, plmSession, mbomParentRefPLMID, psCompletePath, workingInfo, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement,
                    workingInfo_AppDateToValuate);
        } else {
            // This is an intermediate node of the PS (and it is not the root) : insert a new CreateAssembly under the MBOM reference, and process recursively for each child instance

            // Get the attribute values of the PS reference
            DomainObject psRefObj = new DomainObject(psRefID);
            Map psRefAttributes = psRefObj.getAttributeMap(context, true);
            HashMap<String, String> mbomRefAttributes = new HashMap<String, String>();
            mbomRefAttributes.put("PLMEntity.V_Name", (String) psRefAttributes.get("PLMEntity.V_Name"));
            mbomRefAttributes.put("PLMEntity.V_description", (String) psRefAttributes.get("PLMEntity.V_description"));

            // Create a new MBOM reference
            newMBOMRefPLMID = createMBOMReference_new(context, plmSession, "CreateAssembly", null, mbomRefAttributes, false);
            newMBOMRefPID = PLMID.buildFromString(newMBOMRefPLMID).getPid();

            if (valueEnvAttachModel == null) {
                List<String> lModels = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, psRefID);

                if (null != lModels && 0 < lModels.size()) {
                    workingInfo_lModelListOnStructure.addAll(lModels);
                }
            }

            workingInfo.get("newRefPIDList").add(newMBOMRefPID);

            if (mbomParentRefPLMID == null || "".equals(mbomParentRefPLMID)) { // This is the root node of the PS
                // Set scope and attach model (later)
                workingInfo.get("newScopesToCreate_MBOMRefPIDs").add(newMBOMRefPID);
                workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").add(newMBOMRefPLMID);
                workingInfo.get("newScopesToCreate_PSRefPIDs").add(psRefID);
            } else {
                // Get the attribute values of the instance
                String[] psCompletePathList = psCompletePath.split("/");
                DomainRelationship psInstObj = new DomainRelationship(psCompletePathList[psCompletePathList.length - 1]);
                Map psInstAttributes = psInstObj.getAttributeMap(context, true);
                Map mbomInstAttributes = new HashMap();
                mbomInstAttributes.put("PLM_ExternalID", psInstAttributes.get("PLMInstance.PLM_ExternalID"));
                mbomInstAttributes.put("V_description", psInstAttributes.get("PLMInstance.V_description"));
                // Fixed Bug 231-Tree Ordering
                mbomInstAttributes.put("V_TreeOrder", psInstAttributes.get("PLMInstance.V_TreeOrder"));

                // Create a new instance (later)
                workingInfo.get("instanceToCreate_parentRefPLMID").add(mbomParentRefPLMID);
                workingInfo.get("instanceToCreate_childRefPLMID").add(newMBOMRefPLMID);
                workingInfo_instanceAttributes.add(mbomInstAttributes);
            }

            // Bug #231 - DCP - START
            for (int i = 0; i < psInstList.size(); i++) {
                Map<String, String> psInstInfo = (Map<String, String>) psInstList.get(i);
                if (mbomParentRefPLMID == null || "".equals(mbomParentRefPLMID)) { // This is the root node of the PS
                    createMBOMFromEBOMLikePSRecursive_new(context, plmSession, newMBOMRefPLMID, psInstInfo.get("physicalid"), psRefID + "/" + psInstInfo.get("physicalid[connection]"), workingInfo,
                            workingInfo_lModelListOnStructure, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate);
                } else {
                    createMBOMFromEBOMLikePSRecursive_new(context, plmSession, newMBOMRefPLMID, psInstInfo.get("physicalid"), psCompletePath + "/" + psInstInfo.get("physicalid[connection]"),
                            workingInfo, workingInfo_lModelListOnStructure, workingInfo_instanceAttributes, workingInfo_indexInstancesForImplement, workingInfo_AppDateToValuate);
                }
            }
            // Bug #231 - DCP - END
        }

        return newMBOMRefPLMID;
    }

    public static String setImplementLinkProcess_new(Context context, PLMCoreModelerSession plmSession, String mbomParentRefPLMID, String psCompletePath, Map<String, List<String>> workingInfo,
            List<Map<String, String>> workingInfo_instanceAttributes, List<Integer> workingInfo_indexInstancesForImplement, Map<String, String> workingInfo_AppDateToValuate) throws Exception {
        // Get the PID of the PS leaf reference
        String[] psCompletePathList = psCompletePath.split("/");
        String psLeafInstancePID = psCompletePathList[psCompletePathList.length - 1];
        String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstancePID + " select to.physicalid dump |", false, false);

        // Get the PID of the MBOM leaf reference

        String newMBOMRefPLMID = null;

        // Get existing Provide (or create one if there is none) synched with the leaf PS reference (with a scope link)
        newMBOMRefPLMID = getSynchedScopeMBOMRefFromPSRef_new(context, plmSession, psLeafRefPID, null, workingInfo, workingInfo_AppDateToValuate);

        // Instantiate this Provide under it's parent and replicate all the attributes values on the new instance
        DomainRelationship psInstObj = new DomainRelationship(psCompletePathList[psCompletePathList.length - 1]);
        Map psInstAttributes = psInstObj.getAttributeMap(context, true);
        Map mbomInstAttributes = new HashMap();

        // Create a new instance (later)
        mbomInstAttributes.put("V_TreeOrder", psInstAttributes.get("PLMInstance.V_TreeOrder"));
        mbomInstAttributes.put("V_description", psInstAttributes.get("PLMInstance.V_description"));
        mbomInstAttributes.put("PLM_ExternalID", psInstAttributes.get("PLMInstance.PLM_ExternalID"));

        workingInfo.get("instanceToCreate_parentRefPLMID").add(mbomParentRefPLMID);
        workingInfo.get("instanceToCreate_childRefPLMID").add(newMBOMRefPLMID);
        workingInfo_instanceAttributes.add(mbomInstAttributes);

        String trimmedPSPath = psCompletePath.substring(psCompletePath.indexOf("/") + 1);

        // workingInfo.get("mbomLeafInstancePIDList").add(mbomLeafInstancePID);
        workingInfo_indexInstancesForImplement.add(workingInfo.get("instanceToCreate_parentRefPLMID").size() - 1);
        workingInfo.get("psPathList").add(trimmedPSPath);

        return "1";
    }

    public static String getSynchedScopeMBOMRefFromPSRef_new(Context context, PLMCoreModelerSession plmSession, String psRefPID, String mbomRefPLMID, Map<String, List<String>> workingInfo,
            Map<String, String> workingInfo_AppDateToValuate) throws Exception {
        String returnMBOMRefPLMID = null;
        String returnMBOMRefPID = null;

        // Check if PS reference has a scope
        // !!! FOR DEBUG : force the creation of new Provides !!!
        List<String> mbomRefPLMIDScopedWithPSRefList = FRCMBOMModelerUtility.getScopingReferencesFromList_PLMID(context, plmSession, psRefPID);
        // List<String> mbomRefPIDScopedWithPSRefList = new ArrayList<String>();

        // If no scopes found, check if there are scopes that are pending for creation.
        if (mbomRefPLMIDScopedWithPSRefList.size() == 0) {
            int index = workingInfo.get("newScopesToCreate_PSRefPIDs").indexOf(psRefPID);
            if (index >= 0) {
                returnMBOMRefPID = workingInfo.get("newScopesToCreate_MBOMRefPIDs").get(index);
                String scopedMBOMRefPLMID = workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").get(index);
                mbomRefPLMIDScopedWithPSRefList.add(scopedMBOMRefPLMID);
            }
        }

        if (mbomRefPLMIDScopedWithPSRefList.size() > 1) { // PS reference has multiple MBOM scopes
            // If all the elements of the list are within the same revision family, return the latest revision
            boolean isSameMajorIds = true;
            String lastMajorIdsStr = null;

            for (String refPLMID : mbomRefPLMIDScopedWithPSRefList) {
                String refPID = PLMID.buildFromString(refPLMID).getPid();
                String majorIdsStr = MqlUtil.mqlCommand(context, "print bus " + refPID + " select majorids dump |", false, false);

                if (lastMajorIdsStr != null) {
                    if (!lastMajorIdsStr.equals(majorIdsStr))
                        isSameMajorIds = false;
                }

                lastMajorIdsStr = majorIdsStr;
            }

            if (isSameMajorIds) {
                String[] lastMajorIds = lastMajorIdsStr.split("\\|");

                // Return this MBOM scope
                returnMBOMRefPID = lastMajorIds[lastMajorIds.length - 1];

                // Check if it is a provide
                String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);

                boolean isDirect = false;

                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                        isDirect = true;
                }

                if (!isDirect)
                    throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");
            } else {
                throw new Exception("You cannot provide this part from the EBOM : it has multiple scopes !");
            }
        } else if (mbomRefPLMIDScopedWithPSRefList.size() == 1) { // PS reference has already one MBOM scope
            // Return this MBOM scope
            returnMBOMRefPLMID = mbomRefPLMIDScopedWithPSRefList.get(0);

            if (returnMBOMRefPID == null) { // Else, it means that it is a Provide that was created and scope previously in the process, so nothing more to do.
                returnMBOMRefPID = PLMID.buildFromString(returnMBOMRefPLMID).getPid();

                // Check if it is a provide
                String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);

                // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
                String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
                MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
                String psRefApplicabilityDate = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMReference.V_ApplicabilityDate].value dump |", false, false);
                MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefApplicabilityDate + "'", false, false);

                boolean isDirect = false;

                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                        isDirect = true;
                }

                if (!isDirect)
                    throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");
            }
        } else { // PS reference does not already have an MBOM scope
            // Get the previous revision of the PS reference
            String previousRevPSRefPID = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select previous.physicalid dump |", false, false);

            if ("".equals(previousRevPSRefPID)) { // PS reference does not have a previous revision
                if (mbomRefPLMID == null) { // MBOM reference is null
                    // Create a new Provide and return it
                    HashMap<String, String> attributes = new HashMap<String, String>();
                    String psRefInfoStr = MqlUtil.mqlCommand(context,
                            "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value attribute[PLMEntity.V_description].value attribute[PLMReference.V_ApplicabilityDate].value dump |",
                            false, false);
                    String[] psRefInfo = psRefInfoStr.split("\\|", -2);

                    // String psRefTitle = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value dump |", false, false);
                    String psRefTitle = psRefInfo[0];
                    attributes.put("PLMEntity.V_Name", psRefTitle);

                    // String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
                    String psRefDescription = psRefInfo[1];
                    attributes.put("PLMEntity.V_description", psRefDescription);

                    String psRefAppDate = psRefInfo[2];

                    returnMBOMRefPLMID = createMBOMReference_new(context, plmSession, "Provide", null, attributes, false);
                    returnMBOMRefPID = PLMID.buildFromString(returnMBOMRefPLMID).getPid();

                    if (!"".equals(psRefAppDate))
                        workingInfo_AppDateToValuate.put(returnMBOMRefPID, psRefAppDate);

                    workingInfo.get("newRefPIDList").add(returnMBOMRefPID);

                    // Create a scope link between PS reference and MBOM reference
                    workingInfo.get("newScopesToCreate_MBOMRefPIDs").add(returnMBOMRefPID);
                    workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").add(returnMBOMRefPLMID);
                    workingInfo.get("newScopesToCreate_PSRefPIDs").add(psRefPID);
                } else { // MBOM reference is not null
                    List<String> inputListForGetScope = new ArrayList<String>();
                    inputListForGetScope.add(PLMID.buildFromString(mbomRefPLMID).getPid());
                    String psRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                    if (psRefScopePID != null && !"".equals(psRefScopePID)) { // MBOM reference has already a PS scope : throw a new exception
                        throw new Exception("This MBOM node already has a scope, and it is not the EBOM part you are providing !");
                    } else { // MBOM reference does not already have a scope
                        // Return the MBOM reference
                        returnMBOMRefPLMID = mbomRefPLMID;

                        // Create a scope link between PS reference and MBOM reference (later)
                        workingInfo.get("newScopesToCreate_MBOMRefPIDs").add(returnMBOMRefPID);
                        workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").add(returnMBOMRefPLMID);
                        workingInfo.get("newScopesToCreate_PSRefPIDs").add(psRefPID);
                    }
                }
            } else { // PS reference has a previous revision
                // Recursive call on previous revision (with MBOM reference in parameter)
                String mbomRefPLMIDSynchedToPreviousPSRevision = getSynchedScopeMBOMRefFromPSRef_new(context, plmSession, previousRevPSRefPID, mbomRefPLMID, workingInfo, workingInfo_AppDateToValuate);
                String mbomRefPIDSynchedToPreviousPSRevision = PLMID.buildFromString(mbomRefPLMIDSynchedToPreviousPSRevision).getPid();

                // New revision on the MBOM reference returned by the recursive call and return this new MBOM reference
                // First, check if the session needs to be flushed, because in this particular MBOM creation process, the flushSession() is done at the end.
                try {
                    DomainObject obj = DomainObject.newInstance(context, mbomRefPIDSynchedToPreviousPSRevision);
                } catch (Exception e) {
                    // This object has just been created, and has not yet been flushed down to the M1 transaction.
                    // Do the flush now, so that the new revision will be able to perform...
                    flushSession(plmSession);
                }

                returnMBOMRefPID = newRevisionMBOMReference(context, plmSession, mbomRefPIDSynchedToPreviousPSRevision);
                returnMBOMRefPLMID = FRCMBOMModelerUtility.getPLMIDFromPID(context, plmSession, returnMBOMRefPID);
                workingInfo.get("newRefPIDList").add(returnMBOMRefPID);

                // !! CAREFULL : remove the scope on the new revision of the MBOM reference (by default, the new revision duplicates the scope)
                List<String> modifiedInstanceList = FRCMBOMModelerUtility.deleteScopeLinkAndDeleteChildImplementLinks(context, plmSession, returnMBOMRefPID, false);

                // Create a scope link between the PS reference and the new MBOM reference revision (later)
                workingInfo.get("newScopesToCreate_MBOMRefPIDs").add(returnMBOMRefPID);
                workingInfo.get("newScopesToCreate_MBOMRefPLMIDs").add(returnMBOMRefPLMID);
                workingInfo.get("newScopesToCreate_PSRefPIDs").add(psRefPID);

                // Map the attributes
                String psRefInfoStr = MqlUtil.mqlCommand(context,
                        "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value attribute[PLMEntity.V_description].value attribute[PLMReference.V_ApplicabilityDate].value dump |", false,
                        false);
                String[] psRefInfo = psRefInfoStr.split("\\|", -2);

                if ("".equals(psRefInfo[2]))
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefInfo[0] + "' PLMEntity.V_description '" + psRefInfo[1] + "'", false, false);
                else
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefInfo[0] + "' PLMEntity.V_description '" + psRefInfo[1]
                            + "' PLMReference.V_ApplicabilityDate '" + psRefInfo[2] + "'", false, false);
            }
        }

        return returnMBOMRefPLMID;
    }

    public static String createMBOMReference_new(Context context, PLMCoreModelerSession plmSession, String type, String magnitudeType, Map<String, String> attributes, boolean doFlushSession)
            throws Exception {
        String newObjPLMID = null;

        newObjPLMID = FRCMBOMModelerUtility.createMBOMDiscreteReference_PLMID(context, plmSession, type, attributes);

        if (doFlushSession)
            flushSession(plmSession);

        return newObjPLMID;

    }

    public static List<String> createInstanceBulk(Context context, PLMCoreModelerSession plmSession, List<String> list_parentRefPLMID, List<String> list_childRefPLMID,
            List<Map<String, String>> list_instanceAttributes, List<Integer> workingInfo_indexInstancesForImplement, List<String> allCreatedInstancesPIDList) throws Exception {
        List<String> returnList = new ArrayList<String>();

        int index = 0;
        int nextIndex = -1;
        if (index < workingInfo_indexInstancesForImplement.size())
            nextIndex = workingInfo_indexInstancesForImplement.get(index);

        for (int i = 0; i < list_parentRefPLMID.size(); i++) {
            Hashtable att = new Hashtable();
            att.putAll(list_instanceAttributes.get(i));

            String relPID = FRCMBOMModelerUtility.createMBOMInstanceWithPLMID(context, plmSession, list_parentRefPLMID.get(i), list_childRefPLMID.get(i), att);

            allCreatedInstancesPIDList.add(relPID);

            if (i == nextIndex) {
                returnList.add(relPID);

                index++;
                nextIndex = -1;
                if (index < workingInfo_indexInstancesForImplement.size())
                    nextIndex = workingInfo_indexInstancesForImplement.get(index);
            }
        }

        return returnList;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public static String getRootProductStructure(Context context, String[] args) throws Exception { // Called from emxIndentedTableWrapper.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String rootPID = "";

            String manuItemRefID = args[0];

            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(manuItemRefID);
            String productRefPID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

            if (productRefPID != null && !"".equals(productRefPID)) {
                rootPID = productRefPID;

            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
            return rootPID;
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getEmptyMapListProgram(Context context, String[] args) throws Exception { // Called from emxIndentedTableWrapper.jsp
        // This function is used to display PS Table empty when we don't have a Scope to a PS Item
        return new MapList();
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getRevisionsProgram(Context context, String[] args) throws Exception { // Called from FRCReplaceByRevisionPreProcess2.jsp and FRCUpdateScopePreProcess.jsp
        MapList returnML = new MapList();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectPID = (String) paramMap.get("objectId");

        DomainObject doPSReference = new DomainObject(objectPID);

        StringList busSelect = new StringList();
        busSelect.add("type");
        busSelect.add("name");
        busSelect.add("versionid");
        busSelect.add("id");
        Map resultInfoM = doPSReference.getInfo(context, busSelect);
        String obM1ID = (String) resultInfoM.get("id");

        Query q = new Query();
        q.setBusinessObjectType((String) resultInfoM.get("type"));
        q.setBusinessObjectName((String) resultInfoM.get("name"));
        q.setWhereExpression("versionid==\"" + (String) resultInfoM.get("versionid") + "\"");
        BusinessObjectList listRevisions = q.evaluate(context);

        for (int i = 0; i < listRevisions.size(); i++) {
            BusinessObject rev = (BusinessObject) listRevisions.get(i);
            rev.open(context);
            String busid = rev.getObjectId();
            if (!busid.equals(obM1ID)) {
                Map objMap = new HashMap();
                objMap.put("id", busid);
                returnML.add(objMap);
            }

            rev.close(context);

        }

        /*
         * String revisionPIDListStr = MqlUtil.mqlCommand(context, "print bus " + objectPID + " select majorids.id dump |", false, false); String[] revisionPIDList = revisionPIDListStr.split("\\|");
         * 
         * for (String revisionPID : revisionPIDList) { if (!revisionPID.equals(objectPID)) { Map objMap = new HashMap(); objMap.put("id", revisionPID); returnML.add(objMap); } }
         */
        return returnML;
    }

    public static StringList getProdConfList(Context context, String[] args) throws Exception { // Called by command FRCPSGetProdConfigFilter
        StringList sList = new StringList();

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");
            // String objectId = "19616.52467.29668.34997";
            String objectId = null;

            /*
             * Map valuesMap = (Map) requestMap.get("RequestValuesMap"); if (valuesMap != null) { String[] oidList = (String[]) valuesMap.get("objectId"); if (oidList != null && oidList.length > 0)
             * objectId = oidList[0]; }
             * 
             * if (objectId == null || "".equals(objectId)) objectId = (String) requestMap.get("objectId");
             */
            String parentId = (String) requestMap.get("parentOID");
            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(parentId);
            objectId = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

            String toolbarFilterValue = (String) requestMap.get("toolbarFilterValue");

            String pcListStr = "";
            if (objectId != null && !"".equals(objectId)) {
                pcListStr = MqlUtil.mqlCommand(context, true, "print bus " + objectId + " select from[" + REL_VOWNER + "].to[" + TYP_PRODCONF + "].attribute[" + ATT_PLMEXTID + "].value dump |", true);
            }

            if (!"".equals(pcListStr)) {
                sList = FrameworkUtil.split(pcListStr, "|");
            }

            sList.add(0, "-none-");

            // If a filter value has been given, put it at the beginning of the list
            // (so it appears selected by default in the combobox)
            if (toolbarFilterValue != null && !"".equals(toolbarFilterValue)) {
                sList.remove(toolbarFilterValue);
                sList.add(0, toolbarFilterValue);
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return sList;
    }

    public static Vector getEmptyColumn(Context context, String[] args) throws Exception {
        // Get object list information from packed arguments
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");

        // Create result vector
        Vector vecResult = new Vector();

        for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
            Map mapObjectInfo = (Map) itrObjects.next();

            // String manuItemInstID = (String) mapObjectInfo.get("id[connection]");
            // String manuItemRefID = (String) mapObjectInfo.get("id");

            vecResult.add("");
        }

        return vecResult;
    }

    public static Vector<String> get3DLoadColumnHTML(Context context, String[] args) throws Exception { // Called from table FRCPSTable (column FRCMBOMCentral.PSTableColumn3DShow)
        long startTime = System.currentTimeMillis();

        Vector<String> vect = new Vector();

        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        Map columnMap = (Map) programMap.get("columnMap");
        Map columnSettings = (Map) columnMap.get("settings");
        String structureType = (String) columnSettings.get("structureType");

        MapList relBusObjPageList = (MapList) programMap.get("objectList");

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map curMap = (Map) relBusObjPageList.get(i);
            // String objectId = (String) curMap.get("id");
            // DomainObject domObj = DomainObject.newInstance(context,
            // objectId);

            StringBuffer sbResult = new StringBuffer();

            // if (domObj.isKindOf(context, "MCAD Model") ||
            // domObj.isKindOf(context, "MCAD Assembly") ||
            // domObj.isKindOf(context, "MCAD Component") ||
            // domObj.isKindOf(context, "MCAD Versioned Component") ||
            // domObj.isKindOf(context, "MCAD Versioned Assembly")) {
            String idLevel = (String) curMap.get("id[level]");

            String idForCheckbox = "FRC_CB_" + idLevel;

            // sbResult.append("<div
            // style='width:100%;height:10px;text-align:center;'>");
            sbResult.append("<div style='height:0px;text-align:center;'>");
            sbResult.append("<div class='FRCRoundedCheckbox' style='display:inline-block'>");
            sbResult.append("<input type='checkbox' style='visibility:hidden' name='viewer' id='" + idForCheckbox + "' value='" + idForCheckbox + "' onclick='FRCProcess3DLoadCheckboxClick(\""
                    + idLevel + "\", \"" + structureType + "\")'>");
            sbResult.append("</input>");
            sbResult.append("<label for='" + idForCheckbox + "'>");
            sbResult.append("</label>");
            sbResult.append("</div>");
            sbResult.append("</div>");
            sbResult.append("<img style='visibility:hidden;width:0px;height:0px;' onload='FRCProcess3DLoadCheckboxSetState(\"" + idLevel + "\")' src='../common/images/iconActionActions.png'/>");
            // }
            vect.add(sbResult.toString());
        }

        long endTime = System.currentTimeMillis();
        if (perfoTraces != null) {
            perfoTraces.write("Time spent in get3DLoadColumnHTML() : " + (endTime - startTime) + " milliseconds");
            perfoTraces.newLine();
            perfoTraces.flush();
        }

        return vect;
    }

    public static Vector getMBOMNameHTML(Context context, String[] args) throws Exception { // Called from table FRCMBOMTable (column FRCMBOMCentral.MBOMTableColumnTitle)
        // NOT USED ANY MORE, because redefined in FRCMBOMProg???
        long startTime = System.currentTimeMillis();
        try {
            ContextUtil.startTransaction(context, false);

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            List<String> listIDs = new ArrayList<String>();

            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String objectID = (String) mapObjectInfo.get("id");
                listIDs.add(objectID);
            }

            StringList busSelect = new StringList();
            busSelect.add("attribute[PLMEntity.V_Name].value");
            MapList resultInfoML = DomainObject.getInfo(context, listIDs.toArray(new String[0]), busSelect);

            int index = 0;
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String objectID = (String) mapObjectInfo.get("id");

                Map<String, String> resultInfoMap = (Map<String, String>) resultInfoML.get(index);
                index++;

                String objectType = (String) mapObjectInfo.get("type");
                // String objectDisplayStr = MqlUtil.mqlCommand(context, "print bus " + objectID + " select attribute[PLMEntity.V_Name].value dump ' '", false, false);
                String objectDisplayStr = resultInfoMap.get("attribute[PLMEntity.V_Name].value");

                StringBuffer resultSB = new StringBuffer();

                resultSB.append(genObjHTML(context, objectID, objectType, objectDisplayStr, false, false));

                vecResult.add(resultSB.toString());
            }

            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();
            System.out.println("FRC PERFOS : getMBOMNameHTML : " + (endTime - startTime));

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }

    }

    public static Vector getCompletionForPS(Context context, String[] args) throws Exception { // Called from table FRCPSTable (column MBOMCompletion)
        long startTime = System.currentTimeMillis();

        try {
            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            StringBuilder sbHTML;

            List<String> listIDs = new ArrayList<String>();
            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                // System.out.println("FRC : mapObjectInfo = " + mapObjectInfo);
                String idConnection = (String) mapObjectInfo.get("id[connection]");
                if (idConnection != null && !idConnection.isEmpty()) {
                    listIDs.add(idConnection);
                }
            }

            StringList relSelect = new StringList();
            relSelect.add("attribute[PLMInstance.PLM_ExternalID].value");
            MapList resultInfoML = DomainRelationship.getInfo(context, listIDs.toArray(new String[0]), relSelect);

            int index = 0;
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                // System.out.println("FRC : mapObjectInfo = " + mapObjectInfo);
                String objPID = (String) mapObjectInfo.get("physicalid");
                if (objPID == null || objPID.isEmpty()) {
                    String objId = (String) mapObjectInfo.get("id");
                    DomainObject domObj = new DomainObject(objId);
                    objPID = domObj.getInfo(context, "physicalid");
                    mapObjectInfo.put("physicalid", objPID);
                    mapObjectInfo.put("pathPID", objPID);// Consider it as root
                                                         // ???
                }

                String objPIDConnection = (String) mapObjectInfo.get("physicalid[connection]");
                if (objPIDConnection == null || objPIDConnection.isEmpty()) {
                    objPIDConnection = "";
                }
                String pathPID = (String) mapObjectInfo.get("pathPID");
                String pathLID = (String) mapObjectInfo.get("pathLID");

                String idConnection = (String) mapObjectInfo.get("id[connection]");
                String productInstName = "";
                if (idConnection != null && !idConnection.isEmpty()) {
                    Map<String, String> resultInfoMap = (Map<String, String>) resultInfoML.get(index);
                    index++;

                    // productInstName = MqlUtil.mqlCommand(context, "print connection " + idConnection + " select attribute[PLMInstance.PLM_ExternalID].value dump |", false, false);
                    productInstName = resultInfoMap.get("attribute[PLMInstance.PLM_ExternalID].value");
                }
                // Commented for MBO-181:H65:START
                // productInstName = XSSUtil.encodeForHTML(context, productInstName);
                // Commented for MBO-181:H65:END

                // String lastPathLID = "";
                // if (pathLID != null && !pathLID.isEmpty()) {
                // lastPathLID = pathLID.substring(pathLID.lastIndexOf("/") + 1);
                // }

                sbHTML = new StringBuilder();
                sbHTML.append("<div draggable=\"true\" ondragstart=\"FRCDragStart(event, '" + pathPID
                        + "', 'PS');\" ondragend=\"FRCMBOMDragEnd(event);\" style=\"cursor: move;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#E69F00;display:inline-block;\" class=\"completionPS\" title=\"Not Assigned\" id=\"completionPS_");
                sbHTML.append(objPID);
                sbHTML.append("\" instPID=\"");
                sbHTML.append(objPIDConnection);
                sbHTML.append("\" instName=\"");
                // Changes added for MBO-181:H65:START
                sbHTML.append(XSSUtil.encodeForURL(context, productInstName));
                // Changes added for MBO-181:H65:END
                sbHTML.append("\" pathPID=\"");
                sbHTML.append(pathPID);
                sbHTML.append("\" pathLID=\"");
                sbHTML.append(pathLID);
                sbHTML.append("\" ></div>");
                // sbHTML.append("<img src=\"../FRCMBOMCentral/images/onLoadImg.png\" onload=\"updatePSCompletionStatusInstance('");
                // sbHTML.append(objPIDConnection);
                // sbHTML.append("');updateMBOMImplementByLogicalId('");
                // sbHTML.append(lastPathLID);
                // sbHTML.append("');updatePSCompletionStatus('");
                // sbHTML.append(objPID);
                // sbHTML.append("');\"></img>");

                // vecResult.add("<img border=\"0\"
                // src=\"../FRCMBOMCentral/images/Completion_red.png\"></img>");
                vecResult.add(sbHTML.toString());
            }
            // XSSOK

            long endTime = System.currentTimeMillis();
            System.out.println("FRC PERFOS : getCompletionForPS : " + (endTime - startTime));

            if (perfoTraces != null) {
                perfoTraces.write("Time spent in getCompletionForPS() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            throw exp;
        }

    }

    public static String getListAuthorizedChildManufItemTypes(Context context, String[] args) throws Exception { // Called from FRCInsertDuplicatedPreProcess.jsp and FRCInsertDuplicatedPreProcess2.jsp
                                                                                                                 // and FRCInsertExistingPreProcess.jsp and FRCInsertManufItemPreProcess.jsp
        StringBuffer returnSB = new StringBuffer("");

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String type = args[0];

            List<String> listTypes = FRCMBOMModelerUtility.getAuthorizedChildMBOMReferenceTypes(context, plmSession, type);

            boolean firstElem = true;
            for (String childType : listTypes) {
                if (firstElem)
                    firstElem = false;
                else
                    returnSB.append(",");
                returnSB.append(childType);
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnSB.toString();
    }

    public static Vector getImplementColumn(Context context, String[] args) throws Exception { // Called by table FRCMBOMTable (column FRCMBOMCentral.MBOMTableColumnImplement)
        long startTime = System.currentTimeMillis();

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            // Do for each object
            List<String> listInstIDs = new ArrayList<String>();
            List<String> listRefIDs = new ArrayList<String>();
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String manuItemInstID = (String) mapObjectInfo.get("id[connection]");
                String manuItemRefID = (String) mapObjectInfo.get("id");

                if (manuItemInstID != null && !"".equals(manuItemInstID)) {
                    listInstIDs.add(manuItemInstID);
                }

                listRefIDs.add(manuItemRefID);
            }

            List<String> resScope = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, listRefIDs);

            System.out.println("EPI SCOPE : " + resScope);

            List<Map<String, Object>> resImp = null;
            if (listInstIDs.size() > 0) {
                try {
                    resImp = FRCMBOMModelerUtility.getImplementLinkInfoWithEffStatus(context, plmSession, listInstIDs, true);
                } catch (Exception e) {
                    resImp = FRCMBOMModelerUtility.getImplementLinkInfoWithEffStatus(context, plmSession, listInstIDs, false);
                }
            }

            // Get all the instance names without looking twice for the same instance
            Set<String> psInstancePIDList = new HashSet<String>();
            if (resImp != null) {
                for (Map<String, Object> linkInfo : resImp) {
                    List<String> ilPathPIDList = (List<String>) linkInfo.get("PIDList");
                    if (ilPathPIDList != null) {
                        for (int j = 0; j < ilPathPIDList.size(); j++) {
                            String physicalId = ilPathPIDList.get(j);
                            psInstancePIDList.add(physicalId);
                        }
                    }
                }
            }

            Map<String, String> psInstanceNames = new HashMap<String, String>();

            StringList select = new StringList();
            select.add("attribute[PLMInstance.PLM_ExternalID].value");
            MapList resultInfoML = DomainRelationship.getInfo(context, psInstancePIDList.toArray(new String[0]), select);
            int index = 0;
            for (String pid : psInstancePIDList) {
                Map<String, String> resultInfoMap = (Map<String, String>) resultInfoML.get(index);
                index++;
                psInstanceNames.put(pid, resultInfoMap.get("attribute[PLMInstance.PLM_ExternalID].value"));
            }

            // Do for each object
            int resScopeIndex = 0;
            int resImpIndex = 0;
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                StringBuffer cellContentSB = new StringBuffer("");

                Map mapObjectInfo = (Map) itrObjects.next();

                String productRefPID = resScope.get(resScopeIndex);
                resScopeIndex++;

                if (resImp != null) {
                    Map<String, Object> linkInfo = null;
                    String manuItemInstID = (String) mapObjectInfo.get("id[connection]");
                    if (manuItemInstID != null && !"".equals(manuItemInstID)) {
                        linkInfo = resImp.get(resImpIndex);
                        resImpIndex++;
                    }

                    List<String> ilPathPIDList = (List<String>) linkInfo.get("PIDList");
                    List<String> ilPathLIDList = (List<String>) linkInfo.get("LIDList");
                    Boolean ilPathEffSynch = (Boolean) linkInfo.get("EffSynch");

                    if (ilPathPIDList != null) {
                        StringBuilder sbPathPID = new StringBuilder();
                        StringBuilder sbPathLID = new StringBuilder();

                        StringBuffer cellDisplaySB = new StringBuffer("");

                        String lastPID = "";

                        for (int j = 0; j < ilPathPIDList.size(); j++) {
                            // Map elemPath = (Map) mlElemsPathImplement.get(j);
                            // String physicalId = (String) elemPath.get("physicalid");
                            // String logicalId = (String) elemPath.get("logicalid");

                            String physicalId = ilPathPIDList.get(j);
                            String logicalId = ilPathLIDList.get(j);

                            String productInstName = psInstanceNames.get(physicalId);
                            if (productInstName == null || "".equals(productInstName))
                                productInstName = "???";

                            if (j > 0) {
                                cellDisplaySB.append(" => ");
                            }

                            cellDisplaySB.append(productInstName);

                            if (j > 0) {
                                sbPathPID.append("/");
                                sbPathLID.append("/");
                            }
                            sbPathPID.append(physicalId);
                            sbPathLID.append(logicalId);

                            lastPID = physicalId;
                        }

                        String strImplPathPIDS = sbPathPID.toString();
                        String strImplPathLIDS = sbPathLID.toString();
                        // Commented for MBO-181:H65:START
                        // String displayStr = XSSUtil.encodeForHTML(context, cellDisplaySB.toString());
                        // Changes added for MBO-181:H65:END

                        String manufItemPID = (String) mapObjectInfo.get("physicalid");
                        if (manufItemPID == null || manufItemPID.isEmpty()) {
                            String manuItemOID = (String) mapObjectInfo.get("id");
                            DomainObject domManuf = new DomainObject(manuItemOID);
                            manufItemPID = domManuf.getInfo(context, "physicalid");
                        }

                        String level = (String) mapObjectInfo.get("id[level]");
                        if (level == null || "".equals(level))
                            level = (String) mapObjectInfo.get("level");

                        cellContentSB.append("<div onclick=\"FRCUpdateImplementLink('" + level
                                + "')\" style=\"cursor:pointer;border:1px solid black;border-radius:8px;height:16px;width:16px;background-color:#EC3B1D;display:inline-block;\" class=\"completionMBOM\" title=\"\" id=\"completionMBOM_");
                        cellContentSB.append(manufItemPID);
                        cellContentSB.append("\" instPID=\"");
                        cellContentSB.append(manuItemInstID);
                        cellContentSB.append("\" pathPID=\"");
                        cellContentSB.append(strImplPathPIDS);
                        cellContentSB.append("\" pathLID=\"");
                        cellContentSB.append(strImplPathLIDS);
                        cellContentSB.append("\" ></div>");

                        cellContentSB.append("<span ");
                        // HE5 : Added the condition for null for getImplementedInstancesPathsFromList() R&D API
                        // if (!ilPathEffSynch) // If effectivity is not up to date, show the implement link text in red.
                        if (ilPathEffSynch == null)
                            cellContentSB.append("style=\"color:orange\" ");
                        else if (!ilPathEffSynch) // If effectivity is not up to date, show the implement link text in red.
                            cellContentSB.append("style=\"color:red\" ");
                        // Changes added for MBO-184& MBO-183:H65:START
                        cellContentSB.append("title=\"" + manuItemInstID + "\"  id=\"completionMBOM_");
                        cellContentSB.append(manufItemPID);
                        cellContentSB.append("-span\">");
                        // cellContentSB.append(cellDisplaySB.toString());
                        // Changes added for MBO-184 &MBO-183:H65:END
                        cellContentSB.append("</span>");
                        cellContentSB.append("<div class='mBomImplementLink' style='display:none' manufItemInstID='");
                        cellContentSB.append(manuItemInstID);
                        cellContentSB.append("' implemPathPIDS='");
                        cellContentSB.append(strImplPathPIDS);
                        cellContentSB.append("' implemPathLIDS='");
                        cellContentSB.append(strImplPathLIDS);
                        cellContentSB.append("' productInstPID='");
                        cellContentSB.append(lastPID);
                        cellContentSB.append("'></div>");
                    }
                }
                // Add Scope infos
                String manufItemRefID = (String) mapObjectInfo.get("id");

                if (productRefPID != null && !"".equals(productRefPID)) {
                    boolean intermediateScope = false;
                    DomainObject domMBOM = new DomainObject(manufItemRefID);
                    for (String typeInList : baseTypesForMBOMAssemblyNodes) {
                        if (domMBOM.isKindOf(context, typeInList))
                            intermediateScope = true;
                    }

                    cellContentSB.append("<div class='mBomScopeLink' style='display:none' manufItemRefID='");
                    cellContentSB.append(manufItemRefID);
                    cellContentSB.append("' scopeId='");
                    cellContentSB.append(manufItemRefID);
                    cellContentSB.append("-");
                    cellContentSB.append(productRefPID);
                    cellContentSB.append("' productRefPID='");
                    cellContentSB.append(productRefPID);
                    cellContentSB.append("' intermediateScope='");
                    cellContentSB.append((intermediateScope ? "true" : "false"));
                    cellContentSB.append("'></div>");
                }

                vecResult.add(cellContentSB.toString());
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();
            System.out.println("FRC PERFOS : getImplementColumn : " + (endTime - startTime));

            if (perfoTraces != null) {
                perfoTraces.write("Time spent in getImplementColumn() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            return vecResult;
        } catch (Exception exp) {
            long endTime = System.currentTimeMillis();

            if (perfoTraces != null) {
                perfoTraces.write("Time spent in getImplementColumn() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static Vector getRevisionColumn(Context context, String[] args) throws Exception { // Called from table FRCEBOMMBOMTable (column FRCMBOMCentral.MBOMTableColumnRevision) and from table
                                                                                              // FRCPSQueryResultsTable (column FRCMBOMCentral.PSTableColumnRevision) and from table FRCPSTable (column
                                                                                              // FRCMBOMCentral.PSTableColumnRevision) and from table FRCMBOMTable (column
                                                                                              // FRCMBOMCentral.MBOMTableColumnRevision)
        long startTime = System.currentTimeMillis();
        List<String> listIDs = null;

        try {
            ContextUtil.startTransaction(context, false);

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            // FRC START - HE5 : Added the part of code to fix the issue #267
            Map paramMap = (HashMap) programMap.get("paramList");
            boolean isexport = false;
            String export = (String) paramMap.get("exportFormat");
            if (export != null) {
                isexport = true;
            }
            // FRC END - HE5 : Added the part of code to fix the issue #267

            MapList objectList = (MapList) programMap.get("objectList");

            listIDs = new ArrayList<String>();
            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String objectID = (String) mapObjectInfo.get("id");
                listIDs.add(objectID);
            }
            StringList busSelect = new StringList();
            busSelect.add("revision");
            busSelect.add("majorids");
            busSelect.add("majorid");
            busSelect.add("logicalid");
            MapList resultInfoML = DomainObject.getInfo(context, listIDs.toArray(new String[0]), busSelect);

            int index = 0;
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String physicalID = (String) mapObjectInfo.get("id");
                Map<String, String> resultInfoMap = (Map<String, String>) resultInfoML.get(index);
                index++;
                String majorID = resultInfoMap.get("majorid");
                String logicalid = resultInfoMap.get("logicalid");
                String objectRevision = resultInfoMap.get("revision");
                String revisionsListStr = resultInfoMap.get("majorids");
                if (majorID == null)
                    majorID = "temp";
                // FRC START - HE5 : Added the part of code to fix the issue #267
                if (isexport) {
                    vecResult.add(objectRevision);
                } else {

                    if (revisionsListStr.endsWith(physicalID) || revisionsListStr.endsWith(majorID) || revisionsListStr.endsWith(logicalid))
                        vecResult.add("<div>" + objectRevision + "</div>");
                    else
                        vecResult.add("<div style=\"color:red\">" + objectRevision + "</div>");
                }
                // FRC END - HE5 : Added the part of code to fix the issue #267
            }

            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();
            System.out.println("FRC PERFOS : getRevisionColumn (" + listIDs.size() + " objects in list) : " + (endTime - startTime));

            if (perfoTraces != null) {
                perfoTraces.write("Time spent in getRevisionColumn() : " + (endTime - startTime) + " milliseconds");
                perfoTraces.newLine();
                perfoTraces.flush();
            }

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static Vector get3DShowColumn(Context context, String[] args) throws Exception { // Called by table FRCMBOMTable (column Show3D)
        // NOT USED ANY MORE, because redefined in FRCMBOMProg???
        try {
            ContextUtil.startTransaction(context, false);

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String level = (String) mapObjectInfo.get("id[level]");
                if (level == null || "".equals(level))
                    level = (String) mapObjectInfo.get("level");

                vecResult.add("<img border=\"0\" onclick=\"load3DFromMBOMFromRowID('" + level + "')\" src=\"../common/images/iconSmallShowHide3D.gif\" style=\"height: 16px; cursor: pointer\"></img>");
            }

            ContextUtil.commitTransaction(context);
            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static Vector getDiversityColumnOLD(Context context, String[] args) throws Exception { // Called by table FRCPSTable (column FRCMBOMCentral.TableColumnDiversity) and by
        // NOT USED ANY MORE
        long startTime = System.currentTimeMillis();

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            // Do for each object
            List<String> instanceIDList = new ArrayList<String>();
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String instanceID = (String) mapObjectInfo.get("id[connection]");
                if (instanceID != null && !"".equals(instanceID)) {
                    instanceIDList.add(instanceID);
                }
            }

            Map<String, String> effMap = FRCMBOMModelerUtility.getEffectivityOnInstanceList(context, plmSession, instanceIDList, true);

            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                // System.out.println("FRC : mapObjectInfo = " + mapObjectInfo);

                String displayStr = "";

                String instanceID = (String) mapObjectInfo.get("id[connection]");
                if (instanceID != null && !"".equals(instanceID)) {
                    String displayEff = effMap.get(instanceID);

                    if (displayEff != null)
                        displayStr = displayEff;
                }

                StringBuffer cellContentSB = new StringBuffer("");
                cellContentSB.append("<span title=\"" + displayStr + "\"");

                cellContentSB.append(">");
                cellContentSB.append(displayStr);
                cellContentSB.append("</span>");

                vecResult.add(cellContentSB.toString());
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();
            System.out.println("FRC PERFOS : getDiversityColumn : " + (endTime - startTime));

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static Vector getDiversityColumn(Context context, String[] args) throws Exception {
        // NOT USED ANY MORE, because redefined in FRCMBOMProg???
        long startTime = System.currentTimeMillis();
        long timeEff = 0;
        long timeImp = 0;
        long timeCompute = 0;
        long timeCompile = 0;
        long timeCompare = 0;

        try {
            ContextUtil.startTransaction(context, false);

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            Map paramMap = (Map) programMap.get("paramList");
            String parentOID = (String) paramMap.get("objectId");
            String parentOIDIsManufItem = MqlUtil.mqlCommand(context, "print bus " + parentOID + " select type.kindof[DELFmiFunctionReference] dump |", false, false);

            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                // System.out.println("EPI : mapObjectInfo = " + mapObjectInfo);

                String displayStr = "";
                boolean isEffUpToDate = true;

                String instanceID = (String) mapObjectInfo.get("id[connection]");
                if (instanceID != null && !"".equals(instanceID)) {
                    long startTimeEff = System.currentTimeMillis();
                    String currentEffXML = FRCMBOMModelerUtility.getEffectivityXML(context, instanceID, true);
                    long endTimeEff = System.currentTimeMillis();
                    timeEff += (endTimeEff - startTimeEff);

                    displayStr = FRCMBOMModelerUtility.getEffectivityOrderedStringFromXML(context, currentEffXML);

                    if ("TRUE".equalsIgnoreCase(parentOIDIsManufItem)) {
                        long startTimeImp = System.currentTimeMillis();
                        MapList mlElemsPathImplement = FRCMBOMModelerUtility.getImplementPIDList(context, instanceID);
                        long endTimeImp = System.currentTimeMillis();
                        timeImp += (endTimeImp - startTimeImp);

                        if (mlElemsPathImplement.size() > 0) {
                            long startTimeCompute = System.currentTimeMillis();
                            HashMap upToDateEffMap = FRCMBOMModelerUtility.computeInheritedEffectivity(context, mlElemsPathImplement);
                            long endTimeCompute = System.currentTimeMillis();
                            timeCompute += (endTimeCompute - startTimeCompute);

                            String upToDateEffXML = (String) upToDateEffMap.get("effectivity");

                            if (currentEffXML != null && !"".equals(currentEffXML)) {
                                if (upToDateEffXML != null && !"".equals(upToDateEffXML)) {
                                    long startTimeCompile = System.currentTimeMillis();
                                    Map currentEffCompBin = FRCMBOMModelerUtility.getCompiledBinary(context, currentEffXML);
                                    Map upToDateEffCompBin = FRCMBOMModelerUtility.getCompiledBinary(context, upToDateEffXML);
                                    long endTimeCompile = System.currentTimeMillis();
                                    timeCompile += (endTimeCompile - startTimeCompile);

                                    long startTimeCompare = System.currentTimeMillis();
                                    isEffUpToDate = FRCMBOMModelerUtility.areBinaryExpressionsEqual(context, (String) currentEffCompBin.get(EffectivityExpression.COMPILED_BINARY_EXPR),
                                            (String) upToDateEffCompBin.get(EffectivityExpression.COMPILED_BINARY_EXPR));
                                    long endTimeCompare = System.currentTimeMillis();
                                    timeCompare += (endTimeCompare - startTimeCompare);
                                } else {
                                    isEffUpToDate = false;
                                }
                            } else {
                                if (upToDateEffXML != null && !"".equals(upToDateEffXML)) {
                                    displayStr = "No effectivity";
                                    isEffUpToDate = false;
                                }
                            }
                        }
                    }
                }

                StringBuffer cellContentSB = new StringBuffer("");
                cellContentSB.append("<span title=\"" + displayStr + "\"");

                if (!isEffUpToDate)
                    cellContentSB.append(" style=\"color:red\"");

                cellContentSB.append(">");
                cellContentSB.append(displayStr);
                cellContentSB.append("</span>");

                vecResult.add(cellContentSB.toString());
            }

            ContextUtil.commitTransaction(context);

            long endTime = System.currentTimeMillis();
            System.out.println("EPI PERFOS : getDiversityColumn : " + (endTime - startTime));
            System.out.println("EPI PERFOS : getDiversityColumn timeEff : " + timeEff);
            System.out.println("EPI PERFOS : getDiversityColumn timeImp : " + timeImp);
            System.out.println("EPI PERFOS : getDiversityColumn timeCompute : " + timeCompute);
            System.out.println("EPI PERFOS : getDiversityColumn timeCompile : " + timeCompile);
            System.out.println("EPI PERFOS : getDiversityColumn timeCompare : " + timeCompare);

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static HashMap getChangeObjectList(Context context, String[] args) throws Exception { // Called from FRCUpdateCreateFormAfterChangeCreation.jsp and by form
                                                                                                 // FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.Form.Label.ChangeObject) and by form
                                                                                                 // FRCCreateMBOM (field FRCMBOMCentral.Form.Label.ChangeObject) and by command FRCMBOMGetChangeObject
        HashMap retMap = new HashMap();
        StringList slOIDValues = new StringList();
        StringList slDisplayValues = new StringList();

        try {
            ContextUtil.startTransaction(context, false);

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");

            MapList mlCAs = getChangeObjects(context);

            String FRCToolbarGetChangeObjectCmdValue = (String) requestMap.get("FRCToolbarGetChangeObjectCmdValue");
            boolean firstValueSet = false;

            ArrayList<String> listPIDs = new ArrayList<String>();// Avoid having objects twice

            if (null != mlCAs) {
                for (Object obj : mlCAs) {
                    Map map = (Map) obj;
                    String nameObj = (String) map.get("name");
                    String revObj = (String) map.get("revision");
                    String pIDObj = (String) map.get("physicalid");

                    if (!listPIDs.contains(pIDObj)) {// Avoid having objects twice
                        if (FRCToolbarGetChangeObjectCmdValue != null && !"".equals(FRCToolbarGetChangeObjectCmdValue) && FRCToolbarGetChangeObjectCmdValue.equals(pIDObj)) {
                            slOIDValues.add(0, pIDObj);
                            slDisplayValues.add(0, nameObj + " " + revObj);
                            firstValueSet = true;
                        } else {
                            slOIDValues.add(pIDObj);
                            slDisplayValues.add(nameObj + " " + revObj);

                        }
                        listPIDs.add(pIDObj);

                    }
                }
            }

            if (firstValueSet) {
                slOIDValues.add(1, "-none-");
                slDisplayValues.add(1, "-none-");
            } else {
                slOIDValues.add(0, "-none-");
                slDisplayValues.add(0, "-none-");
            }

            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            ex.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw ex;
        }
        retMap.put("field_choices", slOIDValues);
        retMap.put("field_display_choices", slDisplayValues);

        return retMap;
    }

    public static void insertNewManufItem(Context context, String[] args) throws Exception { // Called from FRCInsertManufItemPreProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String parentPID = (String) requestMap.get("objectId");

            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String newRefPID = (String) paramMap.get("newObjectId");

            createInstance(context, plmSession, parentPID, newRefPID);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static Map createNewManufItem(Context context, String[] args) throws Exception { // Called from FRCInsertManufItemPreProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Map returnMap = createNewManufItemReference(context, args);

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String type = (String) programMap.get("TypeActual");

            // Prepare the magnitude, if the object is Continuous
            Locale loc = context.getLocale();
            String magnitudeFieldValue = (String) programMap.get("MagnitudeCreate");
            if (magnitudeFieldValue == null || "".equals(magnitudeFieldValue))
                magnitudeFieldValue = (String) programMap.get("MagnitudeForm");

            String magnitudeType = null;
            if (magnitudeFieldValue != null && !"".equals(magnitudeFieldValue)) {
                String magnitudeFieldKey = "";
                for (String key : listMagnitudeFieldKeys) {
                    if (magnitudeFieldValue.equals(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key)))
                        magnitudeFieldKey = key;
                }

                if ("FRCMBOMCentral.MBOMManufItemMagnitudeLength".equals(magnitudeFieldKey))
                    magnitudeType = "Length";
                else if ("FRCMBOMCentral.MBOMManufItemMagnitudeMass".equals(magnitudeFieldKey))
                    magnitudeType = "Mass";
                else if ("FRCMBOMCentral.MBOMManufItemMagnitudeArea".equals(magnitudeFieldKey))
                    magnitudeType = "Area";
                else if ("FRCMBOMCentral.MBOMManufItemMagnitudeVolume".equals(magnitudeFieldKey))
                    magnitudeType = "Volume";
            }

            HashMap<String, String> attributes = new HashMap<String, String>();

            String newRefPID = createMBOMReference(context, plmSession, type, magnitudeType, attributes);

            String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
            // Modif AFN - Test if a value has been defined into the creation web form
            String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
            if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
                changeObjectName = changeObjectFromForm;
            attachObjectToChange(context, plmSession, changeObjectName, newRefPID);

            Map returnMap = new HashMap();
            returnMap.put("id", newRefPID);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            return returnMap;
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static void freezeManufItems(Context context, String[] args) throws Exception { // Called from FRCFreezeUnfreeze.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            for (String objectID : args) {
                String currentState = MqlUtil.mqlCommand(context, "print bus " + objectID + " select current dump |", false, false);

                if ("IN_WORK".equals(currentState)) {
                    FRCMBOMModelerUtility.changeMaturityMBOMReference(context, plmSession, objectID, "ToFreeze");
                    flushSession(plmSession);

                    // Prepare the promote to RELEASED :
                    // Unsign the transition UnFreeze if needed
                    DomainObject dObject = new DomainObject(objectID);

                    MapList sigDetailsList = dObject.getSignaturesDetails(context, "FROZEN", "IN_WORK");
                    if (sigDetailsList.size() > 0) {
                        Map sigDetails = (Map) sigDetailsList.get(0);
                        String sigApproved = (String) sigDetails.get("approved");
                        String sigSigned = (String) sigDetails.get("signed");
                        if ("TRUE".equalsIgnoreCase(sigApproved) || "TRUE".equalsIgnoreCase(sigSigned)) {
                            MqlUtil.mqlCommand(context, true, "unsign bus $1 signature $2", true, objectID, "UnFreeze");
                        }
                    }

                    // Approve the next promote to RELEASED so that the Complete of the CA may work
                    MqlUtil.mqlCommand(context, true, "approve bus $1 signature $2", true, objectID, "ToRelease");
                }
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static String unfreezeManufItems(Context context, String[] args) throws Exception { // Called from FRCFreezeUnfreeze.jsp
        String sRet = "";
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            for (String objectID : args) {
                String currentState = MqlUtil.mqlCommand(context, "print bus " + objectID + " select current dump |", false, false);

                if ("FROZEN".equals(currentState)) {
                    // Verify the associated CA are not Frozen
                    DomainObject domMfgItem = new DomainObject(objectID);
                    StringList objSelects = new StringList();
                    objSelects.addElement(DomainConstants.SELECT_ID);
                    objSelects.addElement(DomainConstants.SELECT_CURRENT);
                    String sObjTypes = ChangeConstants.TYPE_CHANGE_ACTION;
                    String sRelTypes = ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM;
                    MapList mlCA = domMfgItem.getRelatedObjects(context, sRelTypes, sObjTypes, objSelects, null, true, false, (short) 1, null, null);
                    if (null != mlCA) {
                        for (int i = 0; i < mlCA.size(); i++) {
                            Map mObj = (Map) mlCA.get(i);
                            String sState = (String) mObj.get(DomainConstants.SELECT_CURRENT);
                            if (!"Pending".equalsIgnoreCase(sState) && !"In Work".equalsIgnoreCase(sState)) {
                                String strLanguage = context.getSession().getLanguage();
                                String sErrorMsg = "ERROR|" + EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FRCMBOMCentral.UnFreezeItem.LinkToFreezeCA", strLanguage);
                                ContextUtil.abortTransaction(context);
                                return sErrorMsg;
                            }
                        }
                    }
                    FRCMBOMModelerUtility.changeMaturityMBOMReference(context, plmSession, objectID, "UnFreeze");
                }
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
        return sRet;
    }

    public static String checkReplaceNewRevisionManufItem(Context context, String[] args) throws Exception { // Called from FRCReplaceNewRevisionPreProcess.jsp
        String returnValue = "";

        try {
            ContextUtil.startTransaction(context, false);

            // String instPID = args[0];
            // String childRefPID = args[1];

            // String childRefType = MqlUtil.mqlCommand(context, "print bus " + childRefPID + " select type dump |", false, false);

            /*
             * boolean isTypeForImplementLinks = false; for (String typeInList : baseTypesForImplementLinks) { if (getDerivedTypes(context, typeInList).contains(childRefType)) isTypeForImplementLinks
             * = true; }
             * 
             * if (isTypeForImplementLinks) { returnValue = "You cannot manualy revision Provides or Manufactured Materials : their revisions are syncrhonized with those of the part they implement." ;
             * }
             */
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnValue;
    }

    public static String replaceNewRevisionManufItem(Context context, String[] args) throws Exception { // Called from FRCReplaceNewRevisionPostProcess.jsp
        String returnValue = "";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String instPID = args[0];
            String childRefPID = args[1];
            String changeObjectName = args[2];

            String newChildRefPID = newRevisionMBOMReference(context, plmSession, childRefPID);

            returnValue = newChildRefPID;

            attachObjectToChange(context, plmSession, changeObjectName, newChildRefPID);

            if (instPID != null && !"".equals(instPID))
                FRCMBOMModelerUtility.replaceMBOMInstance(context, plmSession, instPID, newChildRefPID);

            flushAndCloseSession(plmSession);
            // HE5 : Added fix for the error "Transaction not active" after calling the R&D API replaceMBOMInstance
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            if (ContextUtil.isTransactionActive(context)) {
                ContextUtil.abortTransaction(context);
            }
            throw exp;
        }

        return returnValue;
    }

    public static boolean getAccessToMBOMActionsMenu(Context context, String[] args) throws Exception { // Called from menu FRCMBOMActions
        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        String changeObjectName = (String) programMap.get("FRCToolbarGetChangeObjectCmdValue");

        /*
         * if (changeObjectName == null || "".equals(changeObjectName) || changeObjectName.contains("none")) return false; else return true;
         */

        // CO Dissociation :
        return true;
    }

    public static String getConfirmationMessageForSetImplementLink(Context context, String[] args) throws Exception {// Check called by FRCCreateOrUpdateImplementLinkPreProcess.jsp
        String returnValue = "";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomCompletePath = args[0];
            String psCompletePath = args[1];

            String[] mbomCompletePathList = mbomCompletePath.split("/");
            String[] psCompletePathList = psCompletePath.split("/");

            if (psCompletePathList.length == 1)
                throw new Exception("You cannot consume the root part.");

            String mbomLeafInstancePID = mbomCompletePathList[mbomCompletePathList.length - 1];
            String mbomLeafReferencePID = "";
            if (mbomCompletePathList.length > 1) {
                mbomLeafReferencePID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstancePID + " select to.physicalid dump |", false, false);

                String mbomLeafRefType = MqlUtil.mqlCommand(context, "print bus " + mbomLeafReferencePID + " select type dump |", false, false);

                // Check only if it is a direct assignation
                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType)) {
                        List<String> inputListForGetScope = new ArrayList<String>();
                        inputListForGetScope.add(mbomLeafReferencePID);
                        String oldScopeReferencePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                        if (oldScopeReferencePID != null && !"".equals(oldScopeReferencePID)) {
                            String oldScopeReferenceMajorPIDListStr = MqlUtil.mqlCommand(context, "print bus " + oldScopeReferencePID + " select majorids.id dump |", false, false);

                            String psLeafInstancePID = psCompletePathList[psCompletePathList.length - 1];
                            String psLeafReferencePID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstancePID + " select to.physicalid dump |", false, false);

                            if (!oldScopeReferenceMajorPIDListStr.contains(psLeafReferencePID))
                                returnValue = "You have selected a part which is not in the same revision family as the previous provided part. Do you want to continue?";
                        }
                    }
                }

            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnValue;
    }

    public static String setImplementLink(Context context, String[] args) throws Exception {// Called by FRCCreateOrUpdateImplementLinkPostProcess.jsp
        // Return value :
        // 0 = refresh row of the leaf MBOM instance
        // 1 = re-expand the row of the leaf MBOM instance
        // 2 = re-expand the row of the parent of the MBOM instance
        String returnValue = "0";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomCompletePath = args[0];
            String psCompletePaths = args[1];// Multiple paths possible
            String changeObjectPID = args[2];

            String[] arrPsPaths = psCompletePaths.split("\\|", -2);
            int iCurrentRetVal = 0;
            String[] newArgs = new String[3];
            newArgs[0] = mbomCompletePath;
            newArgs[1] = "";
            newArgs[2] = changeObjectPID;

            List<String> newRefPIDList = new ArrayList<String>();

            for (String psPathHere : arrPsPaths) {
                newArgs[1] = psPathHere;

                String returnValueHere = setImplementLinkProcess(context, plmSession, newArgs, newRefPIDList);

                int iRetVal = Integer.parseInt(returnValueHere);
                if (iRetVal > iCurrentRetVal)
                    iCurrentRetVal = iRetVal;
            }

            // Attach all new references created during the process to the change object
            attachListObjectsToChange(context, plmSession, changeObjectPID, newRefPIDList);

            // returnValue = setImplementLinkProcess(context, args);
            returnValue = Integer.toString(iCurrentRetVal);

            flushAndCloseSession(plmSession);
            // HE5 : Added fix for the error "Transaction not active" after calling the R&D API replaceMBOMInstance
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnValue;
    }

    public static String checkCreateNewRootScope(Context context, String[] args) throws Exception { // Called by FRCCreateNewScopePreProcess.jsp
        String returnValue = "";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomRefID = args[0];

            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(mbomRefID);
            String psScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

            if (psScopePID != null && !"".equals(psScopePID))
                returnValue = "This Manufacturing Item has already a scope.";

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnValue;
    }

    public static void createNewRootScope(Context context, String[] args) throws Exception { // Called by FRCCreateNewScopePostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomRefID = args[0];
            String psRefID = args[1];

            String mbomRefPID = MqlUtil.mqlCommand(context, "print bus " + mbomRefID + " select physicalid dump |", false, false);
            String psRefPID = MqlUtil.mqlCommand(context, "print bus " + psRefID + " select physicalid dump |", false, false);

            List<String> modifiedInstancePIDList = FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, mbomRefPID, psRefPID, true);

            System.out.println("FRC : modified instances : " + modifiedInstancePIDList);

            // On the MBOM node, set the same model as config context.
            List<String> listModelsPIDOnPSRef = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, psRefPID);

            if (listModelsPIDOnPSRef.size() > 1)
                throw new Exception("This part has multiple configuration context.");
            else if (listModelsPIDOnPSRef.size() == 1) {
                List<String> mbomRefIDList = new ArrayList<String>();
                mbomRefIDList.add(mbomRefID);

                FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, mbomRefIDList, listModelsPIDOnPSRef);
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static String checkUpdateRootScope(Context context, String[] args) throws Exception { // Called from FRCUpdateScopePreProcess.jsp
        String returnValue = "";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomRefID = args[0];

            // Check that the new PS reference provided is in the same revision family as the existing scope
            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(mbomRefID);
            String existingPSScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

            if (existingPSScopePID == null || "".equals(existingPSScopePID)) {
                throw new Exception("This Manufacturing Item has no existing scope to update.");
            }
            returnValue = existingPSScopePID;

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnValue;
    }

    // FRC changes added for MBO-167 :H65:09/10/2017-START-Method signature change
    public static String updateRootScope(Context context, String[] args) throws Exception { // Called from FRCUpdateScopePostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        // IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();
        StringBuffer returnString = new StringBuffer();
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();
            String mbomRefID = args[0];
            String psRefID = args[1];
            String errorObjects = new String();
            StringBuffer strBuffer = new StringBuffer();
            DomainObject domMfgItem = new DomainObject(mbomRefID);
            DomainObject domPSItem = new DomainObject(psRefID);
            String rootObjectState = domMfgItem.getInfo(context, DomainConstants.SELECT_CURRENT);
            String rootObjectName = domMfgItem.getInfo(context, DomainConstants.SELECT_NAME);
            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_ID);
            objSelects.addElement(DomainConstants.SELECT_NAME);
            objSelects.addElement(DomainConstants.SELECT_CURRENT);
            objSelects.addElement("physicalid");

            StringList relSelects = new StringList();
            relSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            relSelects.addElement("physicalid[connection]");
            matrix.util.Pattern relPattern = new matrix.util.Pattern(PropertyUtil.getSchemaProperty(context, "relationship_DELFmiFunctionIdentifiedInstance"));

            if ("IN_WORK".equalsIgnoreCase(rootObjectState)) {
                MapList mlToConnected = domMfgItem.getRelatedObjects(context, relPattern.getPattern(), // relationshipPattern
                        "*", // typePattern
                        objSelects, // objectSelects
                        relSelects, // relationshipSelects
                        false, // getTo
                        true, // getFrom
                        (short) 0, // recurseToLevel
                        null, // objectWhere,
                        null, // relationshipWhere
                        (int) 0); // limit

                String mbomRefPID = domMfgItem.getInfo(context, "physicalid"); // MqlUtil.mqlCommand(context, "print bus " + mbomRefID + " select physicalid dump |", false, false);
                String psRefPID = domPSItem.getInfo(context, "physicalid");// MqlUtil.mqlCommand(context, "print bus " + psRefID + " select physicalid dump |", false, false);

                List<String> inputListForGetScopeTst = new ArrayList<String>();
                inputListForGetScopeTst.add(mbomRefPID);

                List<String> modifiedInstanceList = FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomRefPID, psRefPID, true);
                flushSession(plmSession);

                for (int i = 0; i < mlToConnected.size(); i++) {
                    Map mapChild = (Map) mlToConnected.get(i);
                    String mbomObjId = (String) mapChild.get("id");
                    String strMBOMName = (String) mapChild.get("name");
                    String mbomObjPhysicalId = (String) mapChild.get("physicalid");

                    if (mbomRefPID != mbomObjPhysicalId) {
                        String mbomObjConnectionId = (String) mapChild.get("id[connection]");
                        String mbomConnectionPhysicalId = (String) mapChild.get("physicalid[connection]");

                        String mbomObjState = (String) mapChild.get("current");
                        String mbomObjName = (String) mapChild.get("name");

                        // Check if the MBOM leaf reference already has a scope
                        List<String> inputListForGetScope = new ArrayList<String>();
                        inputListForGetScope.add(mbomObjPhysicalId);
                        String currentPSRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);
                        List<String> implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomConnectionPhysicalId);
                        List<String> modifiedInstancePIDList = new ArrayList<String>();
                        if (currentPSRefScopePID != null && !"".equals(currentPSRefScopePID) && (implementLink.size() > 0)) {

                            if ("IN_WORK".equalsIgnoreCase(mbomObjState)) {

                                String implementLinkInstancePID = implementLink.get(implementLink.size() - 1);
                                String existingPSRefScopePID = MqlUtil.mqlCommand(context, "print connection " + implementLinkInstancePID + " select to.physicalid dump |", false, false);
                                List<String> modifiedInstanceList2 = FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomObjPhysicalId, existingPSRefScopePID,
                                        true);

                            } else {
                                strBuffer.append("\'").append(strMBOMName).append("\'").append("\n");
                            }
                        }

                    }
                }

            } else {
                strBuffer.append("\'").append(rootObjectName).append("\'").append("\n");
            }

            if (UIUtil.isNotNullAndNotEmpty(strBuffer.toString())) {
                returnString.append("Reconnection not performed due to node(s) not at 'In Work' state:").append("\n").append(strBuffer.toString()).append("\n");

            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);

            // }
        } catch (Exception exp) {
            // System.out.println("FRC MBO167 : ABORT" );
            exp.printStackTrace();

            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
        // FRC changes added for MBO-167 :H65:02/03/2017-END
        return returnString.toString();

    }

    public static void createNewIntermediateScope(Context context, String[] args) throws Exception {// Called from FRCCreateNewScopePostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomCompletePath = args[0];
            String psCompletePath = args[1];

            // Check if the MBOM leaf reference already has a scope
            String[] mbomPathList = mbomCompletePath.split("/");
            if (mbomPathList.length < 2)
                throw new Exception("You cannot select the root Manufacturing Item for a sub-scope creation.");

            String mbomLeafInstPID = mbomPathList[mbomPathList.length - 1];
            String mbomLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstPID + " select to.physicalid dump |", false, false);

            // Check that the MBOM leaf reference is not of type Provide (or similar)
            String mbomLeafRefType = MqlUtil.mqlCommand(context, "print bus " + mbomLeafRefPID + " select type dump |", false, false);
            boolean isIndirect = false;
            for (String typeInList : baseTypesForMBOMAssemblyNodes) {
                if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                    isIndirect = true;
            }
            if (!isIndirect)
                throw new Exception("This type of Manufacturing Item is not valid for defining a sub-scope.");

            String[] psPathList = psCompletePath.split("/");
            if (psPathList.length < 2)
                throw new Exception("You cannot select the root Part for a sub-scope creation.");

            String psLeafInstPID = psPathList[psPathList.length - 1];
            String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstPID + " select to.physicalid dump |", false, false);

            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(mbomLeafRefPID);
            String psRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

            List<String> modifiedInstancePIDList = null;

            if (psRefScopePID != null && !"".equals(psRefScopePID)) { // MBOM reference already has a scope : check that it is the leaf PS reference
                if (!psLeafRefPID.equals(psRefScopePID)) {
                    throw new Exception("You cannot create this sub-scope because the Manufacturing Item already has a different scope");
                }
            } else { // MBOM has no scope : create a new one, even if the PS leaf reference may have scopes already
                modifiedInstancePIDList = FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, mbomLeafRefPID, psLeafRefPID, true);
            }

            System.out.println("FRC : modified instances : " + modifiedInstancePIDList);

            // Create an implement link
            String trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomCompletePath, psCompletePath);

            if (trimmedPSPath == null || "".equals(trimmedPSPath)) {
                throw new Exception("No higher-level scope exists.");
            }

            // Remove any existing implement link
            FRCMBOMModelerUtility.deleteImplementLink(context, plmSession, mbomLeafInstPID, true);

            List<String> mbomLeafInstancePIDList = new ArrayList();
            mbomLeafInstancePIDList.add(mbomLeafInstPID);
            List<String> trimmedPSPathList = new ArrayList();
            trimmedPSPathList.add(trimmedPSPath);
            String retStr = FRCMBOMModelerUtility.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);
            if (!"".equals(retStr))
                throw new Exception(retStr);

            // Set the model and the config criteria on this sub-scope MBOM reference
            String mbomRootRefPID = mbomPathList[0];
            List<String> rootModelPIDList = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, mbomRootRefPID);

            List<String> mbomLeafRefPIDList = new ArrayList<String>();
            mbomLeafRefPIDList.add(mbomLeafRefPID);

            FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, mbomLeafRefPIDList, rootModelPIDList);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context); if (true) throw new Exception("EPI END.");
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    // FRC changes added for MBO-167 :H65:02/03/2017-START-Method Signature change
    public static String updateIntermediateScope(Context context, String[] args) throws Exception { // Called from FRCUpdateScopePostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        StringBuffer errorBuffer = new StringBuffer();
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();

            String mbomPath = args[0];
            String psPath = args[1];

            String[] mbomPathList = mbomPath.split("/");
            if (mbomPathList.length < 2)
                throw new Exception("You cannot select the root Manufacturing Item for an intermediate scope update.");

            String mbomLeafInstPID = mbomPathList[mbomPathList.length - 1];
            String mbomLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstPID + " select to.physicalid dump |", false, false);
            String mbomLeafObjId = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstPID + " select to.id dump |", false, false);

            DomainObject domMfgItem = new DomainObject(mbomLeafObjId);
            String mbomObjState = domMfgItem.getInfo(context, DomainConstants.SELECT_CURRENT);
            String mbomObjName = domMfgItem.getInfo(context, DomainConstants.SELECT_NAME);

            // Check that the MBOM leaf reference is not of type Provide (or similar)
            String mbomLeafRefType = MqlUtil.mqlCommand(context, "print bus " + mbomLeafRefPID + " select type dump |", false, false);
            boolean isIndirect = false;
            for (String typeInList : baseTypesForMBOMAssemblyNodes) {
                if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                    isIndirect = true;
            }
            if (isIndirect) {
                // throw new Exception("This type of Manufacturing Item is not valid for a sub-scope.");
                // Check if the MBOM leaf reference already has a scope

                List<String> inputListForGetScope = new ArrayList<String>();
                inputListForGetScope.add(mbomLeafRefPID);
                String currentPSRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                if (UIUtil.isNotNullAndNotEmpty(currentPSRefScopePID)) {
                    // throw new Exception("The Manufacturing Item you have selected does not have any scope.");

                    // Check if the scope and implement link of the MBOM is already up-to-date
                    List<String> implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomLeafInstPID);
                    if (implementLink.size() > 0) {
                        // throw new Exception("The Manufacturing Item you have selected does not have any implement link.");

                        boolean implementLinkIsUpToDate = false;
                        String implementLinkInstancePID = null;

                        if (psPath != null && !"".equals(psPath)) {
                            implementLinkIsUpToDate = true;

                            for (int n = 0; n < implementLink.size(); n++) {
                                implementLinkInstancePID = implementLink.get(n);
                                if (!psPath.contains(implementLinkInstancePID))
                                    implementLinkIsUpToDate = false;
                            }
                        }

                        if (implementLinkIsUpToDate) {
                            // Check if the scope is up to date
                            String implementLinkLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + implementLinkInstancePID + " select to.physicalid dump |", false, false);

                            List<String> modifiedInstanceList = FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomLeafRefPID, implementLinkLeafRefPID,
                                    true);

                            if (implementLinkLeafRefPID.equals(currentPSRefScopePID)) {
                                // throw new Exception("The scope is already up to date.");

                                errorBuffer.append("\"").append(mbomObjName).append("\"").append(":").append("The scope is already up to date").append("\n");
                            }
                        } else { // Implement link is not up to date : first, try to update it based on logicalids
                            String implementLinkLeafInstPID = null;
                            String implementLinkLeafRefPID = null;
                            int implementLinkUpdateInfo = FRCMBOMModelerUtility.updateImplementLinkFromCandidate(context, plmSession, mbomLeafInstPID);

                            if (implementLinkUpdateInfo == 0) { // Implement link is, in fact, up to date
                                // Get the implement link
                                implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomLeafInstPID);
                                implementLinkLeafInstPID = implementLink.get(implementLink.size() - 1);
                                implementLinkLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + implementLinkLeafInstPID + " select to.physicalid dump |", false, false);

                                List<String> modifiedInstanceList = FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomLeafRefPID, implementLinkLeafRefPID,
                                        true);

                                if (implementLinkLeafRefPID.equals(currentPSRefScopePID)) {

                                    errorBuffer.append("\"").append(mbomObjName).append("\"").append(":").append("The scope is already up to date.").append("\n");
                                }
                            } else if (implementLinkUpdateInfo == 3) { // Only the effectivity has been updated
                                // Get the implement link
                                implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomLeafInstPID);
                                implementLinkLeafInstPID = implementLink.get(implementLink.size() - 1);
                                implementLinkLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + implementLinkLeafInstPID + " select to.physicalid dump |", false, false);

                                List<String> modifiedInstanceList = FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomLeafRefPID, implementLinkLeafRefPID,
                                        true);

                                if (implementLinkLeafRefPID.equals(currentPSRefScopePID)) {
                                    // throw new Exception("The scope is already up to date.");
                                    errorBuffer.append("\"").append(mbomObjName).append("\"").append(":").append("The scope is already up to date.").append("\n");
                                }
                            } else if (implementLinkUpdateInfo == 1) { // Implement link is rerouted
                                // Get the new implement link
                                implementLink = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomLeafInstPID);
                                implementLinkLeafInstPID = implementLink.get(implementLink.size() - 1);
                                implementLinkLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + implementLinkLeafInstPID + " select to.physicalid dump |", false, false);
                            } else if (implementLinkUpdateInfo == 4) { // Implement link is broken : update it to the psPath
                                // if (psPath == null || "".equals(psPath))
                                if (UIUtil.isNotNullAndNotEmpty(psPath)) {
                                    // throw new Exception("The implement link of this Manufacturing Item is broken : you must also select a Part in the EBOM to update it.");

                                    String[] psPathList = psPath.split("/");
                                    if (psPathList.length < 2)
                                        throw new Exception("You cannot select the root Part for an intermediate scope update.");

                                    String trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomPath, psPath);

                                    if (trimmedPSPath == null || "".equals(trimmedPSPath))
                                        throw new Exception("No parent scope exists.");

                                    // Remove any existing implement link
                                    FRCMBOMModelerUtility.deleteImplementLink(context, plmSession, mbomLeafInstPID, true);

                                    // Put a new implement link and update the effectivity
                                    List<String> mbomLeafInstancePIDList = new ArrayList();
                                    mbomLeafInstancePIDList.add(mbomLeafInstPID);
                                    List<String> trimmedPSPathList = new ArrayList();
                                    trimmedPSPathList.add(trimmedPSPath);
                                    String retStr = FRCMBOMModelerUtility.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);
                                    if (!"".equals(retStr))
                                        throw new Exception(retStr);

                                    String[] trimmedPSPathPIDList = trimmedPSPath.split("/");
                                    implementLinkLeafInstPID = trimmedPSPathPIDList[trimmedPSPathPIDList.length - 1];
                                    implementLinkLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + implementLinkLeafInstPID + " select to.physicalid dump |", false, false);

                                    // Reconnection is done
                                    // Implement link is now up to date, but not the scope : update it to the leaf ref of the implement link
                                    List<String> modifiedInstanceList = FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomLeafRefPID,
                                            implementLinkLeafRefPID, true);

                                } else {
                                    errorBuffer.append("\"").append(mbomObjName).append("\"").append(":")
                                            .append("The implement link of this Manufacturing Item is broken : you must also select a Part in the EBOM to update it").append("\n");
                                }
                            }
                        }
                    } else {
                        errorBuffer.append("\"").append(mbomObjName).append("\"").append(":").append("Manufacturing Item you have selected does not have any implement link").append("\n");
                    }
                } else {
                    errorBuffer.append("\"").append(mbomObjName).append("\"").append(":").append("Manufacturing Item you have selected does not have any scope").append("\n");
                }
            } else {
                errorBuffer.append("\"").append(mbomObjName).append("\"").append(":").append("This type of Manufacturing Item is not valid for a sub-scope").append("\n");
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
        // FRC changes added for MBO-167 :H65:09/10/2017-END
        return errorBuffer.toString();
    }

    public static String updateImplementLink(Context context, String[] args) throws Exception { // Called from FRCUpdateImplementLink.jsp
        // Return value :
        // 0 = Nothing needs to be updated
        // 1 = The implement link is updated (and potentially the effectivity too)
        // 2 = The implement link is updated and the Manuf Item was replaced (case of a Provide)
        // 3 = Only the effectivity is updated
        // 4 = Link is broken
        // 5 = Intermediate scope is updated
        String returnValue = "0";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomCompletePath = args[0];
            String changeObjectPID = args[1];

            String[] mbomCompletePathList = mbomCompletePath.split("/");
            String mbomLeafInstancePID = mbomCompletePathList[mbomCompletePathList.length - 1];
            String mbomLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstancePID + " select to.physicalid dump |", false, false);

            int implementLinkUpdateInfo = FRCMBOMModelerUtility.updateImplementLinkFromCandidate(context, plmSession, mbomLeafInstancePID);

            if (implementLinkUpdateInfo == 0) { // Implement link is, in fact, up to date.
                returnValue = "0";
            } else if (implementLinkUpdateInfo == 1) { // Implement link is rerouted.
                returnValue = "1";

                // Get the new implement link
                // MapList newImplementLink = getImplementPIDList(context, mbomLeafInstancePID);
                List<String> newImplementLinkPIDList = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, mbomLeafInstancePID);

                // If the leaf MBOM reference is a Provide, get the synched MBOM from the leaf PS reference, and replace it in the MBOM structure
                String mbomLeafRefType = MqlUtil.mqlCommand(context, "print bus " + mbomLeafRefPID + " select type dump |", false, false);

                boolean isDirect = false;

                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                        isDirect = true;
                }

                String psLeafInstPID = newImplementLinkPIDList.get(newImplementLinkPIDList.size() - 1);
                String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstPID + " select to.physicalid dump |", false, false);

                if (isDirect) {

                    // Get a new synched Provide with the leaf PS reference
                    List<String> newRefPIDList = new ArrayList<String>();
                    String newProvidePID = getSynchedScopeMBOMRefFromPSRefForUpdateImplentLink(context, plmSession, psLeafRefPID, null, newRefPIDList);

                    // Attach all created references to change object.
                    attachListObjectsToChange(context, plmSession, changeObjectPID, newRefPIDList);

                    if (!newProvidePID.equals(mbomLeafRefPID)) { // The leaf MBOM reference is not the one synched with the leaf PS reference. Normally because it is a different revision
                        // Replace the MBOM leaf instance with the new one
                        String newMBOMLeafInstancePID = FRCMBOMModelerUtility.replaceMBOMInstance(context, plmSession, mbomLeafInstancePID, newProvidePID);

                        mbomCompletePath = mbomCompletePath.replace(mbomLeafInstancePID, newMBOMLeafInstancePID);
                        mbomLeafInstancePID = newMBOMLeafInstancePID;

                        returnValue = "2";
                    }
                } else { // Case of an intermediate scoped Create Assembly
                    // Implement link is now up to date, but not the scope : update it to the leaf ref of the implement link
                    List<String> modifiedInstanceList = FRCMBOMModelerUtility.reconnectScopeLinkAndUpdateChildImplementLinks(context, plmSession, mbomLeafRefPID, psLeafRefPID, true);
                    System.out.println("FRC : modified Instances : " + modifiedInstanceList);

                    returnValue = "5";
                }
            } else if (implementLinkUpdateInfo == 4) { // Implement link is broken
                returnValue = "4";
            } else if (implementLinkUpdateInfo == 3) { // Only the effectivity was updated
                returnValue = "3";
            }

            flushSession(plmSession);
            // HE5 : Added fix for the error "Transaction not active" after calling the R&D API replaceMBOMInstance
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnValue;
    }

    public static String getFinalRefPID(Context context, String[] args) throws Exception { // Called from FRCInsertExistingPreProcess2.jsp
        String returnValue = "";
        try {
            ContextUtil.startTransaction(context, false);

            String psPath = args[0];
            String[] psPathList = psPath.split("/");

            if (psPathList.length <= 1)
                throw new Exception("You cannot select the root Part.");

            String psFinalInstPID = psPathList[psPathList.length - 1];
            returnValue = MqlUtil.mqlCommand(context, "print connection " + psFinalInstPID + " select to.physicalid dump |", false, false);

            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnValue;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getAvailableScopesProgram(Context context, String[] args) throws Exception { // Called from FRCInsertExistingPreProcess2.jsp
        MapList returnML = new MapList();

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String objectPID = (String) paramMap.get("objectId");

            String mbomPath = (String) paramMap.get("mbomPath");
            // FRC Fixed Bug 1364-MBOM item shouldn't be inserted under itself:START: H65:02/03/2017
            String selectedchildID = (String) paramMap.get("selectedchildID");
            String mbomObjectPID = null;
            DomainObject mBomObj = null;
            StringList physicalIdList = new StringList();
            if (mbomPath.contains("/")) {
                String[] mbomPathList = mbomPath.split("/");

                mbomObjectPID = mbomPathList[0];
            } else
                mbomObjectPID = mbomPath;
            // Bug #1594-Wrong expand used-H65:20/09/2017-START

            // String strParentObjConnected = MqlUtil.mqlCommand(context, "expand bus " + selectedchildID + " terse to recurse to all dump |", false, false);

            /*
             * if(!strParentObjConnected.isEmpty()){ String[] mbomArray=strParentObjConnected.split("\\n");
             * 
             * for(int i=0; i<mbomArray.length;i++){ String strObjId=mbomArray[i]; StringList strSplit = FrameworkUtil.split(strObjId,"|"); String mbomObjId = (String) strSplit.get(3); String
             * mbomObjPhysicalId=MqlUtil.mqlCommand(context,"print bus " + mbomObjId + " select physicalid dump |", false, false); if(!physicalIdList.contains(mbomObjPhysicalId))
             * physicalIdList.add(mbomObjPhysicalId); } }
             */

            DomainObject domMfgItem = new DomainObject(selectedchildID);
            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_ID);
            objSelects.addElement(DomainConstants.SELECT_NAME);
            matrix.util.Pattern relPattern = new matrix.util.Pattern(PropertyUtil.getSchemaProperty(context, "relationship_DELFmiFunctionIdentifiedInstance"));

            MapList mlToConnected = domMfgItem.getRelatedObjects(context, relPattern.getPattern(), // relationshipPattern
                    "*", // typePattern
                    objSelects, // objectSelects
                    null, // relationshipSelects
                    true, // getTo
                    false, // getFrom
                    (short) 0, // recurseToLevel
                    null, // objectWhere,
                    null, // relationshipWhere
                    (int) 0); // limiDomainObject domMfgItem = new DomainObject(selectedchildID);

            if (mlToConnected.size() > 0) {

                for (int i = 0; i < mlToConnected.size(); i++) {
                    Map mapChild = (Map) mlToConnected.get(i);
                    String mbomObjId = (String) mapChild.get("id");
                    String mbomObjPhysicalId = MqlUtil.mqlCommand(context, "print bus " + mbomObjId + " select physicalid dump |", false, false);
                    if (!physicalIdList.contains(mbomObjPhysicalId))
                        physicalIdList.add(mbomObjPhysicalId);
                }
            }

            // Bug #1594-Wrong expand used-H65:20/09/2017-END
            physicalIdList.add(selectedchildID);

            List<String> mbomRefScopesList = FRCMBOMModelerUtility.getScopingReferencesFromList(context, plmSession, objectPID);

            for (String mbomRefScopePID : mbomRefScopesList) {
                Map objMap = new HashMap();

                if (!physicalIdList.contains(mbomRefScopePID)) {
                    objMap.put("id", mbomRefScopePID);
                    returnML.add(objMap);
                }
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
            physicalIdList.clear();
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnML;
    }

    public static void insertExistingManufItem(Context context, String[] args) throws Exception {// Called from FRCInsertExistingPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        PLMxRefInstanceEntity instanceEntity = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomPath = args[0];
            String mbomRefPID = args[1];
            String psPath = args[2];

            String[] mbomPathList = mbomPath.split("/");

            String mbomParentRefPID = null;

            if (mbomPathList.length == 1) {
                mbomParentRefPID = mbomPathList[0];
            } else {
                String mbomFinalInstPID = mbomPathList[mbomPathList.length - 1];
                mbomParentRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomFinalInstPID + " select to.physicalid dump |", false, false);
            }

            IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();
            String mbomParentRefPLMID = coreAccess.convertM1IDinPLMID(new String[] { mbomParentRefPID })[0];

            String mbomRefPLMID = coreAccess.convertM1IDinPLMID(new String[] { mbomRefPID })[0];

            // Create a new instance
            Hashtable htAttribute = new Hashtable();
            // String newInstPID = createManufItemInstance(context, mbomParentRefPID, mbomRefPID);
            instanceEntity = FRCMBOMModelerAPI.insertExistingPredecessor(context, plmSession, mbomRefPLMID, mbomParentRefPLMID, htAttribute);

            String instancePLMID = instanceEntity.getPLMIdentifier();
            PLMID newInstancePLMIDObj = PLMID.buildFromString(instancePLMID);

            String newInstPID = newInstancePLMIDObj.getPid();

            // Add the custom extension for the effectivity checksum
            MqlUtil.mqlCommand(context, "mod connection " + newInstPID + " add interface FRCCustoExtension1", false, false);

            // Create an implement link
            String trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomPath, psPath);

            if (trimmedPSPath == null || "".equals(trimmedPSPath))
                throw new Exception("No parent scope exists.");

            // Put a new implement link
            List<String> mbomLeafInstancePIDList = new ArrayList();
            mbomLeafInstancePIDList.add(newInstPID);
            List<String> trimmedPSPathList = new ArrayList();
            trimmedPSPathList.add(trimmedPSPath);
            String retStr = FRCMBOMModelerUtility.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);
            if (!"".equals(retStr))
                throw new Exception(retStr);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static String checkIsTypeForParentProvide(Context context, String[] args) throws Exception { // Called from FRCReplaceByRevisionPreProcess.jsp
        String returnValue = "";

        try {
            ContextUtil.startTransaction(context, false);

            String childRefPID = args[0];

            String childRefType = MqlUtil.mqlCommand(context, "print bus " + childRefPID + " select type dump |", false, false);

            boolean isTypeForParentProvide = false;
            for (String typeInList : baseTypesForMBOMAssemblyNodes) {
                if (getDerivedTypes(context, typeInList).contains(childRefType))
                    isTypeForParentProvide = true;
            }

            if (!isTypeForParentProvide) {
                returnValue = "The item selected is not a Manufacturing Assembly or Kit or a Disassemble.";
            }

            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            returnValue = exp.getMessage();
        }

        return returnValue;
    }

    public static String checkIsTypeForReplaceByNewManufMaterial(Context context, String[] args) throws Exception { // Called from FRCReplaceByNewManufMaterialPreProcess.jsp
        String returnValue = "";

        try {
            ContextUtil.startTransaction(context, false);

            String childRefPID = args[0];

            // Check the type of the initial reference
            String isProvideStr = MqlUtil.mqlCommand(context, "print bus " + childRefPID + " select type.kindof[Provide] dump |", false, false);
            String isContinuousProvideStr = MqlUtil.mqlCommand(context, "print bus " + childRefPID + " select type.kindof[ProcessContinuousProvide] dump |", false, false);

            if (!"TRUE".equalsIgnoreCase(isProvideStr) && !"TRUE".equalsIgnoreCase(isContinuousProvideStr))
                returnValue = "Manufacturing Item must be of type Provide or ContinuousProvide";

            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            returnValue = exp.getMessage();
        }

        return returnValue;
    }

    public static void replaceByNewManufMaterial(Context context, String[] args) throws Exception { // Called from FRCReplaceByNewManufMaterialPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomPath = args[0];
            String changeObjectPID = args[1];

            String[] mbomPathList = mbomPath.split("/");
            String mbomRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomPathList[mbomPathList.length - 1] + " select to.physicalid dump |", false, false);

            // Check the type of the initial reference
            String isProvideStr = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select type.kindof[Provide] dump |", false, false);
            String isContinuousProvideStr = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select type.kindof[ProcessContinuousProvide] dump |", false, false);

            if (!"TRUE".equalsIgnoreCase(isProvideStr) && !"TRUE".equalsIgnoreCase(isContinuousProvideStr))
                throw new Exception("Manufacturing Item must be of type Provide or ContinuousProvide");

            // Get the attribute values of the PS reference
            DomainObject mbomRefObj = new DomainObject(mbomRefPID);
            Map mbomRefAttributes = mbomRefObj.getAttributeMap(context, true);
            Map newMBOMRefAttributes = new HashMap();
            newMBOMRefAttributes.put("PLMEntity.V_Name", mbomRefAttributes.get("PLMEntity.V_Name"));
            newMBOMRefAttributes.put("PLMEntity.V_description", mbomRefAttributes.get("PLMEntity.V_description"));

            // Create a new MBOM reference
            HashMap<String, String> attributes = new HashMap<String, String>();

            String newMBOMRefPID = null;
            if ("TRUE".equalsIgnoreCase(isProvideStr)) {
                newMBOMRefPID = createMBOMReference(context, plmSession, "CreateMaterial", null, attributes);
            } else if ("TRUE".equalsIgnoreCase(isContinuousProvideStr)) {
                String magnitudeExtension = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select interface dump |", false, false);
                String magnitudeType = null;
                String quantity = "1.0";
                if (magnitudeExtension.contains("DELFmiContQuantity_Length")) {
                    magnitudeType = "Length";
                    quantity = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select attribute[DELFmiContQuantity_Length.V_ContQuantity].value dump |", false, false);
                    newMBOMRefAttributes.put("DELFmiContQuantity_Length.V_ContQuantity", quantity);
                } else if (magnitudeExtension.contains("DELFmiContQuantity_Mass")) {
                    magnitudeType = "Mass";
                    quantity = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select attribute[DELFmiContQuantity_Mass.V_ContQuantity].value dump |", false, false);
                    newMBOMRefAttributes.put("DELFmiContQuantity_Mass.V_ContQuantity", quantity);
                } else if (magnitudeExtension.contains("DELFmiContQuantity_Area")) {
                    magnitudeType = "Area";
                    quantity = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select attribute[DELFmiContQuantity_Area.V_ContQuantity].value dump |", false, false);
                    newMBOMRefAttributes.put("DELFmiContQuantity_Area.V_ContQuantity", quantity);
                } else if (magnitudeExtension.contains("DELFmiContQuantity_Volume")) {
                    magnitudeType = "Volume";
                    quantity = MqlUtil.mqlCommand(context, "print bus " + mbomRefPID + " select attribute[DELFmiContQuantity_Volume.V_ContQuantity].value dump |", false, false);
                    newMBOMRefAttributes.put("DELFmiContQuantity_Volume.V_ContQuantity", quantity);
                }

                newMBOMRefPID = createMBOMReference(context, plmSession, "ProcessContinuousCreateMaterial", magnitudeType, attributes);
            }

            // Attach the new reference to the change object
            attachObjectToChange(context, plmSession, changeObjectPID, newMBOMRefPID);

            // Replicate all the attributes values on the new MBOM reference
            DomainObject newMBOMRefObj = new DomainObject(newMBOMRefPID);
            newMBOMRefObj.setAttributeValues(context, newMBOMRefAttributes);

            // Get the scope on the old reference
            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(mbomRefPID);
            String scopePSRefPID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

            // Delete the scope on the old reference
            List<String> modifiedInstanceList = FRCMBOMModelerUtility.deleteScopeLinkAndDeleteChildImplementLinks(context, plmSession, mbomRefPID, false);
            System.out.println("FRC : modified Instances : " + modifiedInstanceList);

            // Set the scope on the new reference
            if (scopePSRefPID != null && !"".equals(scopePSRefPID))
                FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newMBOMRefPID, scopePSRefPID, false);

            // Replace the instance
            FRCMBOMModelerUtility.replaceMBOMInstance(context, plmSession, mbomPathList[mbomPathList.length - 1], newMBOMRefPID);

            flushAndCloseSession(plmSession);
            // HE5 : Added fix for the error "Transaction not active" after calling the R&D API replaceMBOMInstance
            if (context.isTransactionActive())
                ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static void insertDuplicatedManufItem(Context context, String[] args) throws Exception { // Called from FRCInsertDuplicatedPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomPath = args[0];
            String templateRefPID = args[1];
            String changeObjectName = args[2];

            // Recursively clone the template root node
            String mbomRefPID = duplicateMBOMStructure(context, plmSession, templateRefPID, changeObjectName);

            // Create a new instance
            String[] mbomPathList = mbomPath.split("/");
            String mbomParentRefPID = null;
            if (mbomPath.contains("/"))
                mbomParentRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomPathList[mbomPathList.length - 1] + " select to.physicalid dump |", false, false);
            else
                mbomParentRefPID = MqlUtil.mqlCommand(context, "print bus " + mbomPath + " select physicalid dump |", false, false);

            createInstance(context, plmSession, mbomParentRefPID, mbomRefPID);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static void deleteScope(Context context, String[] args) throws Exception { // Called from FRCDeleteScopePostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomPath = args[0];

            String[] mbomPathList = mbomPath.split("/");
            String mbomRefPID = null;
            if (mbomPath.contains("/"))
                mbomRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomPathList[mbomPathList.length - 1] + " select to.physicalid dump |", false, false);
            else
                mbomRefPID = MqlUtil.mqlCommand(context, "print bus " + mbomPath + " select physicalid dump |", false, false);

            // Remove the scope link on the root reference
            List<String> modifiedInstanceList = FRCMBOMModelerUtility.deleteScopeLinkAndDeleteChildImplementLinks(context, plmSession, mbomRefPID, true);
            System.out.println("FRC : modified Instances : " + modifiedInstanceList);

            // Remove the implement link on the instance (Careful : if we are on the root, there is no instance)
            if (mbomPath.contains("/")) {
                FRCMBOMModelerUtility.deleteImplementLink(context, plmSession, mbomPathList[mbomPathList.length - 1], true);
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
        // throw new Exception("There is no scope to delete.");
    }

    public static StringList canEditDescription(Context context, String[] args) throws Exception { // Called from table FRCMBOMTable (column Description)
        /**
         * This method is to restrict editing for Description if no CO is selected by user.
         * @param context
         * @param args
         * @author kyb
         * @return StringList
         * @throws Exception
         */
        StringList lRet = new StringList();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        Map requestMap = (Map) programMap.get("requestMap");

        MapList relBusObjPageList = (MapList) programMap.get("objectList");
        for (int i = 0; i < relBusObjPageList.size(); i++) {
            if (0 == i)
                lRet.addElement("false");
            else {
                String sCanEdit = "true";
                lRet.addElement(sCanEdit);
            }
        }

        return lRet;
    }

    // ==== START : Methods for managing attributes in Create and Properties form
    public static boolean isCreate(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.Form.Label.ChangeObject and the
                                                                                      // following empty field)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String nameField = null;
        if (requestMap != null)
            nameField = (String) requestMap.get("nameField");
        else
            nameField = (String) programMap.get("nameField");

        return !(nameField == null || "".equals(nameField));
    }

    public static boolean isProperties(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.MBOMManufItemIdentifier and
                                                                                          // FRCMBOMCentral.MBOMTableColumnMaturity and FRCMBOMCentral.MBOMTableColumnRevision and
                                                                                          // FRCMBOMCentral.MBOMManufItemCreationDate and FRCMBOMCentral.MBOMManufItemModificationDate and
                                                                                          // FRCMBOMCentral.MBOMManufItemResponsible and FRCMBOMCentral.MBOMManufItemOrganization and
                                                                                          // FRCMBOMCentral.MBOMManufItemCollabSpace)
        return !isCreate(context, args);
    }

    public static boolean getAccessToManufItemMagnitudeFieldForCreate(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                                         // FRCMBOMCentral.MBOMManufItemMagnitude)
        return getAccessToManufItemMagnitudeField(context, args) && isCreate(context, args);
    }

    public static boolean getAccessToManufItemMagnitudeFieldForForm(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                                       // FRCMBOMCentral.MBOMManufItemMagnitude)
        return getAccessToManufItemMagnitudeField(context, args) && isProperties(context, args);
    }

    public static boolean isKindOfForWebForm(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                // FRCMBOMCentral.MBOMManufItemPartNumber and FRCMBOMCentral.MBOMManufItemMakeBuy and
                                                                                                // FRCMBOMCentral.MBOMManufItemAssemblyName and FRCMBOMCentral.MBOMManufItemOutsourced and
                                                                                                // FRCMBOMCentral.MBOMManufItemProcessRequired and FRCMBOMCentral.MBOMManufItemProcessRequired and
                                                                                                // FRCMBOMCentral.MBOMManufItemProcessRequired)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String typeStr = (String) programMap.get("type");

        Map<String, String> settings = (Map<String, String>) programMap.get("SETTINGS");
        String listTypesToCheckStr = settings.get("FRCTypesToCheck");
        String[] listTypesToCheck = null;

        if (listTypesToCheckStr == null)
            return false;
        else {
            listTypesToCheck = listTypesToCheckStr.split(",");
        }

        String type = "";
        if (typeStr == null || "".equals(typeStr)) {
            String objectId = (String) programMap.get("objectId");
            type = MqlUtil.mqlCommand(context, "print bus " + objectId + " select type dump |", false, false);
        } else {
            int indexEnd = typeStr.indexOf(",");
            if (typeStr.startsWith("_selectedType:")) {
                if (indexEnd > 0)
                    type = typeStr.substring(14, indexEnd);
                else
                    type = typeStr.substring(14);
            } else {
                if (indexEnd > 0)
                    type = typeStr.substring(0, indexEnd);
                else
                    type = typeStr.substring(0);
            }
        }

        boolean returnValue = false;

        for (String typeToCheck : listTypesToCheck) {
            String ret = MqlUtil.mqlCommand(context, "print type " + type + " select kindof[" + typeToCheck + "] dump |", false, false);
            if (ret.equalsIgnoreCase("TRUE"))
                returnValue = true;
        }

        return returnValue;
    }

    public static boolean getAccessToManufItemMagnitudeField(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (empty field after
                                                                                                                // FRCMBOMCentral.MBOMManufItemProcessRequired)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String typeStr = (String) programMap.get("type");

        String type = "";
        if (typeStr == null || "".equals(typeStr)) {
            String objectId = (String) programMap.get("objectId");
            type = MqlUtil.mqlCommand(context, "print bus " + objectId + " select type dump |", false, false);
        } else {
            int indexEnd = typeStr.indexOf(",");
            if (typeStr.startsWith("_selectedType:")) {
                if (indexEnd > 0)
                    type = typeStr.substring(14, indexEnd);
                else
                    type = typeStr.substring(14);
            } else {
                if (indexEnd > 0)
                    type = typeStr.substring(0, indexEnd);
                else
                    type = typeStr.substring(0);
            }
        }

        if ("ProcessContinuousCreateMaterial".equals(type) || "ProcessContinuousProvide".equals(type))
            return true;
        else
            return false;
    }

    public static boolean getAccessToManufItemQuantityField(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                               // FRCMBOMCentral.MBOMManufItemQuantity)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String typeStr = (String) programMap.get("type");

        String type = "";
        if (typeStr == null || "".equals(typeStr)) {
            String objectId = (String) programMap.get("objectId");
            type = MqlUtil.mqlCommand(context, "print bus " + objectId + " select type dump |", false, false);
        } else {
            int indexEnd = typeStr.indexOf(",");
            if (typeStr.startsWith("_selectedType:")) {
                if (indexEnd > 0)
                    type = typeStr.substring(14, indexEnd);
                else
                    type = typeStr.substring(14);
            } else {
                if (indexEnd > 0)
                    type = typeStr.substring(0, indexEnd);
                else
                    type = typeStr.substring(0);
            }
        }

        // FRC: Fixed Bug 230-Quantity management is wrong, Need to hide 'Quantity' field for 'Continous Provided Material' and 'Continuous Manufactured Material'
        // EPI : I do not understand why : continuous objects have this Quantity field that needs to be visible and editable
        /*
         * String isContinuous = MqlUtil.mqlCommand(context, "print type '" + type + "' select kindof[DELFmiContinuousFunctionReference] dump |", false, false);
         * 
         * if ("TRUE".equalsIgnoreCase(isContinuous)) return true; else return false;
         */

        if ("ProcessContinuousCreateMaterial".equals(type) || "ProcessContinuousProvide".equals(type))
            return true;
        else
            return false;
    }

    public static StringList getListForManufItemMagnitudeField(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                                  // FRCMBOMCentral.MBOMManufItemMagnitude)
        StringList returnList = new StringList();

        Locale loc = context.getLocale();

        for (String key : listMagnitudeFieldKeys) {
            returnList.add(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key));
        }

        return returnList;
    }

    public static String getMagnitude(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.MBOMManufItemMagnitude and
                                                                                         // FRCMBOMCentral.MBOMManufItemMagnitude)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        String returnStr = "";
        String interfaceListStr = MqlUtil.mqlCommand(context, "print bus " + objectId + " select interface dump |", false, false);
        if (!"".equals(interfaceListStr)) {
            String[] interfaceList = interfaceListStr.split("\\|");
            String key = null;
            for (String interfaceName : interfaceList) {
                if ("DELFmiContQuantity_Length".equals(interfaceName))
                    key = "FRCMBOMCentral.MBOMManufItemMagnitudeLength";
                else if ("DELFmiContQuantity_Mass".equals(interfaceName))
                    key = "FRCMBOMCentral.MBOMManufItemMagnitudeMass";
                else if ("DELFmiContQuantity_Area".equals(interfaceName))
                    key = "FRCMBOMCentral.MBOMManufItemMagnitudeArea";
                else if ("DELFmiContQuantity_Volume".equals(interfaceName))
                    key = "FRCMBOMCentral.MBOMManufItemMagnitudeVolume";
            }
            if (key != null) {
                Locale loc = context.getLocale();
                returnStr = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key);
            }
        }

        return returnStr;
    }

    public static String getQuantity(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.MBOMManufItemQuantity)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        String value = "";
        if (objectId == null || "".equals(objectId))
            value = "1.0";
        else {
            value = MqlUtil.mqlCommand(context, "print bus " + objectId + " select attribute[V_ContQuantity].value dump |", false, false);
        }
        return value;
    }

    public static String retrieveQuantity(Context context, String[] args) throws Exception { // Called from form FRCMBOMSetQuantity (field FRCMBOMCentral.MBOMManufItemQuantity)
        // FRC: Fixed Bug 230-Quantity management is wrong
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");
        String relId = (String) paramMap.get("relId");
        String quantity = "1";

        String refQuantityValue = "";
        if (objectId == null || "".equals(objectId))
            refQuantityValue = "1.0";
        else {
            refQuantityValue = MqlUtil.mqlCommand(context, "print bus " + objectId + " select attribute[V_ContQuantity].value dump |", false, false);
        }

        if (relId != null && !"".equals(relId)) {
            String instanceUsageCoefficient = MqlUtil.mqlCommand(context, "print connection " + relId + " select attribute[ProcessInstanceContinuous.V_UsageContCoeff].value dump |", false, false);

            double refQuantity = Double.parseDouble(refQuantityValue);
            double instUsageCoeff = Double.parseDouble(instanceUsageCoefficient);
            double quantityVal = refQuantity * instUsageCoeff;

            quantity = Double.toString(quantityVal);
        }

        return quantity;
    }

    public static void setQuantity(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.MBOMManufItemQuantity)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        String newValue = (String) paramMap.get("New Value");

        String attributeNameListStr = MqlUtil.mqlCommand(context, "print bus " + objectId + " select attribute dump |", false, false);
        String[] attributeNameList = attributeNameListStr.split("\\|");
        String attributeName = null;
        for (String name : attributeNameList) {
            if (name.startsWith("DELFmiContQuantity_"))
                attributeName = name;
        }

        MqlUtil.mqlCommand(context, "mod bus " + objectId + " '" + attributeName + "' '" + newValue + "'", false, false);
    }

    public static void updateQuantity(Context context, String[] args) throws Exception { // Called from form FRCMBOMSetQuantity (field FRCMBOMCentral.MBOMManufItemQuantity)
        // FRC: Fixed Bug 230-Quantity management is wrong
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");
        String relId = (String) paramMap.get("relId");
        String newValue = (String) paramMap.get("New Value");

        String refQuantityValue = "";
        if (objectId == null || "".equals(objectId))
            refQuantityValue = "1.0";
        else {
            refQuantityValue = MqlUtil.mqlCommand(context, "print bus " + objectId + " select attribute[V_ContQuantity].value dump |", false, false);
        }

        double refQuantity = Double.parseDouble(refQuantityValue);
        double newVal = Double.parseDouble(newValue);
        double usageCoeff = newVal / refQuantity;
        String instanceUsgaeCoefficient = Double.toString(usageCoeff);

        if (relId != null && !"".equals(relId)) {
            MqlUtil.mqlCommand(context, "mod connection " + relId + " " + "ProcessInstanceContinuous.V_UsageContCoeff" + " " + instanceUsgaeCoefficient, false, false);
        }
    }

    public static StringList getListForMakeBuyField(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                       // FRCMBOMCentral.MBOMManufItemMakeBuy)
        StringList returnList = new StringList();

        Locale loc = context.getLocale();

        for (String key : listMakeBuyFieldKeys) {
            returnList.add(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key));
        }

        return returnList;
    }

    public static String getMakeBuy(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.MBOMManufItemMakeBuy)
        String returnValue = "";

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        Locale loc = context.getLocale();
        if (objectId != null && !"".equals(objectId)) {
            String key = "FRCMBOMCentral.Undefined";
            String makeBuyIntStr = MqlUtil.mqlCommand(context, "print bus " + objectId + " select attribute[CreateAssembly.V_Manufacturing_Decision] dump |", false, false);
            if (!"".equals(makeBuyIntStr)) {
                if ("2".equals(makeBuyIntStr))
                    key = "FRCMBOMCentral.Make";
                else if ("3".equals(makeBuyIntStr))
                    key = "FRCMBOMCentral.Buy";
            }

            returnValue = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key);
        } else {
            returnValue = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, "FRCMBOMCentral.Undefined");
        }

        return returnValue;
    }

    public static void setMakeBuy(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.MBOMManufItemMakeBuy)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        String makeBuyFieldValue = (String) paramMap.get("New Value");

        Locale loc = context.getLocale();

        String makeBuyFieldKey = "";
        for (String key : listMakeBuyFieldKeys) {
            if (makeBuyFieldValue.equals(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key)))
                makeBuyFieldKey = key;
        }

        String makeBuyAttributeValue = "1";
        if ("FRCMBOMCentral.Make".equals(makeBuyFieldKey))
            makeBuyAttributeValue = "2";
        else if ("FRCMBOMCentral.Buy".equals(makeBuyFieldKey))
            makeBuyAttributeValue = "3";

        MqlUtil.mqlCommand(context, "mod bus " + objectId + " 'CreateAssembly.V_Manufacturing_Decision' '" + makeBuyAttributeValue + "'", false, false);
    }

    public static StringList getListForYesNoField(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                     // FRCMBOMCentral.MBOMManufItemOutsourced and FRCMBOMCentral.MBOMManufItemProcessRequired and
                                                                                                     // FRCMBOMCentral.MBOMManufItemProcessRequired and FRCMBOMCentral.MBOMManufItemProcessRequired)
        StringList returnList = new StringList();

        Locale loc = context.getLocale();

        for (String key : listYesNoFieldKeys) {
            returnList.add(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key));
        }

        return returnList;
    }

    public static String getOutsourced(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.MBOMManufItemOutsourced)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        return getYesNo(context, objectId, "CreateAssembly.V_Outsourced");
    }

    public static String getCreateAssemblyProcessRequired(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                             // FRCMBOMCentral.MBOMManufItemProcessRequired)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        return getYesNo(context, objectId, "CreateAssembly.V_NeedDedicatedSystem");
    }

    public static String getCreateMaterialProcessRequired(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                             // FRCMBOMCentral.MBOMManufItemProcessRequired)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        return getYesNo(context, objectId, "CreateMaterial.V_NeedDedicatedSystem");
    }

    public static String getProcessContinuousCreateMaterialProcessRequired(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                                              // FRCMBOMCentral.MBOMManufItemProcessRequired)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");

        return getYesNo(context, objectId, "ProcessContinuousCreateMaterial.V_NeedDedicatedSystem");
    }

    public static void setOutsourced(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.MBOMManufItemOutsourced)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");
        String yesNoFieldValue = (String) paramMap.get("New Value");

        setYesNo(context, objectId, yesNoFieldValue, "CreateAssembly.V_Outsourced");
    }

    public static void setCreateAssemblyProcessRequired(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                           // FRCMBOMCentral.MBOMManufItemProcessRequired)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");
        String yesNoFieldValue = (String) paramMap.get("New Value");

        setYesNo(context, objectId, yesNoFieldValue, "CreateAssembly.V_NeedDedicatedSystem");
    }

    public static void setCreateMaterialProcessRequired(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                           // FRCMBOMCentral.MBOMManufItemProcessRequired)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");
        String yesNoFieldValue = (String) paramMap.get("New Value");

        setYesNo(context, objectId, yesNoFieldValue, "CreateMaterial.V_NeedDedicatedSystem");
    }

    public static void setProcessContinuousCreateMaterialProcessRequired(Context context, String[] args) throws Exception { // Called from form FRCMBOMManufItemCreateAndPropertiesForm (field
                                                                                                                            // FRCMBOMCentral.MBOMManufItemProcessRequired)
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");
        String yesNoFieldValue = (String) paramMap.get("New Value");

        setYesNo(context, objectId, yesNoFieldValue, "ProcessContinuousCreateMaterial.V_NeedDedicatedSystem");
    }

    public static HashMap getChangeObjectListForm(Context context, String[] args) throws Exception { // Called from FRCUpdateCreateFormAfterChangeCreation.jsp and from form
                                                                                                     // FRCMBOMManufItemCreateAndPropertiesForm (field FRCMBOMCentral.Form.Label.ChangeObject) and form
                                                                                                     // FRCCreateMBOM (field FRCMBOMCentral.Form.Label.ChangeObject)
        HashMap retMap = new HashMap();
        StringList slOIDValues = new StringList();
        StringList slDisplayValues = new StringList();

        try {
            ContextUtil.startTransaction(context, false);
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");
            MapList mlCAs = getChangeObjects(context);

            String sSelectedChange = (String) requestMap.get("FRCMBOMGetChangeObject");
            String forceSelectChange = (String) requestMap.get("forceSelectChange");
            boolean firstValueSet = false;
            ArrayList<String> listPIDs = new ArrayList<String>();// Avoid having objects twice

            if (null != mlCAs) {
                for (Object obj : mlCAs) {
                    Map map = (Map) obj;
                    String nameObj = (String) map.get("name");
                    String revObj = (String) map.get("revision");
                    String pIDObj = (String) map.get("physicalid");

                    if (!listPIDs.contains(pIDObj)) {// Avoid having objects twice
                        if (sSelectedChange != null && !"".equals(sSelectedChange) && sSelectedChange.equals(pIDObj)) {
                            slOIDValues.add(0, pIDObj);
                            slDisplayValues.add(0, nameObj + " " + revObj);
                            firstValueSet = true;
                        } else {
                            slOIDValues.add(pIDObj);
                            slDisplayValues.add(nameObj + " " + revObj);
                        }
                        listPIDs.add(pIDObj);
                    }
                }
            }

            // Propose null value only when creating new root object, not for add new from an indented table
            // CO Dissociated
            if ((null == sSelectedChange && null == forceSelectChange) || null == forceSelectChange) {
                if (firstValueSet) {
                    slOIDValues.add(1, null);
                    slDisplayValues.add(1, "-none-");
                } else {
                    slOIDValues.add(0, null);
                    slDisplayValues.add(0, "-none-");
                }
            }

            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            ex.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw ex;
        }
        retMap.put("field_choices", slOIDValues);
        retMap.put("field_display_choices", slDisplayValues);

        return retMap;
    }

    public int checkAffectedItemsState(Context context, String[] args) throws Exception { // Called by trigger business objects : "eService Trigger Program Parameters"
                                                                                          // PolicyChangeActionStateInApprovalPromoteCheck FRCCheckAffectedItemsState and "eService Trigger Program
                                                                                          // Parameters" PolicyChangeActionStateInWorkPromoteCheck FRCCheckAffectedItemsState
        int oReturn = 0;
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            String strChangeActionId = args[0];
            // String strCurrentState = args[1];
            String strTargetState = args[2];

            StringList objSelects = new StringList(DomainConstants.SELECT_ID);
            objSelects.addElement(DomainConstants.SELECT_TYPE);
            objSelects.addElement(DomainConstants.SELECT_POLICY);
            objSelects.addElement(DomainConstants.SELECT_CURRENT);

            MapList ImplementedItems = null;
            Map<String, String> implementedItemMap;
            String relWhereClause = "attribute[" + ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE + "] == '" + ChangeConstants.FOR_RELEASE + "'";

            // boolean strAutoApproveValue = false;
            boolean bRelease = strTargetState.equalsIgnoreCase("state_Release");
            boolean bApprove = strTargetState.equalsIgnoreCase("state_InApproval");

            if (!ChangeUtil.isNullOrEmpty(strTargetState) && !ChangeUtil.isNullOrEmpty(strChangeActionId) && (bRelease || bApprove)) {
                // Get the Implemented Items connected.
                DomainObject domChange = new DomainObject(strChangeActionId);
                ImplementedItems = domChange.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, // relationship pattern
                        "*", // object pattern
                        objSelects, // object selects
                        new StringList(DomainRelationship.SELECT_ID), // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        relWhereClause, (short) 0); // relationship where clause

                // Map relItemTypPolicyDtls = new HashMap();
                // Set the Approved State on the Implemented Items
                for (Object var : ImplementedItems) {
                    implementedItemMap = (Map<String, String>) var;
                    String strItem = implementedItemMap.get(DomainConstants.SELECT_ID);
                    String strItemType = implementedItemMap.get(DomainConstants.SELECT_TYPE);
                    String strItemPolicy = implementedItemMap.get(DomainConstants.SELECT_POLICY);
                    if ("VPLM_SMB_Definition".equals(strItemPolicy)) {
                        String stateApprovedMapping = ECMAdmin.getApproveStateValue(context, strItemType, strItemPolicy);
                        // strAutoApproveValue = ECMAdmin.getAutoApprovalValue(context, strItemType, strItemPolicy);
                        // if (strAutoApproveValue && !ChangeUtil.isNullOrEmpty(stateApprovedMapping)) {
                        if (!ChangeUtil.isNullOrEmpty(stateApprovedMapping)) {

                            String sCurrent = implementedItemMap.get(DomainConstants.SELECT_CURRENT);
                            // Send notice and block promotion if both are not connected.
                            if (!stateApprovedMapping.equals(sCurrent)) {
                                emxContextUtil_mxJPO.mqlNotice(context, EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FRCMBOMCentral.PromoteCA.NotFrozenChild", context.getLocale()));
                                oReturn = 1;
                                break;
                            }
                            // In case of Release, we check that the ToRelease transition has been signed
                            else if (bRelease) {
                                DomainObject dObject = new DomainObject(strItem);
                                MapList sigDetailsList = dObject.getSignaturesDetails(context, sCurrent, "RELEASED");
                                Map sigDetails = (Map) sigDetailsList.get(0);
                                String sigRelApproved = (String) sigDetails.get("approved");
                                String sigRelSigned = (String) sigDetails.get("signed");
                                sigDetailsList = dObject.getSignaturesDetails(context, sCurrent, "IN_WORK");
                                sigDetails = (Map) sigDetailsList.get(0);
                                String sigUnFreezeApproved = (String) sigDetails.get("approved");
                                String sigUnFreezeSigned = (String) sigDetails.get("signed");

                                if ((!"TRUE".equalsIgnoreCase(sigRelApproved) && !"TRUE".equalsIgnoreCase(sigRelSigned))
                                        || ("TRUE".equalsIgnoreCase(sigUnFreezeApproved) || "TRUE".equalsIgnoreCase(sigUnFreezeSigned))) {
                                    emxContextUtil_mxJPO.mqlNotice(context,
                                            EnoviaResourceBundle.getProperty(context, "FRCMBOMCentral", "FRCMBOMCentral.PromoteCA.NotSignedForRelease", context.getLocale()));
                                    oReturn = 1;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            oReturn = 1;
        }

        return oReturn;
    }
    // ==== END : Methods for managing attributes in Create and Properties form

    // ==== START : Configuration
    public HashMap getProductConfigurationListFromModel(Context context, String[] args) { // Called from command FRCGetProductConfigurationCmd
        HashMap retMap = new HashMap();

        boolean bTrans = false;
        try {
            ContextUtil.startTransaction(context, false);
            bTrans = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");
            StringList slOIDValues = new StringList();
            StringList slDisplayValues = new StringList();
            slOIDValues.add("");
            slDisplayValues.add("-none-");

            String sId;
            if (null != requestMap) {
                sId = (String) requestMap.get("objectId");
                if (null != sId && !"".equals(sId)) {
                    DomainObject domObj = new DomainObject(sId);
                    StringList objSelects = new StringList();
                    objSelects.addElement(DomainConstants.SELECT_ID);
                    objSelects.addElement(DomainConstants.SELECT_NAME);
                    objSelects.addElement(DomainConstants.SELECT_TYPE);
                    String sObjTypes = ProductLineConstants.TYPE_HARDWARE_PRODUCT + "," + ProductLineConstants.TYPE_PRODUCT_CONFIGURATION;
                    String sRelTypes = ProductLineConstants.RELATIONSHIP_MAIN_PRODUCT + "," + ProductLineConstants.RELATIONSHIP_PRODUCTS + ","
                            + ProductLineConstants.RELATIONSHIP_PRODUCT_CONFIGURATION;
                    MapList mlPC = domObj.getRelatedObjects(context, sRelTypes, sObjTypes, objSelects, null, false, true, (short) 2, null, null);
                    if (null != mlPC) {
                        for (int j = 0; j < mlPC.size(); j++) {
                            Map mObj = (Map) mlPC.get(j);
                            String sType = (String) mObj.get(DomainConstants.SELECT_TYPE);
                            if (ProductLineConstants.TYPE_PRODUCT_CONFIGURATION.equals(sType)) {
                                String sPCId = (String) mObj.get(DomainConstants.SELECT_ID);
                                String sPCName = (String) mObj.get(DomainConstants.SELECT_NAME);
                                String[] aExp = getExpressionForPC(context, sPCId);
                                if (null != aExp && 0 < aExp.length)
                                    slOIDValues.add(aExp[0]);
                                slDisplayValues.add(sPCName);
                            }
                        }
                    }
                }
            }
            retMap.put("field_choices", slOIDValues);
            retMap.put("field_display_choices", slDisplayValues);

            if (bTrans)
                ContextUtil.commitTransaction(context);

        } catch (Exception exp) {
            if (bTrans)
                ContextUtil.abortTransaction(context);
            exp.printStackTrace();
        }

        return retMap;
    }

    public HashMap getProductRevisionList(Context context, String[] args) { // Called from command FRCGetProductRevisionCmd
        HashMap retMap = new HashMap();
        boolean bTrans = false;
        try {
            ContextUtil.startTransaction(context, false);
            bTrans = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");
            StringList slOIDValues = new StringList();
            StringList slDisplayValues = new StringList();

            String sId;
            if (null != requestMap) {
                sId = (String) requestMap.get("objectId");
                if (null != sId && !"".equals(sId)) {
                    DomainObject domObj = new DomainObject(sId);
                    StringList objSelects = new StringList();
                    objSelects.addElement(DomainConstants.SELECT_ID);
                    objSelects.addElement(DomainConstants.SELECT_NAME);
                    objSelects.addElement(DomainConstants.SELECT_TYPE);
                    objSelects.addElement(DomainConstants.SELECT_REVISION);
                    String sObjTypes = ProductLineConstants.TYPE_HARDWARE_PRODUCT;
                    String sRelTypes = ProductLineConstants.RELATIONSHIP_MAIN_PRODUCT + "," + ProductLineConstants.RELATIONSHIP_PRODUCTS;
                    MapList mlProdRev = domObj.getRelatedObjects(context, sRelTypes, sObjTypes, objSelects, null, false, true, (short) 1, null, null);
                    if (null != mlProdRev) {
                        for (int j = 0; j < mlProdRev.size(); j++) {
                            Map mObj = (Map) mlProdRev.get(j);
                            String sType = (String) mObj.get(DomainConstants.SELECT_TYPE);
                            if (ProductLineConstants.TYPE_HARDWARE_PRODUCT.equals(sType)) {
                                String sProdId = (String) mObj.get(DomainConstants.SELECT_ID);
                                String sProdName = (String) mObj.get(DomainConstants.SELECT_NAME);
                                String sProdRevision = (String) mObj.get(DomainConstants.SELECT_REVISION);
                                slOIDValues.add(sProdId);
                                slDisplayValues.add(sProdName + " " + sProdRevision);
                            }
                        }
                    }
                }
            }
            if (0 == slOIDValues.size()) {
                slOIDValues.add("");
                slDisplayValues.add("-none-");
            }

            retMap.put("field_choices", slOIDValues);
            retMap.put("field_display_choices", slDisplayValues);
            if (bTrans)
                ContextUtil.commitTransaction(context);

        } catch (Exception exp) {
            if (bTrans)
                ContextUtil.abortTransaction(context);
            exp.printStackTrace();
        }

        return retMap;
    }

    public MapList getAttachedModels(Context context, String[] args) { // Called from command FRCExpressionFilterInput and from graphicalbrowser\DSTreeActions.js
        MapList lRet = new MapList();
        PLMCoreModelerSession plmSession = null;
        try {
            // FRC: Fixed Bug 532,418 related to Feature Options and Effectivity filter
            if (!context.isTransactionActive()) {
                context.start(false);
            }
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");
                List<String> lModels = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sId);
                for (int i = 0; i < lModels.size(); i++) {
                    HashMap tmpMap = new HashMap();
                    // tmpMap.put(DomainConstants.SELECT_ID, getOID(context, (String) lModels[i]));
                    tmpMap.put(DomainConstants.SELECT_ID, lModels.get(i));
                    lRet.add(tmpMap);
                }
            }

            // FRC: Fixed Bug 532,418 related to Feature Options and Effectivity filter
            if (context.isTransactionActive())
                context.abort();

        } catch (Exception exp) {
            exp.printStackTrace();
        } finally {
            closeSession(plmSession);
        }

        return lRet;
    }

    public MapList getAttachedModelsFromRoot(Context context, String[] args) { // Called from command FRCMBOMTableCmd
        MapList lRet = new MapList();
        PLMCoreModelerSession plmSession = null;
        try {

            // FRC: Fixed Bug 532,418 related to Feature Options and Effectivity filter
            if (!context.isTransactionActive()) {
                context.start(false);
            }

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                HashMap reqMap = (HashMap) programMap.get("RequestValuesMap");
                String sId = "";
                if (null != reqMap) {
                    String[] aId = (String[]) reqMap.get("rootID");
                    if (null != aId && 0 < aId.length)
                        sId = aId[0];
                }
                List<String> lModels = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sId);
                for (int i = 0; i < lModels.size(); i++) {
                    HashMap tmpMap = new HashMap();
                    // tmpMap.put(DomainConstants.SELECT_ID, getOID(context, (String) lModels.get(i)));
                    tmpMap.put(DomainConstants.SELECT_ID, lModels.get(i));
                    lRet.add(tmpMap);
                }
            }
            // FRC: Fixed Bug 532,418 related to Feature Options and Effectivity filter
            if (context.isTransactionActive())
                context.abort();

        } catch (Exception exp) {
            exp.printStackTrace();
        } finally {
            closeSession(plmSession);
        }

        return lRet;
    }

    public static StringList canEditEffectivity(Context context, String[] args) throws Exception { // Called from table FRCMBOMTable (column Diversity)
        // Define when the effectivity can be edited on the MBOM structure
        /**
         * KYB Modified this method to restrict editing Effectivity if no CO is selected by user.
         * @param context
         * @param args
         * @return StringList
         * @throws Exception
         */
        StringList lRet = new StringList();

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map requestMap = (Map) programMap.get("requestMap");
            String changeObjectName = (String) requestMap.get("FRCToolbarGetChangeObjectCmdValue");

            boolean coFlag = true;

            List<String> listInstIDs = new ArrayList<String>();

            List<Map<String, Object>> resImp = null;

            MapList relBusObjPageList = (MapList) programMap.get("objectList");
            for (int i = 0; i < relBusObjPageList.size(); i++) {
                Map mapObj = (Map) relBusObjPageList.get(i);
                String relId = (String) mapObj.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                if (relId != null && !"".equals(relId)) {
                    listInstIDs.add(relId);
                }
            }

            if (listInstIDs.size() > 0 && coFlag == true) {
                resImp = FRCMBOMModelerUtility.getImplementLinkInfoWithEffStatus(context, plmSession, listInstIDs, false);
            }

            int index = 0;
            for (int i = 0; i < relBusObjPageList.size(); i++) {
                String sCanEdit = "true";

                if (coFlag == false) {
                    sCanEdit = "false";
                } else {
                    Map mapObj = (Map) relBusObjPageList.get(i);
                    String relId = (String) mapObj.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    if (null != relId && !"".equals(relId)) {
                        Map<String, Object> lImpl = resImp.get(index);
                        index++;

                        if (lImpl.get("PIDList") != null)
                            sCanEdit = "false";
                    } else {
                        sCanEdit = "false";
                    }
                }

                lRet.addElement(sCanEdit);
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return lRet;
    }

    public static Object updateRelEffectivity(Context context, String[] args) throws Exception { // Called from table FRCMBOMTable (column Diversity)
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // update Effectivity for MBOM indented table
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            // System.out.println(programMap);

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            HashMap paramMap = (HashMap) programMap.get("paramMap");

            String relId = (String) paramMap.get("relId");
            String newValue = (String) paramMap.get("New Value");
            String rootId = (String) requestMap.get("parentOID");

            List<String> lModels = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, rootId);

            if (null != newValue && null != relId && !"".equals(relId) && null != lModels && 0 < lModels.size()) {
                // Transform the expression into a neutral text string
                String sNeutralExpr = formatEffExpToNeutral(context, newValue);
                // FRC : Fixed Bug#616-Effectivity Filter should work with AND, OR and NOT in filer expression
                if (sNeutralExpr.startsWith("NOT")) {
                    sNeutralExpr = " " + sNeutralExpr;
                }

                // Convert expression to XML
                String modId = (String) lModels.get(0);
                String sXMLExpr = convertEffNeutralToXML(context, sNeutralExpr, modId, "set");

                // Check that the parent of the instance has the model attached
                String parentPID = MqlUtil.mqlCommand(context, "print connection " + relId + " select from.physicalid dump |", false, false);

                List<String> parentPIDList = new ArrayList<String>();
                parentPIDList.add(parentPID);

                List<String> modelsToAttachList = new ArrayList<String>();
                modelsToAttachList.add(modId);

                FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, parentPIDList, modelsToAttachList);

                // Set New effectivity on the relationship
                if (null != sXMLExpr && 0 < sNeutralExpr.length())
                    FRCMBOMModelerUtility.setOrUpdateEffectivityOnInstance(context, plmSession, relId, sXMLExpr);
                else
                    FRCMBOMModelerUtility.removeEffectivityOnInstance(context, plmSession, relId);
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return true;
    }

    public String getEffectivityFromRel(Context context, String[] args) throws Exception { // Called from effectivity\FRCEffectivityDefinitionDialogFromTable.jsp
        // Computes the current effectivity expression to be displayed in the Edit Effectivity panel (before effectivity edition)
        // Outputs something like "@EF_FO(PHY@EF:2997B1A1000008C056CC669E000003BC~2997B1A1000008C056CC664800000296) AND
        // @EF_FO(PHY@EF:2997B1A1000008C056CC66AE00000440~2997B1A1000008C056CC664800000296)"
        String sRet = "";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String sRelId = (String) programMap.get("relId");
            if (null != sRelId && !"".equals(sRelId)) {
                List<String> sRelIdList = new ArrayList<String>();
                sRelIdList.add(sRelId);

                Map<String, String> effMap = FRCMBOMModelerUtility.getEffectivityOnInstanceList(context, plmSession, sRelIdList, false);

                String sXMLExpression = effMap.get(sRelId);

                if (null != sXMLExpression && !"".equals(sXMLExpression)) {
                    EffectivityExpression myExp = new EffectivityExpression(context, sXMLExpression, null, null);
                    if (null != myExp)
                        sRet = myExp.getActualExpr(context);
                }
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return sRet;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getModelConfigurationDictionary(Context context, String[] args) { // Called from effectivity\FRCEffectivityDefinitionSearch.jsp
        // Called when opening the filter definition panel. Input = model PID
        // This actually builds up the features & options list in the tree

        MapList mlRet = new MapList();

        boolean bTrans = false;
        try {
            ContextUtil.startTransaction(context, false);
            bTrans = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            // System.out.println(programMap);

            String strExpandLevelFilter = "";
            if (programMap.get("emxExpandFilter") != null) {
                strExpandLevelFilter = (String) programMap.get("emxExpandFilter");
            }
            // if(strExpandLevelFilter.equals("All")){
            strExpandLevelFilter = "0";
            // }
            String strObjectId = (String) programMap.get("objectId");
            String rootType = "";
            DomainObject domObj = null;
            if (null != strObjectId && !"".equals(strObjectId)) {
                domObj = new DomainObject(strObjectId);
                rootType = domObj.getType(context);
            }
            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_TYPE);
            objSelects.addElement(DomainConstants.SELECT_ID);
            objSelects.addElement(DomainConstants.SELECT_LEVEL);
            objSelects.addElement("id[parent]");
            StringList relSelects = new StringList();
            relSelects.addElement(DomainRelationship.SELECT_ID);
            relSelects.addElement("from.physicalid");
            relSelects.addElement("id[parent]");
            relSelects.addElement("physicalid");

            if (ProductLineConstants.TYPE_MODEL.equals(rootType)) {
                String sPrdRev = (String) programMap.get("FRCGetProductRevisionCmd");

                // Standard code
                if (null == sPrdRev || "".equals(sPrdRev)) {
                    short expLvl = Short.parseShort(strExpandLevelFilter);
                    StringBuffer sbRelPattern = new StringBuffer();
                    sbRelPattern.append(ConfigurationConstants.RELATIONSHIP_PRODUCTS);
                    sbRelPattern.append(",");
                    sbRelPattern.append(ConfigurationConstants.RELATIONSHIP_CONFIGURATION_STRUCTURES);
                    StringBuffer sbTypePattern = new StringBuffer();
                    sbTypePattern.append(ConfigurationConstants.TYPE_PRODUCTS);
                    sbTypePattern.append(",");
                    sbTypePattern.append(ConfigurationConstants.TYPE_CONFIGURATION_FEATURES);

                    // get complete structure of Product Revisions and Configuration Features under Model
                    MapList includeProductList = domObj.getRelatedObjects(context, sbRelPattern.toString(), // relationshipPattern
                            sbTypePattern.toString(), // typePattern
                            objSelects, // objectSelects
                            relSelects, // relationshipSelects
                            false, // getTo
                            true, // getFrom
                            expLvl, // recurseToLevel
                            null, // objectWhere,
                            null, // relationshipWhere
                            (int) 0); // limit

                    MapList excludeProductList = new MapList();
                    boolean includeProductLevel = true;
                    if (!includeProductLevel) {
                        // Remove Product Revisions and duplicate first level Configuration Features from MapList
                        StringList configFeatList = new StringList();
                        for (int i = 0; i < includeProductList.size(); i++) {
                            Map objMap = (Map) includeProductList.get(i);
                            String strLevel = (String) objMap.get(DomainConstants.SELECT_LEVEL);
                            String strParentPhyId = (String) objMap.get("from.physicalid");
                            objMap.put("id[parent]", strParentPhyId);
                            objMap.put(DomainConstants.SELECT_RELATIONSHIP_ID, (String) objMap.get("physicalid"));

                            // disable selection of objects or parent objects are of Products type
                            DomainObject dmObj = DomainObject.newInstance(context, (String) objMap.get(DomainConstants.SELECT_ID));
                            DomainObject parentObj = DomainObject.newInstance(context, strParentPhyId);
                            if (dmObj.isKindOf(context, ConfigurationConstants.TYPE_PRODUCTS) || parentObj.isKindOf(context, ConfigurationConstants.TYPE_PRODUCTS)) {
                                objMap.put("InsertIntoExp", "false");
                            }

                            if (strLevel.equals("2")) {
                                String strObjId = (String) objMap.get(DomainConstants.SELECT_ID);
                                if (!configFeatList.contains(strObjId)) {
                                    configFeatList.addElement(strObjId);
                                    int level = Integer.parseInt(strLevel);
                                    String newLevel = "" + (--level);
                                    objMap.put(DomainConstants.SELECT_LEVEL, newLevel);
                                    objMap.put("InsertIntoExp", "false");

                                    excludeProductList.add(objMap);

                                    for (int k = i + 1; k < includeProductList.size(); k++) {
                                        objMap = (Map) includeProductList.get(k);
                                        strLevel = (String) objMap.get(DomainConstants.SELECT_LEVEL);
                                        if (strLevel.equals("1") || strLevel.equals("2"))
                                            break;
                                        level = Integer.parseInt(strLevel);
                                        newLevel = "" + (--level);
                                        objMap.put(DomainConstants.SELECT_LEVEL, newLevel);
                                        objMap.put("InsertIntoExp", "false");
                                        excludeProductList.add(objMap);
                                        i = k;
                                    }
                                }
                            }
                        }
                        if (excludeProductList != null && expLvl == 0) {
                            HashMap hmTemp = new HashMap();
                            hmTemp.put("expandMultiLevelsJPO", "true");
                            excludeProductList.add(hmTemp);
                        }
                        mlRet = excludeProductList;
                    } else {
                        for (int i = 0; i < includeProductList.size(); i++) {
                            Map objMap = (Map) includeProductList.get(i);
                            String strParentPhyId = (String) objMap.get("from.physicalid");
                            objMap.put("id[parent]", strParentPhyId);
                            objMap.put(DomainConstants.SELECT_RELATIONSHIP_ID, (String) objMap.get("physicalid"));

                            // disable selection of objects or parent objects are of Products type
                            DomainObject dmObj = DomainObject.newInstance(context, (String) objMap.get(DomainConstants.SELECT_ID));
                            DomainObject parentObj = DomainObject.newInstance(context, strParentPhyId);
                            if (dmObj.isKindOf(context, ConfigurationConstants.TYPE_PRODUCTS) || parentObj.isKindOf(context, ConfigurationConstants.TYPE_PRODUCTS)) {
                                objMap.put("InsertIntoExp", "false");
                            }
                        }
                        if (includeProductList != null && expLvl == 0) {
                            HashMap hmTemp = new HashMap();
                            hmTemp.put("expandMultiLevelsJPO", "true");
                            includeProductList.add(hmTemp);
                        }
                        mlRet = includeProductList;
                    }
                } else {
                    short expLvl = Short.parseShort(strExpandLevelFilter);
                    DomainObject domPrd = DomainObject.newInstance(context, sPrdRev);
                    StringBuffer sbRelPattern = new StringBuffer();
                    sbRelPattern.append(ConfigurationConstants.RELATIONSHIP_CONFIGURATION_STRUCTURES);
                    StringBuffer sbTypePattern = new StringBuffer();
                    sbTypePattern.append(ConfigurationConstants.TYPE_CONFIGURATION_FEATURES);

                    // get complete structure of Product Revisions and Configuration Features under Model
                    MapList featureList = domPrd.getRelatedObjects(context, sbRelPattern.toString(), // relationshipPattern
                            sbTypePattern.toString(), // typePattern
                            objSelects, // objectSelects
                            relSelects, // relationshipSelects
                            false, // getTo
                            true, // getFrom
                            expLvl, // recurseToLevel
                            null, // objectWhere,
                            null, // relationshipWhere
                            (int) 0); // limit

                    for (int i = 0; i < featureList.size(); i++) {
                        Map objMap = (Map) featureList.get(i);
                        String strParentPhyId = (String) objMap.get("from.physicalid");
                        objMap.put("id[parent]", strParentPhyId);
                        objMap.put(DomainConstants.SELECT_RELATIONSHIP_ID, (String) objMap.get("physicalid"));
                    }

                    if (featureList != null && expLvl == 0) {
                        HashMap hmTemp = new HashMap();
                        hmTemp.put("expandMultiLevelsJPO", "true");
                        featureList.add(hmTemp);
                    }
                    mlRet = featureList;
                }
            } else if (ConfigurationConstants.TYPE_CONFIGURATION_FEATURE.equals(rootType)) {
                short expLvl = Short.parseShort(strExpandLevelFilter);
                StringBuffer sbRelPattern = new StringBuffer();
                sbRelPattern.append(ConfigurationConstants.RELATIONSHIP_CONFIGURATION_STRUCTURES);
                StringBuffer sbTypePattern = new StringBuffer();
                sbTypePattern.append(ConfigurationConstants.TYPE_CONFIGURATION_FEATURES);

                // get complete structure of Product Revisions and Configuration Features under Model
                MapList featureList = domObj.getRelatedObjects(context, sbRelPattern.toString(), // relationshipPattern
                        sbTypePattern.toString(), // typePattern
                        objSelects, // objectSelects
                        relSelects, // relationshipSelects
                        false, // getTo
                        true, // getFrom
                        expLvl, // recurseToLevel
                        null, // objectWhere,
                        null, // relationshipWhere
                        (int) 0); // limit

                for (int i = 0; i < featureList.size(); i++) {
                    Map objMap = (Map) featureList.get(i);
                    String strParentPhyId = (String) objMap.get("from.physicalid");
                    objMap.put("id[parent]", strParentPhyId);
                    objMap.put(DomainConstants.SELECT_RELATIONSHIP_ID, (String) objMap.get("physicalid"));
                }

                if (featureList != null && expLvl == 0) {
                    HashMap hmTemp = new HashMap();
                    hmTemp.put("expandMultiLevelsJPO", "true");
                    featureList.add(hmTemp);
                }
                mlRet = featureList;
            }

            for (int i = 0; i < mlRet.size(); i++) {
                Map objMap = (Map) mlRet.get(i);
                String sType = (String) objMap.get(DomainConstants.SELECT_TYPE);
                if (!ConfigurationConstants.TYPE_CONFIGURATION_OPTION.equals(sType)) {
                    objMap.put("disableSelection", "true");
                }
            }
            if (bTrans)
                ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            if (bTrans)
                ContextUtil.abortTransaction(context);
            e.printStackTrace();
        }

        return mlRet;

    }

    public String simplifyEffExpression(Context context, String[] args) throws Exception { // Called from effectivity\EffectivityUtil.jsp
        // Transform effectivity expression given by the Effectivity panel --- @EF_FO(PHY@EF:311E47B100009D245668294E000001E4~311E47B100004E4C56601CA9000008AF)
        // to a simple expression we can display --- Color.Blue

        String sRet = "";

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sExpression = (String) programMap.get("EffExpr");
        if (null != sExpression) {
            String sNeutral = formatEffExpToNeutral(context, sExpression);

            FRCSettings sett = new FRCSettings();
            sett.AndOperator = " AND ";
            sett.OrOperator = " OR ";
            sett.NotOperator = " NOT ";

            FRCEffExpr effFactory = new FRCEffExpr(sett, false, false, false);
            FRCEffExpr curEff = effFactory.parseStringToEffExpr(sett, sNeutral);

            if (curEff != null)
                sRet = curEff.convertToOrderedString();
        }

        return sRet;
    }
    // ==== END : Configuration

    // ==== START : Manage Capable Resources
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getCapableRscFromCreateAssembly(Context context, String[] args) { // Called from command FRCMBOMCapableResources
        MapList retList = new MapList();
        boolean bTrans = false;

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            bTrans = true;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");
                if (null != sId && !"".equals(sId)) {
                    List<String> lRsc = FRCMBOMModelerUtility.getResourcesAttachedToMBOMReference(context, plmSession, sId);
                    for (int i = 0; i < lRsc.size(); i++) {
                        String sRscPID = (String) lRsc.get(i);
                        // TIGTK-9622
                        DomainObject domRsc = DomainObject.newInstance(context, sRscPID);
                        HashMap mTmp = new HashMap();
                        mTmp.put(DomainConstants.SELECT_ID, domRsc.getInfo(context, DomainConstants.SELECT_ID));
                        // TIGTK-9622
                        retList.add(mTmp);
                    }
                }
            }
            if (bTrans)
                closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            e.printStackTrace();
            if (bTrans)
                closeSession(plmSession);
            ContextUtil.abortTransaction(context);
        }

        return retList;
    }

    public String addRsc(Context context, String[] args) { // Called from FRCOthersTableActions.jsp
        // Add an existing capable resource to a Create Assembly
        String sRet = "";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");
                String[] aTableRowId = (String[]) programMap.get("emxTableRowId");

                if (null != aTableRowId && null != sId && !"".equals(sId)) {

                    List<String> lExistingRsc = FRCMBOMModelerUtility.getResourcesAttachedToMBOMReference(context, plmSession, sId);
                    String lExistingRscStr = lExistingRsc.toString();

                    for (int i = 0; i < aTableRowId.length; i++) {
                        String sRowId = aTableRowId[i];
                        String[] aRowId = sRowId.split("\\|");
                        String sNewRsc = "";
                        if (null != aRowId && 1 < aRowId.length)
                            sNewRsc = aRowId[1];
                        String sNewPID = "";
                        if (null != sNewRsc && !"".equals(sNewRsc))
                            sNewPID = MqlUtil.mqlCommand(context, "print bus " + sNewRsc + " select physicalid dump |", false, false);
                        if (null != sNewPID && !"".equals(sNewPID)) {
                            if (!lExistingRscStr.contains(sNewPID)) {
                                FRCMBOMModelerUtility.attachResourceToMBOMReference(context, plmSession, sId, sNewPID);
                            }
                        }
                    }
                }
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
        }

        return sRet;
    }

    public String removeRsc(Context context, String[] args) { // Called from FRCOthersTableActions.jsp
        // Disconnect a capable resource from a Create Assembly
        String sRet = "";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");
                String[] aTableRowId = (String[]) programMap.get("emxTableRowId");

                if (null != aTableRowId && null != sId && !"".equals(sId)) {
                    for (int i = 0; i < aTableRowId.length; i++) {
                        String sRsc = aTableRowId[i];
                        if (null != sRsc && !"".equals(sRsc))
                            FRCMBOMModelerUtility.detachResourceFromMBOMReference(context, plmSession, sId, sRsc);
                    }
                }
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
        }

        return sRet;
    }
    // ==== END : Manage Capable Resources

    // ==== START : Manage Plants
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getPlantsFromCreateAssembly(Context context, String[] args) { // Called from command FRCMBOMPlants
        MapList retList = new MapList();
        boolean bTrans = false;

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            bTrans = true;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");
                if (null != sId && !"".equals(sId)) {
                    List<String> lPlants = FRCMBOMModelerUtility.getPlantsAttachedToMBOMReference(context, plmSession, sId);
                    for (int i = 0; i < lPlants.size(); i++) {
                        String sPlantPID = (String) lPlants.get(i);
                        HashMap mTmp = new HashMap();
                        mTmp.put(DomainConstants.SELECT_ID, sPlantPID);
                        retList.add(mTmp);
                    }
                }
            }
            if (bTrans)
                closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            e.printStackTrace();
            if (bTrans)
                closeSession(plmSession);
            ContextUtil.abortTransaction(context);
        }

        return retList;
    }

    public String addPlant(Context context, String[] args) { // Called from FRCOthersTableActions.jsp
        // Add an existing Plant to a Create Assembly
        String sRet = "";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");
                String[] aTableRowId = (String[]) programMap.get("emxTableRowId");

                if (null != aTableRowId && null != sId && !"".equals(sId)) {

                    List<String> lExistingPlant = FRCMBOMModelerUtility.getPlantsAttachedToMBOMReference(context, plmSession, sId);
                    String lExistingPlantStr = lExistingPlant.toString();

                    for (int i = 0; i < aTableRowId.length; i++) {
                        String sRowId = aTableRowId[i];
                        String[] aRowId = sRowId.split("\\|");
                        String sNewPlant = "";
                        if (null != aRowId && 1 < aRowId.length)
                            sNewPlant = aRowId[1];
                        String sNewPID = "";
                        if (null != sNewPlant && !"".equals(sNewPlant))
                            sNewPID = MqlUtil.mqlCommand(context, "print bus " + sNewPlant + " select physicalid dump |", false, false);
                        if (null != sNewPID && !"".equals(sNewPID)) {
                            if (!lExistingPlantStr.contains(sNewPID)) {
                                FRCMBOMModelerUtility.attachPlantToMBOMReference(context, plmSession, sId, sNewPID);
                            }
                        }
                    }
                }
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
        }

        return sRet;
    }

    public String removePlant(Context context, String[] args) { // Called from FRCOthersTableActions.jsp
        // Disconnect a Plant from a Create Assembly
        String sRet = "";

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String sId = (String) programMap.get("objectId");
                String[] aTableRowId = (String[]) programMap.get("emxTableRowId");

                if (null != aTableRowId && null != sId && !"".equals(sId)) {
                    for (int i = 0; i < aTableRowId.length; i++) {
                        String sPlant = aTableRowId[i];
                        if (null != sPlant && !"".equals(sPlant))
                            FRCMBOMModelerUtility.detachPlantFromMBOMReference(context, plmSession, sId, sPlant);
                    }
                }
            }
            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
        }

        return sRet;
    }
    // ==== END : Manage Plants

    // =====================================================
    // Internal Utilities shared with FRCMBOMTree and FRCPSTree
    // =====================================================

    public static MapList getExpandPS(Context context, String objectId, short expLvl, String filterExpr, String filterValue, String filterInput, StringList relSelect, StringList busSelect)
            throws Exception, FrameworkException {
        // Common PS expand method for IndentedTable and GraphicalBrowser
        MapList res;

        String pcGlobalFilterCompExpr = null;
        String pcGlobalFilterXMLValue = null;

        PLMCoreModelerSession plmSession = null;
        try {
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Prepare the filter expression if there is one
            if (filterExpr != null && !"".equals(filterExpr) && !"undefined".equals(filterExpr) && !"null".equals(filterExpr))
                pcGlobalFilterCompExpr = filterExpr;
            if (filterValue != null && !"".equals(filterValue) && !"undefined".equals(filterValue) && !"null".equals(filterValue)) {
                // Transform the expression into a neutral text string
                String sNeutralExpr = formatEffExpToNeutral(context, filterValue);

                // Get the model of the filter
                String modelName = filterInput.substring(0, filterInput.indexOf(":"));
                String modId = MqlUtil.mqlCommand(context, "print bus Model '" + modelName + "' '' select physicalid dump |", false, false);

                // Convert expression to XML
                pcGlobalFilterXMLValue = convertEffNeutralToXML(context, sNeutralExpr, modId, "filter");
            }

            res = FRCMBOMModelerUtility.getVPMStructure(context, plmSession, objectId, busSelect, relSelect, expLvl, pcGlobalFilterCompExpr, pcGlobalFilterXMLValue);
        } finally {
            closeSession(plmSession);
        }

        return res;
    }

    public static MapList getExpandMBOM(Context context, String objectId, int expLvl, String filterExpr, String filterValue, String filterInput, StringList relSelect, StringList busSelect)
            throws Exception, FrameworkException {
        // Common MBOM expand method for IndentedTable and GraphicalBrowser
        MapList res;

        String pcGlobalFilterCompExpr = null;
        String pcGlobalFilterXMLValue = null;

        PLMCoreModelerSession plmSession = null;
        try {
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Prepare the filter expression if there is one
            if (filterExpr != null && !"".equals(filterExpr) && !"undefined".equals(filterExpr) && !"null".equals(filterExpr))
                pcGlobalFilterCompExpr = filterExpr;
            if (filterValue != null && !"".equals(filterValue) && !"undefined".equals(filterValue) && !"null".equals(filterValue)) {
                // Transform the expression into a neutral text string
                String sNeutralExpr = formatEffExpToNeutral(context, filterValue);

                // Get the model of the filter
                String modelName = filterInput.substring(0, filterInput.indexOf(":"));
                String modId = MqlUtil.mqlCommand(context, "print bus Model '" + modelName + "' '' select physicalid dump |", false, false);

                // Convert expression to XML
                pcGlobalFilterXMLValue = convertEffNeutralToXML(context, sNeutralExpr, modId, "filter");
            }

            res = FRCMBOMModelerUtility.getVPMStructure(context, plmSession, objectId, busSelect, relSelect, (short) expLvl, pcGlobalFilterCompExpr, pcGlobalFilterXMLValue);
        } finally {
            closeSession(plmSession);
        }

        return res;
    }

    // =====================================================
    // Internal Utilities
    // =====================================================

    public static void flushSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.flushSession();
        } catch (Exception e) {
        }
    }

    public static void flushAndCloseSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.flushSession();
        } catch (Exception e) {
        }

        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
        }
    }

    public static void closeSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
        }
    }

    public static String getYesNo(Context context, String objectId, String attributeName) throws Exception {
        String returnValue = "";

        Locale loc = context.getLocale();
        if (objectId != null && !"".equals(objectId)) {
            String key = "FRCMBOMCentral.No";
            String makeBuyIntStr = MqlUtil.mqlCommand(context, "print bus " + objectId + " select attribute[" + attributeName + "] dump |", false, false);
            if (!"".equals(makeBuyIntStr)) {
                if ("2".equals(makeBuyIntStr))
                    key = "FRCMBOMCentral.Yes";
            }

            returnValue = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key);
        } else {
            returnValue = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, "FRCMBOMCentral.No");
        }

        return returnValue;
    }

    public static void setYesNo(Context context, String objectId, String yesNoFieldValue, String attributeName) throws Exception {
        Locale loc = context.getLocale();

        String yesNoFieldKey = "";
        for (String key : listYesNoFieldKeys) {
            if (yesNoFieldValue.equals(EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key)))
                yesNoFieldKey = key;
        }

        String yesNoAttributeValue = "1";
        if ("FRCMBOMCentral.Yes".equals(yesNoFieldKey))
            yesNoAttributeValue = "2";

        MqlUtil.mqlCommand(context, "mod bus " + objectId + " '" + attributeName + "' '" + yesNoAttributeValue + "'", false, false);
    }

    public static List<String> getDerivedTypes(Context context, String type) throws Exception {
        List<String> returnList = derivedTypesList.get(type);

        if (returnList == null) {
            String listTypesStr = MqlUtil.mqlCommand(context, "print type " + type + " select derivative dump |", false, false);
            if (!"".equals(listTypesStr)) {
                String[] listTypes = listTypesStr.split("\\|");
                returnList = new StringList(listTypes);
            } else {
                returnList = new StringList();
            }
            returnList.add(type);
            derivedTypesList.put(type, returnList);
        }

        return returnList;
    }

    public static String genObjHTML(Context context, String objID, String objType, String objDisplayStr, boolean bold, boolean italic) throws Exception {
        String attIcon = UINavigatorUtil.getTypeIconProperty(context, objType);

        StringBuffer anchorStr = new StringBuffer();
        anchorStr.append("<a TITLE=");
        anchorStr.append("\"" + objDisplayStr + "\"");

        anchorStr.append(" href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?emxSuiteDirectory=common&amp;parentOID=null&amp;jsTreeID=null&amp;suiteKey=Framework&amp;objectId=");

        anchorStr.append(objID);
        if (bold)
            anchorStr.append("', '', '', 'false', 'popup', '')\" class=\"object\">");
        else
            anchorStr.append("', '', '', 'false', 'popup', '')\">");

        StringBuffer returnStr = new StringBuffer();
        returnStr.append("<b></b>");
        returnStr.append(anchorStr.toString());
        returnStr.append("<div class=\"typeIconDiv\" style=\"position:relative;display:inline-block;margin-right:4px;\">");// UM5 : I add this div to be able to add the scope icon in the corner on the
                                                                                                                           // JS side
                                                                                                                           // returnStr.append("<img border=\"0\" style=\"height:16px;\"
                                                                                                                           // src=\"../common/images/" + attIcon + "\"/>");
        returnStr.append("<img border=\"0\" src=\"../common/images/" + attIcon + "\"/>");
        // typeIconDivSEBadge
        // returnStr.append("<div class=\"typeIconDivSEBadge\" style=\"position:absolute;display:block;bottom:0px;right:0px;width:8px;height:8px;\"></div>");//UM5 : I add this div to be able to add
        // the scope icon in the corner on the JS side
        returnStr.append("<div class=\"typeIconDivSEBadge\" style=\"position:absolute;display:block;bottom:0px;right:0px;width:11px;height:11px;\"></div>");// UM5 : I add this div to be able to add
                                                                                                                                                            // the scope icon in the corner on the JS
                                                                                                                                                            // side
        returnStr.append("</div>");
        returnStr.append("</a>");
        returnStr.append(anchorStr.toString());
        if (bold)
            returnStr.append("<b>");
        if (italic)
            returnStr.append("<i>");
        returnStr.append(objDisplayStr);
        if (italic)
            returnStr.append("</i>");
        if (bold)
            returnStr.append("</b>");
        returnStr.append("</a>");

        return returnStr.toString();
    }

    public static MapList getChangeObjects(Context context) throws Exception {
        MapList retMl = new MapList();

        try {
            String userID = context.getUser();
            DomainObject domUser = PersonUtil.getPersonObject(context, userID);

            String relPattern = "Senior Technical Assignee,Technical Assignee,Change Coordinator";
            String typePattern = "Change Action,Change Order,Change Request";

            StringList slRel = new StringList(new String[] { "id[connection]", "physicalid[connection]", "name[connection]" });
            String sSelectCAFatherId = "to[Change Action].from.id";
            String sSelectCAFatherPhyId = "to[Change Action].from.physicalid";
            String sSelectCAFatherType = "to[Change Action].from.type";
            String sSelectCAFatherName = "to[Change Action].from.name";
            String sSelectCAFatherRev = "to[Change Action].from.revision";
            String sSelectCAFatherCur = "to[Change Action].from.current";

            String typeWhereExprCA = "( (type == 'Change Action') &&  (current == 'In Work') ) || ( (type == 'Change Order') && ((current == 'Propose') || (current == 'Prepare') || (current == 'In Work')) ) || ( (type =='Change Request')  && ((current == 'Create') || (current == 'Evaluate')) )";
            StringList slTypeCA = new StringList(new String[] { "id", "physicalid", "type", "name", "revision", "current", sSelectCAFatherId, sSelectCAFatherPhyId, sSelectCAFatherType,
                    sSelectCAFatherName, sSelectCAFatherRev, sSelectCAFatherCur });
            MapList mlAssigned = domUser.getRelatedObjects(context, relPattern, typePattern, slTypeCA, slRel, true, false, (short) 1, typeWhereExprCA, "", 0);

            StringList lID = new StringList(); // for storing list of ids and avoid duplicates
            // Build list of assigned changes
            if (null != mlAssigned) {
                for (int i = 0; i < mlAssigned.size(); i++) {
                    Map mObj = (Map) mlAssigned.get(i);
                    String sId = (String) mObj.get("id");
                    String sType = (String) mObj.get("type");
                    HashMap mChange = null;

                    // In case of CA, we add the father object (CR or CO)
                    if ("Change Action".equals(sType)) {
                        StringList lType = getMapValue(mObj, sSelectCAFatherType);
                        // When we have multiple fathers (CO and CR are possible), we keep only the CO
                        if (null != lType && 0 < lType.size()) {
                            String sTmpType = (String) lType.get(0);
                            int index = 0;
                            if ("Change Request".equals(sTmpType)) {
                                for (int j = 1; j < lType.size(); j++) {
                                    if ("Change Order".equals((String) lType.get(j))) {
                                        index = j;
                                    }
                                }
                            }
                            StringList lFatherId = getMapValue(mObj, sSelectCAFatherId);
                            sId = (String) lFatherId.get(index);
                            if (null != sId && !"".equals(sId) && !lID.contains(sId)) {
                                StringList lFatherPhysId = getMapValue(mObj, sSelectCAFatherPhyId);
                                StringList lFatherName = getMapValue(mObj, sSelectCAFatherName);
                                StringList lFatherRev = getMapValue(mObj, sSelectCAFatherRev);
                                StringList lFatherCur = getMapValue(mObj, sSelectCAFatherCur);
                                mChange = new HashMap();
                                mChange.put("id", sId);
                                mChange.put("physicalid", (String) lFatherPhysId.get(index));
                                mChange.put("type", (String) lType.get(index));
                                mChange.put("name", (String) lFatherName.get(index));
                                mChange.put("revision", (String) lFatherRev.get(index));
                                mChange.put("current", (String) lFatherCur.get(index));
                            }
                        }
                    } else if (null != sId && !"".equals(sId) && !lID.contains(sId)) {
                        mChange = new HashMap();
                        mChange.put("id", sId);
                        mChange.put("physicalid", mObj.get("physicalid"));
                        mChange.put("type", mObj.get("type"));
                        mChange.put("name", mObj.get("name"));
                        mChange.put("revision", mObj.get("revision"));
                        mChange.put("current", mObj.get("current"));

                    }
                    if (null != mChange) {
                        retMl.add(mChange);
                        lID.addElement(sId);
                    }
                }
            }

            // Insert owned CO/CR that are not already included
            // findObjects(context, typePattern, namePattern, revPattern, ownerPattern, vaultPattern, whereExpression, boolean expandType, StringList objectSelects)
            String typeWhereExpr = "( (type == 'Change Order') && ((current == 'Propose') || (current == 'Prepare') || (current == 'In Work')) ) || ( (type =='Change Request')  && ((current == 'Create') || (current == 'Evaluate')) )";
            StringList slType = new StringList(new String[] { "id", "physicalid", "type", "name", "revision", "current" });
            MapList mlChangesOwned = DomainObject.findObjects(context, "Change Order,Change Request", "*", "*", userID, null, typeWhereExpr, true, slType);
            if (null != mlChangesOwned) {
                for (int i = 0; i < mlChangesOwned.size(); i++) {
                    Map mObj = (Map) mlChangesOwned.get(i);
                    String sId = (String) mObj.get("id");
                    if (false == lID.contains(sId))
                        retMl.add(mObj);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }

        return retMl;
    }

    private static StringList getMapValue(Map mapObj, String sSelect) {
        StringList lRet = null;
        if (null != mapObj) {
            Object tmpObj = mapObj.get(sSelect);
            if (null != tmpObj) {
                if (tmpObj instanceof String) {
                    lRet = new StringList();
                    lRet.addElement((String) tmpObj);
                } else if (tmpObj instanceof StringList)
                    lRet = (StringList) tmpObj;
            }
        }
        return lRet;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getProductFromEBOM(Context context, String sPartId) throws Exception {
        MapList mlRet = new MapList();

        if (null != sPartId && !"".equals(sPartId)) {
            DomainObject domObj = new DomainObject(sPartId);
            StringList objSelects = new StringList(4);
            objSelects.addElement(DomainConstants.SELECT_ID);
            objSelects.addElement("physicalid");
            objSelects.addElement(DomainConstants.SELECT_TYPE);
            objSelects.addElement(DomainConstants.SELECT_NAME);
            objSelects.addElement("attribute[PLMEntity.V_Name]");
            String sObjTypes = "*";
            String sRelTypes = EngineeringConstants.RELATIONSHIP_PART_SPECIFICATION;
            // Case 1 : Get VPMRef through Part Spec relationship
            MapList mlRelated = domObj.getRelatedObjects(context, sRelTypes, sObjTypes, objSelects, null, false, true, (short) 1, null, null);
            if (null != mlRelated) {
                for (int i = 0; i < mlRelated.size(); i++) {
                    Map mObj = (Map) mlRelated.get(i);
                    String sType = (String) mObj.get(DomainConstants.SELECT_TYPE);
                    if (mxType.isOfParentType(context, sType, "VPMReference")) {
                        mlRet.add(mObj);
                    }
                }
            }

            // Case 2 : Get VPMRef through a query based on TNR
            if (0 == mlRet.size()) {
                StringList partSelects = new StringList(3);
                partSelects.addElement(DomainConstants.SELECT_TYPE);
                partSelects.addElement(DomainConstants.SELECT_NAME);
                partSelects.addElement("minororder");

                Map mPart = domObj.getInfo(context, partSelects);

                String sType = (String) mPart.get(DomainConstants.SELECT_TYPE);
                String psTypeToSearch = ebomToPSTypeMapping.get(sType);
                if (psTypeToSearch == null || "".equals(psTypeToSearch))
                    throw new Exception("EBOM type " + sType + " has no mapping defined for equivalent Product Structure type.");

                String sName = (String) mPart.get(DomainConstants.SELECT_NAME);
                String sRevIndex = (String) mPart.get("minororder");
                String sWhere = "majororder == \"" + sRevIndex + "\"";
                // findObjects(context, typePattern, namePattern, revPattern, ownerPattern, vaultPattern, whereExpression, expandType, objectSelects)
                mlRet = DomainObject.findObjects(context, psTypeToSearch, sName, "*", "*", "*", sWhere, true, objSelects);
            }
        }

        return mlRet;
    }

    public static String createMBOMFromEBOMEmptyStructure(Context context, PLMCoreModelerSession plmSession, String[] args) throws Exception {
        String sRet = "";

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sPartId = (String) programMap.get("objectId");

        MapList mlPrd = getProductFromEBOM(context, sPartId);
        if (null != mlPrd) {
            if (1 < mlPrd.size())
                throw new Exception("Several VPM Products have been found for the given EBOM part");
            if (mlPrd.size() == 0)
                throw new Exception("No VPM Products have been found for the given EBOM part. Please do a \"Collaborate with Physical\".");
            Map mPrd = (Map) mlPrd.get(0);
            String sPrdPhysId = (String) mPrd.get("physicalid");
            if (null != sPrdPhysId && !"".equals(sPrdPhysId)) {
                // Create new manuf item
                String type = (String) programMap.get("TypeActual");

                HashMap<String, String> attributes = new HashMap<String, String>();
                // attributes.put("PLM_ExternalID", "9995");

                String newRefPID = createMBOMReference(context, plmSession, type, null, attributes);

                String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
                // Modif AFN - Test if a value has been defined into the creation web form
                String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
                if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
                    changeObjectName = changeObjectFromForm;
                attachObjectToChange(context, plmSession, changeObjectName, newRefPID);

                // Set scope and attach model
                FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newRefPID, sPrdPhysId, false);

                // Before attaching Models, the plmSession needs to be flushed, otherwise the new objects will not be seen...
                flushSession(plmSession);

                List<String> lModelsArray = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId);

                List<String> newRefPIDList = new ArrayList<String>();
                newRefPIDList.add(newRefPID);

                FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, newRefPIDList, lModelsArray);

                sRet = newRefPID;
            }
        } else {
            throw new Exception("No VPM Product found for the given EBOM part");
        }

        return sRet;
    }

    public static String createMBOMFromEBOMFromTemplate(Context context, PLMCoreModelerSession plmSession, String[] args) throws Exception {
        String sRet = "";

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sPartId = (String) programMap.get("objectId");
        String templateRootRefID = (String) programMap.get("rootMbomOID");

        // Get the change object
        String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
        // Modif AFN - Test if a value has been defined into the creation web form
        String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
        if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
            changeObjectName = changeObjectFromForm;

        MapList mlPrd = getProductFromEBOM(context, sPartId);
        if (null != mlPrd) {
            if (1 < mlPrd.size())
                throw new Exception("Several VPM Products have been found for the given EBOM part");
            if (mlPrd.size() == 0)
                throw new Exception("No VPM Products have been found for the given EBOM part. Please do a \"Collaborate with Physical\".");
            Map mPrd = (Map) mlPrd.get(0);
            String sPrdPhysId = (String) mPrd.get("physicalid");
            if (null != sPrdPhysId && !"".equals(sPrdPhysId)) {
                // Recursively clone the template root node
                String sMbomId = duplicateMBOMStructure(context, plmSession, templateRootRefID, changeObjectName);

                if (null != sMbomId && !"".equals(sMbomId)) {
                    // Set scope and attach model
                    FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, sMbomId, sPrdPhysId, false);

                    List<String> lModelsArray = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId);

                    List<String> sMbomIdList = new ArrayList<String>();
                    sMbomIdList.add(sMbomId);

                    FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, sMbomIdList, lModelsArray);

                    sRet = sMbomId;
                }
            }
        } else {
            throw new Exception("No VPM Product found for the given EBOM part");
        }

        return sRet;
    }

    public static String createMBOMFromEBOMLikePS(Context context, PLMCoreModelerSession plmSession, String[] args) throws Exception {
        String sRet = "";
        FRCMBOMModelerUtility.checkValidScenario(context);
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sPartId = (String) programMap.get("objectId");

        valueEnvAttachModel = System.getenv("DISABLE_ATTACH_MODEL_ON_SCOPE");

        // Get the change object
        String changeObjectName = (String) programMap.get("FRCMBOMGetChangeObject");
        // Modif AFN - Test if a value has been defined into the creation web form
        String changeObjectFromForm = (String) programMap.get("ChangeObjectCreateForm");
        if (null != changeObjectFromForm && !"".equals(changeObjectFromForm))
            changeObjectName = changeObjectFromForm;

        MapList mlPrd = getProductFromEBOM(context, sPartId);
        if (null != mlPrd) {
            if (1 < mlPrd.size())
                throw new Exception("Several VPM Products have been found for the given EBOM part");
            if (mlPrd.size() == 0)
                throw new Exception("No VPM Products have been found for the given EBOM part. Please do a \"Collaborate with Physical\".");
            Map mPrd = (Map) mlPrd.get(0);
            String sPrdPhysId = (String) mPrd.get("physicalid");
            if (null != sPrdPhysId && !"".equals(sPrdPhysId)) {
                List<String> lModelsArray = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, sPrdPhysId);
                lModelListOnStructure = new ArrayList<String>();
                // Recursively process the PS root node
                List<String> newRefPIDList = new ArrayList<String>();
                sRet = createMBOMFromEBOMLikePSRecursive(context, plmSession, null, sPrdPhysId, null, null, lModelsArray.toArray(new String[] {}), newRefPIDList);
                if (lModelListOnStructure.size() > 0) {
                    // remove duplicateModel
                    Set<String> hs = new HashSet<>();
                    hs.addAll(lModelListOnStructure);
                    lModelListOnStructure.clear();
                    lModelListOnStructure.addAll(hs);
                    // System.out.println("List of Model " + lModelListOnStructure);
                    // Attach All Model to the Root
                    List lBOMRef = new ArrayList<List>();
                    lBOMRef.add(sRet);
                    FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, lBOMRef, lModelListOnStructure);
                    // Attcah all the Model to the root.

                }
                // Attach all created references to change object.
                attachListObjectsToChange(context, plmSession, changeObjectName, newRefPIDList);
            }
        } else {
            throw new Exception("No VPM Product found for the given EBOM part");
        }

        return sRet;
    }

    public static String createMBOMFromEBOMLikePSRecursive(Context context, PLMCoreModelerSession plmSession, String mbomParentRef, String psRefID, String mbomCompleteParentPath,
            String psCompletePath, String[] modelsToAttachToRoot, List<String> newRefPIDList) throws Exception {
        String newMBOMRefPID = null;

        // Get all the first level instances of the PS reference
        StringList busSelect = new StringList();
        busSelect.add("physicalid");
        StringList relSelect = new StringList();
        relSelect.add("physicalid[connection]");

        // Bug #231 - DCP - START
        relSelect.add("attribute[PLMInstance.V_TreeOrder].value");

        // MapList psInstList = getVPMStructure(context, psRefID, busSelect, relSelect, (short) 1, null); // Expand first level
        MapList psInstList = FRCMBOMModelerUtility.getVPMStructure(context, plmSession, psRefID, busSelect, relSelect, (short) 1, null, null); // Expand first level

        psInstList.sortStructure("attribute[PLMInstance.V_TreeOrder].value", "ascending", "real");
        // Bug #231 - DCP - END
        if (psInstList.size() == 0 && mbomParentRef != null && !"".equals(mbomParentRef)) {
            // This is a leaf node of the PS (and it is not the root) : create a Provide under the MBOM path, with implement link and effectivity

            String[] argsForImplement = new String[3];
            argsForImplement[0] = mbomCompleteParentPath;
            argsForImplement[1] = psCompletePath;

            List<String> newRefPIDListFromLinkProcess = new ArrayList<String>();

            setImplementLinkProcess(context, plmSession, argsForImplement, newRefPIDListFromLinkProcess);

            newRefPIDList.addAll(newRefPIDListFromLinkProcess);
        } else {
            // This is an intermediate node of the PS (and it is not the root) : insert a new CreateAssembly under the MBOM reference, and process recursively for each child instance

            // Get the attribute values of the PS reference
            DomainObject psRefObj = new DomainObject(psRefID);
            Map psRefAttributes = psRefObj.getAttributeMap(context, true);
            HashMap<String, String> mbomRefAttributes = new HashMap<String, String>();
            mbomRefAttributes.put("PLMEntity.V_Name", (String) psRefAttributes.get("PLMEntity.V_Name"));
            mbomRefAttributes.put("PLMEntity.V_description", (String) psRefAttributes.get("PLMEntity.V_description"));

            // Create a new MBOM reference
            newMBOMRefPID = createMBOMReference(context, plmSession, "CreateAssembly", null, mbomRefAttributes);
            // lModelListOnStructure

            if (valueEnvAttachModel == null) {
                List<String> lModels = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, psRefID);

                if (null != lModels && 0 < lModels.size()) {
                    lModelListOnStructure.addAll(lModels);
                }
            }
            // Replicate all the attributes values on the new MBOM reference
            DomainObject newMBOMRefObj = new DomainObject(newMBOMRefPID);
            // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
            mbomRefAttributes.put("PLMReference.V_ApplicabilityDate", (String) psRefAttributes.get("PLMReference.V_ApplicabilityDate"));
            newMBOMRefObj.setAttributeValues(context, mbomRefAttributes);

            newRefPIDList.add(newMBOMRefPID);

            String newMBOMInstPID = null;

            if (mbomParentRef == null || "".equals(mbomParentRef)) { // This is the root node of the PS
                // Set scope and attach model
                FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, newMBOMRefPID, psRefID, false);

                List<String> newMBOMRefPIDList = new ArrayList<String>();
                newMBOMRefPIDList.add(newMBOMRefPID);

                List<String> modelsToAttachToRootList = new ArrayList<String>();
                for (String modelPID : modelsToAttachToRoot)
                    modelsToAttachToRootList.add(modelPID);

                FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, newMBOMRefPIDList, modelsToAttachToRootList);

            } else {
                // Get the attribute values of the instance
                String[] psCompletePathList = psCompletePath.split("/");
                DomainRelationship psInstObj = new DomainRelationship(psCompletePathList[psCompletePathList.length - 1]);
                Map psInstAttributes = psInstObj.getAttributeMap(context, true);
                Map mbomInstAttributes = new HashMap();
                // mbomInstAttributes.put("PLMInstance.PLM_ExternalID", psInstAttributes.get("PLMInstance.PLM_ExternalID"));
                mbomInstAttributes.put("PLMInstance.V_description", psInstAttributes.get("PLMInstance.V_description"));
                // Fixed Bug 231-Tree Ordering
                mbomInstAttributes.put("PLMInstance.V_TreeOrder", psInstAttributes.get("PLMInstance.V_TreeOrder"));

                // Create a new instance
                newMBOMInstPID = createInstance(context, plmSession, mbomParentRef, newMBOMRefPID);

                // Replicate all the attributes values on the new instance
                DomainRelationship newMBOMInstObj = new DomainRelationship(newMBOMInstPID);
                newMBOMInstObj.setAttributeValues(context, mbomInstAttributes);
            }

            // Bug #231 - DCP - START
            for (int i = 0; i < psInstList.size(); i++) {
                Map<String, String> psInstInfo = (Map<String, String>) psInstList.get(i);
                if (mbomParentRef == null || "".equals(mbomParentRef)) { // This is the root node of the PS
                    createMBOMFromEBOMLikePSRecursive(context, plmSession, newMBOMRefPID, psInstInfo.get("physicalid"), newMBOMRefPID, psRefID + "/" + psInstInfo.get("physicalid[connection]"),
                            modelsToAttachToRoot, newRefPIDList);
                } else {
                    createMBOMFromEBOMLikePSRecursive(context, plmSession, newMBOMRefPID, psInstInfo.get("physicalid"), mbomCompleteParentPath + "/" + newMBOMInstPID,
                            psCompletePath + "/" + psInstInfo.get("physicalid[connection]"), modelsToAttachToRoot, newRefPIDList);
                }
            }
            // Bug #231 - DCP - END
        }

        return newMBOMRefPID;
    }

    public static void attachObjectToChange(Context context, PLMCoreModelerSession plmSession, String changePID, String objectPID) throws Exception {
        List<String> objectPIDList = new ArrayList<String>();
        objectPIDList.add(objectPID);
        if (!"-none-".equals(changePID)) {
            attachListObjectsToChange(context, plmSession, changePID, objectPIDList);
        }
    }

    public static void attachListObjectsToChange(Context context, PLMCoreModelerSession plmSession, String changePID, List<String> objectPIDList) throws Exception {
        // Careful : the session MUST be committed first, otherwise the new reference is not seen in the database!!!!!
        flushSession(plmSession);

        boolean contextPushed = false;
        try {
            if (changePID != null && !"null".equals(changePID) && !"".equals(changePID) && !"-none-".equals(changePID) && !"undefined".equals(changePID)) {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                contextPushed = true;
                // ContextUtil.startTransaction(context, true);
                DomainObject domChange = new DomainObject(changePID);
                // DomainObject domObjToAttach=new DomainObject(objectPID);

                Map mpInvalidObjects = null;
                StringList slAffectedItems = new StringList();
                for (String objectPID : objectPIDList) {
                    slAffectedItems.add(objectPID);
                }

                if (domChange.isKindOf(context, "Change Action")) {
                    mpInvalidObjects = new ChangeAction(changePID).connectAffectedItems(context, slAffectedItems);
                } else if (domChange.isKindOf(context, "Change Order")) {
                    ChangeOrder changeOrder = new ChangeOrder(changePID);
                    mpInvalidObjects = changeOrder.connectAffectedItems(context, slAffectedItems);
                } else if (domChange.isKindOf(context, "Change Request")) {
                    ChangeRequest changeRequest = new ChangeRequest(changePID);
                    mpInvalidObjects = changeRequest.connectAffectedItems(context, slAffectedItems);
                } else {
                    throw new Exception("Function attachObjectToChange : Type of change object not managed");
                }

                String strInvalidObjects = (String) mpInvalidObjects.get("strErrorMSG");
                if (strInvalidObjects != null && !strInvalidObjects.isEmpty()) {
                    throw new Exception("The object '" + strInvalidObjects + "' cannot be attached to the change object.");
                }
                flushSession(plmSession);
            }
            // ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            // ContextUtil.abortTransaction(context);
            throw exp;
        } finally {
            if (contextPushed) {
                ContextUtil.popContext(context);
            }
        }
    }

    public static String setImplementLinkProcess(Context context, PLMCoreModelerSession plmSession, String[] args, List<String> newRefPIDList) throws Exception {
        // Return value :
        // 0 = refresh row of the leaf MBOM instance
        // 1 = re-expand the row of the leaf MBOM instance
        // 2 = re-expand the row of the parent of the MBOM instance
        String returnValue = "0";

        String mbomCompletePath = args[0];
        String psCompletePath = args[1];

        String[] psCompletePathList = psCompletePath.split("/");
        String psLeafInstancePID = psCompletePathList[psCompletePathList.length - 1];
        String psLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + psLeafInstancePID + " select to.physicalid dump |", false, false);

        String[] mbomCompletePathList = mbomCompletePath.split("/");
        String mbomLeafInstancePID = null;
        String mbomLeafRefPID = null;
        if (mbomCompletePathList.length > 1) {
            mbomLeafInstancePID = mbomCompletePathList[mbomCompletePathList.length - 1];
            mbomLeafRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomLeafInstancePID + " select to.physicalid dump |", false, false);
        } else {
            mbomLeafRefPID = mbomCompletePathList[mbomCompletePathList.length - 1];
        }

        String mbomLeafRefType = MqlUtil.mqlCommand(context, "print bus " + mbomLeafRefPID + " select type dump |", false, false);

        boolean isDirect = false;
        boolean isIndirect = false;

        for (String typeInList : baseTypesForMBOMLeafNodes) {
            if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                isDirect = true;
        }

        List<String> newRefPIDListFromSynch = new ArrayList<String>();
        String newMBOMRefPID = null;

        if (isDirect) {
            // Get the synched ManufItem with the lead PS reference (new Provide or the existing leaf ManufItem)
            newMBOMRefPID = getSynchedScopeMBOMRefFromPSRef(context, plmSession, psLeafRefPID, mbomLeafRefPID, newRefPIDListFromSynch);

            if (!newMBOMRefPID.equals(mbomLeafRefPID)) { // The leaf MBOM reference is not the one synched with the leaf PS reference. Normally because it is a different revision
                // Replace the MBOM leaf instance with the new one
                String newMBOMLeafInstancePID = FRCMBOMModelerUtility.replaceMBOMInstance(context, plmSession, mbomLeafInstancePID, newMBOMRefPID);

                mbomCompletePath = mbomCompletePath.replace(mbomLeafInstancePID, newMBOMLeafInstancePID);
                mbomLeafInstancePID = newMBOMLeafInstancePID;

                returnValue = "2";
            }
        } else {
            for (String typeInList : baseTypesForMBOMAssemblyNodes) {
                if (getDerivedTypes(context, typeInList).contains(mbomLeafRefType))
                    isIndirect = true;
            }

            if (isIndirect) {
                // Get a new synched Provide with the leaf PS reference
                newMBOMRefPID = getSynchedScopeMBOMRefFromPSRef(context, plmSession, psLeafRefPID, null, newRefPIDListFromSynch);

                // Insert this Provide under it's parent
                mbomLeafInstancePID = createInstance(context, plmSession, mbomLeafRefPID, newMBOMRefPID);

                DomainRelationship psInstObj = new DomainRelationship(psCompletePathList[psCompletePathList.length - 1]);
                Map psInstAttributes = psInstObj.getAttributeMap(context, true);
                Map mbomInstAttributes = new HashMap();

                mbomInstAttributes.put("PLMInstance.V_TreeOrder", psInstAttributes.get("PLMInstance.V_TreeOrder"));

                // Replicate all the attributes values on the new instance
                DomainRelationship newMBOMInstObj = new DomainRelationship(mbomLeafInstancePID);
                newMBOMInstObj.setAttributeValues(context, mbomInstAttributes);

                mbomCompletePath += "/";
                mbomCompletePath += mbomLeafInstancePID;

                returnValue = "1";
            }
        }

        newRefPIDList.addAll(newRefPIDListFromSynch);

        if (isDirect || isIndirect) {
            String trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomCompletePath, psCompletePath);

            if (trimmedPSPath == null || "".equals(trimmedPSPath)) {
                throw new Exception("No scope exists.");
            }

            // Remove any existing implement link
            FRCMBOMModelerUtility.deleteImplementLink(context, plmSession, mbomLeafInstancePID, true);

            // Put a new implement link
            List<String> mbomLeafInstancePIDList = new ArrayList();
            mbomLeafInstancePIDList.add(mbomLeafInstancePID);
            List<String> trimmedPSPathList = new ArrayList();
            trimmedPSPathList.add(trimmedPSPath);
            String retStr = FRCMBOMModelerUtility.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);
            if (!"".equals(retStr))
                throw new Exception(retStr);
        }
        return returnValue;
    }

    public static String getSynchedScopeMBOMRefFromPSRef(Context context, PLMCoreModelerSession plmSession, String psRefPID, String mbomRefPID, List<String> newRefPIDList) throws Exception {
        String returnMBOMRefPID = null;

        // Check if PS reference has a scope
        List<String> mbomRefPIDScopedWithPSRefList = FRCMBOMModelerUtility.getScopingReferencesFromList(context, plmSession, psRefPID);

        if (mbomRefPIDScopedWithPSRefList.size() > 1) { // PS reference has multiple MBOM scopes
            // If all the elements of the list are within the same revision family, return the latest revision
            boolean isSameMajorIds = true;
            String lastMajorIdsStr = null;

            for (String refPID : mbomRefPIDScopedWithPSRefList) {
                String majorIdsStr = MqlUtil.mqlCommand(context, "print bus " + refPID + " select majorids dump |", false, false);

                if (lastMajorIdsStr != null) {
                    if (!lastMajorIdsStr.equals(majorIdsStr))
                        isSameMajorIds = false;
                }

                lastMajorIdsStr = majorIdsStr;
            }

            if (isSameMajorIds) {
                String[] lastMajorIds = lastMajorIdsStr.split("\\|");

                // Return this MBOM scope
                returnMBOMRefPID = lastMajorIds[lastMajorIds.length - 1];

                // Check if it is a provide
                String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);

                boolean isDirect = false;

                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                        isDirect = true;
                }

                if (!isDirect)
                    throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");
            } else {
                throw new Exception("You cannot provide this part from the EBOM : it has multiple scopes !");
            }
        } else if (mbomRefPIDScopedWithPSRefList.size() == 1) { // PS reference has already one MBOM scope
            // Return this MBOM scope
            returnMBOMRefPID = mbomRefPIDScopedWithPSRefList.get(0);

            // Check if it is a provide
            String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);

            // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
            String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
            MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
            String psRefApplicabilityDate = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMReference.V_ApplicabilityDate].value dump |", false, false);
            MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefApplicabilityDate + "'", false, false);

            boolean isDirect = false;

            for (String typeInList : baseTypesForMBOMLeafNodes) {
                if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                    isDirect = true;
            }

            if (!isDirect)
                throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");
        } else { // PS reference does not already have an MBOM scope
            // Get the previous revision of the PS reference
            String previousRevPSRefPID = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select previous.physicalid dump |", false, false);

            if ("".equals(previousRevPSRefPID)) { // PS reference does not have a previous revision
                if (mbomRefPID == null) { // MBOM reference is null
                    // Create a new Provide and return it
                    // HashMap programMap = new HashMap();
                    // programMap.put("TypeActual", "Provide");
                    // programMap.put("FRCMBOMGetChangeObject", changeObjectPID);

                    HashMap<String, String> attributes = new HashMap<String, String>();
                    returnMBOMRefPID = createMBOMReference(context, plmSession, "Provide", null, attributes);

                    newRefPIDList.add(returnMBOMRefPID);

                    // String[] createArgs = JPO.packArgs(programMap);
                    // Map<String, String> newProvideMap = createNewManufItem(context, createArgs);
                    // returnMBOMRefPID = newProvideMap.get("id");

                    // By default, the Title on the new Provide should be the same as the Title of the VPMReference linked to it
                    String psRefTitle = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value dump |", false, false);
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefTitle + "'", false, false);

                    // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
                    String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
                    String psRefApplicabilityDate = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMReference.V_ApplicabilityDate].value dump |", false, false);
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefApplicabilityDate + "'", false, false);

                    // Create a scope link between PS reference and MBOM reference
                    FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                } else { // MBOM reference is not null
                    List<String> inputListForGetScope = new ArrayList<String>();
                    inputListForGetScope.add(mbomRefPID);
                    String psRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                    if (psRefScopePID != null && !"".equals(psRefScopePID)) { // MBOM reference has already a PS scope : throw a new exception
                        throw new Exception("This MBOM node already has a scope, and it is not the EBOM part you are providing !");
                    } else { // MBOM reference does not already have a scope
                        // Return the MBOM reference
                        returnMBOMRefPID = mbomRefPID;

                        // Create a scope link between PS reference and MBOM reference
                        FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                    }
                }
            } else { // PS reference has a previous revision
                // Recursive call on previous revision (with MBOM reference in parameter)
                String mbomRefPIDSynchedToPreviousPSRevision = getSynchedScopeMBOMRefFromPSRef(context, plmSession, previousRevPSRefPID, mbomRefPID, newRefPIDList);

                // New revision on the MBOM reference returned by the recursive call and return this new MBOM reference
                returnMBOMRefPID = newRevisionMBOMReference(context, plmSession, mbomRefPIDSynchedToPreviousPSRevision);
                newRefPIDList.add(returnMBOMRefPID);

                try {
                    plmSession.flushSession();
                } catch (Exception e) {
                }

                // !! CAREFULL : remove the scope on the new revision of the MBOM reference (by default, the new revision duplicates the scope)
                List<String> modifiedInstanceList = FRCMBOMModelerUtility.deleteScopeLinkAndDeleteChildImplementLinks(context, plmSession, returnMBOMRefPID, false);
                System.out.println("FRC : modified Instances : " + modifiedInstanceList);

                try {
                    plmSession.flushSession();
                } catch (Exception e) {
                }

                // Create a scope link between the PS reference and the new MBOM reference revision
                FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);

                // Map the attributes
                String psRefInfoStr = MqlUtil.mqlCommand(context,
                        "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value attribute[PLMEntity.V_description].value attribute[PLMReference.V_ApplicabilityDate].value dump |", false,
                        false);
                String[] psRefInfo = psRefInfoStr.split("\\|", -2);

                if ("".equals(psRefInfo[2]))
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefInfo[0] + "' PLMEntity.V_description '" + psRefInfo[1] + "'", false, false);
                else
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefInfo[0] + "' PLMEntity.V_description '" + psRefInfo[1]
                            + "' PLMReference.V_ApplicabilityDate '" + psRefInfo[2] + "'", false, false);
            }
        }

        return returnMBOMRefPID;
    }

    public static String restructureInstance(Context context, PLMCoreModelerSession plmSession, String[] args) throws Exception {
        String newInstPID = null;

        String oldInstancePID = args[0];
        String newParentRefPID = args[1];

        // Get the info on the old instance
        // String instanceInfoStr = MqlUtil.mqlCommand(context, "print connection " + oldInstancePID + " select type logicalid to.physicalid dump |", false, false);
        // String[] instanceInfo = instanceInfoStr.split("\\|", -2);

        // Get the child reference pointed by the instance
        String childRefPID = MqlUtil.mqlCommand(context, "print connection " + oldInstancePID + " select to.physicalid dump |", false, false);

        // Get the parent reference of the instance
        String parentRefPID = MqlUtil.mqlCommand(context, "print connection " + oldInstancePID + " select from.physicalid dump |", false, false);

        // Get the attribute values of the instance
        DomainRelationship instObj = new DomainRelationship(oldInstancePID);
        Map instAttributes = instObj.getAttributeMap(context, true);
        instAttributes.remove("PLMInstance.V_TreeOrder");

        // Get the effectivity of the instance
        List<String> oldInstancePIDList = new ArrayList<String>();
        oldInstancePIDList.add(oldInstancePID);

        Map<String, String> effMap = FRCMBOMModelerUtility.getEffectivityOnInstanceList(context, plmSession, oldInstancePIDList, false);

        String effXMLStr = effMap.get(oldInstancePID);

        // Get the implement link of the instance
        List<String> implementPath = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, oldInstancePID);

        // Get the effectivity checksum of the instance
        String checksum = FRCMBOMModelerUtility.getEffectivityChecksumStoredOnInstance(context, oldInstancePID);

        // Delete the instance
        instObj.closeRelationship(context, true);
        DomainRelationship.disconnect(context, oldInstancePID);

        // Create an identical new instance
        newInstPID = createInstance(context, plmSession, newParentRefPID, childRefPID);
        // String newInstPID = MqlUtil.mqlCommand(context, "add connection '" + instanceInfo[0] + "' from " + newParentRefPID + " to " + instanceInfo[2] + " select physicalid dump |", false, false);

        // Replicate all the attributes values on the new instance
        DomainRelationship newInstObj = new DomainRelationship(newInstPID);
        newInstObj.setAttributeValues(context, instAttributes);

        // Replicate the effectivity on this new instance
        if (effXMLStr != null && !"".equals(effXMLStr)) {
            // Set the model on the parent reference of the instance
            List<String> parentModelPIDList = FRCMBOMModelerUtility.getAttachedModelsOnReference(context, plmSession, parentRefPID);

            List<String> newParentRefPIDList = new ArrayList<String>();
            newParentRefPIDList.add(newParentRefPID);

            FRCMBOMModelerUtility.attachModelsOnReferencesAndSetOptionsCriteria(context, plmSession, newParentRefPIDList, parentModelPIDList);

            FRCMBOMModelerUtility.setOrUpdateEffectivityOnInstance(context, plmSession, newInstPID, effXMLStr);
        }

        // Replicate the effectivity checksum on the new instance
        FRCMBOMModelerUtility.storeEffectivityChecksumOnInstance(context, newInstPID, checksum);

        // Replicate the implement link on this new instance, WITHOUT UPDATE OF THE EFFECTIVITY! We want to keep the status (broken or solved) of the link.
        if (implementPath.size() > 0) {
            StringBuffer implementPIDPathSB = new StringBuffer();
            for (int i = 0; i < implementPath.size(); i++) {
                if (implementPIDPathSB.length() > 0)
                    implementPIDPathSB.append("/");

                implementPIDPathSB.append(implementPath.get(i));
            }

            List<String> mbomLeafInstancePIDList = new ArrayList();
            mbomLeafInstancePIDList.add(newInstPID);
            List<String> trimmedPSPathList = new ArrayList();
            trimmedPSPathList.add(implementPIDPathSB.toString());
            String retStr = FRCMBOMModelerUtility.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, false);
            if (!"".equals(retStr))
                throw new Exception(retStr);

        }

        return newInstPID;
    }

    public static String trimPSPathToClosestScope(Context context, PLMCoreModelerSession plmSession, String mbomPathPID, String psPathPID) throws Exception {
        String trimmedPSPath = null;

        // Get the closest PS scoped reference from the MBOM (source) path
        String psScopeRefPID = null;
        String[] mbomPathPIDList = mbomPathPID.split("/");

        int mbomPathIndex = mbomPathPIDList.length - 1;

        if (mbomPathPIDList.length == 1) {
            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(mbomPathPIDList[0]);
            String psRefPID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

            if (psRefPID != null && !"".equals(psRefPID))
                psScopeRefPID = psRefPID;

        } else {
            while (psScopeRefPID == null && mbomPathIndex > 0) {
                String mbomInstancePID = mbomPathPIDList[mbomPathIndex];
                String mbomParentRefPID = MqlUtil.mqlCommand(context, "print connection " + mbomInstancePID + " select from.physicalid dump |", false, false);

                List<String> inputListForGetScope = new ArrayList<String>();
                inputListForGetScope.add(mbomParentRefPID);
                String psRefPID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                if (psRefPID != null && !"".equals(psRefPID))
                    psScopeRefPID = psRefPID;

                mbomPathIndex--;
            }
        }

        if (psScopeRefPID != null) { // A scope was found
            // Search the instance in the ps path that corresponds to this ps ref
            String[] psPathPIDList = psPathPID.split("/");

            int psPathIndex = psPathPIDList.length - 1;
            String psScopeInstPID = null;
            while (psScopeInstPID == null && psPathIndex > 0) {
                String psInstancePID = psPathPIDList[psPathIndex];
                String psParentRefPID = MqlUtil.mqlCommand(context, "print connection " + psInstancePID + " select from.physicalid dump |", false, false);

                if (psParentRefPID.equals(psScopeRefPID))
                    psScopeInstPID = psInstancePID;

                psPathIndex--;
            }

            if (psScopeInstPID != null) { // The scope is in the ps path
                trimmedPSPath = psPathPID.substring(psPathPID.indexOf(psScopeInstPID));
            }

        }

        return trimmedPSPath;
    }

    public static String createInstance(Context context, PLMCoreModelerSession plmSession, String parentRefPID, String childRefPID) throws Exception {
        // Compute the default instance title
        String childRefTitle = MqlUtil.mqlCommand(context, "print bus " + childRefPID + " select attribute[PLMEntity.V_Name].value dump |", false, false);

        String refTitleListStr = MqlUtil.mqlCommand(context, "print bus " + parentRefPID + " select from[PLMInstance].to.attribute[PLMEntity.V_Name].value dump |", false, false);
        int newOccNbr = refTitleListStr.split(Pattern.quote(childRefTitle), -1).length;
        String instanceTitle = childRefTitle + "." + newOccNbr;

        Hashtable instanceAttributes = new Hashtable();
        instanceAttributes.put("PLM_ExternalID", instanceTitle);

        String relPID = FRCMBOMModelerUtility.createMBOMInstance(context, plmSession, parentRefPID, childRefPID, instanceAttributes);
        // MBO-164-MBOM performance issue:START-H65 15/11/2017
        flushSession(plmSession);
        if (UIUtil.isNotNullAndNotEmpty(relPID)) {
            String relationshipId = MqlUtil.mqlCommand(context, "print connection " + relPID + " select id dump", false, false);
            String output = MqlUtil.mqlCommand(context, "mod connection " + relationshipId + " add interface FRCCustoExtension1 ", false, false);
            flushSession(plmSession);
        }
        // MBO-164-Below MBOM performance issue:END-H65 15/11/2017

        return relPID;
    }

    public static String createInstance(Context context, PLMCoreModelerSession plmSession, String parentRefPID, String childRefPID, Hashtable instanceAttributes) throws Exception {

        String relPID = FRCMBOMModelerUtility.createMBOMInstance(context, plmSession, parentRefPID, childRefPID, instanceAttributes);
        flushSession(plmSession);
        return relPID;
    }

    public static String duplicateMBOMStructure(Context context, PLMCoreModelerSession plmSession, String rootMBOMRefPID, String changeObject) throws Exception {
        // Get all the reference PIDs in the structure to duplicate : expand all levels, no config filter, no bus or rel selects (we only want the ref physicalid)
        MapList resExpand = FRCMBOMModelerUtility.getVPMStructure(context, plmSession, rootMBOMRefPID, new StringList("physicalid"), new StringList(), (short) 0, null, null);

        IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();
        IVPLMProductionSystemAuthoring modeler = (IVPLMProductionSystemAuthoring) plmSession
                .getModeler("com.dassault_systemes.vplm.ProductionSystemAuthoring.implementation.VPLMProductionSystemAuthoring");
        HashMap listCloneRefID = new HashMap();
        HashMap listCloneInstID = new HashMap();
        String refID;

        String instID;
        for (Object levelObj : resExpand) {
            Map levelMap = (Map) levelObj;
            refID = (String) levelMap.get("physicalid");
            if (refID != null) {
                listCloneRefID.put(refID, refID);
            }
            instID = (String) levelMap.get("physicalid[connection]");
            if (instID != null) {
                listCloneInstID.put(instID, instID);
            }
        }

        List<String> refAndInstPIList = new ArrayList<String>();
        refAndInstPIList.add(rootMBOMRefPID);
        List<String> refPIDList = new ArrayList<String>(listCloneRefID.values());
        List<String> instPIDList = new ArrayList<String>(listCloneInstID.values());
        refAndInstPIList.addAll(refPIDList);
        refAndInstPIList.addAll(instPIDList);

        List<String> newRefPIDList = FRCMBOMModelerUtility.partialDuplicateMBOMStructure(context, plmSession, refAndInstPIList);

        // Attach all duplicated reference to change object.
        attachListObjectsToChange(context, plmSession, changeObject, newRefPIDList);

        return newRefPIDList.get(0); // Return the PID of the new root ref
    }

    public static String createMBOMReference(Context context, PLMCoreModelerSession plmSession, String type, String magnitudeType, Map<String, String> attributes) throws Exception {
        String isContinuous = MqlUtil.mqlCommand(context, "print type '" + type + "' select kindof[DELFmiContinuousFunctionReference] dump |", false, false);

        String newObjPID = null;

        if ("TRUE".equalsIgnoreCase(isContinuous)) {
            newObjPID = FRCMBOMModelerUtility.createMBOMContinuousReference(context, plmSession, type, magnitudeType, attributes);
        } else {
            newObjPID = FRCMBOMModelerUtility.createMBOMDiscreteReference(context, plmSession, type, attributes);
        }
        flushSession(plmSession);
        return newObjPID;

    }

    public static String newRevisionMBOMReference(Context context, PLMCoreModelerSession plmSession, String refPID) throws Exception {
        // Get the latest existing revision (the one given is not necessarily the latest one)
        String latestRevisionPIDsStr = MqlUtil.mqlCommand(context, "print bus " + refPID + " select majorids.lastmajorid dump |", false, false);
        String[] latestRevisionPIDs = latestRevisionPIDsStr.split("\\|");
        String latestRevisionPID = latestRevisionPIDs[0];

        List<String> referencesToRevise = new ArrayList<String>();
        referencesToRevise.add(latestRevisionPID);

        return FRCMBOMModelerUtility.newVersionMBOMReference(context, plmSession, referencesToRevise).get(0);
    }

    // ==== START : Configuration utilities
    public static String formatEffExpToNeutral(Context context, String sEffExpr) throws Exception {
        // Replace tilde by #
        // Tilde is not accepted when used into an emxIndentedTable cell, so it may have been already replaced into the expression
        // To be consistent and compatible with all possible formats (with or without tilde) we are applying the same transformation here
        String sSeparator = "#";
        String sOrSeparator = ",";
        if (true == sEffExpr.contains("~"))
            sEffExpr = sEffExpr.replaceAll("~", "#");
        StringBuffer sbExp = new StringBuffer();
        String[] sValues = sEffExpr.split("@EF_FO");
        for (int i = 0; i < sValues.length; i++) {
            String[] orValues = null;
            orValues = sValues[i].split(sOrSeparator);
            String sSavedFeat = "";

            for (int j = 0; j < orValues.length; j++) {
                int stPos = -1, midPos = -1, endPos = -1;

                if (0 == j) {
                    stPos = orValues[j].indexOf("PHY@EF:");
                    midPos = orValues[j].indexOf(sSeparator);
                    endPos = midPos + 33;
                } else {
                    stPos = orValues[j].indexOf("PHY@EF:");
                    midPos = orValues[j].indexOf(sSeparator);
                    endPos = midPos + 33;
                }

                if (-1 != stPos && -1 != midPos && -1 != endPos) {
                    String sRel = orValues[j].substring(stPos + 7, midPos);
                    String sModel = orValues[j].substring(midPos + 1, endPos);

                    String sFeat = "", sOpt = "";
                    String[] aIds = new String[1];
                    aIds[0] = sRel;
                    StringList lSelects = new StringList(2);
                    lSelects.add(DomainRelationship.SELECT_FROM_NAME);
                    lSelects.add(DomainRelationship.SELECT_TO_NAME);
                    MapList mlRel = DomainRelationship.getInfo(context, aIds, lSelects);
                    if (null != mlRel && 0 < mlRel.size()) {
                        Map mObj = (Map) mlRel.get(0);
                        sFeat = (String) mObj.get(DomainRelationship.SELECT_FROM_NAME);
                        sOpt = (String) mObj.get(DomainRelationship.SELECT_TO_NAME);
                    }

                    String sNew = "";
                    String sStart = "", sEnd = "";
                    if (0 == j) {
                        sStart = "(";
                        sNew = sFeat;
                        sSavedFeat = sFeat;
                    }
                    // Check we are still on the same conf feature
                    else {
                        if (!sFeat.equals(sSavedFeat))
                            throw new Exception("Error while building neutral expression - multiple features into OR expression");
                    }
                    if (orValues.length - 1 == j)
                        sEnd = ")";
                    String sInitial = sStart + "PHY@EF:" + sRel + sSeparator + sModel + sEnd;
                    sNew += "." + sOpt;
                    String sNewExp = orValues[j].replace(sInitial, sNew);
                    sbExp.append(sNewExp);
                } else {
                    sbExp.append(orValues[j]);
                }
            }
        }

        return sbExp.toString();
    }

    public static String convertEffNeutralToXML(Context context, String sNeutralEff, String sModelId, String sMode) throws Exception {
        FRCSettings sett = new FRCSettings();
        sett.AndOperator = " AND ";
        sett.OrOperator = " OR ";
        sett.NotOperator = " NOT ";

        FRCEffExpr effFactory = new FRCEffExpr(sett, false, false, false);
        FRCEffExpr curEff = effFactory.parseStringToEffExpr(sett, sNeutralEff);
        String sXMLEff = curEff.convertToXML(sMode);

        DomainObject domModel = new DomainObject(sModelId);
        String sModelName = domModel.getName(context);
        String sXMLExpr = addXMLHeaderToEffExpr(sXMLEff, sModelName, sMode);

        return sXMLExpr;
    }

    public static String addXMLHeaderToEffExpr(String sXMLEff, String sModelName, String sMode) throws Exception {
        // FRC: Fixed Bug#556:EBOM and MBOM Refinement function doesn't work (WP2-07 WP2-08 WP-01 and WP3-02)
        String sXMLExpr = "";
        if ("filter".equals(sMode)) {
            String sHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CfgFilterExpression xmlns=\"urn:com:dassault_systemes:config\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:com:dassault_systemes:config CfgFilterExpression.xsd\"><FilterSelection SelectionMode=\"Strict\">";
            String sContext = "<Context HolderType=\"Model\" HolderName=\"" + sModelName + "\">";
            String sSuffix = "</Context></FilterSelection></CfgFilterExpression>";

            sXMLExpr = sHeader + sContext + sXMLEff + sSuffix;
        } else if ("set".equals(sMode)) {
            String sHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><CfgEffectivityExpression xmlns=\"urn:com:dassault_systemes:config\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:com:dassault_systemes:config CfgEffectivityExpression.xsd\"><Expression>";
            String sContext = "<Context HolderType=\"Model\" HolderName=\"" + sModelName + "\">";
            String sSuffix = "</Context></Expression></CfgEffectivityExpression>";

            sXMLExpr = sHeader + sContext + sXMLEff + sSuffix;
        }
        return sXMLExpr;
    }

    public static String[] getExpressionForPC(Context context, String pcObjectId) throws Exception {
        EffectivityFramework eff = new EffectivityFramework();

        MapList mapPCSelectedOptions = ProductConfiguration.getSelectedOptions(context, pcObjectId, false, true);
        MapList selectedOptionsMap = new MapList();
        // If PC is created for a part then Model Id is not required
        if (!mapPCSelectedOptions.isEmpty()) {
            for (int i = 0; i < mapPCSelectedOptions.size(); i++) {
                Map temp = (Map) mapPCSelectedOptions.get(i);
                if (!mxType.isOfParentType(context, temp.get("from.type").toString(), ConfigurationConstants.TYPE_PRODUCTS)) {
                    selectedOptionsMap.add(temp);
                }
            }
        }

        Object objModelID = ProductConfiguration.getModelPhysicalIdBasedOnProductConfiguration(context, pcObjectId);
        StringList slModelID = ConfigurationUtil.convertObjToStringList(context, objModelID);
        StringBuffer strActualExpression = new StringBuffer();
        StringBuffer strDisplayExpression = new StringBuffer();

        MapList dataList = selectedOptionsMap;

        for (int i = 0; i < slModelID.size(); i++) {
            String contextId = (String) slModelID.get(i);

            StringList strRelSelect = new StringList();
            strRelSelect.add("physicalid");

            String contextPhyId = (String) (DomainObject.newInstance(context, contextId)).getInfo(context, "physicalid");
            String strRelIds[] = new String[dataList.size()];
            for (int ii = 0; ii < dataList.size(); ii++) {
                Map mapFOs = (Map) dataList.get(ii);
                strRelIds[ii] = (String) mapFOs.get(DomainRelationship.SELECT_ID);
            }

            StringList strRelSelects = new StringList();
            strRelSelects.addElement("physicalid");
            strRelSelects.addElement("from.physicalid");
            strRelSelects.addElement("to.physicalid");

            MapList mapPhysicalIds = DomainRelationship.getInfo(context, strRelIds, strRelSelects);
            boolean addOperator = false;
            for (int ii = 0; ii < mapPhysicalIds.size(); ii++) {
                Map mapFOs = (Map) mapPhysicalIds.get(ii);
                com.matrixone.json.JSONObject effObj = new com.matrixone.json.JSONObject();
                effObj.put("contextId", contextPhyId); // physicalid of the
                                                       // Model context
                effObj.put("parentId", (String) mapFOs.get("from.physicalid")); // physicalid of the CF
                effObj.put("objId", (String) mapFOs.get("to.physicalid")); // physicalid of the CO
                effObj.put("relId", (String) mapFOs.get("physicalid")); // physicalid of the CO rel
                effObj.put("insertAsRange", false);

                String jsonString = effObj.toString();
                Map formatedExpr = eff.formatExpression(context, "FeatureOption", jsonString);

                String actualFormatedExpr = (String) formatedExpr.get(EffectivityFramework.ACTUAL_VALUE);
                if (actualFormatedExpr != null && !actualFormatedExpr.isEmpty()) {
                    if ((i + 1) != dataList.size() && addOperator) {
                        strActualExpression.append(" " + "AND" + " ");
                        strDisplayExpression.append(" " + "AND" + " ");
                        addOperator = false;
                    }
                    strActualExpression.append(actualFormatedExpr);
                    strDisplayExpression.append((String) formatedExpr.get(EffectivityFramework.DISPLAY_VALUE));
                    addOperator = true;
                }
            }

        }
        String[] result = new String[2];
        result[0] = strActualExpression.toString();
        result[1] = strDisplayExpression.toString();

        return result;
    }
    // ==== END : Configuration utilities

    // KYB Added method for feature #247
    public HashMap getListForResourceTypeField(Context context, String[] args) { // Called from form FRCMBOMResourceCreateAndPropertiesForm, from Type field
        HashMap retMap = new HashMap();
        Locale loc = context.getLocale();
        String key = "FRCMBOMCentral.Resource.TypeDisplayItems";
        StringList resourceTypeDisplayList = new StringList();
        StringList resourceTypeValueList = new StringList();

        String resourceTypeDisplayListStr = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key);
        resourceTypeDisplayList = FrameworkUtil.split(resourceTypeDisplayListStr, ",");

        key = "FRCMBOMCentral.Resource.TypeValueItems";
        String resourceTypeValueListStr = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", loc, key);
        resourceTypeValueList = FrameworkUtil.split(resourceTypeValueListStr, ",");

        retMap.put("field_choices", resourceTypeValueList);
        retMap.put("field_display_choices", resourceTypeDisplayList);

        return retMap;
    }

    /*
     * Insert reference under ref pass in paramter
     * 
     * @param Parent Ref PID
     * 
     * @param Reference to insert
     * 
     * @return
     * 
     */
    public static String insertExistingManufItemFromDBWOLinks(Context context, String[] args) throws Exception {// Called from FRCInsertExistingPostProcess.jsp
        PLMCoreModelerSession plmSession = null;
        PLMxRefInstanceEntity instanceEntity = null;
        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomParentRefPID = args[0];
            String mbomRefPID = args[1];

            IPLMxCoreAccess coreAccess = plmSession.getVPLMAccess();
            String mbomParentRefPLMID = coreAccess.convertM1IDinPLMID(new String[] { mbomParentRefPID })[0];

            String mbomRefPLMID = coreAccess.convertM1IDinPLMID(new String[] { mbomRefPID })[0];

            // Create a new instance
            Hashtable htAttribute = new Hashtable();
            instanceEntity = FRCMBOMModelerAPI.insertExistingPredecessor(context, plmSession, mbomRefPLMID, mbomParentRefPLMID, htAttribute);

            String instancePLMID = instanceEntity.getPLMIdentifier();
            PLMID newInstancePLMIDObj = PLMID.buildFromString(instancePLMID);

            String newInstPID = newInstancePLMIDObj.getPid();

            // Add the custom extension for the effectivity checksum
            MqlUtil.mqlCommand(context, "mod connection " + newInstPID + " add interface FRCCustoExtension1", false, false);

            // Create an implement link

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            return newInstPID;
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    /*
     * In the Insert From Database Context check scope context. and return a list of instance if
     * 
     * @param
     * 
     * @param
     * 
     * @return
     * 
     */
    public static HashMap checkInsertFromDBContext(Context context, String[] args) throws Exception {// Called from FRCInsertExistingFromDBPostProcess.jsp

        HashMap<String, String> listScopedObject = new HashMap<String, String>();

        PLMCoreModelerSession plmSession = null;

        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String objectId = args[0];
            String selectedItem = args[1];

            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(selectedItem);
            String psRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

            if (psRefScopePID == null || "".equals(psRefScopePID)) {
                // getAllChildren
                MapList resExpand = FRCMBOMModelerUtility.getVPMStructure(context, plmSession, selectedItem, new StringList("physicalid"), new StringList("id"), (short) 0, null, null);

                List<String> chilPID = new ArrayList<String>();
                String refID;
                for (Object levelObj : resExpand) {
                    Map levelMap = (Map) levelObj;
                    refID = (String) levelMap.get("physicalid");
                    if (refID != null) {
                        chilPID.add(refID);
                    }
                }
                List<String> scopedChilPID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, chilPID);

                int resScopeIndex = 0;
                for (String itrObjects : chilPID) {
                    String objId = (String) itrObjects;
                    String scopedID = scopedChilPID.get(resScopeIndex);
                    resScopeIndex++;
                    if (scopedID != null && !"".equals(scopedID)) {
                        listScopedObject.put(objId, scopedID);
                    }
                }
            } else {
                String objectPID = new DomainObject(selectedItem).getInfo(context, "physicalid");
                listScopedObject.put(objectPID, psRefScopePID);
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);

        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        } finally {
            return listScopedObject;
        }
    }

    public static MapList getAvailablePSRefScopesProgram(Context context, String[] args) throws Exception { // Called from FRCInsertExistingPreProcess2.jsp
        MapList returnML = new MapList();

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String objectPID = (String) paramMap.get("objectId");

            List<String> mbomRefScopesList = FRCMBOMModelerUtility.getScopingReferencesFromList(context, plmSession, objectPID);

            for (String mbomRefScopePID : mbomRefScopesList) {
                Map objMap = new HashMap();
                objMap.put("id", mbomRefScopePID);
                returnML.add(objMap);
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnML;
    }

    public static void insertExistingCreateImplementLinks(Context context, String[] args) throws Exception {// Called from FRCInsertExistingPostProcess.jsp
        PLMCoreModelerSession plmSession = null;

        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            String mbomPath = args[0];
            String psPath = args[1];
            String newInstPID = args[2];

            // Create an implement link
            String trimmedPSPath = trimPSPathToClosestScope(context, plmSession, mbomPath, psPath);

            if (trimmedPSPath == null || "".equals(trimmedPSPath))
                throw new Exception("No parent scope exists.");

            // Put a new implement link
            List<String> mbomLeafInstancePIDList = new ArrayList();
            mbomLeafInstancePIDList.add(newInstPID);
            List<String> trimmedPSPathList = new ArrayList();
            trimmedPSPathList.add(trimmedPSPath);
            String retStr = FRCMBOMModelerUtility.setImplementLinkBulk(context, plmSession, mbomLeafInstancePIDList, trimmedPSPathList, true);
            if (!"".equals(retStr))
                throw new Exception(retStr);

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getAvailableScopedReferenceProgram(Context context, String[] args) throws Exception { // Called from FRCInsertExistingPreProcess2.jsp
        MapList returnML = new MapList();

        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String mbomRefPID = (String) paramMap.get("objectid");
            String listInstanceID = (String) paramMap.get("listInstanceID");
            String[] array = listInstanceID.split(":");

            for (int i = 0; i < array.length; i++) {
                Map objMap = new HashMap();
                objMap.put("id", array[i]);
                returnML.add(objMap);

            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
            // ContextUtil.abortTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }

        return returnML;
    }

    public String getPSPathFromID(Context context, String[] args) throws Exception {
        String pathPS = "";

        String rootPSID = args[0];
        String instanceID = args[1];
        MapList res;

        ContextUtil.startTransaction(context, false);
        short stopLevel = 0;
        try {
            res = getExpandPS(context, rootPSID, stopLevel, null, null, null, EXPD_REL_SELECT, EXPD_BUS_SELECT);

            // START UM5c06 : Build Paths and save theses in the return maps + add "hasChildren=false" where needed

            // Store path in a Map to be able to manage unsorted return MapList
            HashMap<String, String> mapPaths = new HashMap<String, String>();
            HashMap<String, String> mapPathsLogical = new HashMap<String, String>();

            DomainObject objectDOM = new DomainObject(rootPSID);

            String rootPID = objectDOM.getInfo(context, "physicalid");
            mapPaths.put(rootPID, rootPID);

            String rootLID = objectDOM.getInfo(context, "logicalid");
            mapPathsLogical.put(rootPID, rootLID);

            // Declare variable before to improve prefs
            Map<String, Object> mapObj;
            String objPID, objFromPID, objPIDConnection, objLID, objLIDConnection;
            String newPath = "";
            String newPathLogical = "";
            for (int i = 0; i < res.size(); i++) {
                mapObj = (Map) res.get(i);
                objPID = (String) mapObj.get("physicalid");
                objPIDConnection = (String) mapObj.get("physicalid[connection]");

                objLID = (String) mapObj.get("logicalid");
                objLIDConnection = (String) mapObj.get("logicalid[connection]");
                objFromPID = (String) mapObj.get("from.physicalid");
                newPath = mapPaths.get(objFromPID);
                newPathLogical = mapPathsLogical.get(objFromPID);
                if (newPath != null && !newPath.isEmpty()) {
                    newPath = newPath + "/" + objPIDConnection;
                    newPathLogical = newPathLogical + "/" + objLIDConnection;
                } else {
                    newPath = objPID;
                    newPathLogical = objLID;
                }
                mapPaths.put(objPID, newPath);
                mapPathsLogical.put(objPID, newPathLogical);

                mapObj.put("pathPID", newPath);
                mapObj.put("pathLID", newPathLogical);
                // set ids to use physicalids
                mapObj.put("id", objPID);
                mapObj.put("id[connection]", objPIDConnection);

                // Add hasChildren info here
                if ((i + 1) < res.size()) {
                    Map nextMapObj = (Map) res.get(i + 1);
                    String strLevelNow = (String) mapObj.get("level");
                    String strLevelNext = (String) nextMapObj.get("level");
                    int levelNow = Integer.parseInt(strLevelNow);
                    int levelNext = Integer.parseInt(strLevelNext);
                    if (levelNext > levelNow) {
                        mapObj.put("hasChildren", "true");
                    } else {
                        mapObj.put("hasChildren", "false");
                    }
                } else {
                    // Last object so no childrens
                    mapObj.put("hasChildren", "false");
                }

                // hen info search is found end of the loop proces
                if (objPIDConnection.equals(instanceID)) {
                    i = res.size();
                    pathPS = newPath;
                }
            }

            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            ContextUtil.abortTransaction(context);
            throw ex;
        }

        return pathPS;
    }

    public static Vector getInstanceNameColumn(Context context, String[] args) throws Exception {
        List<String> listIDs = null;

        try {
            ContextUtil.startTransaction(context, false);

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            listIDs = new ArrayList<String>();
            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String objectID = (String) mapObjectInfo.get("id");
                listIDs.add(objectID);
            }
            StringList busSelect = new StringList();
            busSelect.add("attribute[PLMInstance.PLM_ExternalID]");
            MapList resultInfoML = DomainRelationship.getInfo(context, listIDs.toArray(new String[0]), busSelect);

            int index = 0;
            for (Iterator itrObjects = resultInfoML.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String instanceName = (String) mapObjectInfo.get("attribute[PLMInstance.PLM_ExternalID]");

                vecResult.add(instanceName);

            }

            ContextUtil.commitTransaction(context);

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static Vector getInstanceFromColumn(Context context, String[] args) throws Exception {
        List<String> listIDs = null;

        try {
            ContextUtil.startTransaction(context, false);

            // Create result vector
            Vector vecResult = new Vector();

            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");

            listIDs = new ArrayList<String>();
            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String objectID = (String) mapObjectInfo.get("id");
                listIDs.add(objectID);
            }
            StringList busSelect = new StringList();
            busSelect.add("to");
            MapList resultInfoML = DomainRelationship.getInfo(context, listIDs.toArray(new String[0]), busSelect);

            int index = 0;
            for (Iterator itrObjects = resultInfoML.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String fromInfo = (String) mapObjectInfo.get("to");

                vecResult.add(fromInfo);

            }

            ContextUtil.commitTransaction(context);

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    // FRC Start: Fixed Bug 671-Quantity value on Materials
    public static void insertNewManufItemAndUpdateQuantity(Context context, String[] args) throws Exception { // Called from FRCInsertManufItemPreProcess.jsp
        PLMCoreModelerSession plmSession = null;

        try {
            ContextUtil.startTransaction(context, true);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String parentPID = (String) requestMap.get("objectId");
            String quantity = (String) requestMap.get("Quantity");

            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String newRefPID = (String) paramMap.get("newObjectId");

            String relID = createInstance(context, plmSession, parentPID, newRefPID);

            // Update Quantity
            String childRefType = MqlUtil.mqlCommand(context, "print bus " + newRefPID + " select type dump |", false, false);

            String continuousRefTypesListStr = MqlUtil.mqlCommand(context, "print type DELFmiFunctionPPRContinuousReference select derivative dump |", false, false);
            String[] continuousRefTypesListArray = continuousRefTypesListStr.split("\\|");
            List<String> continuousRefTypesList = Arrays.asList(continuousRefTypesListArray);

            if (continuousRefTypesList.contains(childRefType)) {
                // Get Connection Id
                String connectionID = MqlUtil.mqlCommand(context, "print bus " + newRefPID + " select to.id dump |", false, false);

                String refQuantityValue = "";
                if (newRefPID == null || "".equals(newRefPID))
                    refQuantityValue = "1.0";
                else {
                    refQuantityValue = MqlUtil.mqlCommand(context, "print bus " + newRefPID + " select attribute[V_ContQuantity].value dump |", false, false);
                }

                double refQuantity = Double.parseDouble(refQuantityValue);
                double newVal = Double.parseDouble(quantity);
                double usageCoeff = newVal / refQuantity;
                String instanceUsgaeCoefficient = Double.toString(usageCoeff);

                if (relID != null && !"".equals(relID)) {
                    flushSession(plmSession);
                    MqlUtil.mqlCommand(context, "mod connection " + relID + " " + "ProcessInstanceContinuous.V_UsageContCoeff" + " " + instanceUsgaeCoefficient, false, false);
                }
            }

            flushAndCloseSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception exp) {
            exp.printStackTrace();
            flushAndCloseSession(plmSession);
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }
    // FRC End: Fixed Bug 671-Quantity value on Materials

    // FRC Fixed Bug 1364-MBOM item shouldn't be inserted under itself:START: H65-02/03/2017
    // @com.matrixone.apps.framework.ui.ProgramCallable
    public StringList getExcludeOIDList(Context context, String[] args) throws Exception {
        StringList excludeList = new StringList();

        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String selectedchildID = (String) paramMap.get("targetMBOMid");
            String mbomPath = (String) paramMap.get("mbomPath");
            String mbomObjectPID = null;
            if (mbomPath.contains("/")) {
                String[] mbomPathList = mbomPath.split("/");

                mbomObjectPID = mbomPathList[0];
            } else
                mbomObjectPID = mbomPath;
            // Add selected Id in exclude list to avoid MBOM insertion under itself
            String strcurrentObjId = MqlUtil.mqlCommand(context, "print bus " + selectedchildID + " select id dump", false, false);
            if (!excludeList.contains(strcurrentObjId))
                excludeList.add(strcurrentObjId);

            // Bug #1594-Wrong expand used-H65:13/09/2017-START

            // String strObjConnectedToParent = MqlUtil.mqlCommand(context, "expand bus " + selectedchildID + " terse to recurse to all dump |", false, false);

            /*
             * if(!strObjConnectedToParent.isEmpty()){ String[] mbomArray1=strObjConnectedToParent.split("\\n");
             * 
             * for(int i=0; i<mbomArray1.length;i++){ String strObjId=mbomArray1[i]; StringList strSplit = FrameworkUtil.split(strObjId, "|"); String mbomObjId = (String) strSplit.get(3);
             * if(!excludeList.contains(mbomObjId)) excludeList.add(mbomObjId);
             * 
             * }
             * 
             * }
             */

            DomainObject domMfgItem = new DomainObject(selectedchildID);
            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_ID);
            objSelects.addElement(DomainConstants.SELECT_NAME);
            matrix.util.Pattern relPattern = new matrix.util.Pattern(PropertyUtil.getSchemaProperty(context, "relationship_DELFmiFunctionIdentifiedInstance"));

            MapList mlToConnected = domMfgItem.getRelatedObjects(context, relPattern.getPattern(), // relationshipPattern
                    "*", // typePattern
                    objSelects, // objectSelects
                    null, // relationshipSelects
                    true, // getTo
                    false, // getFrom
                    (short) 0, // recurseToLevel
                    null, // objectWhere,
                    null, // relationshipWhere
                    (int) 0); // limi

            if (mlToConnected.size() > 0) {

                for (int i = 0; i < mlToConnected.size(); i++) {
                    Map mapChild = (Map) mlToConnected.get(i);
                    String mbomObjId = (String) mapChild.get("id");
                    if (!excludeList.contains(mbomObjId))
                        excludeList.add(mbomObjId);

                    // Bug #1594-Wrong expand used-H65:13/09/2017-END

                }

            }

            // Bug #1594-Wrong expand used-H65:13/09/2017-END
        } catch (Exception err) {
            err.printStackTrace();
            throw err;
        }
        return excludeList;
    }
    // FRC Fixed Bug 1364-MBOM item shouldn't be inserted under itself:END: H65:02/03/2017

    // FRC Fixed Bug#1424 On revision of top level EBOM Part MBOMs based on previous revision are lost:START: H65
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getMBOMFromEBOMAllRev(Context context, String[] args) { // Called from command FRCEBOMMBOMTableCmd
        MapList retList = new MapList();
        Vector vReturn = new Vector();
        boolean bTrans = false;

        PLMCoreModelerSession plmSession = null;
        try {

            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            bTrans = true;
            String sId = null;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            if (null != programMap) {
                String objId = (String) programMap.get("objectId");
                StringList objSelectable = new StringList(4);
                objSelectable.addElement(DomainConstants.SELECT_TYPE);
                objSelectable.addElement(DomainConstants.SELECT_REVISION);
                objSelectable.addElement(DomainConstants.SELECT_NAME);

                DomainObject dObj = new DomainObject(objId);
                Map objMap = dObj.getInfo(context, objSelectable);
                String sType = (String) objMap.get(DomainConstants.SELECT_TYPE);
                String sName = (String) objMap.get(DomainConstants.SELECT_NAME);
                String sRevision = (String) objMap.get(DomainConstants.SELECT_REVISION);

                // Current object MBOM added in the list 1st

                MapList mlCurrentPrd = getProductFromEBOM(context, objId);
                if (null != mlCurrentPrd) {
                    for (int i = 0; i < mlCurrentPrd.size(); i++) {
                        Map mObj = (Map) mlCurrentPrd.get(i);
                        String sPhysId = (String) mObj.get("physicalid");
                        List<String> lMBOM = FRCMBOMModelerUtility.getScopingReferencesFromList(context, plmSession, sPhysId);
                        // StringList lMBOM = getMBOMScopePIDListFromPS(context, sPhysId);
                        for (int j = 0; j < lMBOM.size(); j++) {
                            HashMap mNewObj = new HashMap();
                            mNewObj.put(DomainConstants.SELECT_ID, lMBOM.get(j));
                            retList.add(mNewObj);
                        }
                    }
                }

                StringList objSelects = new StringList(4);
                objSelects.addElement(DomainConstants.SELECT_ID);
                objSelects.addElement("physicalid");
                objSelects.addElement(DomainConstants.SELECT_TYPE);
                objSelects.addElement(DomainConstants.SELECT_REVISION);
                objSelects.addElement(DomainConstants.SELECT_NAME);
                objSelects.addElement("attribute[PLMEntity.V_Name]");
                String sWhere = "revision !=" + sRevision;

                MapList mlList = DomainObject.findObjects(context, sType, sName, "*", "*", "*", sWhere, true, objSelects);
                if (null != mlList) {
                    for (int n = 0; n < mlList.size(); n++) {
                        Map tempObj = (Map) mlList.get(n);
                        sId = (String) tempObj.get("id");
                        MapList mlPrd = getProductFromEBOM(context, sId);
                        if (null != mlPrd) {
                            for (int i = 0; i < mlPrd.size(); i++) {
                                Map mObj = (Map) mlPrd.get(i);
                                String sPhysId = (String) mObj.get("physicalid");
                                List<String> lMBOM = FRCMBOMModelerUtility.getScopingReferencesFromList(context, plmSession, sPhysId);
                                // StringList lMBOM = getMBOMScopePIDListFromPS(context, sPhysId);
                                for (int j = 0; j < lMBOM.size(); j++) {
                                    HashMap mNewObj = new HashMap();
                                    mNewObj.put(DomainConstants.SELECT_ID, lMBOM.get(j));
                                    retList.add(mNewObj);
                                }
                            }
                        }
                    }
                }
            }
            if (bTrans) {
                closeSession(plmSession);
                ContextUtil.commitTransaction(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (bTrans) {
                closeSession(plmSession);
                ContextUtil.abortTransaction(context);
            }
        }

        return retList;
    }

    public static Vector getRevisionColumnWithColourCoding(Context context, String[] args) throws Exception { // Called from table FRCEBOMMBOMTable (column FRCMBOMCentral.MBOMTableColumnRevision)
        long startTime = System.currentTimeMillis();
        List<String> listIDs = null;
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            // Create result vector
            Vector vecResult = new Vector();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map paramMap = (HashMap) programMap.get("paramList");

            boolean isexport = false;
            StringList mbomConnectedToCurrentObj = new StringList();
            String export = (String) paramMap.get("exportFormat");
            if (export != null) {
                isexport = true;
            }
            String CurrentObjectId = (String) paramMap.get("objectId");

            MapList mlPrd = getProductFromEBOM(context, CurrentObjectId);
            if (null != mlPrd) {
                for (int i = 0; i < mlPrd.size(); i++) {
                    Map mObj = (Map) mlPrd.get(i);
                    String sPhysId = (String) mObj.get("physicalid");
                    List<String> lMBOM = FRCMBOMModelerUtility.getScopingReferencesFromList(context, plmSession, sPhysId);
                    for (int j = 0; j < lMBOM.size(); j++) {

                        String physicalId = lMBOM.get(j);
                        mbomConnectedToCurrentObj.add(physicalId);
                    }
                }
            }
            MapList objectList = (MapList) programMap.get("objectList");

            listIDs = new ArrayList<String>();
            // Do for each object
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();

                String objectID = (String) mapObjectInfo.get("id");
                listIDs.add(objectID);
            }
            StringList busSelect = new StringList();
            busSelect.add("revision");
            busSelect.add("majorids");
            busSelect.add("majorid");
            busSelect.add("logicalid");
            MapList resultInfoML = DomainObject.getInfo(context, listIDs.toArray(new String[0]), busSelect);

            int index = 0;
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String physicalID = (String) mapObjectInfo.get("id");
                Map<String, String> resultInfoMap = (Map<String, String>) resultInfoML.get(index);
                index++;
                String majorID = resultInfoMap.get("majorid");
                String logicalid = resultInfoMap.get("logicalid");
                String objectRevision = resultInfoMap.get("revision");
                String revisionsListStr = resultInfoMap.get("majorids");
                if (majorID == null)
                    majorID = "temp";

                if (isexport) {
                    vecResult.add(objectRevision);
                } else {

                    if (mbomConnectedToCurrentObj.contains(physicalID)) {

                        if (revisionsListStr.endsWith(physicalID) || revisionsListStr.endsWith(majorID) || revisionsListStr.endsWith(logicalid)) {

                            vecResult.add("<div>" + objectRevision + "</div>");

                        } else {
                            vecResult.add("<div style=\"color:red\">" + objectRevision + "</div>");
                        }
                    } else {
                        vecResult.add("<div style=\"color:orange\">" + objectRevision + "</div>");
                    }
                }

            }

            ContextUtil.commitTransaction(context);
            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }

    public static Vector getAssociatedPartRevision(Context context, String[] args) throws Exception { // Called from table FRCEBOMMBOMTable (column FRCMBOMCentral.MBOMTableColumnRevision)
        long startTime = System.currentTimeMillis();
        List<String> listIDs = null;
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);

            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            // Create result vector
            Vector vecResult = new Vector();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map paramMap = (HashMap) programMap.get("paramList");

            boolean isexport = false;
            String emptyString = DomainConstants.EMPTY_STRING;
            StringList mbomConnectedToCurrentObj = new StringList();
            String export = (String) paramMap.get("exportFormat");
            if (export != null) {
                isexport = true;
            }
            String CurrentObjectId = (String) paramMap.get("objectId");
            MapList objectList = (MapList) programMap.get("objectList");

            StringList objSelectable = new StringList(4);
            objSelectable.addElement(DomainConstants.SELECT_TYPE);
            objSelectable.addElement(DomainConstants.SELECT_REVISION);
            objSelectable.addElement(DomainConstants.SELECT_NAME);

            DomainObject dObj = new DomainObject(CurrentObjectId);
            Map objMap = dObj.getInfo(context, objSelectable);
            String sType = (String) objMap.get(DomainConstants.SELECT_TYPE);
            String sName = (String) objMap.get(DomainConstants.SELECT_NAME);
            String sRevision = (String) objMap.get(DomainConstants.SELECT_REVISION);

            // Current object MBOM added in the list 1st

            MapList mlCurrentPrd = getProductFromEBOM(context, CurrentObjectId);
            if (null != mlCurrentPrd) {
                for (int i = 0; i < mlCurrentPrd.size(); i++) {
                    Map mObj = (Map) mlCurrentPrd.get(i);
                    String sPhysId = (String) mObj.get("physicalid");
                    List<String> lMBOM = FRCMBOMModelerUtility.getScopingReferencesFromList(context, plmSession, sPhysId);
                    // StringList lMBOM = getMBOMScopePIDListFromPS(context, sPhysId);
                    for (int j = 0; j < lMBOM.size(); j++) {
                        vecResult.add("<div>" + sRevision + "</div>");
                    }
                }
            }
            StringList objSelects = new StringList(4);
            objSelects.addElement(DomainConstants.SELECT_ID);
            objSelects.addElement("physicalid");
            objSelects.addElement(DomainConstants.SELECT_TYPE);
            objSelects.addElement(DomainConstants.SELECT_REVISION);
            objSelects.addElement(DomainConstants.SELECT_NAME);
            objSelects.addElement("attribute[PLMEntity.V_Name]");
            String sWhere = "revision !=" + sRevision;

            MapList mlList = DomainObject.findObjects(context, sType, sName, "*", "*", "*", sWhere, true, objSelects);
            if (null != mlList) {
                for (int n = 0; n < mlList.size(); n++) {
                    Map tempObj = (Map) mlList.get(n);
                    String sId = (String) tempObj.get("id");
                    String strRevision = (String) tempObj.get("revision");
                    MapList mlPrd = getProductFromEBOM(context, sId);
                    if (null != mlPrd) {
                        for (int i = 0; i < mlPrd.size(); i++) {
                            Map mObj = (Map) mlPrd.get(i);
                            String sPhysId = (String) mObj.get("physicalid");
                            List<String> lMBOM = FRCMBOMModelerUtility.getScopingReferencesFromList(context, plmSession, sPhysId);
                            // changes added to avoid exception in MBOM table-H65:START
                            if (UIUtil.isNotNullAndNotEmpty(lMBOM.toString())) {
                                for (int j = 0; j < lMBOM.size(); j++) {

                                    vecResult.add("<div>" + strRevision + "</div>");
                                }
                            }
                        }
                    }
                }
            }

            if (objectList.size() != vecResult.size()) {
                for (int p = vecResult.size(); p < objectList.size(); p++) {

                    vecResult.add("<div>" + emptyString + "</div>");
                }

            }
            // changes added to avoid exception in MBOM table-H65:END
            ContextUtil.commitTransaction(context);

            return vecResult;
        } catch (Exception exp) {
            exp.printStackTrace();
            ContextUtil.abortTransaction(context);
            throw exp;
        }
    }
    // FRC Fixed Bug#1424 On revision of top level EBOM Part MBOMs based on previous revision are lost:END: H65

    // getSynchedScopeMBOMRefFromPSRefForUpdateImplentLink call from UpdateImplentLink functon only to prevent impact on code moficiation
    public static String getSynchedScopeMBOMRefFromPSRefForUpdateImplentLink(Context context, PLMCoreModelerSession plmSession, String psRefPID, String mbomRefPID, List<String> newRefPIDList)
            throws Exception {
        String returnMBOMRefPID = null;

        // Check if PS reference has a scope
        List<String> mbomRefPIDScopedWithPSRefList = FRCMBOMModelerUtility.getScopingReferencesFromList(context, plmSession, psRefPID);

        if (mbomRefPIDScopedWithPSRefList.size() > 1) { // PS reference has multiple MBOM scopes
            // If all the elements of the list are within the same revision family, return the latest revision
            boolean isSameMajorIds = true;
            String lastMajorIdsStr = null;

            for (String refPID : mbomRefPIDScopedWithPSRefList) {
                String majorIdsStr = MqlUtil.mqlCommand(context, "print bus " + refPID + " select majorids dump |", false, false);

                if (lastMajorIdsStr != null) {
                    if (!lastMajorIdsStr.equals(majorIdsStr))
                        isSameMajorIds = false;
                }

                lastMajorIdsStr = majorIdsStr;
            }

            if (isSameMajorIds) {
                String[] lastMajorIds = lastMajorIdsStr.split("\\|");

                // Return this MBOM scope
                returnMBOMRefPID = lastMajorIds[lastMajorIds.length - 1];

                // Check if it is a provide
                String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);

                boolean isDirect = false;

                for (String typeInList : baseTypesForMBOMLeafNodes) {
                    if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                        isDirect = true;
                }

                if (!isDirect)
                    throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");
            } else {
                throw new Exception("You cannot provide this part from the EBOM : it has multiple scopes !");
            }
        } else if (mbomRefPIDScopedWithPSRefList.size() == 1) { // PS reference has already one MBOM scope
            // Return this MBOM scope
            returnMBOMRefPID = mbomRefPIDScopedWithPSRefList.get(0);

            // Check if it is a provide
            String returnMBOMRefType = MqlUtil.mqlCommand(context, "print bus " + returnMBOMRefPID + " select type dump |", false, false);

            // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
            String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
            MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
            String psRefApplicabilityDate = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMReference.V_ApplicabilityDate].value dump |", false, false);
            MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefApplicabilityDate + "'", false, false);

            boolean isDirect = false;

            for (String typeInList : baseTypesForMBOMLeafNodes) {
                if (getDerivedTypes(context, typeInList).contains(returnMBOMRefType))
                    isDirect = true;
            }

            if (!isDirect)
                throw new Exception("The Part (" + psRefPID + ") is already scoped with a Manufacturing Item (" + returnMBOMRefPID + ") which is not a Provide or a Create Material.");
        } else { // PS reference does not already have an MBOM scope
            // Get the previous revision of the PS reference
            // String previousRevPSRefPID = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select previous.physicalid dump |", false, false);

            String previousRevPSRefPID = "";
            // Bypass for data coming from publish (which does not conatine all sequance info on revision

            StringList objSelectable = new StringList(4);
            objSelectable.addElement(DomainConstants.SELECT_TYPE);
            objSelectable.addElement(DomainConstants.SELECT_REVISION);
            objSelectable.addElement(DomainConstants.SELECT_NAME);
            objSelectable.addElement("attribute[PLMReference.V_order]");

            DomainObject dObj = new DomainObject(psRefPID);
            Map objMap = dObj.getInfo(context, objSelectable);
            String sType = (String) objMap.get(DomainConstants.SELECT_TYPE);
            String sName = (String) objMap.get(DomainConstants.SELECT_NAME);
            String V_Order = (String) objMap.get("attribute[PLMReference.V_order]");
            // V_Order is not dependent of the revision sequence of the policy

            int vorder = Integer.parseInt(V_Order);
            if (vorder > 1) {
                StringList objSelects = new StringList(1);

                objSelects.addElement("physicalid");
                String sWhere = "attribute[PLMReference.V_order] ==" + Integer.toString((vorder - 1));
                MapList mlList = DomainObject.findObjects(context, sType, sName, "*", "*", "*", sWhere, true, objSelects);
                if (null != mlList) {
                    for (int n = 0; n < mlList.size(); n++) {
                        Map tempObj = (Map) mlList.get(n);
                        previousRevPSRefPID = (String) tempObj.get("physicalid");
                    }
                }
            }

            if ("".equals(previousRevPSRefPID)) { // PS reference does not have a previous revision
                if (mbomRefPID == null) { // MBOM reference is null

                    HashMap<String, String> attributes = new HashMap<String, String>();
                    returnMBOMRefPID = createMBOMReference(context, plmSession, "Provide", null, attributes);

                    newRefPIDList.add(returnMBOMRefPID);

                    // By default, the Title on the new Provide should be the same as the Title of the VPMReference linked to it
                    String psRefTitle = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value dump |", false, false);
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefTitle + "'", false, false);

                    // FRC: Fixed Bug#497-MBOM "Description" attribute not filled automatically
                    String psRefDescription = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMEntity.V_description].value dump |", false, false);
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_description '" + psRefDescription + "'", false, false);
                    String psRefApplicabilityDate = MqlUtil.mqlCommand(context, "print bus " + psRefPID + " select attribute[PLMReference.V_ApplicabilityDate].value dump |", false, false);
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMReference.V_ApplicabilityDate '" + psRefApplicabilityDate + "'", false, false);

                    // Create a scope link between PS reference and MBOM reference
                    FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                } else { // MBOM reference is not null
                    List<String> inputListForGetScope = new ArrayList<String>();
                    inputListForGetScope.add(mbomRefPID);
                    String psRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                    if (psRefScopePID != null && !"".equals(psRefScopePID)) { // MBOM reference has already a PS scope : throw a new exception
                        throw new Exception("This MBOM node already has a scope, and it is not the EBOM part you are providing !");
                    } else { // MBOM reference does not already have a scope
                        // Return the MBOM reference
                        returnMBOMRefPID = mbomRefPID;

                        // Create a scope link between PS reference and MBOM reference
                        FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);
                    }
                }
            } else { // PS reference has a previous revision
                // Recursive call on previous revision (with MBOM reference in parameter)
                String mbomRefPIDSynchedToPreviousPSRevision = getSynchedScopeMBOMRefFromPSRefForUpdateImplentLink(context, plmSession, previousRevPSRefPID, mbomRefPID, newRefPIDList);

                // New revision on the MBOM reference returned by the recursive call and return this new MBOM reference
                returnMBOMRefPID = newRevisionMBOMReference(context, plmSession, mbomRefPIDSynchedToPreviousPSRevision);
                newRefPIDList.add(returnMBOMRefPID);

                try {
                    plmSession.flushSession();
                } catch (Exception e) {
                }

                // !! CAREFULL : remove the scope on the new revision of the MBOM reference (by default, the new revision duplicates the scope)
                List<String> modifiedInstanceList = FRCMBOMModelerUtility.deleteScopeLinkAndDeleteChildImplementLinks(context, plmSession, returnMBOMRefPID, false);
                System.out.println("FRC : modified Instances : " + modifiedInstanceList);

                try {
                    plmSession.flushSession();
                } catch (Exception e) {
                }

                // Create a scope link between the PS reference and the new MBOM reference revision
                FRCMBOMModelerUtility.createScopeLinkAndReduceChildImplementLinks(context, plmSession, returnMBOMRefPID, psRefPID, false);

                // Map the attributes
                String psRefInfoStr = MqlUtil.mqlCommand(context,
                        "print bus " + psRefPID + " select attribute[PLMEntity.V_Name].value attribute[PLMEntity.V_description].value attribute[PLMReference.V_ApplicabilityDate].value dump |", false,
                        false);
                String[] psRefInfo = psRefInfoStr.split("\\|", -2);

                if ("".equals(psRefInfo[2]))
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefInfo[0] + "' PLMEntity.V_description '" + psRefInfo[1] + "'", false, false);
                else
                    MqlUtil.mqlCommand(context, "mod bus " + returnMBOMRefPID + " PLMEntity.V_Name '" + psRefInfo[0] + "' PLMEntity.V_description '" + psRefInfo[1]
                            + "' PLMReference.V_ApplicabilityDate '" + psRefInfo[2] + "'", false, false);
            }
        }

        return returnMBOMRefPID;
    }
}
