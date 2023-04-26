package org.usfirst.frc3620.batterytester;

import com.diozero.devices.Ads1x15;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class RealBattery implements IBattery {
  Logger logger = LoggerFactory.getLogger(getClass());

  Ads1x15 ads;

  public RealBattery() {
    ads = new Ads1x15(1, Ads1x15.Address.GND, Ads1x15.PgaConfig._6144MV, Ads1x15.Ads1115DataRate._32HZ);
    ads.setSingleMode(0);
    ads.setSingleMode(1);
  }

  @Override
  public BatteryInfo getBatteryInfo() {
    return new BatteryInfo();
  }

  @Override
  public BatteryReadings getBatteryStatus() {
    double v = ads.getValue(0);
    double a = ads.getValue(1);
    return new BatteryReadings(v, a);
  }

  @Override
  public void setLoad(double amperage) {
    logger.info ("setting load to {}", amperage);
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
    ads.close();
  }
}
