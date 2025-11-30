package org.redukti.optim3;

import org.redukti.jfotoptix.analysis.AnalysisRayFinder;
import org.redukti.jfotoptix.analysis.AnalysisSpot;
import org.redukti.jfotoptix.model.OpticalSystem;
import org.redukti.jfotoptix.parax.ParaxialFirstOrderInfo;
import org.redukti.jfotoptix.tracing.RayTraceResults;
import org.redukti.spec.Prescription;

public class Analysis {

    public int spotDensity = 10;

    public Prescription prescription;
    public double[] pfo;

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
    public OpticalSystem[] systems;
    public AnalysisSpot[] spots;
    public double[] fields;

    public Analysis(Prescription prescription, double[] fields,boolean y_intercept_fields) {
        this.prescription = prescription;
        // ensure we have field 0.0 as the first field
        if (fields.length == 0)
            this.fields = new double[]{0.0};
        else {
            if (fields[0] != 0.0) {
                this.fields = new double[fields.length+1];
                this.fields[0] = 0.0;
                for (int i = 0; i < fields.length; i++)
                    this.fields[i+1] = fields[i];
            }
            else
                this.fields = fields;
        }
    }
    public Analysis(Prescription prescription, double[] fields) {
        this(prescription,fields,false);
    }
    public Analysis(Prescription prescription) {
        this(prescription,new double[0],false);
    }
    public Analysis spotDensity(int density) {
        this.spotDensity = density;
        return this;
    }

    public void compute() {
        systems = new OpticalSystem[fields.length];
        spots = new AnalysisSpot[fields.length];
        for (int i = 0; i < fields.length; i++) {
            systems[i] = prescription.buildSystem(true,fields[i]).build();
            spots[i] = new AnalysisSpot(systems[i],spotDensity).process_analysis();
        }
        pfo = ParaxialFirstOrderInfo.compute(systems[0]).asArray();
    }
}
