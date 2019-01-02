package fpdm.excelreport.diversity;

import java.util.ArrayList;
import java.util.Iterator;

import com.matrixone.apps.domain.DomainObject;

import matrix.db.Context;
import matrix.util.StringList;

public class ProductConfiguration_mxJPO {
    private String sProductConfigurationId = "";

    private String hardwareProductId = "";

    private String sName = "";

    private ArrayList<String> alValues = new ArrayList<String>();

    public ProductConfiguration_mxJPO(String sProductConfigurationId, Context context) throws Exception {
        DomainObject doConfiguration = new DomainObject(sProductConfigurationId);
        doConfiguration.open(context);
        this.sName = doConfiguration.getName();
        this.sProductConfigurationId = sProductConfigurationId;
        this.hardwareProductId = doConfiguration.getInfo(context, "to[Feature Product Configuration].from.to[Main Product].from.physicalid");
        StringList hardwareConfList = doConfiguration.getInfoList(context, "from[Selected Options].torel.physicalid");

        @SuppressWarnings("unchecked")
        Iterator<String> itHardwareConf = hardwareConfList.iterator();
        while (itHardwareConf.hasNext()) {
            String sConf = itHardwareConf.next();
            this.alValues.add(sConf);
        }
        doConfiguration.close(context);
    }

    public ProductConfiguration_mxJPO(String hardwareProductId, ArrayList<String> alValues, String sProductConfigurationId) {
        this.hardwareProductId = hardwareProductId;
        this.alValues = alValues;
        this.sProductConfigurationId = sProductConfigurationId;
    }

    public String getHardwareProductId() {
        return hardwareProductId;
    }

    public void setHardwareProductId(String hardwareProductId) {
        this.hardwareProductId = hardwareProductId;
    }

    public ArrayList<String> getValues() {
        return alValues;
    }

    public void setValues(ArrayList<String> alValues) {
        this.alValues = alValues;
    }

    public String getName() {
        return this.sName;
    }

    public String getProductConfigurationId() {
        return sProductConfigurationId;
    }

    public void setProductConfigurationId(String sProductConfigurationId) {
        this.sProductConfigurationId = sProductConfigurationId;
    }
}
