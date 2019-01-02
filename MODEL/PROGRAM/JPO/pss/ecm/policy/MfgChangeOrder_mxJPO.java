package pss.ecm.policy;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.StringList;
import pss.constants.TigerConstants;

import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.mbom.modeler.utility.FRCMBOMModelerUtility;
import matrix.util.Pattern;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;

public class MfgChangeOrder_mxJPO {

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MfgChangeOrder_mxJPO.class);

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END
    /**
     * Created By : Swapnil Purpose : Update Parent Mfg Part MBOM connection with new revision which are connected to Previous Mfg Part Revision
     * @param context
     * @param args
     * @throws Exception
     */
    // PCM RFC-117: 27/03/2017 : START
    public void floatMfgPartToEnd(Context context, String[] args) throws Exception {
        String RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingChangeAffectedItem");
        String RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingChangeAction");
        String RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingRelatedPlant");
        String RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE = PropertyUtil.getSchemaProperty(context, "relationship_DELFmiFunctionIdentifiedInstance");
        String TYPE_CREATEASSEMBLY = PropertyUtil.getSchemaProperty(context, "type_CreateAssembly");
        String TYPE_CREATEKIT = PropertyUtil.getSchemaProperty(context, "type_CreateKit");
        String TYPE_CREATEMATERIAL = PropertyUtil.getSchemaProperty(context, "type_CreateMaterial");

        String strObjectId = args[0];

        Pattern typePattern = new Pattern(TYPE_CREATEASSEMBLY);
        typePattern.addPattern(TYPE_CREATEKIT);
        typePattern.addPattern(TYPE_CREATEMATERIAL);

        DomainObject domMfgPart = new DomainObject(strObjectId);
        try {
            String strConnectedMCO = domMfgPart.getInfo(context, "to[" + RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].from.to[" + RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].from.id");
            if (UIUtil.isNotNullAndNotEmpty(strConnectedMCO)) {
                DomainObject domMCO = new DomainObject((String) strConnectedMCO);
                String strConnectedPlant = domMCO.getInfo(context, "from[" + RELATIONSHIP_PSS_MANUFACTURINGRELATEDPLANT + "].to.id");

                StringList slMfgPartParents = domMfgPart.getInfoList(context, "to[" + RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE + "].from.id");
                StringList slAffectedItems = domMCO.getInfoList(context,
                        "from[" + RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.from[" + RELATIONSHIP_PSS_MANUFACTURINGCHANGEAFFECTEDITEM + "].to.id");
                boolean flagFound = false;
                for (int cnt = 0; cnt < slAffectedItems.size(); cnt++) {
                    for (int cntParent = 0; cntParent < slMfgPartParents.size(); cntParent++) {
                        if (slMfgPartParents.get(cntParent).equals(slAffectedItems.get(cnt))) {
                            flagFound = true;
                            break;
                        }
                    }
                }
                if (!flagFound) {
                    BusinessObject boPrevRev = domMfgPart.getPreviousRevision(context);
                    if (boPrevRev.exists(context)) {
                        DomainObject domPrevRev = new DomainObject(boPrevRev);
                        StringList selectStmts = new StringList();
                        selectStmts.addElement(DomainConstants.SELECT_ID);

                        StringList selectRelStmts = new StringList();
                        selectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

                        MapList mlreviewerList = domPrevRev.getRelatedObjects(context, RELATIONSHIP_DELFMIFUNCTIONIDENTIFIEDINSTANCE, // relationship pattern
                                typePattern.getPattern(), // object pattern
                                selectStmts, // object selects
                                selectRelStmts, // relationship selects
                                true, // from direction
                                false, // to direction
                                (short) 1, // recursion level
                                null, // object where clause
                                null, 0); // relationship where clause

                        for (int cntml = 0; cntml < mlreviewerList.size(); cntml++) {
                            String strRelId = (String) ((Map) mlreviewerList.get(cntml)).get(DomainConstants.SELECT_RELATIONSHIP_ID);
                            DomainObject domObj = new DomainObject((String) ((Map) mlreviewerList.get(cntml)).get(DomainObject.SELECT_ID));
                            String[] strArgs = { domObj.getInfo(context, DomainObject.SELECT_ID) };
                            String strPlantId = getPlantIdFromMfgPart(context, strArgs);
                            if (strPlantId.equals(strConnectedPlant)) {
                                DomainRelationship.modifyTo(context, strRelId, domMfgPart);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in floatMfgPartToEnd: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }
    // PCM RFC-117: 27/03/2017 : End

    public String getPlantIdFromMfgPart(Context context, String[] args) throws Exception {
        try {
            String strMfgAffectedItemId = args[0];
            String strId = DomainConstants.EMPTY_STRING;
            PLMCoreModelerSession plmSession = PLMCoreModelerSession.getPLMCoreModelerSession(strMfgAffectedItemId);
            plmSession.openSession();
            String strAttachedPlant = pss.mbom.MBOMUtil_mxJPO.getMasterPlant(context, strMfgAffectedItemId);
            if (UIUtil.isNotNullAndNotEmpty(strAttachedPlant)) {
                DomainObject domPlant = new DomainObject(strAttachedPlant);
                strId = domPlant.getInfo(context, DomainConstants.SELECT_ID);
            }
            return strId;
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getPlantIdFromMfgPart: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    /**
     * @description : added as check trigger on MCO which will make sure at least one affected item must be connected to MCO
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkMCAConnection(Context context, String[] args) throws Exception { // constants
        String RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION = PropertyUtil.getSchemaProperty(context, "relationship_PSS_ManufacturingChangeAction");
        String strObjectId = args[0];
        int status = 0;
        try {
            DomainObject domMCO = new DomainObject(strObjectId);
            StringList slConnectedMCA = domMCO.getInfoList(context, "from[" + RELATIONSHIP_PSS_MANUFACTURINGCHANGEACTION + "].to.id");
            if (slConnectedMCA.isEmpty() || slConnectedMCA.size() < 1) {
                status = 1;
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSSEnterpriseChangeMgt.Alert.NoConnectedAI");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
            }
        } catch (Exception w) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkMCAConnection: ", w);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw w;
        }
        return status;
    }

    /**
     * @description : added as check trigger on MCO promotion which will check CN State if connected to CN
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkForCNConnectedToMCO(Context context, String[] args) throws Exception { // constants

        String strObjectId = args[0];
        int status = 0;
        try {
            DomainObject domMCO = DomainObject.newInstance(context, strObjectId);
            StringList lstSelectList = new StringList();
            lstSelectList.add(DomainConstants.SELECT_CURRENT);
            MapList mlConnectedCNList = domMCO.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_RELATEDCN, TigerConstants.TYPE_PSS_CHANGENOTICE, lstSelectList, null, false, true, (short) 0,
                    null, null, 0);

            Iterator<?> iterator = mlConnectedCNList.iterator();

            Map<?, ?> objectMap;
            while (iterator.hasNext()) {
                objectMap = (Map<?, ?>) iterator.next();
                String strMCOCurrent = (String) objectMap.get(DomainConstants.SELECT_CURRENT);
                if (!TigerConstants.STATE_PSS_CHANGENOTICE_CANCELLED.equals(strMCOCurrent)) {
                    status = 1;
                    String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSSEnterpriseChangeMgt.Alert.CNState");
                    strMessage = strMessage.replace("$<name>", args[1]);
                    strMessage = strMessage.replace("$<state>", args[2]);
                    MqlUtil.mqlCommand(context, "notice $1", strMessage);
                    break;

                }
            }

        } catch (Exception w) {
            logger.error("Error in checkForCNConnectedToMCO: ", w);
            throw w;
        }
        return status;
    }
}