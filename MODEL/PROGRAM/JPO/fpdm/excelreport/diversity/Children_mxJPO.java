package fpdm.excelreport.diversity;

public class Children_mxJPO {
    public String id = "";

    public String quantity = "1";

    public String fn = "";

    public String uom = "";

    public String toString() {
        return (quantity + " " + uom + " of " + id + " (" + fn + ")");
    }
}
