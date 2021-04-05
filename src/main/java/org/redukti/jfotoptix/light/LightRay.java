package org.redukti.jfotoptix.light;

import org.redukti.jfotoptix.math.Vector3Pair;

/**
 * Describe a ray of light
 * <p>
 * This class is used to describe a ray of light. It contains
 * geometrical and physical information about a light ray.
 */
public class LightRay {
    protected Vector3Pair _ray;
    protected double _wavelen;
    protected double _intensity;

    public LightRay(Vector3Pair ray, double intensity, double wavelen) {
        this._ray = ray;
        this._intensity = intensity;
        this._wavelen = wavelen;
    }

    public LightRay(Vector3Pair ray) {
        this(ray, 1.0, SpectralLine.d);
    }

    public LightRay(LightRay ray) {
        this(ray._ray, ray._intensity, ray._wavelen);
    }

    public LightRay() {
        this(Vector3Pair.position_000_001);
    }

    public void set_intensity(double intensity) {
        this._intensity = intensity;
    }

    public void set_wavelen(double wavelen) {
        this._wavelen = wavelen;
    }

    public Vector3Pair get_ray() {
        return _ray;
    }

    public double get_intensity() {
        return _intensity;
    }

    public double get_wavelen() {
        return _wavelen;
    }

    public String toString() {
        return "LightRay{wavelen=" + _wavelen + ",origin=" + _ray.origin() + ",direction=" + _ray.direction() + '}';
    }

}
