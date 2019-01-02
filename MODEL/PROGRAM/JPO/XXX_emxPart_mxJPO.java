
/*
 ** ${CLASS:MarketingFeature}
 **
 ** Copyright (c) 1993-2013 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes. Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program
 */

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;

import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkProperties;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.engineering.Part;

import matrix.util.StringList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The <code>emxPart</code> class contains code for the "Part" business type.
 * @version EC 9.5.JCI.0 - Copyright (c) 2002, MatrixOne, Inc.
 */
public class XXX_emxPart_mxJPO extends emxPartBase_mxJPO {
    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @throws Exception
     *             if the operation fails.
     * @since EC 9.5.JCI.0.
     */

    public XXX_emxPart_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    @Override
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getStoredEBOM(Context context, String[] args) throws Exception {
        System.out.println("Method : getUM5StoredEBOM");
        // HashMap paramMap = (HashMap)JPO.unpackArgs(args);
        // String sExpandLevels = (String)paramMap.get("emxExpandFilter");
        String sChildren = null;
        // String IsStructureCompare = (String)paramMap.get("IsStructureCompare");
        // String langStr = (String)paramMap.get("languageStr");
        MapList ebomList = null;
        try {

            ebomList = getUM5EBOMsWithRelSelectables(context, args);

            Iterator itr = ebomList.iterator();
            MapList tList = new MapList();
            while (itr.hasNext()) {
                HashMap newMap = new HashMap((Map) itr.next());
                newMap.put("selection", "multiple");
                // Added for hasChildren starts
                sChildren = (String) newMap.get("from[" + DomainConstants.RELATIONSHIP_EBOM + "]");
                newMap.put("hasChildren", sChildren);
                // Added for hasChildren ends
                tList.add(newMap);
            }
            ebomList.clear();
            ebomList.addAll(tList);
            // 369074
            // if(sExpandLevels!=null && sExpandLevels.length()>0 && ebomList.size() > 0)
            // {
            HashMap hmTemp = new HashMap();
            hmTemp.put("expandMultiLevelsJPO", "true");
            ebomList.add(hmTemp);
            // }
        } catch (FrameworkException Ex) {
            throw Ex;
        }

        return ebomList;
    }

    public MapList getUM5EBOMsWithRelSelectables(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        int nExpandLevel = 0;
        String sExpandLevels = (String) paramMap.get("emxExpandFilter");
        if (sExpandLevels == null || sExpandLevels.equals("null") || sExpandLevels.equals("")) {
            sExpandLevels = (String) paramMap.get("ExpandFilter");
        }
        StringList selectStmts = new StringList(1);
        StringBuffer sObjWhereCond = new StringBuffer();
        if (sExpandLevels == null || sExpandLevels.length() == 0) {
            nExpandLevel = 1;
        } else {
            if ("All".equalsIgnoreCase(sExpandLevels))
                nExpandLevel = 0;
            else if (sExpandLevels != null && sExpandLevels.equalsIgnoreCase("EndItem")) {
                nExpandLevel = 0;
                if (sObjWhereCond.length() > 0) {
                    sObjWhereCond.append(" && ");
                }
                sObjWhereCond.append("(" + EngineeringConstants.SELECT_END_ITEM + " == '" + EngineeringConstants.STR_NO + "')");
                selectStmts.addElement("from[" + EngineeringConstants.RELATIONSHIP_EBOM + "].to.attribute[End Item]");
                selectStmts.addElement("from[" + EngineeringConstants.RELATIONSHIP_EBOM + "].id");
            }

            else
                nExpandLevel = Integer.parseInt(sExpandLevels);
        }

        String partId = (String) paramMap.get("objectId");
        // Added for PUE ECC Reports - R210
        String strObjectId1 = (String) paramMap.get("strObjectId1");
        String strObjectId2 = (String) paramMap.get("strObjectId2");

        String strEffVal = "";
        if (strObjectId1 != null && strObjectId1.equals(partId)) {
            strEffVal = "Eff Param 1";
        } else {
            strEffVal = "Eff Param 2";
        }

        String strSide = (String) paramMap.get("side");
        String strConsolidatedReport = (String) paramMap.get("isConsolidatedReport");
        MapList ebomList = new MapList();

        // reportType can be either BOM or AVL. Depending on this value Location Name is set.
        String reportType = "";
        // location variable holds the value of Location Name
        String location = "";
        // Added for IR-021267

        /*
         * StringList to store the selects on the Domain Object
         */

        /*
         * StringList to store the selects on the relationship
         */
        StringList selectRelStmts = new StringList(6);

        /*
         * String buffer to prepare where condition with End Item value
         */

        /*
         * stores the location ID
         */
        String locationId = null;
        /*
         * Maplist holds the data from the getCorporateMEPData method
         */
        MapList tempList = null;
        // retrieve the selected reportType from the paramMap
        reportType = (String) paramMap.get("reportType");
        // retrieve the selected location by the user
        location = (String) paramMap.get("location");

        // Object Where Clause added for Revision Filter

        String complete = PropertyUtil.getSchemaProperty(context, "policy", DomainConstants.POLICY_DEVELOPMENT_PART, "state_Complete");
        String selectedFilterValue = (String) paramMap.get("ENCBOMRevisionCustomFilter");

        if (selectedFilterValue == null) {
            if (strSide != null) {

                selectedFilterValue = (String) paramMap.get(strSide + "RevOption");
            }
            if (selectedFilterValue == null) {
                selectedFilterValue = "As Stored";
            }

        }
        // Commented for 098364
        // if(selectedFilterValue.equals("Latest Complete")) {
        // sObjWhereCond.append("((current == " +complete+") && (revision == last))||((current == "+ complete+") && (next.current != "+complete+"))");
        // }

        // To display AVL data for the first time with default Host Company of the user.
        try {
            Part partObj = new Part(partId);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            selectStmts.addElement(DomainConstants.SELECT_TYPE);
            selectStmts.addElement(DomainConstants.SELECT_NAME);
            selectStmts.addElement(DomainConstants.SELECT_REVISION);
            selectStmts.addElement(DomainConstants.SELECT_DESCRIPTION);
            // Added for hasChildren
            selectStmts.addElement("from[" + DomainConstants.RELATIONSHIP_EBOM + "]");
            // Added for MCC EC Interoperability Feature
            String strAttrEnableCompliance = PropertyUtil.getSchemaProperty(context, "attribute_EnableCompliance");
            selectStmts.addElement("attribute[" + strAttrEnableCompliance + "]");
            // end
            selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            selectRelStmts.addElement(SELECT_ATTRIBUTE_REFERENCE_DESIGNATOR);
            selectRelStmts.addElement(SELECT_ATTRIBUTE_QUANTITY);
            selectRelStmts.addElement(SELECT_ATTRIBUTE_FIND_NUMBER);
            selectRelStmts.addElement(SELECT_ATTRIBUTE_COMPONENT_LOCATION);
            selectRelStmts.addElement(SELECT_ATTRIBUTE_USAGE);
            // int level=1;
            int level = nExpandLevel;
            System.out.println("level=" + level);

            /* --Modif UM5 for Refinement on Effectivity -- */
            /*
             * ebomList = partObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, // relationship pattern DomainConstants.TYPE_PART, // object pattern selectStmts, // object selects
             * selectRelStmts, // relationship selects false, // to direction true, // from direction (short)level, // recursion level sObjWhereCond.toString(), // object where clause null); //
             * relationship where clause
             */

            // Params already unpacks in paramMap
            // get the value for the Structure Browser Effectivity Filter ? this will be in the form of a binary string
            String sEffectivityFilter = (String) paramMap.get("CFFExpressionFilterInput_OID");

            if (null == sEffectivityFilter || sEffectivityFilter.isEmpty() || sEffectivityFilter.equalsIgnoreCase("undefined")) {
                System.out.println("No Refinement Effectivity");
                ebomList = partObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, // relationship pattern
                        DomainConstants.TYPE_PART, // object pattern
                        selectStmts, // object selects
                        selectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) level, // recursion level
                        sObjWhereCond.toString(), // object where clause
                        null);
            } else {
                System.out.println("Refinement Effectivity :" + sEffectivityFilter + ";");
                // expand the object passing the filter expression parameter
                ebomList = partObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, // relationship pattern
                        DomainConstants.TYPE_PART, // object pattern
                        selectStmts, // object selects
                        selectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) level, // recursion level
                        null, // object where clause
                        null, // relationship where clause
                        (short) 0, // limit
                        false, // check hidden
                        false, // prevent duplicates
                        (short) 0, // pagesize
                        null, // includeType
                        null, // includeRelationship
                        null, // includeMap
                        null, // relKeyPrefix
                        sEffectivityFilter); // Effectivity filter expression as a binary hex string
            }

            // IR023752 start
            // if("EndItem".equalsIgnoreCase(sExpandLevels) )
            /* End Modif UM5 for Refinement on Effectivity */
            if ("EndItem".equalsIgnoreCase(sExpandLevels) && (null == sEffectivityFilter || sEffectivityFilter.isEmpty())) {
                ebomList = getEndItemMapList(context, ebomList, partObj, selectStmts, selectRelStmts);
            }

            // IR023752 end

            if (strConsolidatedReport != null && strConsolidatedReport.equalsIgnoreCase("Yes")) {
                MapList newBOMList = new MapList();
                getFlattenedMapList(ebomList, newBOMList, reportType);
                ebomList = newBOMList;
            }
            // Below code get the last revision of a domain object even if it is not connected to EBOM
            int ebomSize = ebomList.size();
            // -Modified for the fix IR-013085

            // if(ebomList!=null && ebomSize>0 && selectedFilterValue.equals("Latest")) {
            if ((ebomList != null && ebomSize > 0 && selectedFilterValue.equals("Latest")) || (selectedFilterValue.equals("Latest Complete"))) {
                Iterator itr = ebomList.iterator();
                MapList LRev = new MapList();
                String objID = "";
                // Iterate through the maplist and add those parts that are latest but not connected
                while (itr.hasNext()) {
                    Map newMap = (Map) itr.next();
                    String ObjectId = (String) newMap.get("id");
                    String oldRev = (String) newMap.get("revision");
                    DomainObject domObj = DomainObject.newInstance(context, ObjectId);
                    // get the last revision of the object
                    BusinessObject bo = domObj.getLastRevision(context);
                    bo.open(context);
                    objID = bo.getObjectId();
                    String newRev = bo.getRevision();
                    // Modifed for the IR-013085
                    bo.close(context);
                    if (selectedFilterValue.equals("Latest")) {
                        if (oldRev != newRev || !oldRev.equals(newRev)) {
                            newMap.put("id", objID);
                        }
                    }
                    // Added for the IR-013085
                    else if (selectedFilterValue.equals("Latest Complete")) {
                        DomainObject domObjLatest = DomainObject.newInstance(context, objID);
                        String currSta = domObjLatest.getInfo(context, DomainConstants.SELECT_CURRENT);
                        // Added for the IR-026773
                        if (oldRev.equals(newRev)) {
                            if (!complete.equals(currSta))
                                continue;
                            newMap.put("id", objID);

                        } // IR-026773 ends
                        else {
                            while (!currSta.equalsIgnoreCase(complete) && !currSta.equals(complete)) {
                                BusinessObject boObj = domObjLatest.getPreviousRevision(context);
                                boObj.open(context);
                                objID = boObj.getObjectId();
                                currSta = (String) (DomainObject.newInstance(context, objID).getInfo(context, DomainConstants.SELECT_CURRENT));
                                boObj.close(context);
                            }

                            newMap.put("id", objID);
                        }
                    } // IR-013085 ends
                      // Add new map to the HashMap
                    LRev.add(newMap);
                }
                ebomList.clear();
                ebomList.addAll(LRev);
            }

            if (location != null && ("").equals(location) && reportType != null && reportType.equals("AVL")) {
                // retrieve the Person object
                // com.matrixone.apps.common.Person person = (com.matrixone.apps.common.Person)DomainObject.newInstance(context, DomainConstants.TYPE_PERSON);
                // retrieve the Host Company attached to the User.
                location = com.matrixone.apps.common.Person.getPerson(context).getCompany(context).getObjectId(context);
            }
            if (location != null && reportType != null && reportType.equals("AVL")) {
                tempList = new MapList();
                // com.matrixone.apps.common.Person person = (com.matrixone.apps.common.Person)DomainObject.newInstance(context, DomainConstants.TYPE_PERSON);
                locationId = com.matrixone.apps.common.Person.getPerson(context).getCompany(context).getObjectId(context);
                if (locationId.equals(location)) {
                    // In case of Host Company
                    tempList = partObj.getCorporateMEPData(context, ebomList, locationId, true, partId);
                } else {
                    // In case of selected location and All locations
                    tempList = partObj.getCorporateMEPData(context, ebomList, location, false, partId);
                }
                ebomList.clear();
                ebomList.addAll(tempList);
            }

            // fix for bug 311050
            // check the parent obj state
            boolean allowChanges = true;
            StringList strList = new StringList(2);
            strList.add(SELECT_CURRENT);
            strList.add("policy");

            Map map = partObj.getInfo(context, strList);

            String objState = (String) map.get(SELECT_CURRENT);
            String objPolicy = (String) map.get("policy");

            String propAllowLevel = FrameworkProperties.getProperty(context, "emxEngineeringCentral.Part.RestrictPartModification");
            StringList propAllowLevelList = new StringList();

            if (propAllowLevel != null || !("".equals(propAllowLevel)) || !("null".equals(propAllowLevel))) {
                StringTokenizer stateTok = new StringTokenizer(propAllowLevel, ",");
                while (stateTok.hasMoreTokens()) {
                    String tok = (String) stateTok.nextToken();
                    propAllowLevelList.add(FrameworkUtil.lookupStateName(context, objPolicy, tok));
                }
            }
            allowChanges = (!propAllowLevelList.contains(objState));

            // set row editable option
            Iterator itr = ebomList.iterator();
            MapList tList = new MapList();
            while (itr.hasNext()) {
                Map newMap = (Map) itr.next();
                if (allowChanges) {
                    newMap.put("RowEditable", "show");
                } else {
                    newMap.put("RowEditable", "readonly");
                }
                tList.add(newMap);
            }
            ebomList.clear();
            ebomList.addAll(tList);
            // end of fix for bug 311050

            // fix for bug 311050
            // check the parent obj state
            // boolean allowChanges = true;
            // StringList strList = new StringList(2);
            strList.add(SELECT_CURRENT);
            strList.add("policy");

            if (propAllowLevel != null || !("".equals(propAllowLevel)) || !("null".equals(propAllowLevel))) {
                StringTokenizer stateTok = new StringTokenizer(propAllowLevel, ",");
                while (stateTok.hasMoreTokens()) {
                    String tok = (String) stateTok.nextToken();
                    propAllowLevelList.add(FrameworkUtil.lookupStateName(context, objPolicy, tok));
                }
            }
            allowChanges = (!propAllowLevelList.contains(objState));

            // set row editable option
            // Iterator itr = ebomList.iterator();
            // MapList tList = new MapList();
            while (itr.hasNext()) {
                Map newMap = (Map) itr.next();
                if (allowChanges) {
                    newMap.put("RowEditable", "show");
                } else {
                    newMap.put("RowEditable", "readonly");
                }
                tList.add(newMap);
            }
            ebomList.clear();
            ebomList.addAll(tList);
            // end of fix for bug 311050

        }

        catch (FrameworkException Ex) {
            throw Ex;
        }

        return ebomList;

    }

}