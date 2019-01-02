package pss.uls.access;

import java.util.Map;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class ULSAccessUtil_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ULSAccessUtil_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    /**
     * This method checks create access for Program Project allow create access to specific Business Groups
     * @param context
     * @param args
     * @throws Exception
     */
    public boolean checkValidCSForCreateProgProj(Context context, String[] args) throws Exception {
        boolean isAccessible = false;
        try {
            // get the allowed Collaborative space for Program Project
            String strAllowedBusinessGroup = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), "emxFramework.ProgramProject.AllowedCS");

            // get Business group of current user
            String strBusinessGroupOfCurrentUser = PersonUtil.getDefaultProject(context, context.getUser());

            // allow access to user if the current Business Group is in allowed
            // business group list
            if (UIUtil.isNotNullAndNotEmpty(strAllowedBusinessGroup) && strAllowedBusinessGroup.contains(strBusinessGroupOfCurrentUser)) {
                isAccessible = true;
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkValidCSForCreateProgProj: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return isAccessible;
    }

    /**
     * Access Function for creation of Program-Project under another Parent Program-Project
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    @SuppressWarnings("rawtypes")
    public boolean checkParentChildRulesForProgProj(Context context, String[] args) throws Exception {
        final String PROJECT = "Project";
        boolean showCommand = true;
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");

            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            String strParentAttrVal = domainObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_PROGRAMPROJECT + "]");

            if (PROJECT.equals(strParentAttrVal)) {

                showCommand = false;
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkParentChildRulesForProgProj: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return showCommand;
    }

    /**
     * This Method is for getting Common data for all tables
     * @param context
     * @param args
     * @throws Exception
     */

    public StringList getULSRelationshipConstants() throws Exception {
        try {
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            slObjSelectStmts.addElement(DomainConstants.SELECT_CURRENT);
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            slObjSelectStmts.addElement(DomainConstants.SELECT_DESCRIPTION);
            slObjSelectStmts.addElement(DomainConstants.SELECT_TYPE);
            return slObjSelectStmts;
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getULSRelationshipConstants: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }

    }

    /**
     * Access function : Return true if Change Request is not connected to Program-Project else return false
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Priyanka Salunke
     * @since 28/10/2016
     */
    @SuppressWarnings("rawtypes")
    public boolean checkProgramProjectHasCRorNot(Context context, String[] args) throws Exception {
        boolean showMenu = true;
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strProgramProjectId = (String) programMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectId)) {
                DomainObject domProgramProject = DomainObject.newInstance(context, strProgramProjectId);

                // Relationship pattern
                Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);
                // Type pattern
                Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

                // Get connected CR of Program Project
                MapList mlConnectedCRList = domProgramProject.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                        typePattern.getPattern(), // object pattern
                        new StringList(DomainObject.SELECT_ID), // object selects
                        null, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        null, null, null, null, null);

                if (mlConnectedCRList.size() > 0) {

                    showMenu = false;
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkProgramProjectHasCRorNot: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return showMenu;
    }

    // End of method : checkProgramProjectHasCRorNot

    /**
     * Access function : Platform Field will not be displayed if creating Program-Project from context of Platform
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @author Suchit Gangurde
     * @Date: 14/12/2016
     * @For TIGTK-3798
     */

    @SuppressWarnings("rawtypes")
    public boolean showPlatformForCreateProgProj(Context context, String[] args) throws Exception {
        boolean showPlatformField = true;

        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strPlatformId = (String) programMap.get("objectId");
            DomainObject domPlatformObj = DomainObject.newInstance(context, strPlatformId);
            String strTypeName = domPlatformObj.getInfo(context, DomainConstants.SELECT_TYPE);

            if (strTypeName.equalsIgnoreCase(TigerConstants.TYPE_PSS_PLATFORM)) {
                showPlatformField = false;
            }

        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in showPlatformForCreateProgProj: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }

        return showPlatformField;
    }
}