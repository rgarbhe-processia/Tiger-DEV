/*
 ** CAD -XCAD Module By SteepGraph System Purpose: propagate the Charted Drawing to new Rev from last Rev Date: 29-01-2016
 */
package pss.cad2d3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

import com.dassault_systemes.enovia.enterprisechangemgt.common.ChangeConstants;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.utils.MCADUtil;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.UOMUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UIUtil;
import com.matrixone.json.JSONArray;
import com.matrixone.json.JSONObject;

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
import matrix.db.Relationship;
import matrix.db.RelationshipType;
import matrix.db.User;
import matrix.util.MatrixException;
import matrix.util.Pattern;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class DECTGUtil_mxJPO {

    public DECTGUtil_mxJPO() {

        // constructor
    }

    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DECTGUtil_mxJPO.class);
    // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - END

    // modification for TGPSS_CAD-TS-004-Add/remove CAD Representation start
    public static final String SUITE_KEY = "emxIEFDesignCenterStringResource";

    public static final String RELATIONSHIP_CHANGEAFFECTEDITEM = PropertyUtil.getSchemaProperty("relationship_ChangeAffectedItem");

    // modification for TGPSS_CAD-TS-004-Add/remove CAD Representation start

    // modification for TGPSS_CAD-US-003-Create Revision Manually start
    public static final String REGEX_POLICY_PSS_CADOBJECT = "[-,A-H,J-N,P-Z][A-H,J-N,P-Z]";

    public static final String POLICY_VERSIONEDDESIGNPOLICY = PropertyUtil.getSchemaProperty("policy_VersionedDesignPolicy");

    public static final String ATTR_ISVERSIONOBJECT = PropertyUtil.getSchemaProperty("attribute_IsVersionObject");

    // modification for TGPSS_CAD-US-003-Create Revision Manually end

    /**
     * This method will check for Geometry Type of Alternate Representation connected to Part
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int checkGeometryTypeForConnectedPartSpecification(Context context, String[] args) throws Exception {
        int iReturn = 0;
        boolean bFlag = false;
        String strObjectID = args[0];// Get the Object Id
        String strNewGeometryType = args[1];// Get the New Value for Geometry Type
        StringBuffer strErrorMsg = new StringBuffer();
        String strGeometryTypeDefault = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "AttributeRange.PSS_GeometryType.Default");
        try {

            strGeometryTypeDefault = strGeometryTypeDefault.trim();

            if (strGeometryTypeDefault.equals(strNewGeometryType)) {
                DomainObject domObj = DomainObject.newInstance(context, strObjectID);
                StringList lstselectStmts = new StringList(3);
                lstselectStmts.addElement(DomainConstants.SELECT_ID);
                lstselectStmts.addElement(DomainConstants.SELECT_TYPE);
                // TIGTK-8104 : START
                lstselectStmts.addElement(DomainConstants.SELECT_NAME);
                lstselectStmts.addElement(DomainConstants.SELECT_REVISION);
                // TIGTK-8104 : END

                Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);

                MapList mlPArtObject = domObj.getRelatedObjects(context, relPattern.getPattern(), "*", lstselectStmts, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, "", null, 0);
                int iConnectedPartCount = mlPArtObject.size();

                for (int i = 0; i < iConnectedPartCount; i++) {

                    Map mapConnectedPart = (Map) mlPArtObject.get(i);
                    String strConnectedPartId = (String) mapConnectedPart.get(DomainConstants.SELECT_ID);
                    DomainObject domPartObject = DomainObject.newInstance(context, strConnectedPartId);
                    String strPartName = (String) mapConnectedPart.get(DomainConstants.SELECT_NAME);
                    String strPartType = (String) mapConnectedPart.get(DomainConstants.SELECT_TYPE);
                    String strPartRevision = (String) mapConnectedPart.get(DomainConstants.SELECT_REVISION);

                    Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_MODEL);

                    Pattern relPattern1 = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);

                    if (domObj.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {
                        typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
                        relPattern1.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
                        if (strErrorMsg.toString().isEmpty()) {
                            strErrorMsg.append("can not modify attribute Geometry Type as MG, as connected part is already linked with 2D Part Specification having Geometry Type as MG");
                        }
                    } else if (domObj.isKindOf(context, DomainConstants.TYPE_CAD_MODEL)) {
                        typePattern = new Pattern(DomainConstants.TYPE_CAD_MODEL);
                        if (strErrorMsg.toString().isEmpty()) {
                            strErrorMsg.append("can not modify attribute Geometry Type as MG, as connected part is already linked with 3D Part Specification having Geometry Type as MG");
                        }
                    }

                    String sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]== \"" + TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE + "\" )";
                    MapList mlCADObject = domPartObject.getRelatedObjects(context, relPattern1.getPattern(), typePattern.getPattern(), lstselectStmts, null, false, true, (short) 0, sClause, null, 0);
                    if (!mlCADObject.isEmpty()) {

                        iReturn = 1;

                    }

                }

            }
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkGeometryTypeForConnectedPartSpecification: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: END
        catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkGeometryTypeForConnectedPartSpecification: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        // TIGTK-8886 - PTE - 2017-07-11 - START
        if (iReturn == 1) {
            String strStatus = context.getCustomData("PSS_THROW_EXCEPTION");
            if (UIUtil.isNotNullAndNotEmpty(strStatus) && strStatus.equals("TRUE")) {
                throw new Exception(strErrorMsg.toString());
            } else {
                MqlUtil.mqlCommand(context, "notice $1", strErrorMsg.toString());
                iReturn = 1;
            }
            // TIGTK-8886 - PTE - 2017-07-11 - END
        }
        return iReturn;
    }

    /**
     * This method will check for Geometry Type MG of Part Specification connected to Part
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int checkGeometryTypeMGForPartSpecification(Context context, String[] args) throws Exception {
        int iReturn = 0;
        String strReviseOrCloneStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_REVISE_FROM_PCM");

        if (UIUtil.isNotNullAndNotEmpty(strReviseOrCloneStatus) && strReviseOrCloneStatus.equals("True")) {
            iReturn = 0;
            return iReturn;
        }
        String strToObjectID = args[1];// Get the to Object id for Part Specification
        String strFromObjectID = args[0];// Get the from Object id for Part
        // String strRule2 = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "AttributeRange.PSS_GeometryType.Rule2");
        String strCADType2D = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "emxIEFDesignCenter.Common.CADType2D");
        String strCADType3D = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "emxIEFDesignCenter.Common.CADType3D");
        String strGeometryTypeDefault = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "AttributeRange.PSS_GeometryType.Default");
        strGeometryTypeDefault = strGeometryTypeDefault.trim();
        strCADType2D = strCADType2D.trim();
        strCADType3D = strCADType3D.trim();
        StringBuffer strErrorMsg = new StringBuffer();
        String[] arrCADType2D = strCADType2D.split(",");
        String[] arrCADType3D = strCADType3D.split(",");
        boolean b2DCADType = false;
        boolean b3DCADType = false;
        String strToGeometryType = null;
        String strRelatedGeometryType = null;

        StringList slObjectSelect = new StringList();
        slObjectSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
        slObjectSelect.add(DomainConstants.SELECT_CURRENT);
        slObjectSelect.add(DomainConstants.SELECT_POLICY);
        // TIGTK-8104 : START
        slObjectSelect.add(DomainConstants.SELECT_NAME);
        slObjectSelect.add(DomainConstants.SELECT_REVISION);
        // TIGTK-8104 : END
        try {
            DomainObject domToObj = DomainObject.newInstance(context, strToObjectID);
            BusinessObject busFromObj = new BusinessObject(strFromObjectID);
            Map<?, ?> mapToObjectInfo = domToObj.getInfo(context, slObjectSelect);

            strToGeometryType = (String) mapToObjectInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
            if (strToGeometryType == null || strToGeometryType.equals("")) {
                iReturn = 0;
            } else {
                strToGeometryType = strToGeometryType.trim();
            }
            if (strToGeometryType != null && (!"".equals(strToGeometryType))) {
                if (strGeometryTypeDefault.contains(strToGeometryType)) {
                    for (int j = 0; j < arrCADType2D.length; j++) {
                        if (domToObj.isKindOf(context, arrCADType2D[j].trim())) {
                            b2DCADType = true;
                            break;
                        }
                    }
                    for (int j = 0; j < arrCADType3D.length; j++) {
                        if (domToObj.isKindOf(context, arrCADType3D[j].trim())) {
                            b3DCADType = true;
                            break;
                        }
                    }
                    StringList slObjSel = new StringList();
                    slObjSel.add(DomainConstants.SELECT_ID);
                    slObjSel.add(DomainConstants.SELECT_NAME);
                    slObjSel.add(DomainConstants.SELECT_TYPE);
                    slObjSel.add(DomainConstants.SELECT_REVISION);
                    slObjSel.add(DomainConstants.SELECT_CURRENT);
                    slObjSel.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
                    // String strRelObjId = "";
                    StringList slRelSel = new StringList();
                    slRelSel.add(null);
                    Pattern patPartPerRel = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);

                    ExpansionIterator expItPart = busFromObj.getExpansionIterator(context, patPartPerRel.getPattern(), "*", slObjSel, slRelSel, false, true, (short) 0, null, null, (short) 1000, false,
                            true, (short) 1000);
                    MapList mpPartSpecList = FrameworkUtil.toMapList(expItPart, (short) 0, null, null, null, null);
                    if (mpPartSpecList != null) {
                        boolean bFlag = false;
                        for (int i = 0; i < mpPartSpecList.size(); i++) {
                            Map mapPartSpec = (Map) mpPartSpecList.get(i);
                            StringList slToGeometryType = (StringList) mapPartSpec.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
                            if (slToGeometryType != null) {
                                strRelatedGeometryType = (String) slToGeometryType.get(0);
                            }
                            StringList slRelatedId = (StringList) mapPartSpec.get(DomainConstants.SELECT_ID);
                            // TIGTK-8104 : START
                            StringList slRelatedName = (StringList) mapPartSpec.get(DomainConstants.SELECT_NAME);
                            StringList slRelatedRevision = (StringList) mapPartSpec.get(DomainConstants.SELECT_REVISION);
                            // TIGTK-8104 : END
                            String strRelatedId = null;
                            if (slRelatedId != null) {
                                strRelatedId = (String) slRelatedId.get(0);
                            }
                            // TIGTK-8104 : START
                            String strRelatedName = null;
                            if (slRelatedName != null) {
                                strRelatedName = (String) slRelatedName.get(0);
                            }
                            String strRelatedRevision = null;
                            if (slRelatedRevision != null) {
                                strRelatedRevision = (String) slRelatedRevision.get(0);
                            }
                            // TIGTK-8104 : END
                            DomainObject domRelatedObj = DomainObject.newInstance(context, strRelatedId);
                            // TIGTK-8104 : START
                            String strToObjectName = (String) mapToObjectInfo.get(DomainConstants.SELECT_NAME);
                            String strToObjectRevision = (String) mapToObjectInfo.get(DomainConstants.SELECT_REVISION);
                            if (!strToObjectID.equals(strRelatedId) && !(strToObjectName.equals(strRelatedName) && !strToObjectRevision.equals(strRelatedRevision))) {
                                // TIGTK-8104 : END
                                if (strGeometryTypeDefault.contains(strRelatedGeometryType)) {
                                    // slFromType = (StringList) mapPartSpec.get(DomainConstants.SELECT_TYPE);
                                    // if (slFromType != null) {
                                    // strFromType = (String) slFromType.get(0);
                                    // }
                                    if (b2DCADType) {
                                        for (int j = 0; j < arrCADType2D.length; j++) {
                                            bFlag = domRelatedObj.isKindOf(context, arrCADType2D[j].trim());
                                            if (bFlag) {
                                                strErrorMsg.append("can not add object :Part is already connected to 2D Part Specification with Geometry Type MG ");
                                                iReturn = 1;
                                                break;
                                            }
                                        }
                                    } else if (b3DCADType) {
                                        for (int j = 0; j < arrCADType3D.length; j++) {
                                            bFlag = domRelatedObj.isKindOf(context, arrCADType3D[j].trim());
                                            if (bFlag) {
                                                strErrorMsg.append("can not add object :Part is already connected to 3D Part Specification with Geometry Type MG");
                                                iReturn = 1;
                                                break;
                                            }
                                        }
                                    } else {
                                        iReturn = 0;
                                    }

                                    if (iReturn == 1) {
                                        break;
                                    }
                                } else {
                                    iReturn = 0;
                                }
                            }

                        }
                    }
                } else {
                    iReturn = 0;
                }
            }

        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkGeometryTypeMGForPartSpecification: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017 : END
        catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkGeometryTypeMGForPartSpecification: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
        try {
            if (iReturn == 1) {
                String strStatus = context.getCustomData("PSS_THROW_EXCEPTION");
                if (UIUtil.isNotNullAndNotEmpty(strStatus) && strStatus.equals("TRUE")) {
                    throw new Exception(strErrorMsg.toString());
                } else {
                    MqlUtil.mqlCommand(context, "notice $1", strErrorMsg.toString());
                    iReturn = 1;
                }
            }
        } catch (Exception ex) {
            // Findbug Issue coorection start
            // Date: 21/03/2017
            // By: Asha G.
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkGeometryTypeMGForPartSpecification: ", ex);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw ex;
            // Findbug Issue coorection End
        }
        return iReturn;
    }

    // modifications for RFC end

    /**
     * This method will propagate Basis Definition on CAD Release
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public void propagateBasisDefinitions(Context context, String[] args) throws Exception {
        try {
            String strObjectId = args[0];
            DomainObject busObj = DomainObject.newInstance(context, strObjectId);
            DomainObject lastRevisionBusObj = new DomainObject(busObj.getPreviousRevision(context));

            // Check whether last rev exist
            boolean isRevExist = lastRevisionBusObj.exists(context);
            if (!isRevExist) {
                // Last rev does not exist
                return;
            }

            String strRelationship = MCADMxUtil.getActualNameForAEFData(context, "relationship_PSS_BasisDefinition");
            String strType = MCADMxUtil.getActualNameForAEFData(context, "type_DOCUMENTS");
            StringList strListBusObjects = new StringList(1);
            strListBusObjects.addElement(DomainConstants.SELECT_ID);
            StringList strListRelObjects = new StringList(1);

            MapList relatedBDsList = lastRevisionBusObj.getRelatedObjects(context, strRelationship, strType, strListBusObjects, strListRelObjects, true, false, (short) 1, null, null, 0);

            String strBDId = null;

            if (relatedBDsList.size() > 0) {
                for (int j = 0; j < relatedBDsList.size(); j++) {
                    Hashtable htBDs = (Hashtable) relatedBDsList.get(j);
                    strBDId = (String) htBDs.get(DomainConstants.SELECT_ID);

                    if (strBDId != null) {
                        DomainObject domObjBD = DomainObject.newInstance(context, strBDId);

                        domObjBD.setRelatedObject(context, strRelationship, true, strObjectId);
                        // domObjBD.addRelatedObject(context, new RelationshipType(strRelationship), false, strObjectId);
                    }
                }
            }
        } catch (MatrixException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in propagateBasisDefinitions: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    /**
     * This method will connect New rev of object to Parts, old rev would be promoted to obsolete state
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public void propagateChartedDrawingTonewRev(Context context, String[] args) throws Exception {
        try {

            String strObjectID = args[0];// Getting the Object id
            // create business Object
            DomainObject busObj = new DomainObject(strObjectID);
            DomainObject lastRevisionBusObj = new DomainObject(busObj.getPreviousRevision(context));
            String strRelationship = MCADMxUtil.getActualNameForAEFData(context, "relationship_PSS_ChartedDrawing");
            String strType = MCADMxUtil.getActualNameForAEFData(context, "type_Part");
            // Check whether last rev exist
            boolean isRevExist = lastRevisionBusObj.exists(context);
            if (!isRevExist) {
                // Last rev does not exist
                return;
            }
            StringList BusSelectList = new StringList(DomainConstants.SELECT_ID);
            StringList RelSelectList = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);

            // Get COs related to previous revision of CAD
            String strRelChangeAffectedItem = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAffectedItem");
            String strRelChangeAction = PropertyUtil.getSchemaProperty(context, "relationship_ChangeAction");

            String strImplementedItemCO = "to[" + strRelChangeAffectedItem + "].from.to[" + strRelChangeAction + "].from.name";

            StringList slConnectedCO = new StringList(1);
            slConnectedCO.addElement(strImplementedItemCO);

            Map mapCADCO = busObj.getInfo(context, slConnectedCO);

            // Get all part ids where last rev Object is connected
            MapList relatedPartList = lastRevisionBusObj.getRelatedObjects(context, strRelationship, strType, BusSelectList, RelSelectList, true, false, (short) 1, "", "", 0);
            String strPartId = "";
            for (int j = 0; j < relatedPartList.size(); j++) {
                Hashtable partTable = (Hashtable) relatedPartList.get(j);
                strPartId = (String) partTable.get(DomainConstants.SELECT_ID);
                String strRelId = (String) partTable.get(DomainConstants.SELECT_RELATIONSHIP_ID);

                // connect it to relationship
                DomainObject PartObj = new DomainObject(strPartId);

                // Get COs related to previous revision of Part
                // Connect new CAD released revision to the Part which is not connected to the same CO and not obsolete
                String strPartObjState = PartObj.getInfo(context, DomainConstants.SELECT_CURRENT);

                StringList slConnectedChange = PartObj.getInfoList(context, strImplementedItemCO);

                boolean boolConnect = false;

                boolean bIsLastRev = PartObj.isLastRevision(context);
                if (!bIsLastRev || strPartObjState.equalsIgnoreCase(TigerConstants.STATE_PART_OBSOLETE)) {
                    boolConnect = false;
                } else if (slConnectedChange.size() > 0 && mapCADCO != null) {
                    String strCADCOName = (String) mapCADCO.get("to[" + strRelChangeAffectedItem + "].from.to[" + strRelChangeAction + "].from.name");

                    if (slConnectedChange.contains(strCADCOName)) {
                        boolConnect = false;
                    } else {
                        boolConnect = true;
                    }
                } else {
                    boolConnect = true;
                }

                // TIGTK-17437 : Vishal :START
                if (boolConnect) {
                    // PartObj.setRelatedObject(context, strRelationship, true, strObjectID);
                    // DomainRelationship.setToObject(context, strRelId, busObj);
                    String strRelIdExists = MqlUtil.mqlCommand(context, "print bus " + strPartId + " select from[" + strRelationship + "| to.id == '" + strObjectID + "'].id dump", false, false);
                    if (UIUtil.isNullOrEmpty(strRelIdExists)) {
                        PartObj.addRelatedObject(context, new RelationshipType(strRelationship), false, strObjectID);
                    }
                }
                // TIGTK-17437 : Vishal :END

            }

        } catch (MatrixException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in propagateChartedDrawingTonewRev: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }// End of propagateChartedDrawingTonewRev method

    /**
     * This method will promote pervios rev to obsolete state if current is released
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public void obsoleteLastRevOnRelease(Context context, String[] args) throws Exception {
        try {
            String strObjectID = args[0];// Getting the Object id
            // create business Object
            DomainObject busObj = new DomainObject(strObjectID);

            // get previous rev
            DomainObject lastRevisionBusObj = new DomainObject(busObj.getPreviousRevision(context));

            // Check whether last rev exist
            boolean isRevExist = lastRevisionBusObj.exists(context);

            if (!isRevExist) {
                // Last rev does not exist
                return;
            }

            String strCurrentState = lastRevisionBusObj.getInfo(context, DomainConstants.SELECT_CURRENT);
            // TIGTK-3936 -start
            // if last revision is in release state then promote it to obsolete state
            if (strCurrentState.equals(TigerConstants.STATE_RELEASED_CAD_OBJECT)) {
                // TIGTK-3936 -End
                // promote old rev to Obsolete state
                lastRevisionBusObj.promote(context);
            }
        } catch (MatrixException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in obsoleteLastRevOnRelease: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }// End of obsoleteLastRevOnRelease method

    // modification for TGPSS_CAD-TS-004-Add/remove CAD Representation start
    /**
     * This method will exclude CAD Objects which do not have Geometry Type as BD
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public StringList getBDCADObjects(Context context, String[] args) throws Exception {
        StringList slReturn = new StringList();
        Map<?, ?> mapRequest = (Map<?, ?>) JPO.unpackArgs(args);
        String strType = null;
        String strTypeList = (String) mapRequest.get("ftsFilters");
        String strGeometryTypeBD = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "AttributeRange.PSS_GeometryType.Rule3");
        try {
            JSONObject jsonObj = new JSONObject(strTypeList);
            JSONArray jsonArrayObjectTypes = (JSONArray) jsonObj.get("TYPES");
            int iNumberOfObjects = jsonArrayObjectTypes.length();
            StringList slObjectSelect = new StringList();
            HashSet typeSet = new HashSet();
            for (int i = 0; i < iNumberOfObjects; i++) {
                strType = jsonArrayObjectTypes.getString(i);
                String[] strTypeName = strType.split("\\|");
                typeSet.add(strTypeName[1]);
            }
            slObjectSelect.add(DomainConstants.SELECT_ID);
            String typeListForQuery = MCADUtil.getDelimitedStringFromCollection(typeSet, ",");

            Query query = new Query();
            query.setBusinessObjectType(typeListForQuery);
            query.setBusinessObjectName("*");
            query.setBusinessObjectRevision("*");
            String strWhereExpression = "attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]!='" + strGeometryTypeBD.trim() + "'";
            query.setWhereExpression(strWhereExpression);
            ContextUtil.startTransaction(context, true);
            QueryIterator queryIterator = query.getIterator(context, slObjectSelect, (short) 100);
            while (queryIterator.hasNext()) {
                BusinessObjectWithSelect busWithSelect = queryIterator.next();
                String strObjectId = busWithSelect.getSelectData(DomainConstants.SELECT_ID);
                slReturn.add(strObjectId);
            }
            query.close(context);
            queryIterator.close();

            // TIGTK-16265 : 02-08-2018 : START
            String strParentObjectId = (String) mapRequest.get("objectId");
            if (UIUtil.isNotNullAndNotEmpty(strParentObjectId)) {
                DomainObject domCADObject = DomainObject.newInstance(context, strParentObjectId);
                DomainConstants.MULTI_VALUE_LIST.add("from[" + TigerConstants.RELATIONSHIP_PSS_BASISDEFINITION + "]." + DomainConstants.SELECT_TO_ID);
                Map mpConnectedBDObjects = domCADObject.getInfo(context, new StringList("from[" + TigerConstants.RELATIONSHIP_PSS_BASISDEFINITION + "]." + DomainConstants.SELECT_TO_ID));
                DomainConstants.MULTI_VALUE_LIST.remove("from[" + TigerConstants.RELATIONSHIP_PSS_BASISDEFINITION + "]." + DomainConstants.SELECT_TO_ID);
                Object objConnectedBDObjects = mpConnectedBDObjects.get("from[" + TigerConstants.RELATIONSHIP_PSS_BASISDEFINITION + "]." + DomainConstants.SELECT_TO_ID);

                StringList slBDObjects = new StringList();
                if (objConnectedBDObjects != null) {
                    if (objConnectedBDObjects instanceof StringList) {
                        slBDObjects = (StringList) objConnectedBDObjects;
                    } else {
                        slBDObjects.addElement((String) objConnectedBDObjects);
                    }
                }
                if (!slBDObjects.isEmpty()) {
                    slReturn.addAll(slBDObjects);
                }
            }
            // TIGTK-16265 : 02-08-2018 : END

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error in getBDCADObjects: ", e);
            ContextUtil.abortTransaction(context);
        } finally {
            if (ContextUtil.isTransactionActive(context)) {
                ContextUtil.commitTransaction(context);
            }
        }
        return slReturn;
    }// End of getBDCADObjects method

    /**
     * This method will check for Geometry Type for adding Basis Definition. If Geometry Type will be BD, return 0
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int checkGeometryTypeForBD(Context context, String[] args) throws Exception {
        int iReturn = checkGeometryTypeForBasisDefinition(context, args);
        return iReturn;
    }// End of checkGeometryTypeForBD method

    /**
     * This method will check for Geometry Type for adding Part Specification. If Geometry Type will be BD, return 0
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int checkGeometryTypeForPartSpecification(Context context, String[] args) throws Exception {
        int iReturn = checkGeometryTypeAndCurrentStateForPartSpec(context, args);
        return iReturn;
    }// end of method checkGeometryTypeForPartSpecification

    /**
     * This method will check for Geometry Type for Basis Definition for removing Object. If Geometry Type will be BD, return 0
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int checkGeometryTypeForBasisDefinitionDelete(Context context, String[] args) throws Exception {
        int iReturn = checkGeometryTypeForBasisDefinition(context, args);
        return iReturn;
    }// end of method checkGeometryTypeForBasisDefinitionDelete

    /**
     * This method will check for Geometry Type for Part Specification for removing Object. If Geometry Type will be BD, return 0
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int checkGeometryTypeForPartSpecificationDelete(Context context, String[] args) throws Exception {
        int iReturn = checkGeometryTypeAndCurrentStateForPartSpec(context, args);
        return iReturn;
    }// end of method checkGeometryTypeForPartSpecificationDelete

    /**
     * This method will check for Geometry Type for Basis Definition. If Geometry Type will be BD, return 0
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int checkGeometryTypeForBasisDefinition(Context context, String[] args) throws Exception {
        String strGeometryTypeBD = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "AttributeRange.PSS_GeometryType.Rule3");
        strGeometryTypeBD = strGeometryTypeBD.trim();
        String strAttrValueGeometryType = null;
        StringBuffer strErrorMsg = new StringBuffer();
        int iReturn = 0;
        try {
            String strToObjectID = args[1]; // get to object Id for CAD Object
            DomainObject domObj = new DomainObject(strToObjectID);
            strAttrValueGeometryType = domObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE);
            if (strAttrValueGeometryType != null && !strAttrValueGeometryType.equals("")) {
                strAttrValueGeometryType = strAttrValueGeometryType.trim();
            }
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkGeometryTypeForBasisDefinition: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        // check for Geometry type BD
        if (strAttrValueGeometryType != null && strGeometryTypeBD.contains(strAttrValueGeometryType)) {
            iReturn = 0;
        } else {
            strErrorMsg.append("can not add/remove object  :  Geometry Type for selected object is not BD");
            iReturn = 1;
        }
        if (iReturn == 1) {
            String strStatus = context.getCustomData("PSS_THROW_EXCEPTION");
            if (UIUtil.isNotNullAndNotEmpty(strStatus) && strStatus.equals("TRUE")) {
                throw new Exception(strErrorMsg.toString());
            } else {
                MqlUtil.mqlCommand(context, "notice $1", strErrorMsg.toString());
                iReturn = 1;
            }
        }

        return iReturn;
    }// End of checkGeometryTypeForBasisDefinition method

    /**
     * This method will check for Childern state.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int checkForChildernState(Context context, String[] args) throws Exception {
        // get children one level only
        String RPEVal = PropertyUtil.getGlobalRPEValue(context, args[0] + "_promote");

        if (UIUtil.isNotNullAndNotEmpty(RPEVal)) {
            return 0;
        }

        RPEVal = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_PROMOTE_FROM_PCM");
        if (UIUtil.isNotNullAndNotEmpty(RPEVal) && RPEVal.equalsIgnoreCase("true")) {
            return 0;
        }

        StringList busSelects = new StringList(2);
        busSelects.add(DomainConstants.SELECT_CURRENT);
        busSelects.add(DomainConstants.SELECT_STATES);
        DomainObject domobj = new DomainObject(args[0]);
        String targetState = domobj.getInfo(context, DomainConstants.SELECT_CURRENT);
        String strRelCADSubComponent = MCADMxUtil.getActualNameForAEFData(context, "relationship_CADSubComponent");
        MapList utsList = domobj.getRelatedObjects(context, strRelCADSubComponent, "*", busSelects, null, // relationshipSelects
                false, // getTo
                true, // getFrom
                (short) 1, // recurseToLevel
                "", // objectWhere
                null); // relationshipWhere
        int checkPassed = 0;

        Iterator itr = utsList.iterator();
        while (itr.hasNext()) {
            Map map = (Map) itr.next();
            String state = (String) map.get(DomainConstants.SELECT_CURRENT);
            StringList taskStateList = (StringList) map.get(DomainConstants.SELECT_STATES);

            // get the position of the task's current state wrt to its
            // state list
            int taskCurrentPosition = taskStateList.indexOf(state);

            // get the position for which the state of the tasks needs
            // to be checked
            int checkStatePosition = taskStateList.indexOf(targetState);

            // check if the position being checked for exists and if the
            // current position of the task is equal to or greater than
            // the checkStatePosition
            // if this is true return true
            // else return false
            if (checkStatePosition != -1 && taskCurrentPosition > checkStatePosition) {
                continue;
            } else {
                checkPassed = 1;
                String strErrorMsg = "Parent should not be ahead of children. Please promote children to the state higher than Parent";
                Exception ex = new Exception(strErrorMsg);
                throw ex;

                // emxContextUtil_mxJPO.mqlNotice(context, strErrorMsg.toString());
                // break;
            }
        }
        return checkPassed;
    }// End of checkGeometryTypeForBasisDefinition method

    /**
     * This method will check for current state and Geometry Type for Part Specification.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int checkGeometryTypeAndCurrentStateForPartSpec(Context context, String[] args) throws Exception {

        // String STATE_PRELIMINARY = PropertyUtil.getSchemaProperty(context, "policy", TigerConstants.POLICY_PSS_ECPART, "state_Preliminary");
        String strRule1 = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "AttributeRange.PSS_GeometryType.Rule1");
        String strRule2 = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "AttributeRange.PSS_GeometryType.Rule2");
        String strRule3 = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "AttributeRange.PSS_GeometryType.Rule3");

        strRule1 = strRule1.trim();
        strRule2 = strRule2.trim();
        strRule3 = strRule3.trim();
        String strToObjectID = args[1];// Get the to Object id for Part Specification
        String strFromObjectID = args[0];// Get the from Object id for Part

        StringBuffer strErrorMsg = new StringBuffer();
        int iReturn = 0;
        String strFromCurrent = null;
        String strToCurrent = null;
        String strToGeometryType = null;
        StringList slObjectSelect = new StringList();
        String STATE_PRELIMINARY = TigerConstants.STATE_PSS_ECPART_PRELIMINARY;
        slObjectSelect.add("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
        slObjectSelect.add(DomainConstants.SELECT_CURRENT);
        slObjectSelect.add(DomainConstants.SELECT_POLICY);

        String strStateFromRelease = TigerConstants.STATE_PART_RELEASE;
        String strStateFromReview = TigerConstants.STATE_PART_REVIEW;
        String strStateFromApproved = TigerConstants.STATE_PART_APPROVED;
        String strStateFromObsolete = TigerConstants.STATE_PART_OBSOLETE;
        String strPolicyFrom = DomainConstants.EMPTY_STRING;

        try {
            String strReviseOrCloneStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_REVISE_FROM_PCM");
            if (UIUtil.isNotNullAndNotEmpty(strReviseOrCloneStatus) && strReviseOrCloneStatus.equals("True")) {

                return iReturn;
            }

            String strCADDeleteStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_DELETE_CAD_FROM_CANCEL_CO_JSP");
            if (UIUtil.isNotNullAndNotEmpty(strCADDeleteStatus) && strCADDeleteStatus.equals("True")) {
                return iReturn;
            }

            String strApproveCATaskForReplace = PropertyUtil.getGlobalRPEValue(context, "APPROVE_CA_TASK_FOR_REPLACE");
            if (UIUtil.isNotNullAndNotEmpty(strApproveCATaskForReplace) && strApproveCATaskForReplace.equals("True")) {
                return iReturn;
            }

            DomainObject domToObj = new DomainObject(strToObjectID);
            DomainObject domFromObj = new DomainObject(strFromObjectID);
            Map<?, ?> mapToObjectInfo = domToObj.getInfo(context, slObjectSelect);
            Map<?, ?> mapFromObjectInfo = domFromObj.getInfo(context, slObjectSelect);
            strPolicyFrom = (String) mapFromObjectInfo.get(DomainConstants.SELECT_POLICY);

            if ("PSS_Development_Part".equals(strPolicyFrom)) {
                STATE_PRELIMINARY = TigerConstants.STATE_PSS_DEVELOPMENTPART_CREATE;

                strStateFromRelease = TigerConstants.STATE_PSS_DEVELOPMENTPART_COMPLETE;
                strStateFromReview = TigerConstants.STATE_PSS_DEVELOPMENTPART_PEERREVIEW;
                strStateFromObsolete = TigerConstants.STATE_PSS_DEVELOPMENTPART_OBSOLETE;
            }

            strToGeometryType = (String) mapToObjectInfo.get("attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]");
            if (strToGeometryType == null || strToGeometryType.equals("")) {
                return 0;
            } else {
                strToGeometryType = strToGeometryType.trim();
            }
            strFromCurrent = (String) mapFromObjectInfo.get(DomainConstants.SELECT_CURRENT);
            strToCurrent = (String) mapToObjectInfo.get(DomainConstants.SELECT_CURRENT);
        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkGeometryTypeAndCurrentStateForPartSpec: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }

        // check for CAD objects
        // check for rule 3
        if (strToGeometryType != null && (!"".equals(strToGeometryType))) {
            if (strRule3.contains(strToGeometryType)) {
                strErrorMsg.append("can not add object : Geometry Type for selected object is BD");
                iReturn = 1;
            } else if (strFromCurrent.equals(STATE_PRELIMINARY)) // check for rule 1
            {
                if (strRule1.contains(strToGeometryType)) // check for rule 1
                {
                    iReturn = 0;
                } else {
                    strErrorMsg.append("can not add/remove object :  Geometry Type value does not comes under Rule 1");
                    iReturn = 1;
                }
            } else if (strFromCurrent.equals(strStateFromRelease)) {
                if (strToCurrent.equals(TigerConstants.STATE_RELEASED_CAD_OBJECT)) {
                    if (strRule2.contains(strToGeometryType))// check for rule 2
                    {
                        iReturn = 0;
                    } else {
                        strErrorMsg.append("can not add/remove object :  Geometry Type value does not comes under rule 2");
                        iReturn = 1;
                    }
                } else {
                    strErrorMsg.append("can not add/remove object :  Part is in released state and CAD object is not in released state");
                    iReturn = 1;
                }
            } else if (strFromCurrent.equals(strStateFromReview) || strFromCurrent.equals(strStateFromApproved) || strFromCurrent.equals(strStateFromObsolete)) {
                strErrorMsg.append("can not add/remove object :  Part is in Review/Approved/Obsolete state");
                iReturn = 1;
            }
        } else {
            iReturn = 0;

        }
        try {
            if (iReturn == 1) {
                throw new Exception(strErrorMsg.toString());
            }
        } catch (Exception ex) {
            logger.error("Error in checkGeometryTypeAndCurrentStateForPartSpec: ", ex);
            throw ex;
        }
        return iReturn;
    }// end of method checkGeometryTypeAndCurrentState
     // modification for TGPSS_CAD-TS-004-Add/remove CAD Representation end

    // modification for TGPSS_CAD-US-003-Create Revision Manually start
    /**
     * This method will check revision sequence for new object.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int validateRevisionOnCreate(Context context, String[] args) throws Exception {
        int iReturn = 1;
        String strNewObjectRevision = args[2];
        String strNewObjectPolicy = args[3];
        StringBuffer strErrorMsg = new StringBuffer();
        if (!POLICY_VERSIONEDDESIGNPOLICY.equals(strNewObjectPolicy)) {
            if (strNewObjectRevision.matches(REGEX_POLICY_PSS_CADOBJECT)) {
                iReturn = 0;
            } else {
                iReturn = 1;
                // throw new Exception("Revision sequence " + strNewObjectRevision + " is invalid");
                strErrorMsg.append("Revision sequence " + strNewObjectRevision + " is invalid");
            }
        } else {
            iReturn = 0;
        }
        if (iReturn == 1) {
            throw new Exception(strErrorMsg.toString());
        }
        return iReturn;
    }// end of method validateRevisionOnCreate

    /**
     * This method will check revision sequence for Revising object.
     * @param context
     * @param args
     * @throws Exception
     * @author Steepgraph Systems
     */
    public int validateRevisionOnRevise(Context context, String[] args) throws Exception {
        int iReturn = 0;
        try {
            String strObjectRevision = args[2];
            String strObjectPolicy = args[3];
            String strNewObjectRevision = args[4];
            StringBuffer strErrorMsg = new StringBuffer();
            String strObjectId = args[5];

            if (!POLICY_VERSIONEDDESIGNPOLICY.equals(strObjectPolicy)) {

                BusinessObject domObject = new BusinessObject(strObjectId);
                String strNextRevSeq = domObject.getNextSequence(context);
                if (!strNewObjectRevision.equals(strNextRevSeq)) {
                    iReturn = 1;
                    strErrorMsg.append("Revision sequence " + strNewObjectRevision + " is not a valid. Valid revision sequence is " + strNextRevSeq);
                    throw new Exception(strErrorMsg.toString());
                }

                if (strNewObjectRevision.matches(REGEX_POLICY_PSS_CADOBJECT)) {
                    if (strObjectRevision.contains("-") && !strNewObjectRevision.contains("-")) {
                        iReturn = 0;
                    } else if (!strObjectRevision.contains("-") && strNewObjectRevision.contains("-")) {
                        iReturn = 1;
                        strErrorMsg.append("Revision sequence " + strNewObjectRevision + " can not be less than last revision " + strObjectRevision + " of the object");
                    } else {
                        if (strObjectRevision.contains("-") && strNewObjectRevision.contains("-")) {
                            String strCompareOldRevision = strObjectRevision.substring(1);
                            String strCompareNewRevision = strNewObjectRevision.substring(1);
                            int iRevCompare = strCompareNewRevision.compareTo(strCompareOldRevision);
                            if (iRevCompare <= 0) {
                                iReturn = 1;
                                strErrorMsg.append("Revision sequence " + strNewObjectRevision + " can not be less than last revision " + strObjectRevision + " of the object");
                            }
                        } else if (!strObjectRevision.contains("-") && !strNewObjectRevision.contains("-")) {
                            int iRevCompare = strNewObjectRevision.compareTo(strObjectRevision);
                            if (iRevCompare <= 0) {
                                iReturn = 1;
                                strErrorMsg.append("Revision sequence " + strNewObjectRevision + " can not be less than last revision " + strObjectRevision + " of the object");
                            }
                        }
                    }
                } else {
                    iReturn = 1;
                    strErrorMsg.append("Revision sequence " + strNewObjectRevision + " is invalid");
                }
            }
            if (iReturn == 1) {
                throw new Exception(strErrorMsg.toString());
            }
        } catch (Exception e) {
            logger.error("Error in validateRevisionOnRevise()\n", e);
            throw e;
        }
        return iReturn;
    }
    // end of method validateRevisionOnRevise
    // modification for TGPSS_CAD-US-003-Create Revision Manually end

    /**
     * Update the new value of
     * @param context
     *            the Matrix Context
     * @param args
     *            no args needed for this method
     * @returns booloen
     * @throws Exception
     *             if the operation fails
     */
    public Boolean updateOriginalFileName(Context context, String[] args) throws Exception {

        try {
            // unpack args to get map
            HashMap programMap = (HashMap) JPO.unpackArgs(args);

            HashMap paramMap = (HashMap) programMap.get("paramMap");
            // get object id
            String strObjectId = (String) paramMap.get("objectId");

            // get the new value of origininal file name attribute
            // TIGTK-3936
            String strParamNewValue = EnoviaResourceBundle.getProperty(context, "PSS_emxIEFDesignCenter.CAD2D3D.ParamNewValue");
            String strNewFileName = (String) paramMap.get(strParamNewValue);

            DomainObject obj = new DomainObject(strObjectId);

            // String strAttrORginalFilename = MCADMxUtil.getActualNameForAEFData(context, "PSS_OriginalFileName");
            // update the value of property
            obj.setAttributeValue(context, "PSS_OriginalFileName", strNewFileName);
        } catch (Exception ex) {
            logger.error("Error in validateRevisionOnRevise: ", ex);
            throw ex;
        }

        return Boolean.TRUE;

    }

    /**
     * @param context
     * @param args
     *            : Type, name and revision
     * @return: 0 or 1 depending on the situation
     * @throws Exception
     * @description: this method will
     */
    public Boolean canRevise(Context context, String[] args) throws Exception {
        try {
            HashMap programMap = (HashMap) JPO.unpackArgs(args);
            String strObjectID = (String) programMap.get("objectId");
            StringList strList = new StringList();
            strList.add(DomainConstants.SELECT_ID);
            strList.add(DomainConstants.SELECT_CURRENT);
            strList.add(DomainConstants.SELECT_POLICY);
            String strRelPartSpecification = MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification");

            String strRelPSSChartedDwg = MCADMxUtil.getActualNameForAEFData(context, "relationship_PSS_ChartedDrawing");

            DomainObject objDom = new DomainObject(strObjectID);

            BusinessObject objCADObj = objDom.getLastRevision(context);

            objDom = new DomainObject(objCADObj);
            Map rootMap = objDom.getInfo(context, strList);
            String strCurrentState = (String) rootMap.get(DomainConstants.SELECT_CURRENT);
            // TIGTK-3936 -start

            if (!strCurrentState.equals(TigerConstants.STATE_RELEASED_CAD_OBJECT)) {
                // TIGTK-3936 -end
                return false;
            }

            String strRelpattern = strRelPartSpecification + "," + strRelPSSChartedDwg;
            String typePattern = MCADMxUtil.getActualNameForAEFData(context, "type_Part");
            String objectWhere = "policy=='" + TigerConstants.POLICY_PSS_ECPART + "'";

            // Map mapRels = objDom.getRelatedObject(context, strRelpattern, false, strList, null);
            MapList mapRels2 = objDom.getRelatedObjects(context, strRelpattern, typePattern, strList, null, true, false, (short) 1, objectWhere, null, (int) 1);

            // if (mapRels != null && !mapRels.isEmpty()) {
            // return false;
            // }

            if (mapRels2.size() > 0) {
                return false;
            }

            return true;

        } catch (MatrixException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in canRevise: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            return true;
        }

    }

    public int checkCreateRules(Context context, String[] args) throws Exception {
        try {
            String strType = args[0];
            String strName = args[1];
            // String strRevision = args[2];
            String strPolicy = args[3];
            String strPromoteStateError = "";
            // if (strPolicy != POLICY_PSS_CADOBJECT) {
            if (!strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CADOBJECT)) {
                return 0;

            }
            StringList strListID = new StringList();
            strListID.add(DomainConstants.SELECT_ID);
            strListID.add(DomainConstants.SELECT_CURRENT);
            strListID.add(DomainConstants.SELECT_NAME);
            strListID.add(DomainConstants.SELECT_REVISION);

            Query query = new Query();
            query.setBusinessObjectType(strType);
            query.setBusinessObjectName(strName);
            query.setBusinessObjectRevision("*");
            String strWhereExpression = "policy==" + TigerConstants.POLICY_PSS_CADOBJECT;
            // query.setWhereExpression("policy==" + POLICY_PSS_CADOBJECT);
            query.setWhereExpression(strWhereExpression);
            ContextUtil.startTransaction(context, true);
            QueryIterator queryIterator = query.getIterator(context, strListID, (short) 100);

            ArrayList strALofobID = new ArrayList();

            while (queryIterator.hasNext()) {
                BusinessObjectWithSelect busWithSelect = queryIterator.next();
                String rev = busWithSelect.getSelectData(DomainConstants.SELECT_REVISION);

                strALofobID.add(rev);

            }
            query.close(context);
            queryIterator.close();
            Object ia[] = strALofobID.toArray();
            if (ia.length < 2) {
                return 0;

            }

            BusinessObject objCADObj = new BusinessObject(strType, strName, ((String) ia[ia.length - 2]), ""); // domobje.getLastRevision(context);

            DomainObject domobje = new DomainObject(objCADObj);

            Map rootMap = domobje.getInfo(context, strListID);
            String strCurrentState = (String) rootMap.get(DomainConstants.SELECT_CURRENT);

            if (!strCurrentState.equals(TigerConstants.STATE_RELEASED_CAD_OBJECT)) {
                strPromoteStateError = "You can not revise, as " + strName + " is not in " + TigerConstants.STATE_RELEASED_CAD_OBJECT + " state";
                MqlUtil.mqlCommand(context, "notice $1", strPromoteStateError);
                return 1;
            }

            String strRelPartSpecification = MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification");
            String strRelPSSChartedDwg = MCADMxUtil.getActualNameForAEFData(context, "relationship_PSS_ChartedDrawing");
            // String strECPartpolicy = MCADMxUtil.getActualNameForAEFData(context, "policy_ECPart");
            String objectWhere = "policy=='" + TigerConstants.POLICY_PSS_ECPART + "'";

            String typePattern = "Part";
            String relationshipPattern = strRelPSSChartedDwg + "," + strRelPartSpecification;
            MapList mapRels = domobje.getRelatedObjects(context, relationshipPattern, typePattern, strListID, null, true, false, (short) 1, objectWhere, DomainObject.EMPTY_STRING, (short) 1, true,
                    false, (short) 10, null, null, null, DomainObject.EMPTY_STRING);

            if (!mapRels.isEmpty()) {
                // strPromoteStateError = "You can not revise " + strName + ", it is connected to following Part/Parts \n";
                StringBuffer buf = new StringBuffer();
                buf.append("You can not revise " + strName + ", it is connected to following Part/Parts   \n");

                for (int i = 0; i < mapRels.size(); i++) {
                    Map connectIdMap = (Map) mapRels.get(i);
                    // strPromoteStateError += connectIdMap.get(DomainConstants.SELECT_NAME) + "\n";
                    buf.append(connectIdMap.get(DomainConstants.SELECT_NAME) + "\n");
                }

                MqlUtil.mqlCommand(context, "notice $1", buf.toString());

                return 1;
            }

            return 0;

        } catch (MatrixException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkCreateRules: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            ContextUtil.abortTransaction(context);
            return 0;
        } finally {
            if (ContextUtil.isTransactionActive(context)) {
                ContextUtil.commitTransaction(context);
            }
        }

    }

    /**
     * @param context
     * @param args
     *            : type, name , revision and policy
     * @return : 0 if CAD object is in Released state and it is connected to Part which is in Release or preliniminary state and CO/CA
     * @throws Exception
     *             Description: if CAD object is in Released state and it is connected to Part which is in Release or preliniminary state and CO/CA CAD object will be revised
     */
    public int checkReviseRules(Context context, String[] args) throws Exception {
        try {
            // get id
            String strType = args[0];
            String strPromoteStateError = "";

            String strName = args[1];

            String strRevision = args[2];
            String strPolicy = args[3];
            // check for minor revision
            if (!strPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {
                return 0;
            }

            StringList strListID = new StringList();
            strListID.add(DomainConstants.SELECT_ID);
            strListID.add(DomainConstants.SELECT_CURRENT);
            strListID.add(DomainConstants.SELECT_NAME);
            // get business object
            BusinessObject busObj = new BusinessObject(strType, strName, strRevision, "");
            DomainObject domobje = new DomainObject(busObj);
            BusinessObject objCADObj = domobje.getLastRevision(context);
            domobje = new DomainObject(objCADObj);
            Map rootMap = domobje.getInfo(context, strListID);
            String strCurrentState = (String) rootMap.get(DomainConstants.SELECT_CURRENT);
            // check for current state of object
            if (!strCurrentState.equals(TigerConstants.STATE_RELEASED_CAD_OBJECT)) {
                strPromoteStateError = "You can not revise " + strName + ", This should be in Released state.\n";

                MqlUtil.mqlCommand(context, "notice $1", strPromoteStateError);
                return 1;
            }

            // String strRelCandidateAffectedItem = MCADMxUtil.getActualNameForAEFData(context, "relationship_CandidateAffectedItem");
            String strRelPartSpecification = MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification");
            String strRelPSSChartedDwg = MCADMxUtil.getActualNameForAEFData(context, "relationship_PSS_ChartedDrawing");
            String strRELChangeAction = MCADMxUtil.getActualNameForAEFData(context, "relationship_ChangeAction");
            String objectWhere1 = "policy=='" + TigerConstants.POLICY_PSS_ECPART + "' && (last.current=='" + TigerConstants.STATE_PART_REVIEW + "' || last.current=='"
                    + TigerConstants.STATE_PART_APPROVED + "')";
            String typePattern = "Part";
            String relationshipPattern = strRelPSSChartedDwg + "," + strRelPartSpecification;
            MapList mapRels = domobje.getRelatedObjects(context, relationshipPattern, typePattern, strListID, null, true, false, (short) 1, objectWhere1, "", (int) 1);

            if (mapRels != null && !mapRels.isEmpty()) {
                // strPromoteStateError = "You can not revise " + strName + ", it is connected to following Part/Parts which is/are in Approved/Review state \n";
                StringBuffer buf = new StringBuffer();
                buf.append("You can not revise " + strName + ", it is connected to following Part/Parts which is/are in Approved/Review state  \n");
                for (int i = 0; i < mapRels.size(); i++) {
                    Map connectIdMap = (Map) mapRels.get(i);
                    // strPromoteStateError += connectIdMap.get(DomainConstants.SELECT_NAME) + "\n";
                    buf.append(connectIdMap.get(DomainConstants.SELECT_NAME) + "\n");
                }
                MqlUtil.mqlCommand(context, "notice $1", buf.toString());
                return 1;
            }

            objectWhere1 = "policy=='" + TigerConstants.POLICY_PSS_ECPART + "' && (last.current=='" + TigerConstants.STATE_PART_RELEASE + "' || last.current=='"
                    + TigerConstants.STATE_PSS_ECPART_PRELIMINARY + "')";
            mapRels = domobje.getRelatedObjects(context, relationshipPattern, typePattern, strListID, null, true, false, (short) 1, objectWhere1, "", (int) 1);

            if (mapRels != null && !mapRels.isEmpty()) {

                StringList partIDList = new StringList();
                objectWhere1 = "current=='" + TigerConstants.STATE_CHANGEACTION_PENDING + "' || current=='" + TigerConstants.STATE_CHANGEACTION_INWORK + "'";
                typePattern = TigerConstants.TYPE_CHANGEACTION;
                relationshipPattern = RELATIONSHIP_CHANGEAFFECTEDITEM;
                for (int i = 0; i < mapRels.size(); i++) {
                    // Map connectIdMap = (Map) mapRels.get(i);
                    // DomainObject dom = new DomainObject((String)connectIdMap.get(DomainConstants.SELECT_ID));
                    // String id = dom.getInfo(context, "to["+RELATIONSHIP_CHANGEAFFECTEDITEM+"].from.to["+strRELChangeAction+"].from.id");

                    MapList mapRels2 = domobje.getRelatedObjects(context, relationshipPattern, typePattern, strListID, null, true, false, (short) 1, objectWhere1, "", (int) 1);
                    if (mapRels2 != null && !mapRels2.isEmpty()) {
                        for (int j = 0; j < mapRels2.size(); j++) {
                            Map connectIdMap2 = (Map) mapRels2.get(j);

                            DomainObject dom2 = new DomainObject((String) connectIdMap2.get(DomainConstants.SELECT_ID));
                            String id = dom2.getInfo(context, "to[" + strRELChangeAction + "].from.id");
                            if (UIUtil.isNotNullAndNotEmpty(id))
                                partIDList.add(id);

                        }
                    }

                }

                StringList COids = new StringList();
                mapRels = domobje.getRelatedObjects(context, relationshipPattern, typePattern, strListID, null, true, false, (short) 1, objectWhere1, "", (int) 1);

                if (mapRels != null && !mapRels.isEmpty()) {
                    for (int i = 0; i < mapRels.size(); i++) {
                        Map connectIdMap = (Map) mapRels.get(i);

                        DomainObject dom2 = new DomainObject((String) connectIdMap.get(DomainConstants.SELECT_ID));
                        String id = dom2.getInfo(context, "to[" + strRELChangeAction + "].from.id");
                        if (UIUtil.isNotNullAndNotEmpty(id))
                            COids.add(id);
                    }
                    String[] oidsArray = new String[COids.size()];
                    oidsArray = (String[]) COids.toArray(oidsArray);
                    for (String s : oidsArray) {
                        if (partIDList.contains(s))
                            return 0;
                    }

                }

                strPromoteStateError = "You can not revise " + strName + ", it is not connected to any CO/CA  \n";
                MqlUtil.mqlCommand(context, "notice $1", strPromoteStateError);
                return 1;
            }

            return 0;

        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: START
        catch (RuntimeException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkReviseRules: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        } // Fix for FindBugs issue RuntimeException capture: Suchit Gangurde: 28 Feb 2017: END
        catch (Exception e) {

            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkReviseRules: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            return 0;
        }

    }

    /**
     * @param context
     * @param args
     *            : object ID to be demoted
     * @return : 0 if parent is in In work state and 1 if parent is not in In Work state
     * @throws Exception
     *             Description: this will take objects ID as parameter and will check if the parent of this is in IN Work state, if it is not then demotion is blocked.
     */
    public int checkForParentInWork(Context context, String[] args) throws Exception {
        try {
            // TIGTK-6840:Start
            String strCancelStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_COCancelFromInReview");
            if (UIUtil.isNotNullAndNotEmpty(strCancelStatus) && strCancelStatus.equals("True")) {
                return 0;
            }
            // TIGTK-6840:End
            // get Object ID
            String strObjID = args[0];
            DomainObject domObject = new DomainObject(strObjID);
            // Parent object state
            String strObjectWhere = "current!='" + TigerConstants.STATE_PSS_ECPART_PRELIMINARY + "' && current!='" + TigerConstants.STATE_DEVELOPMENTPART_CREATE + "'";
            StringList strListID = new StringList();
            strListID.add(DomainConstants.SELECT_ID);
            strListID.add(DomainConstants.SELECT_NAME);
            String strRelationshipPattern = MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification");

            // get parents
            MapList mapRels = domObject.getRelatedObjects(context, strRelationshipPattern, DomainConstants.QUERY_WILDCARD, strListID, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1,
                    strObjectWhere, DomainConstants.EMPTY_STRING, (int) 1);

            if (mapRels == null || mapRels.isEmpty()) {
                // If no parent with state other then In work found return 0

                return 0;

            }

            String strAlertMessageParentNotInWorkState = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "emxIEFDesignCenter.ErrorMsg.ParentStateNotValid");
            MqlUtil.mqlCommand(context, "notice $1", strAlertMessageParentNotInWorkState);
            return 1;

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in checkForParentInWork: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            return 1;
        }
    }

    /**
     * @param context
     * @param args
     *            : object ID to be demoted
     * @return : true if new iteration of object is created and return false if exception occurs
     * @throws Exception
     *             Description: Method will create new iteration of major object, will copy all files from major to old minor and will link all rels from old minor to new minor
     * @author : steepGraph
     */
    @SuppressWarnings("deprecation")
    public Boolean createNewIteration(Context context, String[] args) throws Exception {
        try {
            // get Object ID
            String strObjID = args[0];
            String strActiveVersionrelname = MCADMxUtil.getActualNameForAEFData(context, "relationship_ActiveVersion");
			// TIGTK - 18183 : stembulkar : start 
			String relLatestVersion = MCADMxUtil.getActualNameForAEFData(context, "relationship_LatestVersion");
			String TYPE_PSS_CAT_DRAWING = PropertyUtil.getSchemaProperty(context, "type_PSS_CATDrawing");
            // TIGTK - 18183 : stembulkar : end
            String strRelDerivedOutput = MCADMxUtil.getActualNameForAEFData(context, "relationship_DerivedOutput");
            String strRelViewable = MCADMxUtil.getActualNameForAEFData(context, "relationship_Viewable");
            String strRelCADSubComponent = MCADMxUtil.getActualNameForAEFData(context, "relationship_CADSubComponent");
            String strRelAssociatedDrawing = MCADMxUtil.getActualNameForAEFData(context, "relationship_AssociatedDrawing");
            String atrbName = PropertyUtil.getSchemaProperty(context, "attribute_CADObjectName");
            String ATTRIBUTE_SPATIAL_LOCATION = PropertyUtil.getSchemaProperty(context, "attribute_SpatialLocation");
            String ATTRIBUTE_REFERENCE_DESIGNATOR = PropertyUtil.getSchemaProperty(context, "attribute_ReferenceDesignator");
            String ATTRIBUTE_IEF_UUID = PropertyUtil.getSchemaProperty(context, "attribute_IEF-UUID");
            String ATTRIBUTE_RELATIONSHIP_UUID = PropertyUtil.getSchemaProperty(context, "attribute_RelationshipUUID");

            // stringlist for object
            StringList strListID = new StringList();
            strListID.add(DomainConstants.SELECT_ID);
            strListID.add("from[" + strActiveVersionrelname + "].to.id");
            strListID.add("from[" + strRelDerivedOutput + "].to.id");
            strListID.add("from[" + strRelDerivedOutput + "].id");
            strListID.add(DomainConstants.SELECT_REVISION);

            // stringlist for relationship
            StringList strRelSelect = new StringList();
            strRelSelect.add("to.id");
            strRelSelect.add("from.id");
            strRelSelect.add(DomainRelationship.SELECT_ID);
            strRelSelect.add("attribute[" + ATTRIBUTE_SPATIAL_LOCATION + "]");
            strRelSelect.add("attribute[" + ATTRIBUTE_REFERENCE_DESIGNATOR + "]");
            strRelSelect.add("attribute[" + ATTRIBUTE_IEF_UUID + "]");
            strRelSelect.add("attribute[" + ATTRIBUTE_RELATIONSHIP_UUID + "]");

            // Create Domain object
            DomainObject domObject = new DomainObject(strObjID);
            // get Active minor version of object
            Map<?, ?> MapActiveVersionID = domObject.getInfo(context, strListID);

            StringList strActiveVersionID = (StringList) MapActiveVersionID.get("from[" + strActiveVersionrelname + "].to.id");
            String strDerivedOutputID = (String) MapActiveVersionID.get("from[" + strRelDerivedOutput + "].to.id");
            String strDerivedOutputRELID = (String) MapActiveVersionID.get("from[" + strRelDerivedOutput + "].id");

            String strRev = (String) MapActiveVersionID.get(DomainConstants.SELECT_REVISION);

            DomainObject domObjectActiveVersion = new DomainObject((String) strActiveVersionID.get(0));
            DomainObject domObjectDerivedOutput = new DomainObject(strDerivedOutputID);
            // get next sequence for minor version
            int intNextSequence = Integer.parseInt(domObjectActiveVersion.getInfo(context, "revindex"));
            intNextSequence = intNextSequence + 1;
            String strLatestVersion = strRev + "." + intNextSequence;

            Pattern relPattern = new Pattern(strRelCADSubComponent);
            relPattern.addPattern(strRelAssociatedDrawing);

            // TIGTK-12992 : If Logged-in User is User Agent then get logged-in user from PCM Process
            String loggedInUser = context.getUser();
            if (loggedInUser.equals("User Agent")) {
                String strCurrentLoggedInUser = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_DEMOTE_USERNAME");
                if (UIUtil.isNotNullAndNotEmpty(strCurrentLoggedInUser))
                    loggedInUser = strCurrentLoggedInUser;
            }
            // TIGTK-12992 : END
            // get Sub Component and Viewable relationships of active minor version
            ExpansionIterator iter = domObjectActiveVersion.getExpansionIterator(context, relPattern.getPattern(), DomainConstants.QUERY_WILDCARD, strListID, strRelSelect, true, true, (short) 1,
                    DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING, (short) 0, false, false, (short) 1, false);

            MapList mapCADComponentsrel = FrameworkUtil.toMapList(iter, (short) 0, null, null, null, null);

            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
            // revise active minor version
            BusinessObject busActiveVersionRevisedObj = null;
            DomainObject domActiveVersionRevisedObj = null;
            BusinessObject busrevisedObjDO;
            try {
                MqlUtil.mqlCommand(context, "trigger off");
                // revise active minor version

                RelationshipType reltype = new RelationshipType(strRelDerivedOutput);

                busActiveVersionRevisedObj = domObjectActiveVersion.reviseObject(context, strLatestVersion, false);
                domActiveVersionRevisedObj = new DomainObject(busActiveVersionRevisedObj);
                // TIGTK-12992 : START
                // Set Owner command was not working so using MQL
                MqlUtil.mqlCommand(context, "mod bus $1 owner $2", busActiveVersionRevisedObj.getObjectId(), loggedInUser);
                // TIGTK-12992 : END
                if (!UIUtil.isNullOrEmpty(strDerivedOutputRELID)) {
                    DomainRelationship.disconnect(context, strDerivedOutputRELID);
                    busrevisedObjDO = domObjectDerivedOutput.reviseObject(context, strLatestVersion, true);
                    busrevisedObjDO.setOwner(context, loggedInUser);

                    Relationship rel = busrevisedObjDO.connect(context, reltype, false, domObject);
                    DomainRelationship doRel = new DomainRelationship(rel);
                    doRel.setAttributeValue(context, atrbName, domObject.getName());

                    rel = busrevisedObjDO.connect(context, reltype, false, busActiveVersionRevisedObj);
                    doRel = new DomainRelationship(rel);
                    doRel.setAttributeValue(context, atrbName, domObject.getName());
                }

                StringList slViewableID = domObject.getInfoList(context, "from[" + strRelViewable + "].to.id");
                StringList slViewableRelID = domObject.getInfoList(context, "from[" + strRelViewable + "].id");

                reltype = new RelationshipType(strRelViewable);

                for (int i = 0; i < slViewableRelID.size(); i++) {
                    DomainRelationship.disconnect(context, (String) slViewableRelID.get(i));
                }

                StringList slViewableActiveVersionRelID = domActiveVersionRevisedObj.getInfoList(context, "from[" + strRelViewable + "].id");

                for (int iCnt = 0; iCnt < slViewableActiveVersionRelID.size(); iCnt++) {
                    DomainRelationship.disconnect(context, (String) slViewableActiveVersionRelID.get(iCnt));
                }

                for (int i = 0; i < slViewableID.size(); i++) {
                    String strViewableObjId = (String) slViewableID.get(i);
                    DomainObject viewableDomObj = new DomainObject(strViewableObjId);

                    busrevisedObjDO = viewableDomObj.reviseObject(context, strLatestVersion, true);
                    busrevisedObjDO.setOwner(context, loggedInUser);

                    Relationship rel = busrevisedObjDO.connect(context, reltype, false, domObject);
                    DomainRelationship doRel = new DomainRelationship(rel);
                    doRel.setAttributeValue(context, atrbName, domObject.getName());

                    rel = busrevisedObjDO.connect(context, reltype, false, busActiveVersionRevisedObj);
                    doRel = new DomainRelationship(rel);
                    doRel.setAttributeValue(context, atrbName, domObject.getName());

                }

            } catch (RuntimeException exp) {
                // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
                logger.error("RuntimeException in createNewIteration: ", exp);
                // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
                throw exp;
            } catch (Exception exp) {
                logger.error("Error in createNewIteration: ", exp);
            } finally {
                MqlUtil.mqlCommand(context, "trigger on");
                ContextUtil.popContext(context);
            }

            Iterator objItr = (Iterator) mapCADComponentsrel.iterator();
            // loop to link all rels to new minor
            for (int i = 0; i < mapCADComponentsrel.size(); i++) {
                Map connectIdMap = (Map) objItr.next();

                BusinessObject busParentObj = new BusinessObject((String) connectIdMap.get(DomainConstants.SELECT_ID));
                boolean from = true;
                if (((String) connectIdMap.get("to.id")).equals((String) strActiveVersionID.get(0))) {
                    from = false;
                }

                String strRelName = (String) connectIdMap.get("relationship");
                RelationshipType reltype = new RelationshipType(strRelName);

                String sRelId = (String) connectIdMap.get(DomainRelationship.SELECT_ID);

                if (TigerConstants.RELATIONSHIP_CADSUBCOMPONENT.equals(strRelName)) {
                    if (!from) {
                        String sParentId = (String) connectIdMap.get("from.id");
                        DomainObject doParent = new DomainObject(sParentId);
                        if (doParent.isLastRevision(context)) {
                            DomainRelationship.setToObject(context, sRelId, domActiveVersionRevisedObj);
                        }
                    } else {
                        DomainRelationship doRel = DomainRelationship.connect(context, domActiveVersionRevisedObj, TigerConstants.RELATIONSHIP_CADSUBCOMPONENT, new DomainObject(busParentObj));

                        AttributeList attributelist = new AttributeList();
                        attributelist.addElement(new Attribute(new AttributeType(ATTRIBUTE_SPATIAL_LOCATION), (String) connectIdMap.get("attribute[" + ATTRIBUTE_SPATIAL_LOCATION + "]")));
                        attributelist.addElement(new Attribute(new AttributeType(ATTRIBUTE_REFERENCE_DESIGNATOR), (String) connectIdMap.get("attribute[" + ATTRIBUTE_REFERENCE_DESIGNATOR + "]")));
                        attributelist.addElement(new Attribute(new AttributeType(ATTRIBUTE_IEF_UUID), (String) connectIdMap.get("attribute[" + ATTRIBUTE_IEF_UUID + "]")));
                        attributelist.addElement(new Attribute(new AttributeType(ATTRIBUTE_RELATIONSHIP_UUID), (String) connectIdMap.get("attribute[" + ATTRIBUTE_RELATIONSHIP_UUID + "]")));
                        doRel.setAttributes(context, attributelist);
                    }
                } else {
                    //TIGTK - 18183 : stembulkar : start
					DomainObject dParentObj = new DomainObject( busParentObj );
					String testRevision = dParentObj.getInfo( context, DomainConstants.SELECT_REVISION );
					String strRevActiveVersionRevisedObj = domActiveVersionRevisedObj.getInfo( context, DomainConstants.SELECT_REVISION );
					String strLatestVersionObjId = dParentObj.getInfo( context, "from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.from[" + relLatestVersion + "].to.id");
					DomainObject dParentLatestVersionObj = DomainObject.newInstance( context, strLatestVersionObjId );

					StringList strList  = new StringList();
					StringList strRelIdList  = new StringList();
					String strType = domActiveVersionRevisedObj.getType( context );
					if( strType.equals( TYPE_PSS_CAT_DRAWING ) ) {
						strList = domActiveVersionRevisedObj.getInfoList( context , "to[" + strRelAssociatedDrawing + "].from.id" );
						strRelIdList = domActiveVersionRevisedObj.getInfoList( context , "to[" + strRelAssociatedDrawing + "].id" );
					} else {
						strList = dParentLatestVersionObj.getInfoList( context , "to[" + strRelAssociatedDrawing + "].from.id" );
						strRelIdList = dParentLatestVersionObj.getInfoList( context , "to[" + strRelAssociatedDrawing + "].id" );
					}

					if( !strList.contains( strLatestVersionObjId ) ) {
						if( strRelIdList.size() > 0 ) {
							String[] relidsArray = new String[strRelIdList.size()];
							relidsArray = (String[]) strRelIdList.toArray(relidsArray);
							DomainRelationship.disconnect( context, relidsArray );
						}
						domActiveVersionRevisedObj.connect( context, reltype, from, dParentLatestVersionObj );
					}
					//TIGTK - 18183 : stembulkar : end
                }

            }

            // link files of major to old minor revision
            MCADMxUtil _util = new MCADMxUtil(context, null, null);
            _util.copyFilesFcsSupported(context, domObject, domObjectActiveVersion);

            return true;

        } catch (MatrixException e) {
            MqlUtil.mqlCommand(context, "notice $1", e.getMessage());
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in createNewIteration: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            return false;
        }

    }

    /**
     * @param context
     * @param args
     * @return : Geometry Type attribute range values map
     * @throws Exception
     *             Description: Method to get Geometry Type attributes ranges based on importance and usage of range values : TIGTK-3452
     * @author : steepGraph
     */
    public Map getGeometryTypeRanges(Context context, String[] args) throws Exception {
        HashMap returnMap = new HashMap();

        try {
            StringList fieldRangeValues = new StringList();
            StringList fieldDisplayRangeValues = new StringList();

            // Most used and important ranges
            ResourceBundle iefProps = ResourceBundle.getBundle("emxIEFDesignCenter");
            String strGeometryTypeMostUsedRanges = iefProps.getString("AttributeRange.PSS_GeometryType.MostUsedRanges");

            StringList strGeometryTypeMostUsedRangesList = FrameworkUtil.split(strGeometryTypeMostUsedRanges, ",");

            boolean propertiesMostUsedRanges = false;

            if (strGeometryTypeMostUsedRangesList.size() > 0) {
                propertiesMostUsedRanges = true;
            }

            String strAttributeRange = "";

            if (propertiesMostUsedRanges) {
                for (int i = 0; i < strGeometryTypeMostUsedRangesList.size(); i++) {
                    strAttributeRange = (String) strGeometryTypeMostUsedRangesList.get(i);

                    fieldRangeValues.add(i, strAttributeRange);
                    fieldDisplayRangeValues.add(i, strAttributeRange);
                }
            }

            // All other ranges
            StringList attrRanges = FrameworkUtil.getRanges(context, TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE);
            attrRanges.sort();

            for (int i = 0; i < attrRanges.size(); i++) {
                strAttributeRange = (String) attrRanges.get(i);

                if (propertiesMostUsedRanges) {
                    if (!strGeometryTypeMostUsedRangesList.contains(strAttributeRange)) {
                        fieldRangeValues.add(strAttributeRange);
                        fieldDisplayRangeValues.add(strAttributeRange);
                    } else {
                        // Already added from properties file - do nothing
                    }
                } else {
                    fieldRangeValues.add(strAttributeRange);
                    fieldDisplayRangeValues.add(strAttributeRange);
                }
            }

            returnMap.put("field_choices", fieldRangeValues);
            returnMap.put("field_display_choices", fieldDisplayRangeValues);
        } catch (Exception e) {
            logger.error("Error in getGeometryTypeRanges: ", e);
            throw e;
        }

        return returnMap;
    }

    public void connectLatestChartedDrawingToRevisedPart(Context context, String args[]) throws Exception {
        String strPartObjectId = args[0];

        StringList slObjectSelect = new StringList();
        slObjectSelect.add(DomainConstants.SELECT_ID);

        try {
            DomainObject domObjectPart = DomainObject.newInstance(context, strPartObjectId);

            Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
            typePattern.addPattern(DomainConstants.TYPE_CAD_MODEL);

            String strPartPolicy = domObjectPart.getInfo(context, DomainConstants.SELECT_POLICY);
            String strPartObsoleteState = PropertyUtil.getSchemaProperty(context, "policy", strPartPolicy, "state_Obsolete");
            String strObjWhereExpression = "latest==true && current!=\"" + strPartObsoleteState + "\"";

            MapList mlChartedDrawings = domObjectPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING, typePattern.getPattern(), slObjectSelect, null, false, true,
                    (short) 1, strObjWhereExpression, null, 0);

            String strChartedDrawingId = "";
            // TIGTK-17437 : VISHAL :START
            // String[] strLatestChartedDrawingArray = new String[mlChartedDrawings.size()];
            StringList slLatestCD = new StringList();
            // TIGTK-17437 : VISHAL :END
            if (mlChartedDrawings.size() > 0) {
                for (int i = 0; i < mlChartedDrawings.size(); i++) {
                    Map mpObj = (Map) mlChartedDrawings.get(i);

                    strChartedDrawingId = (String) mpObj.get(DomainConstants.SELECT_ID);

                    DomainObject domChartedDrawing = DomainObject.newInstance(context, strChartedDrawingId);

                    BusinessObject busRevisedObject = domChartedDrawing.getLastRevision(context);

                    strChartedDrawingId = busRevisedObject.getObjectId(context);
                    // TIGTK-17437 : VISHAL :START
                    DomainObject busObj = new DomainObject(strChartedDrawingId);
                    String strCurrentState = busObj.getInfo(context, DomainConstants.SELECT_CURRENT);
                    if (strCurrentState.equalsIgnoreCase("In Work")) {
                        slLatestCD.add(strChartedDrawingId);
                    }
                    // TIGTK-17437 : VISHAL :END
                }
            }

            // Get latest Revised Revision of the Part
            BusinessObject busLastRevision = domObjectPart.getLastRevision(context);

            DomainObject domRevisedPartObject = DomainObject.newInstance(context, busLastRevision);
            logger.info("Latest Revision of the Charted Drawing is connected to Revised Part");
            RelationshipType relType = new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
            // Findbug Issue coorection start
            // Date: 21/03/2017
            // By: Asha G.
            // TIGTK-17437 : VISHAL :START
            if (slLatestCD.size() > 0 && slLatestCD != null) {
                String[] stringArray = (String[]) slLatestCD.toArray(new String[0]);
                domRevisedPartObject.addRelatedObjects(context, relType, true, stringArray);
            }
            // TIGTK-17437 : VISHAL :END
            // Findbug Issue coorection End

        } catch (Exception e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in connectLatestChartedDrawingToRevisedPart: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
        }
    }

    /**
     * This method is used for connect PartSpecification with new revision of CAD PCM : TIGTK-3909 : 06/03/2017 : AB
     * @param context
     * @param args
     *            -CAD object ID
     * @throws Exception
     */
    public void propagatePartSpecificationTonewRev(Context context, Object[] args) throws Exception {
        try {
            StringList BusSelectList = new StringList(DomainConstants.SELECT_ID);

            StringList RelSelectList = new StringList(DomainRelationship.SELECT_ID);
            RelSelectList.addElement(DomainRelationship.SELECT_NAME);

            String strObjectID = (String) args[0]; // Getting the Object id
            String strCOID = (String) args[1]; // Getting the CO Object id
            StringList slObsolescencePart = (StringList) args[2]; // Getting the list for "For Obsolescence Part"
            // Commented for FIndbugReport 26Nov2018
            StringList slForRevisedPart = (StringList) args[3]; // Getting the list for "For Revised Part"

            DomainObject domCO = DomainObject.newInstance(context, strCOID);
            DomainObject domCAD = new DomainObject(strObjectID);
            DomainObject domPrevRevisionOfCAD = new DomainObject(domCAD.getPreviousRevision(context));
            domPrevRevisionOfCAD = getNonCancelledCAD(context, domPrevRevisionOfCAD);
            // Check whether last rev exist
            boolean isRevExist = domPrevRevisionOfCAD.exists(context);
            if (!isRevExist) {
                // Last rev does not exist
                return;
            }
            StringList slConnectedAffectedItemsOfCO = domCO.getInfoList(context,
                    "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].to.from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].to.id");

            // ALM 6251 : Retrive "Requested Change" value for affected Items and check if they are added for "For Clone / For Replace". If yes, then perform replacement of Spec
            // else take latest revision of Affected Item
            String strResult = MqlUtil.mqlCommand(context, "print bus $1 select $2 dump $3", strCOID,
                    "from[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].to.from[" + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + " | attribute["
                            + TigerConstants.ATTRIBUTE_REQUESTED_CHANGE + "]==\"" + TigerConstants.FOR_CLONE + "\" || attribute[" + TigerConstants.ATTRIBUTE_REQUESTED_CHANGE + "]==\""
                            + TigerConstants.FOR_REPLACE + "\"].to.id",
                    "|");

            StringList slConnectedAffectedItemsForCloneOrReplace = FrameworkUtil.split(strResult, "|");
            // If affected Item is CAD then connect with New Implemented Part with new Implemented CAD
            Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
            relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);

            // Get all part id where last revision Object is connected
            MapList relatedPartList = domPrevRevisionOfCAD.getRelatedObjects(context, relPattern.getPattern(), DomainConstants.TYPE_PART, BusSelectList, RelSelectList, true, false, (short) 1, "", "",
                    0);

            if (!relatedPartList.isEmpty()) {
                for (int j = 0; j < relatedPartList.size(); j++) {
                    Hashtable partTable = (Hashtable) relatedPartList.get(j);
                    String strPartId = (String) partTable.get(DomainConstants.SELECT_ID);
                    String strRel = (String) partTable.get("relationship");
                    String strRelId = (String) partTable.get(DomainRelationship.SELECT_ID);
                    DomainObject PartObj = DomainObject.newInstance(context, strPartId);
                    BusinessObject busNextRev = PartObj.getNextRevision(context);
                    int flag = 0;
                    if (!slObsolescencePart.contains(strPartId)) {
                        if (slConnectedAffectedItemsOfCO.contains(strPartId)) {

                            String strWhere = "current!=" + TigerConstants.STATE_PSS_CANCELPART_CANCELLED;
                            StringList slObjectSelects = new StringList(DomainConstants.SELECT_ID);
                            slObjectSelects.add(DomainConstants.SELECT_CURRENT);

                            MapList mlClonedOrReplacedPart = PartObj.getRelatedObjects(context, TigerConstants.RELATIONSHIP_DERIVED, DomainConstants.TYPE_PART, slObjectSelects, null, false, true,
                                    (short) 1, strWhere, null, 0);

                            if (!mlClonedOrReplacedPart.isEmpty() && slConnectedAffectedItemsForCloneOrReplace.contains(strPartId)) {
                                Map mTempMap = (Map) mlClonedOrReplacedPart.get(0);
                                String strClonedOrReplacedPart = (String) mTempMap.get(DomainConstants.SELECT_ID);
                                DomainObject domClonedOrReplacedPart = DomainObject.newInstance(context, strClonedOrReplacedPart);

                                domClonedOrReplacedPart.addRelatedObject(context, new RelationshipType(strRel), false, strObjectID);
                            } else {
                                // Cancel CO For Revise
                                flag = 1;
                            }
                        } else {
                            /*
                             * When Part is Revised a Trigger connects its Charted Drawing to Revised Part. But when CO Is promoted to In Work and Part and its Charted Drawing are set as For Revise
                             * then Part gets Revised First so it connects Revision 02 of Part with Charted Drawing A as Charted Drawing is not yet Revised by system. So we will disconnect it here
                             */

                            if (strRel.equalsIgnoreCase(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING) && slForRevisedPart.contains(strPartId)) {
                                DomainRelationship.disconnect(context, strRelId);

                            }

                        }
                        if (flag == 1) {
                            BusinessObject boLastRevOfPart = (BusinessObject) PartObj.getLastRevision(context);
                            DomainObject domLastRevOfPart = new DomainObject(boLastRevOfPart);
                            if (UIUtil.isNullOrEmpty(busNextRev.getObjectId())) {
                                DomainRelationship.disconnect(context, strRelId);
                            }
                            String strRelIdExists = MqlUtil.mqlCommand(context,
                                    "print bus " + (String) domLastRevOfPart.getObjectId() + " select from[" + strRel + "| to.id == '" + strObjectID + "'].id dump", false, false);
                            if (UIUtil.isNullOrEmpty(strRelIdExists)) {
                                domLastRevOfPart.addRelatedObject(context, new RelationshipType(strRel), false, strObjectID);
                            }

                        }

                    }

                }
            }
        } catch (MatrixException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in propagatePartSpecificationTonewRev: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    private DomainObject getNonCancelledCAD(Context context, DomainObject domPrevRevision) throws Exception {
        // TODO Auto-generated method stub
        DomainObject domCAD = domPrevRevision;
        String strPolicy = domCAD.getInfo(context, DomainConstants.SELECT_POLICY);
        while (strPolicy.equalsIgnoreCase(TigerConstants.POLICY_PSS_CANCELCAD)) {
            domCAD = new DomainObject(domCAD.getPreviousRevision(context));
            strPolicy = domCAD.getInfo(context, DomainConstants.SELECT_POLICY);
        }
        return domCAD;
    }

    /**
     * This method is used for connect PartSpecification with new revision of CAD & connect CAD's new and old revision. PCM : TIGTK-3909 : 20/02/2017 : AB
     * @param context
     * @param args
     *            -CAD object ID
     * @throws Exception
     */
    public void floatPartSpecification(Context context, String[] args) throws Exception {
        try {
            StringList BusSelectList = new StringList(DomainConstants.SELECT_ID);
            StringList relSelectList = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);

            String strObjectID = args[0]; // Getting the Object id
            DomainObject domCAD = new DomainObject(strObjectID);
            DomainObject domLastRevision = new DomainObject(domCAD.getPreviousRevision(context));

            // Check whether last rev exist
            boolean isRevExist = domLastRevision.exists(context);
            if (!isRevExist) {
                // Last rev does not exist
                return;
            }

            // TIGTK-6843 : PKH : START
            String strPolicy = domLastRevision.getPolicy(context).getName();
            while (TigerConstants.POLICY_PSS_CANCELCAD.equals(strPolicy)) {
                BusinessObject boObjectCAD = domLastRevision.getPreviousRevision(context);
                String strObjId = boObjectCAD.getObjectId(context);
                domLastRevision = DomainObject.newInstance(context, strObjId);
                strPolicy = domLastRevision.getPolicy(context).getName();
            }
            // TIGTK-6843 : PKH : END

            // Connect All old relationship of old CAD's with new revision of CAD
            String strRelPartSpecification = MCADMxUtil.getActualNameForAEFData(context, "relationship_PartSpecification");
            MapList relatedPartList = domLastRevision.getRelatedObjects(context, strRelPartSpecification, DomainConstants.QUERY_WILDCARD, BusSelectList, relSelectList, true, false, (short) 1, "", "",
                    0);
            if (!relatedPartList.isEmpty()) {
                for (int k = 0; k < relatedPartList.size(); k++) {
                    Hashtable partTable = (Hashtable) relatedPartList.get(k);
                    String strPartId = (String) partTable.get(DomainConstants.SELECT_ID);
                    String strOldRelID = (String) partTable.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    // ALM3919 : START
                    // connect CAD with CAD using CADSubComponent relationship
                    DomainObject domPartObj = new DomainObject(strPartId);
                    boolean bolLatestPart = domPartObj.isLastRevision(context);
                    if (bolLatestPart) {
                        DomainRelationship.setToObject(context, strOldRelID, domCAD);
                    }
                    // ALM3919 : END
                }
            }
        } catch (MatrixException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in propagateCADSubComponentTonewRev: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    public void propagateCADSubComponentTonewRev(Context context, String[] args) throws Exception {
        try {
            StringList BusSelectList = new StringList(DomainConstants.SELECT_ID);
            StringList relSelectList = new StringList(DomainConstants.SELECT_RELATIONSHIP_ID);

            String strObjectID = args[0]; // Getting the Object id
            DomainObject domCAD = new DomainObject(strObjectID);
            DomainObject domLastRevision = new DomainObject(domCAD.getPreviousRevision(context));

            // Check whether last rev exist
            boolean isRevExist = domLastRevision.exists(context);
            if (!isRevExist) {
                // Last rev does not exist
                return;
            }

            // TIGTK-6843 : PKH : START
            String strPolicy = domLastRevision.getPolicy(context).getName();
            while (TigerConstants.POLICY_PSS_CANCELCAD.equals(strPolicy)) {
                BusinessObject boObjectCAD = domLastRevision.getPreviousRevision(context);
                String strObjId = boObjectCAD.getObjectId(context);
                domLastRevision = DomainObject.newInstance(context, strObjId);
                strPolicy = domLastRevision.getPolicy(context).getName();
            }
            // TIGTK-6843 : PKH : END

            // Connect All old relationship of old CAD's with new revision of CAD
            String strRelCADSubComponent = MCADMxUtil.getActualNameForAEFData(context, "relationship_CADSubComponent");
            MapList relatedCADList = domLastRevision.getRelatedObjects(context, strRelCADSubComponent, DomainConstants.QUERY_WILDCARD, BusSelectList, relSelectList, true, false, (short) 1, "", "", 0);

            if (!relatedCADList.isEmpty()) {
                for (int k = 0; k < relatedCADList.size(); k++) {
                    Hashtable partTable = (Hashtable) relatedCADList.get(k);
                    String strCADId = (String) partTable.get(DomainConstants.SELECT_ID);
                    String strOldRelID = (String) partTable.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                    // ALM3919 : START
                    // connect CAD with CAD using CADSubComponent relationship
                    DomainObject domCADObj = new DomainObject(strCADId);
                    boolean bolLatestCAD = domCADObj.isLastRevision(context);
                    if (bolLatestCAD) {
                        DomainRelationship.setToObject(context, strOldRelID, domCAD);
                    }
                    // ALM3919 : END
                }
            }
        } catch (MatrixException e) {
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - START
            logger.error("Error in propagateCADSubComponentTonewRev: ", e);
            // TIGTK-5405 - 13-04-2017 - Rutuja Ekatpure - End
            throw e;
        }
    }

    // TIGTK-6912 - VP - 2017-05-19 - START
    /**
     * This method is used to check For MG Geometry Type Before Specification or Charted Drawing Delete
     * @param context
     * @param args
     * @throws Exception
     * @author VP
     */
    public int checkForMGGeometryTypeBeforeSpecificationDelete(Context context, String[] args) throws Exception {
        int iReturn = 0;
        StringBuffer strErrorMsg = new StringBuffer();
        try {
            String strPerformTransitionOnCOPromote = PropertyUtil.getGlobalRPEValue(context, "performTransitionOnCOPromote");

            if (UIUtil.isNotNullAndNotEmpty(strPerformTransitionOnCOPromote) && strPerformTransitionOnCOPromote.equals("True")) {
                return iReturn;
            }
            // TIGTK-13964 : If Relationship is disconnecting from Change Management "Cancel CO" Process then ignore CAD2D-3D rule
            String isPromoteFromCOAction = context.getCustomData("isPromoteFromCOAction");
            if (UIUtil.isNotNullAndNotEmpty(isPromoteFromCOAction))
                return 0;
            String strToObjectID = args[1];// Get the to Object id for Part Specification
            String strFromObjectID = args[0];// Get the from Object id for Part
            if (UIUtil.isNotNullAndNotEmpty(strFromObjectID) && UIUtil.isNotNullAndNotEmpty(strToObjectID)) {
                DomainObject domFromObject = DomainObject.newInstance(context, strFromObjectID);
                DomainObject domToObject = DomainObject.newInstance(context, strToObjectID);

                String strGeometryTypeOfToObject = domToObject.getInfo(context, DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE));

                if (strGeometryTypeOfToObject.equals(TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE)) {
                    StringList lstselectStmts = new StringList(1);
                    lstselectStmts.addElement(DomainConstants.SELECT_ID);

                    StringList lstrelStmts = new StringList();
                    lstrelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

                    Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_MODEL);

                    Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);

                    if (domToObject.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {
                        typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
                        relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
                    }
                    if (domToObject.isKindOf(context, DomainConstants.TYPE_CAD_MODEL)) {
                        typePattern = new Pattern(DomainConstants.TYPE_CAD_MODEL);
                        // TIGTK-12896 : stembulkar : start
                        relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
                        // TIGTK-12896 : stembulkar : end
                    }

                    String sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]== \"" + TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE + "\" )";

                    MapList mlCADObject = domFromObject.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), lstselectStmts, lstrelStmts, false, true, (short) 1, sClause,
                            null, 0);

                    if (mlCADObject.size() < 2) {
                        sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]!= \"" + TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE + "\" )";

                        mlCADObject = domFromObject.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), lstselectStmts, lstrelStmts, false, true, (short) 1, sClause, null,
                                0);
                        if (mlCADObject.size() > 0) {
                            strErrorMsg.append("CAD Objects removal failed. MG CAD Object should be the last to be removed.");
                            iReturn = 1;
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in checkForMGGeometryTypeBeforeSpecificationDelete: ", e);
            throw e;
        }
        try {
            if (iReturn == 1) {
                String strStatus = context.getCustomData("PSS_THROW_EXCEPTION");
                if (UIUtil.isNotNullAndNotEmpty(strStatus) && strStatus.equals("TRUE")) {
                    throw new Exception(strErrorMsg.toString());
                } else {
                    MqlUtil.mqlCommand(context, "notice $1", strErrorMsg.toString());
                    iReturn = 1;
                }

            }
        } catch (Exception ex) {
            logger.error("Error in checkForMGGeometryTypeBeforeSpecificationDelete: ", ex);
            throw ex;
        }
        return iReturn;
    }

    /**
     * This method is used to check For MG Geometry Type Before Specification or Charted Drawing Create
     * @param context
     * @param args
     * @throws Exception
     * @author VP
     */
    public int checkForMGGeometryTypeBeforeSpecificationCreate(Context context, String[] args) throws Exception {
        int iReturn = 0;
        StringBuffer strErrorMsg = new StringBuffer();
        try {
            // TIGTK-10713 : Skip MG check for "Go To Production" functionality : START
            String strIsGoToProduction = context.getCustomData("BOM_GO_TO_PRODUCTION");
            if (UIUtil.isNotNullAndNotEmpty(strIsGoToProduction) && strIsGoToProduction.equals("TRUE")) {
                iReturn = 0;
                return iReturn;
            }
            // TIGTK-10713 : END

            String strReviseOrCloneStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_REVISE_FROM_PCM");

            if (UIUtil.isNotNullAndNotEmpty(strReviseOrCloneStatus) && strReviseOrCloneStatus.equals("True")) {
                iReturn = 0;
                return iReturn;
            }

            String strReviseStatus = context.getCustomData("PSS_PART_REVISE");
            String strToObjectID = args[1];// Get the to Object id for Part Specification
            String strFromObjectID = args[0];// Get the from Object id for Part
            String strCloneEBOM = PropertyUtil.getGlobalRPEValue(context, "PSS_CloneEBOM");
            // TIGTK-8886 - PTE - 2017-07-7 - START
            // TIGTK- 8812 - Modified on 6/7/2017 by SIE :Start
            if (UIUtil.isNotNullAndNotEmpty(strReviseStatus) && strReviseStatus.equals("TRUE")) {
                iReturn = 0;
                return iReturn;
            } else if (UIUtil.isNotNullAndNotEmpty(strCloneEBOM) && strCloneEBOM.equals("CloneEBOM")) {
                iReturn = 0;
                return iReturn;
            } // TIGTK- 8812 - Modified on 6/7/2017 by SIE :End// TIGTK-8886 - PTE - 2017-07-7 - END
            else if (UIUtil.isNotNullAndNotEmpty(strFromObjectID) && UIUtil.isNotNullAndNotEmpty(strToObjectID)) {
                DomainObject domFromObject = DomainObject.newInstance(context, strFromObjectID);
                DomainObject domToObject = DomainObject.newInstance(context, strToObjectID);

                String strGeometryTypeOfToObject = domToObject.getInfo(context, DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE));
                if (strGeometryTypeOfToObject.equals("BD")) {
                    strErrorMsg.append("BD type in not valid");
                    iReturn = 1;

                } else {
                    StringList lstselectStmts = new StringList(1);
                    lstselectStmts.addElement(DomainConstants.SELECT_ID);
                    // TIGTK-8104 : START
                    lstselectStmts.addElement(DomainConstants.SELECT_NAME);
                    lstselectStmts.addElement(DomainConstants.SELECT_REVISION);
                    // TIGTK-8104 : END

                    StringList lstrelStmts = new StringList();
                    lstrelStmts.add(DomainConstants.SELECT_RELATIONSHIP_ID);

                    Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_MODEL);

                    Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);

                    if (domToObject.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {
                        typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
                        relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
                    }
                    if (domToObject.isKindOf(context, DomainConstants.TYPE_CAD_MODEL)) {
                        typePattern = new Pattern(DomainConstants.TYPE_CAD_MODEL);
                        // TIGTK-12896 : start
                        relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
                        // TIGTK-12896 : end
                    }

                    String sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]== \"" + TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE + "\" )";

                    MapList mlCADObject = domFromObject.getRelatedObjects(context, relPattern.getPattern(), typePattern.getPattern(), lstselectStmts, lstrelStmts, false, true, (short) 1, sClause,
                            null, 0);

                    if (strGeometryTypeOfToObject.equals(TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE)) {
                        // TIGTK-8104 : START
                        String strToObjectName = domToObject.getInfo(context, DomainConstants.SELECT_NAME);
                        String strToObjectRevision = domToObject.getInfo(context, DomainConstants.SELECT_REVISION);
                        // TIGTK-8104 : END
                        if (mlCADObject.size() > 0) {
                            // TIGTK-8104 : START
                            for (int a = 0; a < mlCADObject.size(); a++) {
                                Map mCADObj = (Map) mlCADObject.get(a);
                                String strName = (String) mCADObj.get(DomainConstants.SELECT_NAME);
                                String strRevision = (String) mCADObj.get(DomainConstants.SELECT_REVISION);
                                if (strToObjectName.equals(strName) && !strToObjectRevision.equals(strRevision)) {
                                    iReturn = 0;
                                } else {
                                    // TIGTK-12896 : start
                                    strErrorMsg.append(
                                            "The Part is already linked to MG CAD Object of the same type, no more MG CAD Object of that type can be connected to this Part. Please refine your selection");
                                    // TIGTK-12896 : end
                                    iReturn = 1;
                                    // TIGTK-12896 : start
                                    break;
                                    // TIGTK-12896 : end
                                }
                            }
                            // TIGTK-8104 : END
                        } else {
                            iReturn = 0;
                        }
                    } else {
                        if (mlCADObject.size() < 1) {
                            strErrorMsg.append("Part does not have any Part Specification of same type with Geometry Type MG");
                            iReturn = 1;
                        } else {
                            iReturn = 0;
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in checkForMGGeometryTypeBeforeSpecificationCreate: ", e);
            throw e;
        }

        try {
            if (iReturn == 1) {
                String strStatus = context.getCustomData("PSS_THROW_EXCEPTION");
                if (UIUtil.isNotNullAndNotEmpty(strStatus) && strStatus.equals("TRUE")) {
                    throw new Exception(strErrorMsg.toString());
                } else {
                    MqlUtil.mqlCommand(context, "notice $1", strErrorMsg.toString());
                    iReturn = 1;
                }
            }
        } catch (Exception ex) {
            logger.error("Error in checkForMGGeometryTypeBeforeSpecificationCreate: ", ex);
            throw ex;
        }
        return iReturn;
    }
    // TIGTK-6912 - VP - 2017-05-19 - END

    // TIGTK-8886 - PTE - 2017-07-7 - START

    public int checkForMGGeometryTypeBeforeSpecificationModified(Context context, String[] args) throws Exception {
        int iReturn = 0;
        StringBuffer sbErrorMsg = new StringBuffer();
        StringBuffer sbPartListFailed = new StringBuffer();
        boolean bdTrue = false;
        try {
            String strCADObjectID = args[0];// Get the to Object id for Part Specification
            String newGeometryType = args[1];
            if (UIUtil.isNotNullAndNotEmpty(strCADObjectID)) {
                DomainObject domCADObject = DomainObject.newInstance(context, strCADObjectID);
                String strOldAttributeValue = domCADObject.getInfo(context, DomainObject.getAttributeSelect(TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE));
                StringList lstselectStmts = new StringList(3);
                lstselectStmts.addElement(DomainConstants.SELECT_ID);
                lstselectStmts.addElement(DomainConstants.SELECT_TYPE);
                // TIGTK-8104 : START
                lstselectStmts.addElement(DomainConstants.SELECT_NAME);
                lstselectStmts.addElement(DomainConstants.SELECT_REVISION);
                // TIGTK-8104 : END

                Pattern relPattern = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);
                relPattern.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);

                MapList mlPArtObject = domCADObject.getRelatedObjects(context, relPattern.getPattern(), "*", lstselectStmts, DomainConstants.EMPTY_STRINGLIST, true, false, (short) 1, "", null, 0);
                int iConnectedPartCount = mlPArtObject.size();
                if (UIUtil.isNotNullAndNotEmpty(newGeometryType) && newGeometryType.equals("BD") && mlPArtObject.size() > 0) {

                    iReturn = 1;
                    bdTrue = true;

                } else if (strOldAttributeValue.equals(TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE)) {
                    for (int i = 0; i < iConnectedPartCount; i++) {

                        Map mapConnectedPart = (Map) mlPArtObject.get(i);
                        String strConnectedPartId = (String) mapConnectedPart.get(DomainConstants.SELECT_ID);
                        DomainObject domPartObject = DomainObject.newInstance(context, strConnectedPartId);
                        String strPartName = (String) mapConnectedPart.get(DomainConstants.SELECT_NAME);
                        String strPartType = (String) mapConnectedPart.get(DomainConstants.SELECT_TYPE);
                        String strPartRevision = (String) mapConnectedPart.get(DomainConstants.SELECT_REVISION);

                        Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_MODEL);

                        Pattern relPattern1 = new Pattern(DomainConstants.RELATIONSHIP_PART_SPECIFICATION);

                        if (domCADObject.isKindOf(context, DomainConstants.TYPE_CAD_DRAWING)) {
                            typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
                            relPattern1.addPattern(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
                        }
                        if (domCADObject.isKindOf(context, DomainConstants.TYPE_CAD_MODEL)) {
                            typePattern = new Pattern(DomainConstants.TYPE_CAD_MODEL);
                        }

                        String sClause = "(attribute[" + TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE + "]== \"" + TigerConstants.ATTR_RANGE_PSSGEOMETRYTYPE + "\" )";
                        MapList mlCADObject = domPartObject.getRelatedObjects(context, relPattern1.getPattern(), typePattern.getPattern(), lstselectStmts, null, false, true, (short) 0, sClause, null,
                                0);
                        if (!mlCADObject.isEmpty()) {
                            sbPartListFailed.append(strPartType);
                            sbPartListFailed.append(" ");
                            sbPartListFailed.append(strPartName);
                            sbPartListFailed.append(" ");
                            sbPartListFailed.append(strPartRevision);
                            if (!(i == iConnectedPartCount - 1)) {
                                sbPartListFailed.append(",");

                            }
                            iReturn = 1;
                        }

                    }

                }
            }

        } catch (Exception e) {
            logger.error("Error in checkForMGGeometryTypeBeforeSpecificationModified: ", e);
        }
        try {
            if (iReturn == 1) {
                String strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                        "emxEngineeringCentral.PartSpecification.GeometryTypeUpdationError");
                if (bdTrue)
                    strErrorMsg = "can not modify attribute Geometry Type as BD, BD type is not valid";

                String strStatus = context.getCustomData("PSS_THROW_EXCEPTION");
                if (UIUtil.isNotNullAndNotEmpty(strStatus) && strStatus.equals("TRUE")) {
                    sbErrorMsg.append(strErrorMsg);
                    sbErrorMsg.append(sbPartListFailed);
                    throw new Exception(sbErrorMsg.toString());
                } else {
                    sbErrorMsg.append(strErrorMsg);
                    sbErrorMsg.append(sbPartListFailed);
                    MqlUtil.mqlCommand(context, "notice $1", sbErrorMsg.toString());
                    iReturn = 1;
                }
            }
        } catch (Exception ex) {
            logger.error("Error in checkForMGGeometryTypeBeforeSpecificationCreate: ", ex);
            throw ex;
        }
        return iReturn;

    }
    // TIGTK-8886 - PTE - 2017-07-7 - END

    // TIGTK-8336 - PTE - 2017-15-7 - START

    /**
     * This method is used to check For Collabrative Space Before Specification or Charted Drawing Create
     * @param context
     * @param args
     * @throws Exception
     * @author PTE
     */
    public int checkCSOfPartAndCAD(Context context, String[] args) throws Exception {
        int iReturn = 0;
        StringBuffer sbErrorMsg = new StringBuffer();
        StringBuffer sbPartListFailed = new StringBuffer();
        StringBuffer sbCADListFailed = new StringBuffer();
        String strLocale = context.getSession().getLanguage();
        try {
            String strToObjectID = args[1];// Get the to Object id for Part Specification
            String strFromObjectID = args[0];// Get the from Object id for Part
            if (UIUtil.isNotNullAndNotEmpty(strFromObjectID) && UIUtil.isNotNullAndNotEmpty(strToObjectID)) {
                DomainObject domFromObject = DomainObject.newInstance(context, strFromObjectID);
                DomainObject domToObject = DomainObject.newInstance(context, strToObjectID);
                BusinessObject busObjCAD = new BusinessObject(strToObjectID);
                BusinessObject busObjPart = new BusinessObject(strFromObjectID);

                User strCADProjectName = busObjCAD.getProjectOwner(context);
                User strPartProjectName = busObjPart.getProjectOwner(context);
                if (UIUtil.isNotNullAndNotEmpty(strPartProjectName.toString()) && UIUtil.isNotNullAndNotEmpty(strCADProjectName.toString())
                        && !strPartProjectName.toString().equals(strCADProjectName.toString())) {
                    String strPartName = (String) domFromObject.getInfo(context, DomainConstants.SELECT_NAME);
                    String strPartType = (String) domFromObject.getInfo(context, DomainConstants.SELECT_TYPE);
                    String strPartRevision = (String) domFromObject.getInfo(context, DomainConstants.SELECT_REVISION);
                    String strCADName = (String) domToObject.getInfo(context, DomainConstants.SELECT_NAME);
                    String strCADType = (String) domToObject.getInfo(context, DomainConstants.SELECT_TYPE);
                    String strCADRevision = (String) domToObject.getInfo(context, DomainConstants.SELECT_REVISION);
                    sbPartListFailed.append("'");
                    sbPartListFailed.append(i18nNow.getTypeI18NString(strPartType, strLocale));
                    sbPartListFailed.append(" ");
                    sbPartListFailed.append(strPartName);
                    sbPartListFailed.append(" ");
                    sbPartListFailed.append(strPartRevision);
                    sbPartListFailed.append("'");
                    sbCADListFailed.append("'");
                    sbCADListFailed.append(i18nNow.getTypeI18NString(strCADType, strLocale));
                    sbCADListFailed.append(" ");
                    sbCADListFailed.append(strCADName);
                    sbCADListFailed.append(" ");
                    sbCADListFailed.append(strCADRevision);
                    sbCADListFailed.append("'");
                    iReturn = 1;

                }

            }
        } catch (RuntimeException e) {
            logger.error("Error in checkCSOfPartAndCAD: ", e);

        } catch (Exception e) {
            logger.error("Error in checkCSOfPartAndCAD: ", e);
            throw e;
        }

        try {
            if (iReturn == 1) {
                String strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                        "emxEngineeringCentral.PartSpecification.DifferentCollabSpace");
                strErrorMsg = strErrorMsg.replace("<Part_TNR>", sbPartListFailed);
                strErrorMsg = strErrorMsg.replace("<CAD_TNR>", sbCADListFailed);
                sbErrorMsg.append(strErrorMsg);
                String strStatus = context.getCustomData("PSS_THROW_EXCEPTION");
                if (UIUtil.isNotNullAndNotEmpty(strStatus) && strStatus.equals("TRUE")) {
                    throw new Exception(sbErrorMsg.toString());
                } else {
                    MqlUtil.mqlCommand(context, "notice $1", sbErrorMsg.toString());
                    iReturn = 1;
                }
            }
        } catch (Exception ex) {
            logger.error("Error in checkCSOfPartAndCAD: ", ex);
            throw ex;
        }
        return iReturn;
    }

    // TIGTK-8336 - PTE - 2017-15-7 - END

    // TIGTK-8543 - PTE - 2017-7-18 - START

    /**
     * This method is used to check CAD object Connected to EC and STD part before Promote manually In Work to Review
     * @param context
     * @param args
     * @throws Exception
     * @author PTE
     */
    public int checkCADConnectedToECAndSTDPartBeforePromoteToReview(Context context, String[] args) throws Exception {
        int iReturn = 0;
        StringBuffer sbErrorMsg = new StringBuffer();
        StringBuffer sbPartList = new StringBuffer();
        try {
            String strCADObjectID = args[0];// Get the to Object id for Part Specification
            DomainObject domCADObject = DomainObject.newInstance(context, strCADObjectID);
            String strCADPolicy = domCADObject.getInfo(context, DomainConstants.SELECT_POLICY);
            if (UIUtil.isNotNullAndNotEmpty(strCADPolicy) && strCADPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {

                String objectWhere = "policy=='" + TigerConstants.POLICY_PSS_ECPART + "'" + "||" + "policy=='" + TigerConstants.POLICY_STANDARDPART + "'";
                StringList slOBJsel = new StringList();
                slOBJsel.add(DomainConstants.SELECT_ID);
                slOBJsel.add(DomainConstants.SELECT_NAME);
                slOBJsel.add(DomainConstants.SELECT_REVISION);
                slOBJsel.add(DomainConstants.SELECT_TYPE);

                StringList slRelSle = new StringList(1);
                slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                MapList mlConnectedParts = domCADObject.getRelatedObjects(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship pattern
                        DomainConstants.TYPE_PART, // object pattern
                        slOBJsel, // object selects
                        slRelSle, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 1, // recursion level
                        objectWhere, // object where clause
                        null, 0); // relationship where clause
                int iConnectedParts = mlConnectedParts.size();

                if (iConnectedParts > 0 && !mlConnectedParts.isEmpty()) {
                    String strRelPattern = ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM;

                    String selectCoIsActive = new StringBuilder("evaluate[(to[").append(strRelPattern).append("].from.to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION)
                            .append("].from.current smatchlist \"Prepare,In Work,In Approval\" \",\")]").toString();

                    boolean isConnectedActiveCO = Boolean.valueOf((String) domCADObject.getInfo(context, selectCoIsActive));
                    if (!isConnectedActiveCO) {
                        Iterator itrPart = mlConnectedParts.iterator();
                        while (itrPart.hasNext()) {
                            Map mpPart = (Map) itrPart.next();
                            String strPartName = (String) mpPart.get(DomainConstants.SELECT_NAME);
                            String strPartType = (String) mpPart.get(DomainConstants.SELECT_TYPE);
                            String strPartRevision = (String) mpPart.get(DomainConstants.SELECT_REVISION);
                            sbPartList.append("\n");
                            sbPartList.append(strPartType);
                            sbPartList.append(" ");
                            sbPartList.append(strPartName);
                            sbPartList.append(" ");
                            sbPartList.append(strPartRevision);

                        }
                        String strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                                "emxEngineeringCentral.PartSpecification.ECandStdConnected");
                        String strCADName = domCADObject.getInfo(context, DomainConstants.SELECT_NAME);
                        strErrorMsg = strErrorMsg.replace("<$name>", strCADName);
                        sbErrorMsg.append(strErrorMsg);
                        sbErrorMsg.append(sbPartList);
                        MqlUtil.mqlCommand(context, "notice $1", sbErrorMsg.toString());
                        iReturn = 1;
                        return iReturn;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in checkCADConnectedToECAndSTDPartBeforePromoteToReview: ", e);
            throw e;
        }
        return iReturn;
    }

    /**
     * This method is used to check CAD object Connected to EC and STD part before Promote manually Review to Approve and Approve to Released
     * @param context
     * @param args
     * @throws Exception
     * @author PTE
     */
    public int checkCADConnectedToECAndSTDPartBeforePromote(Context context, String[] args) throws Exception {
        int iReturn = 0;
        StringBuffer sbErrorMsg = new StringBuffer();
        StringBuffer sbPartList = new StringBuffer();

        try {
            String strPromoteStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_PROMOTE_FROM_PCM");
            if (UIUtil.isNotNullAndNotEmpty(strPromoteStatus) && strPromoteStatus.equals("True")) {
                iReturn = 0;
                return iReturn;
            }
            String strCADObjectID = args[0];// Get the to Object id for Part Specification
            DomainObject domCADObject = DomainObject.newInstance(context, strCADObjectID);
            String strCADPolicy = domCADObject.getInfo(context, DomainConstants.SELECT_POLICY);
            if (UIUtil.isNotNullAndNotEmpty(strCADPolicy) && strCADPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {

                String objectWhere = "policy=='" + TigerConstants.POLICY_PSS_ECPART + "'" + "||" + "policy=='" + TigerConstants.POLICY_STANDARDPART + "'";
                StringList slOBJsel = new StringList();
                slOBJsel.add(DomainConstants.SELECT_ID);
                slOBJsel.add(DomainConstants.SELECT_NAME);
                slOBJsel.add(DomainConstants.SELECT_REVISION);
                slOBJsel.add(DomainConstants.SELECT_TYPE);

                StringList slRelSle = new StringList(1);
                slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                MapList mlConnectedParts = domCADObject.getRelatedObjects(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship pattern
                        DomainConstants.TYPE_PART, // object pattern
                        slOBJsel, // object selects
                        slRelSle, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 1, // recursion level
                        objectWhere, // object where clause
                        null, 0); // relationship where clause
                int iConnectedParts = mlConnectedParts.size();
                if (iConnectedParts > 0 && !mlConnectedParts.isEmpty()) {
                    Iterator itrPart = mlConnectedParts.iterator();
                    while (itrPart.hasNext()) {
                        Map mpPart = (Map) itrPart.next();
                        String strPartName = (String) mpPart.get(DomainConstants.SELECT_NAME);
                        String strPartType = (String) mpPart.get(DomainConstants.SELECT_TYPE);
                        String strPartRevision = (String) mpPart.get(DomainConstants.SELECT_REVISION);
                        sbPartList.append("\n");
                        sbPartList.append(strPartType);
                        sbPartList.append(" ");
                        sbPartList.append(strPartName);
                        sbPartList.append(" ");
                        sbPartList.append(strPartRevision);

                        String strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                                "emxEngineeringCentral.PartSpecification.ECandStdConnected");
                        String strCADName = domCADObject.getInfo(context, DomainConstants.SELECT_NAME);
                        strErrorMsg = strErrorMsg.replace("<$name>", strCADName);
                        sbErrorMsg.append(strErrorMsg);
                        sbErrorMsg.append(sbPartList);
                        MqlUtil.mqlCommand(context, "notice $1", sbErrorMsg.toString());
                        iReturn = 1;
                        return iReturn;
                    }
                } else {

                    String strRelPattern = ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM;

                    String selectCoIsActive = new StringBuilder("evaluate[(to[").append(strRelPattern).append("].from.to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION)
                            .append("].from.current smatchlist \"Prepare,In Work,In Approval\" \",\")]").toString();

                    boolean isConnectedActiveCO = Boolean.valueOf((String) domCADObject.getInfo(context, selectCoIsActive));
                    if (isConnectedActiveCO) {

                        String strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                                "emxEngineeringCentral.PartSpecification.ConnectedTOPCM");
                        String strCADName = domCADObject.getInfo(context, DomainConstants.SELECT_NAME);
                        strErrorMsg = strErrorMsg.replace("<$name>", strCADName);
                        sbErrorMsg.append(strErrorMsg);
                        MqlUtil.mqlCommand(context, "notice $1", sbErrorMsg.toString());
                        iReturn = 1;
                        return iReturn;

                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in checkCADConnectedToECAndSTDPartBeforePromote: ", e);
            throw e;
        }
        return iReturn;
    }

    /**
     * This method is used to check CAD object Connected to EC and STD part before Revise manually
     * @param context
     * @param args
     * @throws Exception
     * @author PTE
     */
    public int checkCADConnectedToECAndSTDPartBeforeRevise(Context context, String[] args) throws Exception {
        int iReturn = 0;
        StringBuffer sbErrorMsg = new StringBuffer();
        StringBuffer sbPartList = new StringBuffer();

        try {
            String strReviseStatus = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_REVISE_FROM_PCM");

            if (UIUtil.isNotNullAndNotEmpty(strReviseStatus) && strReviseStatus.equals("True")) {
                iReturn = 0;
                return iReturn;
            }
            String strCADObjectID = args[0];// Get the to Object id for Part Specification
            String strCADPolicy = args[1];
            DomainObject domCADObject = DomainObject.newInstance(context, strCADObjectID);
            if (UIUtil.isNotNullAndNotEmpty(strCADPolicy) && strCADPolicy.equals(TigerConstants.POLICY_PSS_CADOBJECT)) {

                String objectWhere = "policy=='" + TigerConstants.POLICY_PSS_ECPART + "'" + "||" + "policy=='" + TigerConstants.POLICY_STANDARDPART + "'";
                StringList slOBJsel = new StringList();
                slOBJsel.add(DomainConstants.SELECT_ID);
                slOBJsel.add(DomainConstants.SELECT_NAME);
                slOBJsel.add(DomainConstants.SELECT_REVISION);
                slOBJsel.add(DomainConstants.SELECT_TYPE);

                StringList slRelSle = new StringList(1);
                slRelSle.addElement(DomainRelationship.SELECT_RELATIONSHIP_ID);

                MapList mlConnectedParts = domCADObject.getRelatedObjects(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, // relationship pattern
                        DomainConstants.TYPE_PART, // object pattern
                        slOBJsel, // object selects
                        slRelSle, // relationship selects
                        true, // to direction
                        false, // from direction
                        (short) 1, // recursion level
                        objectWhere, // object where clause
                        null, 0); // relationship where clause
                int iConnectedParts = mlConnectedParts.size();

                if (iConnectedParts > 0 && !mlConnectedParts.isEmpty()) {
                    Iterator itrPart = mlConnectedParts.iterator();
                    while (itrPart.hasNext()) {
                        Map mpPart = (Map) itrPart.next();
                        String strPartName = (String) mpPart.get(DomainConstants.SELECT_NAME);
                        String strPartType = (String) mpPart.get(DomainConstants.SELECT_TYPE);
                        String strPartRevision = (String) mpPart.get(DomainConstants.SELECT_REVISION);
                        sbPartList.append("\n");
                        sbPartList.append(strPartType);
                        sbPartList.append(" ");
                        sbPartList.append(strPartName);
                        sbPartList.append(" ");
                        sbPartList.append(strPartRevision);

                        String strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                                "emxEngineeringCentral.PartSpecification.ECandStdConnectedForRevise");
                        String strCADName = domCADObject.getInfo(context, DomainConstants.SELECT_NAME);
                        strErrorMsg = strErrorMsg.replace("<$name>", strCADName);
                        sbErrorMsg.append(strErrorMsg);
                        sbErrorMsg.append(sbPartList);
                        MqlUtil.mqlCommand(context, "notice $1", sbErrorMsg.toString());
                        iReturn = 1;
                        return iReturn;
                    }
                } else {

                    String strRelPattern = ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "," + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM;

                    String selectCoIsActive = new StringBuilder("evaluate[(to[").append(strRelPattern).append("].from.to[").append(ChangeConstants.RELATIONSHIP_CHANGE_ACTION)
                            .append("].from.current smatchlist \"Prepare,In Work,In Approval\" \",\")]").toString();

                    boolean isConnectedActiveCO = Boolean.valueOf((String) domCADObject.getInfo(context, selectCoIsActive));
                    if (isConnectedActiveCO) {

                        String strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                                "emxEngineeringCentral.PartSpecification.ConnectedTOPCMForRevise");
                        String strCADName = domCADObject.getInfo(context, DomainConstants.SELECT_NAME);
                        strErrorMsg = strErrorMsg.replace("<$name>", strCADName);
                        sbErrorMsg.append(strErrorMsg);
                        MqlUtil.mqlCommand(context, "notice $1", sbErrorMsg.toString());
                        iReturn = 1;
                        return iReturn;

                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error in checkCADConnectedToECAndSTDPartBeforeRevise: ", e);
            throw e;
        }
        return iReturn;
    }

    /**
     * This method is used to check Approved CAD object Connected to EC or STD part
     * @param context
     * @param args
     * @throws Exception
     * @author PTE
     */
    public int checkForApproveCADECorSTDPartConnected(Context context, String[] args) throws Exception {
        int iReturn = 0;
        StringBuffer sbErrorMsg = new StringBuffer();
        StringBuffer sbCADListFailed = new StringBuffer();
        try {
            String strCADObjectID = args[0];// Get the to Object id for Part Specification
            String strPartObjectID = args[1];// Get the fron Object id for Part Specification
            DomainObject domPartObject = DomainObject.newInstance(context, strPartObjectID);
            DomainObject domCADtObject = DomainObject.newInstance(context, strCADObjectID);

            String strPartPolicy = domPartObject.getInfo(context, DomainConstants.SELECT_POLICY);
            String strCADCurrentState = domCADtObject.getInfo(context, DomainConstants.SELECT_CURRENT);

            if (UIUtil.isNotNullAndNotEmpty(strCADCurrentState) && UIUtil.isNotNullAndNotEmpty(strPartPolicy) && strCADCurrentState.equals(TigerConstants.STATE_CAD_APPROVED)
                    && (strPartPolicy.equals(TigerConstants.POLICY_PSS_ECPART) || strPartPolicy.equals(TigerConstants.POLICY_STANDARDPART))) {

                StringList slAIInfo = new StringList();
                slAIInfo.addElement(DomainConstants.SELECT_TYPE);
                slAIInfo.addElement(DomainConstants.SELECT_REVISION);
                slAIInfo.addElement(DomainConstants.SELECT_NAME);
                Map mapRouteTemplateDetails = domCADtObject.getInfo(context, slAIInfo);
                String strCADType = (String) mapRouteTemplateDetails.get(DomainConstants.SELECT_TYPE);
                String strCADName = (String) mapRouteTemplateDetails.get(DomainConstants.SELECT_NAME);
                String strCADRevision = (String) mapRouteTemplateDetails.get(DomainConstants.SELECT_REVISION);
                String strLocale = context.getSession().getLanguage();
                sbCADListFailed.append("'");
                sbCADListFailed.append(i18nNow.getTypeI18NString(strCADType, strLocale));
                sbCADListFailed.append(" ");
                sbCADListFailed.append(strCADName);
                sbCADListFailed.append(" ");
                sbCADListFailed.append(strCADRevision);
                sbCADListFailed.append("'");
                iReturn = 1;
            }
        } catch (Exception e) {
            logger.error("Error in checkForApproveCADECorSTDPartConnected: ", e);
            throw e;
        }
        try {
            if (iReturn == 1) {
                String strErrorMsg = EnoviaResourceBundle.getProperty(context, "emxEngineeringCentralStringResource", context.getLocale(),
                        "emxEngineeringCentral.PartSpecification.ECandStdConnectedApproveCAD");
                strErrorMsg = strErrorMsg.replace("<$name>", sbCADListFailed);
                sbErrorMsg.append(strErrorMsg);
                String strStatus = context.getCustomData("PSS_THROW_EXCEPTION");
                if (UIUtil.isNotNullAndNotEmpty(strStatus) && strStatus.equals("TRUE")) {
                    throw new Exception(sbErrorMsg.toString());
                } else {
                    MqlUtil.mqlCommand(context, "notice $1", sbErrorMsg.toString());
                    iReturn = 1;
                }
            }
        } catch (Exception ex) {
            logger.error("Error in checkForApproveCADECorSTDPartConnected: ", ex);
            throw ex;
        }
        return iReturn;
    }
    // TIGTK-8543 - PTE - 2017-7-18 - END

    // TIGTK-8876: modifications Start
    /**
     * This method is used to modify description of Charted Drawing if it is blank while connecting to Part
     * @param context
     * @param args
     * @throws Exception
     * @author ashag
     */
    public void updateDescriptionOfChartedDrawing(Context context, String[] args) throws Exception {
        logger.debug("pss.cad2d3d.DECTGUtil:updateDescriptionOfChartedDrawing:START");
        String strDrawingObjectID = args[0];
        try {
            String strChartedDrawingDesc = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "emxIEFDesignCenter.Common.ChartedDrawingDescription");
            if (UIUtil.isNotNullAndNotEmpty(strDrawingObjectID)) {
                DomainObject domDrawingObject = DomainObject.newInstance(context, strDrawingObjectID);
                String strDrawingDescription = (String) domDrawingObject.getInfo(context, DomainConstants.SELECT_DESCRIPTION);
                if (UIUtil.isNullOrEmpty(strDrawingDescription)) {
                    // TIGTK-10610 - Aniket M - 2017-10-13 - START
                    ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), DomainConstants.EMPTY_STRING, DomainConstants.EMPTY_STRING);
                    domDrawingObject.setDescription(context, strChartedDrawingDesc);
                    ContextUtil.popContext(context);
                    // TIGTK-10610 - Aniket M - 2017-10-13 - END
                }
            } else {
                logger.debug("pss.cad2d3d.DECTGUtil:updateDescriptionOfChartedDrawing: Object ID is null or empty. Charted Drawing description not set.");
            }
            logger.debug("pss.cad2d3d.DECTGUtil:updateDescriptionOfChartedDrawing:END");
        } catch (Exception ex) {
            logger.error("pss.cad2d3d.DECTGUtil:updateDescriptionOfChartedDrawing:ERROR", ex);
            throw ex;
        }
    }
    // TIGTK-8876: modifications End

    // TIGTK-9215 ::: START
    /**
     * This method is used to fill CAD Mass Attribute Based On Parameter Mass
     * @param context
     * @param args
     * @throws Exception
     * @author
     */
    public void fillCADMassAttributeBasedOnParameterMass(Context context, String args[]) throws Exception {
        logger.debug("fillCADMassAttributeBasedOnParameterMass:START");
        if (DomainConstants.EMPTY_STRING.equals(args[1]))
            return;
        String strCADObjectId = args[0];
        String strCADParamMassValue = args[1];
        // TIGTK-16165 : 11-08-2018 : START
        String strValue = "0.0 kg";
        // TIGTK-16165 : 11-08-2018 : END

        if (strCADParamMassValue.equals("0g") || strCADParamMassValue.equals("0kg") || strCADParamMassValue.equals("0") || UIUtil.isNullOrEmpty(strCADParamMassValue)) {
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_MASS_SET" + strCADObjectId, "FALSE");
        }

        String isCADMassAlreadySet = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_MASS_SET" + strCADObjectId);
        if ("TRUE".equalsIgnoreCase(isCADMassAlreadySet)
                && ((strCADParamMassValue.equals("0g") || strCADParamMassValue.equals("0kg") || strCADParamMassValue.equals("0") || UIUtil.isNullOrEmpty(strCADParamMassValue)))) {
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_MASS_SET" + strCADObjectId, "FALSE");
            setAttributeValueToBlank(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CAD_Parameter_Mass, true);
            // 20-08-2018 : Added below line to make sys mass blank
            setAttributeValueToBlank(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CAD_System_Mass, true);
            return;
        }

        String strMassValue = DomainConstants.EMPTY_STRING;
        DomainObject domObj = DomainObject.newInstance(context, strCADObjectId);
        String strCADSystemMassValue = domObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CAD_System_Mass);
        // TIGTK-16165 : 11-08-2018 : START
        boolean isFromSysMassToParamMass = false;
        // TIGTK-16165 : 11-08-2018 : END
        if (UIUtil.isNullOrEmpty(strCADParamMassValue)) {
            if (UIUtil.isNotNullAndNotEmpty(strCADSystemMassValue)) {
                strMassValue = strCADSystemMassValue;
            }
        } else {
            // TIGTK-16165 : 12-08-2018 : START
            if (convertMassValueWithUnit(strCADParamMassValue, false).equalsIgnoreCase(strValue) && UIUtil.isNotNullAndNotEmpty(strCADSystemMassValue)) {
                strMassValue = strCADSystemMassValue;
                isFromSysMassToParamMass = true;
                // TIGTK-16165 : 12-08-2018 : END
            } else {
                strMassValue = strCADParamMassValue;
            }
        }

        try {
            // TIGTK-16165 : 11-08-2018 : START
            String strMassValueResult = DomainConstants.EMPTY_STRING;
            if (isFromSysMassToParamMass) {
                strMassValueResult = convertMassValueWithUnit(strMassValue, true);
            } else {
                // TIGTK-16165 : 11-08-2018 : END
                strMassValueResult = convertMassValueWithUnit(strMassValue, false);
            }
            if (!(strMassValue.equalsIgnoreCase(strValue))) {
                domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CADMass, "" + strMassValueResult);
                if (isFromSysMassToParamMass) {
                    PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_MASS_SET" + strCADObjectId, "FALSE");
                } else {
                    if (strCADParamMassValue.equals("0g") || strCADParamMassValue.equals("0kg") || strCADParamMassValue.equals("0") || UIUtil.isNullOrEmpty(strCADParamMassValue)) {
                        PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_MASS_SET" + strCADObjectId, "FALSE");
                        domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CADMass, "" + strMassValueResult);
                    } else
                        PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_MASS_SET" + strCADObjectId, "TRUE");

                }
            }
            setAttributeValueToBlank(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CAD_Parameter_Mass, true);
            // 20-08-2018 : Added below line to make sys mass blank
            setAttributeValueToBlank(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CAD_System_Mass, true);
            logger.debug("fillCADMassAttributeBasedOnParameterMass:END");
        } catch (FrameworkException e) {
            String strMessage = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "emxIEFDesignCenter.ErrorMsg.InvalidMassValue");
            strMessage = strMessage.replace("$<MASS_VALUE>", strCADParamMassValue);
            strMessage = strMessage.replace("$<CAD_OBJECT_NAME>", args[2]);
            e.addMessage(strMessage);
            logger.error("fillCADMassAttributeBasedOnParameterMass:ERROR", e);
            throw e;
        }
    }

    /**
     * This method is used to fill CAD Mass Attribute Based On System Mass
     * @param context
     * @param args
     * @throws Exception
     * @author
     */

    public void fillCADMassAttributeBasedOnSystemMass(Context context, String args[]) throws Exception {
        logger.debug("fillCADMassAttributeBasedOnSystemMass:START");
        if (DomainConstants.EMPTY_STRING.equals(args[1]))
            return;

        String strCADObjectId = args[0];
        DomainObject domObj = DomainObject.newInstance(context, strCADObjectId);
        String isCADMassAlreadySet = PropertyUtil.getGlobalRPEValue(context, "PSS_CAD_MASS_SET" + strCADObjectId);
        if ("TRUE".equalsIgnoreCase(isCADMassAlreadySet)) {
            PropertyUtil.setGlobalRPEValue(context, "PSS_CAD_MASS_SET" + strCADObjectId, "FALSE");
            setAttributeValueToBlank(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CAD_System_Mass, true);
            // 20-08-2018 : Added below line to make param mass blank
            setAttributeValueToBlank(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CAD_Parameter_Mass, true);
            return;
        }

        // if param mass is not null, then in any case, the param mass trigger will be fired and PSS_CADMass will be updated

        String strCADSystemMassValue = args[1];
        try {
            if (UIUtil.isNotNullAndNotEmpty(strCADSystemMassValue)) {
                String strMassValueResult = convertMassValueWithUnit(strCADSystemMassValue, true);
                domObj.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_CADMass, "" + strMassValueResult);
                // TIGTK-16165 : Removed set attribute value to blank
                // setAttributeValueToBlank(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CAD_System_Mass, true);
            }
            logger.debug("fillCADMassAttributeBasedOnSystemMass:END");
        } catch (FrameworkException e) {
            String strMessage = EnoviaResourceBundle.getProperty(context, SUITE_KEY, context.getLocale(), "emxIEFDesignCenter.ErrorMsg.InvalidMassValue");
            strMessage = strMessage.replace("$<MASS_VALUE>", strCADSystemMassValue);
            strMessage = strMessage.replace("$<CAD_OBJECT_NAME>", args[2]);
            logger.error("fillCADMassAttributeBasedOnSystemMass:ERROR", e);
            e.addMessage(strMessage);
            throw e;
        }
    }

    /**
     * This method is used to convert Mass To Double
     * @param context
     * @param args
     * @throws Exception
     * @author
     */
    public String convertMassValueWithUnit(String strMassValue, boolean isSystemMass) throws Exception {
        logger.debug("convertMassToDouble:START");
        String strReturn = "0.0";
        if (UIUtil.isNotNullAndNotEmpty(strMassValue)) {

            strMassValue = strMassValue.toLowerCase();
            boolean isKg = false;
            boolean isNumber = true;

            if (strMassValue.contains("kg")) {
                strMassValue = strMassValue.replace("kg", "");
                isKg = true;
                isNumber = false;
            } else if (strMassValue.contains("g")) {
                strMassValue = strMassValue.replace("g", "");
                isNumber = false;
            }

            strMassValue = strMassValue.trim();
            strMassValue = strMassValue.replace(",", ".");// to cover the French decimal character

            try {
                Double dblParamMassValue = Double.parseDouble(strMassValue);
                // TIGTK-16165:24-07-2018:START
                if (dblParamMassValue == 0.0 && !isSystemMass) {
                    strReturn = "0.0 kg";
                } else if (dblParamMassValue == 0.0 && isSystemMass) {
                    strReturn = "0.0 g";
                }
                // TIGTK-16165:24-07-2018:END
                else if (isKg || isNumber) {
                    if (isSystemMass) {
                        dblParamMassValue = dblParamMassValue * 1000;// for system mass they always want to see unit in g
                        strReturn = dblParamMassValue + " " + "g";
                    } else {
                        strReturn = dblParamMassValue + " " + "kg";
                    }
                } else {
                    strReturn = dblParamMassValue + " " + "g";
                    // when param mass, we have to always store in kg unit.
                    if (!isSystemMass) {
                        dblParamMassValue = dblParamMassValue / 1000;// for param mass they always want to see unit in kg
                        strReturn = dblParamMassValue + " " + "kg";
                    } else {
                        strReturn = dblParamMassValue + " " + "g";
                    }
                }
                logger.debug("convertMassToDouble:END");
            } catch (NumberFormatException e) {
                logger.error("convertMassToDouble:ERROR", e);
                throw e;
            }
        }
        return strReturn;
    }

    /**
     * This method is used to copy CAD Mass To EBOM Mass
     * @param context
     * @param args
     * @throws Exception
     * @author
     */
    public void copyCADMassToEBOMMass(Context context, String args[]) throws Exception {
        logger.debug("copyCADMassToEBOMMass:START");
        try {
            String strCADObjectId = args[0];
            DomainObject domObj = DomainObject.newInstance(context, strCADObjectId);
            String strGeometryTypeValue = domObj.getAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_GEOMETRYTYPE);
            if ("MG".equals(strGeometryTypeValue)) {
                StringList lstselectStmts = new StringList(3);
                lstselectStmts.addElement(DomainConstants.SELECT_ID);

                MapList mlPArtObject = domObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_PART_SPECIFICATION, DomainConstants.TYPE_PART, lstselectStmts, DomainConstants.EMPTY_STRINGLIST,
                        true, false, (short) 1, "", null, 0);

                int iConnectedPartCount = mlPArtObject.size();

                if (iConnectedPartCount > 0) {
                    String strMassUnit = UOMUtil.getInputunit(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CADMass);
                    String strCADMass = UOMUtil.getInputValue(context, strCADObjectId, TigerConstants.ATTRIBUTE_PSS_CADMass);
                    String strNewMassUnitValue = strCADMass + " " + strMassUnit;

                    for (int i = 0; i < iConnectedPartCount; i++) {
                        Map mapConnectedPart = (Map) mlPArtObject.get(i);
                        String strConnectedPartId = (String) mapConnectedPart.get(DomainConstants.SELECT_ID);
                        DomainObject domPartObject = DomainObject.newInstance(context, strConnectedPartId);
                        Map mapAttribute = domPartObject.getAttributeMap(context);
                        if (mapAttribute.containsKey(TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2)) {
                            domPartObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_EBOM_Mass2, strNewMassUnitValue);
                        }

                        domPartObject.setAttributeValue(context, TigerConstants.ATTRIBUTE_PSS_EBOM_CADMass, strNewMassUnitValue);
                    }
                }

            }
            logger.debug("copyCADMassToEBOMMass:END");
        } catch (Exception e) {
            logger.error("copyCADMassToEBOMMass:ERROR", e);
            throw e;
        }
    }
    // TIGTK-9215 ::: END

    public void setAttributeValueToBlank(Context context, String strObjectId, String strAttrName, boolean boolHistoryOff) throws Exception {
        DomainObject domObj = DomainObject.newInstance(context, strObjectId);
        boolean isHistorOff = false;
        try {
            if (boolHistoryOff) {
                MqlUtil.mqlCommand(context, "history off;", true, false);
                isHistorOff = true;
            }

            domObj.setAttributeValue(context, strAttrName, DomainConstants.EMPTY_STRING);
        } catch (Exception Ex) {
            logger.error("Error in setAttributeValueToBlank: ", Ex);
            throw Ex;
        } finally {
            if (isHistorOff)
                MqlUtil.mqlCommand(context, "history on;", true, false);
        }
    }

    // TIGTK - 14549 : start
    /**
     * This method is used to copy attribute value of Spatial Location To Component Location
     * @param context
     * @param args
     * @throws Exception
     * @author
     */
    public void copySpatialLocationToComponentLocation(Context context, String args[]) throws Exception {
        logger.debug("copySpatialLocationToComponentLocation:START");
        try {
            String strRelId = args[0];
            String strAttrSpatialLocationValue = args[1];

            // DomainRelationship domCadSubComponentRel ;

            StringList relSelectList = new StringList(3);
            relSelectList.addElement("from.id");
            relSelectList.addElement("to.id");
            relSelectList.addElement(DomainConstants.SELECT_NAME);
            // domCadSubComponentRel = new DomainRelationship( strRelId );
            MapList mlUsedResult = DomainRelationship.getInfo(context, new String[] { strRelId }, relSelectList);
            Map mObjIdMap = (Map) mlUsedResult.get(0);
            String strFromObjId = (String) mObjIdMap.get("from.id");
            String strToObjId = (String) mObjIdMap.get("to.id");
            DomainObject dFromCADObject = DomainObject.newInstance(context, strFromObjId);
            DomainObject dToCADObject = DomainObject.newInstance(context, strToObjId);
            DomainObject dFromVersionObject;
            DomainObject dToVersionObject;
            String strVersionOfId = dFromCADObject.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");
            if (UIUtil.isNotNullAndNotEmpty(strVersionOfId)) {
                dFromVersionObject = DomainObject.newInstance(context, strVersionOfId);
            } else {
                dFromVersionObject = dFromCADObject;
            }

            strVersionOfId = dToCADObject.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");
            if (UIUtil.isNotNullAndNotEmpty(strVersionOfId)) {
                dToVersionObject = DomainObject.newInstance(context, strVersionOfId);
            } else {
                dToVersionObject = dToCADObject;
            }
            StringList strToPartSpecList = dToVersionObject.getInfoList(context, "to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.id");
            StringList strFromPartSpecList = dFromVersionObject.getInfoList(context, "to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.id");
            StringList lObjSelectStmts = new StringList(1);
            lObjSelectStmts.addElement(DomainConstants.SELECT_ID);
            StringList lRelSelectStmts = new StringList(1);
            lRelSelectStmts.addElement(DomainRelationship.SELECT_ID);
            for (int i = 0; i < strFromPartSpecList.size(); i++) {
                DomainObject dObj = DomainObject.newInstance(context, (String) strFromPartSpecList.get(i));
                MapList mlPartObjectList = dObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lObjSelectStmts, lRelSelectStmts, false, true, (short) 1, "",
                        null, 0);
                for (int j = 0; j < mlPartObjectList.size(); j++) {
                    Map mObjMap = (Map) mlPartObjectList.get(j);
                    String strEBOMRelId = (String) mObjMap.get(DomainRelationship.SELECT_ID);
                    String strEBOMObjId = (String) mObjMap.get(DomainConstants.SELECT_ID);
                    if (strToPartSpecList.contains(strEBOMObjId)) {
                        DomainRelationship.setAttributeValue(context, strEBOMRelId, TigerConstants.ATTRIBUTE_COMPONENTLOCATION, strAttrSpatialLocationValue);
                    }
                }

            }

            logger.debug("copySpatialLocationToComponentLocation:END");
        } catch (Exception e) {
            logger.error("copySpatialLocationToComponentLocation:ERROR", e);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * This method is used to copy attribute value of Spatial Location To Component Location
     * @param context
     * @param args
     * @throws Exception
     * @author
     */
    public void copyRefDesignatorToEBOMRefDesignator(Context context, String args[]) throws Exception {
        logger.debug("copyRefDesignatorToEBOMRefDesignator:START");
        try {
            String strRelId = args[0];
            String strAttrRefDesignatorValue = args[1];
            // DomainRelationship domCadSubComponentRel ;
            String strRelCADSubComponent = MCADMxUtil.getActualNameForAEFData(context, "relationship_CADSubComponent");

            StringList relSelectList = new StringList(3);
            relSelectList.addElement("from.id");
            relSelectList.addElement("to.id");
            relSelectList.addElement(DomainConstants.SELECT_NAME);

            // domCadSubComponentRel = new DomainRelationship( strRelId );
            MapList mlUsedResult = DomainRelationship.getInfo(context, new String[] { strRelId }, relSelectList);

            Map mObjIdMap = (Map) mlUsedResult.get(0);
            String strRelName = (String) mObjIdMap.get(DomainConstants.SELECT_NAME);
            if (strRelName.equalsIgnoreCase(strRelCADSubComponent)) {
                String strFromObjId = (String) mObjIdMap.get("from.id");
                String strToObjId = (String) mObjIdMap.get("to.id");

                DomainObject dFromCADObject = DomainObject.newInstance(context, strFromObjId);
                DomainObject dToCADObject = DomainObject.newInstance(context, strToObjId);
                DomainObject dFromVersionObject;
                DomainObject dToVersionObject;
                String strVersionOfId = dFromCADObject.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");

                if (UIUtil.isNotNullAndNotEmpty(strVersionOfId)) {
                    dFromVersionObject = DomainObject.newInstance(context, strVersionOfId);
                } else {
                    dFromVersionObject = dFromCADObject;
                }

                strVersionOfId = dToCADObject.getInfo(context, "from[" + TigerConstants.RELATIONSHIP_VERSIONOF + "].to.id");

                if (UIUtil.isNotNullAndNotEmpty(strVersionOfId)) {
                    dToVersionObject = DomainObject.newInstance(context, strVersionOfId);
                } else {
                    dToVersionObject = dToCADObject;
                }
                StringList strToPartSpecList = dToVersionObject.getInfoList(context, "to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.id");
                StringList strFromPartSpecList = dFromVersionObject.getInfoList(context, "to[" + DomainConstants.RELATIONSHIP_PART_SPECIFICATION + "].from.id");

                StringList lObjSelectStmts = new StringList(1);
                lObjSelectStmts.addElement(DomainConstants.SELECT_ID);
                StringList lRelSelectStmts = new StringList(1);
                lRelSelectStmts.addElement(DomainRelationship.SELECT_ID);
                for (int i = 0; i < strFromPartSpecList.size(); i++) {
                    DomainObject dObj = DomainObject.newInstance(context, (String) strFromPartSpecList.get(i));
                    MapList mlPartObjectList = dObj.getRelatedObjects(context, DomainConstants.RELATIONSHIP_EBOM, DomainConstants.TYPE_PART, lObjSelectStmts, lRelSelectStmts, false, true, (short) 1,
                            "", null, 0);
                    for (int j = 0; j < mlPartObjectList.size(); j++) {
                        Map mObjMap = (Map) mlPartObjectList.get(j);
                        String strEBOMRelId = (String) mObjMap.get(DomainRelationship.SELECT_ID);
                        String strEBOMObjId = (String) mObjMap.get(DomainConstants.SELECT_ID);
                        if (strToPartSpecList.contains(strEBOMObjId)) {
                            DomainRelationship.setAttributeValue(context, strEBOMRelId, TigerConstants.ATTRIBUTE_REFERENCEDESIGNATOR, strAttrRefDesignatorValue);
                        }
                    }

                }
            }

            logger.debug("copyRefDesignatorToEBOMRefDesignator:END");
        } catch (RuntimeException e) {
            logger.error("copyRefDesignatorToEBOMRefDesignator:ERROR", e);
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            logger.error("copyRefDesignatorToEBOMRefDesignator:ERROR", e);
            e.printStackTrace();
            throw e;
        }
    }

    // TIGTK-17437 : VISHAL :START
    public int connectPrevChartedDrawingWithRevisedPart(Context context, String args[]) throws Exception {
        int iReturn = 0;

        try {
            String strCAOID = args[0];
            String strNewPartOID = args[1];
            DomainObject doCA = DomainObject.newInstance(context, strCAOID);
            DomainObject doNewPart = DomainObject.newInstance(context, strNewPartOID);
            if (doNewPart.isKindOf(context, TigerConstants.TYPE_PART)) {
                StringList slSelects = new StringList(2);
                slSelects.add("previous.id");
                slSelects.add("from[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].to.id");
                BusinessObject boNewPart = new BusinessObject(doNewPart);
                BusinessObjectWithSelect bows = boNewPart.select(context, slSelects);
                String strPrevPartOID = bows.getSelectData("previous.id");
                if (UIUtil.isNotNullAndNotEmpty(strPrevPartOID)) {
                    StringList slNewConnectedCD = bows.getSelectDataList("from[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].to.id");
                    StringList slNewRevConnectedCO = doCA.getInfoList(context, "to[" + TigerConstants.RELATIONSHIP_CHANGEACTION + "].from.id");
                    DomainObject doPrevPart = DomainObject.newInstance(context, strPrevPartOID);
                    StringList slPrevConnectedCD = doPrevPart.getInfoList(context, "from[" + TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING + "].to.id");
                    RelationshipType rtChartedDrawing = new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
                    for (Object chartedDrawing : slPrevConnectedCD) {
                        DomainObject doChartedDrawing = DomainObject.newInstance(context, (String) chartedDrawing);
                        StringList slCO = doChartedDrawing.getInfoList(context, "to[" + ChangeConstants.RELATIONSHIP_IMPLEMENTED_ITEM + TigerConstants.SEPERATOR_COMMA
                                + ChangeConstants.RELATIONSHIP_CHANGE_AFFECTED_ITEM + "].from.to[" + ChangeConstants.RELATIONSHIP_CHANGE_ACTION + "].from.id");
                        if (slNewConnectedCD != null) {
                            if (slCO != null && !slNewRevConnectedCO.contains(slCO) && !slNewConnectedCD.contains((String) chartedDrawing)) {
                                DomainRelationship.connect(context, doNewPart, rtChartedDrawing, doChartedDrawing);
                            }
                        } else {
                            DomainRelationship.connect(context, doNewPart, rtChartedDrawing, doChartedDrawing);

                        }

                    }
                }
            }
        } catch (RuntimeException e) {
            logger.error(e.getLocalizedMessage(), e);
            iReturn = 1;
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            iReturn = 1;
        }
        return iReturn;
    }

    public int disconnectLatestChartedDrawingFromRevisedPart(Context context, String args[]) throws Exception {
        int iReturn = 0;

        try {
            String strPartID = args[0];
            StringList slObjectSelect = new StringList();
            slObjectSelect.add(DomainConstants.SELECT_ID);
            slObjectSelect.add(DomainRelationship.SELECT_ID);
            DomainObject domObjectPart = DomainObject.newInstance(context, strPartID);
            Pattern typePattern = new Pattern(DomainConstants.TYPE_CAD_DRAWING);
            typePattern.addPattern(DomainConstants.TYPE_CAD_MODEL);
            String strObjWhereExpression = "current==last";
            MapList mlChartedDrawings = domObjectPart.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING, typePattern.getPattern(), slObjectSelect, null, false, true,
                    (short) 1, strObjWhereExpression, null, 0);
            int iConnectedPartCount = mlChartedDrawings.size();
            for (int i = 0; i < iConnectedPartCount; i++) {
                Map mpObj = (Map) mlChartedDrawings.get(i);
                String strRelID = (String) mpObj.get(DomainConstants.SELECT_RELATIONSHIP_ID);
                DomainRelationship.disconnect(context, strRelID);
            }

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            iReturn = 1;
        }
        return iReturn;
    }

    public int connectLatestChartedDrawingWithRevisedPart(Context context, String args[]) throws Exception {
        int iReturn = 0;
        try {
            String strNewCDOID = args[0];
            String strprevCD = args[1];
            DomainObject doNewCD = DomainObject.newInstance(context, strNewCDOID);
            DomainObject doPrervCD = DomainObject.newInstance(context, strprevCD);
            String objectWhere = "current== '" + ChangeConstants.STATE_PART_PRELIMINARY + "'";
            StringList BusSelectList = new StringList(DomainConstants.SELECT_ID);
            MapList mapRelPart = doPrervCD.getRelatedObjects(context, TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING, TigerConstants.TYPE_PART, BusSelectList, null, true, false, (short) 1,
                    objectWhere, null, (int) 1);
            RelationshipType rtChartedDrawing = new RelationshipType(TigerConstants.RELATIONSHIP_PSS_CHARTED_DRAWING);
            for (int j = 0; j < mapRelPart.size(); j++) {
                Hashtable partTable = (Hashtable) mapRelPart.get(j);
                String strPartId = (String) partTable.get(DomainConstants.SELECT_ID);
                DomainObject doNewPart = DomainObject.newInstance(context, strPartId);
                DomainRelationship.connect(context, doNewPart, rtChartedDrawing, doNewCD);
            }

        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            iReturn = 1;
        }
        return iReturn;
    }

    // TIGTK-17437 : VISHAL :END
}