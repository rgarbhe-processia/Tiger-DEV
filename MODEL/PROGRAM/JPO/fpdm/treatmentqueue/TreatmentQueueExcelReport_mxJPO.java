package fpdm.treatmentqueue;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import com.matrixone.apps.domain.DomainConstants;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.FrameworkException;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.Context;
import matrix.db.JPO;

public class TreatmentQueueExcelReport_mxJPO extends fpdm.treatmentqueue.TreatmentQueueAbstract_mxJPO {

    public TreatmentQueueExcelReport_mxJPO(Context context, String[] args, String idQueue) throws Exception {
        super(context, args, idQueue);
        init(context, args);
    }

    public TreatmentQueueExcelReport_mxJPO(Context context, String[] args) throws FrameworkException {
        super(context, args);
        init(context, args);
    }

    public void init(Context context, String[] args) throws FrameworkException {
        this.RELATIONSHIP_THIS_REPORT_QUEUE = "FPDM_TreatmentQueueToDoExcelReport";
        this.jobJPOName = "fpdm.treatmentqueue.FPDM_TreatmentQueueExcelReport";
        this.jobMethodName = "processJobReport";

        this.treatmentQueueName = EnoviaResourceBundle.getProperty(context, "FPDM_ExcelReport.treatmentQueueName");
        System.out.println("<FPDM_TreatmentQueueExcelReport> Queue Name (from property) : " + this.treatmentQueueName);

        System.out.println("<FPDM_TreatmentQueueExcelReport> Relation : " + this.RELATIONSHIP_THIS_REPORT_QUEUE);
    }

    public Vector getActionCommands(Context context, String[] args) throws Exception {
        System.out.println("<FPDM_TreatmentQueueExcelReport>");

        HashMap requestMap = (HashMap) JPO.unpackArgs(args);
        MapList objList = (MapList) requestMap.get("objectList");

        Vector columnValues = new Vector();

        Iterator<Hashtable> itObjectList = objList.iterator();

        while (itObjectList.hasNext()) {
            Hashtable oneObject = itObjectList.next();
            System.out.println("<FPDM_TreatmentQueueExcelReport>" + oneObject);

            String cellContent = "";
            cellContent += "<a href=\"FPDM_TreatmentQueue/FPDM_TreatmentQueueUtils.jsp?action=cancel&idconnection=" + oneObject.get("id[connection]") + "\">Cancel</a> | ";
            cellContent += "<a href=\"FPDM_TreatmentQueue/FPDM_TreatmentQueueUtils.jsp?action=add&idconnection=" + oneObject.get("id[connection]") + "\">Add</a>";

            columnValues.add(cellContent);
        }

        return columnValues;
    }

    public static void getVoid(Context context, String[] args) throws Exception {
        HashMap requestMap = (HashMap) JPO.unpackArgs(args);
        System.out.println("<FPDM_TreatmentQueueExcelReport> " + requestMap);

        TreatmentQueueExcelReport_mxJPO tqExcelReport = new TreatmentQueueExcelReport_mxJPO(context, args, (String) requestMap.get("queueId"));
        tqExcelReport.launchTreatmentQueue(context, args);
    }

    /*
     * public MapList newJob(Context context, String[] args, Relationship rlToProcess) throws MatrixException { System.out.println ("<FPDM_TreatmentQueueExcelReport> New Job : " +
     * rlToProcess.getName()); return new MapList(); }
     */

    // public void launchTreatmentQueue(Context context, String[] args) throws FrameworkException, MatrixException {
    // System.out.println("<FPDM_TreatmentQueueExcelReport> Relation for Treatment : " + this.RELATIONSHIP_THIS_REPORT_QUEUE);
    // launchTreatmentQueue(context, args);
    // }

    public void processJobReport(Context context, String[] args) throws Exception {
        HashMap hmParams = JPO.unpackArgs(args);
        System.out.println("<FPDM_TreatmentQueueExcelReport> Job...");
        System.out.println("<FPDM_TreatmentQueueExcelReport> " + hmParams);
    }
}
