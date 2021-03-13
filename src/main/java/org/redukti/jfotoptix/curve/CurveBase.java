package org.redukti.jfotoptix.curve;

import org.redukti.jfotoptix.math.*;

public abstract class CurveBase implements Curve {

    static final class FunctionParams {
        final CurveBase c;
        final double x, y;
        public FunctionParams(CurveBase c, double x, double y) {
            this.c = c;
            this.x = x;
            this.y = y;
        }
    }

//    static final class FunctionSagittaX implements DerivFunction {
//        final FunctionParams params;
//        public FunctionSagittaX(FunctionParams params) {
//            this.params = params;
//        }
//        @Override
//        public double apply(double x) {
//            return  params.c.sagitta (new Vector2 (x, params.y));
//        }
//    }
//
//    static final class FunctionSagittaY implements DerivFunction {
//        final FunctionParams params;
//        public FunctionSagittaY(FunctionParams params) {
//            this.params = params;
//        }
//        @Override
//        public double apply(double y) {
//            return  params.c.sagitta (new Vector2 (params.x, y));
//        }
//    }


    @Override
    public Vector2 derivative(Vector2 xy) {
        //double abserr;
        final FunctionParams params = new FunctionParams(this, xy.x(), xy.y());
        DerivFunction dxf = (x) -> this.sagitta(new Vector2(x, params.y));
        DerivFunction dyf = (y) -> this.sagitta(new Vector2(params.x, y));

        DerivResult result = Derivatives.central_derivative(dxf, xy.x(), 1e-6);
        double dx = result.result;
        result = Derivatives.central_derivative(dyf, xy.y(), 1e-6);
        double dy = result.result;
        // TODO what do we do about error?
        return new Vector2(dx, dy);
    }

    @Override
    public Vector3 intersect(Vector3Position ray) {
        return null;
    }

    @Override
    public Vector3 normal(Vector3 point) {
        Vector2 d = derivative (point.project_xy ());
        return new Vector3 (d.x (), d.y (), -1.0).normalize();
    }
}
