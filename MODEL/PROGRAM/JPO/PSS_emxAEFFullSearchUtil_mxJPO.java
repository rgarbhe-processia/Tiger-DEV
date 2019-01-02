import java.util.HashMap;

import matrix.db.Context;
import matrix.db.JPO;
import pss.constants.TigerConstants;

// ${CLASSNAME}.java
//
// Created on Dec 28, 2007
//
// Copyright (c) 2005-2015 Dassault Systemes.
// All Rights Reserved
// This program contains proprietary and trade secret information of
// Dassault Systems, Inc. Copyright notice is precautionary only and does
// not evidence any actual or intended publication of such program.
//

/**
 * @author Lanka The <code>${CLASSNAME}</code> class/interface contains .
 * @version AEF 10.7.3 - Copyright (c) 2005-2015, Dassault Systems, Inc.
 */

public class PSS_emxAEFFullSearchUtil_mxJPO extends emxAEFFullSearchUtilBase_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PSS_emxAEFFullSearchUtil_mxJPO.class);

    /**
     * @param context
     * @param args
     * @throws Exception
     */
    public PSS_emxAEFFullSearchUtil_mxJPO(Context context, String[] args) throws Exception {
        super(context, args);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
    }

    // Code Added for Tiger-Faurecia by SGS (for the functionality Delete Change Request) : Starts
    /**
     * @param context
     * @param args
     * @return boolean
     * @throws Exception
     */
    public boolean accessDeleteChangeRequest(Context context, String[] args) throws Exception {
        try {
            boolean access = false;
            HashMap requestMap = (HashMap) JPO.unpackArgs(args);
            String field = (String) requestMap.get("field");

            if (field != null) {
                String[] kvFieldPairs = field.split(":");
                for (String fieldValue : kvFieldPairs) {
                    String value = "";
                    String[] KeyPair = fieldValue.split("=");
                    String key = KeyPair[0];

                    if (KeyPair.length > 1)
                        value = KeyPair[1];

                    if ((key.equals("TYPES") && value.equals("Change Request")) || (key.equals("TYPES") && value.equals(TigerConstants.POLICY_PSS_CHANGEREQUEST))) {
                        access = true;
                        break;
                    }
                }
            }
            return access;
        } catch (Exception e) {
            logger.error("Error in accessDeleteChangeRequest: ", e);
            return false;
        }
    }

}
// Code ends