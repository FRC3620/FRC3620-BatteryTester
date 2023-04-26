package org.usfirst.frc3620.batterytester;

import java.util.StringJoiner;

public class BatteryReading {
    private final double voltage;
    private final double amperage;
    BatteryReading(double v, double a) {
        voltage = v;
        amperage = a;
    }

    public double getVoltage() {
        return voltage;
    }

    public double getAmperage() {
        return amperage;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BatteryReading.class.getSimpleName() + "[", "]")
                .add("voltage=" + voltage)
                .add("amperage=" + amperage)
                .toString();
    }
}
