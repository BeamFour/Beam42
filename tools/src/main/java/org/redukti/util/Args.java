package org.redukti.util;

public final class Args {
    public int scenario = 0;
    public String specfile = null;
    public String outputType = "layout";
    public String outputFile = null;
    public String outdir = null;
    public boolean skewRays = false;
    public boolean dumpSystem = false;
    public boolean use_glass_types = true;
    public int trace_density = 20;
    public int spot_density = 50;
    public boolean include_lost_rays = false;
    public boolean only_d_line = false;
    public boolean do_ray_aberrations = false;
    public boolean do_mono_chrome_mtfs = false;
    public boolean use_grid_pattern = false;
    public boolean auto_size_spots = false;
    public boolean do_wideangle_layout = false;

    public static Args parseArguments(String[] args) {
        Args arguments = new Args();
        for (int i = 0; i < args.length; i++) {
            String arg1 = args[i];
            String arg2 = i+1 < args.length ? args[i+1] : null;
            if (arg1.equals("--specfile")) {
                arguments.specfile = arg2;
                i++;
            }
            else if (arg1.equals("-o")) {
                arguments.outputFile = arg2;
                i++;
            }
            else if (arg1.equals("--scenario")) {
                arguments.scenario = Integer.parseInt(arg2);
                i++;
            }
            else if (arg1.equals("--output") || arg1.equals("--type")) {
                arguments.outputType = arg2;
                i++;
            }
            else if (arg1.equals("--outdir")) {
                arguments.outdir = arg2;
                i++;
            }
            else if (arg1.equals("--skew")) {
                arguments.skewRays = true;
            }
            else if (arg1.equals("--dont-use-glass-types")) {
                arguments.use_glass_types = false;
            }
            else if (arg1.equals("--dump-system")) {
                arguments.dumpSystem = true;
            }
            else if (arg1.equals("--exclude-lost-rays")) {
                arguments.include_lost_rays = false;
            }
            else if (arg1.equals("--trace-density")) {
                arguments.trace_density = Integer.parseInt(arg2);
                i++;
            }
            else if (arg1.equals("--spot-density")) {
                arguments.spot_density = Integer.parseInt(arg2);
                i++;
            }
            else if (arg1.equals("--only-d-line")) {
                arguments.only_d_line = true;
            }
            else if (arg1.equals("--output-ray-aberration-plots")) {
                arguments.do_ray_aberrations = true;
            }
            else if (arg1.equals("--output-wavelength-mtfs")) {
                arguments.do_mono_chrome_mtfs = true;
            }
            else if (arg1.equals("--use-grid-pattern-for-spot")) {
                arguments.use_grid_pattern = true;
            }
            else if (arg1.equals("--auto-size-spot-diagrams")) {
                arguments.auto_size_spots = true;
            }
            else if (arg1.equals("--do-wideangle-layout")) {
                arguments.do_wideangle_layout = true;
            }
        }
        return arguments;
    }


}
