package pss.ecm.impactanalysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Attribute;
import matrix.db.AttributeItr;
import matrix.db.AttributeList;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class SummaryViewUtil_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SummaryViewUtil_mxJPO.class);

    static Set<String> setTotalForDomain = new HashSet<String>();

    public Map<String, Double> mpTotalValues = new HashMap<String, Double>();

    public enum ValueType {
        CONCAT, COPY, AVERAGE, SUM, HIGHEST
    }

    /***
     * @author RutujaE
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    static {
        setTotalForDomain.add("PSS_Advanced_Manufacturing");
        setTotalForDomain.add("PSS_Design_Engineering");

    }

    public String getSummaryValueForField(Context context, String args[]) throws Exception {
        logger.debug("SummaryViewUtil_mxJPO : getSummaryValueForField :START ");
        String strFinalValue = DomainConstants.EMPTY_STRING;
        boolean useTitleAttrbiute = false;
        Map<?, ?> programMap = JPO.unpackArgs(args);
        Map<?, ?> paramMap = (Map<?, ?>) programMap.get("paramMap");
        Map<?, ?> fieldMap = (Map<?, ?>) programMap.get("fieldMap");
        Map<?, ?> settings = (Map<?, ?>) fieldMap.get("settings");

        Map<?, ?> requestMap = (Map<?, ?>) programMap.get("requestMap");
        String sRAESymbolicAttributeName = (String) settings.get("RAEAttribute");
        String sRAEAttribute = PropertyUtil.getSchemaProperty(context, sRAESymbolicAttributeName);
        String sValueType = (String) settings.get("ValueType");
        String sIncludeRole = (String) settings.get("IncludeRole");
        String sExcludeRole = (String) settings.get("ExcludeRole");
        String sRiskValue = (String) settings.get("RiskValue");
        String strIAObjectId = (String) paramMap.get("objectId");
        String sView = (String) requestMap.get("view");
        String sFormat = DomainConstants.EMPTY_STRING;
        DomainObject domIA = DomainObject.newInstance(context, strIAObjectId);
        ValueType enmValueType = ValueType.SUM;
        if (!"".equals(sValueType)) {
            enmValueType = ValueType.valueOf(sValueType);
        }
        MapList mlRA = getRAListFRomIA(context, domIA, sIncludeRole, sExcludeRole);
        strFinalValue = getSummaryValue(context, mlRA, sView, null, sRAEAttribute, enmValueType, useTitleAttrbiute, sFormat);

        // TIGTK-13594 :START
        if (UIUtil.isNotNullAndNotEmpty(sRiskValue) && sRiskValue.equals("true")) {
            if (strFinalValue.equals("1")) {
                strFinalValue = "High";
            } else if (strFinalValue.equals("2")) {
                strFinalValue = "Medium";
            } else {
                strFinalValue = "Low";
            }
        }
        // TIGTK-13594 :END

        logger.debug("SummaryViewUtil_mxJPO : getSummaryValueForField :End ");
        return strFinalValue;
    }

    /***
     * @author RutujaE
     * @param context
     * @param domIA
     * @param sIncludeRole
     * @param sExcludeRole
     * @return MapList
     * @throws FrameworkException
     */
    private MapList getRAListFRomIA(Context context, DomainObject domIA, String sIncludeRole, String sExcludeRole) throws Exception {
        logger.debug("SummaryViewUtil_mxJPO : getRAListFRomIA :START ");
        MapList mlRoleAssement;
        try {
            String sbWhere = DomainConstants.EMPTY_STRING;
            if (UIUtil.isNotNullAndNotEmpty(sExcludeRole)) {
                sbWhere = "!(attribute[PSS_RARole] matchlist \"" + sIncludeRole + "\" \",\")";
            } else if (UIUtil.isNotNullAndNotEmpty(sIncludeRole)) {
                sbWhere = "attribute[PSS_RARole] matchlist \"" + sIncludeRole + "\" \", \"";
            }
            StringList slObjectSelect = new StringList();
            slObjectSelect.addElement(DomainConstants.SELECT_ID);
            slObjectSelect.addElement(DomainConstants.SELECT_TYPE);

            // TIGTK-13922 : Replaced getRelatedObjects with getExpansionIterator for performance
            short sQueryLimit = 0;
            ContextUtil.startTransaction(context, false);
            ExpansionIterator expIter = domIA.getExpansionIterator(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slObjectSelect, new StringList(0),
                    false, true, (short) 1, sbWhere, "", sQueryLimit, true, false, (short) 100, false);
            mlRoleAssement = FrameworkUtil.toMapList(expIter, sQueryLimit, null, null, null, null);
            expIter.close();
            ContextUtil.commitTransaction(context);
            logger.debug("SummaryViewUtil_mxJPO : getRAListFRomIA :End ");
        } catch (MatrixException ex) {
            logger.error("Error in getRAListFRomIA: ", ex);
            throw ex;
        }
        return mlRoleAssement;
    }

    /***
     * @param context
     * @param mlRA
     * @param sView
     * @param sRAETitle
     * @param sRAEAttribute
     * @param enumValueType
     * @return String
     * @throws FrameworkException
     */
    private String getSummaryValue(Context context, MapList mlRA, String sView, String sRAETitle, String sRAEAttribute, ValueType enumValueType, boolean useTitleAttrbiute, String sFormat)
            throws FrameworkException {
        logger.debug("SummaryViewUtil_mxJPO : getSummaryValue :START ");
        String strAttributeValue = DomainConstants.EMPTY_STRING;
        StringBuilder sbValue = new StringBuilder();
        String sFinalValue = DomainConstants.EMPTY_STRING;
        List<Double> lstNumberValue = new ArrayList<Double>();
        String prefix = DomainConstants.EMPTY_STRING;
        HashSet<String> hsUniqValue = new HashSet<String>();
        for (Object objRA : mlRA) {
            Map<?, ?> mRA = (Map<?, ?>) objRA;
            String strRAId = (String) mRA.get(DomainConstants.SELECT_ID);
            DomainObject domRA = DomainObject.newInstance(context, strRAId);
            MapList mlRAE = getRAEListForSpecificViewAndTitle(context, domRA, sView, sRAETitle, useTitleAttrbiute);
            for (Object objRAE : mlRAE) {
                Map<?, ?> mRAE = (Map<?, ?>) objRAE;
                String strRAEId = (String) mRAE.get(DomainConstants.SELECT_ID);
                DomainObject domRAE = DomainObject.newInstance(context, strRAEId);
                strAttributeValue = domRAE.getAttributeValue(context, sRAEAttribute);

                switch (enumValueType) {
                case CONCAT:
                    // TIGTK-11798 :Start
                    if (UIUtil.isNotNullAndNotEmpty(strAttributeValue)) {
                        sbValue.append(prefix);
                        prefix = System.getProperty("line.separator");
                        sbValue.append(strAttributeValue);
                    }
                    break;
                case COPY:
                    if (UIUtil.isNotNullAndNotEmpty(strAttributeValue)) {
                        if ("integer".equals(sFormat)) {
                            Double nAttributeValue = Double.parseDouble(strAttributeValue);
                            strAttributeValue = Integer.toString(nAttributeValue.intValue());
                        }
                        hsUniqValue.add(strAttributeValue);
                    }
                    // TIGTK-11798 :End
                    break;
                case SUM:
                case AVERAGE:
                case HIGHEST:

                    if (UIUtil.isNotNullAndNotEmpty(strAttributeValue)) {
                        Double nAttributeValue = Double.parseDouble(strAttributeValue);
                        lstNumberValue.add(nAttributeValue);
                    } else
                        lstNumberValue.add(Double.parseDouble("0"));
                    break;
                default:
                    break;
                }
            }
        }

        switch (enumValueType) {
        case CONCAT:
            // TIGTK-11798 :Start
            sFinalValue = sbValue.toString();
            break;
        case COPY:
            String strLineSep = "";
            StringBuffer sbFinalValue = new StringBuffer();
            for (String strValue : hsUniqValue) {
                sbFinalValue.setLength(0);
                if (UIUtil.isNotNullAndNotEmpty(strValue)) {
                    sbFinalValue.append(sFinalValue);
                    sbFinalValue.append(strLineSep);
                    sbFinalValue.append(strValue);
                    strLineSep = "\n";
                }
                sFinalValue = sbFinalValue.toString();
            }
            // TIGTK-11798 :End
            break;
        case SUM:
            if (!lstNumberValue.isEmpty()) {
                Double dblTemp = getSumValue(lstNumberValue);
                if ("integer".equals(sFormat)) {
                    sFinalValue = Integer.toString(dblTemp.intValue());
                } else {
                    sFinalValue = Double.toString(dblTemp);
                }

            }
            break;
        case AVERAGE:
            if (!lstNumberValue.isEmpty()) {
                sFinalValue = Integer.toString(getAverageValue(lstNumberValue));
            }
            break;
        case HIGHEST:
            if (!lstNumberValue.isEmpty()) {
                sFinalValue = Double.toString(getHighestValue(lstNumberValue));
            }
            break;
        default:
            break;
        }
        logger.debug("SummaryViewUtil_mxJPO : getSummaryValue :End ");

        return sFinalValue;
    }

    /****
     * @param context
     * @param domRA
     * @param strView
     * @param strRAETitle
     * @return MapList
     * @throws FrameworkException
     */
    private MapList getRAEListForSpecificViewAndTitle(Context context, DomainObject domRA, String strView, String strRAETitle, boolean useTitleAttrbiute) throws FrameworkException {
        logger.debug("SummaryViewUtil_mxJPO : getRAEListForSpecificViewAndTitle :START ");
        StringList objSelect = new StringList();
        objSelect.add(DomainConstants.SELECT_ID);
        String strRelWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_VIEW + "]==" + strView;
        String strObjectWhere = "";
        String ATTRIBUTE_PSS_DOMAIN_OR_TITLE = TigerConstants.ATTRIBUTE_PSS_DOMAIN;
        if (useTitleAttrbiute) {
            ATTRIBUTE_PSS_DOMAIN_OR_TITLE = TigerConstants.ATTRIBUTE_PSS_TITLE;
        }
        if (UIUtil.isNotNullAndNotEmpty(strRAETitle)) {
            strObjectWhere = "attribute[" + ATTRIBUTE_PSS_DOMAIN_OR_TITLE + "]=='" + strRAETitle + "'";
        }
        MapList mlRAE = domRA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION, TigerConstants.TYPE_PSS_ROLEASSESSMENT_EVALUATION, objSelect, null, false, true,
                (short) 0, strObjectWhere, strRelWhere, 0);
        logger.debug("SummaryViewUtil_mxJPO : getRAEListForSpecificViewAndTitle :END ");
        return mlRAE;
    }

    /***
     * @author RutujaE
     * @param context
     * @param args
     * @return Vector
     * @throws Exception
     */
    public Vector<String> getSummaryValueForTable(Context context, String args[]) throws Exception {
        logger.debug("SummaryViewUtil_mxJPO : getSummaryValueForTable :Start ");
        Map<?, ?> programMap = JPO.unpackArgs(args);
        Map<?, ?> paramList = (Map<?, ?>) programMap.get("paramList");
        Map<?, ?> columnMap = (Map<?, ?>) programMap.get("columnMap");
        Map<?, ?> mpSettings = (Map<?, ?>) columnMap.get("settings");
        String strRAESymbolicAttributeName = (String) mpSettings.get("RAEAttribute");
        String strRAEAttribute = PropertyUtil.getSchemaProperty(context, strRAESymbolicAttributeName);
        String strValueTypeCopy = (String) mpSettings.get("ValueTypeCopy");
        String strExcludeValue = (String) mpSettings.get("ExcludeValue");
        String strIncludeValue = (String) mpSettings.get("IncludeValue");
        // TIGTK-10759: 8/11/2017 : Start
        String strGlobalValueforDomain = (String) mpSettings.get("GlobalValueforDomain");
        // TIGTK-10759: 8/11/2017 : End
        String strFormat = (String) mpSettings.get("format");
        boolean useTitleAttrbiute = false;
        Vector<String> vtReurn = new Vector<String>();

        HashSet<String> hsExcludeValue = new HashSet<String>();
        HashSet<String> hsValueTypeCopy = new HashSet<String>();
        HashSet<String> hsIncludeValue = new HashSet<String>();
        // TIGTK-10759: 8/11/2017 : Start
        HashSet<String> hsGlobalValueforDomain = new HashSet<String>();
        // TIGTK-10759: 8/11/2017 : End

        if (UIUtil.isNotNullAndNotEmpty(strExcludeValue) && UIUtil.isNotNullAndNotEmpty(strIncludeValue)) {
            String errprMsg = "Illigal Use of 'ExcludeValue' and 'IncludeValue' setting. Cannot use both   setting in singal column.";
            logger.error("Error in SummaryViewUtil_mxJPO : getSummaryValueForTable : " + errprMsg);
            throw new Exception(errprMsg);
        }

        if (UIUtil.isNotNullAndNotEmpty(strExcludeValue)) {
            hsExcludeValue.addAll(Arrays.asList(strExcludeValue.split(",")));
        } else if (UIUtil.isNotNullAndNotEmpty(strIncludeValue)) {
            hsIncludeValue.addAll(Arrays.asList(strIncludeValue.split(",")));
        }
        if (UIUtil.isNotNullAndNotEmpty(strValueTypeCopy)) {
            hsValueTypeCopy.addAll(Arrays.asList(strValueTypeCopy.split(",")));
        }
        // TIGTK-10759: 8/11/2017 : Start
        if (UIUtil.isNotNullAndNotEmpty(strGlobalValueforDomain)) {
            hsGlobalValueforDomain.addAll(Arrays.asList(strGlobalValueforDomain.split(",")));
        }
        // TIGTK-10759: 8/11/2017 : End
        String sView = (String) paramList.get("view");
        String sSummaryView = (String) paramList.get("summaryView");
        MapList mplRAE = (MapList) programMap.get("objectList");
        StringList objSelect = new StringList();
        objSelect.add(DomainConstants.SELECT_ID);
        String sRelWhere = DomainConstants.EMPTY_STRING;
        String strRAETitleAsParam = DomainConstants.EMPTY_STRING;

        for (Object objRAE : mplRAE) {
            boolean isContinue = false;
            ValueType enmValueType = ValueType.SUM;
            Map<?, ?> mpRAEObj = (Map<?, ?>) objRAE;
            String sReturnVal = DomainConstants.EMPTY_STRING;
            String sRAEID = (String) mpRAEObj.get(DomainConstants.SELECT_ID);
            DomainObject domRAE = DomainObject.newInstance(context, sRAEID);
            StringList slRAESelect = new StringList();
            slRAESelect.add(TigerConstants.ATTRIBUTE_PSS_TITLE);
            slRAESelect.add(TigerConstants.ATTRIBUTE_PSS_DOMAIN);
            AttributeList attrTitleAndDomainVal = domRAE.getAttributeValues(context, slRAESelect);
            String sRAETitle = ((Attribute) attrTitleAndDomainVal.get(0)).getValue();
            String sRAEDomian = ((Attribute) attrTitleAndDomainVal.get(1)).getValue();
            String sCompareValue = sRAETitle + "~" + sRAEDomian;

            if (hsExcludeValue.contains(sCompareValue) || hsExcludeValue.contains(sRAETitle)) {
                isContinue = true;
            } else if (!hsIncludeValue.isEmpty() && !(hsIncludeValue.contains(sCompareValue) || hsIncludeValue.contains(sRAETitle))) {
                isContinue = true;
            }
            if (isContinue) {
                sReturnVal = "";
                vtReurn.add(sReturnVal);
                continue;
            }
            sRelWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_VIEW + "] ==" + sSummaryView;
            MapList mlRA = domRAE.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION, TigerConstants.TYPE_PSS_ROLEASSESSMENT, objSelect, null, true, false, (short) 0,
                    null, sRelWhere, 0);
            if (hsValueTypeCopy.contains(sRAETitle)) {
                enmValueType = ValueType.COPY;
            }
            // TIGTK-10759: 8/11/2017 : Start
            strRAETitleAsParam = sRAETitle;
            if (hsGlobalValueforDomain.contains(sRAETitle)) {
                strRAETitleAsParam = null;
            }

            sReturnVal = getSummaryValue(context, mlRA, sView, strRAETitleAsParam, strRAEAttribute, enmValueType, useTitleAttrbiute, strFormat);
            // TIGTK-10759: 8/11/2017 : End
            // this code was added for showing total value as total of particular domain

            try {
                if (!(strRAEAttribute.equals("PSS_Resp") || strRAEAttribute.equals("PSS_Comments"))) {
                    if (!"PSS_Total".equals(sRAETitle) && setTotalForDomain.contains(sRAEDomian) && !"".equals(sReturnVal)) {
                        Double dbleTemp = Double.parseDouble(sReturnVal);
                        if (mpTotalValues.containsKey(sRAEDomian)) {
                            Double dblValue = mpTotalValues.get(sRAEDomian);
                            dbleTemp = dblValue + dbleTemp;
                        }
                        mpTotalValues.put(sRAEDomian, dbleTemp);
                    }
                    if ("PSS_Total".equals(sRAETitle)) {
                        Double dbleTemp = mpTotalValues.get(sRAEDomian);
                        if ("integer".equals(strFormat)) {
                            sReturnVal = Integer.toString(dbleTemp.intValue());
                        } else {
                            sReturnVal = Double.toString(dbleTemp);
                        }

                        // reset the value so other column get correct value
                        mpTotalValues.put(sRAEDomian, 0.0);
                    }
                }
            } catch (Exception e) {
                // if sReturnVal contains characters do nothing
                throw e;
            }

            vtReurn.add(sReturnVal);
        }
        logger.debug("SummaryViewUtil_mxJPO : getSummaryValueForTable :END ");
        return vtReurn;
    }

    /***
     * @author RutujaE
     * @param lstNumberValue
     * @return double
     */
    private double getHighestValue(List<Double> lstNumberValue) {
        double dblMax = Collections.max(lstNumberValue);
        return dblMax;
    }

    /***
     * @author RutujaE
     * @param lstNumberValue
     * @return int
     */
    private int getAverageValue(List<Double> lstNumberValue) {
        double dblSum = getSumValue(lstNumberValue);
        double dblAvg = dblSum / lstNumberValue.size();
        int intAvg = (int) Math.round(dblAvg);
        return intAvg;
    }

    /***
     * @author RutujaE
     * @param lstNumberValue
     * @return double
     */
    private double getSumValue(List<Double> lstNumberValue) {
        double dblSum = 0;
        for (double dblTemp : lstNumberValue) {
            dblSum = dblSum + dblTemp;
            dblSum = (double) Math.round(dblSum * 100) / 100;
        }
        return dblSum;
    }

    // PCM2.0 Spr4:TIGTK-6894:13/9/2017:START
    /**
     * @Description This method is used as column function on table column AssessmentVariableCostEstimation,ToolingCostEstimate to display the calculation values
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getSLCSummaryValueForFIS(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.SummaryViewUtil:getSLCSummaryValueForFIS:START");
        try {
            Vector<String> vtReturn = new Vector<String>();
            Map<?, ?> programMap = JPO.unpackArgs(args);
            Map<?, ?> columnMap = (Map<?, ?>) programMap.get("columnMap");
            Map<?, ?> mpSettings = (Map<?, ?>) columnMap.get("settings");
            boolean useTitleAttrbiute = false;
            String sExcludeValue = (String) mpSettings.get("ExcludeValue");
            String sIncludeValue = (String) mpSettings.get("IncludeValue");
            String strFormat = DomainConstants.EMPTY_STRING;
            String sOIASummaryExpression = (String) mpSettings.get("OIASummaryExpression");
            String sValueTypeCopy = (String) mpSettings.get("ValueTypeCopy");
            ValueType enmValueType = ValueType.SUM; // by default Value Type is SUM

            String sView = (String) mpSettings.get("View");
            String sSummaryView = (String) mpSettings.get("summaryView");
            MapList mplCR = (MapList) programMap.get("objectList");
            StringList sSplitValue = FrameworkUtil.split(sOIASummaryExpression, ".");
            String sRAETitleFromSettings = (String) sSplitValue.get(0);
            String sColumnName = (String) sSplitValue.get(1);
            HashSet<String> hsExcludeValue = new HashSet<String>();
            HashSet<String> hsIncludeValue = new HashSet<String>();
            HashSet<String> hsValueTypeCopy = new HashSet<String>();

            if (UIUtil.isNotNullAndNotEmpty(sExcludeValue) && UIUtil.isNotNullAndNotEmpty(sIncludeValue)) {
                throw new Exception("Illigal Use of 'ExcludeValue' and 'IncludeValue' setting. Cannot use both setting in singal column.");
            }

            if (UIUtil.isNotNullAndNotEmpty(sExcludeValue)) {
                hsExcludeValue.addAll(Arrays.asList(sExcludeValue.split(",")));
            }

            if (UIUtil.isNotNullAndNotEmpty(sIncludeValue)) {
                hsIncludeValue.addAll(Arrays.asList(sIncludeValue.split(",")));
            }
            if (UIUtil.isNotNullAndNotEmpty(sValueTypeCopy)) {
                hsValueTypeCopy.addAll(Arrays.asList(sValueTypeCopy.split(",")));
            }

            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);

            String sRelWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_VIEW + "]==" + sSummaryView;
            String sObjWhere = "";
            if (!"PSS_Global".equals(sRAETitleFromSettings)) {
                sObjWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]==" + sRAETitleFromSettings;
            }

            for (Object objCR : mplCR) {
                Map<?, ?> mpCRObj = (Map<?, ?>) objCR;
                String sFinalValue = "NA";
                String strIAObjectId = (String) mpCRObj.get("LatestRevIAObjectId");
                if (!strIAObjectId.isEmpty()) {
                    MapList mlRAE = pss.ecm.impactanalysis.ImpactAnalysisUtil_mxJPO.getRAEFromIA(context, strIAObjectId, sRelWhere, sObjWhere);
                    double dblTempSumValue = 0;

                    for (Object objRAE : mlRAE) {
                        Map<?, ?> mpRAEObj = (Map<?, ?>) objRAE;
                        boolean isContinue = false;
                        String sRAEID = (String) mpRAEObj.get(DomainConstants.SELECT_ID);
                        DomainObject domRAE = DomainObject.newInstance(context, sRAEID);
                        String sRAETitle = (String) mpRAEObj.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]");
                        String sRAEDomian = (String) mpRAEObj.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]");

                        String sCompareValue = sRAETitle + "~" + sRAEDomian;

                        if (!hsExcludeValue.isEmpty() && !hsIncludeValue.isEmpty()) {
                            if (hsExcludeValue.contains(sCompareValue) || hsExcludeValue.contains(sRAETitle)) {
                                isContinue = true;
                            } else if (!hsIncludeValue.contains(sCompareValue) || !hsIncludeValue.contains(sRAETitle)) {
                                isContinue = true;
                            }
                            if (isContinue) {
                                dblTempSumValue = 0;
                                // vtReturn.add("");
                                continue;
                            }
                        }

                        MapList mlRA = domRAE.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION, TigerConstants.TYPE_PSS_ROLEASSESSMENT, objSelect, null, true,
                                false, (short) 1, null, sRelWhere, 0);

                        String sReturnVal = getSummaryValue(context, mlRA, sView, sRAETitle, sColumnName, enmValueType, useTitleAttrbiute, strFormat);
                        if ("".equals(sReturnVal)) {
                            sReturnVal = "0.0";
                        }
                        dblTempSumValue = dblTempSumValue + Double.parseDouble(sReturnVal);
                        sFinalValue = Double.toString(dblTempSumValue);
                    } // End of RAE Loop

                }
                vtReturn.add(sFinalValue);
            } // End of CR Loop
            return vtReturn;

        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.SummaryViewUtil:getSLCSummaryValueForFIS:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * @Description This method is used as column function on table column BOPCostsEstimation,FreightOutCostsEstimation,ScrapEstimation,CapexEstimation, LaunchCostsEstimation, RMCostEstimation,
     *              DirectLaborCostsEstimation, TuningCostsEstimation to display the calculation values.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> evalSLCOIASummaryExpression(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.SummaryViewUtil:evalSLCOIASummaryExpression:START");
        try {
            ValueType enmValueType = ValueType.SUM;
            Vector<String> vtReturn = new Vector<String>();
            Map<?, ?> programMap = JPO.unpackArgs(args);
            Map<?, ?> columnMap = (Map<?, ?>) programMap.get("columnMap");
            Map<?, ?> mpSettings = (Map<?, ?>) columnMap.get("settings");
            MapList mplCR = (MapList) programMap.get("objectList");

            String sOIAExpression = (String) mpSettings.get("OIASummaryExpression");
            String strFormat = DomainConstants.EMPTY_STRING;
            boolean arithmeticEvalRequired = false;
            boolean useTitleAttrbiute = true;
            if (sOIAExpression.contains(",")) {
                arithmeticEvalRequired = true;
            }
            StringList slKey = FrameworkUtil.split(sOIAExpression, ",");
            String sView = (String) mpSettings.get("View");
            String sSLCCalculationTitle = (String) mpSettings.get("SLCCalculationTitle");

            for (Object objCR : mplCR) {
                String sFinalValue = "NA";
                Map<?, ?> mpCRObj = (Map<?, ?>) objCR;
                String strIAObjectId = (String) mpCRObj.get("LatestRevIAObjectId");
                if (!strIAObjectId.isEmpty()) {
                    DomainObject domIA = DomainObject.newInstance(context, strIAObjectId);

                    Map<String, String> mpExpressionsVsSummaryValue = new HashMap<String, String>();
                    for (int i = 0; i < slKey.size(); i++) {
                        String sKey = (String) slKey.get(i);
                        StringList slKeyValues = FrameworkUtil.split(sKey, ".");
                        String sIncludeRole = (String) slKeyValues.get(0);
                        String sRAETitle = (String) slKeyValues.get(1);
                        String sRAEAttribute = (String) slKeyValues.get(2);

                        MapList mlRA = getRAListFRomIA(context, domIA, sIncludeRole, "");
                        String strFinalValue = getSummaryValue(context, mlRA, sView, sRAETitle, sRAEAttribute, enmValueType, useTitleAttrbiute, strFormat);
                        mpExpressionsVsSummaryValue.put(sKey, strFinalValue);
                    } // End of slKey loop

                    if (arithmeticEvalRequired == true) {
                        String sArithmeticExpression = EnoviaResourceBundle.getProperty(context, "EnterpriseChangeMgt", context.getLocale(),
                                "PSS_EnterpriseChangeMgt.SLC.Arithmetic." + sSLCCalculationTitle);
                        for (Map.Entry<String, String> entry : mpExpressionsVsSummaryValue.entrySet()) {
                            String strExpressionAttribute = entry.getKey();
                            String strSummaryValue = entry.getValue();
                            if (strSummaryValue.isEmpty()) {
                                strSummaryValue = "0";
                            }
                            String sTemp = "${" + strExpressionAttribute + "}";
                            sArithmeticExpression = sArithmeticExpression.replace(sTemp, strSummaryValue);
                        }

                        sFinalValue = pss.slc.util.InfixExprEvaluator_mxJPO.evaluate(sArithmeticExpression);

                    } // End of flag loop
                    else {
                        String sFirstKey = mpExpressionsVsSummaryValue.keySet().iterator().next();
                        String sValue = mpExpressionsVsSummaryValue.get(sFirstKey);
                        if ("".equals(sValue)) {
                            sValue = "Role Assessment Not Available";
                        }
                        sFinalValue = sValue;
                    }
                }
                vtReturn.add(sFinalValue);

            } // End of CR Loop
            return vtReturn;

        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.SummaryViewUtil:evalSLCOIASummaryExpression:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * @Description This method is used as column function on table column TotalUpfrontCosts and NetUpfront to display the calculation values.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> evalSLCExpression(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.SummaryViewUtil:evalSLCExpression:START");
        Vector<String> vtReturn = new Vector<String>();
        try {

            Map<?, ?> programMap = JPO.unpackArgs(args);
            Map<?, ?> columnMap = (Map<?, ?>) programMap.get("columnMap");
            Map<?, ?> mpSettings = (Map<?, ?>) columnMap.get("settings");
            MapList mplCR = (MapList) programMap.get("objectList");
            ValueType enmValueType = ValueType.SUM; // by default Value Type is SUM
            boolean useTitleAttrbiute = true;
            String sView = (String) mpSettings.get("View");
            String sSLCExpression = (String) mpSettings.get("SLCExpression");
            String sSLCCalculationTitle = (String) mpSettings.get("SLCCalculationTitle");
            String strFormat = DomainConstants.EMPTY_STRING;
            StringList slSLCExpression = FrameworkUtil.split(sSLCExpression, ",");
            StringList slSLCAttributes = new StringList();
            StringList slOIAExpression = new StringList();

            for (int i = 0; i < slSLCExpression.size(); i++) {
                String sExpression = (String) slSLCExpression.get(i);

                if (!sExpression.contains(".")) {
                    slSLCAttributes.add(sExpression);
                } else {
                    slOIAExpression.add(sExpression);
                }

            }

            for (Object objCR : mplCR) {
                Map<?, ?> mpCRObj = (Map<?, ?>) objCR;
                String sCRID = (String) mpCRObj.get(DomainConstants.SELECT_ID);
                BusinessObject domCR = new BusinessObject(sCRID);
                String sFinalValue = "0.0";
                String strIAObjectId = (String) mpCRObj.get("LatestRevIAObjectId");

                AttributeList attrList = domCR.getAttributeValues(context, slSLCAttributes);
                AttributeItr attrItr = new AttributeItr(attrList);
                String sArithmeticExpression = EnoviaResourceBundle.getProperty(context, "EnterpriseChangeMgt", context.getLocale(), "PSS_EnterpriseChangeMgt.SLC.Arithmetic." + sSLCCalculationTitle);
                while (attrItr.next()) {
                    Attribute objAttribute = attrItr.obj();
                    String strAttributeName = objAttribute.getName().trim();
                    String strAttributeValue = objAttribute.getValue().trim();
                    if ("".equals(strAttributeValue)) {
                        strAttributeValue = "0";
                    }
                    String sTemp = "${" + strAttributeName + "}";
                    sArithmeticExpression = sArithmeticExpression.replace(sTemp, strAttributeValue);
                }
                if (!strIAObjectId.isEmpty()) {
                    DomainObject domIA = DomainObject.newInstance(context, strIAObjectId);
                    for (int i = 0; i < slOIAExpression.size(); i++) {
                        String sExpression = (String) slOIAExpression.get(i);

                        StringList slExpressionValues = FrameworkUtil.split(sExpression, ".");
                        String sIncludeRole = (String) slExpressionValues.get(0);
                        String sRAETitle = (String) slExpressionValues.get(1);
                        String sRAEAttribute = (String) slExpressionValues.get(2);

                        MapList mlRA = getRAListFRomIA(context, domIA, sIncludeRole, "");
                        String strFinalValue = getSummaryValue(context, mlRA, sView, sRAETitle, sRAEAttribute, enmValueType, useTitleAttrbiute, strFormat);
                        if ("".equals(strFinalValue)) {
                            strFinalValue = "0";
                        }
                        String sTemp1 = "${" + sExpression + "}";
                        sArithmeticExpression = sArithmeticExpression.replace(sTemp1, strFinalValue);
                    } // End of slOIAExpression loop
                    sFinalValue = pss.slc.util.InfixExprEvaluator_mxJPO.evaluate(sArithmeticExpression);

                }
                if (!sArithmeticExpression.contains("{")) {
                    sFinalValue = pss.slc.util.InfixExprEvaluator_mxJPO.evaluate(sArithmeticExpression);
                } else {
                    sFinalValue = "Role Assessment Not Available";

                }
                vtReturn.add(sFinalValue);

            } // End of CR loop

        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.SummaryViewUtil:evalSLCExpression:ERROR ", ex);
            throw ex;
        }
        return vtReturn;
    }

    // PCM2.0 Spr4:TIGTK-6894:13/9/2017:END

    // PCM2.0 Spr4:TIGTK-6897:13/9/2017:START
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getSLCSummaryValueForFCM(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.SummaryViewUtil:getSLCSummaryValueForFCM:START");
        Vector<String> vtReturn = new Vector<String>();
        try {

            Map<?, ?> programMap = JPO.unpackArgs(args);

            Map<?, ?> columnMap = (Map<?, ?>) programMap.get("columnMap");
            Map<?, ?> mpSettings = (Map<?, ?>) columnMap.get("settings");

            String sRAESymbolicAttributeName = (String) mpSettings.get("RAEAttribute");
            String sRAEAttribute = PropertyUtil.getSchemaProperty(context, sRAESymbolicAttributeName);
            String sIncludeRole = (String) mpSettings.get("IncludeRole");
            String sExcludeRole = (String) mpSettings.get("ExcludeRole");
            String sView = (String) mpSettings.get("View");
            String strFormat = DomainConstants.EMPTY_STRING;
            boolean useTitleAttrbiute = true;
            MapList mplCR = (MapList) programMap.get("objectList");

            ValueType enmValueType = ValueType.SUM;

            for (Object objCR : mplCR) {
                Map<?, ?> mpCRObj = (Map<?, ?>) objCR;

                String strIAObjectId = (String) mpCRObj.get("LatestRevIAObjectId");
                String strFinalValue = "";
                // TIGTK-14786: stembulkar : start
                if (UIUtil.isNotNullAndNotEmpty(strIAObjectId)) {
                    // TIGTK-14786: stembulkar : end

                    DomainObject domIA = DomainObject.newInstance(context, strIAObjectId);

                    MapList mlRA = getRAListFRomIA(context, domIA, sIncludeRole, sExcludeRole);

                    strFinalValue = getSummaryValue(context, mlRA, sView, null, sRAEAttribute, enmValueType, useTitleAttrbiute, strFormat);

                }
                vtReturn.add(strFinalValue);

            }

        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.SummaryViewUtil:getSLCSummaryValueForFCM:ERROR ", ex);
            throw ex;
        }
        return vtReturn;
    }

    /**
     * @Description This method is used as column function on table column Weeks to Implement to display the calculation values
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getSLCWeeksToImplementForFCM(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.SummaryViewUtil:getSLCWeeksToImplementForFCM:START");
        try {

            Vector<String> vtReturn = new Vector<String>();
            Map<?, ?> programMap = JPO.unpackArgs(args);
            Map<?, ?> columnMap = (Map<?, ?>) programMap.get("columnMap");
            Map<?, ?> mpSettings = (Map<?, ?>) columnMap.get("settings");
            MapList mplCR = (MapList) programMap.get("objectList");
            ValueType enmValueType = ValueType.SUM;
            boolean useTitleAttrbiute = true;
            String sOIAExpression = (String) mpSettings.get("OIAExpression");
            StringList slKey = FrameworkUtil.split(sOIAExpression, ",");
            String sSLCCalculationTitle = (String) mpSettings.get("SLCCalculationTitle");
            String sView = (String) mpSettings.get("View");
            String strFormat = DomainConstants.EMPTY_STRING;
            for (Object objCR : mplCR) {
                Map<?, ?> mpCRObj = (Map<?, ?>) objCR;
                String strIAObjectId = (String) mpCRObj.get("LatestRevIAObjectId");
                String sFinalValue = "";
                if (!strIAObjectId.isEmpty()) {
                    DomainObject domIA = DomainObject.newInstance(context, strIAObjectId);

                    Map<String, String> mpExpressionsVsSummaryValue = new HashMap<String, String>();

                    for (int i = 0; i < slKey.size(); i++) {
                        String sRAEAttribute = (String) slKey.get(i);
                        String sRoles = EnoviaResourceBundle.getProperty(context, "EnterpriseChangeMgt", context.getLocale(), "PSS_EnterpriseChangeMgt.FCMSum." + sRAEAttribute);
                        String sIncludeRole = "";
                        String sExcludeRole = "";
                        if ("true".equals(String.valueOf(sRoles.startsWith("!")))) {
                            sExcludeRole = sRoles;
                        } else {
                            sIncludeRole = sRoles;
                        }

                        MapList mlRA = getRAListFRomIA(context, domIA, sIncludeRole, sExcludeRole);
                        String strFinalValue = getSummaryValue(context, mlRA, sView, null, sRAEAttribute, enmValueType, useTitleAttrbiute, strFormat);
                        mpExpressionsVsSummaryValue.put(sRAEAttribute, strFinalValue);

                    } // End of slKey Loop
                    String sArithmeticExpression = EnoviaResourceBundle.getProperty(context, "EnterpriseChangeMgt", context.getLocale(),
                            "PSS_EnterpriseChangeMgt.SLC.Arithmetic." + sSLCCalculationTitle);
                    for (Map.Entry<String, String> entry : mpExpressionsVsSummaryValue.entrySet()) {

                        String strExpressionAttribute = entry.getKey();
                        String strSummaryValue = entry.getValue();
                        if (strSummaryValue.isEmpty()) {
                            strSummaryValue = "0";
                        }
                        String sTemp = "${" + strExpressionAttribute + "}";
                        sArithmeticExpression = sArithmeticExpression.replace(sTemp, strSummaryValue);
                    }

                    sFinalValue = pss.slc.util.InfixExprEvaluator_mxJPO.evaluate(sArithmeticExpression);

                }
                vtReturn.add(sFinalValue);
            } // End of CR Loop
            return vtReturn;

        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.SummaryViewUtil:getSLCWeeksToImplementForFCM:ERROR ", ex);
            throw ex;
        }
    }
    // PCM2.0 Spr4:TIGTK-6897:13/9/2017:END
}