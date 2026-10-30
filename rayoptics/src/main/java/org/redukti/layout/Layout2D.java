// This code is part of Beam42 project (https://github.com/BeamFour/Beam42)
// Copyright 2025-2026 by Dibyendu Majumdar
// License GPL v3
package org.redukti.layout;

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
import org.redukti.render.rendering.Renderer;
import org.redukti.render.rendering.RendererSvg;
import org.redukti.render.rendering.RendererViewport;
import org.redukti.render.rendering.Rgb;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Draws a static y-z (meridional) view of a rayoptics model. */
public final class Layout2D {
    private static final Rgb ELEMENT_COLOR = Rgb.rgb_black;
    private static final Rgb STOP_COLOR = Rgb.rgb_black;
    private static final Rgb AXIS_COLOR = Rgb.rgb_gray;
    private static final Rgb[] FIELD_COLORS = {
            Rgb.rgb_red, Rgb.rgb_blue, Rgb.rgb_green, Rgb.rgb_magenta,
            Rgb.rgb_cyan, Rgb.rgb_yellow
    };

    private static final Rgb CHIEF_RAY_COLOR = Rgb.rgb_red;
    private static final Rgb RIM_RAY_COLOR = Rgb.rgb_blue;
    private static final Rgb SPHERE_COLOR = Rgb.rgb_green;

    private static final double STOP_STROKE_WIDTH = 2.5;
    private static final double SPHERE_STROKE_WIDTH = 1.5;
    private static final String VIRTUAL_RAY_DASHES = "6,4";
    private static final int SPHERE_SAMPLES = 65;
    /** A reference surface extends past the rim rays by this fraction of its span. */
    private static final double SPHERE_OVERHANG = 0.1;
    /** Length of the incident rays in front of their first feature, relative to the lens. */
    private static final double RAY_LEAD = 0.15;
    /** A pupil further than this from the lens, in lens lengths, is not drawn. */
    private static final double MAX_PUPIL_DISTANCE = 3.0;
    private static final int LABEL_SIZE = 12;
    private static final double CAPTION_LINE_PIXELS = 16.0;

    /** A renderer-independent path, retained until the viewport bounds are known. */
    private record Polyline(List<Vector2> points, Rgb color, double strokeWidth, String dashes) {
        private Polyline(List<Vector2> points, Rgb color) {
            this(points, color, 1.0, null);
        }
        private Polyline(List<Vector2> points, Rgb color, double strokeWidth) {
            this(points, color, strokeWidth, null);
        }
    }

    /** A marked point; retained like Polyline, and not counted in the viewport. */
    private record Marker(Vector2 point, String label, Rgb color, Renderer.PointStyle style) {}

    /** Reference sphere annotations gathered while the geometry is built. */
    private static final class Annotations {
        final List<Marker> markers = new ArrayList<>();
        final List<String> caption = new ArrayList<>();
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
        Annotations annotations = new Annotations();
        if (options.referenceSphereField != null)
            addReferenceSpheres(geometry, annotations, model, options);

        Bounds bounds = bounds(geometry);
        if (!bounds.valid()) bounds = modelBounds(model);
        bounds.pad(options.margin);
        double unitsPerPixel = 0.0;
        if (!annotations.caption.isEmpty()) {
            // The caption has a fixed size in pixels, so reserve it at the final scale
            Vector2 resolution = renderer.get_2d_output_res();
            double captionPixels = annotations.caption.size() * CAPTION_LINE_PIXELS + CAPTION_LINE_PIXELS / 2.0;
            unitsPerPixel = Math.max((bounds.maxX - bounds.minX) / resolution.x(),
                    (bounds.maxY - bounds.minY) / (resolution.y() - captionPixels));
            bounds.maxY += captionPixels * unitsPerPixel;
        }
        renderer.set_window(new Vector2Pair(new Vector2(bounds.minX, bounds.minY),
                new Vector2(bounds.maxX, bounds.maxY)), true);

        if (options.drawOpticalAxis)
            renderer.draw_segment(new Vector2(bounds.minX, 0.0), new Vector2(bounds.maxX, 0.0), AXIS_COLOR);
        for (Polyline line : geometry) {
            renderer.set_stroke_width(line.strokeWidth);
            renderer.set_stroke_dasharray(line.dashes);
            if (line.points.size() == 2)
                renderer.draw_segment(line.points.get(0), line.points.get(1), line.color);
            else if (line.points.size() > 2)
                renderer.draw_polygon(line.points.toArray(new Vector2[0]), line.color, false, false);
        }
        renderer.set_stroke_width(1.0);
        renderer.set_stroke_dasharray(null);
        drawAnnotations(renderer, annotations, bounds, unitsPerPixel);
    }

    /** Draws the pupil markers, then the caption at the top left of the drawing. */
    private void drawAnnotations(RendererViewport renderer, Annotations annotations, Bounds bounds,
                                 double unitsPerPixel) {
        Vector2 right = new Vector2(1.0, 0.0);
        for (Marker marker : annotations.markers) {
            renderer.draw_point(marker.point, marker.color, marker.style);
            if (marker.label != null)
                renderer.draw_text(marker.point, right, marker.label,
                        EnumSet.of(Renderer.TextAlignMask.TextAlignLeft, Renderer.TextAlignMask.TextAlignBottom),
                        LABEL_SIZE, marker.color);
        }
        double y = bounds.maxY;
        for (String line : annotations.caption) {
            renderer.draw_text(new Vector2(bounds.minX, y), right, line,
                    EnumSet.of(Renderer.TextAlignMask.TextAlignLeft, Renderer.TextAlignMask.TextAlignTop),
                    LABEL_SIZE, ELEMENT_COLOR);
            y -= CAPTION_LINE_PIXELS * unitsPerPixel;
        }
    }

    /**
     * Overlays one field's chief and rim rays and its reference spheres. Object
     * and image space segments are continued to the spheres, dashed where the
     * continuation is virtual.
     */
    private void addReferenceSpheres(List<Polyline> out, Annotations annotations,
                                     OpticalModel model, LayoutOptions options) {
        SequentialModel sm = model.seq_model;
        Field field = model.optical_spec.fov.fields[options.referenceSphereField];
        double wavelength = sm.central_wavelength();
        ReferenceSpheres spheres = ReferenceSpheres.compute(model, field, wavelength, traceOptions(options));

        double firstZ = sm.gbl_tfrms.get(1).t.z;
        double lastZ = sm.gbl_tfrms.get(sm.gbl_tfrms.size() - 2).t.z;
        double imageZ = sm.gbl_tfrms.get(sm.gbl_tfrms.size() - 1).t.z;
        double length = Math.abs(imageZ - firstZ);
        boolean drawEntrance = nearLens(spheres.entrance, firstZ, imageZ, length);
        boolean drawExit = nearLens(spheres.exit, firstZ, imageZ, length);

        addReferenceRay(out, sm, spheres.chief, spheres, CHIEF_RAY_COLOR, drawEntrance, drawExit, length);
        for (ReferenceSpheres.Ray rim : spheres.rims)
            addReferenceRay(out, sm, rim, spheres, RIM_RAY_COLOR, drawEntrance, drawExit, length);

        List<ReferenceSpheres.Ray> rays = new ArrayList<>(spheres.rims);
        rays.add(spheres.chief);
        if (drawEntrance) addSection(out, annotations, spheres.entrance, "E", spheres.chief.incidentDir(),
                rays.stream().map(ReferenceSpheres.Ray::entranceHit).toList());
        if (drawExit) addSection(out, annotations, spheres.exit, "E'", spheres.chief.emergentDir(),
                rays.stream().map(ReferenceSpheres.Ray::exitHit).filter(Objects::nonNull).toList());

        annotations.caption.add(String.format(Locale.ROOT,
                "Field %.3f, %.1f deg, %.1f nm. Red chief ray, blue rim rays, green reference surfaces, dashed virtual, grey paraxial pupils",
                Math.max(Math.abs(field.x), Math.abs(field.y)),
                Math.toDegrees(Math.atan2(spheres.chief.incidentDir().y(), spheres.chief.incidentDir().x())),
                wavelength));
        annotations.caption.add(String.format(Locale.ROOT, "E %s, %s from first surface, paraxial %.3f%s",
                spheres.entrance.isPlane() ? "(plane)" : String.format(Locale.ROOT, "(R %.3f)", spheres.entrance.radius()),
                String.format(Locale.ROOT, "%.3f", spheres.entrance.pupil().x() - firstZ),
                spheres.entrance.paraxialPupil().x() - firstZ, drawEntrance ? "" : ", not drawn"));
        annotations.caption.add(String.format(Locale.ROOT, "E' (R %.3f), %.3f from last surface, paraxial %.3f%s",
                spheres.exit.radius(), spheres.exit.pupil().x() - lastZ,
                spheres.exit.paraxialPupil().x() - lastZ, drawExit ? "" : ", not drawn"));
    }

    /** Reports whether a pupil is close enough to the lens to share a drawing with it. */
    private static boolean nearLens(ReferenceSpheres.Section section, double firstZ, double imageZ, double length) {
        double z = section.pupil().x();
        double limit = MAX_PUPIL_DISTANCE * length;
        return Double.isFinite(z) && z > Math.min(firstZ, imageZ) - limit && z < Math.max(firstZ, imageZ) + limit;
    }

    /** Draws a ray through the lens, its incident lead-in and its continuations to the spheres. */
    private void addReferenceRay(List<Polyline> out, SequentialModel sm, ReferenceSpheres.Ray ray,
                                 ReferenceSpheres spheres, Rgb color,
                                 boolean drawEntrance, boolean drawExit, double length) {
        addRay(out, sm, ray.pkg(), color);

        Vector2 first = ray.firstPoint();
        Vector2 incident = ray.incidentDir();
        double hitDistance = drawEntrance ? alongRay(first, incident, ray.entranceHit()) : 0.0;
        double lead = length * RAY_LEAD - Math.min(hitDistance, 0.0);
        Vector2 centre = spheres.entrance.centre();
        if (centre != null)
            lead = Math.min(lead, alongRay(centre, incident, first));
        out.add(new Polyline(List.of(pointAlong(first, incident, -lead), first), color));
        if (hitDistance > 0.0)
            out.add(new Polyline(List.of(first, ray.entranceHit()), color, 1.0, VIRTUAL_RAY_DASHES));

        Vector2 exitHit = ray.exitHit();
        if (drawExit && exitHit != null && alongRay(ray.lastPoint(), ray.emergentDir(), exitHit) < 0.0)
            out.add(new Polyline(List.of(ray.lastPoint(), exitHit), color, 1.0, VIRTUAL_RAY_DASHES));
    }

    /** Draws a reference surface over the span of the rays, and marks its pupil. */
    private void addSection(List<Polyline> out, Annotations annotations, ReferenceSpheres.Section section,
                            String label, Vector2 chiefDirection, List<Vector2> hits) {
        Vector2 pupil = section.pupil();
        List<Vector2> points = new ArrayList<>();
        if (section.isPlane()) {
            Vector2 tangent = new Vector2(-chiefDirection.y(), chiefDirection.x());
            double low = 0.0, high = 0.0;
            for (Vector2 hit : hits) {
                double offset = alongRay(pupil, tangent, hit);
                low = Math.min(low, offset);
                high = Math.max(high, offset);
            }
            double overhang = (high - low) * SPHERE_OVERHANG;
            points.add(pointAlong(pupil, tangent, low - overhang));
            points.add(pointAlong(pupil, tangent, high + overhang));
        } else {
            Vector2 centre = section.centre();
            double reference = angle(centre, pupil);
            double low = 0.0, high = 0.0;
            for (Vector2 hit : hits) {
                double delta = Math.IEEEremainder(angle(centre, hit) - reference, 2.0 * Math.PI);
                low = Math.min(low, delta);
                high = Math.max(high, delta);
            }
            double overhang = (high - low) * SPHERE_OVERHANG;
            for (int i = 0; i < SPHERE_SAMPLES; i++) {
                double theta = reference + low - overhang
                        + (high - low + 2.0 * overhang) * i / (SPHERE_SAMPLES - 1.0);
                points.add(pointAlong(centre, new Vector2(Math.cos(theta), Math.sin(theta)), section.radius()));
            }
        }
        out.add(new Polyline(points, SPHERE_COLOR, SPHERE_STROKE_WIDTH));
        annotations.markers.add(new Marker(pupil, label, SPHERE_COLOR, Renderer.PointStyle.PointStyleCross));
        annotations.markers.add(new Marker(section.paraxialPupil(), null, AXIS_COLOR, Renderer.PointStyle.PointStyleRound));
    }

    private static double alongRay(Vector2 from, Vector2 direction, Vector2 to) {
        return (to.x() - from.x()) * direction.x() + (to.y() - from.y()) * direction.y();
    }

    private static Vector2 pointAlong(Vector2 from, Vector2 direction, double distance) {
        return new Vector2(from.x() + direction.x() * distance, from.y() + direction.y() * distance);
    }

    private static double angle(Vector2 centre, Vector2 point) {
        return Math.atan2(point.y() - centre.y(), point.x() - centre.x());
    }

    /** Dispatches inferred physical elements to their geometry builders. */
    private void addElements(List<Polyline> out, OpticalModel model, ElementModel elementModel, int samples) {
        SequentialModel sm = model.seq_model;
        for (Element element : elementModel.elements()) {
            if (element instanceof LensElement lensElement) {
                addLens(out, sm, lensElement, samples);
            } else if (element instanceof CementedElement cementedElement) {
                addCementedElement(out, sm, cementedElement, samples);
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
     * Draws a cemented assembly while emitting every shared optical profile
     * once. Each material gap still receives its own upper and lower rim.
     */
    private void addCementedElement(List<Polyline> out, SequentialModel sm,
                                    CementedElement element, int samples) {
        List<LensElement> lenses = new ArrayList<>();
        List<LensDrawing> drawings = new ArrayList<>();
        Map<Integer, SurfaceDrawing> surfaces = new LinkedHashMap<>();

        for (int i = 0; i < element.gaps().size(); i++) {
            LensElement lens = new LensElement(element.surfaceIndices().get(i),
                    element.surfaceIndices().get(i + 1), element.surfaces().get(i),
                    element.surfaces().get(i + 1), element.gaps().get(i));
            LensDrawing drawing = lensDrawing(lens);
            lenses.add(lens);
            drawings.add(drawing);
            mergeSurfaceDrawing(surfaces, lens.firstSurfaceIndex(), lens.surface1(),
                    drawing.drawRadius1, drawing.flat1, drawing.mechanicalRadius);
            mergeSurfaceDrawing(surfaces, lens.secondSurfaceIndex(), lens.surface2(),
                    drawing.drawRadius2, drawing.flat2, drawing.mechanicalRadius);
        }

        for (var entry : surfaces.entrySet()) {
            SurfaceDrawing drawing = entry.getValue();
            addSurface(out, sm, entry.getKey(), drawing.surface, drawing.profileRadius, samples);
            if (drawing.flat)
                addFlats(out, sm, entry.getKey(), drawing.surface,
                        drawing.profileRadius, drawing.mechanicalRadius);
        }
        for (int i = 0; i < lenses.size(); i++) {
            LensDrawing drawing = drawings.get(i);
            addCommonEdges(out, sm, lenses.get(i), drawing.drawRadius1,
                    drawing.drawRadius2, drawing.mechanicalRadius);
        }
    }

    /** Combines the drawing requests made for a surface shared by two glass gaps. */
    private static void mergeSurfaceDrawing(Map<Integer, SurfaceDrawing> drawings, int index,
                                            Surface surface, double profileRadius, boolean flat,
                                            double mechanicalRadius) {
        SurfaceDrawing previous = drawings.get(index);
        if (previous == null) {
            drawings.put(index, new SurfaceDrawing(surface, profileRadius, flat, mechanicalRadius));
        } else {
            drawings.put(index, new SurfaceDrawing(surface,
                    Math.max(previous.profileRadius, profileRadius), previous.flat || flat,
                    Math.max(previous.mechanicalRadius, mechanicalRadius)));
        }
    }

    /** Calculates the same per-gap curvature choices used by addLens. */
    private static LensDrawing lensDrawing(LensElement lens) {
        double od1 = semiDiameter(lens.surface1());
        double od2 = semiDiameter(lens.surface2());
        double mechanicalRadius = Math.max(maxAperture(lens.surface1()), maxAperture(lens.surface2()));
        double cv1 = lens.surface1().profile.cv;
        double cv2 = lens.surface2().profile.cv;
        double drawRadius1;
        double drawRadius2;
        boolean flat1 = false;
        boolean flat2 = false;

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
        return new LensDrawing(drawRadius1, drawRadius2, flat1, flat2, mechanicalRadius);
    }

    private record LensDrawing(double drawRadius1, double drawRadius2, boolean flat1, boolean flat2,
                               double mechanicalRadius) {}

    private record SurfaceDrawing(Surface surface, double profileRadius, boolean flat,
                                  double mechanicalRadius) {}

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
        var traced = Trace.trace_fan(model, fan, field, wavelength, focus, false,
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
