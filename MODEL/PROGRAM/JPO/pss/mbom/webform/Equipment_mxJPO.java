package pss.mbom.webform;

import java.util.HashMap;
import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class Equipment_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Equipment_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    public Equipment_mxJPO() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * this method is used to connect Equipment object with PSS_Plant object
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Exception appears, if error occurred
     */
    @SuppressWarnings("rawtypes")
    public void connectEquipmentWithPlant(Context context, String args[]) throws Exception {

        String strPlantId = DomainObject.EMPTY_STRING;
        try {
            Map programMap = JPO.unpackArgs(args);
            Map paramMap = (Map) programMap.get("paramMap");
            String strEquipmentId = (String) paramMap.get("objectId");
            strPlantId = (String) paramMap.get("New OID");
            DomainObject domEquipment = DomainObject.newInstance(context, strEquipmentId);
            if (UIUtil.isNullOrEmpty(strPlantId)) {
                strPlantId = (String) paramMap.get("New Value");
            }
            if (!UIUtil.isNullOrEmpty(strPlantId)) {
                DomainObject domPlant = DomainObject.newInstance(context, strPlantId);
                MapList mlConnectedPlants = domEquipment.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ASSOCIATED_PLANT, // relationship pattern
                        TigerConstants.TYPE_PSS_PLANT, // object pattern
                        null, // object selects
                        new StringList(DomainConstants.SELECT_ID), // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Postpattern
                        null, null, null);
                if (!(mlConnectedPlants.isEmpty())) {
                    for (int i = 0; i < mlConnectedPlants.size(); i++) {
                        Map mPlantMap = (Map) mlConnectedPlants.get(i);
                        String strRelId = (String) mPlantMap.get(DomainConstants.SELECT_ID);
                        DomainRelationship.disconnect(context, strRelId);
                    }

                    DomainRelationship.connect(context, domEquipment, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_ASSOCIATED_PLANT), domPlant);
                } else {
                    DomainRelationship.connect(context, domEquipment, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_ASSOCIATED_PLANT), domPlant);
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectEquipmentWithPlant: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

    /**
     * this method is used to connect Equipment Request object with PSS_Plant object
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Exception appears, if error occurred
     */
    @SuppressWarnings("rawtypes")
    public void connectEquipmentRequestWithPlant(Context context, String[] args) throws Exception {
        try {
            Map programMap = JPO.unpackArgs(args);
            Map paramMap = (Map) programMap.get("paramMap");
            String strEquipmentRequestId = (String) paramMap.get("objectId");
            String strPlantId = (String) paramMap.get("New OID");
            if (!UIUtil.isNullOrEmpty(strPlantId)) {
                DomainObject domEquipmentRequest = DomainObject.newInstance(context, strEquipmentRequestId);
                DomainObject domPlant = DomainObject.newInstance(context, strPlantId);
                String strConnectedRelId = domEquipmentRequest.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_EQUIPMENT_REQUEST + "].id");
                if (UIUtil.isNotNullAndNotEmpty(strConnectedRelId)) {
                    DomainRelationship.disconnect(context, strConnectedRelId);
                    DomainRelationship.connect(context, domEquipmentRequest, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_EQUIPMENT_REQUEST), domPlant);
                } else {
                    DomainRelationship.connect(context, domEquipmentRequest, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_EQUIPMENT_REQUEST), domPlant);
                }

            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectEquipmentRequestWithPlant: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

    }

    /**
     * this method is used to filter the Equipment Request Object for the search
     * @param context
     * @param args
     * @return List of Equipment Request objects
     * @throws Exception
     *             Exception appears, if error occurred
     */
    @SuppressWarnings("rawtypes")
    public StringList getEquipmentRequest(Context context, String[] args) throws Exception {

        StringList includeList = new StringList();
        try {
            MapList mlEquipmentRequest = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_EQUIPMENT_REQUEST, TigerConstants.VAULT_ESERVICEPRODUCTION, null,
                    new StringList(DomainConstants.SELECT_ID));
            if (!mlEquipmentRequest.isEmpty()) {
                for (int i = 0; i < mlEquipmentRequest.size(); i++) {
                    Map Equipmentmap = (Map) mlEquipmentRequest.get(i);
                    String strEquipmentRequestId = (String) Equipmentmap.get(DomainConstants.SELECT_ID);
                    DomainObject domHarmonyRequest = DomainObject.newInstance(context, strEquipmentRequestId);
                    MapList mEquipmentList = domHarmonyRequest.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_REQUESTED_EQUIPMENT, // relationship pattern
                            TigerConstants.TYPE_VPMREFERENCE, // object pattern
                            new StringList(DomainConstants.SELECT_ID), // object selects
                            null, // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, // Postpattern
                            null, null, null);

                    if (mEquipmentList.isEmpty()) {
                        includeList.add(strEquipmentRequestId);
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getEquipmentRequest: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
        return includeList;
    }

    /**
     * this method is used to exclude the plant objects that are already connected with Equipment
     * @param context
     * @param args
     * @return List of Objects that are to be excluded
     * @throws Exception
     *             Exception appears, if error occurred
     */
    @SuppressWarnings("rawtypes")
    public StringList getPlantObjects(Context context, String[] args) throws Exception {
        StringList excludeListforPlants = new StringList();
        try {
            Map programMap = JPO.unpackArgs(args);
            String strEquipmentId = (String) programMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strEquipmentId)) {
                DomainObject domEquipment = DomainObject.newInstance(context, strEquipmentId);
                MapList mlConnectedPlants = domEquipment.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ASSOCIATED_PLANT, // relationship pattern
                        TigerConstants.TYPE_PSS_PLANT, // object pattern
                        new StringList(DomainConstants.SELECT_ID), // object selects
                        null, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // Postpattern
                        null, null, null);
                for (int i = 0; i < mlConnectedPlants.size(); i++) {
                    Map mPlantMap = (Map) mlConnectedPlants.get(i);
                    excludeListforPlants.add(mPlantMap.get(DomainConstants.SELECT_ID));
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getPlantObjects: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return excludeListforPlants;
    }

    /**
     * @param context
     * @param args
     * @throws Exception
     * @author vbhosale
     * @date 11/08/16
     */
    @SuppressWarnings("rawtypes")
    public static void connectEquipmentWithEuipmentRequest(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        // HashMap requestMap = (HashMap) programMap.get("requestMap");
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String strObjectId = (String) paramMap.get("objectId");
        String strNewObjectId = (String) paramMap.get("New OID");
        try {
            if (UIUtil.isNotNullAndNotEmpty(strObjectId) && UIUtil.isNotNullAndNotEmpty(strNewObjectId)) {
                DomainObject domEquipment = new DomainObject(strObjectId);
                String getObjectId = domEquipment.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_REQUESTED_EQUIPMENT + "].to.id");
                String getRelId = domEquipment.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_REQUESTED_EQUIPMENT + "].id");

                if (UIUtil.isNotNullAndNotEmpty(getObjectId) && UIUtil.isNotNullAndNotEmpty(getRelId)) {
                    if (!getObjectId.equalsIgnoreCase(strNewObjectId)) {
                        DomainRelationship.disconnect(context, getRelId, true);
                        DomainRelationship.connect(context, strObjectId, TigerConstants.RELATIONSHIP_PSS_REQUESTED_EQUIPMENT, strNewObjectId, true);
                    }
                } else {
                    DomainRelationship.connect(context, strObjectId, TigerConstants.RELATIONSHIP_PSS_REQUESTED_EQUIPMENT, strNewObjectId, true);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectEquipmentWithEuipmentRequest: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
    }

}
