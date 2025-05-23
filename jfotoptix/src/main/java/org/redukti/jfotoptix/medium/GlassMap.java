package org.redukti.jfotoptix.medium;

import org.redukti.jfotoptix.light.SpectralLine;

import java.util.HashMap;
import java.util.Map;

public class GlassMap extends Solid {

    protected Map<Double, Double> _index_map = new HashMap<>();
    protected String _manufacturer;

    public GlassMap(String name, Map<Double, Double> indices) {
        super(name);
        this._index_map = indices;
    }

    public GlassMap(String manufacturer, String name, double d_index, double C, double F) {
        super(name);
        _manufacturer = manufacturer;
        _index_map.put(SpectralLine.d, d_index);
        _index_map.put(SpectralLine.C, C);
        _index_map.put(SpectralLine.F, F);
    }

    public GlassMap(String name, double d_index, double C, double F) {
        this(null, name, d_index, C, F);
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
        Double index = _index_map.get(wavelen);
        if (index == null) {
            throw new IllegalArgumentException("Do not know how to get the refractive index for wavelen " + wavelen);
        }
        return index.doubleValue();
    }

    public String get_manufacturer() {
        return _manufacturer;
    }

    @Override
    public String toString() {
        return _name + "{d=" + _index_map.get(SpectralLine.d) + ",C="+ _index_map.get(SpectralLine.C) + ",F=" + _index_map.get(SpectralLine.F) + '}';
    }

    public static GlassMap glassByName(String name) {
        return glasses.get(name);
    }

    public static Map<String, GlassMap> glasses = new HashMap<>();
    static {
        // Hikari as of 1 April 2021
        glasses.put("J-FK5", new GlassMap("Hikari", "J-FK5", 1.48749, 1.485343, 1.492276)); // 70.31
        glasses.put("J-FK01A", new GlassMap("Hikari", "J-FK01A", 1.497, 1.495139, 1.501226)); //81.65
        glasses.put("J-FKH1", new GlassMap("Hikari", "J-FKH1", 1.49782, 1.49598, 1.502009)); // 82.57
        glasses.put("J-FKH2", new GlassMap("Hikari", "J-FKH2", 1.456, 1.454469, 1.45946)); // 91.36
        glasses.put("J-PKH1", new GlassMap("Hikari", "J-PKH1", 1.5186, 1.516311, 1.523731)); // 69.89
        glasses.put("J-PSK02", new GlassMap("Hikari", "J-PSK02", 1.618, 1.615024, 1.624781)); // 63.34
        glasses.put("J-PSK03", new GlassMap("Hikari", "J-PSK03", 1.603, 1.600183, 1.609398)); // 65.44
        glasses.put("J-PSKH1", new GlassMap("Hikari", "J-PSKH1", 1.59319, 1.59054, 1.599276)); // 67.9
        glasses.put("J-PSKH4", new GlassMap("Hikari", "J-PSKH4", 1.59349, 1.590771, 1.599629)); // 67.00
        glasses.put("J-PSKH8", new GlassMap("Hikari", "J-PSKH8", 1.62846, 1.625268, 1.635889)); // 59.17
        glasses.put("J-BK7A", new GlassMap("Hikari", "J-BK7A", 1.5168, 1.514324, 1.522382)); // 64.13
        glasses.put("J-BAK1", new GlassMap("Hikari", "J-BAK1", 1.5725, 1.569472, 1.579464));
        glasses.put("J-BAK2", new GlassMap("Hikari", "J-BAK2", 1.53996, 1.537199, 1.546271));
        glasses.put("J-BAK4", new GlassMap("Hikari", "J-BAK4", 1.56883, 1.565751, 1.575909));
        glasses.put("J-K3", new GlassMap("Hikari", "J-K3", 1.51823, 1.515551, 1.524362));
        glasses.put("J-K5", new GlassMap("Hikari", "J-K5", 1.52249, 1.519803, 1.528627));
        glasses.put("J-KZFH1", new GlassMap("Hikari", "J-KZFH1", 1.61266, 1.608532, 1.622313));
        glasses.put("J-KZFH4", new GlassMap("Hikari", "J-KZFH4", 1.552981, 1.549923, 1.559964));
        glasses.put("J-KZFH6", new GlassMap("Hikari", "J-KZFH6", 1.68376, 1.678397, 1.696564));
        glasses.put("J-KZFH7", new GlassMap("Hikari", "J-KZFH7", 1.73211, 1.727358, 1.74321));
        glasses.put("J-KZFH9",new GlassMap("Hikari", "J-KZFH9",1.738,1.731309,1.754185));
        glasses.put("J-KF6",new GlassMap("Hikari", "J-KF6",1.51742,1.514429,1.524341));
        glasses.put("J-BALF4",new GlassMap("Hikari", "J-BALF4",1.57957,1.576316,1.5871));
        glasses.put("J-BAF3",new GlassMap("Hikari", "J-BAF3",1.58267,1.578929,1.591464));
        glasses.put("J-BAF4",new GlassMap("Hikari", "J-BAF4",1.60562,1.601481,1.615408));
        glasses.put("J-BAF8",new GlassMap("Hikari", "J-BAF8",1.62374,1.619775,1.633044));
        glasses.put("J-BAF10",new GlassMap("Hikari", "J-BAF10",1.67003,1.665785,1.679998));
        glasses.put("J-BAF11",new GlassMap("Hikari", "J-BAF11",1.66672,1.662593,1.676388));
        glasses.put("J-BAF12",new GlassMap("Hikari", "J-BAF12",1.6393,1.635055,1.649314));
        glasses.put("J-BASF2",new GlassMap("Hikari", "J-BASF2",1.66446,1.659032,1.677556));
        glasses.put("J-BASF6",new GlassMap("Hikari", "J-BASF6",1.66755,1.662821,1.678763));
        glasses.put("J-BASF7",new GlassMap("Hikari", "J-BASF7",1.70154,1.696483,1.713586));
        glasses.put("J-BASF8",new GlassMap("Hikari", "J-BASF8",1.72342,1.717827,1.736849));
        glasses.put("J-SK2",new GlassMap("Hikari", "J-SK2",1.60738,1.604139,1.614843));
        glasses.put("J-SK4",new GlassMap("Hikari", "J-SK4",1.61272,1.609539,1.620006));
        glasses.put("J-SK5",new GlassMap("Hikari", "J-SK5",1.58913,1.586191,1.595814));
        glasses.put("J-SK10",new GlassMap("Hikari", "J-SK10",1.6228,1.619492,1.630399));
        glasses.put("J-SK11",new GlassMap("Hikari", "J-SK11",1.56384,1.561006,1.570294));
        glasses.put("J-SK12",new GlassMap("Hikari", "J-SK12",1.58313,1.580141,1.589954));
        glasses.put("J-SK14",new GlassMap("Hikari", "J-SK14",1.60311,1.600078,1.610015));
        glasses.put("J-SK15",new GlassMap("Hikari", "J-SK15",1.62299,1.619729,1.630448));
        glasses.put("J-SK16",new GlassMap("Hikari", "J-SK16",1.62041,1.617264,1.627562));
        glasses.put("J-SK18",new GlassMap("Hikari", "J-SK18",1.63854,1.63505,1.646589));
        glasses.put("J-SSK1",new GlassMap("Hikari", "J-SSK1",1.6172,1.613738,1.625175));
        glasses.put("J-SSK5",new GlassMap("Hikari", "J-SSK5",1.65844,1.654552,1.667504));
        glasses.put("J-SSK8",new GlassMap("Hikari", "J-SSK8",1.61772,1.613998,1.626399));
        glasses.put("J-LLF1",new GlassMap("Hikari", "J-LLF1",1.54814,1.54455,1.556594));
        glasses.put("J-LLF2",new GlassMap("Hikari", "J-LLF2",1.54072,1.53728,1.548793));
        glasses.put("J-LLF6", new GlassMap("Hikari", "J-LLF6", 1.53172, 1.528453, 1.539353));
        glasses.put("J-LF5", new GlassMap("Hikari", "J-LF5", 1.58144, 1.577238, 1.591428)); // 40.98
        glasses.put("J-LF6", new GlassMap("Hikari", "J-LF6", 1.56732, 1.563371, 1.576695));
        glasses.put("J-LF7", new GlassMap("Hikari", "J-LF7", 1.57501, 1.570908, 1.58476));
        glasses.put("J-F1", new GlassMap("Hikari", "J-F1", 1.62588, 1.620742, 1.638263));
        glasses.put("J-F2", new GlassMap("Hikari", "J-F2", 1.62004, 1.615037, 1.632073));
        glasses.put("J-F3", new GlassMap("Hikari", "J-F3", 1.61293, 1.608054, 1.624644));
        glasses.put("J-F5", new GlassMap("Hikari", "J-F5", 1.60342, 1.598747, 1.614615));
        glasses.put("J-F8", new GlassMap("Hikari", "J-F8", 1.59551, 1.591028, 1.606214));
        glasses.put("J-F16", new GlassMap("Hikari", "J-F16", 1.5927, 1.587788, 1.604592));
        glasses.put("J-SF1", new GlassMap("Hikari", "J-SF1", 1.71736, 1.710337, 1.734595)); // 29.57
        glasses.put("J-SF2", new GlassMap("Hikari", "J-SF2", 1.64769, 1.642082, 1.661287)); // 33.73
        glasses.put("J-SF4", new GlassMap("Hikari", "J-SF4", 1.7552, 1.747305, 1.774696)); // 27.57
        glasses.put("J-SF5", new GlassMap("Hikari", "J-SF5", 1.6727, 1.666619, 1.68752)); // 32.19
        glasses.put("J-SF6", new GlassMap("Hikari", "J-SF6", 1.80518, 1.796109, 1.827749)); // 25.45
        glasses.put("J-SF6HS", new GlassMap("Hikari", "J-SF6HS", 1.80518, 1.796109, 1.827749)); // 25.45
        glasses.put("J-SF7", new GlassMap("Hikari", "J-SF7", 1.6398, 1.634385, 1.652905)); // 34.55
        glasses.put("J-SF8", new GlassMap("Hikari", "J-SF8", 1.68893, 1.682509, 1.704616)); // 31.16
        glasses.put("J-SF10", new GlassMap("Hikari", "J-SF10", 1.72825, 1.720838, 1.7465)); // 28.38
        glasses.put("J-SF11", new GlassMap("Hikari", "J-SF11", 1.78472, 1.775941, 1.806548)); // 25.64
        glasses.put("J-SF13", new GlassMap("Hikari", "J-SF13", 1.74077, 1.733069, 1.759772)); // 27.74
        glasses.put("J-SF14", new GlassMap("Hikari", "J-SF14", 1.76182, 1.75358, 1.782237)); // 26.58
        glasses.put("J-SF15", new GlassMap("Hikari", "J-SF15", 1.69895, 1.692227, 1.715424)); // 30.13
        glasses.put("J-SF03", new GlassMap("Hikari", "J-SF03", 1.84666, 1.836505, 1.872084)); // 23.8
        glasses.put("J-SF03HS", new GlassMap("Hikari", "J-SF03HS", 1.84666, 1.836505, 1.872084)); // 23.8
        glasses.put("J-SFS3", new GlassMap("Hikari", "J-SFS3", 1.7847, 1.776116, 1.805989)); // 26.27
        glasses.put("J-SFH1", new GlassMap("Hikari", "J-SFH1", 1.80809, 1.797989, 1.833527)); // 22.74
        glasses.put("J-SFH1HS", new GlassMap("Hikari", "J-SFH1HS", 1.80809, 1.797989, 1.833527)); // 22.74
        glasses.put("J-SFH2", new GlassMap("Hikari", "J-SFH2", 1.86074, 1.85012, 1.887417)); // 23.08
        glasses.put("J-SFH4", new GlassMap("Hikari", "J-SFH4", 1.66382, 1.656918, 1.681192)); // 27.35
        glasses.put("J-SFH5", new GlassMap("Hikari", "J-SFH5", 1.75575, 1.747048, 1.777633)); // 24.71
        glasses.put("J-SFH6", new GlassMap("Hikari", "J-SFH6", 1.71338, 1.705581, 1.732977)); // 26.04
        // TODO SFH8
        glasses.put("J-LAK7", new GlassMap("Hikari", "J-LAK7", 1.6516, 1.648206, 1.659331));  // 58.57
        glasses.put("J-LAK7R", new GlassMap("Hikari", "J-LAK7R", 1.6516, 1.648206, 1.659322));  // 58.62
        glasses.put("J-LAK8", new GlassMap("Hikari", "J-LAK8", 1.713, 1.708982, 1.722196));  // 53.96
        glasses.put("J-LAK9", new GlassMap("Hikari", "J-LAK9", 1.691, 1.687171, 1.69975));  // 54.93
        glasses.put("J-LAK10", new GlassMap("Hikari", "J-LAK10", 1.71999, 1.715672, 1.729995)); // 50.27
        glasses.put("J-LAK12", new GlassMap("Hikari", "J-LAK12", 1.6779, 1.674187, 1.686435));  // 55.35
        glasses.put("J-LAK13", new GlassMap("Hikari", "J-LAK13", 1.6935, 1.689551, 1.702585));  // 53.21
        glasses.put("J-LAK14", new GlassMap("Hikari", "J-LAK14", 1.6968, 1.692974, 1.705525));  // 55.52
        glasses.put("J-LAK18", new GlassMap("Hikari", "J-LAK18", 1.72916, 1.725097, 1.738449));  // 54.61
        glasses.put("J-LAK01", new GlassMap("Hikari", "J-LAK01", 1.64, 1.636739, 1.647371));  // 60.20
        glasses.put("J-LAK02", new GlassMap("Hikari", "J-LAK02", 1.67, 1.66644, 1.678123));  // 57.35
        glasses.put("J-LAK04", new GlassMap("Hikari", "J-LAK04", 1.651, 1.647485, 1.659061));  // 56.24
        glasses.put("J-LAK06", new GlassMap("Hikari", "J-LAK06", 1.6779, 1.673877, 1.687256));  // 50.67
        glasses.put("J-LAK09", new GlassMap("Hikari", "J-LAK09", 1.734, 1.72968, 1.74393));  // 51.51
        glasses.put("J-LAK011", new GlassMap("Hikari", "J-LAK011", 1.741, 1.736741, 1.750784));  // 52.77
        glasses.put("J-LASKH2", new GlassMap("Hikari", "J-LASKH2", 1.755, 1.750628, 1.765054));
        glasses.put("J-LAF2", new GlassMap("Hikari", "J-LAF2", 1.744, 1.739042, 1.755647));
        glasses.put("J-LAF3", new GlassMap("Hikari", "J-LAF3", 1.717, 1.712517, 1.727462));
        glasses.put("J-LAF7", new GlassMap("Hikari", "J-LAF7", 1.7495, 1.743271, 1.764535));
        glasses.put("J-LAF01", new GlassMap("Hikari", "J-LAF01", 1.7, 1.695645, 1.710196));
        glasses.put("J-LAF02", new GlassMap("Hikari", "J-LAF02", 1.72, 1.715094, 1.731604));    // 43.61
        glasses.put("J-LAF04", new GlassMap("Hikari", "J-LAF04", 1.757, 1.752239, 1.768055));
        glasses.put("J-LAF05", new GlassMap("Hikari", "J-LAF05", 1.762, 1.756381, 1.775377));
        glasses.put("J-LAF09", new GlassMap("Hikari", "J-LAF09", 1.697, 1.692687, 1.707073));
        glasses.put("J-LAF010", new GlassMap("Hikari", "J-LAF010", 1.7432, 1.738649, 1.753737));
        glasses.put("J-LAF016", new GlassMap("Hikari", "J-LAF016", 1.801, 1.794267, 1.817203));
        glasses.put("J-LAF016HS", new GlassMap("Hikari", "J-LAF016HS", 1.801, 1.794267, 1.817203));
        glasses.put("J-LAFH3", new GlassMap("Hikari", "J-LAFH3", 1.79504, 1.787036, 1.814745));
        glasses.put("J-LAFH3HS", new GlassMap("Hikari", "J-LAFH3HS", 1.79504, 1.787036, 1.814745));
        glasses.put("J-LASF01", new GlassMap("Hikari", "J-LASF01", 1.7859, 1.780582, 1.798375)); // 44.17
        glasses.put("J-LASF02", new GlassMap("Hikari", "J-LASF02", 1.79952, 1.793865, 1.812862)); // 42.09
        glasses.put("J-LASF03", new GlassMap("Hikari", "J-LASF03", 1.8061, 1.800248, 1.819921)); // 40.97
        glasses.put("J-LASF05", new GlassMap("Hikari", "J-LASF05", 1.83481, 1.828989, 1.848524)); // 42.73
        glasses.put("J-LASF05HS", new GlassMap("Hikari", "J-LASF05HS", 1.83481, 1.828989, 1.848524)); // 42.73
        glasses.put("J-LASF08A", new GlassMap("Hikari", "J-LASF08A", 1.883, 1.876555, 1.898256)); // 40.69
        glasses.put("J-LASF09A", new GlassMap("Hikari", "J-LASF09A", 1.816, 1.810744, 1.828257)); // 46.59
        glasses.put("J-LASF010", new GlassMap("Hikari", "J-LASF010", 1.834, 1.827379, 1.849808)); // 37.18
        glasses.put("J-LASF013", new GlassMap("Hikari", "J-LASF013", 1.8044, 1.798372, 1.818682)); // 36.91
        glasses.put("J-LASF014", new GlassMap("Hikari", "J-LASF014", 1.788, 1.782997, 1.799638)); // 47.35
        glasses.put("J-LASF015", new GlassMap("Hikari", "J-LASF015", 1.804, 1.798824, 1.816078)); // 46.6
        glasses.put("J-LASF015HS", new GlassMap("Hikari", "J-LASF015HS", 1.804, 1.798824, 1.816078)); // 46.6
        glasses.put("J-LASF016", new GlassMap("Hikari", "J-LASF016", 1.7725, 1.767801, 1.78337)); // 46.62
        glasses.put("J-LASF017", new GlassMap("Hikari", "J-LASF017", 1.795, 1.789742, 1.807287)); // 45.31
        glasses.put("J-LASF021", new GlassMap("Hikari", "J-LASF021", 1.85026, 1.842602, 1.868883)); // 32.35
        glasses.put("J-LASF021HS", new GlassMap("Hikari", "J-LASF021HS", 1.85026, 1.842602, 1.868883)); // 32.25
        glasses.put("J-LASFH2", new GlassMap("Hikari", "J-LASFH2", 1.76684, 1.761914, 1.778307)); // 46.78
        glasses.put("J-LASFH6", new GlassMap("Hikari", "J-LASFH6", 1.8061, 1.799034, 1.823209)); // 33.34
        glasses.put("J-LASFH9A", new GlassMap("Hikari", "J-LASFH9A", 1.90265, 1.895235, 1.920469)); // 35.77
        glasses.put("J-LASFH13", new GlassMap("Hikari", "J-LASFH13", 1.90366, 1.895254, 1.924149)); // 31.27
        glasses.put("J-LASFH13HS", new GlassMap("Hikari", "J-LASFH13HS", 1.90366, 1.895254, 1.924149)); // 31.27
        glasses.put("J-LASFH15", new GlassMap("Hikari", "J-LASFH15", 1.95, 1.940626, 1.972976)); // 29.37
        glasses.put("J-LASFH15HS", new GlassMap("Hikari", "J-LASFH15HS", 1.95, 1.940626, 1.972976)); // 29.37
        glasses.put("J-LASFH16", new GlassMap("Hikari", "J-LASFH16", 2.001, 1.991039, 2.02541)); // 29.12
        glasses.put("J-LASFH17", new GlassMap("Hikari", "J-LASFH17", 2.00069, 1.989413, 2.028724)); // 25.46
        glasses.put("J-LASFH17HS", new GlassMap("Hikari", "J-LASFH17HS", 2.00069, 1.989413, 2.028724)); // 25.46
        glasses.put("J-LASFH21", new GlassMap("Hikari", "J-LASFH21", 1.95375, 1.945145, 1.974641)); // 32.33
        glasses.put("J-LASFH22", new GlassMap("Hikari", "J-LASFH22", 1.8485, 1.842718, 1.862094)); // 43.79
        glasses.put("J-LASFH23", new GlassMap("Hikari", "J-LASFH23", 1.85, 1.840948, 1.872398)); // 27.03
        glasses.put("J-LASFH24", new GlassMap("Hikari", "J-LASFH24", 1.902, 1.891774, 1.927478)); // 25.26
        glasses.put("J-LASFH24HS", new GlassMap("Hikari", "J-LASFH24HS", 1.902, 1.891774, 1.927478)); // 25.26
        glasses.put("Q-FK01AS", new GlassMap("Hikari", "Q-FK01AS", 1.49653, 1.49467, 1.500755));
        glasses.put("Q-FKH1S", new GlassMap("Hikari", "Q-FKH1S", 1.49731, 1.495471, 1.501498));
        glasses.put("Q-FKH2S", new GlassMap("Hikari", "Q-FKH2S", 1.45562, 1.454089, 1.459079));
        glasses.put("Q-PSKH1S", new GlassMap("Hikari", "Q-PSKH1S", 1.59255, 1.589901, 1.598633));   // 67.89
        glasses.put("Q-PSKH4S", new GlassMap("Hikari", "Q-PSKH4S", 1.59245, 1.589733, 1.598586));
        glasses.put("Q-PSKH52S", new GlassMap("Hikari", "Q-PSKH52S", 1.61875, 1.61579, 1.625499));
        glasses.put("Q-SK15S", new GlassMap("Hikari", "Q-SK15S", 1.62291, 1.619659, 1.630343));
        glasses.put("Q-SK52S", new GlassMap("Hikari", "Q-SK52S", 1.58286, 1.579869, 1.589664));
        glasses.put("Q-SK55S", new GlassMap("Hikari", "Q-SK55S", 1.58887, 1.58592, 1.595552));
        glasses.put("Q-SF6S", new GlassMap("Hikari", "Q-SF6S", 1.80301, 1.793991, 1.825442));
        glasses.put("Q-LAK52S",new GlassMap("Hikari", "Q-LAK52S",1.67798,1.674224,1.686575));
        glasses.put("Q-LAK53S",new GlassMap("Hikari", "Q-LAK53S",1.69343,1.689487,1.702497));
        glasses.put("Q-LAF010S",new GlassMap("Hikari", "Q-LAF010S",1.743,1.738458,1.753545));
        glasses.put("Q-LAFPH1S",new GlassMap("Hikari", "Q-LAFPH1S",1.743104,1.738575,1.753606));
        glasses.put("Q-LASF03S",new GlassMap("Hikari", "Q-LASF03S",1.80604,1.800166,1.819952));
        glasses.put("Q-LASFH11S", new GlassMap("Hikari", "Q-LASFH11S", 1.77387, 1.768949, 1.785326));
        glasses.put("Q-LASFH12S", new GlassMap("Hikari", "Q-LASFH12S", 1.79063, 1.78537, 1.802946));
        glasses.put("Q-LASFH58S", new GlassMap("Hikari", "Q-LASFH58S", 1.85108, 1.844781, 1.865992));
        glasses.put("Q-LASFH59S", new GlassMap("Hikari", "Q-LASFH59S", 1.82098, 1.815228, 1.834544));
        glasses.put("Q-LASFPH2S", new GlassMap("Hikari", "Q-LASFPH2S", 1.765437, 1.760519, 1.776892));
        glasses.put("Q-LASFPH3S", new GlassMap("Hikari", "Q-LASFPH3S", 1.795256, 1.789992, 1.807566));

        // CaF2
        glasses.put("CaF2", new GlassMap("CaF2", 1.43384, 1.43245, 1.437));

        glasses.put("E-FK5", new GlassMap("Hikari", "E-FK5", 1.48749, 1.48535, 1.49227));//487704
        glasses.put("E-FK01", new GlassMap("Hikari", "E-FK01", 1.497, 1.49514, 1.50123));//497816
        glasses.put("E-FKH1", new GlassMap("Hikari", "E-FKH1", 1.49782, 1.49598, 1.50201));//498825
        glasses.put("E-PKH1", new GlassMap("Hikari", "E-PKH1", 1.5186, 1.51631, 1.52373));//519700
        glasses.put("E-PSK02", new GlassMap("Hikari", "E-PSK02", 1.618, 1.61504, 1.62479));//618634
        glasses.put("E-PSK03", new GlassMap("Hikari", "E-PSK03", 1.603, 1.60019, 1.6094));//603655
        glasses.put("E-BK7", new GlassMap("Hikari", "E-BK7", 1.5168, 1.51432, 1.52238));//517641
        glasses.put("E-BAK1", new GlassMap("Hikari", "E-BAK1", 1.5725, 1.5695, 1.57941));//572577
        glasses.put("E-BAK2", new GlassMap("Hikari", "E-BAK2", 1.53996, 1.53719, 1.54628));//540595
        glasses.put("E-BAK4", new GlassMap("Hikari", "E-BAK4", 1.56883, 1.56577, 1.57587));//569563
        glasses.put("E-K3", new GlassMap("Hikari", "E-K3", 1.51823, 1.51555, 1.52435));//518589
        glasses.put("E-K5", new GlassMap("Hikari", "E-K5", 1.52249, 1.51982, 1.52857));//522597
        glasses.put("E-KF6", new GlassMap("Hikari", "E-KF6", 1.51742, 1.51444, 1.52433));//517523
        glasses.put("E-BALF4", new GlassMap("Hikari", "E-BALF4", 1.57957, 1.57632, 1.58711));//580537
        glasses.put("E-BAF3", new GlassMap("Hikari", "E-BAF3", 1.58267, 1.57893, 1.59146));//583465
        glasses.put("E-BAF4", new GlassMap("Hikari", "E-BAF4", 1.60562, 1.60151, 1.61535));//606438
        glasses.put("E-BAF8", new GlassMap("Hikari", "E-BAF8", 1.62374, 1.61978, 1.63304));//624470
        glasses.put("E-BAF10", new GlassMap("Hikari", "E-BAF10", 1.67003, 1.66579, 1.67997));//670472
        glasses.put("E-BAF11", new GlassMap("Hikari", "E-BAF11", 1.66672, 1.66259, 1.67639));//667483
        glasses.put("E-BAF12", new GlassMap("Hikari", "E-BAF12", 1.6393, 1.63506, 1.6493));//639449
        glasses.put("E-BASF2", new GlassMap("Hikari", "E-BASF2", 1.66446, 1.65904, 1.67754));//664359
        glasses.put("E-BASF6", new GlassMap("Hikari", "E-BASF6", 1.66755, 1.66285, 1.67875));//668420
        glasses.put("E-BASF7", new GlassMap("Hikari", "E-BASF7", 1.70154, 1.6965, 1.71354));//702412
        glasses.put("E-BASF8", new GlassMap("Hikari", "E-BASF8", 1.72342, 1.71781, 1.73688));//723379
        glasses.put("E-SK2", new GlassMap("Hikari", "E-SK2", 1.60738, 1.60414, 1.61485));//607567
        glasses.put("E-SK4", new GlassMap("Hikari", "E-SK4", 1.61272, 1.60955, 1.61998));//613587
        glasses.put("E-SK5", new GlassMap("Hikari", "E-SK5", 1.58913, 1.58619, 1.59582));//589612
        glasses.put("E-SK10", new GlassMap("Hikari", "E-SK10", 1.6228, 1.61949, 1.63041));//623570
        glasses.put("E-SK11", new GlassMap("Hikari", "E-SK11", 1.56384, 1.561, 1.57029));//564607
        glasses.put("E-SK14", new GlassMap("Hikari", "E-SK14", 1.60311, 1.60008, 1.61002));//603607
        glasses.put("E-SK15", new GlassMap("Hikari", "E-SK15", 1.62299, 1.61974, 1.63044));//623582
        glasses.put("E-SK16", new GlassMap("Hikari", "E-SK16", 1.62041, 1.61728, 1.62757));//620603
        glasses.put("E-SK18", new GlassMap("Hikari", "E-SK18", 1.63854, 1.63506, 1.64657));//639555
        glasses.put("E-SSK2", new GlassMap("Hikari", "E-SSK2", 1.6223, 1.61877, 1.6305));//622531
        glasses.put("E-SSK5", new GlassMap("Hikari", "E-SSK5", 1.65844, 1.65455, 1.66749));//658509
        glasses.put("E-SSK8", new GlassMap("Hikari", "E-SSK8", 1.61772, 1.614, 1.62641));//618498
        glasses.put("E-LLF1", new GlassMap("Hikari", "E-LLF1", 1.54814, 1.54457, 1.55654));//548458
        glasses.put("E-LLF2", new GlassMap("Hikari", "E-LLF2", 1.54072, 1.53729, 1.54874));//541472
        glasses.put("E-LLF6", new GlassMap("Hikari", "E-LLF6", 1.53172, 1.52846, 1.53934));//532489
        glasses.put("E-LF5", new GlassMap("Hikari", "E-LF5", 1.58144, 1.57722, 1.59149));//581407
        glasses.put("E-LF6", new GlassMap("Hikari", "E-LF6", 1.56732, 1.56339, 1.57667));//567427
        glasses.put("E-LF7", new GlassMap("Hikari", "E-LF7", 1.57501, 1.5709, 1.58476));//575415
        glasses.put("F2", new GlassMap("Hikari", "F2", 1.62004, 1.61503, 1.6321));//620363
        glasses.put("F5", new GlassMap("Hikari", "F5", 1.60342, 1.59875, 1.61459));//603381
        glasses.put("E-F1", new GlassMap("Hikari", "E-F1", 1.62588, 1.62073, 1.63828));//626356
        glasses.put("E-F2", new GlassMap("Hikari", "E-F2", 1.62004, 1.61502, 1.63213));//620363
        glasses.put("E-F3", new GlassMap("Hikari", "E-F3", 1.61293, 1.60805, 1.62462));//613370
        glasses.put("E-F5", new GlassMap("Hikari", "E-F5", 1.60342, 1.59875, 1.61462));//603380
        glasses.put("E-F8", new GlassMap("Hikari", "E-F8", 1.59551, 1.59102, 1.6062));//596392
        glasses.put("SF5", new GlassMap("Hikari", "SF5", 1.6727, 1.66661, 1.68753));//673322
        glasses.put("SF8", new GlassMap("Hikari", "SF8", 1.68893, 1.6825, 1.70462));//689311
        glasses.put("SF13", new GlassMap("Hikari", "SF13", 1.74077, 1.73307, 1.75986));//741276
        glasses.put("SF03", new GlassMap("Hikari", "SF03", 1.84666, 1.83653, 1.87198));//847239
        glasses.put("E-SF1", new GlassMap("Hikari", "E-SF1", 1.71736, 1.71033, 1.73463));//717295
        glasses.put("E-SF2", new GlassMap("Hikari", "E-SF2", 1.64769, 1.64209, 1.66126));//648338
        glasses.put("E-SF4", new GlassMap("Hikari", "E-SF4", 1.7552, 1.7473, 1.77475));//755275
        glasses.put("E-SF5", new GlassMap("Hikari", "E-SF5", 1.6727, 1.66661, 1.68756));//673321
        glasses.put("E-SF6", new GlassMap("Hikari", "E-SF6", 1.80518, 1.79611, 1.82777));//805254
        glasses.put("E-SF7", new GlassMap("Hikari", "E-SF7", 1.6398, 1.63439, 1.6529));//640346
        glasses.put("E-SF8", new GlassMap("Hikari", "E-SF8", 1.68893, 1.68249, 1.70467));//689311
        glasses.put("E-SF10", new GlassMap("Hikari", "E-SF10", 1.72825, 1.72086, 1.74645));//728285
        glasses.put("E-SF11", new GlassMap("Hikari", "E-SF11", 1.78472, 1.77596, 1.80652));//785257
        glasses.put("E-SF13", new GlassMap("Hikari", "E-SF13", 1.74077, 1.73309, 1.75975));//741278
        glasses.put("E-SF14", new GlassMap("Hikari", "E-SF14", 1.76182, 1.75358, 1.78226));//762266
        glasses.put("E-SF15", new GlassMap("Hikari", "E-SF15", 1.69895, 1.69222, 1.71542));//699301
        glasses.put("E-SF03", new GlassMap("Hikari", "E-SF03", 1.84666, 1.83649, 1.8721));//847238
        glasses.put("E-SFS3", new GlassMap("Hikari", "E-SFS3", 1.7847, 1.77613, 1.80597));//785263
        glasses.put("E-LAK7", new GlassMap("Hikari", "E-LAK7", 1.6516, 1.64821, 1.65934));//652585
        glasses.put("E-LAK8", new GlassMap("Hikari", "E-LAK8", 1.713, 1.70898, 1.72221));//713539
        glasses.put("E-LAK9", new GlassMap("Hikari", "E-LAK9", 1.691, 1.68716, 1.69976));//691548
        glasses.put("E-LAK10", new GlassMap("Hikari", "E-LAK10", 1.71999, 1.71568, 1.73001));//720502
        glasses.put("E-LAK12", new GlassMap("Hikari", "E-LAK12", 1.6779, 1.67419, 1.68642));//678554
        glasses.put("E-LAK13", new GlassMap("Hikari", "E-LAK13", 1.6935, 1.68955, 1.70258));//694532
        glasses.put("E-LAK14", new GlassMap("Hikari", "E-LAK14", 1.6968, 1.69297, 1.70552));//697555
        glasses.put("E-LAK18", new GlassMap("Hikari", "E-LAK18", 1.72916, 1.7251, 1.73844));//729547
        glasses.put("E-LAK01", new GlassMap("Hikari", "E-LAK01", 1.64, 1.63673, 1.64738));//640601
        glasses.put("E-LAK02", new GlassMap("Hikari", "E-LAK02", 1.67, 1.66645, 1.67813));//670573
        glasses.put("E-LAK04", new GlassMap("Hikari", "E-LAK04", 1.651, 1.64749, 1.65908));//651562
        glasses.put("E-LAK06", new GlassMap("Hikari", "E-LAK06", 1.6779, 1.67389, 1.68726));//678507
        glasses.put("E-LAK09", new GlassMap("Hikari", "E-LAK09", 1.734, 1.72969, 1.74394));//734515
        glasses.put("E-LAK011", new GlassMap("Hikari", "E-LAK011", 1.741, 1.73673, 1.7508));//741527
        glasses.put("E-LAKH1", new GlassMap("Hikari", "E-LAKH1", 1.7481, 1.74376, 1.75807));//748523
        glasses.put("E-LASKH2", new GlassMap("Hikari", "E-LASKH2", 1.755, 1.75062, 1.76506));//755523
        glasses.put("LAF7", new GlassMap("Hikari", "LAF7", 1.7495, 1.74323, 1.76453));//750352
        glasses.put("LAF9", new GlassMap("Hikari", "LAF9", 1.79504, 1.78698, 1.81482));//795286
        glasses.put("LAF11", new GlassMap("Hikari", "LAF11", 1.75692, 1.74996, 1.77383));//757317
        glasses.put("E-LAF2", new GlassMap("Hikari", "E-LAF2", 1.744, 1.73905, 1.75566));//744448
        glasses.put("E-LAF3", new GlassMap("Hikari", "E-LAF3", 1.717, 1.71253, 1.72749));//717479
        glasses.put("E-LAF7", new GlassMap("Hikari", "E-LAF7", 1.7495, 1.74327, 1.76452));//749353
        glasses.put("E-LAF9", new GlassMap("Hikari", "E-LAF9", 1.79504, 1.787, 1.81485));//795285
        glasses.put("E-LAF11", new GlassMap("Hikari", "E-LAF11", 1.75692, 1.74995, 1.77391));//757316
        glasses.put("E-LAF01", new GlassMap("Hikari", "E-LAF01", 1.7, 1.69564, 1.7102));//700481
        glasses.put("E-LAF02", new GlassMap("Hikari", "E-LAF02", 1.72, 1.71511, 1.73159));//720437
        glasses.put("E-LAF04", new GlassMap("Hikari", "E-LAF04", 1.757, 1.75223, 1.76806));//757478
        glasses.put("E-LAF05", new GlassMap("Hikari", "E-LAF05", 1.762, 1.75639, 1.77539));//762401
        glasses.put("E-LAF09", new GlassMap("Hikari", "E-LAF09", 1.697, 1.69271, 1.70707));//697485
        glasses.put("E-LAF010", new GlassMap("Hikari", "E-LAF010", 1.7432, 1.73865, 1.75372));//743493
        glasses.put("E-LAF016", new GlassMap("Hikari", "E-LAF016", 1.801, 1.79427, 1.81718));//801350
        glasses.put("E-LAFH2", new GlassMap("Hikari", "E-LAFH2", 1.80384, 1.7969, 1.82062));//804339
        glasses.put("E-LASF01", new GlassMap("Hikari", "E-LASF01", 1.7859, 1.78058, 1.79837));//786442
        glasses.put("E-LASF02", new GlassMap("Hikari", "E-LASF02", 1.79952, 1.79388, 1.8128));//800422
        glasses.put("E-LASF03", new GlassMap("Hikari", "E-LASF03", 1.8061, 1.80025, 1.81994));//806409
        glasses.put("E-LASF04", new GlassMap("Hikari", "E-LASF04", 1.81554, 1.81004, 1.82843));//816443
        glasses.put("E-LASF05", new GlassMap("Hikari", "E-LASF05", 1.83481, 1.82897, 1.84851));//835427
        glasses.put("E-LASF08", new GlassMap("Hikari", "E-LASF08", 1.883, 1.87656, 1.89822));//883408
        glasses.put("E-LASF09", new GlassMap("Hikari", "E-LASF09", 1.816, 1.81075, 1.82825));//816466
        glasses.put("E-LASF010", new GlassMap("Hikari", "E-LASF010", 1.834, 1.82738, 1.84982));//834372
        glasses.put("E-LASF013", new GlassMap("Hikari", "E-LASF013", 1.8044, 1.79837, 1.8187));//804396
        glasses.put("E-LASF014", new GlassMap("Hikari", "E-LASF014", 1.788, 1.783, 1.79963));//788474
        glasses.put("E-LASF015", new GlassMap("Hikari", "E-LASF015", 1.804, 1.79882, 1.81608));//804466
        glasses.put("E-LASF016", new GlassMap("Hikari", "E-LASF016", 1.7725, 1.7678, 1.78337));//772496
        glasses.put("E-LASF017", new GlassMap("Hikari", "E-LASF017", 1.795, 1.78974, 1.80729));//795453
        glasses.put("E-LASFH2", new GlassMap("Hikari", "E-LASFH2", 1.76684, 1.76192, 1.77831));//767468
        glasses.put("E-LASFH9", new GlassMap("Hikari", "E-LASFH9", 1.90265, 1.89522, 1.9205));//903357

        // Hoya
        glasses.put("FC5", new GlassMap("Hoya","FC5",1.48749,1.48535,1.49227)); // 70.44 487-704 0.009
        glasses.put("FCD1", new GlassMap("Hoya","FCD1",1.497,1.49514,1.50123)); // 81.61 497-816 0.0374
        glasses.put("FCD1B", new GlassMap("Hoya","FCD1B",1.4971,1.49524,1.50134)); // 81.56 497-816 0.0369
        glasses.put("FCD10A", new GlassMap("Hoya","FCD10A",1.4586,1.45704,1.46212)); // 90.19 459-902 0.049
        glasses.put("FCD100", new GlassMap("Hoya","FCD100",1.437,1.43559,1.44019)); // 95.1 437-951 0.0564
        glasses.put("FCD500", new GlassMap("Hoya","FCD500",1.55397,1.55162,1.55934)); // 71.76 554-718 0.02
        glasses.put("FCD515", new GlassMap("Hoya","FCD515",1.59282,1.59021,1.59884)); // 68.62 593-686 0.0192
        glasses.put("FCD600", new GlassMap("Hoya","FCD600",1.5941,1.59115,1.60097)); // 60.47 594-605 0.0156
        glasses.put("FCD615", new GlassMap("Hoya","FCD615",1.57144,1.56901,1.57699)); // 71.61 571-716 0.0224
        glasses.put("FCD705", new GlassMap("Hoya","FCD705",1.55032,1.5481,1.55539)); // 75.5 550-755 0.0276
        glasses.put("FCD915", new GlassMap("Hoya","FCD915",1.48071,1.47898,1.48462)); // 85.29 481-853 0.0413
        glasses.put("PCD4", new GlassMap("Hoya","PCD4",1.618,1.61503,1.62478)); // 63.4 618-634 0.0059
        glasses.put("PCD40", new GlassMap("Hoya","PCD40",1.61997,1.61701,1.62672)); // 63.88 620-639 0.0092
        glasses.put("PCD51", new GlassMap("Hoya","PCD51",1.59349,1.59078,1.59964)); // 67 593-670 0.0088
        glasses.put("BSC7", new GlassMap("Hoya","BSC7",1.5168,1.51432,1.52237)); // 64.2 517-642 0.0015
        glasses.put("E-C3", new GlassMap("Hoya","E-C3",1.51823,1.51556,1.52435)); // 58.96 518-590 0.002
        glasses.put("BAC4", new GlassMap("Hoya","BAC4",1.56883,1.56575,1.5759)); // 56.04 569-560 0.001
        glasses.put("BACD5", new GlassMap("Hoya","BACD5",1.58913,1.58619,1.59581)); // 61.25 589-613 0.0022
        glasses.put("BACD14",new GlassMap("Hoya", "BACD14",1.60311,1.60009,1.61002));
        glasses.put("BACD15",new GlassMap("Hoya", "BACD15",1.62299,1.61973,1.63045));
        glasses.put("BACD18",new GlassMap("Hoya", "BACD18",1.63854,1.63505,1.64657));
        glasses.put("BACD16", new GlassMap("Hoya","BACD16",1.62041,1.61727,1.62755)); // 60.35 620-604 -0.0003
        glasses.put("BACED5", new GlassMap("Hoya","BACED5",1.65844,1.65454,1.66749)); // 50.86 658-509 0.0008
        glasses.put("LAC8", new GlassMap("Hoya","LAC8",1.713,1.70898,1.7222)); // 53.94 713-539 -0.0071
        glasses.put("LAC14", new GlassMap("Hoya","LAC14",1.6968,1.69297,1.70553)); // 55.46 697-555 -0.006
        glasses.put("TAC6L", new GlassMap("Hoya","TAC6L",1.755,1.75063,1.76506)); // 52.32 755-523 -0.0068
        glasses.put("TAC6", new GlassMap("Hoya","TAC6",1.755,1.75063,1.76506)); // 52.32 755-523 -0.0069
        glasses.put("TAC8P", new GlassMap("Hoya","TAC8P",1.72916,1.72509,1.73846)); // 54.54 729-545 -0.0049
        glasses.put("TAC8", new GlassMap("Hoya","TAC8",1.72916,1.7251,1.73844)); // 54.67 729-547 -0.0046
        glasses.put("E-CF6", new GlassMap("Hoya","E-CF6",1.51742,1.51444,1.52436)); // 52.15 517-522 0.0045
        glasses.put("E-FEL1", new GlassMap("Hoya","E-FEL1",1.54814,1.54458,1.55654)); // 45.82 548-458 0.0041
        glasses.put("E-FEL2", new GlassMap("Hoya","E-FEL2",1.54072,1.5373,1.54876)); // 47.2 541-472 0.0044
        glasses.put("E-FL5", new GlassMap("Hoya","E-FL5",1.58144,1.57723,1.59145)); // 40.89 581-409 0.002
        glasses.put("E-FL6", new GlassMap("Hoya","E-FL6",1.56732,1.56339,1.57663)); // 42.84 567-428 0.0031
        glasses.put("E-F2", new GlassMap("Hoya","E-F2",1.62004,1.61502,1.6321)); // 36.3 620-363 0.0043
        glasses.put("E-F5", new GlassMap("Hoya","E-F5",1.60342,1.59874,1.61462)); // 38.01 603-380 0.0029
        glasses.put("E-FD1",new GlassMap("Hoya", "E-FD1",1.71736,1.71032,1.73464));
        glasses.put("E-FD1L", new GlassMap("Hoya","E-FD1L",1.71736,1.71032,1.73464)); // 29.5 717-295 0.0088
        glasses.put("E-FD2", new GlassMap("Hoya","E-FD2",1.64769,1.6421,1.66124)); // 33.84 648-338 0.0049
        glasses.put("E-FD4",new GlassMap("Hoya", "E-FD4",1.7552,1.74729,1.77473));
        glasses.put("E-FD4L", new GlassMap("Hoya","E-FD4L",1.7552,1.7473,1.77473)); // 27.53 755-275 0.0111
        glasses.put("E-FD5", new GlassMap("Hoya","E-FD5",1.6727,1.66661,1.68752)); // 32.17 673-322 0.0059
        glasses.put("E-FD8", new GlassMap("Hoya","E-FD8",1.68893,1.68251,1.70462)); // 31.16 689-312 0.0067
        glasses.put("E-FD80", new GlassMap("Hoya","E-FD80",1.6896,1.68318,1.70532)); // 31.14 690-311 0.0108
        glasses.put("E-FD10",new GlassMap("Hoya", "E-FD10",1.72825,1.72082,1.74654));
        glasses.put("E-FD10L", new GlassMap("Hoya","E-FD10L",1.72825,1.72083,1.74654)); // 28.32 728-283 0.0102
        glasses.put("E-FD13", new GlassMap("Hoya","E-FD13",1.74077,1.73307,1.75976)); // 27.76 741-278 0.0094
        glasses.put("E-FD15",new GlassMap("Hoya", "E-FD15",1.69895,1.69221,1.71547));
        glasses.put("E-FD15L", new GlassMap("Hoya","E-FD15L",1.69895,1.69221,1.71547)); // 30.05 699-301 0.0085
        glasses.put("FD60-W", new GlassMap("Hoya","FD60-W",1.80518,1.79611,1.82774)); // 25.46 805-255 0.0132
        glasses.put("FD60", new GlassMap("Hoya","FD60",1.80518,1.79611,1.82774)); // 25.46 805-255 0.0132
        glasses.put("FD110", new GlassMap("Hoya","FD110",1.78472,1.77597,1.80648)); // 25.72 785-257 0.0138
        glasses.put("FD140", new GlassMap("Hoya","FD140",1.76182,1.75359,1.78222)); // 26.61 762-266 0.0119
        glasses.put("FD225", new GlassMap("Hoya","FD225",1.80809,1.79799,1.83349)); // 22.76 808-228 0.0213
        glasses.put("FD270", new GlassMap("Hoya","FD270",1.6843,1.677,1.70252)); // 26.81 684-268 0.0231
        glasses.put("E-FDS1-W", new GlassMap("Hoya","E-FDS1-W",1.92286,1.91038,1.95457)); // 20.88 923-209 0.0283
        glasses.put("E-FDS1", new GlassMap("Hoya","E-FDS1",1.92286,1.91038,1.95457)); // 20.88 923-209 0.0283
        glasses.put("E-FDS2", new GlassMap("Hoya","E-FDS2",2.00272,1.98812,2.04003)); // 19.32 003-193 0.0316
        glasses.put("E-FDS3-W", new GlassMap("Hoya","E-FDS3-W",2.1042,2.08618,2.15106)); // 17.02 104-170 0.0454
        glasses.put("FDS16-W", new GlassMap("Hoya","FDS16-W",1.98612,1.96949,2.02931)); // 16.48 986-165 0.0469
        glasses.put("FDS165-W", new GlassMap("Hoya","FDS165-W",1.98611,1.97016,2.02733)); // 17.25 986-173 0.0431
        glasses.put("FDS18-W", new GlassMap("Hoya","FDS18-W",1.94594,1.93123,1.98383)); // 17.98 946-180 0.0386
        glasses.put("FDS18", new GlassMap("Hoya","FDS18",1.94594,1.93123,1.98383)); // 17.98 946-180 0.0386
        glasses.put("FDS20-W", new GlassMap("Hoya","FDS20-W",1.86966,1.85742,1.90086)); // 20.02 870-200 0.0312
        glasses.put("FDS24-W", new GlassMap("Hoya","FDS24-W",1.92119,1.9102,1.94865)); // 23.96 921-240 0.015
        glasses.put("FDS24-SW", new GlassMap("Hoya","FDS24-SW",1.92119,1.9102,1.94865)); // 23.96 921-240 0.015
        glasses.put("FDS90-SG",new GlassMap("Hoya", "FDS90-SG",1.84666,1.83649,1.87209));
        glasses.put("FDS90-SGP", new GlassMap("Hoya","FDS90-SGP",1.84666,1.83651,1.87203)); // 23.84 847-238 0.0147
        glasses.put("FDS90-SG", new GlassMap("Hoya","FDS90-SG",1.84666,1.83649,1.87209)); // 23.78 847-238 0.0137
        glasses.put("FDS90(P)",new GlassMap("Hoya", "FDS90(P)",1.84666,1.83653,1.87199));
        glasses.put("FF5", new GlassMap("Hoya","FF5",1.5927,1.58782,1.60454)); // 35.45 593-355 0.0082
        glasses.put("FF8", new GlassMap("Hoya","FF8",1.75211,1.74352,1.77355)); // 25.05 752-251 0.016
        glasses.put("BAFD7", new GlassMap("Hoya","BAFD7",1.70154,1.69651,1.71356)); // 41.15 702-412 0.0028
        glasses.put("BAFD8", new GlassMap("Hoya","BAFD8",1.72342,1.71781,1.73685)); // 37.99 723-380 0.0021
        glasses.put("LAF2", new GlassMap("Hoya","LAF2",1.744,1.73906,1.75563)); // 44.9 744-449 -0.0044
        glasses.put("LAF3", new GlassMap("Hoya","LAF3",1.717,1.71251,1.72745)); // 47.98 717-480 -0.0063
        glasses.put("LAF45", new GlassMap("Hoya","LAF45",1.61396,1.6098,1.62366)); // 44.29 614-443 -0.0054
        glasses.put("NBF1", new GlassMap("Hoya","NBF1",1.7433,1.73874,1.75384)); // 49.22 743-492 -0.0103
        glasses.put("NBFD3",new GlassMap("Hoya", "NBFD3",1.8045,1.79849,1.81879));
        glasses.put("NBFD10", new GlassMap("Hoya","NBFD10",1.834,1.82742,1.84975)); // 37.34 834-373 -0.0021
        glasses.put("NBFD11", new GlassMap("Hoya","NBFD11",1.7859,1.78053,1.79842)); // 43.94 786-439 -0.0081
        glasses.put("NBFD13", new GlassMap("Hoya","NBFD13",1.8061,1.80022,1.82001)); // 40.73 806-407 -0.0078
        glasses.put("NBFD15-W", new GlassMap("Hoya","NBFD15-W",1.8061,1.79902,1.82324)); // 33.27 806-333 0
        glasses.put("NBFD15", new GlassMap("Hoya","NBFD15",1.8061,1.79902,1.82324)); // 33.27 806-333 0
        glasses.put("NBFD25", new GlassMap("Hoya","NBFD25",1.85451,1.84473,1.8787)); // 25.15 855-252 0.0073
        glasses.put("NBFD26", new GlassMap("Hoya","NBFD26",1.83401,1.82475,1.85687)); // 25.97 834-260 0.0061
        glasses.put("NBFD265", new GlassMap("Hoya","NBFD265",1.95203,1.94158,1.97792)); // 26.2 952-262 0.0089
        glasses.put("NBFD27", new GlassMap("Hoya","NBFD27",1.9011,1.8915,1.9248)); // 27.06 901-271 0.0076
        glasses.put("NBFD29", new GlassMap("Hoya","NBFD29",1.77047,1.76293,1.78884)); // 29.74 770-297 0.0003
        glasses.put("NBFD30", new GlassMap("Hoya","NBFD30",1.85883,1.85052,1.87915)); // 30 859-300 0.0036
        glasses.put("NBFD32", new GlassMap("Hoya","NBFD32",1.73037,1.72375,1.74641)); // 32.23 730-322 -0.0004
        glasses.put("NBFD38", new GlassMap("Hoya","NBFD38",1.65253,1.64762,1.66415)); // 39.48 653-395 -0.0041
        glasses.put("TAF1", new GlassMap("Hoya","TAF1",1.7725,1.7678,1.78336)); // 49.63 773-496 -0.0086
        glasses.put("TAF3", new GlassMap("Hoya", "TAF3", 1.8042, 1.799, 1.8163));
        glasses.put("TAF3D", new GlassMap("Hoya","TAF3D",1.8042,1.799,1.8163)); // 46.5 804-465 -0.0074
        glasses.put("TAF48", new GlassMap("Hoya","TAF48",1.79091,1.78596,1.80241)); // 48.09 791-481 -0.0074
        glasses.put("TAFD5G", new GlassMap("Hoya","TAFD5G",1.83481,1.82898,1.84852)); // 42.72 835-427 -0.0067
        glasses.put("TAFD5F",new GlassMap("Hoya", "TAFD5F",1.83481,1.82898,1.84852));
        glasses.put("TAFD25", new GlassMap("Hoya","TAFD25",1.90366,1.89526,1.92412)); // 31.32 904-313 0.0028
        glasses.put("TAFD30", new GlassMap("Hoya","TAFD30",1.883,1.87657,1.89821)); // 40.81 883-408 -0.0093
        glasses.put("TAFD32", new GlassMap("Hoya","TAFD32",1.8707,1.86436,1.88573)); // 40.73 871-407 -0.0068
        glasses.put("TAFD33", new GlassMap("Hoya","TAFD33",1.881,1.8745,1.89644)); // 40.14 881-401 -0.006
        glasses.put("TAFD34", new GlassMap("Hoya","TAFD34",1.85033,1.84439,1.86431)); // 42.7 850-427 -0.0069
        glasses.put("TAFD35", new GlassMap("Hoya", "TAFD35", 1.91082, 1.90323, 1.92907));   // 35.25
        glasses.put("TAFD35L", new GlassMap("Hoya","TAFD35L",1.91082,1.90324,1.92907)); // 35.25 911-353 -0.0016
        glasses.put("TAFD37",new GlassMap("Hoya", "TAFD37",1.90043,1.89333,1.91742));
        glasses.put("TAFD37A", new GlassMap("Hoya","TAFD37A",1.90043,1.89333,1.91742)); // 37.37 900-374 -0.0044
        glasses.put("TAFD40-W", new GlassMap("Hoya","TAFD40-W",2.00069,1.98941,2.02872)); // 25.46 001-255 0.0111
        glasses.put("TAFD40", new GlassMap("Hoya","TAFD40",2.00069,1.98941,2.02872)); // 25.46 001-255 0.0111
        glasses.put("TAFD45L", new GlassMap("Hoya","TAFD45L",1.95375,1.94513,1.97465)); // 32.32 954-323 -0.0002
        glasses.put("TAFD45", new GlassMap("Hoya","TAFD45",1.95375,1.94513,1.97465)); // 32.32 954-323 0
        glasses.put("TAFD55-W", new GlassMap("Hoya","TAFD55-W",2.001,1.99105,2.0254)); // 29.13 001-291 0.0036
        glasses.put("TAFD55", new GlassMap("Hoya","TAFD55",2.001,1.99105,2.0254)); // 29.13 001-291 0.0036
        glasses.put("TAFD65", new GlassMap("Hoya","TAFD65",2.0509,2.03965,2.07865)); // 26.94 051-269 0.0053
        glasses.put("TAFD75-W", new GlassMap("Hoya","TAFD75-W",2.102,2.08854,2.13566)); // 23.39 102-234 0.014
        glasses.put("FCD10", new GlassMap("Hoya","FCD10",1.4565,1.45495,1.46001)); // 90.27 457-903 0.0488
        glasses.put("FCD505", new GlassMap("Hoya","FCD505",1.59282,1.59021,1.59884)); // 68.62 593-686 0.0192
        glasses.put("LBC3N", new GlassMap("Hoya","LBC3N",1.60625,1.60336,1.61288)); // 63.72 606-637 0.0108
        glasses.put("BACD2", new GlassMap("Hoya","BACD2",1.60738,1.60414,1.61485)); // 56.72 607-567 0.002
        glasses.put("BACD4", new GlassMap("Hoya","BACD4",1.61272,1.60954,1.62)); // 58.58 613-586 0.0019
        glasses.put("BACD11", new GlassMap("Hoya","BACD11",1.56384,1.56101,1.57028)); // 60.83 564-608 0.0019
        glasses.put("BACD14", new GlassMap("Hoya","BACD14",1.60311,1.60009,1.61002)); // 60.7 603-607 0.002
        glasses.put("BACD15", new GlassMap("Hoya","BACD15",1.62299,1.61973,1.63045)); // 58.12 623-581 0.0007
        glasses.put("BACD18", new GlassMap("Hoya","BACD18",1.63854,1.63505,1.64657)); // 55.45 639-555 -0.001
        glasses.put("LAC7", new GlassMap("Hoya","LAC7",1.6516,1.64821,1.65936)); // 58.4 652-584 -0.0035
        glasses.put("LAC9", new GlassMap("Hoya","LAC9",1.691,1.68715,1.69978)); // 54.7 691-547 -0.008
        glasses.put("LAC10", new GlassMap("Hoya","LAC10",1.72,1.71568,1.72998)); // 50.35 720-504 -0.0074
        glasses.put("LAC12", new GlassMap("Hoya","LAC12",1.6779,1.6742,1.68641)); // 55.52 678-555 -0.0033
        glasses.put("LAC13", new GlassMap("Hoya","LAC13",1.6935,1.68955,1.70255)); // 53.34 694-533 -0.0067
        glasses.put("LACL60", new GlassMap("Hoya","LACL60",1.64,1.63674,1.64737)); // 60.2 640-602 -0.0039
        glasses.put("E-FEL6", new GlassMap("Hoya","E-FEL6",1.53172,1.52847,1.53935)); // 48.84 532-488 0.0052
        glasses.put("E-F1", new GlassMap("Hoya","E-F1",1.62588,1.62074,1.63825)); // 35.74 626-357 0.0038
        glasses.put("E-F3", new GlassMap("Hoya","E-F3",1.61293,1.60805,1.62463)); // 36.96 613-370 0.0032
        glasses.put("E-F8", new GlassMap("Hoya","E-F8",1.59551,1.59103,1.60621)); // 39.22 596-392 0.0026
        glasses.put("E-FD1", new GlassMap("Hoya","E-FD1",1.71736,1.71032,1.73464)); // 29.5 717-295 0.0083
        glasses.put("E-FD4", new GlassMap("Hoya","E-FD4",1.7552,1.74729,1.77472)); // 27.53 755-275 0.0103
        glasses.put("E-FD7", new GlassMap("Hoya","E-FD7",1.6398,1.63439,1.6529)); // 34.57 640-346 0.0056
        glasses.put("E-FD10", new GlassMap("Hoya","E-FD10",1.72825,1.72082,1.74653)); // 28.32 728-283 0.0085
        glasses.put("E-FD15", new GlassMap("Hoya","E-FD15",1.69895,1.69221,1.71547)); // 30.05 699-301 0.0086
        glasses.put("E-FDS3", new GlassMap("Hoya","E-FDS3",2.1042,2.08618,2.15106)); // 17.02 104-170 0.0454
        glasses.put("FDS24", new GlassMap("Hoya","FDS24",1.92119,1.9102,1.94865)); // 23.96 921-240 0.015
        glasses.put("FDS90", new GlassMap("Hoya","FDS90",1.84666,1.83649,1.87209)); // 23.78 847-238 0.0137
        glasses.put("TAC2", new GlassMap("Hoya","TAC2",1.741,1.73672,1.75081)); // 52.61 741-526 -0.0092
        glasses.put("TAC4", new GlassMap("Hoya","TAC4",1.734,1.72965,1.74402)); // 51.05 734-511 -0.0064
        glasses.put("TAC6",new GlassMap("Hoya", "TAC6",1.755,1.75063,1.76506));
        glasses.put("BAF10", new GlassMap("Hoya","BAF10",1.67003,1.66579,1.67999)); // 47.2 670-472 0.0008
        glasses.put("BAF11", new GlassMap("Hoya","BAF11",1.66672,1.66262,1.67642)); // 48.3 667-483 0.0021
        glasses.put("E-ADF10", new GlassMap("Hoya","E-ADF10",1.6131,1.60895,1.62277)); // 44.36 613-444 -0.008
        glasses.put("E-ADF50", new GlassMap("Hoya","E-ADF50",1.65412,1.64921,1.66571)); // 39.62 654-396 -0.0033
        glasses.put("E-BACD10", new GlassMap("Hoya","E-BACD10",1.6228,1.61949,1.63043)); // 56.91 623-569 0.0009
        glasses.put("E-BACED20", new GlassMap("Hoya","E-BACED20",1.6485,1.64482,1.65705)); // 53.03 649-530 -0.0008
        glasses.put("E-BAF8", new GlassMap("Hoya","E-BAF8",1.62374,1.61978,1.63303)); // 47.05 624-471 -0.001
        glasses.put("E-LAF7", new GlassMap("Hoya","E-LAF7",1.7495,1.74325,1.76464)); // 35.04 750-350 0.0054
        glasses.put("NBFD3", new GlassMap("Hoya","NBFD3",1.8045,1.79849,1.81878)); // 39.64 805-396 -0.0055
        glasses.put("NBFD12", new GlassMap("Hoya","NBFD12",1.7995,1.79388,1.81276)); // 42.34 800-423 -0.0072
        glasses.put("TAF2", new GlassMap("Hoya","TAF2",1.7945,1.78925,1.80675)); // 45.39 795-454 -0.0094
        glasses.put("TAF3", new GlassMap("Hoya","TAF3",1.8042,1.799,1.8163)); // 46.5 804-465 -0.0066
        glasses.put("TAF4", new GlassMap("Hoya","TAF4",1.788,1.783,1.79959)); // 47.49 788-475 -0.0091
        glasses.put("TAF5", new GlassMap("Hoya","TAF5",1.816,1.81074,1.82827)); // 46.57 816-466 -0.0083
        glasses.put("TAFD5F", new GlassMap("Hoya","TAFD5F",1.83481,1.82898,1.84852)); // 42.72 835-427 -0.0064
        glasses.put("TAFD35", new GlassMap("Hoya","TAFD35",1.91082,1.90323,1.92907)); // 35.25 911-353 -0.0027
        glasses.put("TAFD37", new GlassMap("Hoya","TAFD37",1.90043,1.89333,1.91742)); // 37.37 900-374 -0.0039
        glasses.put("M-FCD1", new GlassMap("Hoya","M-FCD1",1.4971,1.49524,1.50134)); // 81.56 497-816 0.0369
        glasses.put("MP-FCD1-M20", new GlassMap("Hoya","MP-FCD1-M20",1.4969,1.49504,1.50114)); // 81.51 497-815 0.0342
        glasses.put("MC-FCD1-M20", new GlassMap("Hoya","MC-FCD1-M20",1.4969,1.49504,1.50114)); // 81.51 497-815 0.0342
        glasses.put("M-FCD500", new GlassMap("Hoya","M-FCD500",1.55332,1.55097,1.55869)); // 71.69 553-717 0.0211
        glasses.put("MP-FCD500-20", new GlassMap("Hoya","MP-FCD500-20",1.55352,1.55117,1.55889)); // 71.72 554-717 0.0205
        glasses.put("MC-FCD500-20", new GlassMap("Hoya","MC-FCD500-20",1.55352,1.55117,1.55889)); // 71.72 554-717 0.0205
        glasses.put("M-PCD4", new GlassMap("Hoya","M-PCD4",1.61881,1.61586,1.62555)); // 63.85 619-639 0.0083
        glasses.put("MP-PCD4-40", new GlassMap("Hoya","MP-PCD4-40",1.61921,1.61626,1.62595)); // 63.85 619-639 0.0083
        glasses.put("MC-PCD4-40", new GlassMap("Hoya","MC-PCD4-40",1.61921,1.61626,1.62595)); // 63.85 619-639 0.0083
        glasses.put("M-PCD51", new GlassMap("Hoya","M-PCD51",1.59201,1.58931,1.59814)); // 67.02 592-670 0.0081
        glasses.put("MP-PCD51-70", new GlassMap("Hoya","MP-PCD51-70",1.59271,1.59,1.59885)); // 66.97 593-670 0.0089
        glasses.put("MC-PCD51-70", new GlassMap("Hoya","MC-PCD51-70",1.59271,1.59,1.59885)); // 66.97 593-670 0.0089
        glasses.put("M-PCD55AR", new GlassMap("Hoya","M-PCD55AR",1.63858,1.63509,1.64666)); // 55.18 639-552 0.0042
        glasses.put("MP-PCD55AR", new GlassMap("Hoya","MP-PCD55AR",1.63858,1.63509,1.64666)); // 55.18 639-552 0.0042
        glasses.put("M-BACD5N", new GlassMap("Hoya","M-BACD5N",1.58913,1.58618,1.5958)); // 61.25 589-613 -0.0007
        glasses.put("MP-BACD5N", new GlassMap("Hoya","MP-BACD5N",1.58913,1.58618,1.5958)); // 61.25 589-613 -0.0007
        glasses.put("MC-BACD5N", new GlassMap("Hoya","MC-BACD5N",1.58913,1.58618,1.5958)); // 61.25 589-613 -0.0007
        glasses.put("M-BACD12", new GlassMap("Hoya","M-BACD12",1.58313,1.58014,1.58994)); // 59.46 583-595 -0.0008
        glasses.put("MP-BACD12", new GlassMap("Hoya","MP-BACD12",1.58313,1.58014,1.58994)); // 59.46 583-595 -0.0008
        glasses.put("MC-BACD12", new GlassMap("Hoya","MC-BACD12",1.58313,1.58014,1.58994)); // 59.46 583-595 -0.0008
        glasses.put("M-BACD15", new GlassMap("Hoya","M-BACD15",1.62263,1.61935,1.63005)); // 58.17 623-582 -0.0045
        glasses.put("MP-BACD15", new GlassMap("Hoya","MP-BACD15",1.62263,1.61935,1.63005)); // 58.17 623-582 -0.0045
        glasses.put("M-LAC130", new GlassMap("Hoya","M-LAC130",1.6935,1.68954,1.70258)); // 53.2 694-532 -0.0059
        glasses.put("MP-LAC130", new GlassMap("Hoya","MP-LAC130",1.6935,1.68954,1.70258)); // 53.2 694-532 -0.0059
        glasses.put("MC-LAC130", new GlassMap("Hoya","MC-LAC130",1.6935,1.68954,1.70258)); // 53.2 694-532 -0.0059
        glasses.put("M-LAC14", new GlassMap("Hoya","M-LAC14",1.6968,1.69297,1.70553)); // 55.46 697-555 -0.006
        glasses.put("MP-LAC14-80", new GlassMap("Hoya","MP-LAC14-80",1.6976,1.69377,1.70634)); // 55.51 698-555 -0.006
        glasses.put("M-TAC60",new GlassMap("Hoya", "M-TAC60",1.75501,1.75055,1.76531));
        glasses.put("MP-TAC60-90",new GlassMap("Hoya", "MP-TAC60-90",1.75591,1.75145,1.76622));
        glasses.put("M-TAC80", new GlassMap("Hoya","M-TAC80",1.72903,1.72494,1.73843)); // 54.04 729-540 -0.0064
        glasses.put("MP-TAC80-60", new GlassMap("Hoya","MP-TAC80-60",1.72963,1.72554,1.73903)); // 54.07 730-541 -0.0067
        glasses.put("M-FD80", new GlassMap("Hoya","M-FD80",1.68893,1.68252,1.70463)); // 31.16 689-312 0.0116
        glasses.put("MP-FD80", new GlassMap("Hoya","MP-FD80",1.68893,1.68252,1.70463)); // 31.16 689-312 0.0116
        glasses.put("MC-FD80", new GlassMap("Hoya","MC-FD80",1.68893,1.68252,1.70463)); // 31.16 689-312 0.0116
        glasses.put("M-FDS2", new GlassMap("Hoya","M-FDS2",2.00178,1.98721,2.03905)); // 19.32 002-193 0.0313
        glasses.put("MP-FDS2", new GlassMap("Hoya","MP-FDS2",2.00178,1.98721,2.03905)); // 19.32 002-193 0.0313
        glasses.put("MC-FDS2", new GlassMap("Hoya","MC-FDS2",2.00178,1.98721,2.03905)); // 19.32 002-193 0.0313
        glasses.put("M-FDS910", new GlassMap("Hoya","M-FDS910",1.82115,1.8114,1.84553)); // 24.06 821-241 0.0187
        glasses.put("MP-FDS910-50", new GlassMap("Hoya","MP-FDS910-50",1.82165,1.8119,1.84607)); // 24.04 822-240 0.0187
        glasses.put("MC-FDS910-50", new GlassMap("Hoya","MC-FDS910-50",1.82165,1.8119,1.84607)); // 24.04 822-240 0.0187
        glasses.put("M-NBFD10", new GlassMap("Hoya","M-NBFD10",1.83441,1.82781,1.85019)); // 37.29 834-373 -0.0039
        glasses.put("MP-NBFD10-20", new GlassMap("Hoya","MP-NBFD10-20",1.83461,1.82802,1.8504)); // 37.29 835-373 -0.0048
        glasses.put("M-NBFD130", new GlassMap("Hoya","M-NBFD130",1.8061,1.80022,1.82002)); // 40.73 806-407 -0.0056
        glasses.put("MP-NBFD130", new GlassMap("Hoya","MP-NBFD130",1.8061,1.80022,1.82002)); // 40.73 806-407 -0.0056
        glasses.put("MC-NBFD130", new GlassMap("Hoya","MC-NBFD130",1.8061,1.80022,1.82002)); // 40.73 806-407 -0.0056
        glasses.put("M-TAF31", new GlassMap("Hoya","M-TAF31",1.80139,1.7961,1.81373)); // 45.45 801-455 -0.0084
        glasses.put("MP-TAF31-15", new GlassMap("Hoya","MP-TAF31-15",1.80154,1.79625,1.81388)); // 45.47 802-455 -0.0085
        glasses.put("MC-TAF31-15", new GlassMap("Hoya","MC-TAF31-15",1.80154,1.79625,1.81388)); // 45.47 802-455 -0.0085
        glasses.put("M-TAF101", new GlassMap("Hoya","M-TAF101",1.76802,1.76331,1.77891)); // 49.24 768-492 -0.0082
        glasses.put("MP-TAF101-100", new GlassMap("Hoya","MP-TAF101-100",1.76902,1.76431,1.77991)); // 49.29 769-493 -0.008
        glasses.put("MC-TAF101-100", new GlassMap("Hoya","MC-TAF101-100",1.76902,1.76431,1.77991)); // 49.29 769-493 -0.008
        glasses.put("M-TAF105", new GlassMap("Hoya","M-TAF105",1.7725,1.76779,1.7834)); // 49.5 773-495 -0.0073
        glasses.put("MP-TAF105", new GlassMap("Hoya","MP-TAF105",1.7725,1.76779,1.7834)); // 49.5 773-495 -0.0073
        glasses.put("MC-TAF105", new GlassMap("Hoya","MC-TAF105",1.7725,1.76779,1.7834)); // 49.5 773-495 -0.0073
        glasses.put("MC-TAF115", new GlassMap("Hoya","MC-TAF115",1.77047,1.76577,1.78135)); // 49.46 770-495 -0.0073
        glasses.put("M-TAF401", new GlassMap("Hoya","M-TAF401",1.77377,1.76884,1.78524)); // 47.17 774-472 -0.0078
        glasses.put("MP-TAF401", new GlassMap("Hoya","MP-TAF401",1.77377,1.76884,1.78524)); // 47.17 774-472 -0.0078
        glasses.put("MC-TAF401", new GlassMap("Hoya","MC-TAF401",1.77377,1.76884,1.78524)); // 47.17 774-472 -0.0078
        glasses.put("M-TAFD51", new GlassMap("Hoya","M-TAFD51",1.8208,1.81507,1.83429)); // 42.71 821-427 -0.0072
        glasses.put("MP-TAFD51-50", new GlassMap("Hoya","MP-TAFD51-50",1.8213,1.81557,1.83479)); // 42.72 821-427 -0.0066
        glasses.put("MC-TAFD51-50", new GlassMap("Hoya","MC-TAFD51-50",1.8213,1.81557,1.83479)); // 42.72 821-427 -0.0066
        glasses.put("M-TAFD305", new GlassMap("Hoya","M-TAFD305",1.85135,1.84505,1.86628)); // 40.1 851-401 -0.0067
        glasses.put("MP-TAFD305", new GlassMap("Hoya","MP-TAFD305",1.85135,1.84505,1.86628)); // 40.1 851-401 -0.0067
        glasses.put("MC-TAFD305", new GlassMap("Hoya","MC-TAFD305",1.85135,1.84505,1.86628)); // 40.1 851-401 -0.0067
        glasses.put("M-TAFD315", new GlassMap("Hoya","M-TAFD315",1.85136,1.84505,1.8663)); // 40.07 851-401 -0.0074
        glasses.put("MC-TAFD315", new GlassMap("Hoya","MC-TAFD315",1.85136,1.84505,1.8663)); // 40.07 851-401 -0.0074
        glasses.put("M-TAFD307", new GlassMap("Hoya","M-TAFD307",1.88202,1.87504,1.89873)); // 37.22 882-372 -0.0044
        glasses.put("MP-TAFD307", new GlassMap("Hoya","MP-TAFD307",1.88202,1.87504,1.89873)); // 37.22 882-372 -0.0044
        glasses.put("MC-TAFD307", new GlassMap("Hoya","MC-TAFD307",1.88202,1.87504,1.89873)); // 37.22 882-372 -0.0044
        glasses.put("MC-TAFD317", new GlassMap("Hoya","MC-TAFD317",1.8812,1.87421,1.89792)); // 37.17 881-372 -0.0047
        glasses.put("M-LAC8", new GlassMap("Hoya","M-LAC8",1.713,1.70899,1.72221)); // 53.94 713-539 -0.0066
        glasses.put("MP-LAC8-30", new GlassMap("Hoya","MP-LAC8-30",1.7133,1.70929,1.72251)); // 53.95 713-540 -0.0064
        glasses.put("M-TAC60", new GlassMap("Hoya","M-TAC60",1.75501,1.75055,1.76531)); // 51.16 755-512 -0.0078
        glasses.put("MP-TAC60-90", new GlassMap("Hoya","MP-TAC60-90",1.75591,1.75145,1.76622)); // 51.18 756-512 -0.0079
        glasses.put("M-FDS1", new GlassMap("Hoya","M-FDS1",1.92286,1.91036,1.95456)); // 20.88 923-209 0.0263
        glasses.put("MP-FDS1", new GlassMap("Hoya","MP-FDS1",1.92286,1.91036,1.95456)); // 20.88 923-209 0.0263
        glasses.put("M-LAF81", new GlassMap("Hoya","M-LAF81",1.73077,1.72541,1.74345)); // 40.5 731-405 -0.004
        glasses.put("MP-LAF81", new GlassMap("Hoya","MP-LAF81",1.73077,1.72541,1.74345)); // 40.5 731-405 -0.004
        glasses.put("M-NBF1", new GlassMap("Hoya","M-NBF1",1.7433,1.73876,1.75383)); // 49.33 743-493 -0.0068
        glasses.put("MP-NBF1", new GlassMap("Hoya","MP-NBF1",1.7433,1.73876,1.75383)); // 49.33 743-493 -0.0068
        glasses.put("MC-NBF1", new GlassMap("Hoya","MC-NBF1",1.7433,1.73876,1.75383)); // 49.33 743-493 -0.0068
        glasses.put("MC-NBFD135", new GlassMap("Hoya","MC-NBFD135",1.80834,1.80247,1.82223)); // 40.92 808-409 -0.0061
        glasses.put("M-TAF1", new GlassMap("Hoya","M-TAF1",1.7725,1.76781,1.78342)); // 49.46 773-495 -0.0054
        glasses.put("MC-TAF1", new GlassMap("Hoya","MC-TAF1",1.7725,1.76781,1.78342)); // 49.46 773-495 -0.0054
        glasses.put("M-TAFD405", new GlassMap("Hoya","M-TAFD405",1.9515,1.94223,1.97413)); // 29.83 952-298 0.001
        glasses.put("MP-TAFD405", new GlassMap("Hoya","MP-TAFD405",1.9515,1.94223,1.97413)); // 29.83 952-298 0.001

        // Schott
        glasses.put("BK7G18", new GlassMap("Schott","BK7G18",1.51975,1.51724,1.52541)); // 63.58 520636.252 0.0007
        glasses.put("F2", new GlassMap("Schott","F2",1.62004,1.61503,1.63208)); // 36.37 620364.36 0.0002
        glasses.put("F2G12", new GlassMap("Schott","F2G12",1.62072,1.61573,1.63271)); // 36.56 621366.361 0.0008
        glasses.put("F2HT", new GlassMap("Schott","F2HT",1.62004,1.61503,1.63208)); // 36.37 620364.36 0.0002
        glasses.put("F2HTi", new GlassMap("Schott","F2HTi",1.62004,1.61503,1.63208)); // 36.37 620364.36 0.0002
        glasses.put("F5", new GlassMap("Schott","F5",1.60342,1.59875,1.61461)); // 38.03 603380.347 -0.0003
        glasses.put("FK5HTi", new GlassMap("Schott","FK5HTi",1.48748,1.48534,1.49225)); // 70.47 487705.245 0.0036
        glasses.put("K7",new GlassMap("Schott", "K7",1.51112,1.50854,1.517));
        glasses.put("K10", new GlassMap("Schott","K10",1.50137,1.49867,1.50756)); // 56.41 501564.252 -0.0015
        glasses.put("K5G20", new GlassMap("Schott","K5G20",1.52344,1.52065,1.52987)); // 56.76 523568.259 0.0017
        glasses.put("LAFN7",new GlassMap("Schott", "LAFN7",1.7495,1.74319,1.76464));
        glasses.put("LAK9G15", new GlassMap("Schott","LAK9G15",1.69064,1.6868,1.69941)); // 54.76 691548.353 -0.0055
        glasses.put("LASF35", new GlassMap("Schott","LASF35",2.02204,2.01185,2.04702)); // 29.06 22291.541 0.0033
        glasses.put("LF5", new GlassMap("Schott","LF5",1.58144,1.57723,1.59146)); // 40.85 581409.322 -0.0003
        glasses.put("LF5G19", new GlassMap("Schott","LF5G19",1.59655,1.59214,1.6071)); // 39.89 597399.33 0.0036
        glasses.put("LF5HTi", new GlassMap("Schott","LF5HTi",1.58144,1.57724,1.59145)); // 40.89 581409.322 -0.0004
        glasses.put("LLF1", new GlassMap("Schott","LLF1",1.54814,1.54457,1.55655)); // 45.75 548458.294 -0.0009
        glasses.put("LLF1HTi", new GlassMap("Schott","LLF1HTi",1.54815,1.54459,1.55653)); // 45.9 548459.294 -0.001
        glasses.put("N-BAF10", new GlassMap("Schott","N-BAF10",1.67003,1.66578,1.68)); // 47.11 670471.375 -0.0016
        glasses.put("N-BAF4", new GlassMap("Schott","N-BAF4",1.60568,1.60157,1.61542)); // 43.72 606437.289 0.003
        glasses.put("N-BAF51", new GlassMap("Schott","N-BAF51",1.65224,1.64792,1.66243)); // 44.96 652450.333 -0.0012
        glasses.put("N-BAF52", new GlassMap("Schott","N-BAF52",1.60863,1.60473,1.61779)); // 46.6 609466.305 0.0024
        glasses.put("N-BAK1", new GlassMap("Schott","N-BAK1",1.5725,1.56949,1.57943)); // 57.55 573576.319 0.0002
        glasses.put("N-BAK2", new GlassMap("Schott","N-BAK2",1.53996,1.53721,1.54625)); // 59.71 540597.286 0.0004
        glasses.put("N-BAK4", new GlassMap("Schott","N-BAK4",1.56883,1.56575,1.57591)); // 55.98 569560.305 -0.001
        glasses.put("N-BAK4HT", new GlassMap("Schott","N-BAK4HT",1.56883,1.56575,1.57591)); // 55.98 569560.305 -0.001
        glasses.put("N-BALF4", new GlassMap("Schott","N-BALF4",1.57956,1.57631,1.58707)); // 53.87 580539.311 -0.0012
        glasses.put("N-BALF5", new GlassMap("Schott","N-BALF5",1.54739,1.5443,1.55451)); // 53.63 547536.261 -0.0004
        glasses.put("N-BASF2", new GlassMap("Schott","N-BASF2",1.66446,1.65905,1.67751)); // 36 664360.315 0.0057
        glasses.put("N-BASF64", new GlassMap("Schott","N-BASF64",1.704,1.69872,1.71659)); // 39.38 704394.32 -0.0006
        glasses.put("N-BK10", new GlassMap("Schott","N-BK10",1.49782,1.49552,1.50296)); // 66.95 498670.239 -0.0008
        glasses.put("N-BK7", new GlassMap("Schott","N-BK7",1.5168,1.51432,1.52238)); // 64.17 517642.251 -0.0009
        glasses.put("N-BK7HT", new GlassMap("Schott","N-BK7HT",1.5168,1.51432,1.52238)); // 64.17 517642.251 -0.0009
        glasses.put("N-BK7HTi", new GlassMap("Schott","N-BK7HTi",1.5168,1.51432,1.52238)); // 64.17 517642.251 -0.0009
        glasses.put("N-F2", new GlassMap("Schott","N-F2",1.62005,1.61506,1.63208)); // 36.43 620364.265 0.0056
        glasses.put("N-FK5", new GlassMap("Schott","N-FK5",1.48749,1.48535,1.49227)); // 70.41 487704.245 0.0036
        glasses.put("N-FK51A", new GlassMap("Schott","N-FK51A",1.48656,1.4848,1.49056)); // 84.47 487845.368 0.0342
        glasses.put("N-FK58", new GlassMap("Schott","N-FK58",1.456,1.45446,1.45948)); // 90.9 456909.365 0.0438
        glasses.put("N-K5", new GlassMap("Schott","N-K5",1.52249,1.51982,1.5286)); // 59.48 522595.259 0
        glasses.put("N-KF9", new GlassMap("Schott","N-KF9",1.52346,1.5204,1.53056)); // 51.54 523515.25 -0.0014
        glasses.put("N-KZFS11", new GlassMap("Schott","N-KZFS11",1.63775,1.63324,1.64828)); // 42.41 638424.32 -0.012
        glasses.put("N-KZFS2", new GlassMap("Schott","N-KZFS2",1.55836,1.55519,1.56553)); // 54.01 558540.254 -0.0111
        glasses.put("N-KZFS4", new GlassMap("Schott","N-KZFS4",1.61336,1.60922,1.623)); // 44.49 613445.3 -0.01
        glasses.put("N-KZFS4HT",new GlassMap("Schott", "N-KZFS4HT",1.61336,1.60922,1.623));
        glasses.put("N-KZFS5", new GlassMap("Schott","N-KZFS5",1.65412,1.64922,1.6657)); // 39.7 654397.304 -0.006
        glasses.put("N-KZFS8", new GlassMap("Schott","N-KZFS8",1.72047,1.71437,1.73513)); // 34.7 720347.32 -0.0021
        glasses.put("N-LAF2", new GlassMap("Schott","N-LAF2",1.74397,1.73903,1.75562)); // 44.85 744449.43 -0.0027
        glasses.put("N-LAF21", new GlassMap("Schott","N-LAF21",1.788,1.78301,1.7996)); // 47.49 788475.428 -0.0084
        glasses.put("N-LAF33", new GlassMap("Schott","N-LAF33",1.78582,1.78049,1.79833)); // 44.05 786441.436 -0.0071
        glasses.put("N-LAF34", new GlassMap("Schott","N-LAF34",1.7725,1.7678,1.78337)); // 49.62 773496.424 -0.0085
        glasses.put("N-LAF35",new GlassMap("Schott", "N-LAF35",1.7433,1.73876,1.75381));
        glasses.put("N-LAF7", new GlassMap("Schott","N-LAF7",1.7495,1.7432,1.76472)); // 34.82 749348.373 0.0042
        glasses.put("N-LAK10", new GlassMap("Schott","N-LAK10",1.72003,1.71572,1.72995)); // 50.62 720506.369 -0.0072
        glasses.put("N-LAK12", new GlassMap("Schott","N-LAK12",1.6779,1.67419,1.68647)); // 55.2 678552.41 -0.0024
        glasses.put("N-LAK14", new GlassMap("Schott","N-LAK14",1.6968,1.69297,1.70554)); // 55.41 697554.363 -0.0079
        glasses.put("N-LAK21", new GlassMap("Schott","N-LAK21",1.64049,1.63724,1.6479)); // 60.1 640601.374 -0.0017
        glasses.put("N-LAK22", new GlassMap("Schott","N-LAK22",1.65113,1.6476,1.65925)); // 55.89 651559.377 -0.0031
        glasses.put("N-LAK28", new GlassMap("Schott","N-LAK28",1.74429,1.73985,1.75451)); // 50.77 744508.409 -0.0085
        glasses.put("N-LAK33B", new GlassMap("Schott","N-LAK33B",1.755,1.75062,1.76506)); // 52.3 755523.422 -0.0085
        glasses.put("N-LAK34", new GlassMap("Schott","N-LAK34",1.72916,1.72509,1.73847)); // 54.5 729545.402 -0.0079
        glasses.put("N-LAK7", new GlassMap("Schott","N-LAK7",1.6516,1.64821,1.65934)); // 58.52 652585.384 -0.0021
        glasses.put("N-LAK8", new GlassMap("Schott","N-LAK8",1.713,1.70897,1.72222)); // 53.83 713538.375 -0.0083
        glasses.put("N-LAK9", new GlassMap("Schott","N-LAK9",1.691,1.68716,1.69979)); // 54.71 691547.351 -0.0071
        glasses.put("N-LASF31A", new GlassMap("Schott","N-LASF31A",1.883,1.87656,1.89822)); // 40.76 883408.551 -0.0085
        glasses.put("N-LASF40", new GlassMap("Schott","N-LASF40",1.83404,1.82745,1.84981)); // 37.3 834373.443 -0.0024
        glasses.put("N-LASF41", new GlassMap("Schott","N-LASF41",1.83501,1.82923,1.84859)); // 43.13 835431.485 -0.0083
        glasses.put("N-LASF43", new GlassMap("Schott","N-LASF43",1.8061,1.8002,1.82005)); // 40.61 806406.426 -0.0052
        glasses.put("N-LASF44", new GlassMap("Schott","N-LASF44",1.8042,1.79901,1.8163)); // 46.5 804465.444 -0.0084
        glasses.put("N-LASF45", new GlassMap("Schott","N-LASF45",1.80107,1.79436,1.81726)); // 34.97 801350.363 0.0009
        glasses.put("N-LASF45HT",new GlassMap("Schott", "N-LASF45HT",1.80107,1.79436,1.81726));
        glasses.put("N-LASF46A",new GlassMap("Schott", "N-LASF46A",1.90366,1.89526,1.92411));
        glasses.put("N-LASF46B", new GlassMap("Schott","N-LASF46B",1.90366,1.89526,1.92411)); // 31.32 904313.451 0.0045
        glasses.put("N-LASF55", new GlassMap("Schott","N-LASF55",1.9538,1.94473,1.97594)); // 30.56 954306.486 0.0037
        glasses.put("N-LASF9", new GlassMap("Schott","N-LASF9",1.85025,1.84255,1.86898)); // 32.17 850322.441 0.0037
        glasses.put("N-LASF9HT",new GlassMap("Schott", "N-LASF9HT",1.85025,1.84255,1.86898));
        glasses.put("N-PK51", new GlassMap("Schott","N-PK51",1.52855,1.52646,1.53333)); // 76.98 529770.386 0.0258
        glasses.put("N-PK52A", new GlassMap("Schott","N-PK52A",1.497,1.49514,1.50123)); // 81.61 497816.37 0.0311
        glasses.put("N-PSK3", new GlassMap("Schott","N-PSK3",1.55232,1.54965,1.55835)); // 63.46 552635.291 -0.0005
        glasses.put("N-PSK53A", new GlassMap("Schott","N-PSK53A",1.618,1.61503,1.62478)); // 63.39 618634.357 0.0052
        glasses.put("N-SF1", new GlassMap("Schott","N-SF1",1.71736,1.71035,1.73457)); // 29.62 717296.303 0.0097
        glasses.put("N-SF10", new GlassMap("Schott","N-SF10",1.72828,1.72091,1.74643)); // 28.53 728285.305 0.0108
        glasses.put("N-SF11", new GlassMap("Schott","N-SF11",1.78472,1.77596,1.80651)); // 25.68 785257.322 0.015
        glasses.put("N-SF14", new GlassMap("Schott","N-SF14",1.76182,1.75356,1.78228)); // 26.53 762265.312 0.013
        glasses.put("N-SF15", new GlassMap("Schott","N-SF15",1.69892,1.69222,1.71536)); // 30.2 699302.292 0.0108
        glasses.put("N-SF2", new GlassMap("Schott","N-SF2",1.64769,1.6421,1.66125)); // 33.82 648338.272 0.0081
        glasses.put("N-SF4", new GlassMap("Schott","N-SF4",1.75513,1.74719,1.77477)); // 27.38 755274.315 0.0118
        glasses.put("N-SF5", new GlassMap("Schott","N-SF5",1.67271,1.66664,1.6875)); // 32.25 673323.286 0.0088
        glasses.put("N-SF57", new GlassMap("Schott","N-SF57",1.84666,1.8365,1.8721)); // 23.78 847238.353 0.0178
        glasses.put("N-SF57HT", new GlassMap("Schott","N-SF57HT",1.84666,1.8365,1.8721)); // 23.78 847238.353 0.0178
        glasses.put("N-SF57HTultra", new GlassMap("Schott","N-SF57HTultra",1.84666,1.8365,1.8721)); // 23.78 847238.353 0.0178
        glasses.put("N-SF6", new GlassMap("Schott","N-SF6",1.80518,1.79608,1.82783)); // 25.36 805254.337 0.0146
        glasses.put("N-SF66", new GlassMap("Schott","N-SF66",1.92286,1.91039,1.95459)); // 20.88 923209.4 0.0307
        glasses.put("N-SF6HT", new GlassMap("Schott","N-SF6HT",1.80518,1.79608,1.82783)); // 25.36 805254.337 0.0146
        glasses.put("N-SF6HTultra", new GlassMap("Schott","N-SF6HTultra",1.80518,1.79608,1.82783)); // 25.36 805254.337 0.0146
        glasses.put("N-SF8", new GlassMap("Schott","N-SF8",1.68894,1.68254,1.70455)); // 31.31 689313.29 0.0087
        glasses.put("N-SK11", new GlassMap("Schott","N-SK11",1.56384,1.56101,1.57028)); // 60.8 564608.308 -0.0004
        glasses.put("N-SK14", new GlassMap("Schott","N-SK14",1.60311,1.60008,1.61003)); // 60.6 603606.343 -0.0003
        glasses.put("N-SK16", new GlassMap("Schott","N-SK16",1.62041,1.61727,1.62756)); // 60.32 620603.358 -0.0011
        glasses.put("N-SK2", new GlassMap("Schott","N-SK2",1.60738,1.60414,1.61486)); // 56.65 607567.355 -0.0008
        glasses.put("N-SK2HT", new GlassMap("Schott","N-SK2HT",1.60738,1.60414,1.61486)); // 56.65 607567.355 -0.0008
        glasses.put("N-SK4", new GlassMap("Schott","N-SK4",1.61272,1.60954,1.61999)); // 58.63 613586.354 -0.0004
        glasses.put("N-SK5", new GlassMap("Schott","N-SK5",1.58913,1.58619,1.59581)); // 61.27 589613.33 -0.0007
        glasses.put("N-SK5HTi", new GlassMap("Schott","N-SK5HTi",1.58913,1.58619,1.59581)); // 61.27 589613.33 -0.0007
        glasses.put("N-SSK2", new GlassMap("Schott","N-SSK2",1.62229,1.61877,1.63045)); // 53.27 622533.353 -0.0016
        glasses.put("N-SSK5", new GlassMap("Schott","N-SSK5",1.65844,1.65455,1.66749)); // 50.88 658509.371 -0.0007
        glasses.put("N-SSK8", new GlassMap("Schott","N-SSK8",1.61773,1.61401,1.62641)); // 49.83 618498.327 0.0002
        glasses.put("N-ZK7",new GlassMap("Schott", "N-ZK7",1.50847,1.50592,1.51423));
        glasses.put("N-ZK7A", new GlassMap("Schott","N-ZK7A",1.50805,1.5055,1.51382)); // 61.04 508610.247 -0.0043
        glasses.put("P-BK7",new GlassMap("Schott", "P-BK7",1.5164,1.51392,1.52198));
        glasses.put("P-LAF37", new GlassMap("Schott","P-LAF37",1.7555,1.75054,1.76708)); // 45.66 755457.399 -0.008
        glasses.put("P-LAK35", new GlassMap("Schott","P-LAK35",1.6935,1.68955,1.70259)); // 53.2 693532.385 -0.0061
        glasses.put("P-LASF47", new GlassMap("Schott","P-LASF47",1.8061,1.80023,1.81994)); // 40.9 806409.454 -0.0079
        glasses.put("P-LASF50",new GlassMap("Schott", "P-LASF50",1.8086,1.80266,1.82264));
        glasses.put("P-LASF51",new GlassMap("Schott", "P-LASF51",1.81,1.80411,1.8239));
        glasses.put("P-SF68", new GlassMap("Schott","P-SF68",2.0052,1.99171,2.03958)); // 21 5210.619 0.0308
        glasses.put("P-SF69", new GlassMap("Schott","P-SF69",1.7225,1.71535,1.74007)); // 29.23 723292.293 0.0104
        glasses.put("P-SF8",new GlassMap("Schott", "P-SF8",1.68893,1.68252,1.70457));
        glasses.put("P-SK57", new GlassMap("Schott","P-SK57",1.587,1.58399,1.59384)); // 59.6 587596.301 -0.0024
        glasses.put("P-SK57Q1",new GlassMap("Schott", "P-SK57Q1",1.586,1.58299,1.59284));
        glasses.put("P-SK58A",new GlassMap("Schott", "P-SK58A",1.58913,1.58618,1.59581));
        glasses.put("P-SK60", new GlassMap("Schott","P-SK60",1.61035,1.60714,1.61768)); // 57.9 610579.308 -0.0037
        glasses.put("SF1", new GlassMap("Schott","SF1",1.71736,1.71031,1.73462)); // 29.51 717295.446 0.0042
        glasses.put("SF10", new GlassMap("Schott","SF10",1.72825,1.72085,1.74648)); // 28.41 728284.428 0.0085
        glasses.put("SF11", new GlassMap("Schott","SF11",1.78472,1.77599,1.80645)); // 25.76 785258.474 0.0142
        glasses.put("SF2", new GlassMap("Schott","SF2",1.64769,1.6421,1.66123)); // 33.85 648339.386 0.0017
        glasses.put("SF3", new GlassMap("Schott","SF3",1.74,1.73242,1.75866)); // 28.2 740282.464 0.0056
        glasses.put("SF4", new GlassMap("Schott","SF4",1.7552,1.7473,1.77468)); // 27.58 755276.479 0.0062
        glasses.put("SF5", new GlassMap("Schott","SF5",1.6727,1.66661,1.6875)); // 32.21 673322.407 0.0023
        glasses.put("SF56A", new GlassMap("Schott","SF56A",1.7847,1.77605,1.80615)); // 26.08 785261.492 0.0098
        glasses.put("SF57", new GlassMap("Schott","SF57",1.84666,1.8365,1.87204)); // 23.83 847238.551 0.0123
        glasses.put("SF57HTultra", new GlassMap("Schott","SF57HTultra",1.84666,1.8365,1.87204)); // 23.83 847238.551 0.0123
        glasses.put("SF6", new GlassMap("Schott","SF6",1.80518,1.79609,1.82775)); // 25.43 805254.518 0.0092
        glasses.put("SF6G05", new GlassMap("Schott","SF6G05",1.80906,1.79988,1.8319)); // 25.27 809253.52
        glasses.put("SF6HT", new GlassMap("Schott","SF6HT",1.80518,1.79609,1.82775)); // 25.43 805254.518 0.0092


        // Ohara
        glasses.put("S-FPL51", new GlassMap("Ohara","S-FPL51",1.496999,1.495136,1.501231)); // 81.6 497816 0.028
        glasses.put("S-FPL53", new GlassMap("Ohara","S-FPL53",1.43875,1.437333,1.441955)); // 95 439950 0.0461
        glasses.put("S-FPL55", new GlassMap("Ohara","S-FPL55",1.43875,1.437328,1.441963)); // 94.8 439948 0.0457
        glasses.put("S-FPM2", new GlassMap("Ohara","S-FPM2",1.59522,1.592555,1.601342)); // 67.7 595677 0.0123
        glasses.put("S-FPM3", new GlassMap("Ohara","S-FPM3",1.53775,1.535554,1.542753)); // 74.7 538747 0.0186
        glasses.put("S-FPM4", new GlassMap("Ohara","S-FPM4",1.52841,1.526303,1.533214)); // 76.5 528765 0.0218
        glasses.put("S-FPM5", new GlassMap("Ohara","S-FPM5",1.552,1.549625,1.557433)); // 70.8 552708 0.015
        glasses.put("S-FSL5", new GlassMap("Ohara","S-FSL5",1.48749,1.485344,1.492285)); // 70.2 487702 0.0022
        glasses.put("S-BSL7", new GlassMap("Ohara","S-BSL7",1.51633,1.513855,1.521905)); // 64.1 516641 -0.0024
        glasses.put("S-BSM2", new GlassMap("Ohara","S-BSM2",1.607379,1.604144,1.614835)); // 56.8 607568 -0.0013
        glasses.put("S-BSM10", new GlassMap("Ohara","S-BSM10",1.622799,1.619489,1.630405)); // 57 623570 -0.0028
        glasses.put("S-BSM14", new GlassMap("Ohara","S-BSM14",1.603112,1.600079,1.610024)); // 60.7 603607 -0.0019
        glasses.put("S-BSM15", new GlassMap("Ohara","S-BSM15",1.622992,1.619739,1.63045)); // 58.2 623582 -0.0016
        glasses.put("S-BSM16", new GlassMap("Ohara","S-BSM16",1.620411,1.617276,1.627566)); // 60.3 620603 -0.0012
        glasses.put("S-BSM18", new GlassMap("Ohara","S-BSM18",1.638539,1.635051,1.646582)); // 55.4 639554 -0.0035
        glasses.put("S-BSM25", new GlassMap("Ohara","S-BSM25",1.658441,1.654553,1.667495)); // 50.9 658509 -0.0031
        glasses.put("S-BSM28", new GlassMap("Ohara","S-BSM28",1.617722,1.614005,1.626406)); // 49.8 618498 -0.0006
        glasses.put("S-BSM71", new GlassMap("Ohara","S-BSM71",1.648498,1.644815,1.657046)); // 53 649530 -0.001
        glasses.put("S-BSM81", new GlassMap("Ohara","S-BSM81",1.639999,1.636728,1.647381)); // 60.1 640601 -0.0073
        glasses.put("S-NSL3", new GlassMap("Ohara","S-NSL3",1.518229,1.515556,1.524354)); // 59 518590 -0.0005
        glasses.put("S-NSL36", new GlassMap("Ohara","S-NSL36",1.517417,1.514444,1.524313)); // 52.4 517524 -0.0002
        glasses.put("S-NSL33", new GlassMap("Ohara", "S-NSL33", 1.5145364, 1.5116932, 1.5210974)); // 54.71
        glasses.put("S-BAL2", new GlassMap("Ohara", "S-BAL2", 1.57099, 1.56762, 1.57886));
        glasses.put("S-BAL3", new GlassMap("Ohara","S-BAL3",1.571351,1.568105,1.578895)); // 53 571530 -0.0005
        glasses.put("S-BAL12", new GlassMap("Ohara","S-BAL12",1.539956,1.537194,1.546275)); // 59.5 540595 -0.0012
        glasses.put("S-BAL14", new GlassMap("Ohara","S-BAL14",1.568832,1.565775,1.575867)); // 56.3 569563 -0.0014
        glasses.put("S-BAL35", new GlassMap("Ohara","S-BAL35",1.58913,1.586188,1.595824)); // 61.2 589612 -0.0018
        glasses.put("S-BAL41",new GlassMap("Ohara", "S-BAL41",1.56384,1.561,1.57029));
        glasses.put("S-BAL42", new GlassMap("Ohara","S-BAL42",1.583126,1.580139,1.58996)); // 59.4 583594 -0.002
        glasses.put("S-BAM4", new GlassMap("Ohara","S-BAM4",1.60562,1.601507,1.615364)); // 43.7 606437 0.0013
        glasses.put("S-BAM12", new GlassMap("Ohara","S-BAM12",1.6393,1.635057,1.649304)); // 44.9 639449 -0.0006
        glasses.put("S-BAH10",new GlassMap("Ohara", "S-BAH10",1.67003,1.66579,1.67997));
        glasses.put("S-BAH11", new GlassMap("Ohara","S-BAH11",1.666718,1.662589,1.676386)); // 48.3 667483 -0.0024
        glasses.put("S-BAH27", new GlassMap("Ohara","S-BAH27",1.701536,1.696503,1.713515)); // 41.2 702412 0.0018
        glasses.put("S-BAH28", new GlassMap("Ohara","S-BAH28",1.72342,1.717816,1.736876)); // 38 723380 0.0035
        glasses.put("S-PHM52", new GlassMap("Ohara","S-PHM52",1.618,1.615036,1.624794)); // 63.4 618634 0.0051
        glasses.put("S-PHM52Q", new GlassMap("Ohara","S-PHM52Q",1.618,1.615029,1.624789)); // 63.3 618633 0.0036
        glasses.put("S-PHM53", new GlassMap("Ohara","S-PHM53",1.603001,1.600189,1.609404)); // 65.5 603655 0.0045
        glasses.put("S-TIL1", new GlassMap("Ohara","S-TIL1",1.548141,1.544572,1.556544)); // 45.8 548458 0.0012
        glasses.put("S-TIL2", new GlassMap("Ohara","S-TIL2",1.54072,1.537297,1.548746)); // 47.2 541472 0
        glasses.put("S-TIL6", new GlassMap("Ohara","S-TIL6",1.531717,1.528456,1.539343)); // 48.9 532489 0.0007
        glasses.put("S-TIL25", new GlassMap("Ohara","S-TIL25",1.581439,1.577216,1.591486)); // 40.7 581407 0.0019
        glasses.put("S-TIL26", new GlassMap("Ohara","S-TIL26",1.567322,1.563386,1.576636)); // 42.8 567428 0.0009
        glasses.put("S-TIL27", new GlassMap("Ohara","S-TIL27",1.575006,1.570902,1.584756)); // 41.5 575415 0.0024
        glasses.put("S-TIM2", new GlassMap("Ohara","S-TIM2",1.620041,1.615024,1.632123)); // 36.3 620363 0.0051
        glasses.put("S-TIM5", new GlassMap("Ohara","S-TIM5",1.60342,1.598748,1.614616)); // 38 603380 0.0036
        glasses.put("S-TIM8", new GlassMap("Ohara","S-TIM8",1.595509,1.59103,1.606206)); // 39.2 596392 0.0023
        glasses.put("S-TIM22",new GlassMap("Ohara", "S-TIM22",1.64769,1.6421,1.66126));
        glasses.put("S-TIM25", new GlassMap("Ohara","S-TIM25",1.6727,1.666607,1.687564)); // 32.1 673321 0.0093
        glasses.put("S-TIM27", new GlassMap("Ohara","S-TIM27",1.639799,1.634375,1.652939)); // 34.5 640345 0.0065
        glasses.put("S-TIM28", new GlassMap("Ohara","S-TIM28",1.688931,1.682495,1.704665)); // 31.1 689311 0.0092
        glasses.put("S-TIM35", new GlassMap("Ohara","S-TIM35",1.698947,1.692225,1.715424)); // 30.1 699301 0.0103
        glasses.put("S-TIH1", new GlassMap("Ohara","S-TIH1",1.717362,1.710332,1.734635)); // 29.5 717295 0.011
        glasses.put("S-TIH3", new GlassMap("Ohara","S-TIH3",1.739998,1.732453,1.758605)); // 28.3 740283 0.0122
        glasses.put("S-TIH4", new GlassMap("Ohara","S-TIH4",1.755199,1.747295,1.774745)); // 27.5 755275 0.0133
        glasses.put("S-TIH6", new GlassMap("Ohara","S-TIH6",1.805181,1.796106,1.827775)); // 25.4 805254 0.0158
        glasses.put("S-TIH10", new GlassMap("Ohara","S-TIH10",1.72825,1.720865,1.746453)); // 28.5 728285 0.0123
        glasses.put("S-TIH11", new GlassMap("Ohara","S-TIH11",1.784723,1.775965,1.806519)); // 25.7 785257 0.0162
        glasses.put("S-TIH13", new GlassMap("Ohara","S-TIH13",1.740769,1.733089,1.759746)); // 27.8 741278 0.013
        glasses.put("S-TIH14", new GlassMap("Ohara","S-TIH14",1.761821,1.753567,1.782296)); // 26.5 762265 0.015
        glasses.put("S-TIH18", new GlassMap("Ohara","S-TIH18",1.721507,1.714371,1.739054)); // 29.2 722292 0.0111
        glasses.put("S-TIH23",new GlassMap("Ohara", "S-TIH23",1.7847,1.77613,1.80597));
        glasses.put("S-TIH53", new GlassMap("Ohara","S-TIH53",1.84666,1.836488,1.872096)); // 23.8 847238 0.0175
        glasses.put("S-TIH53W", new GlassMap("Ohara","S-TIH53W",1.84666,1.836488,1.872096)); // 23.8 847238 0.0175
        glasses.put("S-TIH53WN", new GlassMap("Ohara","S-TIH53WN",1.84666,1.836527,1.872007)); // 23.9 847239 0.0179
        glasses.put("S-TIH57", new GlassMap("Ohara","S-TIH57",1.963,1.951598,1.991533)); // 24.1 963241 0.0187
        glasses.put("S-LAL7", new GlassMap("Ohara", "S-LAL7", 1.6516, 1.64821, 1.65934));   // 58.55
        glasses.put("S-LAL7Q", new GlassMap("Ohara","S-LAL7Q",1.6516,1.648192,1.659322)); // 58.5 652585 -0.0078
        glasses.put("S-LAL8", new GlassMap("Ohara","S-LAL8",1.712995,1.708974,1.72221)); // 53.9 713539 -0.0084
        glasses.put("S-LAL9", new GlassMap("Ohara","S-LAL9",1.691002,1.687169,1.699774)); // 54.8 691548 -0.0079
        glasses.put("S-LAL10", new GlassMap("Ohara","S-LAL10",1.719995,1.71567,1.730004)); // 50.2 720502 -0.0081
        glasses.put("S-LAL12", new GlassMap("Ohara","S-LAL12",1.6779,1.674188,1.686438)); // 55.3 678553 -0.0047
        glasses.put("S-LAL12Q", new GlassMap("Ohara","S-LAL12Q",1.6779,1.674171,1.686419)); // 55.3 678553 -0.0085
        glasses.put("S-LAL13",new GlassMap("Ohara", "S-LAL13",1.6935,1.68955,1.70258));
        glasses.put("S-LAL14", new GlassMap("Ohara","S-LAL14",1.696797,1.692974,1.705522)); // 55.5 697555 -0.0082
        glasses.put("S-LAL18", new GlassMap("Ohara","S-LAL18",1.729157,1.725101,1.738436)); // 54.7 729547 -0.0086
        glasses.put("S-LAL18N", new GlassMap("Ohara","S-LAL18N",1.729157,1.725093,1.738449)); // 54.6 729546 -0.0089
        glasses.put("S-LAL19", new GlassMap("Ohara","S-LAL19",1.72916,1.725061,1.738541)); // 54.1 729541 -0.0092
        glasses.put("S-LAL20", new GlassMap("Ohara","S-LAL20",1.6993,1.695196,1.708878)); // 51.1 699511 -0.0036
        glasses.put("S-LAL21", new GlassMap("Ohara","S-LAL21",1.703,1.698952,1.712374)); // 52.4 703524 -0.0061
        glasses.put("S-LAL54",new GlassMap("Ohara", "S-LAL54",1.651,1.64749,1.65908));
        glasses.put("S-LAL54Q", new GlassMap("Ohara","S-LAL54Q",1.651,1.64747,1.659046)); // 56.2 651562 -0.0085
        glasses.put("S-LAL58", new GlassMap("Ohara","S-LAL58",1.693495,1.689393,1.703042)); // 50.8 694508 -0.0047
        glasses.put("S-LAL59", new GlassMap("Ohara","S-LAL59",1.733997,1.729679,1.74394)); // 51.5 734515 -0.0096
        glasses.put("S-LAL61", new GlassMap("Ohara","S-LAL61",1.740999,1.736727,1.750805)); // 52.7 741527 -0.0096
        glasses.put("S-LAL61Q", new GlassMap("Ohara","S-LAL61Q",1.741,1.736733,1.75082)); // 52.6 741526 -0.0085
        glasses.put("S-LAM2", new GlassMap("Ohara","S-LAM2",1.743997,1.739048,1.755661)); // 44.8 744448 -0.0035
        glasses.put("S-LAM3", new GlassMap("Ohara","S-LAM3",1.717004,1.712528,1.727489)); // 47.9 717479 -0.0034
        glasses.put("S-LAM7", new GlassMap("Ohara","S-LAM7",1.749497,1.743275,1.764518)); // 35.3 750353 0.0025
        glasses.put("S-LAM54",new GlassMap("Ohara", "S-LAM54",1.757,1.75223,1.76806));
        glasses.put("S-LAM55", new GlassMap("Ohara","S-LAM55",1.762001,1.756385,1.775388)); // 40.1 762401 -0.0001
        glasses.put("S-LAM60", new GlassMap("Ohara","S-LAM60",1.743198,1.738653,1.753716)); // 49.3 743493 -0.0085
        glasses.put("S-LAM61",new GlassMap("Ohara", "S-LAM61",1.72,1.71533,1.73097));
        glasses.put("S-LAM66", new GlassMap("Ohara","S-LAM66",1.800999,1.794275,1.817182)); // 35 801350 0.0015
        glasses.put("S-LAM73", new GlassMap("Ohara","S-LAM73",1.7936,1.787319,1.808716)); // 37.1 794371 0.0013
        glasses.put("S-LAH51", new GlassMap("Ohara","S-LAH51",1.785896,1.780584,1.798364)); // 44.2 786442 -0.0069
        glasses.put("S-LAH52", new GlassMap("Ohara","S-LAH52",1.799516,1.793879,1.812814)); // 42.2 800422 -0.006
        glasses.put("S-LAH52Q", new GlassMap("Ohara","S-LAH52Q",1.79952,1.793893,1.812821)); // 42.2 800422 -0.0056
        glasses.put("S-LAH53", new GlassMap("Ohara","S-LAH53",1.806098,1.800248,1.819945)); // 40.9 806409 -0.0052
        glasses.put("S-LAH53V", new GlassMap("Ohara","S-LAH53V",1.8061,1.800259,1.819954)); // 40.9 806409 -0.0039
        glasses.put("S-LAH55V", new GlassMap("Ohara","S-LAH55V",1.834807,1.828981,1.84852)); // 42.7 835427 -0.0075
        glasses.put("S-LAH55VS", new GlassMap("Ohara","S-LAH55VS",1.83481,1.828988,1.848519)); // 42.7 835427 -0.0075
        glasses.put("S-LAH58", new GlassMap("Ohara","S-LAH58",1.882997,1.87656,1.898221)); // 40.8 883408 -0.0088
        glasses.put("S-LAH59", new GlassMap("Ohara","S-LAH59",1.816,1.810749,1.828252)); // 46.6 816466 -0.0092
        glasses.put("S-LAH60", new GlassMap("Ohara","S-LAH60",1.834,1.827376,1.849819)); // 37.2 834372 -0.0037
        glasses.put("S-LAH60MQ", new GlassMap("Ohara","S-LAH60MQ",1.834,1.827392,1.849829)); // 37.2 834372 -0.0027
        glasses.put("S-LAH60V", new GlassMap("Ohara","S-LAH60V",1.834,1.827399,1.849815)); // 37.2 834372 -0.0006
        glasses.put("S-LAH63",new GlassMap("Ohara", "S-LAH63",1.8044,1.79838,1.8187));
        glasses.put("S-LAH63Q", new GlassMap("Ohara","S-LAH63Q",1.8044,1.798397,1.81872)); // 39.6 804396 -0.0012
        glasses.put("S-LAH64", new GlassMap("Ohara","S-LAH64",1.788001,1.782998,1.799634)); // 47.4 788474 -0.0089
        glasses.put("S-LAH65V", new GlassMap("Ohara","S-LAH65V",1.804,1.798817,1.816076)); // 46.6 804466 -0.0088
        glasses.put("S-LAH65VS", new GlassMap("Ohara","S-LAH65VS",1.804,1.798816,1.816097)); // 46.5 804465 -0.0085
        glasses.put("S-LAH66", new GlassMap("Ohara","S-LAH66",1.772499,1.767798,1.783374)); // 49.6 773496 -0.0092
        glasses.put("S-LAH66N", new GlassMap("Ohara","S-LAH66N",1.772499,1.767792,1.783383)); // 49.6 773496 -0.0094
        glasses.put("S-LAH71", new GlassMap("Ohara","S-LAH71",1.850259,1.842586,1.868935)); // 32.3 850323 0.0036
        glasses.put("S-LAH75", new GlassMap("Ohara", "S-LAH75", 1.874, 1.8667, 1.8915));    // 35.26
        glasses.put("S-LAH79", new GlassMap("Ohara","S-LAH79",2.0033,1.993011,2.028497)); // 28.3 3283 0.0023
        glasses.put("S-LAH88", new GlassMap("Ohara","S-LAH88",1.9165,1.908035,1.937034)); // 31.6 917316 0.0008
        glasses.put("S-LAH89", new GlassMap("Ohara","S-LAH89",1.8515,1.845304,1.866184)); // 40.8 852408 -0.006
        glasses.put("S-LAH92", new GlassMap("Ohara","S-LAH92",1.8919,1.884824,1.908843)); // 37.1 892371 -0.0034
        glasses.put("S-LAH93", new GlassMap("Ohara","S-LAH93",1.90525,1.897677,1.923515)); // 35 905350 0
        glasses.put("S-LAH95", new GlassMap("Ohara","S-LAH95",1.90366,1.895277,1.924109)); // 31.3 904313 0.0055
        glasses.put("S-LAH96", new GlassMap("Ohara","S-LAH96",1.76385,1.759129,1.774882)); // 48.5 764485 -0.0041
        glasses.put("S-LAH97", new GlassMap("Ohara","S-LAH97",1.755,1.750627,1.765058)); // 52.3 755523 -0.0094
        glasses.put("S-LAH98", new GlassMap("Ohara","S-LAH98",1.95375,1.945141,1.974647)); // 32.3 954323 0.0013
        glasses.put("S-LAH99", new GlassMap("Ohara","S-LAH99",2.001,1.991048,2.0254)); // 29.1 1291 0.0054
        glasses.put("S-LAH99W", new GlassMap("Ohara","S-LAH99W",2.001,1.991048,2.0254)); // 29.1 1291 0.0054
        glasses.put("S-FTM16", new GlassMap("Ohara","S-FTM16",1.592701,1.587795,1.60458)); // 35.3 593353 0.009
        glasses.put("S-NBM51", new GlassMap("Ohara","S-NBM51",1.613397,1.609248,1.623105)); // 44.3 613443 -0.0065
        glasses.put("S-NBM52", new GlassMap("Ohara","S-NBM52",1.62205,1.617539,1.632682)); // 41.1 622411 -0.006
        glasses.put("S-NBH5", new GlassMap("Ohara","S-NBH5",1.654115,1.649225,1.665709)); // 39.7 654397 -0.0036
        glasses.put("S-NBH8", new GlassMap("Ohara","S-NBH8",1.720467,1.714365,1.735123)); // 34.7 720347 -0.0019
        glasses.put("S-NBH51", new GlassMap("Ohara","S-NBH51",1.749505,1.743259,1.764473)); // 35.3 750353 -0.0025
        glasses.put("S-NBH52V", new GlassMap("Ohara","S-NBH52V",1.673,1.667792,1.685384)); // 38.3 673383 -0.0039
        glasses.put("S-NBH53V", new GlassMap("Ohara","S-NBH53V",1.738,1.731322,1.754152)); // 32.3 738323 0.0008
        glasses.put("S-NBH55", new GlassMap("Ohara","S-NBH55",1.8,1.792237,1.819043)); // 29.9 800299 0.0085
        glasses.put("S-NBH56", new GlassMap("Ohara","S-NBH56",1.85478,1.844876,1.879345)); // 24.8 855248 0.0109
        glasses.put("S-NBH57", new GlassMap("Ohara","S-NBH57",1.85025,1.842037,1.870336)); // 30 850300 0.0051
        glasses.put("S-NBH58", new GlassMap("Ohara","S-NBH58",1.7888,1.780757,1.808504)); // 28.4 789284 0.0054
        glasses.put("S-NBH59", new GlassMap("Ohara","S-NBH59",1.766342,1.760039,1.781432)); // 35.8 766358 -0.0043
        glasses.put("S-NPH1", new GlassMap("Ohara","S-NPH1",1.808095,1.798009,1.833513)); // 22.8 808228 0.0261
        glasses.put("S-NPH1W", new GlassMap("Ohara","S-NPH1W",1.808095,1.798009,1.833513)); // 22.8 808228 0.0261
        glasses.put("S-NPH2", new GlassMap("Ohara","S-NPH2",1.92286,1.909158,1.957996)); // 18.9 923189 0.0386
        glasses.put("S-NPH3", new GlassMap("Ohara","S-NPH3",1.95906,1.94376,1.998655)); // 17.5 959175 0.0466
        glasses.put("S-NPH4", new GlassMap("Ohara","S-NPH4",1.89286,1.880484,1.924335)); // 20.4 893204 0.0308
        glasses.put("S-NPH5", new GlassMap("Ohara","S-NPH5",1.858956,1.848209,1.886001)); // 22.7 859227 0.0237
        glasses.put("S-NPH7", new GlassMap("Ohara","S-NPH7",1.7783,1.769024,1.801573)); // 23.9 778239 0.022

        glasses.put("L-LAH91", new GlassMap("Ohara", "L-LAH91", 1.76450, 1.75981, 1.77538));
        glasses.put("L-LAH84", new GlassMap("Ohara", "L-LAH84", 1.80835, 1.80243, 1.82237));
        glasses.put("L-BAL42", new GlassMap("Ohara", "L-BAL42", 1.583126, 1.58013, 1.58995));    // 59.38

    }
}
