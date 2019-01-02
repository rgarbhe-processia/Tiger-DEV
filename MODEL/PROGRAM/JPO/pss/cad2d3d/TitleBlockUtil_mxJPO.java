package pss.cad2d3d;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;

/**
 * Class gathering some util methods
 * @author famann
 */
public class TitleBlockUtil_mxJPO {
    // TIGTK-5406 - 03-04-2017 - VP - START
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(pss.cad2d3d.TitleBlockUtil_mxJPO.class);
    // TIGTK-5406 - 03-04-2017 - VP - END

    static public final int MIN = 192;

    static public final int MAX = 255;

    static public final Vector<String> map = initMap();

    /**
     * The cad definition name pattern.
     */
    private static final String CAD_DEFINITION_NAME_PATTERN = "([0-9]+X?)([A-Z]+.*)";

    /**
     * Reformat a list of lines in splitting all lines in some N characters max lines. ex : vInitialLines = ["First line is the one", "Second line isnot the first"] iMaxNbOfCharactersPerLine = 9 =>
     * Resulted vector = ["First line", "is the on", "e", "Second li", "ne isnot ", "the first"]
     * @param vInitialLines
     *            Initial vector of string
     * @param iMaxNbOfCharactersPerLine
     *            Max Nb Of Characters Per Line
     * @return Reformatted list of lines
     * @throws Exception
     */
    public static Vector<String> reformatLines(Vector<String> vInitialLines, int iMaxNbOfCharactersPerLine) throws Exception {
        int iLimit = iMaxNbOfCharactersPerLine - 2;
        Vector<String> vReformatedLines = new Vector<String>();
        try {
            for (int ii = 0; ii < vInitialLines.size(); ii++) {
                String sLineDescription = (vInitialLines.elementAt(ii)).trim();
                if (sLineDescription.length() <= iLimit) {
                    vReformatedLines.add(sLineDescription);
                } else {
                    Vector<String> vListOfWords = split(sLineDescription, " ");
                    Iterator<String> iter = vListOfWords.iterator();
                    String strLine = "";
                    String strLineBack = "";
                    while (iter.hasNext()) {
                        String strWord = iter.next();
                        if (("EO:".equals(strWord) || "ECO:".equals(strWord) || "ECR:".equals(strWord) || "ECRcd:".equals(strWord)) && !"".equals(strLine)) {
                            vReformatedLines.add(strLine);
                            strLine = "";
                        }
                        strLineBack = strLine;
                        strLine += strWord + " ";
                        int iLength = strLine.length();
                        if (iLength < iLimit) {
                            continue;
                        }
                        logger.debug("/reformatLines --> strLine : " + strLine);
                        vReformatedLines.add(strLineBack);
                        strLine = "";
                        strLine += strWord + " ";

                    }
                    vReformatedLines.add(strLine);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in reformatLines: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return vReformatedLines;
    }

    /**
     * Get Maximum Number of characters writable in a line This piece of information is retrieved from the txt file (7th element) ex : in following section [DESCRIPTION] POLT(0.95,1.9,0.475,0.05)
     * TEXT(-100.5,1.25,left,none,[0],70) [END] => MaxNumberOfCharactersPerLine is 70
     * @param sSection
     *            Name of the section (ex: "DESCRIPTION")
     * @param htTabInstruction
     *            Hashtable where section is memorized
     * @return First defined Maximum Number of characters writable in a line (-1 if not defined)
     */
    public static int getMaxNumberOfCharactersPerLine(String sSection, Hashtable<String, Vector<String>> htTabInstruction) {
        int iMaxNbOfCharactersPerLine = -1;
        try {
            Vector<String> vLines = htTabInstruction.get(sSection);
            for (int iCpt = 0; iCpt < vLines.size(); iCpt++) {
                String sLine = vLines.get(iCpt);
                if (sLine.trim().startsWith("TEXT")) {
                    StringTokenizer token = new StringTokenizer(sLine, "(),");
                    if (token.countTokens() >= 7) {
                        for (int ii = 1; ii <= 6; ii++) {
                            token.nextToken();
                        }
                        iMaxNbOfCharactersPerLine = Integer.parseInt(token.nextToken());
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("/getMaxNumberOfCharactersPerLine: Unable to get max number of characters per line for section " + sSection);
            logger.error("/getMaxNumberOfCharactersPerLine: Error launched : " + ex.getMessage());
        }
        return iMaxNbOfCharactersPerLine;

    }

    /**
     * Split a String and return a Vector
     * @param sString
     *            : The string to split
     * @param sSplitter
     *            : The delimiter char
     * @return Vector
     * @throws Exception
     */
    public static Vector<String> split(String sString, String sSplitter) throws Exception {
        Vector<String> vTemp = new Vector<String>();
        StringTokenizer token = new StringTokenizer(sString, sSplitter);
        while (token.hasMoreTokens()) {
            String sTemp = token.nextToken().trim();
            if (!"".equals(sTemp))
                vTemp.addElement(sTemp);
        }
        return vTemp;
    }

    // FPDM ADD Start JFA 10/07/2006 RFC 4728
    /**
     * Initialize the array containing the mapping between characters with accent and their corresponding char
     * @return
     */
    private static Vector<String> initMap() {
        Vector<String> vResult = new Vector<String>();
        String car = null;

        car = "A";
        vResult.add(car); /* '\u00C0'alt-0192 */
        vResult.add(car); /* '\u00C1'alt-0193 */
        vResult.add(car); /* '\u00C2'alt-0194 */
        vResult.add(car); /* '\u00C3'alt-0195 */
        vResult.add(car); /* '\u00C4'alt-0196 */
        vResult.add(car); /* '\u00C5'alt-0197 */
        car = "AE";
        vResult.add(car); /* '\u00C6'alt-0198 */
        car = "C";
        vResult.add(car); /* '\u00C7'alt-0199 */
        car = "E";
        vResult.add(car); /* '\u00C8'alt-0200 */
        vResult.add(car); /* '\u00C9'alt-0201 */
        vResult.add(car); /* '\u00CA'alt-0202 */
        vResult.add(car); /* '\u00CB'alt-0203 */
        car = "I";
        vResult.add(car); /* '\u00CC'alt-0204 */
        vResult.add(car); /* '\u00CD'alt-0205 */
        vResult.add(car); /* '\u00CE'alt-0206 */
        vResult.add(car); /* '\u00CF'alt-0207 */
        car = "D";
        vResult.add(car); /* '\u00D0'alt-0208 */
        car = "N";
        vResult.add(car); /* '\u00D1'alt-0209 */
        car = "O";
        vResult.add(car); /* '\u00D2'alt-0210 */
        vResult.add(car); /* '\u00D3'alt-0211 */
        vResult.add(car); /* '\u00D4'alt-0212 */
        vResult.add(car); /* '\u00D5'alt-0213 */
        vResult.add(car); /* '\u00D6'alt-0214 */
        car = "*";
        vResult.add(car); /* '\u00D7'alt-0215 */
        car = "0";
        vResult.add(car); /* '\u00D8'alt-0216 */
        car = "U";
        vResult.add(car); /* '\u00D9'alt-0217 */
        vResult.add(car); /* '\u00DA'alt-0218 */
        vResult.add(car); /* '\u00DB'alt-0219 */
        vResult.add(car); /* '\u00DC'alt-0220 */
        car = "Y";
        vResult.add(car); /* '\u00DD'alt-0221 */
        car = "\u00DD";
        vResult.add(car); /* '\u00DE'alt-0222 */
        car = "B";
        vResult.add(car); /* '\u00DF'alt-0223 */
        car = "a";
        vResult.add(car); /* '\u00E0'alt-0224 */
        vResult.add(car); /* '\u00E1'alt-0225 */
        vResult.add(car); /* '\u00E2'alt-0226 */
        vResult.add(car); /* '\u00E3'alt-0227 */
        vResult.add(car); /* '\u00E4'alt-0228 */
        vResult.add(car); /* '\u00E5'alt-0229 */
        car = "ae";
        vResult.add(car); /* '\u00E6'alt-0230 */
        car = "c";
        vResult.add(car); /* '\u00E7'alt-0231 */
        car = "e";
        vResult.add(car); /* '\u00E8'alt-0232 */
        vResult.add(car); /* '\u00E9'alt-0233 */
        vResult.add(car); /* '\u00EA'alt-0234 */
        vResult.add(car); /* '\u00EB'alt-0235 */
        car = "i";
        vResult.add(car); /* '\u00EC'alt-0236 */
        vResult.add(car); /* '\u00ED'alt-0237 */
        vResult.add(car); /* '\u00EE'alt-0238 */
        vResult.add(car); /* '\u00EF'alt-0239 */
        car = "d";
        vResult.add(car); /* '\u00F0'alt-0240 */
        car = "n";
        vResult.add(car); /* '\u00F1'alt-0241 */
        car = "o";
        vResult.add(car); /* '\u00F2'alt-0242 */
        vResult.add(car); /* '\u00F3'alt-0243 */
        vResult.add(car); /* '\u00F4'alt-0244 */
        vResult.add(car); /* '\u00F5'alt-0245 */
        vResult.add(car); /* '\u00F6'alt-0246 */
        car = "/";
        vResult.add(car); /* '\u00F7'alt-0247 */
        car = "0";
        vResult.add(car); /* '\u00F8'alt-0248 */
        car = "u";
        vResult.add(car); /* '\u00F9'alt-0249 */
        vResult.add(car); /* '\u00FA'alt-0250 */
        vResult.add(car); /* '\u00FB'alt-0251 */
        vResult.add(car); /* '\u00FC'alt-0252 */
        car = "y";
        vResult.add(car); /* '\u00FD'alt-0253 */
        car = "\u00DD";
        vResult.add(car); /* '\u00FE'alt-0254 */
        car = "y";
        vResult.add(car); /* '\u00FF'alt-0255 */
        vResult.add(car); /* '\u00FF' alt-0255 */

        return vResult;
    }

    /**
     * Format a string containing special characters (accents) to no accent
     * @param ch
     *            : the string to format
     * @return String well formatted
     */
    private static String formatString(String ch) {
        StringBuffer Result = new StringBuffer(ch);
        for (int bcl = 0; bcl < Result.length(); bcl++) {
            int carVal = ch.charAt(bcl);
            if (carVal >= MIN && carVal <= MAX) {
                String newVal = map.get(carVal - MIN);
                Result.replace(bcl, bcl + 1, newVal);
            }
        }
        return Result.toString();
    }

    /**
     * Format a String by removing all special characters (accents) , and replace carriage return by " - "
     * @param ch
     *            : the string to format
     * @param strCompany
     * @return
     */
    public static String formatDescription(String ch) {
        String sFormatedTxt = "";
        if (ch != null && !"".equals(ch)) {
            sFormatedTxt = formatString(ch);
        }
        return sFormatedTxt;
    }

    /**
     * Format a String by removing a special character
     * @param ASCIICode
     * @param sReplaceBy
     * @param sValue
     * @param ch
     *            : the string to format
     * @return
     */
    public static String convertBadChar(int ASCIICode, String sReplaceBy, String sValue) {
        String sConvertedDesc = "";
        StringBuffer sbConvertedDesc = new StringBuffer();
        try {
            char[] charSeq = sValue.toCharArray();
            for (int ic = 0; ic < charSeq.length; ic++) {
                char c = charSeq[ic];
                int iAscii = (Character.valueOf(c)).hashCode();
                if (iAscii != ASCIICode)
                    sbConvertedDesc.append(c);
                else
                    sbConvertedDesc.append(sReplaceBy);
            }
            sConvertedDesc = sbConvertedDesc.toString();
            if (!"".equals(sConvertedDesc))
                sValue = sConvertedDesc;
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in convertBadChar: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return sValue;
    }

    /**
     * Extract the CAD definition Major revision from the CAD definition Name.
     * @param sCADDefinitionName
     *            the cad definition name
     * @return the Major revision of the given CAD definition name
     */
    public static String extractCADDefinitionMajorRev(String sCADDefinitionName) {
        String sMajorRev = null;
        try {
            if (sCADDefinitionName != null) {
                java.util.regex.Pattern regex = java.util.regex.Pattern.compile(CAD_DEFINITION_NAME_PATTERN);
                Matcher regexMatcher = regex.matcher(sCADDefinitionName);
                if (regexMatcher.find()) {
                    sMajorRev = regexMatcher.group(2);
                }
            }
        } catch (Exception e) {
            // TIGTK-5405 - 11-04-2017 - VB - START
            logger.error("Error in extractCADDefinitionMajorRev: ", e);
            // TIGTK-5405 - 11-04-2017 - VB - END
        }
        return sMajorRev;
    }
}
