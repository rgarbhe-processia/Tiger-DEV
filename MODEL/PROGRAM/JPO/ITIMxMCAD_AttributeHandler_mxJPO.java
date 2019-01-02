
/*
 ** ITIMxMCAD_AttributeHandler
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** To get the value of attributes
 */
import matrix.db.*;

public class ITIMxMCAD_AttributeHandler_mxJPO {

    /**
     * Constructor.
     * @param context
     *            the eMatrix <code>Context</code> object
     * @param args
     *            holds no arguments
     * @throws Exception
     *             if the operation fails
     * @since Sourcing V6R2009x
     */
    public ITIMxMCAD_AttributeHandler_mxJPO(Context context, String[] args) throws Exception {

    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public String getAttributeTypeAndDefaultValue(Context context, String[] listofAttributeNames) throws Exception {

        /**
         * This method takes in one or many Attributes as an input and returns corresponding Type (Integer, Real, String, Boolean, etc,) and default value (returned as string). The return will be a
         * list of AttributeName, AttributeType, Attribute Default Value (string). Each group of name, type, and default value will be separated by '|||' and items in each group will be separated by
         * '@@'. E.g.: name@@type@@value|||name2@@type2@@value2|||...|||FailedAttributeNames:failedName1,...
         **/
        String attrInfoSep = "|||";
        String attrNameTypeValueSeparator = "@@";
        StringBuffer attributeTypeDefaultBuf = new StringBuffer("");
        StringBuffer failedAttributesBuf = new StringBuffer("");
        for (int k = 0; k < listofAttributeNames.length; k++) {
            System.out.println("Looking up the Type and Default value for Attribute, " + listofAttributeNames[k]);

            /*
             * If system attribute (e.g., $$description$$) mql will not return type and default value. Assume string with empty string as default value. There are some date/time (timestamp) system
             * attributes but they need to be handled as strings by CSEs anyway.
             */
            if (listofAttributeNames[k].startsWith("$$")) {
                if (!(attributeTypeDefaultBuf.toString().equals(""))) {
                    attributeTypeDefaultBuf.append(attrInfoSep);
                }
                attributeTypeDefaultBuf.append(listofAttributeNames[k]);
                attributeTypeDefaultBuf.append(attrNameTypeValueSeparator + "string" + attrNameTypeValueSeparator);
            } else {
                MQLCommand mql = new MQLCommand();
                mql.open(context);
                String rpeVal = "print attribute '" + listofAttributeNames[k] + "' select type default dump " + attrNameTypeValueSeparator + " ;";
                /* System.out.println("Query string is: " + rpeVal + "\n"); */
                mql.executeCommand(context, rpeVal);
                String result = mql.getResult().trim();
                String mqlError = mql.getError().trim();
                System.out.println("Query error is: " + mqlError + "\n");
                System.out.println("result of query is: " + result + "\n");
                /*
                 * If error, add attribute name to list of failed attributes and continue to process next attribute
                 */
                if (!(mqlError.equals(""))) {
                    if (!(failedAttributesBuf.toString().equals(""))) {
                        failedAttributesBuf.append(",");
                    }
                    failedAttributesBuf.append(listofAttributeNames[k]);
                    continue;
                }

                if (!(attributeTypeDefaultBuf.toString().equals(""))) {
                    attributeTypeDefaultBuf.append(attrInfoSep);
                }
                attributeTypeDefaultBuf.append(listofAttributeNames[k]);
                attributeTypeDefaultBuf.append(attrNameTypeValueSeparator);
                attributeTypeDefaultBuf.append(result);
            }
        }

        /*
         * If attribute names failed MQL command, append '|FailedAttributeNames:failedAttrName1,failedAttrName2,...failedAttrNameN' Note that there may be no failed names.
         */
        attributeTypeDefaultBuf.append(attrInfoSep + "FailedAttributeNames:");
        attributeTypeDefaultBuf.append(failedAttributesBuf.toString());

        /* System.out.println("The Attribute Type and Default to be returned is as follows" + attributeTypeDefaultBuf.toString()); */

        return attributeTypeDefaultBuf.toString();
    }

    public String getFileMessageDigest(Context context, String[] listOfTNRs) throws Exception {

        /**
         * This method retuns attribute values of bussiness object. Input - list of listOfTNRs Output - list of ( listOfTNRs|IEF-FileMsgDigest attribute value )
         **/
        String attrInfoSep = "|||";
        String attrSeperator = "@@";
        StringBuffer attribututeValues = new StringBuffer("");
        StringBuffer failedTNRs = new StringBuffer("");
        String attributeName = "IEF-FileMessageDigest";
        for (int k = 0; k < listOfTNRs.length; k++) {
            MQLCommand mql = new MQLCommand();
            mql.open(context);
            String rpeVal = "print bus " + listOfTNRs[k] + " select attribute[" + attributeName + "] dump;";
            // System.out.println("Query string is: " + rpeVal + "\n");
            mql.executeCommand(context, rpeVal);
            String result = mql.getResult().trim();
            String mqlError = mql.getError().trim();
            /*
             * If error, add attribute name to list of failed attributes and continue to process next attribute
             */
            if (!(mqlError.equals(""))) {
                if (!(failedTNRs.toString().equals(""))) {
                    failedTNRs.append(",");
                }
                failedTNRs.append(listOfTNRs[k]);
                continue;
            }

            if (!(attribututeValues.toString().equals(""))) {
                attribututeValues.append(attrInfoSep);
            }

            attribututeValues.append(listOfTNRs[k]);
            attribututeValues.append(attrSeperator);
            attribututeValues.append(result);

        }

        /*
         * If attribute names failed MQL command, append '|FailedTNRs:failedTNR1,failedTNR2...failedTNRN' Note that there may be no failed names.
         */
        attribututeValues.append(attrInfoSep + "FailedTNRs:");
        attribututeValues.append(failedTNRs.toString());

        System.out.println("The IEF-FileMessageDigest values are : " + attribututeValues.toString());

        return attribututeValues.toString();
    }

    public String getRevFromFileMessageDigest(Context context, String[] listOfTNFs) throws Exception {

        /**
         * This method retuns attribute values of bussiness object. Input - list of list of type, name, filemessagedigest The format of input must be
         * type@@name@@(formatofile|nameofthefile|hascode)|||.. for ex. UG Versioned Assembly@@A-0000103@@(asm|A-0000103.prt|iSUAAcaWkIGo7sUjA6PwIA==) Output - list of type, name and revision The
         * format of output will be type@@name@@revision||| for ex. UG Versioned Assembly@@A-0000103@@A.0 The MQL query for the above would be temp query bus 'UG Versioned Assembly' A-0000103 * where
         * 'attribute[IEF-FileMessageDigest]=="(asm|A-0000103.prt|iSUAAcaWkIGo7sUjA6PwIA==)"'; This method may not work if the hascode is blank which means that user didn't checkin a file in the
         * object.
         **/

        String TNRSeperator = "|||";
        String attrSeperator = "@@";
        StringBuffer attribututeValues = new StringBuffer("");
        StringBuffer failedTNFs = new StringBuffer("");
        String strTNF;
        String[] listTNF;
        String rpeVal;
        String result;
        String mqlError;
        for (int k = 0; k < listOfTNFs.length; k++) {
            MQLCommand mql = new MQLCommand();
            mql.open(context);
            strTNF = listOfTNFs[k];

            // If input is incorrect, add the TNF to the failed list
            if (strTNF.equals("")) {
                updatefailedTNFs(failedTNFs, listOfTNFs[k]);
                continue;
            }

            // If input is incorrect, add the TNF to the the failed list
            listTNF = strTNF.split("@@");
            if (listTNF.length != 3) {
                updatefailedTNFs(failedTNFs, listOfTNFs[k]);
                continue;
            }

            rpeVal = "temp query bus '" + listTNF[0] + "' '" + listTNF[1] + "' * where 'attribute[IEF-FileMessageDigest]==\"" + listTNF[2] + "\"' select dump @@;";
            // System.out.println("Query string is: " + rpeVal + "\n");
            mql.executeCommand(context, rpeVal);
            result = mql.getResult().trim();
            mqlError = mql.getError().trim();
            // System.out.println("Result: " + result + "\n");
            // System.out.println("Error: " + mqlError + "\n");

            /*
             * If error, add the TNF o the failed list or if ENOVIA couldn't find the item add the item to the list
             */

            if (!(mqlError.equals("")) || (mqlError.equals("") && result.equals(""))) {
                updatefailedTNFs(failedTNFs, listOfTNFs[k]);
                continue;
            }

            attribututeValues.append(result);
            attribututeValues.append(TNRSeperator);

        }

        /*
         * If attribute names failed MQL command, append '|failedTNFs:failedTNF1,failedTNF2...failedTNFN' Note that there may be no failed names.
         */
        attribututeValues.append("failedTNFs:");
        attribututeValues.append(failedTNFs.toString());

        System.out.println("The return string is : " + attribututeValues.toString());

        return attribututeValues.toString();
    }

    private void updatefailedTNFs(StringBuffer failedTNFs, String TNF) {
        if (!(failedTNFs.toString().equals(""))) {
            failedTNFs.append(",");
        }

        failedTNFs.append(TNF);

        return;

    }

}