
/**
 * IEFListTypesToConnect.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries,
 * Copyright notice is precautionary only and does not evidence any actual or intended publication of such program list all types can be conencted to a object by a relationship name and a driection
 * $Archive: $ $Revision: 1.2$ $Author: ds-unamagiri$ rahulp
 * @since AEF 9.5.2.0
 */
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;

import matrix.db.BusinessType;
import matrix.db.BusinessTypeItr;
import matrix.db.BusinessTypeList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.StringList;

public class IEFListTypesToConnect_mxJPO {

    public IEFListTypesToConnect_mxJPO(Context context, String[] args) throws Exception {
    }

    public Object getList(Context context, String[] args) throws Exception {
        StringList sListBusinessType = new StringList();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);

        try {
            BusinessTypeList btListFrom = null, btListTo = null;
            // relationship type name
            String sRelType = (String) paramMap.get("sRelationName");
            // object type
            String sObjType = (String) paramMap.get("sObjType");
            // relationship direction to,from
            String from = (String) paramMap.get("sRelDirection");

            /*
             * //decode relation type and relationship direction if(sRelType!=null) sRelType = URLDecoder.decode(sRelType); if(sObjType!=null) sObjType = URLDecoder.decode(sObjType);
             */

            String sTypePattern = "";
            // boolean flag for relationship direction ,true for from direction
            boolean bDirectionFromFlag = false;
            if (from != null && from.equals("from"))
                bDirectionFromFlag = true;
            BusinessTypeItr btItrObj = null;
            boolean bToAll = false;
            boolean bFromAll = false;

            // cerate RelationshipType from its name
            RelationshipType relTypeSelected = new RelationshipType(sRelType);
            relTypeSelected.open(context);
            // To check whether all the types are there for a relationship in From end and To end.
            if (relTypeSelected.getFromAllTypesAllowed())
                bFromAll = true;
            if (relTypeSelected.getToAllTypesAllowed())
                bToAll = true;
            // To get the From types of the Relationship selected
            if (bFromAll)
                btListFrom = BusinessType.getBusinessTypes(context, true);
            else
                btListFrom = relTypeSelected.getFromTypes(context, true);

            // To get the To types of the Relationship selected
            if (bToAll)
                btListTo = BusinessType.getBusinessTypes(context, true);
            else
                btListTo = relTypeSelected.getToTypes(context, true);

            // To get the list of types
            if (bDirectionFromFlag)
                btItrObj = new BusinessTypeItr(btListFrom);
            else
                btItrObj = new BusinessTypeItr(btListTo);
            relTypeSelected.close(context);
            // To check if there are types connected to To/From End
            if (btItrObj != null) {
                // boolean bISParentContains = false;
                boolean bBuildQueryType = false;
                // Stores the Type object.
                Vector vTypePattern = new Vector();
                /*
                 * 1.If the current object Type present in both ends of the selected relationship,then query for all selected relationship end allowed types. 2.If the current object type present in
                 * the selected relation end allowed types and not in other end then do not query for any types, since Object does not have same end connect access 3.If the current Object not present
                 * in the selected relationship end allowed types and present in the other end allowed types then query for all selected relationship end allowed types.
                 */
                // To check for building query
                if (bDirectionFromFlag) {
                    if (btListFrom.find(sObjType) != null) {
                        if (btListTo.find(sObjType) != null) {
                            bBuildQueryType = true;
                        } else {
                            bBuildQueryType = false;
                        }
                    } else if (btListTo.find(sObjType) != null) {
                        bBuildQueryType = true;
                    }
                } else {
                    if (btListTo.find(sObjType) != null) {
                        if (btListFrom.find(sObjType) != null)
                            bBuildQueryType = true;
                        else
                            bBuildQueryType = false;

                    } else if (btListFrom.find(sObjType) != null)
                        bBuildQueryType = true;
                }
                if (bBuildQueryType) {
                    // If All the types are present in both the ends
                    if (bToAll && bFromAll) {
                        sTypePattern = "*";
                    } else if (bDirectionFromFlag && bFromAll) {// If All the types are presnt in From End
                        sTypePattern = "*";
                    } else if (!bDirectionFromFlag && bToAll) {// If All the types are presnt in To End
                        sTypePattern = "*";
                    } else {
                        while (btItrObj.next()) {
                            boolean bISParentContains = false;
                            BusinessType btQuery = (BusinessType) btItrObj.obj();
                            btQuery.open(context);
                            String sParentName = btQuery.getParent(context);
                            String sObjectName = btQuery.getName();
                            // Get only the Parent Business Type and add to type pattern
                            if (bDirectionFromFlag) {
                                if (btListFrom.find(sParentName) != null) {
                                    bISParentContains = true;
                                }
                            } else {
                                if (btListTo.find(sParentName) != null) {
                                    bISParentContains = true;
                                }
                            }
                            // Add the Type to the pattern if it has no parents, or it's parent is not in the list.
                            if (sParentName != null) {
                                if (sParentName.trim().equals("") || !bISParentContains) {
                                    if (!vTypePattern.contains(sObjectName)) {
                                        vTypePattern.add(btQuery.getName());
                                        sTypePattern += btQuery.getName() + ",";
                                    }
                                }
                            }
                            btQuery.close(context);
                        }
                    }
                }
                vTypePattern.clear();
                // To get the types of the objects that can be connected to the existing object
                if (!sTypePattern.equals("")) {
                    StringTokenizer sTokBusTypes = new StringTokenizer(sTypePattern, ",");
                    while (sTokBusTypes.hasMoreElements()) {
                        sListBusinessType.addElement(sTokBusTypes.nextToken());
                    }
                }
            }
        } catch (Exception ex) {
            sListBusinessType = new StringList();
        }
        return sListBusinessType;
    }
}
