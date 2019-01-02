
/*
 ** DSCShowFilename
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to display FileName
 */

import matrix.db.Context;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.ListIterator;
import java.util.StringTokenizer;
import matrix.db.JPO;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;

import matrix.util.StringList;
import matrix.db.BusinessObject;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;

public class DSCShowFilename_mxJPO {
    private HashMap integrationNameGCOTable = null;

    public DSCShowFilename_mxJPO(Context context, String[] args) throws Exception {
    }

    public Object getFilenameForObject(Context context, String[] args) throws Exception {
        String REL_ACTIVE_VERSION = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
        String SELECT_ON_ACTIVE_MINOR = "from[" + REL_ACTIVE_VERSION + "].to.";
        Vector columnCellContentList = new Vector();

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramList = (HashMap) paramMap.get("paramList");
        MCADGlobalConfigObject gco = null;

        integrationNameGCOTable = (HashMap) paramList.get("GCOTable");

        if (integrationNameGCOTable == null)
            integrationNameGCOTable = (HashMap) paramMap.get("GCOTable");

        MapList relBusObjPageList = (MapList) paramMap.get("objectList");

        String ATTR_CADTYPE = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType") + "]";
        String ATTR_SOURCE = "attribute[" + MCADMxUtil.getActualNameForAEFData(context, "attribute_Source") + "]";
        StringList selectlist = new StringList(6);

        selectlist.add("id");
        selectlist.add("type");

        selectlist.add(DomainConstants.SELECT_FILE_NAME);
        selectlist.add(DomainConstants.SELECT_FILE_FORMAT);
        selectlist.add(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_FILE_NAME);
        selectlist.add(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_FILE_FORMAT);
        selectlist.add("FileName");
        selectlist.add(ATTR_CADTYPE);
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

            StringList fileList = busWithSelect.getSelectDataList(DomainConstants.SELECT_FILE_NAME);
            StringList formatList = busWithSelect.getSelectDataList(DomainConstants.SELECT_FILE_FORMAT);
            String busTypeName = (String) busWithSelect.getSelectData("type");
            String cadType = (String) busWithSelect.getSelectData(ATTR_CADTYPE);
            String sFileName = null;
            String integrationName = null;
            String integrationSource = busWithSelect.getSelectData(ATTR_SOURCE);
            if (integrationSource != null) {
                StringTokenizer integrationSourceTokens = new StringTokenizer(integrationSource, "|");

                if (integrationSourceTokens.hasMoreTokens())
                    integrationName = integrationSourceTokens.nextToken();
                if (integrationName != null && integrationNameGCOTable.containsKey(integrationName)) {
                    gco = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);
                }
            }

            if ((fileList != null && !fileList.isEmpty())) {
                sFileName = (String) fileList.get(0);
            }

            if (sFileName == null || sFileName.equals("")) {
                fileList = busWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_FILE_NAME);
                formatList = busWithSelect.getSelectDataList(SELECT_ON_ACTIVE_MINOR + DomainConstants.SELECT_FILE_FORMAT);
            }
            if ((fileList != null && !fileList.isEmpty()) && (sFileName == null || sFileName.equals(""))) {
                sFileName = (String) fileList.get(0);
            }

            if (sFileName == null || sFileName.equals("")) {
                sFileName = (String) busWithSelect.getSelectData("FileName");
            }

            if (fileList != null && fileList.size() > 1) {
                StringList filenamesList = new StringList();

                if (gco != null) {
                    // [NDM] Start OP6
                    String sMappedFormat = gco.getFormatsForType(busTypeName, cadType);
                    ;

                    /*
                     * if(gco.isMinorType(busTypeName)) { sMappedFormat= gco.getFormatsForType(gco.getCorrespondingType(busTypeName),cadType); } else sMappedFormat =
                     * gco.getFormatsForType(busTypeName,cadType);
                     */

                    // [NDM] Start OP6

                    if (sMappedFormat != null && !sMappedFormat.equals("")) {
                        ListIterator formatListItr = formatList.listIterator();
                        ListIterator fileListItr = fileList.listIterator();

                        while (formatListItr.hasNext()) {
                            String format = (String) formatListItr.next();
                            String file = (String) fileListItr.next();
                            if (format != null && format.equals(sMappedFormat)) {
                                filenamesList.add(file);
                            }
                        }
                    }

                }
                if (!filenamesList.isEmpty()) {
                    sFileName = (String) filenamesList.get(0);
                }
            }

            columnCellContentList.add(sFileName);
        }
        return columnCellContentList;
    }
}
