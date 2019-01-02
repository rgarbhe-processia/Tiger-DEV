
/*
 ** DSCObjectListBase
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Class defining basic infrastructure, contains common data members required for executing any IEF related actions.
 */

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionWithSelect;
import matrix.db.JPO;
import matrix.db.RelationshipWithSelect;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADConfigObjectLoader;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

public class DSCObjectListBase_mxJPO {
    protected HashMap integrationNameGCOTable = null;

    protected MCADServerResourceBundle serverResourceBundle = null;

    protected IEFIntegAccessUtil util = null;

    protected MCADServerGeneralUtil _generalUtil = null;

    protected String localeLanguage = null;

    protected MCADGlobalConfigObject _globalConfig = null;

    protected IEFGlobalCache cache = null;

    public String MOVE_FILES_TO_VERSION = "";

    public String IS_VERSION_OBJECT = "";

    public DSCObjectListBase_mxJPO() {
    }

    public DSCObjectListBase_mxJPO(Context context, String[] args) throws Exception {
        if (!context.isConnected()) {
            MCADServerException.createException("not supported no desktop client", null);
        }
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public MapList getList(Context context, String[] args) throws Exception {
        MapList objectList = new MapList();

        try {
            StringList busSelects = new StringList();

            busSelects.add(DomainConstants.SELECT_ID);
            String typePart = MCADMxUtil.getActualNameForAEFData(context, "type_Part");
            MapList mpList = DomainObject.findObjects(context, typePart, "*", "", busSelects);
            Iterator it = mpList.iterator();

            while (it.hasNext()) {
                Map map = (Map) it.next();
                objectList.add(map);
            }

        } catch (Exception e) {
            System.out.println(e.toString());
            MCADServerException.createException(e.getMessage(), e);
        }

        return objectList;
    }

    public MapList getRelatedObjectList(Context context, String[] args) throws Exception {
        return (getRelatedObjectList(context, (HashMap) JPO.unpackArgs(args)));
    }

    public MapList getRelatedObjectList(Context context, HashMap paramMap) throws Exception {
        MapList objectList = new MapList();

        try {
            MapList relBusObjPageList = (MapList) paramMap.get("objectList");
            short nLevel = 1;

            localeLanguage = (String) paramMap.get("languageStr");
            serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
            cache = new IEFGlobalCache();
            util = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

            String relName = (String) paramMap.get("relName");
            String isTo = (String) paramMap.get("to");
            String isFrom = (String) paramMap.get("from");
            String level = (String) paramMap.get("level");

            boolean getTo = false;
            boolean getFrom = true;

            if (isTo != null && isTo.equalsIgnoreCase("true")) {
                getTo = true;
            } else {
                getTo = false;
            }

            if (isFrom != null && isFrom.equalsIgnoreCase("true")) {
                getFrom = true;
            } else {
                getFrom = false;
            }
            if (level != null && level.length() > 0) {
                nLevel = Short.parseShort(level);
            }

            for (int i = 0; i < relBusObjPageList.size(); i++) {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                String objectId = (String) objDetails.get("id");
                if (objectId == null || objectId.trim().equals(""))
                    continue;

                StringList relSelects = new StringList();
                StringList objSelects = new StringList();
                objSelects.add("revision.last");

                String rel = relName;
                String type = "*";

                BusinessObject bus = new BusinessObject(objectId);
                bus.open(context);

                ExpansionWithSelect expansionWithSelect = util.expandSelectBusObject(context, bus, rel, type, objSelects, relSelects, getTo, getFrom, (short) nLevel, (short) 0, false);
                Enumeration relationsList = expansionWithSelect.getRelationships().elements();
                String relObjectId = "";

                while (relationsList.hasMoreElements()) {
                    RelationshipWithSelect select = (RelationshipWithSelect) relationsList.nextElement();
                    select.open(context);

                    short nNodeLevel = select.getLevel();

                    if (getTo) {
                        relObjectId = select.getFrom().getObjectId();
                        HashMap objMap = new HashMap();

                        objMap.put("id", relObjectId);
                        objMap.put("level", new Short(nNodeLevel).toString());

                        objectList.add(objMap);
                    }
                    if (getFrom) {
                        relObjectId = select.getTo().getObjectId();
                        HashMap objMap = new HashMap();

                        objMap.put("id", relObjectId);
                        objMap.put("level", new Short(nNodeLevel).toString());

                        objectList.add(objMap);
                    }

                    select.close(context);
                }
                bus.close(context);
            }
        } catch (Exception e) {
            System.out.println(e.toString());
            MCADServerException.createException(e.getMessage(), e);
        }

        return objectList;
    }

    protected String getIEFBoId(Context context, String _busObjectID, boolean isActiveMandatory) throws MCADException {
        String retObjectId = _busObjectID;

        try {
            BusinessObject busObj = new BusinessObject(_busObjectID);
            busObj.open(context);
            // [NDM] OP6
            // String type = busObj.getTypeName();
            /*
             * if (util.isMajorObject(context, _busObjectID))//_globalConfig.isMajorType(type)) { if (!_generalUtil.isBusObjectFinalized(context, busObj)) { if(isActiveMandatory) retObjectId =
             * util.getActiveVersionObject(context, _busObjectID); } } else if (_generalUtil.isBusObjectFinalized(context, busObj) && !isActiveMandatory) //minor object & is finalized { busObj =
             * util.getMajorObject(context, busObj); busObj.open(context); retObjectId = busObj.getObjectId(); }
             */
            // [NDM] H68: check only ion the basis of isActiveMandatory and isMajorObject
            if (util.isMajorObject(context, _busObjectID) && isActiveMandatory)
                retObjectId = util.getActiveVersionObject(context, _busObjectID);
            else if (!isActiveMandatory) {
                busObj = util.getMajorObject(context, busObj);
                busObj.open(context);
                retObjectId = busObj.getObjectId();
            }

            busObj.close(context);
        } catch (Exception me) {
            MCADServerException.createException(me.getMessage(), me);
        }
        return retObjectId;
    }

    protected MCADGlobalConfigObject getGlobalConfigObject(Context context, String integrationName, MCADMxUtil mxUtil, boolean isUnassignedInteg) {
        MCADGlobalConfigObject gcoObject = null;

        try {
            IEFSimpleConfigObject simpleConfig = IEFSimpleConfigObject.getSimpleLCO(context);

            if (!simpleConfig.isObjectExists() || isUnassignedInteg)
                simpleConfig = IEFSimpleConfigObject.getSimpleUnassignedIntegRegistry(context);

            String gcoType = MCADMxUtil.getActualNameForAEFData(context, "type_MCADInteg-GlobalConfig");
            String attrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");

            Hashtable integNameGCOMapping = simpleConfig.getAttributeAsHashtable(attrName, "\n", "|");

            if (integNameGCOMapping.containsKey(integrationName)) {
                String gcoName = (String) integNameGCOMapping.get(integrationName);

                MCADConfigObjectLoader configLoader = new MCADConfigObjectLoader(null);
                gcoObject = configLoader.createGlobalConfigObject(context, mxUtil, gcoType, gcoName);
            }
        } catch (Exception e) {

        }

        return gcoObject;
    }

    protected boolean isVersionObject(HashMap objectMap) {
        String result = (String) objectMap.get(IS_VERSION_OBJECT);
        if (result == null || result.length() < 0)
            return false;
        return result.equalsIgnoreCase("true");
    }

    protected boolean fileInMinor(HashMap objectMap) {
        String result = (String) objectMap.get(MOVE_FILES_TO_VERSION);
        if (result == null || result.length() < 0)
            return false;
        return result.equalsIgnoreCase("true");
    }

    public String getViewerValidObjectId(Context context, String objectId, boolean bNavigateToViewable, boolean bNavigateToLatest) throws Exception {
        DSC_CommonUtil_mxJPO jpoUtil = new DSC_CommonUtil_mxJPO();
        BusinessObject busObj = new BusinessObject(objectId);
        busObj.open(context);

        String activeVersionId = "";

        if (bNavigateToLatest == false) {
            activeVersionId = util.getActiveVersionObject(context, objectId);
        } else {
            activeVersionId = util.getLatestMinorID(context, busObj);
        }
        if (activeVersionId == null || activeVersionId.length() == 0)
            return objectId;

        IS_VERSION_OBJECT = MCADMxUtil.getActualNameForAEFData(context, "attribute_IsVersionObject");
        MOVE_FILES_TO_VERSION = MCADMxUtil.getActualNameForAEFData(context, "attribute_MoveFilesToVersion");

        HashMap results = jpoUtil.getCommonDocumentAttributes(context, objectId);
        boolean bIsVersionObject = isVersionObject(results);
        boolean bFileInMinor = false;
        if (bNavigateToViewable) {
            bFileInMinor = true;
        } else {
            bFileInMinor = fileInMinor(results);
        }

        String retObjectId = "";
        if (bFileInMinor) {
            // standard CDM
            if (bIsVersionObject == false) {
                // Navigate to the Active Version
                retObjectId = activeVersionId;
            } else {
                retObjectId = objectId;
            }
        } else {
            // IEF
            retObjectId = objectId;
        }

        busObj.close(context);
        return retObjectId;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getRelatedViewables(Context context, String[] args) throws Exception {
        return (getRelatedViewablesAndDerivedOutputs(context, (HashMap) JPO.unpackArgs(args)));
    }

    public MapList getRelatedViewablesAndDerivedOutputs(Context context, HashMap paramMap) throws Exception {
        MapList retObjectList = new MapList();
        MapList viewablesList = null;
        MapList c3DViewablesList = null; // [IR-293179/IR-329092]:XLV
        MapList derivedOutputsList = null;
        try {
            String objectId = (String) paramMap.get("objectId");

            localeLanguage = (String) paramMap.get("languageStr");
            serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
            cache = new IEFGlobalCache();
            util = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

            String relViewable = MCADMxUtil.getActualNameForAEFData(context, "relationship_Viewable");
            String relC3DViewable = MCADMxUtil.getActualNameForAEFData(context, "relationship_C3DViewable"); // [IR-293179/IR-329092]:XLV
            HashMap newParamMap = new HashMap();
            newParamMap.put("relName", relViewable);
            newParamMap.put("to", "false");
            newParamMap.put("from", "true");
            String integrationName = util.getIntegrationName(context, objectId);
            MapList objectList = new MapList();

            if (integrationName != null && !integrationName.equals("")) {
                // Related Markups bugs
                _globalConfig = (MCADGlobalConfigObject) paramMap.get("GCO");

                integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

                if (integrationNameGCOTable != null) {
                    _globalConfig = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);
                }

                if (_globalConfig == null) {
                    _globalConfig = getGlobalConfigObject(context, integrationName, util, util.getUnassignedIntegrations(context).contains(integrationName));
                }

                if (null == cache) {
                    cache = new IEFGlobalCache();
                }

            }

            if (_globalConfig != null) {
                _generalUtil = new MCADServerGeneralUtil(context, _globalConfig, serverResourceBundle, cache);
                // isActiveMandatory is false,viewable can be connected to Major or Minor depending upon object is finalized or not.
                String validObjectID = getIEFBoId(context, objectId, true);
                HashMap objDetails = new HashMap();

                objDetails.put("id", validObjectID);
                objectList.add(objDetails);
            } else {
                HashMap objDetails = new HashMap();

                objDetails.put("id", objectId);
                objectList.add(objDetails);
            }

            newParamMap.put("objectList", objectList);

            if (null != paramMap && paramMap.containsKey("relDerivedOutput")) {
                // Get DerivedOutputs and Viewables
                String relDerivedOutput = (String) paramMap.get("relDerivedOutput");
                newParamMap.put("relName", relDerivedOutput);
                derivedOutputsList = getRelatedObjectList(context, newParamMap);
                newParamMap.put("relName", relViewable);
                viewablesList = getRelatedObjectList(context, newParamMap);
                // [IR-293179/IR-329092]:XLV:START
                newParamMap.put("relName", relC3DViewable);
                c3DViewablesList = getRelatedObjectList(context, newParamMap);
                if (c3DViewablesList != null && !c3DViewablesList.isEmpty()) {
                    viewablesList.addAll(c3DViewablesList);
                }
                // [IR-293179/IR-329092]:XLV:END
                HashMap viewableAndDerivedOutputMap = new HashMap();
                viewableAndDerivedOutputMap.put("derivedOutputsList", derivedOutputsList);
                viewableAndDerivedOutputMap.put("viewablesList", viewablesList);
                retObjectList.add(viewableAndDerivedOutputMap);
            } else {
                // [IR-293179/IR-329092]:XLV:START
                // Commented Old code below:
                // Get Viewables only
                // retObjectList = getRelatedObjectList(context, newParamMap);

                // New Code:
                newParamMap.put("relName", relViewable);
                viewablesList = getRelatedObjectList(context, newParamMap);

                newParamMap.put("relName", relC3DViewable);
                c3DViewablesList = getRelatedObjectList(context, newParamMap);
                if (c3DViewablesList != null && !c3DViewablesList.isEmpty()) {
                    viewablesList.addAll(c3DViewablesList);
                }
                HashMap viewableMap = new HashMap();
                viewableMap.put("viewablesList", viewablesList);
                retObjectList.add(viewableMap);
                // [IR-293179/IR-329092]:XLV:END
            }
        } catch (Exception e) {
            MCADServerException.createException(e.getMessage(), e);
        }
        return retObjectList;
    }

    public MapList getRelatedParts(Context context, String[] args) throws Exception {
        return (getRelatedParts(context, (HashMap) JPO.unpackArgs(args)));
    }

    public MapList getRelatedParts(Context context, HashMap paramMap) throws Exception {
        MapList retObjectList = null;
        try {
            String objectId = (String) paramMap.get("objectId");

            localeLanguage = (String) paramMap.get("languageStr");
            serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
            cache = new IEFGlobalCache();
            util = new IEFIntegAccessUtil(context, serverResourceBundle, cache);
            String relPartSpecification = MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification");
            HashMap newParamMap = new HashMap();
            newParamMap.put("relName", relPartSpecification);
            newParamMap.put("to", "true");
            newParamMap.put("from", "false");

            String integrationName = util.getIntegrationName(context, objectId);

            MapList objectList = new MapList();

            if (integrationName != null) {
                // Related Parts bugs
                _globalConfig = (MCADGlobalConfigObject) paramMap.get("GCO");
                integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

                if (integrationNameGCOTable != null) {
                    _globalConfig = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);
                }

                if (null == cache) {
                    cache = new IEFGlobalCache();
                }
                _generalUtil = new MCADServerGeneralUtil(context, _globalConfig, serverResourceBundle, cache);
                String validObjectID = getIEFBoId(context, objectId, false);
                if (validObjectID != null) {
                    HashMap objDetails = new HashMap();

                    objDetails.put("id", validObjectID);
                    objectList.add(objDetails);
                }

            }
            newParamMap.put("objectList", objectList);
            retObjectList = getRelatedObjectList(context, newParamMap);
        } catch (Exception e) {
            System.out.println(e.toString());
            MCADServerException.createException(e.getMessage(), e);
        }
        return retObjectList;
    }

    public Hashtable getAllRelatedMarkups(Context context, String[] args) throws Exception {
        return (getAllRelatedMarkups(context, (HashMap) JPO.unpackArgs(args)));
    }

    public Hashtable getAllRelatedMarkups(Context context, HashMap paramMap) throws Exception {
        Hashtable markupsTable = new Hashtable();

        String[] objectIds = (String[]) paramMap.get("objectIds");
        localeLanguage = (String) paramMap.get("languageStr");
        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        HashMap newParamMap = new HashMap();
        MapList markups = new MapList();
        for (int i = 0; i < objectIds.length; i++) {
            String objectId = objectIds[i];

            newParamMap.put("objectId", objectId);
            newParamMap.put("includeViewable", "false");
            newParamMap.put("languageStr", localeLanguage);
            newParamMap.put("GCOTable", integrationNameGCOTable);

            markups = getRelatedMarkups(context, newParamMap);

            markupsTable.put(objectId, markups);
        }
        return markupsTable;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getRelatedMarkups(Context context, String[] args) throws Exception {
        return (getRelatedMarkups(context, (HashMap) JPO.unpackArgs(args)));
    }

    public MapList getRelatedMarkups(Context context, HashMap paramMap) throws Exception {
        MapList retObjectList = new MapList();
        MapList viewablesList = null;
        MapList derivedOutputsList = null;
        try {
            String objectId = (String) paramMap.get("objectId");
            StringList selectables = new StringList();
            selectables.add(DomainObject.SELECT_ID);
            selectables.add("physicalid");

            DomainObject doObj = DomainObject.newInstance(context, objectId);
            Map objInfo = doObj.getInfo(context, selectables);

            String sPhyId = (String) objInfo.get("physicalid");
            if (sPhyId.equals(objectId)) {
                objectId = (String) objInfo.get(DomainObject.SELECT_ID);
            }
            String attrCADType = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");
            String relDerivedOutput = MCADMxUtil.getActualNameForAEFData(context, "relationship_DerivedOutput");
            paramMap.put("relDerivedOutput", relDerivedOutput);

            MapList viewableAndDerivedOutList = new MapList();
            viewableAndDerivedOutList = getRelatedViewablesAndDerivedOutputs(context, paramMap);
            paramMap.remove("relDerivedOutput");

            ArrayList markupObjList = null;
            ArrayList markupListArray = new ArrayList();

            for (int i = 0; i < viewableAndDerivedOutList.size(); i++) {
                Map objMap = (Map) viewableAndDerivedOutList.get(i);
                viewablesList = (MapList) objMap.get("viewablesList");
                if (null != viewablesList && !viewablesList.isEmpty()) {
                    // Get Markups of Viewables
                    markupObjList = getRelatedMarkups(context, viewablesList, objectId, false);// for getting C3dViewable related object bFlag should be false
                    markupListArray.add(markupObjList);
                }
            }

            if (util.hasAttributeForBO(context, objectId, attrCADType)) {
                // case2: related mark up for "CGRViewable Object" or "thumbnailViewable object",
                // where markup is directly connected to those objects.
                // This is to differentiate between jVueViewable and DSC Viewable type(CgrViewable, ThumbnailViewable)
                // jVueViewable does not have "CADType" attribute
                String cadType = util.getAttributeForBO(context, objectId, attrCADType);
                if (!cadType.equals("")) {
                    HashMap objDetailsMap = new HashMap();
                    objDetailsMap.put("id", objectId);
                    MapList objectList = new MapList();
                    objectList.add(objDetailsMap);
                    markupObjList = getRelatedMarkups(context, objectList, objectId, true);// for getting CGRViewable ThumbnailViewable related object bFlag should be true
                    markupListArray.add(markupObjList);
                }
            }
            // store all the Markup objects to the return list
            for (int i = 0; i < markupListArray.size(); i++) {
                ArrayList objList = (ArrayList) markupListArray.get(i);
                for (int j = 0; j < objList.size(); j++) {
                    MapList list = (MapList) objList.get(j);
                    for (int k = 0; k < list.size(); k++) {
                        Map obj = (Map) list.get(k);
                        String markupId = (String) obj.get("id");
                        HashMap objDetails = new HashMap();
                        objDetails.put("id", markupId);
                        retObjectList.add(objDetails);
                    }
                }
            }
        } catch (Exception e) {
            MCADServerException.createException(e.getMessage(), e);
        }
        return retObjectList;
    }

    private ArrayList getRelatedMarkups(Context context, MapList viewablesList, String objectId, boolean bFlag) throws Exception {
        ArrayList markupArray = new ArrayList();
        String relMarkup = MCADMxUtil.getActualNameForAEFData(context, "relationship_Markup");
        String attrCADType = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");

        for (int i = 0; i < viewablesList.size(); i++) {
            Map obj = (Map) viewablesList.get(i);
            String viewableId = (String) obj.get("id");

            HashMap objDetails = new HashMap();
            MapList objectList = new MapList();
            HashMap markupParamMap = new HashMap();
            // for getting C3dViewable related object bFlag should be false
            // for getting CGRViewable ThumbnailViewable related object bFlag should be true
            if (bFlag || !util.hasAttributeForBO(context, viewableId, attrCADType)) {
                objDetails.put("id", viewableId);
                objectList.add(objDetails);
                markupParamMap.put("objectList", objectList);
                markupParamMap.put("relName", relMarkup);
                markupParamMap.put("to", "false");
                markupParamMap.put("from", "true");

                MapList markupList = getRelatedObjectList(context, markupParamMap);
                markupArray.add(markupList);
            }
        }
        return markupArray;
    }
}
