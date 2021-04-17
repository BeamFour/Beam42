package org.redukti.jfotoptix.material;

import org.redukti.jfotoptix.light.SpectralLine;

import java.util.HashMap;
import java.util.Map;

public class GlassMap extends Solid {

    Map<Double, Double> indexMap = new HashMap<>();

    public GlassMap(String name, Map<Double, Double> indices) {
        super(name);
        this.indexMap = indices;
    }

    public GlassMap(String name, double d_index, double C, double F) {
        super(name);
        indexMap.put(SpectralLine.d, d_index);
        indexMap.put(SpectralLine.C, C);
        indexMap.put(SpectralLine.F, F);
    }

    @Override
    public boolean is_opaque() {
        return false;
    }

    @Override
    public boolean is_reflecting() {
        return false;
    }

    @Override
    public double get_refractive_index(double wavelen) {
        Double index = indexMap.get(wavelen);
        if (index == null) {
            throw new IllegalArgumentException("Do not know how to get the refractive index for wavelen " + wavelen);
        }
        return index.doubleValue();
    }

    public static GlassMap glassByName(String name) {
        return glasses.get(name);
    }

    static Map<String, GlassMap> glasses = new HashMap<>();
    static {
        glasses.put("J-FK5",new GlassMap("J-FK5",1.48749,1.485343,1.492276));
        glasses.put("J-FK01A",new GlassMap("J-FK01A",1.497,1.495139,1.501226));
        glasses.put("J-FKH1",new GlassMap("J-FKH1",1.49782,1.49598,1.502009));
        glasses.put("J-FKH2",new GlassMap("J-FKH2",1.456,1.454469,1.45946));
        glasses.put("J-PKH1",new GlassMap("J-PKH1",1.5186,1.516311,1.523731));
        glasses.put("J-PSK02",new GlassMap("J-PSK02",1.618,1.615024,1.624781));
        glasses.put("J-PSK03",new GlassMap("J-PSK03",1.603,1.600183,1.609398));
        glasses.put("J-PSKH1",new GlassMap("J-PSKH1",1.59319,1.59054,1.599276));
        glasses.put("J-PSKH4",new GlassMap("J-PSKH4",1.59349,1.590771,1.599629));
        glasses.put("J-BK7A",new GlassMap("J-BK7A",1.5168,1.514324,1.522382));
        glasses.put("J-BAK1",new GlassMap("J-BAK1",1.5725,1.569472,1.579464));
        glasses.put("J-BAK2",new GlassMap("J-BAK2",1.53996,1.537199,1.546271));
        glasses.put("J-BAK4",new GlassMap("J-BAK4",1.56883,1.565751,1.575909));
        glasses.put("J-K3",new GlassMap("J-K3",1.51823,1.515551,1.524362));
        glasses.put("J-K5",new GlassMap("J-K5",1.52249,1.519803,1.528627));
        glasses.put("J-KZFH1",new GlassMap("J-KZFH1",1.61266,1.608532,1.622313));
        glasses.put("J-KZFH4",new GlassMap("J-KZFH4",1.552981,1.549923,1.559964));
        glasses.put("J-KZFH6",new GlassMap("J-KZFH6",1.68376,1.678397,1.696564));
        glasses.put("J-KZFH7",new GlassMap("J-KZFH7",1.73211,1.727358,1.74321));
        glasses.put("J-KF6",new GlassMap("J-KF6",1.51742,1.514429,1.524341));
        glasses.put("J-BALF4",new GlassMap("J-BALF4",1.57957,1.576316,1.5871));
        glasses.put("J-BAF3",new GlassMap("J-BAF3",1.58267,1.578929,1.591464));
        glasses.put("J-BAF4",new GlassMap("J-BAF4",1.60562,1.601481,1.615408));
        glasses.put("J-BAF8",new GlassMap("J-BAF8",1.62374,1.619775,1.633044));
        glasses.put("J-BAF10",new GlassMap("J-BAF10",1.67003,1.665785,1.679998));
        glasses.put("J-BAF11",new GlassMap("J-BAF11",1.66672,1.662593,1.676388));
        glasses.put("J-BAF12",new GlassMap("J-BAF12",1.6393,1.635055,1.649314));
        glasses.put("J-BASF2",new GlassMap("J-BASF2",1.66446,1.659032,1.677556));
        glasses.put("J-BASF6",new GlassMap("J-BASF6",1.66755,1.662821,1.678763));
        glasses.put("J-BASF7",new GlassMap("J-BASF7",1.70154,1.696483,1.713586));
        glasses.put("J-BASF8",new GlassMap("J-BASF8",1.72342,1.717827,1.736849));
        glasses.put("J-SK2",new GlassMap("J-SK2",1.60738,1.604139,1.614843));
        glasses.put("J-SK4",new GlassMap("J-SK4",1.61272,1.609539,1.620006));
        glasses.put("J-SK5",new GlassMap("J-SK5",1.58913,1.586191,1.595814));
        glasses.put("J-SK10",new GlassMap("J-SK10",1.6228,1.619492,1.630399));
        glasses.put("J-SK11",new GlassMap("J-SK11",1.56384,1.561006,1.570294));
        glasses.put("J-SK12",new GlassMap("J-SK12",1.58313,1.580141,1.589954));
        glasses.put("J-SK14",new GlassMap("J-SK14",1.60311,1.600078,1.610015));
        glasses.put("J-SK15",new GlassMap("J-SK15",1.62299,1.619729,1.630448));
        glasses.put("J-SK16",new GlassMap("J-SK16",1.62041,1.617264,1.627562));
        glasses.put("J-SK18",new GlassMap("J-SK18",1.63854,1.63505,1.646589));
        glasses.put("J-SSK1",new GlassMap("J-SSK1",1.6172,1.613738,1.625175));
        glasses.put("J-SSK5",new GlassMap("J-SSK5",1.65844,1.654552,1.667504));
        glasses.put("J-SSK8",new GlassMap("J-SSK8",1.61772,1.613998,1.626399));
        glasses.put("J-LLF1",new GlassMap("J-LLF1",1.54814,1.54455,1.556594));
        glasses.put("J-LLF2",new GlassMap("J-LLF2",1.54072,1.53728,1.548793));
        glasses.put("J-LLF6",new GlassMap("J-LLF6",1.53172,1.528453,1.539353));
        glasses.put("J-LF5",new GlassMap("J-LF5",1.58144,1.577238,1.591428));
        glasses.put("J-LF6",new GlassMap("J-LF6",1.56732,1.563371,1.576695));
        glasses.put("J-LF7",new GlassMap("J-LF7",1.57501,1.570908,1.58476));
        glasses.put("J-F1",new GlassMap("J-F1",1.62588,1.620742,1.638263));
        glasses.put("J-F2",new GlassMap("J-F2",1.62004,1.615037,1.632073));
        glasses.put("J-F3",new GlassMap("J-F3",1.61293,1.608054,1.624644));
        glasses.put("J-F5",new GlassMap("J-F5",1.60342,1.598747,1.614615));
        glasses.put("J-F8",new GlassMap("J-F8",1.59551,1.591028,1.606214));
        glasses.put("J-F16",new GlassMap("J-F16",1.5927,1.587788,1.604592));
        glasses.put("J-SF1",new GlassMap("J-SF1",1.71736,1.710337,1.734595));
        glasses.put("J-SF2",new GlassMap("J-SF2",1.64769,1.642082,1.661287));
        glasses.put("J-SF4",new GlassMap("J-SF4",1.7552,1.747305,1.774696));
        glasses.put("J-SF5",new GlassMap("J-SF5",1.6727,1.666619,1.68752));
        glasses.put("J-SF6",new GlassMap("J-SF6",1.80518,1.796109,1.827749));
        glasses.put("J-SF6HS",new GlassMap("J-SF6HS",1.80518,1.796109,1.827749));
        glasses.put("J-SF7",new GlassMap("J-SF7",1.6398,1.634385,1.652905));
        glasses.put("J-SF8",new GlassMap("J-SF8",1.68893,1.682509,1.704616));
        glasses.put("J-SF10",new GlassMap("J-SF10",1.72825,1.720838,1.7465));
        glasses.put("J-SF11",new GlassMap("J-SF11",1.78472,1.775941,1.806548));
        glasses.put("J-SF13",new GlassMap("J-SF13",1.74077,1.733069,1.759772));
        glasses.put("J-SF14",new GlassMap("J-SF14",1.76182,1.75358,1.782237));
        glasses.put("J-SF15",new GlassMap("J-SF15",1.69895,1.692227,1.715424));
        glasses.put("J-SF03",new GlassMap("J-SF03",1.84666,1.836505,1.872084));
        glasses.put("J-SF03HS",new GlassMap("J-SF03HS",1.84666,1.836505,1.872084));
        glasses.put("J-SFS3",new GlassMap("J-SFS3",1.7847,1.776116,1.805989));
        glasses.put("J-SFH1",new GlassMap("J-SFH1",1.80809,1.797989,1.833527));
        glasses.put("J-SFH2",new GlassMap("J-SFH2",1.86074,1.85012,1.887417));
        glasses.put("J-LAK7",new GlassMap("J-LAK7",1.6516,1.648206,1.659331));
        glasses.put("J-LAK7R",new GlassMap("J-LAK7R",1.6516,1.648206,1.659322));
        glasses.put("J-LAK8",new GlassMap("J-LAK8",1.713,1.708982,1.722196));
        glasses.put("J-LAK9",new GlassMap("J-LAK9",1.691,1.687171,1.69975));
        glasses.put("J-LAK10",new GlassMap("J-LAK10",1.71999,1.715672,1.729995));
        glasses.put("J-LAK12",new GlassMap("J-LAK12",1.6779,1.674187,1.686435));
        glasses.put("J-LAK13",new GlassMap("J-LAK13",1.6935,1.689551,1.702585));
        glasses.put("J-LAK14",new GlassMap("J-LAK14",1.6968,1.692974,1.705525));
        glasses.put("J-LAK18",new GlassMap("J-LAK18",1.72916,1.725097,1.738449));
        glasses.put("J-LAK01",new GlassMap("J-LAK01",1.64,1.636739,1.647371));
        glasses.put("J-LAK02",new GlassMap("J-LAK02",1.67,1.66644,1.678123));
        glasses.put("J-LAK04",new GlassMap("J-LAK04",1.651,1.647485,1.659061));
        glasses.put("J-LAK06",new GlassMap("J-LAK06",1.6779,1.673877,1.687256));
        glasses.put("J-LAK09",new GlassMap("J-LAK09",1.734,1.72968,1.74393));
        glasses.put("J-LAK011",new GlassMap("J-LAK011",1.741,1.736741,1.750784));
        glasses.put("J-LASKH2",new GlassMap("J-LASKH2",1.755,1.750628,1.765054));
        glasses.put("J-LAF2",new GlassMap("J-LAF2",1.744,1.739042,1.755647));
        glasses.put("J-LAF3",new GlassMap("J-LAF3",1.717,1.712517,1.727462));
        glasses.put("J-LAF7",new GlassMap("J-LAF7",1.7495,1.743271,1.764535));
        glasses.put("J-LAF01",new GlassMap("J-LAF01",1.7,1.695645,1.710196));
        glasses.put("J-LAF02",new GlassMap("J-LAF02",1.72,1.715094,1.731604));
        glasses.put("J-LAF04",new GlassMap("J-LAF04",1.757,1.752239,1.768055));
        glasses.put("J-LAF05",new GlassMap("J-LAF05",1.762,1.756381,1.775377));
        glasses.put("J-LAF09",new GlassMap("J-LAF09",1.697,1.692687,1.707073));
        glasses.put("J-LAF010",new GlassMap("J-LAF010",1.7432,1.738649,1.753737));
        glasses.put("J-LAF016",new GlassMap("J-LAF016",1.801,1.794267,1.817203));
        glasses.put("J-LAF016HS",new GlassMap("J-LAF016HS",1.801,1.794267,1.817203));
        glasses.put("J-LAFH3",new GlassMap("J-LAFH3",1.79504,1.787036,1.814745));
        glasses.put("J-LAFH3HS",new GlassMap("J-LAFH3HS",1.79504,1.787036,1.814745));
        glasses.put("J-LASF01",new GlassMap("J-LASF01",1.7859,1.780582,1.798375));
        glasses.put("J-LASF02",new GlassMap("J-LASF02",1.79952,1.793865,1.812862));
        glasses.put("J-LASF03",new GlassMap("J-LASF03",1.8061,1.800248,1.819921));
        glasses.put("J-LASF05",new GlassMap("J-LASF05",1.83481,1.828989,1.848524));
        glasses.put("J-LASF05HS",new GlassMap("J-LASF05HS",1.83481,1.828989,1.848524));
        glasses.put("J-LASF08A",new GlassMap("J-LASF08A",1.883,1.876555,1.898256));
        glasses.put("J-LASF09A",new GlassMap("J-LASF09A",1.816,1.810744,1.828257));
        glasses.put("J-LASF010",new GlassMap("J-LASF010",1.834,1.827379,1.849808));
        glasses.put("J-LASF013",new GlassMap("J-LASF013",1.8044,1.798372,1.818682));
        glasses.put("J-LASF014",new GlassMap("J-LASF014",1.788,1.782997,1.799638));
        glasses.put("J-LASF015",new GlassMap("J-LASF015",1.804,1.798824,1.816078));
        glasses.put("J-LASF016",new GlassMap("J-LASF016",1.7725,1.767801,1.78337));
        glasses.put("J-LASF017",new GlassMap("J-LASF017",1.795,1.789742,1.807287));
        glasses.put("J-LASF021",new GlassMap("J-LASF021",1.85026,1.842602,1.868883));
        glasses.put("J-LASF021HS",new GlassMap("J-LASF021HS",1.85026,1.842602,1.868883));
        glasses.put("J-LASFH2",new GlassMap("J-LASFH2",1.76684,1.761914,1.778307));
        glasses.put("J-LASFH6",new GlassMap("J-LASFH6",1.8061,1.799034,1.823209));
        glasses.put("J-LASFH9A",new GlassMap("J-LASFH9A",1.90265,1.895235,1.920469));
        glasses.put("J-LASFH13",new GlassMap("J-LASFH13",1.90366,1.895254,1.924149));
        glasses.put("J-LASFH13HS",new GlassMap("J-LASFH13HS",1.90366,1.895254,1.924149));
        glasses.put("J-LASFH15",new GlassMap("J-LASFH15",1.95,1.940626,1.972976));
        glasses.put("J-LASFH16",new GlassMap("J-LASFH16",2.001,1.991039,2.02541));
        glasses.put("J-LASFH17",new GlassMap("J-LASFH17",2.00069,1.989413,2.028724));
        glasses.put("J-LASFH17HS",new GlassMap("J-LASFH17HS",2.00069,1.989413,2.028724));
        glasses.put("J-LASFH21",new GlassMap("J-LASFH21",1.95375,1.945145,1.974641));
        glasses.put("J-LASFH22",new GlassMap("J-LASFH22",1.8485,1.842718,1.862094));
        glasses.put("J-LASFH23",new GlassMap("J-LASFH23",1.85,1.840948,1.872398));
        glasses.put("J-LASFH24",new GlassMap("J-LASFH24",1.902,1.891774,1.927478));
        glasses.put("J-LASFH24HS",new GlassMap("J-LASFH24HS",1.902,1.891774,1.927478));
        glasses.put("J-KZFH9",new GlassMap("J-KZFH9",1.738,1.731309,1.754185));
    }
}
