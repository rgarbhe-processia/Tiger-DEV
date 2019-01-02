import matrix.db.Context;
import matrix.util.MatrixException;

import matrix.db.JPO;

import com.matrixone.MCADIntegration.DataValidation.ValidationObject;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;

public class CustomBaseRule_mxJPO {
    Context context;

    ValidationObject validationObject;

    MCADGlobalConfigObject gcoObject = null;

    protected String _errorMessage = null;

    protected void AddErrorMessage(String iErrMsg) {
        if (null != iErrMsg) {
            if (null == _errorMessage)
                _errorMessage = " " + iErrMsg;
            else
                _errorMessage += " " + iErrMsg;
        }
    }

    public String AddErrorMessage(String sumErrorMessge, String iErrMsg) {
        if (null != iErrMsg) {
            if (null == sumErrorMessge)
                sumErrorMessge = "\n" + iErrMsg;
            else
                sumErrorMessge += "\n" + iErrMsg;
        }

        return sumErrorMessge;
    }

    public String validate(Context context, String[] args) throws MatrixException, Exception {
        this.context = context;
        String[] gco = new String[2];
        gco[0] = args[0];
        gco[1] = args[1];
        gcoObject = (MCADGlobalConfigObject) JPO.unpackArgs(gco);

        gco[0] = args[2];
        gco[1] = args[3];
        validationObject = (ValidationObject) JPO.unpackArgs(gco);

        return "Success";
    }
}
