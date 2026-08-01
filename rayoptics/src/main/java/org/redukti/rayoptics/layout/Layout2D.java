package org.redukti.rayoptics.layout;

import org.redukti.mathlib.Vector2;
import org.redukti.mathlib.Vector2Pair;
import org.redukti.mathlib.Vector3;
import org.redukti.rayoptics.elem.surface.Surface;
import org.redukti.rayoptics.math.Tfm3d;
import org.redukti.rayoptics.optical.OpticalModel;
import org.redukti.rayoptics.raytr.RayPkg;
import org.redukti.rayoptics.raytr.RayResult;
import org.redukti.rayoptics.raytr.Trace;
import org.redukti.rayoptics.raytr.TraceFanDef;
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.RendererViewport;
import org.redukti.render.rendering.Rgb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Draws a static y-z (meridional) view of a rayoptics model. */
public final class Layout2D {
    private static final Rgb ELEMENT_COLOR = Rgb.rgb_black;
    private static final Rgb STOP_COLOR = Rgb.rgb_black;
    private static final Rgb AXIS_COLOR = Rgb.rgb_gray;
    private static final Rgb[] FIELD_COLORS = {
            Rgb.rgb_red, Rgb.rgb_blue, Rgb.rgb_green, Rgb.rgb_magenta,
            Rgb.rgb_cyan, Rgb.rgb_yellow
    };

    private static final double STOP_STROKE_WIDTH = 2.5;

    /** A renderer-independent path, retained until the viewport bounds are known. */
    private record Polyline(List<Vector2> points, Rgb color, double strokeWidth) {
        private Polyline(List<Vector2> points, Rgb color) {
            this(points, color, 1.0);
        }
    }

    /** Renders a complete layout into a standalone SVG document. */
    public String renderSvg(OpticalModel model, double width, double height, LayoutOptions options) {
        RendererSvg renderer = new RendererSvg(width, height);
        render(renderer, model, options);
        return renderer.write(new StringBuilder()).toString();
    }

    /**
     * Builds all requested geometry, fits the viewport around it, and submits
     * the resulting paths to the supplied renderer.
     */
    public void render(RendererViewport renderer, OpticalModel model, LayoutOptions options) {
        if (options == null) options = new LayoutOptions();
        if (options.surfaceSamples < 2) throw new IllegalArgumentException("surfaceSamples must be >= 2");
        if (options.margin < 0.0) throw new IllegalArgumentException("margin must be >= 0");
        if (options.useTraceFan && options.fanRayCount == 1)
            throw new IllegalArgumentException("Trace.trace_fan requires fanRayCount >= 2");

        // Rays can extend beyond the physical elements, so build every requested
        // path before calculating the viewport.
        ElementModel elementModel = new ElementModel(model);
        List<Polyline> geometry = new ArrayList<>();
        if (options.drawElements) addElements(geometry, model, elementModel, options.surfaceSamples);
        if (options.drawReferenceRays || options.fanRayCount > 0) addRays(geometry, model, options);

        Bounds bounds = bounds(geometry);
        if (!bounds.valid()) bounds = modelBounds(model);
        bounds.pad(options.margin);
        renderer.set_window(new Vector2Pair(new Vector2(bounds.minX, bounds.minY),
                new Vector2(bounds.maxX, bounds.maxY)), true);

        if (options.drawOpticalAxis)
            renderer.draw_segment(new Vector2(bounds.minX, 0.0), new Vector2(bounds.maxX, 0.0), AXIS_COLOR);
        for (Polyline line : geometry) {
            renderer.set_stroke_width(line.strokeWidth);
            if (line.points.size() == 2)
                renderer.draw_segment(line.points.get(0), line.points.get(1), line.color);
            else if (line.points.size() > 2)
                renderer.draw_polygon(line.points.toArray(new Vector2[0]), line.color, false, false);
        }
        renderer.set_stroke_width(1.0);
    }

    /** Dispatches inferred physical elements to their geometry builders. */
    private void addElements(List<Polyline> out, OpticalModel model, ElementModel elementModel, int samples) {
        SequentialModel sm = model.seq_model;
        for (Element element : elementModel.elements()) {
            if (element instanceof LensElement lensElement) {
                addLens(out, sm, lensElement, samples);
            } else if (element instanceof Stop stop) {
                addStop(out, sm, stop);
            } else if (element instanceof Aperture aperture) {
                addAperture(out, sm, aperture);
            } else if (element instanceof DummyInterface dummy && "Image".equals(dummy.label())) {
                addImagePlane(out, model, dummy);
            } else if (element instanceof Mirror mirror && mirror.surface() instanceof Surface surface) {
                addSurface(out, sm, mirror.surfaceIndex(), surface, surfaceRadius(surface), samples);
            }
        }
    }

    /** Implements Geopter's curvature-dependent DrawLens/DrawFlat construction. */
    private void addLens(List<Polyline> out, SequentialModel sm, LensElement lensElement, int samples) {
        double od1 = semiDiameter(lensElement.surface1());
        double od2 = semiDiameter(lensElement.surface2());
        double mechanicalRadius = Math.max(maxAperture(lensElement.surface1()), maxAperture(lensElement.surface2()));
        double cv1 = lensElement.surface1().profile.cv;
        double cv2 = lensElement.surface2().profile.cv;

        double drawRadius1;
        double drawRadius2;
        boolean flat1 = false;
        boolean flat2 = false;

        // These cases reproduce Geopter's choice of where a curved optical
        // profile ends and a vertical mechanical flat must begin.
        if (cv1 > 0.0 && cv2 < 0.0) {
            drawRadius1 = mechanicalRadius;
            drawRadius2 = mechanicalRadius;
        } else if ((cv1 > 0.0 && cv2 > 0.0) || (cv1 < 0.0 && cv2 < 0.0)) {
            if (cv1 - cv2 > 0.0) {
                drawRadius1 = mechanicalRadius;
                drawRadius2 = mechanicalRadius;
            } else if (od1 > od2) {
                drawRadius1 = mechanicalRadius;
                drawRadius2 = od2;
                flat2 = true;
            } else {
                drawRadius1 = od1;
                drawRadius2 = mechanicalRadius;
                flat1 = true;
            }
        } else {
            drawRadius1 = od1;
            drawRadius2 = od2;
            flat1 = true;
            flat2 = true;
        }

        addSurface(out, sm, lensElement.firstSurfaceIndex(), lensElement.surface1(), drawRadius1, samples);
        addSurface(out, sm, lensElement.secondSurfaceIndex(), lensElement.surface2(), drawRadius2, samples);
        if (flat1) addFlats(out, sm, lensElement.firstSurfaceIndex(), lensElement.surface1(), od1, mechanicalRadius);
        if (flat2) addFlats(out, sm, lensElement.secondSurfaceIndex(), lensElement.surface2(), od2, mechanicalRadius);
        addCommonEdges(out, sm, lensElement, drawRadius1, drawRadius2, mechanicalRadius);
    }

    /**
     * Samples a surface sag in its local meridional plane and transforms the
     * samples into global layout coordinates.
     */
    private void addSurface(List<Polyline> out, SequentialModel sm, int index, Surface surface,
                            double radius, int samples) {
        List<Vector2> points = new ArrayList<>();
        for (int i = 0; i < samples; i++) {
            double y = -radius + 2.0 * radius * i / (samples - 1.0);
            try {
                double sag = surface.profile.sag(0.0, y);
                if (Double.isFinite(sag)) points.add(toLayout(sm.gbl_tfrms.get(index), new Vector3(0.0, y, sag)));
            } catch (RuntimeException ignored) {
                flush(out, points, ELEMENT_COLOR);
                points = new ArrayList<>();
            }
        }
        flush(out, points, ELEMENT_COLOR);
    }

    /** Adds both flats between a clear optical profile and its mechanical radius. */
    private void addFlats(List<Polyline> out, SequentialModel sm, int surfaceIndex,
                          Surface surface, double profileRadius, double mechanicalRadius) {
        if (mechanicalRadius <= profileRadius) return;
        addFlat(out, sm, surfaceIndex, surface, profileRadius, mechanicalRadius);
        addFlat(out, sm, surfaceIndex, surface, -profileRadius, -mechanicalRadius);
    }

    /** Adds one radial flat, anchored at the sag of the optical profile edge. */
    private void addFlat(List<Polyline> out, SequentialModel sm, int surfaceIndex,
                         Surface surface, double fromY, double toY) {
        try {
            double sag = surface.profile.sag(0.0, fromY);
            Tfm3d tfm = sm.gbl_tfrms.get(surfaceIndex);
            out.add(new Polyline(List.of(
                    toLayout(tfm, new Vector3(0.0, fromY, sag)),
                    toLayout(tfm, new Vector3(0.0, toY, sag))), ELEMENT_COLOR));
        } catch (RuntimeException ignored) {
            // No flat can be anchored when the profile is invalid at its edge.
        }
    }

    /** Adds the upper and lower mechanical rims joining the lens surfaces. */
    private void addCommonEdges(List<Polyline> out, SequentialModel sm, LensElement lensElement,
                                double profileRadius1, double profileRadius2, double mechanicalRadius) {
        addCommonEdge(out, sm, lensElement, profileRadius1, profileRadius2, mechanicalRadius);
        addCommonEdge(out, sm, lensElement, -profileRadius1, -profileRadius2, -mechanicalRadius);
    }

    /**
     * Joins one side of a lens at a common mechanical height, retaining each
     * surface's sag where its curved profile or flat terminates.
     */
    private void addCommonEdge(List<Polyline> out, SequentialModel sm, LensElement lensElement,
                               double profileY1, double profileY2, double edgeY) {
        try {
            double sag1 = lensElement.surface1().profile.sag(0.0, profileY1);
            double sag2 = lensElement.surface2().profile.sag(0.0, profileY2);
            Vector2 p1 = toLayout(sm.gbl_tfrms.get(lensElement.firstSurfaceIndex()),
                    new Vector3(0.0, edgeY, sag1));
            Vector2 p2 = toLayout(sm.gbl_tfrms.get(lensElement.secondSurfaceIndex()),
                    new Vector3(0.0, edgeY, sag2));
            out.add(new Polyline(List.of(p1, p2), ELEMENT_COLOR));
        } catch (RuntimeException ignored) {
            // A mechanical aperture outside the valid profile has no drawable rim point.
        }
    }
    /** Adds the explicitly designated aperture stop using a heavier stroke. */
    private void addStop(List<Polyline> out, SequentialModel sm, Stop stop) {
        addApertureMarker(out, sm.gbl_tfrms.get(stop.surfaceIndex()),
                maxAperture(stop.referenceSurface()), STOP_STROKE_WIDTH);
    }

    /** Adds a field or mechanical aperture using the normal element stroke. */
    private void addAperture(List<Polyline> out, SequentialModel sm, Aperture aperture) {
        addApertureMarker(out, sm.gbl_tfrms.get(aperture.surfaceIndex()),
                maxAperture(aperture.referenceSurface()), 1.0);
    }

    /** Draws the two short bars outside an aperture's open clear diameter. */
    private void addApertureMarker(List<Polyline> out, Tfm3d tfm, double radius, double strokeWidth) {
        double outer = radius * 1.2;
        out.add(new Polyline(List.of(toLayout(tfm, new Vector3(0, radius, 0)),
                toLayout(tfm, new Vector3(0, outer, 0))), STOP_COLOR, strokeWidth));
        out.add(new Polyline(List.of(toLayout(tfm, new Vector3(0, -radius, 0)),
                toLayout(tfm, new Vector3(0, -outer, 0))), STOP_COLOR, strokeWidth));
    }

    /** Draws the image interface, spanning at least the paraxial image height. */
    private void addImagePlane(List<Polyline> out, OpticalModel model, DummyInterface image) {
        double radius = maxAperture(image.surface());
        try {
            if (model.optical_spec.parax_data != null && model.optical_spec.parax_data.fod != null)
                radius = Math.max(radius, Math.abs(model.optical_spec.parax_data.fod.img_ht));
        } catch (RuntimeException ignored) {}
        Tfm3d tfm = model.seq_model.gbl_tfrms.get(image.surfaceIndex());
        out.add(new Polyline(List.of(toLayout(tfm, new Vector3(0, -radius, 0)),
                toLayout(tfm, new Vector3(0, radius, 0))), ELEMENT_COLOR));
    }

    /**
     * Traces chief/marginal reference rays and optional pupil fans for every
     * configured field, assigning one display color per field.
     */
    private void addRays(List<Polyline> out, OpticalModel model, LayoutOptions options) {
        var fields = model.optical_spec.fov.fields;
        double wavelength = model.seq_model.central_wavelength();
        for (int fi = 0; fi < fields.length; fi++) {
            Field field = fields[fi];
            Rgb color = FIELD_COLORS[fi % FIELD_COLORS.length];
            if (options.drawReferenceRays) {
                // Pupil centre is the chief ray; named +/-Y rays are the
                // upper and lower meridional pupil boundaries.
                TraceOptions traceOptions = traceOptions(options);
                RayResult chief = Trace.trace_ray(model, new Vector2(0, 0), field, wavelength, traceOptions);
                addRay(out, model.seq_model, chief.pkg, color);
                List<RayPkg> boundary = Trace.trace_boundary_rays_at_field(model, field, wavelength, traceOptions(options));
                Map<String, RayPkg> named = Trace.boundary_ray_dict(model, boundary);
                addRay(out, model.seq_model, named.get("+Y"), color);
                addRay(out, model.seq_model, named.get("-Y"), color);
            }
            if (options.fanRayCount > 0) {
                if (options.useTraceFan)
                    addTraceFan(out, model, field, wavelength, color, options);
                else
                    addDirectFan(out, model, field, wavelength, color, options);
            }
        }
    }

    /** Traces each normalized pupil coordinate independently, retaining partial failed rays. */
    private void addDirectFan(List<Polyline> out, OpticalModel model, Field field,
                              double wavelength, Rgb color, LayoutOptions options) {
        int count = options.fanRayCount;
        for (int i = 0; i < count; i++) {
            double py = count == 1 ? 0.0 : -1.0 + 2.0 * i / (count - 1.0);
            RayResult result = Trace.trace_ray(model, new Vector2(0, py), field,
                    wavelength, traceOptions(options));
            addRay(out, model.seq_model, result.pkg, color);
        }
    }

    /**
     * Uses Trace.trace_fan unchanged. Its current error filter omits rays which
     * terminate before reaching the image plane.
     */
    private void addTraceFan(List<Polyline> out, OpticalModel model, Field field,
                             double wavelength, Rgb color, LayoutOptions options) {
        TraceFanDef fan = new TraceFanDef(new Vector2(0.0, -1.0),
                new Vector2(0.0, 1.0), options.fanRayCount);
        double focus = model.optical_spec.defocus().get_focus();
        var traced = Trace.trace_fan(model, fan, field, wavelength, focus,
                null, traceOptions(options));
        for (var item : traced)
            addRay(out, model.seq_model, item.ray_pkg, color);
    }
    /** Maps layout ray settings onto the tracing subsystem's options. */
    private TraceOptions traceOptions(LayoutOptions options) {
        TraceOptions result = new TraceOptions();
        result.check_apertures = options.clipRays;
        result.rayerr_filter = "full";
        return result;
    }

    /** Converts local traced-ray intersections into one global layout path. */
    private void addRay(List<Polyline> out, SequentialModel sm, RayPkg ray, Rgb color) {
        if (ray == null || ray.ray == null || ray.ray.size() < 2) return;
        int count = Math.min(ray.ray.size(), sm.gbl_tfrms.size());
        List<Vector2> points = new ArrayList<>();
        // Omit the object-to-first-surface segment; infinite conjugates otherwise dominate the view.
        for (int i = 1; i < count; i++)
            points.add(toLayout(sm.gbl_tfrms.get(i), ray.ray.get(i).p));
        flush(out, points, color);
    }

    /** Beam43 max_aperture corresponds to Geopter's traced SemiDiameter. */
    private static double semiDiameter(Interface surface) {
        return Math.max(Math.abs(surface.max_aperture), 1.0e-9);
    }

    /** Geopter MaxAperture is the larger of semi-diameter and explicit aperture extent. */
    private static double maxAperture(Interface surface) {
        double radius = semiDiameter(surface);
        try {
            double surfaceOd = surface.surface_od();
            if (Double.isFinite(surfaceOd)) radius = Math.max(radius, Math.abs(surfaceOd));
        } catch (RuntimeException ignored) {}
        return radius;
    }

    /** Returns the radial extent used to draw or bound a standalone surface. */
    private static double surfaceRadius(Interface surface) {
        return maxAperture(surface);
    }

    /**
     * Transforms a local point to global coordinates, then projects it onto
     * the layout's z-horizontal/y-vertical meridional plane.
     */
    private static Vector2 toLayout(Tfm3d tfm, Vector3 local) {
        Vector3 global = tfm.rt.multiply(local).add(tfm.t);
        return new Vector2(global.z, global.y);
    }

    /** Stores a sampled path when it contains at least one drawable segment. */
    private static void flush(List<Polyline> out, List<Vector2> points, Rgb color) {
        if (points.size() >= 2) out.add(new Polyline(List.copyOf(points), color));
    }

    /** Calculates an axis-aligned box around all generated paths. */
    private static Bounds bounds(List<Polyline> lines) {
        Bounds bounds = new Bounds();
        for (Polyline line : lines)
            for (Vector2 point : line.points) bounds.add(point);
        return bounds;
    }

    /** Supplies physical-model bounds when no requested geometry was generated. */
    private static Bounds modelBounds(OpticalModel model) {
        Bounds bounds = new Bounds();
        SequentialModel sm = model.seq_model;
        for (int i = 1; i < sm.ifcs.size(); i++) {
            double r = surfaceRadius(sm.ifcs.get(i));
            Tfm3d tfm = sm.gbl_tfrms.get(i);
            bounds.add(toLayout(tfm, new Vector3(0, -r, 0)));
            bounds.add(toLayout(tfm, new Vector3(0, r, 0)));
        }
        return bounds;
    }

    /** Mutable bounds accumulator used while fitting the renderer viewport. */
    private static final class Bounds {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        /** Expands this box to contain a finite point. */
        void add(Vector2 p) {
            if (!Double.isFinite(p.x()) || !Double.isFinite(p.y())) return;
            minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y()); maxY = Math.max(maxY, p.y());
        }
        /** Reports whether at least one finite point has been accumulated. */
        boolean valid() { return minX <= maxX && minY <= maxY; }
        /** Adds a scale-relative margin, with a minimum for degenerate models. */
        void pad(double margin) {
            double dx = maxX - minX, dy = maxY - minY;
            double pad = Math.max(Math.max(dx, dy) * margin, 1.0e-6);
            minX -= pad; maxX += pad; minY -= pad; maxY += pad;
        }
    }
}
