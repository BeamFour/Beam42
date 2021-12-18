package org.redukti.rayoptics.specs;

public class OpticalSpecs {

    public PupilSpec pupil;
    public FieldSpec field_of_view;
    public WvlSpec spectral_region;

    public void update_model() {
        spectral_region.update_model();
        pupil.update_model();
    }
}
