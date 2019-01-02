
/*
 ** DECGetAllFinalizedRevisions
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 */

import java.util.HashMap;
import java.util.StringTokenizer;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADException;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PolicyUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

public class DECGetAllFinalizedRevisions_mxJPO {
    public int mxMain(Context context, String args[]) throws Exception {
        return 0;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAllFinalizedIds(Context context, String args[]) throws Exception {
        MapList inputObjList = new MapList();

        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);

            // Read the params
            String integrationName = (String) paramMap.get("integrationName");
            String localeLanguage = (String) paramMap.get("LocaleLanguage");
            String objectId = (String) paramMap.get("objectId");

            MCADGlobalConfigObject globalConfigObject = (MCADGlobalConfigObject) paramMap.get("GCO");

            StringTokenizer st = new StringTokenizer(objectId, "|");

            String parentId = st.nextToken();
            String childId = st.nextToken();

            String parentMajorId = getMajorObjectId(context, parentId);
            if (null == parentMajorId || "".equals(parentMajorId))
                parentMajorId = parentId;

            MCADServerResourceBundle resourceBundle = new MCADServerResourceBundle(localeLanguage);
            IEFGlobalCache _cache = new IEFGlobalCache();
            MCADMxUtil _util = new MCADMxUtil(context, resourceBundle, _cache);
            MCADServerGeneralUtil _generalUtil = new MCADServerGeneralUtil(context, globalConfigObject, resourceBundle, _cache);

            String currentMajorStateName = _util.getCurrentState(context, parentMajorId);

            BusinessObject busObj = new BusinessObject(childId);
            busObj.open(context);
            BusinessObjectList busList = busObj.getRevisions(context);
            busObj.close(context);

            BusinessObject tempObj = new BusinessObject();

            for (int i = 0; i < busList.size(); i++) {
                tempObj = busList.getElement(i);
                tempObj.open(context);
                if (_generalUtil.isBusObjectFinalized(context, tempObj)) {
                    HashMap idMap = new HashMap();
                    String tempBusId = tempObj.getObjectId();
                    if (!childId.equals(tempBusId) && !(PolicyUtil.checkState(context, tempBusId, currentMajorStateName, PolicyUtil.LT))) {
                        idMap.put("id", tempBusId);
                        inputObjList.add(idMap);
                    }
                }
                tempObj.close(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inputObjList;
    }

    private String getMajorObjectId(Context context, String busId) throws MCADException {
        String majBusObjId = null;
        // get actual name of relationship
        try {
            String sVersionOfRelName = (String) PropertyUtil.getSchemaProperty(context, "relationship_VersionOf");
            String SELECT_ON_MAJOR_ID = "from[" + sVersionOfRelName + "].to.id";

            String[] oids = new String[1];
            oids[0] = busId;

            StringList selectStmts = new StringList(1);
            selectStmts.add(SELECT_ON_MAJOR_ID);

            BusinessObjectWithSelectList busWithSelectList = BusinessObject.getSelectBusinessObjectData(context, oids, selectStmts);

            BusinessObjectWithSelect busWithSelect = (BusinessObjectWithSelect) busWithSelectList.elementAt(0);

            majBusObjId = busWithSelect.getSelectData(SELECT_ON_MAJOR_ID);
        } catch (Exception me) {
            String msg = "[DECGetAllFinalizedRevisions.getMajorObjectId] Exception: " + me.getMessage();
            System.out.println(msg);
            MCADServerException.createException(me.getMessage(), me);
        }

        return majBusObjId;
    }
}
