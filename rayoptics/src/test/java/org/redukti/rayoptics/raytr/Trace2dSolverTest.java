package org.redukti.rayoptics.raytr;

import org.junit.jupiter.api.Test;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.seq.SurfaceData;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class Trace2dSolverTest {

    @Test
    void aimsAnOffAxisRayInBothCoordinates() {
        SequentialModel seqModel = planeSurfaceModel();
        double[] target = {2.5, -3.75};

        RayResultWithStartCoord result = Trace.get_2d_solution(
                seqModel, 1, Vector3.ZERO, 100.0, 587.5618, target, true);

        assertArrayEquals(target, result.start_coords, 1.0e-9);
        assertNotNull(result.rr.pkg);
        assertEquals(target[0], result.rr.pkg.ray.get(1).p.x, 1.0e-9);
        assertEquals(target[1], result.rr.pkg.ray.get(1).p.y, 1.0e-9);
    }

    @Test
    void rawSolverAimsAnOffAxisRayInBothCoordinates() {
        SequentialModel seqModel = planeSurfaceModel();
        double[] target = {-1.25, 4.5};

        RayResultWithStartCoord result = Trace.get_2d_solution_raw(
                seqModel.path(), 1, Vector3.ZERO, 100.0, 587.5618, target, true);

        assertArrayEquals(target, result.start_coords, 1.0e-9);
        assertNotNull(result.rr.pkg);
        assertEquals(target[0], result.rr.pkg.ray.get(1).p.x, 1.0e-9);
        assertEquals(target[1], result.rr.pkg.ray.get(1).p.y, 1.0e-9);
    }

    @Test
    void twoDimensionalSolverAgreesWithOneDimensionalSolverForSymmetricRay() {
        SequentialModel seqModel = surfaceModel(0.01);
        double yTarget = 3.25;

        RayResultWithStartCoord oneDimensional = Trace.get_1d_solution(
                seqModel, 1, Vector3.ZERO, 100.0, 587.5618, yTarget, true);
        RayResultWithStartCoord twoDimensional = Trace.get_2d_solution(
                seqModel, 1, Vector3.ZERO, 100.0, 587.5618,
                new double[]{0.0, yTarget}, true);

        assertEquals(0.0, twoDimensional.start_coords[0], 1.0e-12);
        assertEquals(oneDimensional.start_coords[1], twoDimensional.start_coords[1], 1.0e-8);
        assertEquals(oneDimensional.rr.pkg.ray.get(1).p.y,
                twoDimensional.rr.pkg.ray.get(1).p.y, 1.0e-8);
    }

    @Test
    void acceptsAnExactInitialSolution() {
        SequentialModel seqModel = planeSurfaceModel();

        RayResultWithStartCoord result = Trace.get_2d_solution(
                seqModel, 1, Vector3.ZERO, 100.0, 587.5618, new double[]{0.0, 0.0}, true);

        assertArrayEquals(new double[]{0.0, 0.0}, result.start_coords, 0.0);
        assertNotNull(result.rr.pkg);
        assertEquals(0.0, result.rr.pkg.ray.get(1).p.x, 0.0);
        assertEquals(0.0, result.rr.pkg.ray.get(1).p.y, 0.0);
    }

    private static SequentialModel planeSurfaceModel() {
        return surfaceModel(0.0);
    }

    private static SequentialModel surfaceModel(double curvature) {
        OpticalModel opticalModel = new OpticalModel();
        SequentialModel seqModel = opticalModel.seq_model;
        seqModel.gaps.get(0).thi = 100.0;
        seqModel.add_surface(new SurfaceData(curvature, 10.0).max_aperture(100.0));
        seqModel.do_apertures = false;
        seqModel.update_model();
        return seqModel;
    }
}
