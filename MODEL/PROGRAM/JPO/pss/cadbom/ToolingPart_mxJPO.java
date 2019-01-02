package pss.cadbom;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.engineering.EngineeringUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class ToolingPart_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ToolingPart_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    /**
     * This method is used to auto populate the description field from the listed attributes of context type
     * @param context
     * @param args
     *            -- Tooling Part Object ID is passed as an argument
     * @throws Exception
     *             returns -- nothing
     */
    public void populateAutoDescription(Context context, String args[]) throws Exception {
        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
        try {
            String strToolObjectId = args[0];
            String strEventName = args[1];
            String strCurrentState = args[2];
            String strPolicy = args[3];
            DomainObject domobjTooling = DomainObject.newInstance(context, strToolObjectId);
            if (UIUtil.isNullOrEmpty(strCurrentState)) {
                strCurrentState = domobjTooling.getInfo(context, DomainConstants.SELECT_CURRENT);
            }
            if (UIUtil.isNullOrEmpty(strPolicy)) {
                strPolicy = domobjTooling.getInfo(context, DomainConstants.SELECT_POLICY);
            }
            String strautoDescriptionAttributes = "";
            if (strPolicy.equals("PSS_Tool") && ((strEventName.endsWith("Modify") && strCurrentState.equals(TigerConstants.STATE_PSS_TOOL_INWORK))
                    || (strEventName.endsWith("Promote") && (strCurrentState.equals(TigerConstants.STATE_PSS_TOOL_REVIEW))))) {

                Map AttributeMap = domobjTooling.getAttributeMap(context, true);
                StringList lstselectStmts = new StringList(1);
                lstselectStmts.addElement(DomainConstants.SELECT_NAME);
                lstselectStmts.addElement(DomainConstants.SELECT_ID);
                lstselectStmts.addElement(DomainConstants.SELECT_ORGANIZATION);

                MapList mlobjMapList = domobjTooling.getRelatedObjects(context,

                        DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM, // relationship pattern......
                        TigerConstants.TYPE_GENERAL_CLASS, // object pattern
                        lstselectStmts, // object selects
                        null, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, 0);

                if (mlobjMapList.size() > 0) {

                    Map<String, String> mapGeneralClass = (Map<String, String>) mlobjMapList.get(0);
                    String strGeneralClassName = (String) mapGeneralClass.get(DomainConstants.SELECT_NAME);
                    // PHASE1.1 : TIGTK-9606 : START
                    strGeneralClassName = strGeneralClassName.trim().replace(" ", "_");
                    // PHASE1.1 : TIGTK-9606 : END
                    String strGeneralClassOrg = (String) mapGeneralClass.get(DomainConstants.SELECT_ORGANIZATION);
                    try {
                        strautoDescriptionAttributes = EnoviaResourceBundle.getProperty(context,
                                "emxEngineeringCentral.ToolingPart.PSS_AutoDescriptionField.AttributeList." + strGeneralClassOrg + "." + strGeneralClassName);
                    } catch (Exception e) {
                        strautoDescriptionAttributes = "";
                    }
                    if (UIUtil.isNotNullAndNotEmpty(strautoDescriptionAttributes)) {
                        StringList lstautoDesAttributesList = FrameworkUtil.split(strautoDescriptionAttributes, ",");
                        // int intAttributeListSize = lstautoDesAttributesList.size();
                        StringBuffer strAttributeValuesBuffer = new StringBuffer();

                        if ((lstautoDesAttributesList != null)) {
                            // Added for error found by find bug : 08/11/2016 : START
                            int intAttributeListSize = lstautoDesAttributesList.size();
                            // Added for error found by find bug : 08/11/2016 : END
                            for (int intIndex = 0; intIndex < intAttributeListSize; intIndex++) {
                                String strattributes = (String) lstautoDesAttributesList.get(intIndex);
                                if (strattributes.startsWith("attribute_")) {
                                    String strattribOrgName = PropertyUtil.getSchemaProperty(context, strattributes);
                                    String strattribValue = (String) AttributeMap.get(strattribOrgName);
                                    // TIGTK-9603 : START
                                    String attrNameForDisplay = i18nNow.getRangeI18NString(strattribOrgName, strattribValue, context.getSession().getLanguage());

                                    if (UIUtil.isNotNullAndNotEmpty(attrNameForDisplay) && !attrNameForDisplay.equalsIgnoreCase(TigerConstants.ATTR_VALUE_UNASSIGNED))
                                        strAttributeValuesBuffer.append(" " + attrNameForDisplay);
                                    // TIGTK-9603 : END
                                } else {
                                    String strconstants = (String) lstautoDesAttributesList.get(intIndex);
                                    strAttributeValuesBuffer.append(" " + strconstants);
                                }
                            } // end of for loop
                            String strdesValue = strAttributeValuesBuffer.toString();
                            domobjTooling.setDescription(context, strdesValue);
                        } // end of if
                    }
                }
            }

        } // end of try block
        catch (RuntimeException e) {
            logger.error("Error in populateAutoDescription: ", e);
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in populateAutoDescription: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        } // end of catch
        finally {
            ContextUtil.popContext(context);
        } // end of finally block
    }// end of method

    /**
     * Method to get the connected Parts to Tooling Part Object and Part to Tooling Objcet.
     * @param context
     * @param args
     * @return MapList - Contains the "Object Id" and "Relationship Id" of Parts which are connected to Tooling Part Object and vise versa.
     * @throws Exception
     */
    public MapList getConnectedObjects(Context context, String[] args) throws Exception {
        MapList mlConnectedPartsList = null;
        Boolean bToSide = false;
        Boolean bFromSide = false;
        String FROM = "from";

        HashMap<String, String> programMap = (HashMap) JPO.unpackArgs(args);

        String strToolObjectId = (String) programMap.get("objectId");
        String strTypeName = (String) programMap.get("PSS_TypeName");
        String strRelName = (String) programMap.get("PSS_RelName");
        String strDirection = (String) programMap.get("PSS_Direction");

        final String RELATIONSHIP_PSS_PARTTOOL = PropertyUtil.getSchemaProperty(context, strRelName);
        final String TYPE_NAME = PropertyUtil.getSchemaProperty(context, strTypeName);

        DomainObject domToolObject = DomainObject.newInstance(context, strToolObjectId);
        StringList lstSelectStmts = new StringList();
        StringList lstRelStmts = new StringList();

        lstSelectStmts.add(DomainConstants.SELECT_ID);
        lstRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

        if (strDirection.equalsIgnoreCase(FROM)) {

            bFromSide = true;
            mlConnectedPartsList = domToolObject.getRelatedObjects(context, RELATIONSHIP_PSS_PARTTOOL, TYPE_NAME, lstSelectStmts, lstRelStmts, bFromSide, bToSide, (short) 1, null, null, 0);

        } else {
            bToSide = true;
            mlConnectedPartsList = domToolObject.getRelatedObjects(context, RELATIONSHIP_PSS_PARTTOOL, TYPE_NAME, lstSelectStmts, lstRelStmts, bFromSide, bToSide, (short) 1, null, null, 0);

        }

        return mlConnectedPartsList;

    }

    /**
     * Method to promote Tooling part to obsolete state when its previous revision promoted to Released.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */

    public String obsoletePreviousRevision(Context context, String[] args) throws Exception {
        try {
            String objectId = args[0];

            DomainObject domToolingObject = new DomainObject(objectId);
            BusinessObject boPreviousRevision = domToolingObject.getPreviousRevision(context);

            if (boPreviousRevision.exists(context)) {
                // Getting previous revision ID
                String strPreviousRevID = boPreviousRevision.getObjectId(context);
                // DomainObject domToolObject = new DomainObject(strPreviousRevID);
                BusinessObject busObj = new BusinessObject(strPreviousRevID);
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                busObj.promote(context);
                ContextUtil.popContext(context);

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in obsoletePreviousRevision: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }

        return null;

    }

    /**
     * Method called on the post process functionality of Remove Tooling Part depending on direction as "To" or "From" Method makes check whether Tooling part which is to be removed from part which is
     * connected to another part or not or vise versa.
     * @param context
     * @param args
     * @return MapList - Contains the "Object Id" and "Relationship Id" of Parts which are connected to Tooling Part Object and vise versa.
     * @throws Exception
     */

    public String removeConnectedItems(Context context, String[] args) throws Exception {
        String StrStatus = "";
        try {
            String LASTITEM = "LastItem";
            String SUCCESS = "Success";

            int sNumberOfParts;

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");

            String strDirection = (String) programMap.get("direction");
            String strRelId = (String) programMap.get("relId");
            sNumberOfParts = (int) programMap.get("sNumberOfParts");
            String stremxTableRowId[] = (String[]) programMap.get("emxTableRowId");

            // Findbug Issue correction start
            // Date: 22/03/2017
            // By: Asha G.
            MapList mlConnectedPartList = null;
            // Findbug Issue correction

            if (strDirection.equalsIgnoreCase("To")) {
                DomainObject toolDomObj = DomainObject.newInstance(context, strObjectId);
                StringList slSelectStmts = new StringList();
                StringList slRelStmts = new StringList();

                slSelectStmts.add(DomainConstants.SELECT_ID);
                slRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

                mlConnectedPartList = toolDomObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTTOOL, DomainConstants.TYPE_PART, slSelectStmts, slRelStmts, true, false, (short) 0,
                        null, null, 0);

                int mSize = mlConnectedPartList.size();

                if (mSize > sNumberOfParts) {

                    if (stremxTableRowId != null) {
                        String relIdList[] = new String[stremxTableRowId.length];

                        for (int i = 0; i < stremxTableRowId.length; i++) {
                            StringTokenizer st = new StringTokenizer(stremxTableRowId[i], "|");
                            String sRelId = st.nextToken();

                            relIdList[i] = sRelId;
                        }
                        DomainRelationship.disconnect(context, relIdList);

                    }
                    StrStatus = SUCCESS;
                    return StrStatus;

                }
                if (mSize == sNumberOfParts) {
                    StrStatus = LASTITEM;
                    return StrStatus;
                }

            }
            if (strDirection.equalsIgnoreCase("From")) {

                StringList slSelectStmts = new StringList();
                StringList slRelStmts = new StringList();
                slSelectStmts.add(DomainConstants.SELECT_ID);
                slRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

                // PPDM : TIGTK-6675 : PSE : 29-06-2017 : START
                String sToolPartId = (String) programMap.get("strSelectedObjectId");
                // PPDM : TIGTK-6675 : PSE : 29-06-2017 : END
                DomainObject domToolObj = DomainObject.newInstance(context, sToolPartId);
                mlConnectedPartList = domToolObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PARTTOOL, DomainConstants.TYPE_PART, slSelectStmts, slRelStmts, true, false, (short) 0,
                        null, null, 0);

                int mSize = mlConnectedPartList.size();

                if (mSize > 1) {

                    DomainRelationship.disconnect(context, strRelId);
                    StrStatus = SUCCESS;
                    return StrStatus;
                }
                if (mSize == 1) {

                    StrStatus = LASTITEM;
                    return StrStatus;
                }

            }
        }
        // end of try block
        catch (Exception e) {
            StrStatus = MqlUtil.mqlCommand(context, "notice $1", e.getMessage());
        } // end of catch

        return StrStatus;
    }

    /**
     * To connect the General Class to the Tooling getting created.
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public Object connectToolingToGeneralClass(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");

        String strToolId = (String) paramMap.get("objectId");

        String newGeneralClassIds = (String) paramMap.get("New OID");

        // String strGeneralClassRelationship = RELATIONSHIP_CLASSIFIED_ITEM;
        StringList newGeneralClassList = FrameworkUtil.split(newGeneralClassIds, ",");

        DomainObject doToolObj = DomainObject.newInstance(context, strToolId);
        if ((newGeneralClassList != null) && !newGeneralClassList.isEmpty()) {
            try {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

                // construct array of ids
                if (newGeneralClassList.size() > 0) {

                    Iterator generalClassItr = newGeneralClassList.iterator();
                    while (generalClassItr.hasNext()) {
                        String newGeneralClass = (String) generalClassItr.next();
                        doToolObj.setId(strToolId);
                        DomainObject domainObjectFromType = DomainObject.newInstance(context, newGeneralClass);

                        DomainRelationship.connect(context, domainObjectFromType, DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM, doToolObj);
                    }
                }
            } catch (Exception e) {
                // TIGTK-5405 - 11-04-2017 - VB - START
                logger.error("Error in connectToolingToGeneralClass: ", e);
                // TIGTK-5405 - 11-04-2017 - VB - END
                throw e;
            } finally {
                ContextUtil.popContext(context);
            }
        }

        return Boolean.TRUE;
    }

    /**
     * // TIGTK-10271 : START // TIGTK-10271 : END This method checks if atleast one General Class is connected to the Tooling object
     * @param context
     * @param args
     *            --Object Id of Part
     * @return -- '0'if success...'1' for failure with error message
     * @throws Exception
     */
    public int checkGeneralClass(Context context, String[] args) throws Exception {
        String strToolingObjectID = args[0];
        DomainObject domToolingObject = DomainObject.newInstance(context, strToolingObjectID);
        StringList lstselectStmts = new StringList(1);
        lstselectStmts.addElement(DomainConstants.SELECT_TYPE);

        MapList mlobjMapList = domToolingObject.getRelatedObjects(context,

                DomainConstants.RELATIONSHIP_CLASSIFIED_ITEM, // relationship pattern
                TigerConstants.TYPE_GENERAL_CLASS, // object pattern
                lstselectStmts, // object selects
                null, // relationship selects
                true, // to direction
                false, // from direction
                (short) 1, // recursion level
                null, // object where clause
                null, 0);

        if (mlobjMapList.isEmpty()) // check whether General Class is connected..
        {

            String strAlertMessage = EngineeringUtil.i18nStringNow(context, "emxEngineeringCentral.Alert.PSS_GeneralClassNotConnectedToTool", context.getSession().getLanguage());
            // emxContextUtil_mxJPO.mqlNotice(context, strAlertMessage);
            MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
            return 1;

        } else {
            return 0;
        }

    }

}
