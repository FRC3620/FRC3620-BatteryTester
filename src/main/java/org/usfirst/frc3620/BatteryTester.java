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

    List<Consumer<BatteryStatus>> statusConsumers = new ArrayList<>();

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

            BatteryStatus batteryStatus = battery.getBatteryStatus();
            for (var c : statusConsumers) {
                c.accept(batteryStatus);
            }
        }
    }

    public void startTest (double amperage) {
        battery.setLoad(amperage);
        bumpThread();
    }

    void bumpThread() {
        synchronized (waitLock) {
            waitLock.notify();
        }
    }

    public void addStatusConsumer (Consumer<BatteryStatus> cs) {
        statusConsumers.add(cs);
    }

    void loadOff() {
        logger.info ("shutdown hook activated, shutting off load");
        battery.setLoad(0);
    }
}
