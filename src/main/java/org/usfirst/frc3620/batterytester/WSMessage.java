package org.usfirst.frc3620.batterytester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;
import java.util.StringJoiner;

@JsonSerialize
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

  public static class BatteryTestReading extends WSMessage {
    private final BatteryTester.BatteryTestReading reading;
    private final boolean update;

    public BatteryTestReading(BatteryTester.BatteryTestReading reading, boolean update) {
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
      return new StringJoiner(", ", BatteryReadings.class.getSimpleName() + "[", "]")
        .add("time=" + getTime())
        .add("voltage=" + getVoltage())
        .add("amperage=" + getAmperage())
        .add("catchup=" + getUpdate())
        .toString();
    }

  }

  public static WSMessage START_BATTERY_TEST = new StartBatteryTest();
  private static class StartBatteryTest extends WSMessage {
    public StartBatteryTest() { }
  }

  public static class TickTock extends WSMessage {
    private Date now = new Date();
    public TickTock() { }

    public Date getNow() {
      return now;
    }

    public String getHuman() {
      return now.toString();
    }
  }
}