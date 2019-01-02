
/*
 ** emxFTRPartBase Copyright (c) 1993-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes. Copyright notice is precautionary
 * only and does not evidence any actual or intended publication of such program JPO is Introduced for Ticket TIGTK-6787 | Harika Varanasi | 09/06/2017
 */

import java.util.HashMap;
import java.util.Map;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.apps.configuration.ConfigurationConstants;
import com.matrixone.apps.configuration.ConfigurationUtil;
import com.matrixone.apps.configuration.LogicalFeature;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.Job;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.productline.ProductLineCommon;
import com.matrixone.apps.productline.ProductLineConstants;

public class PSS_emxFTRPart_mxJPO extends emxPLCPart_mxJPO {

    /** A string constant with the value COMMA:",". */
    public static final String STR_COMMA = ",";

    protected static final String PART_ACTIVE_STATUS = "emxProduct.Part.ActiveStatus";

    protected static final String PART_INACTIVE_STATUS = "emxProduct.Part.InactiveStatus";

    protected static final String SIMPLE_INCLUSION_RULE_KEY = "emxProduct.RuleCreation.SimpleInclusion";

    protected static final String COMPLEX_INCLUSION_RULE_KEY = "emxProduct.RuleCreation.ComplexInclusion";

    protected static final String SUITE_KEY = "Configuration";

    // TIGTK-8934 | 07/07/2017 |Harika Varanasi : starts

    public static final short REL_SELECT_ACTIVE = 0;

    public static final short REL_SELECT_PART_INACTIVE = 1;

    public static final short REL_SELECT_DV_INACTIVE = 2;

    public static final short REL_SELECT_GBOM_SUMMARY = 3;

    public static final short REL_SELECT_ALL = 4;

    // TIGTK-8934 | 07/07/2017 |Harika Varanasi : ends

    /**
     * Default Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since ProductCentral 10.0.0.0
     * @grade 0
     */
    public PSS_emxFTRPart_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
        // Added for BackGround Job

    }

    /**
     * This will return all GBOM connected to the context i.e.Active and Inactive.
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author for Ticket TIGTK-6787 | Harika Varanasi | 09/06/2017
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getCompleteGBOMStructure(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strParentId = (String) programMap.get("objectId");

        LogicalFeature compFtr = new LogicalFeature(strParentId);

        String relWhere = DomainObject.EMPTY_STRING;
        String objWhere = DomainObject.EMPTY_STRING;
        // Obj and Rel pattern
        String typePattern = DomainObject.EMPTY_STRING;
        String relPattern = ConfigurationConstants.RELATIONSHIP_GBOM;

        String strCustomPartMode = EnoviaResourceBundle.getProperty(context, "emxConfiguration.PreviewBOM.EnableCustomPartMode");
        StringBuffer sbRelPattern = new StringBuffer(relPattern);
        if (ProductLineCommon.isNotNull(strCustomPartMode) && strCustomPartMode.equalsIgnoreCase("true")) {
            sbRelPattern.append(",");
            sbRelPattern.append(ConfigurationConstants.RELATIONSHIP_CUSTOM_GBOM);
        }

        // Obj and Rel Selects
        StringList objSelects = getGBOMObjectSelects();
        objSelects.addElement("evaluate[last==revision||last.previous==revision]");
        StringList relSelects = getGBOMRelationshipSelects(REL_SELECT_GBOM_SUMMARY);

        int iLevel = ConfigurationUtil.getLevelfromSB(context, args);
        String filterExpression = (String) programMap.get("CFFExpressionFilterInput_OID");

        // retrieve Active Inactive GBOM
        MapList objectList = compFtr.getGBOMStructure(context, typePattern, sbRelPattern.toString(), objSelects, relSelects, false, true, iLevel, 0, objWhere, relWhere,
                DomainObject.FILTER_STR_AND_ITEM, filterExpression);

        objectList.sortStructure(context, DomainConstants.SELECT_NAME, DomainConstants.SELECT_REVISION, "descending", "String");
        int mlSize = objectList.size();
        MapList mlReturnList = new MapList();
        for (int i = 0; i < mlSize; i++) {
            Map mpGBOMPart = (Map) objectList.get(i);
            String strLastTwoRevObject = (String) mpGBOMPart.get("evaluate[last==revision||last.previous==revision]");
            if ("TRUE".equals(strLastTwoRevObject)) {
                mlReturnList.add(mpGBOMPart);
            }
        }
        objectList = mlReturnList;

        return objectList;
    }

    /**
     * return Object Selects
     * @return
     * @author for Ticket TIGTK-6787 | Harika Varanasi | 09/06/2017
     */
    private StringList getGBOMObjectSelects() {
        StringList objSelects = new StringList();
        objSelects.add(DomainConstants.SELECT_OWNER);
        objSelects.add("from[" + ConfigurationConstants.RELATIONSHIP_GBOM + "].to.name");
        return objSelects;
    }

    /**
     * return Relationship Selects
     * @return
     * @author for Ticket TIGTK-6787 | Harika Varanasi | 09/06/2017
     */
    @SuppressWarnings("unchecked")
    private StringList getGBOMRelationshipSelects(int relSelect) {
        StringList relSelects = new StringList(DomainRelationship.SELECT_ID);
        String dvselectable = "from.from[" + ConfigurationConstants.RELATIONSHIP_VARIES_BY + "].to.id";
        String dvnameselectable = "from.from[" + ConfigurationConstants.RELATIONSHIP_VARIES_BY + "].to.name";
        String inactivedvselectable = "from.from[" + ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY + "].to.id";
        String inactivedvselectable2 = "tomid[" + ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY_GBOM + "].fromrel.to.name";

        if (REL_SELECT_ACTIVE == relSelect) {
            relSelects.add(DomainRelationship.SELECT_FROM_TYPE);
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.id");
            relSelects.add(dvselectable);
            relSelects.add(dvnameselectable);
            relSelects.add(DomainRelationship.SELECT_FROM_ID);
            relSelects.add("attribute[" + ConfigurationConstants.ATTRIBUTE_RULE_TYPE + "]");
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_RULE_COMPLEXITY + "]");
            String dvList = "tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_DESIGNVARIANTS + "]";
            relSelects.add(dvList);
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_RIGHT_EXPRESSION + "]");
            relSelects.add(ConfigurationConstants.SELECT_ATTRIBUTE_RULE_TYPE);

        } else if (REL_SELECT_PART_INACTIVE == relSelect) {
            relSelects.add(inactivedvselectable);
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_RULE_COMPLEXITY + "]");
            relSelects.add("frommid[" + ConfigurationConstants.RELATIONSHIP_REPLACED_BY + "].torel.to.name");
            relSelects.add("frommid[" + ConfigurationConstants.RELATIONSHIP_REPLACED_BY + "].frommid[" + ConfigurationConstants.RELATIONSHIP_AUTHORIZING_EC + "].to.name");
            relSelects.add("attribute[" + ConfigurationConstants.ATTRIBUTE_RULE_TYPE + "]");
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.id");
            relSelects.add(DomainRelationship.SELECT_FROM_ID);
            relSelects.add(DomainRelationship.SELECT_FROM_TYPE);
        } else if (REL_SELECT_DV_INACTIVE == relSelect) {
            relSelects.add(inactivedvselectable);
            DomainRelationship.MULTI_VALUE_LIST.add(inactivedvselectable2);
            relSelects.add(inactivedvselectable2);
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_RULE_COMPLEXITY + "]");
            relSelects.add("attribute[" + ConfigurationConstants.ATTRIBUTE_RULE_TYPE + "]");
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.id");
            relSelects.add(DomainRelationship.SELECT_FROM_ID);
            relSelects.add(DomainRelationship.SELECT_FROM_TYPE);
        } else if (REL_SELECT_GBOM_SUMMARY == relSelect) {
            relSelects.add(DomainRelationship.SELECT_FROM_ID);
            relSelects.add(DomainRelationship.SELECT_FROM_TYPE);
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.id");
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_LEFT_EXPRESSION + "]");
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_RIGHT_EXPRESSION + "]");
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_RULE_COMPLEXITY + "]");
            relSelects.add("attribute[" + ConfigurationConstants.ATTRIBUTE_RULE_TYPE + "]");
            relSelects.add("tomid[" + ConfigurationConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.type");
            relSelects.add(dvselectable);

        } else if (REL_SELECT_ALL == relSelect || relSelect > REL_SELECT_GBOM_SUMMARY) {
            relSelects.add("attribute[" + ConfigurationConstants.ATTRIBUTE_RULE_TYPE + "]");
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_RULE_COMPLEXITY + "]");
            String dvList = "tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_DESIGNVARIANTS + "]";
            relSelects.add(dvList);
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.id");
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_LEFT_EXPRESSION + "]");
            relSelects.add("tomid[" + ProductLineConstants.RELATIONSHIP_LEFT_EXPRESSION + "].from.attribute[" + ConfigurationConstants.ATTRIBUTE_RIGHT_EXPRESSION + "]");
            relSelects.add(DomainRelationship.SELECT_FROM_ID);
            relSelects.add(dvselectable);
            relSelects.add(inactivedvselectable);
            //
            DomainRelationship.MULTI_VALUE_LIST.add("from.from[" + ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY + "].to.name");
            relSelects.add("from.from[" + ConfigurationConstants.RELATIONSHIP_INACTIVE_VARIES_BY + "].to.name");
            relSelects.add("frommid[" + ConfigurationConstants.RELATIONSHIP_REPLACED_BY + "].torel.to.name");
            relSelects.add("frommid[" + ConfigurationConstants.RELATIONSHIP_REPLACED_BY + "].frommid[" + ConfigurationConstants.RELATIONSHIP_AUTHORIZING_EC + "].to.name");

        }

        return relSelects;
    }

}
