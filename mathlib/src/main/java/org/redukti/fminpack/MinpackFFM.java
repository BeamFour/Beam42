package org.redukti.fminpack;

import org.redukti.jm.minpack.MinPack;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Paths;
import java.util.Arrays;

import static java.lang.foreign.ValueLayout.*;

public class MinpackFFM {

    // Linker and library
    static final Linker LINKER = Linker.nativeLinker();
    static final SymbolLookup LIB = SymbolLookup.libraryLookup(Paths.get("minpack.dll"), Arena.global());

    static class LMDerFunctionProxy {

        final MinPack.Lmder_Function fcn;

        public LMDerFunctionProxy(MinPack.Lmder_Function fcn) {
            this.fcn = fcn;
        }

        public void invoke(MemorySegment m, MemorySegment n, MemorySegment x, MemorySegment fvec, MemorySegment fjac, MemorySegment ldfjac, MemorySegment iflag) {
            m = m.reinterpret(JAVA_INT.byteSize());
            int m_ = m.get(JAVA_INT,0);
            n = n.reinterpret(JAVA_INT.byteSize());
            int n_ = n.get(JAVA_INT, 0);
            x = x.reinterpret(JAVA_DOUBLE.byteSize()*n_);
            double[] x_ = x.toArray(JAVA_DOUBLE);
            fvec = fvec.reinterpret(JAVA_DOUBLE.byteSize()*m_);
            double[] fvec_ = fvec.toArray(JAVA_DOUBLE);
            fjac = fjac.reinterpret(JAVA_DOUBLE.byteSize()*m_*n_);
            double[] fjac_ = fjac.toArray(JAVA_DOUBLE);
            ldfjac = ldfjac.reinterpret(JAVA_INT.byteSize());
            int ldfjac_ = ldfjac.get(JAVA_INT,0);
            iflag = iflag.reinterpret(JAVA_INT.byteSize());
            int iflag_  = iflag.get(JAVA_INT,0);

            fcn.apply(m_,n_,x_,fvec_,fjac_,ldfjac_,iflag_);

            for (int i = 0; i < fvec_.length; i++)
                fvec.setAtIndex(JAVA_DOUBLE,i,fvec_[i]);
            for (int i = 0; i < fjac_.length; i++)
                fjac.setAtIndex(JAVA_DOUBLE,i,fjac_[i]);
        }

        // Static helper to get MethodHandle from an instance
        static MethodHandle getHandle(LMDerFunctionProxy fcn) throws Exception {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return lookup.findVirtual(
                    fcn.getClass(),
                    "invoke",
                    MethodType.methodType(void.class,
                            MemorySegment.class, MemorySegment.class,   // m,n
                            MemorySegment.class, MemorySegment.class, MemorySegment.class,  // x,fvec,fjac
                            MemorySegment.class, MemorySegment.class) // ldfjac,iflag
            ).bindTo(fcn);
        }
    }

    // Create a MethodHandle for the callback
    static MemorySegment createCallback(LMDerFunctionProxy fcn, Arena arena) throws Exception {
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(
                ADDRESS.withTargetLayout(JAVA_INT), ADDRESS.withTargetLayout(JAVA_INT),   // m,n
                ADDRESS.withTargetLayout(JAVA_DOUBLE), ADDRESS.withTargetLayout(JAVA_DOUBLE), ADDRESS.withTargetLayout(JAVA_DOUBLE),  // x,fvec,fjac
                ADDRESS.withTargetLayout(JAVA_INT), ADDRESS.withTargetLayout(JAVA_INT) // ldfjac,iflag
        );
        return LINKER.upcallStub(LMDerFunctionProxy.getHandle(fcn), fd, arena);
    }

    public static int lmder(MinPack.Lmder_Function fcn,int m,int n,double[] xinit,double[] diag_, int dmode) throws Exception {
        int iinfo = -99;
        if (m < n)
            throw new IllegalArgumentException("Number of goals must be >= number of variables");
        if (xinit.length != n)
            throw new IllegalArgumentException("x[] must have length n");
        if (diag_.length != n)
            throw new IllegalArgumentException("diag[] length must be n");
        try (Arena arena = Arena.ofConfined()) {

            // Allocate arrays
            MemorySegment mm    = arena.allocate(JAVA_INT, 1);
            MemorySegment nn    = arena.allocate(JAVA_INT,1);
            MemorySegment x     = arena.allocate(JAVA_DOUBLE, n);
            MemorySegment fvec  = arena.allocate(JAVA_DOUBLE, m);
            MemorySegment fjac  = arena.allocate(JAVA_DOUBLE, m * n);
            MemorySegment ldfjac = arena.allocate(JAVA_INT,1);
            MemorySegment ftol  = arena.allocate(JAVA_DOUBLE, 1);
            MemorySegment xtol  = arena.allocate(JAVA_DOUBLE, 1);
            MemorySegment gtol  = arena.allocate(JAVA_DOUBLE, 1);
            MemorySegment maxfev = arena.allocate(JAVA_INT,1);
            MemorySegment diag  = arena.allocate(JAVA_DOUBLE, n);
            MemorySegment mode = arena.allocate(JAVA_INT,1);
            MemorySegment factor = arena.allocate(JAVA_DOUBLE,1);
            MemorySegment nprint = arena.allocate(JAVA_INT,1);
            MemorySegment info = arena.allocate(JAVA_INT,1);
            MemorySegment nfev  = arena.allocate(JAVA_INT,1);
            MemorySegment njev  = arena.allocate(JAVA_INT,1);
            MemorySegment ipvt  = arena.allocate(JAVA_INT, n);
            MemorySegment qtf   = arena.allocate(JAVA_DOUBLE, n);
            MemorySegment wa1   = arena.allocate(JAVA_DOUBLE, n);
            MemorySegment wa2   = arena.allocate(JAVA_DOUBLE, n);
            MemorySegment wa3   = arena.allocate(JAVA_DOUBLE, n);
            MemorySegment wa4   = arena.allocate(JAVA_DOUBLE, m);

            MemorySegment fcnStub = createCallback(new LMDerFunctionProxy(fcn), arena);

            /*
            void lmder_ ( void (*fcn)(int *m, int *n, double *x, double *fvec, double *fjec, int *ldfjac, int *iflag ),
   	            int *m, int *n, double *x, double *fvec, double *fjec,
	            int *ldfjac, double *ftol, double *xtol, double *gtol,
	            int *maxfev, double *diag, int *mode, double *factor,
	            int *nprint, int *info, int *nfev, int *njev, int *ipvt,
	            double *qtf, double *wa1, double *wa2, double *wa3,
	            double *wa4 );
             */
            // Lookup minpack_lmder
            MethodHandle minpack_lmder = LINKER.downcallHandle(
                    LIB.find("lmder_").get(),
                    FunctionDescriptor.ofVoid(
                            ADDRESS, // fcn
                            ADDRESS, ADDRESS,  // m, n
                            ADDRESS, ADDRESS, ADDRESS, // x, fvec, fjac
                            ADDRESS, // ldfjac
                            ADDRESS, ADDRESS, ADDRESS, // ftol, xtol, gtol
                            ADDRESS, // maxfev
                            ADDRESS, // diag
                            ADDRESS, // mode
                            ADDRESS, // factor
                            ADDRESS, // nprint
                            ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS // info, nfev, njev, ipvt, qtf, wa1, wa2, wa3, wa4
                    )
            );

            mm.setAtIndex(JAVA_INT,0,m);
            nn.setAtIndex(JAVA_INT,0,n);
            for (int i = 0; i < xinit.length; i++)
                x.setAtIndex(JAVA_DOUBLE,i,xinit[i]);
            ldfjac.setAtIndex(JAVA_INT,0,m);
            ftol.setAtIndex(JAVA_DOUBLE,0,Math.sqrt(MinPack.dpmpar(1)));
            xtol.setAtIndex(JAVA_DOUBLE,0,Math.sqrt(MinPack.dpmpar(1)));
            gtol.setAtIndex(JAVA_DOUBLE,0,0);
            maxfev.setAtIndex(JAVA_INT,0,(n + 1) * 100);
            for (int i = 0; i < diag_.length; i++)
                diag.setAtIndex(JAVA_DOUBLE,i,diag_[i]);
            mode.setAtIndex(JAVA_INT,0,dmode);
            factor.setAtIndex(JAVA_DOUBLE,0,100);
            nprint.setAtIndex(JAVA_INT,0,0);
            info.setAtIndex(JAVA_INT,0,0);
            nfev.setAtIndex(JAVA_INT,0,0);
            njev.setAtIndex(JAVA_INT,0,0);
            // Call minpack_lmder
            minpack_lmder.invoke(
                    fcnStub,
                    mm, nn,
                    x, fvec, fjac,
                    ldfjac,
                    ftol, xtol, gtol,
                    maxfev,
                    diag,
                    mode,
                    factor,
                    nprint,
                    info, nfev, njev, ipvt, qtf, wa1, wa2, wa3, wa4
            );

            iinfo = info.getAtIndex(JAVA_INT,0);
            System.out.println("LMDER finished. info=" + iinfo);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
        return iinfo;
    }

//    public static void main(String[] args) throws Throwable {
//
//            // Example callback
//        LMDerFunction fcn = (mm, nn, xx, ff, fj, ld, iflg) -> {
//            Arrays.fill(ff,0);
//            Arrays.fill(fj,0);
//        };
//        double[] x = {0.,0.,0.};
//        double[] diag = {1.,1.,1.};
//        lmder(fcn,3,3,x,diag);
//    }
}
