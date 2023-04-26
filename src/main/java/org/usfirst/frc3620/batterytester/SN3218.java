package org.usfirst.frc3620.batterytester;

import com.diozero.api.I2CDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;

public class SN3218 implements AutoCloseable {
    Logger logger = LoggerFactory.getLogger(getClass());

    private final int I2C_ADDRESS = 0x54;
    private final byte CMD_ENABLE_OUTPUT = 0x00;
    private final byte CMD_SET_PWM_VALUES = 0x01;
    private final byte CMD_ENABLE_LEDS = 0x13;
    private final byte CMD_UPDATE = 0x16;
    private final byte CMD_RESET = 0x17;

    I2CDevice device;

    boolean autoDisable = false;

    public SN3218(int controller) {
        this(controller, false);
    }

    public SN3218(int controller, boolean autoDisable) {
        device = I2CDevice.builder(I2C_ADDRESS).setController(controller).setByteOrder(ByteOrder.BIG_ENDIAN)
                .build();
        this.autoDisable = autoDisable;
        reset();

        enable();

        int enable_mask = 0x3FFFF;
        device.writeI2CBlockData(CMD_ENABLE_LEDS,
                (byte) (enable_mask & 0x3F),
                (byte) ((enable_mask >> 6) & 0x3F),
                (byte) ((enable_mask >> 12) & 0X3F));
        update();
    }

    void update() {
        device.writeI2CBlockData(CMD_UPDATE, (byte) 0xFF);
    }

    public void enable() {
        device.writeI2CBlockData(CMD_ENABLE_OUTPUT, (byte) 0x01);
    }

    public void disable() {
        logger.info ("disabling");
        device.writeI2CBlockData(CMD_ENABLE_OUTPUT, (byte) 0x00);
    }

    public void reset() {
        device.writeI2CBlockData(CMD_RESET, (byte) 0xff);
    }

    public void output_raw (int index, byte v) {
        // TODO need to check index

        device.writeI2CBlockData((byte) (CMD_SET_PWM_VALUES + index), v);
        update();
    }

    public void close() {
        if (autoDisable) disable();
        device.close();
    }

    public static void main(String[] args) {
        try (SN3218 sn3218 = new SN3218(1, true) ) {
            int i = 0;
            while (true) {
                sn3218.output_raw(i, (byte) 16);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {

                }
                sn3218.output_raw(i, (byte) 0);

                i++;
                if (i > 17) i = 0;
            }
        }
    }
}
