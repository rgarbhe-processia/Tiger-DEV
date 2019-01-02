/**
 * This class will be used to get properties used by the applets
 */
package fpdm.applet.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import faurecia.util.Local_OS_Information;

public class FPDMAppletUtils {
    public static Vector<String> parseStringList(String encodedList, char separator) {
        if(encodedList == null) {
            encodedList = "";
        }
        Vector<String> result = new Vector<String>();
        StringBuffer sb = new StringBuffer();
        int len = encodedList.length();
        for(int i=0; i<len; i++) {
            char c = encodedList.charAt(i);
            if(c == separator) {
                result.addElement(sb.toString());
                sb = new StringBuffer();
            }
            else {
                sb.append(c);
            }
        }
        result.addElement(sb.toString());
        return result;
    }

    public static Hashtable<String, String> parseMapList(String sMapList) {
        Hashtable<String, String> hResult = new Hashtable<String, String>();
        StringTokenizer strTokenPipe = new StringTokenizer(sMapList, "|");
        while (strTokenPipe.hasMoreTokens()) {
            String sAttr = strTokenPipe.nextToken();
            StringTokenizer strTokenEqual = new StringTokenizer(sAttr, "=");
            String sAttrName = strTokenEqual.nextToken();
            String sAttrValue;
            if (strTokenEqual.hasMoreTokens()) {
                sAttrValue = strTokenEqual.nextToken();
            } else {
                sAttrValue = "";
            }
            hResult.put(sAttrName, sAttrValue);
        }
        return hResult;
    }
// THA => To keep for Titleblock
    public static Hashtable buildMxInfoCheckout(String cadId, String cadDefId, String cadDefType, String cadDefName, String cadDefRevision, String listOfFiles) {
        Hashtable<String, Object> result = new Hashtable<String, Object>();
        Vector<String> cadids = parseStringList(cadId, '#');
        Vector<String> ids = parseStringList(cadDefId, '#');
        Vector<String> types = parseStringList(cadDefType, '#');
        Vector<String> names = parseStringList(cadDefName, '#');
        Vector<String> revisions = parseStringList(cadDefRevision, '#');
        Vector<String> listsOfFiles = parseStringList(listOfFiles, '#');
        int len = cadids.size();
        for(int i=0; i<len; i++) {
            Hashtable<String, String> data = new Hashtable<String, String>();
            data.put("id", ids.elementAt(i));
            data.put("type", types.elementAt(i));
            data.put("name", names.elementAt(i));
            data.put("revision", revisions.elementAt(i));
            data.put("listOfFiles", listsOfFiles.elementAt(i));
            result.put(cadids.elementAt(i), data);
        }
        System.out.println("mxInfo:"+result);
        return result;
    }
    
    private static Properties INCAT_DITS_CATIAV5_TB_INI = null;
    private static Properties MAGNX_INIFILEDIR = null;
    @SuppressWarnings("unchecked")
    public static synchronized void loadINCAT_DITS_CATIAV5_TB_INI() throws Exception {
        try {
            AccessController.doPrivileged( new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    // get path of INCAT_DITS_CATIAV5_TB_INIFILE_PATH file 
                    String sDir = getVariableEnvironmentValue("INCAT_DITS_CATIAV5_TB_INIFILE_PATH");
                    File ini = new File(sDir, "INCAT_DITS_CATIAV5_TB.ini");
                    BufferedReader rd = new BufferedReader(new FileReader(ini));
                    INCAT_DITS_CATIAV5_TB_INI = new Properties();
                    String line = rd.readLine();
                    while(line != null) {
                        line = line.trim();
                        if(!line.startsWith("#")) {
                            int index = line.indexOf("=");
                            if(index>=0) {
                                String key = line.substring(0, index).trim();
                                String value = line.substring(index+1).trim();
                                if(value.startsWith("\"") && value.endsWith("\"")) {
                                    value = value.substring(1, value.length()-1);
                                }
                                INCAT_DITS_CATIAV5_TB_INI.put(key, value);
                            }
                            else if(line.length()>0){
                                INCAT_DITS_CATIAV5_TB_INI.put(line, "");
                            }
                        }
                        line = rd.readLine();
                    }
                    return null;
                }
            });
        }
        catch(PrivilegedActionException pae) {
            throw pae.getException();
        }
    }
    
    @SuppressWarnings("unchecked")
    public static synchronized void loadMAGNX_INIFILEDIR() throws Exception {
        try {
            AccessController.doPrivileged( new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    // get path of ini file 
                    String sDir = getVariableEnvironmentValue("MAGNX_INIFILEDIR");
                    // get language of ini file (English, French or German)
                    String sLanguage = getVariableEnvironmentValue("UGII_LANG");
                    // set ini file name
                    String sInitFileName = "magnx_" + sLanguage + ".ini";
                    // We take the configuration for language setted in UGII_LANG variable in InitTool
                    File ini = new File(sDir, sInitFileName);
                    BufferedReader rd = new BufferedReader(new FileReader(ini));
                    MAGNX_INIFILEDIR = new Properties();
                    String line = rd.readLine();
                    while(line != null) {
                        line = line.trim();
                        if(!line.startsWith("#")) {
                            // line have the format : E/lexicalname[31]/entryname[31]/entryvalue[...]
                            // we take only the line with the lexicalname PATH
                            int index = line.indexOf("/");
                            if(index>=0 ) {
                                line = line.substring(index+1);
                                index = line.indexOf("/");
                                if(index>=0 ) {
                                    String lexicalName = line.substring(0, index).trim();
                                    line = line.substring(index+1);
                                    index = line.indexOf("/");
                                    if(index>=0 && "PATH".equals(lexicalName)) {
                                        String key = line.substring(0, index).trim();
                                        key = key.toUpperCase() ;
                                        if ( "INPUT".equals(key) || "OUTPUT".equals(key))
                                            key += "DIR" ;
            
                                        String value = line.substring(index+1).trim();
                                        if(value.startsWith("\"") && value.endsWith("\"")) {
                                            value = value.substring(1, value.length()-1);
                                        }
                                        //System.out.println("key" +key );
                                        //System.out.println("value" +value );
                                        MAGNX_INIFILEDIR.put(key, value);
                                    }
                                    else if(line.length()>0){
                                        MAGNX_INIFILEDIR.put(line, "");
                                    }
                                }
                            }
                        }
                        line = rd.readLine();
                    }
                    //System.out.println("MAGNX_INIFILEDIR.ini = "+MAGNX_INIFILEDIR);
                    return null;
                }
            });
        }
        catch(PrivilegedActionException pae) {
            throw pae.getException();
        }
    }

    public static synchronized String getCatiaV5TbProperty(String key) throws Exception {
        if(INCAT_DITS_CATIAV5_TB_INI == null) {
            loadINCAT_DITS_CATIAV5_TB_INI();
        }
        return INCAT_DITS_CATIAV5_TB_INI.getProperty(key);
    }
    
    public static synchronized String getUGTbProperty(String key) throws Exception {
        if(MAGNX_INIFILEDIR == null) {
            loadMAGNX_INIFILEDIR();
        }
        return MAGNX_INIFILEDIR.getProperty(key);
    }
    
    /**
     * get the value of an environment variable 
     * @param sVarName  : environment variable name
     * @return          : variable value
     * @throws Exception
     */
    public static String getVariableEnvironmentValue( String sVarName ) throws Exception {
        String sValue = null;
        String sType = System.getProperty("os.name");
        if(sType.indexOf("Windows")<0) {
            String[] sCmd = new String[]{"ksh", "-c", "env | grep ^" + sVarName + "="};
            sValue = Local_OS_Information.executeCommand(sCmd , 2000, true);
        }
        else {
            String sCmd = "cmd /c set " + sVarName;
            sValue = Local_OS_Information.executeCommand(sCmd , 2000, true);
        }
        sValue = sValue.substring(sValue.indexOf("=") + 1).trim();
        
        return sValue;
    }
    
}

