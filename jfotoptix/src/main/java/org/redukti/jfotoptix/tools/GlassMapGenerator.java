package org.redukti.jfotoptix.tools;

import org.redukti.jfotoptix.importers.AGFImporter;

public class GlassMapGenerator {
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
    };

    public static void main(final String[] args) {
        String basePath = args[0];
        try {
            AGFImporter importer = new AGFImporter();
            for (Cat cat : catalogs) {
                System.out.println("// " + cat.make);

                var glasses = importer.parse_file(cat.make, basePath + "/" + cat.pathToAgf);
                for (var glass : glasses) {
                    System.out.println(glass.toCodeString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
