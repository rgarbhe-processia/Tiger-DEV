package pss.ecm.impactanalysis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.jdom.Element;
import com.matrixone.jdom.JDOMException;
import com.matrixone.jdom.input.SAXBuilder;

import matrix.db.Context;

public class ImpactAnalysisXMLUtil_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ImpactAnalysisXMLUtil_mxJPO.class);

    static Map<String, Map<String, List<Map<String, String>>>> mpRoleVsViews = new ConcurrentHashMap<>();

    static com.matrixone.jdom.Document xmlDocument = null;

    public static synchronized Map<String, List<Map<String, String>>> getViewVsDisplayAttrMap(Context context, String strRoleName) throws FrameworkException, JDOMException, IOException {
        logger.debug("pss.ecm.impactanalysis.ImpactAnalysisXMLutil:getViewVsDisplayAttrMap:START");

        Map<String, List<Map<String, String>>> mpViewVsDisplayAttrMap = new HashMap<String, List<Map<String, String>>>();
        try {
            if (mpRoleVsViews.containsKey(strRoleName)) {
                mpViewVsDisplayAttrMap = (Map<String, List<Map<String, String>>>) mpRoleVsViews.get(strRoleName);
                return mpViewVsDisplayAttrMap;
            }

            if (xmlDocument == null) {
                String MQLResult = MqlUtil.mqlCommand(context, "print page $1 select content dump", "ECMOnlineImpactAnalysisConfiguration");
                InputStream stream = new ByteArrayInputStream(MQLResult.getBytes("UTF-8"));
                SAXBuilder builder = new SAXBuilder();
                xmlDocument = builder.build(stream);
            }
            Element root = xmlDocument.getRootElement();
            List<Element> lstRoles = root.getChildren();
            for (Element elementRole : lstRoles) {
                String strTempRoleName = elementRole.getAttributeValue("name");
                if (strTempRoleName.equalsIgnoreCase(strRoleName)) {

                    List<Element> lstViews = elementRole.getChildren();
                    for (Element elementView : lstViews) {
                        List<Map<String, String>> lstDisplayAttributes = new ArrayList<Map<String, String>>();
                        String strViewName = elementView.getAttributeValue("name");

                        List<Element> lstDisplayAttributesElements = elementView.getChildren();
                        for (Element elementDisplayAttribute : lstDisplayAttributesElements) {

                            Map<String, String> mapDispAttribute = new HashMap<String, String>();
                            String strTitle = elementDisplayAttribute.getAttributeValue("title");
                            mapDispAttribute.put("PSS_Title", strTitle);

                            String strDomain = elementDisplayAttribute.getAttributeValue("domain");
                            mapDispAttribute.put("PSS_Domain", strDomain);

                            lstDisplayAttributes.add(mapDispAttribute);
                        }

                        mpViewVsDisplayAttrMap.put(strViewName, lstDisplayAttributes);
                    }

                    break;
                }

            }
            mpRoleVsViews.put(strRoleName, mpViewVsDisplayAttrMap);
            logger.debug("pss.ecm.impactanalysis.ImpactAnalysisXMLutil:getViewVsDisplayAttrMap:END");

        } catch (RuntimeException ex) {
            logger.error("pss.ecm.impactanalysis.ImpactAnalysisXMLutil:getViewVsDisplayAttrMap:ERROR ", ex);
            ex.printStackTrace();
            throw ex;
        }
        return mpViewVsDisplayAttrMap;
    }
}
