package org.redukti.rayoptics.seq;

/**
 *     Basic part of a sequential model
 *
 *     The :class:`~sequential.SequentialModel` is a sequence of Interfaces and
 *     Gaps. The Interface class is a boundary between two adjacent Gaps and
 *     their associated media. It specifies several methods that must be
 *     implemented to model the optical behavior of the interface.
 *
 *     The Interface class addresses the following use cases:
 *
 *         - support for ray intersection calculation during ray tracing
 *             - interfaces can be tilted and decentered wrt the adjacent gaps
 *         - support for getting and setting the optical power of the interface
 *         - support for various optical properties, i.e. does it reflect or
 *           transmit
 *         - supports a basic idea of size, the max_aperture
 *
 *     Attributes:
 *         interact_mode: 'transmit' | 'reflect' | 'dummy'
 *         delta_n: refractive index difference across the interface
 *         decenter: :class:`~rayoptics.elem.surface.DecenterData` for the interface, if specified
 *         max_aperture: the maximum aperture radius on the interface
 *
 */
public class Interface {
}
