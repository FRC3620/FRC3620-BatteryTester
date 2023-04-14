package org.usfirst.frc3620.batterytester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class BatteryTester implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    enum Status {
        OFF, RUNNING, PAUSED, DETERMINING_RINT
    }

    Status status = Status.OFF;

    IBattery battery;
    FakeBattery fakeBattery;

    double loadAmperage = 0;

    final Object waitLock = new Object();

    List<BlockingQueue<WSMessage>> statusQueues = new ArrayList<>();

    List<WSMessage.BatteryTestReading> testSamples = new ArrayList<>();

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

            if (status == Status.RUNNING) {
                long now = System.currentTimeMillis();
                long tDelta = now;
                if (t0 != null) {
                    tDelta = now - t0;
                }
                WSMessage.BatteryTestReading batteryTestStatus = new WSMessage.BatteryTestReading(tDelta / 1000.0, battery.getBatteryStatus());

                synchronized (waitLock) {
                    testSamples.add(batteryTestStatus);
                    sendToAll(batteryTestStatus);
                }

                if (batteryTestStatus.getVoltage() < 10.7) {
                    status = Status.OFF;
                    battery.setLoad(0);
                    sendStatus();
                }
            }
        }
    }

    void sendStatus() {
        sendToAll(new WSMessage.BatteryTestStatus(status));
    }

    void sendToAll (WSMessage w) {
        for (var q : statusQueues) {
            try {
                q.put(w);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    Long t0 = null;

    public void startTest() {
        startTest(null);
    }
    public void startTest (Double amperage) {
        if (amperage != null) {
            loadAmperage = amperage;
        }
        if (status == Status.OFF) {
            t0 = System.currentTimeMillis();
            battery.setLoad(loadAmperage);
            testSamples.clear();
        }
        status = Status.RUNNING;
        sendStatus();
        sendToAll(new WSMessage.BatteryStartTestMessage());
        bumpThread();
    }

    public void stopTest () {
        status = Status.OFF;
        battery.setLoad(0.0);
        sendStatus();
        bumpThread();
    }

    void bumpThread() {
        synchronized (waitLock) {
            waitLock.notify();
        }
    }

    public void addStatusConsumer (BlockingQueue<WSMessage> q, boolean catchup) {
        synchronized (waitLock) {
            if (catchup) {
                try {
                    q.put(new WSMessage.BatteryTestStatus(status));
                    q.put(new WSMessage.BatteryStartTestMessage());

                    for (var ts : testSamples) {
                        q.put(ts);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            statusQueues.add(q);
        }
    }

    public void removeStatusConsumer (BlockingQueue<WSMessage> q) {
        synchronized (waitLock) {
            statusQueues.remove(q);
        }
    }

    void loadOff() {
        logger.info ("shutdown hook activated, shutting off load");
        battery.setLoad(0);
    }
}
