/**
 * @commonUtil_mxJPO
 * @Developed for PCM Phase 1(TS_151, TS_154 and TS_155)
 * @author Harika Varanasi : SteepGraph
 */

package pss.ecm.ui;

import java.util.Map;

import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class CommonUtil_mxJPO {

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CommonUtil_mxJPO.class);

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END
    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @author Harika Varanasi : SteepGraph
     */
    public CommonUtil_mxJPO(Context context, String[] args) throws Exception {

    }

    /**
     * getProgramProjectTeamMembersForChange
     * @param context
     * @param strChangeID
     * @param slRolesList
     * @param onlyLead
     * @return
     * @throws Exception
     * @author Harika Varanasi : SteepGraph
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public StringList getProgramProjectTeamMembersForChange(Context context, String strChangeID, StringList slRolesList, boolean onlyLead) throws Exception {
        StringList slProjectTeamMembers = new StringList();
        try {
            DomainObject domChangeObject = DomainObject.newInstance(context);
            pss.ecm.notification.CommonNotificationUtil_mxJPO commonNotObj = new pss.ecm.notification.CommonNotificationUtil_mxJPO(context);
            if (UIUtil.isNotNullAndNotEmpty(strChangeID)) {
                domChangeObject.setId(strChangeID);
                String strWhere = "";
                String strRolesWhere = "";
                String strPositionWhere = "";

                String strMapSelectProjectTeamMembers = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                        .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append(strWhere).append("].to.name").toString();
                if (slRolesList != null && (!slRolesList.isEmpty())) {
                    strRolesWhere = new StringBuilder("(attribute[").append(TigerConstants.ATTRIBUTE_PSS_ROLE).append("] matchlist \"").append(FrameworkUtil.join(slRolesList, ",")).append("\" \",\"")
                            .append(")").toString();
                }

                if (onlyLead) {
                    strPositionWhere = new StringBuilder("(attribute[").append(TigerConstants.ATTRIBUTE_PSS_POSITION).append("]==\"Lead\")").toString();
                }

                if (UIUtil.isNotNullAndNotEmpty(strRolesWhere) && UIUtil.isNotNullAndNotEmpty(strPositionWhere)) {
                    strWhere = new StringBuilder("|(").append(strRolesWhere).append("&&").append(strPositionWhere).append(")").toString();
                } else if (UIUtil.isNotNullAndNotEmpty(strRolesWhere)) {
                    strWhere = "|" + strRolesWhere;
                } else if (UIUtil.isNotNullAndNotEmpty(strPositionWhere)) {
                    strWhere = "|" + strPositionWhere;
                } else {
                    strWhere = "";
                }

                String strSelectProjectTeamMembers = new StringBuilder("to[").append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA).append("].from.from[")
                        .append(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS).append(strWhere).append("].to.name").toString();

                StringList slSelects = new StringList(strSelectProjectTeamMembers);
                // TIGTK-6104 | 03/04/2017 |Harika Varanasi - SteepGraph : Starts
                DomainObject.MULTI_VALUE_LIST.add(strMapSelectProjectTeamMembers);
                // TIGTK-6104 | 03/04/2017 |Harika Varanasi - SteepGraph : Ends
                Map objectMap = domChangeObject.getInfo(context, slSelects);
                slProjectTeamMembers = commonNotObj.getStringListFromMap(context, objectMap, strMapSelectProjectTeamMembers);

            }

        } catch (Exception ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getProgramProjectTeamMembersForChange: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        return slProjectTeamMembers;
    }

}