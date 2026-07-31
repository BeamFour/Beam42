package org.redukti.rayoptics.layout;

/** Options for a static meridional optical-system layout. */
public final class LayoutOptions {
    public boolean drawOpticalAxis = true;
    public boolean drawElements = true;
    public boolean drawReferenceRays = true;
    public int fanRayCount = 0;
    public boolean clipRays = false;
    public int surfaceSamples = 101;
    public double margin = 0.05;

    public LayoutOptions fanRayCount(int count) { this.fanRayCount = count; return this; }
    public LayoutOptions drawReferenceRays(boolean value) { this.drawReferenceRays = value; return this; }
    public LayoutOptions clipRays(boolean value) { this.clipRays = value; return this; }
}
