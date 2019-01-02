
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.matrixone.apps.common.CommonDocument;
import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.DomainObject;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkUtil;
import com.matrixone.apps.domain.util.MapList;
import com.matrixone.apps.domain.util.PropertyUtil;
import com.matrixone.apps.domain.util.eMatrixDateFormat;
import com.matrixone.apps.framework.ui.UIUtil;

import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.StringList;
import pss.constants.TigerConstants;

public class PSS_emxDashboardDocuments_mxJPO extends emxDashboardDocumentsBase_mxJPO {

    public PSS_emxDashboardDocuments_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
    }

    @com.matrixone.apps.framework.ui.ProgramCallable
    public MapList getDocuments(Context context, String[] args) throws Exception {
        MapList mlResults = new MapList();
        Map programMap = (Map) JPO.unpackArgs(args);
        String sMode = (String) programMap.get("mode");

        if (null == sMode) {
            sMode = "";
        }

        StringBuilder sbWhere = new StringBuilder();
        sbWhere.append("((policy != \'");
        sbWhere.append(TigerConstants.POLICY_VERSION);
        sbWhere.append("\')");
        sbWhere.append(" && (policy != \'");
        sbWhere.append(TigerConstants.POLICY_VERSIONEDDESIGNPOLICY);
        sbWhere.append("\')");
        sbWhere.append(" && (policy != \'");
        sbWhere.append(TigerConstants.POLICY_VERSIONEDDESIGNPOLICY);
        sbWhere.append("\')");
        sbWhere.append(" && (policy != \'");
        sbWhere.append(TigerConstants.POLICY_DERIVEDOUTPUTTEAMPOLICY);
        sbWhere.append("\'))");
        sbWhere.append(" && (revision == last)");

        StringList busSelects = new StringList();
        busSelects.add(DomainConstants.SELECT_ID);
        String strPolicy = DomainConstants.SELECT_POLICY;
        String strModified = DomainConstants.SELECT_MODIFIED;
        String strOriginated = DomainConstants.SELECT_ORIGINATED;
        String strType = PropertyUtil.getSchemaProperty(context, DomainObject.SYMBOLIC_type_DOCUMENTS);
        String strExpandLimit = null;
        try {
            strExpandLimit = EnoviaResourceBundle.getProperty(context, "emxFramework.FreezePane.DashboardDocs.QueryLimit");
            if (strExpandLimit == null || strExpandLimit.length() == 0) {
                strExpandLimit = "100";
            }
        } catch (Exception ex) {
            strExpandLimit = "100";
        }
        Short expandLimit = Short.valueOf(strExpandLimit);
        boolean showLimitMessage = false;
        if (sMode.equals("New")) {

            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            cal.add(java.util.GregorianCalendar.DAY_OF_YEAR, -10);

            busSelects.add(strPolicy);
            busSelects.add(strOriginated);

            busSelects.add(DomainConstants.SELECT_TYPE);
            busSelects.add(DomainConstants.SELECT_ID);

            busSelects.add(CommonDocument.SELECT_HAS_ROUTE);
            busSelects.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
            busSelects.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
            busSelects.add(CommonDocument.SELECT_FILE_NAME);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
            busSelects.add(CommonDocument.SELECT_LOCKED);
            busSelects.add(CommonDocument.SELECT_TYPE_OF_IC_DOCUMENT);
            busSelects.add(CommonDocument.SELECT_FILE_FORMAT);
            busSelects.add(CommonDocument.SELECT_REVISION);
            busSelects.add(CommonDocument.SELECT_TYPE_OF_IC_DOCUMENT);
            busSelects.add(CommonDocument.SELECT_TYPE);
            busSelects.add(CommonDocument.SELECT_ID);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION);

            busSelects.add(CommonDocument.SELECT_SUSPEND_VERSIONING);
            busSelects.add(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
            busSelects.add(CommonDocument.SELECT_HAS_CHECKIN_ACCESS);
            busSelects.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
            busSelects.add(CommonDocument.SELECT_FILE_NAME);
            busSelects.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
            busSelects.add(CommonDocument.SELECT_IS_KIND_OF_VC_DOCUMENT);
            busSelects.add("vcfile");
            busSelects.add("vcmodule");
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKER);
            busSelects.add(CommonDocument.SELECT_HAS_TOCONNECT_ACCESS);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
            busSelects.add(CommonDocument.SELECT_OWNER);
            busSelects.add(CommonDocument.SELECT_LOCKED);
            busSelects.add(CommonDocument.SELECT_LOCKER);

            boolean bActivateDSFA = FrameworkUtil.isSuiteRegistered(context, "ActivateDSFA", false, null, null);
            if (bActivateDSFA) {
                busSelects.add(CommonDocument.SELECT_IS_KIND_OF_VC_DOCUMENT);
                busSelects.add(CommonDocument.SELECT_VCFILE_EXISTS);
                busSelects.add(CommonDocument.SELECT_VCFOLDER_EXISTS);
                busSelects.add(CommonDocument.SELECT_VCMODULE_EXISTS);
            }

            sbWhere.append(" && (originated > '");
            sbWhere.append(cal.get(Calendar.MONTH) + 1).append("/").append(cal.get(Calendar.DAY_OF_MONTH)).append("/").append(cal.get(Calendar.YEAR)).append("')");

            StringList orderBys = new StringList();
            orderBys.add("-originated");
            mlResults = DomainObject.findObjects(context, strType, TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), busSelects, expandLimit.shortValue(), orderBys);
            if (mlResults.size() >= Integer.parseInt(strExpandLimit)) {
                showLimitMessage = true;
            }
        } else if (sMode.equals("Changed")) {

            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            cal.add(java.util.GregorianCalendar.DAY_OF_YEAR, -10);

            busSelects.add(strPolicy);
            busSelects.add(strModified);

            busSelects.add(DomainConstants.SELECT_TYPE);
            busSelects.add(DomainConstants.SELECT_ID);

            busSelects.add(CommonDocument.SELECT_HAS_ROUTE);
            busSelects.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
            busSelects.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
            busSelects.add(CommonDocument.SELECT_FILE_NAME);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
            busSelects.add(CommonDocument.SELECT_LOCKED);
            busSelects.add(CommonDocument.SELECT_TYPE_OF_IC_DOCUMENT);
            busSelects.add(CommonDocument.SELECT_FILE_FORMAT);
            busSelects.add(CommonDocument.SELECT_REVISION);
            busSelects.add(CommonDocument.SELECT_TYPE_OF_IC_DOCUMENT);
            busSelects.add(CommonDocument.SELECT_TYPE);
            busSelects.add(CommonDocument.SELECT_ID);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION);

            busSelects.add(CommonDocument.SELECT_SUSPEND_VERSIONING);
            busSelects.add(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
            busSelects.add(CommonDocument.SELECT_HAS_CHECKIN_ACCESS);
            busSelects.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
            busSelects.add(CommonDocument.SELECT_FILE_NAME);
            busSelects.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
            busSelects.add(CommonDocument.SELECT_IS_KIND_OF_VC_DOCUMENT);
            busSelects.add("vcfile");
            busSelects.add("vcmodule");
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKER);
            busSelects.add(CommonDocument.SELECT_HAS_TOCONNECT_ACCESS);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
            busSelects.add(CommonDocument.SELECT_OWNER);
            busSelects.add(CommonDocument.SELECT_LOCKED);
            busSelects.add(CommonDocument.SELECT_LOCKER);

            boolean bActivateDSFA = FrameworkUtil.isSuiteRegistered(context, "ActivateDSFA", false, null, null);
            if (bActivateDSFA) {
                busSelects.add(CommonDocument.SELECT_IS_KIND_OF_VC_DOCUMENT);
                busSelects.add(CommonDocument.SELECT_VCFILE_EXISTS);
                busSelects.add(CommonDocument.SELECT_VCFOLDER_EXISTS);
                busSelects.add(CommonDocument.SELECT_VCMODULE_EXISTS);
            }

            sbWhere.append(" && (" + strModified + " > '");
            sbWhere.append(cal.get(Calendar.MONTH) + 1).append("/").append(cal.get(Calendar.DAY_OF_MONTH)).append("/").append(cal.get(Calendar.YEAR)).append("')");

            StringList orderBys = new StringList();
            orderBys.add("-modified");
            mlResults = DomainObject.findObjects(context, strType, TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), busSelects, expandLimit.shortValue(), orderBys);
            if (mlResults.size() >= Integer.parseInt(strExpandLimit)) {
                showLimitMessage = true;
            }
        } else if (sMode.equals("Recent")) {

            Calendar cal = Calendar.getInstance(TimeZone.getDefault());
            cal.add(java.util.GregorianCalendar.DAY_OF_YEAR, -10);

            busSelects.add(strPolicy);
            busSelects.add(strModified);
            busSelects.add("attribute[" + DomainConstants.ATTRIBUTE_ORIGINATOR + "]");

            busSelects.add(DomainConstants.SELECT_TYPE);
            busSelects.add(DomainConstants.SELECT_ID);

            busSelects.add(CommonDocument.SELECT_HAS_ROUTE);
            busSelects.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
            busSelects.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
            busSelects.add(CommonDocument.SELECT_FILE_NAME);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
            busSelects.add(CommonDocument.SELECT_LOCKED);
            busSelects.add(CommonDocument.SELECT_TYPE_OF_IC_DOCUMENT);
            busSelects.add(CommonDocument.SELECT_FILE_FORMAT);
            busSelects.add(CommonDocument.SELECT_REVISION);
            busSelects.add(CommonDocument.SELECT_TYPE_OF_IC_DOCUMENT);
            busSelects.add(CommonDocument.SELECT_TYPE);
            busSelects.add(CommonDocument.SELECT_ID);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION);

            busSelects.add(CommonDocument.SELECT_SUSPEND_VERSIONING);
            busSelects.add(CommonDocument.SELECT_HAS_CHECKOUT_ACCESS);
            busSelects.add(CommonDocument.SELECT_HAS_CHECKIN_ACCESS);
            busSelects.add(CommonDocument.SELECT_FILE_NAMES_OF_ACTIVE_VERSION);
            busSelects.add(CommonDocument.SELECT_FILE_NAME);
            busSelects.add(CommonDocument.SELECT_MOVE_FILES_TO_VERSION);
            busSelects.add(CommonDocument.SELECT_IS_KIND_OF_VC_DOCUMENT);
            busSelects.add("vcfile");
            busSelects.add("vcmodule");
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKED);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_LOCKER);
            busSelects.add(CommonDocument.SELECT_HAS_TOCONNECT_ACCESS);
            busSelects.add(CommonDocument.SELECT_ACTIVE_FILE_VERSION_ID);
            busSelects.add(CommonDocument.SELECT_OWNER);
            busSelects.add(CommonDocument.SELECT_LOCKED);
            busSelects.add(CommonDocument.SELECT_LOCKER);

            boolean bActivateDSFA = FrameworkUtil.isSuiteRegistered(context, "ActivateDSFA", false, null, null);
            if (bActivateDSFA) {
                busSelects.add(CommonDocument.SELECT_IS_KIND_OF_VC_DOCUMENT);
                busSelects.add(CommonDocument.SELECT_VCFILE_EXISTS);
                busSelects.add(CommonDocument.SELECT_VCFOLDER_EXISTS);
                busSelects.add(CommonDocument.SELECT_VCMODULE_EXISTS);
            }

            sbWhere.append(" && " + CommonDocument.SELECT_OWNER + " == '");
            sbWhere.append(context.getUser());
            sbWhere.append("'");
            sbWhere.append(" && (" + strModified + " > '");
            sbWhere.append(cal.get(Calendar.MONTH) + 1).append("/").append(cal.get(Calendar.DAY_OF_MONTH)).append("/").append(cal.get(Calendar.YEAR)).append("')");

            StringList orderBys = new StringList();
            orderBys.add("-modified");
            mlResults = DomainObject.findObjects(context, strType, TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), busSelects, expandLimit.shortValue(), orderBys);
            if (mlResults.size() >= Integer.parseInt(strExpandLimit)) {
                showLimitMessage = true;
            }
        } else if (sMode.equals("Locked")) {
            String relActiveVersion = PropertyUtil.getSchemaProperty(context, DomainObject.SYMBOLIC_relationship_ActiveVersion);
            busSelects.add("to[" + relActiveVersion + "].from.id");
            busSelects.add("from[" + relActiveVersion + "].to.id");
            busSelects.add("from[Publish Subscribe].to.id");
            busSelects.add(DomainConstants.SELECT_TYPE);
            StringBuilder sblWhere = new StringBuilder();
            sblWhere.append("(revision == last)");
            sblWhere.append(" && locker == \"" + context.getUser() + "\"");
            MapList mlLockedItems = DomainObject.findObjects(context, strType, TigerConstants.VAULT_ESERVICEPRODUCTION, sblWhere.toString(), busSelects);

            ArrayList tempArray = new ArrayList();

            for (int i = 0; i < mlLockedItems.size(); i++) {
                Map mLockedItem = (Map) mlLockedItems.get(i);
                String sId = (String) mLockedItem.get("to[" + relActiveVersion + "].from.id");
                if (UIUtil.isNullOrEmpty(sId)) {
                    sId = (String) mLockedItem.get(DomainConstants.SELECT_ID);
                }
                if (!tempArray.contains(sId)) {
                    tempArray.add(sId);
                    Map mResult = new HashMap();
                    mResult.put("id", sId);
                    mResult.put(DomainConstants.SELECT_TYPE, (String) mLockedItem.get(DomainConstants.SELECT_TYPE));
                    mResult.put("from[Publish Subscribe].to.id", (String) mLockedItem.get("from[Publish Subscribe].to.id"));
                    mlResults.add(mResult);
                }
            }

        } else if (sMode.equals("MRU")) {

            busSelects.add(strModified);

            Calendar cal = Calendar.getInstance();
            cal.add(java.util.GregorianCalendar.DAY_OF_YEAR, -1);

            String sMinute = String.valueOf(cal.get(Calendar.MINUTE));
            String sSecond = String.valueOf(cal.get(Calendar.SECOND));
            String sAMPM = (cal.get(Calendar.AM_PM) == 0) ? "AM" : "PM";
            String sHour = String.valueOf(cal.get(Calendar.HOUR));
            if (sSecond.length() == 1) {
                sSecond = "0" + sSecond;
            }
            if (sMinute.length() == 1) {
                sMinute = "0" + sMinute;
            }

            String inputDateFormat = eMatrixDateFormat.getInputDateFormat();
            String displayDateFormat = eMatrixDateFormat.getEMatrixDateFormat();

            sbWhere.append(" && (" + strModified + " >= \"");

            if (inputDateFormat != null && inputDateFormat.startsWith("dd")) {
                sbWhere.append(cal.get(Calendar.DAY_OF_MONTH)).append("/").append(cal.get(Calendar.MONTH) + 1).append("/").append(cal.get(Calendar.YEAR));
            } else if (inputDateFormat != null && inputDateFormat.startsWith("yyyy")) {
                sbWhere.append(cal.get(Calendar.YEAR)).append("/").append(cal.get(Calendar.MONTH) + 1).append("/").append(cal.get(Calendar.DAY_OF_MONTH));
            } else {
                sbWhere.append(cal.get(Calendar.MONTH) + 1).append("/").append(cal.get(Calendar.DAY_OF_MONTH)).append("/").append(cal.get(Calendar.YEAR));
            }

            if (displayDateFormat.indexOf("HH") > 0) {
                int time = Integer.parseInt(sHour);
                if ("PM".equals(sAMPM)) {
                    time = 12 + Integer.parseInt(sHour);
                }
                sbWhere.append(" ").append(time).append(":").append(sMinute).append(":").append(sSecond);
            } else {
                sbWhere.append(" ").append(cal.get(Calendar.HOUR) + 1).append(":").append(sMinute).append(":").append(sSecond).append(" ").append(sAMPM);
            }
            sbWhere.append("\")");

            mlResults = DomainObject.findObjects(context, strType, TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), busSelects);

        } else if (sMode.equals("NewWeek")) {

            Calendar cNow = Calendar.getInstance();
            cNow.set(Calendar.DAY_OF_WEEK, cNow.getFirstDayOfWeek());
            busSelects.add("from[Publish Subscribe].to.id");
            busSelects.add(DomainConstants.SELECT_TYPE);

            sbWhere.append(" && (" + strOriginated + " >= '");
            sbWhere.append(cNow.get(Calendar.MONTH) + 1).append("/").append(cNow.get(Calendar.DAY_OF_MONTH)).append("/").append(cNow.get(Calendar.YEAR)).append("')");

            mlResults = DomainObject.findObjects(context, strType, TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), busSelects);

        } else if (sMode.equals("ModWeek")) {

            Calendar cNow = Calendar.getInstance();
            cNow.set(Calendar.DAY_OF_WEEK, cNow.getFirstDayOfWeek());
            busSelects.add("from[Publish Subscribe].to.id");
            busSelects.add(DomainConstants.SELECT_TYPE);
            busSelects.add(strOriginated);
            busSelects.add(strModified);

            sbWhere.append(" && (" + strModified + " >= '");
            sbWhere.append(cNow.get(Calendar.MONTH) + 1).append("/").append(cNow.get(Calendar.DAY_OF_MONTH)).append("/").append(cNow.get(Calendar.YEAR)).append("')");

            mlResults = DomainObject.findObjects(context, strType, TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), busSelects);

            for (int i = mlResults.size() - 1; i >= 0; i--) {

                Map mResult = (Map) mlResults.get(i);
                String sOriginated = (String) mResult.get(strOriginated);
                String sModified = (String) mResult.get(strModified);
                sModified = sModified.substring(0, sModified.indexOf(" "));
                sOriginated = sOriginated.substring(0, sOriginated.indexOf(" "));

                if (sOriginated.equals(sModified)) {
                    mlResults.remove(i);
                }

            }

        } else if (sMode.equals("NewMonth")) {

            Calendar cNow = Calendar.getInstance();
            busSelects.add("from[Publish Subscribe].to.id");
            busSelects.add(DomainConstants.SELECT_TYPE);
            sbWhere.append(" && (" + strOriginated + " >= '");
            sbWhere.append(cNow.get(Calendar.MONTH) + 1).append("/1/").append(cNow.get(Calendar.YEAR)).append("')");

            mlResults = DomainObject.findObjects(context, strType, TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), busSelects);

        } else if (sMode.equals("ModMonth")) {

            Calendar cNow = Calendar.getInstance();
            cNow.set(Calendar.DAY_OF_WEEK, cNow.getFirstDayOfWeek());
            busSelects.add("from[Publish Subscribe].to.id");
            busSelects.add(DomainConstants.SELECT_TYPE);
            busSelects.add(strOriginated);
            busSelects.add(strModified);

            sbWhere.append(" && (" + strModified + " >= '");
            sbWhere.append(cNow.get(Calendar.MONTH) + 1).append("/1/").append(cNow.get(Calendar.YEAR)).append("')");

            mlResults = DomainObject.findObjects(context, strType, TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), busSelects);

            for (int i = mlResults.size() - 1; i >= 0; i--) {

                Map mResult = (Map) mlResults.get(i);
                String sOriginated = (String) mResult.get(strOriginated);
                String sModified = (String) mResult.get(strModified);
                sModified = sModified.substring(0, sModified.indexOf(" "));
                sOriginated = sOriginated.substring(0, sOriginated.indexOf(" "));

                if (sOriginated.equals(sModified)) {
                    mlResults.remove(i);
                }

            }

        } else if (sMode.equals("By Date")) {

            String sDate = (String) programMap.get("date");
            String selectedChart = (String) programMap.get("selectedChart");
            Calendar cSelected = Calendar.getInstance(TimeZone.getDefault());
            Calendar cNext = Calendar.getInstance(TimeZone.getDefault());
            long lDate = Long.parseLong(sDate);

            cSelected.setTimeInMillis(lDate);
            cNext.setTimeInMillis(lDate);
            cNext.add(java.util.GregorianCalendar.DAY_OF_YEAR, 1);

            busSelects.add("first");
            busSelects.add("revision");
            busSelects.add("last");

            sbWhere = new StringBuilder();
            sbWhere.append("(" + strPolicy + " != 'Version')");
            // sbWhere.append(" && (revision == last)");

            if ("New Docs".equals(selectedChart)) {
                sbWhere.append(" && ( (" + strOriginated + " > '");
                sbWhere.append(cSelected.get(Calendar.MONTH) + 1).append("/").append(cSelected.get(Calendar.DAY_OF_MONTH)).append("/").append(cSelected.get(Calendar.YEAR)).append("') && ");
                sbWhere.append(" (" + strOriginated + " < '");
                sbWhere.append(cNext.get(Calendar.MONTH) + 1).append("/").append(cNext.get(Calendar.DAY_OF_MONTH)).append("/").append(cNext.get(Calendar.YEAR)).append("') )");
            } else if ("Changed Docs".equals(selectedChart)) {
                sbWhere.append(" && ( (" + strModified + " > '");
                sbWhere.append(cSelected.get(Calendar.MONTH) + 1).append("/").append(cSelected.get(Calendar.DAY_OF_MONTH)).append("/").append(cSelected.get(Calendar.YEAR)).append("') ");
                sbWhere.append(" && (" + strModified + " < '");
                sbWhere.append(cNext.get(Calendar.MONTH) + 1).append("/").append(cNext.get(Calendar.DAY_OF_MONTH)).append("/").append(cNext.get(Calendar.YEAR)).append("') )");
            } else {
                sbWhere.append(" && ( ( (" + strModified + " > '");
                sbWhere.append(cSelected.get(Calendar.MONTH) + 1).append("/").append(cSelected.get(Calendar.DAY_OF_MONTH)).append("/").append(cSelected.get(Calendar.YEAR)).append("') ");
                sbWhere.append(" && (" + strModified + " < '");
                sbWhere.append(cNext.get(Calendar.MONTH) + 1).append("/").append(cNext.get(Calendar.DAY_OF_MONTH)).append("/").append(cNext.get(Calendar.YEAR)).append("') ) || ");
                sbWhere.append(" ( (" + strOriginated + " > '");
                sbWhere.append(cSelected.get(Calendar.MONTH) + 1).append("/").append(cSelected.get(Calendar.DAY_OF_MONTH)).append("/").append(cSelected.get(Calendar.YEAR)).append("') && ");
                sbWhere.append(" (" + strOriginated + " < '");
                sbWhere.append(cNext.get(Calendar.MONTH) + 1).append("/").append(cNext.get(Calendar.DAY_OF_MONTH)).append("/").append(cNext.get(Calendar.YEAR)).append("') ))");
            }

            mlResults = DomainObject.findObjects(context, strType, TigerConstants.VAULT_ESERVICEPRODUCTION, sbWhere.toString(), busSelects);

        }
        if (showLimitMessage) {
            String strError = EnoviaResourceBundle.getProperty(context, "emxComponentsStringResource", context.getLocale(), "emxComponents.DashboardDocs.LimitReached");
            strError = FrameworkUtil.findAndReplace(strError, "{0}", String.valueOf(expandLimit));
            emxContextUtil_mxJPO.mqlNotice(context, strError);
        }
        return mlResults;

    }
}
