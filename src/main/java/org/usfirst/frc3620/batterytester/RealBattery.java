package org.usfirst.frc3620.batterytester;

import com.diozero.devices.Ads1x15;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class RealBattery implements IBattery {
  Logger logger = LoggerFactory.getLogger(getClass());

  AutomationHat automationHat;

  double load = 0;

  public RealBattery() {
    automationHat = new AutomationHat(1, Ads1x15.PgaConfig._4096MV, Ads1x15.Ads1115DataRate._32HZ);
    logger.info ("ads set: {}", automationHat.ads.getName());
  }

  @Override
  public BatteryInfo getBatteryInfo() {
    return new BatteryInfo();
  }

  @Override
  public BatteryReading getBatteryReading() {
    double v = automationHat.readAnalogInputVoltage(1);
    double a = automationHat.readAnalogInputVoltage(2) * (100.0 / 0.075); // 100amp = 75mV
    BatteryReading rv = new BatteryReading(v, a, "");
    logger.debug("got reading: {}", rv);
    return rv;
  }

  @Override
  public void setLoad(double amperage) {
    if (load != amperage) {
      logger.info("setting load to {}", amperage);
      if (amperage > 0) {
        automationHat.setRelay(3, true);
      } else {
        automationHat.setRelay(3, false);
      }
      load = amperage;
    }
  }

  @Override
  public double getLoad() {
    return load;
  }

  @Override
  public Collection<Double> getPossibleLoads() {
    return null;
  }

  @Override
  public void close() {
    try {
      automationHat.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
