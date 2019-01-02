import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import com.dassault_systemes.enovia.enterprisechangemgt.admin.ECMAdmin;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrder;
import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeOrderUI;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.matrixone.MCADIntegration.server.beans.IEFSimpleConfigObject;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.common.Route;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MailUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.StringUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Access;
import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.FileItr;
import matrix.db.FileList;
import matrix.db.JPO;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.db.RelationshipType;
import matrix.db.Signature;
import matrix.db.SignatureList;
import matrix.db.User;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.common.utils.TigerUtils;
import pss.constants.TigerConstants;

public class PSS_enoECMChangeOrder_mxJPO extends emxDomainObject_mxJPO {

    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_enoECMChangeOrder_mxJPO.class);

    // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - END
    // PCM TIGTK-10239: 4/10/2017 : KWagh : START
    private ChangeOrderUI changeOrderUI = null;

    private ChangeOrder changeOrder = null;

    private static final String INFO_TYPE_ACTIVATED_TASK = "activatedTask";

    // TIGTK-11653 :Start
    Map<String, String> mpPartName = new HashMap<String, String>();

    // TIGTK-11653 :End
    StringBuffer sbCAType = new StringBuffer();

    Map<String, String> mpForChildParentConnect = new HashMap<String, String>();

    Map<String, String> mpForConnectDocs = new HashMap<String, String>();

    Map<Integer, String> mpForConnectCADToPart = new HashMap<Integer, String>();

    StringList slForConnectCADToPartonReplace = new StringList();

    StringList slForDisconnectPSfromPart = new StringList();

    StringList slForObsolescencePart = new StringList();

    StringList slForRevisedPart = new StringList();

    StringList slForReplacedorClonedItemsofCurrentCO = new StringList();

    StringList slForCurrentCOAffectedItems = new StringList();

    int nCloneCADCount = 0;

    // PCM TIGTK-10239: 4/10/2017 : KWagh : END
    /**
     * Default Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since Ecm R211
     */
    public PSS_enoECMChangeOrder_mxJPO(Context context, String[] args) throws Exception {

        super(context, args);
        changeOrder = new ChangeOrder();
        // PCM TIGTK-10239: 4/10/2017 : KWagh : START
        changeOrderUI = new ChangeOrderUI();
        // PCM TIGTK-10239: 4/10/2017 : KWagh : END
    }

    public static final String STR_TYPE_ALL_CAD = DomainConstants.TYPE_CAD_DRAWING + "," + DomainConstants.TYPE_CAD_MODEL + "," + DomainConstants.TYPE_DRAWINGPRINT + ",PSS_CATPart";

    // PCM TIGTK-3032 & TIGTK-2825 | 13/09/16 : Ketaki Wagh : Start

    // PCM[2.0]:[JIRA][TIGTK-6856]:Trigger on CA to create the Approval Route for CA :31/7/2017:Pranjali Tupe:START
    private static final Map<String, String> mapProgramProjectRouteTemplateMapping = new HashMap<String, String>();

    static {
        mapProgramProjectRouteTemplateMapping.put(TigerConstants.RANGE_COMMERCIAL_UPDATE, TigerConstants.RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONCO);
        mapProgramProjectRouteTemplateMapping.put(TigerConstants.RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION, TigerConstants.RANGE_APPROVAL_LIST_FORPROTOTYPEONCO);
        mapProgramProjectRouteTemplateMapping.put(TigerConstants.RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION, TigerConstants.RANGE_APPROVAL_LIST_FORSERIALLAUNCHONCO);
        mapProgramProjectRouteTemplateMapping.put(TigerConstants.RANGE_DESIGN_STUDY, TigerConstants.RANGE_APPROVAL_LIST_FORDESIGNSTUDYONCO);
        mapProgramProjectRouteTemplateMapping.put(TigerConstants.RANGE_OTHER, TigerConstants.RANGE_APPROVAL_LIST_FOROTHERPARTSONCO);
        mapProgramProjectRouteTemplateMapping.put(TigerConstants.RANGE_Acquisition, TigerConstants.RANGE_APPROVAL_LIST_FORAcquisitionONCO);
    }
    // PCM[2.0]:[JIRA][TIGTK-6856]:Trigger on CA to start/resume the Approval Route for CA :31/7/2017:Pranjali Tupe:END

    /**
     * connect Chnage manager and change order object
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public DomainRelationship connectChangeManager(Context context, String[] args) throws Exception {

        try {
            // unpacking the Arguments from variable args
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            return connect(context, paramMap, ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR);
        }

        catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectChangeManager: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    private DomainRelationship connect(Context context, HashMap paramMap, String targetRelName) throws Exception {

        try {
            String objectId = (String) paramMap.get(ChangeConstants.OBJECT_ID);
            changeOrder.setId(objectId);
            return changeOrder.connect(context, paramMap, targetRelName, true);
        }

        catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connect: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // PCM RFC-074 : 3/10/2016 : KWagh : Start
    /**
     * Method to check if the Affected Item is already connected to any Change Object which is not Completed. If it is connected, block the addition and throw a Notice to the user.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param String
     *            [] args.
     * @return true/false
     * @throws Exception
     *             if the operation fails
     * @since ECM R211
     */
    public int checkIfAffectedItemAlreadyConnectedToChangeObject(Context context, String args[]) throws Exception {

        if (args == null || args.length < 10) {
            throw new IllegalArgumentException();
        }

        int isError = 0;
        String strMessage = "";
        try {
            StringList slAllowedStates = new StringList();
            String strToObjectId = args[1];
            String strNewRCValue = args[2];

            String strFromType = args[8];
            String strFromObjectId = args[9];
            // TIGTK-17751 : Start
            DomainObject domObjCA = DomainObject.newInstance(context, strFromObjectId);
            String idCOCRConnectedToContextCA = domObjCA.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
            // TIGTK-17751 : End

            DomainObject domObj = DomainObject.newInstance(context, strToObjectId);
            // String strAffectedItemPolicy = domObj.getInfo(context, DomainConstants.SELECT_POLICY);

            Map mRequestedChangeMapping = new HashMap();
            StringList slInWorkObjects = new StringList();
            slInWorkObjects.add(TigerConstants.STATE_PSS_CHANGEORDER_INWORK);
            slInWorkObjects.add(TigerConstants.STATE_PSS_ECPART_PRELIMINARY);
            slInWorkObjects.add(TigerConstants.STATE_PART_REVIEW);
            slInWorkObjects.add(TigerConstants.STATE_PSS_CR_CREATE);
            slInWorkObjects.add(TigerConstants.STATE_DEVELOPMENTPART_PEERREVIEW);
            // TIGTK-6849:PKH:Start
            slInWorkObjects.add(TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED);
            // TIGTK-6849:PKH:End

            StringList slReleased = new StringList();
            slReleased.add("Release");
            slReleased.add("Released");
            // PCM: TIGTK-8082: 24/05/2017: TS :START
            slReleased.add("Complete");
            // PCM: TIGTK-8082: 24/05/2017: TS :END
            mRequestedChangeMapping.put(ChangeConstants.FOR_RELEASE, slInWorkObjects);
            mRequestedChangeMapping.put(ChangeConstants.FOR_REVISE, slReleased);
            mRequestedChangeMapping.put(TigerConstants.FOR_CLONE, slReleased);
            mRequestedChangeMapping.put(TigerConstants.FOR_REPLACE, slReleased);
            mRequestedChangeMapping.put(ChangeConstants.FOR_OBSOLESCENCE, slReleased);

            if (strFromType.equalsIgnoreCase(ChangeConstants.TYPE_CHANGE_ACTION)) {

                StringList slAISelectable = new StringList();
                slAISelectable.add(DomainConstants.SELECT_ID);
                slAISelectable.add(DomainConstants.SELECT_CURRENT);

                MapList mlchangeAction = domObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, ChangeConstants.TYPE_CHANGE_ACTION, slAISelectable, null, true, false,
                        (short) 1, null, null, 0);
                MapList mlImplementedChangeAction = domObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, ChangeConstants.TYPE_CHANGE_ACTION, slAISelectable, null, true,
                        false, (short) 1, null, null, 0);

                // Change Action objects
                if (!mlchangeAction.isEmpty()) {
                    int mlCount = mlchangeAction.size();
                    for (int i = 0; i < mlCount; i++) {

                        Map mChangeAction = (Map) mlchangeAction.get(i);
                        String strCAID = (String) mChangeAction.get(DomainConstants.SELECT_ID);
                        String strCACurrent = (String) mChangeAction.get(DomainConstants.SELECT_CURRENT);

                        if (!strCAID.equalsIgnoreCase(strFromObjectId)) {

                            if ((strCACurrent.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_PENDING)) || (strCACurrent.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_INWORK))
                                    || (strCACurrent.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_INAPPROVAL))) {
                                String strUpdatedMessage = (String) EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), args[4]);
                                String strAffectedItemName = (String) domObj.getInfo(context, DomainConstants.SELECT_NAME);
                                strUpdatedMessage = strUpdatedMessage.replace("$<NAME>", strAffectedItemName);
                                MqlUtil.mqlCommand(context, "notice $1", strUpdatedMessage);
                                return 1;
                            }
                        }
                    }
                }

                if (!mlImplementedChangeAction.isEmpty()) {
                    int mlCount = mlImplementedChangeAction.size();
                    for (int i = 0; i < mlCount; i++) {

                        Map mChangeAction = (Map) mlImplementedChangeAction.get(i);
                        String strCAID = (String) mChangeAction.get(DomainConstants.SELECT_ID);
                        String strCACurrent = (String) mChangeAction.get(DomainConstants.SELECT_CURRENT);

                        if (!strCAID.equalsIgnoreCase(strFromObjectId)) {

                            if ((strCACurrent.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_PENDING)) || (strCACurrent.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_INWORK))
                                    || (strCACurrent.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_INAPPROVAL))) {
                                String strUpdatedMessage = (String) EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), args[4]);
                                String strAffectedItemName = (String) domObj.getInfo(context, DomainConstants.SELECT_NAME);
                                strUpdatedMessage = strUpdatedMessage.replace("$<NAME>", strAffectedItemName);
                                MqlUtil.mqlCommand(context, "notice $1", strUpdatedMessage);
                                return 1;
                            }
                        }
                    }
                }

            }

            // requested change value for released object should be for revise,for Clone,For Replace:RutujaE :30/09/2016:Start

            StringList slObjSelect = new StringList();
            slObjSelect.add(SELECT_CURRENT);
            slObjSelect.add(SELECT_TYPE);
            slObjSelect.add(SELECT_POLICY);
            slObjSelect.add(SELECT_ID);

            Map mapTemp = domObj.getInfo(context, slObjSelect);
            String strObjState = (String) mapTemp.get(SELECT_CURRENT);
            String strPolicy = (String) mapTemp.get(SELECT_POLICY);
            String strObjectId = (String) mapTemp.get(SELECT_ID);
            StringList slLegacyAllowed = new StringList();
            slLegacyAllowed.add(ChangeConstants.FOR_RELEASE);
            slLegacyAllowed.add(ChangeConstants.FOR_OBSOLESCENCE);
            slLegacyAllowed.add(ChangeConstants.FOR_NONE);

            if (strNewRCValue.equalsIgnoreCase(ChangeConstants.FOR_RELEASE) && slReleased.contains(strObjState) && isError == 0) {
                isError = 1;
            } else {
                // policy is Legacy CAD so only allowed values are For Release, for Obsolence , None
                if (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_Legacy_CAD) && isError == 0) {
                    if (!slLegacyAllowed.contains(strNewRCValue)) {
                        // add alert that for legacy CAD only allowed values are for release, For obsolence and None
                        strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Notice.ForPSS_Legacy_CAD");
                        MqlUtil.mqlCommand(context, "notice $1", strMessage);
                        isError = 1;
                    }
                }

                if (strFromType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) {
                    DomainObject domChangeOrder = new DomainObject(strFromObjectId);
                    String strCOCurrent = domChangeOrder.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if (strCOCurrent.equalsIgnoreCase("Complete")) {
                        return isError;
                    }
                }

                if (strNewRCValue.equalsIgnoreCase(ChangeConstants.FOR_RELEASE) && isError == 0) {
                    slAllowedStates = (StringList) mRequestedChangeMapping.get(ChangeConstants.FOR_RELEASE);
                    if (!slAllowedStates.contains(strObjState)) {

                        isError = 1;
                    }
                } else if (strNewRCValue.equalsIgnoreCase(ChangeConstants.FOR_REVISE) && isError == 0) {
                    slAllowedStates = (StringList) mRequestedChangeMapping.get(ChangeConstants.FOR_REVISE);
                    if (!slAllowedStates.contains(strObjState)) {

                        isError = 1;
                    }
                } else if (strNewRCValue.equalsIgnoreCase(TigerConstants.FOR_CLONE) && isError == 0) {
                    slAllowedStates = (StringList) mRequestedChangeMapping.get(TigerConstants.FOR_CLONE);
                    if (!slAllowedStates.contains(strObjState)) {

                        isError = 1;
                    }
                } else if (strNewRCValue.equalsIgnoreCase(TigerConstants.FOR_REPLACE) && isError == 0) {
                    slAllowedStates = (StringList) mRequestedChangeMapping.get(TigerConstants.FOR_REPLACE);
                    if (!slAllowedStates.contains(strObjState)) {
                        isError = 1;
                    }

                } else if (strNewRCValue.equalsIgnoreCase(ChangeConstants.FOR_OBSOLESCENCE) && isError == 0) {
                    slAllowedStates = (StringList) mRequestedChangeMapping.get(ChangeConstants.FOR_OBSOLESCENCE);
                    if (!slAllowedStates.contains(strObjState)) {
                        isError = 1;
                    }
                    // TIGTK-17751 : Start
                    else {
                        boolean isForObolescence = true;
                        if (!checkParentChildPrerequisite(context, idCOCRConnectedToContextCA, isForObolescence, strToObjectId)) {
                            isError = 2;
                        }
                    }
                    // TIGTK-17751 : End
                }
                // TIGTK-17751 : Start
                if (!strNewRCValue.equalsIgnoreCase(ChangeConstants.FOR_OBSOLESCENCE) && isError == 0) {
                    boolean isForObolescence = false;
                    if (!checkParentChildPrerequisite(context, idCOCRConnectedToContextCA, isForObolescence, strToObjectId)) {
                        isError = 3;
                    }
                }
                // TIGTK-17751 : End
            }
            if (isError == 1) {
                // Show alert that User can not set selected value
                StringBuffer sbMessage = new StringBuffer();
                sbMessage.append(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Notice.FinalMessageStart"));
                sbMessage.append(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Notice.FinalMessageEnd"));
                sbMessage.append(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Notice.FinalMappingListStart"));
                sbMessage.append(EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Notice.FinalMappingListEnd"));

                MqlUtil.mqlCommand(context, "notice $1", sbMessage.toString());
            }
            // TIGTK-17751 : Start
            else if (isError == 2) {
                String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Warning.PartObsoluteMessage");
                emxContextUtilBase_mxJPO.mqlNotice(context, strAlertMessage);
                isError = 1;
            } else if (isError == 3) {
                String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "EnterpriseChangeMgt.Warning.ChildPartForObsoluteMessage");
                emxContextUtilBase_mxJPO.mqlNotice(context, strAlertMessage);
                isError = 1;
            }
            // TIGTK-17751 : End

            // Find Bug modifications: 23/03/2017 : KWagh : START
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkIfAffectedItemAlreadyConnectedToChangeObject: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        // Find Bug modifications: 23/03/2017 : KWagh : End
        return isError;

    }

    // TIGTK-17751 : Start
    private boolean checkParentChildPrerequisite(Context context, String idCOCRConnectedToContextCA, boolean isForObolescence, String strToObjectId) {
        try {
            StringBuffer relPattern = new StringBuffer(30);
            relPattern.append(ChangeConstants.RELATIONSHIP_EBOM);
            relPattern.append(",");
            relPattern.append(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
            relPattern.append(",");
            relPattern.append(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
            relPattern.append(",");
            relPattern.append("CAD SubComponent");
            StringBuffer typePattern = new StringBuffer(30);
            typePattern.append(TigerConstants.TYPE_PART);
            typePattern.append(",");
            typePattern.append(TigerConstants.TYPE_CADDRAWING);
            typePattern.append(",");
            typePattern.append(ChangeConstants.TYPE_CAD_MODEL);
            typePattern.append(",");
            typePattern.append("PSS_CATPart");
            typePattern.append(",");
            typePattern.append("PSS_CATProduct");

            StringList objectSelectables = new StringList(2);
            objectSelectables.addElement(SELECT_ID);
            objectSelectables.addElement(SELECT_CURRENT);
            StringList relSelectables = new StringList(2);
            relSelectables.addElement(DomainRelationship.SELECT_ID);
            relSelectables.addElement(DomainRelationship.SELECT_NAME);

            boolean traverseDirectionFrom = false;
            boolean traverseDirectionTo = false;
            if (isForObolescence) {
                traverseDirectionTo = true;
            } else {
                traverseDirectionFrom = true;
            }

            if (UIUtil.isNotNullAndNotEmpty(strToObjectId)) {
                DomainObject dmObj = new DomainObject(strToObjectId);
                MapList mListPartPrerequisites = dmObj.getRelatedObjects(context, relPattern.toString(), typePattern.toString(), objectSelectables, relSelectables, traverseDirectionTo,
                        traverseDirectionFrom, (short) 1, EMPTY_STRING, EMPTY_STRING, (short) 0);
                Iterator partIterator = mListPartPrerequisites.iterator();
                List<String> listNonObsoleteParentIds = new ArrayList<>();

                while (partIterator.hasNext()) {
                    Map tempMap = (Map) partIterator.next();
                    String strCurrentStateOfPrereq = (String) tempMap.get(SELECT_CURRENT);
                    if (isForObolescence) {
                        if (!TigerConstants.STATE_PART_OBSOLETE.equalsIgnoreCase(strCurrentStateOfPrereq)) {
                            listNonObsoleteParentIds.add((String) tempMap.get(SELECT_ID));
                        }
                    } else {
                        listNonObsoleteParentIds.add((String) tempMap.get(SELECT_ID));
                    }
                }
                if (!listNonObsoleteParentIds.isEmpty()) {

                    String[] arrNonObsoleteParentIds = listNonObsoleteParentIds.toArray(new String[listNonObsoleteParentIds.size()]);
                    objectSelectables = new StringList(3);
                    objectSelectables.addElement(SELECT_ID);
                    objectSelectables.addElement("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from[" + ChangeConstants.TYPE_CHANGE_ACTION + "].current");
                    objectSelectables.addElement("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from[" + ChangeConstants.TYPE_CHANGE_ACTION + "].id");
                    objectSelectables.addElement("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].attribute[" + ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE + "]");

                    List<String> excludeStateListCA = new ArrayList<>();
                    Map<String, String> listCA = new HashMap<>();
                    excludeStateListCA.add(TigerConstants.STATE_CHANGEACTION_CANCELLED);
                    excludeStateListCA.add(TigerConstants.STATE_CHANGEACTION_COMPLETE);

                    BusinessObjectWithSelectList businessObjectWithSelectList = BusinessObject.getSelectBusinessObjectData(context, arrNonObsoleteParentIds, objectSelectables);

                    Iterator<BusinessObjectWithSelect> boIterator = businessObjectWithSelectList.iterator();
                    while (boIterator.hasNext()) {
                        BusinessObjectWithSelect businessObjectWithSelect = boIterator.next();
                        String parentId = businessObjectWithSelect.getSelectData(SELECT_ID);
                        StringList listCAid = businessObjectWithSelect
                                .getSelectDataList("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from[" + ChangeConstants.TYPE_CHANGE_ACTION + "].id");
                        StringList listCAstate = businessObjectWithSelect
                                .getSelectDataList("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from[" + ChangeConstants.TYPE_CHANGE_ACTION + "].current");
                        StringList listCAIattribute = businessObjectWithSelect
                                .getSelectDataList("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].attribute[" + ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE + "]");
                        String sCurrentCA = "";
                        if (null != listCAstate) {
                            for (int i = 0; i < listCAstate.size(); i++) {
                                if (!(excludeStateListCA.contains((String) listCAstate.get(i))) && ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase((String) listCAIattribute.get(i))) {
                                    sCurrentCA = (String) listCAid.get(i);
                                }
                            }
                        }

                        if (UIUtil.isNotNullAndNotEmpty(sCurrentCA)) {
                            listCA.put(parentId, sCurrentCA);
                        } else {
                            if (isForObolescence) {
                                return false;
                            }
                        }
                    }

                    Set<String> setCAid = new HashSet<>(listCA.values());
                    String[] arrParentCAIds = setCAid.toArray(new String[setCAid.size()]);

                    objectSelectables = new StringList(2);
                    objectSelectables.addElement(SELECT_ID);
                    objectSelectables.addElement("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
                    businessObjectWithSelectList = BusinessObject.getSelectBusinessObjectData(context, arrParentCAIds, objectSelectables);

                    boIterator = businessObjectWithSelectList.iterator();
                    while (boIterator.hasNext()) {
                        BusinessObjectWithSelect businessObjectWithSelect = boIterator.next();
                        String sCAId = businessObjectWithSelect.getSelectData(SELECT_ID);
                        String sCOid = businessObjectWithSelect.getSelectData("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");

                        if (isForObolescence) {
                            if (!idCOCRConnectedToContextCA.equalsIgnoreCase(sCOid))
                                return false;
                        } else if ((idCOCRConnectedToContextCA.equalsIgnoreCase(sCOid))) {
                            return false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    // TIGTK-17751 : End

    // PCM RFC-074 : 3/10/2016 : KWagh : End

    public Map createChangeOrder(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);

        String strChangeOrderId = "";
        String sVault = (String) programMap.get("Vault");
        String sOwner = (String) programMap.get(SELECT_OWNER);
        Map returnMap = new HashMap();
        try {
            strChangeOrderId = changeOrder.create(context, "type_PSS_ChangeOrder", "policy_PSS_ChangeOrder", sVault, sOwner);
            returnMap.put(ChangeConstants.ID, strChangeOrderId);

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in createChangeOrder: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

        return returnMap;
    }

    public MapList getChangeOrderItems(Context context, String[] args) throws Exception {

        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        String sCRID = (String) paramMap.get("parentOID");

        String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeOrder");
        String typePattern = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeOrder");

        StringList slObjectSle = new StringList(1);
        slObjectSle.addElement(DomainConstants.SELECT_ID);
        slObjectSle.addElement(DomainConstants.SELECT_NAME);

        StringList slRelSle = new StringList(1);
        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

        DomainObject domCRObj = new DomainObject(sCRID);
        // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
        MapList mlChangeOrder = domCRObj.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null);

        return mlChangeOrder;
    }

    // Addition for Tiger - PCM stream by SGS starts

    /**
     * Method to get the connected CAD Object to Original Part.
     * @param context
     * @param args
     *            - Selected Item for Add Affected Item in CO which is comes from PSS_ECMFullSearchPostProcess.jsp
     * @return StringList - Contains the Object Id of the Part which is Passed in method argument and connected CAD Object with them
     * @throws Exception
     */
    public StringList getCADConnectedWithPart(Context context, String[] args) throws Exception {
        StringList selectStmts = new StringList(1);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        StringList relStmts = new StringList(0);
        StringList strSelectedItemsWithSymmetrical = new StringList();
        StringList strCADItems = new StringList();

        for (int i = 0; i < args.length; i++) {
            strSelectedItemsWithSymmetrical.add(args[i]);
            DomainObject domSelectedItem = DomainObject.newInstance(context, (String) strSelectedItemsWithSymmetrical.get(i));
            String strObjectPolicy = (String) domSelectedItem.getInfo(context, domSelectedItem.SELECT_POLICY);
            String strIsPart = (String) domSelectedItem.getInfo(context, "type.kindof[" + DomainConstants.TYPE_PART + "]");

            // Check if Object Type is Part & policy is (Standard Part or PSS_EC_Part) than Check for Connected Symmetrical Part.
            if (strIsPart.equalsIgnoreCase("TRUE") && (strObjectPolicy.equals(TigerConstants.POLICY_PSS_ECPART) || strObjectPolicy.equals(TigerConstants.POLICY_STANDARDPART)
                    || strObjectPolicy.equals(TigerConstants.POLICY_PSS_DEVELOPMENTPART))) {
                MapList mlCADObject = domSelectedItem.getRelatedObjects(context, RELATIONSHIP_PART_SPECIFICATION, // relationship pattern
                        STR_TYPE_ALL_CAD, // object pattern
                        selectStmts, // object selects
                        relStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        null, // object where clause
                        null); // relationship where clause

                if (mlCADObject.size() != 0) {
                    for (int j = 0; j < mlCADObject.size(); j++) {
                        Map<String, String> map = (Map<String, String>) mlCADObject.get(j);
                        String strCADId = map.get("id");
                        strCADItems.add(strCADId); // Add CAD Item's Id to StringList of CAD Items
                    }
                }
            }
        }

        // Add CAD Object of selected part in CA if it is NOT exists on CO's Affected Item
        if (strCADItems.size() != 0) {
            for (int j = 0; j < strCADItems.size(); j++) {
                strSelectedItemsWithSymmetrical.add(strCADItems.get(j));
            }
        }
        return strSelectedItemsWithSymmetrical;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Custom Check trigger method to check when the Change Order is promoted from Prepare to In Work state
     */
    public int checkBasicPreRequisite(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String strMessage = DomainConstants.EMPTY_STRING;
        String strCAID = DomainConstants.EMPTY_STRING;
        String strPartID = DomainConstants.EMPTY_STRING;
        int retValue = 0;
        String strNextPartID = DomainConstants.EMPTY_STRING;

        StringList slPartId = new StringList();
        try {

            String strCOId = args[0];
            DomainObject domObjCO = DomainObject.newInstance(context, strCOId);

            // Connected Change Request Objects
            // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            // PCM TS203-BR060 CO-Release Part without CR : TIGTK-6852 : 31/07/2017 : KWagh: Start

            boolean bIsCOForFirstRelease = isCOForFirstRelease(context, domObjCO);

            if (!bIsCOForFirstRelease) {

                MapList mlConnectedCR = domObjCO.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSle, slRelSle, true, false,
                        (short) 1, null, null, 0);

                if (mlConnectedCR.isEmpty()) {
                    strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.NoCRForCO");
                    MqlUtil.mqlCommand(context, "notice $1", strMessage);

                    return 1;
                }
            }
            // PCM TS203-BR060 CO-Release Part without CR : TIGTK-6852 : 31/07/2017 : KWagh: END

            // Connected Change Action Objects
            MapList mlConnectedCA = domObjCO.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, slObjectSle, slRelSle, false, true, (short) 1,
                    null, null, 0);

            if (mlConnectedCA.isEmpty()) {
                strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.NoAffectedItemForCO");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);

                return 1;
            } else {
                // TIGTK-3351:Code change for issue on CO promote in case symmetric part added in another CA:9/11/2016:RutujaE:Start
                for (int i = 0; i < mlConnectedCA.size(); i++) {
                    Map mCAObj = (Map) mlConnectedCA.get(i);
                    strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    DomainObject domobjCA = DomainObject.newInstance(context, strCAID);

                    // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                    MapList mlConnectedAffectedParts = domobjCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, DomainConstants.TYPE_PART, slObjectSle, slRelSle, false,
                            true, (short) 1, null, null, 0);
                    // add parts from all CA to stringlist
                    for (int j = 0; j < mlConnectedAffectedParts.size(); j++) {
                        Map mPartObj = (Map) mlConnectedAffectedParts.get(j);
                        strPartID = (String) mPartObj.get(DomainConstants.SELECT_ID);
                        slPartId.add(strPartID);
                    }
                }
                // iterate parts list to check related parts
                for (int k = 0; k < slPartId.size(); k++) {
                    strPartID = (String) slPartId.get(k);
                    DomainObject domobjPart = DomainObject.newInstance(context, strPartID);

                    String sName = domobjPart.getInfo(context, DomainConstants.SELECT_NAME);
                    String sType = domobjPart.getInfo(context, DomainConstants.SELECT_TYPE);
                    String sRev = domobjPart.getInfo(context, DomainConstants.SELECT_REVISION);

                    MapList mlConnectedParts = domobjPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, DomainConstants.TYPE_PART, slObjectSle, slRelSle, true, true,
                            (short) 1, null, null, 0);

                    if (!mlConnectedParts.isEmpty()) {
                        for (int r = 0; r < mlConnectedParts.size(); r++) {

                            Map mlConnectedPart = (Map) mlConnectedParts.get(r);
                            strNextPartID = (String) mlConnectedPart.get(DomainConstants.SELECT_ID);

                            if (!slPartId.contains(strNextPartID)) {

                                strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                        "PSS_EnterpriseChangeMgt.Alert.NoSymmericalPartConnected");
                                strMessage = strMessage + " : " + sType + " " + sName + " " + sRev;
                                MqlUtil.mqlCommand(context, "notice $1", strMessage);

                                return 1;
                            }
                        }
                    }
                }
            }

            // TIGTK-3351:Code change for issue on CO promote in case symmetric part added in another CA:9/11/2016:RutujaE:End
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkBasicPreRequisite: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return retValue;
    }

    // PCM TS203-BR060 CO-Release Part without CR : TIGTK-6852 : 31/07/2017 : KWagh: Start
    /**
     * @param context
     * @param domObjCo
     * @return
     * @author KWagh
     * @throws Exception
     */
    public boolean isCOForFirstRelease(Context context, DomainObject domObjCO) throws Exception {
        boolean bResult = true;
        try {

            // CO related project should be in Phase 1, Phase 2a or Phase 2b
            String strProjectCurrent = domObjCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.current");

            if (TigerConstants.STATE_PHASE1.equalsIgnoreCase(strProjectCurrent) || TigerConstants.STATE_PHASE2A.equalsIgnoreCase(strProjectCurrent)
                    || TigerConstants.STATE_PHASE2B.equalsIgnoreCase(strProjectCurrent)) {

                StringList slObjectSle = new StringList();
                slObjectSle.addElement(DomainConstants.SELECT_ID);
                slObjectSle.addElement(DomainConstants.SELECT_TYPE);
                slObjectSle.addElement(DomainConstants.SELECT_POLICY);

                StringList slRelSle = new StringList();
                slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
                slRelSle.addElement(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

                MapList mlConnectedCA = domObjCO.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, slObjectSle, slRelSle, false, true,
                        (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
                Iterator itrChangeAction = mlConnectedCA.iterator();

                while (itrChangeAction.hasNext()) {
                    Map mCAObj = (Map) itrChangeAction.next();
                    String strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    DomainObject domobjCA = DomainObject.newInstance(context, strCAID);
                    MapList mlConnectedAffectedItem = domobjCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, DomainConstants.QUERY_WILDCARD, slObjectSle, slRelSle,
                            false, true, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
                    Iterator itrAffectedItems = mlConnectedAffectedItem.iterator();

                    while (itrAffectedItems.hasNext()) {
                        Map mpAfftectedItem = (Map) itrAffectedItems.next();
                        String strAffectedItemType = (String) mpAfftectedItem.get(DomainConstants.SELECT_TYPE);
                        String strAffectedItemPolicy = (String) mpAfftectedItem.get(DomainConstants.SELECT_POLICY);
                        String strRequestedChangeValue = (String) mpAfftectedItem.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                        String strAffectedItemID = (String) mpAfftectedItem.get(DomainConstants.SELECT_ID);

                        // Check the affected items are only EC Part "For Release" and CO has no related CAD object
                        // EC Part is of Revision - 01
                        if (!(TigerConstants.TYPE_PART.equalsIgnoreCase(strAffectedItemType) && TigerConstants.POLICY_PSS_ECPART.equalsIgnoreCase(strAffectedItemPolicy))
                                || !ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strRequestedChangeValue)) {

                            bResult = false;
                            break;

                        }

                        // EC Part Revision is x but EC Part Revision x-1 is in Development policy and no CO/CA is attached to it.

                        DomainObject domAI = DomainObject.newInstance(context, strAffectedItemID);

                        BusinessObject busPrevisousRev = domAI.getPreviousRevision(context);

                        if (busPrevisousRev.exists(context)) {

                            DomainObject domPrevisousRev = DomainObject.newInstance(context, busPrevisousRev);

                            String strPriviousRevPolicy = domPrevisousRev.getInfo(context, DomainConstants.SELECT_POLICY);
                            // previous revision should have policy as "Development Part" and no CO/CA is attached to it.
                            // if it is not then then return true
                            if (UIUtil.isNotNullAndNotEmpty(strPriviousRevPolicy)) {
                                if (TigerConstants.POLICY_PSS_DEVELOPMENTPART.equalsIgnoreCase(strPriviousRevPolicy)) {

                                    String sChangeActionId = domPrevisousRev.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.id");
                                    if (UIUtil.isNotNullAndNotEmpty(sChangeActionId)) {
                                        bResult = false;
                                        break;
                                    }
                                } else {
                                    // previous revision is of policy "EC Part"
                                    bResult = false;
                                }
                            }
                        }
                    }
                }
            } else {
                bResult = false;
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ex) {
            throw ex;
        }
        return bResult;

    }

    // PCM TS203-BR060 CO-Release Part without CR : TIGTK-6852 : 31/07/2017 : KWagh: End

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Custom Check trigger method to check Validation Rules On Affected Items when the Change Order is promoted from Prepare to In Work state
     */
    public int checkValidationRulesOnAffectedItems(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        int retValue = 0;
        String relpattern;
        String typePattern;
        String strCAID;
        String relpatternAffectedItem;
        String strRequestedChangeAttrValue;
        String strType;
        String strPolicy;
        String strAffectedItemID;
        String strReleasedState;
        try {

            // TIGTK-16382 : 02-08-2018 : START
            boolean bIsCloneOrReplace = false;
            // TIGTK-16382 : 02-08-2018 : END

            String strCOId = args[0];
            DomainObject domObjCo = new DomainObject(strCOId);

            String strForClone = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.ATTRIBUTE_REQUESTED_CHANGE.ForClone");
            String strForReplace = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                    "PSS_EnterpriseChangeMgt.ATTRIBUTE_REQUESTED_CHANGE.ForReplace");
            String strForObsolesence = ChangeConstants.FOR_OBSOLESCENCE;
            String strForRevise = ChangeConstants.FOR_REVISE;
            String strForRelease = ChangeConstants.FOR_RELEASE;

            // Connected Affected Items Objects
            relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");
            typePattern = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");

            relpatternAffectedItem = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAffectedItem");
            // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
            StringList slObjectSle = new StringList(3);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_TYPE);
            slObjectSle.addElement(DomainConstants.SELECT_POLICY);
            // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            slRelSle.addElement(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

            MapList mlConnectedCAs = domObjCo.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null);

            if (!mlConnectedCAs.isEmpty()) {
                StringBuffer sbCATypeList = new StringBuffer();
                int iCheck = 0;

                for (int i = 0; i < mlConnectedCAs.size(); i++) {
                    Map mCAObj = (Map) mlConnectedCAs.get(i);
                    strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    DomainObject domobjCA = new DomainObject(strCAID);
                    String strCAType = checkForRouteTemplateOnProgramProject(context, domObjCo, domobjCA);

                    if (UIUtil.isNotNullAndNotEmpty(strCAType)) {
                        if (iCheck == 0) {
                            sbCATypeList.append(strCAType);
                        } else {
                            sbCATypeList.append(" , ");
                            sbCATypeList.append(strCAType);
                        }
                        iCheck = 1;
                    }

                    MapList mlConnectedAffectedItem = domobjCA.getRelatedObjects(context, relpatternAffectedItem, "*", slObjectSle, slRelSle, false, true, (short) 1, null, null);

                    for (int j = 0; j < mlConnectedAffectedItem.size(); j++) {
                        Map mAffectedItemObj = (Map) mlConnectedAffectedItem.get(j);
                        strAffectedItemID = (String) mAffectedItemObj.get(DomainConstants.SELECT_ID);
                        strRequestedChangeAttrValue = (String) mAffectedItemObj.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                        strType = (String) mAffectedItemObj.get(DomainConstants.SELECT_TYPE);
                        strPolicy = (String) mAffectedItemObj.get(DomainConstants.SELECT_POLICY);
                        DomainObject domAffectedItem = new DomainObject(strAffectedItemID);
                        boolean bKindOfPart = domAffectedItem.isKindOf(context, DomainConstants.TYPE_PART);
                        strReleasedState = ECMAdmin.getReleaseStateValue(context, strType, strPolicy);

                        // PCM : TIGTK-9025 : 12/07/2017 : AB : START
                        if ((strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT))) {
                            String strRelationshipName = RELATIONSHIP_PART_SPECIFICATION + "," + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING;
                            MapList mlConnectedPart = domAffectedItem.getRelatedObjects(context, strRelationshipName, DomainConstants.QUERY_WILDCARD, slObjectSle, slRelSle, true, false, (short) 1,
                                    null, null, (short) 0);

                            StringList slConnectedPartPolicy = new StringList();
                            for (int k = 0; k < mlConnectedPart.size(); k++) {
                                if (mlConnectedPart.size() != 0) {
                                    Map mPartObj = (Map) mlConnectedPart.get(k);
                                    String strPartPolicy = (String) mPartObj.get(DomainConstants.SELECT_POLICY);
                                    slConnectedPartPolicy.add(strPartPolicy);
                                }
                            }

                            if (!slConnectedPartPolicy.isEmpty()
                                    && !(slConnectedPartPolicy.contains(TigerConstants.POLICY_PSS_ECPART) || slConnectedPartPolicy.contains(TigerConstants.POLICY_STANDARDPART))) {
                                String strMsg = "CAD cannot be connected to a CR or CO if the CAD is related to a Dev Part";
                                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                                return 1;
                            }
                        }
                        // PCM : TIGTK-9025 : 12/07/2017 : AB : END

                        if ((strRequestedChangeAttrValue.equalsIgnoreCase(strForRelease)) && bKindOfPart && !(strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART))) {
                            retValue = this.itemInWorkAndLast(context, strAffectedItemID, strReleasedState);

                            if (retValue == 1) {
                                String strMsg = "Standard Part is not in In Work state or Item is not a Last revision";
                                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                                return retValue;
                            }

                        }

                        if ((strRequestedChangeAttrValue.equalsIgnoreCase(strForRelease)) && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT))) {
                            retValue = this.itemInWorkAndLast(context, strAffectedItemID, strReleasedState);
                            if (retValue == 1) {
                                String strMsg = "Standard Part is not in In Work state or Item is not a Last revision";
                                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                                return retValue;
                            }

                        }

                        if ((strRequestedChangeAttrValue.equalsIgnoreCase(strForRelease)) && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART))) {

                            retValue = this.itemInWorkAndLast(context, strAffectedItemID, strReleasedState);
                            if (retValue == 1) {
                                String strMsg = "Standard Part is not in In Work state or Item is not a Last revision";
                                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                                return retValue;
                            }

                        }

                        if ((strRequestedChangeAttrValue.equalsIgnoreCase(strForRevise)) && bKindOfPart && !(strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART))) {
                            retValue = this.itemReleaseAndLastRevisionExists(context, strAffectedItemID, strReleasedState);
                            if (retValue == 1) {
                                String strMsg = "Part is not in 'Released' state or connected Part is not a Last revision or If newer revision exists, the new revision not be in 'In Work' state or must be attached to any other CO";
                                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                                return retValue;
                            }

                        }

                        if ((strRequestedChangeAttrValue.equalsIgnoreCase(strForRevise)) && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT))) {

                            retValue = this.itemReleaseAndLast(context, strAffectedItemID, strReleasedState);
                            if (retValue == 1) {
                                String strMsg = "CAD Part is not in 'Released' state or connected CAD is not a Last revision";
                                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                                return retValue;
                            }

                        }

                        if ((strRequestedChangeAttrValue.equalsIgnoreCase(strForRevise)) && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART))) {

                            retValue = this.itemReleaseAndLastRevisionExists(context, strAffectedItemID, strReleasedState);
                            if (retValue == 1) {
                                String strMsg = "Standard Part is not in 'Released' state or connected Standard Part is not a Last revision or If newer revision exists, the new revision not be in 'In Work' state or must be attached to any other CO";
                                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                                return retValue;
                            }

                        }

                        if (((strRequestedChangeAttrValue.equalsIgnoreCase(strForRevise)) || (strRequestedChangeAttrValue.equalsIgnoreCase(strForClone))
                                || (strRequestedChangeAttrValue.equalsIgnoreCase(strForReplace)) || (strRequestedChangeAttrValue.equalsIgnoreCase(strForObsolesence))) && bKindOfPart
                                && !(strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART))) {

                            retValue = this.itemReleaseAndLast(context, strAffectedItemID, strReleasedState);

                            if (retValue == 1) {
                                String strMsg = "Part is not in Released state or Connected Part is not a Last revision";
                                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                                return retValue;
                            }

                        }

                        if (((strRequestedChangeAttrValue.equalsIgnoreCase(strForClone)) || (strRequestedChangeAttrValue.equalsIgnoreCase(strForReplace))
                                || (strRequestedChangeAttrValue.equalsIgnoreCase(strForObsolesence))) && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT))) {

                            retValue = this.itemReleaseAndLast(context, strAffectedItemID, strReleasedState);
                            if (retValue == 1) {
                                String strMsg = "CAD is not in Released state or Connected CAD is not a Last revision";
                                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                                return retValue;
                            }

                        }

                        if ((strRequestedChangeAttrValue.equalsIgnoreCase(strForObsolesence)) && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART))) {

                            retValue = this.itemReleaseAndLast(context, strAffectedItemID, strReleasedState);
                            if (retValue == 1) {
                                String strMsg = "Standard Part is not in Released state or Connected Standard Part is not a Last revision";
                                MqlUtil.mqlCommand(context, "notice $1", strMsg);
                                return retValue;
                            }

                        }
                        if ((strRequestedChangeAttrValue.equalsIgnoreCase(strForClone) || strRequestedChangeAttrValue.equalsIgnoreCase(strForReplace))
                                && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART))) {
                            // TIGTK-16382 : 02-08-2018 : START
                            bIsCloneOrReplace = true;
                            break;
                            // TIGTK-16382 : 02-08-2018 : END
                        }
                    }

                    // TIGTK-16382 : 02-08-2018 : START
                    if (bIsCloneOrReplace) {
                        StringBuffer sbErrorMessage = new StringBuffer();
                        sbErrorMessage.append("Standard Part is not allowed for Requested Change = For Clone Or For Replace");
                        MqlUtil.mqlCommand(context, "notice $1", sbErrorMessage.toString());
                        return 1;
                    }
                    // TIGTK-16382 : 02-08-2018 : END

                }

                if (sbCATypeList.length() > 0) {
                    String strAlert = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.NORouteForCAType");
                    StringBuffer sbAlert = new StringBuffer();
                    sbAlert.append(strAlert);
                    sbAlert.append(" ");
                    sbAlert.append(sbCATypeList);
                    throw new Exception(sbAlert.toString());
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkValidationRulesOnAffectedItems: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return 0;

        // return retValue;
    }

    public int itemReleaseAndLast(Context context, String strAffectedItemID, String strReleasedState) throws Exception {
        int nresult = 0;
        DomainObject domAffectedItem = new DomainObject(strAffectedItemID);
        String strCurrent = domAffectedItem.getInfo(context, DomainConstants.SELECT_CURRENT);

        boolean bIsLastRev = domAffectedItem.isLastRevision(context);
        // TIGTK-6849:Phase-2.0:PKH:Start
        BusinessObject bomAffectedItem = domAffectedItem.getLastRevision(context);
        String strLastRevPolicy = bomAffectedItem.getPolicy(context).getName();
        // TIGTK-6849:Phase-2.0:PKH:End
        if (bIsLastRev && (strCurrent.equalsIgnoreCase(strReleasedState))) {
            nresult = 0;
        }
        // TIGTK-6849:Phase-2.0:PKH:Start
        else if (TigerConstants.POLICY_PSS_CANCELPART.equals(strLastRevPolicy) || TigerConstants.POLICY_PSS_CANCELCAD.equals(strLastRevPolicy)) {

            nresult = 0;
        }
        // TIGTK-6849:Phase-2.0:PKH:End
        else {
            nresult = 1;
        }
        return nresult;
    }

    public int itemReleaseAndLastRevisionExists(Context context, String strAffectedItemID, String strReleasedState) throws Exception {
        int nresult = 0;
        String strCAID;
        DomainObject domAffectedItem = new DomainObject(strAffectedItemID);
        String strCurrent = domAffectedItem.getInfo(context, DomainConstants.SELECT_CURRENT);
        boolean bIsLastRev = domAffectedItem.isLastRevision(context);
        if (bIsLastRev) {
            if (strCurrent.equalsIgnoreCase(strReleasedState)) {
                nresult = 0;
                return nresult;
            } else {
                nresult = 1;
                return nresult;
            }
        } else {
            BusinessObject busNextRev = domAffectedItem.getNextRevision(context);
            DomainObject domNextRev = new DomainObject(busNextRev);
            String strNextObjCurrent = domNextRev.getInfo(context, DomainConstants.SELECT_CURRENT);
            // Connected Change Action Objects
            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAffectedItem");

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            MapList mlConnectedCAs = domNextRev.getRelatedObjects(context, relpattern, "*", slObjectSle, slRelSle, true, false, (short) 1, null, null);

            if (!mlConnectedCAs.isEmpty()) {
                for (int i = 0; i < mlConnectedCAs.size(); i++) {
                    Map mCAObj = (Map) mlConnectedCAs.get(i);
                    strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    DomainObject domobjCA = new DomainObject(strCAID);

                    String relpatternCO = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");
                    String typpatternCO = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeOrder");
                    MapList mlConnectedCOObj = domobjCA.getRelatedObjects(context, relpatternCO, typpatternCO, slObjectSle, slRelSle, true, false, (short) 1, null, null);

                    if ((strNextObjCurrent.equalsIgnoreCase(strReleasedState)) && (mlConnectedCOObj.isEmpty())) {

                        nresult = 0;
                        return nresult;
                    } else {
                        nresult = 1;
                        return nresult;
                    }
                }
            }

        }
        return nresult;
    }

    public int itemInWorkAndLast(Context context, String strAffectedItemID, String strReleasedState) throws Exception {

        int nresult = 0;

        DomainObject domAffectedItem = new DomainObject(strAffectedItemID);
        String strCurrent = domAffectedItem.getInfo(context, DomainConstants.SELECT_CURRENT);
        boolean bIsLastRev = domAffectedItem.isLastRevision(context);
        StringList strListChildStates = domAffectedItem.getInfoList(context, SELECT_STATES);

        int nReleasedState = strListChildStates.indexOf(strReleasedState);
        int indexObjectState = strListChildStates.indexOf(strCurrent);

        if ((indexObjectState < nReleasedState) && bIsLastRev) {
            nresult = 0;
            return nresult;
        } else {
            nresult = 1;
            return nresult;
        }

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Custom Override trigger method to invoke JSP when the Change Order is promoted from Prepare to In Work state
     */
    public int checkForAffectedItemWithCRDuringPromote(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {
            String objectId = args[0];
            // PCM RFC-074 : 29/09/2016 : KWagh : Start
            DomainObject domCOObj = new DomainObject(objectId);
            String strAttrPSS_CheckFlagForPassedTrigger = domCOObj.getAttributeValue(context, "PSS_CheckFlagForPassedTrigger");
            if (strAttrPSS_CheckFlagForPassedTrigger.equalsIgnoreCase("false")) {
                domCOObj.setAttributeValue(context, "PSS_CheckFlagForPassedTrigger", "true");

                // Connected Affected Items Objects
                String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");
                String typePattern = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");
                String strCAID;
                String typePatterChangeReq = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeRequest");

                StringList slObjectSle = new StringList(3);
                slObjectSle.addElement(DomainConstants.SELECT_ID);
                slObjectSle.addElement(DomainConstants.SELECT_TYPE);
                slObjectSle.addElement(DomainConstants.SELECT_POLICY);

                StringList slRelSle = new StringList(1);
                slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
                slRelSle.addElement(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

                MapList mlConnectedCAs = domCOObj.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null, 0);
                if (!mlConnectedCAs.isEmpty()) {
                    for (int i = 0; i < mlConnectedCAs.size(); i++) {
                        Map mCAObj = (Map) mlConnectedCAs.get(i);
                        strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                        DomainObject domobjCA = new DomainObject(strCAID);

                        MapList mlConnectedChangeRequest = domobjCA.getRelatedObjects(context, relpattern, typePatterChangeReq, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);
                        if (mlConnectedChangeRequest.isEmpty()) {
                            StringBuffer processStr = new StringBuffer();

                            processStr.append("JSP:postProcess");
                            processStr.append("|");
                            processStr.append("commandName=");
                            processStr.append("PSS_PromoteCOfromJSP");
                            processStr.append("|");
                            processStr.append("objectId=");
                            processStr.append(objectId);
                            processStr.append("|");
                            processStr.append("parentOID=");
                            processStr.append(objectId);
                            MqlUtil.mqlCommand(context, "notice $1", processStr.toString());
                            return 1;
                        } // If mlConnectedChangeRequest end
                        else {
                            return 0;
                        } // else end
                    } // For end
                } // If mlConnectedCAs end
            } // IF (strAttrPSS_CheckFlagForPassedTrigger) end
            else {

                return 0;
            }
        } // try end
        catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForAffectedItemWithCRDuringPromote: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        } // catch end
        return 0;
        // PCM RFC-074 : 29/09/2016 : KWagh : End
    }// Method End

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Custom Override trigger method to get Affected Items Not Connected To CR when the Change Order is promoted from Prepare to In Work state
     */
    public MapList getAffectedItemsNotConnectedToCR(Context context, String[] args) throws Exception {
        try {
            // Modified by - KWagh-Start
            String strAIId;
            String strPolicy;
            // Modified by - KWagh-End
            // To hold the table data
            StringList uniqueList = new StringList();

            MapList mlTableData = new MapList();
            String relpatternAffectedItem = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAffectedItem");
            // Get object list information from packed arguments
            Map programMap = (Map) JPO.unpackArgs(args);
            // Get object id
            String strCOObjectId = (String) programMap.get("objectId");

            DomainObject domCOObj = new DomainObject(strCOObjectId);

            // Connected Affected Items Objects
            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeOrder");
            String typePattern = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");
            String strCAID;
            String relpatternChangeReq = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");
            String typePatterChangeReq = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeRequest");

            StringList slObjectSle = new StringList(3);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_TYPE);
            slObjectSle.addElement(DomainConstants.SELECT_POLICY);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            slRelSle.addElement(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
            MapList mlConnectedChangeRequest = domCOObj.getRelatedObjects(context, relpattern, typePatterChangeReq, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

            if (!mlConnectedChangeRequest.isEmpty()) {
                // Modified by - KWagh-Start
                // Added by Kalpesh-Start
                StringList allCRAffectedItem = new StringList();
                StringList allCOAffectedItem = new StringList();
                DomainObject crDOM = new DomainObject();
                StringList crAffectedItem = new StringList();
                // Modified by - KWagh
                for (Object object : mlConnectedChangeRequest) {
                    Map attMap = (Map) object;
                    String crObjectIO = (String) attMap.get(DomainObject.SELECT_ID);
                    crDOM.setId(crObjectIO);

                    StringList objectSelects = new StringList(1);
                    objectSelects.addElement(DomainConstants.SELECT_ID);
                    objectSelects.addElement(DomainConstants.SELECT_POLICY);
                    objectSelects.addElement(DomainConstants.SELECT_TYPE);
                    objectSelects.addElement(DomainConstants.SELECT_CURRENT);
                    StringList relSelects = new StringList(1);
                    relSelects.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                    String strWhere = "(policy!='" + TigerConstants.POLICY_PSS_DEVELOPMENTPART + "'&& policy!='" + TigerConstants.POLICY_PSS_MBOM + "')";
                    String strCADtoPartcondition = "to[Part Specification].from.policy";

                    MapList mlAffectedItemList = crDOM.getRelatedObjects(context, // context
                            RELATIONSHIP_AFFECTED_ITEM, // relationship pattern
                            "*", // object pattern
                            objectSelects, // object selects
                            relSelects, // relationship selects
                            false, // to direction
                            true, // from direction
                            (short) 1, // recursion level
                            strWhere, // object where clause
                            null, // relationship where clause
                            (short) 0);
                    int ncnt = mlAffectedItemList.size();

                    for (int p = 0; p < ncnt; p++) {
                        Map mAffectedItemObj = (Map) mlAffectedItemList.get(p);
                        strAIId = (String) mAffectedItemObj.get(DomainConstants.SELECT_ID);
                        strPolicy = (String) mAffectedItemObj.get(DomainConstants.SELECT_POLICY);

                        DomainObject objAffectedItem = new DomainObject(strAIId);
                        // PCM RFC-074 : 29/09/2016 : KWagh : Start
                        // Affected Item is CAD Object
                        // Check for CAD is related to Development Part.
                        if ((strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT)) || (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_Legacy_CAD))) {
                            String strCADConnectedToDevPart = objAffectedItem.getInfo(context, strCADtoPartcondition);

                            if (strCADConnectedToDevPart.equalsIgnoreCase(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
                                // CAD is related to Development Part
                            } else {
                                crAffectedItem.add(strAIId);
                            }

                        } else {

                            crAffectedItem.add(strAIId);
                        }
                    }
                    // PCM RFC-074 : 29/09/2016 : KWagh : End
                    if (!crAffectedItem.isEmpty()) {
                        allCRAffectedItem.addAll(crAffectedItem);
                    }
                }
                // Added by Kalpesh-End

                MapList mlConnectedCAs = domCOObj.getRelatedObjects(context, relpatternChangeReq, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null, 0);
                if (!mlConnectedCAs.isEmpty()) {
                    for (int i = 0; i < mlConnectedCAs.size(); i++) {
                        Map mCAObj = (Map) mlConnectedCAs.get(i);
                        strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                        DomainObject domobjCA = new DomainObject(strCAID);
                        DomainObject caDOM = new DomainObject();
                        for (Object object : mlConnectedCAs) {
                            Map attMap = (Map) object;
                            String caObjectIO = (String) attMap.get(DomainObject.SELECT_ID);
                            caDOM.setId(caObjectIO);
                            StringList coAffectedItem = domobjCA.getInfoList(context, "from[" + relpatternAffectedItem + "].to.id");
                            if (!coAffectedItem.isEmpty()) {
                                allCOAffectedItem.addAll(coAffectedItem);
                            }
                        }
                    }
                }
                if (!allCRAffectedItem.isEmpty()) {

                    String tempCRAffectedId = "";
                    uniqueList.addAll(allCRAffectedItem);
                    int ncount = allCRAffectedItem.size();
                    for (int i = 0; i < ncount; i++) {
                        tempCRAffectedId = (String) allCRAffectedItem.get(i);

                        if (allCOAffectedItem.contains(tempCRAffectedId)) {
                            // Remove this affected item from CR list(allCRAffectedItem)
                            uniqueList.remove(i);
                        }
                    }
                }

                // show the Affected Items which are connected to CR but not connected to CO(allCRAffectedItem)
                for (int j = 0; j < uniqueList.size(); j++) {
                    Map mpTableData = new HashMap();
                    mpTableData.put("id", uniqueList.get(j));
                    mlTableData.add(mpTableData);
                }
                // Modified by - KWagh-End
            }
            return mlTableData;
        } catch (Exception exp) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getAffectedItemsNotConnectedToCR: ", exp);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw exp;
        }
    }

    /**
     * @author KWagh
     * @param context
     * @param args
     * @throws Exception
     *             Custom method to promote Change Order Object from Prepare to In Work state
     */
    public void promoteChangeOrderAfterCheck(Context context, String[] args) throws Exception {

        Map programMap = (Map) JPO.unpackArgs(args);
        // Get object id
        String strCOObjectId = (String) programMap.get("busObjId");

        BusinessObject busObj = new BusinessObject(strCOObjectId);
        busObj.promote(context);
    }

    /**
     * @author KWagh
     * @param context
     * @param args
     * @throws Exception
     *             Custom method to promote Change Order Object from Prepare to In Work state
     */
    public void performTransitionOnCOPromote(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        int iReplaceFlag = 0;
        StringList slConnectionIdList = new StringList();

        String strOwner = context.getUser();
        // Set RPE value for Title Block auto generation
        PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_PROMOTE_USERNAME", strOwner);
        PropertyUtil.setGlobalRPEValue(context, "performTransitionOnCOPromote", "True");

        StringList slPropogatePartSpecification = new StringList();
        StringList slPropogatePartSpecificationForClone = new StringList();
        MapList mlCAAndAffectedItem = new MapList();
        pss.cad2d3d.DECTGUtil_mxJPO dectgutilJPO = new pss.cad2d3d.DECTGUtil_mxJPO();
        try {
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_REVISE_FROM_PCM", "True");

            String strCOId = args[0];

            DomainObject domObjCo = new DomainObject(strCOId);

            PSS_enoECMChangeRequest_mxJPO changeRequest = new PSS_enoECMChangeRequest_mxJPO(context, null);
            domObjCo.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_COSTARTDATE, changeRequest.getSystemDate(context, null));

            StringList slObjectSelectStatements = new StringList();
            slObjectSelectStatements.addElement(DomainConstants.SELECT_ID);
            slObjectSelectStatements.addElement(DomainConstants.SELECT_TYPE);
            slObjectSelectStatements.addElement(DomainConstants.SELECT_POLICY);
            slObjectSelectStatements.addElement(DomainConstants.SELECT_CURRENT);
            slObjectSelectStatements.addElement(DomainConstants.SELECT_NAME);
            slObjectSelectStatements.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");

            StringList slRelationshipSelectStatement = new StringList();
            slRelationshipSelectStatement.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            slRelationshipSelectStatement.addElement(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

            MapList mlConnectedCAs = domObjCo.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CHANGEACTION, TigerConstants.TYPE_CHANGEACTION, slObjectSelectStatements,
                    slRelationshipSelectStatement, false, true, (short) 1, null, null, 0);
            mlConnectedCAs.sort("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]", "descending", "String");
            if (!mlConnectedCAs.isEmpty()) {
                Iterator itrCA = mlConnectedCAs.iterator();
                while (itrCA.hasNext()) {
                    Map mCAObj = (Map) itrCA.next();
                    String strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);

                    DomainObject domobjCA = new DomainObject(strCAID);

                    MapList mlConnectedAffectedItem = domobjCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, DomainConstants.QUERY_WILDCARD, slObjectSelectStatements,
                            slRelationshipSelectStatement, false, true, (short) 1, null, null, 0);

                    slForCurrentCOAffectedItems.addAll(getStringListFromMaplist(mlConnectedAffectedItem, DomainConstants.SELECT_ID));
                }
            }

            if (!mlConnectedCAs.isEmpty()) {

                Iterator itrCA = mlConnectedCAs.iterator();
                while (itrCA.hasNext()) {
                    Map mCAObj = (Map) itrCA.next();
                    String strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);

                    DomainObject domobjCA = new DomainObject(strCAID);

                    MapList mlConnectedAffectedItem = domobjCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, DomainConstants.QUERY_WILDCARD, slObjectSelectStatements,
                            slRelationshipSelectStatement, false, true, (short) 1, null, null, 0);

                    //TIGTK-18183: Start
                    MapList mlCADDrawings = new MapList();
        
                    for(Object affectedItem : mlConnectedAffectedItem ) {
                        Map map = (Map)affectedItem;
                        String sType = (String) map.get(DomainConstants.SELECT_TYPE);
                        if(TigerConstants.TYPE_PSS_CATDRAWING.equalsIgnoreCase(sType)) {
                            mlCADDrawings.add(map);
                        }
                    }
                    
                    MapList mlTemp = new MapList(mlCADDrawings);
                    
                    for(Object affectedItem : mlConnectedAffectedItem ) {
                        Map map = (Map)affectedItem;
                        String sType = (String) map.get(DomainConstants.SELECT_TYPE);
                        if(!TigerConstants.TYPE_PSS_CATDRAWING.equalsIgnoreCase(sType)) {
                            mlTemp.add(map);
                        }
                    }
                    mlConnectedAffectedItem=mlTemp;
                    //TIGTK-18183: End  

                    // create map of CA id as key and affecteditem maplist as value
                    HashMap<String, Object> mCAAfectedItems = new HashMap<String, Object>();
                    mCAAfectedItems.put(strCAID, mlConnectedAffectedItem);
                    mlCAAndAffectedItem.add(mCAAfectedItems);

                    mlConnectedAffectedItem = sortAffectedItemsInParentChildOrder(context, mlConnectedAffectedItem);
                    // slForCurrentCOAffectedItems.addAll(getStringListFromMaplist(mlConnectedAffectedItem, DomainConstants.SELECT_ID));

                    Iterator itrAffectedItems = mlConnectedAffectedItem.iterator();
                    while (itrAffectedItems.hasNext()) {

                        Map mAffectedItemObj = (Map) itrAffectedItems.next();
                        String strAffectedItemID = (String) mAffectedItemObj.get(DomainConstants.SELECT_ID);
                        DomainObject domAffectedItem = DomainObject.newInstance(context, strAffectedItemID);

                        Map mAffectedInfo = domAffectedItem.getInfo(context, slObjectSelectStatements);

                        String strType = DomainConstants.EMPTY_STRING;
                        String strPolicy = DomainConstants.EMPTY_STRING;

                        if (!mAffectedInfo.isEmpty()) {
                            strType = (String) mAffectedInfo.get(DomainConstants.SELECT_TYPE);
                            strPolicy = (String) mAffectedInfo.get(DomainConstants.SELECT_POLICY);
                        }

                        String strConnection = MqlUtil.mqlCommand(context,
                                "print bus " + strAffectedItemID + " select  to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "|from.id =='" + strCAID + "'].id dump", false, false);

                        DomainRelationship domRelChangeAffectedItem = DomainRelationship.newInstance(context, strConnection);
                        String strRequestedChangeAttrValue = domRelChangeAffectedItem.getAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE);
                        boolean isSendMail = false;// TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 17/03/2017 |Harika Varanasi

                        if ((ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strRequestedChangeAttrValue)) && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT))) {
                            slForDisconnectPSfromPart.add(strAffectedItemID);
                        }
                        if ((ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strRequestedChangeAttrValue)) && (strType.equalsIgnoreCase(TigerConstants.TYPE_PART))) {
                            slForObsolescencePart.add(strAffectedItemID);
                        }
                        // If Requested Change is For Revise Type of Object is Part
                        if ((ChangeConstants.FOR_REVISE.equalsIgnoreCase(strRequestedChangeAttrValue)) && (strType.equalsIgnoreCase(TigerConstants.TYPE_PART))
                                && !(strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART))) {
                            String strNextRevId = DomainConstants.EMPTY_STRING;
                            boolean bIsLastRev = domAffectedItem.isLastRevision(context);

                            if (bIsLastRev) {
                                String nextRev = domAffectedItem.getNextSequence(context);
                                String lastRevVault = domAffectedItem.getVault();
                                BusinessObject boRevisedObj = new BusinessObject();
                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                // TIGTK-8886 - PTE - 2017-07-7 - START
                                try {
                                    context.setCustomData("PSS_PART_REVISE", "TRUE");
                                    boRevisedObj = domAffectedItem.revise(context, nextRev, lastRevVault);

                                } finally {
                                    context.setCustomData("PSS_PART_REVISE", "FALSE");
                                    context.removeFromCustomData("PSS_PART_REVISE");
                                }
                                // TIGTK-8886 - PTE - 2017-07-7 - END
                                DomainObject domRevisedAffectedItem = new DomainObject(boRevisedObj);

                                // PCM TIGTK-3209 | 29/09/16 : Pooja Mantri : Start
                                domRevisedAffectedItem.setOwner(context, strOwner);
                                // PCM TIGTK-3209 | 26/09/16 : Pooja Mantri : Start
                                domRevisedAffectedItem.setAttributeValue(context, DomainConstants.ATTRIBUTE_ORIGINATOR, strOwner);
                                // PCM TIGTK-3209 | 26/09/16 : Pooja Mantri : End

                                ContextUtil.popContext(context);

                                strNextRevId = domRevisedAffectedItem.getId();

                                if (UIUtil.isNotNullAndNotEmpty(strNextRevId)) {

                                    // Connect Revised affected Item and It's symmetrical part (TIGTK-3654 : PCM : 02/01/2017 : AB)
                                    this.connectLatestRevisionsOfPartAndSymmetrical(context, strAffectedItemID, strNextRevId);

                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    boolean bSubProject = domRevisedAffectedItem.hasRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, false);
                                    DomainRelationship domRel = new DomainRelationship();
                                    if (!bSubProject) {
                                        domRel = DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domRevisedAffectedItem);
                                    }
                                    ContextUtil.popContext(context);
                                    isSendMail = true;// TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 17/03/2017 |Harika Varanasi
                                    // PCM : TIGTK-7619 : 16/03/2017 : VB : START
                                    getTriggerAction(context, domRel);
                                    // PCM : TIGTK-7619 : 16/03/2017 : VB : END
                                }
                            }

                            else {
                                // TIGTK-6849:Phase-2.0:PKH:Start
                                BusinessObject bomAffectedItem = domAffectedItem.getLastRevision(context);
                                String strLastRevPolicy = bomAffectedItem.getPolicy(context).getName();
                                if (UIUtil.isNotNullAndNotEmpty(strLastRevPolicy) && TigerConstants.POLICY_PSS_CANCELPART.equals(strLastRevPolicy)) {
                                    String strNextRev = bomAffectedItem.getNextSequence(context);
                                    Map attrMapLatestReleasedAffectedItem = domAffectedItem.getAttributeMap(context, true);
                                    ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    BusinessObject boRevisedObj = new BusinessObject();
                                    try {
                                        context.setCustomData("PSS_PART_REVISE", "TRUE");
                                        boRevisedObj = domAffectedItem.reviseObject(context, strNextRev, true);
                                    } finally {
                                        context.setCustomData("PSS_PART_REVISE", "FALSE");
                                        context.removeFromCustomData("PSS_PART_REVISE");
                                    }

                                    DomainObject domRevisedAffectedItem = DomainObject.newInstance(context, boRevisedObj);
                                    // PTE : TIGTK- 16429 : START
                                    domRevisedAffectedItem.setOwner(context, strOwner);
                                    domRevisedAffectedItem.setAttributeValue(context, DomainConstants.ATTRIBUTE_ORIGINATOR, strOwner);
                                    // PTE : TIGTK- 16429 : END

                                    domRevisedAffectedItem.setAttributeValues(context, attrMapLatestReleasedAffectedItem);
                                    domRevisedAffectedItem.setPolicy(context, TigerConstants.POLICY_PSS_ECPART);
                                    strNextRevId = domRevisedAffectedItem.getId();
                                    boolean bSubProject = domRevisedAffectedItem.hasRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, false);
                                    DomainRelationship domImplementedItemRel = new DomainRelationship();
                                    if (!bSubProject) {
                                        domImplementedItemRel = DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domRevisedAffectedItem);
                                    }
                                    ContextUtil.popContext(context);
                                    getTriggerAction(context, domImplementedItemRel);
                                } else {
                                    // TIGTK-6849:Phase-2.0:PKH:End
                                    BusinessObject busNextRev = domAffectedItem.getNextRevision(context);
                                    DomainObject domNextRev = new DomainObject(busNextRev);
                                    strNextRevId = domNextRev.getId();
                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    boolean bSubProject = domNextRev.hasRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, true);
                                    if (!bSubProject) {
                                        DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domNextRev);
                                    }
                                    ContextUtil.popContext(context);
                                }
                                isSendMail = true;// TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 17/03/2017 |Harika Varanasi
                            }

                            disconnectOriginalStructWithNewImplementItem(context, strNextRevId);
                            slForRevisedPart.add(strNextRevId);
                            connectReviseItemWithChild(context, strAffectedItemID, strNextRevId, strCOId);
                        }

                        // PCM TIGTK-3119 | 15/09/16 : AB : START
                        // If Requested Change is For Revise Type of Object is CAD
                        if ((ChangeConstants.FOR_REVISE.equalsIgnoreCase(strRequestedChangeAttrValue)) && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT))) {
                            boolean bIsLastRev = domAffectedItem.isLastRevision(context);
                            String strNextRevId = DomainConstants.EMPTY_STRING;
                            if (bIsLastRev) {
                                // Revise CAD Object and connect with Change Action
                                BusinessObject bus = new BusinessObject(strAffectedItemID);
                                // TIGTK-14267 - START
                                bus.open(context);
                                String rev = bus.getNextSequence(context);
                                BusinessObject busObject = bus.revise(context, rev, TigerConstants.VAULT_ESERVICEPRODUCTION);
                                bus.update(context);
                                bus.close(context);
                                // TIGTK-14267 - END
                                DomainObject domRevisedAffectedItem = new DomainObject(busObject);

                                slPropogatePartSpecification.add((String) domRevisedAffectedItem.getId(context));
                                domRevisedAffectedItem.setOwner(context, strOwner);
                                // PTE : TIGTK- 16429 : START
                                domRevisedAffectedItem.setAttributeValue(context, DomainConstants.ATTRIBUTE_ORIGINATOR, strOwner);
                                // PTE : TIGTK- 16429 : END

                                strNextRevId = domRevisedAffectedItem.getId(context);
                                if (UIUtil.isNotNullAndNotEmpty(strNextRevId)) {
                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    DomainRelationship domRel = DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domRevisedAffectedItem);
                                    ContextUtil.popContext(context);
                                    isSendMail = true;// TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 17/03/2017 |Harika Varanasi
                                    // PCM : TIGTK-7619 : 16/03/2017 : VB : START
                                    getTriggerAction(context, domRel);
                                    // PCM : TIGTK-7619 : 16/03/2017 : VB : END
                                }

                            } else {
                                // TIGTK-6849:Phase-2.0:PKH:Start
                                BusinessObject bomAffectedItem = domAffectedItem.getLastRevision(context);
                                String strLastRevPolicy = bomAffectedItem.getPolicy(context).getName();

                                if (UIUtil.isNotNullAndNotEmpty(strLastRevPolicy) && TigerConstants.POLICY_PSS_CANCELCAD.equals(strLastRevPolicy)) {
                                    String strNextRev = bomAffectedItem.getNextSequence(context);
                                    Map attrMapLatestReleasedAffectedItem = domAffectedItem.getAttributeMap(context, true);
                                    // TIGTK-14267 START
                                    domAffectedItem.open(context);

                                    ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    BusinessObject boRevisedObj = domAffectedItem.reviseObject(context, strNextRev, true);
                                    DomainObject domRevisedAffectedItem = DomainObject.newInstance(context, boRevisedObj);

                                    // PTE : TIGTK- 16429 : START
                                    domRevisedAffectedItem.setOwner(context, strOwner);
                                    domRevisedAffectedItem.setAttributeValue(context, DomainConstants.ATTRIBUTE_ORIGINATOR, strOwner);
                                    // PTE : TIGTK- 16429 : END
                                    slPropogatePartSpecification.add((String) domRevisedAffectedItem.getId(context));
                                    ContextUtil.popContext(context);

                                    domAffectedItem.close(context);
                                    // TIGTK-14267 - END
                                    ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    domRevisedAffectedItem.setAttributeValues(context, attrMapLatestReleasedAffectedItem);
                                    domRevisedAffectedItem.setPolicy(context, TigerConstants.POLICY_PSS_CADOBJECT);
                                    strNextRevId = domRevisedAffectedItem.getId(context);
                                    DomainRelationship domImplementedItemRel = DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domRevisedAffectedItem);
                                    ContextUtil.popContext(context);
                                    getTriggerAction(context, domImplementedItemRel);
                                } else {
                                    // TIGTK-6849:Phase-2.0:PKH:End
                                    BusinessObject busNextRev = domAffectedItem.getNextRevision(context);
                                    DomainObject domNextRev = new DomainObject(busNextRev);
                                    strNextRevId = domNextRev.getId(context);
                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domNextRev);
                                    ContextUtil.popContext(context);
                                }
                                isSendMail = true;// TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 17/03/2017 |Harika Varanasi
                            }
                            disconnectOriginalStructWithNewImplementItem(context, strNextRevId);
                            connectReviseItemWithChild(context, strAffectedItemID, strNextRevId, strCOId);
                        }
                        // PCM TIGTK-3119 | 15/09/16 : AB : END
                        // If Requested Change is For Revise Type of Object is Standard Part
                        if ((ChangeConstants.FOR_REVISE.equalsIgnoreCase(strRequestedChangeAttrValue)) && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART))) {

                            boolean bIsLastRev = domAffectedItem.isLastRevision(context);
                            String strNextRevId = DomainConstants.EMPTY_STRING;
                            if (bIsLastRev) {

                                String nextRev = domAffectedItem.getNextSequence(context);
                                String lastRevVault = domAffectedItem.getVault();
                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                BusinessObject boRevisedObj = domAffectedItem.revise(context, nextRev, lastRevVault);
                                DomainObject domRevisedAffectedItem = new DomainObject(boRevisedObj);
                                domRevisedAffectedItem.setOwner(context, strOwner);
                                // PTE : TIGTK- 16429 : START
                                domRevisedAffectedItem.setAttributeValue(context, DomainConstants.ATTRIBUTE_ORIGINATOR, strOwner);
                                // PTE : TIGTK- 16429 : END
                                ContextUtil.popContext(context);

                                strNextRevId = domRevisedAffectedItem.getId();

                                if (UIUtil.isNotNullAndNotEmpty(strNextRevId)) {

                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    DomainRelationship domRelImplement = DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domRevisedAffectedItem);
                                    isSendMail = true;
                                    ContextUtil.popContext(context);
                                    // PCM : TIGTK-7619 : 16/03/2017 : VB : START
                                    getTriggerAction(context, domRelImplement);
                                    // PCM : TIGTK-7619 : 16/03/2017 : VB : END
                                }
                            } else {
                                // TIGTK-6849:Phase-2.0:PKH:Start

                                BusinessObject bomAffectedItem = domAffectedItem.getLastRevision(context);
                                String strLastRevPolicy = bomAffectedItem.getPolicy(context).getName();
                                if (UIUtil.isNotNullAndNotEmpty(strLastRevPolicy) && TigerConstants.POLICY_PSS_CANCELPART.equals(strLastRevPolicy)) {
                                    String strNextRev = bomAffectedItem.getNextSequence(context);
                                    Map attrMapLatestReleasedAffectedItem = domAffectedItem.getAttributeMap(context, true);
                                    ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                    BusinessObject boRevisedObj = domAffectedItem.reviseObject(context, strNextRev, true);
                                    DomainObject domRevisedAffectedItem = DomainObject.newInstance(context, boRevisedObj);
                                    // PTE : TIGTK- 16429 : START
                                    domRevisedAffectedItem.setOwner(context, strOwner);
                                    domRevisedAffectedItem.setAttributeValue(context, DomainConstants.ATTRIBUTE_ORIGINATOR, strOwner);
                                    // PTE : TIGTK- 16429 : END
                                    domRevisedAffectedItem.setAttributeValues(context, attrMapLatestReleasedAffectedItem);
                                    domRevisedAffectedItem.setPolicy(context, TigerConstants.POLICY_STANDARDPART);
                                    DomainRelationship domImplementedItemRel = DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domRevisedAffectedItem);
                                    ContextUtil.popContext(context);
                                    getTriggerAction(context, domImplementedItemRel);
                                    strNextRevId = domRevisedAffectedItem.getId();
                                } else {
                                    // TIGTK-6849:Phase-2.0:PKH:End
                                    BusinessObject busNextRev = domAffectedItem.getNextRevision(context);
                                    DomainObject domNextRev = new DomainObject(busNextRev);
                                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING,
                                            DomainConstants.EMPTY_STRING);
                                    DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domNextRev);
                                    ContextUtil.popContext(context);
                                    strNextRevId = domNextRev.getId();
                                }
                                isSendMail = true;// TGPSS_PCM-TS153 Change Actions Notifications_V2.1 | 17/03/2017 |Harika Varanasi
                            }
                            disconnectOriginalStructWithNewImplementItem(context, strNextRevId);
                            slForRevisedPart.add(strNextRevId);
                            connectReviseItemWithChild(context, strAffectedItemID, strNextRevId, strCOId);
                        }
                        // If Requested Change is For Clone
                        if (TigerConstants.FOR_CLONE.equalsIgnoreCase(strRequestedChangeAttrValue)) {
                            String strClone = "Clone";
                            String strClonedObjectID = getClonedObjectId(context, strAffectedItemID, strClone);
                            if (UIUtil.isNotNullAndNotEmpty(strClonedObjectID)) {
                                DomainObject domClonedAffectedItem = DomainObject.newInstance(context, strClonedObjectID);

                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                if (TigerConstants.POLICY_PSS_CADOBJECT.equalsIgnoreCase(domClonedAffectedItem.getPolicy(context).getName())) {
                                    DomainRelationship.connect(context, domAffectedItem, TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD, domClonedAffectedItem);
                                } else {
                                    DomainRelationship domRelDerived = DomainRelationship.connect(context, domAffectedItem, TigerConstants.RELATIONSHIP_DERIVED, domClonedAffectedItem);
                                    domRelDerived.setAttributeValue(context, TigerConstants.ATTRIBUTE_DERIVED_CONTEXT, TigerConstants.PSS_FOR_CLONE);

                                }
                                DomainRelationship domRelImplement = DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domClonedAffectedItem);
                                slForReplacedorClonedItemsofCurrentCO.add(strClonedObjectID);
                                isSendMail = true;
                                disconnectOriginalStructWithNewImplementItem(context, strClonedObjectID);
                                // Added New Clone Oject to stringList for CAD ani related part connection
                                slPropogatePartSpecificationForClone.add(strClonedObjectID);
                                performActionOnReplaceOrClone(context, strClonedObjectID, strAffectedItemID, strCOId, strRequestedChangeAttrValue);

                                ContextUtil.popContext(context);
                                getTriggerAction(context, domRelImplement);
                            }
                        }
                        // If Requested Change is For Replace
                        if (TigerConstants.FOR_REPLACE.equalsIgnoreCase(strRequestedChangeAttrValue)) {
                            String strReplace = "Replace";
                            String strClonedObjectID = getClonedObjectId(context, strAffectedItemID, strReplace);
                            DomainObject domClonedAffectedItem = DomainObject.newInstance(context, strClonedObjectID);
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            if (TigerConstants.POLICY_PSS_CADOBJECT.equalsIgnoreCase(domClonedAffectedItem.getPolicy(context).getName())) {
                                DomainRelationship.connect(context, domAffectedItem, TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD, domClonedAffectedItem);
                            } else {
                                DomainRelationship domRelDerived = DomainRelationship.connect(context, domAffectedItem, TigerConstants.RELATIONSHIP_DERIVED, domClonedAffectedItem);
                                domRelDerived.setAttributeValue(context, TigerConstants.ATTRIBUTE_DERIVED_CONTEXT, TigerConstants.PSS_FOR_REPLACE);
                            }
                            DomainRelationship domRelImplement = DomainRelationship.connect(context, domobjCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domClonedAffectedItem);
                            slForReplacedorClonedItemsofCurrentCO.add(strClonedObjectID);
                            isSendMail = true;
                            disconnectOriginalStructWithNewImplementItem(context, strClonedObjectID);
                            // Added New Clone Oject to stringList for CAD ani related part connection
                            slPropogatePartSpecificationForClone.add(strClonedObjectID);
                            performActionOnReplaceOrClone(context, strClonedObjectID, strAffectedItemID, strCOId, strRequestedChangeAttrValue);

                            ContextUtil.popContext(context);
                            getTriggerAction(context, domRelImplement);

                            // Previous Connection

                            MapList mCAlistPrevious = domAffectedItem.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, DomainConstants.QUERY_WILDCARD,
                                    slObjectSelectStatements, slRelationshipSelectStatement, true, false, (short) 1, null, null, 0);

                            if (!mCAlistPrevious.isEmpty()) {
                                Iterator ItrPriviousCA = mCAlistPrevious.iterator();
                                while (ItrPriviousCA.hasNext()) {
                                    Map mCAPreviousObj = (Map) ItrPriviousCA.next();
                                    String strCAPrevID = (String) mCAPreviousObj.get(DomainConstants.SELECT_ID);

                                    String strselConnectionIdPrevious = "from[Change Affected Item|to.id==" + strAffectedItemID + "].id";

                                    String strMQLcommand = "print bus " + strCAPrevID + " select '" + strselConnectionIdPrevious + "' dump";

                                    String strConnectionIdPrevious = MqlUtil.mqlCommand(context, strMQLcommand);

                                    // Added : 07/11/2016
                                    if (UIUtil.isNotNullAndNotEmpty(strConnectionIdPrevious)) {

                                        // Added : 07/11/2016
                                        iReplaceFlag = 1;
                                        slConnectionIdList.addElement(strConnectionIdPrevious);
                                    }
                                }
                            }
                        }

                    }
                    if (!slForRevisedPart.isEmpty() && !slForDisconnectPSfromPart.isEmpty()) {
                        for (int j = 0; j < slForRevisedPart.size(); j++) {
                            DomainObject domPart = DomainObject.newInstance(context, (String) slForRevisedPart.get(j));
                            String strConnectedCAD = MqlUtil.mqlCommand(context, "print bus " + (String) slForRevisedPart.get(j) + " select from[" + RELATIONSHIP_PART_SPECIFICATION + ","
                                    + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].to.id dump |", false, false);
                            StringList slConnectedCAD = FrameworkUtil.split(strConnectedCAD, "|");
                            if (!slConnectedCAD.isEmpty()) {
                                for (int k = 0; k < slConnectedCAD.size(); k++) {
                                    if (slForDisconnectPSfromPart.contains((String) slConnectedCAD.get(k))) {
                                        String strRelIdExists = MqlUtil.mqlCommand(context, "print bus " + (String) slForRevisedPart.get(j) + " select from[" + RELATIONSHIP_PART_SPECIFICATION + ","
                                                + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "| to.id == '" + (String) slConnectedCAD.get(k) + "'].id dump", false, false);
                                        if (UIUtil.isNotNullAndNotEmpty(strRelIdExists))
                                            DomainRelationship.disconnect(context, strRelIdExists);
                                    }
                                }

                            }
                        }

                    }
                    if (iReplaceFlag == 1 && !slConnectionIdList.isEmpty()) {
                        for (int i = 0; i < slConnectionIdList.size(); i++) {
                            String strRelId = (String) slConnectionIdList.get(i);
                            slForConnectCADToPartonReplace.add(strRelId);

                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            MqlUtil.mqlCommand(context, "trigger off");
                            DomainRelationship.setAttributeValue(context, strRelId, TigerConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_OBSOLESCENCE);
                            MqlUtil.mqlCommand(context, "trigger on");
                            ContextUtil.popContext(context);

                        }

                    }

                    // mpPartName.clear();

                    // Promote Change Actions connected to the Change Order to In Work state.
                    context.setCustomData("isPromoteFromCOAction", "true");
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    // PCM TIGTK-3281 | 07/10/16 : Pooja Mantri : Start
                    domobjCA.setState(context, "In Work");
                    ContextUtil.popContext(context);

                    // PCM TIGTK-3281 | 04/10/16 : Pooja Mantri : End
                    context.setCustomData("isPromoteFromCOAction", "");

                }
            }
            // TIGTK-3318 :Rutuja Ekatpure:8/10/2016:Start
            promoteCOActionToReviewState(context, mlCAAndAffectedItem);
            // TIGTK-3318 :Rutuja Ekatpure:8/10/2016:End

            // PCM : TIGTK-3909 : 06/03/2017 : AB : START
            if (!slPropogatePartSpecification.isEmpty()) {
                int intSize = slPropogatePartSpecification.size();
                for (int k = 0; k < intSize; k++) {
                    Object[] argsJPO = new Object[4];
                    argsJPO[0] = (String) slPropogatePartSpecification.get(k);
                    argsJPO[1] = strCOId;
                    argsJPO[2] = slForObsolescencePart;
                    argsJPO[3] = slForRevisedPart;
                    dectgutilJPO.propagatePartSpecificationTonewRev(context, argsJPO);
                }
            }
            // PCM : TIGTK-3909 : 06/03/2017 : AB : END
            // PCM RFC-074 : 29/09/2016 : KWagh : End

            // Connect CAD to Related Part

            if (!mpForConnectCADToPart.isEmpty()) {
                Iterator<Entry<Integer, String>> itr = mpForConnectCADToPart.entrySet().iterator();

                while (itr.hasNext()) {
                    Entry e = itr.next();
                    int strKey = (int) e.getKey();
                    String strCADObjDetails = (String) mpForConnectCADToPart.get(strKey);

                    String strArr[] = strCADObjDetails.split(":");
                    String domPart = strArr[0];
                    String domNewImplementPart = strArr[1];
                    String strRelName = strArr[2];
                    String strCADObj = strArr[3];

                    String[] argsJPO = new String[5];
                    argsJPO[0] = strCADObj;
                    argsJPO[1] = domPart;
                    argsJPO[2] = domNewImplementPart;
                    argsJPO[3] = strRelName;

                    propagatePartSpecificationToNewCloneObj(context, argsJPO);

                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in performTransitionOnCOPromote: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        } finally {
            // TIGTK-8543 - PTE - 2017-7-18 - START
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_REVISE_FROM_PCM", "");
            // TIGTK-14289 - START
            mpPartName.clear();
            // TIGTK-14289 - END
        }
        // TIGTK-8543 - PTE - 2017-7-18 - END

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is invoked via a promote check trigger when the connected Affected Item (CAD or Control Document) to a Change Action is promoted by the user. Checks whether the CAD has
     *             files checked-in
     */
    public int checkAtleastOneFileCheckedIn(Context context, String args[]) throws Exception {

        if (args == null || args.length < 10) {
            throw new IllegalArgumentException();
        }

        int isreturn = 0;

        try {

            String strCADId = args[0];

            String strMessage;
            DomainObject domCADObj = new DomainObject(strCADId);
            // PCM RFC-074 : 27/09/2016 : KWagh : Start
            String sName = domCADObj.getInfo(context, DomainConstants.SELECT_NAME);
            String sType = domCADObj.getInfo(context, DomainConstants.SELECT_TYPE);
            String sRev = domCADObj.getInfo(context, DomainConstants.SELECT_REVISION);
            String strActualType = i18nNow.getTypeI18NString(sType, context.getSession().getLanguage());

            FileList filelist = domCADObj.getFiles(context);

            boolean bResult = filelist.isEmpty();
            // int i = filelist.size();
            // PCM RFC-074 : 27/09/2016 : KWagh : End
            if (bResult) {

                strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.OneFileCheckedIn");
                strMessage = strMessage + "  " + strActualType + " " + sName + "  " + sRev;

                MqlUtil.mqlCommand(context, "notice $1", strMessage);

                return 1;
            } else {
                // do nothing
            }

        } catch (Exception e) {

            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkAtleastOneFileCheckedIn: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

        return isreturn;

    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is invoked via a promote check trigger when the Part (connected as an Affected Item to a Change Action) is promoted by the user. Checks whether the CAD is connected to
     *             the Part
     */
    public int checkConnectedCADDrawingAndControlDocumentInReviewOrBeyond(Context context, String args[]) throws Exception {

        if (args == null || args.length < 10) {
            throw new IllegalArgumentException();
        }

        int isreturn = 0;

        try {

            String strPartId = args[0];

            String strMessage;
            String strCADID;
            String strCurrentSate;
            DomainObject domPartObj = new DomainObject(strPartId);

            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_PartSpecification");

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            // PCM RFC-074 : 27/09/2016 : KWagh : Start
            MapList mConnectList = domPartObj.getRelatedObjects(context, relpattern, "*", slObjectSle, slRelSle, false, true, (short) 1, null, null, 0);

            if (mConnectList.isEmpty()) {
                // do nothing
                strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ConnectedAtleastOneCADToThePart");

                MqlUtil.mqlCommand(context, "notice $1", strMessage);

                return 1;

            } else {

                if (mConnectList.size() > 0) {
                    for (int i = 0; i < mConnectList.size(); i++) {
                        Map mCADObj = (Map) mConnectList.get(i);
                        strCADID = (String) mCADObj.get(DomainConstants.SELECT_ID);
                        strCurrentSate = (String) mCADObj.get(DomainConstants.SELECT_CURRENT);
                        DomainObject domCADObj = new DomainObject(strCADID);

                        StringList strListCADStates = domCADObj.getInfoList(context, SELECT_STATES);

                        int indexApprovedState = strListCADStates.indexOf("Review");
                        int indexOfCurrentSate = strListCADStates.indexOf(strCurrentSate);
                        // PCM RFC-074 : 27/09/2016 : KWagh : End
                        if (indexOfCurrentSate < indexApprovedState) {

                            strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                    "PSS_EnterpriseChangeMgt.Alert.ConnectedCADControlDocumentIsNotInReviewStateOrBeyond");

                            MqlUtil.mqlCommand(context, "notice $1", strMessage);
                            return 1;
                        }

                    }
                }
            }

        } catch (Exception e) {

            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkConnectedCADDrawingAndControlDocumentInReviewOrBeyond: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

        return isreturn;

    }

    /**
     * @param context
     * @param args
     * @throws Exception
     *             Modified By PCM : 12/12/2016 - TIGTK:3500 : AB This method is invoked via a promote action trigger when the connected Affected Item to a Change Action is promoted by the user
     */
    public void promoteChangeActionIfAffectedItemPromotedIsLast(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        // PCM RFC-074 : 30/09/2016 : KWagh : Start
        // Added by Kalpesh for CA Route creation issue-Start
        boolean contextPush = false;
        // Added by Kalpesh for CA Route creation issue-End
        try {
            // Added by Kalpesh for CA Route creation issue-Start
            String contextUser = context.getUser();
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            context.setCustomData("contextUser", contextUser);
            contextPush = true;
            // Added by Kalpesh for CA Route creation issue-End

            String strCAID = DomainConstants.EMPTY_STRING;
            String strCADID;
            String strPartID;
            String strPartCurrent;
            String strCADCurrent;
            Map mPartObj;
            String strObjId = args[0];
            HashSet<String> stateSet = new HashSet<>();

            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAffectedItem");
            String relpatternImplemented = PropertyUtil.getSchemaProperty(context, "relationship_ImplementedItem");
            // String typePattern = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
            slObjectSle.addElement(DomainConstants.SELECT_POLICY);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            slRelSle.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

            DomainObject domObj = new DomainObject(strObjId);
            String strType = domObj.getInfo(context, DomainConstants.SELECT_TYPE);
            String strPolicy = domObj.getInfo(context, DomainConstants.SELECT_POLICY);

            // TIGTK-8232 : PTE : 06-08-2017 : START
            StringList slCAIDList = domObj.getInfoList(context, "to[" + relpattern + "].from.id");
            for (int p = 0; p < slCAIDList.size(); p++) {
                String strChangeActionId = (String) slCAIDList.get(p);
                DomainObject domobjCA = new DomainObject(strChangeActionId);
                String strCAState = domobjCA.getInfo(context, DomainConstants.SELECT_CURRENT);
                if (!TigerConstants.STATE_CHANGEACTION_CANCELLED.equals(strCAState)) {
                    strCAID = strChangeActionId;
                }
            }
            // TIGTK-8232 : PTE : 06-08-2017 : END
            if (UIUtil.isNullOrEmpty(strCAID)) {
                // TIGTK-8232 : PTE : 06-08-2017 : START
                StringList slCAObjIDList = domObj.getInfoList(context, "to[" + relpatternImplemented + "].from.id");
                for (int p = 0; p < slCAObjIDList.size(); p++) {
                    String strChangeActionId = (String) slCAObjIDList.get(p);
                    DomainObject domobjCA = new DomainObject(strChangeActionId);
                    String strCAState = domobjCA.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if (!TigerConstants.STATE_CHANGEACTION_CANCELLED.equals(strCAState)) {
                        strCAID = strChangeActionId;
                    }
                }
                // TIGTK-8232 : PTE : 06-08-2017 : END
            }

            if (UIUtil.isNotNullAndNotEmpty(strCAID)) {
                DomainObject domobjCA = new DomainObject(strCAID);
                String strCACurrent = domobjCA.getInfo(context, DomainConstants.SELECT_CURRENT);

                if (TigerConstants.STATE_CHANGEACTION_COMPLETE.equalsIgnoreCase(strCACurrent)) {
                    StringList strCAIDS = domObj.getInfoList(context, "to[" + relpattern + "].from.id");

                    for (int i = 0; i < strCAIDS.size(); i++) {
                        String strCAId = (String) strCAIDS.get(i);
                        domobjCA = new DomainObject(strCAId);
                        String strCAState = domobjCA.getInfo(context, DomainConstants.SELECT_CURRENT);

                        if (!TigerConstants.STATE_CHANGEACTION_COMPLETE.equalsIgnoreCase(strCAState)) {
                            strCACurrent = strCAState;
                            break;
                        }
                    }
                }

                if (DomainConstants.TYPE_PART.equalsIgnoreCase(strType)) {

                    MapList mlConnectedParts = domobjCA.getRelatedObjects(context, relpattern, DomainConstants.TYPE_PART, slObjectSle, slRelSle, false, true, (short) 1, null, null, 0);

                    MapList mlConnectedImplementedParts = domobjCA.getRelatedObjects(context, relpatternImplemented, DomainConstants.TYPE_PART, slObjectSle, slRelSle, false, true, (short) 1, null,
                            null, 0);

                    if (!mlConnectedImplementedParts.isEmpty()) {
                        mlConnectedParts.addAll(mlConnectedImplementedParts);
                    }

                    if (!mlConnectedParts.isEmpty()) {
                        if (mlConnectedParts.size() == 1) {

                            for (int j = 0; j < mlConnectedParts.size(); j++) {
                                mPartObj = (Map) mlConnectedParts.get(j);
                                strPartID = (String) mPartObj.get(DomainConstants.SELECT_ID);
                                if (strPartID.equalsIgnoreCase(strObjId) && strCACurrent.equalsIgnoreCase("In Work")) {
                                    // Promote Change Action to In Review state
                                    domobjCA.setState(context, "In Approval");
                                }
                            }
                        } else {
                            for (int k = 0; k < mlConnectedParts.size(); k++) {
                                mPartObj = (Map) mlConnectedParts.get(k);
                                strPartID = (String) mPartObj.get(DomainConstants.SELECT_ID);
                                strPartCurrent = (String) mPartObj.get(DomainConstants.SELECT_CURRENT);

                                if (!strPartID.equalsIgnoreCase(strObjId)) {
                                    stateSet.add(strPartCurrent);
                                }
                            }

                            if (stateSet.size() > 1) {
                                if (stateSet.size() == 2 && stateSet.contains(TigerConstants.STATE_PART_RELEASE) && stateSet.contains(TigerConstants.STATE_PART_REVIEW)
                                        && strCACurrent.equalsIgnoreCase("In Work")) {
                                    // Promote Change Action to In Review state
                                    domobjCA.setState(context, "In Approval");
                                } else if (stateSet.size() == 2 && stateSet.contains(TigerConstants.STATE_PART_RELEASE) && stateSet.contains(TigerConstants.STATE_PART_APPROVED)
                                        && strCACurrent.equalsIgnoreCase("In Work")) {
                                    // Promote Change Action to In Review state
                                    domobjCA.setState(context, "In Approval");
                                }
                            } else {
                                if (stateSet.iterator().hasNext()) {
                                    String sState = stateSet.iterator().next();

                                    if ((TigerConstants.STATE_PART_REVIEW.equalsIgnoreCase(sState) || TigerConstants.STATE_PART_RELEASE.equalsIgnoreCase(sState)
                                            || TigerConstants.STATE_PART_APPROVED.equalsIgnoreCase(sState)) && strCACurrent.equalsIgnoreCase("In Work")) {
                                        // Promote Change Action to In Review state
                                        domobjCA.setState(context, "In Approval");

                                    }
                                }

                            }

                        }
                    }
                } else if (TigerConstants.POLICY_PSS_CADOBJECT.equalsIgnoreCase(strPolicy)) {
                    Map mCADObj;

                    // TIGTK-11877 : Updating this where clause so as not to fetch Obsolete state CAD objects : START
                    String objectWhere = "policy ==" + "\"" + TigerConstants.POLICY_PSS_CADOBJECT + "\" && current != Obsolete";
                    // TIGTK-11877 : Updating this where clause so as not to fetch Obsolete state CAD objects : END

                    MapList mlConnectedCADs = domobjCA.getRelatedObjects(context, relpattern, "*", slObjectSle, slRelSle, false, true, (short) 1, objectWhere, null);

                    MapList mlConnectedImplementedCADs = domobjCA.getRelatedObjects(context, relpatternImplemented, "*", slObjectSle, slRelSle, false, true, (short) 1, objectWhere, null);

                    if (!mlConnectedImplementedCADs.isEmpty()) {
                        mlConnectedCADs.addAll(mlConnectedImplementedCADs);
                    }

                    if (!mlConnectedCADs.isEmpty()) {
                        if (mlConnectedCADs.size() == 1) {
                            // as updated in where clause for state, if the revision of the CAD object is generated then the mlConnectedCADs list will contain the revised CAD object
                            // and based on the revised CAD objects state connected CA will be promoted
                            for (int j = 0; j < mlConnectedCADs.size(); j++) {
                                mCADObj = (Map) mlConnectedCADs.get(j);
                                strCADID = (String) mCADObj.get(DomainConstants.SELECT_ID);

                                if (strCADID.equalsIgnoreCase(strObjId) && strCACurrent.equalsIgnoreCase("In Work")) {
                                    // Promote Change Action to In Review state
                                    domobjCA.setState(context, "In Approval");
                                }
                            }
                        } else {
                            for (int k = 0; k < mlConnectedCADs.size(); k++) {
                                mCADObj = (Map) mlConnectedCADs.get(k);
                                strCADID = (String) mCADObj.get(DomainConstants.SELECT_ID);
                                strCADCurrent = (String) mCADObj.get(DomainConstants.SELECT_CURRENT);
                                if (!(strCADID.equalsIgnoreCase(strObjId))) {
                                    stateSet.add(strCADCurrent);
                                }
                            }

                            if (stateSet.size() > 1) {
                                if (stateSet.size() == 2 && stateSet.contains(TigerConstants.STATE_RELEASED_CAD_OBJECT) && stateSet.contains(TigerConstants.STATE_CAD_REVIEW)
                                        && strCACurrent.equalsIgnoreCase("In Work")) {
                                    // Promote Change Action to In Review state
                                    domobjCA.setState(context, "In Approval");
                                } else if (stateSet.size() == 2 && stateSet.contains(TigerConstants.STATE_RELEASED_CAD_OBJECT) && stateSet.contains(TigerConstants.STATE_CAD_APPROVED)
                                        && strCACurrent.equalsIgnoreCase("In Work")) {
                                    // Promote Change Action to In Review state
                                    domobjCA.setState(context, "In Approval");
                                }
                            } else {
                                if (stateSet.iterator().hasNext()) {
                                    String sState = stateSet.iterator().next();

                                    if ((TigerConstants.STATE_CAD_REVIEW.equalsIgnoreCase(sState) || TigerConstants.STATE_RELEASED_CAD_OBJECT.equalsIgnoreCase(sState)
                                            || TigerConstants.STATE_CAD_APPROVED.equalsIgnoreCase(sState)) && strCACurrent.equalsIgnoreCase("In Work")) {
                                        // Promote Change Action to In Review state
                                        domobjCA.setState(context, "In Approval");
                                    }
                                }

                            }
                        }
                    }
                } else if (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_Legacy_CAD)) {
                    Map mCADObj;

                    String objectWhere = "policy ==" + "\"" + TigerConstants.POLICY_PSS_Legacy_CAD + "\"";

                    MapList mlConnectedCADs = domobjCA.getRelatedObjects(context, relpattern, "*", slObjectSle, slRelSle, false, true, (short) 1, objectWhere, null, 0);

                    MapList mlConnectedImplementedCADs = domobjCA.getRelatedObjects(context, relpatternImplemented, "*", slObjectSle, slRelSle, false, true, (short) 1, objectWhere, null, 0);

                    if (!mlConnectedImplementedCADs.isEmpty()) {
                        mlConnectedCADs.addAll(mlConnectedImplementedCADs);
                    }

                    if (!mlConnectedCADs.isEmpty()) {
                        if (mlConnectedCADs.size() == 1) {

                            for (int j = 0; j < mlConnectedCADs.size(); j++) {
                                mCADObj = (Map) mlConnectedCADs.get(j);
                                strCADID = (String) mCADObj.get(DomainConstants.SELECT_ID);

                                if (strCADID.equalsIgnoreCase(strObjId) && strCACurrent.equalsIgnoreCase("In Work")) {
                                    // Promote Change Action to In Review state
                                    domobjCA.setState(context, "In Approval");
                                }
                            }
                        } else {
                            for (int k = 0; k < mlConnectedCADs.size(); k++) {
                                mCADObj = (Map) mlConnectedCADs.get(k);
                                strCADID = (String) mCADObj.get(DomainConstants.SELECT_ID);
                                strCADCurrent = (String) mCADObj.get(DomainConstants.SELECT_CURRENT);

                                if (!(strCADID.equalsIgnoreCase(strObjId))) {
                                    stateSet.add(strCADCurrent);
                                }
                            }

                            if (stateSet.size() > 1) {
                                if (stateSet.size() == 2 && stateSet.contains(TigerConstants.STATE_RELEASED_CAD_OBJECT) && stateSet.contains(TigerConstants.STATE_CAD_REVIEW)
                                        && strCACurrent.equalsIgnoreCase("In Work")) {
                                    // Promote Change Action to In Review state
                                    domobjCA.setState(context, "In Approval");
                                }
                            } else {
                                if (stateSet.iterator().hasNext()) {
                                    String sState = stateSet.iterator().next();

                                    if ((TigerConstants.STATE_CAD_REVIEW.equalsIgnoreCase(sState) || TigerConstants.STATE_RELEASED_CAD_OBJECT.equalsIgnoreCase(sState))
                                            && strCACurrent.equalsIgnoreCase("In Work")) {
                                        // Promote Change Action to In Review state
                                        domobjCA.setState(context, "In Approval");
                                    }
                                }

                            }
                        }
                    }
                }

            } else {
                // Do Nothing

            }
            // PCM TIGTK-3040 | 15/09/16 : AB : END
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteChangeActionIfAffectedItemPromotedIsLast: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
            // Added by Kalpesh for CA Route creation issue-Start
        } finally {
            if (contextPush) {
                ContextUtil.popContext(context);
            }
            // Added by Kalpesh for CA Route creation issue-End
            // PCM RFC-074 : 30/09/2016 : KWagh : End
        }

    }

    /**
     * @param context
     * @param args
     * @throws Exception
     *             This method is invoked via a promote action trigger when the Change Action is promoted by the system.
     */
    public void promoteChangeOrderIfChangeActionPromotedIsLast(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            String strChangeOrderID = "";
            Map mCAObj;
            String sCAID;
            String sCACurrent;
            HashSet<String> stateSet = new HashSet<>();

            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");
            String STATE_INReview_InApproval = PropertyUtil.getSchemaProperty("policy", TigerConstants.POLICY_PSS_CHANGEORDER, "state_InApproval");
            // PCM TIGTK-3694 : 1/12/2016 : KWagh : Start
            String STATE_Complete = PropertyUtil.getSchemaProperty("policy", TigerConstants.POLICY_PSS_CHANGEORDER, "state_Complete");
            StringList slAllowedStates = new StringList();
            slAllowedStates.add(STATE_INReview_InApproval);
            slAllowedStates.add(STATE_Complete);
            // PCM TIGTK-3694 : 1/12/2016 : KWagh : End
            String strCAId = args[0];

            String typePattern = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
            slObjectSle.addElement(DomainConstants.SELECT_POLICY);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            DomainObject domCAObj = new DomainObject(strCAId);

            MapList mlConnectedCOs = domCAObj.getRelatedObjects(context, relpattern, TigerConstants.TYPE_PSS_CHANGEORDER, slObjectSle, slRelSle, true, false, (short) 1, null, null);
            if (!mlConnectedCOs.isEmpty()) {

                for (int index = 0; index < mlConnectedCOs.size(); index++) {
                    Map mCOObj = (Map) mlConnectedCOs.get(index);
                    strChangeOrderID = (String) mCOObj.get(DomainConstants.SELECT_ID);

                }

                DomainObject domChangeOrder = new DomainObject(strChangeOrderID);
                String strCOType = domChangeOrder.getType(context);

                if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(strCOType)) {
                    MapList mlConnectedCAs = domChangeOrder.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null);

                    if (!mlConnectedCAs.isEmpty()) {
                        if (mlConnectedCAs.size() == 1) {

                            for (int j = 0; j < mlConnectedCAs.size(); j++) {
                                mCAObj = (Map) mlConnectedCAs.get(j);
                                sCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                                if (sCAID.equalsIgnoreCase(strCAId)) {

                                    // Promote Change Order to In Review state
                                    domChangeOrder.setState(context, STATE_INReview_InApproval);

                                }
                            }
                        } else {
                            for (int k = 0; k < mlConnectedCAs.size(); k++) {
                                mCAObj = (Map) mlConnectedCAs.get(k);
                                sCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                                sCACurrent = (String) mCAObj.get(DomainConstants.SELECT_CURRENT);

                                if (!(sCAID.equalsIgnoreCase(strCAId))) {

                                    stateSet.add(sCACurrent);

                                }
                            }
                            // PCM TIGTK-3694 : 1/12/2016 : KWagh : Start
                            if (stateSet.iterator().hasNext()) {
                                String sState = stateSet.iterator().next();

                                if (slAllowedStates.contains(sState)) {
                                    // Promote Change Order to In Review state
                                    domChangeOrder.setState(context, STATE_INReview_InApproval);
                                }
                                // PCM TIGTK-3694 : 1/12/2016 : KWagh : End

                            }

                        }
                    }
                } else {
                }
            } else {
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteChangeOrderIfChangeActionPromotedIsLast: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    /**
     * @author SteepGraph - Ketaki W., Ajay V., Dhiren P.
     * @param context
     * @param args
     *            - ObjectId of ChangeAction
     * @return
     * @throws Exception
     */
    public void promoteLinkedAffectedImplementedItemsToApproved(Context context, String[] args) throws Exception {
        boolean bIsContextPushed = false;
        try {
            // TIGTK-8543 - PTE - 2017-7-21 - START
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_PROMOTE_FROM_PCM", "True");
            // TIGTK-8543 - PTE - 2017-7-21 - END

            String strChangeActionObjId = args[0]; // Change Object

            StringList objectSelects = new StringList(1);
            objectSelects.addElement(DomainConstants.SELECT_ID);

            DomainObject domCAObject = DomainObject.newInstance(context, strChangeActionObjId);

            Pattern relPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
            relPattern.addPattern(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);

            String strWhereExp = " " + DomainConstants.SELECT_CURRENT + " == '" + TigerConstants.STATE_PART_REVIEW + "' ";

            MapList mpListItems = domCAObject.getRelatedObjects(context, relPattern.getPattern(), DomainConstants.QUERY_WILDCARD, objectSelects, DomainConstants.EMPTY_STRINGLIST, false, true,
                    (short) 1, strWhereExp, DomainConstants.EMPTY_STRING, (short) 0);

            StringList slItemIds = getStringListFromMaplist(mpListItems, DomainConstants.SELECT_ID);
            pss.ecm.ui.MfgChangeOrder_mxJPO MfgChangeOrderBase = new pss.ecm.ui.MfgChangeOrder_mxJPO();
            mpListItems = MfgChangeOrderBase.getOrderedParentChild(context, slItemIds);

            if (!mpListItems.isEmpty()) {
                Iterator itrItems = mpListItems.iterator();
                while (itrItems.hasNext()) {
                    Map mpItem = (Map) itrItems.next();
                    String strItemObjId = (String) mpItem.get(DomainConstants.SELECT_ID);
                    DomainObject domItemObj = DomainObject.newInstance(context, strItemObjId);

                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    bIsContextPushed = true;
                    domItemObj.setState(context, TigerConstants.STATE_PART_APPROVED);
                }
            }
        } catch (Exception ex) {
            logger.error("Error in promoteLinkedAffectedImplementedItemsToApproved: ", ex);
            throw ex;
        } finally {
            // TIGTK-8543 - PTE - 2017-7-21 - START
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_PROMOTE_FROM_PCM", "");
            // TIGTK-8543 - PTE - 2017-7-21 - END
            if (bIsContextPushed) {
                ContextUtil.popContext(context);
            }
        }
    }

    /**
     * Method to get the Related CAD of Part and Add into CA on Create Action Trigger for Relationship "Change Affected Item".
     * @param context
     * @param args
     *            - FromObjectId,ToObjectId & RelationShip Id
     * @return
     * @throws Exception
     */
    public void addRelatedCADOfPartToCA(Context context, String[] args) throws Exception {
        try {
            String strFromObjectId = args[0];
            String strToObjectId = args[1];
            // TIGTK-3961:Performance issue for string concat:Rutuja Ekatpure:23/1/2017:Start
            String strCOId = EMPTY_STRING;
            String strCRId = EMPTY_STRING;
            // TIGTK-3961:Performance issue for string concat:Rutuja Ekatpure:23/1/2017:End
            String strCAOwner = EMPTY_STRING;
            MapList changeActionList = new MapList();

            StringList strCADItems = new StringList();
            boolean flagCAD = false;
            boolean bolPartAddFromCR = false;

            DomainObject domPartObject = DomainObject.newInstance(context, strToObjectId);
            DomainObject domCAObject = DomainObject.newInstance(context, strFromObjectId);
            String strObjectType = (String) domPartObject.getInfo(context, DomainConstants.SELECT_TYPE);
            StringList selectStmts = new StringList(1);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            selectStmts.addElement(DomainConstants.SELECT_NAME);
            selectStmts.addElement(DomainConstants.SELECT_POLICY);
            StringList relStmts = new StringList(0);

            // Check if Object Type is Part
            if (strObjectType.equals(DomainConstants.TYPE_PART)) {
                // get the CAD Objects connected with Part
                // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                MapList mlCADObject = domPartObject.getRelatedObjects(context, RELATIONSHIP_PART_SPECIFICATION, STR_TYPE_ALL_CAD, selectStmts, relStmts, false, true, (short) 1, null, null);
                if (mlCADObject.size() != 0) {
                    for (int j = 0; j < mlCADObject.size(); j++) {
                        Map<String, String> map = (Map<String, String>) mlCADObject.get(j);
                        String strCADObjectId = map.get("id");
                        strCADItems.add(strCADObjectId);
                    }
                }

                MapList changeOrderList = domCAObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, TigerConstants.TYPE_PSS_CHANGEORDER, selectStmts, relStmts, true, false,
                        (short) 1, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0);

                MapList channgeRequestList = domCAObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, TigerConstants.TYPE_PSS_CHANGEREQUEST, selectStmts, relStmts, true,
                        false, (short) 1, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0);
                for (int m = 0; m < channgeRequestList.size(); m++) {
                    Map mCRObj = (Map) channgeRequestList.get(m);
                    strCRId = (String) mCRObj.get(DomainConstants.SELECT_ID);
                    DomainObject domCRObject = new DomainObject(strCRId);
                    String strCRCurrentState = domCRObject.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if (strCRCurrentState.equalsIgnoreCase("create") || strCRCurrentState.equalsIgnoreCase("submit")) {
                        bolPartAddFromCR = true;
                    }
                }

                // On based of Affected Item added From Change Request OR Change Order
                if (bolPartAddFromCR == true) {
                    DomainObject domCRObject = new DomainObject(strCRId);
                    // Get connected Change Action Objects with CR
                    changeActionList = domCRObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, selectStmts, relStmts, false, true,
                            (short) 1, "", "", (short) 0);
                } else {
                    for (int i = 0; i < changeOrderList.size(); i++) {
                        Map mCOObj = (Map) changeOrderList.get(i);
                        strCOId = (String) mCOObj.get(DomainConstants.SELECT_ID);
                        DomainObject domCOObj = new DomainObject(strCOId);
                        // Get connected Change Action Objects with CO
                        changeActionList = domCOObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, selectStmts, relStmts, false, true,
                                (short) 1, "", "", (short) 0);
                    }
                }
                // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                // Find Bug modifications: 23/03/2017 : KWagh : START
                // Find Bug modifications: 23/03/2017 : KWagh : End
                for (int k = 0; k < changeActionList.size(); k++) {
                    Map mCAObj = (Map) changeActionList.get(k);
                    String strCAId = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    DomainObject domCAObj = new DomainObject(strCAId);
                    strCAOwner = domCAObj.getInfo(context, DomainConstants.SELECT_OWNER);
                    // get Change Affected item for check Group
                    MapList changeAffectedItemList = domCAObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, DomainConstants.QUERY_WILDCARD, selectStmts, relStmts,
                            false, true, (short) 1, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0);

                    for (int l = 0; l < changeAffectedItemList.size(); l++) {
                        Map mChanegAffectedObj = (Map) changeAffectedItemList.get(l);
                        String strChangeAffectedPolicy = (String) mChanegAffectedObj.get(DomainConstants.SELECT_POLICY);
                        String strChangeAffectedName = (String) mChanegAffectedObj.get(DomainConstants.SELECT_NAME);
                        // PCM RFC-074 : 29/09/2016 : KWagh : Start
                        // Check for Type of Change Action is for CAD or Other type
                        if ((strChangeAffectedPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) || (strChangeAffectedPolicy.equals(TigerConstants.POLICY_PSS_Legacy_CAD))) {
                            flagCAD = true;
                            for (int i = 0; i < mlCADObject.size(); i++) {
                                Map mCADObj = (Map) mlCADObject.get(i);
                                String strCADItemId = (String) mCADObj.get(DomainConstants.SELECT_ID);
                                String strCADItemName = (String) mCADObj.get(DomainConstants.SELECT_NAME);
                                if (!strCADItemName.equals(strChangeAffectedName)) {
                                    DomainObject domCADObj = new DomainObject(strCADItemId);
                                    // Connect CAD objects with Change Action of CAD types
                                    DomainRelationship.connect(context, domCAObj, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, domCADObj);
                                }
                            }
                        }
                        // PCM RFC-074 : 29/09/2016 : KWagh : End
                    }
                }

                if (flagCAD == false && strCADItems.size() != 0 && bolPartAddFromCR == false) {
                    // If Affected Item is add from Change Order & CA of CAD not available then create new CA for CAD and Connect with CO
                    String strNewCAForCAD = FrameworkUtil.autoName(context, "type_ChangeAction", DomainObject.EMPTY_STRING, "policy_ChangeAction", "eService Production", null, false, false);
                    DomainObject domNewCAForCAD = new DomainObject(strNewCAForCAD);
                    domNewCAForCAD.setOwner(context, strCAOwner);

                    if (strCADItems.size() != 0) {
                        for (int l = 0; l < strCADItems.size(); l++) {
                            String strCADObjectId = (String) strCADItems.get(l);
                            DomainObject domCAD = new DomainObject(strCADObjectId);
                            String strCADCurrent = domCAD.getInfo(context, DomainConstants.SELECT_CURRENT);
                            DomainRelationship domRel = DomainRelationship.connect(context, domNewCAForCAD, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, domCAD);
                            if (strCADCurrent.equalsIgnoreCase("Released")) {
                                DomainRelationship.setAttributeValue(context, domRel.toString(), "Requested Change", "For Revise");
                            } else {
                                DomainRelationship.setAttributeValue(context, domRel.toString(), "Requested Change", "For Release");
                            }
                        }
                    }
                    DomainObject domCO = new DomainObject(strCOId);
                    DomainRelationship.connect(context, domCO, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, domNewCAForCAD);
                } else if (flagCAD == false && strCADItems.size() != 0 && bolPartAddFromCR == true) {

                    // If Affected Item is add from Change Request & CA of CAD not available then create new CA for CAD and Connect with CR
                    String strNewCAForCAD = FrameworkUtil.autoName(context, "type_ChangeAction", DomainObject.EMPTY_STRING, "policy_ChangeAction", "eService Production", null, false, false);
                    DomainObject domNewCAForCAD = new DomainObject(strNewCAForCAD);
                    domNewCAForCAD.setOwner(context, strCAOwner);

                    if (strCADItems.size() != 0) {
                        for (int l = 0; l < strCADItems.size(); l++) {
                            String strCADObjectId = (String) strCADItems.get(l);
                            DomainObject domCAD = new DomainObject(strCADObjectId);
                            DomainRelationship.connect(context, domNewCAForCAD, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, domCAD);
                        }
                    }

                    DomainObject domCRObject = new DomainObject(strCRId);
                    DomainRelationship.connect(context, domCRObject, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, domNewCAForCAD);
                }

            }

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in addRelatedCADOfPartToCA: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    /**
     * Method to get promote ChangeImplementedItem in released state for Complete CO.
     * @param context
     * @param args
     *            - FromObjectId,ToObjectId & RelationShip Id
     * @return
     * @throws Exception
     */
    public int completeChangeOrderIfLastChangeActionIsComplete(Context context, String[] args) throws Exception {
        try {
            // TIGTK-6185 | 11/04/2017 | Harika Varanasi : Starts
            String strLoginedUser = PropertyUtil.getGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME);
            // TIGTK-6185 | 11/04/2017 | Harika Varanasi : Ends
            String strChangeActionObjId = args[0]; // Change Object
            String strCRId = "";
            String strCOId = "";
            String strCAID = "";
            String strCACurrent = "";
            String strChangeManager = "";
            String strCOName = "";
            HashSet<String> stateSet = new HashSet<>();
            StringList objectSelects = new StringList(0);
            objectSelects.addElement(SELECT_ID);
            objectSelects.addElement(SELECT_POLICY);
            objectSelects.addElement(SELECT_NAME);
            objectSelects.addElement(SELECT_CURRENT);
            StringList relSelects = new StringList(1);
            relSelects.addElement(SELECT_RELATIONSHIP_ID);
            // PCM RFC-074 : 27/09/2016 : KWagh : Start

            String STATE_COMPLETE = PropertyUtil.getSchemaProperty("policy", ChangeConstants.POLICY_CHANGE_ACTION, "state_Complete");
            DomainObject domCAObject = new DomainObject(strChangeActionObjId);

            // Get Connected Chane Order with Change Action
            MapList listChangeOrderId = domCAObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, TigerConstants.TYPE_PSS_CHANGEORDER, objectSelects, relSelects, true, false,
                    (short) 1, "", "", (short) 0);

            for (int i = 0; i < listChangeOrderId.size(); i++) {
                Map mapCOId = (Map) listChangeOrderId.get(i);
                strCOId = (String) mapCOId.get("id");
                DomainObject domCOId = new DomainObject(strCOId);

                // Get Connected All Change Action with Chane Order
                // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                MapList mlistChangeActions = domCOId.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, objectSelects, relSelects, false, true,
                        (short) 1, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0);

                // Check All Change Action objects are in complete state Or not

                // PCM RFC-074 : 27/09/2016 : KWagh : End
                if (!mlistChangeActions.isEmpty()) {
                    if (mlistChangeActions.size() == 1) {

                        for (int j = 0; j < mlistChangeActions.size(); j++) {
                            Map mCA = (Map) mlistChangeActions.get(j);
                            strCAID = (String) mCA.get(DomainConstants.SELECT_ID);
                            if (strCAID.equalsIgnoreCase(strChangeActionObjId)) {

                                // Promote Change Order to Complete state
                                // TIGTK-6185 | 11/04/2017 | Harika Varanasi : Starts
                                if (UIUtil.isNotNullAndNotEmpty(strLoginedUser)) {
                                    PropertyUtil.setGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME, strLoginedUser);
                                }
                                // TIGTK-6185 | 11/04/2017 | Harika Varanasi : Ends
                                domCOId.setState(context, "Complete");
                                strCOName = (String) domCOId.getInfo(context, DomainConstants.SELECT_NAME);

                                MapList changeRequestList = domCOId.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEREQUEST, objectSelects,
                                        new StringList(0), true, false, (short) 1, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0);

                                for (int k = 0; k < changeRequestList.size(); k++) {
                                    Map mObj = (Map) changeRequestList.get(k);

                                    strCRId = (String) mObj.get(DomainConstants.SELECT_ID);
                                    DomainObject domCRObj = new DomainObject(strCRId);
                                    strChangeManager = domCRObj.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.name");
                                }
                                // Find Bug modifications: 23/03/2017 : KWagh : End
                            }
                        }
                    } else {
                        for (int k = 0; k < mlistChangeActions.size(); k++) {
                            Map mpCA = (Map) mlistChangeActions.get(k);
                            strCAID = (String) mpCA.get(DomainConstants.SELECT_ID);
                            strCACurrent = (String) mpCA.get(DomainConstants.SELECT_CURRENT);

                            if (!(strCAID.equalsIgnoreCase(strChangeActionObjId))) {

                                stateSet.add(strCACurrent);

                            }
                        }

                        if (stateSet.size() > 1) {

                            // do nothing

                        } else {
                            if (stateSet.iterator().hasNext()) {
                                String sState = stateSet.iterator().next();

                                if (STATE_COMPLETE.equalsIgnoreCase(sState)) {
                                    // Promote Change Order to Complete state
                                    // TIGTK-6185 | 11/04/2017 | Harika Varanasi : Starts
                                    if (UIUtil.isNotNullAndNotEmpty(strLoginedUser)) {
                                        PropertyUtil.setGlobalRPEValue(context, ContextUtil.MX_LOGGED_IN_USER_NAME, strLoginedUser);
                                    }
                                    // TIGTK-6185 | 11/04/2017 | Harika Varanasi : Ends
                                    domCOId.setState(context, "Complete");
                                    strCOName = (String) domCOId.getInfo(context, DomainConstants.SELECT_NAME);

                                    MapList changeRequestList = domCOId.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEREQUEST, objectSelects,
                                            new StringList(0), true, false, (short) 1, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0);

                                    for (int k = 0; k < changeRequestList.size(); k++) {
                                        Map mObj = (Map) changeRequestList.get(k);

                                        strCRId = (String) mObj.get(DomainConstants.SELECT_ID);

                                        DomainObject domCRObj = new DomainObject(strCRId);
                                        strChangeManager = domCRObj.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR + "].to.name");
                                    }
                                    // Find Bug modifications: 23/03/2017 : KWagh : End
                                }
                            }

                        }

                    }
                }

            }

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in completeChangeOrderIfLastChangeActionIsComplete: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return 0;
    }

    /**
     * This Method is modified by PCM : TIGTK-9534 : 06/09/2017 : AB. Method to Update Assignee of CA's Affected Item Modified for TIGTK-4156 : PCM : 03/02/2017 : AB
     * @param context
     * @param args
     *            -
     * @return
     * @throws Exception
     */
    public void updateItemAssignee(Context context, String[] args) throws Exception {

        try {
            String strNewAssigneeObjId = DomainConstants.EMPTY_STRING;
            StringBuffer strBufferCAValue = new StringBuffer();

            Map programMap = (HashMap) JPO.unpackArgs(args);
            String strCAObjId = DomainConstants.EMPTY_STRING;
            HashMap paramMap = (HashMap) programMap.get("paramMap");

            String strChangeAffectedObjId = (String) paramMap.get("objectId");
            String strNewAssigneeObjName = (String) paramMap.get("New Value");

            // Get Existing RelationShip of Affected Item with Assignee
            StringList objectSelects = new StringList(DomainConstants.SELECT_ID);
            StringList relSelects = new StringList(SELECT_RELATIONSHIP_ID);

            // Get the connected Change Action of Affected Item and get the old relationship(Item Assignee) id
            Pattern relPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ITEMASSIGNEE);
            relPattern.addPattern(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);

            Pattern typePattern = new Pattern(ChangeConstants.TYPE_CHANGE_ACTION);
            typePattern.addPattern(DomainConstants.TYPE_PERSON);

            String strObjectWhere = "(current == '" + ChangeConstants.STATE_CHANGE_ACTION_PENDING + "' || current == '" + ChangeConstants.STATE_CHANGE_ACTION_INWORK + "' || current == '"
                    + DomainConstants.STATE_PERSON_ACTIVE + "')";

            DomainObject domChangeAffectedObjId = new DomainObject(strChangeAffectedObjId);
            MapList listItemInfo = domChangeAffectedObjId.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), objectSelects, relSelects, true, true, (short) 1,
                    strObjectWhere, DomainObject.EMPTY_STRING, 0);

            if (!listItemInfo.isEmpty()) {
                for (int i = 0; i < listItemInfo.size(); i++) {
                    Map mapItemInfo = (Map) listItemInfo.get(i);
                    String strType = (String) mapItemInfo.get(DomainConstants.SELECT_TYPE);

                    if (strType.equalsIgnoreCase(DomainConstants.TYPE_PERSON)) {
                        String strOldRelIdWithAssignee = (String) mapItemInfo.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        DomainRelationship.disconnect(context, strOldRelIdWithAssignee); // Disconnecting the existing relationship
                        ContextUtil.popContext(context);
                    } else if (strType.equalsIgnoreCase(ChangeConstants.TYPE_CHANGE_ACTION)) {
                        strCAObjId = (String) mapItemInfo.get(DomainConstants.SELECT_ID);
                    }
                }
            }

            // Get Person Id Using name.
            StringList slObjectSelects = new StringList(1);
            slObjectSelects.add(SELECT_ID);
            MapList mlPersonInfo = DomainObject.findObjects(context, TYPE_PERSON, strNewAssigneeObjName, "*", null, TigerConstants.VAULT_ESERVICEPRODUCTION, "revision == last", false,
                    slObjectSelects);
            for (int i = 0; i < mlPersonInfo.size(); i++) {
                Map map = (Map) mlPersonInfo.get(i);
                strNewAssigneeObjId = (String) map.get(DomainConstants.SELECT_ID);
            }

            // Connect New Assignee with Change Affected Item using "PSS_ItemAssignee" Relationship
            DomainObject domNewAssigneeObjId = new DomainObject(strNewAssigneeObjId);
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            DomainRelationship.connect(context, domChangeAffectedObjId, TigerConstants.RELATIONSHIP_PSS_ITEMASSIGNEE, domNewAssigneeObjId);
            ContextUtil.popContext(context);

            // Get the Data of Change Action Id and AffectedItem Id with Assignee of Affected Item
            String strRPEValue = PropertyUtil.getGlobalRPEValue(context, "PSS_NotifyCAAssignees");
            String strRPECAValue = PropertyUtil.getGlobalRPEValue(context, "PSS_CAIds");

            StringList slRPEValue = new StringList();
            if (UIUtil.isNotNullAndNotEmpty(strRPECAValue)) {
                slRPEValue = FrameworkUtil.split(strRPECAValue, ",");
            }

            // Create the eqation of all Id.
            // For Example : If 1 CA and multiple items are changed then eqation be like CA,ItemID|NewAssigneeID@ItemID2|NewAssigneeID2
            // And If Assignee of items which is belong from different CA then equation will create like
            // CA,ItemID|NewAssigneeID@ItemID2|NewAssigneeID2#CA2,ItemID3|NewAssigneeID3@ItemID4|NewAssigneeID4

            StringBuffer strBufferValue = new StringBuffer();
            if (!slRPEValue.isEmpty() && slRPEValue.contains(strCAObjId)) {
                strBufferValue.append(strRPEValue);
                strBufferValue.append("@");
            } else {
                if (UIUtil.isNotNullAndNotEmpty(strRPEValue)) {
                    strBufferValue.append(strRPEValue);
                    strBufferValue.append("~");
                }

                strBufferValue.append(strCAObjId);
                strBufferValue.append(",");
            }

            strBufferValue.append(strChangeAffectedObjId);
            strBufferValue.append("|");
            strBufferValue.append(strNewAssigneeObjId);

            PropertyUtil.setGlobalRPEValue(context, "PSS_NotifyCAAssignees", strBufferValue.toString());
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAIds", strBufferCAValue.toString());

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in updateItemAssignee: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    /**
     * This Method is modified by PCM : TIGTK-9534 : 06/09/2017 : AB This Method is used to split CA Id , Affecetd item id and New assignee of affected item id
     * @param context
     * @param args
     * @throws Exception
     */
    public void notifyAssignees(Context context, String[] args) throws Exception {
        try {
            StringList slAssignneeIds = new StringList();
            Map mapAssignneAndItemId = new HashMap();
            Map mapItemAndCAId = new HashMap();

            // Get the equation of CAId, AffectedItemId and NewAssigneeId of Affected Item which is set as RPE value
            String strRPEValue = PropertyUtil.getGlobalRPEValue(context, "PSS_NotifyCAAssignees");
            PropertyUtil.setGlobalRPEValue(context, "PSS_NotifyCAAssignees", DomainConstants.EMPTY_STRING);
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAIds", DomainConstants.EMPTY_STRING);
            StringList slMultipleCAsRPEValue = new StringList();

            // Split eqation for one CA and Multiple CA, then create group of RPE value
            if (UIUtil.isNotNullAndNotEmpty(strRPEValue)) {
                if (strRPEValue.contains("~")) {
                    slMultipleCAsRPEValue = FrameworkUtil.split(strRPEValue, "~");
                } else {
                    slMultipleCAsRPEValue.add(strRPEValue);
                }
            }

            // Create Map for AffectedItem and Assignee
            if (!slMultipleCAsRPEValue.isEmpty()) {
                for (int i = 0; i < slMultipleCAsRPEValue.size(); i++) {
                    // Split by comma for separate all Change Actions Id
                    strRPEValue = (String) slMultipleCAsRPEValue.get(i);
                    StringList strListRPEValue = FrameworkUtil.split(strRPEValue, ",");
                    if (strListRPEValue.size() > 0) {
                        String strCAId = (String) strListRPEValue.get(0);
                        String strCAAIAssignee = (String) strListRPEValue.get(1);

                        if (UIUtil.isNotNullAndNotEmpty(strCAAIAssignee)) {
                            // Split by @ if there was multiple assignee changed of affetced item which is belonged to same Change Action
                            StringList slListRPECAAIAssignee = FrameworkUtil.split(strCAAIAssignee, "@");

                            if (slListRPECAAIAssignee.size() > 0) {
                                for (int j = 0; j < slListRPECAAIAssignee.size(); j++) {
                                    String strCAAIAssigneeId = (String) slListRPECAAIAssignee.get(j);

                                    if (UIUtil.isNotNullAndNotEmpty(strCAAIAssigneeId)) {
                                        // Split Affected item id and new Assignee of that item
                                        StringList strListCAValue = FrameworkUtil.split(strCAAIAssigneeId, "|");

                                        if (strListCAValue.size() > 0) {
                                            String strCAAffectedItemId = (String) strListCAValue.get(0);
                                            String strCAAssigneeId = (String) strListCAValue.get(1);

                                            // create Map of Affected item and Assignee of Affected item. Also create one map for Affected item and Related CA
                                            if (mapAssignneAndItemId.containsKey(strCAAssigneeId)) {
                                                String strValueOfAffectedItem = (String) mapAssignneAndItemId.get(strCAAssigneeId);
                                                StringBuffer sbItemList = new StringBuffer();
                                                sbItemList.append(strValueOfAffectedItem).append("|").append(strCAAffectedItemId);
                                                mapAssignneAndItemId.put(strCAAssigneeId, sbItemList.toString());
                                            } else {
                                                mapAssignneAndItemId.put(strCAAssigneeId, strCAAffectedItemId);
                                            }

                                            mapItemAndCAId.put(strCAAffectedItemId, strCAId);

                                            if (!slAssignneeIds.contains(strCAAssigneeId)) {
                                                slAssignneeIds.add(strCAAssigneeId);
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }

            // Call this Method to send Mail Notification to All selected Assignee
            postAssigneeUpdate(context, slAssignneeIds, mapItemAndCAId, mapAssignneAndItemId);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * This Method is modified by PCM : TIGTK-9534 : 06/09/2017 : AB Method used to send notifications based on RPE value set from Update Assignee on CA : TIGTK-4591
     * @param context
     * @param slAssignneeIds
     * @param mapItemAndCAId
     * @param mapAssignneAndItemId
     * @throws Exception
     */
    public void postAssigneeUpdate(Context context, StringList slAssignneeIds, Map mapItemAndCAId, Map mapAssignneAndItemId) throws Exception {
        try {
            StringList slItemsCAID = new StringList();
            String strCONumber = DomainConstants.EMPTY_STRING;
            String strProgramProjectId = DomainConstants.EMPTY_STRING;
            String strProgramProjectName = DomainConstants.EMPTY_STRING;
            String strProgramProjectDescription = DomainConstants.EMPTY_STRING;
            String strConnectedCOId = DomainConstants.EMPTY_STRING;
            String strCODescription = DomainConstants.EMPTY_STRING;
            String strCOCreator = DomainConstants.EMPTY_STRING;
            String strCOCurrent = DomainConstants.EMPTY_STRING;
            String strCOVirtualImplDate = DomainConstants.EMPTY_STRING;
            String strReasonForChangeValue = DomainConstants.EMPTY_STRING;

            // Get the Base URL to create link for All IDs
            String strBaseURL = MailUtil.getBaseURL(context);
            if (UIUtil.isNotNullAndNotEmpty(strBaseURL)) {
                int position = strBaseURL.lastIndexOf("/");
                strBaseURL = strBaseURL.substring(0, position);
            }

            // Mail Notification send To different Assignee which is updated (Per one Assignee only one notification will be send)
            if (!slAssignneeIds.isEmpty()) {
                int intSizeAssigneeList = slAssignneeIds.size();
                for (int i = 0; i < intSizeAssigneeList; i++) {
                    StringList toList = new StringList();
                    Map mapAssigneesCAAndItem = new HashMap();
                    // Get the AssigneeID
                    String strAssigneeID = (String) slAssignneeIds.get(i);

                    // get the All affected Item which have assigneed this new Assignee
                    String strAffectedItem = (String) mapAssignneAndItemId.get(strAssigneeID);
                    StringList slAffectedItem = FrameworkUtil.split(strAffectedItem, "|");

                    if (!slAffectedItem.isEmpty()) {
                        int intSizeAffectedItemList = slAffectedItem.size();
                        for (int j = 0; j < intSizeAffectedItemList; j++) {
                            String strAffectedItemID = (String) slAffectedItem.get(j);
                            String strItemsCAId = (String) mapItemAndCAId.get(strAffectedItemID);

                            // Collect the All CA which is belonged to specific Assignee
                            if (!slItemsCAID.contains(strItemsCAId)) {
                                slItemsCAID.add(strItemsCAId);
                            }

                            // Create Map for specific AffectedItem and CA of that Items (Which is belonged to one specific Assignee)
                            if (mapAssigneesCAAndItem.containsKey(strItemsCAId)) {
                                String strValueOfAffectedItem = (String) mapAssigneesCAAndItem.get(strItemsCAId);
                                StringBuffer sbItemList = new StringBuffer();
                                sbItemList.append(strValueOfAffectedItem).append("|").append(strAffectedItemID);
                                mapAssigneesCAAndItem.put(strItemsCAId, sbItemList.toString());
                            } else {
                                mapAssigneesCAAndItem.put(strItemsCAId, strAffectedItemID);
                            }
                        }

                        // Create Notification for Assignee
                        StringBuffer strCANames = new StringBuffer();

                        // To List for the Notification, Add Assignee name in the To List
                        DomainObject domAssigneePersonObject = DomainObject.newInstance(context, strAssigneeID);
                        String strAssigneeName = (String) domAssigneePersonObject.getInfo(context, DomainConstants.SELECT_NAME);
                        toList.add(strAssigneeName);

                        // Get the Information related to Change Action
                        StringList objSelects = new StringList();
                        objSelects.add(DomainConstants.SELECT_NAME);
                        objSelects.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");

                        // Create Subject for Mail Notification
                        String strLanguage = context.getSession().getLanguage();
                        String strMsgTigerKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.Tiger");
                        String strCAAffectedItemReassignKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage),
                                "emxFramework.Message.CAAffectedItemReassignKey");
                        String strMsgAgainstStringKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.AgainstString");
                        String strMsghasBeenAssignedForKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage),
                                "emxFramework.Message.hasBeenAssignedFor1");

                        StringBuffer subjectKey = new StringBuffer();
                        subjectKey.append(strMsgTigerKey + " ");
                        subjectKey.append(strCAAffectedItemReassignKey + " ");

                        // Create Message body
                        StringBuffer msg = new StringBuffer();
                        String strMsgCOAffectedItemKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.AffectedItem");
                        String strMsgCODescriptionKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.CODescription");
                        String strMsgReasonForChangeKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage), "emxFramework.Message.ReasonForChange");
                        String strMsgVirtualImplPlanDateKey = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", new Locale(strLanguage),
                                "emxFramework.Message.VirtualImplementationPlanDate");
                        String strProgramProjectKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                "EnterpriseChangeMgt.Label." + "Project_Code");
                        String strProgramProjectDescriptionKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                "EnterpriseChangeMgt.Label." + "Project_Description");
                        String strCOCreatorKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Label." + "CO_Creator");
                        String strCONameKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Label." + "CO");
                        String strCODescriptionKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                "EnterpriseChangeMgt.Label." + "CO_Description");
                        String strCOStateKey = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Label." + "State");

                        // Now create Link for the Affected Item and related Change Action
                        if (!slItemsCAID.isEmpty()) {
                            int intSizeOfCA = slItemsCAID.size();
                            for (int k = 0; k < intSizeOfCA; k++) {
                                StringBuffer strAffectedItemsURL = new StringBuffer();
                                String strCAID = (String) slItemsCAID.get(k);
                                DomainObject domCAObject = DomainObject.newInstance(context, strCAID);
                                Map mapChangeActionInfo = domCAObject.getInfo(context, objSelects);
                                String strCAName = (String) mapChangeActionInfo.get(DomainConstants.SELECT_NAME);
                                strConnectedCOId = (String) mapChangeActionInfo.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");

                                // create CA Names group for Subject line
                                if (!strCANames.toString().isEmpty()) {
                                    strCANames.append(" , ").append(strCAName);
                                } else {
                                    strCANames.append(strCAName);
                                }

                                // Get the Information related to Change Order
                                objSelects.add(DomainConstants.SELECT_DESCRIPTION);
                                objSelects.add(DomainConstants.SELECT_NAME);
                                objSelects.add(DomainConstants.SELECT_CURRENT);
                                objSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                                objSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE + "]");
                                objSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                                objSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                                objSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");

                                DomainObject domCOObject = DomainObject.newInstance(context, strConnectedCOId);
                                Map mapChangeOrderInfo = domCOObject.getInfo(context, objSelects);
                                strCONumber = (String) mapChangeOrderInfo.get(DomainConstants.SELECT_NAME);
                                strCOCurrent = (String) mapChangeOrderInfo.get(DomainConstants.SELECT_CURRENT);
                                strCOCreator = (String) mapChangeOrderInfo.get("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
                                strCODescription = (String) mapChangeOrderInfo.get(DomainConstants.SELECT_DESCRIPTION);
                                strCOVirtualImplDate = (String) mapChangeOrderInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_COVIRTUALIMPLEMENTATIONDATE + "]");
                                strProgramProjectName = (String) mapChangeOrderInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
                                strProgramProjectId = (String) mapChangeOrderInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                                strProgramProjectDescription = (String) mapChangeOrderInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");

                                subjectKey.append(strCAName + " ");

                                // Create Link for Affected Item and add that into message body
                                String strAllAffectedItemID = (String) mapAssigneesCAAndItem.get(strCAID);
                                StringList slAffectedItems = FrameworkUtil.split(strAllAffectedItemID, "|");

                                // TIGTK-10802 : Sayali D : 9 -Nov -2017 START
                                StringBuffer strBufferAffectedItemIdRPE = new StringBuffer();
                                StringBuffer streBufferAffectedItemNamesRPE = new StringBuffer();

                                for (int l = 0; l < slAffectedItems.size(); l++) {
                                    if (l != 0) {
                                        strAffectedItemsURL.append(" , ");
                                        strBufferAffectedItemIdRPE.append(",");
                                        streBufferAffectedItemNamesRPE.append(",");
                                    }

                                    String strAffectedItemId = (String) slAffectedItems.get(l);
                                    strBufferAffectedItemIdRPE.append((String) slAffectedItems.get(l));
                                    streBufferAffectedItemNamesRPE.append(MqlUtil.mqlCommand(context, "print bus " + slAffectedItems.get(l) + " select name dump |", false, false));
                                    strAffectedItemsURL.append(strBaseURL + "/emxTree.jsp?objectId=" + strAffectedItemId);
                                }

                                String strCAsURL = strBaseURL + "/emxTree.jsp?objectId=" + strCAID;
                                msg.append(strCAsURL);
                                msg.append("\n");
                                msg.append(strMsgCOAffectedItemKey + " ");
                                msg.append(strAffectedItemsURL.toString());
                                msg.append("\n");

                                PropertyUtil.setGlobalRPEValue(context, "PSS_AffectedItemIds", strBufferAffectedItemIdRPE.toString());
                                PropertyUtil.setGlobalRPEValue(context, "PSS_AffectedItemNames", streBufferAffectedItemNamesRPE.toString());
                                Map payLoad = new HashMap();
                                payLoad.put("toList", toList);
                                emxNotificationUtil_mxJPO.objectNotification(context, strCAID, "PSS_CAImplementItemReassignNotification", payLoad);

                                // TIGTK-10802 : Sayali D : 9 -Nov -2017 END
                            }

                            subjectKey.append(strMsgAgainstStringKey + " ");
                            subjectKey.append(strCONumber + " ");
                            subjectKey.append(strMsghasBeenAssignedForKey + " ");
                            subjectKey.append(strProgramProjectName);

                            msg.append("\n");
                            msg.append("\n");
                            msg.append(strProgramProjectKey + " ");
                            msg.append(" - ").append(strBaseURL + "/emxTree.jsp?objectId=" + strProgramProjectId);
                            msg.append("\n");
                            msg.append(strProgramProjectDescriptionKey + " ");
                            msg.append(" - ").append(strProgramProjectDescription);
                            msg.append("\n");
                            msg.append(strCONameKey + " ");
                            msg.append(" - ").append(strBaseURL + "/emxTree.jsp?objectId=" + strConnectedCOId);
                            msg.append("\n");
                            msg.append(strMsgCODescriptionKey);
                            msg.append(strCODescription);
                            msg.append("\n");
                            msg.append(strCOStateKey);
                            msg.append(" - ").append(strCOCurrent);
                            msg.append("\n");
                            msg.append(strMsgVirtualImplPlanDateKey + " ");
                            msg.append(strCOVirtualImplDate);
                            msg.append("\n");
                            msg.append(strCOCreatorKey + " - ");
                            msg.append(strCOCreator);
                            msg.append("\n");

                            // TIGTK-10802 : Sayali D : 9 -Nov -2017 START
                            // commented OLD format email
                            /*
                             * MailUtil.sendNotification(context, toList, // toList null, // ccList null, // bccList subjectKey.toString(), // subjectKey null, // subjectKeys null, // subjectValues
                             * msg.toString(), // messageKey null, // messageKeys null, // messageValues null, // objectIdList null); // companyName
                             */
                            // TIGTK-10802 : Sayali D : 9 -Nov -2017 END
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in postAssigneeUpdate: ", e);
            throw e;
        }
    }

    public void cancelChangeOrderInPrepareStat(Context context, String[] args) throws Exception {
        boolean bolPromoteCR = true;
        String strChangeRequestId = "";
        String strChangeRequestBillableValue = "";
        Map mapCOObj;
        String strCOCurrentState = "";
        StringList objSelect = new StringList();
        objSelect.add(DomainConstants.SELECT_ID);
        objSelect.add(DomainConstants.SELECT_CURRENT);
        objSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");
        String strCACurrent;
        String strCAID;
        String strMessage;
        String strMessageNew;
        HashMap param = (HashMap) JPO.unpackArgs(args);

        String objectId = (String) param.get("busObjId");

        DomainObject domObjCO = new DomainObject(objectId);
        String strCOName = domObjCO.getName(context);
        // Connected Change Action Objects

        String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");
        String typePattern = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");

        StringList slObjectSle = new StringList(1);
        slObjectSle.addElement(DomainConstants.SELECT_ID);
        slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

        StringList slRelSle = new StringList(1);
        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

        MapList mlConnectedCA = domObjCO.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, false, true, (short) 1, null, null);

        if (!mlConnectedCA.isEmpty()) {
            for (int i = 0; i < mlConnectedCA.size(); i++) {
                Map mCAObj = (Map) mlConnectedCA.get(i);
                strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                strCACurrent = (String) mCAObj.get(DomainConstants.SELECT_CURRENT);

                DomainObject domobjCA = new DomainObject(strCAID);
                // Check whether the Related Change Action is in Prepare(Pending) state
                if (strCACurrent.equalsIgnoreCase("Pending")) {

                    // Promote Change Action to Cancelled state
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    MqlUtil.mqlCommand(context, "trigger off");
                    domobjCA.setState(context, "Cancelled");
                    MqlUtil.mqlCommand(context, "trigger on");
                    ContextUtil.popContext(context);

                    // Promote Change Order to Cancelled state
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    MqlUtil.mqlCommand(context, "trigger off");
                    domObjCO.setState(context, "Cancelled");
                    MqlUtil.mqlCommand(context, "trigger on");
                    ContextUtil.popContext(context);

                } else {
                    strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                            "PSS_EnterpriseChangeMgt.Alert.ChangeActionNotInPrepareStateForCancel1");
                    strMessage = strMessage + " " + strCOName + "  ";

                    strMessageNew = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                            "PSS_EnterpriseChangeMgt.Alert.ChangeActionNotInPrepareStateForCancel2");
                    strMessage = strMessage + strMessageNew;
                    MqlUtil.mqlCommand(context, "notice $1", strMessage);
                }

            }
        }
        MapList listConnectedChangeRequest = domObjCO.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEREQUEST, objSelect, null, true, false,
                (short) 0, null, null, 0);
        if (listConnectedChangeRequest.size() != 0) {
            for (int i = 0; i < listConnectedChangeRequest.size(); i++) {
                Map mapConnectedChangeRequest = (Map) listConnectedChangeRequest.get(i);
                strChangeRequestId = (String) mapConnectedChangeRequest.get(DomainConstants.SELECT_ID);
                strChangeRequestBillableValue = (String) mapConnectedChangeRequest.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");
                DomainObject domChangeRequestObject = new DomainObject(strChangeRequestId);
                MapList listConnectedChangeOrder = domChangeRequestObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEORDER, objSelect, null,
                        false, true, (short) 1, null, null);
                if (listConnectedChangeOrder.size() != 0 && strChangeRequestBillableValue.equalsIgnoreCase("No")) {
                    for (int k = 0; k < listConnectedChangeOrder.size(); k++) {
                        mapCOObj = (Map) listConnectedChangeOrder.get(k);
                        strCOCurrentState = (String) mapCOObj.get(DomainConstants.SELECT_CURRENT);
                        if (strCOCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED) || strCOCurrentState.equalsIgnoreCase(TigerConstants.STATE_CHANGEORDER_IMPLEMENTED)) {
                        } else {
                            bolPromoteCR = false;
                        }
                    }
                }
            }
        }
        if (bolPromoteCR == true && strChangeRequestBillableValue.equalsIgnoreCase("No")) {
            DomainObject domChangeRequestObj = new DomainObject(strChangeRequestId);
            domChangeRequestObj.setState(context, TigerConstants.STATE_COMPLETE_CR);
        }
    }

    /**
     * @param context
     * @param args
     * @throws Exception
     *             This method is invoked via a Demote action trigger when the Part is demoted from Review to In Work
     */
    public void demotePartIfChangeActionInWork(Context context, String[] args) throws Exception {
        // TIGTK-6843:Phase-2.0:PKH:Start
        // Added for CO cancel and cancel implemented item functionality - for disconnect Affected item CA is Deleted as per trigger method.Prevent from Deletion CA- added below code.
        String strCancelStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_COCancelFromInReview");
        try {
            if (UIUtil.isNullOrEmpty(strCancelStatus) && !strCancelStatus.equals("True")) {
                String strCAID;
                String strCACurrent;
                String strCOID;
                String strCOCurrent;
                String strMessage;
                String strPartId = args[0];
                DomainObject domPartObj = new DomainObject(strPartId);

                // Connected Change Action Objects
                String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAffectedItem");
                String typePattern = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");
                // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                StringList slObjectSle = new StringList(2);
                slObjectSle.addElement(DomainConstants.SELECT_ID);
                slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
                // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                StringList slRelSle = new StringList(1);
                slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                MapList mlConnectedChangeAction = domPartObj.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

                if (!mlConnectedChangeAction.isEmpty()) {
                    for (int i = 0; i < mlConnectedChangeAction.size(); i++) {
                        Map mCAObj = (Map) mlConnectedChangeAction.get(i);
                        strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                        strCACurrent = (String) mCAObj.get(DomainConstants.SELECT_CURRENT);
                        DomainObject domobjCA = new DomainObject(strCAID);

                        // Check Change Action is in In Work State
                        if (!(strCACurrent.equalsIgnoreCase("In Work"))) {

                            // Check Change Action is in In Review (In Approval) State
                            if (strCACurrent.equalsIgnoreCase("In Approval")) {
                                // Demote Part To In work(Preliminary) and Demote Change Action To In work
                                domobjCA.setState(context, "In Work");

                                // Get Connected Change Order Objects

                                String relpatternCA = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");
                                String typePatternCO = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeOrder");

                                MapList mlConnectedChangeOrder = domobjCA.getRelatedObjects(context, relpatternCA, typePatternCO, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

                                if (!mlConnectedChangeOrder.isEmpty()) {
                                    for (int j = 0; j < mlConnectedChangeOrder.size(); j++) {
                                        Map mCOObj = (Map) mlConnectedChangeOrder.get(j);
                                        strCOID = (String) mCOObj.get(DomainConstants.SELECT_ID);
                                        strCOCurrent = (String) mCOObj.get(DomainConstants.SELECT_CURRENT);
                                        DomainObject domCOObj = new DomainObject(strCOID);

                                        if (!(strCOCurrent.equalsIgnoreCase("In Work"))) {
                                            domCOObj.setState(context, "In Work");
                                        } else {
                                            // Change Order is in In work
                                        }

                                    }
                                }

                            } else {
                                // Change Action is Not in In Review (In Approval) State
                                // Throw error
                                strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.CAInReview");
                                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                            }
                        } else {
                            // Change Action is in In Work
                            // Demote Part To In work(Preliminary)
                        }

                    }

                }
            }
            // TIGTK-6843:Phase-2.0:PKH:END

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in demotePartIfChangeActionInWork: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    /**
     * @param context
     * @param args
     * @throws Exception
     *             This method is invoked via a Demote check trigger when the CAD is demoted from Review to In Work
     */
    public int checkCADAssociatedPartInWorkState(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            String strCADId = args[0];
            // String strPartID;
            String strPartCurrent;
            String strMessage;
            int isError = 0;
            DomainObject domCADObj = new DomainObject(strCADId);

            // Get Connected Part Objects

            String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_PartSpecification");
            String typePattern = PropertyUtil.getSchemaProperty(context, "type_Part");

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
            slObjectSle.addElement(DomainConstants.SELECT_TYPE);
            slObjectSle.addElement(DomainConstants.SELECT_REVISION);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            MapList mConnectedParts = domCADObj.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

            if (!mConnectedParts.isEmpty()) {
                for (int j = 0; j < mConnectedParts.size(); j++) {
                    Map mPArtObj = (Map) mConnectedParts.get(j);
                    String strPartTYPE = (String) mPArtObj.get(DomainConstants.SELECT_TYPE);
                    String strPartName = (String) mPArtObj.get(DomainConstants.SELECT_NAME);
                    String strPartRevision = (String) mPArtObj.get(DomainConstants.SELECT_REVISION);

                    strPartCurrent = (String) mPArtObj.get(DomainConstants.SELECT_CURRENT);

                    if (!(strPartCurrent.equalsIgnoreCase("Preliminary"))) {

                        strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.RelatedPartOfCADNotInWork1");
                        strMessage = strMessage + "  " + strPartTYPE + " " + strPartName + " " + strPartRevision;
                        strMessage = strMessage + "  "
                                + EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.RelatedPartOfCADNotInWork2");
                        MqlUtil.mqlCommand(context, "notice $1", strMessage);
                        isError = 1;
                        return isError;
                    } else {
                        return isError;
                    }

                }
            }
            return isError;
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkCADAssociatedPartInWorkState: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    /**
     * @param context
     * @param args
     * @throws Exception
     *             This method is invoked via a Demote action trigger when the CAD is demoted from Review to In Work
     */
    public void demoteCADIfChangeActionInWork(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }

        try {
            // Find Bug modifications: 23/03/2017 : KWagh : START
            String strCAID;
            String strCACurrent;
            String strCOID;
            String strCOCurrent;
            String strPartCurrent;
            String relpattern;
            String typePattern;
            String strMessage;
            String strCADId = args[0];

            DomainObject domCADObj = new DomainObject(strCADId);

            // Get Connected Part Objects

            String relpatternPart = PropertyUtil.getSchemaProperty(context, "relationship_PartSpecification");
            String typePatternPart = PropertyUtil.getSchemaProperty(context, "type_Part");

            StringList slObjectSle = new StringList();
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            // Find Bug modifications: 23/03/2017 : KWagh : End
            MapList mConnectedParts = domCADObj.getRelatedObjects(context, relpatternPart, typePatternPart, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

            if (!mConnectedParts.isEmpty()) {
                for (int j = 0; j < mConnectedParts.size(); j++) {
                    Map mPArtObj = (Map) mConnectedParts.get(j);

                    strPartCurrent = (String) mPArtObj.get(DomainConstants.SELECT_CURRENT);

                    if (strPartCurrent.equalsIgnoreCase("Preliminary")) {

                        // Connected Change Action Objects
                        relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAffectedItem");
                        typePattern = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");

                        slObjectSle = new StringList(2);
                        slObjectSle.addElement(DomainConstants.SELECT_ID);
                        slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

                        slRelSle = new StringList(1);
                        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                        MapList mlConnectedChangeAction = domCADObj.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

                        if (!mlConnectedChangeAction.isEmpty()) {
                            for (int i = 0; i < mlConnectedChangeAction.size(); i++) {
                                Map mCAObj = (Map) mlConnectedChangeAction.get(i);
                                strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                                strCACurrent = (String) mCAObj.get(DomainConstants.SELECT_CURRENT);
                                DomainObject domobjCA = new DomainObject(strCAID);
                                // Check Change Action is in In Work State
                                if (!(strCACurrent.equalsIgnoreCase("In Work"))) {

                                    // Check Change Action is in In Review (In Approval) State
                                    if (strCACurrent.equalsIgnoreCase("In Approval")) {

                                        // Demote CAD To In work(Preliminary)and Demote Change Action To In work
                                        domobjCA.setState(context, "In Work");

                                        // Get Connected Change Order Objects

                                        String relpatternCA = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");
                                        String typePatternCO = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeOrder");

                                        MapList mlConnectedChangeOrder = domobjCA.getRelatedObjects(context, relpatternCA, typePatternCO, slObjectSle, slRelSle, true, false, (short) 1, null, null);

                                        if (!mlConnectedChangeOrder.isEmpty()) {
                                            for (int h = 0; h < mlConnectedChangeOrder.size(); h++) {
                                                Map mCOObj = (Map) mlConnectedChangeOrder.get(h);
                                                strCOID = (String) mCOObj.get(DomainConstants.SELECT_ID);
                                                strCOCurrent = (String) mCOObj.get(DomainConstants.SELECT_CURRENT);

                                                DomainObject domCOObj = new DomainObject(strCOID);

                                                if (!(strCOCurrent.equalsIgnoreCase("In Work"))) {
                                                    domCOObj.setState(context, "In Work");
                                                } else {
                                                    // Change Order is in In work
                                                }

                                            }
                                        }

                                    } else {
                                        // Change Action is not in In Review (In Approval) State
                                        // Throw error
                                        strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.CAInReview");
                                        MqlUtil.mqlCommand(context, "notice $1", strMessage);
                                    }
                                } else {
                                    // Change Action is in In Work
                                    // Demote CAD To In work(Preliminary)

                                }

                            }

                        }

                    }
                }
            } else {
                // Connected Change Action Objects
                relpattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAffectedItem");
                typePattern = PropertyUtil.getSchemaProperty(context, "type_ChangeAction");

                slObjectSle = new StringList(2);
                slObjectSle.addElement(DomainConstants.SELECT_ID);
                slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

                slRelSle = new StringList(1);
                slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                MapList mlConnectedChangeAction = domCADObj.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

                if (!mlConnectedChangeAction.isEmpty()) {
                    for (int i = 0; i < mlConnectedChangeAction.size(); i++) {
                        Map mCAObj = (Map) mlConnectedChangeAction.get(i);
                        strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                        strCACurrent = (String) mCAObj.get(DomainConstants.SELECT_CURRENT);
                        DomainObject domobjCA = new DomainObject(strCAID);
                        // Check Change Action is in In Work State
                        if (!(strCACurrent.equalsIgnoreCase("In Work"))) {

                            // Check Change Action is in In Review (In Approval) State
                            if (strCACurrent.equalsIgnoreCase("In Approval")) {

                                // Demote CAD To In work
                                domCADObj.setState(context, "In Work");
                                // Demote Change Action To In work
                                domobjCA.setState(context, "In Work");

                                // Get Connected Change Order Objects

                                String relpatternCA = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");
                                String typePatternCO = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeOrder");

                                MapList mlConnectedChangeOrder = domobjCA.getRelatedObjects(context, relpatternCA, typePatternCO, slObjectSle, slRelSle, true, false, (short) 1, null, null);

                                if (!mlConnectedChangeOrder.isEmpty()) {
                                    for (int h = 0; h < mlConnectedChangeOrder.size(); h++) {
                                        Map mCOObj = (Map) mlConnectedChangeOrder.get(h);
                                        strCOID = (String) mCOObj.get(DomainConstants.SELECT_ID);
                                        strCOCurrent = (String) mCOObj.get(DomainConstants.SELECT_CURRENT);

                                        DomainObject domCOObj = new DomainObject(strCOID);

                                        if (!(strCOCurrent.equalsIgnoreCase("In Work"))) {

                                            domCOObj.setState(context, "In Work");
                                        } else {
                                            // Change Order is in In work
                                        }

                                    }
                                }

                            } else {
                                // Change Action is not in In Review (In Approval) State
                                // Throw error
                                strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.CAInReview");
                                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                            }
                        } else {
                            // Change Action is in In Work
                            // Demote CAD To In work
                            domCADObj.setState(context, "In Work");
                        }

                    }

                }

            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in demoteCADIfChangeActionInWork: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

    }

    // Modified by KWagh - TIGTK-2772 -Start
    /**
     * This method is invoked via a Promote check trigger when the CO is promoted from Complete to Implemented
     * @param context
     * @param args
     * @throws Exception
     */
    public int checkForPreconditionsBeforePromotingCOToImplementedState(Context context, String[] args) throws Exception {
        String strCOId = args[0];
        int intReturn = 0;
        try {
            StringBuffer sMCOList = new StringBuffer();
            StringList objectSelects = new StringList(2);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_CURRENT);
            objectSelects.addElement(DomainConstants.SELECT_NAME);
            // PCM:TIGTK-3677 | 25/11/16 | Gautami: Start
            StringList slListForStateCheck = new StringList();
            slListForStateCheck.addElement(TigerConstants.STATE_PSS_MCO_COMPLETE);
            slListForStateCheck.addElement(TigerConstants.STATE_PSS_MCO_IMPLEMENTED);
            slListForStateCheck.addElement(TigerConstants.STATE_PSS_MCO_CANCELLED);
            slListForStateCheck.addElement(TigerConstants.STATE_PSS_MCO_REJECTED);
            // PCM:TIGTK-3677 | 25/11/16 | Gautami: End

            DomainObject domCO = new DomainObject(strCOId);
            MapList mlMCO = domCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, objectSelects,
                    new StringList(0), false, true, (short) 1, null, null, (short) 0);
            int nCount = mlMCO.size();

            if (mlMCO.isEmpty()) {
                intReturn = 0;
            } else {
                for (int i = 0; i < nCount; i++) {
                    Map mMCO = (Map) mlMCO.get(i);
                    String strMCOCurrent = (String) mMCO.get(DomainConstants.SELECT_CURRENT);
                    String strMCOName = (String) mMCO.get(DomainConstants.SELECT_NAME);
                    // PCM:TIGTK-3677 | 25/11/16 | Gautami: Start
                    if (!slListForStateCheck.contains(strMCOCurrent)) {
                        sMCOList.append(strMCOName + "\n");
                        intReturn = 1;
                    }
                    // PCM:TIGTK-3677 | 25/11/16 | Gautami: End
                }
            }

            // manual promotion is done when no CN is attached to the CO
            // For this option Code is Pending it will applied in future when CN functionality is developed
            if (intReturn == 1) {
                // PCM:TIGTK-3677 | 25/11/16 | Gautami: Start
                sMCOList.append(
                        EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.CORelatedMCONotCompleteOrImplemented"));
                // PCM:TIGTK-3677 | 25/11/16 | Gautami: End
                MqlUtil.mqlCommand(context, "notice $1", sMCOList.toString());
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForPreconditionsBeforePromotingCOToImplementedState: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return intReturn;
        // Modified for PCM by Ketaki Wagh-- For US051
    }

    // Modified by KWagh - TIGTK-2772 -End

    /**
     * This method is invoked via a Promote Override trigger when the CO is promoted from Complete to Implemented if no CN is attached to the CO & CR is in "In Process" state
     * @param context
     * @param args
     * @throws Exception
     */
    public int calledFromOverrideDuringPromoteCOToImplemented(Context context, String[] args) throws Exception {
        try {
            // Modified for PCM by Ketaki Wagh-- For US051
            String objectId = args[0];
            StringBuffer processStr = new StringBuffer();
            processStr.append("JSP:postProcess|commandName=PSS_PromoteCOToImplementCommand|objectId=");
            processStr.append(objectId);

            MqlUtil.mqlCommand(context, "notice $1", processStr.toString());
            return 1;
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in calledFromOverrideDuringPromoteCOToImplemented: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        // Modified for PCM by Ketaki Wagh-- For US051
    }

    /**
     * This method is invoked via 'PSS_PromoteCOToImplementCommand command' when the CO is promoted from Complete to Implemented if no CN is attached to the CO
     * @param context
     * @param args
     * @throws Exception
     */
    public void promoteCOToImplemented(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strCOId = (String) requestMap.get("objectId");
            // K wagh TIGTK-2772
            // Promote Change order To Implemented state
            DomainObject domCOID = new DomainObject(strCOId);
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            MqlUtil.mqlCommand(context, "trigger off");
            domCOID.setState(context, TigerConstants.STATE_CHANGEORDER_IMPLEMENTED);
            MqlUtil.mqlCommand(context, "trigger on");
            ContextUtil.popContext(context);
            boolean bolPromoteCR = true;
            String strChangeRequestId = "";
            String strChangeRequestBillableValue = "";
            Map mapCOObj;
            String strCOCurrentState = "";
            String strChangeOrderId = (String) requestMap.get("objectId");
            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);
            objSelect.add(DomainConstants.SELECT_CURRENT);
            objSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");

            // Get Connected Change Request of CO
            DomainObject domChangeOrderObject = DomainObject.newInstance(context, strChangeOrderId);
            MapList listConnectedChangeRequest = domChangeOrderObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEREQUEST, objSelect, null,
                    true, false, (short) 0, null, null, 0);
            if (listConnectedChangeRequest.size() != 0) {
                for (int i = 0; i < listConnectedChangeRequest.size(); i++) {
                    Map mapConnectedChangeRequest = (Map) listConnectedChangeRequest.get(i);
                    strChangeRequestId = (String) mapConnectedChangeRequest.get(DomainConstants.SELECT_ID);
                    strChangeRequestBillableValue = (String) mapConnectedChangeRequest.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");
                    DomainObject domChangeRequestObject = new DomainObject(strChangeRequestId);
                    MapList listConnectedChangeOrder = domChangeRequestObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEORDER, objSelect,
                            null, false, true, (short) 1, null, null);
                    if (listConnectedChangeOrder.size() != 0 && strChangeRequestBillableValue.equalsIgnoreCase("No")) {
                        for (int k = 0; k < listConnectedChangeOrder.size(); k++) {
                            mapCOObj = (Map) listConnectedChangeOrder.get(k);
                            strCOCurrentState = (String) mapCOObj.get(DomainConstants.SELECT_CURRENT);
                            if (strCOCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED)
                                    || strCOCurrentState.equalsIgnoreCase(TigerConstants.STATE_CHANGEORDER_IMPLEMENTED)) {
                            } else {
                                bolPromoteCR = false;
                                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                        "PSS_EnterpriseChangeMgt.Alert.COsNotInCompleteToCloseChangeRequest");
                                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                            }
                        }
                    }
                }
            }
            if (bolPromoteCR == true && strChangeRequestBillableValue.equalsIgnoreCase("No")) {
                DomainObject domChangeRequestObj = new DomainObject(strChangeRequestId);
                domChangeRequestObj.setState(context, TigerConstants.STATE_COMPLETE_CR);
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteCOToImplemented: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeAffectedItems(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strChangeId = (String) programMap.get("objectId");
        StringList strlAffItemList = new StringList();
        if (ChangeUtil.isNullOrEmpty(strChangeId))
            return strlAffItemList;
        try {
            setId(strChangeId);
            StringList changeActionList = getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].to.id");
            // [PTE]
            StringBuffer sbRelPattern = new StringBuffer(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
            sbRelPattern.append(",");
            // Findbug Issue correction start
            // Date: 22/03/2017
            // By: Asha G.
            sbRelPattern.append(StringUtil.join(ECMAdmin.getAllCustomChangeRels(context), ","));
            // Findbug Issue correction End
            MapList resultList = null;
            Map map = null;
            for (int i = 0; i < changeActionList.size(); i++) {
                setId((String) changeActionList.get(i));
                resultList = getRelatedObjects(context, sbRelPattern.toString(), "*", new StringList(DomainObject.SELECT_ID), null, false, true, (short) 2, DomainObject.EMPTY_STRING,
                        DomainObject.EMPTY_STRING);
                Iterator itr = resultList.iterator();
                while (itr.hasNext()) {
                    map = (Map) itr.next();
                    strlAffItemList.addElement((String) map.get(DomainObject.SELECT_ID));
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in excludeAffectedItems: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return strlAffItemList;
    }

    /**
     * This Trigger is Inactive, PCM : TIGTK-5278 : 31/05/2017 : AB : START, It will be Active in PHASE 2.0 This method is used for Check CAD is not Related to any Development part for RelationShip
     * ChangeAffectedItem's creation.
     * @param context
     *            Context : User's Context.
     * @param args
     *            String array
     * @return
     * @throws Exception
     *             if searching Parts object fails.
     */
    public int connectAffectedItemCheck(Context context, String args[]) throws Exception {
        try {
            String strToObjectId = args[1];

            DomainObject domToObject = DomainObject.newInstance(context, strToObjectId);
            String strObjectPolicy = (String) domToObject.getInfo(context, DomainConstants.SELECT_POLICY);
            String strObjectName = (String) domToObject.getInfo(context, DomainConstants.SELECT_NAME);
            String strObjectRevesion = (String) domToObject.getInfo(context, DomainConstants.SELECT_REVISION);

            // If Affected Item Is CAD then Check for connected Part objects
            if (strObjectPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT)) {
                StringList selectStmts = new StringList(1);
                selectStmts.addElement(DomainConstants.SELECT_ID);
                selectStmts.addElement(DomainConstants.SELECT_POLICY);
                StringList relStmts = new StringList(0);
                boolean bolNotDevPart = false;

                // Get CAD Connected Part list
                MapList mlCADConnectedPartObject = domToObject.getRelatedObjects(context, RELATIONSHIP_PART_SPECIFICATION, TYPE_PART, selectStmts, relStmts, true, false, (short) 1, null, null);

                if (mlCADConnectedPartObject.size() != 0) {
                    for (int j = 0; j < mlCADConnectedPartObject.size(); j++) {
                        Map<String, String> map = (Map<String, String>) mlCADConnectedPartObject.get(j);
                        String strPartPolicy = map.get(DomainConstants.SELECT_POLICY);

                        // Check if Policy of Part is Development Part
                        if (strPartPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
                            String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                    "PSS_EnterpriseChangeMgt.Alert.SelectedCADRelatedToDevPart");
                            strMessage = strMessage.replace("<$NAME>", strObjectName);
                            strMessage = strMessage.replace("<$REVISION>", strObjectRevesion);
                            MqlUtil.mqlCommand(context, "notice $1", strMessage);
                            return 1;
                        } else {
                            bolNotDevPart = true;
                        }
                    }

                    if (bolNotDevPart == true) {
                        return 0;
                    }
                }
            }

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectAffectedItemCheck: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

        return 0;
    }

    /**
     * This method is invoked via a Create Action trigger when the Change Action connected to Affected Item. If the selected Affected Item is of *Part type having Symmetrical Part/CAD object and if
     * Symmetrical Part/CAD is not already present in Change Action then add it automatically.
     * @param context
     *            Context : User's Context.
     * @param args
     *            String array
     * @return
     * @throws Exception
     *             if searching Parts object fails.
     */

    /**
     * @param context
     * @param args
     * @throws Exception
     *             This method is invoked via a Delete action trigger when we Remove the affected Item from MCO
     */
    public void deleteMCAOnLastAffectedItem(Context context, String args[]) throws Exception {
        try {
            String RELATIONSHIP_MFGCHANGEAFFECTEDITEM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingChangeAffectedItem");

            String strMCAObjectId = args[0];
            String strAffectedItemId = args[1];
            boolean bFlag = false;

            DomainObject domainMCAObj = new DomainObject(strMCAObjectId);

            StringList slSelectStmts = new StringList();
            StringList slRelStmts = new StringList();

            slSelectStmts.add(DomainConstants.SELECT_ID);
            slRelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

            MapList mlConnectList = domainMCAObj.getRelatedObjects(context, RELATIONSHIP_MFGCHANGEAFFECTEDITEM, "*", slSelectStmts, slRelStmts, false, true, (short) 1, null, null, 0);
            if (mlConnectList != null)
                for (int i = 0; i < mlConnectList.size(); i++) {
                    Map mapAffectedItem = (Map) mlConnectList.get(i);
                    String strAffectedId = (String) mapAffectedItem.get(DomainConstants.SELECT_ID);
                    if (!strAffectedId.equals(strAffectedItemId)) {
                        bFlag = true;
                    }
                }

            if (!bFlag) {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                domainMCAObj.deleteObject(context);
                ContextUtil.popContext(context);
            }

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in deleteMCAOnLastAffectedItem: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

    }

    @com.matrixone.apps.framework.ui.ExcludeOIDProgramCallable
    public StringList excludeAffectedItemsForCR(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strChangeId = (String) programMap.get("objectId");
        StringList strlAffItemList = new StringList();
        if (ChangeUtil.isNullOrEmpty(strChangeId))
            return strlAffItemList;
        try {
            setId(strChangeId);
            String relPattern = PropertyUtil.getSchemaProperty("relationship_PSS_AffectedItem");
            MapList resultList = null;
            Map map = null;
            resultList = getRelatedObjects(context, relPattern, "*", new StringList(DomainObject.SELECT_ID), null, false, true, (short) 2, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING);
            Iterator itr = resultList.iterator();
            while (itr.hasNext()) {
                map = (Map) itr.next();
                strlAffItemList.addElement((String) map.get(DomainObject.SELECT_ID));
            }
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in excludeAffectedItemsForCR: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return strlAffItemList;
    }

    /**
     * Method to get the "Route Template" name connected to "PSS Program Project" object
     * @param context
     * @param args
     *            - ObjectId of "Change Action"
     * @return -- String -- "Route Template" object Name
     * @throws Exception
     */
    public String getRouteTemplate(Context context, String objectId) throws Exception {
        String strRouteTemplateName = "";
        try {

            // Modified for PCM RFC033 for TS047 by Pooja Mantri
            final String RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONCO = "Approval List for Commercial update on CO";
            final String RANGE_APPROVAL_LIST_FORPROTOTYPEONCO = "Approval List for Prototype on CO";
            final String RANGE_APPROVAL_LIST_FORSERIALLAUNCHONCO = "Approval List for Serial Launch on CO";
            final String RANGE_APPROVAL_LIST_FORDESIGNSTUDYONCO = "Approval List for Design study on CO";
            final String RANGE_APPROVAL_LIST_FOROTHERPARTSONCO = "Approval List for Other Parts on CO";
            final String RANGE_APPROVAL_LIST_FORCADONCO = "Approval List for CAD on CO";
            final String RANGE_APPROVAL_LIST_FORSTANDARDPARTSONCO = "Approval List for Standard Parts on CO";
            // Modified for PCM RFC033 for TS047 by Pooja Mantri
            final String RANGE_OTHER = "Other";
            final String RANGE_DESIGN_STUDY = "Design study";
            final String RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION = "Serial Tool Launch/Modification";
            final String RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION = "Prototype Tool Launch/Modification";
            final String RANGE_COMMERCIAL_UPDATE = "Commercial Update";

            String strPSSRouteTemplateTypeValue = "";
            String strTypeAffectedItem = "";
            // Domain Object Instance
            DomainObject domCAObject = DomainObject.newInstance(context, objectId);

            // Get connected ChangeOrder objects with ChangeAction
            StringList objectSelects = new StringList(1);
            objectSelects.addElement(SELECT_ID);
            StringList relSelects = new StringList(1);
            relSelects.addElement(SELECT_RELATIONSHIP_ID);

            MapList changeOrderList = domCAObject.getRelatedObjects(context, // context
                    ChangeConstants.RELATIONSHIP_CHANGE_ACTION, // relationship pattern
                    TigerConstants.TYPE_PSS_CHANGEORDER, // object pattern
                    objectSelects, // object selects
                    relSelects, // relationship selects
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    "", // object where clause
                    "", // relationship where clause
                    (short) 0);

            for (int m = 0; m < changeOrderList.size(); m++) {
                Map mCOObj = (Map) changeOrderList.get(m);
                String strCOId = (String) mCOObj.get(DomainConstants.SELECT_ID);
                DomainObject domCOObj = new DomainObject(strCOId);
                String strPurposeOfRelease = domCOObj.getAttributeValue(context, "PSS_Purpose_Of_Release");

                // Get the Connected "PSS Program Project" to "PSS_ChangeOrder" with "PSS_ConnectedPCMData" relationship
                String strProgramProjectOID = (String) domCOObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

                // Create Mapping map for "PSS Program Project" object
                Map programProjectMap = new HashMap<>();
                programProjectMap.put(RANGE_COMMERCIAL_UPDATE, RANGE_APPROVAL_LIST_FORCOMMERCIALUPDATEONCO);
                programProjectMap.put(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION, RANGE_APPROVAL_LIST_FORPROTOTYPEONCO);
                programProjectMap.put(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION, RANGE_APPROVAL_LIST_FORSERIALLAUNCHONCO);
                programProjectMap.put(RANGE_DESIGN_STUDY, RANGE_APPROVAL_LIST_FORDESIGNSTUDYONCO);
                programProjectMap.put(RANGE_OTHER, RANGE_APPROVAL_LIST_FOROTHERPARTSONCO);

                // Creating "PSS Program Project" Object Instance
                DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);

                // Get connected Affected Item objects with ChangeOrder
                MapList affectedItemList = domCAObject.getRelatedObjects(context, // context
                        ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, // relationship pattern
                        DomainConstants.QUERY_WILDCARD, // object pattern
                        objectSelects, // object selects
                        relSelects, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        "", // object where clause
                        "", // relationship where clause
                        (short) 0);

                StringList busSelect = new StringList();
                // Check Type of connected Affected Item objects with ChangeOrder
                for (int j = 0; j < affectedItemList.size(); j++) {
                    Map domAffectedItemObj = (Map) affectedItemList.get(j);
                    String strAffectedId = (String) domAffectedItemObj.get(DomainConstants.SELECT_ID);
                    DomainObject domAffectedIdObj = new DomainObject(strAffectedId);
                    strTypeAffectedItem = domAffectedIdObj.getInfo(context, SELECT_TYPE);
                    String strPolicyAffectedItem = domAffectedIdObj.getInfo(context, SELECT_POLICY);

                    if (strTypeAffectedItem.equals(TYPE_PART)) {
                        if (strPurposeOfRelease.equals(RANGE_DESIGN_STUDY)) {
                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_DESIGN_STUDY);
                        } else if (strPurposeOfRelease.equals(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION)) {
                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_SERIAL_TOOL_LAUNCH_MODIFICATION);
                        } else if (strPurposeOfRelease.equals(RANGE_COMMERCIAL_UPDATE)) {
                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_COMMERCIAL_UPDATE);
                        } else if (strPurposeOfRelease.equals(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION)) {
                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_PROTOTYPE_TOOL_LAUNCH_MODIFICATION);
                        } else if (strPurposeOfRelease.equals(RANGE_OTHER)) {
                            strPSSRouteTemplateTypeValue = (String) programProjectMap.get(RANGE_OTHER);
                        }
                    } else if (strPolicyAffectedItem.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                        // strRouteTemplateName = "Approval List for CAD Route Template";
                        busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                                + RANGE_APPROVAL_LIST_FORCADONCO + "'].to.name");
                    } else if (strPolicyAffectedItem.equals(TigerConstants.POLICY_STANDARDPART)) {
                        // strRouteTemplateName = "Approval List for CAD Route Template";
                        busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                                + RANGE_APPROVAL_LIST_FORSTANDARDPARTSONCO + "'].to.name");
                    }

                }
                if (strTypeAffectedItem.equals(TYPE_PART)) {
                    busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                            + strPSSRouteTemplateTypeValue + "'].to.name");

                }
                Map mapRouteTemplateDetails = domProgramProjectObject.getInfo(context, busSelect);
                strRouteTemplateName = (String) mapRouteTemplateDetails.get("from[PSS_ConnectedRouteTemplates].to.name");
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getRouteTemplate: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return strRouteTemplateName;
    }

    /**
     * @author SteepGraph - Ketaki W., Ajay V., Dhiren P.
     * @param context
     * @param args
     *            - ObjectId of Change Order
     * @return
     * @throws Exception
     *             This method is invoked via a promote action trigger when the CO is promoted from In Review(In Approval) to Complete
     */
    public void promoteLinkedAffectedImplementedItemsToRelease(Context context, String[] args) throws Exception {
        boolean bIsContextPushed = false;
        try {
            // TIGTK-8543 - PTE - 2017-7-18 - START
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_PROMOTE_FROM_PCM", "True");
            // TIGTK-8543 - PTE - 2017-7-18 - END
            // String strAffectedItemswithRequestedChange = DomainConstants.EMPTY_STRING;
            StringBuffer sbAffectedItemswithRequestedChange = new StringBuffer();

            String strCOObjectID = args[0];

            StringList objectSelects = new StringList(1);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_POLICY);
            objectSelects.addElement(DomainConstants.SELECT_NAME);
            objectSelects.addElement(DomainConstants.SELECT_TYPE);
            objectSelects.addElement(DomainConstants.SELECT_CURRENT);
            objectSelects.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");

            StringList relSelects = new StringList(1);
            relSelects.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            relSelects.addElement(DomainConstants.SELECT_NAME);
            relSelects.addElement(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

            DomainObject domCOObj = DomainObject.newInstance(context, strCOObjectID);

            MapList mlConnectedCA = domCOObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, objectSelects, relSelects, false, true,
                    (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0);

            if (!mlConnectedCA.isEmpty()) {
                StringList slAffectedItemIDs = new StringList();
                Map<String, String> mpReasonForChange = new HashMap<String, String>();
                StringList slFloatCAD = new StringList();

                Pattern relPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
                relPattern.addPattern(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);

                Iterator<Map<?, ?>> itrCAs = mlConnectedCA.iterator();
                while (itrCAs.hasNext()) {
                    Map<?, ?> mpCA = itrCAs.next();
                    String strCAObjID = (String) mpCA.get(DomainConstants.SELECT_ID);
                    String strCAType = (String) mpCA.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");
                    DomainObject domCAObj = DomainObject.newInstance(context, strCAObjID);

                    MapList mlAffectedItems = domCAObj.getRelatedObjects(context, relPattern.getPattern(), DomainConstants.QUERY_WILDCARD, objectSelects, relSelects, false, true, (short) 1,
                            DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0);
                    if (!mlAffectedItems.isEmpty()) {
                        Iterator itrAffectedItem = mlAffectedItems.iterator();
                        while (itrAffectedItem.hasNext()) {
                            Map mpAffectedItem = (Map) itrAffectedItem.next();
                            String strAffectedItemObjID = (String) mpAffectedItem.get(DomainConstants.SELECT_ID);
                            String strRelName = (String) mpAffectedItem.get(DomainConstants.SELECT_NAME);
                            slAffectedItemIDs.addElement(strAffectedItemObjID);
                            String strRequestedChange = (String) mpAffectedItem.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                            mpReasonForChange.put(strAffectedItemObjID, strRequestedChange);
                            if (!strCAType.equalsIgnoreCase(TigerConstants.ATTRIBUTE_PSS_CATYPE_CAD) && strRelName.equalsIgnoreCase(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM)) {
                                strRequestedChange = strRequestedChange.replace(" ", "_");
                                // strAffectedItemswithRequestedChange = strAffectedItemswithRequestedChange+strAffectedItemObjID+"^"+strRequestedChange+"|";
                                sbAffectedItemswithRequestedChange.append(strAffectedItemObjID + "^" + strRequestedChange + "|");
                            }

                        }
                    }
                }

                PropertyUtil.setGlobalRPEValue(context, "CompletingCOId", strCOObjectID);
                PropertyUtil.setGlobalRPEValue(context, "sbAffectedItemswithRequestedChange", sbAffectedItemswithRequestedChange.toString());

                Map<String, StringList> mpGroupedAffectedItemsBasedOnPolicy = getGroupedAffectedItemsBasedOnPolicy(context, slAffectedItemIDs);

                String[] strArraySortedPolicyAsKey = { TigerConstants.POLICY_PSS_Legacy_CAD, TigerConstants.POLICY_PSS_CADOBJECT, TigerConstants.POLICY_STANDARDPART,
                        TigerConstants.POLICY_PSS_ECPART };

                for (String strPolicyKey : strArraySortedPolicyAsKey) {

                    StringList slGroupedAffectedItemsToPromote = mpGroupedAffectedItemsBasedOnPolicy.get(strPolicyKey);

                    pss.ecm.ui.MfgChangeOrder_mxJPO MfgChangeOrderBase = new pss.ecm.ui.MfgChangeOrder_mxJPO();
                    MapList mlOrderdAffectedItemsToPromote = MfgChangeOrderBase.getOrderedParentChild(context, slGroupedAffectedItemsToPromote);

                    if (!mlOrderdAffectedItemsToPromote.isEmpty()) {
                        Iterator itrAffectedItemToPromote = mlOrderdAffectedItemsToPromote.iterator();
                        while (itrAffectedItemToPromote.hasNext()) {
                            Map mpAffectedItem = (Map) itrAffectedItemToPromote.next();
                            String strItemObjId = (String) mpAffectedItem.get(DomainConstants.SELECT_ID);
                            DomainObject domItemObj = DomainObject.newInstance(context, strItemObjId);
                            String strRequestedChange = (String) mpReasonForChange.get(strItemObjId);
                            String strItemState = domItemObj.getInfo(context, DomainConstants.SELECT_CURRENT);
                            String strItemPolicy = domItemObj.getInfo(context, DomainConstants.SELECT_POLICY);
                            if (!ChangeConstants.FOR_NONE.equalsIgnoreCase(strRequestedChange)) {
                                String strItemTargetState = "";
                                String strReleaseState = PropertyUtil.getSchemaProperty(context, "policy", strItemPolicy, "state_Release");
                                String strObsoleteState = PropertyUtil.getSchemaProperty(context, "policy", strItemPolicy, "state_Obsolete");
                                if (strItemState.equalsIgnoreCase(strObsoleteState))
                                    continue;
                                else if (strItemState.equalsIgnoreCase(strReleaseState) && !TigerConstants.FOR_CLONE.equalsIgnoreCase(strRequestedChange)) {
                                    strItemTargetState = strObsoleteState;
                                } else {
                                    strItemTargetState = strReleaseState;
                                }

                                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                                bIsContextPushed = true;
                                /*
                                 * if (TigerConstants.POLICY_PSS_CADOBJECT.equalsIgnoreCase(strItemPolicy) && strItemTargetState.equals(strObsoleteState)) { MqlUtil.mqlCommand(context, "trigger on");
                                 * domItemObj.setState(context, strItemTargetState); MqlUtil.mqlCommand(context, "trigger off"); } else
                                 */
                                domItemObj.setState(context, strItemTargetState);

                                if (TigerConstants.POLICY_PSS_CADOBJECT.equalsIgnoreCase(strItemPolicy) && (!strItemState.equalsIgnoreCase(TigerConstants.STATE_RELEASED_CAD_OBJECT))) {
                                    slFloatCAD.add(strItemObjId);
                                }
                            }
                        }
                    }
                }

                if (!slFloatCAD.isEmpty()) {
                    for (int m = 0; m < slFloatCAD.size(); m++) {
                        pss.cad2d3d.DECTGUtil_mxJPO dectgUTILL = new pss.cad2d3d.DECTGUtil_mxJPO();
                        String[] argsJPO = new String[1];
                        argsJPO[0] = (String) slFloatCAD.get(m);
                        dectgUTILL.propagateCADSubComponentTonewRev(context, argsJPO);
                        dectgUTILL.floatPartSpecification(context, argsJPO);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Error in promoteLinkedAffectedImplementedItemsToRelease: ", ex);
            throw ex;
        } finally {
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_PROMOTE_FROM_PCM", "");
            if (bIsContextPushed) {
                ContextUtil.popContext(context);
            }
        }
    }

    /**
     * @author SteepGraph
     * @param context
     * @param slAffectedItemIDs
     *            - ObjectId of Affected Items
     * @return
     * @throws FrameworkException
     *             Method used to group Affected Items based on policy
     */
    private Map<String, StringList> getGroupedAffectedItemsBasedOnPolicy(Context context, StringList slAffectedItemIDs) throws FrameworkException {
        Map<String, StringList> mpOrderedAffectedItemsBasedOnPolicy = new HashMap<String, StringList>();
        StringList slECParts = new StringList();
        StringList slSTDParts = new StringList();
        StringList slCADs = new StringList();
        StringList slLegacyCADs = new StringList();

        for (Object objAffectedItemId : slAffectedItemIDs) {
            String strAffectedItemObjID = (String) objAffectedItemId;
            DomainObject domAffectedItemObj = DomainObject.newInstance(context, strAffectedItemObjID);
            String strAffectedItemPolicy = domAffectedItemObj.getInfo(context, DomainConstants.SELECT_POLICY);
            if (TigerConstants.POLICY_PSS_ECPART.equalsIgnoreCase(strAffectedItemPolicy)) {
                slECParts.add(strAffectedItemObjID);
                mpOrderedAffectedItemsBasedOnPolicy.put(strAffectedItemPolicy, slECParts);
            } else if (TigerConstants.POLICY_PSS_CADOBJECT.equalsIgnoreCase(strAffectedItemPolicy)) {
                slCADs.add(strAffectedItemObjID);
                mpOrderedAffectedItemsBasedOnPolicy.put(strAffectedItemPolicy, slCADs);
            } else if (TigerConstants.POLICY_PSS_Legacy_CAD.equalsIgnoreCase(strAffectedItemPolicy)) {
                slLegacyCADs.add(strAffectedItemObjID);
                mpOrderedAffectedItemsBasedOnPolicy.put(strAffectedItemPolicy, slLegacyCADs);
            } else if (TigerConstants.POLICY_STANDARDPART.equalsIgnoreCase(strAffectedItemPolicy)) {
                slSTDParts.add(strAffectedItemObjID);
                mpOrderedAffectedItemsBasedOnPolicy.put(strAffectedItemPolicy, slSTDParts);
            }
        }
        return mpOrderedAffectedItemsBasedOnPolicy;
    }

    /**
     * @param context
     *            ()
     * @param args
     * @return
     * @throws Exception
     *             Custom method to Update Project code at creation Of Change Order Added for TS073
     */

    public String getProjectName(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String strfunctionality = (String) requestMap.get("functionality");
        String strParentOID = (String) requestMap.get("parentOID");

        if (UIUtil.isNotNullAndNotEmpty(strfunctionality) && (strfunctionality.equalsIgnoreCase("MoveToNewCO"))) {
            strParentOID = (String) requestMap.get("objectId");
        }
        DomainObject domParentOID = new DomainObject(strParentOID);
        StringBuffer strBuf = new StringBuffer();
        String strProgProjId = "";
        String strProjId = "";
        String strProgramProjectId = "";
        String strProgramProjectName = "";

        String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPCMData");
        String typePattern = PropertyUtil.getSchemaProperty(context, "type_PSS_ProgramProject");

        if (UIUtil.isNotNullAndNotEmpty(strParentOID)) {
            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            String strParentObjectType = (String) domParentOID.getInfo(context, DomainConstants.SELECT_TYPE);
            if (strParentObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST) || strParentObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEORDER)) {
                // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                MapList mlCRConnectedProgramProject = domParentOID.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

                if (mlCRConnectedProgramProject.size() != 0) {
                    for (int j = 0; j < mlCRConnectedProgramProject.size(); j++) {
                        Map mapProgramProject = (Map) mlCRConnectedProgramProject.get(j);
                        strProgramProjectId = (String) mapProgramProject.get(DomainConstants.SELECT_ID);
                        strProgramProjectName = (String) mapProgramProject.get(DomainConstants.SELECT_NAME);
                    }
                }
            }
            if (strParentObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_PROGRAMPROJECT)) {
                strProgramProjectId = strParentOID;
                strProgramProjectName = domParentOID.getInfo(context, DomainConstants.SELECT_NAME);
            }
            strBuf.append("<input type=\"textbox\" id=\"ProjectCode\" value=\"" + strProgramProjectName + "\" name=\"ProjectCode\" readOnly='true'/>");
            strBuf.append("<input type=\"hidden\" id=\"ProjectCodeId\" value=\"" + strProgramProjectId + "\" name=\"ProjectCodeId\" />");

        } else {
            MapList mlProgramProjectList;
            StringList objectSelect = new StringList();
            objectSelect.add(DomainObject.SELECT_ID);
            objectSelect.add(DomainObject.SELECT_NAME);
            objectSelect.add(DomainObject.SELECT_DESCRIPTION);

            String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, context.getUser());
            String strCollaborativeSpace = (strSecurityContext.split("[.]")[2]);
            String queryLimit = "0";
            mlProgramProjectList = DomainObject.findObjects(context, "PSS_ProgramProject", // type keyed in or selected from type chooser
                    "*", "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION,
                    "project==" + strCollaborativeSpace + " && current!=Active && from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]==False", "", // save to the .finder later
                    false, objectSelect, Short.parseShort(queryLimit), "*", "");
            if (mlProgramProjectList.size() > 0) {
                Map mPrj = (Map) mlProgramProjectList.get(0);
                strProgProjId = (String) mPrj.get(DomainObject.SELECT_NAME);
                strProjId = (String) mPrj.get(DomainObject.SELECT_ID);
            }

            strBuf.append("<input type=\"textbox\" id=\"ProjectCode\" value=\"" + strProgProjId + "\" name=\"ProjectCode\" readOnly='true'/>");
            strBuf.append("<input type=\"hidden\" id=\"ProjectCodeId\" value=\"" + strProjId + "\" name=\"ProjectCodeId\" />");
        }

        return strBuf.toString();
    }

    /**
     * @param context
     *            ()
     * @param args
     * @return
     * @throws Exception
     *             Custom method to Update Program-Project Description at creation Of Change Order Added for TS073
     */

    public String getProjectDescription(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get("requestMap");
        String strParentOID = (String) requestMap.get("parentOID");
        DomainObject domParentOID = new DomainObject(strParentOID);
        MapList mlProgramProjectList;
        String strProgProjId = "";
        String strProgramProjectDescription = "";
        StringBuffer strBuf = new StringBuffer();
        String relpattern = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPCMData");
        String typePattern = PropertyUtil.getSchemaProperty(context, "type_PSS_ProgramProject");

        if (UIUtil.isNotNullAndNotEmpty(strParentOID)) {
            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_DESCRIPTION);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            String strParentObjectType = (String) domParentOID.getInfo(context, DomainConstants.SELECT_TYPE);
            if (strParentObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                MapList mlCRConnectedProgramProject = domParentOID.getRelatedObjects(context, relpattern, typePattern, slObjectSle, slRelSle, true, false, (short) 1, null, null, 0);

                if (mlCRConnectedProgramProject.size() != 0) {
                    for (int j = 0; j < mlCRConnectedProgramProject.size(); j++) {
                        Map mapProgramProject = (Map) mlCRConnectedProgramProject.get(j);
                        strProgramProjectDescription = (String) mapProgramProject.get(DomainConstants.SELECT_DESCRIPTION);
                    }
                }
            }
            if (strParentObjectType.equalsIgnoreCase(TigerConstants.TYPE_PSS_PROGRAMPROJECT)) {
                strProgramProjectDescription = domParentOID.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
            }
            strBuf.append("<TextArea id=\"ProjectCode\" name=\"ProjectCode\" readOnly='true'>" + strProgramProjectDescription + "</TextArea>");

        } else {
            StringList objectSelect = new StringList();
            objectSelect.add(DomainObject.SELECT_ID);
            objectSelect.add(DomainObject.SELECT_NAME);
            objectSelect.add(DomainObject.SELECT_DESCRIPTION);

            String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, context.getUser());

            String strCollaborativeSpace = (strSecurityContext.split("[.]")[2]);
            String queryLimit = "0";
            mlProgramProjectList = DomainObject.findObjects(context, "PSS_ProgramProject", // type keyed in or selected from type chooser
                    "*", "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION,
                    "project==" + strCollaborativeSpace + " && current!=Active && from[" + TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT + "]==False", "", // save to the .finder later
                    false, objectSelect, Short.parseShort(queryLimit), "*", "");

            if (mlProgramProjectList.size() > 0) {
                Map mPrj = (Map) mlProgramProjectList.get(0);
                strProgProjId = (String) mPrj.get(DomainObject.SELECT_ID);
                DomainObject doProgProj = DomainObject.newInstance(context, strProgProjId);
                strProgramProjectDescription = (String) doProgProj.getInfo(context, DomainObject.SELECT_DESCRIPTION);
            }
            strBuf.append("<TextArea id=\"ProjectCode\" name=\"ProjectCode\" readOnly='true'>" + strProgramProjectDescription + "</TextArea>");

        }

        return strBuf.toString();
    }

    /**
     * This method is called as "Range Function" on "PSS Purpose Of Release" field".
     * @param context
     * @param args
     * @author -- Pooja Mantri -- JIRA 2205
     * @return Map - Contains the attribute ranges
     * @throws Exception
     */
    public Map getDefaultPurposeOfReleaseFromCR(Context context, String args[]) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            final String FIELD_CHOICES = "field_choices";
            final String FIELD_DISPLAY_CHOICES = "field_display_choices";

            HashMap programMap = JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            boolean isPart = false;
            // initialize the Stringlist fieldRangeValues, fieldDisplayRangeValues
            StringList fieldRangeValues = new StringList();
            StringList fieldDisplayRangeValues = new StringList();

            String strCRObjectId = (String) requestMap.get("objectId");

            StringList attrRanges = FrameworkUtil.getRanges(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);
            attrRanges.sort();
            String strAttributeRange = "";
            if (UIUtil.isNotNullAndNotEmpty(strCRObjectId)) {
                // Creating DomainObject Instance of "PSS_ChangeRequest" Object
                DomainObject domPSSChangeRequestObject = DomainObject.newInstance(context, strCRObjectId);

                // Get "PSS_Purpose_Of_Release" attribute value for "PSS_ChangeRequest" Object
                String strPSSCOPurposeOfRelease = domPSSChangeRequestObject.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");

                // Get attribute ranges for attribute "PSS_Purpose_Of_Release"

                if (UIUtil.isNotNullAndNotEmpty(strPSSCOPurposeOfRelease)) {
                    attrRanges.remove("");
                    for (int i = 0; i < attrRanges.size(); i++) {
                        strAttributeRange = (String) attrRanges.get(i);
                        if (strAttributeRange.equalsIgnoreCase(strPSSCOPurposeOfRelease)) {
                            fieldDisplayRangeValues.add(0, strAttributeRange);
                            fieldRangeValues.add(0, strAttributeRange);

                        } else {
                            fieldRangeValues.add(strAttributeRange);
                            fieldDisplayRangeValues.add(strAttributeRange);
                        }
                    }
                } else {
                    isPart = true;
                }

            } else {
                isPart = true;
            }

            if (isPart) {
                for (int i = 0; i < attrRanges.size(); i++) {
                    strAttributeRange = (String) attrRanges.get(i);
                    fieldRangeValues.add(strAttributeRange);
                    fieldDisplayRangeValues.add(strAttributeRange);
                }
            }
            returnMap.put(FIELD_CHOICES, fieldRangeValues);
            returnMap.put(FIELD_DISPLAY_CHOICES, fieldDisplayRangeValues);
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getDefaultPurposeOfReleaseFromCR: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return returnMap;
    }

    // PCM TIGTK-3032 & TIGTK-2825 | 12/09/16 : Ketaki Wagh : Start
    /**
     * Added by Sabari for triggers development-Start This Trigger is used for Promotion of MCA In Review to Complete promote Affected Item to Approve If it is last MCA, promote MCO to Complete
     * @param context
     * @param args
     * @throws Exception
     */
    public void promoteLastCOorMCOToCompleteState(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        HashSet<String> stateSet = new HashSet<>();
        String strMCAID = args[0];
        String strNewMCAID = DomainConstants.EMPTY_STRING;
        String strMCACurrent = DomainConstants.EMPTY_STRING;
        String strComment = DomainConstants.EMPTY_STRING;
        DomainObject domMCAObj = new DomainObject(strMCAID);

        StringList sList = new StringList(2);
        sList.addElement(DomainConstants.SELECT_ID);
        sList.addElement(DomainConstants.SELECT_NAME);
        sList.addElement(DomainConstants.SELECT_CURRENT);
        // TIGTK-7057:Rutuja Ekatpure:15/6/2017:start
        // Get Related Affected Items
        StringList SlItemOID = domMCAObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.id");
        // order Affected Item list in sequence of child first and then parent
        pss.ecm.ui.MfgChangeOrder_mxJPO MfgChangeOrderBase = new pss.ecm.ui.MfgChangeOrder_mxJPO();
        MapList mlSortedOID = MfgChangeOrderBase.getOrderedParentChild(context, SlItemOID);

        for (Object objAI : mlSortedOID) {
            Map mAffectedItem = (Map) objAI;
            String strAffectedItemID = (String) mAffectedItem.get(DomainConstants.SELECT_ID);
            DomainObject domAffectedItem = new DomainObject(strAffectedItemID);
            String strAffItemCurrentState = domAffectedItem.getInfo(context, DomainConstants.SELECT_CURRENT);
            if (TigerConstants.STATE_PSS_MBOM_REVIEW.equalsIgnoreCase(strAffItemCurrentState)) {
                // Approve the Signature between Review and Approved state
                SignatureList sigList = domAffectedItem.getSignatures(context, strAffItemCurrentState, TigerConstants.STATE_MBOM_APPROVED);
                Iterator<Signature> sigItr = sigList.iterator();
                while (sigItr.hasNext()) {
                    Signature sig = (Signature) sigItr.next();
                    strComment = "Approved";
                    domAffectedItem.approveSignature(context, sig, strComment);
                }
                // Approve Affected Item
                domAffectedItem.setState(context, TigerConstants.STATE_MBOM_APPROVED);

            }

        }

        // Promote MCO to Complete
        // TIGTK-7057:Rutuja Ekatpure:15/6/2017:End
        MapList mlConnectedMCO = (MapList) domMCAObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, sList,
                DomainObject.EMPTY_STRINGLIST, true, false, (short) 1, null, null, 0);

        Map mMCO = (Map) mlConnectedMCO.get(0);
        String strMCOID = (String) mMCO.get(DomainObject.SELECT_ID);
        DomainObject domMCO = new DomainObject(strMCOID);

        // Get Connected Mfg Change Action

        MapList mlConnectedMCAs = (MapList) domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, sList,
                DomainObject.EMPTY_STRINGLIST, false, true, (short) 1, null, null, 0);

        int nMCACount = mlConnectedMCAs.size();

        // Check All Mfg Change Action objects are in complete state Or not

        if (!mlConnectedMCAs.isEmpty()) {
            if (nMCACount == 1) {

                for (int j = 0; j < nMCACount; j++) {
                    Map mMCA = (Map) mlConnectedMCAs.get(j);
                    strNewMCAID = (String) mMCA.get(DomainConstants.SELECT_ID);
                    if (strNewMCAID.equalsIgnoreCase(strMCAID)) {

                        // Promote MCO to Complete
                        // PCM TIGTK-3588 | 21/11/16 :Pooja Mantri : Start
                        domMCO.promote(context);
                        // PCM TIGTK-3588 | 21/11/16 :Pooja Mantri : End
                    }
                }
            } else {

                for (int k = 0; k < nMCACount; k++) {
                    Map mMCA = (Map) mlConnectedMCAs.get(k);
                    strNewMCAID = (String) mMCA.get(DomainConstants.SELECT_ID);
                    strMCACurrent = (String) mMCA.get(DomainConstants.SELECT_CURRENT);

                    if (!(strNewMCAID.equalsIgnoreCase(strMCAID))) {

                        stateSet.add(strMCACurrent);

                    }
                }

                if (stateSet.size() > 1) {

                    // do nothing

                } else {
                    if (stateSet.iterator().hasNext()) {
                        String sState = stateSet.iterator().next();

                        if (TigerConstants.STATE_PSS_MCA_COMPLETE.equalsIgnoreCase(sState)) {
                            // Promote MCO to Complete
                            // PCM TIGTK-3588 | 21/11/16 :Pooja Mantri : Start
                            domMCO.promote(context);
                            // PCM TIGTK-3588 | 21/11/16 :Pooja Mantri : End
                        }
                    }
                }

            }
        }
    }

    // PCM TIGTK-3032 & TIGTK-2825 | 12/09/16 : Ketaki Wagh : End

    // Modified by KWagh - TIGTK-2772 -Start
    // To Check Related CO and MCO state are in Implmented or cancelled
    public int checkRelatedCOandMCOsState(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String strCRID = args[0];
        int iResult = 0;
        Locale strLocale = context.getLocale();
        // TIGTK-12921 : 24-01-2018 : START
        StringList slCRAttribute = new StringList();
        slCRAttribute.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
        slCRAttribute.add("attribute[" + TigerConstants.ATTRIBUTE_BRANCH_TO + "]");
        // TIGTK-12921 : 24-01-2018 : END
        DomainObject domCRObj = new DomainObject(strCRID);
        // PCM TIGTK-6452/ ALM 3382 | 14/04/17 :KWagh : Start
        // TIGTK-12921 : 24-01-2018 : START
        Map<String, String> mpCRInfo = domCRObj.getInfo(context, slCRAttribute);
        String strCRType = mpCRInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
        String strBranchTo = mpCRInfo.get("attribute[" + TigerConstants.ATTRIBUTE_BRANCH_TO + "]");
        // TIGTK-12921 : 24-01-2018 : END
        // Concat CO and MCO Realtionship
        String strRelPattern = TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "," + ChangeConstants.RELATIONSHIP_CHANGE_ORDER;
        // Concat CO and MCO Type
        String strTypePattern = TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER + "," + TigerConstants.TYPE_PSS_CHANGEORDER;
        StringList objectselect = new StringList(2);
        objectselect.addElement(DomainConstants.SELECT_ID);
        objectselect.addElement(DomainConstants.SELECT_TYPE);
        objectselect.addElement(DomainConstants.SELECT_NAME);
        objectselect.addElement(DomainConstants.SELECT_CURRENT);
        // TIGTK-12921 : 24-01-2018 : START
        StringBuffer sbBusWhere = new StringBuffer();
        sbBusWhere.append("current != '");
        sbBusWhere.append(TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED);
        sbBusWhere.append("' && current != '");
        sbBusWhere.append(TigerConstants.STATE_PSS_MCO_CANCELLED);
        sbBusWhere.append("'");

        if (!TigerConstants.STATE_REJECTED_CR.equals(strBranchTo)) {
            sbBusWhere.append(" && current != '");
            sbBusWhere.append(TigerConstants.STATE_CHANGEORDER_IMPLEMENTED);
            sbBusWhere.append("' && current != '");
            sbBusWhere.append(TigerConstants.STATE_PSS_MCO_IMPLEMENTED);
            sbBusWhere.append("' && current != '");
            sbBusWhere.append(TigerConstants.STATE_PSS_MCO_REJECTED);
            sbBusWhere.append("'");
        }
        // TIGTK-12921 : 24-01-2018 : END
        // PCM TIGTK-4279: 03/09/2017 : PTE : START
        // TIGTK-8451:Rutuja Ekatpure:8/6/2015:Start
        StringList slConnectedCO = domCRObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].to.id");
        StringList slConnectedMCO = domCRObj.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "].to.id");
        // TIGTK-12921 : 24-01-2018 : START
        if (!TigerConstants.STATE_REJECTED_CR.equals(strBranchTo) && slConnectedCO.isEmpty() && slConnectedMCO.isEmpty() && !TigerConstants.PROGRAM_CR.equalsIgnoreCase(strCRType)) {

            // TIGTK-12921 : 23-01-2018 : END
            String strNoCOConnectedMsg = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", strLocale, "PSS_EnterpriseChangeMgt.Alert.strNoCOMCOConnectedMsg");
            MqlUtil.mqlCommand(context, "notice $1", strNoCOConnectedMsg);
            iResult = 1;
        }
        // TIGTK-8451:Rutuja Ekatpure:8/6/2015:End
        // PCM TIGTK-6452/ ALM 3382 | 14/04/17 :KWagh : Start
        // PCM TIGTK-4279: 03/09/2017 : PTE : Ends
        MapList mlconnectedCOMCO = (MapList) domCRObj.getRelatedObjects(context, strRelPattern, strTypePattern, objectselect, DomainObject.EMPTY_STRINGLIST, false, true, (short) 1,
                sbBusWhere.toString(), null, 0);

        StringList slAlert = new StringList();
        // TIGTK-12921 : 24-01-2018 : START
        for (Object objCOMCOInfo : mlconnectedCOMCO) {
            Map<?, ?> mconnectedCOMCO = (Map<?, ?>) objCOMCOInfo;
            // TIGTK-12921 : 24-01-2018 : END
            String strType = (String) mconnectedCOMCO.get(DomainConstants.SELECT_TYPE);
            String strCurrent = (String) mconnectedCOMCO.get(DomainConstants.SELECT_CURRENT);
            String strName = (String) mconnectedCOMCO.get(DomainConstants.SELECT_NAME);
            if (strType.equalsIgnoreCase("PSS_ChangeOrder")) {
                strType = "Change Order";
            } else if (strType.equalsIgnoreCase("PSS_ManufacturingChangeOrder")) {
                strType = "Mfg Change Order";
            }
            slAlert.add("Type  :" + strType + ", Name  : " + strName + ", Current : " + strCurrent);

        }
        // TIGTK-12921 : 24-01-2018 : START
        if (!slAlert.isEmpty()) {
            String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", strLocale,
                    "PSS_EnterpriseChangeMgt.Alert.NoConnectedChangeItemsInRequiredState");
            if (TigerConstants.STATE_REJECTED_CR.equals(strBranchTo)) {
                strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", strLocale, "PSS_EnterpriseChangeMgt.Alert.NoConnectedChangeItemsAreCancelled");
            }
            strAlertMessage = strAlertMessage.concat(FrameworkUtil.join(slAlert, "\n"));
            emxContextUtilBase_mxJPO.mqlNotice(context, strAlertMessage);
            iResult = 1;
        }
        // TIGTK-12921 : 24-01-2018 : END
        return iResult;

    }

    // This Trigger is used to Promote CO/MCO to Implemented from Complete state.
    // If it is last CO/MCO Promote and CR is Non Billable then promote CR to "In Process" to "Complete" state
    public void promoteConnectedCRToCompleteState(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        String ObjectID = args[0];
        String object_Policy = "";
        String strRelPattern = "";
        String strTypePattern = "";
        DomainObject domObj = new DomainObject(ObjectID);
        StringList busSelects = new StringList(2);
        busSelects.add(DomainConstants.SELECT_POLICY);
        Map boInfo = domObj.getInfo(context, busSelects);
        object_Policy = (String) boInfo.get(DomainConstants.SELECT_POLICY);
        String obj_Policy = PropertyUtil.getSchemaProperty(context, "policy_PSS_ChangeOrder");
        strTypePattern = PropertyUtil.getSchemaProperty(context, "type_PSS_ChangeRequest");
        if (object_Policy.equals(obj_Policy)) {
            strRelPattern = PropertyUtil.getSchemaProperty(context, "relationship_ChangeOrder");
        } else {
            strRelPattern = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingChangeOrder");
        }
        StringList sList = new StringList(2);
        sList.addElement(DomainConstants.SELECT_ID);
        sList.addElement(DomainConstants.SELECT_NAME);
        String strBusWhereclause = "attribute[PSS_CRBillable] == No";
        MapList mlConnectedList = (MapList) domObj.getRelatedObjects(context, strRelPattern, strTypePattern, sList, DomainObject.EMPTY_STRINGLIST, true, false, (short) 1, strBusWhereclause, null, 0);
        int listSize = mlConnectedList.size();

        if (listSize > 0) {
            final String ATTRIBUTE_COMMENTS = PropertyUtil.getSchemaProperty(context, "attribute_Comments");
            // Concat CO and MCO Realtionship
            strRelPattern = TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER + "," + ChangeConstants.RELATIONSHIP_CHANGE_ORDER;
            // Concat CO and MCO Type
            strTypePattern = TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER + "," + TigerConstants.TYPE_PSS_CHANGEORDER;
            for (int i = 0; i < listSize; i++) {
                Map mTemp = (Map) mlConnectedList.get(i);
                String sCRID = (String) mTemp.get(DomainObject.SELECT_ID);

                StringList selectRelatedList = new StringList(2);
                selectRelatedList.addElement(DomainConstants.SELECT_ID);
                selectRelatedList.addElement(DomainConstants.SELECT_NAME);
                selectRelatedList.addElement(DomainConstants.SELECT_CURRENT);

                String strBusWhereclause2 = "current != Implemented && current != Cancelled && current != Rejected";
                DomainObject doCR = new DomainObject(sCRID);
                MapList mlCRConnectedList = (MapList) doCR.getRelatedObjects(context, strRelPattern, strTypePattern, selectRelatedList, DomainObject.EMPTY_STRINGLIST, false, true, (short) 1,
                        strBusWhereclause2, null, 0);

                int sizeCR = mlCRConnectedList.size();

                if (sizeCR == 0) {
                    MapList mlCOAndMCO = (MapList) doCR.getRelatedObjects(context, strRelPattern, strTypePattern, selectRelatedList, DomainObject.EMPTY_STRINGLIST, false, true, (short) 1, null, null,
                            0);
                    StringBuffer strCRAndMCO = new StringBuffer();
                    for (int cnt = 0; cnt < mlCOAndMCO.size(); cnt++) {
                        Map mCOOrMCO = (Map) mlCOAndMCO.get(cnt);
                        // Findbug Issue correction start
                        // Date: 22/03/2017
                        // By: Asha G.
                        strCRAndMCO.append("Name: ");
                        strCRAndMCO.append(mCOOrMCO.get(DomainObject.SELECT_NAME));
                        strCRAndMCO.append("   State: ");
                        strCRAndMCO.append(mCOOrMCO.get(DomainObject.SELECT_CURRENT));
                        // Findbug Issue correction End
                    }
                    // TIGTK-7702 :Rutuja Ekatpure:19/5/2017:Start

                    String strCRCurrentState = doCR.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if (TigerConstants.STATE_PSS_CR_INPROCESS.equalsIgnoreCase(strCRCurrentState)) {
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        doCR.setAttributeValue(context, ATTRIBUTE_COMMENTS, strCRAndMCO.toString());
                        doCR.setState(context, TigerConstants.STATE_COMPLETE_CR);
                        ContextUtil.popContext(context);
                    }

                    // TIGTK-7702 :Rutuja Ekatpure:19/5/2017:End
                    // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Starts
                    emxNotificationUtil_mxJPO.objectNotification(context, sCRID, "PSS_CRCompletePromoteNotification", null);
                    // TGPSS_PCM-TS152 Change Request Notifications_V2.2 | 07/03/2017 |Harika Varanasi : Ends
                }
            }
        }
    }

    // Modified by KWagh - TIGTK-2772 -End

    // PCM TIGTK-3807 : 16/12/2016 : KWagh : Start
    /**
     * @Description : This method used for demote CO from UI Command cancel CO. When CO is in prepare State Author : Swapnil Input : context , args[objectid]
     **/
    public void cancelChangeOrderInPrepareState(Context context, String[] args) throws Exception {

        String strRelPattern;
        StringList objSelect = new StringList();
        objSelect.add(DomainConstants.SELECT_ID);
        objSelect.add(DomainConstants.SELECT_CURRENT);
        objSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");
        String strCAID;
        HashMap hmParam = (HashMap) JPO.unpackArgs(args);

        String objectId = (String) hmParam.get("busObjId");

        DomainObject domObjCO = new DomainObject(objectId);
        StringList slObjectSelect = new StringList(1);
        slObjectSelect.addElement(DomainConstants.SELECT_ID);
        slObjectSelect.addElement(DomainConstants.SELECT_CURRENT);

        StringList slRelSelect = new StringList(1);
        slRelSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

        MapList mlConnectedCA = domObjCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CHANGEACTION, TigerConstants.TYPE_CHANGEACTION, slObjectSelect, slRelSelect, false, true, (short) 1,
                null, null, 0);

        try {
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            MqlUtil.mqlCommand(context, "trigger off");

            if (!mlConnectedCA.isEmpty() && mlConnectedCA.size() > 0) {
                for (int i = 0; i < mlConnectedCA.size(); i++) {
                    Map mCAObj = (Map) mlConnectedCA.get(i);
                    strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    DomainObject domobjCA = new DomainObject(strCAID);
                    // Check whether the Related Change Action is in Prepare(Pending) state
                    domobjCA.setState(context, "Cancelled");

                }

            }
            // Promote Change Order to Cancelled state
            domObjCO.setState(context, "Cancelled");

        } finally {
            MqlUtil.mqlCommand(context, "trigger on");
            ContextUtil.popContext(context);
        }
        // Complete the Change Request

        strRelPattern = TigerConstants.RELATIONSHIP_CHANGEORDER;

        StringList sList = new StringList(2);
        sList.addElement(DomainConstants.SELECT_ID);
        sList.addElement(DomainConstants.SELECT_NAME);
        String strBusWhereclause = "attribute[PSS_CRBillable] == No";
        MapList mlConnectedCRList = (MapList) domObjCO.getRelatedObjects(context, strRelPattern, TigerConstants.TYPE_PSS_CHANGEREQUEST, sList, DomainObject.EMPTY_STRINGLIST, true, false, (short) 1,
                strBusWhereclause, null, 0);
        int nCRlistSize = mlConnectedCRList.size();

        if (nCRlistSize > 0) {

            for (int n = 0; n < nCRlistSize; n++) {
                Map mTemp = (Map) mlConnectedCRList.get(n);
                String sCRID = (String) mTemp.get(DomainObject.SELECT_ID);

                StringList slObjSelect = new StringList(2);
                slObjSelect.addElement(DomainConstants.SELECT_ID);
                slObjSelect.addElement(DomainConstants.SELECT_NAME);
                slObjSelect.addElement(DomainConstants.SELECT_CURRENT);

                DomainObject doCR = new DomainObject(sCRID);
                String strCRState = doCR.getInfo(context, DomainConstants.SELECT_CURRENT);
                // TIGTK-6161 :PCM|Rutuja Ekatpure:6/4/2017:Start
                String strWhere = "current != Implemented && current != Cancelled";
                // TIGTK-6161 :PCM|Rutuja Ekatpure:6/4/2017:End
                MapList mlCOConnectedList = doCR.getRelatedObjects(context, strRelPattern, TigerConstants.TYPE_PSS_CHANGEORDER, slObjSelect, DomainObject.EMPTY_STRINGLIST, false, true, (short) 1,
                        strWhere, null, 0);
                int nCOListsize = mlCOConnectedList.size();

                String sWhere = "(current!=Implemented && current!=Cancelled && current!=Rejected)";
                MapList mlMCOConnectedList = doCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, slObjSelect,
                        DomainObject.EMPTY_STRINGLIST, false, true, (short) 1, sWhere, null, 0);
                int nMCOListsize = mlMCOConnectedList.size();

                if ((nCOListsize == 0) && (nMCOListsize == 0)) {

                    // Promote Change Request to Complete state
                    if (TigerConstants.STATE_PSS_CR_INPROCESS.equalsIgnoreCase(strCRState)) {
                        // TIGTK-7626:Modified by SIE ON 9/05/2017:Start
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        doCR.promote(context);
                        ContextUtil.popContext(context);
                        // TIGTK-7626:Modified by SIE ON 9/05/2017:End
                    }
                }
            }
        }

    }

    // PCM TIGTK-3807 : 16/12/2016 : KWagh : End
    // Added by Sabari for triggers development-End
    /**
     * This method is called as "Range Function" on "PSS Purpose Of Release" field of MCO Creation.
     * @param context
     * @param args
     * @author -- Pooja Mantri
     * @return Map - Contains the attribute ranges
     * @throws Exception
     */
    public Map getDefaultPurposeOfReleaseonMCO(Context context, String args[]) throws Exception {
        HashMap returnMap = new HashMap();
        try {
            final String FIELD_CHOICES = "field_choices";
            final String FIELD_DISPLAY_CHOICES = "field_display_choices";

            HashMap programMap = JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            // initialize the Stringlist fieldRangeValues, fieldDisplayRangeValues
            StringList fieldRangeValues = new StringList();
            StringList fieldDisplayRangeValues = new StringList();

            String strChangeObjectId = (String) requestMap.get("parentOID");
            StringList attrRanges = FrameworkUtil.getRanges(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);
            attrRanges.sort();
            String strAttributeRange = "";
            if (UIUtil.isNotNullAndNotEmpty(strChangeObjectId)) {
                // Creating DomainObject Instance of "PSS_ChangeRequest" Object
                DomainObject domPSSChangeRequestObject = DomainObject.newInstance(context, strChangeObjectId);

                String strPSSCOPurposeOfRelease = domPSSChangeRequestObject.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
                // Get attribute ranges for attribute "PSS_Purpose_Of_Release"
                attrRanges.remove("");
                for (int i = 0; i < attrRanges.size(); i++) {
                    strAttributeRange = (String) attrRanges.get(i);
                    // TIGTK-2985 : condition changed:Rutuja Ekatpure :06/09/2016
                    if (strAttributeRange.equalsIgnoreCase(strPSSCOPurposeOfRelease)) {
                        fieldDisplayRangeValues.add(0, strAttributeRange);
                        fieldRangeValues.add(0, strAttributeRange);

                    } else {
                        fieldRangeValues.add(strAttributeRange);
                        fieldDisplayRangeValues.add(strAttributeRange);
                    }
                }

            } else {
                for (int i = 0; i < attrRanges.size(); i++) {
                    strAttributeRange = (String) attrRanges.get(i);
                    fieldRangeValues.add(strAttributeRange);
                    fieldDisplayRangeValues.add(strAttributeRange);
                }
            }
            returnMap.put(FIELD_CHOICES, fieldRangeValues);
            returnMap.put(FIELD_DISPLAY_CHOICES, fieldDisplayRangeValues);
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getDefaultPurposeOfReleaseonMCO: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return returnMap;
    }

    /**
     * Created By Swapnil Patil Description : Below method will return true if context person is member of ProgramProject connected to CR/CO this is added as access function on command to add affected
     * Items to CR/CO
     */
    public boolean CRCOAddAffectedAccess(Context context, String[] args) throws Exception {
        try {
            HashMap argsMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) argsMap.get("objectId");
            Map mSETTINGS = (Map) argsMap.get("SETTINGS");
            String strCommandFrom = (String) mSETTINGS.get("commandFrom");

            String RELATIONSHIP_PSS_CONNECTEDPCMDATA = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedPCMData");
            String RELATIONSHIP_PSS_CONNECTEDMEMBERS = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ConnectedMembers");

            boolean flag = false;
            DomainObject domObj = new DomainObject(objectId);

            String strState = domObj.getInfo(context, DomainObject.SELECT_CURRENT);
            String strType = domObj.getInfo(context, DomainObject.SELECT_TYPE);

            if (strType.equals(TigerConstants.TYPE_PSS_CHANGEREQUEST) && strCommandFrom.equals("CR")) {
                if (strState.equals(TigerConstants.STATE_PSS_CR_CREATE) || strState.equals(TigerConstants.STATE_SUBMIT_CR)) {
                    flag = true;
                }
            }
            if (strType.equals(TigerConstants.TYPE_PSS_CHANGEORDER) && strCommandFrom.equals("CO")) {
                if (strState.equals(TigerConstants.STATE_PSS_CHANGEORDER_PREPARE)) {
                    flag = true;
                }
            }

            if (flag) {
                StringList slProjectList = domObj.getInfoList(context, "to[" + RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");
                String strContextUser = context.getUser();

                if ((domObj.getInfo(context, DomainObject.SELECT_OWNER)).equals(context.getUser())) {
                    flag = true;
                } else if (slProjectList.contains(strContextUser)) {
                    flag = true;
                } else {
                    flag = false;
                }
            }
            return flag;
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in CRCOAddAffectedAccess: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * Connects Sketch object from "PSS_ChangeOrder"
     * @author -- Pooja Mantri -- TIGTK-2765
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds ParamMap
     * @throws Exception
     *             if the operation fails
     **/
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public void connectSupportingDocToCO(Context context, String[] args) throws Exception {
        try {

            HashMap progMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) progMap.get("paramMap");
            HashMap requestMap = (HashMap) progMap.get("requestMap");

            String sketchId = (String) paramMap.get("objectId");
            String crId = (String) requestMap.get("parentOID");

            DomainObject sketchDomObj = DomainObject.newInstance(context, sketchId);

            HashMap attributeMap = new HashMap(1);
            attributeMap.put(DomainConstants.ATTRIBUTE_TITLE, sketchDomObj.getName(context));

            ContextUtil.pushContext(context);

            sketchDomObj.setAttributeValues(context, attributeMap);

            if (UIUtil.isNotNullAndNotEmpty(crId)) {
                DomainRelationship.connect(context, new DomainObject(crId), TigerConstants.RELATIONSHIP_PSS_SUPPORTINGDOCUMENT, sketchDomObj);
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            ContextUtil.popContext(context);
        }
    }

    // PCM RFC-076 : 02/09/16 : Swapnil Patil : END
    /**
     * @description : This method is use to promote Change Request to complete state
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void promoteCRToComplete(Context context, String[] args) throws Exception {
        HashMap param = (HashMap) JPO.unpackArgs(args);
        // Get the ObjectId of CR
        Map requestMap = (Map) param.get("requestMap");
        String objectId = (String) requestMap.get("objectId");
        BusinessObject busObj = new BusinessObject(objectId);

        // Promote PSS_Change Request to complete state
        boolean flag = false;
        try {
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            flag = true;
            busObj.promote(context);
            // domObjCR.promote(context);
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteCRToComplete: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        } finally {
            if (flag == true)
                ContextUtil.popContext(context);
        }
    }

    // PCM RFC-076 : 02/09/16 : Swapnil Patil : END

    // PCM : Trigger on MBOM to check MCA connected before promote MBOM : 07/09/16 : Rutuja Ekatpure : Start
    /***
     * method used to check state of MBOM's connected MCA ,If MCA is in In work or above then promote MBOM :Rutuja Ekatpure (1/9/2016)
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkRelatedMCAState(Context context, String args[]) throws Exception {

        String strMessage;
        String strCAId;
        String strCurrentSate;
        if (args == null) {
            throw new IllegalArgumentException();
        }
        int isreturn = 0;
        try {
            String strPartId = args[0];
            DomainObject domPartObj = new DomainObject(strPartId);

            StringList slObjectSle = new StringList(3);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

            // get related MCA
            MapList mConnectList = domPartObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM, // relationship pattern
                    TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION, // Type pattern
                    slObjectSle, // object select
                    null, // relationship select
                    true, // to direction
                    false, // from direction
                    (short) 1, // recurstion level
                    null, // object where
                    null, // relationship where
                    0); // limit
            if (mConnectList.isEmpty()) {
                // do nothing
                strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.NoChangeItemsConnected");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                isreturn = 1;

            } else {
                if (mConnectList.size() > 0) {
                    Map mCAObj = (Map) mConnectList.get(0);
                    strCAId = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    strCurrentSate = (String) mCAObj.get(DomainConstants.SELECT_CURRENT);
                    DomainObject domCAObj = new DomainObject(strCAId);

                    StringList strListCAStates = domCAObj.getInfoList(context, SELECT_STATES);

                    int indexApprovedState = strListCAStates.indexOf("In Work");
                    int indexOfCurrentSate = strListCAStates.indexOf(strCurrentSate);
                    // check state of connected MCA is Inwork or below
                    if (indexOfCurrentSate < indexApprovedState) {

                        strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ErrorWhilePromoteInReview");
                        MqlUtil.mqlCommand(context, "notice $1", strMessage);
                        isreturn = 1;
                    }

                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkRelatedMCAState: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return isreturn;
    }

    // PCM : 07/09/16 : Rutuja Ekatpure :End

    // PCM TIGTK-2758 : 13/09/16 : Pooja Mantri :Start
    /**
     * Method used to check access for adding existing CO to CR based on current state of CR and "Parallel Track" value
     * @param context
     * @param args
     * @return -- boolean -- Returnflag which states true or false
     * @author -- Pooja Mantri
     * @throws Exception
     */
    public boolean checkAccessForAddExistingCOs(Context context, String[] args) throws Exception {
        boolean retStatus = false;
        try {
            final String ATTRIBUTE_PSSPARALLELTRACK_YES = "Yes";
            final String ATTRIBUTE_PSSPARALLELTRACK_NO = "No";

            HashMap programMap = JPO.unpackArgs(args);
            // Get "CR Object" Id
            String sCRObjectId = (String) programMap.get("objectId");
            // Create Domain Object instance
            DomainObject domCRObject = DomainObject.newInstance(context, sCRObjectId);

            // Defining Selectlist
            StringList slSelectList = new StringList();
            slSelectList.add(DomainConstants.SELECT_CURRENT);
            slSelectList.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");

            // Get Current state and "Parallel Track" value for CR
            Map mCRDetailsMap = domCRObject.getInfo(context, slSelectList);
            String sCurrent = (String) mCRDetailsMap.get(DomainConstants.SELECT_CURRENT);
            String sParallelTrackValue = (String) mCRDetailsMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");

            if (sCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_CR_INPROCESS) && sParallelTrackValue.equalsIgnoreCase(ATTRIBUTE_PSSPARALLELTRACK_NO)) {
                retStatus = true;
            } else if (sCurrent.equalsIgnoreCase(TigerConstants.STATE_SUBMIT_CR) && sParallelTrackValue.equalsIgnoreCase(ATTRIBUTE_PSSPARALLELTRACK_YES)) {
                // PCM TIGTK-3633 | 19/11/16 :Pooja Mantri : Start
                retStatus = false;
                // PCM TIGTK-3633 | 19/11/16 :Pooja Mantri : End
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkRelatedMCAState: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return retStatus;
    }

    // PCM TIGTK-2758 : 13/09/16 : Pooja Mantri :End

    /***
     * Promotes CO from Complete to Implemented and send notification if all CO and MCO of related CR's are implemented or cancelled. PCM : TIGTK-7889 : 01/06/2017 : AB
     * @param context
     * @param args
     * @return nothing
     * @throws Exception
     */

    public void promoteCOToImplementedState(Context context, String[] args) throws Exception {
        try {
            // Get the Object Id of Change Order
            HashMap param = (HashMap) JPO.unpackArgs(args);
            Map requestMap = (Map) param.get("requestMap");
            String objectId = (String) requestMap.get("objectId");

            // Get Connected Change Request of Change order
            StringList slObjectSelects = new StringList();
            slObjectSelects.add(DomainConstants.SELECT_ID);
            slObjectSelects.add(DomainConstants.SELECT_CURRENT);
            slObjectSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEORDER);
            typePattern.addPattern(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER);

            Pattern relPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_ORDER);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER);

            DomainObject domChangeOrder = DomainObject.newInstance(context, objectId);

            try {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                domChangeOrder.promote(context);
            } finally {
                ContextUtil.popContext(context);
            }

            MapList listConnectedChangeRequest = domChangeOrder.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSelects, null,
                    true, false, (short) 0, null, null, 0);

            // get Connected all CO and MCO of Change request and check that all objects are Implememted or not.
            if (!listConnectedChangeRequest.isEmpty()) {
                for (int i = 0; i < listConnectedChangeRequest.size(); i++) {
                    boolean bolAllCOImplemented = true;
                    Map mapConnectedChangeRequest = (Map) listConnectedChangeRequest.get(i);
                    String strChangeRequestId = (String) mapConnectedChangeRequest.get(DomainConstants.SELECT_ID);
                    String strChangeRequestBillableValue = (String) mapConnectedChangeRequest.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");
                    DomainObject domChangeRequest = new DomainObject(strChangeRequestId);

                    MapList listConnectedCOAndMCO = domChangeRequest.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), slObjectSelects, null, false, true, (short) 1, null,
                            null, 0);

                    if (!listConnectedCOAndMCO.isEmpty()) {
                        for (int k = 0; k < listConnectedCOAndMCO.size(); k++) {
                            Map mapCOObj = (Map) listConnectedCOAndMCO.get(k);
                            String strCOCurrentState = (String) mapCOObj.get(DomainConstants.SELECT_CURRENT);
                            if (!(strCOCurrentState.equalsIgnoreCase(TigerConstants.STATE_PSS_CHANGEORDER_CANCELLED)
                                    || strCOCurrentState.equalsIgnoreCase(TigerConstants.STATE_CHANGEORDER_IMPLEMENTED))) {
                                bolAllCOImplemented = false;
                            }
                        }
                    }

                    String strContextUser = context.getUser();
                    Map payload = new HashMap();
                    payload.put("fromList", strContextUser);
                    // if All CO and MCO of related CR's are Implemented and CR is billable then send notification to Change Manager
                    if (bolAllCOImplemented && "Yes".equalsIgnoreCase(strChangeRequestBillableValue)) {
                        emxNotificationUtil_mxJPO.objectNotification(context, objectId, "PSS_COImplementNotification", payload);
                    } else if (bolAllCOImplemented && "No".equalsIgnoreCase(strChangeRequestBillableValue)) {
                        emxNotificationUtil_mxJPO.objectNotification(context, objectId, "PSS_COImplementNotificationNonBillable", payload);
                    }

                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in promoteCOToImplementedState: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    /**
     * @Description: This method gets the Affected items that are not transferred to CO on creation of new CO from the CR.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getAffectedItemsNotConnectedToCA(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList mlFinalAffectedItemList = new MapList();

        try {
            String strCOId = (String) programMap.get("objectId");
            DomainObject domCO = new DomainObject(strCOId);
            String strRelatedCRId = domCO.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ORDER + "].from.id");

            DomainObject domCR = new DomainObject(strRelatedCRId);
            StringList slAIConnectedToCR = domCR.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");
            StringList slAIConnectedToCO = domCO.getInfoList(context,
                    "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].to.from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id");

            for (int k = 0; k < slAIConnectedToCO.size(); k++) {
                String strId = (String) slAIConnectedToCO.get(k);
                if (slAIConnectedToCR.contains(strId))
                    slAIConnectedToCR.remove(strId);
            }
            for (int j = 0; j < slAIConnectedToCR.size(); j++) {
                // TIGTK-6870 - Hiren - Start
                String strId = (String) slAIConnectedToCR.get(j);
                DomainObject domObj = new DomainObject(strId);
                String strPolicy = domObj.getInfo(context, DomainConstants.SELECT_POLICY).toString();
                if (!strPolicy.equals(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {
                    Map tempMap = new HashMap();
                    tempMap.put(DomainObject.SELECT_ID, slAIConnectedToCR.get(j).toString());
                    mlFinalAffectedItemList.add(tempMap);
                }
                // TIGTK-6870 - Hiren - End
            }

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getAffectedItemsNotConnectedToCA: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return mlFinalAffectedItemList;
    }

    // PCM TIGTK-3584 : 16/11/2016 : KWagh : Start

    /**
     * @Description: This method gets the Affected items that are not transferred to CO on creation of new CO from the CR.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */

    @com.matrixone.apps.framework.ui.ProgramCallable
    public boolean getSizeOfAffectedItemsListNotConnectedToCA(Context context, String[] args) throws Exception {

        boolean bResult = false;
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList mlFinalAffectedItemList = new MapList();
        StringList slPartType = new StringList();
        StringList slCADType = new StringList();
        StringList slStandardPartList = new StringList();
        StringList slDevelopmentPart = new StringList();

        try {
            String strRelatedCRId = (String) programMap.get("objectId");
            DomainObject domObjCR = new DomainObject(strRelatedCRId);

            String strCRconnectedProgramid = domObjCR.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            StringList objectSelects = new StringList(1);
            objectSelects.addElement(DomainConstants.SELECT_ID);
            objectSelects.addElement(DomainConstants.SELECT_POLICY);
            objectSelects.addElement(DomainConstants.SELECT_TYPE);
            objectSelects.add("to[" + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
            objectSelects.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]");
            objectSelects.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current");
            objectSelects.add(DomainConstants.SELECT_CURRENT);

            StringList relSelects = new StringList(1);
            relSelects.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null);
            String where = "policy != " + TigerConstants.POLICY_PSS_MBOM + " && (current==" + TigerConstants.STATE_RELEASED_CAD_OBJECT + " || current==" + TigerConstants.STATE_PART_RELEASE
                    + " || current == '" + TigerConstants.STATE_CHANGEACTION_INWORK + "' || current == " + TigerConstants.STATE_PSS_ECPART_PRELIMINARY + " || current=="
                    + TigerConstants.STATE_PART_REVIEW + " || current == '" + TigerConstants.STATE_DEVELOPMENTPART_PEERREVIEW + "' || current == " + TigerConstants.STATE_PSS_CR_CREATE + ")";

            StringList slReleaseItem = new StringList();
            slReleaseItem.add("Released");
            slReleaseItem.add("Release");

            MapList mlCRAffectedItemsOriginal = domObjCR.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, // relationship pattern
                    DomainConstants.QUERY_WILDCARD, // object pattern
                    objectSelects, // object selects
                    relSelects, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    null, // object where clause
                    null, // relationship where clause
                    (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    null, null, null, null);

            MapList mlCRAffectedItems = domObjCR.getRelatedObjects(context, // context
                    TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, // relationship pattern
                    DomainConstants.QUERY_WILDCARD, // object pattern
                    objectSelects, // object selects
                    relSelects, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    where, // object where clause
                    null, // relationship where clause
                    (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 0, // pageSize
                    null, null, null, null);

            if (!mlCRAffectedItems.isEmpty() && mlCRAffectedItems.size() > 0) {
                // TIGTK-8229 :Modiofied by SIE :Start
                StringList slActiveCAState = new StringList();
                slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_PENDING);
                slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_INWORK);
                slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_INAPPROVAL);
                // TIGTK-8229 :Modiofied by SIE :End
                for (int k = 0; k < mlCRAffectedItems.size(); k++) {
                    Map mTemp = (Map) mlCRAffectedItems.get(k);
                    StringList slSpecsConnectedPartPolicy = changeUtil.getStringListFromMap(context, mTemp, "to[" + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");

                    String strPolicy = (String) mTemp.get(DomainObject.SELECT_POLICY);
                    String strPartState = (String) mTemp.get(DomainObject.SELECT_CURRENT);
                    String strAIId = (String) mTemp.get(DomainObject.SELECT_ID);

                    // TIGTK-8229 :Modiofied by SIE :Start
                    StringList slConnectedCACurrent = changeUtil.getStringListFromMap(context, mTemp, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current");
                    boolean flag = isAffectedItemHasNoActiveCAConnected(slActiveCAState, slConnectedCACurrent);
                    DomainObject domAI = DomainObject.newInstance(context, strAIId);
                    String strGoveringPrjId = domAI.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                    if (strGoveringPrjId == null) {
                        strGoveringPrjId = strCRconnectedProgramid;
                    }
                    if ((TigerConstants.POLICY_PSS_ECPART.equals(strPolicy)) && (flag || slReleaseItem.contains(strPartState)) && (strGoveringPrjId.equalsIgnoreCase(strCRconnectedProgramid))) {

                        slPartType.add(strAIId);

                    } else if (((TigerConstants.POLICY_PSS_CADOBJECT.equals(strPolicy)) || (TigerConstants.POLICY_PSS_Legacy_CAD.equals(strPolicy))) && (flag || slReleaseItem.contains(strPartState))
                            && !slSpecsConnectedPartPolicy.contains(TigerConstants.POLICY_PSS_DEVELOPMENTPART) && (strGoveringPrjId.equalsIgnoreCase(strCRconnectedProgramid))) {
                        slCADType.add(strAIId);

                    } else if ((TigerConstants.POLICY_STANDARDPART.equals(strPolicy)) && (flag || slReleaseItem.contains(strPartState))
                            && (strGoveringPrjId.equalsIgnoreCase(strCRconnectedProgramid))) {
                        slStandardPartList.add(strAIId);

                    } // TIGTK-8229 :Modiofied by SIE :End
                    else if (TigerConstants.POLICY_PSS_DEVELOPMENTPART.equals(strPolicy)) {
                        slDevelopmentPart.add(strAIId);
                    }
                }

            }

            mlFinalAffectedItemList.addAll(slPartType);
            mlFinalAffectedItemList.addAll(slCADType);
            mlFinalAffectedItemList.addAll(slStandardPartList);
            mlFinalAffectedItemList.addAll(slDevelopmentPart);

            int nOriginalcnt = mlCRAffectedItemsOriginal.size();
            int nFinalcnt = mlFinalAffectedItemList.size();
            if (nOriginalcnt > nFinalcnt) {
                bResult = true;
            }

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getSizeOfAffectedItemsListNotConnectedToCA: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

        return bResult;
    }
    // PCM TIGTK-3584 : 16/11/2016 : KWagh : End

    // TIGTK-3318 :While promoting affected item to 'Obsolescence':8/10/2016:start
    /**
     * @Description: This method used to promote CA from Prepare to In work and In work to Review in case 1.Affected item in released state and requested change is for obsolence 2.Affected item in
     *               review state
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public void promoteCOActionToReviewState(Context context, MapList mlMCAandAffectedItems) throws Exception {

        for (int k = 0; k < mlMCAandAffectedItems.size(); k++) {
            Map mCAAndAffectedItem = (Map) mlMCAandAffectedItems.get(k);
            // PCM TIGTK-3951: 15/02/2017 : KWagh : START : Performance Issue
            // get keys
            Set<String> keys = new HashSet<String>();
            Iterator<Map.Entry> itr = mCAAndAffectedItem.entrySet().iterator();

            while (itr.hasNext()) {
                Entry e = itr.next();
                String strKey = (String) e.getKey();
                keys.add(strKey);

            }
            // PCM TIGTK-3951: 15/02/2017 : KWagh : End : Performance Issue
            for (String strCAID : keys) {

                MapList mlConnectedAffectedItem = (MapList) mCAAndAffectedItem.get(strCAID);
                DomainObject domobjCA = new DomainObject(strCAID);

                int isToPromote = 0;
                for (int j = 0; j < mlConnectedAffectedItem.size(); j++) {
                    Map mAffectedItemObj = (Map) mlConnectedAffectedItem.get(j);
                    String strAffectedItemState = (String) mAffectedItemObj.get(DomainConstants.SELECT_CURRENT);
                    String strRequestedChangeAttrValue = (String) mAffectedItemObj.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

                    if ((strAffectedItemState.equalsIgnoreCase(TigerConstants.STATE_PART_REVIEW))
                            || ((strAffectedItemState.equalsIgnoreCase(TigerConstants.STATE_PART_RELEASE) || strAffectedItemState.equalsIgnoreCase(TigerConstants.STATE_RELEASED_CAD_OBJECT))
                                    && strRequestedChangeAttrValue.equalsIgnoreCase(ChangeConstants.FOR_OBSOLESCENCE))) {
                        isToPromote++;
                    }
                }
                if (isToPromote == mlConnectedAffectedItem.size()) {
                    // promote CA to In Work state
                    domobjCA.setState(context, "In Work");
                    // promote CA to In Review State
                    String contextUser = context.getUser();
                    if (contextUser != null && !"".equals(contextUser)) {
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        context.setCustomData("contextUser", contextUser);
                    }
                    domobjCA.setState(context, "In Approval");
                    context.setCustomData("contextUser", "");
                    ContextUtil.popContext(context);

                }
            }
        }
    }

    // PCM : Trigger on Part to check CA connected before promote part from in work to in revew : 24/10/16 : Rutuja Ekatpure : Start
    /***
     * method used to check state of part's connected CA ,If CA is in In work or above then promote part :Rutuja Ekatpure (24/10/2016)
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkRelatedCAState(Context context, String args[]) throws Exception {
        // PCM TIGTK-5118: 06/04/2017 : KWagh : START
        String strMessage = DomainConstants.EMPTY_STRING;
        String strCAId = DomainConstants.EMPTY_STRING;
        String strCurrentSate = DomainConstants.EMPTY_STRING;
        int isreturn = 0;

        try {
            String strAffectedItemId = args[0];
            DomainObject domAIObj = new DomainObject(strAffectedItemId);

            String StrType = domAIObj.getInfo(context, DomainConstants.SELECT_TYPE);

            Pattern relPattern = new Pattern(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
            relPattern.addPattern(ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM);

            StringList slObjectSle = new StringList(3);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

            // get related Change Action
            MapList mConnectList = domAIObj.getRelatedObjects(context, relPattern.getPattern(), // relationship pattern
                    ChangeConstants.TYPE_CHANGE_ACTION, // type pattern
                    slObjectSle, // object select
                    null, // relationship select
                    true, // to direction
                    false, // from direction
                    (short) 1, // recursion level
                    null, // object where
                    null, // relationship where
                    0); // limit
            if (mConnectList.isEmpty()) {
                // TIGTK-3585:condition added for cad objects manual promotion blocking issue:Rutuja Ekatpure:start
                if (TigerConstants.TYPE_PART.equalsIgnoreCase(StrType)) {
                    strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.NoChangeItemsConnectedToPart");
                    MqlUtil.mqlCommand(context, "notice $1", strMessage);
                    isreturn = 1;
                }
                // PCM TIGTK-5118: 06/04/2017 : KWagh : End
                // TIGTK-3585:condition added for cad objects manual promotion blocking issue:Rutuja Ekatpure:End
            } else {

                Map mCAObj = (Map) mConnectList.get(0);
                strCAId = (String) mCAObj.get(DomainConstants.SELECT_ID);
                strCurrentSate = (String) mCAObj.get(DomainConstants.SELECT_CURRENT);
                DomainObject domCAObj = new DomainObject(strCAId);

                StringList strListCAStates = domCAObj.getInfoList(context, SELECT_STATES);

                int indexApprovedState = strListCAStates.indexOf(TigerConstants.STATE_CHANGEACTION_INWORK);
                int indexOfCurrentSate = strListCAStates.indexOf(strCurrentSate);
                // check state of connected CA is Inwork or below
                if (indexOfCurrentSate < indexApprovedState) {

                    strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ErrorWhilePromotePartInReview");
                    String strCAName = (String) domCAObj.getInfo(context, DomainConstants.SELECT_NAME);
                    strMessage = strMessage.replace("$<name>", strCAName);
                    MqlUtil.mqlCommand(context, "notice $1", strMessage);
                    isreturn = 1;
                }

            }
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkRelatedCAState: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return isreturn;
    }

    // PCM : 24/10/16 : Rutuja Ekatpure :End

    /**
     * This method is used to String for the Required selectable from the Maplist
     * @param mlInputList
     * @param selectable
     * @return
     * @throws Exception
     */
    // PCM TIGTK-3593 | 24/11/16 : Gautami : Start
    public StringList getStringListFromMaplist(MapList mlInputList, String selectable) throws Exception {
        StringList slReturnList = new StringList();
        if (!mlInputList.isEmpty() && mlInputList.size() > 0) {
            for (int i = 0; i < mlInputList.size(); i++) {
                Map tempMap = (Map) mlInputList.get(i);
                String strValue = (String) tempMap.get(selectable);
                slReturnList.add(strValue);
            }
        }
        return slReturnList;
    }

    // PCM TIGTK-3593 | 24/11/16 : Gautami : End

    /**
     * This method is used to connect Part and Symmetrical's latest revisions Date: 02/01/2017 : TIGTK-3654
     * @author abhalani
     * @param context
     * @param args
     *            Part's old Id and Part's new Revision id
     * @return
     * @throws Exception
     */

    public void connectLatestRevisionsOfPartAndSymmetrical(Context context, String strOldID, String strNewID) throws Exception {
        try {
            HashMap mapAttributes = new HashMap();
            String strPartID = strOldID; // Part ID
            String strRevisedId = strNewID; // New revised part ID
            DomainObject domPart = new DomainObject(strPartID);
            DomainObject domRevisedPart = new DomainObject(strRevisedId);

            StringList relSelects = new StringList();
            relSelects.add(DomainRelationship.SELECT_ID);
            relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS + "]");
            relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSMANAGEINPAIRS + "]");

            StringList selectStmts = new StringList();
            selectStmts.add(DomainConstants.SELECT_ID);

            // Get Realated symmetrical part of the part
            MapList mlSymmetricalPartObject = domPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, // relationship pattern
                    DomainConstants.TYPE_PART, // object pattern
                    selectStmts, // object selects
                    relSelects, // relationship selects
                    true, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, 0); // relationship where clause

            if (mlSymmetricalPartObject.size() != 0) {
                for (int j = 0; j < mlSymmetricalPartObject.size(); j++) {
                    Map<String, String> map = (Map<String, String>) mlSymmetricalPartObject.get(j);
                    mapAttributes.put(TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS, map.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSIDENTICALMASS + "]"));
                    mapAttributes.put(TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSMANAGEINPAIRS, map.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_SYMMETRICALPARTSMANAGEINPAIRS + "]"));
                    String strSymmetricpartId = map.get(DomainObject.SELECT_ID);
                    DomainObject domSymmetricalPart = new DomainObject(strSymmetricpartId);
                    boolean isLastRev = domSymmetricalPart.isLastRevision(context);

                    // If Symmetrical Part is not latest revision then connect latest revision with the each other
                    if (!isLastRev) {
                        BusinessObject boLatestRevisionOfSymmetrical = (BusinessObject) domSymmetricalPart.getLastRevision(context);
                        DomainObject domLatestRevisionOfSymmetrical = new DomainObject(boLatestRevisionOfSymmetrical);

                        // Check Part is Original or Symmetrical part for new Conenction
                        String strSymmetricalPart = domPart.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART + "].from.id");
                        // Findbug Issue correction start
                        // Date: 22/03/2017
                        // By: Asha G.
                        DomainRelationship domRel = null;
                        if (UIUtil.isNotNullAndNotEmpty(strSymmetricalPart)) {
                            domRel = DomainRelationship.connect(context, domLatestRevisionOfSymmetrical, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, domRevisedPart);
                        } else {
                            domRel = DomainRelationship.connect(context, domRevisedPart, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, domLatestRevisionOfSymmetrical);

                        }

                        // Set attribute of old Relationship which is exist between between Part it's and Symmetrical part
                        if (domRel != null)
                            domRel.setAttributeValues(context, mapAttributes);
                        // Findbug Issue correction End
                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectLatestRevisionsOfPartAndSymmetrical: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }

    }

    // PCM TIGTK-3862: 5/1/2017 : KWagh : START
    /**
     * This method is called from trigger. Method is user promote connected Change Order object to Implemented state if Last MCO is promoted to Implemented state
     * @param context
     * @param args
     * @throws Exception
     */
    public void promoteConnectedChangeOrderToImplementedState(Context context, String[] args) throws Exception {
        boolean bPushedContext = false;
        try {
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, null, null);
            bPushedContext = true;

            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }
            String strMCOID = args[0];
            String sMCOID;
            String sMCOCurrent;

            StringList slAllowedMCOState = new StringList();
            slAllowedMCOState.add(TigerConstants.STATE_PSS_MCO_IMPLEMENTED);
            slAllowedMCOState.add(TigerConstants.STATE_PSS_MCO_REJECTED);
            slAllowedMCOState.add(TigerConstants.STATE_PSS_MCO_CANCELLED);

            // boolean
            boolean allowPromotion = true;

            DomainObject domMCO = new DomainObject(strMCOID);

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            MapList mlchangeOrders = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, TigerConstants.TYPE_PSS_CHANGEORDER, slObjectSle, slRelSle, true,
                    false, (short) 1, null, null, 0);

            if (!mlchangeOrders.isEmpty()) {
                int mlCount = mlchangeOrders.size();

                for (int i = 0; i < mlCount; i++) {

                    Map mChangeOrder = (Map) mlchangeOrders.get(i);
                    String strCOID = (String) mChangeOrder.get(DomainConstants.SELECT_ID);
                    String strCOCurrent = (String) mChangeOrder.get(DomainConstants.SELECT_CURRENT);

                    DomainObject domCO = new DomainObject(strCOID);
                    if (TigerConstants.STATE_PSS_CHANGEORDER_COMPLETE.equals(strCOCurrent)) {
                        MapList mlMCOS = domCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER, slObjectSle,
                                slRelSle, false, true, (short) 1, null, null, 0);

                        if (!mlMCOS.isEmpty()) {
                            int nCnt = mlMCOS.size();
                            if (nCnt == 1) {

                                // Promote Change Order to Implemented state
                                domCO.setState(context, TigerConstants.STATE_CHANGEORDER_IMPLEMENTED);
                            }

                            else {
                                for (int n = 0; n < nCnt; n++) {

                                    Map mMCO = (Map) mlMCOS.get(n);
                                    sMCOID = (String) mMCO.get(DomainConstants.SELECT_ID);
                                    sMCOCurrent = (String) mMCO.get(DomainConstants.SELECT_CURRENT);

                                    if (!(sMCOID.equalsIgnoreCase(strMCOID))) {

                                        if (!slAllowedMCOState.contains(sMCOCurrent)) {

                                            allowPromotion = false;
                                            break;
                                        }

                                    }
                                }

                                if (allowPromotion) {
                                    // Promote Change Order to Implemented state
                                    domCO.setState(context, TigerConstants.STATE_CHANGEORDER_IMPLEMENTED);
                                }

                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in promoteConnectedChangeOrderToImplementedState()\n", e);
            throw e;
        } finally {
            if (bPushedContext) {
                ContextUtil.popContext(context);
            }
        }
    }

    // PCM TIGTK-3862: 5/1/2017 : KWagh : END

    /**
     * This method is used to check whether affcted item which is selected is already connected to any CO which is active Date : 10/01/2017 : TIGTK-3907
     * @author abhalani
     * @param context
     * @param args
     *            - affected item which is selected for create CO from EBOM
     * @return- error message if any item is connected to active CO
     * @throws Exception
     */

    public String checkAffectedItemConnectedToActiveCO(Context context, String args[]) throws Exception {
        // TODO Auto-generated method stub
        String errorMessage = "";
        try {
            // get the list of affected item which is selected for create CO from EBOM
            Map paramMap = JPO.unpackArgs(args);
            StringList slAffectedItems = (StringList) paramMap.get("selectedItemsList");
            String itemConnectedToChangeAlready = EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(),
                    "EnterpriseChangeMgt.Warning.ContextItemAlreadyConnectedWarning");

            StringList slActiveCOState = new StringList();
            slActiveCOState.add(TigerConstants.STATE_PSS_CHANGEORDER_PREPARE);
            slActiveCOState.add(TigerConstants.STATE_PSS_CHANGEORDER_INWORK);
            slActiveCOState.add(TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL);
            StringBuffer sbErrorMsg = new StringBuffer();
            // get the connected ChangeOrder of affected item
            for (int i = 0; i < slAffectedItems.size(); i++) {
                String strAffectedItemId = (String) slAffectedItems.get(i);
                DomainObject domSelectedItem = DomainObject.newInstance(context, strAffectedItemId);
                StringList slRelatedCOCurrent = domSelectedItem.getInfoList(context,
                        "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.current");

                if (!slRelatedCOCurrent.isEmpty()) {
                    for (int j = 0; j < slRelatedCOCurrent.size(); j++) {
                        String strCOCurrent = (String) slRelatedCOCurrent.get(j);

                        // check connected CO is active or not, if it is active then create warning message for that affected item
                        if (slActiveCOState.contains(strCOCurrent)) {
                            String strAffectedItemName = (String) domSelectedItem.getInfo(context, DomainConstants.SELECT_NAME);
                            // TIGTK-3961:Performance issue for string concat:Rutuja Ekatpure:23/1/2017:Start
                            sbErrorMsg.append(" ");
                            sbErrorMsg.append(strAffectedItemName);
                            sbErrorMsg.append(" ");
                            sbErrorMsg.append(itemConnectedToChangeAlready);
                        }
                    }
                }
            }
            errorMessage = sbErrorMsg.toString();
            // TIGTK-3961:Performance issue for string concat:Rutuja Ekatpure:23/1/2017:End
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkAffectedItemConnectedToActiveCO: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }

        return errorMessage;
    }

    /**
     * For getting unique stringList from input list
     * @param slInputList
     * @return
     * @throws Exception
     */
    public StringList getUniqueIdList(StringList slInputList) throws Exception {
        StringList slReturnList = new StringList();
        try {

            for (int i = 0; i < slInputList.size(); i++) {

                String objectId = (String) slInputList.get(i);
                if (!slReturnList.contains(objectId)) {
                    slReturnList.add(objectId);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getUniqueIdList: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return slReturnList;
    }

    // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
    /**
     * This method is called from PSS_ECMFullSearchPostProcess.jsp. It makes a check whether Affected Items are connected to Active CO or not based on Affected Item Object(CAD, Part, Standard). It
     * returns the final list of affected Items to be connected with CO Object.
     * @param context
     * @param selectedItemsList
     * @return
     * @throws Exception
     */
    public StringList processCOAffectedItems(Context context, String args[]) throws Exception {
        StringList slApprovedItemsList = new StringList();
        try {
            boolean bolCADConnectedToOnlyDevPart = false;
            Map programMap = (HashMap) JPO.unpackArgs(args);
            StringList selectedItemsList = (StringList) programMap.get("selectedItemsList");
            DomainObject domAffectedItem = new DomainObject();
            int slSize = selectedItemsList.size();
            String strPolicy = "";
            String strObjId = "";
            StringList slselectStmt = new StringList();
            slselectStmt.addElement(DomainConstants.SELECT_POLICY);
            slselectStmt.addElement("to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
            slselectStmt.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].from.policy");
            boolean bFlag = false;

            for (int i = 0; i < slSize; i++) {
                strObjId = (String) selectedItemsList.get(i);

                domAffectedItem.setId(strObjId);

                Map mapAffectedItemDetails = domAffectedItem.getInfo(context, slselectStmt);
                strPolicy = (String) mapAffectedItemDetails.get(DomainConstants.SELECT_POLICY);
                // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                /*
                 * PCM : TIGTK-5278 : 31/05/2017 : AB : START PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null); StringList slSpecsConnectedPartPolicy =
                 * changeUtil.getStringListFromMap(context, mapAffectedItemDetails, "to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy"); // PCM : TIGTK-7126 : 02/05/2017 : AB :
                 * START StringList slChartedDrawingConnectedPartPolicy = changeUtil.getStringListFromMap(context, mapAffectedItemDetails, "to[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING +
                 * "].from.policy"); slSpecsConnectedPartPolicy.add(slChartedDrawingConnectedPartPolicy);
                 */// PCM : TIGTK-5278 : 31/05/2017 : AB : END

                // PCM : TIGTK-7126 : 02/05/2017 : AB : END
                // PCM : TIGTK-4119 : 02/02/2017 : AB : START
                // PCM TIGTK-4273 | 06/02/2017 : AB : START
                String strRelPattern = ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM;

                String selectCoIsActive = new StringBuilder("evaluate[(to[").append(strRelPattern).append("].from.to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION)
                        .append("].from.current smatchlist \"Prepare,In Work,In Approval\" \",\")]").toString();

                boolean isConnectedActiveCO = Boolean.valueOf((String) domAffectedItem.getInfo(context, selectCoIsActive));
                // PCM TIGTK-4273 | 06/02/2017 : AB : START
                // PCM : TIGTK-4119 : 02/02/2017 : AB : END

                if (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART) && !isConnectedActiveCO) {
                    // In case of Standard part , get the CAD+Charted Drawing connected to it which is not connected to dev part and another active CO
                    slApprovedItemsList.addAll(getCADAndDrawingObjectsList(context, strObjId));
                    // TIGTK-7942:Rutuja Ekatpure:24/5/2017:Start
                    slApprovedItemsList.add(strObjId);
                    // TIGTK-7942:Rutuja Ekatpure:24/5/2017:End
                }

                else if (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_ECPART) && !isConnectedActiveCO) {
                    // In case of EC part; Inner Case 1, get the CAD+Charted Drawing connected to it which is not connected to dev part and another active CO
                    slApprovedItemsList.addAll(getCADAndDrawingObjectsList(context, strObjId));
                    // In case of EC part; Inner Case 2, find the symmetrical part connected to it CAD+ Charted Drawing connected to symmetricla part
                    // and which is not connected to dev part and another active CO
                    // getting the Symmetrical parts :
                    StringList slSymmetricParts = getSymmetricalPart(context, new String[] { strObjId });
                    StringList slFinalSymmetricalist = new StringList();
                    slFinalSymmetricalist.addAll(slSymmetricParts);
                    // Find Bug modifications: 23/03/2017 : KWagh : START
                    if (!slSymmetricParts.isEmpty()) {
                        // Find Bug modifications: 23/03/2017 : KWagh : End
                        if (slSymmetricParts.contains(strObjId)) {
                            slFinalSymmetricalist.remove(strObjId);
                        }
                        if (slFinalSymmetricalist.size() > 0) {
                            String symmPartId = (String) slFinalSymmetricalist.get(0);
                            StringList symmConnectedObjList = getCADAndDrawingObjectsList(context, symmPartId);

                            slApprovedItemsList.addAll(symmConnectedObjList);
                        }
                        slApprovedItemsList.addAll(slSymmetricParts);

                    }
                    // PCM : TIGTK-9060 : 28/07/2017 : AB : START
                } else if ((strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT) || strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_Legacy_CAD)) && !isConnectedActiveCO) {
                    // In case of PSS_CAD_Object or Pss_legacy_CAD for charted drawing , then if it is not connected to Active CO
                    // PCM : TIGTK-5278 : 31/05/2017 : AB : START
                    // if (!(slSpecsConnectedPartPolicy.contains(TigerConstants.POLICY_PSS_DEVELOPMENTPART))) {
                    Map mapObjects = new HashMap();
                    mapObjects.put("domCADItem", domAffectedItem);
                    boolean bolAllowConnection = this.checkCADValidationForAddIntoCO(context, JPO.packArgs(mapObjects));
                    if (bolAllowConnection) {
                        slApprovedItemsList.addElement(strObjId);
                    } else {
                        bolCADConnectedToOnlyDevPart = true;
                    }

                    // }
                    // PCM : TIGTK-5278 : 31/05/2017 : AB : END
                } // PCM:TIGTK-4060 | 15/3/2017 |Rutuja Ekatpure :Start
                else if (isConnectedActiveCO) {
                    bFlag = true;
                }
                // PCM:TIGTK-4060 | 15/3/2017 |Rutuja Ekatpure :End

            }
            if (bFlag) {
                String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.ConnectedToActiveCO");
                emxContextUtilBase_mxJPO.mqlNotice(context, strAlertMessage);
            }

            if (bolCADConnectedToOnlyDevPart) {
                String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Warning.CADConnectedToDevPart");
                emxContextUtilBase_mxJPO.mqlNotice(context, strAlertMessage);
            }
            // PCM : TIGTK-9060 : 28/07/2017 : AB : END
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in processCOAffectedItems: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return getUniqueIdList(slApprovedItemsList);
    }

    /**
     * Method to get the connected Symmetrical Parts to Original Part.
     * @param context
     * @param args
     *            - Selected Item for Add Affected Item in CR which is comes from PSS_ECMFullSearchPostProcess.jsp
     * @return StringList - Contains the Object Id of the Part which is Passed in method argument and connected Symmetrical Part with them
     * @throws Exception
     */
    public StringList getSymmetricalPart(Context context, String[] args) throws Exception {
        // PCM TIGTK-3293 : 8/10/16 : Kwagh : Start
        String strCOselectable = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current";
        // TIGTK - 11674 : START
        String strCOSelectableImplemetedItem = "to [" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.current";
        StringList slActiveCAState = new StringList();
        slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_PENDING);
        slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_INWORK);
        slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_INAPPROVAL);
        // TIGTK - 11674 : End
        StringList selectStmts = new StringList(1);
        selectStmts.addElement(DomainConstants.SELECT_ID);
        selectStmts.addElement(strCOselectable);
        selectStmts.addElement(strCOSelectableImplemetedItem);
        StringList relStmts = new StringList(0);
        StringList strSelectedItems = new StringList();
        StringList strSymmetricalItems = new StringList();
        // Start : Harika Modifications As per Comments from Arjun: Removed the Individual getInfo Calls
        StringList slObjSelects = new StringList();
        slObjSelects.addElement(DomainConstants.SELECT_TYPE);
        slObjSelects.addElement(DomainConstants.SELECT_POLICY);
        slObjSelects.addElement("type.kindof[" + DomainConstants.TYPE_PART + "]");
        String strObjectPolicy = "";
        String strIsPart = "";
        // End : Harika Modifications As per Comments from Arjun: Removed the Individual getInfo Calls
        for (int i = 0; i < args.length; i++) {
            strSelectedItems.add(args[i]);
            DomainObject domSelectedItem = DomainObject.newInstance(context, (String) strSelectedItems.get(i));
            // Start : Harika Modifications As per Comments from Arjun : Removed the Individual getInfo Calls
            // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
            Map objectsMap = domSelectedItem.getInfo(context, slObjSelects);
            strObjectPolicy = (String) objectsMap.get(DomainConstants.SELECT_POLICY);
            strIsPart = (String) objectsMap.get("type.kindof[" + DomainConstants.TYPE_PART + "]");
            // End : Harika Modifications As per Comments from Arjun: Removed the Individual getInfo Calls

            // Check if Object Type is Part & policy is (Standard Part or PSS_EC_Part) than Check for Connected Symmetrical Part.
            if (strIsPart.equalsIgnoreCase("TRUE") && (strObjectPolicy.equals(TigerConstants.POLICY_PSS_ECPART))) { // || strObjectPolicy.equals(TigerConstants.POLICY_PSS_DEVELOPMENTPART))) {
                // TIGTK-3242 :AB : 23/09/2016: START
                String strObjectWhere = "id != " + (String) strSelectedItems.get(i) + "&& policy != " + TigerConstants.POLICY_PSS_DEVELOPMENTPART;

                MapList mlSymmetricalPartObject = domSelectedItem.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, // relationship pattern
                        DomainConstants.TYPE_PART, // object pattern
                        selectStmts, // object selects
                        relStmts, // relationship selects
                        true, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        strObjectWhere, // object where clause
                        null, 0); // relationship where clause
                // TIGTK-3242 :AB : 23/09/2016: END

                if (mlSymmetricalPartObject.size() != 0) {
                    for (int j = 0; j < mlSymmetricalPartObject.size(); j++) {
                        Map<String, String> map = (Map<String, String>) mlSymmetricalPartObject.get(j);
                        String strSymmetricpartId = map.get(DomainObject.SELECT_ID);
                        PSS_enoECMChangeUtil_mxJPO encEMCChangeUtil = new PSS_enoECMChangeUtil_mxJPO(context, args);
                        StringList slConnectedCAToSymmPart = encEMCChangeUtil.getStringListFromMap(context, map, strCOselectable);
                        // TIGTK - 11674
                        StringList slConnectedCAToSymmPartImplementedItem = encEMCChangeUtil.getStringListFromMap(context, map, strCOSelectableImplemetedItem);
                        // TIGTK - 11674 : End
                        if (!slConnectedCAToSymmPart.contains(TigerConstants.STATE_CHANGEACTION_PENDING) && !slConnectedCAToSymmPart.contains(TigerConstants.STATE_CHANGEACTION_INWORK)
                                && !slConnectedCAToSymmPart.contains(TigerConstants.STATE_CHANGEACTION_INAPPROVAL)) {
                            strSymmetricalItems.add(strSymmetricpartId);
                        } // Add Symetrical Part Id to StringList of Symmetrical Items
                          // PCM TIGTK-3293 : 8/10/16 : Kwagh : End
                          // TIGTK - 11674
                        if (!slActiveCAState.contains(slConnectedCAToSymmPartImplementedItem) && !slActiveCAState.contains(slConnectedCAToSymmPart)) {
                            strSymmetricalItems.add(strSymmetricpartId);
                        }
                        // TIGTK - 11674 : End
                    }
                }
            }
        }

        if (strSymmetricalItems.size() != 0) {
            for (int j = 0; j < strSymmetricalItems.size(); j++) {
                // TIGTK-3120 :Rutuja Ekatpure :15/09/2016:Start
                if (!strSelectedItems.contains(strSymmetricalItems.get(j))) {
                    strSelectedItems.add(strSymmetricalItems.get(j));
                }
                // TIGTK-3120 :Rutuja Ekatpure :15/09/2016:End
            }
        }
        return strSelectedItems;
    }

    /**
     * This method is used to get the CAD Object connected to Object using "Part Specification" relationship.
     * @param context
     * @param strObjectId
     * @return
     * @throws Exception
     */
    public StringList getCADAndDrawingObjectsList(Context context, String strObjectId) throws Exception {
        StringList slItems = new StringList();
        try {
            DomainObject domObject = DomainObject.newInstance(context, strObjectId);
            // TIGTK-17754 : stembulkar : start
            String strObjCurrentState = domObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            // TIGTK-17754 : stembulkar : end
            // PCM : TIGTK-7126 : 02/05/2017 : AB : START
            Pattern relpattern = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
            relpattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
            // TIGTK-17754 : stembulkar : start
            StringList slObjectSelect = new StringList(2);
            slObjectSelect.add(DomainConstants.SELECT_ID);
            slObjectSelect.add(DomainConstants.SELECT_CURRENT);
            // TIGTK-17754 : stembulkar : end

            // PCM TIGTK-5278: 15/03/2017 : KWagh : START
            // PCM TIGTK-3977 | 24/01/17 :Pooja Mantri : Start -- For Same CAD in CO Issue

            // PCM : TIGTK-5278 : 31/05/2017 : AB : START
            // This where clause will be implement in PHASE 2.0. So, I have commented this
            /*
             * String strBusWhere = "(!(to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "," + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].from.policy==\"" +
             * TigerConstants.POLICY_PSS_DEVELOPMENTPART + "\"))&&(!((to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "]==\"True\")&&(to[" +
             * ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current smatchlist \"Pending,In Work,In Approval\" \",\")))";
             */

            // Start : TIGTK-17754 : Prakash B
            /*
             * StringBuilder sbWhere = new StringBuilder(); sbWhere.append("(current != 'Obsolete') &&"); sbWhere.append("(!(to["); sbWhere.append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
             * sbWhere.append("]==\"True\")&&(to["); sbWhere.append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
             * sbWhere.append("].from.current smatchlist \"Pending,In Work,In Approval\" \",\"))");
             */

            StringBuilder sbWhere = new StringBuilder();
            sbWhere.append("(current != 'Obsolete') && ");
            sbWhere.append("!(to[");
            sbWhere.append(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);
            sbWhere.append("].from.current smatchlist \"Pending,In Work,In Approval\" \",\")");

            // END : TIGTK-17754 : Prakash B

            // PCM : TIGTK-5278 : 31/05/2017 : AB : END
            MapList mlRelatedObjects = domObject.getRelatedObjects(context, relpattern.getPattern(), STR_TYPE_ALL_CAD, slObjectSelect, null, false, true, (short) 1, sbWhere.toString(), null, 0);
            // TIGTK-17754 : stembulkar : start
            String strSpecCurrentState = "";
            MapList objList = new MapList();
            Map objMap;
            int size = mlRelatedObjects.size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    objMap = (Map) mlRelatedObjects.get(i);
                    strSpecCurrentState = (String) objMap.get(DomainConstants.SELECT_CURRENT);
                    if (TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(strObjCurrentState)) {
                        strObjCurrentState = TigerConstants.STATE_INWORK_CAD_OBJECT;
                    }
                    // TIGTK-17754 : Prakash B
                    if (strSpecCurrentState.equalsIgnoreCase(strObjCurrentState) || strObjCurrentState.contains(strSpecCurrentState) || strSpecCurrentState.contains(strObjCurrentState)) {
                        objList.add(mlRelatedObjects.get(i));
                    }
                }
            }
            // TIGTK-17754 : stembulkar : end
            // PCM : TIGTK-7126 : 02/05/2017 : AB : END
            // PCM TIGTK-3977 | 24/01/17 :Pooja Mantri : End -- For Same CAD in CO Issue
            // PCM TIGTK-5278: 15/03/2017 : KWagh : End
            // TIGTK-17754 : stembulkar : start
            slItems = getStringListFromMaplist(objList, DomainConstants.SELECT_ID);
            // TIGTK-17754 : stembulkar : end
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCADAndDrawingObjectsList: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return slItems;

    }

    /**
     * Description : This method is used to get "Standard Parts" connected to CA Objects
     * @author abhalani
     * @args
     * @Date Oct 5, 2016
     */

    public void splitChangeActionForStandardItems(Context context, String[] args) throws Exception {
        StringList slStandardAffectedItems = new StringList();
        Map mPartToRC = new HashMap();

        String strCAID = "";

        PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null);
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        // Get the ObjectID of CO
        String strCOID = (String) programMap.get("strCOObjectID");
        DomainObject domCO = new DomainObject(strCOID);

        // Get the All Change Action of related CO
        StringList slCOobjectSelects = new StringList(3);
        slCOobjectSelects.add(DomainObject.SELECT_ID);
        slCOobjectSelects.add(DomainObject.SELECT_POLICY);
        // select CA type
        slCOobjectSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");

        MapList mlCAConnectedToCO = domCO.getRelatedObjects(context, // context // here
                ChangeConstants.RELATIONSHIP_CHANGE_ACTION, // relationship pattern
                ChangeConstants.TYPE_CHANGE_ACTION, // object pattern
                slCOobjectSelects, // object selects
                null, // relationship selects
                false, // to direction
                true, // from direction
                (short) 0, // recursion level
                null, // object where clause
                null, // relationship where clause
                (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 0, // pageSize
                null, null, null, null);
        if (mlCAConnectedToCO.size() != 0) {
            for (int i = 0; i < mlCAConnectedToCO.size(); i++) {
                Map mapCAObject = (Map) mlCAConnectedToCO.get(i);
                String strCAOID = (String) mapCAObject.get(DomainConstants.SELECT_ID);
                DomainObject domCA = new DomainObject(strCAOID);
                String strCAType = (String) mapCAObject.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");
                StringList relSelects = new StringList(2);
                relSelects.add(DomainRelationship.SELECT_RELATIONSHIP_ID);
                relSelects.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

                // Get Related Affected Items
                MapList mlAIConnectedToCA = domCA.getRelatedObjects(context, // context // here
                        ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, // relationship pattern
                        DomainConstants.QUERY_WILDCARD, // object pattern
                        slCOobjectSelects, // object selects
                        relSelects, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, // relationship where clause
                        (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 0, // pageSize
                        null, null, null, null);
                StringList slTempAffectedItemIDs = new StringList();
                int count = 0;
                StringList slRelId = new StringList();
                if (mlAIConnectedToCA.size() != 0) {
                    int intSizeOfList = mlAIConnectedToCA.size();
                    for (int j = 0; j < intSizeOfList; j++) {
                        Map mapAIObject = (Map) mlAIConnectedToCA.get(j);
                        String strAIID = (String) mapAIObject.get(DomainConstants.SELECT_ID);
                        String strRelID = (String) mapAIObject.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                        String strAIPolicy = (String) mapAIObject.get(DomainConstants.SELECT_POLICY);
                        String strRequesedChangeValue = (String) mapAIObject.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

                        if (UIUtil.isNotNullAndNotEmpty(strAIPolicy) && TigerConstants.POLICY_STANDARDPART.equalsIgnoreCase(strAIPolicy) && intSizeOfList != 1) {
                            // Add standard Affected Item to List
                            slTempAffectedItemIDs.add(strAIID);
                            // map contains requested change value
                            mPartToRC.put(strAIID, strRequesedChangeValue);
                            // list of relationship ids to disconnect from current CA
                            slRelId.add(strRelID);
                            count++;
                        }
                    }
                    if ("Standard".equals(strCAType) && count == intSizeOfList) {
                        // old CA having stamdard part connected
                        strCAID = strCAOID;
                    } else {
                        for (int j = 0; j < slRelId.size(); j++) {
                            // disconnect standard part from old CA
                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                            // PCM TIGTK-4385 | 08/02/17 : AB : START
                            MqlUtil.mqlCommand(context, "trigg on");
                            DomainRelationship.disconnect(context, (String) slRelId.get(j));
                            MqlUtil.mqlCommand(context, "trigg off");
                            // PCM TIGTK-4385 | 08/02/17 : AB : END
                            ContextUtil.popContext(context);
                            // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                        }
                        slStandardAffectedItems.addAll(slTempAffectedItemIDs);
                    }
                }
            }
            // ALM defect -1684 :multiple standard part not added on CO:28/11/2016:Rutuja Ekatpure:start
            if (slStandardAffectedItems.size() != 0) {
                // Attach all Standard Part AI to CA.
                if (UIUtil.isNullOrEmpty(strCAID)) {
                    // if no CA with affected item as standard part then create new CA and attach all std part
                    strCAID = changeUtil.createNewCA(context, strCOID);
                }
                DomainObject domNewCA = new DomainObject(strCAID);
                domNewCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, "Standard");
                Map mCATOStdPartMap = DomainRelationship.connect(context, domNewCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, true,
                        ((String) FrameworkUtil.join(slStandardAffectedItems, ",")).split(","));
                PropertyUtil.setGlobalRPEValue(context, "splitChangeActionForStandardItems", "TRUE");
                changeUtil.setRequestedChange(context, mCATOStdPartMap, mPartToRC);
                PropertyUtil.setGlobalRPEValue(context, "splitChangeActionForStandardItems", "");

                // PCM TIGTK-4436: 02/03/2017 : KWagh : START

                String strFunctionality = (String) programMap.get("functionality");
                if (UIUtil.isNotNullAndNotEmpty(strFunctionality)) {
				//TIGTK-14264 : START
                    if ("MoveToNewCO".equalsIgnoreCase(strFunctionality) || "MoveToExistingCO".equalsIgnoreCase(strFunctionality) || "AddToExistingChange".equalsIgnoreCase(strFunctionality) || "AddToNewChange".equalsIgnoreCase(strFunctionality)) {

                        if (mCATOStdPartMap.size() > 0) {
                            Iterator itr = mCATOStdPartMap.entrySet().iterator();
                            while (itr.hasNext()) {
                                Map.Entry entry = (Entry) itr.next();

                                String strRelID = (String) entry.getValue();
                                DomainRelationship objRelID = new DomainRelationship(strRelID);
                                objRelID.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REASON_FOR_CHANGE, domCO.getDescription(context));
                                //TIGTK-14264 : end
                                this.setAttributeOnRelationship(context, strRelID);
                            }
                        }
                    }

                }
                // PCM TIGTK-4436: 02/03/2017 : KWagh : End
            }
            // ALM defect -1684 :multiple standard part not added on CO:28/11/2016:Rutuja Ekatpure:end
        }
    }

    /**
     * This method is invoked via a Create Action trigger when the Change Action connected to Affected Item. If the selected Affected Item is of *Part type having Symmetrical Part/CAD object and if
     * Symmetrical Part/CAD is not already present in Change Action then add it automatically.
     * @param context
     *            Context : User's Context.
     * @param args
     *            String array
     * @return
     * @throws Exception
     *             if searching Parts object fails.
     */
    public int connectSymmetricalPartsAndCAD(Context context, String args[]) throws Exception {
        try {
            String strCOselectable = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current";
            String strFromObjectId = args[0];
            String strToObjectId = args[1];
            HashSet hsRelatedSymmPartsAndCAD = new HashSet();
            StringList allCAAffectedItemList = new StringList();
            StringList selectStmts = new StringList(1);
            selectStmts.addElement(DomainConstants.SELECT_ID);
            selectStmts.addElement(DomainConstants.SELECT_NAME);
            selectStmts.addElement(DomainConstants.SELECT_POLICY);
            selectStmts.addElement(strCOselectable);
            // PCM TIGTK-3417 - 14/10/16 - KWagh,AB - Start
            DomainObject domFromObject = DomainObject.newInstance(context, strFromObjectId);
            String strRelatedCOID = domFromObject.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
            DomainObject domCO = DomainObject.newInstance(context, strRelatedCOID);
            StringList slRelatedCRID = domCO.getInfoList(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ORDER + "].from.id");

            if (slRelatedCRID.isEmpty() || slRelatedCRID.contains(null)) {
                // PCM TIGTK-3417 - 14/10/16 - KWagh,AB - End

                // PCM TIGTK-3101 : 19/09/16 : AB : START
                selectStmts.add("to[" + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
                // PCM TIGTK-3635 | 23/11/16 :Pooja Mantri : Start
                StringList slNotAllowedCAStateList = new StringList();
                slNotAllowedCAStateList.add(TigerConstants.STATE_CHANGEACTION_PENDING);
                slNotAllowedCAStateList.add(TigerConstants.STATE_CHANGEACTION_INWORK);
                slNotAllowedCAStateList.add(TigerConstants.STATE_CHANGEACTION_INAPPROVAL);
                // PCM TIGTK-3635 | 23/11/16 :Pooja Mantri: End
                StringList relStmts = new StringList(0);
                PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null);
                DomainObject domToObject = DomainObject.newInstance(context, strToObjectId);
                String strIsPart = (String) domToObject.getInfo(context, "type.kindof[" + DomainConstants.TYPE_PART + "]");
                String strObjectPolicy = (String) domToObject.getInfo(context, DomainConstants.SELECT_POLICY);
                if (strIsPart.equalsIgnoreCase("TRUE") && (strObjectPolicy.equals(TigerConstants.POLICY_PSS_ECPART) || strObjectPolicy.equals(TigerConstants.POLICY_STANDARDPART))) {

                    // Check if Object Type is Part & policy is (Standard Part or PSS_EC_Part) than get the CAD Objects connected with Part
                    // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                    MapList mlCADObject = domToObject.getRelatedObjects(context, RELATIONSHIP_PART_SPECIFICATION, STR_TYPE_ALL_CAD, selectStmts, relStmts, true, true, (short) 1, null, null, 0);

                    if (mlCADObject.size() != 0) {
                        for (int j = 0; j < mlCADObject.size(); j++) {
                            Map<String, String> map = (Map<String, String>) mlCADObject.get(j);
                            String strCADObjectId = map.get("id");
                            // PCM TIGTK-3635 | 23/11/16 :Pooja Mantri: Start
                            // TODO //commented by ARJUN TO BE CHECK WITH PPOJA
                            // ARJUN PLEASE VERIFY THIS CALL
                            StringList slCACurrentState = changeUtil.getStringListFromMap(context, map, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current");
                            boolean flag = isAffectedItemHasNoActiveCAConnected(slNotAllowedCAStateList, slCACurrentState);
                            // PCM TIGTK-3635 | 23/11/16 :Pooja Mantri: End
                            StringList slSpecsConnectedPartPolicy = changeUtil.getStringListFromMap(context, map, "to[" + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
                            // PCM TIGTK-3635 | 23/11/16 :Pooja Mantri: Start
                            if (!slSpecsConnectedPartPolicy.contains(TigerConstants.POLICY_PSS_DEVELOPMENTPART) && flag) {
                                // PCM TIGTK-3635 | 23/11/16 :Pooja Mantri: End
                                hsRelatedSymmPartsAndCAD.add(strCADObjectId);
                            }
                        }
                    }
                }
                // PCM TIGTK-3101 : 19/09/16 : AB : END
                // Check if Object Type is Part & policy is (Standard Part or PSS_EC_Part) than Check for Connected Symmetrical Part.
                // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                MapList mlSymmetricalPartObject = domToObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, DomainConstants.TYPE_PART, selectStmts, relStmts, true,
                        true, (short) 1, null, null, 0);

                if (mlSymmetricalPartObject.size() != 0) {
                    for (int k = 0; k < mlSymmetricalPartObject.size(); k++) {
                        Map<String, String> map = (Map<String, String>) mlSymmetricalPartObject.get(k);
                        // PCM TIGTK-3293 : 8/10/16 : Kwagh : Start
                        String strSymmetricpartId = map.get(DomainObject.SELECT_ID);
                        PSS_enoECMChangeUtil_mxJPO encEMCChangeUtil = new PSS_enoECMChangeUtil_mxJPO(context, args);
                        StringList slConnectedCAToSymmPart = encEMCChangeUtil.getStringListFromMap(context, map, strCOselectable);

                        // PCM TIGTK-3806 | 15/12/16 :Pooja Mantri : Start
                        // commented by Arjun to avoid issue ot type casting . We are not using this variable anyways. To be checked by POOJA

                        // PCM TIGTK-3846 | 28/12/16 :Pooja Mantri : Start
                        if ((slConnectedCAToSymmPart.size() > 0) && (!slConnectedCAToSymmPart.contains(TigerConstants.STATE_CHANGEACTION_PENDING)
                                && !slConnectedCAToSymmPart.contains(TigerConstants.STATE_CHANGEACTION_INWORK) && !slConnectedCAToSymmPart.contains(TigerConstants.STATE_CHANGEACTION_INAPPROVAL)))
                            // PCM TIGTK-3846 | 28/12/16 :Pooja Mantri : End
                            // PCM TIGTK-3806 | 15/12/16 :Pooja Mantri : End
                            hsRelatedSymmPartsAndCAD.add(strSymmetricpartId);
                        // PCM TIGTK-3293 : 8/10/16 : Kwagh : End
                    }
                }

                // HashSet to StringList
                ArrayList<String> list = new ArrayList<String>(hsRelatedSymmPartsAndCAD);
                StringList relatedObjectsOfPartList = new StringList(list);
                if (relatedObjectsOfPartList.size() != 0) {
                    HashSet finalAffectedItems = new HashSet();

                    // Get Connected Change Order with Change Action
                    MapList changeOrderList = domFromObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, TigerConstants.TYPE_PSS_CHANGEORDER, selectStmts, relStmts, true,
                            false, (short) 1, null, null);

                    for (int i = 0; i < changeOrderList.size(); i++) {
                        Map mCOObj = (Map) changeOrderList.get(i);
                        String strCOId = (String) mCOObj.get(DomainConstants.SELECT_ID);
                        DomainObject domCOObj = new DomainObject(strCOId);

                        // Get connected Change Action Objects with CO
                        MapList changeActionList = domCOObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, selectStmts, relStmts, false,
                                true, (short) 1, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0);

                        for (int l = 0; l < changeActionList.size(); l++) {
                            Map mCAObj = (Map) changeActionList.get(l);
                            String strCAId = (String) mCAObj.get(DomainConstants.SELECT_ID);
                            DomainObject domCAObj = new DomainObject(strCAId);

                            // get Change Affected item of ChangeAction
                            // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                            MapList changeAffectedItemList = domCAObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, DomainConstants.QUERY_WILDCARD, selectStmts,
                                    relStmts, false, true, (short) 1, DomainObject.EMPTY_STRING, DomainObject.EMPTY_STRING, (short) 0);

                            // Add Affected Item To hsRelatedSymmPartsAndCAD HashSet
                            for (int m = 0; m < changeAffectedItemList.size(); m++) {
                                Map mChanegAffectedObj = (Map) changeAffectedItemList.get(m);
                                String strChangeAffectedItemId = (String) mChanegAffectedObj.get(DomainConstants.SELECT_ID);
                                allCAAffectedItemList.add(strChangeAffectedItemId);
                                // PCM TIGTK-3293 : 8/10/16 : Kwagh : Start
                                PSS_enoECMChangeUtil_mxJPO encEMCChangeUtil = new PSS_enoECMChangeUtil_mxJPO(context, args);
                                StringList slConnectedCAToSymmPart = encEMCChangeUtil.getStringListFromMap(context, mChanegAffectedObj, strCOselectable);

                                // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                                if ((slConnectedCAToSymmPart.size() > 0)
                                        && (!slConnectedCAToSymmPart.contains(TigerConstants.STATE_CHANGEACTION_PENDING) && !slConnectedCAToSymmPart.contains(TigerConstants.STATE_CHANGEACTION_INWORK)
                                                && !slConnectedCAToSymmPart.contains(TigerConstants.STATE_CHANGEACTION_INAPPROVAL)))
                                    // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts
                                    // PCM TIGTK-3293 : 8/10/16 : Kwagh : End
                                    hsRelatedSymmPartsAndCAD.add(strChangeAffectedItemId);
                            }

                        }

                        for (int k = 0; k < relatedObjectsOfPartList.size(); k++) {
                            boolean bolSameItem = false;
                            for (int m = 0; m < allCAAffectedItemList.size(); m++) {
                                if (allCAAffectedItemList.get(m).equals(relatedObjectsOfPartList.get(k))) {
                                    bolSameItem = true;
                                } else {
                                }
                            }

                            if (bolSameItem == false) {
                                finalAffectedItems.add(relatedObjectsOfPartList.get(k));
                            }
                        }

                        if (finalAffectedItems.size() != 0) {
                            ArrayList<String> arrayList = new ArrayList<String>(finalAffectedItems);

                            StringList slFinalItems = new StringList(arrayList);
                            // Connect All Final affected items with Change Order
                            ChangeOrder changeOrder = new ChangeOrder(strCOId);
                            // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                            Map mpInvalidObjects = changeOrder.connectAffectedItems(context, slFinalItems);
                            // Pooja
                            String strInvalidObjectts = (String) mpInvalidObjects.get("strErrorMSG");
                            // PCM : TIGTK-10183 : 28/09/2017 : AB : START
                            // Set the flag on Relationship
                            this.setTranserFromCRFlagForCOChangeAssessment(context, JPO.packArgs(mpInvalidObjects));
                            // PCM : TIGTK-10183 : 28/09/2017 : AB : END
                            if (!ChangeUtil.isNullOrEmpty(strInvalidObjectts)) {
                                MqlUtil.mqlCommand(context, "notice $1", strInvalidObjectts);
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                    }

                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectSymmetricalPartsAndCAD: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return 0;
    }

    // PCM TIGTK-3846 | 13/01/17 :Pooja Mantri : End -- For Symmetric Parts

    /**
     * Added by Arjun
     * @param slPrimaryList
     * @param slListToIterate
     * @return
     * @throws Exception
     */
    public boolean isAffectedItemHasNoActiveCAConnected(StringList slPrimaryActiveCAStateList, StringList slListOfConnectedCAState) throws Exception {
        boolean flag = true;
        for (int i = 0; i < slListOfConnectedCAState.size(); i++) {
            if (slPrimaryActiveCAStateList.contains(slListOfConnectedCAState.get(i))) {
                flag = false;
                break;
            }
        }

        return flag;
    }

    // ARJUN END

    // PCM TIGTK-3981 | 24/01/17 :Harika Varanasi : Start -- For Planned End Date CA
    /**
     * This method is invoked to Update "Change Action" attributes "PSS_PlannedEndDate" and Assignee field from PSS_ECMFullSearchPostProcess.jsp.
     * @param --
     *            CO Object Id
     * @return -- void --Nothing
     * @throws Exception
     */
    public void updateChangeActionRel(Context context, String[] args) throws Exception {
        PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null);
        String[] arrCOCAId = new String[2];
        String[] arrCAInfo = new String[2];
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        // Get the ObjectID of CO
        String strCOID = (String) programMap.get("strCOObjectID");
        arrCOCAId[0] = strCOID;
        arrCAInfo[1] = "PSS_ChangeAction";

        // PCM TIGTK-3846 | 27/01/17 :Pooja Mantri : Start -- For Symmetric Parts
        StringList objectSelect = new StringList(2);
        objectSelect.add(DomainConstants.SELECT_ID);
        objectSelect.add("interface[PSS_ChangeAction]");
        // PCM TIGTK-3846 | 27/01/17 :Pooja Mantri : End -- For Symmetric Parts

        DomainObject domCO = new DomainObject(strCOID);
        MapList mlCAConnectedToCO = domCO.getRelatedObjects(context, // context // here
                ChangeConstants.RELATIONSHIP_CHANGE_ACTION, // relationship pattern
                ChangeConstants.TYPE_CHANGE_ACTION, // object pattern
                objectSelect, // object selects
                null, // relationship selects
                false, // to direction
                true, // from direction
                (short) 0, // recursion level
                null, // object where clause
                null, // relationship where clause
                (short) 0, false, // checkHidden
                true, // preventDuplicates
                (short) 0, // pageSize
                null, null, null, null);

        if (mlCAConnectedToCO.size() > 0) {
            for (int i = 0; i < mlCAConnectedToCO.size(); i++) {
                Map mapCAObject = (Map) mlCAConnectedToCO.get(i);
                String strCAOID = (String) mapCAObject.get(DomainConstants.SELECT_ID);
                // PCM TIGTK-3846 | 27/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                String interfaceExists = (String) mapCAObject.get("interface[PSS_ChangeAction]");
                // PCM TIGTK-3846 | 27/01/17 :Pooja Mantri : End -- For Symmetric Parts

                // PCM TIGTK-3846 | 27/01/17 :Pooja Mantri : Start -- For Symmetric Parts
                if (UIUtil.isNotNullAndNotEmpty(interfaceExists) && !"TRUE".equalsIgnoreCase(interfaceExists)) {
                    arrCOCAId[1] = strCAOID;
                    arrCAInfo[0] = strCAOID;
                    changeUtil.setInterfaceOnChangeAction(context, arrCAInfo);
                    changeUtil.setAttributesOnChangeAction(context, arrCOCAId);
                }
                // PCM TIGTK-3846 | 27/01/17 :Pooja Mantri : End -- For Symmetric Parts
            }
        }

    }

    // PCM TIGTK-3981 | 24/01/17 :Harika Varanasi : End -- For Planned End Date CA

    /**
     * This method used in PostProcess of MoveToNewCO, for disconnect the Relationship between newly created CA and CR PCM : TIGTK-3984 : 30/01/2017 : AB
     * @param context
     * @param args
     * @throws Exception
     */

    public void disconnectCAAndCRConnection(Context context, String args[]) throws Exception {
        try {
            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);

            // Get the ObjectId of newly created CO
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String strNewCOID = (String) paramMap.get("newObjectId");
            DomainObject domCO = new DomainObject(strNewCOID);

            // Get the connected all CA of newly created ChangeOrder
            MapList mlConnectedCAs = domCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CHANGEACTION, TigerConstants.TYPE_CHANGEACTION, slObjectSle, null, false, true, (short) 1, null, null,
                    0);
            if (!mlConnectedCAs.isEmpty()) {
                int sizeMap = mlConnectedCAs.size();
                for (int i = 0; i < sizeMap; i++) {
                    Map mapCAInfo = (Map) mlConnectedCAs.get(i);
                    String strCAID = (String) mapCAInfo.get(DomainConstants.SELECT_ID);

                    // Get the Relationship id between CA and CR
                    String str = "print bus " + strCAID + " select to[" + TigerConstants.TYPE_CHANGEACTION + "|from.type.kindof[" + TigerConstants.TYPE_PSS_CHANGEREQUEST + "]].id";
                    String strResult = MqlUtil.mqlCommand(context, str, false, false);
                    String strCAAndCRConnectionID = DomainConstants.EMPTY_STRING;
                    if (strResult.lastIndexOf('=') != -1) {
                        strCAAndCRConnectionID = strResult.substring(strResult.lastIndexOf('=') + 1);
                    }

                    // Disconenct relationship between CA and CR
                    if (UIUtil.isNotNullAndNotEmpty(strCAAndCRConnectionID)) {
                        DomainRelationship.disconnect(context, strCAAndCRConnectionID);
                    }
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in disconnectCAAndCRConnection: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
    }

    // PCM TIGTK-4436: 02/03/2017 : KWagh : START

    /**
     * @author KWagh
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is used to set attribute "PSS_TRANSFERFROMCRFLAG" to indicate affected items are transferred from CR or manually added on CO.
     */
    public boolean setFlagForTransferredFromCR(Context context, String args[]) throws Exception {
        boolean bIsSet = false;
        try {

            Map programMap = (HashMap) JPO.unpackArgs(args);
            StringList selectedItemsList = (StringList) programMap.get("selectedItemsList");

            String strAffectedItemID = DomainConstants.EMPTY_STRING;

            int nListSize = selectedItemsList.size();

            for (int cnt = 0; cnt < nListSize; cnt++) {
                strAffectedItemID = (String) selectedItemsList.get(cnt);

                DomainObject domAI = new DomainObject(strAffectedItemID);

                StringList slObjSelect = new StringList();
                slObjSelect.add(DomainConstants.SELECT_ID);

                StringList slRelSelect = new StringList();
                slRelSelect.add(DomainRelationship.SELECT_RELATIONSHIP_ID);

                String strWhere = "(current!='" + TigerConstants.STATE_CHANGEACTION_COMPLETE + "'&& current!='" + TigerConstants.STATE_CHANGEACTION_CANCELLED + "'&& current!='"
                        + TigerConstants.STATE_CHANGEACTION_ONHOLD + "')";
                Access access = domAI.getAccessMask(context);
                if (access.hasReadAccess()) {
                    MapList mlchangeAction = domAI.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, ChangeConstants.TYPE_CHANGE_ACTION, slObjSelect, slRelSelect, true,
                            false, (short) 1, strWhere, null, 0);

                    if (!mlchangeAction.isEmpty()) {
                        int mlCount = mlchangeAction.size();
                        for (int i = 0; i < mlCount; i++) {

                            Map mChangeAction = (Map) mlchangeAction.get(i);
                            String strRelationshipID = (String) mChangeAction.get(DomainRelationship.SELECT_RELATIONSHIP_ID);
                            this.setAttributeOnRelationship(context, strRelationshipID);
                            bIsSet = true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setFlagForTransferredFromCR: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return bIsSet;

    }

    // PCM TIGTK-4436: 02/03/2017 : KWagh : End

    // PCM TIGTK-4436: 02/03/2017 : KWagh : START
    /**
     * @author KWagh
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is used to diplay flag on table.
     */
    public Vector<String> showFlagForTransferredFromCR(Context context, String[] args) throws Exception {
        try {
            Vector<String> vecResult = new Vector<String>();

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);

            MapList objectList = (MapList) programMap.get("objectList");

            for (int i = 0; i < objectList.size(); i++) {
                StringBuilder str = new StringBuilder();
                Map<String, String> map = (Map<String, String>) objectList.get(i);

                String strConnectionId = (String) map.get("id[connection]");

                DomainRelationship DomRelChangeAffectedItem = new DomainRelationship(strConnectionId);

                String strAttrPSS_TransferFromCRFlag = DomRelChangeAffectedItem.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TRANSFERFROMCRFLAG);

                String statusImageString = "";
                if (strAttrPSS_TransferFromCRFlag.equalsIgnoreCase("NO")) {
                    statusImageString = "<a><img ALIGN=\"right\" border=\"0\" src=\"../common/images/buttonDialogCancel.gif\"  title=\"Manually added in Change Order\"></img></a>";
                } else if (strAttrPSS_TransferFromCRFlag.equalsIgnoreCase("YES")) {
                    statusImageString = "<a><img ALIGN=\"right\" border=\"0\" src=\"../common/images/buttonDialogDone.gif\" title=\"Transferred from CR\"></img></a>";
                }

                str.append("");
                str.append(statusImageString);
                vecResult.add(str.toString());
            }

            return vecResult;

        } catch (Exception ex) {
            throw ex;
        }
    }

    // PCM TIGTK-4436: 02/03/2017 : KWagh : End

    // PCM TIGTK-4436: 02/03/2017 : KWagh : START
    /**
     * @author KWagh
     * @param context
     * @param args
     * @throws Exception
     *             This method is used to set attribute "PSS_TRANSFERFROMCRFLAG" to indicate affected items are transferred from CR or manually added on CO.
     */
    public void setFlagForMoveToNewCO(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            // Findbug Issue correction start
            // Date: 22/03/2017
            // By: Asha G.
            HashMap requestMap = (HashMap) programMap.get("requestMap");

            String strSelectedObjects = (String) requestMap.get("selectedObjIdList");

            StringList slSplitedRelIdList = null;
            // Findbug Issue correction End
            StringList strRelIdList = new StringList();
            String strRelID = DomainConstants.EMPTY_STRING;
            StringList slSplitedList = (StringList) FrameworkUtil.split(strSelectedObjects, "~");

            // TIGTK-14264
            String strDescription = (String) requestMap.get("Description");
           
            int nSplitListSize = slSplitedList.size();
            for (int cnt = 0; cnt < nSplitListSize; cnt++) {

                String strIds = (String) slSplitedList.get(cnt);

                slSplitedRelIdList = FrameworkUtil.split(strIds, "|");
                strRelID = (String) slSplitedRelIdList.get(0);

                strRelIdList.add(strRelID);

            }

            int nRelListSize = strRelIdList.size();
            for (int i = 0; i < nRelListSize; i++) {
                strRelID = (String) strRelIdList.get(i);
                DomainRelationship drOb = new DomainRelationship(strRelID);
                // TIGTK-14264
                drOb.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REASON_FOR_CHANGE, strDescription);
                this.setAttributeOnRelationship(context, strRelID);
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setFlagForMoveToNewCO: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            // throw ex;
        }

    }

    // PCM TIGTK-4436: 02/03/2017 : KWagh : End

    // PCM TIGTK-4436: 02/03/2017 : KWagh : START
    /**
     * @author KWagh
     * @param context
     * @param strRelID
     * @throws Exception
     *             This method is used to set attribute value on relationship "Chamhe Affected Item"
     */
    public void setAttributeOnRelationship(Context context, String strRelID) throws Exception {

        try {

            if (UIUtil.isNotNullAndNotEmpty(strRelID)) {
                DomainRelationship DomRelChangeAffectedItem = new DomainRelationship(strRelID);
                DomRelChangeAffectedItem.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TRANSFERFROMCRFLAG, "No");
            }
        } catch (Exception e) {
            logger.error("Error in setAttributeOnRelationship: ", e);
        }
    }

    // PCM TIGTK-4436: 02/03/2017 : KWagh : End

    // PCM TIGTK-4436: 02/03/2017 : KWagh : START
    /**
     * @author KWagh
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is used to set attribute "PSS_TRANSFERFROMCRFLAG" to indicate affected items are transferred from CR or manually added on CO.
     */
    public boolean setFlagForMoveToExistingCO(Context context, String args[]) throws Exception {
        boolean bIsSet = false;
        try {

            Map programMap = (HashMap) JPO.unpackArgs(args);
            String strCOID = (String) programMap.get("strCOObjectID");
            Map mapCAtype = (HashMap) programMap.get("mapCAtype");
            String strCAID = DomainConstants.EMPTY_STRING;
            DomainObject domCO = new DomainObject(strCOID);

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            MapList mlConnectedCA = domCO.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, slObjectSle, slRelSle, false, true, (short) 1,
                    null, null, 0);
            int nSize = mlConnectedCA.size();

            if (!mlConnectedCA.isEmpty()) {
                for (int i = 0; i < nSize; i++) {
                    Map mCAObj = (Map) mlConnectedCA.get(i);
                    strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    DomainObject domobjCA = new DomainObject(strCAID);

                    MapList mlConnectedAffectedItem = domobjCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, "*", slObjectSle, slRelSle, false, true, (short) 1, null,
                            null, 0);

                    if (!mlConnectedAffectedItem.isEmpty()) {
                        int mlCount = mlConnectedAffectedItem.size();
                        for (int cnt = 0; cnt < mlCount; cnt++) {

                            Map mAI = (Map) mlConnectedAffectedItem.get(cnt);
                            String strRelationshipID = (String) mAI.get(DomainRelationship.SELECT_RELATIONSHIP_ID);
                            // TIGTK-14264
                            String strAIid = (String) mAI.get(DomainConstants.SELECT_ID);
                            String strCAtype = (String)mapCAtype.get(strAIid);
                            if(UIUtil.isNotNullAndNotEmpty(strCAtype) && "false".equalsIgnoreCase(strCAtype))
                            {
                                DomainRelationship drObj = new DomainRelationship(strRelationshipID);
                                drObj.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REASON_FOR_CHANGE, domCO.getDescription(context));
                            }
                            this.setAttributeOnRelationship(context, strRelationshipID);
                            bIsSet = true;
                        }
                    }
                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setFlagForMoveToExistingCO: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw ex;
        }
        return bIsSet;

    }

    // PCM TIGTK-4436: 02/03/2017 : KWagh : End

    // PCM TIGTK-4631: 06/03/2017 : KWagh : START
    /**
     * @author Kwagh
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is used to display Change Order objects related Change Action.
     */
    public static String getChangeOrderName(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String objectId = (String) paramMap.get("objectId");
        String strCOName = DomainConstants.EMPTY_STRING;

        DomainObject domCA = new DomainObject(objectId);

        StringList slObjectSle = new StringList(1);
        slObjectSle.addElement(DomainConstants.SELECT_ID);
        slObjectSle.addElement(DomainConstants.SELECT_NAME);

        StringList slRelSle = new StringList(1);
        slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

        MapList mlConnectedCO = domCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, TigerConstants.TYPE_PSS_CHANGEORDER, slObjectSle, slRelSle, true, false, (short) 1, null,
                null, 0);

        if (!mlConnectedCO.isEmpty()) {
            int mlCount = mlConnectedCO.size();
            for (int cnt = 0; cnt < mlCount; cnt++) {

                Map mCO = (Map) mlConnectedCO.get(cnt);
                strCOName = (String) mCO.get(DomainConstants.SELECT_NAME);

            }
        }
        return strCOName;
    }

    // PCM TIGTK-4631: 06/03/2017 : KWagh : End

    // PCM TIGTK-4473: 08/03/2017 : Pooja Mantri : START
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is used to get getContextObjectAttribute.
     */
    public StringList getContextObjectAttribute(Context context, String[] args) throws Exception {

        StringList slContextAttributeValueList = new StringList();

        try {

            HashMap programMap = JPO.unpackArgs(args);
            HashMap paramList = (HashMap) programMap.get("paramList");
            MapList objectList = (MapList) programMap.get("objectList");
            HashMap columnMap = (HashMap) programMap.get("columnMap");
            HashMap settingsMap = (HashMap) columnMap.get("settings");

            String strAttributeNameSetting = (String) settingsMap.get("attribute");
            String strAttributeName = PropertyUtil.getSchemaProperty(context, strAttributeNameSetting);

            String strParentOID = (String) paramList.get("parentOID");
            DomainObject domParent = DomainObject.newInstance(context, strParentOID);

            String strAttributeValue = domParent.getAttributeValue(context, strAttributeName);
            for (int i = 0; i < objectList.size(); i++) {
                slContextAttributeValueList.add(strAttributeValue);
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getContextObjectAttribute: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return slContextAttributeValueList;
    }

    // PCM TIGTK-4473: 08/03/2017 : Pooja Mantri : END

    // PCM TIGTK-4475: 08/03/2017 : Pooja Mantri : START
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is used to get CRs related to CO on Related CRs Tab.
     */

    public MapList getChangeRequestItems(Context context, String[] args) throws Exception {

        MapList mlCRList = new MapList();

        try {

            HashMap programMap = JPO.unpackArgs(args);
            String sCOID = (String) programMap.get("objectId");
            DomainObject domCo = DomainObject.newInstance(context, sCOID);
            String strCurrent = (String) domCo.getInfo(context, DomainConstants.SELECT_CURRENT);

            StringList slObjectSle = new StringList(1);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            slRelSle.addElement("attribute[PSS_InWorkCONewCRTag]");

            DomainObject domCOObj = new DomainObject(sCOID);

            mlCRList = domCOObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ORDER, TigerConstants.TYPE_PSS_CHANGEREQUEST, slObjectSle, slRelSle, true, false, (short) 1, null, null,
                    0);

            if (strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_CHANGEORDER_INWORK)) {
                for (Object objCRInfo : mlCRList) {
                    Map mpCR = (Map) objCRInfo;
                    String strAttributeInWorkCONewCRTag = (String) mpCR.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_INWORKCONEWCRTAG + "]");
                    if (strAttributeInWorkCONewCRTag.equalsIgnoreCase(TigerConstants.ATTRIBUTE_PSS_INWORKCONEWCRTAG_RANGE_NO)) {
                        mpCR.put(TigerConstants.TABLE_SETTING_DISABLESELECTION, "true");
                    }

                }
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeRequestItems: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return mlCRList;
    }

    // PCM TIGTK-4475: 08/03/2017 : Pooja Mantri : END

    // PCM TIGTK-4475 | 09/03/17 : Pooja Mantri : START
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             This method is used to get Implemented Items For CO on CO Implemeted Items tab.
     */

    public MapList getCOImplementedItems(Context context, String[] args) throws Exception {
        MapList mlImplementedObjects = new MapList();
        try {

            HashMap programMap = JPO.unpackArgs(args);
            String strCOObjectId = (String) programMap.get("objectId");
            // Create Domain Object of CO
            DomainObject domCOObject = DomainObject.newInstance(context, strCOObjectId);

            StringList slObjectList = new StringList(DomainConstants.SELECT_ID);
            slObjectList.add(DomainObject.SELECT_POLICY);

            // Get Connected CA to CO
            StringList slCAOIDList = domCOObject.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].to.id");

            for (int i = 0; i < slCAOIDList.size(); i++) {
                String strCAOID = (String) slCAOIDList.get(i);
                // Create Domain Object of CA
                DomainObject domCAObject = DomainObject.newInstance(context, strCAOID);
                // TIGTK-14080 START
                String strCAName = domCAObject.getInfo(context, DomainConstants.SELECT_NAME);
                String strCAType = domCAObject.getInfo(context, DomainConstants.SELECT_TYPE);
                String strCARev = domCAObject.getInfo(context, DomainConstants.SELECT_REVISION);
                String strCAPolicy = domCAObject.getInfo(context, DomainConstants.SELECT_POLICY);
                String strCAState = domCAObject.getInfo(context, DomainConstants.SELECT_CURRENT);

                Map tempMap = new HashMap();
                tempMap.put("objectId", strCAOID);
                PSS_enoECMChangeAction_mxJPO objCA = new PSS_enoECMChangeAction_mxJPO(context, args);
                MapList mlTempList = objCA.getImplementedItems(context, JPO.packArgs(tempMap));

                if (!mlTempList.isEmpty()) {
                    for (int j = 0; j < mlTempList.size(); j++) {
                        Map mtempMap = (Map) mlTempList.get(j);
                        mtempMap.put("relatedCAName", strCAName);
                        mtempMap.put("relatedCAId", strCAOID);
                        mtempMap.put("relatedCAType", strCAType);
                        mtempMap.put("relatedCARev", strCARev);
                        mtempMap.put("relatedCAPolicy", strCAPolicy);
                        mtempMap.put("relatedCAState", strCAState);
                        mlImplementedObjects.add(mtempMap);
                    }

                }
                // TIGTK-14080 END
            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCOImplementedItems: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return mlImplementedObjects;
    }

    // PCM TIGTK-4475 | 09/03/17 : Pooja Mantri : END

    public MapList disableNotEligibleItemsForRemoval(Context context, MapList mlImplementedObjects, String strCOObjectId) throws Exception {
        // TODO Auto-generated method stub
        MapList mplFinal = new MapList();
        for (int j = 0; j < mlImplementedObjects.size(); j++) {
            Map mImplementedItem = (Map) mlImplementedObjects.get(j);
            String strImplObjID = (String) mImplementedItem.get(DomainObject.SELECT_ID);
            String strImplObjIDPolicy = (String) mImplementedItem.get(DomainObject.SELECT_POLICY);
            DomainObject domImplObj = DomainObject.newInstance(context, strImplObjID);
            String strOriginalObjectsActiveCO = getCOObjectConnectedToOriginalObject(context, domImplObj, strImplObjIDPolicy);

            if (UIUtil.isNotNullAndNotEmpty(strOriginalObjectsActiveCO) && strOriginalObjectsActiveCO.equalsIgnoreCase(strCOObjectId)) {

                mImplementedItem.put(TigerConstants.TABLE_SETTING_DISABLESELECTION, "true");
            }
            mplFinal.add(mImplementedItem);
        }
        return mplFinal;
    }

    // PCM TIGTK-5226 | 15/03/17 : VP : START
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @summary This method is used to display the Other Comment field on the Change Order WebForm
     */
    public String displayOtherCommentField(Context context, String[] args) throws Exception {
        String strCommentField = DomainConstants.EMPTY_STRING;

        try {
            HashMap programMap = JPO.unpackArgs(args);

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String strMode = (String) requestMap.get("mode");
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            HashMap fieldMap = (HashMap) programMap.get("fieldMap");
            HashMap settings = (HashMap) fieldMap.get("settings");

            // Findbug Issue correction start
            // Date: 22/03/2017
            // By: Asha G.

            // Findbug Issue correction End
            String strAdminType = (String) settings.get("Admin Type");

            String strAttributeValue = DomainConstants.EMPTY_STRING;

            String strObjectId = (String) paramMap.get("objectId");

            if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
                DomainObject domCurrentObject = DomainObject.newInstance(context, strObjectId);
                String strAttributeName = PropertyUtil.getSchemaProperty(context, strAdminType);
                strAttributeValue = domCurrentObject.getInfo(context, DomainObject.getAttributeSelect(strAttributeName));

            }

            StringBuilder sbFieldValue = new StringBuilder();

            if (UIUtil.isNullOrEmpty(strMode) || strMode.equalsIgnoreCase("edit") || strMode.equalsIgnoreCase("create")) {
                // TIGTK-11657 : 27/11/17 : TS : START
                sbFieldValue.append("<textarea name=\"PSS_OtherComments\" contenteditable=\"true\" id= \"calc_PSS_OtherComments\" size=\"20\">");
                sbFieldValue.append(XSSUtil.encodeForHTML(context, strAttributeValue));
                sbFieldValue.append("</textarea>");
                // TIGTK-11657 : 27/11/17 : TS : END
            } else {
                sbFieldValue.append(strAttributeValue);
            }

            strCommentField = sbFieldValue.toString();
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in displayOtherCommentField: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return strCommentField;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @summary This method is used to set or update the value of the Other Comment captured for the Other Purpose of Release for Change Order
     */
    public void setOtherCommentInformation(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String objectId = (String) paramMap.get("objectId");
            String newValue = (String) paramMap.get("New Value");

            HashMap fieldMap = (HashMap) programMap.get("fieldMap");
            HashMap settings = (HashMap) fieldMap.get("settings");
            String adminType = (String) settings.get("Admin Type");

            String strAttributeName = PropertyUtil.getSchemaProperty(context, adminType);

            DomainObject domCurrentObject = DomainObject.newInstance(context, objectId);
            domCurrentObject.setAttributeValue(context, strAttributeName, newValue);
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setOtherCommentInformation: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
    }

    // PCM TIGTK-5226 | 15/03/17 : VP : END

    // PCM TIGTK-4891: 14/03/2017 : KWagh : START
    /**
     * Description: Access function for displaying the edit commands on CA Affected Item / Implemented Item page.
     * @author KWagh
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */

    public boolean hasEditAccessOnAffectedItems(Context context, String args[]) throws Exception {
        boolean bResult = true;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            StringList slAICurrent = new StringList();

            String strCAID = (String) programMap.get("objectId");
            DomainObject domCA = new DomainObject(strCAID);

            StringList slAISelectable = new StringList();
            slAISelectable.add(DomainConstants.SELECT_ID);
            slAISelectable.add(DomainConstants.SELECT_CURRENT);

            MapList mlAIList = domCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, "*", slAISelectable, null, false, true, (short) 1, null, null, 0);

            if (!mlAIList.isEmpty()) {
                int mlCount = mlAIList.size();
                for (int i = 0; i < mlCount; i++) {

                    Map mAI = (Map) mlAIList.get(i);

                    String strCurrent = (String) mAI.get(DomainConstants.SELECT_CURRENT);
                    slAICurrent.add(strCurrent);

                }
            }

            if (slAICurrent.size() > 0) {
                if ((slAICurrent.contains(TigerConstants.STATE_PART_RELEASE)) || (slAICurrent.contains(TigerConstants.STATE_RELEASED_CAD_OBJECT))) {
                    bResult = false;
                }
            }

        } catch (Exception e) {

            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in hasEditAccessOnAffectedItems: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return bResult;
    }

    // PCM TIGTK-4891: 14/03/2017 : KWagh : End

    // PCM TIGTK-4891: 14/03/2017 : KWagh : START
    /**
     * Description: Access function for displaying the edit commands on CA Affected Item / Implemented Item page.
     * @author KWagh
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */

    public boolean hasEditImplementedItems(Context context, String args[]) throws Exception {
        boolean bResult = true;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            StringList slIMPCurrent = new StringList();
            // Findbug Issue correction start
            // Date: 22/03/2017
            // By: Asha G.

            // Findbug Issue correction End
			// Start : TIGTK-14264 : Sub TIGTK-18220
            String strID = (String) programMap.get("objectId");
            DomainObject domObj = new DomainObject(strID);
            // END : TIGTK-14264 : Sub TIGTK-18220

            StringList slAISelectable = new StringList();
            slAISelectable.add(DomainConstants.SELECT_ID);
            slAISelectable.add(DomainConstants.SELECT_CURRENT);

            // Start : TIGTK-14264 : Sub TIGTK-18220
            MapList mlIMPlist = new MapList();
            if (domObj.isKindOf(context, TigerConstants.TYPE_CHANGEACTION))
                mlIMPlist = domObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, "*", slAISelectable, null, false, true, (short) 1, null, null, 0);
            // END : TIGTK-14264 : Sub TIGTK-18220

            if (!mlIMPlist.isEmpty()) {
                int mlCount = mlIMPlist.size();
                for (int i = 0; i < mlCount; i++) {

                    Map mIMP = (Map) mlIMPlist.get(i);

                    String strCurrent = (String) mIMP.get(DomainConstants.SELECT_CURRENT);
                    slIMPCurrent.add(strCurrent);

                }
            }

            if (slIMPCurrent.size() > 0) {
                if ((slIMPCurrent.contains(TigerConstants.STATE_PART_RELEASE)) || (slIMPCurrent.contains(TigerConstants.STATE_RELEASED_CAD_OBJECT))) {
                    bResult = false;
                }
            }
            // Start : TIGTK-14264 : Sub TIGTK-18220
            if (bResult) {
                bResult = hasCOCAEditAccess(context, args);
                if (!bResult) {
                    bResult = checkAccessForImplementedItemAssignee(context, domObj);
                }
            }
            // END : TIGTK-14264 : Sub TIGTK-18220
        } catch (Exception e) {

            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in hasEditImplementedItems: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return bResult;
    }

    // PCM TIGTK-4891: 14/03/2017 : KWagh : End

    // PCM TIGTK-5397: 23/03/2017 : Harika Varanasi : START
    /**
     * setTranserFromCRFlagForCOChangeAssessment.
     * @author Harika Varanasi : SteepGraph
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */

    public boolean setTranserFromCRFlagForCOChangeAssessment(Context context, String args[]) throws Exception {
        boolean bResult = true;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map mapCAParts = (HashMap) programMap.get("objIDCAMap");
            int mpSize = mapCAParts.size();
            if ((!mapCAParts.isEmpty()) && mpSize > 0) {
                String strPartId = DomainConstants.EMPTY_STRING;
                String strCAId = DomainConstants.EMPTY_STRING;
                String strChangeAffeRelId = DomainConstants.EMPTY_STRING;
                Set<Map.Entry<String, String>> set = mapCAParts.entrySet();
                for (Map.Entry<String, String> me : set) {
                    strPartId = me.getKey();
                    strCAId = me.getValue();

                    // TIGTK-16710 : 12-08-2018 : START
                    DomainObject domAI = new DomainObject(strPartId);

                    StringList slObjSelect = new StringList();
                    slObjSelect.add(DomainConstants.SELECT_ID);

                    StringList slRelSelect = new StringList();
                    slRelSelect.add(DomainRelationship.SELECT_RELATIONSHIP_ID);

                    StringBuffer sbObjectWhere = new StringBuffer();
                    sbObjectWhere.append("(current!='");
                    sbObjectWhere.append(TigerConstants.STATE_CHANGEACTION_COMPLETE);
                    sbObjectWhere.append("' && current!='");
                    sbObjectWhere.append(TigerConstants.STATE_CHANGEACTION_CANCELLED);
                    sbObjectWhere.append("' && current!='");
                    sbObjectWhere.append(TigerConstants.STATE_CHANGEACTION_ONHOLD);
                    sbObjectWhere.append("')");

                    MapList mlChangeAction = domAI.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, ChangeConstants.TYPE_CHANGE_ACTION, slObjSelect, slRelSelect, true,
                            false, (short) 1, sbObjectWhere.toString(), null, 0);
                    if (mlChangeAction != null && !mlChangeAction.isEmpty()) {
                        for (int intIA = 0; intIA < mlChangeAction.size(); intIA++) {
                            Map mpAIInfoMap = (Map) mlChangeAction.get(intIA);
                            String strRleationshipId = (String) mpAIInfoMap.get(DomainRelationship.SELECT_RELATIONSHIP_ID);
                            DomainRelationship drObject = new DomainRelationship(strRleationshipId);
                            // TIGTK-14264
                            String strCA = (String) mpAIInfoMap.get(DomainConstants.SELECT_ID);
                            if (UIUtil.isNotNullAndNotEmpty(strCA)) {
                                // DomainObject dmObj = new DomainObject(strCA);
                                BusinessObject busObj = new BusinessObject(strCA);
                                drObject.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REASON_FOR_CHANGE, busObj.getDescription(context));
                            }
                            setAttributeOnRelationship(context, strRleationshipId);
                        }

                    }
                    // TIGTK-16710 : 12-08-2018 : END
                }
            }

        } catch (Exception e) {

            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in setTranserFromCRFlagForCOChangeAssessment: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return bResult;
    }

    // PCM TIGTK-5397: 23/03/2017 : Harika Varanasi : End

    // PCM TIGTK-5118: 06/04/2017 : KWagh : START

    /**
     * @author Kwagh
     * @param context
     * @param args
     * @throws Exception
     *             This trigger method is used to connect symmetrical part to the same Change order object which is connected to the original part.
     */
    public void connectSymmetricalPartToContextCO(Context context, String args[]) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {

            String strFromObjectId = args[0];
            String strToObjectId = args[1];

            int nSize;
            String strCAID = DomainConstants.EMPTY_STRING;

            DomainObject domFromSideObj = new DomainObject(strFromObjectId);
            DomainObject domToSideObj = new DomainObject(strToObjectId);

            StringList slAISelectable = new StringList();
            slAISelectable.add(DomainConstants.SELECT_ID);

            DomainObject domCA = new DomainObject();
            // PCM TIGTK-5118: 06/04/2017 : KWagh : START
            String strBusWhereclause = "current ==" + TigerConstants.STATE_CHANGEACTION_PENDING;
            // PCM TIGTK-5118: 06/04/2017 : KWagh : End
            MapList mlAffectedchangeActionFromSide = domFromSideObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, ChangeConstants.TYPE_CHANGE_ACTION, slAISelectable,
                    null, true, false, (short) 1, strBusWhereclause, null, 0);

            MapList mlAffectedchangeActionToSide = domToSideObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, ChangeConstants.TYPE_CHANGE_ACTION, slAISelectable, null,
                    true, false, (short) 1, strBusWhereclause, null, 0);

            MapList mlImplementedchangeActionFromSide = domFromSideObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, ChangeConstants.TYPE_CHANGE_ACTION, slAISelectable,
                    null, true, false, (short) 1, strBusWhereclause, null, 0);

            MapList mlImplementedchangeActionToSide = domToSideObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, ChangeConstants.TYPE_CHANGE_ACTION, slAISelectable, null,
                    true, false, (short) 1, strBusWhereclause, null, 0);

            // For Affected Items

            if (mlAffectedchangeActionFromSide.isEmpty() && !mlAffectedchangeActionToSide.isEmpty()) {
                // From side part is not connected to Active Change Action
                nSize = mlAffectedchangeActionToSide.size();
                for (int cnt = 0; cnt < nSize; cnt++) {
                    Map mCAMap = (Map) mlAffectedchangeActionToSide.get(cnt);
                    strCAID = (String) mCAMap.get(DomainConstants.SELECT_ID);

                    domCA.setId(strCAID);

                    this.connectAIToCO(context, domCA, domFromSideObj);

                }
            } else if (mlAffectedchangeActionToSide.isEmpty() && !mlAffectedchangeActionFromSide.isEmpty()) {
                // To side part is not connected to Active Change Action
                nSize = mlAffectedchangeActionFromSide.size();
                for (int i = 0; i < nSize; i++) {
                    Map mpCAMap = (Map) mlAffectedchangeActionFromSide.get(i);
                    strCAID = (String) mpCAMap.get(DomainConstants.SELECT_ID);
                    domCA.setId(strCAID);

                    this.connectAIToCO(context, domCA, domToSideObj);

                }
            }

            // For Implemented Items

            if (mlImplementedchangeActionFromSide.isEmpty() && !mlImplementedchangeActionToSide.isEmpty()) {
                // From side part is not connected to Active Change Action
                nSize = mlImplementedchangeActionToSide.size();
                for (int cnt = 0; cnt < nSize; cnt++) {
                    Map mCAMap = (Map) mlImplementedchangeActionToSide.get(cnt);
                    strCAID = (String) mCAMap.get(DomainConstants.SELECT_ID);

                    domCA.setId(strCAID);
                    this.connectImplObjToCO(context, domCA, domFromSideObj);

                }
            } else if (mlImplementedchangeActionToSide.isEmpty() && !mlImplementedchangeActionFromSide.isEmpty()) {
                // To side part is not connected to Active Change Action
                nSize = mlImplementedchangeActionFromSide.size();
                for (int i = 0; i < nSize; i++) {
                    Map mpCAMap = (Map) mlImplementedchangeActionFromSide.get(i);
                    strCAID = (String) mpCAMap.get(DomainConstants.SELECT_ID);
                    domCA.setId(strCAID);

                    this.connectImplObjToCO(context, domCA, domToSideObj);

                }

            }

        } catch (Exception e) {

            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectSymmetricalPartToContextCO: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    // PCM TIGTK-5118: 06/04/2017 : KWagh : End
    // PCM TIGTK-5118: 06/04/2017 : KWagh : START
    /**
     * @author KWagh
     * @param context
     * @param CA
     *            object , AI Object
     * @throws Exception
     *             This method is used to connect CA object and AI Object by relationship "Chamhe Affected Item"
     */
    public void connectAIToCO(Context context, DomainObject domCA, DomainObject domAI) throws Exception {

        try {
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

            DomainRelationship domRelChangeAffectedItem = DomainRelationship.connect(context, domCA, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, domAI);
            domRelChangeAffectedItem.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_RELEASE);
            // TIGTK-14264
            domRelChangeAffectedItem.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REASON_FOR_CHANGE, domCA.getDescription(context));
            domRelChangeAffectedItem.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TRANSFERFROMCRFLAG, "No");

            ContextUtil.popContext(context);
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectAIToCO: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;

        }
    }

    // PCM TIGTK-5118: 06/04/2017 : KWagh : End
    // PCM TIGTK-5118: 06/04/2017 : KWagh : START
    /**
     * @author KWagh
     * @param context
     * @param CA
     *            object , Implemented Object
     * @throws Exception
     *             This method is used to connect CA object and Implemented Object by relationship "Implemented Items"
     */
    public void connectImplObjToCO(Context context, DomainObject domCA, DomainObject domImpl) throws Exception {
        try {
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

            DomainRelationship domRelImplementedItem = DomainRelationship.connect(context, domCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, domImpl);
            domRelImplementedItem.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_RELEASE);

            ContextUtil.popContext(context);
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectImplObjToCO: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;

        }
    }

    // PCM TIGTK-5118: 06/04/2017 : KWagh : End

    // PCM TIGTK-5946: 28/04/2017 : KWagh : start

    /**
     * @author Kwagh This method is call from Promote action trigger when CO is promoted to Completion state. Once Affected Item object is released which is added in CO as requested Change "For
     *         Replacement". System should replace the old Object with new Object in all assemblies. This method is used for "Part" objects . Code is Pending for CAD objects it will applied in future.
     * @param context
     * @param args
     * @throws Exception
     */
    public void replaceItemsOnCOCompletion(Context context, String[] args) throws Exception {
        if (args == null || args.length < 1) {
            throw (new IllegalArgumentException());
        }
        try {

            PropertyUtil.setGlobalRPEValue(context, "APPROVE_CA_TASK_FOR_REPLACE", "True");
            String strCOId = args[0];
            String strAffectedItemID = DomainConstants.EMPTY_STRING;
            String strRequestedChangeAttrValue = DomainConstants.EMPTY_STRING;
            String strType = DomainConstants.EMPTY_STRING;
            String strPolicy = DomainConstants.EMPTY_STRING;
            String strCAID = DomainConstants.EMPTY_STRING;

            StringList slCurrentCOAffectedItems = new StringList();
            DomainObject domCO = new DomainObject(strCOId);

            StringList slObjectSle = new StringList(3);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_TYPE);
            slObjectSle.addElement(DomainConstants.SELECT_POLICY);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

            StringList slRelSle = new StringList(1);
            slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            slRelSle.addElement(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

            MapList mlConnectedCAs = domCO.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, slObjectSle, slRelSle, false, true, (short) 1,
                    null, null, 0);
            if (!mlConnectedCAs.isEmpty()) {
                Iterator itrCA = mlConnectedCAs.iterator();

                while (itrCA.hasNext()) {

                    Map mCAObj = (Map) itrCA.next();
                    strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    DomainObject domobjCA = new DomainObject(strCAID);
                    StringBuffer sbWhereForAI = new StringBuffer();
                    sbWhereForAI.append("current == '" + TigerConstants.STATE_OBSOLETE + "'");

                    MapList mlConnectedAffectedItem = domobjCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, "*", slObjectSle, slRelSle, false, true, (short) 1,
                            sbWhereForAI.toString(), null, 0);
                    slCurrentCOAffectedItems.addAll(getStringListFromMaplist(mlConnectedAffectedItem, DomainConstants.SELECT_ID));
                }
            }

            if (!mlConnectedCAs.isEmpty()) {
                Iterator itrCA = mlConnectedCAs.iterator();

                while (itrCA.hasNext()) {

                    Map mCAObj = (Map) itrCA.next();
                    strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    DomainObject domobjCA = new DomainObject(strCAID);
                    StringBuffer sbWhereForAI = new StringBuffer();
                    sbWhereForAI.append("current == '" + TigerConstants.STATE_OBSOLETE + "'");

                    MapList mlConnectedAffectedItem = domobjCA.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, "*", slObjectSle, slRelSle, false, true, (short) 1,
                            sbWhereForAI.toString(), null, 0);
                    Iterator itrAI = mlConnectedAffectedItem.iterator();

                    while ((itrAI.hasNext())) {
                        // TIGTK-9721:22/09/2017:Start
                        String strReplacedID = DomainConstants.EMPTY_STRING;
                        // TIGTK-9721:22/09/2017:End
                        Map mAffectedItemObj = (Map) itrAI.next();
                        strAffectedItemID = (String) mAffectedItemObj.get(DomainConstants.SELECT_ID);
                        strRequestedChangeAttrValue = (String) mAffectedItemObj.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                        strType = (String) mAffectedItemObj.get(DomainConstants.SELECT_TYPE);
                        strPolicy = (String) mAffectedItemObj.get(DomainConstants.SELECT_POLICY);

                        DomainObject domAffectedItem = new DomainObject(strAffectedItemID);

                        // If Requested Change is For Replacement Type of Object is Part
                        if ((strRequestedChangeAttrValue.equalsIgnoreCase(ChangeConstants.FOR_OBSOLESCENCE)) && (strType.equalsIgnoreCase(TigerConstants.TYPE_PART))
                                && !(strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART))) {

                            // Get list of Replaced Objects
                            StringBuffer sbwhereForDerived = new StringBuffer();
                            sbwhereForDerived.append("current == '" + TigerConstants.STATE_PART_RELEASE + "'");

                            MapList mlReplacedObjList = domAffectedItem.getRelatedObjects(context, TigerConstants.RELATIONSHIP_DERIVED, // relationship pattern
                                    DomainConstants.TYPE_PART, // object pattern
                                    new StringList(DomainConstants.SELECT_ID), // object selects
                                    slRelSle, // relationship selects
                                    false, // to direction
                                    true, // from direction
                                    (short) 1, // recursion level
                                    sbwhereForDerived.toString(), // object where clause
                                    null, 0); // relationship where clause

                            if (!mlReplacedObjList.isEmpty()) {

                                Iterator itrReplacedObj = mlReplacedObjList.iterator();
                                while (itrReplacedObj.hasNext()) {
                                    Map mReplacedObj = (Map) itrReplacedObj.next();

                                    strReplacedID = (String) mReplacedObj.get(DomainConstants.SELECT_ID);
                                }
                            }
                            DomainObject domReplacedObj = new DomainObject();

                            if (UIUtil.isNotNullAndNotEmpty(strReplacedID)) {
                                domReplacedObj.setId(strReplacedID);

                                // Get list of Parent Objects of Original Object
                                StringBuffer sbWhere = new StringBuffer();

                                // TIGTK-17234 : Start
                                StringList objectSelectables = new StringList(DomainConstants.SELECT_ID);
                                objectSelectables.add(DomainConstants.SELECT_CURRENT);
                                // TIGTK-17234 : End

                                sbWhere.append("(revision == 'last')");
                                MapList mlImmediatePartParentOIDsList = domAffectedItem.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, // relationship pattern
                                        DomainConstants.TYPE_PART, // object pattern
                                        objectSelectables, // object selects
                                        slRelSle, // relationship selects
                                        true, // to direction
                                        false, // from direction
                                        (short) 1, // recursion level
                                        sbWhere.toString(), // object where clause
                                        null, 0); // relationship where clause

                                if (!mlImmediatePartParentOIDsList.isEmpty()) {
                                    String strParentRelID = DomainConstants.EMPTY_STRING;
                                    String strParentState = DomainConstants.EMPTY_STRING;
                                    Iterator itrParentList = mlImmediatePartParentOIDsList.iterator();
                                    while (itrParentList.hasNext()) {
                                        Map mapImmediatePartParentInfo = (Map) itrParentList.next();

                                        strParentRelID = (String) mapImmediatePartParentInfo.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                                        // TIGTK-17234 : Start
                                        strParentState = (String) mapImmediatePartParentInfo.get(DomainConstants.SELECT_CURRENT);
                                        if (!TigerConstants.STATE_PART_OBSOLETE.equalsIgnoreCase(strParentState)) {
                                            DomainRelationship.setToObject(context, strParentRelID, domReplacedObj);
                                        }
                                        // TIGTK-17234 : End
                                    }
                                }
                            }

                        } else if ((strRequestedChangeAttrValue.equalsIgnoreCase(ChangeConstants.FOR_OBSOLESCENCE)) && (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT))) {
                            // If Requested Change is For Replacement Type of Object is CAD
                            // Get list of Replaced Objects
                            StringBuffer sbwhereForDerived = new StringBuffer();
                            sbwhereForDerived.append("current == '" + TigerConstants.STATE_RELEASED_CAD_OBJECT + "'");

                            MapList mlReplacedObjList = domAffectedItem.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD, // relationship pattern
                                    "*", // object pattern
                                    new StringList(DomainConstants.SELECT_ID), // object selects
                                    slRelSle, // relationship selects
                                    false, // to direction
                                    true, // from direction
                                    (short) 1, // recursion level
                                    sbwhereForDerived.toString(), // object where clause
                                    null, 0); // relationship where clause

                            if (!mlReplacedObjList.isEmpty()) {

                                Iterator itrReplacedObj = mlReplacedObjList.iterator();
                                while (itrReplacedObj.hasNext()) {
                                    Map mReplacedObj = (Map) itrReplacedObj.next();

                                    strReplacedID = (String) mReplacedObj.get(DomainConstants.SELECT_ID);
                                }

                                DomainObject domReplacedObj = new DomainObject();

                                if (UIUtil.isNotNullAndNotEmpty(strReplacedID)) {
                                    domReplacedObj.setId(strReplacedID);

                                    // Get list of Parent Objects of Original Object
                                    StringBuffer sbWhere = new StringBuffer();
                                    sbWhere.append("(revision == 'last')");
                                    MapList mlImmediatePartParentOIDsList = domAffectedItem.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CADSUBCOMPONENT, // relationship pattern
                                            "*", // object pattern
                                            new StringList(DomainConstants.SELECT_ID), // object selects
                                            slRelSle, // relationship selects
                                            true, // to direction
                                            false, // from direction
                                            (short) 1, // recursion level
                                            sbWhere.toString(), // object where clause
                                            null, 0); // relationship where clause

                                    if (!mlImmediatePartParentOIDsList.isEmpty()) {
                                        String strParentRelID = DomainConstants.EMPTY_STRING;
                                        Iterator itrParentList = mlImmediatePartParentOIDsList.iterator();
                                        while (itrParentList.hasNext()) {
                                            Map mapImmediatePartParentInfo = (Map) itrParentList.next();

                                            strParentRelID = (String) mapImmediatePartParentInfo.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                                            // TIGTK-17234-mkakade : START
                                            if (!slCurrentCOAffectedItems.contains(strParentRelID)) {
                                                // TIGTK-17234-mkakade : END
                                                DomainRelationship.setToObject(context, strParentRelID, domReplacedObj);
                                            }

                                        }

                                    }

                                    // Get list of Child Objects of Original Object

                                    MapList mlImmediatePartChildOIDsList = domAffectedItem.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CADSUBCOMPONENT, // relationship pattern
                                            "*", // object pattern
                                            new StringList(DomainConstants.SELECT_ID), // object selects
                                            slRelSle, // relationship selects
                                            false, // to direction
                                            true, // from direction
                                            (short) 1, // recursion level
                                            sbWhere.toString(), // object where clause
                                            null, 0); // relationship where clause

                                    if (!mlImmediatePartChildOIDsList.isEmpty()) {
                                        String strChildRelID = DomainConstants.EMPTY_STRING;
                                        Iterator itrChild = mlImmediatePartChildOIDsList.iterator();
                                        while (itrChild.hasNext()) {
                                            Map mpChildInfo = (Map) itrChild.next();

                                            strChildRelID = (String) mpChildInfo.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                                            // TIGTK-17234-mkakade : START
                                            if (!slCurrentCOAffectedItems.contains(strChildRelID)) {
                                                // TIGTK-17234-mkakade : END
                                                DomainRelationship.setFromObject(context, strChildRelID, domReplacedObj);
                                            }
                                        }

                                    }

                                    // Replace the original CAD object which is connected to Part with "Part Specification" relationship
                                    StringList slOBJsel = new StringList();
                                    slOBJsel.add(DomainConstants.SELECT_ID);
                                    slOBJsel.add(DomainConstants.SELECT_CURRENT);
                                    MapList mlConnectedParts = domAffectedItem.getRelatedObjects(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship pattern
                                            DomainConstants.TYPE_PART, // object pattern
                                            slOBJsel, // object selects
                                            slRelSle, // relationship selects
                                            true, // to direction
                                            false, // from direction
                                            (short) 1, // recursion level
                                            null, // object where clause
                                            null, 0); // relationship where clause

                                    if (!mlConnectedParts.isEmpty()) {
                                        String strPartRelID = DomainConstants.EMPTY_STRING;
                                        String strPartCurrent = DomainConstants.EMPTY_STRING;
                                        String strPartID = DomainConstants.EMPTY_STRING;
                                        Iterator itrPart = mlConnectedParts.iterator();
                                        while (itrPart.hasNext()) {
                                            Map mpPart = (Map) itrPart.next();

                                            strPartRelID = (String) mpPart.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                                            strPartCurrent = (String) mpPart.get(DomainConstants.SELECT_CURRENT);

                                            if (TigerConstants.STATE_OBSOLETE.equalsIgnoreCase(strPartCurrent)) {

                                                strPartID = (String) mpPart.get(DomainConstants.SELECT_ID);
                                                DomainObject domPart = new DomainObject(strPartID);
                                                if (domPart.isLastRevision(context)) {

                                                    String strNewPartId = domPart.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_DERIVED + "].to.id");
                                                    if (UIUtil.isNotNullAndNotEmpty(strNewPartId)) {
                                                        DomainObject domNew = DomainObject.newInstance(context, strNewPartId);
                                                        String strNewObjPolicy = domNew.getInfo(context, DomainConstants.SELECT_POLICY);
                                                        if (!strNewObjPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CANCELPART)) {

                                                            // PCM TIGTK-5946: 3/05/2017 : KWagh : START
                                                            String strRelIdExists = MqlUtil.mqlCommand(context, "print bus " + strNewPartId + " select from["
                                                                    + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "| to.id == '" + strReplacedID + "'].id dump", false, false);

                                                            if (UIUtil.isNullOrEmpty(strRelIdExists)) {
                                                                DomainRelationship.connect(context, domNew, ChangeConstants.RELATIONSHIP_PART_SPECIFICATION, domReplacedObj);
                                                            }
                                                        }
                                                    }

                                                } else {
                                                    BusinessObject busPart = domPart.getLastRevision(context);
                                                    DomainObject domLatest = new DomainObject(busPart);
                                                    StringList slObjSelect = new StringList();
                                                    slObjSelect.add(DomainConstants.SELECT_POLICY);
                                                    slObjSelect.add(DomainConstants.SELECT_CURRENT);
                                                    Map sMapInfo = domLatest.getInfo(context, slObjSelect);
                                                    String strLatestObjPolicy = (String) sMapInfo.get(DomainConstants.SELECT_POLICY);
                                                    String strLatestObjCurrent = (String) sMapInfo.get(DomainConstants.SELECT_CURRENT);

                                                    if (!strLatestObjPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CANCELPART)
                                                            && !strLatestObjCurrent.equalsIgnoreCase(TigerConstants.STATE_OBSOLETE)) {

                                                        String strRelIdExists = MqlUtil.mqlCommand(context, "print bus " + (String) domLatest.getObjectId() + " select from["
                                                                + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "| to.id == '" + strReplacedID + "'].id dump", false, false);
                                                        if (UIUtil.isNullOrEmpty(strRelIdExists)) {
                                                            DomainRelationship.connect(context, domLatest, ChangeConstants.RELATIONSHIP_PART_SPECIFICATION, domReplacedObj);
                                                        }
                                                        // PCM TIGTK-5946: 3/04/2017 : KWagh : End
                                                    }
                                                }
                                            }

                                        }

                                    }

                                }

                            }
                            // PCM TIGTK-6201: 7/04/2017 : Rutuja Ekatpure : End
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in replaceItemsOnCOCompletion: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        } finally {
            PropertyUtil.setGlobalRPEValue(context, "APPROVE_CA_TASK_FOR_REPLACE", "");
        }
    }

    // PCM TIGTK-5946: 28/04/2017 : KWagh : End

    private void StringList() {
        // TODO Auto-generated method stub

    }

    /***
     * This method used to check symmetrical Part connected to the context part is connected to CA or not.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkSymmetricalPartIsConnectedToCA(Context context, String args[]) throws Exception {

        String strMessage = DomainConstants.EMPTY_STRING;
        String strCAId = DomainConstants.EMPTY_STRING;
        String strSymmetricalPartID = DomainConstants.EMPTY_STRING;

        int isreturn = 0;

        try {
            String strPartID = args[0];
            DomainObject domPart = new DomainObject(strPartID);

            StringList slObjectSle = new StringList(3);
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);

            // Get Realated symmetrical part of the part
            MapList mlSymmetricalPartObject = domPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, // relationship pattern
                    DomainConstants.TYPE_PART, // object pattern
                    slObjectSle, // object selects
                    null, // relationship selects
                    true, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, 0); // relationship where clause

            if (!mlSymmetricalPartObject.isEmpty()) {

                Iterator itrSymmetrical = mlSymmetricalPartObject.iterator();
                while (itrSymmetrical.hasNext()) {
                    Map mSymmtericalPart = (Map) itrSymmetrical.next();

                    strSymmetricalPartID = (String) mSymmtericalPart.get(DomainConstants.SELECT_ID);
                    if (UIUtil.isNotNullAndNotEmpty(strSymmetricalPartID)) {
                        DomainObject domSymmetricalObj = new DomainObject(strSymmetricalPartID);

                        // Modified by Suchit G.for TIGTK-8677 on 28/06/2017 to change state to "In Approval" from "In Review": START
                        String strBusWhereclause = "current ==" + TigerConstants.STATE_CHANGEACTION_PENDING + "|| current =='" + TigerConstants.STATE_CHANGEACTION_INWORK + "'|| current =='"
                                + TigerConstants.STATE_CHANGEACTION_INAPPROVAL + "'";
                        // Modified by Suchit G.for TIGTK-8677 on 28/06/2017 to change state to "In Approval" from "In Review": END

                        // Added by Suchit G. for TIGTK-8677 on 20/06/2017: START
                        String strRelPattern = ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM;
                        // Added by Suchit G. for TIGTK-8677 on 20/06/2017: END

                        // Modified by Suchit G. for TIGTK-8677 on 20/06/2017 to add strRelPattern in place of ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM: START
                        MapList mlChangeAction = domSymmetricalObj.getRelatedObjects(context, strRelPattern, ChangeConstants.TYPE_CHANGE_ACTION, slObjectSle, null, true, false, (short) 1,
                                strBusWhereclause, null, 0);
                        // Modified by Suchit G. for TIGTK-8677 on 20/06/2017 to add strRelPattern in place of ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM: END

                        if (mlChangeAction.isEmpty()) {

                            strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                                    "PSS_EnterpriseChangeMgt.Alert.SymmetricalPartNotConnectedToCA");
                            String strPartName = domPart.getInfo(context, DomainConstants.SELECT_NAME);
                            strMessage = strMessage.replace("${names}", strPartName);
                            MqlUtil.mqlCommand(context, "notice $1", strMessage);
                            isreturn = 1;

                        }

                    }
                }

            }

        } catch (Exception e) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in replaceItemsOnCOCompletion: ", e);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
        return isreturn;
    }

    /**
     * Description : This method is copied from enoECMChangeOrderBase. PCM : TIGTK-6158 : 07/04/2017 : AB Method invoked from the Delete Trigger of rel "Change Affected Item". This method is Used to
     * delete the CA Object, if the Affected Item is the Last one to be removed.
     * @param context
     * @param args
     *            CA Object and Affected Item Object
     * @return integer
     * @throws Exception
     */
    public int deleteCAOnLastAffectedItem(Context context, String args[]) throws Exception {
        try {
            // TIGTK-6843:Phase-2.0:PKH:Start
            // Added for CO cancel and cancel implemented item functionality - for disconnect Affected item CA is Deleted as per trigger method.Prevent from Deletion CA- added below code.
            String strCancelStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_COCancel");
            if (UIUtil.isNotNullAndNotEmpty(strCancelStatus) && strCancelStatus.equals("True")) {
                return 0;
            }
            // TIGTK-6843:Phase-2.0:PKH:END
            String strCAObjectId = args[0];
            PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null);
            StringList objSelects = new StringList();
            String strRelselect = "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id";
            objSelects.addElement(SELECT_CURRENT);
            objSelects.addElement(strRelselect);
            DomainObject doObj = DomainObject.newInstance(context, strCAObjectId);
            Map map = doObj.getInfo(context, objSelects);
            StringList slRemainingAffectedItems = changeUtil.getStringListFromMap(context, map, strRelselect);
            if (slRemainingAffectedItems.isEmpty()) {
                String current = (String) map.get(SELECT_CURRENT);
                String strCAPolicyName = ChangeConstants.POLICY_CHANGE_ACTION;
                String strCancelledState = PropertyUtil.getSchemaProperty(context, ChangeConstants.POLICY, strCAPolicyName, ChangeConstants.STATE_SYMBOLIC_CANCELLED);
                if (!strCancelledState.equalsIgnoreCase(current))
                    doObj.deleteObject(context);
            }
        } catch (Exception Ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in deleteCAOnLastAffectedItem: ", Ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
        return 0;
    }

    /**
     * This method used in check trigger on connection.If Collabrative space of both Object is different then connection block by this trigger. PCM : TIGTK-6963 : 21/04/2017 : AB
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkCollabrativeSpaceOfChangeObject(Context context, String args[]) throws Exception {
        int intReturn = 0;
        try {
            String strFromObjectID = args[0];
            String strToObjectID = args[1];
            DomainObject domFromObject = DomainObject.newInstance(context, strFromObjectID);
            DomainObject domToObject = DomainObject.newInstance(context, strToObjectID);
            String strFromCollabSpace = (String) domFromObject.getInfo(context, "project");
            String strToCollabSpace = (String) domToObject.getInfo(context, "project");
            String strType = (String) domFromObject.getInfo(context, DomainConstants.SELECT_TYPE);
            // if COllobrativeSpace of from side Object and to side Object is not same then block connection
            if (!strFromCollabSpace.equalsIgnoreCase(strToCollabSpace)) {
                // PCM:TIGTK-6965 : VB : START
                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEORDER)) {
                    String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                            "PSS_EnterpriseChangeMgt.Notice.CollobrativeSpaceIsNotSameOfManufacturingChangeObject");
                    intReturn = 1;
                    throw new Exception(strMessage);
                    // PCM:TIGTK-6965 : VB : END
                } else {
                    String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                            "PSS_EnterpriseChangeMgt.Notice.CollobrativeSpaceIsNotSameOfChangeObject");
                    emxContextUtilBase_mxJPO.mqlNotice(context, strMessage);
                    intReturn = 1;
                }
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Notice.CollobrativeSpaceIsNotSameOfChangeObject");
                intReturn = 1;
                throw new Exception(strMessage);

            }
        } catch (Exception e) {
            emxContextUtilBase_mxJPO.mqlNotice(context, e.getMessage());
            logger.error("Error in checkCotextPersonIsProgramProjectMemberOrNot: ", e);
        }
        return intReturn;

    }

    // PCM TIGTK-6952 : PSE : 24-04-2017 : START
    /**
     * Method used to check access for adding CO to Part and CAD
     * @param context
     * @param args
     * @return -- boolean -- Return flag which states true or false
     * @author -- Priyanka Salunke
     * @since -- 24/April/2017
     * @throws Exception
     */
    public boolean checkAccessForAddCOsToPartAndCAD(Context context, String[] args) throws Exception {
        boolean bReturnStatus = false;
        try {
            // PCM TIGTK-9392| 22/08/17 :KWagh : Start
            HashMap programMap = JPO.unpackArgs(args);
            String strOjectId = (String) programMap.get("objectId");

            DomainObject domObject = DomainObject.newInstance(context, strOjectId);

            StringList slSelectable = new StringList();
            slSelectable.add(DomainConstants.SELECT_CURRENT);
            slSelectable.add(DomainConstants.SELECT_POLICY);
            slSelectable.add(DomainConstants.SELECT_ID);

            Map mObjectInfoMap = domObject.getInfo(context, slSelectable);
            String strCurrentState = (String) mObjectInfoMap.get(DomainConstants.SELECT_CURRENT);
            String strPolicy = (String) mObjectInfoMap.get(DomainConstants.SELECT_POLICY);

            ArrayList alAllowedStateList = new ArrayList();
            alAllowedStateList.add(TigerConstants.STATE_CHANGEACTION_INWORK);
            alAllowedStateList.add(TigerConstants.STATE_PART_REVIEW);
            alAllowedStateList.add(TigerConstants.STATE_PART_RELEASE);
            alAllowedStateList.add(TigerConstants.STATE_RELEASED_CAD_OBJECT);

            if (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_ECPART) || strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART)
                    || ((strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT) || strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_Legacy_CAD))
                            && alAllowedStateList.contains(strCurrentState))) {

                boolean bConnectedCA = domObject.hasRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, false);

                if (!bConnectedCA) {
                    bReturnStatus = true;
                } else {

                    String sWhere = "(current != '" + TigerConstants.STATE_CHANGEACTION_CANCELLED + "') &&  (current != '" + TigerConstants.STATE_CHANGEACTION_COMPLETE + "')";

                    MapList mlConnectedCA = domObject.getRelatedObjects(context, // context
                            ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, // relationship pattern
                            ChangeConstants.TYPE_CHANGE_ACTION, // object pattern
                            slSelectable, // object selects
                            new StringList(DomainConstants.SELECT_RELATIONSHIP_ID), // relationship selects
                            true, // to direction
                            false, // from direction
                            (short) 0, // recursion level
                            sWhere, // object where clause
                            null, // relationship where clause
                            (short) 0);

                    if ((mlConnectedCA.isEmpty())) {
                        bReturnStatus = true;
                    }
                }
            }
            // PCM TIGTK-9392| 22/08/17 :KWagh : End
        } catch (Exception ex) {
            logger.error("Exception in  PSS_enoECMChangeOrder : checkAccessForAddExistingCOsToPart() method  ", ex);
        }

        return bReturnStatus;
    }

    // PCM TIGTK-6952 : PSE : 24-04-2017 :End

    // PCM TIGTK-6720 : VB : 28-04-2017 :START

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             Custom Check trigger method to check when the Change Order is promoted from Prepare to In Work state
     */
    public int checkChangeOrderOwnerInPreparetoInWork(Context context, String args[]) throws Exception {
        try {
            String objectId = args[0];
            if (UIUtil.isNotNullAndNotEmpty(objectId)) {
                BusinessObject busChangeOrder = new BusinessObject(objectId);
                User strOwner = busChangeOrder.getOwner(context);
                User strObjectOrganization = busChangeOrder.getOrganizationOwner(context);
                User strObjectProject = busChangeOrder.getProjectOwner(context);
                String strLoggedUser = context.getUser();
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Alert.OwnerCONoLoggedUserOrSecurityContext");
                String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strLoggedUser);
                if (UIUtil.isNotNullAndNotEmpty(strLoggedUserSecurityContext)) {
                    String strLoggerUserProject = (strLoggedUserSecurityContext.split("[.]")[2]);
                    String strLoggerUserOrganization = (strLoggedUserSecurityContext.split("[.]")[1]);
                    String strLoggerUserRole = (strLoggedUserSecurityContext.split("[.]")[0]);
                    // TIGTK-16727 : 11-08-2018 : START
                    String strAllowedRoles = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.PromoteCO.AllowedRoles");
                    StringList slAllowedRoles = FrameworkUtil.split(strAllowedRoles, ",");
                    if ((strLoggedUser.equalsIgnoreCase(strOwner.toString())) && (slAllowedRoles.contains(strLoggerUserRole.trim()))) {
                        // TIGTK-16727 : 11-08-2018 : END
                        if (!(strObjectOrganization.toString().equalsIgnoreCase(strLoggerUserOrganization) && strObjectProject.toString().equalsIgnoreCase(strLoggerUserProject))) {
                            MqlUtil.mqlCommand(context, "notice $1", strMessage);
                            return 1;
                        }
                    } else {
                        MqlUtil.mqlCommand(context, "notice $1", strMessage);
                        return 1;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in  PSS_enoECMChangeOrder : checkChangeOrderOwnerInPreparetoInWork() method  ", e);
        }
        return 0;
    }

    // PCM TIGTK-6720 : VB : 28-04-2017 :END

    // PCM TIGTK-6795: 05/03/2017 : PTE : START

    /**
     * This method is called from PSS_ECMFullSearchPostProcess.jsp. It makes a check whether Affected Items are connected to Active CO or not based on Affected Item Object(CAD, Part, Standard). It
     * returns the final list of affected Items to be connected with CO Object.
     * @param context
     * @param selectedItemsList
     * @return
     * @throws Exception
     */
    public StringList getCOAffectedItems(Context context, String args[]) throws Exception {
        StringList slApprovedItemsList = new StringList();
        try {
            Map programMap = (HashMap) JPO.unpackArgs(args);
            StringList selectedItemsList = (StringList) programMap.get("selectedItemsList");
            DomainObject domAffectedItem = new DomainObject();
            int slSize = selectedItemsList.size();
            String strPolicy = "";
            String strObjId = "";
            StringList slselectStmt = new StringList();
            slselectStmt.addElement(DomainConstants.SELECT_POLICY);
            slselectStmt.addElement("to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
            slselectStmt.addElement("to[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].from.policy");

            PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null);
            for (int i = 0; i < slSize; i++) {
                strObjId = (String) selectedItemsList.get(i);

                domAffectedItem.setId(strObjId);
                // domAffectedItem = new DomainObject(strObjId);

                Map mapAffectedItemDetails = domAffectedItem.getInfo(context, slselectStmt);
                strPolicy = (String) mapAffectedItemDetails.get(DomainConstants.SELECT_POLICY);
                // Find Bug Issue Dead Local Store : Priyanka Salunke : 28-Feb-2017
                StringList slSpecsConnectedPartPolicy = changeUtil.getStringListFromMap(context, mapAffectedItemDetails, "to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
                // PCM : TIGTK-7126 : 02/05/2017 : AB : START
                StringList slChartedDrawingConnectedPartPolicy = changeUtil.getStringListFromMap(context, mapAffectedItemDetails,
                        "to[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].from.policy");
                slSpecsConnectedPartPolicy.add(slChartedDrawingConnectedPartPolicy);
                // PCM : TIGTK-7126 : 02/05/2017 : AB : END
                // PCM : TIGTK-4119 : 02/02/2017 : AB : START
                // PCM TIGTK-4273 | 06/02/2017 : AB : START
                String strRelPattern = ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM;

                String selectCoIsActive = new StringBuilder("evaluate[(to[").append(strRelPattern).append("].from.to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION)
                        .append("].from.current smatchlist \"Prepare,In Work,In Approval\" \",\")]").toString();

                boolean isConnectedActiveCO = Boolean.valueOf((String) domAffectedItem.getInfo(context, selectCoIsActive));
                // PCM TIGTK-4273 | 06/02/2017 : AB : START
                // PCM : TIGTK-4119 : 02/02/2017 : AB : END

                if (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_STANDARDPART) && !isConnectedActiveCO) {
                    // In case of Standard part , get the CAD+Charted Drawing connected to it which is not connected to dev part and another active CO
                    slApprovedItemsList.add(strObjId);
                    slApprovedItemsList.addAll(getCADAndDrawingObjectsList(context, strObjId));
                }

                else if (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_ECPART) && !isConnectedActiveCO) {
                    // In case of EC part; Inner Case 1, get the CAD+Charted Drawing connected to it which is not connected to dev part and another active CO
                    slApprovedItemsList.addAll(getCADAndDrawingObjectsList(context, strObjId));
                    // In case of EC part; Inner Case 2, find the symmetrical part connected to it CAD+ Charted Drawing connected to symmetricla part
                    // and which is not connected to dev part and another active CO
                    // getting the Symmetrical parts :
                    StringList slSymmetricParts = getSymmetricalPart(context, new String[] { strObjId });
                    StringList slFinalSymmetricalist = new StringList();
                    slFinalSymmetricalist.addAll(slSymmetricParts);
                    // Find Bug modifications: 23/03/2017 : KWagh : START
                    if (!slSymmetricParts.isEmpty()) {
                        // Find Bug modifications: 23/03/2017 : KWagh : End
                        if (slSymmetricParts.contains(strObjId)) {
                            slFinalSymmetricalist.remove(strObjId);
                        }
                        if (slFinalSymmetricalist.size() > 0) {
                            String symmPartId = (String) slFinalSymmetricalist.get(0);

                            StringList symmConnectedObjList = getCADAndDrawingObjectsList(context, symmPartId);

                            slApprovedItemsList.addAll(symmConnectedObjList);
                        }
                        slApprovedItemsList.addAll(slSymmetricParts);

                    }

                } else if ((strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT) || strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_Legacy_CAD)) && !isConnectedActiveCO) {
                    // In case of PSS_CAD_Object or Pss_legacy_CAD for charted drawing , then if it is not connected to Active CO
                    // PCM : TIGTK-9042 : 13/07/2017 : AB : START
                    slApprovedItemsList.addElement(strObjId);
                    // PCM : TIGTK-9042 : 13/07/2017 : AB : END
                } // PCM:TIGTK-4060 | 15/3/2017 |Rutuja Ekatpure :Start

            }

        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getCOAffectedItems: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return getUniqueIdList(slApprovedItemsList);
    }

    // PCM TIGTK-6795: 05/03/2017 : PTE : END

    // PCM TIGTK-6795: 05/03/2017 : PTE : START

    /**
     * This method perform the trigger action while Change Order promoted Prepare to In Work
     * @param context
     * @param DomainRelationship
     * @return
     * @throws Exception
     */

    public void getTriggerAction(Context context, DomainRelationship domRel) {
        boolean isEnbeldTrigger = false;
        try {
            if (UIUtil.isNotNullAndNotEmpty(domRel.toString())) {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "User Agent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                MqlUtil.mqlCommand(context, "trigger off");
                domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_RELEASE);
                isEnbeldTrigger = true;
            }
        } catch (Exception ex) {
            logger.error("Error in getTriggerAction: ", ex);
        } finally {
            if (isEnbeldTrigger) {
                try {
                    MqlUtil.mqlCommand(context, "trigger on");
                    ContextUtil.popContext(context);
                } catch (Exception e) {
                    logger.error("Error in getTriggerAction: ", e);
                }
            }

        }
    }

    // PCM : Trigger on MBOM to check MBOM connected components state before promote MBOM : 13/6/2017 : Rutuja Ekatpure : Start
    /***
     * method used to check state of MBOM's connected components is ahead of Parent MBOM state
     * @param context
     * @param args
     *            args0 MBOM id args1 MBOM current state
     * @return int value 0 if pass 1 if fail
     * @throws Exception
     */
    public int checkStateOfChildIsAheadOfParent(Context context, String args[]) throws Exception {

        if (args == null || args.length < 1) {
            throw new IllegalArgumentException();
        }
        int isReturn = 0;

        try {
            String strMBOMId = args[0];
            String strMBOMCurrentState = args[1];

            String strPropertyKey = "PSSEnterpriseChangeMgt.Alert.ChildIsNotAheadOfParent." + strMBOMCurrentState.replaceAll(" ", "_");
            String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strPropertyKey);

            DomainObject domMBOMObj = new DomainObject(strMBOMId);
            StringList slMBOMStates = domMBOMObj.getInfoList(context, DomainConstants.SELECT_STATES);

            StringList slObjectSle = new StringList(2);
            slObjectSle.addElement(DomainConstants.SELECT_CURRENT);
            slObjectSle.addElement(DomainConstants.SELECT_POLICY);

            // get expand structure of MBOM till 1 level
            MapList mlExpandMBOM = PSS_FRCMBOMProg_mxJPO.getExpandMBOM(context, strMBOMId, 1, null, null, null, new StringList(), slObjectSle);

            for (Object objMBOM : mlExpandMBOM) {
                Map mChildMBOM = (Map) objMBOM;
                String strChildMBOMPolicy = (String) mChildMBOM.get(DomainConstants.SELECT_POLICY);
                String strChildMBOMState = (String) mChildMBOM.get(DomainConstants.SELECT_CURRENT);
                // check policy of child component is MBOM or not
                if (TigerConstants.POLICY_PSS_MBOM.equalsIgnoreCase(strChildMBOMPolicy)) {

                    int intChildStateIndex = slMBOMStates.indexOf(strChildMBOMState);
                    int intParentStateIndex = slMBOMStates.indexOf(strMBOMCurrentState);

                    // check child state is ahead of parent state or not and child is in cancelled state or obsolete state or not
                    if (!(strChildMBOMState.equalsIgnoreCase(TigerConstants.STATE_PSS_MBOM_CANCELLED) || strChildMBOMState.equalsIgnoreCase(TigerConstants.STATE_OBSOLETE))
                            && (intParentStateIndex >= intChildStateIndex)) {
                        // this condition added for handling the case where parent and child are in different MCA ,MCO having 2 MCA,child is in Approved state,now complete MCA containing parent in
                        // revew state.
                        // in this case on MCA completion parent promoted to approved state and on last MCA prmotion this method called for MCO promotion action,both parent child is at same state,so
                        // this become problem.
                        if (!(strMBOMCurrentState.equalsIgnoreCase(TigerConstants.STATE_MBOM_APPROVED) && strChildMBOMState.equalsIgnoreCase(TigerConstants.STATE_MBOM_APPROVED))) {
                            isReturn = 1;
                            strMessage = strMessage.replace("$<name>", domMBOMObj.getInfo(context, DomainConstants.SELECT_NAME));
                            MqlUtil.mqlCommand(context, "notice $1", strMessage);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in checkStateOfChildIsAheadOfParent: ", e);
        }
        return isReturn;
    }

    // PCM : Trigger on MBOM to check MBOM connected components state before promote MBOM : 13/6/2017 : Rutuja Ekatpure :End

    /**
     * This method is used to check that CAD is connected to only Development Part or no, return false if it is connected to only devPart. PCM : TIGTK-9042 & TIGTK-9060 : AB
     * @param context
     * @param args
     *            DomainObject of CAD Part
     * @return
     * @throws Exception
     */
    public Boolean checkCADValidationForAddIntoCO(Context context, String args[]) throws Exception {
        Boolean bolReturn = true;
        try {
            // get the DomainObject of CAD part
            Map programMap = (HashMap) JPO.unpackArgs(args);
            DomainObject domAffectedItem = (DomainObject) programMap.get("domCADItem");

            // get the charted Drawing and Part specification's policy
            String strRelationshipSelects = DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "," + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING;
            StringList slObjectSle = new StringList(DomainConstants.SELECT_POLICY);
            MapList mlConnectedPart = domAffectedItem.getRelatedObjects(context, strRelationshipSelects, DomainConstants.QUERY_WILDCARD, slObjectSle, new StringList(), true, false, (short) 1, null,
                    null, (short) 0);

            StringList slConnectedPartPolicy = new StringList();
            for (int l = 0; l < mlConnectedPart.size(); l++) {
                if (mlConnectedPart.size() != 0) {
                    Map mPartObj = (Map) mlConnectedPart.get(l);
                    String strPartPolicy = (String) mPartObj.get(DomainConstants.SELECT_POLICY);
                    slConnectedPartPolicy.add(strPartPolicy);
                }
            }

            // If the CAD is connected to only Development Part then return validation false
            if (!slConnectedPartPolicy.isEmpty()) {
                if (!(slConnectedPartPolicy.contains(TigerConstants.POLICY_PSS_ECPART) || slConnectedPartPolicy.contains(TigerConstants.POLICY_STANDARDPART))) {
                    bolReturn = false;
                }
            }
            return bolReturn;
        } catch (Exception ex) {
            logger.error("Error in checkCADValidationForAddIntoCO: ", ex);
            throw ex;
        }
    }

    // PCM[2.0]:[JIRA][TIGTK-6856]:Trigger on CA to create the Approval Route for CA :31/7/2017:Pranjali Tupe:START

    /**
     * This method is used to create the Approval Route for CA. PCM : TIGTK-6856
     * @param context
     * @param args
     *            ObjectId of CA and Attribute values for Route.
     * @return
     * @throws Exception
     */
    public void createApprovalListRouteForCA(Context context, String[] args) throws Exception {
        // Get CA Object Id and Route Attribute Values from args[]
        boolean isContextPushed = false;
        logger.debug("PSS_enoECMChangeOrder:createApprovalListRouteForCA:START");
        try {
            String strObjectId = args[0];
            String strRouteBaseState = args[1];
            String strRouteBasePurpose = args[2];
            String strRouteScope = args[3];
            String strRouteCompletionAction = args[4];
            String strRouteAutoStopOnRejection = args[5];
            String strRouteStateCondition = args[6];

            DomainObject domCA = DomainObject.newInstance(context, strObjectId);
            String strCAType = domCA.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE);
            String strCAOwner = domCA.getOwner(context).getName();
            // PCM TIGTK-10012: 28/10/2017 : START
            String strCAAssignee = domCA.getInfo(context, "from[" + ChangeConstants.RELATIONSHIP_TECHNICAL_ASSIGNEE + "].to.name");
            // PCM TIGTK-10012: 28/10/2017 : End
            String strCOId = domCA.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");

            DomainObject domCOObj = DomainObject.newInstance(context, strCOId);
            String strPurposeOfRelease = domCOObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);
            String strProgramProjectOID = domCOObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            StringList busSelect = new StringList();

            if ("Part".equals(strCAType)) {
                String strRouteTemplateObjectValue = getProgramProjectRouteTemplateValue(context, strProgramProjectOID, strPurposeOfRelease);
                busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='" + strRouteTemplateObjectValue
                        + "'].to.id");
            } else if ("Standard".equals(strCAType)) {
                busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                        + TigerConstants.RANGE_APPROVAL_LIST_FORSTANDARDPARTSONCO + "'].to.id");
            } else if ("CAD".equals(strCAType)) {
                busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                        + TigerConstants.RANGE_APPROVAL_LIST_FORCADONCO + "'].to.id");
            } else {
                String strCATypeMesssage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.NOValidCAType");
                throw new Exception(strCATypeMesssage);

            }

            DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
            // Get Route Template From ProgramProject
            Map mapRouteTemplateDetails = domProgramProjectObject.getInfo(context, busSelect);
            String strRouteTemplateId = (String) mapRouteTemplateDetails.get("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.id");

            // Create the Route Template Object using strRouteTemplateId
            Route routeObject = new Route(); // Create new Route Object.

            Hashtable mpRelAttributeMap = new Hashtable();
            mpRelAttributeMap.put("routeBasePurpose", strRouteBasePurpose);
            mpRelAttributeMap.put("Scope", strRouteScope);
            mpRelAttributeMap.put("State Condition", strRouteStateCondition);
            mpRelAttributeMap.put(DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE, strRouteBaseState);
            mpRelAttributeMap.put(strObjectId, strRouteBaseState);

            ContextUtil.pushContext(context, strCAOwner, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isContextPushed = true;
            Map routeMap = Route.createRouteWithScope(context, strObjectId, null, null, true, mpRelAttributeMap);
            ContextUtil.popContext(context);
            isContextPushed = false;

            String strRouteId = (String) routeMap.get("routeId");
            routeObject.setId(strRouteId);

            DomainObject domObjRoute = DomainObject.newInstance(context, strRouteId);
            // Set Attribute value of Route
            Map mpRelNewAttributeMap = new Hashtable();
            mpRelNewAttributeMap.put("Route Completion Action", strRouteCompletionAction);
            mpRelNewAttributeMap.put("Auto Stop On Rejection", strRouteAutoStopOnRejection);
            mpRelNewAttributeMap.put("Route Base Purpose", strRouteBasePurpose);

            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            isContextPushed = true;
            // PCM TIGTK-10012: 28/10/2017 : START

            if (!strCAOwner.equalsIgnoreCase(strCAAssignee)) {
                strCAOwner = strCAAssignee;
            }

            domObjRoute.setOwner(context, strCAOwner);

            // PCM TIGTK-10012: 28/10/2017 : End
            routeObject.setAttributeValues(context, mpRelNewAttributeMap);
            ContextUtil.popContext(context);
            isContextPushed = false;

            // If the route template id is not null then connect the route to the route template
            if (strRouteTemplateId != null && !strRouteTemplateId.equalsIgnoreCase("null") && !strRouteTemplateId.equalsIgnoreCase(DomainConstants.EMPTY_STRING)) {

                ContextUtil.pushContext(context, strCAOwner, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                isContextPushed = true;
                routeObject.connectTemplate(context, strRouteTemplateId);
                routeObject.addMembersFromTemplate(context, strRouteTemplateId);
                ContextUtil.popContext(context);
                isContextPushed = false;
            }

            // Set attribute of Route Node relationship,
            final String SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE = "attribute[" + DomainObject.ATTRIBUTE_SCHEDULED_COMPLETION_DATE + "]";

            Pattern typePattern = new Pattern(DomainConstants.TYPE_ROUTE_TASK_USER);
            typePattern.addPattern(DomainConstants.TYPE_PERSON);

            StringList slRelSelect = new StringList(1);
            slRelSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            slRelSelect.add(SELECT_ATTRIBUTE_SCHEDULED_COMPLETION_DATE);

            MapList mapListRouteNodeRel = domObjRoute.getRelatedObjects(context, DomainObject.RELATIONSHIP_ROUTE_NODE, typePattern.getPattern(), null, slRelSelect, false, true, (short) 1, null, null);

            if (mapListRouteNodeRel != null && !mapListRouteNodeRel.isEmpty()) {
                Iterator itrRouteNodeList = mapListRouteNodeRel.iterator();

                while (itrRouteNodeList.hasNext()) {
                    Map connectIdMap = (Map) itrRouteNodeList.next();
                    String strRelId = (String) connectIdMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    // Creating relationship object.
                    DomainRelationship domRelRouteNode = DomainRelationship.newInstance(context, strRelId);
                    // setting attribute (scheduled completion date) values for that relationship.
                    HashMap attributes = new HashMap();
                    attributes.put(DomainConstants.ATTRIBUTE_DUEDATE_OFFSET, "7");
                    attributes.put(DomainConstants.ATTRIBUTE_DATE_OFFSET_FROM, "Route Start Date");
                    attributes.put(DomainConstants.ATTRIBUTE_ASSIGNEE_SET_DUEDATE, "No");

                    domRelRouteNode.setAttributeValues(context, attributes);
                }
            }
            logger.debug("PSS_enoECMChangeOrder:createApprovalListRouteForCA:END");
        } catch (Exception ex) {
            // Abort transaction.
            logger.error("PSS_enoECMChangeOrder:createApprovalListRouteForCA:ERROR ", ex);
            throw ex;
        } finally {
            if (isContextPushed) {
                ContextUtil.popContext(context);
            }
        }
    }
    // PCM[2.0]:[JIRA][TIGTK-6856]:Trigger on CA to create the Approval Route for CA :31/7/2017:Pranjali Tupe:END

    public String getProgramProjectRouteTemplateValue(Context context, String strProgramProjectId, String strCOPurposeOfRelease) {

        String strPSSRouteTemplateTypeValue = DomainConstants.EMPTY_STRING;
        logger.debug("PSS_enoECMChangeOrder:getProgramProjectRouteTemplateValue:START");
        try {
            // Get the value based on CO Purpose Of Release
            if (mapProgramProjectRouteTemplateMapping.containsKey(strCOPurposeOfRelease)) {
                strPSSRouteTemplateTypeValue = (String) mapProgramProjectRouteTemplateMapping.get(strCOPurposeOfRelease);
            }
            logger.debug("PSS_enoECMChangeOrder:getProgramProjectRouteTemplateValue:END");
        } catch (Exception ex) {
            logger.error("PSS_enoECMChangeOrder:getProgramProjectRouteTemplateValue:ERROR ", ex);
            throw ex;
        }
        return strPSSRouteTemplateTypeValue;
    }

    // PCM[2.0]:[JIRA][TIGTK-6856]:Trigger on CA to start/resume the Approval Route for CA :31/7/2017:Pranjali Tupe:START
    /**
     * This method is used to start/resume the Approval Route for CA.
     * @param context
     * @param args
     *            ObjectId of CA.
     * @return
     * @throws Exception
     */
    public void startApprovalListRoutesForCA(Context context, String[] args) throws Exception {

        logger.debug("PSS_enoECMChangeOrder:startApprovalListRoutesForCA:START");
        try {
            // Get CA ObjectId object from args[].
            String strCAObjectId = args[0];
            // Create DomainObject of Issue from Issue ID.
            DomainObject domChangeAction = DomainObject.newInstance(context, strCAObjectId);
            // Get CA Planned END date
            String strPlannedEndDate = domChangeAction.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_PLANNEDENDDATE + "]");
            // Get all Routes connected to Change Action.
            StringList objectSelect = new StringList();
            objectSelect.add(DomainConstants.SELECT_ID);
            // TIGTK-5945: START
            objectSelect.addElement("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_STATUS + "]");
            // TIGTK-5945: END
            // PCM TIGTK-9673: 4/10/2017 : KWagh : START
            StringList slRelSelect = new StringList();
            slRelSelect.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

            MapList mlConnectedRoute = domChangeAction.getRelatedObjects(context, DomainConstants.RELATIONSHIP_OBJECT_ROUTE, DomainConstants.TYPE_ROUTE, objectSelect, slRelSelect, false, true,
                    (short) 1, null, null, 0);
            // Iterate over Route MapList and Check for Route Status. If Status is "Stopped" OR "Finished" resume the Route else Start the Route.
            Iterator itrRoutes = mlConnectedRoute.iterator();
            while (itrRoutes.hasNext()) {
                Map mpRoute = (Map) itrRoutes.next();
                // TIGTK-5945: START
                String sRouteStatus = (String) mpRoute.get("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_STATUS + "]");
                // TIGTK-5945: END

                String strObjRouteRelID = (String) mpRoute.get(DomainRelationship.SELECT_RELATIONSHIP_ID);
                DomainRelationship domRelObjectRoute = DomainRelationship.newInstance(context, strObjRouteRelID);
                String strRouteBaseState = domRelObjectRoute.getAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_BASE_STATE);

                String sRouteID = (String) mpRoute.get(DomainConstants.SELECT_ID);

                Route route = new Route(sRouteID);
                if (strRouteBaseState.equalsIgnoreCase("state_InApproval")) {
                    if ("Stopped".equals(sRouteStatus) || "Finished".equals(sRouteStatus)) {
                        // Restarting the already connected Route
                        route.resume(context);
                    } else {
                        route.setAttributeValue(context, DomainConstants.ATTRIBUTE_ROUTE_STATUS, "Started");
                        route.setState(context, Route.STATE_ROUTE_IN_PROCESS);
                    }
                }
                StringList slInboxTaskList = route.getInfoList(context, "to[" + DomainConstants.RELATIONSHIP_ROUTE_TASK + "].from.id");

                if (slInboxTaskList.size() > 0) {
                    Iterator itrInboxTask = slInboxTaskList.iterator();
                    while (itrInboxTask.hasNext()) {
                        String strInboxTaskbjId = (String) itrInboxTask.next();
                        DomainObject domITTask = new DomainObject(strInboxTaskbjId);
                        domITTask.setAttributeValue(context, ATTRIBUTE_SCHEDULED_COMPLETION_DATE, strPlannedEndDate);
                    }
                }

                // PCM TIGTK-9673: 4/10/2017 : KWagh : End
            }
            logger.debug("PSS_enoECMChangeOrder:startApprovalListRoutesForCA:END");
        } catch (Exception ex) {
            // Abort transaction.
            logger.error("PSS_enoECMChangeOrder:startApprovalListRoutesForCA:ERROR ", ex);
            throw ex;
        }
    }
    // PCM[2.0]:[JIRA][TIGTK-6856]:Trigger on CA to start/resume the Approval Route for CA :31/7/2017:Pranjali Tupe:END

    /**
     * PCM : TIGTK-9060 : 28/07/2017 : AB.This Method is used to get the list of CAD which is connected to only Development Part
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Map getCADListIfItIsConnectedToOnlyDevPart(Context context, String args[]) throws Exception {
        Map mapNotValidCAD = new HashMap();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            Map mapObjectRelID = (Map) programMap.get("mapObjIdRelId");

            StringList slObjectSelects = new StringList(DomainConstants.SELECT_POLICY);
            slObjectSelects.add(DomainConstants.SELECT_NAME);

            if (!mapObjectRelID.isEmpty()) {
                StringList slObjectID = (StringList) mapObjectRelID.get("ObjId");
                int intSize = slObjectID.size();

                for (int i = 0; i < intSize; i++) {
                    String strAffectedItemID = (String) slObjectID.get(i);
                    DomainObject domAffectedItem = DomainObject.newInstance(context, strAffectedItemID);

                    Map mapItemInfo = (Map) domAffectedItem.getInfo(context, slObjectSelects);
                    String strItemPolicy = (String) mapItemInfo.get(DomainConstants.SELECT_POLICY);

                    if (TigerConstants.POLICY_PSS_CADOBJECT.equals(strItemPolicy) || TigerConstants.POLICY_PSS_Legacy_CAD.equals(strItemPolicy)) {
                        // get the charted Drawing and Part specification's policy
                        String strRelationshipSelects = DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "," + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING;
                        StringList slObjectSle = new StringList(DomainConstants.SELECT_POLICY);
                        MapList mlConnectedPart = domAffectedItem.getRelatedObjects(context, strRelationshipSelects, DomainConstants.QUERY_WILDCARD, slObjectSle, new StringList(), true, false,
                                (short) 1, null, null, (short) 0);

                        StringList slConnectedPartPolicy = new StringList();
                        for (int l = 0; l < mlConnectedPart.size(); l++) {
                            if (mlConnectedPart.size() != 0) {
                                Map mPartObj = (Map) mlConnectedPart.get(l);
                                String strPartPolicy = (String) mPartObj.get(DomainConstants.SELECT_POLICY);
                                slConnectedPartPolicy.add(strPartPolicy);
                            }
                        }

                        // If the CAD is connected to only Development Part then return validation false
                        if (!slConnectedPartPolicy.isEmpty()) {
                            if (!(slConnectedPartPolicy.contains(TigerConstants.POLICY_PSS_ECPART) || slConnectedPartPolicy.contains(TigerConstants.POLICY_STANDARDPART))) {
                                String strItemname = (String) mapItemInfo.get(DomainConstants.SELECT_NAME);
                                mapNotValidCAD.put(strAffectedItemID, strItemname);
                            }
                        }
                    }

                }
            }

        } catch (Exception Ex) {
            logger.error("Error in getCADListIfItIsConnectedToOnlyDevPart: ", Ex);
            throw Ex;
        }
        return mapNotValidCAD;
    }

    // PCM TIGTK-10239: 4/10/2017 : KWagh : START

    /**
     * @author Gets Approval tasks and shows on Properties page of Change.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public String getApprovalTasksOnChange(Context context, String[] args) throws Exception {
        // PCM TIGTK-3856: 3/1/2017 : KWagh : START
        // XSSOK
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) programMap.get(ChangeConstants.REQUEST_MAP);
        String objectId = (String) requestMap.get(ChangeConstants.OBJECT_ID);
        changeOrderUI.setId(objectId);
        MapList taskmpList = changeOrderUI.getCurrentAssignedTasksOnObject(context);
        MapList taskMapList = this.getValidTaskList(context, taskmpList, objectId);
        // PCM TIGTK-3856: 3/1/2017 : KWagh : End

        // For export to CSV
        String exportFormat = null;
        boolean exportToExcel = false;
        if (requestMap.containsKey("reportFormat")) {
            exportFormat = (String) requestMap.get("reportFormat");
        }
        if ("CSV".equals(exportFormat)) {
            exportToExcel = true;
        }

        String taskTreeActualLink = getTaskTreeHref(context, requestMap);
        String taskApprovalActualLink = getTaskApprovalHref(context);

        String taskTreeTranslatedLink = "";
        String taskApprovalTranslatedLink = "";

        Map mapObjectInfo;
        String strName;
        String strInfoType;
        String taskObjectId;
        StringBuffer returnHTMLBuffer = new StringBuffer(100);
        if (taskMapList.size() > 0) {
            if (!exportToExcel) {
                returnHTMLBuffer.append("<div><table><tr><td class=\"object\">");
                returnHTMLBuffer.append(EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Message.ApprovalRequired"));
                returnHTMLBuffer.append("</td></tr><br/><tr><td>");
                returnHTMLBuffer.append(EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Message.ApprovalMessage"));
                returnHTMLBuffer.append("</td></tr></table></div>");
            } else {
                returnHTMLBuffer.append(EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Message.ApprovalRequired"));
                returnHTMLBuffer.append("\n");
                returnHTMLBuffer.append(EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Message.ApprovalMessage"));
                returnHTMLBuffer.append("\n\n");

            }
        }
        // Do for each object
        for (Iterator itrObjects = taskMapList.iterator(); itrObjects.hasNext();) {
            mapObjectInfo = (Map) itrObjects.next();
            strName = (String) mapObjectInfo.get("name");
            strInfoType = (String) mapObjectInfo.get("infoType");

            if (INFO_TYPE_ACTIVATED_TASK.equals(strInfoType)) {

                if (!exportToExcel) {
                    taskObjectId = (String) mapObjectInfo.get(ChangeConstants.ID);
                    taskTreeTranslatedLink = FrameworkUtil.findAndReplace(taskTreeActualLink, "${OBJECT_ID}", taskObjectId);
                    taskTreeTranslatedLink = FrameworkUtil.findAndReplace(taskTreeTranslatedLink, "${NAME}", strName);
                    returnHTMLBuffer.append("<div><table><tr><td>");

                    returnHTMLBuffer.append(EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Label.TaskAssigned"))
                            .append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;").append(taskTreeTranslatedLink);
                    returnHTMLBuffer.append("</td></tr><br/>\n\n\n\n\n<td>");

                    taskApprovalTranslatedLink = FrameworkUtil.findAndReplace(taskApprovalActualLink, "${TASK_ID}", taskObjectId);
                    taskApprovalTranslatedLink = FrameworkUtil.findAndReplace(taskApprovalTranslatedLink, "${OBJECT_ID}", (String) mapObjectInfo.get("parentObjectId"));
                    taskApprovalTranslatedLink = FrameworkUtil.findAndReplace(taskApprovalTranslatedLink, "${STATE}", (String) mapObjectInfo.get("parentObjectState"));

                    returnHTMLBuffer.append(EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Label.ApprovalStatus"))
                            .append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;").append(taskApprovalTranslatedLink);
                    returnHTMLBuffer.append("</td></tr></table></div>");
                } else {
                    returnHTMLBuffer.append(EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Label.TaskAssigned"))
                            .append("      ").append(strName);
                    returnHTMLBuffer.append("\n");
                    returnHTMLBuffer.append(EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Label.ApprovalStatus"))
                            .append("      ")
                            .append(EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Command.AwaitingApproval"));

                }
            }
        }

        return returnHTMLBuffer.toString();
    }
    // PCM TIGTK-10239: 4/10/2017 : KWagh : END

    // PCM TIGTK-10239: 4/10/2017 : KWagh : START
    // PCM TIGTK-3856: 3/1/2017 : KWagh : START
    /**
     * @author Kwagh
     * @param context
     * @param objectId
     * @return
     * @throws Exception
     */
    public MapList getValidTaskList(Context context, MapList mlTableData, String objectId) throws Exception {

        MapList retunrList = new MapList();
        if (mlTableData.size() > 0) {

            int tableDataSize = mlTableData.size();
            for (int i = 0; i < tableDataSize; i++) {

                Map mTemp = (Map) mlTableData.get(i);
                String strRouteTaskUser = (String) mTemp.get("attribute[Route Task User]");
                String taskId = (String) mTemp.get("id");

                DomainObject domTask = DomainObject.newInstance(context, taskId);
                String strUserName = domTask.getInfo(context, "from[" + DomainConstants.RELATIONSHIP_PROJECT_TASK + "].to.name");

                if (UIUtil.isNotNullAndNotEmpty(strRouteTaskUser) && strRouteTaskUser.startsWith("role_")) {

                    String strContextUser = context.getUser();
                    String strMQL = "print person  '" + strContextUser + "' select isassigned[" + PropertyUtil.getSchemaProperty(context, strRouteTaskUser) + "] dump";

                    boolean isToBeAccepted = "true".equalsIgnoreCase(MqlUtil.mqlCommand(context, strMQL, true));
                    if (isToBeAccepted) {
                        boolean isPersonProgamMember = checkPersonProgramMember(context, strContextUser, objectId);

                        // PCM : TIGTK-4434 : 02/03/2017 : AB : START
                        DomainObject domObj = new DomainObject(objectId);
                        String strType = (String) domObj.getInfo(context, DomainConstants.SELECT_TYPE);

                        // Check IF Route is on MCA then current user is Plant=Member or not
                        if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {
                            boolean isPersonPlantMember = this.checkPersonPlantMember(context, strContextUser, objectId);
                            if (isPersonProgamMember && isPersonPlantMember && !strUserName.contains("auto"))
                                retunrList.add(mTemp);
                        } else {
                            if (isPersonProgamMember && !strUserName.contains("auto")) {
                                retunrList.add(mTemp);
                            }
                        }
                        // PCM : TIGTK-4434 : 02/03/2017 : AB : END
                    }
                } else {
                    retunrList.add(mTemp);
                }
            }

        }

        return retunrList;
    }

    // PCM TIGTK-3856: 3/1/2017 : KWagh : End
    // PCM TIGTK-10239: 4/10/2017 : KWagh : END

    // PCM TIGTK-10239: 4/10/2017 : KWagh : START
    // PCM TIGTK-3856: 3/1/2017 : KWagh : START
    /**
     * @author kwagh
     * @param context
     * @param userName
     * @param objectId
     * @return
     * @throws Exception
     */
    public boolean checkPersonProgramMember(Context context, String userName, String objectId) throws Exception {

        StringList slProgramprojectMemberList = new StringList();
        boolean isvalid = false;

        String strSelectableforCR = "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name";
        String strSelectableforCA = "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from["
                + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name";
        String strSelectableforMCA = "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from["
                + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name";

        DomainObject domChangeObject = new DomainObject(objectId);

        StringList slRequestedInfo = new StringList();
        slRequestedInfo.add(DomainObject.SELECT_TYPE);

        Map requestMap = domChangeObject.getInfo(context, slRequestedInfo);

        // Get Type of Change Object
        String strObjType = (String) requestMap.get(DomainConstants.SELECT_TYPE);

        if (strObjType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION) || strObjType.equalsIgnoreCase(TigerConstants.TYPE_CHANGEACTION)
                || strObjType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {

            // Get List of Program-Project members for MCA
            if (strObjType.equalsIgnoreCase(TigerConstants.TYPE_PSS_MANUFACTURINGCHANGEACTION)) {

                slProgramprojectMemberList = domChangeObject.getInfoList(context, strSelectableforMCA);

            } // Get List of Program-Project members for CA
            else if (strObjType.equalsIgnoreCase(TigerConstants.TYPE_CHANGEACTION)) {

                slProgramprojectMemberList = domChangeObject.getInfoList(context, strSelectableforCA);

            } // Get List of Program-Project members for CR
            else if (strObjType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {

                slProgramprojectMemberList = domChangeObject.getInfoList(context, strSelectableforCR);
            }

            if (slProgramprojectMemberList.contains(userName)) {

                isvalid = true;
            }
        } else
            isvalid = true;

        return isvalid;
    }

    /**
     * Check If current user is Member of connected Plant and also member from PlantMembers connected To Program-Project of MCO PCM : TIGTK-4434 : 02/03/2017 : AB
     * @param context
     * @param mlTableData
     * @param objectId
     * @return
     * @throws Exception
     */
    public boolean checkPersonPlantMember(Context context, String userName, String objectId) throws Exception {
        boolean isvalid = false;
        try {
            DomainObject domMCA = new DomainObject(objectId);

            // Get the Connected plant members of MCO
            StringList strMCOPlantMembers = domMCA.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.from["
                    + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");

            // Get Plant Members connected To Program-Project of MCO
            StringList slPlantMembersConnectedToProject = domMCA.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.to["
                    + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPLANTMEMBERTOPROGPROJ + "].to.name");

            // If current user is Member of connected Plant and also member from PlantMembers connected To Program-Project of MCO
            if (strMCOPlantMembers.contains(userName) && slPlantMembersConnectedToProject.contains(userName)) {
                isvalid = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return isvalid;
    }

    // PCM TIGTK-3856: 3/1/2017 : KWagh : End

    /**
     * @author Prepare Task Tree HREF for the give map values
     * @param context
     * @param paramMap
     * @return
     * @throws Exception
     */
    private String getTaskTreeHref(Context context, Map paramMap) throws Exception {
        StringBuffer strTreeLink = new StringBuffer();
        strTreeLink.append("<a href=\"JavaScript:emxTableColumnLinkClick('../common/emxTree.jsp?relId=");
        strTreeLink.append((String) paramMap.get("relId"));
        strTreeLink.append("&parentOID=");
        strTreeLink.append((String) paramMap.get("parentOID"));
        strTreeLink.append("&jsTreeID=");
        strTreeLink.append((String) paramMap.get("jsTreeID"));
        strTreeLink.append("&suiteKey=Framework");
        strTreeLink.append("&emxSuiteDirectory=common");
        strTreeLink.append("&objectId=${OBJECT_ID}&taskName=${NAME}");
        strTreeLink.append("', '', '', 'true', 'popup', '')\"  class=\"object\">");
        strTreeLink.append("<img border=\"0\" src=\"images/iconSmallTask.gif\">${NAME}</a>");
        return strTreeLink.toString();
    }

    /**
     * @author Prepare Awaiting Approval HREF for the give map values
     * @param context
     * @param paramMap
     * @return
     * @throws Exception
     */
    private String getTaskApprovalHref(Context context) throws Exception {
        // Form the Approve link template
        StringBuffer strTaskApproveLink = new StringBuffer(64);
        strTaskApproveLink.append(
                "<a target=\"hiddenFrame\" class=\"object\" href=\"../common/emxLifecycleApproveRejectPreProcess.jsp?emxTableRowId=${OBJECT_ID}^${STATE}^^${TASK_ID}&objectId=${OBJECT_ID}&suiteKey=Framework");
        strTaskApproveLink.append("\"><img border='0' src='../common/images/iconActionApprove.gif' />");

        strTaskApproveLink.append(EnoviaResourceBundle.getProperty(context, ChangeConstants.RESOURCE_BUNDLE_ENTERPRISE_STR, context.getLocale(), "EnterpriseChangeMgt.Command.AwaitingApproval"));
        strTaskApproveLink.append("</a>");

        return strTaskApproveLink.toString();

    }

    // PCM TIGTK-10239: 4/10/2017 : KWagh : End

    // PCM TIGTK-11324: 15-Nov-2017 : SayaliD : START
    /**
     * This is used as Edit Access Function for structure effectivity column to set that cell as editable or not.
     * @param context
     * @param args
     * @return StringList contains boolean value which will decide weather cell is editable or not.
     * @throws Exception
     */
    public static StringList getCellLevelEditAccess(Context context, String args[]) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        Boolean bAdmin = false;
        Boolean bCAowner = false;
        String strContextUser = context.getUser();
        String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strContextUser);
        String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);
        if (assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
            bAdmin = true;
        }
        StringList returnStringList = new StringList(objectList.size());
        Iterator objectItr = objectList.iterator();
        while (objectItr.hasNext()) {
            Map curObjectMap = (Map) objectItr.next();
            String CAObjectID = (String) curObjectMap.get("id");
            DomainObject domCAobj = new DomainObject(CAObjectID);
            if ((domCAobj.getInfo(context, DomainObject.SELECT_OWNER)).equalsIgnoreCase(strContextUser))
                bCAowner = true;
            // admin | CA owner | CA assignee
            if (bAdmin || bCAowner || strContextUser.equalsIgnoreCase(domCAobj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.name"))) {
                returnStringList.addElement(Boolean.TRUE);
            } else {
                returnStringList.addElement(Boolean.FALSE);
            }
        }
        return returnStringList;
    }

    /**
     * This Method is used as Access Function on Edit command of Related CA's tab and Related MCA's tab Change Owner / Admin users / Connect Program-Project members
     * @param context
     * @param args
     * @return Boolean -- > The output whether command to be visible or no
     * @throws Exception
     */
    public boolean editAccess(Context context, String args[]) throws Exception {
        boolean showEditCommand = false;
        //
        try {
            HashMap<?, ?> programMap = JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domChangeObject = DomainObject.newInstance(context, strObjectId);
            StringList slSelectStmts = new StringList();
            slSelectStmts.add(DomainConstants.SELECT_OWNER);
            slSelectStmts.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");

            Map<String, Object> mapChangeObjectDetails = domChangeObject.getInfo(context, slSelectStmts);
            String strOwner = (String) mapChangeObjectDetails.get(DomainConstants.SELECT_OWNER);
            StringList slPPConnectedMembersList = (StringList) mapChangeObjectDetails
                    .get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");
            Boolean bAdmin = false;
            Boolean bOwner = false;
            Boolean bPPMembers = false;
            String strContextUser = context.getUser();
            String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strContextUser);
            String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);
            if (assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)) {
                bAdmin = true;
            }
            if ((strOwner).equalsIgnoreCase(strContextUser))
                bOwner = true;

            if (slPPConnectedMembersList.contains(strContextUser))
                bPPMembers = true;

            if (bAdmin || bOwner || bPPMembers)
                showEditCommand = true;
        } catch (Exception ex) {
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in showCreateAndAddExistingMCOOnCRCO: ", ex);
            // TIGTK-5405 - 17-04-2017 - Rutuja Ekatpure - End
        }
        return showEditCommand;
    }

    // PCM TIGTK-11324: 15-Nov-2017 : SayaliD : END

    /**
     * This Method is used to exclude Governing project for affected items attached to CO
     * @param context
     * @param args
     * @return StringList -- >
     * @throws Exception
     */

    public StringList excludeAffectedItemsForCOAddExisting(Context context, String args[]) throws Exception {

        StringList slExcludeAffectedItemsList = new StringList();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String strCOObjectID = (String) programMap.get("objectId");

            DomainObject domCO = DomainObject.newInstance(context, strCOObjectID);

            StringList slCurrentAffectedItemsList = domCO.getInfoList(context,
                    "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].to.from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + ".to.id");
            slExcludeAffectedItemsList.addAll(slCurrentAffectedItemsList);

            String strCOconnectedProgramid = domCO.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            StringList slObjSelect = new StringList();

            slObjSelect.add(DomainObject.SELECT_ID);
            slObjSelect.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
            slObjSelect.add(DomainConstants.SELECT_CURRENT);// TIGTK-14861 - rgarbhe

            // TIGTK-14861 - rgarbhe : Start
            StringList slReleaseItem = new StringList();
            slReleaseItem.add("Released");
            slReleaseItem.add("Release");
            // TIGTK-14861 - rgarbhe :End

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PART);

            typePattern.addPattern(TigerConstants.TYPE_MCADDRAWING);

            typePattern.addPattern(TigerConstants.TYPE_MCAD_COMPONENT);

            typePattern.addPattern(TigerConstants.TYPE_MCAD_ASSEMBLY);

            typePattern.addPattern(TigerConstants.TYPE_MCADREPRESENTATION);

            typePattern.addPattern(TigerConstants.TYPE_PROEASSEMBLY);

            typePattern.addPattern(TigerConstants.TYPE_IEFASSEMBLYFAMILY);

            typePattern.addPattern(TigerConstants.TYPE_IEFCOMPONENTFAMILY);

            Query query = new Query();

            query.setBusinessObjectType(typePattern.getPattern());

            query.setBusinessObjectName(DomainConstants.QUERY_WILDCARD);

            query.setBusinessObjectRevision(DomainConstants.QUERY_WILDCARD);

            query.setWhereExpression(DomainConstants.EMPTY_STRING);

            ContextUtil.startTransaction(context, true);
            QueryIterator queryIterator = query.getIterator(context, slObjSelect, (short) 100);

            while (queryIterator.hasNext()) {

                BusinessObjectWithSelect busWithSelect = queryIterator.next();

                String strAffectedItemsID = busWithSelect.getSelectData(DomainConstants.SELECT_ID);

                String strGoveringPrjId = busWithSelect.getSelectData("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");

                String strPartState = busWithSelect.getSelectData(DomainConstants.SELECT_CURRENT);// TIGTK-14861 - rgarbhe

                if (slReleaseItem.contains(strPartState) && UIUtil.isNotNullAndNotEmpty(strGoveringPrjId) && !strCOconnectedProgramid.equals(strGoveringPrjId)) {

                    slExcludeAffectedItemsList.add(strAffectedItemsID);

                }

            }
            // TIGTK-12992 : START
            queryIterator.close();
            // TIGTK-12992 : END
        } catch (Exception e) {
            logger.error("Error in excludeAffectedItemsForCOAddExisting: ", e);
            ContextUtil.abortTransaction(context);
        } finally {
            if (ContextUtil.isTransactionActive(context)) {
                ContextUtil.commitTransaction(context);
            }
        }

        return slExcludeAffectedItemsList;

    }

    /**
     * This method is used to Clone and Replace the Affected Item of CO
     * @param context
     * @param strSourceObjId
     * @return String TIGTK -11653
     * @throws FrameworkException
     */

    public String getClonedObjectId(Context context, String strSourceObjId, String strRequestedChangeAttrValue) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : getClonedObjectId : START");
        try {
            DomainObject domSourceObj = DomainObject.newInstance(context, strSourceObjId);
            String strSourceObjectType = domSourceObj.getInfo(context, DomainConstants.SELECT_TYPE);
            String strAutoNumberSeries = DomainConstants.EMPTY_STRING;
            String strClonedName = DomainConstants.EMPTY_STRING;
            StringBuffer sbKey = new StringBuffer();
            int flag = 0;

            if (domSourceObj.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {
                strSourceObjectType = DomainConstants.TYPE_CAD_DRAWING;
            } else if (domSourceObj.isKindOf(context, DomainConstants.TYPE_CAD_MODEL)) {
                strSourceObjectType = DomainConstants.TYPE_CAD_MODEL;
            }
            String strObjectGeneratorName = FrameworkUtil.getAliasForAdmin(context, DomainConstants.SELECT_TYPE, strSourceObjectType, true);

            if (DomainConstants.TYPE_PART.equals(strSourceObjectType)) {
                strClonedName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, strAutoNumberSeries);

                sbKey.append(strSourceObjId);
                sbKey.append(":");
                sbKey.append(strRequestedChangeAttrValue);
                mpPartName.put(sbKey.toString(), strClonedName);

            } else if (domSourceObj.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING) || domSourceObj.isKindOf(context, DomainConstants.TYPE_CAD_MODEL)) {
                // TIGTK-Change done as per new dev
                StringBuffer strRelationship = new StringBuffer(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
                // if (DomainConstants.TYPE_CAD_DRAWING.equals(strSourceObjectType)) {
                strRelationship.append(",");
                strRelationship.append(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
                // }

                // StringList slSourcePartID = domSourceObj.getInfoList(context, "to[" + strRelationship.toString() + "].from.id");
                MapList mlSourcePartID = domSourceObj.getRelatedObjects(context, strRelationship.toString(), DomainConstants.QUERY_WILDCARD, new StringList(DomainConstants.SELECT_ID), null, true,
                        false, (short) 1, null, null, 0);
                if (!mlSourcePartID.isEmpty()) {
                    for (int i = 0; i < mlSourcePartID.size(); i++) {
                        StringBuffer sbCheckKey = new StringBuffer();
                        Map mTempMap = (Map) mlSourcePartID.get(i);
                        String strSourcePartID = (String) mTempMap.get(DomainConstants.SELECT_ID);
                        sbCheckKey.append(strSourcePartID);
                        sbCheckKey.append(":");
                        sbCheckKey.append(strRequestedChangeAttrValue);
                        if (UIUtil.isNotNullAndNotEmpty(strSourcePartID)) {
                            DomainObject domSourcePart = DomainObject.newInstance(context, strSourcePartID);
                            String strSourcePartName = domSourcePart.getInfo(context, DomainConstants.SELECT_NAME);
                            String strSourceCADName = domSourceObj.getInfo(context, DomainConstants.SELECT_NAME);

                            if (mpPartName.containsKey(sbCheckKey.toString())) {
                                flag = 1;
                                strClonedName = mpPartName.get(sbCheckKey.toString());
                                if (strSourceCADName.equals(strSourcePartName) || strSourceCADName.startsWith(strSourcePartName + "_") || strSourceCADName.contains("_" + strSourcePartName + "_")
                                        || strSourceCADName.endsWith("_" + strSourcePartName)) {
                                    strClonedName = strSourceCADName.replace(strSourcePartName, strClonedName);
                                } else {
                                    strClonedName = strClonedName + "_" + strSourceCADName;
                                }
                            }
                        }
                    }
                    // TIGTK-Change done as per new dev
                }

                if (flag == 0) {
                    strAutoNumberSeries = "FAURECIA";
                    strClonedName = DomainObject.getAutoGeneratedName(context, strObjectGeneratorName, strAutoNumberSeries);
                }
            }
            BusinessObject busClonedObj = domSourceObj.cloneObject(context, strClonedName);
            DomainObject domclonedSourceObj = DomainObject.newInstance(context, busClonedObj);
            String strClonedObjectID = domclonedSourceObj.getId(context);

            String strTitleOriginalParent = domclonedSourceObj.getAttributeValue(context, DomainConstants.ATTRIBUTE_TITLE);

            int index = strTitleOriginalParent.lastIndexOf(".");
            String strExtensionOringalParent = DomainObject.EMPTY_STRING;
            if (index != -1)
                strExtensionOringalParent = strTitleOriginalParent.substring(index);
            String strClonedNewName = strClonedName + strExtensionOringalParent;
            // TIGTK-14267 -START
            if (domSourceObj.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING) || domSourceObj.isKindOf(context, DomainConstants.TYPE_CAD_MODEL)) {
                String[] args = new String[2];
                args[0] = strSourceObjId;
                args[1] = strClonedObjectID;
                //TIGTK-14267 :Vishal :START
                domclonedSourceObj.setAttributeValue(context, DomainConstants.ATTRIBUTE_TITLE, strClonedNewName);
                String strWorkspace = TigerUtils.getWorkspaceForBus(context, context.createWorkspace(), strClonedNewName);
                DomainObject doOrignalCad = DomainObject.newInstance(context, strSourceObjId);
                DomainObject doclonedCad = DomainObject.newInstance(context, strClonedObjectID);
                BusinessObject boOrignalCad = new BusinessObject(strSourceObjId);
                BusinessObject boclonedCad = new BusinessObject(strClonedObjectID);
                String slCadFile = DomainConstants.EMPTY_STRING;
                //DomainObject domCADObj = new DomainObject(strClonedObjectID);
                FileList filelist = boclonedCad.getFiles(context);
                FileItr fileItr = new FileItr(filelist);
                matrix.db.File file = null;
                while (fileItr.next()) {
                    file = fileItr.obj();
                    slCadFile = file.getName();
                }
                if (doOrignalCad.isKindOf(context, "PSS_CATProduct") || doclonedCad.isKindOf(context, "PSS_CATProduct")) {
                    if (!filelist.isEmpty()) {
                        boOrignalCad.checkoutFiles(context, false, "asm", filelist, strWorkspace);
                    }
                    TigerUtils.deleteFile(context, strClonedObjectID, "asm", slCadFile);
                } else if (doOrignalCad.isKindOf(context, "PSS_CATPart") || doclonedCad.isKindOf(context, "PSS_CATPart")) {
                    if (!filelist.isEmpty()) {
                        boOrignalCad.checkoutFiles(context, false, "prt", filelist, strWorkspace);
                    }
                    TigerUtils.deleteFile(context, strClonedObjectID, "prt", slCadFile);
                } else if (doOrignalCad.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING) || doclonedCad.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {
                    if (!filelist.isEmpty()) {
                        boOrignalCad.checkoutFiles(context, false, "drw", filelist, strWorkspace);
                    }
                    TigerUtils.deleteFile(context, strClonedObjectID, "drw", slCadFile);
                }
                String strNewModifiedName = DomainConstants.EMPTY_STRING;
                File cadFile = new File(strWorkspace + java.io.File.separator + (String) slCadFile);
                strNewModifiedName = slCadFile.replaceAll(slCadFile.substring(0, slCadFile.indexOf(".")), strClonedNewName);
                File fNew = new File(strWorkspace, strNewModifiedName);
                Files.move(cadFile.toPath(), fNew.toPath(), StandardCopyOption.REPLACE_EXISTING);
                if (doOrignalCad.isKindOf(context, "PSS_CATProduct") || doclonedCad.isKindOf(context, "PSS_CATProduct")) {
                    boclonedCad.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, "asm", strNewModifiedName, strWorkspace);
                } else if (doOrignalCad.isKindOf(context, "PSS_CATPart") || doclonedCad.isKindOf(context, "PSS_CATPart")) {
                    boclonedCad.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, "prt", strNewModifiedName, strWorkspace);
                } else if (doOrignalCad.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING) || doclonedCad.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {
                    boclonedCad.checkinFile(context, true, true, DomainConstants.EMPTY_STRING, "drw", strNewModifiedName, strWorkspace);
                }
                // TIGTK-14267 :Vishal :END
                PSS_emxPart_mxJPO emxPart = new PSS_emxPart_mxJPO(context, args);
                emxPart.cloneAndConnectVersionedCADObjects(context, strSourceObjId, strClonedObjectID);
            }
            // TIGTK-14267 - END
            logger.debug("PSS_enoECMChangeOrder : getClonedObjectId : End");
            return strClonedObjectID;
        } catch (RuntimeException e) {
            logger.error("Error in PSS_enoECMChangeOrder : getClonedObjectId : ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : getClonedObjectId : ", e);
            throw e;
        }

    }

    private String getGlobalConfigObjectName(Context context, String busId) throws Exception {
        // Get the IntegrationName
        IEFGuessIntegrationContext_mxJPO guessIntegration = new IEFGuessIntegrationContext_mxJPO(context, null);
        String jpoArgs[] = new String[1];
        jpoArgs[0] = busId;
        String integrationName = guessIntegration.getIntegrationName(context, jpoArgs);

        // Get the relevant GCO Name

        String gcoName = null;

        IEFSimpleConfigObject simpleLCO = IEFSimpleConfigObject.getSimpleLCO(context);

        String attrName = MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-IntegrationToGCOMapping");
        if (simpleLCO.isObjectExists()) {
            Hashtable integNameGcoMapping = simpleLCO.getAttributeAsHashtable(attrName, "\n", "|");
            gcoName = (String) integNameGcoMapping.get(integrationName);
        } else {
            IEFGetRegistrationDetails_mxJPO registrationDetailsReader = new IEFGetRegistrationDetails_mxJPO(context, null);
            String args[] = new String[1];
            args[0] = integrationName;
            String registrationDetails = registrationDetailsReader.getRegistrationDetails(context, args);
            gcoName = registrationDetails.substring(registrationDetails.lastIndexOf("|") + 1);
        }

        return gcoName;
    }

    /**
     * This method is used to Sort Affected Item as Parent and Child
     * @param context
     * @param mlConnectedAffectedItem
     * @return Maplist TIGTK -11653
     * @throws Exception
     */
    public MapList sortAffectedItemsInParentChildOrder(Context context, MapList mlConnectedAffectedItem) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : sortAffectedItemsInParentChildOrder : START");
        try {
            PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null);
            StringList slAffectedItemsIds = changeUtil.getStringListFromMapList(mlConnectedAffectedItem, DomainConstants.SELECT_ID);
            pss.ecm.ui.MfgChangeOrder_mxJPO MfgChangeOrderBase = new pss.ecm.ui.MfgChangeOrder_mxJPO();
            mlConnectedAffectedItem = MfgChangeOrderBase.getOrderedParentChild(context, slAffectedItemsIds);
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : sortAffectedItemsInParentChildOrder : ", e);
            throw e;
        }
        logger.debug("PSS_enoECMChangeOrder : sortAffectedItemsInParentChildOrder : End");
        return mlConnectedAffectedItem;
    }

    /**
     * BR-100 This is Access Expression For Add and Remove Command in CA and CO
     * @param context
     * @param args
     * @return
     * @throws Exception
     */

    public boolean hasAddRemoveImplementedItemsToInWorkCOAccess(Context context, String args[]) throws Exception {
        boolean bResult = false;
        String strContextUser = context.getUser();
        String strRoleAssigned = PersonUtil.getDefaultSCRole(context, strContextUser);
        String strContextCAAssignee = DomainConstants.EMPTY_STRING;

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strChangeObjID = (String) programMap.get("objectId");
        DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjID);

        // TIGTK-13351 : 16-02-2018 : START
        String strCOOwner = DomainConstants.EMPTY_STRING;
        boolean bCOCAStateCheck = false;
        // TIGTK-13351 : 16-02-2018 : END
        StringList slAssigneeList = new StringList();
        if (domChangeObject.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEORDER)) {

            // TIGTK-13351 : 16-02-2018 : START
            StringList slSelectables = new StringList(DomainConstants.SELECT_OWNER);
            slSelectables.add(DomainConstants.SELECT_CURRENT);
            Map mGetData = domChangeObject.getInfo(context, slSelectables);
            String strCOCurrent = (String) mGetData.get(DomainConstants.SELECT_CURRENT);
            strCOOwner = (String) mGetData.get(DomainConstants.SELECT_OWNER);
            if (strCOCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_CHANGEORDER_INWORK)) {
                bCOCAStateCheck = true;
            }
            // TIGTK-13351 : 16-02-2018 : END
            StringList slCAList = domChangeObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].to.id");

            if (!slCAList.isEmpty()) {
                for (int i = 0; i < slCAList.size(); i++) {
                    String strCAId = (String) slCAList.get(i);
                    DomainObject domCA = DomainObject.newInstance(context, strCAId);
                    String strCAAssignee = (String) domCA.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to." + DomainConstants.SELECT_NAME);
                    slAssigneeList.add(strCAAssignee);
                }

            }

        } else {
            StringList slSelectables = new StringList("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from." + DomainConstants.SELECT_ID);
            slSelectables.add("from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to." + DomainConstants.SELECT_NAME);
            // TIGTK-13351 : 16-02-2018 : START
            slSelectables.addElement(DomainConstants.SELECT_CURRENT);
            // TIGTK-13351 : 16-02-2018 : END
            Map mGetDataFromCA = domChangeObject.getInfo(context, slSelectables);
            String strCOIDFromCA = (String) mGetDataFromCA.get("to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from." + DomainConstants.SELECT_ID);
            strContextCAAssignee = (String) mGetDataFromCA.get("from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to." + DomainConstants.SELECT_NAME);
            slAssigneeList.add(strContextCAAssignee);
            DomainObject domCO = DomainObject.newInstance(context, strCOIDFromCA);
            // TIGTK-13351 : 16-02-2018 : START
            strCOOwner = domCO.getInfo(context, DomainConstants.SELECT_OWNER);
            String strCACurrent = (String) mGetDataFromCA.get(DomainConstants.SELECT_CURRENT);
            if (strCACurrent.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_INWORK)) {
                bCOCAStateCheck = true;
            }
            // TIGTK-13351 : 16-02-2018 : END
        }

        // TIGTK-13351 : 16-02-2018 : START
        if (bCOCAStateCheck) {
            if (TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR.equalsIgnoreCase(strRoleAssigned) || TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM.equalsIgnoreCase(strRoleAssigned)
                    || strContextUser.equalsIgnoreCase(strCOOwner) || slAssigneeList.contains(strContextUser)) {
                bResult = true;
            }
        }
        // TIGTK-13351 : 16-02-2018 : END

        return bResult;
    }

    public boolean hasAddCRAccess(Context context, String args[]) throws Exception {
        return hasAddRemoveCRAccess(context, args, true);
    }

    public boolean hasRemoveCRAccess(Context context, String args[]) throws Exception {
        return hasAddRemoveCRAccess(context, args, false);
    }

    public boolean hasAddRemoveCRAccess(Context context, String args[], boolean isAddAccess) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        boolean bResult = false;
        String strCOID = (String) programMap.get("objectId");
        DomainObject domCO = DomainObject.newInstance(context, strCOID);

        String strContextUser = context.getUser();
        String strRoleAssigned = PersonUtil.getDefaultSCRole(context, strContextUser);

        StringList slSelectObj = new StringList();
        slSelectObj.add(DomainConstants.SELECT_CURRENT);
        slSelectObj.add(DomainConstants.SELECT_ORIGINATOR);
        slSelectObj.add(DomainConstants.SELECT_OWNER);

        // TIGTK-17600 : 26/10/2018 : Prakash B - Start
        slSelectObj.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from." + DomainConstants.SELECT_ID);

        StringList objectSelect = new StringList(DomainConstants.SELECT_NAME);
        StringBuffer relWhere = new StringBuffer();
        relWhere.append("attribute[");
        relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
        relWhere.append("]");
        relWhere.append(" == '");
        relWhere.append(TigerConstants.ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD);
        relWhere.append("'");
        // TIGTK-17600 : 26/10/2018 : Prakash B - end

        Map mapCOInfo = domCO.getInfo(context, slSelectObj);

        String strCOCurrent = (String) mapCOInfo.get(DomainConstants.SELECT_CURRENT);
        String strCOCreator = (String) mapCOInfo.get(DomainConstants.SELECT_ORIGINATOR);
        String strCOOwner = (String) mapCOInfo.get(DomainConstants.SELECT_OWNER);

        // TIGTK-17600 : 26/10/2018 : Prakash B - Start
        String strProjectId = (String) mapCOInfo.get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from." + DomainConstants.SELECT_ID);
        pss.ecm.enoECMChange_mxJPO objenoECMChange = new pss.ecm.enoECMChange_mxJPO();
        MapList mlPM = objenoECMChange.getMembersFromProgram(context, new DomainObject(strProjectId), objectSelect, DomainConstants.EMPTY_STRINGLIST, DomainConstants.EMPTY_STRING,
                relWhere.toString());

        int size = mlPM.size();
        StringList slPM = new StringList();
        Map mapPM = null;
        for (int i = 0; i < size; i++) {
            mapPM = (Map) mlPM.get(i);
            slPM.add(mapPM.get(DomainObject.SELECT_NAME));
        }

        if (TigerConstants.STATE_PSS_CHANGEORDER_PREPARE.equalsIgnoreCase(strCOCurrent)
                && (strContextUser.equalsIgnoreCase(strCOCreator) || strContextUser.equalsIgnoreCase(strCOOwner) || TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR.equalsIgnoreCase(strRoleAssigned)
                        || TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM.equalsIgnoreCase(strRoleAssigned) || slPM.contains(strContextUser))) {
            bResult = true;
        } // TIGTK-17600 : 26/10/2018 : Prakash B - end
        else if (TigerConstants.STATE_PSS_CHANGEORDER_INWORK.equalsIgnoreCase(strCOCurrent)) {
            if (TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR.equalsIgnoreCase(strRoleAssigned) || TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM.equalsIgnoreCase(strRoleAssigned)) {
                bResult = true;
            } else if (isAddAccess && strCOOwner.equalsIgnoreCase(strContextUser)) {
                bResult = true;
            }
        }
        return bResult;
    }

    /**
     * This is Include OID to get the Objects Which is not conencted to any Active CO alreaady
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @comment TigerConstants value in switch Case is not Validated.
     */
    public StringList searchForEligibleImplementedItems(Context context, String args[]) throws Exception {

        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strChangeObjectID = (String) programMap.get("objectId");

            DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjectID);

            StringList slActiveCAState = new StringList();
            StringList slEligibleImplementedItems = new StringList();
            slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_PENDING);
            slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_INWORK);
            slActiveCAState.add(TigerConstants.STATE_CHANGEACTION_INAPPROVAL);

            StringList slObjSelect = new StringList();
            slObjSelect.add(DomainObject.SELECT_ID);

            String strWhereClause = DomainConstants.EMPTY_STRING;

            String[] strArray = { TigerConstants.POLICY_PSS_ECPART, TigerConstants.POLICY_STANDARDPART, TigerConstants.POLICY_PSS_CADOBJECT, TigerConstants.POLICY_PSS_Legacy_CAD };
            String srePolicies = String.join(",", strArray);

            if (domChangeObject.isKindOf(context, TigerConstants.TYPE_CHANGEACTION)) {
                String strCAType = domChangeObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE);
                switch (strCAType) {
                case "CAD":
                    String[] strArrayCAD = { TigerConstants.POLICY_PSS_CADOBJECT, TigerConstants.POLICY_PSS_Legacy_CAD };
                    srePolicies = String.join(",", strArrayCAD);
                    break;
                case "Part":
                    String[] strArrayPart = { TigerConstants.POLICY_PSS_ECPART };
                    srePolicies = String.join(",", strArrayPart);
                    break;
                case "Standard":
                    String[] strArrayStdPart = { TigerConstants.POLICY_STANDARDPART };
                    srePolicies = String.join(",", strArrayStdPart);
                    break;
                default:
                    break;
                }
            }

            strWhereClause = "policy matchlist \"" + srePolicies + "\" \",\"";

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PART);

            typePattern.addPattern(TigerConstants.TYPE_MCADDRAWING);
            typePattern.addPattern(TigerConstants.TYPE_MCAD_COMPONENT);
            typePattern.addPattern(TigerConstants.TYPE_MCAD_ASSEMBLY);
            typePattern.addPattern(TigerConstants.TYPE_MCADREPRESENTATION);
            typePattern.addPattern(TigerConstants.TYPE_PROEASSEMBLY);
            typePattern.addPattern(TigerConstants.TYPE_IEFASSEMBLYFAMILY);
            typePattern.addPattern(TigerConstants.TYPE_IEFCOMPONENTFAMILY);

            Query query = new Query();
            query.setBusinessObjectType(typePattern.getPattern());
            query.setBusinessObjectName(DomainConstants.QUERY_WILDCARD);
            query.setBusinessObjectRevision(DomainConstants.QUERY_WILDCARD);
            query.setWhereExpression(strWhereClause);

            ContextUtil.startTransaction(context, false);
            QueryIterator queryIterator = query.getIterator(context, slObjSelect, (short) 100);

            while (queryIterator.hasNext()) {

                BusinessObjectWithSelect busWithSelect = queryIterator.next();
                String strImplemnetedItemID = busWithSelect.getSelectData(DomainConstants.SELECT_ID);
                DomainObject domImplementedItem = DomainObject.newInstance(context, strImplemnetedItemID);

                StringList slSelectables = new StringList();
                slSelectables.add("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current");
                slSelectables.add("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.current");

                Map mpImplItemInfo = domImplementedItem.getInfo(context, slSelectables);

                String strConnectedAffectedCACurrent = (String) mpImplItemInfo.get("to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.current");
                String strConnectedImplementedCACurrent = (String) mpImplItemInfo.get("to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].from.current");
                String strPolicy = domImplementedItem.getPolicy(context).getName();

                if (!slActiveCAState.contains(strConnectedImplementedCACurrent) && !slActiveCAState.contains(strConnectedAffectedCACurrent)) {

                    if (TigerConstants.POLICY_PSS_CADOBJECT.equalsIgnoreCase(strPolicy)) {

                        StringList slPartSpecPolicys = domImplementedItem.getInfoList(context, "to[" + ChangeConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.policy");
                        StringList slChartedDrawingPolicys = domImplementedItem.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].from.policy");

                        if (!slPartSpecPolicys.contains(TigerConstants.POLICY_PSS_DEVELOPMENTPART) && !slChartedDrawingPolicys.contains(TigerConstants.POLICY_PSS_DEVELOPMENTPART)) {

                            slEligibleImplementedItems.add(strImplemnetedItemID);
                        }
                    } else {
                        slEligibleImplementedItems.add(strImplemnetedItemID);
                    }

                }
            }
            queryIterator.close();
            query.close(context);
            ContextUtil.commitTransaction(context);

            return slEligibleImplementedItems;
        } catch (RuntimeException e) {
            logger.error("Error in PSS_enoECMChangeOrder : searchForEligibleImplementedItems : ", e);
            throw e;
        }

    }

    public void connectImplementedItems(Context context, String args[]) throws Exception {
        try {
            Map programMap = (HashMap) JPO.unpackArgs(args);
            StringList slSelectedImplementedItems = (StringList) programMap.get("selectList");
            String strCOObjId = (String) programMap.get("objectId");

            Set<String> hsECPart = new HashSet<>();
            Set<String> hsSTDPart = new HashSet<>();
            Set<String> hsCADPart = new HashSet<>();
            Set<String> hsImpList = new HashSet<>();

            Iterator itrSelectedItems = slSelectedImplementedItems.iterator();
            while (itrSelectedItems.hasNext()) {
                String strObjectID = (String) itrSelectedItems.next();
                BusinessObject domImplObj = new BusinessObject(strObjectID);
                String strPolicy = domImplObj.getPolicy(context).getName();

                if (TigerConstants.POLICY_PSS_ECPART.equalsIgnoreCase(strPolicy)) {
                    hsECPart.add(strObjectID);
                } else if (TigerConstants.POLICY_STANDARDPART.equalsIgnoreCase(strPolicy)) {
                    hsSTDPart.add(strObjectID);
                } else
                    hsCADPart.add(strObjectID);
            }

            Object[] objArray = hsECPart.toArray();
            String[] stringArray = Arrays.copyOf(objArray, objArray.length, String[].class);
            StringList slSymmetricalObjects = getSymmetricalPart(context, stringArray);
            hsECPart.addAll(slSymmetricalObjects);

            Map<String, String> mpCATypeVsCAId = getInWorkCAIdswithCATypeConnectedToCO(context, strCOObjId);

            for (Entry<String, String> objEntry : mpCATypeVsCAId.entrySet()) {

                DomainObject domCOObj = DomainObject.newInstance(context, strCOObjId);
                String strPurposeOfRelease = domCOObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);
                String strProgramProjectOID = domCOObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

                String strCAType = objEntry.getKey();
                String strCAId = objEntry.getValue();
                StringList busSelect = new StringList();

                switch (strCAType) {
                case "CAD":
                    hsImpList = hsCADPart;
                    busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                            + TigerConstants.RANGE_APPROVAL_LIST_FORCADONCO + "'].to.id");
                    break;
                case "Part":
                    hsImpList = hsECPart;
                    String strRouteTemplateObjectValue = getProgramProjectRouteTemplateValue(context, strProgramProjectOID, strPurposeOfRelease);
                    busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                            + strRouteTemplateObjectValue + "'].to.id");
                    break;
                case "Standard":
                    hsImpList = hsSTDPart;
                    busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                            + TigerConstants.RANGE_APPROVAL_LIST_FORSTANDARDPARTSONCO + "'].to.id");
                    break;
                default:
                    break;
                }

                DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
                // Get Route Template From ProgramProject
                Map mapRouteTemplateDetails = domProgramProjectObject.getInfo(context, busSelect);
                String strRouteTemplateId = (String) mapRouteTemplateDetails.get("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.id");

                if (UIUtil.isNotNullAndNotEmpty(strRouteTemplateId)) {
                    connectImplementedItemToCA(context, strCOObjId, strCAId, strCAType, hsImpList);
                }
            }

        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : connectImplementedItems : ", e);
            throw e;
        }
    }

    public void connecteImplementedItemToCA(Context context, String args[]) throws Exception {
        // TODO: GEtCA ID and CAT Type and seledcted IMPL Items
        Set<String> hsImpList = new HashSet<>();
        Map programMap = (HashMap) JPO.unpackArgs(args);
        StringList slSelectedImplementedItems = (StringList) programMap.get("selectList");
        String strCAObjId = (String) programMap.get("objectId");
        DomainObject domCA = DomainObject.newInstance(context, strCAObjId);
        String strCAType = domCA.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE);
        hsImpList.addAll(slSelectedImplementedItems);
        if (TigerConstants.ATTRIBUTE_PSS_CATYPE_PART.equals(strCAType)) {
            Object[] objArray = hsImpList.toArray();
            String[] stringArray = Arrays.copyOf(objArray, objArray.length, String[].class);
            StringList slSymmetricalObjects = getSymmetricalPart(context, stringArray);
            hsImpList.addAll(slSymmetricalObjects);
        }
        connectImplementedItemToCA(context, DomainConstants.EMPTY_STRING, strCAObjId, strCAType, hsImpList);
    }

    private void connectImplementedItemToCA(Context context, String strCOID, String strCAId, String strCAType, Set<String> hsImpList) throws Exception {
        boolean bIsUserAgent = false;
        try {
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            DomainObject domCA = null;
            bIsUserAgent = true;
            if (UIUtil.isNotNullAndNotEmpty(strCOID) && UIUtil.isNullOrEmpty(strCAId) && !hsImpList.isEmpty()) {
                domCA = createAndPromoteChangeAction(context, strCOID, strCAType);
                connectRouteTemplateToNewCA(context, domCA, strCAType, strCOID);

            } else {
                domCA = new DomainObject(strCAId);
            }

            if (domCA.exists(context)) {
                StringList slAffectedItems = domCA.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id");
                hsImpList.removeAll(slAffectedItems);
                Object[] sArryImplItemIds = hsImpList.toArray();
                String[] stringArray = Arrays.copyOf(sArryImplItemIds, sArryImplItemIds.length, String[].class);

                Map<String, String> mObjectIdVsRelIDsMap = DomainRelationship.connect(context, domCA, ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, true, stringArray);

                setRequestedChangeForImplementedItems(context, mObjectIdVsRelIDsMap);
            }
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : connectImplementedItemToCA : ", e);
            throw e;
        } finally {
            if (bIsUserAgent) {
                ContextUtil.popContext(context);
            }
        }
    }

    public void setRequestedChangeForImplementedItems(Context context, Map<?, ?> mCATOPartMap) throws FrameworkException {

        for (Entry<?, ?> objEntry : mCATOPartMap.entrySet()) {
            String relId = (String) objEntry.getValue();
            DomainRelationship domRelImplementedItem = new DomainRelationship(relId);
            domRelImplementedItem.setAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE, ChangeConstants.FOR_RELEASE);

        }
    }

    public DomainObject createAndPromoteChangeAction(Context context, String strCOObjId, String strCAType) throws Exception {

        try {
            PSS_enoECMChangeUtil_mxJPO changeUtil = new PSS_enoECMChangeUtil_mxJPO(context, null);
            String CAId = changeUtil.createNewCA(context, strCOObjId);
            BusinessObject boCA = new BusinessObject(CAId);
            boCA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE, strCAType);
            boCA.promote(context);
            DomainObject domCA = new DomainObject(boCA);
            return domCA;
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : createAndPromoteChangeAction : ", e);
            throw e;
        }

    }

    private Map<String, String> getInWorkCAIdswithCATypeConnectedToCO(Context context, String strCOId) throws Exception {
        try {

            Map<String, String> mpCATypeVsCAId = new HashMap<String, String>();
            mpCATypeVsCAId.put(TigerConstants.ATTRIBUTE_PSS_CATYPE_PART, DomainConstants.EMPTY_STRING);
            mpCATypeVsCAId.put(TigerConstants.ATTRIBUTE_PSS_CATYPE_CAD, DomainConstants.EMPTY_STRING);
            mpCATypeVsCAId.put(TigerConstants.ATTRIBUTE_PSS_CATYPE_STD, DomainConstants.EMPTY_STRING);
            DomainObject domCO = DomainObject.newInstance(context, strCOId);
            StringList slCASelectable = new StringList();
            slCASelectable.add(DomainConstants.SELECT_ID);
            slCASelectable.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");

            String strCAWhereClause = "current == '" + TigerConstants.STATE_CHANGEACTION_INWORK + "'";

            MapList mlInWorkCAs = domCO.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ACTION, slCASelectable, new StringList(), false, true,
                    (short) 1, strCAWhereClause, null, 0);
            if (!mlInWorkCAs.isEmpty()) {
                Iterator itrInworkCAs = mlInWorkCAs.iterator();
                while (itrInworkCAs.hasNext()) {
                    Map<String, String> mpCAinfo = (Map<String, String>) itrInworkCAs.next();
                    String strCAId = mpCAinfo.get(DomainConstants.SELECT_ID);
                    String strCAType = mpCAinfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "]");
                    mpCATypeVsCAId.put(strCAType, strCAId);
                }
            }
            return mpCATypeVsCAId;

        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : getInWorkCAIdswithCATypeConnectedToCO : ", e);
            throw e;
        }
    }

    public int checkSymmetricalPartPresentOrNot(Context context, String args[]) throws Exception {

        try {

            int nReturn = 0;
            String strCAId = args[0];
            DomainObject domCA = DomainObject.newInstance(context, strCAId);
            StringList slImplemetedItems = domCA.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.id");
            if (!slImplemetedItems.isEmpty()) {
                Iterator itrImplItems = slImplemetedItems.iterator();
                while (itrImplItems.hasNext()) {
                    String ImplementedItemId = (String) itrImplItems.next();
                    DomainObject domImplObj = DomainObject.newInstance(context, ImplementedItemId);
                    String strPolicy = domImplObj.getInfo(context, DomainConstants.SELECT_POLICY);

                    if (TigerConstants.POLICY_PSS_ECPART.equalsIgnoreCase(strPolicy)) {

                        StringList slObjectSelect = new StringList(1);
                        slObjectSelect.addElement(DomainConstants.SELECT_ID);
                        String strObjectWhere = DomainConstants.SELECT_ID + "!= " + ImplementedItemId;
                        MapList mlSymmetricalParts = domImplObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_HASSYMMETRICALPART, DomainConstants.TYPE_PART, slObjectSelect,
                                DomainConstants.EMPTY_STRINGLIST, true, true, (short) 1, strObjectWhere, null, 0);
                        if (!mlSymmetricalParts.isEmpty()) {
                            Iterator itrSymmetricalParts = mlSymmetricalParts.iterator();
                            while (itrSymmetricalParts.hasNext()) {
                                Map mSymmetricalPart = (Map) itrSymmetricalParts.next();
                                String strSymmetricalPartID = (String) mSymmetricalPart.get(DomainConstants.SELECT_ID);
                                if (!slImplemetedItems.contains(strSymmetricalPartID)) {
                                    nReturn = 1;
                                    break;
                                }
                            }

                        }

                    }
                }
            }
            return nReturn;
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : checkSymmetricalPartPresentOrNot : ", e);
            throw e;
        }
    }

    private String getCOObjectConnectedToOriginalObject(Context context, DomainObject domImplObj, String strImplObjIDPolicy) throws Exception {
        try {

            String strActiveCOID = DomainConstants.EMPTY_STRING;
            String strRelationBetweenImplObjAndOriginalObj = DomainConstants.EMPTY_STRING;

            if (TigerConstants.POLICY_PSS_ECPART.equalsIgnoreCase(strImplObjIDPolicy) || TigerConstants.POLICY_STANDARDPART.equalsIgnoreCase(strImplObjIDPolicy)) {

                strRelationBetweenImplObjAndOriginalObj = TigerConstants.RELATIONSHIP_DERIVED;
            } else {
                strRelationBetweenImplObjAndOriginalObj = TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD;

            }

            String strOriginalObjID = domImplObj.getInfo(context, "from[" + strRelationBetweenImplObjAndOriginalObj + "].to.id");

            if (UIUtil.isNotNullAndNotEmpty(strOriginalObjID)) {

                DomainObject domOriginalObj = DomainObject.newInstance(context, strOriginalObjID);
                MapList mlConnectedActiveCO = this.getConnectedActiveCO(context, domOriginalObj);
                Iterator itrActiveCO = mlConnectedActiveCO.iterator();
                while (itrActiveCO.hasNext()) {
                    Map mpActiveCO = (Map) itrActiveCO.next();
                    strActiveCOID = (String) mpActiveCO.get(DomainConstants.SELECT_ID);
                }
            }
            return strActiveCOID;
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : getCOObjectConnectedToOriginalObject : ", e);
            throw e;
        }

    }

    private MapList getConnectedActiveCO(Context context, DomainObject domImplObj) throws Exception {
        try {
            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_CHANGEACTION);
            relationshipPattern.addPattern(ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_CHANGEACTION);
            typePattern.addPattern(TigerConstants.TYPE_PSS_CHANGEORDER);

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEORDER);

            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.add(DomainObject.SELECT_ID);

            StringBuffer sbObjectWhere = new StringBuffer();
            sbObjectWhere.append("current matchlist '");
            sbObjectWhere.append(TigerConstants.STATE_PSS_CHANGEORDER_PREPARE);
            sbObjectWhere.append(",");
            sbObjectWhere.append(TigerConstants.STATE_PSS_CHANGEORDER_INWORK);
            sbObjectWhere.append(",");
            sbObjectWhere.append(TigerConstants.STATE_PSS_CHANGEORDER_INAPPROVAL);
            sbObjectWhere.append("'");

            MapList mlActiveCO = domImplObj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    sbObjectWhere.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);

            return mlActiveCO;

        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : getConnectedActiveCO : ", e);
            throw e;
        }

    }

    public StringList getSystemCreatedFinalImplementedItemIdList(Context context, DomainObject domCAObject) throws Exception {
        try {
            StringList slSystemCreatedImplementedItemIdList = new StringList();
            StringList relSelects = new StringList(1);
            relSelects.add(DomainRelationship.SELECT_RELATIONSHIP_ID);
            relSelects.add(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
            StringList slAISelectable = new StringList();
            slAISelectable.add(DomainConstants.SELECT_ID);
            slAISelectable.add(DomainConstants.SELECT_CURRENT);
            MapList mlAIList = domCAObject.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, "*", slAISelectable, relSelects, false, true, (short) 1, null, null, 0);

            Iterator<Map<?, ?>> itrAffectedItem = mlAIList.iterator();
            while (itrAffectedItem.hasNext()) {
                Map<?, ?> AffectedItem = itrAffectedItem.next();
                String strAffectedItemID = (String) AffectedItem.get(DomainConstants.SELECT_ID);
                DomainObject domAffectedItem = DomainObject.newInstance(context, strAffectedItemID);
                String strCRRequesedChangeValue = (String) AffectedItem.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                if (ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strCRRequesedChangeValue) || ChangeConstants.FOR_NONE.equalsIgnoreCase(strCRRequesedChangeValue)
                        || ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strCRRequesedChangeValue)) {
                    slSystemCreatedImplementedItemIdList.add(strAffectedItemID);

                } else if (ChangeConstants.FOR_REVISE.equalsIgnoreCase(strCRRequesedChangeValue)) {
                    if (!domAffectedItem.isLastRevision(context)) {
                        BusinessObject bNextRev = domAffectedItem.getNextRevision(context);
                        if (bNextRev.exists(context)) {
                            String strNextRevID = bNextRev.getObjectId();
                            slSystemCreatedImplementedItemIdList.add(strNextRevID);
                        }
                    }

                } else if (TigerConstants.FOR_CLONE.equalsIgnoreCase(strCRRequesedChangeValue) || TigerConstants.FOR_REPLACE.equalsIgnoreCase(strCRRequesedChangeValue)) {
                    if (domAffectedItem.isKindOf(context, TigerConstants.TYPE_PART)) {
                        String strNewPartID = domAffectedItem.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_DERIVED + "].to.id");
                        if (UIUtil.isNotNullAndNotEmpty(strNewPartID))
                            slSystemCreatedImplementedItemIdList.add(strNewPartID.trim());
                    } else {

                        String strNewPartID = domAffectedItem.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].to.id");
                        if (UIUtil.isNotNullAndNotEmpty(strNewPartID))
                            slSystemCreatedImplementedItemIdList.add(strNewPartID);
                    }

                }
            }
            return slSystemCreatedImplementedItemIdList;
        } catch (RuntimeException e) {
            logger.error("Error in PSS_enoECMChangeOrder : getSystemCreatedFinalImplementedItemIdList : ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : getSystemCreatedFinalImplementedItemIdList : ", e);
            throw e;
        }

    }

    public StringList getSystemCreatedImplementedItemIdList(Context context, StringList slCAOIDList) throws Exception {
        try {
            Iterator itrChange = slCAOIDList.iterator();
            StringList slSystemCreatedImplementedItemIdList = new StringList();
            while (itrChange.hasNext()) {
                String strCAOID = (String) itrChange.next();
                // Create Domain Object of CA
                DomainObject domCAObject = DomainObject.newInstance(context, strCAOID);
                slSystemCreatedImplementedItemIdList.addAll(getSystemCreatedFinalImplementedItemIdList(context, domCAObject));
            }
            return slSystemCreatedImplementedItemIdList;
        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : getSystemCreatedImplementedItemIdList : ", e);
            throw e;
        }

    }

    /**
     * Description : This method is copied from enoECMChangeOrderBase. PCM :Method invoked from the Delete Trigger of rel "Change Affected Item" And "Implemented Item" method is Used to delete the CA
     * Object, if the Affected Item or Implemented Item is the Last one to be removed.
     * @param context
     * @param args
     *            CA Object and Affected Item Object
     * @return integer
     * @throws Exception
     */
    public int deleteCAOnLastImplementedItemAndAffectedItem(Context context, String args[]) throws Exception {
        try {

            // Added for CO cancel and cancel implemented item functionality - for disconnect Affected item CA is Deleted as per trigger method.Prevent from Deletion CA- added below code.
            String strCancelStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_COCancel");
            if (UIUtil.isNotNullAndNotEmpty(strCancelStatus) && strCancelStatus.equals("True")) {
                return 0;
            }
            String strCAObjectId = args[0];
            DomainObject domCAObject = DomainObject.newInstance(context, strCAObjectId);
            String strCACurrent = domCAObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            if (!strCACurrent.equals(TigerConstants.STATE_CHANGEACTION_CANCELLED)) {

                StringList objSelects = new StringList(SELECT_ID);

                MapList mpImplementedItemsAndAffectedItem = domCAObject.getRelatedObjects(context,
                        ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM, // relationship
                        // pattern
                        DomainConstants.QUERY_WILDCARD, // object pattern
                        objSelects, // object selects
                        new StringList(DomainRelationship.SELECT_ID), // relationship
                        // selects
                        false, // to direction
                        true, // from direction
                        (short) 1, // recursion level
                        EMPTY_STRING, // object where clause
                        null, (short) 0); // relationship where clause
                if (mpImplementedItemsAndAffectedItem.isEmpty())

                    domCAObject.deleteObject(context);
            }

        } catch (Exception Ex) {
            logger.error("Error in deleteCAOnLastImplementedItemAndAffectedItem: ", Ex);
            throw Ex;
        }
        return 0;
    }

    /**
     * This method is used for post process for Editing Change Order.
     * @param context
     * @param args
     * @throws Exception
     * @Since 07-02-2018 : TIGTK-13112
     * @author psalunke
     */
    public HashMap postProcessForChangeOrderEdit(Context context, String args[]) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : postProcessForChangeOrderEdit : Start");
        try {
            // OOTB Post Process code : START
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            HashMap resultMap = new HashMap();

            if (ChangeUtil.isCFFInstalled(context)) {
                String createJPO = ECMAdmin.getCustomChangeCreateJPO(context, "XCE");

                if (UIUtil.isNotNullAndNotEmpty(createJPO)) {
                    String programName = createJPO.replaceAll(":.*$", "").trim();
                    String methodName = createJPO.replaceAll("^.*:", "").trim();

                    resultMap = JPO.invoke(context, programName, null, methodName, args, HashMap.class);
                }
            }
            // OOTB Post Process code : END
            String strChangeObjectID = (String) requestMap.get("objectId");
            String strProgProjId = (String) requestMap.get("ProjectCodeOID");
            if (UIUtil.isNotNullAndNotEmpty(strProgProjId)) {
                PSS_enoECMChangeRequest_mxJPO jpoChangeObject = new PSS_enoECMChangeRequest_mxJPO(context, null);
                jpoChangeObject.updateCSOfChangeObject(context, strProgProjId, strChangeObjectID);
            }
            logger.debug("PSS_enoECMChangeOrder : postProcessForChangeOrderEdit : END");
            return resultMap;

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : postProcessForChangeOrderEdit : ERROR", ex);
            throw ex;
        }
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     *             TIGTK-11674 This method is used to diplay flag on CO table.
     */
    public Vector<String> showFlagForTransferredFromCO(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : showFlagForTransferredFromCO : Start");
        try {
            Vector<String> vecResult = new Vector<String>();

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramList");
            String strChangeObjectId = (String) paramMap.get("objectId");
            DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjectId);
            // Get list for CR connected to CO
            StringList slConnectedCR = domChangeObject.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_CHANGEORDER + "].from.id");

            MapList objectList = (MapList) programMap.get("objectList");
            for (int i = 0; i < objectList.size(); i++) {
                boolean flag = false;
                StringBuilder str = new StringBuilder();
                Map<String, String> map = (Map<String, String>) objectList.get(i);

                String strAffectedItemId = (String) map.get("id");
                DomainObject domAffectedId = DomainObject.newInstance(context, strAffectedItemId);
                // Get list for CR connected to Affected Item
                StringList slCRConnectedToAI = domAffectedId.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].from.id");

                for (int k = 0; k < slCRConnectedToAI.size(); k++) {
                    String strCRId = (String) slCRConnectedToAI.get(k);
                    if (slConnectedCR.contains(strCRId)) {
                        flag = true;
                        break;
                    }
                }
                String statusImageString = "";
                if (flag == false) {
                    statusImageString = "<a><img ALIGN=\"right\" border=\"0\" src=\"../common/images/buttonDialogCancel.gif\"  title=\"Manually added in Change Order\"></img></a>";
                } else {
                    statusImageString = "<a><img ALIGN=\"right\" border=\"0\" src=\"../common/images/buttonDialogDone.gif\" title=\"Transferred from CR\"></img></a>";
                }

                str.append("");
                str.append(statusImageString);
                vecResult.add(str.toString());
            }
            logger.debug("PSS_enoECMChangeOrder : showFlagForTransferredFromCO : END");
            return vecResult;

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : showFlagForTransferredFromCO : ERROR", ex);
            throw ex;
        }
    }

    /**
     * TIGTK-13283
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */

    public StringList getImplementedItemIdList(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : getImplementedItemIdList : START");
        try {
            StringList slSystemCreatedImplementedItemIdList = new StringList();
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domObject = DomainObject.newInstance(context, strObjectId);
            String strType = domObject.getInfo(context, DomainConstants.SELECT_TYPE);

            if (TigerConstants.TYPE_PSS_CHANGEORDER.equals(strType)) {
                // Get Connected CA to CO
                StringList slCAOIDList = domObject.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].to.id");
                slSystemCreatedImplementedItemIdList = getSystemCreatedImplementedItemIdList(context, slCAOIDList);
            } else if (ChangeConstants.TYPE_CHANGE_ACTION.equals(strType)) {
                slSystemCreatedImplementedItemIdList = getSystemCreatedFinalImplementedItemIdList(context, domObject);
            }
            logger.debug("PSS_enoECMChangeOrder : getImplementedItemIdList : END");
            return slSystemCreatedImplementedItemIdList;

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : getImplementedItemIdList : ERROR", ex);
            throw ex;
        }

    }

    public void connectRouteTemplateToNewCA(Context context, DomainObject domCA, String strCAType, String strCOID) throws Exception {
        try {
            StringList slCAbusSelect = new StringList();
            slCAbusSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.name");
            slCAbusSelect.add("from[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].to.from[" + DomainConstants.RELATIONSHIP_INITIATING_ROUTE_TEMPLATE + "].to.id");
            slCAbusSelect.add(DomainConstants.SELECT_ID);
            Map mapToObjectInfo = domCA.getInfo(context, slCAbusSelect);
            String strAlreadyConnectedRouteTemplateOfCAName = (String) mapToObjectInfo.get("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.name");

            String strRouteTemplateId = (String) mapToObjectInfo
                    .get("from[" + DomainConstants.RELATIONSHIP_OBJECT_ROUTE + "].to.from[" + DomainConstants.RELATIONSHIP_INITIATING_ROUTE_TEMPLATE + "].to.id");

            if (UIUtil.isNullOrEmpty(strAlreadyConnectedRouteTemplateOfCAName)) {
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                domCA.addToObject(context, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES), strRouteTemplateId);
                ContextUtil.popContext(context);
            }

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : connectRouteTemplateToNewCA : ERROR", ex);
            throw ex;

        }

    }

    public String checkForRouteTemplateOnProgramProject(Context context, DomainObject domCOObj, DomainObject domCA) throws Exception {
        // Get CA Object Id and Route Attribute Values from args[]
        boolean isContextPushed = false;
        logger.debug("PSS_enoECMChangeOrder:checkForRouteTemplateOnProgramProject:START");
        try {
            String strCATypeReturn = DomainConstants.EMPTY_STRING;
            String strPurposeOfRelease = domCOObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);
            String strProgramProjectOID = domCOObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            String strCAType = domCA.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CATYPE);
            String strCATypeMesssage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.NOValidCAType");

            StringList busSelect = new StringList();

            if ("Part".equals(strCAType)) {
                String strRouteTemplateObjectValue = getProgramProjectRouteTemplateValue(context, strProgramProjectOID, strPurposeOfRelease);
                busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='" + strRouteTemplateObjectValue
                        + "'].to.id");
            } else if ("Standard".equals(strCAType)) {
                busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                        + TigerConstants.RANGE_APPROVAL_LIST_FORSTANDARDPARTSONCO + "'].to.id");
            } else if ("CAD".equals(strCAType)) {
                busSelect.add("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "|attribute[" + TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE + "]=='"
                        + TigerConstants.RANGE_APPROVAL_LIST_FORCADONCO + "'].to.id");
            } else {
                throw new Exception(strCATypeMesssage);

            }

            DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
            // Get Route Template From ProgramProject
            Map mapRouteTemplateDetails = domProgramProjectObject.getInfo(context, busSelect);
            String strRouteTemplateId = (String) mapRouteTemplateDetails.get("from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES + "].to.id");
            if (UIUtil.isNullOrEmpty(strRouteTemplateId)) {
                strCATypeReturn = strCAType;
            }
            logger.debug("PSS_enoECMChangeOrder : checkForRouteTemplateOnProgramProject : END");
            return strCATypeReturn;

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : checkForRouteTemplateOnProgramProject : ERROR", ex);
            throw ex;
        }
    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */

    public StringList getCloneDiversityColumnCellEditAccess(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : getCloneDiversityColumnCellEditAccess : START");
        try {
            StringList slAccess = getCloneDiversityOrCopyReferenceDocEditAccess(context, args, true);
            logger.debug("PSS_enoECMChangeOrder : getCloneDiversityColumnCellEditAccess : END");
            return slAccess;

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : getCloneDiversityColumnCellEditAccess : ERROR", ex);
            throw ex;
        }

    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */

    public StringList getCopyReferenceDocumentColumnCellEditAccess(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : getCopyReferenceDocumentColumnCellEditAccess : START");
        try {
            StringList slAccess = getCloneDiversityOrCopyReferenceDocEditAccess(context, args, false);
            logger.debug("PSS_enoECMChangeOrder : getCopyReferenceDocumentColumnCellEditAccess : END");
            return slAccess;

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : getCopyReferenceDocumentColumnCellEditAccess : ERROR", ex);
            throw ex;
        }

    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */

    public StringList getCloneDiversityOrCopyReferenceDocEditAccess(Context context, String[] args, boolean isEditAccessForCloneDiversity) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : getCloneDiversityOrCopyReferenceDocEditAccess : START");
        try {
            StringList slAccess = new StringList();

            // Unpack args
            Map programMap = JPO.unpackArgs(args);
            MapList mlAffectedItems = (MapList) programMap.get("objectList");

            // Iterate over the list get Change Affected Item relationships id and the policy of the object.
            String strAccessValue = "false";
            Iterator<Map<?, ?>> iterAffectedItems = mlAffectedItems.iterator();
            while (iterAffectedItems.hasNext()) {
                strAccessValue = "false";
                Map<String, String> mAI = (Map<String, String>) iterAffectedItems.next();
                String strRelChangeAffectedItemID = mAI.get("id[connection]");
                String strPolicy = mAI.get("policy");

                // Create Domain Relationship object and get value of "Requested Change" object.
                DomainRelationship domChangeAffectedItem = DomainRelationship.newInstance(context, strRelChangeAffectedItemID);
                String strRequestedChange = domChangeAffectedItem.getAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE);

                // Check value of Requested Change and policy and give access accordingly.
                if ((TigerConstants.FOR_REPLACE.equalsIgnoreCase(strRequestedChange) || TigerConstants.FOR_CLONE.equalsIgnoreCase(strRequestedChange))) {
                    strAccessValue = "true";
                }
                if (isEditAccessForCloneDiversity && "true".equals(strAccessValue)
                        && !(TigerConstants.POLICY_PSS_ECPART.equalsIgnoreCase(strPolicy) || TigerConstants.POLICY_STANDARDPART.equalsIgnoreCase(strPolicy))) {
                    strAccessValue = "false";
                }
                slAccess.add(strAccessValue);
            }

            logger.debug("PSS_enoECMChangeOrder : getCloneDiversityOrCopyReferenceDocEditAccess : END");
            return slAccess;

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : getCloneDiversityOrCopyReferenceDocEditAccess : ERROR", ex);
            throw ex;
        }

    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @throws Exception
     */

    public void resetCloneDiversityValue(Context context, String[] args) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : resetCloneDiversityValue : START");
        try {
            // Get relationship id and new value of the attribute "Requested Change" from arguments.

            String strRelId = args[0];
            String strNewAttributeValue = args[1];

            if (!TigerConstants.FOR_CLONE.equalsIgnoreCase(strNewAttributeValue) && !TigerConstants.FOR_REPLACE.equalsIgnoreCase(strNewAttributeValue)) {
                DomainRelationship domChangeAffectedItems = DomainRelationship.newInstance(context, strRelId);

                HashMap attribMap = new HashMap();
                attribMap.put(TigerConstants.ATTRIBUTE_PSS_CLONECOLORDIVERSITY, DomainConstants.EMPTY_STRING);
                attribMap.put(TigerConstants.ATTRIBUTE_PSS_CLONETECHNICALDIVERSITY, DomainConstants.EMPTY_STRING);
                attribMap.put(TigerConstants.ATTRIBUTE_PSS_KEEPREFERENCEDOCUMENT, DomainConstants.EMPTY_STRING);

                domChangeAffectedItems.setAttributeValues(context, attribMap);
            }
            logger.debug("PSS_enoECMChangeOrder : resetCloneDiversityValue : END");

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : resetCloneDiversityValue : ERROR", ex);
            throw ex;
        }

    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @throws Exception
     */

    public void performActionOnReplaceOrClone(Context context, String sObjectID, String strAffectedItem, String strCOID, String strRequestedChange) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : performActionOnReplaceOrClone : START");
        try {
            DomainObject domNewImplementedItem = DomainObject.newInstance(context, sObjectID);
            int flag = 0;
            // String strExpandRelationshipName = DomainConstants.EMPTY_STRING;
            StringBuffer strExpandRelationshipName = new StringBuffer();
            String strOriginalObjectRelationshipName = DomainConstants.EMPTY_STRING;

            DomainObject domAffectedItem = DomainObject.newInstance(context, strAffectedItem);
            String strType = domAffectedItem.getInfo(context, DomainObject.SELECT_TYPE);

            cloneColorDiversityAndReferenceDoc(context, strCOID, domAffectedItem, domNewImplementedItem);

            // TIGTK-15701 START
            if (DomainConstants.TYPE_PART.equals(strType)) {
                strType = DomainConstants.TYPE_PART;
                strOriginalObjectRelationshipName = TigerConstants.RELATIONSHIP_DERIVED;
                // strExpandRelationshipName = DomainConstants.RELATIONSHIP_EBOM;
                strExpandRelationshipName.append(DomainConstants.RELATIONSHIP_EBOM);
            } else {
                strType = DomainConstants.QUERY_WILDCARD;
                strOriginalObjectRelationshipName = TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD;
                // strExpandRelationshipName = TigerConstants.RELATIONSHIP_CADSUBCOMPONENT;
                strExpandRelationshipName.append(TigerConstants.RELATIONSHIP_CADSUBCOMPONENT);
                strExpandRelationshipName.append(",");
                strExpandRelationshipName.append(TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING);
            }
            // TIGTK-15701 END
            // String strOriginalObjID = domNewImplementedItem.getInfo(context, "to[" + strOriginalObjectRelationshipName + "].from.id");

            StringList slObjSelect = new StringList(2);
            slObjSelect.add("to[" + strOriginalObjectRelationshipName + "].from.id");
            slObjSelect.add(DomainConstants.SELECT_POLICY);
            slObjSelect.add("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
            Map mapObjInfo = domNewImplementedItem.getInfo(context, slObjSelect);
            String strOriginalObjID = (String) mapObjInfo.get("to[" + strOriginalObjectRelationshipName + "].from.id");
            String strNewImplementedItemPolicy = (String) mapObjInfo.get(DomainConstants.SELECT_POLICY);

            // String strNewImplementedItemLatestVersionId = DomainConstants.EMPTY_STRING;
            DomainObject domNewImplementedItemLatestVersion = new DomainObject();

            if ((strNewImplementedItemPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT))) {
                String strNewImplementedItemLatestVersionId = (String) mapObjInfo.get("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
                /// domNewImplementedItemLatestVersion = DomainObject.newInstance(context, strNewImplementedItemLatestVersionId);
                if (UIUtil.isNotNullAndNotEmpty(strNewImplementedItemLatestVersionId)) {

                    domNewImplementedItemLatestVersion.setId(strNewImplementedItemLatestVersionId);
                }
            }
            if (UIUtil.isNotNullAndNotEmpty(strOriginalObjID)) {

                DomainObject domOriginalObj = DomainObject.newInstance(context, strOriginalObjID);
                String strPolicy = domOriginalObj.getInfo(context, DomainConstants.SELECT_POLICY);

                StringList slRelationshipSelect = new StringList();
                slRelationshipSelect.add(DomainRelationship.SELECT_ID);
                slRelationshipSelect.add(DomainRelationship.SELECT_NAME);

                // Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_MODEL);
                // typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);

                StringList slObjectSelect = new StringList();
                slObjectSelect.add(DomainConstants.SELECT_ID);

                // Get list of Parent Objects of Original Object
                MapList mlImmediateParentInfo = domOriginalObj.getRelatedObjects(context, strExpandRelationshipName.toString(), strType, slObjectSelect, slRelationshipSelect, true, false, (short) 1,
                        null, null, 0);

                MapList mlImmediateChildInfo = domOriginalObj.getRelatedObjects(context, strExpandRelationshipName.toString(), strType, slObjectSelect, slRelationshipSelect, false, true, (short) 1,
                        null, null, 0);
                // If affected Item is CAD then connect with New Implemented Part with new Implemented CAD
                if ((strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT))) {

                    Pattern relationshipPattern = new Pattern(RELATIONSHIP_PART_SPECIFICATION);
                    relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);

                    MapList mlConnectedPart = domAffectedItem.getRelatedObjects(context, relationshipPattern.getPattern(), DomainConstants.TYPE_PART, slObjectSelect, slRelationshipSelect, true, false,
                            (short) 1, null, null, (short) 0);

                    if (!mlConnectedPart.isEmpty()) {
                        Iterator itrParts = mlConnectedPart.iterator();
                        while (itrParts.hasNext()) {
                            Map mpPartInfo = (Map) itrParts.next();
                            String strPartID = (String) mpPartInfo.get(DomainConstants.SELECT_ID);
                            String strRelID = (String) mpPartInfo.get(DomainRelationship.SELECT_NAME);
                            DomainObject domPart = DomainObject.newInstance(context, strPartID);

                            MapList mlActiveCOParent = getConnectedActiveCOWithCA(context, domPart);

                            if (!mlActiveCOParent.isEmpty()) {
                                Iterator itrCO = mlActiveCOParent.iterator();
                                while (itrCO.hasNext()) {
                                    Map mpActiveCOParent = (Map) itrCO.next();
                                    String strActiveCOID = (String) mpActiveCOParent.get("ChangeOrderId");
                                    if (strActiveCOID.equalsIgnoreCase(strCOID)) {
                                        String strChildRequestedChange = (String) mpActiveCOParent.get("RequestedChange");
                                        String strChangeAffectedRelId = (String) mpActiveCOParent.get("ChangeAffectedRelId");

                                        // String strNewImpactedPart = domPart.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_DERIVED + "].to.id");
                                        String strNewImpactedPart = DomainConstants.EMPTY_STRING;
                                        String strWhere = "current!=" + TigerConstants.STATE_PSS_CANCELPART_CANCELLED;
                                        StringList slObjectSelects = new StringList(DomainConstants.SELECT_ID);
                                        slObjectSelects.add(DomainConstants.SELECT_CURRENT);

                                        MapList mlClonedOrReplacedPart = domPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_DERIVED, DomainConstants.TYPE_PART, slObjectSelects, null,
                                                false, true, (short) 1, strWhere, null, 0);

                                        /*
                                         * if (!mlClonedOrReplacedPart.isEmpty()) { Map mTempMap = (Map) mlClonedOrReplacedPart.get(0); strNewImpactedPart = (String)
                                         * mTempMap.get(DomainConstants.SELECT_ID); }
                                         */
                                        if (!mlClonedOrReplacedPart.isEmpty()) {

                                            for (int m = 0; m < mlClonedOrReplacedPart.size(); m++) {
                                                Map mTempMap = (Map) mlClonedOrReplacedPart.get(m);
                                                String strTempClonedObject = (String) mTempMap.get(DomainConstants.SELECT_ID);
                                                if (slForReplacedorClonedItemsofCurrentCO.contains(strTempClonedObject))
                                                    strNewImpactedPart = strTempClonedObject;
                                            }
                                        }

                                        if (UIUtil.isNullOrEmpty(strNewImpactedPart)) {
                                            BusinessObject steNewRevPart = domPart.getLastRevision(context);
                                            strNewImpactedPart = steNewRevPart.getObjectId();
                                            if (!strPartID.equalsIgnoreCase(strNewImpactedPart)) {
                                                String strRelIdExists = MqlUtil.mqlCommand(context,
                                                        "print bus " + strNewImpactedPart + " select from[" + strRelID + "| to.id == '" + (String) strAffectedItem + "'].id dump", false, false);
                                                if (UIUtil.isNotNullAndNotEmpty(strRelIdExists)) {
                                                    DomainRelationship.disconnect(context, strRelIdExists);
                                                }
                                            }

                                        }

                                        DomainObject domNewImpactedPart = DomainObject.newInstance(context, strNewImpactedPart);
                                        String strGeometryTypeValue = domNewImplementedItem.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE);

                                        if (TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE.equals(strGeometryTypeValue)) {
                                            if (!ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strChildRequestedChange) || slForConnectCADToPartonReplace.contains(strChangeAffectedRelId)) {
                                                DomainRelationship.connect(context, domNewImpactedPart, strRelID, domNewImplementedItem);
                                            }
                                        } else {
                                            if (!ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strChildRequestedChange) || slForConnectCADToPartonReplace.contains(strChangeAffectedRelId)) {
                                                nCloneCADCount++;
                                                StringBuffer sbCloneCADKey = new StringBuffer();
                                                sbCloneCADKey.append(strPartID);
                                                sbCloneCADKey.append(":");
                                                sbCloneCADKey.append(strNewImpactedPart);
                                                sbCloneCADKey.append(":");
                                                sbCloneCADKey.append(strRelID);
                                                sbCloneCADKey.append(":");
                                                sbCloneCADKey.append(sObjectID);
                                                mpForConnectCADToPart.put(nCloneCADCount, sbCloneCADKey.toString());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // For PARENT Info

                if (!mlImmediateParentInfo.isEmpty()) {
                    Iterator itrParent = mlImmediateParentInfo.iterator();
                    while (itrParent.hasNext()) {
                        Map mpParentInfo = (Map) itrParent.next();
                        String strParentID = (String) mpParentInfo.get(DomainConstants.SELECT_ID);
                        DomainObject domParent = DomainObject.newInstance(context, strParentID);

                        MapList mlActiveCOParent = getConnectedActiveCOWithCA(context, domParent);

                        if (!mlActiveCOParent.isEmpty()) {
                            Iterator itrCO = mlActiveCOParent.iterator();
                            while (itrCO.hasNext()) {
                                Map mpActiveCOParent = (Map) itrCO.next();
                                String strActiveCOID = (String) mpActiveCOParent.get("ChangeOrderId");

                                if (strActiveCOID.equalsIgnoreCase(strCOID)) {

                                    String strParentRequestedChange = (String) mpActiveCOParent.get("RequestedChange");
                                    String strParentRelID = (String) mpParentInfo.get(DomainRelationship.SELECT_ID);
                                    String strParentRelName = (String) mpParentInfo.get(DomainRelationship.SELECT_NAME);

                                    if (ChangeConstants.FOR_REVISE.equalsIgnoreCase(strParentRequestedChange)) {

                                        // No new Connection on Parent side Because , First Child Iterate then Parent so Parent Implemented Item still not created yet.
                                        StringBuffer sbKey = new StringBuffer();
                                        sbKey.append(strParentID);
                                        sbKey.append(":");
                                        sbKey.append(strOriginalObjID);
                                        mpForChildParentConnect.put(sbKey.toString(), sObjectID);

                                        // TIGTK-15701 START
                                        if (strParentRelName.equalsIgnoreCase(TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING)) {
                                            BusinessObject boLatestRevisionParent = domParent.getLastRevision(context);
                                            String strLatestRevisionParent = boLatestRevisionParent.getObjectId();

                                            DomainObject domParentLatestRevision = DomainObject.newInstance(context, strLatestRevisionParent);
                                            DomainObject domParentLatestVersionRevision = DomainObject.newInstance(context,
                                                    (String) domParentLatestRevision.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id"));

                                            if (!strParentID.equalsIgnoreCase(strLatestRevisionParent)) {
                                                String strConnectedDerivedObjId = domNewImplementedItem.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].from.id");
                                                String strRelIdExists = MqlUtil.mqlCommand(context,
                                                        "print bus " + strLatestRevisionParent + " select from[" + strParentRelName + "| to.id == '" + strConnectedDerivedObjId + "'].id dump", false,
                                                        false);

                                                if (UIUtil.isNotNullAndNotEmpty(strRelIdExists)) {
                                                    DomainRelationship.disconnect(context, strRelIdExists);
                                                }
                                                DomainRelationship domRelID = DomainRelationship.connect(context, domParentLatestRevision, strParentRelName, domNewImplementedItem);
                                                String strParentPolicy = (String) domNewImplementedItem.getInfo(context, DomainConstants.SELECT_POLICY);
                                                if (strParentPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT) && strNewImplementedItemPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                                                    connectLatestVersionObject(context, domParentLatestVersionRevision, domNewImplementedItem, strParentRelName);
                                                }
                                                cloneTechnicalDiversity(context, strParentRelID, domRelID, DomainConstants.EMPTY_STRING);
                                                setAttributesOnClonedCAD(context, domParentLatestRevision, domNewImplementedItem, domRelID, strParentRelName);
                                            }
                                        }
                                        // TIGTK-15701 END

                                    } else if (ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strParentRequestedChange)) {

                                        DomainRelationship.setToObject(context, strParentRelID, domNewImplementedItem);

                                        // STARTTTTTTTTTTTTTTTT
                                        // Parent -> For Release and Child -> For clone in this case strucutre is not opened in catia becasue in latest version object not connected
                                        // and attribute is not set so below code is added

                                        String strLatestVersionOfParent = domParent.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

                                        String strLatestVersionOfNewImplementedItem = domNewImplementedItem.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

                                        String strLatestVersionOfOriginalObj = domOriginalObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

                                        String strVersionObjConnectionId = "";

                                        if (UIUtil.isNotNullAndNotEmpty(strLatestVersionOfOriginalObj) && UIUtil.isNotNullAndNotEmpty(strLatestVersionOfParent)) {
                                            strVersionObjConnectionId = MqlUtil.mqlCommand(context, "print bus " + strLatestVersionOfParent + " select from["
                                                    + TigerConstants.RELATIONSHIP_CADSUBCOMPONENT + "| to.id == '" + strLatestVersionOfOriginalObj + "'].id dump", false, false);
                                        }

                                        if (UIUtil.isNotNullAndNotEmpty(strVersionObjConnectionId)) {
                                            DomainObject domLatestVersionOfNewImplementedItem = DomainObject.newInstance(context, strLatestVersionOfNewImplementedItem);
                                            DomainRelationship.setToObject(context, strVersionObjConnectionId, domLatestVersionOfNewImplementedItem);
                                            DomainRelationship domParentRelID = DomainRelationship.newInstance(context, strParentRelID);
                                            setAttributesOnClonedCAD(context, domParent, domNewImplementedItem, domParentRelID, TigerConstants.RELATIONSHIP_CADSUBCOMPONENT);

                                        }
                                        // ENDDDDDDDDDDDDDDDDDDDDDD

                                    } else if (TigerConstants.FOR_CLONE.equalsIgnoreCase(strParentRequestedChange)) {
                                        // String strConnectedDerivedObjId = domParent.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].to.id");
                                        String strWhere = "current!=" + TigerConstants.STATE_PSS_CANCELPART_CANCELLED;
                                        String strConnectedDerivedObjId = DomainConstants.EMPTY_STRING;
                                        StringList slObjectSelects = new StringList(DomainConstants.SELECT_ID);
                                        slObjectSelects.add(DomainConstants.SELECT_CURRENT);
                                        slObjectSelects.add(DomainConstants.SELECT_POLICY);

                                        MapList mlClonedOrReplacedPart = domParent.getRelatedObjects(context, strOriginalObjectRelationshipName, strType, slObjectSelects, null, false, true, (short) 1,
                                                strWhere, null, 0);
                                        if (!mlClonedOrReplacedPart.isEmpty()) {

                                            for (int m = 0; m < mlClonedOrReplacedPart.size(); m++) {
                                                Map mTempMap = (Map) mlClonedOrReplacedPart.get(m);
                                                String strTempClonedObject = (String) mTempMap.get(DomainConstants.SELECT_ID);
                                                if (slForReplacedorClonedItemsofCurrentCO.contains(strTempClonedObject))
                                                    strConnectedDerivedObjId = strTempClonedObject;
                                            }
                                        }
                                        if (strParentRelName.equalsIgnoreCase(TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING)) {
                                            if (UIUtil.isNotNullAndNotEmpty(strConnectedDerivedObjId)) {
                                                String strRelIdExists = MqlUtil.mqlCommand(context,
                                                        "print bus " + strConnectedDerivedObjId + " select from[" + strParentRelName + "| to.id == '" + strOriginalObjID + "'].id dump", false, false);
                                                if (UIUtil.isNotNullAndNotEmpty(strRelIdExists)) {
                                                    DomainRelationship.disconnect(context, strRelIdExists);
                                                    DomainRelationship domRelID = DomainRelationship.connect(context, new DomainObject(strConnectedDerivedObjId), strParentRelName,
                                                            domNewImplementedItem);
                                                }
                                            }

                                        }

                                    }
                                }
                            }
                        }
                    }
                }
                // For Child Info
                if (!mlImmediateChildInfo.isEmpty()) {
                    Iterator itrChild = mlImmediateChildInfo.iterator();
                    while (itrChild.hasNext()) {
                        Map mpChildInfo = (Map) itrChild.next();

                        String strChildID = (String) mpChildInfo.get(DomainConstants.SELECT_ID);
                        DomainObject domChild = DomainObject.newInstance(context, strChildID);
                        String strNameChild = domChild.getInfo(context, DomainConstants.SELECT_NAME);
                        String strChildRelName = (String) mpChildInfo.get(DomainRelationship.SELECT_NAME);
                        String strChildRelID = (String) mpChildInfo.get(DomainRelationship.SELECT_ID);

                        MapList mlActiveCOChild = getConnectedActiveCOWithCA(context, domChild);
                        if (!mlActiveCOChild.isEmpty()) {
                            Iterator itrCOChild = mlActiveCOChild.iterator();
                            StringList slReviseChildIds = new StringList();

                            while (itrCOChild.hasNext()) {
                                boolean bflag = false;
                                Map mpChildCO = (Map) itrCOChild.next();
                                String strChangeAffectedItemRelID = (String) mpChildCO.get("ChangeAffectedRelId");
                                String strActiveChildCOID = (String) mpChildCO.get("ChangeOrderId");
                                String strChildRequestedChange = (String) mpChildCO.get("RequestedChange");
                                String strChangeAffectedRelId = (String) mpChildCO.get("ChangeAffectedRelId");

                                String strWhere = "current!=" + TigerConstants.STATE_PSS_CANCELPART_CANCELLED;
                                String strChildPolicy = DomainConstants.EMPTY_STRING;
                                String strNewClonedItem = DomainConstants.EMPTY_STRING;
                                StringList slObjectSelects = new StringList(DomainConstants.SELECT_ID);
                                slObjectSelects.add(DomainConstants.SELECT_CURRENT);
                                slObjectSelects.add(DomainConstants.SELECT_POLICY);

                                MapList mlClonedOrReplacedPart = domChild.getRelatedObjects(context, strOriginalObjectRelationshipName, strType, slObjectSelects, null, false, true, (short) 1,
                                        strWhere, null, 0);
                                if (!mlClonedOrReplacedPart.isEmpty()) {
                                    for (int m = 0; m < mlClonedOrReplacedPart.size(); m++) {
                                        Map mTempMap = (Map) mlClonedOrReplacedPart.get(m);
                                        String strTempClonedObject = (String) mTempMap.get(DomainConstants.SELECT_ID);
                                        if (slForReplacedorClonedItemsofCurrentCO.contains(strTempClonedObject)) {
                                            strNewClonedItem = strTempClonedObject;
                                            strChildPolicy = (String) mTempMap.get(DomainConstants.SELECT_POLICY);
                                        }
                                    }
                                }
                                if (strActiveChildCOID.equalsIgnoreCase(strCOID)) {
                                    if (TigerConstants.FOR_CLONE.equalsIgnoreCase(strChildRequestedChange) || TigerConstants.FOR_REPLACE.equalsIgnoreCase(strChildRequestedChange)
                                            || ChangeConstants.FOR_REVISE.equalsIgnoreCase(strChildRequestedChange)) {

                                        bflag = true;
                                        // strImplementItem = domChild.getInfo(context, "from[" + strOriginalObjectRelationshipName + "].to.id");

                                        if (UIUtil.isNullOrEmpty(strNewClonedItem)) {
                                            BusinessObject lastRevObj = domChild.getLastRevision(context);
                                            strNewClonedItem = lastRevObj.getObjectId();
                                            slReviseChildIds.add(strNewClonedItem);
                                        }
                                        DomainObject domChildImplementItem = DomainObject.newInstance(context, strNewClonedItem);

                                        StringList slObjSelectList = new StringList(2);
                                        slObjSelectList.add(DomainConstants.SELECT_POLICY);
                                        slObjSelectList.add(DomainConstants.SELECT_NAME);

                                        Map mpObjInfo = domChildImplementItem.getInfo(context, slObjSelectList);

                                        String StrImplementedChildItem = (String) mpObjInfo.get(DomainConstants.SELECT_NAME);
                                        String strImplementedItemChildPolicy = (String) mpObjInfo.get(DomainConstants.SELECT_POLICY);

                                        // String StrImplementedChildItem = domChildImplementItem.getInfo(context, DomainConstants.SELECT_NAME);
                                        String strRelIdExists = MqlUtil.mqlCommand(context,
                                                "print bus " + sObjectID + " select from[" + strChildRelName + "| to.id == '" + strNewClonedItem + "'].id dump", false, false);
                                        DomainRelationship domRelID = null;
                                        if (UIUtil.isNullOrEmpty(strRelIdExists)) {
                                            domRelID = DomainRelationship.connect(context, domNewImplementedItem, strChildRelName, domChildImplementItem);
                                            if (strImplementedItemChildPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT) && strNewImplementedItemPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                                                connectLatestVersionObject(context, domNewImplementedItemLatestVersion, domChildImplementItem, strChildRelName);
                                            }

                                            if (TigerConstants.FOR_CLONE.equalsIgnoreCase(strRequestedChange) || TigerConstants.FOR_REPLACE.equalsIgnoreCase(strRequestedChange)) {
                                                cloneTechnicalDiversity(context, strChildRelID, domRelID, strChangeAffectedItemRelID);
                                            } else if (ChangeConstants.FOR_REVISE.equalsIgnoreCase(strChildRequestedChange)) {
                                                cloneTechnicalDiversity(context, strChildRelID, domRelID, DomainConstants.EMPTY_STRING);
                                            }
                                            setAttributesOnClonedCAD(context, domNewImplementedItem, domChildImplementItem, domRelID, strChildRelName);
                                        }

                                    }
                                    if (ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strChildRequestedChange)) {
                                        break;
                                    }

                                    if (ChangeConstants.FOR_REVISE.equalsIgnoreCase(strChildRequestedChange)) {

                                    } else if (ChangeConstants.FOR_RELEASE.equalsIgnoreCase(strChildRequestedChange)) {
                                        DomainRelationship.setFromObject(context, strChildRelID, domNewImplementedItem);
                                    }
                                } else {

                                    if (UIUtil.isNullOrEmpty(strNewClonedItem)) {
                                        BusinessObject lastRevObj = domChild.getLastRevision(context);
                                        String strNewReviseItem = lastRevObj.getObjectId();
                                        if (mlActiveCOChild.size() == 1) {
                                            bflag = true;
                                        } else if (mlActiveCOChild.size() > 1 && !slForCurrentCOAffectedItems.contains(strChildID)) {
                                            bflag = true;
                                        }
                                        // if (!slReviseChildIds.contains(strNewReviseItem) && bflag == true)
                                        if (bflag == true) {
                                            String strRelIdExists = MqlUtil.mqlCommand(context,
                                                    "print bus " + sObjectID + " select from[" + strChildRelName + "| to.id == '" + strChildID + "'].id dump", false, false);

                                            if (UIUtil.isNullOrEmpty(strRelIdExists)) {
                                                DomainRelationship domRelID = DomainRelationship.connect(context, domNewImplementedItem, strChildRelName, domChild);
                                                if (strChildPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT) && strNewImplementedItemPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                                                    connectLatestVersionObject(context, domNewImplementedItemLatestVersion, domChild, strChildRelName);
                                                }
                                                cloneTechnicalDiversity(context, strChildRelID, domRelID, DomainConstants.EMPTY_STRING);
                                                setAttributesOnClonedCAD(context, domNewImplementedItem, domChild, domRelID, strChildRelName);
                                            }
                                        }
                                    }
                                }

                            }

                        } else {
                            // TIGTK-15840 START
                            DomainRelationship domRelID = DomainRelationship.connect(context, domNewImplementedItem, strChildRelName, domChild);
                            cloneTechnicalDiversity(context, strChildRelID, domRelID, DomainConstants.EMPTY_STRING);
                            // TIGTK-15840 END
                        }
                    }
                }
            }

            logger.debug("PSS_enoECMChangeOrder : performActionOnReplaceOrClone : END");

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : performActionOnReplaceOrClone : ERROR", ex);
            throw ex;
        }

    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @throws Exception
     */

    public void cloneTechnicalDiversity(Context context, String strParentRelID, DomainRelationship domRelID, String strChangeAffectedItemRelID) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : cloneTechnicalDiversity : START");
        try {
            StringList lstExcludeAttribute = new StringList();
            String strAttrCloneTechDiversity = DomainConstants.EMPTY_STRING;
            if (UIUtil.isNotNullAndNotEmpty(strChangeAffectedItemRelID)) {
                strAttrCloneTechDiversity = getValueOfCloneAttribute(context, strChangeAffectedItemRelID, TigerConstants.ATTRIBUTE_PSS_CLONETECHNICALDIVERSITY);
            }

            if (UIUtil.isNullOrEmpty(strChangeAffectedItemRelID) || !TigerConstants.ATTRIBUTE_PSS_CLONETECHNICALDIVERSITY_RANGE_YES.equals(strAttrCloneTechDiversity)) {

                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_ISVPLMVISIBLE);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_PSS_REPLACED_WITH_LATEST_REVISION);

                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDIMPACTINGCRITERIA);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDCRITERIADICTIONARY);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYTYPES);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYEXPRESSION);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYCOMPILEDFORM);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITY_VARIABLE_INDEXES);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYEXPRESSIONBINARY);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYPROPOSEDEXPRESSION);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_EFFECTIVITYORDEREDCRITERIA);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_PSS_SIMPLEREFFECTIVITYEXPRESSION);
                lstExcludeAttribute.add(TigerConstants.ATTRIBUTE_PSS_CUSTOMEFFECTIVITYEXPRESSION);

            } else {
                String strModRelMQL = "mod connection " + domRelID + " add interface \"Effectivity Framework\"";
                MqlUtil.mqlCommand(context, strModRelMQL);
            }
            PSS_emxPart_mxJPO part = new PSS_emxPart_mxJPO(context, null);
            // TIGTK-14855-START
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            part.copySourceRelDataToNewRel(context, strParentRelID, domRelID.toString(), lstExcludeAttribute, true);
            ContextUtil.popContext(context);
            // TIGTK-14855-END
            logger.debug("PSS_enoECMChangeOrder : cloneTechnicalDiversity : END");

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : cloneTechnicalDiversity : ERROR", ex);
            throw ex;
        }

    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @throws Exception
     */

    public void cloneColorDiversityAndReferenceDoc(Context context, String strCOId, DomainObject domOriginalObject, DomainObject domNewImplementedItem) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : cloneColorDiversityAndReferenceDoc : START");
        try {
            String strChangeAffectedItemRelID = DomainConstants.EMPTY_STRING;
            MapList mlActiveCOParent = getConnectedActiveCOWithCA(context, domOriginalObject);

            if (!mlActiveCOParent.isEmpty()) {
                Iterator itrCO = mlActiveCOParent.iterator();
                while (itrCO.hasNext()) {
                    Map mpActiveCOParent = (Map) itrCO.next();
                    String strActiveCOID = (String) mpActiveCOParent.get("ChangeOrderId");
                    if (strActiveCOID.equalsIgnoreCase(strCOId)) {
                        strChangeAffectedItemRelID = (String) mpActiveCOParent.get("ChangeAffectedRelId");
                    }
                }
            }

            String strAttrCloneColorDiversity = getValueOfCloneAttribute(context, strChangeAffectedItemRelID, TigerConstants.ATTRIBUTE_PSS_CLONECOLORDIVERSITY);
            String strAttrKeepReferenceDocument = getValueOfCloneAttribute(context, strChangeAffectedItemRelID, TigerConstants.ATTRIBUTE_PSS_KEEPREFERENCEDOCUMENT);

            if (TigerConstants.ATTRIBUTE_PSS_CLONECOLORDIVERSITY_RANGE_YES.equalsIgnoreCase(strAttrCloneColorDiversity)) {
                StringList lstselectStmts = new StringList();
                lstselectStmts.addElement(DomainConstants.SELECT_ID);
                lstselectStmts.addElement(DomainConstants.SELECT_NAME);

                StringList lstrelStmts = new StringList();

                MapList mlColorDiversityObject = domOriginalObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_COLORLIST, TigerConstants.TYPE_PSS_COLOROPTION, lstselectStmts,
                        lstrelStmts, false, true, (short) 1, null, null, 0);

                Iterator itrcolorDiversity = mlColorDiversityObject.iterator();
                while (itrcolorDiversity.hasNext()) {
                    Map<String, String> mapColordiversityObject = (Map<String, String>) itrcolorDiversity.next();
                    String strColorObjectId = (String) mapColordiversityObject.get(DomainConstants.SELECT_ID);
                    String strColorName = (String) mapColordiversityObject.get(DomainConstants.SELECT_NAME);
                    DomainObject domColorDiversitySourceObj = DomainObject.newInstance(context, strColorObjectId);
                    DomainRelationship.connect(context, domNewImplementedItem, TigerConstants.RELATIONSHIP_PSS_COLORLIST, domColorDiversitySourceObj);

                }
            }

            if (!TigerConstants.ATTRIBUTE_PSS_KEEPREFERENCEDOCUMENT_RANGE_YES.equalsIgnoreCase(strAttrKeepReferenceDocument)) {
                StringList lstselectStmts = new StringList(1);
                lstselectStmts.addElement(DomainConstants.SELECT_ID);

                StringList lstrelStmts = new StringList();
                lstrelStmts.addElement(DomainRelationship.SELECT_ID);

                MapList mlReferenceDocument = domNewImplementedItem.getRelatedObjects(context, RELATIONSHIP_REFERENCE_DOCUMENT, CommonDocument.DEFAULT_DOCUMENT_TYPE, lstselectStmts, lstrelStmts,
                        false, true, (short) 1, null, null, 0);
                String relIdList[] = new String[mlReferenceDocument.size()];
                Iterator itrReferenceDocument = mlReferenceDocument.iterator();
                int i = 0;
                while (itrReferenceDocument.hasNext()) {
                    Map<String, String> mapReferenceDocumentObject = (Map<String, String>) itrReferenceDocument.next();
                    String strRelId = (String) mapReferenceDocumentObject.get(DomainRelationship.SELECT_ID);
                    relIdList[i] = strRelId;
                    i++;
                }
                DomainRelationship.disconnect(context, relIdList);

            }

            logger.debug("PSS_enoECMChangeOrder : cloneColorDiversityAndReferenceDoc : END");

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : cloneColorDiversityAndReferenceDoc : ERROR", ex);
            throw ex;
        }
    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @throws Exception
     */

    public String getValueOfCloneAttribute(Context context, String strChangeAffectedItemRelID, Object attrName) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : getValueOfCloneAttribute : START");
        try {
            DomainRelationship domChangeAffectedItems = DomainRelationship.newInstance(context, strChangeAffectedItemRelID);
            Map mpAttributeMap = domChangeAffectedItems.getAttributeMap(context);
            String strAttributeValue = (String) mpAttributeMap.get(attrName);
            logger.debug("PSS_enoECMChangeOrder : getValueOfCloneAttribute : END");
            return strAttributeValue;

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : getValueOfCloneAttribute : ERROR", ex);
            throw ex;
        }

    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @throws Exception
     */

    private MapList getConnectedActiveCOWithCA(Context context, DomainObject domImplObj) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : getConnectedActiveCOWithCA : START");
        try {
            MapList mlActiveMCO = new MapList();

            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.add(DomainObject.SELECT_ID);

            StringList slRelationshipSelectStatement = new StringList();
            slRelationshipSelectStatement.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);
            slRelationshipSelectStatement.addElement(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);

            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            MapList mlCAItem = domImplObj.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM, TigerConstants.TYPE_CHANGEACTION, slObjSelectStmts,
                    slRelationshipSelectStatement, true, false, (short) 1, null, null, 0);

            if (!mlCAItem.isEmpty()) {

                Iterator itrCA = mlCAItem.iterator();
                while (itrCA.hasNext()) {
                    HashMap mpForCO = new HashMap<>();
                    Map mCAObj = (Map) itrCA.next();
                    String strCAID = (String) mCAObj.get(DomainConstants.SELECT_ID);
                    String strChangeAffectedRelId = (String) mCAObj.get(DomainRelationship.SELECT_RELATIONSHIP_ID);
                    String strRequestedChange = (String) mCAObj.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                    mpForCO.put("RequestedChange", strRequestedChange);
                    mpForCO.put("ChangeAffectedRelId", strChangeAffectedRelId);

                    DomainObject domCAObject = DomainObject.newInstance(context, strCAID);

                    MapList mlConnectedCOs = domCAObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_CHANGEACTION, TigerConstants.TYPE_PSS_CHANGEORDER, slObjSelectStmts, null, true, false,
                            (short) 1, null, null, 0);
                    if (!mlConnectedCOs.isEmpty()) {

                        Iterator itrCO = mlConnectedCOs.iterator();
                        while (itrCO.hasNext()) {
                            Map mCOObj = (Map) itrCO.next();
                            String strCOID = (String) mCOObj.get(DomainConstants.SELECT_ID);
                            String strCOName = (String) mCOObj.get(DomainConstants.SELECT_NAME);
                            mpForCO.put("ChangeOrderId", strCOID);
                        }
                    }
                    mlActiveMCO.add(mpForCO);
                }

            }
            ContextUtil.popContext(context);
            logger.debug("PSS_enoECMChangeOrder : getConnectedActiveCOWithCA : END");
            return mlActiveMCO;

        } catch (Exception e) {
            logger.error("Error in PSS_enoECMChangeOrder : getConnectedActiveCOWithCA : ", e);
            throw e;
        }

    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @throws Exception
     */

    public void connectReviseItemWithChild(Context context, String strAffectedItemID, String strNewReviseItem, String strCOId) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : connectReviseItemWithChild : START");
        try {
            int iFlag = 0;
            StringList slStateCheckList = new StringList();
            slStateCheckList.addElement(TigerConstants.STATE_PSS_ECPART_PRELIMINARY);
            slStateCheckList.addElement(TigerConstants.STATE_PART_APPROVED);
            slStateCheckList.addElement(TigerConstants.STATE_PART_REVIEW);
            slStateCheckList.addElement(TigerConstants.STATE_PART_RELEASE);
            slStateCheckList.addElement(TigerConstants.STATE_INWORK_CAD_OBJECT);
            slStateCheckList.addElement(TigerConstants.STATE_CAD_APPROVED);
            slStateCheckList.addElement(TigerConstants.STATE_CAD_REVIEW);
            slStateCheckList.addElement(TigerConstants.STATE_RELEASED_CAD_OBJECT);

            StringBuffer strExpandRelationshipName = new StringBuffer();
            DomainObject domChangeAffectedItems = DomainObject.newInstance(context, strAffectedItemID);
            String strType = domChangeAffectedItems.getInfo(context, DomainObject.SELECT_TYPE);

            DomainObject domNewReviseItem = DomainObject.newInstance(context, strNewReviseItem);
            if (DomainConstants.TYPE_PART.equals(strType)) {
                strType = DomainConstants.TYPE_PART;
                strExpandRelationshipName.append(DomainConstants.RELATIONSHIP_EBOM);
            } else {
                strType = DomainConstants.QUERY_WILDCARD;
                // TIGTK-14669 START
                strExpandRelationshipName.append(TigerConstants.RELATIONSHIP_CADSUBCOMPONENT);
                strExpandRelationshipName.append(",");
                strExpandRelationshipName.append(TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING);
                // TIGTK-14669 END
            }
            // Get List of Immediate Child

            StringList slRelationshipSelect = new StringList();
            slRelationshipSelect.add(DomainRelationship.SELECT_ID);
            slRelationshipSelect.add(DomainRelationship.SELECT_NAME);

            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_ID);

            MapList mlImmediateChildInfo = domChangeAffectedItems.getRelatedObjects(context, strExpandRelationshipName.toString(), strType, slObjectSelect, slRelationshipSelect, false, true,
                    (short) 1, null, null, 0);
            MapList mlImmediateParentInfo = domChangeAffectedItems.getRelatedObjects(context, strExpandRelationshipName.toString(), strType, slObjectSelect, slRelationshipSelect, true, false,
                    (short) 1, null, null, 0);
            String strLatestVersionItemID = DomainConstants.EMPTY_STRING;
            String strPolicyOfReviseItem = DomainConstants.EMPTY_STRING;
            StringList busSelect = new StringList(DomainObject.SELECT_POLICY);
            busSelect.add("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
            Map<String, Object> mapNewReviseItem = domNewReviseItem.getInfo(context, busSelect);
            if (!mapNewReviseItem.isEmpty()) {
                strPolicyOfReviseItem = (String) mapNewReviseItem.get(DomainConstants.SELECT_POLICY);
                if (strPolicyOfReviseItem.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                    strLatestVersionItemID = (String) mapNewReviseItem.get("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
                }
            }

            // get Immediate Child Info

            if (!mlImmediateChildInfo.isEmpty()) {
                Iterator itrChild = mlImmediateChildInfo.iterator();
                while (itrChild.hasNext()) {
                    Map mpChildInfo = (Map) itrChild.next();

                    String strChildID = (String) mpChildInfo.get(DomainConstants.SELECT_ID);
                    String strChildRelID = (String) mpChildInfo.get(DomainRelationship.SELECT_ID);
                    String strChildRelName = (String) mpChildInfo.get(DomainRelationship.SELECT_NAME);
                    DomainObject domChild = DomainObject.newInstance(context, strChildID);
                    String strNameChild = domChild.getInfo(context, DomainConstants.SELECT_NAME);

                    Boolean bConnectChildWithDiffCO = false;

                    MapList mlActiveCOChild = getConnectedActiveCOWithCA(context, domChild);

                    if (!mlActiveCOChild.isEmpty()) {
                        Iterator itrCOChild = mlActiveCOChild.iterator();
                        StringList slCOList = new StringList();
                        while (itrCOChild.hasNext()) {
                            Map mpChildCO = (Map) itrCOChild.next();
                            String strActiveChildCOID = (String) mpChildCO.get("ChangeOrderId");
                            slCOList.addElement(strActiveChildCOID);
                            if (strActiveChildCOID.equalsIgnoreCase(strCOId)) {
                                String strChangeAffectedItemRelID = (String) mpChildCO.get("ChangeAffectedRelId");
                                String strChildRequestedChange = (String) mpChildCO.get("RequestedChange");

                                String strImplementItem = DomainConstants.EMPTY_STRING;

                                // If The parent has Requested Change is for "For Clone /For replace" then Check for Maplist BR-103 : START
                                StringBuffer sbKey = new StringBuffer();
                                sbKey.append(strAffectedItemID);
                                sbKey.append(":");
                                sbKey.append(strChildID);

                                if (mpForChildParentConnect.containsKey(sbKey.toString())) {
                                    strImplementItem = mpForChildParentConnect.get(sbKey.toString());
                                    iFlag = 1;
                                }
                                // BR-103 : END
                                if (UIUtil.isNullOrEmpty(strImplementItem)) {
                                    BusinessObject lastRevObj = domChild.getLastRevision(context);
                                    strImplementItem = lastRevObj.getObjectId();
                                }
                                DomainObject domImplementItem = DomainObject.newInstance(context, strImplementItem);
                                String strState = DomainConstants.EMPTY_STRING;
                                String strPolicy = DomainConstants.EMPTY_STRING;

                                StringList slBusSelect = new StringList(DomainConstants.SELECT_CURRENT);
                                slBusSelect.add(DomainConstants.SELECT_POLICY);

                                Map<String, Object> mapImplementItemInfo = (Map<String, Object>) domImplementItem.getInfo(context, slBusSelect);
                                if (!mapImplementItemInfo.isEmpty()) {
                                    strState = (String) mapImplementItemInfo.get(DomainConstants.SELECT_CURRENT);
                                    strPolicy = (String) mapImplementItemInfo.get(DomainConstants.SELECT_POLICY);
                                }
                                if (iFlag == 1) {
                                    // TIGTK-15701 START
                                    if (!ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strChildRequestedChange)) {
                                        DomainRelationship domRelID = DomainRelationship.connect(context, domNewReviseItem, strChildRelName, domImplementItem);
                                        if (TigerConstants.FOR_CLONE.equalsIgnoreCase(strChildRequestedChange) || TigerConstants.FOR_REPLACE.equalsIgnoreCase(strChildRequestedChange)) {
                                            cloneTechnicalDiversity(context, strChildRelID, domRelID, strChangeAffectedItemRelID);
                                        }
                                        if (strPolicyOfReviseItem.equals(TigerConstants.POLICY_PSS_CADOBJECT) && strPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                                            DomainObject domLatestVersionItemID = DomainObject.newInstance(context, strLatestVersionItemID);
                                            connectLatestVersionObject(context, domLatestVersionItemID, domImplementItem, strChildRelName);
                                            setAttributesOnClonedCAD(context, domNewReviseItem, domImplementItem, domRelID, strChildRelName);
                                        }
                                        // TIGTK-15701 END
                                    }

                                } else {
                                    if (slStateCheckList.contains(strState)) {

                                        if (!ChangeConstants.FOR_OBSOLESCENCE.equalsIgnoreCase(strChildRequestedChange)) {
                                            DomainRelationship domRelID = DomainRelationship.connect(context, domNewReviseItem, strChildRelName, domImplementItem);
                                            // For Revise Technical Divercity not Required to copy on new Object hence passing Empty Sting
                                            cloneTechnicalDiversity(context, strChildRelID, domRelID, DomainConstants.EMPTY_STRING);

                                            if (strPolicyOfReviseItem.equals(TigerConstants.POLICY_PSS_CADOBJECT) && strPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                                                DomainObject domLatestVersionItemID = DomainObject.newInstance(context, strLatestVersionItemID);
                                                connectLatestVersionObject(context, domLatestVersionItemID, domImplementItem, strChildRelName);
                                                setCADAttributes(context, domNewReviseItem, domImplementItem, domRelID, strChildRelName);

                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!slCOList.contains(strCOId)) {
                            bConnectChildWithDiffCO = true;
                        }
                    } else {
                        bConnectChildWithDiffCO = true;
                    }
                    if (bConnectChildWithDiffCO == true) {
                        BusinessObject lastRevObj = domChild.getLastRevision(context);
                        String strImplementItem = lastRevObj.getObjectId();
                        DomainObject domImplementItem = DomainObject.newInstance(context, strImplementItem);
                        String strState = DomainConstants.EMPTY_STRING;
                        String strPolicy = DomainConstants.EMPTY_STRING;

                        StringList slBusSelect = new StringList(DomainConstants.SELECT_CURRENT);
                        slBusSelect.add(DomainConstants.SELECT_POLICY);

                        Map<String, Object> mapImplementItemInfo = (Map<String, Object>) domImplementItem.getInfo(context, slBusSelect);
                        if (!mapImplementItemInfo.isEmpty()) {
                            strState = (String) mapImplementItemInfo.get(DomainConstants.SELECT_CURRENT);
                            strPolicy = (String) mapImplementItemInfo.get(DomainConstants.SELECT_POLICY);
                        }
                        BusinessObject domImplObj = new BusinessObject(strImplementItem);
                        Access access = domImplObj.getAccessMask(context);

                        if (slStateCheckList.contains(strState) && access.hasToConnectAccess()) {

                            DomainRelationship domRelID = DomainRelationship.connect(context, domNewReviseItem, strChildRelName, domImplementItem);
                            // For Revise Technical Divercity not Required to copy on new Object hence passing Empty Sting
                            cloneTechnicalDiversity(context, strChildRelID, domRelID, DomainConstants.EMPTY_STRING);

                            if (strPolicyOfReviseItem.equals(TigerConstants.POLICY_PSS_CADOBJECT) && strPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                                DomainObject domLatestVersionItemID = DomainObject.newInstance(context, strLatestVersionItemID);
                                connectLatestVersionObject(context, domLatestVersionItemID, domImplementItem, strChildRelName);
                                setCADAttributes(context, domNewReviseItem, domImplementItem, domRelID, strChildRelName);

                            }

                        }

                    }
                }
            }

            // get Immediate Parent Info
            if (!mlImmediateParentInfo.isEmpty()) {
                Iterator itrParent = mlImmediateParentInfo.iterator();
                StringList sldomParents = new StringList();
                while (itrParent.hasNext()) {
                    Map mpParentInfo = (Map) itrParent.next();
                    String strParentID = (String) mpParentInfo.get(DomainConstants.SELECT_ID);
                    DomainObject domParent = DomainObject.newInstance(context, strParentID);
                    String strParentRelID = (String) mpParentInfo.get(DomainRelationship.SELECT_ID);
                    String strParentRelName = (String) mpParentInfo.get(DomainRelationship.SELECT_NAME);
                    Boolean bConnectParentWithDiffCO = false;

                    MapList mlActiveCOParent = getConnectedActiveCOWithCA(context, domParent);
                    if (!mlActiveCOParent.isEmpty()) {
                        Iterator itrCO = mlActiveCOParent.iterator();
                        StringList slCOList = new StringList();
                        while (itrCO.hasNext()) {
                            Map mpActiveCOParent = (Map) itrCO.next();
                            String strActiveCOID = (String) mpActiveCOParent.get("ChangeOrderId");
                            slCOList.addElement(strActiveCOID);
                        }
                        if (!slCOList.contains(strCOId)) {
                            // If Parent is present in current CO then Parent new Revision is not created according to structure
                            bConnectParentWithDiffCO = true;

                        } else {
                            // TIGTK-14669 START
                            BusinessObject boLatestRevisionParent = domParent.getLastRevision(context);
                            String strLatestRevisionParent = boLatestRevisionParent.getObjectId();
                            DomainObject domParentLatestRevision = DomainObject.newInstance(context, strLatestRevisionParent);
                            String strAffectedItemPolicy = (String) domParentLatestRevision.getInfo(context, DomainConstants.SELECT_POLICY);
                            if (!strParentID.equalsIgnoreCase(strLatestRevisionParent) && !strAffectedItemPolicy.equals(TigerConstants.POLICY_PSS_CANCELPART)
                                    && !strAffectedItemPolicy.equals(TigerConstants.POLICY_PSS_CANCELCAD)) {
                                String strPolicy = domParentLatestRevision.getInfo(context, DomainConstants.SELECT_POLICY);
                                DomainRelationship domRelID = DomainRelationship.connect(context, domParentLatestRevision, strParentRelName, domNewReviseItem);
                                cloneTechnicalDiversity(context, strParentRelID, domRelID, DomainConstants.EMPTY_STRING);

                                if (strPolicyOfReviseItem.equals(TigerConstants.POLICY_PSS_CADOBJECT) && strPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                                    DomainObject domLatestVersionItemID = DomainObject.newInstance(context, strLatestVersionItemID);
                                    connectLatestVersionObject(context, domParentLatestRevision, domLatestVersionItemID, strParentRelName);
                                    setCADAttributes(context, domParentLatestRevision, domNewReviseItem, domRelID, strParentRelName);

                                }
                            }
                            // TIGTK-14669 END
                        }
                    } else {
                        bConnectParentWithDiffCO = true;
                    }
                    if (bConnectParentWithDiffCO == true) {
                        BusinessObject lastRevObj = domParent.getLastRevision(context);
                        String strImplementItem = lastRevObj.getObjectId();
                        if (!sldomParents.contains(strImplementItem)) {
                            sldomParents.add(strImplementItem);
                            DomainObject domImplementItem = DomainObject.newInstance(context, strImplementItem);
                            String strState = DomainConstants.EMPTY_STRING;
                            String strPolicy = DomainConstants.EMPTY_STRING;

                            StringList slBusSelect = new StringList(DomainConstants.SELECT_CURRENT);
                            slBusSelect.add(DomainConstants.SELECT_POLICY);

                            Map<String, Object> mapImplementItemInfo = (Map<String, Object>) domImplementItem.getInfo(context, slBusSelect);
                            if (!mapImplementItemInfo.isEmpty()) {
                                strState = (String) mapImplementItemInfo.get(DomainConstants.SELECT_CURRENT);
                                strPolicy = (String) mapImplementItemInfo.get(DomainConstants.SELECT_POLICY);
                            }
                            Access access = domImplementItem.getAccessMask(context);

                            if (access.hasToConnectAccess() && (TigerConstants.STATE_PSS_ECPART_PRELIMINARY.contains(strState) || TigerConstants.STATE_INWORK_CAD_OBJECT.contains(strState))) {
                                DomainRelationship domRelID = DomainRelationship.connect(context, domImplementItem, strParentRelName, domNewReviseItem);

                                // For Revise Technical Divercity not Required to copy on new Object hence passing Empty Sting
                                cloneTechnicalDiversity(context, strParentRelID, domRelID, DomainConstants.EMPTY_STRING);

                                if (strPolicyOfReviseItem.equals(TigerConstants.POLICY_PSS_CADOBJECT) && strPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                                    DomainObject domLatestVersionItemID = DomainObject.newInstance(context, strLatestVersionItemID);
                                    connectLatestVersionObject(context, domImplementItem, domLatestVersionItemID, strParentRelName);
                                    setCADAttributes(context, domImplementItem, domNewReviseItem, domRelID, strParentRelName);

                                }

                                BusinessObject boPrevRevOfRevisedID = domNewReviseItem.getPreviousRevision(context);
                                String strPrevRevOfRevisedID = boPrevRevOfRevisedID.getObjectId();

                                DomainObject domPrevRevOfRevisedID = DomainObject.newInstance(context, strPrevRevOfRevisedID);
                                StringList slConnectedIds = domPrevRevOfRevisedID.getInfoList(context, "to[" + strParentRelName + "].from.id");

                                if (slConnectedIds.contains(strImplementItem)) {
                                    lastRevObj.disconnect(context, new RelationshipType(strParentRelName), true, boPrevRevOfRevisedID);
                                }
                            }
                        }
                    }
                }
            }

            logger.debug("PSS_enoECMChangeOrder : connectReviseItemWithChild : END");

        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : connectReviseItemWithChild : ERROR", ex);
            throw ex;
        }
    }

    /**
     * TIGTK-11675
     * @param context
     * @param args
     * @throws Exception
     */

    public void disconnectOriginalStructWithNewImplementItem(Context context, String strNewReviseItem) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : disconnectOriginalStructWithNewImplementItem : START");
        try {
            StringBuffer strExpandRelationshipName = new StringBuffer();
            StringBuffer strSpecRelationName = new StringBuffer();
            strSpecRelationName.append(RELATIONSHIP_PART_SPECIFICATION);
            strSpecRelationName.append(",");
            strSpecRelationName.append(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
            DomainObject domNewReviseItem = DomainObject.newInstance(context, strNewReviseItem);
            StringList slObjectSelects = new StringList(2);
            slObjectSelects.addElement(DomainConstants.SELECT_POLICY);
            slObjectSelects.addElement(DomainConstants.SELECT_TYPE);
            slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
            // strLatestVersionItemID = (String) mapNewReviseItem.get("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
            Map<String, String> mReviseObjectInfoMap = domNewReviseItem.getInfo(context, slObjectSelects);
            String strRevisedObjectPolicy = mReviseObjectInfoMap.get(DomainConstants.SELECT_POLICY);
            String strType = mReviseObjectInfoMap.get(DomainConstants.SELECT_TYPE);
            String strLatestVersionOfRevisedObj = DomainConstants.EMPTY_STRING;
            if (strRevisedObjectPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                strLatestVersionOfRevisedObj = mReviseObjectInfoMap.get("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
            }

            // TIGTK-14669 START
            if (DomainConstants.TYPE_PART.equals(strType)) {
                strType = DomainConstants.TYPE_PART;
                strExpandRelationshipName.append(DomainConstants.RELATIONSHIP_EBOM);
            } else {
                strType = DomainConstants.QUERY_WILDCARD;
                strExpandRelationshipName.append(TigerConstants.RELATIONSHIP_CADSUBCOMPONENT);
            //TIGTK-17999 START
                strExpandRelationshipName.append(",");
                strExpandRelationshipName.append(TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING);
            //TIGTK-17999 END
            }
            // TIGTK-14669 END
            // Get List of Immediate Child

            StringList slRelationshipSelect = new StringList();
            slRelationshipSelect.add(DomainRelationship.SELECT_ID);

            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_ID);
            slObjectSelect.add(DomainConstants.SELECT_POLICY);

            MapList mlImmediateChildInfo = domNewReviseItem.getRelatedObjects(context, strExpandRelationshipName.toString(), strType, slObjectSelect, slRelationshipSelect, false, true, (short) 1,
                    null, null, 0);
            MapList mlImmediateParentInfo = domNewReviseItem.getRelatedObjects(context, strExpandRelationshipName.toString(), strType, slObjectSelect, slRelationshipSelect, true, false, (short) 1,
                    null, null, 0);
            MapList mlConnectedCancelledSpecifications = domNewReviseItem.getRelatedObjects(context, strSpecRelationName.toString(), DomainConstants.QUERY_WILDCARD, slObjectSelect,
                    slRelationshipSelect, false, true, (short) 1, null, null, 0);
            // get Immediate Child Info
            if (!mlImmediateChildInfo.isEmpty()) {
                Iterator itrChild = mlImmediateChildInfo.iterator();
                while (itrChild.hasNext()) {
                    Map mpChildInfo = (Map) itrChild.next();
                    String strRelId = (String) mpChildInfo.get(DomainRelationship.SELECT_ID);
                    String strChildObjPolicy = (String) mpChildInfo.get(DomainConstants.SELECT_POLICY);
                    String strChildObjId = (String) mpChildInfo.get(DomainConstants.SELECT_ID);
                    DomainObject domChildObj = DomainObject.newInstance(context, strChildObjId);
					// TIGTK-17999 : stembulkar : start
					String strPartId = "";
					// TIGTK-17999 : stembulkar : end
                    if (strChildObjPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT) && strRevisedObjectPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {

                        String strChildLatestVersionId = domChildObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

                        String strRelIdExists = MqlUtil.mqlCommand(context,
                                "print bus " + strLatestVersionOfRevisedObj + " select from[" + TigerConstants.RELATIONSHIP_CADSUBCOMPONENT + "| to.id == '" + strChildLatestVersionId + "'].id dump",
                                false, false);
                        if (UIUtil.isNotNullAndNotEmpty(strRelIdExists)) {
                            MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", strLatestVersionOfRevisedObj, TigerConstants.RELATIONSHIP_CADSUBCOMPONENT, strChildLatestVersionId);
                        }
                        
                        //TIGTK-18183: Start
                        strRelIdExists = MqlUtil.mqlCommand(context,
                                "print bus " + strLatestVersionOfRevisedObj + " select from[" + TigerConstants.RELATIONSHIP_ASSOCIATED_DRAWING + "| to.id == '" + strChildLatestVersionId + "'].id dump",
                                false, false);

                        // TIGTK-17999 : stembulkar : start
						DomainObject dLatestVersionOfRevisedObj = DomainObject.newInstance( context, strChildLatestVersionId );
						String strLatestVersionObjId = dLatestVersionOfRevisedObj.getInfo( context, "from[VersionOf].to.id" );
						DomainObject dLatestVersionObj = DomainObject.newInstance( context, strLatestVersionObjId );
						BusinessObject bObj = dLatestVersionObj.getPreviousRevision( context );
						DomainObject dObject = new DomainObject( bObj ); 
						strPartId = dObject.getInfo( context, "to[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].from.id" );
						
						if ( UIUtil.isNotNullAndNotEmpty(strRelIdExists) &&  !UIUtil.isNotNullAndNotEmpty( strPartId ) ) {
                            MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", strLatestVersionOfRevisedObj, TigerConstants.RELATIONSHIP_ASSOCIATED_DRAWING, strChildLatestVersionId);
                        }
                        //TIGTK-18183: End
                        
                    }

                    BusinessObject lastRevObj = domChildObj.getLastRevision(context);
                    String strImplementItem = lastRevObj.getObjectId();
                    BusinessObject domImplObj = new BusinessObject(strImplementItem);
                    Access access = domImplObj.getAccessMask(context);
                    if (access.hasReadAccess() &&  !UIUtil.isNotNullAndNotEmpty( strPartId )) {
                        DomainRelationship.disconnect(context, strRelId);

                    }
                }
            }
            // TIGTK-14669 START
            if (!mlImmediateParentInfo.isEmpty()) {
                Iterator itrParent = mlImmediateParentInfo.iterator();
                while (itrParent.hasNext()) {
                    Map mpParentInfo = (Map) itrParent.next();
                    String strRelId = (String) mpParentInfo.get(DomainRelationship.SELECT_ID);
                    String strParentObjPolicy = (String) mpParentInfo.get(DomainConstants.SELECT_POLICY);
					// TIGTK-17999 : stembulkar : start
					String strPartId = "";
					// TIGTK-17999 : stembulkar : end
                    if (strParentObjPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT) && strRevisedObjectPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                        String strParentObjId = (String) mpParentInfo.get(DomainConstants.SELECT_ID);
                        DomainObject domParentObj = DomainObject.newInstance(context, strParentObjId);
                        String strParentLatestVersionId = domParentObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
                        String strRelIdExists = MqlUtil.mqlCommand(context,
                                "print bus " + strParentLatestVersionId + " select from[" + TigerConstants.RELATIONSHIP_CADSUBCOMPONENT + "| to.id == '" + strLatestVersionOfRevisedObj + "'].id dump",
                                false, false);
                        if (UIUtil.isNotNullAndNotEmpty(strRelIdExists)) {
                            MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", strParentLatestVersionId, TigerConstants.RELATIONSHIP_CADSUBCOMPONENT, strLatestVersionOfRevisedObj);
                        }

                        //TIGTK-18183: Start
                        strRelIdExists = MqlUtil.mqlCommand(context,
                                "print bus " + strParentLatestVersionId + " select from[" + TigerConstants.RELATIONSHIP_ASSOCIATED_DRAWING + "| to.id == '" + strLatestVersionOfRevisedObj + "'].id dump",
                                false, false);
                        // TIGTK-17999 : stembulkar : start
						DomainObject dLatestVersionOfRevisedObj = DomainObject.newInstance( context, strLatestVersionOfRevisedObj );
						String strLatestVersionObjId = dLatestVersionOfRevisedObj.getInfo( context, "from[VersionOf].to.id" );
						DomainObject dLatestVersionObj = DomainObject.newInstance( context, strLatestVersionObjId );
						BusinessObject bObj = dLatestVersionObj.getPreviousRevision( context );
						DomainObject dObject = new DomainObject( bObj ); 
						strPartId = dObject.getInfo( context, "to[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].from.id" );
						
						if ( UIUtil.isNotNullAndNotEmpty(strRelIdExists) &&  !UIUtil.isNotNullAndNotEmpty( strPartId ) ) {
                            MqlUtil.mqlCommand(context, "disconnect bus $1 relationship $2 to $3", strParentLatestVersionId, TigerConstants.RELATIONSHIP_ASSOCIATED_DRAWING, strLatestVersionOfRevisedObj);
                        }
                        // TIGTK-17999 : stembulkar : end
                        
                    }
                    if( !UIUtil.isNotNullAndNotEmpty( strPartId ) ) {
						DomainRelationship.disconnect(context, strRelId);
					}
                }
            }
            if (!mlConnectedCancelledSpecifications.isEmpty()) {
                Iterator itrChild = mlConnectedCancelledSpecifications.iterator();
                while (itrChild.hasNext()) {
                    Map mpChildInfo = (Map) itrChild.next();
                    String strRelId = (String) mpChildInfo.get(DomainRelationship.SELECT_ID);
                    String strChildObjPolicy = (String) mpChildInfo.get(DomainConstants.SELECT_POLICY);
                    if (strChildObjPolicy.equals(TigerConstants.POLICY_PSS_CANCELCAD)) {
                        DomainRelationship.disconnect(context, strRelId);
                    }
                }
            }
            // TIGTK-14669 END
            logger.debug("PSS_enoECMChangeOrder : disconnectOriginalStructWithNewImplementItem : END");

        } catch (RuntimeException ex) {
            logger.error("Error in PSS_enoECMChangeOrder : disconnectOriginalStructWithNewImplementItem : ERROR", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeOrder : disconnectOriginalStructWithNewImplementItem : ERROR", ex);
            throw ex;
        }
    }

    /**
     * This method is used for connect PartSpecification with new Clone of CAD PCM
     * @param context
     * @param args
     *            -CAD object ID
     * @throws Exception
     */
    public void propagatePartSpecificationToNewCloneObj(Context context, String[] args) throws Exception {
        try {
            // StringList BusSelectList = new StringList(DomainConstants.SELECT_ID);

            String strCAD = args[0]; // Getting the Object id
            String strPart = args[1];
            String strNewImplementPart = args[2];
            String strCADRelName = args[3];

            DomainObject domNewImplementPart = DomainObject.newInstance(context, strNewImplementPart);
            DomainObject domPart = DomainObject.newInstance(context, strPart);
            DomainObject domCAD = DomainObject.newInstance(context, strCAD);

            // If affected Item is CAD then connect with New Implemented Part with new Implemented CAD
            Pattern relPattern = new Pattern(RELATIONSHIP_PART_SPECIFICATION);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);

            StringList slObjectSelect = new StringList();
            slObjectSelect.addElement(DomainConstants.SELECT_ID);
            slObjectSelect.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");

            StringList slRelSelect = new StringList();
            slRelSelect.addElement(DomainRelationship.SELECT_NAME);

            String sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]== \"" + TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE + "\" )";

            MapList mlNewCADObject = domNewImplementPart.getRelatedObjects(context, relPattern.getPattern(), "*", slObjectSelect, null, false, true, (short) 1, sClause, null, 0);

            if (mlNewCADObject.isEmpty()) {
                MapList mlCADObject = domPart.getRelatedObjects(context, relPattern.getPattern(), "*", slObjectSelect, null, false, true, (short) 1, sClause, null, 0);

                Iterator itrCAD = mlCADObject.iterator();
                while (itrCAD.hasNext()) {
                    Map mpCAD = (Map) itrCAD.next();
                    String strMGCAD = (String) mpCAD.get(DomainConstants.SELECT_ID);
                    DomainObject domMGCAD = DomainObject.newInstance(context, strMGCAD);
                    String strRelName = (String) mpCAD.get("relationship");
                    DomainRelationship.connect(context, domNewImplementPart, strRelName, domMGCAD);

                }
            }
            DomainRelationship.connect(context, domNewImplementPart, strCADRelName, domCAD);

        } catch (MatrixException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in propagatePartSpecificationTonewRev: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    /**
     * @author PTE
     * @param context
     * @param args
     * @throws Exception
     *             Custom method to Connect Latest version object to CAD object
     */
    public void connectLatestVersionObject(Context context, DomainObject domLatestVersionItemID, DomainObject domImplementItem, String relName) throws Exception {
        String strIterationOfCloneorRevise = DomainConstants.EMPTY_STRING;

        StringList slObjectSelects = new StringList(2);
        slObjectSelects.addElement(DomainConstants.SELECT_POLICY);
        slObjectSelects.addElement("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

        Map<String, String> mObjectInfoMap = domImplementItem.getInfo(context, slObjectSelects);
        String strRevisedObjectPolicy = mObjectInfoMap.get(DomainConstants.SELECT_POLICY);
        String strLatestVersionOfRevisedObj = DomainConstants.EMPTY_STRING;

        if (strRevisedObjectPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
            strIterationOfCloneorRevise = mObjectInfoMap.get("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

            DomainObject domIterationOfCloneorRevise = new DomainObject();
            if (UIUtil.isNotNullAndNotEmpty(strIterationOfCloneorRevise)) {
                domIterationOfCloneorRevise.setId(strIterationOfCloneorRevise);

                String strRelIdExists = MqlUtil.mqlCommand(context,
                        "print bus " + (String) domLatestVersionItemID.getObjectId() + " select from[" + relName + "| to.id == '" + (String) domIterationOfCloneorRevise.getObjectId() + "'].id dump",
                        false, false);

                if (UIUtil.isNullOrEmpty(strRelIdExists)) {
                    DomainRelationship.connect(context, domLatestVersionItemID, relName, domIterationOfCloneorRevise);
                }

            }
        }
    }

    // TIGTK-14080 START
    /**
     * @param context
     * @param args
     * @throws Exception
     *             Custom method to get Requested Change attribute value ,on the CO Implemented Item table column
     */
    public Vector<String> getRequestedChange(Context context, String[] args) throws Exception {
        Vector vReqChange = new Vector();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList mlObjList = (MapList) programMap.get("objectList");
            if (!mlObjList.isEmpty()) {
                for (int i = 0; i < mlObjList.size(); i++) {
                    Map tempMap = (Map) mlObjList.get(i);
                    String strReqChange = (String) tempMap.get(ChangeConstants.SELECT_ATTRIBUTE_REQUESTED_CHANGE);
                    vReqChange.add(strReqChange);
                }
            } else {
                vReqChange.add(DomainConstants.EMPTY_STRING);
            }

        } catch (Exception e) {
            logger.error("Error in getRequestedChange: ", e);
            throw e;
        }

        return vReqChange;
    }

    // TIGTK-14080 END

    public void setCADAttributes(Context context, DomainObject domCADParent, DomainObject domCADChild, DomainRelationship domRelCADSubComponent, String strRelCADSubComponent) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : setCADAttributes : START");
        try {
            StringList slObjSelect = new StringList();
            slObjSelect.add(DomainObject.SELECT_ID);
            slObjSelect.add(DomainObject.SELECT_NAME);
            slObjSelect.add(DomainObject.SELECT_TYPE);
            slObjSelect.add("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");
            Map mapInfoParentObj = domCADParent.getInfo(context, slObjSelect);
            Map mapInfoChildObj = domCADChild.getInfo(context, slObjSelect);

            String strParentID = (String) mapInfoParentObj.get(DomainObject.SELECT_ID);
            String strParentName = (String) mapInfoParentObj.get(DomainObject.SELECT_NAME);
            String strParentType = (String) mapInfoParentObj.get(DomainObject.SELECT_TYPE);
            String strClonedParentActiveVersion = (String) mapInfoParentObj.get("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

            String strChildID = (String) mapInfoChildObj.get(DomainObject.SELECT_ID);
            String strChildName = (String) mapInfoChildObj.get(DomainObject.SELECT_NAME);
            String strChildActiveVersion = (String) mapInfoChildObj.get("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

            // Get Title extension from Original Parent Object
            String strOriginalParentId = DomainConstants.EMPTY_STRING;
            BusinessObject busPrevisousRevOfParentNew = domCADParent.getPreviousRevision(context);

            if (busPrevisousRevOfParentNew.exists(context)) {

                DomainObject domPrevisousRev = DomainObject.newInstance(context, busPrevisousRevOfParentNew);
                strOriginalParentId = domPrevisousRev.getId(context);
            } else
                strOriginalParentId = strParentID;

            DomainObject domOriginalParent = DomainObject.newInstance(context, strOriginalParentId);
            String strTitleOriginalParent = domOriginalParent.getAttributeValue(context, "Title");

            int index = strTitleOriginalParent.lastIndexOf(".");
            String strExtensionOringalParent = DomainObject.EMPTY_STRING;
            if (index != -1)
                strExtensionOringalParent = strTitleOriginalParent.substring(index);

            // Set attribute Title and Rename Form on Clone CAD Parent Object
            AttributeList attributelistForParentCloneCAD = new AttributeList();
            if (UIUtil.isNotNullAndNotEmpty(strExtensionOringalParent))
                attributelistForParentCloneCAD.addElement(new Attribute(new AttributeType("Title"), strParentName + strExtensionOringalParent));
            else
                attributelistForParentCloneCAD.addElement(new Attribute(new AttributeType("Title"), strParentName));
            // TIGTK-17221 :Start
            if (domCADParent.isKindOf(context, TigerConstants.TYPE_MCAD_MODEL)) {
                domCADParent.setAttributes(context, attributelistForParentCloneCAD);
            }
            // TIGTK-17221 :End
            // Set Attribute on Title and Rename Form on Clone Active Version CAD Parent Object
            DomainObject domClonedParentActiveVersion = DomainObject.newInstance(context, strClonedParentActiveVersion);
            // TIGTK-17221 :Start
            if (domClonedParentActiveVersion.isKindOf(context, TigerConstants.TYPE_MCAD_MODEL)) {
                domClonedParentActiveVersion.setAttributes(context, attributelistForParentCloneCAD);
            }
            // TIGTK-17221 :End
            // DomainObject domOriginalChild = DomainObject.newInstance(context, strOriginalChildId);
            String strTitleOriginalChild = domCADChild.getAttributeValue(context, "Title");

            int index1 = strTitleOriginalChild.lastIndexOf(".");
            String strExtensionOringalChild = DomainObject.EMPTY_STRING;
            if (index1 != -1)
                strExtensionOringalChild = strTitleOriginalChild.substring(index1);

            // Set attribute Title and Rename Form on Clone CAD Child Object
            AttributeList attributelistForChildCAD = new AttributeList();
            if (UIUtil.isNotNullAndNotEmpty(strExtensionOringalChild))
                attributelistForChildCAD.addElement(new Attribute(new AttributeType("Title"), strChildName + strExtensionOringalChild));
            else
                attributelistForChildCAD.addElement(new Attribute(new AttributeType("Title"), strChildName));
            if (domCADChild.isKindOf(context, TigerConstants.TYPE_MCAD_MODEL)) {
                domCADChild.setAttributes(context, attributelistForChildCAD);
            }

            // Set Attribute on Title and Rename Form on Clone Active Version CAD Child Object
            DomainObject domClonedChildActiveVersion = DomainObject.newInstance(context, strChildActiveVersion);

            if (domClonedChildActiveVersion.isKindOf(context, TigerConstants.TYPE_MCAD_MODEL)) {
                domClonedChildActiveVersion.setAttributes(context, attributelistForChildCAD);
            }
            String strOriginalCADRel = MqlUtil.mqlCommand(context,
                    "print bus " + strOriginalParentId + " select from[" + strRelCADSubComponent + "| to.id == '" + domCADChild.getId(context) + "'].id dump", false, false);

            String strOriginalChildId = DomainConstants.EMPTY_STRING;
            if (UIUtil.isNullOrEmpty(strOriginalCADRel)) {

                BusinessObject busPrevisousRevOfChildNew = domCADChild.getPreviousRevision(context);

                if (busPrevisousRevOfChildNew.exists(context)) {

                    DomainObject domPrevisousRev = DomainObject.newInstance(context, busPrevisousRevOfChildNew);
                    strOriginalChildId = domPrevisousRev.getId(context);
                } else
                    strOriginalChildId = strChildID;

                strOriginalCADRel = MqlUtil.mqlCommand(context, "print bus " + strOriginalParentId + " select from[" + strRelCADSubComponent + "| to.id == '" + strOriginalChildId + "'].id dump",
                        false, false);

            }

            // START :: TIGTK-17234 :: ALM-6210
            Map mapOriginalCADAttributes = new HashMap();
            if (UIUtil.isNotNullAndNotEmpty(strOriginalCADRel)) {
                StringList slRels = FrameworkUtil.split(strOriginalCADRel, TigerConstants.SEPERATOR_COMMA);
                for (Object rel : slRels) {
                    DomainRelationship domOriginalCADRel = DomainRelationship.newInstance(context, (String) rel);
                    mapOriginalCADAttributes = domOriginalCADRel.getAttributeMap(context, true);
                    // Get Clone Object Active Version Rel Id
                    String strClonedVersionCADRel = MqlUtil.mqlCommand(context,
                            "print bus " + strClonedParentActiveVersion + " select from[" + strRelCADSubComponent + "| to.id == '" + strChildActiveVersion + "'].id dump", false, false);
                    if (UIUtil.isNotNullAndNotEmpty(strClonedVersionCADRel)) {
                        StringList slClonedVersions = FrameworkUtil.split(strClonedVersionCADRel, TigerConstants.SEPERATOR_COMMA);
                        for (Object clonedVersionCADRel : slClonedVersions) {
                            DomainRelationship domVersionCADRel = DomainRelationship.newInstance(context, (String) clonedVersionCADRel);
                            domVersionCADRel.setAttributeValues(context, mapOriginalCADAttributes);
                        }
                    }
                }
            }
            // END :: TIGTK-17234 :: ALM-6210

            logger.debug("PSS_enoECMChangeOrder : setCADAttributes : END");

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Error in PSS_enoECMChangeOrder : setCADAttributes : ERROR", ex);
            throw ex;
        }
    }

    public void setAttributesOnClonedCAD(Context context, DomainObject domClonedParent, DomainObject domClonedChild, DomainRelationship domRelClonedObj, String strRelName) throws Exception {
        logger.debug("PSS_enoECMChangeOrder : setAttributesOnClonedCAD : START");
        try {

            StringList slObjSelect = new StringList();
            slObjSelect.add(DomainObject.SELECT_ID);
            slObjSelect.add(DomainObject.SELECT_NAME);
            slObjSelect.add(DomainObject.SELECT_TYPE);
            slObjSelect.add("to[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].from.id");
            slObjSelect.add("to[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].from.name");
            slObjSelect.add("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

            Map mapInfoParentClonedObj = domClonedParent.getInfo(context, slObjSelect);
            Map mapInfoChildClonedObj = domClonedChild.getInfo(context, slObjSelect);

            String strClonedParentID = (String) mapInfoParentClonedObj.get(DomainObject.SELECT_ID);
            String strClonedParentName = (String) mapInfoParentClonedObj.get(DomainObject.SELECT_NAME);
            // String strClonedParentType = (String) mapInfoParentClonedObj.get(DomainObject.SELECT_TYPE);
            String strOriginalParentId = (String) mapInfoParentClonedObj.get("to[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].from.id");
            String strOrignalParentName = (String) mapInfoParentClonedObj.get("to[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].from.name");
            String strOrignalParentCurrent = (String) mapInfoParentClonedObj.get("to[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].from.current");
            String strClonedParentActiveVersion = (String) mapInfoParentClonedObj.get("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

            String strClonedChildID = (String) mapInfoChildClonedObj.get(DomainObject.SELECT_ID);
            String strClonedChildName = (String) mapInfoChildClonedObj.get(DomainObject.SELECT_NAME);
            String strOriginalChildId = (String) mapInfoChildClonedObj.get("to[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].from.id");
            String strOrignalChildName = (String) mapInfoChildClonedObj.get("to[" + TigerConstants.RELATIONSHIP_PSS_DERIVEDCAD + "].from.name");
            String strClonedChildActiveVersion = (String) mapInfoChildClonedObj.get("from[" + TigerConstants.RELATIONSHIP_LATESTVERSION + "].to.id");

            // Get Title extension from Original Parent Object
            // TIGTK-17221
            if (UIUtil.isNotNullAndNotEmpty(strOrignalParentCurrent) && !TigerConstants.STATE_INWORK_CAD_OBJECT.equals(strOrignalParentCurrent)) {
                if (UIUtil.isNullOrEmpty((strOriginalParentId))) {
                    BusinessObject busPrevisousRevOfParentNew = domClonedParent.getPreviousRevision(context);

                    if (busPrevisousRevOfParentNew.exists(context)) {

                        DomainObject domPrevisousRev = DomainObject.newInstance(context, busPrevisousRevOfParentNew);
                        strOriginalParentId = domPrevisousRev.getId(context);
                    } else {
                        strOriginalParentId = strClonedParentID;
                    }
                }

                DomainObject domOriginalParent = DomainObject.newInstance(context, strOriginalParentId);
                String strTitleOriginalParent = domOriginalParent.getAttributeValue(context, "Title");

                int index = strTitleOriginalParent.lastIndexOf(".");
                String strExtensionOringalParent = DomainObject.EMPTY_STRING;
                if (index != -1)
                    strExtensionOringalParent = strTitleOriginalParent.substring(index);

                // Set attribute Title and Rename Form on Clone CAD Parent Object
                AttributeList attributelistForParentCloneCAD = new AttributeList();
                attributelistForParentCloneCAD.addElement(new Attribute(new AttributeType("Renamed From"), strOrignalParentName));
                if (UIUtil.isNotNullAndNotEmpty(strExtensionOringalParent))
                    attributelistForParentCloneCAD.addElement(new Attribute(new AttributeType("Title"), strClonedParentName + strExtensionOringalParent));
                else
                    attributelistForParentCloneCAD.addElement(new Attribute(new AttributeType("Title"), strClonedParentName));

                // TIGTK-17221 :Start
                if (domClonedParent.isKindOf(context, TigerConstants.TYPE_MCAD_MODEL)) {
                    domClonedParent.setAttributes(context, attributelistForParentCloneCAD);
                }
                //// TIGTK-17221 :End
                // Set Attribute on Title and Rename Form on Clone Active Version CAD Parent Object
                // DomainObject domClonedParentActiveVersion = DomainObject.newInstance(context, strClonedParentActiveVersion);
                BusinessObject boClonedParentActiveVersion = new BusinessObject(strClonedParentActiveVersion);
                boClonedParentActiveVersion.setAttributes(context, attributelistForParentCloneCAD);
            } else {
                strOriginalParentId = strClonedParentID;
            }

            // Get Title extension from Original Child Object

            if (UIUtil.isNullOrEmpty((strOriginalChildId))) {

                BusinessObject busPrevisousRevOfChildNew = domClonedChild.getPreviousRevision(context);

                if (busPrevisousRevOfChildNew.exists(context)) {

                    DomainObject domPrevisousRev = DomainObject.newInstance(context, busPrevisousRevOfChildNew);
                    strOriginalChildId = domPrevisousRev.getId(context);
                } else {
                    strOriginalChildId = strClonedChildID;
                }
            }
            // if(UIUtil.isNotNullAndNotEmpty((strOriginalChildId))){

            DomainObject domOriginalChild = DomainObject.newInstance(context, strOriginalChildId);
            String strTitleOriginalChild = domOriginalChild.getAttributeValue(context, "Title");

            int index1 = strTitleOriginalChild.lastIndexOf(".");
            String strExtensionOringalChild = DomainObject.EMPTY_STRING;
            if (index1 != -1)
                strExtensionOringalChild = strTitleOriginalChild.substring(index1);

            // Set attribute Title and Rename Form on Clone CAD Child Object
            AttributeList attributelistForChildCloneCAD = new AttributeList();
            attributelistForChildCloneCAD.addElement(new Attribute(new AttributeType("Renamed From"), strOrignalChildName));
            if (UIUtil.isNotNullAndNotEmpty(strExtensionOringalChild))
                attributelistForChildCloneCAD.addElement(new Attribute(new AttributeType("Title"), strClonedChildName + strExtensionOringalChild));
            else
                attributelistForChildCloneCAD.addElement(new Attribute(new AttributeType("Title"), strClonedChildName));
            // TIGTK-17221 :Start
            if (domClonedChild.isKindOf(context, TigerConstants.TYPE_MCAD_MODEL)) {
                domClonedChild.setAttributes(context, attributelistForChildCloneCAD);
            }
            // TIGTK-17221 :End
            // Set Attribute on Title and Rename Form on Clone Active Version CAD Child Object
            // DomainObject domClonedChildActiveVersion = DomainObject.newInstance(context, strClonedChildActiveVersion);
            // TIGTK-17221 :Start
            if (UIUtil.isNotNullAndNotEmpty(strClonedChildActiveVersion)) {
                BusinessObject boClonedChildActiveVersion = new BusinessObject(strClonedChildActiveVersion);
                boClonedChildActiveVersion.setAttributes(context, attributelistForChildCloneCAD);
            }
            // TIGTK-17221 :End

            String strOriginalCADRel = MqlUtil.mqlCommand(context, "print bus " + strOriginalParentId + " select from[" + strRelName + "| to.id == '" + strOriginalChildId + "'].id dump", false,
                    false);
            Map mapOriginalCADAttributes = null;
            DomainRelationship domOriginalCADRel = null;
            // String strOriginalChildId = DomainConstants.EMPTY_STRING;
            boolean bFlag = false;
            if (UIUtil.isNullOrEmpty(strOriginalCADRel)) {
                domOriginalCADRel = domRelClonedObj;
                bFlag = true;
            } else {
                domOriginalCADRel = DomainRelationship.newInstance(context, strOriginalCADRel);
            }
            // TIGTK-17221
            if (!DomainRelationship.RELATIONSHIP_EBOM.equalsIgnoreCase(strRelName) && (UIUtil.isNotNullAndNotEmpty(strOriginalCADRel) || bFlag)) {
                mapOriginalCADAttributes = domOriginalCADRel.getAttributeMap(context, true);
                domRelClonedObj.setAttributeValues(context, mapOriginalCADAttributes);
                domRelClonedObj.setAttributeValue(context, "Renamed From", "[" + strOrignalChildName + "]");

                // Get Clone Object Active Version Rel Id

                String strClonedVersionCADRel = MqlUtil.mqlCommand(context,
                        "print bus " + strClonedParentActiveVersion + " select from[" + strRelName + "| to.id == '" + strClonedChildActiveVersion + "'].id dump", false, false);
                if (UIUtil.isNotNullAndNotEmpty(strClonedVersionCADRel)) {
                    DomainRelationship domClonedVersionCADRel = DomainRelationship.newInstance(context, strClonedVersionCADRel);
                    domClonedVersionCADRel.setAttributeValues(context, mapOriginalCADAttributes);
                    domClonedVersionCADRel.setAttributeValue(context, "Renamed From", "[" + strOrignalChildName + "]");

                }

            }

            logger.debug("PSS_enoECMChangeOrder : setAttributesOnClonedCAD : END");

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Error in PSS_enoECMChangeOrder : setAttributesOnClonedCAD : ERROR", ex);
            throw ex;
        }
    }

    // TIGTK-16083 - mkakade : START
    /**
     * @param context
     * @param args
     * @throws Exception
     *             Custom method to validate value of attribute PSS_COVirtualImplementationDate
     */
    public int validateVirtualImplementationPlannedDate(Context context, String[] args) throws Exception {
        int iReturn = 0;
        String ATTRIBUTE_PSS_COVirtualImplementationDate = PropertyUtil.getSchemaProperty(context, "attribute_PSS_COVirtualImplementationDate");
        try {
            String strCOId = args[0];
            if (null != strCOId && !"".equals(strCOId)) {
                StringList slSelects = new StringList(1);
                StringBuffer sbSelect = new StringBuffer(20);
                sbSelect.append("attribute[");
                sbSelect.append(ATTRIBUTE_PSS_COVirtualImplementationDate);
                sbSelect.append("]");

                BusinessObject boCO = new BusinessObject(strCOId);
                slSelects.addElement(sbSelect.toString());
                BusinessObjectWithSelect boCOSelect = boCO.select(context, slSelects); // slSelect is a StringList containing all selected fields names

                String strVirtualImpDate = boCOSelect.getSelectData("attribute[" + ATTRIBUTE_PSS_COVirtualImplementationDate + "]");
                // If date is blank send error message and block trigger
                if (!UIUtil.isNotNullAndNotEmpty(strVirtualImpDate)) {
                    String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                            "PSS_EnterpriseChangeMgt.Alert.BlankVirtualImplementationDate");
                    System.out.println("strMessage : " + strMessage);
                    emxContextUtil_mxJPO.mqlNotice(context, strMessage);
                    iReturn = 1;
                }
            } else {
                iReturn = 1;
            }

        } catch (Exception e) {
            logger.error("Error in validateVirtualImplementationPlannedDate: ", e);
            throw e;
        }
        return iReturn;
    }

    // TIGTK-16083 - mkakade : END

    /**
     * TIGTK-13635 : ALM-5736
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int connectGPofCOAsGoverningProjectForPart(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: connectGPofCOAsGoverningProjectForPart ::::::::");
        int iReturn = 0;
        try {
            String strChangeActionOID = args[0];
            String strPartOID = args[1];
            if (UIUtil.isNotNullAndNotEmpty(strPartOID) && UIUtil.isNotNullAndNotEmpty(strChangeActionOID)) {
                DomainObject doPart = DomainObject.newInstance(context, strPartOID);
                StringList slObjectSelects = new StringList(6);
                slObjectSelects.add(DomainConstants.SELECT_POLICY);
                slObjectSelects.add(DomainConstants.SELECT_CURRENT);
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].id");
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].id");
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].from.id");
                BusinessObject boPart = new BusinessObject(strPartOID);
                BusinessObjectWithSelect bows = boPart.select(context, slObjectSelects);
                String strCurrent = bows.getSelectData(DomainConstants.SELECT_CURRENT);
                String strPolicy = bows.getSelectData(DomainConstants.SELECT_POLICY);
                String strGPRelID = bows.getSelectData("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].id");
                String strPartGPOID = bows.getSelectData("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                StringList slIPRelID = bows.getSelectDataList("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].id");
                StringList slPartIPOID = bows.getSelectDataList("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].from.id");

                if (TigerConstants.POLICY_PSS_ECPART.equals(strPolicy) && TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(strCurrent)) {
                    DomainObject doChangeAction = DomainObject.newInstance(context, strChangeActionOID);
                    slObjectSelects = new StringList(1);
                    slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                    MapList mlChangeDetails = doChangeAction.getRelatedObjects(context, ChangeConstants.RELATIONSHIP_CHANGE_ACTION, ChangeConstants.TYPE_CHANGE_ORDER, slObjectSelects,
                            new StringList(), true, false, (short) 1, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, 0);
                    if (!mlChangeDetails.isEmpty()) {
                        String strCOGPOID = (String) ((Map) mlChangeDetails.get(0)).get("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
                        if (UIUtil.isNotNullAndNotEmpty(strCOGPOID)) {
                            boolean bConnect = false;
                            if (UIUtil.isNotNullAndNotEmpty(strGPRelID) && !strCOGPOID.equals(strPartGPOID)) {
                                try {
                                    ContextUtil.startTransaction(context, true);
                                    DomainRelationship.disconnect(context, strGPRelID);
                                    ContextUtil.commitTransaction(context);
                                    bConnect = true;
                                } catch (Exception e) {
                                    ContextUtil.abortTransaction(context);
                                    emxContextUtil_mxJPO.mqlError(context, e.getLocalizedMessage());
                                    logger.error(e.getLocalizedMessage(), e);
                                    return 1;
                                }
                            } else if (UIUtil.isNullOrEmpty(strGPRelID)) {
                                bConnect = true;
                            }
                            DomainObject doProgramProject = DomainObject.newInstance(context, strCOGPOID);
                            if (bConnect) {
                                if (slPartIPOID != null && !slPartIPOID.isEmpty() && slPartIPOID.contains(strCOGPOID)) {
                                    String strIPRelID = (String) slIPRelID.get(slPartIPOID.indexOf(strCOGPOID));
                                    DomainRelationship.disconnect(context, strIPRelID);
                                    slIPRelID.remove(slPartIPOID.indexOf(strCOGPOID));
                                    slPartIPOID.remove(strCOGPOID);
                                }
                                DomainRelationship.connect(context, doProgramProject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT, doPart);
                            }
                            pss.uls.ULSUIUtil_mxJPO ulsUtil = new pss.uls.ULSUIUtil_mxJPO();
                            ulsUtil.checkAndDisconnectInvalidImpacttedProjectLinks(context, doPart, slPartIPOID, slIPRelID);
                            ulsUtil.createImpactedProjectLinkForEBOM(context, doPart, doProgramProject);
                        }
                    }
                }
            }
        } catch (RuntimeException re) {
            iReturn = 1;
            logger.error(re.getLocalizedMessage(), re);
        } catch (Exception e) {
            iReturn = 1;
            logger.error(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: connectGPofCOAsGoverningProjectForPart ::::::::");
        return iReturn;
    }

    /**
     * TIGTK-13635 : ALM-5736
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int disconnectCurrentGPAndconnectLatestMeaturedGP(Context context, String args[]) throws Exception {
        logger.debug(":::::::: ENTER :: disconnectCurrentGPAndconnectLatestMeaturedGP ::::::::");
        int iReturn = 0;
        try {
            String strPartOID = args[1];
            if (UIUtil.isNotNullAndNotEmpty(strPartOID)) {
                DomainObject doPart = DomainObject.newInstance(context, strPartOID);
                StringList slObjectSelects = new StringList(2);
                slObjectSelects.add(DomainConstants.SELECT_POLICY);
                slObjectSelects.add(DomainConstants.SELECT_CURRENT);
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].id");
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].id");
                slObjectSelects.add("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].from.id");
                BusinessObject boPart = new BusinessObject(strPartOID);
                BusinessObjectWithSelect bows = boPart.select(context, slObjectSelects);

                String strCurrent = bows.getSelectData(DomainConstants.SELECT_CURRENT);
                String strPolicy = bows.getSelectData(DomainConstants.SELECT_POLICY);
                String strGPRelID = bows.getSelectData("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].id");
                StringList slIPRelID = bows.getSelectDataList("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].id");
                StringList slPartIPOID = bows.getSelectDataList("to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDIMPACTEDPROJECT + "].from.id");
                if (TigerConstants.POLICY_PSS_ECPART.equals(strPolicy) && TigerConstants.STATE_PSS_ECPART_PRELIMINARY.equals(strCurrent)) {
                    if (UIUtil.isNotNullAndNotEmpty(strGPRelID)) {
                        try {
                            ContextUtil.startTransaction(context, true);
                            DomainRelationship.disconnect(context, strGPRelID);
                            ContextUtil.commitTransaction(context);
                        } catch (Exception e) {
                            ContextUtil.abortTransaction(context);
                            emxContextUtil_mxJPO.mqlError(context, e.getLocalizedMessage());
                            logger.error(e.getLocalizedMessage(), e);
                            return 1;
                        }
                    }
                    pss.uls.ULSUIUtil_mxJPO ulsUtil = new pss.uls.ULSUIUtil_mxJPO();
                    String strPPOID = ulsUtil.getLatestMaturedProgramProjectFromObjectCS(context, doPart);
                    if (UIUtil.isNotNullAndNotEmpty(strPPOID)) {
                        if (slPartIPOID != null && !slPartIPOID.isEmpty() && slPartIPOID.contains(strPPOID)) {
                            try {
                                String strIPRelID = (String) slIPRelID.get(slPartIPOID.indexOf(strPPOID));
                                ContextUtil.startTransaction(context, true);
                                DomainRelationship.disconnect(context, strIPRelID);
                                ContextUtil.commitTransaction(context);
                                slIPRelID.remove(slPartIPOID.indexOf(strPPOID));
                                slPartIPOID.remove(strPPOID);
                            } catch (Exception e) {
                                ContextUtil.abortTransaction(context);
                                emxContextUtil_mxJPO.mqlError(context, e.getLocalizedMessage());
                                logger.error(e.getLocalizedMessage(), e);
                                return 1;
                            }
                        }
                        ulsUtil.checkAndDisconnectInvalidImpacttedProjectLinks(context, doPart, slPartIPOID, slIPRelID);
                        DomainRelationship.connect(context, DomainObject.newInstance(context, strPPOID), TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT, doPart);
                    }
                }
            }
        } catch (RuntimeException re) {
            iReturn = 1;
            logger.error(re.getLocalizedMessage(), re);
        } catch (Exception e) {
            iReturn = 1;
            logger.error(e.getLocalizedMessage(), e);
        }
        logger.debug(":::::::: EXIT :: disconnectCurrentGPAndconnectLatestMeaturedGP ::::::::");
        return iReturn;
    }

    // TIGTK-14264 : Prakash B : START

    /**
     * This method is invoked via a Create Action trigger when the Change Action connected to Affected Item. If CA description set then it will be defaulted to the selected Affected Item via
     * relationship "Change Affected Item" OR "Implemented Item".
     * @param context
     *            Context : User's Context.
     * @param args
     *            String array
     * @return
     * @throws Exception
     *             if fails.
     */
    public void setAttributeReasonforChange(Context context, String args[]) throws Exception {
        try {
            String strFromCAObjectId = args[0];
            // String strToAIObjectId = args[1];
            String strRelId = DomainConstants.EMPTY_STRING;
            // String strRelName = DomainConstants.EMPTY_STRING;
            String strDesc = DomainConstants.EMPTY_STRING;
            if (args.length > 2) {
                strRelId = args[2];
                // strRelName = args[3];
                DomainObject objCA = new DomainObject(strFromCAObjectId);
                strDesc = objCA.getAttributeValue(context, DomainConstants.SELECT_DESCRIPTION);
                if (UIUtil.isNullOrEmpty(strDesc)) {
                    strDesc = objCA.getInfo(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from." + DomainConstants.SELECT_DESCRIPTION);
                }
                if (UIUtil.isNotNullAndNotEmpty(strDesc) && UIUtil.isNotNullAndNotEmpty(strRelId)) {
                    DomainRelationship.newInstance(context, strRelId).setAttributeValue(context, DomainConstants.ATTRIBUTE_REASON_FOR_CHANGE, strDesc);
                }
            }

        } catch (Exception e) {
            logger.error("Error in setAttributeReasonforChange: ", e);
        }
    }

    // TIGTK-14264 : Prakash B : End

    // TIGTK-14860 : stembukar : start
    /**
     * TIGTK-14860 : ALM-5890
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkParentAffectedItemPR(Context context, String[] args) throws Exception {
        String sObjectId = args[0];
        String strMessage = "Please make sure, Purpose of Release of context CO for Engineering Affected Items connected with Requested Change as For Revise or For Replacement is at least equal or greater than Purpose of Release of CO with which last revision or original EC Parts are Released";
        try {
            DomainObject dChangeOrder = DomainObject.newInstance(context, sObjectId);

            String[] arrPROfPart = new String[2];
            arrPROfPart[1] = dChangeOrder.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);

            StringList slChangeActionIdList = dChangeOrder.getInfoList(context, "from[" + TigerConstants.TYPE_CHANGEACTION + "].to.id");

            for (int i = 0; i < slChangeActionIdList.size(); i++) {
                String sChangeActionId = (String) slChangeActionIdList.get(i);
                DomainObject dChangeAction = DomainObject.newInstance(context, sChangeActionId);
                StringList sChildPartList = dChangeAction.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id");
                StringList sReqChangeList = dChangeAction.getInfoList(context,
                        "from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].attribute[" + ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE + "]");

                for (int j = 0; j < sChildPartList.size(); j++) {
                    String sChildPartId = (String) sChildPartList.get(j);
                    DomainObject dChildPart = DomainObject.newInstance(context, sChildPartId);
                    String sRequestedChange = (String) sReqChangeList.get(j);
                    if (sRequestedChange.equalsIgnoreCase("For Revise") || sRequestedChange.equalsIgnoreCase("For Replace")) {
                        // StringList slParentPartList = dChildPart.getInfoList( context, "to[EBOM].from.id" );
                        // for( int x = 0; x < slParentPartList.size(); x++ ) {
                        String sPreviousRevObjId = dChildPart.getPreviousRevision(context).getObjectId();
                        if (!UIUtil.isNotNullAndNotEmpty(sPreviousRevObjId)) {
                            sPreviousRevObjId = sChildPartId;
                        }
                        if (UIUtil.isNotNullAndNotEmpty(sPreviousRevObjId)) {
                            DomainObject dParentPart = DomainObject.newInstance(context, sPreviousRevObjId);
                            StringList slCAObjectList = dParentPart.getInfoList(context, "to[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.id");
                            if (slCAObjectList.size() > 1) {
                                for (int k = 0; k < slCAObjectList.size(); k++) {
                                    String sCAObjId = (String) slCAObjectList.get(k);
                                    DomainObject dCAObj = DomainObject.newInstance(context, sCAObjId);
                                    String sCACurrentState = dCAObj.getCurrentState(context).getName();
                                    if (sCACurrentState.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_COMPLETE)) {
                                        arrPROfPart[0] = dCAObj.getInfo(context,
                                                "to[" + TigerConstants.TYPE_CHANGEACTION + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
                                        if (!comparePurposeOfRelease(context, arrPROfPart)) {
                                            emxContextUtil_mxJPO.mqlNotice(context, strMessage);
                                            return 1;
                                        }
                                    }
                                }
                            } else {
                                if (slCAObjectList.size() == 1) {
                                    String sCAObjId = (String) slCAObjectList.get(0);
                                    DomainObject dCAObj = DomainObject.newInstance(context, sCAObjId);
                                    arrPROfPart[0] = dCAObj.getInfo(context, "to[" + TigerConstants.TYPE_CHANGEACTION + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
                                    if (!comparePurposeOfRelease(context, arrPROfPart)) {
                                        emxContextUtil_mxJPO.mqlNotice(context, strMessage);
                                        return 1;
                                    }
                                }
                            }
                        }
                        // }
                    }
                }
            }
        } catch (Exception ex) {
            throw ex;
        }

        return 0;
    }

    /**
     * TIGTK-14860 : ALM-5890
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean comparePurposeOfRelease(Context context, String[] args) {
        boolean bFlag = false;

        String strPRForParentPart = args[0];
        String strPRForChildPart = args[1];

        try {
            if (UIUtil.isNotNullAndNotEmpty(strPRForParentPart) && UIUtil.isNotNullAndNotEmpty(strPRForChildPart)) {
                if ("Acquisition".equalsIgnoreCase(strPRForParentPart)) {
                    if ("Acquisition".equalsIgnoreCase(strPRForChildPart) || "Prototype Tool Launch/Modification".equalsIgnoreCase(strPRForChildPart)
                            || "Serial Tool Launch/Modification".equalsIgnoreCase(strPRForChildPart)) {
                        bFlag = true;
                    }
                } else if ("Prototype Tool Launch/Modification".equalsIgnoreCase(strPRForParentPart)) {
                    if ("Prototype Tool Launch/Modification".equalsIgnoreCase(strPRForChildPart) || "Serial Tool Launch/Modification".equalsIgnoreCase(strPRForChildPart)) {
                        bFlag = true;
                    }
                } else if ("Serial Tool Launch/Modification".equalsIgnoreCase(strPRForParentPart)) {
                    if ("Serial Tool Launch/Modification".equalsIgnoreCase(strPRForChildPart)) {
                        bFlag = true;
                    }
                } else {
                    bFlag = true;
                }
            }
        } catch (Exception ex) {
            throw ex;
        }
        return bFlag;
    }

    // TIGTK-14860 : stembukar : end

    // Start : TIGTK-14264 : Sub TIGTK-18220
    /**
     * Description: Edit Access function for attribute ReasonForChange
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */

    public StringList hasEditAccessOnReasonForChangeAffected(Context context, String args[]) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String sParentID = (String) requestMap.get("parentOID");

            HashMap columnMap = (HashMap) programMap.get("columnMap");
            String strColumnName = (String) columnMap.get("name");

            MapList objectList = (MapList) programMap.get("objectList");
            boolean bOwnerAdminPDL = false;
            boolean isCA = false;
            boolean isPrepare = false;
            boolean isCAState = true;
            String strCAAssignee = DomainConstants.EMPTY_STRING;
            String strCAState = DomainConstants.EMPTY_STRING;
            DomainObject domChangeObject = DomainObject.newInstance(context, sParentID);
            String strParentState = domChangeObject.getInfo(context, DomainConstants.SELECT_CURRENT);
            if (domChangeObject.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEORDER)) {
                bOwnerAdminPDL = checkAccessForOwnerAdminPDL(context, args, sParentID);
                if(TigerConstants.STATE_PSS_CHANGEORDER_PREPARE.equalsIgnoreCase(strParentState))
                    isPrepare = true;
            } else if (domChangeObject.isKindOf(context, TigerConstants.TYPE_CHANGEACTION)) {
                String strCOid = domChangeObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.id");
                bOwnerAdminPDL = checkAccessForOwnerAdminPDL(context, args, strCOid);
                strCAAssignee = (String) domChangeObject.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.name");
                isCA = true;
                if(TigerConstants.STATE_CHANGEACTION_PENDING.equalsIgnoreCase(strParentState))
                    isPrepare = true;
            }
            
            String strContextUser = context.getUser();
            StringList slLegacyAllowed = new StringList();
            slLegacyAllowed.add(ChangeConstants.FOR_RELEASE);
            slLegacyAllowed.add(ChangeConstants.FOR_OBSOLESCENCE);
            String StrRequested_Change = DomainConstants.EMPTY_STRING;
            String relatedCAId = DomainConstants.EMPTY_STRING;
            StringList slReturn = new StringList(objectList.size());
            Iterator objectItr = objectList.iterator();
            while (objectItr.hasNext()) {
                Map curObjectMap = (Map) objectItr.next();
                isCAState = true;
                String strObjectID = (String) curObjectMap.get(DomainConstants.SELECT_ID);
                String strItemType = (String) DomainObject.newInstance(context, strObjectID).getInfo(context, DomainConstants.SELECT_TYPE);
                if (!isCA) {
                    relatedCAId = (String) curObjectMap.get("relatedCAId");
                    strCAAssignee = (String) domChangeObject.newInstance(context, relatedCAId).getInfo(context, "from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.name");
                    strCAState =  (String) curObjectMap.get("relatedCAState");
                    if(!(strCAState.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_PENDING) || strCAState.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_INWORK)))
                        isCAState = false;
                }
                
                String strRelID = (String) curObjectMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                if (UIUtil.isNotNullAndNotEmpty(strRelID))
                    StrRequested_Change = DomainRelationship.newInstance(context, strRelID).getAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE);

                if(UIUtil.isNotNullAndNotEmpty(strColumnName) && "ReasonForChange".equalsIgnoreCase(strColumnName)) {
                   
                    if (UIUtil.isNotNullAndNotEmpty(strItemType) && strItemType.contains(ChangeConstants.TYPE_CHANGE_ACTION)) {
                        slReturn.addElement("false");
                    } else {
                        if (UIUtil.isNotNullAndNotEmpty(StrRequested_Change) && slLegacyAllowed.contains(StrRequested_Change)) {
                            if (bOwnerAdminPDL && isCAState)
                                slReturn.addElement("true");
                            else if (UIUtil.isNotNullAndNotEmpty(strCAAssignee) && strContextUser.equalsIgnoreCase(strCAAssignee) && isCAState) {
                                slReturn.addElement("true");
                            } else
                                slReturn.addElement("false");
                        } else
                            slReturn.addElement("false");
                    }
                }
                else if(UIUtil.isNotNullAndNotEmpty(strColumnName) && ("CARequestedChange".equalsIgnoreCase(strColumnName) || "CRCORequestedChange".equalsIgnoreCase(strColumnName))) {
                    
                    if(isPrepare) {
                        if (bOwnerAdminPDL)
                            slReturn.addElement("true");
                        else if (UIUtil.isNotNullAndNotEmpty(strCAAssignee) && strContextUser.equalsIgnoreCase(strCAAssignee)) {
                            slReturn.addElement("true");
                        } else
                            slReturn.addElement("false");
                    }
                    else
                        slReturn.addElement("false");
                }

            }
            return slReturn;
        } catch (Exception e) {

            logger.error("Error in hasEditAccessOnReasonForChangeAffected: ", e);
            throw e;
        }
    }

    /**
     * Description: Edit Access function for attribute ReasonForChange
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */

    public StringList hasEditAccessOnReasonForChangeImplemented(Context context, String args[]) throws Exception {
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            String sParentID = (String) requestMap.get("parentOID");

            MapList objectList = (MapList) programMap.get("objectList");
            boolean bOwnerAdminPDL = false;
            boolean isCA = false;
            boolean isCAState = true;
            String strCAAssignee = DomainConstants.EMPTY_STRING;
            String strCAState = DomainConstants.EMPTY_STRING;
            DomainObject domChangeObject = DomainObject.newInstance(context, sParentID);
            String relatedCAId = DomainConstants.EMPTY_STRING;
            if (domChangeObject.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEORDER)) {
                bOwnerAdminPDL = checkAccessForOwnerAdminPDL(context, args, sParentID);
            } else if (domChangeObject.isKindOf(context, TigerConstants.TYPE_CHANGEACTION)) {
                String strCOid = domChangeObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.id");
                bOwnerAdminPDL = checkAccessForOwnerAdminPDL(context, args, strCOid);
                strCAAssignee = (String) domChangeObject.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.name");
                isCA = true;
            }

            StringList slLegacyAllowed = new StringList();
            slLegacyAllowed.add(ChangeConstants.FOR_RELEASE);
            String strContextUser = context.getUser();
            String StrRequested_Change = DomainConstants.EMPTY_STRING;
            StringList slReturn = new StringList(objectList.size());
            Iterator objectItr = objectList.iterator();
            StringList slSelectStmts = new StringList();
            slSelectStmts.add(DomainConstants.SELECT_TYPE);
            slSelectStmts.add("from[" + TigerConstants.RELATIONSHIP_PSS_ITEMASSIGNEE + "].to.name");
            while (objectItr.hasNext()) {
                Map curObjectMap = (Map) objectItr.next();
                isCAState = true;
                String strObjectID = (String) curObjectMap.get(DomainConstants.SELECT_ID);
                Map<String, Object> mapChangeObjectDetails = DomainObject.newInstance(context, strObjectID).getInfo(context, slSelectStmts);
                String strItemType = (String) mapChangeObjectDetails.get(DomainConstants.SELECT_TYPE);
                String strItemAssignee = (String) mapChangeObjectDetails.get("from[" + TigerConstants.RELATIONSHIP_PSS_ITEMASSIGNEE + "].to.name");

                if (!isCA) {
                    relatedCAId = (String) curObjectMap.get("relatedCAId");
                    strCAAssignee = (String) domChangeObject.newInstance(context, relatedCAId).getInfo(context, "from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.name");
                    strCAState =  (String) curObjectMap.get("relatedCAState");
                    if(!(strCAState.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_PENDING) || strCAState.equalsIgnoreCase(TigerConstants.STATE_CHANGEACTION_INWORK)))
                        isCAState = false;
                }
                String strRelID = (String) curObjectMap.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                if (UIUtil.isNotNullAndNotEmpty(strRelID))
                    StrRequested_Change = DomainRelationship.newInstance(context, strRelID).getAttributeValue(context, ChangeConstants.ATTRIBUTE_REQUESTED_CHANGE);

                if (UIUtil.isNotNullAndNotEmpty(strItemType) && strItemType.contains(ChangeConstants.TYPE_CHANGE_ACTION)) {
                    slReturn.addElement("false");
                } else {
                    if (UIUtil.isNotNullAndNotEmpty(StrRequested_Change) && slLegacyAllowed.contains(StrRequested_Change)) {
                        if (bOwnerAdminPDL && isCAState)
                            slReturn.addElement("true");
                        else if (UIUtil.isNotNullAndNotEmpty(strCAAssignee) && strContextUser.equalsIgnoreCase(strCAAssignee) && isCAState) {
                            slReturn.addElement("true");
                        } else if (UIUtil.isNotNullAndNotEmpty(strItemAssignee) && strContextUser.equalsIgnoreCase(strItemAssignee)  && isCAState) {
                            slReturn.addElement("true");
                        } else
                            slReturn.addElement("false");
                    } else
                        slReturn.addElement("false");
                }

            }

            return slReturn;
        } catch (Exception e) {

            logger.error("Error in hasEditAccessOnReasonForChangeImplemented: ", e);
            throw e;
        }
    }

    // TIGTK-14264 : Sub TIGTK-18220 : END

    /**
     * Method to check and highlight Part in red on CO context if the Part gone under Policy change after adding into CO TIGTK-14892 :: ALM-4106 :: PSI
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector highlightPartsWithPolicyChange(Context context, String[] args) throws Exception {
        Vector vReturn = null;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get(ChangeConstants.OBJECT_LIST);
            vReturn = new Vector(objectList.size());
            String strAIOid = DomainConstants.EMPTY_STRING;
            String strAIPolicy = DomainConstants.EMPTY_STRING;
            String strAIName = DomainConstants.EMPTY_STRING;
            String strAIType = DomainConstants.EMPTY_STRING;
            String strRelatedCAOID = DomainConstants.EMPTY_STRING;
            String strCAType = DomainConstants.EMPTY_STRING;
            String strTreeLink = DomainConstants.EMPTY_STRING;
            String strAITypeIcon = DomainConstants.EMPTY_STRING;
            StringBuilder sbHref = null;
            DomainObject doCA = DomainObject.newInstance(context);
            Iterator itrAIs = objectList.iterator();
            while (itrAIs.hasNext()) {
                Map mpAI = (Map) itrAIs.next();
                strAIOid = (String) mpAI.get(DomainConstants.SELECT_ID);
                strAIName = (String) mpAI.get(DomainConstants.SELECT_NAME);
                strAIType = (String) mpAI.get(DomainConstants.SELECT_TYPE);
                strAIPolicy = (String) mpAI.get(DomainConstants.SELECT_POLICY);
                strRelatedCAOID = (String) mpAI.get(ChangeConstants.RELATED_CA_ID);
                strAITypeIcon = UINavigatorUtil.getTypeIconProperty(context, strAIType);
                sbHref = new StringBuilder(500);
                strTreeLink = "<a class=\"object\" href=\"JavaScript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=" + XSSUtil.encodeForHTMLAttribute(context, strAIOid)
                        + "', '800', '575','true','content')\"><img border='0' src='../common/images/" + XSSUtil.encodeForHTMLAttribute(context, strAITypeIcon) + "'/>"
                        + XSSUtil.encodeForHTML(context, strAIName) + "</a>";
                if (!ChangeUtil.isNullOrEmpty(strRelatedCAOID) && !ChangeUtil.isNullOrEmpty(strAIPolicy)) {
                    doCA.setId(strRelatedCAOID);
                    strCAType = doCA.getInfo(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_CATYPE + "].value");
                    if (((TigerConstants.POLICY_PSS_ECPART.equals(strAIPolicy) || TigerConstants.POLICY_PSS_DEVELOPMENTPART.equals(strAIPolicy))
                            && !TigerConstants.ATTRIBUTE_PSS_CATYPE_PART.equals(strCAType))
                            || (TigerConstants.POLICY_STANDARDPART.equals(strAIPolicy) && !TigerConstants.ATTRIBUTE_PSS_CATYPE_STD.equals(strCAType))) {
                        strTreeLink = "<a class=\"object\" style=\"color:red;\" href=\"JavaScript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId="
                                + XSSUtil.encodeForHTMLAttribute(context, strAIOid) + "', '800', '575','true','content')\"><img border='0' src='../common/images/"
                                + XSSUtil.encodeForHTMLAttribute(context, strAITypeIcon) + "'/>" + XSSUtil.encodeForHTML(context, strAIName) + "</a>";
                    }
                }
                sbHref.append(strTreeLink);
                vReturn.addElement(sbHref.toString());
            }
        } catch (Exception e) {
            logger.debug(e.getLocalizedMessage(), e);
            throw new FrameworkException(e);
        }
        return vReturn;
    }
	
	// START : TIGTK-14264 : Sub TIGTK-18220
    /**
     * This Method is used for Access Function on Edit command of Affected item and Implemented item view present in CO/CA context
     * @param context
     * @param args
     * @return Boolean -- > The output whether command to be visible or no
     * @throws Exception
     */
    public boolean hasCOCAEditAccess(Context context, String args[]) throws Exception {
        boolean showEditCommand = false;
        
        try {
            HashMap<?, ?> programMap = JPO.unpackArgs(args);
            String strObjectId = (String) programMap.get("objectId");
            DomainObject domChangeObject = DomainObject.newInstance(context, strObjectId);
            if (domChangeObject.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEORDER)) {

                showEditCommand = checkAccessForOwnerAdminPDL(context, args, strObjectId);
                if (!showEditCommand)
                    showEditCommand = checkAccessForCAAsssignee(context, strObjectId, true);

            } else if (domChangeObject.isKindOf(context, TigerConstants.TYPE_CHANGEACTION)) {

                String strCOid = domChangeObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.id");
                showEditCommand = checkAccessForOwnerAdminPDL(context, args, strCOid);
                if (!showEditCommand)
                    showEditCommand = checkAccessForCAAsssignee(context, strObjectId, false);

            }

        } catch (Exception ex) {
            logger.error("Error in hasCOCAEditAccess: ", ex);
            ex.printStackTrace();
        }
        return showEditCommand;
    }

    /**
     * This Method is used for check Access For Owner OR Admin OR PDL in Affected / Implemented item view present in CO/CA context
     * @param context
     * @param String[]
     * @param String CO object id
     * @return Boolean -- > The output whether command to be visible or no
     * @throws Exception
     */
    private boolean checkAccessForOwnerAdminPDL(Context context, String args[], String StrCOId) throws Exception {
        boolean showEditCommand = false;

        StringList slPMLList = null;
        pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);

        String strContextUser = context.getUser();
        String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, strContextUser);
        String assignedRoles = (strLoggedUserSecurityContext.split("[.]")[0]);

        String strOwner = DomainObject.newInstance(context, StrCOId).getOwner(context).getName();

        if (assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || assignedRoles.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)
                || (strOwner).equalsIgnoreCase(strContextUser)) {
            showEditCommand = true;
        } else {
            slPMLList = commonObj.getProgramProjectTeamMembersForChange(context, (String) StrCOId, new StringList(TigerConstants.ROLE_PSS_PRODUCT_DEVELOPMENT_LEAD), true);
            if (null != slPMLList && !slPMLList.isEmpty() && slPMLList.contains(strContextUser))
                showEditCommand = true;

        }

        return showEditCommand;
    }

    /**
     * This Method is used for check Access For CA Assignee in Affected / Implemented item view present in CO/CA context
     * @param context
     * @param String object id of CO/CA
     * @param boolean isCO or not 
     * @return Boolean -- > The output whether command to be visible or no
     * @throws Exception
     */
    private boolean checkAccessForCAAsssignee(Context context, String StrId, boolean isCO) throws Exception {
        boolean showEditCommand = false;
        String strContextUser = context.getUser();
        StringList slCAAssignee = new StringList();
        if (isCO) {
            slCAAssignee = DomainObject.newInstance(context, StrId).getInfoList(context,
                    "from[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].to.from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.name");
        } else {
            slCAAssignee.add((String) DomainObject.newInstance(context, StrId).getInfo(context, "from[" + TigerConstants.RELATIONSHIP_TECHNICALASSIGNEE + "].to.name"));

        }
        if (!slCAAssignee.isEmpty() && slCAAssignee.contains(strContextUser))
            showEditCommand = true;

        return showEditCommand;
    }

    /**
     * This Method is used for check Access For Implemented Item Assignee in Implemented item view present in CO/CA context
     * @param context
     * @param DomainObject
     * @return Boolean -- > The output whether command to be visible or no
     * @throws Exception
     */
    private boolean checkAccessForImplementedItemAssignee(Context context, DomainObject domChangeObject) throws Exception {
        boolean showEditCommand = false;
        String contextUser = context.getUser();
        StringList slCAAssignee = new StringList();
        if (domChangeObject.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEORDER)) {
            slCAAssignee = domChangeObject.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].to.from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.from["
                    + TigerConstants.RELATIONSHIP_PSS_ITEMASSIGNEE + "].to.name");

        } else if (domChangeObject.isKindOf(context, TigerConstants.TYPE_CHANGEACTION)) {
            slCAAssignee = domChangeObject.getInfoList(context, "from[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + "].to.from[" + TigerConstants.RELATIONSHIP_PSS_ITEMASSIGNEE + "].to.name");

        }
        if (!slCAAssignee.isEmpty() && slCAAssignee.contains(contextUser))
            showEditCommand = true;

        return showEditCommand;
    }
    // end : TIGTK-14264 : Sub TIGTK-18220

}// end of class