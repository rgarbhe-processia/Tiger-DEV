
/**
 * IEFFilterQuery.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright
 * notice is precautionary only and does not evidence any actual or intended publication of such program JPO finds filters a saved query on type and returns new results $Archive: $ $Revision: 1.2$
 * $Author: ds-unamagiri$ rahulp
 * @since AEF 9.5.2.0
 */
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.MQLCommand;
import matrix.db.Query;
import matrix.util.SelectList;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;

public class IEFFilterQuery_mxJPO {
    private static final String IC_PROPERTIES_FILE = "emxIEFDesignCenter";

    private static final String IC_VALIDATE_QUERY_TRIGGER = "emxIEFDesignCenter.ValidateQueryTrigger";

    public IEFFilterQuery_mxJPO(Context context, String[] args) throws Exception {
    }

    public String removeHiddenType(String types, Context context, boolean expand) {
        MQLCommand mqlCmd = new MQLCommand();
        String sResult = "";
        String newString = "";
        HashMap typeList = new HashMap();
        try {
            mqlCmd.open(context);
            if (expand)
                mqlCmd.executeCommand(context, "list  $1 $2 select $3 $4 $5 dump $6", "type", types, "hidden", "name", "derivative", "|");
            else
                mqlCmd.executeCommand(context, "list $1 $2 select $3 $4 dump $5", "type", types, "hidden", "name", "|");
            sResult = mqlCmd.getResult();
            // sResult = sResult.replace('\n','~');
            mqlCmd.close(context);
            StringTokenizer strTkParent = new StringTokenizer(sResult, "\n");
            while (strTkParent.hasMoreTokens()) {
                String token = strTkParent.nextToken();
                StringTokenizer strTkParentRow = new StringTokenizer(token, "|");
                String hidden = strTkParentRow.nextToken();
                String name = strTkParentRow.nextToken();
                if (hidden.equalsIgnoreCase("false")) {
                    typeList.put(name, name);
                }

                if (expand) {
                    while (strTkParentRow.hasMoreTokens()) {
                        String derivedType = strTkParentRow.nextToken();
                        MQLCommand mqlCmdDerived = new MQLCommand();
                        mqlCmdDerived.open(context);
                        mqlCmdDerived.executeCommand(context, "list $1 $2 select hidden name dump $3", "type", derivedType, "|");
                        String sResultDerived = mqlCmdDerived.getResult();
                        // sResultDerived = sResultDerived.replace('\n','~');
                        mqlCmdDerived.close(context);
                        StringTokenizer strTkDerived = new StringTokenizer(sResultDerived, "\n");
                        while (strTkDerived.hasMoreTokens()) {
                            String token1 = strTkDerived.nextToken();
                            StringTokenizer strTkDerivedRow = new StringTokenizer(token1, "|");
                            String derivedHidden = strTkDerivedRow.nextToken();
                            String derivedName = strTkDerivedRow.nextToken();
                            if (derivedHidden.equalsIgnoreCase("false")) {
                                typeList.put(derivedName, derivedName);
                            }
                        }
                    }
                }
            }
            Iterator itr = typeList.keySet().iterator();
            int i = 0;
            newString = "( ";
            while (itr.hasNext()) {
                /*
                 * newString+=(String)(itr.next()); if(i!=typeList.size()-1) newString+=","; i++;
                 */
                newString += "type ~~\"" + (String) (itr.next()) + "\"";
                if (i != typeList.size() - 1)
                    newString += " || ";
                i++;
            }
            newString += " )";
        } catch (Exception me) {
            String sErrorMsg = "list type '" + types + "' select name derived.name dump ---FAILED---" + me;
            System.out.println("ERROR # " + sErrorMsg);
        }
        return newString;
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public Object getList(Context context, String[] args) throws Exception {
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList businessObjectList = new MapList();
        ResourceBundle icProps = ResourceBundle.getBundle(IC_PROPERTIES_FILE);
        String sValidateQuery = "";
        try {
            sValidateQuery = icProps.getString(IC_VALIDATE_QUERY_TRIGGER);
        } catch (Exception e) {
            sValidateQuery = "false";
        }
        boolean bValidateQuery = sValidateQuery.equalsIgnoreCase("true");

        // selected saved query name
        String savedQueryName = (String) paramMap.get("emxTableRowId");
        if (savedQueryName == null)
            savedQueryName = (String) paramMap.get("queryName");
        // list of type name
        String typeList = (String) paramMap.get("selType");
        // limit for filtered query
        String queryLimit = (String) paramMap.get("queryLimit");
        if (queryLimit == null || queryLimit.equals("null") || queryLimit.equals(""))
            queryLimit = "0";
        if (savedQueryName != null || !savedQueryName.equals("")) {
            // decode the query name
            // savedQueryName = URLDecoder.decode(savedQueryName);
            matrix.db.Query query = new matrix.db.Query(savedQueryName);
            query.open(context);
            String type = query.getBusinessObjectType();
            // DSCINC 1200
            if (type.equals("*") && paramMap.get("txtType") != null) {
                type = (String) paramMap.get("txtType");
            }
            // DSCINC 1200
            String oldtype = type;
            type = removeHiddenType(type, context, query.getExpandTypes());

            // extract where clause and create a new where
            // clause by adding the types
            String where = query.getWhereExpression();
            String oldWhere = where;
            if (typeList != null && !typeList.equals("")) {
                // typeList = URLDecoder.decode(typeList);
                StringTokenizer str = new StringTokenizer(typeList, ",");
                String types = "";
                int index = 0;
                int tokenCount = str.countTokens();
                while (str.hasMoreTokens()) {
                    String token = str.nextToken();
                    types += " type ~~ " + "\"" + token + "\"";
                    if (index != tokenCount - 1)
                        types += " || ";
                    index++;
                }
                if (where != null && !where.equals("") && where.length() > 0)
                    where += " && " + types;
                else
                    where += types;
            }
            String newWhere = where;
            if (newWhere == null || newWhere.equalsIgnoreCase("null") || newWhere.equals("") || newWhere.length() == 0)
                newWhere = type;
            else
                newWhere += " " + " && " + " " + type;

            // take the last query executed and modify accordingly
            Query newQuery = new Query(".finder");
            // newQuery.setBusinessObjectType("*");
            newQuery.setBusinessObjectType(type);
            newQuery.setBusinessObjectName(query.getBusinessObjectName());
            newQuery.setBusinessObjectRevision(query.getBusinessObjectRevision());
            if (!oldtype.equals(type))
                newQuery.setExpandType(false);
            else
                newQuery.setExpandType(query.getExpandTypes());
            newQuery.setOwnerPattern(query.getOwnerPattern());
            newQuery.setSearchText(query.getSearchText());
            newQuery.setSearchFormat(query.getSearchFormat());
            newQuery.setVaultPattern(query.getVaultPattern());
            newQuery.setWhereExpression(newWhere);
            newQuery.setObjectLimit(Short.parseShort(queryLimit));
            // execute query
            SelectList resultSelects = new SelectList(4);
            resultSelects.add(DomainObject.SELECT_ID);
            resultSelects.add(DomainObject.SELECT_NAME);
            resultSelects.add(DomainObject.SELECT_TYPE);
            resultSelects.add(DomainObject.SELECT_REVISION);
            businessObjectList = FrameworkUtil.toMapList(newQuery.select(context, resultSelects));
            // if(!oldtype.equals(type))
            {
                newQuery.setBusinessObjectType(oldtype);
                newQuery.setExpandType(query.getExpandTypes());
                newQuery.setWhereExpression(oldWhere);
                newQuery.setQueryTrigger(bValidateQuery);
                newQuery.update(context);
            }
            query.close(context);
            newQuery.close(context);
        }
        return businessObjectList;
    }
}
