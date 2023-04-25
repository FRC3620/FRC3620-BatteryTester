package org.usfirst.frc3620.batterytester;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;

import java.util.Collection;

public class RealBattery implements IBattery {
  Context pi4j;
  Ads

  public RealBattery() {
    pi4j = Pi4J.newAutoContext();
  }

  @Override
  public BatteryInfo getBatteryInfo() {
    return null;
  }

  @Override
  public BatteryReadings getBatteryStatus() {
    return null;
  }

  @Override
  public void setLoad(double amperage) {

  }

  @Override
  public double getLoad() {
    return 0;
  }

  @Override
  public Collection<Double> getPossibleLoads() {
    return null;
  }

  @Override
  public void close() {
    pi4j.shutdown();
    pi4j = null;
  }
}
