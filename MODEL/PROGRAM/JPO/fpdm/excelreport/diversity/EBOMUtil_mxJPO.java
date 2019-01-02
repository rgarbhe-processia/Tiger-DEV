package fpdm.excelreport.diversity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.util.MatrixException;
import matrix.util.SelectList;

public class EBOMUtil_mxJPO {
    /**
     * Return a ResolvedEBOM object containing the consolidated EBOM
     * @param context
     * @param objectId
     * @return the object containing the resolved EBOM
     * @throws MatrixException
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOMWithQuantity(Context context, String objectId) throws MatrixException {
        fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc = new fpdm.excelreport.diversity.ProductConfiguration_mxJPO("", new ArrayList<String>(), "");
        BusinessObject boRoot = new BusinessObject(objectId);
        return getEBOMWithQuantity(context, boRoot, pc);
    }

    /**
     * Return a ResolvedEBOM object containing the consolidated EBOM who contain elements selected from object in a SelectList
     * @param context
     * @param objectId
     *            the object containing the resolved EBOM
     * @param slInfos
     *            the SelectList element containing attributes to select on elements
     * @return
     * @throws MatrixException
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOMWithQuantity(Context context, String objectId, SelectList slInfos) throws MatrixException {
        fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc = new fpdm.excelreport.diversity.ProductConfiguration_mxJPO("", new ArrayList<String>(), "");
        BusinessObject boRoot = new BusinessObject(objectId);
        return getEBOMWithQuantity(context, boRoot, pc, slInfos);
    }

    /**
     * Return a ResolvedEBOM object containing the consolidated EBOM corresponding to a ProductConfiguration
     * @param context
     * @param objectId
     *            the object containing the resolved EBOM
     * @param pc
     *            The ProductConfiguration object
     * @return
     * @throws MatrixException
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOMWithQuantity(Context context, String objectId, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc) throws MatrixException {
        BusinessObject boRoot = new BusinessObject(objectId);
        return getEBOMWithQuantity(context, boRoot, pc);
    }

    /**
     * Return a ResolvedEBOM object containing the consolidated EBOM corresponding to a ProductConfiguration, who contain elements selected from object in a SelectList
     * @param context
     * @param objectId
     *            the object containing the resolved EBOM
     * @param pc
     *            The ProductConfiguration object
     * @param slInfos
     *            the SelectList element containing attributes to select on elements
     * @return
     * @throws MatrixException
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOMWithQuantity(Context context, String objectId, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc, SelectList slInfos)
            throws MatrixException {
        BusinessObject boRoot = new BusinessObject(objectId);
        return getEBOMWithQuantity(context, boRoot, pc, slInfos);
    }

    /**
     * Return a ResolvedEBOM object containing the consolidated EBOM
     * @param context
     * @param boRoot
     *            The BusinessObject to expand
     * @return
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOMWithQuantity(Context context, BusinessObject boRoot) {
        fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc = new fpdm.excelreport.diversity.ProductConfiguration_mxJPO("", new ArrayList<String>(), "");
        return getEBOMWithQuantity(context, boRoot, pc);
    }

    /**
     * Return a ResolvedEBOM object containing the consolidated EBOM who contain elements selected from object in a SelectList
     * @param context
     * @param boRoot
     *            The BusinessObject to expand
     * @param slInfos
     *            The SelectList element containing attributes to select on elements
     * @return
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOMWithQuantity(Context context, BusinessObject boRoot, SelectList slInfos) {
        fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc = new fpdm.excelreport.diversity.ProductConfiguration_mxJPO("", new ArrayList<String>(), "");
        return getEBOMWithQuantity(context, boRoot, pc, slInfos);
    }

    /**
     * Return a ResolvedEBOM object containing the consolidated EBOM corresponding to a ProductConfiguration
     * @param context
     * @param boRoot
     *            The BusinessObject to expand
     * @param pc
     *            The ProductConfiguration object
     * @return
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOMWithQuantity(Context context, BusinessObject boRoot, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc) {
        fpdm.excelreport.diversity.ResolvedEBOM_mxJPO rebom = fpdm.excelreport.diversity.EBOMUtil_mxJPO.getEBOM(context, boRoot, pc);
        System.out.println("REBOM : " + rebom);
        fpdm.excelreport.diversity.ResolvedEBOM_mxJPO resultBOM = fpdm.excelreport.diversity.EBOMUtil_mxJPO.consolidateEBOM(rebom);
        return resultBOM;
    }

    /**
     * Return a ResolvedEBOM object containing the consolidated EBOM corresponding to a ProductConfiguration, who contain elements selected from object in a SelectList
     * @param context
     * @param boRoot
     *            The BusinessObject to expand
     * @param pc
     *            The ProductConfiguration object
     * @param slInfos
     *            The SelectList element containing attributes to select on elements
     * @return
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOMWithQuantity(Context context, BusinessObject boRoot, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc,
            SelectList slInfos) {
        fpdm.excelreport.diversity.ResolvedEBOM_mxJPO rebom = fpdm.excelreport.diversity.EBOMUtil_mxJPO.getEBOM(context, boRoot, pc, slInfos);
        fpdm.excelreport.diversity.ResolvedEBOM_mxJPO resultBOM = fpdm.excelreport.diversity.EBOMUtil_mxJPO.consolidateEBOM(rebom);
        return resultBOM;
    }

    /**
     * Consolidate a bom from a ResolvedEBOM object
     * @param rebom
     * @return
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO consolidateEBOM(fpdm.excelreport.diversity.ResolvedEBOM_mxJPO rebom) {
        HashMap<String, fpdm.excelreport.diversity.ResolvedPart_mxJPO> hmParts = rebom.hmBOM;

        for (Entry<String, fpdm.excelreport.diversity.ResolvedPart_mxJPO> entry : hmParts.entrySet()) {
            try {
                fpdm.excelreport.diversity.ResolvedPart_mxJPO aPart = entry.getValue();
                System.out.println(aPart.sId + " -> " + aPart.uom);

                ArrayList<fpdm.excelreport.diversity.Children_mxJPO> alChildren = aPart.children;

                HashMap<String, Integer> hmIDChildrenAlreadyKnown = new HashMap<String, Integer>();
                ArrayList<Integer> alToDelete = new ArrayList<Integer>();

                for (int x = 0; x < alChildren.size(); x++) {
                    fpdm.excelreport.diversity.Children_mxJPO thisChildren = alChildren.get(x);

                    if (!hmIDChildrenAlreadyKnown.containsKey(thisChildren.id)) {
                        hmIDChildrenAlreadyKnown.put(thisChildren.id, x);
                    } else {
                        if (!fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRangePart.contains(thisChildren.uom)) {
                            int intFNExisting = Integer.parseInt(alChildren.get(hmIDChildrenAlreadyKnown.get(thisChildren.id)).fn);
                            int intFNNew = Integer.parseInt(thisChildren.fn);

                            float intQuantityExisting = Float.parseFloat(alChildren.get(hmIDChildrenAlreadyKnown.get(thisChildren.id)).quantity);
                            float intQuantityNew = Float.parseFloat(thisChildren.quantity);
                            float newQuantity = intQuantityExisting + intQuantityNew;

                            if (intFNExisting < intFNNew) {
                                alChildren.get(hmIDChildrenAlreadyKnown.get(thisChildren.id)).quantity = "" + newQuantity;
                                alChildren.get(x).quantity = "0";
                                alToDelete.add(x);
                            } else {
                                alChildren.get(hmIDChildrenAlreadyKnown.get(thisChildren.id)).quantity = "0";
                                alChildren.get(x).quantity = "" + newQuantity;
                                alToDelete.add(hmIDChildrenAlreadyKnown.get(thisChildren.id));
                            }
                        }
                    }
                }

                for (int x = alChildren.size() - 1; x > 0; x--) {
                    float floatQuantity = Float.parseFloat(alChildren.get(x).quantity);
                    if (0 == floatQuantity) {
                        alChildren.remove(x);
                    }
                }

                aPart.children = alChildren;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        rebom.hmBOM = hmParts;

        return rebom;
    }

    /**
     * Return a ResolvedEBOM object containing the EBOM
     * @param context
     * @param objectId
     * @return the object containing the resolved EBOM
     * @throws MatrixException
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOM(Context context, String objectId) throws MatrixException {
        fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc = new fpdm.excelreport.diversity.ProductConfiguration_mxJPO("", new ArrayList<String>(), "");
        BusinessObject boRoot = new BusinessObject(objectId);
        return getEBOM(context, boRoot, pc);
    }

    /**
     * Return a ResolvedEBOM object containing the EBOM who contain elements selected from object in a SelectList
     * @param context
     * @param objectId
     *            the object containing the resolved EBOM
     * @param slInfos
     *            the SelectList element containing attributes to select on elements
     * @return
     * @throws MatrixException
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOM(Context context, String objectId, SelectList slInfos) throws MatrixException {
        fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc = new fpdm.excelreport.diversity.ProductConfiguration_mxJPO("", new ArrayList<String>(), "");
        BusinessObject boRoot = new BusinessObject(objectId);
        return getEBOM(context, boRoot, pc, slInfos);
    }

    /**
     * Return a ResolvedEBOM object containing the EBOM corresponding to a ProductConfiguration
     * @param context
     * @param objectId
     *            the object containing the resolved EBOM
     * @param pc
     *            The ProductConfiguration object
     * @return
     * @throws MatrixException
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOM(Context context, String objectId, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc) throws MatrixException {
        BusinessObject boRoot = new BusinessObject(objectId);
        return getEBOM(context, boRoot, pc);
    }

    /**
     * Return a ResolvedEBOM object containing the EBOM corresponding to a ProductConfiguration, who contain elements selected from object in a SelectList
     * @param context
     * @param objectId
     *            the object containing the resolved EBOM
     * @param pc
     *            The ProductConfiguration object
     * @param slInfos
     *            the SelectList element containing attributes to select on elements
     * @return
     * @throws MatrixException
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOM(Context context, String objectId, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc, SelectList slInfos)
            throws MatrixException {
        BusinessObject boRoot = new BusinessObject(objectId);
        return getEBOM(context, boRoot, pc, slInfos);
    }

    /**
     * Return a ResolvedEBOM object containing the EBOM
     * @param context
     * @param boRoot
     *            The BusinessObject to expand
     * @return
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOM(Context context, BusinessObject boRoot) {
        fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc = new fpdm.excelreport.diversity.ProductConfiguration_mxJPO("", new ArrayList<String>(), "");
        return getEBOM(context, boRoot, pc);
    }

    /**
     * Return a ResolvedEBOM object containing the EBOM who contain elements selected from object in a SelectList
     * @param context
     * @param boRoot
     *            The BusinessObject to expand
     * @param slInfos
     *            The SelectList element containing attributes to select on elements
     * @return
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOM(Context context, BusinessObject boRoot, SelectList slInfos) {
        fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc = new fpdm.excelreport.diversity.ProductConfiguration_mxJPO("", new ArrayList<String>(), "");
        return getEBOM(context, boRoot, pc, slInfos);
    }

    /**
     * Return a ResolvedEBOM object containing the EBOM corresponding to a ProductConfiguration
     * @param context
     * @param boRoot
     *            The BusinessObject to expand
     * @param pc
     *            The ProductConfiguration object
     * @return
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOM(Context context, BusinessObject boRoot, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc) {
        // ResolvedEBOM rebom = new ResolvedEBOM(boRoot.getObjectId(), pc);
        return getEBOM(context, boRoot, pc, new SelectList());
    }

    /**
     * Return a ResolvedEBOM object containing the consolidated EBOM corresponding to a ProductConfiguration, who contain elements selected from object in a SelectList
     * @param context
     * @param boRoot
     *            The BusinessObject to expand
     * @param pc
     *            The ProductConfiguration object
     * @param slInfos
     *            The SelectList element containing attributes to select on elements
     * @return
     */
    public static fpdm.excelreport.diversity.ResolvedEBOM_mxJPO getEBOM(Context context, BusinessObject boRoot, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc, SelectList slInfos) {
        fpdm.excelreport.diversity.ResolvedEBOM_mxJPO rebom = new fpdm.excelreport.diversity.ResolvedEBOM_mxJPO(boRoot.getObjectId(), pc, slInfos);
        rebom.builtResolvedEBOM(context);
        return rebom;
    }
}
