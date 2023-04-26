package org.usfirst.frc3620.batterytester;

import java.util.StringJoiner;

public class BatteryReading {
    private final double voltage;
    private final double amperage;
    private final String loadDescription;
    BatteryReading(double v, double a, String ld) {
        voltage = v;
        amperage = a;
        loadDescription = ld;
    }

    public double getVoltage() {
        return voltage;
    }

    public double getAmperage() {
        return amperage;
    }

    public String getLoadDescription() {
        return loadDescription;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BatteryReading.class.getSimpleName() + "[", "]")
                .add("voltage=" + voltage)
                .add("amperage=" + amperage)
                .add("load=" + loadDescription)
                .toString();
    }
}
