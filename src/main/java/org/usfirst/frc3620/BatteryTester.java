package org.usfirst.frc3620;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BatteryTester implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    IBattery battery;
    FakeBattery fakeBattery;

    Object waitLock = new Object();

    List<Consumer<BatteryTestStatus>> statusConsumers = new ArrayList<>();

    public BatteryTester (IBattery battery)  {
        this.battery = battery;
        if (battery instanceof FakeBattery) {
            fakeBattery = (FakeBattery) battery;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::loadOff));
    }

    @Override
    public void run() {
        while (true) {
            synchronized (waitLock) {
                try {
                    waitLock.wait(1000);
                } catch (InterruptedException ignored) {

                }

            }
            if (fakeBattery != null) fakeBattery.update();

            long now = System.currentTimeMillis();
            long tDelta = now;
            if (t0 != null) {
                tDelta = now - t0;
            }
            BatteryTestStatus batteryStatus = new BatteryTestStatus(tDelta / 1000.0, battery.getBatteryStatus());
            for (var c : statusConsumers) {
                c.accept(batteryStatus);
            }
        }
    }

    Long t0 = null;
    public void startTest (double amperage) {
        t0 = System.currentTimeMillis();
        battery.setLoad(amperage);
        bumpThread();
    }

    void bumpThread() {
        synchronized (waitLock) {
            waitLock.notify();
        }
    }

    public void addStatusConsumer (Consumer<BatteryTestStatus> cs) {
        statusConsumers.add(cs);
    }

    void loadOff() {
        logger.info ("shutdown hook activated, shutting off load");
        battery.setLoad(0);
    }
}
