package fpdm.gui;
/**
 * copyright FAURECIA - All Rights Reserved. ProgramName : FPDMOrganizationName
 * <H1>Description</H1> This class allow to get the Organization name
 * @author
 * @param 1:
 * @since FPDM 1.0.0
 * @version 1
 */

import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.util.DebugUtil;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MqlUtil;

import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.MatrixException;

public class SystemInfo_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.gui.SystemInfo");

    private static Map<String, String> mSystemInfos = null;

    private static final String SYSTEM_PROGRAM_NAME = "FPDMSystemInformations.tcl";

    private static final String PROPERTY_ORGANIZATION_NAME = "faureciaOrganizationName";

    private static final String PROPERTY_ENVIRONMENT_NAME = "faureciaEnvironment";

    private static final String PROPERTY_HF_LEVEL_NAME = "faureciaHFLevel";

    private static final String PROPERTY_SP_LEVEL_NAME = "faureciaSPLevel";

    private static final String PROPERTY_RELEASE_NAME = "faureciaRelease";

    private static String sOrganizationName;

    private static String sEnvironment;

    private static String sHFLevel;

    private static String sSPLevel;

    private static String sRelease;

    public static synchronized Map<String, String> getInstance(Context context) throws FrameworkException {
        return (mSystemInfos != null ? mSystemInfos : (mSystemInfos = getProgramPropertiesValues(context, SYSTEM_PROGRAM_NAME)));
    }

    public static synchronized Map<String, String> resetInstance(Context context) throws FrameworkException {
        mSystemInfos = getProgramPropertiesValues(context, SYSTEM_PROGRAM_NAME);
        return mSystemInfos;
    }

    /* Constructor */
    public SystemInfo_mxJPO(Context context, String[] args) throws Exception {
        DebugUtil.setDebug(false);

        Map<String, String> mSystemInfos = getInstance(context);

        if (sOrganizationName == null) {
            sOrganizationName = mSystemInfos.get(PROPERTY_ORGANIZATION_NAME);
        }
        if (sEnvironment == null) {
            sEnvironment = mSystemInfos.get(PROPERTY_ENVIRONMENT_NAME);
        }
        if (sHFLevel == null) {
            sHFLevel = mSystemInfos.get(PROPERTY_HF_LEVEL_NAME);
        }
        if (sSPLevel == null) {
            sSPLevel = mSystemInfos.get(PROPERTY_SP_LEVEL_NAME);
        }
        if (sRelease == null) {
            sRelease = mSystemInfos.get(PROPERTY_RELEASE_NAME);
        }
    }

    /**
     * Check property and return the organization name.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return String : The organization name
     * @throws Exception
     */
    public static String getName(Context context) throws FrameworkException {
        if (sOrganizationName == null) {
            sOrganizationName = getProgramPropertyValue(context, PROPERTY_ORGANIZATION_NAME);
        }
        return sOrganizationName;
    }

    /**
     * Check property on program eServiceSystemInformation.tcl and return the value for Environment (INT, QUA...)
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return String
     * @throws Exception
     */
    public static String getEnvironment(Context context) throws FrameworkException {
        if (sEnvironment == null) {
            sEnvironment = getProgramPropertyValue(context, PROPERTY_ENVIRONMENT_NAME);
        }
        return sEnvironment;
    }

    /**
     * Check property on program eServiceSystemInformation.tcl and return the value for Hot Fix Level (HF1, HF2 ...)
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return String
     * @throws Exception
     */
    public static String getHFLevel(Context context) throws FrameworkException {
        if (sHFLevel == null) {
            sHFLevel = getProgramPropertyValue(context, PROPERTY_HF_LEVEL_NAME);
        }
        return sHFLevel;
    }

    /**
     * Check property on program eServiceSystemInformation.tcl and return the value for Script Pack Level (SP1, SP2 ...)
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return String: Return the service Pack level
     * @throws Exception
     */
    public static String getSPLevel(Context context) throws FrameworkException {
        if (sSPLevel == null) {
            sSPLevel = getProgramPropertyValue(context, PROPERTY_SP_LEVEL_NAME);
        }
        return sSPLevel;
    }

    /**
     * Check property on program eServiceSystemInformation.tcl and return the value for Release (2.6.1, 2.7.0 ...)
     * @param context
     *            the eMatrix <code>Context</code> object
     * @return String
     * @throws Exception
     */
    public static String getRelease(Context context) throws FrameworkException {
        if (sRelease == null) {
            sRelease = getProgramPropertyValue(context, PROPERTY_RELEASE_NAME);
        }
        return sRelease;
    }

    /**
     * Call getName without arguments except the context
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Not used
     * @return String : The organization name
     * @throws Exception
     */
    public String getName(Context context, String[] args) throws FrameworkException {
        return getName(context);
    }

    /**
     * Method used for getName with a JPO.invoke
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            : Not used
     * @return String: The organization name
     * @throws Exception
     */
    public String getRelease(Context context, String[] args) throws FrameworkException {
        return getRelease(context);
    }

    /**
     * Method used for getSPLevel with a JPO.invoke
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Not used
     * @return String: return the service pack level
     * @throws Exception
     */
    public String getSPLevel(Context context, String[] args) throws FrameworkException {
        return getSPLevel(context);
    }

    /**
     * Method used for call getHFLevel with a JPO.invoke
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Not used
     * @return String: return the HF level
     * @throws Exception
     */
    public String getHFLevel(Context context, String[] args) throws FrameworkException {
        return getHFLevel(context);
    }

    /**
     * Method used for call getEnvironment with a JPO.invoke
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Not used
     * @return String: The environment
     * @throws Exception
     */
    public String getEnvironment(Context context, String[] args) throws FrameworkException {
        return getEnvironment(context);
    }

    /**
     * Main
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Not used
     * @return
     * @throws Exception
     */
    public int mxMain(Context context, String[] args) throws FrameworkException {
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(new MatrixWriter(context));
            writer.println(getName(context));
        } catch (FrameworkException e) {
            logger.error("Error in mxMain()\n", e);
            throw e;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        return 0;
    }

    /**
     * Get property value from the Map
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sPropertyName
     *            property name
     * @return
     * @throws FrameworkException
     */
    private static String getProgramPropertyValue(Context context, String sPropertyName) throws FrameworkException {
        mSystemInfos = getInstance(context);

        String sPropertyValue = mSystemInfos.get(sPropertyName);

        return sPropertyValue;
    }

    /**
     * Get all properties defined on the program and initialize a static Map
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param sProgramName
     *            program name
     * @return
     * @throws FrameworkException
     */
    private static Map<String, String> getProgramPropertiesValues(Context context, String sProgramName) throws FrameworkException {
        Map<String, String> mProgramProperties = new HashMap<String, String>();

        try {
            logger.debug("getProgramPropertiesValues() - Initializing System Infos Map ...");
            String sPropertiesValues = MqlUtil.mqlCommand(context, "print program $1 select $2", SYSTEM_PROGRAM_NAME, "property.value");
            String[] saProperties = sPropertiesValues.split("\\n");

            Pattern pPatternProperty = Pattern.compile("property\\[([^ ]*)\\].value = ([^| ]*)");
            Matcher mProp = null;
            String sPropertyName = null;
            String sPropertyValue = null;
            for (int i = 0; i < saProperties.length; i++) {
                mProp = pPatternProperty.matcher(saProperties[i]);
                if (mProp.find()) {
                    sPropertyName = mProp.group(1);
                    sPropertyValue = mProp.group(2);
                    mProgramProperties.put(sPropertyName, sPropertyValue);
                }
            }

        } catch (Exception e) {
            logger.error("Error in getProgramPropertiesValues()\n", e);
            throw e;
        }

        return mProgramProperties;

    }

    /**
     * Get all properties defined on the program
     * @plm.usage JSP: emxNavigator.jsp and FPDM_SystemInformationsProperties.jsp
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            contains HAproxy node name
     * @return
     * @throws FrameworkException
     * @throws MatrixException
     */
    public Map<String, String> getSystemInformations(Context context, String[] args) throws FrameworkException, MatrixException {

        Map<String, String> mSystemInfos = getInstance(context);

        if (!mSystemInfos.containsKey("hostname")) {
            String sHaproxyNodeName = args[0];
            logger.debug("getSystemInformations() - sHaproxyNodeName = <" + sHaproxyNodeName + ">");

            // extract HOSTNAME and NODE from from string
            getHostname(sHaproxyNodeName);
        }
        logger.debug("getSystemInformations() - mSystemInfos = <" + mSystemInfos + ">");

        return mSystemInfos;
    }

    /**
     * Get environment and node names from HAproxy node name
     * @param sHaproxyNodeName
     *            HAproxy node name
     */
    protected void getHostname(String sHaproxyNodeName) {
        HashMap<String, String> envMapping = new HashMap<>();
        envMapping.put("prod", "prd");
        envMapping.put("test", "tst");

        String sHostName = "";
        String sNodeName = "";
        if (sHaproxyNodeName != null) {
            String[] aNodename = sHaproxyNodeName.split("-", -1);
            if (aNodename.length >= 5) {
                // [as, dev, 3dspace, 001, node01]
                // String bg = aNodename[0];
                String env = envMapping.get(aNodename[1]) != null ? envMapping.get(aNodename[1]) : aNodename[1];
                String m = aNodename[2];
                String vm = aNodename[3];
                sNodeName = aNodename[4].toUpperCase(Locale.ENGLISH);
                sHostName = MessageFormat.format("{0}-tiger-{1}-{2}.app.corp", env, m, vm);
            }
        }

        mSystemInfos.put("hostname", sHostName);
        mSystemInfos.put("node", sNodeName);
    }

    /**
     * Get all properties defined on the program
     * @plm.usage JSP: FPDM_SystemInformationsPropertiesReset.jsp
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            Not used
     * @throws FrameworkException
     * @throws MatrixException
     */
    public void resetSystemInformations(Context context, String[] args) throws FrameworkException, MatrixException {
        logger.debug("resetSystemInformations() - start reset ...");
        Map<String, String> mSystemInfos = resetInstance(context);
        logger.debug("resetSystemInformations() - mSystemInfos = <" + mSystemInfos + ">");
    }

}
