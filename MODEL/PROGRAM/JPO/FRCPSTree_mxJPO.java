import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.ds.DS3DExperienceBOM.II2GraphicalBrowserBase;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.Context;
import matrix.util.StringList;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class FRCPSTree_mxJPO extends II2GraphicalBrowserBase {

    private static final String ATT_VNAME = "attribute[PLMEntity.V_Name]";

    private static final String ATT_EXTID = "attribute[PLMInstance.PLM_ExternalID].value";

    public FRCPSTree_mxJPO(Context context, String[] args) {
        super(context, args);
    }

    @Override
    protected String getRootId(Context context, String objectId) throws Exception {
        String returnStr = "";

        PLMCoreModelerSession plmSession = null;
        try {
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            DomainObject domRoot = new DomainObject(objectId);
            if (domRoot.isKindOf(context, "VPMReference"))
                returnStr = objectId;
            else {
                List<String> inputListForGetScope = new ArrayList<String>();
                inputListForGetScope.add(objectId);
                returnStr = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

                // KYB Start - Fixed Bug#238-MBOM Graphical view is not accessible
                if (returnStr != null && "".equals(returnStr)) {
                    returnStr = objectId;
                }
                // KYB End - Fixed Bug#238-MBOM Graphical view is not accessible
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                plmSession.closeSession(true);
            } catch (Exception e) {
            }
        }

        return returnStr;
    }

    @Override
    protected MapList getStructure(Context context, Map<String, String[]> argsMap) throws Exception {

        String objectId = getRootId(context, getURLParameterString((String[]) argsMap.get("objectId")));

        String filterExpression = getURLParameterString((String[]) argsMap.get("filterExpr"));

        MapList res = FRCMBOMProg_mxJPO.getExpandPS(context, objectId, (short) 0 /* expLvl */, filterExpression, null, null, getAllRelSelects(), getAllTypeSelects());

        return res;
    }

    @Override
    protected void modifyStrucureAfterExpand(Context context, MapList ml) {
        long startTime = System.currentTimeMillis();
        if (ml.isEmpty())
            return;

        // check if first element is the Root
        Map root = (Map) ml.get(0);
        if (root == null || root.get("id[connection]") != null) {
            System.out.println("MBOMTree - Error : there is no root !!");
            return; // this is not the root
        }

        // Build Paths and save theses in the return maps + add "hasChildren=false" where needed

        // Store path in a Map to be able to manage unsorted return MapList
        HashMap<String, String> mapPaths = new HashMap<String, String>();
        HashMap<String, String> mapPathsLogical = new HashMap<String, String>();

        String rootPID = (String) root.get("physicalid");
        String rootLID = (String) root.get("logicalid");
        mapPaths.put(rootPID, rootPID);
        mapPathsLogical.put(rootPID, rootLID);
        root.put("pathPID", rootPID);
        root.put("pathLID", rootLID);

        // Declare variable before to improve prefs
        Map<String, Object> mapObj;
        String objPID, objFromPID, objPIDConnection, objLID, objLIDConnection;
        String newPath = "";
        String newPathLogical = "";

        for (int i = 1; i < ml.size(); i++) {
            mapObj = (Map) ml.get(i);
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

            // Add hasChildren info here
            if ((i + 1) < ml.size()) {
                Map nextMapObj = (Map) ml.get(i + 1);
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

            // add/rename BI attributes
            mapObj.put("instPID", objPIDConnection);
            mapObj.put("instName", mapObj.get(ATT_EXTID));
            mapObj.remove(ATT_EXTID);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("FRC PERFOS : modifyStrucureAfterExpand (PS) : " + (endTime - startTime));
    }

    @Override
    protected StringList getRelSelects() {
        StringList sels = new StringList();
        sels.addAll(FRCMBOMProg_mxJPO.EXPD_REL_SELECT);
        sels.add(ATT_EXTID);
        return sels;
    }

    @Override
    protected StringList getTypeSelects() {
        StringList sels = new StringList();
        sels.addAll(FRCMBOMProg_mxJPO.EXPD_BUS_SELECT);
        sels.add(ATT_VNAME);
        return sels;
    }

    @Override
    protected String getMiddleContent(Context context, Map<String, Object> obj) {
        return String.valueOf(obj.get(ATT_VNAME));
    }

    @Override
    protected String getCustoJSPageName() {
        return "FRCPSTreeJS";
    }

    @Override
    protected String getDefaultBI() {
        return "Default";
    }

    @Override
    protected String getCustoCSS() {
        StringBuffer sb = new StringBuffer();
        sb.append(".rule-scope      { border: 2px solid #1B991B; }");
        sb.append(".rule-scopeAndNoImplement { border: 2px solid #1B991B; background-color: #FFCA54; }");
        sb.append(".rule-noImplement { background-color: #FFCA54; }");
        sb.append(".rule-implementDirect { background-color: #6DBBFD; }");
        sb.append(".rule-scopeAndImplementDirect { border: 2px solid #1B991B; background-color: #7FD868; }");
        sb.append(".rule-implementIndirect { background-color: #2A689C; }");
        return sb.toString();
    }

    @Override
    protected boolean autoDisplayAdvancedFilters() {
        return false;
    }

    @Override
    protected String getRelationshipPattern() {
        return null; // Not Needed : getStrucutre
    }

    @Override
    protected String getTypePattern() {
        return null; // Not Needed : getStrucutre
    }

    @Override
    protected boolean debug() {
        return false;
    }
}
