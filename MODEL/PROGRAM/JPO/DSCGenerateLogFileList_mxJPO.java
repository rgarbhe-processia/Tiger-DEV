
/*
 ** DSCGenerateLogFileList
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Populates the rows and columns of the DSCLogFileList table
 */

import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.MCADIntegration.server.MCADServerException;
import com.matrixone.apps.domain.util.MapList;

public class DSCGenerateLogFileList_mxJPO {
    /**
     * @param context
     *            the Matrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     */
    public DSCGenerateLogFileList_mxJPO(Context context, String[] args) throws Exception {
        if (!context.isConnected()) {
            MCADServerException.createException("not supported on desktop client", null);
        }
    }

    /**
     * This method is executed if a specific method is not specified.
     * @param context
     *            the Matrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @returns nothing
     * @throws Exception
     *             if the operation fails
     */
    public int mxMain(Context context, String[] args) throws Exception {
        if (!context.isConnected()) {
            MCADServerException.createException("not supported on desktop client", null);
        }
        return 0;
    }

    /**
     * Get the list of entries to display.
     * @param context
     *            the Matrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - objectList MapList
     * @returns Object of type MapList
     * @throws Exception
     *             if the operation fails
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getFileList(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        // Get list of files retrieved by applet
        String fileList = (String) paramMap.get("appletReturn");
        if (fileList == null)
            fileList = "";

        MapList idList = new MapList();
        StringTokenizer st = new StringTokenizer(fileList, "|");

        while (st.hasMoreTokens()) {
            HashMap item = new HashMap();
            item.put("id", st.nextToken());
            idList.add(item);
        }
        return idList;
    }

    /**
     * Get the list of filenames to display in the filename column.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains a Map with the following entries: objectList contains a MapList
     * @return Vector contains list of Object names
     * @throws Exception
     *             if the operation fails
     */

    public Vector getFileName(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        Vector vec = new Vector(objectList.size());

        try {
            for (int i = 0; i < objectList.size(); i++) {
                HashMap collMap = (HashMap) objectList.get(i);
                String fileName = java.net.URLEncoder.encode((String) collMap.get("id"));
                String StrName = "<a href=\"../iefdesigncenter/emxDSCDisplayClientHtmlFile.jsp?fileName=" + fileName + "\" target=\"_blank\">" + (String) collMap.get("id") + "</a>";
                vec.addElement(StrName);
            }
        } catch (Exception e) {
            MCADServerException.createException(e.toString(), e);
        }
        return vec;
    }

    /**
     * Get the list of integration names to display in the integName column.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains a Map with the following entries: objectList contains a MapList
     * @return Vector contains list of integration names
     * @throws Exception
     *             if the operation fails
     */
    public Vector getIntegrationName(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        Vector vec = new Vector(objectList.size());

        try {
            for (int i = 0; i < objectList.size(); i++) {
                HashMap collMap = (HashMap) objectList.get(i);
                // String fileNameWithPath = java.net.URLEncoder.encode((String)collMap.get("id"));
                String fileNameWithPath = (String) collMap.get("id");
                int lastPathIndex = fileNameWithPath.lastIndexOf(java.io.File.separator);

                String fileName = fileNameWithPath.substring(lastPathIndex + 1);

                StringTokenizer st = new StringTokenizer(fileName, "_");
                int j = 0;
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (j == 1) {
                        vec.addElement(token);
                    }
                    j++;
                }
            }
        } catch (Exception e) {
            MCADServerException.createException(e.toString(), e);
        }
        return vec;
    }
}
