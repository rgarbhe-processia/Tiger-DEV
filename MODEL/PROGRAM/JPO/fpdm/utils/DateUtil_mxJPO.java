package fpdm.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import com.matrixone.apps.domain.util.eMatrixDateFormat;

public class DateUtil_mxJPO {

    // default constructor
    public DateUtil_mxJPO() {
    }

    /**
     * Format date to be conform to the eMatrix Date format
     * @param sFormatedDate
     *            the locale formated date in
     * @param sOriginLocale
     *            the locale related to the formated date
     * @return the eMatrix Formated date
     * @throws Exception
     */
    public static String normalizeDate(String sFormatedDate, String sOriginLocale) throws Exception {
        // constructs the good locale
        String language = "";
        String country = "";
        Locale loc;
        if (sOriginLocale.indexOf('_') != -1) {
            language = sOriginLocale.substring(0, 2);
            country = sOriginLocale.substring(3, 5);
            loc = new Locale(language, country);
        } else {
            loc = new Locale(sOriginLocale);
        }

        SimpleDateFormat locFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.MEDIUM, loc);

        Date d = locFormat.parse(sFormatedDate);

        SimpleDateFormat dateMXFormat = new SimpleDateFormat(eMatrixDateFormat.strEMatrixDateFormat);

        return (dateMXFormat.format(d));
    }

    /**
     * Get the current date and format it for matrix use
     * @return
     */
    public static String getCurrentDateTimeMatrixFormat() {
        GregorianCalendar calendar = new GregorianCalendar();
        String matrixFormat = eMatrixDateFormat.getEMatrixDateFormat();
        SimpleDateFormat currentDateFormat = new SimpleDateFormat(matrixFormat);
        return currentDateFormat.format(calendar.getTime());
    }

}
