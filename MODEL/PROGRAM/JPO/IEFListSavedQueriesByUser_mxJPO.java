
/**
 * IEFListSavedQueriesByUser.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries,
 * Copyright notice is precautionary only and does not evidence any actual or intended publication of such program JPO returns savedQueryList for context user Project. Infocentral Migration to UI
 * level 3 $Archive: $ $Revision: 1.2$ $Author: ds-unamagiri$
 * @since AEF 9.5.2.0
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;

import com.matrixone.MCADIntegration.uicomponents.beans.IEF_CellData;
import com.matrixone.MCADIntegration.uicomponents.beans.IEF_ColumnDefinition;
import com.matrixone.MCADIntegration.uicomponents.util.IEF_CustomMapList;

public class IEFListSavedQueriesByUser_mxJPO {
    public IEFListSavedQueriesByUser_mxJPO(Context context, String[] args) throws Exception {
    }

    public Object getColumnDefinitions(Context context, String[] args) throws Exception {
        ArrayList columnDefs = new ArrayList();
        // Creating 1 column: Query Name
        IEF_ColumnDefinition column1 = new IEF_ColumnDefinition();
        // Initializing column for Query Name
        column1.setColumnTitle("emxInfoCentral.Common.Name");
        column1.setColumnKey("QueryName");
        column1.setColumnDataType("string");
        column1.setColumnType("text");
        columnDefs.add(column1);
        return columnDefs;
    }

    public Object getTableData(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        IEF_CellData cellData = null;
        // get all saved queries for context user
        IEF_CustomMapList savedQueriesList = new IEF_CustomMapList();
        try {
            String command = "list  Query  user '" + context.getUser() + "'";
            MQLCommand mql = new MQLCommand();
            boolean ret = mql.executeCommand(context, "list query user $1", context.getUser());
            if (ret) {
                String result = mql.getResult();
                StringTokenizer tokenizer = new StringTokenizer(result, "\n");

                while (tokenizer.hasMoreElements()) {
                    String queryName = (String) tokenizer.nextElement();
                    Map map = new Hashtable();
                    cellData = new IEF_CellData();
                    cellData.setCellText(queryName);
                    // colum query name
                    map.put("QueryName", cellData);
                    // unique indentifier for each row
                    map.put("ID", queryName);
                    // setting ReadOnly flag
                    if (queryName.equals(".finder"))
                        map.put("ReadOnly", "false");
                    else
                        map.put("ReadOnly", "false");
                    savedQueriesList.add(map);
                }
            }
        } catch (Exception ex) {
            savedQueriesList = new IEF_CustomMapList();
        }
        // list of hashtable each hastable represents one row
        return savedQueriesList;

    }
}
