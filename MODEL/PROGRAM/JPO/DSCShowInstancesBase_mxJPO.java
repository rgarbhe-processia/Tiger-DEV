
/*
 ** DSCShowInstancesBase
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to determine whether or not to show instances command in menu tree.
 */
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MatrixWriter;

import matrix.util.StringList;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADUtil;

public class DSCShowInstancesBase_mxJPO {
    String _sObjId;

    MatrixWriter _mxWriter = null;

    protected MCADServerResourceBundle serverResourceBundle = null;

    protected IEFGlobalCache cache = null;

    protected IEFIntegAccessUtil mxUtil = null;

    /**
     * The no-argument constructor.
     */
    public DSCShowInstancesBase_mxJPO() {
    }

    /**
     * Constructor which accepts the Matrix context and an array of String arguments.
     */
    public DSCShowInstancesBase_mxJPO(Context context, String[] args) throws Exception {
        _mxWriter = new MatrixWriter(context);
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public Boolean canShowInstances(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get("objectId");
        String language = (String) paramMap.get("languageStr");

        return isFamilylike(context, objectId, language);

    }

    private Boolean isFamilylike(Context context, String objectId, String language) throws Exception {
        serverResourceBundle = new MCADServerResourceBundle(language);
        cache = new IEFGlobalCache();
        mxUtil = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

        String integrationName = mxUtil.getIntegrationName(context, objectId);

        IEFSimpleConfigObject simpleGCO = null;

        if (mxUtil.getUnassignedIntegrations(context).contains(integrationName))
            simpleGCO = IEFSimpleConfigObject.getSimpleGCOForUnassginedInteg(context, integrationName);
        else
            simpleGCO = IEFSimpleConfigObject.getSimpleGCO(context, integrationName);

        if (simpleGCO != null) {
            Hashtable typeClassMapping = simpleGCO.getAttributeAsHashtable(MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-TypeClassMapping"), "\n", "|");
            String familyLikeCADTypes = (String) typeClassMapping.get("TYPE_FAMILY_LIKE");

            if (familyLikeCADTypes != null) {
                BusinessObject busObject = new BusinessObject(objectId);
                String cadType = mxUtil.getCADTypeForBO(context, busObject);

                Vector familyTypesList = MCADUtil.getVectorFromString(familyLikeCADTypes, ",");

                if (familyTypesList.contains(cadType))
                    return new Boolean(true);
                else
                    return new Boolean(false);
            } else {
                return new Boolean(false);
            }
        } else {
            return new Boolean(false);
        }
    }

    public Boolean canShowInstancesCombobox(Context context, String[] args) {
        boolean canShowInstanceList = false;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String originalObjectId = (String) paramMap.get("originalObjectId");
            String language = (String) paramMap.get("languageStr");
            if (originalObjectId == null || originalObjectId.equals(""))
                canShowInstanceList = false;
            else
                canShowInstanceList = isFamilylike(context, originalObjectId, language);
        } catch (Throwable e) {
        }
        return new Boolean(canShowInstanceList);
    }

    public HashMap getInstanceListForCombobox(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        StringList fieldRangeValues = new StringList();
        StringList fieldDisplayRangeValues = new StringList();

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String languageStr = (String) paramMap.get("languageStr");
        String objectId = (String) requestMap.get("objectId");
        String familyId = (String) requestMap.get("originalObjectId");

        serverResourceBundle = new MCADServerResourceBundle(languageStr);
        MCADGlobalConfigObject gco = (MCADGlobalConfigObject) requestMap.get("GCO");
        MCADServerGeneralUtil serverGeneralUtil = new MCADServerGeneralUtil(context, gco, serverResourceBundle, new IEFGlobalCache());
        // familyId = serverGeneralUtil.getValidObjectIdForNavigation(context,familyId);
        String attributeTitle = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Title") + "]";
        String strAciveInstanceRel = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveInstance");
        Hashtable relsAndEnds = gco.getRelationshipsOfClass(MCADServerSettings.FAMILY_LIKE);
        Enumeration allRels = relsAndEnds.keys();

        String selectInstanceId = null;
        String selectInstanceName = null;
        String selectInstanceTitle = null;

        StringList busSelectList = new StringList(11);
        while (allRels.hasMoreElements()) {
            String reln = (String) allRels.nextElement();
            String end = (String) relsAndEnds.get(reln);
            String baseSelect = "";
            if (end.equals("to"))
                end = "from";
            else
                end = "to";

            if (end.equals("from"))
                baseSelect = "from[" + reln + "].to.";
            else
                baseSelect = "to[" + reln + "].from.";

            selectInstanceId = baseSelect + "id";
            selectInstanceName = baseSelect + "name";
            selectInstanceTitle = baseSelect + attributeTitle;
        }
        String selectActiveInstanceId = "from[" + strAciveInstanceRel + "].to.id";
        busSelectList.add(selectInstanceId);
        if (gco.isObjectAndFileNameDifferent())
            busSelectList.add(selectInstanceTitle);
        else
            busSelectList.add(selectInstanceName);
        busSelectList.add(selectActiveInstanceId);

        String[] oids = new String[1];
        oids[0] = familyId;
        BusinessObjectWithSelectList busWithSelectList = BusinessObjectWithSelect.getSelectBusinessObjectData(context, oids, busSelectList);
        BusinessObjectWithSelect busWithSelect = busWithSelectList.getElement(0);

        StringList instanceIdList = (StringList) busWithSelect.getSelectDataList(selectInstanceId);
        StringList instanceNameList = (StringList) busWithSelect.getSelectDataList(selectInstanceName);
        StringList instanceTitleList = (StringList) busWithSelect.getSelectDataList(selectInstanceTitle);
        String activeInstanceId = (String) busWithSelect.getSelectData(selectActiveInstanceId);

        StringList labelList = instanceNameList;

        boolean isFamilyNameAppended = !gco.isUniqueInstanceNameInDBOn();
        if (gco.isObjectAndFileNameDifferent()) {
            labelList = instanceTitleList;
            isFamilyNameAppended = true;
        }
        String strLabelActive = i18nNow.getI18nString("emxIEFDesignCenter.Label.Active", "emxIEFDesignCenterStringResource", languageStr);
        MCADMxUtil mcadMxUtil = new MCADMxUtil(context, serverResourceBundle, new IEFGlobalCache());
        for (int i = 0; i < instanceIdList.size(); i++) {
            String instanceId = (String) instanceIdList.elementAt(i);
            String instanceMajorId = null;
            String label = MCADUtil.getIndivisualInstanceName((String) labelList.elementAt(i), isFamilyNameAppended);
            if (instanceId.equals(activeInstanceId))
                label = strLabelActive + ": " + label;
            if (!mcadMxUtil.isMajorObject(context, instanceId)) {
                BusinessObject majorObj = null;
                majorObj = mcadMxUtil.getMajorObject(context, new BusinessObject(instanceId));
                instanceMajorId = majorObj.getObjectId(context);
            } else {
                instanceMajorId = instanceId;
            }

            if (instanceMajorId.equals(objectId) || instanceId.equals(objectId)) {
                fieldRangeValues.add(0, instanceId);
                fieldDisplayRangeValues.add(0, label);
            } else {
                fieldRangeValues.add(instanceId);
                fieldDisplayRangeValues.add(label);
            }
        }

        returnMap.put("field_choices", fieldRangeValues);
        returnMap.put("field_display_choices", fieldDisplayRangeValues);
        return returnMap;
    }
}
