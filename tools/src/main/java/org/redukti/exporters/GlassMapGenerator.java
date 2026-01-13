package org.redukti.exporters;

import org.redukti.importers.agf.AGFBase;
import org.redukti.importers.agf.AGFImporter;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.medium.GlassMap;
import org.redukti.mathlib.M;

import java.text.DecimalFormat;

public class GlassMapGenerator {

    public static DecimalFormat decimalFormat = M.decimal_format(5);
    public static DecimalFormat decimalFormat2 = M.decimal_format(2);

    static final class Cat {
        String make;
        String pathToAgf;

        public Cat(String make, String pathToAgf) {
            this.make = make;
            this.pathToAgf = pathToAgf;
        }
    }

    public static Cat[] catalogs = new Cat[] {
            new Cat("Hikari", "glassdata/NIKON-HIKARI20220701_MD_BD_added_HG.agf"),
            new Cat("Hoya", "glassdata/HOYA20250623_include_obsolete.agf"),
            new Cat("Schott", "glassdata/SCHOTT.AGF"),
            new Cat("Ohara", "glassdata/OHARA.agf"),
            new Cat("CDGM", "glassdata/CDGM.AGF"),
    };

//    public static GlassMap toGlassMap(AGFBase agf) {
//        return new GlassMap(
//                agf._manufacturer,
//                agf._name,
//                agf.get_refractive_index(SpectralLine.d),
//                agf.get_refractive_index(SpectralLine.C),
//                agf.get_refractive_index(SpectralLine.F),
//                agf.get_refractive_index(SpectralLine.e),
//                agf.get_refractive_index(SpectralLine.C_),
//                agf.get_refractive_index(SpectralLine.F_),
//                agf.get_abbe_vd(),
//                agf.get_abbe_ve(),
//                0.0);
//    }

    public static String toCodeString(AGFBase g) {
        StringBuilder sb = new StringBuilder();
        sb.append("glasses.put(\"").append(g._name).append("\", new GlassMap(\"");
        sb.append(g._manufacturer).append("\", \"").append(g._name).append("\", ");
        sb.append(decimalFormat.format(g.get_refractive_index(SpectralLine.d))).append(", ");
        sb.append(decimalFormat.format(g.get_refractive_index(SpectralLine.C))).append(", ");
        sb.append(decimalFormat.format(g.get_refractive_index(SpectralLine.F))).append(", ");
        sb.append(decimalFormat.format(g.get_refractive_index(SpectralLine.e))).append(", ");
        sb.append(decimalFormat.format(g.get_refractive_index(SpectralLine.C_))).append(", ");
        sb.append(decimalFormat.format(g.get_refractive_index(SpectralLine.F_))).append(", ");
        sb.append(decimalFormat.format(g.get_refractive_index(SpectralLine.g))).append(", ");
        sb.append(decimalFormat2.format(g.get_abbe_vd())).append(", ");
        sb.append(decimalFormat2.format(g.get_abbe_ve())).append(", ");
        sb.append("0.0));");
        return sb.toString();
    }


    public static void main(final String[] args) {
        String basePath = args[0];
        try {
            AGFImporter importer = new AGFImporter();
            for (Cat cat : catalogs) {
                System.out.println("// " + cat.make);

                var glasses = importer.parse_file(cat.make, basePath + "/" + cat.pathToAgf);
                for (var glass : glasses) {
                    System.out.println(toCodeString(glass));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
