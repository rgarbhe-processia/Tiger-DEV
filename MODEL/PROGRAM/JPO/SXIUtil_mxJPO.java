import matrix.db.Context;

public class SXIUtil_mxJPO {

    public SXIUtil_mxJPO(Context context, String[] args) {
    }

    public String mxMain(Context ctx, String[] args) throws Exception {
        if (args != null && args.length > 0)
            return args[0];
        return "";
    }
}
