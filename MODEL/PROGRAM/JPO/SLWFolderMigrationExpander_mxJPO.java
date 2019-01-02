import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADAppletServletProtocol;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.Relationship;
import matrix.db.RelationshipWithSelect;
import matrix.db.RelationshipWithSelectList;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class SLWFolderMigrationExpander_mxJPO {
    private static final String SLW_GCO_TYPE = "SolidWorksInteg-GlobalConfig";

    private static final String SLW_GCO_NAME = "SWNewArch";

    private StringList busSelects = null;

    private StringList relSelects = null;

    private boolean isExpandAll = false;

    private Hashtable relNameSelectList = new Hashtable();

    private final String SELECT_EXPRESSION_ID = "id";

    private final String SELECT_EXPRESSION_FROM_CHILD_ID = "from.id";

    private final String SELECT_EXPRESSION_FROM_CHILD_TYPE = "from.type";

    private final String SELECT_EXPRESSION_TO_CHILD_ID = "to.id";

    private final String SELECT_EXPRESSION_TO_CHILD_TYPE = "to.type";

    // results
    private HashMap busIdRelidChildDataMap = new HashMap();

    private HashMap busidBusSelectsMap = new HashMap();

    private HashMap relidRelSelectsMap = new HashMap();

    // private String [] objectIds = null;
    private Hashtable relsAndEnds = null;

    private short expandLevel = 0;

    private boolean fetchVersions = false;

    private MCADServerResourceBundle serverResourceBundle = null;

    private IEFGlobalCache cache = null;

    private MCADMxUtil _mxUtil = null;

    private MCADGlobalConfigObject _gco = null;

    private MCADServerGeneralUtil _generalUtil = null;

    private static String RELATIONSHIP_ACTIVE_VERSION = "";

    private static String RELATIONSHIP_VERSIONOF = "";

    private static String RELATIONSHIP_INSTANCEOF = "";

    public SLWFolderMigrationExpander_mxJPO(Context context, String[] args) throws Exception {
        this.busSelects = getBusSelectList();

        serverResourceBundle = new MCADServerResourceBundle("en-US");
        cache = new IEFGlobalCache();
        _mxUtil = new MCADMxUtil(context, serverResourceBundle, cache);

        _gco = getGlobalConfigObject(context);// (MCADGlobalConfigObject)programMap.get("GCO");
        _generalUtil = new MCADServerGeneralUtil(context, _gco, serverResourceBundle, cache);

        Hashtable relClassMapTable = _gco.getRelationshipsOfClass(MCADAppletServletProtocol.ASSEMBLY_LIKE);
        Hashtable externalReferenceLikeRelsAndEnds = _gco.getRelationshipsOfClass(MCADServerSettings.CIRCULAR_EXTERNAL_REFERENCE_LIKE);

        Hashtable relclassTableClone = new Hashtable(relClassMapTable);

        Enumeration externalReferenceRels = externalReferenceLikeRelsAndEnds.keys();
        while (externalReferenceRels.hasMoreElements()) {
            String relName = (String) externalReferenceRels.nextElement();

            if (relclassTableClone.containsKey(relName))
                relclassTableClone.remove(relName);
        }

        this.relsAndEnds = relclassTableClone;

        this.relSelects = getRelSelectList(relclassTableClone);

        this.isExpandAll = true;

        // this.objectIds = args;

        RELATIONSHIP_ACTIVE_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
        RELATIONSHIP_VERSIONOF = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
        RELATIONSHIP_INSTANCEOF = MCADMxUtil.getActualNameForAEFData(context, "relationship_InstanceOf");
    }

    private StringList getRelSelectList(Hashtable relsAndEnds) {
        StringList relSelectionList = new StringList();

        relSelectionList.addElement("id");
        relSelectionList.addElement("name");

        return relSelectionList;
    }

    private StringList getBusSelectList() {
        StringList busSelectionList = new StringList();

        busSelectionList.add("type");
        busSelectionList.add("name");
        busSelectionList.add("to[" + RELATIONSHIP_INSTANCEOF + "].from.from[" + RELATIONSHIP_VERSIONOF + "].to.last.id"); // assumption: bulk-loaded data would use major in structure.
        busSelectionList.add("to[" + RELATIONSHIP_INSTANCEOF + "].from.last.id");
        busSelectionList.add("to[" + RELATIONSHIP_INSTANCEOF + "].from.from[" + RELATIONSHIP_VERSIONOF + "].to.last.attribute[Title]");
        busSelectionList.add("to[" + RELATIONSHIP_INSTANCEOF + "].from.last.attribute[Title]");

        return busSelectionList;
    }

    /*
     * public SLWFolderMigrationExpander(String [] objectIds, Hashtable relsAndEnds, StringList busSelects, StringList relSelects, short expandLevel, boolean fetchVersions) throws MatrixException {
     * this.busSelects = busSelects; this.relSelects = relSelects;
     * 
     * if(this.busSelects == null) this.busSelects = new StringList();
     * 
     * if(!this.busSelects.contains(this.SELECT_EXPRESSION_ID)) this.busSelects.add(this.SELECT_EXPRESSION_ID);
     * 
     * if(this.relSelects == null) this.relSelects = new StringList();
     * 
     * this.isExpandAll = (expandLevel <= 0);
     * 
     * this.objectIds = objectIds; this.relsAndEnds = relsAndEnds; this.expandLevel = expandLevel;
     * 
     * this.fetchVersions = fetchVersions; }
     */

    private StringList getBusSelectList(Hashtable relsAndEnds) {
        StringList busSelectionList = new StringList();

        Enumeration relList = relsAndEnds.keys();

        busSelectionList.add(this.SELECT_EXPRESSION_ID);
        busSelectionList.addAll(busSelects);

        if (relsAndEnds != null) {
            // For expanding
            while (relList.hasMoreElements()) {
                String relName = (String) relList.nextElement();

                if (!relNameSelectList.containsKey(relName)) {
                    String relEnd = (String) relsAndEnds.get(relName);
                    String expEnd = "";

                    if (relEnd.equals("to"))
                        expEnd = "from";
                    else
                        expEnd = "to";

                    String selectExpression = expEnd + "[" + relName + "].id"; // rel id
                    relNameSelectList.put(relName, selectExpression);

                    busSelectionList.add(selectExpression);
                } else
                    busSelectionList.add((String) relNameSelectList.get(relName));
            }
        }

        return busSelectionList;
    }

    private StringList getRelSelectList() {
        StringList relSelect = new StringList(6);

        relSelect.addElement(SELECT_EXPRESSION_ID);
        relSelect.addElement(SELECT_EXPRESSION_TO_CHILD_ID);
        relSelect.addElement(SELECT_EXPRESSION_FROM_CHILD_ID);

        relSelect.addAll(relSelects);

        return relSelect;
    }

    private HashSet getRelidsForSelect(Hashtable relsAndEnds, BusinessObjectWithSelectList busWithSelectionList) {
        HashSet relIdsForSelect = new HashSet(relsAndEnds.size());

        Enumeration relList = null;

        for (int i = 0; i < busWithSelectionList.size(); i++) {
            BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) busWithSelectionList.elementAt(i);

            relList = relsAndEnds.keys();

            while (relList.hasMoreElements()) {
                String relName = (String) relList.nextElement();

                String relSelectList = (String) relNameSelectList.get(relName);

                StringList relIdList = busObjectWithSelect.getSelectDataList(relSelectList); // rel id

                if (relIdList != null && relIdList.size() > 0)
                    relIdsForSelect.addAll(relIdList);
            }
        }

        return relIdsForSelect;
    }

    public void expandInputObject(Context context, String[] oids, Set<String> expandedIdsCache) throws MatrixException {
        busIdRelidChildDataMap = new HashMap();
        expandInputObjects(context, oids, expandedIdsCache);
    }

    public void expandInputObjects(Context context, String[] oids, Set expandedIdsCache) throws MatrixException {
        HashSet objectIdsForFurtherExpansion = new HashSet();

        StringList busSelectionList = getBusSelectList(relsAndEnds);

        BusinessObjectWithSelectList busWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, oids, busSelectionList);

        HashSet relIds = getRelidsForSelect(relsAndEnds, busWithSelectionList);

        if (relIds != null && relIds.size() > 0) {
            String[] relids = new String[relIds.size()];

            relIds.toArray(relids);

            StringList relSelectList = getRelSelectList();

            RelationshipWithSelectList relsWithSelectList = Relationship.getSelectRelationshipData(context, relids, relSelectList);

            for (int i = 0; i < relsWithSelectList.size(); i++) {
                RelationshipWithSelect relwithSelect = (RelationshipWithSelect) relsWithSelectList.elementAt(i);

                String relId = relwithSelect.getSelectData("id");
                relidRelSelectsMap.put(relId, relwithSelect);
            }
        }

        for (int i = 0; i < busWithSelectionList.size(); i++) {
            BusinessObjectWithSelect busWithSelect = busWithSelectionList.getElement(i);

            String busid = busWithSelect.getSelectData(this.SELECT_EXPRESSION_ID);

            Enumeration relList = relsAndEnds.keys();

            HashMap relidChildDataMap = new HashMap();

            busIdRelidChildDataMap.put(busid, relidChildDataMap);

            while (relList.hasMoreElements()) {
                String relName = (String) relList.nextElement();

                String relSelect = (String) relNameSelectList.get(relName);

                StringList relIdList = busWithSelect.getSelectDataList(relSelect); // rel id

                if (relIdList == null || relIdList.size() == 0)
                    continue;

                String relEnd = (String) relsAndEnds.get(relName);

                for (int k = 0; k < relIdList.size(); k++) {
                    String relId = (String) relIdList.elementAt(k);

                    RelationshipWithSelect relSelectsData = (RelationshipWithSelect) relidRelSelectsMap.get(relId);

                    String childObjectId = relSelectsData.getSelectData(SELECT_EXPRESSION_TO_CHILD_ID);

                    if (relEnd.equalsIgnoreCase("from")) {
                        childObjectId = relSelectsData.getSelectData(SELECT_EXPRESSION_FROM_CHILD_ID);
                    }

                    String[] childData = new String[2];
                    childData[0] = childObjectId;

                    relidChildDataMap.put(relId, childData);

                    if (!expandedIdsCache.contains(childObjectId)) {
                        childData[1] = "false";
                        objectIdsForFurtherExpansion.add(childObjectId);
                        expandedIdsCache.add(childObjectId);
                    } else {
                        childData[1] = "true";
                    }
                }
            }

            busidBusSelectsMap.put(busid, busWithSelect);
        }

        oids = new String[objectIdsForFurtherExpansion.size()];

        objectIdsForFurtherExpansion.toArray(oids);

        if (oids.length > 0)
            expandInputObjects(context, oids, expandedIdsCache);
    }

    public HashMap getRelidChildBusIdList(String busId) {
        return (HashMap) busIdRelidChildDataMap.get(busId);
    }

    public BusinessObjectWithSelect getBusinessObjectWithSelect(String busId) {

        return (BusinessObjectWithSelect) busidBusSelectsMap.get(busId);
    }

    public RelationshipWithSelect getRelationshipWithSelect(String relationshipId) {
        return (RelationshipWithSelect) relidRelSelectsMap.get(relationshipId);
    }

    public HashMap getBusIdRelidChildBusIdMap() {
        return busIdRelidChildDataMap;
    }

    private MCADGlobalConfigObject getGlobalConfigObject(Context context) throws Exception {
        if (_gco == null) {
            MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader();
            _gco = configLoader.createGlobalConfigObject(context, _mxUtil, SLW_GCO_TYPE, SLW_GCO_NAME);
        }

        return _gco;
    }
}
