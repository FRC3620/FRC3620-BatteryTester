package org.usfirst.frc3620;

import java.util.StringJoiner;

public class BatteryStatus {
    private final double voltage;
    private final double amperage;
    BatteryStatus (double v, double a) {
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
        return new StringJoiner(", ", BatteryStatus.class.getSimpleName() + "[", "]")
                .add("voltage=" + voltage)
                .add("amperage=" + amperage)
                .toString();
    }
}
