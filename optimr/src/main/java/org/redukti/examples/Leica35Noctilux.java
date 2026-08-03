package org.redukti.examples;

import org.redukti.importers.obench.OpticalBenchDataImporter;
import org.redukti.optim.*;
import org.redukti.spec.Prescription;

public class Leica35Noctilux {

    private static Prescription getPrescriptionFromFile(String specfile) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_file(specfile);
        return Prescription.build_prescription(specs,true);
    }
    private static Prescription getPrescriptionFromBuffer(String buffer) throws Exception {
        OpticalBenchDataImporter.LensSpecifications specs = new OpticalBenchDataImporter.LensSpecifications();
        specs.parse_buffer(buffer);
        return Prescription.build_prescription(specs,true);
    }

    static Var[] buildVars(Prescription prescription, int group) {
        if (group == 0) {
            return new Var[]{
                    new VarRadius(prescription, 0),
                    new VarRadius(prescription, 1),
                    new VarRadius(prescription, 2),
                    new VarRadius(prescription, 4),
                    new VarRadius(prescription, 5),
                    new VarRadius(prescription, 8),
                    new VarRadius(prescription, 9),
                    new VarRadius(prescription, 10),
                    new VarRadius(prescription, 11),
                    new VarRadius(prescription, 12),
                    new VarRadius(prescription, 13),
                    new VarRadius(prescription, 14),
                    new VarRadius(prescription, 15),
            };
        }
        else if (group == 1){
            return new Var[] {
                        new VarAsphCoeff(prescription,3,1,1E6),
                        new VarAsphCoeff(prescription,10,1,1E7),
                        new VarAsphCoeff(prescription,15,1,1E6),
                };
        }
        else if (group == 2) {
            return new Var[]{
                    new VarAsphCoeff(prescription, 3, 2, 1E9),
                    new VarAsphCoeff(prescription, 10, 2, 1E9),
                    new VarAsphCoeff(prescription, 15, 2, 1E9),
            };
        }
        else if (group == 3) {
            return new Var[]{
                    new VarAsphCoeff(prescription, 3, 3, 1E11),
                    new VarAsphCoeff(prescription, 10, 3, 1E12),
                    new VarAsphCoeff(prescription, 15, 3, 1E11),
            };
        }
        else if (group == 4) {
            return new Var[]{
                    new VarAsphCoeff(prescription, 15, 4, 1E15),
            };
        }
        else if (group == 5) {
            return new Var[]{
                    new VarAsphCoeff(prescription, 15, 5, 1E17),
            };
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    static Goal[] buildGoals(Analysis analysis) {
        return new Goal[] {
//                        new GoalSpotRMS(analysis, 1, 8.0, 1.0),
//                        new GoalSpotRMS(analysis, 2, 10.0, 1.0),
//                        new GoalSpotRMS(analysis, 3, 15.0, 1.0),
//                        new GoalSpotRMS(analysis, 4, 20.0, 1.0),
//                        new GoalSpotMaxRadius(analysis, 1, 12.0, 1.0),
//                        new GoalSpotMaxRadius(analysis, 2, 16.0, 1.0),
//                        new GoalSpotMaxRadius(analysis, 3, 20.0, 1.0),
//                        new GoalSpotMaxRadius(analysis, 4, 60.0, 1.0),

                new GeoMTF(analysis,1,0,10,0.99,1.3),
                new GeoMTF(analysis,1,1,10,0.99,1.3),
                new GeoMTF(analysis,2,0,10,0.99,1.2),
                new GeoMTF(analysis,2,1,10,0.99,1.2),
                new GeoMTF(analysis,3,0,10,0.95,1.1),
                new GeoMTF(analysis,3,1,10,0.95,1.1),
                new GeoMTF(analysis,4,0,10,0.9,1.0),
                new GeoMTF(analysis,4,1,10,0.7,1.0),
                // below are figures from patent
                new GeoMTF(analysis,1,0,20,0.9,1.3),
                new GeoMTF(analysis,1,1,20,0.9,1.3),
                new GeoMTF(analysis,2,0,20,0.9,1.2),
                new GeoMTF(analysis,2,1,20,0.9,1.2),
                new GeoMTF(analysis,3,0,20,0.8,1.1),
                new GeoMTF(analysis,3,1,20,0.8,1.1),
                new GeoMTF(analysis,4,0,20,0.7,1.0),
                new GeoMTF(analysis,4,1,20,0.5,1.1),

                new GeoMTF(analysis,1,0,40,0.8,1.3),
                new GeoMTF(analysis,1,1,40,0.8,1.3),
                new GeoMTF(analysis,2,0,40,0.7,1.2),
                new GeoMTF(analysis,2,1,40,0.7,1.2),
                new GeoMTF(analysis,3,0,40,0.6,1.1),
                new GeoMTF(analysis,3,1,40,0.6,1.1),
                new GeoMTF(analysis,4,0,40,0.5,1.0),
                new GeoMTF(analysis,4,1,40,0.3,1.0),

                new GoalParax(analysis, ParaxHelper.Effective_focal_length,34.56, 1.0),
                new GoalParax(analysis, ParaxHelper.Fno, 1.223, 1.0),
                //new GoalParax(analysis, ParaxHelper.Back_focal_length, 14.42, 1.0),
                //new GoalParax(analysis, ParaxHelper.Pp1, 51.8, 1.0),
                //new GoalParax(analysis, ParaxHelper.Ppk, 20.2, 1.0)

//                        new GoalRayAberration(analysis,1,0,0,546.074, 0,1),
//                        new GoalRayAberration(analysis,1,0,1,546.074, 0,1),
//                        new GoalRayAberration(analysis,1,0,2,546.074, 0,1),
//                        new GoalRayAberration(analysis,1,0,3,546.074, 0,1),
//                        new GoalRayAberration(analysis,1,0,4,546.074, 0,1),
//                        new GoalRayAberration(analysis,1,0,5,546.074, 0,1),
//                        new GoalRayAberration(analysis,1,0,6,546.074, 0,1),
//                        new GoalRayAberration(analysis,1,0,7,546.074, 0,1),
//                        new GoalRayAberration(analysis,1,0,8,546.074, 0,1),
//                        new GoalRayAberration(analysis,1,0,9,546.074, 0,1),

                new GoalRayAberration(analysis,1,0,0,587.5618, 0,1),
                new GoalRayAberration(analysis,1,0,1,587.5618, 0,1),
                new GoalRayAberration(analysis,1,0,2,587.5618, 0,1),
                new GoalRayAberration(analysis,1,0,3,587.5618, 0,1),
                new GoalRayAberration(analysis,1,0,4,587.5618, 0,1),
                new GoalRayAberration(analysis,1,0,5,587.5618, 0,1),
                new GoalRayAberration(analysis,1,0,6,587.5618, 0,1),
                new GoalRayAberration(analysis,1,0,7,587.5618, 0,1),
                new GoalRayAberration(analysis,1,0,8,587.5618, 0,1),
                new GoalRayAberration(analysis,1,0,9,587.5618, 0,1),
                new GoalRayAberration(analysis,1,0,0,486.1327, 0,1),
                new GoalRayAberration(analysis,1,0,1,486.1327, 0,1),
                new GoalRayAberration(analysis,1,0,2,486.1327, 0,1),
                new GoalRayAberration(analysis,1,0,3,486.1327, 0,1),
                new GoalRayAberration(analysis,1,0,4,486.1327, 0,1),
                new GoalRayAberration(analysis,1,0,5,486.1327, 0,1),
                new GoalRayAberration(analysis,1,0,6,486.1327, 0,1),
                new GoalRayAberration(analysis,1,0,7,486.1327, 0,1),
                new GoalRayAberration(analysis,1,0,8,486.1327, 0,1),
                new GoalRayAberration(analysis,1,0,9,486.1327, 0,1),

                new GoalRayAberration(analysis,1,0,0,656.2725, 0,1),
                new GoalRayAberration(analysis,1,0,1,656.2725, 0,1),
                new GoalRayAberration(analysis,1,0,2,656.2725, 0,1),
                new GoalRayAberration(analysis,1,0,3,656.2725, 0,1),
                new GoalRayAberration(analysis,1,0,4,656.2725, 0,1),
                new GoalRayAberration(analysis,1,0,5,656.2725, 0,1),
                new GoalRayAberration(analysis,1,0,6,656.2725, 0,1),
                new GoalRayAberration(analysis,1,0,7,656.2725, 0,1),
                new GoalRayAberration(analysis,1,0,8,656.2725, 0,1),
                new GoalRayAberration(analysis,1,0,9,656.2725, 0,1),

//                        new GoalRayAberration(analysis,2,0,0,546.074, 0,1),
//                        new GoalRayAberration(analysis,2,0,1,546.074, 0,1),
//                        new GoalRayAberration(analysis,2,0,2,546.074, 0,1),
//                        new GoalRayAberration(analysis,2,0,3,546.074, 0,1),
//                        new GoalRayAberration(analysis,2,0,4,546.074, 0,1),
//                        new GoalRayAberration(analysis,2,0,5,546.074, 0,1),
//                        new GoalRayAberration(analysis,2,0,6,546.074, 0,1),
//                        new GoalRayAberration(analysis,2,0,7,546.074, 0,1),
//                        new GoalRayAberration(analysis,2,0,8,546.074, 0,1),
//                        new GoalRayAberration(analysis,2,0,9,546.074, 0,1),

                new GoalRayAberration(analysis,2,0,0,587.5618, 0,1),
                new GoalRayAberration(analysis,2,0,1,587.5618, 0,1),
                new GoalRayAberration(analysis,2,0,2,587.5618, 0,1),
                new GoalRayAberration(analysis,2,0,3,587.5618, 0,1),
                new GoalRayAberration(analysis,2,0,4,587.5618, 0,1),
                new GoalRayAberration(analysis,2,0,5,587.5618, 0,1),
                new GoalRayAberration(analysis,2,0,6,587.5618, 0,1),
                new GoalRayAberration(analysis,2,0,7,587.5618, 0,1),
                new GoalRayAberration(analysis,2,0,8,587.5618, 0,1),
                new GoalRayAberration(analysis,2,0,9,587.5618, 0,1),

                new GoalRayAberration(analysis,2,0,0,486.1327, 0,1),
                new GoalRayAberration(analysis,2,0,1,486.1327, 0,1),
                new GoalRayAberration(analysis,2,0,2,486.1327, 0,1),
                new GoalRayAberration(analysis,2,0,3,486.1327, 0,1),
                new GoalRayAberration(analysis,2,0,4,486.1327, 0,1),
                new GoalRayAberration(analysis,2,0,5,486.1327, 0,1),
                new GoalRayAberration(analysis,2,0,6,486.1327, 0,1),
                new GoalRayAberration(analysis,2,0,7,486.1327, 0,1),
                new GoalRayAberration(analysis,2,0,8,486.1327, 0,1),
                new GoalRayAberration(analysis,2,0,9,486.1327, 0,1),

                new GoalRayAberration(analysis,2,0,0,656.2725, 0,1),
                new GoalRayAberration(analysis,2,0,1,656.2725, 0,1),
                new GoalRayAberration(analysis,2,0,2,656.2725, 0,1),
                new GoalRayAberration(analysis,2,0,3,656.2725, 0,1),
                new GoalRayAberration(analysis,2,0,4,656.2725, 0,1),
                new GoalRayAberration(analysis,2,0,5,656.2725, 0,1),
                new GoalRayAberration(analysis,2,0,6,656.2725, 0,1),
                new GoalRayAberration(analysis,2,0,7,656.2725, 0,1),
                new GoalRayAberration(analysis,2,0,8,656.2725, 0,1),
                new GoalRayAberration(analysis,2,0,9,656.2725, 0,1),

//                        new GoalRayAberration(analysis,3,0,0,546.074, 0,1),
//                        new GoalRayAberration(analysis,3,0,1,546.074, 0,1),
//                        new GoalRayAberration(analysis,3,0,2,546.074, 0,1),
//                        new GoalRayAberration(analysis,3,0,3,546.074, 0,1),
//                        new GoalRayAberration(analysis,3,0,4,546.074, 0,1),
//                        new GoalRayAberration(analysis,3,0,5,546.074, 0,1),
//                        new GoalRayAberration(analysis,3,0,6,546.074, 0,1),
//                        new GoalRayAberration(analysis,3,0,7,546.074, 0,1),
//                        new GoalRayAberration(analysis,3,0,8,546.074, 0,1),
//                        new GoalRayAberration(analysis,3,0,9,546.074, 0,1),

                new GoalRayAberration(analysis,3,0,0,587.5618, 0,1),
                new GoalRayAberration(analysis,3,0,1,587.5618, 0,1),
                new GoalRayAberration(analysis,3,0,2,587.5618, 0,1),
                new GoalRayAberration(analysis,3,0,3,587.5618, 0,1),
                new GoalRayAberration(analysis,3,0,4,587.5618, 0,1),
                new GoalRayAberration(analysis,3,0,5,587.5618, 0,1),
                new GoalRayAberration(analysis,3,0,6,587.5618, 0,1),
                new GoalRayAberration(analysis,3,0,7,587.5618, 0,1),
                new GoalRayAberration(analysis,3,0,8,587.5618, 0,1),
                new GoalRayAberration(analysis,3,0,9,587.5618, 0,1),

                new GoalRayAberration(analysis,3,0,0,486.1327, 0,1),
                new GoalRayAberration(analysis,3,0,1,486.1327, 0,1),
                new GoalRayAberration(analysis,3,0,2,486.1327, 0,1),
                new GoalRayAberration(analysis,3,0,3,486.1327, 0,1),
                new GoalRayAberration(analysis,3,0,4,486.1327, 0,1),
                new GoalRayAberration(analysis,3,0,5,486.1327, 0,1),
                new GoalRayAberration(analysis,3,0,6,486.1327, 0,1),
                new GoalRayAberration(analysis,3,0,7,486.1327, 0,1),
                new GoalRayAberration(analysis,3,0,8,486.1327, 0,1),
                new GoalRayAberration(analysis,3,0,9,486.1327, 0,1),

                new GoalRayAberration(analysis,3,0,0,656.2725, 0,1),
                new GoalRayAberration(analysis,3,0,1,656.2725, 0,1),
                new GoalRayAberration(analysis,3,0,2,656.2725, 0,1),
                new GoalRayAberration(analysis,3,0,3,656.2725, 0,1),
                new GoalRayAberration(analysis,3,0,4,656.2725, 0,1),
                new GoalRayAberration(analysis,3,0,5,656.2725, 0,1),
                new GoalRayAberration(analysis,3,0,6,656.2725, 0,1),
                new GoalRayAberration(analysis,3,0,7,656.2725, 0,1),
                new GoalRayAberration(analysis,3,0,8,656.2725, 0,1),
                new GoalRayAberration(analysis,3,0,9,656.2725, 0,1),

//                        new GoalRayAberration(analysis,4,0,0,546.074, 0,1),
//                        new GoalRayAberration(analysis,4,0,1,546.074, 0,1),
//                        new GoalRayAberration(analysis,4,0,2,546.074, 0,1),
//                        new GoalRayAberration(analysis,4,0,3,546.074, 0,1),
//                        new GoalRayAberration(analysis,4,0,4,546.074, 0,1),
//                        new GoalRayAberration(analysis,4,0,5,546.074, 0,1),
//                        new GoalRayAberration(analysis,4,0,6,546.074, 0,1),
//                        new GoalRayAberration(analysis,4,0,7,546.074, 0,1),
//                        new GoalRayAberration(analysis,4,0,8,546.074, 0,1),
//                        new GoalRayAberration(analysis,4,0,9,546.074, 0,1),

                new GoalRayAberration(analysis,4,0,0,587.5618, 0,1),
                new GoalRayAberration(analysis,4,0,1,587.5618, 0,1),
                new GoalRayAberration(analysis,4,0,2,587.5618, 0,1),
                new GoalRayAberration(analysis,4,0,3,587.5618, 0,1),
                new GoalRayAberration(analysis,4,0,4,587.5618, 0,1),
                new GoalRayAberration(analysis,4,0,5,587.5618, 0,1),
                new GoalRayAberration(analysis,4,0,6,587.5618, 0,1),
                new GoalRayAberration(analysis,4,0,7,587.5618, 0,1),
                new GoalRayAberration(analysis,4,0,8,587.5618, 0,1),
                new GoalRayAberration(analysis,4,0,9,587.5618, 0,1),

                new GoalRayAberration(analysis,4,0,0,486.1327, 0,1),
                new GoalRayAberration(analysis,4,0,1,486.1327, 0,1),
                new GoalRayAberration(analysis,4,0,2,486.1327, 0,1),
                new GoalRayAberration(analysis,4,0,3,486.1327, 0,1),
                new GoalRayAberration(analysis,4,0,4,486.1327, 0,1),
                new GoalRayAberration(analysis,4,0,5,486.1327, 0,1),
                new GoalRayAberration(analysis,4,0,6,486.1327, 0,1),
                new GoalRayAberration(analysis,4,0,7,486.1327, 0,1),
                new GoalRayAberration(analysis,4,0,8,486.1327, 0,1),
                new GoalRayAberration(analysis,4,0,9,486.1327, 0,1),

                new GoalRayAberration(analysis,4,0,0,656.2725, 0,1),
                new GoalRayAberration(analysis,4,0,1,656.2725, 0,1),
                new GoalRayAberration(analysis,4,0,2,656.2725, 0,1),
                new GoalRayAberration(analysis,4,0,3,656.2725, 0,1),
                new GoalRayAberration(analysis,4,0,4,656.2725, 0,1),
                new GoalRayAberration(analysis,4,0,5,656.2725, 0,1),
                new GoalRayAberration(analysis,4,0,6,656.2725, 0,1),
                new GoalRayAberration(analysis,4,0,7,656.2725, 0,1),
                new GoalRayAberration(analysis,4,0,8,656.2725, 0,1),
                new GoalRayAberration(analysis,4,0,9,656.2725, 0,1),

                ///
//                        new GoalRayAberration(analysis,1,1,0,587.5618, 0,1),
//                        new GoalRayAberration(analysis,1,1,1,587.5618, 0,1),
//                        new GoalRayAberration(analysis,1,1,2,587.5618, 0,1),
//                        new GoalRayAberration(analysis,1,1,3,587.5618, 0,1),
//                        new GoalRayAberration(analysis,1,1,4,587.5618, 0,1),
//                        new GoalRayAberration(analysis,1,1,5,587.5618, 0,1),
//                        new GoalRayAberration(analysis,1,1,6,587.5618, 0,1),
//                        new GoalRayAberration(analysis,1,1,7,587.5618, 0,1),
//                        new GoalRayAberration(analysis,1,1,8,587.5618, 0,1),
//                        new GoalRayAberration(analysis,1,1,9,587.5618, 0,1),
//                        new GoalRayAberration(analysis,1,1,0,486.1327, 0,1),
//                        new GoalRayAberration(analysis,1,1,1,486.1327, 0,1),
//                        new GoalRayAberration(analysis,1,1,2,486.1327, 0,1),
//                        new GoalRayAberration(analysis,1,1,3,486.1327, 0,1),
//                        new GoalRayAberration(analysis,1,1,4,486.1327, 0,1),
//                        new GoalRayAberration(analysis,1,1,5,486.1327, 0,1),
//                        new GoalRayAberration(analysis,1,1,6,486.1327, 0,1),
//                        new GoalRayAberration(analysis,1,1,7,486.1327, 0,1),
//                        new GoalRayAberration(analysis,1,1,8,486.1327, 0,1),
//                        new GoalRayAberration(analysis,1,1,9,486.1327, 0,1),
//                        new GoalRayAberration(analysis,1,1,0,656.2725, 0,1),
//                        new GoalRayAberration(analysis,1,1,1,656.2725, 0,1),
//                        new GoalRayAberration(analysis,1,1,2,656.2725, 0,1),
//                        new GoalRayAberration(analysis,1,1,3,656.2725, 0,1),
//                        new GoalRayAberration(analysis,1,1,4,656.2725, 0,1),
//                        new GoalRayAberration(analysis,1,1,5,656.2725, 0,1),
//                        new GoalRayAberration(analysis,1,1,6,656.2725, 0,1),
//                        new GoalRayAberration(analysis,1,1,7,656.2725, 0,1),
//                        new GoalRayAberration(analysis,1,1,8,656.2725, 0,1),
//                        new GoalRayAberration(analysis,1,1,9,656.2725, 0,1),
//
//                        new GoalRayAberration(analysis,2,1,0,587.5618, 0,1),
//                        new GoalRayAberration(analysis,2,1,1,587.5618, 0,1),
//                        new GoalRayAberration(analysis,2,1,2,587.5618, 0,1),
//                        new GoalRayAberration(analysis,2,1,3,587.5618, 0,1),
//                        new GoalRayAberration(analysis,2,1,4,587.5618, 0,1),
//                        new GoalRayAberration(analysis,2,1,5,587.5618, 0,1),
//                        new GoalRayAberration(analysis,2,1,6,587.5618, 0,1),
//                        new GoalRayAberration(analysis,2,1,7,587.5618, 0,1),
//                        new GoalRayAberration(analysis,2,1,8,587.5618, 0,1),
//                        new GoalRayAberration(analysis,2,1,9,587.5618, 0,1),
//                        new GoalRayAberration(analysis,2,1,0,486.1327, 0,1),
//                        new GoalRayAberration(analysis,2,1,1,486.1327, 0,1),
//                        new GoalRayAberration(analysis,2,1,2,486.1327, 0,1),
//                        new GoalRayAberration(analysis,2,1,3,486.1327, 0,1),
//                        new GoalRayAberration(analysis,2,1,4,486.1327, 0,1),
//                        new GoalRayAberration(analysis,2,1,5,486.1327, 0,1),
//                        new GoalRayAberration(analysis,2,1,6,486.1327, 0,1),
//                        new GoalRayAberration(analysis,2,1,7,486.1327, 0,1),
//                        new GoalRayAberration(analysis,2,1,8,486.1327, 0,1),
//                        new GoalRayAberration(analysis,2,1,9,486.1327, 0,1),
//                        new GoalRayAberration(analysis,2,1,0,656.2725, 0,1),
//                        new GoalRayAberration(analysis,2,1,1,656.2725, 0,1),
//                        new GoalRayAberration(analysis,2,1,2,656.2725, 0,1),
//                        new GoalRayAberration(analysis,2,1,3,656.2725, 0,1),
//                        new GoalRayAberration(analysis,2,1,4,656.2725, 0,1),
//                        new GoalRayAberration(analysis,2,1,5,656.2725, 0,1),
//                        new GoalRayAberration(analysis,2,1,6,656.2725, 0,1),
//                        new GoalRayAberration(analysis,2,1,7,656.2725, 0,1),
//                        new GoalRayAberration(analysis,2,1,8,656.2725, 0,1),
//                        new GoalRayAberration(analysis,2,1,9,656.2725, 0,1),

//                        new GoalRayAberration(analysis,3,1,0,587.5618, 0,2),
//                        new GoalRayAberration(analysis,3,1,1,587.5618, 0,2),
//                        new GoalRayAberration(analysis,3,1,2,587.5618, 0,2),
//                        new GoalRayAberration(analysis,3,1,3,587.5618, 0,2),
//                        new GoalRayAberration(analysis,3,1,4,587.5618, 0,2),
//                        new GoalRayAberration(analysis,3,1,5,587.5618, 0,2),
//                        new GoalRayAberration(analysis,3,1,6,587.5618, 0,2),
//                        new GoalRayAberration(analysis,3,1,7,587.5618, 0,2),
//                        new GoalRayAberration(analysis,3,1,8,587.5618, 0,2),
//                        new GoalRayAberration(analysis,3,1,9,587.5618, 0,2),
//                        new GoalRayAberration(analysis,3,1,0,486.1327, 0,2),
//                        new GoalRayAberration(analysis,3,1,1,486.1327, 0,2),
//                        new GoalRayAberration(analysis,3,1,2,486.1327, 0,2),
//                        new GoalRayAberration(analysis,3,1,3,486.1327, 0,2),
//                        new GoalRayAberration(analysis,3,1,4,486.1327, 0,2),
//                        new GoalRayAberration(analysis,3,1,5,486.1327, 0,2),
//                        new GoalRayAberration(analysis,3,1,6,486.1327, 0,2),
//                        new GoalRayAberration(analysis,3,1,7,486.1327, 0,2),
//                        new GoalRayAberration(analysis,3,1,8,486.1327, 0,2),
//                        new GoalRayAberration(analysis,3,1,9,486.1327, 0,2),
//                        new GoalRayAberration(analysis,3,1,0,656.2725, 0,2),
//                        new GoalRayAberration(analysis,3,1,1,656.2725, 0,2),
//                        new GoalRayAberration(analysis,3,1,2,656.2725, 0,2),
//                        new GoalRayAberration(analysis,3,1,3,656.2725, 0,2),
//                        new GoalRayAberration(analysis,3,1,4,656.2725, 0,2),
//                        new GoalRayAberration(analysis,3,1,5,656.2725, 0,2),
//                        new GoalRayAberration(analysis,3,1,6,656.2725, 0,2),
//                        new GoalRayAberration(analysis,3,1,7,656.2725, 0,2),
//                        new GoalRayAberration(analysis,3,1,8,656.2725, 0,2),
//                        new GoalRayAberration(analysis,3,1,9,656.2725, 0,2),
//
//                        new GoalRayAberration(analysis,4,1,0,587.5618, 0,2),
//                        new GoalRayAberration(analysis,4,1,1,587.5618, 0,2),
//                        new GoalRayAberration(analysis,4,1,2,587.5618, 0,2),
//                        new GoalRayAberration(analysis,4,1,3,587.5618, 0,2),
//                        new GoalRayAberration(analysis,4,1,4,587.5618, 0,2),
//                        new GoalRayAberration(analysis,4,1,5,587.5618, 0,2),
//                        new GoalRayAberration(analysis,4,1,6,587.5618, 0,2),
//                        new GoalRayAberration(analysis,4,1,7,587.5618, 0,2),
//                        new GoalRayAberration(analysis,4,1,8,587.5618, 0,2),
//                        new GoalRayAberration(analysis,4,1,9,587.5618, 0,2),
//                        new GoalRayAberration(analysis,4,1,0,486.1327, 0,2),
//                        new GoalRayAberration(analysis,4,1,1,486.1327, 0,2),
//                        new GoalRayAberration(analysis,4,1,2,486.1327, 0,2),
//                        new GoalRayAberration(analysis,4,1,3,486.1327, 0,2),
//                        new GoalRayAberration(analysis,4,1,4,486.1327, 0,2),
//                        new GoalRayAberration(analysis,4,1,5,486.1327, 0,2),
//                        new GoalRayAberration(analysis,4,1,6,486.1327, 0,2),
//                        new GoalRayAberration(analysis,4,1,7,486.1327, 0,2),
//                        new GoalRayAberration(analysis,4,1,8,486.1327, 0,2),
//                        new GoalRayAberration(analysis,4,1,9,486.1327, 0,2),
//                        new GoalRayAberration(analysis,4,1,0,656.2725, 0,2),
//                        new GoalRayAberration(analysis,4,1,1,656.2725, 0,2),
//                        new GoalRayAberration(analysis,4,1,2,656.2725, 0,2),
//                        new GoalRayAberration(analysis,4,1,3,656.2725, 0,2),
//                        new GoalRayAberration(analysis,4,1,4,656.2725, 0,2),
//                        new GoalRayAberration(analysis,4,1,5,656.2725, 0,2),
//                        new GoalRayAberration(analysis,4,1,6,656.2725, 0,2),
//                        new GoalRayAberration(analysis,4,1,7,656.2725, 0,2),
//                        new GoalRayAberration(analysis,4,1,8,656.2725, 0,2),
//                        new GoalRayAberration(analysis,4,1,9,656.2725, 0,2),

        };
    }

    public static void main(String[] args) throws Exception {
        Prescription prescription = null;
        String buffer = null;
        //prescription._generate_d_line_only = false;
        for (int i = 0; i < 6*3; i++) {
            if (buffer == null)
                prescription = getPrescriptionFromFile(args[0]);
            else
                prescription = getPrescriptionFromBuffer(buffer);
            var analysis = new Analysis(prescription, new double[]{0.0,0.25,0.5,0.75}, new int[] {10,20,40});
            var f = new MeritFunction(analysis,
                    buildVars(prescription,i%6),
                    buildGoals(analysis)
                    );
            analysis.compute();
            var lm = f.getSolver();
            System.out.println("Aberrations:\n");
            System.out.println(analysis._ray_aberrations.list_ray_fans());
            System.out.println("Before:\n");
            System.out.println(f.toString());
            var istatus = lm.solve();
            System.out.println("Status = " + istatus);
            System.out.println("After:\n");
            System.out.println(f.toString());
            System.out.println(prescription.toString());
            buffer = prescription.toString();
        }

    }
}
