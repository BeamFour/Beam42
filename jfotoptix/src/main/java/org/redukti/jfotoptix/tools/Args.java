package org.redukti.jfotoptix.tools;

public final class Args {
    int scenario = 0;
    String specfile = null;
    String outputType = "layout";
    String outputFile = null;
    String outdir = null;
    boolean skewRays = false;
    boolean dumpSystem = false;
    boolean use_glass_types = true;
    int trace_density = 20;
    int spot_density = 50;
    boolean include_lost_rays = true;
    boolean only_d_line = false;

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
        }
        return arguments;
    }


}
