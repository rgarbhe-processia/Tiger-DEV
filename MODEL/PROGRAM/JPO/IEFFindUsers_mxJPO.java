
/**
 * IEFFindUsers.java Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright
 * notice is precautionary only and does not evidence any actual or intended publication of such program This is a JPO which act as a data source for rendering data in to a custom table . Using this
 * JPO program developer can create their own column definitions and can return tabledata in a IEF_CustomMapList which stores each row of table as Map objects. Project. Infocentral Migration to UI
 * level 3 $Archive: $ $Revision: 1.2$ $Author: ds-unamagiri$
 * @since AEF 9.5.2.0
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import matrix.db.Context;
import matrix.db.Group;
import matrix.db.GroupItr;
import matrix.db.GroupList;
import matrix.db.JPO;
import matrix.db.PersonItr;
import matrix.db.PersonList;
import matrix.db.Role;
import matrix.db.RoleItr;
import matrix.db.RoleList;
import matrix.util.Pattern;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.uicomponents.beans.IEF_CellData;
import com.matrixone.MCADIntegration.uicomponents.beans.IEF_ColumnDefinition;
import com.matrixone.MCADIntegration.uicomponents.util.IEF_CustomMapList;

public class IEFFindUsers_mxJPO {
    StringList sListPerson;

    StringList sListGroup;

    StringList sListRole;

    String sFilter;

    /**
     * This is constructor which intializes variable declared
     * @since AEF 9.5.2.0
     */

    public IEFFindUsers_mxJPO(Context context, String[] args) throws Exception {
    }

    /**
     * This method retutns list of column definitions by setting their column properties. These columns definitions control the look & feel of the target table. Following methos controls look & feel
     * of table displayong toolsets. Table displaying users has following columns user name + icon for person / role / group
     * @param Context
     *            context for user logged in
     * @param String
     *            array
     * @return Object as ArrayList
     * @since AEF 9.5.2.0
     */
    public Object getColumnDefinitions(Context context, String[] args) throws Exception {
        ArrayList columnDefs = new ArrayList();

        // Creating 2 columns : user Name, type
        IEF_ColumnDefinition column1 = new IEF_ColumnDefinition();

        // Initializing column for user name
        column1.setColumnTitle("emxInfoCentral.Common.Name");
        column1.setColumnKey("Name");
        column1.setColumnDataType("string");
        column1.setColumnType("icon");
        column1.setColumnTarget("content");

        // Adding them to the list to be returned to the caller
        columnDefs.add(column1);
        return columnDefs;
    }

    /**
     * List returned by this method is used to render a table displaying users corresponding to the query entered in the user-search dialog. It returns a IEF_CustomMapList containing hashmap objects.
     * Each hashmap descrbes a row in the table.
     * @param Context
     *            context for user logged in
     * @param String
     *            array This method expects following parameters to be packed in string array sUserName=<user_name_value> sFirstName=<first_name_value> sLastName=<last_name_value> sCompany=
     *            <company_name_value> sTopLevel=<toplevel_value> sPerson=<person_value> sGroup=<group_value> sRole=<role_value> queryLimit=<query_limit_value>
     * @return Object as IEF_CustomMapList
     * @author GauravG
     * @since AEF 9.5.2.0
     */

    public Object getTableData(Context context, String[] args) throws Exception {
        IEF_CustomMapList usersList = null;

        try {
            usersList = new IEF_CustomMapList();

            // Build query using list of parameters received from the caller
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);

            // Read the limit specified by the end user
            String sLimit = (String) paramMap.get("queryLimit");
            int nDisplayLimit = 0;

            if ((sLimit != null) && (!sLimit.equals("")))
                nDisplayLimit = Integer.parseInt(sLimit);

            int iFilterCount = getUserList(paramMap, context);

            // loop over each of the lists & create a row for each of them
            if (((sListPerson != null) && (sListPerson.size() != 0)) || ((sListGroup != null) && (sListGroup.size() != 0)) || ((sListRole != null) && (sListRole.size() != 0))) {
                // check if number of entries received as serach result are more than the limit nDisplayLimit
                if (iFilterCount > nDisplayLimit) {
                    iFilterCount = nDisplayLimit;
                }

                int count = 0;
                count = filterList(sListPerson, iFilterCount, usersList, "images/iconPerson.gif");

                count += filterList(sListGroup, iFilterCount, usersList, "images/iconGroup.gif");

                count += filterList(sListRole, iFilterCount, usersList, "images/iconRole.gif");

            } // End Of If any of the lists is non empty

        } catch (Exception e) {

            usersList = new IEF_CustomMapList();
        }

        return usersList;
    }

    private int filterList(StringList currentList, int iLimit, IEF_CustomMapList usersList, String sImageSrc) {
        int count = 0;
        Pattern patternGeneric = null;
        IEF_CellData cellData = null;
        Map map = null;

        // Iterate through given list
        if (currentList != null && currentList.size() > 0) {
            for (int i = 0; ((i < currentList.size()) && (usersList.size() < iLimit)); i++) {
                String sValue = "";
                String sReassign = (String) currentList.elementAt(i);

                if ((sFilter == null) && (sFilter.equals(""))) {
                    sValue = sReassign;
                } else {
                    patternGeneric = new Pattern(sFilter);
                    if (patternGeneric.match(sReassign)) {
                        sValue = sReassign;
                    }
                }

                if (!sValue.equals("")) {
                    // Create a map representing a row of table displaying list of users
                    map = new Hashtable();

                    // put row id
                    map.put("ID", sValue);

                    // Create a cell for column for user name
                    cellData = new IEF_CellData();
                    cellData.setCellText(sValue);
                    cellData.setIconUrl(sImageSrc);
                    map.put("Name", cellData);

                    // add row for this person to the list
                    usersList.add(map);
                    count++;
                }
            }
        }
        return count;
    }

    private int getUserList(HashMap paramMap, Context context) {
        int iEntriesFound = 0;

        try {
            // To Store all the Params
            String sTopLevel = (String) paramMap.get("chkbxTopLevel");
            String sPerson = (String) paramMap.get("chkbxPerson");
            String sGroup = (String) paramMap.get("chkbxGroup");
            String sRole = (String) paramMap.get("chkbxRole");
            sFilter = (String) paramMap.get("txtFilter");

            boolean bTopLevel = false;
            boolean bPerson = false;
            boolean bGroup = false;
            boolean bRole = false;

            // Depending on the checkbox status, set fkag status
            if ((sTopLevel != null) && (sTopLevel.equals("checked"))) {
                bTopLevel = true;
            }

            if ((sPerson != null) && (sPerson.equals("checked"))) {
                bPerson = true;
            }

            if ((sGroup != null) && (sGroup.equals("checked"))) {
                bGroup = true;
            }

            if ((sRole != null) && (sRole.equals("checked"))) {
                bRole = true;
            }

            if ((sFilter == null) || (sFilter.equals(""))) {
                sFilter = "";
            }

            sListPerson = new StringList();
            sListGroup = new StringList();
            sListRole = new StringList();

            // populate the list of people
            if (bPerson) {
                PersonList personListGeneric = matrix.db.Person.getPersons(context, true);
                PersonItr personItrGeneric = new PersonItr(personListGeneric);

                while (personItrGeneric.next()) {
                    sListPerson.addElement(personItrGeneric.obj().toString());
                    iEntriesFound++;
                }
            }

            // populate the List of groups
            GroupList groupListGeneric = null;
            GroupItr groupItrGeneric = null;
            if ((bTopLevel == false) && (bGroup == true)) {
                groupListGeneric = Group.getGroups(context, true);
                groupItrGeneric = new GroupItr(groupListGeneric);

                while (groupItrGeneric.next()) {
                    sListGroup.addElement(groupItrGeneric.obj().toString());
                    iEntriesFound++;
                }
            } else {
                if ((bTopLevel == true) && (bGroup == true)) {
                    groupListGeneric = Group.getTopLevelGroups(context, true);
                    groupItrGeneric = new GroupItr(groupListGeneric);

                    while (groupItrGeneric.next()) {
                        sListGroup.addElement(groupItrGeneric.obj().toString());
                        iEntriesFound++;
                    }
                }
            }

            // populate the List of roles
            // criteria specified.
            RoleList roleListGeneric = null;
            RoleItr roleItrGeneric = null;

            if ((bTopLevel == false) && (bRole == true)) {
                roleListGeneric = Role.getRoles(context, true);
                roleItrGeneric = new RoleItr(roleListGeneric);

                while (roleItrGeneric.next()) {
                    sListRole.addElement(roleItrGeneric.obj().toString());
                    iEntriesFound++;
                }
            } else {
                if ((bTopLevel == true) && (bRole == true)) {
                    roleListGeneric = Role.getTopLevelRoles(context, true);
                    roleItrGeneric = new RoleItr(roleListGeneric);

                    while (roleItrGeneric.next()) {
                        sListRole.addElement(roleItrGeneric.obj().toString());
                        iEntriesFound++;
                    }
                }
            }

            sListPerson.sort();
            sListGroup.sort();
            sListRole.sort();
        } catch (Exception ex) {
            iEntriesFound = 0;
        }
        return iEntriesFound;
    }

}// End of class
