package fpdm.excelreport;

public class ImageReport_mxJPO {
    public String format = "";

    public String fileName = "";

    public String id = "";

    public String toString() {
        return "Image " + this.format + " : (" + this.id + ") " + this.fileName;
    }
}
