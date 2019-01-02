import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.matrixone.apps.domain.util.ContextUtil;
import com.matrixone.apps.domain.util.PropertyUtil;

import dsis.com.props.DBPropertyUtils;
import dsis.com.xml.props.PropertyUtils;
import dsis.com.xpdmapp.XPDMXMLGenerator;
import matrix.db.Context;

/**
 * Clone of XPDMExtract_mxJPO
 */
public class PSS_XPDMExtract_mxJPO extends XPDMExtract_mxJPO {
    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(XPDMExtract_mxJPO.class);

    private int counter = 1;

    /**
     * Clone of extract method
     * @param context
     * @param args
     * @return
     */
    public Map<String, Object> publishEBOM(Context context, String[] args) throws Exception {
        Map<String, Object> returnMap = new HashMap<String, Object>();
        XPDMXMLGenerator gen = null;
        boolean isPushed = false;
        try {
            // MBO-129:Start
            PropertyUtil.setRPEValue(context, "CURRENT_USER", context.getUser(), true);
            // MBO-129:End
            ContextUtil.pushContext(context, PropertyUtil.getSchemaProperty(context, "person_UserAgent"), "", "");
            isPushed = true;
            InitializePropertyUtils(context);
            logger.info("\n\n" + getNextNumber() + ". Parsing arguments.");
            Map<ArgType, String> mArgs = parseArgs(args);
            boolean isFilterEnabled = Boolean.parseBoolean(mArgs.get(ArgType.FILTER));
            // boolean isDebugMode = Boolean.parseBoolean(mArgs.get(ArgType.DEBUG));

            preqsValidation(isFilterEnabled);
            logger.info("\n" + getNextNumber() + ". Extraction process initialized.");

            String filename = mArgs.get(ArgType.FILENAME);
            String filterFile = mArgs.get(ArgType.FILTER_FILE);

            boolean isDumpCGR = Boolean.parseBoolean(mArgs.get(ArgType.DUMP_CGR));
            boolean isEffectivityExtract = parseEffectivityExtractArg(mArgs);

            gen = new XPDMXMLGenerator(context, true, isDumpCGR, true, isEffectivityExtract);
            gen.setFilterEnabled(isFilterEnabled);
            if (!isEmpty(filename)) {
                gen.setFilename(filename);
            }

            if (isFilterEnabled && !isEmpty(filterFile)) {
                gen.setFilterFile(filterFile);
            }

            logger.info(getNextNumber() + ". Extraction started.");
            logger.info("\tFilename: ", gen.getFilename());
            logger.info("\tXML checkout dir: ", gen.getBaseOutputDir());

            gen.generate(mArgs.get(ArgType.TYPE), mArgs.get(ArgType.NAME), mArgs.get(ArgType.REVISION));
            logger.info(getNextNumber() + ". Extraction completed.");

            returnMap.put("Status", gen.getXFLOWXML().getGlobalStatus().getStatus());
            // returnMap.put("ObjectNumbers", gen.getXFLOWXML().getObjectStatus().getObject().size());
            returnMap.put("StatusInfoList", gen.getXFLOWXML().getGlobalStatus().getGlobalStatusDetailInfo());
        } catch (Exception e) {
            logger.info("\n->ERROR MESSAGE: ", e.getMessage());
            logger.error("Error in publishEBOM: ", e);
            throw e;
        } finally {
            if (isPushed) {
                ContextUtil.popContext(context);
            }
        }
        return returnMap;
    }

    private void InitializePropertyUtils(Context context) {
        PropertyUtils.InitPropertyUtil(context);
        DBPropertyUtils.InitPropertyUtil(context);
    }

    private boolean parseEffectivityExtractArg(Map<ArgType, String> mArgs) {
        String extractEffectivity = mArgs.get(ArgType.EXTRACT_EFFECTIVITY);
        return (extractEffectivity != null && "n".equalsIgnoreCase(extractEffectivity)) ? false : true;
    }

    private void preqsValidation(boolean isFilterEnabled) throws Exception {
        logger.info(getNextNumber() + ". Prereqs validation started.");
        if (!XPDMXMLGenerator.validateBeforeExtraction()) {
            throw new Exception("\n\t-> Could not start extraction. Please check prereqs.");
        }

        if (isFilterEnabled) {
            XPDMXMLGenerator.validateFilter();
        }
        logger.info(getNextNumber() + ". Prereqs validation completed. All good!");
    }

    private int getNextNumber() {
        return counter++;
    }

    private boolean isEmpty(String text) {
        return (text == null || text.trim().isEmpty());
    }

    private Map<ArgType, String> parseArgs(String[] args) throws Exception {
        Map<ArgType, String> mArgs = new HashMap<ArgType, String>(7);
        mArgs.put(ArgType.FORCE_CREATE_DIR, "FALSE");
        mArgs.put(ArgType.FILTER, "FALSE");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                ArgType argType = ArgType.findArgtype(arg);
                if (argType == null) {
                    logger.info("\n\tINFO: Arg " + arg + " is not valid which get ignored during extraction.\n");
                    continue;
                }
                String nextArg = getNextArgument(args, arg, argType, ++i);
                mArgs.put(argType, nextArg);
            }
        }

        if (mArgs.containsKey(ArgType.OID) && (mArgs.containsKey(ArgType.TYPE) || mArgs.containsKey(ArgType.REVISION) || mArgs.containsKey(ArgType.NAME))) {
            logger.info("\n\tINFO: Argument contains OID. TNR get ignored during extraction proccess\n");
        }

        return mArgs;
    }

    private String getNextArgument(String[] args, String arg, ArgType argType, int count) throws Exception {
        String nextArg = args[count];
        if (argType == ArgType.FILTER) {
            nextArg = "TRUE";
        } else if (argType == ArgType.DEBUG) {
            nextArg = "TRUE";
        } else if (argType == ArgType.REVISION) {
            return nextArg;
        }

        if (nextArg.startsWith("-")) {
            StringBuilder errorBuilder = new StringBuilder("Invalid arg sequence ");
            errorBuilder.append(Arrays.toString(args).replaceAll("[\\[,\\]]", " ")).append(" \n");
            errorBuilder.append("\tExpecting '").append(argType.getText()).append("' after ").append(arg);
            errorBuilder.append("\n\t-> i.e. ").append(arg).append(" <<").append(argType.getText()).append(">> ");
            throw new Exception(errorBuilder.toString());
        }

        return nextArg;
    }

    enum ArgType {

        OID("-oid", "oid"), TYPE("-type", "business object type"), NAME("-name", "business object name"), REVISION("-rev", "business object revision"), XPDM_DIR("-xmlDir",
                "XPDM xml checkout directory path"), FILENAME("-filename", "XPDM xml file name"), CAD_DIR("-cadDir", "CAD checkout directory path"), FORCE_CREATE_DIR("-forceDir",
                        " Boolean value(True/Flase) to force fully create driectory structure."), DUMP_CGR("-dumpCGR", " Boolean value(True/Flase) to dump CGR."), EXTRACT_EFFECTIVITY("-ef",
                                "Extract effectivity (default:yes)"), FILTER("-filter",
                                        "Enable filter from file"), FILTER_FILE("-filterFile", "Filter part based on given filter.txt file"), DEBUG("-debug", "Run extractor in debug mode");

        private final String value, text;

        ArgType(String arg, String text) {
            this.value = arg;
            this.text = text;
        }

        public String getValue() {
            return value;
        }

        public String getText() {
            return text;
        }

        public static ArgType findArgtype(String arg) {
            for (ArgType argType : values()) {
                if (argType.getValue().equalsIgnoreCase(arg)) {
                    return argType;
                }
            }
            return null;
        }

    }
}