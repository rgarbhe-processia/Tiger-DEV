package pss.mbom.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.mxType;
import com.matrixone.apps.engineering.EngineeringConstants;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class UIUtil_mxJPO {

    // TIGTK-5405 - 07-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UIUtil_mxJPO.class);

    // TIGTK-5405 - 07-04-2017 - VB - END

    public static final StringList STATES_INWORK = new StringList() {
        {
            add("In Work");
            add("IN_WORK");
            add("Approved");
            add("InWork");
        }
    };

    public static void flushAndCloseSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.flushSession();
        } catch (Exception e) {
        }

        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
        }
    }

    /**
     * This method is used to display the connected CRs, CNs and MCOs of MBOM in table
     * @param context
     * @param args
     * @throws Exception
     */
    public Vector<String> getModificationHistory(Context context, String[] args) throws Exception {

        Vector<String> vModHistory = new Vector<String>(10);
        try {
            Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList mlObjectList = (MapList) programMap.get("objectList");

            Iterator<?> itrObjList = mlObjectList.iterator();
            while (itrObjList.hasNext()) {
                Map<?, ?> objMap = (Map<?, ?>) itrObjList.next();
                String strObjectId = (String) objMap.get(DomainConstants.SELECT_ID);
                DomainObject domModHistory = DomainObject.newInstance(context, strObjectId);

                StringList slObjSelectStmts = new StringList();
                slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
                slObjSelectStmts.addElement(DomainConstants.SELECT_TYPE);
                slObjSelectStmts.addElement(DomainConstants.SELECT_CURRENT);
                slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

                StringList slRelSelectStmts = new StringList(1);
                slRelSelectStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
                typePattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);
                typePattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION);

                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_RELATED150MBOM);

                Pattern postTypePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
                postTypePattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);

                MapList mlList = domModHistory.getRelatedObjects(context, relPattern.getPattern(), // Relationship
                        // Pattern
                        typePattern.getPattern(), // Object Pattern
                        slObjSelectStmts, // Object Selects
                        slRelSelectStmts, // Relationship Selects
                        true, // to direction
                        false, // from direction
                        (short) 3, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        postTypePattern, // Post Type Pattern
                        null, null, null);
                Iterator<?> itr = mlList.iterator();
                StringBuilder sbPrevCRs = new StringBuilder();
                sbPrevCRs.append("<div> Previous CRs : ");

                StringBuilder sbOnCRs = new StringBuilder();
                sbOnCRs.append("<div> Ongoing CRs : ");

                StringBuilder sbPrevMCOs = new StringBuilder();
                sbPrevMCOs.append("<div> Previous MCOs : ");

                StringBuilder sbOnMCOs = new StringBuilder();
                sbOnMCOs.append("<div> Ongoing MCOs : ");

                StringBuilder sbPrevCNs = new StringBuilder();
                sbPrevCNs.append("<div> Previous CNs : ");

                StringBuilder sbOnCNs = new StringBuilder();
                sbOnCNs.append("<div> Ongoing CNs : ");
                StringList processesIds = new StringList();
                while (itr.hasNext()) {
                    Map<?, ?> mTempMap = (Map<?, ?>) itr.next();

                    String strType = (String) mTempMap.get(DomainConstants.SELECT_TYPE);
                    String strName = (String) mTempMap.get(DomainConstants.SELECT_NAME);
                    String strState = (String) mTempMap.get(DomainConstants.SELECT_CURRENT);
                    String strId = (String) mTempMap.get(DomainConstants.SELECT_ID);

                    if (UIUtil.isNotNullAndNotEmpty(strId) && !processesIds.contains(strId)) {
                        processesIds.addElement(strId);
                        // if (strType.equals(TYPE_PSS_CHANGEREQUEST) || strType.equals(TYPE_CHANGEREQUEST)) {
                        if (strType.equals(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                            if (strState.equals(TigerConstants.STATE_COMPLETE_CR) || strState.equals(TigerConstants.STATE_REJECTED_CR)) {

                                sbPrevCRs.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                                sbPrevCRs.append(strId);
                                sbPrevCRs.append("', '860', '520', 'false', 'popup')\">");
                                sbPrevCRs.append(" " + strName);
                                sbPrevCRs.append("</a>  ");
                            }

                            else {
                                sbOnCRs.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                                sbOnCRs.append(strId);
                                sbOnCRs.append("', '860', '520', 'false', 'popup')\">");
                                sbOnCRs.append(" " + strName);
                                sbOnCRs.append("</a>  ");

                            }
                            // } else if (strType.equals(TYPE_MCO) || strType.equals(TYPE_PSS_MANUFACTUINGCHANGEORDER)) {
                        } else if (strType.equals(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                            if (strState.equals(TigerConstants.STATE_PSS_MCO_REJECTED) || strState.equals(TigerConstants.STATE_PSS_MCO_CANCELLED)
                                    || strState.equals(TigerConstants.STATE_PSS_MCO_IMPLEMENTED) || strState.equals(TigerConstants.STATE_PSS_MCO_COMPLETE)) {

                                sbPrevMCOs.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                                sbPrevMCOs.append(strId);
                                sbPrevMCOs.append("', '860', '520', 'false', 'popup')\">");
                                sbPrevMCOs.append(" " + strName);
                                sbPrevMCOs.append("</a>  ");

                            } else {

                                sbOnMCOs.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                                sbOnMCOs.append(strId);
                                sbOnMCOs.append("', '860', '520', 'false', 'popup')\">");
                                sbOnMCOs.append(" " + strName);
                                sbOnMCOs.append("</a>  ");

                            }
                        }
                    }
                }
                Pattern typeCNPattern = new Pattern(TigerConstants.TYPE_FPDM_MBOMPART);
                typeCNPattern.addPattern(TigerConstants.TYPE_PSS_CHANGENOTICE);

                Pattern relCNPattern = new Pattern(TigerConstants.RELATIONSHIP_FPDM_GENERATEDMBOM);
                relCNPattern.addPattern(TigerConstants.RELATIONSHIP_FPDM_CNAFFECTEDITEMS);

                Pattern postCNPattern = new Pattern(TigerConstants.TYPE_PSS_CHANGENOTICE);

                MapList mlCNList = domModHistory.getRelatedObjects(context, relCNPattern.getPattern(), // Relationship
                        // Pattern
                        typeCNPattern.getPattern(), // Object Pattern
                        slObjSelectStmts, // Object Selects
                        slRelSelectStmts, // Relationship Selects
                        true, // to direction
                        true, // from direction
                        (short) 2, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        postCNPattern, // Post Type Pattern
                        null, null, null);
                Iterator<?> itrlist = mlCNList.iterator();
                processesIds = new StringList();
                while (itrlist.hasNext()) {
                    Map<?, ?> mMap = (Map<?, ?>) itrlist.next();

                    String strCNType = (String) mMap.get(DomainConstants.SELECT_TYPE);
                    String strCNName = (String) mMap.get(DomainConstants.SELECT_NAME);
                    String strCNState = (String) mMap.get(DomainConstants.SELECT_CURRENT);
                    String strCNId = (String) mMap.get(DomainConstants.SELECT_ID);
                    if (UIUtil.isNotNullAndNotEmpty(strCNId) && !processesIds.contains(strCNId)) {
                        processesIds.addElement(strCNId);
                        if (strCNType.equals(TigerConstants.TYPE_PSS_CHANGENOTICE)) {
                            if (strCNState.equals(TigerConstants.STATE_TRANSFERERROR) || strCNState.equals(TigerConstants.STATE_FULLYINTEGRATED)
                                    || strCNState.equals(TigerConstants.STATE_CN_CANCELLED)) {

                                sbPrevCNs.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                                sbPrevCNs.append(strCNId);
                                sbPrevCNs.append("', '860', '520', 'false', 'popup')\">");
                                sbPrevCNs.append(" " + strCNName);
                                sbPrevCNs.append("</a>  ");

                            } else {

                                sbOnCNs.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                                sbOnCNs.append(strCNId);
                                sbOnCNs.append("', '860', '520', 'false', 'popup')\">");
                                sbOnCNs.append(" " + strCNName);
                                sbOnCNs.append("</a>  ");

                            }
                        }
                    }
                }
                sbPrevCRs.append("</div>");
                sbOnCRs.append("</div>");
                sbPrevMCOs.append("</div>");
                sbOnMCOs.append("</div>");
                sbPrevCNs.append("</div>");
                sbOnCNs.append("</div>");

                vModHistory.add(sbPrevCRs.toString() + sbOnCRs.toString() + sbPrevMCOs.toString() + sbOnMCOs.toString() + sbPrevCNs.toString() + sbOnCNs.toString());
            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getModificationHistory: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END

        }

        return vModHistory;

    }

    /**
     * Get all associated Products to Part.
     * @name getAllAssociatedProductsFromPartId
     * @param context
     *            the Matrix Context
     * @param args
     *            contains- -- programMap -> objectId
     * @returns Map
     * @throws FrameworkException
     *             if the operation fails
     */
    public Map<Integer, String> getAllAssociatedProductsFromPartId(Context context, String[] args) throws Exception {

        MapList mlProductList = new MapList();
        HashMap<Integer, String> mapRetProducts = new HashMap<Integer, String>();
        HashSet<String> setProducts = new HashSet<String>();
        String strProductId;

        PLMCoreModelerSession plmSession = null;
        boolean transactioActive = false;
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strMBOMObjectId = (String) programMap.get("objectId");
            Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_GBOM);
            relPattern.addPattern(DomainConstants.RELATIONSHIP_EBOM);

            Pattern postRelPattern = new Pattern(TigerConstants.RELATIONSHIP_GBOM);

            StringList slselectObjStmts = new StringList(DomainConstants.SELECT_ID);
            slselectObjStmts.addElement(DomainConstants.SELECT_NAME);
            List<String> objectIdList = new ArrayList<String>();
            objectIdList.add(strMBOMObjectId);

            ContextUtil.startTransaction(context, false);
            transactioActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            List<String> strPhysicalStructureId = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, objectIdList);
            if (strPhysicalStructureId != null && strPhysicalStructureId.size() > 0) {
                MapList mapLIst = getPartFromVPMReference(context, strPhysicalStructureId.get(0));
                if (mapLIst != null && mapLIst.size() > 0) {
                    Map objMap = (Map) mapLIst.get(0);
                    DomainObject domPart = DomainObject.newInstance(context, (String) objMap.get(DomainConstants.SELECT_ID));
                    mlProductList = domPart.getRelatedObjects(context, relPattern.getPattern(), // relationship
                            // pattern
                            DomainConstants.QUERY_WILDCARD, // object pattern
                            slselectObjStmts, // object selects
                            null, // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 0, // recursion level
                            null, // object where clause
                            null, (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, // Post pattern
                            postRelPattern, null, null);
                }
            }

            // Iterate mlProductList to get Product Id and Products in HashSet
            // setProducts
            Iterator<?> itr = mlProductList.iterator();
            while (itr.hasNext()) {
                Map<?, ?> mTempMap = (Map<?, ?>) itr.next();
                strProductId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                setProducts.add(strProductId);
            }

            // Iterate HashSet setProducts and add values to HashMap
            // mapRetProducts.
            Iterator<String> itrSetProducts = setProducts.iterator();
            int intProductIndex = 0;
            while (itrSetProducts.hasNext()) {
                mapRetProducts.put(intProductIndex, itrSetProducts.next());
                intProductIndex++;
            }
            closeSession(plmSession);
            if (transactioActive) {
                ContextUtil.commitTransaction(context);
            }
        } catch (Exception e) {
            closeSession(plmSession);
            if (transactioActive) {
                ContextUtil.abortTransaction(context);
            }
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getAllAssociatedProductsFromPartId: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END

            throw e;

        }
        return mapRetProducts;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public static MapList getPartFromVPMReference(Context context, String sPorductId) throws Exception {
        MapList mlRet = new MapList();
        try {
            if (UIUtil.isNotNullAndNotEmpty(sPorductId)) {
                DomainObject domObj = new DomainObject(sPorductId);
                StringList objSelects = new StringList(4);
                objSelects.addElement(DomainConstants.SELECT_ID);
                objSelects.addElement("physicalid");
                objSelects.addElement(DomainConstants.SELECT_TYPE);
                objSelects.addElement(DomainConstants.SELECT_NAME);
                objSelects.addElement(DomainConstants.SELECT_REVISION);
                String sObjTypes = "*";
                String sRelTypes = EngineeringConstants.RELATIONSHIP_PART_SPECIFICATION;
                // Case 1 : Get Part through Part Spec relationship
                MapList mlRelated = domObj.getRelatedObjects(context, sRelTypes, sObjTypes, objSelects, null, true, false, (short) 1, null, null);
                if (null != mlRelated) {
                    for (int i = 0; i < mlRelated.size(); i++) {
                        Map mObj = (Map) mlRelated.get(i);
                        String sType = (String) mObj.get(DomainConstants.SELECT_TYPE);
                        if (mxType.isOfParentType(context, sType, "Part")) {
                            mlRet.add(mObj);
                        }
                    }
                }

                // Case 2 : Get Part through a query based on TNR
                if (0 == mlRet.size()) {
                    StringList productSelects = new StringList(3);
                    productSelects.addElement(DomainConstants.SELECT_TYPE);
                    productSelects.addElement(DomainConstants.SELECT_NAME);
                    productSelects.addElement("majororder");

                    Map mProduct = domObj.getInfo(context, productSelects);

                    /*
                     * String sType = (String)mPart.get(DomainConstants.SELECT_TYPE); String psTypeToSearch = ebomToPSTypeMapping.get(sType); if (psTypeToSearch == null || "".equals(psTypeToSearch))
                     * throw new Exception ("EBOM type " + sType + " has no mapping defined for equivalent Product Structure type.");
                     */

                    String sName = (String) mProduct.get(DomainConstants.SELECT_NAME);
                    String sRevIndex = (String) mProduct.get("majororder");
                    String sWhere = "minororder == \"" + sRevIndex + "\"";
                    // findObjects(context, typePattern, namePattern, revPattern, ownerPattern, vaultPattern, whereExpression, expandType, objectSelects)
                    mlRet = DomainObject.findObjects(context, DomainConstants.TYPE_PART, sName, "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION, sWhere, true, objSelects);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getPartFromVPMReference: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END

        }
        return mlRet;
    }

    /**
     * Get all Color Options which are connected to selected Part and render as column.
     * @name getProductRelatedColorAsColumns
     * @param context
     *            the Matrix Context
     * @param args
     *            contains-programMap --> columnMap ,programMap --> objectList
     * @returns StringList
     * @throws Exception
     *             if the operation fails
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getProductRelatedColorAsColumns(Context context, String[] args) throws Exception {

        HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> requestMap = (HashMap<?, ?>) paramMap.get("requestMap");

        String strProductId = (String) requestMap.get("strProductId");
        StringList typeList = FrameworkUtil.split(strProductId, ",");
        String STR_ACCEPTED_CATALOG_STATE = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.ColorCatalog.AcceptedState");
        String STR_ACCEPTED_COLOR_STATE = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.ColorOption.AcceptedState");

        StringList slObjSelectStmts = new StringList(2);
        slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
        slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
        slObjSelectStmts.addElement(DomainConstants.SELECT_CURRENT);
        // TIGTK-10500:Start
        slObjSelectStmts.addElement(TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
        // TIGTK-10500:End

        StringList slRelSelectStmts = new StringList(1);
        MapList mlColumnMapList = new MapList();

        MapList newMap = new MapList();
        try {
            for (int i = 0; i < typeList.size(); i++) {
                String strId = (String) typeList.get(i);
                DomainObject domProductObject = DomainObject.newInstance(context, strId);

                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_COLORCATALOG);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION);

                Pattern postTypePattern = new Pattern(TigerConstants.TYPE_PSS_COLOROPTION);
                Pattern postRelPattern = new Pattern(TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION);
                // Define a new MapList to return.

                String strObjectWhereCondition = DomainConstants.SELECT_CURRENT + "== \"" + STR_ACCEPTED_COLOR_STATE + "\" || " + DomainConstants.SELECT_CURRENT + "== \"" + STR_ACCEPTED_CATALOG_STATE
                        + "\"";
                MapList mlColor = (MapList) domProductObject.getRelatedObjects(context, relPattern.getPattern(), // Relationship
                        // Pattern
                        "*", // Object Pattern
                        slObjSelectStmts, // Object Selects
                        slRelSelectStmts, // Relationship Selects
                        false, // to direction
                        true, // From direction
                        (short) 0, // recursion level
                        strObjectWhereCondition, // object where clause
                        "", // Relationship where clause
                        (short) 0, // limit
                        postTypePattern, // Include type
                        postRelPattern, // Include Relationship
                        null // Include Map
                );
                Iterator<?> itr = mlColor.iterator();

                while (itr.hasNext()) {
                    newMap.add(itr.next());
                }
            }
            // Remove Duplicate Color Options
            HashSet<Map> hsUniqueColorOptions = new HashSet<Map>();
            hsUniqueColorOptions.addAll(newMap);
            newMap.clear();
            newMap.addAll(hsUniqueColorOptions);

            // Sorts the list of maps based on the given sort key name,
            // direction, and type
            newMap.sortStructure(DomainConstants.SELECT_NAME, "ascending", "string");

            Iterator<?> itr = newMap.iterator();
            while (itr.hasNext()) {
                Map<?, ?> mNewMap = (Map<?, ?>) itr.next();
                // TIGTK-10500:Start
                String strColumnName = (String) mNewMap.get(TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
                // TIGTK-10500:End
                String strColumnId = (String) mNewMap.get(DomainConstants.SELECT_ID);

                // create a column map to be returned.
                Map colMap = new HashMap();
                Map<String, String> settingsMap = new HashMap<String, String>();

                // Set information of Column Settings in settingsMap
                settingsMap.put("Column Type", "programHTMLOutput");

                settingsMap.put("program", "pss.mbom.table.UIUtil");
                settingsMap.put("function", "getCheckboxesOfColorForPart");
                settingsMap.put("Editable", "true");
                settingsMap.put("Sortable", "false");
                settingsMap.put("Width", "20");
                settingsMap.put("Registered Suite", "FRCMBOMCentral");

                // set column information
                colMap.put("name", "PSS_ColorOption|" + strColumnId);
                colMap.put("label", "<img src='../common/images/assignColors24x24.png' class='PSSRotateColumn' onload='parent.rotateText(this)'/>" + strColumnName + "");
                colMap.put("settings", settingsMap);
                mlColumnMapList.add(colMap);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getProductRelatedColorAsColumns: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw new FrameworkException(e);

        }
        return mlColumnMapList;
    }

    /**
     * If Part is connected to Color Options then show Image icon else blank.
     * @name displayColorMatrixTableIcon
     * @param context
     *            the Matrix Context
     * @param args
     *            contains-programMap --> objectList
     * @returns StringList
     * @throws Exception
     *             if the operation fails
     */
    public Vector<String> displayColorMatrixTableIcon(Context context, String args[]) throws Exception {
        Vector<String> vecReturnList = new Vector<String>();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> paramList = (Map<?, ?>) programMap.get("paramList");
            String objectId = (String) paramList.get("objectId");
            MapList mlObjectIdsList = (MapList) programMap.get("objectList");
            Map<?, ?> mTempMap;

            DomainObject domPartObject;
            String strPartId;
            StringList slPartConnectionToColorOptions;

            String strIconLink = DomainObject.EMPTY_STRING;

            for (int i = 0; i < mlObjectIdsList.size(); i++) {
                mTempMap = (Map<?, ?>) mlObjectIdsList.get(i);
                strPartId = (String) mTempMap.get("id");
                domPartObject = DomainObject.newInstance(context, strPartId);

                slPartConnectionToColorOptions = domPartObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id");
                if (slPartConnectionToColorOptions.size() > 0) {
                    strIconLink = "<img src='./images/assignColors16x16.png' alt='Colors' onclick=\"showModalDialog('../FRCMBOMCentral/PSS_FRCMBOMModifyColorOptionPreProcess.jsp?commandType=ColorOptionMatrix&amp;objectId="
                            + strPartId + "&amp;parentMBOMId=" + objectId + "',700,600)\"/>";
                } else {
                    strIconLink = "";
                }

                vecReturnList.addElement(strIconLink);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in displayColorMatrixTableIcon: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw new FrameworkException(e);
        }
        return vecReturnList;
    }

    /**
     * Render Column of Color Options.
     * @name getCheckboxesOfColorForPart
     * @param context
     *            the Matrix Context
     * @param args
     *            contains-programMap --> columnMap ,programMap --> objectList
     * @returns StringList
     * @throws Exception
     *             if the operation fails
     */
    public Vector<String> getCheckboxesOfColorForPart(Context context, String args[]) throws Exception {
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> columnMap = (HashMap<?, ?>) programMap.get("columnMap");
        Vector<String> vecColorOption = new Vector<String>();
        PLMCoreModelerSession plmSession = null;
        try {
            ContextUtil.startTransaction(context, false);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            MapList objectList = (MapList) programMap.get("objectList");

            String strColorOptionName = (String) columnMap.get("name");
            String strColorOptionId = strColorOptionName.split("\\|")[1];

            // Do for each object
            List<String> listRefIDs = new ArrayList<String>();
            for (Iterator itrObjects = objectList.iterator(); itrObjects.hasNext();) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String manuItemRefID = (String) mapObjectInfo.get("id");
                listRefIDs.add(manuItemRefID);
            }
            List<String> resScope = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, listRefIDs);

            DomainObject domColorOptionObject = DomainObject.newInstance(context, strColorOptionId);
            StringList slColorOptionPartConnection = domColorOptionObject.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].from.physicalid");
            for (int i = 0; i < objectList.size(); i++) {
                Map tempMap = (Map<?, ?>) objectList.get(i);
                String strPartId = (String) tempMap.get("id");
                String strPartLevel = (String) tempMap.get("level");
                String strLevelId = (String) tempMap.get("id[level]");
                String strConnectionId = (String) tempMap.get("id[connection]");
                DomainObject domPartObject = DomainObject.newInstance(context, strPartId);
                String strPartCurrentState = (String) domPartObject.getInfo(context, DomainConstants.SELECT_CURRENT);
                StringBuilder sbCheckbox = new StringBuilder();
                StringBuilder sbClassName = new StringBuilder();

                sbClassName.append(strPartId);
                sbClassName.append("|");
                sbClassName.append(strColorOptionId);
                sbClassName.append("|");
                sbClassName.append(strPartLevel);
                boolean enableCheckbox = false;
                if (STATES_INWORK.contains(strPartCurrentState)) {
                    if (UIUtil.isNotNullAndNotEmpty(resScope.get(i))) {
                        enableCheckbox = false;
                    } else {
                        List<String> implLinkList = FRCMBOMModelerUtility.getImplementLinkInfoSimple(context, plmSession, strConnectionId);
                        if (implLinkList != null && !implLinkList.isEmpty()) {
                            enableCheckbox = false;
                        } else {
                            enableCheckbox = true;
                        }
                    }
                }

                if (slColorOptionPartConnection.contains(strPartId)) {
                    if (enableCheckbox) {
                        sbCheckbox.append("<img src='./images/checked.png' imageName='checked' alt='checked' width='16px;' id='");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("|");
                        sbCheckbox.append(strConnectionId);
                        sbCheckbox.append("' class='");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("' onclick=\"parent.changeMatrixColImageForEditOptions('");
                        sbCheckbox.append(strLevelId);
                        sbCheckbox.append("','");
                        sbCheckbox.append(strColorOptionName);
                        sbCheckbox.append("','");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("|");
                        sbCheckbox.append(strConnectionId);
                        sbCheckbox.append("')\"/>");
                        vecColorOption.addElement(sbCheckbox.toString());
                    } else {
                        sbCheckbox.append("<img src='./images/checkeddisable.png' imageName='checked' alt='checked' width='16px;' id='");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("|");
                        sbCheckbox.append(strConnectionId);
                        sbCheckbox.append("' class='");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("' onclick=\"parent.changeMatrixColImageForEditOptions('");
                        sbCheckbox.append(strLevelId);
                        sbCheckbox.append("','");
                        sbCheckbox.append(strColorOptionName);
                        sbCheckbox.append("','");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("|");
                        sbCheckbox.append(strConnectionId);
                        sbCheckbox.append("')\"/>");
                        vecColorOption.addElement(sbCheckbox.toString());
                    }
                } else {
                    if (enableCheckbox) {
                        sbCheckbox.append("<img src='./images/unchecked.png' imageName='unchecked' alt='unchecked' width='16px;' id='");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("|");
                        sbCheckbox.append(strConnectionId);
                        sbCheckbox.append("' class='");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("' onclick=\"parent.changeMatrixColImageForEditOptions('");
                        sbCheckbox.append(strLevelId);
                        sbCheckbox.append("','");
                        sbCheckbox.append(strColorOptionName);
                        sbCheckbox.append("','");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("|");
                        sbCheckbox.append(strConnectionId);
                        sbCheckbox.append("')\"/>");
                        vecColorOption.addElement(sbCheckbox.toString());
                    } else {
                        sbCheckbox.append("<img src='./images/uncheckeddisable.png' imageName='disabled' alt='disabled' width='16px;' id='");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("|");
                        sbCheckbox.append(strConnectionId);
                        sbCheckbox.append("' class='");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("' onclick=\"parent.changeMatrixColImageForEditOptions('");
                        sbCheckbox.append(strLevelId);
                        sbCheckbox.append("','");
                        sbCheckbox.append(strColorOptionName);
                        sbCheckbox.append("','");
                        sbCheckbox.append(sbClassName.toString());
                        sbCheckbox.append("|");
                        sbCheckbox.append(strConnectionId);
                        sbCheckbox.append("')\"/>");
                        vecColorOption.addElement(sbCheckbox.toString());
                    }
                }
            }
            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            closeSession(plmSession);
            ContextUtil.abortTransaction(context);
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getCheckboxesOfColorForPart: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw e;

        }
        return vecColorOption;
    }

    public static void closeSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
        }
    }

    /**
     * This method is used for getting MBOM revisions
     * @name getMBOMrevisions
     * @param context
     *            the Matrix Context
     * @param args
     *            contains-paramMap --> object id
     * @returns StringList
     * @throws Exception
     *             if the operation fails
     */
    public StringList getMBOMrevisions(Context context, String[] args) throws Exception {
        // MapList mlMBOMList = new MapList();
        StringList slMBOMList = new StringList();
        try {
            HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strObjectID = (String) paramMap.get("objectId1");
            DomainObject domMBOMObjectId = DomainObject.newInstance(context, strObjectID);
            StringList revisionList = domMBOMObjectId.getInfoList(context, "majorids");
            Iterator<?> itr = revisionList.iterator();
            while (itr.hasNext()) {
                // Map mTempMap = (Map) itr.next();
                String steRevisionId = (String) itr.next();
                DomainObject dObj = DomainObject.newInstance(context, steRevisionId);
                slMBOMList.add(dObj.getInfo(context, DomainConstants.SELECT_ID));
            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getMBOMrevisions: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw new FrameworkException(e);
        }
        return slMBOMList;
    }

    /**
     * this method is used to hide/display the checkbox within the harmony object table,based on condition that.... the harmony is connected to the parent MBOM?
     * @param context
     * @param args
     * @return Vector of Boolean values
     * @throws Exception
     *             Exception appears, if error occured
     */
    public Vector getCheckboxStateForHarmonies(Context context, String args[]) throws Exception {

        int temp = 0;
        Vector value = new Vector();
        try {
            Map map = JPO.unpackArgs(args);
            MapList connectedHarmonyList = (MapList) map.get("objectList");
            Map paramMap = (Map) map.get("paramList");
            String strMBOMId = (String) paramMap.get("objectId");
            DomainObject domMBOM = DomainObject.newInstance(context, strMBOMId);
            String strMBOMName = domMBOM.getInfo(context, DomainConstants.SELECT_NAME);

            if (!(connectedHarmonyList.isEmpty())) {
                for (int j = 0; j < connectedHarmonyList.size(); j++) {
                    Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
                    typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
                    typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
                    typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);
                    typePattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);

                    Map harmonyMap = (Map) connectedHarmonyList.get(j);
                    String strHarmonyId = (String) harmonyMap.get(DomainConstants.SELECT_ID);
                    DomainObject domHarmony = DomainObject.newInstance(context, strHarmonyId);
                    MapList connectedMBOMList = domHarmony.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSSMBOM_HARMONIES, // relationship pattern
                            typePattern.getPattern(), // object pattern
                            new StringList(DomainConstants.SELECT_NAME), // object selects
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

                    if (connectedMBOMList.isEmpty()) {
                        value.add("true");
                    } else if (connectedMBOMList.size() == 1) {
                        Map mMBOMMap = (Map) connectedMBOMList.get(0);
                        String strMBOM = (String) mMBOMMap.get(DomainConstants.SELECT_NAME);
                        if (strMBOM.equals(strMBOMName)) {
                            value.add("false");
                        } else {
                            value.add("true");
                        }

                    } else {
                        for (int k = 0; k < connectedMBOMList.size(); k++) {

                            Map mMBOMMap = (Map) connectedMBOMList.get(k);
                            String strMBOM = (String) mMBOMMap.get(DomainConstants.SELECT_NAME);
                            if (strMBOM.equals(strMBOMName)) {
                                temp = 1;
                                break;
                            }

                        }
                        if (temp == 1) {
                            value.add("false");
                        } else {
                            value.add("true");
                        }

                    }

                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getCheckboxStateForHarmonies: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }

        return value;

    }

    /**
     * This method is used for checking Equipment Plant
     * @name getResourceColumnLinkWithPlantCheck
     * @param context
     *            the Matrix Context
     * @param args
     * @returns MapList
     * @throws Exception
     *             if the operation fails
     */
    public Vector getResourceColumnLinkWithPlantCheck(Context context, String[] args) throws Exception {

        String strHoverMessage = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(), "PSS_FRCMBOMCentral.HoverMessage");
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> paramList = (HashMap<?, ?>) programMap.get("paramList");
        MapList objectList = (MapList) programMap.get("objectList");
        String strMBOMId = (String) paramList.get("objectId");
        String strEquipmentId = null;
        Vector<String> result = new Vector<String>();
        PLMCoreModelerSession plmSession = null;
        ContextUtil.startTransaction(context, false);
        try {
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            List<String> lMBOMPlants = getPlantsAttachedToMBOMReference(context, plmSession, strMBOMId);

            Iterator itrEQId = objectList.iterator();
            while (itrEQId.hasNext()) {
                StringBuilder htmlString = new StringBuilder();
                Map mEquipmentMap = (Map) itrEQId.next();
                strEquipmentId = (String) mEquipmentMap.get("id");
                DomainObject domEquipmentObj = new DomainObject(strEquipmentId);
                StringList slTempEquipmentPlantIds = domEquipmentObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_ASSOCIATED_PLANT + "].to.physicalid");
                boolean plantCommon = false;
                if (slTempEquipmentPlantIds.isEmpty()) {
                    plantCommon = true;
                }
                for (int j = 0; j < slTempEquipmentPlantIds.size(); j++) {
                    String strEQPlantID = (String) slTempEquipmentPlantIds.get(j);
                    if (lMBOMPlants.contains(strEQPlantID)) {
                        plantCommon = true;
                    }
                }
                String strEQName = domEquipmentObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
                if (UIUtil.isNullOrEmpty(strEQName)) {
                    strEQName = domEquipmentObj.getInfo(context, DomainConstants.SELECT_NAME);
                }
                htmlString.append("<div");
                htmlString.append((plantCommon) ? ">" : " style=\"background-color:red \" title='" + strHoverMessage + "'>");
                htmlString.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxPortal.jsp?portal=PSS_FRCMBOMResourcePortal").append("&amp;objectId=");
                htmlString.append(strEquipmentId);
                htmlString.append("', '860', '520', 'false', 'popup')\">");
                htmlString.append(strEQName);
                htmlString.append("</a> ");
                htmlString.append("</div>");
                result.add(htmlString.toString());
            }

            closeSession(plmSession);
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getResourceColumnLinkWithPlantCheck: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return result;
    }

    /* This method is added for getResourceColumnLinkWithPlantCheck */
    // Plants - NOTE : Faurecia will implement their own links to Plants,
    // directly in ${CLASS:FRCMBOMProg}, in replacement of these methods.
    public static List<String> getPlantsAttachedToMBOMReference(Context context, PLMCoreModelerSession plmSession, String instPID) throws Exception {
        // No refactoring to do : R&D will not provide an API, because Faurecia
        // will use ENOVIA plant objects, with their own links.

        List<String> returnList = new ArrayList<String>();
        final String REL_VOWNER = "VPLMrel/PLMConnection/V_Owner";
        StringList lRsc = new StringList();

        String sAssemblyPID = DomainObject.EMPTY_STRING;
        try {
            if (UIUtil.isNotNullAndNotEmpty(instPID))
                sAssemblyPID = MqlUtil.mqlCommand(context, "print bus " + instPID + " select physicalid dump |", false, false);
            if (UIUtil.isNotNullAndNotEmpty(sAssemblyPID)) {
                String listPathIDStr = MqlUtil.mqlCommand(context, "query path type SemanticRelation containing " + sAssemblyPID + " select id dump |", false, false);
                if (!"".equals(listPathIDStr)) {
                    String[] listPathID = listPathIDStr.split("\n");
                    for (String pathDesc : listPathID) {
                        String[] aPathDesc = pathDesc.split("\\|");
                        if (null != aPathDesc && 1 < aPathDesc.length) {
                            String pathID = aPathDesc[1];

                            String pathSemantics = MqlUtil.mqlCommand(context, "print path " + pathID + " select attribute[RoleSemantics].value dump |", false, false);
                            if ("PLM_ImplementLink_TargetReference3".equals(pathSemantics)) {
                                String targetPhysId = MqlUtil.mqlCommand(context, "print path " + pathID + " select owner.to[" + REL_VOWNER + "].from.physicalid dump |", false, false);
                                if (null != targetPhysId && !"".equals(targetPhysId))
                                    lRsc.addElement(targetPhysId);
                            }
                        }
                    }
                }
            }

            for (Object rscPIDObj : lRsc) {
                returnList.add((String) rscPIDObj);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getPlantsAttachedToMBOMReference: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw new FrameworkException(e);
        }

        return returnList;
    }

    public Vector<String> displayLinkedItems(Context context, String[] args) throws Exception {
        Vector result = new Vector();

        String strMBOMName;
        String objectType = DomainObject.EMPTY_STRING;
        try {

            Map programMap = JPO.unpackArgs(args);

            MapList mObjectList = (MapList) programMap.get("objectList");

            for (int i = 0; i < mObjectList.size(); i++) {
                int flag = 0;
                StringBuilder strBuild = new StringBuilder();
                Map mMBOMMap = (Map) mObjectList.get(i);
                StringList strMBOMList = (StringList) mMBOMMap.get("linkedItems");
                for (int j = 0; j < strMBOMList.size(); j++) {
                    String strMBOMId = (String) strMBOMList.get(j);
                    DomainObject domMBOMObj = new DomainObject(strMBOMId);
                    strMBOMName = domMBOMObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME);
                    // StringBuffer resultSB = new StringBuffer();
                    objectType = domMBOMObj.getInfo(context, DomainConstants.SELECT_TYPE);

                    // strBuild.append(strMBOMName);
                    if (flag == 0) {
                        strBuild.append(genObjHTML(context, strMBOMId, objectType, strMBOMName, false, false));
                        flag = 1;
                    }

                    else {
                        strBuild.append(",");
                        strBuild.append(genObjHTML(context, strMBOMId, objectType, strMBOMName, false, false));

                    }

                }

                result.add(strBuild.toString());

            }

        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in displayLinkedItems: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return result;
    }

    public static String genObjHTML(Context context, String objID, String objType, String objDisplayStr, boolean bold, boolean italic) throws Exception {
        String attIcon = UINavigatorUtil.getTypeIconProperty(context, objType);

        StringBuffer anchorStr = new StringBuffer();
        anchorStr.append("<a TITLE=");
        anchorStr.append("\"" + objDisplayStr + "\"");

        anchorStr.append(" href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?emxSuiteDirectory=common&amp;parentOID=null&amp;jsTreeID=null&amp;suiteKey=Framework&amp;objectId=");

        anchorStr.append(objID);
        if (bold)
            anchorStr.append("', '', '', 'false', 'popup', '')\" class=\"object\">");
        else
            anchorStr.append("', '', '', 'false', 'popup', '')\">");

        StringBuffer returnStr = new StringBuffer();
        returnStr.append("<b></b>");
        returnStr.append(anchorStr.toString());
        returnStr.append("<div class=\"typeIconDiv\" style=\"position:relative;display:inline-block;margin-right:4px;\">");// UM5 : I add this div to be able to add the scope icon in the corner on the
        // JS side
        // returnStr.append("<img border=\"0\" style=\"height:16px;\" src=\"../common/images/" + attIcon + "\"/>");
        returnStr.append("<img border=\"0\" src=\"../common/images/" + attIcon + "\"/>");
        // typeIconDivSEBadge
        // returnStr.append("<div class=\"typeIconDivSEBadge\" style=\"position:absolute;display:block;bottom:0px;right:0px;width:8px;height:8px;\"></div>");//UM5 : I add this div to be able to add
        // the scope icon in the corner on the JS side
        returnStr.append("<div class=\"typeIconDivSEBadge\" style=\"position:absolute;display:block;bottom:0px;right:0px;width:11px;height:11px;\"></div>");// UM5 : I add this div to be able to add
        // the scope icon in the corner on the JS
        // side
        returnStr.append("</div>");
        returnStr.append("</a>");
        returnStr.append(anchorStr.toString());
        if (bold)
            returnStr.append("<b>");
        if (italic)
            returnStr.append("<i>");
        returnStr.append(objDisplayStr);
        if (italic)
            returnStr.append("</i>");
        if (bold)
            returnStr.append("</b>");
        returnStr.append("</a>");

        return returnStr.toString();
    }

    public Vector getOptionsAvailable(Context context, String[] args) throws Exception {

        Vector value = new Vector();
        int temp = 0;
        String strRelId = DomainObject.EMPTY_STRING;
        try {
            Map map = JPO.unpackArgs(args);
            MapList connectedHarmonyList = (MapList) map.get("objectList");
            Map paramMap = (Map) map.get("paramList");
            String strMBOMId = (String) paramMap.get("objectId");
            DomainObject domMBOM = DomainObject.newInstance(context, strMBOMId);
            String strMBOMName = domMBOM.getInfo(context, DomainConstants.SELECT_NAME);

            if (!(connectedHarmonyList.isEmpty())) {
                for (int j = 0; j < connectedHarmonyList.size(); j++) {
                    StringBuilder sbHTML = new StringBuilder();
                    Pattern typePattern = new Pattern(TigerConstants.TYPE_CREATEASSEMBLY);
                    typePattern.addPattern(TigerConstants.TYPE_CREATEKIT);
                    typePattern.addPattern(TigerConstants.TYPE_CREATEMATERIAL);
                    typePattern.addPattern(TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE);
                    typePattern.addPattern(TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL);

                    Map harmonyMap = (Map) connectedHarmonyList.get(j);
                    String strHarmonyId = (String) harmonyMap.get(DomainConstants.SELECT_ID);

                    DomainObject domHarmony = DomainObject.newInstance(context, strHarmonyId);
                    MapList connectedMBOMList = domHarmony.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSSMBOM_HARMONIES, // relationship pattern
                            typePattern.getPattern(), // object pattern
                            new StringList(DomainConstants.SELECT_NAME), // object selects
                            new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 1, // recursion level
                            null, // object where clause
                            null, (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, // Postpattern
                            null, null, null);

                    if (connectedMBOMList.size() == 1) {

                        Map mMBOMMap = (Map) connectedMBOMList.get(0);
                        String strMBOM = (String) mMBOMMap.get(DomainConstants.SELECT_NAME);
                        strRelId = (String) mMBOMMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                        if (strMBOM.equals(strMBOMName)) {
                            sbHTML.append("<a href=\"JavaScript:emxTableColumnLinkClick('../FRCMBOMCentral/PSS_FRCMBOMHarmonyAddExistingProcess.jsp?").append("RelId=").append(strRelId)
                                    .append("&amp;pssMode=remove").append("','700','600','true','listHidden','").append("')\">")
                                    .append("<img border=\"0\" src=\"../common/images/buttonMiniDeleteParam.gif\" name=\"Remove\" id=\"Remove\" alt=\"myimage\"  title=\"Remove\"></img></a>");
                            value.add(sbHTML.toString());
                        } else {
                            value.add(" ");
                        }
                    } else {
                        if (!(connectedMBOMList.isEmpty())) {
                            for (int k = 0; k < connectedMBOMList.size(); k++) {

                                Map mMBOMMap = (Map) connectedMBOMList.get(k);
                                String strMBOM = (String) mMBOMMap.get(DomainConstants.SELECT_NAME);
                                strRelId = (String) mMBOMMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                                if (strMBOM.equals(strMBOMName)) {
                                    temp = 1;
                                    break;
                                }

                            }
                            if (temp == 1) {
                                sbHTML.append("<a href=\"JavaScript:emxTableColumnLinkClick('../FRCMBOMCentral/PSS_FRCMBOMHarmonyAddExistingProcess.jsp?").append("RelId=").append(strRelId)
                                        .append("&amp;pssMode=remove").append("','700','600','true','listHidden','").append("')\">")
                                        .append("<img border=\"0\" src=\"../common/images/buttonMiniDeleteParam.gif\" name=\"Remove\" id=\"Remove\" alt=\"myimage\"  title=\"Remove\"></img></a>");
                                value.add(sbHTML.toString());
                            } else {
                                value.add(" ");
                            }
                        } else {
                            value.add(" ");
                        }

                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getOptionsAvailable: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }

        return value;

    }

    public MapList getProgramsfromMBOM(Context context, String[] args) throws Exception {
        MapList mList = new MapList();
        try {
            Map programMap = JPO.unpackArgs(args);
            String strMBOMObjectId = (String) programMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strMBOMObjectId)) {
                mList = pss.mbom.MBOMUtil_mxJPO.getProgramFromMBOM(context, strMBOMObjectId);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getProgramsfromMBOM: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END

        }
        return mList;
    }

    /**
     * This method is used to display Harmony of MBOM on the Harmony Association Table
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector getHarmonyDisplayName(Context context, String[] args) throws Exception {

        Vector result = new Vector();
        try {
            Map programMap = JPO.unpackArgs(args);

            MapList ObjectList = (MapList) programMap.get("objectList");
            if (ObjectList != null && !(ObjectList.isEmpty())) {
                for (int i = 0; i < ObjectList.size(); i++) {
                    Map mObjectMap = (Map) ObjectList.get(i);
                    String strObjectId = (String) mObjectMap.get(DomainConstants.SELECT_ID);

                    DomainObject domObject = new DomainObject(strObjectId);
                    String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);

                    if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_HARMONY)) {
                        String strName = domObject.getInfo(context, DomainConstants.SELECT_NAME);
                        result.add(strName);
                    } else {
                        result.add("");
                    }

                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getHarmonyDisplayName: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return result;
    }

    /**
     * This method is used to display Color of MBOM on the Harmony Association Table
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getColorCodeHTML(Context context, String[] args) throws Exception {

        Vector<String> result = new Vector();
        int index = 0;
        String name = "PSS_Color";
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            // TIGTK-7252:Rutuja Ekatpure:21/7/2017:Start
            Map paramList = (Map) programMap.get("paramList");
            String pcID = (String) paramList.get("FRCAppliedPCField");
            String strFilterApplied = "No";

            if (UIUtil.isNotNullAndNotEmpty(pcID) && !pcID.equals("-none-")) {
                strFilterApplied = "Yes";
            }
            // TIGTK-7252:Rutuja Ekatpure:21/7/2017:End
            if (objectList != null && !(objectList.isEmpty())) {
                for (int i = 0; i < objectList.size(); i++) {
                    Map mObjectMap = (Map) objectList.get(i);
                    DomainObject domObject = new DomainObject((String) mObjectMap.get(DomainConstants.SELECT_ID));
                    String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
                    if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_HARMONY)) {
                        String strColorable = (String) mObjectMap.get("Colorable");
                        String strPhantomLevel = (String) mObjectMap.get(TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_LEVEL);
                        String strPhantomPart = (String) mObjectMap.get(TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_PART);
                        String parentType = (String) mObjectMap.get("ParentType");

                        if (UIUtil.isNullOrEmpty(parentType)) {
                            parentType = "";
                        }
                        String strRelId = (String) mObjectMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        DomainRelationship domRel = DomainRelationship.newInstance(context, strRelId);
                        if ((strPhantomLevel.equalsIgnoreCase("Yes") && !parentType.equals(TigerConstants.TYPE_CREATEMATERIAL))
                                || (strPhantomPart.equalsIgnoreCase("Yes") && parentType.equals(TigerConstants.TYPE_CREATEMATERIAL))) {
                            if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
                                // PropertyUtil.setRPEValue(context, HARMONY_REL_ID,strRelId , true);
                                domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, "Ignore");
                            }
                            result.add("Ignore");
                        } else {
                            String colorPID = DomainRelationship.getAttributeValue(context, strRelId, TigerConstants.ATTRIBUTE_PSS_COLORPID);
                            if (colorPID == null) {
                                colorPID = "";
                            }
                            // TIGTK-7251:Rutuja Ekatpure:20/7/2017:Start
                            String strMaterialType = DomainConstants.EMPTY_STRING;
                            Boolean isSpecificMaterial = false;
                            String strParentId = (String) mObjectMap.get("id[parent]");
                            DomainObject domParentObject = new DomainObject(strParentId);

                            if (domParentObject.isKindOf(context, TigerConstants.TYPE_PROCESSCONTINUOUSPROVIDE)
                                    || domParentObject.isKindOf(context, TigerConstants.TYPE_PROCESS_CONTINUOUS_CREATE_MATERIAL)) {
                                strMaterialType = domParentObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROCESSCONTINUOUSPROVIDE_PSS_MATERIALTYPE);
                                if (UIUtil.isNullOrEmpty(strColorable) && "Specific".equalsIgnoreCase(strMaterialType)) {
                                    isSpecificMaterial = true;
                                }
                            }
                            boolean bIsGenericMaterial = "Generic".equalsIgnoreCase(strMaterialType);

                            // TIGTK-7251:Rutuja Ekatpure:20/7/2017:End
                            StringList strColorList = (StringList) mObjectMap.get("PSS_ColorList");
                            if (strColorList != null && !(strColorList.isEmpty()) && !bIsGenericMaterial) {
                                boolean selected = false;
                                StringBuilder csb = new StringBuilder();
                                for (int j = 0; j < strColorList.size(); j++) {
                                    String colorPhyId = (String) strColorList.get(j);
                                    if (UIUtil.isNotNullAndNotEmpty(colorPhyId)) {
                                        DomainObject domColorOption = DomainObject.newInstance(context, colorPhyId);
                                        String strColorCode = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORCODE);

                                        String strDisplayName = domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_DISPLAYNAME);
                                        csb.append("<option value='" + strColorList.get(j) + "'");
                                        if (colorPhyId.equals(colorPID)) {
                                            selected = true;
                                            csb.append(" selected='selected' ");
                                        } // TIGTK-7252:Rutuja Ekatpure:21/7/2017:Start
                                        else if ((strMaterialType.equalsIgnoreCase("Specific")) && (strFilterApplied.equalsIgnoreCase("Yes"))) {
                                            selected = true;
                                            csb.append(" selected='selected' ");
                                            domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, strColorCode + "(" + strDisplayName + ")");
                                        }
                                        // TIGTK-7252:Rutuja Ekatpure:21/7/2017:End
                                        csb.append(">" + strColorCode + "(" + strDisplayName.replace("&", "") + ")" + "</option>\n");
                                    }
                                }

                                if (!selected && colorPID.equals("Ignore")) {
                                    csb.append("<option value='Ignore' selected='selected' >" + "Ignore" + "</option>\n");
                                    domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, "Ignore");
                                    selected = true;
                                } else {
                                    csb.append("<option value='Ignore'>" + "Ignore" + "</option>\n");
                                }
                                // TIGTK-11481:Rutuja Ekatpure:24/11/2017:Start
                                StringBuilder sb = new StringBuilder();
                                sb.append("<select  id = 'PSS_Color" + index + "' onChange='updateHarmonyFieldValue(this,\"" + name + "\")' name='" + "ColorBox" + "'>\n");
                                if (!isSpecificMaterial) {
                                    if (!selected && colorPID.equals("N/A")) {
                                        csb.append("<option value='N/A' selected='selected' >" + "N/A" + "</option>\n");
                                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, "N/A");
                                        selected = true;
                                    } // TIGTK-10503 :Start
                                    else if (!selected && (UIUtil.isNullOrEmpty(strColorable) || strColorable.equalsIgnoreCase("No"))) {
                                        csb.append("<option value='N/A' selected='selected' >" + "N/A" + "</option>\n");
                                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, "N/A");
                                        selected = true;
                                    } // TIGTK-10503 :End
                                    else {
                                        csb.append("<option value='N/A'>" + "N/A" + "</option>\n");
                                    }
                                }
                                if (!selected) {
                                    domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, "");
                                    sb.append("<option value='empty' selected='selected' ></option>\n");
                                } else {
                                    sb.append("<option value='empty'></option>\n");
                                }

                                sb.append(csb.toString());
                                sb.append("</select>\n");
                                // TIGTK-11481:Rutuja Ekatpure:24/11/2017:End
                                result.add(sb.toString());

                            } else {
                                boolean selected = false;
                                StringBuilder csb = new StringBuilder();
                                // TIGTK-7251:Rutuja Ekatpure:20/7/2017:Start
                                if (!selected && colorPID.equals("Ignore")) {
                                    csb.append("<option value='Ignore' selected='selected' >" + "Ignore" + "</option>\n");
                                    domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, "Ignore");
                                    selected = true;

                                } else {
                                    csb.append("<option value='Ignore'>" + "Ignore" + "</option>\n");
                                }

                                if (!selected && colorPID.equals("N/A") || bIsGenericMaterial) {
                                    csb.append("<option value='N/A' selected='selected' >" + "N/A" + "</option>\n");
                                    selected = true;
                                    if (bIsGenericMaterial) {
                                        domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, "N/A");
                                    } // TIGTK-7251:Rutuja Ekatpure:20/7/2017:End
                                } // TIGTK-10503 :Start
                                else if (!selected && (UIUtil.isNullOrEmpty(strColorable) || strColorable.equalsIgnoreCase("No"))) {
                                    csb.append("<option value='N/A' selected='selected' >" + "N/A" + "</option>\n");
                                    domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, "N/A");
                                    selected = true;
                                } // TIGTK-10503 :End
                                else {
                                    csb.append("<option value='N/A'>" + "N/A" + "</option>\n");
                                }

                                StringBuilder sb = new StringBuilder();
                                sb.append("<select  id = 'PSS_Color" + index + "' onChange='updateHarmonyFieldValue(this,\"" + name + "\")' name='" + "ColorBox" + "'>\n");
                                if (!selected) {
                                    domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, "");
                                    sb.append("<option value='empty' selected='selected' ></option>\n");
                                } else {// TIGTK-7251:Rutuja Ekatpure:20/7/2017:Start
                                    if (!bIsGenericMaterial) {
                                        sb.append("<option value='empty'></option>\n");
                                    }
                                    // TIGTK-7251:Rutuja Ekatpure:20/7/2017:End
                                }
                                sb.append(csb.toString());
                                sb.append("</select>\n");

                                result.add(sb.toString());
                            }
                        }

                    } else {
                        result.add("");
                    }
                    index++;
                }
            }
        } catch (

        Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getColorCodeHTML: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return result;
    }

    /**
     * This method is used to display Customer Part Number on the Harmony Association table
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getCustomerPartNumberHTML(Context context, String[] args) throws Exception {

        Vector<String> result = new Vector();
        String name = "PSS_CustomerPartNumber";
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            if (objectList != null && !(objectList.isEmpty())) {
                for (int i = 0; i < objectList.size(); i++) {

                    Map mObjectMap = (Map) objectList.get(i);
                    DomainObject domObject = new DomainObject((String) mObjectMap.get(DomainConstants.SELECT_ID));
                    String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
                    if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_HARMONY)) {
                        String strPhantomLevel = (String) mObjectMap.get(TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_LEVEL);
                        String strPhantomPart = (String) mObjectMap.get(TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_INSTANCEEXT_PSS_PHANTOM_PART);
                        String parentType = (String) mObjectMap.get("ParentType");
                        if (UIUtil.isNullOrEmpty(parentType)) {
                            parentType = "";
                        }

                        String strRelId = (String) mObjectMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        if ((strPhantomLevel.equalsIgnoreCase("Yes") && !parentType.equals(TigerConstants.TYPE_CREATEMATERIAL))
                                || (strPhantomPart.equalsIgnoreCase("Yes") && parentType.equals(TigerConstants.TYPE_CREATEMATERIAL))) {
                            if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
                                DomainRelationship domRel = DomainRelationship.newInstance(context, strRelId);
                                // PropertyUtil.setRPEValue(context, HARMONY_REL_ID,strRelId , true);
                                domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER, "");
                                domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OLD_MATERIAL_NUMBER, "");
                                domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_QUANTITY, "0.0");
                            }
                            result.add("");
                        } else {
                            Object objCUstomerPartNumber = mObjectMap.get("PSS_CustomerPartNumberList");
                            StringList slCustomerPartNumber = new StringList();
                            if (objCUstomerPartNumber != null && objCUstomerPartNumber instanceof StringList) {
                                slCustomerPartNumber = (StringList) objCUstomerPartNumber;
                            } else if (objCUstomerPartNumber != null) {
                                slCustomerPartNumber.add((String) objCUstomerPartNumber);
                            }

                            String customerPartNumberAttr = DomainRelationship.getAttributeValue(context, strRelId, TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER);
                            if (customerPartNumberAttr == null) {
                                customerPartNumberAttr = "";
                            }

                            if (!(slCustomerPartNumber.isEmpty())) {
                                StringBuilder csb = new StringBuilder("");
                                boolean selected = false;
                                for (int j = 0; j < slCustomerPartNumber.size(); j++) {
                                    String custometPartNumber = ((String) slCustomerPartNumber.get(j)).trim();

                                    if (customerPartNumberAttr.equals(custometPartNumber)) {
                                        csb.append("<option value='" + custometPartNumber + "' selected='selected'>" + custometPartNumber + "</option>\n");
                                        selected = true;
                                    } else {
                                        csb.append("<option value='" + custometPartNumber + "'>" + custometPartNumber + "</option>\n");
                                    }
                                }
                                StringBuilder sb = new StringBuilder("");
                                sb.append("<select  id = 'PSS_CustomerPartNumber" + i + "' onChange='updateHarmonyFieldValue(this,\"" + name + "\")' name='CustomerPartNumber'>\n");
                                if (!selected) {
                                    sb.append("<option value=' ' selected='selected'></option>\n");
                                } else {
                                    sb.append("<option value=' '></option>\n");
                                }
                                sb.append(csb.toString());
                                sb.append("</select>\n");
                                result.add(sb.toString());
                            } else {
                                StringBuilder sb = new StringBuilder("");
                                sb.append("<select  id = 'PSS_CustomerPartNumber" + i + "' onChange='updateHarmonyFieldValue(this,\"" + name + "\")' name='CustomerPartNumber'>\n");
                                sb.append("<option value=' ' selected='selected'></option>\n");
                                sb.append("</select>\n");
                                result.add(sb.toString());
                            }
                        }
                    } else {
                        result.add("");
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getCustomerPartNumberHTML: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return result;
    }

    /**
     * This method is used to update Color ID on the Harmony Association relationship
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void updateColorPID(Context context, String[] args) throws Exception {

        try {
            Map mprogramMap = JPO.unpackArgs(args);
            Map paramMap = (Map) mprogramMap.get("paramMap");
            String selectedValue = (String) paramMap.get("New Value");

            String strRelId = (String) paramMap.get("relId");
            if (UIUtil.isNotNullAndNotEmpty(selectedValue)) {
                if (selectedValue.equals("empty")) {
                    selectedValue = "";
                }
                DomainRelationship domRel = new DomainRelationship(strRelId);
                domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COLORPID, selectedValue);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in updateColorPID: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
    }

    /**
     * This method is used to update Customer Part Number on the Harmony Association relationship
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void updateCustomerPartNumber(Context context, String[] args) throws Exception {

        try {
            Map mprogramMap = JPO.unpackArgs(args);
            Map paramMap = (Map) mprogramMap.get("paramMap");
            String selectedValue = (String) paramMap.get("New Value");

            String strRelId = (String) paramMap.get("relId");
            if (UIUtil.isNotNullAndNotEmpty(strRelId)) {
                DomainRelationship domRel = new DomainRelationship(strRelId);
                domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CUSTOMERPARTNUMBER, selectedValue);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in updateCustomerPartNumber: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
    }

    /**
     * This method is used display Unit of Measure of MBOM in the Harmony Association Table
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector getUnitOfMeasure(Context context, String[] args) throws Exception {

        Vector result = new Vector();
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            if (objectList != null && !(objectList.isEmpty())) {
                for (int i = 0; i < objectList.size(); i++) {

                    Map mObjectMap = (Map) objectList.get(i);
                    DomainObject domObject = new DomainObject((String) mObjectMap.get(DomainConstants.SELECT_ID));
                    String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
                    if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_HARMONY)) {
                        result.add((String) mObjectMap.get("PSS_UnitOfMeasure"));

                    } else {
                        result.add("");
                    }

                }
            } else {
                result.add("");
            }

        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getUnitOfMeasure: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return result;
    }

    /**
     * This method is used display Unit of Measure category of MBOM in the Harmony Association Table
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector getUnitofMeasureCategory(Context context, String[] args) throws Exception {
        Vector result = new Vector();
        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            if (objectList != null && !(objectList.isEmpty())) {
                for (int i = 0; i < objectList.size(); i++) {

                    Map mObjectMap = (Map) objectList.get(i);
                    DomainObject domObject = new DomainObject((String) mObjectMap.get(DomainConstants.SELECT_ID));
                    String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
                    if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_HARMONY)) {
                        result.add((String) mObjectMap.get("PSS_UnitOfMeasureCategory"));

                    } else {
                        result.add("");
                    }

                }
            } else {
                result.add("");
            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getUnitofMeasureCategory: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return result;
    }

    /**
     * This method is used display Unit of Measure category of MBOM in the Harmony Association Table
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector getQuantity(Context context, String[] args) throws Exception {
        Vector result = new Vector();

        try {
            Map programMap = JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            if (objectList != null && !(objectList.isEmpty())) {
                for (int i = 0; i < objectList.size(); i++) {

                    Map mObjectMap = (Map) objectList.get(i);
                    DomainObject domObject = new DomainObject((String) mObjectMap.get(DomainConstants.SELECT_ID));
                    String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);
                    if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_HARMONY)) {
                        result.add((String) mObjectMap.get("Quantity"));

                    } else {
                        result.add("");
                    }

                }
            } else {
                result.add("");
            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getQuantity: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return result;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static MapList getSpareManufacturingItems(Context context, String[] args) throws Exception {

        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        MapList mapRelatedItems = new MapList();
        try {

            String strPartObjectId = (String) programMap.get("objectId");
            DomainObject dom = new DomainObject(strPartObjectId);
            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_ID);
            mapRelatedItems = dom.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM, "*", objectSelects, DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, "",
                    "", 0);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getSpareManufacturingItems: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }

        return mapRelatedItems;

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector getSpareAvailableColumn(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");

        Vector vecReturn = null;
        StringBuffer sb = null;
        try {
            vecReturn = new Vector(objectList.size());
            Map map = null;
            String strObjID = DomainObject.EMPTY_STRING;
            String strRelID = DomainObject.EMPTY_STRING;
            // String stateInwork = FrameworkUtil.lookupStateName(context, TigerConstants.POLICY_PSS_MBOM, "state_InWork");
            String strTreeLink = DomainObject.EMPTY_STRING;
            Iterator objectItr = objectList.iterator();

            while (objectItr.hasNext()) {
                map = (Map) objectItr.next();
                sb = new StringBuffer(500);
                strObjID = (String) map.get("id");
                strRelID = (String) map.get("id[connection]");

                if (!UIUtil.isNullOrEmpty(strObjID)) {
                    DomainObject domObj = new DomainObject(strObjID);
                    StringList strlstRelsid = domObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM + "].to.id");
                    String current = domObj.getInfo(context, DomainConstants.SELECT_CURRENT);

                    if (strlstRelsid == null || strlstRelsid.isEmpty()) {

                        // TIGTK-9774 : START
                        if (!(current.equalsIgnoreCase(TigerConstants.STATE_MBOM_RELEASED)) && !(current.equalsIgnoreCase(TigerConstants.STATE_MBOM_OBSOLETE))) {
                            // TIGTK-9774 : START
                            strTreeLink = "<a class=\"object\" href=\"JavaScript:emxTableColumnLinkClick('../common/emxFullSearch.jsp?excludeOIDprogram= pss.mbom.table.UIUtil:excludeConnectedSpareMBOM&amp;objectId="
                                    + XSSUtil.encodeForHTMLAttribute(context, strObjID) + "&amp;relId=" + XSSUtil.encodeForHTMLAttribute(context, strRelID)
                                    + "&amp;table=PSS_FRCMBOMListTable&amp;field=TYPES=CreateAssembly,CreateKit,CreateMaterial:PSS_SPARE=Yes&amp;header=PSS_FRCMBOMCentral.Header.SpareMBOM&amp;selection=multiple&amp;submitAction=refreshCaller&amp;hideHeader=true&amp;submitURL=../FRCMBOMCentral/PSS_SpareMBOMProcess.jsp?fromContext=NoLink&amp;checkAction=No&amp;customAction=add', '800', '575','true','popup')\">No</a>";
                        } else {

                            strTreeLink = "<a class=\"object\" >No</a>";
                        }
                    } else {
                        strTreeLink = "<a class=\"object\" href=\"JavaScript:emxTableColumnLinkClick('../common/emxIndentedTable.jsp?objectId=" + XSSUtil.encodeForHTMLAttribute(context, strObjID)
                                + "&amp;relId=" + XSSUtil.encodeForHTMLAttribute(context, strRelID)
                                + "&amp;table=PSS_FRCMBOMListTable&amp;program=pss.mbom.table.UIUtil:getSpareManufacturingItems&amp;header=PSS_FRCMBOMCentral.Header.SpareMBOM&amp;suiteKey=FRCMBOMCentral&amp;StringResourceFileId=emxFRCMBOMCentralStringResources&amp;SuiteDirectory=FRCMBOMCentral&amp;toolbar=PSS_FRCMBOMSpareToolbar&amp;selection=multiple', '800', '575','true','popup')\">Yes</a>";
                    }
                    sb.append(strTreeLink);

                } else
                    sb.append("");

                vecReturn.addElement(sb.toString());

            }

        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getSpareAvailableColumn: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            throw new FrameworkException(e);
        }

        return vecReturn;

    }

    /**
     * @param context
     * @param args
     */
    public void addSpareMBOM(Context context, String[] args) {
        HashMap<?, ?> programMap;
        try {

            programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strPartObjectId = (String) programMap.get("objectId");
            String[] relArr = (String[]) programMap.get("objectIdListOfSelectedObjects");
            StringList attrib = new StringList();

            for (int i = 0; i < relArr.length; i++) {// length is the property of array
                String val = relArr[i];

                attrib.add((String) FrameworkUtil.split(val, "|").get(0));
            }
            // String[] relArr = (String[]) lstObjectstobeconnected.toArray();
            RelationshipType rel = new RelationshipType(TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM);
            DomainObject domObje = new DomainObject(strPartObjectId);
            String[] attribarray = new String[attrib.size()];
            attribarray = (String[]) attrib.toArray(attribarray);
            domObje.addRelatedObjects(context, rel, true, attribarray);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in addSpareMBOM: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }

    }

    /**
     * @param context
     * @param args
     */
    public void removeSpareMBOM(Context context, String[] args) {
        HashMap<?, ?> programMap;
        try {

            programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            StringList strPartObjectId = (StringList) programMap.get("relationshipIdList");
            String[] relArr = new String[strPartObjectId.size()];

            relArr = (String[]) strPartObjectId.toArray(relArr);
            DomainRelationship.disconnect(context, relArr);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in removeSpareMBOM: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList excludeConnectedObjects(Context context, String[] args) throws Exception {

        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        StringList connetcedSpareMBOMIds = new StringList();
        try {

            String strPartObjectId = (String) programMap.get("objectId");
            DomainObject dom = new DomainObject(strPartObjectId);
            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_ID);
            MapList mapRelatedItems = dom.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM, "*", objectSelects, DomainConstants.EMPTY_STRINGLIST, false, true,
                    (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            if (mapRelatedItems != null && !mapRelatedItems.isEmpty()) {
                Iterator<?> itr = mapRelatedItems.iterator();

                while (itr.hasNext()) {
                    Map<?, ?> mTempMap = (Map<?, ?>) itr.next();

                    connetcedSpareMBOMIds.add((String) mTempMap.get(DomainConstants.SELECT_ID));
                }

            }
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in excludeConnectedObjects: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return connetcedSpareMBOMIds;

    }

    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeConnectedSpareMBOM(Context context, String[] args) throws Exception {
        Map programMap = (Map) JPO.unpackArgs(args);
        StringList excludeList = new StringList();
        try {
            String strObjectIds = (String) programMap.get("objectId");
            DomainObject domObjFeature = new DomainObject(strObjectIds);

            excludeList = domObjFeature.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_SPAREMANUFACTURINGITEM + "].to.id");
            excludeList.add(strObjectIds);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in excludeConnectedSpareMBOM: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return excludeList;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector getAlternateAvailableColumn(Context context, String[] args) throws Exception {
        final String PSS_MBOM = PropertyUtil.getSchemaProperty(context, "policy_PSS_MBOM");
        final String STATE_IN_WORK = PropertyUtil.getSchemaProperty(context, "policy", PSS_MBOM, "state_InWork");
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");

        Vector vecReturn = null;

        ContextUtil.startTransaction(context, false);
        context.setApplication("VPLM");
        PLMCoreModelerSession plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
        plmSession.openSession();

        try {
            vecReturn = new Vector(objectList.size());
            Map map = null;
            String strObjID = DomainObject.EMPTY_STRING;
            String strTreeLink = DomainObject.EMPTY_STRING;

            Iterator objectItr = objectList.iterator();
            // Map<String, List<String>> map2 = FRCMBOMModelerUtility.getAlternateReferences(context, plmSession, iPointingReferences, iComputeStatus);
            objectItr = objectList.iterator();
            while (objectItr.hasNext()) {
                map = (Map) objectItr.next();

                StringBuilder sb = new StringBuilder("");
                strObjID = (String) map.get("physicalid");
                if (UIUtil.isNullOrEmpty(strObjID)) {
                    strObjID = (String) map.get(DomainConstants.SELECT_ID);
                }
                String relId = (String) map.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                String rootnode = (String) map.get("Root Node");

                String checkType = (String) map.get(DomainConstants.SELECT_TYPE);
                if (!TigerConstants.LIST_TYPE_MATERIALS.contains(checkType) && !checkType.equalsIgnoreCase(TigerConstants.TYPE_PSS_OPERATION)
                        && !checkType.equalsIgnoreCase(TigerConstants.TYPE_PSS_LINEDATA)) {
                    if (!UIUtil.isNullOrEmpty(strObjID) && UIUtil.isNullOrEmpty(rootnode)) {

                        List<String> iPointingReferences = new ArrayList<String>();
                        iPointingReferences.add(strObjID);
                        boolean[] iComputeStatus = new boolean[1];
                        iComputeStatus[0] = true;
                        Map<String, List<String>> map2 = FRCMBOMModelerUtility.getAlternateReferences(context, plmSession, iPointingReferences, iComputeStatus);

                        List<String> val = (List<String>) map2.get(strObjID);
                        DomainObject domMBOM = DomainObject.newInstance(context, strObjID);
                        String strState = domMBOM.getInfo(context, DomainConstants.SELECT_CURRENT);
                        if (val == null || val.isEmpty()) {
                            strTreeLink = "No";
                        } else {

                            if (strState.equalsIgnoreCase(STATE_IN_WORK)) {

                                strTreeLink = "<a class=\"object\" href=\"JavaScript:emxTableColumnLinkClick('../common/emxIndentedTable.jsp?program=pss.mbom.table.UIUtil:includeConnectedAlternateMBOM&amp;objectId="
                                        + XSSUtil.encodeForHTMLAttribute(context, strObjID) + "&amp;relId=" + XSSUtil.encodeForHTMLAttribute(context, relId)
                                        + "&amp;table=AEFGeneralSearchResults&amp;header=PSS_FRCMBOMCentral.Header.AlternateMBOM&amp;suiteKey=FRCMBOMCentral&amp;submitAction=refreshCaller&amp;selection=single&amp;submitURL=../FRCMBOMCentral/PSS_FRCMBOMReplaceByExistingPostProcess.jsp?isAlternate=true&amp;FRCToolbarGetChangeObjectCmdValue=change', '800', '575','true','popup')\">Yes</a>";

                                // TIGTK-3667 : END

                            } else {
                                // TIGTK-4412 : START
                                strTreeLink = "<a class=\"object\" href=\"JavaScript:emxTableColumnLinkClick('../common/emxIndentedTable.jsp?program=pss.mbom.table.UIUtil:includeConnectedAlternateMBOM&amp;objectId="
                                        + XSSUtil.encodeForHTMLAttribute(context, strObjID) + "&amp;relId=" + XSSUtil.encodeForHTMLAttribute(context, relId)
                                        + "&amp;table=AEFGeneralSearchResults&amp;header=PSS_FRCMBOMCentral.Header.AlternateMBOM&amp;suiteKey=FRCMBOMCentral&amp;FRCToolbarGetChangeObjectCmdValue=change', '800', '575','true','popup')\">Yes</a>";
                                // TIGTK-4412 : END
                            }

                        }
                        sb.append(strTreeLink);
                    }
                }
                vecReturn.addElement(sb.toString());
            }

            closeSession(plmSession);
            // if (transactioActive) {
            ContextUtil.commitTransaction(context);
            // }
            // vecReturn.addElement("a");

        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in getAlternateAvailableColumn: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
            closeSession(plmSession);
            // if (transactioActive) {
            ContextUtil.commitTransaction(context);
            // }
            throw new FrameworkException(e);
        }

        return vecReturn;

    }

    @com.matrixone.apps.framework.ui.IncludeOIDProgramCallable
    public MapList includeConnectedAlternateMBOM(Context context, String[] args) throws Exception {
        Map programMap = (Map) JPO.unpackArgs(args);

        String strObjectIds = (String) programMap.get("objectId");
        MapList includeList = new MapList();
        try {
            List<String> iPointingReferences = new ArrayList<String>();
            iPointingReferences.add(strObjectIds);
            boolean[] iComputeStatus = new boolean[1];
            iComputeStatus[0] = true;
            ContextUtil.startTransaction(context, false);
            context.setApplication("VPLM");
            PLMCoreModelerSession plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            Map<String, List<List<Object>>> map2 = FRCMBOMModelerUtility.getAlternateReferencesAndRelationship(context, plmSession, iPointingReferences, iComputeStatus);
            List<List<Object>> lst = map2.get(strObjectIds);

            Iterator itr = lst.iterator();
            HashMap mNewObj = new HashMap();
            while (itr.hasNext()) {
                List<String> objLst = (List<String>) itr.next();
                DomainObject dom = new DomainObject(objLst.get(1));
                mNewObj.put(DomainConstants.SELECT_ID, dom.getInfo(context, DomainConstants.SELECT_ID));
                HashMap mNewObj1 = new HashMap();
                mNewObj1.putAll(mNewObj);
                includeList.add(mNewObj1);
            }
            closeSession(plmSession);
            // if (transactioActive) {
            ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            // TIGTK-5405 - 07-04-2017 - VB - START
            logger.error("Error in includeConnectedAlternateMBOM: ", e);
            // TIGTK-5405 - 07-04-2017 - VB - END
        }
        return includeList;
    }

    public static Vector getOwnership(Context context, String[] args) throws Exception {

        Vector vecResult = new Vector();
        String strlistPathIds = DomainConstants.EMPTY_STRING;
        StringList listMfgProuctionPlanningPID = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map paramList = (Map) programMap.get("paramList");
            String strMBOMPID = (String) paramList.get("objectId");
            MapList objectList = (MapList) programMap.get("objectList");
            if (UIUtil.isNotNullAndNotEmpty(strMBOMPID)) {
                String strquery = "query path type SemanticRelation containing " + strMBOMPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                strlistPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
            }
            if (UIUtil.isNotNullAndNotEmpty(strlistPathIds)) {
                String[] strOwnerArray = strlistPathIds.split("\n");
                for (int i = 0; i < strOwnerArray.length; i++) {
                    String strPhysicalId = strOwnerArray[i];
                    listMfgProuctionPlanningPID.add(strPhysicalId);
                }
            }

            Iterator itrObjects = objectList.iterator();
            // Do for each object
            while (itrObjects.hasNext()) {
                Map mapObjectInfo = (Map) itrObjects.next();
                String strPlantId = (String) mapObjectInfo.get(DomainConstants.SELECT_ID);
                if (UIUtil.isNotNullAndNotEmpty(strPlantId)) {
                    if (!listMfgProuctionPlanningPID.isEmpty()) {
                        for (int i = 0; i < listMfgProuctionPlanningPID.size(); i++) {
                            String strPlantOwnership = DomainConstants.EMPTY_STRING;
                            String strMfgProductionPlanning = (String) listMfgProuctionPlanningPID.get(i);
                            String strMfgPlanningId = strMfgProductionPlanning.split("\\|")[1];
                            DomainObject dMfgProductionPlanningObj = DomainObject.newInstance(context, strMfgPlanningId);
                            String strPlantFromMfgPlanning = dMfgProductionPlanningObj.getInfo(context, "to[VPLMrel/PLMConnection/V_Owner].from.physicalid");
                            if (strPlantId.equalsIgnoreCase(strPlantFromMfgPlanning)) {
                                strPlantOwnership = dMfgProductionPlanningObj.getAttributeValue(context, "PSS_ManufacturingPlantExt.PSS_Ownership");
                                vecResult.add(strPlantOwnership);
                            }
                        }
                    }
                }
            }
            return vecResult;
        } catch (Exception e) {
            logger.error("Error in getOwnership: ", e);
            throw e;
        }
    }

    public static void updateOwnershipOfPlant(Context context, String[] args) throws Exception {

        String strlistPathIds = DomainConstants.EMPTY_STRING;
        StringList listMfgProuctionPlanningPID = new StringList();
        PLMCoreModelerSession plmSession = null;
        boolean transactionActive = false;
        try {

            ContextUtil.startTransaction(context, true);
            transactionActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String strMBOMPID = (String) requestMap.get("objectId");
            String strPlantId = (String) paramMap.get("objectId");
            String strNewValue = (String) paramMap.get("New Value");
            if (UIUtil.isNotNullAndNotEmpty(strMBOMPID)) {
                String strquery = "query path type SemanticRelation containing " + strMBOMPID + " where owner.type=='MfgProductionPlanning' select owner.physicalid dump |";
                strlistPathIds = MqlUtil.mqlCommand(context, strquery, false, false);
            }
            if (UIUtil.isNotNullAndNotEmpty(strlistPathIds)) {
                String[] strOwnerArray = strlistPathIds.split("\n");
                for (int i = 0; i < strOwnerArray.length; i++) {
                    String strPhysicalId = strOwnerArray[i];
                    listMfgProuctionPlanningPID.add(strPhysicalId);
                }
            }

            // Do for each object

            if (UIUtil.isNotNullAndNotEmpty(strPlantId)) {
                if (!listMfgProuctionPlanningPID.isEmpty()) {
                    for (int i = 0; i < listMfgProuctionPlanningPID.size(); i++) {
                        String strMfgProductionPlanning = (String) listMfgProuctionPlanningPID.get(i);
                        String strMfgPlanningId = strMfgProductionPlanning.split("\\|")[1];
                        DomainObject dMfgProductionPlanningObj = DomainObject.newInstance(context, strMfgPlanningId);
                        String strPlantFromMfgPlanning = dMfgProductionPlanningObj.getInfo(context, "to[VPLMrel/PLMConnection/V_Owner].from.physicalid");

                        if (strPlantId.equalsIgnoreCase(strPlantFromMfgPlanning)) {
                            String strOwnershipValue = (String) dMfgProductionPlanningObj.getAttributeValue(context, "PSS_ManufacturingPlantExt.PSS_Ownership");
                            if (strOwnershipValue.equals("Consumer")) {
                                String strMasterPlantConnectedToObject = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strMBOMPID);
                                if (UIUtil.isNotNullAndNotEmpty(strMasterPlantConnectedToObject))
                                    MqlUtil.mqlCommand(context, "notice $1", "Only one plant should be Master Plant.");
                                else
                                    dMfgProductionPlanningObj.setAttributeValue(context, "PSS_ManufacturingPlantExt.PSS_Ownership", strNewValue);
                            } else
                                dMfgProductionPlanningObj.setAttributeValue(context, "PSS_ManufacturingPlantExt.PSS_Ownership", strNewValue);
                        }
                    }
                }
            }

            flushAndCloseSession(plmSession);
            if (transactionActive)
                ContextUtil.commitTransaction(context);
        } catch (Exception e) {
            logger.error("Error in updateOwnershipOfPlant: ", e);
            if (transactionActive)
                ContextUtil.abortTransaction(context);
            throw e;
        }

    }

    /**
     * This method return the Type Of Part Value in table
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static Vector getTypeOfPartValue(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        Vector vecReturn = null;
        try {
            vecReturn = new Vector(objectList.size());
            Iterator objectItr = objectList.iterator();
            while (objectItr.hasNext()) {
                Map map = (Map) objectItr.next();
                StringBuilder sb = new StringBuilder();
                String strObjID = (String) map.get(TigerConstants.SELECT_PHYSICALID);
                String strRelID = (String) map.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                if (UIUtil.isNotNullAndNotEmpty(strObjID) && UIUtil.isNotNullAndNotEmpty(strRelID)) {
                    String strTypeOfPart = DomainConstants.EMPTY_STRING;
                    String strRelName = MqlUtil.mqlCommand(context, "print connection " + strRelID + " select name dump |", false, false);
                    if (strRelName.equalsIgnoreCase(TigerConstants.RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE)) {
                        DomainRelationship domRel = DomainRelationship.newInstance(context, strRelID);
                        strTypeOfPart = domRel.getAttributeValue(context, TigerConstants.ATTRIBUTE_TYPE_OF_PART);
                    } else {
                        DomainObject domMaterial = DomainObject.newInstance(context, strObjID);
                        strTypeOfPart = domMaterial.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_MANUFACTURING_ITEMEXT_PSS_MAKEORBUYMATERIAL);
                    }
                    sb.append(strTypeOfPart);
                }

                vecReturn.addElement(sb.toString());
            }

        } catch (Exception e) {
            logger.error("Error in getTypeOfPartValue: ", e);

            throw e;
        }
        return vecReturn;
    }

    // TIGTK-10265:Rutuja Ekatpure:15/11/2017:Start
    /***
     * this method used for programHTMLOutput on column for product configuration in table PSS_MBOMHarmonyToPCMatrix.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public List getProductConfiguration(Context context, String[] args) throws Exception {
        logger.debug("getProductConfiguration : Start");
        HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> requestMap = (HashMap<?, ?>) paramMap.get("requestMap");
        MapList columnMapList = new MapList();

        try {
            String strObjectId = (String) requestMap.get("objectId");
            StringList slProductAssociated = (StringList) getProductsAssociatedToPart(context, strObjectId);
            if (slProductAssociated.isEmpty()) {
                String strProductNotConnectedErrorMsg = EnoviaResourceBundle.getProperty(context, "emxFRCMBOMCentralStringResources", context.getLocale(),
                        "PSS_FRCMBOMCentral.Error.Message.PartIsNotConnectedToAnyProduct");
                DomainObject domPartObject = DomainObject.newInstance(context, strObjectId);
                StringList slPartInfoKeys = new StringList();
                slPartInfoKeys.add(DomainConstants.SELECT_TYPE);
                slPartInfoKeys.add(DomainConstants.SELECT_NAME);
                slPartInfoKeys.add(DomainConstants.SELECT_REVISION);
                Map mapPartInfo = domPartObject.getInfo(context, slPartInfoKeys);

                strProductNotConnectedErrorMsg = strProductNotConnectedErrorMsg.replace("$<type>", (String) mapPartInfo.get(DomainConstants.SELECT_TYPE));
                strProductNotConnectedErrorMsg = strProductNotConnectedErrorMsg.replace("$<name>", (String) mapPartInfo.get(DomainConstants.SELECT_NAME));
                strProductNotConnectedErrorMsg = strProductNotConnectedErrorMsg.replace("$<revision>", (String) mapPartInfo.get(DomainConstants.SELECT_REVISION));
                MqlUtil.mqlCommand(context, "notice $1", strProductNotConnectedErrorMsg);
            } else {
                for (int i = 0; i < slProductAssociated.size(); i++) {
                    String strProductId = (String) slProductAssociated.get(i);
                    DomainObject domProduct = DomainObject.newInstance(context, strProductId);
                    StringList slObjSelectStmts = new StringList();
                    slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
                    slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                    slObjSelectStmts.addElement(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME);
                    StringList slRelSelectStmts = new StringList();

                    Pattern relPattern = new Pattern("Product Configuration");
                    MapList mlPC = (MapList) domProduct.getRelatedObjects(context, relPattern.getPattern(), // Relationship
                            // Pattern
                            "Product Configuration", // Object Pattern
                            slObjSelectStmts, // Object Selects
                            slRelSelectStmts, // Relationship Selects
                            false, // to direction
                            true, // From direction
                            (short) 0, // recursion level
                            null, // object where clause
                            null, // Relationship where clause
                            (short) 0, // limit
                            null, // Include type
                            null, // Include Relationship
                            null // Include Map
                    );
                    Iterator<?> itr = mlPC.iterator();

                    while (itr.hasNext()) {

                        Map<?, ?> newMap = (Map<?, ?>) itr.next();
                        String strColumnName = (String) newMap.get(TigerConstants.SELECT_ATTRIBUTE_MARKETINGNAME);
                        String strColumnId = (String) newMap.get(DomainConstants.SELECT_ID);

                        // create a column map to be returned.
                        HashMap colMap = new HashMap();
                        HashMap settingsMap = new HashMap();

                        // Set information of Column Settings in settingsMap
                        settingsMap.put("Column Type", "programHTMLOutput");
                        settingsMap.put("program", "pss.mbom.table.UIUtil");
                        settingsMap.put("function", "getCheckboxesForPC");
                        settingsMap.put("Editable", "true");
                        settingsMap.put("Sortable", "false");
                        settingsMap.put("Width", "50");
                        settingsMap.put("Registered Suite", "FRCMBOMCentral");
                        colMap.put("name", "PSS_PCOption|" + strColumnId);
                        colMap.put("label", XSSUtil.encodeForHTML(context, strColumnName));
                        colMap.put("settings", settingsMap);
                        columnMapList.add(colMap);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Errot in getProductConfiguration : " + e);
            throw e;
        }
        logger.debug("getProductConfiguration : End");
        return columnMapList;
    }

    /****
     * this method used to construct check boxes for PC
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector getCheckboxesForPC(Context context, String[] args) throws Exception {
        logger.debug("getCheckboxesForPC : Start");
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> columnMap = (HashMap<?, ?>) programMap.get("columnMap");
        MapList mlObjectIdsList = (MapList) programMap.get("objectList");

        String strPCName = (String) columnMap.get("name");
        String strPCId = strPCName.split("\\|")[1];

        Vector<String> vecPCWithHarmonyOption = new Vector<String>();

        DomainObject domPCObject = DomainObject.newInstance(context, strPCId);
        StringList slHarmonyAssociated = domPCObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_PCASSOCIATEDTOHARMONY + "].to.id");

        for (int i = 0; i < mlObjectIdsList.size(); i++) {
            Map<?, ?> tempMap = (Map<?, ?>) mlObjectIdsList.get(i);
            String strHarmonyId = (String) tempMap.get("id");
            String strParentId = (String) tempMap.get("id[parent]");
            StringBuilder sbCheckbox = new StringBuilder();
            StringBuilder sbValue = new StringBuilder();

            sbValue.append(strPCId);
            sbValue.append("|");
            sbValue.append(strHarmonyId);
            String strCurrentValue = strParentId + "|" + strHarmonyId;
            // TIGTK-12823 :START
            String strRelSelect = "from[" + TigerConstants.RELATIONSHIP_PSS_PCASSOCIATEDTOHARMONY + "|to.id" + "==" + strHarmonyId + "].id";

            String strCommand = "print bus \"" + strPCId + "\"   select  \"" + strRelSelect + "\"  dump | ";
            String strResult = MqlUtil.mqlCommand(context, strCommand, true, true);
            StringList slRelidsList = new StringList();
            StringList slAttributeValue = new StringList();
            if (!strResult.isEmpty()) {
                slRelidsList = FrameworkUtil.split(strResult, "|");
            }
            if (slRelidsList.size() > 0) {
                for (int j = 0; j < slRelidsList.size(); j++) {
                    String strRel = (String) slRelidsList.get(j);
                    String strSttribute = DomainRelationship.getAttributeValue(context, strRel, "PSS_PCHarmonyIdentification");
                    slAttributeValue.add(strSttribute);
                }
            }

            if (slHarmonyAssociated.contains(strHarmonyId) && slAttributeValue.contains(strCurrentValue)) {
                // TIGTK-12823 :END
                sbCheckbox.append("<input type=\"checkbox\" checked=\"true\"  name=\"PCSelected\" id='");
                sbCheckbox.append(strHarmonyId);
                sbCheckbox.append("' class='");
                sbCheckbox.append(sbValue.toString());
                sbCheckbox.append("' value='");
                sbCheckbox.append(sbValue.toString());
                sbCheckbox.append("' onclick=\"javascript:changePCHarmonyMatrixEditOptions(this");
                sbCheckbox.append(")\"/>");
                sbCheckbox.append("<input type=\"hidden\"   name=\"PCUnSelected\" value=\"\"");
                sbCheckbox.append(" class='");
                sbCheckbox.append(sbValue.toString());
                sbCheckbox.append("'/>");
                vecPCWithHarmonyOption.addElement(sbCheckbox.toString());
            } else {
                sbCheckbox.append("<input type=\"checkbox\" name=\"PCSelected\" id='");
                sbCheckbox.append(strHarmonyId);
                sbCheckbox.append("' class='");
                sbCheckbox.append(sbValue.toString());
                sbCheckbox.append("' value='");
                sbCheckbox.append(sbValue.toString());
                sbCheckbox.append("' onclick=\"javascript:changePCHarmonyMatrixEditOptions(this");
                sbCheckbox.append(")\"/>");
                vecPCWithHarmonyOption.addElement(sbCheckbox.toString());
            }
        }
        logger.debug("getCheckboxesForPC : End");
        return vecPCWithHarmonyOption;
    }

    /***
     * this methiod used to getting hardware product connected to part
     * @param context
     * @param strObjectId
     * @return
     * @throws Exception
     */
    public StringList getProductsAssociatedToPart(Context context, String strObjectId) throws Exception {
        logger.debug("getProductsAssociatedToPart : Start");
        MapList mlProductList = new MapList();
        String strProductId = DomainConstants.EMPTY_STRING;
        StringList slProductID = new StringList();
        PLMCoreModelerSession plmSession = null;
        boolean transactioActive = false;
        try {
            Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_GBOM);
            relPattern.addPattern(DomainConstants.RELATIONSHIP_EBOM);

            Pattern postRelPattern = new Pattern(TigerConstants.RELATIONSHIP_GBOM);

            StringList slselectObjStmts = new StringList(DomainConstants.SELECT_ID);
            slselectObjStmts.addElement(DomainConstants.SELECT_NAME);
            List<String> objectIdList = new ArrayList<String>();
            objectIdList.add(strObjectId);

            ContextUtil.startTransaction(context, false);
            transactioActive = true;
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();

            List<String> strPhysicalStructureId = FRCMBOMModelerUtility.getScopedPSReferencePIDFromList(context, plmSession, objectIdList);
            if (strPhysicalStructureId != null && strPhysicalStructureId.size() > 0) {
                MapList mapLIst = getPartFromVPMReference(context, strPhysicalStructureId.get(0));
                if (mapLIst != null && mapLIst.size() > 0) {
                    Map objMap = (Map) mapLIst.get(0);
                    DomainObject domPart = DomainObject.newInstance(context, (String) objMap.get(DomainConstants.SELECT_ID));
                    mlProductList = domPart.getRelatedObjects(context, relPattern.getPattern(), // relationship
                            // pattern
                            DomainConstants.QUERY_WILDCARD, // object pattern
                            slselectObjStmts, // object selects
                            null, // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 0, // recursion level
                            null, // object where clause
                            null, (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 1000, // pageSize
                            null, // Post pattern
                            postRelPattern, null, null);
                }
            }
            Iterator<?> itr = mlProductList.iterator();
            while (itr.hasNext()) {
                Map<?, ?> mTempMap = (Map<?, ?>) itr.next();
                strProductId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                slProductID.addElement(strProductId);
            }

            flushAndCloseSession(plmSession);
            if (transactioActive) {
                ContextUtil.commitTransaction(context);
            }
        } catch (Exception e) {
            flushAndCloseSession(plmSession);
            if (transactioActive) {
                ContextUtil.abortTransaction(context);
            }
            logger.error("Error in getProductsAssociatedToPart: ", e);
            throw e;
        }
        logger.debug("getProductsAssociatedToPart : End");
        return slProductID;
    }
    // TIGTK-10265:Rutuja Ekatpure:15/11/2017:End
}