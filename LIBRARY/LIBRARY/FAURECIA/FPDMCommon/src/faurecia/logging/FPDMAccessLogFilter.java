package faurecia.logging;

import java.io.IOException;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//import matrix.db.Context;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

//import com.matrixone.servlet.Framework;

/**
 * Logs access times.
 * 
 * This class contains code from Apache Tomcat. That code is subject to the following licence : Copyright 1999-2001,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * @author lebasn
 * 
 */
public class FPDMAccessLogFilter implements Filter {
    private String loggerName = FPDMAccessLogFilter.class.getName();

    private String pattern = "";

    private Logger logger = null;

    private Level level = Level.INFO;

    private static FPDMAccessLogFilter instance = null;

    /**
     * The set of month abbreviations for log messages.
     */
    private static final String months[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };

    /**
     * A date formatter to format a Date into a date in the format "yyyy-MM-dd".
     */
    private SimpleDateFormat dateFormatter = null;

    /**
     * A date formatter to format Dates into a day string in the format "dd".
     */
    private SimpleDateFormat dayFormatter = null;

    /**
     * A date formatter to format a Date into a month string in the format "MM".
     */
    private SimpleDateFormat monthFormatter = null;

    /**
     * Time taken formatter for 3 decimal places.
     */
    private DecimalFormat timeTakenFormatter = null;

    /**
     * A date formatter to format a Date into a year string in the format "yyyy".
     */
    private SimpleDateFormat yearFormatter = null;

    /**
     * A date formatter to format a Date into a time in the format "kk:mm:ss" (kk is a 24-hour representation of the hour).
     */
    private SimpleDateFormat timeFormatter = null;

    /**
     * The time zone relative to GMT.
     */
    private String timeZone = null;

    /**
     * The system time when we last updated the Date that this valve uses for log lines.
     */
    private Date currentDate = null;

    /**
     * When formatting log lines, we often use strings like this one (" ").
     */
    private String space = " ";

    public void init(FilterConfig config) throws ServletException {
        String lg = config.getInitParameter("loggerName");
        if (lg != null) {
            loggerName = lg;
        }
        logger = Logger.getLogger(loggerName);

        String p = config.getInitParameter("pattern");
        if (p != null) {
            pattern = p;
        }

        String l = config.getInitParameter("level");
        if (l != null) {
            level = Level.toLevel(l);
        }

        // Initialize the timeZone, Date formatters, and currentDate
        TimeZone tz = TimeZone.getDefault();
        timeZone = calculateTimeZoneOffset(tz.getRawOffset());

        dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        dateFormatter.setTimeZone(tz);
        dayFormatter = new SimpleDateFormat("dd");
        dayFormatter.setTimeZone(tz);
        monthFormatter = new SimpleDateFormat("MM");
        monthFormatter.setTimeZone(tz);
        yearFormatter = new SimpleDateFormat("yyyy");
        yearFormatter.setTimeZone(tz);
        timeFormatter = new SimpleDateFormat("HH:mm:ss");
        timeFormatter.setTimeZone(tz);
        currentDate = new Date();
        timeTakenFormatter = new DecimalFormat("0.000");
        instance = this;
    }

    /**
     * Log a message summarizing the specified request and response, according to the format specified by the <code>pattern</code> property.
     * 
     * @param request
     *            Request being processed
     * @param response
     *            Response being processed
     * @param context
     *            The valve context used to invoke the next valve in the current processing pipeline
     * 
     * @exception IOException
     *                if an input/output error has occurred
     * @exception ServletException
     *                if a servlet error has occurred
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        // Pass this request on to the next filter in our pipeline
        long t1 = System.currentTimeMillis();

        filterChain.doFilter(request, response);

        long t2 = System.currentTimeMillis();
        long time = t2 - t1;
        log(request, response, time);
    }

    public void destroy() {
        instance = null;
    }

    /**
     * Return the month abbreviation for the specified month, which must be a two-digit String.
     * 
     * @param month
     *            Month number ("01" .. "12").
     */
    private String lookup(String month) {

        int index;
        try {
            index = Integer.parseInt(month) - 1;
        } catch (Throwable t) {
            index = 0; // Can not happen, in theory
        }
        return (months[index]);

    }

    /**
     * Return the replacement text for the specified pattern character.
     * 
     * @param pattern
     *            Pattern character identifying the desired text
     * @param date
     *            the current Date so that this method doesn't need to create one
     * @param request
     *            Request being processed
     * @param response
     *            Response being processed
     */
    private String replace(char pattern, Date date, ServletRequest req, ServletResponse res, long time) {

        String value = null;

        HttpServletRequest hreq = null;
        if (req instanceof HttpServletRequest)
            hreq = (HttpServletRequest) req;

        if (pattern == 'a') {
            value = req.getRemoteAddr();
        } else if (pattern == 'A') {
            try {
                value = InetAddress.getLocalHost().getHostAddress();
            } catch (Throwable e) {
                value = "127.0.0.1";
            }
        } else if (pattern == 'b') {
            value = "-";
        } else if (pattern == 'h') {
            value = req.getRemoteHost();
        } else if (pattern == 'H') {
            value = req.getProtocol();
        } else if (pattern == 'l') {
            value = "-";
        } else if (pattern == 'm') {
            if (hreq != null)
                value = hreq.getMethod();
            else
                value = "";
        } else if (pattern == 'p') {
            value = "" + req.getServerPort();
        } else if (pattern == 'D') {
            value = "" + time;
        } else if (pattern == 'q') {
            String query = null;
            if (hreq != null)
                query = hreq.getQueryString();
            if (query != null)
                value = "?" + query;
            else
                value = "";
        } else if (pattern == 'r') {
            StringBuffer sb = new StringBuffer();
            if (hreq != null) {
                sb.append(hreq.getMethod());
                sb.append(space);
                sb.append(hreq.getRequestURI());
                if (hreq.getQueryString() != null) {
                    sb.append('?');
                    sb.append(hreq.getQueryString());
                }
                sb.append(space);
                sb.append(hreq.getProtocol());
            } else {
                sb.append("- - ");
                sb.append(req.getProtocol());
            }
            value = sb.toString();
        } else if (pattern == 'S') {
            if (hreq != null)
                if (hreq.getSession(false) != null)
                    value = hreq.getSession(false).getId();
                else
                    value = "-";
            else
                value = "-";
        } else if (pattern == 's') {
            value = "-";
        } else if (pattern == 't') {
            StringBuffer temp = new StringBuffer("[");
            temp.append(dayFormatter.format(date)); // Day
            temp.append('/');
            temp.append(lookup(monthFormatter.format(date))); // Month
            temp.append('/');
            temp.append(yearFormatter.format(date)); // Year
            temp.append(':');
            temp.append(timeFormatter.format(date)); // Time
            temp.append(' ');
            temp.append(timeZone); // Timezone
            temp.append(']');
            value = temp.toString();
        } else if (pattern == 'T') {
            value = timeTakenFormatter.format(time / 1000d);
        } else if (pattern == 'u') {
//            if (hreq != null) {
//                HttpSession session = hreq.getSession(false);
//                if (session != null) {
//                    Context context = Framework.getContext(session);
//                    if (context != null) {
//                        value = context.getUser();
//                    }
//                }
//            }
            if (value == null)
                value = "-";
        } else if (pattern == 'U') {
            if (hreq != null)
                value = hreq.getRequestURI();
            else
                value = "-";
        } else if (pattern == 'v') {
            value = req.getServerName();
        } else {
            value = "???" + pattern + "???";
        }

        if (value == null) {
            return ("");
        }
        return (value);

    }

    /**
     * Return the replacement text for the specified "header/parameter".
     * 
     * @param header
     *            The header/parameter to get
     * @param type
     *            Where to get it from i=input,c=cookie,r=ServletRequest,s=Session
     * @param request
     *            Request being processed
     * @param response
     *            Response being processed
     */
    private String replace(String header, char type, ServletRequest req, ServletResponse response) {

        Object value = null;

        HttpServletRequest hreq = null;
        if (req instanceof HttpServletRequest)
            hreq = (HttpServletRequest) req;

        switch (type) {
        case 'i':
            if (null != hreq)
                value = hreq.getHeader(header);
            else
                value = "??";
            break;
        /*
         * // Someone please make me work case 'o': break;
         */
        case 'c':
            Cookie[] c = hreq.getCookies();
            for (int i = 0; c != null && i < c.length; i++) {
                if (header.equals(c[i].getName())) {
                    value = c[i].getValue();
                    break;
                }
            }
            break;
        case 'r':
            if (null != hreq)
                value = hreq.getAttribute(header);
            else
                value = "??";
            break;
        case 's':
            if (null != hreq) {
                HttpSession sess = hreq.getSession(false);
                if (null != sess)
                    value = sess.getAttribute(header);
            }
            break;
        default:
            value = "???";
        }

        /* try catch in case toString() barfs */
        try {
            if (value != null) {
                if (value instanceof String) {
                    return (String) value;
                }
                return value.toString();
            }
            return "-";
        } catch (Throwable e) {
            return "-";
        }
    }

    /**
     * This method returns a Date object that is accurate to within one second. If a thread calls this method to get a Date and it's been less than 1 second since a new Date was created, this method
     * simply gives out the same Date again so that the system doesn't spend time creating Date objects unnecessarily.
     */
    private synchronized Date getDate() {

        // Only create a new Date once per second, max.
        long systime = System.currentTimeMillis();
        if ((systime - currentDate.getTime()) > 1000) {
            currentDate = new Date(systime);
        }

        return currentDate;

    }

    private String calculateTimeZoneOffset(long offset) {
        StringBuffer tz = new StringBuffer();
        if ((offset < 0)) {
            tz.append("-");
            offset = -offset;
        } else {
            tz.append("+");
        }

        long hourOffset = offset / (1000 * 60 * 60);
        long minuteOffset = (offset / (1000 * 60)) % 60;

        if (hourOffset < 10)
            tz.append("0");
        tz.append(hourOffset);

        if (minuteOffset < 10)
            tz.append("0");
        tz.append(minuteOffset);

        return tz.toString();
    }

    public void log(ServletRequest request, ServletResponse response, long time) {
        if (logger.isEnabledFor(level)) {
            Date date = getDate();
            StringBuffer result = new StringBuffer();

            // Generate a message based on the defined pattern
            boolean replace = false;
            for (int i = 0; i < pattern.length(); i++) {
                char ch = pattern.charAt(i);
                if (replace) {
                    /*
                     * For code that processes {, the behavior will be ... if I do not enounter a closing } - then I ignore the {
                     */
                    if ('{' == ch) {
                        StringBuffer name = new StringBuffer();
                        int j = i + 1;
                        for (; j < pattern.length() && '}' != pattern.charAt(j); j++) {
                            name.append(pattern.charAt(j));
                        }
                        if (j + 1 < pattern.length()) {
                            /* the +1 was to account for } which we increment now */
                            j++;
                            result.append(replace(name.toString(), pattern.charAt(j), request, response));
                            i = j; /* Since we walked more than one character */
                        } else {
                            // D'oh - end of string - pretend we never did this
                            // and do processing the "old way"
                            result.append(replace(ch, date, request, response, time));
                        }
                    } else {
                        result.append(replace(ch, date, request, response, time));
                    }
                    replace = false;
                } else if (ch == '%') {
                    replace = true;
                } else {
                    result.append(ch);
                }
            }
            logger.log(level, result.toString());
        }
    }

    public static FPDMAccessLogFilter getInstance() {
        return instance;
    }
}
