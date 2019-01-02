package pss.e2e.ui;

import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.User;
import matrix.db.UserItr;

import java.util.HashMap;
import java.util.Map;

import pss.constants.TigerConstants;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.MqlUtil;

public class BGNotifications_mxJPO {

    // TIGTK-5405 - 11-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BGNotifications_mxJPO.class);
    // TIGTK-5405 - 11-04-2017 - VB - END

    /**
     * @description: This method is used to set the values on Business Unit.
     * @param context
     * @param args
     * @return Nothing
     * @throws Exception
     */

    public void setNotificationsValuesonBG(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strBusUnitObjId = (String) programMap.get("objectId");
        DomainObject domBusUnitObj = new DomainObject(strBusUnitObjId);

        Map mAttributeMap = new HashMap();
        boolean isContextPushed = false;
        try {
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isContextPushed = true;
            String strOpenedFrequency = (String) programMap.get("OpenedFrequency");
            mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2ENOTIFICATIONOPENEDCHGSFREQUENCY, strOpenedFrequency);

            String strDelayedFrequency = (String) programMap.get("DelayedFrequency");
            mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2ENOTIFICATIONDELAYEDCHGSFREQUENCY, strDelayedFrequency);

            String strRemainderFrequency = (String) programMap.get("RemainderFrequency");
            mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2ENOTIFICATIONREMAINDERFREQUENCY, strRemainderFrequency);

            String strcrduration = (String) programMap.get("crduration");
            mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2ECRDURATIONATSUBMITSTATE, strcrduration);

            String strcnduration = (String) programMap.get("cnduration");
            mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2EOPENEDCNDURATION, strcnduration);

            String strcoduration = (String) programMap.get("coduration");
            mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2ECODURATIONATPREPARESTATE, strcoduration);

            String strmcoduration = (String) programMap.get("mcoduration");
            mAttributeMap.put(TigerConstants.ATTRIBUTE_PSS_E2EMCODURATIONATPREPARESTATE, strmcoduration);

            domBusUnitObj.setAttributeValues(context, mAttributeMap);
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in setNotificationsValuesonBG: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }

    }

    /**
     * @description: This method is used to check the access on the End To End Command in Category Menu of the Business Unit
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public boolean checkAccessOnBG(Context context, String args[]) throws Exception {
        String strRoleName = "";
        boolean access = false;
        try {
            String strUser = context.getUser();
            matrix.db.Person person = new matrix.db.Person(strUser);
            person.open(context);
            UserItr userItr = new UserItr(person.getAssignments(context));
            while (userItr.next()) {
                User userObj = userItr.obj();
                if (userObj instanceof matrix.db.Role) {
                    strRoleName = userObj.getName();
                    if (strRoleName.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || (strRoleName.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM))
                            || (strRoleName.equalsIgnoreCase(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR))) {
                        access = true;
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in checkAccessOnBG: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        if (access == true) {
            return true;
        }
        return false;
    }
}
