
/*
 * * ${CLASS:PSS_ColorCatalog}** Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes. Copyright notice
 * is precautionary only and does not evidence any actual or intended publication of such program
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.apps.productline.ProductLineConstants;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;
import com.matrixone.apps.configuration.ConfigurationConstants;

/**
 * This JPO class has some methods pertaining to MarketingFeature Extension.
 * @author XOG
 * @version R210 - Copyright (c) 1993-2015 Dassault Systemes.
 */
public class PSS_ColorCatalog_mxJPO extends ConfigurationFeatureBase_mxJPO {
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_ColorCatalog_mxJPO.class);

    private static final long serialVersionUID = 1L;

    /**
     * Create a new ${CLASS:MarketingFeature} object from a given id.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments.
     * @throws Exception
     *             if the operation fails
     * @author XOG
     * @since R210
     */

    public PSS_ColorCatalog_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Main entry point.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @return an integer status code (0 = success)
     * @throws Exception
     *             if the operation fails
     * @author XOG
     * @since R210
     */
    public int mxMain(Context context, String[] args) throws Exception {
        if (!context.isConnected()) {
            String sContentLabel = EnoviaResourceBundle.getProperty(context, "Configuration", "emxProduct.Error.UnsupportedClient", context.getSession().getLanguage());
            throw new Exception(sContentLabel);
        }
        return 0;
    }

    /**
     * This Methods is used to get the Color Catalog Structure.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            - Holds the following arguments 0 - HashMap containing the following arguments
     * @return Object - MapList containing the Color Catalog Structure Details
     * @throws FrameworkException
     * @author A69
     * @since R212
     * @Modified by Priyanka Salunke for RFC-92(DIVERSITY)
     * @Modified on : 10/10/2016
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getColorCatalogStructure(Context context, String[] args) throws FrameworkException {

        MapList mpReturnList = new MapList();
        try {
            HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strObjId = (String) paramMap.get("objectId");
            String sExpandLevels = (String) paramMap.get("emxExpandFilter");
            int expandLevel = "All".equals(sExpandLevels) ? 0 : Integer.parseInt(sExpandLevels);
            StringList selectStmts = new StringList(2);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            StringList selectRelStmts = new StringList(1);
            selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            // Added by Priyanka Salunke on Date : 11/10/2016 for RFC-92(Diversity)
            String strRelPattern = TigerConstants.RELATIONSHIP_PSS_COLORCATALOG + "," + TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION + ","
                    + TigerConstants.RELATIONSHIP_MANDATORYCONFIGURATIONFEATURES;
            // End by Priyanka Salunke on Date : 11/10/2016 for RFC-92(Diversity)
            String strTypePattern = TigerConstants.TYPE_PSS_COLORCATALOG + "," + TigerConstants.TYPE_PSS_COLOROPTION;

            DomainObject domObj = DomainObject.newInstance(context, strObjId);
            mpReturnList = domObj.getRelatedObjects(context, strRelPattern, strTypePattern, // object
                    // pattern
                    selectStmts, // object selects
                    selectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) expandLevel, // recursion level
                    null, // object where clause
                    null, 0); // relationship where clause
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getColorCatalogStructure: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return mpReturnList;
    }

    @SuppressWarnings("rawtypes")
    public Map<String, String> validateColorCodeValue(Context context, String[] args) throws FrameworkException {
        String STR_COLOR_CODE_ALREADY_EXISTS = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.ColorOption.ColorCodeValueCheck");
        // TIGTK-2541 : 31-08-2016 : START
        String defualtOrg = PersonUtil.getDefaultOrganization(context, context.getUser());
        // TIGTK-2541 : 31-08-2016 : END
        HashMap<String, String> mapStatus = new HashMap<String, String>();
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> paramMap = (Map<?, ?>) programMap.get("paramMap");

            // Added for Diversity stream issue TIGTK-4742 : Priyanka Salunke : 16-Feb-2017 : START
            Map<?, ?> mRequestMap = (Map<?, ?>) programMap.get("requestMap");
            String strOEMCode = (String) mRequestMap.get("PSS_OEMCode");
            String strDisplayText = "";
            // Added for Diversity stream issue TIGTK-4742 : Priyanka Salunke : 16-Feb-2017 : END

            String strNewObjectId = (String) paramMap.get("newObjectId");
            DomainObject domNewObj;
            // Added for Diversity stream issue TIGTK-3756 : Priyanka Salunke : 08-Dec-2016 : START
            if (UIUtil.isNotNullAndNotEmpty(strNewObjectId)) {
                // Added for Diversity stream issue TIGTK-3756 : Priyanka Salunke : 08-Dec-2016 : END
                domNewObj = DomainObject.newInstance(context, strNewObjectId);
                strDisplayText = (String) mRequestMap.get("Display Text");
            } else {
                String strObjectId = (String) paramMap.get("objectId");
                domNewObj = DomainObject.newInstance(context, strObjectId);
                strDisplayText = (String) mRequestMap.get("DisplayText");
            }
            // Added for Diversity stream issue TIGTK-3756 : Priyanka Salunke : 08-Dec-2016 : START

            // Added for Diversity stream issue TIGTK-4742 : Priyanka Salunke : 07-Feb-2017 : START
            StringBuffer sbSAPDescription = new StringBuffer();
            if (UIUtil.isNotNullAndNotEmpty(strOEMCode) && UIUtil.isNullOrEmpty(strDisplayText)) {
                sbSAPDescription.append(strOEMCode);
            } else if (UIUtil.isNotNullAndNotEmpty(strDisplayText) && UIUtil.isNullOrEmpty(strOEMCode)) {
                sbSAPDescription.append(strDisplayText);
            } else if (UIUtil.isNotNullAndNotEmpty(strOEMCode) && UIUtil.isNotNullAndNotEmpty(strDisplayText)) {
                sbSAPDescription.append(strOEMCode);
                sbSAPDescription.append(" ");
                sbSAPDescription.append(strDisplayText);
            }

            if (sbSAPDescription != null) {
                domNewObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_SAPDESCRIPTION, sbSAPDescription.toString());
            }
            // Added for Diversity stream issue TIGTK-4742 : Priyanka Salunke : 07-Feb-2017 : END

            StringList slObjectSelect = new StringList(3);
            slObjectSelect.add(DomainConstants.SELECT_ID);
            slObjectSelect.add(DomainConstants.SELECT_NAME);
            slObjectSelect.add(DomainConstants.SELECT_ORGANIZATION);
            slObjectSelect.add(TigerConstants.SELECT_ATTRIBUTE_PSS_COLORCODE);

            Map mapColorOptionInfo = domNewObj.getInfo(context, slObjectSelect);
            String strNewPSSColorCodeValue = (String) mapColorOptionInfo.get(TigerConstants.SELECT_ATTRIBUTE_PSS_COLORCODE);
            String strNewPSSColorCodeObjID = (String) mapColorOptionInfo.get(DomainConstants.SELECT_ID);

            // TIGTK-2541 : 31-08-2016 : START

            StringBuilder strQuery = new StringBuilder();
            strQuery.append("(");
            strQuery.append(DomainConstants.SELECT_ID).append(" !='").append(strNewPSSColorCodeObjID).append("'");
            strQuery.append(" && ");
            strQuery.append(DomainConstants.SELECT_ORGANIZATION).append(" =='").append(defualtOrg).append("'");
            strQuery.append(" && ");
            strQuery.append(TigerConstants.SELECT_ATTRIBUTE_PSS_COLORCODE).append(" == '").append(strNewPSSColorCodeValue).append("'");
            strQuery.append(")");
            MapList mlColorOptions = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_COLOROPTION, TigerConstants.VAULT_ESERVICEPRODUCTION, strQuery.toString(), slObjectSelect);
            if (mlColorOptions.size() > 0) {
                mapStatus.put("Action", "ERROR");
                STR_COLOR_CODE_ALREADY_EXISTS = STR_COLOR_CODE_ALREADY_EXISTS.replace("$<attribute[attribute_PSS_ColorCode]>", strNewPSSColorCodeValue);
                STR_COLOR_CODE_ALREADY_EXISTS = STR_COLOR_CODE_ALREADY_EXISTS.replace("$<organization>", defualtOrg);
                mapStatus.put("Message", STR_COLOR_CODE_ALREADY_EXISTS);
            }
            // Added for Diversity stream issue TIGTK-3756 : Priyanka Salunke : 08-Dec-2016 : END
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in validateColorCodeValue: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            ContextUtil.abortTransaction(context);
        } finally {
            ContextUtil.commitTransaction(context);

        }
        return mapStatus;
    }

    /**
     * In case Color Option is getting created under a Color Catalog, it display Context Color Catalog name in the WebForm In case stand-alone Color Option is getting created (Global Action), it
     * Provite facility to choose context Color Catalog
     * @param context
     *            the Matrix Context
     * @param args
     *            contains- -- programMap -> requestMap -> String objectId/ String UIContext -- programMap -> fieldMap -> String name
     * @returns String
     * @throws FrameworkException
     *             if the operation fails
     * @author A69
     * @since R212
     */
    public String getContextColorCatalog(Context context, String[] args) throws FrameworkException {
        StringBuffer sb = new StringBuffer();

        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap<?, ?> requestMap = (HashMap<?, ?>) programMap.get("requestMap");

            String strObjectID = (String) requestMap.get("objectId");
            String strUIContext = (String) requestMap.get("UIContext");

            if (!ProductLineCommon.isNotNull(strObjectID)) {
                strObjectID = "";
            }

            if ((ProductLineCommon.isNotNull(strUIContext) && !(strUIContext.equals("GlobalActions") || strUIContext.equals("Classification"))) || strObjectID.length() != 0) {
                DomainObject objContext = DomainObject.newInstance(context, strObjectID);

                StringList newList = new StringList();
                newList.addElement(ProductLineConstants.SELECT_NAME);
                newList.addElement(ProductLineConstants.SELECT_REVISION);
                newList.addElement(ProductLineConstants.SELECT_TYPE);
                Map<?, ?> strContextMap = objContext.getInfo(context, newList);

                String strTemp = "<a TITLE=";
                String strEndHrefTitle = ">";
                String strEndHref = "</a>";
                sb.append(strTemp);
                String strTypeIcon = ProductLineCommon.getTypeIconProperty(context, (String) strContextMap.get(ProductLineConstants.SELECT_TYPE));

                sb.append("\"\"");
                sb.append(strEndHrefTitle);
                sb.append("<img border=\'0\' src=\'../common/images/");
                sb.append(strTypeIcon);
                sb.append("\'/>");
                sb.append(strEndHref);
                // Find Bug : Dodgy Code : PS : 21-March-2017
                sb.append(" ");
                sb.append(XSSUtil.encodeForXML(context, strContextMap.get(ProductLineConstants.SELECT_NAME).toString()));
                sb.append(" ");
                sb.append(strContextMap.get(ProductLineConstants.SELECT_REVISION));
            }
        } catch (Exception e) {
            throw new FrameworkException(e);
        }
        return sb.toString();
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
     * @author Chintan DADHANIA
     * @since 15-Feb-2016
     */
    public Map<Integer, String> getAllAssociatedProductsFromPartId(Context context, String[] args) throws FrameworkException {

        HashMap<Integer, String> mapRetProducts = new HashMap<Integer, String>();
        String strProductId;

        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

            String strPartObjectId = (String) programMap.get("objectId");

            Set<String> setProducts = new HashSet<String>();

            DomainObject domPartObject = new DomainObject(strPartObjectId);
            MapList returnList = (MapList) domPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_GBOM + "," + DomainConstants.RELATIONSHIP_EBOM, "*",
                    new StringList(DomainConstants.SELECT_ID), new StringList(), true, false, (short) 0, "", "", 0, null, new Pattern(TigerConstants.RELATIONSHIP_GBOM), null);

            Iterator<?> itr = returnList.iterator();
            while (itr.hasNext()) {
                Map<?, ?> newMap = (Map<?, ?>) itr.next();
                strProductId = (String) newMap.get(DomainConstants.SELECT_ID);
                setProducts.add(strProductId);
            }

            Iterator<String> itrSetProducts = setProducts.iterator();
            // add values to Map.
            int intProductIndex = 0;
            while (itrSetProducts.hasNext()) {
                mapRetProducts.put(intProductIndex, itrSetProducts.next());
                intProductIndex++;
            }

        } catch (Exception e) {
            throw new FrameworkException(e);
        }
        return mapRetProducts;
    }

    /**
     * Get BOM of Selected Part.
     * @name getSelectedPartsForMatrixView
     * @param context
     *            the Matrix Context
     * @param args
     *            contains-
     * @returns MapList
     * @throws Exception
     *             if the operation fails
     * @author Chintan DADHANIA
     * @since 15-Feb-2016
     */
    public MapList getSelectedPartsForMatrixView(Context context, String[] args) throws Exception {
        PSS_emxPart_mxJPO emxPart = new PSS_emxPart_mxJPO(context, args);
        MapList mlPartList = emxPart.getEBOMsWithRelSelectablesSB(context, args);
        return mlPartList;
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
     * @author Chintan DADHANIA
     * @since 20-Feb-2016
     */
    @SuppressWarnings("rawtypes")
    public Vector getCheckboxesOfColorForPart(Context context, String[] args) throws Exception {
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> columnMap = (HashMap<?, ?>) programMap.get("columnMap");
        String STR_STATE = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.Part.Colorable.Modify.Condition.State");
        String STR_IS_COLORABLE = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.Part.Colorable.Condition");
        // MBOM:PHASE2.0 : TIGTK-7247 : PSE : 21-07-2017 : START
        String strMaterialAllowedStates = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(),
                "PSS_emxConfiguration.Material.Colorable.Modify.Condition.State");
        // MBOM:PHASE2.0 : TIGTK-7247 : PSE : 21-07-2017 : END
        MapList mlObjectIdsList = (MapList) programMap.get("objectList");

        String strColorOptionName = (String) columnMap.get("name");
        String strColorOptionId = strColorOptionName.split("\\|")[1];

        Vector<String> vecColorOption = new Vector<String>();

        DomainObject domColorOptionObject = DomainObject.newInstance(context, strColorOptionId);
        StringList slColorOptionPartConnection = domColorOptionObject.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].from.id");
        StringList slColorOptionPartCurrentState = domColorOptionObject.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].from.current");

        DomainObject domPartObject;
        String strPartCurrentState;
        Map<?, ?> tempMap;
        String strPartId, strPartLevel, strConnectionId;
        StringBuilder sbCheckbox;
        StringBuilder sbClassName;
        String strLevelId, strIsPartColorable;
        StringList slPartInfo;
        Map<?, ?> mapPartInfoData;
        int intIndexOfPart = -1;
        for (int i = 0; i < mlObjectIdsList.size(); i++) {
            tempMap = (Map<?, ?>) mlObjectIdsList.get(i);
            strPartId = (String) tempMap.get("id");
            strPartLevel = (String) tempMap.get("level");
            strLevelId = (String) tempMap.get("id[level]");
            strConnectionId = (String) tempMap.get("id[connection]");

            sbCheckbox = new StringBuilder();
            sbClassName = new StringBuilder();

            sbClassName.append(strPartId);
            sbClassName.append("|");
            sbClassName.append(strColorOptionId);
            sbClassName.append("|");
            sbClassName.append(strPartLevel);

            if (slColorOptionPartConnection.contains(strPartId)) {
                intIndexOfPart = slColorOptionPartConnection.indexOf(strPartId);
                if (STR_STATE.contains(slColorOptionPartCurrentState.get(intIndexOfPart).toString())) {
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
                    // MBOM:PHASE2.0 : TIGTK-7247 : PSE : 21-07-2017 : START
                } else if (strMaterialAllowedStates.contains(slColorOptionPartCurrentState.get(intIndexOfPart).toString())) {
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
                    // MBOM:PHASE2.0 : TIGTK-7247 : PSE : 21-07-2017 : END
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
                domPartObject = DomainObject.newInstance(context, strPartId);
                slPartInfo = new StringList(2);
                slPartInfo.add("current");
                slPartInfo.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_COLORABLE + "]");
                mapPartInfoData = (Map<?, ?>) domPartObject.getInfo(context, slPartInfo);

                strPartCurrentState = (String) mapPartInfoData.get("current");
                strIsPartColorable = (String) mapPartInfoData.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_COLORABLE + "]");
                if (STR_STATE.contains(strPartCurrentState) && strIsPartColorable.equalsIgnoreCase(STR_IS_COLORABLE)) {
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
                    // MBOM:PHASE2.0 : TIGTK-7247 : PSE : 21-07-2017 : START
                } else if (strMaterialAllowedStates.contains(strPartCurrentState)) {
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
                    // MBOM:PHASE2.0 : TIGTK-7247 : PSE : 21-07-2017 : END
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
        return vecColorOption;
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
     * @author Chintan DADHANIA
     * @since 20-Feb-2016
     * @Modified By : Priyanka Salunke
     * @Modified on : 11/10/2016
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List getProductRelatedColorAsColumns(Context context, String[] args) throws Exception {
        HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> requestMap = (HashMap<?, ?>) paramMap.get("requestMap");

        String strProductId = (String) requestMap.get("strProductId");
        String STR_ACCEPTED_CATALOG_STATE = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.ColorCatalog.AcceptedState");
        String STR_ACCEPTED_COLOR_STATE = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.ColorOption.AcceptedState");

        StringList selectStmts = new StringList(2);
        selectStmts.addElement(DomainConstants.SELECT_NAME);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        selectStmts.addElement(DomainConstants.SELECT_CURRENT);
        selectStmts.addElement(TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);

        DomainObject domProductObject = DomainObject.newInstance(context, strProductId);

        String strObjectWhereCondition = DomainConstants.SELECT_CURRENT + "== \"" + STR_ACCEPTED_COLOR_STATE + "\" || " + DomainConstants.SELECT_CURRENT + "== \"" + STR_ACCEPTED_CATALOG_STATE + "\"";
        // Added by Priyanka Salunke for RFC-92(Diversity) on Date : 11/10/2016
        MapList lsColor = (MapList) domProductObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_COLORCATALOG + "," + TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION, "*", selectStmts,
                new StringList(), false, true, (short) 0, strObjectWhereCondition, "", 0, new Pattern(TigerConstants.TYPE_PSS_COLOROPTION),
                new Pattern(TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION), null);

        // End by Priyanka Salunke for RFC-92(Diversity) on Date : 11/10/2016

        // Remove Duplicate Color Options
        HashSet<?> hsUniqueColorOptions = new HashSet<Map>();
        hsUniqueColorOptions.addAll(lsColor);
        lsColor.clear();
        lsColor.addAll(hsUniqueColorOptions);

        lsColor.sortStructure(DomainConstants.SELECT_NAME, "ascending", "string");
        // Define a new MapList to return.
        MapList columnMapList = new MapList();
        int i = 0;
        Iterator<?> itr = lsColor.iterator();
        while (itr.hasNext()) {
            Map<?, ?> newMap = (Map<?, ?>) itr.next();
            String strColumnName = (String) newMap.get(TigerConstants.SELECT_ATTRIBUTE_DISPLAYNAME);
            String strColumnId = (String) newMap.get(DomainConstants.SELECT_ID);

            // create a column map to be returned.
            HashMap colMap = new HashMap();
            HashMap settingsMap = new HashMap();

            // Set information of Column Settings in settingsMap
            settingsMap.put("Column Type", "programHTMLOutput");
            settingsMap.put("program", "PSS_ColorCatalog");
            settingsMap.put("function", "getCheckboxesOfColorForPart");
            settingsMap.put("Editable", "true");
            settingsMap.put("Sortable", "false");
            settingsMap.put("Width", "20");
            settingsMap.put("Registered Suite", "Configuration");
            // TIGTK-6819:PKH:Phase-2.0:Start
            if (i % 2 == 0) {
                settingsMap.put("Style Column", "PSS_EditorsColumn");
            }
            i++;
            // TIGTK-6819:PKH:Phase-2.0:End
            // set column information
            colMap.put("name", "PSS_ColorOption|" + strColumnId);
            // Modified for TIGTK-3657: Priyanka Salunke : 14-Dec-2016 : START
            colMap.put("label", "<img src='../common/images/assignColors24x24.png' class='PSSRotateColumn' onload='parent.rotateText(this)'/>" + XSSUtil.encodeForHTML(context, strColumnName) + "");
            // Modified for TIGTK-3657: Priyanka Salunke : 14-Dec-2016 : END
            colMap.put("settings", settingsMap);
            columnMapList.add(colMap);
        }

        return columnMapList;
    }

    /**
     * If Part is connected to Color Options then show Image icon else blank.
     * @name checkForAssignedColors
     * @param context
     *            the Matrix Context
     * @param args
     *            contains-programMap --> objectList
     * @returns StringList
     * @throws Exception
     *             if the operation fails
     * @author Chintan DADHANIA
     * @since 20-Feb-2016
     */

    @SuppressWarnings("rawtypes")
    public Vector displayColorMatrixTableIcon(Context context, String[] args) throws FrameworkException {
        Vector<String> vecReturnList = new Vector<String>();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList mlObjectIdsList = (MapList) programMap.get("objectList");
            Map<?, ?> tempMap;

            DomainObject domPartObject;
            String strPartId;
            StringList slPartConnectionToColorOptions;

            String strIconLink = "";

            for (int i = 0; i < mlObjectIdsList.size(); i++) {
                tempMap = (Map<?, ?>) mlObjectIdsList.get(i);
                strPartId = (String) tempMap.get("id");
                domPartObject = DomainObject.newInstance(context, strPartId);

                slPartConnectionToColorOptions = domPartObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id");
                if (slPartConnectionToColorOptions.size() > 0) {
                    strIconLink = "<img src='./images/assignColors16x16.png' alt='Colors' onclick=\"showModalDialog('../configuration/PSS_ModifyColorOptionPreProcess.jsp?commandType=ColorOptionMatrix&amp;objectId="
                            + strPartId + "',700,600)\"/>";
                } else {
                    strIconLink = " ";
                }

                vecReturnList.addElement(strIconLink);
            }
        } catch (Exception e) {
            throw new FrameworkException(e);
        }
        return vecReturnList;
    }

    /**
     * Get all Color options which are connected to part.
     * @name getAllColorOptionsOfProduct
     * @param context
     *            the Matrix Context
     * @param args
     *            contains-programMap --> strProductId
     * @returns StringList
     * @throws Exception
     *             if the operation fails
     * @author Chintan DADHANIA
     * @since 20-Feb-2016
     */

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getAllColorOptionsOfPart(Context context, String[] args) throws Exception {
        HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

        String strPartId = (String) programMap.get("objectId");
        StringList selectStmts = new StringList(2);
        selectStmts.addElement(DomainConstants.SELECT_NAME);
        selectStmts.addElement(DomainConstants.SELECT_ID);

        DomainObject domPartObject = DomainObject.newInstance(context, strPartId);

        MapList lsColor = (MapList) domPartObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_COLORLIST, TigerConstants.TYPE_PSS_COLOROPTION, selectStmts, new StringList(), false, true,
                (short) 0, "", "", 0);

        // Remove Duplicate Color Options
        HashSet<?> hsUniqueColorOptions = new HashSet<Map>();
        hsUniqueColorOptions.addAll(lsColor);
        lsColor.clear();
        lsColor.addAll(hsUniqueColorOptions);
        lsColor.sortStructure(DomainConstants.SELECT_NAME, "ascending", "string");
        return lsColor;
    }

    public StringList displayColorListIcon(Context context, String[] args) throws FrameworkException {
        StringList slReturnList = new StringList();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList mlObjectIdsList = (MapList) programMap.get("objectList");
            Map<?, ?> tempMap;

            DomainObject domPartObject;
            String strPartId;
            StringList slPartConnectionToColorOptions;

            String strIconLink = "";

            for (int i = 0; i < mlObjectIdsList.size(); i++) {
                tempMap = (Map<?, ?>) mlObjectIdsList.get(i);
                strPartId = (String) tempMap.get("id");
                domPartObject = DomainObject.newInstance(context, strPartId);

                slPartConnectionToColorOptions = domPartObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "].to.id");
                if (slPartConnectionToColorOptions.size() > 0) {
                    strIconLink = "<img src='./images/allColor.png' alt='Colors' onclick=\"showModalDialog('../configuration/PSS_ModifyColorOptionPreProcess.jsp?commandType=ColorOptionList&amp;objectId="
                            + strPartId + "',700,600)\"/>";
                } else {
                    strIconLink = " ";
                }
                slReturnList.add(strIconLink);
            }
        } catch (Exception e) {
            throw new FrameworkException(e);
        }
        return slReturnList;
    }

    /**
     * This Method is added for Showing Edit Command :PnO Stream :SIE
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    public static Boolean isNotFrozenState(Context context, String[] args) throws FrameworkException {
        boolean bNotInvalidState = false;
        bNotInvalidState = isFrozenState(context, args);
        return !bNotInvalidState;
    }

    /**
     * This Method is added for exclude the already connected Color Catalog and Color options
     * @name excludeAvailableColorCatalogAndOptions
     * @param context
     *            the Matrix Context
     * @throws Exception
     *             if the operation fails
     * @Created By : Priyanka Salunke
     * @Created on : 11/10/2016
     */
    @SuppressWarnings("rawtypes")
    public Object excludeAvailableColorCatalogAndOptions(Context context, String[] args) throws Exception {

        StringList excludeOID = new StringList();
        try {
            HashMap mProgramMap = (HashMap) JPO.unpackArgs(args);
            String strType = (String) mProgramMap.get("txtType");
            String strObjectID = (String) mProgramMap.get("objectId");
            DomainObject domObject = new DomainObject();
            if (UIUtil.isNotNullAndNotEmpty(strObjectID)) {
                domObject = DomainObject.newInstance(context, strObjectID);
            }
            // TIGTK-3993 - 01-Feb-2017 - START
            MapList mlReturnList = null;
            // TIGTK-3993 - 01-Feb-2017 - END
            StringList slSelectable = new StringList();
            slSelectable.add(DomainConstants.SELECT_ID);

            if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_COLORCATALOG)) {
                StringBuffer sbWhere = new StringBuffer();
                sbWhere.append("to[");
                sbWhere.append(TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION);
                sbWhere.append("] == True");
                Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_CONFIGURATION_OPTION);
                Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_COLOROPTION);
                // Get already Connected Color Options
                mlReturnList = domObject.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                        typePattern.getPattern(), // object pattern
                        slSelectable, // object selects
                        null, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        sbWhere.toString(), // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null, null);
            } else {
                StringBuffer sbWhere1 = new StringBuffer();
                sbWhere1.append("to[");
                sbWhere1.append(TigerConstants.RELATIONSHIP_PSS_COLORCATALOG);
                sbWhere1.append("] == True");
                Pattern relationshipPattern1 = new Pattern(TigerConstants.RELATIONSHIP_PSS_COLORCATALOG);
                Pattern typePattern1 = new Pattern(TigerConstants.TYPE_PSS_COLORCATALOG);
                // Get already Connected Color Catalog
                mlReturnList = domObject.getRelatedObjects(context, relationshipPattern1.getPattern(), // relationship pattern
                        typePattern1.getPattern(), // object pattern
                        slSelectable, // object selects
                        null, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        sbWhere1.toString(), // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null, null);

            }
            // Exclude the objects which are already connected
            if (mlReturnList != null && mlReturnList.size() > 0) {
                Iterator itr = mlReturnList.iterator();
                Map mTempMap;
                while (itr.hasNext()) {
                    mTempMap = (Map) itr.next();
                    excludeOID.add((String) mTempMap.get(DomainConstants.SELECT_ID));
                }
            }
        } catch (Exception e) {
            logger.error("Exception in PSS_ColorCatalog : excludeAvailableColorCatalogAndOptions() :" + e);
            throw e;
        }
        return excludeOID;
    }

    // End of Method excludeAvailableColorCatalogAndOptions

    // Disconnect color options

    /**
     * @Method name disconnectColorOption
     * @param args
     * @return an integer status code (0 = success)
     * @throws Exception
     *             if the operation fails
     * @author Priyanka Salunke : TIGTK-9117
     * @since 18/10/2016
     * @Modified on : 01-11-2017
     */
    @SuppressWarnings("rawtypes")
    public int disconnectColorOption(Context context, String[] args) throws Exception {
        int iFlag = 0;
        boolean bFlag = false;
        try {
            logger.error("Exception in PSS_ColorCatalog : disconnectColorOption() :START ");
            String strColorCatalogId = args[1];
            String strColorOptionID = args[0];
            if (UIUtil.isNotNullAndNotEmpty(strColorCatalogId) && UIUtil.isNotNullAndNotEmpty(strColorOptionID)) {
                DomainObject domColorCatalogObject = DomainObject.newInstance(context, strColorCatalogId);
                String strType = (String) domColorCatalogObject.getInfo(context, DomainConstants.SELECT_TYPE);
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_COLORCATALOG)) {
                    DomainObject domColorOptionObject = DomainObject.newInstance(context, strColorOptionID);
                    String strSelectedColorDisplayName = (String) domColorOptionObject.getInfo(context, ConfigurationConstants.SELECT_ATTRIBUTE_DISPLAY_NAME);

                    String strMessage = EnoviaResourceBundle.getProperty(context, "emxConfigurationStringResource", context.getLocale(), "PSS_emxConfiguration.ColorConnectedToPart.RemoveAlert");
                    StringBuffer sbAlertMessage = new StringBuffer();
                    sbAlertMessage.append(strMessage);
                    StringList slColorOptionIdList = new StringList();

                    // Type Pattern
                    Pattern typeHPPattern = new Pattern(TigerConstants.TYPE_HARDWARE_PRODUCT);

                    // Rel Pattern
                    Pattern relHPPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_COLORCATALOG);

                    // Get Part of context Product
                    MapList mlConnectedHPOfColorCatalog = domColorCatalogObject.getRelatedObjects(context, relHPPattern.getPattern(), // relationship pattern
                            typeHPPattern.getPattern(), // object pattern
                            new StringList(DomainConstants.SELECT_ID), // object selects
                            new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 1, // recursion level
                            DomainConstants.EMPTY_STRING, // object where clause
                            DomainConstants.EMPTY_STRING, // relationship where clause
                            (short) 0, false, // checkHidden
                            true, // preventDuplicates
                            (short) 0, // pageSize
                            null, null, null, null, null);
                    if (mlConnectedHPOfColorCatalog != null && !mlConnectedHPOfColorCatalog.isEmpty()) {

                        for (int intHPCount = 0; intHPCount < mlConnectedHPOfColorCatalog.size(); intHPCount++) {
                            Map mpHPInfo = (Map) mlConnectedHPOfColorCatalog.get(intHPCount);
                            String strHPId = (String) mpHPInfo.get(DomainConstants.SELECT_ID);

                            if (UIUtil.isNotNullAndNotEmpty(strHPId)) {
                                HashMap inputMap = new HashMap();
                                inputMap.put("ProductId", strHPId);
                                String[] args1 = JPO.packArgs(inputMap);

                                MapList mlConnectedPartsOfColorCatalogHP = checkColorConnectedWithProductEBOM(context, args1);
                                if (mlConnectedPartsOfColorCatalogHP != null && !mlConnectedPartsOfColorCatalogHP.isEmpty()) {
                                    for (int intCCHPPartCount = 0; intCCHPPartCount < mlConnectedPartsOfColorCatalogHP.size(); intCCHPPartCount++) {
                                        Map mpCCHPPartInfo = (Map) mlConnectedPartsOfColorCatalogHP.get(intCCHPPartCount);
                                        Object objCCHPPartsColorIds = mpCCHPPartInfo.get("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "]." + DomainConstants.SELECT_TO_ID);
                                        StringList slColorOptionsIdOfCCHPParts = new StringList();
                                        if (objCCHPPartsColorIds != null) {
                                            if (objCCHPPartsColorIds instanceof StringList) {
                                                slColorOptionsIdOfCCHPParts = (StringList) objCCHPPartsColorIds;
                                            } else {
                                                slColorOptionsIdOfCCHPParts.addElement((String) objCCHPPartsColorIds);
                                            }
                                        }
                                        if (slColorOptionsIdOfCCHPParts != null && !slColorOptionsIdOfCCHPParts.isEmpty()) {
                                            if (slColorOptionsIdOfCCHPParts.contains(strColorOptionID)) {
                                                if (!slColorOptionIdList.contains(strColorOptionID)) {
                                                    slColorOptionIdList.addElement(strColorOptionID);
                                                    sbAlertMessage.append(strSelectedColorDisplayName);
                                                    sbAlertMessage.append("\n");
                                                    bFlag = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (bFlag) {
                            emxContextUtil_mxJPO.mqlNotice(context, sbAlertMessage.toString());
                            iFlag = 1;
                        }
                    }
                }
            }
            logger.error("Exception in PSS_ColorCatalog : disconnectColorOption() :END ");
        } catch (Exception e) {
            logger.error("Exception in PSS_ColorCatalog : disconnectColorOption() :" + e);
            throw e;
        }
        return iFlag;
    }

    // disconnectColorOption End

    /**
     * This method is to set value of SAP Description in table view
     * @Method name updateSAPDescription
     * @param args
     * @throws Exception
     *             if the operation fails
     * @author Priyanka Salunke
     * @since 10/April/2017
     */
    public void updateSAPDescription(Context context, String[] args) throws Exception {
        try {
            Map<?, ?> mProgramMap = (Map<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> mParamMap = (Map<?, ?>) mProgramMap.get("paramMap");
            Map<?, ?> mColumnMap = (Map<?, ?>) mProgramMap.get("columnMap");
            Map<?, ?> mSettingsMap = (Map<?, ?>) mColumnMap.get("settings");
            String strColumnName = (String) mSettingsMap.get("Column Name");
            if (UIUtil.isNotNullAndNotEmpty(strColumnName)) {
                String strColorOptionId = (String) mParamMap.get("objectId");
                if (UIUtil.isNotNullAndNotEmpty(strColorOptionId)) {
                    DomainObject domColorOption = DomainObject.newInstance(context, strColorOptionId);
                    String strObjectType = domColorOption.getInfo(context, DomainConstants.SELECT_TYPE);
                    String strNewValue = (String) mParamMap.get("New Value");
                    if (strObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_COLOROPTION)) {
                        String strOEMCodeValue;
                        String strDisplayTextValue;
                        StringBuffer sbSAPDescription = new StringBuffer();
                        if (strColumnName.equalsIgnoreCase("PSS_OEMCode")) {
                            strDisplayTextValue = (String) domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_VARIANTDISPLAYTEXT);
                            sbSAPDescription.append(strNewValue);
                            sbSAPDescription.append(" ");
                            sbSAPDescription.append(strDisplayTextValue);
                            domColorOption.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OEMCODE, strNewValue);
                        } else if (strColumnName.equalsIgnoreCase("PSS_DisplayText")) {
                            strOEMCodeValue = (String) domColorOption.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_OEMCODE);
                            sbSAPDescription.append(strOEMCodeValue);
                            sbSAPDescription.append(" ");
                            sbSAPDescription.append(strNewValue);
                            domColorOption.setAttributeValue(context, TigerConstants.ATTRIBUTE_VARIANTDISPLAYTEXT, strNewValue);
                        }
                        // Set Value of SAP Description
                        if (sbSAPDescription != null) {
                            domColorOption.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_SAPDESCRIPTION, sbSAPDescription.toString());
                        }
                    } else {
                        domColorOption.setAttributeValue(context, TigerConstants.ATTRIBUTE_VARIANTDISPLAYTEXT, strNewValue);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in PSS_ColorCatalog : updateSAPDescription() :" + e);
            throw e;
        }
    }

    // END updateSAPDescription

    /**
     * This method gives the edit access for OEM Code and Display Text
     * @param context
     * @param args
     * @throws Exception
     * @author Priyanka Salunke
     * @since 11-April-2017
     */
    public StringList getEditAccessForOEMCodeAndDisplayText(Context context, String[] args) throws Exception {
        StringList slOEMCodeAndDisplayTextEditable = new StringList();
        try {
            Map<?, ?> mProgramMap = (Map<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> mColumnMap = (Map<?, ?>) mProgramMap.get("columnMap");
            Map<?, ?> mSettingsMap = (Map<?, ?>) mColumnMap.get("settings");
            String strColumnName = (String) mSettingsMap.get("Column Name");
            MapList mlObjectIdsList = (MapList) mProgramMap.get("objectList");
            for (int i = 0; i < mlObjectIdsList.size(); i++) {
                Map mObjectIDMap = (Map<?, ?>) mlObjectIdsList.get(i);
                String strObjectId = (String) mObjectIDMap.get(DomainConstants.SELECT_ID);
                if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                    DomainObject domColorOption = DomainObject.newInstance(context, strObjectId);
                    String strObjectType = domColorOption.getInfo(context, DomainConstants.SELECT_TYPE);
                    if (strObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_COLOROPTION)) {
                        slOEMCodeAndDisplayTextEditable.add(true);
                    } else if (strObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_COLORCATALOG) && strColumnName.equalsIgnoreCase("PSS_DisplayText")) {
                        slOEMCodeAndDisplayTextEditable.add(true);
                    } else {
                        slOEMCodeAndDisplayTextEditable.add(false);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in PSS_ColorCatalog : getEditAccessForOEMCode() :" + e);
            throw e;
        }
        return slOEMCodeAndDisplayTextEditable;
    }
    // End getEditAccessForOEMCode()

    // TIGTK-6808 | 15/06/2017 | Harika Varanasi : Starts
    public MapList getAllAssociatedProductsForPart(Context context, String[] args) throws FrameworkException {
        MapList mlRetProducts = new MapList();
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strPartObjectId = (String) programMap.get("objectId");
            DomainObject domPartObject = DomainObject.newInstance(context, strPartObjectId);
            String relationshipPattern = TigerConstants.RELATIONSHIP_GBOM + "," + DomainConstants.RELATIONSHIP_EBOM;
            String typePattern = TigerConstants.TYPE_PART + "," + TigerConstants.TYPE_PRODUCTS + "," + TigerConstants.TYPE_HARDWARE_PRODUCT;
            StringList objectSelects = new StringList();
            objectSelects.addElement(DomainConstants.SELECT_ID);
            Pattern includeType = new Pattern(TigerConstants.TYPE_PRODUCTS);
            includeType.addPattern(TigerConstants.TYPE_HARDWARE_PRODUCT);
            Pattern includeRelationship = new Pattern(TigerConstants.RELATIONSHIP_GBOM);
            // Added to remove duplication of products : PSE : START
            MapList mlProducts = domPartObject.getRelatedObjects(context, relationshipPattern, typePattern, objectSelects, new StringList(), true, false, (short) 0, DomainConstants.EMPTY_STRING, "",
                    (short) 0, includeType, includeRelationship, null);

            StringList slUniqueProducts = new StringList();
            for (int i = 0; i < mlProducts.size(); i++) {
                Map mProductInfoMap = (Map) mlProducts.get(i);
                String strProdId = (String) mProductInfoMap.get(DomainConstants.SELECT_ID);
                if (!slUniqueProducts.contains(strProdId)) {
                    slUniqueProducts.add(strProdId);
                    mlRetProducts.add(mProductInfoMap);
                }
            }
            // Added to remove duplication of products : PSE : END

        } catch (Exception ex) {
            logger.error("Exception in PSS_ColorCatalog : getAllAssociatedProductsForPart() :" + ex);
        }
        return mlRetProducts;
    }
    // TIGTK-6808 | 15/06/2017 | Harika Varanasi : Starts

    /**
     * This method use to check selected color is connected with context Product part or not
     * @param context
     * @param args
     * @throws Exception
     * @author psalunke : TIGTK-9117
     * @since 01-11-2017
     */
    public MapList checkColorConnectedWithProductEBOM(Context context, String[] args) throws Exception {
        MapList mlConnectedPartsOfColorCatalogHP = new MapList();
        try {
            logger.error("PSS_ColorCatalog : checkColorConnectedWithProductEBOM() : START");
            Map<?, ?> mProgramMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strProductId = (String) mProgramMap.get("ProductId");

            if (UIUtil.isNotNullAndNotEmpty(strProductId)) {
                DomainObject domHPObj = DomainObject.newInstance(context, strProductId);
                // Object selects
                StringList slObjectSelects = new StringList();
                slObjectSelects.addElement(DomainConstants.SELECT_ID);
                slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_PSS_COLORLIST + "]." + DomainConstants.SELECT_TO_ID);

                // Type Pattern
                Pattern includeTypePattern = new Pattern(TigerConstants.TYPE_PART);

                // Rel Pattern
                Pattern relPattern = new Pattern(TigerConstants.RELATIONSHIP_GBOM);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_EBOM);
                mlConnectedPartsOfColorCatalogHP = domHPObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                        includeTypePattern.getPattern(), // object pattern
                        slObjectSelects, // object selects
                        new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        DomainConstants.EMPTY_STRING, // object where clause
                        DomainConstants.EMPTY_STRING, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 0, // pageSize
                        null, null, null, null, null);
            }
            logger.error("PSS_ColorCatalog : checkColorConnectedWithProductEBOM() : END");
            return mlConnectedPartsOfColorCatalogHP;
        } catch (Exception e) {
            logger.error("Exception in PSS_ColorCatalog : checkColorConnectedWithProductEBOM() :" + e);
            throw e;
        }

    }
    // End checkColorConnectedWithProductEBOM()

}