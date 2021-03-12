package org.redukti.jfotoptix.io;

public class Rgb {
    public final double r;
    public final double g;
    public final double b;
    public final double a;

    public Rgb (double red, double green, double blue, double alpha) {
        this.r = red;
        this.g = green;
        this.b = blue;
        this.a = alpha;
    }

    public static final Rgb rgb_black = new Rgb (0.0f, 0.0f, 0.0f, 1.0f);
    public static final  Rgb rgb_red = new Rgb (1.0f, 0.0f, 0.0f, 1.0f);
    public static final  Rgb rgb_green = new Rgb(0.0f, 1.0f, 0.0f, 1.0f);
    public static final  Rgb rgb_blue = new Rgb(0.0f, 0.0f, 1.0f, 1.0f);
    public static final  Rgb rgb_yellow = new Rgb(1.0f, 1.0f, 0.0f, 1.0f);
    public static final  Rgb rgb_cyan = new Rgb(0.0f, 1.0f, 1.0f, 1.0f);
    public static final  Rgb rgb_magenta = new Rgb(1.0f, 0.0f, 1.0f, 1.0f);
    public static final  Rgb rgb_gray = new Rgb(0.5f, 0.5f, 0.5f, 1.0f);
    public static final  Rgb rgb_white = new Rgb(1.0f, 1.0f, 1.0f, 1.0f);
}
