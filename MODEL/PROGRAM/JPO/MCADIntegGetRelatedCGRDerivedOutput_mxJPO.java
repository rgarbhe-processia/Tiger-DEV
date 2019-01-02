
/*
 ** MCADIntegGetRelatedCGRDerivedOutput
 **
 ** Copyright Dassault Systemes, 1992-2008. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to get the related CGRViewbale Objects connected with ViewableOf relationship to a "CAT Part" or "CATIA v4 Model" business object.
 */
import java.util.*;
import java.util.Set;

import matrix.db.*;
import matrix.util.*;

import com.matrixone.MCADIntegration.server.*;
import com.matrixone.MCADIntegration.server.beans.*;
import com.matrixone.MCADIntegration.server.cache.*;

/*
 ** import com.matrixone.apps.domain.util.* is added for type MapList. Applicable only for V6R2010 onwards
 */
import com.matrixone.apps.domain.util.*;

import com.matrixone.MCADIntegration.utils.*;
import com.matrixone.MCADIntegration.utils.xml.*;

public class MCADIntegGetRelatedCGRDerivedOutput_mxJPO {
    protected Context _context = null;

    protected MCADGlobalConfigObject _globalConfig = null;

    protected MCADMxUtil _util = null;

    protected MCADServerGeneralUtil _generalUtil = null;

    protected MCADServerResourceBundle _serverResourceBundle = null;

    protected IEFGlobalCache _cache = null;

    protected DSCExpandObjectWithSelect _objectExpander = null;

    protected boolean isSequenceAvailable = false;

    protected Vector revisionSequenceList = null;

    public MCADIntegGetRelatedCGRDerivedOutput_mxJPO() {
    }

    public MCADIntegGetRelatedCGRDerivedOutput_mxJPO(Context context, String[] args) throws Exception {
        if (!context.isConnected())
            MCADServerException.createException("Not supported on desktop client!!!", null);

        Hashtable argsTable = (Hashtable) JPO.unpackArgs(args);
        String sLanguage = (String) argsTable.get("language");

        init(context, argsTable, sLanguage);
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    protected void init(Context context, Hashtable argsTable, String sLanguage) throws Exception {
        _context = context;
        _serverResourceBundle = new MCADServerResourceBundle(sLanguage);
        _cache = new IEFGlobalCache();
        _util = new MCADMxUtil(context, _serverResourceBundle, _cache);
        _globalConfig = (MCADGlobalConfigObject) argsTable.get("GCO");
        _generalUtil = new MCADServerGeneralUtil(context, _globalConfig, _serverResourceBundle, _cache);
        _objectExpander = new DSCExpandObjectWithSelect(context, _generalUtil, _util, _globalConfig);
    }

    /**
     * This function contains the implementation for showing the vertical view. The function is passed a business object ID as argument. The vertical view is applied on the object of this busID, to
     * show it's dependent objects on the checkout page. The implementation should be designed to use this business object ID and expand it, and create a structure which contains different nodes with
     * corresponding relationship IDs and busIDs. This XML structure should be returned by the function. It is then used by IEF to show the related objects in the vertical view on the checkout page.
     * @param context
     *            The user context
     * @param args
     *            A string array of arguments used. The first element of the array MUST be the busID, the others are optional, depending on the implementation.
     * @return a IEFXmlNode which contains nodes strcuture to be shown in checkout page. XML structure should be as shown below. <viewdetails> <node busid="" relid=""> <node busid="" relid=""/>
     *         </node> <node busid="" relid=""/> </viewdetails>
     */

    public IEFXmlNode getVerticalViewBOIDs(Context context, String[] args) throws Exception {

        String busId = args[0];
        String expandedObjectIds = args[1];
        String expandDrawing = args[2];
        Hashtable requestTable = new Hashtable(1);
        StringTokenizer objIDs = new StringTokenizer(expandedObjectIds, ",");
        HashSet objIDSet = new HashSet();

        while (objIDs.hasMoreTokens()) {
            objIDSet.add((String) objIDs.nextToken().trim());
        }

        requestTable.put(busId, objIDSet);

        Vector argsVector = new Vector(2);
        argsVector.addElement(requestTable);
        argsVector.addElement(expandDrawing);

        String[] requestInfo = JPO.packArgs(argsVector);

        Hashtable resultTable = getVerticalViewBOIDsForObjectIds(context, requestInfo);
        return (IEFXmlNode) resultTable.get(busId);
    }

    public Hashtable getVerticalViewBOIDsForObjectIds(Context context, String[] args) throws Exception {

        Vector arguments = (Vector) JPO.unpackArgs(args);
        Hashtable requestTable = (Hashtable) arguments.elementAt(0);
        String expandDrawing = (String) arguments.elementAt(1);

        Hashtable returnTable = new Hashtable(requestTable.size());
        try {
            Hashtable relsAndEnds = _globalConfig.getRelationshipsOfClass("all");
            relsAndEnds.put("Viewable", "to");

            // commenting this line as this is NOT required -- HUM
            // relsAndEnds = changeEndsForChild(relsAndEnds);

            Hashtable objectIdExpLevelMap = getObjectIdExpLevelMap(requestTable.keySet());

            /*
             * * V6R2010 onwards getRelationshipAndChildObjectInfoForParent uses 9 arguments* Added last 2 arguments new MapList() , new HashMap()
             */
            Hashtable busIdDependentRelIdTable = _objectExpander.getRelationshipAndChildObjectInfoForParent(context, objectIdExpLevelMap, relsAndEnds, new HashMap(), new Hashtable(), false,
                    MCADAppletServletProtocol.VIEW_AS_BUILT, new MapList(), new HashMap());

            Enumeration busids = requestTable.keys();

            while (busids.hasMoreElements()) {
                String busId = (String) busids.nextElement();
                Hashtable relIdParentDetails = (Hashtable) busIdDependentRelIdTable.get(busId);
                HashSet expandedObjectIds = (HashSet) requestTable.get(busId);

                returnTable.put(busId, getVerticalViewBOIDs(context, busId, (HashSet) expandedObjectIds.clone(), expandDrawing, relIdParentDetails));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnTable;
    }

    private IEFXmlNode getVerticalViewBOIDs(Context context, String busId, HashSet expandedObjectIds, String expandDrawing, Hashtable relIdParentDetails) throws Exception {
        IEFXmlNode viewDetailsNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
        viewDetailsNode.setName("viewdetails");
        BusinessObject busObject = new BusinessObject(busId);
        busObject.open(_context);
        String sObjCADType = busObject.getTypeName();
        busObject.close(_context);
        if (sObjCADType.equals("CATPart") == false && sObjCADType.equals("CATIA V4 Model") == false) {
            return viewDetailsNode;
        }

        try {
            Vector relNameList = new Vector();
            relNameList.addElement(_util.getActualNameForAEFData(_context, "relationship_Viewable"));

            Hashtable busNameRevisionSequenceMap = new Hashtable();
            Hashtable busNameLatestRevisionMap = new Hashtable();
            Hashtable busNameRelationshipIDMap = new Hashtable();
            Hashtable relationshipIDBusIDMap = new Hashtable();
            Hashtable alreadyExpandedItems = new Hashtable();

            Enumeration relIds = relIdParentDetails.keys();
            while (relIds.hasMoreElements()) {

                String relId = (String) relIds.nextElement();

                Vector indvObjectDetails = (Vector) relIdParentDetails.get(relId);

                String connectedObjId = (String) indvObjectDetails.elementAt(0);
                String relName = (String) indvObjectDetails.elementAt(1);

                if (relNameList.contains(relName)) {
                    relationshipIDBusIDMap.put(relId, connectedObjId);

                    BusinessObject connectedBusObj = new BusinessObject(connectedObjId);
                    connectedBusObj.open(_context);

                    String connectedObjName = connectedBusObj.getName();
                    String currentRevision = connectedBusObj.getRevision();
                    String sCADType = connectedBusObj.getTypeName();

                    if (sCADType.equals("CgrViewable") == false) {
                        continue;
                    }
                    if (expandedObjectIds.contains(connectedObjId)) {
                        alreadyExpandedItems.put(connectedObjName, relId);
                    }

                    if (!busNameLatestRevisionMap.containsKey(connectedObjName)) {
                        busNameLatestRevisionMap.put(connectedObjName, currentRevision);
                        busNameRelationshipIDMap.put(connectedObjName, relId);
                    } else {
                        Vector revisionSequence = (Vector) busNameRevisionSequenceMap.get(connectedObjName);
                        if (revisionSequence == null) {
                            revisionSequence = generateRevisionSequenceData(connectedObjId);
                            busNameRevisionSequenceMap.put(connectedObjName, revisionSequence);
                        }

                        String latestRevision = (String) busNameLatestRevisionMap.get(connectedObjName);
                        if (revisionSequence.indexOf(currentRevision) > revisionSequence.indexOf(latestRevision)) {
                            busNameLatestRevisionMap.put(connectedObjName, currentRevision);
                            busNameRelationshipIDMap.put(connectedObjName, relId);
                        }
                    }
                }
            }

            Enumeration expandedIDsElements = alreadyExpandedItems.keys();
            while (expandedIDsElements.hasMoreElements()) {
                String expandedObjectName = (String) expandedIDsElements.nextElement();
                String relatID = (String) alreadyExpandedItems.get(expandedObjectName);
                busNameRelationshipIDMap.put(expandedObjectName, relatID);

            }

            // Add selected node to the already expanded nodes list to avoid repetition when drawing object is expanded
            if (!expandedObjectIds.contains(busId))
                expandedObjectIds.add(busId);

            Enumeration relationshipIDsElements = busNameRelationshipIDMap.elements();
            while (relationshipIDsElements.hasMoreElements()) {

                String relationshipID = (String) relationshipIDsElements.nextElement();
                String busID = (String) relationshipIDBusIDMap.get(relationshipID);

                if (!expandedObjectIds.contains(busID)) {
                    IEFXmlNode viewNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                    viewNode.setName("node");

                    Hashtable attributesTable = new Hashtable();
                    attributesTable.put("busid", busID);
                    attributesTable.put("relid", relationshipID);
                    viewNode.setAttributes(attributesTable);

                    viewDetailsNode.addNode(viewNode);

                    // Get dependent nodes for drawing object
                    if (expandDrawing.equals("true"))
                        addChildNodes(busID, viewNode, expandedObjectIds);
                }
            }
        } catch (Exception e) {
            MCADServerException.createException(e.getMessage(), e);
        }

        return viewDetailsNode;
    }

    protected Vector generateRevisionSequenceData(String busID) {

        Vector sortedRevisionsList = new Vector();

        try {
            BusinessObject busObject = new BusinessObject(busID);
            busObject.open(_context);

            BusinessObject connectedMajorBusObject = null;
            if (_globalConfig.isMajorType(busObject.getTypeName())) {
                connectedMajorBusObject = busObject;
            } else {
                connectedMajorBusObject = _util.getMajorObject(_context, busObject);
                connectedMajorBusObject.open(_context);
            }

            BusinessObjectList majorBusObjectsList = connectedMajorBusObject.getRevisions(_context);
            BusinessObjectItr majorBusObjectsItr = new BusinessObjectItr(majorBusObjectsList);
            while (majorBusObjectsItr.next()) {
                BusinessObject majorBusObject = majorBusObjectsItr.obj();
                majorBusObject.open(_context);

                String majorRevision = majorBusObject.getRevision();

                BusinessObjectList minorBusObjectsList = _util.getMinorObjects(_context, majorBusObject);
                if (minorBusObjectsList.size() > 0) {
                    BusinessObject connectedMinorBusObject = minorBusObjectsList.getElement(0);

                    minorBusObjectsList = connectedMinorBusObject.getRevisions(_context);

                    BusinessObjectItr minorBusObjectsItr = new BusinessObjectItr(minorBusObjectsList);
                    while (minorBusObjectsItr.next()) {
                        BusinessObject minorBusObject = minorBusObjectsItr.obj();
                        minorBusObject.open(_context);
                        String minorRevision = minorBusObject.getRevision();
                        minorBusObject.close(_context);

                        sortedRevisionsList.addElement(minorRevision);
                    }
                }

                boolean isFinalized = _generalUtil.isBusObjectFinalized(_context, majorBusObject);
                if (isFinalized)
                    sortedRevisionsList.addElement(majorRevision);

                majorBusObject.close(_context);
            }
        } catch (Exception ex) {

        }

        return sortedRevisionsList;
    }

    protected void addChildNodes(String parentObjectId, IEFXmlNode parentNode, HashSet alreadyExpandedObjectIDs) throws MCADException {
        Hashtable relsAndEnds = _globalConfig.getRelationshipsOfClass("all");
        relsAndEnds.put("Viewable", "to");

        String queryResult = _generalUtil.getFilteredFirstLevelChildAndRelIds(_context, parentObjectId, true, relsAndEnds, null, true, null);

        StringTokenizer childObjectsTokens = new StringTokenizer(queryResult, "\n");
        while (childObjectsTokens.hasMoreTokens()) {
            String childObjectDetails = childObjectsTokens.nextToken();

            StringTokenizer childObjectElements = new StringTokenizer(childObjectDetails, "|");

            String level = childObjectElements.nextToken();
            String relName = childObjectElements.nextToken();
            String direction = childObjectElements.nextToken();
            String objectId = childObjectElements.nextToken();
            String relId = childObjectElements.nextToken();

            if (!alreadyExpandedObjectIDs.contains(objectId)) {
                IEFXmlNode viewNode = new IEFXmlNodeImpl(IEFXmlNodeImpl.TAG);
                viewNode.setName("node");

                Hashtable attributesTable = new Hashtable();
                attributesTable.put("busid", objectId);
                attributesTable.put("relid", relId);
                viewNode.setAttributes(attributesTable);

                parentNode.addNode(viewNode);
            }
        }
    }

    private Hashtable changeEndsForChild(Hashtable relAndEnds) {

        Hashtable returnTable = new Hashtable(relAndEnds.size());

        Enumeration relNames = relAndEnds.keys();

        while (relNames.hasMoreElements()) {
            String relName = (String) relNames.nextElement();
            String relEnd = (String) relAndEnds.get(relName);

            if (relEnd.equalsIgnoreCase("from"))
                relEnd = "to";
            else
                relEnd = "from";

            returnTable.put(relName, relEnd);
        }

        return returnTable;
    }

    private Hashtable getObjectIdExpLevelMap(Set objectIds) {

        Hashtable returnTable = new Hashtable(objectIds.size());

        Iterator objectIdItr = objectIds.iterator();

        while (objectIdItr.hasNext()) {
            String objectId = (String) objectIdItr.next();
            returnTable.put(objectId, "1");
        }

        return returnTable;
    }
}
