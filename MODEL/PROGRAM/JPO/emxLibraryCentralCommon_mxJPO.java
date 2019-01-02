
/*
 * emxLibraryCentralCommon.java
 *
 * Copyright (c) 1992-2015 Dassault Systemes.
 *
 * All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not evidence any actual or intended
 * publication of such program.
 *
 * static const RCSID [] = "$Id: ${CLASSNAME}.java.rca 1.8 Wed Oct 22 16:02:38 2008 przemek Experimental przemek $";
 */

import java.util.HashMap;
import java.util.StringTokenizer;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.library.LibraryCentralConstants;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.StringList;

/**
 * @version AEF Rossini - Copyright (c) 2002, MatrixOne, Inc.
 */
public class emxLibraryCentralCommon_mxJPO extends emxLibraryCentralCommonBase_mxJPO {

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            the Java <code>String[]</code> object
     * @throws Exception
     *             if the operation fails
     * @since AEF 10.6.0.1
     */

    public emxLibraryCentralCommon_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Override OOTB method for TIGTK-14892 :: ALM-4106 :: PSI Removes objects from a class
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following arguments: childIds - a String array of children object ids relationship - the relationship to connect the new object with parentId - the id of the parent Object
     *            to connect with
     * @returns String with the result of the delete.
     * @throws Exception
     *             if the operation fails
     */
    public String removeObjects(Context context, String[] packedArgs) throws Exception {
        HashMap mapObjsToDelete = null;
        HashMap childIds = null;
        String objectsNotRemoved = "";
        String parentId = "";
        String relationship = "";
        String strObjectID = "";
        String sRelSubclass = LibraryCentralConstants.RELATIONSHIP_SUBCLASS;
        String sRelClassifiedItem = LibraryCentralConstants.RELATIONSHIP_CLASSIFIED_ITEM;
        StringBuffer strRelationhipPattern = new StringBuffer();
        strRelationhipPattern.append(sRelSubclass);
        strRelationhipPattern.append(",");
        strRelationhipPattern.append(sRelClassifiedItem);
        String strChildIds[] = null;
        String searchType = "";
        String strQuery = null;
        try {
            mapObjsToDelete = (HashMap) JPO.unpackArgs(packedArgs);
            parentId = (String) mapObjsToDelete.get("parentId");
            relationship = (String) mapObjsToDelete.get("relationship");
            childIds = (HashMap) mapObjsToDelete.get("childIds");
            searchType = (String) mapObjsToDelete.get("searchType");

            String strChildId = "";
            RelationshipType relType = new RelationshipType(relationship);
            // START :: TIGTK-14892 :: ALM-4106 :: PSI
            StringList slParentsOID = FrameworkUtil.split(parentId, "|");
            DomainObject parentObj;
            for (Object object : slParentsOID) {
                parentId = (String) object;
                parentObj = new DomainObject(parentId);
                // END :: TIGTK-14892 :: ALM-4106 :: PSI
                // Temporary variables for children
                DomainObject tempChildObj;
                int iSize = childIds.size();
                StringList slSelAttrs = new StringList();
                slSelAttrs.add(SELECT_ID);
                for (int i = 0; i < iSize; i++) {
                    boolean removed = false;
                    strChildId = (String) childIds.get("childIds[" + i + "]");
                    tempChildObj = new DomainObject((String) childIds.get("childIds[" + i + "]"));
                    try {
                        MapList resultList = (MapList) tempChildObj.getRelatedObjects(context, strRelationhipPattern.toString(), LibraryCentralConstants.QUERY_WILDCARD, slSelAttrs, null, false, true,
                                (short) 1, null, null);
                        if (resultList.size() == 0) {
                            parentObj.disconnect(context, relType, true, tempChildObj);
                            removed = true;
                            if (searchType != null && searchType.equalsIgnoreCase("All Levels")) {
                                String strResultID = "";
                                String strTemp = "";
                                strQuery = "expand bus $1 from relationship $2,$3 recurse to all select bus $4 where $5 == $6 dump $7";
                                String strResult = MqlUtil.mqlCommand(context, strQuery, parentId, sRelSubclass, sRelClassifiedItem, "id", "from[" + sRelClassifiedItem + "].to.id", strChildId.trim(),
                                        ",");
                                StringTokenizer stResult = new StringTokenizer(strResult, "\n");
                                while (stResult.hasMoreTokens()) {
                                    strTemp = (String) stResult.nextToken();
                                    strResultID = strTemp.substring(strTemp.lastIndexOf(",") + 1);
                                    parentObj.setId(strResultID);
                                    parentObj.disconnect(context, relType, true, tempChildObj);
                                }
                            }
                        } else {
                            objectsNotRemoved += (String) childIds.get("childIds[" + i + "]") + ",";
                        }

                    } catch (Exception e) {
                        objectsNotRemoved += (String) childIds.get("childIds[" + i + "]") + ",";
                    }
                }

                strQuery = "expand bus $1 to relationship $2 recurse to all select bus $3 dump $4";
                objectsNotRemoved += "|" + (String) MqlUtil.mqlCommand(context, strQuery, parentId, sRelSubclass, "id", ",");
            } // TIGTK-14892 :: ALM-4106 :: PSI
        } catch (Exception eUnpack) {
            throw eUnpack;

        }
        // --Return Objects Not Removed
        return objectsNotRemoved;
    }

}
