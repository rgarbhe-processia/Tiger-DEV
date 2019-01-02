import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

public class FRC_3DViewUtils_mxJPO {
    public static final String ATT_CONFEFF = "PLMInstance.V_hasConfigEffectivity";

    public static final String ATT_COMPFORM = "PLMInstance.V_EffectivityCompiledForm";

    private final static Logger LOG = Logger.getLogger(FRC_3DViewUtils_mxJPO.class);

    public FRC_3DViewUtils_mxJPO(Context context, String[] args) throws Exception {
        if (!context.isConnected())
            throw new Exception("not supported on desktop client");
    }

    public int mxMain(Context context, String[] args) throws Exception {
        if (!context.isConnected())
            throw new Exception("not supported on desktop client");
        return 0;
    }

    @SuppressWarnings("rawtypes")
    public static void buildInstancePathList(Context context, String curPathValue, String vpmInstId, String rootRefID, String keyToWrite, String splitToWrite, ArrayList<String> finalList)
            throws Exception {
        // (context, "", vpmInstId, rootRefID, keyToWrite, splitToWrite, instancePathList)
        StringList slRelVPM = new StringList(new String[] { "id", keyToWrite, "from.id", "from.attribute[SynchroEBOMExt.V_InEBOMUser]"// if false then it's a phantom Product
        });
        String[] ids = new String[1];
        ids[0] = vpmInstId;
        MapList mlRelsInfo = DomainRelationship.getInfo(context, ids, slRelVPM);
        Map mapRelInfos = (Map) mlRelsInfo.get(0);

        String vpmInstKey = (String) mapRelInfos.get(keyToWrite);

        String interValue = ("".equals(curPathValue)) ? vpmInstKey : (vpmInstKey + splitToWrite + curPathValue);// We are going up in the VPMInstance path because VPLMProject are pointing to the last
                                                                                                                // VPMInstance in case you use Phantom Product

        String fromObjId = (String) mapRelInfos.get("from.id");
        String fromObjInEbom = (String) mapRelInfos.get("from.attribute[SynchroEBOMExt.V_InEBOMUser]");

        if (!"false".equalsIgnoreCase(fromObjInEbom) && rootRefID.equals(fromObjId)) {
            // Parent Product is not a Phantom Product and is the Part Spec of the parent Part of the EBOM rel
            finalList.add(interValue);
            // System.out.println("finalList.add(interValue) Add:" + interValue);
        } else if ("false".equalsIgnoreCase(fromObjInEbom)) {
            // Parent Product is a Phantom Product
            DomainObject domParentPhantom = new DomainObject(fromObjId);
            StringList slParentVPMInst = domParentPhantom.getInfoList(context, "to[VPMInstance].id");

            if (slParentVPMInst == null)
                return;// It's a Phantom product with no parent Product

            for (int i = 0; i < slParentVPMInst.size(); i++) {
                String parentVPMInstId = (String) slParentVPMInst.get(i);
                buildInstancePathList(context, interValue, parentVPMInstId, rootRefID, keyToWrite, splitToWrite, finalList);
            }
        } // else : parent is not a Phantom Product and is not the Part Spec of the parent Part of the Ebom rel
    }

    @SuppressWarnings("rawtypes")
    public static ArrayList<String> getVPLMInstancesForEBOMRel(Context context, String ebomRelID, String keyToWrite, String splitToWrite) throws Exception {

        ArrayList<String> instancePathList = new ArrayList<String>();

        StringList slRelEBOM = new StringList(new String[] { "id", "from.from[Part Specification].to.id", "frommid[VPLMInteg-VPLMProjection].torel[VPMInstance].id" });
        String[] ids = new String[1];
        ids[0] = ebomRelID;
        MapList mlRelsInfo = DomainRelationship.getInfo(context, ids, slRelEBOM);
        Map mapRelInfos = (Map) mlRelsInfo.get(0);

        // Get Part Spec Id
        Object objRootRefId = mapRelInfos.get("from.from[Part Specification].to.id");
        String rootRefID = "";
        if (objRootRefId instanceof StringList) {
            rootRefID = (String) ((StringList) objRootRefId).get(0);
            System.err.println("Warning more than one Part Specification found on parent Object, the first one is taken : Maybe not the right one !");
        } else {
            rootRefID = (String) objRootRefId;
        }
        if ("".equals(rootRefID))
            throw new Exception("Could not find Part Specification on parent Object");

        // Get List of VPMInstance Ids
        StringList slVPMInst = null;
        Object objVPMInstKey = mapRelInfos.get("frommid[VPLMInteg-VPLMProjection].torel[VPMInstance].id");
        if (objVPMInstKey == null)
            throw new Exception("Could not find");
        if (objVPMInstKey instanceof StringList) {
            slVPMInst = (StringList) objVPMInstKey;
        } else {
            slVPMInst = new StringList(new String[] { ((String) objVPMInstKey) });
        }

        // Loop through the VPMInstance list
        for (int i = 0; i < slVPMInst.size(); i++) {
            String vpmInstId = (String) slVPMInst.get(i);
            buildInstancePathList(context, "", vpmInstId, rootRefID, keyToWrite, splitToWrite, instancePathList);
        }

        return instancePathList;
    }

    public static ArrayList<String> convertEBOMPathToVPLMPath(Context context, String ebomPath, String keyToWrite, String splitToWrite) {
        ArrayList<String> res = new ArrayList<String>();

        String[] arrPath = ebomPath.split(splitToWrite);
        try {
            DomainObject domRoot = new DomainObject(arrPath[0]);

            String vplmRefRoot = domRoot.getInfo(context, "from[Part Specification].to." + keyToWrite);
            res.add(vplmRefRoot);

            for (int i = 1; i < arrPath.length; i++) {
                String ebomRel = arrPath[i];
                // System.out.println("ebomRel=" + ebomRel);
                try {
                    ArrayList<String> listInstances = getVPLMInstancesForEBOMRel(context, ebomRel, keyToWrite, splitToWrite);
                    ArrayList<String> newRes = new ArrayList<String>();
                    // System.out.println("listInstances.size()=" + listInstances.size());
                    // System.out.println("listInstances=" + listInstances.toString());
                    for (int j = 0; j < res.size(); j++) {
                        String currPath = res.get(j);
                        for (int k = 0; k < listInstances.size(); k++) {
                            String addPath = listInstances.get(k);
                            newRes.add(currPath + splitToWrite + addPath);
                        }
                    }
                    res = newRes;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return res;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getVPMRefsFromEBOMPathFromJSP(Context context, String[] args) throws Exception {

        LOG.info("getVPMRefsFromEBOMPathFromJSP Start");
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String ebomPath = (String) programMap.get("ebomPath");

        LOG.info("getVPMRefsFromEBOMPathFromJSP ebomPath : " + ebomPath);

        MapList resultML = new MapList();
        ArrayList<String> arrVPLMPaths = convertEBOMPathToVPLMPath(context, ebomPath, "physicalid", "/");

        LOG.info("convertEBOMPathToVPLMPath returns : " + arrVPLMPaths.toString());

        StringList instSelect = new StringList();
        instSelect.add("physicalid");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_1].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_2].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_3].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_4].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_5].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_6].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_7].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_8].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_9].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_10].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_11].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_12].value");
        // instSelect.add("from.physicalid");
        instSelect.add("to.physicalid");

        // 1 - For each instance id : put a map with type = VPMInstance, position = posAttributes, id= physicalid
        // 2 - For each end instances do an expand to get all instances and VPMRefs under it + Operations of 1st Step + Map for VPMRef with the full path
        for (int i = 0; i < arrVPLMPaths.size(); i++) {
            String[] arrPathIds = arrVPLMPaths.get(i).split("/");
            // at 0 it's the Root Object
            String[] idsVPMInstances = Arrays.copyOfRange(arrPathIds, 1, arrPathIds.length);
            MapList mlRelsInfo = DomainRelationship.getInfo(context, idsVPMInstances, instSelect);

            /*
             * for(int j=0; j<mlRelsInfo.size(); j++){ Map mapInst=(Map)mlRelsInfo.get(j); mapInst.put("typeObj", "VPMInstance");//Specific type for JS script to load 3D resultML.add(mapInst); }
             */
            resultML.addAll(mlRelsInfo);

            // Last Instance map
            Map mapLastInst = (Map) mlRelsInfo.get(mlRelsInfo.size() - 1);
            String idRef = (String) mapLastInst.get("to.physicalid");

            DomainObject domVPMRef = new DomainObject(idRef);
            StringList typeSelect = new StringList();
            typeSelect.add("from[VPMRepInstance]");// If true we have a rep
            MapList mlSubStructure = domVPMRef.getRelatedObjects(context, "VPMInstance", "VPMReference", typeSelect, instSelect, false, true, (short) 0, "", "", 0);
            LOG.info("mlSubStructure : " + mlSubStructure.toString());
            String currentPath = arrVPLMPaths.get(i);

            // Before exploring the substructure, add the object info itself in case it's a leaf with some rep
            Map mapVPMRefInfo = domVPMRef.getInfo(context, typeSelect);
            String hasVPMRepInfo = (String) mapVPMRefInfo.get("from[VPMRepInstance]");
            if (hasVPMRepInfo.equalsIgnoreCase("TRUE")) {
                mapVPMRefInfo.put("pathVPLM", currentPath);
                mapLastInst.putAll(mapVPMRefInfo);
            }

            // Now explore the sub Structure
            int lastLevel = 0;
            for (int j = 0; j < mlSubStructure.size(); j++) {
                Map mapVPMRef = (Map) mlSubStructure.get(j);
                String strLevel = (String) mapVPMRef.get("level");
                int currLevel = Integer.parseInt(strLevel);
                String instId = (String) mapVPMRef.get("physicalid");
                // Update current Path
                if (currLevel > lastLevel) {// Should increment only by 1
                    currentPath += "/" + instId;
                    lastLevel = currLevel;
                } else {
                    int nRemove = (lastLevel - currLevel) + 1;
                    for (int k = 0; k < nRemove; k++) {
                        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
                    }
                    currentPath += "/" + instId;
                    lastLevel = currLevel;
                }
                String hasVPMRep = (String) mapVPMRef.get("from[VPMRepInstance]");
                if (hasVPMRep.equalsIgnoreCase("TRUE")) {
                    mapVPMRef.put("pathVPLM", currentPath);
                }
            }
            resultML.addAll(mlSubStructure);
        }

        LOG.info("resultML : " + resultML.toString());
        LOG.info("getVPMRefsFromEBOMPathFromJSP End");
        return resultML;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getVPMRefsFromVPMPathFromJSP(Context context, String[] args) throws Exception {
        LOG.info("getVPMRefsFromEBOMPathFromJSP Start");
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String ebomPath = (String) programMap.get("ebomPath");
        String filterCompiledExpression = (String) programMap.get("filterCompiledExpression");

        LOG.info("getVPMRefsFromEBOMPathFromJSP ebomPath : " + ebomPath);

        MapList resultML = new MapList();
        // ArrayList<String> arrVPLMPaths=convertEBOMPathToVPLMPath(context, ebomPath, "physicalid", "/");
        // ArrayList<String> arrVPLMPaths=new ArrayList<String>(Arrays.asList(ebomPath.split("/")));
        ArrayList<String> arrVPLMPaths = new ArrayList<String>();
        arrVPLMPaths.add(ebomPath);

        LOG.info("convertEBOMPathToVPLMPath returns : " + arrVPLMPaths.toString());

        StringList instSelect = new StringList();
        instSelect.add("physicalid");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_1].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_2].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_3].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_4].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_5].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_6].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_7].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_8].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_9].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_10].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_11].value");
        instSelect.add("attribute[LPAbstractInstance.V_matrix_12].value");
        // instSelect.add("from.physicalid");
        instSelect.add("to.physicalid");

        // 1 - For each instance id : put a map with type = VPMInstance, position = posAttributes, id= physicalid
        // 2 - For each end instances do an expand to get all instances and VPMRefs under it + Operations of 1st Step + Map for VPMRef with the full path
        for (int i = 0; i < arrVPLMPaths.size(); i++) {
            String[] arrPathIds = arrVPLMPaths.get(i).split("/");
            // at 0 it's the Root Object
            String[] idsVPMInstances = Arrays.copyOfRange(arrPathIds, 1, arrPathIds.length);
            MapList mlRelsInfo = DomainRelationship.getInfo(context, idsVPMInstances, instSelect);

            /*
             * for(int j=0; j<mlRelsInfo.size(); j++){ Map mapInst=(Map)mlRelsInfo.get(j); mapInst.put("typeObj", "VPMInstance");//Specific type for JS script to load 3D resultML.add(mapInst); }
             */
            resultML.addAll(mlRelsInfo);

            // Last Instance map
            String idRef = null;
            Map mapLastInst = new HashMap();
            if (mlRelsInfo.size() > 0) {
                mapLastInst = (Map) mlRelsInfo.get(mlRelsInfo.size() - 1);
                idRef = (String) mapLastInst.get("to.physicalid");
            } else { // Case where only a root is given in the path
                idRef = arrPathIds[0];
            }

            // If there is a filter, initate it
            String filterId = null;
            String whereClauseRel = "";
            if (filterCompiledExpression != null && !"".equals(filterCompiledExpression)) {
                filterId = MqlUtil.mqlCommand(context, false, "exec prog ConfigFiltering_InitFilter " + filterCompiledExpression, false);
                whereClauseRel = "escape  ( IF (!(attribute[" + ATT_CONFEFF + "] == TRUE)) THEN 1 ELSE (execute[ConfigFiltering_ApplyFilter " + filterId + "  attribute\\[" + ATT_COMPFORM + "\\] ]) )";
            }

            DomainObject domVPMRef = new DomainObject(idRef);
            StringList typeSelect = new StringList();
            typeSelect.add("from[VPMRepInstance]");// If true we have a rep
            MapList mlSubStructure = domVPMRef.getRelatedObjects(context, "VPMInstance", "VPMReference", typeSelect, instSelect, false, true, (short) 0, "", whereClauseRel, 0);
            LOG.info("mlSubStructure : " + mlSubStructure.toString());
            String currentPath = arrVPLMPaths.get(i);

            // Before exploring the substructure, add the object info itself in case it's a leaf with some rep
            Map mapVPMRefInfo = domVPMRef.getInfo(context, typeSelect);
            String hasVPMRepInfo = (String) mapVPMRefInfo.get("from[VPMRepInstance]");
            if (hasVPMRepInfo.equalsIgnoreCase("TRUE")) {
                mapVPMRefInfo.put("pathVPLM", currentPath);
                mapLastInst.putAll(mapVPMRefInfo);
            }
            // Now explore the sub Structure
            int lastLevel = 0;
            for (int j = 0; j < mlSubStructure.size(); j++) {
                Map mapVPMRef = (Map) mlSubStructure.get(j);
                String strLevel = (String) mapVPMRef.get("level");
                int currLevel = Integer.parseInt(strLevel);
                String instId = (String) mapVPMRef.get("physicalid");
                // Update current Path
                if (currLevel > lastLevel) {// Should increment only by 1
                    currentPath += "/" + instId;
                    lastLevel = currLevel;
                } else {
                    int nRemove = (lastLevel - currLevel) + 1;
                    for (int k = 0; k < nRemove; k++) {
                        currentPath = currentPath.substring(0, currentPath.lastIndexOf("/"));
                    }
                    currentPath += "/" + instId;
                    lastLevel = currLevel;
                }
                String hasVPMRep = (String) mapVPMRef.get("from[VPMRepInstance]");
                if (hasVPMRep.equalsIgnoreCase("TRUE")) {
                    mapVPMRef.put("pathVPLM", currentPath);
                }
            }
            resultML.addAll(mlSubStructure);

            // If there is a filter, release it
            if (filterId != null && !"".equals(filterId)) {
                MqlUtil.mqlCommand(context, false, "exec prog ConfigFiltering_ReleaseFilter " + filterId, false);
            }

        }

        LOG.info("resultML : " + resultML.toString());
        LOG.info("getVPMRefsFromEBOMPathFromJSP End");
        return resultML;
    }

    @SuppressWarnings("rawtypes")
    public ArrayList<String> getVPLMPathsForEBOMPath(Context context, String[] args) throws Exception {
        LOG.info("getVPLMPathsForEBOMPath Start");
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String ebomPath = (String) programMap.get("ebomPath");
        ArrayList<String> arrVPLMPaths = convertEBOMPathToVPLMPath(context, ebomPath, "physicalid", "/");
        LOG.info("getVPLMPathsForEBOMPath END");
        return arrVPLMPaths;
    }
}
