package fpdm.excelreport.diversity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EBOMEffectivityOptionValue_mxJPO {
    ArrayList<String> alValues = new ArrayList<String>();

    public EBOMEffectivityOptionValue_mxJPO(String optionValue) {
        // System.out.println("<Test effectivity> New Value " + optionValue);

        String[] asValues = optionValue.split(Pattern.quote(" OR "));

        for (int x = 0; x < asValues.length; x++) {
            String sValue = asValues[x];

            // System.out.println("<Test effectivity> Test Value final " + sValue);

            Pattern pattConfHard = Pattern.compile("([0-9A-F]{32})~([0-9A-F]{32})");
            Matcher m = pattConfHard.matcher(sValue);

            if (m.find()) {
                // System.out.println("<Test effectivity> New Value final " + m.group(1));
                alValues.add(m.group(1));
            }
        }
    }

    public boolean match(fpdm.excelreport.diversity.ProductConfiguration_mxJPO productConf) {
        ArrayList<String> alConfigurations = productConf.getValues();

        Iterator<String> iValue = alValues.iterator();

        while (iValue.hasNext()) {
            if (alConfigurations.contains(iValue.next())) {
                return true;
            }
        }

        return false;
    }
}
