
/*
 ** DSCShowTitle
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display Major object Title
 */

import matrix.db.Context;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.StringTokenizer;
import matrix.db.JPO;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;

import matrix.util.StringList;
import matrix.db.BusinessObject;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;

public class DSCShowTitle_mxJPO {
    private HashMap integrationNameGCOTable = null;

    public DSCShowTitle_mxJPO(Context context, String[] args) throws Exception {
    }

    public Object getTitleForObject(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramList = (HashMap) paramMap.get("paramList");
        MCADGlobalConfigObject gco = null;

        integrationNameGCOTable = (HashMap) paramList.get("GCOTable");

        if (integrationNameGCOTable == null)
            integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        String REL_VERSION_OF = MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf");
        String SELECT_ON_MAJOR = "from[" + REL_VERSION_OF + "].to.";
        String ATTR_Title = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Title") + "]";
        String ATTR_SOURCE = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";

        StringList selectlist = new StringList(6);

        selectlist.add("id");
        selectlist.add("type");

        selectlist.add(ATTR_Title);
        selectlist.add(SELECT_ON_MAJOR + ATTR_Title);
        selectlist.add(ATTR_SOURCE);

        HashMap gcoTable = new HashMap();
        String[] oids = new String[relBusObjPageList.size()];

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            String objectId = (String) objDetails.get("id");
            oids[i] = objectId;
        }

        BusinessObjectWithSelectList busWithSelectlist = BusinessObject.getSelectBusinessObjectData(context, oids, selectlist);

        for (int i = 0; i < busWithSelectlist.size(); i++) {
            BusinessObjectWithSelect busWithSelect = busWithSelectlist.getElement(i);

            String integrationName = null;
            String integrationSource = busWithSelect.getSelectData(ATTR_SOURCE);

            if (integrationSource != null) {
                StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

                if (integrationSourceTokens.hasMoreTokens())
                    integrationName = integrationSourceTokens.nextToken();
            }

            MCADGlobalConfigObject gcObject = null;

            if (integrationName != null && integrationNameGCOTable.containsKey(integrationName)) {
                gcObject = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);
            }

            String title = busWithSelect.getSelectData(ATTR_Title);
            ;

            if (null != gcObject && !gcObject.isObjectAndFileNameDifferent()) {
                String strType = busWithSelect.getSelectData("type");
                boolean isInputMajor = gcObject.isMajorType(strType);

                if (!isInputMajor) {
                    title = busWithSelect.getSelectData(SELECT_ON_MAJOR + ATTR_Title);
                }
            }

            columnCellContentList.add(title);
        }

        return columnCellContentList;
    }
}
