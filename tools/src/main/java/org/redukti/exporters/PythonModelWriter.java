package org.redukti.exporters;

import org.redukti.exporters.ModelSpec.Op;
import org.redukti.spec.SurfaceType;
import org.redukti.spec.VigType;

/**
 * Renders a {@link ModelSpec} as an upstream ray-optics Python script.
 *
 * @see JavaModelWriter for the sibling that must stay in step with this one
 */
public class PythonModelWriter {

    public String write(ModelSpec spec) {
        StringBuilder sb = new StringBuilder();
        preamble(sb);
        for (Op op : spec.ops())
            emit(op, spec, sb);
        return sb.toString();
    }

    private void preamble(StringBuilder sb) {
        sb.append("from rayoptics.environment import *\n")
          .append("from rayoptics.raytr.trace import apply_paraxial_vignetting\n")
          .append("\n")
          .append("opm = OpticalModel()\n")
          .append("sm  = opm['seq_model']\n")
          .append("osp = opm['optical_spec']\n")
          .append("pm  = opm['parax_model']\n")
          .append("em  = opm['ele_model']\n")
          .append("pt  = opm['part_tree']\n")
          .append("ar  = opm['analysis_results']\n");
    }

    private void emit(Op op, ModelSpec spec, StringBuilder sb) {
        if (op instanceof ModelSpec.SetPupil) {
            sb.append("osp.pupil = PupilSpec(osp, key=['image', 'f/#'], value=")
              .append(num(spec.fno())).append(")\n");
        }
        else if (op instanceof ModelSpec.SetFieldSpec) {
            sb.append("osp.field_of_view = FieldSpec(osp, key=('object', 'angle'), value=")
              .append(num(spec.half_angle_degrees()))
              .append(", flds=").append(list(spec.fields()))
              .append(", is_relative=").append(bool(spec.is_relative()))
              .append(", is_wide_angle=").append(bool(spec.is_wide_angle()))
              .append(")\n");
        }
        else if (op instanceof ModelSpec.SetSpectralRegion) {
            double[] wvls = spec.wavelengths();
            double[] wts = spec.weights();
            sb.append("osp.spectral_region = WvlSpec([");
            for (int i = 0; i < wvls.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("(").append(num(wvls[i])).append(", ").append(num(wts[i])).append(")");
            }
            sb.append("], ref_wl=").append(spec.reference_wavelength_index()).append(")\n");
        }
        else if (op instanceof ModelSpec.SetTitle) {
            sb.append("opm.system_spec.title = '").append(escape(spec.title())).append("'\n")
              .append("opm.system_spec.dimensions = '").append(escape(spec.dimensions())).append("'\n");
        }
        else if (op instanceof ModelSpec.SetRadiusMode) {
            sb.append("opm.radius_mode = ").append(bool(spec.radius_mode())).append("\n");
        }
        else if (op instanceof ModelSpec.SetObjectDistance) {
            sb.append("sm.gaps[0].thi = ").append(num(spec.object_distance())).append("\n");
        }
        else if (op instanceof ModelSpec.AddSurface o) {
            SurfaceType s = o.surface();
            sb.append("sm.add_surface([").append(num(s.get_radius_of_curvature()))
              .append(", ").append(num(spec.thickness(s)));
            if (s.get_refractive_index() != 0.0) {
                if (s.get_glass_name() != null && s.get_catalog_name() != null) {
                    sb.append(", '").append(escape(s.get_glass_name())).append("', '")
                      .append(escape(s.get_catalog_name())).append("'");
                }
                else {
                    sb.append(", ").append(num(s.get_refractive_index()))
                      .append(", ").append(num(s.get_abbe_vd()));
                }
            }
            sb.append("], sd=").append(num(spec.semi_diameter(s))).append(")\n");
        }
        else if (op instanceof ModelSpec.SetProfile o) {
            SurfaceType s = o.surface();
            sb.append("sm.ifcs[sm.cur_surface].profile = ")
              .append(s.is_odd_asphere() ? "RadialPolynomial" : "EvenPolynomial")
              .append("(r=").append(num(s.get_radius_of_curvature()))
              .append(", cc=").append(num(s.get_cc()))
              .append(", coefs=").append(list(s.get_aspheric_coeffs())).append(")\n");
        }
        else if (op instanceof ModelSpec.SetStop) {
            sb.append("sm.set_stop()\n");
        }
        else if (op instanceof ModelSpec.SetDoApertures) {
            sb.append("sm.do_apertures = ").append(bool(spec.do_apertures())).append("\n");
        }
        else if (op instanceof ModelSpec.UpdateModel) {
            sb.append("opm.update_model()\n");
        }
        else if (op instanceof ModelSpec.ApplyVignetting) {
            sb.append(vignetting(spec.vig_type())).append("\n");
        }
        else {
            throw new IllegalStateException("No Python rendering for op " + op.getClass().getSimpleName());
        }
    }

    /**
     * All of these except the paraxial one are re-exported by
     * rayoptics.environment; apply_paraxial_vignetting is imported explicitly in
     * the preamble.
     */
    private String vignetting(VigType kind) {
        return switch (kind) {
            case Paraxial -> "apply_paraxial_vignetting(opm)";
            case SetVig -> "set_vignetting(opm)";
            case SetPupil -> "set_pupil(opm)";
            case SetStopAperture -> "set_stop_aperture(opm)";
            case SetApertures -> "set_apertures(opm)";
            case None -> throw new IllegalStateException("VigType.None should not be emitted");
        };
    }

    /**
     * Shortest round tripping decimal, matching Python's repr.
     * <p>
     * Double.toString spells the non-finite values Infinity, -Infinity and NaN,
     * none of which are Python names - a plane surface recorded as an infinite
     * radius would emit a script that dies with a NameError.
     */
    static String num(double value) {
        if (Double.isNaN(value))
            return "float('nan')";
        if (Double.isInfinite(value))
            return value > 0 ? "float('inf')" : "float('-inf')";
        return Double.toString(value);
    }

    static String bool(boolean value) {
        return value ? "True" : "False";
    }

    static String list(double[] values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(num(values[i]));
        }
        return sb.append("]").toString();
    }

    static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
