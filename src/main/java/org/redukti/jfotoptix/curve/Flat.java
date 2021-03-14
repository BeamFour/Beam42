package org.redukti.jfotoptix.curve;

import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;

public class Flat extends Rotational {

    public static Flat flat = new Flat();

    @Override
    public double sagitta(double s) {
        return 0;
    }

    @Override
    public double derivative (double r)
    {
        return 1.0;
    }

    /*

intersection d'un plan defini par :

P(Px, Py, Pz) appartenant au plan
N(Px, Py, Pz) normal au plan

avec une droite AB definie par l'ensemble des points tel que:

A + * t B

on a :

t=(Nz*Pz+Ny*Py+Nx*Px-Az*Nz-Ay*Ny-Ax*Nx)/(Bz*Nz+By*Ny+Bx*Nx)

*/

    @Override
    public Vector3 intersect (Vector3Pair ray)
    {
        double s = ray.direction ().z ();
        if (s == 0)
            return null;
        double a = -ray.origin ().z () / s;
        if (a < 0)
            return null;
        return ray.origin ().plus(ray.direction ().times(a));
    }

    @Override
    public Vector3 normal (Vector3 point)
    {
        return new Vector3 (0, 0, -1);
    }
}
