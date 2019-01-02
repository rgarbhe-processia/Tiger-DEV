import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.common.Person;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipList;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.common.utils.TigerUtils;
import pss.constants.TigerConstants;

/**
 * @category UI Access Util
 * 
 *           <pre>
 * Class to define & handle all UI access related business cases
 *           </pre>
 * 
 * @author PSI
 * @since 2018 OCT 31
 */
public class PSS_CommonUIAccessUtil_mxJPO extends emxDomainObject_mxJPO {
    private static final Logger log = LoggerFactory.getLogger(PSS_CommonUIAccessUtil_mxJPO.class);

    private static final long serialVersionUID = 1L;

    public PSS_CommonUIAccessUtil_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * Checks whether the context user has access to the UI component for "Transfer Ownership"
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public boolean isTransferOwnershipAllowed(Context context, String[] args) throws Exception {
        log.debug(":::::::::: ENTER :: isTransferOwnershipAllowed ::::::::::");
        boolean bShowCommand = false;
        try {
            Map programMap = (Map) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            StringList slObjectSelectables = new StringList();
            slObjectSelectables.add(DomainConstants.SELECT_POLICY);
            slObjectSelectables.add(DomainConstants.SELECT_CURRENT);
            slObjectSelectables.add("project");
            slObjectSelectables.add("organization");
            // Part or CAD
            DomainObject doObject = DomainObject.newInstance(context, strObjectId);
            Map mpObjectDetails = doObject.getInfo(context, slObjectSelectables);
            if (!mpObjectDetails.isEmpty()) {
                String strPolicy = (String) mpObjectDetails.get(DomainConstants.SELECT_POLICY);
                String strCurrent = (String) mpObjectDetails.get(DomainConstants.SELECT_CURRENT);
                if (UIUtil.isNotNullAndNotEmpty((String) mpObjectDetails.get("project")) && UIUtil.isNotNullAndNotEmpty((String) mpObjectDetails.get("organization"))) {
                    if (TigerConstants.POLICY_PSS_ECPART.equals(strPolicy) && !TigerConstants.STATE_PART_OBSOLETE.equals(strCurrent)) {
                        bShowCommand = true;
                    } else if (TigerConstants.POLICY_STANDARDPART.equals(strPolicy) && !TigerConstants.STATE_STANDARDPART_OBSOLETE.equals(strCurrent)) {
                        bShowCommand = true;
                    } else if (TigerConstants.POLICY_PSS_CADOBJECT.equals(strPolicy) && TigerConstants.STATE_RELEASED_CAD_OBJECT.equals(strCurrent)) {
                        bShowCommand = true;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug(":::::::::: EXIT :: isTransferOwnershipAllowed ::::::::::");
        return bShowCommand;
    }

    /**
     * Checks whether the context user has access to make conection between Global & Local PP To do so, the user must login with any of the below Sec.Context roles,
     * 
     * <pre>
     *      1. Program Manager
     *      2. Global Administrator
     *      3. PLM Support Team
     * </pre>
     * 
     * TIGTK-17648 :: ALM-6241
     * @param context
     * @param args
     * @return bHasAccess
     * @throws Exception
     */
    public boolean checkForAccessToAddLocalPP(Context context, String[] args) throws Exception {
        log.debug(":::::::::: ENTER :: checkForAccessToAddLocalPP ::::::::::");
        boolean bHasAccess = false;
        try {
            String strLogginRole = (String) FrameworkUtil.split(context.getRole().substring(5), ".").get(0);
            StringList slAllowedRoles = new StringList(3);
            slAllowedRoles.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
            slAllowedRoles.add(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM);
            slAllowedRoles.add(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR);
            if (UIUtil.isNotNullAndNotEmpty(strLogginRole) && slAllowedRoles.contains(strLogginRole)) {
                bHasAccess = true;
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug(":::::::::: EXIT :: checkForAccessToAddLocalPP ::::::::::");
        return bHasAccess;
    }

    /**
     * checks whether the Part is eligible to show Change Policy command to user TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean checkForAccessToChangePolicy(Context context, String[] args) throws Exception {
        log.debug(":::::::::: ENTER :: checkForAccessToChangePolicy ::::::::::");
        boolean bHasAccess = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strPartOID = (String) programMap.get("objectId");
            String strExpression = "evaluate[relationship[" + TigerConstants.RELATIONSHIP_EBOM + "]==False " + " && relationship[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "]==False"
                    + " && relationship[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "]==False" + " && relationship[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "]==False"
                    + " && relationship[" + TigerConstants.RELATIONSHIP_ISSUE + "]==False" + " && relationship[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "]==False" + " && relationship["
                    + DomainConstants.RELATIONSHIP_ALTERNATE + "]==False" + " && relationship[" + PropertyUtil.getSchemaProperty(context, "relationship_EBOMSubstitute") + "]==False"
                    + " && relationship[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]==False]" + " && relationship[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "]==False]";
            DomainObject doPart = DomainObject.newInstance(context, strPartOID);
            String strEval = doPart.getInfo(context, strExpression);
            if (TigerConstants.STRING_TRUE.equalsIgnoreCase(strEval)) {
                bHasAccess = true;
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug(":::::::::: EXIT :: checkForAccessToChangePolicy ::::::::::");
        return bHasAccess;
    }

    /**
     * checks whether the Part is eligible to show EC to Standard Change Policy & Reclassify command to user TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean checkForAccessToECToStandardPolicyChange(Context context, String[] args) throws Exception {
        log.debug(":::::::::: ENTER :: checkForAccessToECToStandardPolicyChange ::::::::::");
        boolean bHasAccess = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strPartOID = (String) programMap.get("objectId");
            // PC05 - leaf level
            DomainObject doPart = DomainObject.newInstance(context, strPartOID);
            StringList slObjSelects = new StringList(5);
            slObjSelects.add(DomainConstants.SELECT_CURRENT);
            slObjSelects.add(DomainConstants.SELECT_POLICY);
            slObjSelects.add("next.policy");
            slObjSelects.add("next.id");
            slObjSelects.add("from[" + TigerConstants.RELATIONSHIP_EBOM + "]");
            Map mpPartInfoMap = doPart.getInfo(context, slObjSelects);
            String strEBOM = (String) mpPartInfoMap.get("from[" + TigerConstants.RELATIONSHIP_EBOM + "]");
            if (TigerConstants.STRING_FALSE.equalsIgnoreCase(strEBOM)) {
                String strCurrent = (String) mpPartInfoMap.get(DomainConstants.SELECT_CURRENT);
                if (TigerConstants.STATE_PART_RELEASE.equals(strCurrent)) {
                    // PC02 - For Released Part, system ensures, Higher non-cancelled revision/active CO does not exist on context "EC Part"
                    StringBuilder sbRelPattern = new StringBuilder(126);
                    sbRelPattern.append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.RELATIONSHIP_CHANGEACTION);
                    StringBuilder sbTypePattern = new StringBuilder(126);
                    sbTypePattern.append(TigerConstants.TYPE_CHANGEACTION).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.TYPE_PSS_CHANGEORDER);
                    StringBuilder sbPostStateMatchPattern = new StringBuilder(256);
                    sbPostStateMatchPattern.append(TigerConstants.STATE_PSS_CHANGEORDER_INWORK).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL);
                    Map<String, String> hmPatternMap = new HashMap<String, String>(1);
                    hmPatternMap.put(DomainConstants.SELECT_CURRENT, sbPostStateMatchPattern.toString());
                    MapList mlChangeOrder = doPart.getRelatedObjects(context, sbRelPattern.toString(), sbTypePattern.toString(), slObjSelects, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 2,
                            null, null, (short) 0, false, true, (short) 100, new Pattern(TigerConstants.TYPE_PSS_CHANGEORDER), null, hmPatternMap, null);
                    if (mlChangeOrder.isEmpty()) {
                        if (mpPartInfoMap.containsKey("next.id")) {
                            String strNextPartOID = (String) mpPartInfoMap.get("next.id");
                            if (UIUtil.isNotNullAndNotEmpty(strNextPartOID) && TigerConstants.POLICY_PSS_CANCELPART.equals((String) mpPartInfoMap.get("next.policy"))) {
                                bHasAccess = true;
                                DomainObject doNextRevPart = DomainObject.newInstance(context, strNextPartOID);
                                sbRelPattern = new StringBuilder(126);
                                sbRelPattern.append(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM).append(TigerConstants.SEPERATOR_COMMA).append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM)
                                        .append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.RELATIONSHIP_CHANGEACTION);
                                mlChangeOrder = doNextRevPart.getRelatedObjects(context, sbRelPattern.toString(), sbTypePattern.toString(), slObjSelects, DomainConstants.EMPTY_STRINGLIST, true, false,
                                        (short) 2, null, null, (short) 0, false, true, (short) 100, new Pattern(TigerConstants.TYPE_PSS_CHANGEORDER), null, hmPatternMap, null);
                                if (mlChangeOrder.isEmpty()) {
                                    // NO active CO connected on next rev of context Part
                                    bHasAccess = true;
                                } else {
                                    bHasAccess = false;
                                }
                            }
                        } else {
                            // NO higher revision of context Part
                            bHasAccess = true;
                        }
                    }
                } else if (TigerConstants.STATE_PART_OBSOLETE.equals(strCurrent)) {
                    // PC03 - For Obsolete Part, system ensures, Next revision of context Obsolete part is already gone through policy change
                    if (mpPartInfoMap.containsKey("next.policy")) {
                        String strCtxRevPolicy = (String) mpPartInfoMap.get(DomainConstants.SELECT_POLICY);
                        String strNextRevPolicy = (String) mpPartInfoMap.get("next.policy");
                        if (UIUtil.isNotNullAndNotEmpty(strNextRevPolicy) && !TigerConstants.POLICY_PSS_CANCELPART.equals(strNextRevPolicy) && !strCtxRevPolicy.equals(strNextRevPolicy)) {
                            bHasAccess = true;
                        }
                    } else {
                        // incase - only one revision available and that too obsolete (may not be the case right now)
                        bHasAccess = true;
                    }
                }

                // PC04 System have Standard Collaborative space for business unit decided using #UC1
                // This is not developed due to condradictory beween PC04 & FR001-->#S04-->#02
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug(":::::::::: EXIT :: checkForAccessToECToStandardPolicyChange ::::::::::");
        return bHasAccess;
    }

    /**
     * checks whether the Part is eligible to show Standard to EC Change Policy & Reclassify command to user TIGTK-14892 :: ALM-4106
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean checkForAccessToStandardToECPolicyChange(Context context, String[] args) throws Exception {
        log.debug(":::::::::: ENTER :: checkForAccessToStandardToECPolicyChange ::::::::::");
        boolean bHasAccess = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strPartOID = (String) programMap.get("objectId");
            DomainObject doPart = DomainObject.newInstance(context, strPartOID);
            // PC03 - Part selected for operation does not have any "Standard Part" connected as immediate parent
            StringList slEBOMStdParent = doPart.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_EBOM + "].from.policy");
            if (slEBOMStdParent.isEmpty() || !slEBOMStdParent.contains(TigerConstants.POLICY_STANDARDPART)) {
                StringList slObjSelects = new StringList(4);
                slObjSelects.add(DomainConstants.SELECT_CURRENT);
                slObjSelects.add(DomainConstants.SELECT_POLICY);
                slObjSelects.add("next.policy");
                slObjSelects.add("next.id");
                Map mpPartInfoMap = doPart.getInfo(context, slObjSelects);
                String strCurrent = (String) mpPartInfoMap.get(DomainConstants.SELECT_CURRENT);
                if (TigerConstants.STATE_PART_RELEASE.equals(strCurrent)) {
                    // PC05 - Higher non-cancelled revision /active CO does not exist on Released revision of "Standard Part"
                    StringBuilder sbRelPattern = new StringBuilder(126);
                    sbRelPattern.append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.RELATIONSHIP_CHANGEACTION);
                    StringBuilder sbTypePattern = new StringBuilder(126);
                    sbTypePattern.append(TigerConstants.TYPE_CHANGEACTION).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.TYPE_PSS_CHANGEORDER);
                    StringBuilder sbPostStateMatchPattern = new StringBuilder(256);
                    sbPostStateMatchPattern.append(TigerConstants.STATE_PSS_CHANGEORDER_INWORK).append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL);
                    Map<String, String> hmPatternMap = new HashMap<String, String>(1);
                    hmPatternMap.put(DomainConstants.SELECT_CURRENT, sbPostStateMatchPattern.toString());
                    MapList mlChangeOrder = doPart.getRelatedObjects(context, sbRelPattern.toString(), sbTypePattern.toString(), slObjSelects, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 2,
                            null, null, (short) 0, false, true, (short) 100, new Pattern(TigerConstants.TYPE_PSS_CHANGEORDER), null, hmPatternMap, null);
                    if (mlChangeOrder.isEmpty()) {
                        if (mpPartInfoMap.containsKey("next.id")) {
                            String strNextPartOID = (String) mpPartInfoMap.get("next.id");
                            if (UIUtil.isNotNullAndNotEmpty(strNextPartOID) && TigerConstants.POLICY_PSS_CANCELPART.equals((String) mpPartInfoMap.get("next.policy"))) {
                                bHasAccess = true;
                                DomainObject doNextRevPart = DomainObject.newInstance(context, strNextPartOID);
                                sbRelPattern = new StringBuilder(126);
                                sbRelPattern.append(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM).append(TigerConstants.SEPERATOR_COMMA).append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM)
                                        .append(TigerConstants.SEPERATOR_COMMA).append(TigerConstants.RELATIONSHIP_CHANGEACTION);
                                mlChangeOrder = doNextRevPart.getRelatedObjects(context, sbRelPattern.toString(), sbTypePattern.toString(), slObjSelects, DomainConstants.EMPTY_STRINGLIST, true, false,
                                        (short) 2, null, null, (short) 0, false, true, (short) 100, new Pattern(TigerConstants.TYPE_PSS_CHANGEORDER), null, hmPatternMap, null);
                                if (mlChangeOrder.isEmpty()) {
                                    // NO active CO connected on next rev of context Part
                                    bHasAccess = true;
                                } else {
                                    bHasAccess = false;
                                }
                            }
                        } else {
                            // NO higher revision of context Part
                            bHasAccess = true;
                        }
                    } else {
                        // NO active CO connected on context Part
                        bHasAccess = true;
                    }
                } else if (TigerConstants.STATE_PART_OBSOLETE.equals(strCurrent)) {
                    // PC02 - For Obsolete Part, system ensures, Next revision of context Obsolete part is already gone through policy change
                    if (mpPartInfoMap.containsKey("next.policy")) {
                        String strCtxRevPolicy = (String) mpPartInfoMap.get(DomainConstants.SELECT_POLICY);
                        String strNextRevPolicy = (String) mpPartInfoMap.get("next.policy");
                        if (UIUtil.isNotNullAndNotEmpty(strNextRevPolicy) && !TigerConstants.POLICY_PSS_CANCELPART.equals(strNextRevPolicy) && !strCtxRevPolicy.equals(strNextRevPolicy)) {
                            bHasAccess = true;
                        }
                    } else {
                        // incase - only one revision available and that too obsolete (may not be the case right now)
                        bHasAccess = true;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug(":::::::::: EXIT :: checkForAccessToStandardToECPolicyChange ::::::::::");
        return bHasAccess;
    }

    /**
     * checks whether the Part is eligible to Change Policy
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean hasAccessForPolicyChange(Context context, String[] args) throws Exception {
        log.debug(":::::::::: ENTER :: hasAccessForPolicyChange ::::::::::");
        boolean bHasAccess = false;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strPartOID = (String) programMap.get("objectId");
            String strChangeTo = (String) ((Map) (programMap.get("SETTINGS"))).get("changeTo");
            ArrayList<String> alAllowedRoles = new ArrayList<String>(2);
            alAllowedRoles.add(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR);
            alAllowedRoles.add(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM);
            DomainObject doPart = DomainObject.newInstance(context, strPartOID);
            StringList slSelectables = new StringList(2);
            slSelectables.add(DomainConstants.SELECT_OWNER);
            slSelectables.add(DomainConstants.SELECT_POLICY);
            Map mpInfo = doPart.getInfo(context, slSelectables);
            String strOwner = (String) mpInfo.get(DomainConstants.SELECT_OWNER);
            String strPolicy = (String) mpInfo.get(DomainConstants.SELECT_POLICY);
            String strLoggedInPerson = TigerUtils.getLoggedInUserName(context);
            String strLoggedInRole = (String) FrameworkUtil.split(context.getRole().substring(5), ".").get(0);

            if (UIUtil.isNotNullAndNotEmpty(strChangeTo) && UIUtil.isNotNullAndNotEmpty(strPolicy) && UIUtil.isNotNullAndNotEmpty(strLoggedInRole) && UIUtil.isNotNullAndNotEmpty(strOwner)
                    && UIUtil.isNotNullAndNotEmpty(strLoggedInPerson)) {
                if ((TigerConstants.POLICY_DEVELOPMENTPART.equals(strPolicy) || TigerConstants.POLICY_PSS_DEVELOPMENTPART.equals(strPolicy)) && "EC".equals(strChangeTo)) {
                    if (alAllowedRoles.contains(strLoggedInRole) || strOwner.equals(strLoggedInPerson)) {
                        bHasAccess = true;
                    }
                } else if ((TigerConstants.POLICY_DEVELOPMENTPART.equals(strPolicy) || TigerConstants.POLICY_PSS_DEVELOPMENTPART.equals(strPolicy)) && "Standard".equals(strChangeTo)) {
                    if (alAllowedRoles.contains(strLoggedInRole) || (TigerConstants.ROLE_PSS_GTS_ENGINEER.equals(strLoggedInRole) && strOwner.equals(strLoggedInPerson))) {
                        bHasAccess = true;
                    }
                } else if (TigerConstants.POLICY_PSS_ECPART.equals(strPolicy) && "Development".equals(strChangeTo)) {
                    if (alAllowedRoles.contains(strLoggedInRole) || strOwner.equals(strLoggedInPerson)) {
                        bHasAccess = true;
                    }
                } else if (TigerConstants.POLICY_PSS_ECPART.equals(strPolicy) && "Standard".equals(strChangeTo)) {
                    if (alAllowedRoles.contains(strLoggedInRole) || (TigerConstants.ROLE_PSS_GTS_ENGINEER.equals(strLoggedInRole) && strOwner.equals(strLoggedInPerson))) {
                        bHasAccess = true;
                    }
                } else if (TigerConstants.POLICY_STANDARDPART.equals(strPolicy) && "Development".equals(strChangeTo)) {
                    if (alAllowedRoles.contains(strLoggedInRole) || (TigerConstants.ROLE_PSS_GTS_ENGINEER.equals(strLoggedInRole) && strOwner.equals(strLoggedInPerson))) {
                        bHasAccess = true;
                    }
                } else if (TigerConstants.POLICY_STANDARDPART.equals(strPolicy) && "EC".equals(strChangeTo)) {
                    if (alAllowedRoles.contains(strLoggedInRole) || (TigerConstants.ROLE_PSS_GTS_ENGINEER.equals(strLoggedInRole) && strOwner.equals(strLoggedInPerson))) {
                        bHasAccess = true;
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            throw new Exception(e.getLocalizedMessage(), e);
        }
        log.debug(":::::::::: EXIT :: hasAccessForPolicyChange ::::::::::");
        return bHasAccess;
    }
    
    /**
     * Shows User Type field is editable or not  based on condition PSS_ConnectedMembers link
     * @param context
     * @param args
     * @return true or false
     * @throws Exception
     */
    // ALM 3311 VISHAL :START
    public boolean showUserTypeEditable(Context context, String[] args) throws Exception{
       String   strLoggedInPerson = PersonUtil.getPersonObjectID(context);
       DomainObject doPerson = DomainObject.newInstance(context, strLoggedInPerson);
       String strPSSConnected =   doPerson.getInfo(context,"to[PSS_ConnectedMembers]");
       
       if(strPSSConnected.equalsIgnoreCase(TigerConstants.STRING_TRUE)){ 
           return true;
       }else{
           return false;
       }
  
    }

    public boolean showBGTypeEditable(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strBusinessUnitId = (String) programMap.get("objectId");
        DomainObject doBusinessUnit = DomainObject.newInstance(context, strBusinessUnitId);
        String strPSSConnected = doBusinessUnit.getInfo(context, "to[PSS_ConnectedMembers]");
        if (strPSSConnected.equalsIgnoreCase(TigerConstants.STRING_TRUE)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean showBGTypeReadable(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strBusinessUnitId = (String) programMap.get("objectId");
        DomainObject doBusinessUnit = DomainObject.newInstance(context, strBusinessUnitId);
        String strPSSConnected = doBusinessUnit.getInfo(context, "to[PSS_ConnectedMembers]");
        if (strPSSConnected.equalsIgnoreCase(TigerConstants.STRING_TRUE)) {
            return true;
        } else {
            return false;
        }
    }

    // ALM 3311 VISHAL :END
    
    
    
}
