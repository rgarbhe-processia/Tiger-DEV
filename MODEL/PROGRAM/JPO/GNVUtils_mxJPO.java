import com.matrixone.apps.domain.DomainObject;
import com.matrixone.fcs.common.ImageRequestData;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import matrix.db.BusinessObjectProxy;
import matrix.db.Context;
import matrix.db.FileList;
import matrix.db.JPO;
import matrix.util.StringList;

public class GNVUtils_mxJPO {

    public static Integer iTraceLevel = 10;

    public GNVUtils_mxJPO(Context context, String[] args) throws Exception {
    }

    // Trace output
    public static void writeTrace(String sJPO, String sMethod, String sText, String sValue, Boolean bTime, Integer iLevel) {

        if (iTraceLevel <= iLevel) {

            if (sText.equalsIgnoreCase("start")) {
                sText = "---------- START ----------";
            } else if (sText.equalsIgnoreCase("end")) {
                sText = "----------  END  ----------";
            }

            if (bTime)
                sText = " (" + new Date() + ") " + sText;

            StringBuilder sbText = new StringBuilder();
            sbText.append(sJPO);
            sbText.append(".");
            sbText.append(sMethod);
            sbText.append(" : ");
            sbText.append(sText);

            if (!"".equals(sValue)) {
                sbText.append(" = ");
                sbText.append(sValue);
            }

            System.out.println(sbText.toString());

        }

    }

    public static void writeTrace(String sJPO, String sMethod, String sText) {
        writeTrace(sJPO, sMethod, sText, "", false, 1);
    }

    public static void writeTrace(String sJPO, String sMethod, String sText, Boolean bTime) {
        writeTrace(sJPO, sMethod, sText, "", bTime, 1);
    }

    public static void writeTrace(String sJPO, String sMethod, String sText, Integer iLevel) {
        writeTrace(sJPO, sMethod, sText, "", false, iLevel);
    }

    public static void writeTrace(String sJPO, String sMethod, String sText, Object sValue) {
        writeTrace(sJPO, sMethod, sText, (String) sValue, false, 1);
    }

    public static void writeTrace(String sJPO, String sMethod, String sText, Object sValue, Boolean bTime) {
        writeTrace(sJPO, sMethod, sText, (String) sValue, bTime, 1);
    }

    // Image Display
    public static String getPrimaryImageURL(Context context, String[] args, String sOID, String sFormat, String sMCSURL, String sDefaultImage) throws Exception {

        // writeTrace("GNVUtils", "getPrimaryImageURL", "START");

        if (null == sDefaultImage || "".equals(sDefaultImage)) {
            sDefaultImage = "../common/images/icon48x48ImageNotFound.gif";
        }

        String sResult = sDefaultImage;
        DomainObject dObject = new DomainObject(sOID);
        StringList busSelects = new StringList();

        busSelects.add("to[Image Holder].from.id");
        busSelects.add("to[Image Holder].from.attribute[Primary Image]");
        busSelects.add("from[Primary Image].to.id");

        Map mData = dObject.getInfo(context, busSelects);
        String sOIDImageHolder = (String) mData.get("to[Image Holder].from.id");
        String sFileName = (String) mData.get("to[Image Holder].from.attribute[Primary Image]");

        if (null == sOIDImageHolder) {
            sOIDImageHolder = (String) mData.get("from[Primary Image].to.id");
            if (null != sOIDImageHolder) {
                DomainObject doImage = new DomainObject(sOIDImageHolder);
                FileList fileList = doImage.getFiles(context);
                if (fileList.size() > 0) {
                    for (int k = 0; k < fileList.size(); k++) {
                        matrix.db.File fTemp = (matrix.db.File) fileList.get(k);
                        String sFormatFile = fTemp.getFormat();
                        if (sFormatFile.equals(sFormat)) {
                            sFileName = fTemp.getName();
                            break;
                        }
                    }
                }
            }
        }

        if (null != sOIDImageHolder) {

            if (!sFormat.equals("generic")) {
                sFileName = sFileName.substring(0, sFileName.lastIndexOf(".")) + ".jpg";
            }

            ArrayList bopArrayList = new ArrayList();
            BusinessObjectProxy bop = new BusinessObjectProxy(sOIDImageHolder, sFormat, sFileName, false, false);
            bopArrayList.add(bop);

            if (null == sMCSURL)
                sMCSURL = getMCSURL(context, args);
            else if (sMCSURL.equals(""))
                sMCSURL = getMCSURL(context, args);

            String[] tmpImageUrls = ImageRequestData.getImageURLS(context, sMCSURL, bopArrayList);
            sResult = tmpImageUrls[0];
        }

        return sResult;

    }

    public static String getMCSURL(Context context, String[] args) throws Exception {

        Map programMap = (Map) JPO.unpackArgs(args);
        Map imageData = new HashMap();

        if (programMap.containsKey("ImageData")) {
            imageData = (Map) programMap.get("ImageData");
        } else if (programMap.containsKey("paramList")) {
            Map paramList = (Map) programMap.get("paramList");
            imageData = (Map) paramList.get("ImageData");
        } else {
            Map requestMap = (Map) programMap.get("requestMap");
            imageData = (Map) requestMap.get("ImageData");
        }

        return (String) imageData.get("MCSURL");

    }

}