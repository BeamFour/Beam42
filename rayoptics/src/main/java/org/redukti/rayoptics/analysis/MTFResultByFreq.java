package org.redukti.rayoptics.analysis;

import java.util.List;

public class MTFResultByFreq {
    public int freq;
    // data for above freq across fields
    public double[] sag_mtf_by_field;
    public double[] tan_mtf_by_field;

    public MTFResultByFreq(List<PolyMTF> mtfs_by_field, int freq) {
        this.freq = freq;
        sag_mtf_by_field = new double[mtfs_by_field.size()];
        tan_mtf_by_field = new double[mtfs_by_field.size()];
        for (var fi = 0; fi < mtfs_by_field.size(); fi++) {
            var mtf = mtfs_by_field.get(fi);
            int i = -1;
            // TODO interpolate
            for (int j = 0; j < mtf.freq.length; j++) {
                if ((int) mtf.freq[j] == freq) {
                    i = j;
                    break;
                }
            }
            if (i < 0) throw new IllegalArgumentException("No data for freq " + freq);
            sag_mtf_by_field[fi] = mtf.mag_x[i];
            tan_mtf_by_field[fi] = mtf.mag_y[i];
        }
    }
}
