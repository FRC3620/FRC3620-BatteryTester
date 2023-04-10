package org.usfirst.frc3620;

import java.util.Collection;

public interface IBattery {
    public BatteryInfo getBatteryInfo();

    public BatteryStatus getBatteryStatus();

    public void setLoad (double amperage);

    public Collection<Double> getPossibleLoads();
}
