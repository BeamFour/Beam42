package org.redukti.optim;


import org.redukti.rayoptics.analysis.RayAberrationResult;
import org.redukti.rayoptics.analysis.SpotAnalysis;
import org.redukti.rayoptics.analysis.SpotAnalysisResult;
import org.redukti.rayoptics.analysis.TransverseRayAberrationAnalysis;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.Prescription;

public class Analysis {

    public Prescription prescription;
    public double[] fields;
    public double[] pfo;
    public SpotAnalysisResult.SpotResultsByField[] spots;
    public RayAberrationResult ray_aberrations;

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

    public Analysis(Prescription prescription, double[] fields) {
        this.prescription = prescription;
        this.fields = fields;
    }
    public void compute() {
        system = prescription.build_ray_optics_model(true,fields,true,true);
        var spotAnalysis = SpotAnalysis.eval(system,21, new TraceOptions());
        spots = spotAnalysis.spot_results.toArray(new SpotAnalysisResult.SpotResultsByField[0]);
        pfo = ParaxHelper.asArray(system.optical_spec.parax_data.fod);
        ray_aberrations = TransverseRayAberrationAnalysis.eval(system,10,new TraceOptions());
    }
}
