package logic.utility;
import org.rosuda.JRI.*;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;

public class Solver implements RMainLoopCallbacks {

    public void rWriteConsole(Rengine re, String text, int oType) {
        System.out.print(text);
    }

    public void rBusy(Rengine re, int which) {
        System.out.println("rBusy(" + which + ")");
    }

    public String rReadConsole(Rengine re, String prompt, int addToHistory) {
        System.out.print(prompt);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String s = br.readLine();
            return (s == null || s.length() == 0) ? s : s + "\n";
        } catch (Exception e) {
            System.out.println("jriReadConsole exception: " + e.getMessage());
        }
        return null;
    }

    public void rShowMessage(Rengine re, String message) {
        System.out.println("rShowMessage \"" + message + "\"");
    }

    public String rChooseFile(Rengine re, int newFile) {
        FileDialog fd = new FileDialog(new Frame(), (newFile == 0) ? "Select a file" : "Select a new file", (newFile == 0) ? FileDialog.LOAD : FileDialog.SAVE);
        fd.show();
        String res = null;
        if (fd.getDirectory() != null) res = fd.getDirectory();
        if (fd.getFile() != null) res = (res == null) ? fd.getFile() : (res + fd.getFile());
        return res;
    }

    public void rFlushConsole(Rengine re) {
    }

    public void rLoadHistory(Rengine re, String filename) {
    }

    public void rSaveHistory(Rengine re, String filename) {
    }


    public static void main(String[] args) {
        // just making sure we have the right version of everything
//        if (!Rengine.versionCheck()) {
//            System.err.println("** Version mismatch - Java files don't match library version.");
//            System.exit(1);
//        }
    	
    	Rengine.DEBUG=5;
    	System.out.println(Rengine.getVersion());
    	System.out.println(Rengine.versionCheck());
    	System.out.println(Rengine.getMainEngine());
        Rengine re = SettingsProvider.getRe();
        
        System.out.println(Rengine.getMainEngine());
        re.eval("library(rms)");
        System.out.println("Creating Rengine (with arguments)");
        // 1) we pass the arguments from the command line
        // 2) we won't use the main loop at first, we'll start it later
        //    (that's the "false" as second argument)
        // 3) the callbacks are implemented by the TextConsole class above
        
       
       // Rengine re = new Rengine(args, false, new Solver());
        System.out.println("Rengine created, waiting for R");
        // the engine creates R is a new thread, so we should wait until it's ready
        if (!re.waitForR()) {
            System.out.println("Cannot load R");
            return;
        }


        re.eval("mat <- matrix(c(3, 4, 2," +
                "                2, 1, 2," +
                "                1, 3, 2), nrow=3, byrow=TRUE)");
        //re.eval("print(mat)");
        re.eval("lp <- OP(objective = c(2, 4, 3)," +
                "         constraints = L_constraint(L = mat," +
                "                                    dir = c(\"<=\", \"<=\", \"<=\")," +
                "                                    rhs = c(60, 40, 80))," +
                "         maximum = TRUE)");

        re.eval("ROI_applicable_solvers(lp)");
        re.eval("sol <- ROI_solve(lp, solver = \"symphony\")");
        re.eval("print(sol)");
    }
}