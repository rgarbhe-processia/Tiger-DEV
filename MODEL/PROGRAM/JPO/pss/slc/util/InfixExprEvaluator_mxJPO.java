package pss.slc.util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class InfixExprEvaluator_mxJPO {

    protected static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InfixExprEvaluator_mxJPO.class);

    static ScriptEngineManager mgr = new ScriptEngineManager();

    static ScriptEngine engine = mgr.getEngineByName("JavaScript");

    // PCM2.0 Spr4:TIGTK-6894:13/9/2017:START
    /**
     * @Description This method is used to calculate the value for Arithmetic Expressions
     * @param sArithmeticExpression
     * @return
     * @throws Exception
     */
    public static String evaluate(String sArithmeticExpression) throws Exception {
        logger.debug("pss.slc.util.InfixExprEvaluator:evaluate:START");
        try {
            return engine.eval(sArithmeticExpression).toString();
        } catch (Exception ex) {
            logger.error("Error in pss.slc.util.InfixExprEvaluator:evaluate:ERROR ", ex);
            throw ex;
        }
    }
    // PCM2.0 Spr4:TIGTK-6894:13/9/2017:END
}
