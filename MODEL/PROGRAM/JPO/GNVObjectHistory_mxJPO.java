import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MqlUtil;
import com.matrixone.apps.domain.util.PersonUtil;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.i18nNow;
import com.matrixone.apps.framework.ui.UINavigatorUtil;
import com.matrixone.fcs.common.ImageRequestData;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import matrix.db.AttributeType;
import matrix.db.BusinessObject;
import matrix.db.BusinessObjectProxy;
import matrix.db.Context;
import matrix.db.History;
import matrix.db.HistoryItr;
import matrix.db.HistoryList;
import matrix.db.JPO;
import matrix.db.NameValue;
import matrix.db.NameValueItr;
import matrix.db.NameValueList;
import matrix.db.Page;
import matrix.util.MatrixException;
import matrix.util.StringList;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;

public class GNVObjectHistory_mxJPO {

    SimpleDateFormat sdfShort = new SimpleDateFormat("M/d/yyyy h:mm:ss aaa");

    List<String> lExcludedAttributes = new ArrayList<String>();

    List<String> lExcludedRelationships = new ArrayList<String>();

    public GNVObjectHistory_mxJPO(Context context, String[] args) throws Exception {
    }

    public String[] getData(Context context, String[] args) throws Exception {

        // GNVUtils_mxJPO.writeTrace("GNVObjectHistory", "getData", "START");

        String[] aResults = new String[9];
        HashMap programMap = (HashMap) JPO.unpackArgs(args);
        String sMCSURL = (String) programMap.get("MCSURL");
        String sForm = (String) programMap.get("form");
        String sOID = (String) programMap.get("objectId");
        String sShowName = (String) programMap.get("showName");
        String sLanguage = (String) programMap.get("language");
        sLanguage = sLanguage.substring(0, 2);
        String[] aAttributes = new String[0];
        String[] aLabels = new String[0];

        // Retrieve configuration settings
        Page page = new Page("GNVObjectHistory");
        page.open(context);

        String line = "";
        InputStream input = page.getContentsAsStream(context);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        while ((line = reader.readLine()) != null) {

            if (line.contains("=")) {

                String sSetting = line.substring(0, line.indexOf("="));
                String sValue = line.substring(line.indexOf("=") + 1);
                sSetting = sSetting.trim();
                sValue = sValue.trim();

                if (sSetting.equals("objectHistory.ExcludeAttributes"))
                    addConfigurationSettings(sValue, lExcludedAttributes);
                else if (sSetting.equals("objectHistory.ExcludeRelationships"))
                    addConfigurationSettings(sValue, lExcludedRelationships);

            }

        }

        page.close(context);

        // Parse History Data
        StringBuilder[] sbData = new StringBuilder[8];

        for (int i = 0; i < sbData.length; i++)
            sbData[i] = new StringBuilder();

        getFormDetails(context, sForm, aAttributes, aLabels, sLanguage);
        parseHistory(context, sOID, sbData, sLanguage, aAttributes, aLabels, sMCSURL);

        for (int i = 0; i < sbData.length; i++)
            if (sbData[i].length() > 0)
                sbData[i].setLength(sbData[i].length() - 1);

        // Set return values
        aResults[0] = sbData[0].toString(); // ids
        aResults[1] = sbData[1].toString(); // names
        aResults[2] = sbData[2].toString(); // time
        aResults[3] = sbData[3].toString(); // state
        aResults[4] = sbData[4].toString(); // user
        aResults[5] = sbData[5].toString(); // value
        aResults[6] = sbData[6].toString(); // sort
        aResults[7] = sbData[7].toString(); // sbFilter
        aResults[8] = getObjectHistoryTitle(context, sOID, sShowName);

        return aResults;

    }

    public void addConfigurationSettings(String sValues, List<String> list) {
        String[] aValues = sValues.split(",");
        for (int i = 0; i < aValues.length; i++) {
            String sValue = aValues[i].trim();
            list.add(sValue);
        }
    }

    public void getFormDetails(Context context, String sForm, String[] aAttributes, String[] aLabels, String sLanguage) throws FrameworkException {

        // GNVUtils_mxJPO.writeTrace("GNVObjectHistory", "getFormDetails", "START");

        String sFormTest = MqlUtil.mqlCommand(context, true, "list form '" + sForm + "';", true);

        if (!"".equals(sFormTest)) {

            String sFieldName = MqlUtil.mqlCommand(context, true, "print form '" + sForm + "' select field.name dump;", true);
            String sFieldExpression = MqlUtil.mqlCommand(context, true, "print form '" + sForm + "' select field.expression dump;", true);
            String sFieldSuite = MqlUtil.mqlCommand(context, true, "print form '" + sForm + "' select field.setting[Registered Suite] dump;", true);

            // GNVUtils_mxJPO.writeTrace("GNVObjectHistory", "getFormDetails", "sFieldSuite = " + sFieldSuite);

            sFieldExpression = sFieldExpression.replaceAll(",,", ",_,");
            if (sFieldExpression.endsWith(","))
                sFieldExpression += "_";

            String[] aFieldNames = sFieldName.split(",");
            String[] aFieldExpressions = sFieldExpression.split(",");
            String[] aFieldSuites = sFieldSuite.split(",");
            aAttributes = new String[aFieldNames.length];
            aLabels = new String[aFieldNames.length];

            for (int j = 0; j < aAttributes.length; j++) {
                aAttributes[j] = "";
            }
            for (int j = 0; j < aLabels.length; j++) {
                aLabels[j] = "";
            }

            for (int j = 0; j < aFieldNames.length; j++) {

                String sFieldType = MqlUtil.mqlCommand(context, true, "print form '" + sForm + "' select field[" + aFieldNames[j] + "].expressiontype dump;", true);

                if (sFieldType.equals("businessobject")) {

                    String sExpression = aFieldExpressions[j];
                    String sFieldLabel = MqlUtil.mqlCommand(context, true, "print form '" + sForm + "' select field[" + aFieldNames[j] + "].label dump;", true);

                    if ("TRUE".equals(aFieldSuites[j])) {
                        String sSuite = MqlUtil.mqlCommand(context, true, "print form '" + sForm + "' select field[" + aFieldNames[j] + "].setting[Registered Suite].value dump;", true);
                        aLabels[j] = EnoviaResourceBundle.getProperty(context, sSuite, sFieldLabel, sLanguage);
                    } else {
                        aLabels[j] = sFieldLabel;
                    }

                    if (sExpression.startsWith("$<"))
                        sExpression = sExpression.substring(2);

                    if (sExpression.startsWith("name")) {
                        aAttributes[j] = "name";
                    } else if (sExpression.startsWith("type")) {
                        aAttributes[j] = "type";
                    } else if (sExpression.startsWith("revision")) {
                        aAttributes[j] = "revision";
                    } else if (sExpression.startsWith("description")) {
                        aAttributes[j] = "description";
                    } else if (sExpression.startsWith("current")) {
                        aAttributes[j] = "current";
                    } else if (sExpression.startsWith("policy")) {
                        aAttributes[j] = "policy";
                    } else if (sExpression.startsWith("vault")) {
                        aAttributes[j] = "vault";
                    } else if (sExpression.startsWith("owner")) {
                        aAttributes[j] = "owner";
                    }

                    else if (sExpression.startsWith("attribute[")) {
                        sExpression = sExpression.substring(10, sExpression.indexOf("]"));
                        if (sExpression.startsWith("attribute_")) {
                            sExpression = PropertyUtil.getSchemaProperty(context, sExpression);
                        }
                        aAttributes[j] = sExpression;
                    }

                }
            }
        }
    }

    public void parseHistory(Context context, String sOID, StringBuilder[] sbData, String sLanguage, String[] aAttributes, String[] aLabels, String sMCSURL) throws MatrixException, Exception {

        // GNVUtils_mxJPO.writeTrace("GNVObjectHistory", "parseHistory", "START");

        diff_match_patch diff = new diff_match_patch();
        Locale locale = new Locale(sLanguage.substring(0, 2));
        DateFormat ldf = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, 2, locale);
        int iCounter = 0;
        java.util.List<String> lItems = new ArrayList<String>();
        HashMap hmHistory = UINavigatorUtil.getHistoryData(context, sOID);
        DomainObject dObject = new DomainObject(sOID);
        String sPolicy = dObject.getInfo(context, "policy");
        String sCurrent = dObject.getInfo(context, "current");
        Vector vTime = (Vector) hmHistory.get("time");
        Vector vUser = (Vector) hmHistory.get("user");
        Vector vAction = (Vector) hmHistory.get("action");
        Vector vState = (Vector) hmHistory.get("state");
        Vector vDescription = (Vector) hmHistory.get("description");
        Calendar cRecent = Calendar.getInstance(TimeZone.getDefault());
        cRecent.add(java.util.GregorianCalendar.DAY_OF_YEAR, -30);

        StringList slEvents = new StringList("connect");
        HistoryList historyList = dObject.getFilteredHistory(context, slEvents, false, null, 0, 0, false, null, false, 200);
        HistoryItr historyItr = new HistoryItr(historyList);

        for (int i = 0; i < vTime.size(); i++) {

            String sFilter = "0";
            String sLabel = "";
            String sChange = "";
            String sTime = (String) vTime.get(i);
            String sUser = (String) vUser.get(i);
            String sAction = (String) vAction.get(i);
            String sState = (String) vState.get(i);
            String sDescription = (String) vDescription.get(i);
            Boolean bAdd = false;

            sTime = sTime.substring(6);
            sUser = sUser.substring(6);
            sState = sState.substring(7);

            Date date = (Date) sdfShort.parse(sTime);

            if (sAction.contains(("onnect"))) {

                sDescription = sDescription.trim();
                historyItr = new HistoryItr(historyList);

                while (historyItr.next()) {

                    History history = historyItr.obj();
                    String sEvent = history.getEvent();
                    NameValueList nvList = history.getInfo();
                    NameValueItr nvItr = new NameValueItr(nvList);
                    String sRType = "";
                    String sType = "";
                    String sName = "";
                    String sRev = "";
                    String sDir = "";

                    while (nvItr.next()) {

                        NameValue nv = nvItr.obj();
                        String name = nv.getName();
                        String value = nv.getValue();

                        if (name.equals("relationship"))
                            sRType = value;
                        else if (name.equals("direction"))
                            sDir = value;
                        else if (name.equals("type"))
                            sType = value;
                        else if (name.equals("name"))
                            sName = value;
                        else if (name.equals("revision"))
                            sRev = value;

                    }

                    if (lExcludedRelationships.indexOf(sRType) == -1) {

                        String sKey = sRType + " " + sDir + " " + sType + " " + sName + " " + sRev;
                        sKey = sKey.trim();

                        if (sKey.equals(sDescription)) {

                            // Build output string to describe related item
                            StringBuilder sbObject = new StringBuilder();
                            BusinessObject bObject = new BusinessObject(sType, sName, sRev, "eService Production");
                            StringBuilder sbLink = new StringBuilder();
                            Boolean bObjectExists = bObject.exists(context);

                            sLabel = EnoviaResourceBundle.getAdminI18NString(context, "Relationship", sRType, sLanguage);
                            if (sLabel.startsWith("emxFramework"))
                                sLabel = sRType;

                            if (bObjectExists) {

                                bObject.open(context);
                                sbLink.append("id=\"object").append(bObject.getObjectId()).append("\" ");
                                sbLink.append("onmousedown=\"openChangeObject(event, this.id);\" ");

                                bObject.close(context);

                                if (sType.equals("Person")) {

                                    DomainObject doUser = new DomainObject(bObject.getObjectId());
                                    String sClass = "changeObject changePersonAdd";
                                    String sFirstName = doUser.getInfo(context, "attribute[First Name]");
                                    String sLastName = doUser.getInfo(context, "attribute[Last Name]");
                                    sLastName = sLastName.toUpperCase();

                                    if (sAction.equals("disconnect"))
                                        sClass = "changeObject changePersonRemove";

                                    String sImage = GNVUtils_mxJPO.getPrimaryImageURL(context, null, doUser.getObjectId(context), "mxThumbnail Image", sMCSURL, "../common/images/noPicture.gif");
                                    sbObject.append("<img class=\"changePerson\" src=\"").append(sImage).append("\" />");
                                    sbObject.append("<span ").append(sbLink.toString()).append(" class=\"").append(sClass).append("\">");
                                    sbObject.append(sLastName).append(", ").append(sFirstName);
                                    sbObject.append("<br/>");
                                    sbObject.append(sName);
                                    sbObject.append("</span>");

                                } else if (sType.equals("Image Holder")) {

                                    DomainObject doImage = new DomainObject(bObject.getObjectId());
                                    String sClass = "changeObject changePersonAdd";
                                    String sPrimaryImage = doImage.getInfo(context, "attribute[Primary Image]");
                                    String sFileName = sPrimaryImage.substring(0, sPrimaryImage.lastIndexOf(".")) + ".jpg";
                                    String sTypeNLS = i18nNow.getTypeI18NString(sType, sLanguage);

                                    if (sAction.equals("disconnect"))
                                        sClass = "changeObject changePersonRemove";

                                    ArrayList bopArrayList = new ArrayList();
                                    BusinessObjectProxy bop = new BusinessObjectProxy(bObject.getObjectId(), "mxThumbnail Image", sFileName, false, false);
                                    bopArrayList.add(bop);
                                    String[] tmpImageUrls = ImageRequestData.getImageURLS(context, sMCSURL, bopArrayList);
                                    String sImage = tmpImageUrls[0];

                                    sbObject.append("<img class=\"changePerson\" src=\"").append(sImage).append("\" />");
                                    sbObject.append("<span ").append(sbLink.toString()).append(" class=\"").append(sClass).append("\">");
                                    sbObject.append(sTypeNLS);
                                    sbObject.append("<br/>");
                                    sbObject.append(sPrimaryImage);
                                    sbObject.append("</span>");

                                } else {

                                    sbLink.append("class=\"changeObject\" ");

                                }

                            }

                            if (sbObject.length() == 0) {

                                if (sAction.equals("connect")) {
                                    sbObject.append("<ins ").append(sbLink.toString()).append(" >");
                                } else {
                                    sbObject.append("<del ").append(sbLink.toString()).append(" >");
                                }

                                String sTypeLocal = EnoviaResourceBundle.getAdminI18NString(context, "Type", sType, sLanguage);
                                String sIcon = UINavigatorUtil.getTypeIconProperty(context, sType);
                                if (sIcon.equals(""))
                                    sIcon = "iconSmallDefault.gif";

                                sbObject.append("<img class=\"changeTypeIcon\" src=\"../common/images/").append(sIcon).append("\" />");
                                sbObject.append(sTypeLocal);
                                sbObject.append("<br/>");
                                sbObject.append(sName).append(" (").append(sRev).append(")");

                                if (sEvent.equals("connect")) {
                                    sbObject.append("</ins>");
                                } else {
                                    sbObject.append("</del>");
                                }

                            }

                            bAdd = true;
                            sChange = sbObject.toString();

                            break;
                        }
                    }
                }

            } else if (sAction.equals(("modify"))) {

                if (!sDescription.contains("#DENIED!  was:")) {
                    if (!sDescription.contains("was: #DENIED!")) {

                        String sAttribute = sDescription.substring(0, sDescription.indexOf(":"));
                        String sWas = "";
                        String sValue = "";
                        int iWas = sDescription.lastIndexOf(" was: ");

                        if (iWas != -1) {
                            sValue = sDescription.substring(sDescription.indexOf(":") + 2, iWas);
                            sWas = sDescription.substring(iWas + 6);
                        } else {
                            sValue = sDescription.substring(sDescription.indexOf(":") + 2);
                        }

                        sAttribute = sAttribute.trim();
                        sValue = sValue.trim();

                        if (lExcludedAttributes.indexOf(sAttribute) == -1) {

                            bAdd = true;
                            if (!sAttribute.equals("description")) {
                                AttributeType aType = new AttributeType(sAttribute);
                                aType.open(context);
                                if (!aType.isHidden()) {
                                    StringList slChoices = aType.getChoices(context);
                                    if (null != slChoices) {
                                        if (!sWas.equals(""))
                                            sWas = EnoviaResourceBundle.getRangeI18NString(context, sAttribute, sWas, sLanguage);
                                        if (!sValue.equals(""))
                                            sValue = EnoviaResourceBundle.getRangeI18NString(context, sAttribute, sValue, sLanguage);
                                    }
                                } else {
                                    bAdd = false;
                                }
                                aType.close(context);
                            }

                            LinkedList<Diff> list = diff.diff_main(sWas, sValue);
                            diff.diff_cleanupSemantic(list);

                            sLabel = sAttribute;
                            for (int j = 0; j < aAttributes.length; j++) {
                                if (aAttributes[j].equalsIgnoreCase(sAttribute)) {
                                    sLabel = aLabels[j];
                                }
                            }

                            if (sLabel.equals("description"))
                                sLabel = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Basic.Description", sLanguage);

                            sChange = diff.diff_prettyHtml(list);

                        }

                    }
                }

            } else if (sAction.equals(("change name"))) {

                String sAttribute = "Name";
                int iWas = sDescription.indexOf(" was: ");
                int iRev = sDescription.lastIndexOf(" revision:");
                String sValue = sDescription.substring(7, iWas);
                String sWas = sDescription.substring(iWas + 6, iRev);

                sAttribute = sAttribute.trim();
                sValue = sValue.trim();
                sWas = sWas.trim();

                if (!sWas.equals(sValue)) { // in case of revision change

                    bAdd = true;
                    LinkedList<Diff> list = diff.diff_main(sWas, sValue);
                    diff.diff_cleanupSemantic(list);

                    sLabel = sAttribute;
                    for (int j = 0; j < aAttributes.length; j++) {
                        if (aAttributes[j].equalsIgnoreCase(sAttribute)) {
                            sLabel = aLabels[j];
                        }
                    }

                    sChange = diff.diff_prettyHtml(list);

                }

            } else if (sAction.contains(("mote"))) {

                bAdd = true;
                sLabel = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Basic.Current", sLanguage);
                sChange = EnoviaResourceBundle.getStateI18NString(context, sPolicy, sState, sLanguage);
                if (sChange.startsWith("emxFramework."))
                    sChange = sState;

            } else if (sAction.contains(("change policy"))) {

                bAdd = true;
                sLabel = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Basic.Policy", sLanguage);
                int iWas = sDescription.indexOf(" was: ");
                String sValue = sDescription.substring(10, iWas);
                String sWas = sDescription.substring(iWas + 5);
                sValue = sValue.trim();
                sWas = sWas.trim();

                sValue = i18nNow.getAdminI18NString("policy", sValue, sLanguage);
                sWas = i18nNow.getAdminI18NString("policy", sWas, sLanguage);

                LinkedList<Diff> list = diff.diff_main(sWas, sValue);
                diff.diff_cleanupSemantic(list);
                sChange = diff.diff_prettyHtml(list);

            } else if (sAction.contains(("change type"))) {

                bAdd = true;
                sLabel = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Basic.Type", sLanguage);
                int iWas = sDescription.indexOf(" was: ");
                String sValue = sDescription.substring(8, iWas);
                String sWas = sDescription.substring(iWas + 5);
                sValue = sValue.trim();
                sWas = sWas.trim();

                sValue = i18nNow.getTypeI18NString(sValue, sLanguage);
                sWas = i18nNow.getTypeI18NString(sWas, sLanguage);

                LinkedList<Diff> list = diff.diff_main(sWas, sValue);
                diff.diff_cleanupSemantic(list);
                sChange = diff.diff_prettyHtml(list);

            } else if (sAction.contains(" ownership")) {

                if (sDescription.contains("_PRJ")) {

                    sLabel = EnoviaResourceBundle.getProperty(context, "Framework", "emxFramework.Common.DomainAccessTreeCategory", sLanguage);
                    String sValue = sDescription.substring(sDescription.indexOf("|") + 1, sDescription.indexOf("|Multiple"));
                    StringBuilder sbObject = new StringBuilder();

                    if (sValue.endsWith("_PRJ")) {

                        sValue = sValue.substring(0, sValue.length() - 4);
                        BusinessObject bObject = new BusinessObject("Person", sValue, "-", "eService Production");
                        StringBuilder sbLink = new StringBuilder();

                        if (bObject.exists(context)) {

                            bObject.open(context);
                            sbLink.append("id=\"object").append(bObject.getObjectId()).append("\" ");
                            sbLink.append("onclick=\"openChangeObject(this.id);\" ");

                            DomainObject doUser = new DomainObject(bObject.getObjectId());

                            bObject.close(context);

                            bAdd = true;
                            String sClass = "changePersonAdd";
                            String sFirstName = doUser.getInfo(context, "attribute[First Name]");
                            String sLastName = doUser.getInfo(context, "attribute[Last Name]");
                            sLastName = sLastName.toUpperCase();

                            if (sAction.equals("remove ownership"))
                                sClass = "changePersonRemove";

                            String sImage = GNVUtils_mxJPO.getPrimaryImageURL(context, null, doUser.getObjectId(context), "mxThumbnail Image", sMCSURL, "../common/images/noPicture.gif");
                            sbObject.append("<img class=\"changePerson\" src=\"").append(sImage).append("\" />");
                            sbObject.append("<span ").append(sbLink.toString()).append(" class=\"").append(sClass).append("\">");
                            sbObject.append(sLastName).append(", ").append(sFirstName);
                            sbObject.append("<br/>");
                            sbObject.append(sValue);
                            sbObject.append("</span>");

                            sChange = sbObject.toString();

                        }

                    }
                }

            }

            if (bAdd) {

                // Determine filter values
                if (date.getTime() >= cRecent.getTimeInMillis())
                    sFilter += "1";
                if (sCurrent.equals(sState))
                    sFilter += "2";
                if (lItems.indexOf(sLabel) == -1) {
                    sFilter += "3";
                    lItems.add(sLabel);
                }
                if (iCounter < 30)
                    sFilter += "4";

                String sStateNLS = EnoviaResourceBundle.getStateI18NString(context, sPolicy, sState, sLanguage);

                if (sStateNLS.startsWith("emxFramework."))
                    sStateNLS = sState;

                sChange = sChange.replaceAll("style=\"background:#ffe6e6;\"", "");
                sChange = sChange.replaceAll("style=\"background:#e6ffe6;\"", "");

                sbData[0].append("'").append(iCounter++).append("',");
                sbData[1].append("'").append(sLabel).append("',");
                sbData[2].append("'").append(ldf.format(date)).append("',");
                sbData[3].append("'").append(sStateNLS).append("',");
                sbData[4].append("'").append(PersonUtil.getFullName(context, sUser.trim())).append("',");
                sbData[5].append("'").append(sChange).append("',");
                sbData[6].append("'").append(date.getTime()).append("',");
                sbData[7].append("'").append(sFilter).append("',");

            }

        }

    }

    public String getObjectHistoryTitle(Context context, String sOID, String sShowName) throws Exception {

        StringBuilder sbResult = new StringBuilder();

        if (sShowName.equalsIgnoreCase("TRUE")) {
            DomainObject dObject = new DomainObject(sOID);
            String sName = dObject.getInfo(context, "name");
            sbResult.append("<b>");
            sbResult.append(sName);
            sbResult.append("</b> ");
        }
        sbResult.append("Object History");

        return sbResult.toString();

    }

}