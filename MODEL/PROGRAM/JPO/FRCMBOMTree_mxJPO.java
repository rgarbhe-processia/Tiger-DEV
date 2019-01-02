import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.ds.DS3DExperienceBOM.II2GraphicalBrowserBase;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Context;
import matrix.util.StringList;

import com.mbom.modeler.utility.*;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class FRCMBOMTree_mxJPO extends II2GraphicalBrowserBase {
    private static final String ATT_VNAME = "attribute[PLMEntity.V_Name]";

    public FRCMBOMTree_mxJPO(Context context, String[] args) {
        super(context, args);
    }

    @Override
    protected MapList getStructure(Context context, Map<String, String[]> argsMap) throws Exception {

        String objectId = getRootId(context, getURLParameterString((String[]) argsMap.get("objectId")));

        String filterExpression = getURLParameterString((String[]) argsMap.get("filterExpr"));

        MapList res = FRCMBOMProg_mxJPO.getExpandMBOM(context, objectId, 0 /* expLvl */, filterExpression, null, null, getAllRelSelects(), getAllTypeSelects());

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

        PLMCoreModelerSession plmSession = null;
        try {
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            // Build Paths and save theses in the return maps

            HashMap<String, String> mapPaths = new HashMap<String, String>();// Store path in a Map to be able to manage unsorted return MapList

            String rootPID = (String) root.get("physicalid");
            mapPaths.put(rootPID, rootPID);

            List<String> inputListForGetScope = new ArrayList<String>();
            inputListForGetScope.add(rootPID);
            String rootProductRefPID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope).get(0);

            if (rootProductRefPID != null && !"".equals(rootProductRefPID)) {

                boolean intermediateScope = false;
                DomainObject domMBOM = new DomainObject(rootPID);
                for (String typeInList : FRCMBOMProg_mxJPO.baseTypesForMBOMAssemblyNodes) {
                    if (domMBOM.isKindOf(context, typeInList))
                        intermediateScope = true;
                }

                root.put("manufItemRefID", rootPID);
                root.put("scopeId", rootPID + "-" + rootProductRefPID);
                root.put("productRefPID", rootProductRefPID);
                root.put("intermediateScope", (intermediateScope ? "true" : "false"));
            }

            // Declare variable before to improve prefs
            Map<String, Object> mapObj;
            String objPID, objFromPID, objPIDConnection;
            String newPath = "";
            for (int i = 1; i < ml.size(); i++) {
                mapObj = (Map) ml.get(i);
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

                // Add Scope Infos
                String productRefScopePID;
                List<String> inputListForGetScope2 = new ArrayList<String>();
                inputListForGetScope2.add(objPID);
                productRefScopePID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope2).get(0);

                if (productRefScopePID != null)
                    mapObj.put("scopePID", productRefScopePID);

                // Add Implement Infos
                // ArrayList<String> productInstPIDList = null;
                if (objPIDConnection != null && !"".equals(objPIDConnection)) {
                    // productInstPIDList = ${CLASS:FRCMBOMProg}.getImplementPIDList(context, objPIDConnection);
                    // if(productInstPIDList!=null)mapObj.put("implementPIDs", productInstPIDList.toString());

                    List<String> listInstIDs = new ArrayList<String>();
                    listInstIDs.add(objPIDConnection);
                    List<Map<String, Object>> resImp = FRCMBOMModelerUtility.getImplementLinkInfoWithEffStatus(context, plmSession, listInstIDs, false);
                    Map<String, Object> linkInfo = resImp.get(0);

                    List<String> ilPathPIDList = (List<String>) linkInfo.get("PIDList");
                    List<String> ilPathLIDList = (List<String>) linkInfo.get("LIDList");

                    if (ilPathPIDList != null) {
                        StringBuilder sbPathPID = new StringBuilder();
                        StringBuilder sbPathLID = new StringBuilder();
                        StringBuilder sbEndPathPID = new StringBuilder();
                        StringBuilder sbEndPathLID = new StringBuilder();
                        String lastPID = "";
                        String lastLID = "";
                        for (int j = 0; j < ilPathPIDList.size(); j++) {
                            String physicalId = ilPathPIDList.get(j);
                            String logicalId = ilPathLIDList.get(j);

                            if (j > 0) {
                                sbPathPID.append("/");
                                sbPathLID.append("/");
                                sbEndPathPID.append("/");
                                sbEndPathLID.append("/");
                                sbEndPathPID.append(physicalId);
                                sbEndPathLID.append(logicalId);
                            }
                            sbPathPID.append(physicalId);
                            sbPathLID.append(logicalId);

                            lastPID = physicalId;
                            lastLID = logicalId;
                        }

                        mapObj.put("implPathPIDs", sbPathPID.toString());
                        mapObj.put("implPathLIDs", sbPathLID.toString());

                        mapObj.put("implEndPIDs", sbEndPathPID.toString());
                        mapObj.put("implEndLIDs", sbEndPathLID.toString());

                        mapObj.put("productInstPID", lastPID);
                        mapObj.put("productInstLID", lastLID);

                        mapObj.put("manufItemInstID", objPIDConnection);
                    }
                }

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

                // add data

                String manufItemRefID = (String) mapObj.get("physicalid");

                List<String> inputListForGetScope3 = new ArrayList<String>();
                inputListForGetScope3.add(manufItemRefID);
                String productRefPID = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, inputListForGetScope3).get(0);

                if (productRefPID != null && !"".equals(productRefPID)) {

                    boolean intermediateScope = false;
                    DomainObject domMBOM = new DomainObject(manufItemRefID);
                    for (String typeInList : FRCMBOMProg_mxJPO.baseTypesForMBOMAssemblyNodes) {
                        if (domMBOM.isKindOf(context, typeInList))
                            intermediateScope = true;
                    }

                    mapObj.put("manufItemRefID", manufItemRefID);
                    mapObj.put("scopeId", manufItemRefID + "-" + productRefPID);
                    mapObj.put("productRefPID", productRefPID);
                    mapObj.put("intermediateScope", (intermediateScope ? "true" : "false"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                plmSession.closeSession(true);
            } catch (Exception e) {
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.println("FRC PERFOS : modifyStrucureAfterExpand (MBOM) : " + (endTime - startTime));
    }

    @Override
    protected StringList getRelSelects() {
        StringList sels = new StringList();
        sels.addAll(FRCMBOMProg_mxJPO.EXPD_REL_SELECT);
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
        return "FRCMBOMTreeJS";
    }

    @Override
    protected String getDefaultBI() {
        return "Default";
    }

    @Override
    protected String getCustoCSS() {
        StringBuffer sb = new StringBuffer();
        sb.append(".rule-scopeBroken { border: 2px dotted #FB4B4B; }");// Scope broken
        sb.append(".rule-scopeOK { border: 2px solid #1B991B; }");

        sb.append(".rule-noScopeAndImplementKO { background-color: #FF6A6A; }");
        sb.append(".rule-noScopeAndImplementLID { background-color: #FFD16A; }");
        sb.append(".rule-noScopeAndImplementPID { background-color: #7FD868; }");

        sb.append(".rule-scopeOKAndImplementKO { border: 2px solid #1B991B; background-color: #FF6A6A; }");
        sb.append(".rule-scopeOKAndImplementLID { border: 2px solid #1B991B; background-color: #FFD16A; }");
        sb.append(".rule-scopeOKAndImplementPID { border: 2px solid #1B991B; background-color: #7FD868; }");

        sb.append(".rule-scopeBrokenAndImplementKO { border: 2px dotted #FB4B4B; background-color: #FF6A6A; }");
        sb.append(".rule-scopeBrokenAndImplementLID { border: 2px dotted #FB4B4B; background-color: #FFD16A; }");
        sb.append(".rule-scopeBrokenAndImplementPID { border: 2px dotted #FB4B4B; background-color: #7FD868; }");
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
