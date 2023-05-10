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
        STOPPED, RUNNING, PAUSED
    }

    enum InternalStatus {
        DETERMINING_RINT_1_1, DETERMINING_RINT_1_2, LOADED, DETERMINING_RINT_2_1, DETERMINING_RINT_2_2;
    }

    Status status = Status.STOPPED;

    InternalStatus internalStatus = InternalStatus.DETERMINING_RINT_1_1;

    Timer test_t0 = null;

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
        int loop_timer_counter = 0; // set to non-zero to get some benchmarking along the way
        while (true) {
            Timer loop_t0 = new Timer();
            if (fakeBattery != null) fakeBattery.update();

            BatteryReading batteryReading = battery.getBatteryReading();

            if (status == Status.RUNNING || status == Status.PAUSED) {
                double tDelta = loop_t0.elapsed(0);
                if (test_t0 != null) {
                    tDelta = test_t0.elapsed(loop_t0);
                }

                if (loop_timer_counter > 0) {
                    logger.debug ("reading battery, t = {}", loop_t0.elapsed());
                }

                BatteryTestReading batteryTestReading = new BatteryTestReading(tDelta, batteryReading, 0, 0);
                logger.debug("sample {}, t {}, v {}, a {}, is {}", testSamples.size(), tDelta, batteryTestReading.getVoltage(), batteryTestReading.getAmperage(), internalStatus);


                if (loop_timer_counter > 0) {
                    logger.debug ("adding to array, t = {}", loop_t0.elapsed());
                }
                synchronized (testSamples) {
                    testSamples.add(batteryTestReading);
                }

                if (loop_timer_counter > 0) {
                    logger.debug ("sending samples, t = {}", loop_t0.elapsed());
                }
                sendToAll(new WSMessage.BatteryTestReadingMessage(batteryTestReading, true));
                if (loop_timer_counter > 0) {
                    logger.debug ("samples sent, t = {}", loop_t0.elapsed());
                }

                if (internalStatus == InternalStatus.DETERMINING_RINT_1_1) {
                    battery.setLoad(loadAmperage);
                    internalStatus = InternalStatus.DETERMINING_RINT_1_2;
                } else if (internalStatus == InternalStatus.DETERMINING_RINT_1_2) {
                    internalStatus = InternalStatus.LOADED;
                } else if (internalStatus == InternalStatus.LOADED) {
                    if (status != Status.PAUSED) {
                        battery.setLoad(loadAmperage);
                    } else {
                        battery.setLoad(0);
                    }
                } else if (internalStatus == InternalStatus.DETERMINING_RINT_2_1) {
                    battery.setLoad(0);
                    internalStatus = InternalStatus.DETERMINING_RINT_2_2;
                } else if (internalStatus == InternalStatus.DETERMINING_RINT_2_2) {
                    status = Status.STOPPED;
                    sendStatus();
                }

                if (batteryTestReading.getVoltage() < 10.7) {
                    logger.info ("hit voltage threshold, shutting down test");
                    internalStatus = InternalStatus.DETERMINING_RINT_2_1;
                }

            } else {
                sendToAll(new WSMessage.BatteryReadingMessage(batteryReading));
            }

            sendToAll(new WSMessage.TickTockMessage()); // help determine dropped connections

            if (loop_timer_counter > 0) loop_timer_counter--;

            synchronized (collectionThreadWaitLock) {
                double delay = 1.0;
                if (internalStatus == InternalStatus.DETERMINING_RINT_1_2 || internalStatus == InternalStatus.DETERMINING_RINT_2_2) {
                    delay = 0.1;
                }
                try {
                    collectionThreadWaitLock.wait((int) (delay * 1000));
                } catch (InterruptedException ignored) {

                }
            }
        }
    }

    public Status getStatus() {
        return status;
    }

    void sendStatus() {
        sendToAll(new WSMessage.BatteryTestStatusMessage(status));
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

    public boolean startTest () {
        if (status == Status.RUNNING) {
            logger.info ("tried to start test, but already running");
            return false;
        }
        if (status == Status.STOPPED) {
            if (fakeBattery != null) fakeBattery.reset();
            logger.info ("starting test, load = {}", loadAmperage);
            test_t0 = new Timer();
            testSamples.clear();
            vaH = 0.0;
            aH = 0.0;
            sendToAll(WSMessage.START_BATTERY_TEST);
        } else if (status == Status.PAUSED) {
            logger.info ("resuming test, load = {}", loadAmperage);
            // nothing to do
        }
        internalStatus = InternalStatus.DETERMINING_RINT_1_1;
        status = Status.RUNNING;
        sendStatus();
        logger.info("bumping the collection thread");
        bumpCollectionThread();
        return true;
    }

    public void setLoadAmperage(double a) {
        loadAmperage = a;
    }

    public boolean pauseTest() {
        if (status != Status.RUNNING) {
            return false;
        }
        status = Status.PAUSED;
        battery.setLoad(0.0);
        sendStatus();
        bumpCollectionThread();
        return true;
    }

    public boolean stopTest () {
        if (status == Status.STOPPED) {
            return false;
        }
        internalStatus = InternalStatus.DETERMINING_RINT_2_1;
        sendStatus();
        bumpCollectionThread();
        return true;
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
                    q.put(new WSMessage.BatteryTestStatusMessage(status));
                    q.put(WSMessage.START_BATTERY_TEST);

                    synchronized (testSamples) {
                        Iterator<BatteryTestReading> i = testSamples.iterator();
                        while (i.hasNext()) {
                            BatteryTestReading r = i.next();
                            boolean shouldUpdate = ! i.hasNext(); // don't update if more coming
                            WSMessage catchupMessage = new WSMessage.BatteryTestReadingMessage(r, shouldUpdate);
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
        battery.close();
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

        public BatteryTestReading(double t, BatteryReading batteryStatus, double vaH, double aH) {
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
            return new StringJoiner(", ", BatteryReading.class.getSimpleName() + "[", "]")
                    .add("time=" + time)
                    .add("voltage=" + getVoltage())
                    .add("amperage=" + getAmperage())
                    .add("vaH=" + getVaH())
                    .add("aH=" + getAH())
                    .toString();
        }
    }

}
