package org.usfirst.frc3620;

import java.util.StringJoiner;

public class BatteryTestStatus extends BatteryStatus {
  private final double time;

  BatteryTestStatus (double t, double v, double a) {
    super(v, a);
    time = t;
  }

  BatteryTestStatus (double t, BatteryStatus batteryStatus) {
    super(batteryStatus.getVoltage(), batteryStatus.getAmperage());
    time = t;
  }

  public double getTime() {
    return time;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", BatteryStatus.class.getSimpleName() + "[", "]")
      .add("time=" + time)
      .add("voltage=" + getVoltage())
      .add("amperage=" + getAmperage())
      .toString();
  }





}
