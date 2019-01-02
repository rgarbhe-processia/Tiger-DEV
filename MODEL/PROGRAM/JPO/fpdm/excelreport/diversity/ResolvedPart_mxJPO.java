package fpdm.excelreport.diversity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.ExpansionIterator;
import matrix.util.MatrixException;
import matrix.util.SelectList;
import matrix.util.StringList;

public class ResolvedPart_mxJPO {

    static String SELECT_FROM_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION_TO_ID = "from[PSS_VariantAssemblyProductConfiguration].to.id";

    static String ATTRIBUTE_UNIT_OF_MEASURE = "Unit of Measure";

    static String SELECT_EBOM_QUANTITY = "attribute[Quantity].value";

    static String SELECT_UNIT_OF_MEASURE = "attribute[" + ATTRIBUTE_UNIT_OF_MEASURE + "].value";

    static String SELECT_EBOM_TO_UNIT_OF_MEASURE = "to." + SELECT_UNIT_OF_MEASURE;

    static String SELECT_EBOM_FIND_NUMBER = "attribute[Find Number].value";

    static SelectList selectStmts = null;

    static SelectList slRelationSelects = null;

    static SelectList selectStmtsPC = null;

    static SelectList slRelationSelectsPC = null;

    public static StringList slRangePart = null;

    String sId;

    String sName;

    String sRevision;

    String uom; // Unit of measure

    String sVariantId = null;

    String sVariantName = null;

    Hashtable<String, String> htMoreInfos = new Hashtable<String, String>();

    HashMap<String, ?> hmMoreInfos = new HashMap<String, Object>();

    ArrayList<fpdm.excelreport.diversity.Children_mxJPO> children = new ArrayList<fpdm.excelreport.diversity.Children_mxJPO>();

    fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc;

    public ResolvedPart_mxJPO(String sId, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc) {
        this.init();

        this.sId = sId;
        this.pc = pc;
    }

    public ResolvedPart_mxJPO(String sId, fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc, SelectList slInfos) {
        this.init(slInfos);
        this.sId = sId;
        this.pc = pc;
    }

    private synchronized void init() {
        if (null == fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmts) {
            this.init(null);
        }
    }

    private synchronized void init(SelectList hmInfos) {
        if (null == fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmts) {
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmts = new SelectList(3);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmts.add(DomainConstants.SELECT_ID);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmts.add("from[PSS_PartVariantAssembly].to.name");
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmts.add("from[PSS_PartVariantAssembly].to.id");

            fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRelationSelects = new SelectList(2);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRelationSelects.add(DomainConstants.SELECT_RELATIONSHIP_ID);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRelationSelects.add("attribute[PSS_CustomEffectivityExpression]");
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRelationSelects.add(SELECT_EBOM_QUANTITY);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRelationSelects.add(SELECT_EBOM_TO_UNIT_OF_MEASURE);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRelationSelects.add(SELECT_EBOM_FIND_NUMBER);

            fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmtsPC = new SelectList(2);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmtsPC.add(DomainConstants.SELECT_ID);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmtsPC.add(DomainConstants.SELECT_NAME);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmtsPC.add(SELECT_UNIT_OF_MEASURE);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.selectStmtsPC.add(fpdm.excelreport.diversity.ResolvedPart_mxJPO.SELECT_FROM_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION_TO_ID);
            if (!(null == hmInfos)) {
                ResolvedPart_mxJPO.selectStmtsPC.addAll(hmInfos);
            }
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRelationSelectsPC = new SelectList(1);
            fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRelationSelectsPC.add(DomainConstants.SELECT_RELATIONSHIP_ID);
        }
    }

    public synchronized void builPartInfo(Context context) {
        // System.out.println("Build Part Info " + this.sId);

        // Initialize Part Unit of Measure Range
        if (null == fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRangePart) {
            AttributeType atAttribute = new AttributeType("Unit of Measure");
            try {
                atAttribute.open(context);
                fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRangePart = atAttribute.getChoices();
                atAttribute.close(context);
            } catch (MatrixException e) {
                e.printStackTrace();
            }

            fpdm.excelreport.diversity.ResolvedPart_mxJPO.slRangePart.remove("EA (each)");
        }

        // retrieve part information
        try {
            if (!context.isTransactionActive()) {
                context.start(false);
            }
            BusinessObject boThis = new BusinessObject(this.sId);
            boThis.open(context);
            this.sName = boThis.getName();
            this.sRevision = boThis.getRevision();

            try {
                DomainObject doPart = new DomainObject(this.sId);
                Hashtable htPartInfo = (Hashtable) doPart.getInfo(context, selectStmtsPC);
                // System.out.println("Part infos : " + htPartInfo);
                this.uom = (String) htPartInfo.get(SELECT_UNIT_OF_MEASURE);
                this.htMoreInfos = htPartInfo;
            } catch (Exception e) {
                e.printStackTrace();
            }

            // System.out.println(boThis);
            // String sWhere = "from[PSS_VariantAssemblyProductConfiguration].to.id==\"" + this.pc.getProductConfigurationId() + "\"";

            ExpansionIterator iterVariant = boThis.getExpansionIterator(context, "PSS_PartVariantAssembly", // relationship
                    "PSS_VariantAssembly", // type pattern
                    selectStmtsPC, // list of select statement pertaining to Business Objects
                    slRelationSelectsPC, // list of select statement pertaining to Relationships
                    false, // get To relationships
                    true, // get From relationships
                    (short) 1, // the number of levels to expand, 0 equals expand all
                    null, // where clause to apply to objects, can be empty
                    "", // where clause to apply to relationship, can be empty
                    (short) 0, // the maximum number of objects to return
                    false, // true to check for hidden types per MX_SHOW_HIDDEN_TYPE_OBJECTS setting; false to return all objects, even if hidden
                    false, // true to return each target object only once in expansion
                    (short) 10, // page size to use for streaming data source
                    false // boolean true to force HashTable data to StringList;
            // false will return String for single-valued selects,
            // StringList for multi-valued selects
            );
            MapList mlObjectsListVariant = FrameworkUtil.toMapList(iterVariant, (short) 0, null, null, null, null);

            // System.out.println("Variants of " + this.sId + " : " + mlObjectsListVariant);

            @SuppressWarnings("unchecked")
            Iterator<Hashtable<String, Object>> itVariant = mlObjectsListVariant.iterator();
            while ((itVariant.hasNext()) && (null == this.sVariantId)) {
                Hashtable<String, Object> variant = itVariant.next();

                Object productConfs = variant.get(fpdm.excelreport.diversity.ResolvedPart_mxJPO.SELECT_FROM_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION_TO_ID);
                // System.out.println("************************** " + productConfs.getClass().getName());

                if ("java.lang.String".equals(productConfs.getClass().getName())) {
                    if (variant.get(fpdm.excelreport.diversity.ResolvedPart_mxJPO.SELECT_FROM_VARIANT_ASSEMBLY_PRODUCT_CONFIGURATION_TO_ID).equals(this.pc.getProductConfigurationId())) {
                        this.sVariantId = (String) variant.get(DomainConstants.SELECT_ID);
                        this.sVariantName = (String) variant.get(DomainConstants.SELECT_NAME);
                    }
                } else if ("matrix.util.StringList".equals(productConfs.getClass().getName())) {
                    StringList slProductConfs = (StringList) productConfs;
                    if (slProductConfs.contains(this.pc.getProductConfigurationId())) {
                        this.sVariantId = (String) variant.get(DomainConstants.SELECT_ID);
                        this.sVariantName = (String) variant.get(DomainConstants.SELECT_NAME);
                    }
                }
            }

            boThis.close(context);
        } catch (MatrixException e1) {
            e1.printStackTrace();
        } finally {
            try {
                context.abort();
            } catch (MatrixException e) {
                e.printStackTrace();
            }
        }

        // add children
        try {
            BusinessObject boObj = new BusinessObject(this.sId);
            if (!context.isTransactionActive()) {
                context.start(false);
            }
            ExpansionIterator iter = boObj.getExpansionIterator(context, DomainConstants.RELATIONSHIP_EBOM, // relationship
                    DomainConstants.TYPE_PART, // type pattern
                    selectStmts, // list of select statement pertaining to Business Objects
                    slRelationSelects, // list of select statement pertaining to Relationships
                    false, // get To relationships
                    true, // get From relationships
                    (short) 1, // the number of levels to expand, 0 equals expand all
                    null, // where clause to apply to objects, can be empty
                    "", // where clause to apply to relationship, can be empty
                    (short) 0, // the maximum number of objects to return
                    false, // true to check for hidden types per MX_SHOW_HIDDEN_TYPE_OBJECTS setting; false to return all objects, even if hidden
                    false, // true to return each target object only once in expansion
                    (short) 10, // page size to use for streaming data source
                    false // boolean true to force HashTable data to StringList;
            // false will return String for single-valued selects,
            // StringList for multi-valued selects
            );
            MapList mlObjectsList = FrameworkUtil.toMapList(iter, (short) 0, null, null, null, null);
            // System.out.println("Children of " + this.sId + " : " + mlObjectsList);

            @SuppressWarnings("unchecked")
            Iterator<Hashtable<String, String>> itChildren = mlObjectsList.iterator();
            while (itChildren.hasNext()) {
                Hashtable<String, String> child = itChildren.next();
                if ((fpdm.excelreport.diversity.ResolvedPart_mxJPO.isPartInProductConf(context, this.pc, child)) || "".equals(this.pc.getProductConfigurationId())) {
                    fpdm.excelreport.diversity.Children_mxJPO thisChild = new fpdm.excelreport.diversity.Children_mxJPO();

                    thisChild.id = child.get(DomainConstants.SELECT_ID);
                    System.out.println("Child : " + child);
                    System.out.println("Quantity : " + child.get(SELECT_EBOM_QUANTITY));
                    thisChild.quantity = child.get(SELECT_EBOM_QUANTITY);
                    thisChild.fn = child.get(SELECT_EBOM_FIND_NUMBER);
                    thisChild.uom = child.get(SELECT_EBOM_TO_UNIT_OF_MEASURE);

                    this.children.add(thisChild);
                }
            }
        } catch (MatrixException e) {
            e.printStackTrace();
        } finally {
            try {
                context.abort();
            } catch (MatrixException e) {
                e.printStackTrace();
            }
        }
    }

    public String getId() {
        return sId;
    }

    public void setId(String sId) {
        this.sId = sId;
    }

    public String getName() {
        return sName;
    }

    public void setName(String sname) {
        this.sName = sname;
    }

    public String getRevision() {
        return sRevision;
    }

    public void setRevision(String sRevision) {
        this.sRevision = sRevision;
    }

    public String getVariantName() {
        return sVariantName;
    }

    public String getVariantId() {
        return sVariantId;
    }

    public void setVariantId(String sVariantId) {
        this.sVariantId = sVariantId;
    }

    public ArrayList<String> getChildren() {
        ArrayList<String> resultChildren = new ArrayList<String>();

        Iterator<fpdm.excelreport.diversity.Children_mxJPO> itChild = children.iterator();

        while (itChild.hasNext()) {
            fpdm.excelreport.diversity.Children_mxJPO thisChild = itChild.next();

            resultChildren.add(thisChild.id);
        }

        return resultChildren;
    }

    public List<fpdm.excelreport.diversity.Children_mxJPO> getRealChildren() {
        return this.children;
    }

    public fpdm.excelreport.diversity.ProductConfiguration_mxJPO getPc() {
        return pc;
    }

    public void setPc(fpdm.excelreport.diversity.ProductConfiguration_mxJPO pc) {
        this.pc = pc;
    }

    public static boolean isPartInProductConf(Context context, fpdm.excelreport.diversity.ProductConfiguration_mxJPO productConf, Hashtable<String, String> infoRelation) {
        if ("".equals(infoRelation.get("attribute[PSS_CustomEffectivityExpression]"))) {
            return true;
        } else {
            String customEffectivityExpression = infoRelation.get("attribute[PSS_CustomEffectivityExpression]");
            fpdm.excelreport.diversity.EBOMEffectivityConfigurations_mxJPO ebeConfigurations = new fpdm.excelreport.diversity.EBOMEffectivityConfigurations_mxJPO(customEffectivityExpression);
            return ebeConfigurations.match(productConf);
        }
    }

    public String getInfo(String infoName) {
        return this.htMoreInfos.get(infoName);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getId());
        sb.append(" / ");
        sb.append(this.getName());
        sb.append(" / ");
        sb.append(this.getVariantId());
        sb.append(" [[[ ");
        sb.append(this.htMoreInfos);
        sb.append(" ]]] ");
        sb.append(this.getChildren().size() + " children ---" + this.children + "---");

        return sb.toString();
    }
}
