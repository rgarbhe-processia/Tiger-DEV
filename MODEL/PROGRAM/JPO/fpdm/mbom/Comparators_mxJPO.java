package fpdm.mbom;

import java.io.Serializable;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Comparators_mxJPO {

    public static class MapComparator implements Comparator<Object>, Serializable {

        private static final long serialVersionUID = 1L;

        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a", Locale.US);

        private List<?> keys = null;

        public MapComparator(List<?> paramList) {
            this.keys = paramList;
        }

        public MapComparator(String name, String direction, String type) {
            Hashtable<String, String> localHashMap = new Hashtable<String, String>();
            localHashMap.put("name", name);
            localHashMap.put("dir", direction);
            localHashMap.put("type", type);
            ArrayList<Hashtable<String, String>> htKeys = new ArrayList<Hashtable<String, String>>();
            htKeys.add(localHashMap);
            this.keys = htKeys;
        }

        public int compare(Object paramObject1, Object paramObject2) {
            int i = 0;
            int j = 1;
            Map<?, ?> localMap1 = null;
            String str1 = null;
            String str2 = null;
            String str3 = null;
            String str4 = null;
            String str5 = null;
            Long localLong1 = null;
            Long localLong2 = null;
            Double localDouble1 = null;
            Double localDouble2 = null;
            Date localDate1 = null;
            Date localDate2 = null;
            Map<?, ?> localMap2 = (Map<?, ?>) paramObject1;
            Map<?, ?> localMap3 = (Map<?, ?>) paramObject2;
            Iterator<?> localIterator = this.keys.iterator();
            do {
                if (!(localIterator.hasNext()))
                    break;
                localMap1 = (Map<?, ?>) localIterator.next();
                str1 = (String) localMap1.get("name");
                str2 = (String) localMap1.get("dir");
                str3 = (String) localMap1.get("type");
                if (!("ascending".equals(str2)))
                    j = -1;
                else
                    j = 1;
                str4 = (String) localMap2.get(str1);
                str5 = (String) localMap3.get(str1);
                if ((((str4 == null) || (str4.equals("")))) && (((str5 == null) || (str5.equals(""))))) {
                    i = 0;
                } else if ((str4 == null) || (str4.equals(""))) {
                    i = -1;
                } else if ((str5 == null) || (str5.equals(""))) {
                    i = 1;
                } else if ("string".equals(str3)) {
                    i = str4.compareToIgnoreCase(str5);
                } else if ("stringpad".equals(str3)) {
                    i = str4.length() - str5.length();
                    if (i != 0)
                        continue;
                    i = str4.compareToIgnoreCase(str5);
                } else if ("integer".equals(str3)) {
                    try {
                        localLong1 = Long.valueOf(str4);
                        localLong2 = Long.valueOf(str5);
                        i = localLong1.compareTo(localLong2);
                    } catch (NumberFormatException localNumberFormatException1) {
                        i = str4.compareToIgnoreCase(str5);
                    }
                } else if ("real".equals(str3)) {
                    try {
                        localDouble1 = Double.valueOf(str4);
                        localDouble2 = Double.valueOf(str5);
                        i = localDouble1.compareTo(localDouble2);
                    } catch (NumberFormatException localNumberFormatException2) {
                        i = str4.compareToIgnoreCase(str5);
                    }
                } else {
                    if (!("date".equals(str3)))
                        continue;
                    localDate1 = DATE_FORMAT.parse(str4, new ParsePosition(0));
                    localDate2 = DATE_FORMAT.parse(str5, new ParsePosition(0));
                    i = localDate1.compareTo(localDate2);
                }
            } while (i == 0);
            return (i * j);
        }
    }

}
