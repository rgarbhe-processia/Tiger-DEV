
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.mxBus;
import com.matrixone.apps.framework.ui.UITableIndented;
import com.matrixone.jdom.Element;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import matrix.db.RelationshipWithSelect;
import matrix.util.StringList;

public class FPDM_ExcelReport_mxJPO extends emxCommonDocumentBase_mxJPO {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    static String TYPE_EXCEL_REPORT = "";

    static String POLICY_EXCEL_REPORT = "";

    static String TYPE_EXCEL_REPORT_PARAMETER = "";

    static String POLICY_EXCEL_REPORT_PARAMETER = "";

    static String ATTRIBUTE_EXCEL_REPORT_CONTEXT = "";

    static String ATTRIBUTE_EXCEL_REPORT_PARAMETER_DEFAULT = "";

    static String ATTRIBUTE_EXCEL_REPORT_PARAMETER_TYPE = "";

    static String ATTRIBUTE_EXCEL_REPORT_PARAMETER_MANDATORY = "";

    static String ATTRIBUTE_EXCEL_REPORT_PARAMETER_VALUES = "";

    static String RELATIONSHIP_EXCEL_REPORT_TO_EXCEL_REPORT_PARAMETER = "";

    static String ROLE_GLOBAL_ADMINISTRATOR = "";

    private static final String MARKUP_NEW = "new";

    /**
     * Constructor - call the super constructor and set the treatmentQueue to the name given in the property file
     * @param context
     *            an eMatrix Context object
     * @param args
     *            given by the application
     * @throws Exception
     */
    public FPDM_ExcelReport_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
        this.initProperties(context, args);
    }

    public void initProperties(Context context, String[] args) {
        FPDM_ExcelReport_mxJPO.TYPE_EXCEL_REPORT = PropertyUtil.getSchemaProperty(context, "type_FPDM_ExcelReport");
        FPDM_ExcelReport_mxJPO.POLICY_EXCEL_REPORT = PropertyUtil.getSchemaProperty(context, "policy_FPDM_ExcelReport");

        FPDM_ExcelReport_mxJPO.TYPE_EXCEL_REPORT_PARAMETER = PropertyUtil.getSchemaProperty(context, "type_FPDM_ExcelReportParameter");
        FPDM_ExcelReport_mxJPO.POLICY_EXCEL_REPORT_PARAMETER = PropertyUtil.getSchemaProperty(context, "policy_FPDM_ExcelReportParameter");

        FPDM_ExcelReport_mxJPO.ATTRIBUTE_EXCEL_REPORT_CONTEXT = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_FPDM_ExcelReportContext") + "].value";
        FPDM_ExcelReport_mxJPO.ATTRIBUTE_EXCEL_REPORT_PARAMETER_DEFAULT = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_FPDM_ExcelReportParameterDefault") + "]";
        FPDM_ExcelReport_mxJPO.ATTRIBUTE_EXCEL_REPORT_PARAMETER_TYPE = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_FPDM_ExcelReportParameterType") + "]";
        FPDM_ExcelReport_mxJPO.ATTRIBUTE_EXCEL_REPORT_PARAMETER_MANDATORY = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_FPDM_ExcelReportParameterMandatory") + "]";
        FPDM_ExcelReport_mxJPO.ATTRIBUTE_EXCEL_REPORT_PARAMETER_VALUES = "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_FPDM_ExcelReportParameterValues") + "]";

        FPDM_ExcelReport_mxJPO.RELATIONSHIP_EXCEL_REPORT_TO_EXCEL_REPORT_PARAMETER = PropertyUtil.getSchemaProperty(context, "relationship_FPDM_ExcelReportToFPDM_ExcelReportParameter");

        FPDM_ExcelReport_mxJPO.ROLE_GLOBAL_ADMINISTRATOR = PropertyUtil.getSchemaProperty(context, "role_PSS_Global_Administrator");
    }

    /**
     *
     */
    public Map preCheckin(Context context, HashMap uploadParamsMap, String objectId) throws Exception {
        Map preCheckinMap = new HashMap();

        String objectIdParam = (String) uploadParamsMap.get("objectId");

        BusinessObject boExcelReport = new BusinessObject(objectIdParam);
        boExcelReport.lock(context);
        boExcelReport.close(context);
        deleteBeforeCheckin(context, new String[] { objectIdParam });

        return preCheckinMap;
    }

    /**
     * Delete all files associated to a given Document
     * @param context
     *            an eMatrix Context object
     * @param args
     *            args[0] contain the id of a Document
     * @return 0 if success
     * @throws Exception
     */
    public int deleteBeforeCheckin(Context context, String[] args) throws Exception {
        com.matrixone.apps.common.CommonDocument cd = new com.matrixone.apps.common.CommonDocument(args[0]);
        MapList filesToDelete = cd.getAllFormatFiles(context);

        Iterator<HashMap> itDeletion = filesToDelete.iterator();
        while (itDeletion.hasNext()) {
            HashMap oneToDelete = itDeletion.next();
            cd.deleteFile(context, (String) oneToDelete.get("filename"), (String) oneToDelete.get("format"));
        }

        return 0;
    }

    /**
     * @param context
     *            an eMatrix Context object
     * @param args
     * @return
     * @throws Exception
     */
    public int lockObjectBeforeCheckin(Context context, String[] args) throws Exception {
        return 0;
    }

    /**
     * Delete file associated to the FPDM_ExcelReport object
     * @param context
     *            an eMatrix Context object
     * @param args
     *            args[0] contain the id of the FPDM_ExcelReport Business Object
     * @return
     * @throws Exception
     */
    public MapList deleteFile(Context context, String[] args) throws Exception {
        DomainObject doFiles = new DomainObject(args[0]);
        com.matrixone.apps.common.CommonDocument cd = new com.matrixone.apps.common.CommonDocument(args[0]);
        MapList filesToDelete = cd.getFiles(context, null, null, null, null);
        return new MapList();
    }

    /**
     * Get a list of all Excel Reports
     * @param context
     *            an eMatrix Context object
     * @param args
     *            send by the application
     * @return
     * @throws Exception
     */
    public MapList getExcelReports(Context context, String[] args) throws Exception {
        HashMap requestMap = (HashMap) JPO.unpackArgs(args);

        try {
            // Add the selectables
            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_ID);

            DomainObject dObj = new DomainObject();

            // Call Find objects method to get the id's with select list and where clause
            MapList excelReports = dObj.findObjects(context, TYPE_EXCEL_REPORT, "*", "*", "*", "*", null, true, objSelects);

            return excelReports;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Get a list of all report for a given page
     * @param context
     *            an eMatrix Context object
     * @param args
     *            args[0] must contain the name of the page from where we are
     * @return a MapList containing all report for the context given in parameter
     * @throws Exception
     */
    public MapList getAllReportsForContext(Context context, String[] args) throws Exception {
        String fromPage = args[0];

        try {
            String sStates = PropertyUtil.getSchemaProperty(context, "policy", POLICY_EXCEL_REPORT, "state_Released");
            if (context.isAssigned(ROLE_GLOBAL_ADMINISTRATOR)) {
                sStates = PropertyUtil.getSchemaProperty(context, "policy", POLICY_EXCEL_REPORT, "state_Review") + "," + sStates;
            }

            StringList objSelects = new StringList();
            objSelects.addElement(DomainConstants.SELECT_ID);
            objSelects.addElement(DomainConstants.SELECT_DESCRIPTION);
            objSelects.addElement(DomainConstants.SELECT_REVISION);

            String sWhere = ATTRIBUTE_EXCEL_REPORT_CONTEXT + "=='" + fromPage + "'";
            sWhere += " and current matchlist '" + sStates + "' ','";

            MapList mlExcelReportsForContext = DomainObject.findObjects(context, TYPE_EXCEL_REPORT, "*", "*", "*", "*", sWhere, true, objSelects);

            mlExcelReportsForContext.addSortKey("description", "ascending", "String");
            mlExcelReportsForContext.sort();

            return mlExcelReportsForContext;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * @param context
     *            Enovia {@link Context}
     * @param args
     *            List containing an objectId
     * @return list of all reports generated from a given context (from an objectId)
     * @throws Exception
     */
    public MapList getExcelReportListInContest(Context context, String[] args) throws Exception {
        HashMap<String, Object> requestMap = (HashMap<String, Object>) JPO.unpackArgs(args);
        MapList result = new MapList();

        try {
            String sReportId = (String) requestMap.get("objectId");
            BusinessObject boReport = new BusinessObject(sReportId);

            StringList selectBusStmts = new StringList(6);
            selectBusStmts.addElement(DomainConstants.SELECT_ID);

            try {
                ContextUtil.startTransaction(context, true);
                ExpansionIterator eiReportParameters = boReport.getExpansionIterator(context, "FPDM_AllToFPDM_ExcelReportResult", // relationship pattern
                        "FPDM_ExcelReportResult", // type pattern
                        selectBusStmts, // list of select statement pertaining to Business Objects
                        new StringList(), // list of select statement pertaining to Relationships
                        false, // get To relationships
                        true, // get From relationships
                        (short) 1, // the number of levels to expand, 0 equals expand all
                        null, // where clause to apply to objects, can be empty
                        null, // where clause to apply to relationship, can be empty
                        (short) 0, // the maximum number of objects to return
                        false, // true to check for hidden types per MX_SHOW_HIDDEN_TYPE_OBJECTS setting; false to return all objects, even if hidden
                        false, // true to return each target object only once in expansion
                        (short) 500, // page size to use for streaming data source
                        false // boolean true to force HashTable data to StringList; false will return String for single-valued selects, StringList for multi-valued selects
                );

                while (eiReportParameters.hasNext()) {
                    HashMap hmParameter = new HashMap();
                    RelationshipWithSelect rwsParam = eiReportParameters.next();
                    for (Object stmt : selectBusStmts) {
                        hmParameter.put(stmt, rwsParam.getTargetSelectData((String) stmt));
                    }
                    result.add(hmParameter);
                }
                eiReportParameters.close();
            } finally {
                ContextUtil.commitTransaction(context);
            }

        } catch (Exception e) {
            throw e;
        }

        return result;
    }

    /**
     * Get a list of all ExcelReportParameters linked with a given report
     * @param context
     *            an eMatrix Context object
     * @param args
     *            must contain objectId, the id of the report from where we list parameters
     * @return A MapList containing all parameters from the report passed in parameter
     * @throws Exception
     */
    public MapList getExcelReportParameters(Context context, String[] args) throws Exception {
        HashMap requestMap = (HashMap) JPO.unpackArgs(args);

        try {
            String sReportId = (String) requestMap.get("objectId");
            BusinessObject boReport = new BusinessObject(sReportId);

            StringList selectBusStmts = new StringList(6);
            selectBusStmts.addElement(DomainConstants.SELECT_ID);
            selectBusStmts.addElement(FPDM_ExcelReport_mxJPO.ATTRIBUTE_EXCEL_REPORT_PARAMETER_DEFAULT);
            selectBusStmts.addElement(FPDM_ExcelReport_mxJPO.ATTRIBUTE_EXCEL_REPORT_PARAMETER_TYPE);
            selectBusStmts.addElement(FPDM_ExcelReport_mxJPO.ATTRIBUTE_EXCEL_REPORT_PARAMETER_MANDATORY);
            selectBusStmts.addElement(FPDM_ExcelReport_mxJPO.ATTRIBUTE_EXCEL_REPORT_PARAMETER_VALUES);
            selectBusStmts.addElement(DomainConstants.SELECT_DESCRIPTION);

            StringList selectRelStmts = new StringList(0);

            MapList mlReportParameters = new MapList();
            try {
                ContextUtil.startTransaction(context, true);
                ExpansionIterator eiReportParameters = boReport.getExpansionIterator(context, RELATIONSHIP_EXCEL_REPORT_TO_EXCEL_REPORT_PARAMETER, // relationship pattern
                        TYPE_EXCEL_REPORT_PARAMETER, // type pattern
                        selectBusStmts, // list of select statement pertaining to Business Objects
                        selectRelStmts, // list of select statement pertaining to Relationships
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
                );

                while (eiReportParameters.hasNext()) {
                    HashMap hmParameter = new HashMap();
                    RelationshipWithSelect rwsParam = eiReportParameters.next();
                    for (Object stmt : selectBusStmts) {
                        hmParameter.put(stmt, rwsParam.getTargetSelectData((String) stmt));
                    }
                    mlReportParameters.add(hmParameter);
                }
                eiReportParameters.close();
            } finally {
                ContextUtil.commitTransaction(context);
            }

            return mlReportParameters;
        } catch (Exception e) {
            throw e;
        }
    }

    public void saveParameters(Context context, String[] args) throws Exception {
        Map<String, Object> map = (Map<String, Object>) JPO.unpackArgs(args);
    }

    /**
     * Create new parameter for a report from a line of the parameter's list
     * @param context
     *            an eMatrix Context object
     * @param args
     *            send by the save button of a table
     * @return an HashMap containing the changed row to be processed by the table UI
     * @throws Exception
     */
    public HashMap<String, Object> inlineCreateExcelReportParameter(Context context, String[] args) throws Exception {

        HashMap<String, Object> doc = new HashMap<String, Object>();
        HashMap<String, Object> request = (HashMap) JPO.unpackArgs(args);
        HashMap<String, Object> paramMap = (HashMap) request.get("paramMap");

        HashMap<String, Object> hmRelAttributesMap;
        HashMap<String, Object> columnsMap;
        HashMap<String, Object> changedRowMap;
        HashMap<String, Object> returnMap;
        String sType = (String) paramMap.get("type");
        String sSymbolicName = com.matrixone.apps.framework.ui.UICache.getSymbolicName(context, sType, "type");

        String sUser = context.getUser();

        Map<String, Object> smbAttribMap;

        MapList mlItems = new MapList();

        String parentObjectId = (String) paramMap.get("parentOID");

        Element elm = (Element) request.get("contextData");

        MapList chgRowsMapList = UITableIndented.getChangedRowsMapFromElement(context, elm);

        try {
            DomainObject parentObj = DomainObject.newInstance(context, parentObjectId);

            Iterator<HashMap<String, Object>> it = chgRowsMapList.iterator();
            while (it.hasNext()) {
                changedRowMap = (HashMap) it.next();

                String sRelId = (String) changedRowMap.get("relId");
                String sRowId = (String) changedRowMap.get("rowId");
                String markup = (String) changedRowMap.get("markup");

                columnsMap = (HashMap) changedRowMap.get("columns");

                if (MARKUP_NEW.equals(markup)) {
                    String sParameterDesc = (String) columnsMap.get("Description");

                    String sParameterValues = (String) columnsMap.get("FPDM_ExcelReportParameterValues");
                    String sParameterMandatory = (String) columnsMap.get("FPDM_ExcelReportParameterMandatory");
                    String sParameterDefault = (String) columnsMap.get("FPDM_ExcelReportParameterDefault");
                    String sParameterType = (String) columnsMap.get("FPDM_ExcelReportParameterType");

                    String typeAlias = FrameworkUtil.getAliasForAdmin(context, "type", FPDM_ExcelReport_mxJPO.TYPE_EXCEL_REPORT_PARAMETER, true);
                    String policyAlias = FrameworkUtil.getAliasForAdmin(context, "policy", FPDM_ExcelReport_mxJPO.POLICY_EXCEL_REPORT_PARAMETER, true);

                    String sParameterID = "";
                    try {
                        sParameterID = FrameworkUtil.autoName(context, typeAlias, policyAlias);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    BusinessObject excelReportParameterObject = new BusinessObject(sParameterID);

                    excelReportParameterObject.open(context);
                    excelReportParameterObject.setAttributeValue(context, "FPDM_ExcelReportParameterType", sParameterType);
                    excelReportParameterObject.setAttributeValue(context, "FPDM_ExcelReportParameterValues", sParameterValues);
                    excelReportParameterObject.setAttributeValue(context, "FPDM_ExcelReportParameterDefault", sParameterDefault);
                    excelReportParameterObject.setAttributeValue(context, "FPDM_ExcelReportParameterMandatory", sParameterMandatory);
                    excelReportParameterObject.setDescription(context, sParameterDesc);
                    excelReportParameterObject.setOwner(context, sUser);
                    RelationshipType rtRelation = new RelationshipType(FPDM_ExcelReport_mxJPO.RELATIONSHIP_EXCEL_REPORT_TO_EXCEL_REPORT_PARAMETER);
                    DomainRelationship domRelation = new DomainRelationship(excelReportParameterObject.connect(context, rtRelation, false, parentObj));
                    excelReportParameterObject.update(context);
                    excelReportParameterObject.close(context);

                    returnMap = new HashMap<String, Object>();
                    returnMap.put("pid", parentObjectId);
                    sRelId = domRelation.toString();
                    returnMap.put("relid", sRelId);
                    returnMap.put("oid", sParameterID);
                    returnMap.put("rowId", sRowId);
                    returnMap.put("markup", markup);
                    columnsMap.put("Name", excelReportParameterObject.getName());

                    returnMap.put("columns", columnsMap);

                    mlItems.add(returnMap); // returnMap having all the elements
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        doc.put("Action", "success");
        doc.put("changedRows", mlItems); // Adding the key "ChangedRows"

        return doc;
    }

    /**
     * Revise Excel Report : - Revise the report - clone attribute(s) of the old report - link this new attributes to the revised report
     * @param context
     *            an eMatrix Context object
     * @param args
     *            must contain objectId, the id of the report to revise
     * @return the id of the new report
     * @throws Exception
     */
    public String reviseExcelReport(Context context, String[] args) throws Exception {
        context.start(true);

        HashMap<String, Object> request = (HashMap) JPO.unpackArgs(args);

        String objectId = (String) request.get("objectId");

        DomainObject doReport = new DomainObject(objectId);

        // Revise the report
        BusinessObject lastRevObj = doReport.getLastRevision(context);
        String nextRev = lastRevObj.getNextSequence(context);
        String lastRevVault = lastRevObj.getVault();
        BusinessObject revBO = doReport.revise(context, nextRev, lastRevVault);

        // clone and attach parameters
        StringList selectStmts = new StringList(1);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        StringList selectRelStmts = new StringList(1);
        selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

        MapList parametersList = FrameworkUtil.toMapList(doReport.getExpansionIterator(context, FPDM_ExcelReport_mxJPO.RELATIONSHIP_EXCEL_REPORT_TO_EXCEL_REPORT_PARAMETER, // relationship pattern
                FPDM_ExcelReport_mxJPO.TYPE_EXCEL_REPORT_PARAMETER, // type pattern
                selectStmts, // list of select statement pertaining to Business Objects
                selectRelStmts, // list of select statement pertaining to Relationships
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

        Iterator itParams = parametersList.iterator();
        while (itParams.hasNext()) {
            try {
                Hashtable objPart = (Hashtable) itParams.next();

                String sParameterId = (String) objPart.get("id");
                DomainObject objParameter = new DomainObject(sParameterId);
                objParameter.open(context);

                String sNewName = DomainObject.getAutoGeneratedName(context, "type_" + FPDM_ExcelReport_mxJPO.TYPE_EXCEL_REPORT_PARAMETER, "");
                BusinessObject boTemp = objParameter.cloneObject(context, sNewName, "01", context.getVault().getName(), true);

                boTemp.open(context);
                RelationshipType rtRelation = new RelationshipType(FPDM_ExcelReport_mxJPO.RELATIONSHIP_EXCEL_REPORT_TO_EXCEL_REPORT_PARAMETER);
                boTemp.connect(context, rtRelation, false, revBO);
                boTemp.update(context);
                boTemp.close(context);

                objParameter.close(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        context.commit();
        return revBO.getObjectId(context);
    }

    /**
     * Check if a template is attached to the report
     * @param context
     *            an eMatrix Context object
     * @param args
     *            args[0] contains the id of a report
     * @return 0 if success
     * @throws Exception
     */
    public int checkTemplate(Context context, String[] args) throws Exception {
        int iReturn = 0;
        try {
            String sExcelReportId = args[0];
            DomainObject doExcelReport = DomainObject.newInstance(context, sExcelReportId);
            String sTemplateName = doExcelReport.getInfo(context, "format.file.name");

            if ("".equals(sTemplateName)) {
                String sMessage = EnoviaResourceBundle.getProperty(context, "ExcelReport", "FPDM_ExcelReport.message.NoTemplateUploaded", context.getSession().getLanguage());
                emxContextUtil_mxJPO.mqlNotice(context, sMessage);
                iReturn = 1;
            } else {
                // TODO:compare the report template tags to the fields returned by the report JPO
            }
        } catch (Exception e) {
            e.printStackTrace();
            iReturn = 1;
        }
        return iReturn;
    }

    /**
     * Delete the parameters of a report when deleted
     * @param context
     *            an eMatrix Context object
     * @param args
     *            args[0] contains the id of a report
     * @return 0 for preprocess
     * @throws Exception
     */
    public int deleteReportParameters(Context context, String[] args) throws Exception {
        try {
            String sExcelReportId = args[0];
            BusinessObject boExcelReport = new BusinessObject(sExcelReportId);

            RelationshipWithSelect rws;
            ExpansionIterator eiParameters = boExcelReport.getExpansionIterator(context, RELATIONSHIP_EXCEL_REPORT_TO_EXCEL_REPORT_PARAMETER, TYPE_EXCEL_REPORT_PARAMETER,
                    new StringList(DomainConstants.SELECT_ID), new StringList(), false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0, true, false,
                    (short) 500);
            while (eiParameters.hasNext()) {
                rws = eiParameters.next();
                mxBus.delete(context, rws.getTargetSelectData(DomainConstants.SELECT_ID));
            }
            eiParameters.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return 0;
    }

    public StringList getExcelReportProgram(Context context, String args[]) throws Exception {
        StringList slReportProgram = new StringList();

        HashMap<String, StringList> hmReportPrograms = (HashMap<String, StringList>) getExcelReportJPOList(context, args);
        StringList asChoicePrograms = hmReportPrograms.get("field_choices");

        return asChoicePrograms;
    }

    public void updateExcelReportProgram(Context context, String[] args) throws Exception {
        Map<String, Object> mArgs = JPO.unpackArgs(args);

        Map<String, Object> paramMap = (Map<String, Object>) mArgs.get("paramMap");

        String objectId = (String) paramMap.get("objectId");
        DomainObject doReport = DomainObject.newInstance(context, objectId);
    }

    /**
     * Display the list of Report JPO
     * @param context
     *            an eMatrix Context object
     * @throws Exception
     */
    public Object getExcelReportJPOList(Context context, String[] args) throws Exception {
        try {
            String response = fpdm.eai.utils.WebServiceManager_mxJPO.callEAIWS(context, "ExcelReportWebService", "getAllAvailableExcelReports", new HashMap<String, String>());
            StringList slExcelReportJPOList = new StringList();

            String[] slExcelReportTempList = response.split(Pattern.quote("|"));
            slExcelReportJPOList.addAll(Arrays.asList(slExcelReportTempList));

            HashMap hmReportJPOList = new HashMap();

            hmReportJPOList.put("field_choices", slExcelReportJPOList);
            hmReportJPOList.put("field_display_choices", slExcelReportJPOList);

            return hmReportJPOList;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    /**
     * Display the list of Tags for an ExcelReport program
     * @param context
     *            an eMatrix Context object
     * @throws Exception
     */
    public StringList getAllTagsForReport(Context context, String[] args) throws Exception {
        try {
            String objectId = args[0];

            DomainObject doReport = new DomainObject(objectId);
            String sReportProgramName = doReport.getInfo(context, "attribute[" + PropertyUtil.getSchemaProperty(context, "attribute_FPDM_ExcelReportJPO") + "].value");

            Map<String, String> mParams = new HashMap<>();
            mParams.put("reportName", sReportProgramName);

            String response = fpdm.eai.utils.WebServiceManager_mxJPO.callEAIWS(context, "ExcelReportWebService", "getTagsForReport", mParams);
            StringList lsExcelReportTagsList = new StringList();
            String[] slExcelReportTempList = response.split(Pattern.quote("|"));
            lsExcelReportTagsList.addAll(Arrays.asList(slExcelReportTempList));

            return lsExcelReportTagsList;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Update the attribute FPDM_ExcelReportJPO at creation
     * @param context
     *            an eMatrix Context object
     * @throws Exception
     */
    public static void updateExcelReportJPO(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String objectId = (String) paramMap.get("objectId");
            String newValue = (String) paramMap.get("New Value");

            DomainObject doExcelReport = DomainObject.newInstance(context, objectId);
            doExcelReport.setAttributeValue(context, PropertyUtil.getSchemaProperty(context, "attribute_FPDM_ExcelReportJPO"), newValue);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}