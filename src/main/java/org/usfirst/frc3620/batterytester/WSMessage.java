package org.usfirst.frc3620.batterytester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;
import java.util.StringJoiner;

@JsonSerialize
public class WSMessage {
  static ObjectMapper objectMapper = new ObjectMapper();

  /**
   * run Jackson through it's paces. Do this early so that the battery testing thread does not
   * take a significant delay the first time through (initial invocation of writeValueAsString
   * loads a *lot* of classes.
   */
  static public void prime() {
    // get classes loaded early
    WSMessage w = new BatteryTestReadingMessage(new BatteryTester.BatteryTestReading(0, 0, 0, 0, 0), true);
    try {
      objectMapper.writeValueAsString(w);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private static class Dummy {
    public String s;
    public double d;
    public int i;
    public boolean b;
  }

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
    M (WSMessage payload) {
      String s = payload.getClass().getSimpleName();
      if (s.endsWith("Message")) {
        s = s.substring(0, s.length() - 7);
      }
      this.messageType = s;
      this.payload = payload;
    }
  }

  static public class BatteryTestStatusMessage extends WSMessage {
    private final BatteryTester.Status status;

    public BatteryTestStatusMessage(BatteryTester.Status status) {
      this.status = status;
    }

    /**
     * @return current status of battery tester
     */
    public BatteryTester.Status getStatus() {
      return status;
    }
  }

  public static class BatteryTestReadingMessage extends WSMessage {
    private final BatteryTester.BatteryTestReading reading;
    private final boolean update;

    public BatteryTestReadingMessage(BatteryTester.BatteryTestReading reading, boolean update) {
      this.reading = reading;
      this.update = update;
    }

    /**
     * @return returns epoch millisecond time of battery reading
     */
    public double getTime() {
      return reading.getTime();
    }

    public double getAmperage() {
      return reading.getAmperage();
    }

    public double getVoltage() {
      return reading.getVoltage();
    }

    public boolean getUpdate() {
      return update;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", BatteryReading.class.getSimpleName() + "[", "]")
        .add("time=" + getTime())
        .add("voltage=" + getVoltage())
        .add("amperage=" + getAmperage())
        .add("catchup=" + getUpdate())
        .toString();
    }
  }

  public static class BatteryReadingMessage extends WSMessage {
    private final BatteryReading reading;
    public BatteryReadingMessage(BatteryReading reading) {
      this.reading = reading;
    }

    public double getAmperage() {
      return reading.getAmperage();
    }

    public double getVoltage() {
      return reading.getVoltage();
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
              .add("voltage=" + getVoltage())
              .add("amperage=" + getAmperage())
              .toString();
    }
  }

  public static WSMessage START_BATTERY_TEST = new StartBatteryTestMessage();
  private static class StartBatteryTestMessage extends WSMessage {
    public StartBatteryTestMessage() { }
  }

  public static class TickTockMessage extends WSMessage {
    private Date now = new Date();
    public TickTockMessage() { }

    public Date getNow() {
      return now;
    }

    public String getHuman() {
      return now.toString();
    }
  }
}