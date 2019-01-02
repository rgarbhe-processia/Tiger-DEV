package pss.uls;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.common.InboxTask;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.json.JSONObject;

import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.common.utils.TigerUtils;
import pss.constants.TigerConstants;

@SuppressWarnings("serial")
public class ULSUIUtil_mxJPO extends DomainObject {

    final static String RECURSION_LEVEL = "level";

    final static String RECURSION_VALUE = "1";

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ULSUIUtil_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    /**
     * This method for Connect the BG and OEMGroup to the OEMGroup is created.
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public void connectBGToOEMGroup(Context context, String[] args) throws Exception {
        try {
            // MapList mlList = new MapList();
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> paramMap = (HashMap<?, ?>) programMap.get("paramMap");

            // get object id Of OEMGroup from param map
            String strObjectId = (String) paramMap.get("objectId");

            // get Business group of current user
            String strBusinessGroupOfCurrentUser = PersonUtil.getDefaultOrganization(context, context.getUser());
            StringList slBusSelects = new StringList(DomainConstants.SELECT_ID);

            MapList mlList = DomainObject.findObjects(context, TYPE_ORGANIZATION, TigerConstants.VAULT_ESERVICEPRODUCTION, "name==\'" + strBusinessGroupOfCurrentUser.concat("\'"), slBusSelects);

            if (!mlList.isEmpty()) {
                Iterator it = mlList.iterator();
                Map mTempMap = (Map) it.next();

                String strBGObjectId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                DomainRelationship.connect(context, strBGObjectId, TigerConstants.RELATIONSHIP_PSS_BUSINESSGROUP, strObjectId, true);
            } else {
                String strErrorMessage = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "PSS_emxComponents.BusinessGroup.NotFoundErrorMessage");
                strErrorMessage = MessageFormat.format(strErrorMessage, strBusinessGroupOfCurrentUser);
                throw new RuntimeException(strErrorMessage);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectBGToOEMGroup: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * Added this function to get ExcludedOIds. this StringList returns list for Exclude ids
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    @SuppressWarnings("rawtypes")
    public static Object excludeConnectedObjects(Context context, String[] args) throws Exception {
        StringList excludeOID = new StringList();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) paramMap.get("objectId");
            String strRelationship = (String) paramMap.get("srcDestRelName");
            String strFieldtype = (String) paramMap.get("field_actual");
            strFieldtype = strFieldtype.replace("TYPES=", "");
            strFieldtype = strFieldtype.trim();

            StringTokenizer strToken = new StringTokenizer(strFieldtype, ",");
            int i = 0;
            String strTypeArr[] = new String[strToken.countTokens()];
            StringBuffer strtype = new StringBuffer();
            int k = strToken.countTokens();
            while (strToken.hasMoreTokens()) {
                strTypeArr[i] = strToken.nextToken();
                String[] temp = strTypeArr[i].split(":");

                strtype.append(PropertyUtil.getSchemaProperty(context, temp[0]));
                if (k != ++i) {
                    strtype.append(",");
                }
            }
            DomainObject domainObject = new DomainObject(objectId);
            StringList slSelectList = new StringList();
            slSelectList.add(DomainConstants.SELECT_ID);

            domainObject.open(context);
            String strRel = PropertyUtil.getSchemaProperty(context, strRelationship);

            MapList mlList = domainObject.getRelatedObjects(context, strRel, strtype.toString(), slSelectList, null, true, true, (short) 1, null, null, 0);
            if (mlList.size() > 0) {
                Iterator itr = mlList.iterator();
                Map map;
                while (itr.hasNext()) {
                    map = (Map) itr.next();
                    excludeOID.add((String) map.get(DomainConstants.SELECT_ID));
                }
                excludeOID.add(objectId);
            }
            return excludeOID;
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in excludeConnectedObjects: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }
    }

    /**
     * This Method is for getting Common data for all tables
     * @param context
     * @param args
     * @throws Exception
     */

    public StringList getULSRelationshipConstants() throws Exception {
        try {
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            slObjSelectStmts.addElement(DomainConstants.SELECT_CURRENT);
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            slObjSelectStmts.addElement(DomainConstants.SELECT_DESCRIPTION);
            slObjSelectStmts.addElement(DomainConstants.SELECT_TYPE);
            return slObjSelectStmts;
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getULSRelationshipConstants: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }

    }

    /**
     * This method is used to display the parent of OEM i.e OEM Group in table
     * @param context
     * @param args
     * @throws Exception
     */

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getOEMULSData(Context context, String[] args) throws Exception {

        // MapList mlList = new MapList();
        MapList mlULSList = new MapList();
        // HashMap programMap = new HashMap();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, strObjectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();
            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDENGINE);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typePattern.addPattern(TigerConstants.TYPE_PSS_ENGINE);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null, null);

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlULSList.add(mTempMap);
                }

            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getOEMULSData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return mlULSList;
    }

    /**
     * This method is used to display the connected PROGRAM-PROJECT to OEM and Vehicle shows in table
     * @param context
     * @param args
     * @throws Exception
     */

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getOEMProgProjData(Context context, String[] args) throws Exception {

        // MapList mlList = new MapList();
        MapList mlProgProjList = new MapList();
        // HashMap programMap = new HashMap();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, strObjectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();
            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            String curLevel;

            String strProgramProject = DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT);
            String strSOPDate = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_SOPDate"));
            String strComplexity = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_Complexity"));
            slObjSelectStmts.addElement(strProgramProject);
            slObjSelectStmts.addElement(strSOPDate);
            slObjSelectStmts.addElement(strComplexity);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End
            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlProgProjList.add(mTempMap);
                }

            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getOEMProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return mlProgProjList;
    }

    /**
     * This method is used to display the connected VEHICLE to OEM in table
     * @param context
     * @param args
     * @throws Exception
     */

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getOEMVehicleData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, strObjectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();

            String strCustomerCode = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_CustomerCode"));
            slObjSelectStmts.addElement(strCustomerCode);
            String strCommercialName = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_CommercialName"));
            slObjSelectStmts.addElement(strCommercialName);
            String strCarClass = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_CarClass"));
            slObjSelectStmts.addElement(strCarClass);
            String strChassis = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_Chassis"));
            slObjSelectStmts.addElement(strChassis);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null, null);

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlVehicleList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlVehicleList.add(mTempMap);
                }
            }
            return mlVehicleList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getOEMVehicleData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This method is to display connected OEM , OEM Group and Platform Indirectly to the RnDCenter
     * @param context
     * @param args
     * @throws Exception
     **/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getRnDCenterULSData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, strObjectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();
            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            String curLevel;
            // Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY);
            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
            // relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);

            // Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_OEMGROUP);
            // typePattern.addPattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typeList.addPattern(TigerConstants.TYPE_PSS_OEM);
            typeList.addPattern(TigerConstants.TYPE_PSS_PLATFORM);
            StringList slUniqueId = new StringList();
            StringList slProgProjId = domainObj.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY + "].from.id");
            MapList mlULSList = new MapList();

            for (int k = 0; k < slProgProjId.size(); k++) {

                String strConnectedProgProjId = (String) slProgProjId.get(k);
                DomainObject domainProgprojObj = DomainObject.newInstance(context, strConnectedProgProjId);

                MapList mlList = domainProgprojObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship // pattern
                        typePattern.getPattern(), // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 3, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        typeList, null, null, null);
                // Modify for TIGTK-6955:ULS:PKH:Start
                mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
                // Modify for TIGTK-6955:ULS:PKH:End
                Iterator i = mlList.iterator();
                while (i.hasNext()) {
                    Map mTempMap = (Map) i.next();
                    curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                    mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                    String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                    if (!slUniqueId.contains(strObjId)) {
                        slUniqueId.addElement(strObjId);
                        if (!("1").equalsIgnoreCase(curLevel))
                            mTempMap.put("disableSelection", "True");
                        // TIGTK-5925 :ULS RFC-109: By AG: Ends
                        mlULSList.add(mTempMap);
                    }
                }
            }
            return mlULSList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getRnDCenterULSData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This method is to display connected Program-Project to RnDCenter in table
     * @param context
     * @param args
     * @throws Exception
     **/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getRnDCenterProgProjData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, strObjectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            MapList mlList = domainObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY, // relationship // pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typeList, null, null, null);

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlProgProjList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlProgProjList.add(mTempMap);
                }
            }
            return mlProgProjList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getRnDCenterProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This method is to display connected vehicle indirectly to RnDCenter in table
     * @param context
     * @param args
     * @throws Exception
     **/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getRnDCenterVehicleData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, strObjectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();
            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            String curLevel;

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);
            typeList.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship // pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePattern, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End
            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlVehicleList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlVehicleList.add(mTempMap);
                }
            }
            return mlVehicleList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getRnDCenterVehicleData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This Method is to display connected OEM , Engine and Platform to OEMGroup in table
     * @param context
     * @param args
     * @throws Exception
     **/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getOEMGroupULSData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            String strSelectedType = domainObj.getInfo(context, DomainConstants.SELECT_TYPE);
            MapList mlULSList = new MapList();

            if (strSelectedType.equals(TigerConstants.TYPE_PSS_OEMGROUP)) {
                StringList slObjSelectStmts = getULSRelationshipConstants();

                Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);
                relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORM);
                relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDENGINE);
                relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDENGINE);

                Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PLATFORM);
                typePattern.addPattern(TigerConstants.TYPE_PSS_ENGINE);
                typePattern.addPattern(TigerConstants.TYPE_PSS_OEM);

                Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_ENGINE);
                typePostPattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);
                typePostPattern.addPattern(TigerConstants.TYPE_PSS_OEM);

                // TIGTK-5925 :ULS RFC-109: By AG: Starts
                StringList slSelectRelStmts = new StringList(2);
                slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

                String curLevel;

                MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship // pattern
                        typePattern.getPattern(), // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 2, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        typePostPattern, null, null, null, null);
                // Modify for TIGTK-6955:ULS:PKH:Start
                mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
                // Modify for TIGTK-6955:ULS:PKH:End
                Iterator i = mlList.iterator();
                StringList slUniqueId = new StringList();
                while (i.hasNext()) {
                    Map mTempMap = (Map) i.next();
                    curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                    mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                    String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                    if (!slUniqueId.contains(strObjId)) {
                        slUniqueId.addElement(strObjId);
                        if (!("1").equalsIgnoreCase(curLevel))
                            mTempMap.put("disableSelection", "True");
                        // TIGTK-5925 :ULS RFC-109: By AG: Ends
                        mlULSList.add(mTempMap);
                    }
                }
            } else {
                StringList slObjSelectStmts = getULSRelationshipConstants();

                Pattern objectPattern = new Pattern(TigerConstants.TYPE_PSS_OEMGROUP);
                Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORM);
                StringList slSelectRelStmts = new StringList(1);
                slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                mlULSList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship// pattern
                        objectPattern.getPattern(), // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, (short) 0); // relationship Where Clause
            }
            return mlULSList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getOEMGroupULSData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This Method is to display connected Program -Project to OEMGroup in table
     * @param context
     * @param args
     * @throws Exception
     **/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getOEMGroupProgProjData(Context context, String[] args) throws Exception {
        MapList mlProgProjList = new MapList();

        try {
            MapList mlList = getProgProjData(context, args);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End
            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            String curLevel;
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);

                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlProgProjList.add(mTempMap);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getOEMGroupProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return mlProgProjList;
    }

    /**
     * This Method is to get connected Program-Project to OEMGroup
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public MapList getProgProjData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();

            String strProgramProject = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_ProgramProject"));
            String strSOPDate = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_SOPDate"));
            String strComplexity = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_Complexity"));

            slObjSelectStmts.addElement(strProgramProject);
            slObjSelectStmts.addElement(strSOPDate);
            slObjSelectStmts.addElement(strComplexity);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEM);

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);
            // TIGTK-5925 :ULS RFC-109: By AG: Ends

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship // pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 3, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    DomainObject.PAGE_SIZE, // pageSize
                    typePostPattern, null, null, null, null);
            return mlList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This Method is to display connected Vehicle to OEMGroup in table
     * @param context
     * @param args
     * @throws Exception
     **/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getOEMGroupVehicleData(Context context, String[] args) throws Exception {
        MapList mlVehicleList = new MapList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();

            String strCustomerCode = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_CustomerCode"));
            slObjSelectStmts.addElement(strCustomerCode);

            String strCommercialName = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_CommercialName"));
            slObjSelectStmts.addElement(strCommercialName);

            String strCarClass = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_CarClass"));
            slObjSelectStmts.addElement(strCarClass);

            String strChassis = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_Chassis"));
            slObjSelectStmts.addElement(strChassis);
            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEMGROUPTOVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_OEM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            String curLevel;

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship // pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 3, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    DomainObject.PAGE_SIZE, // pageSize
                    typePostPattern, null, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlVehicleList.add(mTempMap);
                }

            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getOEMGroupVehicleData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return mlVehicleList;
    }

    /**
     * This method returns only those Program-Projects which are not connected to any other Platform,OEM,OEM GROUP
     * @param context
     * @param args
     * @return Object
     * @throws Exception
     *             if fails
     */
    // Modified for TIGTK-8877 - 7-13-2017 - PTE - START
    public Object excludeConnectedProgProj(Context context, String[] args) throws Exception {

        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            StringBuffer sbWhere = new StringBuffer();
            if (domainObj.isKindOf(context, TigerConstants.TYPE_PSS_OEM)) {
                sbWhere.append("to[");
                sbWhere.append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEM);
                sbWhere.append("] == True");
            }
            if (domainObj.isKindOf(context, TigerConstants.TYPE_PSS_OEMGROUP)) {
                sbWhere.append("to[");
                sbWhere.append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
                sbWhere.append("] == True");
            }
            if (domainObj.isKindOf(context, TigerConstants.TYPE_PSS_PLATFORM)) {
                sbWhere.append("to[");
                sbWhere.append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);
                sbWhere.append("] == True");
            }
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            StringList excludeOID = new StringList();

            StringList slSelectable = new StringList();
            slSelectable.add(DomainConstants.SELECT_ID);

            MapList mlReturnList = DomainObject.findObjects(context, typePattern.getPattern(), TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), slSelectable);

            if (mlReturnList.size() > 0) {
                Iterator itr = mlReturnList.iterator();
                Map mTempMap;
                while (itr.hasNext()) {
                    mTempMap = (Map) itr.next();
                    excludeOID.add((String) mTempMap.get(DomainConstants.SELECT_ID));
                }
            }
            // Modified for TIGTK-8877 - 7-13-2017 - PTE - END
            return excludeOID;
        } catch (Exception ex) {
            logger.error("Error in excludeConnectedProgProj: ", ex);
            throw ex;
        }

    }
    // End of method :

    /**
     * This method is to display connected PRODUCT to Engine in table
     * @param context
     * @param args
     * @throws Exception
     **/
    @SuppressWarnings("rawtypes")
    public MapList getEngineProductData(Context context, String[] args) throws Exception {

        final String TYPE_PRODUCT = PropertyUtil.getSchemaProperty(context, "type_Products");
        final String TYPE_HARDWARE_PRODUCT = PropertyUtil.getSchemaProperty(context, "type_HardwareProduct");
        final String TYPE_SERVICE_PRODUCT = PropertyUtil.getSchemaProperty(context, "type_ServiceProduct");
        final String TYPE_SOFTWARE_PRODUCT = PropertyUtil.getSchemaProperty(context, "type_SoftwareProduct");
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domEngine = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            slObjSelectStmts.addElement(DomainConstants.SELECT_CURRENT);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPRODUCT);

            Pattern objectPattern = new Pattern(TYPE_PRODUCT);
            Pattern objectPostPattern = new Pattern(TYPE_HARDWARE_PRODUCT);
            objectPostPattern.addPattern(TYPE_SERVICE_PRODUCT);
            objectPostPattern.addPattern(TYPE_SOFTWARE_PRODUCT);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            MapList mlList = domEngine.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship // pattern
                    objectPattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    objectPostPattern, null, null, null, null); // relationship
            // Where Clause
            return mlList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getEngineProductData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This method is used to display the connected OEM AND OEM GROUP to ENGINE in table
     * @param context
     * @param args
     * @throws Exception
     **/

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getEngineULSData(Context context, String[] args) throws Exception {
        final String TYPE_PSS_OEMGROUP = PropertyUtil.getSchemaProperty(context, "type_PSS_OEMGroup");
        final String TYPE_PSS_OEM = PropertyUtil.getSchemaProperty(context, "type_PSS_OEM");
        final String RELATIONSHIP_ASSIGNED_ENGINE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AssignedEngine");
        final String RELATIONSHIP_CONNECTED_ENGINE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedEngine");
        final String RELATIONSHIP_ASSIGNED_OEM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AssignedOEM");

        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");

            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            String curLevel;

            Pattern relationshipPattern = new Pattern(RELATIONSHIP_ASSIGNED_ENGINE);
            relationshipPattern.addPattern(RELATIONSHIP_CONNECTED_ENGINE);
            relationshipPattern.addPattern(RELATIONSHIP_ASSIGNED_OEM);

            Pattern typePattern = new Pattern(TYPE_PSS_OEM);
            typePattern.addPattern(TYPE_PSS_OEMGROUP);

            Pattern typePostPattern = new Pattern(TYPE_PSS_OEMGROUP);
            typePostPattern.addPattern(TYPE_PSS_OEM);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship
                    // pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End

            Iterator i = mlList.iterator();

            StringList slUniqueId = new StringList();
            MapList mlULSList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);

                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);

                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlULSList.add(mTempMap);
                }
            }
            return mlULSList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getEngineULSData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This method is used to display the connected Program-project to Platform in table
     * @param context
     * @param args
     * @throws Exception
     **/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getPlatformProgProjData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);

            String objectId = (String) programMap.get("objectId");

            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            String curLevel;

            String strProgramProject = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_ProgramProject"));
            String strSOPDate = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_SOPDate"));
            String strComplexity = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_Complexity"));
            slObjSelectStmts.addElement(strProgramProject);
            slObjSelectStmts.addElement(strSOPDate);
            slObjSelectStmts.addElement(strComplexity);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);
            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship
                    // pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End

            Iterator i = mlList.iterator();

            StringList slUniqueId = new StringList();
            MapList mlProgProjList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);

                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);

                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlProgProjList.add(mTempMap);
                }
            }
            return mlProgProjList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getPlatformProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This method is used to display the connected Product to Platform in table
     * @param context
     * @param args
     * @throws Exception
     **/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getPlatformProductData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);

            String objectId = (String) programMap.get("objectId");

            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            String curLevel;

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORMTOPRODUCT);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PRODUCTS);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_HARDWARE_PRODUCT);
            typePostPattern.addPattern(TigerConstants.TYPE_SERVICEPRODUCT);
            typePostPattern.addPattern(TigerConstants.TYPE_SOFTWAREPRODUCT);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship
                    // pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, // Product Display
                    null, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End
            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlProductList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlProductList.add(mTempMap);
                }
            }
            return mlProductList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getPlatformProductData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected OEM ,OEMGroup , Platform of Destination Region
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getDestinationRegionULSData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();
            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            String curLevel;

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typePostPattern.addPattern(TigerConstants.TYPE_PSS_OEM);
            typePostPattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 4, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlULSList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlULSList.add(mTempMap);
                }
            }
            return mlULSList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getDestinationRegionULSData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected Vehicle of Destination Region
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getDestinationRegionVehicleData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            String curLevel;

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlVehicleList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlVehicleList.add(mTempMap);
                }
            }
            return mlVehicleList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getDestinationRegionVehicleData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected Program-Project of Destination Region
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getDestinationRegionProgProjData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();
            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null, null);

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlProgProjList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlProgProjList.add(mTempMap);
                }
            }
            return mlProgProjList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getDestinationRegionProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected OEMGroup ,Platform of Division
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getDivisionULSData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            String curLevel;

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_RESPONSIBLEDIVISION);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typeList.addPattern(TigerConstants.TYPE_PSS_OEM);
            typeList.addPattern(TigerConstants.TYPE_PSS_PLATFORM);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 4, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typeList, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End
            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlULSList = new MapList();

            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlULSList.add(mTempMap);
                }
            }
            return mlULSList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getDivisionULSData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected Program-Project of Division
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getDivisionProgProjData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            MapList mlList = domainObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RESPONSIBLEDIVISION, // relationship pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typeList, null, null, null);

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlProgProjList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlProgProjList.add(mTempMap);
                }
            }

            return mlProgProjList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getDivisionProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected Vehicle of Division
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getDivisionVehicleData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_RESPONSIBLEDIVISION);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);
            typeList.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePattern, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End
            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlVehicleList = new MapList();
            String curLevel;
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlVehicleList.add(mTempMap);
                }
            }
            return mlVehicleList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getDivisionVehicleData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected OEM , OEMGroup ,Platform of Plant
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getPlantULSData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();
            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typeList.addPattern(TigerConstants.TYPE_PSS_OEM);
            typeList.addPattern(TigerConstants.TYPE_PSS_PLATFORM);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 4, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typeList, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End
            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlULSList = new MapList();
            String curLevel;
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlULSList.add(mTempMap);
                }
            }
            return mlULSList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getPlantULSData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected Program-Project of Plant
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getPlantProgProjData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();
            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            MapList mlList = domainObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY, // relationship pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typeList, null, null, null);

            Iterator i = mlList.iterator();
            MapList mlProgProjList = new MapList();
            StringList slUniqueId = new StringList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlProgProjList.add(mTempMap);
                }
            }
            return mlProgProjList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getPlantProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected Vehicle of Plant
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getPlantVehicleData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);
            typeList.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePattern, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlVehicleList = new MapList();
            String curLevel;
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlVehicleList.add(mTempMap);
                }
            }
            return mlVehicleList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getPlantVehicleData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected OEM and OEMGroup of Vehicle
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getVehicleULSData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();
            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_OEM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEMGROUP);

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_OEM);
            typePostPattern.addPattern(TigerConstants.TYPE_PSS_OEMGROUP);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlULSList = new MapList();
            String curLevel;
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlULSList.add(mTempMap);
                }
            }
            return mlULSList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getVehicleULSData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected Program-Project of Vehicle
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getVehicleProgProjData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();

            String strProgramProject = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_ProgramProject"));
            String strSOPDate = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_SOPDate"));
            String strComplexity = DomainObject.getAttributeSelect(PropertyUtil.getSchemaProperty(context, "attribute_PSS_Complexity"));
            slObjSelectStmts.addElement(strProgramProject);
            slObjSelectStmts.addElement(strSOPDate);
            slObjSelectStmts.addElement(strComplexity);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlProgProjList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlProgProjList.add(mTempMap);
                }
            }
            return mlProgProjList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getVehicleProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To display connected Products of Vehicle
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getVehicleProductData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);

            String objectId = (String) programMap.get("objectId");

            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();
            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_VED);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PRODUCTS);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VED);

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_HARDWARE_PRODUCT);
            typePostPattern.addPattern(TigerConstants.TYPE_SERVICEPRODUCT);
            typePostPattern.addPattern(TigerConstants.TYPE_SOFTWAREPRODUCT);
            typePostPattern.addPattern(TigerConstants.TYPE_PSS_VED);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End
            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlProductList = new MapList();
            String curLevel;
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put(RECURSION_LEVEL, RECURSION_VALUE);
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlProductList.add(mTempMap);
                }
            }
            return mlProductList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getVehicleProductData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To show data of ULS on Program-Project
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getProgramProjectULSData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");

            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();

            // TIGTK-5925 :ULS RFC-109: By AG: Starts
            StringList slSelectRelStmts = new StringList(2);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.add(DomainRelationship.SELECT_LEVEL);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEM);
            // TIGTK-7917:27/07/2017 :PTE:START
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
            // TIGTK-7917:27/07/2017 :PTE:END
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PLATFORM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);
            // TIGTK-7917:27/07/2017 :PTE:START
            typePattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            // TIGTK-7917:27/07/2017 :PTE:END

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_OEMGROUP);
            typePostPattern.addPattern(TigerConstants.TYPE_PSS_OEM);
            typePostPattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 3, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            // Modify for TIGTK-6955:ULS:PKH:Start
            mlList.sort(DomainRelationship.SELECT_LEVEL, "ascending", "String");
            // Modify for TIGTK-6955:ULS:PKH:End
            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlULSList = new MapList();
            String curLevel;
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                curLevel = (String) mTempMap.get(DomainRelationship.SELECT_LEVEL);
                mTempMap.put("level", "1");

                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);

                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    if (!("1").equalsIgnoreCase(curLevel))
                        mTempMap.put("disableSelection", "True");
                    // TIGTK-5925 :ULS RFC-109: By AG: Ends
                    mlULSList.add(mTempMap);
                }

                // Always perform below remove operation at the end, after mlULSList is freezed
                // Display OEM Group only connected via
                DomainObject domObj = DomainObject.newInstance(context, strObjId);
                if (domObj.isKindOf(context, TigerConstants.TYPE_PSS_OEMGROUP)) {
                    boolean checkVisibility = false;

                    // OEM Group -> Program-Project
                    checkVisibility = checkOEMGroupProgramProject(context, objectId, domObj);

                    // OEM Group -> Platform -> Program-Project
                    if (!checkVisibility) {
                        checkVisibility = checkOEMGroupPlatformProgramProject(context, objectId, domObj);
                    }

                    // OEM Group -> OEM -> Vehicle -> Program-Project
                    if (!checkVisibility) {
                        checkVisibility = checkOEMGroupOEMVehicleProgramProject(context, objectId, domObj);
                    }
                    // TIGTK-7917:27/07/2017 :PTE:START
                    // OEM Group -> Program-Project -> Program-Project
                    if (!checkVisibility) {
                        checkVisibility = checkOEMGroupProgProjConnectedToChildProgProj(context, objectId, domObj);
                    }
                    // TIGTK-7917:27/07/2017 :PTE:END
                    if (!checkVisibility) {
                        mlULSList.remove(mTempMap);
                    }
                } // TIGTK-7917:27/07/2017 :PTE:START
                else if (domObj.exists(context) && domObj.isKindOf(context, TigerConstants.TYPE_PSS_PLATFORM)) {
                    boolean checkVisibility = false;
                    // Platform -> Program-Project
                    checkVisibility = checkPlatformProgramProject(context, objectId, domObj);
                    if (!checkVisibility) {
                        mlULSList.remove(mTempMap);
                    }
                } else if (domObj.isKindOf(context, TigerConstants.TYPE_PSS_OEM)) {
                    boolean checkVisibility = false;

                    // OEM-> Program-Project
                    checkVisibility = checkOEMProgramProject(context, objectId, domObj);

                    // OEM -> Vehicle -> Program-Project
                    if (!checkVisibility) {
                        checkVisibility = checkOEMVehicleProgramProject(context, objectId, domObj);
                    }
                    // OEM -> Program-Project -> Program-Project
                    if (!checkVisibility) {
                        checkVisibility = checkOEMProgProjConnectedToChildProgProj(context, objectId, domObj);
                    }

                    if (!checkVisibility) {
                        mlULSList.remove(mTempMap);
                    }
                }
                // TIGTK-7917:27/07/2017 :PTE:END
            }

            return mlULSList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgramProjectULSData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * @param context
     * @param objectId
     * @param domOEMGroupObj
     * @return
     * @throws Exception
     */
    public boolean checkOEMGroupProgramProject(Context context, String objectId, DomainObject domOEMGroupObj) throws Exception {
        boolean checkVisibility = false;

        Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
        Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        StringList slObjSelectStmts = new StringList(1);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

        StringList slSelectRelStmts = new StringList(1);

        MapList mpListProgProj = domOEMGroupObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                typePattern.getPattern(), // object pattern
                slObjSelectStmts, // object selects
                slSelectRelStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 0, // recursion level
                null, // object where clause
                null, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                typePostPattern, null, null, null, null);

        if (mpListProgProj.size() > 0) {
            String strProgProjObjId = null;
            for (int i = 0; i < mpListProgProj.size(); i++) {
                strProgProjObjId = (String) ((Map) mpListProgProj.get(i)).get(DomainConstants.SELECT_ID);

                if (objectId.equals(strProgProjObjId)) {
                    checkVisibility = true;
                }
            }
        }

        return checkVisibility;
    }

    /**
     * @param context
     * @param objectId
     * @param domOEMGroupObj
     * @return
     * @throws Exception
     */
    public boolean checkOEMGroupPlatformProgramProject(Context context, String objectId, DomainObject domOEMGroupObj) throws Exception {
        boolean checkVisibility = false;

        Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORM);
        relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);

        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PLATFORM);
        typePattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        StringList slObjSelectStmts = new StringList(1);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

        StringList slSelectRelStmts = new StringList(1);

        MapList mpListProgProj = domOEMGroupObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                typePattern.getPattern(), // object pattern
                slObjSelectStmts, // object selects
                slSelectRelStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 0, // recursion level
                null, // object where clause
                null, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                typePostPattern, null, null, null, null);

        if (mpListProgProj.size() > 0) {
            String strProgProjObjId = null;
            for (int i = 0; i < mpListProgProj.size(); i++) {
                strProgProjObjId = (String) ((Map) mpListProgProj.get(i)).get(DomainConstants.SELECT_ID);

                if (objectId.equals(strProgProjObjId)) {
                    checkVisibility = true;
                }
            }
        }

        return checkVisibility;
    }

    /**
     * @param context
     * @param objectId
     * @param domOEMGroupObj
     * @return
     * @throws Exception
     */
    public boolean checkOEMGroupOEMVehicleProgramProject(Context context, String objectId, DomainObject domOEMGroupObj) throws Exception {
        boolean checkVisibility = false;

        Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);
        relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);
        relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);

        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_OEM);
        typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);
        typePattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        StringList slObjSelectStmts = new StringList(1);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

        StringList slSelectRelStmts = new StringList(1);

        MapList mpListProgProj = domOEMGroupObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                typePattern.getPattern(), // object pattern
                slObjSelectStmts, // object selects
                slSelectRelStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 0, // recursion level
                null, // object where clause
                null, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                typePostPattern, null, null, null, null);

        if (mpListProgProj.size() > 0) {
            String strProgProjObjId = null;
            for (int i = 0; i < mpListProgProj.size(); i++) {
                strProgProjObjId = (String) ((Map) mpListProgProj.get(i)).get(DomainConstants.SELECT_ID);

                if (objectId.equals(strProgProjObjId)) {
                    checkVisibility = true;
                }
            }
        }

        return checkVisibility;
    }

    /**
     * To show data of Product on Program-Project
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getProgramProjectProductData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PRODUCTS);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_HARDWARE_PRODUCT);
            typePattern.addPattern(TigerConstants.TYPE_SERVICEPRODUCT);
            typePattern.addPattern(TigerConstants.TYPE_SOFTWAREPRODUCT);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePattern, null, null, null);

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlProductList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put("level", "1");
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlProductList.add(mTempMap);
                }
            }
            return mlProductList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgramProjectProductData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To show data of Location of program-Project
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getProgramProjectLocationData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_RESPONSIBLEDIVISION);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_PLANT);
            typeList.addPattern(TigerConstants.TYPE_PSS_RnDCENTER);
            typeList.addPattern(TigerConstants.TYPE_PSS_DIVISION);
            typeList.addPattern(TigerConstants.TYPE_PSS_DESTINATIONREGION);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PLANT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_RnDCENTER);
            typePattern.addPattern(TigerConstants.TYPE_PSS_DIVISION);
            typePattern.addPattern(TigerConstants.TYPE_PSS_DESTINATIONREGION);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePattern, null, null, null);

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlLocationList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put("level", "1");
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlLocationList.add(mTempMap);
                }
            }
            return mlLocationList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgramProjectLocationData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To show data of Program-Project on Program-Project
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @modified by : Priyanka Salunke for RFC-095 on Date : 19/10/2016
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getProgramProjectProgProjData(Context context, String[] args) throws Exception {
        boolean bToDirection = false;
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            // Added for RFC-095 By Priyanka Salunke On Date : 19/10/2016 : START
            String strType = domainObj.getInfo(context, DomainConstants.SELECT_TYPE);
            if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_PROGRAMPROJECT)) {
                bToDirection = true;
            }
            // Added for RFC-095 By Priyanka Salunke On Date : 19/10/2016 : END
            StringList slObjSelectStmts = getULSRelationshipConstants();

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_GLOBALLOCALPROGRAMPROJECT);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_NAME);
            slSelectRelStmts.addElement(DomainRelationship.SELECT_FROM_ID);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    bToDirection, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePattern, null, null, null);

            Iterator i = mlList.iterator();
            MapList mlProgProjList = new MapList();
            Map mUniqueId = new HashMap();
            while (i.hasNext()) {

                Map mTempMap = (Map) i.next();
                mTempMap.put("level", "1");
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                String strRelName = (String) mTempMap.get(DomainConstants.SELECT_RELATIONSHIP_NAME);
                String strKey = strRelName + strObjId;
                if (!mUniqueId.containsKey(strKey)) {
                    mUniqueId.put(strKey, "");
                    mlProgProjList.add(mTempMap);
                }
            }
            return mlProgProjList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgramProjectProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To show data of Vehicle on Program-Project
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getProgramProjectVehicleData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();
            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_VEHICLE);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePattern, null, null, null);

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlVehicleDataList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put("level", "1");
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlVehicleDataList.add(mTempMap);
                }
            }
            return mlVehicleDataList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgramProjectVehicleData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To show data of Workspace on Program-Project
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getProgramProjectWorkspaceData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            DomainObject domainObj = DomainObject.newInstance(context, objectId);

            StringList slObjSelectStmts = getULSRelationshipConstants();

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDWORKSPACE);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern typeList = new Pattern(TigerConstants.TYPE_PROJECT);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PROJECT);

            MapList mlList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typeList.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePattern, null, null, null);

            Iterator i = mlList.iterator();
            StringList slUniqueId = new StringList();
            MapList mlWorkspaceDataList = new MapList();
            while (i.hasNext()) {
                Map mTempMap = (Map) i.next();
                mTempMap.put("level", "1");
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlWorkspaceDataList.add(mTempMap);
                }
            }
            return mlWorkspaceDataList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgramProjectWorkspaceData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * To show data of CR,CO,Issue,MCO,CN
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @Modified By : Priyanka Salunke @date : 17-March-2017
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getConnectedPCMData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            // PCM1.1 : TIGTK-4569 : Priyanka Salunke : 17-March-2017 : Start
            String strObjectId = (String) programMap.get("objectId");
            String strObjectType = (String) programMap.get("fromCommand");

            DomainObject domProgramProjectObj = DomainObject.newInstance(context, strObjectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern typePattern = new Pattern("");
            Pattern typePostPattern = new Pattern("");
            Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);
            ;
            if (strObjectType.equals("CR")) {
                typePostPattern.addPattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
                typePattern.addPattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            } else if (strObjectType.equals("CO")) {
                typePostPattern.addPattern(TigerConstants.TYPE_PSS_CHANGEORDER);
                typePattern.addPattern(TigerConstants.TYPE_PSS_CHANGEORDER);
            } else if (strObjectType.equals("Issue")) {
                // TS-185 Issue-Create From SLC and Change Management -- TIGTK -6312 : 09/08/2017 : KWagh
                typePostPattern.addPattern(TigerConstants.TYPE_PSS_ISSUE);
                typePattern.addPattern(TigerConstants.TYPE_PSS_ISSUE);
                // TS-185 Issue-Create From SLC and Change Management -- TIGTK -6312 : 09/08/2017 : KWagh
            } else if (strObjectType.equals("MCO")) {
                typePostPattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
                typePattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
            } else if (strObjectType.equals("CN")) {
                typePostPattern.addPattern(TigerConstants.TYPE_PSS_CHANGENOTICE);
                typePattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
                typePattern.addPattern(TigerConstants.TYPE_PSS_CHANGENOTICE);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_RELATEDCN);
            }
            // Get Connected PCM Objects of Program Project
            MapList mlConnectedPCMData = domProgramProjectObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    DomainConstants.EMPTY_STRING, // object where clause
                    DomainConstants.EMPTY_STRING, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            // PCM1.1 : TIGTK-4569 : Priyanka Salunke : 17-March-2017 : END
            Iterator itr = mlConnectedPCMData.iterator();
            StringList slUniqueId = new StringList();
            MapList mlPCMDataList = new MapList();
            while (itr.hasNext()) {
                Map mTempMap = (Map) itr.next();
                mTempMap.put("level", "1");
                String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueId.contains(strObjId)) {
                    slUniqueId.addElement(strObjId);
                    mlPCMDataList.add(mTempMap);
                }
            }
            return mlPCMDataList;

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getConnectedPCMData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }
    // End getConnectedPCMData() : Priyanka Salunke :TIGTK-4569

    /**
     * PCM Tab of Program-Project To show data of member
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public MapList getProgramProjectPersonData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");

            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS);

            Pattern typePattern = new Pattern(TYPE_PERSON);

            Pattern typePostPattern = new Pattern(TYPE_PERSON);

            MapList mlPerDataList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            return mlPerDataList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProgramProjectPersonData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * This Method is for find ExcludeOIDs OEMGroup , OEM , Platform not connected to ProgramProject
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    // Modified for TIGTK-8877 - 7-13-2017 - PTE - START
    @SuppressWarnings("rawtypes")
    public Object excludeConnectedULSObjects(Context context, String[] args) throws Exception {

        try {
            StringList excludeOID = new StringList();
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) paramMap.get("objectId");
            String strFieldtype = (String) paramMap.get("field_actual");
            strFieldtype = strFieldtype.replace("TYPES=", "");
            strFieldtype = strFieldtype.trim();
            StringTokenizer strToken = new StringTokenizer(strFieldtype, ",");
            int i = 0;
            String strTypeArr[] = new String[strToken.countTokens()];
            StringBuffer strtype = new StringBuffer();
            int k = strToken.countTokens();
            while (strToken.hasMoreTokens()) {
                strTypeArr[i] = strToken.nextToken();
                String[] temp = strTypeArr[i].split(":");

                strtype.append(PropertyUtil.getSchemaProperty(context, temp[0]));
                if (k != ++i) {
                    strtype.append(",");
                }
            }
            DomainObject domainObject = new DomainObject(objectId);
            StringList slSelectList = new StringList();
            slSelectList.add(DomainConstants.SELECT_ID);

            domainObject.open(context);
            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEM);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);

            MapList mlList = domainObject.getRelatedObjects(context, relationshipPattern.getPattern(), strtype.toString(), slSelectList, null, true, true, (short) 1, null, null, 0);
            if (mlList.size() > 0) {
                Iterator itr = mlList.iterator();
                Map map;
                while (itr.hasNext()) {
                    map = (Map) itr.next();
                    excludeOID.add((String) map.get(DomainConstants.SELECT_ID));
                }
                excludeOID.add(objectId);
            }
            return excludeOID;
            // Modified for TIGTK-8877 - 7-13-2017 - PTE - END
        } catch (Exception ex) {
            logger.error("Error in excludeConnectedULSObjects: ", ex);
            throw ex;
        }
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    /**
     * This Method is used to check if any PSS_ProgramProject objects are connected using "PSS_SubProgramProject" relationship. It also checks if "CORE Team" value is properly assigned to
     * PSS_ProgramProject object
     * @param context
     * @param args
     * @param args0
     *            -- PSS_ProgramProject object OID
     * @return -- int -- Status of "PSS_ProgramProject" object promotion from "Active" to next state
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int promoteCheckRolePosition(Context context, String[] args) throws Exception {
        int iReturnFlag = 0;
        try {
            String[] strArgs = new String[2];
            strArgs[0] = args[0];
            strArgs[1] = "CURRENT_STATE";
            DomainObject domProgramProjectObject = DomainObject.newInstance(context, args[0]);
            StringList slProgProjRel = domProgramProjectObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "].to.id");
            if (slProgProjRel.size() > 0) {
                iReturnFlag = 1;
                if (iReturnFlag == 1) {
                    String strParentProgramProjectConnectedMsg = EnoviaResourceBundle.getFrameworkStringResourceProperty(context,
                            "emxFramework.PSS_ProgramProject.PSS_ProgramProjectParentConnected.AlertMessage", context.getLocale());
                    MqlUtil.mqlCommand(context, "notice $1", strParentProgramProjectConnectedMsg);
                }
            } else {
                boolean bResult = checkLeadRolePresentOnPP(context, strArgs);
                if (bResult)
                    iReturnFlag = 1;
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in promoteCheckRolePosition: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return iReturnFlag;
    }

    /**
     * This Method is for find ExcludeOIDs OEM not connected to OEMGroup
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public Object excludeConnectedOEM(Context context, String[] args) throws Exception {
        StringList excludeOID = new StringList();

        StringList slSelectable = new StringList();
        slSelectable.add(DomainConstants.SELECT_ID);
        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_OEM);

        StringBuffer sbWhere = new StringBuffer();
        sbWhere.append("to[");
        sbWhere.append(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);
        sbWhere.append("] == True");

        MapList mlReturnList = DomainObject.findObjects(context, typePattern.getPattern(), TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), slSelectable);

        try {
            if (mlReturnList.size() > 0) {
                Iterator itr = mlReturnList.iterator();
                Map mTempMap;
                while (itr.hasNext()) {
                    mTempMap = (Map) itr.next();
                    excludeOID.add((String) mTempMap.get(DomainConstants.SELECT_ID));
                }
            }
            return excludeOID;

        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in excludeConnectedOEM: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }
    }

    /**
     * To include Members connected to a Plant
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public StringList plantConnectedMembers(Context context, String[] args) throws Exception {
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strEmxTableRowId = (String) paramMap.get("emxTableRowId");
            String strProgProjId = (String) paramMap.get("objectId");
            StringList slTokens = FrameworkUtil.split(strEmxTableRowId, "|");

            String strPlantId = "";
            strPlantId = (String) slTokens.get(1);

            DomainObject domPlantObj = DomainObject.newInstance(context, strPlantId);
            DomainObject domProgProjObj = DomainObject.newInstance(context, strProgProjId);

            String strPlantName = domPlantObj.getInfo(context, DomainConstants.SELECT_NAME);
            StringList slObjSelects = new StringList();
            StringList slRelSelects = new StringList();
            slObjSelects.add(DomainConstants.SELECT_ID);
            slRelSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "]");

            StringList slPlantMembers = domPlantObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.id");

            MapList mlProgProjMembers = domProgProjObj.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ, // relationshipPattern
                    TYPE_PERSON, // typePattern
                    slObjSelects, // objectSelects
                    slRelSelects, // relationshipSelects
                    false, // getTo
                    true, // getFrom
                    (short) 1, // recurseToLevel
                    null, // objectWhere
                    null, // relationshipWhere
                    (short) 0, // limit
                    null, // includeType
                    null, // includeRelationship
                    null); // includeMap

            if (!mlProgProjMembers.isEmpty()) {
                for (int itr = 0; itr < mlProgProjMembers.size(); itr++) {
                    Map mProgProjMember = (Map) mlProgProjMembers.get(itr);
                    String strMemId = (String) mProgProjMember.get(DomainConstants.SELECT_ID);
                    String strPlantNameProgProj = (String) mProgProjMember.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "]");
                    // TIGTK-5159 | 24/03/2014 | Harika Varanasi : Starts
                    if (slPlantMembers.contains(strMemId) && strPlantNameProgProj.contains(strPlantName)) {
                        slPlantMembers.remove(strMemId);
                    }
                    // TIGTK-5159 | 24/03/2014 | Harika Varanasi : Ends
                }
            }

            return slPlantMembers;
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * Members Tab of Program-Project - Displays Member connected to the Program Project To show Members connected to Plants
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public MapList getMembersConnectedProgProjData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (HashMap) JPO.unpackArgs(args);

            String strProgProjId = (String) programMap.get("objectId");
            StringList slObjSelects = new StringList();
            StringList slRelSelects = new StringList();
            slObjSelects.add(DomainConstants.SELECT_ID);
            slRelSelects.add(DomainRelationship.SELECT_ID);
            DomainObject domProgProjObj = DomainObject.newInstance(context, strProgProjId);
            MapList mlProgProjMembers = domProgProjObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ, TYPE_PERSON, slObjSelects, slRelSelects, false, true,
                    (short) 1, null, null, (short) 0, null, null, null);
            return mlProgProjMembers;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getMembersConnectedProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * Plants Tab of Program-Project To show Plants connected to Program-Project
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public MapList getPlantConnectedProgProjData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);

            String strProgProjId = (String) programMap.get("objectId");
            DomainObject domProgProjObj = DomainObject.newInstance(context, strProgProjId);

            StringList slSelectStmts = new StringList();
            StringList slRelStmts = new StringList();

            slSelectStmts.add(DomainConstants.SELECT_ID);
            slRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

            // MapList containing the Plants connected with Program-Project using PSS_ProductionEntity relationship

            MapList mlConnectedPlantList = domProgProjObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY, // relationship
                    TigerConstants.TYPE_PSS_PLANT, // type
                    slSelectStmts, // object selects
                    slRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, null, 0);
            return mlConnectedPlantList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getPlantConnectedProgProjData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * People Tab of Plant To show data of member
     * @param context
     * @param args
     * @return Owner : Sneha I
     * @throws Exception
     */

    @SuppressWarnings("rawtypes")
    public MapList getPlantPersonData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");

            DomainObject domainObjPlant = DomainObject.newInstance(context, objectId);

            StringList lstSelectStmts = new StringList();
            StringList lstRelStmts = new StringList();

            lstSelectStmts.add(DomainConstants.SELECT_ID);
            lstRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS);
            Pattern typePattern = new Pattern(TYPE_PERSON);
            Pattern typePostPattern = new Pattern(TYPE_PERSON);

            MapList mlPerDataList = domainObjPlant.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    lstSelectStmts, // object selects
                    lstRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);
            return mlPerDataList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getPlantPersonData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    // ULS TIGTK-2914 | RFC-078 : 02/09/16 : Swapnil Patil : START
    /**
     * @description : This method is use to get related ULS Objects of selected product.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public MapList getProductData(Context context, String[] args) throws Exception {
        try {
            Map programMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");

            DomainObject domProduct = DomainObject.newInstance(context, objectId);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPRODUCT);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORMTOPRODUCT);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_ENGINE);
            typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);

            MapList mlProductData = domProduct.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    new StringList(DomainObject.SELECT_ID), // object selects
                    new StringList(DomainRelationship.SELECT_ID), // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null, null);
            return mlProductData;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getProductData: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    // ULS TIGTK-2914 | RFC-078 : 02/09/16 : Swapnil Patil : END

    /**
     * This Method is for find ExcludeOIDs Program-Project not connected to Program-Project
     * @param context
     * @param args
     * @return Object
     * @throws Exception
     *             if fails
     * @author Priyanka Salunke
     * @since 19/10/2016
     */
    @SuppressWarnings("rawtypes")
    public Object excludeConnectedProgProjToProgProj(Context context, String[] args) throws Exception {
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strProgramProjectId = (String) paramMap.get("objectId");
            DomainObject domProgramProjectObject = newInstance(context, strProgramProjectId);
            StringList slCurrentProgProjParents = domProgramProjectObject.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "].from.id");
            StringList excludeOID = new StringList();
            StringList slSelectable = new StringList();
            slSelectable.add(DomainConstants.SELECT_ID);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            // Expression or condition to exclude the objects from search result
            StringBuffer sbWhere = new StringBuffer();
            sbWhere.append("to[");
            sbWhere.append(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
            sbWhere.append("] == True");

            MapList mlReturnList = DomainObject.findObjects(context, typePattern.getPattern(), TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), slSelectable);

            if (mlReturnList.size() > 0) {
                Iterator itr = mlReturnList.iterator();
                Map mTempMap;
                while (itr.hasNext()) {
                    mTempMap = (Map) itr.next();
                    excludeOID.add((String) mTempMap.get(DomainConstants.SELECT_ID));
                }
            }
            for (int i = 0; i < slCurrentProgProjParents.size(); i++) {
                String strProgprojId = (String) slCurrentProgProjParents.get(i);
                if (!excludeOID.contains(strProgprojId)) {
                    excludeOID.add(strProgprojId);
                }

            }
            return excludeOID;
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in excludeConnectedProgProjToProgProj: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }
    }

    // End of method : excludeConnectedProgProjToProgProj

    /**
     * To Include Program-Project in add existing functionality of Program-Project to Program-Project
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     *             if fails
     * @author Priyanka Salunke
     * @since 20/10/2016
     */
    @SuppressWarnings("rawtypes")
    public StringList addExistingProgramProjectToProgramProject(Context context, String[] args) throws Exception {
        StringList slProgramProjectIncludeOIDList = new StringList();
        try {

            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strProgramProjectId = (String) paramMap.get("objectId");
            DomainObject domProgramProjectObject = new DomainObject();
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectId)) {
                domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectId);
            }
            // Get PSS_ProgramPRoject attribute value
            String strProgramProjectAttribute = (String) domProgramProjectObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT);
            StringList slSelectable = new StringList();
            slSelectable.add(DomainConstants.SELECT_ID);
            slSelectable.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT + "]");
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            StringBuffer sbWhereExpression = new StringBuffer();
            sbWhereExpression.append(DomainConstants.SELECT_ID + "!=" + strProgramProjectId);
            // Where expression to Search Program-Project as per the value of PSS_ProgramProject attribute
            if (strProgramProjectAttribute.equalsIgnoreCase("Project")) {
                sbWhereExpression.append(" && attribute[" + TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT + "] == const'Project'");
            }
            // Search Program-PRoject present in system
            MapList mlReturnList = DomainObject.findObjects(context, typePattern.getPattern(), TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhereExpression.toString(), slSelectable);
            if (mlReturnList != null && !mlReturnList.isEmpty()) {
                Iterator itrReturnList = mlReturnList.iterator();
                while (itrReturnList.hasNext()) {
                    Map mReturnMap = (Map) itrReturnList.next();
                    slProgramProjectIncludeOIDList.add((String) mReturnMap.get(DomainConstants.SELECT_ID));
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in addExistingProgramProjectToProgramProject: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }
        return slProgramProjectIncludeOIDList;
    }

    // End of method :

    /**
     * Checks if CAD Route Template is added to Program-Project or not. If not, promote is not allowed
     * @param context
     * @param args
     * @return int
     * @throws Exception
     *             if fails
     * @author Suchit Gangurde
     * @Date 15/12/2016
     * @For TIGTK-3651
     */
    @SuppressWarnings("rawtypes")
    public int checkConnectedCADRouteTemplate(Context context, String[] args) throws Exception {
        StringList slCORouteTemplateList = new StringList(1); // StringList to check against if CAD Route Template is connected to Program-Project
        StringList slFinalList = new StringList(1); // Final list to check if Program-Project has CAD Route Template connected
        String strCADName = "Approval List for CAD on CO";
        int iReturnFlag = 0;

        slCORouteTemplateList.addElement("Approval List for Commercial update on CO");
        slCORouteTemplateList.addElement("Approval List for Prototype on CO");
        slCORouteTemplateList.addElement("Approval List for Serial Launch on CO");
        slCORouteTemplateList.addElement("Approval List for Design study on CO");
        slCORouteTemplateList.addElement("Approval List for Other Parts on CO");
        slCORouteTemplateList.addElement("Approval List for CAD on CO");
        slCORouteTemplateList.addElement("Approval List for Standard Parts on CO");

        try {
            String strProgProjId = args[0];
            DomainObject domProgProjObj = DomainObject.newInstance(context, strProgProjId);
            StringList slSelectStmts = new StringList(); // object selects
            slSelectStmts.addElement(DomainConstants.SELECT_ID);
            slSelectStmts.addElement(DomainConstants.SELECT_NAME);

            StringList slRelSelects = new StringList(1); // relationship selects
            slRelSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]");

            // Get Route Templates connected to Program-Project
            MapList mlConnectedRouteTemplatesList = domProgProjObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES, // relationship
                    DomainConstants.TYPE_ROUTE_TEMPLATE, // types to fetch from other end
                    false, // getTO
                    true, // getFrom
                    1, // recursionTo level
                    slSelectStmts, // object selects
                    slRelSelects, // relationship selects
                    DomainConstants.EMPTY_STRING, // object where
                    DomainConstants.EMPTY_STRING, // relationship where
                    0, // limit
                    DomainConstants.EMPTY_STRING, // post rel pattern
                    DomainConstants.EMPTY_STRING, // post type pattern
                    null); // post patterns

            // Iterate over MapList to check connected Route Templates against list of Route Templates on CO
            if (mlConnectedRouteTemplatesList.size() != 0) {
                for (int mlItr = 0; mlItr < mlConnectedRouteTemplatesList.size(); mlItr++) {
                    Map mTempMap = (Map) mlConnectedRouteTemplatesList.get(mlItr);
                    String strAttrName = (String) mTempMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]");

                    for (int slItr = 0; slItr <= slCORouteTemplateList.size(); slItr++) {
                        if (slCORouteTemplateList.contains(strAttrName)) {
                            if (!slFinalList.contains(strAttrName))
                                slFinalList.addElement(strAttrName);
                        }
                    }
                }
            }

            if (slFinalList.size() >= 1) {
                if (!slFinalList.contains(strCADName)) {
                    iReturnFlag = 1;
                    if (iReturnFlag == 1) {
                        String CADRouteTemplateNotConnectedMsg = EnoviaResourceBundle.getFrameworkStringResourceProperty(context,
                                "emxFramework.PSS_ProgramProject.PSS_CADRouteTemplateNotConnected.AlertMessage", context.getLocale());
                        MqlUtil.mqlCommand(context, "notice $1", CADRouteTemplateNotConnectedMsg);
                    }

                }
            }
            return iReturnFlag;
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkConnectedCADRouteTemplate: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }
    }
    // End of method :

    /**
     * This method returns only those Program-Projects which are not connected to any other Divisions
     * @param context
     * @param args
     * @return Object
     * @throws Exception
     *             if fails
     * @author Suchit Gangurde
     * @Date 16/02/2017
     * @For TIGTK-4441
     */
    public Object excludeConnectedProgProjToDivision(Context context, String[] args) throws Exception {

        try {
            MapList mlReturnList = null;
            StringList excludeOID = new StringList();

            StringList slSelectable = new StringList();
            slSelectable.add(DomainConstants.SELECT_ID);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            StringBuffer sbWhere = new StringBuffer();
            sbWhere.append("from[");
            sbWhere.append(TigerConstants.RELATIONSHIP_PSS_RESPONSIBLEDIVISION);
            sbWhere.append("] == True");

            mlReturnList = DomainObject.findObjects(context, typePattern.getPattern(), TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), slSelectable);

            if (mlReturnList.size() > 0) {
                Iterator itr = mlReturnList.iterator();
                Map mTempMap;
                while (itr.hasNext()) {
                    mTempMap = (Map) itr.next();
                    excludeOID.add((String) mTempMap.get(DomainConstants.SELECT_ID));
                }
            }

            return excludeOID;
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in excludeConnectedProgProjToDivision: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }

    }
    // End of method :

    // TIGTK-7915:29/05/2017 :PTE:START
    /**
     * This Method is for find ExcludeOIDs Program Project , Vehicle , Engine not connected to Product
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public Object excludeConnectedULSObjectsToProduct(Context context, String[] args) throws Exception {
        StringList excludeOID = new StringList();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) paramMap.get("objectId");
            String strFieldtype = (String) paramMap.get("field_actual");
            strFieldtype = strFieldtype.replace("TYPES=", "");
            strFieldtype = strFieldtype.trim();
            StringTokenizer strToken = new StringTokenizer(strFieldtype, ",");
            int i = 0;
            String strTypeArr[] = new String[strToken.countTokens()];
            StringBuffer strtype = new StringBuffer();
            int k = strToken.countTokens();
            while (strToken.hasMoreTokens()) {
                strTypeArr[i] = strToken.nextToken();
                String[] temp = strTypeArr[i].split(":");

                strtype.append(PropertyUtil.getSchemaProperty(context, temp[0]));
                if (k != ++i) {
                    strtype.append(",");
                }
            }
            DomainObject domainObject = new DomainObject(objectId);
            StringList slSelectList = new StringList();
            slSelectList.add(DomainConstants.SELECT_ID);

            domainObject.open(context);
            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPRODUCT);

            MapList mlList = domainObject.getRelatedObjects(context, relationshipPattern.getPattern(), strtype.toString(), slSelectList, null, true, true, (short) 1, null, null, 0);
            if (mlList.size() > 0) {
                Iterator itr = mlList.iterator();
                Map map;
                while (itr.hasNext()) {
                    map = (Map) itr.next();
                    excludeOID.add((String) map.get(DomainConstants.SELECT_ID));
                }
                excludeOID.add(objectId);
            }
            return excludeOID;
        } catch (Exception ex) {
            logger.error("Error in excludeConnectedULSObjectsToProduct: ", ex);
            throw ex;
        }
    }
    // TIGTK-7915:29/05/2017 :PTE:END

    // TIGTK-7912:29/05/2017 :PTE:START
    /**
     * This Method is for find ExcludeOIDs Program Project , Vehicle , Engine not connected to Product
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public Object excludeConnectedULSObjectsToOG(Context context, String[] args) throws Exception {
        try {
            StringList excludeOID = new StringList();

            StringList slSelectList = new StringList();
            slSelectList.add(DomainConstants.SELECT_ID);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_ENGINE);
            typePattern.addPattern(TigerConstants.TYPE_PSS_PLATFORM);
            typePattern.addPattern(TigerConstants.TYPE_PSS_OEM);

            StringList slSelectable = new StringList();
            slSelectable.add(DomainConstants.SELECT_ID);
            slSelectable.add(DomainConstants.SELECT_NAME);

            StringBuffer sbWhere = new StringBuffer();
            sbWhere.append("to[");
            sbWhere.append(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDPLATFORM);
            sbWhere.append("] == TRUE || to[");
            sbWhere.append(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDENGINE);
            sbWhere.append("] == TRUE || to[");
            sbWhere.append(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDOEM);
            sbWhere.append("] == TRUE");

            MapList mlULSList = DomainObject.findObjects(context, typePattern.getPattern(), TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), slSelectable);

            if (mlULSList.size() > 0) {
                Iterator itr = mlULSList.iterator();
                Map mTempMap;
                while (itr.hasNext()) {
                    mTempMap = (Map) itr.next();
                    excludeOID.add((String) mTempMap.get(DomainConstants.SELECT_ID));
                }
            }
            return excludeOID;
        } catch (Exception ex) {
            logger.error("Error in excludeConnectedULSObjectsToOG: ", ex);
            throw ex;
        }
    }

    // TIGTK-7912:29/05/2017 :PTE:END
    /**
     * This method is used to Show Plant connected to Member in Member tree menu
     * @param context
     * @param args
     * @return maplist TIGTK-8661 :Modified on 21/06/2017 by SIE
     * @throws Exception
     */

    public MapList getPlantConnectedToMember(Context context, String[] args) throws Exception {
        try {
            HashMap<String, String> programMap = (HashMap) JPO.unpackArgs(args);

            String strProgProjId = (String) programMap.get("objectId");
            DomainObject domProgProjObj = DomainObject.newInstance(context, strProgProjId);

            StringList slSelectStmts = new StringList();
            StringList slRelStmts = new StringList();

            slSelectStmts.add(DomainConstants.SELECT_ID);
            slRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

            // MapList containing the Plants connected with Member using PSS_CONNECTEDMEMBERS relationship

            MapList mlConnectedPlantList = domProgProjObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, // relationship
                    TigerConstants.TYPE_PSS_PLANT, // type
                    slSelectStmts, // object selects
                    slRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 0, // recursion level
                    null, null, 0);

            return mlConnectedPlantList;

        } catch (Exception e) {
            logger.error("Error in getPlantConnectedToMember: ", e);
            throw e;
        }

    }

    // TIGTK-7917:27/07/2017 :PTE:START

    /**
     * @param context
     * @param objectId
     * @param domOEMGroupObj
     * @return
     * @throws Exception
     */
    public boolean checkOEMGroupProgProjConnectedToChildProgProj(Context context, String objectId, DomainObject domOEMGroupObj) throws Exception {
        boolean checkVisibility = false;

        Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEMGROUP);
        relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
        Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        StringList slObjSelectStmts = new StringList(1);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

        StringList slSelectRelStmts = new StringList(1);

        MapList mpListProgProj = domOEMGroupObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                typePattern.getPattern(), // object pattern
                slObjSelectStmts, // object selects
                slSelectRelStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 2, // recursion level
                null, // object where clause
                null, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                typePostPattern, null, null, null, null);

        if (mpListProgProj.size() > 0) {
            String strProgProjObjId = null;
            for (int i = 0; i < mpListProgProj.size(); i++) {
                strProgProjObjId = (String) ((Map) mpListProgProj.get(i)).get(DomainConstants.SELECT_ID);

                if (objectId.equals(strProgProjObjId)) {
                    checkVisibility = true;
                }
            }
        }

        return checkVisibility;
    }

    /**
     * @param context
     * @param objectId
     * @param domPlatformObj
     * @return
     * @throws Exception
     */
    public boolean checkPlatformProgramProject(Context context, String objectId, DomainObject domPlatformObj) throws Exception {
        boolean checkVisibility = false;

        Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLATFORM);

        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        StringList slObjSelectStmts = new StringList(1);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

        StringList slSelectRelStmts = new StringList(1);

        MapList mpListProgProj = domPlatformObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                typePattern.getPattern(), // object pattern
                slObjSelectStmts, // object selects
                slSelectRelStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 0, // recursion level
                null, // object where clause
                null, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                typePostPattern, null, null, null, null);

        if (mpListProgProj.size() > 0) {
            String strProgProjObjId = null;
            for (int i = 0; i < mpListProgProj.size(); i++) {
                strProgProjObjId = (String) ((Map) mpListProgProj.get(i)).get(DomainConstants.SELECT_ID);
                if (objectId.equals(strProgProjObjId)) {
                    checkVisibility = true;
                }
            }
        }

        return checkVisibility;
    }

    /**
     * @param context
     * @param objectId
     * @param domOEMObj
     * @return
     * @throws Exception
     */
    public boolean checkOEMProgramProject(Context context, String objectId, DomainObject domOEMObj) throws Exception {
        boolean checkVisibility = false;

        Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEM);
        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
        Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        StringList slObjSelectStmts = new StringList(1);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

        StringList slSelectRelStmts = new StringList(1);

        MapList mpListProgProj = domOEMObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                typePattern.getPattern(), // object pattern
                slObjSelectStmts, // object selects
                slSelectRelStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 0, // recursion level
                null, // object where clause
                null, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                typePostPattern, null, null, null, null);

        if (mpListProgProj.size() > 0) {
            String strProgProjObjId = null;
            for (int i = 0; i < mpListProgProj.size(); i++) {
                strProgProjObjId = (String) ((Map) mpListProgProj.get(i)).get(DomainConstants.SELECT_ID);

                if (objectId.equals(strProgProjObjId)) {
                    checkVisibility = true;
                }
            }
        }

        return checkVisibility;
    }

    /**
     * @param context
     * @param objectId
     * @param domOEMObj
     * @return
     * @throws Exception
     */
    public boolean checkOEMVehicleProgramProject(Context context, String objectId, DomainObject domOEMObj) throws Exception {
        boolean checkVisibility = false;

        Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDVEHICLE);
        relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ASSIGNEDVEHICLE);

        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
        typePattern.addPattern(TigerConstants.TYPE_PSS_VEHICLE);

        Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        StringList slObjSelectStmts = new StringList(1);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

        StringList slSelectRelStmts = new StringList(1);

        MapList mpListProgProj = domOEMObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                typePattern.getPattern(), // object pattern
                slObjSelectStmts, // object selects
                slSelectRelStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 0, // recursion level
                null, // object where clause
                null, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                typePostPattern, null, null, null, null);

        if (mpListProgProj.size() > 0) {
            String strProgProjObjId = null;
            for (int i = 0; i < mpListProgProj.size(); i++) {
                strProgProjObjId = (String) ((Map) mpListProgProj.get(i)).get(DomainConstants.SELECT_ID);

                if (objectId.equals(strProgProjObjId)) {
                    checkVisibility = true;
                }
            }
        }

        return checkVisibility;
    }

    /**
     * @param context
     * @param objectId
     * @param domOEMObj
     * @return
     * @throws Exception
     */
    public boolean checkOEMProgProjConnectedToChildProgProj(Context context, String objectId, DomainObject domOEMObj) throws Exception {
        boolean checkVisibility = false;

        Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
        relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDOEM);
        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
        Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        StringList slObjSelectStmts = new StringList(1);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

        StringList slSelectRelStmts = new StringList(1);

        MapList mpListProgProj = domOEMObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                typePattern.getPattern(), // object pattern
                slObjSelectStmts, // object selects
                slSelectRelStmts, // relationship selects
                false, // to direction
                true, // from direction
                (short) 2, // recursion level
                null, // object where clause
                null, (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 1000, // pageSize
                typePostPattern, null, null, null, null);

        if (mpListProgProj.size() > 0) {
            String strProgProjObjId = null;
            for (int i = 0; i < mpListProgProj.size(); i++) {
                strProgProjObjId = (String) ((Map) mpListProgProj.get(i)).get(DomainConstants.SELECT_ID);

                if (objectId.equals(strProgProjObjId)) {
                    checkVisibility = true;
                }
            }
        }

        return checkVisibility;
    }
    // TIGTK-7917:27/07/2017 :PTE:END

    // TIGTK-9583:30/08/2017 :PTE:Start

    /**
     * Member Tab of Person To show data of Program Project related member with position
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public MapList getPPConnectedToMemberWithRolePosition(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strPersonObjId = (String) programMap.get("objectId");

            DomainObject domainObj = DomainObject.newInstance(context, strPersonObjId);

            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            MapList mlProgProjDataList = domainObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern......
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, 0);

            return mlProgProjDataList;
        } catch (Exception e) {
            logger.error("Error in getPPConnectedToMemberWithRolePosition: ", e);
            throw e;
        }
    }
    // TIGTK-9583:30/08/2017 :PTE:END

    // RFC-138 Start

    /**
     * RFC-138: Method to check all core role with lead position present on PP or not with respective state
     * @param context
     * @param args
     * @throws Exception
     */

    public static boolean checkLeadRolePresentOnPP(Context context, String args[]) throws Exception {
        try {
            String strObjectId = args[0];
            DomainObject domProgramProjectObject = DomainObject.newInstance(context, strObjectId);
            boolean iReturnFlag = false;
            String strRoleSelect = DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_PSS_ROLE);
            String strPositionSelect = DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_PSS_POSITION);
            StringList slObjSelectStmts = new StringList(2);
            slObjSelectStmts.addElement(strPositionSelect);
            slObjSelectStmts.addElement(strRoleSelect);
            final String POSITION_LEAD = "Lead";
            Locale strLocale = new Locale(context.getSession().getLanguage());

            Pattern typePattern = new Pattern(DomainConstants.TYPE_PERSON);
            Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS);
            MapList mlRolePositionList = domProgramProjectObject.getRelatedObjects(context, relPattern.getPattern(), // relationship
                    typePattern.getPattern(), // object pattern
                    null, // object selects
                    slObjSelectStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null, null);
            String strRole = DomainConstants.EMPTY_STRING;
            String strPosition = DomainConstants.EMPTY_STRING;
            boolean boolGetLeadRoleListOnPreviousState = false;
            if (UIUtil.isNotNullAndNotEmpty(args[1]) && "PREVIOUS_STATE".equals(args[1])) {
                boolGetLeadRoleListOnPreviousState = true;
            }
            StringList slCoreRoleLead = getCoreRoleList(context, strObjectId, boolGetLeadRoleListOnPreviousState);
            StringList slMissingRoleList = new StringList();
            Map mapRolePositionForCoreTeam = new HashMap();
            for (Object object : mlRolePositionList) {
                Map mapRolePosition = (Map) object;
                strRole = (String) mapRolePosition.get(strRoleSelect);
                strPosition = (String) mapRolePosition.get(strPositionSelect);
                if (slCoreRoleLead.contains(strRole) && POSITION_LEAD.equals(strPosition)) {
                    mapRolePositionForCoreTeam.put(strRole, strPosition);
                }
            }
            if (mapRolePositionForCoreTeam.size() < 9) {

                Set keySet = mapRolePositionForCoreTeam.keySet();

                for (int j = 0; j < slCoreRoleLead.size(); j++) {
                    String roleValue = slCoreRoleLead.get(j).toString();
                    if (!keySet.contains(roleValue)) {
                        String strKey = "emxFramework.Role." + roleValue;
                        String strPSSRoleValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", strLocale, strKey);
                        slMissingRoleList.add(strPSSRoleValue);
                        iReturnFlag = true;
                    }
                }
                if (iReturnFlag) {
                    String strCoreTeamNotDefinedMsg = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.PSS_ProgramProject.CoreRoleNotAssigned.AlertMessage",
                            context.getLocale());
                    strCoreTeamNotDefinedMsg = strCoreTeamNotDefinedMsg + " : " + slMissingRoleList.join(" | ");
                    MqlUtil.mqlCommand(context, "notice $1", strCoreTeamNotDefinedMsg);
                }
            }
            return iReturnFlag;
        } catch (Exception ex) {
            logger.error("Error in checkLeadRolePresentOnPP: ", ex);
            throw ex;
        }

    }

    /**
     * RFC-138: Method to return core role list with respective state
     * @param context
     * @param args
     * @throws Exception
     */
    public static StringList getCoreRoleList(Context context, String strObjectId, boolean boolGetLeadRoleListOnPreviousState) throws Exception {
        try {
            StringList lstReturnCoreRoleList = new StringList();
            String strRoleList = DomainConstants.EMPTY_STRING;

            DomainObject domObj = DomainObject.newInstance(context, strObjectId);

            //TIGTK-12983 - ssamel : START
			//String strCurrentState = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            StringList slSelect = new StringList(2);
            slSelect.addElement(DomainConstants.SELECT_CURRENT);
            slSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_OWNERSHIP + "].value");
            
            Map mpPPDetails = domObj.getInfo(context, slSelect);

            String strCurrentState = (String) mpPPDetails.get(DomainConstants.SELECT_CURRENT);
            String sOwnership = (String) mpPPDetails.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_OWNERSHIP + "].value");
            //TIGTK-12983 - ssamel : END
            
            String strTargetState = strCurrentState;

            if (boolGetLeadRoleListOnPreviousState) {
                Vector stateNames = getPolicyStates(context, TigerConstants.POLICY_PSS_PROGRAM_PROJECT);

                int iPreviousStateIndex = stateNames.indexOf(strCurrentState) - 1;

                if (iPreviousStateIndex >= 0 && iPreviousStateIndex <= stateNames.size()) {
                    strTargetState = (String) stateNames.get(iPreviousStateIndex);
                }
            }

            try {
                strRoleList = EnoviaResourceBundle.getProperty(context, "emxFramework.ULS.Program-Project.CoreLeadRoleList." + strTargetState.replace(" ", "_"));
            } catch (Exception e) {
                String strCoreRoleKeyDefault = "emxFramework.ULS.Program-Project.CoreLeadRoleList.Default";
                strRoleList = EnoviaResourceBundle.getProperty(context, strCoreRoleKeyDefault);
            }
            StringList lstCoreRoleList = FrameworkUtil.split(strRoleList, ",");
            for (int i = 0; i < lstCoreRoleList.size(); i++) {
                String strRole = (String) lstCoreRoleList.get(i);
                String strRealRole = PropertyUtil.getSchemaProperty(context, strRole);
                
                //TIGTK-12983 - ssamel : START
                //lstReturnCoreRoleList.add(strRealRole);
                if(UIUtil.isNotNullAndNotEmpty(sOwnership) && TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equals(sOwnership) && 
                        strRole.contains(TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV))
                {
                    lstReturnCoreRoleList.add(strRealRole);
                }
                else if(UIUtil.isNotNullAndNotEmpty(sOwnership) && TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_FAURECIA.equals(sOwnership) && 
                        strRole.contains(TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_FAURECIA))
                {
                    lstReturnCoreRoleList.add(strRealRole);
                }
                //TIGTK-12983 - ssamel : END
            }
            return lstReturnCoreRoleList;
        } catch (Exception ex) {
            logger.error("Error in getCoreRoleList: ", ex);
            throw ex;
        }
    }

    /**
     * This method will get states associated with a policy.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param policy
     *            String representing policy.
     * @return Vector representing states associated with a Policy.
     * @throws Exception
     *             if the operation fails.
     * @since AEF 10-0-1-0.
     */
    static protected Vector getPolicyStates(Context context, String policy) throws Exception {
        try {
            Vector states = new Vector();

            MQLCommand cmd = new MQLCommand();
            cmd.executeCommand(context, "print policy $1 select state dump $2", policy, "|");
            StringTokenizer tokens = new StringTokenizer(cmd.getResult(), "|\n");

            while (tokens.hasMoreTokens()) {
                String name = tokens.nextToken();
                states.add(name);
            }

            return states;
        } catch (Exception ex) {
            logger.error("Error in postProcessForPPMemberUpdate: ", ex);
            throw ex;
        }
    }

    /**
     * RFC-138: Method to check on save as core role list present on PP with lead with respective state
     * @param context
     * @param args
     * @throws Exception
     */
    public HashMap postProcessForPPMemberUpdate(Context context, String args[]) throws Exception {
        try {
            HashMap returnMap = new HashMap();
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<String, String> paramMap = (HashMap<String, String>) programMap.get("paramMap");
            String strObjectId = (String) paramMap.get("objectId");
            DomainObject domObj = DomainObject.newInstance(context, strObjectId);
            String strCurrentState = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            if (!TigerConstants.STATE_ACTIVE.equals(strCurrentState)) {
                String[] args_tmp = new String[] { strObjectId, "PREVIOUS_STATE" };
                boolean boolValidLeadRoles = checkLeadRolePresentOnPP(context, args_tmp);
                if (boolValidLeadRoles)
                    returnMap.put("Action", "Stop");
            }
            return returnMap;
        } catch (Exception ex) {
            logger.error("Error in postProcessForPPMemberUpdate: ", ex);
            throw ex;
        }
    }

    /**
     * RFC-138: Method to check on remove as core role list present on PP with lead with respective state
     * @param context
     * @param args
     * @throws Exception
     */
    public int checkLeadRoleOnMemberDisconnection(Context context, String args[]) throws Exception {
        try {
            int iReturn = 0;
            String strRelId = args[0];
            String strObjectId = args[1];
            String strPersonName = args[2];
            DomainObject domObj = DomainObject.newInstance(context, strObjectId);
            String strCurrentState = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            if (!TigerConstants.STATE_ACTIVE.equals(strCurrentState)) {
                StringList strRoleList = getCoreRoleList(context, strObjectId, true);
                Map relAttributeMap = DomainRelationship.getAttributeMap(context, strRelId);
                String strRolePosition = (String) relAttributeMap.get(TigerConstants.ATTRIBUTE_PSS_POSITION);
                if ("Lead".equalsIgnoreCase(strRolePosition)) {
                    String strRoleName = (String) relAttributeMap.get(TigerConstants.ATTRIBUTE_PSS_ROLE);
                    Locale strLocale = new Locale(context.getSession().getLanguage());
                    String strPSSRoleValue = "emxFramework.Role." + strRoleName;
                    String strPSSRole = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", strLocale, strPSSRoleValue);

                    if (UIUtil.isNotNullAndNotEmpty(strRoleName)) {
                        if (strRoleList.contains(strRoleName)) {
                            String strMsg = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.PSS_ProgramProject.CanNotRemoveLeadRole.AlertMessage", context.getLocale());
                            strMsg = strMsg.replace("$<PERSON_NAME>", strPersonName);
                            strMsg = strMsg.replace("$<ROLE_NAME>", strPSSRole);
                            MqlUtil.mqlCommand(context, "notice $1", strMsg);
                            iReturn = 1;
                        }
                    }
                }
            }

            return iReturn;
        } catch (Exception ex) {
            logger.error("Error in checkLeadRoleOnMemberDisconnection: ", ex);
            throw ex;
        }

    }

    /**
     * RFC-138: Method to check All Children PP Obsolete Or NonAwarded for promotion of PP ,only promotion of parent allowed if all child are in Obsolete Or NonAwarded
     * @param context
     * @param args
     * @throws Exception
     */
    public int checkIfAllChildrenPPObsoleteOrNonAwarded(Context context, String args[]) throws Exception {
        try {
            int iReturn = 0;
            String strObjectId = args[0];
            DomainObject domObj = DomainObject.newInstance(context, strObjectId);
            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            String strWhereClause = "(current!='" + TigerConstants.STATE_OBSOLETE + "' && current!='" + TigerConstants.STATE_NONAWARDED + "')";
            MapList mlList = domObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT, TigerConstants.TYPE_PSS_PROGRAMPROJECT, slObjSelectStmts, slSelectRelStmts, false,
                    true, (short) 1, strWhereClause, null, 0);
            if (mlList.size() > 0) {
                String strMsg = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.PSS_ProgramProject.AllChildrenNonObsoleteNonAwarded.AlertMessage", context.getLocale());
                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                iReturn = 1;
            }
            return iReturn;
        } catch (Exception ex) {
            logger.error("Error in checkIfAllChildrenPPObsoleteOrNonAwarded: ", ex);
            throw ex;
        }
    }

    /**
     * RFC-138: Method to change assignee of task and role assessment on action of user-role is removed from Program-Project
     * @param context
     * @param args
     * @throws Exception
     */

    public void reassignTasksAndRoleAssessments(Context context, String[] args) throws Exception {

        try {
            String strProgramProjectObjId = args[0];
            String strPersonObjId = args[1];
            DomainObject domPersonObj = DomainObject.newInstance(context, strPersonObjId);
            String strPersonName = domPersonObj.getInfo(context, DomainObject.SELECT_NAME);

            // Pass RelId from trigger
            if (!isCurrentMemberLast(context, strProgramProjectObjId, args[2], strPersonObjId))
                return;

            MapList mpListCR = getCRs(context, strProgramProjectObjId);

            MapList mpListCA = getCAs(context, strProgramProjectObjId);

            MapList mpListMCA = getMCAs(context, strProgramProjectObjId);

            StringList slChangeObject = new StringList(1);
            if (mpListCR.size() > 0) {
                StringList slRoleAssessmentObjId = new StringList(1);

                Iterator itrCR = mpListCR.iterator();
                while (itrCR.hasNext()) {
                    Map mpCR = (Map) itrCR.next();
                    String strCRObjId = (String) mpCR.get(DomainObject.SELECT_ID);

                    slChangeObject.addElement(strCRObjId);

                    MapList mpListRoleAssessment = getRoleAssessments(context, strCRObjId, strPersonName);

                    if (mpListRoleAssessment.size() > 0) {
                        Iterator itrRoleAssessment = mpListRoleAssessment.iterator();
                        while (itrRoleAssessment.hasNext()) {
                            Map mpRoleAssessment = (Map) itrRoleAssessment.next();
                            String strRoleAssessmentObjId = (String) mpRoleAssessment.get(DomainObject.SELECT_ID);
                            slRoleAssessmentObjId.addElement(strRoleAssessmentObjId);
                        }
                    }
                }
                String strLeadPMObjId = getLead(context, strProgramProjectObjId, TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                DomainObject domNewTaskUser = new DomainObject(strLeadPMObjId);
                String strLeadPM = domNewTaskUser.getInfo(context, DomainConstants.SELECT_NAME);

                // Process Role Assessments
                if (UIUtil.isNotNullAndNotEmpty(strLeadPM) && !slRoleAssessmentObjId.isEmpty()) {
                    for (int i = 0; i < slRoleAssessmentObjId.size(); i++) {
                        String strRoleAssessmentObjId = (String) slRoleAssessmentObjId.get(i);

                        DomainObject domRoleAssessmentObj = DomainObject.newInstance(context, strRoleAssessmentObjId);
                        matrix.db.Access mAccess = domRoleAssessmentObj.getAccessMask(context);
                        if (mAccess.hasChangeOwnerAccess())
                            domRoleAssessmentObj.setOwner(context, strLeadPM);
                        else {
                            boolean isPushedContext = false;
                            MqlUtil.mqlCommand(context, "history off", true, false);
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            domRoleAssessmentObj.setOwner(context, strLeadPM);
                            isPushedContext = true;
                            if (isPushedContext) {
                                ContextUtil.popContext(context);
                            }
                            MqlUtil.mqlCommand(context, "history on", true, false);

                        }
                    }
                }
            }

            if (mpListCA.size() > 0) {
                Iterator itrCA = mpListCA.iterator();
                while (itrCA.hasNext()) {
                    Map mpCA = (Map) itrCA.next();
                    String strCAObjId = (String) mpCA.get(DomainObject.SELECT_ID);

                    slChangeObject.addElement(strCAObjId);
                }
            }

            if (mpListMCA.size() > 0) {
                Iterator itrMCA = mpListMCA.iterator();
                while (itrMCA.hasNext()) {
                    Map mpMCA = (Map) itrMCA.next();
                    String strMCOObjId = (String) mpMCA.get(DomainObject.SELECT_ID);
                    slChangeObject.addElement(strMCOObjId);
                }
            }

            // Process Tasks
            if (!slChangeObject.isEmpty()) {
                StringList slInboxTasks = getInboxTasks(context, slChangeObject, strPersonName);

                if (!slInboxTasks.isEmpty()) {
                    String strLeadCMOID = getLead(context, strProgramProjectObjId, TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                    DomainObject domNewTaskUser = new DomainObject(strLeadCMOID);
                    String strNewTaskUser = domNewTaskUser.getInfo(context, DomainConstants.SELECT_NAME);

                    for (int i = 0; i < slInboxTasks.size(); i++) {
                        String strInboxTaskObjId = (String) slInboxTasks.get(i);
                        InboxTask inboxTask = new InboxTask(strInboxTaskObjId);
                        inboxTask.reAssignTask(context, domNewTaskUser, DomainConstants.TYPE_PERSON, inboxTask.getOwner(context).getName(), DomainConstants.TYPE_PERSON, strNewTaskUser);

                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error in reassignTasksAndRoleAssessments: ", ex);
            throw ex;
        }
    }

    /**
     * RFC-138: Method to check on remove of Member from PP,Member which is to be removed is last on that PP,only that case reassignment of task done.
     * @param context
     * @param ProgramProject
     *            id
     * @param RelId
     * @param PersonId
     * @throws Exception
     */
    public boolean isCurrentMemberLast(Context context, String strProgramProjectObjId, String strMemberConnectionId, String strPersonId) throws Exception {
        try {
            DomainObject domObj = DomainObject.newInstance(context, strProgramProjectObjId);
            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS);

            Pattern typePattern = new Pattern(TYPE_PERSON);

            StringBuffer sbWhereBuffer = new StringBuffer();
            sbWhereBuffer.append(DomainConstants.SELECT_ID);
            sbWhereBuffer.append(" == ");
            sbWhereBuffer.append(strPersonId);
            sbWhereBuffer.append(" && ");
            sbWhereBuffer.append(DomainConstants.SELECT_RELATIONSHIP_ID);
            sbWhereBuffer.append(" != '");
            sbWhereBuffer.append(strMemberConnectionId);
            sbWhereBuffer.append("'");

            MapList mlPerDataList = domObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slSelectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    sbWhereBuffer.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null, null);

            if (mlPerDataList.size() > 0)
                return false;

            return true;
        } catch (Exception ex) {
            logger.error("Error in isCurrentMemberLast: ", ex);
            throw ex;
        }

    }

    /**
     * RFC-138: Method to get Inbox Tasks of Member which is to be removed form PP for reassignment
     * @param context
     * @param strChangeObjId
     * @param Person
     *            objectId
     * @return
     * @throws Exception
     */
    private StringList getInboxTasks(Context context, StringList slChangeObject, String strPersonName) throws Exception {
        StringList slInboxTasks = new StringList();

        StringList objectSelects = new StringList(1);
        objectSelects.addElement(DomainObject.SELECT_ID);

        Pattern relPattern = new Pattern(DomainObject.RELATIONSHIP_OBJECT_ROUTE);
        relPattern.addPattern(DomainObject.RELATIONSHIP_ROUTE_TASK);

        Pattern typePattern = new Pattern(DomainObject.TYPE_ROUTE);
        typePattern.addPattern(DomainObject.TYPE_INBOX_TASK);

        StringBuilder sbObjWhere = new StringBuilder();

        sbObjWhere.append("(");
        sbObjWhere.append(DomainConstants.SELECT_TYPE);
        sbObjWhere.append(" == ");
        sbObjWhere.append(DomainConstants.TYPE_ROUTE);
        sbObjWhere.append(") || (");
        sbObjWhere.append(DomainConstants.SELECT_TYPE);
        sbObjWhere.append(" == ");
        sbObjWhere.append("'");
        sbObjWhere.append(DomainConstants.TYPE_INBOX_TASK);
        sbObjWhere.append("'");
        sbObjWhere.append(" && ");
        sbObjWhere.append(DomainConstants.SELECT_OWNER);
        sbObjWhere.append(" == '");
        sbObjWhere.append(strPersonName);
        sbObjWhere.append("'");
        sbObjWhere.append(" && ");
        sbObjWhere.append(DomainConstants.SELECT_CURRENT);
        sbObjWhere.append(" == ");
        sbObjWhere.append("'" + DomainConstants.STATE_INBOX_TASK_ASSIGNED + "'");
        sbObjWhere.append(")");

        for (int i = 0; i < slChangeObject.size(); i++) {
            String strChangeObjId = (String) slChangeObject.get(i);

            DomainObject domChangeObj = DomainObject.newInstance(context, strChangeObjId);
            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_INBOX_TASK);

            MapList mpListInboxTasks = domChangeObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    objectSelects, // object selects
                    DomainConstants.EMPTY_STRINGLIST, // relationship selects
                    true, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    sbObjWhere.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    typePostPattern, null, null, null, null);

            if (mpListInboxTasks.size() > 0) {
                Iterator itrInboxTask = mpListInboxTasks.iterator();
                while (itrInboxTask.hasNext()) {
                    Map mpInboxTask = (Map) itrInboxTask.next();
                    String strInboxTaskbjId = (String) mpInboxTask.get(DomainObject.SELECT_ID);
                    slInboxTasks.addElement(strInboxTaskbjId);
                }
            }
        }

        return slInboxTasks;
    }

    /**
     * RFC-138: Method to get Role Assessments of Member which is to be removed form PP for reassignment
     * @param context
     * @param strCRObjId
     * @param strPersonName
     * @return
     * @throws Exception
     */
    private MapList getRoleAssessments(Context context, String strCRObjId, String strPersonName) throws Exception {

        try {
            StringList objectSelects = new StringList(1);
            objectSelects.addElement(DomainObject.SELECT_ID);

            Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_IMPACTANALYSIS);
            typePattern.addPattern(TigerConstants.TYPE_PSS_ROLEASSESSMENT);

            DomainObject domCRObj = DomainObject.newInstance(context, strCRObjId);

            StringBuilder sbObjWhere = new StringBuilder();

            sbObjWhere.append("(");
            sbObjWhere.append(DomainConstants.SELECT_TYPE);
            sbObjWhere.append(" == ");
            sbObjWhere.append(TigerConstants.TYPE_PSS_IMPACTANALYSIS);
            sbObjWhere.append(") || (");
            sbObjWhere.append(DomainConstants.SELECT_TYPE);
            sbObjWhere.append(" == ");
            sbObjWhere.append("'");
            sbObjWhere.append(TigerConstants.TYPE_PSS_ROLEASSESSMENT);
            sbObjWhere.append("'");
            sbObjWhere.append(" && ");
            sbObjWhere.append(DomainConstants.SELECT_OWNER);
            sbObjWhere.append(" == '");
            sbObjWhere.append(strPersonName);
            sbObjWhere.append("'");
            sbObjWhere.append(")");

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_ROLEASSESSMENT);

            MapList mpListRoleAssessment = domCRObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    objectSelects, // object selects
                    DomainConstants.EMPTY_STRINGLIST, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    sbObjWhere.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    typePostPattern, null, null, null, null);

            return mpListRoleAssessment;

        } catch (Exception ex) {
            logger.error("Error in getRoleAssessments: ", ex);
            throw ex;
        }

    }

    /**
     * RFC-138 Method to get lead Change Coordinator or Program Manger to assign task in case particular person is no more on that PP.
     * @param context
     * @param strProgramProjectObjId
     * @param strRoleName
     * @return
     * @throws Exception
     */
    private String getLead(Context context, String strProgramProjectObjId, String strRoleName) throws Exception {
        try {

            // TIGTK-12983 : JV : START
            DomainObject doProgramProject=DomainObject.newInstance(context,strProgramProjectObjId);
            String attrOwnership = doProgramProject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OWNERSHIP);
            if(TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV.equalsIgnoreCase(attrOwnership)) {
                strRoleName = strRoleName + "_" + TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV;
            }
            // TIGTK-12983 : JV : END
            
            String strQuery = "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]=='" + strRoleName + "' && attribute["
                    + TigerConstants.ATTRIBUTE_PSS_POSITION + "]=='Lead'].to.id";

            String strCmd = "print bus \"" + strProgramProjectObjId + "\"   select  \"" + strQuery + "\"  dump | ";

            String strLeadPerson = MqlUtil.mqlCommand(context, strCmd, true, true);
            return strLeadPerson;
        } catch (Exception ex) {
            logger.error("Error in getLead: ", ex);
            throw ex;
        }

    }

    /**
     * RFC-138 Method to get connected CR for context PP.
     * @param context
     * @param strProgramProjectObjId
     * @return
     * @throws Exception
     */
    private MapList getCRs(Context context, String strProgramProjectObjId) throws Exception {
        try {

            StringList objectSelects = new StringList(1);
            objectSelects.addElement(DomainObject.SELECT_ID);

            String strRelationship = TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA;
            String strType = TigerConstants.TYPE_PSS_CHANGEREQUEST;

            DomainObject domProgramProjectObj = DomainObject.newInstance(context, strProgramProjectObjId);

            MapList mpListCR = domProgramProjectObj.getRelatedObjects(context, strRelationship, strType, objectSelects, DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1,
                    DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0);

            return mpListCR;
        } catch (Exception ex) {
            logger.error("Error in getCRs: ", ex);
            throw ex;
        }
    }

    /**
     * RFC-138 Method to get connected CA for context PP.
     * @param context
     * @param strProgramProjectObjId
     * @return
     * @throws Exception
     */
    private MapList getCAs(Context context, String strProgramProjectObjId) throws Exception {
        try {
            StringList objectSelects = new StringList(1);
            objectSelects.addElement(DomainObject.SELECT_ID);

            DomainObject domProgramProjectObj = DomainObject.newInstance(context, strProgramProjectObjId);

            Pattern relPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_ACTION);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);

            Pattern typePattern = new Pattern(ChangeConstants.TYPE_CHANGE_ACTION);
            typePattern.addPattern(TigerConstants.TYPE_PSS_CHANGEORDER);

            Pattern typePostPattern = new Pattern(ChangeConstants.TYPE_CHANGE_ACTION);

            MapList mpListCA = domProgramProjectObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    objectSelects, // object selects
                    DomainConstants.EMPTY_STRINGLIST, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    typePostPattern, null, null, null, null);

            return mpListCA;
        } catch (Exception ex) {
            logger.error("Error in getCAs: ", ex);
            throw ex;
        }
    }

    /**
     * RFC-138 Method to get connected MCA for context PP.
     * @param context
     * @param strProgramProjectObjId
     * @return
     * @throws Exception
     */
    private MapList getMCAs(Context context, String strProgramProjectObjId) throws Exception {
        try {

            StringList objectSelects = new StringList(1);
            objectSelects.addElement(DomainObject.SELECT_ID);

            Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
            typePattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION);

            DomainObject domProgramProjectObj = DomainObject.newInstance(context, strProgramProjectObjId);

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION);

            MapList mpListInboxTasks = domProgramProjectObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    objectSelects, // object selects
                    DomainConstants.EMPTY_STRINGLIST, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    typePostPattern, null, null, null, null);

            return mpListInboxTasks;
        } catch (Exception ex) {
            logger.error("Error in getMCAs: ", ex);
            throw ex;
        }
    }

    /**
     * RFC-138: Method to reset attribute "Branch to" when PP demote from Non Awarded and Obsolete
     * @param context
     * @param args
     * @throws Exception
     */

    public void demotePPFromNonAwardedOrObsolete(Context context, String[] args) throws Exception {
        try {
            String strProgramProjectObjId = args[0];
            DomainObject domPPObj = DomainObject.newInstance(context, strProgramProjectObjId);
            domPPObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, "None");

        } catch (Exception ex) {
            logger.error("Error in demotePPFromNonAwardedOrObsolete: ", ex);
            throw ex;
        }
    }

    // RFC-138 END

    /**
     * TIGTK-9779: Method to display Plant list connected to member exclude the context plant.
     * @param args
     * @throws Exception
     */

    public Vector<String> getWhereUsed(Context context, String[] args) throws Exception {

        try {
            Vector vWhereUsedValuesVector = new Vector();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            Map paramMap = (Map) programMap.get("paramList");
            String strReportFormat = (String) paramMap.get("reportFormat");
            String strParentOID = (String) paramMap.get("parentOID");
            Iterator iterator = objectList.iterator();

            Map objectMap;

            boolean isReport = false;
            if (UIUtil.isNotNullAndNotEmpty(strReportFormat))
                isReport = true;

            while (iterator.hasNext()) {
                objectMap = (Map) iterator.next();

                String strId = (String) objectMap.get("id");
                String strRelName = (String) objectMap.get("relationship");

                DomainObject domObjectPart = DomainObject.newInstance(context, strId);
                StringList slSelectRelStmts = new StringList(1);
                slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                StringList slObjSelectStmts = new StringList(1);
                slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);

                Pattern relationshipPattern = new Pattern(DomainConstants.EMPTY_STRING);

                String strRelWhere = DomainConstants.EMPTY_STRING;
                String strObjWhere = DomainConstants.EMPTY_STRING;

                Pattern typePattern = new Pattern(DomainConstants.EMPTY_STRING);
                Pattern typePostPattern = new Pattern(DomainConstants.EMPTY_STRING);

                if (strRelName.equals(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ)) {
                    relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ);
                    slSelectRelStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "]");
                    strRelWhere = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "] != \"" + DomainConstants.EMPTY_STRING + "\")";
                    strObjWhere = "id==" + strParentOID;
                    typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
                    typePattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
                } else if (strRelName.equals(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS)) {
                    relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS);
                    typePostPattern = new Pattern(TigerConstants.TYPE_PSS_PLANT);
                    typePattern.addPattern(TYPE_PERSON);
                    typePattern.addPattern(TigerConstants.TYPE_PSS_PLANT);
                }
                // TIGTK-11729 :FindBug | 28-Nov-2017 START
                MapList mlList = domObjectPart.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                        typePattern.getPattern(), // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 0, // recursion level
                        strObjWhere, // object where clause
                        strRelWhere, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        typePostPattern, null, null, null, null);

                // TIGTK-11729 :FindBug | 28-Nov-2017 END
                Iterator<?> itr = mlList.iterator();
                StringBuilder sbObj = new StringBuilder();
                while (itr.hasNext()) {
                    Map<?, ?> mTempMap = (Map<?, ?>) itr.next();
                    String strName = DomainConstants.EMPTY_STRING;
                    String strPlantId = DomainConstants.EMPTY_STRING;
                    if (strRelName.equals(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ)) {
                        strName = (String) mTempMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "]");
                    } else {
                        strName = (String) mTempMap.get(DomainConstants.SELECT_NAME);
                        strPlantId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                    }

                    String strPlantName = DomainConstants.EMPTY_STRING;

                    StringList slAPlantList = FrameworkUtil.split(strName, ",");

                    for (int i = 0; i < slAPlantList.size(); i++) {
                        strPlantName = (String) slAPlantList.get(i);
                        if (isReport) {
                            sbObj.append(strPlantName);
                            if (itr.hasNext())
                                sbObj.append(";");
                        } else {
                            sbObj.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                            if (UIUtil.isNotNullAndNotEmpty(strPlantId))
                                sbObj.append(strPlantId);
                            else
                                sbObj.append(strId);
                            sbObj.append("', '860', '520', 'false', 'popup')\">");
                            sbObj.append(" " + strPlantName);
                            sbObj.append("</a>");

                            if (itr.hasNext())
                                sbObj.append("<br/>");

                        }
                    }
                }
                vWhereUsedValuesVector.add(sbObj.toString());
            }
            return vWhereUsedValuesVector;
        } catch (Exception e) {
            logger.error("Error in ULSUIUtil : getWhereUsed: ", e);
            throw e;
        }

    }

    /**
     * TIGTk-7911: Method to check Program Type Attribute value when object is connected with relationship PSS_GlobalLocalProgramProject
     * @param context
     * @param args
     * @throws Exception
     */
    public int checkIfGlobalLocalPPLinkExist(Context context, String args[]) throws Exception {
        try {
            int iReturn = 0;
            String strProgProjId = args[0];
            DomainObject domProgProj = DomainObject.newInstance(context, strProgProjId);
            String strNewAttrValue = args[1];
            String strKey = DomainConstants.EMPTY_STRING;

            if ("Global".equals(strNewAttrValue)) {
                boolean isPPConnectedTo = domProgProj.hasRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_GLOBALLOCALPROGRAMPROJECT, false);
                if (isPPConnectedTo)
                    strKey = "emxFramework.PSS_ProgramProject.PSS_ProgramProjectConnected.Local.AlertMessage";
            } else if ("Local".equals(strNewAttrValue)) {
                boolean isPPConnectedFrom = domProgProj.hasRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_GLOBALLOCALPROGRAMPROJECT, true);
                if (isPPConnectedFrom)
                    strKey = "emxFramework.PSS_ProgramProject.PSS_ProgramProjectConnected.Global.AlertMessage";
            }
            if (UIUtil.isNotNullAndNotEmpty(strKey)) {
                String strMessage = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, strKey, context.getLocale());
                String strPPName = args[2];
                strMessage = strMessage.replace("$<NAME>", strPPName);
                MqlUtil.mqlCommand(context, "notice $1", strMessage);

                iReturn = 1;
            }
            return iReturn;

        } catch (Exception ex) {
            logger.error("Error in checkIfGlobalLocalPPLinkExist: ", ex);
            throw ex;
        }
    }

    /**
     * TIGTk-7911 This method is used on the Table Column that displays program-Project connected with ParentChild rel or GlobalLocal
     * @param context
     *            the eMatrix <code>Context</code> object
     * @returns StringList of X
     * @throws Exception
     *             if the operation fails
     */

    public StringList displayLinkTypePP(Context context, String[] args) throws Exception {
        StringList slProgProjResult = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList mlObjList = (MapList) programMap.get("objectList");
            Iterator itrobjectList = mlObjList.iterator();

            while (itrobjectList.hasNext()) {
                Map mapCurrentObject = (Map) itrobjectList.next();

                String strCurrentPPId = (String) mapCurrentObject.get(DomainConstants.SELECT_ID);
                String strRelFromId = (String) mapCurrentObject.get(DomainRelationship.SELECT_FROM_ID);

                String strRelPPName = (String) mapCurrentObject.get(DomainConstants.SELECT_RELATIONSHIP_NAME);

                boolean isCurrentPPOnFromSide = false;
                if (strCurrentPPId.equals(strRelFromId))
                    isCurrentPPOnFromSide = true;
                String strKey = "";

                if (TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT.equals(strRelPPName)) {
                    if (isCurrentPPOnFromSide)
                        strKey = "emxFramework.DisplayLinkType.Text.Parent";
                    else
                        strKey = "emxFramework.DisplayLinkType.Text.Child";

                } else {
                    if (isCurrentPPOnFromSide)
                        strKey = "emxFramework.DisplayLinkType.Text.Global";
                    else
                        strKey = "emxFramework.DisplayLinkType.Text.Local";
                }
                String strValue = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, strKey, context.getLocale());
                slProgProjResult.add(strValue);

            }
            return slProgProjResult;
        } catch (Exception e) {

            logger.error("Error in displayLinkTypePP: ", e);

            throw e;
        }

    }

    /**
     * TIGTk-7911 Method called on Program-Project properties page to get Local Global Program Type of PP.
     * @param context
     * @param args
     * @return String --- Program-Type Value if PP is type Local
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public String getLinkToProgramInfoOnPPPropertiesPage(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map paramMap = (Map) programMap.get("paramMap");
            String strPPObjId = (String) paramMap.get("objectId");
            DomainObject domPPobj = DomainObject.newInstance(context, strPPObjId);
            boolean areGlobalPPConnected = domPPobj.hasRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_GLOBALLOCALPROGRAMPROJECT, false);
            String strKey = DomainConstants.EMPTY_STRING;
            if (areGlobalPPConnected) {
                strKey = "Link_to_global";
            } else {
                strKey = "Standalone";
            }
            String strText = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.PPLinkToProgramInfo.Text." + strKey, context.getLocale());
            return strText;
        } catch (Exception ex) {
            logger.error("Error in getLinkToProgramInfoOnPPPropertiesPage: ", ex);
            throw ex;
        }

    }

    /**
     * TIGTk-7911 Method called on creation of Global Local link relationship.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkParentChildProgramType(Context context, String args[]) throws Exception {
        try {
            int iReturn = 0;
            String strFromPPId = args[0];
            DomainObject domObjFromPP = DomainObject.newInstance(context, strFromPPId);
            String strFromPPProgramType = domObjFromPP.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROGRAMTYPE);
            String strToPPId = args[1];
            DomainObject domObjToPP = DomainObject.newInstance(context, strToPPId);
            String strToPPProgramType = domObjToPP.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROGRAMTYPE);
            if (!"Global".equalsIgnoreCase(strFromPPProgramType) || !"Local".equalsIgnoreCase(strToPPProgramType)) {
                String strGlobalLocalProgramProjectConnectedMsg = EnoviaResourceBundle.getFrameworkStringResourceProperty(context,
                        "emxFramework.PSS_ProgramProject.Global_Local_Connection.AlertMessage", context.getLocale());
                iReturn = 1;
                throw new Exception(strGlobalLocalProgramProjectConnectedMsg.toString());
            }
            return iReturn;
        } catch (Exception ex) {
            logger.error("Error in checkParentChildProgramType: ", ex);
            throw ex;
        }
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author aniketm
     * @description method is used to display Master RnD Center value on PP Location table.
     */
    public StringList isMasterRnDCenter(Context context, String[] args) throws Exception {

        StringList strList = new StringList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);

            Map paramMap = (HashMap) programMap.get("paramList");
            String strRootObjectId = (String) paramMap.get("objectId");

            MapList mlObjectList = (MapList) programMap.get("objectList");

            for (int i = 0; i < mlObjectList.size(); i++) {
                Map mapObject = (Map) mlObjectList.get(i);
                String objectId = (String) mapObject.get(DomainConstants.SELECT_ID);

                DomainObject domObject = new DomainObject(objectId);
                String Type = domObject.getInfo(context, DomainObject.SELECT_TYPE);

                if (TigerConstants.TYPE_PSS_RnDCENTER.equals(Type)) {
                    // get PP
                    DomainObject domPP = DomainObject.newInstance(context, strRootObjectId);
                    StringList objectSelect = new StringList(DomainConstants.SELECT_ID);
                    StringList relSelect = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
                    relSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_MASTER_RnD_CENTER + "]");

                    StringBuffer objWhere = new StringBuffer();
                    objWhere.append("id ==");
                    objWhere.append(objectId);
                    MapList mlSelectedRel = domPP.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY, TigerConstants.TYPE_PSS_RnDCENTER, objectSelect, relSelect, false, true,
                            (short) 0, objWhere.toString(), DomainConstants.EMPTY_STRING, 0);

                    if (mlSelectedRel != null && !mlSelectedRel.isEmpty()) {

                        Map selectedRel = (Map) mlSelectedRel.get(0);
                        String StrRelationshipId = (String) selectedRel.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                        String ismasterRnd = (String) selectedRel.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_MASTER_RnD_CENTER + "]");

                        if (ismasterRnd.equalsIgnoreCase("yes")) {
                            strList.add("Yes");
                        } else {
                            strList.add(" ");
                        }

                    }
                } else {
                    strList.add(" ");
                }
            }
        } catch (Exception ex) {
            logger.error("Error in isMasterRnDCenter  in JPO pss.uls.ULSUIUtil: ", ex);
        }
        return strList;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author aniketm
     * @description method is used to set RnD Center as a Master
     */
    public String setMasterRnDCenter(Context context, String[] args) throws Exception {

        String strResult = "FAIL";
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String plantId = (String) paramMap.get("strRnDCenterId");
        String ProgramProjId = (String) paramMap.get("objectId");

        try {
            StringList objectSelect = new StringList(DomainConstants.SELECT_ID);
            StringList relSelect = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);

            DomainObject domPP = DomainObject.newInstance(context, ProgramProjId);

            // Find any previous Rnd Center marked as Yes if found set it to No
            StringBuffer relWhere = new StringBuffer();
            relWhere.append("attribute[");
            relWhere.append(TigerConstants.ATTRIBUTE_PSS_MASTER_RnD_CENTER);
            relWhere.append("].value");
            relWhere.append(" == ");
            relWhere.append("Yes");

            MapList domRelatedRnDCenter = domPP.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY, TigerConstants.TYPE_PSS_RnDCENTER, objectSelect, relSelect, false, true,
                    (short) 0, DomainConstants.EMPTY_STRING, relWhere.toString(), 0);
            if (domRelatedRnDCenter != null && !domRelatedRnDCenter.isEmpty()) {
                Map selectedRel = (Map) domRelatedRnDCenter.get(0);
                String StrRelationshipId = (String) selectedRel.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                DomainRelationship RelObject = DomainRelationship.newInstance(context, StrRelationshipId);
                RelObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MASTER_RnD_CENTER, "No");
            }

            // Set newly selected Rnd Center as a Master and set value to Yes
            StringBuffer objWhere = new StringBuffer();
            objWhere.append("id ==");
            objWhere.append(plantId);
            MapList mlSelectedRel = domPP.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY, TigerConstants.TYPE_PSS_RnDCENTER, objectSelect, relSelect, false, true,
                    (short) 0, objWhere.toString(), DomainConstants.EMPTY_STRING, 0);
            if (mlSelectedRel != null && !mlSelectedRel.isEmpty()) {
                Map selectedRel = (Map) mlSelectedRel.get(0);
                String StrRelationshipId = (String) selectedRel.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                DomainRelationship RelObject = DomainRelationship.newInstance(context, StrRelationshipId);

                RelObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MASTER_RnD_CENTER, "Yes");
            }
        } catch (Exception ex) {
            logger.error("Error in setMasterRnDCenter: ", ex);
            throw ex;

        } finally {
            return strResult;
        }

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author PriyankaT
     * @description method is used to Transfer Member of Local PP to Global if PSS_Role attribute is 1st time modified on connected member relationship.
     */
    public int copyMemberFromLocalToGlobalPP(Context context, String[] args) throws Exception {

        String strNewValue = args[0];
        String strobjectId = args[1];
        String strMemberId = args[2];
        String strOldAttrValue = args[4];
        boolean isContextPushed = false;
        try {
            if (UIUtil.isNotNullAndNotEmpty(strOldAttrValue))
                return 0;

            if (!TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS.equals(args[5]) || !TigerConstants.TYPE_PSS_PROGRAMPROJECT.equals(args[6]))
                return 0;

            String strMember = "Member";
            DomainObject domObjPP = DomainObject.newInstance(context, strobjectId);
            DomainObject domChildObj = DomainObject.newInstance(context, strMemberId);

            String strTypeOfPP = domObjPP.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROGRAMTYPE);
            if ("Local".equals(strTypeOfPP)) {
                String strGlobalId = domObjPP.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_GLOBALLOCALPROGRAMPROJECT + "].from.id");
                if (UIUtil.isNotNullAndNotEmpty(strGlobalId)) {
                    DomainObject domGlobalPP = DomainObject.newInstance(context, strGlobalId);
                    if (UIUtil.isNotNullAndNotEmpty(strGlobalId)) {
                        String objectWhere = "id == '" + strMemberId + "'";
                        StringBuffer relWhere = new StringBuffer();
                        relWhere.append("attribute[");
                        relWhere.append(TigerConstants.ATTRIBUTE_PSS_POSITION);
                        relWhere.append("]");
                        relWhere.append(" == const\"");
                        relWhere.append(strMember);
                        relWhere.append("\" && ");
                        relWhere.append("attribute[");
                        relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
                        relWhere.append("]");
                        relWhere.append(" == '");
                        relWhere.append(strNewValue);
                        relWhere.append("'");

                        StringList slSelectRelStmts = new StringList(3);
                        slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                        slSelectRelStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_POSITION + "]");
                        slSelectRelStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]");

                        StringList slObjSelectStmts = new StringList(2);
                        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                        MapList mapGlobalPerson = domGlobalPP.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, // relationship pattern
                                TYPE_PERSON, // object pattern
                                slObjSelectStmts, // object selects
                                slSelectRelStmts, // relationship selects
                                false, // to direction
                                true, // from direction
                                (short) 1, // recursion level
                                objectWhere, // object where clause
                                relWhere.toString(), 0);

                        if (mapGlobalPerson == null || mapGlobalPerson.isEmpty()) {
                            DomainRelationship domMemberRel = DomainRelationship.connect(context, domGlobalPP, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, domChildObj);
                            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            isContextPushed = true;
                            domMemberRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_POSITION, strMember);
                            domMemberRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_ROLE, strNewValue);

                        }
                    }
                }
            }

        } catch (Exception ex) {
            logger.error("Error in copyMemberFromLocalToGlobalPP: ", ex);
            throw ex;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
                isContextPushed = false;
            }
        }
        return 0;
    }

    /**
     * TIGTk-11536 This method call While addition of Program-Project under Program-Project. A program-project with attribute "program" is only a father of one level of program-project with attribute
     * "program" same for "Project"
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    public int checkForValidChildProgramProject(Context context, String[] args) throws Exception {
        logger.debug("checkForValidChildProgramProject : Start ");
        int iReturn = 0;
        final String PROGRAM = "Program";
        final String PROJECT = "Project";
        String strMessage = DomainConstants.EMPTY_STRING;
        try {
            String strFromObjId = args[0]; // From Program-Project Object Id
            String strToObjId = args[1]; // To Program-Project Object Id

            // get Root Program-Project Type
            DomainObject domFromObj = DomainObject.newInstance(context, strFromObjId);
            String strFromPPProgramType = domFromObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT);

            // get Child Program-Project Type
            DomainObject domToObj = DomainObject.newInstance(context, strToObjId);
            String strToPPProgramType = domToObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT);

            // Check Program is not added under Project
            if (strFromPPProgramType.equals(PROJECT) && strToPPProgramType.equals(PROGRAM)) {
                iReturn = 1;
                strMessage = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.PSS_ProgramProject.ProgramUnderProject.AlertMessage", context.getLocale());
            } else {
                // object select list
                StringList slSelectFromObject = new StringList(1);
                slSelectFromObject.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT + "].value");

                // Check parent present to the From Object Prog-Proj
                Map mParentMap = domFromObj.getRelatedObject(context, TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT, false, slSelectFromObject, null);

                if (mParentMap != null && !mParentMap.isEmpty()) {
                    String strParentProgProjType = (String) mParentMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT + "].value");

                    // Check hierarchy not exists in more than one level with same Program-project Type
                    if (strFromPPProgramType.equals(strToPPProgramType) && strParentProgProjType.equals(strToPPProgramType)) {
                        iReturn = 1;
                        strMessage = EnoviaResourceBundle.getFrameworkStringResourceProperty(context, "emxFramework.PSS_ProgramProject.ValidateChildType.AlertMessage", context.getLocale());
                    }
                }
            }

            if (iReturn == 1) {
                throw new Exception(strMessage);
            }

        } catch (Exception ex) {
            logger.error("Error in checkForValidChildProgramProject : ", ex);
            throw ex;
        }
        logger.debug("checkForValidChildProgramProject : END ");
        return iReturn;
    }

    /**
     * This method is for make Person searchable which is connected with PSS_ConnectedMemebers relationship
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author psalunke : TIGTK-12867
     * @modified at 19-07-2018
     */

    public String getMembers(Context context, String[] args) throws Exception {

        StringBuffer sbOrgReturnValue = new StringBuffer();
        try {
            String objectId = args[0];
            DomainObject domObj = new DomainObject(objectId);
            StringList objectSelects = new StringList(DomainConstants.SELECT_NAME);
            Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_MEMBER);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS);

            MapList orgList = domObj.getRelatedObjects(context, relPattern.getPattern(), "*", objectSelects, null, true, // boolean getTo,
                    false, // boolean getFrom,
                    (short) 0, null, null, 0);

            String strDel = matrix.db.SelectConstants.cSelectDelimiter;

            Iterator itr = orgList.iterator();
            while (itr.hasNext()) {
                Map orgMap = (Map) itr.next();
                sbOrgReturnValue.append(orgMap.get(DomainConstants.SELECT_NAME));
                sbOrgReturnValue.append(strDel);
            }
        } catch (Exception e) {
            logger.error("Error in pss.uls.ULSUIUtil : getMembers : ", e);
        }
        return sbOrgReturnValue.toString();
    }

    /**
     * @param context
     * @param strGlobalPPId
     * @param strLocalPPId
     * @throws Exception
     *             This method will add Member of Global Program Project to Local on connection of Local PP to Global PP.
     */
    public void copyMemberFromLocalToGlobalPPOnConnection(Context context, String[] args) throws Exception {

        logger.debug(" copyMemberFromLocalToGlobalPPOnConnection : START");
        boolean isContextPushed = false;
        try {
            String strGlobalPPId = args[0];
            String strLocalPPId = args[1];

            DomainObject domGlobalPP = DomainObject.newInstance(context, strGlobalPPId);
            DomainObject domLocalPP = DomainObject.newInstance(context, strLocalPPId);
            String strMember = "Member";

            StringList busSelects = new StringList();
            StringList relSelects = new StringList();
            busSelects.add(DomainObject.SELECT_ID);

            relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);
            relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]");

            MapList mapLocalPPerson = domLocalPP.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, DomainConstants.TYPE_PERSON, busSelects, relSelects, true, true,
                    (short) 1, null, null, 0);
            Iterator itr = mapLocalPPerson.iterator();
            while (itr.hasNext()) {
                Map mpPP = (Map) itr.next();
                String strMemberId = (String) mpPP.get(DomainObject.SELECT_ID);
                String strLocalRole = (String) mpPP.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]");
                if (UIUtil.isNotNullAndNotEmpty(strLocalRole)) {
                    String objectWhere = "id == '" + strMemberId + "'";
                    StringBuffer relWhere = new StringBuffer();
                    relWhere.append("attribute[");
                    relWhere.append(TigerConstants.ATTRIBUTE_PSS_POSITION);
                    relWhere.append("]");
                    relWhere.append(" == const\"");
                    relWhere.append(strMember);
                    relWhere.append("\" && ");
                    relWhere.append("attribute[");
                    relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
                    relWhere.append("]");
                    relWhere.append(" == '");
                    relWhere.append(strLocalRole);
                    relWhere.append("'");

                    StringList slSelectRelStmts = new StringList(3);
                    slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                    slSelectRelStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_POSITION + "]");
                    slSelectRelStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]");

                    StringList slObjSelectStmts = new StringList(2);
                    slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                    MapList mapGlobalPerson = domGlobalPP.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, // relationship pattern
                            TYPE_PERSON, // object pattern
                            slObjSelectStmts, // object selects
                            slSelectRelStmts, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            objectWhere, // object where clause
                            relWhere.toString(), 0);
                    if (mapGlobalPerson == null || mapGlobalPerson.isEmpty()) {
                        DomainObject domMemberLocal = DomainObject.newInstance(context, strMemberId);

                        DomainRelationship domMemberRel = DomainRelationship.connect(context, domGlobalPP, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, domMemberLocal);
                        ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        isContextPushed = true;
                        domMemberRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_POSITION, strMember);
                        domMemberRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_ROLE, strLocalRole);

                    }
                }
            }

        } catch (FrameworkException e) {
            logger.error("copyMemberFromLocalToGlobalPPOnConnection : ERROR ", e);
            throw e;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
                isContextPushed = false;
            }
        }
    }

    /**
     * Description : This method is used to display Program-Project Member Category Commands
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     * @since 13-07-2018 : TIGTK-15849
     * @author psalunke
     */
    public boolean showProgramProjectMemberCommands(Context context, String[] args) throws Exception {
        logger.debug("\n pss.uls.ULSUIUtil:showProgramProjectMemberCommands:START");
        boolean hasAccess = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strProgramProjectId = (String) programMap.get("objectId");

            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectId)) {
                DomainObject domProgProj = DomainObject.newInstance(context, strProgramProjectId);
                StringList slPPSelects = new StringList();
                slPPSelects.addElement(DomainConstants.SELECT_OWNER);
                slPPSelects.addElement("project");
                Map mpProgramProjectInfo = domProgProj.getInfo(context, slPPSelects);
                String strProgramProjectOwner = (String) mpProgramProjectInfo.get(DomainConstants.SELECT_OWNER);
                String strProgramProjectCS = (String) mpProgramProjectInfo.get("project");
                String strLoggedInUser = context.getUser();
                String strDefaultContext = PersonUtil.getDefaultProject(context, strLoggedInUser);
                if (PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)
                        || strProgramProjectOwner.equalsIgnoreCase(strLoggedInUser)) {
                    hasAccess = true;
                } else if (PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_PROGRAM_MANAGER) && strDefaultContext.equalsIgnoreCase(strProgramProjectCS)) {

                    String strRoleSelect = DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_PSS_ROLE);
                    String strPositionSelect = DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_PSS_POSITION);
                    // Relationship Selects
                    StringList slRelSelect = new StringList(3);
                    slRelSelect.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
                    slRelSelect.addElement(strPositionSelect);
                    slRelSelect.addElement(strRoleSelect);
                    // Relationship Pattern
                    Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS);
                    // Type Pattern
                    Pattern typePattern = new Pattern(TYPE_PERSON);
                    Pattern typePostPattern = new Pattern(TYPE_PERSON);

                    final String POSITION_LEAD = "Lead";

                    String strPersonId = PersonUtil.getPersonObjectID(context, strLoggedInUser);

                    // Object Where
                    StringBuffer sbObjectWhere = new StringBuffer();
                    sbObjectWhere.append(DomainConstants.SELECT_ID);
                    sbObjectWhere.append(" == \"");
                    sbObjectWhere.append(strPersonId);
                    sbObjectWhere.append("\"");
                    // Relationship Where
                    StringBuffer sbRelWhere = new StringBuffer();
                    sbRelWhere.append("attribute[");
                    sbRelWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
                    sbRelWhere.append("]");
                    sbRelWhere.append(" == \"");
                    sbRelWhere.append(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                    sbRelWhere.append("\"");

                    MapList mlConnectedMemebersOfPP = domProgProj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                            typePattern.getPattern(), // object pattern
                            new StringList(DomainConstants.SELECT_ID), // object selects
                            slRelSelect, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            sbObjectWhere.toString(), // object where clause
                            sbRelWhere.toString(), // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            typePostPattern, null, null, null, null);
                    if (mlConnectedMemebersOfPP != null && !mlConnectedMemebersOfPP.isEmpty()) {
                        hasAccess = true;
                    }
                }
            }
            logger.debug("\n pss.uls.ULSUIUtil:showProgramProjectMemberCommands:END");
        } catch (Exception ex) {
            logger.error("\n pss.uls.ULSUIUtil:showProgramProjectMemberCommands:ERROR ", ex);
            throw ex;
        }
        return hasAccess;
    }

    /**
     * Method to get latest meatured PP TIGTK-13635 : ALM-5736
     * @param context
     * @return
     * @throws Exception
     */
    public String getLatestMaturedProgramProject(Context context, DomainObject part, String collaborativeSpace, StringList projectMemberRoles, boolean checkForProjectMember) throws Exception {
        logger.debug(":::::::: ENTER :: getLatestMaturedProgramProject ::::::::");
        String strProgramProjectOID = DomainConstants.EMPTY_STRING;
        try {
            String strObjectWhere = (part != null) ? "project == '" + part.getInfo(context, "project") + "'"
                    : (UIUtil.isNotNullAndNotEmpty(collaborativeSpace)) ? "project == '" + collaborativeSpace + "'" : DomainConstants.EMPTY_STRING;
            if (UIUtil.isNotNullAndNotEmpty(strObjectWhere)) {
                Query query = new Query();
                query.setBusinessObjectType(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
                query.setBusinessObjectName(DomainConstants.QUERY_WILDCARD);
                query.setBusinessObjectRevision(DomainConstants.QUERY_WILDCARD);
                query.setVaultPattern(TigerConstants.VAULT_ESERVICEPRODUCTION);
                // Find bug error shall be eliminated
                query.setWhereExpression(strObjectWhere);

                StringList slObjSelects = new StringList(5);
                slObjSelects.add(DomainConstants.SELECT_ID);
                slObjSelects.add(DomainConstants.SELECT_CURRENT);
                slObjSelects.add("current.actual");
                slObjSelects.add("from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]");
                if (null != projectMemberRoles && !projectMemberRoles.isEmpty()) {
                    for (Object projMemberRole : projectMemberRoles) {
                        slObjSelects.add(
                                "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "].value=='" + (String) projMemberRole + "'].to.name");
                    }
                } else {
                    slObjSelects.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");
                }

                QueryIterator queryIterator = query.getIterator(context, slObjSelects, (short) 0);
                query.close(context);
                String strCurrent = DomainConstants.EMPTY_STRING;
                boolean bIsPPHasChildPP = true;
                String strcontextUser = TigerUtils.getLoggedInUserName(context);
                MapList mlProgramProjectObjectsList = new MapList();
                StringList slStates = TigerUtils.getStates(context, TigerConstants.POLICY_PSS_PROGRAM_PROJECT);
                int iOriginalIndex = 0;
                while (queryIterator.hasNext()) {
                    BusinessObjectWithSelect bowsPP = queryIterator.next();
                    bIsPPHasChildPP = Boolean.valueOf(bowsPP.getSelectData("from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]"));
                    // proceed only if leaf PP
                    if (!bIsPPHasChildPP) {
                        strCurrent = bowsPP.getSelectData(DomainConstants.SELECT_CURRENT);
                        // proceed only if current is !(Activ && Non-Awarded && Obsolete)
                        if (UIUtil.isNotNullAndNotEmpty(strCurrent) && !TigerConstants.STATE_ACTIVE.equals(strCurrent) && !TigerConstants.STATE_OBSOLETE.equals(strCurrent)
                                && !TigerConstants.STATE_NONAWARDED.equals(strCurrent)) {
                            StringList slMembers = bowsPP.getSelectDataList("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");
                            // proceed only if Person is memebr of PP
                            if (!checkForProjectMember || (slMembers != null && !slMembers.isEmpty() && slMembers.contains(strcontextUser))) {
                                int icurrentIndex = slStates.indexOf(strCurrent);
                                // Sorting with highest state - Phase 1 < Phase 2a < Phase 2b < Phase 3 < Phase 4 < Phase 5 < In Service
                                if (icurrentIndex >= iOriginalIndex) {
                                    if (icurrentIndex > iOriginalIndex) {
                                        iOriginalIndex = icurrentIndex;
                                        if (mlProgramProjectObjectsList.size() > 0) {
                                            mlProgramProjectObjectsList.clear();
                                        }
                                    }
                                    Map mpPPOfContextUser = new HashMap();
                                    mpPPOfContextUser.put(DomainConstants.SELECT_ID, bowsPP.getSelectData(DomainConstants.SELECT_ID));
                                    mpPPOfContextUser.put(DomainConstants.SELECT_CURRENT, strCurrent);
                                    mpPPOfContextUser.put("current.actual", bowsPP.getSelectData("current.actual"));
                                    mlProgramProjectObjectsList.add(mpPPOfContextUser);
                                } // sort END
                            }
                        }
                    }
                }
                queryIterator.close();
                if (!mlProgramProjectObjectsList.isEmpty()) {
                    mlProgramProjectObjectsList.sort("current.actual", "descending", "date");
                    strProgramProjectOID = (String) ((Map) mlProgramProjectObjectsList.get(0)).get(DomainConstants.SELECT_ID);
                }
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: getLatestMaturedProgramProject ::::::::");
        return strProgramProjectOID;
    }

    /**
     * Method to get latest meatured PP from the Colobrative Space of Part TIGTK-13635 : ALM-5736
     * @param context
     * @param part
     * @return
     * @throws Exception
     */
    public String getLatestMaturedProgramProjectFromObjectCS(Context context, DomainObject part) throws Exception {
        logger.debug(":::::::: ENTER :: getLatestMaturedProgramProjectFromObjectCS ::::::::");
        String strProgramProjectOID = DomainConstants.EMPTY_STRING;
        try {
            strProgramProjectOID = getLatestMaturedProgramProject(context, part, null, null, true);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: getLatestMaturedProgramProjectFromObjectCS ::::::::");
        return strProgramProjectOID;
    }

    /**
     * Method to get latest meatured PP from the Colobrative Space with specific role as a project member role. TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public String getLatestMaturedPPFromCSWithRole(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: getLatestMaturedPPFromCSWithRole ::::::::");
        String strProgramProjectOID = DomainConstants.EMPTY_STRING;
        boolean isPushContext = false;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strCollaborativeSpace = (String) paramMap.get("NewCollaborativeSpace");
            String strMemberRole = (String) paramMap.get("memberRole");
            TigerUtils.pushContextToSuperUser(context);
            isPushContext = true;
            ContextUtil.startTransaction(context, false);
            strProgramProjectOID = getLatestMaturedProgramProject(context, null, strCollaborativeSpace, FrameworkUtil.split(strMemberRole, ":"), true);
            ContextUtil.commitTransaction(context);
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectOID)) {
                DomainObject doPP = DomainObject.newInstance(context, strProgramProjectOID);
                strProgramProjectOID = doPP.getInfo(context, DomainConstants.SELECT_NAME) + "@" + strProgramProjectOID;
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }finally {
            if(isPushContext)
                ContextUtil.popContext(context);
        }
        logger.debug(":::::::: EXIT :: getLatestMaturedPPFromCSWithRole ::::::::");
        return strProgramProjectOID;
    }

    /**
     * Method to get latest meatured PP from CS where the context user may not be the member of target PP
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public String getLatestMaturedPPFromCSWithoutMemberCheck(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: getLatestMaturedPPFromCSWithoutMemberCheck ::::::::");
        String strProgramProjectOID = DomainConstants.EMPTY_STRING;
        boolean isPushContext = false;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strCollaborativeSpace = (String) paramMap.get("NewCollaborativeSpace");
            TigerUtils.pushContextToSuperUser(context);
            isPushContext = true;
            ContextUtil.startTransaction(context, false);
            strProgramProjectOID = getLatestMaturedProgramProject(context, null, strCollaborativeSpace, null, false);
            ContextUtil.commitTransaction(context);
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectOID)) {
                DomainObject doPP = DomainObject.newInstance(context, strProgramProjectOID);
                strProgramProjectOID = doPP.getInfo(context, DomainConstants.SELECT_NAME) + "@" + strProgramProjectOID;
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }finally {
            if(isPushContext)
                ContextUtil.popContext(context);
        }
        logger.debug(":::::::: EXIT :: getLatestMaturedPPFromCSWithoutMemberCheck ::::::::");
        return strProgramProjectOID;
    }

    /**
     * Method to get latest meatured PP from the Colobrative Space
     * @param context
     * @param collaborativeSpace
     * @return
     * @throws Exception
     */
    public String getLatestMaturedProgramProjectFromCS(Context context, String collaborativeSpace) throws Exception {
        logger.debug(":::::::: ENTER :: getLatestMaturedProgramProjectFromCS ::::::::");
        String strProgramProjectOID = DomainConstants.EMPTY_STRING;
        try {
            ContextUtil.startTransaction(context, false);
            strProgramProjectOID = getLatestMaturedProgramProject(context, null, collaborativeSpace, null, true);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: getLatestMaturedProgramProjectFromCS ::::::::");
        return strProgramProjectOID;
    }

    /**
     * Get all leaf level PP to which the context user is member with specific role TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getAllLeafProgramProjectWithMemberRole(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: getAllLeafProgramProjectWithMemberRole ::::::::");
        StringList slProgramProjectOID = new StringList();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String projectMemberRole = (String) paramMap.get("memberRole");
            StringList slprojectMemberRoles = FrameworkUtil.split(projectMemberRole, ":");
            Query query = new Query();
            query.setBusinessObjectType(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            query.setBusinessObjectName(DomainConstants.QUERY_WILDCARD);
            query.setBusinessObjectRevision(DomainConstants.QUERY_WILDCARD);
            query.setVaultPattern(TigerConstants.VAULT_ESERVICEPRODUCTION);

            StringList slObjSelects = new StringList(4);
            slObjSelects.add(DomainConstants.SELECT_ID);
            slObjSelects.add(DomainConstants.SELECT_CURRENT);
            slObjSelects.add("from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]");
            for (Object projMemberRole : slprojectMemberRoles) {
                projMemberRole = PropertyUtil.getSchemaProperty(context, (String) projMemberRole);
                slObjSelects.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "].value=='" + projMemberRole + "'].to.name");
            }
            ContextUtil.startTransaction(context, false);
            QueryIterator queryIterator = query.getIterator(context, slObjSelects, (short) 0);
            query.close(context);
            String strCurrent = DomainConstants.EMPTY_STRING;
            boolean bIsPPHasChildPP = true;
            String strcontextUser = TigerUtils.getLoggedInUserName(context);
            while (queryIterator.hasNext()) {
                BusinessObjectWithSelect bowsPP = queryIterator.next();
                bIsPPHasChildPP = Boolean.valueOf(bowsPP.getSelectData("from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]"));
                // proceed only if leaf PP
                if (!bIsPPHasChildPP) {
                    strCurrent = bowsPP.getSelectData(DomainConstants.SELECT_CURRENT);
                    // proceed only if current is !(Activ && Non-Awarded && Obsolete)
                    if (UIUtil.isNotNullAndNotEmpty(strCurrent) && !TigerConstants.STATE_ACTIVE.equals(strCurrent) && !TigerConstants.STATE_OBSOLETE.equals(strCurrent)
                            && !TigerConstants.STATE_NONAWARDED.equals(strCurrent)) {
                        StringList slMembers = bowsPP.getSelectDataList("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");
                        // proceed only if Person is memebr of PP
                        if (slMembers != null && !slMembers.isEmpty() && slMembers.contains(strcontextUser)) {
                            slProgramProjectOID.add(bowsPP.getSelectData(DomainConstants.SELECT_ID));
                        }
                    }
                }
            }
            queryIterator.close();
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: getAllLeafProgramProjectWithMemberRole ::::::::");
        return slProgramProjectOID;
    }

    /**
     * Method to get all leaf PP from the CS of Parent Parts if any. If no parent parts available then, list PP from selected CS.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getAllLeafPPFromCSOfParentParts(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: getAllLeafPPFromCSOfParentParts ::::::::");
        StringList slProgramProjectOID = new StringList();
        try {
            HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
            String strSelectedCollaSpace = hmParamMap.containsKey("collaborativeSpace") ? (String) hmParamMap.get("collaborativeSpace") : DomainConstants.EMPTY_STRING;
            String strPartOID = (String) hmParamMap.get("objectId");
            String strWhereCondition = DomainConstants.EMPTY_STRING;
            DomainObject doPart = DomainObject.newInstance(context, strPartOID);
            MapList mlParentParts = doPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, new StringList("project"), null, true, false, (short) 1,
                    DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
            String strProject = DomainConstants.EMPTY_STRING;
            StringBuilder sbCollaSpace = new StringBuilder(1024);
            if (!mlParentParts.isEmpty()) {
                Iterator itrParts = mlParentParts.iterator();
                while (itrParts.hasNext()) {
                    Map mpParentPart = (Map) itrParts.next();
                    strProject = (String) mpParentPart.get("project");
                    sbCollaSpace.append(strProject).append(TigerConstants.SEPERATOR_COMMA);
                }
                if (sbCollaSpace.length() > 0) {
                    sbCollaSpace.setLength(sbCollaSpace.length() - 1);
                    strWhereCondition = "project matchlist \"" + sbCollaSpace.toString() + "\" \",\"";
                }
            } else {
                strWhereCondition = UIUtil.isNotNullAndNotEmpty(strSelectedCollaSpace) ? "project == '" + strSelectedCollaSpace + "'" : DomainConstants.EMPTY_STRING;
            }
            MapList mlProgramProject = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PROGRAMPROJECT, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD,
                    DomainConstants.QUERY_WILDCARD, TigerConstants.VAULT_ESERVICEPRODUCTION, strWhereCondition, false, new StringList(DomainConstants.SELECT_ID));
            if (!mlProgramProject.isEmpty()) {
                Iterator itrProgramProjects = mlProgramProject.iterator();
                while (itrProgramProjects.hasNext()) {
                    Map mpProgramProject = (Map) itrProgramProjects.next();
                    strProject = (String) mpProgramProject.get(DomainConstants.SELECT_ID);
                    slProgramProjectOID.add(strProject);
                }
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: getAllLeafPPFromCSOfParentParts ::::::::");
        return slProgramProjectOID;
    }

    /**
     * checks and deletes the impacted project link if, no parent part with same PP as Governing Project available
     * @param context
     * @param part
     * @param impactedProjectList
     * @throws Exception
     */
    public void checkAndDisconnectInvalidImpacttedProjectLinks(Context context, DomainObject part, StringList impactedProjectList, StringList impactedProjectRelOIDList) throws Exception {
        logger.debug(":::::::: ENTER :: checkAndDisconnectInvalidImpacttedProjectLinks ::::::::");
        try {
            if (impactedProjectList != null && !impactedProjectList.isEmpty()) {
                StringBuilder sbRelPattern = new StringBuilder(126);
                sbRelPattern.append(DomainConstants.RELATIONSHIP_EBOM).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT);
                StringBuilder sbTypePattern = new StringBuilder(126);
                sbTypePattern.append(TigerConstants.TYPE_PART).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
                StringList slObjSelects = new StringList(1);
                slObjSelects.add(DomainConstants.SELECT_ID);
                StringList slRelSelects = new StringList(1);
                slRelSelects.add(DomainRelationship.SELECT_ID);
                MapList mlProgramProjects = part.getRelatedObjects(context, sbRelPattern.toString(), sbTypePattern.toString(), slObjSelects, slRelSelects, true, false, (short) 2, null, null,
                        (short) 0, false, true, (short) 1000, new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT), null, null, null);
                if (!mlProgramProjects.isEmpty()) {
                    StringList slGPOIDList = new StringList();
                    Iterator itr = mlProgramProjects.iterator();
                    while (itr.hasNext()) {
                        Map mpPP = (Map) itr.next();
                        if ("2".equals((String) mpPP.get(DomainConstants.SELECT_LEVEL))) {
                            slGPOIDList.add((String) mpPP.get(DomainConstants.SELECT_ID));
                        }
                    }
                    ContextUtil.startTransaction(context, true);
                    for (int i = 0; i < impactedProjectList.size(); i++) {
                        String strIPoid = (String) impactedProjectList.get(i);
                        if (!slGPOIDList.contains(strIPoid)) {
                            DomainRelationship.disconnect(context, (String) impactedProjectRelOIDList.get(i));
                        }
                    }
                    ContextUtil.commitTransaction(context);
                }
            }
        } catch (RuntimeException re) {
            ContextUtil.abortTransaction(context);
            logger.error(re.getLocalizedMessage(), re);
            throw new Exception(re.getLocalizedMessage(), re);
        } catch (Exception e) {
            ContextUtil.abortTransaction(context);
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: checkAndDisconnectInvalidImpacttedProjectLinks ::::::::");
    }

    /**
     * TIGTK-13635 : ALM-5736
     * @param context
     * @param parentPart
     * @param programProject
     * @throws Exception
     */
    public void createImpactedProjectLinkForEBOM(Context context, DomainObject parentPart, DomainObject programProject) throws Exception {
        logger.debug(":::::::: ENTER :: createImpactedProjectLinkForEBOM ::::::::");
        try {
            if (parentPart != null && programProject != null) {
                StringList slObjSelectables = new StringList(2);
                slObjSelectables.add(DomainConstants.SELECT_ID);
                slObjSelectables.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "]");
                String strObjWhere = "policy == '" + TigerConstants.POLICY_PSS_ECPART + "' && current == '" + TigerConstants.STATE_PSS_ECPART_PRELIMINARY + "' && to["
                        + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "] == FALSE" + " && to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id != '"
                        + programProject.getInfo(context, DomainConstants.SELECT_ID) + "'";
                MapList mlEBOM = parentPart.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, slObjSelectables, new StringList(), false, true, (short) 1,
                        strObjWhere, DomainConstants.EMPTY_STRING, 0);
                if (!mlEBOM.isEmpty()) {
                    int iEBOMSize = mlEBOM.size();
                    ArrayList<String> alEBOMParts = new ArrayList<String>(iEBOMSize);
                    for (int i = 0; i < iEBOMSize; i++) {
                        String strPartOID = (String) ((Map) mlEBOM.get(i)).get(DomainConstants.SELECT_ID);
                        if (UIUtil.isNotNullAndNotEmpty(strPartOID) && !alEBOMParts.contains(strPartOID)) {
                            alEBOMParts.add(strPartOID);
                        }
                    }
                    if (!alEBOMParts.isEmpty()) {
                        String[] saEBOMParts = alEBOMParts.toArray(new String[alEBOMParts.size()]);
                        DomainRelationship.connect(context, programProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT, true, saEBOMParts);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: createImpactedProjectLinkForEBOM ::::::::");
    }

    /**
     * Get Part CS and GP as a json string
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public String getPartCSandGP(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: getPartCSandGP ::::::::");
        String strCSandGP = DomainConstants.EMPTY_STRING;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strPartOID = (String) paramMap.get("objectId");
            DomainObject doPart = DomainObject.newInstance(context, strPartOID);
            String SELECT_GP_OID = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id";
            String SELECT_GP_NAME = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.name";
            StringList slSelectables = new StringList(3);
            slSelectables.add("project");
            slSelectables.add(SELECT_GP_OID);
            slSelectables.add(SELECT_GP_NAME);
            Map mpPartInfo = doPart.getInfo(context, slSelectables);
            JSONObject json = new JSONObject();
            if (mpPartInfo.containsKey(SELECT_GP_OID)) {
                json.put("GoverningProjectOID", (String) mpPartInfo.get(SELECT_GP_OID));
                json.put("GoverningProject", (String) mpPartInfo.get(SELECT_GP_NAME));
            }
            MapList mlPnoProjects = DomainObject.findObjects(context, DomainConstants.TYPE_PNOPROJECT, (String) mpPartInfo.get("project"), "-", DomainConstants.QUERY_WILDCARD,
                    TigerConstants.VAULT_ESERVICEPRODUCTION, DomainConstants.EMPTY_STRING, false, new StringList(DomainConstants.SELECT_ID));
            if (!mlPnoProjects.isEmpty()) {
                json.put("CollaborativeSpaceOID", (String) ((Map) mlPnoProjects.get(0)).get(DomainConstants.SELECT_ID));
                json.put("CollaborativeSpace", (String) mpPartInfo.get("project"));
            }
            strCSandGP = json.toString();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: getPartCSandGP ::::::::");
        return strCSandGP;
    }
}