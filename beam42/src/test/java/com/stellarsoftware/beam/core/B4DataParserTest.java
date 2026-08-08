package com.stellarsoftware.beam.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.stellarsoftware.beam.core.B4constants.MNLINES;
import static com.stellarsoftware.beam.core.B4constants.MPRESENT;

class B4DataParserTest {

    @Test
    void parsesInjectedMediaModel() {
        Globals.init();
        Globals.reg = new Registry(null);
        RT13 rt13 = new RT13();
        MEDDataModel media = new MEDDataModel(rt13);

        Assertions.assertTrue(media.bLoadFile(new File("../Examples/BeamFour/GLASS.MED")));

        new B4DataParser(null, null, media, rt13).parse(true);

        Assertions.assertEquals(1, Globals.giFlags[MPRESENT]);
        Assertions.assertTrue(Globals.giFlags[MNLINES] > 0);
    }
}
