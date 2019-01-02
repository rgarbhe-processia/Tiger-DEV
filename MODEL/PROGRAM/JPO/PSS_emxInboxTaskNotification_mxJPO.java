
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import pss.constants.TigerConstants;
import matrix.db.Context;
import matrix.db.Group;
import matrix.db.JPO;
import matrix.db.Role;
import matrix.db.UserItr;
import matrix.util.MatrixException;
import matrix.util.StringList;

import com.matrixone.apps.common.util.ComponentsUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MessageUtil;
import com.matrixone.apps.domain.util.MqlUtil;

public class PSS_emxInboxTaskNotification_mxJPO extends emxSpool_mxJPO {
    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxInboxTaskNotification_mxJPO.class);

    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - END

    /**
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public PSS_emxInboxTaskNotification_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * This method is executed if a specific method is not specified.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @returns nothing
     * @throws Exception
     *             if the operation fails
     * @since AEF Rossini
     * @grade 0
     */
    public int mxMain(Context context, String[] args) throws Exception {
        if (!context.isConnected())
            throw new Exception(ComponentsUtil.i18nStringNow("emxComponents.Generic.NotSupportedOnDesktopClient", context.getLocale().getLanguage()));
        return 0;
    }

    /**
     * Get the subject for the notification.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            [0] contains map of values for building the notification subject
     * @returns the Subject string
     * @throws Exception
     *             if the operation fails
     * @since V6R2012x
     */
    public String getSubject(Context context, String[] args) throws Exception {

        Map info = (Map) JPO.unpackArgs(args);
        Map payload = (Map) info.get("payload");
        String subject = (String) payload.get("subject");
        String[] subjectKeys = (String[]) payload.get("subjectKeys");
        String[] subjectValues = (String[]) payload.get("subjectValues");
        String companyName = null;
        String basePropName = (String) info.get("bundleName");
        Locale locale = (Locale) info.get("locale");

        subject = MessageUtil.getMessage(context, null, subject, subjectKeys, subjectValues, companyName, locale, basePropName);

        return (subject);
    }

    /**
     * Get the html message for the notification. Called to create email.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            [0] contains map of values for building the notification html
     * @returns the html message string
     * @throws Exception
     *             if the operation fails
     * @since V6R2012x
     */
    public String getMessageHTML(Context context, String[] args) throws Exception {

        // get the message text
        String message = getMessage(context, args);

        // add in some white space
        if (message.length() > 0) {
            message += "<br><br>";
        }

        // build the table part of the message
        Map info = (Map) JPO.unpackArgs(args);
        Map payload = (Map) info.get("payload");
        String tableHeader = (String) payload.get("tableHeader");
        String tableRow = (String) payload.get("tableRow");
        String[] tableRowKeys = (String[]) payload.get("tableRowKeys");
        String[] tableRowValues = (String[]) payload.get("tableRowValues");
        String clickMessage = (String) payload.get("click");
        String[] clickKeys = (String[]) payload.get("clickKeys");
        String[] clickValues = (String[]) payload.get("clickValues");

        String companyName = null;
        String basePropName = (String) info.get("bundleName");
        Locale locale = (Locale) info.get("locale");

        message += "<html><body>";
        message += "<style>body, th, td {font-family:Verdana;font-size:11px;text-align:left;padding:5px;}</style>";

        if (clickMessage != null && clickMessage.length() > 0) {
            clickMessage = MessageUtil.getMessage(context, null, clickMessage, clickKeys, clickValues, companyName, locale, basePropName);
        }

        if (clickMessage != null && clickMessage.length() > 0) {
            message += clickMessage;
            message += "<br><br>";
        }

        tableHeader = MessageUtil.getMessage(context, tableHeader, companyName, locale, basePropName);
        if (tableHeader != null && tableHeader.length() > 0) {
            message += "<table border = 1><thead>" + tableHeader + "</thead><tbody><tr>";
            message += MessageUtil.getMessage(context, null, tableRow, tableRowKeys, tableRowValues, companyName, locale, basePropName);
            message += "</tr></tbody></table>";
        }

        message += "</body></html>";

        return (message);
    }

    /**
     * Get the text message for the notification. Called to generate icon mail.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains map of values for building the notification text
     * @returns the text message string
     * @throws Exception
     *             if the operation fails
     * @since V6R2012x
     */
    public String getMessageText(Context context, String[] args) throws Exception {

        Map info = (Map) JPO.unpackArgs(args);
        String basePropName = (String) info.get("bundleName");
        String baseURL = (String) info.get("baseURL");
        Locale locale = (Locale) info.get("locale");
        String objectId = (String) info.get("id");

        // get the message text
        String message = getMessage(context, args);
        // add in links for route, task and content
        message += emxMailUtil_mxJPO.getInboxTaskMailMessage(context, objectId, locale, basePropName, baseURL, null);
        return (message);
    }

    /**
     * Get the message for the notification.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains map of values for building the notification text
     * @returns the text message string
     * @throws Exception
     *             if the operation fails
     * @since V6R2012x
     */
    private String getMessage(Context context, String[] args) throws Exception {

        Map info = (Map) JPO.unpackArgs(args);
        Map payload = (Map) info.get("payload");
        String message = (String) payload.get("message");
        String[] messageKeys = (String[]) payload.get("messageKeys");
        String[] messageValues = (String[]) payload.get("messageValues");
        String companyName = null;
        String basePropName = (String) info.get("bundleName");
        Locale locale = (Locale) info.get("locale");

        message = MessageUtil.getMessage(context, null, message, messageKeys, messageValues, companyName, locale, basePropName);
        return (message);
    }

    // TIGTK-7496:Rutuja Ekatpure:15/5/2017:start
    /**
     * This method is used to get the name of the route task owner.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains map of values for the route task
     * @return StringList containing the name of the route task owner.
     * @throws Exception
     *             if the operation fails
     * @since V6R2012x
     */
    public StringList getRouteTaskOwner(Context context, String[] args) throws Exception {

        StringList routeTaskOwnerList = new StringList();
        String routeTaskOwner = null;
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String objectId = (String) programMap.get(SELECT_ID);
            DomainObject routeTask = DomainObject.newInstance(context, objectId);
            routeTaskOwner = routeTask.getInfo(context, SELECT_OWNER);
            String strContentObjType = routeTask.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_ROUTE_TASK + "].to.to[" + TigerConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.type");

            String type = MqlUtil.mqlCommand(context, "print user '" + routeTaskOwner + "' select type dump");

            // if owner is a group or role then get the members
            if ("group".equals(type) || "role".equals(type)) {

                if (TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION.equalsIgnoreCase(strContentObjType)) {

                    String strProgProjId = routeTask.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_ROUTE_TASK + "].to.to[" + TigerConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.to["
                            + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                    StringList slProgProjMember = getProgramProjectMembers(context, type, routeTaskOwner, strProgProjId);
                    String strChangeObjId = routeTask.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_ROUTE_TASK + "].to.to[" + TigerConstants.RELATIONSHIP_OBJECT_ROUTE + "].from.id");
                    DomainObject domMCA = new DomainObject(strChangeObjId);

                    // Get the Connected plant members of MCO
                    StringList slMCOPlantMembers = domMCA.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from["
                            + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");

                    // Get Plant Members connected To Program-Project of MCO
                    StringList slPlantMembersConnectedToProject = domMCA.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to["
                            + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ + "].to.name");

                    for (int i = 0; i < slProgProjMember.size(); i++) {
                        if ((slMCOPlantMembers.contains(slProgProjMember.get(i))) && (slPlantMembersConnectedToProject.contains(slProgProjMember.get(i)))) {
                            routeTaskOwnerList.add(slProgProjMember.get(i));
                        }
                    }
                } else {
                    routeTaskOwnerList = getGroupOrRoleMembers(context, type, routeTaskOwner);
                }
            } else {
                if (routeTaskOwner != null) {
                    routeTaskOwnerList.add(routeTaskOwner);
                }
            }
        } catch (Exception e) {
            throw e;
        }

        return (routeTaskOwnerList);
    }

    // TIGTK-7496:Rutuja Ekatpure:15/5/2017:End
    /**
     * This method is used to get the name of the route owner.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains map of values for the route task
     * @return StringList containing the name of the route owner.
     * @throws Exception
     *             if the operation fails
     * @since V6R2012x
     */
    public StringList getRouteOwner(Context context, String[] args) throws Exception {

        StringList routeOwnerList = new StringList();
        String routeOwner = null;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) programMap.get(SELECT_ID);
            DomainObject routeTask = DomainObject.newInstance(context, objectId);
            routeOwner = routeTask.getInfo(context, "from[" + RELATIONSHIP_ROUTE_TASK + "].to.owner");

        } catch (Exception e) {
            throw e;
        }

        if (routeOwner != null) {
            routeOwnerList.add(routeOwner);
        }

        return (routeOwnerList);
    }

    /**
     * This method is used to get the notification agent.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains map of values for the route task
     * @return String containing the name of the context user.
     * @throws Exception
     *             if the operation fails
     * @since V6R2012x
     */
    public String getNotificationAgent(Context context, String[] args) throws MatrixException {

        String agent = null;

        try {
            agent = emxMailUtil_mxJPO.getAgentName(context, new String[] {});
        } catch (Exception e) {
            throw (new MatrixException(e));
        }

        if (agent == null || agent.length() == 0) {
            agent = context.getUser();
        }

        return (agent);
    }

    /**
     * This method consolidates the given Notification Requests assuming these are similar notifications. For now it merges the Attachments.
     * @param mapSrcNotificationRequest
     *            Map of notification request which is to be merged
     * @param mapDestNotificationRequest
     *            Map of notification request in which to be merged. This will be modified as a result.
     * @return void
     * @since R212
     */
    protected void mergeSimilarNotification(Map mapSrcNotificationRequest, Map mapDestNotificationRequest, String[] fieldKeys, String[] fieldSeparators) {
        if (mapDestNotificationRequest != null && mapSrcNotificationRequest != null) {

            for (int i = 0, j = 0; i < fieldKeys.length && j < fieldSeparators.length; i++, j++) {

                String strDest = (String) mapDestNotificationRequest.get(fieldKeys[i]);
                String strSrc = (String) mapSrcNotificationRequest.get(fieldKeys[i]);

                if (strDest == null) {
                    strDest = "";
                }
                if (strSrc == null) {
                    strSrc = "";
                }

                if (strSrc.length() != 0) {
                    // if this is the body html then do something different
                    if (fieldKeys[i].equals(SELECT_ATTRIBUTE_BODY_HTML)) {
                        // use only the html portion of the destination string
                        strDest = lastSubString(strDest, "<html>", "</html>");

                        // compare table headers and use the longer
                        String strSrcHead = lastSubString(strSrc, "<thead>", "</thead>");
                        String strDestHead = lastSubString(strDest, "<thead>", "</thead>");
                        if (strSrcHead.length() > strDestHead.length()) {
                            int insertPos = strDest.indexOf("<thead>");
                            strDest = strDest.substring(0, insertPos) + strSrcHead + strDest.substring(insertPos + strDestHead.length());
                        }

                        // find the last </tr> in the destination string
                        int indexDest = strDest.lastIndexOf("</tr>");
                        if (indexDest != -1) {
                            indexDest += "</tr>".length();

                            // find the table row in the source message
                            String tableRow = lastSubString(strSrc, "<tr>", "</tr>");
                            if (tableRow != null) {
                                // insert into the destination table
                                strDest = strDest.substring(0, indexDest) + tableRow + strDest.substring(indexDest);
                            }
                        }
                    } else {
                        // if not the first message then append the separator
                        if (strDest.length() != 0) {
                            strDest += fieldSeparators[j];
                        }
                        strDest += strSrc;
                    }

                    mapDestNotificationRequest.put(fieldKeys[i], strDest);
                }
            }
        }
    }

    /**
     * Retrieves the last substring of the source wrapped by the begin and end strings. The begin and end strings are included in the result. Used to extract the last table row from a notification so
     * that it can be merged into another notification table.
     * @param source
     *            the source string
     * @param begin
     *            the beginning of the string to extract
     * @param end
     *            the end of the string to extract
     * @return String the resulting string, null if unable to find begin or end
     * @throws Exception
     *             if the operation fails
     * @since V6R2012x
     */
    private String lastSubString(String source, String begin, String end) {
        String result = null;

        if (source != null && source.length() > 0 && begin != null && begin.length() > 0 && end != null && end.length() > 0) {
            int beginIndex = source.lastIndexOf(begin);
            if (beginIndex != -1) {
                int endIndex = source.lastIndexOf(end);
                if (endIndex != -1) {
                    endIndex += end.length();
                    result = source.substring(beginIndex, endIndex);
                }
            }
        }

        return (result);
    }

    /**
     * Returns the list of members for the given group or role.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param groupOrRole
     *            contains either "group" or "role"
     * @param groupOrRoleName
     *            name of the group or role
     * @return StringList the list of members of the group or role
     * @throws Exception
     *             if the operation fails
     * @since V6R2012x
     */
    private StringList getGroupOrRoleMembers(Context context, String groupOrRole, String groupOrRoleName) throws Exception {

        StringList members = new StringList();

        if ("role".equalsIgnoreCase(groupOrRole)) {
            Role role = new Role(groupOrRoleName);
            role.open(context);
            UserItr itr = new UserItr(role.getAssignments(context));
            while (itr.next()) {
                if (itr.obj() instanceof matrix.db.Person) {
                    members.add(itr.obj().getName());
                }
            }
            role.close(context);
        } else if ("group".equalsIgnoreCase(groupOrRole)) {
            Group group = new Group(groupOrRoleName);
            group.open(context);
            UserItr itr = new UserItr(group.getAssignments(context));
            while (itr.next()) {
                if (itr.obj() instanceof matrix.db.Person) {
                    members.add(itr.obj().getName());
                }
            }
            group.close(context);
        }

        return (members);
    }

    // Addition for Tiger - PCM stream by SGS starts
    /**
     * This method is used to get the name of the Change Manager of Change Request.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains map of values for the route task
     * @return StringList containing the name of the route owner.
     * @throws Exception
     *             if the operation fails
     */
    public StringList getChangeManager(Context context, String[] args) throws Exception {

        StringList ChangeManagerList = new StringList();
        String strChangeManager = null;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap payloadMap = (HashMap) programMap.get("payload");
            String[] tableKeys = (String[]) payloadMap.get("tableRowKeys");
            String[] tableValues = (String[]) payloadMap.get("tableRowValues");

            for (int i = 0; i < tableKeys.length; i++) {
                if (tableKeys[i].equals("strChangeManagerName")) {
                    strChangeManager = tableValues[i];
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeManager: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

        if (strChangeManager != null) {
            ChangeManagerList.add(strChangeManager);
        }

        return (ChangeManagerList);
    }

    // Addition for Tiger - PCM stream by SGS ends

    // TIGTK-7496:Rutuja Ekatpure:15/5/2017:Start
    /**
     * @param context
     * @param groupOrRole
     * @param groupOrRoleName
     * @param strProgProjId
     * @return
     * @throws Exception
     */
    public StringList getProgramProjectMembers(Context context, String groupOrRole, String groupOrRoleName, String strProgProjId) throws Exception {

        StringList members = new StringList();
        try {
            DomainObject domProgramProjectObj = DomainObject.newInstance(context, strProgProjId);
            StringList objectSelect = new StringList(DomainConstants.SELECT_NAME);
            StringBuffer relWhere = new StringBuffer();
            relWhere.append("attribute[");
            relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
            relWhere.append("]");
            relWhere.append(" == '");
            // TIGTK-5890 : PTE : 4/7/2017 : START
            relWhere.append(groupOrRoleName);
            // TIGTK-5890 : PTE : 4/7/2017 : END
            relWhere.append("'");
            MapList mlPerson = getMembersFromProgram(context, domProgramProjectObj, objectSelect, DomainConstants.EMPTY_STRINGLIST, DomainConstants.EMPTY_STRING, relWhere.toString());

            if (!mlPerson.isEmpty()) {
                for (int i = 0; i < mlPerson.size(); i++) {
                    String strPersonName = (String) ((Map) mlPerson.get(i)).get(DomainConstants.SELECT_NAME);
                    members.addElement(strPersonName);
                }
            }
        } catch (Exception e) {
            logger.error("Error in getProgramProjectMembers: ", e);
        }
        return (members);
    }

    /**
     * @param context
     * @param domProgram
     * @param objectSelect
     * @param relSelect
     * @param objWhere
     * @param relWhere
     * @return
     * @throws Exception
     */
    public MapList getMembersFromProgram(Context context, DomainObject domProgram, StringList objectSelect, StringList relSelect, String objWhere, String relWhere) throws Exception {
        MapList mlMemberList = new MapList();
        try {
            mlMemberList = domProgram.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, DomainObject.TYPE_PERSON, objectSelect, relSelect, false, true, (short) 0, objWhere,
                    relWhere, 0);
        } catch (Exception ex) {
            logger.error("Error in getMembersFromProgram: ", ex);
        }
        return mlMemberList;
    }

    // TIGTK-7496:Rutuja Ekatpure:15/5/2017:End
}