
//
// $Id: ${CLASSNAME}.java.rca 1.5 Wed Oct 22 16:54:21 2008 przemek Experimental przemek $
//
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

public class emxMultipleClassificationClassification_mxJPO extends emxMultipleClassificationClassificationBase_mxJPO {
    public emxMultipleClassificationClassification_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Override OOTB method for TIGTK-14892 :: ALM-4106 :: PSI The method reclassifies end items from one class to other in one step
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following list of arguments: 0 - childIds the list of end items to be reclassified 1 - currentParent Id 2 - newParent Id 3 - relationship the parent child relationship
     * @return HashMap with the reclassify details
     * @throws FrameworkException
     *             if the operation fails
     */
    public HashMap reclassify(Context context, String[] args) throws FrameworkException {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        Map map = null;
        try {
            map = (Map) JPO.unpackArgs(args);
        } catch (Exception e) {
            throw (new FrameworkException(e));
        }

        String[] childIds = (String[]) map.get("childIds");
        String currentParentId = (String) map.get("currentParent");
        String newParentId = (String) map.get("newParent");
        String relationship = (String) map.get("relationship");

        // START :: TIGTK-14892 :: ALM-4106 :: PSI
        if (UIUtil.isNullOrEmpty(newParentId.trim())) {
            return new HashMap();
        }
        // END :: TIGTK-14892 :: ALM-4106 :: PSI

        StringList toAddIds = toStringList(childIds);
        StringList children = null;
        try {
            children = getClassifiedItems(context, newParentId);
        } catch (Exception ex) {
            throw new FrameworkException(ex);
        }

        toAddIds.removeAll(children);
        String[] toAddIdsArray = toStringArray(toAddIds);
        HashMap childIdsMap = toHashMap(childIds);

        HashMap argsMap = new HashMap();
        argsMap.put("objectId", newParentId);
        argsMap.put("childIds", toAddIdsArray);
        argsMap.put("relationship", relationship);

        HashMap argsMapRemove = new HashMap();
        argsMapRemove.put("parentId", currentParentId);
        argsMapRemove.put("childIds", childIdsMap);
        argsMapRemove.put("relationship", relationship);

        String strLanguageStr = context.getSession().getLanguage();
        // check to see if user has "toconnect" access on the classifiedItems being reclassified
        // if not for any one item, then abort the process with an error
        try {

            StringList selects = new StringList(2);
            selects.add("current.access[toconnect]");
            selects.add(DomainObject.SELECT_ID);
            selects.add(DomainObject.SELECT_TYPE);
            selects.add(DomainObject.SELECT_NAME);
            selects.add(DomainObject.SELECT_REVISION);
            MapList mlist = DomainObject.getInfo(context, childIds, selects);
            Iterator mitr = mlist.iterator();
            MapList noToconnectAccessObjectMapList = new MapList();
            while (mitr.hasNext()) {
                Map m = (Map) mitr.next();
                boolean hasAccess = new Boolean((String) m.get("current.access[toconnect]")).booleanValue();
                if (!hasAccess) {
                    noToconnectAccessObjectMapList.add(m);
                }
            }

            if (noToconnectAccessObjectMapList.size() > 0) {
                // Changes added by PSA11 start(IR-532214-3DEXPERIENCER2015x).
                Iterator itr = noToconnectAccessObjectMapList.iterator();
                String errorMsg = "";
                while (itr.hasNext()) {
                    Map noToconnectAccessObjectMap = (Map) itr.next();
                    errorMsg += noToconnectAccessObjectMap.get(DomainObject.SELECT_TYPE) + " " + noToconnectAccessObjectMap.get(DomainObject.SELECT_NAME) + " "
                            + noToconnectAccessObjectMap.get(DomainObject.SELECT_REVISION) + "\n";
                }
                String selectOneObject = EnoviaResourceBundle.getProperty(context, "emxLibraryCentralStringResource", new Locale(strLanguageStr),
                        "emxMultipleClassification.Message.ObjectsNotReclassifiedNoToConnectAccess");
                errorMsg = selectOneObject + "\n" + errorMsg;

                throw new Exception(errorMsg);
                // Changes added by PSA11 end.
            }

        } catch (Exception exp) {
            String error = exp.getMessage();
            throw new FrameworkException(error);
        }
        StringList strList = new StringList();
        if (currentParentId != null) {
            // START :: TIGTK-14892 :: ALM-4106 :: PSI
            // strList.addElement(currentParentId);
            strList.addAll(FrameworkUtil.split(currentParentId, "|"));
            // END :: TIGTK-14892 :: ALM-4106 :: PSI
        }
        if (newParentId != null) {
            strList.addElement(newParentId);
        }

        HashMap returnMap = new HashMap();
        String sRelSubclass = com.matrixone.apps.library.LibraryCentralConstants.RELATIONSHIP_SUBCLASS;
        String strResult = "";
        String strId = "";
        for (int i = 0; i < strList.size(); i++) {
            strId = (String) strList.get(i);
            String strQuery = "expand bus $1 to relationship $2 recurse to all select bus $3 dump $4";
            strResult = MqlUtil.mqlCommand(context, strQuery, strId, sRelSubclass, "id", ",");
            returnMap.put(strId, strResult);
        }

        try {

            String lastObjectIdInAddChildren = toAddIdsArray[toAddIdsArray.length - 1];
            String lastObjectIdInRemoveObjects = "1";

            if (currentParentId != null) {
                lastObjectIdInRemoveObjects = (String) childIdsMap.get("childIds[" + (childIdsMap.size() - 1) + "]");
            }

            String[] modifiedArgs = JPO.packArgs(argsMap);
            // Start the transacation
            if (!context.isTransactionActive()) {
                context.start(true);
                MqlUtil.mqlCommand(context, "set env global $1 $2", "LASTOBJECTID", lastObjectIdInAddChildren);
                addChildren(context, modifiedArgs);
                String[] removeArgs = JPO.packArgs(argsMapRemove);
                if (currentParentId != null) {
                    MqlUtil.mqlCommand(context, "set env global $1 $2", "LASTOBJECTID", lastObjectIdInRemoveObjects);
                    // removeObjects(context, removeArgs);

                    // Changes added by PSA11 start(IR-532112-3DEXPERIENCER2015x).
                    StringBuffer strObjNotRemovedName = null;
                    String languageStr = context.getSession().getLanguage();
                    String objectsNotRemoved = removeObjects(context, removeArgs);
                    int index = objectsNotRemoved.indexOf("|");
                    String sObjId = null;
                    objectsNotRemoved = objectsNotRemoved.substring(0, index);
                    strObjNotRemovedName = new StringBuffer();
                    StringTokenizer st = new StringTokenizer(objectsNotRemoved, ",");
                    while (st.hasMoreTokens()) {
                        sObjId = st.nextToken().trim();
                        DomainObject dObj = new DomainObject(sObjId);
                        strObjNotRemovedName.append(dObj.getInfo(context, DomainObject.SELECT_NAME));
                        strObjNotRemovedName.append(" ");
                    }
                    if (strObjNotRemovedName.length() > 0) {
                        StringBuffer errorMsg = new StringBuffer();
                        errorMsg.append(EnoviaResourceBundle.getProperty(context, "emxLibraryCentralStringResource", new Locale(languageStr), "emxDocumentCentral.Message.ObjectsNotRemoved"));
                        errorMsg.append(" \n").append(strObjNotRemovedName.toString().trim());
                        throw new Exception(errorMsg.toString());
                    }
                    // Changes added by PSA11 end.
                }
                context.commit();
            } else {
                MqlUtil.mqlCommand(context, "set env global $1 $2", "LASTOBJECTID", lastObjectIdInAddChildren);
                addChildren(context, modifiedArgs);
                String[] removeArgs = JPO.packArgs(argsMapRemove);
                if (currentParentId != null) {
                    MqlUtil.mqlCommand(context, "set env global $1 $2", "LASTOBJECTID", lastObjectIdInRemoveObjects);
                    // removeObjects(context, removeArgs);

                    // Changes added by PSA11 start(IR-532112-3DEXPERIENCER2015x).
                    StringBuffer strObjNotRemovedName = null;
                    String languageStr = context.getSession().getLanguage();
                    String objectsNotRemoved = removeObjects(context, removeArgs);
                    int index = objectsNotRemoved.indexOf("|");
                    String sObjId = null;
                    objectsNotRemoved = objectsNotRemoved.substring(0, index);
                    strObjNotRemovedName = new StringBuffer();
                    StringTokenizer st = new StringTokenizer(objectsNotRemoved, ",");
                    while (st.hasMoreTokens()) {
                        sObjId = st.nextToken().trim();
                        DomainObject dObj = new DomainObject(sObjId);
                        strObjNotRemovedName.append(dObj.getInfo(context, DomainObject.SELECT_NAME));
                        strObjNotRemovedName.append(" ");
                    }
                    if (strObjNotRemovedName.length() > 0) {
                        StringBuffer errorMsg = new StringBuffer();
                        errorMsg.append(EnoviaResourceBundle.getProperty(context, "emxLibraryCentralStringResource", new Locale(languageStr), "emxDocumentCentral.Message.ObjectsNotRemoved"));
                        errorMsg.append(" \n").append(strObjNotRemovedName.toString().trim());
                        throw new Exception(errorMsg.toString());
                    }
                    // Changes added by PSA11 end.
                }
            }

            // End of transaction

        } catch (Exception e) {
            try {
                context.abort();
            } catch (Exception ex) {
                throw new FrameworkException(ex);
            }
            throw new FrameworkException(e);
        }
        return returnMap;
    }

    private StringList toStringList(String[] array) {
        int size = array.length;
        StringList returnList = new StringList();
        for (int k = 0; k < size; k++) {
            returnList.addElement(array[k]);
        }
        return returnList;
    }

    private String[] toStringArray(StringList list) {
        int size = list.size();
        String[] returnArray = new String[size];
        for (int k = 0; k < size; k++) {
            returnArray[k] = (String) list.get(k);
        }
        return returnArray;

    }

    private HashMap toHashMap(String[] array) {
        int size = array.length;
        HashMap map = new HashMap();
        for (int k = 0; k < size; k++) {
            map.put("childIds[" + k + "]", array[k]);
        }
        return map;
    }
}
