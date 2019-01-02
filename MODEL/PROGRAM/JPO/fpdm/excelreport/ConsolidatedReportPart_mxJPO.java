package fpdm.excelreport;

import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsolidatedReportPart_mxJPO implements fpdm.excelreport.Consolidated_mxJPO {
    private static Logger logger = LoggerFactory.getLogger("fpdm.excelreport.ConsolidatedReportPart_mxJPO");

    public int level = -1;

    public String name = "";

    public float quantity = 1f;

    @Override
    public String getLevel() {
        if (this.level == -1) {
            return "";
        } else {
            return String.valueOf(this.level);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getQuantity() {
        if (this.level == -1) {
            return "";
        } else {
            return String.valueOf(this.quantity);
        }
    }

    public boolean isPresent(List<? extends fpdm.excelreport.ConsolidatedReportPart_mxJPO> lRP) {
        logger.debug("<ExcelReport> size of lRP : " + lRP.size());
        logger.debug("<ExcelReport> " + this.getName() + " is present ?");
        if (lRP.size() <= 1) {
            return false;
        }

        int iThisLevel = -1;
        try {
            iThisLevel = this.level;
        } catch (NumberFormatException e) {
            logger.debug("System can't convert this level to int : " + this.getLevel());
            return false;
        }

        ListIterator<? extends fpdm.excelreport.ConsolidatedReportPart_mxJPO> iterator = lRP.listIterator(lRP.size());

        while (iterator.hasPrevious()) {
            ConsolidatedReportPart_mxJPO rp = iterator.previous();
            int iRPLevel = -1;
            try {
                iRPLevel = rp.level;

                logger.debug("<ExcelReport> Levels : " + iThisLevel + " -> " + iRPLevel);
                if (-1 != iRPLevel) {
                    if (0 == iRPLevel) {
                        return false;
                    }

                    if (iThisLevel == iRPLevel) {
                        logger.debug("<ExcelReport> Names : " + this.name + " -> " + rp.name);
                        if (this.name.equals(rp.name)) {
                            float thisQuantity = this.quantity;
                            float rpQuantity = rp.quantity;
                            float newQuantity = thisQuantity + rpQuantity;

                            rp.quantity = newQuantity;

                            return true;
                        }
                    } else if (iThisLevel > iRPLevel) {
                        return false;
                    }
                }
            } catch (NumberFormatException e) {
                logger.debug("<ExcelReport> System can't convert this level to int : " + rp.getLevel());
            }
        }

        return false;
    }
}
