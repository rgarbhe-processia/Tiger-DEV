package fpdm.excelreport.diversity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EBOMEffectivityConfiguration_mxJPO {
    String hardwareProductId = "";

    ArrayList<fpdm.excelreport.diversity.EBOMEffectivityOptionValue_mxJPO> alOptionValue = new ArrayList<fpdm.excelreport.diversity.EBOMEffectivityOptionValue_mxJPO>();

    int matchingItem = 0;

    public EBOMEffectivityConfiguration_mxJPO(String configuration) {
        Pattern pattConfHard = Pattern.compile("([0-9A-F]{32})~([0-9A-F]{32})");
        Matcher m = pattConfHard.matcher(configuration);
        if (m.find()) {
            hardwareProductId = m.group(2);
        }

        String[] asOptionValues = configuration.split(Pattern.quote(" AND "));

        for (int x = 0; x < asOptionValues.length; x++) {
            fpdm.excelreport.diversity.EBOMEffectivityOptionValue_mxJPO ebeOptionValue = new fpdm.excelreport.diversity.EBOMEffectivityOptionValue_mxJPO(asOptionValues[x]);
            alOptionValue.add(ebeOptionValue);
        }
    }

    public boolean match(fpdm.excelreport.diversity.ProductConfiguration_mxJPO productConf) {
        if (!hardwareProductId.equals(productConf.getHardwareProductId())) {
            return false;
        }

        Iterator<fpdm.excelreport.diversity.EBOMEffectivityOptionValue_mxJPO> itOptionvalue = alOptionValue.iterator();
        while (itOptionvalue.hasNext()) {
            fpdm.excelreport.diversity.EBOMEffectivityOptionValue_mxJPO value = itOptionvalue.next();
            if (!value.match(productConf)) {
                return false;
            }
        }

        return true;
    }
}
