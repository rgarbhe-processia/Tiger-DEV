package pss.ecm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.common.Route;
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

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.db.RelationshipList;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;
import java.util.Set;

public class enoECMChange_mxJPO {
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(enoECMChange_mxJPO.class);

    public static final String REL_PSS_SUPPORTING_DOCUMENT = PropertyUtil.getSchemaProperty("relationship_PSS_SupportingDocument");

    // TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1 | 23/02/2017 |Harika Varanasi : Starts
    public LinkedHashMap<String, String> lhmCOSelectionStore = new LinkedHashMap<String, String>();

    // TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1 | 23/02/2017 |Harika Varanasi : Ends

    // TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2 | 06/03/2017 |Harika Varanasi : Starts
    public LinkedHashMap<String, String> lhmCRGenericSelectionStore = new LinkedHashMap<String, String>();

    public LinkedHashMap<String, String> lhmCRImpactSelectionStore = new LinkedHashMap<String, String>();

    // TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2 | 06/03/2017 |Harika Varanasi : Ends

    // PCM : TIGTK-8085 : PSE : 06-06-2017 : START
    public LinkedHashMap<String, String> lhmCRCommentsSelectionStore = new LinkedHashMap<String, String>();
    // PCM : TIGTK-8085 : PSE : 06-06-2017 : END

    // PCM : TIGTK-11455 : SayaliD : 22-Nov-2017 : START
    public LinkedHashMap<String, String> lhmCACommentsSelectionStore = new LinkedHashMap<String, String>();

    public LinkedHashMap<String, String> lhmCARejectCommentsSelectionStore = new LinkedHashMap<String, String>();
    // PCM : TIGTK-11455 : SayaliD : 22-Nov-2017 : END

    // PCM : TIGTK-8914 : 07/07/2017 : AM : declared a new variable for CR Rejection Commentnt
    public LinkedHashMap<String, String> lhmCRRejectSelectionStore = new LinkedHashMap<String, String>();

    // TGPSS_PCM_TS_153_ChangeActionsNotifications_V2.1 | 16/03/2017 |Harika Varanasi : Starts
    public LinkedHashMap<String, String> lhmCASelectionStore = new LinkedHashMap<String, String>();

    // TGPSS_PCM_TS_153_ChangeActionsNotifications_V2.1 | 16/03/2017 |Harika Varanasi : Ends
    public LinkedHashMap<String, String> lhmIAGenericSelectionStore = new LinkedHashMap<String, String>();

    /**
     * This method is used for Connect Sketch Document And MCO
     * @param context
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public void connectToChangeObject(Context context, String[] args) throws Exception {
        try {
            HashMap progMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) progMap.get("paramMap");
            HashMap requestMap = (HashMap) progMap.get("requestMap");

            // Get the Sketch Document & MCO Id
            String strSketchId = (String) paramMap.get("objectId");
            String strMCOId = (String) requestMap.get("parentOID");
            DomainObject domSketchId = DomainObject.newInstance(context, strSketchId);
            DomainObject domMCOId = DomainObject.newInstance(context, strMCOId);

            // Set the Sketch Document Title
            HashMap<String, String> attributeMap = new HashMap<String, String>(1);
            attributeMap.put(DomainConstants.ATTRIBUTE_TITLE, domSketchId.getName(context));
            // PCM TIGTK-3231 | 29/09/16 : Pooja Mantri : Start
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            domSketchId.setAttributeValues(context, attributeMap);

            // Connect MCO & Sketch Document with Relationship
            if (UIUtil.isNotNullAndNotEmpty(strMCOId)) {
                DomainRelationship.connect(context, domMCOId, REL_PSS_SUPPORTING_DOCUMENT, domSketchId);
                ContextUtil.popContext(context);
                // PCM TIGTK-3231 | 29/09/16 : Pooja Mantri : End
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectToChangeObject: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    // PCM:TIGTK-4565 | 20/02/17 | Pooja Mantri : Start
    /**
     * This method is called from Table PSS_AIProgramProjectInfoTable. It is used to fetch "Lead Change Manager" connected to Program Project
     * @param context
     * @param args
     * @return -- StringList -- Containing "Lead Change Manager" connected to Program Project
     * @throws Exception
     */
    public StringList getLeadChangeManager(Context context, String[] args) throws Exception {

        StringList slLeadChangeManager = new StringList();
        StringList objectSelect = new StringList(DomainConstants.SELECT_NAME);

        StringBuffer relWhere = new StringBuffer();
        relWhere.append("attribute[");
        relWhere.append(TigerConstants.ATTRIBUTE_PSS_POSITION);
        relWhere.append("]");
        relWhere.append(" == ");
        relWhere.append("Lead");
        relWhere.append(" && ");
        relWhere.append("attribute[");
        relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
        relWhere.append("]");
        relWhere.append(" == '");
        // TIGTK-5890 : PTE : 4/7/2017 : START
        relWhere.append(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
        // TIGTK-5890 : PTE : 4/7/2017 : END
        relWhere.append("'");
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) programMap.get("objectList");
            for (int i = 0; i < mlObjectList.size(); i++) {
                Map mapObject = (Map) mlObjectList.get(i);
                String objectId = (String) mapObject.get(DomainConstants.SELECT_ID);

                DomainObject domObject = new DomainObject(objectId);
                String type = domObject.getInfo(context, DomainObject.SELECT_TYPE);

                if (TigerConstants.TYPE_PSS_PROGRAMPROJECT.equals(type)) {

                    MapList mlLeadCM = getMembersFromProgram(context, domObject, objectSelect, DomainConstants.EMPTY_STRINGLIST, DomainConstants.EMPTY_STRING, relWhere.toString());
                    if (mlLeadCM.size() > 0) {
                        Map mapLeadCMDetails = (Map) mlLeadCM.get(0);
                        slLeadChangeManager.add(mapLeadCMDetails.get(DomainObject.SELECT_NAME));
                        // PCM : TIGTK-8107 : 04/07/2017 : AB : START
                    } else {
                        slLeadChangeManager.add("");
                        // PCM : TIGTK-8107 : 04/07/2017 : AB : END
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error in getLeadChangeManager in JPO pss.ecm.enoECMChange: ", ex);
        }
        return slLeadChangeManager;

    }

    /**
     * This Method is called from getChangeManagers and getLeadChangeManager. It is a common method to fetch Program_Project Members based on selectStmts.
     * @param context
     * @param domProgram
     * @param objectSelect
     * @param relSelet
     * @param objWhere
     * @param relWhere
     * @return -- MapList -- Containing Members connected to Program Project
     * @throws Exception
     */
    public MapList getMembersFromProgram(Context context, DomainObject domProgram, StringList objectSelect, StringList relSelect, String objWhere, String relWhere) throws Exception {
        MapList mlMemberList = new MapList();
        try {
            mlMemberList = domProgram.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, DomainObject.TYPE_PERSON, objectSelect, relSelect, false, true, (short) 0, objWhere,
                    relWhere, 0);
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getMembersFromProgram: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return mlMemberList;
    }

    /**
     * This method is called from Table PSS_AIProgramProjectInfoTable. It is used to fetch "Change Managers" connected to Program Project
     * @param context
     * @param args
     * @return -- StringList -- Containing "Change Managers" connected to Program Project
     * @throws Exception
     */
    public StringList getChangeManagers(Context context, String[] args) throws Exception {
        StringList slChangeManagers = new StringList();
        StringList objectSelect = new StringList(DomainConstants.SELECT_NAME);

        StringBuffer relWhere = new StringBuffer();
        relWhere.append("attribute[");
        relWhere.append(TigerConstants.ATTRIBUTE_PSS_POSITION);
        relWhere.append("]");
        relWhere.append(" != ");
        relWhere.append("Lead");
        relWhere.append(" && ");
        relWhere.append("attribute[");
        relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
        relWhere.append("]");
        relWhere.append(" == '");
        // TIGTK-5890 : PTE : 4/7/2017 : START
        relWhere.append(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
        // TIGTK-5890 : PTE: 4/7/2017 : END
        relWhere.append("'");
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) programMap.get("objectList");
            for (int i = 0; i < mlObjectList.size(); i++) {
                Map mapObject = (Map) mlObjectList.get(i);
                String objectId = (String) mapObject.get(DomainConstants.SELECT_ID);

                DomainObject domObject = new DomainObject(objectId);

                String type = domObject.getInfo(context, DomainObject.SELECT_TYPE);

                if (TigerConstants.TYPE_PSS_PROGRAMPROJECT.equals(type)) {

                    MapList mlLeadCM = getMembersFromProgram(context, domObject, objectSelect, DomainConstants.EMPTY_STRINGLIST, DomainConstants.EMPTY_STRING, relWhere.toString());
                    StringList slIntermediate = new StringList();
                    for (int j = 0; j < mlLeadCM.size(); j++) {
                        Map mapLeadCMDetails = (Map) mlLeadCM.get(j);

                        slIntermediate.add(mapLeadCMDetails.get(DomainObject.SELECT_NAME));
                    }
                    slChangeManagers.add(FrameworkUtil.join(slIntermediate, ","));
                }
            }
        } catch (Exception ex) {
            logger.error("Error in getChangeManagers  in JPO pss.ecm.enoECMChange: ", ex);
        }
        return slChangeManagers;

    }

    /**
     * This Method is used to populate CRs/ COs related to Program-Project Object. It is ProgramHTMLOutput. PCM : TIGTK-8581 : 19/06/2017 : AB
     * @param context
     * @param args
     * @return -- StringList -- Containing CR/CO List related to Program-Project Object
     * @throws Exception
     */
    public StringList getCRsCOsRelatedToAI(Context context, String[] args) throws Exception {
        StringList slRelatedChangeToAI = new StringList();

        try {
            StringList slChangeConnectedToItem = null;
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonNotObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            Map programMap = (Map) JPO.unpackArgs(args);
            Map columnMap = (Map) programMap.get("columnMap");
            Map settings = (Map) columnMap.get("settings");

            String strFlagType = (String) settings.get("typeFlag");
            MapList mlObjectList = (MapList) programMap.get("objectList");

            String strObjectType = "";

            StringList objectSelect = new StringList();
            objectSelect.add(DomainObject.SELECT_NAME);
            objectSelect.add(DomainObject.SELECT_ID);
            objectSelect.add(DomainObject.SELECT_CURRENT);

            for (int i = 0; i < mlObjectList.size(); i++) {
                Map mapObject = (Map) mlObjectList.get(i);
                String objectId = (String) mapObject.get(DomainConstants.SELECT_ID);
                String objectIdParent = (String) mapObject.get("id[parent]");
                DomainObject domObject = new DomainObject(objectId);
                DomainObject domItem = new DomainObject(objectIdParent);

                String type = domObject.getInfo(context, DomainObject.SELECT_TYPE);

                if (TigerConstants.TYPE_PSS_PROGRAMPROJECT.equals(type)) {
                    if ("CR".equals(strFlagType)) {
                        strObjectType = TigerConstants.TYPE_PSS_CHANGEREQUEST;
                        DomainObject.MULTI_VALUE_LIST.add("to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.id");
                        StringList slSelects = new StringList("to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.id");
                        Map objectMap = domItem.getInfo(context, slSelects);
                        slChangeConnectedToItem = commonNotObj.getStringListFromMap(context, objectMap, "to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.id");
                    } else {
                        strObjectType = TigerConstants.TYPE_PSS_CHANGEORDER;
                        DomainObject.MULTI_VALUE_LIST.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
                        StringList slSelects = new StringList("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
                        Map objectMap = domItem.getInfo(context, slSelects);
                        slChangeConnectedToItem = commonNotObj.getStringListFromMap(context, objectMap,
                                "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
                    }

                    MapList mlConnectedChangeList = domObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, strObjectType, objectSelect,
                            DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);

                    StringList slIntermediate = new StringList();

                    for (int j = 0; j < mlConnectedChangeList.size(); j++) {
                        Map tempMap = (Map) mlConnectedChangeList.get(j);
                        String name = (String) tempMap.get(DomainObject.SELECT_NAME);
                        String id = (String) tempMap.get(DomainObject.SELECT_ID);
                        String current = (String) tempMap.get(DomainObject.SELECT_CURRENT);

                        if (slChangeConnectedToItem.contains(id)) {
                            StringBuffer output = new StringBuffer(" ");
                            output.append("<tr><td width=\"60%\">");
                            output.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                            output.append(id);
                            output.append("', '700', '600', 'false', 'popup', '')\" class=\"object\">");
                            output.append(" " + name);
                            output.append("</a></td>");
                            output.append("<td width=\"40%\">");
                            output.append(current);
                            output.append("</td>");
                            output.append("</tr>");
                            if (!slRelatedChangeToAI.contains(output.toString())) {
                                slIntermediate.add(output.toString());
                            }
                        }
                    }

                    slRelatedChangeToAI.add("<table border=\"3\">" + FrameworkUtil.join(slIntermediate, "") + "</table>");
                } else {
                    slRelatedChangeToAI.add("");
                }

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCRsCOsRelatedToAI: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return slRelatedChangeToAI;

    }

    /**
     * This method is used as Expand Program on Table PSS_AffectedItemsProgramProjectInfoTable. It is used to display ProgramProject Objects
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public MapList expandAffectedItems(Context context, String[] args) throws Exception {
        MapList mlProgramProject = new MapList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domAffectedItem = DomainObject.newInstance(context, strObjectId);

            if (domAffectedItem.isKindOf(context, DomainObject.TYPE_PART)) {
                MapList mlPartListEBOM = getEBOMForPart(context, strObjectId);
                MapList mListProgramProject = getProgramProjectsForPart(context, mlPartListEBOM);
                if (mListProgramProject != null && mListProgramProject.size() > 0) {
                    mlProgramProject.addAll(mListProgramProject);
                }
            }
            if (domAffectedItem.isKindOf(context, DomainConstants.TYPE_CAD_MODEL) || domAffectedItem.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {
                StringBuffer relPattern = new StringBuffer();
                relPattern.append("to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.id");
                StringList slConnectedPart = domAffectedItem.getInfoList(context, relPattern.toString());

                for (int j = 0; j < slConnectedPart.size(); j++) {
                    String strPartId = (String) slConnectedPart.get(j);
                    MapList mlPartList = getEBOMForPart(context, strPartId);
                    MapList mListProgramProject = getProgramProjectsForPart(context, mlPartList);
                    if (mListProgramProject != null && mListProgramProject.size() > 0) {
                        mlProgramProject.addAll(mListProgramProject);
                    }
                } // end for loop
            } // end if loop
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in expandAffectedItems: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return mlProgramProject;
    }

    /**
     * This method is used as Expand Program on Table PSS_AffectedItemsProgramProjectInfoTable. It is used to display ProgramProject Objects
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public MapList getProgramProjectsForPart(Context context, MapList mlPartList) throws Exception {

        MapList mListProgramProject = new MapList();
        MapList mListRepetedIntermediate = new MapList();
        StringList slProgramProject = new StringList();
        try {
            StringList busSelects = new StringList();
            busSelects.add(DomainObject.SELECT_ID);
            busSelects.add(DomainObject.SELECT_TYPE);
            busSelects.add(DomainObject.SELECT_NAME);

            Iterator itrPart = mlPartList.iterator();
            while (itrPart.hasNext()) {
                Map mPart = (Map) itrPart.next();
                String typeStr = (String) mPart.get(DomainConstants.SELECT_TYPE);
                if (UIUtil.isNotNullAndNotEmpty(typeStr) && typeStr.equals(TigerConstants.TYPE_HARDWARE_PRODUCT)) {
                    String strProductId = (String) mPart.get(DomainConstants.SELECT_ID);
                    if (UIUtil.isNotNullAndNotEmpty(strProductId)) {

                        // PCM : TIGTK-8107 : 25/05/2017 : AB : START
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        DomainObject domProduct = DomainObject.newInstance(context, strProductId);
                        // PCM : TIGTK-7315 : 24/05/2017 : AB : START
                        MapList mlIntermediateList = domProduct.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT, // relationship pattern
                                TigerConstants.TYPE_PSS_PROGRAMPROJECT, // object pattern
                                busSelects, // object selects
                                null, // relationship selects
                                true, // to direction
                                false, // from direction
                                (short) 1, // recursion level
                                null, // object where clause
                                null, (short) 0, false, // checkHidden
                                true, // preventDuplicates
                                (short) 0, // pageSize
                                null, // Product Display
                                null, null, null, null);

                        ContextUtil.popContext(context);
                        // PCM : TIGTK-8107 : 25/05/2017 : AB : END

                        // PCM : TIGTK-8107 : 04/07/2017 : AB : END
                        if (!mlIntermediateList.isEmpty() && mlIntermediateList.size() > 0) {
                            for (int i = 0; i < mlIntermediateList.size(); i++) {
                                Map mapIntermediateList = (Map) mlIntermediateList.get(i);
                                String strNameProgramProject = (String) mapIntermediateList.get(DomainConstants.SELECT_NAME);
                                if (slProgramProject.contains(strNameProgramProject)) {
                                    mListRepetedIntermediate.add(mlIntermediateList.get(i));
                                } else {
                                    slProgramProject.add(strNameProgramProject);
                                }
                            }
                            mlIntermediateList.removeAll(mListRepetedIntermediate);
                            mListProgramProject.addAll(mlIntermediateList);
                        }
                        // PCM : TIGTK-8107 : 04/07/2017 : AB : END
                        // PCM : TIGTK-7315 : 24/05/2017 : AB : END

                    } // end if loop
                }
            } // end while loop

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getProgramProjectsForPart: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return mListProgramProject;

    }

    /**
     * This method is used as Expand Program on Table PSS_AffectedItemsProgramProjectInfoTable. It is used to display ProgramProject Objects
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public MapList getEBOMForPart(Context context, String strAffectedItemId) throws Exception {
        MapList mlReturnList = new MapList();
        try {
            DomainObject domPartObj = DomainObject.newInstance(context, strAffectedItemId);
            StringList busSelects = new StringList();
            busSelects.add(DomainObject.SELECT_ID);
            busSelects.add(DomainObject.SELECT_TYPE);
            busSelects.add(DomainObject.SELECT_NAME);
            Pattern typePattern = new Pattern(DomainConstants.TYPE_PART);
            typePattern.addPattern(TigerConstants.TYPE_PRODUCTS);

            Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_EBOM);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_GBOM);

            // PCM : TIGTK-8107 : 25/05/2017 : AB : START
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            MapList mlParts = domPartObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    busSelects, // object selects
                    null, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    null, // Product Display
                    null, null, null, null);
            // PCM : TIGTK-8744 : 28/06/2017 : PTE : START
            Iterator itrlist = mlParts.iterator();
            while (itrlist.hasNext()) {
                Map mPart = (Map) itrlist.next();
                String strObjId = (String) mPart.get(DomainConstants.SELECT_ID);
                if (UIUtil.isNotNullAndNotEmpty(strObjId) && !mlReturnList.contains(strObjId)) {
                    mlReturnList.add(mPart);

                }
            }
            // PCM : TIGTK-8744 : 28/06/2017 : PTE : END

            ContextUtil.popContext(context);
            // PCM : TIGTK-8107 : 25/05/2017 : AB : END

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getEBOMForPart: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mlReturnList;

    }

    /**
     * This Method is used to populate IsGoverning Value. It is ProgramHTMLOutput.
     * @param context
     * @param args
     * @return -- StringList -- Containing CR/CO List related to Program-Project Object
     * @throws Exception
     */
    public StringList getIsGoverningProjectValue(Context context, String[] args) throws Exception {
        StringList slReturn = new StringList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) programMap.get("objectList");

            String strOutput = DomainConstants.EMPTY_STRING;

            for (int i = 0; i < mlObjectList.size(); i++) {
                Map mapObject = (Map) mlObjectList.get(i);

                String objectId = (String) mapObject.get(DomainConstants.SELECT_ID);
                DomainObject domObject = new DomainObject(objectId);
                String type = domObject.getInfo(context, DomainObject.SELECT_TYPE);
                String StrParentID = (String) mapObject.get("id[parent]");
                if (TigerConstants.TYPE_PSS_PROGRAMPROJECT.equals(type)) {

                    DomainObject domPart = DomainObject.newInstance(context, StrParentID);
                    String strGoverning = domPart.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                    if (UIUtil.isNotNullAndNotEmpty(strGoverning) && strGoverning.equals(objectId)) {
                        strOutput = "Governing";
                    } else {
                        strOutput = "Impacted";
                    }

                }
                slReturn.add(strOutput);

            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getIsGoverningProjectValue: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return slReturn;

    }

    // TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1 | 23/02/2017 |Harika Varanasi : Starts

    /**
     * getChangeOrderInformation method is used to get all information about PSS_ChangeOrder As a Part of TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1
     * @param context
     * @param strIssueId
     * @return Map
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public Map getChangeOrderInformation(Context context, String strCOId) throws Exception {
        Map mapCO = new HashMap();
        try {

            if (UIUtil.isNotNullAndNotEmpty(strCOId)) {
                DomainObject domCOObj = DomainObject.newInstance(context);
                domCOObj.setId(strCOId);

                StringList slObjectSelects = new StringList(13);
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                slObjectSelects.addElement(DomainConstants.SELECT_NAME);
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
                slObjectSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE + "]");
                slObjectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].from.name");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].to.id");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].to.name");

                // PCM : TIGTK-7745 : 01/09/2017 : AB : START
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].to.name");
                DomainConstants.MULTI_VALUE_LIST.add("to[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].from.id");
                DomainConstants.MULTI_VALUE_LIST.add("to[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].from.name");
                // PCM : TIGTK-7745 : 01/09/2017 : AB : END
                mapCO = domCOObj.getInfo(context, slObjectSelects);
                String strCurrent = (String) mapCO.get("current");
                if (strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL)) {
                    mapCO.put("current", TigerConstants.STATE_INREVIEW_CR);
                }
            }

        } catch (RuntimeException re) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeOrderInformation: ", re);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeOrderInformation: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        } finally {
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].to.id");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].to.name");
            DomainConstants.MULTI_VALUE_LIST.remove("to[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].from.id");
            DomainConstants.MULTI_VALUE_LIST.remove("to[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].from.name");
        }

        return mapCO;
    }

    /**
     * transformCOMapToHTMLList As a Part of TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1
     * @param context
     * @param objectMap
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public MapList transformCOMapToHTMLList(Context context, Map objectMap, String strBaseURL) throws Exception {
        MapList mlInfoList = new MapList();
        try {

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);

            StringList slHyperLinkLabelKey = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.CONotification.HyperLinkLabelKey"), ",");
            StringList slHyperLinkLabelKeyIds = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.CONotification.HyperLinkLabelKeyIds"), ",");
            initializeCOLinkedHashMap();
            mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCOSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in transformCOMapToHTMLList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mlInfoList;
    }

    /**
     * initializeCOLinkedHashMap As a Part of TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1
     * @throws Exception
     *             if the operation fails
     * @author Harika Varanasi : SteepGraph
     */
    public void initializeCOLinkedHashMap() throws Exception {
        try {

            if (lhmCOSelectionStore != null && (lhmCOSelectionStore.isEmpty())) {

                lhmCOSelectionStore.put("Title", "SectionHeader");
                lhmCOSelectionStore.put("Subject", "SectionSubject");
                lhmCOSelectionStore.put("Main_Information", "SectionHeader");
                lhmCOSelectionStore.put("Project_Code", "to[PSS_ConnectedPCMData].from.name");
                lhmCOSelectionStore.put("Project_Description", "to[PSS_ConnectedPCMData].from.description");
                lhmCOSelectionStore.put("CO", "name");
                lhmCOSelectionStore.put("CO_Description", "description");
                lhmCOSelectionStore.put("State", "current");
                lhmCOSelectionStore.put("Virtual_Implementation_Planned_Date", "attribute[PSS_COVirtualImplementationDate]");
                lhmCOSelectionStore.put("CO_Creator", "attribute[Originator]");
                // TIGTK-7580 : START
                lhmCOSelectionStore.put("Comment", "TransferComments");
                // TIGTK-7580 : END
                lhmCOSelectionStore.put("Useful_Links", "SectionHeader");
                lhmCOSelectionStore.put("Related_Content", "from[Change Action].to.name");
                lhmCOSelectionStore.put("Related_CR_Content", "to[Change Order].from.name");
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeCOLinkedHashMap: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }

    /**
     * getCONotificationBodyHTML method is used to CO messageHTML in Notification Object As apart of TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getCONotificationBodyHTML(Context context, String[] args) throws Exception {
        String messageHTML = "";
        boolean bIsCOInWorkNotificaton = false;
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strIssueObjId = (String) programMap.get("id");
            String strBaseURL = (String) programMap.get("baseURL");
            String notificationObjName = (String) programMap.get("notificationName");
            StringList busSelects = new StringList("attribute[Subject Text]");

            Map issueMap = (Map) getChangeOrderInformation(context, strIssueObjId);

            String strSubjectKey = "";
            MapList mpNotificationList = DomainObject.findObjects(context, "Notification", notificationObjName, "*", "*", TigerConstants.VAULT_ESERVICEADMINISTRATION, "", null, true, busSelects,
                    (short) 0);

            if (mpNotificationList != null && (!mpNotificationList.isEmpty())) {
                strSubjectKey = (String) ((Map) mpNotificationList.get(0)).get("attribute[Subject Text]");

            }
            if (UIUtil.isNotNullAndNotEmpty(strSubjectKey)) {
                issueMap.put("SectionSubject", EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strSubjectKey));
            } else {
                issueMap.put("SectionSubject", "");
            } // TIGTK-7580 : START
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_TransferOwnershipNotification".equalsIgnoreCase(notificationObjName)) {
                Map payLoadMap = (Map) programMap.get("payload");
                if (payLoadMap != null) {
                    if (payLoadMap.containsKey("TransferComments")) {
                        String strComments = (String) payLoadMap.get("TransferComments");
                        issueMap.put("TransferComments", strComments);
                    }
                }
                strSubjectKey = getTranferOwnershipSubject(context, args);
                issueMap.put("SectionSubject", strSubjectKey);
            } else {
                issueMap.put("TransferComments", "");
            }
            // TIGTK-7580:END
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_COInWorkNotification".equalsIgnoreCase(notificationObjName)) {
                PropertyUtil.setGlobalRPEValue(context, "PSS_NOTIFICATION_NAME", "PSS_COInWorkNotification");
                bIsCOInWorkNotificaton = true;
            }

            MapList mlInfoList = transformCOMapToHTMLList(context, issueMap, strBaseURL);

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            String strStyleSheet = commonObj.getFormStyleSheet(context, "PSS_NotificationStyleSheet");
            messageHTML = commonObj.getHTMLTwoColumnTable(context, mlInfoList, strStyleSheet);
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCONotificationBodyHTML: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        } finally {
            if (bIsCOInWorkNotificaton)
                PropertyUtil.setGlobalRPEValue(context, "PSS_NOTIFICATION_NAME", "");
        }
        return messageHTML;
    }

    /**
     * getChangeInWorkNotificationList As apart of TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public StringList getChangeInWorkNotificationList(Context context, String[] args) throws Exception {
        StringList slInWorkList = new StringList();
        try {
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonNotObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);

            String selectCOTechAssignee = new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_CHANGEACTION).append("].to.from[").append(TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE)
                    .append("].to.name").toString();
            String selectMCOTechAssignee = new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION).append("].to.from[")
                    .append(TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE).append("].to.name").toString();

            StringList objectSelects = new StringList();
            objectSelects.addElement(selectCOTechAssignee);
            objectSelects.addElement(selectMCOTechAssignee);

            Map programMap = (Map) JPO.unpackArgs(args);
            String strCOObjId = (String) programMap.get("id");

            if (UIUtil.isNotNullAndNotEmpty(strCOObjId)) {
                DomainObject domCOObj = DomainObject.newInstance(context, strCOObjId);
                // PCM : TIGTK-9180 : 01/08/2017 : AB : START
                DomainObject.MULTI_VALUE_LIST.add(selectCOTechAssignee);
                DomainObject.MULTI_VALUE_LIST.add(selectMCOTechAssignee);
                objectSelects.addElement(DomainConstants.SELECT_OWNER);

                Map mapCOObj = domCOObj.getInfo(context, objectSelects);

                if (!mapCOObj.isEmpty()) {
                    // TIGTK-10701 -- START
                    String strCOOwner = (String) mapCOObj.get(DomainConstants.SELECT_OWNER);
                    // TIGTK-10701 -- END
                    if (domCOObj.isKindOf(context, TigerConstants.TYPE_CHANGEORDER)) {
                        StringList slAssigneeList = commonNotObj.getStringListFromMap(context, mapCOObj, selectCOTechAssignee);

                        HashSet setInWorkList = new HashSet<>(slAssigneeList);
                        slInWorkList.addAll(setInWorkList);

                        // Remove CO Owner if that person is also assignee of any ChangeAction
                        while (slInWorkList.contains(strCOOwner)) {
                            slInWorkList.remove(strCOOwner);
                        }
                        // PCM : TIGTK-9180 : 01/08/2017 : AB : END
                    } else if (domCOObj.isKindOf(context, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                        // TIGTK-10701 -- START
                        slInWorkList = commonNotObj.getStringListFromMap(context, mapCOObj, selectMCOTechAssignee);
                        // Remove MCO Owner if that person is also assignee of any MANUFACTURING ChangeAction
                        while (slInWorkList.contains(strCOOwner)) {
                            slInWorkList.remove(strCOOwner);
                        }
                        // TIGTK-10701 -- END
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeInWorkNotificationList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return slInWorkList;
    }

    /**
     * getCOCompleteNotificationList As apart of TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public StringList getCOCompleteNotificationList(Context context, String[] args) throws Exception {
        StringList slCOCompleteList = new StringList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCOObjId = (String) programMap.get("id");
            StringList slRolesList = new StringList();
            // TIGTK-12916 :START
            String rpeUserName = PropertyUtil.getGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME);

            // TIGTK-5890 : PTE : 4/7/2017 : START
            slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANUFACTURING_LEADER);
            slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
            // TIGTK-5890 : PTE : 4/7/2017 : END
            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
            slCOCompleteList = commonObj.getProgramProjectTeamMembersForChange(context, strCOObjId, slRolesList, true);

            if (slCOCompleteList.contains(rpeUserName)) {
                slCOCompleteList.remove(rpeUserName);
            }

            // TIGTK-12916 :END
        } catch (Exception ex) {
            logger.error("Error in getCOCompleteNotificationList: ", ex);
        }
        return slCOCompleteList;

    }

    /**
     * getCOImplementNotificationLists As apart of TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public StringList getToListCOImplementNotificationList(Context context, String[] args) throws Exception {
        StringList slToCOImplementedList = new StringList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCOObjId = (String) programMap.get("id");
            StringList slRolesList = new StringList();
            // TIGTK-5890 : PTE : 4/7/2017 : START
            slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
            // TIGTK-5890 : PTE : 4/7/2017 : END
            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
            slToCOImplementedList = commonObj.getProgramProjectTeamMembersForChange(context, strCOObjId, slRolesList, true);

        } catch (Exception ex) {
            logger.error("Error in getToListCOImplementNotificationList ", ex);
        }
        return slToCOImplementedList;

    }

    /**
     * getCOImplementNotificationLists As apart of TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    // TIGTK-6186 | Harika Varanast | 05/04/2017 : Starts
    public String getFromListCOImplementNotificationList(Context context, String[] args) throws Exception {
        String strFromCOImplementedList = "";
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            Map payload = (Map) programMap.get("payload");
            strFromCOImplementedList = (String) payload.get("fromList");

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getFromListCOImplementNotificationList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return strFromCOImplementedList;

    }

    // TIGTK-6186 | Harika Varanast | 05/04/2017 : Ends
    // TGPSS_PCM_TS_154_ChangeOrderNotifications_V3.1 | 23/02/2017 |Harika Varanasi : Ends

    // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 06/03/2017 |Harika Varanasi : Starts

    /**
     * getCRNotificationBodyHTML method is used to CR messageHTML in Notification Object As apart of TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getCRNotificationBodyHTML(Context context, String[] args) throws Exception {
        String messageHTML = "";
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCRObjId = (String) programMap.get("id");
            String strBaseURL = (String) programMap.get("baseURL");
            String notificationObjName = (String) programMap.get("notificationName");
            String attrSubText = PropertyUtil.getSchemaProperty(context, "attribute_SubjectText");
            StringList busSelects = new StringList("attribute[" + attrSubText + "]");
            boolean isImpact = false;
            String strSectionSub = "";
            Map mapCR = (Map) getChangeRequestInformation(context, strCRObjId);
            String strSubjectKey = "";
            MapList mpNotificationList = DomainObject.findObjects(context, "Notification", notificationObjName, "*", "*", TigerConstants.VAULT_ESERVICEADMINISTRATION, "", null, true, busSelects,
                    (short) 0);

            if (mpNotificationList != null && (!mpNotificationList.isEmpty())) {
                strSubjectKey = (String) ((Map) mpNotificationList.get(0)).get("attribute[" + attrSubText + "]");
            }

            if (UIUtil.isNotNullAndNotEmpty(strSubjectKey)) {
                strSectionSub = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strSubjectKey);
            }
            mapCR.put("SectionSubject", strSectionSub);
            // TIGTK-9020:Rutuja Ekatpure:9/7/2017:Start
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName)
                    && ("PSS_CREvaluatePromoteImpactAnalysisNotification".equalsIgnoreCase(notificationObjName) || "PSSPerformImpactAnalysisNotification".equalsIgnoreCase(notificationObjName))) {
                isImpact = true;
            }
            // TIGTK-9020:Rutuja Ekatpure:9/7/2017:End
            // PCM : TIGTK-8085 : PSE : 06-06-20147 : START
            Map payLoadMap = (Map) programMap.get("payload");

            // PCM : TIGTK-8559 : 13/06/2017 : AB : START
            if (payLoadMap != null) {
                // PCM : TIGTK-10909 :Sayali D : 13-Nov-2017 :START
                if (payLoadMap.containsKey("ChangeStateTo")) {
                    String strChangeStateTo = (String) payLoadMap.get("ChangeStateTo");
                    mapCR.remove("current");
                    mapCR.put("current", strChangeStateTo);
                }
                // PCM : TIGTK-10909 :Sayali D : 13-Nov-2017 :END
                if (payLoadMap.containsKey("Comments")) {
                    String strComments = (String) payLoadMap.get("Comments");
                    mapCR.put("Comments", strComments);
                }
                // TIGTK-11455 :Start
                if (payLoadMap.containsKey("RejectedTask_Comment")) {
                    String strComments = (String) payLoadMap.get("RejectedTask_Comment");
                    mapCR.put("RejectedTask_Comment", strComments);
                }
                // TIGTK-11455 :End
                if (payLoadMap.containsKey("ReasonForRework")) {
                    String strReasonForRework = (String) payLoadMap.get("ReasonForRework");
                    mapCR.put("ReasonForRework", strReasonForRework);
                }
                if (payLoadMap.containsKey("ReasonForCancellationRequest")) {
                    String strReasonForCancellationRequest = (String) payLoadMap.get("ReasonForCancellationRequest");
                    mapCR.put("ReasonForCancellationRequest", strReasonForCancellationRequest);
                } // TIGTK-7580: START
                if (payLoadMap.containsKey("TransferComments")) {
                    String strTransferComments = (String) payLoadMap.get("TransferComments");
                    mapCR.put("TransferComments", strTransferComments);
                } else {
                    mapCR.put("TransferComments", "");
                }
                // TIGTK-7580 : END
            }
            // PCM : TIGTK-8559 : 13/06/2017 : AB : END
            boolean isSubmit = false;
            boolean isCreate = false;
            // PCM : TIGTK-8914 : 07/07/2017 : AM . Added variable isReject
            boolean isReject = false;
            boolean isCancelled = false;
            // TIGTK-11455 START
            boolean isTaskReject = false;
            boolean isReassign = false;
            // TIGTK-11455 END
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_CRSubmitPromoteNotification".equalsIgnoreCase(notificationObjName)) {
                isSubmit = true;
            } else if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_CRCreateDemoteNotification".equalsIgnoreCase(notificationObjName)) {
                isCreate = true;
            }
            // PCM : TIGTK-8914 : 07/07/2017 : AM : START
            else if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && ("PSS_CRRejectNotification".equalsIgnoreCase(notificationObjName)
                    || "PSS_RejectMasterCRNotification".equalsIgnoreCase(notificationObjName) || "PSS_RejectSlaveCRNotification".equalsIgnoreCase(notificationObjName))) {
                isReject = true;
            }
            // PCM : TIGTK-8914 : 07/07/2017 : AM : END
            else if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_COCancellationRequestNotification".equalsIgnoreCase(notificationObjName)) {
                isCancelled = true;
            } // TIGTK_7580 : START
            else if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_TransferOwnershipNotification".equalsIgnoreCase(notificationObjName)) {
                strSectionSub = getTranferOwnershipSubject(context, args);
                mapCR.put("SectionSubject", strSectionSub);
            }
            // TIGTK-7580 : END
            // TIGTK-11455 :START
            else if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_CRTaskRejectNotification".equalsIgnoreCase(notificationObjName)) {
                isTaskReject = true;
            }
            // TIGTK-11455 :END
            else if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_CRTaskReassignNotification".equalsIgnoreCase(notificationObjName)) {
                strSectionSub = getTaskReassignmentSubject(context, args);
                mapCR.put("SectionSubject", strSectionSub);
                // TIGTK-11455 START
                isReassign = true;
                // TIGTK-11455 END
            }
            // TIGTK-10709 -- END
            // TIGTK-11455 START
            MapList mlInfoList = transformCRMapToHTMLList(context, mapCR, strBaseURL, isImpact, isSubmit, isCreate, isReject, isCancelled, isTaskReject, isReassign);
            // TIGTK-11455 END
            // PCM : TIGTK-8212 : 09/06/2017 : AB : END
            // PCM : TIGTK-8085 : PSE : 06-06-20147 : END
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            String strStyleSheet = commonObj.getFormStyleSheet(context, "PSS_NotificationStyleSheet");
            messageHTML = commonObj.getHTMLTwoColumnTable(context, mlInfoList, strStyleSheet);

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCRNotificationBodyHTML: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return messageHTML;
    }

    /**
     * getChangeRequestInformation method is used to get all information about PSS_ChangeRequest As a Part of TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2
     * @param context
     * @param strCRId
     * @return Map
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map getChangeRequestInformation(Context context, String strCRId) throws Exception {
        Map mapCR = new HashMap();
        try {

            if (UIUtil.isNotNullAndNotEmpty(strCRId)) {
                DomainObject domCRObj = DomainObject.newInstance(context, strCRId);

                StringList slObjectSelects = new StringList(24);
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                slObjectSelects.addElement(DomainConstants.SELECT_NAME);
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
                slObjectSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTITLE + "]");
                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
                slObjectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                // PCM : TIGTK-8914 : 07/07/2017 : AM : START
                slObjectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_COMMENTS + "]");
                // PCM : TIGTK-8914 : 07/07/2017 : AM : END

                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE + "]");
                // PCM : TIGTK-7604 : 15/05/2017 : AB : START
                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");
                slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACKCOMMENT + "]");
                // PCM : TIGTK-7604 : 15/05/2017 : AB : END
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.name");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.name");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.id");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.name");
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.id");

                // PCM : TIGTK-7745 : 01/09/2017 : AB : START
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.name");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.name");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.id");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.name");
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");
                // PCM : TIGTK-7745 : 01/09/2017 : AB : END

                mapCR = domCRObj.getInfo(context, slObjectSelects);
                String personEmail = "";
                if (mapCR != null && mapCR.size() > 0 && mapCR.containsKey("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]")) {
                    String strChangeInitiator = (String) mapCR.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                    if (UIUtil.isNotNullAndNotEmpty(strChangeInitiator)) {
                        personEmail = new StringBuilder("<a href=\"mailto:").append(PersonUtil.getEmail(context, strChangeInitiator)).append("\" target=\"_top\">")
                                .append(PersonUtil.getEmail(context, strChangeInitiator)).append("</a>").toString();
                    }
                } else {
                    mapCR = new HashMap();
                }

                mapCR.put("changeInitiatorEmail", personEmail);
            }

        } catch (RuntimeException rex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeRequestInformation: ", rex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw rex;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeRequestInformation: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        } finally {
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.name");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.id");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.name");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.id");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.name");
            DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");
        }

        return mapCR;
    }

    /**
     * transformCRMapToHTMLList As a Part of TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2
     * @param context
     * @param objectMap
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph //PCM : TIGTK-8914 : 07/07/2017 : AM : Changed Method declaration to add on another variable - isReject
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList transformCRMapToHTMLList(Context context, Map objectMap, String strBaseURL, boolean isImpact, boolean isSubmit, boolean isCreate, boolean isReject, boolean isCancelled,
            boolean isTaskReject, boolean isReassign) throws Exception {
        MapList mlInfoList = new MapList();
        try {

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);

            StringList slHyperLinkLabelKey = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.CRNotification.HyperLinkLabelKey"), ",");
            StringList slHyperLinkLabelKeyIds = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.CRNotification.HyperLinkLabelKeyIds"), ",");
            // Fix for FindBugs Static field : Harika Varanasi : 21 March 2017 : Start
            if (isImpact) {
                // PCM : TIGTK-8085 : PSE : 06-06-2017 : START
                // PCM : TIGTK-8212 : 09/06/2017 : AB : START
                initializeCRLinkedHashMap(isImpact, isSubmit, isCreate, isReject, isCancelled, isTaskReject, isReassign, lhmCRImpactSelectionStore);
                // PCM : TIGTK-8085 : PSE : 06-06-2017 : END
                // PCM : TIGTK-8212 : 09/06/2017 : AB : END
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCRImpactSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
                // PCM : TIGTK-8085 : PSE : 06-06-2017 : START

            } else if (isSubmit || isReassign) {// TIGTK-11455 START isReassign : add Comments
                initializeCRLinkedHashMap(isImpact, isSubmit, isCreate, isReject, isCancelled, isTaskReject, isReassign, lhmCRCommentsSelectionStore);
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCRCommentsSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            }
            // PCM : TIGTK-8914 : 07/07/2017 : AM : START
            else if (isReject) {
                initializeCRLinkedHashMap(isImpact, isSubmit, isCreate, isReject, isCancelled, isTaskReject, isReassign, lhmCRRejectSelectionStore);// add name from top
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCRRejectSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            }
            // PCM : TIGTK-8914 : 07/07/2017 : AM : END
            // TIGTK-11455:START
            else if (isTaskReject) {
                initializeCRLinkedHashMap(isImpact, isSubmit, isCreate, isReject, isCancelled, isTaskReject, isReassign, lhmCRRejectSelectionStore);// add name from top
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCRRejectSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            }
            // TIGTK-11455:END
            else if (isCancelled) {
                initializeCRLinkedHashMap(isImpact, isSubmit, isCreate, isReject, isCancelled, isTaskReject, isReassign, lhmCRRejectSelectionStore);// add name from top
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCRRejectSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            } else {
                initializeCRLinkedHashMap(isImpact, isSubmit, isCreate, isReject, isCancelled, isTaskReject, isReassign, lhmCRGenericSelectionStore);
                // PCM : TIGTK-8085 : PSE : 06-06-2017 : END
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCRGenericSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            }
            // Fix for FindBugs Static field : Harika Varanasi : 21 March 2017 : End

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in transformCRMapToHTMLList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mlInfoList;
    }

    /**
     * initializeCRLinkedHashMap As a Part of TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2
     * @throws Exception
     *             if the operation fails
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    // Fix for FindBugs Static field : Harika Varanasi : 21 March 2017 : Start
    public void initializeCRLinkedHashMap(boolean isImapct, boolean isSubmit, boolean isCreate, boolean isReject, boolean isCancelled, boolean isTaskReject, boolean isReassign,
            LinkedHashMap<String, String> linkedHashMap) throws Exception {
        try {

            if (linkedHashMap != null && (linkedHashMap.isEmpty())) {

                linkedHashMap.put("Title", "SectionHeader");
                linkedHashMap.put("Subject", "SectionSubject");
                linkedHashMap.put("Main_Information", "SectionHeader");
                linkedHashMap.put("Project_Code", "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                linkedHashMap.put("Project_Description", "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                linkedHashMap.put("CR", DomainConstants.SELECT_NAME);
                linkedHashMap.put("CR_Description", DomainConstants.SELECT_DESCRIPTION);
                linkedHashMap.put("State", DomainConstants.SELECT_CURRENT);
                linkedHashMap.put("CR_Title", "attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTITLE + "]");
                linkedHashMap.put("CR_Type", "attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
                linkedHashMap.put("Change_Initiator", DomainConstants.SELECT_ORIGINATOR);
                linkedHashMap.put("Change_Initiator_Email", "changeInitiatorEmail");
                // PCM : TIGTK-7745 : 21/08/2017 : AB : START
                linkedHashMap.put("Parallel_Track", "attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");
                linkedHashMap.put("Parallel_Track_Comment", "attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACKCOMMENT + "]");
                // PCM : TIGTK-7745 : 21/08/2017 : AB : END

                if (isImapct) {
                    linkedHashMap.put("Requested_Assessment_End_Date", "attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE + "]");
                }
                // PCM : TIGTK-8085 : PSE : 06-06-2017 : START
                // TIGTK-11455
                if (isSubmit || isReassign) {
                    linkedHashMap.put("Comments", "Comments");
                }

                // PCM : TIGTK-8914 : 07/07/2017 : AM : START
                // TIGTK-1145 :Start
                if (isTaskReject) {
                    linkedHashMap.put("RejectedTask_Comment", "RejectedTask_Comment");
                }
                // TIGTK-1145 :End
                // PCM : TIGTK-8914 : 07/07/2017 : AM : START

                if (isReject) {
                    linkedHashMap.put("Rejection_Comments", "attribute[" + DomainConstants.ATTRIBUTE_COMMENTS + "]");
                }
                // PCM : TIGTK-8914 : 07/07/2017 : AM : START

                // PCM : TIGTK-8212 : 09/06/2017 : AB : START
                if (isCreate) {
                    linkedHashMap.put("Reason_For_Rework", "ReasonForRework");
                }
                // PCM : TIGTK-8212 : 09/06/2017 : AB : END
                // PCM : TIGTK-8085 : PSE : 06-06-2017 : END
                if (isCancelled) {
                    linkedHashMap.put("Reason_For_Cancellation_Request", "ReasonForCancellationRequest");
                }
                // TIGTK-7580 : START
                linkedHashMap.put("Comment", "TransferComments");
                // TIGTK-7580 : END
                linkedHashMap.put("Useful_Links", "SectionHeader");
                linkedHashMap.put("Related_Content", "from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.name");
                linkedHashMap.put("Related_COs", "from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.name");
                linkedHashMap.put("Related_MCOs", "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.name");
            }

        } catch (RuntimeException re) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeCRLinkedHashMap: ", re);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeCRLinkedHashMap: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        // return linkedHashMap;
    }

    // Fix for FindBugs Static field : Harika Varanasi : 21 March 2017 : End

    /**
     * getCRChangeManagerList As a Part of TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public StringList getCRPromoteToEvaluateImpactAnalysisList(Context context, String[] args) throws Exception {
        StringList slCREvaluateRouteAssignees = new StringList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCRObjId = (String) programMap.get("id");
            if (UIUtil.isNotNullAndNotEmpty(strCRObjId)) {
                slCREvaluateRouteAssignees = getChangeRouteAssignees(context, strCRObjId, "state_Evaluate");
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCRPromoteToEvaluateImpactAnalysisList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return slCREvaluateRouteAssignees;
    }

    /**
     * PCM : TIGTK-7496 : 22/06/2017 : AB getChangeRouteAssignees
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public StringList getChangeRouteAssignees(Context context, String strChangeObjectId, String strSymbolicState) throws Exception {
        StringList slRoteAssigneeList = new StringList();
        try {
            String strSelectProgramProjectId = DomainConstants.EMPTY_STRING;
            if (UIUtil.isNotNullAndNotEmpty(strChangeObjectId)) {
                // FindBug TIGTK-11306
                // String strWhere = DomainConstants.EMPTY_STRING;
                //
                // if (UIUtil.isNotNullAndNotEmpty(strSymbolicState)) {
                // strWhere = new StringBuilder("").append("|(").append(Route.SELECT_ROUTE_BASESTATE).append(" matchlist '").append(strSymbolicState).append("' ',')").toString();
                // }

                String strAttrRouteTaskUser = "attribute[" + Route.ATTRIBUTE_ROUTE_TASK_USER + "]";
                // TIGTK-5450 : START

                // PCM : TIGTK-5450 & TIGTK-9408 : 04/08/2017 : AB : START
                DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjectId);
                String strTypeOfChange = domChangeObject.getInfo(context, DomainConstants.SELECT_TYPE);

                if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strTypeOfChange)) {
                    strSelectProgramProjectId = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.id").toString();
                } else if (TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(strTypeOfChange)) {
                    strSelectProgramProjectId = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_RELATEDCN).append("].from.to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA)
                            .append("].from.id").toString();
                } else {
                    strSelectProgramProjectId = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION).append("].from.to[")
                            .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.id").toString();
                }
                // PCM : TIGTK-5450 & TIGTK-9408 : 04/08/2017 : AB : END

                // TIGTK-5450 : END
                String selectRoutePersonName = new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_OBJECT_ROUTE).append("].to.from[").append(DomainConstants.RELATIONSHIP_ROUTE_NODE)
                        .append("|to.type==\"Person\"].to.name").toString();
                String selectRouteGroupOrRoleName = new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_OBJECT_ROUTE).append("].to.from[").append(DomainConstants.RELATIONSHIP_ROUTE_NODE)
                        .append("|to.type!=\"Person\"].").append(strAttrRouteTaskUser).toString();

                String selectMCOPlantMembers = DomainConstants.EMPTY_STRING;
                String selectPlantMembersConnectedToProject = DomainConstants.EMPTY_STRING;

                // TIGTK-12339 :START
                if (TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(strTypeOfChange)) {
                    // TIGTK-10708:START
                    String strPlantName = domChangeObject.getInfo(context,
                            "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");

                    selectMCOPlantMembers = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_RELATEDCN).append("].from.from[")
                            .append(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT).append("].to.from[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append("].to.name")
                            .toString();

                    selectPlantMembersConnectedToProject = "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from["
                            + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "]=='" + strPlantName + "'].to.name";

                } else {
                    String strPlantName = domChangeObject.getInfo(context,
                            "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.name");

                    selectMCOPlantMembers = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION).append("].from.from[")
                            .append(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT).append("].to.from[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append("].to.name")
                            .toString();

                    selectPlantMembersConnectedToProject = "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA
                            + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANTNAME + "]=='" + strPlantName
                            + "'].to.name";
                }
                // TIGTK-10708:END
                // TIGTK-12339 :END
                String strMapRoutePersonName = new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_OBJECT_ROUTE).append("].to.from[").append(DomainConstants.RELATIONSHIP_ROUTE_NODE)
                        .append("].to.name").toString();
                String strMapRouteGroupOrRoleName = new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_OBJECT_ROUTE).append("].to.from[").append(DomainConstants.RELATIONSHIP_ROUTE_NODE)
                        .append("].").append(strAttrRouteTaskUser).toString();

                StringList objSelects = new StringList();
                // TIGTK-5450 : START
                objSelects.addElement(DomainConstants.SELECT_TYPE);
                objSelects.addElement(strSelectProgramProjectId);
                // TIGTK-5450 : END
                objSelects.addElement(selectRoutePersonName);
                objSelects.addElement(selectRouteGroupOrRoleName);
                objSelects.addElement(DomainConstants.SELECT_NAME);
                objSelects.addElement(selectMCOPlantMembers);
                objSelects.addElement(selectPlantMembersConnectedToProject);

                BusinessObjectWithSelectList busWithSelectionList = BusinessObject.getSelectBusinessObjectData(context, new String[] { strChangeObjectId }, objSelects);
                BusinessObjectWithSelect busObjectWithSelect = (BusinessObjectWithSelect) busWithSelectionList.elementAt(0);

                StringList slPersonsList = busObjectWithSelect.getSelectDataList(strMapRoutePersonName);
                if (slPersonsList != null && !slPersonsList.isEmpty()) {
                    slRoteAssigneeList.addAll(slPersonsList);
                }

                StringList slSymbolicRolesOrGroupsList = busObjectWithSelect.getSelectDataList(strMapRouteGroupOrRoleName);
                // TIGTK-5450 : START
                // TIGTK-12719 : Start
                // Note : Used MQL as type filtration is not working with "BusinessObject.getSelectBusinessObjectData"
                String strProgramProjectId = DomainConstants.EMPTY_STRING;
                if (TigerConstants.TYPE_CHANGEACTION.equalsIgnoreCase(strTypeOfChange)) {
                    String strSelect = "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from[|type != " + TigerConstants.TYPE_PSS_CHANGEREQUEST + "].to["
                            + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id";
                    strProgramProjectId = MqlUtil.mqlCommand(context, "print bus " + strChangeObjectId + " select " + strSelect + " dump |", true, true);
                } else {
                    strProgramProjectId = busObjectWithSelect.getSelectData(strSelectProgramProjectId);
                }
                // TIGTK-12719 : End

                String strObjectType = busObjectWithSelect.getSelectData(DomainConstants.SELECT_TYPE);
                // TIGTK-5450 : END
                if (slSymbolicRolesOrGroupsList != null && (!slSymbolicRolesOrGroupsList.isEmpty())) {
                    int slsize = slSymbolicRolesOrGroupsList.size();
                    String strSymbolicName = DomainConstants.EMPTY_STRING;
                    for (int i = 0; i < slsize; i++) {
                        strSymbolicName = (String) slSymbolicRolesOrGroupsList.get(i);
                        if (UIUtil.isNotNullAndNotEmpty(strSymbolicName)) {
                            // TIGTK-5450 : START

                            // Get role persons from Program-Project and send notification
                            String strActualRoleName = PropertyUtil.getSchemaProperty(context, strSymbolicName);
                            DomainObject domProgramProjectObj = DomainObject.newInstance(context, strProgramProjectId);
                            StringList objectSelect = new StringList(DomainConstants.SELECT_NAME);
                            StringBuffer relWhere = new StringBuffer();
                            relWhere.append("attribute[");
                            relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
                            relWhere.append("]");
                            relWhere.append(" == '");
                            // TIGTK-5890 : PTE : 4/7/2017 : START
                            relWhere.append(strActualRoleName);
                            // TIGTK-5890 : PTE : 4/7/2017 : END
                            relWhere.append("'");
                            MapList mlPerson = getMembersFromProgram(context, domProgramProjectObj, objectSelect, DomainConstants.EMPTY_STRINGLIST, DomainConstants.EMPTY_STRING, relWhere.toString());

                            // PCM : TIGTK-7745 : 23/08/2017 : AB : START
                            // TIGTK-12339 :START
                            if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strObjectType) || ChangeConstants.TYPE_CHANGE_ACTION.equalsIgnoreCase(strObjectType)) {
                                // PCM : TIGTK-7745 : 23/08/2017 : AB : END
                                if (mlPerson.size() > 0) {
                                    for (int j = 0; j < mlPerson.size(); j++) {
                                        String strPerson = (String) ((Map) mlPerson.get(j)).get(DomainConstants.SELECT_NAME);
                                        slRoteAssigneeList.addElement(strPerson);
                                    }
                                }
                            } else if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strObjectType) || TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(strObjectType)) { // TIGTK-12339
                                                                                                                                                                                                    // :END
                                if (mlPerson.size() > 0) {
                                    // TIGTK-10708:START
                                    // Get the Connected plant members of MCO
                                    StringList slMCOPlantMembers = busObjectWithSelect.getSelectDataList(selectMCOPlantMembers);
                                    // Get Plant Members connected To Program-Project of MCO
                                    String strMember = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump", strChangeObjectId, selectPlantMembersConnectedToProject);
                                    StringList slPlantMembersConnectedToProject = new StringList();
                                    String[] sMembers = strMember.split(",");
                                    for (int j = 0; j < sMembers.length; j++) {
                                        String strPlantMember = sMembers[j];
                                        slPlantMembersConnectedToProject.add(strPlantMember);
                                    }
                                    // TIGTK-10708:END
                                    // If Program-Project member of RouteTaskRole is Member of connected Plant and also member from PlantMembers connected To Program-Project of MCO
                                    for (int k = 0; k < mlPerson.size(); k++) {
                                        String strPersonName = (String) ((Map) mlPerson.get(k)).get(DomainConstants.SELECT_NAME);
                                        if (slMCOPlantMembers.contains(strPersonName) && slPlantMembersConnectedToProject.contains(strPersonName)) {
                                            slRoteAssigneeList.add(strPersonName);
                                        }
                                    }
                                }
                            } else {
                                slRoteAssigneeList.addElement(PropertyUtil.getSchemaProperty(context, strSymbolicName));
                            }
                            // TIGTK-5450 : END
                        }
                    }
                }
            }

        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: START
        catch (RuntimeException re) {
            logger.error("Runtime Exception in method slRoteAssigneeList: ", re);
            throw re;
        }
        // Fix for FindBugs issue RuntimeException capture: Harika Varanasi : 21 March 2017: START
        catch (Exception e) {
            logger.error("Error in slRoteAssigneeList: ", e);
            throw e;
        }

        return slRoteAssigneeList;
    }

    /**
     * getProgramProjectTeamMembersForPart
     * @param context
     * @param strPartID
     * @param slRolesList
     * @param onlyLead
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public StringList getProgramProjectTeamMembersForPart(Context context, String strPartID, StringList slRolesList, boolean onlyLead) throws Exception {
        StringList slProjectTeamMembers = new StringList();
        try {

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonNotObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            if (UIUtil.isNotNullAndNotEmpty(strPartID)) {
                DomainObject domPartObject = DomainObject.newInstance(context, strPartID);
                String strWhere = "";
                String strRolesWhere = "";
                String strPositionWhere = "";
                // TIGTK-15607 : Prakash B : Start
                String strMapSelectGProjectTeamMembers = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT).append("].from.from[")
                        .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append(strWhere).append("].to.name").toString();
                String strMapSelectIProjectTeamMembers = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT).append("].from.from[")
                        .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append(strWhere).append("].to.name").toString();
                // TIGTK-15607 : Prakash B : end
                if (slRolesList != null && (!slRolesList.isEmpty())) {
                    strRolesWhere = new StringBuilder("(attribute[").append(TigerConstants.ATTRIBUTE_PSS_ROLE).append("] matchlist \"").append(FrameworkUtil.join(slRolesList, ",")).append("\" \",\"")
                            .append(")").toString();
                }

                if (onlyLead) {
                    strPositionWhere = new StringBuilder("(attribute[").append(TigerConstants.ATTRIBUTE_PSS_POSITION).append("]==\"Lead\")").toString();
                }

                if (UIUtil.isNotNullAndNotEmpty(strRolesWhere) && UIUtil.isNotNullAndNotEmpty(strPositionWhere)) {
                    strWhere = new StringBuilder("|(").append(strRolesWhere).append("&&").append(strPositionWhere).append(")").toString();
                } else if (UIUtil.isNotNullAndNotEmpty(strRolesWhere)) {
                    strWhere = "|" + strRolesWhere;
                } else if (UIUtil.isNotNullAndNotEmpty(strPositionWhere)) {
                    strWhere = "|" + strPositionWhere;
                } else {
                    strWhere = "";
                }

                // TIGTK-15607 : Prakash B : Start
                String strSelectProjectTeamMembers = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT).append("].from.from[")
                        .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append(strWhere).append("].to.name").toString();

                StringList slSelects = new StringList(strSelectProjectTeamMembers);
                slSelects.add(new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT).append("].from.from[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS)
                        .append(strWhere).append("].to.name").toString());
                // PCM : TIGTK-7622 : 18/05/2017 : AB : START
                // DomainObject.MULTI_VALUE_LIST.add(strMapSelectGProjectTeamMembers);
                // DomainObject.MULTI_VALUE_LIST.add(strMapSelectIProjectTeamMembers);
                // PCM : TIGTK-7390 : 26/06/2017 : AB : START
                // TIGTK-15607 : Prakash B : End
                Map objectMap = null;
                try {
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    objectMap = domPartObject.getInfo(context, slSelects);
                } finally {
                    ContextUtil.popContext(context);
                }
                // PCM : TIGTK-7390 : 26/06/2017 : AB : END
                // TIGTK-15607 : Prakash B : Start
                StringList slProjectMembers = commonNotObj.getStringListFromMap(context, objectMap, strMapSelectGProjectTeamMembers);
                slProjectMembers.addAll(commonNotObj.getStringListFromMap(context, objectMap, strMapSelectIProjectTeamMembers));
                // TIGTK-15607 : Prakash B : End
                if (!slProjectMembers.isEmpty()) {
                    HashSet setProjectteamMembers = new HashSet(slProjectMembers);
                    ArrayList<String> list = new ArrayList<String>(setProjectteamMembers);
                    slProjectTeamMembers = new StringList(list);
                }
                // PCM : TIGTK-7622 : 18/05/2017 : AB : END
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getProgramProjectTeamMembersForPart: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return slProjectTeamMembers;
    }

    /**
     * getCRPromoteToInEvaluateLeadCMList As a Part of TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public StringList getCRPromoteToEvaluateLeadCMList(Context context, String[] args) throws Exception {
        StringList slCREvaluateLeadCMList = new StringList();
        // TIGTK-10699 -- START
        String strContextUser = context.getUser();
        // TIGTK-10699 -- END
        try {
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonNotObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCRObjId = (String) programMap.get("id");
            if (UIUtil.isNotNullAndNotEmpty(strCRObjId)) {
                // For Lead Change Managers Program Project Team Members
                // TIGTK-5890 : PTE : 4/7/2017 : START

                // PCM : TIGTK-7390 : 14/06/2017 : AB : START
                DomainObject domCRObj = DomainObject.newInstance(context, strCRObjId);
                StringList slObjSelects = new StringList();
                slObjSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");
                DomainObject.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");

                Map objCRMap = domCRObj.getInfo(context, slObjSelects);
                StringList slAffectedItemsOfCR = commonNotObj.getStringListFromMap(context, objCRMap, "from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");

                if (!slAffectedItemsOfCR.isEmpty()) {
                    for (int i = 0; i < slAffectedItemsOfCR.size(); i++) {
                        String strItemObjectID = (String) slAffectedItemsOfCR.get(i);
                        MapList rootList = this.getAllRootFromECPart(context, strItemObjectID);

                        for (int intIndexRoot = 0; intIndexRoot < rootList.size(); intIndexRoot++) {
                            Map rootMap = (Map) rootList.get(intIndexRoot);
                            String objectId = (String) rootMap.get(DomainObject.SELECT_ID);
                            StringList slTempLeadChgMngrs = getProgramProjectTeamMembersForPart(context, objectId, new StringList(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR), true);
                            if (!slTempLeadChgMngrs.isEmpty()) {
                                // TIGTK-15607 : Prakash B : Start
                                // slCREvaluateLeadCMList.addAll(slTempLeadChgMngrs);
                                Iterator<String> itr = slTempLeadChgMngrs.iterator();
                                while (itr.hasNext()) {
                                    String string = (String) itr.next();
                                    if (!slCREvaluateLeadCMList.contains(string))
                                        slCREvaluateLeadCMList.add(string);
                                }
                                // TIGTK-15607 : Prakash B : End
                            }
                        }
                    }
                }
                // PCM : TIGTK-7390 : 14/06/2017 : AB : END
                // TIGTK-5890 : PTE : 4/7/2017 : END
            }
            // TIGTK-10699 -- START
            while (slCREvaluateLeadCMList.contains(strContextUser)) {
                slCREvaluateLeadCMList.remove(strContextUser);
            }
            // TIGTK-10699 -- END
        } catch (Exception ex) {
            logger.error("Error in getCRPromoteToEvaluateLeadCMList: ", ex);
        }

        return slCREvaluateLeadCMList;
    }

    /**
     * getCRPromoteToInReviewList As a Part of TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public StringList getCRPromoteToInReviewList(Context context, String[] args) throws Exception {
        StringList slCRReviewRouteAssignees = new StringList();

        // PCM : TIGTK-10799 : 13/11/2017 : Aniket Madhav : START
        String strContextUser = context.getUser();
        // PCM : TIGTK-10799 : 13/11/2017 : Aniket Madhav : END

        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCRObjId = (String) programMap.get("id");
            if (UIUtil.isNotNullAndNotEmpty(strCRObjId)) {
                slCRReviewRouteAssignees = getChangeRouteAssignees(context, strCRObjId, "state_InReview");
            }

            // PCM : TIGTK-10799 : 23/08/2017 : Aniket Madhav : START
            if (slCRReviewRouteAssignees.contains(strContextUser)) {
                while (slCRReviewRouteAssignees.contains(strContextUser)) {
                    slCRReviewRouteAssignees.remove(strContextUser);
                }
            }
            // PCM : TIGTK-10799 : 23/08/2017 : Aniket Madhav : END

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCRPromoteToInReviewList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return slCRReviewRouteAssignees;
    }

    /**
     * getToListForCRRejectAndInProcessNotifications As a Part of TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public StringList getToListForCRRejectAndInProcessNotifications(Context context, String[] args) throws Exception {
        StringList slCRRejectionToList = new StringList();
        StringList slReturnList = new StringList();
        // TIGTK-10757 -- START
        String strContextUser = DomainConstants.EMPTY_STRING;
        // TIGTK-10757 -- END
        try {
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonNotObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            Map programMap = (Map) JPO.unpackArgs(args);
            // TIGTK-10699 -- START
            String strNotificationObjName = (String) programMap.get("notificationName");
            if (UIUtil.isNotNullAndNotEmpty(strNotificationObjName) && "PSS_CRInProcessPromoteNotification".equals(strNotificationObjName)) {
                pss.ecm.ui.MfgChangeOrder_mxJPO mfgChangeObj = new pss.ecm.ui.MfgChangeOrder_mxJPO();
                strContextUser = mfgChangeObj.getGenericContextOrLoginUser(context, args);

            } else {
                strContextUser = context.getUser();
            }
            // TIGTK-10699 -- END
            String strCRObjId = (String) programMap.get("id");
            if (UIUtil.isNotNullAndNotEmpty(strCRObjId)) {
                DomainObject domCRObj = DomainObject.newInstance(context, strCRObjId);
                StringList slObjSelects = new StringList();
                slObjSelects.addElement(DomainConstants.SELECT_OWNER);
                slObjSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                slObjSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");
                DomainObject.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");
                Map objCRMap = domCRObj.getInfo(context, slObjSelects);
                String strOwner = (String) objCRMap.get(DomainConstants.SELECT_OWNER);
                String strCRInitiator = (String) objCRMap.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                StringList slAffectedItemsOfCR = commonNotObj.getStringListFromMap(context, objCRMap, "from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");
                // Adding Owner to the To List
                if (UIUtil.isNotNullAndNotEmpty(strOwner)) {
                    slCRRejectionToList.addElement(strOwner);
                }

                // Adding CR Initiator to the To List
                if (UIUtil.isNotNullAndNotEmpty(strCRInitiator)) {
                    slCRRejectionToList.addElement(strCRInitiator);
                }

                // Adding Lead Change Managers of related program-projects having an affected item in the CR to the To List

                // TIGTK-5890 : PTE : 4/7/2017 : START
                // PCM : TIGTK-7390 : 14/06/2017 : AB : START
                StringList slLeadChgMngrs = new StringList();
                if (!slAffectedItemsOfCR.isEmpty()) {
                    for (int i = 0; i < slAffectedItemsOfCR.size(); i++) {
                        String strItemObjectID = (String) slAffectedItemsOfCR.get(i);
                        MapList rootList = this.getAllRootFromECPart(context, strItemObjectID);

                        for (int intIndexRoot = 0; intIndexRoot < rootList.size(); intIndexRoot++) {
                            Map rootMap = (Map) rootList.get(intIndexRoot);
                            String objectId = (String) rootMap.get(DomainObject.SELECT_ID);
                            StringList slTempLeadChgMngrs = getProgramProjectTeamMembersForPart(context, objectId, new StringList(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR), true);
                            // TIGTK-8852 : PTE : 6/30/2017 : START
                            if (slTempLeadChgMngrs != null && slTempLeadChgMngrs.size() > 0) {
                                // TIGTK-8852 : PTE : 6/30/2017 : END
                                slLeadChgMngrs.addAll(slTempLeadChgMngrs);
                            }
                        }
                    }
                }
                // PCM : TIGTK-7390 : 14/06/2017 : AB : END
                // TIGTK-5890 : PTE : 4/7/2017 : END

                // Fix for FindBugs issue Redundant Null Check : Harika Varanasi : 21 March 2017: START
                if (slLeadChgMngrs.size() > 0 && (!slLeadChgMngrs.isEmpty())) {
                    slCRRejectionToList.addAll(slLeadChgMngrs);
                }
                // Fix for FindBugs issue Redundant Null Check : Harika Varanasi : 21 March 2017: End

                // add All Impact Analysis Assignees, All Review Assignees to CR To List
                // TIGTK-10772 Suchit Gangurde: 01/11/2017: START
                StringList slAllIAAssignees = getConnectedImpactAnalysisAssignees(context, strCRObjId);
                if (!slAllIAAssignees.isEmpty()) {
                    slCRRejectionToList.addAll(slAllIAAssignees);
                }
                if (slAllIAAssignees.size() > 0 && (!slAllIAAssignees.isEmpty())) {
                    slCRRejectionToList.addAll(slAllIAAssignees);
                }
                // TIGTK-10772 Suchit Gangurde: 01/11/2017: END

                StringList slAllRouteAssignees = getChangeRouteAssignees(context, strCRObjId, "state_InReview");

                if (!slAllRouteAssignees.isEmpty()) {
                    slCRRejectionToList.addAll(slAllRouteAssignees);
                }
                // Fix for FindBugs issue Redundant Null Check : Harika Varanasi : 21 March 2017: START
                if (slAllRouteAssignees.size() > 0 && (!slAllRouteAssignees.isEmpty())) {
                    slCRRejectionToList.addAll(slAllRouteAssignees);
                }
                // Fix for FindBugs issue Redundant Null Check : Harika Varanasi : 21 March 2017: End
                // PCM : TIGTK-7622 : 18/05/2017 : AB : START
                if (!slCRRejectionToList.isEmpty()) {
                    HashSet setCRRejectionToList = new HashSet(slCRRejectionToList);
                    ArrayList<String> list = new ArrayList<String>(setCRRejectionToList);
                    slReturnList = new StringList(list);
                }
                // PCM : TIGTK-7622 : 18/05/2017 : AB : END
                // TIGTK-10757 -- START
                while (slReturnList.contains(strContextUser)) {
                    slReturnList.remove(strContextUser);
                }
                // TIGTK-10757 -- END
            }
        } catch (Exception ex) {
            logger.error("Error in getToListForCRRejectAndInProcessNotifications: ", ex);
        }
        return slReturnList;
    }

    /**
     * getCRGenericPromoteToList As a Part of TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public StringList getCRGenericPromoteToList(Context context, String[] args) throws Exception {
        StringList slCRGenericToList = new StringList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            Map payload = (Map) programMap.get("payload");
            slCRGenericToList = (StringList) payload.get("toList");
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCRGenericPromoteToList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return slCRGenericToList;
    }

    /**
     * getCRGenericPromoteToList As a Part of TGPSS_PCM_TS_152_ChangeRequestNotifications_V2.2
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public String getCRGenericPromoteFromList(Context context, String[] args) throws Exception {
        String strCRGenericFrom = "";
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            Map payload = (Map) programMap.get("payload");
            // TIGTK-10704: Suchit Gangurde: START
            if (!payload.isEmpty() && payload.containsKey("fromList")) {
                // TIGTK-13122 : START
                Object objFromList = payload.get("fromList");
                if (objFromList.getClass().equals(String.class))
                    strCRGenericFrom = (String) payload.get("fromList");
                else
                    strCRGenericFrom = (String) ((StringList) payload.get("fromList")).get(0);
            }
            // TIGTK-13122 : END
            // TIGTK-10704: Suchit Gangurde: END
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCRGenericPromoteFromList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return strCRGenericFrom;
    }

    // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 06/03/2017 |Harika Varanasi : Ends

    // TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 16/03/2017 |Harika Varanasi : Starts
    /**
     * getChangeActionInformation method is used to get all information about Change Action As a Part of TGPSS_PCM_TS_153_ChangeActionNotifications_V2.1
     * @param context
     * @param strIssueId
     * @return Map
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map getChangeActionInformation(Context context, String strCAId) throws Exception {
        Map mapCA = new HashMap();
        try {

            if (UIUtil.isNotNullAndNotEmpty(strCAId)) {
                DomainObject domCAObj = DomainObject.newInstance(context, strCAId);

                StringList slObjectSelects = new StringList(13);
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                slObjectSelects.addElement(DomainConstants.SELECT_NAME);
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
                slObjectSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE + "]");
                slObjectSelects.addElement("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                // TIGTK-5924 | 30/03/2017 | Harika Varanasi : Starts
                slObjectSelects.addElement(DomainConstants.SELECT_OWNER);
                // TIGTK-5924 | 30/03/2017 | Harika Varanasi : Ends
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.name");
                // TIGTK-5924 | 30/03/2017 | Harika Varanasi : Starts
                slObjectSelects.addElement("from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.name");
                slObjectSelects.addElement("from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id");
                slObjectSelects.addElement("from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.id");
                slObjectSelects.addElement("from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.name");

                DomainObject.MULTI_VALUE_LIST.add("from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.name");
                DomainObject.MULTI_VALUE_LIST.add("from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id");
                DomainObject.MULTI_VALUE_LIST.add("from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.id");
                DomainObject.MULTI_VALUE_LIST.add("from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.name");
                // TIGTK-5924 | 30/03/2017 | Harika Varanasi : Ends

                mapCA = domCAObj.getInfo(context, slObjectSelects);
                // TIGTK-5924 | 30/03/2017 | Harika Varanasi : Starts

                // TIGTK-10802 : Sayali D : 9 -Nov -2017 START
                StringList slCOobjSelectables = new StringList();
                slCOobjSelectables.add(DomainConstants.SELECT_DESCRIPTION);
                slCOobjSelectables.add(DomainConstants.SELECT_NAME);
                slCOobjSelectables.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                // TIGTK-10802 : Sayali D : 9 -Nov -2017 END
                if (mapCA.size() > 0) {
                    String strCurrent = (String) mapCA.get(DomainConstants.SELECT_CURRENT);
                    if (UIUtil.isNotNullAndNotEmpty(strCurrent) && TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL.equalsIgnoreCase(strCurrent)) {
                        mapCA.put(DomainConstants.SELECT_CURRENT, TigerConstants.STATE_INREVIEW_CR);
                    }
                    // TIGTK-11522 : TS : 4/12/2017 : START
                    if (UIUtil.isNotNullAndNotEmpty(strCurrent) && TigerConstants.STATE_CHANGEACTION_PENDING.equalsIgnoreCase(strCurrent)) {
                        mapCA.put(DomainConstants.SELECT_CURRENT, TigerConstants.STATE_PSS_CHANGEORDER_PREPARE);
                    }
                    // TIGTK-11522 : TS : 4/12/2017 : END
                    pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
                    StringList slCAContentId = new StringList();
                    StringList slCAContentName = new StringList();
                    if (mapCA.containsKey("from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.id")) {
                        slCAContentId.addAll(commonObj.getStringListFromMap(context, mapCA, "from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.id"));
                        slCAContentName.addAll(commonObj.getStringListFromMap(context, mapCA, "from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.name"));
                    }

                    if (mapCA.containsKey("from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id")) {
                        slCAContentId.addAll(commonObj.getStringListFromMap(context, mapCA, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id"));
                        slCAContentName.addAll(commonObj.getStringListFromMap(context, mapCA, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.name"));
                    }

                    if (!slCAContentId.isEmpty() && slCAContentId.size() > 0) {
                        mapCA.put("CAContentId", slCAContentId);
                        mapCA.put("CAContentName", slCAContentName);
                    }

                    // TIGTK-10802 : Sayali D : 9 -Nov -2017 START
                    String strConnectedCOId = (String) mapCA.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
                    DomainObject domCOObject = DomainObject.newInstance(context, strConnectedCOId);
                    // Get the Information related to Change Order
                    Map mapChangeOrderInfo = domCOObject.getInfo(context, slCOobjSelectables);
                    String strCONumber = (String) mapChangeOrderInfo.get(DomainConstants.SELECT_NAME);
                    String strCOCreator = (String) mapChangeOrderInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                    String strCODescription = (String) mapChangeOrderInfo.get(DomainConstants.SELECT_DESCRIPTION);
                    mapCA.put("CO_id", strConnectedCOId);
                    mapCA.put("CO_name", strCONumber);
                    mapCA.put("CO_Description", strCODescription);
                    mapCA.put("CO_Creator", strCOCreator);
                    // TIGTK-10802 : Sayali D : 9 -Nov -2017 END
                }
                // TIGTK-5924 | 30/03/2017 | Harika Varanasi : Starts
            }

        } catch (RuntimeException re) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeActionInformation: ", re);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeActionInformation: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            // TIGTK-9425| 01/09/17 : Start
        } finally {
            DomainObject.MULTI_VALUE_LIST.remove("from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.name");
            DomainObject.MULTI_VALUE_LIST.remove("from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id");
            DomainObject.MULTI_VALUE_LIST.remove("from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.id");
            DomainObject.MULTI_VALUE_LIST.remove("from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.name");
        }
        // TIGTK-9425| 01/09/17 : End
        return mapCA;
    }

    /**
     * transformCAMapToHTMLList As a Part of TGPSS_PCM_TS_153_ChangeActionNotifications_V2.1
     * @param context
     * @param objectMap
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public MapList transformCAMapToHTMLList(Context context, Map objectMap, String strBaseURL, boolean isTaskRejected, boolean isReassign) throws Exception {
        MapList mlInfoList = new MapList();
        try {

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);

            StringList slHyperLinkLabelKey = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.CANotification.HyperLinkLabelKey"), ",");
            StringList slHyperLinkLabelKeyIds = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.CANotification.HyperLinkLabelKeyIds"), ",");
            // TIGTK-11455 :START
            if (isReassign) {
                initializeCALinkedHashMap(isReassign, isTaskRejected, lhmCACommentsSelectionStore);
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCACommentsSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            }
            if (isTaskRejected) {
                initializeCALinkedHashMap(isReassign, isTaskRejected, lhmCARejectCommentsSelectionStore);
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCARejectCommentsSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            } else {
                initializeCALinkedHashMap(isReassign, isTaskRejected, lhmCASelectionStore);
                mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCASelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);
            }
            // TIGTK-11455 :END

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in transformCAMapToHTMLList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mlInfoList;
    }

    // TIGTK-10802 : Sayali D : 9 -Nov -2017 START
    /**
     * transformCAMapToHTMLList As a Part of TIGTK-10802
     * @param context
     * @param objectMap
     * @return
     * @throws Exception
     * @author Sayali Deshpande : SteepGraph
     */
    @SuppressWarnings({ "rawtypes" })
    public MapList transformCAMapToHTMLList(Context context, Map objectMap, String strBaseURL, String notificationObjName) throws Exception {
        MapList mlInfoList = new MapList();
        try {

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);

            StringList slHyperLinkLabelKey = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.CANotification.HyperLinkLabelKey"), ",");
            StringList slHyperLinkLabelKeyIds = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.CANotification.HyperLinkLabelKeyIds"), ",");
            initializeCALinkedHashMap();

            // TIGTK-10802 : Sayali D : 9 -Nov -2017 START
            if (notificationObjName.equalsIgnoreCase("PSS_CAImplementItemReassignNotification")) {
                // CO as hyperlink
                slHyperLinkLabelKey.add("CO");
                slHyperLinkLabelKeyIds.add("CO_id"); // objectMap has CO_id which store CO objectID

                // add CO information to be fetched from objectMap
                lhmCASelectionStore.put("CO", "CO_name");
                lhmCASelectionStore.put("CO_Creator", "CO_Creator");
                lhmCASelectionStore.put("CO_Description", "CO_Description");
            }
            // TIGTK-10802 : Sayali D : 9 -Nov -2017 END
            mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmCASelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in transformCAMapToHTMLList: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return mlInfoList;
    }

    // TIGTK-10802 : Sayali D : 9 -Nov -2017 END
    /**
     * initializeCALinkedHashMap As a Part of TGPSS_PCM_TS_153_ChangeActionNotifications_V2.1
     * @throws Exception
     *             if the operation fails
     * @author Harika Varanasi : SteepGraph
     */
    public void initializeCALinkedHashMap() throws Exception {
        try {

            if (lhmCASelectionStore != null && (lhmCASelectionStore.isEmpty())) {

                lhmCASelectionStore.put("Title", "SectionHeader");
                lhmCASelectionStore.put("Subject", "SectionSubject");
                lhmCASelectionStore.put("Main_Information", "SectionHeader");
                lhmCASelectionStore.put("Project_Code", "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                lhmCASelectionStore.put("Project_Description",
                        "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                lhmCASelectionStore.put("CA", DomainConstants.SELECT_NAME);
                lhmCASelectionStore.put("CA_Description", DomainConstants.SELECT_DESCRIPTION);
                lhmCASelectionStore.put("State", DomainConstants.SELECT_CURRENT);
                lhmCASelectionStore.put("CA_Virtual_Implementation_Planned_Date",
                        "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE + "]");
                // TIGTK-5924 | 31/03/2017 | Harika Varanasi : Starts
                lhmCASelectionStore.put("CA_Creator", DomainConstants.SELECT_OWNER);
                lhmCASelectionStore.put("Useful_Links", "SectionHeader");
                lhmCASelectionStore.put("CA_Content", "CAContentName");
                // TIGTK-5924 | 31/03/2017 | Harika Varanasi : Ends
            }

        } catch (RuntimeException re) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeCALinkedHashMap: ", re);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeCALinkedHashMap: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }

    public void initializeCALinkedHashMap(boolean isReassign, boolean isTaskRejected, LinkedHashMap<String, String> linkedHashMap) throws Exception {
        try {

            if (linkedHashMap != null && (linkedHashMap.isEmpty())) {

                linkedHashMap.put("Title", "SectionHeader");
                linkedHashMap.put("Subject", "SectionSubject");
                linkedHashMap.put("Main_Information", "SectionHeader");
                linkedHashMap.put("Project_Code", "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                linkedHashMap.put("Project_Description", "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                linkedHashMap.put("CA", DomainConstants.SELECT_NAME);
                linkedHashMap.put("CA_Description", DomainConstants.SELECT_DESCRIPTION);
                linkedHashMap.put("State", DomainConstants.SELECT_CURRENT);
                linkedHashMap.put("CA_Virtual_Implementation_Planned_Date",
                        "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE + "]");
                linkedHashMap.put("CA_Creator", DomainConstants.SELECT_OWNER);
                // TIGTK-11455 :START
                if (isReassign) {
                    linkedHashMap.put("Comments", "Comments");
                }
                if (isTaskRejected) {
                    linkedHashMap.put("RejectedTask_Comment", "RejectedTask_Comment");
                }
                // TIGTK-11455 :END
                linkedHashMap.put("Useful_Links", "SectionHeader");
                linkedHashMap.put("CA_Content", "CAContentName");

            }
        } catch (RuntimeException re) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeCALinkedHashMap: ", re);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw re;
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in initializeCALinkedHashMap: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

    }

    /**
     * getChangeApprovalListAssignees As a Part of TGPSS_PCM_TS_153_ChangeActionNotifications_V2.1
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public StringList getChangeApprovalListAssignees(Context context, String[] args) throws Exception {
        StringList slCAApprovalRouteAssignees = new StringList();

        // PCM : TIGTK-10799 : 13/11/2017 : Aniket Madhav : START
        pss.ecm.ui.MfgChangeOrder_mxJPO mfgChangeOrder = new pss.ecm.ui.MfgChangeOrder_mxJPO();
        String strContextUser = mfgChangeOrder.getGenericContextOrLoginUser(context, args);
        // PCM : TIGTK-10799 : 13/11/2017 : Aniket Madhav : END

        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCAObjId = (String) programMap.get("id");
            if (UIUtil.isNotNullAndNotEmpty(strCAObjId)) {
                DomainObject domChangeObj = DomainObject.newInstance(context, strCAObjId);
                StringList slTempCAAssignees = new StringList();
                if (domChangeObj.isKindOf(context, TigerConstants.TYPE_CHANGEACTION)) {
                    String strCAStates = TigerConstants.STATE_CHANGEACTION_PENDING + "," + TigerConstants.STATE_CHANGEACTION_INWORK + "," + TigerConstants.STATE_CHANGEACTION_INAPPROVAL;
                    slTempCAAssignees = getChangeRouteAssignees(context, strCAObjId, strCAStates);
                } else if (domChangeObj.isKindOf(context, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {
                    String strMCAStates = TigerConstants.STATE_PSS_MCA_PREPARE + "," + TigerConstants.STATE_PSS_MCA_INWORK + "," + TigerConstants.STATE_PSS_MCA_INREVIEW;
                    slTempCAAssignees = getChangeRouteAssignees(context, strCAObjId, strMCAStates);
                }

                // PCM : TIGTK-7745 : 23/08/2017 : AB : START
                if (!slTempCAAssignees.isEmpty()) {
                    HashSet setCAApprovalRouteAssignees = new HashSet(slTempCAAssignees);
                    ArrayList<String> list = new ArrayList<String>(setCAApprovalRouteAssignees);
                    slCAApprovalRouteAssignees = new StringList(list);
                }
                // PCM : TIGTK-7745 : 23/08/2017 : AB : END
            }

            // PCM : TIGTK-10799 : 23/08/2017 : Aniket Madhav : START
            if (slCAApprovalRouteAssignees.contains(strContextUser)) {
                while (slCAApprovalRouteAssignees.contains(strContextUser)) {
                    slCAApprovalRouteAssignees.remove(strContextUser);
                }
            }
            // PCM : TIGTK-10799 : 23/08/2017 : Aniket Madhav : END

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeApprovalListAssignees: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return slCAApprovalRouteAssignees;
    }

    /**
     * getCATaskRejectAssignees As apart of TGPSS_PCM_TS_153_ChangeActionNotifications_V2.1
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public StringList getCATaskRejectAssignees(Context context, String[] args) throws Exception {
        StringList slCATaskRejectList = new StringList();
        try {

            pss.ecm.notification.CommonNotificationUtil_mxJPO commonNotObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCAObjId = (String) programMap.get("id");
            if (UIUtil.isNotNullAndNotEmpty(strCAObjId)) {
                DomainObject domCAObj = DomainObject.newInstance(context, strCAObjId);

                StringList objectSelects = new StringList();
                String selectCATechAssignee = new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE).append("].to.name").toString();
                objectSelects.addElement(selectCATechAssignee);
                objectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.id");
                objectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.id");
                objectSelects.addElement(DomainConstants.SELECT_TYPE);

                Map mapCAObj = domCAObj.getInfo(context, objectSelects);

                // PCM : TIGTK-7745 : 28/08/2017 : AB : START
                if (!mapCAObj.isEmpty()) {
                    pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                    slCATaskRejectList = commonNotObj.getStringListFromMap(context, mapCAObj, selectCATechAssignee);
                    String strChangeObjectType = (String) mapCAObj.get(DomainConstants.SELECT_TYPE);

                    StringList slPMLList = new StringList();
                    if (TigerConstants.TYPE_CHANGEACTION.equalsIgnoreCase(strChangeObjectType)) {
                        slPMLList = commonObj.getProgramProjectTeamMembersForChange(context, (String) mapCAObj.get("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.id"),
                                new StringList(TigerConstants.ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD), true);
                    } else if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strChangeObjectType)) {
                        slPMLList = commonObj.getProgramProjectTeamMembersForChange(context, (String) mapCAObj.get("to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.id"),
                                new StringList(TigerConstants.ROLE_PSS_PROGRAM_MANUFACTURING_LEADER), true);
                    }
                    // PCM : TIGTK-7745 : 28/08/2017 : AB : END
                    // TIGTK-13917 : 03-04-2018 : START
                    StringList slTaskApprovalAssigneesList = getChangeApprovalListAssignees(context, args);
                    if (!slTaskApprovalAssigneesList.isEmpty()) {
                        slCATaskRejectList.addAll(slTaskApprovalAssigneesList);
                    }
                    // TIGTK-13917 : 03-04-2018 : END
                    if (!slPMLList.isEmpty()) {
                        slCATaskRejectList.addAll(slPMLList);
                    }
                    // TIGTK-13917 : 03-04-2018 : START
                    String rpeUserName = PropertyUtil.getGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME);
                    if (!slCATaskRejectList.isEmpty()) {
                        slCATaskRejectList.remove(rpeUserName);
                        HashSet<?> hsUniqueCATaskRejectList = new HashSet<Map>();
                        hsUniqueCATaskRejectList.addAll(slCATaskRejectList);
                        slCATaskRejectList.clear();
                        slCATaskRejectList.addAll(hsUniqueCATaskRejectList);
                    }
                    // TIGTK-13917 : 03-04-2018 : END
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCATaskRejectAssignees: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return slCATaskRejectList;

    }

    /**
     * getCANotificationBodyHTML method is used to CA messageHTML in Notification Object As apart of TGPSS_PCM_TS_153_ChangeActionNotifications_V2.1
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getCANotificationBodyHTML(Context context, String[] args) throws Exception {
        String messageHTML = "";
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCAObjId = (String) programMap.get("id");
            String strBaseURL = (String) programMap.get("baseURL");
            String notificationObjName = (String) programMap.get("notificationName");
            String attrSubText = PropertyUtil.getSchemaProperty(context, "attribute_SubjectText");
            StringList busSelects = new StringList("attribute[" + attrSubText + "]");
            // TIGTK-10709 -- START
            String strSectionSub = DomainConstants.EMPTY_STRING;
            // TIGTK-10709 -- END
            Map CAMap = (Map) getChangeActionInformation(context, strCAObjId);
            // //TIGTK-11455 :START
            Map payLoadMap = (Map) programMap.get("payload");
            if (payLoadMap != null) {
                if (payLoadMap.containsKey("RejectedTask_Comment")) {
                    String strComments = (String) payLoadMap.get("RejectedTask_Comment");
                    CAMap.put("RejectedTask_Comment", strComments);
                }
                if (payLoadMap.containsKey("Comments")) {
                    String strComments = (String) payLoadMap.get("Comments");
                    CAMap.put("Comments", strComments);
                }
            }
            // TIGTK-11455 :END
            String strSubjectKey = "";
            MapList mpNotificationList = DomainObject.findObjects(context, "Notification", notificationObjName, "*", "*", TigerConstants.VAULT_ESERVICEADMINISTRATION, "", null, true, busSelects,
                    (short) 0);

            if (mpNotificationList != null && (!mpNotificationList.isEmpty())) {
                strSubjectKey = (String) ((Map) mpNotificationList.get(0)).get("attribute[" + attrSubText + "]");

            }
            if (UIUtil.isNotNullAndNotEmpty(strSubjectKey)) {
                CAMap.put("SectionSubject", EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strSubjectKey));
            } else {
                CAMap.put("SectionSubject", "");
            }
            boolean isReassign = false;
            // TIGTK-10709 -- START
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_CATaskReassignNotification".equalsIgnoreCase(notificationObjName)) {
                strSectionSub = getTaskReassignmentSubject(context, args);
                CAMap.put("SectionSubject", strSectionSub);
                isReassign = true;
            }
            // TIGTK-10709 -- END

            // TIGTK-10768 -- START
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_CAAssigneeReassignNotification".equalsIgnoreCase(notificationObjName)) {
                strSectionSub = getChangeReassignmentSubject(context, args);
                CAMap.put("SectionSubject", strSectionSub);
            }
            // TIGTK-10768 -- END
            // TIGTK-11455 :START
            boolean isTaskRejected = false;
            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_CATaskRejectNotification".equalsIgnoreCase(notificationObjName)) {

                isTaskRejected = true;
            }

            MapList mlInfoList = transformCAMapToHTMLList(context, CAMap, strBaseURL, isTaskRejected, isReassign);
            // TIGTK-11455 :END
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            String strStyleSheet = commonObj.getFormStyleSheet(context, "PSS_NotificationStyleSheet");
            messageHTML = commonObj.getHTMLTwoColumnTable(context, mlInfoList, strStyleSheet);
        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCANotificationBodyHTML: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return messageHTML;
    }

    // TIGTK-10802 : Sayali D : 9 -Nov -2017 START
    /**
     * getImplementedItemsNotificationBodyHTML method is used to messageHTML in Notification Object As apart of TGPSS_PCM_TS_153_ChangeActionNotifications_V2.1 and TIGTK-10802
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Sayali Deshpande : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public String getImplementedItemsNotificationBodyHTML(Context context, String[] args) throws Exception {
        String messageHTML = "";
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strCAObjId = (String) programMap.get("id");
            String strBaseURL = (String) programMap.get("baseURL");
            String notificationObjName = (String) programMap.get("notificationName");
            String attrSubText = PropertyUtil.getSchemaProperty(context, "attribute_SubjectText");
            StringList busSelects = new StringList("attribute[" + attrSubText + "]");
            String strSectionSub = DomainConstants.EMPTY_STRING;
            Map CAMap = (Map) getChangeActionInformation(context, strCAObjId);

            String strSubjectKey = "";
            MapList mpNotificationList = DomainObject.findObjects(context, "Notification", notificationObjName, "*", "*", TigerConstants.VAULT_ESERVICEADMINISTRATION, "", null, true, busSelects,
                    (short) 0);

            if (mpNotificationList != null && (!mpNotificationList.isEmpty())) {
                strSubjectKey = (String) ((Map) mpNotificationList.get(0)).get("attribute[" + attrSubText + "]");

            }
            if (UIUtil.isNotNullAndNotEmpty(strSubjectKey)) {
                CAMap.put("SectionSubject", EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strSubjectKey));
            } else {
                CAMap.put("SectionSubject", "");
            }

            if (UIUtil.isNotNullAndNotEmpty(notificationObjName) && "PSS_CATaskReassignNotification".equalsIgnoreCase(notificationObjName)) {
                strSectionSub = getTaskReassignmentSubject(context, args);
                CAMap.put("SectionSubject", strSectionSub);
            }

            String strRPEAffectedItemIds = PropertyUtil.getGlobalRPEValue(context, "PSS_AffectedItemIds");
            strRPEAffectedItemIds = strRPEAffectedItemIds.replace("[", "");
            strRPEAffectedItemIds = strRPEAffectedItemIds.replace("]", "");
            String strRPEAffectedItemNames = PropertyUtil.getGlobalRPEValue(context, "PSS_AffectedItemNames");
            strRPEAffectedItemNames = strRPEAffectedItemNames.replace("[", "");
            strRPEAffectedItemNames = strRPEAffectedItemNames.replace("]", "");
            PropertyUtil.setGlobalRPEValue(context, "PSS_AffectedItemIds", DomainConstants.EMPTY_STRING);
            PropertyUtil.setGlobalRPEValue(context, "PSS_AffectedItemNames", DomainConstants.EMPTY_STRING);

            // FindBug TIGTK-11306
            StringList slCAContentId = FrameworkUtil.split(strRPEAffectedItemIds, ",");
            StringList slCAContentName = FrameworkUtil.split(strRPEAffectedItemNames, ",");
            CAMap.remove("CAContentId");
            CAMap.put("CAContentId", slCAContentId);
            CAMap.remove("CAContentName");
            CAMap.put("CAContentName", slCAContentName);
            MapList mlInfoList = transformCAMapToHTMLList(context, CAMap, strBaseURL, notificationObjName);
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            String strStyleSheet = commonObj.getFormStyleSheet(context, "PSS_NotificationStyleSheet");
            messageHTML = commonObj.getHTMLTwoColumnTable(context, mlInfoList, strStyleSheet);
        } catch (Exception ex) {
            logger.error("Error in getImplementedItemsNotificationBodyHTML: ", ex);
        }
        return messageHTML;
    }

    // TIGTK-10802 : Sayali D : 9 -Nov -2017 END

    // TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 16/03/2017 |Harika Varanasi : Ends

    /**
     * Modified by PCM : TIGTK-9324 : 09/08/2017 : AB. getChildOrParentLevel method is used on Assessment table to show the level.
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author PK : SteepGraph
     */
    public Vector getChildOrParentLevel(Context context, String[] args) throws Exception {
        Vector<String> vObjLevels = new Vector<String>();
        try {
            Map mapIDVSLevel = new HashMap<>();
            Map programMap = (Map) JPO.unpackArgs(args);
            Map paramList = (Map) programMap.get("paramList");
            String strTableRowID = (String) paramList.get("emxTableRowId");
            String strContextChangeId = (String) paramList.get("contextCOId");
            pss.ecm.PSS_enoECMChangeAssessment_mxJPO changeAssessment = new pss.ecm.PSS_enoECMChangeAssessment_mxJPO(context, args);

            // Get the all change assessment items of selected part
            Map mapArgs = new HashMap();
            mapArgs.put("emxTableRowId", strTableRowID);
            mapArgs.put("contextCOId", strContextChangeId);
            MapList mlResult = changeAssessment.getChangeAssessmentItems(context, JPO.packArgs(mapArgs));

            // Create map of Affected Item's id and Level of Change Assesssment.
            if (!mlResult.isEmpty()) {
                int intSize = mlResult.size();
                for (int i = 0; i < intSize; i++) {
                    Map mapItemInfo = (Map) mlResult.get(i);
                    String strLabel = (String) mapItemInfo.get("strLabel");
                    String strLevel = (String) mapItemInfo.get("level");
                    String strItemID = (String) mapItemInfo.get("id");

                    // If change assessment item is child then append (+) sign with level and if it is parent the append (-) sign with level
                    if ("Where Used".equalsIgnoreCase(strLabel) || "Parent CAD Parts".equalsIgnoreCase(strLabel)) {
                        strLevel = "-" + strLevel;
                    } else {
                        strLevel = "+" + strLevel;
                    }
                    mapIDVSLevel.put(strItemID, strLevel);
                }
            }

            // Set the Level as per Item listed in table of Change Assessment
            MapList objectList = (MapList) programMap.get("objectList");
            Iterator itrObj = objectList.iterator();
            while (itrObj.hasNext()) {
                Map mpTemp = (Map) itrObj.next();
                String strItemID = (String) mpTemp.get("id");
                String strRelationship = (String) mpTemp.get("relationship");

                // If Part is connected with CAD than give constants (1) sign
                if (DomainConstants.RELATIONSHIP_PART_SPECIFICATION.equalsIgnoreCase(strRelationship) || TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING.equalsIgnoreCase(strRelationship)) {
                    vObjLevels.addElement("+1");
                } else {
                    vObjLevels.addElement((String) mapIDVSLevel.get(strItemID));
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChildOrParentLevel: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return vObjLevels;
    }

    /**
     * This method gets the Interchangeability status on CR.
     * @param context
     * @param args
     * @throws Exception
     * @author Suchit Gangurde
     */
    public String getInterchangeabilityStatus(Context context, String[] args) throws Exception {
        String sHTML = "";
        try {
            HashMap<String, Object> programMap = (HashMap<String, Object>) JPO.unpackArgs(args);
            HashMap<String, Object> paramMap = (HashMap<String, Object>) programMap.get("paramMap");
            String strCRId = (String) paramMap.get("objectId");
            DomainObject domCRObj = DomainObject.newInstance(context, strCRId);
            StringList slBusSelects = new StringList(DomainConstants.SELECT_ID);
            slBusSelects.add(DomainConstants.SELECT_NAME);
            StringList relSelects = new StringList();
            relSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_INTERCHANGEABILITY + "]");
            relSelects.add("from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].attribute[TigerConstants.ATTRIBUTE_PSS_INTERCHANGEABILITY].value");
            MapList mlAffectedItems = domCRObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, // relationship
                    "*", // type Pattern
                    slBusSelects, // object selects
                    relSelects, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where
                    null, // relationship where
                    0);
            List<String> lstPartsNotInterchangeable = getNotInterchangeableParts(context, mlAffectedItems);
            if (lstPartsNotInterchangeable.isEmpty()) {
                programMap.put("InterchangeabilityStatus", "Complete");
                sHTML = "<img src=\"../common/images/iconActionApprove.png\" alt=\"Interchangeability status Completed\" title=\"Interchangeability status Completed\"></img>";
            } else {
                programMap.put("InterchangeabilityStatus", "InComplete");
                StringBuilder sbFinal = new StringBuilder();
                sbFinal.append("<table><tr>");
                sbFinal.append("<td class=\"heading2\">Part ");
                Iterator<String> itr = lstPartsNotInterchangeable.iterator();
                while (itr.hasNext()) {
                    sbFinal.append(itr.next());
                    if (itr.hasNext()) {
                        sbFinal.append(", ");
                    }
                }
                sbFinal.append(" are not interchangeable. Related parents are not in the affected Items");
                sbFinal.append("</td></tr><table>");
                sHTML = sbFinal.toString();
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getInterchangeabilityStatus: ", Ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return sHTML;
    }

    /**
     * This method returns the List of Part IDs which are not interchangeable.
     * @param context
     * @param args
     * @throws Exception
     * @author Suchit Gangurde
     */
    private List<String> getNotInterchangeableParts(Context context, MapList mlAffectedItems) throws FrameworkException {
        List<String> lstNotInterChangeablePart = new ArrayList<String>();
        try {
            List<String> lstAffectedItemPartIds = getAffectedItemsIDList(mlAffectedItems);
            Iterator<?> itrAffectedItems = mlAffectedItems.iterator();
            while (itrAffectedItems.hasNext()) {
                Map<?, ?> mpAffectedItem = (Map<?, ?>) itrAffectedItems.next();
                String strObjId = (String) mpAffectedItem.get(DomainConstants.SELECT_ID);
                String strPartName = (String) mpAffectedItem.get(DomainConstants.SELECT_NAME);
                String strValue = (String) mpAffectedItem.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_INTERCHANGEABILITY + "]");
                if ("No".equalsIgnoreCase(strValue)) {
                    List<String> lstAllPArent = getAllParentIDs(context, strObjId);

                    Iterator<String> itrParentIDs = lstAllPArent.iterator();
                    while (itrParentIDs.hasNext()) {
                        if (!lstAffectedItemPartIds.contains(itrParentIDs.next()) && !lstNotInterChangeablePart.contains(strPartName)) {
                            lstNotInterChangeablePart.add(strPartName);
                        }
                    }
                }
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getNotInterchangeableParts: ", Ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return lstNotInterChangeablePart;
    }

    /**
     * This method returns List of AffectedItems ID from AffectedItems MapList.
     * @param context
     * @param args
     * @throws Exception
     * @author Suchit Gangurde
     */
    private List<String> getAffectedItemsIDList(MapList mlAffectedItems) {
        List<String> lstFinalList = getIDListFromMap(DomainConstants.SELECT_ID, mlAffectedItems);
        return lstFinalList;

    }

    /**
     * This method returns the List of parent ID of given AfftetedItem.
     * @param context
     * @param args
     * @throws Exception
     * @author Suchit Gangurde
     */
    private List<String> getAllParentIDs(Context context, String sObjectId) throws FrameworkException {
        List<String> lstReturn = null;
        try {
            StringList slBusSelects = new StringList(DomainConstants.SELECT_ID);
            slBusSelects.add(DomainConstants.SELECT_ID);
            String sRelPattern = DomainRelationship.RELATIONSHIP_EBOM + "," + TigerConstants.RELATIONSHIP_CADSUBCOMPONENT;
            DomainObject domChildObj = DomainObject.newInstance(context, sObjectId);
            MapList mpListAllParent = domChildObj.getRelatedObjects(context, sRelPattern, // relationship
                    "*", // type Pattern
                    slBusSelects, // object selects
                    null, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    null, // object where
                    null, // relationship where
                    0);
            lstReturn = getIDListFromMap(DomainConstants.SELECT_ID, mpListAllParent);
        } catch (Exception Ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getAllParentIDs: ", Ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return lstReturn;

    }

    /**
     * This method is used to provide list of IDs from MapList.
     * @param context
     * @param args
     * @throws Exception
     * @author Suchit Gangurde
     */
    private List<String> getIDListFromMap(String sKey, MapList mplstItems) {
        List<String> lstFinalList = new ArrayList<String>();
        try {

            Iterator<?> itrItem = mplstItems.iterator();
            while (itrItem.hasNext()) {
                Map<?, ?> mpItem = (Map<?, ?>) itrItem.next();
                String strObjId = (String) mpItem.get(sKey);
                lstFinalList.add(strObjId);
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getIDListFromMap: ", Ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        return lstFinalList;

    }

    // PCM TIGTK-6382 : PSE : 27-04-2017 : START
    /**
     * Method used to get connected change requests of the affected item with its current state and program-project
     * @param context
     * @param args
     * @return StringList@author -- Priyanka Salunke
     * @since -- 27/April/2017
     * @throws Exception
     */
    public StringList getConnectedCRs(Context context, String[] args) throws Exception {

        StringList lstCRInfo = new StringList();
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) programMap.get("objectList");
            if (mlObjectList != null) // START If
            {

                Iterator<Map<?, ?>> irtAffectedItem = mlObjectList.iterator();
                // Object Selects
                String SELECT_PROGRAM_PROJECT_NAME_FROM_CR = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from." + DomainConstants.SELECT_NAME;
                String SELECT_PROGRAM_PROJECT_ID_FROM_CR = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from." + DomainConstants.SELECT_ID;
                String TABLE_DATA_HTML = "<td style=\"border-style: solid;border-width:0.1px;padding:5px;width:60%\">";
                StringList slObjectSelect = new StringList();
                slObjectSelect.add(DomainConstants.SELECT_ID);
                slObjectSelect.add(DomainConstants.SELECT_NAME);
                slObjectSelect.add(DomainConstants.SELECT_CURRENT);
                slObjectSelect.add(SELECT_PROGRAM_PROJECT_NAME_FROM_CR);
                slObjectSelect.add(SELECT_PROGRAM_PROJECT_ID_FROM_CR);

                // Relationship Selects
                StringList slRelSelect = new StringList();
                slRelSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
                // Type Pattern
                Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
                // Relationship Pattern
                Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM);
                // Object where expression
                StringBuilder sbWhere = new StringBuilder();
                sbWhere.append("current != ");
                sbWhere.append(TigerConstants.STATE_COMPLETE_CR);
                sbWhere.append(" || current != ");
                sbWhere.append(TigerConstants.STATE_REJECTED_CR);

                while (irtAffectedItem.hasNext()) // START: Firtst While Loop
                {
                    Map<?, ?> mpAfftecteditemInfo = irtAffectedItem.next();
                    String strAfftedItemObjectId = (String) mpAfftecteditemInfo.get(DomainConstants.SELECT_ID);
                    DomainObject domAffectedItem = DomainObject.newInstance(context, strAfftedItemObjectId);
                    // Get Related CRs of Affected Items
                    MapList mlRelatedCRsList = domAffectedItem.getRelatedObjects(context, // context
                            relationshipPattern.getPattern(), // relationship
                            // pattern
                            typePattern.getPattern(), // object pattern
                            slObjectSelect, // object selects
                            slRelSelect, // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 0, // recursion level
                            sbWhere.toString(), // object where clause
                            null, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, null, null, null);
                    // Process if connected CR list is not empty
                    if (mlRelatedCRsList != null && !mlRelatedCRsList.isEmpty()) {
                        Iterator<Map<?, ?>> itrCR = mlRelatedCRsList.iterator();
                        StringBuilder sbHTMLTable = null;
                        StringBuilder sbFinalHTMLForAffectedItem = new StringBuilder();
                        while (itrCR.hasNext()) {
                            Map<?, ?> mpCR = itrCR.next();
                            String strCRObjectId = (String) mpCR.get(DomainConstants.SELECT_ID);
                            String strCRName = (String) mpCR.get(DomainConstants.SELECT_NAME);
                            String strCRState = (String) mpCR.get(DomainConstants.SELECT_CURRENT);
                            String strProgramProjectId = (String) mpCR.get(SELECT_PROGRAM_PROJECT_ID_FROM_CR);
                            String strProgramProjectName = (String) mpCR.get(SELECT_PROGRAM_PROJECT_NAME_FROM_CR);
                            sbHTMLTable = new StringBuilder();
                            sbHTMLTable.append("<tr>");
                            sbHTMLTable.append(TABLE_DATA_HTML);
                            sbHTMLTable.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                            sbHTMLTable.append(strCRObjectId);
                            sbHTMLTable.append("', '700', '600', 'false', 'popup', '')\" class=\"object\">");
                            sbHTMLTable.append(" ");
                            sbHTMLTable.append(strCRName);
                            sbHTMLTable.append("</a></td>");
                            sbHTMLTable.append(TABLE_DATA_HTML);
                            sbHTMLTable.append(strCRState);
                            sbHTMLTable.append("</td>");
                            sbHTMLTable.append(TABLE_DATA_HTML);
                            sbHTMLTable.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                            sbHTMLTable.append(strProgramProjectId);
                            sbHTMLTable.append("', '700', '600', 'false', 'popup', '')\" class=\"object\">");
                            sbHTMLTable.append(" ");
                            sbHTMLTable.append(strProgramProjectName);
                            sbHTMLTable.append("</a></td>");
                            sbHTMLTable.append("</tr>");
                            sbFinalHTMLForAffectedItem.append("<table>");
                            sbFinalHTMLForAffectedItem.append(sbHTMLTable.toString());
                            sbFinalHTMLForAffectedItem.append("</table>");

                        }
                        lstCRInfo.add(sbFinalHTMLForAffectedItem.toString());
                    } else {
                        lstCRInfo.add(DomainConstants.EMPTY_STRING);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in pss.ecm.enoECMChange : getConnectedCRs()  ", e);
            throw e;
        }
        return lstCRInfo;
    }// PCM : TIGTK-6382 : PSE : 27-04-2017 :END

    /**
     * This method is used to get All Root Part of given EC Part. PCM : TIGTK-7390 : 14/06/2017 : AB
     * @param context
     * @param ecPartId
     * @return
     * @throws Exception
     */
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

        } catch (Exception ex) {
            logger.error("Error in getAllRootFromECPart: ", ex);
            throw ex;
        }
        return allRootParts;
    }

    // TIGTK-6843:Phase-2.0:PKH:Start
    /**
     * To Cancelled the CO and Related CA.
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public String cancelCO(Context context, String[] args) throws Exception {
        boolean bIsTransactionActive = false;
        ContextUtil.startTransaction(context, true);
        bIsTransactionActive = true;
        StringBuffer strCADObjectsTobeDeleted = new StringBuffer();
        logger.debug("cancelCO : START");
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            context.setCustomData("isPromoteFromCOAction", "true");
            String strObjectId = (String) programMap.get("objectId");
            String strFunctionality = (String) programMap.get("functionality");
            boolean isDelete = false;
            if (strFunctionality.equalsIgnoreCase("cancelCOAndDeleteImplItems")) {
                isDelete = true;
            }
            DomainObject domCOObject = DomainObject.newInstance(context, strObjectId);

            StringList slCASelectable = new StringList();
            slCASelectable.add(DomainConstants.SELECT_ID);
            slCASelectable.add(DomainConstants.SELECT_CURRENT);
            slCASelectable.add(DomainConstants.SELECT_OWNER);
            slCASelectable.add(DomainConstants.SELECT_TYPE);
            slCASelectable.add(DomainConstants.SELECT_REVISION);
            slCASelectable.add(DomainConstants.SELECT_NAME);
            MapList mlConnectedCAs = domCOObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, slCASelectable, null, false, true,
                    (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
            StringList slCAOIDList = getStringListFromMaplist(mlConnectedCAs, DomainConstants.SELECT_ID);
            StringList slSystemCreatedFinalImplementedItem = getSystemCreatedImplementedItemIdList(context, slCAOIDList);
            Iterator<Map<?, ?>> itrChangeAction = mlConnectedCAs.iterator();
            HashSet<String> owners = new HashSet<String>();
            while (itrChangeAction.hasNext()) {
                Map<?, ?> mCAInfo = itrChangeAction.next();
                DomainObject domCA = DomainObject.newInstance(context, (String) mCAInfo.get(DomainConstants.SELECT_ID));
                HashMap<String, Object> hsImplementedItems = processImplementedItemsAndGetItemOwner(context, domCA, isDelete, slSystemCreatedFinalImplementedItem);
                HashSet<String> hsImplementedItemOwners = (HashSet<String>) hsImplementedItems.get("hsImplementedItemOwners");
                strCADObjectsTobeDeleted.append((String) hsImplementedItems.get("strCADObjectsTobeDeleted"));
                HashSet<String> hsAffectedItemOwners = disconnectAffectedItemsAndGetItemOwner(context, domCA, strObjectId, isDelete);
                if (hsImplementedItemOwners != null && !hsImplementedItemOwners.isEmpty()) {
                    owners.addAll(hsImplementedItemOwners);
                }

                if (!hsAffectedItemOwners.isEmpty())
                    owners.addAll(hsAffectedItemOwners);
                if (!isDelete) {
                    ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    domCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, TigerConstants.STATE_CHANGEACTION_CANCELLED);
                    domCA.promote(context);
                    ContextUtil.popContext(context);
                }

            }

            StringList onwerList = new StringList();
            onwerList.addAll(owners);
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            domCOObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_BRANCH_TO, TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED);
            domCOObject.promote(context);
            ContextUtil.popContext(context);
            sendNotificationToChangeManagers(context, domCOObject, onwerList);

            // End Transaction
            if (bIsTransactionActive) {
                ContextUtil.commitTransaction(context);
            }
            context.setCustomData("isPromoteFromCOAction", "");
            logger.debug("cancelCO : END");
        } catch (Exception ex) {
            // Abort transaction.
            context.setCustomData("isPromoteFromCOAction", "");
            if (bIsTransactionActive) {
                try {
                    ContextUtil.abortTransaction(context);
                } catch (Exception exFrameWork) {
                    logger.error("cancelCO [ContextUtil.abortTransaction ]:ERROR ", exFrameWork);
                    throw exFrameWork;
                }
            }
            logger.error("cancelCO : ERROR ", ex);
            throw ex;
        }
        return strCADObjectsTobeDeleted.toString();
    }

    /**
     * To diconnect the Affected Item.
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public HashSet<String> disconnectAffectedItemsAndGetItemOwner(Context context, DomainObject domCAObject, String strCOObjectId, boolean isDelete) throws Exception {

        HashSet<String> hsAffectedItemOwners = new HashSet<String>();
        logger.debug("disconnectAffectedItemsAndGetItemOwner : START");
        try {
            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_ID);
            objectSelects.add(DomainConstants.SELECT_CURRENT);
            objectSelects.add(DomainConstants.SELECT_OWNER);

            StringList relSelects = new StringList();
            relSelects.add(DomainRelationship.SELECT_ID);
            MapList mlAIList = domCAObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, DomainConstants.QUERY_WILDCARD, objectSelects, relSelects, false, true,
                    (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
            String strAIName = DomainConstants.EMPTY_STRING;
            String strAIType = DomainConstants.EMPTY_STRING;
            String strAIRevision = DomainConstants.EMPTY_STRING;
            if (!mlAIList.isEmpty()) {

                Iterator<Map<?, ?>> itrAffectedItem = mlAIList.iterator();
                while (itrAffectedItem.hasNext()) {
                    Map<?, ?> mapAffectedItem = (Map<?, ?>) itrAffectedItem.next();
                    String strCurrent = (String) mapAffectedItem.get(DomainObject.SELECT_CURRENT);
                    String strObjectId = (String) mapAffectedItem.get(DomainObject.SELECT_ID);

                    String strRelId = (String) mapAffectedItem.get(DomainRelationship.SELECT_ID);
                    String strOwner = (String) mapAffectedItem.get(DomainConstants.SELECT_OWNER);
                    BusinessObject busAffectedItem = new BusinessObject(strObjectId);
                    DomainObject doAIObj = DomainObject.newInstance(context, busAffectedItem);
                    strAIName = doAIObj.getInfo(context, DomainConstants.SELECT_NAME);

                    strAIType = doAIObj.getInfo(context, DomainConstants.SELECT_TYPE);
                    strAIRevision = doAIObj.getInfo(context, DomainConstants.SELECT_REVISION);

                    // TIGTK-12992 : Set RPE value for CAD Iteration auto generation on Demote
                    PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_DEMOTE_USERNAME", context.getUser());
                    // TIGTK-12992 : END
                    // TIGTK-12992 :START
                    ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

                    busAffectedItem.open(context);
                    if (TigerConstants.STATE_PART_REVIEW.equalsIgnoreCase(strCurrent)) {
                        PropertyUtil.setGlobalRPEValue(context, "PSS_COCancelFromInReview", "True");
                        busAffectedItem.demote(context);
                    }
                    if (TigerConstants.STATE_PART_APPROVED.equals(strCurrent)) {
                        busAffectedItem.demote(context);
                        busAffectedItem.demote(context);
                    }
                    busAffectedItem.close(context);

                    if (!isDelete) {
                        PropertyUtil.setGlobalRPEValue(context, "PSS_COCancel", "True");
                    }

                    DomainRelationship.disconnect(context, strRelId);
                    ContextUtil.popContext(context);

                    StringBuffer sbHistoryAction = new StringBuffer();
                    sbHistoryAction.delete(0, sbHistoryAction.length());
                    sbHistoryAction.append("Disconnect / Demote ");
                    sbHistoryAction.append(strAIName);
                    sbHistoryAction.append(" ");
                    sbHistoryAction.append(strAIType);
                    sbHistoryAction.append(" ");
                    sbHistoryAction.append(strAIRevision);
                    modifyHistory(context, strCOObjectId, sbHistoryAction.toString(), " ");
                    // TIGTK-12992 :END
                    hsAffectedItemOwners.add(strOwner);

                }
            }
            logger.debug("disconnectAffectedItemsAndGetItemOwner : END");
        } catch (RuntimeException e) {
            logger.error("disconnectAffectedItemsAndGetItemOwner : ERROR ", e);
            throw e;

        } catch (Exception ex) {
            logger.error("disconnectAffectedItemsAndGetItemOwner : ERROR", ex);
            throw ex;
        }
        return hsAffectedItemOwners;

    }

    /**
     * To Cancelled the Implemented Item.
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public HashMap<String, Object> processImplementedItemsAndGetItemOwner(Context context, DomainObject domCAObject, boolean isDelete, StringList slSystemCreatedFinalImplementedItem)
            throws Exception {
        HashSet<String> hsImplementedItemOwners = new HashSet<String>();
        HashMap<String, Object> returnMap = new HashMap<String, Object>();
        String strCADObjectsTobeDeleted = "";
        logger.debug("processImplementedItemsAndGetItemOwner : START");
        try {
            StringList slImplementedItemSelectable = new StringList();
            slImplementedItemSelectable.add(DomainConstants.SELECT_ID);

            slImplementedItemSelectable.add(DomainConstants.SELECT_OWNER);

            slImplementedItemSelectable.add(DomainConstants.SELECT_NAME);
            StringList slRelSelect = new StringList();
            slRelSelect.add(DomainRelationship.SELECT_ID);
            MapList mlImplementedItems = domCAObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, DomainConstants.QUERY_WILDCARD, slImplementedItemSelectable, slRelSelect,
                    false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);

            StringList slImplementedItemsgettingCancelled = getStringListFromMaplist(mlImplementedItems, DomainConstants.SELECT_ID);

            Iterator<Map<?, ?>> itrImplItem = mlImplementedItems.iterator();
            if (!mlImplementedItems.isEmpty() && mlImplementedItems.size() > 0) {
                String strCancelPolicy = DomainConstants.EMPTY_STRING;
                while (itrImplItem.hasNext()) {
                    Map<?, ?> strImplementedItemObject = (Map<?, ?>) itrImplItem.next();
                    String strImplementedItemId = (String) strImplementedItemObject.get(DomainConstants.SELECT_ID);
                    DomainObject domImplementedItem = DomainObject.newInstance(context, strImplementedItemId);
                    BusinessObject bo = domImplementedItem.getPreviousRevision(context);
                    String strOwner = (String) strImplementedItemObject.get(DomainConstants.SELECT_OWNER);

                    String strImplementedItemRelId = (String) strImplementedItemObject.get(DomainRelationship.SELECT_ID);

                    if (slSystemCreatedFinalImplementedItem.contains(strImplementedItemId)) {

                        if (domImplementedItem.isKindOf(context, DomainObject.TYPE_PART)) {
                            strCancelPolicy = TigerConstants.POLICY_PSS_CANCELPART;

                        } else {
                            strCancelPolicy = TigerConstants.POLICY_PSS_CANCELCAD;

                        }

                        StringList lstSelectStmts = new StringList(4);
                        lstSelectStmts.addElement(DomainConstants.SELECT_ID);

                        StringList lstRelStmts = new StringList(4);
                        lstRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                        MapList mlPartsMapList = domImplementedItem.getRelatedObjects(context, TigerConstants.RELATIONSHIP_EBOM, DomainObject.TYPE_PART, lstSelectStmts, lstRelStmts, true, false,
                                (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
                        Iterator<Map<?, ?>> itrPartBOM = mlPartsMapList.iterator();
                        if (!mlPartsMapList.isEmpty() && mlPartsMapList.size() > 0) {
                            while (itrPartBOM.hasNext()) {
                                Map<?, ?> mPart = (Map<?, ?>) itrPartBOM.next();
                                String strId = (String) mPart.get(DomainConstants.SELECT_ID);
                                String strRelId = (String) mPart.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                                DomainRelationship.disconnect(context, strRelId);
                                if (bo.exists(context)) {
                                    bo.open(context);
                                    DomainObject dobPreviousRev = DomainObject.newInstance(context, bo);
                                    String strPreviousRevisionState = (String) dobPreviousRev.getInfo(context, DomainConstants.SELECT_CURRENT);
                                    String strPreviousRevisionID = dobPreviousRev.getId(context);

                                    if (strPreviousRevisionState.equals(TigerConstants.STATE_PART_RELEASE) && !slImplementedItemsgettingCancelled.contains(strId)) {
                                        DomainRelationship.connect(context, strId, TigerConstants.RELATIONSHIP_EBOM, strPreviousRevisionID, true);

                                    }

                                }

                            }
                        }

                        if (isDelete) {

                            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

                            DomainRelationship.disconnect(context, (String) strImplementedItemObject.get(DomainRelationship.SELECT_ID));

                            if (domImplementedItem.isKindOf(context, DomainObject.TYPE_PART)) {
                                domImplementedItem.deleteObject(context);
                            } else {
                                strCADObjectsTobeDeleted += strImplementedItemId + ",";
                            }
                            ContextUtil.popContext(context);

                        } else {
                            domImplementedItem.open(context);
                            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            domImplementedItem.setPolicy(context, strCancelPolicy);
                            ContextUtil.popContext(context);
                            domImplementedItem.close(context);

                        }
                        hsImplementedItemOwners.add(strOwner);

                        returnMap.put("hsImplementedItemOwners", hsImplementedItemOwners);
                        returnMap.put("strCADObjectsTobeDeleted", strCADObjectsTobeDeleted);
                    } else {
                        ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        DomainRelationship.disconnect(context, strImplementedItemRelId);
                        ContextUtil.popContext(context);

                    }
                }
            }
            logger.debug("processImplementedItemsAndGetItemOwner : END");
        } catch (Exception ex) {
            logger.error("processImplementedItemsAndGetItemOwner : ERROR", ex);
            throw ex;
        }
        return returnMap;
    }

    /**
     * This method is used to modify the History of the object.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param strObjectId
     *            - The object id.
     * @return strAction - The action to be added in the history.
     * @return strComment - The comment to be added in the history
     * @throws Exception
     *             if the operation fails
     */
    protected void modifyHistory(Context context, String strObjectId, String strAction, String strComment) throws Exception {
        logger.debug("modifyHistory : START");
        StringBuffer sbMqlCommand = new StringBuffer("modify bus ");
        sbMqlCommand.append(strObjectId);
        sbMqlCommand.append(" add history \"");
        sbMqlCommand.append("disconnect ");
        sbMqlCommand.append("\" comment \"");
        sbMqlCommand.append(strAction);

        MqlUtil.mqlCommand(context, sbMqlCommand.toString(), true);
        logger.debug("modifyHistory : END");
    }

    // TIGTK-6843:Phase-2.0:PKH:End

    /**
     * This method is used for send notification to ChangeManager of related CR and owner of Affected Items when user performing CO Cancellation
     * @author AshaG Modified By : PCM : TIGTK-10003 : 26/09/2017 : AB
     * @param context
     * @param domCOObject
     * @param hmToListCancelledCO
     * @throws Exception
     */
    public void sendNotificationToChangeManagers(Context context, DomainObject domCOObject, StringList hmToListCancelledCO) throws Exception {
        try {
            logger.debug("sendNotificationToChangeManagers : START");
            String strContextUser = context.getUser();
            StringList slSelectable = new StringList(DomainConstants.SELECT_ID);
            slSelectable.add(DomainConstants.SELECT_OWNER);
            slSelectable.add(DomainConstants.SELECT_NAME);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            Pattern relPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_ORDER);

            // Get Related all CR of ChangeOrder
            MapList mlRelatedCRList = domCOObject.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), slSelectable, null, true, false, (short) 1, null, null, 0);

            Iterator itrCRItem = mlRelatedCRList.iterator();
            StringList hmToListLastCO = new StringList();
            StringList slObjectSelectsCR = new StringList("from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.name");
            slObjectSelectsCR.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            if (!mlRelatedCRList.isEmpty() && mlRelatedCRList.size() > 0) {
                while (itrCRItem.hasNext()) {
                    Map mapCRObject = (Map) itrCRItem.next();
                    String strCRID = (String) mapCRObject.get(DomainConstants.SELECT_ID);
                    DomainObject domCRObject = DomainObject.newInstance(context, strCRID);

                    // get the Change Manager of Change Request
                    Map mapCRInfo = domCRObject.getInfo(context, slObjectSelectsCR);
                    String strCRChangeManager = (String) mapCRInfo.get("from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.name");

                    if (UIUtil.isNotNullAndNotEmpty(strCRChangeManager)) {
                        if (!hmToListCancelledCO.contains(strCRChangeManager)) {
                            hmToListCancelledCO.add(strCRChangeManager);
                        }
                    }

                    // Check that current CO is last CO of that particular CR or not
                    boolean isLast = isLastCO(context, domCOObject, domCRObject);

                    // If this is Last CO of CR then get Lead Chaneg Manager of Program-Project
                    if (isLast) {
                        // Get the Lead Change Manager of Program_project which is connected to CR
                        String strCRsProgramProjectID = (String) mapCRInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

                        Map mapProgramProject = new HashMap();
                        mapProgramProject.put(DomainConstants.SELECT_ID, strCRsProgramProjectID);

                        MapList mlProgramProject = new MapList();
                        mlProgramProject.add(mapProgramProject);

                        Map mapProgramProjectList = new HashMap();
                        mapProgramProjectList.put("objectList", mlProgramProject);

                        StringList slLeadChangeManager = this.getLeadChangeManager(context, JPO.packArgs(mapProgramProjectList));
                        if (!slLeadChangeManager.isEmpty()) {
                            String strLeadCMOfProgramProject = (String) slLeadChangeManager.get(0);
                            if (!hmToListLastCO.contains(strLeadCMOfProgramProject) && UIUtil.isNotNullAndNotEmpty(strLeadCMOfProgramProject)) {
                                hmToListLastCO.add(strLeadCMOfProgramProject);
                            }
                        }
                    }
                }
            }

            // Send Mail notification to change Manger of Related CR and Part and CAD Owners of the cancelled and disconnected items
            if (!hmToListCancelledCO.isEmpty() && hmToListCancelledCO.size() > 0) {

                Map<String, Object> payload = new HashMap<String, Object>();
                if (hmToListCancelledCO.contains(strContextUser)) {
                    hmToListCancelledCO.remove(strContextUser);
                }
                payload.put("toList", hmToListCancelledCO);

                Map<String, Object> programMap = new HashMap<String, Object>();
                programMap.put("objectId", domCOObject.getId());
                programMap.put("notificationName", "PSS_COCancelNotification");
                programMap.put("payload", payload);

                // Send CO Cancel Mail Notification
                PropertyUtil.setGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME, context.getUser());
                JPO.invoke(context, "emxNotificationUtil", null, "objectNotificationFromMap", JPO.packArgs(programMap));
                PropertyUtil.setGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME, "");

            }

            // Send Mail notification to lead change Manger of Program-Project which is connected to CR
            if (!hmToListLastCO.isEmpty() && hmToListLastCO.size() > 0) {

                Map<String, Object> payload = new HashMap<String, Object>();
                Map<String, Object> programMap = new HashMap<String, Object>();
                if (hmToListLastCO.contains(strContextUser)) {
                    hmToListLastCO.remove(strContextUser);
                }
                payload.put("toList", hmToListLastCO);

                programMap.put("objectId", domCOObject.getId());
                programMap.put("notificationName", "PSS_LastCOCancelNotification");
                programMap.put("payload", payload);

                JPO.invoke(context, "emxNotificationUtil", null, "objectNotificationFromMap", JPO.packArgs(programMap));
            }
        } catch (Exception ex) {
            logger.error("sendNotificationToChangeManagers : ERROR", ex);
            throw ex;
        }
        logger.debug("sendNotificationToChangeManagers : END");
    }

    /**
     * This Method is used for Check whether current CO is last CO or not, it is used in mail notification method (sendNotificationToChangeManagers) when user prform CO Cancellation operation
     * @author AshaG Modified by : PCM : TIGTK-10003 : 26/09/2017 : AB
     * @param context
     * @param domCOObject
     * @param domCRObject
     * @return
     */
    private boolean isLastCO(Context context, DomainObject domCOObject, DomainObject domCRObject) {
        logger.debug("pss.ecm.enoECMChange:isLastCO:START");
        StringList objectSelect = new StringList();
        boolean isLast = true;
        objectSelect.add(DomainConstants.SELECT_ID);
        objectSelect.add(DomainConstants.SELECT_CURRENT);
        String strWhere = DomainConstants.EMPTY_STRING;
        try {
            MapList mlConnectedCOs = domCRObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEORDER, objectSelect, null, false, true, (short) 1,
                    strWhere, null, 0);
            if (!(mlConnectedCOs.isEmpty())) {
                Iterator itrCO = mlConnectedCOs.iterator();
                while (itrCO.hasNext()) {
                    Map mapCO = (Map) itrCO.next();
                    String current = (String) mapCO.get(DomainObject.SELECT_CURRENT);
                    String objectId = (String) mapCO.get(DomainObject.SELECT_ID);
                    String strCurrentCOId = (String) domCOObject.getId(context);
                    if (!(TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED.equalsIgnoreCase(current)) && !(TigerConstants.STATE_CHANGEORDER_IMPLEMENTED.equalsIgnoreCase(current))
                            && !strCurrentCOId.equalsIgnoreCase(objectId)) {
                        return false;
                    }
                }
            }
        } catch (FrameworkException e) {
            logger.error("pss.ecm.enoECMChange:isLastCO:ERROR", e);

        }
        logger.debug("pss.ecm.enoECMChange:isLastCO:END");
        return isLast;
    }

    public StringList getCOCancelNotifiers(Context context, String args[]) {
        logger.debug("pss.ecm.enoECMChange:getCOCancelNotifiers:START");
        StringList toList = new StringList();
        HashMap programMap;
        try {
            programMap = JPO.unpackArgs(args);

            Map<String, Object> payload = (HashMap) programMap.get("payload");
            toList = (StringList) payload.get("toList");
        } catch (Exception e) {
            logger.error("pss.ecm.enoECMChange:getCOCancelNotifiers:ERROR", e);

        }
        logger.debug("pss.ecm.enoECMChange:getCOCancelNotifiers:END");
        return toList;

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author PTE : SteepGraph
     */
    @SuppressWarnings("rawtypes")
    public StringList getToListForCOCancellationRequestNotifications(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.enoECMChange : getToListForCOCancellationRequestNotifications : START");
        StringList toList = new StringList();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            Map<String, Object> payload = (HashMap) programMap.get("payload");
            toList = (StringList) payload.get("toList");
            logger.debug("pss.ecm.enoECMChange : getToListForCOCancellationRequestNotifications : END");
        } catch (Exception ex) {
            logger.error("pss.ecm.enoECMChange : getToListForCOCancellationRequestNotifications : ERROR", ex);
        }
        return toList;
    }

    // TIGTK-6906 : TS : 30/08/2017 : START

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkCADDemote(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.enoECMChange : checkCADDemote : START");
        String CAD = "CAD";
        int intReturn = 0;
        try {
            String strCADId = args[0];
            DomainObject domCAD = DomainObject.newInstance(context, strCADId);
            String sObjectWhereClause = "attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]==" + CAD + " && current =='" + TigerConstants.STATE_DEVELOPMENTPART_COMPLETE + "'";
            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            MapList mlConnectedCADCAs = domCAD.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, TigerConstants.TYPE_CHANGEACTION, slObjectSle, slRelSle, true, false,
                    (short) 1, sObjectWhereClause, null, (short) 0);
            if (mlConnectedCADCAs.size() != 0) {
                Locale strLocale = new Locale(context.getSession().getLanguage());
                String strCADDemoteNoticeMsg = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", strLocale, "PSS_EnterpriseChangeMgt.notice.CADDemoteNotice");
                MqlUtil.mqlCommand(context, "notice $1", strCADDemoteNoticeMsg);
                intReturn = 1;
            } else {
                intReturn = 0;
            }
            logger.debug("pss.ecm.enoECMChange : checkCADDemote : END");
        } catch (Exception ex) {
            logger.error("pss.ecm.enoECMChange : checkCADDemote : ERROR", ex);
            throw ex;
        }
        return intReturn;
    }

    /**
     * @param context
     * @param domCA
     * @return int
     * @throws Exception
     */
    public int checkCARelaunch(Context context, DomainObject domCA) throws Exception {
        logger.debug("pss.ecm.enoECMChange : checkCARelaunch : START");
        try {
            StringList slObjectSle = new StringList();
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            // TIGTK-16777:22-08-2018:START
            StringBuffer sbObjectWhere = new StringBuffer();
            sbObjectWhere.append("(current == '");
            sbObjectWhere.append(TigerConstants.STATE_PSS_CHANGEORDER_COMPLETE);
            sbObjectWhere.append("' || current == '");
            sbObjectWhere.append(TigerConstants.STATE_CHANGEORDER_IMPLEMENTED);
            sbObjectWhere.append("')");
            // TIGTK-16777:22-08-2018:END

            MapList mlConnectedCO = domCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, TigerConstants.TYPE_PSS_CHANGEORDER, slObjectSle, slRelSle, true, false, (short) 1,
                    sbObjectWhere.toString(), null, (short) 0);

            // TIGTK-16777:22-08-2018:START
            boolean bConnectedCOComplete = false;
            // boolean bPartCAIsComplete = false;
            boolean allowRelaunchCA = false;
            int hasPartConnectedflag = 0;

            if (!mlConnectedCO.isEmpty()) {
                bConnectedCOComplete = true;
            } else {
                // TIGTK-16777 : 10-09-2018 : START
                String strChangeOrderId = domCA.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "]." + DomainConstants.SELECT_FROM_ID);
                DomainObject domChangeOrder = DomainObject.newInstance(context, strChangeOrderId);
                slObjectSle.addElement(DomainObject.SELECT_ID);
                slObjectSle.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "].value");
                sbObjectWhere = new StringBuffer();
                sbObjectWhere.append("attribute[");
                sbObjectWhere.append(TigerConstants.ATTRIBUTE_PSS_CATYPE);
                sbObjectWhere.append("].value != '");
                sbObjectWhere.append(TigerConstants.ATTRIBUTE_PSS_CATYPE_CAD);
                sbObjectWhere.append("'");

                MapList mlAllCAofCO = domChangeOrder.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, slObjectSle, slRelSle, false, // to
                        // direction
                        true, // from direction
                        (short) 1, sbObjectWhere.toString(), // object where
                        null, (short) 0);

                StringList slAllCAofCO = getStringListFromMaplist(mlAllCAofCO, DomainConstants.SELECT_ID);
                // TIGTK-16777 : 10-09-2018 : END
                StringList slObjectSelects = new StringList();
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement(DomainConstants.SELECT_POLICY);

                Pattern cadRelPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);

                sbObjectWhere = new StringBuffer();
                sbObjectWhere.append("(policy == '");
                sbObjectWhere.append(TigerConstants.POLICY_PSS_CADOBJECT);
                sbObjectWhere.append("' || policy == '");
                sbObjectWhere.append(TigerConstants.POLICY_PSS_Legacy_CAD);
                sbObjectWhere.append("')");

                // TIGKT-16777 : 30-08-2018 : START
                boolean isCAHasImplementedItems = domCA.hasRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, true);

                // If CA have Implemented Item then traverse with Implemented Item relationship
                if (isCAHasImplementedItems) {
                    cadRelPattern = new Pattern(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);
                }
                // TIGKT-16777 : 30-08-2018 : END
                MapList mlCACAD = domCA.getRelatedObjects(context, cadRelPattern.getPattern(), // rel pattern
                        "*", // type pattern
                        slObjectSelects, // object select
                        new StringList(DomainRelationship.SELECT_ID), // rel select
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        sbObjectWhere.toString(), // object where clause
                        null, 0); // relationship where clause

                Pattern typePattern = new Pattern(DomainConstants.TYPE_PART);
                typePattern.addPattern(ChangeConstants.TYPE_CHANGE_ACTION);
                Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);

                Pattern typePostPattern = new Pattern(ChangeConstants.TYPE_CHANGE_ACTION);

                slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);

                if (mlCACAD != null && !mlCACAD.isEmpty()) {
                    Iterator itrCAAI = mlCACAD.iterator();
                    while (itrCAAI.hasNext()) {
                        Map mpCAIAMap = (Map) itrCAAI.next();

                        String strCADObjectId = (String) mpCAIAMap.get(DomainConstants.SELECT_ID);
                        DomainObject domCAD = DomainObject.newInstance(context, strCADObjectId);

                        // TIGKT-16777 : 30-08-2018 : START
                        // Check CAD is connected to any CA with Implemented Item relationship
                        boolean isCADConnectedAsImplementedItem = domCAD.hasRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, false);

                        // If CAD is connected as Implemented Item to CA then traverse with Implemented Item relationship
                        if (isCADConnectedAsImplementedItem) {
                            relPattern.addPattern(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);
                            slObjectSelects.addElement("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "]." + DomainConstants.SELECT_FROM_ID);
                        } else {
                            // Add Affected Item relationship for traversing
                            relPattern.addPattern(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
                            slObjectSelects.addElement("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]." + DomainConstants.SELECT_FROM_ID);
                        }
                        // TIGKT-16777 : 30-08-2018 : END
                        slObjectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");

                        MapList mlConnectedPartsCA = domCAD.getRelatedObjects(context, relPattern.getPattern(), // relationship
                                typePattern.getPattern(), // types to fetch from other end
                                true, // to direction
                                false, // from direction
                                (short) 0, // recursion level
                                slObjectSelects, // object selects
                                new StringList(DomainRelationship.SELECT_ID), // relationship selects
                                DomainConstants.EMPTY_STRING, // object where
                                DomainConstants.EMPTY_STRING, // relationship where
                                (short) 0, // limit
                                DomainConstants.EMPTY_STRING, // post rel pattern
                                typePostPattern.getPattern(), // post type pattern
                                null);
                        if (mlConnectedPartsCA != null && !mlConnectedPartsCA.isEmpty()) {

                            Iterator itrPartCA = mlConnectedPartsCA.iterator();
                            while (itrPartCA.hasNext()) {
                                Map mpPartCA = (Map) itrPartCA.next();
                                String strCAId = (String) mpPartCA.get(DomainConstants.SELECT_ID);
                                String strCAType = (String) mpPartCA.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");
                                // TIGTK-16777 : 10-09-2018 : START
                                if (slAllCAofCO.contains(strCAId)) {
                                    // TIGTK-16777 : 10-09-2018 : END
                                    if (UIUtil.isNotNullAndNotEmpty(strCAType) && (!strCAType.equalsIgnoreCase(TigerConstants.ATTRIBUTE_PSS_CATYPE_CAD))) {
                                        String strPartCAState = (String) mpPartCA.get(DomainConstants.SELECT_CURRENT);
                                        // TIGTK-16777 : 10-09-2018 : START
                                        hasPartConnectedflag = 1;
                                        // If part CA is in IN WORK state then allow relaunch CA
                                        if (strPartCAState.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_INWORK)) {
                                            allowRelaunchCA = true;
                                            break;
                                        }
                                    }
                                } // IF CA is current CO's CA end
                                  // TIGTK-16777 : 10-09-2018 : END
                            } // 2nd while end
                        }
                        // TIGTK-16777 : 10-09-2018 : START
                        if (allowRelaunchCA) {
                            break;
                        }
                    } // 1st while break

                    if (hasPartConnectedflag == 0) {
                        allowRelaunchCA = true;
                    }
                    // TIGTK-16777 : 10-09-2018 : END
                }
            }

            if (bConnectedCOComplete) {
                // 1 means CAD CA's CO is complete
                return 1;
            } else if (!allowRelaunchCA) {
                // 2 means CA CAD have some CAD's whose connected Part are linked to Complete CA
                return 2;
            }
            // TIGTK-16777:22-08-2018:END
            logger.debug("pss.ecm.enoECMChange : checkCARelaunch : END");
        } catch (Exception ex) {
            logger.error("pss.ecm.enoECMChange : checkCARelaunch : ERROR", ex);
            throw ex;
        }
        // 0 means allow CA Relaunch
        return 0;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int doCARelaunch(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.enoECMChange : doCARelaunch : START");
        boolean isContextPushed = false;
        int intCARelaunchResult = 0;
        try {
            String strCAId = args[0];
            DomainObject domCA = DomainObject.newInstance(context, strCAId);
            // TIGTK-16777:22-08-2018:START
            intCARelaunchResult = checkCARelaunch(context, domCA);
            if (intCARelaunchResult == 0) {
                // Allow relauch functionality
                // TIGTK-16777:22-08-2018:END
                ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isContextPushed = true;
                domCA.setState(context, TigerConstants.STATE_CHANGEACTION_INWORK);
            }
        } catch (Exception ex) {
            logger.error("pss.ecm.enoECMChange : doCARelaunch : ERROR", ex);
            throw ex;
        } finally {
            if (isContextPushed)
                ContextUtil.popContext(context);
        }
        logger.debug("pss.ecm.enoECMChange : doCARelaunch : END");
        return intCARelaunchResult;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean showCARelaunch(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.enoECMChange : showRelaunchCA : START");
        String CAD = "CAD";
        boolean isReturn = false;
        try {

            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String strCAID = (String) paramMap.get("objectId");
            DomainObject domCA = DomainObject.newInstance(context, strCAID);
            StringList slSelectable = new StringList();

            slSelectable.add(DomainConstants.SELECT_CURRENT);
            slSelectable.add("from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.name");
            slSelectable.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.owner");

            Map<?, ?> mapUserInfo = domCA.getInfo(context, slSelectable);

            String strCAAssignee = (String) mapUserInfo.get("from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.name");
            String strCAType = domCA.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE);
            String strCOOwner = (String) mapUserInfo.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.owner");
            StringList slImplementedItemAssignee = (StringList) domCA.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.owner");
            StringList slAffectedItemAssignee = (StringList) domCA.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.owner");
            String strCurrentUser = context.getUser();
            if ((strCAType.equals("CAD")) && (TigerConstants.STATE_CHANGEACTION_COMPLETE.equals((String) mapUserInfo.get(DomainConstants.SELECT_CURRENT)) && (strCurrentUser.equals(strCAAssignee)
                    || strCurrentUser.equals(strCOOwner) || slImplementedItemAssignee.contains(strCurrentUser) || slAffectedItemAssignee.contains(strCurrentUser)))) {
                isReturn = true;
            }
            logger.debug("pss.ecm.enoECMChange : showRelaunchCA : END");
        } catch (Exception ex) {
            logger.error("pss.ecm.enoECMChange : showRelaunchCA : ERROR", ex);
            throw ex;
        }

        return isReturn;
    }
    // TIGTK-6906 : TS : 30/08/2017 : END

    // TIGTK-6874 : Phase-2.0 : PKH : START
    /**
     * check Mandatory PreRequisite CRs
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public int checkMandatoryPreRequisiteCRs(Context context, String[] args) throws Exception {
        logger.debug("checkMandatoryPreRequisiteCRs : START");
        // StringList lstCurrent = new StringList();
        try {
            String strCRId = args[0];
            DomainObject domObjCR = DomainObject.newInstance(context, strCRId);

            // TIGTK-17608:30-10-2018:Start
            String strObjectWhereClause = "current ==" + TigerConstants.STATE_PSS_CR_CREATE + "|| current =='" + TigerConstants.STATE_SUBMIT_CR + "'|| current =='" + TigerConstants.STATE_INREVIEW_CR
                    + "'|| current == const\"" + TigerConstants.STATE_EVALUATE + "\"";
            // TIGTK-17608:30-10-2018:End

            String strRelWhereClause = "attribute[" + TigerConstants.ATTRIBUTE_PSS_MANDATORYCR + "] == 'Mandatory'";

            StringList slObjectSelect = new StringList(1);
            slObjectSelect.addElement(DomainConstants.SELECT_ID);
            slObjectSelect.addElement(DomainConstants.SELECT_CURRENT);

            MapList mlPreRequisiteCRs = domObjCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSelect,
                    DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, strObjectWhereClause, strRelWhereClause, 0);

            // TIGTK-17608:30-10-2018:Start
            /*
             * Iterator<Map<?, ?>> itrCR = mlPreRequisiteCRs.iterator(); while (itrCR.hasNext()) { Map tempMap = (Map) itrCR.next(); String strCurrent = (String)
             * tempMap.get(DomainObject.SELECT_CURRENT);
             * 
             * if (!lstCurrent.contains(strCurrent)) lstCurrent.add(strCurrent); }
             * 
             * boolean bShowRejectionAlert = false; if (lstCurrent.contains("Rejected")) {
             * 
             * bShowRejectionAlert = true; }
             */
            // TIGTK-17608:30-10-2018:End

            if (null != mlPreRequisiteCRs && mlPreRequisiteCRs.size() > 0) {

                String strNoticeMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.notice.OneOfTheMandatoryPreRequisiteCRsIsNotInOrBeyondInProcessState");

                MqlUtil.mqlCommand(context, "notice $1", strNoticeMessage);
                return 1;
            }

            // TIGTK-17608:30-10-2018:Start
            /*
             * if (lstCurrent.size() > 0 && bShowRejectionAlert) {
             * 
             * String strNoticeMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
             * "PSS_EnterpriseChangeMgt.notice.OneOfTheMandatoryPreRequisiteCRsIsInRejectedState");
             * 
             * MqlUtil.mqlCommand(context, "notice $1", strNoticeMessage); return 1; }
             */
            // TIGTK-17608:30-10-2018:End

            logger.debug("checkMandatoryPreRequisiteCRs : END");
        } catch (Exception e) {
            logger.error("checkMandatoryPreRequisiteCRs : ERROR ", e);

            throw e;
        }
        return 0;

    }

    // TIGTK-6874 : Phase-2.0 : PKH : END
    // TIGTK-6888 : Phase-2.0 : PKH : START
    /**
     * get ToList for CR Rejection Notification
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public StringList getMasterCRRejectNotificationRecipients(Context context, String args[]) throws Exception {
        logger.debug("getMasterCRRejectNotificationRecipients : START");
        try {

            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);

            String strCRObjectId = (String) programMap.get(DomainConstants.SELECT_ID);
            String strContextUser = context.getUser();
            StringList slToList = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(strCRObjectId)) {

                DomainObject domMasterCR = DomainObject.newInstance(context, strCRObjectId);

                String strRelWhereClause = "attribute[" + TigerConstants.ATTRIBUTE_PSS_MANDATORYCR + "] == 'Mandatory'";

                StringList slObjectSelect = new StringList(1);
                slObjectSelect.addElement(DomainConstants.SELECT_ID);
                MapList mlPreRequisiteCRs = domMasterCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSelect,
                        DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, DomainConstants.EMPTY_STRING, strRelWhereClause, 0);

                Iterator<Map<?, ?>> itrCRID = mlPreRequisiteCRs.iterator();
                while (itrCRID.hasNext()) {
                    Map<?, ?> mIds = (Map<?, ?>) itrCRID.next();
                    String strCRID = (String) mIds.get("id");
                    DomainObject domObjCR = DomainObject.newInstance(context, strCRID);
                    StringList slRolesList = new StringList();
                    String strChangeManager = domObjCR.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].to.name");
                    slToList.addElement(strChangeManager);
                    slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                    pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);

                    slToList = commonObj.getProgramProjectTeamMembersForChange(context, strCRID, slRolesList, true);

                    slToList.addElement(strChangeManager);

                }
                while (slToList.contains(strContextUser)) {
                    slToList.remove(strContextUser);
                }

            }
            logger.debug("getMasterCRRejectNotificationRecipients : END");
            return slToList;

        } catch (Exception e) {
            logger.error("getMasterCRRejectNotificationRecipients : ERROR ", e);

            throw e;
        }
    }

    /**
     * get ToList for CR Rejection Notification
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public StringList getSlaveCRRejectNotificationRecipients(Context context, String args[]) throws Exception {
        logger.debug("getSlaveCRRejectNotificationRecipients : START");
        try {

            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strCRObjectId = (String) programMap.get(DomainConstants.SELECT_ID);
            String strContextUser = context.getUser();
            StringList slToList = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(strCRObjectId)) {

                DomainObject domMasterCR = DomainObject.newInstance(context, strCRObjectId);

                String strRelWhereClause = "attribute[" + TigerConstants.ATTRIBUTE_PSS_MANDATORYCR + "] == 'Mandatory'";

                StringList slObjectSelect = new StringList(1);
                slObjectSelect.addElement(DomainConstants.SELECT_ID);
                MapList mlPreRequisiteCRs = domMasterCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSelect,
                        DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, DomainConstants.EMPTY_STRING, strRelWhereClause, 0);

                Iterator<Map<?, ?>> itrCRID = mlPreRequisiteCRs.iterator();
                while (itrCRID.hasNext()) {
                    Map<?, ?> mIds = (Map<?, ?>) itrCRID.next();
                    String strCRID = (String) mIds.get("id");
                    DomainObject domObjCR = DomainObject.newInstance(context, strCRID);
                    StringList slRolesList = new StringList();
                    String strChangeManager = domObjCR.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].to.name");
                    slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                    // TIGTK-10757 -- START
                    slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                    // TIGTK-10757 -- END
                    pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                    slToList = commonObj.getProgramProjectTeamMembersForChange(context, strCRID, slRolesList, true);
                    slToList.addElement(strChangeManager);

                }
                while (slToList.contains(strContextUser)) {
                    slToList.remove(strContextUser);
                }

            }
            logger.debug("getSlaveCRRejectNotificationRecipients : END");
            return slToList;

        } catch (Exception e) {
            logger.error("getSlaveCRRejectNotificationRecipients : ERROR ", e);

            throw e;
        }
    }

    // TIGTK-6888 : Phase-2.0 : PKH : END
    // TIGTK-6859 : Phase-2.0 : PKH :START
    /**
     * Check visibility of Abstain command
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public boolean showAbstainCR(Context context, String[] args) throws Exception {
        logger.debug("showAbstainCR : START");
        boolean returnVal = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strCRID = (String) programMap.get("objectId");
            DomainObject domObjCR = DomainObject.newInstance(context, strCRID);
            String strCurrentUser = context.getUser();
            String strCurrent = domObjCR.getInfo(context, DomainConstants.SELECT_CURRENT);
            String strSlaveCR = domObjCR.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR + "].to.id");
            String strMasterCR = domObjCR.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR + "].from.id");
            String strChangeManager = domObjCR.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].to.name");

            if ((TigerConstants.STATE_PSS_CR_CREATE.equals(strCurrent) || TigerConstants.STATE_SUBMIT_CR.equals(strCurrent))
                    && (UIUtil.isNotNullAndNotEmpty(strMasterCR) && UIUtil.isNullOrEmpty(strSlaveCR))) {
                StringList slRole = new StringList();
                slRole.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                pss.ecm.ui.CommonUtil_mxJPO objCommoUtil = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                StringList sProjectManager = objCommoUtil.getProgramProjectTeamMembersForChange(context, strCRID, slRole, true);
                if (strCurrentUser.equals(strChangeManager) || strCurrentUser.equals(sProjectManager.get(0))) {
                    returnVal = true;
                }
            }
            logger.debug("showAbstainCR : END");
        } catch (Exception ex) {
            logger.error("showAbstainCR : ERROR ", ex);
            throw ex;
        }
        return returnVal;
    }

    /**
     * get ToList for Abstain Impact Analysis
     * @param context
     * @param args
     * @return Map
     * @throws Exception
     */
    public StringList getMasterCRChangeManager(Context context, String[] args) throws Exception {
        logger.debug("getMasterCRChangeManager : START");
        StringList slRole = new StringList();
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strCRObjId = (String) programMap.get(DomainConstants.SELECT_ID);
            String strContextUser = context.getUser();
            DomainObject domObjCR = DomainObject.newInstance(context, strCRObjId);
            // TIGTK-11058 , TIGTK-10793 : START : notification to Lead CM of Master CR to which Prerequisite CR is attached
            String strLeadCRid = domObjCR.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_PREREQUISITECR + "].from.id");
            StringList slRolesList = new StringList();
            slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
            StringList slToList = commonObj.getProgramProjectTeamMembersForChange(context, strLeadCRid, slRolesList, true);

            for (int i = 0; i < slToList.size(); i++) {
                slRole.add(slToList.get(i));
            }
            // TIGTK-11058 , TIGTK-10793 : END
            while (slRole.contains(strContextUser)) {
                slRole.remove(strContextUser);
            }
            logger.debug("getMasterCRChangeManager : END");
        } catch (Exception e) {
            logger.error("getMasterCRChangeManager : ERROR ", e);
            throw e;
        }
        return slRole;
    }

    // TIGTK-6859 : Phase-2.0 : PKH :END

    // TIGTK-9020:Rutuja Ekatpure:5/9/2017:Start
    /***
     * this method used on Notification object of Impact analysis for Body HTML
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getPerformImpactAnalysisNotificationBodyHTML(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.enoECMChange : getPerformImpactAnalysisNotificationBodyHTML : Start");
        StringBuffer sbMailBody = new StringBuffer();
        try {
            String strIABodyHTML = getCRNotificationBodyHTML(context, args);

            sbMailBody.append(strIABodyHTML);
        } catch (Exception e) {
            logger.error("Error in pss.ecm.enoECMChange : getPerformImpactAnalysisNotificationBodyHTML : " + e.getMessage());
            throw e;
        }
        logger.debug("pss.ecm.enoECMChange : getPerformImpactAnalysisNotificationBodyHTML : End");
        return sbMailBody.toString();
    }

    /***
     * this method used on Notification object of Impact analysis for Body HTML
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getImpactAnalysisNotificationBodyHTML(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.enoECMChange_mxJPO : getImpactAnalysisNotificationBodyHTML : Start");
        String messageHTML = DomainConstants.EMPTY_STRING;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strObjId = (String) programMap.get("id");
            String strBaseURL = (String) programMap.get("baseURL");
            String notificationObjName = (String) programMap.get("notificationName");
            StringList busSelects = new StringList("attribute[" + TigerConstants.ATTRIBUTE_SUBJECT_TEXT + "]");
            boolean isImpact = false;
            String strSectionSub = DomainConstants.EMPTY_STRING;
            if ("PSSRoleAssessmentReassignmentNotification".equalsIgnoreCase(notificationObjName)) {
                DomainObject domRAObj = DomainObject.newInstance(context, strObjId);

                strObjId = domRAObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.id");
            }
            Map mapIA = (Map) getImpactAnalysisInformation(context, strObjId);
            String strSubjectKey = DomainConstants.EMPTY_STRING;
            MapList mpNotificationList = DomainObject.findObjects(context, "Notification", notificationObjName, "*", "*", TigerConstants.VAULT_ESERVICEADMINISTRATION, "", null, true, busSelects,
                    (short) 0);

            if (mpNotificationList != null && (!mpNotificationList.isEmpty())) {
                strSubjectKey = (String) ((Map) mpNotificationList.get(0)).get("attribute[" + TigerConstants.ATTRIBUTE_SUBJECT_TEXT + "]");
            }

            if (UIUtil.isNotNullAndNotEmpty(strSubjectKey)) {
                strSectionSub = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strSubjectKey);
            }
            mapIA.put("SectionSubject", strSectionSub);
            if ("PSSPerformImpactAnalysisNotification".equalsIgnoreCase(notificationObjName)) {
                isImpact = true;
            }
            MapList mlInfoList = transformIAMapToHTMLList(context, mapIA, strBaseURL, isImpact);
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            String strStyleSheet = commonObj.getFormStyleSheet(context, "PSS_NotificationStyleSheet");
            messageHTML = commonObj.getHTMLTwoColumnTable(context, mlInfoList, strStyleSheet);

        } catch (Exception ex) {
            logger.error("Error in pss.ecm.enoECMChange_mxJPO : getImpactAnalysisNotificationBodyHTML: ", ex);
        }
        logger.debug("pss.ecm.enoECMChange_mxJPO : getImpactAnalysisNotificationBodyHTML : End");
        return messageHTML;
    }

    /***
     * this method used for getting impact analysis information for building notification body
     * @param context
     * @param strIAId
     * @return Map
     * @throws Exception
     */
    public Map getImpactAnalysisInformation(Context context, String strIAId) throws Exception {
        logger.debug("pss.ecm.enoECMChange_mxJPO : getImpactAnalysisInformation : Start");
        Map mapIA = new HashMap();
        try {
            if (UIUtil.isNotNullAndNotEmpty(strIAId)) {
                DomainObject domIAObj = DomainObject.newInstance(context, strIAId);
                StringList slObjectSelects = new StringList(24);
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                slObjectSelects.addElement(DomainConstants.SELECT_NAME);
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement(DomainConstants.SELECT_CURRENT);
                slObjectSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTITLE + "]");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");

                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE + "]");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACKCOMMENT + "]");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.id");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.name");
                slObjectSelects.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.id");

                mapIA = domIAObj.getInfo(context, slObjectSelects);
                String personEmail = DomainConstants.EMPTY_STRING;
                if (mapIA != null && mapIA.size() > 0 && mapIA.containsKey("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]")) {
                    String strChangeInitiator = (String) mapIA.get("to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                    if (UIUtil.isNotNullAndNotEmpty(strChangeInitiator)) {
                        personEmail = new StringBuilder("<a href=\"mailto:").append(PersonUtil.getEmail(context, strChangeInitiator)).append("\" target=\"_top\">")
                                .append(PersonUtil.getEmail(context, strChangeInitiator)).append("</a>").toString();
                    }
                } else {
                    mapIA = new HashMap();
                }
                mapIA.put("changeInitiatorEmail", personEmail);
            }
        } catch (RuntimeException rex) {
            logger.error("Error in pss.ecm.enoECMChange_mxJPO : getChangeRequestInformation: ", rex);
            throw rex;
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.enoECMChange_mxJPO : getChangeRequestInformation: ", ex);
        }
        logger.debug("pss.ecm.enoECMChange_mxJPO : getImpactAnalysisInformation : End");
        return mapIA;
    }

    /****
     * this method used for transforming Map to HTML List of Notification body
     * @param context
     * @param objectMap
     * @param strBaseURL
     * @param isImpact
     *            : for adding Requested_Assessment_End_Date
     * @return MapList
     * @throws Exception
     */
    public MapList transformIAMapToHTMLList(Context context, Map objectMap, String strBaseURL, boolean isImpact) throws Exception {
        logger.debug("pss.ecm.enoECMChange_mxJPO : transformIAMapToHTMLList : Start");
        MapList mlInfoList = new MapList();
        try {
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            StringList slHyperLinkLabelKey = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.IANotification.HyperLinkLabelKey"), ",");
            StringList slHyperLinkLabelKeyIds = FrameworkUtil
                    .split(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.IANotification.HyperLinkLabelKeyIds"), ",");
            initializeIALinkedHashMap(lhmIAGenericSelectionStore, isImpact);
            mlInfoList = commonObj.transformGenericMapToHTMLList(context, objectMap, strBaseURL, lhmIAGenericSelectionStore, slHyperLinkLabelKey, slHyperLinkLabelKeyIds);

        } catch (Exception ex) {
            logger.error("Error in pss.ecm.enoECMChange_mxJPO : transformIAMapToHTMLList: ", ex);
        }
        logger.debug("pss.ecm.enoECMChange_mxJPO : transformIAMapToHTMLList : End");
        return mlInfoList;
    }

    /****
     * this method used for generating Linked HashMap of Notification object information
     * @param linkedHashMap
     * @param isImpact
     * @throws Exception
     */
    public void initializeIALinkedHashMap(LinkedHashMap<String, String> linkedHashMap, boolean isImpact) throws Exception {
        logger.debug("pss.ecm.enoECMChange_mxJPO : initializeIALinkedHashMap : Start");
        try {
            if (linkedHashMap != null && (linkedHashMap.isEmpty())) {

                linkedHashMap.put("Title", "SectionHeader");
                linkedHashMap.put("Subject", "SectionSubject");
                linkedHashMap.put("Main_Information", "SectionHeader");
                linkedHashMap.put("Project_Code", "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                linkedHashMap.put("Project_Description",
                        "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
                linkedHashMap.put("IA", DomainConstants.SELECT_NAME);
                linkedHashMap.put("IA_Description", DomainConstants.SELECT_DESCRIPTION);
                linkedHashMap.put("State", DomainConstants.SELECT_CURRENT);
                linkedHashMap.put("CR_Title", "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTITLE + "]");
                linkedHashMap.put("CR_Type", "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
                linkedHashMap.put("Change_Initiator", "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from." + DomainConstants.SELECT_ORIGINATOR);
                linkedHashMap.put("Change_Initiator_Email", "changeInitiatorEmail");
                if (isImpact) {
                    linkedHashMap.put("Requested_Assessment_End_Date",
                            "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE + "]");
                }
                linkedHashMap.put("Parallel_Track", "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");
                linkedHashMap.put("Parallel_Track_Comment", "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACKCOMMENT + "]");
                linkedHashMap.put("Useful_Links", "SectionHeader");
                linkedHashMap.put("Related_Content", "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.name");
                linkedHashMap.put("Related_COs", "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.name");
                linkedHashMap.put("Related_MCOs", "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.name");
            }

        } catch (RuntimeException re) {
            logger.error("Error in pss.ecm.enoECMChange_mxJPO : initializeIALinkedHashMap: ", re);
            throw re;
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.enoECMChange_mxJPO : initializeIALinkedHashMap: ", ex);
        }
        logger.debug("pss.ecm.enoECMChange_mxJPO : initializeIALinkedHashMap : End");
    }

    // TIGTK-9020:Rutuja Ekatpure:5/9/2017:End
    // TIGTK-6900 : START
    /****
     * This method is used as programHTMLOutput field in Clone CR Webform
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getCRCloneFormFields(Context context, String[] args) throws Exception {
        logger.debug("getCRCloneFormFields : START");
        StringBuffer strBufReturn = new StringBuffer();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map fieldMap = (HashMap) programMap.get("fieldMap");
            Map settingsMap = (HashMap) fieldMap.get("settings");
            String strSettingName = (String) settingsMap.get("attribute");
            strBufReturn.append("<select name=\"" + strSettingName + "\" >"); // Define the <select> element
            String strPropertyKey = "PSS_EnterpriseChangeMgt.CloneCR.SelectBox." + strSettingName;
            String strLanguage = context.getSession().getLanguage();
            String strPropertyValue = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", new Locale(strLanguage), strPropertyKey);
            StringList slPropertyValues = FrameworkUtil.split(strPropertyValue, ",");
            for (int i = 0; i < slPropertyValues.size(); i++) {
                strBufReturn.append(" <option value=\"" + slPropertyValues.get(i) + "\" >" + slPropertyValues.get(i) + "</option>");
            }
            strBufReturn.append("</select>");

        } catch (Exception ex) {
            logger.error("getCRCloneFormFields : ERROR ", ex);
            throw ex;
        }
        logger.debug("getCRCloneFormFields : END");
        return strBufReturn.toString();
    }

    /****
     * This method is used as post Process JPO for clone CR.
     * @param context
     * @param args
     * @return String[]
     * @throws Exception
     */
    public String[] cloneAndConnectCRs(Context context, String[] args) throws Exception {
        logger.debug("cloneAndConnectCRs : START");
        String[] alCloneIds = null;
        try {
            HashMap programMap = JPO.unpackArgs(args);
            String strCRobjectId = (String) programMap.get("objectId");
            String strCRNoOfClone = (String) programMap.get("NumberOfCopies");
            String strCRLinkAffectedItem = (String) programMap.get("IncludeRelatedAffectedItems");
            String strCRLinkReferenceDoc = (String) programMap.get("LinkReferenceDocument");
            String strCRLinkToOriginal = (String) programMap.get("LinkToOriginal");
            String strProgramProjectOID = (String) programMap.get("ProgramProjectOID");
            // TIGTK-17784 : START
            DomainObject objProgramProjectId = new DomainObject(strProgramProjectOID);
            String strPPState = objProgramProjectId.getInfo(context, DomainConstants.SELECT_CURRENT);
            // TIGTK-17784 : END
            int noofClone = Integer.parseInt(strCRNoOfClone);
            DomainObject domOriginalCRObj = DomainObject.newInstance(context, strCRobjectId);
            Map<String, String> mapAttribute = new HashMap<String, String>();
            mapAttribute.put(TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK, "No");
            mapAttribute.put(TigerConstants.ATTRIBUTE_PSS_CRCUSTOMERCHANGENUMBER, DomainConstants.EMPTY_STRING);
            mapAttribute.put(TigerConstants.ATTRIBUTE_BRANCH_TO, "None");
            // TIGTK-17784 : START
            mapAttribute.put(TigerConstants.ATTRIBUTE_PSS_PROJECT_PHASE_AT_CREATION, strPPState);
            // TIGTK-17784 : END
            String strAutoNumberSeries = DomainConstants.EMPTY_STRING;
            alCloneIds = new String[noofClone];
            for (int i = 0; i < noofClone; i++) {
                String strAutoName = DomainObject.getAutoGeneratedName(context, "type_ChangeRequest", strAutoNumberSeries);
                DomainObject domClonedCR = new DomainObject(domOriginalCRObj.cloneObject(context, strAutoName));
                // Copy the rest attributes as it is and except "Customer Change Number" and "Parallel Track"
                domClonedCR.setAttributeValues(context, mapAttribute);
                String clonedObjectId = domClonedCR.getId(context);
                alCloneIds[i] = clonedObjectId;
            }
            DomainObject domProgramProject = DomainObject.newInstance(context, strProgramProjectOID);
            DomainRelationship.connect(context, domProgramProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, true, alCloneIds);

            // get Lead changeManager from program project and connect to clone Change request
            StringList objectSelect = new StringList(DomainConstants.SELECT_ID);

            StringBuffer relWhere = new StringBuffer();
            relWhere.append("attribute[");
            relWhere.append(TigerConstants.ATTRIBUTE_PSS_POSITION);
            relWhere.append("]");
            relWhere.append(" == ");
            relWhere.append("Lead");
            relWhere.append(" && ");
            relWhere.append("attribute[");
            relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
            relWhere.append("]");
            relWhere.append(" == '");
            relWhere.append(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
            relWhere.append("'");
            MapList mlLeadCM = getMembersFromProgram(context, domProgramProject, objectSelect, DomainConstants.EMPTY_STRINGLIST, DomainConstants.EMPTY_STRING, relWhere.toString());
            if (mlLeadCM.size() > 0) {
                Map mapLeadCMDetails = (Map) mlLeadCM.get(0);
                String strLeadCMId = (String) mapLeadCMDetails.get(DomainObject.SELECT_ID);
                DomainObject domPersonObject = DomainObject.newInstance(context, (String) strLeadCMId);
                for (String strCloneId : alCloneIds) {
                    DomainObject domChangeRequestObject = DomainObject.newInstance(context, strCloneId);
                    // Connect Change Request with Change Manager
                    DomainRelationship.connect(context, domChangeRequestObject, ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR, domPersonObject);
                }

            }

            // Associate Affected Items to Cloned Object
            if ("Yes".equalsIgnoreCase(strCRLinkAffectedItem)) {
                connectAffectedItemsToCloneCR(context, domOriginalCRObj, alCloneIds);
            }
            // Associate Reference Document to Cloned Object
            if ("No".equalsIgnoreCase(strCRLinkReferenceDoc)) {
                for (String strCloneId : alCloneIds) {
                    disConnectDocument(context, strCloneId);
                }
            }
            if ("Clone".equalsIgnoreCase(strCRLinkReferenceDoc)) {
                cloneAndConnectReferenceDocumentsToCR(context, domOriginalCRObj, alCloneIds);
            }
            // Associate Clone CR To Original CR Object
            if ("Yes".equalsIgnoreCase(strCRLinkToOriginal)) {
                linkOriginalCRToCloneCR(context, domOriginalCRObj, alCloneIds);
            }

        } catch (Exception ex) {
            logger.error("cloneAndConnectCRs : ERROR ", ex);
            throw ex;
        }
        logger.debug("cloneAndConnectCRs : END");
        return alCloneIds;
    }

    /****
     * This method is used to connect the Original CR to Clone CR Object
     * @param context
     * @param domOriginalCRObj
     * @param arCloneIds
     * @return void
     * @throws Exception
     */
    public void linkOriginalCRToCloneCR(Context context, DomainObject domOriginalCRObj, String[] alCloneIds) throws Exception {
        logger.debug("linkOriginalCRToCloneCR : START");
        try {
            // TIGTK-10737 :16/11/2017 Aniket M START
            DomainRelationship.connect(context, domOriginalCRObj, TigerConstants.RELATIONSHIP_PSS_RELATED_CR, true, alCloneIds);
            // TIGTK-10737 :16/11/2017 Aniket M END
        } catch (Exception ex) {
            logger.error("linkOriginalCRToCloneCR : ERROR ", ex);
            throw ex;
        }
        logger.debug("linkOriginalCRToCloneCR : END");
    }

    /****
     * This method is used to connect the Reference Documents from Original CR to Clone CR Object
     * @param context
     * @param domOriginalCRObj
     * @param arCloneIds
     * @return void
     * @throws Exception
     */
    public void cloneAndConnectReferenceDocumentsToCR(Context context, DomainObject domOriginalCRObj, String[] alCloneIds) throws Exception {
        logger.debug("cloneAndConnectReferenceDocumentsToCR : START");
        try {
            for (String strCloneId : alCloneIds) {
                disConnectDocument(context, strCloneId);

            }
            StringList slReferenceDocsList = domOriginalCRObj.getInfoList(context, "from[" + DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT + "].to.id");
            // Iterate over the Reference Document and generate the Clone Reference Document
            Iterator itrReferenceDoc = slReferenceDocsList.iterator();
            while (itrReferenceDoc.hasNext()) {
                String strAutoNumberSeries = DomainConstants.EMPTY_STRING;
                String strReferenceDocId = (String) itrReferenceDoc.next();
                DomainObject domDocument = DomainObject.newInstance(context, strReferenceDocId);
                String strAutoName = DomainObject.getAutoGeneratedName(context, "type_Document", strAutoNumberSeries);
                DomainObject domCloneDocument = new DomainObject(domDocument.cloneObject(context, strAutoName));
                // Connect Clone Reference Document to each Clone CR Object
                for (String strCloneId : alCloneIds) {
                    DomainObject domCloneCR = DomainObject.newInstance(context, strCloneId);
                    // Generate the Cloned Document Objects
                    DomainRelationship.connect(context, domCloneCR, DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT, domCloneDocument);
                }
            }
        } catch (Exception ex) {
            logger.error("cloneAndConnectReferenceDocumentsToCR : ERROR ", ex);
            throw ex;
        }
        logger.debug("cloneAndConnectReferenceDocumentsToCR : END");

    }

    /****
     * This method is used to disconnect the Reference Documents from Original CR to Clone CR Object
     * @param context
     * @param arCloneIds
     * @return void
     * @throws Exception
     */
    public void disConnectDocument(Context context, String strCloneId) throws Exception {
        logger.debug("disConnectDocument: START ");
        try {
            BusinessObject boCloneCR = new BusinessObject(strCloneId);
            RelationshipList relationList = boCloneCR.getAllRelationship(context);
            Iterator it = relationList.iterator();
            while (it.hasNext()) {
                Relationship relationship = (Relationship) it.next();
                if (UIUtil.isNotNullAndNotEmpty(relationship.getTypeName()) && relationship.getTypeName().equalsIgnoreCase(DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT)) {
                    boCloneCR.disconnect(context, relationship);
                }
            }
        } catch (Exception ex) {
            logger.error("disConnectDocument : ", ex);
        }
        logger.debug("disConnectDocument: END ");

    }

    /****
     * This method is used to connect the Affected Items from Original CR to Clone CR Object
     * @param context
     * @param domOriginalCRObj
     * @param arCloneIds
     * @return void
     * @throws Exception
     */
    public void connectAffectedItemsToCloneCR(Context context, DomainObject domOriginalCRObj, String[] alCloneIds) throws Exception {
        logger.debug("connectAffectedItemsToCloneCR : START");
        try {
            StringList objectSelects = new StringList(1);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_CURRENT);
            StringList relSelects = new StringList(1);
            relSelects.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_INTERCHANGEABILITY + "]");
            StringBuffer sbWhereForAI = new StringBuffer();
            sbWhereForAI.append("current != '" + TigerConstants.STATE_OBSOLETE + "'");
            MapList mlCRAffectedItems = domOriginalCRObj.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, // relationship pattern
                    DomainConstants.QUERY_WILDCARD, // object pattern
                    objectSelects, // object selects
                    relSelects, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    sbWhereForAI.toString(), // object where clause
                    null, // relationship where clause
                    (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    null, null, null, null);
            Iterator itr = mlCRAffectedItems.iterator();
            Map mapAIAndInterChangeAbility = new HashMap();
            while (itr.hasNext()) {
                Map map = (Map) itr.next();
                String strId = (String) map.get(DomainConstants.SELECT_ID);
                String strInterChangeAbility = (String) map.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_INTERCHANGEABILITY + "]");
                mapAIAndInterChangeAbility.put(strId, strInterChangeAbility);
            }

            StringList slAfftectedItemList = getStringListFromMapList(mlCRAffectedItems, DomainConstants.SELECT_ID);
            if (!slAfftectedItemList.isEmpty()) {
                for (String strCloneId : alCloneIds) {
                    DomainObject domCloneCR = DomainObject.newInstance(context, strCloneId);
                    Map mapRelIDs = DomainRelationship.connect(context, domCloneCR, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, true,
                            (String[]) slAfftectedItemList.toArray(new String[slAfftectedItemList.size()]));

                    Set inputSet = mapRelIDs.entrySet();
                    Iterator inputItr = inputSet.iterator();
                    while (inputItr.hasNext()) {
                        Map.Entry inputEntry = (Map.Entry) inputItr.next();
                        String strAIObjectId = (String) inputEntry.getKey();
                        String strRelId = (String) inputEntry.getValue();
                        DomainObject domAffectedID = DomainObject.newInstance(context, strAIObjectId);
                        String strAICurrentState = domAffectedID.getInfo(context, DomainConstants.SELECT_CURRENT);
                        String strRequestedChangeValue = ChangeConstants.FOR_NONE;

                        StringList slReleasedState = new StringList();
                        slReleasedState.add(TigerConstants.STATE_RELEASED_CAD_OBJECT);
                        slReleasedState.add(TigerConstants.STATE_PART_RELEASE);

                        if (slReleasedState.contains(strAICurrentState)) {
                            strRequestedChangeValue = ChangeConstants.FOR_REVISE;

                        } else if (!strAICurrentState.equals(TigerConstants.STATE_PSS_CANCELPART_CANCELLED) && !strAICurrentState.equals(TigerConstants.STATE_PSS_CANCELCAD_CANCELLED)) {
                            strRequestedChangeValue = ChangeConstants.FOR_RELEASE;

                        }
                        String strInterChangeAbility = (String) mapAIAndInterChangeAbility.get(strAIObjectId);
                        DomainRelationship domRel = DomainRelationship.newInstance(context, strRelId);
                        domRel.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, strRequestedChangeValue);
                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_INTERCHANGEABILITY, strInterChangeAbility);

                    }

                }
            }
        } catch (Exception ex) {
            logger.error("connectAffectedItemsToCloneCR : ERROR ", ex);
            throw ex;
        }
        logger.debug("connectAffectedItemsToCloneCR : END");

    }

    /****
     * This method is used to get List of CLoned CR to display in table.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public MapList getCloneIdListForDisplay(Context context, String[] args) throws Exception {
        logger.debug("getCloneIdListForDisplay : START");
        MapList returnList = new MapList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestValuesMap = (HashMap) programMap.get("RequestValuesMap");
            String[] cloneIssueIds = (String[]) requestValuesMap.get("objIds");
            StringList slIds = FrameworkUtil.split(cloneIssueIds[0].toString(), "-");
            for (int i = 0; i < slIds.size(); i++) {
                HashMap ObjectMap = new HashMap();
                ObjectMap.put(DomainConstants.SELECT_ID, slIds.get(i).toString());
                returnList.add(ObjectMap);
            }
        } catch (Exception ex) {
            logger.error("getCloneIdListForDisplay : ERROR ", ex);
            throw ex;

        }
        logger.debug("getCloneIdListForDisplay : END");
        return returnList;
    }

    // TIGTK-7580 : START
    /****
     * This method is used to get HTML format of transfer ownership notifications of CR,CO,CN,MCO, issue
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getTransferOwnershipNotificationHTML(Context context, String[] args) throws Exception {
        logger.debug("getTransferOwnershipNotifiactionHTML : START");
        String sHTMLMsg = "";
        try {

            Map programMap = (Map) JPO.unpackArgs(args);
            String sChangeObjId = (String) programMap.get("id");
            DomainObject domChange = DomainObject.newInstance(context, sChangeObjId);

            String sType = domChange.getInfo(context, DomainConstants.SELECT_TYPE);
            if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(sType)) {
                sHTMLMsg = getCRNotificationBodyHTML(context, args);
            } else if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(sType)) {
                sHTMLMsg = getCONotificationBodyHTML(context, args);
            } else if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER.equalsIgnoreCase(sType)) {
                pss.ecm.ui.MfgChangeOrder_mxJPO mfgChangeOrderJPO = new pss.ecm.ui.MfgChangeOrder_mxJPO();
                sHTMLMsg = mfgChangeOrderJPO.getMCONotificationBodyHTML(context, args);
            } else if (TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(sType)) {
                pss.ecm.ui.ChangeNotice_mxJPO changeNoticeJPO = new pss.ecm.ui.ChangeNotice_mxJPO();
                sHTMLMsg = changeNoticeJPO.getCNNotificationBodyHTML(context, args);
            } else if (TigerConstants.TYPE_PSS_ISSUE.equalsIgnoreCase(sType)) {
                pss.ecm.notification.IssueNotificationUtil_mxJPO issueNotificationJPO = new pss.ecm.notification.IssueNotificationUtil_mxJPO(context, args);
                sHTMLMsg = issueNotificationJPO.getIssueNotificationBodyHTML(context, args);
            }

        } catch (Exception ex) {
            logger.error("getTransferOwnershipNotifiactionHTML : ERROR ", ex);
            throw ex;
        }
        logger.debug("getTransferOwnershipNotifiactionHTML : END");
        return sHTMLMsg;
    }

    /****
     * This method is used to give access of Transfer ownership command
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public boolean getTransferOwnershipAccess(Context context, String[] args) throws Exception {
        logger.debug("getTransferOwnershipAccess : START");
        boolean bResult = false;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String sChangeObjectID = (String) paramMap.get("objectId");
            DomainObject domChangeObj = DomainObject.newInstance(context, sChangeObjectID);
            String sCurrent = domChangeObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            String sType = domChangeObj.getInfo(context, DomainConstants.SELECT_TYPE);
            String sOwner = domChangeObj.getInfo(context, DomainConstants.SELECT_OWNER);
            String strContextUser = context.getUser();
            boolean bContextUserRole = false;

            StringList slRolesList = new StringList();
            slRolesList.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
            slRolesList.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, null);

            if (TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(sType)) {
                sChangeObjectID = domChangeObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_RELATEDCN + "].from.id");
            }

            StringList slPMLeadRoleMember = commonObj.getProgramProjectTeamMembersForChange(context, sChangeObjectID, slRolesList, true);
            if (!slPMLeadRoleMember.isEmpty()) {
                String strLeadPMPP = (String) slPMLeadRoleMember.get(0);
                if (strLeadPMPP.equalsIgnoreCase(strContextUser)) {
                    bContextUserRole = true;
                }
            }

            String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, strContextUser);
            String strRoleAssigned = (strSecurityContext.split("[.]")[0]);
            if (TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR.equalsIgnoreCase(strRoleAssigned) || TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM.equalsIgnoreCase(strRoleAssigned)
                    || sOwner.equalsIgnoreCase(strContextUser)) {
                bContextUserRole = true;
            }

            StringList slAllowedTypes = new StringList();
            slAllowedTypes.add(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            slAllowedTypes.add(TigerConstants.TYPE_PSS_CHANGEORDER);
            slAllowedTypes.add(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
            slAllowedTypes.add(TigerConstants.TYPE_PSS_CHANGENOTICE);
            slAllowedTypes.add(TigerConstants.TYPE_PSS_ISSUE);

            StringList slDeniedStates = new StringList();
            slDeniedStates.add(TigerConstants.STATE_COMPLETE_CR);
            slDeniedStates.add(TigerConstants.STATE_REJECTED_CR);
            slDeniedStates.add(TigerConstants.STATE_PSS_CHANGEORDER_COMPLETE);
            slDeniedStates.add(TigerConstants.STATE_CHANGEORDER_IMPLEMENTED);
            slDeniedStates.add(TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED);
            slDeniedStates.add(TigerConstants.STATE_PSS_MCO_IMPLEMENTED);
            slDeniedStates.add(TigerConstants.STATE_PSS_MCO_CANCELLED);
            slDeniedStates.add(TigerConstants.STATE_PSS_MCO_COMPLETE);
            slDeniedStates.add(TigerConstants.STATE_PSS_CHANGENOTICE_CANCELLED);
            slDeniedStates.add(TigerConstants.STATE_FULLYINTEGRATED);
            slDeniedStates.add(TigerConstants.STATE_CN_CANCELLED);
            slDeniedStates.add(TigerConstants.STATE_PSS_ISSUE_CLOSED);
            slDeniedStates.add(TigerConstants.STATE_PSS_ISSUE_REJECTED);

            if (slAllowedTypes.contains(sType) && !slDeniedStates.contains(sCurrent) && bContextUserRole) {
                bResult = true;
            }
        } catch (Exception ex) {
            logger.error("getTransferOwnershipAccess : ERROR ", ex);
            throw ex;
        }
        logger.debug("getTransferOwnershipAccess : END");
        return bResult;
    }

    /****
     * This method is used to get subject of transfer ownership notification
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getTranferOwnershipSubject(Context context, String[] args) throws Exception {
        logger.debug("getTranferOwnershipSubject : START");
        String strSubjectKey = "";
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String sChangeObjectID = (String) paramMap.get("id");
            DomainObject domChangeObj = DomainObject.newInstance(context, sChangeObjectID);
            String sType = domChangeObj.getInfo(context, DomainConstants.SELECT_TYPE);
            strSubjectKey = "EnterpriseChangeMgt.Notification.SubjectkeyTransferOwnership.";
            if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(sType)) {
                strSubjectKey += TigerConstants.TYPE_PSS_CHANGEREQUEST;
            } else if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(sType)) {
                strSubjectKey += TigerConstants.TYPE_PSS_CHANGEORDER;
            } else if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER.equalsIgnoreCase(sType)) {
                strSubjectKey += TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER;
            } else if (TigerConstants.TYPE_PSS_CHANGENOTICE.equalsIgnoreCase(sType)) {
                strSubjectKey += TigerConstants.TYPE_PSS_CHANGENOTICE;
            } else if (TigerConstants.TYPE_PSS_ISSUE.equalsIgnoreCase(sType)) {
                strSubjectKey += TigerConstants.TYPE_PSS_ISSUE;
            }
            strSubjectKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strSubjectKey);
        } catch (Exception ex) {
            logger.error("getTranferOwnershipSubject : ERROR ", ex);
            throw ex;
        }
        logger.debug("getTranferOwnershipSubject : END");
        return strSubjectKey;
    }
    // TIGTK-7580 : END

    // TIGTK-10261:START
    public String isProgramProjectFieldEditable(Context context, String[] args) throws Exception {
        logger.debug("isProgramProjectFieldEditable : START");
        String isEditable = "false";
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            String sChangeObjectID = (String) paramMap.get("objectId");

            DomainObject domChangeObj = DomainObject.newInstance(context, sChangeObjectID);
            // TIGTK-13112 : PSE : 09-02-2018 : START
            StringList slObjectsSelectes = new StringList();
            slObjectsSelectes.addElement(DomainConstants.SELECT_CURRENT);
            slObjectsSelectes.addElement(DomainConstants.SELECT_TYPE);
            Map mChangeInfoMap = domChangeObj.getInfo(context, slObjectsSelectes);
            String sCurrent = (String) mChangeInfoMap.get(DomainConstants.SELECT_CURRENT);
            String sType = (String) mChangeInfoMap.get(DomainConstants.SELECT_TYPE);
            // TIGTK-13112 : PSE : 09-02-2018 : END
            if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equals(sType)) {

                StringList slState = new StringList();
                slState.add(TigerConstants.STATE_PSS_CR_CREATE);
                slState.add(TigerConstants.STATE_SUBMIT_CR);
                if (slState.contains(sCurrent)) {
                    // TIGTK-13112 : PSE : 09-02-2018 : START
                    // Type Pattern
                    Pattern includeTypePattern = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
                    includeTypePattern.addPattern(TigerConstants.TYPE_PSS_CHANGEORDER);

                    // Rel Pattern
                    Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER);
                    relPattern.addPattern(TigerConstants.RELATIONSHIP_CHANGEORDER);

                    MapList mlConnectedCOMCO = domChangeObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                            includeTypePattern.getPattern(), // object pattern
                            new StringList(DomainConstants.SELECT_ID), // object selects
                            new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                            true, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            DomainConstants.EMPTY_STRING, // object where clause
                            DomainConstants.EMPTY_STRING, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 0, // pageSize
                            null, null, null, null, null);
                    if (mlConnectedCOMCO.isEmpty()) {
                        isEditable = "true";
                    }
                    // TIGTK-13112 : PSE : 09-02-2018 : END
                }
            } else if (TigerConstants.TYPE_PSS_CHANGEORDER.equals(sType)) {

                if (TigerConstants.STATE_PSS_CHANGEORDER_PREPARE.equals(sCurrent)) {
                    // TIGTK-13112 : PSE : 09-02-2018 : START
                    // Type Pattern
                    Pattern includeTypePattern = new Pattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
                    includeTypePattern.addPattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
                    includeTypePattern.addPattern(TigerConstants.TYPE_CHANGEACTION);

                    // Rel Pattern
                    Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER);
                    relPattern.addPattern(TigerConstants.RELATIONSHIP_CHANGEORDER);
                    relPattern.addPattern(ChangeConstants.RELATIONSHIP_CHANGE_ACTION);

                    MapList mlConnectedCRCAMCO = domChangeObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                            includeTypePattern.getPattern(), // object pattern
                            new StringList(DomainConstants.SELECT_ID), // object selects
                            new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                            true, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            DomainConstants.EMPTY_STRING, // object where clause
                            DomainConstants.EMPTY_STRING, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 0, // pageSize
                            null, null, null, null, null);
                    if (mlConnectedCRCAMCO.isEmpty()) {
                        isEditable = "true";
                    }
                }
            }
            // TIGTK-13112 : PSE : 08-02-2018 : START
            else if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER.equals(sType)) {
                if (TigerConstants.STATE_PSS_MCO_PREPARE.equals(sCurrent)) {
                    // Type Pattern
                    Pattern includeTypePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEORDER);
                    includeTypePattern.addPattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
                    includeTypePattern.addPattern(TigerConstants.TYPE_PSS_CHANGENOTICE);

                    // Rel Pattern
                    Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER);
                    relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_RELATEDCN);

                    MapList mlConnectedCRCOCN = domChangeObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                            includeTypePattern.getPattern(), // object pattern
                            new StringList(DomainConstants.SELECT_ID), // object selects
                            new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                            true, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            DomainConstants.EMPTY_STRING, // object where clause
                            DomainConstants.EMPTY_STRING, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 0, // pageSize
                            null, null, null, null, null);
                    if (mlConnectedCRCOCN.isEmpty()) {
                        isEditable = "true";
                    }
                }
            }
            // TIGTK-13112 : PSE : 08-02-2018 : END
        } catch (Exception ex) {
            logger.error("isProgramProjectFieldEditable : ERROR ", ex);
            throw ex;
        }
        logger.debug("isProgramProjectFieldEditable : END");
        return isEditable;
    }
    // TIGTK-10261:END

    // TIGTK-10709 -- START
    /****
     * This method is used to get subject of task reassignment notification
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getTaskReassignmentSubject(Context context, String[] args) throws Exception {
        logger.debug("getTaskReassignmentSubject : START");
        StringBuffer sbSubjectKey = new StringBuffer();
        String strTaskReassignKey = DomainConstants.EMPTY_STRING;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            HashMap payload = (HashMap) paramMap.get("payload");
            String strInboxTaskId = (String) payload.get("inboxtaskId");
            if (UIUtil.isNotNullAndNotEmpty(strInboxTaskId)) {
                DomainObject domInboxTaskObject = DomainObject.newInstance(context, strInboxTaskId);

                // Get Name of Inbox Task object:
                String strInboxTaskName = (String) domInboxTaskObject.getInfo(context, DomainConstants.SELECT_NAME);
                strTaskReassignKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "EnterpriseChangeMgt.CRNotification.SubjectkeyTaskReassignment");
                sbSubjectKey.append(" " + strTaskReassignKey + " ");
                sbSubjectKey.append(strInboxTaskName);
            }

        } catch (Exception ex) {
            logger.error("getTaskReassignmentSubject : ERROR ", ex);
            throw ex;
        }
        logger.debug("getTaskReassignmentSubject : END");

        return sbSubjectKey.toString();
    }
    // TIGTK-10709 -- END

    // TIGTK-10697: TS :30/10/2017:END

    // TIGTK-10772 Suchit Gangurde: 01/11/2017: START
    /***
     * This method is used to get Impact Analysis assignees of CR.
     * @param context
     * @param strCRObjId
     * @return MapList
     * @throws Exception
     */
    public StringList getConnectedImpactAnalysisAssignees(Context context, String strCRObjId) throws Exception {
        logger.debug("pss.ecm.enoECMChange_mxJPO:getConnectedImpactAnalysisAssignees:START ");
        try {
            StringList slAllIAAssignees = new StringList();
            StringList slObjectSel = new StringList();
            slObjectSel.addElement(DomainConstants.SELECT_ID);
            slObjectSel.addElement(DomainConstants.SELECT_NAME);
            String strwhere = "revision == last";

            DomainObject domCRObj = DomainObject.newInstance(context, strCRObjId);
            MapList mlImpactAnalysis = domCRObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjectSel,
                    DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, strwhere, null, 0);

            if (mlImpactAnalysis != null && !mlImpactAnalysis.isEmpty()) {
                Iterator itrImpactAnalysis = mlImpactAnalysis.iterator();
                while (itrImpactAnalysis.hasNext()) {
                    Map mImpactAnalysis = (Map) itrImpactAnalysis.next();
                    String strIAObjID = (String) mImpactAnalysis.get(DomainConstants.SELECT_ID);
                    if (UIUtil.isNotNullAndNotEmpty(strIAObjID)) {

                        DomainObject domIAObj = DomainObject.newInstance(context, strIAObjID);
                        StringList slRAOwnerList = domIAObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].to.owner");
                        slAllIAAssignees.addAll(slRAOwnerList);
                    }
                }
            }

            logger.debug("pss.ecm.enoECMChange_mxJPO:getConnectedImpactAnalysisAssignees:END ");
            return slAllIAAssignees;
        } catch (Exception ex) {
            logger.error("error in pss.ecm.enoECMChange_mxJPO:getConnectedImpactAnalysisAssignees: " + ex.getMessage());
            throw ex;
        }
    }

    // TIGTK-10772 Suchit Gangurde: 01/11/2017: END
    // TIGTK-10768: TS : 15/11/2017 : Start
    /****
     * This method is used to get subject of task reassignment notification
     * @param context
     * @param args
     * @return String
     * @throws Exception
     */
    public String getChangeReassignmentSubject(Context context, String[] args) throws Exception {
        logger.debug("getChangeReassignmentSubject : START");
        StringBuffer sbSubjectKey = new StringBuffer();
        String strChangeAssigneeKey = DomainConstants.EMPTY_STRING;
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            HashMap payload = (HashMap) paramMap.get("payload");
            String strChangeObjName = (String) payload.get("ChangeObjName");
            String strChangeObjType = (String) payload.get("ChangeObjType");
            if (TigerConstants.TYPE_CHANGEACTION.equalsIgnoreCase(strChangeObjType)) {

                strChangeAssigneeKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "EnterpriseChangeMgt.CANotification.SubjectkeyCAAssigneeReassignment");
                sbSubjectKey.append(" " + strChangeAssigneeKey + " ");
                sbSubjectKey.append(strChangeObjName);
            } else if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strChangeObjType)) {

                strChangeAssigneeKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "EnterpriseChangeMgt.MCANotification.SubjectkeyMCAAssigneeReassignment");
                sbSubjectKey.append(" " + strChangeAssigneeKey + " ");
                sbSubjectKey.append(strChangeObjName);
            }
        } catch (Exception ex) {
            logger.error("getChangeReassignmentSubject : ERROR ", ex);
            throw ex;
        }
        logger.debug("getChangeReassignmentSubject : END");
        return sbSubjectKey.toString();
    }

    public StringList getChangeReassignmentToList(Context context, String[] args) throws Exception {
        logger.debug("getChangeReassignmentToList : START");
        StringList slGenericToList = new StringList();
        String strContextUser = context.getUser();
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            Map payload = (Map) programMap.get("payload");
            String strReturnedUser = (String) payload.get("toList");
            slGenericToList.add(strReturnedUser);
            if (slGenericToList.contains(strContextUser)) {
                while (slGenericToList.contains(strContextUser)) {
                    slGenericToList.remove(strContextUser);
                }
            }

        } catch (Exception ex) {

            logger.error("Error in getChangeReassignmentToList: ", ex);

        }
        logger.debug("getChangeReassignmentToList : END");
        return slGenericToList;
    }

    // TIGTK-10768: TS : 15/11/2017 : Start
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

    /**
     * This is used as Edit Access Function for Requested Change column to set that cell as editable or not depands on state of AI.
     * @param context
     * @param args
     * @return StringList contains boolean value which will decide weather cell is editable or not.
     * @throws Exception
     */
    public static StringList getStateLevelEditAccess(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");

        StringList returnStringList = new StringList(objectList.size());
        Iterator objectItr = objectList.iterator();
        while (objectItr.hasNext()) {
            Map curObjectMap = (Map) objectItr.next();
            String AIObjectID = (String) curObjectMap.get("id");
            DomainObject domainObj = DomainObject.newInstance(context, AIObjectID);
            String strAICurrentState = domainObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            if (strAICurrentState.equals(TigerConstants.STATE_PSS_CANCELPART_CANCELLED) || strAICurrentState.equals(TigerConstants.STATE_PSS_CANCELCAD_CANCELLED)) {
                returnStringList.add(Boolean.FALSE);
            } else {
                returnStringList.add(Boolean.TRUE);
            }
        }
        return returnStringList;
    }

    public StringList getStringListFromMaplist(MapList mlInputList, String selectable) throws Exception {
        StringList slReturnList = new StringList();
        if (!mlInputList.isEmpty() && mlInputList.size() > 0) {
            for (int i = 0; i < mlInputList.size(); i++) {
                Map tempMap = (Map) mlInputList.get(i);
                String strValue = (String) tempMap.get(selectable);
                slReturnList.add(strValue);
            }
        }
        return slReturnList;
    }

    public StringList getSystemCreatedFinalImplementedItemIdList(Context context, DomainObject domCAObject) throws Exception {
        try {
            StringList slSystemCreatedImplementedItemIdList = new StringList();
            StringList relSelects = new StringList(1);
            relSelects.add(DomainRelationship.SELECT_RELATIONSHIP_ID);
            relSelects.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
            StringList slAISelectable = new StringList();
            slAISelectable.add(DomainConstants.SELECT_ID);
            slAISelectable.add(DomainConstants.SELECT_CURRENT);
            MapList mlAIList = domCAObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, "*", slAISelectable, relSelects, false, true, (short) 1, null, null, 0);

            Iterator<Map<?, ?>> itrAffectedItem = mlAIList.iterator();
            while (itrAffectedItem.hasNext()) {
                Map<?, ?> AffectedItem = itrAffectedItem.next();
                String strAffectedItemID = (String) AffectedItem.get(DomainConstants.SELECT_ID);
                DomainObject domAffectedItem = DomainObject.newInstance(context, strAffectedItemID);
                String strCRRequesedChangeValue = (String) AffectedItem.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                if (ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strCRRequesedChangeValue) || ChangeConstants.FOR_NONE.equalsIgnoreCase(strCRRequesedChangeValue)
                        || ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strCRRequesedChangeValue)) {
                    slSystemCreatedImplementedItemIdList.add(strAffectedItemID);

                } else if (ChangeConstants.FOR_REVISE.equalsIgnoreCase(strCRRequesedChangeValue)) {
                    if (!domAffectedItem.isLastRevision(context)) {
                        BusinessObject bNextRev = domAffectedItem.getNextRevision(context);
                        if (bNextRev.exists(context)) {
                            String strNextRevID = bNextRev.getObjectId();
                            slSystemCreatedImplementedItemIdList.add(strNextRevID);
                        }
                    }

                } else if (TigerConstants.FOR_CLONE.equalsIgnoreCase(strCRRequesedChangeValue) || TigerConstants.FOR_REPLACE.equalsIgnoreCase(strCRRequesedChangeValue)) {
                    if (domAffectedItem.isKindOf(context, TigerConstants.TYPE_PART)) {
                        String strNewPartID = domAffectedItem.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_DERIVED + "].to.id");
                        if (UIUtil.isNotNullAndNotEmpty(strNewPartID))
                            slSystemCreatedImplementedItemIdList.add(strNewPartID.trim());
                    } else {

                        String strNewPartID = domAffectedItem.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].to.id");
                        if (UIUtil.isNotNullAndNotEmpty(strNewPartID))
                            slSystemCreatedImplementedItemIdList.add(strNewPartID);
                    }

                }
            }
            return slSystemCreatedImplementedItemIdList;
        } catch (RuntimeException e) {
            logger.error("Error in enoECMChange : getSystemCreatedFinalImplementedItemIdList : ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in enoECMChange: getSystemCreatedFinalImplementedItemIdList : ", e);
            throw e;
        }

    }

    public StringList getSystemCreatedImplementedItemIdList(Context context, StringList slCAOIDList) throws Exception {
        try {
            Iterator itrChange = slCAOIDList.iterator();
            StringList slSystemCreatedImplementedItemIdList = new StringList();
            while (itrChange.hasNext()) {
                String strCAOID = (String) itrChange.next();
                // Create Domain Object of CA
                DomainObject domCAObject = DomainObject.newInstance(context, strCAOID);
                slSystemCreatedImplementedItemIdList.addAll(getSystemCreatedFinalImplementedItemIdList(context, domCAObject));
            }
            return slSystemCreatedImplementedItemIdList;
        } catch (Exception e) {
            logger.error("Error in enoECMChange : getSystemCreatedImplementedItemIdList : ", e);
            throw e;
        }

    }
}