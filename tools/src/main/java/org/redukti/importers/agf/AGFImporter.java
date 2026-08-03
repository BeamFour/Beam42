package org.redukti.importers.agf;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AGFImporter {

    String[] splitLine(String line) {
        return line.trim().split("\\s+");
    }

    static double parseDouble(String s) {
        if (s == null || s.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public List<AGFBase> parse_file(String make, String file_name) throws Exception {
        String currentName = null;
        AGFBase currentGlass = null;
        List<AGFBase> glasses = new ArrayList<>();
        int dispersionFormula = 0;
        String code = null;
        String nd = null;
        String vd = null;
        String dpgf = null;
        String relative_cost = null;
        var lines = Files.readAllLines(new File(file_name).toPath());
        for (var line : lines) {
            if (line.isEmpty() || line.startsWith("!")) continue;
            var words = splitLine(line);
            if (words[0].equals("NM")) {
                /*
                NM <glass name> <dispersion formula #> <MIL#> <N(d)> <V(d)> <Exclude Sub> <status> <melt freq>
                   <glass name> is the name of the material.
                   The dispersion formula number is
                    1 for Schott,
                    2 for Sellmeier 1,
                    3 for Herzberger,
                    4 for Sellmeier 2,
                    5 for Conrady,
                    6 for Sellmeier 3,
                    7 for Handbook of Optics 1,
                    8 for Handbook of Optics 2,
                    9 for Sellmeier 4,
                    10 for Extended,
                    11 for Sellmeier 5,
                    12 for Extended 2
                    13 for Extended 3.
                The MIL# is provided for back compatibility and is not used, but a placeholder value must be provided.
                The nd and vd values are also provided for reference but are not used.
                The "exclude sub" flag is 0 for no and 1 for yes.
                Status is 0 for Standard, 1 for Preferred, 2 for Obsolete, 3 for Special, and 4 for Melt.
                Melt Freq is an integer between 1 and 5 to indicate the relative frequency of melting by the manufacturer.
                 */
                if (currentGlass != null && currentName != null) {
                    if (dpgf != null) {
                        double v = parseDouble(dpgf);
                        if (v != 0.0) currentGlass.set_dgpF(v);
                    }
                    if (relative_cost != null) {
                        double v = parseDouble(relative_cost);
                        if (v != 0.0) currentGlass.set_relative_cost(v);
                    }
                    glasses.add(currentGlass);
                }
                currentGlass = null;
                currentName = words[1];
                dispersionFormula = (int) parseDouble(words[2]);
                code = words[3];
                nd = words[4];
                vd = words[5];
                dpgf = null;
                relative_cost = null;
            } else if (words[0].equals("ED")) {
                dpgf = words[4];
            } else if (words[0].equals("OD")) {
                relative_cost = words[1];
            } else if (words[0].equals("CD")) {
                // coefficient data - up to 10
                double[] coefs = new double[words.length-1];
                for (int i = 0; i < coefs.length; i++) {
                    coefs[i] = parseDouble(words[i+1]);
                }
                if (dispersionFormula == 1)
                    currentGlass = new SchottFormula(make,currentName,coefs);
                else if (dispersionFormula == 12)
                    currentGlass = new Extended2Formula(make,currentName,coefs);
                else if (dispersionFormula == 13)
                    currentGlass = new Extended3Formula(make,currentName,coefs);
                else if (dispersionFormula == 2)
                    currentGlass = new Sellmeier1Formula(make,currentName,coefs);
                else if (dispersionFormula == 3)
                    currentGlass = null; // new HerzbergerFormula(make,currentName,coefs);
                else if (dispersionFormula == 6)
                    currentGlass = new Sellmeier3Formula(make,currentName,coefs);
                else
                    System.err.println("Unsupported dispersion formula " + dispersionFormula + " in glass " + currentName);
            }
        }
        if (currentGlass != null && currentName != null) {
            if (dpgf != null) {
                double v = parseDouble(dpgf);
                if (v != 0.0) currentGlass.set_dgpF(v);
            }
            if (relative_cost != null) {
                double v = parseDouble(relative_cost);
                if (v != 0.0) currentGlass.set_relative_cost(v);
            }
            glasses.add(currentGlass);
        }
        return glasses;
    }
}
