package fpdm.ecm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.common.Organization;
import com.matrixone.apps.common.RouteTemplate;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.Query;
import matrix.db.QueryIterator;
import matrix.db.RelationshipWithSelect;
import matrix.util.MatrixException;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class RouteTemplate_mxJPO {

    private static Logger logger = LoggerFactory.getLogger("RouteTemplate");

    private static String sSuiteKey = "emxFrameworkStringResource";

    public HashMap<String, StringList> getRTPurposeRanges(Context context, String args[]) {
        Locale locale = context.getLocale();

        StringList slChoices = new StringList();
        slChoices.addElement("PSS_ReviewTemplate");
        slChoices.addElement("PSS_CommercialApprovalCO");
        slChoices.addElement("PSS_PrototypeApprovalCO");
        slChoices.addElement("PSS_SerialApprovalCO");
        slChoices.addElement("PSS_DesignApprovalCO");
        slChoices.addElement("PSS_PartApprovalCO");
        slChoices.addElement("PSS_CADApprovalCO");
        slChoices.addElement("PSS_StandardApprovalCO");
        slChoices.addElement("PSS_AcquisitionCO");
        slChoices.addElement("PSS_CommercialApprovalMCO");
        slChoices.addElement("PSS_PrototypeApprovalMCO");
        slChoices.addElement("PSS_SerialApprovalMCO");
        slChoices.addElement("PSS_DesignApprovalMCO");
        slChoices.addElement("PSS_PartApprovalMCO");
        slChoices.addElement("PSS_AcquisitionMCO");
        slChoices.addElement("PSS_ReviewCN");

        StringList slDisplayChoices = new StringList();
        slDisplayChoices.addElement("CR - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.ReviewTemplate"));
        slDisplayChoices.addElement("CO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.CommercialApprovalCO"));
        slDisplayChoices.addElement("CO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.PrototypeApprovalCO"));
        slDisplayChoices.addElement("CO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.SerialApprovalCO"));
        slDisplayChoices.addElement("CO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.DesignApprovalCO"));
        slDisplayChoices.addElement("CO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.PartApprovalListCO"));
        slDisplayChoices.addElement("CO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.CADApprovalCO"));
        slDisplayChoices.addElement("CO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.StandardApprovalCO"));
        slDisplayChoices.addElement("CO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.Acquisition"));
        slDisplayChoices.addElement("MCO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.CommercialApprovalMCO"));
        slDisplayChoices.addElement("MCO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.PrototypeApprovalMCO"));
        slDisplayChoices.addElement("MCO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.SerialApprovalMCO"));
        slDisplayChoices.addElement("MCO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.DesignApprovalMCO"));
        slDisplayChoices.addElement("MCO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.PartApprovalListMCO"));
        slDisplayChoices.addElement("MCO - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.Acquisition"));
        slDisplayChoices.addElement("CN - " + EnoviaResourceBundle.getProperty(context, sSuiteKey, locale, "emxFramework.PCM.ReviewCN"));

        HashMap<String, StringList> hmRTPurposeRanges = new HashMap<String, StringList>();
        hmRTPurposeRanges.put("field_choices", slChoices);
        hmRTPurposeRanges.put("field_display_choices", slDisplayChoices);
        return hmRTPurposeRanges;
    }

    public StringList getRTPurpose(Context context, String args[]) {
        StringList slRTPurpose = new StringList();
        try {
            Map<?, ?> hmProgram = (Map<?, ?>) JPO.unpackArgs(args);
            Map<String, Object> paramMap = (HashMap<String, Object>) hmProgram.get("paramMap");
            Map<String, Object> requestMap = (HashMap<String, Object>) hmProgram.get("requestMap");

            String sRTId = (String) paramMap.get("objectId");
            DomainObject doRT = DomainObject.newInstance(context, sRTId);
            Attribute attFPDM_RTPurpose = doRT.getAttributeValues(context, TigerConstants.ATTRIBUTE_FPDM_RTPURPOSE);

            slRTPurpose = attFPDM_RTPurpose.getValueList();

            String mode = (String) requestMap.get("mode");
            if ("view".equals(mode)) {
                HashMap<String, StringList> hmRTPurposeRanges = getRTPurposeRanges(context, args);
                StringList slRTPurposeChoices = (StringList) hmRTPurposeRanges.get("field_choices");
                StringList slRTPurposeDisplayChoices = (StringList) hmRTPurposeRanges.get("field_display_choices");

                for (int i = 0; i < slRTPurpose.size(); i++) {
                    int pos = slRTPurposeChoices.indexOf((String) slRTPurpose.get(i));
                    slRTPurpose.set(i, (String) slRTPurposeDisplayChoices.get(pos));
                }
                slRTPurpose = new StringList(slRTPurpose.join(", "));
            }
        } catch (Exception e) {
            logger.error("Error in getRTPurpose method\n", e);
        }
        return slRTPurpose;
    }

    public void updateRTPurpose(Context context, String[] args) {
        try {
            Map<?, ?> hmProgram = (Map<?, ?>) JPO.unpackArgs(args);
            Map<String, Object> paramMap = (HashMap<String, Object>) hmProgram.get("paramMap");

            StringList slFPDM_RTPurpose = new StringList((String[]) paramMap.get("New Values"));
            AttributeType attTypeFPDM_RTPurpose = new AttributeType(TigerConstants.ATTRIBUTE_FPDM_RTPURPOSE);
            Attribute attFPDM_RTPurpose = new Attribute(attTypeFPDM_RTPurpose, slFPDM_RTPurpose);
            AttributeList attList = new AttributeList();
            attList.addElement(attFPDM_RTPurpose);

            String sRTId = (String) paramMap.get("objectId");
            DomainObject doRT = DomainObject.newInstance(context, sRTId);
            doRT.setAttributes(context, attList);
        } catch (Exception e) {
            logger.error("Error in updateRTPurpose method\n", e);
        }
    }

    public String checkForDefaultRTWithSelectedPurpose(Context context, String[] args) {
        StringBuilder sbErrorMsg = new StringBuilder();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String[] saFPDM_RTPurpose = (String[]) programMap.get("FPDM_RTPurpose");
            String concat_RTPurpose = StringUtils.join(saFPDM_RTPurpose, ",");

            String sRTId = (String) programMap.get("objectId");

            BusinessObjectWithSelect bows;
            StringList slBus = new StringList();
            slBus.addElement(DomainConstants.SELECT_NAME);

            Query queryRT = new Query();
            queryRT.setBusinessObjectType(DomainConstants.TYPE_ROUTE_TEMPLATE);
            String sExpression = "attribute[" + TigerConstants.ATTRIBUTE_FPDM_RTDEFAULT + "].value==TRUE && attribute[" + TigerConstants.ATTRIBUTE_FPDM_RTPURPOSE + "].value matchlist \""
                    + concat_RTPurpose + "\" \",\"";
            queryRT.setWhereExpression(sExpression);
            queryRT.setVaultPattern(TigerConstants.VAULT_ESERVICEPRODUCTION);
            try {
                ContextUtil.startTransaction(context, true);
                QueryIterator queryItr = queryRT.getIterator(context, slBus, (short) 200);

                while (queryItr.hasNext()) {
                    bows = queryItr.next();
                    bows.open(context);
                    if (!bows.getObjectId().equals(sRTId)) {
                        if (sbErrorMsg.length() == 0) {
                            sbErrorMsg.append(EnoviaResourceBundle.getProperty(context, sSuiteKey, context.getLocale(), "emxFramework.CreateRouteTemplate.ErrorMsg"));
                            sbErrorMsg.append(" ");
                        } else {
                            sbErrorMsg.append(", ");
                        }
                        sbErrorMsg.append(bows.getSelectData(DomainConstants.SELECT_NAME));
                    }
                    bows.close(context);
                }
                queryItr.close();
                queryRT.close(context);
            } finally {
                ContextUtil.abortTransaction(context);
            }
        } catch (Exception e) {
            logger.error("Error in checkForDefaultRTWithSelectedPurpose method\n", e);
        }
        return sbErrorMsg.toString();
    }

    // Duplicated from emxRouteTemplateBase:routeTemplateEditProcess
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public HashMap routeTemplateEditProcess(Context context, String[] args) throws Exception {
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        Map requestMap = (Map) programMap.get("requestMap");

        // Start of code added for Program-Project automation
        HashMap hmResult = new HashMap();
        String sFPDM_RTDefault = (String) requestMap.get("FPDM_RouteTemplateDefault");
        if ("TRUE".equals(sFPDM_RTDefault)) {
            Map requestValuesMap = (Map) programMap.get("requestValuesMap");
            String[] saFPDM_RTPurpose = (String[]) requestValuesMap.get("FPDM_RouteTemplatePurpose");
            String sRTId = (String) requestMap.get("objectId");

            HashMap hmMethodArgs = new HashMap();
            hmMethodArgs.put("FPDM_RTPurpose", saFPDM_RTPurpose);
            hmMethodArgs.put("objectId", sRTId);
            String[] saMethodArgs = JPO.packArgs(hmMethodArgs);

            String sErrorMsg = checkForDefaultRTWithSelectedPurpose(context, saMethodArgs);
            if (!"".equals(sErrorMsg)) {
                hmResult.put("Action", "STOP");
                hmResult.put("Message", sErrorMsg);
                return hmResult;
            }
        }
        // End of code added for Program-Project automation

        String strAutoStopOnRejection = (String) requestMap.get("AutoStopOnRejection");
        String sDescription = (String) requestMap.get("Description");
        String organizationId = (String) requestMap.get("organizationId");
        String ownerId = (String) requestMap.get("OwnerOID");
        String scope = (String) requestMap.get("scope");
        String routeTemplateId = (String) requestMap.get("objectId");
        String sAvailability = (String) requestMap.get("availability");
        String sExternalAvailable = (String) requestMap.get("txtWSFolder");// sWorkspaceName
        String sExternalAvailableId = (String) requestMap.get("folderId");// sWorkspaceId
        String strRouteTaskEdit = (String) requestMap.get("Route Task Edits");
        String sAttrRouteTaskEdit = PropertyUtil.getSchemaProperty(context, "attribute_TaskEditSetting");
        String strOldRouteTaskEdits = null;
        final String ATTRIBUTE_AUTO_STOP_ON_REJECTION = PropertyUtil.getSchemaProperty(context, "attribute_AutoStopOnRejection");
        if (strRouteTaskEdit == null) {
            strRouteTaskEdit = "";
        }
        if (routeTemplateId != null) {
            RouteTemplate routeTemplateObj = (RouteTemplate) DomainObject.newInstance(context, routeTemplateId);
            strOldRouteTaskEdits = routeTemplateObj.getAttributeValue(context, sAttrRouteTaskEdit);
            if (strOldRouteTaskEdits == null) {
                strOldRouteTaskEdits = "";
            }
            if ((sAvailability == null) || ("".equals(sAvailability))) {
                DomainObject connectDO = DomainObject.newInstance(context);
                if ((sExternalAvailableId != null) && (!sExternalAvailableId.equals(""))) {
                    connectDO.setId(sExternalAvailableId);
                    sAvailability = connectDO.getInfo(context, connectDO.SELECT_TYPE);
                }
            }
            HashMap detailsMap = new HashMap();
            ContextUtil.startTransaction(context, true);
            if (organizationId == null || "null".equals(organizationId)) {
                organizationId = "";
            }
            if (organizationId.trim().length() > 0) {
                String keyVal = "newId=";
                int i = organizationId.indexOf(keyVal);
                if (i > -1) {
                    organizationId = organizationId.substring(i + keyVal.length(), organizationId.length());
                }
            }
            detailsMap.put("ownerId", ownerId);
            detailsMap.put("availability", sAvailability);
            detailsMap.put("workspaceName", sExternalAvailable);
            detailsMap.put("workspaceId", sExternalAvailableId);
            detailsMap.put("description", sDescription);
            detailsMap.put("organizationId", organizationId);

            String oldRelId = (String) routeTemplateObj.getInfo(context, "to[" + routeTemplateObj.RELATIONSHIP_OWNING_ORGANIZATION + "].id");
            String oldScopeId = (String) routeTemplateObj.getInfo(context, "to[" + routeTemplateObj.RELATIONSHIP_OWNING_ORGANIZATION + "].from.id");
            try {
                if (oldScopeId != null && !oldScopeId.equals(organizationId) && !organizationId.equals("")) {
                    Organization organization = new Organization(organizationId);
                    DomainRelationship.modifyFrom(context, oldRelId, organization);
                    String oldRouteScopeId = (String) routeTemplateObj.getInfo(context, "to[" + routeTemplateObj.RELATIONSHIP_ROUTE_TEMPLATES + "].from.id");
                    detailsMap.put("organizationId", oldRouteScopeId);
                }

                Map mapAttributeValues = new HashMap();

                if (!strRouteTaskEdit.equals(strOldRouteTaskEdits)) {
                    mapAttributeValues.put(sAttrRouteTaskEdit, strRouteTaskEdit);
                }

                mapAttributeValues.put(routeTemplateObj.ATTRIBUTE_RESTRICT_MEMBERS, scope);

                if (strAutoStopOnRejection != null) {
                    mapAttributeValues.put(ATTRIBUTE_AUTO_STOP_ON_REJECTION, strAutoStopOnRejection);
                }

                routeTemplateObj.setAttributeValues(context, mapAttributeValues);

                routeTemplateObj.editRouteTemplate(context, detailsMap);
                ContextUtil.commitTransaction(context);
            } catch (Exception e) {
                ContextUtil.abortTransaction(context);
            }
        }
        // Start of code added for Program-Project automation
        return hmResult;
        // End of code added for Program-Project automation
    }

    public static HashMap<String, StringList> getRTForSpecificPurpose(Context context, String args[]) throws FrameworkException {
        String sRTPurpose = "";
        HashMap<String, Object> programMap;
        try {
            programMap = (HashMap) JPO.unpackArgs(args);
            Map fieldMap = (Map) programMap.get("fieldMap");
            sRTPurpose = (String) fieldMap.get(DomainConstants.SELECT_NAME);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        String sWhereClause = "current==Active && revision==last";
        StringList selectStmts = new StringList();
        selectStmts.add(DomainConstants.SELECT_ID);
        selectStmts.add(DomainConstants.SELECT_TYPE);
        selectStmts.add(DomainConstants.SELECT_NAME);

        DomainObject doPerson;

        doPerson = DomainObject.newInstance(context, PersonUtil.getPersonObjectID(context));

        List<String> lsUserOrgIds = new ArrayList<>();

        MapList mpBUAndDepFromUser = new MapList();
        ExpansionIterator expIter;
        try {
            ContextUtil.startTransaction(context, false);
            expIter = doPerson.getExpansionIterator(context, DomainConstants.RELATIONSHIP_MEMBER + "," + DomainConstants.RELATIONSHIP_BUSINESS_UNIT_EMPLOYEE,
                    DomainConstants.TYPE_BUSINESS_UNIT + "," + DomainConstants.TYPE_DEPARTMENT, selectStmts, new StringList(), true, false, (short) 1, "", null, (short) 0, false, false, (short) 100,
                    false);

            while (expIter.hasNext()) {
                RelationshipWithSelect relBUAndDepFromUser = expIter.next();
                lsUserOrgIds.add(relBUAndDepFromUser.getTargetSelectData(DomainConstants.SELECT_ID));
            }
            expIter.close();
        } catch (MatrixException me) {
            logger.error("Error : {}", me.getMessage());
        } finally {
            try {
                ContextUtil.commitTransaction(context);
            } catch (FrameworkException e) {
                e.printStackTrace();
            }
        }

        TreeMap<String, String> tmRouteTemplates = new TreeMap<>();
        boolean bDefaultRTExists = false;
        try {
            ContextUtil.startTransaction(context, false);
            expIter = doPerson.getExpansionIterator(context,
                    DomainConstants.RELATIONSHIP_ROUTE_TEMPLATES + "," + DomainConstants.RELATIONSHIP_COMPANY_DEPARTMENT + "," + DomainConstants.RELATIONSHIP_DIVISION,
                    DomainConstants.TYPE_ROUTE_TEMPLATE + "," + DomainConstants.TYPE_BUSINESS_UNIT + "," + DomainConstants.TYPE_DEPARTMENT, selectStmts, new StringList(), true, false, (short) 1,
                    "(type != 'Route Template') || (current==Active && revision==last)", null, (short) 0, false, false, (short) 100, false);
            while (expIter.hasNext()) {
                RelationshipWithSelect relRTFromUser = expIter.next();
                String sRTIid = relRTFromUser.getTargetSelectData(DomainConstants.SELECT_ID);
                String sRTType = relRTFromUser.getTargetSelectData(DomainConstants.SELECT_TYPE);

                if (DomainConstants.TYPE_ROUTE_TEMPLATE.equals(sRTType)) {
                    String sRTName = relRTFromUser.getTargetSelectData(DomainConstants.SELECT_NAME);

                    tmRouteTemplates.put(sRTName, sRTIid);
                }

            }
            expIter.close();
        } catch (MatrixException me) {
            logger.error("Error : {}", me.getMessage());
        } finally {
            ContextUtil.abortTransaction(context);
        }

        try {
            DomainObject doCompany = DomainObject.newInstance(context, PersonUtil.getUserCompanyId(context));
            ContextUtil.startTransaction(context, false);
            expIter = doCompany.getExpansionIterator(context, DomainConstants.RELATIONSHIP_ROUTE_TEMPLATES, DomainConstants.TYPE_ROUTE_TEMPLATE, selectStmts, new StringList(), false, true, (short) 1,
                    sWhereClause, null, (short) 0, false, false, (short) 100, false);

            while (expIter.hasNext()) {
                RelationshipWithSelect relRTFromUser = expIter.next();
                String sRTId = relRTFromUser.getTargetSelectData(DomainConstants.SELECT_ID);
                String sRTName = relRTFromUser.getTargetSelectData(DomainConstants.SELECT_NAME);
                tmRouteTemplates.put(sRTName, sRTId);
            }
            expIter.close();
        } catch (MatrixException me) {
            logger.error("Error : {}", me.getMessage());
        } finally {
            ContextUtil.abortTransaction(context);
        }

        return checkRTForSpecificPurpose(context, tmRouteTemplates, lsUserOrgIds, sRTPurpose);
    }

    private static HashMap<String, StringList> checkRTForSpecificPurpose(Context context, TreeMap<String, String> tmRoutesTemplates, List<String> lUserOrgIds, String sRTPurpose) {
        HashMap<String, StringList> hmRTForSpecificPurpose = new HashMap<String, StringList>();

        StringList slChoices = new StringList();
        StringList slDisplayChoices = new StringList();
        hmRTForSpecificPurpose.put("field_choices", slChoices);
        hmRTForSpecificPurpose.put("field_display_choices", slDisplayChoices);

        Iterator<String> itRouteTemplateName = tmRoutesTemplates.keySet().iterator();

        String sAutoStopOnRejection = "Immediate";

        String selectOwningOrganizationType = "to[" + DomainConstants.RELATIONSHIP_OWNING_ORGANIZATION + "].from." + DomainConstants.SELECT_TYPE;
        StringList slRTInformation = new StringList();
        slRTInformation.add(DomainConstants.SELECT_CURRENT);
        slRTInformation.add(DomainConstants.SELECT_REVISION);
        slRTInformation.add(DomainConstants.SELECT_OWNER);
        slRTInformation.add(RouteTemplate.SELECT_OWNING_ORGANIZATION_ID);
        slRTInformation.add(selectOwningOrganizationType);
        slRTInformation.add("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE + "].value");
        slRTInformation.add("attribute[" + TigerConstants.ATTRIBUTE_AUTOSTOPONREJECTION + "].value");
        slRTInformation.add("attribute[" + TigerConstants.ATTRIBUTE_FPDM_RTDEFAULT + "].value");
        slRTInformation.add("attribute[" + TigerConstants.ATTRIBUTE_FPDM_RTPURPOSE + "].value");

        boolean bDefaultRTExists = false;
        while (itRouteTemplateName.hasNext()) {
            String sRTName = itRouteTemplateName.next();
            String sRTId = tmRoutesTemplates.get(sRTName);

            String sTypeBU = PropertyUtil.getSchemaProperty(context, "type_BusinessUnit");
            String sTypeDepartment = PropertyUtil.getSchemaProperty(context, "type_Department");

            BusinessObject buRT;
            String owningOrganizationType = "";
            String sDefault = "";
            try {
                DomainObject doOwningOrganization = DomainObject.newInstance(context);
                buRT = new BusinessObject(sRTId);
                BusinessObjectWithSelect bosRT = buRT.select(context, slRTInformation);

                String sOwningOrganizationId = bosRT.getSelectData(RouteTemplate.SELECT_OWNING_ORGANIZATION_ID);
                if (!sOwningOrganizationId.isEmpty()) {
                    doOwningOrganization.setId(sOwningOrganizationId);
                    owningOrganizationType = bosRT.getSelectData(selectOwningOrganizationType);
                }

                StringList slPurposes = bosRT.getSelectDataList("attribute[" + TigerConstants.ATTRIBUTE_FPDM_RTPURPOSE + "].value");
                sDefault = bosRT.getSelectData("attribute[" + TigerConstants.ATTRIBUTE_FPDM_RTDEFAULT + "].value");

                if ((context.getUser().equals(bosRT.getSelectData(DomainConstants.SELECT_OWNER))) || ((!owningOrganizationType.equals(sTypeBU)) && (!owningOrganizationType.equals(sTypeDepartment)))
                        || (lUserOrgIds.contains(sOwningOrganizationId))) {
                    if (("Approval".equals(bosRT.getSelectData("attribute[" + DomainConstants.ATTRIBUTE_ROUTE_BASE_PURPOSE + "].value"))) && slPurposes.contains(sRTPurpose)
                            && (sAutoStopOnRejection.equals(bosRT.getSelectData("attribute[" + TigerConstants.ATTRIBUTE_AUTOSTOPONREJECTION + "].value")))) {
                        if ("TRUE".equalsIgnoreCase(sDefault)) {
                            slChoices.add(0, sRTId);
                            slDisplayChoices.add(0, sRTName);
                            bDefaultRTExists = true;
                        } else {
                            slChoices.add(sRTId);
                            slDisplayChoices.add(sRTName);
                        }
                    }
                }
            } catch (MatrixException e) {
                e.printStackTrace();
            }

        }

        if (bDefaultRTExists) {
            slChoices.addElement("");
            slDisplayChoices.addElement("");
        } else {
            slChoices.add(0, "");
            slDisplayChoices.add(0, "");
        }

        return hmRTForSpecificPurpose;
    }
}
