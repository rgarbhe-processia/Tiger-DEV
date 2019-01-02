import com.matrixone.apps.domain.util.FrameworkException;
import com.ds.dso.exportfiles.emxExtractSchema;
import com.ds.dso.license.SpinnerLicenseCheck;

import matrix.db.Context;

import com.matrixone.apps.domain.util.MqlUtil;

public class emxExtract_mxJPO {

    @SuppressWarnings("deprecation")
    public int mxMain(Context context, String[] args) throws Exception {
        emxExtractSchema emxExtractSchemaObj = new emxExtractSchema();
        String mCommandEnv = "get env 1";

        String mCommandEnv2 = "get env 2";

        String bJPOExtraction = "get env JPOEXTRACTION";

        SpinnerLicenseCheck spinnerLicenseCheckObj = new SpinnerLicenseCheck();
        String sSchemaType = MqlUtil.mqlCommand(context, mCommandEnv);
        String sSchemaName = MqlUtil.mqlCommand(context, mCommandEnv2);
        String bJPO = MqlUtil.mqlCommand(context, bJPOExtraction);
        boolean isValid = spinnerLicenseCheckObj.SpinnerRunTimeCheck(context);
        if (isValid) {
            emxExtractSchemaObj.setbJPO(bJPO);
            emxExtractSchemaObj.extractFiles(context, sSchemaType, sSchemaName, bJPO);
        }
        return 0;

    }

}
