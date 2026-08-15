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
    public boolean _append_failed_spot_rays = false;
    /** Whether ordinary spot rays are rejected by physical surface apertures. */
    public boolean _check_spot_apertures = true;
    public int _contrast_num_rings = 3;
    public int _contrast_num_spokes = 6;
    /** See {@link ContrastOptions#calibrate_frequency(boolean)}; off by default. */
    public boolean _contrast_calibrate_frequency = false;
    /** See {@link ContrastOptions#center_residuals(boolean)}; off by default. */
    public boolean _contrast_center_residuals = false;
    public boolean _compute_spots = true;
    public boolean _compute_ray_aberrations = true;
    public boolean _compute_mtf = true;
    /** How each rebuilt model establishes its vignetting; see {@link #vignetting(VigType)}. */
    public VigType _vig_type = VigType.SetPupil;
    /** See {@link #freezing_vignetting(boolean)}; off by default. */
    public boolean _freeze_vignetting = false;
    /** Captured on the first compute when frozen: [field][vux, vlx, vuy, vly]. */
    private double[][] _frozen_vignetting;
    private Double _frozen_pupil_value;
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
    public Analysis retaining_failed_spot_rays(boolean value) {
        _append_failed_spot_rays = value;
        return this;
    }
    public Analysis checking_spot_apertures(boolean value) {
        _check_spot_apertures = value;
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
    /** See {@link ContrastOptions#calibrate_frequency(boolean)}. */
    public Analysis calibrating_contrast_frequency(boolean value) {
        _contrast_calibrate_frequency = value;
        return this;
    }
    /** See {@link ContrastOptions#center_residuals(boolean)}. */
    public Analysis centering_contrast_residuals(boolean value) {
        _contrast_center_residuals = value;
        return this;
    }
    public Analysis required_analyses(boolean spots, boolean rayAberrations, boolean mtf) {
        _compute_spots = spots || mtf;
        _compute_ray_aberrations = rayAberrations;
        _compute_mtf = mtf;
        return this;
    }

    /**
     * How every rebuilt optical model establishes its vignetting factors.
     *
     * <p>{@link VigType#SetPupil} is the default and what all existing regression values
     * were generated under: it resizes the pupil so the axial marginal ray meets the stop
     * edge, then measures all four factors with real rays. {@link VigType#SetVig} measures
     * the factors the same way without the resize, and agrees closely - within 0.005 of
     * pupil half-width and 3-4 MTF decimals on both test lenses.
     *
     * <p>{@link VigType#Paraxial} is cheaper but sets only the <em>y</em> factors: a
     * paraxial ray is meridional and says nothing about the sagittal pupil, so x comes out
     * unvignetted at every field. That makes the pupil an ellipse even on axis, where
     * sagittal and tangential MTF must be equal - measured 0.148 apart at 40 cyc/mm on the
     * Leica 75/2 and 0.010 on the Otus. It also means sagittal is optimized over a
     * superset of the real aperture and tangential over a subset.
     */
    public Analysis vignetting(VigType vigType) {
        _vig_type = vigType == null ? VigType.None : vigType;
        discard_frozen_vignetting();
        return this;
    }

    /**
     * Measure the vignetting factors once, then hold them fixed for the rest of the run.
     *
     * <p>Apertures are never optimization variables, but vignetting is not therefore
     * constant: it is where rays land on those fixed apertures, and 28 of 29 variables on
     * the Leica 75/2 move a factor within a single Jacobian step. The drift is smooth, so
     * it does not corrupt the finite difference, but it does mean the solver
     * differentiates the design and the pupil together - and a more heavily vignetted lens
     * has less aberration, so shrinking the pupil is a way to improve an MTF-like merit
     * that costs nothing in the merit and real light in the lens. Freezing removes that
     * route and gives every iteration the same pupil to be compared on.
     *
     * <p>The cost is staleness: the factors describe the design as it was at capture, so
     * the further the solve travels the more the assumed pupil diverges from the real one.
     * Call {@link #discard_frozen_vignetting()} between solver restarts to re-measure.
     *
     * <p>With {@link VigType#SetPupil} the captured pupil value is held too, since factors
     * measured at one working f/# do not describe another. That pins {@code fod.fno}, so a
     * {@link GoalParax} on {@link ParaxHelper#Fno} becomes inert in that combination.
     */
    public Analysis freezing_vignetting(boolean value) {
        _freeze_vignetting = value;
        if (!value) discard_frozen_vignetting();
        return this;
    }

    /** Drop captured factors so the next {@link #compute()} measures them afresh. */
    public Analysis discard_frozen_vignetting() {
        _frozen_vignetting = null;
        _frozen_pupil_value = null;
        return this;
    }

    /** The frozen factors as [field][vux, vlx, vuy, vly], or null if not frozen yet. */
    public double[][] frozen_vignetting() {
        if (_frozen_vignetting == null) return null;
        double[][] copy = new double[_frozen_vignetting.length][];
        for (int i = 0; i < copy.length; i++) copy[i] = _frozen_vignetting[i].clone();
        return copy;
    }

    private OpticalModel build_model(VigType vigType) {
        return new RayOpticsModelBuilder(_prescription)
                .build_optical_model(true, _fields, false, vigType, true, _scenario);
    }

    private OpticalModel build_vignetted_model() {
        if (!_freeze_vignetting) return build_model(_vig_type);
        if (_frozen_vignetting == null) capture_vignetting();
        // Build without establishing vignetting, then stamp the captured state on.
        OpticalModel model = build_model(VigType.None);
        var fields = model.optical_spec.fov.fields;
        if (fields.length != _frozen_vignetting.length)
            throw new IllegalStateException("frozen vignetting was captured for "
                    + _frozen_vignetting.length + " fields but the model has " + fields.length);
        if (_frozen_pupil_value != null
                && !_frozen_pupil_value.equals(model.optical_spec.pupil.value)) {
            model.optical_spec.pupil.value = _frozen_pupil_value;
            model.update_model();
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i].vux = _frozen_vignetting[i][0];
            fields[i].vlx = _frozen_vignetting[i][1];
            fields[i].vuy = _frozen_vignetting[i][2];
            fields[i].vly = _frozen_vignetting[i][3];
        }
        return model;
    }

    private void capture_vignetting() {
        OpticalModel reference = build_model(_vig_type);
        var fields = reference.optical_spec.fov.fields;
        _frozen_vignetting = new double[fields.length][4];
        for (int i = 0; i < fields.length; i++) {
            _frozen_vignetting[i][0] = fields[i].vux;
            _frozen_vignetting[i][1] = fields[i].vlx;
            _frozen_vignetting[i][2] = fields[i].vuy;
            _frozen_vignetting[i][3] = fields[i].vly;
        }
        _frozen_pupil_value = reference.optical_spec.pupil.value;
    }

    public void compute() {
        _opt_model = build_vignetted_model();
        _pfo = ParaxHelper.asArray(_opt_model.optical_spec.parax_data.fod);
        if (_compute_spots) {
            SpotOptions options;
            if (_spot_pattern == SpotOptions.PATTERN_GAUSS_QUADRATURE) {
                options = new SpotOptions().use_gaussian_quadrature()
                        .num_rings(_num_rings).num_spokes(_num_spokes)
                        .append_failed_rays(_append_failed_spot_rays)
                        .check_apertures(_check_spot_apertures);
            }
            else {
                options = new SpotOptions().use_hexapolar().num_rays(_num_rays)
                        .check_apertures(_check_spot_apertures);
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
                    .num_spokes(_contrast_num_spokes)
                    .calibrate_frequency(_contrast_calibrate_frequency)
                    .center_residuals(_contrast_center_residuals);
            _contrasts[i] = ContrastAnalysis.eval(_opt_model, options);
        }
    }
}
