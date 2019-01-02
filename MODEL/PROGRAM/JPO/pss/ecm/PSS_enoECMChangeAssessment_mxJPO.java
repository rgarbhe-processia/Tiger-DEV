package pss.ecm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeManagement;
import com.dassault_systemes.enovia.enterprisechangemgt.util.ChangeUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class PSS_enoECMChangeAssessment_mxJPO extends DomainObject {

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_enoECMChangeAssessment_mxJPO.class);
    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END

    String ChildItems = "Child Items";

    String ParentItems = "Parent Items";

    String ChildAndRelated = "Child and Related Items";

    String ParentAndRelated = "Parent and Related Items";

    String RelatedItem = "Related Item";

    public static final String RELATIONSHIP_CANDIDATE_AFFECTED_ITEM = PropertyUtil.getSchemaProperty("relationship_CandidateAffectedItem");

    public static final String VAULT = "eService Production";

    /**
     * Constructs a new ChangeAssessmentBase JPO object.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     */

    public PSS_enoECMChangeAssessment_mxJPO(Context context, String[] args) throws Exception {
        super();
    }

    /**
     * Getting the Child/ Parent from the selected item
     * @param context
     * @param args
     * @return MapList of Child/ Parent
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getChangeAssessmentItems(Context context, String[] args) throws Exception {
        MapList mlResult = new MapList();
        MapList mlFinal = new MapList();
        try {
            HashMap hmParamMap = (HashMap) JPO.unpackArgs(args);
            Map tempmap;
            String[] arrTableRowIds = new String[1];
            String strTableRowID = (String) hmParamMap.get("emxTableRowId");
            arrTableRowIds[0] = strTableRowID;
            ChangeUtil changeUtil = new ChangeUtil();
            StringList slObjectIds = changeUtil.getAffectedItemsIds(context, arrTableRowIds);
            ChangeManagement ChangeManagement = new ChangeManagement();
            MapList mlOutput = ChangeManagement.getChangeAssessment(context, slObjectIds);

            // For Charted Drawing PTE

            StringList lstselectStmts = new StringList(1);
            lstselectStmts.addElement(DomainConstants.SELECT_ID);

            StringList lstrelStmts = new StringList();
            lstrelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

            String strChangeAssessmentId = (String) slObjectIds.get(0);
            DomainObject domChangeAssessment = DomainObject.newInstance(context, strChangeAssessmentId);

            Pattern typePatternForChartedDrawing = new Pattern(DomainConstants.EMPTY_STRING);
            Pattern RelPatternForChartedDrawing = new Pattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);

            Boolean bIsFromForChartedDrawing = false;
            Boolean bIsToForChartedDrawing = false;

            if (domChangeAssessment.isKindOf(context, DomainConstants.TYPE_PART)) {
                typePatternForChartedDrawing.addPattern(DomainConstants.TYPE_CAD_DRAWING);
                typePatternForChartedDrawing.addPattern(DomainConstants.TYPE_CAD_MODEL);
                bIsFromForChartedDrawing = true;
            } else {
                typePatternForChartedDrawing.addPattern(DomainConstants.TYPE_PART);
                bIsToForChartedDrawing = true;
            }

            MapList mlChartedDrawingObjects = domChangeAssessment.getRelatedObjects(context, RelPatternForChartedDrawing.getPattern(), typePatternForChartedDrawing.getPattern(), lstselectStmts,
                    lstrelStmts, bIsToForChartedDrawing, bIsFromForChartedDrawing, (short) 0, null, null, 0);

            // Associate Drawing PTE
            Pattern typePatternForAssociatedDrawing = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
            Pattern RelPatternForAssociatedDrawing = new Pattern(TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING);

            Boolean bIsFromForAssociatedDrawing = false;
            Boolean bIsToForAssociatedDrawing = false;
            MapList mlAssociatedDrawingObjects = new MapList();
            if (!domChangeAssessment.isKindOf(context, DomainConstants.TYPE_PART)) {
                if (domChangeAssessment.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING) || domChangeAssessment.isKindOf(context, DomainConstants.TYPE_CAD_MODEL)) {
                    typePatternForAssociatedDrawing.addPattern(DomainConstants.TYPE_CAD_DRAWING);
                    bIsFromForAssociatedDrawing = true;

                }
                if (domChangeAssessment.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {
                    bIsToForAssociatedDrawing = true;
                    typePatternForAssociatedDrawing.addPattern(DomainConstants.TYPE_CAD_MODEL);
                }
                mlAssociatedDrawingObjects = domChangeAssessment.getRelatedObjects(context, RelPatternForAssociatedDrawing.getPattern(), typePatternForAssociatedDrawing.getPattern(), lstselectStmts,
                        lstrelStmts, bIsToForAssociatedDrawing, bIsFromForAssociatedDrawing, (short) 1, null, null, 0);

            }
            mlChartedDrawingObjects.addAll(mlAssociatedDrawingObjects);

            Iterator itr = mlChartedDrawingObjects.iterator();
            while (itr.hasNext()) {
                Map mpObj = (Map) itr.next();
                String strRelationshipName = (String) mpObj.get("relationship");
                String strObjID = (String) mpObj.get(DomainConstants.SELECT_ID);
                DomainObject domObj = DomainObject.newInstance(context, strObjID);
                if (TigerConstants.RELATIONSHIP_ASSOCIATEDDRAWING.equals(strRelationshipName) && !domObj.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {
                    mpObj.put("strLabel", "Parent CAD Parts");
                } else {
                    mpObj.put("strLabel", "");
                }

                mlOutput.add(mpObj);

            }

            // PCM : TIGTK-4086 : 31/01/2017 : AB : START
            // Get the ObjectId of Change Object and check whether it is ChangeOrder or not
            String strContextChangeId = (String) hmParamMap.get("contextCOId");
            DomainObject domChange = new DomainObject(strContextChangeId);
            String strTypeOfCHange = domChange.getInfo(context, DomainConstants.SELECT_TYPE);
            int size = mlOutput.size();
            if (TigerConstants.TYPE_PSS_CHANGEORDER.equalsIgnoreCase(strTypeOfCHange) && !mlOutput.isEmpty()) {
                for (int i = 0; i < size; i++) {
                    Map mapItemInfo = (Map) mlOutput.get(i);
                    String strItemID = (String) mapItemInfo.get(DomainConstants.SELECT_ID);
                    DomainObject domItem = new DomainObject(strItemID);
                    String strItemPolicy = domItem.getInfo(context, DomainConstants.SELECT_POLICY);
                    // PCM :TIGTK-6351 :4/12/2017 : PTE
                    String strCurrentState = domItem.getInfo(context, DomainConstants.SELECT_CURRENT);
                    // PCM :TIGTK-6351 :4/12/2017 : PTE End
                    // Remove Development Part for add Affected item in CO from Change Assessment
                    if (TigerConstants.POLICY_PSS_DEVELOPMENTPART.equalsIgnoreCase(strItemPolicy) || strCurrentState.equals(DomainConstants.STATE_PART_OBSOLETE)) {

                        // PCM :TIGTK-4798 :3/11/2017 : PTE Start
                        mlResult.add(mapItemInfo);
                        // PCM :TIGTK-4798 :3/11/2017 : PTE End
                    }
                }
                // PCM :TIGTK-4798 :3/11/2017 : PTE Start
                for (int i = 0; i < size; i++) {
                    Map mapItemInfo = (Map) mlOutput.get(i);
                    if (!mlResult.contains(mapItemInfo)) {
                        mlFinal.add(mapItemInfo);
                    }
                }
                // PCM :TIGTK-4798 :3/11/2017 : PTE End
            } // PCM :TIGTK-4694 :16/3/2017 :Start
            else if (TigerConstants.TYPE_PSS_CHANGEREQUEST.equalsIgnoreCase(strTypeOfCHange) && !mlOutput.isEmpty()) {
                for (int i = 0; i < mlOutput.size(); i++) {
                    Map mapItemInfo = (Map) mlOutput.get(i);
                    String strItemID = (String) mapItemInfo.get(DomainConstants.SELECT_ID);
                    DomainObject domItem = new DomainObject(strItemID);
                    String strItemState = domItem.getInfo(context, DomainConstants.SELECT_CURRENT);
                    // Remove obsolete and approved Part for add Affected item in CR from Change Assessment
                    if (DomainConstants.STATE_PART_APPROVED.equalsIgnoreCase(strItemState) || DomainConstants.STATE_PART_OBSOLETE.equalsIgnoreCase(strItemState)) {
                        // PCM :TIGTK-4798 :3/11/2017 : PTE Start
                        mlResult.add(mapItemInfo);
                        // PCM :TIGTK-4798 :3/11/2017 : PTE End
                    }
                }
                // PCM :TIGTK-4798 :3/11/2017 : PTE Start
                for (int i = 0; i < size; i++) {
                    Map mapItemInfo = (Map) mlOutput.get(i);
                    if (!mlResult.contains(mapItemInfo)) {
                        mlFinal.add(mapItemInfo);
                    }
                }
                // PCM :TIGTK-4798 :3/11/2017 : PTE Start
            }
            // PCM :TIGTK-4694 :16/3/2017 :End
            // PCM : TIGTK-4086 : 31/01/2017 : AB : END

        } catch (Exception Ex) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in getChangeAssessmentItems: ", Ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw Ex;
        }
        return mlFinal;
    }

    public StringList displayColorForAIInChangeAssessment(Context context, String[] args) throws Exception {

        try {
            StringList slStyles = new StringList();
            // Get object list information from packed arguments
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            MapList objectList = (MapList) programMap.get("objectList");
            HashMap paramList = (HashMap) programMap.get("paramList");
            String strContextChangeObjectId = (String) paramList.get("contextCOId");
            DomainObject domContextChangeObjectId = DomainObject.newInstance(context, strContextChangeObjectId);
            String strProjectId = domContextChangeObjectId.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDPCMDATA + "].from.id");

            for (Iterator itrTableRows = objectList.iterator(); itrTableRows.hasNext();) {
                StringBuffer sbBuffer = new StringBuffer();
                Map mapObjectInfo = (Map) itrTableRows.next();
                String strPartObjectId = (String) mapObjectInfo.get(DomainConstants.SELECT_ID);
                DomainObject domPartObject = DomainObject.newInstance(context, strPartObjectId);

                String strPartName = domPartObject.getInfo(context, DomainConstants.SELECT_NAME);
                String strGoverningProjectID = domPartObject.getInfo(context, "to[" + TigerConstants.RELATIONSHIP_PSS_CONNECTEDGOVERNINGPROJECT + "].from.id");
                // START :: Fix for issue identied while testing ALM-5809 implementation :: Check for CR as well
                if ((domContextChangeObjectId.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEORDER) || domContextChangeObjectId.isKindOf(context, TigerConstants.TYPE_PSS_CHANGEREQUEST))
                        && UIUtil.isNotNullAndNotEmpty(strGoverningProjectID) && !strProjectId.equals(strGoverningProjectID)) {
                    // END :: Fix for issue identied while testing ALM-5809 implementation :: Check for CR as well
                    sbBuffer.append("<p style=\"color:red;\">");
                    sbBuffer.append(strPartName);
                    sbBuffer.append("</p>");
                } else {
                    sbBuffer.append(strPartName);
                }
                slStyles.addElement(sbBuffer.toString());

            }
            return slStyles;

        } catch (Exception Ex) {
            logger.error("Error in displayColorForAIInChangeAssessment: ", Ex);
            throw Ex;
        }

    }
}
