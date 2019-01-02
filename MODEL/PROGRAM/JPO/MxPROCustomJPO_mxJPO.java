
/*
 ** MxPROCustomJPO
 **
 ** Copyright Dassault Systemes, 1992-2007. All Rights Reserved. This program contains proprietary and trade secret information of Dassault Systemes and its subsidiaries, Copyright notice is
 * precautionary only and does not evidence any actual or intended publication of such program
 **
 ** Program to use as trigger on Revise event of Major Objects
 */

import java.io.*;
import java.util.*;

import matrix.db.Context;
import matrix.db.MatrixWriter;
import matrix.util.MatrixException;
import matrix.util.StringList;

public class MxPROCustomJPO_mxJPO {
    /**
     * The no-argument constructor.
     */
    public MxPROCustomJPO_mxJPO() {
    }

    /**
     * Constructor which accepts the Matrix context and an array of String arguments.
     */
    public MxPROCustomJPO_mxJPO(Context context, String[] args) throws Exception {
    }

    public int mxMain(Context context, String[] args) throws Exception {
        return 0;
    }

    public String logUnSavedTNRDetails(Context context, String[] args) throws Exception {
        String sResult = "failed";
        try {

            System.out.println("[MxPROCustomJPO.logUnSavedTNRDetails]... entered ");
            String errorName = args[0];
            String userName = args[1];
            String topLevelAssemblyName = args[2];
            String unSavedFileNames = args[3];

            System.out.println("[MxPROCustomJPO.logUnSavedTNRDetails]... errorName: " + errorName + " + userName: " + userName + " topLevelAssemblyName: " + topLevelAssemblyName
                    + " unSavedFileNames: " + unSavedFileNames);

            String output = "errorName: " + errorName + " userName: " + userName + " topLevelAssemblyName: " + topLevelAssemblyName + " unSavedFileNames: " + unSavedFileNames;
            write("MxPROCustomJPO_logUnSavedTNRDetails.log", output);
            System.out.println("Sucessfull writing of log MxPROCustomJPO_logUnSavedTNRDetails.log");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[MxPROCustomJPO.logUnSavedTNRDetails]... Error logging TNR...");
        }

        return sResult;
    }

    private void write(String fileName, String output) throws IOException {
        FileWriter aWriter = new FileWriter(fileName, true);
        aWriter.write(output + "\n");
        aWriter.flush();
        aWriter.close();
    }

}
