// Copyright 2017-2025 Michael J. Hayford
// Original software https://github.com/mjhoptics/ray-optics
// Java version by Dibyendu Majumdar
package org.redukti.rayoptics.optical;

import org.redukti.rayoptics.parax.ParaxModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.OpticalSpecs;
import org.redukti.rayoptics.specs.SystemSpec;

/**
 * Top level container for optical model.
 *
 *     The OpticalModel serves as a top level container of model properties.
 *     Key aspects are built-in element and surface based repesentations of the
 *     optical surfaces.
 *     A sequential optical model is a sequence of surfaces and gaps.
 *     Additionally, it includes optical usage information to specify the
 *     aperture, field of view, spectrum and focus.
 *
 *     Attributes:
 *         ro_version: current version of rayoptics
 *         radius_mode: if True output radius, else output curvature
 *         NYI specsheet: :class:`~rayoptics.parax.specsheet.SpecSheet`
 *         system_spec: :class:`.SystemSpec`
 *         seq_model: :class:`~rayoptics.seq.sequential.SequentialModel`
 *         optical_spec: :class:`~rayoptics.raytr.opticalspec.OpticalSpecs`
 *         NYI parax_model: :class:`~rayoptics.parax.paraxialdesign.ParaxialModel`
 *         NYI ele_model: :class:`~rayoptics.elem.elements.ElementModel`
 */
public class OpticalModel {

    public SequentialModel seq_model;
    public OpticalSpecs optical_spec;
    public SystemSpec system_spec;
    public ParaxModel parax_model;
    public boolean radius_mode;
    public String dimensions = "mm";

    public OpticalModel(boolean radius_mode) {
        seq_model = new SequentialModel(this);
        optical_spec = new OpticalSpecs(this);
        system_spec = new SystemSpec();
        parax_model = new ParaxModel(this,1.0);
        seq_model.update_model();
    }
    public OpticalModel() {
        this(false);
    }

    /**
     * Model and its constituents are updated.
     */
    public void update_model() {
        seq_model.update_model();
        optical_spec.update_model();
        update_optical_properties();
    }

    /**
     * Compute first order and other optical properties.
     */
    public void update_optical_properties() {
        // OpticalSpec maintains first order and ray aiming for fields
        optical_spec.update_optical_properies();
        // Update the ParaxialModel as needed
        parax_model.update_model();
        // Update surface apertures, if requested (do_apertures=True)
        seq_model.update_optical_properties();
    }

    public double nm_to_sys_units(double nm) {
        return system_spec.nm_to_sys_units(nm);
    }

    public void apply_scale_factor(double scale_factor) {
        seq_model.apply_scale_factor(scale_factor);
        optical_spec.apply_scale_factor(scale_factor);
        optical_spec.update_model();
        update_optical_properties();
    }
}
