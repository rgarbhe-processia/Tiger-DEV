package fpdm.excelreport.diversity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

public class EBOMEffectivityConfigurations_mxJPO {
    ArrayList<fpdm.excelreport.diversity.EBOMEffectivityConfiguration_mxJPO> alConfiguration = new ArrayList<fpdm.excelreport.diversity.EBOMEffectivityConfiguration_mxJPO>();

    public EBOMEffectivityConfigurations_mxJPO(String configurationString) {
        // System.out.println("*************************************************************************************");
        String[] asConfigurations = configurationString.split(Pattern.quote("||"));

        for (int x = 0; x < asConfigurations.length; x++) {
            fpdm.excelreport.diversity.EBOMEffectivityConfiguration_mxJPO ebeConfiguration = new fpdm.excelreport.diversity.EBOMEffectivityConfiguration_mxJPO(asConfigurations[x]);
            alConfiguration.add(ebeConfiguration);
        }

    }

    public boolean match(fpdm.excelreport.diversity.ProductConfiguration_mxJPO productConf) {
        Iterator<fpdm.excelreport.diversity.EBOMEffectivityConfiguration_mxJPO> itConfigurations = alConfiguration.iterator();

        while (itConfigurations.hasNext()) {
            fpdm.excelreport.diversity.EBOMEffectivityConfiguration_mxJPO configuration = itConfigurations.next();
            boolean matchConfiguration = configuration.match(productConf);
            if (matchConfiguration) {
                return matchConfiguration;
            }
        }

        return false;
    }
}
