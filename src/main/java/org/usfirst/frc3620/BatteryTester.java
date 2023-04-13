package org.usfirst.frc3620;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

public class BatteryTester implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    IBattery battery;
    FakeBattery fakeBattery;

    final Object waitLock = new Object();

    List<BlockingQueue<BatteryTestStatus>> statusQueues = new ArrayList<>();

    List<BatteryTestStatus> testSamples = new ArrayList<>();

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
            BatteryTestStatus batteryTestStatus = new BatteryTestStatus(tDelta / 1000.0, battery.getBatteryStatus());

            synchronized (waitLock) {
                testSamples.add(batteryTestStatus);
                for (var q : statusQueues) {
                    try {
                        q.put(batteryTestStatus);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
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

    public void addStatusConsumer (BlockingQueue<BatteryTestStatus> q, boolean catchup) {
        synchronized (waitLock) {
            if (catchup) {
                for (var ts : testSamples) {
                    try {
                        q.put(ts);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            statusQueues.add(q);
        }
    }

    public void removeStatusConsumer (Queue<BatteryTestStatus> q) {
        synchronized (waitLock) {
            statusQueues.remove(q);
        }
    }

    void loadOff() {
        logger.info ("shutdown hook activated, shutting off load");
        battery.setLoad(0);
    }
}
