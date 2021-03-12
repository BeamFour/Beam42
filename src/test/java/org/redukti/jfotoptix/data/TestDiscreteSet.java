package org.redukti.jfotoptix.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class TestDiscreteSet {

    static final class DataPoint {
        double a,b,c,d;

        public DataPoint(String line) {
            String[] values = line.split(" ");
            Assertions.assertTrue(values.length >= 3);
            this.a = Double.parseDouble(values[0]);
            this.b = Double.parseDouble(values[1]);
            this.c = Double.parseDouble(values[2]);
            if (values.length == 4)
                this.d = Double.parseDouble(values[3]);
        }

    }

    List<DataPoint> lines;

    @BeforeEach
    public void setupInput() throws Exception {
        lines = parseDataPoints("/test_discrete_set-in.txt");
        Assertions.assertTrue(lines.size() > 0);
    }

    private List<DataPoint> parseDataPoints(String name) throws IOException, URISyntaxException {
        return Files.readAllLines(Paths.get(this.getClass().getResource(name).toURI()))
            .stream().map(e -> new DataPoint(e)).collect(Collectors.toList());
    }

    private void doTest(Interpolation method) throws Exception {
        String expected = "/test_discrete_set-" + method.name() + ".txt";
        List<DataPoint> results = parseDataPoints(expected);
        Assertions.assertTrue(results.size() > 0);

        final double N = 15;
        final double R = 10;

        DiscreteSet d = new DiscreteSet();
        for (int i = 0; i < (int)N; i++) {
            d.add_data(lines.get(i).a, lines.get(i).b, lines.get(i).c);
        }
        d.setInterpolation(method);

        int i = 0;
        for (double x = -N/2.0 - 2.0; x < N/2.0 + 2.0; x += 1.0/R)
        {
            DataPoint ex = results.get(i++);
            double xx = ex.a;
            double y = ex.b;
            double yy = ex.c;
            double yyy = ex.d;

            Assertions.assertEquals(x, xx, 1e-10, method.name() + ":unexpected x value in test data " + x + ":" + xx);
            Assertions.assertEquals(y, d.interpolate(x), 1e-10, method.name() + ":bad y value " + x + " " + y + " " + d.interpolate(x));
            Assertions.assertEquals(yy, d.interpolate(x, 1), 1e-10, method.name() + ":bad yy value " + x + " " + yy + " " + d.interpolate(x, 1));
            Assertions.assertEquals(yyy, d.interpolate(x, 2), 1e-10, method.name() + ":bad yyy value " + x + " " + yyy + " " + d.interpolate(x, 2));
        }
    }

    @Test
    public void testNearest() throws Exception {
        doTest(Interpolation.Nearest);
    }

    @Test
    public void testLinear() throws Exception {
        doTest(Interpolation.Linear);
    }

    @Test
    public void testQuadratic() throws Exception {
        doTest(Interpolation.Quadratic);
    }

    @Test
    public void testCubicSimple() throws Exception {
        doTest(Interpolation.CubicSimple);
    }

    @Test
    public void testCubic() throws Exception {
        doTest(Interpolation.Cubic);
    }

    @Test
    public void testCubic2() throws Exception {
        doTest(Interpolation.Cubic2);
    }

    @Test
    public void testCubicDerivInit() throws Exception {
        doTest(Interpolation.CubicDerivInit);
    }

    @Test
    public void testCubic2DerivInit() throws Exception {
        doTest(Interpolation.Cubic2DerivInit);
    }

    @Test
    public void testCubicDeriv() throws Exception {
        doTest(Interpolation.CubicDeriv);
    }

    @Test
    public void testCubic2Deriv() throws Exception {
       doTest(Interpolation.Cubic2Deriv);
    }

}
