package org.redukti.optim;

import org.redukti.rayoptics.analysis.*;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.spec.Prescription;
import org.redukti.spec.RayOpticsModelBuilder;
import org.redukti.spec.VigType;

public class Analysis {

    public Prescription _prescription;
    public double[] _fields;
    public double[] _pfo;
    public SpotAnalysisResult.SpotResultsForField[] _spots;
    public RayAberrationResult _ray_aberrations;
    public MTFResultByFreq[] _mtfs;
    public int[] _freqs;
    public final int _scenario;

    public final static int NUM_TRANSVERSE_RAYS = 10;
    public final static int NUM_SPOT_RAYS = 64;

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
    public OpticalModel _opt_model;

    /**
     * When optimizing a prescription that has multiple scenarios configured
     * use this constructor and set the scenario. At present optimization must
     * be performed for each scenario independently.
     *
     * @param prescription Optical prescription
     * @param fields    Fields to be computed, values range from 0.0 to 1.0
     * @param freqs The MTF frequencies to be computed
     * @param scenario  The scenario to use, defaults to 0
     */
    public Analysis(Prescription prescription, double[] fields, int[] freqs, int scenario) {
        this._prescription = prescription;
        this._fields = fields;
        this._freqs = freqs;
        this._scenario = scenario;
    }
    public Analysis(Prescription prescription, double[] fields, int[] freqs) {
        this(prescription,fields,freqs,0);
    }
    public void compute() {
        // TODO support scenarios
        _opt_model = new RayOpticsModelBuilder(_prescription)
                .build_optical_model(true, _fields,false, VigType.SetPupil, true, _scenario);
        var spotAnalysis = SpotAnalysis.eval(_opt_model,new SpotOptions().num_rays(NUM_SPOT_RAYS).use_grid(false));
        _spots = spotAnalysis.spot_results.toArray(new SpotAnalysisResult.SpotResultsForField[0]);
        _pfo = ParaxHelper.asArray(_opt_model.optical_spec.parax_data.fod);
        // we set append_if_none to get all rays even if they
        // failed to trace, the failed rays are handled in GoalRayAberration
        _ray_aberrations = TransverseRayAberrationAnalysis.eval(_opt_model, NUM_TRANSVERSE_RAYS,true, new TraceOptions());
        _mtfs = spotAnalysis.computeMTFs(_freqs);
    }
}
