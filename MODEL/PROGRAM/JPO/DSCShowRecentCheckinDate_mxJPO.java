
/*
 ** DSCShowRecentCheckinDate
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display design label.
 */
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.util.MapList;

public class DSCShowRecentCheckinDate_mxJPO {
    public static final String TNR_SEP = "~";

    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private String localeLanguage = null;

    private IEFGlobalCache cache = null;

    public DSCShowRecentCheckinDate_mxJPO(Context context, String[] args) throws Exception {
    }

    private String getCheckinDate(Context context, MapList files, String objectId) {
        String checkinDate = "";
        String busType = "";
        String busName = "";
        String busRev = "";
        try {
            BusinessObject bus = new BusinessObject(objectId);
            bus.open(context);
            busType = bus.getTypeName();
            busName = bus.getName();
            busRev = bus.getRevision();
            bus.close(context);
            for (int i = 0; i < files.size(); i++) {

                String type = "";
                String name = "";
                String revision = "";
                Map map = (Map) files.get(i);
                String tnrString = (String) map.get("id");
                StringTokenizer tnr = new StringTokenizer(tnrString, TNR_SEP);
                if (tnr.hasMoreTokens()) {
                    type = tnr.nextToken();
                }
                if (tnr.hasMoreTokens()) {
                    name = tnr.nextToken();
                }
                if (tnr.hasMoreTokens()) {
                    revision = tnr.nextToken();
                }
                if (tnr.hasMoreTokens()) {
                    checkinDate = tnr.nextToken();
                }
                if (busType.equals(type) && busName.equals(name) && busRev.equals(revision)) {
                    return checkinDate;
                }
            }

            return checkinDate;

        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return checkinDate;
    }

    public Vector getDesignCheckinDate(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        localeLanguage = (String) paramMap.get("LocaleLanguage");

        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new MCADMxUtil(context, serverResourceBundle, cache);

        DSCCheckinHistoryUtil_mxJPO checkinHistoryProgram = new DSCCheckinHistoryUtil_mxJPO(context, args);

        MapList files = checkinHistoryProgram.getRecentCheckinFilesDetail(context, args);

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            String checkinDate = null;

            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);
                String objectId = (String) objDetails.get("id");
                String integrationName = util.getIntegrationName(context, objectId);

                checkinDate = getCheckinDate(context, files, objectId);
                BusinessObject bus = new BusinessObject(objectId);

                bus.close(context);
            } catch (Exception e) {

            }

            columnCellContentList.add(checkinDate);
        }

        return columnCellContentList;
    }

}
