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
    automationHat = new AutomationHat(1, Ads1x15.PgaConfig._6144MV, Ads1x15.Ads1015DataRate._128HZ);
    automationHat.ads.setSingleMode(0);
    automationHat.ads.setSingleMode(1);
  }

  @Override
  public BatteryInfo getBatteryInfo() {
    return new BatteryInfo();
  }

  @Override
  public BatteryReading getBatteryReading() {
    double v = automationHat.getAdsValue(1);
    double a = automationHat.getAdsValue(2);
    BatteryReading rv = new BatteryReading(v, a);
    logger.debug("got reading: {}", rv);
    return rv;
  }

  @Override
  public void setLoad(double amperage) {
    if (load != amperage) {
      logger.info("setting load to {}", amperage);
      if (amperage > 0) {
        automationHat.setRelay(1, true);
      } else {
        automationHat.setRelay(1, false);
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
