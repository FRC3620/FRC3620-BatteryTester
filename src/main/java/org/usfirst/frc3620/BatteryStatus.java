package org.usfirst.frc3620;

public class BatteryStatus {
    private double voltage, amperage;

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
}
