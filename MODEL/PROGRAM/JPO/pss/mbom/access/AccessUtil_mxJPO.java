package pss.mbom.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

@SuppressWarnings("deprecation")
public class AccessUtil_mxJPO {

    // TIGTK-5405 - 06-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AccessUtil_mxJPO.class);
    // TIGTK-5405 - 06-04-2017 - VB - END

    public static void flushAndCloseSession(PLMCoreModelerSession plmSession) {
        try {
            plmSession.flushSession();
        } catch (Exception e) {
        }

        try {
            plmSession.closeSession(true);
        } catch (Exception e) {
        }
    }

    /**
     * This method is for checkPlntConnection()
     * @param context
     * @param args
     * @throws Exception
     */
    // Plants - NOTE : Faurecia will implement their own links to Plants, directly in ${CLASS:FRCMBOMProg}, in replacement of these methods.
    public static List<String> getPlantsAttachedToMBOMReference(Context context, PLMCoreModelerSession plmSession, String instPID) throws Exception {
        // No refactoring to do : R&D will not provide an API, because Faurecia will use ENOVIA plant objects, with their own links.
        List<String> returnList = new ArrayList<>();
        StringList lRsc = new StringList();
        String sAssemblyPID = DomainObject.EMPTY_STRING;
        try {
            if (UIUtil.isNotNullAndNotEmpty(instPID))
                sAssemblyPID = MqlUtil.mqlCommand(context, "print bus " + instPID + " select physicalid dump |", false, false);
            if (UIUtil.isNotNullAndNotEmpty(sAssemblyPID)) {
                String listPathIDStr = MqlUtil.mqlCommand(context, "query path type SemanticRelation containing " + sAssemblyPID + " select id dump |", false, false);
                if (!"".equals(listPathIDStr)) {
                    String[] listPathID = listPathIDStr.split("\n");
                    for (String pathDesc : listPathID) {
                        String[] aPathDesc = pathDesc.split("\\|");
                        if (null != aPathDesc && 1 < aPathDesc.length) {
                            String pathID = aPathDesc[1];
                            String pathSemantics = MqlUtil.mqlCommand(context, "print path " + pathID + " select attribute[RoleSemantics].value dump |", false, false);
                            if (TigerConstants.PLM_IMPLEMENTLINK_TARGETREFERENCE3.equalsIgnoreCase(pathSemantics)) {
                                String targetPhysId = MqlUtil.mqlCommand(context, "print path " + pathID + " select owner.to[" + TigerConstants.RELATIONSHIP_VOWNER + "].from.physicalid dump |", false,
                                        false);
                                if (UIUtil.isNotNullAndNotEmpty(targetPhysId))
                                    lRsc.addElement(targetPhysId);
                            }
                        }
                    }
                }
            }

            for (Object rscPIDObj : lRsc) {
                returnList.add((String) rscPIDObj);
            }
        } catch (Exception e) {
            // TIGTK-5405 - 04-04-2017 - VB - START
            if (logger.isInfoEnabled())
                logger.info("Error in getPlantsAttachedToMBOMReference: ", e);
            // TIGTK-5405 - 04-04-2017 - VB - END
        }
        return returnList;
    }

    /**
     * This method is used to check that plant is connected to MBOM or not table
     * @param context
     * @param args
     * @throws Exception
     */
    public boolean checkPlantConnection(Context context, String[] args) throws Exception {

        boolean returnCheck = false;
        try {
            PLMCoreModelerSession plmSession = null;
            ContextUtil.startTransaction(context, false);
            context.setApplication("VPLM");
            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");

            String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strObjectId);
            if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                flushAndCloseSession(plmSession);
                ContextUtil.commitTransaction(context);
                returnCheck = false;

            } else {
                flushAndCloseSession(plmSession);
                ContextUtil.commitTransaction(context);
                returnCheck = true;

            }
        } catch (Exception e) {
            // TIGTK-5405 - 06-04-2017 - VB - START
            logger.error("Error in checkPlantConnection: ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
        }
        return returnCheck;
    }
}