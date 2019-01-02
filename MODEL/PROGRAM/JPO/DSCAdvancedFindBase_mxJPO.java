
/**
 * DSCAdvancedFindBase.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries,
 * Copyright notice is precautionary only and does not evidence any actual or intended publication of such program JPO for find and advanced find $Archive: $ $Revision: 1.1$ $Author: ds-unamagiri$
 * cnataraja
 * @since AEF 9.5.2.0
 */
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Query;
import matrix.util.SelectList;

import com.matrixone.MCADIntegration.utils.MCADUrlUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;

public class DSCAdvancedFindBase_mxJPO {
    protected static final String IC_PROPERTIES_FILE = "emxIEFDesignCenter";

    protected static final String IC_VALIDATE_QUERY_TRIGGER = "emxIEFDesignCenter.ValidateQueryTrigger";

    public DSCAdvancedFindBase_mxJPO(Context context, String[] args) throws Exception {
    }

    public String getParameter(HashMap paramMap, String key) throws Exception {
        String paramValue = (String) paramMap.get(key);
        if (paramValue == null || paramValue.equalsIgnoreCase("null") || paramValue.length() == 0) {
            if (key.equals("queryLimit"))
                paramValue = "0";
            else {
                if (key.equals("txtWhereClause") || key.equals("txtPattern") || key.equals("queryName"))
                    paramValue = "";
                else
                    paramValue = "*";
            }
        }
        return paramValue;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getList(Context context, String[] args) throws Exception {
        eMatrixDateFormat.setEMatrixDateFormat();

        String sAnd = "&&";
        char chDblQuotes = '\"';
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList businessObjectList = new MapList();
        String type = getParameter(paramMap, "selType");
        String expandCheckFlag = getParameter(paramMap, "expandCheckFlag");
        String oldtype = type;
        ResourceBundle icProps = ResourceBundle.getBundle(IC_PROPERTIES_FILE);
        String sValidateQuery = "";

        String eco = getParameter(paramMap, "txtECO");

        try {
            sValidateQuery = icProps.getString(IC_VALIDATE_QUERY_TRIGGER);
        } catch (Exception e) {
            sValidateQuery = "false";
        }
        boolean bValidateQuery = sValidateQuery.equalsIgnoreCase("true");
        // take care of the scenario where the user can type in the type name instead of
        // using the type chooser

        if ((type == null) || (type.equals("...")) || ((type != null) && (type.length() == 0)) || ((type != null) && ("*".equals(type)))) {
            oldtype = getParameter(paramMap, "txtType");
        }
        String name = getParameter(paramMap, "txtName");
        String revision = getParameter(paramMap, "txtRev");
        String vault = getParameter(paramMap, "txtVault");
        String state = getParameter(paramMap, "state");
        boolean isLatestRevisionOnly = false;
        String strLatestRevisionOnly = getParameter(paramMap, "latestRevisionOnly");
        if (strLatestRevisionOnly != null && strLatestRevisionOnly.equalsIgnoreCase("true") == true)
            isLatestRevisionOnly = true;

        String sOwner = getParameter(paramMap, "owner");

        String sDisplayOwner = getParameter(paramMap, "displayowner");
        sOwner = sOwner.trim();

        if (null == sOwner || ("null").equals(sOwner) || (",").equals(sOwner) || ("*").equals(sOwner))
            sOwner = getParameter(paramMap, "txtOwner");

        sOwner = sOwner.trim();

        sDisplayOwner = sDisplayOwner.trim();
        String description = getParameter(paramMap, "description");
        String searchFormat = getParameter(paramMap, "comboFormat");

        String searchText = getParameter(paramMap, "txtPattern");

        String where = getParameter(paramMap, "txtWhereClause");
        String queryLimit = getParameter(paramMap, "queryLimit");
        String attachment = getParameter(paramMap, "preferences");

        String createdBefore = getParameter(paramMap, "createdBefore");
        String createdAfter = getParameter(paramMap, "createdAfter");

        if (!createdBefore.equals("*") && !createdBefore.equals(""))
            createdBefore = MCADUrlUtil.hexDecode(createdBefore);

        if (!createdAfter.equals("*") && !createdAfter.equals(""))
            createdAfter = MCADUrlUtil.hexDecode(createdAfter);

        String workspaceFolderId = getParameter(paramMap, "workspaceFolderId");
        String matchCase = (String) paramMap.get("matchCase");
        boolean bMatchCase = true;
        String timeZone = (String) paramMap.get("timeZone");
        double clientTZOffset = new Double(timeZone).doubleValue();
        java.util.Locale localeObj = (java.util.Locale) paramMap.get("localeObj");

        if (null == matchCase || 0 == matchCase.length())
            bMatchCase = false;

        String owner = "*";

        if (null == sOwner || ("null").equals(sOwner) || (",").equals(sOwner)) {
            sOwner = "";
        }

        StringTokenizer st = new StringTokenizer(sOwner, ";");
        while (st.hasMoreTokens()) {
            if (owner.equals("*")) {
                owner = "";
                owner = owner + "," + st.nextToken();
            }
            break;
        }
        if (sDisplayOwner != null && sDisplayOwner.length() > 0 && sDisplayOwner.indexOf(";") < 0 && !("*").equals(sDisplayOwner))
            owner = sDisplayOwner;

        String revisionType = "";
        if (!getParameter(paramMap, "revisionType").equals("null")) {
            revisionType = getParameter(paramMap, "revisionType");
        }

        if (isLatestRevisionOnly)
            revisionType = "latest";

        try {
            if (searchText.equals("*"))
                searchText = "";

            if (where.equals("*"))
                where = "";

            if (attachment != null) {
                String format = "";

                if (attachment.equals("Yes")) {
                    format = " format.hasfile==true ";
                } else if (attachment.equals("No")) {
                    format = " format.hasfile==false ";
                } else {
                    format = "";
                }

                if (where.equals("")) {
                    if (!format.equals(""))
                        where += format;
                } else {
                    if (!format.equals(""))
                        where += " " + sAnd + " " + format;
                }
            }

            if (!(description == null || description.equalsIgnoreCase("null") || description.length() == 0 || description.equals("*"))) {
                String sDescQuery = "description ~~ " + chDblQuotes + description + chDblQuotes;
                if (where == null || where.equalsIgnoreCase("null") || where.equals("") || where.length() == 0)
                    where += sDescQuery;
                else
                    where += " " + sAnd + " " + sDescQuery;
            }

            if (!(searchFormat == null || searchFormat.equalsIgnoreCase("null") || searchFormat.length() == 0 || searchFormat.equals("*"))) {
                String sFormat = "";
                StringTokenizer str = new StringTokenizer(searchFormat, ",");
                int tokenCount = str.countTokens();
                int index = 0;
                while (str.hasMoreTokens()) {
                    String format = str.nextToken();
                    sFormat += "format==" + "\"" + format + "\"";
                    if (index != tokenCount - 1)
                        sFormat += " || ";
                    index++;
                }

                if (where == null || where.equalsIgnoreCase("null") || where.equals("") || where.length() == 0)
                    where = "((" + sFormat + "))";
                else
                    where += " " + sAnd + " ((" + sFormat + "))";
            }

            // Designer Central DSC Starts
            // createdBefore
            if (!(createdBefore == null || createdBefore.equalsIgnoreCase("null") || createdBefore.length() == 0 || createdBefore.equals("*"))) {
                int intDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
                DateFormat formatter = DateFormat.getDateInstance(intDateFormat, localeObj);
                Date date = formatter.parse(createdBefore);
                createdBefore = formatter.format(date);
                // String strInputTime = "00:00:00 AM";
                String strInputTime = "11:59:59 PM";
                createdBefore = eMatrixDateFormat.getFormattedInputDateTime(context, createdBefore, strInputTime, clientTZOffset, localeObj);

                String sExpCreatedBefore = "originated<" + "\"" + createdBefore + "\"";
                if (where == null || where.equalsIgnoreCase("null") || where.equals("") || where.length() == 0)
                    where = "((" + sExpCreatedBefore + "))";
                else
                    where += " " + sAnd + " ((" + sExpCreatedBefore + "))";
            }

            // createdAfter
            if (!(createdAfter == null || createdAfter.equalsIgnoreCase("null") || createdAfter.length() == 0 || createdAfter.equals("*"))) {
                int intDateFormat = eMatrixDateFormat.getEMatrixDisplayDateFormat();
                DateFormat formatter = DateFormat.getDateInstance(intDateFormat, localeObj);
                Date date = formatter.parse(createdAfter);
                createdAfter = formatter.format(date);
                // String strInputTime = "00:00:00 AM";
                String strInputTime = "11:59:59 PM";
                createdAfter = eMatrixDateFormat.getFormattedInputDateTime(context, createdAfter, strInputTime, clientTZOffset, localeObj);

                String sExpCreatedAfter = "originated>" + "\"" + createdAfter + "\"";
                if (where == null || where.equalsIgnoreCase("null") || where.equals("") || where.length() == 0)
                    where = "((" + sExpCreatedAfter + "))";
                else
                    where += " " + sAnd + " ((" + sExpCreatedAfter + "))";
            }

            if (!(state == null || state.equalsIgnoreCase("null") || state.length() == 0 || state.equals("*"))) {
                String sExpState = "current" + "==\"" + state + "\"";
                if (where == null || where.equalsIgnoreCase("null") || where.equals("") || where.length() == 0)
                    where = "((" + sExpState + "))";
                else
                    where += " " + sAnd + " ((" + sExpState + "))";
            }

            String sSymFolderRelationship = "relationship_VaultedDocuments";
            if (workspaceFolderId.startsWith("p_")) {
                workspaceFolderId = workspaceFolderId.substring(2);
                sSymFolderRelationship = "relationship_VaultedDocumentsRev2";
            }
            // workspaceFolderId
            if (!(workspaceFolderId == null || workspaceFolderId.equalsIgnoreCase("null") || workspaceFolderId.length() == 0 || workspaceFolderId.equals("*"))) {
                String vaultedObjectRelationship = PropertyUtil.getSchemaProperty(context, sSymFolderRelationship);
                String sExpWorkspaceFolderId = "to[" + vaultedObjectRelationship + "].from.id == \"" + workspaceFolderId + "\"";
                if (where == null || where.equalsIgnoreCase("null") || where.equals("") || where.length() == 0)
                    where = "((" + sExpWorkspaceFolderId + "))";
                else
                    where += " " + sAnd + " ((" + sExpWorkspaceFolderId + "))";
            }

            // Designer Central DSC Ends
            SelectList resultSelects = new SelectList(4);
            resultSelects.add(DomainObject.SELECT_ID);
            resultSelects.add(DomainObject.SELECT_NAME);
            resultSelects.add(DomainObject.SELECT_TYPE);
            resultSelects.add(DomainObject.SELECT_REVISION);
            resultSelects.add(DomainObject.SELECT_CURRENT);

            // using search of saved query?
            if (type != null && !"".equals(type.trim())) {
                // expand bo?
                boolean expandFlag = false;
                if ("true".equalsIgnoreCase(expandCheckFlag)) {
                    expandFlag = true;
                }

                // find only objects whose type is not hidden
                // mql and adk returns objects whose type is hidden
                // so set whereclause to filter the result

                // 15/12/03 ------------ Start --------
                if (revisionType.equals("latest")) {
                    // where += "revision==last";
                    if (where == null || where.equalsIgnoreCase("null") || where.equals("") || where.length() == 0)
                        where += "revision==last";
                    else
                        where += " " + sAnd + " " + "revision==last";
                }

                // 15/12/03 ------------ End --------

                String whereClause = where;
                if (whereClause == null || whereClause.equalsIgnoreCase("null") || whereClause.equals("") || whereClause.length() == 0) {
                    whereClause += "type.hidden == FALSE";
                } else {
                    whereClause += " " + sAnd + " " + "type.hidden == FALSE";
                }

                if (false == bMatchCase && false == name.equals("*")) {
                    StringTokenizer nameTokens = new StringTokenizer(name, ",");

                    if (nameTokens.hasMoreTokens())
                        whereClause += " " + sAnd + " " + "name smatchlist \"" + name + "\" " + "','";
                    else
                        whereClause += " " + sAnd + " " + "name ~~  \"" + name + "\"";

                    name = "*";
                }

                if (!eco.equals("*") && !eco.equals("")) {
                    ResourceBundle iefProperties = ResourceBundle.getBundle("ief");
                    String ecoAttributeName = iefProperties.getString("mcadIntegration.ECOAttributeName");
                    whereClause += " " + sAnd + " attribute[" + ecoAttributeName + "] ~~ \"" + eco + "\" ";
                }

                // DSC 10.6
                businessObjectList = DomainObject.findObjects(context, oldtype, // type keyed in or selected from type chooser
                        name, revision, owner, vault, whereClause, "", // save to the .finder later
                        expandFlag, resultSelects, Short.parseShort(queryLimit), "*", searchText);
            }

            /***************************************************************
             * //Follwing code is commented for bug fix id :291260 //This code is used to update search criteria to saved queries. //saved query is being used if (savedQueryName != null &&
             * !"".equals(savedQueryName) && !"null".equals(savedQueryName)) { Query savedQuery = new Query(savedQueryName); savedQuery.open(context); savedQuery.setBusinessObjectType(oldtype);
             * savedQuery.setBusinessObjectName(name); savedQuery.setBusinessObjectRevision(revision); savedQuery.setExpandType("true".equalsIgnoreCase(expandCheckFlag));
             * savedQuery.setOwnerPattern(owner); savedQuery.setSearchText(searchText); savedQuery.setSearchFormat("*"); savedQuery.setVaultPattern(vault); savedQuery.setWhereExpression(where);
             * savedQuery.setObjectLimit(Short.parseShort(queryLimit)); savedQuery.setQueryTrigger(bValidateQuery); savedQuery.update(context); savedQuery.close(context); }
             ******************************************************************/
            boolean previousTransaction = context.isTransactionActive();
            if (previousTransaction)
                context.commit();
            context.start(true);
            // finally after the results are found,
            // set the current query to the .finder
            Query newQuery = new Query(".finder");
            newQuery.open(context);
            newQuery.setBusinessObjectType(oldtype);
            newQuery.setBusinessObjectName(name);
            newQuery.setBusinessObjectRevision(revision);
            newQuery.setExpandType("true".equalsIgnoreCase(expandCheckFlag));
            newQuery.setOwnerPattern(owner);
            newQuery.setSearchText(searchText);
            newQuery.setSearchFormat("*");
            newQuery.setVaultPattern(vault);
            newQuery.setWhereExpression(where);
            newQuery.setObjectLimit(Short.parseShort(queryLimit));
            newQuery.setQueryTrigger(bValidateQuery);
            newQuery.update(context);
            newQuery.close(context);

            // Save the search using ".iefdesignsenterFinder" to retrive the last execute query in IEF Design
            // center search dialog.
            // DSCINC 1200
            Query newidcFinderQuery = new Query(".iefdesigncenterFinder");
            newidcFinderQuery.open(context);
            newidcFinderQuery.setBusinessObjectType(oldtype);
            newidcFinderQuery.setBusinessObjectName(name);
            newidcFinderQuery.setBusinessObjectRevision(revision);
            newidcFinderQuery.setExpandType("true".equalsIgnoreCase(expandCheckFlag));
            newidcFinderQuery.setOwnerPattern(owner);
            newidcFinderQuery.setSearchText(searchText);
            newidcFinderQuery.setSearchFormat("*");
            newidcFinderQuery.setVaultPattern(vault);
            newidcFinderQuery.setWhereExpression(where);
            newidcFinderQuery.setObjectLimit(Short.parseShort(queryLimit));
            newidcFinderQuery.setQueryTrigger(bValidateQuery);
            newidcFinderQuery.update(context);
            newidcFinderQuery.close(context);
            // DSCINC 1200

            context.commit();
            if (previousTransaction)
                context.start(false);
        } catch (Exception ex) {
            ex.printStackTrace();
            businessObjectList = new MapList();
        }
        return businessObjectList;
    }
}
