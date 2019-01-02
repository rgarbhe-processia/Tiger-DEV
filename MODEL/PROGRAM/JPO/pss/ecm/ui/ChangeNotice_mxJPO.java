package pss.ecm.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.DomainSymbolicConstants;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.ui.UIUtil;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class ChangeNotice_mxJPO {

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChangeNotice_mxJPO.class);

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END

    // TGPSS_PCM_TS_157_Change NoticeNotifications_V2.2 | 14/03/2017 | Harika Varanasi : Starts
    public LinkedHashMap<String, String> lhmMCNSelectionStore = new LinkedHashMap<String, String>();

    // TGPSS_PCM_TS_157_Change NoticeNotifications_V2.2 | 14/03/2017 | Harika Varanasi : Ends
    public LinkedHashMap<String, String> lhmMCNRejectSelectionStore = new LinkedHashMap<String, String>();

    public LinkedHashMap<String, String> lhmMCNCommentsSelectionStore = new LinkedHashMap<String, String>();

    /**
     * TS: 100 This Method is used to create the Change Notice with Object and Name Generator It will create route in between state In Review and In transfer Modified by PCM : 09/01/2017 : AB
     * @param context
     * @param args
     * @throws Exception
     *             Owner : Vishal B
     */

    public Map createChangeNotice(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            // TIGTK-8883 :Rutuja Ekatpure:5/7/2017:start
            HashMap requestMap = (HashMap) programMap.get("RequestValuesMap");
            Locale lLocale = (Locale) requestMap.get("localeObject");
            // TIGTK-8883 :Rutuja Ekatpure:5/7/2017:End
            HashMap<String, String> attributeMap = new HashMap<String, String>();
            // Fix for FindBugs issue Method call on instantiation : Harika Varanasi : 21 March 2017 : Start
            DomainObject domCN;
            // Fix for FindBugs issue Method call on instantiation : Harika Varanasi : 21 March 2017 : End
            String YES = "Yes";
            String NO = "No";
            String CNObjectId = DomainConstants.EMPTY_STRING;
            String MCOobjectId = (String) programMap.get("parentOID");
            // TIGTK-9709| 08/09/17 : Start
            DomainObject domMCO = new DomainObject(MCOobjectId);
            String strType = domMCO.getInfo(context, DomainConstants.SELECT_TYPE);

            if (!strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {

                MCOobjectId = (String) programMap.get("objectId");
                domMCO.setId(MCOobjectId);
            }
            // TIGTK-9709| 08/09/17 : End
            // TIGTK-8883 :Rutuja Ekatpure:5/7/2017:start
            String strActualEffctDate = (String) programMap.get("Effectivity Date");
            strActualEffctDate = getDateInMatrixDateFormat(strActualEffctDate, lLocale);
            attributeMap.put(TigerConstants.ATTRIBUTE_PSS_ACTUALEFFECTIVITYDATE, strActualEffctDate);

            // TIGTK-8883 :Rutuja Ekatpure:5/7/2017:End
            String ActualName = "";

            // Fix for FindBugs issue Method call on instantiation : Harika Varanasi : 21 March 2017 : Start
            com.matrixone.apps.common.Person loginPerson;// = (com.matrixone.apps.common.Person) DomainObject.newInstance(context, DomainConstants.TYPE_PERSON);
            // Fix for FindBugs issue Method call on instantiation : Harika Varanasi : 21 March 2017 : End
            loginPerson = com.matrixone.apps.common.Person.getPerson(context);
            String strLoginPersonID = (String) loginPerson.getObjectId();
            DomainObject domPerson = new DomainObject(strLoginPersonID);
            String strPerName = domPerson.getInfo(context, DomainConstants.SELECT_NAME);
            String strBGName = PersonUtil.getDefaultOrganization(context, strPerName);

            if ("FAS".equalsIgnoreCase(strBGName)) {
                ActualName = "AS";
            } else if ("FIS".equalsIgnoreCase(strBGName)) {
                ActualName = "IS";
            } else if ("FAE".equalsIgnoreCase(strBGName)) {
                ActualName = "MS";
            } else if ("FECT".equalsIgnoreCase(strBGName)) {
                ActualName = "ES";
            }

            String PlantCode = domMCO.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");
            String sapTransfer = domMCO.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED);

            if (UIUtil.isNotNullAndNotEmpty(PlantCode)) {
                PlantCode = getLength(PlantCode);
            }
            // RFC-033-AB-IT12
            String strOwner = context.getUser();
            String strVault = context.getVault().toString();

            try {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                String strCNname = FrameworkUtil.autoName(context, "type_PSS_ChangeNotice", null, "policy_PSS_ChangeNotice", null, null, true, true);
                domCN = DomainObject.newInstance(context, TigerConstants.TYPE_PSS_CHANGENOTICE);
                strCNname = ActualName.concat(PlantCode.concat(strCNname));
                domCN.createObject(context, TigerConstants.TYPE_PSS_CHANGENOTICE, strCNname, "-", TigerConstants.POLICY_PSS_CHANGENOTICE, strVault);
                domCN.setOwner(context, strOwner);
                // PCM : TIGTK-5157 : 08/03/2017 : AB : START
                attributeMap.put(DomainConstants.ATTRIBUTE_ORIGINATOR, strOwner);
                // PCM : TIGTK-5157 : 08/03/2017 : AB : END
                domCN.setAttributeValues(context, attributeMap);
                // PCM : TIGTK-5267 : 10/04/2017 : AB : START
                if (sapTransfer.equalsIgnoreCase(NO)) {
                    domMCO.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TRANSFER_TO_SAP_EXPECTED, YES);
                }
                // PCM : TIGTK-5267 : 10/04/2017 : AB : END
            } finally {
                ContextUtil.popContext(context);
            }

            // RFC-033-AB-IT12
            CNObjectId = domCN.getObjectId();
            String[] CNObjectArray = new String[] { CNObjectId };
            RelationshipType relType = new RelationshipType(TigerConstants.RELATIONSHIP_PSS_RELATEDCN);
            domMCO.addRelatedObjects(context, relType, true, CNObjectArray);

            String ProjectId = domMCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            if (UIUtil.isNotNullAndNotEmpty(ProjectId)) {
                String RouteID = getRouteConnected(context, ProjectId, CNObjectId);

            }

            returnMap.put("newObjectId", CNObjectId);
            returnMap.put("id", CNObjectId);

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in createChangeNotice: ", e);
            throw e;
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return returnMap;
    }

    /**
     * TS: 100 This is Intermediate to calculate the length of String
     * @param context
     * @param args
     * @throws Exception
     *             Owner : Vishal B
     */
    public String getLength(String getString) {
        int length = getString.length();
        if (length > 4) {
            getString = getString.substring(0, 4);
        }

        return getString;
    }

    /**
     * TS: 100 This is Intermediate method will create route in between state In Review and In transfer Modified for PCM TIGTK-3831 : 21/12/2016 : AB
     * @param context
     * @param args
     * @throws Exception
     *             Owner : Vishal B
     */

    public String getRouteConnected(Context context, String ProjectId, String CNObjectId) {
        String strRouteId = "";

        try {

            DomainObject domProject = new DomainObject(ProjectId);
            String DEFAULT_CN_REVIEWER = "Default CN Reviewer on CN";
            StringList busSelect = new StringList();
            busSelect.add(
                    "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='" + DEFAULT_CN_REVIEWER + "'].to.id");
            Map mapRouteTemplateDetails = domProject.getInfo(context, busSelect);
            String strRouteTemplateId = (String) mapRouteTemplateDetails.get("from[PSS_ConnectedRouteTemplates].to.id");
            if (UIUtil.isNotNullAndNotEmpty(strRouteTemplateId)) {
                DomainObject RouteTemplate = new DomainObject(strRouteTemplateId);
                String description = "";
                String strRouteBasePurpose = "Route Base Purpose";
                description = RouteTemplate.getInfo(context, "description");
                String ROUTE_BASE_PURPOSE_APPROVAL = RouteTemplate.getAttributeValue(context, strRouteBasePurpose);

                final String ROUTE_REVISION = "1";
                String strName = DomainObject.getAutoGeneratedName(context, DomainSymbolicConstants.SYMBOLIC_type_Route, "");

                Route newRoute = new Route();
                newRoute.createObject(context, DomainConstants.TYPE_ROUTE, strName, ROUTE_REVISION, DomainConstants.POLICY_ROUTE, context.getVault().getName());
                strRouteId = newRoute.getInfo(context, DomainConstants.SELECT_ID);
                newRoute.setDescription(context, description);
                // PCM : TIGTK-4352 : 09/02/2017 : AB : START
                // Set Attribute value of Route
                Map mpRelNewAttributeMap = new Hashtable();
                mpRelNewAttributeMap.put("Route Completion Action", "Promote Connected Object");
                mpRelNewAttributeMap.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE, ROUTE_BASE_PURPOSE_APPROVAL);

                newRoute.setAttributeValues(context, mpRelNewAttributeMap);
                // PCM : TIGTK-4352 : 09/02/2017 : AB : END
                DomainRelationship dRel = newRoute.addRelatedObject(context, new RelationshipType(DomainConstants.RELATIONSHIP_OBJECT_ROUTE), true, CNObjectId);

                // Add Attribute to Relationship
                Map<String, String> mapAttribute = new HashMap<String, String>();
                mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_POLICY, "policy_PSS_ChangeNotice");
                mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE, "state_InReview");
                mapAttribute.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE, ROUTE_BASE_PURPOSE_APPROVAL);
                dRel.setAttributeValues(context, mapAttribute);
                newRoute.connectTemplate(context, strRouteTemplateId);
                newRoute.addMembersFromTemplate(context, strRouteTemplateId);

            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getRouteConnected: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return strRouteId;

    }

    /**
     * TS: 100 This method will show created Change notice in the table
     * @param context
     * @param args
     * @throws Exception
     *             Owner : Vishal B
     */

    public MapList getRelatedCN(Context context, String args[]) throws Exception {
        MapList mlCNList = new MapList();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String MCOobjectId = (String) programMap.get("objectId");

            DomainObject domMCO = new DomainObject(MCOobjectId);

            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            mlCNList = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATEDCN, // relationship
                    // pattern
                    TigerConstants.TYPE_PSS_CHANGENOTICE, // object pattern
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

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getRelatedCN: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return mlCNList;

    }

    /**
     * TS: 113 This method is used to promote Change Notice To In Transfer state if Route is not present . Oderwise promote the Change Notice to In Review state and start the Approval Route Modified
     * for PCM TIGTK-3831 : 21/12/2016 & 04/01/2017 : AB
     * @param context
     * @param args
     * @throws Exception
     *             Owner : Vishal B and Priyanka S
     */
    public int promoteChangeNoticeToInTransfer(Context context, String[] args) throws Exception {

        final String STATE_STARTED = "Started";
        // Relationship between Route and Task

        String ObjectId = args[0];
        // String IN_TRANSFER = PropertyUtil.getSchemaProperty(context, "state_InTransfer");
        final String ROUTE_BASE_STATE = "Route Base State";
        final String IN_REVIEW = "state_InReview";
        // MapList mlRouteList = new MapList();

        DomainObject domCN = new DomainObject(ObjectId);

        StringList slSelectRelStmts = new StringList(1);
        slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

        StringList slObjSelectStmts = new StringList(1);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

        MapList mlRouteList = domCN.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, // relationship
                // pattern
                DomainConstants.TYPE_ROUTE, // object pattern
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

        try {

            if (!mlRouteList.isEmpty()) {
                Iterator itr = mlRouteList.iterator();
                while (itr.hasNext()) {
                    Map mMBOMObjMap = (Map) itr.next();
                    String strRelId = (String) mMBOMObjMap.get(DomainRelationship.SELECT_ID);
                    String strRouteId = (String) mMBOMObjMap.get("id");
                    DomainRelationship domrel = new DomainRelationship(strRelId);
                    String strState = domrel.getAttributeValue(context, ROUTE_BASE_STATE);
                    if (!(strState.equals(IN_REVIEW))) {

                        // RFC-033-AB-IT12
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        domCN.setState(context, "In Transfer");
                        ContextUtil.popContext(context);
                        // RFC-033-AB-IT12

                    }
                    // Code to promote Change Notice from Prepare to In Review state and Start the Approval Route
                    else {
                        DomainObject domRoute = new DomainObject(strRouteId);
                        StringList slNewRouteNodeRelId = domRoute.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_ROUTE_NODE + "].id");

                        if (slNewRouteNodeRelId != null && !slNewRouteNodeRelId.isEmpty()) {
                            for (int i = 0; i < slNewRouteNodeRelId.size(); i++) {

                                String strRouteNodeRelId = (String) slNewRouteNodeRelId.get(i);
                                DomainRelationship domRelRouteNode = DomainRelationship.newInstance(context, strRouteNodeRelId);

                                // setting attribute (scheduled completion date) values for that relationship.
                                String strRouteDueDateTime = this.getRouteDueDate(context, args);

                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                domRelRouteNode.setAttributeValue(context, DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE, strRouteDueDateTime);
                                ContextUtil.popContext(context);
                            }
                        }
                        // TIGTK-6243 :Rutuja Ekatpure:10/4/2017:Start
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        domRoute.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, STATE_STARTED);
                        domRoute.setState(context, "In Process");
                        ContextUtil.popContext(context);
                        // TIGTK-6243 :Rutuja Ekatpure:10/4/2017:End

                    }
                }
            } else {
                // RFC-033-AB-IT12
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                domCN.setState(context, "In Transfer");
                ContextUtil.popContext(context);
                // RFC-033-AB-IT12
            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteChangeNoticeToInTransfer: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return 0;
    } // End of method promoteChangeNoticeToInTransfer

    /**
     * This method is called on postProcess functionality of Cancel CN.
     * @param context
     * @param args
     * @return void - Nothing
     * @throws Exception
     * @author -- Pooja Mantri -- TS105 -- Cancel the CN
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public void cancelChangeNotice(Context context, String[] args) throws Exception {

        Locale strLocale = context.getLocale();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String objectID = (String) requestMap.get("objectId");
            String strComments = (String) requestMap.get("Comments");
            final String CANCELLED = "Cancelled";
            DomainObject domCN = DomainObject.newInstance(context, objectID);

            StringList slCNSelecable = new StringList();
            slCNSelecable.add("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.current");
            slCNSelecable.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_SAP_RESPONSE + "]");
            slCNSelecable.add(DomainObject.SELECT_CURRENT);

            Map mCNInfo = domCN.getInfo(context, slCNSelecable);
            String strMCOState = (String) mCNInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.current");

            if (UIUtil.isNullOrEmpty(strMCOState)) {
                String strNoMCOsMsg = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", strLocale, "PSS_EnterpriseChangeMgt.Alert.NoMCOs");
                MqlUtil.mqlCommand(context, "notice $1", strNoMCOsMsg);
            } else if (UIUtil.isNotNullAndNotEmpty(strMCOState) && strMCOState.equals(TigerConstants.STATE_PSS_MCO_CANCELLED)) {

                String strNoMCOsStateMsg = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", strLocale, "PSS_EnterpriseChangeMgt.Alert.MCOInCancelledState");
                MqlUtil.mqlCommand(context, "notice $1", strNoMCOsStateMsg);
            } else {
                // RFC-033-AB-IT12
                // PCM TIGTK-3380 | 25/11/16 : Gautami : Start
                Map<String, String> mAttributesOnCN = new HashMap();
                mAttributesOnCN.put(TigerConstants.ATTRIBUTE_PSS_CNREASONFORCANCELLATION, strComments);
                mAttributesOnCN.put(TigerConstants.ATTRIBUTE_PSS_SAP_RESPONSE, CANCELLED);
                domCN.setAttributeValues(context, mAttributesOnCN);
                domCN.promote(context);

                // TIGTK-11455 :START
                Map payload = new HashMap();
                payload.put("RejectedTask_Comment", strComments);
                // TIGTK-11455 :END
                // TGPSS_PCM-TS157 Change Notice Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Starts
                Map objMap = new HashMap();
                objMap.put("objectId", objectID);
                objMap.put("notificationName", "PSS_CNRejectNotification");
                objMap.put("payload", payload);

                JPO.invoke(context, "emxNotificationUtil", null, "objectNotificationFromMap", JPO.packArgs(objMap));
                // TGPSS_PCM-TS157 Change Notice Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Ends

                // RFC-033-AB-IT12
                // PCM TIGTK-3380 | 25/11/16 : Gautami : End
            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in cancelChangeNotice: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }

    /*
     * TS101 - This method is to get all the associated CreateAssemblies connected to Change Notice
     */
    public MapList getItemsToTransferMfgParts(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList mlRelatedParts = new MapList();

        try {
            if (null != programMap) {
                String strChannelNoticeId = (String) programMap.get("objectId");

                DomainObject domChannelNotice = new DomainObject(strChannelNoticeId);
                StringList objSelects = new StringList();
                objSelects.addElement(DomainConstants.SELECT_ID);
                objSelects.addElement(DomainConstants.SELECT_TYPE);
                objSelects.addElement(DomainConstants.SELECT_NAME);
                objSelects.addElement(DomainConstants.SELECT_REVISION);
                objSelects.addElement(DomainConstants.SELECT_OWNER);
                objSelects.addElement(DomainConstants.SELECT_POLICY);
                objSelects.addElement(DomainConstants.SELECT_CURRENT);

                mlRelatedParts = domChannelNotice.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CNAFFECTEDITEMS, // relationship
                        TigerConstants.TYPE_CREATEASSEMBLY, // object pattern
                        objSelects, // object selects
                        new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, 0);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getItemsToTransferMfgParts: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return mlRelatedParts;
    }

    /*
     * TS104 - This method is called from PSS_CNMBOMTools command to get list of Tools connected with Context Change Notice
     */
    public MapList getCNRelatedTools(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList mlRelatedTools = new MapList();

        try {
            if (null != programMap) {
                String strChannelNoticeId = (String) programMap.get("objectId");

                DomainObject domChannelNotice = new DomainObject(strChannelNoticeId);
                StringList objSelects = new StringList();
                objSelects.addElement(DomainConstants.SELECT_ID);
                objSelects.addElement(DomainConstants.SELECT_TYPE);
                objSelects.addElement(DomainConstants.SELECT_NAME);
                objSelects.addElement(DomainConstants.SELECT_REVISION);
                objSelects.addElement(DomainConstants.SELECT_OWNER);
                objSelects.addElement(DomainConstants.SELECT_POLICY);
                objSelects.addElement(DomainConstants.SELECT_CURRENT);

                String TYPE_VPM_REFERENCE = PropertyUtil.getSchemaProperty(context, "type_VPMReference");

                mlRelatedTools = domChannelNotice.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CNAFFECTEDITEMS, // relationship pattern
                        TYPE_VPM_REFERENCE, // object pattern
                        objSelects, // object selects
                        null, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, 0);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNRelatedTools: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return mlRelatedTools;
    }

    /*
     * TS101 - This method is called from PSS_CNAddMfgParts command to get list of Create Assemblies to be displayed on search Page
     */
    public StringList includeCreateAssembly(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String RELATIONSHIP_PSS_ALTERNATE_MBOM_REFERENCE = PropertyUtil.getSchemaProperty(context, "relationship_PSS_AlternateMBOMReference");

        Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
        Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_RELATEDCN);
        Pattern typePatternMfgChangeAction = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION);
        Pattern relPatternMfgChangeAction = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION);
        Pattern typePatternCreateAssembly = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
        Pattern relPatternMfgChangeAffectedItem = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM);
        Pattern relPatternAlternateMBOMReference = new Pattern(RELATIONSHIP_PSS_ALTERNATE_MBOM_REFERENCE);

        StringList slCreateAssemblyList = new StringList();

        try {
            String strChannelNoticeId = (String) programMap.get("parentOID");
            // MapList mlRelatedMCO = new MapList();
            DomainObject domChannelNotice = new DomainObject(strChannelNoticeId);
            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_ID);

            // MCO to Change Notice
            MapList mlRelatedMCO = domChannelNotice.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    objSelects, // object selects
                    null, // relationship selects
                    true, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, 0);
            int nRelatedMBOMCount = mlRelatedMCO.size();

            for (int i = 0; i < nRelatedMBOMCount; i++) {
                Map mapMCOObj = (Map) mlRelatedMCO.get(i);
                String strMBOMId = (String) mapMCOObj.get("id");
                DomainObject domMCO = new DomainObject(strMBOMId);
                // MCO to MCA
                MapList mlRelatedChangeAction = domMCO.getRelatedObjects(context, relPatternMfgChangeAction.getPattern(), // relationship pattern
                        typePatternMfgChangeAction.getPattern(), // object pattern
                        objSelects, // object selects
                        null, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, 0);
                int nRelatedChangeActionCount = mlRelatedChangeAction.size();

                for (int j = 0; j < nRelatedChangeActionCount; j++) {
                    Map mapMCAObj = (Map) mlRelatedChangeAction.get(j);
                    String strMCAId = (String) mapMCAObj.get("id");
                    DomainObject domMCA = new DomainObject(strMCAId);
                    // MapList mlRelatedCreateAssembly = new MapList();

                    // MCA to CreateAssembly
                    MapList mlRelatedCreateAssembly = domMCA.getRelatedObjects(context, relPatternMfgChangeAffectedItem.getPattern(), // relationship pattern
                            typePatternCreateAssembly.getPattern(), // object pattern
                            objSelects, // object selects
                            null, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, 0);
                    int nRelatedAssemblyCount = mlRelatedCreateAssembly.size();

                    for (int k = 0; k < nRelatedAssemblyCount; k++) {
                        Map mapCreateAssemblyObj = (Map) mlRelatedCreateAssembly.get(k);
                        String strCreateAssemblyId = (String) mapCreateAssemblyObj.get("id");
                        DomainObject domCreateAssembly = new DomainObject(strCreateAssemblyId);
                        // MapList mlRelatedCreateAssemblies = new MapList();

                        // CreateAssembly to CreateAssembly
                        MapList mlRelatedCreateAssemblies = domCreateAssembly.getRelatedObjects(context, relPatternAlternateMBOMReference.getPattern(), // relationship pattern
                                typePatternCreateAssembly.getPattern(), // object pattern
                                objSelects, // object selects
                                null, // relationship selects
                                false, // to direction
                                true, // from direction
                                (short) 1, // recursion level
                                null, // object where clause
                                null, 0);
                        int nRelatedCreateAssembliesCount = mlRelatedCreateAssemblies.size();

                        for (int l = 0; l < nRelatedCreateAssembliesCount; l++) {
                            Map mapCreateAssembly = (Map) mlRelatedCreateAssemblies.get(l);
                            String strRelatedCreateAssemblyId = (String) mapCreateAssembly.get("id");
                            slCreateAssemblyList.add(strRelatedCreateAssemblyId);
                        }
                        slCreateAssemblyList.add(strCreateAssemblyId);
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in includeCreateAssembly: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return slCreateAssemblyList;
    }

    /**
     * TS:117 This method for visibilty of Remove affected cn command
     * @param context
     * @param args
     * @throws Exception
     */
    public Boolean showCommandIfContextPersonIsProgProjMember(Context context, String args[]) throws Exception {
        boolean result = false;
        try {
            String currentUser = context.getUser();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCNobjectId = (String) programMap.get("objectId");

            DomainObject domCN = new DomainObject(strCNobjectId);
            String strMCOConnected = domCN.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.id");

            DomainObject domMCO = new DomainObject(strMCOConnected);
            String strProgramConnected = domMCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            DomainObject domProgProj = new DomainObject(strProgramConnected);
            StringList slMember = domProgProj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");

            String CURRENT_STATE = domCN.getInfo(context, DomainConstants.SELECT_CURRENT);

            if (slMember.contains(currentUser) && CURRENT_STATE.equals(TigerConstants.STATE_PREPARE_CN)) {
                result = true;

            } else {
                result = false;
            }

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in showCommandIfContextPersonIsProgProjMember: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return result;
    }// end of method

    /**
     * This is method will Demote the Change NOtice and evoke the SAP PLM Integration web service
     * @param context
     * @param args
     * @throws Exception
     *             Owner : Vishal B
     */

    public int demoteCNandEvokeTransferToSAP(Context context, String[] args) throws Exception {
        /*
         * Get the Object ID Demote it to In Transfer State Evoke SAP PLM Integration web service
         */

        return 0;

    }

    /**
     * This is method will promote the Change NOtice and as per SAP feedback
     * @param context
     * @param args
     * @throws Exception
     *             Owner : Vishal B
     */

    public void promoteCNUponSAPFeedback(Context context, String SAP_Response, String CNObjectId) throws Exception {

        final String FULLY_INTEGRATED = "Fully Integrated";
        final String NOT_FULLY_INTEGRATED = "Not Fully Integrated";
        final String TRANSFER_ERROR = "Transfer Error";
        DomainObject domCN = new DomainObject(CNObjectId);
        try {

            if (UIUtil.isNotNullAndNotEmpty(SAP_Response)) {

                if (SAP_Response.equalsIgnoreCase(FULLY_INTEGRATED)) {
                    domCN.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_SAP_RESPONSE, SAP_Response);
                    domCN.promote(context);
                }
                if (SAP_Response.equalsIgnoreCase(NOT_FULLY_INTEGRATED)) {
                    domCN.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_SAP_RESPONSE, SAP_Response);
                    domCN.promote(context);
                }
                if (SAP_Response.equalsIgnoreCase(TRANSFER_ERROR)) {
                    domCN.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_SAP_RESPONSE, SAP_Response);
                    domCN.promote(context);
                }
            }

        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: START
        catch (RuntimeException re) {
            throw re;
        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: END
        catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteCNUponSAPFeedback: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }

    /**
     * This is method will transfers the CN Affected Item to SAP
     * @param context
     * @param args
     * @throws Exception
     *             Owner : Vishal B
     */
    public void transferCNaffectedItemToSAP(Context context, String[] args) throws Exception {

        // SAP Related Content
        String CnObjectId = args[0];
        // DomainObject domCN = new DomainObject(CnObjectId);
        try {
            // call the web service and pass this StringList and Pass Response back to enovia

            String PSS_SAP_Response = "Transfer Error";
            promoteCNUponSAPFeedback(context, PSS_SAP_Response, CnObjectId);
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in transferCNaffectedItemToSAP: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
    }

    /**
     * This is method will Set Attribute Effectivity Date of CN to Affected Item
     * @param context
     * @param args
     * @throws Exception
     *             Owner : Vishal B
     */
    public void populatesEffectivityDate(Context context, String[] args) throws Exception {

        String ATTRIBUTE_PLMREFERENCE_V_APPLICABILITY_DATE = PropertyUtil.getSchemaProperty(context, "attribute_PLMReference.V_ApplicabilityDate");
        String TYPE_VPM_REFERENCE = PropertyUtil.getSchemaProperty(context, "type_VPMReference");
        String cnObjectId = args[0];
        DomainObject domCN = new DomainObject(cnObjectId);
        String cnEFdate = domCN.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_EFFECTIVITYDATE);
        Pattern typePattern = new Pattern(TYPE_VPM_REFERENCE);
        typePattern.addPattern(TigerConstants.TYPE_CREATEASSEMBLY);
        Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CNAFFECTEDITEMS);

        StringList objSelects = new StringList();
        objSelects.addElement(DomainConstants.SELECT_ID);

        MapList mlRelatedAffectedItem = domCN.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                typePattern.getPattern(), // object pattern
                objSelects, // object selects
                null, // relationship selects
                false, // to direction
                true, // from direction
                (short) 1, // recursion level
                null, // object where clause
                null, 0);

        try {
            if (!mlRelatedAffectedItem.isEmpty()) {
                Iterator iter = mlRelatedAffectedItem.iterator();
                while (iter.hasNext()) {
                    Map checkMap = (Map) iter.next();
                    String ObjectId = (String) checkMap.get("id");
                    DomainObject domAffectedItem = new DomainObject(ObjectId);
                    String Applicability_Date = domAffectedItem.getAttributeValue(context, ATTRIBUTE_PLMREFERENCE_V_APPLICABILITY_DATE);

                    if (UIUtil.isNullOrEmpty(Applicability_Date)) {
                        domAffectedItem.setAttributeValue(context, ATTRIBUTE_PLMREFERENCE_V_APPLICABILITY_DATE, cnEFdate);
                    }

                }

            }

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in populatesEffectivityDate: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }// end of method

    /**
     * This method is called on postProcess functionality of Sketch creation -- TS137 -- Attach Reference Documents to CN. It is used to connect "PSS Change Notice" object with "Sketch" Object.
     * @param context
     * @param args
     * @return -- void -- Nothing
     * @author -- Pooja Mantri
     * @throws Exception
     */
    public void connectToCN(Context context, String[] args) throws Exception {
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            HashMap paramMap = (HashMap) programMap.get("paramMap");

            String strPSSChangeNoticeId = (String) requestMap.get("parentOID");
            String strSketchObjId = (String) paramMap.get("newObjectId");

            // Creating Domain Object Instance for "PSS Change Notice" Object
            DomainObject domPSSChangeNoticeObj = DomainObject.newInstance(context, strPSSChangeNoticeId);

            // Creating Domain Object Instance for "Sketch" Object
            DomainObject domSketch = DomainObject.newInstance(context, strSketchObjId);

            // Get the Name for newly created Sketch Object
            String strSketchObjName = domSketch.getInfo(context, DomainConstants.SELECT_NAME);

            // Set Title for Sketch Object
            domSketch.setAttributeValue(context, DomainConstants.ATTRIBUTE_TITLE, strSketchObjName);

            // Connecting "PSS Change Notice" Object with "Sketch" Object
            if (UIUtil.isNotNullAndNotEmpty(strPSSChangeNoticeId)) {
                DomainRelationship.connect(context, domPSSChangeNoticeObj, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_SUPPORTINGDOCUMENT), domSketch);
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectToCN: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
    }

    /**
     * This method is for Send the Notification to the owner about promotion of state As per SAP feedback the Promotion of state is done
     * @param context
     * @param args
     * @return -- void -- Nothing
     * @author -- Vishal B
     * @throws Exception
     */

    public void sendCNPromoteNotification(Context context, String args[]) {

        String strLanguage = context.getSession().getLanguage();
        String strMsgTigerKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Tiger");
        String strMessage = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.promoted");
        String strState = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.state");

        String strType = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Policy.PSS_Change_Notice");
        // String strName = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.name");
        // String strCurrentState = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Currentstate");
        // String strNextState = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Nextstate");
        String strOwner = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Basic.Owner");

        try {
            String CNObjectId = args[0];
            // String CurrentState = args[1];
            String NextState = args[2];

            DomainObject domCN = new DomainObject(CNObjectId);
            String getCNCreator = domCN.getInfo(context, DomainConstants.SELECT_OWNER);
            String getCNname = domCN.getInfo(context, DomainConstants.SELECT_NAME);
            String getCNType = domCN.getInfo(context, DomainConstants.SELECT_TYPE);

            // Subject : CN {Name} has been promoted to {CURRENT} state;

            // "SubjectKey" StringBuffer
            StringBuffer subjectKey = new StringBuffer();
            subjectKey.append(strMsgTigerKey + " ");
            subjectKey.append(getCNType + " ");
            subjectKey.append(getCNname + " ");
            subjectKey.append(strMessage + " ");
            subjectKey.append(NextState + " ");
            subjectKey.append(strState + " ");

            // "strBufferMessage" StringBuffer
            StringBuffer strBufferMessage = new StringBuffer();

            strBufferMessage.append(strType + " ");
            strBufferMessage.append(getCNname + "\n");
            strBufferMessage.append(strMessage + " ");
            strBufferMessage.append(NextState + " ");
            strBufferMessage.append(strState + " \n");
            strBufferMessage.append(strOwner + ": " + getCNCreator);

            StringList toList = new StringList(getCNCreator);

            // Send Notification to Change Manager of CR for reject Task.
            MailUtil.sendNotification(context, toList, // toList
                    null, // ccList
                    null, // bccList
                    subjectKey.toString(), // subjectKey
                    null, // subjectKeys
                    null, // subjectValues
                    strBufferMessage.toString(), // messageKey
                    null, // messageKeys
                    null, // messageValues
                    null, // objectIdList
                    null); // companyName

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in sendCNPromoteNotification: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }

    /**
     * This Method is used on the Create Form of the Change Notice to display the MCO Related Program-Project
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getCNMCORelatedProject(Context context, String[] args) throws Exception {
        String strProgProjId = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strMCO = (String) requestMap.get("objectId");
            DomainObject domMCO = DomainObject.newInstance(context, strMCO);
            strProgProjId = domMCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNMCORelatedProject: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return strProgProjId;
    }

    /**
     * This Method is used on the Create Form of the Change Notice to display the MCO Related Plant
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */

    public String getCNMCORelatedPlantObject(Context context, String[] args) throws Exception {
        String strPlantName = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strMCO = (String) requestMap.get("objectId");
            DomainObject domMCO = DomainObject.newInstance(context, strMCO);
            strPlantName = domMCO.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNMCORelatedPlantObject: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return strPlantName;
    }

    /**
     * This Method is used on the Create Form of the Change Notice to display the MCO Related 150% MBOM
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getCNMCORelated150MBOM(Context context, String[] args) throws Exception {
        String str150MBOMVName = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strMCO = (String) requestMap.get("objectId");
            DomainObject domMCO = DomainObject.newInstance(context, strMCO);
            String str150MBOMId = domMCO.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM + "].to.id");
            // TIGTK-8076 - 5-23-2017 - PTE - START
            DomainObject domObj = new DomainObject(str150MBOMId);
            str150MBOMVName = domObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNMCORelated150MBOM: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return str150MBOMVName;
    }

    /**
     * This Method is used on the Item to Transfer Tab to fetch the contents Table
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */

    // TS-101: Changes after the Reviews
    // Program for Table on Item to Transfer Tab of CN
    public MapList getItemsToTransfer(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList mlRelatedParts = new MapList();

        try {
            if (null != programMap) {
                String strChannelNoticeId = (String) programMap.get("objectId");

                DomainObject domChannelNotice = new DomainObject(strChannelNoticeId);
                StringList objSelects = new StringList();
                objSelects.addElement(DomainConstants.SELECT_ID);
                objSelects.addElement(DomainConstants.SELECT_TYPE);
                objSelects.addElement(DomainConstants.SELECT_NAME);
                objSelects.addElement(DomainConstants.SELECT_REVISION);
                objSelects.addElement(DomainConstants.SELECT_OWNER);
                objSelects.addElement(DomainConstants.SELECT_POLICY);
                objSelects.addElement(DomainConstants.SELECT_CURRENT);

                String TYPE_VPM_REFERENCE = PropertyUtil.getSchemaProperty(context, "type_VPMReference");
                String TYPE_FPDMMBOMPART = PropertyUtil.getSchemaProperty(context, "type_FPDM_MBOMPart");

                Pattern typePattern = new Pattern(TYPE_VPM_REFERENCE);
                typePattern.addPattern(TYPE_FPDMMBOMPART);

                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CNAFFECTEDITEMS);

                mlRelatedParts = domChannelNotice.getRelatedObjects(context, relPattern.getPattern(), // relationship
                        typePattern.getPattern(), // object pattern
                        objSelects, // object selects
                        new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, 0);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getItemsToTransfer: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return mlRelatedParts;

    }

    /**
     * Include OID Program for Add Existing Parts on the Items to Transfer Tab of the Change Notice
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */

    // TS-101: Changes after the Reviews
    // Program to Include the 100% MBOM on the Add Existing for Parts command on the Item to Transfer Tab of CN
    public StringList getRelated100MBOM(Context context, String[] args) throws Exception {
        StringList sl100MBOM = new StringList();
        StringList slConnectedMBOM = new StringList();
        try {
            String TYPE_FPDMMBOMPART = PropertyUtil.getSchemaProperty(context, "type_FPDM_MBOMPart");
            String RELATIONSHIP_FPDMGENERATEDMBOM = PropertyUtil.getSchemaProperty(context, "relationship_FPDM_GeneratedMBOM");
            StringList objSelects = new StringList();
            objSelects.add(DomainConstants.SELECT_ID);
            StringList relSelects = new StringList();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCNID = (String) programMap.get("objectId");
            DomainObject domChangeNotice = new DomainObject(strCNID);
            String strMCOId = domChangeNotice.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.id");
            // TIGTK-7456 | 09/05/2017 | Vishal Bhosale : SteepGraph : START
            MapList mlConnectedAffectedItem = domChangeNotice.getRelatedObjects(context, TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS, // relationship
                    TYPE_FPDMMBOMPART, // object pattern
                    objSelects, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, 0);

            if (!mlConnectedAffectedItem.isEmpty()) {
                for (int i = 0; i < mlConnectedAffectedItem.size(); i++) {
                    Map mConnectedMBOMPart = (Map) mlConnectedAffectedItem.get(i);
                    String strMBOMPart = (String) mConnectedMBOMPart.get(DomainConstants.SELECT_ID);
                    slConnectedMBOM.add(strMBOMPart);
                }
            }
            // TIGTK-7456 | 09/05/2017 | Vishal Bhosale : SteepGraph : END

            if (!strMCOId.equalsIgnoreCase(null) && !strMCOId.equalsIgnoreCase("")) {
                DomainObject domMCO = new DomainObject(strMCOId);
                String strAssemblyId = domMCO.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM + "].to.id");
                if (strAssemblyId != null && !"".equalsIgnoreCase(strAssemblyId)) {
                    DomainObject domAssembly = new DomainObject(strAssemblyId);

                    // Alternate Way - Starts
                    /*
                     * String str100MBOMId = domAssembly.getInfo(context, "from[" + RELATIONSHIP_PSS_RELATED150MBOM + "].to.id"); if(str100MBOMId!=null && !"".equalsIgnoreCase(str100MBOMId)){
                     * sl100MBOM.add(str100MBOMId); }
                     */
                    // Alternate Way - Ends

                    // Ask CARDINALITY of Relationship RELATIONSHIP_FPDMGENERATEDMBOM is
                    MapList mlRelated100MBOM = domAssembly.getRelatedObjects(context, RELATIONSHIP_FPDMGENERATEDMBOM, // relationship
                            TYPE_FPDMMBOMPART, // object pattern
                            objSelects, // object selects
                            relSelects, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, 0);
                    for (int i = 0; i < mlRelated100MBOM.size(); i++) {
                        Map m100MBOM = (Map) mlRelated100MBOM.get(i);
                        String str100MBOMId = (String) m100MBOM.get(DomainConstants.SELECT_ID);
                        if (!slConnectedMBOM.contains(str100MBOMId))
                            sl100MBOM.add(str100MBOMId);
                    }

                }

            }
            // TIGTK-5813 | 31/03/2017 | Harika Varanasi : SteepGraph : Starts
            if (sl100MBOM.isEmpty()) {
                sl100MBOM.addElement(" ");
            }
            // TIGTK-5813 | 31/03/2017 | Harika Varanasi : SteepGraph : Ends
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getRelated100MBOM: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return sl100MBOM;
    }

    /**
     * Include OID Program for Add Existing Tools on the Items to Transfer Tab of the Change Notice
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */

    // TS-104: Changes after the Reviews
    // Program to Include the 100% MBOM on the Add Existing for Tools command on the Item to Transfer Tab of CN
    public StringList getRelatedTools(Context context, String[] args) throws Exception {
        StringList slTools = new StringList();
        PLMCoreModelerSession plmSession = null;
        // Fix for FindBugs issue Possible Null pointer : Harika Varanasi : 21 March 2017: START
        boolean bIsPLMSessionOpen = false;
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCNID = (String) programMap.get("objectId");
            StringList affectedITems = new StringList();
            StringList objSelects = new StringList();
            // RFC-115: PCM : 04/05/2017: AG Start
            objSelects.add(DomainConstants.SELECT_ID);
            objSelects.add("physicalid");
            objSelects.add(DomainConstants.SELECT_TYPE);

            StringList relSelects = new StringList();
            relSelects.add("to.id");
            relSelects.add("to.physicalid");
            DomainObject domChangeNotice = new DomainObject(strCNID);
            // Modified for findbug issue TIGTK-6439 : 04/13/2017 : PTE- Start
            StringList slobjTmp = domChangeNotice.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS + "].to.id");
            StringList excludeIds = DomainConstants.EMPTY_STRINGLIST;
            excludeIds = slobjTmp;

            StringList slMCOSelects = new StringList("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.id");
            slMCOSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.physicalid");

            Map mapChangeObjectDetails = domChangeNotice.getInfo(context, slMCOSelects);
            String strMCOId = (String) mapChangeObjectDetails.get("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.id");
            String strMCOPlantName = (String) mapChangeObjectDetails
                    .get("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.physicalid");

            if (strMCOId != null && !"".equalsIgnoreCase(strMCOId)) {
                DomainObject domMCO = new DomainObject(strMCOId);
                String relPatter = TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "," + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM;
                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM);

                MapList mlInfos = domMCO.getRelatedObjects(context, relPatter, "*", DomainConstants.EMPTY_STRINGLIST, relSelects, false, true, (short) 0, "", "", 0, null, relPattern, null);

                for (Iterator<?> iterator2 = mlInfos.iterator(); iterator2.hasNext();) {
                    Map<?, ?> mObject = (Map<?, ?>) iterator2.next();

                    String affectedItemID = (String) mObject.get("to.physicalid");
                    // PCM : TIGTK-9047 : 14/07/2017 : AB : START
                    List<String> lPlants = FRCMBOMModelerUtility.getPlantsAttachedToMBOMReference(context, plmSession, affectedItemID);

                    if (lPlants != null && !lPlants.isEmpty()) {
                        if (lPlants.contains(strMCOPlantName)) {
                            affectedITems.add(affectedItemID);
                        }
                    }
                    // PCM : TIGTK-9047 : 14/07/2017 : AB : END
                }
                String strAssemblyId = domMCO.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM + "].to.id");

                if (strAssemblyId != null && !"".equalsIgnoreCase(strAssemblyId)) {
                    Map<String, Object> mapObj;
                    String objPID, type;
                    ContextUtil.startTransaction(context, false);

                    context.setApplication("VPLM");
                    plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
                    plmSession.openSession();
                    bIsPLMSessionOpen = true;
                    MapList res = (MapList) FRCMBOMModelerUtility.getVPMStructure(context, plmSession, strAssemblyId, objSelects, DomainConstants.EMPTY_STRINGLIST, (short) 0, null, null);

                    for (int i = 0; i < res.size(); i++) {
                        mapObj = (Map) res.get(i);
                        objPID = (String) mapObj.get("physicalid");
                        String strLogicalID = (String) mapObj.get("logicalid");
                        type = (String) mapObj.get(DomainConstants.SELECT_TYPE);
                        // PCM : TIGTK-9047 : 14/07/2017 : AB : START
                        List<String> lPlants = FRCMBOMModelerUtility.getPlantsAttachedToMBOMReference(context, plmSession, strLogicalID);
                        // PCM : TIGTK-9177 : 11/08/2017 : AB : START
                        if (lPlants != null && !lPlants.isEmpty()) {
                            // PCM : TIGTK-9177 : 11/08/2017 : AB : END
                            if (lPlants.contains(strMCOPlantName)) {
                                affectedITems.add(objPID);
                            }
                        }
                        // PCM : TIGTK-9047 : 14/07/2017 : AB : END
                    }

                    for (Iterator iterator = affectedITems.iterator(); iterator.hasNext();) {

                        List lConnectedTools = FRCMBOMModelerUtility.getResourcesAttachedToMBOMReference(context, plmSession, (String) iterator.next());
                        for (int itr = 0; itr < lConnectedTools.size(); itr++) {
                            String strToolPhyId = (String) lConnectedTools.get(itr);
                            DomainObject domTool = DomainObject.newInstance(context, strToolPhyId);
                            String strPolicy = domTool.getInfo(context, DomainConstants.SELECT_POLICY);
                            String strToolId = domTool.getInfo(context, DomainConstants.SELECT_ID);
                            // Modified for findbug issue TIGTK-6439 : 04/13/2017 : PTE
                            // PCM : TIGTK-9177 : 17/08/2017 : AB : START
                            if (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_TOOL) && !excludeIds.contains(strToolId) && !slTools.contains(strToolId)) {
                                // PCM : TIGTK-9177 : 17/08/2017 : AB : END
                                slTools.add(strToolId);
                            }

                        }

                    }

                    // Fix for FindBugs issue Possible Null pointer : Harika Varanasi : 21 March 2017: End
                }
            }
            // TIGTK-5813 | 31/03/2017 | Harika Varanasi : SteepGraph : Starts
            if (slTools.isEmpty()) {
                slTools.addElement(" ");
            }
            // TIGTK-5813 | 31/03/2017 | Harika Varanasi : SteepGraph : Ends
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getRelatedTools: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End

            ContextUtil.abortTransaction(context);
        } finally {
            if (bIsPLMSessionOpen) {
                plmSession.closeSession(true);
                ContextUtil.commitTransaction(context);
            }

        }
        // RFC-115: PCM : 04/05/2017: AG END
        return slTools;
    }

    /**
     * Access Funtion for the Add Existing Parts on Change Notice in Item to Transfer Tab
     * @param context
     * @param args
     * @return Boolean
     * @throws Exception
     */
    // TS-101: Changes after the Reviews
    // Access Funtion
    public boolean checkAddPartsAccess(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCNID = (String) programMap.get("objectId");
            String currentUser = context.getUser();
            DomainObject domChangeNotice = new DomainObject(strCNID);
            String strObjOwner = domChangeNotice.getInfo(context, DomainConstants.SELECT_OWNER);

            if (strObjOwner.equalsIgnoreCase(currentUser)) {
                String strCNType = domChangeNotice.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_CNTYPE + "]");
                if ("Parts and BOM".equalsIgnoreCase(strCNType)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkAddPartsAccess: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return false;
    }

    /**
     * Access Funtion for the Add Existing Tools on Change Notice in Item to Transfer Tab
     * @param context
     * @param args
     * @return Boolean
     * @throws Exception
     */
    // TS-104: Changes after the Reviews
    // Access Funtion
    public boolean checkAddToolAccess(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCNID = (String) programMap.get("objectId");
            String currentUser = context.getUser();
            DomainObject domChangeNotice = new DomainObject(strCNID);
            String strObjOwner = domChangeNotice.getInfo(context, DomainConstants.SELECT_OWNER);

            if (strObjOwner.equalsIgnoreCase(currentUser)) {
                String strCNType = domChangeNotice.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_CNTYPE + "]");
                if ("Tool".equalsIgnoreCase(strCNType)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkAddToolAccess: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return false;
    }

    /**
     * This Method is used on the Properties Form of the Change Notice to display the CN Related Program Project
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    // TS-102 - CN- Properties Page

    public String getCNMCOProject(Context context, String[] args) throws Exception {
        String showRelObjectName = DomainObject.EMPTY_STRING;
        try {

            String strProgProjId = DomainObject.EMPTY_STRING;
            String strProgProjName = DomainObject.EMPTY_STRING;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strCNId = (String) requestMap.get("objectId");

            DomainObject domChangeNotice = new DomainObject(strCNId);
            String strMCOId = domChangeNotice.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.id");
            DomainObject domMCO = DomainObject.newInstance(context, strMCOId);
            strProgProjName = domMCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
            strProgProjId = domMCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            if (strProgProjName == null || "".equalsIgnoreCase(strProgProjName)) {
                strProgProjName = "";
            }
            StringBuffer sbBuffer = new StringBuffer();
            sbBuffer.append("&#160;&#160;<a href=\"JavaScript:emxFormLinkClick('../common/emxTree.jsp?objectId=");
            sbBuffer.append(XSSUtil.encodeForURL(context, strProgProjId));
            sbBuffer.append("','popup', '', '', '')\">");
            sbBuffer.append(XSSUtil.encodeForHTML(context, strProgProjName));
            sbBuffer.append("</a>&nbsp;");
            showRelObjectName = sbBuffer.toString();
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNMCOProject: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return showRelObjectName;
    }

    /**
     * This Method is used on the Properties Form of the Change Notice to display the CN Related Plant
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */

    // TS-102 - CN- Properties Page
    public String getCNMCORelatedPlant(Context context, String[] args) throws Exception {
        String showRelObjectName = DomainObject.EMPTY_STRING;
        try {
            String strPlantName = DomainObject.EMPTY_STRING;
            String strPlantId = DomainObject.EMPTY_STRING;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strCNId = (String) requestMap.get("objectId");

            DomainObject domChangeNotice = new DomainObject(strCNId);
            String strMCOId = domChangeNotice.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.id");

            DomainObject domMCO = DomainObject.newInstance(context, strMCOId);
            strPlantName = domMCO.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");
            strPlantId = domMCO.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.id");

            if (strPlantName == null || "".equalsIgnoreCase(strPlantName)) {
                strPlantName = "";
            }
            StringBuffer sbBuffer = new StringBuffer();
            sbBuffer.append("&#160;&#160;<a href=\"JavaScript:emxFormLinkClick('../common/emxTree.jsp?objectId=");
            sbBuffer.append(XSSUtil.encodeForURL(context, strPlantId));
            sbBuffer.append("','popup', '', '', '')\">");
            sbBuffer.append(XSSUtil.encodeForHTML(context, strPlantName));
            sbBuffer.append("</a>&nbsp;");
            showRelObjectName = sbBuffer.toString();

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNMCORelatedPlant: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return showRelObjectName;
    }

    /**
     * This Method is used on the Properties Form of the Change Notice to display the CN Related 150% MBOM
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */

    // TS-102 - CN- Properties Page
    public String getCNMCO150MBOM(Context context, String[] args) throws Exception {
        String showRelObjectName = DomainObject.EMPTY_STRING;
        try {
            String strVName = DomainObject.EMPTY_STRING;
            String str150MBOMId = DomainObject.EMPTY_STRING;
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strCNId = (String) requestMap.get("objectId");
            DomainObject domChangeNotice = new DomainObject(strCNId);
            String strMCOId = domChangeNotice.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.id");
            DomainObject domMCO = DomainObject.newInstance(context, strMCOId);
            str150MBOMId = domMCO.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM + "].to.id");
            // TIGTK-8076 - 5-23-2017 - PTE - START
            DomainObject domObj = new DomainObject(str150MBOMId);
            strVName = domObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
            if (strVName == null || "".equalsIgnoreCase(strVName)) {
                strVName = "";
            }
            // TIGTK-8076 - 5-23-2017 - PTE - END

            if (strVName == null || "".equalsIgnoreCase(strVName)) {
                strVName = "";
            }

            StringBuffer sbBuffer = new StringBuffer();
            sbBuffer.append("&#160;&#160;<a href=\"JavaScript:emxFormLinkClick('../common/emxTree.jsp?objectId=");
            sbBuffer.append(XSSUtil.encodeForURL(context, str150MBOMId));
            sbBuffer.append("','popup', '', '', '')\">");
            sbBuffer.append(XSSUtil.encodeForHTML(context, strVName));
            sbBuffer.append("</a>&nbsp;");
            showRelObjectName = sbBuffer.toString();

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNMCO150MBOM: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return showRelObjectName;
    }

    /**
     * @description: Function on Effectivity Date Field which populates the Physical Implementation Planned Date of related MCO
     * @param context
     * @param args
     * @return String
     * @throws Exception
     * @TIGTK 2763
     */
    public String getCNMCORelatedSOPDate(Context context, String[] args) throws Exception {
        String strDate = "";
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        try {
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strMCOId = (String) requestMap.get("objectId");
            DomainObject domMCO = DomainObject.newInstance(context, strMCOId);
            strDate = domMCO.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PHYSICAL_IMPLEMENTATION_PLANNED_DATE);
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNMCORelatedSOPDate: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return strDate;
    }

    /**
     * @description: Update Function to update the Effectivity Date on CN Creation.
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @TIGTK 2763
     */

    public void setCNEffectivityDate(Context context, String[] args) throws Exception {
        String strNewDate = "";
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        try {
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strCNId = (String) paramMap.get("objectId");
            strNewDate = (String) paramMap.get("New Value");
            Locale lLocale = (Locale) requestMap.get("localeObject");
            // TIGTK-8883 :Rutuja Ekatpure:5/7/2017:Start
            // convert date into matrix date format to support different browser languase
            strNewDate = getDateInMatrixDateFormat(strNewDate, lLocale);
            // TIGTK-8883 :Rutuja Ekatpure:5/7/2017:End
            DomainObject domCN = DomainObject.newInstance(context, strCNId);
            domCN.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_EFFECTIVITYDATE, strNewDate);
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setCNEffectivityDate: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    public String getObjectLinkHTML(Context context, String text, String objectId) throws Exception {

        String link = getObjectLink(context, objectId);
        // Fix for FindBugs issue Redundant Null Check : Harika Varanasi : 21 March 2017: Start
        if (UIUtil.isNotNullAndNotEmpty(link)) {
            // Fix for FindBugs issue Redundant Null Check : Harika Varanasi : 21 March 2017: End
            link = "<a href='" + link + "'>" + text + "</a>";
        } else {
            link = text;
        }

        return (link);
    }

    public String getObjectLink(Context context, String objectId) throws Exception {

        String link = "";
        String baseURL = MailUtil.getBaseURL(context);
        if (baseURL != null && baseURL.length() > 0) {
            link = baseURL + "?objectId=" + objectId;
        }

        return (link);
    }

    /**
     * Description : This method is used get the calendar date for Route Due Date Modified for TIGTK-3831
     * @author abhalani
     * @args
     * @Date December 14, 2016
     */

    public String getRouteDueDate(Context context, String[] args) throws Exception {
        SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, -1);
        cal.add(Calendar.DATE, 7);
        Date date = cal.getTime();
        String strDate = _mxDateFormat.format(date);
        return strDate;
    }

    /**
     * Description : This method is used to check that whether CN has connected with any affected item, if it is connected then block the modify of PSS_CN_Type's value PCM : TIGTK-4786 : AB
     * @author abhalani
     * @args
     * @Date February 27, 2017
     */

    public int checkAffectedItemIsConnectedToCNOrNot(Context context, String[] args) throws Exception {

        int intReturn = 0;
        try {
            String strCNID = args[0];
            DomainObject domCN = new DomainObject(strCNID);
            StringList slCNAffectedItems = domCN.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS + "].to.id");

            // If Affected Items connected with Change Notice than Don't allow modification of PSS_CNType attribute.
            if (!slCNAffectedItems.isEmpty()) {
                String strLanguage = context.getSession().getLanguage();
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage), "PSS_EnterpriseChangeMgt.Alert.ModifyCNTypeAlert");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                intReturn = 1;
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkAffectedItemIsConnectedToCNOrNot: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return intReturn;
    }

    /**
     * Description : This method is used check affected item is conected to CN for TIGTK-4672
     * @author PTE
     * @args
     * @Date feb 27, 2017
     */
    // Fix for FindBugs Method naming convention : Harika Varanasi : 21 March 2017: Start
    public int checkAffectedItemConnected(Context context, String args[]) throws Exception {
        // Fix for FindBugs Method naming convention : Harika Varanasi : 21 March 2017: End
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            Locale strLocale = context.getLocale();
            String objectId = args[0];
            DomainObject domCNObj = new DomainObject(objectId);
            String strAffectedItemId = domCNObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS + "].to.id");

            if (UIUtil.isNullOrEmpty(strAffectedItemId)) {
                String strNoAffectedItemMsg = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", strLocale, "PSS_EnterpriseChangeMgt.Alert.NoAffectesItemOnCN");
                MqlUtil.mqlCommand(context, "notice $1", strNoAffectedItemMsg);
                return 1;
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkAffectedItemConnected: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return 0;
    }

    // PCM TIGTK-4936 | 02/03/17 :Pooja Mantri : Start
    /**
     * PMC : TIGTK-8755 : 23/06/2017 : AB This method is called as Access Program on Command PSS_ECMCNCancel. It is used to check if Connected MCO is Cancelled or Not
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    public boolean checkAccessForCancelCN(Context context, String args[]) throws Exception {
        boolean retStatus = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");

            // Create Change Notice Object
            DomainObject domChangeNotice = DomainObject.newInstance(context, strObjectId);

            // Get the Owner and current state of Change Object
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(DomainConstants.SELECT_CURRENT);
            slObjectSelects.add(DomainConstants.SELECT_ORIGINATOR);
            slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.current");

            Map mapChangeNoticeInfo = domChangeNotice.getInfo(context, slObjectSelects);

            // Get Connected MCO State to CN Object
            String slMCOCurrent = (String) mapChangeNoticeInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.current");
            if (TigerConstants.STATE_PSS_MCO_CANCELLED.equalsIgnoreCase(slMCOCurrent)) {
                retStatus = false;
            } else {
                String strOriginator = (String) mapChangeNoticeInfo.get(DomainConstants.SELECT_ORIGINATOR);
                String strCurrent = (String) mapChangeNoticeInfo.get(DomainConstants.SELECT_CURRENT);

                // If change object is in Prepare state then check for User Access
                if (TigerConstants.STATE_PREPARE_CN.equalsIgnoreCase(strCurrent)) {
                    // If current user is Global user or PLM SupportTeam then allow access Or if loggedin user is originator
                    String strLoggedUser = context.getUser();
                    String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strLoggedUser);

                    if (UIUtil.isNotNullAndNotEmpty(strLoggedUserSecurityContext)) {
                        String strLoggerUserRole = (strLoggedUserSecurityContext.split("[.]")[0]);

                        if (strLoggerUserRole.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || strLoggerUserRole.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)
                                || strOriginator.equalsIgnoreCase(strLoggedUser)) {
                            retStatus = true;
                        }
                    }
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkAccessForCancelCN: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return retStatus;
    }

    // PCM TIGTK-4936 | 02/03/17 :Pooja Mantri : End

    // TGPSS_PCM_TS_157_ChangeNoticeNotifications_V2.2 | 14/03/2017 | Harika Varanasi : Starts

    /**
     * getChangeNoticeInformation method is used to get all information about Change Notice As a Part of TGPSS_PCM_TS_157_ChangeNoticeNotifications_V2.2
     * @param context
     * @param strCNId
     * @return Map
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public Map getChangeNoticeInformation(Context context, String strCNId) throws Exception {
        Map mapCN = new HashMap();
        try {
            if (UIUtil.isNotNullAndNotEmpty(strCNId)) {
                DomainObject domCNObj = DomainObject.newInstance(context, strCNId);

                StringList slObjectSelects = new StringList(20);
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                slObjectSelects.addElement(DomainConstants.SELECT_NAME);
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
                slObjectSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CNTYPE + "]");
                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_EFFECTIVITYDATE + "]");
                slObjectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.id");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS + "].to.name");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS + "].to.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.id");

                // PCM : TIGTK-9528 : 0109/2017 : AB : START
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS + "].to.name");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS + "].to.id");
                // PCM : TIGTK-9528 : 0109/2017 : AB : END
                mapCN = domCNObj.getInfo(context, slObjectSelects);
            }

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeNoticeInformation: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        } finally {
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS + "].to.name");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS + "].to.id");
        }

        return mapCN;
    }

    /**
     * transformCNMapToHTMLList As a Part of TGPSS_PCM_TS_157_ChangeNoticeNotifications_V2.2
     * @param context
     * @param objectMap
     * @return MapList
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public MapList transformCNMapToHTMLList(Context context, Map objectMap, String strBaseURL, boolean isTaskReject, boolean isReassign) throws Exception {
        MapList mlInfoList = new MapList();
        try {

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);

            StringList slHyperLinkLabelKey = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.CNNotification.HyperLinkLabelKey"), ",");
            StringList slHyperLinkLabelKeyIds = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.CNNotification.HyperLinkLabelKeyIds"), ",");
            // TIGTK-11455 :START
            if (isReassign) {
                initializeCNLinkedHashMap(isReassign, isTaskReject, lhmMCNCommentsSelectionStore);
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmMCNCommentsSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            } else if (isTaskReject) {
                initializeCNLinkedHashMap(isReassign, isTaskReject, lhmMCNRejectSelectionStore);
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmMCNRejectSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);

            } else {
                initializeCNLinkedHashMap(isReassign, isTaskReject, lhmMCNSelectionStore);
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmMCNSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            }
            // TIGTK-11455 :END
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in transformCNMapToHTMLList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mlInfoList;
    }

    /**
     * initializeCNLinkedHashMap As a Part of TGPSS_PCM_TS_157_ChangeNoticeNotifications_V2.2
     * @throws Exception
     *             if the operation fails
     * @author Harika Varanasi : SteepGraph
     */
    public void initializeCNLinkedHashMap(boolean isReassign, boolean isTaskReject, LinkedHashMap<String, String> lhmMCNSelectionStore) throws Exception {
        try {

            if (lhmMCNSelectionStore != null && (lhmMCNSelectionStore.isEmpty())) {

                lhmMCNSelectionStore.put("Title", "SectionHeader");
                lhmMCNSelectionStore.put("Subject", "SectionSubject");
                lhmMCNSelectionStore.put("Main_Information", "SectionHeader");
                lhmMCNSelectionStore.put("Project_Code", "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                lhmMCNSelectionStore.put("Project_Description",
                        "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                lhmMCNSelectionStore.put("CN", DomainConstants.SELECT_NAME);
                lhmMCNSelectionStore.put("CN_Description", DomainConstants.SELECT_DESCRIPTION);
                lhmMCNSelectionStore.put("State", DomainConstants.SELECT_CURRENT);
                lhmMCNSelectionStore.put("CN_Type", "attribute[" + TigerConstants.ATTRIBUTE_PSS_CNTYPE + "]");
                lhmMCNSelectionStore.put("Effectivity_Date", "attribute[" + TigerConstants.ATTRIBUTE_PSS_EFFECTIVITYDATE + "]");
                lhmMCNSelectionStore.put("CN_Plant", "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");
                lhmMCNSelectionStore.put("CN_Creator", "attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                // TIGTK-7580 : START
                lhmMCNSelectionStore.put("Comment", "TransferComments");
                // TIGTK-7580 : END
                // TIGTK-11455 :START
                if (isTaskReject) {
                    lhmMCNSelectionStore.put("RejectedTask_Comment", "RejectedTask_Comment");
                }
                if (isReassign) {
                    lhmMCNSelectionStore.put("Comments", "Comments");
                }
                // TIGTK-11455 :END
                lhmMCNSelectionStore.put("Useful_Links", "SectionHeader");
                lhmMCNSelectionStore.put("CN_Content", "from[" + TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS + "].to.name");
                lhmMCNSelectionStore.put("Related_MCOs", "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.name");
            }

        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeCNLinkedHashMap: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }

    /**
     * getCNNotificationBodyHTML method is used to CN messageHTML in Notification Object As apart of TGPSS_PCM_TS_157_ChangeNoticeNotifications_V2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getCNNotificationBodyHTML(Context context, String[] args) throws Exception {
        String messageHTML = "";
        // TIGTK-10709 -- START
        String strSectionSub = DomainConstants.EMPTY_STRING;
        // TIGTK-10709 -- END
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCNObjId = (String) programMap.get("id");
            String strBaseURL = (String) programMap.get("baseURL");
            String notificationObjName = (String) programMap.get("notificationName");
            String strAttrSubText = PropertyUtil.getSchemaProperty(context, "attribute_SubjectText");
            StringList busSelects = new StringList("attribute[" + strAttrSubText + "]");
            String strAttrTransferSAPValue = "Yes";

            // TIGTK-11455 :START
            DomainObject domCNObject = DomainObject.newInstance(context, strCNObjId);

            Map CNMap = new HashMap();
            if (domCNObject.isKindOf(context, TigerConstants.TYPE_PSS_CHANGENOTICE)) {
                CNMap = (Map) getChangeNoticeInformation(context, strCNObjId);
            }
            // TIGTK-11455 :END
            String strSubjectKey = "";
            MapList mpNotificationList = DomainObject.findObjects(context, "Notification", notificationObjName, "*", "*", TigerConstants.VAULT_ESERVICEADMINISTRATION, "", null, true, busSelects,
                    (short) 0);
            Map payLoadMap = (Map) programMap.get("payload");
            // TIGTK-11455 :START
            if (payLoadMap != null) {

                if (payLoadMap.containsKey("id")) {
                    strCNObjId = (String) payLoadMap.get("id");
                    CNMap = (Map) getChangeNoticeInformation(context, strCNObjId);
                }
                if (payLoadMap.containsKey("TransferComments")) {
                    String strComments = (String) payLoadMap.get("TransferComments");
                    CNMap.put("TransferComments", strComments);
                }
                if (payLoadMap.containsKey("RejectedTask_Comment")) {
                    String strComments = (String) payLoadMap.get("RejectedTask_Comment");
                    CNMap.put("RejectedTask_Comment", strComments);
                }
                if (payLoadMap.containsKey("Comments")) {
                    String strComments = (String) payLoadMap.get("Comments");
                    CNMap.put("Comments", strComments);
                }
            }
            // TIGTK-11455 :end
            if (mpNotificationList != null && (!mpNotificationList.isEmpty())) {
                strSubjectKey = (String) ((Map) mpNotificationList.get(0)).get("attribute[" + strAttrSubText + "]");
            }

            if (UIUtil.isNotNullAndNotEmpty(strSubjectKey)) {
                CNMap.put("SectionSubject", EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strSubjectKey));
            } else {
                CNMap.put("SectionSubject", "");
            } // TIGTK-7580: START
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_TransferOwnershipNotification".equalsIgnoreCase(notificationObjName)) {
                pss.ecm.enoECMChange_mxJPO enoECMChange = new pss.ecm.enoECMChange_mxJPO();
                strSubjectKey = enoECMChange.getTranferOwnershipSubject(context, args);
                CNMap.put("SectionSubject", strSubjectKey);

            } else {
                CNMap.put("TransferComments", "");
            }
            // TIGTK-7580 : END
            boolean isTaskReject = false;
            boolean isReassign = false;
            // TIGTK-10709 -- START
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_CNTaskReassignNotification".equalsIgnoreCase(notificationObjName)) {
                pss.ecm.enoECMChange_mxJPO enoECMObj = new pss.ecm.enoECMChange_mxJPO();
                strSectionSub = enoECMObj.getTaskReassignmentSubject(context, args);
                CNMap.put("SectionSubject", strSectionSub);
                isReassign = true;
            }
            // TIGTK-11455 :START
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName)
                    && ("PSSObjectRouteTaskRejectedForChangeNoticeEvent".equalsIgnoreCase(notificationObjName) || "PSS_CNRejectNotification".equalsIgnoreCase(notificationObjName))) {
                isTaskReject = true;
            }
            // TIGTK-10709 -- END
            MapList mlInfoList = transformCNMapToHTMLList(context, CNMap, strBaseURL, isTaskReject, isReassign);
            // TIGTK-11455 :END
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            String strStyleSheet = commonObj.getFormStyleSheet(context, "PSS_NotificationStyleSheet");
            messageHTML = commonObj.getHTMLTwoColumnTable(context, mlInfoList, strStyleSheet);
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNNotificationBodyHTML: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return messageHTML;
    }

    /**
     * getCNRouteAssigneeList As a Part of TGPSS_PCM_TS_157_ChangeNoticeNotifications_V2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public StringList getCNRouteAssigneeList(Context context, String[] args) throws Exception {
        StringList slCNApprovalRouteAssignees = new StringList();

        // PCM : TIGTK-10799 : 13/11/2017 : Aniket Madhav : START
        String strContextUser = context.getUser();
        // PCM : TIGTK-10799 : 13/11/2017 : Aniket Madhav : END

        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCNObjId = (String) programMap.get("id");
            if (UIUtil.isNotNullAndNotEmpty(strCNObjId)) {
                pss.ecm.enoECMChange_mxJPO enoECMChangeObject = new pss.ecm.enoECMChange_mxJPO();
                StringList slTempCNApprovalRouteAssignees = enoECMChangeObject.getChangeRouteAssignees(context, strCNObjId, "state_InReview");

                // PCM : TIGTK-7745 : 24/08/2017 : AB : START
                if (!slTempCNApprovalRouteAssignees.isEmpty()) {
                    HashSet setCAApprovalRouteAssignees = new HashSet(slTempCNApprovalRouteAssignees);
                    ArrayList<String> list = new ArrayList<String>(setCAApprovalRouteAssignees);
                    slCNApprovalRouteAssignees = new StringList(list);
                }
                // PCM : TIGTK-7745 : 24/08/2017 : AB : END

                // PCM : TIGTK-10799 : 23/08/2017 : Aniket Madhav : START
                if (slCNApprovalRouteAssignees.contains(strContextUser)) {
                    while (slCNApprovalRouteAssignees.contains(strContextUser)) {
                        slCNApprovalRouteAssignees.remove(strContextUser);
                    }
                }
                // PCM : TIGTK-10799 : 23/08/2017 : Aniket Madhav : END
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNRouteAssigneeList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return slCNApprovalRouteAssignees;
    }

    /**
     * getCNRejectionList As a Part of TGPSS_PCM_TS_157_ChangeNoticeNotifications_V2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public StringList getCNRejectionList(Context context, String[] args) throws Exception {
        StringList slCNRejectionToList = new StringList();

        // PCM : TIGTK-10799 : 13/11/2017 : Aniket Madhav : START
        String strContextUser = context.getUser();
        pss.ecm.ui.MfgChangeOrder_mxJPO mfgChangeOrder = new pss.ecm.ui.MfgChangeOrder_mxJPO();
        strContextUser = mfgChangeOrder.getGenericContextOrLoginUser(context, args);
        // PCM : TIGTK-10799 : 13/11/2017 : Aniket Madhav : END
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCNObjId = (String) programMap.get("id");
            if (UIUtil.isNotNullAndNotEmpty(strCNObjId)) {
                DomainObject domCNObj = DomainObject.newInstance(context, strCNObjId);
                String strCRInitiator = domCNObj.getInfo(context, "attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");

                // Adding CR Initiator to the To List
                if (UIUtil.isNotNullAndNotEmpty(strCRInitiator)) {
                    slCNRejectionToList.addElement(strCRInitiator);
                }

                // add All approval Assignees to CN To List
                pss.ecm.enoECMChange_mxJPO enoECMChangeObject = new pss.ecm.enoECMChange_mxJPO();
                StringList slAllRouteAssignees = enoECMChangeObject.getChangeRouteAssignees(context, strCNObjId, "state_InReview");
                // Fix for FindBugs issue Redundant Null check capture: Harika Varanasi : 21 March 2017: START
                if (slAllRouteAssignees.size() > 0 && (!slAllRouteAssignees.isEmpty())) {
                    // Fix for FindBugs issue Redundant Null check capture: Harika Varanasi : 21 March 2017: START
                    slCNRejectionToList.addAll(slAllRouteAssignees);
                }

                // PCM : TIGTK-10799 : 23/08/2017 : Aniket Madhav : START
                if (slCNRejectionToList.contains(strContextUser)) {
                    while (slCNRejectionToList.contains(strContextUser)) {
                        slCNRejectionToList.remove(strContextUser);
                    }
                }
                // PCM : TIGTK-10799 : 23/08/2017 : Aniket Madhav : END

            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCNRejectionList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return slCNRejectionToList;
    }

    // TGPSS_PCM_TS_157_ChangeNoticeNotifications_V2.2 | 14/03/2017 |Harika Varanasi : Ends

    // PCM TIGTK-6065 | 31/03/17 :Pooja Mantri : Start
    /**
     * Description : This method is used get the MCOs connected to CN.
     * @return -- MapList -- Containing MCO Id
     * @args
     */

    public MapList getRelatedMCOToCN(Context context, String args[]) throws Exception {
        MapList mlMCOList = new MapList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strCNObjectId = (String) programMap.get("objectId");

            DomainObject domCN = new DomainObject(strCNObjectId);
            StringList slObjSelectStmts = new StringList(1);
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            // 3951:Find Bugs
            mlMCOList = domCN.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATEDCN, // relationship
                    // pattern
                    TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, // object pattern
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
            // 3951:Find Bugs

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getRelatedMCOToCN: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return mlMCOList;

    }

    // PCM TIGTK-6065 | 31/03/17 :Pooja Mantri : End

    // TIGTK-5921 | 04/04/2017 | Harika Varanasi : Starts
    /**
     * reStartRouteOnCNTaskRejection
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    public void reStartRouteOnCNTaskRejection(Context context, String[] args) throws Exception {
        try {
            String ObjectId = args[0];
            if (UIUtil.isNotNullAndNotEmpty(ObjectId)) {
                DomainObject domCN = DomainObject.newInstance(context, ObjectId);

                StringList slSelectRelStmts = new StringList(1);

                StringList slObjSelectStmts = new StringList(1);
                slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                slObjSelectStmts.addElement("to[" + Route.RELATIONSHIP_ROUTE_TASK + "].from.attribute[" + Route.ATTRIBUTE_APPROVAL_STATUS + "]");

                DomainObject.MULTI_VALUE_LIST.add("to[" + Route.RELATIONSHIP_ROUTE_TASK + "].from.attribute[" + Route.ATTRIBUTE_APPROVAL_STATUS + "]");
                MapList mlRouteList = domCN.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, // relationship
                        // pattern
                        DomainConstants.TYPE_ROUTE, // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null, null);

                int mlSize = mlRouteList.size();
                if (!mlRouteList.isEmpty() && mlSize > 0) {
                    Map routeMap = null;
                    String strRouteId = "";
                    String strTaskApprvoalStatus = "";
                    StringList slRtAppStatus = new StringList();
                    for (int i = 0; i < mlSize; i++) {
                        routeMap = (Map) mlRouteList.get(i);
                        strRouteId = (String) routeMap.get(DomainConstants.SELECT_ID);
                        try {
                            strTaskApprvoalStatus = (String) routeMap.get("to[" + Route.RELATIONSHIP_ROUTE_TASK + "].from.attribute[" + Route.ATTRIBUTE_APPROVAL_STATUS + "]");
                            slRtAppStatus.addElement(strTaskApprvoalStatus);
                        } catch (ClassCastException cse) {
                            slRtAppStatus = (StringList) routeMap.get("to[" + Route.RELATIONSHIP_ROUTE_TASK + "].from.attribute[" + Route.ATTRIBUTE_APPROVAL_STATUS + "]");
                        }

                        if (slRtAppStatus.size() > 0 && slRtAppStatus.contains("Reject")) {
                            com.matrixone.apps.common.Route route = new com.matrixone.apps.common.Route(strRouteId);
                            // route.reStart(context);
                            route.resume(context);
                        }
                    }
                }

            }

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in reStartRouteOnCNTaskRejection: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }

    // TIGTK-5921 | 04/04/2017 | Harika Varanasi : Ends

    // TIGTK-8883 :Rutuja Ekatpure:5/7/2017:start
    /****
     * this method used to parse date which is in different format according to browser languase and format into matrix format
     * @param strLocalDate
     *            : local date in string format
     * @param lLocale
     *            :locale object
     * @return String : formated date
     * @author Rutuja Ekatpure : SteepGraph
     */
    public String getDateInMatrixDateFormat(String strLocalDate, Locale lLocale) {
        String strDateInMatrixFormat = DomainConstants.EMPTY_STRING;
        try {
            // get matrix date format
            SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
            // get local date format according tobrowser language used
            String strlocalDateFormat = ((java.text.SimpleDateFormat) java.text.DateFormat.getDateInstance(eMatrixDateFormat.getEMatrixDisplayDateFormat(), lLocale)).toPattern();
            SimpleDateFormat sdlocalDateDormat = new SimpleDateFormat(strlocalDateFormat, lLocale);
            // parse date according to local date format
            Date date = sdlocalDateDormat.parse(strLocalDate);
            // format date in matrix date format
            strDateInMatrixFormat = _mxDateFormat.format(date);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("Error in getDateInMatrixDateFormat: ", e);
        }
        return strDateInMatrixFormat;
    }
    // TIGTK-8883 :Rutuja Ekatpure:5/7/2017:end
}