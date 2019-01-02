
/*
 ** emxDomainAccess
 **
 ** Copyright (c) 1992-2015 Dassault Systemes. All Rights Reserved. This program contains proprietary and trade secret information of MatrixOne, Inc. Copyright notice is precautionary only and does not
 * evidence any actual or intended publication of such program.
 **
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.componentcentral.CPCConstants;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

/**

 */
public class emxDomainAccess_mxJPO extends emxDomainAccessBase_mxJPO {

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation
     * @since V6R2011x
     * @grade 0
     */
    public emxDomainAccess_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    /**
     * To update the ownership of the Single/Mass CAD or Part TIGTK-17752 :: ALM-6018
     * @see emxDomainAccessBase_mxJPO#updateOwnership(Context, String[])
     * @param context
     * @param args
     * @throws Exception
     */
    @com.matrixone.apps.framework.ui.PostProcessCallable
    public void updateOwnership(Context context, String[] args) throws Exception {
        HashMap<?, ?> fieldMap = (HashMap<?, ?>) JPO.unpackArgs(args);
        HashMap<?, ?> requestMap = (HashMap<?, ?>) fieldMap.get("requestMap");
        String objectId = (String) requestMap.get("objectId");
        String owner = (String) requestMap.get("Name");
        String Organization = (String) requestMap.get("Organization");
        String project = (String) requestMap.get("Project");
        String strRowIds = (String) requestMap.get("rowIds");
        StringList slRowIdsList = new StringList();
        if (UIUtil.isNotNullAndNotEmpty(strRowIds))
            slRowIdsList = FrameworkUtil.split(strRowIds, TigerConstants.SEPERATOR_COMMA);
        else
            slRowIdsList.add(objectId);
        String[] strArrayRowIds = (String[]) slRowIdsList.toArray(new String[slRowIdsList.size()]);
        StringList slSelect = new StringList(DomainConstants.SELECT_ID);
        slSelect.add(DomainConstants.SELECT_CURRENT);
        slSelect.add(DomainConstants.SELECT_TYPE);
        slSelect.add(DomainConstants.SELECT_POLICY);
        slSelect.add(DomainConstants.SELECT_OWNER);
        MapList mlRowIDInfo = DomainObject.getInfo(context, strArrayRowIds, slSelect);
        try {
            Iterator<HashMap> itrRowId = mlRowIDInfo.iterator();
            while (itrRowId.hasNext()) {
                Map mapPart = itrRowId.next();
                String strPartId = (String) mapPart.get(DomainConstants.SELECT_ID);
                String strPartState = (String) mapPart.get(DomainConstants.SELECT_CURRENT);
                String strPartType = (String) mapPart.get(DomainConstants.SELECT_TYPE);
                String strPartPolicy = (String) mapPart.get(DomainConstants.SELECT_POLICY);

                DomainObject domainObject = DomainObject.newInstance(context, strPartId);
                if ((DomainConstants.TYPE_PART.equalsIgnoreCase(strPartType))
                        && ((TigerConstants.POLICY_PSS_DEVELOPMENTPART.equalsIgnoreCase(strPartPolicy) || CPCConstants.POLICY_CONFIGURED_PART.equalsIgnoreCase(strPartPolicy)
                                || TigerConstants.STATE_PART_OBSOLETE.equalsIgnoreCase(strPartState) || TigerConstants.STATE_PSS_CANCELPART_CANCELLED.equalsIgnoreCase(strPartState)))) {
                    itrRowId.remove();
                    continue;
                }
                if (domainObject.isKindOf(context, CommonDocument.TYPE_DOCUMENTS) && !TigerConstants.STATE_RELEASED_CAD_OBJECT.equalsIgnoreCase(strPartState)) {
                    itrRowId.remove();
                    continue;
                }

                if (domainObject.isKindOf(context, CommonDocument.TYPE_DOCUMENTS)) {
                    StringList slObjectsToTransfer = getOwnershipTransferAllownedCADIterationsAndRelatedItems(context, strPartId);

                    if (!slObjectsToTransfer.isEmpty()) {
                        DomainObject doCAD = DomainObject.newInstance(context);
                        PropertyUtil.setRPEValue(context, "PSS_Update_Project_owner", "TRUE", true);
                        for (Object cadOID : slObjectsToTransfer) {
                            String strOID = (String) cadOID;
                            if (UIUtil.isNotNullAndNotEmpty(strOID)) {
                                doCAD.setId(strOID);
                                doCAD.TransferOwnership(context, owner, project, Organization);
                            }
                        }
                    }
                } else if (DomainConstants.TYPE_PART.equalsIgnoreCase(strPartType)) {
                    if (TigerConstants.STATE_PART_RELEASE.equalsIgnoreCase(strPartState)) {
                        domainObject.open(context);
                        domainObject.setProjectOwner(context, project);
                        domainObject.setOrganizationOwner(context, Organization);
                        domainObject.update(context);
                        domainObject.close(context);

                    } else {
                        domainObject.TransferOwnership(context, owner, project, Organization);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getLocalizedMessage(), e);
        } finally {
            PropertyUtil.setRPEValue(context, "PSS_Update_Project_owner", "FASLE", true);
        }
    }

    /**
     * To update the ownership of the CAD Object and related Iterations and DerivedOutputs TIGTK-17752 :: ALM-6018
     * @param context
     * @param objectId
     *            CAD object id
     * @return StringList list of version/iteration ids to TransferOwnership
     * @throws Exception
     */
    private StringList getOwnershipTransferAllownedCADIterationsAndRelatedItems(Context context, String objectId) throws Exception {
        try {
            // CAD
            MCADMxUtil mxUtil = new MCADMxUtil(context, new MCADServerResourceBundle("en-us"), new IEFGlobalCache());
            // all related CAD iterations
            String[] saVersionObjects = mxUtil.getAllVersionObjects(context, new String[] { objectId }, false);
            StringList slObjectsToTransfer = new StringList(saVersionObjects.length * 3 + 2); // iteration + related DO&Thumbnails(*3)+Major+PDFArchive(for TB)
            slObjectsToTransfer.add(objectId); // major CAD object
            StringList slSelectables = new StringList(3);
            slSelectables.add(DomainConstants.SELECT_ID);
            slSelectables.add(TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
            slSelectables.add("from[" + TigerConstants.RELATIONSHIP_VIEWABLE + "].to.id");
            slSelectables.add("from[" + TigerConstants.RELATIONSHIP_IMAGEHOLDER + "].to.id");

            // All related Derived Output & Viewable Thumbnails of iterations
            BusinessObjectWithSelectList bwslDerivedOutput = BusinessObject.getSelectBusinessObjectData(context, saVersionObjects, slSelectables);
            int iDOSize = bwslDerivedOutput.size();
            for (int i = 0; i < iDOSize; ++i) {
                BusinessObjectWithSelect localBusinessObjectWithSelect = (BusinessObjectWithSelect) bwslDerivedOutput.get(i);
                String strVersionOID = localBusinessObjectWithSelect.getSelectData(DomainConstants.SELECT_ID);
                String strDOID = localBusinessObjectWithSelect.getSelectData(TigerConstants.SELECT_FROM_DERIVED_OUTPUT_TO_ID);
                StringList slViewableOID = localBusinessObjectWithSelect.getSelectDataList("from[" + TigerConstants.RELATIONSHIP_VIEWABLE + "].to.id");
                slObjectsToTransfer.add(strVersionOID);
                if (UIUtil.isNotNullAndNotEmpty(strDOID))
                    slObjectsToTransfer.add(strDOID);
                if (slViewableOID != null && !slViewableOID.isEmpty())
                    slObjectsToTransfer.addAll(slViewableOID);
            }
            // PDF Archive
            String strPDFArchive = new DomainObject(objectId).getInfo(context, TigerConstants.SELECT_FROM_PSS_PDF_ARCHIVE_TO_ID);
            if (UIUtil.isNotNullAndNotEmpty(strPDFArchive))
                slObjectsToTransfer.add(strPDFArchive);
            return slObjectsToTransfer;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getLocalizedMessage(), e);
        }
    }

}
