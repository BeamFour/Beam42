package org.redukti.rayoptics.layout;

import org.junit.jupiter.api.Test;
import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

import java.nio.file.Files;
import java.nio.file.Path;

public class Layout2DIntegrationTest {

    static OpticalBenchDataImporter.LensSpecifications getSpecsFromFile(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return specs;
    }
    static Prescription createPrescription(OpticalBenchDataImporter.LensSpecifications specs, boolean use_glass_types, boolean d_line) {
        var wvls = d_line ? new double[] {587.5618} : new double[] {587.5618, 486.1327, 656.2725};
        var wts = d_line ? new double[] {1.0} : new double[] {1.0, 1.0, 1.0};
        return Prescription.build_prescription(specs,use_glass_types,wvls,wts,0);
    }
    static OpticalModel createFromSpecfile(String specfile) throws Exception {
        var specs =  getSpecsFromFile(specfile);
        var prescription = createPrescription(specs,true,false);
        final double[] fields = {0.0, 1.0};
        return new RayOpticsModelBuilder(prescription).build_optical_model(true,fields,false, VigType.SetPupil,true,0);
    }
    static void doLayout(OpticalModel model, String testname) throws Exception {
        var osp = model.optical_spec;
        Layout2D layout = new Layout2D();
        Path output = Path.of("target", "layout-tests");
        Files.createDirectories(output);

        String elements = layout.renderSvg(model, 1000, 500,
                new LayoutOptions().drawReferenceRays(false));
        String reference = layout.renderSvg(model, 1000, 500, new LayoutOptions());
        String fan = layout.renderSvg(model, 1000, 500,
                new LayoutOptions().drawReferenceRays(false).fanRayCount(9).clipRays(true));
        String traceFan = layout.renderSvg(model, 1000, 500,
                new LayoutOptions().drawReferenceRays(false).fanRayCount(9).clipRays(true).useTraceFan(true));

        Files.writeString(output.resolve(testname + "-elements.svg"), elements);
        Files.writeString(output.resolve(testname + "-reference-rays.svg"), reference);
        Files.writeString(output.resolve(testname + "-ray-fan.svg"), fan);
        Files.writeString(output.resolve(testname + "-trace-fan.svg"), traceFan);
        Files.writeString(output.resolve(testname + "-vig.txt"), osp.list_str(new StringBuilder()).toString());
    }

    @Test
    public void testLeica35mmNoctilux() throws Exception {
        OpticalModel model = createFromSpecfile("../Examples/jfotoptix/leica-m-35mm-f1.2/prescription.txt");
        doLayout(model,"leica-ex1");
    }
}
