package fpdm.nemo;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import matrix.db.Attribute;
import matrix.db.AttributeList;
import matrix.db.AttributeType;
import matrix.db.Context;
import matrix.db.JPO;
import matrix.db.Relationship;
import matrix.util.MatrixException;

public class NEMOProcesses_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.nemo.NEMOProcesses");

    public NEMOProcesses_mxJPO(Context context, String[] args) {
        logger.info("<NemoProcesse> NEMOProcesses init...");
    }

    public String lockQueue(Context context, String[] args) {
        logger.info("<NemoProcesse> LOCK QUEUE");

        fpdm.treatmentqueue.TreatmentQueueNemoConversion_mxJPO tqNemo;
        try {
            tqNemo = new fpdm.treatmentqueue.TreatmentQueueNemoConversion_mxJPO(context, args);
            tqNemo.lockQueue(context);
        } catch (Exception e) {
            e.printStackTrace();
            return "false";
        }

        return "true";
    }

    public String unlockQueue(Context context, String[] args) {
        logger.info("<NemoProcesse> UNLOCK QUEUE");

        fpdm.treatmentqueue.TreatmentQueueNemoConversion_mxJPO tqNemo;
        try {
            tqNemo = new fpdm.treatmentqueue.TreatmentQueueNemoConversion_mxJPO(context, args);
            tqNemo.unlockQueue(context);
        } catch (Exception e) {
            e.printStackTrace();
            return "false";
        }

        return "true";
    }

    public String refreshConversions(Context context, String[] args) {
        try {
            fpdm.eai.utils.WebServiceManager_mxJPO.callEAIWS(context, "NemoWebService", "createAndUpdateConversionConfiguration", new HashMap<String, String>());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return "";
    }

    public String cancel(Context context, String[] args) throws Exception {
        logger.info("<NemoProcesse> CANCEL CONNECTION(S)");

        HashMap<String, Object> hmArgs;
        try {
            hmArgs = JPO.unpackArgs(args);
        } catch (Exception e) {
            e.printStackTrace();
            return "false";
        }

        String relID = (String) hmArgs.get("relId");

        this.cancelSingleConversion(context, relID);

        return "true";
    }

    private boolean cancelSingleConversion(Context context, String relID) throws Exception {
        logger.info("<NemoProcesse> CANCEL Single Conversion");

        Relationship rlNemo = new Relationship(relID);

        rlNemo.open(context);

        String sNemoProcessId = rlNemo.getAttributeValues(context, fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_IDENTIFIANT).getValue();

        if ("".equals(sNemoProcessId)) {
            rlNemo.remove(context);
        } else {
            AttributeList alNemoConversion = new AttributeList();
            Attribute aProcessStatus = new Attribute(new AttributeType(fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_STATUS), "CANCEL");
            alNemoConversion.add(aProcessStatus);
            rlNemo.setAttributes(context, alNemoConversion);
        }

        return true;
    }

    public String cancelConversions(Context context, String[] args) throws Exception {
        logger.info("<NemoProcesse> RESET CONNECTION");

        HashMap<String, Object> hmArgs = JPO.unpackArgs(args);
        String[] emxTableRowId = (String[]) hmArgs.get("emxTableRowId");
        List<String> listRowId = Arrays.asList(emxTableRowId);

        listRowId.stream().forEach(item -> {
            String[] ids = item.split(Pattern.quote("|"));
            try {
                this.cancelSingleConversion(context, ids[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return "true";
    }

    public String resetConversions(Context context, String[] args) throws Exception {
        logger.info("<NemoProcesse> RESET CONNECTION");

        HashMap<String, Object> hmArgs = JPO.unpackArgs(args);
        String[] emxTableRowId = (String[]) hmArgs.get("emxTableRowId");
        List<String> listRowId = Arrays.asList(emxTableRowId);

        listRowId.stream().forEach(item -> {
            String[] ids = item.split(Pattern.quote("|"));

            if (ids.length > 1) {
                String relId = ids[0];

                Relationship nemoRelation = new Relationship(relId);

                AttributeList alConversion = new AttributeList();

                Attribute attJobId = new Attribute(new AttributeType(fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_IDENTIFIANT), "");
                alConversion.add(attJobId);

                Attribute attJobProcessStatut = new Attribute(new AttributeType(fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_STATUS), "");
                alConversion.add(attJobProcessStatut);

                Attribute attJobMessage = new Attribute(new AttributeType(fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QUEUE_JOB_MESSAGE), "");
                alConversion.add(attJobMessage);

                Attribute attJobIteration = new Attribute(new AttributeType(fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QUEUE_JOB_ITERATION), "0");
                alConversion.add(attJobIteration);

                Attribute attJobLimitOfCancelTrials = new Attribute(new AttributeType(fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_FPDM_LIMIT_CANCEL_TRIALS), "0");
                alConversion.add(attJobLimitOfCancelTrials);

                try {
                    nemoRelation.setAttributes(context, alConversion);
                } catch (MatrixException e) {
                    e.printStackTrace();
                }
            }
        });

        return "true";
    }

    public String resendCheckinRequest(Context context, String[] args) throws Exception {
        logger.info("<NemoProcesse> RESET CONNECTION");

        HashMap<String, Object> hmArgs = JPO.unpackArgs(args);
        String[] emxTableRowId = (String[]) hmArgs.get("emxTableRowId");
        List<String> listRowId = Arrays.asList(emxTableRowId);

        listRowId.stream().forEach(item -> {
            String[] ids = item.split(Pattern.quote("|"));

            if (ids.length > 1) {
                String relId = ids[0];

                Relationship nemoRelation = new Relationship(relId);

                AttributeList alConversion = new AttributeList();

                Attribute attJobId = new Attribute(new AttributeType(fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_CONVERSION_STATUS), "DONE");
                alConversion.add(attJobId);

                Attribute attJobMessage = new Attribute(new AttributeType(fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QUEUE_JOB_MESSAGE), "");
                alConversion.add(attJobMessage);

                Attribute attJobIteration = new Attribute(new AttributeType(fpdm.nemo.NEMOUtils_mxJPO.ATTRIBUTE_QUEUE_JOB_ITERATION), "0");
                alConversion.add(attJobIteration);

                try {
                    nemoRelation.setAttributes(context, alConversion);
                } catch (MatrixException e) {
                    e.printStackTrace();
                }
            }
        });

        return "true";
    }
}
