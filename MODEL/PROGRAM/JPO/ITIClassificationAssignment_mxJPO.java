
/*
 ** ITIClassificationAssignment
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** This program is used for MCAD model classification
 */

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;
import java.util.Iterator;
import java.util.Date;
import java.io.StringReader;
import java.io.Reader;
import java.util.ListIterator;
import java.util.Iterator;
import matrix.db.Context;
import matrix.db.BusinessObject;
import matrix.db.AttributeType;
import matrix.db.MatrixLogWriter;
import matrix.util.MatrixException;
import matrix.util.StringList;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.jdom.Element;
import com.matrixone.jdom.Document;
import com.matrixone.jdom.output.XMLOutputter;
import com.matrixone.apps.classification.ClassificationConstants;
import com.matrixone.apps.classification.Classification;
import com.matrixone.jdom.Element;
import com.matrixone.jdom.Document;
import com.matrixone.jdom.input.SAXBuilder;
// import com.matrixone.jdom.*;
// import com.matrixone.jdom.input.*;
// import com.matrixone.jdom.output.*;

public class ITIClassificationAssignment_mxJPO {

    /**
     * default Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since Sourcing V6R2012x
     */
    public ITIClassificationAssignment_mxJPO() {
        initialize();
    }

    /**
     * 2-arg Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since Sourcing V6R2012x
     */
    public ITIClassificationAssignment_mxJPO(Context context, String args[]) {
        this.context = context;
        initialize();
    }

    public int mxMain(Context context, String[] args) throws Exception {
        try {
            getClassificationAndConfigurationDetails(context, args);
            // preCheckinExClassificationAction(context,args);
            // postCheckinExClassificationAction(context,args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Method is intiated before checkinEx. It creates temporary holder
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds username, caseName and XML
     * @throws Exception
     *             if the operation fails
     * @return String - "SUCCESS" if operatio is successful
     * @since Sourcing V6R2012x
     */
    public String postXMLForClassification(Context context, String[] args) throws Exception {
        logMessage("postXMLForClassification", "STARTED");
        String userName = args[0];
        String cseName = args[1];
        String classificationXML = args[2];
        boolean isContextPushed = false;
        logMessage("postXMLForClassification", "userName " + userName);
        logMessage("postXMLForClassification", "cseName " + cseName);
        logMessage("postXMLForClassification", "classificationXML " + classificationXML);
        String responseXML = "";
        ;
        Element xmlClassificationObjectListNode = new Element("classifiedobjectlist");

        try {
            if (!isContextPushed) {
                logMessage("postXMLForClassification", "Context not pushed");
                // Updated to fix user agent password issue in 15x
                // ContextUtil.pushContext(context, _superUser, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                ContextUtil.pushContext(context);
                logMessage("postXMLForClassification", "Context  pushed successfully");
                isContextPushed = true;
            }

            logMessage("postXMLForClassification", "Performing classification");
            SAXBuilder builder = new SAXBuilder();
            Reader in = new StringReader(classificationXML);
            Document doc = builder.build(in);
            Element classifedObjectListTopNode = doc.getRootElement();
            java.util.List classifiedObjectNodeList = classifedObjectListTopNode.getChildren("classifiedobject");
            logMessage("postXMLForClassification", "Classified object size  :" + classifiedObjectNodeList);
            if (classifiedObjectNodeList == null || (classifiedObjectNodeList != null && classifiedObjectNodeList.size() == 0)) {
                // XML not defined properly
                logMessage("postXMLForClassification", "not classifeidobject found in the xml");
                if (DO_NOT_ALLOW_CHECKIN_IF_NOCLASSIFICATION) {
                    throw new Exception("CAD Model classification details not available");
                } else {
                    XMLOutputter outputterWS = new XMLOutputter();
                    return outputterWS.outputString(xmlClassificationObjectListNode);
                }
            } else {
                ListIterator classifiedObjectItr = classifiedObjectNodeList.listIterator();
                StringList objectSelects = new StringList();
                objectSelects.addElement(DomainConstants.SELECT_ID);
                objectSelects.addElement(DomainConstants.SELECT_REVISION);

                while (classifiedObjectItr.hasNext()) {
                    Element classifiedObjectNode = (Element) classifiedObjectItr.next();
                    String majorObjectName = classifiedObjectNode.getAttributeValue("name");
                    String majorObjectType = classifiedObjectNode.getAttributeValue("majortype");
                    logMessage("postXMLForClassification", "majorObjectName   " + majorObjectName);
                    logMessage("postXMLForClassification", "majorObjectType " + majorObjectType);
                    Element xmlClassificationObjectNode = new Element("classifiedobject");
                    xmlClassificationObjectNode.setAttribute(new com.matrixone.jdom.Attribute("majortype", majorObjectType));
                    xmlClassificationObjectNode.setAttribute(new com.matrixone.jdom.Attribute("name", majorObjectName));

                    MapList objectList = DomainObject.findObjects(context, // Context
                            majorObjectType, // Type
                            majorObjectName, // Name
                            DomainConstants.QUERY_WILDCARD, // Revision
                            DomainConstants.QUERY_WILDCARD, // Owner
                            DomainConstants.QUERY_WILDCARD, // Vault
                            "", // Where expression
                            true, // Expand Type
                            objectSelects); // Selectable

                    // Classification is supported only for the first revision of the object.
                    // So get the first object from objectList
                    if (objectList == null || objectList.size() == 0) {
                        String errMsg = " UG Model " + majorObjectType + " " + majorObjectName + " not found";
                        logMessage("postXMLForClassification", errMsg);
                        // Set result as false
                        xmlClassificationObjectNode.setAttribute(new com.matrixone.jdom.Attribute("itemclassification", "FAILED"));
                        xmlClassificationObjectNode.setAttribute(new com.matrixone.jdom.Attribute("derivedclssification", "FAILED"));
                        xmlClassificationObjectNode.setAttribute(new com.matrixone.jdom.Attribute("reason", errMsg));
                        xmlClassificationObjectListNode.addContent(xmlClassificationObjectNode);
                        continue;
                    }
                    logMessage("postXMLForClassification", "majorObjectList " + objectList);

                    HashMap majorObjectMap = (HashMap) objectList.get(0);
                    String majorObjectId = (String) majorObjectMap.get(DomainObject.SELECT_ID);
                    String majorObjectRevision = (String) majorObjectMap.get(DomainObject.SELECT_REVISION);
                    xmlClassificationObjectNode.setAttribute(new com.matrixone.jdom.Attribute("revision", majorObjectRevision));

                    logMessage("postXMLForClassification", "majorObjectId " + majorObjectId);
                    logMessage("postXMLForClassification", "Clasifying both major and derived output objects");
                    performClassification(classifiedObjectNode, majorObjectId, xmlClassificationObjectNode);
                    logMessage("postXMLForClassification", "Clasifying both major and derived output objects");
                    xmlClassificationObjectListNode.addContent(xmlClassificationObjectNode);
                }
            }
            logMessage("postXMLForClassification", "Classification Done");

            // xmlClassificationObjectListNode.setAttribute(new com.matrixone.jdom.Attribute("result", "SUCCESS"));
        } catch (Exception e) {
            logMessage("postXMLForClassification", "Error while classifying" + e.getMessage());
            // xmlClassificationObjectListNode.setAttribute(new com.matrixone.jdom.Attribute("result", "FAILED"));
            throw e;
        } finally {
            logMessage("postXMLForClassification", "isContextPushed " + isContextPushed);
            if (isContextPushed) {
                logMessage("postXMLForClassification", "Context Popp started");
                ContextUtil.popContext(context);
                logMessage("postXMLForClassification", "Context Pop Done");
                isContextPushed = false;
            }
        }
        XMLOutputter outputterWS = new XMLOutputter();
        return outputterWS.outputString(xmlClassificationObjectListNode);
    }

    /**
     * Method is intiated before checkinEx. It creates temporary holder
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds username, caseName and XML
     * @throws Exception
     *             if the operation fails
     * @return String - "SUCCESS" if operatio is successful
     * @since Sourcing V6R2012x
     */
    public String preCheckinExClassificationAction(Context context, String[] args) throws Exception {
        logMessage("preCheckinExClassificationAction", "STARTED");
        String userName = args[0];
        String cseName = args[1];
        String classificationXML = args[2];
        logMessage("preCheckinExClassificationAction", "userName " + userName);
        logMessage("preCheckinExClassificationAction", "cseName " + cseName);
        logMessage("preCheckinExClassificationAction", "classificationXML " + classificationXML);
        String nameOfHOlder = userName + "-" + cseName;
        logMessage("preCheckinExClassificationAction", "nameOfHOlder " + nameOfHOlder);
        // 1. Check if tempobject with username is existing or not
        // 2. If not existing then create one object and update the XML
        // 3. If existing, update the attribute with the XML
        // 4. finally return "SUCCESS" message to CSE
        MapList objectList = searchForClassificationHolder(nameOfHOlder);
        logMessage("preCheckinExClassificationAction", "objectList " + objectList.toString());
        if (objectList == null || objectList.size() == 0) {
            logMessage("preCheckinExClassificationAction", "No object exist. Creating object");
            BusinessObject object = new BusinessObject(MCAD_CheckinEx_Classification_Holder, nameOfHOlder, SYMBOL_HYPHON, "eService Production");
            object.create(context, MCAD_CLASSIFICATION_POLICY);
            DomainObject domObject = new DomainObject(object);
            // domObject.setDescription(context,classificationXML);
            domObject.setAttributeValue(context, MCADInteg_CLASSIFICATIONXML_RECEIVEDFROMCSE, classificationXML);
            logMessage("preCheckinExClassificationAction", "Object created");

        } else {
            logMessage("preCheckinExClassificationAction", "Object found");
            HashMap objectDetailsMap = (HashMap) objectList.get(0);
            String objectId = (String) objectDetailsMap.get(DomainObject.SELECT_ID);
            DomainObject object = new DomainObject(objectId);
            // object.setDescription(context,classificationXML);
            object.setAttributeValue(context, MCADInteg_CLASSIFICATIONXML_RECEIVEDFROMCSE, classificationXML);
            logMessage("preCheckinExClassificationAction", "Attribute is updatedd");
        }
        logMessage("preCheckinExClassificationAction", "FINISHED");

        return "SUCCESS";
    }

    /**
     * Method is intiated After checkinEx. It deletes temporary holder
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds username and cseName
     * @throws Exception
     *             if the operation fails
     * @return String - "SUCCESS" if operatio is successful
     * @since Sourcing V6R2012x
     */
    public String postCheckinExClassificationAction(Context context, String[] args) throws Exception {
        logMessage("postCheckinExClassificationAction", "STARTED");
        String userName = args[0];
        String cseName = args[1];
        logMessage("postCheckinExClassificationAction", "userName " + userName);
        logMessage("postCheckinExClassificationAction", "cseName " + cseName);
        String nameOfHOlder = userName + "-" + cseName;
        logMessage("postCheckinExClassificationAction", "nameOfHOlder " + nameOfHOlder);
        // 1. Check if tempobject with nameOfHOlder is existing or not
        // 2. If existing then delete the object
        // 4. finally return "SUCCESS" message to CSE
        MapList objectList = searchForClassificationHolder(nameOfHOlder);
        if (objectList == null || objectList.size() == 0) {
            logMessage("postCheckinExClassificationAction", "No object exist.");
            if (DO_NOT_ALLOW_CHECKIN_IF_NOCLASSIFICATION) {
                throw new Exception("CAD Model classification details not available");
            }

        } else {
            logMessage("postCheckinExClassificationAction", "Object found");
            HashMap objectDetailsMap = (HashMap) objectList.get(0);
            String objectId = (String) objectDetailsMap.get(DomainObject.SELECT_ID);
            DomainObject object = new DomainObject(objectId);
            object.delete(context);
            logMessage("postCheckinExClassificationAction", "Object deleted");
        }
        logMessage("postCheckinExClassificationAction", "FINISHED");

        return "SUCCESS";
    }

    public int classifyObjects(Context context, String args[]) throws Exception {
        logMessage("classifyObjects", "STARTED");
        boolean isContextPushed = false;
        int retValue = 1;
        try {
            if (!isContextPushed) {
                logMessage("classifyObjects", "Context not pushed");
                // Updated to fix user agent password issue in 15x
                // ContextUtil.pushContext(context, _superUser, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                ContextUtil.pushContext(context);
                logMessage("classifyObjects", "Context  pushed successfully");
                isContextPushed = true;
            }
            logMessage("classifyObjects", "Performing classification");
            retValue = performClassification(context, args);
            logMessage("classifyObjects", "Classification Done");
        } catch (Exception e) {
            logMessage("classifyObjects", "Error while classifying" + e.getMessage());
            throw e;
        } finally {
            logMessage("classifyObjects", "isContextPushed " + isContextPushed);
            if (isContextPushed) {
                logMessage("classifyObjects", "Context Popp started");
                ContextUtil.popContext(context);
                logMessage("classifyObjects", "Context Pop Done");
                isContextPushed = false;
            }
        }
        logMessage("classifyObjects", "Return value " + retValue);
        logMessage("classifyObjects", "FINISHED");
        return retValue;
    }

    /**
     * Method is to be configured on Major object type Creat event as action trigger for classyfing objects
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds objectId, caseName
     * @throws Exception
     *             if the operation fails
     * @return int - 0 if successful
     * @since Sourcing V6R2012x
     */
    public int performClassification(Context context, String args[]) throws Exception {
        logMessage("performClassification", "STARTED");
        String objectId = args[0];
        String cseName = args[1];
        DomainObject mcadObject = new DomainObject(objectId);
        MapList objectInfo = mcadObject.getBasicInfo(context);
        HashMap basicInfo = (HashMap) objectInfo.get(0);
        String objectOwner = (String) basicInfo.get("owner");
        String objectName = (String) basicInfo.get("name");
        String objectType = (String) basicInfo.get("type");

        logMessage("performClassification", "objectOwner " + objectOwner);
        logMessage("performClassification", "objectName " + objectName);
        logMessage("performClassification", "objectType " + objectType);

        // 1. Search for the holder object and retrieve attribute value
        // IF holder not found, then return
        // if attribute is empty, then return
        // 2. Iterate through the classified object list and locate the mcad object based on type and name
        // 3. if found, Search for the classification, connect with the mcad object, update attributes
        String nameOfHOlder = objectOwner + "-" + cseName;
        logMessage("performClassification", "nameOfHOlder " + nameOfHOlder);
        MapList objectList = searchForClassificationHolder(nameOfHOlder);
        if (objectList == null || objectList.size() == 0) {
            logMessage("performClassification", "no holder objet found ");
            // NO hoder object is found i.e, no classifed details avaiablle
            if (DO_NOT_ALLOW_CHECKIN_IF_NOCLASSIFICATION) {
                throw new Exception("CAD Model classification details not available");
            } else {
                return 0;
            }
        } else {
            logMessage("performClassification", "Holder object is found ");
            HashMap objectDetailsMap = (HashMap) objectList.get(0);
            String holderObjectId = (String) objectDetailsMap.get(DomainObject.SELECT_ID);
            DomainObject holderObject = new DomainObject(holderObjectId);
            // String classifedObjectDetailsInXML = holderObject.getDescription(context);
            String classifedObjectDetailsInXML = holderObject.getAttributeValue(context, MCADInteg_CLASSIFICATIONXML_RECEIVEDFROMCSE);
            logMessage("performClassification", "Classified object details :" + classifedObjectDetailsInXML);
            if (classifedObjectDetailsInXML == null || "".equals(classifedObjectDetailsInXML.trim())) {
                logMessage("performClassification", "Classification information is not available");
                if (DO_NOT_ALLOW_CHECKIN_IF_NOCLASSIFICATION) {
                    throw new Exception("CAD Model classification details not available");
                } else {
                    return 0;
                }
            }

            SAXBuilder builder = new SAXBuilder();
            Reader in = new StringReader(classifedObjectDetailsInXML);
            Document doc = builder.build(in);
            Element classifedObjectListTopNode = doc.getRootElement();
            java.util.List classifiedObjectNodeList = classifedObjectListTopNode.getChildren("classifiedobject");
            logMessage("performClassification", "Classified object size  :" + classifiedObjectNodeList);
            if (classifiedObjectNodeList == null || (classifiedObjectNodeList != null && classifiedObjectNodeList.size() == 0)) {
                // XML not defined properly
                logMessage("performClassification", "not classifeidobject found in the xml");
                if (DO_NOT_ALLOW_CHECKIN_IF_NOCLASSIFICATION) {
                    throw new Exception("CAD Model classification details not available");
                } else {
                    return 0;
                }
            } else {
                ListIterator classifiedObjectItr = classifiedObjectNodeList.listIterator();
                boolean nodeFound = false;
                while (classifiedObjectItr.hasNext()) {
                    Element classifiedObjectNode = (Element) classifiedObjectItr.next();
                    String name = classifiedObjectNode.getAttributeValue("name");
                    String type = classifiedObjectNode.getAttributeValue("majortype");
                    logMessage("performClassification", "name   " + name);
                    logMessage("performClassification", "type " + type);
                    if (name.equalsIgnoreCase(objectName) && type.equalsIgnoreCase(objectType)) {
                        nodeFound = true;
                        logMessage("performClassification", "Classification details are found  ");
                        performClassification(classifiedObjectNode, objectId);
                        logMessage("performClassification", "Object classification completed successfully");
                        break;
                    }
                }
                if (!nodeFound && DO_NOT_ALLOW_CHECKIN_IF_NOCLASSIFICATION) {
                    throw new Exception("CAD Model classification details not available");
                }
            }
        }
        logMessage("performClassification", "END");
        return 0;
    }

    /**
     * Method returns configuration details and list of classifications user has acess Return information from this method is an XML containing details of - configuration types, objects, mandatory,
     * allowednumber, attributes etc
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds username and csename
     * @throws Exception
     *             if the operation fails
     * @since Sourcing V6R2012x
     */
    public String getClassificationAndConfigurationDetails(Context context, String[] args) throws Exception {
        this.context = context;

        if (args.length != 2) {
            logMessage("getClassificationAndConfigurationDetails", "Missing required details - Username and CSEName");
            throw new Exception("Missing required details - Username and CSEName");
        }

        userName = args[0];
        cseName = args[1];

        if (userName == null || "".equals(userName.trim())) {
            logMessage("getClassificationAndConfigurationDetails", "Missing required details - UserName. " + "Its either null or blank");
            throw new Exception("Missing required details - UserName. Its either null or blank");
        }

        if (cseName == null || "".equals(cseName.trim())) {
            logMessage("getClassificationAndConfigurationDetails", "Missing required details - csaeName. " + "Its either null or blank");
            throw new Exception("Missing required details - csaeName. Its either null or blank");
        }

        if (!CSE_CLASSIFICATION_ASSIGNMENT_MAPPING.containsKey(cseName)) {
            logMessage("getClassificationAndConfigurationDetails", "Missing required details - csaeName. " + "Its either null or blank");
            throw new Exception("CSE :" + cseName + " not supported for classification");
        }

        logMessage("getClassificationAndConfigurationDetails", "Retrieving configuration details");
        // Get configuration details from the GCO
        HashMap configurationMap = getCSEClassificationConfigurations(cseName);
        logMessage("getClassificationAndConfigurationDetails", "FINAL Configurations defined for CSE :::" + configurationMap);

        logMessage("getClassificationAndConfigurationDetails", "Retrieving configurations accessible for User");
        HashMap classifiationsAccessibleForUser = getClassificationsAccessibleForUser(userName);
        logMessage("getClassificationAndConfigurationDetails", "List of classifications accessible for user " + classifiationsAccessibleForUser);

        String responseXML = prepareResponseXML(classifiationsAccessibleForUser, configurationMap);
        logMessage("getClassificationAndConfigurationDetails", "responseXML " + responseXML);
        if (responseXML == null || "".equals(responseXML)) {
            logMessage("getClassificationsAccessibleForUser", "Failed to retrieve classificaiton details for user " + userName);
            throw new Exception("Failed to retrieve classificaiton details for user " + userName);
        }
        if (WRITE_DEBUG_TO_RMI_LOGS && matrixLogger != null) {
            matrixLogger.close();
        }
        return responseXML;
    }

    public int classifyDerivedOuputAndViewables(Context context, String args[]) throws Exception {
        logMessage("classifyDerivedOuputAndViewables", "STARTED");
        boolean isContextPushed = false;
        int retValue = 1;
        try {
            if (!isContextPushed) {
                logMessage("classifyDerivedOuputAndViewables", "Context not pushed");
                // Updated to fix user agent password issue in 15x
                // ContextUtil.pushContext(context, _superUser, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                ContextUtil.pushContext(context);
                logMessage("classifyDerivedOuputAndViewables", "Context  pushed successfully");
                isContextPushed = true;
            }
            logMessage("classifyDerivedOuputAndViewables", "Performing classification");
            retValue = performClassifyDerivedOuputAndViewables(context, args);
            logMessage("classifyDerivedOuputAndViewables", "Classification Done");
        } catch (Exception e) {
            logMessage("classifyDerivedOuputAndViewables", "Error while classifying" + e.getMessage());
            throw e;
        } finally {
            logMessage("classifyDerivedOuputAndViewables", "isContextPushed " + isContextPushed);
            if (isContextPushed) {
                logMessage("classifyDerivedOuputAndViewables", "Context Popp started");
                ContextUtil.popContext(context);
                logMessage("classifyDerivedOuputAndViewables", "Context Pop Done");
                isContextPushed = false;
            }
        }
        logMessage("classifyDerivedOuputAndViewables", "Return value " + retValue);
        logMessage("classifyDerivedOuputAndViewables", "FINISHED");
        return retValue;
    }

    /**
     * Method is to be configured on Derived Ouput and Vieable relationshps When DP/Viewable realtionship is created between minor object and DP object - Navigate to the major object, retrieve
     * classificaitons and assign on derived object
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds fromobjectId, toobjectId, relationship
     * @throws Exception
     *             if the operation fails
     * @return int - 0 if successful
     * @since Sourcing V6R2012x
     */
    public int performClassifyDerivedOuputAndViewables(Context context, String[] args) throws Exception {
        logMessage("performClassifyDerivedOuputAndViewables", "STARTED");
        String fromObjectID = args[0];
        String toObjectID = args[1];
        String symMinorObjectToMajorObjectRelatioship = args[2];
        if (fromObjectID == null || "".equalsIgnoreCase(fromObjectID) || toObjectID == null || "".equalsIgnoreCase(toObjectID) || symMinorObjectToMajorObjectRelatioship == null
                || "".equalsIgnoreCase(symMinorObjectToMajorObjectRelatioship)) {
            logMessage("performClassifyDerivedOuputAndViewables", "Error while classifying derivedoutput/viewables");
            throw new Exception("Error while classifying derivedoutput/viewables");
        }
        String actRelationNameOfMinorToMajor = PropertyUtil.getSchemaProperty(context, symMinorObjectToMajorObjectRelatioship);
        // Navigate from Minor object i.e, fromObjectId to major object using the relationship
        StringList relationshipSelects = new StringList();
        relationshipSelects.addElement(DomainRelationship.SELECT_ID);

        StringList objectSelects = new StringList();
        objectSelects.addElement(DomainObject.SELECT_ID);
        objectSelects.addElement(DomainObject.SELECT_NAME);
        objectSelects.addElement(DomainObject.SELECT_TYPE);

        DomainObject minorObject = new DomainObject(fromObjectID);
        DomainObject dpObject = new DomainObject(toObjectID);
        MapList objectInfo = dpObject.getBasicInfo(context);
        HashMap basicInfo = (HashMap) objectInfo.get(0);
        String dpObjectName = (String) basicInfo.get("name");
        String dpObjectType = (String) basicInfo.get("type");
        logMessage("performClassifyDerivedOuputAndViewables", "Derived ouput object details " + objectInfo);
        logMessage("performClassifyDerivedOuputAndViewables", "dpObjectName " + dpObjectName);
        logMessage("performClassifyDerivedOuputAndViewables", "dpObjectType " + dpObjectType);
        String searchTypes = MCAD_DERIVED_OUTPUT_TYPE + "," + MCAD_VIEWABLE;
        logMessage("performClassifyDerivedOuputAndViewables", "searchTypes " + searchTypes);
        // Classify only 1st revision of the derivedoutput/viewables. Skip for other revision/versions
        MapList objectList = DomainObject.findObjects(context, // Context
                searchTypes, // Type
                dpObjectName, // Name
                DomainConstants.QUERY_WILDCARD, // Revision
                DomainConstants.QUERY_WILDCARD, // Owner
                DomainConstants.QUERY_WILDCARD, // Vault
                "revision!=A.0", // Where expression
                true, // Expand Type
                objectSelects); // Selectable
        if (objectList != null && objectList.size() > 0) {
            logMessage("performClassifyDerivedOuputAndViewables", "Found More than 1 derived ouput object");
            logMessage("performClassifyDerivedOuputAndViewables", "DP Object list " + objectList);
            logMessage("performClassifyDerivedOuputAndViewables", "Skipping classifying the object");
            logMessage("performClassifyDerivedOuputAndViewables", "END");
            return 0;
        }
        logMessage("performClassifyDerivedOuputAndViewables", "Derived Ouput objectList " + objectList);
        // Get the major object
        MapList majorObjectList = (MapList) minorObject.getRelatedObjects(context, // Context
                actRelationNameOfMinorToMajor, // Relationship name
                DomainConstants.QUERY_WILDCARD, // Type name
                objectSelects, // Object selectables
                relationshipSelects, // Relationship selects
                false, // from side
                true, // toside
                (short) 0, // Levels to expand
                null, "");
        if (majorObjectList == null || majorObjectList.size() == 0) {
            logMessage("performClassifyDerivedOuputAndViewables", "No Major Object found. Can't classify model");
            // throw new Exception("Error while classifying derivedoutput/viewables. No CAD Model fond");
            return 0;
        }
        logMessage("performClassifyDerivedOuputAndViewables", "majorObjectList " + majorObjectList);
        Hashtable majorObjectMap = (Hashtable) majorObjectList.get(0);
        String majorObjectId = (String) majorObjectMap.get(DomainObject.SELECT_ID);
        logMessage("performClassifyDerivedOuputAndViewables", "majorObjectId " + majorObjectId);
        DomainObject majorObject = new DomainObject(majorObjectId);
        // Retrieve major object classification by navigating through classified item relationship
        objectSelects.addElement("attribute[" + ClassificationConstants.ATTRIBUTE_MXSYS_INTERFACE + "].value");
        MapList classifiedObjectList = (MapList) majorObject.getRelatedObjects(context, // Context
                MCAD_CLASSIFIED_ITEM_RELATIONSHIP, // Relationship name
                DomainConstants.QUERY_WILDCARD, // Type name
                objectSelects, // Object selectables
                relationshipSelects, // Relationship selects
                true, // from side
                false, // toside
                (short) 0, // Levels to expand
                null, "");
        if (classifiedObjectList == null || classifiedObjectList.size() == 0) {
            logMessage("performClassifyDerivedOuputAndViewables", "Derivedouput/Viewable can't be classified as " + "CAD Model not classified.");
            if (DO_NOT_ALLOW_CHECKIN_IF_NOCLASSIFICATION) {
                throw new Exception("Derivedouput/Viewable can't be classified as " + "CAD Model not classified.");
            } else {
                logMessage("performClassifyDerivedOuputAndViewables", "END");
                return 0;
            }
        }
        logMessage("performClassifyDerivedOuputAndViewables", "classifiedObjectList " + classifiedObjectList);
        logMessage("performClassifyDerivedOuputAndViewables", "No of classification  " + classifiedObjectList.size());
        for (int counter = 0; counter < classifiedObjectList.size(); counter++) {
            Hashtable classifiedObjectMap = (Hashtable) classifiedObjectList.get(counter);
            String classifiedObjectId = (String) classifiedObjectMap.get(DomainObject.SELECT_ID);
            String classifiedObjectName = (String) classifiedObjectMap.get(DomainObject.SELECT_NAME);
            String classifiedObjectType = (String) classifiedObjectMap.get(DomainObject.SELECT_TYPE);
            logMessage("performClassifyDerivedOuputAndViewables", "classifiedObjectList " + classifiedObjectList);
            logMessage("performClassifyDerivedOuputAndViewables", "classifiedObjectName  " + classifiedObjectName);
            logMessage("performClassifyDerivedOuputAndViewables", "classifiedObjectType  " + classifiedObjectType);

            // Get the classification interface name
            String interfaces = (String) classifiedObjectMap.get("attribute[" + ClassificationConstants.ATTRIBUTE_MXSYS_INTERFACE + "].value");
            logMessage("performClassifyDerivedOuputAndViewables", "interfaces :" + interfaces);

            // Get the interface attributes
            StringBuffer getAgAttrsCmd = new StringBuffer();
            getAgAttrsCmd.append("list interface \"");
            getAgAttrsCmd.append(interfaces);
            getAgAttrsCmd.append("\" select name attribute dump , recordsep |");
            logMessage("performClassifyDerivedOuputAndViewables", "getAgAttrsCmd " + getAgAttrsCmd);
            String baseAttrsData = MqlUtil.mqlCommand(context, getAgAttrsCmd.toString(), true).trim();
            logMessage("performClassifyDerivedOuputAndViewables", "baseAttrsData " + baseAttrsData);
            StringList attrList = FrameworkUtil.split(baseAttrsData, ",");
            logMessage("performClassifyDerivedOuputAndViewables", "attrList  " + attrList);

            // Get the attribute values from major object
            HashMap attrValues = (HashMap) majorObject.getAttributeDetails(context);
            logMessage("performClassifyDerivedOuputAndViewables", "attrValues   " + attrValues);
            HashMap classifiedAttributeValues = new HashMap();
            logMessage("performClassifyDerivedOuputAndViewables", "Attribute Values");
            for (int attrCounter = 1; attrCounter < attrList.size(); attrCounter++) {
                String attrName = (String) attrList.get(attrCounter);
                HashMap attrMap = (HashMap) attrValues.get(attrName);
                logMessage("performClassifyDerivedOuputAndViewables", attrName);
                if (attrMap.containsKey(attrName)) {
                    String attrValue = (String) attrMap.get("value");
                    logMessage("performClassifyDerivedOuputAndViewables", attrName + "=" + attrValue);
                    classifiedAttributeValues.put(attrName, attrValue);
                }
            }
            logMessage("performClassifyDerivedOuputAndViewables", "classifiedAttributeValues " + classifiedAttributeValues);

            // Now connect DP object with classified object
            // Update the attributes
            // DomainObject classificationObject = new DomainObject(classifiedObjectId);
            // DomainRelationship.connect(context, classificationObject, MCAD_CLASSIFIED_ITEM_RELATIONSHIP, dpObject);
            Classification.addEndItems(context, classifiedObjectId, new String[] { toObjectID });
            logMessage("performClassifyDerivedOuputAndViewables", "DP Object classified SUCCESSFULLY with " + classifiedObjectType + " - " + classifiedObjectName);
            if (!classifiedAttributeValues.isEmpty()) {
                dpObject.setAttributeValues(context, classifiedAttributeValues);
                logMessage("performClassifyDerivedOuputAndViewables", "DP Object classified attributes are set");
            }
        }
        logMessage("performClassifyDerivedOuputAndViewables", "END");
        return 0;
    }

    /**
     * This utility method to perform classification of objects
     * @param Element
     *            - XML classifiedobject node
     * @param String
     *            - Object ID
     * @throws Exception
     *             if the operation fails
     * @returns void -
     * @since Sourcing V6R2012x
     */
    private Element performClassification(Element classifiedObjectNode, String objectId, Element objectNode) throws Exception {
        logMessage("performClassification", "STARTED");
        java.util.List classificationNodeList = classifiedObjectNode.getChildren("classification");
        ListIterator classificationListItr = classificationNodeList.listIterator();
        boolean overallItemclassification = true;
        boolean overallDpClassification = true;
        StringBuffer overallFailedmessages = new StringBuffer();
        DomainObject mcadObject = new DomainObject(objectId);
        String tempMessage = "";

        while (classificationListItr.hasNext()) {
            Element xmlClassificationNode = new Element("classification");
            try {
                Element classificationNode = (Element) classificationListItr.next();
                String classificationName = classificationNode.getAttributeValue("name");
                String classificationType = classificationNode.getAttributeValue("type");
                logMessage("performClassification", "classificationName " + classificationName);
                logMessage("performClassification", "classificationType " + classificationType);
                xmlClassificationNode.setAttribute(new com.matrixone.jdom.Attribute("name", classificationName));
                xmlClassificationNode.setAttribute(new com.matrixone.jdom.Attribute("type", classificationType));

                HashMap objAttributes = new HashMap();

                java.util.List objAttribElemList = (java.util.List) classificationNode.getChildren("attributelist");
                Element objAttribElem = null;
                if (objAttribElemList.size() > 0) {
                    objAttribElem = (Element) objAttribElemList.get(0);
                    java.util.List childElementsList = objAttribElem.getChildren("attribute");
                    Iterator childElementsListItr = childElementsList.iterator();
                    while (childElementsListItr.hasNext()) {
                        Element attribElem = (Element) childElementsListItr.next();
                        String attribName = (String) attribElem.getAttribute("name").getValue();
                        String attribValue = (String) attribElem.getAttribute("value").getValue();
                        objAttributes.put(attribName, attribValue);
                    }
                }
                logMessage("performClassification", "objAttributes  " + objAttributes);

                StringList objectSelects = new StringList();
                objectSelects.add(DomainObject.SELECT_TYPE);
                objectSelects.add(DomainObject.SELECT_NAME);
                objectSelects.add(DomainObject.SELECT_REVISION);
                objectSelects.add(DomainObject.SELECT_ID);

                MapList objectList = DomainObject.findObjects(context, // Context
                        classificationType, // Type
                        classificationName, // Name
                        "*", // Revision SYMBOL_HYPHON
                        DomainConstants.QUERY_WILDCARD, // Owner
                        DomainConstants.QUERY_WILDCARD, // Vault
                        null, // Where expression
                        true, // Expand Type
                        objectSelects); // Selectable
                logMessage("performClassification", "objectList   " + objectList);
                if (objectList == null || objectList.size() == 0) {
                    tempMessage = "No Classification object found with type " + classificationType + " name :" + classificationName;
                    logMessage("performClassification", tempMessage);
                    overallItemclassification = false;
                    overallDpClassification = false;
                    overallFailedmessages.append(tempMessage + "\n");
                    xmlClassificationNode.setAttribute(new com.matrixone.jdom.Attribute("result", "FAILED"));
                    xmlClassificationNode.setAttribute(new com.matrixone.jdom.Attribute("message", tempMessage));
                } else {
                    String classificationID = (String) ((HashMap) objectList.get(0)).get(DomainObject.SELECT_ID);
                    DomainObject classificationObject = new DomainObject(classificationID);
                    boolean isClassified = false;
                    try {
                        // Connect the classificaiton object and mcad object
                        // DomainRelationship.connect(context, classificationObject, MCAD_CLASSIFIED_ITEM_RELATIONSHIP, mcadObject);
                        Classification.addEndItems(context, classificationID, new String[] { objectId });
                        logMessage("performClassification", "MCAD Object classified SUCCESSFULLY with " + classificationType + " - " + classificationName);
                        if (!objAttributes.isEmpty()) {
                            mcadObject.setAttributeValues(context, objAttributes);
                            logMessage("performClassification", "MCAD Object attributes are set");
                        }
                        logMessage("performClassification", "ITEM CLASSIFICAITON SUCCESSFUL");
                        isClassified = true;
                    } catch (Exception e) {
                        tempMessage = "Item classficiation failed  - " + e.getMessage();
                        isClassified = false;
                        overallItemclassification = false;
                        overallDpClassification = false;
                        overallFailedmessages.append(tempMessage + "\n");
                        xmlClassificationNode.setAttribute(new com.matrixone.jdom.Attribute("result", "FAILED"));
                        xmlClassificationNode.setAttribute(new com.matrixone.jdom.Attribute("message", tempMessage));
                        logMessage("performClassification", "ITEM Classficiation Error " + tempMessage);
                    }
                    if (isClassified) {
                        xmlClassificationNode.setAttribute(new com.matrixone.jdom.Attribute("result", "SUCCESS"));
                        xmlClassificationNode.setAttribute(new com.matrixone.jdom.Attribute("message", ""));

                        try {
                            logMessage("performClassification", " Getting mirnor object details ");
                            // Get the minor object details

                            StringList relationshipSelects = new StringList();
                            relationshipSelects.addElement(DomainRelationship.SELECT_ID);
                            MapList minorObjectList = (MapList) mcadObject.getRelatedObjects(context, // Context
                                    RELATIONSHIP_VERSIONOF, // Relationship name
                                    DomainConstants.QUERY_WILDCARD, // Type name
                                    objectSelects, // Object selectables
                                    relationshipSelects, // Relationship selects
                                    true, // from side
                                    false, // toside
                                    (short) 0, // Levels to expand
                                    null, "");
                            logMessage("performClassification", " minorObjectList  " + minorObjectList);
                            // There should be only one Version object connected at this level
                            // So we will get first object from minorObjectList
                            Hashtable minorObjectmap = (Hashtable) minorObjectList.get(0);
                            String minorObjectId = (String) minorObjectmap.get(DomainObject.SELECT_ID);
                            DomainObject minorObject = new DomainObject(minorObjectId);
                            logMessage("performClassification", " minorObjectId  " + minorObjectId);

                            logMessage("performClassification", " Getting Derived output and Viewables");
                            // Get the Derived Output and Viewable object details
                            String searchTypes = MCAD_DERIVED_OUTPUT_TYPE + "," + MCAD_VIEWABLE;
                            MapList derivedOutputObjectList = (MapList) minorObject.getRelatedObjects(context, // Context
                                    RELATIONSHIP_DERIVEDOUTPUT + "," + RELATIONSHIP_VIEWABLE, // Relationship name
                                    searchTypes, // Type name
                                    objectSelects, // Object selectables
                                    relationshipSelects, // Relationship selects
                                    false, // from side
                                    true, // toside
                                    (short) 0, // Levels to expand
                                    null, "");
                            logMessage("performClassification", " derivedOutputObjectList " + derivedOutputObjectList);
                            if (derivedOutputObjectList == null || derivedOutputObjectList.size() == 0) {
                                logMessage("performClassification", " No Derived Outputs found");
                            } else {
                                logMessage("performClassification", " START CLASSIFYING DERIVED OUTPUTS");
                                for (int dpcounter = 0; dpcounter < derivedOutputObjectList.size(); dpcounter++) {
                                    try {
                                        Hashtable dpObjectMap = (Hashtable) derivedOutputObjectList.get(dpcounter);
                                        logMessage("performClassification", " dpObjectMap " + dpObjectMap);
                                        String dpObjectId = (String) dpObjectMap.get(DomainObject.SELECT_ID);
                                        logMessage("performClassification", " dpObjectId " + dpObjectId);
                                        DomainObject dpObject = new DomainObject(dpObjectId);

                                        // Connect the classificaiton object and mcad object
                                        // DomainRelationship.connect(context, classificationObject, MCAD_CLASSIFIED_ITEM_RELATIONSHIP, dpObject);
                                        Classification.addEndItems(context, classificationID, new String[] { dpObjectId });
                                        logMessage("performClassification", "MCAD Object classified SUCCESSFULLY with " + classificationType + " - " + classificationName);
                                        if (!objAttributes.isEmpty()) {
                                            dpObject.setAttributeValues(context, objAttributes);
                                            logMessage("performClassification", "MCAD Object attributes are set");
                                        }
                                    } catch (Exception e) {
                                        tempMessage = " Derived Output classfication failed " + e.getMessage();
                                        overallDpClassification = false;
                                        overallFailedmessages.append(tempMessage + "\n");
                                        logMessage("performClassification", tempMessage);
                                    }
                                }
                                logMessage("performClassification", " END CLASSIFYING DERIVED OUTPUTS");
                            }
                        } catch (Exception e) {
                            tempMessage = " Error while classifying DP objects" + e.getMessage();
                            logMessage("performClassification", tempMessage);
                            overallDpClassification = false;
                            overallFailedmessages.append(tempMessage + "\n");
                        }
                    }
                }
            } catch (Exception e) {
                String errMsg = e.getMessage();
                logMessage("performClassification", "Exception " + errMsg);
                xmlClassificationNode.setAttribute(new com.matrixone.jdom.Attribute("result", "FAILED"));
                xmlClassificationNode.setAttribute(new com.matrixone.jdom.Attribute("message", errMsg));
                overallItemclassification = false;
                overallDpClassification = false;
                overallFailedmessages.append(errMsg + "\n");
            }
            objectNode.addContent(xmlClassificationNode);
        }

        logMessage("performClassification", "overallItemclassification " + overallItemclassification);
        logMessage("performClassification", "overallDpClassification " + overallDpClassification);
        logMessage("performClassification", "overallFailedmessages " + overallFailedmessages.toString());

        if (overallItemclassification) {
            objectNode.setAttribute(new com.matrixone.jdom.Attribute("itemclassification", "SUCCESS"));
        } else {
            objectNode.setAttribute(new com.matrixone.jdom.Attribute("itemclassification", "FAILED"));
        }

        if (overallDpClassification) {
            objectNode.setAttribute(new com.matrixone.jdom.Attribute("derivedclssification", "SUCCESS"));
        } else {
            objectNode.setAttribute(new com.matrixone.jdom.Attribute("derivedclssification", "FAILED"));
        }

        if (overallItemclassification && overallDpClassification) {
            objectNode.setAttribute(new com.matrixone.jdom.Attribute("reason", ""));
        } else {
            objectNode.setAttribute(new com.matrixone.jdom.Attribute("reason", overallFailedmessages.toString()));
        }
        logMessage("performClassification", "END");

        return objectNode;
    }

    /**
     * This utility method to perform classification of objects
     * @param Element
     *            - XML classifiedobject node
     * @param String
     *            - Object ID
     * @throws Exception
     *             if the operation fails
     * @returns void -
     * @since Sourcing V6R2012x
     */
    private void performClassification(Element classifiedObjectNode, String objectId) throws Exception {
        logMessage("performClassification", "STARTED");
        java.util.List classificationNodeList = classifiedObjectNode.getChildren("classification");
        ListIterator classificationListItr = classificationNodeList.listIterator();
        while (classificationListItr.hasNext()) {
            Element classificationNode = (Element) classificationListItr.next();
            String classificationName = classificationNode.getAttributeValue("name");
            String classificationType = classificationNode.getAttributeValue("type");
            logMessage("performClassification", "classificationName " + classificationName);
            logMessage("performClassification", "classificationType " + classificationType);

            HashMap objAttributes = new HashMap();

            java.util.List objAttribElemList = (java.util.List) classificationNode.getChildren("attributelist");
            Element objAttribElem = null;
            if (objAttribElemList.size() > 0) {
                objAttribElem = (Element) objAttribElemList.get(0);
                java.util.List childElementsList = objAttribElem.getChildren("attribute");
                Iterator childElementsListItr = childElementsList.iterator();
                while (childElementsListItr.hasNext()) {
                    Element attribElem = (Element) childElementsListItr.next();
                    String attribName = (String) attribElem.getAttribute("name").getValue();
                    String attribValue = (String) attribElem.getAttribute("value").getValue();
                    objAttributes.put(attribName, attribValue);
                }
            }
            logMessage("performClassification", "objAttributes  " + objAttributes);

            StringList objectSelects = new StringList();
            objectSelects.add(DomainObject.SELECT_TYPE);
            objectSelects.add(DomainObject.SELECT_NAME);
            objectSelects.add(DomainObject.SELECT_REVISION);
            objectSelects.add(DomainObject.SELECT_ID);

            MapList objectList = DomainObject.findObjects(context, // Context
                    classificationType, // Type
                    classificationName, // Name
                    SYMBOL_HYPHON, // Revision
                    DomainConstants.QUERY_WILDCARD, // Owner
                    DomainConstants.QUERY_WILDCARD, // Vault
                    null, // Where expression
                    true, // Expand Type
                    objectSelects); // Selectable
            logMessage("performClassification", "objectList   " + objectList);
            if (objectList == null || objectList.size() == 0) {
                String tempMessage = "No Classification object found with type " + classificationType + " name :" + classificationName;
                logMessage("generateClassificaitonXMLNode", tempMessage);
                throw new Exception(tempMessage);
            } else {
                String classificationID = (String) ((HashMap) objectList.get(0)).get(DomainObject.SELECT_ID);
                DomainObject classificationObject = new DomainObject(classificationID);
                DomainObject mcadObject = new DomainObject(objectId);
                // Connect the classificaiton object and mcad object
                DomainRelationship.connect(context, classificationObject, MCAD_CLASSIFIED_ITEM_RELATIONSHIP, mcadObject);
                logMessage("generateClassificaitonXMLNode", "MCAD Object classified SUCCESSFULLY with " + classificationType + " - " + classificationName);
                if (!objAttributes.isEmpty()) {
                    mcadObject.setAttributeValues(context, objAttributes);
                    logMessage("generateClassificaitonXMLNode", "MCAD Object attributes are set");
                }
            }
        }
        logMessage("performClassification", "END");
    }

    /**
     * This utility method to search for classification holder object
     * @param userName
     *            - nam of the holder object to search for
     * @throws Exception
     *             if the operation fails
     * @returns MapList -
     * @since Sourcing V6R2012x
     */
    private MapList searchForClassificationHolder(String holdName) throws Exception {
        StringList objectSelects = new StringList();
        objectSelects.add(DomainObject.SELECT_TYPE);
        objectSelects.add(DomainObject.SELECT_NAME);
        objectSelects.add(DomainObject.SELECT_REVISION);
        objectSelects.add(DomainObject.SELECT_ID);
        logMessage("searchForClassificationHolder", "Searching for objects of type :" + MCAD_CheckinEx_Classification_Holder);
        MapList objectList = DomainObject.findObjects(context, // Context
                MCAD_CheckinEx_Classification_Holder, // Type
                holdName, // Name
                SYMBOL_HYPHON, // Revision
                DomainConstants.QUERY_WILDCARD, // Owner
                DomainConstants.QUERY_WILDCARD, // Vault
                null, // Where expression
                true, // Expand Type
                objectSelects); // Selectable
        logMessage("searchForClassificationHolder", "objectList " + objectList.toString());
        return objectList;
    }

    /**
     * This Method prepares filanl response XML for list of configurations user has access with attribute details
     * @param HashMap
     *            - classifcaitons list allowed for the user
     * @param HashMap
     *            - Configuration map defined for the CSE
     * @throws Exception
     *             if the operation fails
     * @returns String - XML string
     * @since Sourcing V6R2012x
     */
    private String prepareResponseXML(HashMap classificationAllowedForUser, HashMap configurationMap) throws Exception {
        // 1. Get the list of the configurations types allowed for the user
        // 2. For each configuration type
        // a) verify if configuration type defiend in GCO or not ... if not ignore
        // b) if defined in GCO, get list of allowed objects and verif if user is having access or not
        // c) if user is having access for allowed objects, get the attribute details and prepare the XML
        logMessage("prepareResponseXML", "STARTED");

        Element xmlresponseNode = new Element("response");
        Element userNameNode = new Element("username");
        userNameNode.setText(userName);
        xmlresponseNode.addContent(userNameNode);
        Element cseNameNode = new Element("integrationname");
        cseNameNode.setText(cseName);
        xmlresponseNode.addContent(cseNameNode);

        Element xmlClassificationTypeListNode = new Element("classificationtypelist");
        boolean hasValidClassificationTypes = false;
        String returnXML = "";

        Set allowedClassificationTypesForUser = classificationAllowedForUser.keySet();
        logMessage("prepareResponseXML", "Classification Types allowed for user " + allowedClassificationTypesForUser.toString());
        Iterator userAllowedClassificationIterator = allowedClassificationTypesForUser.iterator();
        while (userAllowedClassificationIterator.hasNext()) {
            String classificaitonType = (String) userAllowedClassificationIterator.next();
            logMessage("prepareResponseXML", "classificaitonType " + classificaitonType);

            if (configurationMap.containsKey(classificaitonType)) {
                Element xmlClassificationTypeNode = new Element("classificationtype");
                xmlClassificationTypeNode.setAttribute(new com.matrixone.jdom.Attribute("type", classificaitonType));

                String mandatoryValue = (String) ((HashMap) configurationMap.get(classificaitonType)).get(MANDATORY_KEY);
                String allowedNumberValue = (String) ((HashMap) configurationMap.get(classificaitonType)).get(SELECTABLE_KEY);
                Element mandatoryNode = new Element(MANDATORY_KEY);
                mandatoryNode.setText(mandatoryValue);
                Element allowedNumberNode = new Element(SELECTABLE_KEY);
                allowedNumberNode.setText(allowedNumberValue);
                xmlClassificationTypeNode.addContent(mandatoryNode);
                xmlClassificationTypeNode.addContent(allowedNumberNode);

                boolean hasClassificationObjects = false;
                StringList userAllowedObjectList = (StringList) classificationAllowedForUser.get(classificaitonType);
                StringList configrationAllowedObjects = (StringList) ((HashMap) configurationMap.get(classificaitonType)).get(ALLOWEDCLASSIFICATIONS_KEY);

                logMessage("prepareResponseXML", "Classifications for which user is having access ::" + userAllowedObjectList);
                logMessage("prepareResponseXML", "Classifications allowed by configuration ::" + configrationAllowedObjects);

                if (configrationAllowedObjects.contains(ALLOWED_OBJECTS_ALL)) {
                    for (int userAllowedCounter = 0; userAllowedCounter < userAllowedObjectList.size(); userAllowedCounter++) {
                        String userClassifiedAccessObectName = (String) userAllowedObjectList.get(userAllowedCounter);
                        hasClassificationObjects = true;
                        logMessage("prepareResponseXML", "userClassifiedAccessObectName " + userClassifiedAccessObectName);
                        xmlClassificationTypeNode.addContent(generateClassificaitonXMLNode(classificaitonType, userClassifiedAccessObectName));
                    }
                } else {
                    for (int configAllowedCounter = 0; configAllowedCounter < configrationAllowedObjects.size(); configAllowedCounter++) {
                        String userClassifiedAccessObectName = (String) configrationAllowedObjects.get(configAllowedCounter);
                        // Verify if configuration object is in user allowed list
                        if (userAllowedObjectList.contains(userClassifiedAccessObectName)) {
                            // User is having access for the classification object
                            // prepare the XML nodes
                            hasClassificationObjects = true;
                            logMessage("prepareResponseXML", "userClassifiedAccessObectName " + userClassifiedAccessObectName);
                            xmlClassificationTypeNode.addContent(generateClassificaitonXMLNode(classificaitonType, userClassifiedAccessObectName));

                        }
                    }
                }
                if (hasClassificationObjects) {
                    hasValidClassificationTypes = true;
                    logMessage("prepareResponseXML", "hasValidClassificationTypes " + hasValidClassificationTypes);
                    xmlClassificationTypeListNode.addContent(xmlClassificationTypeNode);
                }
            }
        }

        if (hasValidClassificationTypes) {
            xmlresponseNode.addContent(xmlClassificationTypeListNode);
            XMLOutputter outputterWS = new XMLOutputter();
            returnXML = outputterWS.outputString(xmlresponseNode);
        } else {
            if (DO_NOT_ALLOW_CHECKIN_IF_NOCLASSIFICATION) {
                throw new Exception("CAD Model classification details not available");
            } else {
                return "";
            }
        }
        logMessage("prepareResponseXML", "ENDED");
        return returnXML;
    }

    /**
     * This utility method to generate classification details
     * @param classificationType
     *            - classification type
     * @param classificationObjectName
     *            - classificaiton name
     * @throws Exception
     *             if the operation fails
     * @returns Element -
     * @since Sourcing V6R2012x
     */

    private Element generateClassificaitonXMLNode(String classificationType, String classificationObjectName) throws Exception {
        logMessage("generateClassificaitonXMLNode", "STARTED");
        Element classificationNode = new Element(ALLOWEDCLASSIFICATIONS_KEY);
        classificationNode.setAttribute(new com.matrixone.jdom.Attribute("name", classificationObjectName));
        // 1.Find the classification object type, name and retrieve the hidden field ID
        // 2.Get the interfaces and attribute details
        // 3.For each attribute get the deails
        StringList objectSelects = new StringList();
        objectSelects.add(DomainObject.SELECT_TYPE);
        objectSelects.add(DomainObject.SELECT_NAME);
        objectSelects.add(DomainObject.SELECT_REVISION);
        objectSelects.add(DomainObject.SELECT_DESCRIPTION);
        objectSelects.add("attribute[" + ClassificationConstants.ATTRIBUTE_MXSYS_INTERFACE + "].value");

        String tempMessage = "";

        MapList objectList = DomainObject.findObjects(context, // Context
                classificationType, // Type
                classificationObjectName, // Name
                "*", // Revision SYMBOL_HYPHON
                DomainConstants.QUERY_WILDCARD, // Owner
                DomainConstants.QUERY_WILDCARD, // Vault
                null, // Where expression
                true, // Expand Type
                objectSelects); // Selectable
        if (objectList == null || objectList.size() == 0) {
            tempMessage = "No Classification object found with type " + classificationType + " name :" + classificationObjectName;
            logMessage("generateClassificaitonXMLNode", tempMessage);
            throw new Exception(tempMessage);
        }
        logMessage("generateClassificaitonXMLNode", "objectList :" + objectList.toString());
        String interfaces = (String) ((HashMap) objectList.get(0)).get("attribute[" + ClassificationConstants.ATTRIBUTE_MXSYS_INTERFACE + "].value");
        logMessage("generateClassificaitonXMLNode", "interfaces :" + interfaces);

        StringBuffer getAgAttrsCmd = new StringBuffer();
        getAgAttrsCmd.append("list interface \"");
        getAgAttrsCmd.append(interfaces);
        getAgAttrsCmd.append("\" select name attribute dump , recordsep |");
        logMessage("generateClassificaitonXMLNode", "getAgAttrsCmd " + getAgAttrsCmd);
        String baseAttrsData = MqlUtil.mqlCommand(context, getAgAttrsCmd.toString(), true).trim();
        logMessage("generateClassificaitonXMLNode", "baseAttrsData " + baseAttrsData);
        StringList attrList = FrameworkUtil.split(baseAttrsData, ",");
        logMessage("generateClassificaitonXMLNode", "ATRLIST BEFORE " + attrList);
        int startIndex = 1;
        // If Attribute configurations defined for the clasfication object, then consider only those object
        // else consider all the attributes
        MapList attributesDefinedInConfiguration = null;
        if (classificationSpecificAttributeMap != null && classificationSpecificAttributeMap.containsKey(classificationObjectName)) {
            attributesDefinedInConfiguration = (MapList) classificationSpecificAttributeMap.get(classificationObjectName);
            // Soft the attributes based on the order number here
            logMessage("generateClassificaitonXMLNode", "Configured Attributes List Before sorting " + attributesDefinedInConfiguration);
            attributesDefinedInConfiguration.sort(ATTRIBUTE_ORDER_KEY, "ascending", "string");
            logMessage("generateClassificaitonXMLNode", "Configured Attributes List After sorting " + attributesDefinedInConfiguration);
            StringList newAttrList = new StringList();
            for (int atrCounter = 0; atrCounter < attributesDefinedInConfiguration.size(); atrCounter++) {
                HashMap tempMap = (HashMap) attributesDefinedInConfiguration.get(atrCounter);
                String attributeConfigured = (String) tempMap.get(ATTRIBUTE_NAME_KEY);
                logMessage("generateClassificaitonXMLNode", "Checking if attribute " + attributeConfigured + " exist on interface");
                if (attrList.contains(attributeConfigured)) {
                    newAttrList.addElement(attributeConfigured);
                    logMessage("generateClassificaitonXMLNode", "Attribute " + attributeConfigured + " exist on interface. Considered for Item Clasification");
                } else {
                    logMessage("generateClassificaitonXMLNode", "Attribute " + attributeConfigured + " defined in configuration not exist on classification " + classificationObjectName);
                    throw new Exception("Attribute " + attributeConfigured + " defined in configuration not exist on classification " + classificationObjectName);
                }
            }
            logMessage("generateClassificaitonXMLNode", " Final Attribute List allowed for Item Clasification " + newAttrList);
            attrList = newAttrList;
            startIndex = 0;
        }
        logMessage("generateClassificaitonXMLNode", "ATRLIST after " + attrList);
        logMessage("generateClassificaitonXMLNode", "startIndex  " + startIndex);
        for (int attrCounter = startIndex; attrCounter < attrList.size(); attrCounter++) {
            String attributeName = (String) attrList.get(attrCounter);
            AttributeType attrType = new AttributeType(attributeName);
            String attributeDataType = attrType.getDataType(context);
            String defaultValue = attrType.getDefaultValue(context);
            StringList choices = attrType.getChoices(context);
            boolean multiLine = attrType.isMultiLine();
            // 07172013 - Fix for displayname error
            /*
             * String aliasName = FrameworkUtil.getAliasForAdmin(context, "attribute",attributeName, true); if(aliasName==null || "".equals(aliasName)){ aliasName = attributeName; }
             */
            // 07172013 - Fix for displayname error
            String attributeMandatory = MANDATORY_NO;
            String attributeOrder = "NOTDEFINED";
            // Configurations defined in GCO ... Get the default value, mandatory or not
            if (attributesDefinedInConfiguration != null) {
                logMessage("generateClassificaitonXMLNode", "ATTRIBUTE CONFIGURATIONS DEFINED");
                for (int lstCounter = 0; lstCounter < attributesDefinedInConfiguration.size(); lstCounter++) {
                    HashMap tempMap = (HashMap) attributesDefinedInConfiguration.get(lstCounter);
                    String nameOfAttribute = (String) tempMap.get(ATTRIBUTE_NAME_KEY);
                    if (nameOfAttribute.equals(attributeName)) {
                        String atrDefaultInConfiguration = (String) tempMap.get(ATTRIBUTE_DEFAULT_KEY);
                        String atrMandatoryInConfiguration = (String) tempMap.get(ATTRIBUTE_MANDATORY_KEY);
                        attributeOrder = (String) tempMap.get(ATTRIBUTE_ORDER_KEY);
                        logMessage("generateClassificaitonXMLNode", "nameOfAttribute " + nameOfAttribute);
                        logMessage("generateClassificaitonXMLNode", "atrDefaultInConfiguration " + atrDefaultInConfiguration);
                        logMessage("generateClassificaitonXMLNode", "atrMandatoryInConfiguration " + atrMandatoryInConfiguration);
                        logMessage("generateClassificaitonXMLNode", "attributeOrder " + attributeOrder);
                        if (!(ATTRIBUTE_DEFAULT_FROM_ENOVIA.equals(atrDefaultInConfiguration))) {
                            if (choices != null && !(choices.contains(atrDefaultInConfiguration))) {
                                logMessage("generateClassificaitonXMLNode", "choices " + choices);
                                throw new Exception("Attribute " + nameOfAttribute + " default value in configuration " + "not matching with range values.");
                            }
                            defaultValue = atrDefaultInConfiguration;
                        }
                        if (atrMandatoryInConfiguration != null && !"".equals(atrMandatoryInConfiguration)) {
                            if (MANDATORY_YES_ONELETTER.equalsIgnoreCase(atrMandatoryInConfiguration) || MANDATORY_YES.equalsIgnoreCase(atrMandatoryInConfiguration)) {
                                attributeMandatory = MANDATORY_YES;
                            } else {
                                attributeMandatory = MANDATORY_NO;
                            }
                        }
                        logMessage("generateClassificaitonXMLNode", "defaultValue  " + defaultValue);
                        logMessage("generateClassificaitonXMLNode", "attributeMandatory  " + attributeMandatory);
                        break;
                    }
                }
                logMessage("generateClassificaitonXMLNode", "ATRLIST after " + attrList);
            }
            Element attributeNode = new Element("attribute");
            attributeNode.setAttribute(new com.matrixone.jdom.Attribute("name", attributeName));
            // 07172013 - Fix for displayname error
            // attributeNode.setAttribute(new com.matrixone.jdom.Attribute("displayname", aliasName));
            attributeNode.setAttribute(new com.matrixone.jdom.Attribute("displayname", attributeName));
            // 07172013 - Fix for displayname error
            if ("string".equalsIgnoreCase(attributeDataType)) {
                attributeNode.setAttribute(new com.matrixone.jdom.Attribute("multiline", Boolean.toString(multiLine)));
            }
            Element tempNode = new Element("datatype");
            tempNode.setText(attributeDataType);
            attributeNode.addContent(tempNode);
            tempNode = new Element("defaultvalue");
            tempNode.setText(defaultValue);
            attributeNode.addContent(tempNode);
            tempNode = new Element("order");
            tempNode.setText(attributeOrder);
            attributeNode.addContent(tempNode);
            tempNode = new Element("mandatory");
            tempNode.setText(attributeMandatory);
            attributeNode.addContent(tempNode);
            tempNode = new Element("rangelist");
            if (choices != null) {
                for (int rangeCounter = 0; rangeCounter < choices.size(); rangeCounter++) {
                    Element rangeNode = new Element("range");
                    rangeNode.setText((String) choices.get(rangeCounter));
                    tempNode.addContent(rangeNode);
                }
            }
            attributeNode.addContent(tempNode);
            classificationNode.addContent(attributeNode);
        }

        logMessage("generateClassificaitonXMLNode", "ENDED");
        return classificationNode;

    }

    /**
     * Method retrieves list of configurations for which user is having access
     * @param userName
     *            name of the user
     * @throws Exception
     *             if the operation fails
     * @returns HashMap - key is configuration types and value is list of objects
     * @since Sourcing V6R2012x
     */
    private HashMap getClassificationsAccessibleForUser(String userName) throws Exception {
        // This is temporary implementation of User access for Classifications
        // This should be updated with GE specific API calls once the details are avaialble
        // Main goals here is to get a standard format which is used further processing so that no impact even if GE
        // specific API calls are made.
        // Once GE specific API's are integrated, then make sure to return the data in the following HashMap format
        // where key is - Classification Type, value is - list of classification objects for which user is having access
        logMessage("getClassificationsAccessibleForUser", "STARTED");
        HashMap userConfigurationList = userAccessImplementationByITI(userName);
        if (userConfigurationList.isEmpty()) {
            logMessage("getClassificationsAccessibleForUser", "No access for Classifications");
            throw new Exception("No access for Classifications");
        }
        logMessage("getClassificationsAccessibleForUser", "ENDED");
        return userConfigurationList;
    }

    /**
     * Dumay method for retrieving Classificaiton details for which user has access
     * @param userName
     *            name of the user
     * @throws Exception
     *             if the operation fails
     * @returns HashMap - key is configuration types and value is list of objects
     * @since Sourcing V6R2012x
     */
    private HashMap userAccessImplementationByITI(String userName) throws Exception {
        // Search for the "classification" types - configurationTypes
        logMessage("userAccessImplementationByITI", "STARTED");
        logMessage("userAccessImplementationByITI", "Searching for classification types " + configurationTypes);
        if (configurationTypes == null || configurationTypes.isEmpty() || configurationTypes.size() == 0) {
            logMessage("userAccessImplementationByITI", "Classificaiton configuration not defined");
            throw new Exception("Classificaiton configurations not defined");
        }
        String allowedConfigTypes = null;
        for (int counter = 0; counter < configurationTypes.size(); counter++) {
            if (allowedConfigTypes == null) {
                allowedConfigTypes = (String) configurationTypes.get(0);
            } else {
                allowedConfigTypes = allowedConfigTypes + "," + (String) configurationTypes.get(counter);
            }
        }
        logMessage("userAccessImplementationByITI", "Searching for classification types " + allowedConfigTypes);
        logMessage("userAccessImplementationByITI", "user name  " + context.getUser());

        StringList objectSelects = new StringList();
        objectSelects.add(DomainObject.SELECT_TYPE);
        objectSelects.add(DomainObject.SELECT_NAME);
        objectSelects.add(DomainObject.SELECT_DESCRIPTION);

        MapList tempList = DomainObject.findObjects(context, // Context
                allowedConfigTypes, // Type
                DomainObject.QUERY_WILDCARD, // Name
                "*", // Revision SYMBOL_HYPHON
                DomainConstants.QUERY_WILDCARD, // Owner
                DomainConstants.QUERY_WILDCARD, // Vault
                null, // Where expression
                true, // Expand Type
                objectSelects); // Selectable
        if (tempList == null || tempList.size() == 0) {
            logMessage("userAccessImplementationByITI", "No access to classify CAD models");
            throw new Exception("No Classification Types assigned to the user. The MCAD Classification will not be allowed");
        }

        HashMap allowedClassificationsForUser = new HashMap();
        for (int counter = 0; counter < tempList.size(); counter++) {
            HashMap configurationDetails = (HashMap) tempList.get(counter);
            String type = (String) configurationDetails.get(DomainObject.SELECT_TYPE);
            String name = (String) configurationDetails.get(DomainObject.SELECT_NAME);
            if (allowedClassificationsForUser.containsKey(type)) {
                StringList objectList = (StringList) allowedClassificationsForUser.get(type);
                objectList.addElement(name);
                allowedClassificationsForUser.put(type, objectList);

            } else {
                StringList objectList = new StringList();
                objectList.addElement(name);
                allowedClassificationsForUser.put(type, objectList);
            }
        }
        logMessage("userAccessImplementationByITI", "Allowed Classificaiton List for User :::" + allowedClassificationsForUser);
        logMessage("userAccessImplementationByITI", "ENDED");
        return allowedClassificationsForUser;
    }

    /**
     * Method retrieves classification assignment for specific CSE, prepares Map with configuration details and returns.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds username and csename
     * @throws Exception
     *             if the operation fails
     * @returns HashMap - key is configuration types
     * @since Sourcing V6R2012x
     */
    private HashMap getCSEClassificationConfigurations(String cseName) throws Exception {

        /*
         * Logic for reading the assignment tracker configurations 1. Find the correct classificaiton object based on CSEName with configurations 2. Tokenize and prepare configurations as HashMap
         * described below {"IP Control Class", {mandatory="YES/NO", allowednumber="Number or MANY", classifications=STRINGLIST of names}}
         */
        HashMap cseMCADClassificationAssignment = getClassificationAssignmentControlForCSE(cseName);
        logMessage("getCSEClassificationConfigurations", "CSE :" + cseName + " Clasification assignment " + "configurations :- " + cseMCADClassificationAssignment);

        // String definedConfigurations = (String) cseMCADClassificationAssignment.get(DomainObject.SELECT_DESCRIPTION);
        String definedTypeConfigurations = (String) cseMCADClassificationAssignment.get("attribute[" + MCADInteg_CLASSIFICATIONTYPE_CONFIGURATION + "].value");
        logMessage("getCSEClassificationConfigurations", "definedTypeConfigurations  " + definedTypeConfigurations);

        if (definedTypeConfigurations == null || "".equals(definedTypeConfigurations.trim())) {
            logMessage("getCSEClassificationConfigurations", "Classification Assignment control for " + cseName + " is empty");
            throw new Exception("Classification Assignment control for " + cseName + " is empty");
        }

        String definedAttributeConfigurations = (String) cseMCADClassificationAssignment.get("attribute[" + MCADInteg_CLASSIFICATIONATTRIBUTE_CONFIGURATION + "].value");
        logMessage("getCSEClassificationConfigurations", "definedAttributeConfigurations  " + definedAttributeConfigurations);
        classificationSpecificAttributeMap = parseAttributeConfigurations(definedAttributeConfigurations);

        return parseConfiguration(definedTypeConfigurations);
    }

    /**
     * Utility method for parsing attribute configurations defined in Assignment Controlobject Key is type of the configuration boject like Class III, Class IV etc Value is MapList of HashMaps with
     * key-value paris for mandatory, defaultvalue, order, name Attribute Configurations defined in control object with following syntax #ClassificationObjectName|AttributeName|Attribute Order in
     * Excel Sheet|Clasification Default Value or Enovia Attribute Defualt Value|Mandatory
     * @param String
     *            - ATTRIBUTE configurations defined in GCO
     * @throws Exception
     *             if the operation fails or configurations not defined properly
     * @returns Map - Key is classificaiton object, value is maplist of HashMaps
     * @since Sourcing V6R2012x
     */
    private HashMap parseAttributeConfigurations(String definedAttributeConfigurations) throws Exception {
        logMessage("parseAttributeConfigurations", "STARTED");
        HashMap returnMap = new HashMap();
        if (definedAttributeConfigurations == null || "".equals(definedAttributeConfigurations.trim())) {
            logMessage("parseAttributeConfigurations", "No Attribute Configurations defined");
        } else {
            StringList configuredLines = FrameworkUtil.split(definedAttributeConfigurations, LINE_SEPARATOR);
            int numberOfLines = configuredLines.size();
            String tempLine = "";
            String classificationObjectFieldValue = "";
            String attributeNameFieldValue = "";
            String orderFieldValue = "";
            String defaultFieldValue = "";
            String mandatoryFieldValue = "";
            String tmpMesage = "";
            logMessage("parseAttributeConfigurations", "Total number of lines in Attribute configurations " + numberOfLines);

            for (int lineCounter = 0; lineCounter < numberOfLines; lineCounter++) {
                tempLine = (String) configuredLines.get(lineCounter);
                if (tempLine.length() == 0 || tempLine.startsWith(IGNORE_LINE_SYMBOL1) || tempLine.startsWith(IGNORE_LINE_SYMBOL2)) {
                    continue;
                }

                tmpMesage = "";
                classificationObjectFieldValue = "";
                attributeNameFieldValue = "";
                orderFieldValue = "";
                defaultFieldValue = "";
                mandatoryFieldValue = "";

                logMessage("parseAttributeConfigurations", "parsing line " + tempLine);
                StringList configuredFields = FrameworkUtil.split(tempLine, FIELD_SEPARATOR);
                int numberOfFields = configuredFields.size();
                if (numberOfFields < NUMBER_OF_ATTRIBUTE_FIELDS_PER_LINE) {
                    tmpMesage = "Configuration not defined properly at line number " + (lineCounter + 1) + ". Missing fields";
                    logMessage("parseAttributeConfigurations", tmpMesage);
                    throw new Exception(tmpMesage);
                }

                classificationObjectFieldValue = (String) configuredFields.get(CLASSIFICATION_OBJECTNAME_INDEX);
                attributeNameFieldValue = (String) configuredFields.get(ATTRIBUTENAME_INDEX);
                orderFieldValue = (String) configuredFields.get(ATTRIBUTE_ORDER_INDEX);
                defaultFieldValue = (String) configuredFields.get(ATTRIBUTE_DEFAULT_INDEX);
                mandatoryFieldValue = (String) configuredFields.get(ATTRIBUTE_MANDATORY_INDEX);
                logMessage("parseAttributeConfigurations", "classificationObjectFieldValue " + classificationObjectFieldValue);
                logMessage("parseAttributeConfigurations", "attributeNameFieldValue " + attributeNameFieldValue);
                logMessage("parseAttributeConfigurations", "orderFieldValue " + orderFieldValue);
                logMessage("parseAttributeConfigurations", "defaultFieldValue " + defaultFieldValue);
                logMessage("parseAttributeConfigurations", "mandatoryFieldValue " + mandatoryFieldValue);

                if (classificationObjectFieldValue == null || "".equals(classificationObjectFieldValue.trim())) {
                    tmpMesage = "At line number " + (lineCounter + 1) + " Classification object field not defined properly for attribute configuration";
                    logMessage("parseAttributeConfigurations", tmpMesage);
                    throw new Exception(tmpMesage);
                }
                if (attributeNameFieldValue == null || "".equals(attributeNameFieldValue.trim())) {
                    tmpMesage = "At line number " + (lineCounter + 1) + " Attribute Name field not defined properly";
                    logMessage("parseAttributeConfigurations", tmpMesage);
                    throw new Exception(tmpMesage);
                }
                if (orderFieldValue == null || "".equals(orderFieldValue.trim())) {
                    tmpMesage = "At line number " + (lineCounter + 1) + " Attribute Order types field not defined properly";
                    logMessage("parseAttributeConfigurations", tmpMesage);
                    throw new Exception(tmpMesage);
                }
                if (defaultFieldValue == null) {
                    tmpMesage = "At line number " + (lineCounter + 1) + " Attribute default field  not defined properly";
                    logMessage("parseAttributeConfigurations", tmpMesage);
                    throw new Exception(tmpMesage);
                }
                if (mandatoryFieldValue == null) {
                    tmpMesage = "At line number " + (lineCounter + 1) + " Attribute Mandatory field  not defined properly";
                    logMessage("parseAttributeConfigurations", tmpMesage);
                    throw new Exception(tmpMesage);
                }

                if (MANDATORY_YES_ONELETTER.equalsIgnoreCase(mandatoryFieldValue) || MANDATORY_YES.equalsIgnoreCase(mandatoryFieldValue)) {
                    mandatoryFieldValue = MANDATORY_YES;
                } else if ("".equals(mandatoryFieldValue.trim()) || MANDATORY_NO_ONELETTER.equalsIgnoreCase(mandatoryFieldValue) || MANDATORY_NO.equalsIgnoreCase(mandatoryFieldValue)) {
                    mandatoryFieldValue = MANDATORY_NO;
                }

                HashMap configMap = new HashMap();
                configMap.put(ATTRIBUTE_NAME_KEY, attributeNameFieldValue);
                configMap.put(ATTRIBUTE_ORDER_KEY, orderFieldValue);
                configMap.put(ATTRIBUTE_DEFAULT_KEY, defaultFieldValue);
                configMap.put(ATTRIBUTE_MANDATORY_KEY, mandatoryFieldValue);
                logMessage("parseAttributeConfigurations", "Line Number :" + (lineCounter + 1) + "    configMap " + configMap);

                if (returnMap.containsKey(classificationObjectFieldValue)) {
                    MapList classificationAttributeLst = (MapList) returnMap.get(classificationObjectFieldValue);
                    classificationAttributeLst.add(configMap);
                    returnMap.put(classificationObjectFieldValue, classificationAttributeLst);
                } else {
                    MapList classificationAttributeLst = new MapList();
                    classificationAttributeLst.add(configMap);
                    returnMap.put(classificationObjectFieldValue, classificationAttributeLst);
                }
                logMessage("parseAttributeConfigurations", "Line Number :" + (lineCounter + 1) + "    configMap " + configMap);
            }
        }
        logMessage("parseAttributeConfigurations", "returnMap  " + returnMap);
        logMessage("parseAttributeConfigurations", "END");
        return returnMap;
    }

    /**
     * Utility method for parsing configurations defined in Assignment Controlobject Key is type of the configuration like IP Control Class, Export Control Class Value is HashMap with key-value paris
     * for mandatory, noallowedselections, control objects Configurations defined in control object with following syntax Classificaiton Type|One or Many Classification Types|Mandatory|List of
     * Classification objects Return HashMap from this method is HashMap where key is String i.e, "Classificaiton Type" Value is HashMap. For example, Export Control Class|One|Y|EAR~ITAR~Not Export
     * Controlled is translated as {"Export Control Class", {mandatory="YES", allowednumber="1", classifications={EAR,ITAR,Not Export Controlled}}
     * @param String
     *            - configurations defined in GCO
     * @throws Exception
     *             if the operation fails or configurations not defined properly
     * @returns Map - Key is classificaiton type, value is HashMap
     * @since Sourcing V6R2012x
     */
    private HashMap parseConfiguration(String definedConfigurations) throws Exception {

        logMessage("parseConfiguration", "STARTED");
        HashMap returnConfigurationsMap = new HashMap();

        StringList configuredLines = FrameworkUtil.split(definedConfigurations, LINE_SEPARATOR);
        int numberOfLines = configuredLines.size();
        String tempLine = "";
        String classificationTypeFieldValue = "";
        String selectableFieldValue = "";
        String mandatoryFieldValue = "";
        String allowedObjectsFieldValue = "";
        String tmpMesage = "";
        logMessage("parseConfiguration", "Total number of lines in configurations " + numberOfLines);

        for (int lineCounter = 0; lineCounter < numberOfLines; lineCounter++) {
            tempLine = (String) configuredLines.get(lineCounter);
            if (tempLine.length() == 0 || tempLine.startsWith(IGNORE_LINE_SYMBOL1) || tempLine.startsWith(IGNORE_LINE_SYMBOL2)) {
                continue;
            }

            tmpMesage = "";
            classificationTypeFieldValue = "";
            selectableFieldValue = "";
            mandatoryFieldValue = "";
            allowedObjectsFieldValue = "";

            logMessage("parseConfiguration", "parsing line " + tempLine);
            StringList configuredFields = FrameworkUtil.split(tempLine, FIELD_SEPARATOR);
            int numberOfFields = configuredFields.size();
            if (numberOfFields < NUMBER_OF_FIELDS_PER_LINE) {
                tmpMesage = "Configuration not defined properly at line number " + (lineCounter + 1);
                logMessage("parseConfiguration", tmpMesage);
                throw new Exception(tmpMesage);
            }

            classificationTypeFieldValue = (String) configuredFields.get(CLASSIFICATION_FIELD_INDEX);
            selectableFieldValue = (String) configuredFields.get(SELECTABLE_FIELD_INDEX);
            mandatoryFieldValue = (String) configuredFields.get(MANDATORY_FIELD_INDEX);
            allowedObjectsFieldValue = (String) configuredFields.get(ALLOWED_FIELD_INDEX);
            logMessage("parseConfiguration", "classificationTypeFieldValue " + classificationTypeFieldValue);
            logMessage("parseConfiguration", "selectableFieldValue " + selectableFieldValue);
            logMessage("parseConfiguration", "mandatoryFieldValue " + mandatoryFieldValue);
            logMessage("parseConfiguration", "allowedObjectsFieldValue " + allowedObjectsFieldValue);

            if (classificationTypeFieldValue == null || "".equals(classificationTypeFieldValue.trim())) {
                tmpMesage = "At line number " + (lineCounter + 1) + " Classification types field not defined properly";
                logMessage("parseConfiguration", tmpMesage);
                throw new Exception(tmpMesage);
            }
            if (selectableFieldValue == null || "".equals(selectableFieldValue.trim())) {
                tmpMesage = "At line number " + (lineCounter + 1) + " Selectable types field not defined properly";
                logMessage("parseConfiguration", tmpMesage);
                throw new Exception(tmpMesage);
            }
            if (mandatoryFieldValue == null || "".equals(mandatoryFieldValue.trim())) {
                tmpMesage = "At line number " + (lineCounter + 1) + " Mandatory types field not defined properly";
                logMessage("parseConfiguration", tmpMesage);
                throw new Exception(tmpMesage);
            }
            if (allowedObjectsFieldValue == null || "".equals(allowedObjectsFieldValue.trim())) {
                tmpMesage = "At line number " + (lineCounter + 1) + " Allowed objects field not defined properly";
                logMessage("parseConfiguration", tmpMesage);
                throw new Exception(tmpMesage);
            }

            if (CONSTANT_ONE.equalsIgnoreCase(selectableFieldValue)) {
                selectableFieldValue = "1";
            } else if (CONSTANT_MANY.equalsIgnoreCase(selectableFieldValue)) {
                selectableFieldValue = CONSTANT_MANY;
            } else {
                try {
                    Integer.valueOf(selectableFieldValue);
                } catch (NumberFormatException nfe) {
                    tmpMesage = "At line number " + (lineCounter + 1) + " selectable number of configurations " + "filed not defiend properly. Allowed values are <NUMBER> OR MANY OR ONE ";
                    logMessage("parseConfiguration", tmpMesage);
                    throw new Exception(tmpMesage);
                }
            }

            if (MANDATORY_YES_ONELETTER.equalsIgnoreCase(mandatoryFieldValue) || MANDATORY_YES.equalsIgnoreCase(mandatoryFieldValue)) {
                mandatoryFieldValue = MANDATORY_YES;
            } else if (MANDATORY_NO_ONELETTER.equalsIgnoreCase(mandatoryFieldValue) || MANDATORY_NO.equalsIgnoreCase(mandatoryFieldValue)) {
                mandatoryFieldValue = MANDATORY_NO;
            }

            logMessage("parseConfiguration", "classificationTypeFieldValue " + classificationTypeFieldValue);
            logMessage("parseConfiguration", "selectableFieldValue " + selectableFieldValue);
            logMessage("parseConfiguration", "mandatoryFieldValue " + mandatoryFieldValue);
            logMessage("parseConfiguration", "allowedObjectsFieldValue " + allowedObjectsFieldValue);

            HashMap configMap = new HashMap();
            configMap.put(SELECTABLE_KEY, selectableFieldValue);
            configMap.put(MANDATORY_KEY, mandatoryFieldValue);
            if (ALLOWED_OBJECTS_ALL.equalsIgnoreCase(allowedObjectsFieldValue)) {
                StringList allowedConfigObjectsList = new StringList();
                allowedConfigObjectsList.addElement(ALLOWED_OBJECTS_ALL);
                configMap.put(ALLOWEDCLASSIFICATIONS_KEY, allowedConfigObjectsList);
            } else {
                StringList allowedObjectList = FrameworkUtil.split(allowedObjectsFieldValue, ALLOWED_OBJECTS_SEPARATOR);
                configMap.put(ALLOWEDCLASSIFICATIONS_KEY, allowedObjectList);
            }

            logMessage("parseConfiguration", "Configuration Map for  " + classificationTypeFieldValue + " is - " + configMap);
            configurationTypes.add(classificationTypeFieldValue);
            returnConfigurationsMap.put(classificationTypeFieldValue, configMap);
        }
        if (returnConfigurationsMap.isEmpty()) {
            tmpMesage = "No Configuration Defined for CSE ::" + cseName;
            logMessage("parseConfiguration", tmpMesage);
            throw new Exception(tmpMesage);
        }
        logMessage("parseConfiguration", "END");
        return returnConfigurationsMap;
    }

    /**
     * Utility method for searching and reading CSE specific assignment tracker
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds username and csename
     * @throws Exception
     *             if the operation fails
     * @returns Map - Map with object details like type, name revision, attribute etc
     * @since Sourcing V6R2012x
     */
    private HashMap getClassificationAssignmentControlForCSE(String cseName) throws Exception {
        StringList objectSelects = new StringList();
        objectSelects.add(DomainObject.SELECT_TYPE);
        objectSelects.add(DomainObject.SELECT_NAME);
        objectSelects.add(DomainObject.SELECT_REVISION);
        objectSelects.add("attribute[" + MCADInteg_CLASSIFICATIONTYPE_CONFIGURATION + "].value");
        objectSelects.add("attribute[" + MCADInteg_CLASSIFICATIONATTRIBUTE_CONFIGURATION + "].value");

        String assignmentTrackerName = (String) CSE_CLASSIFICATION_ASSIGNMENT_MAPPING.get(cseName);
        if (assignmentTrackerName == null || "".equals(assignmentTrackerName)) {
            logMessage("getClassificationAssignmentControlForCSE", "CSE :" + cseName + " not supported for classification");
            throw new Exception("CSE :" + cseName + " not supported for classification");
        }

        MapList tempList = DomainObject.findObjects(context, // Context
                MCAD_CLASSIFICATION_ASSIGNMENT_TRACKER, // Type
                assignmentTrackerName, // Name
                SYMBOL_HYPHON, // Revision
                DomainConstants.QUERY_WILDCARD, // Owner
                DomainConstants.QUERY_WILDCARD, // Vault
                null, // Where expression
                true, // Expand Type
                objectSelects); // Selectable
        if (tempList == null || tempList.size() == 0) {
            logMessage("getClassificationAssignmentControlForCSE", "No MCD Classification Assignment control object " + "defined for CSE :" + cseName);
            throw new Exception("MCD Classification Assignment control object not found for CSE :" + cseName);
        }

        logMessage("getClassificationAssignmentControlForCSE", "Found " + tempList.size() + " Classification controlobjects for CSE  :" + cseName);
        return ((HashMap) tempList.get(0));
    }

    /**
     * Utility method for intializing classification assignment
     * @returns void
     * @since Sourcing V6R2012x
     */
    private void initialize() {

        if (WRITE_DEBUG_TO_RMI_LOGS && matrixLogger == null) {
            matrixLogger = new MatrixLogWriter(context);
        }
        // Update this as required.
        // Below variable defiens CSE to Classification configuration mapping
        // If CSE needs to be evaluated , then edit the following mappings
        CSE_CLASSIFICATION_ASSIGNMENT_MAPPING.put("MxPRO", "PRO Classification Assignment Control");
        CSE_CLASSIFICATION_ASSIGNMENT_MAPPING.put("MxUG", "NX Classification Assignment Control");
        logMessage("initialize", CSE_CLASSIFICATION_ASSIGNMENT_MAPPING.toString());

    }

    /**
     * Utility method for logging messages. Its a simple logging mechanism
     * @param method
     *            - method name
     * @param message
     *            - message to be logged
     * @returns void -
     * @since Sourcing V6R2012x
     */
    private void logMessage(String method, String message) {
        if (DEBUG) {
            String logMessage = new Date() + " ::: [ITIClassificationAssignment." + method + "] :::  " + message;
            if (WRITE_DEBUG_TO_RMI_LOGS && matrixLogger != null) {
                try {
                    matrixLogger.write(logMessage);
                    matrixLogger.flush();
                } catch (Exception e) {
                    System.out.println(logMessage);
                }
            } else {
                System.out.println(logMessage);
            }
        }
    }

    private static final String MCAD_CLASSIFICATION_ASSIGNMENT_TRACKER = PropertyUtil.getSchemaProperty("type_MCADClassificationAssignment");

    private static final String MCAD_CheckinEx_Classification_Holder = "";

    // PropertyUtil.getSchemaProperty("type_MCADCheckinExClassificationHolder");
    private static final String MCAD_CLASSIFICATION_POLICY = PropertyUtil.getSchemaProperty("policy_MCADClassficationPolicy");

    private static final String MCAD_CLASSIFIED_ITEM_RELATIONSHIP = PropertyUtil.getSchemaProperty("relationship_ClassifiedItem");

    // private static final String MCAD_CLASSIFICATION_ASSIGNMENT_TRACKER_REV = "-";
    private static final String MCAD_DERIVED_OUTPUT_TYPE = PropertyUtil.getSchemaProperty("type_DerivedOutput");

    private static final String MCADInteg_CLASSIFICATIONTYPE_CONFIGURATION = PropertyUtil.getSchemaProperty("attribute_MCADInteg-ClassificationType-Configuration");

    private static final String MCADInteg_CLASSIFICATIONATTRIBUTE_CONFIGURATION = PropertyUtil.getSchemaProperty("attribute_MCADInteg-ClassificationAttribute-Configuration");

    private static final String MCADInteg_CLASSIFICATIONXML_RECEIVEDFROMCSE = "";

    // PropertyUtil.getSchemaProperty("attribute_MCADInteg-ClassificationXML-ReceivedFromCSE");
    private static final String RELATIONSHIP_DERIVEDOUTPUT = PropertyUtil.getSchemaProperty("relationship_DerivedOutput");

    private static final String RELATIONSHIP_VIEWABLE = PropertyUtil.getSchemaProperty("relationship_Viewable");

    private static final String RELATIONSHIP_VERSIONOF = PropertyUtil.getSchemaProperty("relationship_VersionOf");

    private static final String MCAD_VIEWABLE = PropertyUtil.getSchemaProperty("type_Viewable");

    private static final String _superUser = PropertyUtil.getSchemaProperty("person_UserAgent");

    private static final String SYMBOL_HYPHON = "-";

    private static final String LINE_SEPARATOR = "\n";

    private static final String FIELD_SEPARATOR = "|";

    private static final String ALLOWED_OBJECTS_SEPARATOR = "~";

    private static final String ALLOWED_OBJECTS_ALL = "ALL";

    private static final String CONSTANT_MANY = "MANY";

    private static final String CONSTANT_ONE = "ONE";

    private static final String IGNORE_LINE_SYMBOL1 = "#";

    private static final String IGNORE_LINE_SYMBOL2 = "//";

    // Type Configurations
    private static final int NUMBER_OF_FIELDS_PER_LINE = 4;

    private static final int CLASSIFICATION_FIELD_INDEX = 0;

    private static final int SELECTABLE_FIELD_INDEX = 1;

    private static final int MANDATORY_FIELD_INDEX = 2;

    private static final int ALLOWED_FIELD_INDEX = 3;

    private static final String SELECTABLE_KEY = "allowednumber";

    private static final String MANDATORY_KEY = "mandatory";

    private static final String ALLOWEDCLASSIFICATIONS_KEY = "classification";

    // Attribute Configurations
    private static final int NUMBER_OF_ATTRIBUTE_FIELDS_PER_LINE = 5;

    private static final int CLASSIFICATION_OBJECTNAME_INDEX = 0;

    private static final int ATTRIBUTENAME_INDEX = 1;

    private static final int ATTRIBUTE_ORDER_INDEX = 2;

    private static final int ATTRIBUTE_DEFAULT_INDEX = 3;

    private static final int ATTRIBUTE_MANDATORY_INDEX = 4;

    private static final String ATTRIBUTE_NAME_KEY = "attributename";

    private static final String ATTRIBUTE_ORDER_KEY = "attributeorder";

    private static final String ATTRIBUTE_DEFAULT_KEY = "attributedefault";

    private static final String ATTRIBUTE_MANDATORY_KEY = "attributemandatory";

    private static final String ATTRIBUTE_DEFAULT_FROM_ENOVIA = "ENOVIAATRDEFAULT";

    private static final String MANDATORY_YES_ONELETTER = "Y";

    private static final String MANDATORY_NO_ONELETTER = "N";

    private static final String MANDATORY_YES = "YES";

    private static final String MANDATORY_NO = "NO";

    private static final HashMap CSE_CLASSIFICATION_ASSIGNMENT_MAPPING = new HashMap(2);

    private static final boolean DEBUG = true;

    private static final boolean WRITE_DEBUG_TO_RMI_LOGS = true;

    private static final boolean DO_NOT_ALLOW_CHECKIN_IF_NOCLASSIFICATION = false;

    private MatrixLogWriter matrixLogger = null;

    private Context context = null;

    private String userName = "";

    private String cseName = "";

    private ArrayList configurationTypes = new ArrayList();

    private HashMap classificationSpecificAttributeMap = null;
}
