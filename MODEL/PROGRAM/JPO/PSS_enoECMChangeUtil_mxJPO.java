
/*
 * * ${CLASS:PSS_enoECMChangeUtil}** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved.
 */
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import com.dassault_systemes.enovia.enterprisechangemgt.admin.ECMAdmin;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.componentcentral.CPCConstants;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.framework.ui.UICache;
import com.matrixone.apps.framework.ui.UIUtil;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.DateFormatUtil;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

/**
 * The <code>PSS_enoECMChangeUtil</code> class contains code for the "Part" business type.
 * @version ECM R215 - # Copyright (c) 1992-2015 Dassault Systemes.
 */
public class PSS_enoECMChangeUtil_mxJPO extends enoECMChangeUtilBase_mxJPO {
    /**
     *
     */
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_enoECMChangeUtil_mxJPO.class);

    private static final long serialVersionUID = -8149651804367039959L;

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @throws Exception
     *             if the operation fails.
     * @since ECM R215
     */

    public PSS_enoECMChangeUtil_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    // Addition for Tiger Faurecia - PCM stream by SGS
    public MapList getConnectedChanges(Context context, String[] args) throws Exception {
        MapList totalRelatedListCAs = new MapList();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strPartId = (String) paramMap.get(ChangeConstants.OBJECT_ID);

            String strType = "PSS_ChangeRequest";
            StringList stlObjectSelects = new StringList();
            stlObjectSelects.addElement(SELECT_ID);
            String changeRequestSelect = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + ChangeConstants.TYPE_CHANGE_REQUEST + "].id";
            String changeOrderSelect = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + strType + "].id";
            stlObjectSelects.addElement(changeRequestSelect);
            stlObjectSelects.addElement(changeOrderSelect);
            stlObjectSelects.addElement(SELECT_CURRENT);
            DomainObject newPart = new DomainObject(strPartId);
            Set changeActions = new HashSet();
            // Findbug Issue correction start
            // Date: 21/03/2017
            // By: Asha G.
            StringBuffer relPattern = new StringBuffer(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
            relPattern.append(",");
            relPattern.append(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);
            // Findbug Issue correction End
            MapList totalresultList = newPart.getRelatedObjects(context, relPattern.toString(), ChangeConstants.TYPE_CHANGE_ACTION, stlObjectSelects, new StringList(SELECT_RELATIONSHIP_ID), true,
                    false, (short) 1, null, null, 0);
            Iterator itr = totalresultList.iterator();
            // String sCAId,changeId,current;
            String sCAId, current;

            while (itr.hasNext()) {
                Map mplcaObject = (Map) itr.next();
                sCAId = (String) mplcaObject.get(SELECT_ID);
                current = (String) mplcaObject.get(SELECT_CURRENT);
                if (!changeActions.contains(sCAId)) {
                    if (!ChangeConstants.STATE_CHANGE_ACTION_PENDING.equalsIgnoreCase(current))
                        mplcaObject.put("disableSelection", "true");
                    totalRelatedListCAs.add(mplcaObject);
                    changeActions.add(sCAId);
                }
            }
        } catch (Exception ex) {

            // TIGTK-5405 - 17-04-2017 - PTE - START
            logger.error("Error in getConnectedChanges: ", ex);
            throw ex;
            // TIGTK-5405 - 17-04-2017 - PTE - End

        }
        return totalRelatedListCAs;
    }

    public void setInterfaceOnChangeAction(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            String strCAObjID = args[0];
            String strInterface = args[1];

            String strMQLCommand = "mod bus " + strCAObjID + " add interface " + strInterface;

            MqlUtil.mqlCommand(context, strMQLCommand);

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - PTE - START
            logger.error("Error in setInterfaceOnChangeAction: ", ex);
            // TIGTK-5405 - 17-04-2017 - PTE - End
            throw ex;
        }

    }

    /**
     * this method will set the attributes of affected items while user tries to use relationship Change Action
     * @param context
     * @param args
     * @throws Exception
     */
    public void setAttributesOnChangeAction(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            String strFromSideObjId = args[0];
            String strTosideObjeID = args[1];
            DomainObject objCOobj = new DomainObject(strFromSideObjId);
            DomainObject objCA = new DomainObject(strTosideObjeID);
            // TIGTK-14264 : Prakash : START
            DomainRelationship objRel = DomainRelationship.newInstance(context);
            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_TYPE);
            objSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
            Map objMap = objCOobj.getInfo(context, objSelects);
            String strType = (String) objMap.get(DomainConstants.SELECT_TYPE);
            String strDescription = (String) objMap.get(DomainConstants.SELECT_DESCRIPTION);

            if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(strType)) {
                String strCAAssignee;
                Date dateBeforetwoDays;
                StringList busSelects = new StringList();
                busSelects.add("from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].id");
                busSelects.add("from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].id");
                busSelects.add("from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.id");

                objCA.setDescription(context, strDescription);
                Map objCAMap = objCA.getInfo(context, busSelects);
                StringList slRelChangeAI = getListValue(objCAMap, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].id");
                StringList slRelImpI = getListValue(objCAMap, "from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].id");
                String strCAAssigneeId = (String) objCAMap.get("from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.id");

                if (!slRelChangeAI.isEmpty()) {
                    for (int i = 0; i < slRelChangeAI.size(); i++) {
                        objRel = DomainRelationship.newInstance(context, (String) slRelChangeAI.get(i));
                        objRel.setAttributeValue(context, DomainConstants.ATTRIBUTE_REASON_FOR_CHANGE, strDescription);
                    }
                }
                if (!slRelImpI.isEmpty()) {
                    for (int i = 0; i < slRelImpI.size(); i++) {
                        objRel = DomainRelationship.newInstance(context, (String) slRelImpI.get(i));
                        objRel.setAttributeValue(context, DomainConstants.ATTRIBUTE_REASON_FOR_CHANGE, strDescription);
                    }
                }
                // TIGTK-14264 : Prakash : END
                String strCOOriginator = objCOobj.getAttributeValue(context, DomainConstants.ATTRIBUTE_ORIGINATOR);
                if (strCAAssigneeId == null) {
                    strCAAssignee = null;

                } else {
                    DomainObject domAssignee = new DomainObject(strCAAssigneeId);
                    strCAAssignee = domAssignee.getInfo(context, DomainConstants.SELECT_NAME);
                }
                if ((strCAAssignee == null) || (strCAAssignee.equals("")) || (!strCAAssignee.equalsIgnoreCase(strCOOriginator))) {
                    BusinessObject boPerson = new BusinessObject(DomainConstants.TYPE_PERSON, strCOOriginator, "-", null);

                    if (boPerson.exists(context)) {
                        DomainRelationship.connect(context, objCA, ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE, new DomainObject(boPerson));
                    }
                }

                // Modify for TIGER Constants
                String strattrPSS_COVirtualImplementationDate = objCOobj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE);
                if (!ChangeUtil.isNullOrEmpty(strattrPSS_COVirtualImplementationDate)) {
                    Date dtPSS_COVirtualImplementationDate = new Date(strattrPSS_COVirtualImplementationDate);

                    int iday = dtPSS_COVirtualImplementationDate.getDay();
                    if (iday == 0) {

                        dateBeforetwoDays = new Date(dtPSS_COVirtualImplementationDate.getTime() - 3 * 24 * 3600 * 1000l);
                    } else if (iday == 1) {

                        dateBeforetwoDays = new Date(dtPSS_COVirtualImplementationDate.getTime() - 4 * 24 * 3600 * 1000l);
                    } else if (iday == 2) {

                        dateBeforetwoDays = new Date(dtPSS_COVirtualImplementationDate.getTime() - 4 * 24 * 3600 * 1000l);
                    } else {
                        dateBeforetwoDays = new Date(dtPSS_COVirtualImplementationDate.getTime() - 2 * 24 * 3600 * 1000l);
                    }
                    // TIGTK-14777 : START
                    Date date = new Date();
                    if (dateBeforetwoDays.before(date)) {
                        dateBeforetwoDays = dtPSS_COVirtualImplementationDate;
                    }
                    // TIGTK-14777 : END
                    String strdatebeoretwodays = DateFormatUtil.formatDate(context, dateBeforetwoDays);

                    // Modify for TIGER Constants
                    objCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PLANNEDENDDATE, strdatebeoretwodays);
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - PTE - START
            logger.error("Error in setAttributesOnChangeAction: ", ex);
            // TIGTK-5405 - 17-04-2017 - PTE - End
            throw ex;
        }

    }

    public void modifyPlannedEndDateOnChangeAction(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            String strCOObjId = args[0];
            String strCAId;

            Date dateBeforetwoDays;
            DomainObject domCOObject = new DomainObject(strCOObjId);

            String strattrPSS_COVirtualImplementationDate = domCOObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE);
            Date dtPSS_COVirtualImplementationDate = new Date(strattrPSS_COVirtualImplementationDate);

            StringList objectSelects = new StringList(1);
            objectSelects.addElement(SELECT_ID);
            StringList relSelects = new StringList(1);
            relSelects.addElement(SELECT_RELATIONSHIP_ID);

            MapList changeActionList = domCOObject.getRelatedObjects(context, // context
                    ChangeConstants.RELATIONSHIP_CHANGE_ACTION, // relationship pattern
                    ChangeConstants.TYPE_CHANGE_ACTION, // object pattern
                    objectSelects, // object selects
                    relSelects, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, // relationship where clause
                    (short) 0);

            for (int i = 0; i < changeActionList.size(); i++) {
                Map mCAObj = (Map) changeActionList.get(i);
                strCAId = (String) mCAObj.get(DomainConstants.SELECT_ID);
                DomainObject domCAObj = new DomainObject(strCAId);

                int iday = dtPSS_COVirtualImplementationDate.getDay();
                if (iday == 0) {

                    dateBeforetwoDays = new Date(dtPSS_COVirtualImplementationDate.getTime() - 3 * 24 * 3600 * 1000l);
                } else if (iday == 1) {

                    dateBeforetwoDays = new Date(dtPSS_COVirtualImplementationDate.getTime() - 4 * 24 * 3600 * 1000l);
                } else if (iday == 2) {

                    dateBeforetwoDays = new Date(dtPSS_COVirtualImplementationDate.getTime() - 4 * 24 * 3600 * 1000l);
                } else {
                    dateBeforetwoDays = new Date(dtPSS_COVirtualImplementationDate.getTime() - 2 * 24 * 3600 * 1000l);
                }
                // TIGTK-14777 : START
                Date date = new Date();
                if (dateBeforetwoDays.before(date)) {
                    dateBeforetwoDays = dtPSS_COVirtualImplementationDate;
                }
                // TIGTK-14777 : END
                String strdatebeoretwodays = DateFormatUtil.formatDate(context, dateBeforetwoDays);

                domCAObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PLANNEDENDDATE, strdatebeoretwodays);
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - PTE - START
            logger.error("Error in modifyPlannedEndDateOnChangeAction: ", ex);
            // TIGTK-5405 - 17-04-2017 - PTE - End
            throw ex;
        }

    }

    /**
     * this method will connect selected CR'S with CO
     * @param context
     * @param args
     *            .. packed array that consists of Change Order ObjectId and Object Id's of Change Request Objects...that are to be connected with CO
     * @return 'o' on SUCCESS.....and '1' on failure
     ** @throws Exception
     *             ....in case of any error.
     */
    public int connectCRtoCO(Context context, String args[]) throws Exception {
        String sChangeOrderId = "CO_ObjectId";
        String sChangeRequestId = "ObjId";
        String strCRID = "";
        try {
            // PCM Developed for Reduce code Redundancy | 20/09/16 : AB : START
            HashMap paramMap = (HashMap<String, String>) JPO.unpackArgs(args);
            String strChangeOrderId = (String) paramMap.get(sChangeOrderId);
            DomainObject domCO = DomainObject.newInstance(context, strChangeOrderId);
            String strCOCurrent = (String) domCO.getInfo(context, DomainConstants.SELECT_CURRENT);
            StringList strChangeRequestIdList = (StringList) paramMap.get(sChangeRequestId);
            if (!strChangeRequestIdList.isEmpty()) {
                strCRID = (String) strChangeRequestIdList.get(0);
            }

            if (TigerConstants.STATE_PSS_CHANGEORDER_INWORK.equalsIgnoreCase(strCOCurrent)) {
                DomainObject domCR = DomainObject.newInstance(context, strCRID);
                boolean bIsUserAgent = false;
                try {
                    ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    bIsUserAgent = true;
                    DomainRelationship domRelChangeOrder = DomainRelationship.connect(context, domCR, new RelationshipType(ChangeConstants.RELATIONSHIP_CHANGE_ORDER), domCO);
                    domRelChangeOrder.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_INWORKCONEWCRTAG, TigerConstants.ATTRIBUTE_PSS_INWORKCONEWCRTAG_RANGE_YES);
                    return 0;
                } catch (Exception ex) {
                    logger.error("Error in connectCRtoCO: ", ex);
                    return 1;
                } finally {
                    if (bIsUserAgent) {
                        ContextUtil.popContext(context);
                    }
                }
            } else {
                // set Parameter for CR & CO Id
                paramMap.clear();
                paramMap.put("ObjId", new StringList(strChangeOrderId));
                paramMap.put("CR_ObjectId", strCRID);

                // Connect CR to CO & Also connect Affected Item of CR to CO
                // This method is used for Connect All affected item of CR to Co when Add existing CR to CO Functionality Execute
                this.connectCOToCR(context, JPO.packArgs(paramMap));
                return 0;
            } // PCM Developed for Reduce code Redundancy | 20/09/16 : AB : END

        } catch (Exception ex) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 17-04-2017 - PTE - START
            logger.error("Error in connectCRtoCO: ", ex);
            // TIGTK-5405 - 17-04-2017 - PTE - End
            return 1;
        }
    }

    /**
     * this method will exclude all the CR objects that are already connected with CO
     * @param context
     * @param args
     * @return List of CR Object Id's that are already connected with CO and need to be excluded...
     * @throws Exception..in
     *             case of any error
     */
    public StringList excludeConnectedCR(Context context, String args[]) throws Exception {

        StringList excludeList = new StringList();
        try {
            HashMap<String, String> paramMap = (HashMap<String, String>) JPO.unpackArgs(args);
            String strChangeOrderId = (String) paramMap.get(ChangeConstants.OBJECT_ID);
            DomainObject domChangeOrder = newInstance(context, strChangeOrderId);
            StringList objectSle = new StringList(DomainConstants.SELECT_ID);
            // get all the CR objects that are connected with CO
            MapList mList = domChangeOrder.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEREQUEST, objectSle, null, true, false, (short) 1, null,
                    null, (short) 0);

            for (int i = 0; i < mList.size(); i++) {
                Map<String, String> tempMap = (Map<String, String>) mList.get(i);
                excludeList.add(tempMap.get(DomainConstants.SELECT_ID));
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - PTE - START
            logger.error("Error in excludeConnectedCR: ", ex);
            // TIGTK-5405 - 17-04-2017 - PTE - End
            throw ex;

        }
        return excludeList;

    }

    // modification for TS-0125 by Krushna - START
    /**
     * This method is used merge Affected Item in MCA
     * @param context
     * @param args
     * @return True or False
     * @throws Exception
     */
    public Boolean checkTypeOfMCAForSelectedItem(Context context, String[] args) throws Exception {
        Map paramMap = JPO.unpackArgs(args);

        boolean bReturnFlag = false;
        String strRelId = null;
        String strObjectId = null;
        StringList selectedCAItemsList = (StringList) paramMap.get("selectedItemsList");
        String strMCOObjectId = (String) paramMap.get("objectId");
        try {
            ContextUtil.startTransaction(context, true);
            DomainObject domMCO = DomainObject.newInstance(context, strMCOObjectId);
            DomainObject domObjMCA = new DomainObject();
            if (selectedCAItemsList != null) {
                String strObjGeneratorName = UICache.getObjectGenerator(context, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, "");
                String strName = DomainObject.getAutoGeneratedName(context, strObjGeneratorName, "");

                domObjMCA.createObject(context, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, strName, "-", TigerConstants.POLICY_PSS_MANUFACTURINGCHANGEACTION,
                        TigerConstants.VAULT_ESERVICEPRODUCTION);
                // strNewMCAObjId = domObjMCA.getObjectId();
            }

            StringList objectSelects = new StringList(6);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_TYPE);
            StringList relSelects = new StringList(6);
            relSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            // Added : 07/11/2016
            if (selectedCAItemsList != null && selectedCAItemsList.size() != 0) {
                // Added : 07/11/2016
                for (int i = 0; i < selectedCAItemsList.size(); i++) {
                    String strMCAID = (String) selectedCAItemsList.get(i);
                    DomainObject domMCA = DomainObject.newInstance(context, strMCAID);
                    MapList mlAffectedItemList = domMCA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, "*", objectSelects, relSelects, false, true,
                            (short) 0, "", "", (short) 0);
                    if (mlAffectedItemList != null)
                        for (int j = 0; j < mlAffectedItemList.size(); j++) {
                            Map mapAffectedItem = (Map) mlAffectedItemList.get(j);
                            strRelId = (String) mapAffectedItem.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                            strObjectId = (String) mapAffectedItem.get(DomainConstants.SELECT_ID);
                            DomainObject domAffectedItem = DomainObject.newInstance(context, strObjectId);
                            domObjMCA.connectTo(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, domAffectedItem);

                            DomainRelationship.disconnect(context, strRelId, true);
                        }
                }
            }
            domObjMCA.connectFrom(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, domMCO);
            bReturnFlag = true;
            ContextUtil.commitTransaction(context);
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - PTE - START
            logger.error("Error in checkTypeOfMCAForSelectedItem: ", ex);
            // TIGTK-5405 - 17-04-2017 - PTE - End
            ContextUtil.abortTransaction(context);
            throw ex;
        }
        return bReturnFlag;
    }

    // modification for TS-0125 by Krushna - END

    // PCM RFC-074 : 28/09/2016 : KWagh : Start
    /**
     * This method is used for Check Type (ENG,DEC etc) on basis of functionality of CA for Affected Item
     * @param context
     * @param args
     * @return True or False
     * @throws Exception
     */
    public Boolean checkTypeOfCAForSelectedItem(Context context, String[] args) throws Exception {
        // PCM TIGTK-4139: 14/02/2017 : KWagh : START
        Boolean bResult = true;
        try {
            Map paramMap = JPO.unpackArgs(args);
            MapList infoList = new MapList();
            HashSet<String> sCAType = new HashSet<>();

            String functionality = (String) paramMap.get("functionality");
            StringList selectedCAItemsList = (StringList) paramMap.get("selectedItemsList");

            String SELECT_CHANGE_AFFECTED_ITEM = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.";

            if ("MergeCA".equals(functionality)) {
                // Findbug Issue correction start
                // Date: 21/03/2017
                // By: Asha G.
                StringList slIDList = null;
                StringList slPolicyList = null;
                StringList slTypeList = null;
                // Findbug Issue correction End

                int nCASize = selectedCAItemsList.size();
                for (int i = 0; i < nCASize; i++) {

                    String strCAID = (String) selectedCAItemsList.get(i);

                    DomainObject domCA = new DomainObject(strCAID);

                    String strPSS_CAType = domCA.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE);

                    sCAType.add(strPSS_CAType);

                    slIDList = domCA.getInfoList(context, SELECT_CHANGE_AFFECTED_ITEM + SELECT_ID);
                    slTypeList = domCA.getInfoList(context, SELECT_CHANGE_AFFECTED_ITEM + SELECT_TYPE);
                    slPolicyList = domCA.getInfoList(context, SELECT_CHANGE_AFFECTED_ITEM + SELECT_POLICY);

                    HashMap attMap = new HashMap<>();

                    attMap.put(SELECT_CHANGE_AFFECTED_ITEM + SELECT_ID, slIDList.get(0));
                    attMap.put(SELECT_CHANGE_AFFECTED_ITEM + SELECT_TYPE, slTypeList.get(0));
                    attMap.put(SELECT_CHANGE_AFFECTED_ITEM + SELECT_POLICY, slPolicyList.get(0));

                    infoList.add(attMap);

                }

            }
            // PCM RFC-074 : 28/09/2016 : KWagh : End
            if (!"MergeCA".equals(functionality)) {
                String[] arrayOfAffectedItemIds = new String[selectedCAItemsList.size()];
                arrayOfAffectedItemIds = (String[]) selectedCAItemsList.toArray(arrayOfAffectedItemIds);
                StringList busSelect = new StringList(3);
                busSelect.add(SELECT_TYPE);
                busSelect.add(SELECT_POLICY);
                infoList = DomainObject.getInfo(context, arrayOfAffectedItemIds, busSelect);
            }
            bResult = checkTypeOfCAForSelectedItem(context, infoList);

            if (sCAType.size() > 1 && sCAType.contains("Standard")) {
                bResult = false;
            }

            // PCM TIGTK-4139: 14/02/2017 : KWagh : END
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - PTE - START
            logger.error("Error in checkTypeOfCAForSelectedItem: ", ex);
            throw ex;
            // TIGTK-5405 - 17-04-2017 - PTE - End

        }
        return bResult;
    }

    // ALM defect -1684 :multiple standard part not added on CO:2/12/2016:Rutuja Ekatpure:start
    /**
     * This method is used for Get CA List of same Type of Affected Items
     * @param context
     * @param args
     * @return True or False
     * @throws Exception
     */
    public StringList CAOfSameTypeAffectedItem(Context context, String[] args) throws Exception {
        StringList slCAExcludeList = new StringList();
        try {
            StringList slErrorReturn = new StringList();
            slErrorReturn.add("Error");
            String strSelectedCAType = "";
            String strErrorMsg = "";
            Map paramMap = JPO.unpackArgs(args);
            StringList caIdsList = (StringList) paramMap.get("caIdsList");

            DomainObject domCA = new DomainObject((String) caIdsList.get(0));

            StringList slObjectSel = new StringList(2);
            slObjectSel.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");
            slObjectSel.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");

            Map mCAInfo = domCA.getInfo(context, slObjectSel);
            // get the type of selected affected items CA
            strSelectedCAType = (String) mCAInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");

            // get CO connected
            String strCOID = (String) mCAInfo.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");

            for (int i = 0; i < caIdsList.size(); i++) {
                String strSelectedCAId = (String) caIdsList.get(i);
                // exclude CA related to selected item
                if (!slCAExcludeList.contains(strSelectedCAId)) {
                    slCAExcludeList.add(strSelectedCAId);
                }
                DomainObject domCAObj = new DomainObject(strSelectedCAId);
                String strCAType = domCAObj.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");
                // if type mismatch when more than one item selected to move then throw error
                if (!strSelectedCAType.equals(strCAType)) {
                    strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                            "PSS_EnterpriseChangeMgt.Alert.TypeOfSelectedItemNotSameErrorMsg");
                    slErrorReturn.add(strErrorMsg);
                    return slErrorReturn;
                }
            }
            StringList slObjectSlect = new StringList(2);
            slObjectSlect.addElement(DomainConstants.SELECT_ID);
            slObjectSlect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");

            DomainObject domCO = new DomainObject(strCOID);
            MapList mlChangeAction = domCO.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, slObjectSlect, null, false, true, (short) 1, null,
                    null, 0);

            // iterate list of CA for type check to exclude from list
            for (int cnt = 0; cnt < mlChangeAction.size(); cnt++) {
                String strCAID = (String) ((Map) mlChangeAction.get(cnt)).get(DomainObject.SELECT_ID);
                String strCAType = (String) ((Map) mlChangeAction.get(cnt)).get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");
                // exclude CA having different CA type than selected item
                if (!caIdsList.contains(strCAID)) {
                    if (!strSelectedCAType.equals(strCAType)) {
                        slCAExcludeList.add(strCAID);
                    }
                }
            }
            // if all CA is in excude list then throw error
            if (mlChangeAction.size() == slCAExcludeList.size()) {
                strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.NoMatchingCAErrorMsg");
                slErrorReturn.add(strErrorMsg);
                return slErrorReturn;
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in CAOfSameTypeAffectedItem: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return slCAExcludeList;
    }

    // ALM defect -1684 :multiple standard part not added on CO:2/12/2016:Rutuja Ekatpure:End
    /**
     * This method is used for get Groupe name (ENG,DEC etc).
     * @param context
     * @param args
     *            MapList of Type & policy
     * @return True or False
     * @throws Exception
     */
    public Boolean checkTypeOfCAForSelectedItem(Context context, MapList infoList) throws Exception {
        // PCM RFC-074 : 28/09/2016 : KWagh : Start
        HashSet<String> groupNameSet = new HashSet<String>(infoList.size());
        try {
            Map attMap;
            String SELECT_CHANGE_AFFECTED_ITEM = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.";
            com.dassault_systemes.enovia.enterprisechangemgt.admin.ECMConfigurationData emcConfigData = com.dassault_systemes.enovia.enterprisechangemgt.admin.ECMConfigurationData
                    .getInstance(context);
            // Group Name
            for (Object object : infoList) {
                attMap = (Map) object;
                groupNameSet.add(emcConfigData.getGroupName(context, (String) attMap.get(SELECT_CHANGE_AFFECTED_ITEM + SELECT_TYPE), (String) attMap.get(SELECT_CHANGE_AFFECTED_ITEM + SELECT_POLICY)));
                if (groupNameSet.size() > 1)
                    break;
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in checkTypeOfCAForSelectedItem: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        if (groupNameSet.size() > 1) {
            return Boolean.valueOf(false);
        } else {

            return Boolean.valueOf(true);
        }
        // PCM RFC-074 : 28/09/2016 : KWagh : End
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             List of CO Object Id's that are already connected with CR and need to be excluded...
     */
    public StringList excludeConnectedCO(Context context, String[] args) throws Exception {
        StringList slCOObjIds = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCRObjectId = (String) programMap.get("objectId");

            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeOrder");
            String typePattern = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeOrder");

            StringList slObjectSle = new StringList(2);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            DomainObject domCRObj = new DomainObject(strCRObjectId);
            MapList mlChangeOrder = domCRObj.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null);

            if (mlChangeOrder.size() != 0) {
                for (int j = 0; j < mlChangeOrder.size(); j++) {
                    Map mCOOBJ = (Map) mlChangeOrder.get(j);
                    String sCOObjID = (String) mCOOBJ.get(DomainConstants.SELECT_ID);
                    slCOObjIds.add(sCOObjID); // Add Change Order Objects Id to StringList
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in excludeConnectedCO: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return slCOObjIds;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method connects the CR to the affected Item.
     */
    // RFC-033-AB-IT12
    public Map connectCRToAffectedItem(Context context, String[] args) throws Exception {
        try {
            // PCM TIGTK-3921: 12/01/2017 : KWagh : START
            Map mpAffectedItems = new HashMap();
            String strAffectedItemId;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCRId = (String) programMap.get("busObjId");
            StringList slselectedItemsList = (StringList) programMap.get("selectedItemsList");

            StringList slSymmetricalList = this.getSymmetricalPartForCR(context, slselectedItemsList);
            slselectedItemsList.addAll(slSymmetricalList);
            StringList slCADList = this.getCADObjectsForCR(context, slselectedItemsList);
            slselectedItemsList.addAll(slCADList);

            PSS_enoECMChangeOrder_mxJPO ChangeOrderBase = new PSS_enoECMChangeOrder_mxJPO(context, args);
            StringList slAffectedItems = ChangeOrderBase.getUniqueIdList(slselectedItemsList);

            DomainObject domCRObj = new DomainObject(strCRId);
            StringList slCRAffectedItems = domCRObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");

            for (int i = 0; i < slAffectedItems.size(); i++) {
                strAffectedItemId = (String) slAffectedItems.get(i);

                if (UIUtil.isNotNullAndNotEmpty(strCRId) && UIUtil.isNotNullAndNotEmpty(strAffectedItemId)) {

                    if (!slCRAffectedItems.contains(strAffectedItemId)) {
                        DomainObject domAffectedItem = new DomainObject(strAffectedItemId);

                        StringList objSelectlist = new StringList();
                        objSelectlist.add(DomainConstants.SELECT_CURRENT);
                        objSelectlist.add(DomainConstants.SELECT_POLICY);

                        Map mAfeectedItem = domAffectedItem.getInfo(context, objSelectlist);
                        String strItemState = (String) mAfeectedItem.get(DomainConstants.SELECT_CURRENT);
                        // PCM TIGTK-3446 : 20/10/2016 : KWagh : Start
                        String strItemPolicy = (String) mAfeectedItem.get(DomainConstants.SELECT_POLICY);

                        // Check If Already That Relationship Exist or not

                        // PCM : TIGTK-6853 : 24/04/2017 : AB : START
                        DomainRelationship domRel;
                        // PCM : TIGTK-9132 : 26/07/2017 : AB : START
                        PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, null);
                        boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domAffectedItem, domCRObj);

                        if (TigerConstants.POLICY_PSS_MBOM.equalsIgnoreCase(strItemPolicy) && TigerConstants.STATE_MBOM_RELEASED.equalsIgnoreCase(strItemState) || bolAllowPushPop) {
                            // PCM : TIGTK-9132 : 26/07/2017 : AB : END
                            try {
                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                domRel = DomainRelationship.connect(context, domCRObj, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, domAffectedItem);
                            } finally {
                                ContextUtil.popContext(context);
                            }
                        } else {
                            domRel = DomainRelationship.connect(context, domCRObj, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, domAffectedItem);
                        }
                        // PCM : TIGTK-6853 : 24/04/2017 : AB : END
                        if (!strItemPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_Legacy_CAD)) {
                            if (strItemState.equals("Released") || strItemState.equals("Release") || strItemState.equals(ChangeConstants.STATE_DEVELOPMENT_PART_COMPLETE)) {
                                // PCM:PHASE1.1: TIGTK-9293 : PSE : 31-08-2017 : START
                                boolean isContextPushed = false;
                                try {
                                    if (strItemPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART) && bolAllowPushPop) {
                                        ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                        isContextPushed = true;
                                    }
                                    domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_REVISE);
                                } catch (Exception ex) {
                                    logger.error("PSS_enoECMChangeUtil:connectCRToAffectedItem : Error while set Requested Change attribute : ", ex);
                                    throw ex;
                                } finally {
                                    if (isContextPushed) {
                                        ContextUtil.popContext(context);
                                    }
                                }
                                // PCM:PHASE1.1: TIGTK-9293 : PSE : 31-08-2017 : END
                            } else if (strItemState.equals(TigerConstants.STATE_PSS_CANCELPART_CANCELLED) && strItemState.equals(TigerConstants.STATE_PSS_CANCELCAD_CANCELLED)) {
                                domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_NONE);

                            } else {
                                domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_RELEASE);
                            }
                        } else {

                            if (strItemState.equals("Released")) {
                                domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_NONE);
                            } else {
                                domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_RELEASE);
                            }

                        }
                        // }
                        // PCM TIGTK-3446 : 20/10/2016 : KWagh : End
                        // PCM TIGTK-2965 | RFC-XXX : 06/09/16 : AB : END
                    }
                }
                mpAffectedItems.put("strAffectedItemId", strAffectedItemId);

                // PCM TIGTK-3921: 12/01/2017 : KWagh : End
            }
            return mpAffectedItems;
        } catch (Exception ex) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in connectCRToAffectedItem: ", ex);
            // TIGTK-5405 - 18-04-2017 - PTE - End
            // PCM : TIGTK-9132 : 24/07/2017 : AB : START
            MqlUtil.mqlCommand(context, "Notice $1", ex.getMessage());
            // PCM : TIGTK-9132 : 24/07/2017 : AB : START
            throw (ex);
        }
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public MapList getAffectedItems(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCRId = (String) programMap.get("objectId");
            DomainObject domCR = new DomainObject(strCRId);
            MapList mlTableData = new MapList();

            StringList objectSelects = new StringList(6);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_TYPE);

            StringList relSelects = new StringList(1);
            relSelects.addElement(DomainRelationship.SELECT_ID);

            // Get Affected Items connected with CR
            MapList mlAffectedItemsList = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, "*", objectSelects, relSelects, false, true, (short) 1, "", "", (short) 0);

            if (!mlAffectedItemsList.isEmpty()) {
                for (int j = 0; j < mlAffectedItemsList.size(); j++) {
                    Map mAffectedItemObj = (Map) mlAffectedItemsList.get(j);

                    mlTableData.add(mAffectedItemObj);
                }
            }
            return mlTableData;
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getAffectedItems: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
    }

    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeAffectedItems(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strChangeId = (String) programMap.get("objectId");
        StringList strlAffItemList = new StringList();
        if (ChangeUtil.isNullOrEmpty(strChangeId))
            return strlAffItemList;

        try {
            setId(strChangeId);
            String relPattern = PropertyUtil.getSchemaProperty("relationship_PSS_AffectedItem");
            MapList resultList = null;
            Map map = null;

            resultList = getRelatedObjects(context, relPattern, "*", new StringList(DomainObject.SELECT_ID), null, false, true, (short) 2, "", "");

            Iterator itr = resultList.iterator();
            while (itr.hasNext()) {
                map = (Map) itr.next();
                strlAffItemList.addElement((String) map.get(DomainObject.SELECT_ID));
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in excludeAffectedItems: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return strlAffItemList;
    }

    // TIGTK-3120 :Rutuja Ekatpure :19/09/2016:Start
    // Connect Change Request to Change Action
    /**
     * This method is used for part and CAD Part in Change Request Modified by PCM : TIGTK-3973 : 24/01/2017 : AB
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Map addChangeRequestToPart(Context context, String[] args) throws Exception {
        try {

            Map mpCRs = new HashMap();
            StringList slselectedItemsList = new StringList();
            String strCRId;

            // PCM : TIGTK-6419 & 6447 : 13/04/2017 : AB : START
            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_CURRENT);
            objectSelects.add(DomainConstants.SELECT_TYPE);

            StringList slAllowedManufaturingType = new StringList();
            slAllowedManufaturingType.add(TigerConstants.TYPE_CREATEASSEMBLY);
            slAllowedManufaturingType.add(TigerConstants.TYPE_CREATEKIT);
            slAllowedManufaturingType.add(TigerConstants.TYPE_CREATEMATERIAL);
            slAllowedManufaturingType.add(DomainConstants.TYPE_PART);
            // PCM : TIGTK-6419 & 6447 : 13/04/2017 : AB : END

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strPartId = DomainConstants.EMPTY_STRING;
            StringList slPartId = (StringList) programMap.get("affeItemList");
            StringList slCRList = (StringList) programMap.get("slCRList");
            for (int j = 0; j < slPartId.size(); j++) {

                strPartId = (String) slPartId.get(j);
                if (UIUtil.isNotNullAndNotEmpty(strPartId)) {
                    // For Get the Symmetrical Part of related Current Part
                    StringList slSelectedItemsWithSymmetrical = this.getSymmetricalPartForCR(context, slPartId);

                    slselectedItemsList.addAll(slPartId);
                    slselectedItemsList.addAll(slSelectedItemsWithSymmetrical);

                    // For Get the CAD part of current part and Symmetrical Part of related Current Part
                    StringList slSelectedItemsWithPartsSymmetricalAndCAD = this.getCADObjectsForCR(context, slselectedItemsList);

                    slselectedItemsList.addAll(slSelectedItemsWithPartsSymmetricalAndCAD);

                }
            }

            // Get unique list of Affected Items
            PSS_enoECMChangeOrder_mxJPO ChangeOrderBase = new PSS_enoECMChangeOrder_mxJPO(context, args);
            StringList slAffectedItems = ChangeOrderBase.getUniqueIdList(slselectedItemsList);

            for (int i = 0; i < slCRList.size(); i++) {
                strCRId = (String) slCRList.get(i);
                DomainObject domCRObj = new DomainObject(strCRId);

                Iterator itrAffectedItem = slAffectedItems.iterator();
                while (itrAffectedItem.hasNext()) {

                    String strRelatedPart = (String) itrAffectedItem.next();
                    DomainObject domRelatedPart = DomainObject.newInstance(context, strRelatedPart);
                    // PCM TIGTK-4098 | 01/02/2017 : AB : START
                    // Note : here, If multiple CR connected with Part then GetInfo is not working properly. so we have to call two different call for get the state and connected CR
                    StringList slCRConnectedToPart = domRelatedPart.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.id");

                    // PCM : TIGTK-6419 & 6447 : 13/04/2017 : AB : START
                    Map mapItemInfo = (Map) domRelatedPart.getInfo(context, objectSelects);
                    String strItemState = (String) mapItemInfo.get(DomainConstants.SELECT_CURRENT);
                    String strItemType = (String) mapItemInfo.get(DomainConstants.SELECT_TYPE);

                    // PCM TIGTK-7908 | 29/05/2017 : AB : START
                    if (!slCRConnectedToPart.contains(strCRId) && (slAllowedManufaturingType.contains(strItemType) || domRelatedPart.isKindOf(context, TigerConstants.TYPE_MCAD_MODEL)
                            || domRelatedPart.isKindOf(context, TigerConstants.TYPE_MCADDRAWING))) {
                        // PCM TIGTK-7908 | 29/05/2017 : AB : END
                        // PCM : TIGTK-6419 & 6447 : 13/04/2017 : AB : END
                        // PCM TIGTK-4098 | 01/02/2017 : AB : START

                        // PCM : TIGTK-9132 : 26/07/2017 : AB : START
                        PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, null);
                        boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domRelatedPart, domCRObj);
                        // Connect Part & Connected Symmetrical Part & Connected
                        // CAD Part with Change Request

                        boolean isPushedContext = false;
                        if (bolAllowPushPop) {
                            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            isPushedContext = true;
                        }

                        try {
                            DomainRelationship domRel = DomainRelationship.connect(context, domCRObj, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, domRelatedPart);
                            // PCM TIGTK-3054 | 20/09/16 : AB : START
                            if (strItemState.equals("Released") || strItemState.equals("Release") || strItemState.equals(ChangeConstants.STATE_DEVELOPMENT_PART_COMPLETE)) {
                                domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_REVISE);
                            } else {
                                domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_RELEASE);
                            }
                            // PCM TIGTK-3054 | 20/09/16 : AB : START
                        } finally {
                            if (isPushedContext) {
                                ContextUtil.popContext(context);
                            }
                        }
                        // PCM : TIGTK-9132 : 26/07/2017 : AB : END
                    }

                }
                mpCRs.put("strCRId", strCRId);
            }
            // PCM TIGTK-3609 : 11/11/2016 : KWagh : End
            return mpCRs;
        } catch (Exception ex) {
            // PCM : TIGTK-9132 : 24/07/2017 : AB : START
            MqlUtil.mqlCommand(context, "Notice $1", ex.getMessage());
            // PCM : TIGTK-9132 : 24/07/2017 : AB : END
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in addChangeRequestToPart: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
    }

    // TIGTK-3120 :Rutuja Ekatpure :19/09/2016:End
    // Connect Change Order and Change Action
    public void addNewChangeRequestToPart(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strPartId = (String) requestMap.get("objectId");
            String strCRId = (String) paramMap.get("objectId");
            if ((strCRId.equalsIgnoreCase("") || strCRId.equalsIgnoreCase(null)) && (strPartId.equalsIgnoreCase("") || strPartId.equalsIgnoreCase(null))) {
            } else {
                DomainObject domCRObj = new DomainObject(strCRId);
                DomainObject domPart = new DomainObject(strPartId);

                // PCM : TIGTK-9132 : 26/07/2017 : AB : START
                PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, null);
                boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domPart, domCRObj);

                boolean isPushedContext = false;
                if (bolAllowPushPop) {
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    isPushedContext = true;
                }

                try {
                    DomainRelationship.connect(context, domCRObj, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, domPart);

                } finally {
                    if (isPushedContext) {
                        ContextUtil.popContext(context);
                    }
                }
                // PCM : TIGTK-9132 : 26/07/2017 : AB : END

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in addNewChangeRequestToPart: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
    }

    public MapList getConnectedChangeRequests(Context context, String[] args) throws Exception {
        MapList totalRelatedListCRs = new MapList();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strPartId = (String) paramMap.get(ChangeConstants.OBJECT_ID);
            StringList stlObjectSelects = new StringList();
            stlObjectSelects.addElement(SELECT_ID);
            stlObjectSelects.addElement(SELECT_CURRENT);
            DomainObject newPart = new DomainObject(strPartId);
            String relPattern = PropertyUtil.getSchemaProperty("relationship_PSS_AffectedItem");
            String strType = PropertyUtil.getSchemaProperty("type_PSS_ChangeRequest");
            MapList totalresultList = newPart.getRelatedObjects(context, relPattern, strType, stlObjectSelects, new StringList(SELECT_RELATIONSHIP_ID), true, false, (short) 1, null, null, 0);
            Iterator itr = totalresultList.iterator();
            while (itr.hasNext()) {
                Map mplcrObject = (Map) itr.next();
                totalRelatedListCRs.add(mplcrObject);
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getConnectedChangeRequests: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }

        return totalRelatedListCRs;
    }

    public int connectAffectedItemtoCR(Context context, String args[]) throws Exception {
        int result = 0;
        try {
            PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, null);
            String strRelID;
            StringList slobjSelects = new StringList();
            slobjSelects.add(DomainConstants.SELECT_ID);
            slobjSelects.add(DomainConstants.SELECT_TYPE);
            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            StringList affeItemList = (StringList) programMap.get("affeItemList");
            StringList lstnewCRIdsToBeConnected = (StringList) programMap.get("lstnewCRIdsToBeConnected");
            String strOldCRID = (String) programMap.get("strOldCRID");
            DomainObject domOLdCR = DomainObject.newInstance(context, strOldCRID);
            MapList mlConnectedObj = domOLdCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, "*", slObjectSle, slRelSle, false, true, (short) 1, null, null);
            if (!mlConnectedObj.isEmpty()) {
                for (int d = 0; d < mlConnectedObj.size(); d++) {
                    Map mObj = (Map) mlConnectedObj.get(d);
                    strRelID = (String) mObj.get(DomainRelationship.SELECT_RELATIONSHIP_ID);
                    DomainRelationship.disconnect(context, strRelID);
                }
            }
            for (int index = 0; index < lstnewCRIdsToBeConnected.size(); index++) {
                String sCRID = (String) lstnewCRIdsToBeConnected.get(index);
                DomainObject domNewCR = new DomainObject(sCRID);
                for (int a = 0; a < affeItemList.size(); a++) {
                    String sAffectedItems = (String) affeItemList.get(a);
                    DomainObject domAffectedItem = new DomainObject(sAffectedItems);
                    MapList mlAIRelatedCA = domAffectedItem.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, // relationship pattern
                            DomainConstants.QUERY_WILDCARD, // object pattern
                            slobjSelects, // object selects
                            null, // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 0, // recursion level
                            null, // object where clause
                            null, 0);
                    if (mlAIRelatedCA.isEmpty()) {

                        // PCM : TIGTK-9132 : 26/07/2017 : AB : START
                        boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domAffectedItem, domNewCR);
                        boolean isPushedContext = false;
                        if (bolAllowPushPop) {
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            isPushedContext = true;
                        }

                        try {
                            DomainRelationship.connect(context, domNewCR, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, domAffectedItem);
                        } finally {
                            if (isPushedContext) {
                                ContextUtil.popContext(context);
                            }
                        }
                        // PCM : TIGTK-9132 : 26/07/2017 : AB : END
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in connectAffectedItemtoCR: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return result;
    }

    /**
     * Description : This method is modified for Add related symmetrical part and CAD Part in Change Request (TIGTK-2297 & TIGTK-2298) This method Modified by PCM : TIGTK-3973 : 20/01/2017 : AB
     * @author abhalani
     * @args CR Id & Part Id
     * @Date Jul 19, 2016
     */

    public void addChangeRequestToPartfromChangeMgnt(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCRId = (String) programMap.get("targetObjId");
            DomainObject domCR = new DomainObject(strCRId);
            StringList listAffectedItem = (StringList) programMap.get("affeItemList");

            // Get the related Symmetrical part of related Current affected Item
            StringList slSymmetricalParts = this.getSymmetricalPartForCR(context, listAffectedItem);

            StringList slselectedItemsList = new StringList();
            slselectedItemsList.addAll(listAffectedItem);
            slselectedItemsList.addAll(slSymmetricalParts);

            // Get the CAD Part of related Current affected Item
            StringList slCADList = this.getCADObjectsForCR(context, slselectedItemsList);
            slselectedItemsList.addAll(slCADList);

            // Get unique list of Affected Items
            PSS_enoECMChangeOrder_mxJPO ChangeOrderBase = new PSS_enoECMChangeOrder_mxJPO(context, args);
            StringList slAffectedItems = ChangeOrderBase.getUniqueIdList(slselectedItemsList);

            if (slAffectedItems.size() != 0) {
                PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, null);
                for (int k = 0; k < slAffectedItems.size(); k++) {
                    String strRelatedPart = (String) slAffectedItems.get(k);

                    DomainObject domRelatedPart = new DomainObject(strRelatedPart);
                    // PCM TIGTK-4098 | 01/02/2017 : AB : START
                    // Note : here, If multiple CR connected with Part then GetInfo is not working properly. so we have to call two different call for get the state and connected CR
                    StringList slCRConnectedToPart = domRelatedPart.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.id");
                    String strItemState = domRelatedPart.getInfo(context, DomainConstants.SELECT_CURRENT);

                    if (!slCRConnectedToPart.contains(strCRId)) {
                        // PCM TIGTK-4098 | 01/02/2017: AB : END
                        // Connect Part & Connected Symmetrical Part & Connected CAD Part with Change Request

                        // PCM : TIGTK-9132 : 26/07/2017 : AB : START
                        boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domRelatedPart, domCR);
                        boolean isPushedContext = false;
                        if (bolAllowPushPop) {
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            isPushedContext = true;
                        }

                        try {
                            DomainRelationship domRel = DomainRelationship.connect(context, domCR, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, domRelatedPart);
                            // PCM TIGTK-3054 | 20/09/16 : AB : START
                            // PCM TIGTK-8082 | 25/05/17 : TS : START
                            if (strItemState.equals(TigerConstants.STATE_RELEASED_CAD_OBJECT) || strItemState.equals(TigerConstants.STATE_PART_RELEASE)
                                    || strItemState.equals(ChangeConstants.STATE_DEVELOPMENT_PART_COMPLETE)) {
                                // PCM TIGTK-8082 | 25/05/17 : TS : END
                                domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_REVISE);
                            } else {
                                domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_RELEASE);
                            }
                            // PCM TIGTK-3054 | 20/09/16 : AB : END
                        } finally {
                            if (isPushedContext) {
                                ContextUtil.popContext(context);
                            }
                        }
                        // PCM : TIGTK-9132 : 26/07/2017 : AB : END
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            // PCM : TIGTK-9132 : 24/07/2017 : AB : START
            MqlUtil.mqlCommand(context, "Notice $1", ex.getMessage());
            // PCM : TIGTK-9132 : 24/07/2017 : AB : END
            logger.error("Error in addChangeRequestToPartfromChangeMgnt: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
    }

    // Added for TS124
    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList includeConnectedMCAObjects(Context context, String[] args) throws Exception {
        // Findbug Issue correction start
        // Date: 21/03/2017
        // By: Asha G.
        StringList slMCAIds = null;
        // Findbug Issue correction End
        Map programMap = (Map) JPO.unpackArgs(args);
        try {
            String strMCOId = (String) programMap.get("objectId");
            Pattern relPatternMfgChangeAffectedItem = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM);
            Pattern typePatternMfgChangeAction = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION);

            String strSelectedAffectedItem = (String) programMap.get("slSelectedAffectedItem");
            String strAfctedItemsIds = strSelectedAffectedItem.substring(1, strSelectedAffectedItem.length() - 1);
            String[] strSelectedAffectedItems = strAfctedItemsIds.split(",");
            int nSelectedAffectedItemCount = strSelectedAffectedItems.length;
            DomainObject doMCOObj = new DomainObject(strMCOId);
            slMCAIds = doMCOObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.id");
            String strWhere = "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "]==True";
            for (int i = 0; i < nSelectedAffectedItemCount; i++) {
                String strAffectedItemId = (String) strSelectedAffectedItems[i];
                DomainObject doAffectedItemObj = new DomainObject(strAffectedItemId);
                StringList slobjSelects = new StringList();
                slobjSelects.addElement(DomainConstants.SELECT_ID);
                MapList mlRelatedMCA = doAffectedItemObj.getRelatedObjects(context, relPatternMfgChangeAffectedItem.getPattern(), // relationship pattern
                        typePatternMfgChangeAction.getPattern(), // object pattern
                        slobjSelects, // object selects
                        null, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 0, // recursion level
                        strWhere, // object where clause
                        null, 0);
                Map mRelatedMCA = (Map) mlRelatedMCA.get(0);
                String strMCAId = (String) mRelatedMCA.get(DomainConstants.SELECT_ID);
                if (slMCAIds.contains(strMCAId)) {
                    slMCAIds.remove(strMCAId);
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in includeConnectedMCAObjects: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return slMCAIds;
    }

    public Vector getProjectLeadChangeManager(Context context, String[] args) throws Exception {
        Vector vProjectCountList = new Vector();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList objIdList = (MapList) paramMap.get("objectList");
        try {
            for (int i = 0; i < objIdList.size(); i++) {
                String strPerson = DomainConstants.EMPTY_STRING;

                StringList busSelects = new StringList();
                StringList relSelects = new StringList();
                busSelects.add(DomainObject.SELECT_ID);
                busSelects.add(DomainObject.SELECT_TYPE);
                busSelects.add(DomainObject.SELECT_NAME);
                relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);
                // TIGTK-5890 : PTE : 4/7/2017 : START
                // PCM : TIGTK-7695 : 11/05/2017 : AB : START
                relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]");
                relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_POSITION + "]");
                // PCM : TIGTK-7695 : 11/05/2017 : AB : END
                // TIGTK-5890 : PTE : 4/7/2017 : End
                DomainObject domProject = new DomainObject(((Map) objIdList.get(i)).get(SELECT_ID) + "");
                MapList mapPerson = domProject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, DomainConstants.TYPE_PERSON, busSelects, relSelects, true, true, (short) 1,
                        null, null, 0);
                for (int intIndex = 0; intIndex < mapPerson.size(); intIndex++) {
                    Map currentMap = (Map) mapPerson.get(intIndex);
                    String strRole = (String) currentMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]");
                    String strPosition = (String) currentMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_POSITION + "]");

                    // TIGTK-5890 : PTE : 4/7/2017
                    if (TigerConstants.ROLE_PSS_CHANGE_COORDINATOR.equalsIgnoreCase(strRole) && "Lead".equals(strPosition)) {
                        if (UIUtil.isNullOrEmpty(strPerson)) {
                            strPerson = (String) currentMap.get(DomainObject.SELECT_NAME);
                        } else {
                            strPerson = strPerson + "," + currentMap.get(DomainObject.SELECT_NAME);
                        }

                    }
                }

                // PCM : TIGTK-8136 : 25/05/2017 : AB : START
                if (UIUtil.isNotNullAndNotEmpty(strPerson)) {
                    vProjectCountList.add(strPerson);
                }
                // PCM : TIGTK-8136 : 25/05/2017 : AB : END

            }
        } catch (Exception e) {
            logger.error("in method getProjectLeadChangeManager: ", e);
            throw e;
        }
        return vProjectCountList;
    }

    public Vector getProjectPrograms(Context context, String[] args) throws Exception {
        Vector vProjectCountList = new Vector();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList objIdList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < objIdList.size(); i++) {
            StringBuffer strPrograms = new StringBuffer();
            try {
                StringList busSelects = new StringList();
                StringList relSelects = new StringList();
                busSelects.add(DomainObject.SELECT_ID);
                busSelects.add(DomainObject.SELECT_TYPE);
                busSelects.add(DomainObject.SELECT_NAME);
                relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);
                busSelects.add("attribute[PSS_ProgramProject]");

                DomainObject domProject = new DomainObject(((Map) objIdList.get(i)).get(SELECT_ID) + "");

                MapList mapProgram = domProject.getRelatedObjects(context, "PSS_SubProgramProject", "PSS_ProgramProject", busSelects, relSelects, true, true, (short) 1, null, null, 0);

                for (int intIndex = 0; intIndex < mapProgram.size(); intIndex++) {
                    Map currentMap = (Map) mapProgram.get(intIndex);

                    if (currentMap.get("attribute[PSS_ProgramProject]").equals("Program")) {
                        String strObjId = (String) currentMap.get(DomainObject.SELECT_ID);
                        String strObjName = (String) currentMap.get(DomainObject.SELECT_NAME);
                        strPrograms.append(" <a href=\"../common/emxTree.jsp?objectId=" + strObjId + "\">" + strObjName + "</a><br/>");
                    }
                }
            } catch (Exception ex) {
                // TIGTK-5405 - 18-04-2017 - PTE - START
                logger.error("Error in getProjectPrograms: ", ex);
                throw ex;
                // TIGTK-5405 - 18-04-2017 - PTE - End

            }
            vProjectCountList.add(strPrograms.toString());
        }
        return vProjectCountList;
    }

    // Addition for Tiger Faurecia - PCM stream by SGS ends
    // Start-- Addition for Tiger Faurecia - PCM stream by SGS Swapnil Patil

    DomainObject dom = new DomainObject();

    public static final String TYPE_TECHNICALSPECIFICATION = PropertyUtil.getSchemaProperty("type_TechnicalSpecification");

    public static final String TYPE_VIEWABLE = PropertyUtil.getSchemaProperty("type_Viewable");

    public static final String TYPE_DOCUMENTS = PropertyUtil.getSchemaProperty("type_DOCUMENTS");

    public static final String TYPE_PLMENTITY = PropertyUtil.getSchemaProperty("type_PLMEntity");

    public Vector getRelatedProjectCounts(Context context, String[] args) throws Exception {
        Vector vProjectCountList = new Vector();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            MapList objIdList = (MapList) paramMap.get("objectList");

            // PCM TIGTK-4473 | 07/03/17 : Pooja Mantri : Start
            String strMode = DomainConstants.EMPTY_STRING;
            HashMap paramList = (HashMap) paramMap.get("paramList");
            String strParentOID = (String) paramList.get("parentOID");
            DomainObject domParentObject = DomainObject.newInstance(context, strParentOID);
            String strParentObjectType = domParentObject.getInfo(context, DomainConstants.SELECT_TYPE);
            String strTableName = (String) paramList.get("table");
            if (UIUtil.isNotNullAndNotEmpty(strTableName) && "PSS_ECMChangeAssessmentResult".equals(strTableName)) {
                strMode = "ChangeAssessment";
            }

            if (UIUtil.isNotNullAndNotEmpty(strParentOID)) {

                if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equals(strParentObjectType)) {
                    strMode = "CR";
                } else if (TigerConstants.TYPE_PSS_CHANGEORDER.equals(strParentObjectType) || TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER.equals(strParentObjectType)) {
                    strMode = "CO";
                }
            }
            // PCM TIGTK-4473 | 07/03/17 : Pooja Mantri : End

            // StringBuffer sb = new StringBuffer();
            for (int i = 0; i < objIdList.size(); i++) {
                StringBuffer strFormAction = new StringBuffer();
                Map objectIds = (Map) objIdList.get(i);
                String strObjId = (String) objectIds.get("id");
                DomainObject domObject = DomainObject.newInstance(context, strObjId);
                // Modified by PCM : TIGTK-3951 : 25/01/2017 : AB :START
                StringBuffer sbListObjectId = new StringBuffer();
                MapList mapProjectList = null;
                // PCM TIGTK-2998 & TIGTK-3789 | RFC-XXX : 06/09/16 : AB : START
                if (domObject.isKindOf(context, DomainConstants.TYPE_PART)) {
                    mapProjectList = getProjectListFromEBOM(context, strObjId);
                } else if (domObject.isKindOf(context, TYPE_DOCUMENTS) || domObject.isKindOf(context, TYPE_TECHNICALSPECIFICATION) || domObject.isKindOf(context, TYPE_VIEWABLE)) {
                    mapProjectList = getProjectListFromEBOM(context, strObjId);
                } else if (domObject.isKindOf(context, TigerConstants.TYPE_DELFMIFUNCTIONREFERENCE) || domObject.isKindOf(context, TYPE_PLMENTITY)) {
                    mapProjectList = getProjectListFromMBOM(context, strObjId);
                } else {
                    mapProjectList = new MapList();
                }
                // PCM TIGTK-2998 & TIGTK-3789 | RFC-XXX : 06/09/16 : AB : END
                int strProjectCountint = 0;
                for (int cntProg = 0; cntProg < mapProjectList.size(); cntProg++) {
                    String str = (String) ((Map) mapProjectList.get(cntProg)).get("id");
                    if (!sbListObjectId.toString().contains(str)) {
                        sbListObjectId.append(str);
                        sbListObjectId.append(",");
                        strProjectCountint++;
                    }
                    // Modified by PCM : TIGTK-3951 : 25/01/2017 : AB : END
                }

                // Ticket:TIGTK-2750: SteepGraph(Rutuja Ekatpure):Start
                if (strProjectCountint == 0) {
                    // Findbug Issue correction start
                    // Date: 21/03/2017
                    // By: Asha G.
                    strFormAction.append(strProjectCountint);

                } else {
                    // PCM TIGTK-4473 | 07/03/17 : Pooja Mantri : Start
                    strFormAction.append("<input id='projectInput");
                    strFormAction.append(i);
                    // TIGTK-14849:04-07-2018:STARTS
                    strFormAction.append("' name='projectInput");
                    strFormAction.append(i);
                    // TIGTK-14849:04-07-2018:ENDS
                    strFormAction.append("' type='hidden' value='");
                    // TIGTK-6878:START
                    strFormAction.append(strObjId);
                    // TIGTK-6878:END
                    strFormAction.append("'/><a onclick=\"showModalDialog('../enterprisechangemgt/PSS_ECMAffectedProjectSummary.jsp?selectedRowNo=");
                    strFormAction.append(i);
                    strFormAction.append("&amp;mode=");
                    strFormAction.append(strMode);
                    strFormAction.append("&amp;program=PSS_enoECMChangeUtil:getProjectList', 800, 600, true)\">");
                    strFormAction.append(strProjectCountint);
                    strFormAction.append("</a>");
                    // Findbug Issue correction End
                    // PCM TIGTK-4473 | 07/03/17 : Pooja Mantri : End
                }
                // Ticket:TIGTK-2750: SteepGraph(Rutuja Ekatpure):End

                vProjectCountList.add(strFormAction.toString());
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getRelatedProjectCounts: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return vProjectCountList;
    }

    public MapList getProjectList(Context context, String[] args) throws Exception {
        MapList mapProjectList = new MapList();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String requestString = (String) programMap.get("projectList");
            // RE:TIGTK-6897:19/9/2017:Start
            String requestRelString = (String) programMap.get("projectRelList");

            if (requestString.contains(",") && UIUtil.isNotNullAndNotEmpty(requestRelString)) {
                String[] arr = requestString.split(",");
                String[] arrRel = requestRelString.split(",");

                for (int i = 0; i < arr.length; i++) {
                    Map<String, String> mapObject = new HashMap<String, String>();
                    mapObject.put("id", arr[i]);
                    mapObject.put("id[connection]", arrRel[i]);
                    mapProjectList.add(mapObject);
                }
            } else if (requestString.contains(",")) {
                String[] arr = requestString.split(",");
                for (int i = 0; i < arr.length; i++) {
                    Map<String, String> mapObject = new HashMap<String, String>();
                    mapObject.put("id", arr[i]);
                    mapProjectList.add(mapObject);
                }
            }
            // RE:TIGTK-6897:19/9/2017:End
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getProjectList: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return mapProjectList;
    }

    public MapList getProjectListFromPartSpecification(Context context, String args) throws Exception {
        MapList allProjects = new MapList();
        try {
            String strObjectId = args;// (String)args[0];
            MapList tempProjectsList = null;
            DomainObject domCAD = DomainObject.newInstance(context, strObjectId);
            StringList busSelects = new StringList();
            StringList relSelects = new StringList();
            busSelects.add(DomainObject.SELECT_ID);
            busSelects.add(DomainObject.SELECT_TYPE);
            busSelects.add(DomainObject.SELECT_NAME);
            busSelects.add(DomainObject.SELECT_REVISION);

            // PCM : TIGTK-6794 : 20/04/2017 : AB : START
            try {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

                MapList mapCADPartList = domCAD.getRelatedObjects(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship // pattern
                        DomainConstants.TYPE_PART, // object pattern
                        busSelects, // object selects
                        relSelects, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 0, // pageSize
                        null, // Product Display
                        null, null, null, null);
                // ALM defect 1628 :CAD object related project count is wrong :Rutuja Ekatpure:22/11/2016:start
                for (int intIndex = 0; intIndex < mapCADPartList.size(); intIndex++) {
                    Map currentMap = (Map) mapCADPartList.get(intIndex);
                    tempProjectsList = getProjectListFromEBOM(context, (String) currentMap.get(DomainObject.SELECT_ID));
                    if (!tempProjectsList.isEmpty()) {
                        allProjects.addAll(tempProjectsList);
                    }
                }
            } finally {
                ContextUtil.popContext(context);
            }
            // PCM : TIGTK-6794 : 20/04/2017 : AB : END

            // ALM defect 1628 :CAD object related project count is wrong :Rutuja Ekatpure:22/11/2016:End
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getProjectListFromPartSpecification: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return allProjects;
    }

    public MapList getProjectListFromEBOM(Context context, String ecPartID) throws Exception {
        MapList allProjects = new MapList();
        // TIGTK-6878 : Phase-2.0 : START
        try {
            StringList busSelects = new StringList();
            StringList relSelects = new StringList();
            busSelects.add(DomainObject.SELECT_ID);
            busSelects.add(DomainObject.SELECT_TYPE);
            busSelects.add(DomainObject.SELECT_NAME);
            busSelects.add(DomainObject.SELECT_REVISION);

            // PCM : TIGTK-6794 : 20/04/2017 : AB : START
            try {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                // PCM : TIGTK-8528 : 16/06/2017 : AB : START
                DomainObject ecPart2 = DomainObject.newInstance(context, ecPartID);
                // PCM : TIGTK-8528 : 16/06/2017 : AB : END
                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT);
                // pattern
                Pattern busPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

                MapList productProjectList = ecPart2.getRelatedObjects(context, relPattern.getPattern(), // relationship //
                        // pattern
                        busPattern.getPattern(), // object pattern
                        busSelects, // object selects
                        relSelects, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 0, // pageSize
                        null, // Product Display
                        null, null, null, null);

                for (int intIndex = 0; intIndex < productProjectList.size(); intIndex++) {
                    Map currentMap = (Map) productProjectList.get(intIndex);

                    if (currentMap.get(DomainObject.SELECT_TYPE).equals(TigerConstants.TYPE_PSS_PROGRAMPROJECT)) {
                        allProjects.add(currentMap);
                    }
                }
            } finally {
                ContextUtil.popContext(context);
            }
            // PCM : TIGTK-6794 : 20/04/2017 : AB : END
            // TIGTK-6878 : Phase-2.0 : START
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getProjectListFromEBOM: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return allProjects;
    }

    public MapList getAllRootFromECPart(Context context, String ecPartId) throws Exception {

        MapList allRootParts = new MapList();
        try {
            DomainObject ecPart = DomainObject.newInstance(context, ecPartId);
            StringList busSelects = new StringList();
            StringList relSelects = new StringList();
            busSelects.add(DomainObject.SELECT_ID);
            busSelects.add(DomainObject.SELECT_TYPE);
            busSelects.add(DomainObject.SELECT_NAME);
            busSelects.add(DomainObject.SELECT_REVISION);
            busSelects.add("to[" + DomainConstants.RELATIONSHIP_EBOM + "]");

            Map mapDomPart = ecPart.getInfo(context, busSelects);

            String toRootPartEBOM = (String) mapDomPart.get("to[" + DomainConstants.RELATIONSHIP_EBOM + "]");
            if (toRootPartEBOM.equals("False")) {
                allRootParts.add(mapDomPart);
                return allRootParts;
            }

            // PCM : TIGTK-6794 : 20/04/2017 : AB : START
            try {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                MapList resultList = ecPart.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, // relationship // pattern
                        DomainConstants.TYPE_PART, // object pattern
                        busSelects, // object selects
                        relSelects, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 0, // pageSize
                        null, // Product Display
                        null, null, null, null);

                for (int intIndex = 0; intIndex < resultList.size(); intIndex++) {
                    Map attMap = (Map) resultList.get(intIndex);
                    String toEBOM = (String) attMap.get("to[" + DomainConstants.RELATIONSHIP_EBOM + "]");
                    if (toEBOM.equals("False")) {
                        allRootParts.add(attMap);
                    }
                }
            } finally {
                ContextUtil.popContext(context);
            }
            // PCM : TIGTK-6794 : 20/04/2017 : AB : END

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getAllRootFromECPart: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return allRootParts;
    }

    public static String convertingListToString(Context paramContext, StringList paramStringList) throws Exception {
        StringBuffer localStringBuffer = new StringBuffer();
        try {
            String str = "";
            for (int i = 0; i < paramStringList.size(); i++) {
                str = (String) paramStringList.get(i);
                if (localStringBuffer.length() != 0) {
                    localStringBuffer.append(",");
                    localStringBuffer.append(str);
                } else {
                    localStringBuffer.append(str);
                }
            }
        } catch (Exception localException) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in convertingListToString: ", localException);

            // TIGTK-5405 - 18-04-2017 - PTE - End
            throw localException;
        }

        return localStringBuffer.toString();
    }

    public MapList getChangeAssessment(Context paramContext, StringList paramStringList) throws Exception {

        MapList localMapList1 = new MapList();
        try {
            String str1 = "";

            String str2 = "";
            String str3 = "";
            String str4 = "";
            String str5 = convertingListToString(paramContext, paramStringList);
            StringList localStringList1 = new StringList("id");

            StringList localStringList3 = new StringList("type");
            localStringList3.add("policy");
            dom.setId(str5);
            Map localMap2 = dom.getInfo(paramContext, localStringList3);
            StringList localStringList4 = ECMAdmin.getRegisteredTypesActual(paramContext);
            String str6 = convertingListToString(paramContext, localStringList4);
            StringList localStringList5 = ECMAdmin.getChangeAssessmentRels(paramContext, (String) localMap2.get("type"), (String) localMap2.get("policy"));

            if (localStringList5 != null) {
                StringList localStringList2 = new StringList("id[connection]");
                Iterator localIterator = localStringList5.iterator();
                while (localIterator.hasNext()) {
                    str1 = (String) localIterator.next();
                    String[] arrayOfString = str1.split(",");
                    str2 = arrayOfString[0];
                    str3 = arrayOfString[1];
                    str4 = EnoviaResourceBundle.getProperty(paramContext, arrayOfString[3], paramContext.getLocale(), arrayOfString[2]);
                    boolean bool1 = true;
                    boolean bool2 = false;
                    if ("to".equalsIgnoreCase(str3)) {
                        bool1 = false;
                        bool2 = true;
                    }

                    ArrayList localArrayList = new ArrayList();
                    String str7 = "";
                    Short sVal = 0;
                    // Findbug Issue correction start
                    // Date: 21/03/2017
                    // By: Asha G.
                    MapList localMapList2 = dom.getRelatedObjects(paramContext, str2, str6, localStringList1, localStringList2, bool2, bool1, sVal, null, null, 0);
                    // Findbug Issue correction End
                    for (int i = 0; i < localMapList2.size(); i++) {
                        Map localMap1 = (Map) localMapList2.get(i);
                        str7 = (String) localMap1.get("id");
                        if (!localArrayList.contains(str7)) {
                            localArrayList.add(str7);
                            localMap1.put("strLabel", str4);
                            localMapList1.add(localMapList2.get(i));
                        }
                    }
                }
            }
        } catch (Exception localException) {
            logger.error("Error in getChangeAssessment: ", localException);
            throw localException;
        }
        return localMapList1;
    }

    /**
     * Getting the Child/ Parent from the selected item
     * @param context
     * @param args
     * @return MapList of Child/ Parent
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getChangeAssessmentItems(Context context, String[] args) throws Exception {
        MapList mlOutput = null;
        try {
            HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
            String[] arrTableRowIds = new String[1];
            String strTableRowID = (String) hmParamMap.get("emxTableRowId");
            arrTableRowIds[0] = strTableRowID;
            StringList busSelects = new StringList();
            StringList relSelects = new StringList();
            relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            busSelects.add(DomainObject.SELECT_ID);

            StringList slObjectIds = getAffectedItemsIds(context, arrTableRowIds);
            mlOutput = getChangeAssessment(context, slObjectIds);
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getChangeAssessmentItems: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return mlOutput;
    }

    public StringList getAffectedItemsIds(Context paramContext, String[] paramArrayOfString) throws Exception {
        // Findbug Issue correction start
        // Date: 21/03/2017
        // By: Asha G.
        StringList localStringList1 = null;
        // Findbug Issue correction End
        try {
            Map localMap = getObjectIdsRelIdsMapFromTableRowID(paramArrayOfString);
            StringList localStringList2 = (StringList) localMap.get("RelId");
            String[] arrayOfString = (String[]) (String[]) localStringList2.toArray(new String[localStringList2.size()]);
            MapList localMapList = DomainRelationship.getInfo(paramContext, arrayOfString, new StringList("to.id"));
            localStringList1 = getStringListFromMapList(localMapList, "to.id");
        } catch (Exception localException) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getAffectedItemsIds: ", localException);
            throw localException;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return localStringList1;
    }

    public static Map getObjectIdsRelIdsMapFromTableRowID(String[] paramArrayOfString) {
        HashMap localHashMap = new HashMap();
        try {
            int i = 0;

            StringList localStringList1 = new StringList();
            StringList localStringList2 = new StringList();

            if (paramArrayOfString != null) {
                // Findbug Issue correction start
                // Date: 21/03/2017
                // By: Asha G.
                StringList localStringList3 = null;
                // Findbug Issue correction End
                i = paramArrayOfString.length;
                for (int j = 0; j < i; j++) {
                    localStringList3 = FrameworkUtil.split(paramArrayOfString[j], "|");

                    if (localStringList3.size() == 3) {
                        localStringList1.addElement((String) localStringList3.get(0));
                    } else {
                        localStringList2.addElement((String) localStringList3.get(0));
                        localStringList1.addElement((String) localStringList3.get(1));
                    }
                }
            }
            localHashMap.put("ObjId", localStringList1);
            localHashMap.put("RelId", localStringList2);
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getObjectIdsRelIdsMapFromTableRowID: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return localHashMap;
    }

    public StringList getStringListFromMapList(MapList paramMapList, String paramString) {
        int i = paramMapList.size();
        StringList localStringList = new StringList(i);

        for (int j = 0; j < i; j++) {
            Map localMap = (Map) paramMapList.get(j);
            localStringList.addAll(getListValue(localMap, paramString));
        }

        return localStringList;
    }

    public static StringList getListValue(Map paramMap, String paramString) {
        Object localObject = paramMap.get(paramString);
        return (localObject instanceof String) ? new StringList((String) localObject) : localObject == null ? new StringList(0) : (StringList) localObject;
    }

    public static MapList getProjectListFromMBOM(Context context, String mfgId) throws Exception {

        MapList mlist = new MapList();
        PLMCoreModelerSession plmSession = null;
        boolean transactionActive = false;
        try {

            Pattern relpattern = new Pattern(TigerConstants.RELATIONSHIP_GBOM);
            relpattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT);
            relpattern.addPattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
            Pattern typepattern = new Pattern(DomainConstants.TYPE_PART);
            typepattern.addPattern(TigerConstants.TYPE_PLMCORE_REFERENCE);
            typepattern.addPattern(TigerConstants.TYPE_VPMREFERENCE);
            typepattern.addPattern(TigerConstants.TYPE_PRODUCTS);
            typepattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            Pattern finalPattern = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);
            StringList slselectObjStmts = new StringList(DomainConstants.SELECT_ID);
            slselectObjStmts.addElement(DomainConstants.SELECT_NAME);
            slselectObjStmts.addElement(TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT);
            List<String> objectIdList = new ArrayList();
            objectIdList.add(mfgId);

            ContextUtil.startTransaction(context, false);
            transactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            List<String> strPhysicalStructureId = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, objectIdList);
            if (strPhysicalStructureId != null && strPhysicalStructureId.size() > 0) {
                MapList mapLIst = pss.mbom.MBOMUtil_mxJPO.getPartFromVPMReference(context, strPhysicalStructureId.get(0));
                if (mapLIst != null && mapLIst.size() > 0) {
                    Map objMap = (Map) mapLIst.get(0);
                    DomainObject domPart = DomainObject.newInstance(context, (String) objMap.get(DomainConstants.SELECT_ID));

                    // PCM : TIGTK-6964 : 26/04/2017 : AB : START
                    try {
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        mlist = domPart.getRelatedObjects(context, relpattern.getPattern(), // relationship pattern
                                typepattern.getPattern(), // object pattern
                                slselectObjStmts, // object selects
                                null, // relationship selects
                                true, // to direction
                                false, // from direction
                                (short) 0, // recursion level
                                null, // object where clause
                                null, (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 1000, // pageSize
                                finalPattern, // Postpattern
                                null, null, null);
                    } finally {
                        ContextUtil.popContext(context);
                    }
                    // PCM : TIGTK-6964 : 26/04/2017 : AB : END
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getProjectListFromMBOM: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
            // Findbug Issue correction start
            // Date: 22/03/2017
            // By: Asha G.
        } finally {
            if (plmSession != null) {
                plmSession.closeSession(true);
                if (transactionActive) {
                    ContextUtil.abortTransaction(context);
                }
            }

        }
        // Findbug Issue correction End
        return mlist;
    }

    // End-- Addition for Tiger Faurecia - PCM stream by SGS Swapnil Patil

    /**
     * Description : This method is used for Add Affected Item to Change request when Change Request created from EBOM This method Modified by PCM : TIGTK-3973 : 20/01/2017 : AB
     * @author abhalani
     * @args
     * @Date Jul 20, 2016
     */
    public void addPartToChangeRequestfromEBOM(Context context, String[] args) throws Exception {

        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            StringList strCRIdS = (StringList) programMap.get("strCRID");
            String strCRId = (String) strCRIdS.get(0);
            DomainObject domCR = new DomainObject(strCRId);
            StringList listAffectedItem = (StringList) programMap.get("affeItemList");

            // PCM : TIGTK-6419 : 13/04/2017 : AB : START
            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_CURRENT);
            objectSelects.add(DomainConstants.SELECT_TYPE);

            StringList slNotAllowedManufaturingType = new StringList();
            slNotAllowedManufaturingType.add(TigerConstants.TYPE_PSS_LINEDATA);
            slNotAllowedManufaturingType.add(TigerConstants.TYPE_PSS_OPERATION);

            // PCM : TIGTK-6419 : 13/04/2017 : AB : END
            if (UIUtil.isNotNullAndNotEmpty(strCRId) && listAffectedItem.size() != 0) {

                // Get the related Symmetrical part of related Current affected Item
                StringList slSymmetricalParts = this.getSymmetricalPartForCR(context, listAffectedItem);

                StringList slselectedItemsList = new StringList();
                slselectedItemsList.addAll(listAffectedItem);
                slselectedItemsList.addAll(slSymmetricalParts);

                // Get the CAD Part of related Current affected Item
                StringList slCADList = this.getCADObjectsForCR(context, slselectedItemsList);
                slselectedItemsList.addAll(slCADList);

                // Get unique list of Affected Items
                PSS_enoECMChangeOrder_mxJPO ChangeOrderBase = new PSS_enoECMChangeOrder_mxJPO(context, args);
                StringList slAffectedItems = ChangeOrderBase.getUniqueIdList(slselectedItemsList);
                int nCount = slAffectedItems.size();

                if (slAffectedItems.size() != 0) {
                    for (int cnt = 0; cnt < nCount; cnt++) {
                        String strRelatedPart = (String) slAffectedItems.get(cnt);
                        DomainObject domRelatedPart = new DomainObject(strRelatedPart);
                        // PCM : TIGTK-6419 : 13/04/2017 : AB : START
                        Map mapItemInfo = domRelatedPart.getInfo(context, objectSelects);
                        String strItemState = (String) mapItemInfo.get(DomainConstants.SELECT_CURRENT);
                        String strItemType = (String) mapItemInfo.get(DomainConstants.SELECT_TYPE);

                        if (!slNotAllowedManufaturingType.contains(strItemType)) {
                            // PCM : TIGTK-6419 : 13/04/2017 : AB : END
                            // PCM : TIGTK-9132 : 26/07/2017 : AB : START
                            PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, null);
                            boolean bolAllowPushPop = changeRequest.isAllowConnctionOfStandardPartAndChangeRequest(context, domRelatedPart, domCR);
                            // Connect Part & Connected Symmetrical Part &
                            // Connected CAD Part with Change Request

                            boolean isPushedContext = false;
                            if (bolAllowPushPop) {
                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                isPushedContext = true;
                            }

                            try {
                                // Connect Part & Connected Symmetrical Part &
                                // Connected CAD Part with Change Request
                                DomainRelationship domRel = DomainRelationship.connect(context, domCR, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, domRelatedPart);
                                // condition changed :Rutuja Ekatpure:10/11/2016:start
                                if (strItemState.equals(TigerConstants.STATE_RELEASED_CAD_OBJECT) || strItemState.equals(TigerConstants.STATE_PART_RELEASE)
                                        || strItemState.equals(ChangeConstants.STATE_DEVELOPMENT_PART_COMPLETE)) {
                                    domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_REVISE);
                                } else {
                                    domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_RELEASE);
                                }
                            } finally {
                                if (isPushedContext) {
                                    ContextUtil.popContext(context);
                                }
                            }
                            // PCM : TIGTK-9132 : 26/07/2017 : AB : END
                        }
                        // condition changed :Rutuja Ekatpure:10/11/2016:end
                    }
                    // PCM TIGTK-3609 : 11/11/2016 : KWagh : End
                }
            }
        } catch (Exception ex) {
            // PCM : TIGTK-9132 : 24/07/2017 : AB : START
            MqlUtil.mqlCommand(context, "Notice $1", ex.getMessage());
            // PCM : TIGTK-9132 : 24/07/2017 : AB : END
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in addPartToChangeRequestfromEBOM: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
    }

    /**
     * Description : Include Object ID of CO which is Connected with same Program Project of CR
     * @author abhalani
     * @args
     * @Date Jul 22, 2016
     */
    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList includeConnectedCO(Context context, String[] args) throws Exception {
        // TODO Auto-generated method stub
        StringList listCOIDs = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strParentOID = (String) programMap.get("objectId");
            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPCMData");
            String typePattern = PropertyUtil.getSchemaProperty(context, "type_PSS_ProgramProject");
            String strProgramProjectId = "";

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);

            // Get Connected Program Project of Change Request
            DomainObject domParentOID = new DomainObject(strParentOID);
            MapList mlCRConnectedProgramProject = domParentOID.getRelatedObjects(context, relpattern, typePattern, slObjectSle, null, true, false, (short) 1, null, null, 0);

            if (mlCRConnectedProgramProject.size() != 0) {
                for (int j = 0; j < mlCRConnectedProgramProject.size(); j++) {
                    Map mapProgramProject = (Map) mlCRConnectedProgramProject.get(j);
                    strProgramProjectId = (String) mapProgramProject.get(DomainConstants.SELECT_ID);
                }
            }

            // Get Change Order Id which have same program project as Change Request
            MapList listCOId = (MapList) domParentOID.findObjects(context, TigerConstants.TYPE_PSS_CHANGEORDER, TigerConstants.VAULT_ESERVICEPRODUCTION,
                    "(to[PSS_ConnectedPCMData].from.id)==" + strProgramProjectId + "", slObjectSle);

            if (listCOId.size() != 0) {
                for (int j = 0; j < listCOId.size(); j++) {
                    Map mapCOID = (Map) listCOId.get(j);
                    String strCOId = (String) mapCOID.get(DomainConstants.SELECT_ID);
                    listCOIDs.add(strCOId);
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in includeConnectedCO: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }

        return listCOIDs;
    }

    /**
     * Description : Include Object ID of CR which is Connected with same Program Project of CO
     * @author abhalani
     * @args
     * @Date Jul 22, 2016
     */
    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public StringList includeConnectedObjectsOfCR(Context context, String[] args) throws Exception {
        // TODO Auto-generated method stub
        StringList listCRIDs = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strParentOID = (String) programMap.get("objectId");
            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPCMData");
            String typePattern = PropertyUtil.getSchemaProperty(context, "type_PSS_ProgramProject");
            String strProgramProjectId = "";

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);

            // Get Connected Program Project of Change Order
            DomainObject domParentOID = new DomainObject(strParentOID);
            MapList mlCOConnectedProgramProject = domParentOID.getRelatedObjects(context, relpattern, typePattern, slObjectSle, null, true, false, (short) 1, null, null, 0);

            if (mlCOConnectedProgramProject.size() != 0) {
                for (int j = 0; j < mlCOConnectedProgramProject.size(); j++) {
                    Map mapProgramProject = (Map) mlCOConnectedProgramProject.get(j);
                    strProgramProjectId = (String) mapProgramProject.get(DomainConstants.SELECT_ID);
                }
            }

            // Get Change Request Id which have same program project as Change Order
            MapList listCRId = (MapList) domParentOID.findObjects(context, TigerConstants.TYPE_PSS_CHANGEREQUEST, TigerConstants.VAULT_ESERVICEPRODUCTION,
                    "(to[PSS_ConnectedPCMData].from.id)==" + strProgramProjectId + "", slObjectSle);

            if (listCRId.size() != 0) {
                for (int j = 0; j < listCRId.size(); j++) {
                    // TIGTK-6245:Modified on 11/04/2017 by SIE :Start
                    Map mapCRID = (Map) listCRId.get(j);
                    String strCRId = (String) mapCRID.get(DomainConstants.SELECT_ID);
                    DomainObject domCRID = new DomainObject(strCRId);
                    String strParallelTrackVal = domCRID.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK);
                    if (strParallelTrackVal.equals("No"))
                        listCRIDs.add(strCRId);
                    // TIGTK-6245:Modified on 11/04/2017 by SIE :END
                }
            }
            if (listCRIDs.isEmpty())
                listCRIDs.add(DomainConstants.EMPTY_STRING);

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in includeConnectedObjectsOfCR: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return listCRIDs;
    }

    // PCM TIGTK-3852: 30/12/2016 : KWagh : START
    /**
     * @description: On removal of CR from CO, this Method is used to display the list of Affected Items not related to any CR but related to the CO.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     * @TIGTK-3852
     */
    public MapList getAffectedItemNotHavingCRwithSameCO(Context context, String[] args) throws Exception {
        MapList mlAffectedItems = new MapList();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        try {
            String strCAId;
            String strCOId = (String) programMap.get("parentID");
            DomainObject domCO = new DomainObject(strCOId);

            String strSelectCR = "to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.id";

            StringList objectSelects = new StringList(1);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(strSelectCR);

            StringList relSelects = new StringList(1);
            relSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            MapList changeActionList = domCO.getRelatedObjects(context, // context
                    ChangeConstants.RELATIONSHIP_CHANGE_ACTION, // relationship pattern
                    ChangeConstants.TYPE_CHANGE_ACTION, // object pattern
                    objectSelects, // object selects
                    relSelects, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, // relationship where clause
                    (short) 0);

            for (int i = 0; i < changeActionList.size(); i++) {
                Map mCAObj = (Map) changeActionList.get(i);
                strCAId = (String) mCAObj.get(DomainConstants.SELECT_ID);
                DomainObject domCA = new DomainObject(strCAId);

                MapList mlCOAffectedItems = domCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, // relationship pattern
                        DomainConstants.QUERY_WILDCARD, // object pattern
                        objectSelects, // object selects
                        relSelects, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, 0);

                int ncount = mlCOAffectedItems.size();
                for (int cnt = 0; cnt < ncount; cnt++) {
                    Map mAI = (Map) mlCOAffectedItems.get(cnt);
                    StringList slConnectedCR = getStringListFromMap(context, mAI, strSelectCR);
                    if ((slConnectedCR.isEmpty()) || (slConnectedCR.contains(null))) {
                        mlAffectedItems.add(mAI);
                    }
                }

            }

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getAffectedItemNotHavingCRwithSameCO: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }

        return mlAffectedItems;
    }

    public void demoteChangeOrderOnDemoteChangeAction(Context context, String[] args) throws Exception {
        String caId = args[0];
        DomainObject ca = DomainObject.newInstance(context, caId);
        String rel_affectedItem = PropertyUtil.getSchemaProperty("relationship_ChangeAction");
        DomainObject co = DomainObject.newInstance(context);
        String currentState = "";
        String STATE_IN_REVIEW = "In Approval";
        boolean isPushContext = false;
        try {
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isPushContext = true;
            StringList coList = ca.getInfoList(context, "to[" + rel_affectedItem + "].from.id");
            for (Object object : coList) {
                co.setId((String) object);
                currentState = co.getInfo(context, DomainObject.SELECT_CURRENT);
                if (currentState.equals(STATE_IN_REVIEW)) {
                    co.demote(context);
                } else {
                    continue;
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in demoteChangeOrderOnDemoteChangeAction: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        } finally {
            if (isPushContext) {
                ContextUtil.popContext(context);
            }
        }
    }

    /**
     * This Method is used to Include OID Program for TechAssignee form PSS_type_ChangeAction
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public StringList getMemberToConnectedProgramProject(Context context, String[] args) throws Exception {
        StringList slPersonList = new StringList();

        try {
            // Modified by - kwagh TIGTK-2838 -Start
            // Modified by KWagh - For MCA Assignee List - Start
            HashSet<String> plantName = new HashSet<>();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            StringList slMemberList = new StringList();
            String objectId = (String) programMap.get("objectId");

            DomainObject domObj = new DomainObject(objectId);

            String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
            // PCM TIGTK-10768 : 16/11/17 : TS : START
            String strTechnicalAssigneeId = domObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.id");
            // PCM TIGTK-10768 : 16/11/17 : TS : END

            // TIGTK-13608 : Get program-project from CO when user use Type Ahead functionality to change Assignee : START
            if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) {
                slMemberList = domObj.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.id");

            }
            // TIGTK-13608 : END
            else if (strType.equalsIgnoreCase(ChangeConstants.TYPE_CHANGE_ACTION)) {
                slMemberList = domObj.getInfoList(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from["
                        + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.id");
            } else if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                String strMCOPlantName = DomainConstants.EMPTY_STRING;
                StringList slPlantConnectedToProject = new StringList();
                String strProgProjId = DomainConstants.EMPTY_STRING;

                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {
                    strMCOPlantName = domObj.getInfo(context,
                            "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");

                    slPlantConnectedToProject = domObj.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to["
                            + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ + "].id");

                    strProgProjId = domObj.getInfo(context,
                            "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                } else {

                    strMCOPlantName = domObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");

                    slPlantConnectedToProject = domObj.getInfoList(context,
                            "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ + "].id");

                    strProgProjId = domObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                }

                int nCount = slPlantConnectedToProject.size();
                for (int cnt = 0; cnt < nCount; cnt++) {
                    String strConnectionID = (String) slPlantConnectedToProject.get(cnt);
                    DomainRelationship domRel = new DomainRelationship(strConnectionID);
                    String STRPSS_PLANTNAME = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PLANTNAME);

                    plantName.add(STRPSS_PLANTNAME);
                }
                if (plantName.contains(strMCOPlantName)) {
                    // PCM TIGTK-3143 : 20/09/16 : AB : START
                    DomainObject domProgramProject = new DomainObject(strProgProjId);

                    StringList objectSelects = new StringList(1);
                    objectSelects.addElement(DomainConstants.SELECT_ID);
                    objectSelects.addElement(DomainConstants.SELECT_TYPE);
                    objectSelects.add(DomainConstants.SELECT_CURRENT);

                    StringList relSelects = new StringList(1);
                    relSelects.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                    String strWhere = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "] == \"" + strMCOPlantName + "\")";
                    MapList mlMembersList = domProgramProject.getRelatedObjects(context, // context
                            TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ, // relationship
                            // pattern
                            DomainConstants.TYPE_PERSON, // object pattern
                            objectSelects, // object selects
                            relSelects, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 0, // recursion level
                            null, // object where clause
                            strWhere, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 0, // pageSize
                            null, null, null, null);

                    if (!mlMembersList.isEmpty()) {
                        for (int i = 0; i < mlMembersList.size(); i++) {
                            Map mTemp = (Map) mlMembersList.get(i);
                            String strMember = (String) mTemp.get("id");
                            slMemberList.add(strMember);
                        }
                    }
                    // PCM TIGTK-3143 : 20/09/16 : AB : END
                }
                // Modified by KWagh - For MCA Assignee List - END
            }
            // Modified by - kwagh TIGTK-2838 -End
            // Modified By KWagh - TIGTK-2901 - Start
            int nCount = slMemberList.size();
            for (int i = 0; i < nCount; i++) {
                String strPersonID = (String) slMemberList.get(i);

                DomainObject domPersonObject = new DomainObject(strPersonID);
                String strPersonCurrent = domPersonObject.getInfo(context, DomainConstants.SELECT_CURRENT);
                if (strPersonCurrent.equalsIgnoreCase("Active")) {
                    slPersonList.add(strPersonID);
                }
            }

            // PCM TIGTK-10768 : 16/11/17 : TS : START
            if (slPersonList.contains(strTechnicalAssigneeId)) {
                slPersonList.remove(strTechnicalAssigneeId);
            }
            // PCM TIGTK-10768 : 16/11/17 : TS : END
            if (slPersonList.isEmpty())
                slPersonList.add(DomainConstants.EMPTY_STRING);
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getMemberToConnectedProgramProject: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return slPersonList;
        // Modified By KWagh - TIGTK-2901 - End
    }

    // Modified by - kwagh TIGTK-2770 -Start

    /**
     * This Method is used to get List of Assignees for Change Action
     * @author SteepGraph
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public StringList getAssigneeForCA(Context context, String[] args) throws Exception {
        StringList slAssignees = new StringList();

        HashMap mpProgram = (HashMap) JPO.unpackArgs(args);
        String strObjectId = (String) mpProgram.get("rootObjectId");
        if (UIUtil.isNullOrEmpty(strObjectId)) {
            strObjectId = (String) mpProgram.get("objectId");
        }

        try {

            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domObjChange = DomainObject.newInstance(context, strObjectId);

                String strChangeObjType = domObjChange.getInfo(context, DomainConstants.SELECT_TYPE);

                if (UIUtil.isNotNullAndNotEmpty(strChangeObjType)) {
                    StringList slMemberList = new StringList();
                    if (TigerConstants.TYPE_CHANGEACTION.equals(strChangeObjType)) {
                        slMemberList = domObjChange.getInfoList(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA
                                + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.id");
                    } else if (TigerConstants.TYPE_PSS_CHANGEORDER.equals(strChangeObjType)) {
                        slMemberList = domObjChange.getInfoList(context,
                                "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.id");
                    }

                    for (Iterator<String> iterator = slMemberList.iterator(); iterator.hasNext();) {
                        String strPersonObjID = iterator.next();
                        DomainObject domPersonObject = DomainObject.newInstance(context, strPersonObjID);
                        String strPersonState = domPersonObject.getInfo(context, DomainConstants.SELECT_CURRENT);
                        if ((TigerConstants.STATE_ACTIVE).equals(strPersonState)) {
                            slAssignees.add(strPersonObjID);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error in getAssigneeForCA: ", ex);
            throw ex;
        }
        return slAssignees;
    }

    // Modified for SLC
    public Vector getImpactedProjectCounts(Context context, String[] args) throws Exception {
        Vector vProjectCountList = new Vector();
        try {
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            int strProjectCountint = 0;
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            MapList objIdList = (MapList) paramMap.get("objectList");

            for (int j = 0; j < objIdList.size(); j++) {
                StringBuffer strFormAction = new StringBuffer();
                Map objectIds = (Map) objIdList.get(j);
                String strCRObjId = (String) objectIds.get("id");

                Pattern relationship_pattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM);
                Pattern type_pattern = new Pattern(DomainConstants.TYPE_PART);
                type_pattern.addPattern(TYPE_TECHNICALSPECIFICATION);
                type_pattern.addPattern(TYPE_DOCUMENTS);
                type_pattern.addPattern(TYPE_VIEWABLE);
                type_pattern.addPattern(TYPE_PLMENTITY);
                type_pattern.addPattern(TigerConstants.TYPE_DELFMIFUNCTIONREFERENCE);

                StringList slselectObjStmts = new StringList();
                slselectObjStmts.addElement(DomainConstants.SELECT_ID);
                DomainObject domCRObject = DomainObject.newInstance(context, strCRObjId);
                // PCM : TIGTK-7062 : 27/04/2017 : AB : START
                // TIGTK-11724 | FindBug | //toDirection Expand made false | 30-Nov-2017 | SayaliD.
                MapList mapList;
                mapList = domCRObject.getRelatedObjects(context, relationship_pattern.getPattern(), // relationship pattern
                        type_pattern.getPattern(), // object pattern
                        slselectObjStmts, // object selects
                        null, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, // postPattern
                        null, null, null);

                // PCM : TIGTK-7062 : 27/04/2017 : AB : END
                if (mapList.size() == 0) {
                    // Findbug Issue correction start
                    // Date: 21/03/2017
                    // By: Asha G.
                    strFormAction.append("<input id='projectInput");
                    strFormAction.append(j);
                    // TIGTK-14849:04-07-2018:STARTS
                    strFormAction.append("' name='projectInput");
                    strFormAction.append(j);
                    // TIGTK-14849:04-07-2018:ENDS
                    strFormAction.append("' type='hidden' value='");
                    strFormAction.append("");
                    strFormAction.append("'/><a onclick=\"showModalDialog('../enterprisechangemgt/PSS_ECMAffectedProjectSummary.jsp?selectedRowNo=");
                    strFormAction.append(j);
                    strFormAction.append("&amp;program=PSS_enoECMChangeUtil:getProjectList', 800, 600, true)\">");
                    strFormAction.append("0 ");
                    strFormAction.append("</a>");
                    // Findbug Issue correction End
                } else {
                    // TIGTK-4821 - vpadhiyar - 27-02-2017 - START
                    StringBuffer sbListObjectId = new StringBuffer();
                    strProjectCountint = 0;

                    Iterator i = mapList.iterator();
                    while (i.hasNext()) {
                        Map mTempMap = (Map) i.next();
                        String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                        DomainObject domObject = DomainObject.newInstance(context, strObjId);
                        String strType = domObject.getInfo(context, DomainObject.SELECT_TYPE);

                        MapList mapProjectList = null;

                        if (strType.equals(DomainConstants.TYPE_PART)) {
                            mapProjectList = getProjectListFromEBOM(context, strObjId);
                        } else if (domObject.isKindOf(context, TYPE_DOCUMENTS) || domObject.isKindOf(context, TYPE_TECHNICALSPECIFICATION) || domObject.isKindOf(context, TYPE_VIEWABLE)
                                || domObject.isKindOf(context, TYPE_PLMENTITY)) {
                            mapProjectList = getProjectListFromPartSpecification(context, strObjId);
                        } else if (domObject.isKindOf(context, TigerConstants.TYPE_DELFMIFUNCTIONREFERENCE)) {
                            mapProjectList = getProjectListFromMBOM(context, strObjId);
                        } else {

                        }

                        if (mapProjectList != null) {
                            for (int cntProg = 0; cntProg < mapProjectList.size(); cntProg++) {
                                String str = (String) ((Map) mapProjectList.get(cntProg)).get("id");
                                if (!sbListObjectId.toString().contains(str)) {
                                    sbListObjectId.append(str);
                                    sbListObjectId.append(",");
                                    strProjectCountint++;
                                }

                            }
                        }
                    }
                    // Findbug Issue correction start
                    // Date: 21/03/2017
                    // By: Asha G.
                    strFormAction.append("<input id='projectInput");
                    strFormAction.append(j);
                    // TIGTK-14849:04-07-2018:STARTS
                    strFormAction.append("' name='projectInput");
                    strFormAction.append(j);
                    // TIGTK-14849:04-07-2018:ENDS
                    strFormAction.append("' type='hidden' value='");
                    strFormAction.append(sbListObjectId.toString());
                    strFormAction.append("'/>");
                    strFormAction.append("<a onclick=\"showModalDialog('../enterprisechangemgt/PSS_ECMAffectedProjectSummary.jsp?selectedRowNo=");
                    strFormAction.append(j);
                    strFormAction.append("&amp;program=PSS_enoECMChangeUtil:getProjectList', 800, 600, true)\">");
                    strFormAction.append(strProjectCountint);
                    strFormAction.append("</a>");
                    // Findbug Issue correction End
                    // TIGTK-4821 - vpadhiyar - 27-02-2017 - END
                }
                vProjectCountList.add(strFormAction.toString());
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getImpactedProjectCounts: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        } finally {
            ContextUtil.popContext(context);
        }
        return vProjectCountList;
    }

    // PCM TIGTK-1736 09/09/2016 : Swapnil Patil :START
    /**
     * This Method is used to get connected MCO List from current MBOM
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public MapList getConnectedMCO(Context context, String[] args) throws Exception {
        MapList mlMCOList = null;
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String strCreateAssemplyObjectId = (String) programMap.get("objectId");
            DomainObject domCreateAssembly = new DomainObject(strCreateAssemplyObjectId);

            StringList objectSelects = new StringList();
            objectSelects.addElement(DomainConstants.SELECT_ID);
            // TIGTK-3405 :change in logic for MBOM change management:Rutuja Ekatpure:Start
            Pattern rel_pattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION);
            rel_pattern.addPattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM);

            Pattern type_pattern = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
            type_pattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION);

            Pattern includeType_pattern = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);

            mlMCOList = domCreateAssembly.getRelatedObjects(context, // context
                    rel_pattern.getPattern(), // relationship pattern
                    type_pattern.getPattern(), // object pattern
                    objectSelects, // object selects
                    null, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, // relationship where clause
                    includeType_pattern, // include type
                    null, // include relationship
                    null); // include map
            // TIGTK-3405 :change in logic for MBOM change management:Rutuja Ekatpure:End
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getConnectedMCO: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return mlMCOList;
    }

    // PCM TIGTK-1736 09/09/2016 : Swapnil Patil :END

    public String createNewCA(Context context, String strCOId) throws Exception {
        DomainObject domCO = new DomainObject();
        String strCAId = "";
        try {
            if (UIUtil.isNotNullAndNotEmpty(strCOId)) {
                domCO = DomainObject.newInstance(context, strCOId);
            }
            String strOwner = context.getUser();
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            String strNewCAForPart = FrameworkUtil.autoName(context, "type_ChangeAction", "", "policy_ChangeAction", "eService Production", null, false, false);
            DomainObject domNewCAForPart = new DomainObject(strNewCAForPart);
            // TIGTK-9094 : START
            if ("User Agent".equals(strOwner)) {
                strOwner = domCO.getInfo(context, DomainConstants.SELECT_OWNER);
            }
            // TIGTK-9094 : END
            domNewCAForPart.setOwner(context, strOwner);

            strCAId = domNewCAForPart.getInfo(context, DomainConstants.SELECT_ID);
            DomainRelationship.connect(context, domCO, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, domNewCAForPart);
            ContextUtil.popContext(context);
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in createNewCA: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return strCAId;
    }

    /**
     * Description : This method will set the Requested Change Attribute value on CA and AffectedItem's relationship Modified for Performance Issue : TIGTK-3951 : 01/02/2017 : AB
     * @author abhalani
     * @args
     * @Date Sep 15, 2016
     */
    public void setRequestedChange(Context context, Map relMap, Map partReqChangeMap) throws Exception {

        try {
            if (relMap.size() > 0 && partReqChangeMap.size() > 0) {
                Iterator itr = relMap.entrySet().iterator();
                while (itr.hasNext()) {
                    Map.Entry entry = (Entry) itr.next();
                    String ObjectID = (String) entry.getKey();
                    String relId = (String) entry.getValue();
                    String strAIReqChange = (String) partReqChangeMap.get(ObjectID);

                    if (UIUtil.isNotNullAndNotEmpty(strAIReqChange)) {
                        DomainRelationship dr = new DomainRelationship(relId);
                        dr.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, strAIReqChange);
                        String strSplitChangeActionForStandardItems = PropertyUtil.getGlobalRPEValue(context, "splitChangeActionForStandardItems");
                        if (UIUtil.isNotNullAndNotEmpty(strSplitChangeActionForStandardItems) && strSplitChangeActionForStandardItems.equals("TRUE"))
                            dr.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TRANSFERFROMCRFLAG, "No");
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in setRequestedChange: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
    }

    /**
     * @param context
     * @param CAId
     * @return
     * @throws Exception
     */

    public String getCAType(Context context, String CAId) throws Exception {

        DomainObject domCA = new DomainObject(CAId);
        String strAIPolicy = domCA.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.policy");
        String CAType = "";
        if (TigerConstants.POLICY_PSS_ECPART.equals(strAIPolicy)) {
            CAType = "Part";
            // Modified for TIGTK-6255 : Priyanka Salunke : 18-04-2017 : Start
        } else if (TigerConstants.POLICY_PSS_CADOBJECT.equals(strAIPolicy) || (TigerConstants.POLICY_PSS_Legacy_CAD.equals(strAIPolicy))) {
            // Modified for TIGTK-6255 : Priyanka Salunke : 18-04-2017 : End
            CAType = "CAD";
        } else if (TigerConstants.POLICY_STANDARDPART.equals(strAIPolicy)) {
            CAType = "Standard";
        }
        return CAType;
    }

    /**
     * Description : This method is used for get StringList from Map.
     * @author abhalani
     * @args
     * @Date Sep 19, 2016
     */
    public StringList getStringListFromMap(Context context, Map inputMap, String selectable) throws Exception {
        StringList slOutput = new StringList();

        Object obj = (Object) inputMap.get(selectable);
        if (obj instanceof StringList) {
            slOutput = (StringList) obj;
        } else {
            String temp = (String) obj;
            // PCM : TIGTK-6158 : 07/04/2017 : AB
            if (UIUtil.isNotNullAndNotEmpty(temp)) {
                slOutput.add(temp);
            }
            // PCM : TIGTK-6158 : 07/04/2017 : AB
        }

        return slOutput;
        // TODO Auto-generated method stub

    }

    /***
     * method used to include MCO of same program project of CR:Rutuja Ekatpure (23/9/2016)
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList includeMCOOfSameProgProject(Context context, String args[]) throws Exception {
        try {
            StringList slMCO = new StringList();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            // Get the ObjectID of CR
            String strCRId = (String) programMap.get("objectId");

            if (UIUtil.isNotNullAndNotEmpty(strCRId)) {
                DomainObject domCR = new DomainObject(strCRId);

                // Get Program Project id of CR
                String strProgProjID = domCR.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

                StringList objectSelects = new StringList();
                objectSelects.addElement(DomainConstants.SELECT_ID);

                // Get MCO of same prog proj as CR
                MapList mpList = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, "*", "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION,
                        "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id==" + strProgProjID, true, objectSelects);

                for (int i = 0; i < mpList.size(); i++) {

                    HashMap mapMCOObject = (HashMap) mpList.get(i);
                    String strMCOOID = (String) mapMCOObject.get(DomainConstants.SELECT_ID);
                    slMCO.add(strMCOOID);
                }
            }
            return slMCO;
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in includeMCOOfSameProgProject: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
    }

    /***
     * method used to include MCO or CR of same program project of CR or MCO:Rutuja Ekatpure (4/10/2016)
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList includeObjectOfSameProgProject(Context context, String args[]) throws Exception {

        try {
            StringList slIncludeOID = new StringList();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            // Get the ObjectID of MCO/CR
            String strId = (String) programMap.get("objectId");

            if (UIUtil.isNotNullAndNotEmpty(strId)) {
                DomainObject domObj = new DomainObject(strId);
                String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
                // Get Program Project id of CR/MCO
                String strProgProjID = domObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                StringList objectSelects = new StringList();
                objectSelects.addElement(DomainConstants.SELECT_ID);
                // Findbug Issue correction start
                // Date: 21/03/2017
                // By: Asha G.
                MapList mpList = null;
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST) || strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) {
                    // Get MCO of same prog proj as CR
                    mpList = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, "*", "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION,
                            "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id==" + strProgProjID, true, objectSelects);
                } else {
                    // Get CR of same prog proj as MCO
                    mpList = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_CHANGEREQUEST, "*", "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION,
                            "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id==" + strProgProjID, true, objectSelects);
                }
                if (mpList != null) {
                    for (int i = 0; i < mpList.size(); i++) {

                        HashMap mapObject = (HashMap) mpList.get(i);
                        String strObjectID = (String) mapObject.get(DomainConstants.SELECT_ID);
                        slIncludeOID.add(strObjectID);
                    }
                }
                // Findbug Issue correction End
            }
            // Rutuja Ekatpure:int can not be casr to Map Error:22/8/2017:start
            if (slIncludeOID.size() == 0) {
                slIncludeOID.add(" ");
            }
            // Rutuja Ekatpure:int can not be casr to Map Error:22/8/2017:End
            return slIncludeOID;
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in includeObjectOfSameProgProject: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
    }

    /**
     * Description : This method is used for set CA type Last Modified for TIGTK-3774 : 19/12/2016 : AB
     * @author abhalani
     * @args
     * @Date Oct 7, 2016
     */
    public void setCATypeonNewlyCreatedCA(Context context, String[] args) throws Exception {
        // TODO Auto-generated method stub
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        boolean isPushContext = false;
        PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, args);
        // Get the ObjectID of CO
        String strCOID = (String) programMap.get("strCOObjectID");
        DomainObject domCO = new DomainObject(strCOID);

        // Get the All Change Action of related CO
        StringList slCOobjectSelects = new StringList();
        slCOobjectSelects.add(DomainObject.SELECT_ID);

        MapList mlCAConnectedToCO = domCO.getRelatedObjects(context, // context // here
                ChangeConstants.RELATIONSHIP_CHANGE_ACTION, // relationship pattern
                ChangeConstants.TYPE_CHANGE_ACTION, // object pattern
                slCOobjectSelects, // object selects
                null, // relationship selects
                false, // to direction
                true, // from direction
                (short) 0, // recursion level
                null, // object where clause
                null, // relationship where clause
                (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 0, // pageSize
                null, null, null, null);

        if (mlCAConnectedToCO.size() != 0) {
            for (int i = 0; i < mlCAConnectedToCO.size(); i++) {
                Map mapCAObject = (Map) mlCAConnectedToCO.get(i);
                String strCAOID = (String) mapCAObject.get(DomainConstants.SELECT_ID);
                DomainObject domCA = new DomainObject(strCAOID);
                String strCATypeValue = domCA.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE);

                // Change CollobrativeSpace OfChange Object
                String[] strArgs = new String[1];
                strArgs[0] = strCAOID;
                changeRequest.changeCollobrativeSpaceOfChangeObject(context, strArgs);

                if (UIUtil.isNullOrEmpty(strCATypeValue)) {
                    // set the attribute CAType on Change Action
                    String strCAType = this.getCAType(context, strCAOID);
                    // Kwagh - modified for Access related issue -Start
                    try {
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        isPushContext = true;
                        domCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, strCAType);
                    } catch (Exception ex) {
                        // TIGTK-5405 - 18-04-2017 - PTE - START
                        logger.error("Error in setCATypeonNewlyCreatedCA: ", ex);
                        throw ex;
                        // TIGTK-5405 - 18-04-2017 - PTE - End

                    } finally {
                        if (isPushContext) {
                            ContextUtil.popContext(context);
                        }
                    }
                    // Kwagh - modified for Access related issue -End
                }
            }
        }

    }

    /**
     * @param context
     * @param CAId
     * @return
     * @throws Exception
     */

    public String getCATypeFromJSP(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        // Get the ObjectID of CA
        String strCAID = (String) programMap.get("CAId");
        return this.getCAType(context, strCAID);

    }

    /***
     * This method is used to check if Route Template is connected to Program Project
     * @param context
     * @param args
     * @return int
     * @throws Exception
     */

    public int isRouteTemplateConnectedToProgProj(Context context, String args[]) throws Exception {
        int result = 1;
        try {
            String strObjectId = args[0];
            DomainObject domRT = new DomainObject(strObjectId);
            String strMessage1 = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                    "PSS_EnterpriseChangeMgt.Alert.ProgProjConnectedToRouteTemplate");
            String strMessage2 = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                    "PSS_EnterpriseChangeMgt.Alert.ProgProjDisConnectedToRouteTemplate");
            StringList slConnectedProgProj = new StringList();
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(DomainConstants.SELECT_ID);
            slObjectSelects.add(DomainConstants.SELECT_NAME);
            MapList mlConnectedProgProj = domRT.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES, // relationship pattern
                    TigerConstants.TYPE_PSS_PROGRAMPROJECT, // object pattern
                    slObjectSelects, // object selects
                    null, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, // relationship where clause
                    (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    null, null, null, null);

            if (mlConnectedProgProj.isEmpty() || mlConnectedProgProj.size() == 0) {
                result = 0;
            } else {
                for (int i = 0; i < mlConnectedProgProj.size(); i++) {
                    Map tempMap = (Map) mlConnectedProgProj.get(i);
                    String strName = (String) tempMap.get(DomainConstants.SELECT_NAME);
                    if (!slConnectedProgProj.contains(strName))
                        slConnectedProgProj.add(strName);
                }
                String strFinalMsg = strMessage1.concat("\n").concat(FrameworkUtil.join(slConnectedProgProj, "\n")).concat("\n").concat(strMessage2);
                MqlUtil.mqlCommand(context, "notice $1", strFinalMsg);
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in isRouteTemplateConnectedToProgProj: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return result;
    }

    // PCM TIGTK-3646 : 23/11/2016 : KWagh : Start
    /***
     * This method is used to transfer Ownership of Change Request Object
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    public void transferOwnership(Context context, String[] args) throws Exception {
        boolean isPushContext = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);

            String objectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
            String newOwner = (String) requestMap.get(ChangeConstants.NEW_OWNER);
            String strTransferReason = (String) requestMap.get("TransferReason");
            if (UIUtil.isNotNullAndNotEmpty(objectId)) {
                DomainObject domCR = new DomainObject(objectId);
                // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Starts
                String strContextUser = context.getUser();
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isPushContext = true;

                // TIGTK-7616:Rutuja Ekatpure: On CR transfer ownership two notification send ,one from change owner trigger on CR and
                // one from post process of transfer ownership command,notification from change owner trigger on CR not required becuase of mail format
                // so adding below code:9/5/2017:start
                MqlUtil.mqlCommand(context, false, "trigger off", false);
                // PCM : TIGTK-3707 : 1/12/16 : AB : START
                domCR.setOwner(context, newOwner);
                // PCM : TIGTK-3707 : 1/12/16 : AB : END
                MqlUtil.mqlCommand(context, false, "trigger on", false);
                // TIGTK-7616:Rutuja Ekatpure:9/5/2017:End

                Map payload = new HashMap();
                payload.put("toList", new StringList(newOwner));
                payload.put("fromList", new StringList(strContextUser));
                // TIGTK-7580 : START
                if (UIUtil.isNotNullAndNotEmpty(strTransferReason)) {
                    payload.put("TransferComments", strTransferReason);
                }
                // TIGTK-7580 : END
                
                // TIGTK-18230 : START
                StringList slCAs = null;
                if(domCR.isKindOf(context, TigerConstants.TYPE_CHANGEORDER)) {
                    slCAs = domCR.getInfoList(context, "from["+ChangeConstants.RELATIONSHIP_CHANGE_ACTION+"].to.id");
					if(null != slCAs) { 
                    int iSize = slCAs.size();
                    for (int i = 0; i < iSize; i++)
                        DomainObject.newInstance(context, slCAs.get(i).toString()).setOwner(context, newOwner);
				}
                }
                // TIGTK-18230 : END
                
                emxNotificationUtil_mxJPO.objectNotification(context, objectId, "PSS_TransferOwnershipNotification", payload);
                // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Ends
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in transferOwnership: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        } finally {
            if (isPushContext) {
                ContextUtil.popContext(context);
            }
        }
    }

    // PCM TIGTK-3646 : 23/11/2016 : KWagh : End

    // PCM TIGTK-3921: 12/01/2017 : KWagh : START

    /**
     * Method to get the connected Symmetrical Parts to Original Part.
     * @param context
     * @param slselectedItemsList
     *            - Selected Item for Add Affected Item in CR which is comes from PSS_ECMFullSearchPostProcess.jsp
     * @return StringList - Contains the Object Id of the Part which is Passed in method argument and connected Symmetrical Part with them
     * @throws Exception
     */
    public StringList getSymmetricalPartForCR(Context context, StringList slselectedItemsList) throws Exception {
        try {
            StringList selectStmts = new StringList(1);
            selectStmts.addElement(DomainConstants.SELECT_ID);

            StringList slSelectedItems = new StringList();
            StringList slSymmetricalItems = new StringList();

            StringList slObjSelects = new StringList();
            slObjSelects.addElement(DomainConstants.SELECT_TYPE);
            slObjSelects.addElement(DomainConstants.SELECT_POLICY);
            // Findbug Issue correction start
            // Date: 21/03/2017
            // By: Asha G.
            Map objectsMap = null;
            // Findbug Issue correction End

            String strObjectPolicy = "";
            for (int i = 0; i < slselectedItemsList.size(); i++) {
                slSelectedItems.add(slselectedItemsList.get(i));
                DomainObject domSelectedItem = DomainObject.newInstance(context, (String) slSelectedItems.get(i));
                objectsMap = domSelectedItem.getInfo(context, slObjSelects);
                strObjectPolicy = (String) objectsMap.get(DomainConstants.SELECT_POLICY);

                if (((strObjectPolicy.equals(TigerConstants.POLICY_PSS_ECPART)) || strObjectPolicy.equals(TigerConstants.POLICY_PSS_DEVELOPMENTPART))) {

                    MapList mlSymmetricalPartObject = domSelectedItem.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, // relationship pattern
                            DomainConstants.TYPE_PART, // object pattern
                            selectStmts, // object selects
                            DomainObject.EMPTY_STRINGLIST, // relationship selects
                            true, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, 0); // relationship where clause

                    if (mlSymmetricalPartObject.size() > 0) {
                        for (int j = 0; j < mlSymmetricalPartObject.size(); j++) {
                            Map<String, String> map = (Map<String, String>) mlSymmetricalPartObject.get(j);
                            String strSymmetricpartId = map.get(DomainObject.SELECT_ID);
                            slSymmetricalItems.add(strSymmetricpartId); // Add Symmetrical Part Id to StringList of Symmetrical Items

                        }
                    }
                }
            }

            return slSymmetricalItems;
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getSymmetricalPartForCR: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
    }

    // PCM TIGTK-3921: 12/01/2017 : KWagh : End

    // PCM TIGTK-3921: 12/01/2017 : KWagh : START

    /**
     * Method will return the list of CAD connected to Part (EC/ STD/DEV)
     * @param context
     * @param slselectedItemsList
     * @return
     * @throws Exception
     */
    public StringList getCADObjectsForCR(Context context, StringList slselectedItemsList) throws Exception {
        StringList slItems = new StringList();
        String strCADID = DomainConstants.EMPTY_STRING;
        try {
            // PCM : TIGTK-7928 : 29/05/2017 : AB : START
            Pattern relpattern = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
            relpattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
            // PCM : TIGTK-7928 : 29/05/2017 : AB : END
            for (int cnt = 0; cnt < slselectedItemsList.size(); cnt++) {
                String strPartID = (String) slselectedItemsList.get(cnt);
                DomainObject domObject = DomainObject.newInstance(context, strPartID);

                String strIsPart = domObject.getInfo(context, "type.kindof[" + DomainConstants.TYPE_PART + "]");
                if (strIsPart.equalsIgnoreCase("True")) {

                    StringList slObjectSelect = new StringList(DomainConstants.SELECT_ID);
                    slObjectSelect.add(DomainConstants.SELECT_CURRENT);

                    MapList mlRelatedObjects = domObject.getRelatedObjects(context, relpattern.getPattern(), "*", slObjectSelect, DomainObject.EMPTY_STRINGLIST, false, true, (short) 1, null, null, 0);

                    if (!mlRelatedObjects.isEmpty()) {
                        for (int i = 0; i < mlRelatedObjects.size(); i++) {
                            Map mCADObj = (Map) mlRelatedObjects.get(i);
                            // PCM : TIGTK-8535 : 12/06/2017 : AB : START
                            String strCADCurrent = (String) mCADObj.get(DomainConstants.SELECT_CURRENT);
                            strCADID = (String) mCADObj.get(DomainConstants.SELECT_ID);
                            if (!(TigerConstants.STATE_CAD_APPROVED.equalsIgnoreCase(strCADCurrent) || TigerConstants.STATE_OBSOLETE.equalsIgnoreCase(strCADCurrent))) {
                                slItems.add(strCADID);
                            }
                            // PCM : TIGTK-8535 : 12/06/2017 : AB : END
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getCADObjectsForCR: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return slItems;

    }

    // PCM TIGTK-3921: 12/01/2017 : KWagh : End

    // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
    /**
     * Method to check If Relationship already exists or not
     * @param context
     * @param slselectedItemsList
     * @return
     * @throws Exception
     */
    public StringList checkRelationshipExist(Context context, StringList slToList, StringList slFromList) throws Exception {
        try {
            // Fing Bug Issue : Priyanka Salunke
            if (slToList != null && !slToList.isEmpty() && slFromList != null && !slFromList.isEmpty()) {
                int slSize = slFromList.size();
                for (int i = 0; i < slSize; i++) {
                    if ((slToList.contains((String) slFromList.get(i)))) {
                        slToList.remove((String) slToList.get(i));
                    }
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in checkRelationshipExist: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
        return slToList;
    }

    /**
     * Description : Added when user clicks on Add Eixsting CO to CR
     * @author abhalani
     * @args
     * @Date Sep 15, 2016
     */
    public void connectCOToCR(Context context, String args[]) throws Exception {
        StringList slobjectSelects = new StringList();
        StringList slrelSelects = new StringList();
        // MapList mlCRAffectedItems = new MapList();
        String strCRId = "";
        String strCOId = "";
        DomainObject domCR = new DomainObject();
        DomainObject domNewCA = new DomainObject();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        Map mPartToRC = new HashMap();

        try {
            StringList slChangeOrderIds = (StringList) programMap.get("ObjId");
            strCRId = (String) programMap.get("CR_ObjectId");
            if (UIUtil.isNotNullAndNotEmpty(strCRId)) {
                domCR = DomainObject.newInstance(context, strCRId);
            }
            // PCM TIGTK-10830 : 2/11/17 : START
            String strCRconnectedProgramid = domCR.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            // PCM TIGTK-10830 : 2/11/17 : END
            slobjectSelects.add(DomainConstants.SELECT_ID);
            slobjectSelects.add(DomainConstants.SELECT_CURRENT);
            slobjectSelects.add(DomainConstants.SELECT_POLICY);
            slobjectSelects.add(DomainConstants.SELECT_TYPE);
            slobjectSelects.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]");
            slobjectSelects.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current");
            slobjectSelects.add("to[" + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
            slrelSelects.add(DomainRelationship.SELECT_RELATIONSHIP_ID);
            slrelSelects.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
            // PCM TIGTK-3243 : 23/09/16 : AB : START
            String where = "policy != PSS_Development_Part && policy != PSS_MBOM && (current==Released || current==Release || current == 'In Work' || current == Preliminary || current==Review)";// "to["+ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM+"]
            // ==
            // false";

            // Get CR Related Affected Items
            MapList mlCRAffectedItems = domCR.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, // relationship pattern
                    DomainConstants.QUERY_WILDCARD, // object pattern
                    slobjectSelects, // object selects
                    slrelSelects, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    where, // object where clause
                    null, // relationship where clause
                    (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    null, null, null, null);
            // PCM TIGTK-3243 : 23/09/16 : AB : END
            Map mapAICurrentState = new HashMap();
            if (!mlCRAffectedItems.isEmpty() && mlCRAffectedItems.size() > 0) {
                StringList slPartType = new StringList();
                StringList slCADType = new StringList();
                StringList slStandardType = new StringList();
                // PCM TIGTK-3847 : 27/12/2016 : KWagh : START
                StringList slCAState = new StringList();
                slCAState.add("Complete");

                boolean isPart = false;
                boolean isCAD = false;
                boolean isStandard = false;
                int intCRAffectedItemsListSize = mlCRAffectedItems.size();
                for (int k = 0; k < intCRAffectedItemsListSize; k++) {
                    Map mAffectedItem = (Map) mlCRAffectedItems.get(k);
                    StringList slSpecsConnectedPartPolicy = getStringListFromMap(context, mAffectedItem, "to[" + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
                    String strPolicy = (String) mAffectedItem.get(DomainObject.SELECT_POLICY);
                    String strCRRequesedChangeValue = (String) mAffectedItem.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                    String strAIId = (String) mAffectedItem.get(DomainObject.SELECT_ID);
                    String strPartState = (String) mAffectedItem.get(DomainObject.SELECT_CURRENT);
                    mapAICurrentState.put(strAIId, strPartState);
                    // PCM TIGTK-10830 : 2/11/17 : START
                    DomainObject domAI = DomainObject.newInstance(context, strAIId);
                    String strGoveringPrjId = domAI.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                    if (strGoveringPrjId == null) {
                        strGoveringPrjId = DomainConstants.EMPTY_STRING;
                    }
                    if ((UIUtil.isNullOrEmpty(strGoveringPrjId)) || (strGoveringPrjId.equalsIgnoreCase(strCRconnectedProgramid))) {
                        // PCM TIGTK-10830 : 2/11/17 : END

                        // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                        // Check if Affected Item Connected To Active CO
                        // PCM : same like TIGTK-4119 : 02/02/2017 : AB : START
                        String strRelPattern = ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM;
                        String selectCoIsActive = new StringBuilder("evaluate[((to[").append(strRelPattern).append("].from.to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION)
                                .append("].from.current smatchlist \"Prepare,In Work,In Approval\" \",\"))]").toString();
                        // Findbug Issue correction start
                        // Date: 21/03/2017
                        // By: Asha G.
                        DomainObject domObj = new DomainObject(strAIId);
                        String val = domObj.getInfo(context, selectCoIsActive);
                        boolean isConnectedActiveCO = Boolean.valueOf(val);

                        // Findbug Issue correction End
                        // PCM : same like TIGTK-4119 : 02/02/2017 : AB : END
                        // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                        // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts

                        // PCM: TIGTK-7779 : 17/05/2017: TS: START
                        /*
                         * if (!(ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strCRRequesedChangeValue) && (TigerConstants.STATE_RELEASED_CAD_OBJECT.equalsIgnoreCase(strPartState) ||
                         * TigerConstants.STATE_PART_RELEASE.equalsIgnoreCase(strPartState)))) {
                         */
                        if ((ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strCRRequesedChangeValue))
                                && (TigerConstants.STATE_RELEASED_CAD_OBJECT.equalsIgnoreCase(strPartState) || TigerConstants.STATE_PART_RELEASE.equalsIgnoreCase(strPartState))) {
                            strCRRequesedChangeValue = ChangeConstants.FOR_REVISE;
                        }
                        if ((TigerConstants.POLICY_PSS_ECPART.equals(strPolicy)) && !isConnectedActiveCO) {
                            slPartType.add(strAIId);
                            mPartToRC.put(strAIId, strCRRequesedChangeValue);
                        }
                        // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                        // Added for Standard Part
                        // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                        else if ((TigerConstants.POLICY_STANDARDPART.equals(strPolicy)) && !isConnectedActiveCO) {
                            slStandardType.add(strAIId);
                            mPartToRC.put(strAIId, strCRRequesedChangeValue);
                        }
                        // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                        // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                        else if ((TigerConstants.POLICY_PSS_CADOBJECT.equals(strPolicy) || TigerConstants.POLICY_PSS_Legacy_CAD.equals(strPolicy)) && !isConnectedActiveCO
                                && !slSpecsConnectedPartPolicy.contains(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
                            slCADType.add(strAIId);
                            mPartToRC.put(strAIId, strCRRequesedChangeValue);
                        }
                        // }
                        // PCM: TIGTK-7779 : 17/05/2017: TS: END
                        // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                    }
                }
                StringList slValidCADId = checkStateOfCADForTransferAffectedItemsFromCRToCO(context, slPartType, slStandardType, slCADType, mapAICurrentState);
                slCADType = slValidCADId;
                // PCM TIGTK-3847 : 27/12/2016 : KWagh : End
                if (!slChangeOrderIds.isEmpty()) {
                    strCOId = (String) slChangeOrderIds.get(0);

                    if (UIUtil.isNotNullAndNotEmpty(strCOId)) {
                        DomainObject domCO = DomainObject.newInstance(context, strCOId);

                        StringList slCOobjectSelects = new StringList();
                        slCOobjectSelects.add(DomainObject.SELECT_ID);
                        slCOobjectSelects.add("from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.policy");

                        MapList mlCAConnectedToCO = domCO.getRelatedObjects(context, // context // here
                                ChangeConstants.RELATIONSHIP_CHANGE_ACTION, // relationship pattern
                                ChangeConstants.TYPE_CHANGE_ACTION, // object pattern
                                slCOobjectSelects, // object selects
                                null, // relationship selects
                                false, // to direction
                                true, // from direction
                                (short) 0, // recursion level
                                null, // object where clause
                                null, // relationship where clause
                                (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 0, // pageSize
                                null, null, null, null);

                        // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                        StringList slConnectedCAItem = new StringList();
                        // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts

                        for (int j = 0; j < mlCAConnectedToCO.size(); j++) {
                            Map mTemp = (Map) mlCAConnectedToCO.get(j);
                            String strCAId = (String) mTemp.get(DomainObject.SELECT_ID);
                            String strCAType = getCAType(context, strCAId);
                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                            DomainObject domChangeAction = DomainObject.newInstance(context, strCAId);
                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts

                            if ("Part".equals(strCAType) && isPart == false && slPartType.size() > 0) {

                                // connect Part List to CA
                                // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                                slConnectedCAItem = domChangeAction.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id");
                                slPartType = checkRelationshipExist(context, slPartType, slConnectedCAItem);

                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                MqlUtil.mqlCommand(context, "trigger off");
                                Map mCATOPartMap = DomainRelationship.connect(context, new DomainObject(strCAId), ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                        ((String) FrameworkUtil.join(slPartType, ",")).split(","));
                                MqlUtil.mqlCommand(context, "trigger on");
                                ContextUtil.popContext(context);
                                // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                                setRequestedChange(context, mCATOPartMap, mPartToRC);
                                isPart = true;

                                // Harika
                            }
                            if ("CAD".equals(strCAType) && isCAD == false && slCADType.size() > 0) {
                                // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                                // connect CAD List to CA
                                slConnectedCAItem = domChangeAction.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id");
                                slCADType = checkRelationshipExist(context, slCADType, slConnectedCAItem);
                                // Findbug Issue correction start
                                // Date: 21/03/2017
                                // By: Asha G.
                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                MqlUtil.mqlCommand(context, "trigger off");
                                Map mCATOCADMap = DomainRelationship.connect(context, new DomainObject(strCAId), ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                        ((String) FrameworkUtil.join(slCADType, ",")).split(","));
                                MqlUtil.mqlCommand(context, "trigger on");
                                ContextUtil.popContext(context);
                                // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                                setRequestedChange(context, mCATOCADMap, mPartToRC);
                                isCAD = true;
                                // Findbug Issue correction End

                            }
                            if ("Standard".equals(strCAType) && isCAD == false && slStandardType.size() > 0) {

                                // connect CAD List to CA
                                // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                                slConnectedCAItem = domChangeAction.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id");
                                slStandardType = checkRelationshipExist(context, slStandardType, slConnectedCAItem);

                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                MqlUtil.mqlCommand(context, "trigger off");
                                Map mCATOStandardMap = DomainRelationship.connect(context, new DomainObject(strCAId), ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                        ((String) FrameworkUtil.join(slStandardType, ",")).split(","));
                                MqlUtil.mqlCommand(context, "trigger on");
                                ContextUtil.popContext(context);
                                // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                                setRequestedChange(context, mCATOStandardMap, mPartToRC);
                                isStandard = true;

                            }

                        }

                        if (isCAD == false && slCADType.size() > 0) {
                            // ceate new CA and attach all AI of CAD

                            String CAId = createNewCA(context, strCOId);
                            domNewCA = DomainObject.newInstance(context, CAId);
                            domNewCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, "CAD");
                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts

                            // Findbug Issue correction start
                            // Date: 22/03/2017
                            // By: Asha G.
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            MqlUtil.mqlCommand(context, "trigger off");
                            Map mCATOCADMap = DomainRelationship.connect(context, domNewCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                    ((String) FrameworkUtil.join(slCADType, ",")).split(","));
                            MqlUtil.mqlCommand(context, "trigger on");
                            ContextUtil.popContext(context);
                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                            setRequestedChange(context, mCATOCADMap, mPartToRC);
                            // Findbug Issue correction End

                        }

                        if (isPart == false && slPartType.size() > 0) {
                            // ceate new CA and attach all AI of Part
                            String CAId = createNewCA(context, strCOId);
                            domNewCA = DomainObject.newInstance(context, CAId);
                            domNewCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, "Part");
                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts

                            // Findbug Issue correction start
                            // Date: 22/03/2017
                            // By: Asha G.
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            MqlUtil.mqlCommand(context, "trigger off");
                            Map mCATOPartMap = DomainRelationship.connect(context, domNewCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                    ((String) FrameworkUtil.join(slPartType, ",")).split(","));

                            MqlUtil.mqlCommand(context, "trigger on");
                            ContextUtil.popContext(context);
                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                            setRequestedChange(context, mCATOPartMap, mPartToRC);
                            // Findbug Issue correction End

                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts

                        }

                        if (isStandard == false && slStandardType.size() > 0) {
                            // ceate new CA and attach all AI of Part
                            String CAId = createNewCA(context, strCOId);
                            domNewCA = DomainObject.newInstance(context, CAId);
                            domNewCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, "Standard");
                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts

                            // Findbug Issue correction start
                            // Date: 22/03/2017
                            // By: Asha G.
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            MqlUtil.mqlCommand(context, "trigger off");
                            Map mCATOStandardMap = DomainRelationship.connect(context, domNewCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                                    ((String) FrameworkUtil.join(slStandardType, ",")).split(","));
                            MqlUtil.mqlCommand(context, "trigger on");
                            ContextUtil.popContext(context);
                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                            setRequestedChange(context, mCATOStandardMap, mPartToRC);
                            // Findbug Issue correction end

                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                        }

                    }
                    // connect all CO to CR
                    DomainRelationship.connect(context, domCR, new RelationshipType(ChangeConstants.RELATIONSHIP_CHANGE_ORDER), true, ((String) FrameworkUtil.join(slChangeOrderIds, ",")).split(","));
                }
            } else {
                DomainRelationship.connect(context, domCR, new RelationshipType(ChangeConstants.RELATIONSHIP_CHANGE_ORDER), true, ((String) FrameworkUtil.join(slChangeOrderIds, ",")).split(","));
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in connectCOToCR: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End

        }
    }

    // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
    // FS_01 Change Request |13/02/2017 : Harika Varanasi : Start
    public Vector getRelatedProjectCRs(Context context, String[] args) throws Exception {
        Vector vProjectCountList = new Vector();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            MapList objIdList = (MapList) paramMap.get("objectList");
            int objListSize = objIdList.size();
            DomainObject domObject = DomainObject.newInstance(context);
            StringBuffer strFormAction;

            String strSelectsrelatedCR = "to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.id";
            StringList slRelatedCRList = null;
            for (int i = 0; i < objListSize; i++) {
                strFormAction = new StringBuffer();

                Map objectIds = (Map) objIdList.get(i);
                String strObjId = (String) objectIds.get("id");
                domObject.setId(strObjId);
                slRelatedCRList = domObject.getInfoList(context, strSelectsrelatedCR);

                MapList mapProjectList = null;

                int strProjectCountint = 0;
                strProjectCountint = slRelatedCRList.size();

                if (strProjectCountint == 0) {
                    // Findbug Issue correction start
                    // Date: 21/03/2017
                    // By: Asha G.
                    strFormAction.append(strProjectCountint);

                } else {
                    strFormAction.append("<input id='CRInput");
                    strFormAction.append(i);
                    // TIGTK-14849:04-07-2018:STARTS
                    strFormAction.append("' name='CRInput");
                    strFormAction.append(i);
                    // TIGTK-14849:04-07-2018:ENDS
                    strFormAction.append("' type='hidden' value='");
                    strFormAction.append(FrameworkUtil.join(slRelatedCRList, ","));
                    strFormAction.append("'/><a onclick=\"showModalDialog('../enterprisechangemgt/PSS_ECMAffectedProjectSummary.jsp?selectedRowNo=");
                    strFormAction.append(i);
                    strFormAction.append("&amp;mode=partRelatedCRs&amp;program=PSS_enoECMChangeUtil:getPartCRList', 800, 600, true)\">");
                    strFormAction.append(strProjectCountint);
                    strFormAction.append("</a>");
                    // Findbug Issue correction end
                }

                vProjectCountList.add(strFormAction.toString());
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getRelatedProjectCRs: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return vProjectCountList;
    }

    public MapList getPartCRList(Context context, String[] args) throws Exception {
        MapList mapProjectList = new MapList();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String requestString = (String) programMap.get("CRList");

            StringList slIds = FrameworkUtil.split(requestString, ",");
            if (slIds != null && (!slIds.isEmpty())) {
                String strId = "";
                for (int i = 0; i < slIds.size(); i++) {
                    strId = (String) slIds.get(i);
                    if (UIUtil.isNotNullAndNotEmpty(strId)) {
                        Map<String, String> mapObject = new HashMap<String, String>();
                        mapObject.put("id", strId);
                        mapProjectList.add(mapObject);
                    }

                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 18-04-2017 - PTE - START
            logger.error("Error in getPartCRList: ", ex);
            throw ex;
            // TIGTK-5405 - 18-04-2017 - PTE - End
        }
        return mapProjectList;
    }

    // FS_01 Change Request |13/02/2017 : Harika Varanasi : Start
    // PCM TIGTK-4473 | 07/03/17 : Pooja Mantri : Start
    /**
     * This method is used to get the Lead Program Manager and dispaly it in table PSS_CRAssessmentAffectedProjectsTable
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    public Vector getProjectLeadProgramManager(Context context, String[] args) throws Exception {
        Vector vProjectCountList = new Vector();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList objIdList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < objIdList.size(); i++) {
            String strPerson = "";
            try {
                StringList busSelects = new StringList();
                StringList relSelects = new StringList();
                busSelects.add(DomainObject.SELECT_ID);
                busSelects.add(DomainObject.SELECT_TYPE);
                busSelects.add(DomainObject.SELECT_NAME);
                relSelects.add(DomainObject.SELECT_RELATIONSHIP_ID);
                relSelects.add("attribute[PSS_Role]");
                relSelects.add("attribute[PSS_Position]");

                DomainObject domProject = new DomainObject(((Map) objIdList.get(i)).get(SELECT_ID) + "");

                MapList mapPerson = domProject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, DomainConstants.TYPE_PERSON, busSelects, relSelects, true, true, (short) 1,
                        null, null, 0);
                for (int intIndex = 0; intIndex < mapPerson.size(); intIndex++) {
                    Map currentMap = (Map) mapPerson.get(intIndex);

                    if (currentMap.get("attribute[PSS_Role]").equals(TigerConstants.ROLE_PSS_PROGRAM_MANAGER) && currentMap.get("attribute[PSS_Position]").equals("Lead")) {
                        if (UIUtil.isNullOrEmpty(strPerson)) {
                            strPerson = (String) currentMap.get(DomainObject.SELECT_NAME);
                        } else {
                            strPerson = strPerson + "," + currentMap.get(DomainObject.SELECT_NAME);
                        }

                    }

                }
            } catch (Exception ex) {
                // TIGTK-5405 - 18-04-2017 - PTE - START
                logger.error("Error in getProjectLeadProgramManager: ", ex);
                throw ex;
                // TIGTK-5405 - 18-04-2017 - PTE - End

            }
            vProjectCountList.add(strPerson);

        }

        return vProjectCountList;
    }

    // PCM TIGTK-4473 | 07/03/17 : Pooja Mantri : End

    // PCM TIGTK-6957: 21/04/2017 : KWagh : START

    /**
     * This method is used for Get include OID for Move to existing CA
     * @author KWagh
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList includeOIDForCAAddExisting(Context context, String[] args) throws Exception {
        try {
            StringList slFinalList = new StringList();
            StringList slCAIncludeOID = new StringList();
            Map paramMap = JPO.unpackArgs(args);
            String strCOID = (String) paramMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strCOID)) {
                DomainObject domCO = new DomainObject(strCOID);
                StringList slObjectSel = new StringList();
                slObjectSel.add(DomainConstants.SELECT_ID);

                MapList mlChangeAction = domCO.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, slObjectSel, null, false, true, (short) 1,
                        null, null, 0);

                int nCASize = mlChangeAction.size();
                // iterate list of CA
                for (int cnt = 0; cnt < nCASize; cnt++) {
                    String strCAID = (String) ((Map) mlChangeAction.get(cnt)).get(DomainObject.SELECT_ID);
                    slCAIncludeOID.add(strCAID);
                }
            }
            String strSelectedCAID = (String) paramMap.get("caIdsList");
            strSelectedCAID = strSelectedCAID.substring(1, strSelectedCAID.length() - 1);
            StringList caIdsList = new StringList();
            // Added for TIGTK-7945 : Priyanka Salunke : 29-05-2017 : START
            if (strSelectedCAID.contains(",")) {
                caIdsList = FrameworkUtil.split(strSelectedCAID, ",");
            } else {
                caIdsList.add(strSelectedCAID);
            }
            Map paramMapForJPO = new HashMap();
            paramMapForJPO.put("caIdsList", caIdsList);
            String[] arg = JPO.packArgs(paramMapForJPO);
            StringList lsExcludeOID = this.CAOfSameTypeAffectedItem(context, arg);
            int intExcludeOIDSize = lsExcludeOID.size();
            int intIncludeOIDSize = slCAIncludeOID.size();
            if (intExcludeOIDSize > 0) {
                for (int i = 0; i < intExcludeOIDSize; i++) {
                    String strExcludeOID = (String) lsExcludeOID.get(i);
                    strExcludeOID = strExcludeOID.trim();
                    for (int j = 0; j < intIncludeOIDSize; j++) {
                        String strIncludeOID = (String) slCAIncludeOID.get(j);
                        String strIncludeOIDActual = strIncludeOID.trim();
                        if (strIncludeOIDActual.equals(strExcludeOID)) {
                            slFinalList.add(strIncludeOID);
                            break;
                        }
                    }
                }
                slCAIncludeOID.removeAll(slFinalList);
            }
            return slCAIncludeOID;
        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeUtil : includeOIDForCAAddExisting() : ", ex);
            throw ex;
        }
        // Added for TIGTK-7945 : Priyanka Salunke : 29-05-2017 : END
    }

    // PCM TIGTK-6957: 21/04/2017 : KWagh : END

    // TIGTK-9930:26/09/2017:Start
    /***
     * This is used to replace old revision of route template connected to all Projects when new revision is Active.
     * @param context
     * @param args
     * @return int
     * @throws Exception
     */

    public void replaceRouteTemplateOnRevise(Context context, String args[]) throws Exception {

        try {
            String strRouteTemplateID = args[0];
            DomainObject domRT = DomainObject.newInstance(context, strRouteTemplateID);

            if (domRT.isLastRevision(context)) {
                // TIGTK-9930:Rutuja Ekatpure:24/11/2017:Start
                String strName = domRT.getInfo(context, DomainConstants.SELECT_NAME);
                String strWhereExp = "current == " + DomainConstants.STATE_ROUTE_TEMPLATE_ACTIVE + "&&" + DomainObject.SELECT_ID + "!= " + strRouteTemplateID + "&&(to["
                        + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "]=='True')";
                StringList slSelect = new StringList();
                slSelect.addElement(DomainConstants.SELECT_ID);
                slSelect.addElement(DomainConstants.SELECT_REVISION);

                // Get all the Active Revisions of Route templates which are connected to any program-project
                MapList previousRevisionsOfRouteTemplate = DomainObject.findObjects(context, DomainConstants.TYPE_ROUTE_TEMPLATE, strName, DomainConstants.QUERY_WILDCARD,
                        DomainConstants.QUERY_WILDCARD, TigerConstants.VAULT_ESERVICEPRODUCTION, strWhereExp, true, slSelect);

                // Sort all revision in descending order
                previousRevisionsOfRouteTemplate.sort(DomainConstants.SELECT_REVISION, "descending", "String");

                // Get Highest Active revion which is connected to any program-project
                if (!previousRevisionsOfRouteTemplate.isEmpty()) {
                    Map mRevisionsOfRouteTemplate = (Map) previousRevisionsOfRouteTemplate.get(0);
                    String strRouteID = (String) mRevisionsOfRouteTemplate.get(DomainConstants.SELECT_ID);

                    DomainObject domRTPrevisousRev = DomainObject.newInstance(context, strRouteID);

                    StringList slObjectSelects = new StringList();
                    slObjectSelects.add(DomainConstants.SELECT_ID);

                    StringList slRelSelects = new StringList();
                    slRelSelects.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                    // Get list of Program-projects connected to the Route template
                    MapList mlConnectedProgramProjects = domRTPrevisousRev.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES, TigerConstants.TYPE_PSS_PROGRAMPROJECT,
                            slObjectSelects, slRelSelects, true, false, (short) 0, null, null, (short) 0);

                    if (!mlConnectedProgramProjects.isEmpty()) {

                        Iterator itrPP = mlConnectedProgramProjects.iterator();
                        while (itrPP.hasNext()) {
                            Map mpProjectInfo = (Map) itrPP.next();

                            String strProjectRTRelID = (String) mpProjectInfo.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                            // Replace old revision of Route template with the new one
                            DomainRelationship.setToObject(context, strProjectRTRelID, domRT);

                        }

                    }
                }
                // TIGTK-9930:Rutuja Ekatpure:24/11/2017:End
            }
        } catch (RuntimeException re) {

            logger.error("Error in replaceRouteTemplateOnRevise: ", re);

            throw re;
        } catch (Exception ex) {

            logger.error("Error in replaceRouteTemplateOnRevise: ", ex);
            throw ex;

        }
    }

    // TIGTK-9930:26/09/2017:End

    /**
     * This method is used to change the Transfer ownership of Part and CDA object
     * @param context
     * @param args
     * @throws Exception
     *             TIGKT-17752
     */

    public void getTransferOwnership(Context context, String args[]) throws Exception {
        try {
            logger.debug("PSS_enoECMChangeUtil : getTransferOwnership : START");
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strObjectId = (String) paramMap.get("objectId");
            String strProjectCode = (String) paramMap.get("ProjectCode");
            // TIGTK-17752 : 31/10/2018 : Start
            String strRowIds = (String) paramMap.get("rowIds");
            StringList slRowIdsList = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(strRowIds))
                slRowIdsList = FrameworkUtil.split(strRowIds, ",");
            else
                slRowIdsList.add(strObjectId);

            String[] strArrayRowIds = (String[]) slRowIdsList.toArray(new String[slRowIdsList.size()]);
            StringList slSelect = new StringList(DomainConstants.SELECT_ID);
            slSelect.add(DomainConstants.SELECT_CURRENT);
            slSelect.add(DomainConstants.SELECT_TYPE);
            slSelect.add(DomainConstants.SELECT_POLICY);
            MapList mlRowIDInfo = DomainObject.getInfo(context, strArrayRowIds, slSelect);
            Iterator<HashMap> itrRowId = mlRowIDInfo.iterator();
            while (itrRowId.hasNext()) {

                Map mapPart = itrRowId.next();
                String strPartId = (String) mapPart.get(DomainConstants.SELECT_ID);
                String strPartState = (String) mapPart.get(DomainConstants.SELECT_CURRENT);
                String strPartType = (String) mapPart.get(DomainConstants.SELECT_TYPE);
                String strPartPolicy = (String) mapPart.get(DomainConstants.SELECT_POLICY);

                DomainObject domPart = DomainObject.newInstance(context, strPartId);

                if ((DomainConstants.TYPE_PART.equalsIgnoreCase(strPartType))
                        && ((TigerConstants.POLICY_PSS_DEVELOPMENTPART.equalsIgnoreCase(strPartPolicy) || CPCConstants.POLICY_CONFIGURED_PART.equalsIgnoreCase(strPartPolicy)
                                || TigerConstants.STATE_PART_OBSOLETE.equalsIgnoreCase(strPartState) || TigerConstants.STATE_PSS_CANCELPART_CANCELLED.equalsIgnoreCase(strPartState)))) {
                    itrRowId.remove();
                    continue;
                }
                if (domPart.isKindOf(context, CommonDocument.TYPE_DOCUMENTS) && !TigerConstants.STATE_RELEASED_CAD_OBJECT.equalsIgnoreCase(strPartState)) {
                    itrRowId.remove();
                    continue;
                }

                // TIGTK-17752 : 31/10/2018 : END
                HashMap hmImpactedVsRelID = new HashMap();
                BusinessObject busPPObj = new BusinessObject(TigerConstants.TYPE_PSS_PROGRAMPROJECT, strProjectCode, "", TigerConstants.VAULT_ESERVICEPRODUCTION);
                busPPObj.open(context);
                String strPPId = busPPObj.getObjectId();
                busPPObj.close(context);

                StringList slObjectSel = new StringList(2);
                slObjectSel.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].id");
                slObjectSel.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");

                Map mProgramProjectInfo = domPart.getInfo(context, slObjectSel);
                String strGoverningProjectRelId = (String) mProgramProjectInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].id");
                String strCurrentGoverningPP = (String) mProgramProjectInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                // TIGTK-17752 : 31/10/2018
                if (UIUtil.isNotNullAndNotEmpty(strGoverningProjectRelId))
                    DomainRelationship.disconnect(context, strGoverningProjectRelId);

                DomainRelationship.connect(context, strPPId, TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT, strPartId, true);

                StringList stlObjectSelects = new StringList();
                stlObjectSelects.addElement(SELECT_ID);

                StringList stlRelSelects = new StringList();
                stlRelSelects.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                MapList mlImpactedProjects = domPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT, TigerConstants.TYPE_PSS_PROGRAMPROJECT, stlObjectSelects,
                        stlRelSelects, true, false, (short) 1, null, null, 0);

                Iterator itr = mlImpactedProjects.iterator();
                StringList slImpactedProjectList = new StringList();
                if (!mlImpactedProjects.isEmpty()) {
                    while (itr.hasNext()) {
                        Map tempMap = (Map) itr.next();
                        String strProjectId = (String) tempMap.get(SELECT_ID);
                        slImpactedProjectList.addElement(strProjectId);

                        String strRelID = (String) tempMap.get(DomainRelationship.SELECT_RELATIONSHIP_ID);
                        hmImpactedVsRelID.put(strProjectId, strRelID);
                    }
                }
                // TIGTK-17752 : 31/10/2018
                if (UIUtil.isNotNullAndNotEmpty(strCurrentGoverningPP) && !slImpactedProjectList.contains(strCurrentGoverningPP)) {
                    DomainObject domGoverningProject = DomainObject.newInstance(context, strCurrentGoverningPP);
                    DomainRelationship.connect(context, domGoverningProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT, domPart);
                }

                if (slImpactedProjectList.contains(strPPId)) {
                    if (hmImpactedVsRelID.containsKey(strPPId)) {
                        String RelId = (String) hmImpactedVsRelID.get(strPPId);
                        DomainRelationship.disconnect(context, RelId);
                    }
                }
                // TIGTK-17752 : 31/10/2018 : Start
            }
            // TIGTK-17752 : 31/10/2018 : END
            logger.debug("PSS_enoECMChangeUtil : getTransferOwnership : END");
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeUtil : getTransferOwnership : ", e);
            throw e;
        }
    }

    /**
     * This method is used to List the Program-Project in selected Collaborative Space for transfer Ownership
     * @param context
     * @param args
     * @throws Exception
     *             TIGKT-12753
     */
    public String getListOfProgramProject(Context context, String args[]) throws Exception {
        logger.debug("PSS_enoECMChangeUtil : getListOfProgramProject : START");
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strNewOwner = (String) paramMap.get("NewOwner");
            String strCollaborativeSpace = (String) paramMap.get("CollaborativeSpace");

            StringBuffer sbProgramProjects = new StringBuffer();

            String strwhere = "current!= " + TigerConstants.STATE_ACTIVE + " && current!= " + TigerConstants.STATE_OBSOLETE + " && current!= \"" + TigerConstants.STATE_NONAWARDED
                    + "\" && project == \"" + strCollaborativeSpace + "\" && from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]==False";

            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);
            objSelect.add(DomainConstants.SELECT_NAME);
            // find program project
            MapList mlProgramProjectObjectsList = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PROGRAMPROJECT, TigerConstants.VAULT_ESERVICEPRODUCTION, strwhere, objSelect);
            int iSize = mlProgramProjectObjectsList.size();
            for (int i = 0; i < iSize; i++) {
                HashMap<?, ?> mapProgramProjectObject = (HashMap<?, ?>) mlProgramProjectObjectsList.get(i);
                DomainObject domProgProj = DomainObject.newInstance(context, (String) mapProgramProjectObject.get(DomainConstants.SELECT_ID));
                // get list of members connected to program project
                StringList strProgramProjectMembers = domProgProj.getInfoList(context, "from[PSS_ConnectedMembers].to.name");

                if (strProgramProjectMembers.contains(strNewOwner)) {
                    sbProgramProjects.append(mapProgramProjectObject.get(DomainConstants.SELECT_NAME));
                    sbProgramProjects.append("@");
                }
            }
            logger.debug("PSS_enoECMChangeUtil : getListOfProgramProject : END");
            return sbProgramProjects.toString();

        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeUtil : getListOfProgramProject : ", e);
            throw e;
        }
    }

    /**
     * This method is used to remove duplicate Entries
     * @param context
     * @param args
     * @throws Exception
     *             TIGKT-12753
     */
    private static StringList removeDuplicates(StringList list) throws Exception {
        logger.debug("PSS_enoECMChangeUtil : removeDuplicates : START");
        try {
            HashSet<String> hashSet = new HashSet<String>();
            for (int i = 0; i < list.size(); i++) {
                hashSet.add((String) list.get(i));
            }
            list.clear();
            list.addAll(hashSet);
            logger.debug("PSS_enoECMChangeUtil : removeDuplicates : END");
            return list;
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeUtil : removeDuplicates : ", e);
            throw e;
        }

    }

    /**
     * To reload the program-Project on change of Person with selected Collab Space TIGTK-12753
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public HashMap<String, StringList> reloadProgramProject(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeUtil : reloadProgramProject : START");
        try {
            HashMap<String, StringList> programProjectMap = new HashMap<String, StringList>();
            HashMap<?, ?> fieldMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> fieldValue = (HashMap<?, ?>) fieldMap.get("fieldValues");
            String strUserName = ((String) fieldValue.get("Name")).trim();
            String strProject = ((String) fieldValue.get("Project")).trim();

            Map mParam = new HashMap();
            mParam.put("NewOwner", strUserName);
            mParam.put("CollaborativeSpace", strProject);

            String result = getListOfProgramProject(context, JPO.packArgs(mParam));
            StringList slProgramProjectList = FrameworkUtil.split(result, "@");
            // to remove the duplicate entries from the list

            slProgramProjectList = removeDuplicates(slProgramProjectList);
            programProjectMap.put("RangeValues", slProgramProjectList);
            logger.debug("PSS_enoECMChangeUtil : reloadProgramProject : END");
            return programProjectMap;

        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeUtil : reloadProgramProject : ", e);
            throw e;
        }
    }

    /**
     * To get the Range of Program-project for the context user with default Collab Space TIGTK-12753
     * @param context
     * @param args
     * @return Object
     * @throws Exception
     */
    public Object getProgramProjectRangeValues(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeUtil : getProgramProjectRangeValues : START");
        try {
            HashMap tempMap = new HashMap();
            // HashMap programMap = (HashMap) JPO.unpackArgs(args);
            // HashMap paramMap = (HashMap) programMap.get("paramMap");
            // String strObjectId = (String) paramMap.get("objectId");
            String strEmptyString = DomainConstants.EMPTY_STRING;
            // DomainObject domObj = DomainObject.newInstance(context, strObjectId);
            // START :: TIGTK-17414 :: ALM-6016 :: PSI
            String strOwner = context.getUser();
            // END :: TIGTK-17414 :: ALM-6016 :: PSI

            StringList fieldRangeValues = new StringList();
            StringList fieldDisplayRangeValues = new StringList();

            // to get the default Project
            String defualtProj = PersonUtil.getDefaultProject(context, context.getUser());

            Map mParam = new HashMap();
            mParam.put("NewOwner", strOwner);
            mParam.put("CollaborativeSpace", defualtProj);

            String result = getListOfProgramProject(context, JPO.packArgs(mParam));
            StringList slProgramProjectList = FrameworkUtil.split(result, "@");
            if (!slProgramProjectList.isEmpty()) {
                for (int j = 0; j < slProgramProjectList.size(); j++) {
                    if (UIUtil.isNotNullAndNotEmpty((String) slProgramProjectList.get(j))) {
                        fieldRangeValues.add(slProgramProjectList.get(j));
                        fieldDisplayRangeValues.add(slProgramProjectList.get(j));
                    }
                }
            } else {
                fieldRangeValues.add(strEmptyString);
                fieldDisplayRangeValues.add(strEmptyString);
            }
            tempMap.put("field_choices", fieldRangeValues);
            tempMap.put("field_display_choices", fieldDisplayRangeValues);
            logger.debug("PSS_enoECMChangeUtil : getProgramProjectRangeValues : END");
            return tempMap;
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeUtil : getProgramProjectRangeValues : ", e);
            throw e;
        }

    }

    /**
     * This method is used to reload Organization on change of Person
     * @param context
     * @param args
     * @throws Exception
     *             TIGKT-12753
     */

    // public String reloadOrganizations(Context context, String[] args) throws Exception {
    public String reloadOrganizations(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeUtil : reloadOrganizations : START");
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String userName = (String) paramMap.get("NewOwner");
            userName = userName.trim();
            // to get the Default Organization
            String defaultOrg = PersonUtil.getDefaultOrganization(context, userName);
            logger.debug("PSS_enoECMChangeUtil : reloadOrganizations : end");

            return defaultOrg;
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeUtil : reloadOrganizations : ", e);
            throw e;
        }

    }

    /**
     * This method is used to get Organization for transfer Ownership
     * @param context
     * @param args
     * @throws Exception
     *             TIGKT-12753
     */
    public String getOrgValue(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeUtil : getOrgValue : START");
        try {
            // to get the Default Organization
            String defaultOrg = PersonUtil.getDefaultOrganization(context, context.getUser());
            logger.debug("PSS_enoECMChangeUtil : getOrgValue : END");
            return defaultOrg;

        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeUtil : getOrgValue : ", e);
            throw e;
        }

    }

    /**
     * This method is used to display the default Organization / Project on top of the list
     * @param Organization
     * @param defaultOrg
     * @param resultList
     */
    public StringList displayDefaultValueOnTop(String defaultValue, StringList resultList) throws Exception {
        try {
            logger.debug("PSS_enoECMChangeUtil : displayDefaultValueOnTop : START");
            if (resultList.size() > 0) {
                // to remove the duplicate entries from the list
                resultList = removeDuplicates(resultList);
                for (int i = 0; i < resultList.size(); i++) {
                    if (resultList.get(i).equals(defaultValue)) {
                        resultList.remove(i);
                        resultList.add(0, defaultValue);
                        break;
                    }
                }
                return resultList;
            }
            return resultList;
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeUtil : displayDefaultValueOnTop : ", e);
            throw e;
        }
    }

    /**
     * This method is used for Transfer Affected Items From CR To CO depending on state
     * @author PTE
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList checkStateOfCADForTransferAffectedItemsFromCRToCO(Context context, StringList slPartType, StringList slStandardType, StringList slCADType, Map mapAICurrentState)
            throws Exception {
        try {
            StringList slValidCADId = new StringList();
            Iterator itrCADType = slCADType.iterator();
            while (itrCADType.hasNext()) {

                String strCADId = (String) itrCADType.next();
                DomainObject domCADObj = DomainObject.newInstance(context, strCADId);
                String strCADState = domCADObj.getInfo(context, DomainConstants.SELECT_CURRENT);

                StringList lstselectStmts = new StringList(3);
                lstselectStmts.addElement(DomainConstants.SELECT_ID);
                lstselectStmts.addElement(DomainConstants.SELECT_CURRENT);

                Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);

                MapList mlPArtObject = domCADObj.getRelatedObjects(context, relPattern.getPattern(), "*", lstselectStmts, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, "", null, 0);
                boolean bIsCADConnectedToCRPart = false;
                Iterator itr = mlPArtObject.iterator();
                while (itr.hasNext()) {
                    Map map = (Map) itr.next();
                    String strPartID = (String) map.get(DomainConstants.SELECT_ID);
                    if (slPartType.contains(strPartID) || slStandardType.contains(strPartID)) {
                        bIsCADConnectedToCRPart = true;
                        String strPartState = (String) mapAICurrentState.get(strPartID);
                        String strMappedCADState = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentral", context.getLocale(),
                                "PSS_emxEngineeringCentral.PartToCADStateMapping." + strPartState);
                        if (strCADState.equals(strMappedCADState.trim())) {
                            slValidCADId.add(strCADId);
                            break;

                        }
                    }
                }
                if (bIsCADConnectedToCRPart == false) {
                    slValidCADId.add(strCADId);
                }
            }
            return slValidCADId;
        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeUtil : checkStateOfCADForTransferAffectedItemsFromCRToCO() : ", ex);
            throw ex;
        }
    }

    /**
     * This method will set Due Date On Route Inbox Task date will be Planned End Date of Change Action
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public void setDueDateOnRouteInboxTaskForCA(Context context, String[] args) throws Exception {

        String strObjectID = args[0];// Get the Object Id
        DomainObject domObjectID = new DomainObject(strObjectID);
        if (domObjectID.isKindOf(context, ChangeConstants.TYPE_CHANGE_ACTION)) {
            String strPlannedEndDate = args[1];// Get the New Value for Planned End Date
            try {

                StringList objectSelects = new StringList(1);
                objectSelects.addElement(DomainObject.SELECT_ID);

                Pattern relPattern = new Pattern(DomainObject.RELATIONSHIP_OBJECT_ROUTE);
                relPattern.addPattern(DomainObject.RELATIONSHIP_ROUTE_TASK);

                Pattern typePattern = new Pattern(DomainObject.TYPE_ROUTE);
                typePattern.addPattern(DomainObject.TYPE_INBOX_TASK);

                Pattern typePostPattern = new Pattern(TigerConstants.TYPE_INBOX_TASK);

                MapList mpListInboxTasks = domObjectID.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                        typePattern.getPattern(), // object pattern
                        objectSelects, // object selects
                        DomainConstants.EMPTY_STRINGLIST, // relationship selects
                        true, // to direction
                        true, // from direction
                        (short) 2, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 0, // pageSize
                        typePostPattern, null, null, null, null);
                if (mpListInboxTasks.size() > 0) {
                    Iterator itrInboxTask = mpListInboxTasks.iterator();
                    while (itrInboxTask.hasNext()) {
                        Map mpInboxTask = (Map) itrInboxTask.next();
                        String strInboxTaskbjId = (String) mpInboxTask.get(DomainObject.SELECT_ID);
                        DomainObject domITTask = new DomainObject(strInboxTaskbjId);
                        domITTask.setAttributeValue(context, ATTRIBUTE_SCHEDULED_COMPLETION_DATE, strPlannedEndDate);
                    }
                }

            } catch (Exception e) {
                logger.error("Error in SetDueDateOnRouteInboxTaskForCA: ", e);
            }
        }
    }

    /**
     * Change Assessment window to display what items are already connected CO or not UE :: TIGTK-17749 :: ALM-5809
     * @author PSI
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getIsItemsConnectedToChange(Context context, String[] args) throws Exception {
        Vector<String> vConnectedToChangeColumns = new Vector<String>();
        try {
            HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
            MapList mlAssessmentObjects = (MapList) hmParamMap.get("objectList");
            String strChangeOID = (String) ((HashMap) hmParamMap.get("paramList")).get("contextCOId");

            if (!mlAssessmentObjects.isEmpty() && UIUtil.isNotNullAndNotEmpty(strChangeOID)) {
                MapList mlConnectedAffectedItems = null;
                DomainObject doChange = DomainObject.newInstance(context, strChangeOID);
                if (doChange.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEORDER)) {
                    ChangeOrder changeOrder = new ChangeOrder(strChangeOID);
                    mlConnectedAffectedItems = changeOrder.getAffectedItems(context);
                } else {
                    StringList slObjSelectables = new StringList(1);
                    slObjSelectables.addElement(DomainConstants.SELECT_ID);
                    mlConnectedAffectedItems = doChange.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, DomainConstants.QUERY_WILDCARD, slObjSelectables, null, false, true,
                            (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0);
                }

                StringList slConnectedAffectedItemsOID = new StringList(mlConnectedAffectedItems.size());
                if (!mlConnectedAffectedItems.isEmpty()) {
                    for (Object object : mlConnectedAffectedItems) {
                        slConnectedAffectedItemsOID.add((String) ((Map) object).get(DomainConstants.SELECT_ID));
                    }
                }

                for (Object object : mlAssessmentObjects) {
                    String strAssessmentItemOID = (String) ((Map) object).get(DomainConstants.SELECT_ID);
                    if (UIUtil.isNotNullAndNotEmpty(strAssessmentItemOID) && slConnectedAffectedItemsOID.contains(strAssessmentItemOID)) {
                        StringBuilder sbHtml = new StringBuilder();
                        sbHtml.append("<div style=\"float:center\" align=\"center\">");
                        sbHtml.append("<img src='../common/images/iconActionApprove.png' border='0' title='Part already connected to the Change' />");
                        sbHtml.append("</div>");
                        vConnectedToChangeColumns.add(sbHtml.toString());
                    } else {
                        vConnectedToChangeColumns.add(DomainConstants.EMPTY_STRING);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return vConnectedToChangeColumns;
    }
}