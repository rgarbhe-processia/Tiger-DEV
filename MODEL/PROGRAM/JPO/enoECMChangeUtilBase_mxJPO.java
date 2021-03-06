
/*
 ** ${CLASSNAME}
 **
 ** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes. Copyright notice is precautionary only and does
 * not evidence any actual or intended publication of such program
 */

import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.StringList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.dassault_systemes.enovia.e6w.foundation.jaxb.FieldValue;
import com.dassault_systemes.enovia.e6w.foundation.ServiceBase;
import com.dassault_systemes.enovia.enterprisechangemgt.admin.ECMAdmin;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeManagement;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeSubscription;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeWidgets;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.common.util.SubscriptionUtil;
import com.matrixone.apps.domain.*;
import com.matrixone.apps.domain.util.*;
import com.matrixone.apps.framework.ui.UIUtil;
import com.dassault_systemes.enovia.e6w.foundation.jaxb.Status;

/**
 * The <code>enoECMChangeUtilBase</code> class contains implementation code for enoECMChangeUtil
 * @version ECM R211 - # Copyright (c) 1992-2015 Dassault Systemes.
 */

public class enoECMChangeUtilBase_mxJPO extends emxDomainObject_mxJPO {

    /**
     *
     */
    private static final long serialVersionUID = 3382171941614201584L;

    private ChangeUtil changeUtil = null;

    private ChangeWidgets changeWidgets = null;

    private static HashMap _typePolicyStateIndexMap = null;

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @throws Exception
     *             if the operation fails.
     * @since ECM R211
     */
    public enoECMChangeUtilBase_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
        changeUtil = new ChangeUtil();
        changeWidgets = new ChangeWidgets();
    }

    /**
     * This method is executed if a specific method is not specified.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds no arguments.
     * @return int.
     * @throws Exception
     *             if the operation fails.
     * @since ECM R211
     */
    public int mxMain(Context context, String[] args) throws Exception {
        if (true) {
            throw new Exception("must specify method on emxDesignResponsibilityDeleteCheck invocation");
        }
        return 0;
    }

    /**
     * Connects ECR/ECO with the Passed Object.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param Hashmap
     *            holds the input arguments: strRelationship holds relationship with which ECR will be connected New Value is object Id of updated Object
     * @throws Exception
     *             if the operations fails
     * @since ECM R211
     */

    public DomainRelationship connect(Context context, HashMap paramMap, String strRelationship, boolean isToSide) throws Exception {
        try {
            DomainRelationship drship = null;
            // Relationship name
            DomainObject oldListObject = null;
            DomainObject newListObject = null;
            // Getting the ECR Object id and the new MemberList object id
            String strChangeobjectId = (String) paramMap.get("objectId");
            DomainObject changeObj = new DomainObject(strChangeobjectId);
            // for bug 343816 and 343817 starts
            String strNewToTypeObjId = (String) paramMap.get("New OID");

            if (strNewToTypeObjId == null || "null".equals(strNewToTypeObjId) || strNewToTypeObjId.length() <= 0 || "Unassigned".equals(strNewToTypeObjId)) {
                strNewToTypeObjId = (String) paramMap.get("New Value");
            }
            // for bug 343816 and 343817 ends
            String strOldToTypeObjId = (String) paramMap.get("Old OID");

            RelationshipType relType = new RelationshipType(strRelationship);
            if (strOldToTypeObjId != null && !"null".equals(strOldToTypeObjId) && strOldToTypeObjId.length() > 0 && !"Unassigned".equals(strOldToTypeObjId)) {
                oldListObject = new DomainObject(strOldToTypeObjId);
                changeObj.disconnect(context, relType, isToSide, oldListObject);
            }

            if (strNewToTypeObjId != null && !"null".equals(strNewToTypeObjId) && strNewToTypeObjId.length() > 0 && !"Unassigned".equals(strNewToTypeObjId)) {
                newListObject = new DomainObject(strNewToTypeObjId);

                drship = new DomainRelationship(
                        isToSide ? DomainRelationship.connect(context, changeObj, relType, newListObject) : DomainRelationship.connect(context, newListObject, relType, changeObj));
            }

            return drship;
        } catch (Exception ex) {
            throw new FrameworkException((String) ex.getMessage());
        }

    }

    /**
     * Method to transfer the ownership of CO from properties page
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public void transferOwnership(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);

        String transferReason = (String) requestMap.get(ChangeConstants.TRANSFER_REASON);
        String objectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
        String newOwner = (String) requestMap.get(ChangeConstants.NEW_OWNER);
        String[] params = { transferReason, newOwner };

        new ChangeOrder(objectId).transferOwnership(context, transferReason, newOwner);

    }

    /**
     * @author this method gets the List of persons to be notified on occurrence of events like cancel\On hold\Resume\Complete. The Owner\Originator\Reviewer\ApproverList\Distribution List
     *         members\Technical and Senior technical assignees of related CAs will be added in ToList.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            holds the following input arguments: - The ObjectID of the Change Process
     * @throws Exception
     *             if the operation fails.
     * @since ECM R214.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public StringList getToListForChangeProcess(Context context, String[] args) throws Exception {

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) paramMap.get(SELECT_ID);
        ChangeOrder changeOrder = new ChangeOrder(objectId);
        return changeOrder.getToListForChangeProcess(context);
    }

    /**
     * Get the list of all Objects connected to the context Change object as Satisfied Items using 'Resolved To' relationship
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - HashMap containing one String entry for key "objectId"
     * @return a <code>MapList</code> object having the list of Implemented Items for this Change
     * @throws Exception
     *             if the operation fails
     * @since ECM 214
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAllResolvedItems(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strObjectId = (String) programMap.get(ChangeConstants.OBJECT_ID);
        String strRelResolvedTo = RELATIONSHIP_RESOLVED_TO;

        MapList relBusObjPageList = new MapList();
        StringList objectSelects = new StringList(SELECT_ID);
        StringList relSelects = new StringList(SELECT_RELATIONSHIP_ID);

        // the number of levels to expand, 1 equals expand one level, 0 equals expand all
        short recurseToLevel = 1;
        // retrieving ImplementedItems List from Change Context
        setId(strObjectId);
        relBusObjPageList = getRelatedObjects(context, strRelResolvedTo, QUERY_WILDCARD, objectSelects, relSelects, false, true, recurseToLevel, EMPTY_STRING, EMPTY_STRING, 0);
        return relBusObjPageList;
    }

    /**
     * Added for excluding the connected objects to the given object. This can be used for generic purpose.
     * @param context
     * @param args
     * @return List of Object Ids
     * @throws Exception
     * @since R211 ECM
     */
    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeConnectedObjects(Context context, String[] args) throws Exception {
        Map programMap = (Map) JPO.unpackArgs(args);
        String strObjectId = (String) programMap.get(ChangeConstants.OBJECT_ID);
        String strRelationship = (String) programMap.get(ChangeConstants.TARGET_REL_NAME);
        // Get the From side from the URL to decide on traversal
        String strFrom = (String) programMap.get(ChangeConstants.IS_FROM);
        String sMode = (String) programMap.get("");
        StringList excludeList = new StringList();
        String strField = (String) programMap.get("field");

        if (strField != null) {
            // get the Field value from URL param to know the types
            strField = strField.substring(strField.indexOf("=") + 1, strField.length());
            if (strField.indexOf(":") > 0) {
                strField = strField.substring(0, strField.indexOf(":"));
            }
        }
        StringList sSelectables = new StringList();
        sSelectables.add(DomainConstants.SELECT_ID);

        boolean bisTo = true;
        boolean bisFrom = false;
        DomainObject domObj = DomainObject.newInstance(context, strObjectId);

        if ("true".equalsIgnoreCase(strFrom)) {
            bisTo = false;
            bisFrom = true;
        }
        MapList childObjects = domObj.getRelatedObjects(context, PropertyUtil.getSchemaProperty(context, strRelationship), strField == null ? "*" : strField, new StringList(DomainConstants.SELECT_ID),
                null, bisTo, bisFrom, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

        for (int i = 0; i < childObjects.size(); i++) {
            Map tempMap = (Map) childObjects.get(i);
            excludeList.add(tempMap.get(DomainConstants.SELECT_ID));
        }

        return excludeList;
    }

    /**
     * Method call to get the Name in the Last Name, First Name format.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the HashMap containing the following arguments objectList - MapList containn the list of busines objetcs paramList - HashMap containg the arguments like reportFormat,ObjectId,
     *            SuiteDirectory, TreeId
     * @return Object - Vector containing names in last name, first name format
     * @throws Exception
     *             if the operation fails
     * @since ECM R211
     */
    public Vector getCompleteName(Context context, String[] args) throws Exception {

        // XSSOK
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objList = (MapList) programMap.get("objectList");
        HashMap paramList = (HashMap) programMap.get("paramList");
        String strReportFormat = (String) paramList.get("reportFormat");
        Vector completeName = new Vector();

        String strSuiteDir = (String) paramList.get("SuiteDirectory");
        String strJsTreeID = (String) paramList.get("jsTreeID");
        String strParentObjectId = (String) paramList.get("objectId");

        // No of objects
        int iNoOfObjects = objList.size();
        String strObjId;
        String strFirstName;
        String strRelId;
        String strLastName;

        String arrObjId[] = new String[iNoOfObjects];
        String arrRelId[] = new String[iNoOfObjects];

        for (int i = 0; i < iNoOfObjects; i++) {
            arrObjId[i] = (String) ((HashMap) objList.get(i)).get(DomainConstants.SELECT_ID);
            arrRelId[i] = (String) ((HashMap) objList.get(i)).get(DomainConstants.SELECT_RELATIONSHIP_ID);
        }
        StringList listSelect = new StringList(2);
        String strAttrb1 = "attribute[" + DomainConstants.ATTRIBUTE_FIRST_NAME + "]";
        String strAttrb2 = "attribute[" + DomainConstants.ATTRIBUTE_LAST_NAME + "]";
        listSelect.addElement(strAttrb1);
        listSelect.addElement(strAttrb2);

        // Instantiating BusinessObjectWithSelectList of matrix.db and fetching attributes of the objectids
        BusinessObjectWithSelectList attributeList = getSelectBusinessObjectData(context, arrObjId, listSelect);
        StringBuffer strFullName;
        for (int i = 0; i < iNoOfObjects; i++) {
            strFullName = new StringBuffer(100);
            strObjId = arrObjId[i];
            strRelId = arrRelId[i];
            strFirstName = attributeList.getElement(i).getSelectData(strAttrb1);
            strLastName = attributeList.getElement(i).getSelectData(strAttrb2);
            // Constructing the HREF
            if (strReportFormat != null && strReportFormat.equals("null") == false && strReportFormat.equals("") == false) {
                strFullName.append(strLastName).append(" ").append(strFirstName);
            } else {

                strFullName
                        .append("<img src = \"images/iconSmallPerson.gif\"/>&#160;<a href=\"JavaScript:emxTableColumnLinkClick('emxTree.jsp?name="
                                + XSSUtil.encodeForHTMLAttribute(context, strLastName) + XSSUtil.encodeForHTMLAttribute(context, strFirstName) + "&amp;treeMenu=type_Person&amp;emxSuiteDirectory=")
                        .append(XSSUtil.encodeForHTMLAttribute(context, strSuiteDir)).append("&amp;relId=").append(XSSUtil.encodeForHTMLAttribute(context, strRelId)).append("&amp;parentOID=")
                        .append(XSSUtil.encodeForHTMLAttribute(context, strParentObjectId)).append("&amp;jsTreeID=").append(XSSUtil.encodeForHTMLAttribute(context, strJsTreeID))
                        .append("&amp;objectId=").append(XSSUtil.encodeForHTMLAttribute(context, strObjId)).append("', 'null', 'null', 'true', 'popup')\" class=\"object\">")
                        .append(XSSUtil.encodeForHTML(context, strLastName)).append(",&#160;").append(XSSUtil.encodeForHTML(context, strFirstName)).append("</a>");

            }

            completeName.addElement(strFullName.toString());
        }
        return completeName;
    }

    /**
     * Returns a StringList of the object ids for all Assignees for a given change context.
     * @param context
     *            the eMatrix <code>Context</code> object.
     * @param args
     *            contains a packed HashMap containing objectId of change object
     * @return StringList.
     * @since ECM R211
     * @throws Exception
     *             if the operation fails.
     */
    public StringList getAssigneesOIDs(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String parentObjectId = (String) programMap.get(ChangeConstants.OBJECT_ID);
        StringList result = new StringList();

        if (ChangeUtil.isNullOrEmpty(parentObjectId))
            return result;

        String objectId;
        Map map;

        ChangeManagement changeObj = new ChangeManagement(parentObjectId);
        MapList mapList = (MapList) changeObj.getAssignees(context);
        Iterator itr = mapList.iterator();

        for (int i = 0; itr.hasNext(); i++) {
            map = (Map) itr.next();
            objectId = (String) map.get(DomainConstants.SELECT_ID);
            result.addElement(objectId);
        }

        String strOrgId = ChangeUtil.getRtoIdFromName(context, changeObj.getInfo(context, SELECT_ORGANIZATION));

        if (!ChangeUtil.isNullOrEmpty(strOrgId)) {
            StringList strlPersonSelects = new StringList(1);
            strlPersonSelects.add(DomainConstants.SELECT_ID);
            String strOrgWhereClause = "!(to[" + PropertyUtil.getSchemaProperty(context, "relationship_Member") + "].from.id == " + strOrgId + ")";
            // Need to eliminate persons from different orgs
            MapList totalresultList = DomainObject.findObjects(context, DomainConstants.TYPE_PERSON, "*", "*", "*", "*", strOrgWhereClause, null, true, strlPersonSelects, (short) 0);

            Iterator itrPersons = totalresultList.iterator();
            while (itrPersons.hasNext()) {
                Map mapPerson = (Map) itrPersons.next();
                result.add((String) mapPerson.get(DomainConstants.SELECT_ID));
            }
        }

        return result;
    }

    private boolean isValidData(String data) {
        return ((data == null || "null".equals(data)) ? 0 : data.trim().length()) > 0;
    }

    /**
     * Displays the Range Values on Edit for Attribute Type of dependency.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds a HashMap containing the following entries:
     * @param paramMap
     *            hold a HashMap containing the following keys, "objectId"
     * @return HashMap contains actual and display values
     * @throws Exception
     *             if operation fails
     * @since ECM R211
     */
    public HashMap displayPrerequisiteTypeValues(Context context, String[] args) throws Exception {
        String strLanguage = context.getSession().getLanguage();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get(ChangeConstants.PARAM_MAP);
        String ChangeObjectId = (String) paramMap.get(ChangeConstants.OBJECT_ID);

        StringList dependencyTypes = FrameworkUtil.getRanges(context, ChangeConstants.ATTRIBUTE_PREREQUISITE_TYPE);
        HashMap rangeMap = new HashMap();
        StringList listChoices = new StringList();
        StringList listDispChoices = new StringList();

        String attrValue = "";
        String dispValue = "";

        for (Iterator dependencyItr = dependencyTypes.iterator(); dependencyItr.hasNext();) {
            attrValue = dependencyItr.next().toString();
            dispValue = i18nNow.getRangeI18NString(ChangeConstants.ATTRIBUTE_PREREQUISITE_TYPE, attrValue, strLanguage);
            listDispChoices.add(dispValue);
            listChoices.add(attrValue);
        }

        rangeMap.put("field_choices", listChoices);
        rangeMap.put("field_display_choices", listDispChoices);

        return rangeMap;
    }

    /**
     * Updates the Type of Dependency attribute value based on user selection.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds a HashMap containing the following entries:
     * @param paramMap
     *            - a HashMap containing the following keys, "relId","RequestedChange"
     * @return int 0 for success or 1 for failure
     * @throws Exception
     *             if operation fails
     * @since ECM R211
     */
    public int updatePrerequisiteTypeValues(Context context, String[] args) throws Exception {
        String strLanguage = context.getSession().getLanguage();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get(ChangeConstants.PARAM_MAP);

        try {
            String sRelId = (String) paramMap.get(ChangeConstants.SELECT_REL_ID);
            String newTypeOfDependency = (String) paramMap.get(ChangeConstants.NEW_VALUE);

            DomainRelationship.newInstance(context, sRelId).setAttributeValue(context, ChangeConstants.ATTRIBUTE_PREREQUISITE_TYPE, newTypeOfDependency);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 1;
        }
        return 0;
    }

    /**
     * Exclude the Changes which leads to deadlock while connecting prerequisites
     * @author
     * @param context
     * @param argv
     * @return StringList
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludePrerequisites(Context context, String[] argv) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(argv);
        String strObjectId = (String) programMap.get(ChangeConstants.OBJECT_ID);
        return new ChangeManagement(strObjectId).excludeParentAndChilds(context, ChangeConstants.RELATIONSHIP_PREREQUISITE);

    }

    /**
     * Fetches the objects which are subscribed by the context user
     * @author V1V
     * @param context
     * @param argv
     * @return MapList
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAllSubscribedObjects(Context context, String[] args) throws Exception {

        MapList mlObjectsList = null;
        MapList mlReturnList = new MapList();
        HashSet objSet = new HashSet();
        String strObjId = "";
        Map tempMap = null;

        try {

            // Getting the person object
            DomainObject personObj = PersonUtil.getPersonObject(context);

            // Relationship Pattern
            String relPattern = ChangeConstants.RELATIONSHIP_SUBSCRIBED_PERSON + "," + ChangeConstants.RELATIONSHIP_PUBLISH + ChangeConstants.COMMA_SEPERATOR
                    + ChangeConstants.RELATIONSHIP_PUBLISH_SUBSCRIBE;
            // Type Pattern
            String typePattern = ChangeConstants.TYPE_EVENT + ChangeConstants.COMMA_SEPERATOR + ChangeConstants.TYPE_PUBLISH_SUBSCRIBE + ChangeConstants.COMMA_SEPERATOR
                    + ChangeConstants.TYPE_CHANGE_ACTION + ChangeConstants.COMMA_SEPERATOR + ChangeConstants.TYPE_CHANGE_ORDER + ChangeConstants.COMMA_SEPERATOR + ChangeConstants.TYPE_CHANGE_REQUEST;

            // Getting all the objects subscribed by the context user
            mlObjectsList = personObj.getRelatedObjects(context, relPattern, typePattern, true, false, (short) 3, new StringList(DomainConstants.SELECT_ID), null, DomainConstants.EMPTY_STRING,
                    DomainConstants.EMPTY_STRING, 0, ChangeConstants.RELATIONSHIP_PUBLISH_SUBSCRIBE,
                    ChangeConstants.TYPE_CHANGE_ACTION + ChangeConstants.COMMA_SEPERATOR + ChangeConstants.TYPE_CHANGE_ORDER + ChangeConstants.COMMA_SEPERATOR + ChangeConstants.TYPE_CHANGE_REQUEST,
                    null);

            Iterator it = mlObjectsList.iterator();

            while (it.hasNext()) {

                Map objectMap = (Map) it.next();
                strObjId = (String) objectMap.get(DomainConstants.SELECT_ID);
                // Getting unique object ids
                objSet.add(strObjId);

            }

            Iterator iterator = objSet.iterator();

            while (iterator.hasNext()) {

                tempMap = new HashMap();
                tempMap.put(SELECT_ID, (String) iterator.next());
                // Adding unique object ids to be returned
                mlReturnList.add(tempMap);
            }

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            throw new FrameworkException(e);
        }

        // Object Id Maplist
        return mlReturnList;

    }

    /**
     * Displays Edit and Delete link (Quick Actions) under Change Subscriptions of My Changes View
     * @author V1V
     * @param context
     * @param argv
     * @return Vector
     * @throws Exception
     */
    public Vector showChangeSubscriptionsQuickActions(Context context, String[] args) throws Exception {
        // XSSOK
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        Vector vecReturn = new Vector(objectList.size());
        String strCommand = PropertyUtil.getSchemaProperty(context, "command_ECMChangeSubscriptions");

        try {

            Map tempMap = null;
            String strSubscriptionEditEvents = "";
            String strSubscriptionDeleteEvents = "";
            String strObjectId = "";

            // Iterating each object
            for (Iterator itrObjectList = objectList.iterator(); itrObjectList.hasNext();) {

                StringBuffer sb = new StringBuffer();
                tempMap = (Map) itrObjectList.next();
                strObjectId = (String) tempMap.get(DomainConstants.SELECT_ID);

                // Setting Edit Link
                strSubscriptionEditEvents = "<a target=\"listHidden\" href=\"javascript:showModalDialog('../components/emxSubscriptionDialog.jsp?objectId="
                        + XSSUtil.encodeForHTMLAttribute(context, strObjectId) + "&amp;submitAction=refreshCaller')\"><img border='0' src='../common/images/iconSmallSignature.gif'/></a>";
                sb.append(strSubscriptionEditEvents);

                sb.append("\n");
                sb.append("\n");

                // Setting Delete Link
                strSubscriptionDeleteEvents = "<a href=\"javascript:submitWithCSRF('../enterprisechangemgt/ECMDisconnectProcess.jsp?objectId=" + XSSUtil.encodeForHTMLAttribute(context, strObjectId)
                        + "&amp;functionality=deleteSubscriptions&amp;commandName=" + XSSUtil.encodeForHTMLAttribute(context, strCommand)
                        + "',getTopWindow().findFrame(getTopWindow(),'listHidden'))\"><img border='0' src='../common/images/iconActionDelete.gif'/></a>";
                sb.append(strSubscriptionDeleteEvents);

                // Adding quick action for each row
                vecReturn.add(sb.toString());

            }

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            throw new FrameworkException(e);
        }

        return vecReturn;

    }

    /**
     * Displays Events subscribed by the context user for a particular object
     * @author V1V
     * @param context
     * @param argv
     * @return Vector
     * @throws Exception
     */
    public Vector showSubscribedEvents(Context context, String[] args) throws Exception {
        // XSSOK
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        Vector vecReturn = new Vector(objectList.size());
        DomainObject domObj = null;

        try {

            Map tempMap = null;
            String strObjectId = "";

            // Getting the business id of context person
            String strPersonId = PersonUtil.getPersonObject(context).getInfo(context, DomainConstants.SELECT_ID);

            // Iterating each object
            for (Iterator itrObjectList = objectList.iterator(); itrObjectList.hasNext();) {

                StringBuffer sb = new StringBuffer();
                StringList slEventSelects = new StringList();
                tempMap = (Map) itrObjectList.next();
                strObjectId = (String) tempMap.get(DomainConstants.SELECT_ID);
                String strEventId = "";
                String strPerson = "";
                String strEventName = "";
                String strEventLabel = "";

                StringList slPersonList = new StringList();

                domObj = DomainObject.newInstance(context, strObjectId);

                // Object Select List
                slEventSelects.addElement(DomainConstants.SELECT_ID);
                slEventSelects.addElement("attribute[" + ChangeConstants.ATTRIBUTE_EVENT_TYPE + "]");

                // Relationship Pattern
                String relPattern = ChangeConstants.RELATIONSHIP_PUBLISH_SUBSCRIBE + ChangeConstants.COMMA_SEPERATOR + ChangeConstants.RELATIONSHIP_PUBLISH;
                // Type Pattern
                String typePattern = ChangeConstants.TYPE_PUBLISH_SUBSCRIBE + ChangeConstants.COMMA_SEPERATOR + ChangeConstants.TYPE_EVENT;

                // Fetching all the events attached to this object
                MapList mlObjectsList = domObj.getRelatedObjects(context, relPattern, typePattern, false, true, (short) 2, slEventSelects, null, DomainConstants.EMPTY_STRING,
                        DomainConstants.EMPTY_STRING, 0, ChangeConstants.RELATIONSHIP_PUBLISH, ChangeConstants.TYPE_EVENT, null);

                Iterator it = mlObjectsList.iterator();

                while (it.hasNext()) {

                    Map objectMap = (Map) it.next();

                    strEventId = (String) objectMap.get(DomainConstants.SELECT_ID);
                    // Get the person list for each event
                    slPersonList = DomainObject.newInstance(context, strEventId).getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_SUBSCRIBED_PERSON + "].to.id");

                    // Check if context person has subscribed to above event
                    if (slPersonList.contains(strPersonId)) {

                        strEventName = (String) objectMap.get("attribute[" + ChangeConstants.ATTRIBUTE_EVENT_TYPE + "]");
                        strEventLabel = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(),
                                "EnterpriseChangeMgt.Event." + strEventName.replace(" ", "_"));

                        sb.append("o");
                        sb.append("\n");
                        sb.append(XSSUtil.encodeForHTML(context, strEventLabel));
                        sb.append("<br/>");
                    }

                }

                // Adding events for each row
                vecReturn.add(sb.toString());

            }

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            throw new FrameworkException(e);
        }

        return vecReturn;

    }

    /**
     * Sends notification when new CA is generated under CO
     * @author V1V
     * @param context
     * @param argv
     * @return void
     * @throws Exception
     */
    public void newCAAddedNotification(Context context, String args[]) throws Exception {

        try {
            // args[] parameters

            String sFromObjId = args[0];
            String sFromObject = args[1];
            String sToObject = args[2];
            String sNotificationName = args[3];

            StringList toList = new StringList();
            StringList objectIdList = new StringList();
            StringList subscribersList = new StringList();

            String sSubject = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(),
                    "EnterpriseChangeMgt.Message.Subject.GenerateNewChangeAction");

            String sMessage = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(),
                    "EnterpriseChangeMgt.Message.Body.GenerateNewChangeAction");

            String sAddedUnder = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Message.isAddedUnder");

            // Checking for Subscribers in CO Object
            subscribersList = SubscriptionUtil.getSubscribersList(context, sFromObjId, sNotificationName, ChangeConstants.OBJECT, true);

            // If there are subscribers for CO Object, create a toList
            if (subscribersList.size() != 0) {

                String strSubscriberDetails = "";
                String strSubscribersName = "";

                for (Iterator iterator = subscribersList.iterator(); iterator.hasNext();) {

                    strSubscriberDetails = (String) iterator.next();
                    strSubscribersName = strSubscriberDetails.substring(0, strSubscriberDetails.indexOf("|"));

                    // Adding Subscribed persons to the list
                    toList.addElement(strSubscribersName);

                }

                // Creating the Subject and Message for Icon Mail
                String sCOSubject = sSubject + sFromObject;
                String sCOMessage = sMessage + sToObject + " " + sAddedUnder + sFromObject;

                // Adding CO Id
                objectIdList.add(sFromObjId);

                // Notify all Subscribers of CO
                emxMailUtil_mxJPO.sendNotification(context, toList, null, null, sCOSubject, null, null, sCOMessage, null, null, objectIdList, null, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR);
            }
        } catch (Exception ex) {
            throw ex;
        }

    }

    /**
     * Sends notification when CO Change Coordinator is assigned
     * @author V1V
     * @param context
     * @param argv
     * @return void
     * @throws Exception
     */
    public void changeCoordinatorAddedNotification(Context context, String args[]) throws Exception {

        try {
            // args[] parameters
            String sFromObjId = args[0];
            String sFromObject = args[1];
            String sToObject = args[2];
            String sNotificationName = args[3];

            StringList toList = new StringList();
            StringList objectIdList = new StringList();
            StringList subscribersList = new StringList();

            String sSubject = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Message.Subject.ModifyCOAttribute");

            String sMessage = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Message.Body.ModifyCOAttribute");

            // Checking for Subscribers in CO Object
            subscribersList = SubscriptionUtil.getSubscribersList(context, sFromObjId, sNotificationName, ChangeConstants.OBJECT, true);

            // If there are subscribers for CO Object, create a toList
            if (subscribersList.size() != 0) {

                String strSubscriberDetails = "";
                String strSubscribersName = "";

                for (Iterator iterator = subscribersList.iterator(); iterator.hasNext();) {

                    strSubscriberDetails = (String) iterator.next();
                    strSubscribersName = strSubscriberDetails.substring(0, strSubscriberDetails.indexOf("|"));

                    // Adding Subscribed persons to the list
                    toList.addElement(strSubscribersName);

                }

                // Creating the Subject and Message for Icon Mail
                String sCOSubject = sSubject + sFromObject;
                String sCOMessage = sToObject + sMessage + sFromObject;

                // Adding CO Object Id
                objectIdList.add(sFromObjId);

                // Notify all Subscribers of CO
                emxMailUtil_mxJPO.sendNotification(context, toList, null, null, sCOSubject, null, null, sCOMessage, null, null, objectIdList, null, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR);
            }
        } catch (Exception ex) {
            throw ex;
        }

    }

    /**
     * Sends notification when Affected Item Added - CO and CA Both
     * @Assumption Only Primary or Secondary Affected Items are added from CO or CA context, respectively In future if Secondary Affected Items are also allowed to be added from CO context, the logic
     *             of the code has to be changed
     * @author V1V
     * @param context
     * @param argv
     * @return void
     * @throws Exception
     */
    public void affectedItemAddedNotification(Context context, String args[]) throws Exception {

        try {

            String strAffectedItemType = "";
            DomainObject dmo = null;
            boolean isRelId = false;
            String strRelId = "";
            String strCAId = "";
            String strobjectId = "";
            String strHistory = "";
            String strId = "";
            String strTemp = "";
            String strTempObjNames = "";
            String strEvent = "";
            String strCAName = "";
            String strType = "";
            StringBuffer sbObjectNames = new StringBuffer();
            StringList toList = new StringList();
            StringList objectIdList = new StringList();
            StringList subscribersList = new StringList();
            StringList sCAid = new StringList();
            StringList slTemp = new StringList();
            StringList slHistoryTemp = new StringList();
            String sTransHistory = args[0];
            String sAffectedItemAddedNotification = args[1];
            Map sCAMap = new HashMap();

            String sSubjectAddedUnder = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(),
                    "EnterpriseChangeMgt.Message.Subject.AffectedItemAddedUnder");

            String sMessageAddedUnder = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(),
                    "EnterpriseChangeMgt.Message.Body.AffectedItemAddedUnder");

            // Parsing the event history
            HashMap transHistoryMap = ChangeSubscription.parseHistory(context, sTransHistory);

            Iterator iterate = transHistoryMap.entrySet().iterator();

            while (iterate.hasNext()) {
                Map.Entry map = (Map.Entry) iterate.next();
                strobjectId = (String) map.getKey();
                strHistory = (String) map.getValue();

                // Validate whether that objectId have some history of events
                if (strHistory != null && !"".equals(strHistory)) {

                    // split the objectid with _ char to get the details of id either objectid or connectionid
                    slTemp = FrameworkUtil.split(strobjectId, "_");
                    strId = (String) slTemp.get(0);
                    strTemp = (String) slTemp.get(1);

                    if (strTemp.equalsIgnoreCase(ChangeConstants.CONNECTION)) {
                        strRelId = strId;
                        strId = MqlUtil.mqlCommand(context, "print connection $1 select $2 $3 $4 dump $5", strId, "from.id", "from.name", "from.type", "|");
                        sCAid = FrameworkUtil.split(strId, "|");
                        strId = (String) sCAid.get(0);
                        strCAName = (String) sCAid.get(1);
                        strType = (String) sCAid.get(2);

                    }

                    slHistoryTemp = FrameworkUtil.split(strHistory, "_");
                    strEvent = (String) slHistoryTemp.get(0);

                    // Check if the object is of Type CA
                    if (ChangeConstants.TYPE_CHANGE_ACTION.equalsIgnoreCase(strType)) {

                        // Fetches the list of objects - to side
                        strTempObjNames = ChangeSubscription.getAffectedObjectsList(context, FrameworkUtil.split(strHistory, "|"));
                        if (!(sbObjectNames.toString()).isEmpty()) {
                            sbObjectNames.append(ChangeConstants.COMMA_SEPERATOR);
                        }
                        sbObjectNames.append(strTempObjNames);
                        strCAId = strId;

                    }
                    if (UIUtil.isNotNullAndNotEmpty(strCAId)) {
                        if (!sCAMap.containsKey(strCAId)) {
                            sCAMap.put(strCAId, strCAName + "|" + strTempObjNames);
                        } else {
                            String tempvalue = (String) sCAMap.get(strCAId);
                            tempvalue += "," + strTempObjNames;
                            sCAMap.put(strCAId, tempvalue);
                        }
                    }

                    if (!isRelId) {

                        if (strTemp.equalsIgnoreCase(ChangeConstants.CONNECTION) && strEvent.equalsIgnoreCase(ChangeConstants.CREATE)) {
                            // Set the rel to newly created id of connection
                            // Considering that only Primary or Secondary Affected Items are added from CO or CA context, respectively
                            isRelId = true;

                        }
                    }
                }
            }
            if (!sCAMap.isEmpty()) {
                Iterator iterator = sCAMap.keySet().iterator();

                String value;
                while (iterator.hasNext()) {
                    strCAId = (String) iterator.next();
                    value = (String) sCAMap.get(strCAId);

                    sCAid = FrameworkUtil.split(value, "|");
                    strCAName = (String) sCAid.get(0);
                    strTempObjNames = (String) sCAid.get(1);

                    sendNotificationforAffectedItemsAdded(context, strCAId, sAffectedItemAddedNotification, sSubjectAddedUnder, sMessageAddedUnder, strCAName, strTempObjNames);
                }
            }

            if (!strRelId.isEmpty()) {

                // Getting the list of objects added
                String strObjectNames = (!ChangeUtil.isNullOrEmpty(sbObjectNames.toString())) ? sbObjectNames.toString().substring(0, sbObjectNames.length() - 1) : "";

                StringList slParentSelects = new StringList();
                slParentSelects.addElement(SELECT_NAME);
                slParentSelects.addElement(SELECT_ID);

                // Fetching the Parent Object Details
                MapList mParentObj = dmo.newInstance(context, strCAId).getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, DomainConstants.QUERY_WILDCARD, slParentSelects, null,
                        true, false, (short) 1, null, null, (short) 0);

                if (!mParentObj.isEmpty()) {
                    Iterator iter = mParentObj.iterator();
                    while (iter.hasNext()) {
                        Map map = (Map) iter.next();
                        String strParentObjId = (String) map.get(SELECT_ID);
                        String strParentObjName = (String) map.get(SELECT_NAME);
                        sendNotificationforAffectedItemsAdded(context, strParentObjId, sAffectedItemAddedNotification, sSubjectAddedUnder, sMessageAddedUnder, strParentObjName, strObjectNames);
                    }
                }
            }

        } catch (Exception ex) {
            throw ex;
        }

    }

    /**
     * Sends notification when Affected Item Removed - CO and CA Both * @Assumption Only Primary or Secondary Affected Items are removed from CO or CA context, respectively In future if Primary
     * Affected Items are also allowed to be removed from CA context, the logic of the code has to be changed
     * @author V1V
     * @param context
     * @param argv
     * @return void
     * @throws Exception
     */
    public int affectedItemRemovedNotification(Context context, String args[]) throws Exception {

        try {

            String sFromObject = args[0];
            String sFromObjId = args[1];
            String sToObject = args[2];
            String sToId = args[3];
            String sRelId = args[4];
            String sNotificationName = args[5];

            StringList toList = new StringList();
            StringList subscribersList = new StringList();
            DomainObject dmo = null;
            Map sCAMap = new HashMap();

            sCAMap.put(SELECT_NAME, sFromObject);
            sCAMap.put(SELECT_ID, sFromObjId);

            String sSubject = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(),
                    "EnterpriseChangeMgt.Message.Subject.AffectedItemRemovedFrom");

            String sMessage = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(),
                    "EnterpriseChangeMgt.Message.Body.AffectedItemRemovedFrom");

            StringList slParentSelects = new StringList();
            slParentSelects.addElement(SELECT_NAME);
            slParentSelects.addElement(SELECT_ID);

            // Fetching the Parent Object Details
            MapList mParentObj = dmo.newInstance(context, sFromObjId).getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, DomainConstants.QUERY_WILDCARD, slParentSelects, null,
                    true, false, (short) 1, null, null, (short) 0);
            ;
            mParentObj.add(sCAMap);
            if (!mParentObj.isEmpty()) {
                Iterator iter = mParentObj.iterator();
                while (iter.hasNext()) {
                    StringList objectIdList = new StringList();
                    Map map = (Map) iter.next();
                    String strParentObjId = (String) map.get(SELECT_ID);
                    String strParentObjName = (String) map.get(SELECT_NAME);

                    // Checking for Subscribers Parent Object - CO
                    subscribersList = SubscriptionUtil.getSubscribersList(context, strParentObjId, sNotificationName, ChangeConstants.OBJECT, true);

                    // If there are subscribers for Parent Object CO, then send email to them
                    if (subscribersList.size() != 0) {
                        String strSubscriberDetails = "";
                        String strSubscribersName = "";

                        for (Iterator iterator = subscribersList.iterator(); iterator.hasNext();) {

                            strSubscriberDetails = (String) iterator.next();
                            strSubscribersName = strSubscriberDetails.substring(0, strSubscriberDetails.indexOf("|"));

                            // Adding Subscribed persons to the list
                            toList.addElement(strSubscribersName);

                        }

                        // Creating the Subject and Message for notification
                        String sCOSubject = sSubject + strParentObjName;
                        String sCOMessage = sToObject + " " + sMessage + strParentObjName;

                        // Adding Parent Object Id - CO
                        objectIdList.add(strParentObjId);

                        // Notify all Subscribers of CO
                        emxMailUtil_mxJPO.sendNotification(context, toList, null, null, sCOSubject, null, null, sCOMessage, null, null, objectIdList, null,
                                ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR);

                    }
                }
            }

        } catch (Exception ex) {
            throw ex;
        }

        return 0;
    }

    /**
     * Method to get connected CAs from CR and CO tabs from ChangeManagement from Application types(eg : Part,feature..)
     * @param context
     * @param args
     * @return MapList
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getConnectedChanges(Context context, String[] args) throws Exception {
        MapList totalRelatedListCAs = new MapList();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String strPartId = (String) paramMap.get(ChangeConstants.OBJECT_ID);
        String functionality = (String) paramMap.get("functionality");

        StringList stlObjectSelects = new StringList();
        stlObjectSelects.addElement(SELECT_ID);
        String changeRequestSelect = "evaluate[to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + ChangeConstants.TYPE_CHANGE_REQUEST + "].id]";
        String changeOrderSelect = "evaluate[to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from[" + ChangeConstants.TYPE_CHANGE_ORDER + "].id]";
        stlObjectSelects.addElement(changeRequestSelect);
        stlObjectSelects.addElement(changeOrderSelect);
        stlObjectSelects.addElement(SELECT_CURRENT);
        DomainObject newPart = new DomainObject(strPartId);
        Set changeActions = new HashSet();

        StringBuffer relPattern = new StringBuffer(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
        relPattern.append(",").append(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);
        MapList totalresultList = newPart.getRelatedObjects(context, relPattern.toString(), ChangeConstants.TYPE_CHANGE_ACTION, stlObjectSelects, new StringList(SELECT_RELATIONSHIP_ID), true, false,
                (short) 1, null, null, 0);
        Iterator itr = totalresultList.iterator();
        String sCAId, changeId, current;
        String changeSelect;
        changeSelect = "isChangeRequestTab".equalsIgnoreCase(functionality) ? changeRequestSelect : changeOrderSelect;
        while (itr.hasNext()) {
            Map mplcaObject = (Map) itr.next();
            sCAId = (String) mplcaObject.get(SELECT_ID);
            changeId = (String) mplcaObject.get(changeSelect);
            current = (String) mplcaObject.get(SELECT_CURRENT);
            if (UIUtil.isNotNullAndNotEmpty(changeId) && !changeActions.contains(sCAId)) {
                if (!ChangeConstants.STATE_CHANGE_ACTION_PENDING.equalsIgnoreCase(current))
                    mplcaObject.put("disableSelection", "true");
                totalRelatedListCAs.add(mplcaObject);
                changeActions.add(sCAId);
            }
        }
        return totalRelatedListCAs;
    }

    /**
     * Method to get all connected Change objects except CO and CA for context object
     * @param context
     * @param args
     * @return MapList
     */

    public MapList getConnectedLegacyObjects(Context context, String[] args) throws Exception {
        MapList mplRelatedObjects = new MapList();
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strObjectId = (String) programMap.get("objectId");

        StringList objectSelects = new StringList();
        objectSelects.add(DomainObject.SELECT_ID);
        objectSelects.add(DomainObject.SELECT_TYPE);

        DomainObject partObject = new DomainObject(strObjectId);
        MapList mplAllRelatedObjects = partObject.getRelatedObjects(context, QUERY_WILDCARD, PropertyUtil.getSchemaProperty(context, "type_Change"), objectSelects, null, true, false, (short) 0, null,
                null, 0);

        for (int i = 0; i < mplAllRelatedObjects.size(); i++) {
            Hashtable ObjectMap = (Hashtable) mplAllRelatedObjects.get(i);
            String strObjType = (String) ObjectMap.get(DomainObject.SELECT_TYPE);
            if (!(strObjType.equals(ChangeConstants.TYPE_CHANGE_ACTION) || strObjType.equals(ChangeConstants.TYPE_CHANGE_ORDER))) {
                mplRelatedObjects.add(ObjectMap);
            }
        }

        return mplRelatedObjects;
    }

    /**
     * Method returns true if context object type and policy are registered as change type and policy
     * @param context
     * @param args
     * @return boolean
     */

    public boolean displayConnectedCACOLegacyObjects(Context context, String[] args) throws Exception {
        HashMap programMap = JPO.unpackArgs(args);
        String strObjId = (String) programMap.get("objectId");
        DomainObject newObject = DomainObject.newInstance(context, strObjId);

        StringList objectSelect = new StringList(2);
        objectSelect.add(DomainConstants.SELECT_TYPE);
        objectSelect.add(DomainConstants.SELECT_POLICY);

        Map infoMap = newObject.getInfo(context, objectSelect);
        String type = (String) infoMap.get(DomainConstants.SELECT_TYPE);
        String policy = (String) infoMap.get(DomainConstants.SELECT_POLICY);

        String configuredPartPolicy = PropertyUtil.getSchemaProperty(context, "policy_ConfiguredPart");

        return ((!configuredPartPolicy.equals(policy)) && ECMAdmin.isRegisteredTypePolicy(context, type, policy));
    }

    /**
     * Shows status on CO/CR/CA Properties page.
     * @param context
     * @param args
     * @return String
     * @throws Exception
     * @since R216 ECM
     */
    public String showChangeStatusOnProperties(Context context, String[] args) throws Exception {
        // XSSOK
        String statusString = "";
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) paramMap.get(ChangeConstants.REQUEST_MAP);
            String objectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);

            // For export to CSV
            String exportFormat = null;
            boolean exportToExcel = false;
            if (requestMap != null && requestMap.containsKey("reportFormat")) {
                exportFormat = (String) requestMap.get("reportFormat");
            }
            if ("CSV".equals(exportFormat)) {
                exportToExcel = true;
            }

            ChangeManagement change = new ChangeManagement(objectId);
            // The new parameter is added for IR-453887-3DEXPERIENCER2015X
            statusString = change.showStatusIcon(context, objectId, exportToExcel);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new FrameworkException(ex);
        }
        return statusString;
    }

    /**
     * Program to get Approval Status For CO/CR/CA
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds the following input arguments: 0 - Object
     * @return a <code>MapList</code> object having the list of Assignees,their relIds and rel names for this Change Object
     * @throws Exception
     *             if the operation fails
     * @since ECM R211
     */
    public Vector showChangeStatus(Context context, String args[]) throws Exception {
        // XSSOK
        Vector columnVals = null;
        String objectId = "";
        String statusString = "";
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map paramMap = (Map) programMap.get("paramList");
            MapList objectList = (MapList) programMap.get(ChangeConstants.OBJECT_LIST);

            // For export to CSV
            String exportFormat = null;
            boolean exportToExcel = false;
            if (paramMap != null && paramMap.containsKey("reportFormat")) {
                exportFormat = (String) paramMap.get("reportFormat");
            }
            if ("CSV".equals(exportFormat)) {
                exportToExcel = true;
            }

            StringList sObjectIDList = changeUtil.getStringListFromMapList(objectList, ChangeConstants.ID);

            if (objectList == null || objectList.size() == 0) {
                return columnVals;
            } else {
                columnVals = new Vector(sObjectIDList.size());
            }
            for (int i = 0; i < sObjectIDList.size(); i++) {
                objectId = (String) sObjectIDList.get(i);
                ChangeManagement change = new ChangeManagement(objectId);
                statusString = change.showStatusIcon(context, objectId, exportToExcel);
                columnVals.add(statusString);
            }
            return columnVals;

        } catch (Exception e) {
            throw new FrameworkException(e);
        }
    }// end of method

    /**
     * Access program for 3DLive Examine channel in CO/CR affected items page
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Boolean showAffectedItem3DChannelCOCR(Context context, String[] args) throws Exception {

        return ChangeUtil.isReportedAgainstItemPart(context, args);

    }

    /**
     * Access program for 3DLive Examine channel in affected items page
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Boolean showAffectedItem3DChannel(Context context, String[] args) throws Exception {

        String pref3DLive = PropertyUtil.getAdminProperty(context, "Person", context.getUser(), "preference_3DLiveExamineToggle");
        boolean flag = "Show".equals(pref3DLive);

        if (flag) {
            return ChangeUtil.isReportedAgainstItemPart(context, args);
        }
        return flag;
    }

    /**
     * Method to check 3D command enable
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Boolean show3DToggleCommand(Context context, String[] args) throws Exception {

        String pref3DLive = PropertyUtil.getAdminProperty(context, "Person", context.getUser(), "preference_3DLiveExamineToggle");
        boolean flag = ("Hide".equals(pref3DLive) || "".equals(pref3DLive));

        if (flag) {
            return ChangeUtil.isReportedAgainstItemPart(context, args);
        }

        return flag;
    }

    /**
     * Method to check 3D command disable
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Boolean hide3DToggleCommand(Context context, String[] args) throws Exception {

        String pref3DLive = PropertyUtil.getAdminProperty(context, "Person", context.getUser(), "preference_3DLiveExamineToggle");
        boolean flag = "Show".equals(pref3DLive);

        if (flag) {
            return ChangeUtil.isReportedAgainstItemPart(context, args);
        }

        return flag;

    }

    // #############################START OF ECM WIDGETS#############################

    /**
     * Method to get Change Requests/Change Orders objects for widgets
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */

    public MapList getMyChangesForWidget(Context context, String[] args) throws Exception {
        try {
            Map<String, Object> programMap = (Map<String, Object>) JPO.unpackArgs(args);
            Map jpoArgs = (Map) programMap.get(ServiceBase.JPO_ARGS);
            String selectedTypes = (String) jpoArgs.get(ChangeConstants.TYPE);
            String widgetName = (String) jpoArgs.get("widgetName");
            ChangeWidgets changeWidgets = new ChangeWidgets();
            if ("CRCOs".equalsIgnoreCase(widgetName))
                return changeWidgets.getMyChangeRequestsAndChangeOrdersForWidget(context, selectedTypes);
            if ("CAs".equalsIgnoreCase(widgetName))
                return changeWidgets.getMyChangeActionsForWidget(context, selectedTypes);
            if ("legacyChanges".equalsIgnoreCase(widgetName))
                return changeWidgets.getMyLegacyChangesForWidget(context, selectedTypes);
            if ("manufacturingChanges".equalsIgnoreCase(widgetName))
                return changeWidgets.getMyManufacturingChangesForWidget(context, selectedTypes);
        } catch (Exception widgetException) {
            System.out.println("Exception occurred in enoECMChangeUtil : getMyChangesForWidget");
            widgetException.printStackTrace();
        }
        return new MapList();
    }

    /**
     * Returns the label of Change Actions
     * @param context
     * @param args
     * @throws Exception
     */
    public void getChangeActionsCountAndLabel(Context context, String[] args) throws Exception {

        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String fieldKey = (String) programMap.get(ServiceBase.JPO_WIDGET_FIELD_KEY);
            MapList objectList = (MapList) programMap.get(ServiceBase.JPO_WIDGET_DATA);
            Map<String, String> widgetArgs = (Map<String, String>) programMap.get(ServiceBase.JPO_WIDGET_ARGS);
            String baseURI = widgetArgs.get(ServiceBase.ARG_BASE_URI);
            Map typeCategoryMap = new HashMap();
            typeCategoryMap.put(ChangeConstants.TYPE_CHANGE_ORDER, "ECMChangeContent");
            typeCategoryMap.put(ChangeConstants.TYPE_CHANGE_REQUEST, "ECMChangeContent");
            new ChangeWidgets().getHTMLForCountAndLabel(context, fieldKey, objectList, "EnterpriseChangeMgt.Widget.ChangeActionsLabel", "EnterpriseChangeMgt.Widget.ChangeActionsMouseOverText",
                    baseURI);
        } catch (Exception label) {
            System.out.println("Exception occurred in getChangeActionLabel");
            label.printStackTrace();
        }
    }

    /**
     * Returns the label of Affected items
     * @param context
     * @param args
     * @throws Exception
     */
    public void getAffectedItemsCountAndLabel(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String fieldKey = (String) programMap.get(ServiceBase.JPO_WIDGET_FIELD_KEY);
            MapList objectList = (MapList) programMap.get(ServiceBase.JPO_WIDGET_DATA);
            Map<String, String> widgetArgs = (Map<String, String>) programMap.get(ServiceBase.JPO_WIDGET_ARGS);
            String baseURI = widgetArgs.get(ServiceBase.ARG_BASE_URI);
            Map typeCategoryMap = new HashMap();
            typeCategoryMap.put(ChangeConstants.TYPE_CHANGE_ACTION, "ECMCAAffectedItems");
            new ChangeWidgets().getHTMLForCountAndLabel(context, fieldKey, objectList, "EnterpriseChangeMgt.Widget.AffectedItems", "EnterpriseChangeMgt.Widget.AffectedItemsMouseOverText", baseURI);
        } catch (Exception label) {
            System.out.println("Exception occurred in getAffectedItemsLabel");
            label.printStackTrace();
        }
    }

    /**
     * Returns the label of Affected items
     * @param context
     * @param args
     * @throws Exception
     */
    public void getAffectedItemsCountAndLabelForLegacyAndMFGChanges(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String fieldKey = (String) programMap.get(ServiceBase.JPO_WIDGET_FIELD_KEY);
            MapList objectList = (MapList) programMap.get(ServiceBase.JPO_WIDGET_DATA);
            Map<String, String> widgetArgs = (Map<String, String>) programMap.get(ServiceBase.JPO_WIDGET_ARGS);
            String baseURI = widgetArgs.get(ServiceBase.ARG_BASE_URI);
            new ChangeWidgets().getHTMLForCountAndLabel(context, fieldKey, objectList, "EnterpriseChangeMgt.Widget.AffectedItems", "EnterpriseChangeMgt.Widget.AffectedItemsMouseOverText", baseURI);
        } catch (Exception label) {
            System.out.println("Exception occurred in getAffectedItemsLabel");
            label.printStackTrace();
        }
    }

    /**
     * Returns the category cammand of Affected items
     * @param context
     * @param args
     * @throws Exception
     */
    public void getAffectedItemsDefaultCommandForLegacyAndMFGChanges(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String fieldKey = (String) programMap.get(ServiceBase.JPO_WIDGET_FIELD_KEY);
            MapList objectList = (MapList) programMap.get(ServiceBase.JPO_WIDGET_DATA);
            Map typeCategoryMap = getAffectedItemCategoryPerChange();
            new ChangeWidgets().getDefaultCommandForLegacyAndMFGChanges(context, fieldKey, objectList, typeCategoryMap);
        } catch (Exception label) {
            System.out.println("Exception occurred in getAffectedItemsLabel");
            label.printStackTrace();
        }
    }

    /**
     * Returns the Progress status of CO/CRs
     * @param context
     * @param args
     * @throws Exception
     */
    public void getProgressStatusForWidgets(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String fieldKey = (String) programMap.get(ServiceBase.JPO_WIDGET_FIELD_KEY);
            MapList objectList = (MapList) programMap.get(ServiceBase.JPO_WIDGET_DATA);
            new ChangeWidgets().getProgressStatus(context, fieldKey, objectList);
        } catch (Exception progress) {
            System.out.println("Exception occurred in getProgressStatusForWidgets");
            progress.printStackTrace();
        }
    }

    /**
     * Returns the Progress status of CAs
     * @param context
     * @param args
     * @throws Exception
     */
    public void getProgressStatusForChangeActionWidgets(Context context, String[] args) throws Exception {
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String fieldKey = (String) programMap.get(ServiceBase.JPO_WIDGET_FIELD_KEY);
            MapList objectList = (MapList) programMap.get(ServiceBase.JPO_WIDGET_DATA);
            new ChangeWidgets().getProgressStatusForChangeAction(context, fieldKey, objectList);
        } catch (Exception progress) {
            System.out.println("Exception occurred in getProgressStatusForChangeAction");
            progress.printStackTrace();
        }
    }

    /**
     * Returns the Badge status COs/CRs
     * @param context
     * @param args
     * @throws Exception
     */
    public void getBadgeStatusForWidgets(Context context, String[] args) throws Exception {
        try {
            Map<String, Object> map = (Map<String, Object>) JPO.unpackArgs(args);
            MapList ml = (MapList) map.get("JPO_WIDGET_DATA");
            String fieldKey = (String) map.get("JPO_WIDGET_FIELD_KEY");
            new ChangeWidgets().getBadgeStatusForWidgets(context, fieldKey, ml);
        } catch (Exception badge) {
            System.out.println("Exception occurred in getBadgeStatusForWidgets");
            badge.printStackTrace();
        }
    }

    /**
     * Returns the Badge status COs/CRs
     * @param context
     * @param args
     * @throws Exception
     */
    public void getBadgeStatusForChangeActionWidgets(Context context, String[] args) throws Exception {
        try {
            Map<String, Object> map = (Map<String, Object>) JPO.unpackArgs(args);
            MapList ml = (MapList) map.get("JPO_WIDGET_DATA");
            String fieldKey = (String) map.get("JPO_WIDGET_FIELD_KEY");
            new ChangeWidgets().getBadgeStatusForChangeAction(context, fieldKey, ml);
        } catch (Exception badge) {
            System.out.println("Exception occurred in getBadgeStatusForChangeActionWidgets");
            badge.printStackTrace();
        }
    }

    /**
     * Gets the Affected Item Category for legacy changes.
     * @param context
     *            - the eMatrix <code>Context</code> object
     * @return the same map list provided by the widget - MapList
     * @throws Exception
     *             if the operation fails
     **/
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map getAffectedItemCategoryPerChange() {
        HashMap categoryMap = new HashMap();
        categoryMap.put("ECO", "msCategory");
        categoryMap.put("ECR", "msCategory");
        categoryMap.put("DECO", "msCategory");
        categoryMap.put("PUE ECO", "PUEECOAffectedItemsTreeCategory");
        categoryMap.put("Configured Change Action", "PUEECOAffectedItemsTreeCategory");
        categoryMap.put("MECO", "MBOMMECOAffectedItemsTreeCategory");
        categoryMap.put("DCR", "MBOMDCRAffectedItemsTreeCategory");
        categoryMap.put("MCO", "MBOMAffectedPartRevisions");

        return categoryMap;
    }

    // ############################# ENG OF ECM WIDGETS#############################
    /**
     * This method is used as access function for Governing CO column in CR affected items page.
     * @param context
     * @param args
     * @return True or False
     * @throws Exception
     */
    public boolean isChangeRequestTab(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) paramMap.get(ChangeConstants.REQUEST_MAP);
        String isChangeRequestTab = (String) paramMap.get("functionality");

        return "isChangeRequestTab".equalsIgnoreCase(isChangeRequestTab) ? new Boolean(true) : new Boolean(false);

    }

    /**
     * This method is used as access function for Governing CO column in CR affected items page.
     * @param context
     * @param args
     * @return True or False
     * @throws Exception
     */
    public boolean isChangeOrderTab(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) paramMap.get(ChangeConstants.REQUEST_MAP);
        String isChangeOrderTab = (String) paramMap.get("functionality");

        return "isChangeOrderTab".equalsIgnoreCase(isChangeOrderTab) ? new Boolean(true) : new Boolean(false);

    }

    /**
     * This method is used to send Notification while adding affected items to Change.
     * @param context
     * @param args
     */
    public void sendNotificationforAffectedItemsAdded(Context context, String strParentObjId, String sAffectedItemAddedNotification, String sSubjectAddedUnder, String sMessageAddedUnder,
            String strParentObjName, String strObjectNames) throws Exception {

        StringList subscribersList = new StringList();
        StringList toList = new StringList();
        StringList objectIdList = new StringList();
        subscribersList = SubscriptionUtil.getSubscribersList(context, strParentObjId, sAffectedItemAddedNotification, ChangeConstants.OBJECT, true);
        if (subscribersList.size() != 0) {
            String strSubscriberDetails = "";
            String strSubscribersName = "";

            for (Iterator iterator = subscribersList.iterator(); iterator.hasNext();) {

                strSubscriberDetails = (String) iterator.next();
                strSubscribersName = strSubscriberDetails.substring(0, strSubscriberDetails.indexOf("|"));

                // Adding Subscribed persons to the list
                toList.addElement(strSubscribersName);

            }

            // Creating the Subject and Message for notification
            String sCOSubject = sSubjectAddedUnder + strParentObjName;
            String sCOMessage = strObjectNames + sMessageAddedUnder + strParentObjName;

            // Adding Parent Object Id - CO
            objectIdList.add(strParentObjId);

            // Notify all Subscribers of CO
            emxMailUtil_mxJPO.sendNotification(context, toList, null, null, sCOSubject, null, null, sCOMessage, null, null, objectIdList, null, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR);

        }
    }
}// end of class
