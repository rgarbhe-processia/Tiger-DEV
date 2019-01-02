package faurecia.util.cad;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import faurecia.util.DebugUtil;

/**
 * @author
 * 
 *         To change this generated comment edit the template variable "typecomment": Window>Preferences>Java>Templates. To enable and disable the creation of type comments go to Window>P
 *         erences>Java>Code Generation.
 */
public class HPGLMerger {

    public static File mergeHpgl(String sFile1, String sFile2, String sOffsetX, String sOffsetY, boolean bHPGL2) throws IOException {

        // String scriptFile = sScript + " -i "+ sFile1 +" -c "+ sFile2 + " -o "+ sFile1 +".plt -x "+ sOffsetX + " -y " + sOffsetY;
        // DebugUtil.debug (DebugUtil.DEBUG, "Internal execution of the script: " + scriptFile);
        try {
            DebugUtil.debug(DebugUtil.DEBUG, "IS hpgl 2: " + bHPGL2);
            String BEGINPLOTHPGL2 = "^[%-1BBPIN;PS260000,35200;NP 255;CR 0,255,0,255,0,255;SP1;PA ";
            String ENDPLOTHPGL2 = ";EC;PG;\n";
            String BEGINPLOTHPGL = "BP;IN;PA ";
            String ENDPLOTHPGL = "PG;\n";

            BufferedReader sFilePlot = new BufferedReader(new FileReader(sFile1));
            BufferedReader sFileTitleBlock = new BufferedReader(new FileReader(sFile2));

            File mergedFile = new File(sFile1 + ".plt");
            FileWriter sFileOut = new FileWriter(mergedFile);
            BufferedWriter sFileOutBW = new BufferedWriter(sFileOut);
            BigDecimal sOff_x = new BigDecimal("0");
            BigDecimal sOff_y = new BigDecimal("0");
            String lineout = "";
            String value = "";
            int flag_lb = 0;
            BigDecimal i = new BigDecimal("0");
            int termi = '';
            float sx = 0;
            float sy = 0;
            sOff_x = new BigDecimal("400");
            sOff_y = new BigDecimal("400");
            BigDecimal sXX = new BigDecimal(sOffsetX);
            BigDecimal sYY = new BigDecimal(sOffsetY);
            sOff_x = sOff_x.multiply(sXX);
            sOff_y = sOff_y.multiply(sYY);
            lineout = BEGINPLOTHPGL.concat(" ").concat(sOff_x.toString()).concat(",").concat(sOff_y.toString()).concat(";");
            sFileOutBW.write(lineout);

            int c = 0;
            while ((c = sFileTitleBlock.read()) != -1) {
                sFileOutBW.write(c);
                switch (c) {
                case 'P':
                    if (flag_lb == 0) {
                        c = sFileTitleBlock.read();
                        sFileOutBW.write(c);
                        if (c == 'A') {
                            value = "";
                            while (((c = sFileTitleBlock.read()) != -1) && (c != ',')) {
                                value = value + (char) c;
                            }
                            i = new BigDecimal(value);
                            i = i.add(sOff_x);
                            sFileOutBW.write(i.toString() + ",");
                            value = "";
                            while (((c = sFileTitleBlock.read()) != -1) && (c != ';'))
                                value = value + (char) c;
                            i = new BigDecimal(value);
                            i = i.add(sOff_y);
                            sFileOutBW.write(i.toString() + ";");
                        }
                    }
                    break;
                case 'L':
                    if (flag_lb == 0) {
                        c = sFileTitleBlock.read();
                        sFileOutBW.write(c);
                        if (c == 'B')
                            flag_lb = 1;
                    }
                    break;
                case '':
                    flag_lb = 0;
                    break;
                }
            }
            sFileOutBW.write("DF;");

            /*
             * Ajout d'un repositionnement a l'origine pour resoudre le probleme sur UniGraphics
             */
            sFileOutBW.write("PU;PA0,0;");
            if (bHPGL2) {
                sFileTitleBlock.close();
                sFilePlot.close();
                sFileOutBW.flush();
                sFileOut.flush();
                sFileOutBW.close();
                sFileOut.close();
                FileOutputStream writer = new FileOutputStream(sFile1 + ".plt", true);
                FileInputStream reader = new FileInputStream(sFile1);
                // skip the first 8 bytes of data (hpgl2 initialization string)
                if (reader.available() >= 8) {
                    for (int k = 0; k < 8; k++) {
                        reader.read();
                    }
                }
                while ((c = reader.read()) != -1) {
                    writer.write(c);
                    writer.flush();
                }
                writer.flush();
                writer.close();
                reader.close();
            } else {
                while ((c = sFilePlot.read()) != -1) {
                    switch (c) {
                    case 'd':
                    case 'D':
                        c = sFilePlot.read();
                        if (c != 'T' && c != 't') {
                            sFileOutBW.write("D" + (char) c);
                        } else {
                            sFileOutBW.write("DT");
                            termi = sFilePlot.read();
                            sFileOutBW.write(termi);
                        }
                    case 'l':
                    case 'L':
                        c = sFilePlot.read();
                        if (c != 'B' && c != 'b') {
                            sFileOutBW.write("L" + (char) c);
                        } else {
                            sFileOutBW.write("LB");
                            while ((c = sFilePlot.read()) != termi)
                                sFileOutBW.write(c);
                        }

                        break;
                    case 'e':
                    case 'E':
                        c = sFilePlot.read();
                        if (c != 'C' && c != 'c') {
                            sFileOutBW.write("E" + (char) c);
                        }
                        break;
                    case 'i':
                    case 'I':
                        c = sFilePlot.read();
                        if (c != 'N' && c != 'n')
                            sFileOutBW.write("I" + (char) c);
                        break;
                    case 'b':
                    case 'B':
                        c = sFilePlot.read();
                        if (c != 'P' && c != 'p')
                            sFileOutBW.write("B" + (char) c);
                        break;

                    case 's':
                    case 'S':
                        c = sFilePlot.read();
                        if (c != 'P' && c != 'p')
                            sFileOutBW.write("S" + (char) c);
                        else {
                            c = sFilePlot.read();
                            switch (c) {
                            case '0':
                                break;
                            default:
                                sFileOutBW.write("SP" + (char) c);
                                break;
                            }
                        }
                        break;

                    case 'n':
                    case 'N':
                        c = sFilePlot.read();
                        if (c != 'R' && c != 'r')
                            sFileOutBW.write("N" + (char) c);
                        break;

                    case 'p':
                    case 'P':
                        c = sFilePlot.read();
                        switch (c) {
                        case 's':
                        case 'S':
                            while (((sFilePlot.read()) != ';') && (c != -1))
                                ;
                            break;
                        case 'g':
                        case 'G':
                            break;
                        // SPRING prise en compte des ordres PE....;
                        case 'e':
                        case 'E':
                            sFileOutBW.write("P" + (char) c);
                            while ((c = sFilePlot.read()) != ';')
                                sFileOutBW.write(c);
                            sFileOutBW.write(c);
                            break;
                        // SPRING
                        default:
                            sFileOutBW.write("P" + (char) c);
                            break;
                        }
                        break;
                    case 'a':
                    case 'A':
                        c = sFilePlot.read();
                        switch (c) {
                        case 'h':
                        case 'H':
                            break;
                        case 'f':
                        case 'F':
                            break;
                        default:
                            sFileOutBW.write("A" + (char) c);
                            break;
                        }
                        break;
                    default:
                        sFileOutBW.write(c);
                    }
                }
                sFileOutBW.write(ENDPLOTHPGL);
                sFileTitleBlock.close();
                sFilePlot.close();
                sFileOutBW.flush();
                sFileOut.flush();
                sFileOutBW.close();
                sFileOut.close();
            }
            return mergedFile;
        } catch (IOException e) {
            throw new IOException("Error while merging title block : " + e.getMessage());
        }
    }

}
