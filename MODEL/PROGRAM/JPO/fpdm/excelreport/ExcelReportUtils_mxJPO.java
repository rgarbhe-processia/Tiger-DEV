package fpdm.excelreport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.jdl.MatrixSession;
import com.matrixone.json.JSONArray;
import com.matrixone.json.JSONObject;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectProxy;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.db.JPO;
import matrix.db.JPOSupport;
import matrix.db.RelationshipType;
import matrix.util.MatrixException;
import matrix.util.SelectList;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class ExcelReportUtils_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("ExcelReportUtils_mxJPO");

    public static final String TYPE_FPDM_EXCEL_REPORT = "FPDM_ExcelReport";

    public static final String TYPE_FPDM_EXCEL_REPORT_PARAMETER = "FPDM_ExcelReportParameter";

    public static final String POLICY_FPDM_EXCEL_REPORT = "FPDM_ExcelReport";

    public static final String POLICY_FPDM_EXCEL_REPORT_PARAMETER = "FPDM_ExcelReportParameter";

    public static final String ATTRIBUTE_FPDM_EXCEL_REPORT_JPO = "FPDM_ExcelReportJPO";

    public static final String ATTRIBUTE_FPDM_EXCEL_REPORT_CONTEXT = "FPDM_ExcelReportContext";

    public static final String ATTRIBUTE_FPDM_EXCEL_REPORT_PARAMETER_TYPE = "FPDM_ExcelReportParameterType";

    public static final String ATTRIBUTE_FPDM_EXCEL_REPORT_PARAMETER_VALUES = "FPDM_ExcelReportParameterValues";

    public static final String ATTRIBUTE_FPDM_EXCEL_REPORT_PARAMETER_DEFAULT = "FPDM_ExcelReportParameterDefault";

    public static final String ATTRIBUTE_FPDM_EXCEL_REPORT_PARAMETER_MANDATORY = "FPDM_ExcelReportParameterMandatory";

    public static final String RELATIONSHIP_EXCEL_REPORT_TO_EXCEL_REPORT_PARAMETER = "FPDM_ExcelReportToFPDM_ExcelReportParameter";

    public static final String FORMAT_EXCEL_REPORT = "xlsx";

    public ExcelReportUtils_mxJPO() {
    }

    /**
     * return the translation of an object's state
     * @param context
     *            The Enovia Context
     * @param policy
     *            Policy of the Object
     * @param current
     *            Current state to search in translation
     * @param language
     * @return
     * @throws MatrixException
     */
    public static String getI18nState(Context context, String policy, String current, String language) throws MatrixException {
        String state = "";
        state = EnoviaResourceBundle.getStateI18NString(context, policy, current, language);

        if (state.contains(".")) {
            return current;
        }

        return state;
    }

    /**
     * return the translation of a type
     * @param context
     *            The Enovia Context
     * @param type
     *            Type to search in translation
     * @param language
     * @return
     * @throws MatrixException
     */
    public static String getI18nType(Context context, String type, String language) throws MatrixException {
        String returnType = "";
        returnType = EnoviaResourceBundle.getTypeI18NString(context, type, language);

        if (returnType.contains(".")) {
            return type;
        }

        if ("".equals(returnType)) {
            return type;
        }

        return returnType;
    }

    public static String getI18nPolicy(Context context, String policy, String language) throws MatrixException {
        String returnPolicy = "";
        returnPolicy = EnoviaResourceBundle.getAdminI18NString(context, "Policy", policy, language);

        if (returnPolicy.contains(".")) {
            return policy;
        }

        if ("".equals(returnPolicy)) {
            return policy;
        }

        return returnPolicy;
    }

    public static String stringListOrStringToString(Object object, String separator) {
        if (null != object) {
            if ("java.lang.String".equals(object.getClass().getName())) {
                return (String) object;
            } else if ("matrix.util.StringList".equals(object.getClass().getName())) {
                StringList slResults = (StringList) object;
                Iterator<String> itResult = slResults.iterator();
                StringBuffer sbResult = new StringBuffer();
                while (itResult.hasNext()) {
                    sbResult.append(itResult.next());
                    if (itResult.hasNext()) {
                        sbResult.append(separator);
                    }
                }
                return (sbResult.toString());
            }
        }
        return "";
    }

    public static List<? extends fpdm.excelreport.ConsolidatedReportPart_mxJPO> consolidateReport(List<? extends fpdm.excelreport.ConsolidatedReportPart_mxJPO> reportParts) {
        List<fpdm.excelreport.ConsolidatedReportPart_mxJPO> newReportParts = new ArrayList<fpdm.excelreport.ConsolidatedReportPart_mxJPO>();
        boolean isLastPresent = false;
        for (fpdm.excelreport.ConsolidatedReportPart_mxJPO rp : reportParts) {
            logger.debug("<ExcelReport> Current level : " + rp.getLevel());

            if (-1 == rp.level) {
                if (!isLastPresent) {
                    newReportParts.add(rp);
                }
            } else {
                boolean isPresent = rp.isPresent(newReportParts);
                logger.debug("<ExcelReport> " + isPresent);
                if (!isPresent) {
                    newReportParts.add(rp);
                    isLastPresent = false;
                } else {
                    isLastPresent = true;
                }
            }
        }
        return newReportParts;
    }

    /**
     * Check if report contain elements that cannot be filled
     * @param context
     *            an eMatrix Context object
     * @param args
     *            args[0] contain the id of a report
     * @return 0 if success
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public int getMissingElements(Context context, String[] args) throws Exception {
        /*
         * ArrayList<String> alMissingData = new ArrayList<String>(); String sExcelReportId = args[0]; String strTempData = DomainObject.EMPTY_STRING;
         * 
         * if (UIUtil.isNotNullAndNotEmpty(sExcelReportId)) { HashSet<String> hsReportData = ExcelReportUtils_mxJPO.getReportElements(context, sExcelReportId); HashSet<String> hsClassData =
         * ExcelReportUtils_mxJPO.getClassElements(context, sExcelReportId);
         * 
         * Iterator iterator = hsReportData.iterator(); while (iterator.hasNext()) { strTempData = (String) iterator.next(); if (!hsClassData.contains(strTempData)) { alMissingData.add(strTempData); }
         * }
         * 
         * if (!alMissingData.isEmpty()) { MqlUtil.mqlCommand(context, "notice $1", "The attributes " + alMissingData + " cannot be filled."); return 1; } }
         */

        return 0;
    }

    @SuppressWarnings("resource")
    private static HashSet<String> getReportElements(Context context, String strObjectId) throws Exception {
        HashSet<String> hsReportFields = new HashSet<String>();
        FileList listOfFiles = new FileList();

        Path temp = Files.createTempDirectory("TMPEXCEL_");
        BusinessObject boReportSrc = new BusinessObject(strObjectId);
        listOfFiles = boReportSrc.getFiles(context);
        boReportSrc.checkoutFiles(context, false, "xlsx", listOfFiles, temp.toAbsolutePath().toString());

        try (InputStream is = new FileInputStream(temp.toAbsolutePath().toString() + File.separatorChar + listOfFiles.get(0).toString())) {
            XSSFWorkbook wb = new XSSFWorkbook(is);
            XSSFSheet sheet = wb.getSheetAt(0);
            hsReportFields = ExcelReportUtils_mxJPO.findReportData(sheet);
            FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpdir") + File.separator + temp.getFileName() + File.separator));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return hsReportFields;
    }

    @SuppressWarnings("rawtypes")
    private static HashSet<String> getClassElements(Context context, String strObjectId) throws Exception {
        HashSet<String> hsClassFields = new HashSet<String>();
        DomainObject doReport = DomainObject.newInstance(context, strObjectId);
        String strFullClassName = JPOSupport.getClassName(context,
                "fpdm.excelreport.queries." + doReport.getAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_FPDM_ExcelReportJPO")));
        Class thisClass = Class.forName(strFullClassName);

        for (Class cls : thisClass.getDeclaredClasses()) {
            if (!Modifier.isStatic(cls.getModifiers())) {
                Field[] fld = cls.getDeclaredFields();
                for (int i = 0; i < fld.length; i++) {
                    hsClassFields.add(fld[i].getName());
                }
            }

            Field[] fld = cls.getSuperclass().getDeclaredFields();
            for (int i = 0; i < fld.length; i++) {
                hsClassFields.add(fld[i].getName());
            }
        }

        hsClassFields.add("blank");

        return hsClassFields;
    }

    private static HashSet<String> findReportData(XSSFSheet sheet) {
        HashSet<String> result = new HashSet<String>();
        String strCellContent = "${";
        String strCellSplit = ".";
        String strCellValue = "";

        String regexAttribute = '\\' + "$\\{[^\\.]+\\.([^\\}]+)\\}";
        Pattern pattern = Pattern.compile(regexAttribute);

        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                    strCellValue = cell.getRichStringCellValue().getString();

                    if (strCellValue.contains(strCellContent) && strCellValue.contains(strCellSplit)) {
                        Matcher matcher = pattern.matcher(strCellValue);

                        while (matcher.find()) {
                            for (int i = 1; i <= matcher.groupCount(); i++) {
                                logger.debug("Groupe " + i + " : " + matcher.group(i));
                                result.add(matcher.group(i));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public static fpdm.excelreport.ImageReport_mxJPO getImageReport(Context context, String partID) throws Exception {
        fpdm.excelreport.ImageReport_mxJPO image = new fpdm.excelreport.ImageReport_mxJPO();

        SelectList selectStmtsIdName = new SelectList();
        selectStmtsIdName.add(DomainConstants.SELECT_ID);
        selectStmtsIdName.add(DomainConstants.SELECT_NAME);
        selectStmtsIdName.add(DomainConstants.SELECT_TYPE);

        DomainObject doPart = new DomainObject(partID);
        MapList partSpecList = FrameworkUtil.toMapList(doPart.getExpansionIterator(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship pattern
                "*", // type pattern
                selectStmtsIdName, // list of select statement pertaining to Business Objects
                new SelectList(), // list of select statement pertaining to Relationships
                false, // get To relationships
                true, // get From relationships
                (short) 1, // the number of levels to expand, 0 equals expand all
                null, // where clause to apply to objects, can be empty
                null, // where clause to apply to relationship, can be empty
                (short) 0, // the maximum number of objects to return
                false, // true to check for hidden types per MX_SHOW_HIDDEN_TYPE_OBJECTS setting; false to return all objects, even if hidden
                false, // true to return each target object only once in expansion
                (short) 1, // page size to use for streaming data source
                false // boolean true to force HashTable data to StringList; false will return String for single-valued selects, StringList for multi-valued selects
        ), (short) 0, null, null, null, null);

        @SuppressWarnings("unchecked")
        Iterator<Hashtable<String, String>> itPartSpec = (Iterator<Hashtable<String, String>>) partSpecList.iterator();

        String SELECT_FILE_NAME = "format[PNG].file.name";
        SelectList selectStmtsImage = new SelectList();
        selectStmtsImage.add(DomainConstants.SELECT_ID);
        selectStmtsImage.add(DomainConstants.SELECT_NAME);
        selectStmtsImage.add(DomainConstants.SELECT_TYPE);
        selectStmtsImage.add(SELECT_FILE_NAME);

        while (itPartSpec.hasNext()) {
            Hashtable<String, String> partSpec = itPartSpec.next();
            String partSpecId = partSpec.get(DomainObject.SELECT_ID);
            logger.debug("Part Spec : " + partSpecId);

            DomainObject doPartSpec = new DomainObject(partSpecId);
            MapList viewableList = FrameworkUtil.toMapList(doPartSpec.getExpansionIterator(context, TigerConstants.RELATIONSHIP_VIEWABLE, // relationship pattern
                    "ThumbnailViewable", // type pattern
                    selectStmtsImage, // list of select statement pertaining to Business Objects
                    new SelectList(), // list of select statement pertaining to Relationships
                    false, // get To relationships
                    true, // get From relationships
                    (short) 1, // the number of levels to expand, 0 equals expand all
                    null, // where clause to apply to objects, can be empty
                    null, // where clause to apply to relationship, can be empty
                    (short) 0, // the maximum number of objects to return
                    false, // true to check for hidden types per MX_SHOW_HIDDEN_TYPE_OBJECTS setting; false to return all objects, even if hidden
                    false, // true to return each target object only once in expansion
                    (short) 1, // page size to use for streaming data source
                    false // boolean true to force HashTable data to StringList; false will return String for single-valued selects, StringList for multi-valued selects
            ), (short) 0, null, null, null, null);
            Iterator<Hashtable<String, String>> itViewable = (Iterator<Hashtable<String, String>>) viewableList.iterator();

            while (itViewable.hasNext()) {
                logger.debug("=== image for " + partID);
                Hashtable<String, String> viewable = itViewable.next();

                String sFilename = viewable.get(SELECT_FILE_NAME);
                String sPNGFileID = viewable.get(DomainConstants.SELECT_ID);

                image.format = "PNG";
                image.fileName = sFilename;
                image.id = sPNGFileID;

                if ((null != sFilename) && (null != sPNGFileID)) {
                    return image;
                }
            }
        }
        return new fpdm.excelreport.ImageReport_mxJPO();
    }

    public static List<Map<String, String>> getRouteTasks(Context context, String objectID) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();

        logger.debug("Route : " + objectID);

        String SELECT_TITLE = "attribute[" + DomainConstants.ATTRIBUTE_TITLE + "].value";
        String SELECT_ROUTE_ACTION = "attribute[" + DomainConstants.ATTRIBUTE_ROUTE_ACTION + "].value";
        String SELECT_COMMENTS = "attribute[" + DomainConstants.ATTRIBUTE_COMMENTS + "].value";
        String SELECT_APPROVAL_STATUS = "attribute[" + DomainConstants.ATTRIBUTE_APPROVAL_STATUS + "].value";
        String SELECT_SCHEDULED_COMPLETION_DATE = "attribute[" + DomainConstants.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "].value";
        String SELECT_ROUTESEQUENCE = "attribute[" + DomainObject.ATTRIBUTE_ROUTE_SEQUENCE + "].value";
        String SELECT_ROUTE_NODE_ID = DomainObject.getAttributeSelect(DomainObject.ATTRIBUTE_ROUTE_NODE_ID);
        String SELECT_COMPLETED_DATE = "attribute[" + DomainObject.ATTRIBUTE_ACTUAL_COMPLETION_DATE + "].value";

        SelectList SELECT_TASKS = new SelectList();
        SELECT_TASKS.add(DomainConstants.SELECT_ID);
        SELECT_TASKS.add(DomainConstants.SELECT_NAME);
        SELECT_TASKS.add(SELECT_TITLE);
        SELECT_TASKS.add(DomainConstants.SELECT_REVISION);
        SELECT_TASKS.add(DomainConstants.SELECT_CURRENT);
        SELECT_TASKS.add(DomainConstants.SELECT_POLICY);
        SELECT_TASKS.add(SELECT_ROUTE_ACTION);
        SELECT_TASKS.add(SELECT_COMMENTS);
        SELECT_TASKS.add(SELECT_APPROVAL_STATUS);
        SELECT_TASKS.add(SELECT_SCHEDULED_COMPLETION_DATE);
        SELECT_TASKS.add(DomainConstants.SELECT_OWNER);
        SELECT_TASKS.add(SELECT_ROUTESEQUENCE);
        SELECT_TASKS.add(SELECT_ROUTE_NODE_ID);
        SELECT_TASKS.add(SELECT_COMPLETED_DATE);

        SelectList SELECT_NODES = new SelectList();
        SELECT_NODES.add(DomainConstants.SELECT_ID);
        SELECT_NODES.add(DomainConstants.SELECT_NAME);
        SELECT_NODES.add(DomainConstants.SELECT_CURRENT);
        SELECT_NODES.add(DomainConstants.SELECT_POLICY);

        SelectList SELECT_NODES_RELATIONS = new SelectList();
        SELECT_NODES_RELATIONS.add(SELECT_ROUTE_NODE_ID);
        SELECT_NODES_RELATIONS.add(SELECT_ROUTESEQUENCE);
        SELECT_NODES_RELATIONS.add(SELECT_SCHEDULED_COMPLETION_DATE);
        SELECT_NODES_RELATIONS.add(SELECT_APPROVAL_STATUS);
        SELECT_NODES_RELATIONS.add(SELECT_ROUTE_ACTION);
        SELECT_NODES_RELATIONS.add(SELECT_TITLE);
        SELECT_NODES_RELATIONS.add(SELECT_COMPLETED_DATE);

        BusinessObject boRoute = null;
        try {
            boRoute = new BusinessObject(objectID);
        } catch (MatrixException e) {
            e.printStackTrace();
        }

        MapList mlTasks = null;
        try {
            if (null != boRoute) {
                context.start(false);
                mlTasks = FrameworkUtil.toMapList(boRoute.getExpansionIterator(context, TigerConstants.RELATIONSHIP_ROUTE_TASK, // relationship pattern
                        "*", // type pattern
                        SELECT_TASKS, // list of select statement pertaining to Business Objects
                        new SelectList(), // list of select statement pertaining to Relationships
                        true, // get To relationships
                        false, // get From relationships
                        (short) 1, // the number of levels to expand, 0 equals expand all
                        null, // where clause to apply to objects, can be empty
                        null, // where clause to apply to relationship, can be empty
                        (short) 0, // the maximum number of objects to return
                        false, // true to check for hidden types per MX_SHOW_HIDDEN_TYPE_OBJECTS setting; false to return all objects, even if hidden
                        false, // true to return each target object only once in expansion
                        (short) 1, // page size to use for streaming data source
                        false // boolean true to force HashTable data to StringList; false will return String for single-valued selects, StringList for multi-valued selects
                ), (short) 0, null, null, null, null);
                context.abort();

                logger.debug("Tasks : " + mlTasks);
            }
        } catch (MatrixException e) {
            e.printStackTrace();
        }

        MapList mlNodes = null;
        try {
            if (null != boRoute) {
                context.start(false);
                mlNodes = FrameworkUtil.toMapList(boRoute.getExpansionIterator(context, DomainObject.RELATIONSHIP_ROUTE_NODE, // relationship pattern
                        "*", // type pattern
                        SELECT_NODES, // list of select statement pertaining to Business Objects
                        SELECT_NODES_RELATIONS, // list of select statement pertaining to Relationships
                        false, // get To relationships
                        true, // get From relationships
                        (short) 1, // the number of levels to expand, 0 equals expand all
                        null, // where clause to apply to objects, can be empty
                        null, // where clause to apply to relationship, can be empty
                        (short) 0, // the maximum number of objects to return
                        false, // true to check for hidden types per MX_SHOW_HIDDEN_TYPE_OBJECTS setting; false to return all objects, even if hidden
                        false, // true to return each target object only once in expansion
                        (short) 1, // page size to use for streaming data source
                        false // boolean true to force HashTable data to StringList; false will return String for single-valued selects, StringList for multi-valued selects
                ), (short) 0, null, null, null, null);
                context.abort();
            }
        } catch (MatrixException e) {
            e.printStackTrace();
        }

        Iterator<Hashtable<String, String>> itNode = mlNodes.iterator();
        while (itNode.hasNext()) {
            Hashtable<String, String> nodeInformations = itNode.next();
            String idRouteNode = nodeInformations.get(SELECT_ROUTE_NODE_ID);
            logger.debug("Route node ID : " + idRouteNode);

            boolean added = false;
            Iterator<Hashtable<String, String>> itTask = mlTasks.iterator();
            while (itTask.hasNext()) {
                Hashtable<String, String> taskInformations = itTask.next();
                String comparateRouteNode = taskInformations.get(SELECT_ROUTE_NODE_ID);
                if (idRouteNode.equals(comparateRouteNode)) {
                    taskInformations.put("taskName", taskInformations.get("name"));
                    taskInformations.put(SELECT_ROUTESEQUENCE, nodeInformations.get(SELECT_ROUTESEQUENCE));
                    taskInformations.put(DomainObject.SELECT_NAME, nodeInformations.get(DomainObject.SELECT_NAME));
                    if (nodeInformations.contains(SELECT_COMPLETED_DATE)) {
                        taskInformations.put(SELECT_COMPLETED_DATE, nodeInformations.get(SELECT_COMPLETED_DATE));
                    }
                    result.add(taskInformations);
                    added = true;
                    continue;
                }
            }

            if (!added) {
                nodeInformations.put("taskName", "Route Node");
                result.add(nodeInformations);
            }
        }

        return result;
    }

    public static HashMap<String, String> getDynamicParameter(Context context, String[] args) {
        HashMap<String, String> hmResult = new HashMap<>();
        HashMap<String, String> obj = null;
        try {
            obj = JPO.unpackArgs(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String response = fpdm.eai.utils.WebServiceManager_mxJPO.callEAIWS(context, "ExcelReportWebService", "getDynamicParameter", obj);
        hmResult = (HashMap<String, String>) Arrays.asList(response.split(Pattern.quote("|"))).stream().map(s -> s.split(",")).collect(Collectors.toMap(e -> e[0], e -> e[1]));
        return hmResult;
    }

    public static BusinessObjectProxy getReportToShow(Context context, String[] args) throws Exception {
        HashMap<String, String> programArgs = JPO.unpackArgs(args);
        String objectId = programArgs.get("objectId");

        DomainObject doInput = DomainObject.newInstance(context, objectId);

        if (!"FPDM_ExcelReportResult".equalsIgnoreCase(doInput.getType(context))) {
            logger.debug("Not a report... Searching for the last report");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a");

            StringList objectSelect = new StringList();
            objectSelect.add(DomainConstants.SELECT_ID);
            objectSelect.add(DomainConstants.SELECT_ORIGINATED);

            MapList mlSubObjects = doInput.getRelatedObjects(context, "FPDM_AllToFPDM_ExcelReportResult", "FPDM_ExcelReportResult", objectSelect, null, false, true, (short) 1, "", "", 0);

            String lastReportId = "";
            LocalDate ldLast = null;
            for (Iterator<Map<String, String>> ite = mlSubObjects.iterator(); ite.hasNext();) {
                Map<String, String> mInfos = (Map<String, String>) ite.next();
                String originated = mInfos.get(DomainConstants.SELECT_ORIGINATED);

                // convert String to LocalDate
                LocalDate localDate = LocalDate.parse(originated, formatter);
                if ((null == ldLast) || (localDate.isAfter(ldLast))) {
                    ldLast = localDate;
                    lastReportId = mInfos.get(DomainConstants.SELECT_ID);
                }
            }

            if (!lastReportId.isEmpty()) {
                doInput.setId(lastReportId);
            }
        }

        StringList selectList = new StringList(2);
        selectList.add("format.file.name");
        selectList.add("format");
        selectList.add("type");
        Map documentInfo = doInput.getInfo(context, selectList);

        BusinessObjectProxy bop = new BusinessObjectProxy((String) documentInfo.get("id"), (String) documentInfo.get("format"), (String) ((StringList) documentInfo.get("format.file.name")).get(0),
                false, false);

        return bop;
    }

    @com.matrixone.apps.framework.ui.PostProcessCallable
    public static void createNewDefaultExcelReport(Context context, String[] args) {
        try {
            Map<String, Object> mArgs = JPO.unpackArgs(args);

            String typeAliasParameter = FrameworkUtil.getAliasForAdmin(context, "type", TYPE_FPDM_EXCEL_REPORT_PARAMETER, true);
            String policyAliasParameter = FrameworkUtil.getAliasForAdmin(context, "policy", TYPE_FPDM_EXCEL_REPORT_PARAMETER, true);

            Map<String, String> requestMap = (Map<String, String>) mArgs.get("requestMap");
            Map<String, String> paramMap = (Map<String, String>) mArgs.get("paramMap");

            String programName = requestMap.get("FPDM_ExcelReportDataExtractionJPO");

            String sObjectId = paramMap.get("objectId");

            DomainObject dmoNewObject = DomainObject.newInstance(context, sObjectId);

            Map<String, String> mArgsToSend = new HashMap<>();
            mArgsToSend.put("webService", "ExcelReportWebService");
            mArgsToSend.put("sMethod", "createDefaultReport");
            mArgsToSend.put("reportName", programName);

            String[] argsToSend = JPO.packArgs(mArgsToSend);

            String result = fpdm.eai.utils.WebServiceManager_mxJPO.callEAIWS(context, argsToSend);

            JSONObject jso = new JSONObject(result);
            JSONArray description = (JSONArray) jso.get("description");
            dmoNewObject.setDescription(context, getValueFromJSONXLR(description));

            JSONArray organization = (JSONArray) jso.get("organization");
            dmoNewObject.setOrganizationOwner(context, getValueFromJSONXLR(organization));

            AttributeList alReport = new AttributeList();

            StringList slContexts = new StringList();
            JSONArray contexts = (JSONArray) jso.get("contexts");
            if (contexts.length() > 0) {
                for (int x = 0; x < contexts.length(); x++) {
                    slContexts.add(contexts.get(x));
                    Attribute attributeContext = new Attribute(new AttributeType("FPDM_ExcelReportContext"), slContexts);
                    alReport.add(attributeContext);
                }
            }

            Attribute attributeQuery = new Attribute(new AttributeType("FPDM_ExcelReportJPO"), programName);
            alReport.add(attributeQuery);

            dmoNewObject.setAttributes(context, alReport);

            JSONArray arrayTemplateName = (JSONArray) jso.get("templateName");
            String sTemplateName = getValueFromJSONXLR(arrayTemplateName);

            JSONArray arrayTemplate = (JSONArray) jso.get("template");
            String sTemplate = getValueFromJSONXLR(arrayTemplate);

            if (!"".equals(sTemplate)) {
                byte[] decodedImg = Base64.getDecoder().decode(sTemplate.getBytes(StandardCharsets.UTF_8));
                Path destinationFile = Paths.get(System.getProperty("java.io.tmpdir"), sTemplateName);
                Files.write(destinationFile, decodedImg);

                dmoNewObject.checkinFile(context, true, false, "", "xlsx", sTemplateName, destinationFile.getParent().toAbsolutePath().toString());
            }

            if (jso.contains("parameters")) {
                JSONArray arrayParameters = (JSONArray) jso.get("parameters");
                if (arrayParameters.length() > 0) {
                    for (int x = 0; x < arrayParameters.length(); x++) {
                        JSONObject jsoParameter = (JSONObject) arrayParameters.get(x);
                        String defaultValue = getValueFromJSONXLR((JSONArray) jsoParameter.get("defaultValue"));
                        String values = getValueFromJSONXLR((JSONArray) jsoParameter.get("values"));
                        String type = getValueFromJSONXLR((JSONArray) jsoParameter.get("type"));
                        String mandatory = getValueFromJSONXLR((JSONArray) jsoParameter.get("mandatory"));
                        String descriptionParameter = getValueFromJSONXLR((JSONArray) jsoParameter.get("description"));

                        String sExcelReportParameterID = "";
                        try {
                            sExcelReportParameterID = FrameworkUtil.autoName(context, typeAliasParameter, policyAliasParameter);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        DomainObject excelReportParameterObject = new DomainObject(sExcelReportParameterID);
                        excelReportParameterObject.open(context);
                        excelReportParameterObject.setDescription(context, descriptionParameter);
                        excelReportParameterObject.setAttributeValue(context, ATTRIBUTE_FPDM_EXCEL_REPORT_PARAMETER_TYPE, type);
                        excelReportParameterObject.setAttributeValue(context, ATTRIBUTE_FPDM_EXCEL_REPORT_PARAMETER_DEFAULT, defaultValue);
                        excelReportParameterObject.setAttributeValue(context, ATTRIBUTE_FPDM_EXCEL_REPORT_PARAMETER_VALUES, values);
                        excelReportParameterObject.setAttributeValue(context, ATTRIBUTE_FPDM_EXCEL_REPORT_PARAMETER_MANDATORY, mandatory);

                        excelReportParameterObject.setOrganizationOwner(context, getValueFromJSONXLR(organization));
                        excelReportParameterObject.setProjectOwner(context, "common_public");

                        RelationshipType rtRelation = new RelationshipType(RELATIONSHIP_EXCEL_REPORT_TO_EXCEL_REPORT_PARAMETER);
                        new DomainRelationship(excelReportParameterObject.connect(context, rtRelation, false, dmoNewObject));

                        dmoNewObject.update(context);
                        excelReportParameterObject.close(context);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error creating Excel report : {}", e);
        }
    }

    private static String getValueFromJSONXLR(JSONArray values) throws MatrixException {
        if (values.length() > 0) {
            return (String) values.get(0);
        }

        return "";
    }

    public static Map<String, String> getResult(Context context, String[] args) throws Exception {
        Map<String, String> result = new HashMap<>();

        Map<String, String> mArgs = JPO.unpackArgs(args);

        MatrixSession mxs = context.getSession();

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(bo);
        so.writeObject(mxs);
        so.flush();
        String sSession = javax.xml.bind.DatatypeConverter.printBase64Binary(bo.toByteArray());

        Map<String, String> programMap = new HashMap<>();

        programMap.put("webService", "ExcelReportWebService");
        programMap.put("sMethod", "runExcelReports");
        programMap.put("sSession", sSession);
        programMap.put("returnType", "map");
        programMap.putAll(mArgs);

        String sResult = fpdm.eai.utils.WebServiceManager_mxJPO.callEAIWS(context, "ExcelReportWebService", "runExcelReports", programMap);

        if (sResult.startsWith("MESSAGE")) {
            result.put("message", sResult.substring(10));
        } else {
            JSONObject jsnobject = new JSONObject(sResult);

            Iterator<String> itJson = jsnobject.keys();
            while (itJson.hasNext()) {
                String key = itJson.next();
                result.put(key, jsnobject.getString(key));
            }
        }

        return result;
    }
}
