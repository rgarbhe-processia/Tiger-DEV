package pss.common.utils;

import com.matrixone.apps.domain.DomainConstants;

import pss.constants.TigerConstants;

public class TigerEnums {

	public enum MessageType{
		ERROR_MESSAGE("error"), NOTICE_MESSAGE("notice"), WARNING_MESSAGE("warning");
		
		private final String objectType;
		
		private MessageType(String type){
			this.objectType = type;
		}
		
		public final String objectType() {
            return this.objectType;
        }

        public final String toString() {
            return this.objectType;
        }
	}
	
	public enum FileFormat {
        XML(), PDF(), GENERIC(), THUMBNAIL(), PNG();

        public static String XML() {
            return "XML";
        }
        
        public static String PDF() {
            return "PDF";
        }
        
        public static String GENERIC() {
            return "generic";
        }
        
        public static String THUMBNAIL() {
            return "THUMBNAIL";
        }
        
        public static String PNG() {
            return "PNG";
        }
	}
	
	public enum FileExtension {
        XML(), TEXT(), PDF(), LST(), CAD();

        public static String XML() {
            return ".xml";
        }
        
        public static String TEXT() {
            return ".txt";
        }
        
        public static String PDF() {
            return ".pdf";
        }
        
        public static String LST() {
            return ".lst";
        }
        
        public static String CAD() {
            return ".cad";
        }
	}
	
	public enum Font {
	    ARIAL("arial.ttf"), CALIBRI("calibri.ttf"), MONOSPAC821_BT("Monos.ttf"), TIMES_NEW_ROMAN("times.ttf"), VERDANA("verdana.ttf");
	    private final String strFontName;
        
        private Font(String fontName){
            this.strFontName = fontName;
        }
        
        public final String toString() {
            return this.strFontName;
        }
        
        public final String getFontName() {
            return this.strFontName;
        }
    }
	
	public enum TitleBlockPattern {
	    FASFAE("FAS FAE"), BASISDEFINITION("Basis Definition"), RENAULTNISSAN("Renault Nissan"), FCM("FCM");
	    private final String PATTERN;
        
        private TitleBlockPattern(String patternName){
            this.PATTERN = patternName;
        }
        
        public final String getPattern(){
            return  this.PATTERN;
        }
        
        public final String toString(){
            return this.PATTERN;
        }
        
        public static boolean isValidPattern(String pattern) {
            for (TitleBlockPattern enumPattern : TitleBlockPattern.values()) {
                if (pattern.equalsIgnoreCase(enumPattern.toString()) || pattern.replaceAll(TigerConstants.STRING_SINGLE_SPACE, DomainConstants.EMPTY_STRING)
                                                                                        .toUpperCase().equalsIgnoreCase(enumPattern.toString())) {
                    return true;
                }
            }
            return false;
        }
        
        public static TitleBlockPattern getPattern(String pattern) {
            for (TitleBlockPattern enumPattern : TitleBlockPattern.values()) {
                if (pattern.equalsIgnoreCase(enumPattern.toString()) || pattern.replaceAll(TigerConstants.STRING_SINGLE_SPACE, DomainConstants.EMPTY_STRING)
                                                                                        .toUpperCase().equalsIgnoreCase(enumPattern.toString())) {
                    return enumPattern;
                }
            }
            throw new IllegalArgumentException("Invalid Pattern!!!");
        }
	}
}
