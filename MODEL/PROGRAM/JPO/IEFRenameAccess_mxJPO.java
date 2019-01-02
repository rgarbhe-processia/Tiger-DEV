
/*
 ** IEFRenameAccess
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to get Rename access info
 */
import java.util.HashMap;
import java.util.Hashtable;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectItr;
import matrix.db.BusinessObjectList;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;

public class IEFRenameAccess_mxJPO {
    private MCADServerResourceBundle serverResourceBundle = null;

    private MCADMxUtil util = null;

    private String localeLanguage = null;

    private IEFGlobalCache cache = null;

    public IEFRenameAccess_mxJPO(Context context, String[] args) throws Exception {
    }

    // returns String [] arg, arg[0] = "true" OR "false" arg[1] = error message (if arg[0] is "false")
    public String[] getRenameAccessInfo(Context context, String[] args) throws Exception {
        String objectID = args[0];
        String language = args[1];

        serverResourceBundle = new MCADServerResourceBundle(language);

        String[] renameAccessInfo = new String[2];
        renameAccessInfo[0] = "true";
        renameAccessInfo[1] = "";

        cache = new IEFGlobalCache();
        util = new MCADMxUtil(context, serverResourceBundle, cache);

        String userName = context.getUser();

        BusinessObject majorBusObject = new BusinessObject(objectID);
        majorBusObject.open(context);
        String revision = majorBusObject.getRevision();
        majorBusObject.close(context);
        BusinessObjectList majorBusObjectsList = majorBusObject.getRevisions(context);
        BusinessObjectItr majorBusObjectsItr = new BusinessObjectItr(majorBusObjectsList);
        while (majorBusObjectsItr.next()) {
            BusinessObject busObject = majorBusObjectsItr.obj();

            String locker = busObject.getLocker(context).getName();
            if (locker != null && !locker.equals("") && !locker.equals(userName)) {
                revision = busObject.getRevision();
                renameAccessInfo[0] = "false";
                Hashtable messageTable = new Hashtable();
                messageTable.put("REVISION", revision);
                messageTable.put("USERNAME", locker);

                renameAccessInfo[1] = serverResourceBundle.getString("mcadIntegration.Server.Message.CannotRenameObjectStreamIsLockedByUser", messageTable);
                break;
            } else if (!busObject.getAccessMask(context).hasChangeNameAccess()) {
                renameAccessInfo[0] = "false";

                Hashtable messageTable = new Hashtable();
                messageTable.put("REVISION", revision);

                renameAccessInfo[1] = serverResourceBundle.getString("mcadIntegration.Server.Message.CannotRenameObjectNoRenameAccess", messageTable);
                break;
            }
        }
        return renameAccessInfo;
    }

    // Pages other than details page
    public Boolean hasRenameAccess(Context context, String[] args) {
        boolean hasRenameAccess = false;

        try {
            Access access = matrix.db.Person.getAccessMask(context);
            hasRenameAccess = access.hasChangeNameAccess();
        } catch (Throwable e) {
        }

        return new Boolean(hasRenameAccess);
    }

    // Details page
    public Boolean hasRenameAccessOnBusObj(Context context, String[] args) {
        boolean hasRenameAccess = false;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) paramMap.get("objectId");
            String majorObjId = null;
            String relVersionOf = "from[" + MCADMxUtil.getActualNameForAEFData(context, "relationship_VersionOf") + "].to.id";

            String[] objIds = new String[1];
            objIds[0] = objectId;

            StringList busSelectionList = new StringList();
            busSelectionList.addElement("id");
            busSelectionList.addElement(relVersionOf);

            BusinessObjectWithSelectList buslWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, objIds, busSelectionList);
            BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) buslWithSelectionList.elementAt(0);

            if (null != busObjectWithSelect)
                majorObjId = busObjectWithSelect.getSelectData(relVersionOf);

            if (null == majorObjId || majorObjId.equals(""))
                majorObjId = objectId;

            IEFActionAccess_mxJPO integAccessJPO = new IEFActionAccess_mxJPO(context, args);

            BusinessObject busObject = new BusinessObject(majorObjId);

            Access ObjAccess = busObject.getAccessMask(context);

            if (ObjAccess.hasChangeNameAccess() && integAccessJPO.canShowCommandForIntegUser(context, args).equals(Boolean.TRUE)) {
                hasRenameAccess = true;
            }
        } catch (Throwable e) {
        }

        return new Boolean(hasRenameAccess);
    }
}
