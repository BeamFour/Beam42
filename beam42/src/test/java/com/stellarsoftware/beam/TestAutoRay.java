package com.stellarsoftware.beam;

import com.stellarsoftware.beam.core.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class TestAutoRay {

    @Test
    public void testFisheyeB() {
        Globals.init();
        Globals.reg = new Registry(null);  // create and load registry
        RT13 rt13 = Globals.RT13;
        OPTDataModel optDataModel = new OPTDataModel(rt13);
        Assertions.assertTrue(optDataModel.bLoadFile(new File("../Examples/FisheyeB.OPT")));
        RAYDataModel rayDataModel = new RAYDataModel(rt13);
        Assertions.assertTrue(rayDataModel.bLoadFile(new File("../Examples/FisheyeB.RAY")));
        B4DataParser b4DataParser = new B4DataParser(optDataModel, rayDataModel, null, rt13);
        b4DataParser.parse(true);
        AutoRayGenerator autoRayGenerator = new AutoRayGenerator(optDataModel, rayDataModel, rt13);
        Assertions.assertTrue(autoRayGenerator.generate());
        autoRayGenerator.vUpdateRayStarts();
        System.out.println(optDataModel.getTableString());
        System.out.println(rayDataModel.getTableString());
        String[] expectedX0 = {
                "48.568",
                "44.132",
                "39.763",
                "35.469",
                "31.258",
                "7.477",
                "3.747",
                "0.000",
                "-3.747",
                "-7.477",
                "-31.258",
                "-35.469",
                "-39.763",
                "-44.132",
                "-48.568"
        };

        int f = rayDataModel.rI2F(RAYDataModel.getCombinedRayFieldOp("X0"));
        Assertions.assertEquals(0, f);
        for (int row = 0; row <expectedX0.length; row++) {
            String value = rayDataModel.getFieldTrim(f, row+3);
            Assertions.assertEquals(expectedX0[row], value);
        }
    }

}
