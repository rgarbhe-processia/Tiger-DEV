
/*
 ** DSCShowFolderContent.java
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display design label.
 */
import java.util.HashMap;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

public class DSCShowFolderContent_mxJPO {
    public static final String TNR_SEP = "~";

    private String localeLanguage = null;

    private HashMap integrationNameGCOTable = null;

    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private IEFGlobalCache cache = null;

    public DSCShowFolderContent_mxJPO(Context context, String[] args) throws Exception {
    }

    public Object getFolderPathHtmlString(Context context, String[] args) throws Exception {
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new MCADMxUtil(context, serverResourceBundle, cache);

        localeLanguage = (String) paramMap.get("LocaleLanguage");

        String htmlString = "";

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            try {
                HashMap objDetails = (HashMap) relBusObjPageList.get(i);

                String folderPath = (String) objDetails.get(DomainObject.SELECT_NAME);

                htmlString = replaceSpacesWithHtmlSpaces(folderPath);
            } catch (Exception e) {
            }

            columnCellContentList.add(htmlString);
        }

        return columnCellContentList;
    }

    private String replaceSpacesWithHtmlSpaces(String stringValue) {
        String stringValueWithHtmlSpaces = stringValue;

        int index = stringValueWithHtmlSpaces.indexOf(" ");
        while (index > -1) {
            stringValueWithHtmlSpaces = stringValueWithHtmlSpaces.substring(0, index) + "&nbsp;" + stringValueWithHtmlSpaces.substring(index + 1);
            index = stringValueWithHtmlSpaces.indexOf(" ");
        }

        return stringValueWithHtmlSpaces;
    }
}
