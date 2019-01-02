/**
 * 
 */
package faurecia.util;

import java.applet.Applet;
import java.text.MessageFormat;
import java.util.Hashtable;

/**
 * @author lebasn
 *
 */
public class TranslationUtil {
    private static Hashtable htSchemaProperty;
    private static Hashtable htTranslation;
    
    public static void init(Applet applet) {
        htSchemaProperty = new Hashtable();
        htTranslation = new Hashtable();
        initHashtable(applet, "SCHEMAPROPERTY", htSchemaProperty);
        initHashtable(applet, "PROPERTYTRANSLATION", htTranslation);
        System.out.println("htSchemaProperty:"+htSchemaProperty);
        System.out.println("htTranslation:"+htTranslation);
    }

    private static void initHashtable(Applet applet, String sParamName, Hashtable htToFill) {
        int iCounter = 0;
        String sProperty = applet.getParameter(sParamName + iCounter);
        while (sProperty != null) {
            String[] sParam = StringUtil.split(sProperty, "=");
            htToFill.put(sParam[0], sParam[1]);
            iCounter++;
            sProperty = applet.getParameter(sParamName + iCounter);
        }
        
    }
    
    public static String getSchemaProperty(String sProperty) {
        return (String) htSchemaProperty.get(sProperty);
    }

    public static String getTranslation(String sProperty) {
        return (String) htTranslation.get(sProperty);
    }

    public static String getTranslationWithSubstitution(String sProperty, Object[] args) {        
        String sTranslation = getTranslation(sProperty);
        String sTranslationFormated = MessageFormat.format(sTranslation, args);
        return sTranslationFormated;
    }
}
