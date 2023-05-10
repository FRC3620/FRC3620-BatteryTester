package org.usfirst.frc3620.batterytester;

public class Timer implements Cloneable {
    long nanos;
    public Timer() {
        reset();
    }

    public void reset() {
        nanos = System.nanoTime();
    }

    public double elapsed() {
        return elapsed(System.nanoTime());
    }

    public double elapsed(Timer t) {
        return elapsed(t.nanos);
    }

    double elapsed(long t) {
        return (t - nanos) / 1_000_000_000.0;
    }

    @Override
    public Timer clone() {
        try {
            return (Timer) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
