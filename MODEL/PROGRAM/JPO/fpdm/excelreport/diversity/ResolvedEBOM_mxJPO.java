package fpdm.excelreport.diversity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import matrix.db.Context;
import matrix.util.SelectList;

public class ResolvedEBOM_mxJPO {
    private String sHeadOfBOMID;

    private fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc;

    private SelectList slInfos = null;

    public HashMap<String, fpdm.excelreport.diversity.ResolvedPart_mxJPO> hmBOM = new HashMap<String, fpdm.excelreport.diversity.ResolvedPart_mxJPO>();

    private LinkedList<String> toResolve = new LinkedList<String>();

    public ResolvedEBOM_mxJPO(String sHeadOfBOMID, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc) {
        this.sHeadOfBOMID = sHeadOfBOMID;
        this.pc = pc;

        this.toResolve.add(sHeadOfBOMID);
    }

    public ResolvedEBOM_mxJPO(String sHeadOfBOMID, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc, SelectList slInfos) {
        this.slInfos = slInfos;

        this.sHeadOfBOMID = sHeadOfBOMID;
        this.pc = pc;

        this.toResolve.add(sHeadOfBOMID);
    }

    public HashMap<String, fpdm.excelreport.diversity.ResolvedPart_mxJPO> builtResolvedEBOM(Context context) {
        while (toResolve.size() > 0) {
            String idToResolve = toResolve.getFirst();

            fpdm.excelreport.diversity.ResolvedPart_mxJPO rp = new fpdm.excelreport.diversity.ResolvedPart_mxJPO(idToResolve, pc, slInfos);
            rp.builPartInfo(context);
            ArrayList<String> children = rp.getChildren();
            Iterator<String> itChildren = children.iterator();
            while (itChildren.hasNext()) {
                String sIdChild = itChildren.next();
                if (!hmBOM.containsKey(sIdChild)) {
                    toResolve.add(sIdChild);
                }
            }

            hmBOM.put(idToResolve, rp);
            toResolve.removeFirst();
        }

        return hmBOM;
    }

    public fpdm.excelreport.diversity.ProductConfiguration_mxJPO getProductConfiguration() {
        return this.pc;
    }

    public Set<String> getAllId() {
        return hmBOM.keySet();
    }

    public String getsHeadOfBOMID() {
        return sHeadOfBOMID;
    }

    public void setsHeadOfBOMID(String sHeadOfBOMID) {
        this.sHeadOfBOMID = sHeadOfBOMID;
    }

    public boolean contains(String sPartId) {
        return hmBOM.keySet().contains(sPartId);
    }

    public String toString() {
        return this.hmBOM.toString();
    }
}
