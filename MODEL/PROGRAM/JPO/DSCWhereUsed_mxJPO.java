
/*
 ** DSCWhereUsed
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** JPO to find where all this object is used
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADWhereusedHelper;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.utils.MCADLocalConfigObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.DomainObject;

public class DSCWhereUsed_mxJPO {
    protected MCADWhereusedHelper whereusedHelper = null;

    public DSCWhereUsed_mxJPO(Context context, String[] args) throws Exception {
    }

    private void init(Context context, String[] args) throws Exception {
        HashMap argumentMap = (HashMap) JPO.unpackArgs(args);

        String languageStr = (String) argumentMap.get("languageStr");
        MCADGlobalConfigObject gco = (MCADGlobalConfigObject) argumentMap.get("GCO");
        MCADLocalConfigObject lco = (MCADLocalConfigObject) argumentMap.get("LCO");

        MCADServerResourceBundle serverResourceBundle = new MCADServerResourceBundle(languageStr);

        whereusedHelper = new MCADWhereusedHelper(context, gco, lco, serverResourceBundle, null);
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getList(Context context, String[] args) throws Exception {
        init(context, args);

        MapList returnBusObjectList = new MapList();

        HashMap argumentMap = (HashMap) JPO.unpackArgs(args);

        StringList selectables = new StringList();
        selectables.add(DomainObject.SELECT_ID);
        selectables.add("physicalid");
        String objectId = (String) argumentMap.get("objectId");

        DomainObject doObj = DomainObject.newInstance(context, objectId);
        Map objInfo = doObj.getInfo(context, selectables);

        String sPhyId = (String) objInfo.get("physicalid");
        if (sPhyId.equals(objectId)) {
            objectId = (String) objInfo.get(DomainObject.SELECT_ID);
        }
        String filterLevel = (String) argumentMap.get("filterLevel");
        String filterRev = (String) argumentMap.get("filterRev");
        String filterVer = (String) argumentMap.get("filterVer");
        String lateralViewName = (String) argumentMap.get("filterDesignVersion");

        try {
            String level = "1";

            if (null != filterLevel && filterLevel.length() > 0 && filterLevel.indexOf("Highest") != -1) {
                level = "Highest";
            } else if (null != filterLevel && filterLevel.length() > 0) {
                level = getLevelOfExpansion(filterLevel);
            }

            returnBusObjectList = whereusedHelper.getWhereusedList(context, objectId, level, filterRev, filterVer, lateralViewName, false, true);
        } catch (Exception ex) {
            System.out.println("DSCWhereUsed::getList] Exception : " + ex.getMessage());
            MCADServerException.createException(ex.getMessage(), ex);
        }

        return returnBusObjectList;
    }

    private String getLevelOfExpansion(String filterLevel) {
        String expLevel = "1";

        if (filterLevel.indexOf("UpTo") == -1) {
            expLevel = "ALL";
        } else {
            int pos = filterLevel.indexOf(":");
            if (pos > 0) {
                String levelType = filterLevel.substring(0, pos);
                String levelNumber = filterLevel.substring(pos + 1, filterLevel.length());
                if (null != levelType && null != levelNumber && levelNumber.length() > 0 && levelType.indexOf("UpTo") != -1) {
                    expLevel = levelNumber;
                }
            }
        }

        return expLevel;
    }

    public Object getLevel(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        Vector columnCellContentList = new Vector();
        MapList objectList = (MapList) paramMap.get("objectList");
        String level = "0";

        for (int i = 0; i < objectList.size(); i++) {
            Map objDetails = (Map) objectList.get(i);
            level = (String) objDetails.get("nlevel");

            if (level == null || level.length() == 0) {
                level = "0";
            }

            columnCellContentList.add(level);
        }
        return columnCellContentList;
    }

    public Object getTargetDesignLabel(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        for (int i = 0; i < relBusObjPageList.size(); i++) {
            Map objDetails = (Map) relBusObjPageList.get(i);
            String childRev = (String) objDetails.get("childRev");
            if (null != childRev) {
                columnCellContentList.add(childRev);
            } else {
                columnCellContentList.add("");
            }
        }

        return columnCellContentList;
    }

    public MapList getAllRevisionAndVersions(Context context, String[] args) throws Exception {
        HashMap argMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) argMap.get("objectId");

        init(context, args);

        return whereusedHelper.getVersionsIdsForAllRevision(context, objectId);
    }
}
