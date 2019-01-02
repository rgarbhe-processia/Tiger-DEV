package pss.mbom.command;

import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;

import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicClassItf;
import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicFactory;
import com.dassault_systemes.iPLMDictionaryPublicItf.IPLMDictionaryPublicItf;
import com.dassault_systemes.vplm.ResourceAuthoring.interfaces.IVPLMResourceAuthoring;
import com.dassault_systemes.vplm.modeler.PLMCoreAbstractModeler;
import com.dassault_systemes.vplm.modeler.PLMCoreModelerSession;
import com.dassault_systemes.vplm.modeler.PLMxTemplateFactory;
import com.dassault_systemes.vplm.modeler.entity.PLMxReferenceEntity;
import com.dassault_systemes.vplm.modeler.template.IPLMTemplateContext;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.plmql.cmd.PLMID;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.RelationshipType;
import pss.constants.TigerConstants;

public class Equipment_mxJPO {

    // TIGTK-5405 - 06-04-2017 - VB - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Equipment_mxJPO.class);
    // TIGTK-5405 - 06-04-2017 - VB - END

    public Equipment_mxJPO() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * this method is used to create Equipment object,by adding interface PSS_Equipment
     * @param context
     * @param args
     * @return returns ID of the generated object
     * @throws Exception
     *             Exception appears, if error occurred
     */
    @SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
    public HashMap createEquiment(Context context, String args[]) throws Exception {

        String TYPE_SYMBOLIC_VPMREFERENCE = FrameworkUtil.getAliasForAdmin(context, "type", TigerConstants.TYPE_VPMREFERENCE, true);
        String strEquipmentId = DomainObject.EMPTY_STRING;
        String strEquipmentName = DomainObject.EMPTY_STRING;
        String strDisplayName = DomainObject.EMPTY_STRING;
        String strEquipmentType = DomainObject.EMPTY_STRING;
        DomainObject domEquipmentRequest = new DomainObject();
        PLMCoreModelerSession plmSession = null;
        boolean activeTransaction = false;
        try {
            HashMap programMap = JPO.unpackArgs(args);
            HashMap returnMap = new HashMap();
            String strEquimentRequestId = (String) programMap.get("Equipment Request");
            strEquipmentType = (String) programMap.get("Equipment Type");
            if (!UIUtil.isNullOrEmpty(strEquimentRequestId)) {
                domEquipmentRequest = DomainObject.newInstance(context, strEquimentRequestId);
                strDisplayName = domEquipmentRequest.getAttributeValue(context, TigerConstants.ATTRIBUTE_DISPLAY_NAME);
            }
            strEquipmentName = DomainObject.getAutoGeneratedName(context, TYPE_SYMBOLIC_VPMREFERENCE, TigerConstants.INTERFACE_PSS_EQUIPMENT);
            ContextUtil.startTransaction(context, true);
            activeTransaction = true;
            context.setApplication("VPLM");

            plmSession = PLMCoreModelerSession.getPLMCoreModelerSessionFromContext(context);
            plmSession.openSession();
            IVPLMResourceAuthoring resourceAuthoringModeler = (IVPLMResourceAuthoring) plmSession.getModeler("com.dassault_systemes.vplm.ResourceAuthoring.implementation.VPLMResourceAuthoring");

            IPLMDictionaryPublicFactory ipldmFactory = new IPLMDictionaryPublicFactory();
            IPLMDictionaryPublicItf dico = ipldmFactory.getDictionary();

            IPLMTemplateContext brContext = PLMxTemplateFactory.newContext((PLMCoreAbstractModeler) resourceAuthoringModeler);
            Context ctx = (Context) brContext.getExecuteRuleCtx();

            IPLMDictionaryPublicClassItf iFuncCreateResourceType = dico.getClass(ctx, "VPMReference");

            Hashtable attributes = new Hashtable<>();
            attributes.put("Ext(PSS_MBOM/PSS_PublishedPart).PSS_PSTime", System.currentTimeMillis() / 1000);
            PLMxReferenceEntity rsc_Ref = resourceAuthoringModeler.createReferenceFromType(iFuncCreateResourceType, "PSS_Resources/PSS_Equipment", attributes);
            if (rsc_Ref == null)
                throw new Exception("Create Equipment Item failed : returned null.");

            flushAndCloseSession(plmSession);

            String refPLMIDStr = rsc_Ref.getPLMIdentifier();
            PLMID refPLMID = PLMID.buildFromString(refPLMIDStr);
            strEquipmentId = refPLMID.getPid();
            DomainObject domEquipment = DomainObject.newInstance(context, strEquipmentId);
            domEquipment.setPolicy(context, TigerConstants.POLICY_PSS_EQUIPMENT);
            String strChangeString = "modify bus $1 revision $2 name $3;";
            MqlUtil.mqlCommand(context, strChangeString, strEquipmentId, "01.1", strEquipmentName);
            domEquipment.setAttributeValue(context, TigerConstants.ATTRIBUTE_EQUIPMENT_TYPE, strEquipmentType);
            domEquipment.setAttributeValue(context, TigerConstants.ATTRIBUTE_DISPLAY_NAME, strDisplayName);
            domEquipment.setAttributeValue(context, TigerConstants.ATTRIBUTE_V_NAME, strEquipmentName);
            domEquipment.setAttributeValue(context, TigerConstants.ATTRIBUTE_PLM_EXTERNALID, strEquipmentName);

            if (!UIUtil.isNullOrEmpty(strEquimentRequestId)) {
                DomainRelationship.connect(context, domEquipment, new RelationshipType(TigerConstants.RELATIONSHIP_PSS_REQUESTED_EQUIPMENT), domEquipmentRequest);
            }

            if (activeTransaction) {
                ContextUtil.commitTransaction(context);
            }

            returnMap.put(DomainConstants.SELECT_ID, strEquipmentId);
            return returnMap;
        } catch (Exception e) {
            // TIGTK-5405 - 06-04-2017 - VB - START
            logger.error("Error in createEquiment: ", e);
            // TIGTK-5405 - 06-04-2017 - VB - END
            if (activeTransaction) {
                ContextUtil.abortTransaction(context);
            }
            throw e;
        }
    }

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
}
