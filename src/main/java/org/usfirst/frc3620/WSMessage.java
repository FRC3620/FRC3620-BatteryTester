package org.usfirst.frc3620;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.StringJoiner;

public class WSMessage {
  static ObjectMapper objectMapper = new ObjectMapper();

  public String json() {
    try {
      return objectMapper.writeValueAsString(new M(this));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  static class M {
    public final String messageType;
    public final WSMessage payload;
    M(WSMessage payload) {
      this.messageType = payload.getClass().getSimpleName();
      this.payload = payload;
    }
  }

  static public class BatteryTestStatus extends WSMessage {
    private final BatteryTester.Status status;

    public BatteryTestStatus(BatteryTester.Status status) {
      this.status = status;
    }

    /**
     * @return current status of battery tester
     */
    public BatteryTester.Status getStatus() {
      return status;
    }
  }

  public static class BatteryTestReadings extends WSMessage {
    private final double time, voltage, amperage;

    public BatteryTestReadings(double t, double v, double a) {
      this.time = t;
      this.voltage = v;
      this.amperage = a;
    }

    public BatteryTestReadings(double t, BatteryReadings batteryStatus) {
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

  public static class BatteryStartTestMessage extends WSMessage {
    public BatteryStartTestMessage() { }
  }
}