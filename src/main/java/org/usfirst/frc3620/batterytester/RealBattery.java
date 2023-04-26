package org.usfirst.frc3620.batterytester;

import com.pi4j.Pi4J;
import com.pi4j.catalog.components.Ads1115;
import com.pi4j.catalog.components.helpers.PIN;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;

import java.util.Collection;

public class RealBattery implements IBattery {
  Context pi4j;
  Ads1115 adc;
  DigitalOutput load1, load2, load3;

  public RealBattery() {
    pi4j = Pi4J.newAutoContext();
    adc = new Ads1115(pi4j, 1, Ads1115.GAIN.GAIN_6_144V, Ads1115.ADDRESS.GND, 4);
    load1 = pi4j.create(buildDigitalOutputConfig(pi4j, PIN.PWM13));
    load2 = pi4j.create(buildDigitalOutputConfig(pi4j, PIN.PWM19));
    load3 = pi4j.create(buildDigitalOutputConfig(pi4j, PIN.D16));
  }

  @Override
  public BatteryInfo getBatteryInfo() {
    return null;
  }

  @Override
  public BatteryReadings getBatteryStatus() {
    double v = adc.singleShotAIn0();
    double a = adc.singleShotAIn1();
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
    adc.deregisterAll();
    adc = null;
    pi4j.shutdown();
    pi4j = null;
  }

  protected DigitalOutputConfig buildDigitalOutputConfig(Context pi4j, com.pi4j.catalog.components.helpers.PIN address) {
    return DigitalOutput.newConfigBuilder(pi4j)
      .id("BCM" + address)
      .name("LED")
      .address(address.getPin())
      .initial(DigitalState.LOW)
      .shutdown(DigitalState.LOW)
      .build();
  }
}
