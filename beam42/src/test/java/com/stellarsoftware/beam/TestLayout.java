package com.stellarsoftware.beam;

import com.stellarsoftware.beam.core.*;
import com.stellarsoftware.beam.core.render.DrawLayout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class TestLayout {

    @Test
    public void testBasicLayout() {
        Globals.init();
        Globals.reg = new Registry(null);  // create and load registry
        RT13 rt13 = Globals.RT13;
        OPTDataModel optDataModel = new OPTDataModel(rt13);
        Assertions.assertTrue(optDataModel.bLoadFile(new File("../Examples/BeamFour/LENS.OPT")));
        B4DataParser b4DataParser = new B4DataParser(optDataModel, null, null, rt13);
        b4DataParser.parse(true);

        // Setup preferences
        Globals.reg.putuo(B4constants.UO_LAYOUT, 3, "10"); // Arc Segments
        Globals.reg.putuo(B4constants.UO_LAYOUT, 10, "F"); // Hruler
        Globals.reg.putuo(B4constants.UO_LAYOUT, 11, "F"); // Vruler
        Globals.reg.putuo(B4constants.UO_LAYOUT, 12, "F"); // Xaxis
        Globals.reg.putuo(B4constants.UO_LAYOUT, 13, "F"); // Yaxis
        Globals.reg.putuo(B4constants.UO_LAYOUT, 14, "F"); // Zaxis
        Globals.reg.putuo(B4constants.UO_LAYOUT, 19, "T"); // N
        Globals.reg.putuo(B4constants.UO_LAYOUT, 20, "F"); // E
        Globals.reg.putuo(B4constants.UO_LAYOUT, 21, "T"); // S
        Globals.reg.putuo(B4constants.UO_LAYOUT, 22, "F"); // W
        Globals.reg.putuo(B4constants.UO_LAYOUT, 23, "F"); // NE
        Globals.reg.putuo(B4constants.UO_LAYOUT, 24, "F"); // SE
        Globals.reg.putuo(B4constants.UO_LAYOUT, 25, "F"); // SW
        Globals.reg.putuo(B4constants.UO_LAYOUT, 26, "F"); // NW

        DrawLayout drawLayout = new DrawLayout();
        drawLayout.doTechList(true);

        // TODO how to validate output?
        Assertions.assertEquals(168, drawLayout.baseList.size());
        Assertions.assertEquals(40, drawLayout.finishList.size());
    }
}
