package org.redukti.jfotoptix.examples;

import org.redukti.jfotoptix.curve.Flat;
import org.redukti.jfotoptix.io.RendererSvg;
import org.redukti.jfotoptix.light.SpectralLine;
import org.redukti.jfotoptix.material.Abbe;
import org.redukti.jfotoptix.math.Vector3;
import org.redukti.jfotoptix.math.Vector3Pair;
import org.redukti.jfotoptix.shape.Rectangle;
import org.redukti.jfotoptix.sys.Image;
import org.redukti.jfotoptix.sys.Lens;
import org.redukti.jfotoptix.sys.OpticalSystem;
import org.redukti.jfotoptix.sys.PointSource;

public class Tessar {

    public static void main(String[] args) {

        OpticalSystem.Builder systemBuilder = new OpticalSystem.Builder();
        Lens.Builder lensBuilder = new Lens.Builder()
                .position(Vector3Pair.position_000_001)
                .add_surface(1/0.031186861,  14.934638, 4.627804137,
                    new Abbe(Abbe.AbbeFormula.AbbeVd, 1.607170, 59.5002, 0.0))
                .add_surface(0,              14.934638, 5.417429465)
                .add_surface(1/-0.014065441, 12.766446, 3.728230979,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.575960, 41.2999, 0.0))
                .add_surface(1/0.034678487,  11.918098, 4.417903733)
                .add_stop(12.066273, 2.288913925)
                .add_surface(0,              12.372318, 1.499288597,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.526480, 51.4000, 0.0))
                .add_surface(1/0.035104369,  14.642815, 7.996205852,
                        new Abbe(Abbe.AbbeFormula.AbbeVd, 1.623770, 56.8998, 0.0))
                .add_surface(1/-0.021187519, 14.642815, 85.243965130);
        systemBuilder.add(lensBuilder);
        Image.Builder imagePlaneBuilder = new Image.Builder()
                .position(new Vector3Pair(new Vector3(0, 0, 125.596), Vector3.vector3_001))
                .curve(Flat.flat)
                .shape(new Rectangle(5.0*2));
        systemBuilder.add(imagePlaneBuilder);

        PointSource.Builder ps = new PointSource.Builder(PointSource.SourceInfinityMode.SourceAtInfinity, Vector3.vector3_001)
                .add_spectral_line(SpectralLine.d)
                .add_spectral_line(SpectralLine.C)
                .add_spectral_line(SpectralLine.F);
        systemBuilder.add(ps);

        RendererSvg renderer = new RendererSvg( 800, 400);
        OpticalSystem system = systemBuilder.build();
        System.out.println(system);
        // draw 2d system layout
        system.draw_2d_fit(renderer);
        system.draw_2d(renderer);

        System.out.println(renderer.write(new StringBuilder()).toString());
    }


}
