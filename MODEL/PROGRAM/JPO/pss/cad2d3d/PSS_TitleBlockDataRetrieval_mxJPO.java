package pss.cad2d3d;

/**
 * This JPO class use for Data retrieval logic in RE-Generation of Title Block Development
 * @author : Prakash BALKAWADE : PSI
 * @version : 2.0.7 - Title Block
 * @created : Oct 2018
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.State;
import matrix.db.StateList;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.common.utils.TigerAppConfigProperties;
import pss.common.utils.TigerUtils;
import pss.constants.TigerConstants;

public class PSS_TitleBlockDataRetrieval_mxJPO {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_TitleBlockDataRetrieval_mxJPO.class);

    private static final String STRING_MSGLASTREV = "****** LAST REVISION ******";

    private static final String STRING_DATEAUTHOR = "DATEAUTHOR";

    private static final String STRING_NUMBER = "NUMBER";

    private static final String STRING_SEPERATOR_COMMATAB = " ,\t ";

    private static final String STRING_LEAD = "Lead";

    private static final String STRING_INVALID_CHOICE = "Invalid choice";

    private static final String STRING_FORRELEASE = "For Release";

    private static final String STRING_ACTUAL = "actual";

    private static final String STRING_FIRST_ID = "first.id";

    private static final String STRING_ISLAST = "islast";

    private static final String STRING_PROJECT = "project";

    private static final String STRING_FAURECIA_LOGO_RVBJPG = "Faurecia_logo-RVB.jpg";

    private static final String STRING_FIRSTANGLEJPG = "First_Angle.jpg";

    private static final String STRING_THIRDANGLEJPG = "Third_Angle.jpg";

    private static final String STRING_BR = "<br>";

    private static final String STRING_BRBR = "<br><br>";

    private static final String STRING_MM = " mm";

    private static final String STRING_US = "US";

    private static final String STRING_DEG = "&deg;";

    private static final String STRING_PLUSMN = "&plusmn; ";

    private static final String STRING_DESCRIPTION = "DESCRIPTION";

    private static final String STRING_AUTHOR = "AUTHOR";

    private static final String STRING_DATEOFCHANGE = "DATEOFCHANGE";

    private static final String STRING_REVISION = "REVISION";

    private static final String STRING_DATEFORMATDMMYY = "dMMMyy";

    private static final String STRING_DATEFORMAT = "M/dd/yyyy h:mm:ss a";

    private static final String STRING_CURRENTACTUAL = "current.actual";

    private static final String STRING_CHANGEDESCRIPTION = "CHANGEDESCRIPTION";

    private static final String STRING_BASISDEF = "BASISDEF";

    private static final String STRING_SEPERATOR_DOT = "...";

    private static final String STRING_HISTORYBLOCKCELLSEQUENCE = ".HISTORYBLOCK.CELL.SEQUENCE";

    private static final String STRING_BASICDEFINITIONCELLSEQUENCE = ".BASICDEFINITION.CELL.SEQUENCE";

    private static final String LINKEDPARTBLOCKCELLSEQUENCE = ".LINKEDPARTBLOCK.CELL.SEQUENCE";

    private static final String STRING_TREATMENTSAFETY1 = "TREATMENTSAFETY1";

    private static final String STRING_TREATMENTSAFETY2 = "TREATMENTSAFETY2";

    private static final String STRING_TREATMENT = "TREATMENT";

    private static final String STRING_SEMIMANUFACTURSTANDARD = "SEMIMANUFACTURSTANDARD";

    private static final String STRING_MATERIALSAFETY1 = "MATERIALSAFETY1";

    private static final String STRING_MATERIALSAFETY2 = "MATERIALSAFETY2";

    private static final String STRING_MATERIALSTANDARD = "MATERIALSTANDARD";

    private static final String STRING_SAFETY1 = "SAFETY1";

    private static final String STRING_SAFETY2 = "SAFETY2";

    private static final String STRING_X2 = "X2";

    private static final String STRING_MIRRORED = "MIRRORED";

    private static final String STRING_SPACE_Tab = "\n\t";

    private static final String STRING_MASS = "MASS";

    private static final String STRING_PART = "PART";

    private static final String List = "List";

    private static final String XML = "XML";

    private static final String STRING_LTBRGT_SPACE = "&lt;br/&gt; ";

    private static final String String_LTBRGT = "&lt;br/&gt;";

    private static final String RULSBLOCK = "RENAULTNISSAN.ULSBLOCK";

    private static final String RCADBLOCK = "RENAULTNISSAN.CADBLOCK";

    private static final String RENOTB = "RENAULTNISSAN.RENOTB";

    private static final String RLINKEDPARTBLOCK = "RENAULTNISSAN.LINKEDPARTBLOCK";

    private static final String RHISTORYBLOCK = "RENAULTNISSAN.HISTORYBLOCK";

    private static final String BULSBLOCK = "BASISDEFINITION.ULSBLOCK";

    private static final String HISTORYBLOCK = "FASFAE.HISTORYBLOCK";

    private static final String LINKEDPARTBLOCK = "FASFAE.LINKEDPARTBLOCK";

    private static final String BASICDEFINITION = "FASFAE.BASICDEFINITION";

    private static final String CADBLOCK = "FASFAE.CADBLOCK";

    private static final String ULSBLOCK = "FASFAE.ULSBLOCK";

    private static final String DRAWINGBANNER = "FCM.DRAWINGBANNER";

    private static final String BHISTORYBLOCK = "BASISDEFINITION.HISTORYBLOCK";

    private static final String BCADBLOCK = "BASISDEFINITION.CADBLOCK";

    // XML
    private static final String TAG_BASISDEFINITIONDESCRIPTION_START = "<BASISDEFINITIONDESCRIPTION>";

    private static final String TAG_BASISDEFNAME_START = "<BASISDEFNAME>";

    private static final String BASISDEFNAME = "BASIS DEF NAME";

    private static final String TAG_BASISDEFNAME_END = "</BASISDEFNAME>";

    private static final String TAG_BASISDEFREVISION_START = "<BASISDEFREVISION>";

    private static final String BASISDEFREVISION = "BASIS DEF REVISION";

    private static final String TAG_BASISDEFREVISION_END = "</BASISDEFREVISION>";

    private static final String TAG_BASISDEFINITION_START = "<BASISDEFINITION>";

    private static final String BASISDEFINITION = "BASIS DEF.";

    private static final String TAG_BASISDEFINITION_END = "</BASISDEFINITION>";

    private static final String TAG_STATUS_START = "<STATUS>";

    private static final String STATUS = "STATUS";

    private static final String TAG_STATUS_END = "</STATUS>";

    private static final String TAG_DESCRIPTION_START = "<DESCRIPTION>";

    private static final String DESCRIPTION = " CHANGE DESCRIPTION ";

    private static final String TAG_DESCRIPTION_END = "</DESCRIPTION>";

    private static final String TAG_BASISDEFINITIONDESCRIPTION_END = "</BASISDEFINITIONDESCRIPTION>";

    private static final String XML_VERSION = "<?xml version=\"1.0\"?>";

    private static final String TAG_TITLEBLOCK_START = "<TITLEBLOCK>";

    private static final String TAG_TITLEBLOCK_END = "</TITLEBLOCK>";

    private static final String TAG_PATTERN_START = "<PATTERN>";

    private static final String TAG_PATTERN_END = "</PATTERN>";

    /**
     * Constructor
     * @param context
     *            - enovia context object
     * @param args
     *            - holds no arguments.
     * @throws Exception
     *             - if the operation fails.
     */
    public PSS_TitleBlockDataRetrieval_mxJPO(Context context, String[] args) throws Exception {

    }

    /**
     * This method will generate Data and XML file for Title Block.
     * @param context
     * @param domObject
     * @param args
     * @param strCheckinDir
     * @param strXMLFileName
     * @throws Exception
     * @author PSI
     */
    public HashMap<String, List<String[]>> generateTitleBlockData(Context context, DomainObject domObject, String[] args, String strCheckinDir, String strXMLFileName) throws Exception {
        String strPattern = args[1];
        String strDrawinghistory = args[2];
        String strNoRevision = args[3];
        String strLinkedParts = args[4];
        String strUpperLevelStructure = args[5];

        HashMap<String, List<String[]>> mapOfTitleBlockData = new HashMap<String, List<String[]>>();

        List<String[]> listOfHistory = new ArrayList<String[]>();
        List<String[]> listOfLinkedPart = new ArrayList<String[]>();
        List<String[]> listOfBasisDefinition = new ArrayList<String[]>();
        List<String[]> listOfCADBlock = new ArrayList<String[]>();
        List<String[]> listOfULSBlock = new ArrayList<String[]>();
        List<String[]> listOfDrawingBannerFCM = new ArrayList<String[]>();
        List<String[]> listOfBasisDefHistory = new ArrayList<String[]>();
        List<String[]> listOfRenaultNissan = new ArrayList<String[]>();

        StringBuilder sbHistory = new StringBuilder(64);
        StringBuilder sbLinkedPart = new StringBuilder(64);
        StringBuilder sbBasisDefinition = new StringBuilder(64);
        StringBuilder sbCADBlock = new StringBuilder(64);
        StringBuilder sbULSBlock = new StringBuilder(64);
        StringBuilder sbDrawingBannerFCM = new StringBuilder(64);
        StringBuilder sbBasisDefHistory = new StringBuilder(64);
        StringBuilder sbRenaultNissan = new StringBuilder(16);

        try {
            StringBuilder sbFinal = new StringBuilder();
            sbFinal.append(XML_VERSION);
            sbFinal.append(TAG_TITLEBLOCK_START);
            sbFinal.append(TAG_PATTERN_START);
            sbFinal.append(strPattern);
            sbFinal.append(TAG_PATTERN_END);

            strPattern = strPattern.replaceAll(" ", DomainConstants.EMPTY_STRING).toUpperCase();

            Map mapHistoryBlock = TigerConstants.STRING_YES.equalsIgnoreCase(strDrawinghistory) ? generateHistoryBlock(context, domObject, strNoRevision, strPattern) : new HashMap();
            if (!mapHistoryBlock.isEmpty() && mapHistoryBlock.containsKey(XML) && mapHistoryBlock.containsKey(List)) {
                listOfHistory = (List<String[]>) mapHistoryBlock.get(List);
                sbHistory = (StringBuilder) mapHistoryBlock.get(XML);
            }

            Map mapOfLinkedPart = TigerConstants.STRING_YES.equalsIgnoreCase(strLinkedParts) ? generateLinkedPartBlock(context, domObject, strPattern) : new HashMap();
            if (!mapOfLinkedPart.isEmpty() && mapOfLinkedPart.containsKey(XML) && mapOfLinkedPart.containsKey(List)) {
                sbLinkedPart = (StringBuilder) mapOfLinkedPart.get(XML);
                listOfLinkedPart = (List<String[]>) mapOfLinkedPart.get(List);
            }

            Map mapOfBasisDefinition = generateBasisDefinitionBlock(context, domObject, strPattern);
            if (!mapOfBasisDefinition.isEmpty() && mapOfBasisDefinition.containsKey(XML) && mapOfBasisDefinition.containsKey(List)) {
                sbBasisDefinition = (StringBuilder) mapOfBasisDefinition.get(XML);
                listOfBasisDefinition = (List<String[]>) mapOfBasisDefinition.get(List);
            }

            Map mapOfCADBlock = generateCADBlock(context, domObject);
            if (!mapOfCADBlock.isEmpty() && mapOfCADBlock.containsKey(XML) && mapOfCADBlock.containsKey(List)) {
                sbCADBlock = (StringBuilder) mapOfCADBlock.get(XML);
                listOfCADBlock = (List<String[]>) mapOfCADBlock.get(List);
            }

            Map mapOfULSBlock = generateULSBlock(context, domObject, strUpperLevelStructure);
            if (!mapOfULSBlock.isEmpty() && mapOfULSBlock.containsKey(XML) && mapOfULSBlock.containsKey(List)) {
                sbULSBlock = (StringBuilder) mapOfULSBlock.get(XML);
                listOfULSBlock = (List<String[]>) mapOfULSBlock.get(List);
            }

            switch (strPattern) {
            case "FASFAE":
                mapOfTitleBlockData.put(HISTORYBLOCK, listOfHistory);
                mapOfTitleBlockData.put(LINKEDPARTBLOCK, listOfLinkedPart);
                mapOfTitleBlockData.put(BASICDEFINITION, listOfBasisDefinition);
                mapOfTitleBlockData.put(CADBLOCK, listOfCADBlock);
                mapOfTitleBlockData.put(ULSBLOCK, listOfULSBlock);

                if (TigerConstants.STRING_YES.equalsIgnoreCase(strDrawinghistory)) {
                    sbFinal.append(sbHistory);
                }
                if (TigerConstants.STRING_YES.equalsIgnoreCase(strLinkedParts)) {
                    sbFinal.append(sbLinkedPart);
                }
                if (!DomainConstants.EMPTY_STRING.equalsIgnoreCase(sbBasisDefinition.toString())) {
                    sbFinal.append(sbBasisDefinition);
                }
                sbFinal.append(sbCADBlock);
                sbFinal.append(sbULSBlock);

                break;
            case "FCM":
                Map mapOfDrawingBannerFCM = generateDrawingBannnerFCM(context, domObject, strLinkedParts);
                if (!mapOfDrawingBannerFCM.isEmpty() && mapOfDrawingBannerFCM.containsKey(XML) && mapOfDrawingBannerFCM.containsKey(List)) {
                    sbDrawingBannerFCM = (StringBuilder) mapOfDrawingBannerFCM.get(XML);
                    listOfDrawingBannerFCM = (List<String[]>) mapOfDrawingBannerFCM.get(List);
                }
                mapOfTitleBlockData.put(DRAWINGBANNER, listOfDrawingBannerFCM);
                sbFinal.append(sbDrawingBannerFCM);
                break;
            case "BASISDEFINITION":
                Map mapOfBasisDefHistory = TigerConstants.STRING_YES.equalsIgnoreCase(strDrawinghistory) ? generateBasisDefHistoryBlock(context, domObject, strNoRevision, strPattern) : new HashMap();
                if (!mapOfBasisDefHistory.isEmpty() && mapOfBasisDefHistory.containsKey(XML) && mapOfBasisDefHistory.containsKey(List)) {
                    sbBasisDefHistory = (StringBuilder) mapOfBasisDefHistory.get(XML);
                    listOfBasisDefHistory = (List<String[]>) mapOfBasisDefHistory.get(List);
                }
                mapOfTitleBlockData.put(BHISTORYBLOCK, listOfBasisDefHistory);
                mapOfTitleBlockData.put(BCADBLOCK, listOfCADBlock);
                mapOfTitleBlockData.put(BULSBLOCK, listOfULSBlock);

                sbFinal.append(sbBasisDefHistory);
                sbFinal.append(sbCADBlock);
                sbFinal.append(sbULSBlock);
                break;
            case "RENAULTNISSAN":
                mapOfTitleBlockData.put(RHISTORYBLOCK, listOfHistory);
                mapOfTitleBlockData.put(RLINKEDPARTBLOCK, listOfLinkedPart);

                Map mapOfRenaultNissan = generateRenaultNissanBlock(context);
                if (!mapOfRenaultNissan.isEmpty() && mapOfRenaultNissan.containsKey(XML) && mapOfRenaultNissan.containsKey(List)) {
                    sbRenaultNissan = (StringBuilder) mapOfRenaultNissan.get(XML);
                    listOfRenaultNissan = (List<String[]>) mapOfRenaultNissan.get(List);
                }
                mapOfTitleBlockData.put(RENOTB, listOfRenaultNissan);
                mapOfTitleBlockData.put(RCADBLOCK, listOfCADBlock);
                mapOfTitleBlockData.put(RULSBLOCK, listOfULSBlock);

                if (TigerConstants.STRING_YES.equalsIgnoreCase(strDrawinghistory)) {
                    sbFinal.append(sbHistory);
                }
                if (TigerConstants.STRING_YES.equalsIgnoreCase(strLinkedParts)) {
                    sbFinal.append(sbLinkedPart);
                }
                sbFinal.append(sbRenaultNissan);
                sbFinal.append(sbCADBlock);
                sbFinal.append(sbULSBlock);
                break;
            default:
                logger.info(STRING_INVALID_CHOICE);

            }
            sbFinal.append(TAG_TITLEBLOCK_END);

            File file = new File(new StringBuilder(strCheckinDir).append(File.separator).append(strXMLFileName).toString());

            // creates the file
            boolean isCreated = file.createNewFile();
            if (isCreated) {
                Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
                try {
                    writer.write(sbFinal.toString());
                    writer.flush();
                } finally {
                    writer.close();
                }
            } else {
                logger.error("Error in generateTitleBlockData: ", file);
            }
        } catch (Exception e) {
            logger.error("Error in generateTitleBlockData: ", e.getMessage());
            e.printStackTrace();
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return mapOfTitleBlockData;
    }

    /**
     * This method will generate data for Linked Part Block for Title Block.
     * @param context
     * @param currentDrawingObject
     * @param strPattern
     * @throws Exception
     * @author PSI
     */
    private Map generateLinkedPartBlock(Context context, DomainObject currentDrawingObject, String strPattern) throws Exception {

        StringBuilder strXMLFINAL = new StringBuilder(256);
        List<String[]> listOfLinkedPart = new ArrayList<String[]>();
        Map<String, String> mapOfLinkedPart = new HashMap<String, String>();
        Map mapLinkedPartBlock = new HashMap();
        String strATTRIBUTE_PSS_MASS = DomainConstants.EMPTY_STRING;
        StringBuilder strXML = new StringBuilder(256);
        try {

            String strSequence = TigerAppConfigProperties.getPropertyValue(context, new StringBuilder(35).append(strPattern).append(LINKEDPARTBLOCKCELLSEQUENCE).toString());
            StringList slSequence = FrameworkUtil.split(strSequence, TigerConstants.SEPERATOR_COMMA);
            int seqSize = slSequence.size();

            strXML.append("\n<LINKEDPARTBLOCK>\t");
            strXML.append("\n\t<PART>PART</PART>\n<NAME>Name</NAME>\n<REVISION>Revision</REVISION>\n\t<MASS> MASS (g) </MASS>\n\t<MIRRORED> MIRRORED </MIRRORED>\n\t<X2> X2 </X2>");
            strXML.append("\n\t<STATUS>STATUS </STATUS>\n\t<PARTSAFETYCLASS>PART SAFETY CLASS</PARTSAFETYCLASS>");
            strXML.append("\n\t<MATERIALSTANDARD> MATERIAL + STANDARD </MATERIALSTANDARD>\n\t<MATERIALSAFETYCLASS>PARTSAFETYCLASS</MATERIALSAFETYCLASS>");
            strXML.append(
                    "\n\t<SEMIMANUFACTURESTANDARD> SEMI MANUFACTUR. STANDARD</SEMIMANUFACTURESTANDARD>\n\t<HEATTREATMENT>TREATMENT</HEATTREATMENT>\n\t<TREATMENTSAFETYCLASS>Part Safety Class</TREATMENTSAFETYCLASS>");
            strXML.append("\n</LINKEDPARTBLOCK>\t");

            String strSelectSC = new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_SAFETY_CLASSIFICATION).append("].value").toString();
            String strSelectMSC = new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_MATERIAL_SAFETY_CLASSIFICATION).append("].value").toString();
            String strSelectHT = new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_HEATTREATMENT).append("].value").toString();
            String strSelectTSC = new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_TREATMENT_SAFETY_CLASS).append("].value").toString();

            StringList slObjSel = new StringList(32);
            slObjSel.add(DomainConstants.SELECT_ID);
            slObjSel.add(DomainConstants.SELECT_NAME);
            slObjSel.add(DomainConstants.SELECT_REVISION);
            slObjSel.add(DomainConstants.SELECT_CURRENT);
            slObjSel.add(DomainConstants.SELECT_POLICY);
            slObjSel.add(strSelectSC);
            slObjSel.add(strSelectMSC);
            slObjSel.add(strSelectHT);
            slObjSel.add(strSelectTSC);
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass1).append("].value").toString());

            slObjSel.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSEOFRELEASE_DEVPART + "]");
            slObjSel.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.attribute["
                    + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "].value");
            slObjSel.add("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.attribute["
                    + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "].value");

            String strSelectSymmID = new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.").append(DomainConstants.SELECT_ID).toString();
            String strSelectSymmName = new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.").append(DomainConstants.SELECT_NAME).toString();
            String strSelectSymmRev = new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.").append(DomainConstants.SELECT_REVISION)
                    .toString();

            String strSelectHasSymm = new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("]").toString();

            // selectable for Symmetrical Parts With Same Mass
            slObjSel.add(strSelectSymmID);
            slObjSel.add(strSelectSymmName);
            slObjSel.add(strSelectSymmRev);
            slObjSel.add(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.attribute[").append(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2)
                    .append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.attribute[").append(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3)
                    .append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.attribute[").append(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass1)
                    .append("].value").toString());

            slObjSel.add(strSelectHasSymm);

            // selectable for Mirrored Part
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].to.").append(DomainConstants.SELECT_ID).toString());
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].to.").append(DomainConstants.SELECT_NAME).toString());
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].to.").append(DomainConstants.SELECT_REVISION).toString());
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].attribute[")
                    .append(TigerConstants.ATTRIBUTE_PSS_SYMMETRICAL_PARTS_MANAGE_IN_PAIRS).append("]").toString());
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("]").toString());
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].to.from[").append(DomainConstants.RELATIONSHIP_PART_SPECIFICATION)
                    .append("].to.name").toString());
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].to.from[").append(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING)
                    .append("].to.name").toString());
            // selectable for Material
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_MATERIAL).append("].to.").append(DomainConstants.SELECT_NAME).toString());
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_MATERIAL).append("].to.").append(DomainConstants.SELECT_DESCRIPTION).toString());

            // slObjSel.add("from[" + TigerConstants.RELATIONSHIP_PSS_MATERIAL + "].to." + "attribute[" + TigerConstants.ATTRIBUTE_SEMI_MANUFACTURED_PRODUCT_STANDARD + "].value");
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_MATERIAL).append("].to.")
                    .append("attribute[" + TigerConstants.ATTRIBUTE_PSS_SEMIMANUFACTUREDPRODUCTSTANDARD + "].value").toString());

            MapList mpPartList = getConnectedPart(context, currentDrawingObject, slObjSel, DomainConstants.EMPTY_STRINGLIST, 0);
            int iSize = mpPartList.size();
            Iterator<HashMap> itr = mpPartList.iterator();

            if (iSize > 0) {

                MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-us"), new IEFGlobalCache());
                Map mpPart = new HashMap();
                while (itr.hasNext()) {
                    mapOfLinkedPart = new HashMap();

                    mpPart = itr.next();
                    String strPartId = (String) ((StringList) mpPart.get(DomainConstants.SELECT_ID)).get(0);
                    String strPartName = (String) ((StringList) mpPart.get(DomainConstants.SELECT_NAME)).get(0);
                    String strPartRev = (String) ((StringList) mpPart.get(DomainConstants.SELECT_REVISION)).get(0);
                    StringList slAttrSafetyClass = (StringList) mpPart.get(strSelectSC);
                    StringList slMaterialSafetyClass = (StringList) mpPart.get(strSelectMSC);
                    StringList slHeatTreatment = (StringList) mpPart.get(strSelectHT);
                    String strHeatTreatmentValue = DomainConstants.EMPTY_STRING;

                    if (mxUtil.hasAttributeForBO(context, strPartId, TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3)) {
                        strATTRIBUTE_PSS_MASS = TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3;
                    } else if (mxUtil.hasAttributeForBO(context, strPartId, TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2)) {
                        strATTRIBUTE_PSS_MASS = TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2;
                    } else if (mxUtil.hasAttributeForBO(context, strPartId, TigerConstants.ATTRIBUTE_PSS_EBOM_Mass1)) {
                        strATTRIBUTE_PSS_MASS = TigerConstants.ATTRIBUTE_PSS_EBOM_Mass1;
                    }

                    String strPartMass = (UIUtil.isNotNullAndNotEmpty(strATTRIBUTE_PSS_MASS))
                            ? (String) ((StringList) mpPart.get(new StringBuilder(64).append("attribute[").append(strATTRIBUTE_PSS_MASS).append("].value").toString())).get(0)
                            : "0.0";
                    strPartMass = UIUtil.isNullOrEmpty(strPartMass) ? "0.0" : strPartMass;
                    double dPartMass = Double.parseDouble(strPartMass);

                    // =====Has Sym Part
                    String strHasSymmetric = (String) ((StringList) mpPart.get(strSelectHasSymm)).get(0);
                    if ("True".equalsIgnoreCase(strHasSymmetric)) {
                        StringList slSymPartIdList = (StringList) mpPart.get(strSelectSymmID);
                        String strSymPartId = (String) slSymPartIdList.get(0);
                        if (mxUtil.hasAttributeForBO(context, strSymPartId, TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3)) {
                            strATTRIBUTE_PSS_MASS = TigerConstants.ATTRIBUTE_PSS_EBOM_Mass3;
                        } else if (mxUtil.hasAttributeForBO(context, strSymPartId, TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2)) {
                            strATTRIBUTE_PSS_MASS = TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2;
                        } else if (mxUtil.hasAttributeForBO(context, strSymPartId, TigerConstants.ATTRIBUTE_PSS_EBOM_Mass1)) {
                            strATTRIBUTE_PSS_MASS = TigerConstants.ATTRIBUTE_PSS_EBOM_Mass1;
                        } else {
                            strATTRIBUTE_PSS_MASS = DomainConstants.EMPTY_STRING;
                        }
                        String strSymPartName = (String) ((StringList) mpPart.get(strSelectSymmName)).get(0);
                        String strSymPartRev = (String) ((StringList) mpPart.get(strSelectSymmRev)).get(0);
                        String strSymPartMass = (UIUtil.isNotNullAndNotEmpty(strATTRIBUTE_PSS_MASS))
                                ? (String) ((StringList) mpPart.get(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.")
                                        .append("attribute[").append(strATTRIBUTE_PSS_MASS).append("].value").toString())).get(0)
                                : "0.0";
                        strSymPartMass = UIUtil.isNullOrEmpty(strSymPartMass) ? "0.0" : strSymPartMass;
                        double dSymPartMass = Double.parseDouble(strSymPartMass);

                        // If the Part is Symmetric Part to any other Part connected the
                        // same Drawing and Mass/Weight are same for those Parts then
                        // remove from the list - no need to display Symmetrical Part
                        // with same Mass/Weight on Title Block.
                        if (!(strSymPartName.equals(strPartName) && strSymPartRev.equals(strPartRev)) && (Double.compare(dSymPartMass, dPartMass) == 0)) {
                            itr.remove();
                            continue;
                        }
                    }

                    if (slHeatTreatment == null) {
                        strHeatTreatmentValue = DomainConstants.EMPTY_STRING;
                    } else {
                        strHeatTreatmentValue = (String) slHeatTreatment.get(0);
                    }
                    strHeatTreatmentValue = TigerUtils.replaceWordCharsForXML(context, strHeatTreatmentValue);

                    if (strHeatTreatmentValue.length() > 30) {
                        strHeatTreatmentValue = TigerUtils.replaceAndWordWrap(context, strHeatTreatmentValue, 30);
                    }
                    if (strHeatTreatmentValue.length() > 60) {
                        int loc = StringUtils.ordinalIndexOf(strHeatTreatmentValue.toString(), "&lt;br/&gt;", 2);
                        if (loc > 0) {
                            strHeatTreatmentValue = strHeatTreatmentValue.substring(0, loc);
                        }
                    }
                    StringList slTreatmentSafetyClass = (StringList) mpPart.get(strSelectTSC);

                    StringBuilder strTreatmentSafetyClass = new StringBuilder();
                    StringBuilder strSafetyClass = new StringBuilder();
                    StringBuilder strMaterialSafetyClass = new StringBuilder();

                    if (slTreatmentSafetyClass == null) {
                        strTreatmentSafetyClass.append(DomainConstants.EMPTY_STRING);
                    } else {
                        strTreatmentSafetyClass.append((String) slTreatmentSafetyClass.get(0));
                    }
                    if (slAttrSafetyClass == null) {
                        strSafetyClass.append(DomainConstants.EMPTY_STRING);
                    } else {
                        strSafetyClass.append((String) slAttrSafetyClass.get(0));
                    }
                    if (slMaterialSafetyClass == null) {
                        strMaterialSafetyClass.append(DomainConstants.EMPTY_STRING);
                    } else {
                        strMaterialSafetyClass.append((String) slMaterialSafetyClass.get(0));
                    }

                    String strMASS = DomainConstants.EMPTY_STRING;
                    if ((BigDecimal.valueOf(dPartMass).scale() > 2)) {
                        DecimalFormat df = new DecimalFormat("0.00");
                        df.setRoundingMode(java.math.RoundingMode.DOWN);
                        strMASS = df.format(dPartMass);
                    } else
                        strMASS = String.valueOf(dPartMass);

                    String PART = strPartName + "-" + strPartRev;
                    String strPolicy = (String) ((StringList) mpPart.get(DomainConstants.SELECT_POLICY)).get(0);
                    String strSTATUS = (String) ((StringList) mpPart.get(DomainConstants.SELECT_CURRENT)).get(0);

                    String strPurposeOfRelease = DomainConstants.EMPTY_STRING;

                    strPurposeOfRelease = purposeOfRelease(context, mpPart, strPolicy, strPattern);

                    strSTATUS = MCADMxUtil.getNLSName(context, "State", strSTATUS, "Policy", strPolicy, context.getLocale().getLanguage());

                    strXML.append("\n<LINKEDPARTBLOCK>\t");
                    strXML.append("\n<PART>");
                    strXML.append(PART);
                    strXML.append("</PART>");
                    strXML.append("\n<NAME>");
                    strXML.append(strPartName);
                    strXML.append("</NAME>");
                    strXML.append("\n<REVISION>");
                    strXML.append(strPartRev);
                    strXML.append("</REVISION>");
                    strXML.append("\n\t<MASS>");
                    strXML.append(strMASS);
                    strXML.append("</MASS>");

                    mapOfLinkedPart.put(STRING_PART, PART);
                    mapOfLinkedPart.put(STRING_MASS, strMASS);

                    BusinessObject domPart = new BusinessObject(strPartId);

                    String strX2 = DomainConstants.EMPTY_STRING;
                    StringBuilder sbMIRRORED = new StringBuilder();
                    String strSymName = DomainConstants.EMPTY_STRING;
                    String strSymRev = DomainConstants.EMPTY_STRING;

                    String strHasSymm = (String) ((StringList) mpPart.get(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("]").toString()))
                            .get(0);

                    if (TigerConstants.STRING_TRUE.equalsIgnoreCase(strHasSymm)) {
                        StringList strName = (StringList) mpPart
                                .get(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].to.").append(DomainConstants.SELECT_NAME).toString());
                        strSymName = (String) strName.get(0);
                        StringList strrRev = (StringList) mpPart
                                .get(new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].to.").append(DomainConstants.SELECT_REVISION).toString());
                        strSymRev = (String) strrRev.get(0);
                        StringList slPartSpec = (StringList) mpPart.get(new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].to.from[")
                                .append(DomainConstants.RELATIONSHIP_PART_SPECIFICATION).append("].to.name").toString());
                        StringList slChartedDrawing = (StringList) mpPart.get(new StringBuilder("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].to.from[")
                                .append(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING).append("].to.name").toString());
                        if (slPartSpec == null && slChartedDrawing == null) {
                            sbMIRRORED.append(DomainConstants.EMPTY_STRING);
                        } else {
                            sbMIRRORED.append(new StringBuilder(64).append("\n\t").append(strSymName).append("-").append(strSymRev).toString());
                        }
                        StringList strPairs = (StringList) mpPart.get(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].attribute[")
                                .append(TigerConstants.ATTRIBUTE_PSS_SYMMETRICAL_PARTS_MANAGE_IN_PAIRS).append("]").toString());
                        if (strPairs != null) {
                            strX2 = strPairs.get(0).toString();
                        }
                    }
                    strXML.append("\n\t<SYMETRIC>");
                    strXML.append(strSymName);
                    strXML.append("</SYMETRIC>");
                    strXML.append("\n\t<SYMREVISION>");
                    strXML.append(strSymRev);
                    strXML.append("</SYMREVISION>");
                    strXML.append("\n\t<MIRRORED>");
                    strXML.append(sbMIRRORED.toString());
                    strXML.append("</MIRRORED>\n\t<X2>");
                    strXML.append(strX2);
                    strXML.append("</X2>");
                    strXML.append("\n\t<STATUS>" + strSTATUS + "&lt;br/&gt;" + strPurposeOfRelease + "</STATUS>");

                    strXML.append("\n\t<PARTSAFETYCLASS>" + strSafetyClass + " </PARTSAFETYCLASS>");
                    sbMIRRORED = new StringBuilder(strSymName).append("-").append(strSymRev);
                    mapOfLinkedPart.put(STRING_MIRRORED, sbMIRRORED.toString());
                    mapOfLinkedPart.put(STRING_X2, "Yes".equalsIgnoreCase(strX2) ? "<CROSS>" : DomainConstants.EMPTY_STRING);
                    mapOfLinkedPart.put(STATUS, strSTATUS + "<br>" + strPurposeOfRelease);

                    mapOfLinkedPart.put(STRING_SAFETY1, "1".equalsIgnoreCase(strSafetyClass.toString()) ? "<CROSS>" : DomainConstants.EMPTY_STRING);
                    mapOfLinkedPart.put(STRING_SAFETY2, "2".equalsIgnoreCase(strSafetyClass.toString()) ? "<CROSS>" : DomainConstants.EMPTY_STRING);

                    StringBuilder sbManufactureSTD = new StringBuilder();
                    StringBuilder sbMaterialDesc = new StringBuilder();

                    Object objMaterialName = (Object) mpPart
                            .get(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_MATERIAL).append("].to.").append(DomainConstants.SELECT_NAME).toString());
                    Object ObjDescMaterial = (Object) mpPart
                            .get(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_MATERIAL).append("].to.").append(DomainConstants.SELECT_DESCRIPTION).toString());
                    Object objManufactureSTD = (Object) mpPart.get(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_PSS_MATERIAL).append("].to.")
                            .append("attribute[" + TigerConstants.ATTRIBUTE_PSS_SEMIMANUFACTUREDPRODUCTSTANDARD + "].value").toString());

                    StringList slMaterialName = new StringList();
                    StringList slDescMaterial = new StringList();
                    StringList slManufactureSTD = new StringList();

                    if (objMaterialName != null && objMaterialName instanceof StringList && ObjDescMaterial != null && objManufactureSTD != null) {
                        slMaterialName = (StringList) objMaterialName;
                        slDescMaterial = (StringList) ObjDescMaterial;
                        slManufactureSTD = (StringList) objManufactureSTD;
                    } else if (objMaterialName != null && objMaterialName instanceof String && ObjDescMaterial != null && objManufactureSTD != null) {
                        slMaterialName.add(objMaterialName);
                        slDescMaterial.add(ObjDescMaterial);
                        slManufactureSTD.add(objManufactureSTD);
                    }

                    int iMaterialSize = slMaterialName.size();
                    if (iMaterialSize > 0) {
                        for (int j = 0; j < iMaterialSize; j++) {
                            String strDescMaterial = StringEscapeUtils.escapeXml10((String) slDescMaterial.get(j));
                            strDescMaterial = TigerUtils.replaceWordCharsForXML(context, strDescMaterial);
                            String strManufactureSTD = StringEscapeUtils.escapeXml10((String) slManufactureSTD.get(j));

                            strManufactureSTD = TigerUtils.replaceWordCharsForXML(context, strManufactureSTD);
                            int iManufactureSTDCount = 0;
                            int iDescMaterialCount = 0;
                            if (UIUtil.isNullOrEmpty(strManufactureSTD)) {
                                sbManufactureSTD.append(DomainConstants.EMPTY_STRING);
                            } else {
                                iManufactureSTDCount = 1;
                                if (strManufactureSTD.length() > 27) {
                                    strManufactureSTD = TigerUtils.replaceAndWordWrap(context, strManufactureSTD, 27);
                                    iManufactureSTDCount = 2;
                                }
                                if (strManufactureSTD.length() > 54) {
                                    int loc = StringUtils.ordinalIndexOf(strManufactureSTD.toString(), "&lt;br/&gt;", 2);
                                    if (loc > 0) {
                                        strManufactureSTD = strManufactureSTD.substring(0, loc);
                                    }
                                }
                                if (j == 0) {
                                    sbManufactureSTD.append(strManufactureSTD);
                                } else {
                                    if (!sbManufactureSTD.toString().endsWith(String_LTBRGT))
                                        sbManufactureSTD.append(String_LTBRGT);
                                    sbManufactureSTD.append(strManufactureSTD);
                                }
                            }
                            if (UIUtil.isNullOrEmpty(strDescMaterial)) {
                                sbMaterialDesc.append(DomainConstants.EMPTY_STRING);
                            } else {
                                iDescMaterialCount = 1;
                                if (strDescMaterial.length() > 40) {
                                    strDescMaterial = TigerUtils.replaceAndWordWrap(context, strDescMaterial, 40);
                                    iDescMaterialCount = 2;
                                }
                                if (strDescMaterial.length() > 80) {
                                    int loc = StringUtils.ordinalIndexOf(strDescMaterial.toString(), String_LTBRGT, 2);
                                    if (loc > 0) {
                                        strDescMaterial = strDescMaterial.substring(0, loc);
                                    }
                                }
                                if (j == 0) {
                                    sbMaterialDesc.append(strDescMaterial);
                                } else {
                                    if (!sbMaterialDesc.toString().endsWith(String_LTBRGT))
                                        sbMaterialDesc.append(String_LTBRGT);
                                    sbMaterialDesc.append(strDescMaterial);
                                }

                            }
                            int idiff = 0;
                            if (iDescMaterialCount != iManufactureSTDCount) {
                                if (iDescMaterialCount > iManufactureSTDCount) {
                                    idiff = iDescMaterialCount - iManufactureSTDCount;
                                    if (idiff == 1)
                                        sbManufactureSTD.append(STRING_LTBRGT_SPACE);
                                    else if (idiff == 2) {
                                        sbManufactureSTD.append(STRING_LTBRGT_SPACE);
                                        sbManufactureSTD.append(STRING_LTBRGT_SPACE);
                                    }

                                } else if (iManufactureSTDCount > iDescMaterialCount) {
                                    idiff = iManufactureSTDCount - iDescMaterialCount;
                                    if (idiff == 1)
                                        sbMaterialDesc.append(STRING_LTBRGT_SPACE);
                                    else if (idiff == 2) {
                                        sbMaterialDesc.append(STRING_LTBRGT_SPACE);
                                        sbMaterialDesc.append(STRING_LTBRGT_SPACE);
                                    }
                                }
                            }

                        }
                    }

                    strXML.append("\n\t<MATERIALSTANDARD>");
                    strXML.append(XSSUtil.encodeForXML(context, sbMaterialDesc.toString()));
                    strXML.append("</MATERIALSTANDARD>");
                    strXML.append("\n\t<MATERIALSAFETYCLASS>");
                    strXML.append(strMaterialSafetyClass);
                    strXML.append(" </MATERIALSAFETYCLASS>");
                    strXML.append("\n\t<SEMIMANUFACTURESTANDARD>");
                    strXML.append(XSSUtil.encodeForXML(context, sbManufactureSTD.toString()));
                    strXML.append("</SEMIMANUFACTURESTANDARD>");
                    strXML.append("\n\t<HEATTREATMENT>");
                    strXML.append(XSSUtil.encodeForXML(context, strHeatTreatmentValue));
                    strXML.append(" </HEATTREATMENT>");
                    strXML.append("\n\t<TREATMENTSAFETYCLASS>");
                    strXML.append(strTreatmentSafetyClass);
                    strXML.append(" </TREATMENTSAFETYCLASS>");
                    strXML.append("\n</LINKEDPARTBLOCK>\t");

                    // mapOfLinkedPart.put("MATERIALSTANDARD", sbMaterialDesc.toString().replace("&lt;br/&gt;", "<br>").replaceAll("&#xa;", ". "));
                    mapOfLinkedPart.put(STRING_MATERIALSTANDARD, TigerUtils.replaceWordCharsForPDF(context, sbMaterialDesc.toString()));
                    mapOfLinkedPart.put(STRING_MATERIALSAFETY1, "1".equalsIgnoreCase(strMaterialSafetyClass.toString()) ? TigerConstants.TITLE_BLOCK_CELL_CROSS_TAG : DomainConstants.EMPTY_STRING);
                    mapOfLinkedPart.put(STRING_MATERIALSAFETY2, "2".equalsIgnoreCase(strMaterialSafetyClass.toString()) ? TigerConstants.TITLE_BLOCK_CELL_CROSS_TAG : DomainConstants.EMPTY_STRING);

                    // mapOfLinkedPart.put("SEMIMANUFACTURSTANDARD", sbManufactureSTD.toString().replace("&lt;br/&gt;", "<br>"));
                    mapOfLinkedPart.put(STRING_SEMIMANUFACTURSTANDARD, TigerUtils.replaceWordCharsForPDF(context, sbManufactureSTD.toString()));
                    mapOfLinkedPart.put(STRING_TREATMENT, TigerUtils.replaceWordCharsForPDF(context, strHeatTreatmentValue));
                    mapOfLinkedPart.put(STRING_TREATMENTSAFETY1, "1".equalsIgnoreCase(strTreatmentSafetyClass.toString()) ? TigerConstants.TITLE_BLOCK_CELL_CROSS_TAG : DomainConstants.EMPTY_STRING);
                    mapOfLinkedPart.put(STRING_TREATMENTSAFETY2, "2".equalsIgnoreCase(strTreatmentSafetyClass.toString()) ? TigerConstants.TITLE_BLOCK_CELL_CROSS_TAG : DomainConstants.EMPTY_STRING);
                    String[] strArrayOfSrq = new String[seqSize];
                    int j = 0;
                    for (j = 0; j < seqSize; j++) {
                        strArrayOfSrq[j] = (String) mapOfLinkedPart.get(slSequence.get(j));
                    }
                    listOfLinkedPart.add(strArrayOfSrq);
                }
            } /*
               * else { strXML.append("\n<LINKEDPARTBLOCK>\t"); strXML.append("\n\t<PART></PART>\n\t<MASS></MASS>\n\t<MIRRORED></MIRRORED>\n\t<X2></X2>");
               * strXML.append("\n\t<STATUS></STATUS>\n\t<PARTSAFETYCLASS></PARTSAFETYCLASS>");
               * strXML.append("\n\t<MATERIALSTANDARD></MATERIALSTANDARD>\n\t<MATERIALSAFETYCLASS></MATERIALSAFETYCLASS>");
               * strXML.append("\n\t<SEMIMANUFACTURESTANDARD></SEMIMANUFACTURESTANDARD>\n\t<HEATTREATMENT></HEATTREATMENT>\n\t<TREATMENTSAFETYCLASS></TREATMENTSAFETYCLASS>");
               * strXML.append("\n</LINKEDPARTBLOCK>\t"); }
               */

            strXMLFINAL.append(strXML);
            mapLinkedPartBlock.put(XML, strXMLFINAL);
            mapLinkedPartBlock.put(List, listOfLinkedPart);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in generateLinkedPartBlock: ", e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return mapLinkedPartBlock;
    }

    /**
     * This method will generate data for Basis Definition Block for Title Block.
     * @param context
     * @param dObj
     * @param strPattern
     * @throws Exception
     * @author PSI
     */
    private Map generateBasisDefinitionBlock(Context context, DomainObject dObj, String strPattern) throws Exception {
        StringBuilder strXMLFINAL = new StringBuilder();
        List<String[]> listOfBasisDefinition = new ArrayList<String[]>();
        Map mapBasisDefinitionBlock = new HashMap();
        StringBuilder strXML = new StringBuilder();
        try {
            String strSequence = TigerAppConfigProperties.getPropertyValue(context, new StringBuilder(strPattern).append(STRING_BASICDEFINITIONCELLSEQUENCE).toString());
            StringList slSequence = FrameworkUtil.split(strSequence, TigerConstants.SEPERATOR_COMMA);
            int iSeqSize = slSequence.size();

            StringList slObjSel = new StringList(4);
            slObjSel.add(DomainConstants.SELECT_NAME);
            slObjSel.add(DomainConstants.SELECT_REVISION);
            slObjSel.add(DomainConstants.SELECT_CURRENT);
            slObjSel.add(DomainConstants.SELECT_DESCRIPTION);

            Pattern pTypePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
            pTypePattern.addPattern(DomainConstants.TYPE_CAD_MODEL);
            ExpansionIterator expCadObj = dObj.getExpansionIterator(context, TigerConstants.RELATIONSHIP_PSS_BASIS_DEFINITION, pTypePattern.getPattern(), slObjSel, new StringList(), false, true,
                    (short) 0, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 1000, false, true, (short) 1000);
            MapList mlCadObj = FrameworkUtil.toMapList(expCadObj, (short) 0, null, null, null, null);
            expCadObj.close();
            if (mlCadObj.size() > 0) {

                strXML.append(TAG_BASISDEFINITIONDESCRIPTION_START);
                strXML.append(new StringBuilder(TAG_BASISDEFNAME_START).append(BASISDEFNAME).append(TAG_BASISDEFNAME_END).append(TAG_BASISDEFREVISION_START).append(BASISDEFREVISION)
                        .append(TAG_BASISDEFREVISION_END).append(TAG_BASISDEFINITION_START).append(BASISDEFINITION).append(TAG_BASISDEFINITION_END).append(TAG_STATUS_START).append(STATUS)
                        .append(TAG_STATUS_END).append(TAG_DESCRIPTION_START).append(DESCRIPTION).append(TAG_DESCRIPTION_END).toString());
                strXML.append(TAG_BASISDEFINITIONDESCRIPTION_END);

                int iSize = mlCadObj.size();
                for (int i = 0; i < iSize; i++) {
                    Map mpCadObj = (Map) mlCadObj.get(i);
                    String strSymName = (String) ((StringList) mpCadObj.get(DomainConstants.SELECT_NAME)).get(0);
                    String strSymRev = (String) ((StringList) mpCadObj.get(DomainConstants.SELECT_REVISION)).get(0);
                    int iLengthCad = strSymName.length();
                    // if name greater than 35 chars then display 32 chars of name+...
                    if (iLengthCad > 35) {
                        strSymName = strSymName.substring(0, 32);
                        strSymName = new StringBuilder(strSymName).append(STRING_SEPERATOR_DOT).toString();
                    }
                    String strBASIS_DEF = new StringBuilder(strSymName).append(" ").append(strSymRev).toString();

                    String strSTATUS = (String) ((StringList) mpCadObj.get(DomainConstants.SELECT_CURRENT)).get(0);
                    String strBasisDefinitionDesc = StringEscapeUtils.escapeXml10((String) ((StringList) mpCadObj.get(DomainConstants.SELECT_DESCRIPTION)).get(0));
                    if (strBasisDefinitionDesc.length() > 82) {
                        strBasisDefinitionDesc = TigerUtils.replaceAndWordWrap(context, strBasisDefinitionDesc, 82);
                    }

                    strXML.append(TAG_BASISDEFINITIONDESCRIPTION_START);
                    strXML.append(TAG_BASISDEFNAME_START);
                    strXML.append(strSymName);
                    strXML.append(TAG_BASISDEFNAME_END);
                    strXML.append(TAG_BASISDEFREVISION_START);
                    strXML.append(strSymRev);
                    strXML.append(TAG_BASISDEFREVISION_END);
                    strXML.append(TAG_BASISDEFINITION_START);
                    strXML.append(strBASIS_DEF);
                    strXML.append(TAG_BASISDEFINITION_END);
                    strXML.append(TAG_STATUS_START);
                    strXML.append(strSTATUS);
                    strXML.append(TAG_STATUS_END);
                    strXML.append(TAG_DESCRIPTION_START);
                    strXML.append(XSSUtil.encodeForXML(context, strBasisDefinitionDesc));
                    strXML.append(TAG_DESCRIPTION_END);
                    strXML.append(TAG_BASISDEFINITIONDESCRIPTION_END);

                    Map mapOfBasisDefinition = new HashMap();
                    mapOfBasisDefinition.put(STRING_BASISDEF, strBASIS_DEF);
                    mapOfBasisDefinition.put(STATUS, strSTATUS);
                    mapOfBasisDefinition.put(STRING_CHANGEDESCRIPTION, TigerUtils.replaceWordCharsForPDF(context, strBasisDefinitionDesc));
                    String[] strArrayOfSrq = new String[iSeqSize];
                    int j = 0;
                    for (j = 0; j < iSeqSize; j++) {
                        strArrayOfSrq[j] = (String) mapOfBasisDefinition.get(slSequence.get(j));
                    }
                    listOfBasisDefinition.add(strArrayOfSrq);
                }
            }
            strXMLFINAL.append(strXML);
            if (UIUtil.isNotNullAndNotEmpty(strXMLFINAL.toString()) && !listOfBasisDefinition.isEmpty()) {
                mapBasisDefinitionBlock.put(XML, strXMLFINAL);
                mapBasisDefinitionBlock.put(List, listOfBasisDefinition);
            }
        } catch (Exception e) {
            logger.error("Error in generateBasisDefinitionBlock: ", e.getMessage());
            throw new Exception(e);
        }
        return mapBasisDefinitionBlock;
    }

    /**
     * This method will generate Data for Renault Nissan Block for Title Block.
     * @param context
     * @throws Exception
     * @author PSI
     */
    private Map generateRenaultNissanBlock(Context context) throws Exception {
        StringBuilder strXMLFINAL = new StringBuilder();
        List<String[]> listOfRenaultNissan = new ArrayList<String[]>();
        Map mapRenaultNissanBlock = new HashMap();
        strXMLFINAL.append("<RENAULTNISSAN></RENAULTNISSAN>");
        // method use to pass empty data
        mapRenaultNissanBlock.put(XML, strXMLFINAL);
        mapRenaultNissanBlock.put(List, listOfRenaultNissan);
        return mapRenaultNissanBlock;
    }

    /**
     * This method will generate Data for Basis Definition History Block for Title Block.
     * @param context
     * @param dObj
     * @param strNoRevision
     * @param strPattern
     * @throws Exception
     * @author PSI
     */
    private Map generateBasisDefHistoryBlock(Context context, DomainObject dObj, String strNoRevision, String strPattern) throws Exception {

        StringBuilder strXMLFINAL = new StringBuilder(64);
        List<String[]> listOfBasisDefHistory = new ArrayList<String[]>();
        Map mapBasisDefHistoryBlock = new HashMap();
        StringBuilder strXML = new StringBuilder();
        try {
            String strSequence = TigerAppConfigProperties.getPropertyValue(context, new StringBuilder(35).append(strPattern).append(STRING_HISTORYBLOCKCELLSEQUENCE).toString());

            StringList slSequence = FrameworkUtil.split(strSequence, TigerConstants.SEPERATOR_COMMA);

            int iSeqSize = slSequence.size();

            StringList strSelectsSingle = new StringList(7);
            strSelectsSingle.add(DomainConstants.SELECT_ID);
            strSelectsSingle.add(DomainConstants.SELECT_REVISION);
            strSelectsSingle.add(DomainConstants.SELECT_CURRENT);
            strSelectsSingle.add(DomainConstants.SELECT_ORIGINATED);

            strSelectsSingle.add(STRING_CURRENTACTUAL);
            strSelectsSingle.add(DomainConstants.SELECT_OWNER);
            strSelectsSingle.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_ACTIVEVERSION).append("].to.").append(DomainConstants.SELECT_REVISION).toString());
            strSelectsSingle.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_MCADINTEG_COMMENT).append("].value").toString());

            MapList mpList = null;
            try {
                TigerUtils.pushContextToSuperUser(context);
                mpList = dObj.getRevisionsInfo(context, strSelectsSingle, new StringList());
            } finally {
                ContextUtil.popContext(context);
            }
            int iSize = mpList == null ? 0 : mpList.size();
            int iNoRevision = 3;
            if (iSize > 0) {
                if (TigerConstants.ALL.equals(strNoRevision)) {
                    iNoRevision = iSize;
                } else {
                    iNoRevision = iSize - Integer.parseInt(strNoRevision);
                }
                if (iNoRevision < 0) {
                    iNoRevision = 0;
                }

                for (int i = iNoRevision; i < iSize; i++) {
                    Map mapCADobj = (Map) mpList.get(i);
                    Map mapOfBasisDefHistory = new HashMap();
                    String strId = (String) mapCADobj.get(DomainConstants.SELECT_ID);
                    // BusinessObject bo = new BusinessObject(strId);
                    // MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-us"), new IEFGlobalCache());
                    // BusinessObject activeMinorBus = mxUtil.getActiveMinor(context, bo);
                    // String strRev = activeMinorBus.getRevision();
                    String strRev = (String) mapCADobj
                            .get(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_ACTIVEVERSION).append("].to.").append(DomainConstants.SELECT_REVISION).toString());
                    String strState = (String) mapCADobj.get(DomainConstants.SELECT_CURRENT);
                    String strOriginated = (String) mapCADobj.get(DomainConstants.SELECT_ORIGINATED);
                    String strLastPromote = (String) mapCADobj.get(STRING_CURRENTACTUAL);
                    if (UIUtil.isNotNullAndNotEmpty(strLastPromote.toString())) {
                        SimpleDateFormat currentDateFormat = new SimpleDateFormat(STRING_DATEFORMAT);

                        SimpleDateFormat requiredDateFormat = new SimpleDateFormat(STRING_DATEFORMATDMMYY);
                        Date date = currentDateFormat.parse(strLastPromote.toString());
                        strLastPromote = requiredDateFormat.format(date);
                    }

                    String strOwner = (String) mapCADobj.get(DomainConstants.SELECT_OWNER);
                    if (strOwner.length() > 9)
                        strOwner = strOwner.substring(0, 9);
                    // TODO use of StringBuilder instead of concat
                    // TODO move XML tags to TigerConstant file
                    String strDescription = StringEscapeUtils.escapeXml10((String) mapCADobj.get("attribute[" + TigerConstants.ATTRIBUTE_MCADINTEG_COMMENT + "].value"));
                    if (strDescription.length() > 82) {
                        strDescription = TigerUtils.replaceAndWordWrap(context, strDescription, 82);
                    }

                    strXML.append("\n<BASISDEFINITIONHISTORY>");
                    strXML.append("\n\t<REVISION>" + strRev + "</REVISION>");
                    strXML.append("\n\t<STATE>" + strState + "</STATE>");
                    strXML.append("\n\t<LASTPROMOTE>" + strLastPromote + "</LASTPROMOTE>");
                    strXML.append("\n\t<OWNER>" + strOwner + "</OWNER>");
                    strXML.append("\n\t<CHECKINREASON>" + XSSUtil.encodeForXML(context, strDescription) + "</CHECKINREASON>");
                    strXML.append("\n\t<ORIGINATED>" + strOriginated + "</ORIGINATED>");
                    strXML.append("\n</BASISDEFINITIONHISTORY>");
                    mapOfBasisDefHistory.put(STRING_REVISION, strRev);
                    mapOfBasisDefHistory.put(STATUS, strState);
                    mapOfBasisDefHistory.put(STRING_DATEOFCHANGE, strLastPromote);
                    mapOfBasisDefHistory.put(STRING_AUTHOR, strOwner);
                    mapOfBasisDefHistory.put(STRING_DESCRIPTION, TigerUtils.replaceWordCharsForPDF(context, strDescription));
                    String[] strArrayOfSrq = new String[iSeqSize];

                    for (int j = 0; j < iSeqSize; j++) {
                        strArrayOfSrq[j] = (String) mapOfBasisDefHistory.get(slSequence.get(j));
                    }
                    listOfBasisDefHistory.add(strArrayOfSrq);
                }

                strXML.append("\n<BASISDEFINITIONHISTORY>\t");
                strXML.append("\n\t<REVISION>REV</REVISION>" + "\n\t<STATE> STATUS </STATE>\n\t<LASTPROMOTE>DATE OF CHANGE</LASTPROMOTE>"
                        + "\n\t<OWNER>AUTHOR</OWNER>\n\t<CHECKINREASON>CHANGE DESCRIPTION</CHECKINREASON><ORIGINATED>ORIGINATED</ORIGINATED>");
                strXML.append("\n</BASISDEFINITIONHISTORY>");

                strXMLFINAL.append(strXML);
                mapBasisDefHistoryBlock.put(XML, strXMLFINAL);
                mapBasisDefHistoryBlock.put(List, listOfBasisDefHistory);
            }
        } catch (Exception e) {
            logger.error("Error in generateBasisDefHistoryBlock: ", e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return mapBasisDefHistoryBlock;
    }

    /**
     * This method will generate data for CAD Block for Title Block.
     * @param context
     * @param dObj
     * @throws Exception
     * @author PSI
     */
    private Map generateCADBlock(Context context, DomainObject dObj) throws Exception {

        StringBuilder strXMLFINAL = new StringBuilder(64);
        StringBuilder strXMLCONTENT = new StringBuilder(64);
        StringBuilder strXMLCADINFO = new StringBuilder(64);
        List<String[]> listOfCADBlock = new ArrayList<String[]>();
        Map mapCADBlock = new HashMap();
        try {
            StringBuilder strXML = new StringBuilder();

            StringList slObjSel = new StringList(17);
            slObjSel.add(DomainConstants.SELECT_DESCRIPTION);
            // TODO use of StringBuilder instead of concat
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS1).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS2).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS3).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS4).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_UNDIMENSIONEDRADIUS).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_ANGULARTOLERANCE).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_LINEARTOLERANCE).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_SCALE).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DRAWINGVIEWCONVENTION).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DRAWINGFORMAT).append("].value").toString());
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_FOLIONUMBER).append("].value").toString());

            slObjSel.add(DomainConstants.SELECT_CURRENT);
            slObjSel.add(STRING_CURRENTACTUAL);
            slObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE).append("].value").toString());
            slObjSel.add(DomainConstants.SELECT_NAME);
            slObjSel.add(DomainConstants.SELECT_REVISION);
            slObjSel.add(new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_ACTIVEVERSION).append("].to.").append(DomainConstants.SELECT_REVISION).toString());
            Map mpCadObj = dObj.getInfo(context, slObjSel);
            String strDesc = StringEscapeUtils.escapeXml10((String) mpCadObj.get(DomainConstants.SELECT_DESCRIPTION));
            if (strDesc.length() > 50) {
                strDesc = TigerUtils.replaceAndWordWrap(context, strDesc, 50);
            }
            if (strDesc.length() > 100) {
                int loc = StringUtils.ordinalIndexOf(strDesc, String_LTBRGT, 2);
                if (loc > 0) {
                    strDesc = strDesc.substring(0, loc);
                    strDesc = new StringBuilder(strDesc).append(STRING_SEPERATOR_DOT).toString();
                }
            }

            String strDimensionSTD1 = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS1).append("].value").toString());
            String strDimensionSTD2 = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS2).append("].value").toString());
            String strDimensionSTD3 = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS3).append("].value").toString());
            String strDimensionSTD4 = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DIMENSIONINGSTANDARDS4).append("].value").toString());
            String strUnDimensionedR = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_UNDIMENSIONEDRADIUS).append("].value").toString());
            String strAngularTolerance = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_ANGULARTOLERANCE).append("].value").toString());
            String strLinearTolerance = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_LINEARTOLERANCE).append("].value").toString());
            String strScale = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_SCALE).append("].value").toString());
            String strDrawingViewConvention = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DRAWINGVIEWCONVENTION).append("].value").toString());
            String strDrawingFormat = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_DRAWINGFORMAT).append("].value").toString());
            String strFolioNumber = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_FOLIONUMBER).append("].value").toString());

            String strState = (String) mpCadObj.get(DomainConstants.SELECT_CURRENT);
            String strLastPromotion = (String) mpCadObj.get(STRING_CURRENTACTUAL);

            String strGeometryType = (String) mpCadObj.get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE).append("].value").toString());
            String strName = (String) mpCadObj.get(DomainConstants.SELECT_NAME);

            // MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-us"), new IEFGlobalCache());
            // BusinessObject activeMinorBus = mxUtil.getActiveMinor(context, dObj);
            // String strRevision = activeMinorBus.getRevision();
            String strRevision = DomainConstants.EMPTY_STRING;
            try {
                TigerUtils.pushContextToSuperUser(context);
                strRevision = dObj.getInfo(context,
                        new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_ACTIVEVERSION).append("].to.").append(DomainConstants.SELECT_REVISION).toString());
            } finally {
                ContextUtil.popContext(context);
            }
            int iLengthCad = strName.length();
            // if name greater than 19 chars then display 16 chars of name+...
            if (iLengthCad > 19) {
                strName = strName.substring(0, 16);
                strName = new StringBuilder(strName).append(STRING_SEPERATOR_DOT).toString();
            }
            StringBuilder sbNameConcat = new StringBuilder("CAD (");
            sbNameConcat.append(strGeometryType);
            sbNameConcat.append("): ");
            sbNameConcat.append(strName);
            sbNameConcat.append(strRevision);
            // TODO move XML tags to TigerConstant file
            strXMLCADINFO.append("\n<CADNAME>" + sbNameConcat + "</CADNAME>");
            strXMLCADINFO.append("\n<CADREVISION>" + strRevision + "</CADREVISION>");
            strXMLCADINFO.append("\n<SCALE>" + strScale + "</SCALE>");
            strXMLCADINFO.append("\n<VIEWCONV>" + strDrawingViewConvention + "</VIEWCONV>");
            strXMLCADINFO.append("\n<FORMAT>" + strDrawingFormat + "</FORMAT>");
            strXMLCADINFO.append("\n<SHEET>" + strFolioNumber + " </SHEET>");
            strXMLCADINFO.append("\n<STATUS>" + strState + " </STATUS>");
            SimpleDateFormat currentDateFormat = new SimpleDateFormat(STRING_DATEFORMAT);

            SimpleDateFormat requiredDateFormat = new SimpleDateFormat(STRING_DATEFORMATDMMYY);
            Date date = currentDateFormat.parse(strLastPromotion);

            strXMLCADINFO.append("\n<PROMOTIONDATE>" + requiredDateFormat.format(date) + "</PROMOTIONDATE>");

            if (strDimensionSTD1.length() > 15) {
                strDimensionSTD1 = strDimensionSTD1.substring(0, 15);
            }
            if (strDimensionSTD2.length() > 15) {
                strDimensionSTD2 = strDimensionSTD2.substring(0, 15);
            }
            if (strDimensionSTD3.length() > 15) {
                strDimensionSTD3 = strDimensionSTD3.substring(0, 15);
            }
            if (strDimensionSTD4.length() > 15) {
                strDimensionSTD4 = strDimensionSTD4.substring(0, 15);
            }

            StringBuilder sbDimensionSTD = new StringBuilder(DomainConstants.EMPTY_STRING);
            sbDimensionSTD.append(strDimensionSTD1);
            sbDimensionSTD.append(TigerConstants.SEPERATOR_COMMA);
            sbDimensionSTD.append(strDimensionSTD2);
            sbDimensionSTD.append(TigerConstants.SEPERATOR_COMMA);
            sbDimensionSTD.append(strDimensionSTD3);
            sbDimensionSTD.append(TigerConstants.SEPERATOR_COMMA);
            sbDimensionSTD.append(strDimensionSTD4);

            strXMLCONTENT.append("\n\t<DIMENSIONINGSTANDARD>" + sbDimensionSTD + "\n</DIMENSIONINGSTANDARD>");

            strXMLCONTENT.append("\n\t<INNERRADIUS>" + strUnDimensionedR + "</INNERRADIUS>");

            strXMLCONTENT.append("\n\t<ANGULARTOLERANCE>" + strAngularTolerance + "</ANGULARTOLERANCE>");
            strXMLCONTENT.append("\n\t<LINEARTOLERANCE>" + strLinearTolerance + "</LINEARTOLERANCE>");
            strXMLCONTENT.append("\n\t<CADINFO>" + strXMLCADINFO + "\n</CADINFO>");
            strXMLCONTENT.append("\n\t<CADINFO>" + strXMLCADINFO + "\n</CADINFO>");

            strXML.append("\n\t<HEADING>" + XSSUtil.encodeForXML(context, strDesc) + "</HEADING>");
            strXML.append("\n\t<CONTENT>" + strXMLCONTENT + "</CONTENT>");

            strXMLFINAL.append("\n<CADBLOCK>");
            strXMLFINAL.append(strXML);
            strXMLFINAL.append("\n</CADBLOCK>");

            String[] strArrayOfSrq = new String[12];

            strArrayOfSrq[0] = TigerUtils.replaceWordCharsForPDF(context, strDesc);

            strArrayOfSrq[1] = sbDimensionSTD.toString().replace(TigerConstants.SEPERATOR_COMMA, STRING_BR);
            strArrayOfSrq[2] = strUnDimensionedR;

            StringBuilder sbAngularTolerance = new StringBuilder();
            sbAngularTolerance.append(StringEscapeUtils.unescapeHtml4(STRING_PLUSMN));
            sbAngularTolerance.append(strAngularTolerance);
            sbAngularTolerance.append(StringEscapeUtils.unescapeHtml4(STRING_DEG));

            strArrayOfSrq[3] = sbAngularTolerance.toString();

            StringBuilder sbLinearTolerance = new StringBuilder();
            sbLinearTolerance.append(StringEscapeUtils.unescapeHtml4(STRING_PLUSMN));
            sbLinearTolerance.append(strLinearTolerance);
            sbLinearTolerance.append(STRING_MM);

            strArrayOfSrq[4] = sbLinearTolerance.toString();
            if (STRING_US.equalsIgnoreCase(strDrawingViewConvention))
                strArrayOfSrq[5] = STRING_THIRDANGLEJPG;
            else
                strArrayOfSrq[5] = STRING_FIRSTANGLEJPG;
            strArrayOfSrq[6] = sbNameConcat.toString();
            strArrayOfSrq[7] = strScale;
            strArrayOfSrq[8] = strDrawingFormat;
            strArrayOfSrq[9] = strFolioNumber;
            strArrayOfSrq[10] = strState;
            strArrayOfSrq[11] = requiredDateFormat.format(date);

            listOfCADBlock.add(strArrayOfSrq);

            mapCADBlock.put(XML, strXMLFINAL);
            mapCADBlock.put(List, listOfCADBlock);

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in generateCADBlock: ", e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return mapCADBlock;

    }

    /**
     * This method will generate data for ULS Block for Title Block.
     * @param context
     * @param dObj
     * @throws Exception
     * @author PSI
     */
    private Map generateULSBlock(Context context, DomainObject dObj, String strUpperLevelStructure) throws Exception {

        StringBuilder strXMLFINAL = new StringBuilder();
        List<String[]> listOfULSBlock = new ArrayList<String[]>();
        Map mapULSBlock = new HashMap();

        StringBuilder strXML = new StringBuilder();
        StringList slObjSel = new StringList();

        String[] strArrayOfSrq = new String[3];
        // StringBuilder sbProgramProject = new StringBuilder();
        // StringBuilder sbVehicle = new StringBuilder();
        StringList slVehicle = new StringList();
        StringList slProgramProject = new StringList();

        StringList strObjSel = new StringList();

        StringBuilder sbOrganizationInfo = new StringBuilder();
        String strProductDesc = DomainConstants.EMPTY_STRING;
        String strAddress = DomainConstants.EMPTY_STRING;
        String strCity = DomainConstants.EMPTY_STRING;
        String strPostalCode = DomainConstants.EMPTY_STRING;
        String strPhoneNum = DomainConstants.EMPTY_STRING;
        String strIsRnDCenter = DomainConstants.EMPTY_STRING;

        String postalcode, city, country = DomainConstants.EMPTY_STRING;
        Map mpRnDCenter = new HashMap();
        boolean isSameMasterRndCenter = false;

        try {
            slObjSel.add(DomainConstants.SELECT_ID);
            slObjSel.add(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_GBOM).append("].from.to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT).append("].from.")
                    .append(DomainConstants.SELECT_TYPE).toString());
            slObjSel.add(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_GBOM).append("].from.to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT).append("].from.")
                    .append(DomainConstants.SELECT_NAME).toString());
            slObjSel.add(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_GBOM).append("].from.to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT).append("].from.")
                    .append(DomainConstants.SELECT_CURRENT).toString());

            strObjSel.add(DomainConstants.SELECT_ID);
            strObjSel.add(DomainConstants.SELECT_DESCRIPTION);
            strObjSel.add(new StringBuilder(64).append("attribute[").append(DomainConstants.ATTRIBUTE_POSTAL_CODE).append("]"));
            strObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_ADDRESS1).append("]"));
            strObjSel.add(new StringBuilder(64).append("attribute[").append(DomainConstants.ATTRIBUTE_CITY).append("]"));
            strObjSel.add(new StringBuilder(64).append("attribute[").append(DomainConstants.ATTRIBUTE_COUNTRY).append("]"));
            strObjSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_ORGANIZATION_PHONE_NUMBER).append("]"));
            MapList mpPartList = getConnectedPart(context, dObj, slObjSel, DomainConstants.EMPTY_STRINGLIST, 0);
            if (TigerConstants.STRING_YES.equalsIgnoreCase(strUpperLevelStructure)) {
                if (!mpPartList.isEmpty()) {
                    String strWrapProgramProject = DomainConstants.EMPTY_STRING;
                    String strWrapVehicle = DomainConstants.EMPTY_STRING;
                    int iPSize = mpPartList.size();
                    for (int i = 0; i < iPSize; i++) {
                        Map mpPart = (Map) mpPartList.get(i);
                        StringList slPrgrmPrjctVehicleName = (StringList) mpPart.get(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_GBOM).append("].from.to[")
                                .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT).append("].from.").append(DomainConstants.SELECT_NAME).toString());
                        StringList slPrgrmPrjctVehicleType = (StringList) mpPart.get(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_GBOM).append("].from.to[")
                                .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT).append("].from.").append(DomainConstants.SELECT_TYPE).toString());
                        StringList slPrgrmPrjctVehicleState = (StringList) mpPart.get(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_GBOM).append("].from.to[")
                                .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPRODUCT).append("].from.").append(DomainConstants.SELECT_CURRENT).toString());

                        if (slPrgrmPrjctVehicleName != null) {
                            int iPPVsize = slPrgrmPrjctVehicleName.size();
                            for (int j = 0; j < iPPVsize; j++) {
                                if (TigerConstants.TYPE_PSS_VEHICLE.equals(slPrgrmPrjctVehicleType.get(j)) && !TigerConstants.STATE_VEHICLE_INACTIVE.equals(slPrgrmPrjctVehicleState.get(j))) {
                                    String strVeh = (String) slPrgrmPrjctVehicleName.get(j);
                                    if (!slVehicle.contains(strVeh)) {
                                        slVehicle.add(strVeh);
                                    }
                                } else if (TigerConstants.TYPE_PSS_PROGRAMPROJECT.equals(slPrgrmPrjctVehicleType.get(j)) && !TigerConstants.STATE_NONAWARDED.equals(slPrgrmPrjctVehicleState.get(j))
                                        && !TigerConstants.STATE_OBSOLETE.equals(slPrgrmPrjctVehicleState.get(j))) {
                                    String strProg = (String) slPrgrmPrjctVehicleName.get(j);
                                    if (!slProgramProject.contains(strProg)) {
                                        slProgramProject.add(strProg);
                                    }
                                }
                            }
                        }
                    }

                    if (!slProgramProject.isEmpty()) {
                        strWrapProgramProject = slProgramProject.toString().replaceAll(", ", ",").replace("[", DomainConstants.EMPTY_STRING).replace("]", DomainConstants.EMPTY_STRING);
                        if (strWrapProgramProject.length() > 55) {
                            strWrapProgramProject = new StringBuilder(strWrapProgramProject.substring(0, 52)).append(STRING_SEPERATOR_DOT).toString();
                        }
                    }
                    if (!slVehicle.isEmpty()) {
                        strWrapVehicle = slVehicle.toString().replaceAll(", ", ",").replace("[", DomainConstants.EMPTY_STRING).replace("]", DomainConstants.EMPTY_STRING);
                        if (strWrapVehicle.length() > 55) {
                            strWrapVehicle = new StringBuilder(strWrapVehicle.substring(0, 52)).append(STRING_SEPERATOR_DOT).toString();
                        }
                    }

                    strWrapProgramProject = TigerUtils.replaceWordCharsForXML(context, strWrapProgramProject);
                    strWrapVehicle = TigerUtils.replaceWordCharsForXML(context, strWrapVehicle);
                    // TODO move XML tags to TigerConstant file
                    strXML.append("\n\t<PROGRAM>");
                    strXML.append(XSSUtil.encodeForXML(context, strWrapProgramProject));
                    strXML.append("</PROGRAM>");
                    strXML.append("\n\t<VEHICLE>");
                    strXML.append(XSSUtil.encodeForXML(context, strWrapVehicle));
                    strXML.append("</VEHICLE>");
                    StringBuilder sbPPV = new StringBuilder(": ").append(strWrapProgramProject);
                    sbPPV.append(STRING_BRBR);
                    sbPPV.append(": ").append(strWrapVehicle);

                    strArrayOfSrq[0] = sbPPV.toString();
                } else {
                    strXML.append("\n\t<PROGRAM>");
                    strXML.append("</PROGRAM>");
                    strXML.append("\n\t<VEHICLE>");
                    strXML.append("</VEHICLE>");

                    strArrayOfSrq[0] = new StringBuilder(16).append(": ").append(STRING_BRBR).append(": ").toString();
                }
                strArrayOfSrq[1] = STRING_FAURECIA_LOGO_RVBJPG;
            } else {
                strArrayOfSrq[0] = new StringBuilder(16).append(": ").append(STRING_BRBR).append(": ").toString();
                strArrayOfSrq[1] = STRING_FAURECIA_LOGO_RVBJPG;
            }
            // if Multiple Program Project exist in CS
            // get Collab space of drawing object
            String strDrawingCollabSpace = dObj.getInfo(context, STRING_PROJECT);
            String whereCond = "project == const'" + strDrawingCollabSpace + "' &&  from[" + TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY + "].to.type==const'" + TigerConstants.TYPE_PSS_RnDCENTER
                    + "'";

            StringBuilder sbULSInfo = new StringBuilder();
            StringList strRelSel = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);
            strRelSel.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_MASTER_RnD_CENTER).append("].value").toString());

            MapList mpPPFromCollabSpace = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PROGRAMPROJECT, TigerConstants.VAULT_ESERVICEPRODUCTION, whereCond,
                    new StringList(DomainConstants.SELECT_ID));

            int iPPfromCS = mpPPFromCollabSpace.size();
            if (iPPfromCS > 0) {
                String strMasterRndCenterId = DomainConstants.EMPTY_STRING;
                int i = 0;
                boolean doExit = false;
                for (i = 0; i < iPPfromCS; i++) {
                    Map mpPP = (Map) mpPPFromCollabSpace.get(i);
                    String strPPID = (String) mpPP.get(DomainConstants.SELECT_ID);
                    BusinessObject domPPObj = new BusinessObject(strPPID);

                    ExpansionIterator expItRndCenter = domPPObj.getExpansionIterator(context, TigerConstants.RELATIONSHIP_PSS_PRODUCTIONENTITY, TigerConstants.TYPE_PSS_RnDCENTER, strObjSel, strRelSel,
                            false, true, (short) 0, null, null, (short) 1000, false, true, (short) 1000);
                    MapList mpConnectRnDCenters = FrameworkUtil.toMapList(expItRndCenter, (short) 0, null, null, null, null);
                    expItRndCenter.close();
                    int iRndSize = mpConnectRnDCenters.size();

                    // Only one PP in current Collaborative space
                    if (iPPfromCS == 1) {
                        if (iRndSize > 0) {
                            isSameMasterRndCenter = true;
                            if (iRndSize == 1) {
                                mpRnDCenter = (Map) mpConnectRnDCenters.get(0);
                                strProductDesc = (String) ((StringList) mpRnDCenter.getOrDefault(DomainConstants.SELECT_DESCRIPTION, " ")).get(0);
                            } else {
                                int j = 0;
                                for (j = 0; j < iRndSize; j++) {
                                    Map mpTempRnDCenter = (Map) mpConnectRnDCenters.get(j);
                                    strIsRnDCenter = (String) ((StringList) mpTempRnDCenter
                                            .get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_MASTER_RnD_CENTER).append("].value").toString())).get(0);
                                    if (TigerConstants.STRING_YES.equalsIgnoreCase(strIsRnDCenter)) {
                                        mpRnDCenter = mpTempRnDCenter;
                                        strProductDesc = (String) ((StringList) mpRnDCenter.getOrDefault(DomainConstants.SELECT_DESCRIPTION, " ")).get(0);
                                        doExit = true;
                                        break;
                                    }
                                }
                            }
                        }

                    } else if (iPPfromCS > 1) {
                        int j = 0;
                        for (j = 0; j < iRndSize; j++) {
                            Map mpTempRnDCenter = (Map) mpConnectRnDCenters.get(j);
                            strIsRnDCenter = (String) ((StringList) mpTempRnDCenter
                                    .get(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_MASTER_RnD_CENTER).append("].value").toString())).get(0);
                            if (TigerConstants.STRING_YES.equalsIgnoreCase(strIsRnDCenter)) {
                                mpRnDCenter = mpTempRnDCenter;
                                String strMasterRndCenter = (String) ((StringList) mpRnDCenter.get(DomainConstants.SELECT_ID)).get(0);
                                if (strMasterRndCenterId.isEmpty() || strMasterRndCenter.equals(strMasterRndCenterId)) {
                                    strMasterRndCenterId = strMasterRndCenter;
                                    isSameMasterRndCenter = true;
                                } else if (!strMasterRndCenter.equals(strMasterRndCenterId)) {
                                    isSameMasterRndCenter = false;
                                    doExit = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (doExit) {
                        break;
                    }
                }
                if (isSameMasterRndCenter && !mpRnDCenter.isEmpty()) {
                    strProductDesc = (String) ((StringList) mpRnDCenter.getOrDefault(DomainConstants.SELECT_DESCRIPTION, " ")).get(0);

                    strAddress = (String) ((StringList) mpRnDCenter.getOrDefault("attribute[" + TigerConstants.ATTRIBUTE_ADDRESS1 + "]", " ")).get(0);
                    postalcode = (String) ((StringList) mpRnDCenter.getOrDefault("attribute[" + DomainConstants.ATTRIBUTE_POSTAL_CODE + "]", " ")).get(0);
                    city = (String) ((StringList) mpRnDCenter.getOrDefault("attribute[" + DomainConstants.ATTRIBUTE_CITY + "]", " ")).get(0);
                    country = (String) ((StringList) mpRnDCenter.getOrDefault("attribute[" + DomainConstants.ATTRIBUTE_COUNTRY + "]", " ")).get(0);

                    country = country.toUpperCase();
                    strPhoneNum = (String) ((StringList) mpRnDCenter.getOrDefault("attribute[" + TigerConstants.ATTRIBUTE_ORGANIZATION_PHONE_NUMBER + "]", " ")).get(0);
                    strCity = new StringBuilder(postalcode).append(" ").append(city).append(" - ").append(country).toString();

                    if (strProductDesc.length() > 50)
                        strProductDesc = strProductDesc.substring(0, 50);
                    if (strAddress.length() > 50)
                        strAddress = strAddress.substring(0, 50);
                    if (strCity.length() > 50)
                        strCity = strCity.substring(0, 50);
                    if (strPhoneNum.length() > 18)
                        strPhoneNum = strPhoneNum.substring(0, 18);
                }

                strPhoneNum = TigerUtils.replaceWordCharsForXML(context, strPhoneNum);
                strProductDesc = TigerUtils.replaceWordCharsForXML(context, strProductDesc);
                strAddress = TigerUtils.replaceWordCharsForXML(context, strAddress);
                strPostalCode = TigerUtils.replaceWordCharsForXML(context, strCity);
                sbOrganizationInfo.append(strProductDesc);
                sbOrganizationInfo.append(String_LTBRGT);
                sbOrganizationInfo.append(strAddress);
                sbOrganizationInfo.append(String_LTBRGT);
                sbOrganizationInfo.append(strPostalCode);
                sbOrganizationInfo.append(String_LTBRGT);
                sbOrganizationInfo.append(strPhoneNum);
                // TODO move XML tags to TigerConstant file
                strXML.append("\n\t<DEPT_NAME>");
                strXML.append(XSSUtil.encodeForXML(context, strProductDesc));
                strXML.append("</DEPT_NAME>");
                strXML.append("\n\t<DEPT_STREET>");
                strXML.append(XSSUtil.encodeForXML(context, strAddress));
                strXML.append("</DEPT_STREET>");
                strXML.append("\n\t<DEPT_CITY>");
                strXML.append(XSSUtil.encodeForXML(context, strPostalCode));
                strXML.append("</DEPT_CITY>");
                strXML.append("\n\t<DEPT_PHONE>");
                strXML.append(XSSUtil.encodeForXML(context, strPhoneNum));
                strXML.append("</DEPT_PHONE>");

                sbULSInfo.append(TigerUtils.replaceWordCharsForPDF(context, strProductDesc));
                sbULSInfo.append(STRING_BR);
                sbULSInfo.append(TigerUtils.replaceWordCharsForPDF(context, strAddress));
                sbULSInfo.append(STRING_BR);
                sbULSInfo.append(TigerUtils.replaceWordCharsForPDF(context, strCity));
                sbULSInfo.append(STRING_BR);
                sbULSInfo.append(TigerUtils.replaceWordCharsForPDF(context, strPhoneNum));
            }

            strXML.append("\n\t<DESIGNRESPONSIBILITY>");
            strXML.append(XSSUtil.encodeForXML(context, sbOrganizationInfo.toString()));
            strXML.append("</DESIGNRESPONSIBILITY>");
            strArrayOfSrq[2] = sbULSInfo.toString();
            listOfULSBlock.add(strArrayOfSrq);

            strXMLFINAL.append("\n<ULSBLOCK>\t");
            strXMLFINAL.append(strXML);
            strXMLFINAL.append("\n</ULSBLOCK>");

            mapULSBlock.put(XML, strXMLFINAL);
            mapULSBlock.put(List, listOfULSBlock);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return mapULSBlock;
    }

    /**
     * This method will generate Data for FCM Pattern for Title Block.
     * @param context
     * @param dObj
     * @throws Exception
     * @author PSI
     */
    private Map generateDrawingBannnerFCM(Context context, DomainObject dObj, String strLinkedParts) throws Exception {
        StringBuilder strXMLFINAL = new StringBuilder(64);
        List<String[]> listOfDrawingBannnerFCM = new ArrayList<String[]>();
        Map mapDrawingBannnerFCM = new HashMap();
        String[] strArrayOfSrq = new String[1];
        StringBuilder strXML = new StringBuilder(64);
        StringList slObjSel = new StringList(5);
        StringList slRelSel = new StringList(1);

        MapList mpPartListChartedDrawing = new MapList();
        MapList mpPartList = new MapList();
        try {
            String strCADState = dObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            String strDrawingis = EnoviaResourceBundle.getProperty(context, "emxIEFDesignCenterStringResource", context.getLocale(), "emxIEFDesignCenter.TitleBlock.FCMDrawingIs");
            String strCDHighestState = EnoviaResourceBundle.getProperty(context, "emxIEFDesignCenterStringResource", context.getLocale(),
                    "emxIEFDesignCenter.TitleBlock.FCMChartedDrawingHighestState");
            String strPurposeIs = EnoviaResourceBundle.getProperty(context, "emxIEFDesignCenterStringResource", context.getLocale(), "emxIEFDesignCenter.TitleBlock.PurposeIs");
            String strPartNR = EnoviaResourceBundle.getProperty(context, "emxIEFDesignCenterStringResource", context.getLocale(), "emxIEFDesignCenter.TitleBlock.PartNR");
            strDrawingis = String.format(strDrawingis, strCADState);
            strXML.append(strDrawingis);

            if (UIUtil.isNotNullAndNotEmpty(strLinkedParts) && strLinkedParts.equalsIgnoreCase("yes")) {
                slObjSel.add(DomainConstants.SELECT_ID);
                slObjSel.add(DomainConstants.SELECT_CURRENT);
                slObjSel.add(DomainConstants.SELECT_POLICY);
                slObjSel.add(DomainConstants.SELECT_NAME);
                slObjSel.add(DomainConstants.SELECT_REVISION);

                slRelSel.add(DomainConstants.SELECT_RELATIONSHIP_NAME);

                MapList mpAllPartList = getConnectedPart(context, dObj, slObjSel, slRelSel, 0);

                int iSize = mpAllPartList.size();
                int i = 0;
                for (i = 0; i < iSize; i++) {
                    Map mapAllPart = (Map) mpAllPartList.get(i);
                    if (TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING.equalsIgnoreCase((String) ((StringList) mapAllPart.get(DomainConstants.SELECT_RELATIONSHIP_NAME)).get(0)))
                        mpPartListChartedDrawing.add(mapAllPart);
                    else
                        mpPartList.add(mapAllPart);
                }
                int iCDSize = mpPartListChartedDrawing.size();
                if (iCDSize > 0) {
                    int highestIndex = 0;
                    int highestCountIndex = 0;
                    DomainObject domPartObj = DomainObject.newInstance(context);
                    for (int j = 0; j < iCDSize; j++) {
                        Map mapPart = (Map) mpPartListChartedDrawing.get(j);
                        String strPartId = (String) ((StringList) mapPart.get(DomainConstants.SELECT_ID)).get(0);
                        String strPartState = (String) ((StringList) mapPart.get(DomainConstants.SELECT_CURRENT)).get(0);
                        domPartObj.setId(strPartId);
                        StateList prStateLst = domPartObj.getStates(context);

                        // Loop through states list and get the index of current
                        // state
                        int iCurrentStateIndex = 0;
                        int iStateSize = prStateLst.size();
                        for (int k = 0; k < iStateSize; k++) {
                            if ((((State) prStateLst.elementAt(k)).getName()).equalsIgnoreCase(strPartState))
                                iCurrentStateIndex = k;
                        }

                        // If current state is higher update highest index
                        if (iCurrentStateIndex > highestIndex && !"Obsolete".equalsIgnoreCase(strPartState)) {
                            highestIndex = iCurrentStateIndex;
                            highestCountIndex = j;
                        }
                    }

                    // Use highest index for highest state of Part amongst all Part
                    // Specifications
                    Map mapHighestIndexPart = (Map) mpPartListChartedDrawing.get(highestCountIndex);
                    String strPartState = (String) ((StringList) mapHighestIndexPart.get(DomainConstants.SELECT_CURRENT)).get(0);
                    String strPolicy = (String) ((StringList) mapHighestIndexPart.get(DomainConstants.SELECT_POLICY)).get(0);
                    strPartState = MCADMxUtil.getNLSName(context, "State", strPartState, "Policy", strPolicy, context.getLocale().getLanguage());
                    strCDHighestState = String.format(strCDHighestState, strPartState);
                    strXML.append(strCDHighestState);
                }

                if (mpPartList.size() == 1) {
                    Map mapPart = (Map) mpPartList.get(0);
                    String strPartId = (String) ((StringList) mapPart.get(DomainConstants.SELECT_ID)).get(0);
                    String strPartName = (String) ((StringList) mapPart.get(DomainConstants.SELECT_NAME)).get(0);
                    String strPartRevision = (String) ((StringList) mapPart.get(DomainConstants.SELECT_REVISION)).get(0);
                    String strPartState = (String) ((StringList) mapPart.get(DomainConstants.SELECT_CURRENT)).get(0);

                    String strPolicy = (String) ((StringList) mapPart.get(DomainConstants.SELECT_POLICY)).get(0);
                    strPartState = MCADMxUtil.getNLSName(context, "State", strPartState, "Policy", strPolicy, context.getLocale().getLanguage());
                    strPartNR = String.format(strPartNR, strPartName, strPartRevision, strPartState);
                    strXML.append(strPartNR);

                    DomainObject domPartObj = DomainObject.newInstance(context, strPartId);
                    String strPurposeOfRelease = DomainConstants.EMPTY_STRING;

                    StringList slSelects = new StringList();
                    slSelects.add(new StringBuilder(64).append("attribute[").append(TigerConstants.ATTRIBUTE_PSS_PURPOSEOFRELEASE_DEVPART).append("]"));
                    slSelects.add(new StringBuilder(64).append("to[").append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM).append("].from.to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION)
                            .append("].from.attribute[").append(TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE).append("].value").toString());

                    slSelects.add(new StringBuilder(64).append("to[").append(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM).append("].from.to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION)
                            .append("].from.attribute[").append(TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE).append("].value").toString());

                    Map mapPurposeOfRelease = domPartObj.getInfo(context, slSelects);

                    strPurposeOfRelease = purposeOfRelease(context, mapPurposeOfRelease, strPolicy, "FCM");
                    if (UIUtil.isNotNullAndNotEmpty(strPurposeOfRelease)) {
                        strPurposeIs = String.format(strPurposeIs, strPurposeOfRelease);
                        strXML.append(strPurposeIs);
                    }
                }
            }
            // TODO move XML tags to TigerConstant file
            strXMLFINAL.append("\n<DRAWINGBANNER>");
            strXMLFINAL.append(strXML);
            strXMLFINAL.append("</DRAWINGBANNER>");

            strArrayOfSrq[0] = strXML.toString();

            listOfDrawingBannnerFCM.add(strArrayOfSrq);

            mapDrawingBannnerFCM.put(XML, strXMLFINAL);
            mapDrawingBannnerFCM.put(List, listOfDrawingBannnerFCM);
        } catch (Exception e) {
            logger.error("Error in generateDrawingBannnerFCM: ", e);
            e.printStackTrace();
            throw new Exception(e.getLocalizedMessage(), e);
        }

        return mapDrawingBannnerFCM;

    }

    /**
     * This method will generate History Block for Title Block.
     * @param context
     * @param dObj
     * @param strNoRevision
     * @param strPattern
     * @throws Exception
     * @author PSI
     */
    public Map generateHistoryBlock(Context context, DomainObject dObj, String strNoRevision, String strPattern) throws Exception {

        StringBuilder strXMLFINAL = new StringBuilder(256);
        List<String[]> listOfHistory = new ArrayList<String[]>();
        Map mapHistoryBlock = new HashMap();
        Map mapHistory = new HashMap();

        String strCOName = DomainConstants.EMPTY_STRING;
        String strCRName = DomainConstants.EMPTY_STRING;
        String strCODesc = DomainConstants.EMPTY_STRING;
        StringBuilder strXML = new StringBuilder(256);
        StringBuilder sbItemToChange = new StringBuilder(16);
        try {
            String strSequence = TigerAppConfigProperties.getPropertyValue(context, new StringBuilder(35).append(strPattern).append(STRING_HISTORYBLOCKCELLSEQUENCE).toString());
            StringList slSequence = FrameworkUtil.split(strSequence, TigerConstants.SEPERATOR_COMMA);
            int seqSize = slSequence.size();

            StringList strSelects = new StringList(2);
            strSelects.add(new StringBuilder(64).append("to[").append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM).append("].from.to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION)
                    .append("].from.current.actual").toString());
            strSelects.add(new StringBuilder(64).append("to[").append(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM).append("].from.to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION)
                    .append("].from.current.actual").toString());

            StringList strSelectsSingle = new StringList(5);
            strSelectsSingle.add(DomainConstants.SELECT_ID);
            strSelectsSingle.add(DomainConstants.SELECT_CURRENT);
            strSelectsSingle.add(DomainConstants.SELECT_REVISION);
            strSelectsSingle.add("islast");
            strSelectsSingle.add("first.id");

            MapList mpList = dObj.getRevisionsInfo(context, strSelectsSingle, strSelects);
            int iNoRevision = 3;
            int iMLSize = mpList.size();
            // MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-us"), new IEFGlobalCache());

            if (!mpList.isEmpty()) {
                if (TigerConstants.ALL.equals(strNoRevision)) {
                    iNoRevision = 0;
                } else {
                    iNoRevision = iMLSize - Integer.parseInt(strNoRevision);
                }

                if (iNoRevision < 0) {
                    iNoRevision = 0;
                }
                for (int i = iNoRevision; i < iMLSize; i++) {
                    strXML.append("<HISTORY>");
                    mapHistory = new HashMap();

                    Map mpId = (Map) mpList.get(i);
                    String strCadDrwId = (String) mpId.get(DomainConstants.SELECT_ID);
                    DomainObject domCatRev = DomainObject.newInstance(context, strCadDrwId);
                    // BusinessObject activeMinorBus = mxUtil.getActiveMinor(context, domCatRev);
                    // String strRevision = activeMinorBus.getRevision();
                    String strRevision = DomainConstants.EMPTY_STRING;
                    try {
                        TigerUtils.pushContextToSuperUser(context);
                        strRevision = domCatRev.getInfo(context,
                                new StringBuilder(64).append("from[").append(TigerConstants.RELATIONSHIP_ACTIVEVERSION).append("].to.").append(DomainConstants.SELECT_REVISION).toString());
                    } finally {
                        ContextUtil.popContext(context);
                    }
                    String strStatus = (String) mpId.get(DomainConstants.SELECT_CURRENT);

                    strXML.append(new StringBuilder(64).append("\n\t<REVISION>").append(strRevision).append("</REVISION>"));
                    mapHistory.put(STRING_REVISION, strRevision);

                    String ISLast = (String) mpId.get(STRING_ISLAST);
                    String firstID = (String) mpId.get(STRING_FIRST_ID);

                    String COID = DomainConstants.EMPTY_STRING;
                    boolean ISfirst = false;

                    StringBuilder sbCOCRName = new StringBuilder();
                    StringBuilder sbLastPromoteDate = new StringBuilder();
                    StringBuilder sbChangeAuthor = new StringBuilder();
                    String strChangeAuthorValue = DomainConstants.EMPTY_STRING;
                    StringBuilder strChangeDescr = new StringBuilder();
                    String strChangeDescrValue = DomainConstants.EMPTY_STRING;

                    StringList slCOList = new StringList();

                    StringBuilder sbRelPattern = new StringBuilder(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
                    sbRelPattern.append(TigerConstants.SEPERATOR_COMMA);
                    sbRelPattern.append(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);
                    sbRelPattern.append(TigerConstants.SEPERATOR_COMMA);
                    sbRelPattern.append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION);

                    StringBuilder sbTypePattern = new StringBuilder(ChangeConstants.TYPE_CHANGE_ACTION);
                    sbTypePattern.append(TigerConstants.SEPERATOR_COMMA);
                    sbTypePattern.append(ChangeConstants.TYPE_CHANGE_ORDER);

                    StringList slSelectsCO = new StringList();
                    slSelectsCO.add(DomainConstants.SELECT_NAME);
                    slSelectsCO.add(DomainConstants.SELECT_ID);
                    slSelectsCO.add(new StringBuilder(64).append("to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ORDER).append("].from.name").toString());
                    slSelectsCO.add(DomainConstants.SELECT_DESCRIPTION);
                    slSelectsCO.add(DomainConstants.SELECT_CURRENT);
                    slSelectsCO.add(new StringBuilder(64).append(DomainConstants.SELECT_CURRENT).append(TigerConstants.SEPERATOR_DOT).append(STRING_ACTUAL).toString());
                    slSelectsCO.add(DomainConstants.SELECT_ORIGINATED);
                    slSelectsCO.add(DomainConstants.SELECT_ORIGINATOR);
                    slSelectsCO.add(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                            .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append("].attribute[").append(TigerConstants.ATTRIBUTE_PSS_ROLE).append("].value").toString());
                    slSelectsCO.add(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                            .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append("].attribute[").append(TigerConstants.ATTRIBUTE_PSS_POSITION).append("].value").toString());

                    slSelectsCO.add(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                            .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append("].to.name").toString());

                    MapList mpCOList = domCatRev.getRelatedObjects(context, sbRelPattern.toString(), sbTypePattern.toString(), slSelectsCO, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 0,
                            null, null, 0);

                    Iterator itr = mpCOList.iterator();

                    while (itr.hasNext()) {
                        Map mapChange = (Map) itr.next();

                        String strType = (String) mapChange.get(DomainConstants.SELECT_TYPE);
                        String coState = (String) mapChange.get(DomainConstants.SELECT_CURRENT);
                        String strReasonForChnage = (String) mapChange.get(new StringBuilder(64).append("attribute[").append(ChangeConstants.ATTRIBUTE_REASON_FOR_CHANGE).append("]").toString());

                        if (ChangeConstants.TYPE_CHANGE_ACTION.equalsIgnoreCase(strType))
                            itr.remove();
                        else if (ChangeConstants.TYPE_CHANGE_ORDER.equalsIgnoreCase(strType) && (TigerConstants.STATE_CHANGEACTION_ONHOLD.equalsIgnoreCase(coState)
                                || TigerConstants.STATE_PSS_CANCELCAD_CANCELLED.equalsIgnoreCase(coState) || STRING_FORRELEASE.equalsIgnoreCase(strReasonForChnage))) {

                            itr.remove();
                        } else {
                            mapChange.replace(DomainConstants.SELECT_LEVEL, Integer.parseInt((String) mapChange.get(DomainConstants.SELECT_LEVEL)) - 1);
                        }
                    }

                    int iCOSize = mpCOList.size();

                    if (mpCOList.isEmpty()) {

                        sbCOCRName.append(" ");
                        strChangeDescr.append(" ");
                        sbChangeAuthor.append(" ");
                        sbLastPromoteDate.append(" ");
                        strXML.append("\n\t<STATUS></STATUS>");
                        mapHistory.put(STATUS, " ");
                    } else {

                        Map mapCOInfo = (Map) mpCOList.get(0);

                        String coState = (String) mapCOInfo.get(DomainConstants.SELECT_CURRENT);
                        String strReasonForChnage = (String) mapCOInfo.get(new StringBuilder(64).append("attribute[").append(ChangeConstants.ATTRIBUTE_REASON_FOR_CHANGE).append("]").toString());

                        StringBuilder sbStateKey = new StringBuilder("emxFramework.State.PSS_ChangeOrder.");
                        sbStateKey.append(coState.replaceAll(" ", "_"));
                        coState = EnoviaResourceBundle.getProperty(context, "Framework", sbStateKey.toString(), context.getSession().getLanguage());
                        COID = (String) mapCOInfo.get(DomainConstants.SELECT_ID);

                        String strPromoteDate = (String) mapCOInfo.get(new StringBuilder(64).append(DomainConstants.SELECT_CURRENT).append(".").append(STRING_ACTUAL).toString());

                        if (firstID.equalsIgnoreCase(strCadDrwId))
                            ISfirst = true;

                        if (strPromoteDate == null) {
                            sbLastPromoteDate.append(" ");
                        } else {
                            sbLastPromoteDate.append(strPromoteDate);
                        }

                        if (coState.equalsIgnoreCase(TigerConstants.STATE_PSS_CHANGEORDER_PREPARE)) {
                            sbChangeAuthor.append((String) mapCOInfo.get(DomainConstants.SELECT_ORIGINATOR));
                            sbLastPromoteDate.append((String) mapCOInfo.get(DomainConstants.SELECT_ORIGINATED));
                        } else {

                            Object slRole = (Object) mapCOInfo.get(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                                    .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append("].attribute[").append(TigerConstants.ATTRIBUTE_PSS_ROLE).append("].value").toString());

                            Object slPosition = (Object) mapCOInfo.get(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                                    .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append("].attribute[").append(TigerConstants.ATTRIBUTE_PSS_POSITION).append("].value").toString());

                            Object slPersonName = (Object) mapCOInfo.get(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                                    .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append("].to.name").toString());

                            if (slRole != null && slRole instanceof StringList && slPosition != null && slPersonName != null) {
                                StringList roles = (StringList) slRole;
                                StringList positions = (StringList) slPosition;
                                StringList persons = (StringList) slPersonName;
                                for (int l = 0; l < roles.size(); l++) {
                                    if (TigerConstants.ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD.equalsIgnoreCase((String) roles.get(l)) && STRING_LEAD.equals((String) positions.get(l))) {
                                        if (!UIUtil.isNullOrEmpty((String) persons.get(l))) {
                                            sbChangeAuthor.append((String) persons.get(l));
                                            sbChangeAuthor.append(STRING_SPACE_Tab);
                                        }
                                    }
                                }
                            } else if (slRole != null && slRole instanceof String && slPosition != null && slPersonName != null) {
                                if (TigerConstants.ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD.equalsIgnoreCase((String) slRole) && STRING_LEAD.equals((String) slPosition)) {
                                    if (!UIUtil.isNullOrEmpty((String) slPersonName)) {
                                        sbChangeAuthor.append((String) slPersonName);
                                        sbChangeAuthor.append(STRING_SPACE_Tab);
                                    }
                                }
                            }
                        }

                        strXML.append("\n\t<STATUS>" + coState + "</STATUS>");
                        mapHistory.put(STATUS, coState);

                        Object objCRName = mapCOInfo.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ORDER + "].from.name");
                        StringList slCRNumbers = new StringList();
                        if (objCRName != null && objCRName instanceof StringList) {
                            slCRNumbers = (StringList) objCRName;
                        } else if (objCRName != null && objCRName instanceof String) {
                            slCRNumbers.add(objCRName);
                        }
                        int iCRSize = slCRNumbers.size();
                        StringBuilder sbCRnames = new StringBuilder();
                        for (int k = 0; k < iCRSize; k++) {
                            sbCRnames.append(slCRNumbers.get(k));
                            sbCRnames.append(TigerConstants.SEPERATOR_COMMA);
                        }
                        strCRName = sbCRnames.toString();

                        strCOName = (String) mapCOInfo.get(DomainConstants.SELECT_NAME);
                        strCODesc = StringEscapeUtils.escapeXml10((String) mapCOInfo.get(DomainConstants.SELECT_DESCRIPTION));
                        strCODesc = TigerUtils.replaceWordCharsForXML(context, strCODesc);
                        slCOList.add(strCOName);

                        strChangeDescr.append(strCODesc);
                        // if (UIUtil.isNotNullAndNotEmpty(strReasonForChnage)) {
                        // strChangeDescr.append(STRING_SEPERATOR_COMMATAB+ strReasonForChnage);
                        // }
                    }
                    strXML.append("\n\t<ECOECRNUMBER>");
                    strXML.append(strCOName + "," + strCRName);
                    strXML.append("</ECOECRNUMBER>");
                    strXML.append("\n\t<DATEAUTHOROFCHANGE>");

                    StringBuilder sbCOCRNames = new StringBuilder(strCOName).append(STRING_BR).append(strCRName.replace(TigerConstants.SEPERATOR_COMMA, STRING_BR));

                    mapHistory.put(STRING_NUMBER, sbCOCRNames.toString());

                    if (UIUtil.isNotNullAndNotEmpty(sbLastPromoteDate.toString()) && UIUtil.isNotNullAndNotEmpty(sbChangeAuthor.toString())) {
                        SimpleDateFormat currentDateFormat = new SimpleDateFormat(STRING_DATEFORMAT);

                        SimpleDateFormat requiredDateFormat = new SimpleDateFormat(STRING_DATEFORMATDMMYY);
                        Date date = currentDateFormat.parse(sbLastPromoteDate.toString());

                        strXML.append(requiredDateFormat.format(date));
                        strXML.append(TigerConstants.SEPERATOR_COMMA);

                        strChangeAuthorValue = sbChangeAuthor.toString();

                        if (strChangeAuthorValue.length() > 9)
                            strChangeAuthorValue = strChangeAuthorValue.substring(0, 9);

                        strXML.append(strChangeAuthorValue);
                        mapHistory.put(STRING_DATEAUTHOR, new StringBuilder(64).append(requiredDateFormat.format(date)).append(STRING_BR).append(strChangeAuthorValue).toString());

                    } else {
                        strXML.append(" ");
                        mapHistory.put(STRING_DATEAUTHOR, " ");
                    }
                    strXML.append("</DATEAUTHOROFCHANGE>");

                    // TIGTK-14264 : Prakash : START
                    // if CO not empty then get all Part Name linked to the CO and with a relationship to the selected CAD Drawing (Specification or Charted drawing)
                    MapList mlPartsWithReasonForChange = UIUtil.isNotNullAndNotEmpty(COID) ? partLinkedToCOAndDrawing(context, COID, domCatRev) : new MapList();
                    StringList slAffectedIds = getStringListFromMapList(mlPartsWithReasonForChange, DomainConstants.SELECT_ID);
                    StringBuilder sbAllAffectedItems = new StringBuilder(16);
                    StringBuilder sbChangeDescription = new StringBuilder(32);
                    int iSize = mlPartsWithReasonForChange.size();

                    boolean isLast = false;
                    if (ISLast.equalsIgnoreCase(TigerConstants.STRING_TRUE) && !strStatus.equalsIgnoreCase(TigerConstants.STATE_RELEASED_CAD_OBJECT)
                            && !strStatus.equalsIgnoreCase(TigerConstants.STATE_OBSOLETE_CAD_OBJECT)) {
                        strChangeDescrValue = STRING_MSGLASTREV;
                        isLast = true;
                    }

                    for (int j = 0; j < iSize; j++) {
                        Hashtable mapConnectedPart = (Hashtable) mlPartsWithReasonForChange.get(j);
                        String slConnectedPartName = (String) mapConnectedPart.get(DomainConstants.SELECT_NAME);
                        String slConnectedPartID = (String) mapConnectedPart.get(DomainConstants.SELECT_ID);
                        String slConnectedREVISIONIDS = (String) mapConnectedPart.get(DomainConstants.SELECT_REVISION);
                        String strReasonForChangeDescription = (String) mapConnectedPart
                                .get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].attribute[" + DomainConstants.ATTRIBUTE_REASON_FOR_CHANGE + "].value");
                        if (UIUtil.isNullOrEmpty(strReasonForChangeDescription))
                            strReasonForChangeDescription = (String) mapConnectedPart
                                    .get("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].attribute[" + DomainConstants.ATTRIBUTE_REASON_FOR_CHANGE + "].value");
                        String strAffectedItems = new StringBuilder(64).append(slConnectedPartName).append(TigerConstants.SEPERATOR_MINUS).append(slConnectedREVISIONIDS).toString();

                        String strSymName = (String) mapConnectedPart
                                .get(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.").append(DomainConstants.SELECT_NAME).toString());
                        String strSymRev = (String) mapConnectedPart
                                .get(new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.").append(DomainConstants.SELECT_REVISION).toString());
                        String strSymId = (String) mapConnectedPart
                                .get(new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.").append(DomainConstants.SELECT_ID).toString());

                        if (UIUtil.isNotNullAndNotEmpty(strSymId) && slAffectedIds.contains(strSymId)) {
                            String strSymPartDesc = EnoviaResourceBundle.getProperty(context, "emxIEFDesignCenterStringResource", context.getLocale(), "FCM.symPart.ReasonForChangeDescription");
                            strReasonForChangeDescription = new StringBuilder(strSymPartDesc).append(strSymName).append(" + ").append(strSymRev).toString();
                        }

                        int iAffectedItemsLineCount = 0;
                        int iReasonForChangeDescCount = 0;
                        if (UIUtil.isNullOrEmpty(strAffectedItems)) {
                            sbAllAffectedItems.append(DomainConstants.EMPTY_STRING);
                        } else {
                            iAffectedItemsLineCount = 1;
                            if (j == 0) {
                                sbAllAffectedItems.append(strAffectedItems);
                            } else {
                                if (!sbAllAffectedItems.toString().endsWith(String_LTBRGT))
                                    sbAllAffectedItems.append(String_LTBRGT);
                                sbAllAffectedItems.append(strAffectedItems);
                            }
                        }

                        boolean bFlag = false;

                        if (UIUtil.isNullOrEmpty(strReasonForChangeDescription)) {
                            String strDesc = strChangeDescr.toString();
                            strReasonForChangeDescription = strDesc;
                            if (UIUtil.isNullOrEmpty(strDesc)) {
                                sbChangeDescription.append(DomainConstants.EMPTY_STRING);
                                bFlag = true;
                            }
                        }
                        if (!bFlag) {
                            strReasonForChangeDescription = TigerUtils.replaceWordCharsForXML(context, strReasonForChangeDescription);
                            // Change for RFC-153 :: no. of chanracter per line is 82. (Earlier it's 77)
                            if (strReasonForChangeDescription.length() > 82) {
                                strReasonForChangeDescription = TigerUtils.replaceAndWordWrap(context, strReasonForChangeDescription, 82);
                            }

                            String[] lines = strReasonForChangeDescription.split(String_LTBRGT);
                            iReasonForChangeDescCount = lines.length;
                            if (strReasonForChangeDescription.endsWith(String_LTBRGT))
                                iReasonForChangeDescCount--;

                            if (j == 0) {
                                sbChangeDescription.append(strReasonForChangeDescription);
                            } else {
                                if (!sbChangeDescription.toString().endsWith(String_LTBRGT))
                                    sbChangeDescription.append(String_LTBRGT);
                                sbChangeDescription.append(strReasonForChangeDescription);
                            }
                        }

                        int idiff = 0;
                        if (iReasonForChangeDescCount != iAffectedItemsLineCount && !isLast) {
                            if (iReasonForChangeDescCount > iAffectedItemsLineCount) {
                                idiff = iReasonForChangeDescCount - iAffectedItemsLineCount;
                                for (int k = 0; k < idiff; k++) {
                                    sbAllAffectedItems.append(STRING_LTBRGT_SPACE);
                                }

                            }
                        }

                    }

                    strChangeDescrValue = sbChangeDescription.toString();

                    if (ISLast.equalsIgnoreCase(TigerConstants.STRING_TRUE) && !strStatus.equalsIgnoreCase(TigerConstants.STATE_RELEASED_CAD_OBJECT)
                            && !strStatus.equalsIgnoreCase(TigerConstants.STATE_OBSOLETE_CAD_OBJECT)) {
                        strChangeDescrValue = STRING_MSGLASTREV;
                    }

                    strXML.append(new StringBuilder(64).append("<ITEMTOCHANGE>").append(sbAllAffectedItems.toString()).append("</ITEMTOCHANGE>").toString());
                    strXML.append(new StringBuilder(64).append("\n\t<CHANGEDESCRIPTION>").append(XSSUtil.encodeForXML(context, strChangeDescrValue)).append("</CHANGEDESCRIPTION>").toString());
                    mapHistory.put("AFFECTEDITEMS", TigerUtils.replaceWordCharsForPDF(context, sbAllAffectedItems.toString()));
                    mapHistory.put("DESCRIPTION", TigerUtils.replaceWordCharsForPDF(context, strChangeDescrValue));
                    // TIGTK-14264 : Prakash : END
                    strXML.append("\n</HISTORY>");

                    String[] strArrayOfSrq = new String[seqSize];
                    int s = 0;
                    for (s = 0; s < seqSize; s++) {
                        strArrayOfSrq[s] = (String) mapHistory.get(slSequence.get(s));
                    }
                    listOfHistory.add(strArrayOfSrq);
                }
            }
            strXML.append(
                    "\n<HISTORY>\n\t<REVISION>REV</REVISION>\n\t<STATUS> CO STATUS </STATUS>\n\t<ECOECRNUMBER> CO NUMBER , CR NUMBER </ECOECRNUMBER>\n\t<DATEAUTHOROFCHANGE> DATE/AUTHOR OF CHANGE </DATEAUTHOROFCHANGE>");
            strXML.append("\n\t<ITEMTOCHANGE> AFFECTED ITEMS </ITEMTOCHANGE>\n\t<CHANGEDESCRIPTION>CHANGE DESCRIPTION </CHANGEDESCRIPTION>\n</HISTORY>");
            strXMLFINAL.append("\n<CADHISTORY>\t");
            strXMLFINAL.append(strXML);
            strXMLFINAL.append("\n</CADHISTORY>");

            mapHistoryBlock.put(XML, strXMLFINAL);
            mapHistoryBlock.put(List, listOfHistory);
        } catch (Exception e) {
            logger.error("Error in generateHistoryBlock: ", e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        return mapHistoryBlock;
    }

    public MapList partLinkedToCOAndDrawing(Context context, String COID, DomainObject domCatRev) throws Exception {
        MapList mlPartsWithReasonForChange = new MapList();
        DomainObject domCO = DomainObject.newInstance(context, COID);
        StringBuilder sbRel = new StringBuilder(ChangeConstants.RELATIONSHIP_CHANGE_ACTION);
        sbRel.append(TigerConstants.SEPERATOR_COMMA);
        sbRel.append(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);
        sbRel.append(TigerConstants.SEPERATOR_COMMA);
        sbRel.append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);

        StringBuilder sbPart = new StringBuilder(ChangeConstants.TYPE_CHANGE_ACTION);
        sbPart.append(TigerConstants.SEPERATOR_COMMA);
        sbPart.append(ChangeConstants.TYPE_PART);

        StringList slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_ID);
        slObjectSelect.add(DomainConstants.SELECT_TYPE);

        MapList partMapListLinkedToCO = domCO.getRelatedObjects(context, sbRel.toString(), sbPart.toString(), slObjectSelect, new StringList(DomainRelationship.SELECT_ID), false, true, (short) 0,
                null, null, 0);

        StringList conaffectedParts = new StringList();
        conaffectedParts.addAll(getStringListFromMapList(partMapListLinkedToCO, DomainConstants.SELECT_ID));

        Pattern patRel = new Pattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
        patRel.addPattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);

        // TIGTK-14264 : Prakash : START
        Pattern patPart = new Pattern(DomainConstants.TYPE_PART);
        slObjectSelect.add(DomainConstants.SELECT_ID);
        slObjectSelect.add(DomainConstants.SELECT_NAME);
        slObjectSelect.add(DomainConstants.SELECT_REVISION);
        slObjectSelect.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "|from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id==" + COID + "].attribute["
                + DomainConstants.ATTRIBUTE_REASON_FOR_CHANGE + "].value");
        slObjectSelect.add("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "|from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id==" + COID + "].attribute["
                + DomainConstants.ATTRIBUTE_REASON_FOR_CHANGE + "].value");
        slObjectSelect.add(new StringBuilder(64).append("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.").append(DomainConstants.SELECT_NAME).toString());
        slObjectSelect.add(new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.").append(DomainConstants.SELECT_REVISION).toString());
        slObjectSelect.add(new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART).append("].from.").append(DomainConstants.SELECT_ID).toString());

        MapList mlPArtObject = domCatRev.getRelatedObjects(context, patRel.getPattern(), patPart.getPattern(), slObjectSelect, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1,
                DomainConstants.EMPTY_STRING, null, 0);
        int iLinkedPartCount = mlPArtObject.size();

        for (int i = 0; i < iLinkedPartCount; i++) {
            Map mapConnectedPart = (Map) mlPArtObject.get(i);
            String strConnectedPartId = (String) mapConnectedPart.get(DomainConstants.SELECT_ID);
            if (conaffectedParts.contains(strConnectedPartId)) {
                mlPartsWithReasonForChange.add(mapConnectedPart);
            }
        }
        // TIGTK-14264 : Prakash : END
        return mlPartsWithReasonForChange;
    }

    /**
     * This method used to replace Word Chars And Encode For XML
     * @param context
     * @param strConvertedValue
     * @throws Exception
     * @author PSI
     */
    private String purposeOfRelease(Context context, Map mpPart, String strPolicy, String strPattern) {
        String strPurposeOfRelease = DomainConstants.EMPTY_STRING;
        Object objPurposeOfRelease = DomainConstants.EMPTY_STRING;
        if (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
            objPurposeOfRelease = mpPart.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSEOFRELEASE_DEVPART + "]");
            if (objPurposeOfRelease instanceof StringList) {
                strPurposeOfRelease = (String) ((StringList) objPurposeOfRelease).get(0);
            } else {
                strPurposeOfRelease = (String) objPurposeOfRelease;
            }
            if (UIUtil.isNotNullAndNotEmpty(strPurposeOfRelease))
                strPurposeOfRelease = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(),
                        "emxFramework.Range.PSS_PurposeOfRelease_DEVPart." + strPurposeOfRelease);
        } else {
            objPurposeOfRelease = mpPart.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.attribute["
                    + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "].value");
            if (objPurposeOfRelease instanceof StringList) {
                strPurposeOfRelease = (String) ((StringList) objPurposeOfRelease).get(0);
            } else {
                strPurposeOfRelease = (String) objPurposeOfRelease;
            }
            if (!UIUtil.isNotNullAndNotEmpty(strPurposeOfRelease)) {
                objPurposeOfRelease = mpPart.get("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.attribute["
                        + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "].value");
                if (objPurposeOfRelease instanceof StringList) {
                    strPurposeOfRelease = (String) ((StringList) objPurposeOfRelease).get(0);
                } else {
                    strPurposeOfRelease = (String) objPurposeOfRelease;
                }
            }
        }
        if (UIUtil.isNotNullAndNotEmpty(strPurposeOfRelease)) {
            strPurposeOfRelease = strPurposeOfRelease.replaceAll(" ", DomainConstants.EMPTY_STRING).replaceAll("/", DomainConstants.EMPTY_STRING);
            strPurposeOfRelease = EnoviaResourceBundle.getProperty(context, "emxIEFDesignCenterStringResource", context.getLocale(),
                    new StringBuilder(64).append(strPattern).append(".purposeOfRelease.").append(strPurposeOfRelease).toString());

        } else {
            strPurposeOfRelease = DomainConstants.EMPTY_STRING;
        }
        return strPurposeOfRelease;
    }

    /**
     * This method will get Connected Part
     * @param context
     * @param currentDrawingObject
     * @param slObjSel
     * @param slRelSel
     * @param recurseToLevel
     * @throws Exception
     * @author PSI
     */
    private MapList getConnectedPart(Context context, DomainObject currentDrawingObject, StringList slObjSel, StringList slRelSel, int recurseToLevel) throws Exception {
        MapList mpPartList = new MapList();
        try {
            Pattern patPartPerRel = new Pattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
            patPartPerRel.addPattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
            Pattern patPartPerObj = new Pattern(DomainConstants.TYPE_PART);
            ExpansionIterator expItPart = currentDrawingObject.getExpansionIterator(context, patPartPerRel.getPattern(), patPartPerObj.getPattern(), slObjSel, slRelSel, true, false, (short) 0, null,
                    null, (short) 1000, false, true, (short) 1000);
            mpPartList = FrameworkUtil.toMapList(expItPart, (short) recurseToLevel, null, null, null, null);
            expItPart.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mpPartList;
    }

    public StringList getStringListFromMapList(MapList paramMapList, String paramString) {
        int i = paramMapList.size();
        StringList localStringList = new StringList(i);
        for (int j = 0; j < i; j++) {
            Map localMap = (Map) paramMapList.get(j);
            String strType = (String) localMap.get(DomainConstants.SELECT_TYPE);
            if (ChangeConstants.TYPE_PART.equalsIgnoreCase(strType))
                localStringList.addAll(getListValue(localMap, paramString));
        }
        return localStringList;
    }

    public static StringList getListValue(Map paramMap, String paramString) {
        Object localObject = paramMap.get(paramString);
        return (localObject instanceof String) ? new StringList((String) localObject) : localObject == null ? new StringList(0) : (StringList) localObject;
    }
}