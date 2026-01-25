package org.redukti.optim;

import org.redukti.rayoptics.analysis.*;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.Prescription;
import org.redukti.spec.VigType;

public class Analysis {

    public Prescription prescription;
    public double[] fields;
    public double[] pfo;
    public SpotAnalysisResult.SpotResultsForField[] spots;
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
        // TODO support scenarios
        system = prescription.build_ray_optics_model(true,fields,false, VigType.SetPupil, true,0);
        var spotAnalysis = SpotAnalysis.eval(system,new SpotOptions().num_rays(64).use_grid(false));
        spots = spotAnalysis.spot_results.toArray(new SpotAnalysisResult.SpotResultsForField[0]);
        pfo = ParaxHelper.asArray(system.optical_spec.parax_data.fod);
        ray_aberrations = TransverseRayAberrationAnalysis.eval(system,10,new TraceOptions());
        mtfs = spotAnalysis.computeMTFs(freqs);
    }
}
