import java.util.Hashtable;

import matrix.db.Attribute;
import matrix.db.BusinessObject;
import matrix.db.Context;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;

public class ENODC5GetLatestFrozenRevision_mxJPO extends MCADIntegGetLatestFrozenRevisionBase_mxJPO {

    protected String ATTR_CADTYPE;

    public ENODC5GetLatestFrozenRevision_mxJPO() {
        super();
    }

    public ENODC5GetLatestFrozenRevision_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    protected void init(Context context, String[] packedGCO, String sLanguage) throws Exception {
        super.init(context, packedGCO, sLanguage);
        ATTR_CADTYPE = MCADMxUtil.getActualNameForAEFData(context, "attribute_CADType");
    }

    public Hashtable getLatestForObjectIds(Context context, String[] oids) throws Exception {
        Hashtable returnTable = super.getLatestForObjectIds(context, oids);

        for (String id : oids) {
            BusinessObject Object = new BusinessObject(id);
            Attribute CADType = Object.getAttributeValues(context, ATTR_CADTYPE);
            if (CADType.getValue().equals("embeddedComponent")) {
                returnTable.put(id, id);
            }
        }

        return returnTable;
    }
}
