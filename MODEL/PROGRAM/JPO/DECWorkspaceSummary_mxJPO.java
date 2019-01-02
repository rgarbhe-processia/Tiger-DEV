
/*
 ** DECNumberOfDocuments
 **
 ** Copyright Dassault Systemes, 1992-2010. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Fetches the number of documents under a folder(Workspace Vault)
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.List;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;

public class DECWorkspaceSummary_mxJPO {
    public Object getNumberOfDocuments(Context context, String[] args) {
        Vector retunValues = new Vector();

        try {
            Map dataMap = (Map) JPO.unpackArgs(args);

            List objectList = (List) dataMap.get("objectList");

            String[] folderIds = new String[objectList.size()];

            for (int i = 0; i < objectList.size(); i++) {
                Map idsMap = (Map) objectList.get(i);
                folderIds[i] = idsMap.get("id").toString();
            }

            String actualRelationName = MCADMxUtil.getActualNameForAEFData(context, "relationship_VaultedDocuments");
            String acutalTypeName = MCADMxUtil.getActualNameForAEFData(context, "type_DOCUMENTS");

            // Condition clause that select all DOCUMENTS id that are at 'to' side of folder and have relationship 'Vaulted Objects'
            String SELECT_DOCUMENT_IDS = "from[" + actualRelationName + "].to[" + acutalTypeName + "].id";

            StringList busSelectList = new StringList();
            busSelectList.addElement(SELECT_DOCUMENT_IDS);

            BusinessObjectWithSelectList businessObjectWithSelectList = BusinessObject.getSelectBusinessObjectData(context, folderIds, busSelectList);

            for (int index = 0; index < businessObjectWithSelectList.size(); index++) {
                int totalCount = 0;
                BusinessObjectWithSelect data = businessObjectWithSelectList.getElement(index);
                Vector selectKeys = data.getSelectKeys();
                Vector selectValues = data.getSelectValues();

                ArrayList selectKeysList = new ArrayList(selectKeys);

                for (Iterator iterator = selectKeysList.iterator(); iterator.hasNext();) {
                    String selectForExpand = (String) iterator.next();

                    int selectDataCount = data.getSelectDataList(selectForExpand).size();

                    int indexOfSelectKey = selectKeys.indexOf(selectForExpand);

                    if (indexOfSelectKey > -1) {
                        selectKeys.removeElementAt(indexOfSelectKey);
                        selectValues.removeElementAt(indexOfSelectKey);
                    }

                    totalCount = totalCount + selectDataCount;
                }

                String numberOfDocument = String.valueOf(totalCount);
                retunValues.addElement(numberOfDocument);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return retunValues;
    }
}
