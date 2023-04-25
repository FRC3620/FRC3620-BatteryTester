package org.usfirst.frc3620.batterytester;

import java.util.Collection;

public interface IBattery {
    public BatteryInfo getBatteryInfo();

    public BatteryReadings getBatteryStatus();

    public void setLoad (double amperage);

    public double getLoad ();

    public Collection<Double> getPossibleLoads();

    default public void close() { }
}
