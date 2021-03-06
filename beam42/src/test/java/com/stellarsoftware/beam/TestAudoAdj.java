package com.stellarsoftware.beam;

import com.stellarsoftware.beam.core.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

public class TestAudoAdj {

    @Test
    public void testAuto1() {
        Globals.init();
        Globals.reg = new Registry(null);  // create and load registry
        RT13 rt13 = Globals.RT13;
        OPTDataModel optDataModel = new OPTDataModel(rt13);
        Assertions.assertTrue(optDataModel.bLoadFile(new File("../Examples/BeamFour/AUTO1.OPT")));
        RAYDataModel rayDataModel = new RAYDataModel(rt13);
        Assertions.assertTrue(rayDataModel.bLoadFile(new File("../Examples/BeamFour/AUTO1.RAY")));
        B4DataParser b4DataParser = new B4DataParser(optDataModel, rayDataModel, null, rt13);
        b4DataParser.parse(true);
//        new InOut(optDataModel, rayDataModel);
        AutoAdjuster autoAdjuster = new AutoAdjuster(optDataModel, rayDataModel, null, null, rt13);
        autoAdjuster.run();
        System.out.println(optDataModel.getTableString());
        Assertions.assertEquals("3.12613", optDataModel.getFieldTrim(optDataModel.oI2F(OPTDataModel.getOptFieldAttrib("Z")), 5));
        System.out.println(rayDataModel.getTableString());
    }

}
