package org.usfirst.frc3620.batterytester;

import org.junit.jupiter.api.Test;

public class FakeBatteryTest {

    @Test
    public void test01() {
        BatteryInfo bi = new BatteryInfo();
        bi.nominalCapacity = 18.2;
        FakeBattery b = new FakeBattery(bi);
        b.setLoad(10);
        long t = 0;
        double ampHours = 0;
        while (true) {
            BatteryReadings batteryStatus = b.getBatteryStatus();
            double v = batteryStatus.getVoltage();
            double a = batteryStatus.getAmperage();
            ampHours += a / 3600.0;
            System.out.println (t + ": voltage = " + v + ", capacity = " + b.currentCapacity);
            if (v < 11.7) break;

            t += 1000;
            b.update(t);
        }
        System.out.println ("total amp hours = " + ampHours);
    }
}
