package org.usfirst.frc3620;

import java.util.StringJoiner;

public class BatteryReadings {
    private final double voltage;
    private final double amperage;
    BatteryReadings(double v, double a) {
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
        return new StringJoiner(", ", BatteryReadings.class.getSimpleName() + "[", "]")
                .add("voltage=" + voltage)
                .add("amperage=" + amperage)
                .toString();
    }
}
