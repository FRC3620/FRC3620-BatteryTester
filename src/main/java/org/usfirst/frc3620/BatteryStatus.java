package org.usfirst.frc3620;

import java.util.StringJoiner;

public class BatteryStatus {
    private final double voltage;
    private final double amperage;
    private final long time;

    BatteryStatus (long t, double v, double a) {
        time = t;
        voltage = v;
        amperage = a;
    }

    public long getTime() {
        return time;
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
                .add("time=" + time)
                .add("voltage=" + voltage)
                .add("amperage=" + amperage)
                .toString();
    }
}
