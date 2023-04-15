package org.usfirst.frc3620.batterytester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.BlockingQueue;

public class BatteryTester implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    enum Status {
        OFF, RUNNING
    }

    enum InternalStatus {
        DETERMINING_RINT_1, LOADED, DETERMINING_RINT_2
    }

    Status status = Status.OFF;

    InternalStatus internalStatus = InternalStatus.DETERMINING_RINT_1;

    IBattery battery;
    FakeBattery fakeBattery;

    double loadAmperage = 0;

    final Object testThreadWaitLock = new Object();

    final List<BlockingQueue<WSMessage>> statusQueues = new ArrayList<>();

    final List<BatteryTestReading> testSamples = new ArrayList<>();

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
            if (fakeBattery != null) fakeBattery.update();

            if (status == Status.RUNNING) {
                long now = System.currentTimeMillis();
                long tDelta = now;
                if (t0 != null) {
                    tDelta = now - t0;
                }

                BatteryTestReading batteryTestReading = new BatteryTestReading(tDelta / 1000.0, battery.getBatteryStatus());

                synchronized (testSamples) {
                    testSamples.add(batteryTestReading);
                    sendToAll(new WSMessage.BatteryTestReading(batteryTestReading, true));
                }

                if (internalStatus == InternalStatus.DETERMINING_RINT_1) {
                    internalStatus = InternalStatus.LOADED;
                    battery.setLoad(loadAmperage);
                }

                if (internalStatus == InternalStatus.DETERMINING_RINT_2) {
                    status = Status.OFF;
                    sendStatus();
                }

                if (batteryTestReading.getVoltage() < 10.7) {
                    internalStatus = InternalStatus.DETERMINING_RINT_2;
                    battery.setLoad(0);
                }

            }

            sendToAll(new WSMessage.TickTock()); // help determine dropped connections

            synchronized (testThreadWaitLock) {
                try {
                    testThreadWaitLock.wait(1000);
                } catch (InterruptedException ignored) {

                }
            }
        }
    }

    void sendStatus() {
        sendToAll(new WSMessage.BatteryTestStatus(status));
    }

    void sendToAll (WSMessage w) {
        synchronized (statusQueues) {
            for (var q : statusQueues) {
                try {
                    q.put(w);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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
            testSamples.clear();
        }
        internalStatus = InternalStatus.DETERMINING_RINT_1;
        status = Status.RUNNING;
        sendStatus();
        sendToAll(WSMessage.START_BATTERY_TEST);
        bumpThread();
    }

    public void stopTest () {
        status = Status.OFF;
        battery.setLoad(0.0);
        sendStatus();
        bumpThread();
    }

    void bumpThread() {
        synchronized (testThreadWaitLock) {
            testThreadWaitLock.notify();
        }
    }

    public void addStatusConsumer (BlockingQueue<WSMessage> q, boolean catchup) {
        synchronized (statusQueues) {
            if (catchup) {
                try {
                    q.put(new WSMessage.BatteryTestStatus(status));
                    q.put(WSMessage.START_BATTERY_TEST);

                    synchronized (testSamples) {
                        Iterator<BatteryTestReading> i = testSamples.iterator();
                        while (i.hasNext()) {
                            BatteryTestReading r = i.next();
                            boolean shouldUpdate = ! i.hasNext(); // don't update if more coming
                            WSMessage catchupMessage = new WSMessage.BatteryTestReading(r, shouldUpdate);
                            q.put(catchupMessage);
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            statusQueues.add(q);
        }
    }

    public void removeStatusConsumer (BlockingQueue<WSMessage> q) {
        synchronized (statusQueues) {
            statusQueues.remove(q);
        }
    }

    void loadOff() {
        logger.info ("shutdown hook activated, shutting off load");
        battery.setLoad(0);
    }

    public static class BatteryTestReading {
        private final double time, voltage, amperage;

        public BatteryTestReading(double t, double v, double a) {
            this.time = t;
            this.voltage = v;
            this.amperage = a;
        }

        public BatteryTestReading(double t, BatteryReadings batteryStatus) {
            this(t, batteryStatus.getVoltage(), batteryStatus.getAmperage());
        }

        /**
         * @return returns epoch millisecond time of battery reading
         */
        public double getTime() {
            return time;
        }

        public double getAmperage() {
            return amperage;
        }

        public double getVoltage() {
            return voltage;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", BatteryReadings.class.getSimpleName() + "[", "]")
                    .add("time=" + time)
                    .add("voltage=" + getVoltage())
                    .add("amperage=" + getAmperage())
                    .toString();
        }
    }

}
