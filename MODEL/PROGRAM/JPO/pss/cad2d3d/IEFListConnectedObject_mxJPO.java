/**
 * IEFListConnectedObject.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries,
 * Copyright notice is precautionary only and does not evidence any actual or intended publication of such program
 */

package pss.cad2d3d;

import java.util.HashMap;
import java.util.StringTokenizer;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFEBOMConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.framework.ui.UIUtil;

public class IEFListConnectedObject_mxJPO {
    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IEFListConnectedObject_mxJPO.class);
    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END

    private MCADServerResourceBundle resourceBundle = null;

    private IEFGlobalCache cache = null;

    private MCADGlobalConfigObject gco = null;

    public IEFListConnectedObject_mxJPO(Context context, String[] args) throws Exception {
    }

    // list all connected objects in to direction for a relationship name
    public Object getToList(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        paramMap.put("toFrom", "to");
        return getList(context, JPO.packArgs(paramMap));
    }

    // list all connected objects in from direction for a relationship name
    public Object getFromList(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        paramMap.put("toFrom", "from");
        return getList(context, JPO.packArgs(paramMap));
    }

    // list connected objects with a relationship name and direction
    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getList(Context context, String[] args) throws Exception {
        MapList maplist = new MapList();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String functionalPageName = (String) paramMap.get("funcPageName");
        String languageStr = (String) paramMap.get("languageStr");
        gco = (MCADGlobalConfigObject) paramMap.get("GCO");
        this.resourceBundle = new MCADServerResourceBundle(languageStr);
        this.cache = new IEFGlobalCache();
        MCADMxUtil util = new MCADMxUtil(context, resourceBundle, cache);

        BusinessObject busObj = new BusinessObject(objectId);
        busObj.open(context);
        // String busType = busObj.getTypeName();
        busObj.close(context);

        // name of the filter to apply .
        String filterName = (String) paramMap.get("WorkSpaceFilter");
        String toFrom = (String) paramMap.get("toFrom");
        // relationship name
        String relName = (String) paramMap.get("relationshipName");
        String excludeRelNames = (String) paramMap.get("excludeRelNames");
        // String relWhere = "";
        StringBuffer bufRelWhere = new StringBuffer();

        boolean isValidObjIdRequired = true;
        if (gco != null) {
            String sEBOMRegistryTNR = gco.getEBOMRegistryTNR();
            StringTokenizer sEBOMRegistryTNRDetails = new StringTokenizer(sEBOMRegistryTNR, "|");
            if (sEBOMRegistryTNRDetails.countTokens() >= 3) {
                // String sEBOMRConfigObjType = (String) sEBOMRegistryTNRDetails.nextElement();
                // String sEBOMRConfigObjName = (String) sEBOMRegistryTNRDetails.nextElement();
                // String sEBOMRConfigObjRev = (String) sEBOMRegistryTNRDetails.nextElement();
            }

            // [NDM] OP6
            if (relName != null && !relName.equalsIgnoreCase("null") && relName.trim().equalsIgnoreCase(MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification"))
                    && util.isMajorObject(context, objectId) && gco.isCreateVersionObjectsEnabled()) {
                isValidObjIdRequired = false;
            }
        }

        if (isValidObjIdRequired && !"RelatedItems".equalsIgnoreCase(functionalPageName)) {
            if (objectId != null && !"".equals(objectId) && gco != null) {
                MCADServerGeneralUtil genUtil = new MCADServerGeneralUtil(context, gco, resourceBundle, cache);
                objectId = genUtil.getValidObjctIdForEBOMSynch(context, objectId);
            }
        }

        if (excludeRelNames != null && !excludeRelNames.equals("null") && !excludeRelNames.equals("")) {
            // relWhere += " where '";
            bufRelWhere.append(" where '");
            // String exp = "";
            StringBuffer expBuffer = new StringBuffer();
            StringTokenizer tk = new StringTokenizer(excludeRelNames, "|");
            while (tk.hasMoreTokens()) {
                String token = tk.nextToken();
                if (token == null || token.length() <= 0)
                    continue;
                // if (exp.length() > 0)
                // exp += "&&";
                if (expBuffer.length() > 0)
                    expBuffer.append("&&");

                // exp += " (type!=\"" + token + "\") ";
                expBuffer.append(" (type!=\"" + token + "\") ");
            }
            // relWhere += exp + "'";
            bufRelWhere.append(expBuffer.toString());
            bufRelWhere.append("'");
        }

        if (toFrom == null || toFrom.equals("") || toFrom.equals("null") || toFrom.equals("*"))
            toFrom = "";
        if (relName == null || relName.equals("") || relName.equals("null") || relName.equals("*") || relName.equals("All"))
            relName = "";
        else {
            // SGS modification starts
            // Purpose: enforce to get custom rel list
            // relName = " Relationship '" + relName+",PSS_CharteredDrawing'"; Commented OOTB code
            String strChartedDraing = MCADMxUtil.getActualNameForAEFData(context, "relationship_PSS_ChartedDrawing");
            relName = "  Relationship '" + relName + "," + strChartedDraing + "'";

            // SGS modification ends
        }
        String filter = "";

        // For role-based filters - Start
        String sRoleName = "";
        if (filterName != null && !filterName.equals("null") && !filterName.equals("") && filterName.indexOf("::") != -1) {
            sRoleName = filterName.substring(0, filterName.indexOf("::"));
            filterName = filterName.substring(filterName.indexOf("::") + 2);
        }
        // For role-based filters - End

        if (filterName != null && !filterName.equals("null") && !filterName.equals(""))
            filter = "  filter  \"" + filterName + "\"  ";

        // create the filter Object . extact all the attributes of the filter
        // create a where clause and add the clause while quering for connected
        // objects .

        try {
            // query to get all connected objects after a filter applied on them .
            String query = "";
            if (!("".equalsIgnoreCase(sRoleName))) {
                // Apply role-based filter
                query = "set workspace user '" + sRoleName + "'; expand  bus " + objectId + "  " + toFrom + relName + filter + "  terse  select  rel id  " + bufRelWhere.toString()
                        + " select bus type name revision dump |  recordsep \n; ";
            } else {
                // Apply user-based filter
                query = "expand  bus " + objectId + "  " + toFrom + relName + filter + "  terse  select  rel id  " + bufRelWhere.toString() + " select bus type name revision dump |  recordsep \n; ";
            }
            // MQLCommand mql = new MQLCommand();
            // boolean ret = mql.executeCommand(context, query);
            String strResult = MqlUtil.mqlCommand(context, query, false, false);
            if (UIUtil.isNotNullAndNotEmpty(strResult)) {
                StringTokenizer tokenizer = new StringTokenizer(strResult, "\n");
                int tokenCount = tokenizer.countTokens();
                for (int i = 0; i < tokenCount; i++) {
                    String token = tokenizer.nextToken();
                    String busid = "";
                    String relid = "";
                    String type = "";
                    String name = "";
                    String revision = "";
                    String rel_name = "";
                    String rel_end = "";
                    int index = 0;
                    int index1 = token.indexOf("|");
                    while (token.indexOf("|") > -1) {
                        String value = "";
                        index1 = token.indexOf("|");
                        if (index1 > 0) {
                            value = token.substring(0, index1);
                            if (index1 + 1 < token.length())
                                token = token.substring(index1 + 1);
                        } else {
                            value = "";
                            if (token.length() > 1)
                                token = token.substring(1);
                            else
                                token = "";
                        }
                        if (index == 1)
                            rel_name = value;
                        else if (index == 2)
                            rel_end = value;
                        else if (index == 3)
                            busid = value;
                        else if (index == 4)
                            type = value;
                        else if (index == 5)
                            name = value;
                        else if (index == 6)
                            revision = value;
                        index++;
                    }
                    relid = token;

                    HashMap map = new HashMap();
                    // get busid and relid
                    map.put("id", busid);
                    map.put("id[connection]", relid);
                    map.put("type", type);
                    map.put("name", name);
                    map.put("rel_name", rel_name);
                    map.put("rel_type", rel_name);
                    map.put("rel_end", rel_end);
                    map.put("revision", revision);
                    maplist.add(map);
                }
            }
            // mql.close(context);
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getList: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: END
        catch (Exception ex) {
            maplist = new MapList();
        }

        return maplist;
    }
}
