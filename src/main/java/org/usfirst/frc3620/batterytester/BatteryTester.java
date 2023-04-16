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

    double vaH = 0.0;
    double aH = 0.0;

    IBattery battery;
    FakeBattery fakeBattery;

    double loadAmperage = 0;

    final Object collectionThreadWaitLock = new Object();

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
        logger.info("starting the collection thread");
        int loop_timer_counter = 0; // set to none-zero to get some benchmarking along the way
        while (true) {
            long now = System.currentTimeMillis();
            long t00 = now;
            if (fakeBattery != null) fakeBattery.update();

            if (status == Status.RUNNING) {
                long tDelta = now;
                if (t0 != null) {
                    tDelta = now - t0;
                }

                if (loop_timer_counter > 0) {
                    logger.debug ("reading battery, t = {}", System.currentTimeMillis() - t00);
                }

                BatteryTestReading batteryTestReading = new BatteryTestReading(tDelta / 1000.0, battery.getBatteryStatus(), 0, 0);
                logger.debug("sample {}, t {}, v {}, a {}", testSamples.size(), tDelta, batteryTestReading.getVoltage(), batteryTestReading.getAmperage());


                if (loop_timer_counter > 0) {
                    logger.debug ("adding to array, t = {}", System.currentTimeMillis() - t00);
                }
                synchronized (testSamples) {
                    testSamples.add(batteryTestReading);
                }

                if (loop_timer_counter > 0) {
                    logger.debug ("sending samples, t = {}", System.currentTimeMillis() - t00);
                }
                sendToAll(new WSMessage.BatteryTestReading(batteryTestReading, true));
                if (loop_timer_counter > 0) {
                    logger.debug ("samples sent, t = {}", System.currentTimeMillis() - t00);
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

            if (loop_timer_counter > 0) loop_timer_counter--;

            synchronized (collectionThreadWaitLock) {
                try {
                    collectionThreadWaitLock.wait(500);
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
            vaH = 0.0;
            aH = 0.0;
        }
        internalStatus = InternalStatus.DETERMINING_RINT_1;
        status = Status.RUNNING;
        sendStatus();
        sendToAll(WSMessage.START_BATTERY_TEST);
        logger.info("bumping the collection thread");
        bumpCollectionThread();
    }

    public void stopTest () {
        status = Status.OFF;
        battery.setLoad(0.0);
        sendStatus();
        bumpCollectionThread();
    }

    void bumpCollectionThread() {
        synchronized (collectionThreadWaitLock) {
            collectionThreadWaitLock.notify();
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
        private final double time, voltage, amperage, vaH, aH;

        public BatteryTestReading(double t, double v, double a, double vaH, double aH) {
            this.time = t;
            this.voltage = v;
            this.amperage = a;
            this.vaH = vaH;
            this.aH = aH;
        }

        public BatteryTestReading(double t, BatteryReadings batteryStatus, double vaH, double aH) {
            this(t, batteryStatus.getVoltage(), batteryStatus.getAmperage(), vaH, aH);
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

        public double getVaH() {
            return vaH;
        }

        public double getAH() {
            return aH;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", BatteryReadings.class.getSimpleName() + "[", "]")
                    .add("time=" + time)
                    .add("voltage=" + getVoltage())
                    .add("amperage=" + getAmperage())
                    .add("vaH=" + getVaH())
                    .add("aH=" + getAH())
                    .toString();
        }
    }

}
