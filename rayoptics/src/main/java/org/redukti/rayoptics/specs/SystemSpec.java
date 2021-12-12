package org.redukti.rayoptics.specs;

public class SystemSpec {

    public String title;
    public String initials;
    public String dimensions;
    public double temperature;
    public double pressure;

    public SystemSpec() {
        title = "";
        initials = "";
        dimensions = "mm";
        temperature = 20.0;
        pressure = 760.0;
    }


}