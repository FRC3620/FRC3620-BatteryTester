package org.usfirst.frc3620;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FakeBattery implements IBattery {

    // voltage at 0%, 10%, ..., 100%
    static double[] voltages = { 10.5, 11.31, 11.58, 11.75, 11.90, 12.06, 12.20, 12.32, 12.42, 12.0, 12.70 };
    List<Double> possibleLoads = new ArrayList<>();
    BatteryInfo batteryInfo;

    double currentCapacity = 0; // Amp hours
    double currentLoad = 0;
    double internalResistance = 0;
    Long lastT = null;

    public FakeBattery(BatteryInfo batteryInfo) {
        this.batteryInfo = batteryInfo;

        possibleLoads.add(0.0);
        possibleLoads.add(10.0);

        currentCapacity = batteryInfo.getNominalCapacity();
        currentLoad = 0;
    }

    @Override
    public BatteryInfo getBatteryInfo() {
        return batteryInfo;
    }

    @Override
    public BatteryStatus getBatteryStatus() {
        double v = calculateInternalVoltage();
        double ohms = 12.0 / currentLoad;
        double a = v / (ohms + internalResistance);
        double drop = a * internalResistance;
        return new BatteryStatus(lastT, v - drop, a);
    }

    @Override
    public void setLoad(double amperage) {
        currentLoad = amperage;
    }

    @Override
    public Collection<Double> getPossibleLoads() {
        return possibleLoads;
    }

    public void update() {
        update(System.currentTimeMillis());
    }

    public void update(long t) {
        if (lastT != null && currentLoad != 0) {
            double ohms = 12.0 / currentLoad;
            double tDelta = t - lastT;
            double voltage = calculateInternalVoltage();
            double current = voltage / (ohms + internalResistance);
            double ampHourDelta = current * (tDelta / 3600.0);
            currentCapacity -= ampHourDelta;
        }
        lastT = t;
    }

    double calculateInternalVoltage() {
        double capacityLeft = currentCapacity / batteryInfo.getNominalCapacity();
        if (capacityLeft >= 1.00) {
            return voltages[10];
        }
        int index = (int) (capacityLeft * 10);
        double v1 = voltages[index];
        double v2 = voltages[index + 1];
        double vSpan = v2 - v1;
        double spanSize = (capacityLeft % 0.1) * 10;
        return v1 + (vSpan * spanSize);
    }
}
