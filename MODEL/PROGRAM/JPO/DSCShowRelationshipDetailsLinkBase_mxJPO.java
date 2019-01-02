
/*
 ** DSCShowRelationshipDetailsLinkBase
 **
 ** @ Dassault Systemes, 2002-2007. All rights reserved.
 **
 ** Program to display Relationship Details Icon
 */

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Relationship;

import com.matrixone.MCADIntegration.server.MCADServerResourceBundle;
import com.matrixone.MCADIntegration.server.MCADServerSettings;
import com.matrixone.MCADIntegration.server.beans.MCADMxUtil;
import com.matrixone.MCADIntegration.server.beans.IEFIntegAccessUtil;
import com.matrixone.MCADIntegration.server.beans.MCADServerGeneralUtil;
import com.matrixone.MCADIntegration.server.cache.IEFGlobalCache;
import com.matrixone.MCADIntegration.utils.MCADGlobalConfigObject;
import com.matrixone.apps.domain.util.MapList;

public class DSCShowRelationshipDetailsLinkBase_mxJPO {
    protected HashMap integrationNameGCOTable = null;

    protected MCADServerResourceBundle serverResourceBundle = null;

    protected IEFIntegAccessUtil util = null;

    protected MCADServerGeneralUtil serverGeneralUtil = null;

    protected String localeLanguage = null;

    protected HashMap paramMap = null;

    protected String strFileFormat = "";

    protected IEFGlobalCache cache = null;

    public DSCShowRelationshipDetailsLinkBase_mxJPO(Context context, String[] args) throws Exception {

    }

    public Object getHtmlString(Context context, String[] args) throws Exception {
        paramMap = (HashMap) JPO.unpackArgs(args);

        MapList relBusObjPageList = (MapList) paramMap.get("objectList");
        Map paramList = (Map) paramMap.get("paramList");

        localeLanguage = (String) paramList.get("languageStr");

        integrationNameGCOTable = (HashMap) paramList.get("GCOTable");

        Vector columnCellContentList = new Vector();

        serverResourceBundle = new MCADServerResourceBundle(localeLanguage);
        cache = new IEFGlobalCache();
        util = new IEFIntegAccessUtil(context, serverResourceBundle, cache);

        Vector assignedIntegrations = util.getAssignedIntegrations(context);

        for (int i = 0; i < relBusObjPageList.size(); i++) {
            StringBuffer htmlBuffer = new StringBuffer();

            try {
                Map objDetails = (Map) relBusObjPageList.get(i);
                String objectId = (String) objDetails.get("id");
                String integrationName = util.getIntegrationName(context, objectId);
                String relId = (String) objDetails.get("id[connection]");
                String isRootNode = (String) objDetails.get("Root Node");
                String relDetailsURL = "";
                String relDetailsToolTip = serverResourceBundle.getString("mcadIntegration.Server.AltText.RelationshipDetails");
                String imgDetails = "";
                if ((isRootNode != null) && !("".equals(isRootNode)) && (isRootNode.equalsIgnoreCase("true"))) {
                    htmlBuffer.append("");
                } else {
                    if (integrationName != null && integrationNameGCOTable.containsKey(integrationName) && assignedIntegrations.contains(integrationName)) {
                        MCADGlobalConfigObject gco = (MCADGlobalConfigObject) integrationNameGCOTable.get(integrationName);
                        String relName = "";
                        if ((relId != null) && !("".equals(relId))) {
                            matrix.db.Relationship rel = new Relationship(relId);
                            rel.open(context);
                            relName = rel.getTypeName();
                            rel.close(context);
                        } else {
                            Hashtable relClassMapTable = gco.getRelationshipsOfClass(MCADServerSettings.CAD_SUBCOMPONENT_LIKE);
                            Enumeration relClassEnums = relClassMapTable.keys();
                            if (relClassEnums.hasMoreElements()) {
                                relName = (String) relClassEnums.nextElement();
                            }
                        }

                        String cadRelName = gco.getCADRelFromMxRel(relName);
                        String relDirection = gco.getRelationshipDirection(cadRelName);
                        boolean isRelExternalRefLike = gco.isRelationshipOfClass(relName, MCADServerSettings.EXTERNAL_REFERENCE_LIKE);

                        if (relDirection.equalsIgnoreCase("to")) {
                            if (isRelExternalRefLike) {
                                imgDetails = "../integrations/images/iconTreeToArrowExternalRef.gif";
                            } else {
                                imgDetails = "../iefdesigncenter/images/iconTreeToArrow.gif";
                            }
                        } else {
                            if (isRelExternalRefLike) {
                                imgDetails = "../integrations/images/iconTreeFromArrowExternalRef.gif";
                            } else {
                                imgDetails = "../iefdesigncenter/images/iconTreeFromArrow.gif";
                            }
                        }

                        relDetailsURL = "../iefdesigncenter/emxInfoRelationshipDetailsDialogFS.jsp?Target Location=popup&amp;integrationName=" + integrationName + "&amp;objectId=" + objectId
                                + "&amp;suiteKey=DesignerCentral" + "&amp;relId=" + relId;
                        String relDetailsHref = "javascript:showNonModalDialog('" + relDetailsURL + "','700','500')";
                        htmlBuffer.append(getFeatureIconContent(relDetailsHref, imgDetails, relDetailsToolTip, ""));
                    }
                }
            } catch (Exception e) {

            }

            columnCellContentList.add(htmlBuffer.toString());
        }

        return columnCellContentList;
    }

    protected String getFeatureIconContent(String href, String featureImage, String toolTop, String targetName) {
        StringBuffer featureIconContent = new StringBuffer();

        featureIconContent.append("<a href=\"");
        featureIconContent.append(href);
        featureIconContent.append("\" ");
        if (targetName.length() > 0) {
            featureIconContent.append("target=\"");
            featureIconContent.append(targetName);
            featureIconContent.append("\"");
        }
        featureIconContent.append(" ><img src=\"");
        featureIconContent.append(featureImage);
        featureIconContent.append("\" border=\"0\" title=\"");
        featureIconContent.append(toolTop);
        featureIconContent.append("\"/></a>");

        return featureIconContent.toString();
    }

}
