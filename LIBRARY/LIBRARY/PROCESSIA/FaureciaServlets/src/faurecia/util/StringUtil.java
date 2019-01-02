/*
 * Creation Date : 9 juil. 04
 */
package faurecia.util;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author fcolin
 */
public class StringUtil {

    public static String[] split(String sInitString, String sDelim) {
        StringTokenizer token = new StringTokenizer(sInitString, sDelim, true);
        Vector v_RetCode = new Vector();
        int iCpt = 0;
        while (token.hasMoreTokens()) {
            String sElt = token.nextToken();
            if (!sDelim.equals(sElt)) {
                v_RetCode.addElement(sElt);
                if (token.hasMoreTokens()) {
                    token.nextToken();
                }
            } else {
                v_RetCode.addElement("");
            }
            iCpt++;
        }
        String[] sRetCode = new String[v_RetCode.size()];

        iCpt = 0;
        for (Enumeration e = v_RetCode.elements(); e.hasMoreElements();) {
            sRetCode[iCpt] = (String) e.nextElement();
            iCpt++;
        }

        return sRetCode;
    }

    public static void isMandatory(String sParameterValue, String parameterName) throws Exception {
        if (returnNullIfEmpty(sParameterValue) == null) {
            throw new Exception("Missing the mandatory parameter:" + parameterName);
        }
    }

    public static String returnNullIfEmpty(String sParameterValue) {
        if ("".equals(sParameterValue)) {
            return null;
        }
        return sParameterValue;
    }

    public static String returnDefaultIfEmpty(String sParameterValue, String sDefaultValue) {
        if (sParameterValue == null || "".equals(sParameterValue)) {
            return sDefaultValue;
        }
        return sParameterValue;
    }

    /**
     * Convenient method that say if a parameter is null: A parameter is null if: - it doesn't appear in the list of parameter - It appear but there is no value for it - It appear but the value "null"
     * is passed
     * 
     */
    private boolean isNull(String s) {
        return ((s == null) || ("".equals(s.trim())) || ("null".equalsIgnoreCase(s)));
    }

    public static Hashtable constructHashtable(String sAttValue, String sDelim1, String sDelim2) {
        Hashtable ht_Ret = new Hashtable();
        String[] aTempString = split(sAttValue, sDelim2);
        for (int i = 0; i < aTempString.length; i++) {
            int iIndex = aTempString[i].indexOf(sDelim1);
            if (iIndex >= 0 && iIndex < aTempString[i].length()) {
                String sKey = aTempString[i].substring(0, iIndex);
                String sValue = aTempString[i].substring(iIndex + 1);
                ht_Ret.put(sKey, sValue);
            }
        }
        return ht_Ret;
    }

    public static String getAsString(Object data) {
        String result = null;
        if (data instanceof List) {
            List sl = (List) data;
            if (sl.size() > 0) {
                result = (String) sl.get(0);
            }
        } else {
            result = (String) data;
        }
        if (result == null) {
            result = "";
        }
        return result;
    }

    // this resembles java.lang.String.replaceFirst method, but it does not accept rexex, only plain Strings
    public static String replaceFirst(String source, String search, String replacement) {
        int index = source.indexOf(search);
        if (index >= 0) {
            StringBuffer sb = new StringBuffer();
            sb.append(source.substring(0, index));
            sb.append(replacement);
            index = index + search.length();
            sb.append(source.substring(index));
            return sb.toString();
        } else {
            return source;
        }
    }

    // this resembles java.lang.String.replaceAll method, but it does not accept rexex, only plain Strings
    public static String replaceAll(String source, String search, String replacement) {
        StringBuffer sb = new StringBuffer();
        int index = source.indexOf(search);
        int lastIndex = 0;
        while (index >= lastIndex) {
            sb.append(source.substring(lastIndex, index));
            sb.append(replacement);
            lastIndex = index + search.length();
            index = source.indexOf(search, lastIndex);
        }
        sb.append(source.substring(lastIndex));
        return sb.toString();
    }

    public static String getTextFormatString(Vector colValueList, String delimiter) {

        StringBuffer formatedColValues = new StringBuffer("");
        // Test if the the value is a number and has one value in the list
        if (colValueList.size() == 1) {
            String columnValue = (String) colValueList.get(0);
            columnValue = columnValue.replace('\n', ' ');

            formatedColValues.append(columnValue);
        } else {
            for (int i = 0; i < colValueList.size(); i++) {
                String columnValue = (String) colValueList.get(i);
                columnValue = columnValue.replace('\n', ' ');

                if (i == colValueList.size() - 1)
                    formatedColValues.append(columnValue);
                else
                    formatedColValues.append(columnValue + delimiter);
            }
        }

        return (formatedColValues.toString());
    }

    public static String replaceCharacters(String source, char[] charList, char replacementChar) {
        String retString = source;
        if (retString == null) {
            retString = "";
        }
        if (charList != null && charList.length > 0) {
            for (int index = 0; index < charList.length; index++) {
                char sElement = charList[index];
                retString = retString.replace(sElement, replacementChar);
            }

        }

        return retString;
    }

    public static String removeWhiteSpace(String str) throws Exception {
        String whiteSpace = " ";
        String newstr = "";
        try {
            for (int i = 0; i < str.length(); i++) {
                String chstr = str.charAt(i) + "";
                if (chstr.equals(whiteSpace)) {
                    newstr = str.substring(0, i) + str.substring(i + 1, str.length());
                    str = newstr;
                }
            }
        } catch (Exception e) {
            throw e;
        }
        return str;
    }

    /**
     * Return a substring representing the first <endIndex> characters
     * 
     * @param str :
     *            string
     * @param endIndex :
     *            size of the substring
     * @return : String
     */
    public static String subString(String str, int endIndex) {
        String newstr = "";

        if (endIndex >= str.length()) {
            newstr = str;
        } else {
            newstr = str.substring(0, endIndex);
        }
        return newstr;
    }
}
