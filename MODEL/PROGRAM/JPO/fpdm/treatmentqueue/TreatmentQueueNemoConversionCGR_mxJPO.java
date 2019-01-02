package fpdm.treatmentqueue;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matrixone.apps.domain.DomainRelationship;
import com.matrixone.apps.domain.util.EnoviaResourceBundle;
import com.matrixone.apps.domain.util.MapList;

import matrix.db.BusinessObject;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.util.MatrixException;

public class TreatmentQueueNemoConversionCGR_mxJPO extends fpdm.treatmentqueue.TreatmentQueueAbstract_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.treatmentqueue.TreatmentQueueNemoConversionCGR");

    public static final String ATTRIBUTE_RELATIONSHIP_QUEUE_TODO_NEMO_ACTION = "FPDM_QueueJobNemoAction";

    public TreatmentQueueNemoConversionCGR_mxJPO(Context context, String[] args, String idQueue) throws Exception {
        super(context, args, idQueue);
        init(context, args);
    }

    public TreatmentQueueNemoConversionCGR_mxJPO(Context context, String[] args) throws MatrixException {
        super(context, args);
        init(context, args);
    }

    public void init(Context context, String[] args) throws MatrixException {
        this.TYPE_TREATMENT_QUEUE = "FPDM_TreatmentQueueNemoConversion";
        this.RELATIONSHIP_THIS_REPORT_QUEUE = "FPDM_TreatmentQueueToDoNemoCGRViewables";
        this.jobJPOName = "fpdm.treatmentqueue.FPDM_TreatmentQueueNemoConversion";
        this.jobMethodName = "processJobReport";

        this.treatmentQueueName = "FPDM_TreatmentQueueNemoCGRViewable";
        logger.info("<FPDM_TreatmentQueueNamoConversion> Queue Name (from property) : " + this.treatmentQueueName);
        logger.info("<FPDM_TreatmentQueueNamoConversion> Relation : " + this.RELATIONSHIP_THIS_REPORT_QUEUE);
        this.boQueue = new BusinessObject(this.TYPE_TREATMENT_QUEUE, this.treatmentQueueName, "-", "");
        boQueue.open(context);
        logger.info(boQueue.toString());
    }

    public MapList getOnDemandElements(Context context, String[] args) throws Exception {
        HashMap<String, Object> requestMap = (HashMap<String, Object>) JPO.unpackArgs(args);
        String sRelationName = (String) requestMap.get("relationName");

        MapList mlResult = new MapList();

        if (!"".equals(this.treatmentQueueName)) {
            logger.info("<TreatmentQueueNemoConversionCGR> Vault : " + context.getVault().getName());

            BusinessObject boQueue = new BusinessObject(this.TYPE_TREATMENT_QUEUE, this.treatmentQueueName, "-", null);
            logger.info(boQueue.toString());

            boQueue.open(context);

            String queueId = boQueue.getObjectId(context);
            MapList mlObjectsList = fpdm.treatmentqueue.TreatmentQueueAbstract_mxJPO.getAllObjectsInTreatmentQueue(context, queueId, sRelationName,
                    "attribute[FPDM_QueueJobIsOnDemand].value == 'TRUE'", new String[] { "*" }, this.queueStatesToNotShow);
            boQueue.close(context);

            mlResult.addAll(mlObjectsList);
        }
        logger.info("<TreatmentQueueNemoConversionCGR> MAPLIST : " + mlResult);
        return mlResult;
    }

    @Override
    public Vector getActionCommands(Context context, String[] args) throws Exception {
        logger.info("<FPDM_TreatmentQueueNemoConversionCGR>");

        HashMap requestMap = (HashMap) JPO.unpackArgs(args);
        MapList objList = (MapList) requestMap.get("objectList");

        Vector columnValues = new Vector();

        Iterator<Hashtable> itObjectList = objList.iterator();

        while (itObjectList.hasNext()) {
            Hashtable oneObject = itObjectList.next();
            logger.info("<FPDM_TreatmentQueueNamoConversion>" + oneObject);

            String cellContent = "";

            cellContent += "<a href=\"../engineeringcentral/FPDM_NemoProcesses.jsp?action=cancel&relId=" + oneObject.get("id[connection]") + "\" target=\"listHidden\">Cancel</a>";

            columnValues.add(cellContent);
        }

        return columnValues;
    }

    public static void getVoid(Context context, String[] args) throws Exception {
        HashMap requestMap = (HashMap) JPO.unpackArgs(args);
        logger.info("<FPDM_TreatmentQueueNemoConversionCGR> " + requestMap);

        TreatmentQueueNemoConversionCGR_mxJPO tqNemoConversion = new TreatmentQueueNemoConversionCGR_mxJPO(context, args, (String) requestMap.get("queueId"));
        tqNemoConversion.launchTreatmentQueue(context, args);
    }

    public void processJobReport(Context context, String[] args) throws Exception {
        HashMap hmParams = JPO.unpackArgs(args);
        logger.info("<FPDM_TreatmentQueueNemoConversionCGR> Job...");
        logger.info("<FPDM_TreatmentQueueNemoConversionCGR> " + hmParams);
    }

    public Vector<Object> getMessage(Context context, String[] args) throws Exception {
        @SuppressWarnings("rawtypes")
        HashMap requestMap = (HashMap) JPO.unpackArgs(args);

        String sLanguage = context.getSession().getLanguage();

        System.out.println(requestMap);

        Vector<Object> columnValues = new Vector<Object>();

        MapList objList = (MapList) requestMap.get("objectList");

        @SuppressWarnings({ "rawtypes", "unchecked" })
        Iterator<Hashtable> itObjectList = objList.iterator();

        while (itObjectList.hasNext()) {
            Hashtable<String, String> htInfos = itObjectList.next();
            DomainRelationship dRelation = new DomainRelationship((String) htInfos.get("id[connection]"));
            String sJobProcessStatus = dRelation.getAttributeValue(context, "FPDM_QueueJobProcessStatus");
            if ("".equals(sJobProcessStatus)) {
                sJobProcessStatus = "EMPTY";
            }

            String message = EnoviaResourceBundle.getProperty(context, "EngineeringCentral", "FPDM.NEMO.Message." + sJobProcessStatus, sLanguage);

            columnValues.add(message);
        }

        return columnValues;
    }
}
