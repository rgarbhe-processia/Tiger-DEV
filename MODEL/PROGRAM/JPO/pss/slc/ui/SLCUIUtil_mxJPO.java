package pss.slc.ui;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.XSSUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Access;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.db.JPO;
import matrix.db.User;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

@SuppressWarnings("serial")
public class SLCUIUtil_mxJPO extends DomainObject {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SLCUIUtil_mxJPO.class);

    private static final long serialVersionUID = 1L;

    /**
     * TIGTK-13922 : Get Selectables for SLC Table
     * @param context
     * @return
     */
    public StringList getSLCTableSelectables(Context context) {
        StringList slselectObjStmts = new StringList();
        try {
            slselectObjStmts.addElement(DomainConstants.SELECT_ID);
            slselectObjStmts.addElement(DomainConstants.SELECT_CURRENT);
            slselectObjStmts.addElement(DomainConstants.SELECT_NAME);
            slselectObjStmts.addElement(DomainConstants.SELECT_DESCRIPTION);
            slselectObjStmts.addElement(DomainConstants.SELECT_TYPE);
            slselectObjStmts.addElement(ChangeConstants.SELECT_ATTRIBUTE_REASON_FOR_CHANGE);
            slselectObjStmts.addElement(DomainConstants.SELECT_ORIGINATED);
            slselectObjStmts.addElement(DomainConstants.SELECT_OWNER);
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ACTUALIMPLEMENTATIONDATE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ASSESSMENTDEVTCOST + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ASSESSMENT_PC_L + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ASSESSMENT_PARTCOST + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ASSESSMENT_PROCESSRISK + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ASSESSMENT_PRODUCTRISK + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ASSESSMENTTOOLINGCOST_GAUGES + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_BPUPDATEDATE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRBILLABLE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRORIGINSUBTYPE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRORIGINTYPE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CAPEXCONTRIB_LAUNCHCOST + "]");
            slselectObjStmts.addElement("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CONTROLPLAN + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRCUSTOMERAGREEMENTDATE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRCUSTOMERCHANGENUMBER + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRCUSTOMERINVOLVEMENT + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_DVP_PVP + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRDATEOFLASTTRANSFERTOCHANGEMANAGER + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_DECISIONCASHPAYMENT + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_DECISION_CUSTOMERSTATUS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_DECISION_DATE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_DESIGNREVIEWDATE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_DESIGNREVIEWSTATUS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_FMEA_D_P_L + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_IMPACTONACTBP + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_IMPACTED_REGIONS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_INTERCHANGEABILITYPROPAGATIONSTATUS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_KCC_KPC + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PPAP_INTERN_OR_CUST_DATE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PPAP_INTERN_OR_CUST_STATUS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACK + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARALLELTRACKCOMMENT + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PARTPRICE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PROJECT_PHASE_AT_CREATION + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_PURPOSE_OF_RELEASE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREASONFORCHANGE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRREQUESTEDASSESSMENTENDDATE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCCOMMENTS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SALESSTATUS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SUPPLIER_TEAM_FEASIBILITY_COMMITMENT + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONCOMMENT + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTARGETCHANGEIMPLEMENTATIONDATE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTITLE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_TOOLINGKICKOFFDATE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_TOOLINGKICKOFFSTATUS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_TOOLINGREVIEWDATE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_TOOLINGREVIEWSTATUS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_TOOLINGPRICE + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_VALIDATION_MVP_R_R + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCCAPEXCONTRIBUTION + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCDEVELOPMENTCONTRIBUTION + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ACTIONCOMMENTS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ASSESSMENT_RISK + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCBOPCOSTS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCCAPEX + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCCAPEXCUSTOMERCASHCONTRIBUTION + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCCONTRIBUTION + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCDIRECTLABORCOST + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCDNDDPROTOSALES + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCFREIGHTOUTCOSTS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCIMPACTONPPAPAIDBYCUSTOMERPERPART + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCLAUNCHCOSTS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCLAUNCHCOSTSCUSTOMERPARTICIPATION + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCPROTOCOSTS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCSCRAP + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCSUBSIDIES + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCTOOLINGCOSTS + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCTOOLINGSALES + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ISSUE_DESCRIPTION + "]");
            slselectObjStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_SLCRMCOST + "]");

            slselectObjStmts.addElement("relationship[" + TigerConstants.RELATIONSHIP_CHANGECOORDINATOR + "].to.name");
            slselectObjStmts.addElement("relationship[" + TigerConstants.RELATIONSHIP_ISSUE + "].to.description");
            slselectObjStmts.addElement("relationship[" + TigerConstants.RELATIONSHIP_ISSUE + "].to.name");
            slselectObjStmts.addElement("relationship[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.name");
            slselectObjStmts.addElement("relationship[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.description");
            slselectObjStmts.addElement("relationship[" + TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS + "].to.name");

        } catch (Exception ex) {
            logger.error("Error in getSLCTableSelectables: ", ex);
            throw ex;
        }

        return slselectObjStmts;
    }

    /**
     * This method returns All Change Requests
     * @param context
     * @param args
     * @return list of CR objects
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public MapList getAllCRs(Context context, String[] args) throws Exception {
        long lStartTime = System.nanoTime();
        Pattern relationship_pattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
        relationship_pattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);

        Pattern type_pattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        type_pattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        Pattern finalType = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        try {
            StringList slselectObjStmts = getSLCTableSelectables(context);
            Map programMap = (Map) JPO.unpackArgs(args);
            String objectId = (String) programMap.get("objectId");
            String strProgProj = (String) programMap.get("parentOID");
            // TIGTK-16801 : 29-08-2018 : START
            boolean bAdminOrPMCMEditAllow = isAdminOrPMCMofRelatedPP(context, strProgProj);
            // TIGTK-16801 : 29-08-2018 : END

            BusinessObject busObj = new BusinessObject(objectId);

            // TIGTK-13922 START
            MapList mapList = FrameworkUtil.toMapList(busObj.getExpansionIterator(context, relationship_pattern.getPattern(), type_pattern.getPattern(), slselectObjStmts, new StringList(), false,
                    true, (short) 0, null, null, (short) 0, false, true, (short) 100, false), (short) 0, finalType, null, null, null);
            // TIGTK-13922 END
            // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:Start
            // sort maplist according to CR state
            if (mapList.size() > 0) {
                MapList mlReturn = sortCRListByState(context, mapList);
                long lEndTime = System.nanoTime();
                long output = lEndTime - lStartTime;

                logger.debug("Total Execution time to Get all CRs in milliseconds: " + output / 1000000);
                // PCM2.0 Spr4:TIGTK-6894:19/9/2017:START

                Iterator<Map<String, String>> itrCR = mlReturn.iterator();

                while (itrCR.hasNext()) {
                    Map tempMap = itrCR.next();
                    String strCRId = (String) tempMap.get(DomainConstants.SELECT_ID);
                    MapList mlIA = getImpactAnalysis(context, strCRId);

                    String strIAId = "";
                    if (!mlIA.isEmpty()) {
                        Map mpIA = (Map) mlIA.get(0);
                        strIAId = (String) mpIA.get(DomainConstants.SELECT_ID);
                    }
                    tempMap.put("LatestRevIAObjectId", strIAId);
                    // TIGTK-16801 : 29-08-2018 : START
                    tempMap.put("bAdminOrPMCMEditAllow", bAdminOrPMCMEditAllow);
                    // TIGTK-16801 : 29-08-2018 : END
                }
                // PCM2.0 Spr4:TIGTK-6894:19/9/2017:END
                return mlReturn;
            } else {
                return mapList;
            }
            // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:
        } catch (Exception ex) {
            logger.error("Error in getAllCRs: ", ex);
            throw ex;
        }

    }

    public MapList getImpactAnalysis(Context context, String strCRId) throws Exception {
        MapList mlIA;
        try {
            long lStartTime = System.nanoTime();
            BusinessObject domCR = new BusinessObject(strCRId);
            short sQueryLimit = 0;
            String objWhere = "revision==last";
            StringList slObjSelects = new StringList(DomainConstants.SELECT_ID);

            ContextUtil.startTransaction(context, false);
            ExpansionIterator expIter = domCR.getExpansionIterator(context, TigerConstants.RELATIONSHIP_PSS_IMPACTANALYSIS, TigerConstants.TYPE_PSS_IMPACTANALYSIS, slObjSelects, new StringList(0),
                    false, true, (short) 1, objWhere, "", sQueryLimit, true, false, (short) 100, false);
            mlIA = FrameworkUtil.toMapList(expIter, sQueryLimit, null, null, null, null);
            expIter.close();
            ContextUtil.commitTransaction(context);
            long lEndTime = System.nanoTime();
            long output = lEndTime - lStartTime;

            logger.debug("Total Execution time to Get all Impact Analysis in milliseconds: " + output / 1000000);
        } catch (Exception ex) {
            logger.error("Error in getImpactAnalysis: ", ex);
            throw ex;
        }

        return mlIA;
    }

    // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:Start
    /***
     * method used for sorting maplist of CR according to CR state
     * @param context
     * @param mlCR
     * @return
     * @throws Exception
     */
    public MapList sortCRListByState(Context context, MapList mlCR) throws Exception {
        // TIGTK-13922 START

        MapList mlTempCRFinal = new MapList();
        MapList mlTempCRCreate = new MapList();
        MapList mlTempCRSubmit = new MapList();
        MapList mlTempCREvaluate = new MapList();
        MapList mlTempCRINReview = new MapList();
        MapList mlTempCRInProcess = new MapList();
        MapList mlTempCRComplete = new MapList();
        MapList mlTempCRRejected = new MapList();
        try {

            Iterator itr = mlCR.iterator();
            while (itr.hasNext()) {
                Map mpCRObj = (Map) itr.next();
                String strCRState = (String) mpCRObj.get(DomainConstants.SELECT_CURRENT);
                switch (strCRState) {
                case "Create":
                    mlTempCRCreate.add(mpCRObj);
                    break;

                case "Submit":
                    mlTempCRSubmit.add(mpCRObj);

                    break;
                case "Evaluate":
                    mlTempCREvaluate.add(mpCRObj);
                    break;
                case "In Review":
                    mlTempCRINReview.add(mpCRObj);
                    break;
                case "In Process":
                    mlTempCRInProcess.add(mpCRObj);
                    break;
                case "Complete":
                    mlTempCRComplete.add(mpCRObj);
                    break;
                case "Rejected":
                    mlTempCRRejected.add(mpCRObj);
                    break;
                default:
                    break;

                }
            }
            mlTempCRFinal.addAll(mlTempCRCreate);
            mlTempCRFinal.addAll(mlTempCRSubmit);
            mlTempCRFinal.addAll(mlTempCREvaluate);
            mlTempCRFinal.addAll(mlTempCRINReview);
            mlTempCRFinal.addAll(mlTempCRInProcess);
            mlTempCRFinal.addAll(mlTempCRComplete);
            mlTempCRFinal.addAll(mlTempCRRejected);
            // TIGTK-13922 END

        } catch (Exception ex) {
            logger.error("Error in sortCRListByState: ", ex);
            throw ex;
        }
        return mlTempCRFinal;
    }

    // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:End

    /**
     * This method returns All Change Requests which are In Process
     * @param context
     * @param args
     * @return list of CR objects
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public MapList getInProcessCRs(Context context, String[] args) throws Exception {

        Pattern relationship_pattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
        relationship_pattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);

        Pattern type_pattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        type_pattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        Pattern finalType = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

        try {
            StringList slselectObjStmts = getSLCTableSelectables(context);

            Map programMap = (Map) JPO.unpackArgs(args);
            String strProgProjId = (String) programMap.get("objectId");
            // TIGTK-16801 : 30-08-2018 : START
            boolean bAdminOrPMCMEditAllow = isAdminOrPMCMofRelatedPP(context, strProgProjId);
            StringBuffer sbObjectWhere = new StringBuffer();
            sbObjectWhere.append("(type==\"");
            sbObjectWhere.append(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            sbObjectWhere.append("\"&&current==\"");
            sbObjectWhere.append(TigerConstants.STATE_PSS_CR_INPROCESS);
            sbObjectWhere.append("\")");
            // TIGTK-16801 : 30-08-2018 : END

            DomainObject domainObj = DomainObject.newInstance(context, strProgProjId);

            MapList mapList = domainObj.getRelatedObjects(context, relationship_pattern.getPattern(), // relationship pattern
                    type_pattern.getPattern(), // object pattern
                    slselectObjStmts, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    sbObjectWhere.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    finalType, // Postpattern
                    null, null, null);

            // PCM2.0 Spr4:TIGTK-6894:19/9/2017:START
            if (mapList.size() > 0) {

                Iterator<Map<String, String>> itrCR = mapList.iterator();
                while (itrCR.hasNext()) {
                    Map tempMap = itrCR.next();
                    String strCRId = (String) tempMap.get(DomainConstants.SELECT_ID);
                    MapList mlIA = getImpactAnalysis(context, strCRId);

                    String strIAId = "";
                    if (!mlIA.isEmpty()) {
                        Map mpIA = (Map) mlIA.get(0);
                        strIAId = (String) mpIA.get(DomainConstants.SELECT_ID);
                    }
                    tempMap.put("LatestRevIAObjectId", strIAId);
                    // TIGTK-16801 : 30-08-2018 : START
                    tempMap.put("bAdminOrPMCMEditAllow", bAdminOrPMCMEditAllow);
                    // TIGTK-16801 : 30-08-2018 : END
                }
            }
            // PCM2.0 Spr4:TIGTK-6894:19/9/2017:END
            return mapList;
        } catch (Exception ex) {
            logger.error("Error in getInProcessCRs: ", ex);
            throw ex;
        }
    }

    /**
     * This method returns All Change Requests that are completed
     * @param context
     * @param args
     * @return list of CR objects
     * @throws Exception
     */

    @SuppressWarnings("rawtypes")
    public MapList getCompletedCRs(Context context, String[] args) throws Exception {

        Pattern relationship_pattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
        relationship_pattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);

        Pattern type_pattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        type_pattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        Pattern finalType = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        try {
            StringList slselectObjStmts = getSLCTableSelectables(context);

            Map programMap = (Map) JPO.unpackArgs(args);
            String strProgProjId = (String) programMap.get("objectId");
            // TIGTK-16801 : 30-08-2018 : START
            boolean bAdminOrPMCMEditAllow = isAdminOrPMCMofRelatedPP(context, strProgProjId);
            StringBuffer sbObjectWhere = new StringBuffer();
            sbObjectWhere.append("(type==\"");
            sbObjectWhere.append(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            sbObjectWhere.append("\"&&current==\"");
            sbObjectWhere.append(TigerConstants.STATE_COMPLETE_CR);
            sbObjectWhere.append("\")");
            // TIGTK-16801 : 30-08-2018 : END

            DomainObject domainObj = DomainObject.newInstance(context, strProgProjId);

            MapList mapList = domainObj.getRelatedObjects(context, relationship_pattern.getPattern(), // relationship pattern
                    type_pattern.getPattern(), // object pattern
                    slselectObjStmts, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    sbObjectWhere.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    finalType, // Postpattern
                    null, null, null);

            // PCM2.0 Spr4:TIGTK-6894:19/9/2017:START
            if (mapList.size() > 0) {

                Iterator<Map<String, String>> itrCR = mapList.iterator();
                while (itrCR.hasNext()) {
                    Map tempMap = itrCR.next();
                    String strCRId = (String) tempMap.get(DomainConstants.SELECT_ID);
                    MapList mlIA = getImpactAnalysis(context, strCRId);

                    String strIAId = "";
                    if (!mlIA.isEmpty()) {
                        Map mpIA = (Map) mlIA.get(0);
                        strIAId = (String) mpIA.get(DomainConstants.SELECT_ID);
                    }
                    tempMap.put("LatestRevIAObjectId", strIAId);
                    // TIGTK-16801 : 30-08-2018 : START
                    tempMap.put("bAdminOrPMCMEditAllow", bAdminOrPMCMEditAllow);
                    // TIGTK-16801 : 30-08-2018 : END
                }
            }
            // PCM2.0 Spr4:TIGTK-6894:19/9/2017:END
            return mapList;
        } catch (Exception ex) {
            logger.error("Error in getCompletedCRs: ", ex);
            throw ex;
        }
    }

    /**
     * This method returns All Change Requests that are opened
     * @param context
     * @param args
     * @return list of CR objects
     * @throws Exception
     */
    public MapList getOpenedCRs(Context context, String[] args) throws Exception {

        Pattern relationship_pattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
        relationship_pattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);

        Pattern type_pattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        type_pattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        Pattern finalType = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

        try {

            StringList slselectObjStmts = getSLCTableSelectables(context);

            Map programMap = (Map) JPO.unpackArgs(args);
            String strProgProjId = (String) programMap.get("objectId");
            // TIGTK-16801 : 30-08-2018 : START
            boolean bAdminOrPMCMEditAllow = isAdminOrPMCMofRelatedPP(context, strProgProjId);
            StringBuffer sbObjectWhere = new StringBuffer();
            sbObjectWhere.append("(type==\"");
            sbObjectWhere.append(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            sbObjectWhere.append("\"&&current!=\"");
            sbObjectWhere.append(TigerConstants.STATE_COMPLETE_CR);
            sbObjectWhere.append("\"");
            sbObjectWhere.append("&&");
            sbObjectWhere.append("current!=\"");
            sbObjectWhere.append(TigerConstants.STATE_REJECTED_CR);
            sbObjectWhere.append("\")");
            // TIGTK-16801 : 30-08-2018 : END

            DomainObject domainObj = DomainObject.newInstance(context, strProgProjId);

            MapList mapList = domainObj.getRelatedObjects(context, relationship_pattern.getPattern(), // relationship pattern
                    type_pattern.getPattern(), // object pattern
                    slselectObjStmts, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    sbObjectWhere.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    finalType, // Postpattern
                    null, null, null);
            // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:Start
            if (mapList.size() > 0) {
                // return sortCRListByState(context, mapList);
                // PCM2.0 Spr4:TIGTK-6894:19/9/2017:START
                MapList mlReturn = sortCRListByState(context, mapList);

                Iterator<Map<String, String>> itrCR = mlReturn.iterator();
                while (itrCR.hasNext()) {
                    Map tempMap = itrCR.next();
                    String strCRId = (String) tempMap.get(DomainConstants.SELECT_ID);
                    MapList mlIA = getImpactAnalysis(context, strCRId);

                    String strIAId = "";
                    if (!mlIA.isEmpty()) {
                        Map mpIA = (Map) mlIA.get(0);
                        strIAId = (String) mpIA.get(DomainConstants.SELECT_ID);
                    }
                    tempMap.put("LatestRevIAObjectId", strIAId);
                    // TIGTK-16801 : 30-08-2018 : START
                    tempMap.put("bAdminOrPMCMEditAllow", bAdminOrPMCMEditAllow);
                    // TIGTK-16801 : 30-08-2018 : END
                }
                return mlReturn;
                // PCM2.0 Spr4:TIGTK-6894:19/9/2017:END
            } else {
                return mapList;
            }
            // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:End
        } catch (Exception ex) {
            logger.error("Error in getOpenedCRs: ", ex);
            throw ex;
        }
    }

    /**
     * This method returns All Change Requests that are open since a month
     * @param context
     * @param args
     * @return list of CR objects
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MapList getOpenedThisMonthCRs(Context context, String[] args) throws Exception {

        Pattern relationship_pattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
        relationship_pattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);

        Pattern type_pattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        type_pattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        Pattern finalType = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

        MapList mapList = new MapList();
        try {

            StringList slselectObjStmts = getSLCTableSelectables(context);

            Map programMap = (Map) JPO.unpackArgs(args);
            String strProgProjId = (String) programMap.get("objectId");
            // TIGTK-16801 : 29-08-2018 : START
            boolean bAdminOrPMCMEditAllow = isAdminOrPMCMofRelatedPP(context, strProgProjId);
            StringBuffer sbObjectWhere = new StringBuffer();
            sbObjectWhere.append("(type==\"");
            sbObjectWhere.append(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            sbObjectWhere.append("\"&&current!=\"");
            sbObjectWhere.append(TigerConstants.STATE_COMPLETE_CR);
            sbObjectWhere.append("\"");
            sbObjectWhere.append("&&");
            sbObjectWhere.append("current!=\"");
            sbObjectWhere.append(TigerConstants.STATE_REJECTED_CR);
            sbObjectWhere.append("\")");
            // TIGTK-16801 : 29-08-2018 : END
            DomainObject domainObj = DomainObject.newInstance(context, strProgProjId);

            Date dcurrentDate = new Date();

            MapList tempList = domainObj.getRelatedObjects(context, relationship_pattern.getPattern(), // relationship pattern
                    type_pattern.getPattern(), // object pattern
                    slselectObjStmts, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    sbObjectWhere.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    finalType, // Postpattern
                    null, null, null);

            for (int i = 0; i < tempList.size(); i++) {

                Map map = (Map) tempList.get(i);

                String sCROriginDate = (String) map.get("originated");
                SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
                Date dCROriginDate = formatter.parse(sCROriginDate);

                Calendar cal = Calendar.getInstance();
                cal.setTime(dcurrentDate);
                cal.add(Calendar.MONTH, -1);
                Date dLastMonthDate = cal.getTime();

                if (dCROriginDate.after(dLastMonthDate) && dCROriginDate.before(dcurrentDate)) {
                    mapList.add(map);
                }

                // TIGTK-16801 : 30-08-2018 : START
                map.put("bAdminOrPMCMEditAllow", bAdminOrPMCMEditAllow);
                // TIGTK-16801 : 30-08-2018 : END

            }
            // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:Start
            if (mapList.size() > 1) {
                return sortCRListByState(context, mapList);
            } else {
                return mapList;
            }
            // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:End
        } catch (Exception ex) {
            logger.error("Error in getOpenedThisMonthCRs: ", ex);
            throw ex;
        }
    }

    /**
     * This method returns All Change Requests that of type Customer
     * @param context
     * @param args
     * @return list of CR objects
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public MapList getCustomerCRs(Context context, String[] args) throws Exception {

        Pattern relationship_pattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
        relationship_pattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);

        Pattern type_pattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        type_pattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        Pattern finalType = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

        try {

            StringList slselectObjStmts = getSLCTableSelectables(context);

            Map programMap = (Map) JPO.unpackArgs(args);
            String strProgProjId = (String) programMap.get("objectId");
            // TIGTK-16801 : 30-08-2018 : START
            boolean bAdminOrPMCMEditAllow = isAdminOrPMCMofRelatedPP(context, strProgProjId);
            StringBuffer sbObjectWhere = new StringBuffer();
            sbObjectWhere.append("(type==\"");
            sbObjectWhere.append(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            sbObjectWhere.append("\"&&attribute[");
            sbObjectWhere.append(TigerConstants.ATTRIBUTE_PSS_CRORIGINTYPE);
            sbObjectWhere.append("] == ");
            sbObjectWhere.append("\"");
            sbObjectWhere.append("Customer");
            sbObjectWhere.append("\")");
            // TIGTK-16801 : 30-08-2018 : END

            DomainObject domainObj = DomainObject.newInstance(context, strProgProjId);

            MapList mapList = domainObj.getRelatedObjects(context, relationship_pattern.getPattern(), // relationship pattern
                    type_pattern.getPattern(), // object pattern
                    slselectObjStmts, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    sbObjectWhere.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    finalType, // Postpattern
                    null, null, null);
            // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:Start
            if (mapList.size() > 1) {
                // return sortCRListByState(context, mapList);
                // PCM2.0 Spr4:TIGTK-6894:19/9/2017:START
                MapList mlReturn = sortCRListByState(context, mapList);

                Iterator<Map<String, String>> itrCR = mlReturn.iterator();
                while (itrCR.hasNext()) {
                    Map tempMap = itrCR.next();
                    String strCRId = (String) tempMap.get(DomainConstants.SELECT_ID);
                    MapList mlIA = getImpactAnalysis(context, strCRId);

                    String strIAId = "";
                    if (!mlIA.isEmpty()) {
                        Map mpIA = (Map) mlIA.get(0);
                        strIAId = (String) mpIA.get(DomainConstants.SELECT_ID);
                    }
                    tempMap.put("LatestRevIAObjectId", strIAId);
                    // TIGTK-16801 : 30-08-2018 : START
                    tempMap.put("bAdminOrPMCMEditAllow", bAdminOrPMCMEditAllow);
                    // TIGTK-16801 : 30-08-2018 : END
                }
                return mlReturn;
                // PCM2.0 Spr4:TIGTK-6894:19/9/2017:END
            } else {
                return mapList;
            }
            // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:End
        } catch (Exception ex) {
            logger.error("Error in getCustomerCRs: ", ex);
            throw ex;
        }
    }

    /**
     * This method returns All Change Requests that are Active
     * @param context
     * @param args
     * @return list of CR objects
     * @throws Exception
     */
    public MapList getActiveCRs(Context context, String[] args) throws Exception {

        Pattern relationship_pattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);
        relationship_pattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);

        Pattern type_pattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        type_pattern.addPattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

        Pattern finalType = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
        try {

            StringList slselectObjStmts = getSLCTableSelectables(context);

            Map programMap = (Map) JPO.unpackArgs(args);
            String strProgProjId = (String) programMap.get("objectId");
            // TIGTK-16801 : 30-08-2018 : START
            boolean bAdminOrPMCMEditAllow = isAdminOrPMCMofRelatedPP(context, strProgProjId);
            StringBuffer sbWhereBuffer = new StringBuffer();
            sbWhereBuffer.append("(type==\"");
            sbWhereBuffer.append(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            sbWhereBuffer.append("\"&&current!=\"");
            sbWhereBuffer.append(TigerConstants.STATE_REJECTED_CR);
            sbWhereBuffer.append("\")");
            // TIGTK-16801 : 30-08-2018 : END

            DomainObject domainObj = DomainObject.newInstance(context, strProgProjId);

            MapList mapList = domainObj.getRelatedObjects(context, relationship_pattern.getPattern(), // relationship pattern
                    type_pattern.getPattern(), // object pattern
                    slselectObjStmts, // object selects
                    null, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 0, // recursion level
                    sbWhereBuffer.toString(), // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    finalType, // Postpattern
                    null, null, null);

            // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:Start
            if (mapList.size() > 0) {
                // return sortCRListByState(context, mapList);
                MapList mlReturn = sortCRListByState(context, mapList);

                // PCM2.0 Spr4:TIGTK-6894:19/9/2017:START

                Iterator<Map<String, String>> itrCR = mlReturn.iterator();
                while (itrCR.hasNext()) {
                    Map tempMap = itrCR.next();
                    String strCRId = (String) tempMap.get(DomainConstants.SELECT_ID);
                    MapList mlIA = getImpactAnalysis(context, strCRId);

                    String strIAId = "";
                    if (!mlIA.isEmpty()) {
                        Map mpIA = (Map) mlIA.get(0);
                        strIAId = (String) mpIA.get(DomainConstants.SELECT_ID);
                    }
                    tempMap.put("LatestRevIAObjectId", strIAId);
                    // TIGTK-16801 : 30-08-2018 : START
                    tempMap.put("bAdminOrPMCMEditAllow", bAdminOrPMCMEditAllow);
                    // TIGTK-16801 : 30-08-2018 : END
                }
                return mlReturn;
                // PCM2.0 Spr4:TIGTK-6894:19/9/2017:END

            } else {
                return mapList;
            }
            // TIGTK_3961:Sort CR data according to CR state :23/1/2017:Rutuja Ekatpure:End

        } catch (Exception ex) {
            logger.error("Error in getActiveCRs: ", ex);
            throw ex;
        }
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
            logger.error("Error in getULSRelationshipConstants: ", ex);
            throw ex;
        }

    }

    /**
     * To show data of CR,CO,Issue,MCO
     * @param context
     * @param args
     * @return consolidated CRs list
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public MapList getConnectedPCMDataForSLC(Context context, String[] args) throws Exception {

        try {
            Map programMap = (Map) JPO.unpackArgs(args);

            String objectId = (String) programMap.get("objectId");
            String strType = (String) programMap.get("fromCommand");

            DomainObject domainObj = DomainObject.newInstance(context, objectId);
            StringList slObjSelectStmts = getULSRelationshipConstants();

            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern relationshipPatternForPCMData = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA);
            Pattern relationshipPatternForSubProgramProjects = new Pattern(TigerConstants.RELATIONSHIP_PSS_SUBPROGRAMPROJECT);

            String strRelPattern = relationshipPatternForPCMData.getPattern() + "," + relationshipPatternForSubProgramProjects.getPattern();

            Pattern typePatternForPCMData = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);
            Pattern typePatternForSubProgramProjects = new Pattern(TigerConstants.TYPE_PSS_PROGRAMPROJECT);

            String strTypePattern = typePatternForPCMData.getPattern() + "," + typePatternForSubProgramProjects.getPattern();
            MapList mlPCMDataList = new MapList();
            if (strType.equals("CR")) {
                Pattern typePostPattern = new Pattern(TigerConstants.TYPE_PSS_CHANGEREQUEST);

                MapList mlList = domainObj.getRelatedObjects(context, strRelPattern, // relationship pattern
                        strTypePattern, // object pattern
                        slObjSelectStmts, // object selects
                        slSelectRelStmts, // relationship selects
                        false, // to direction
                        true, // from direction
                        (short) 0, // recursion level
                        null, // object where clause
                        null, (short) 0, false, // checkHidden
                        true, // preventDuplicates
                        (short) 1000, // pageSize
                        typePostPattern, null, null, null, null);

                Iterator i = mlList.iterator();
                StringList slUniqueId = new StringList();

                while (i.hasNext()) {
                    Map mTempMap = (Map) i.next();
                    mTempMap.put("level", "1");
                    String strObjId = (String) mTempMap.get(DomainConstants.SELECT_ID);
                    if (!slUniqueId.contains(strObjId)) {
                        slUniqueId.addElement(strObjId);
                        mlPCMDataList.add(mTempMap);
                    }
                }
            }
            return mlPCMDataList;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getConnectedPCMDataForSLC: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw e;
        }
    }

    /**
     * Method to display list of Options available to filter Change Request objects
     * @param context
     * @param args
     * @return list of filters
     * @throws Exception
     */
    public StringList getCRFilterRangeData(Context context, String[] args) throws Exception {
        StringList filterCRList = new StringList();

        filterCRList.addElement("All");
        filterCRList.addElement("Active");
        filterCRList.addElement("Opened");
        filterCRList.addElement("Opened this month and not closed");
        filterCRList.addElement("In Process");
        filterCRList.addElement("Completed");
        filterCRList.addElement("Customer");
        // Findbug Issue correction start
        // Date: 21/03/2017
        // By: Asha G.

        // Findbug Issue correction End

        return filterCRList;
    }

    /**
     * Method to obtain count of Affected Items connected to the CR
     * @param context
     * @param args
     * @return count of Affected Items connected to the CR
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public Vector getRelatedAffectedItemsCount(Context context, String[] args) throws Exception {

        Vector vAffectedItemList = new Vector();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            MapList objIdList = (MapList) paramMap.get("objectList");
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

            for (int i = 0; i < objIdList.size(); i++) {
                StringBuffer sbFormAction = new StringBuffer();
                Map objectIds = (Map) objIdList.get(i);
                String strObjId = (String) objectIds.get(DomainConstants.SELECT_ID);
                BusinessObject busCR = new BusinessObject(strObjId);

                ExpansionIterator expIter = busCR.getExpansionIterator(context, TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM, DomainConstants.QUERY_WILDCARD, new StringList(DomainConstants.SELECT_ID),
                        new StringList(0), false, true, (short) 1, null, "", (short) 0, true, false, (short) 100, false);
                MapList mlAffectedItems = FrameworkUtil.toMapList(expIter, (short) 0, null, null, null, null);
                expIter.close();

                int intAffectedItemsCount = 0;
                // if (UIUtil.isNotNullAndNotEmpty(strAffectedItemCount))
                if (!mlAffectedItems.isEmpty())
                    // intAffectedItemsCount = Integer.parseInt(strAffectedItemCount);
                    intAffectedItemsCount = mlAffectedItems.size();
                // TIGTK-16067:16-07-2018:STARTS
                if (intAffectedItemsCount == 0) {
                    sbFormAction.append(intAffectedItemsCount);
                } else {
                    // TIGTK-16067:16-07-2018:ENDS
                    sbFormAction.append("<a onclick=\"showModalDialog('../common/emxTree.jsp?objectId=");
                    sbFormAction.append(strObjId);
                    sbFormAction.append("&amp;DefaultCategory=PSS_ECMChangeContent', 800, 600, true)\">");
                    sbFormAction.append(intAffectedItemsCount);
                    sbFormAction.append("");
                    sbFormAction.append("</a>");
                }
                vAffectedItemList.add(sbFormAction.toString());
            }
        } catch (Exception e) {
            logger.error("Error in getRelatedAffectedItemsCount: ", e);
            throw e;
        }
        return vAffectedItemList;
    }

    /**
     * Method to obtain count of Reference Documents connected to the CR
     * @param context
     * @param args
     * @return count of Refrence Documents connected to the CR
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Vector getRelatedReferenceDocumentsCount(Context context, String[] args) throws Exception {

        Vector vReferenceDocumentList = new Vector();
        try {
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            MapList objIdList = (MapList) paramMap.get("objectList");
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

            for (int i = 0; i < objIdList.size(); i++) {
                StringBuffer sbFormAction = new StringBuffer();
                Map objectIds = (Map) objIdList.get(i);
                String strObjId = (String) objectIds.get(DomainConstants.SELECT_ID);
                BusinessObject busCR = new BusinessObject(strObjId);

                ExpansionIterator expIter = busCR.getExpansionIterator(context, DomainConstants.RELATIONSHIP_REFERENCE_DOCUMENT, DomainConstants.QUERY_WILDCARD,
                        new StringList(DomainConstants.SELECT_ID), new StringList(0), false, true, (short) 1, null, "", (short) 0, true, false, (short) 100, false);
                MapList mlAffectedItems = FrameworkUtil.toMapList(expIter, (short) 0, null, null, null, null);
                expIter.close();

                int intReferenceDocumentCount = 0;
                if (!mlAffectedItems.isEmpty())
                    intReferenceDocumentCount = mlAffectedItems.size();
                // TIGTK-16067:16-07-2018:STARTS
                if (intReferenceDocumentCount == 0) {
                    sbFormAction.append(intReferenceDocumentCount);
                } else {
                    // TIGTK-16067:16-07-2018:ENDS
                    // Find Bug : Dodgy Code : Priyanka Salunke : 21- March-2017 : Start
                    sbFormAction.append("<a onclick=\"showModalDialog('../common/emxTree.jsp?objectId=");
                    sbFormAction.append(strObjId);
                    sbFormAction.append("&amp;DefaultCategory=PSS_ECMCOReferenceDocs', 800, 600, true)\">");
                    sbFormAction.append(intReferenceDocumentCount);
                    sbFormAction.append("");
                    sbFormAction.append("</a>");
                    // Find Bug : Dodgy Code : Priyanka Salunke : 21- March-2017 : End
                }
                vReferenceDocumentList.add(sbFormAction.toString());
            }
        } catch (Exception e) {
            logger.error("Error in getRelatedReferenceDocumentsCount: ", e);
            throw e;
        }
        return vReferenceDocumentList;
    }

    /**
     * Method to obtain list of Change Orders connected to the CR
     * @param context
     * @param args
     * @return count of COs connected to CR
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public Vector getRelatedCOCount(Context context, String[] args) throws Exception {
        try {
            Vector vConnectedCOsList = new Vector();
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            MapList objIdList = (MapList) paramMap.get("objectList");
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

            for (int i = 0; i < objIdList.size(); i++) {
                StringBuffer sbFormAction = new StringBuffer();
                Map objectIds = (Map) objIdList.get(i);
                String strObjId = (String) objectIds.get(DomainConstants.SELECT_ID);
                BusinessObject busCR = new BusinessObject(strObjId);

                ExpansionIterator expIter = busCR.getExpansionIterator(context, TigerConstants.RELATIONSHIP_CHANGEORDER, DomainConstants.QUERY_WILDCARD, new StringList(DomainConstants.SELECT_ID),
                        new StringList(0), false, true, (short) 1, null, "", (short) 0, true, false, (short) 100, false);
                MapList mlAffectedItems = FrameworkUtil.toMapList(expIter, (short) 0, null, null, null, null);
                expIter.close();

                int intCOCount = 0;
                if (!mlAffectedItems.isEmpty())
                    intCOCount = mlAffectedItems.size();

                // TIGTK-16067:16-07-2018:STARTS
                if (intCOCount == 0) {
                    sbFormAction.append(intCOCount);
                } else {
                    // TIGTK-16067:16-07-2018:ENDS
                    // Find Bug : Dodgy Code : Priyanka Salunke : 21- March-2017 : Start
                    sbFormAction.append("<a onclick=\"showModalDialog('../common/emxTree.jsp?objectId=");
                    sbFormAction.append(strObjId);
                    sbFormAction.append("&amp;DefaultCategory=PSS_ChangeRequestRelatedCOs', 800, 600, true)\">");
                    sbFormAction.append(intCOCount);
                    sbFormAction.append("");
                    sbFormAction.append("</a>");
                    // Find Bug : Dodgy Code : Priyanka Salunke : 21- March-2017 : End
                }
                vConnectedCOsList.add(sbFormAction.toString());
            }
            return vConnectedCOsList;
        } catch (Exception e) {
            logger.error("Error in getRelatedCOCount: ", e);
            throw e;
        }
    }

    /**
     * Method to obtain list of Change Orders connected to the CR
     * @param context
     * @param args
     * @return count of MCOs connected to CR
     * @throws Exception
     */
    @SuppressWarnings("rawtypes")
    public Vector getRelatedMCOCount(Context context, String[] args) throws Exception {
        try {
            Vector vConnectedMCOsList = new Vector();
            HashMap paramMap = (HashMap) JPO.unpackArgs(args);
            MapList objIdList = (MapList) paramMap.get("objectList");
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);

            for (int i = 0; i < objIdList.size(); i++) {
                StringBuffer sbFormAction = new StringBuffer();
                Map objectIds = (Map) objIdList.get(i);
                String strObjId = (String) objectIds.get(DomainConstants.SELECT_ID);
                BusinessObject busCR = new BusinessObject(strObjId);

                ExpansionIterator expIter = busCR.getExpansionIterator(context, TigerConstants.RELATIONSHIP_PSS_MANUFACTURINGCHANGEORDER, DomainConstants.QUERY_WILDCARD,
                        new StringList(DomainConstants.SELECT_ID), new StringList(0), false, true, (short) 1, null, "", (short) 0, true, false, (short) 100, false);
                MapList mlAffectedItems = FrameworkUtil.toMapList(expIter, (short) 0, null, null, null, null);
                expIter.close();

                int intMCOCount = 0;
                if (!mlAffectedItems.isEmpty())
                    intMCOCount = mlAffectedItems.size();
                // TIGTK-16067:16-07-2018:STARTS
                if (intMCOCount == 0) {
                    sbFormAction.append(intMCOCount);
                } else {
                    // TIGTK-16067:16-07-2018:ENDS
                    // Find Bug : Dodgy Code : Priyanka Salunke : 21- March-2017 : Start
                    sbFormAction.append("<a onclick=\"showModalDialog('../common/emxTree.jsp?objectId=");
                    sbFormAction.append(strObjId);
                    sbFormAction.append("&amp;DefaultCategory=PSS_ChangeRequestRelatedMCOs', 800, 600, true)\">");
                    sbFormAction.append(intMCOCount);
                    sbFormAction.append("");
                    sbFormAction.append("</a>");
                    // Find Bug : Dodgy Code : Priyanka Salunke : 21- March-2017 : End
                }
                vConnectedMCOsList.add(sbFormAction.toString());
            }
            return vConnectedMCOsList;
        } catch (Exception e) {
            logger.error("Error in getRelatedMCOCount: ", e);
            throw e;
        }
    }

    /**
     * Modified this method for TIGTK-10244 Method to promote or demote the Change Request object ,...using programHTMLOutput...on state cell hover
     * @param context
     * @param args
     * @return Vector containing programHtml
     * @throws Exception
     */
    public Vector<String> promoteDemoteOnStateCellHover(Context context, String[] args) throws Exception {
        try {
            Vector<String> vecResult = new Vector<String>();
            // Added for error found by find bug : 08/11/2016 : START
            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            // Added for error found by find bug : 08/11/2016 : END
            MapList objectList = (MapList) programMap.get("objectList");
            Map paramList = (Map) programMap.get("paramList");
            String parentOID = (String) paramList.get("parentOID");

            for (int i = 0; i < objectList.size(); i++) {
                StringBuilder str = new StringBuilder();
                Map<String, String> map = (Map<String, String>) objectList.get(i);

                String sCurrentState = (String) map.get("current");
                String objId = (String) map.get("id");

                String statusImageString = "";

                if (sCurrentState.equalsIgnoreCase("Create")) {
                    // PCM TIGTK-3961: 17/02/2017 : KWagh : START
                    statusImageString = "<div  onmouseover=\"document.getElementById('image_divp" + i + "').style.display = 'block';\" onmouseout=\"document.getElementById('image_divp" + i
                            + "').style.display = 'none';\"><table><tr><td>" + "Promote" + "</td><td><a style=\"display:none;\" href=\"#\" id=\"image_divp" + i + "\" onClick=\"promoteDemoteCR('"
                            + objId + "','" + parentOID
                            + "','blocked1')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionPromote.gif\" name=\"Promote\" id=\"Promote\" alt=\"myimage\"  title=\"Promote\"></img></a></td></tr></table></div>";

                } else if (sCurrentState.equalsIgnoreCase("In Process")) {
                    // PCM TIGTK-3961: 17/02/2017 : KWagh : START
                    statusImageString = "<div  onmouseover=\"document.getElementById('image_divp" + i + "').style.display = 'block';\" onmouseout=\"document.getElementById('image_divp" + i
                            + "').style.display = 'none';\"><table><tr><td>" + "Promote" + "</td><td><a style=\"display:none;\" href=\"#\" id=\"image_divp" + i + "\" onClick=\"promoteDemoteCR('"
                            + objId + "','" + parentOID
                            + "','Promote')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionPromote.gif\" name=\"Promote\" id=\"Promote\" alt=\"myimage\"  title=\"Promote\"></img></a></td></tr></table></div>";

                } else if (sCurrentState.equalsIgnoreCase("Submit")) {
                    statusImageString = "<div  onmouseover=\"document.getElementById('image_divp" + i + "').style.display = 'block';document.getElementById('image_divd" + i
                            + "').style.display = 'block';\" onmouseout=\"document.getElementById('image_divp" + i + "').style.display = 'none';document.getElementById('image_divd" + i
                            + "').style.display = 'none';\"><table><tr><td>" + "Promote/Demote" + "</td><td><a style=\"display:none;\" href=\"#\" id=\"image_divp" + i
                            + "\" onClick=\"promoteDemoteCR('" + objId + "','" + parentOID
                            + "','Promote')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionPromote.gif\" name=\"Promote\" id=\"Promote\" alt=\"myimage\"  title=\"Promote\"></img></a></td><td><a style=\"display:none;\" href=\"#\" id=\"image_divd"
                            + i + "\" onClick=\"promoteDemoteCR('" + objId + "','" + parentOID
                            + "','Demote')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionDemote.gif\" name=\"Demote\" id=\"Demote\" alt=\"myimage\"  title=\"Demote\"></img></a></td></tr></table></div>";

                } else if (sCurrentState.equalsIgnoreCase("Evaluate") || sCurrentState.equalsIgnoreCase("In Review")) {
                    statusImageString = "<div  onmouseover=\"document.getElementById('image_divp" + i + "').style.display = 'block';document.getElementById('image_divd" + i
                            + "').style.display = 'block';\" onmouseout=\"document.getElementById('image_divp" + i + "').style.display = 'none';document.getElementById('image_divd" + i
                            + "').style.display = 'none';\"><table><tr><td>" + "Promote/Demote" + "</td><td><a style=\"display:none;\" href=\"#\" id=\"image_divp" + i
                            + "\" onClick=\"promoteDemoteCR('" + objId + "','" + parentOID
                            + "','blocked2')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionPromote.gif\" name=\"Promote\" id=\"Promote\" alt=\"myimage\"  title=\"Promote\"></img></a></td><td><a style=\"display:none;\" href=\"#\" id=\"image_divd"
                            + i + "\" onClick=\"promoteDemoteCR('" + objId + "','" + parentOID
                            + "','Demote')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionDemote.gif\" name=\"Demote\" id=\"Demote\" alt=\"myimage\"  title=\"Demote\"></img></a></td></tr></table></div>";
                } else if (sCurrentState.equalsIgnoreCase("Complete") || sCurrentState.equalsIgnoreCase("Rejected")) {
                    statusImageString = "";
                }

                /*
                 * else if (sCurrentState.equalsIgnoreCase("In Review")) {
                 * 
                 * statusImageString = "<div  onmouseover=\"document.getElementById('image_divp" + i + "').style.display = 'block';document.getElementById('image_divd" + i +
                 * "').style.display = 'block';\" onmouseout=\"document.getElementById('image_divp" + i + "').style.display = 'none';document.getElementById('image_divd" + i +
                 * "').style.display = 'none';\"><table><tr><td>" + "Demote" + "</td><td><a style=\"display:none;\" href=\"#\" id=\"image_divp" + i + "\" onClick=\"promoteDemoteCR('" + objId + "','" +
                 * parentOID +
                 * "','Demote')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionDemote.gif\" name=\"Demote\" id=\"Demote\" alt=\"myimage\"  title=\"Demote\"></img></a></td></tr></table></div>"
                 * ;
                 * 
                 * } else if (sCurrentState.equalsIgnoreCase("In Process")) { statusImageString = "<div  onmouseover=\"document.getElementById('image_divp" + i +
                 * "').style.display = 'block';document.getElementById('image_divd" + i + "').style.display = 'block';\" onmouseout=\"document.getElementById('image_divp" + i +
                 * "').style.display = 'none';document.getElementById('image_divd" + i + "').style.display = 'none';\"><table><tr><td>" + "Promote/Demote" +
                 * "</td><td><a style=\"display:none;\" href=\"#\" id=\"image_divp" + i + "\" onClick=\"promoteDemoteCR('" + objId +
                 * "','Promote')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionPromote.gif\" name=\"Promote\" id=\"Promote\" alt=\"myimage\"  title=\"Promote\"></img></a></td><td><a style=\"display:none;\" href=\"#\" id=\"image_divd"
                 * + i + "\" onClick=\"promoteDemoteCR('" + objId +
                 * "','Demote')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionDemote.gif\" name=\"promote\" id=\"Demote\" alt=\"myimage\"  title=\"Demote\"></img></a></td></tr></table></div>"
                 * ;
                 * 
                 * }
                 */ else if (sCurrentState.equalsIgnoreCase("Complete") || sCurrentState.equalsIgnoreCase("Rejected")) {
                    statusImageString = "";

                    /*
                     * statusImageString = "<div  onmouseover=\"document.getElementById('image_divp" + i + "').style.display = 'block';document.getElementById('image_divd" + i +
                     * "').style.display = 'block';\" onmouseout=\"document.getElementById('image_divp" + i + "').style.display = 'none';document.getElementById('image_divd" + i +
                     * "').style.display = 'none';\"><table><tr><td>" + "Promote/Demote" + "</td><td><a style=\"display:none;\" href=\"#\" id=\"image_divp" + i + "\" onClick=\"promoteDemoteCR('" +
                     * objId +
                     * "','blocked4')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionPromote.gif\" name=\"Promote\" id=\"Promote\" alt=\"myimage\"  title=\"Promote\"></img></a></td><td><a style=\"display:none;\" href=\"#\" id=\"image_divd"
                     * + i + "\" onClick=\"promoteDemoteCR('" + objId +
                     * "','Demote')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionDemote.gif\" name=\"promote\" id=\"Demote\" alt=\"myimage\"  title=\"Demote\"></img></a></td></tr></table></div>"
                     * ;
                     */
                    // SLC : TIGTK-3928 : 19/01/2017 : AB : START
                } /*
                   * else if (sCurrentState.equalsIgnoreCase("Rejected")) { statusImageString = "<div  onmouseover=\"document.getElementById('image_divp" + i +
                   * "').style.display = 'block';document.getElementById('image_divd" + i + "').style.display = 'block';\" onmouseout=\"document.getElementById('image_divp" + i +
                   * "').style.display = 'none';document.getElementById('image_divd" + i + "').style.display = 'none';\"><table><tr><td>" + "Promote/Demote" +
                   * "</td><td><a style=\"display:none;\" href=\"#\" id=\"image_divp" + i + "\" onClick=\"promoteDemoteCR('" + objId +
                   * "','blocked2')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionPromote.gif\" name=\"Promote\" id=\"Promote\" alt=\"myimage\"  title=\"Promote\"></img></a></td><td><a style=\"display:none;\" href=\"#\" id=\"image_divd"
                   * + i + "\" onClick=\"promoteDemoteCR('" + objId +
                   * "','Demote')\"><img ALIGN=\"right\" border=\"0\" src=\"../common/images/iconActionDemote.gif\" name=\"promote\" id=\"Demote\" alt=\"myimage\"  title=\"Demote\"></img></a></td></tr></table></div>"
                   * ;
                   * 
                   * }
                   */
                // SLC : TIGTK-3928 : 19/01/2017 : AB : END
                // PCM TIGTK-3961: 17/02/2017 : KWagh : End
                str.append("");
                str.append(statusImageString);
                vecResult.add(str.toString());
            }

            return vecResult;

        } catch (Exception ex) {
            throw ex;
        }
    }

    public void connectProgramProject(Context context, String[] args) throws Exception {

        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        HashMap paramMap = (HashMap) programMap.get("paramMap");
        String strChangeRequestOID = (String) paramMap.get("objectId");
        String strProgramProjectName = (String) paramMap.get("New Value");
        String strRelID = (String) paramMap.get("relId");

        StringList objectSelect = new StringList();
        objectSelect.add(DomainObject.SELECT_ID);

        try {
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectName)) {

                DomainRelationship.disconnect(context, strRelID);

                MapList mlProgramProjectObjectsList = DomainObject.findObjects(context, TigerConstants.TYPE_PSS_PROGRAMPROJECT, strProgramProjectName, "", "*", TigerConstants.VAULT_ESERVICEPRODUCTION,
                        null, false, objectSelect);

                Map mPrj = (Map) mlProgramProjectObjectsList.get(0);
                String strProgProjId = (String) mPrj.get(DomainObject.SELECT_ID);

                DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgProjId);
                DomainObject domChangeRequestObject = DomainObject.newInstance(context, strChangeRequestOID);
                String strType = (String) domChangeRequestObject.getInfo(context, DomainConstants.SELECT_TYPE);
                String strProgramProjectCurrent = (String) domProgramProjectObject.getInfo(context, DomainConstants.SELECT_CURRENT);

                if (strType.equalsIgnoreCase(TigerConstants.TYPE_PSS_CHANGEREQUEST)) {
                    domChangeRequestObject.setAttributeValue(context, "PSS_Project_Phase_at_Creation", strProgramProjectCurrent);
                }
                ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);

                DomainRelationship.connect(context, domProgramProjectObject, TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA, domChangeRequestObject);
                ContextUtil.popContext(context);
            }
        } catch (Exception ex) {
            throw ex;
        }
    }

    public void connectChangeManager(Context context, String[] args) throws Exception {

        try {
            // unpacking the Arguments from variable args
            final String TYPE_PERSON = PropertyUtil.getSchemaProperty(context, "type_Person");

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String strChangeRequestOID = (String) paramMap.get("objectId");
            String strNewChangeManagerName = (String) paramMap.get("New Value");
            DomainObject domChangeRequestObject = DomainObject.newInstance(context, strChangeRequestOID);
            StringList slselectObjStmts = new StringList();
            StringList slselectRelStmts = new StringList();
            slselectObjStmts.addElement(DomainConstants.SELECT_ID);
            slselectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern relationship_pattern = new Pattern(TigerConstants.RELATIONSHIP_CHANGECOORDINATOR);
            Pattern type_pattern = new Pattern(TYPE_PERSON);
            MapList mapList = domChangeRequestObject.getRelatedObjects(context, relationship_pattern.getPattern(), // relationship pattern
                    type_pattern.getPattern(), // object pattern
                    slselectObjStmts, // object selects
                    slselectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);
            if (!mapList.isEmpty()) {
                Map map = (Map) mapList.get(0);
                String strCMRelId = (String) map.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                DomainRelationship.disconnect(context, strCMRelId);
            }
            StringList objectSelect = new StringList();
            objectSelect.add(DomainObject.SELECT_ID);
            MapList mlPersonList = DomainObject.findObjects(context, type_pattern.getPattern(), strNewChangeManagerName, "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION, null, false, objectSelect);
            Map mPerson = (Map) mlPersonList.get(0);
            String strChangeManagerOID = (String) mPerson.get(DomainObject.SELECT_ID);
            DomainObject domPersonObject = DomainObject.newInstance(context, strChangeManagerOID);
            DomainRelationship.connect(context, domChangeRequestObject, ChangeConstants.RELATIONSHIP_CHANGE_COORDINATOR, domPersonObject);
        }

        catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in connectChangeManager: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }

    }

    public void updateRelatedIssue(Context context, String[] args) throws Exception {

        try {
            // unpacking the Arguments from variable args

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            String strChangeRequestOID = (String) paramMap.get("objectId");
            String strNewIssueName = (String) paramMap.get("New Value");
            DomainObject domChangeRequestObject = DomainObject.newInstance(context, strChangeRequestOID);
            StringList slselectObjStmts = new StringList();
            StringList slselectRelStmts = new StringList();
            slselectObjStmts.addElement(DomainConstants.SELECT_ID);
            slselectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);

            Pattern relationship_pattern = new Pattern(TigerConstants.RELATIONSHIP_ISSUE);
            Pattern type_pattern = new Pattern(TigerConstants.TYPE_ISSUE);
            MapList mapList = domChangeRequestObject.getRelatedObjects(context, relationship_pattern.getPattern(), // relationship pattern
                    type_pattern.getPattern(), // object pattern
                    slselectObjStmts, // object selects
                    slselectRelStmts, // relationship selects
                    false, // to direction
                    true, // from direction
                    (short) 1, // recursion level
                    null, // object where clause
                    null, (short) 0, false, // checkHidden
                    true, // preventDuplicates
                    (short) 1000, // pageSize
                    null, // postPattern
                    null, null, null);
            if (!mapList.isEmpty()) {
                Map map = (Map) mapList.get(0);
                String strIssueRelId = (String) map.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                DomainRelationship.disconnect(context, strIssueRelId);
            }
            StringList objectSelect = new StringList();
            objectSelect.add(DomainObject.SELECT_ID);
            MapList mlIssueList = DomainObject.findObjects(context, type_pattern.getPattern(), strNewIssueName, "*", "*", TigerConstants.VAULT_ESERVICEPRODUCTION, null, false, objectSelect);
            Map mIssue = (Map) mlIssueList.get(0);
            String strNewIssueOID = (String) mIssue.get(DomainObject.SELECT_ID);
            DomainObject domIssueObject = DomainObject.newInstance(context, strNewIssueOID);
            DomainRelationship.connect(context, domChangeRequestObject, TigerConstants.RELATIONSHIP_ISSUE, domIssueObject);
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in updateRelatedIssue: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
            throw ex;
        }

    }

    /**
     * This is used as Edit Access Function for CR's different columns in Program-Project SLC based on its related conditions.
     * @param context
     * @param args
     * @return StringList contains boolean value which will decide weather cell is editable or not.
     * @throws Exception
     * @modified for : TIGTK-16801 : 29-08-2018
     */
    public static StringList isCREditatableFromSLC(Context context, String[] args) throws Exception { // Called from table FRCMBOMTable (column Description)
        logger.debug("\n pss.slc.ui.SLCUIUtil:isCREditatableFromSLC:END");
        StringList slReturn = new StringList();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            // TIGTK-16801 : 29-08-2018 : START
            HashMap columnMap = (HashMap) programMap.get("columnMap");
            HashMap settings = (HashMap) columnMap.get("settings");
            // Create and Submit state related functioning
            String strCreateOrSubmitStateColumn = (String) settings.get("ColumnForCreateOrSubmitState");
            // Parallel Track Comment related functioning
            String strParallelTrackCommentColumn = (String) settings.get("ColumnIsParallelTrackComment");
            // Customer Agreement Date elated functioning
            String strCustomerAgreementDateColumn = (String) settings.get("ColumnIsCustomerAgreementDate");
            // Create state related functioning
            String strColumnForCreateState = (String) settings.get("ColumnForCreateState");

            StringList slStateListForCustomerAgreenmentDate = new StringList(TigerConstants.STATE_PSS_CR_CREATE);
            slStateListForCustomerAgreenmentDate.addElement(TigerConstants.STATE_SUBMIT_CR);
            slStateListForCustomerAgreenmentDate.addElement(TigerConstants.STATE_EVALUATE);
            slStateListForCustomerAgreenmentDate.addElement(TigerConstants.STATE_INREVIEW_CR);
            // TIGTK-16801 : 29-08-2018 : END

            MapList mlRelBusObjPageList = (MapList) programMap.get("objectList");
            for (int i = 0; i < mlRelBusObjPageList.size(); i++) {
                Map mpObjectData = (Map) mlRelBusObjPageList.get(i);
                String strCurrent = (String) mpObjectData.get("current");

                // TIGTK-16801 : 28-08-2018 : START
                // User with PM / CM or Admin roles
                boolean bAdminOrPMCMEditAllow = (boolean) mpObjectData.get("bAdminOrPMCMEditAllow");

                boolean isCREditAllowInSLC = false;

                if (bAdminOrPMCMEditAllow) {
                    // Parallel Track Comment related check
                    if (UIUtil.isNotNullAndNotEmpty(strParallelTrackCommentColumn)) {
                        if (strParallelTrackCommentColumn.equalsIgnoreCase("ParallelTrackComment")) {
                            String strCRType = (String) mpObjectData.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_CRTYPE + "]");
                            if ((strCurrent.equalsIgnoreCase(TigerConstants.STATE_SUBMIT_CR)) && strCRType.equalsIgnoreCase(TigerConstants.ENGINEERING_CR)) {
                                isCREditAllowInSLC = true;
                            }
                        }
                    }
                    // Customer Agreement Date related check
                    else if (UIUtil.isNotNullAndNotEmpty(strCustomerAgreementDateColumn)) {
                        if (strCustomerAgreementDateColumn.equalsIgnoreCase("CustomerAgreementDate")) {
                            if (slStateListForCustomerAgreenmentDate.contains(strCurrent)) {
                                isCREditAllowInSLC = true;
                            }
                        }
                    }
                    // Create and Submit state related check
                    else if (UIUtil.isNotNullAndNotEmpty(strCreateOrSubmitStateColumn)) {
                        if (strCreateOrSubmitStateColumn.equalsIgnoreCase("CreateOrSubmitState")) {
                            if (strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_CR_CREATE) || strCurrent.equalsIgnoreCase(TigerConstants.STATE_SUBMIT_CR)) {
                                isCREditAllowInSLC = true;
                            }
                        }
                    }
                    // Create state related check
                    else if (UIUtil.isNotNullAndNotEmpty(strColumnForCreateState)) {
                        if (strColumnForCreateState.equalsIgnoreCase("CreateState")) {
                            if (strCurrent.equalsIgnoreCase(TigerConstants.STATE_PSS_CR_CREATE)) {
                                isCREditAllowInSLC = true;
                            }
                        }
                    }

                    // Gave access when context user is PM/CM of related PP or Admin
                    else {
                        isCREditAllowInSLC = true;
                    }
                }

                // Based on condition check result allow Edit
                if (isCREditAllowInSLC) {
                    slReturn.addElement("true");
                } else {
                    slReturn.addElement("false");
                }
            }
            // TIGTK-16801 : 28-08-2018 : END
            logger.debug("\n pss.slc.ui.SLCUIUtil:isCREditatableFromSLC:END");
        } catch (Exception ex) {
            logger.error("\n pss.slc.ui.SLCUIUtil:isCREditatableFromSLC:ERROR ", ex);
            throw ex;
        }
        return slReturn;
    }
    // isCREditatableFromSLC : END

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Vector getDecisionProgram(Context context, String[] args) throws Exception {

        final String STRING_GO = "Go";
        final String STRING_NO_GO = "No Go";
        final String STRING_COMPLETE = "Complete";
        final String STRING_BLANK = " ";

        Vector vDecisionProgramList = new Vector();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        MapList objIdList = (MapList) paramMap.get("objectList");

        for (int i = 0; i < objIdList.size(); i++) {
            StringBuffer sbFormAction = new StringBuffer();
            Map objectIds = (Map) objIdList.get(i);
            String strObjCurrent = (String) objectIds.get(DomainConstants.SELECT_CURRENT);
            String strDecisionValue = "";
            if (strObjCurrent.equalsIgnoreCase("In Process")) {
                strDecisionValue = STRING_GO;
            } else if (strObjCurrent.equalsIgnoreCase("Rejected")) {
                strDecisionValue = STRING_NO_GO;
            } else if (strObjCurrent.equalsIgnoreCase("Complete")) {
                strDecisionValue = STRING_COMPLETE;
            } else {
                strDecisionValue = STRING_BLANK;
            }
            // Find Bug : Dodgy Code : Priyanka Salunke : 21-March-2017 : Start
            sbFormAction.append("<p>");
            sbFormAction.append(strDecisionValue);
            sbFormAction.append("");
            sbFormAction.append("</p>");
            // Find Bug : Dodgy Code : Priyanka Salunke : 21-March-2017 : End
            vDecisionProgramList.add(sbFormAction.toString());
        }

        return vDecisionProgramList;
    }

    @SuppressWarnings("rawtypes")
    public StringList updateProgProjOnCreate(Context context, String[] args) throws Exception {

        StringList slProgProgName = new StringList();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) paramMap.get("requestMap");
        String strProgProjId = (String) requestMap.get("objectId");
        DomainObject domObjProgProj = DomainObject.newInstance(context, strProgProjId);
        String strName = (String) domObjProgProj.getInfo(context, DomainConstants.SELECT_NAME);
        slProgProgName.add(strName);
        return slProgProgName;
    }

    @SuppressWarnings("rawtypes")
    public StringList updateProgProjDescriptionOnCreate(Context context, String[] args) throws Exception {

        StringList slProgProgDesc = new StringList();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) paramMap.get("requestMap");
        String strProgProjId = (String) requestMap.get("objectId");
        DomainObject domObjProgProj = DomainObject.newInstance(context, strProgProjId);
        String strDesc = (String) domObjProgProj.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
        slProgProgDesc.add(strDesc);
        return slProgProgDesc;
    }

    public StringList updateProgProjCurrentOnCreate(Context context, String[] args) throws Exception {

        StringList slProgProgCurrent = new StringList();
        HashMap paramMap = (HashMap) JPO.unpackArgs(args);
        HashMap requestMap = (HashMap) paramMap.get("requestMap");
        String strProgProjId = (String) requestMap.get("objectId");
        DomainObject domObjProgProj = DomainObject.newInstance(context, strProgProjId);
        String strCurrent = (String) domObjProgProj.getInfo(context, DomainConstants.SELECT_CURRENT);
        slProgProgCurrent.add(strCurrent);
        return slProgProgCurrent;
    }

    public StringList getConnectedChangeCoordObjsToProgProj(Context context, String[] args) throws Exception {
        StringList lstReturn = new StringList();
        try {
            final String SELECT_ATTRIBUTE_PSS_ROLE = "attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]";
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            String strProgramProjectOID = (String) programMap.get("objectId");
            DomainObject domProgramProjectObject = DomainObject.newInstance(context, strProgramProjectOID);
            StringList objSelect = new StringList();
            objSelect.add(DomainConstants.SELECT_ID);
            objSelect.add(DomainConstants.SELECT_NAME);
            StringList relSelect = new StringList();
            relSelect.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            relSelect.add(SELECT_ATTRIBUTE_PSS_ROLE);
            // TIGTK-5890 : PTE : 4/7/2017 : START
            String strRelWhereclause = " attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "] == \"" + TigerConstants.ROLE_PSS_CHANGE_COORDINATOR + "\"";
            // TIGTK-5890 : PTE : 4/7/2017 : END
            MapList mlConnectedChangeCoordMembersList = domProgramProjectObject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, TYPE_PERSON, objSelect, relSelect, false,
                    true, (short) 0, null, strRelWhereclause, 0);
            for (int i = 0; i < mlConnectedChangeCoordMembersList.size(); i++) {
                Map mapConnectedChangeCoordMember = (Map) mlConnectedChangeCoordMembersList.get(i);
                String strChangeCoordOID = (String) mapConnectedChangeCoordMember.get(DomainConstants.SELECT_ID);

                lstReturn.add(strChangeCoordOID);
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in getConnectedChangeCoordObjsToProgProj: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return lstReturn;
    }

    /**
     * @param
     * @description this method is used to give edit access to the user having Role "Change Manager" on context user
     * @throws Exception
     */
    public static int isAccess(Context context, String strProgProj, String contextUser) throws Exception {

        int flag = 0;

        try {
            DomainObject domProgramproject = DomainObject.newInstance(context, strProgProj);
            StringList slObjSelectStmts = new StringList();
            slObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            slObjSelectStmts.addElement(DomainConstants.SELECT_NAME);
            StringList slSelectRelStmts = new StringList(1);
            slSelectRelStmts.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
            slSelectRelStmts.addElement("attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "]");
            // TIGTK-5890 : PTE : 4/7/2017 : START
            String strWhere = "attribute[" + TigerConstants.ATTRIBUTE_PSS_ROLE + "] == " + "\"" + TigerConstants.ROLE_PSS_CHANGE_COORDINATOR + "\"";
            // TIGTK-5890 : PTE : 4/7/2017 : END

            MapList mlPerson = domProgramproject.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS, "Person", slObjSelectStmts, slSelectRelStmts, false, true, (short) 0,
                    null, strWhere);
            Iterator j = mlPerson.iterator();

            while (j.hasNext()) {
                Map mTempMap = (Map) j.next();
                if (mTempMap.containsValue(contextUser)) {
                    flag = 1;
                }
            }
        } catch (Exception ex) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in isAccess: ", ex);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return flag;
    }

    /**
     * This function is called on PSS_SLCChangeRequest Table. It is used to populated "Impact Analysis" Route of Related CR Object.
     * @param context
     * @param args
     * @return
     * @throws Exception
     * @Modified By : Priyanka Salunke : 20-March-2017
     */
    public StringList getConnectedImpactAnalysisInfoToCR(Context context, String args[]) throws Exception {
        StringList slReturn = new StringList();
        try {
            Map programMap = JPO.unpackArgs(args);

            MapList mlObjectList = (MapList) programMap.get("objectList");

            if (!mlObjectList.isEmpty()) {
                for (int i = 0; i < mlObjectList.size(); i++) {
                    Map mapObject = (Map) mlObjectList.get(i);
                    String strImpactId = (String) mapObject.get("LatestRevIAObjectId");
                    if (UIUtil.isNotNullAndNotEmpty(strImpactId)) {
                        DomainObject domIAObject = DomainObject.newInstance(context, strImpactId);
                        String strImpactName = domIAObject.getInfo(context, DomainConstants.SELECT_NAME);

                        StringBuffer sbOutputBuffer = new StringBuffer();
                        sbOutputBuffer.append("<a href=\"javaScript:emxTableColumnLinkClick('");
                        sbOutputBuffer.append("../common/emxTree.jsp?DefaultCategory=PSS_ImpactAnalysisRelatedRA&amp;objectId=");
                        sbOutputBuffer.append(XSSUtil.encodeForHTMLAttribute(context, strImpactId));
                        sbOutputBuffer.append("', '700', '600', 'false', 'popup', '')\">");
                        sbOutputBuffer.append(" ");
                        sbOutputBuffer.append(XSSUtil.encodeForHTMLAttribute(context, strImpactName));
                        sbOutputBuffer.append("</a>");

                        if (sbOutputBuffer.toString().length() > 0) {
                            slReturn.add(sbOutputBuffer.toString());
                        } else {
                            slReturn.add(" ");
                        }
                    } else
                        slReturn.add(" ");
                }
            }
        } catch (Exception ex) {
            logger.error("Error in getConnectedRouteTaskInfoToCR: ", ex);
            throw ex;
        }
        return slReturn;

    } // End of method getConnectedRouteTaskInfoToCR

    /**
     * Description : This method is used in PostProcess of Creation CR from SLC view.
     * @param args
     * @return
     * @throws Exception
     * @Created By : AB : TIGTK-8733 : 23/06/2017
     */
    public void setProjectPhaseAtCreationOnCreationOfCRFromSLC(Context context, String args[]) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap requestMap = (HashMap) programMap.get("requestMap");
            HashMap paramMap = (HashMap) programMap.get("paramMap");

            // get the Change Request id and Program-Project current state
            String strChangeRequestOID = (String) paramMap.get("objectId");
            String strProgramProjectPhaseAtCreation = (String) requestMap.get("Project Phase at Creation");
            String strProgramProjectId = (String) requestMap.get("parentOID");
            String strChangeType = (String) requestMap.get("TypeActual");
            DomainObject domChangeRequest = DomainObject.newInstance(context, strChangeRequestOID);

            // Set "Project Phase At Creation" attribute on CR
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectPhaseAtCreation) && TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strChangeType)) {
                domChangeRequest.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_PROJECT_PHASE_AT_CREATION, strProgramProjectPhaseAtCreation);
                updateCSOfChangeObject(context, strProgramProjectId, strChangeRequestOID);

            }
        } catch (Exception ex) {
            logger.error("Error in setProjectPhaseAtCreationOnCreationOfCRFromSLC: ", ex);
            throw ex;
        }
    }

    // PCM2.0 Spr4:TIGTK-6891:12/9/2017:START
    /**
     * @Description This method is invoked via Edit Access Function on Billable field in SLC Table View
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public StringList isBillableFieldEditable(Context context, String[] args) throws Exception {
        logger.debug("pss.slc.ui.SLCUIUtil:isBillableFieldEditable:START");
        StringList slReturn = new StringList();
        int flag = 0;
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList mlCRObjectList = (MapList) programMap.get("objectList");

            StringList slStateList = new StringList(TigerConstants.STATE_PSS_CR_CREATE);
            slStateList.add(TigerConstants.STATE_SUBMIT_CR);
            slStateList.add(TigerConstants.STATE_EVALUATE);
            slStateList.add(TigerConstants.STATE_INREVIEW_CR);

            Iterator<Map> itrCRObject = mlCRObjectList.iterator();
            while (itrCRObject.hasNext()) {
                Map<?, ?> mpCRObject = (Map<?, ?>) itrCRObject.next();

                String sCRId = (String) mpCRObject.get(DomainConstants.SELECT_ID);
                DomainObject domCRObject = DomainObject.newInstance(context, sCRId);
                String strCurrent = domCRObject.getInfo(context, DomainConstants.SELECT_CURRENT);
                String strOwner = (String) domCRObject.getInfo(context, DomainConstants.SELECT_OWNER);
                BusinessObject busObjCR = new BusinessObject(sCRId);

                boolean bAccess = busObjCR.checkAccess(context, (short) Access.cModify);
                String contextUser = context.getUser();
                String strProgProj = (String) mpCRObject.get("id[parent]");
                flag = isAccess(context, strProgProj, contextUser);

                if ((flag == 1 || (strOwner.equalsIgnoreCase(contextUser))) && slStateList.contains(strCurrent) && (bAccess == true)) {
                    slReturn.add(true);
                } else {
                    slReturn.add(false);
                }

            }

        } catch (Exception ex) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:isBillableFieldEditable:ERROR ", ex);
            throw ex;
        }
        return slReturn;
    }

    // PCM2.0 Spr4:TIGTK-6891:12/9/2017:END
    // PCM2.0 Spr4:TIGTK-6894:13/9/2017:START
    /**
     * @Description This method is used as to show the count of Program-Project connected to Affected Items.On clicking the count value,table shows the list of Governing and Impacted Program-Projects
     *              for Affected Item.
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getAIRelatedProjectCounts(Context context, String[] args) throws Exception {
        logger.debug("pss.slc.ui.SLCUIUtil:getAIRelatedProjectCounts:START");
        Vector vProjectCountList = new Vector();
        try {

            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList mlCRObjectIdList = (MapList) programMap.get("objectList");
            String strMode = "CRSLCView";

            int nCRCount = 0;
            Iterator itrCRObjectList = mlCRObjectIdList.iterator();
            while (itrCRObjectList.hasNext()) {

                Map mpCRObject = (Map) itrCRObjectList.next();
                String strCRObjectId = (String) mpCRObject.get(DomainConstants.SELECT_ID);

                DomainObject domCRObject = DomainObject.newInstance(context, strCRObjectId);
                StringList slAffectedItemList = domCRObject.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_AFFECTEDITEM + "].to.id");

                StringBuffer sbProjectIDList = new StringBuffer();
                StringBuffer sbProjectRelIDList = new StringBuffer();
                int nProjectCountPerCR = 0;

                Iterator itrAffectedItemList = slAffectedItemList.iterator();
                while (itrAffectedItemList.hasNext()) {
                    String strAffectedItemObjectId = (String) itrAffectedItemList.next();
                    DomainObject domAffectedItemObject = DomainObject.newInstance(context, strAffectedItemObjectId);

                    pss.ecm.MultiProgramChange_mxJPO multiProjramObj = new pss.ecm.MultiProgramChange_mxJPO();
                    MapList mlConnectedProgramProjects = multiProjramObj.getConnectedProjectFromAffectedItem(context, domAffectedItemObject, DomainObject.EMPTY_STRING, 0);
                    // PCM2.0 TIGTK-10418: 9/10/2017:START
                    if (mlConnectedProgramProjects != null) {
                        // PCM2.0 TIGTK-10418: 9/10/2017:END
                        int intProgramProjectCount = mlConnectedProgramProjects.size();
                        for (int cntProg = 0; cntProg < intProgramProjectCount; cntProg++) {
                            Map mProgProj = (Map) mlConnectedProgramProjects.get(cntProg);
                            String strProgramProjectId = (String) mProgProj.get("id");
                            // RE:TIGTK-6897:19/9/2017:Start
                            String strRelId = (String) mProgProj.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                            sbProjectRelIDList.append(strRelId);
                            sbProjectRelIDList.append(",");
                            // RE:TIGTK-6897:19/9/2017:End
                            sbProjectIDList.append(strProgramProjectId);
                            sbProjectIDList.append(",");
                            nProjectCountPerCR++;
                        }
                    }
                }

                StringBuffer sbFormAction = new StringBuffer();
                // TIGTK-16067:16-07-2018:STARTS
                if (nProjectCountPerCR == 0) {
                    sbFormAction.append(nProjectCountPerCR);
                } else {
                    // TIGTK-16067:16-07-2018:ENDS
                    sbFormAction.append("<input id='projectInput");
                    sbFormAction.append(nCRCount);
                    sbFormAction.append("' type='hidden' value='");
                    sbFormAction.append(sbProjectIDList.toString());
                    // RE:TIGTK-6897:19/9/2017:Start
                    sbFormAction.append("'/><input id='projectRelInput");
                    sbFormAction.append(nCRCount);
                    sbFormAction.append("' type='hidden' value='");
                    sbFormAction.append(sbProjectRelIDList.toString());
                    // RE:TIGTK-6897:19/9/2017:End
                    sbFormAction.append("'/><a onclick=\"showModalDialog('../enterprisechangemgt/PSS_ECMAffectedProjectSummary.jsp?selectedRowNo=");
                    sbFormAction.append(nCRCount);
                    sbFormAction.append("&amp;mode=");
                    sbFormAction.append(strMode);
                    // RE:TIGTK-6897:19/9/2017:Start
                    sbFormAction.append("&amp;objectId=");
                    sbFormAction.append(strCRObjectId);
                    // RE:TIGTK-6897:19/9/2017:End

                    sbFormAction.append("&amp;program=PSS_enoECMChangeUtil:getProjectList', 800, 600, true)\">");
                    sbFormAction.append(nProjectCountPerCR);
                    sbFormAction.append("</a>");
                }
                vProjectCountList.add(sbFormAction.toString());
                nCRCount++;
            }

        } catch (Exception ex) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:getAIRelatedProjectCounts:ERROR ", ex);
            throw ex;
        }
        return vProjectCountList;
    }

    // PCM2.0 Spr4:TIGTK-6894:13/9/2017:END

    /**
     * @Description This method is used to Get Issue connected to CR on SLC View
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector<String> getRelatedIssue(Context context, String[] args) throws Exception {
        logger.debug("pss.slc.ui.SLCUIUtil:getRelatedIssue:START");
        Vector<String> vtReturn = new Vector<String>();
        try {
            Map<?, ?> programMap = JPO.unpackArgs(args);
            MapList mplCR = (MapList) programMap.get("objectList");

            for (Object objCR : mplCR) {
                StringBuilder sbObj = new StringBuilder();
                String strIssueId = DomainConstants.EMPTY_STRING;

                Map<?, ?> mpCRObj = (Map<?, ?>) objCR;
                String strCRID = (String) mpCRObj.get(DomainConstants.SELECT_ID);
                DomainObject domCRId = DomainObject.newInstance(context, strCRID);
                StringList slIssueList = domCRId.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_PSS_GOVERNINGISSUE + "].from.id");

                if (!slIssueList.isEmpty()) {
                    Iterator<?> itr = slIssueList.iterator();
                    while (itr.hasNext()) {
                        strIssueId = (String) itr.next();
                        DomainObject domIssueId = DomainObject.newInstance(context, strIssueId);
                        String strIssueName = domIssueId.getInfo(context, DomainConstants.SELECT_NAME);

                        sbObj.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                        sbObj.append(strIssueId);
                        sbObj.append("', '860', '520', 'false', 'popup')\">");
                        sbObj.append(" " + strIssueName);
                        sbObj.append("</a>");

                        if (itr.hasNext())
                            sbObj.append("<br/>");
                    }
                }
                vtReturn.add(sbObj.toString());
            }
            logger.debug("pss.slc.ui.SLCUIUtil:getRelatedIssue:END");

        } catch (Exception ex) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:getRelatedIssue:ERROR ", ex);
            throw ex;
        }
        return vtReturn;
    }

    // RE:TIGTK-6897:19/9/2017:Start
    /***
     * this method called from PSS_ProjectInfoTable table column Affected Item for checking visibility
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public boolean showAffectedItemColumn(Context context, String[] args) throws Exception {
        logger.debug("pss.slc.ui.SLCUIUtil:showAffectedItemColumn :Start");
        boolean isReturn = false;
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String strObjectId = (String) programMap.get("ChangeObjectId");
        if (UIUtil.isNotNullAndNotEmpty(strObjectId)) {
            DomainObject domChangeObj = DomainObject.newInstance(context, strObjectId);
            String strChangeType = domChangeObj.getInfo(context, DomainConstants.SELECT_TYPE);
            if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strChangeType)) {
                isReturn = true;
            }
        }
        logger.debug("isReturn : " + isReturn);
        logger.debug("pss.slc.ui.SLCUIUtil:showAffectedItemColumn :End");
        return isReturn;
    }

    /***
     * this method used on Affected Item column of PSS_ProjectInfoTable table
     * @param context
     * @param args
     * @return Vector
     * @throws Exception
     */
    public Vector<String> getAffectedItemOnRelatedProject(Context context, String[] args) throws Exception {
        logger.debug("pss.slc.ui.SLCUIUtil:getAffectedItemOnRelatedProject :Start");
        Vector<String> vAffectedItem = new Vector<String>();
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramList = (HashMap) programMap.get("paramList");
            String strProjectRelList = (String) paramList.get("projectRelList");
            if (UIUtil.isNotNullAndNotEmpty(strProjectRelList)) {
                String[] strRelId = strProjectRelList.split(",");
                StringList slRelSelect = new StringList();
                slRelSelect.add("to.name");
                slRelSelect.add("to.id");
                MapList mlAffectedItem = DomainRelationship.getInfo(context, strRelId, slRelSelect);

                for (Object obj : mlAffectedItem) {
                    Map mAffectedItem = (Map) obj;
                    StringBuffer output = new StringBuffer(" ");
                    output.append("<tr><td width=\"60%\">");
                    output.append("<a href=\"javascript:emxTableColumnLinkClick('../common/emxTree.jsp?objectId=");
                    output.append((String) mAffectedItem.get("to.id"));
                    output.append("', '700', '600', 'false', 'insert', '')\" class=\"object\">");
                    output.append(" " + (String) mAffectedItem.get("to.name"));
                    output.append("</a></td>");
                    output.append("</tr>");
                    vAffectedItem.add(output.toString());
                }
            }
        } catch (Exception e) {
            logger.error("ERROR in pss.slc.ui.SLCUIUtil:getAffectedItemOnRelatedProject : " + e.getMessage());
        }
        logger.debug("pss.slc.ui.SLCUIUtil:getAffectedItemOnRelatedProject :End");
        return vAffectedItem;
    }
    // RE:TIGTK-6897:19/9/2017:End

    /**
     * @Description This method is used to Show Edit access for state Create, Submit, Evaluate and In Review in SLC View
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public static StringList isOwnerAndActiveState(Context context, String[] args) throws Exception { // Called from table FRCMBOMTable (column Description)
        logger.debug("pss.slc.ui.SLCUIUtil:isOwnerAndActiveState:START");
        StringList slReturn = new StringList();
        int flag = 0;
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList mlCRObjectList = (MapList) programMap.get("objectList");

            StringList slStateList = new StringList(TigerConstants.STATE_PSS_CR_CREATE);
            slStateList.add(TigerConstants.STATE_SUBMIT_CR);
            slStateList.add(TigerConstants.STATE_EVALUATE);
            slStateList.add(TigerConstants.STATE_INREVIEW_CR);

            Iterator<Map> itrCRObject = mlCRObjectList.iterator();
            while (itrCRObject.hasNext()) {
                Map<?, ?> mpCRObject = (Map<?, ?>) itrCRObject.next();

                String sCRId = (String) mpCRObject.get(DomainConstants.SELECT_ID);
                DomainObject domCRObject = DomainObject.newInstance(context, sCRId);
                String strCurrent = domCRObject.getInfo(context, DomainConstants.SELECT_CURRENT);

                String strOwner = (String) domCRObject.getInfo(context, DomainConstants.SELECT_OWNER);
                String contextUser = context.getUser();

                String strProgProj = (String) mpCRObject.get("id[parent]");
                flag = isAccess(context, strProgProj, contextUser);

                if (slStateList.contains(strCurrent) && (flag == 1 || strOwner.equalsIgnoreCase(contextUser))) {
                    slReturn.add(true);
                } else {
                    slReturn.add(false);
                }
            }
            logger.debug("pss.slc.ui.SLCUIUtil:isOwnerAndActiveState :End");
        } catch (Exception ex) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:isOwnerAndActiveState:ERROR ", ex);
            throw ex;
        }
        return slReturn;
    }

    /**
     * @Description This method is invoked via Edit Access Function on Parallel Track field in SLC FAS Table View
     * @param context
     * @param args
     * @return StringList
     * @throws Exception
     */
    public StringList isParallelTrackFieldEditable(Context context, String[] args) throws Exception {
        logger.debug("pss.slc.ui.SLCUIUtil:isBillableFieldEditable:START");
        StringList slReturn = new StringList();
        int flag = 0;
        try {

            HashMap<?, ?> programMap = (HashMap<?, ?>) JPO.unpackArgs(args);
            MapList mlCRObjectList = (MapList) programMap.get("objectList");

            StringList slStateList = new StringList(TigerConstants.STATE_PSS_CR_CREATE);
            slStateList.add(TigerConstants.STATE_SUBMIT_CR);

            Iterator<Map> itrCRObject = mlCRObjectList.iterator();
            while (itrCRObject.hasNext()) {
                Map<?, ?> mpCRObject = (Map<?, ?>) itrCRObject.next();

                String sCRId = (String) mpCRObject.get(DomainConstants.SELECT_ID);
                DomainObject domCRObject = DomainObject.newInstance(context, sCRId);
                String strCRType = domCRObject.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRTYPE);
                String strCurrent = domCRObject.getInfo(context, DomainConstants.SELECT_CURRENT);
                String strOwner = (String) domCRObject.getInfo(context, DomainConstants.SELECT_OWNER);
                String contextUser = context.getUser();

                String strProgProj = (String) mpCRObject.get("id[parent]");
                flag = isAccess(context, strProgProj, contextUser);

                if (slStateList.contains(strCurrent) && (flag == 1 || strOwner.equalsIgnoreCase(contextUser))
                        && !(TigerConstants.MANUFACTURING_CR.equals(strCRType) || TigerConstants.PROGRAM_CR.equals(strCRType))) {
                    slReturn.add(true);
                } else {
                    slReturn.add(false);
                }
            }

        } catch (Exception ex) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:isBillableFieldEditable:ERROR ", ex);
            throw ex;
        }
        return slReturn;
    }

    /**
     * TIGTK-13922 : This is a generic function use to get SLC Table attribute values by reading "Attribute Name" column setting
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector getSLCTableAttributeValue(Context context, String args[]) throws Exception {

        Vector attrValuesVector = new Vector();
        try {
            long lStartTime = System.nanoTime();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap columnMap = (HashMap) programMap.get("columnMap");
            HashMap settings = (HashMap) columnMap.get("settings");
            String strAdminType = (String) settings.get("Attribute Name");
            MapList objectList = (MapList) programMap.get("objectList");
            boolean keyExists = false;
            if (objectList.size() > 0) {
                Map infoMap = (Map) objectList.get(0);
                keyExists = infoMap.containsKey("attribute[" + strAdminType + "]");
            }
            if (keyExists) {
                Iterator iterator = objectList.iterator();

                String strAttrValue;
                Map objectMap;
                while (iterator.hasNext()) {
                    objectMap = (Map) iterator.next();

                    strAttrValue = getStringValue(objectMap, "attribute[" + strAdminType + "]");
                    attrValuesVector.addElement(strAttrValue);
                }
            }
            long lEndTime = System.nanoTime();
            long output = lEndTime - lStartTime;

            logger.debug("Total Execution time to Get Attribute " + strAdminType + " Value in milliseconds: " + output / 1000000);
        } catch (Exception e) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:getSLCTableAttributeValue: ", e);
            e.printStackTrace();
            throw e;
        }

        return attrValuesVector;
    }

    private String getStringValue(Map map, String key) {
        String strReturn = "";
        try {
            Object objValue = map.get(key);
            if (objValue != null) {
                if (objValue.getClass().equals(String.class))
                    strReturn = (String) map.get(key);
                else {
                    strReturn = FrameworkUtil.join((StringList) objValue, ", ");
                }
            }
        } catch (Exception e) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:getStringValue: ", e);
            throw e;
        }
        return strReturn;
    }

    /**
     * TIGTK-13922 : This is a generic function use to get SLC Table attribute values by reading "Attribute Name" column setting
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Vector getSLCTableRelAttributeValue(Context context, String args[]) throws Exception {
        Vector attrValuesVector = new Vector();

        try {
            long lStartTime = System.nanoTime();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap columnMap = (HashMap) programMap.get("columnMap");
            HashMap settings = (HashMap) columnMap.get("settings");
            String strSelectExpression = (String) settings.get("Select Expression");
            MapList objectList = (MapList) programMap.get("objectList");

            boolean keyExists = false;

            if (objectList.size() > 0) {
                Map infoMap = (Map) objectList.get(0);
                keyExists = infoMap.containsKey(strSelectExpression);
            }

            if (keyExists) {
                Iterator iterator = objectList.iterator();

                String strAttrValue;
                Map objectMap;
                while (iterator.hasNext()) {
                    objectMap = (Map) iterator.next();
                    strAttrValue = getStringValue(objectMap, strSelectExpression);
                    attrValuesVector.addElement(strAttrValue);
                }
            }
            long lEndTime = System.nanoTime();
            long output = lEndTime - lStartTime;

            logger.debug("Total Execution time to Get " + strSelectExpression + " Value in milliseconds: " + output / 1000000);
        } catch (Exception e) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:getSLCTableAttributeValue: ", e);
            throw e;
        }

        return attrValuesVector;
    }

    /**
     * TIGTK-13922 : This is a generic function use to update SLC Table attribute values
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Boolean updateSLCTableAttributeValue(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");
            HashMap columnMap = (HashMap) programMap.get("columnMap");
            HashMap settings = (HashMap) columnMap.get("settings");
            String strAdminType = (String) settings.get("Attribute Name");
            String strFormat = (String) settings.get("format");

            String objectId = (String) paramMap.get("objectId");
            String newValue = (String) paramMap.get("New Value");
            DomainObject domObj = new DomainObject(objectId);

            HashMap requestMap = (HashMap) programMap.get("requestMap");
            Locale lLocale = (Locale) requestMap.get("locale");
            if (strFormat.equalsIgnoreCase("date"))
                newValue = getDateInMatrixDateFormat(newValue, lLocale);
            if (TigerConstants.ATTRIBUTE_PSS_CRTITLE.equals(strAdminType) && UIUtil.isNullOrEmpty(newValue)) {

                String strCRName = domObj.getInfo(context, DomainConstants.SELECT_NAME);
                domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CRTITLE, strCRName);

            } else {
                domObj.setAttributeValue(context, strAdminType, newValue);
            }
        } catch (Exception e) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:updateSLCTableAttributeValue: ", e);
            throw e;
        }
        return Boolean.TRUE;
    }

    public String getDateInMatrixDateFormat(String strLocalDate, Locale lLocale) {
        String strDateInMatrixFormat = DomainConstants.EMPTY_STRING;
        try {
            // get matrix date format
            SimpleDateFormat _mxDateFormat = new SimpleDateFormat(eMatrixDateFormat.getEMatrixDateFormat(), Locale.US);
            // get local date format according to browser language used
            String strlocalDateFormat = ((java.text.SimpleDateFormat) java.text.DateFormat.getDateInstance(eMatrixDateFormat.getEMatrixDisplayDateFormat(), lLocale)).toPattern();
            SimpleDateFormat sdlocalDateDormat = new SimpleDateFormat(strlocalDateFormat, lLocale);
            // parse date according to local date format
            Date date = sdlocalDateDormat.parse(strLocalDate);
            // format date in matrix date format
            strDateInMatrixFormat = _mxDateFormat.format(date);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("Error in getDateInMatrixDateFormat: ", e);
        }
        return strDateInMatrixFormat;
    }

    /**
     * TIGTK-13922 : This is a generic function use to update SLC Table Description values
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public Boolean updateSLCTableDescriptionValue(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap paramMap = (HashMap) programMap.get("paramMap");

            String objectId = (String) paramMap.get("objectId");
            String newValue = (String) paramMap.get("New Value");
            DomainObject domObj = new DomainObject(objectId);

            domObj.setDescription(context, newValue);
        } catch (Exception e) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:updateSLCTableDescriptionValue: ", e);
            throw e;
        }
        return Boolean.TRUE;
    }

    /**
     * TIGTK-13922 : This is a generic function use to get SLC Table attribute range values
     * @param context
     * @param args
     * @return
     * @throws Exception
     */
    public HashMap displayRangeValues(Context context, String[] args) throws Exception {
        HashMap rangeMap = new HashMap();
        try {
            String strLanguage = context.getSession().getLanguage();
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            HashMap columnMap = (HashMap) programMap.get("columnMap");
            HashMap settings = (HashMap) columnMap.get("settings");
            String strAdminType = (String) settings.get("Attribute Name");
            StringList strRangeValues = FrameworkUtil.getRanges(context, strAdminType);

            StringList listChoices = new StringList();
            StringList listDispChoices = new StringList();

            String attrValue = "";
            String dispValue = "";

            for (int i = 0; i < strRangeValues.size(); i++) {
                attrValue = (String) strRangeValues.get(i);
                if (UIUtil.isNotNullAndNotEmpty(attrValue) && !attrValue.equals("None")) {
                    dispValue = i18nNow.getRangeI18NString(strAdminType, attrValue, strLanguage);
                    listDispChoices.add(dispValue);
                    listChoices.add(attrValue);
                }
            }

            rangeMap.put("field_choices", listChoices);
            rangeMap.put("field_display_choices", listDispChoices);
        } catch (Exception e) {
            logger.error("Error in pss.slc.ui.SLCUIUtil:displayRangeValues: ", e);
            throw e;
        }

        return rangeMap;
    }

    /**
     * This method is used to update the Project(CS) of Change Object as per selected Program Projects Project(CS)
     * @param context
     * @param args,Program
     *            Project Object Id , Change Object Id
     * @throws Exception
     * @Since 07-02-2018 : TIGTK-13112
     * @author psalunke
     */
    public void updateCSOfChangeObject(Context context, String strProgProjId, String strChangeObjectID) throws Exception {
        logger.debug("PSS_enoECMChangeRequest : updateCSOfChangeObject : START");
        try {
            if (UIUtil.isNotNullAndNotEmpty(strProgProjId) && UIUtil.isNotNullAndNotEmpty(strChangeObjectID)) {
                BusinessObject busProgProjObj = new BusinessObject(strProgProjId);
                User usrProgProjProjectName = busProgProjObj.getProjectOwner(context);
                String strProgProjProjectName = usrProgProjProjectName.toString();
                BusinessObject busChangeObj = new BusinessObject(strChangeObjectID);
                User usrChangeProjectName = busChangeObj.getProjectOwner(context);
                String strChangeObjectProjectName = usrChangeProjectName.toString();
                if (!strProgProjProjectName.equalsIgnoreCase(strChangeObjectProjectName)) {
                    MqlUtil.mqlCommand(context, "history off", true, false);
                    boolean isContextPushed = false;
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, TigerConstants.PERSON_USER_AGENT), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    isContextPushed = true;
                    busChangeObj.open(context);
                    busChangeObj.setProjectOwner(context, strProgProjProjectName);
                    busChangeObj.update(context);
                    busChangeObj.close(context);
                    if (isContextPushed) {
                        ContextUtil.popContext(context);
                        isContextPushed = false;
                    }
                    MqlUtil.mqlCommand(context, "history on", true, false);

                    String strMqlHistory = "modify bus $1 add history $2 comment $3";

                    StringBuffer sbInfo = new StringBuffer();
                    sbInfo.append("project: ");
                    sbInfo.append(strProgProjProjectName);

                    MqlUtil.mqlCommand(context, strMqlHistory, strChangeObjectID, "change", sbInfo.toString() + " was " + strChangeObjectProjectName);

                }
            }
            logger.debug("PSS_enoECMChangeRequest : updateCSOfChangeObject : END");
        } catch (Exception ex) {
            logger.error("Error in PSS_enoECMChangeRequest : updateCSOfChangeObject : ERROR ", ex);
            throw ex;
        }
    }

    /**
     * Description : This method is used to display Program-Project SLC Category Commands
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     * @since 16-07-2018 : TIGTK-16050
     * @author psalunke
     */
    public boolean showCommandsInSLC(Context context, String[] args) throws Exception {
        logger.debug("\n pss.slc.ui.SLCUIUtil:showCommandsInSLC:START");
        boolean hasAccess = false;
        try {
            Map<?, ?> programMap = (Map<?, ?>) JPO.unpackArgs(args);
            String strProgramProjectId = (String) programMap.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strProgramProjectId)) {
                DomainObject domProgProj = DomainObject.newInstance(context, strProgramProjectId);
                StringList slPPSelects = new StringList();
                slPPSelects.addElement(DomainConstants.SELECT_OWNER);
                slPPSelects.addElement("project");
				// TIGTK - 18225 : stembulkar : start
                slPPSelects.addElement(DomainConstants.SELECT_CURRENT);
				// TIGTK - 18225 : stembulkar : end
                Map mpProgramProjectInfo = domProgProj.getInfo(context, slPPSelects);
                String strProgramProjectOwner = (String) mpProgramProjectInfo.get(DomainConstants.SELECT_OWNER);
                String strProgramProjectCS = (String) mpProgramProjectInfo.get("project");
				// TIGTK - 18225 : stembulkar : start
                String strProgramProjectCurrentState = (String) mpProgramProjectInfo.get( DomainConstants.SELECT_CURRENT );
				if( !strProgramProjectCurrentState.equalsIgnoreCase( "Active" ) ) {
				// TIGTK - 18225 : stembulkar : end
					String strLoggedInUser = context.getUser();
					String strDefaultContext = PersonUtil.getDefaultProject(context, strLoggedInUser);
					if (PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || PersonUtil.hasAssignment(context, TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM)
							|| strProgramProjectOwner.equalsIgnoreCase(strLoggedInUser)) {
						hasAccess = true;
					} else if (strDefaultContext.equalsIgnoreCase(strProgramProjectCS)) {

						String strPositionSelect = DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_PSS_POSITION);
						// Relationship Selects
						StringList slRelSelect = new StringList(2);
						slRelSelect.addElement(DomainConstants.SELECT_RELATIONSHIP_ID);
						slRelSelect.addElement(strPositionSelect);
						// Relationship Pattern
						Pattern relationshipPattern = new Pattern(TigerConstants.RELATIONSHIP_PSS_CONNECTEDMEMBERS);
						// Type Pattern
						Pattern typePattern = new Pattern(TYPE_PERSON);
						Pattern typePostPattern = new Pattern(TYPE_PERSON);

						String strPersonId = PersonUtil.getPersonObjectID(context, strLoggedInUser);

						// Object Where
						StringBuffer sbObjectWhere = new StringBuffer();
						sbObjectWhere.append(DomainConstants.SELECT_ID);
						sbObjectWhere.append(" == \"");
						sbObjectWhere.append(strPersonId);
						sbObjectWhere.append("\"");

						MapList mlConnectedMemebersOfPP = domProgProj.getRelatedObjects(context, relationshipPattern.getPattern(), // relationship pattern
								typePattern.getPattern(), // object pattern
								new StringList(DomainConstants.SELECT_ID), // object selects
								slRelSelect, // relationship selects
								false, // to direction
								true, // from direction
								(short) 1, // recursion level
								sbObjectWhere.toString(), // object where clause
								DomainConstants.EMPTY_STRING, // relationship where clause
								(short) 0, false, // checkHidden
								true, // preventDuplicates
								(short) 1000, // pageSize
								typePostPattern, null, null, null, null);
						if (mlConnectedMemebersOfPP != null && !mlConnectedMemebersOfPP.isEmpty()) {
							hasAccess = true;
						}
					}
				// TIGTK - 18225 : stembulkar : start
				}
				// TIGTK - 18225 : stembulkar : end
            }
            logger.debug("\n pss.slc.ui.SLCUIUtil:showCommandsInSLC:END");
        } catch (Exception ex) {
            logger.error("\n pss.slc.ui.SLCUIUtil:showCommandsInSLC:ERROR ", ex);
            throw ex;
        }
        return hasAccess;
    }

    /**
     * Description : This method is used gave edit access to CR's in Program-Project SLC category if user is Admin or Program-Projects CM/PM
     * @param context
     * @param Program-Project
     *            Object Id
     * @return boolean
     * @throws Exception
     * @since 30-08-2018 : TIGTK-16801
     * @author psalunke
     */
    public boolean isAdminOrPMCMofRelatedPP(Context context, String strProgProj) throws Exception {
        logger.debug("\n pss.slc.ui.SLCUIUtil:isAdminOrPMCMofRelatedPP:START");
        boolean bAdminOrPMCMEditAllow = false;
        try {
            if (UIUtil.isNotNullAndNotEmpty(strProgProj)) {
                DomainObject domProgramProject = DomainObject.newInstance(context, strProgProj);
                StringBuffer relWhere = new StringBuffer();
                relWhere.append("(attribute[");
                relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
                relWhere.append("]");
                relWhere.append(" == '");
                relWhere.append(TigerConstants.ROLE_PSS_CHANGE_COORDINATOR);
                relWhere.append("' || attribute[");
                relWhere.append(TigerConstants.ATTRIBUTE_PSS_ROLE);
                relWhere.append("]");
                relWhere.append(" == '");
                relWhere.append(TigerConstants.ROLE_PSS_PROGRAM_MANAGER);
                relWhere.append("')");
                pss.ecm.enoECMChange_mxJPO enoECMChange = new pss.ecm.enoECMChange_mxJPO();
                MapList mlPMCMofConextPP = enoECMChange.getMembersFromProgram(context, domProgramProject, new StringList(DomainConstants.SELECT_NAME), DomainConstants.EMPTY_STRINGLIST,
                        DomainConstants.EMPTY_STRING, relWhere.toString());
                StringList slContextPPPMCM = new StringList();
                if (mlPMCMofConextPP != null && !mlPMCMofConextPP.isEmpty()) {
                    for (int j = 0; j < mlPMCMofConextPP.size(); j++) {
                        Map mpPMCMofPP = (Map) mlPMCMofConextPP.get(j);
                        String strPMorCMName = (String) mpPMCMofPP.get(DomainObject.SELECT_NAME);
                        slContextPPPMCM.addElement(strPMorCMName);
                    }
                }
                String contextUser = context.getUser();
                String strLoggedUserSecurityContext = PersonUtil.getDefaultSecurityContext(context, contextUser);
                String strAssignedRole = (strLoggedUserSecurityContext.split("[.]")[0]);
                // check context user have Admins roles or context user is PM/CM of context PRogram-Project then allow edit in SLC
                if ((strAssignedRole.equalsIgnoreCase(TigerConstants.ROLE_PSS_GLOBAL_ADMINISTRATOR) || strAssignedRole.equalsIgnoreCase(TigerConstants.ROLE_PSS_PLM_SUPPORT_TEAM))
                        || (slContextPPPMCM.contains(contextUser))) {
                    bAdminOrPMCMEditAllow = true;
                }
            }
            logger.debug("\n pss.slc.ui.SLCUIUtil:isAdminOrPMCMofRelatedPP:END");
        } catch (Exception ex) {
            logger.error("\n pss.slc.ui.SLCUIUtil:isAdminOrPMCMofRelatedPP:ERROR ", ex);
            throw ex;
        }
        return bAdminOrPMCMEditAllow;
    }

}