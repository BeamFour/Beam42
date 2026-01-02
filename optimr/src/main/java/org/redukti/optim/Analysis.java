package org.redukti.optim;


import org.redukti.rayoptics.analysis.*;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.Prescription;

import java.util.ArrayList;

public class Analysis {

    public Prescription prescription;
    public double[] fields;
    public double[] pfo;
    public SpotAnalysisResult.SpotResultsByField[] spots;
    public RayAberrationResult ray_aberrations;
    public MTFResultByFreq[] mtfs;
    public int[] freqs;

    /**
     * Systems and spot analysis setup for each field
     * Always has at least 1 field, and first field is always 0.0
     * i.e. horizontal rays
     * Additional fields can be added by constructor
     *
     * By default, the system[0] system is for parallel rays (field 0)
     * but if being used to find rays then the angle of view is variable
     * and set by the optimizer
     */
    public OpticalModel system;

    public Analysis(Prescription prescription, double[] fields, int[] freqs) {
        this.prescription = prescription;
        this.fields = fields;
        this.freqs = freqs;
    }
    public void compute() {
        system = prescription.build_ray_optics_model(true,fields,true,true);
        var spotAnalysis = SpotAnalysis.eval(system,new SpotOptions().num_rays(64).use_grid(false));
        spots = spotAnalysis.spot_results.toArray(new SpotAnalysisResult.SpotResultsByField[0]);
        pfo = ParaxHelper.asArray(system.optical_spec.parax_data.fod);
        ray_aberrations = TransverseRayAberrationAnalysis.eval(system,10,new TraceOptions());
        computeMTFs(spotAnalysis);
    }
    private void computeMTFs(SpotAnalysisResult spotAnalysis) {
        var mtfs = new ArrayList<PolyMTF>();
        for (int i = 0; i < spotAnalysis.spot_results.size(); i++) {
            var spotFld = spotAnalysis.spot_results.get(i);
            PolyMTF polyMtfForField = null;
            for (var intercepts: spotFld.intercepts) {
                var mtf = new MonochromaticGeometricMTF(intercepts);
                if (polyMtfForField == null)
                    polyMtfForField = new PolyMTF(mtf.mtf.fft_size,mtf.h2d.pixel_size);
                polyMtfForField.add(mtf.mtf, intercepts.wvl == 587.5618 ? 1.0: 0.5);
            }
            if (polyMtfForField != null) {
                polyMtfForField.compute();
                mtfs.add(polyMtfForField);
            }
        }
        var mtfResults = new ArrayList<MTFResultByFreq>();
        for (var freq: freqs)
            mtfResults.add(new MTFResultByFreq(mtfs,freq));
        this.mtfs = mtfResults.toArray(new MTFResultByFreq[0]);
    }
}
