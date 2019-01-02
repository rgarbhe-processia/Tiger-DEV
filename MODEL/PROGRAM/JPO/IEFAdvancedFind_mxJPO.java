
/**
 * INFAdvancedFind.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright
 * notice is precautionary only and does not evidence any actual or intended publication of such program JPO for find and advanced find $Archive: $ $Revision: 1.2$ $Author: ds-unamagiri$ cnataraja
 * @since AEF 9.5.2.0
 */
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Query;
import matrix.util.SelectList;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;

public class IEFAdvancedFind_mxJPO {
    private static final String IC_PROPERTIES_FILE = "emxIEFDesignCenter";

    private static final String IC_VALIDATE_QUERY_TRIGGER = "emxIEFDesignCenter.ValidateQueryTrigger";

    public IEFAdvancedFind_mxJPO(Context context, String[] args) throws Exception {
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
        String sAnd = "&&";
        char chDblQuotes = '\"';
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String objectId = getParameter(paramMap, "objectId");
        MapList businessObjectList = new MapList();
        String savedQueryName = getParameter(paramMap, "queryName");
        String type = getParameter(paramMap, "selType");
        String expandCheckFlag = getParameter(paramMap, "expandCheckFlag");
        String oldtype = type;
        ResourceBundle icProps = ResourceBundle.getBundle(IC_PROPERTIES_FILE);
        String sValidateQuery = "";
        try {
            sValidateQuery = icProps.getString(IC_VALIDATE_QUERY_TRIGGER);
        } catch (Exception e) {
            sValidateQuery = "false";
        }
        boolean bValidateQuery = sValidateQuery.equalsIgnoreCase("true");
        // take care of the scenario where the user can type in the type name instead of
        // using the type chooser
        if ((type == null) || ((type != null) && (type.length() == 0)) || ((type != null) && ("*".equals(type)))) {
            oldtype = getParameter(paramMap, "txtType");
        }
        String name = getParameter(paramMap, "txtName");
        String revision = getParameter(paramMap, "txtRev");
        String vault = getParameter(paramMap, "txtVault");
        String owner = getParameter(paramMap, "txtOwner");
        String description = getParameter(paramMap, "description");
        String searchFormat = getParameter(paramMap, "comboFormat");

        String searchText = getParameter(paramMap, "txtPattern");

        String where = getParameter(paramMap, "txtWhereClause");
        String queryLimit = getParameter(paramMap, "queryLimit");
        String attachment = getParameter(paramMap, "preferences");

        // 15/12/03 ------------ Start --------

        String revisionType = "";
        if (!getParameter(paramMap, "revisionType").equals("null")) {
            revisionType = getParameter(paramMap, "revisionType");
        }

        // 15/12/03 ------------ End --------
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

            SelectList resultSelects = new SelectList(4);
            resultSelects.add(DomainObject.SELECT_ID);
            resultSelects.add(DomainObject.SELECT_NAME);
            resultSelects.add(DomainObject.SELECT_TYPE);
            resultSelects.add(DomainObject.SELECT_REVISION);

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

            context.commit();
            if (previousTransaction)
                context.start(false);
        } catch (Exception ex) {
            businessObjectList = new MapList();
        }
        return businessObjectList;
    }
}
