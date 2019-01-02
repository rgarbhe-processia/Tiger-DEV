import java.util.List;
import java.util.Map;
import java.util.Vector;

import matrix.db.BusinessObject;
import matrix.db.BusinessObjectWithSelect;
import matrix.db.BusinessObjectWithSelectList;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;

import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;

public class ENODC5GetAttributes_mxJPO {
    public Object getFileRenamedFromAndFileSource(Context context, String args[]) {
        StringList specifiedAttributes = new StringList();

        try {
            String SELECT_IEF_FILE_SOURCE_ATTR = new StringBuffer("attribute[").append(MCADMxUtil.getActualNameForAEFData(context, "attribute_IEF-FileSource")).append("]").toString();
            String SELECT_RENAMED_FROM_ATTR = new StringBuffer("attribute[").append(MCADMxUtil.getActualNameForAEFData(context, "attribute_RenamedFrom")).append("]").toString();
            specifiedAttributes.add(SELECT_IEF_FILE_SOURCE_ATTR);
            specifiedAttributes.add(SELECT_RENAMED_FROM_ATTR);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getSpecifiedAttributes(context, args, specifiedAttributes);
    }

    public Object getClonedFrom(Context context, String args[]) {
        StringList specifiedAttributes = new StringList();

        try {
            String SELECT_MCADINTEG_CLONE_FROM_ATTR = new StringBuffer("attribute[").append(MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-ClonedFrom")).append("]").toString();
            specifiedAttributes.add(SELECT_MCADINTEG_CLONE_FROM_ATTR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getSpecifiedAttributes(context, args, specifiedAttributes);
    }

    public Object getSourceObj(Context context, String args[]) {
        StringList specifiedAttributes = new StringList();

        try {
            String SELECT_MCADINTEG_SOURCE_OBJ_ATTR = new StringBuffer("attribute[").append(MCADMxUtil.getActualNameForAEFData(context, "attribute_MCADInteg-SourceObj")).append("]").toString();
            specifiedAttributes.add(SELECT_MCADINTEG_SOURCE_OBJ_ATTR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getSpecifiedAttributes(context, args, specifiedAttributes);
    }

    private Object getSpecifiedAttributes(Context context, String args[], StringList specifiedAttributes) {
        Vector returnValues = new Vector();
        try {
            Map dataMap = (Map) JPO.unpackArgs(args);
            List objectList = (List) dataMap.get("objectList");
            String[] objsId = getObjectsId(dataMap);

            BusinessObjectWithSelectList busSelectDataList = BusinessObject.getSelectBusinessObjectData(context, objsId, specifiedAttributes);

            for (int i = 0; i < busSelectDataList.size(); i++) {
                BusinessObjectWithSelect busSelectData = busSelectDataList.getElement(i);
                String attributeValue = "";

                for (int j = 0; j < specifiedAttributes.size(); j++) {
                    String attribute = (String) specifiedAttributes.get(j);

                    if (j == 0)
                        attributeValue = busSelectData.getSelectData(attribute);
                    else
                        attributeValue = attributeValue + "," + busSelectData.getSelectData(attribute);
                }
                returnValues.add(attributeValue);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValues;
    }

    private String[] getObjectsId(Map dataMap) {
        List objectList = (List) dataMap.get("objectList");
        String[] objIds = new String[objectList.size()];

        for (int i = 0; i < objectList.size(); i++) {
            Map idsMap = (Map) objectList.get(i);
            objIds[i] = idsMap.get("id").toString();
        }
        return objIds;
    }

}
