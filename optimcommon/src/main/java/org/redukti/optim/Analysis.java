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
    public ContrastAnalysisResult[] _contrasts;
    public int[] _freqs;
    public int[] _contrast_freqs = new int[0];
    public final int _scenario;
    public int _spot_pattern = SpotOptions.PATTERN_HEXAPOLAR;
    public int _num_rays = 64;
    public int _num_rings = 14;
    public int _num_spokes = 20;
    public int _contrast_num_rings = 3;
    public int _contrast_num_spokes = 6;
    public boolean _compute_spots = true;
    public boolean _compute_ray_aberrations = true;
    public boolean _compute_mtf = true;
    public final static int NUM_TRANSVERSE_RAYS = 10;

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
    public Analysis using_gauss_quadrature_pattern(int num_rings, int num_spokes) {
        _spot_pattern = SpotOptions.PATTERN_GAUSS_QUADRATURE;
        _num_rings = num_rings;
        _num_spokes = num_spokes;
        return this;
    }
    public Analysis using_hexapolar_pattern(int num_rays) {
        _spot_pattern = SpotOptions.PATTERN_HEXAPOLAR;
        _num_rays = num_rays;
        return this;
    }
    public Analysis using_contrast_analysis(int[] frequencies, int num_rings, int num_spokes) {
        _contrast_freqs = frequencies == null ? new int[0] : frequencies.clone();
        _contrast_num_rings = num_rings;
        _contrast_num_spokes = num_spokes;
        return this;
    }
    public Analysis required_analyses(boolean spots, boolean rayAberrations, boolean mtf) {
        _compute_spots = spots || mtf;
        _compute_ray_aberrations = rayAberrations;
        _compute_mtf = mtf;
        return this;
    }
    public void compute() {
        _opt_model = new RayOpticsModelBuilder(_prescription)
                .build_optical_model(true, _fields,false, VigType.SetPupil, true, _scenario);
        _pfo = ParaxHelper.asArray(_opt_model.optical_spec.parax_data.fod);
        if (_compute_spots) {
            SpotOptions options;
            if (_spot_pattern == SpotOptions.PATTERN_GAUSS_QUADRATURE) {
                options = new SpotOptions().use_gaussian_quadrature().num_rings(_num_rings).num_spokes(_num_spokes);
            }
            else {
                options = new SpotOptions().use_hexapolar().num_rays(_num_rays);
            }
            var spotAnalysis = SpotAnalysis.eval(_opt_model,options);
            _spots = spotAnalysis.spot_results.toArray(new SpotAnalysisResult.SpotResultsForField[0]);
            _mtfs = _compute_mtf ? spotAnalysis.computeMTFs(_freqs) : null;
        }
        else {
            _spots = null;
            _mtfs = null;
        }
        // We set append_if_none to retain failed fan rays; their goals apply a penalty.
        _ray_aberrations = _compute_ray_aberrations
                ? TransverseRayAberrationAnalysis.eval(
                        _opt_model, NUM_TRANSVERSE_RAYS, true, new TraceOptions())
                : null;
        _contrasts = new ContrastAnalysisResult[_contrast_freqs.length];
        for (int i = 0; i < _contrast_freqs.length; i++) {
            var options = new ContrastOptions(_contrast_freqs[i])
                    .num_rings(_contrast_num_rings)
                    .num_spokes(_contrast_num_spokes);
            _contrasts[i] = ContrastAnalysis.eval(_opt_model, options);
        }
    }
}
