package pss.ecm.impactanalysis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.ui.UIUtil;
import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;
import com.matrixone.apps.domain.DomainRelationship;

public class ImpactAnalysisUtil_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ImpactAnalysisUtil_mxJPO.class);

    String[] lsIAAllowedRoles = new String[] {};

    // TIGTK-7585:Pranjali Tupe :29/8/2017:START
    /**
     * @Description : This method is used to create ImpactAnalysis Structure when CR is promoted to submit state
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void createImpactAnalysisObjectStructure(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:createImpactAnalysisObjectStructure:START");
        boolean isTransactionActive = false;
        try {

            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;

            Map<String, DomainObject> mpDomainVsRAE = new HashMap<String, DomainObject>();

            String strCRObjId = args[0];
            DomainObject domCR = DomainObject.newInstance(context, strCRObjId);

            String strIAId = domCR.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].to.id");

            if (UIUtil.isNotNullAndNotEmpty(strIAId)) {
                return;
            }

            DomainObject domLA = getListOfAssessor(context, domCR);
            if (domLA == null) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSSEnterpriseChangeMgt.Notice.NoListOfAssessorForProgramProject");
                throw new Exception(strMessage);
            }
            DomainObject domIA = createAndConnectIAtoCR(context, domCR);
            DomainObject.MULTI_VALUE_LIST.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");
            StringList lsLARoleList = domLA.getInfoList(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");

            if (lsLARoleList.isEmpty()) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSSEnterpriseChangeMgt.Notice.NoRolesForListOfAssessor");
                throw new Exception(strMessage);
            }
            // TIGTK-12983 : JV : START
            pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
            boolean isJV = jvUtil.isJVProgramProject(context, strCRObjId);
            // TIGTK-12983 : JV : END
            for (Object objRole : lsLARoleList) {
                String strRoleValue = (String) objRole;
                // TIGTK-12983 : JV : START
                if(isJV) {                    
                    //strRoleValue=strRoleValue+"_"+TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV;
                }
                // TIGTK-12983 : JV : END
                createAndConnectRAtoIA(context, domIA, strRoleValue, mpDomainVsRAE);
            }
            DomainObject.MULTI_VALUE_LIST.remove("to[" + TigerConstants.RELATIONSHIP_ASSIGNEDISSUE + "].from.name");
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:createImpactAnalysisObjectStructure:END");
        } catch (Exception ex) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:createImpactAnalysisObjectStructure:ERROR ", ex);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            throw ex;
        }
    }

    /**
     * @Description : This method is used to create And Connect IA to CR
     * @param context
     * @param domCR
     * @return DomainObject
     * @throws Exception
     */
    private DomainObject createAndConnectIAtoCR(Context context, DomainObject domCR) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:createAndConnectIAtoCR:START");
        DomainObject domIAObject = null;
        try {
            PropertyUtil.setGlobalRPEValue(context, "PSS_CREATE_IA", "True");
            String strImpactAnalysisId = FrameworkUtil.autoName(context, "type_PSS_ImpactAnalysis", "policy_PSS_ImpactAnalysis");
            domIAObject = DomainObject.newInstance(context, strImpactAnalysisId);
            // TIGTK-11999 :START
            String strCRChangeCoordinator = domCR.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].to.name");
            domIAObject.setOwner(context, strCRChangeCoordinator);
            // TIGTK-11999 :END

            DomainRelationship.connect(context, domCR, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, domIAObject);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:createAndConnectIAtoCR:END");
        } catch (Exception ex) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:createAndConnectIAtoCR:ERROR ", ex);
            throw ex;
        } finally {
            PropertyUtil.setGlobalRPEValue(context, "PSS_CREATE_IA", "");
        }
        return domIAObject;
    }

    /**
     * @Description : This method is used to getListOfAssessor for purpose of release on CR
     * @param context
     * @param domCR
     * @return DomainObject
     * @throws Exception
     */
    private DomainObject getListOfAssessor(Context context, DomainObject domCR) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getListOfAssessor:START");
        DomainObject domLA = null;
        try {

            String strProgramProjectID = domCR.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");
            DomainObject domProgramProject = DomainObject.newInstance(context, strProgramProjectID);
            String strPurposeOfRelease = domCR.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);

            StringList objectSelects = new StringList();
            objectSelects.add(DomainConstants.SELECT_ID);

            StringList relSelect = new StringList();
            relSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            relSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");

            String strRelWhereclause = "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "] == \"" + strPurposeOfRelease + "\"";

            MapList mlListOfAssessor = domProgramProject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDASSESSORS, TigerConstants.TYPE_PSS_LISTOFASSESSORS, objectSelects,
                    relSelect, false, // getTo false
                    true, // getFrom true
                    (short) 1, // 1
                    null, strRelWhereclause, 0); // limit 0

            if (mlListOfAssessor.size() <= 0) {
                return null;
            }

            Map<?, ?> mpLA = (Map<?, ?>) mlListOfAssessor.get(0);
            String strLAId = (String) mpLA.get(DomainConstants.SELECT_ID);
            domLA = DomainObject.newInstance(context, strLAId);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getListOfAssessor:END");
        } catch (Exception ex) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:getListOfAssessor:ERROR ", ex);
            throw ex;
        }
        return domLA;
    }

    /**
     * @Description : This method is used to create And Connect RA to IA
     * @param context
     * @param domIA
     * @param strRoleValue
     * @throws Exception
     */
    private void createAndConnectRAtoIA(Context context, DomainObject domIA, String strRoleValue, Map mpDomainVsRAE) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:createAndConnectRAtoIA:START");
        try {

            Map<String, String> relAtt = new HashMap<String, String>();
            String strRAObjId = FrameworkUtil.autoName(context, "type_PSS_RoleAssessment", "policy_PSS_RoleAssessment");

            DomainObject domRA = DomainObject.newInstance(context, strRAObjId);

            String strCRId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
            domRA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_RAROLE, strRoleValue);
            String strRAOwner = getRAOwner(context, strRoleValue, strCRId);

            DomainRelationship domRelRA = DomainRelationship.connect(context, domIA, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, domRA);

            String sRADueDate = getRADueDate(context, strCRId);
            relAtt.put(TigerConstants.ATTRIBUTE_PSS_RAROLE, strRoleValue);

            if (!sRADueDate.isEmpty()) {
                relAtt.put(TigerConstants.ATTRIBUTE_PSS_DUEDATE, sRADueDate);
            }
            domRelRA.setAttributeValues(context, relAtt);

            createAndConnectRAEtoRA(context, domRA, strRoleValue, strRAOwner, mpDomainVsRAE);
            domRA.setOwner(context, strRAOwner);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:createAndConnectRAtoIA:END");
        } catch (Exception ex) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:createAndConnectRAtoIA:ERROR ", ex);
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * @Description : This method is used to get RA Owner
     * @param context
     * @param strRoleValue
     * @param strCRId
     * @return
     * @throws Exception
     */
    private String getRAOwner(Context context, String strRoleValue, String strCRId) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRAOwner:START");
        String strRAOwner = DomainConstants.EMPTY_STRING;
        try {

            StringList slRolesList = new StringList();
            slRolesList.add(strRoleValue);

            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, null);
            StringList slLeadRoleMember = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, true);

            if (slLeadRoleMember.isEmpty()) {
                slRolesList.clear();
                // TIGTK-12983 : JV : START
                pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
                boolean isJV = jvUtil.isJVProgramProject(context, strCRId);
                if(isJV) {
                    slRolesList.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
                } else {
                slRolesList.add(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                }
                // TIGTK-12983 : JV : END               
                slLeadRoleMember = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, true);
            }
            if (!slLeadRoleMember.isEmpty()) {
                strRAOwner = (String) slLeadRoleMember.get(0);
            }

            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRAOwner:END");
        } catch (Exception ex) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRAOwner:ERROR ", ex);
            ex.printStackTrace();
            throw ex;
        }
        return strRAOwner;
    }

    /**
     * @Description : This method is used to get RA DueDate
     * @param context
     * @param strCRId
     * @return
     * @throws Exception
     */
    private String getRADueDate(Context context, String strCRId) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRADueDate:START");
        String sCRRequestedAssessmentEndDate = DomainConstants.EMPTY_STRING;
        try {

            DomainObject domCR = DomainObject.newInstance(context, strCRId);
            sCRRequestedAssessmentEndDate = domCR.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE);

        } catch (Exception e) {
            throw e;
        }
        return sCRRequestedAssessmentEndDate;
    }

    /**
     * @Description : This method is used to create And Connect RAE to RA
     * @param context
     * @param domRA
     * @param strRoleValue
     * @throws Exception
     */
    public void createAndConnectRAEtoRA(Context context, DomainObject domRA, String strRoleValue, String strRAOwner, Map<String, DomainObject> mpDomainVsRAE) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:createAndConnectRAEtoRA:START");
        boolean isPushed = false;
        try {

            Map<String, List<Map<String, String>>> mpViewVsDisplayAttrMap = pss.ecm.impactanalysis.ImpactAnalysisXMLUtil_mxJPO.getViewVsDisplayAttrMap(context, strRoleValue);

            for (Entry<String, List<Map<String, String>>> entryViewVsDisplayAttributes : mpViewVsDisplayAttrMap.entrySet()) {

                String strView = entryViewVsDisplayAttributes.getKey();
                List<Map<String, String>> lstDisplayAttributes = entryViewVsDisplayAttributes.getValue();

                for (Map<String, String> mapDisplayAttribute : lstDisplayAttributes) {

                    DomainObject domRAE = null;

                    String strPSS_Domain = (String) mapDisplayAttribute.get(TigerConstants.ATTRIBUTE_PSS_DOMAIN);
                    String strPSS_Title = (String) mapDisplayAttribute.get(TigerConstants.ATTRIBUTE_PSS_TITLE);
                    String strTitle_Domain = strPSS_Title + "." + strPSS_Domain;
                    if ("FISSummaryView".equals(strView) && mpDomainVsRAE != null && mpDomainVsRAE.containsKey(strTitle_Domain)) {
                        domRAE = (DomainObject) mpDomainVsRAE.get(strTitle_Domain);
                    } else {
                        domRAE = createRAE(context, mapDisplayAttribute);
                        if ("FISSummaryView".equals(strView)) {
                            mpDomainVsRAE.put(strTitle_Domain, domRAE);
                        }
                    }
                    connectRAEToRA(context, domRA, domRAE, strView);

                    try {
                        ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                        isPushed = true;
                        domRAE.setOwner(context, strRAOwner);
                    } catch (Exception e) {
                        throw e;
                    } finally {
                        if (isPushed) {
                            ContextUtil.popContext(context);
                        }

                    }
                }
                logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:createAndConnectRAEtoRA:END");
            }
        } catch (Exception ex) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:createAndConnectRAEtoRA:ERROR ", ex);
            throw ex;
        }

    }

    /**
     * @Description : This method is used to create RAE
     * @param context
     * @param mapDisplayAttribute
     * @return
     * @throws Exception
     */
    public DomainObject createRAE(Context context, Map<?, ?> mapDisplayAttribute) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:createRAE:START");
        DomainObject domRAE = null;
        try {
            String strRAEObjId = FrameworkUtil.autoName(context, "type_PSS_RoleAssessmentEvaluation", "policy_PSS_RoleAssessmentEvaluation");

            domRAE = DomainObject.newInstance(context, strRAEObjId);

            if (mapDisplayAttribute != null)
                domRAE.setAttributeValues(context, mapDisplayAttribute);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:createRAE:END");
        } catch (Exception ex) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:createRAE:ERROR ", ex);
            throw ex;
        }
        return domRAE;
    }

    /**
     * @Description : This method is used to connect RAE To RA
     * @param context
     * @param domRA
     * @param domRAE
     * @param strView
     * @throws Exception
     */
    public void connectRAEToRA(Context context, DomainObject domRA, DomainObject domRAE, String strView) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectRAEToRA:START");
        try {

            DomainRelationship domRel = DomainRelationship.connect(context, domRA, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION, domRAE);
            domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VIEW, strView);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectRAEToRA:END");
        } catch (Exception ex) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectRAEToRA:ERROR ", ex);
            throw ex;
        }

    }

    // TIGTK-7585:Pranjali Tupe :29/8/2017:END

    /***
     * This method is used to show Impact analysis objects related to CR
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public MapList getConnectedImpactAnalyis(Context context, String[] args) throws Exception {
        logger.debug("ImpactAnalysisUtil_mxJPO:getConnectedImpactAnalyis:START ");
        try {
            HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strObjId = (String) paramMap.get("objectId");

            StringList slObjectSle = new StringList();
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);

            DomainObject domCRObj = DomainObject.newInstance(context, strObjId);

            MapList mlImpactAnalysis = domCRObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjectSle,
                    DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, DomainConstants.EMPTY_STRING, null, 0);
            logger.debug("ImpactAnalysisUtil_mxJPO:getConnectedImpactAnalyis:END ");
            return mlImpactAnalysis;
        } catch (Exception ex) {
            logger.error("error in ImpactAnalysisUtil_mxJPO:getConnectedImpactAnalyis: " + ex.getMessage());
            throw ex;
        }
    }

    /**
     * TIGTK-7595 : Dhiren PATEL Demote CR START
     * @param context
     * @param args
     * @throws MatrixException
     */
    public static void reviseImpactAnalysisStructure(Context context, String strCRObjId) throws Exception {

        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseImpactAnalysisStructure:START");
        boolean isTransactionActive = false;
        try {
            ContextUtil.startTransaction(context, true);
            isTransactionActive = true;
            DomainObject domCR = DomainObject.newInstance(context, strCRObjId);
            StringList slObjectSle = new StringList();
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            String strwhere = "revision == last";
            MapList mlIA = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjectSle, DomainConstants.EMPTY_STRINGLIST,
                    false, true, (short) 1, strwhere, null, 0);

            if (!mlIA.isEmpty()) {
                Map<?, ?> mIA = (Map<?, ?>) mlIA.get(0);
                String sImpactAnalysisID = (String) mIA.get(DomainConstants.SELECT_ID);

                reviseImpactAnalysisObject(context, domCR, sImpactAnalysisID);
            }
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseImpactAnalysisStructure:END");
        } catch (MatrixException e) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseImpactAnalysisStructure:ERROR ", e);
            if (isTransactionActive) {
                ContextUtil.abortTransaction(context);
            }
            e.printStackTrace();
            throw e;
        } finally {
            if (isTransactionActive) {
                ContextUtil.commitTransaction(context);
            }
        }

    }

    /**
     * @param context
     * @param domCR
     * @param sImpactAnalysisID
     * @throws MatrixException
     */
    private static void reviseImpactAnalysisObject(Context context, DomainObject domCR, String sImpactAnalysisID) throws Exception {

        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseImpactAnalysisObject:START");
        try {
            StringList slBusSelect = new StringList(DomainConstants.SELECT_NAME);
            slBusSelect.add(DomainConstants.SELECT_TYPE);
            slBusSelect.add(DomainConstants.SELECT_REVISION);
            slBusSelect.add(DomainConstants.SELECT_OWNER);

            String strIAName = DomainConstants.EMPTY_STRING;
            String strIAType = DomainConstants.EMPTY_STRING;
            String strIARevision = DomainConstants.EMPTY_STRING;
            String strIAOwner = DomainConstants.EMPTY_STRING;

            DomainObject domImpactAnalysis = DomainObject.newInstance(context, sImpactAnalysisID);
            Map<String, Object> mapIAInfo = (Map<String, Object>) domImpactAnalysis.getInfo(context, slBusSelect);
            if (!mapIAInfo.isEmpty()) {
                strIAName = (String) mapIAInfo.get(DomainConstants.SELECT_NAME);
                strIAType = (String) mapIAInfo.get(DomainConstants.SELECT_TYPE);
                strIARevision = (String) mapIAInfo.get(DomainConstants.SELECT_REVISION);
                strIAOwner = (String) mapIAInfo.get(DomainConstants.SELECT_OWNER);
            }

            BusinessObject busRevisedImpactAnalysis = domImpactAnalysis.reviseObject(context, false);
            DomainObject domRevisedImpactAnalysis = new DomainObject(busRevisedImpactAnalysis);
            domRevisedImpactAnalysis.setOwner(context, strIAOwner);
            DomainRelationship.connect(context, domCR, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, domRevisedImpactAnalysis);
            StringList slobjSelect = new StringList();
            slobjSelect.addElement(DomainConstants.SELECT_ID);
            MapList mplRoleAssessment = domImpactAnalysis.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slobjSelect, null, false,
                    true, (short) 1, null, null, 0);

            reviseRoleAssessmentObjects(context, mplRoleAssessment, domRevisedImpactAnalysis);
            // TIGTK-13618 : START
            ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            domImpactAnalysis.promote(context);
            ContextUtil.popContext(context);
            StringBuffer sbHistoryAction = new StringBuffer();
            sbHistoryAction.delete(0, sbHistoryAction.length());
            sbHistoryAction.append(" Add history for Promote ");
            sbHistoryAction.append(strIAName);
            sbHistoryAction.append(" ");
            sbHistoryAction.append(strIAType);
            sbHistoryAction.append(" ");
            sbHistoryAction.append(strIARevision);
            modifyHistory(context, sImpactAnalysisID, sbHistoryAction.toString(), " ");
            // TIGTK-13618 : START

            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseImpactAnalysisObject:END");
        } catch (RuntimeException e) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseImpactAnalysisObject:ERROR ", e);
            throw e;
        }
    }

    /**
     * @param context
     * @param mplRoleAssessment
     * @param domRevisedImpactAnalysis
     * @throws MatrixException
     */
    private static void reviseRoleAssessmentObjects(Context context, MapList mplRoleAssessment, DomainObject domRevisedImpactAnalysis) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseRoleAssessmentObjects:START");
        try {
            StringList slBusSelect = new StringList();
            Set<String> setRevisedRAEs = new HashSet<String>();
            slBusSelect.add(DomainConstants.SELECT_ID);
            StringList slRelSelect = new StringList();
            slRelSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_VIEW + "]");
            Iterator<Map<?, ?>> iterRoleAssessment = mplRoleAssessment.iterator();
            while (iterRoleAssessment.hasNext()) {
                Map<?, ?> mpRoleAssessmentInfo = iterRoleAssessment.next();
                String sRoleAssessmentId = (String) mpRoleAssessmentInfo.get(DomainConstants.SELECT_ID);
                DomainObject domRoleAssessment = DomainObject.newInstance(context, sRoleAssessmentId);
                // TIGTK-13618 : START
                StringList slRABusSelect = new StringList(DomainConstants.SELECT_NAME);
                slRABusSelect.add(DomainConstants.SELECT_TYPE);
                slRABusSelect.add(DomainConstants.SELECT_REVISION);

                String strRAName = DomainConstants.EMPTY_STRING;
                String strRAType = DomainConstants.EMPTY_STRING;
                String strRARevision = DomainConstants.EMPTY_STRING;

                Map<String, Object> mapRAInfo = (Map<String, Object>) domRoleAssessment.getInfo(context, slRABusSelect);
                if (!mapRAInfo.isEmpty()) {
                    strRAName = (String) mapRAInfo.get(DomainConstants.SELECT_NAME);
                    strRAType = (String) mapRAInfo.get(DomainConstants.SELECT_TYPE);
                    strRARevision = (String) mapRAInfo.get(DomainConstants.SELECT_REVISION);
                }

                String strRAOwner = domRoleAssessment.getInfo(context, DomainConstants.SELECT_OWNER);
                BusinessObject busRevisedRoleAssessment = domRoleAssessment.reviseObject(context, false);
                DomainObject domRevisedRoleAssessment = new DomainObject(busRevisedRoleAssessment);
                domRevisedRoleAssessment.setOwner(context, strRAOwner);
                DomainRelationship.connect(context, domRevisedImpactAnalysis, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, domRevisedRoleAssessment);
                // TIGTK-13641 : Vishal T :START
                domRevisedRoleAssessment.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_READYFORDESCISION, "FALSE");
                // TIGTK-13641 : Vishal T :END
                MapList mplRoleAssessmentEvaluation = domRoleAssessment.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION,
                        TigerConstants.TYPE_PSS_ROLEASSESSMENT_EVALUATION, slBusSelect, slRelSelect, false, true, (short) 1, null, null, 0);
                reviseRoleAssessmentEvaluationObjects(context, mplRoleAssessmentEvaluation, domRevisedRoleAssessment, setRevisedRAEs);
                ContextUtil.pushContext(context, TigerConstants.PERSON_USER_AGENT, DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                domRoleAssessment.promote(context);
                ContextUtil.popContext(context);
                StringBuffer sbHistoryAction = new StringBuffer();
                sbHistoryAction.delete(0, sbHistoryAction.length());
                sbHistoryAction.append(" Add history for Promote ");
                sbHistoryAction.append(strRAName);
                sbHistoryAction.append(" ");
                sbHistoryAction.append(strRAType);
                sbHistoryAction.append(" ");
                sbHistoryAction.append(strRARevision);
                modifyHistory(context, sRoleAssessmentId, sbHistoryAction.toString(), " ");
                // TIGTK-13618 : START
            }
            setRevisedRAEs.clear();
            setRevisedRAEs = null;
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseRoleAssessmentObjects:END");
        } catch (RuntimeException e) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseRoleAssessmentObjects:ERROR ", e);
            throw e;
        }

    }

    /**
     * @param context
     * @param mplDisplayAttributes
     * @param domRevisedRoleAssessment
     * @throws FrameworkException
     */
    private static void reviseRoleAssessmentEvaluationObjects(Context context, MapList mplDisplayAttributes, DomainObject domRevisedRoleAssessment, Set<String> setRevisedRAEs) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseRoleAssessmentEvaluationObjects:START");
        try {

            Iterator<Map<?, ?>> iterRoleAssessmentEvaluation = mplDisplayAttributes.iterator();
            while (iterRoleAssessmentEvaluation.hasNext()) {
                Map<?, ?> mpRoleAssessmentEvaluationInfo = iterRoleAssessmentEvaluation.next();
                String sRoleAssessmentEvaluationId = (String) mpRoleAssessmentEvaluationInfo.get(DomainConstants.SELECT_ID);
                String strView = (String) mpRoleAssessmentEvaluationInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_VIEW + "]");
                DomainObject domRoleAssessmentEvaluation = DomainObject.newInstance(context, sRoleAssessmentEvaluationId);
                // TIGTK-14457 : Get Owner of old revision
                String strRAOwner = domRevisedRoleAssessment.getInfo(context, DomainConstants.SELECT_OWNER);

                BusinessObject busRevisedRoleAssessmentEvaluation = null;
                if (setRevisedRAEs.contains(sRoleAssessmentEvaluationId)) {
                    busRevisedRoleAssessmentEvaluation = domRoleAssessmentEvaluation.getNextRevision(context);
                } else {
                    setRevisedRAEs.add(sRoleAssessmentEvaluationId);
                    busRevisedRoleAssessmentEvaluation = domRoleAssessmentEvaluation.reviseObject(context, false);

                }
                DomainObject domRevisedRoleAssessmentEvaluation = new DomainObject(busRevisedRoleAssessmentEvaluation);
                DomainRelationship RoleAssessmentEvaluationRel = DomainRelationship.connect(context, domRevisedRoleAssessment, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION,
                        domRevisedRoleAssessmentEvaluation);
                // TIGTK-14457 : Set Owner
                domRevisedRoleAssessmentEvaluation.setOwner(context, strRAOwner);
                RoleAssessmentEvaluationRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_VIEW, strView);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseRoleAssessmentEvaluationObjects:END");
        } catch (RuntimeException e) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseRoleAssessmentEvaluationObjects:ERROR ", e);
            throw e;
        } catch (Exception e) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:reviseRoleAssessmentEvaluationObjects:ERROR ", e);
            throw e;
        }

    }

    /** Dhiren PATEL Demote CR END */

    /**
     * This method is used to Get List of Assessors in table
     * @param context
     * @param args
     * @return maplist
     * @author sirale
     * @throws Exception
     */
    public MapList getListOfAssessors(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getListOfAssessors:START");
        try {
            StringList slbusSelects = new StringList();
            slbusSelects.add(DomainConstants.SELECT_ID);

            MapList mlLAList = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_LISTOFASSESSORS, DomainConstants.QUERY_WILDCARD, DomainConstants.QUERY_WILDCARD,
                    DomainConstants.QUERY_WILDCARD, TigerConstants.VAULT_ESERVICEPRODUCTION, DomainConstants.EMPTY_STRING, true, slbusSelects);

            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getListOfAssessors:END");
            return mlLAList;
        } catch (Exception ex) {
            logger.error("Error in  pss.ecm.impactanalysis.ImpactAnalysisUtil:getListOfAssessors:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * This method is used to get Organization in Create form
     * @param context
     * @param args
     * @return String
     * @author sirale
     * @throws Exception
     */
    public String getOrganizationForLA(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getOrganizationForLA:START");
        String strOrganizationValue = DomainConstants.EMPTY_STRING;
        try {
            String strSecurityContext = PersonUtil.getDefaultSecurityContext(context, context.getUser());
            strOrganizationValue = (strSecurityContext.split("[.]")[1]);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getOrganizationForLA:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getOrganizationForLA:ERROR ", ex);
            throw ex;
        }
        return strOrganizationValue;
    }

    /**
     * This method is used to Check Default LA is present in database
     * @param context
     * @param args
     * @return string
     * @author sirale
     * @throws Exception
     */
    public String checkModificationsForDefaultLAValue(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkModificationsForDefaultLAValue:START");
        String strResult = DomainConstants.EMPTY_STRING;
        try {

            Map<?, ?> programMap = JPO.unpackArgs(args);
            String strPurposeOfRelease = (String) programMap.get("strPurposeOfReleaseVal");
            String strOrganization = (String) programMap.get("strOrganization");

            String sWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]== \"" + strPurposeOfRelease + "\"&&" + DomainConstants.SELECT_ORGANIZATION + " ==\"" + strOrganization
                    + "\"&&attribute[" + TigerConstants.ATTRIBUTE_PSS_LADEFAULTVALUE + "]==true";

            MapList mlListOfAssessors = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_LISTOFASSESSORS, TigerConstants.VAULT_ESERVICEPRODUCTION, sWhere,
                    new StringList(DomainConstants.SELECT_ID));

            if (!mlListOfAssessors.isEmpty()) {
                strResult = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.DefaultLAExists");
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkModificationsForDefaultLAValue:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:checkModificationsForDefaultLAValue:ERROR ", ex);
            throw ex;
        }
        return strResult;
    }

    /**
     * This method is used to remove LA
     * @param context
     * @param args
     * @return
     * @author sirale
     * @throws Exception
     */
    public void disconnectLA(Context context, String[] args) throws Exception {
        try {
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:disconnectLA:STARTS");
            Map<?, ?> programMap = JPO.unpackArgs(args);
            StringList slLAObjectIds = (StringList) programMap.get("objectIds");

            StringList slLAObjectsNotconnectedToPP = getLANotConnectedToPP(context, slLAObjectIds);
            if (!slLAObjectsNotconnectedToPP.isEmpty()) {
                for (String strObjectId : slLAObjectsNotconnectedToPP.toList()) {
                    slLAObjectIds.removeAll(slLAObjectsNotconnectedToPP);
                    DomainObject.deleteObjects(context, new String[] { strObjectId });
                }
            }
            if (!slLAObjectIds.isEmpty()) {
                StringBuffer sbLANameList = new StringBuffer();

                // TIGTK-15642:29-06-2018:STARTS
                String strLAConnectedToPPlert = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.RemoveLA");
                sbLANameList.append(strLAConnectedToPPlert);
                sbLANameList.append("\n");

                int intLAIdSize = slLAObjectIds.size();
                for (int intLAIds = 0; intLAIds < intLAIdSize; intLAIds++) {
                    String strObjectID = (String) slLAObjectIds.get(intLAIds);
                    DomainObject domObjLA = DomainObject.newInstance(context, strObjectID);
                    String strLAName = domObjLA.getInfo(context, DomainConstants.SELECT_NAME);
                    if (intLAIds != 0) {
                        sbLANameList.append(",");
                    }
                    sbLANameList.append(strLAName);
                }
                // TIGTK-15642:29-06-2018:ENDS
                MqlUtil.mqlCommand(context, "notice $1", sbLANameList.toString());
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:disconnectLA:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:disconnectLA:ERROR ", ex);
            // TIGTK-15642:29-06-2018:STARTS
            String strErrorMessage = ex.getMessage();
            strErrorMessage = strErrorMessage.replace("java.lang.Exception: Message:", DomainConstants.EMPTY_STRING);
            strErrorMessage = strErrorMessage.replace("Severity:2 ErrorCode:1500028", DomainConstants.EMPTY_STRING);
            MqlUtil.mqlCommand(context, "notice $1", strErrorMessage);
            // TIGTK-15642:29-06-2018:ENDS
        }
    }

    /**
     * This method is used to get LA which are connected to PP
     * @param context
     * @param args
     * @return StringList
     * @author sirale
     * @throws Exception
     */
    public StringList getLANotConnectedToPP(Context context, StringList slLADeleteObjIds) throws FrameworkException {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getLANotConnectedToPP:START");
        StringList slLANotConnectedToPP = new StringList();
        String FALSE = "False";
        try {
            for (String strObjectId : slLADeleteObjIds.toList()) {
                DomainObject domLA = DomainObject.newInstance(context, strObjectId);
                String bIsProjectConnected = domLA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDASSESSORS + "]");
                if (FALSE.equals(bIsProjectConnected)) {
                    slLANotConnectedToPP.add(strObjectId);
                }
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getLANotConnectedToPP:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getLANotConnectedToPP:ERROR ", ex);
            throw ex;
        }
        return slLANotConnectedToPP;

    }

    /**
     * This method is used to get Roles connected to LA
     * @param context
     * @param args
     * @return MapList
     * @author sirale
     * @throws Exception
     */
    public MapList getRolesConnectedToLA(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRolesConnectedToLA:START");
        MapList mlRolesConnectedToLA = new MapList();
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            String sLAObjID = (String) programMap.get("objectId");
            DomainObject domLA = DomainObject.newInstance(context, sLAObjID);
            DomainObject.MULTI_VALUE_LIST.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");
            StringList slLARoleValues = domLA.getInfoList(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");
            while (slLARoleValues.contains(DomainConstants.EMPTY_STRING)) {
                slLARoleValues.remove(DomainConstants.EMPTY_STRING);
            }
            Iterator<?> itrRoleName = slLARoleValues.iterator();
            while (itrRoleName.hasNext()) {
                String strRoleName = (String) itrRoleName.next();
                Map<String, String> mapObject = new HashMap<String, String>();
                mapObject.put(DomainConstants.SELECT_ID, strRoleName);
                mlRolesConnectedToLA.add(mapObject);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRolesConnectedToLA:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getRolesConnectedToLA:ERROR ", ex);
            throw ex;
        }
        return mlRolesConnectedToLA;
    }

    /**
     * This method is used to get Roles in Search
     * @param context
     * @param args
     * @return MapList
     * @author sirale
     * @throws Exception
     */
    public MapList getRolesSearchResults(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRolesSearchResults:START");
        StringList lsIAAllowedRoles = new StringList();
        MapList mlListOfAssessorsRoles = new MapList();
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            String sLAObjId = (String) programMap.get("objectId");

            if (lsIAAllowedRoles.isEmpty()) {
                String strMQLCommand = "list role * where property[IS_IMPACT_ANALYSIS_ASSESSMENT_ROLE].value=='true'";
                String strMQLResult = MqlUtil.mqlCommand(context, true, strMQLCommand, true);
                lsIAAllowedRoles = FrameworkUtil.split(strMQLResult, System.getProperty("line.separator"));
            }

            DomainObject domLA = DomainObject.newInstance(context, sLAObjId);
            DomainObject.MULTI_VALUE_LIST.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");
            StringList slLARoleValues = domLA.getInfoList(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");

            for (int i = 0; i < lsIAAllowedRoles.size(); i++) {
                String strRoleName = (String) lsIAAllowedRoles.get(i);
                if (UIUtil.isNotNullAndNotEmpty(strRoleName)) {
                    if (!slLARoleValues.contains(strRoleName)) {
                        Map<String, String> mpLARole = new HashMap<String, String>();
                        mpLARole.put(DomainConstants.SELECT_ID, strRoleName);
                        mlListOfAssessorsRoles.add(mpLARole);
                    }
                }
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRolesSearchResults:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getRolesSearchResults:ERROR ", ex);
            throw ex;
        }
        return mlListOfAssessorsRoles;
    }

    /**
     * This method is used to get Roles connected to LA
     * @param context
     * @param args
     * @return
     * @author sirale
     * @throws Exception
     */
    public void connectRoleToLA(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectRoleToLA:START");
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);

            String sLAObjId = (String) programMap.get("objectId");
            StringList slLARoleList = (StringList) programMap.get("slSelectedRows");

            DomainObject domLA = DomainObject.newInstance(context, sLAObjId);
            DomainObject.MULTI_VALUE_LIST.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");
            StringList slLARoleValues = domLA.getInfoList(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");
            slLARoleValues.addAll(slLARoleList);

            while (slLARoleValues.contains(DomainConstants.EMPTY_STRING)) {
                slLARoleValues.remove(DomainConstants.EMPTY_STRING);
            }
            AttributeList attListSelectedRoles = builtRoleAttributeList(slLARoleValues);
            domLA.setAttributeValues(context, attListSelectedRoles);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectRoleToLA:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:connectRoleToLA:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * This method is used to get Roles List
     * @param context
     * @param args
     * @return AttributeList
     * @author sirale
     * @throws Exception
     */
    public AttributeList builtRoleAttributeList(StringList slSelectedRoles) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:builtRoleAttributeList:START");
        AttributeList attListSelectedRoles = new AttributeList();
        try {
            AttributeType attTypeSelectedRoles = new AttributeType(TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES);
            Attribute attSelectedRoles = new Attribute(attTypeSelectedRoles, slSelectedRoles);
            attListSelectedRoles.addElement(attSelectedRoles);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:builtRoleAttributeList:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:builtRoleAttributeList:ERROR ", ex);
            throw ex;
        }
        return attListSelectedRoles;
    }

    /**
     * This method is used to get Short name for Role
     * @param context
     * @param args
     * @return StringList
     * @author sirale
     * @throws Exception
     */
    public StringList getShortRoleName(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getShortRoleName:START");
        StringList slRoleName = new StringList();

        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            MapList mlRoleObjs = (MapList) programMap.get("objectList");

            for (Object objRole : mlRoleObjs) {
                Map<?, ?> mpRole = (Map<?, ?>) objRole;
                String strRoleName = (String) mpRole.get(DomainConstants.SELECT_ID);
                String strKey = "emxFramework.ShortFormRoleName." + strRoleName;
                String strPSSRoleValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), strKey);
                slRoleName.addElement(strPSSRoleValue);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getShortRoleName:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getShortRoleName:ERROR ", ex);
            throw ex;
        }
        return slRoleName;
    }

    /**
     * This method is used to get Role name for description
     * @param context
     * @param args
     * @return StringList
     * @author sirale
     * @throws Exception
     */
    public StringList getRoleName(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleName:START");
        StringList slRoleName = new StringList();

        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            MapList mlRoleObjs = (MapList) programMap.get("objectList");

            for (Object objRole : mlRoleObjs) {
                Map<?, ?> mpRole = (Map<?, ?>) objRole;
                String strRoleName = (String) mpRole.get(DomainConstants.SELECT_ID);
                String strKey = "emxFramework.Role." + strRoleName;

                String strPSSRoleValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), strKey);

                slRoleName.addElement(strPSSRoleValue);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleName:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleName:ERROR ", ex);
            throw ex;
        }
        return slRoleName;
    }

    /**
     * This method is used to Remove Roles from LA
     * @param context
     * @param args
     * @return boolean
     * @author sirale
     * @throws Exception
     */
    public void removeRolesFromLA(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:removeRolesFromLA:START");
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            StringList slSelectedRoles = (StringList) programMap.get("slSelectedRows");
            String strLAObjID = (String) programMap.get("objectId");
            DomainObject domLA = DomainObject.newInstance(context, strLAObjID);

            DomainObject.MULTI_VALUE_LIST.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");
            StringList slLARoleValues = domLA.getInfoList(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");

            while (slLARoleValues.contains(DomainConstants.EMPTY_STRING)) {
                slLARoleValues.remove(DomainConstants.EMPTY_STRING);
            }
            slLARoleValues.removeAll(slSelectedRoles);

            AttributeList attListSelectedRoles = builtRoleAttributeList(slLARoleValues);
            domLA.setAttributes(context, attListSelectedRoles);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:removeRolesFromLA:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:removeRolesFromLA:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * This method is used to Connect LA to Program Project
     * @param context
     * @param args
     * @return
     * @author sirale
     * @throws Exception
     */
    public void connectListOfAssessorToProgramProject(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectListOfAssessorToProgramProject:START");
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            Map<?, ?> paramMap = (Map<?, ?>) programMap.get("paramMap");
            Map<?, ?> fieldMap = (Map<?, ?>) programMap.get("fieldMap");
            Map<?, ?> settingsMap = (Map<?, ?>) fieldMap.get("settings");
            String strNewId = (String) paramMap.get("New OID");
            String strPSS_Purpose_Of_Release = (String) settingsMap.get("fromListOfAssessor");
            String objectId = (String) paramMap.get("objectId");
            DomainObject domProgProj = DomainObject.newInstance(context, objectId);

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDASSESSORS);
            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_LISTOFASSESSORS);

            String SELECT_PURPOSE_OF_RELEASE = "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]";

            StringList slRelSelects = new StringList();
            slRelSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            slRelSelects.add(SELECT_PURPOSE_OF_RELEASE);

            String strLAType = getAppropriateAttrValue(strPSS_Purpose_Of_Release);
            String strRelWhere = SELECT_PURPOSE_OF_RELEASE + "== \"" + strLAType + "\"";

            MapList mlListOfAssessor = domProgProj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    new StringList(DomainObject.SELECT_ID), // object selects
                    slRelSelects, // relationship selects
                    false, // to direction
                    true, // from direction;
                    (short) 1, // recursion level
                    null, // object where clause
                    strRelWhere, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null, null);

            if (mlListOfAssessor.size() > 0) {
                Iterator<?> itrAssessor = mlListOfAssessor.iterator();
                while (itrAssessor.hasNext()) {
                    Map<?, ?> mapAssessor = (Map<?, ?>) itrAssessor.next();
                    String strConnectionId = (String) mapAssessor.get("id[connection]");
                    if (UIUtil.isNotNullAndNotEmpty(strConnectionId)) {
                        DomainRelationship.disconnect(context, strConnectionId);
                    }
                }
            }

            if (UIUtil.isNotNullAndNotEmpty(strNewId)) {
                DomainObject domLA = DomainObject.newInstance(context, strNewId);
                DomainRelationship drAssessor = DomainRelationship.connect(context, domProgProj, TigerConstants.RELATIONSHIP_PSS_CONNECTEDASSESSORS, domLA);
                drAssessor.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE, strLAType);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectListOfAssessorToProgramProject:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:connectListOfAssessorToProgramProject:ERROR ", ex);
            throw ex;
        }

    }

    /**
     * This method is used to include List for LA
     * @param context
     * @param args
     * @return StringList
     * @author sirale
     * @throws Exception
     */
    public StringList includeEligibleLA(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:includeEligibleLA:START");
        StringList listLAIDs = new StringList();
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            String strPSS_Purpose_Of_Release = (String) programMap.get("fromListOfAssessor");
            String objectId = (String) programMap.get("objectId");

            String strLAType = getAppropriateAttrValue(strPSS_Purpose_Of_Release);

            DomainObject domProgProj = DomainObject.newInstance(context, objectId);
            String strOrganization = domProgProj.getInfo(context, DomainConstants.SELECT_ORGANIZATION);

            String sWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]== \"" + strLAType + "\"&&" + DomainConstants.SELECT_ORGANIZATION + " ==\"" + strOrganization + "\"";

            StringList objectSelect = new StringList();
            objectSelect.add(DomainObject.SELECT_ID);

            MapList mlListOfAssessors = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_LISTOFASSESSORS, TigerConstants.VAULT_ESERVICEPRODUCTION, sWhere, objectSelect);

            if (mlListOfAssessors.size() > 0) {
                Iterator<?> itrLA = mlListOfAssessors.iterator();
                while (itrLA.hasNext()) {
                    Map<?, ?> mLA = (Map<?, ?>) itrLA.next();
                    String strLAId = (String) mLA.get(DomainConstants.SELECT_ID);
                    listLAIDs.add(strLAId);
                }
            }

            if (listLAIDs.size() == 0) {
                listLAIDs.add(" ");
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:includeEligibleLA:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:includeEligibleLA:ERROR ", ex);
            throw ex;
        }
        return listLAIDs;
    }

    /**
     * This method is used to get Purpose of Release value
     * @param context
     * @param args
     * @return String
     * @author sirale
     * @throws Exception
     */
    public String getAppropriateAttrValue(String fromTemplate) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getAppropriateAttrValue:START");
        String strAttrValueToSet = DomainObject.EMPTY_STRING;
        try {
            if ("CommercialUpdate".equals(fromTemplate)) {
                strAttrValueToSet = "Commercial Update";
            } else if ("PrototypeToolLaunchModification".equals(fromTemplate)) {
                strAttrValueToSet = "Prototype Tool Launch/Modification";
            } else if ("SerialToolLaunchModification".equals(fromTemplate)) {
                strAttrValueToSet = "Serial Tool Launch/Modification";
            } else if ("DesignStudy".equals(fromTemplate)) {
                strAttrValueToSet = "Design study";
            } else if ("Other".equals(fromTemplate)) {
                strAttrValueToSet = "Other";
            }
            // TIGTK-11675 : 27/11/17 : TS : START
            else if ("Acquisition".equals(fromTemplate)) {
                strAttrValueToSet = "Acquisition";
            }

            // TIGTK-11675 : 27/11/17 : TS : END

            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getAppropriateAttrValue:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getAppropriateAttrValue:ERROR ", ex);
            throw ex;
        }
        return strAttrValueToSet;
    }

    /**
     * This method is used to connect LA
     * @param context
     * @param args
     * @return
     * @author sirale
     * @throws Exception
     */
    public void connectDefaultListOfAssessor(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectDefaultListOfAssessor:START");
        try {
            String strProjectId = args[0];
            DomainObject domProjObj = DomainObject.newInstance(context, strProjectId);
            String strOrganization = DomainConstants.EMPTY_STRING;

            StringList slSelectList = new StringList();
            slSelectList.addElement(DomainConstants.SELECT_ORGANIZATION);

            Map<?, ?> tempmap = domProjObj.getInfo(context, slSelectList);

            if (!tempmap.isEmpty()) {
                strOrganization = (String) tempmap.get(DomainConstants.SELECT_ORGANIZATION);
            }
            // Connect Default True Route Template
            connectDefaultRouteTemplate(context, strOrganization, domProjObj);

            String sWhere = DomainConstants.SELECT_ORGANIZATION + " ==\"" + strOrganization + "\"&&attribute[" + TigerConstants.ATTRIBUTE_PSS_LADEFAULTVALUE + "]==true";

            StringList objectSelect = new StringList();
            objectSelect.add(DomainObject.SELECT_ID);
            objectSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");

            MapList mlListOfAssessors = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_LISTOFASSESSORS, TigerConstants.VAULT_ESERVICEPRODUCTION, sWhere, objectSelect);

            Iterator<?> itrLA = mlListOfAssessors.iterator();
            while (itrLA.hasNext()) {
                Map<?, ?> mLA = (Map<?, ?>) itrLA.next();
                String strLAObjID = (String) mLA.get(DomainConstants.SELECT_ID);
                DomainObject domLA = DomainObject.newInstance(context, strLAObjID);
                String strLAPurposeOfRelease = (String) mLA.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
                String strLAstate = domLA.getInfo(context, DomainConstants.SELECT_CURRENT);
                if (TigerConstants.STATE_LA_ACTIVE.equals(strLAstate)) {
                    DomainRelationship domRelPSS_ListOfAssessors = DomainRelationship.connect(context, domProjObj, TigerConstants.RELATIONSHIP_PSS_CONNECTEDASSESSORS, domLA);
                    domRelPSS_ListOfAssessors.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE, strLAPurposeOfRelease);
                }
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectDefaultListOfAssessor:END");
        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:connectDefaultListOfAssessor:ERROR ", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:connectDefaultListOfAssessor:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * This method is used to check At least one Role is connected to LA when pramoting
     * @param context
     * @param args
     * @return int
     * @author sirale
     * @throws Exception
     */
    public int checkAtleastOneRoleIsConnected(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkAtleastOneRoleIsConnected:START");
        int isConnected = 0;
        try {
            String strObject = args[0];
            DomainObject domLA = DomainObject.newInstance(context, strObject);

            DomainObject.MULTI_VALUE_LIST.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");
            StringList slLARoleValues = domLA.getInfoList(context, "attribute[" + TigerConstants.ATTRIBUTE_PSS_LISTOFASSESSORSELECTEDROLES + "].value");

            while (slLARoleValues.contains(DomainConstants.EMPTY_STRING)) {
                slLARoleValues.remove(DomainConstants.EMPTY_STRING);
            }
            if (slLARoleValues.isEmpty()) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Alert.checkAtleastOneRoleIsConnected");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                isConnected = 1;
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkAtleastOneRoleIsConnected:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:checkAtleastOneRoleIsConnected:ERROR ", ex);
            throw ex;
        }
        return isConnected;
    }

    /**
     * This method is used to check LA not conneted to PP while demoting
     * @param context
     * @param args
     * @return int
     * @author sirale
     * @throws Exception
     */
    public int checkLANotConnectedToBeyondActiveProgramProject(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkLANotConnectedToBeyondActiveProgramProject:START");
        int isConnected = 0;
        try {
            String strObject = args[0];
            DomainObject domLA = DomainObject.newInstance(context, strObject);

            StringBuffer sbWhere = new StringBuffer();

            StringList slbusSelects = new StringList();
            slbusSelects.add(DomainConstants.SELECT_ID);
            slbusSelects.add(DomainConstants.SELECT_CURRENT);

            sbWhere.append("(" + DomainConstants.SELECT_CURRENT + " != " + TigerConstants.STATE_ACTIVE + ") || (" + DomainConstants.SELECT_CURRENT + " != " + TigerConstants.STATE_OBSOLETE + ") || ("
                    + DomainConstants.SELECT_CURRENT + " != \"" + TigerConstants.STATE_NONAWARDED + "\")");

            MapList mlListOfAssessor = domLA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDASSESSORS, // relationship pattern
                    TigerConstants.TYPE_PSS_PROGRAMPROJECT, // object pattern
                    slbusSelects, // object selects
                    null, // relationship selects
                    true, // to direction
                    false, // from direction;
                    (short) 1, // recursion level
                    sbWhere.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null, null);

            if (mlListOfAssessor.size() > 0) {
                String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Alert.checkLANotConnectedToBeyondActiveProgramProject");
                MqlUtil.mqlCommand(context, "notice $1", strMessage);
                isConnected = 1;
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkLANotConnectedToBeyondActiveProgramProject:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:checkLANotConnectedToBeyondActiveProgramProject:ERROR ", ex);
            throw ex;
        }
        return isConnected;
    }

    /**
     * This method is used to check FOR LA
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkForListOfAssessors(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkForListOfAssessors:START");
        int intResult = 0;
        try {
            String strProgramProjectObjId = args[0]; // Program-Project Object Id
            String strChangeObjId = args[1]; // Change Object Id
            String strChangeObjType = args[2]; // Change Object Type

            if (!TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strChangeObjType))
                return intResult;

            DomainObject domChangeObject = DomainObject.newInstance(context, strChangeObjId);
            DomainObject domProgProj = DomainObject.newInstance(context, strProgramProjectObjId);
            String strProgramProjectName = domProgProj.getInfo(context, DomainConstants.SELECT_NAME);

            String strPurposeOfRelease = domChangeObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE);

            // Define Object Select
            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);
            // Define Relationship Select
            StringList relSelects = new StringList();
            relSelects.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");

            String relWhereClause = "attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]== \"" + strPurposeOfRelease + "\"";

            MapList mlListOfAssessor = domProgProj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDASSESSORS, // relationship pattern
                    TigerConstants.TYPE_PSS_LISTOFASSESSORS, // object pattern
                    new StringList(DomainObject.SELECT_ID), // object selects
                    relSelects, // relationship selects
                    false, // to direction
                    true, // from direction;
                    (short) 1, // recursion level
                    null, // object where clause
                    relWhereClause, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, null, null, null, null);

            if (mlListOfAssessor.isEmpty()) {
                intResult = 1;
                String strAlertMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(),
                        "PSS_EnterpriseChangeMgt.Alert.ListOfAssessorsNotFoundForPurposeOfRelease");
                // TIGTK-10177 : POP UP MESSAGE UPDATED : START
                strAlertMessage = strAlertMessage.replace("{ProgramProjectName}", strProgramProjectName);
                strAlertMessage = strAlertMessage.replace("{PurposeOfRelease}", strPurposeOfRelease);
                // TIGTK-10177 : POP UP MESSAGE UPDATED : END
                MqlUtil.mqlCommand(context, "notice $1", strAlertMessage);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkForListOfAssessors:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:checkForListOfAssessors:ERROR ", ex);
            throw ex;
        }
        return intResult;
    }

    /**
     * This method is used to Check default LA
     * @param context
     * @param args
     * @return
     * @author sirale
     * @throws Exception
     */
    public void checkDefaultLA(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkDefaultLA:START");
        String TRUE = "true";
        String strPurposeOfRelease = DomainConstants.EMPTY_STRING;
        String strOrganization = DomainConstants.EMPTY_STRING;
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            Map<?, ?> paramMap = (Map<?, ?>) programMap.get("paramMap");
            String strObject = (String) paramMap.get("objectId");
            String strDefaultLA = (String) paramMap.get("New Value");

            DomainObject domObject = DomainObject.newInstance(context, strObject);

            StringList slSelectList = new StringList();
            slSelectList.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            slSelectList.addElement(DomainConstants.SELECT_ORGANIZATION);

            Map<?, ?> tempmap = domObject.getInfo(context, slSelectList);

            if (!tempmap.isEmpty()) {
                strPurposeOfRelease = (String) tempmap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
                strOrganization = (String) tempmap.get(DomainConstants.SELECT_ORGANIZATION);
            }
            Map<String, String> argsMap = new HashMap<String, String>();
            argsMap.put("strPurposeOfReleaseVal", strPurposeOfRelease);
            argsMap.put("strOrganization", strOrganization);
            String[] strargs = JPO.packArgs(argsMap);

            if (TRUE.equalsIgnoreCase(strDefaultLA)) {
                String strMessage = checkModificationsForDefaultLAValue(context, strargs);
                if (UIUtil.isNotNullAndNotEmpty(strMessage)) {
                    throw new Exception("\n" + strMessage);
                }
            }
            domObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_LADEFAULTVALUE, strDefaultLA);

            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkDefaultLA:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:checkDefaultLA:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * This method is Edit access function on LA Table for edit only active LA
     * @param context
     * @param args
     * @return StringList
     * @author sirale
     * @throws Exception
     */
    public static StringList isLAStateInactive(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:isLAStateInactive:START");
        StringList lRet = new StringList();
        String strCurrent = DomainConstants.EMPTY_STRING;
        try {
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList mlRelBusObjPageList = (MapList) programMap.get("objectList");
            for (int i = 0; i < mlRelBusObjPageList.size(); i++) {
                Map<?, ?> mpObjectData = (Map<?, ?>) mlRelBusObjPageList.get(i);
                String strId = (String) mpObjectData.get("id");
                DomainObject domLA = DomainObject.newInstance(context, strId);

                StringList slSelectList = new StringList();
                slSelectList.addElement(DomainConstants.SELECT_CURRENT);

                Map<?, ?> tempmap = domLA.getInfo(context, slSelectList);

                if (!tempmap.isEmpty()) {
                    strCurrent = (String) tempmap.get(DomainConstants.SELECT_CURRENT);
                }

                if (TigerConstants.STATE_LA_INACTIVE.equalsIgnoreCase(strCurrent)) {
                    lRet.addElement("true");
                } else {
                    lRet.addElement("false");
                }
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:isLAStateInactive:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:isLAStateInactive:ERROR ", ex);
            throw ex;
        }
        return lRet;
    }

    /***
     * This method is used to show Role Assessment objects related to IA.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public MapList getConnectedRoleAssessment(Context context, String[] args) throws Exception {
        logger.debug("ImpactAnalysisUtil_mxJPO:getConnectedRoleAssessment:START ");
        try {
            HashMap<?, ?> paramMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strObjId = (String) paramMap.get("objectId");

            StringList slObjectSle = new StringList();
            slObjectSle.addElement(DomainConstants.SELECT_ID);
            slObjectSle.addElement(DomainConstants.SELECT_NAME);

            StringList slRelSle = new StringList();
            slRelSle.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            DomainObject domIAObj = DomainObject.newInstance(context, strObjId);
            MapList mlRoleAssement = new MapList();
            // TIGTK-13641 : Vishal T :START
            if (domIAObj.isKindOf(context, TigerConstants.TYPE_PSS_ROLEASSESSMENT)) {
                String strIAId = domIAObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.id");
                DomainObject domCRIAObj = DomainObject.newInstance(context, strIAId);
                MapList mlRoleAssement1 = domCRIAObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slObjectSle, slRelSle, false,
                        true, (short) 1, null, null, 0);
                int size = mlRoleAssement1.size();
                for (int i = 0; i < size; i++) {
                    Map mp = (Map) mlRoleAssement1.get(i);
                    if (mp.containsValue(strObjId)) {
                        mlRoleAssement.add(mlRoleAssement1.get(i));
                    }
                }

            }

            else if (domIAObj.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                String strIAId = domIAObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].to.last.id");
                if (UIUtil.isNotNullAndNotEmpty(strIAId)) {
                    DomainObject domCRIAObj = DomainObject.newInstance(context, strIAId);
                    mlRoleAssement = domCRIAObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slObjectSle, slRelSle, false, true,
                            (short) 1, null, null, 0);

                }
            } else {

                mlRoleAssement = domIAObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slObjectSle, slRelSle, false, true,
                        (short) 1, null, null, 0);
            }

            // TIGTK-13641 : Vishal T :END
            logger.debug("ImpactAnalysisUtil_mxJPO:getConnectedRoleAssessment:END ");
            return mlRoleAssement;
        } catch (Exception ex) {
            logger.error("error in ImpactAnalysisUtil_mxJPO:getConnectedRoleAssessment: " + ex.getMessage());
            throw ex;
        }
    }

    /***
     * This method is used to populate data in Summary view table.
     * @param context
     * @param args
     * @return MapList
     * @throws Exception
     */
    public MapList getSummaryTableData(Context context, String args[]) throws Exception {
        logger.debug("ImpactAnalysisUtil_mxJPO : getSummaryTableData :START ");
        Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
        String strSummaryView = (String) programMap.get("summaryView");
        String strIAObjID = (String) programMap.get("objectId");
        String strRelWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_VIEW + "]==" + strSummaryView;
        MapList mlReturn = getSummaryDisplayAttributes(context, strIAObjID, strRelWhere, null);
        logger.debug("ImpactAnalysisUtil_mxJPO : getSummaryTableData :END ");
        return mlReturn;
    }

    /***
     * This method is called from JPO method pss.ecm.impactanalysis.ImpactAnalysisUtil : getSummaryTableData which is used to populate data in Summary view table.
     * @param context
     * @param strIAObjID
     * @param strRelWhere
     * @param strObjWhere
     * @return MapList
     * @throws FrameworkException
     */
    public static MapList getSummaryDisplayAttributes(Context context, String strIAObjID, String strRelWhere, String strObjWhere) throws FrameworkException {
        logger.debug("ImpactAnalysisUtil_mxJPO : getSummaryDisplayAttributes :START ");
        MapList mlReturn = new MapList();
        try {
            StringList slRAobjSelect = new StringList();
            slRAobjSelect.add(DomainConstants.SELECT_ID);
            StringList slRAEobjSelect = new StringList();
            slRAEobjSelect.add(DomainConstants.SELECT_ID);
            slRAEobjSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]");
            slRAEobjSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]");
            DomainObject domIA = DomainObject.newInstance(context, strIAObjID);
            MapList mplRAObjs = domIA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slRAobjSelect, null, false, true, (short) 1,
                    null, null, 0);

            for (Object objRA : mplRAObjs) {
                Map<?, ?> mpRA = (Map<?, ?>) objRA;
                String strRAID = (String) mpRA.get(DomainConstants.SELECT_ID);
                DomainObject domRA = DomainObject.newInstance(context, strRAID);
                MapList mplRAEvalObjs = domRA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION, TigerConstants.TYPE_PSS_ROLEASSESSMENT_EVALUATION, slRAEobjSelect,
                        null, false, true, (short) 1, strObjWhere, strRelWhere, 0);
                for (Object objRAEval : mplRAEvalObjs) {
                    Map<String, String> mpRAE = (Map<String, String>) objRAEval;
                    String strTitleTechValue = mpRAE.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]");
                    String strDomainTechValue = mpRAE.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]");

                    String strTitleKey = "PSS_EnterpriseChangeMgt.Label.Title." + strTitleTechValue;
                    String strDomainKey = "PSS_EnterpriseChangeMgt.Label.Domain." + strDomainTechValue;
                    String strTitleDisplayValue = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strTitleKey);
                    String strDomainDisplayValue = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), strDomainKey);
                    mpRAE.put("attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]", strTitleDisplayValue);
                    mpRAE.put("attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]", strDomainDisplayValue);
                    if (!mlReturn.contains(mpRAE)) {
                        mlReturn.add(mpRAE);
                    }
                }
            }
        } catch (RuntimeException ex) {
            logger.error("ERROR in ImpactAnalysisUtil_mxJPO : getSummaryDisplayAttributes :  " + ex.getMessage());
            throw ex;
        }
        logger.debug("ImpactAnalysisUtil_mxJPO : getSummaryDisplayAttributes :END ");
        return mlReturn;
    }

    /***
     * @author RutujaE
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getDecisionColumnCellEditAccess(Context context, String[] args) throws Exception {
        logger.debug("ImpactAnalysisUtil_mxJPO : getDecisionColumnCellEditAccess  :Start");
        boolean isOwner = false;
        StringList slAccess = getRADecisionOrOwnerAccess(context, args, isOwner);
        logger.debug("ImpactAnalysisUtil_mxJPO : getDecisionColumnCellEditAccess :End ");
        return slAccess;
    }

    /***
     * @author RutujaE
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getOwnerColumnCellEditAccess(Context context, String[] args) throws Exception {
        logger.debug("ImpactAnalysisUtil_mxJPO : getOwnerColumnCellEditAccess  :Start");

        boolean isOwner = true;
        StringList slAccess = getRADecisionOrOwnerAccess(context, args, isOwner);
        logger.debug("ImpactAnalysisUtil_mxJPO : getOwnerColumnCellEditAccess  :End");
        return slAccess;
    }

    /***
     * @author RutujaE
     * @param context
     * @param args
     * @param isOwnerEditAccess
     * @return
     * @throws Exception
     */
    public StringList getRADecisionOrOwnerAccess(Context context, String[] args, Boolean isOwnerEditAccess) throws Exception {
        logger.debug("ImpactAnalysisUtil_mxJPO : getRADecisionOrOwnerAccess  :Start");
        StringList slAccess = new StringList();
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            Map<?, ?> requestMap = (Map<?, ?>) programMap.get("requestMap");
            String strIAID = (String) requestMap.get("objectId");
            MapList mlRAs = (MapList) programMap.get("objectList");
            DomainObject domIA = DomainObject.newInstance(context, strIAID);
            // TIGTK-13641 : Vishal T :START
            String strCRId;

            if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_ROLEASSESSMENT)) {
                String strIAId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.id");
                DomainObject domCRIA = DomainObject.newInstance(context, strIAId);
                strCRId = domCRIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
            } else {

                if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                    strCRId = strIAID;
                    String strIAId = domIA.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].to.last.id");
                    DomainObject domCRIAObj = DomainObject.newInstance(context, strIAId);

                } else {
                    strCRId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
                }
            }
            // TIGTK-13641 : Vishal T :END
            DomainObject domCR = DomainObject.newInstance(context, strCRId);
            String strCRCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);
            String strContextUser = context.getUser();
            StringList slRolesList = new StringList();
            // TIGTK-12983 : JV : START
            pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
            if(jvUtil.isJVProgramProject(context, strCRId)) {                
                slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
                slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR_JV);                
            } else {
            slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
            slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
            }
            // TIGTK-12983 : JV : END
            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
            StringList slCMPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, false);
            for (Object objRA : mlRAs) {
                boolean isEditAccess = false;
                Map<String, String> mpRA = (Map<String, String>) objRA;

                String sRAId = (String) mpRA.get(DomainConstants.SELECT_ID);
                DomainObject domObjRA = DomainObject.newInstance(context, sRAId);
                String strRAOwner = domObjRA.getInfo(context, DomainConstants.SELECT_OWNER);

                // TIGTK-13641 : Vishal T :START
                if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_ROLEASSESSMENT)) {
                    if (TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent)) {
                        if ((TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent)) && isOwnerEditAccess
                                && (strRAOwner.equalsIgnoreCase(strContextUser) || slCMPMList.contains(strContextUser))) {
                            isEditAccess = false;
                        } else {
                            isEditAccess = true;
                        }
                    }
                }
                if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_IMPACTANALYSIS)) {
                    if (TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent)) {
                        if ((TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent)) && isOwnerEditAccess
                                && (strRAOwner.equalsIgnoreCase(strContextUser) || slCMPMList.contains(strContextUser))) {
                            isEditAccess = true;
                        } else {
                            isEditAccess = false;
                        }
                    }
                }
                if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_IMPACTANALYSIS)) {
                    if (TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent)) {
                        if ((TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent)) && isOwnerEditAccess
                                && (strRAOwner.equalsIgnoreCase(strContextUser) || slCMPMList.contains(strContextUser))) {
                            isEditAccess = true;

                        } else {
                            isEditAccess = false;
                        }
                    }
                }
                if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                    // TIGTK-12011 :START
                    if (TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent)) {
                        if ((TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent)) && isOwnerEditAccess
                                && (strRAOwner.equalsIgnoreCase(strContextUser) || slCMPMList.contains(strContextUser))) {
                            isEditAccess = true;
                        } else {
                            isEditAccess = false;
                        }
                        // TIGTK-12011 :END
                    }
                }
                if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                    // TIGTK-12011 :START
                    if (TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent)) {
                        if ((TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent)) && isOwnerEditAccess
                                && (strRAOwner.equalsIgnoreCase(strContextUser) || slCMPMList.contains(strContextUser))) {
                            isEditAccess = true;
                        } else {
                            isEditAccess = false;
                        }
                    }
                }
                if (isEditAccess) {
                    slAccess.add("true");
                } else {
                    slAccess.add("false");
                }
            }
        } catch (Exception e) {
            logger.error("ERROR in ImpactAnalysisUtil_mxJPO : getRADecisionOrOwnerAccess  :" + e.getMessage());
            throw e;
        }
        logger.debug("ImpactAnalysisUtil_mxJPO : getRADecisionOrOwnerAccess  :End");
        return slAccess;
    }

    /***
     * @author RutujaE
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getEligibleMemberForRAOwner(Context context, String[] args) throws Exception {
        logger.debug("ImpactAnalysisUtil_mxJPO : getEligibleMemberForRAOwner  :Start");
        StringList slOwnerMemberList = new StringList();
        StringList slMemberList = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("rootObjectId");
            String strRAobjectId = (String) programMap.get("rowObjectId");
            // TIGTK-11441- Changes Made for typeahead : 22-11-2017 : START
            // TIGTK-13641 : Vishal T :START
            if (UIUtil.isNullOrEmpty(objectId)) {
                HashMap requestMap = (HashMap) programMap.get("requestMap");
                objectId = (String) requestMap.get("objectId");

                HashMap typeAheadMap = (HashMap) programMap.get("typeAheadMap");
                strRAobjectId = (String) typeAheadMap.get("rowObjectId");
            }
            // TIGTK-13641 : Vishal T :END
            // TIGTK-11441- Changes Made for typeahead : 22-11-2017 : END
            DomainObject domIA = DomainObject.newInstance(context, objectId);
            DomainObject domRA = DomainObject.newInstance(context, strRAobjectId);
            String sRAOwner = domRA.getInfo(context, DomainConstants.SELECT_OWNER);
            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
            // TIGTK-13641 : Vishal T :START
            String strCRId;
            if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_ROLEASSESSMENT)) {
                String strIAId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.id");
                DomainObject domCRIA = DomainObject.newInstance(context, strIAId);
                strCRId = domCRIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
            }
            if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                strCRId = objectId;
            } else {
                strCRId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
            }
            // TIGTK-13641 : Vishal T :END
            DomainObject domCR = DomainObject.newInstance(context, strCRId);
            String strCRCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);

            // TIGTK-12011 :START
            String strRARole = domRA.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_RAROLE);
            StringList slRolesList = new StringList();
            slRolesList.addElement(strRARole);

            // TIGTK-12983 : JV : START
            pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
            boolean isJV = jvUtil.isJVProgramProject(context, strCRId);
            // TIGTK-12983 : JV : END
            
            if ((TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent))) {
                // TIGTK-12983 : JV : START
                if(isJV) {
                    slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
                } else {
                slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                }
                // TIGTK-12983 : JV : END
                slMemberList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, false);
            } else if ((TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent))) {
                StringList slCMPMRole = new StringList();
                // TIGTK-12983 : JV : START
                if(isJV) {
                    slCMPMRole.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR_JV);
                    slCMPMRole.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
                } else {
                slCMPMRole.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                slCMPMRole.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                }
                // TIGTK-12983 : JV : END
                StringList slCMPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slCMPMRole, false);
                String strContextUser = context.getUser();

                if (slCMPMList.contains(strContextUser)) {
                    // TIGTK-12983 : JV : START
                    if(isJV) {
                        slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
                    } else {
                    slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                }
                    // TIGTK-12983 : JV : END                    
                }
                slMemberList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, false);
                slMemberList.remove(sRAOwner);
            }
            // TIGTK-12011 :END
            for (Object obj : slMemberList) {
                String strPersonName = (String) obj;
                slOwnerMemberList.add(PersonUtil.getPersonObjectID(context, strPersonName));
            }
            if (slOwnerMemberList.isEmpty()) {
                slOwnerMemberList.add(" ");
            }
        } catch (Exception e) {
            logger.error("ERROR in ImpactAnalysisUtil_mxJPO : getEligibleMemberForRAOwner  :" + e.getMessage());
            throw e;
        }
        logger.debug("ImpactAnalysisUtil_mxJPO : getEligibleMemberForRAOwner  :End");
        return slOwnerMemberList;
    }

    /****
     * @author RutujaE
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public Vector<String> getDisplayValueForTitle(Context context, String[] args) throws Exception {
        logger.debug("ImpactAnalysisUtil_mxJPO : getDisplayValueForTitle :Start ");
        String SELECT_ATTRIBUTE_PSS_TITLE = "attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]";
        Vector<String> vctrValue = getDisplayValue(context, args, SELECT_ATTRIBUTE_PSS_TITLE);
        logger.debug("ImpactAnalysisUtil_mxJPO : getDisplayValueForTitle :End ");
        return vctrValue;
    }

    /***
     * @author RutujaE
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public Vector<String> getDisplayValueForDomain(Context context, String[] args) throws Exception {
        logger.debug("ImpactAnalysisUtil_mxJPO : getDisplayValueForDomain :Start ");
        String SELECT_ATTRIBUTE_PSS_DOMAIN = "attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]";
        Vector<String> vctrValue = getDisplayValue(context, args, SELECT_ATTRIBUTE_PSS_DOMAIN);
        logger.debug("ImpactAnalysisUtil_mxJPO : getDisplayValueForDomain :End ");
        return vctrValue;
    }

    /***
     * @author RutujaE
     * @param context
     * @param args
     * @param sSelectAttribute
     * @return StringList
     * @throws Exception
     */
    public Vector<String> getDisplayValue(Context context, String[] args, String sSelectAttribute) throws Exception {
        logger.debug("ImpactAnalysisUtil_mxJPO : getDisplayValue :Start ");
        Vector<String> vctrReturn = new Vector<String>();
        Map<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        MapList objectList = (MapList) programMap.get("objectList");
        Iterator<Map<?, ?>> itrObjList = objectList.iterator();
        while (itrObjList.hasNext()) {
            Map<?, ?> tempMap = itrObjList.next();
            String sValue = (String) tempMap.get(sSelectAttribute);
            vctrReturn.add(sValue);
        }
        logger.debug("ImpactAnalysisUtil_mxJPO : getDisplayValue :End ");
        return vctrReturn;
    }

    // PCM2.0 Spr4:TIGTK-9021:1/9/2017:START
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public int checkImpactAnalysisIsRejected(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:checkImpactAnalysisIsRejected:START");
        int retValue = 0;
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }
            String strCRID = args[0];
            DomainObject domCR = DomainObject.newInstance(context, strCRID);
            String strCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);
            StringList slObjectSelects = new StringList();
            slObjectSelects.addElement(DomainConstants.SELECT_ID);
            String strwhere = "revision == last";
            String strRelWhere = "";
            String strMessage = "";

            MapList mlIA = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjectSelects, DomainConstants.EMPTY_STRINGLIST,
                    false, true, (short) 1, strwhere, null, 0);
            if (!mlIA.isEmpty()) {
                Map<?, ?> mapIA = (Map<?, ?>) mlIA.get(0);
                String strIAId = (String) mapIA.get(DomainConstants.SELECT_ID);
                DomainObject domIA = DomainObject.newInstance(context, strIAId);
                if (TigerConstants.STATE_EVALUATE.equals(strCurrent)) {
                    strRelWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_DECISION + "] =='" + TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_NODECISION + "'";
                    strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.IAWithNODECISION");

                } else {
                    strRelWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_DECISION + "] =='" + TigerConstants.ATTRIBUTE_PSS_DECISION_RANGE_NOGO + "' ";
                    strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "PSS_EnterpriseChangeMgt.Alert.IAWithNOGO");
                }
                MapList mlRA = domIA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slObjectSelects, null, false, true, (short) 0,
                        strwhere, strRelWhere, 0);
                if (!mlRA.isEmpty()) {
                    MqlUtil.mqlCommand(context, "notice $1", strMessage);
                    retValue = 1;
                }
            }
        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:checkImpactAnalysisIsRejected:ERROR ", ex);
            throw ex;
        }
        return retValue;
    }

    /**
     * @param context
     * @param args
     * @throws Exception
     */
    public void completeRoleAssessment(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:completeRoleAssessment:START");
        try {
            if (args == null || args.length < 1) {
                throw (new IllegalArgumentException());
            }

            String strCRID = args[0];
            DomainObject domCR = DomainObject.newInstance(context, strCRID);

            StringList slObjectSelects = new StringList();
            slObjectSelects.addElement(DomainConstants.SELECT_ID);

            String strwhere = "revision == last";

            MapList mlIA = domCR.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjectSelects, DomainConstants.EMPTY_STRINGLIST,
                    false, true, (short) 1, strwhere, null, 0);

            if (!mlIA.isEmpty()) {
                Map<?, ?> mapIA = (Map<?, ?>) mlIA.get(0);
                String strIAId = (String) mapIA.get(DomainConstants.SELECT_ID);
                DomainObject domIA = DomainObject.newInstance(context, strIAId);

                MapList mlRA = domIA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slObjectSelects, null, false, true, (short) 0,
                        strwhere, null, 0);

                Iterator<?> itrRA = mlRA.iterator();
                while (itrRA.hasNext()) {
                    Map<?, ?> mRA = (Map<?, ?>) itrRA.next();
                    String strRAID = (String) mRA.get(DomainConstants.SELECT_ID);
                    DomainObject domRA = DomainObject.newInstance(context, strRAID);

                    domRA.setState(context, TigerConstants.STATE_ROLEASSESSMENT_COMPLETE);
                }
            }
        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:completeRoleAssessment:ERROR ", ex);
            throw ex;
        }
    }

    // PCM2.0 Spr4:TIGTK-9021:1/9/2017:END
    // PCM2.0 Spr4:TIGTK-7587:6/9/2017:START
    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean deleteRA(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:disconnectRA:START");
        boolean bShowLastRANotice = true;
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap mapObjIdRelId = (HashMap) programMap.get("mapObjIdRelId");
            StringList slObjID = (StringList) mapObjIdRelId.get("ObjId");

            String strIAObjectId = (String) programMap.get("objectId");

            DomainObject domIA = DomainObject.newInstance(context, strIAObjectId);
            // TIGTK-13641 : Vishal T :START
            if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                strIAObjectId = domIA.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].to.last.id");
                domIA = DomainObject.newInstance(context, strIAObjectId);
            }
            // TIGTK-13641 : Vishal T :END
            StringList slRAID = new StringList();
            StringList slObjectSelects = new StringList();
            slObjectSelects.addElement(DomainConstants.SELECT_ID);
            MapList mplRAObjs = domIA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slObjectSelects, null, false, true, (short) 1,
                    null, null, 0);

            Iterator itrRA = mplRAObjs.iterator();
            while (itrRA.hasNext()) {
                Map mRA = (Map) itrRA.next();
                String strRAID = (String) mRA.get(DomainConstants.SELECT_ID);
                slRAID.add(strRAID);
            }

            int iConnectedRA = slRAID.size();
            int iSelectedRA = slObjID.size();
            int iRemainingRA = iConnectedRA - iSelectedRA;
            if (iRemainingRA >= 1) {
                deleteRAEObjects(context, slObjID);

                DomainObject.deleteObjects(context, (String[]) slObjID.toArray(new String[slObjID.size()]));
                bShowLastRANotice = false;
            }

        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:disconnectRA:ERROR ", ex);
            throw ex;
        }
        return bShowLastRANotice;
    }

    /**
     * @param context
     * @param slRAID
     * @throws Exception
     */
    public void deleteRAEObjects(Context context, StringList slRAID) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:deleteRAEObjects:START");
        try {
            StringList slRAEIDs = new StringList();
            Iterator itrRA = slRAID.iterator();

            while (itrRA.hasNext()) {
                String strRAId = (String) itrRA.next();
                DomainObject domRA = DomainObject.newInstance(context, strRAId);

                StringList slObjectSelects = new StringList();
                slObjectSelects.addElement(DomainConstants.SELECT_ID);

                MapList mlRAE = domRA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION, TigerConstants.TYPE_PSS_ROLEASSESSMENT_EVALUATION, slObjectSelects,
                        DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, null, null, 0);

                Iterator itrRAE = mlRAE.iterator();
                while (itrRAE.hasNext()) {
                    Map mRAE = (Map) itrRAE.next();
                    String strRAEID = (String) mRAE.get(DomainConstants.SELECT_ID);
                    slRAEIDs.add(strRAEID);

                }

                DomainObject.deleteObjects(context, (String[]) slRAEIDs.toArray(new String[slRAEIDs.size()]));
            }
        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:deleteRAEObjects:ERROR ", ex);
            throw ex;
        }
    }

    public boolean showRACreateAndRemoveActionCommands(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:showRACreateAndRemoveActionCommands:START");

        boolean boolReturn = false;
        try {

            HashMap<?, ?> param = (HashMap<?, ?>) JPO.unpackArgs(args);
            String strObjectId = (String) param.get("objectId");
            DomainObject domIA = DomainObject.newInstance(context, strObjectId);
            // TIGTK-13641 : Vishal T :START
            String strCRId;
            // StringList slRAOwner;
            // String strIACurrent;

            if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_ROLEASSESSMENT)) {
                String strIAId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.id");
                DomainObject domCRIA = DomainObject.newInstance(context, strIAId);
                // slRAOwner = domCRIA.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].to.owner");
                // strIACurrent = domCRIA.getInfo(context, DomainConstants.SELECT_CURRENT);
                strCRId = domCRIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
                // TIGTK-12983 : JV : START
                pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
                boolean isJV = jvUtil.isJVProgramProject(context, strCRId);
                // TIGTK-12983 : JV : END
                DomainObject domCR = DomainObject.newInstance(context, strCRId);
                String strCRCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);
                String strContextUser = context.getUser();
                StringList slRolesList = new StringList();
                // TIGTK-12983 : JV : START
                if(isJV) {
                    slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
                    slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR_JV);                    
                } else {
                slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                }
                // TIGTK-12983 : JV : END

                pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                StringList slCMPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, false);
                if (slCMPMList.contains(strContextUser) && TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent)) {
                    boolReturn = false;
                }
            } else if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                strCRId = strObjectId;
                // DomainObject domCRID = DomainObject.newInstance(context, strObjectId);
                // strIACurrent = domCRID.getInfo(context, DomainConstants.SELECT_CURRENT);
                String strIAId = domIA.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].to.last.id");
                if (UIUtil.isNotNullAndNotEmpty(strCRId) && (UIUtil.isNotNullAndNotEmpty(strIAId))) {
                    // DomainObject domCRIA = DomainObject.newInstance(context, strIAId);
                    // slRAOwner = domCRIA.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].to.owner");
                    // strIACurrent = domCRIA.getInfo(context, DomainConstants.SELECT_CURRENT);
                    DomainObject domCR = DomainObject.newInstance(context, strCRId);
                    String strCRCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);
                    String strContextUser = context.getUser();
                    StringList slRolesList = new StringList();
                    // TIGTK-12983 : JV : START
                    pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
                    boolean isJV = jvUtil.isJVProgramProject(context, strCRId);
                    
                    if(isJV) {
                        slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
                        slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR_JV);                    
                    } else {
                    slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                    slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                    }
                    // TIGTK-12983 : JV : END
                    pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                    StringList slCMPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, false);
                    if (slCMPMList.contains(strContextUser) && TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent)) {
                        boolReturn = true;
                    }
                }
            } else {
                strCRId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
                DomainObject domCR = DomainObject.newInstance(context, strCRId);
                String strCRCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);
                String strContextUser = context.getUser();
                StringList slRolesList = new StringList();
                // TIGTK-12983 : JV : START
                pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
                boolean isJV = jvUtil.isJVProgramProject(context, strCRId);
                if(isJV) {
                    slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
                    slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR_JV);                    
                } else {
                slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                }
                // TIGTK-12983 : JV : END                
                pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                StringList slCMPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, false);
                if (slCMPMList.contains(strContextUser) && TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent)) {
                    boolReturn = true;
                }
            }
            // TIGTK-13641 : Vishal T :END
        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:showRACreateAndRemoveActionCommands:ERROR ", ex);
            throw ex;
        }
        return boolReturn;
    }

    /**
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Map<String, StringList> getRoleforRA(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleforRA:START");

        HashMap<String, StringList> rangeMap = new HashMap<String, StringList>();
        StringList fieldChoices = new StringList();
        StringList fieldDisplayChoices = new StringList();
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> requestMap = (Map<?, ?>) programMap.get("requestMap");
            String strIAObjectId = (String) requestMap.get("parentOID");
            // TIGTK-13641 : Vishal T :START
            DomainObject domIA = DomainObject.newInstance(context, strIAObjectId);
            String strCRId=DomainConstants.EMPTY_STRING;
            if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                strCRId = strIAObjectId;
                String strIAId = domIA.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].to.last.id");
                domIA = DomainObject.newInstance(context, strIAId);
            }else {
                strCRId =  domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
            }
            // TIGTK-13641 : Vishal T :END
            if (lsIAAllowedRoles.length == 0) {
                String strMQLCommand = "list role * where property[IS_IMPACT_ANALYSIS_ASSESSMENT_ROLE].value=='true'";
                String strMQLResult = MqlUtil.mqlCommand(context, true, strMQLCommand, true);
                lsIAAllowedRoles = strMQLResult.split("\n");
            }
            List<String> lstString = new ArrayList<>(Arrays.asList(lsIAAllowedRoles));
            StringList slObjectSle = new StringList();
            slObjectSle.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_RAROLE + "]");
            String strwhere = "revision == last";
            MapList mlRoleAssementConnectedToIA = domIA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slObjectSle,
                    DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, strwhere, null, 0);
            Iterator itrRAIA = mlRoleAssementConnectedToIA.iterator();
            
            // TIGTK-12983 : JV : START
            pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
            boolean isJV = jvUtil.isJVProgramProject(context, strCRId);                
            // TIGTK-12983 : JV : END
            
            while (itrRAIA.hasNext()) {
                Map mRA = (Map) itrRAIA.next();
                String strRARole = (String) mRA.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_RAROLE + "]");
                if(isJV) {
                    strRARole = strRARole.replaceAll("_" + TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV,DomainConstants.EMPTY_STRING);
                }
                if (lstString.contains(strRARole)) {
                    lstString.remove(strRARole);
                }
            }
            Iterator itrRoles = lstString.iterator();
            Locale strLocale = new Locale(context.getSession().getLanguage());
            while (itrRoles.hasNext()) {
                String strRoleName = (String) itrRoles.next();
                String strKey = "emxFramework.Role." + strRoleName;
                String strPSSRoleValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", strLocale, strKey);
                if (!strPSSRoleValue.isEmpty()) {
                    fieldChoices.add(strRoleName);
                    fieldDisplayChoices.add(strPSSRoleValue);
                }

            }
            rangeMap.put("field_choices", fieldChoices);
            rangeMap.put("field_display_choices", fieldDisplayChoices);
        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleforRA :ERROR ", ex);
            throw ex;
        }
        return rangeMap;
    }

    public boolean showRAEditActionCommand(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:showRAEditActionCommand:START");
        boolean boolReturn = false;
        try {

            HashMap param = (HashMap) JPO.unpackArgs(args);
            String objectId = (String) param.get("objectId");

            DomainObject domIA = DomainObject.newInstance(context, objectId);

            // TIGTK-11350 : TS : 17/11/2017: Start
            // String strIACurrent = domIA.getInfo(context, DomainConstants.SELECT_CURRENT);
            // TIGTK-11350 : TS : 17/11/2017: End
            // TIGTK-13641 : Vishal T :START
            String strCRId;
            StringList slRAOwner;
            String strIACurrent;
            if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_ROLEASSESSMENT)) {
                String strIAId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.id");
                DomainObject domCRIA = DomainObject.newInstance(context, strIAId);
                slRAOwner = domCRIA.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].to.owner");
                strIACurrent = domCRIA.getInfo(context, DomainConstants.SELECT_CURRENT);
                strCRId = domCRIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
                DomainObject domCR = DomainObject.newInstance(context, strCRId);
                String strCRCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);
                String strContextUser = context.getUser();
                StringList slRolesList = new StringList();
                slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                StringList slCMPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, false);
                // TIGTK-12011 :START
                if ((TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent) || TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent))
                        && (!TigerConstants.STATE_IMPACTANALYSIS_CANCELLED.equalsIgnoreCase(strIACurrent))) {
                    if (slCMPMList.contains(strContextUser) || slRAOwner.contains(strContextUser)) {
                        boolReturn = true;
                    }
                }
            } else if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                strCRId = objectId;
                String strIAId = domIA.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].to.last.id");
                if (UIUtil.isNotNullAndNotEmpty(strIAId)) {
                    DomainObject domCRIA = DomainObject.newInstance(context, strIAId);
                    slRAOwner = domCRIA.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].to.owner");
                    strIACurrent = domCRIA.getInfo(context, DomainConstants.SELECT_CURRENT);
                    DomainObject domCR = DomainObject.newInstance(context, strCRId);
                    String strCRCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);
                    String strContextUser = context.getUser();
                    StringList slRolesList = new StringList();
                    slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                    slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                    pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                    StringList slCMPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, false);
                    // TIGTK-12011 :START
                    if ((TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent) || TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent))
                            && (!TigerConstants.STATE_IMPACTANALYSIS_CANCELLED.equalsIgnoreCase(strIACurrent))) {
                        if (slCMPMList.contains(strContextUser) || slRAOwner.contains(strContextUser)) {
                            boolReturn = true;
                        }
                    }
                }
            } else {
                strCRId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
                slRAOwner = domIA.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].to.owner");
                strIACurrent = domIA.getInfo(context, DomainConstants.SELECT_CURRENT);
                // TIGTK-13641 : Vishal T :END
                DomainObject domCR = DomainObject.newInstance(context, strCRId);
                String strCRCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);
                String strContextUser = context.getUser();
                StringList slRolesList = new StringList();
                // TIGTK-12983 : JV : START
                pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
                if(jvUtil.isJVProgramProject(context, strCRId)) {                
                    slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
                    slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR_JV);                
                } else {
                slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                }
                // TIGTK-12983 : JV : END
                pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
                StringList slCMPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, false);

                // TIGTK-12011 :START
                if ((TigerConstants.STATE_SUBMIT_CR.equalsIgnoreCase(strCRCurrent) || TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent))
                        && (!TigerConstants.STATE_IMPACTANALYSIS_CANCELLED.equalsIgnoreCase(strIACurrent))) {

                    if (slCMPMList.contains(strContextUser) || slRAOwner.contains(strContextUser)) {
                        boolReturn = true;
                    }
                }
            }
            // TIGTK-12011 :END

            // TIGTK-11350 : TS : 17/11/2017: End

        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:showRAEditActionCommand :ERROR ", ex);
            throw ex;
        }

        return boolReturn;
    }

    public StringList getProgramProjectMembers(Context context, String strCRId) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:showRAEditActionCommand:START");

        try {
            DomainObject domCR = DomainObject.newInstance(context, strCRId);

            StringList slProgramProjectMembers = domCR.getInfoList(context,
                    "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.from[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS + "].to.name");

            return slProgramProjectMembers;

        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:showRAEditActionCommand :ERROR ", ex);
            throw ex;
        }

    }

    public void connectRAToIAAndCreateRAE(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectRAToIAAndCreateRAE:START");
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> requestMap = (Map<?, ?>) programMap.get("requestMap");
            String strIAId = (String) requestMap.get("objectId");
            DomainObject domIA = DomainObject.newInstance(context, strIAId);
            // TIGTK-13641 : Vishal T :START
            if (domIA.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST)) {

                strIAId = domIA.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].to.last.id");
                domIA = DomainObject.newInstance(context, strIAId);
            }
            // TIGTK-13641 : Vishal T :START
            String strCRId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
            String strRoleValue = (String) requestMap.get("PSS_CreateRA");
            // TIGTK-12983 : JV : START
            pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
            if(jvUtil.isJVProgramProject(context, strCRId)) {                
                strRoleValue =strRoleValue+"_"+TigerConstants.ATTRIBUTE_PSS_USERTYPE_RANGE_JV;             
            }
            // TIGTK-12983 : JV : END
            Map<?, ?> paramMap = (Map<?, ?>) programMap.get("paramMap");
            String strRAId = (String) paramMap.get("objectId");
            DomainObject domRA = DomainObject.newInstance(context, strRAId);

            Map<String, String> relAtt = new HashMap<String, String>();

            domRA.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_RAROLE, strRoleValue);
            String strRAOwner = getRAOwner(context, strRoleValue, strCRId);

            DomainRelationship domRelRA = DomainRelationship.connect(context, domIA, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, domRA);

            String sRADueDate = getRADueDate(context, strCRId);
            relAtt.put(TigerConstants.ATTRIBUTE_PSS_RAROLE, strRoleValue);

            if (!sRADueDate.isEmpty()) {
                relAtt.put(TigerConstants.ATTRIBUTE_PSS_DUEDATE, sRADueDate);

            }
            domRelRA.setAttributeValues(context, relAtt);

            Map<String, DomainObject> mpDomainVsRAE = new HashMap<String, DomainObject>();

            Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT);
            relationshipPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION);

            Pattern typePattern = new Pattern(TigerConstants.TYPE_PSS_ROLEASSESSMENT);
            typePattern.addPattern(TigerConstants.TYPE_PSS_ROLEASSESSMENT_EVALUATION);

            Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_ROLEASSESSMENT_EVALUATION);
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.add(DomainConstants.SELECT_ID);
            slObjSelectStmts.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]");
            slObjSelectStmts.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]");

            StringList slRelSelectStmts = new StringList();
            slRelSelectStmts.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_VIEW + "]");

            MapList mlRAEList = domIA.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
                    typePattern.getPattern(), // object pattern
                    slObjSelectStmts, // object selects
                    slRelSelectStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 2, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    typePostPattern, null, null, null, null);

            for (Object objRAE : mlRAEList) {
                Map<?, ?> mpRAEObj = (Map<?, ?>) objRAE;
                String strRAEObjId = (String) mpRAEObj.get(DomainConstants.SELECT_ID);
                DomainObject domRAE = DomainObject.newInstance(context, strRAEObjId);
                String strPSS_Domain = (String) mpRAEObj.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]");
                String strPSS_Title = (String) mpRAEObj.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]");
                String strPSS_View = (String) mpRAEObj.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_VIEW + "]");
                if ("FISSummaryView".equals(strPSS_View)) {
                    String strTitle_Domain = strPSS_Title + "." + strPSS_Domain;
                    mpDomainVsRAE.put(strTitle_Domain, domRAE);
                }
            }

            createAndConnectRAEtoRA(context, domRA, strRoleValue, strRAOwner, mpDomainVsRAE);
            domRA.setOwner(context, strRAOwner);

        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:connectRAToIAAndCreateRAE :ERROR ", ex);
            throw ex;
        }
    }

    // PCM2.0 Spr4:TIGTK-7587:6/9/2017:END
    // TIGTK-9020:Rutuja Ekatpure:5/9/2017:Start
    /***
     * This method used on Impact analysis Notification object for getting To list
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList getPerformImpactAnalysisNotificationRecipients(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil : getPerformImpactAnalysisNotificationRecipients : Start");
        StringList slRAOwner = new StringList();
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strCRObjId = (String) programMap.get(DomainConstants.SELECT_ID);
            DomainObject domCRObj = DomainObject.newInstance(context, strCRObjId);

            StringList slObjSelect = new StringList();
            slObjSelect.add(DomainConstants.SELECT_ID);
            slObjSelect.add(DomainConstants.SELECT_OWNER);

            String strObjWhere = "revision == last";
            MapList mlImapctObject = domCRObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjSelect,
                    DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, strObjWhere, DomainConstants.EMPTY_STRING, 0);

            for (Object objIA : mlImapctObject) {
                Map<?, ?> mImpactObj = (Map<?, ?>) objIA;
                String strIAObjId = (String) mImpactObj.get(DomainConstants.SELECT_ID);
                DomainObject domIAObj = DomainObject.newInstance(context, strIAObjId);
                MapList mlRAObjects = domIAObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slObjSelect,
                        DomainConstants.EMPTY_STRINGLIST, false, true, (short) 1, strObjWhere, DomainConstants.EMPTY_STRING, 0);

                for (Object objRA : mlRAObjects) {
                    Map<?, ?> mRA = (Map<?, ?>) objRA;
                    String strRAOwner = (String) mRA.get(DomainConstants.SELECT_OWNER);
                    slRAOwner.add(strRAOwner);
                }
            }

        } catch (Exception e) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil : getPerformImpactAnalysisNotificationRecipients : " + e.getMessage());
            throw e;
        }
        slRAOwner = getUniqueList(slRAOwner);
        String strContextUser = context.getUser();
        if (slRAOwner.contains(strContextUser))
            slRAOwner.remove(strContextUser);
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil : getPerformImpactAnalysisNotificationRecipients : End");
        return slRAOwner;
    }

    /***
     * this method used for removing duplicate entry in StringList
     * @param slInputList
     * @return
     */
    public StringList getUniqueList(StringList slInputList) {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil : getUniqueList : Start");
        StringList slReturn = new StringList();
        try {
            for (Object obj : slInputList) {
                String strTempValue = (String) obj;
                if (!slReturn.contains(strTempValue)) {
                    slReturn.add(strTempValue);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil : getUniqueList : End");
        return slReturn;
    }

    /***
     * This method is used to define toList for functionality Impact Analysis reassignment (i.e. Role Assessment Reassignment)
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList getRoleAssessmentReassignmentNotificationRecipients(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil : getRoleAssessmentReassignmentNotificationRecipients : Start");
        StringList slNewOwner = new StringList();
        Map<?, ?> programMap = JPO.unpackArgs(args);
        String strRAObjId = (String) programMap.get(DomainConstants.SELECT_ID);
        DomainObject domRA = DomainObject.newInstance(context, strRAObjId);
        String strRAOwner = domRA.getInfo(context, DomainConstants.SELECT_OWNER);
        // TIGTK-10792: TS : 08/11/2017 : Start
        Map payLoad = (Map) programMap.get("payload");
        String strContextUser = (String) payLoad.get("fromList");
        if (!strContextUser.equals(strRAOwner)) {
            slNewOwner.add(strRAOwner);
        }
        // TIGTK-10792: TS : 08/11/2017 : End
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil : getRoleAssessmentReassignmentNotificationRecipients : End");
        return slNewOwner;
    }

    /***
     * this method used on Impact Analysis Rejection Notification Object for getting To List
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList getImpactAnalysisRejectionNotificationRecipients(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil : getImpactAnalysisRejectionNotificationRecipients : Start");
        StringList slCMs = new StringList();
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strIAObjId = (String) programMap.get(DomainConstants.SELECT_ID);
            DomainObject domIAObj = DomainObject.newInstance(context, strIAObjId);
            String strCRObjId = domIAObj.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");

            StringList slRolesList = new StringList();
            slRolesList.addElement(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, null);
            slCMs = commonObj.getProgramProjectTeamMembersForChange(context, strCRObjId, slRolesList, false);
        } catch (Exception e) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil : getImpactAnalysisRejectionNotificationRecipients : " + e.getMessage());
        }
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil : getImpactAnalysisRejectionNotificationRecipients : End");
        return slCMs;

    }

    // TIGTK-9020:Rutuja Ekatpure:5/9/2017:End
    /**
     * @description : This method is to get all Role Assessments on Task DashBoard
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public MapList getRoleAssessments(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleAssessments:START");
        try {
            MapList mlRoleAssessment = new MapList();
            String strContextUser = context.getUser();
            StringList objectSelect = new StringList();
            objectSelect.add(DomainConstants.SELECT_ID);
            String objWhere = "owner==" + "\"" + strContextUser + "\"";
            MapList mlRoleAssessments = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_ROLEASSESSMENT, TigerConstants.VAULT_ESERVICEPRODUCTION, objWhere, objectSelect);
            Iterator<?> itrRoleAssessment = mlRoleAssessments.iterator();
            while (itrRoleAssessment.hasNext()) {
                Map<String, String> objectMap = (Map<String, String>) itrRoleAssessment.next();
                objectMap.put("disableSelection", "true");
                mlRoleAssessment.add(objectMap);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleAssessments:END");
            return mlRoleAssessment;
        } catch (Exception e) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleAssessments:ERROR ", e);
            throw e;
        }
    }

    // PCM2.0 Spr4:TIGTK-7591:11/9/2017:START
    /**
     * This method is Edit access function on LA Table for edit only active LA
     * @param context
     * @param args
     * @return MapList
     * @author sirale
     * @throws Exception
     */
    public MapList getViewSpecificDisplayAttributes(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getViewSpecificDisplayAttributes:START");
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            String strViewName = (String) programMap.get("view");
            String strRAId = (String) programMap.get("objectId");
            DomainObject domRA = DomainObject.newInstance(context, strRAId);

            StringList slObjSelect = new StringList();
            slObjSelect.add(DomainConstants.SELECT_ID);
            slObjSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]");
            slObjSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]");
            String relWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_VIEW + "]==" + strViewName;

            MapList mlRAE = domRA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION, TigerConstants.TYPE_PSS_ROLEASSESSMENT_EVALUATION, slObjSelect, null, false,
                    true, (short) 1, null, relWhere, 0);

            Iterator<Map<String, String>> itrRAE = mlRAE.iterator();
            while (itrRAE.hasNext()) {
                Map<String, String> tempMap = itrRAE.next();
                String sTitleTechValue = tempMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]");
                String sDomainTechValue = tempMap.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]");

                if (sDomainTechValue.equals("")) {
                    sDomainTechValue = "NA";
                }
                String sTitleKey = "PSS_EnterpriseChangeMgt.Label.Title." + sTitleTechValue;
                String sDomainKey = "PSS_EnterpriseChangeMgt.Label.Domain." + sDomainTechValue;

                String sTitleDisplayValue = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), sTitleKey);
                String sDomainDisplayValue = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), sDomainKey);

                tempMap.put("attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]", sTitleDisplayValue);
                tempMap.put("attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]", sDomainDisplayValue);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getViewSpecificDisplayAttributes:END");
            return mlRAE;
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getViewSpecificDisplayAttributes:ERROR ", ex);
            throw ex;
        }

    }

    /**
     * This method is Edit access function on LA Table for edit only active LA
     * @param context
     * @param args
     * @return MapList
     * @author sirale
     * @throws Exception
     */
    public MapList getConnectedImpactAnalyisForRA(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getConnectedImpactAnalyisForRA:START");
        try {
            HashMap programMap = JPO.unpackArgs(args);
            String strRAobjectId = (String) programMap.get("objectId");
            DomainObject domRA = DomainObject.newInstance(context, strRAobjectId);
            StringList slobjSelect = new StringList();
            slobjSelect.add(DomainConstants.SELECT_ID);

            MapList mlImpactAnalysis = domRA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slobjSelect, null, true, false,
                    (short) 1, null, null, 0);

            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getConnectedImpactAnalyisForRA:END");
            return mlImpactAnalysis;
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getViewSpecificDisplayAttributes:ERROR ", ex);
            throw ex;
        }

    }

    public boolean getTableColumnShowAccess(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getTableColumnShowAccess:START");
        boolean isAllowed = false;
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            Map<?, ?> mpSettings = (Map<?, ?>) programMap.get("SETTINGS");
            String sExcludeRole = (String) mpSettings.get("ExcludeRole");
            String sIncludeRole = (String) mpSettings.get("IncludeRole");

            String strRAObjId = (String) programMap.get("objectId");
            DomainObject domRA = DomainObject.newInstance(context, strRAObjId);

            String strRAtype = domRA.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_RAROLE);

            isAllowed = getAccess(context, strRAtype, sIncludeRole, sExcludeRole, false);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getTableColumnShowAccess:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getTableColumnShowAccess:ERROR ", ex);
            throw ex;
        }
        return isAllowed;
    }

    public boolean getAccess(Context context, String sCompareValue, String sInclude, String sExclude, boolean isEditAccess) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getAccess:START");
        String sIncludeOrExclude = "";
        boolean isExclude = false;
        boolean isAccess = false;
        try {
            if (UIUtil.isNullOrEmpty(sExclude) && UIUtil.isNullOrEmpty(sInclude)) {
                return true;
            }
            if (UIUtil.isNotNullAndNotEmpty(sExclude) && UIUtil.isNotNullAndNotEmpty(sInclude)) {
                throw new Exception("Illigal Use of 'ExcludeRole' and 'IncludeRole' setting. Cannot use both setting in single column.");
            }
            if (UIUtil.isNotNullAndNotEmpty(sExclude)) {
                sIncludeOrExclude = sExclude;
                isExclude = true;
            } else if (UIUtil.isNotNullAndNotEmpty(sInclude)) {
                sIncludeOrExclude = sInclude;
            }
            isAccess = evaluateAccess(sCompareValue, sIncludeOrExclude, isExclude);

            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getAccess:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getAccess:ERROR ", ex);
            throw ex;
        }
        return isAccess;
    }

    public boolean evaluateAccess(String sCompareValue, String sIncludeOrExclude, boolean isExclude) {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:evaluateAccess:START");
        boolean isAccess = false;
        try {
            HashSet<String> hsIncludeOrExclude = new HashSet<String>();
            hsIncludeOrExclude.addAll(Arrays.asList(sIncludeOrExclude.split(",")));
            if (hsIncludeOrExclude.contains(sCompareValue)) {
                isAccess = true;
            }
            if (!isAccess) {
                String[] sArrayCompareValue = sCompareValue.split("~");
                if (hsIncludeOrExclude.contains(sArrayCompareValue[0])) {
                    isAccess = true;
                }
            }
            if (isExclude) {
                isAccess = !isAccess;
            }

            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:evaluateAccess:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:evaluateAccess:ERROR ", ex);
            throw ex;
        }
        return isAccess;
    }

    public boolean getFormFieldShowAccess(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getFormFieldShowAccess:START");
        boolean isAllowed = false;

        try {
            Map<?, ?> paramMap = (Map<?, ?>) JPO.unpackArgs(args);
            Map<?, ?> mpSettings = (Map<?, ?>) paramMap.get("SETTINGS");

            String sExcludeRole = (String) mpSettings.get("ExcludeRole");
            String sIncludeRole = (String) mpSettings.get("IncludeRole");
            String strRAEObjId = (String) paramMap.get("objectId");

            DomainObject domRAE = DomainObject.newInstance(context, strRAEObjId);

            String strCompareValueRARole = domRAE.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION + "].from.attribute[" + TigerConstants.ATTRIBUTE_PSS_RAROLE + "]");

            isAllowed = getAccess(context, strCompareValueRARole, sIncludeRole, sExcludeRole, false);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getFormFieldShowAccess:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getFormFieldShowAccess:ERROR ", ex);
            throw ex;
        }
        return isAllowed;
    }

    public StringList getTableColumnCellEditAccess(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getTableColumnCellEditAccess:START");
        StringList slCellAccessRtrn = new StringList();
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            MapList mlRAEObjs = (MapList) programMap.get("objectList");
            Map<?, ?> columnMap = (Map<?, ?>) programMap.get("columnMap");
            Map<?, ?> mpSettings = (Map<?, ?>) columnMap.get("settings");
            String sIncludeValue = (String) mpSettings.get("IncludeValue");
            String sExcludeValue = (String) mpSettings.get("ExcludeValue");
            for (Object objRAEInfo : mlRAEObjs) {
                boolean isAllowed = false;
                String sIsAllowed = "false";

                Map<?, ?> mpRAEInfo = (Map<?, ?>) objRAEInfo;
                String strRAEId = (String) mpRAEInfo.get("id");
                DomainObject domRAE = DomainObject.newInstance(context, strRAEId);
                String strTitle = domRAE.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_TITLE);
                String strDomain = domRAE.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_DOMAIN);

                String strCompare = strTitle + "~" + strDomain;

                isAllowed = getAccess(context, strCompare, sIncludeValue, sExcludeValue, true);
                if (isAllowed) {
                    sIsAllowed = "true";
                }
                slCellAccessRtrn.add(sIsAllowed);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getTableColumnCellEditAccess:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getTableColumnCellEditAccess:ERROR ", ex);
            throw ex;
        }
        return slCellAccessRtrn;

    }

    // PCM2.0 Spr4:TIGTK-7591:11/9/2017:END

    /**
     * @description : This method is to get all Role Name in IA table column
     * @param context
     * @param args
     * @return StringList
     * @author sirale
     * @throws Exception
     */
    public StringList getRoleNameForRA(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleNameForRA:START");
        StringList slRoleName = new StringList();

        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            MapList mlRoleObjs = (MapList) programMap.get("objectList");

            for (Object objRole : mlRoleObjs) {
                Map<?, ?> mpRole = (Map<?, ?>) objRole;
                String strRAObjectId = (String) mpRole.get(DomainConstants.SELECT_ID);
                DomainObject domIAObj = DomainObject.newInstance(context, strRAObjectId);
                String strRoleName = domIAObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_RAROLE);
                String strKey = "emxFramework.Role." + strRoleName;
                String strPSSRoleValue = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), strKey);
                slRoleName.addElement(strPSSRoleValue);
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleNameForRA:END");
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleNameForRA:ERROR ", ex);
            throw ex;
        }
        return slRoleName;
    }

    /**
     * @description : This method is to get Role on Property page of RA
     * @param context
     * @param args
     * @return String
     * @author sirale
     * @throws Exception
     */
    public String getRoleForRA(Context context, String[] args) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleForRA:START");

        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            Map<?, ?> requestMap = (Map<?, ?>) programMap.get("requestMap");
            String strRAObjectId = (String) requestMap.get("objectId");
            DomainObject domIAObj = DomainObject.newInstance(context, strRAObjectId);

            String strRoleName = domIAObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_RAROLE);
            String strKey = "emxFramework.Role." + strRoleName;
            String strDisplayRoleName = EnoviaResourceBundle.getProperty(context, "emxFrameworkStringResource", context.getLocale(), strKey);

            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleForRA:END");
            return strDisplayRoleName;
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:getRoleForRA:ERROR ", ex);
            throw ex;
        }
    }

    /**
     * @description : This method is used to show edit button on RA FAS and FIS View
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public boolean showRAEditAccess(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:showRAEditAccess:START");
        boolean boolReturn = false;

        String strRAOwner = DomainConstants.EMPTY_STRING;
        String strRARole = DomainConstants.EMPTY_STRING;
        String PSS_PCANDL = "PSS_PCAndL";
        try {

            HashMap<?, ?> param = (HashMap<?, ?>) JPO.unpackArgs(args);
            String objectId = (String) param.get("objectId");
            String strView = (String) param.get("view");

            String strContextUser = context.getUser();

            StringList slList = new StringList(DomainConstants.SELECT_OWNER);
            slList.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_RAROLE + "].value");
            DomainObject domRA = DomainObject.newInstance(context, objectId);

            Map<?, ?> mRAObjs = domRA.getInfo(context, slList);
            if (!mRAObjs.isEmpty()) {
                strRAOwner = (String) mRAObjs.get(DomainConstants.SELECT_OWNER);
                strRARole = (String) mRAObjs.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_RAROLE + "].value");
            }
            String strIAId = domRA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.id");
            if (UIUtil.isNotNullAndNotEmpty(strIAId)) {
                DomainObject domIA = DomainObject.newInstance(context, strIAId);
                String slCRId = domIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");

                DomainObject domCR = DomainObject.newInstance(context, slCRId);
                String strCRCurrent = domCR.getInfo(context, DomainConstants.SELECT_CURRENT);
                if (UIUtil.isNotNullAndNotEmpty(strRARole) && UIUtil.isNotNullAndNotEmpty(strView)) {
                    if ((PSS_PCANDL.equalsIgnoreCase(strRARole) && "FISView".equalsIgnoreCase(strView))) {
                        return boolReturn;
                    }
                }
                // TIGTK-13175 :Start
                if ((TigerConstants.STATE_EVALUATE.equalsIgnoreCase(strCRCurrent) || TigerConstants.STATE_INREVIEW_CR.equalsIgnoreCase(strCRCurrent)) && strRAOwner.contains(strContextUser)) {
                    // TIGTK-13175 :End
                    boolReturn = true;
                }
            }
        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:showRAEditAccess :ERROR ", ex);
            throw ex;
        }
        return boolReturn;
    }

    /**
     * @description : This method is used to show decision date on change of Decision
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public void setDecisiondate(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:setDecisiondate:START");
        try {
            SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, -1);
            String startEffectivityDate = _mxDateFormat.format(new Date());

            String strRAObjectId = (String) args[0];
            DomainObject domRA = DomainObject.newInstance(context, strRAObjectId);
            String strRelId = domRA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].id");
            DomainRelationship domRel = new DomainRelationship(strRelId);
            domRel.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_DECISION_DATE, startEffectivityDate);
        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:setDecisiondate :ERROR ", ex);
            throw ex;
        }
    }

    /***
     * This method is called from JPO method pss.ecm.impactanalysis.SummaryViewUtil : getSLCSummaryValueForFIS which is used to get List of RAE from IA Object.
     * @param context
     * @param strIAObjID
     * @param strRelWhere
     * @param strObjWhere
     * @return MapList
     * @throws FrameworkException
     */
    public static MapList getRAEFromIA(Context context, String strIAObjID, String strRelWhere, String strObjWhere) throws FrameworkException {
        logger.debug("ImpactAnalysisUtil_mxJPO : getRAEFromIA :START ");
        MapList mlReturn = new MapList();
        try {
            StringList slRAobjSelect = new StringList();
            slRAobjSelect.add(DomainConstants.SELECT_ID);
            StringList slRAEobjSelect = new StringList();
            slRAEobjSelect.add(DomainConstants.SELECT_ID);
            slRAEobjSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_TITLE + "]");
            slRAEobjSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_DOMAIN + "]");
            DomainObject domIA = DomainObject.newInstance(context, strIAObjID);
            MapList mplRAObjs = domIA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT, TigerConstants.TYPE_PSS_ROLEASSESSMENT, slRAobjSelect, null, false, true, (short) 1,
                    null, null, 0);

            for (Object objRA : mplRAObjs) {
                Map<?, ?> mpRA = (Map<?, ?>) objRA;
                String strRAID = (String) mpRA.get(DomainConstants.SELECT_ID);
                DomainObject domRA = DomainObject.newInstance(context, strRAID);
                MapList mplRAEvalObjs = domRA.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT_EVALUATION, TigerConstants.TYPE_PSS_ROLEASSESSMENT_EVALUATION, slRAEobjSelect,
                        null, false, true, (short) 1, strObjWhere, strRelWhere, 0);
                for (Object objRAEval : mplRAEvalObjs) {
                    Map<String, String> mpRAE = (Map<String, String>) objRAEval;
                    if (!mlReturn.contains(mpRAE)) {
                        mlReturn.add(mpRAE);
                    }
                }
            }
        } catch (RuntimeException ex) {
            logger.error("ERROR in ImpactAnalysisUtil_mxJPO : getRAEFromIA :  " + ex.getMessage());
            throw ex;
        }
        logger.debug("ImpactAnalysisUtil_mxJPO : getRAEFromIA :END ");
        return mlReturn;
    }

    // TIGTK-10697: TS :30/10/2017:START

    /**
     * @description : This method is used to show Status Column as per the Decision and Due date
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public List<String> showRAStatus(Context context, String args[]) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:showRAStatus:START");
        String statusImageString = DomainConstants.EMPTY_STRING;
        List<String> StatusIconTagList = new Vector<String>();

        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            MapList mlRAObjs = (MapList) programMap.get("objectList");

            for (Object objRA : mlRAObjs) {

                Map<?, ?> mpRole = (Map<?, ?>) objRA;
                String strRAObjectId = (String) mpRole.get(DomainConstants.SELECT_ID);

                String strQueryDecisionDate = "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].attribute[" + TigerConstants.ATTRIBUTE_PSS_DECISION_DATE + "]";
                String strCmdDecisionDate = "print bus \"" + strRAObjectId + "\"   select  \"" + strQueryDecisionDate + "\"  dump | ";
                String strDecsionDate = MqlUtil.mqlCommand(context, strCmdDecisionDate, true, true);

                String strQueryDueDate = "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].attribute[" + TigerConstants.ATTRIBUTE_PSS_DUEDATE + "]";
                String strCmdDueDate = "print bus \"" + strRAObjectId + "\"   select  \"" + strQueryDueDate + "\"  dump | ";
                String strDueDate = MqlUtil.mqlCommand(context, strCmdDueDate, true, true);

                SimpleDateFormat dateFormat = new SimpleDateFormat(eMatrixDateFormat.getInputDateFormat(), context.getLocale());

                String GREEN_ICON = "<img src=\"../common/images/iconStatusGreen.gif\" border=\"0\"  align=\"middle\"/>";
                String RED_ICON = "<img src=\"../common/images/iconStatusRed.gif\" border=\"0\"  align=\"middle\"/>";

                if (strDecsionDate.isEmpty() || strDueDate.isEmpty()) {
                    statusImageString = RED_ICON;
                } else {

                    Date dtDecisionDate = dateFormat.parse(strDecsionDate);
                    Date dtDueDate = dateFormat.parse(strDueDate);

                    if (dtDecisionDate.before(dtDueDate) || dtDecisionDate.equals(dtDueDate)) {
                        statusImageString = GREEN_ICON;
                    } else if (dtDecisionDate.after(dtDueDate) || strDecsionDate.isEmpty()) {
                        statusImageString = RED_ICON;
                    }
                }

                StatusIconTagList.add(statusImageString);
            }

        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:showRAStatus:ERROR ", ex);
            throw ex;
        }
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:showRAStatus:END");
        return StatusIconTagList;

    }

    @com.matrixone.apps.framework.ui.PostProcessCallable
    public HashMap refreshIndentedTable(Context context, String args[]) throws Exception {
        HashMap returnMap = new HashMap();
        returnMap.put("Action", "refresh");
        Map<?, ?> programMap = JPO.unpackArgs(args);
        Map<?, ?> requestMap = (Map<?, ?>) programMap.get("requestMap");
        String strIAID = (String) requestMap.get("objectId");
        DomainObject domRT = DomainObject.newInstance(context, strIAID);
        // TIGTK-13641 : Vishal T :START
        String strRAdescion = domRT.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_READYFORDESCISION);
        String strMessage = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), "EnterpriseChangeMgt.Message.Subject.OwnerDecision");
        if (strRAdescion.equalsIgnoreCase("FALSE")) {
            returnMap.put("Action", "ERROR");
            returnMap.put("Message", strMessage);
        }
        // TIGTK-13641 : Vishal T :END
        return returnMap;
    }

    // TIGTK-10697: TS :30/10/2017:END

    /**
     * This method is used to modify the History of the object.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param strObjectId
     *            - The object id.
     * @return strAction - The action to be added in the history.
     * @return strComment - The comment to be added in the history
     * @throws Exception
     *             if the operation fails
     */
    protected static void modifyHistory(Context context, String strObjectId, String strAction, String strComment) throws Exception {
        logger.debug("modifyHistory : START");
        StringBuffer sbMqlCommand = new StringBuffer("modify bus ");
        sbMqlCommand.append(strObjectId);
        sbMqlCommand.append(" add history \"");
        sbMqlCommand.append("disconnect ");
        sbMqlCommand.append("\" comment \"");
        sbMqlCommand.append(strAction);

        MqlUtil.mqlCommand(context, sbMqlCommand.toString(), true);
        logger.debug("modifyHistory : END");
    }

    /**
     * This method is used to connect Default True Route Template to Program-Project
     * @param context
     * @param args
     * @return
     * @author sirale
     * @throws Exception
     */
    public void connectDefaultRouteTemplate(Context context, String strOrganization, DomainObject domProjObj) throws Exception {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectDefaultRouteTemplate:START");
        try {
            StringList slSelectList = new StringList();
            slSelectList.addElement(DomainConstants.SELECT_ORGANIZATION);
            String sWhere = DomainConstants.SELECT_REVISION + "==last&&" + DomainConstants.SELECT_CURRENT + "==Active&&" + DomainConstants.SELECT_ORGANIZATION + " ==\"" + strOrganization
                    + "\"&&attribute[" + TigerConstants.ATTRIBUTE_FPDM_RTDEFAULT + "]==TRUE";

            StringList objectSelect = new StringList();
            objectSelect.add(DomainObject.SELECT_ID);
            objectSelect.add("attribute[" + TigerConstants.ATTRIBUTE_FPDM_RTPURPOSE + "]");

            MapList mlRouteTemplate = DomainObject.findObjects(context, DomainConstants.TYPE_ROUTE_TEMPLATE, TigerConstants.VAULT_ESERVICEPRODUCTION, sWhere, objectSelect);

            Iterator<?> itrRT = mlRouteTemplate.iterator();
            while (itrRT.hasNext()) {
                Map<?, ?> mRT = (Map<?, ?>) itrRT.next();
                String strRTObjID = (String) mRT.get(DomainConstants.SELECT_ID);
                DomainObject domRT = DomainObject.newInstance(context, strRTObjID);
                String strRTPurposeOfRelease = (String) mRT.get("attribute[" + TigerConstants.ATTRIBUTE_FPDM_RTPURPOSE + "]");

                String[] sRTPurposeOfRelease = strRTPurposeOfRelease.split("PSS_");

                int iCount = sRTPurposeOfRelease.length;
                for (int i = 0; i < iCount; i++) {
                    strRTPurposeOfRelease = sRTPurposeOfRelease[i];
                    if (UIUtil.isNotNullAndNotEmpty(strRTPurposeOfRelease)) {
                        strRTPurposeOfRelease = strRTPurposeOfRelease.trim();
                        StringBuffer sbKey = new StringBuffer("PSS_EnterpriseChangeMgt.FPDM_RTPurpose.PSS_");
                        sbKey.append(strRTPurposeOfRelease);
                        strRTPurposeOfRelease = EnoviaResourceBundle.getProperty(context, "emxEnterpriseChangeMgtStringResource", context.getLocale(), sbKey.toString());
                        DomainRelationship domRelId = DomainRelationship.connect(context, domProjObj, TigerConstants.RELATIONSHIP_PSS_CONNECTEDROUTETEMPLATES, domRT);
                        domRelId.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_ROUTETEMPLATETYPE, strRTPurposeOfRelease);
                    }
                }
            }
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisUtil:connectDefaultRouteTemplate:END");
        } catch (RuntimeException ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:connectDefaultRouteTemplate:ERROR ", ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Error in pss.ecm.impactanalysis.ImpactAnalysisUtil:connectDefaultRouteTemplate:ERROR ", ex);
            throw ex;
        }
    }

    // TIGTK-13641 : Vishal T :START
    public boolean showRAReadyForDecision(Context context, String[] args) throws Exception {
        HashMap param = (HashMap) JPO.unpackArgs(args);
        String objectId = (String) param.get("objectId");
        boolean boolReturn = false;
        DomainObject domRA = DomainObject.newInstance(context, objectId);
        // START :: TIGTK-18253 :: ALM-6408
        if (domRA.isKindOf(context, TigerConstants.TYPE_PSS_ROLEASSESSMENT_EVALUATION)) {
            objectId = (String) param.get("parentId");
            if(UIUtil.isNotNullAndNotEmpty(objectId)){
                domRA.setId(objectId);
            }
        }
        // END :: TIGTK-18253 :: ALM-6408
        String strCurrent = domRA.getInfo(context, DomainConstants.SELECT_CURRENT);
        String slRAOwner = domRA.getInfo(context, DomainConstants.SELECT_OWNER);
        if (strCurrent.equalsIgnoreCase(TigerConstants.STATE_ACTIVE)) {
            String strIAId = domRA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_ROLEASSESSMENT + "].from.id");
            DomainObject domCRIA = DomainObject.newInstance(context, strIAId);
            String strContextUser = context.getUser();
            String strCRId = domCRIA.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].from.id");
            // TIGTK-12983 : JV : START
            pss.jv.common.PSS_JVUtil_mxJPO jvUtil = new pss.jv.common.PSS_JVUtil_mxJPO();                
            boolean isJV = jvUtil.isJVProgramProject(context, strCRId);
            // TIGTK-12983 : JV : END
            StringList slRolesList = new StringList();
            if(isJV) {
                slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER_JV);
            } else {
            slRolesList.addElement(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
            }
            slRolesList.addElement(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR);
            pss.ecm.ui.CommonUtil_mxJPO commonObj = new pss.ecm.ui.CommonUtil_mxJPO(context, args);
            StringList slCMPMList = commonObj.getProgramProjectTeamMembersForChange(context, strCRId, slRolesList, false);
            DomainObject domCRId = DomainObject.newInstance(context, strCRId);
            String strCRCurrent = domCRId.getInfo(context, DomainConstants.SELECT_CURRENT);
            if ((slCMPMList.contains(strContextUser) || slRAOwner.contains(strContextUser)) && (strCRCurrent.contains(TigerConstants.STATE_EVALUATE))) {
                boolReturn = true;
            } else {
                boolReturn = false;
            }
        }
        return boolReturn;

    }
    // TIGTK-13641 : Vishal T :END

    // TIGTK-13641 : Vishal T :START
    public String getCRRelatedLatestImpactAnalyisObject(Context context, String[] args) throws Exception {
        HashMap param = (HashMap) JPO.unpackArgs(args);
        String strCRId = (String) param.get("id");
        DomainObject domCRObj = DomainObject.newInstance(context, strCRId);
        String strLatestRevIAId = domCRObj.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].to.last.id");
        return strLatestRevIAId;

    }
    // TIGTK-13641 : Vishal T :START
}