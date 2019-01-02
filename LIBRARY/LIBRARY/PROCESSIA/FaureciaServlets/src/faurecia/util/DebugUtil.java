/*
 * Creation Date : 8 July 04
 * 
 */
package faurecia.util;

/**
 * @author fcolin
 *
 * Classe permettant de tracer les messages de debug
 * required JVM : 1.1
 */
public class DebugUtil {
    public static boolean bDebugModeIsOn = true;
    public static int DEBUG = 4;
    public static int INFO = 3;
    public static int WARNING = 2;
    public static int ERROR = 1;
    public static int iDebugLevel = 4;
    
    
    public static void debug (int iTraceLevel, String sMessage) {
        if (iTraceLevel <= iDebugLevel) {
            System.out.println(sMessage);
            System.out.flush();
        }        
    }

    public static void debug (int iTraceLevel, String sMessage1, String[] aStringArray) {
        if (iTraceLevel <= iDebugLevel) {
            
            System.out.println(sMessage1);
            for (int i = 0; i<aStringArray.length; i++) {
                System.out.println("   --  " + aStringArray[i]);
            }
            System.out.flush();
        }        
        
    }

    public static void debug (int iTraceLevel, String sMessage1, String sMessage2, String[] aStringArray) {
        if (iTraceLevel <= iDebugLevel) {
            
            System.out.println(sMessage1 + " : " + sMessage2);
            for (int i = 0; i<aStringArray.length; i++) {
                System.out.println("   --  " + aStringArray[i]);
            }
            System.out.flush();
        }        
        
    }

    public static void debug (int iTraceLevel, String sMessage1, String sMessage2, Object[] aObjectArray) {
        if (iTraceLevel <= iDebugLevel) {
            
            System.out.println(sMessage1 + " : " + sMessage2);
            for (int i = 0; i<aObjectArray.length; i++) {
                System.out.println("   --  " + aObjectArray[i].getClass().getName() + " :   " + aObjectArray[i].toString());
            }
            System.out.flush();
        }        
        
    }
    // usage : debug(this.getClass().getName(), "My message");
    public static void debug (int iTraceLevel, String sMessage1, String sMessage2) {
        if (iTraceLevel <= iDebugLevel) {
            System.out.println(sMessage1 + " : " + sMessage2);
            System.out.flush();
        }        
    }

    // usage : debug(this, "My message");
    public static void debug (int iTraceLevel, Object obj, String sMessage2) {
        if (iTraceLevel <= iDebugLevel) {
            System.out.println(obj.getClass().getName() + " : " + sMessage2);
            System.out.flush();
        }        
    }
    
    public static void initDebugger(boolean b_Debug) {
        if (b_Debug) {
            DebugUtil.iDebugLevel = DebugUtil.DEBUG;
        } else {
            DebugUtil.iDebugLevel = DebugUtil.INFO;
        }
        System.out.println("Set debugger trace to " + DebugUtil.iDebugLevel);
    }

    public static void initDebugger(int i_Debug) {
        DebugUtil.iDebugLevel = i_Debug;
        System.out.println("Set debugger trace to " + DebugUtil.iDebugLevel);
    }


}
