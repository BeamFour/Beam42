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
import org.redukti.rayoptics.raytr.TraceOptions;
import org.redukti.rayoptics.seq.Interface;
import org.redukti.rayoptics.seq.SequentialModel;
import org.redukti.rayoptics.specs.Field;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.RendererViewport;
import org.redukti.render.rendering.Rgb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Draws a static y-z (meridional) view of a rayoptics model. */
public final class SystemLayout2D {
    private static final Rgb ELEMENT_COLOR = Rgb.rgb_black;
    private static final Rgb STOP_COLOR = Rgb.rgb_black;
    private static final Rgb AXIS_COLOR = Rgb.rgb_gray;
    private static final Rgb[] FIELD_COLORS = {
            Rgb.rgb_red, Rgb.rgb_green, Rgb.rgb_blue, Rgb.rgb_magenta,
            Rgb.rgb_cyan, Rgb.rgb_yellow
    };

    private record Polyline(List<Vector2> points, Rgb color) {}

    public String renderSvg(OpticalModel model, double width, double height, LayoutOptions options) {
        RendererSvg renderer = new RendererSvg(width, height);
        render(renderer, model, options);
        return renderer.write(new StringBuilder()).toString();
    }

    public void render(RendererViewport renderer, OpticalModel model, LayoutOptions options) {
        if (options == null) options = new LayoutOptions();
        if (options.surfaceSamples < 2) throw new IllegalArgumentException("surfaceSamples must be >= 2");
        if (options.margin < 0.0) throw new IllegalArgumentException("margin must be >= 0");

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
            if (line.points.size() == 2)
                renderer.draw_segment(line.points.get(0), line.points.get(1), line.color);
            else if (line.points.size() > 2)
                renderer.draw_polygon(line.points.toArray(new Vector2[0]), line.color, false, false);
        }
    }

    private void addElements(List<Polyline> out, OpticalModel model, ElementModel elementModel, int samples) {
        SequentialModel sm = model.seq_model;
        Set<Integer> drawnSurfaces = new HashSet<>();
        for (Element element : elementModel.elements()) {
            if (element instanceof Lens lens) {
                addSurface(out, sm, lens.firstSurfaceIndex(), lens.surface1(), samples, drawnSurfaces);
                addSurface(out, sm, lens.secondSurfaceIndex(), lens.surface2(), samples, drawnSurfaces);
                addLensEdges(out, sm, lens);
            } else if (element instanceof Stop stop) {
                addStop(out, sm, stop);
            } else if (element instanceof Mirror mirror && mirror.surface() instanceof Surface surface) {
                addSurface(out, sm, mirror.surfaceIndex(), surface, samples, drawnSurfaces);
            }
        }
    }

    private void addSurface(List<Polyline> out, SequentialModel sm, int index, Surface surface,
                            int samples, Set<Integer> drawn) {
        if (!drawn.add(index)) return;
        double radius = surfaceRadius(surface);
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

    private void addLensEdges(List<Polyline> out, SequentialModel sm, Lens lens) {
        double r1 = surfaceRadius(lens.surface1());
        double r2 = surfaceRadius(lens.surface2());
        addEdge(out, sm, lens, r1, r2);
        addEdge(out, sm, lens, -r1, -r2);
    }

    private void addEdge(List<Polyline> out, SequentialModel sm, Lens lens, double y1, double y2) {
        try {
            Vector2 p1 = toLayout(sm.gbl_tfrms.get(lens.firstSurfaceIndex()),
                    new Vector3(0.0, y1, lens.surface1().profile.sag(0.0, y1)));
            Vector2 p2 = toLayout(sm.gbl_tfrms.get(lens.secondSurfaceIndex()),
                    new Vector3(0.0, y2, lens.surface2().profile.sag(0.0, y2)));
            out.add(new Polyline(List.of(p1, p2), ELEMENT_COLOR));
        } catch (RuntimeException ignored) {
            // A mechanical aperture outside the valid profile has no drawable rim point.
        }
    }

    private void addStop(List<Polyline> out, SequentialModel sm, Stop stop) {
        double radius = Math.abs(stop.referenceSurface().max_aperture);
        double outer = radius * 1.2;
        Tfm3d tfm = sm.gbl_tfrms.get(stop.surfaceIndex());
        out.add(new Polyline(List.of(toLayout(tfm, new Vector3(0, radius, 0)),
                toLayout(tfm, new Vector3(0, outer, 0))), STOP_COLOR));
        out.add(new Polyline(List.of(toLayout(tfm, new Vector3(0, -radius, 0)),
                toLayout(tfm, new Vector3(0, -outer, 0))), STOP_COLOR));
    }

    private void addRays(List<Polyline> out, OpticalModel model, LayoutOptions options) {
        var fields = model.optical_spec.fov.fields;
        double wavelength = model.seq_model.central_wavelength();
        for (int fi = 0; fi < fields.length; fi++) {
            Field field = fields[fi];
            Rgb color = FIELD_COLORS[fi % FIELD_COLORS.length];
            if (options.drawReferenceRays) {
                TraceOptions traceOptions = traceOptions(options);
                RayResult chief = Trace.trace_ray(model, new Vector2(0, 0), field, wavelength, traceOptions);
                addRay(out, model.seq_model, chief.pkg, color);
                List<RayPkg> boundary = Trace.trace_boundary_rays_at_field(model, field, wavelength, traceOptions(options));
                Map<String, RayPkg> named = Trace.boundary_ray_dict(model, boundary);
                addRay(out, model.seq_model, named.get("+Y"), color);
                addRay(out, model.seq_model, named.get("-Y"), color);
            }
            if (options.fanRayCount > 0) {
                int count = options.fanRayCount;
                for (int i = 0; i < count; i++) {
                    double py = count == 1 ? 0.0 : -1.0 + 2.0 * i / (count - 1.0);
                    RayResult result = Trace.trace_ray(model, new Vector2(0, py), field,
                            wavelength, traceOptions(options));
                    addRay(out, model.seq_model, result.pkg, color);
                }
            }
        }
    }

    private TraceOptions traceOptions(LayoutOptions options) {
        TraceOptions result = new TraceOptions();
        result.check_apertures = options.clipRays;
        result.rayerr_filter = "full";
        return result;
    }

    private void addRay(List<Polyline> out, SequentialModel sm, RayPkg ray, Rgb color) {
        if (ray == null || ray.ray == null || ray.ray.size() < 2) return;
        int count = Math.min(ray.ray.size(), sm.gbl_tfrms.size());
        List<Vector2> points = new ArrayList<>();
        // Omit the object-to-first-surface segment; infinite conjugates otherwise dominate the view.
        for (int i = 1; i < count; i++)
            points.add(toLayout(sm.gbl_tfrms.get(i), ray.ray.get(i).p));
        flush(out, points, color);
    }

    private static double surfaceRadius(Interface surface) {
        try {
            double radius = surface.surface_od();
            if (Double.isFinite(radius) && radius > 0.0) return radius;
        } catch (RuntimeException ignored) {}
        return Math.max(Math.abs(surface.max_aperture), 1.0e-9);
    }

    private static Vector2 toLayout(Tfm3d tfm, Vector3 local) {
        Vector3 global = tfm.rt.multiply(local).add(tfm.t);
        return new Vector2(global.z, global.y);
    }

    private static void flush(List<Polyline> out, List<Vector2> points, Rgb color) {
        if (points.size() >= 2) out.add(new Polyline(List.copyOf(points), color));
    }

    private static Bounds bounds(List<Polyline> lines) {
        Bounds bounds = new Bounds();
        for (Polyline line : lines)
            for (Vector2 point : line.points) bounds.add(point);
        return bounds;
    }

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

    private static final class Bounds {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        void add(Vector2 p) {
            if (!Double.isFinite(p.x()) || !Double.isFinite(p.y())) return;
            minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y()); maxY = Math.max(maxY, p.y());
        }
        boolean valid() { return minX <= maxX && minY <= maxY; }
        void pad(double margin) {
            double dx = maxX - minX, dy = maxY - minY;
            double pad = Math.max(Math.max(dx, dy) * margin, 1.0e-6);
            minX -= pad; maxX += pad; minY -= pad; maxY += pad;
        }
    }
}
