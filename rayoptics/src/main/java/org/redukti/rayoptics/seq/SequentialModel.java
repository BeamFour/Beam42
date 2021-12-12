package org.redukti.rayoptics.seq;

/**
 * Manager class for a sequential optical model
 *
 *     A sequential optical model is a sequence of surfaces and gaps.
 *
 *     The sequential model has this structure
 *     <pre>
 *
 *         IfcObj  Ifc1  Ifc2  Ifc3 ... Ifci-1   IfcImg
 *              \  /  \  /  \  /             \   /
 *              GObj   G1    G2              Gi-1
 *
 *     where
 *
 *         - Ifc is a :class:`~rayoptics.seq.interface.Interface` instance
 *         - G   is a :class:`~rayoptics.seq.gap.Gap` instance
 *
 *     </pre>
 *
 *     There are N interfaces and N-1 gaps. The initial configuration has an
 *     object and image Surface and an object gap.
 *
 *     The Interface API supports implementation of an optical action, such as
 *     refraction, reflection, scatter, diffraction, etc. The Interface may be
 *     realized as a physical profile separating the adjacent gaps or an idealized
 *     object, such as a thin lens or 2 point HOE.
 *
 *     The Gap class maintains a simple separation (z translation) and the medium
 *     filling the gap. More complex coordinate transformations are handled
 *     through the Interface API.
 *
 *     Attributes:
 *         opt_model: parent optical model
 *         ifcs: list of :class:`~rayoptics.seq.interface.Interface`
 *         gaps: list of :class:`~rayoptics.seq.gap.Gap`
 *         lcl_tfrms: forward transform, interface to interface
 *         rndx: a list with refractive indices for all **wvls**
 *         z_dir: -1 if gap follows an odd number of reflections, otherwise +1
 *         gbl_tfrms: global coordinates of each interface wrt the 1st interface
 *         stop_surface (int): index of stop interface
 *         cur_surface (int): insertion index for next interface
 */
public class SequentialModel {
}
