package org.usfirst.frc3620.batterytester;

import com.diozero.api.I2CDevice;

import java.nio.ByteOrder;

public class SN3218 implements AutoCloseable {
    private final int I2C_ADDRESS = 0x54;
    private final byte CMD_ENABLE_OUTPUT = 0x00;
    private final byte CMD_SET_PWM_VALUES = 0x01;

    private final byte CMD_ENABLE_LEDS = 0x13;
    private final byte CMD_UPDATE = 0x16;
    private final byte CMD_RESET = 0x17;

    I2CDevice device;

    public SN3218(int controller) {
        device = I2CDevice.builder(I2C_ADDRESS).setController(controller).setByteOrder(ByteOrder.BIG_ENDIAN)
                .build();

        int enable_mask = 0x3FFFF;
        device.writeBlockData(I2C_ADDRESS, CMD_ENABLE_LEDS,
                (byte) (enable_mask & 0x3F),
                (byte) ((enable_mask >> 6) & 0x3F),
                (byte) ((enable_mask >> 12) & 0X3F));
        device.writeBlockData(I2C_ADDRESS, CMD_UPDATE, (byte) 0xFF);
    }

    public void enable() {
        device.writeBlockData(I2C_ADDRESS, CMD_ENABLE_OUTPUT, (byte) 0x01);
    }

    public void disable() {
        device.writeBlockData(I2C_ADDRESS, CMD_ENABLE_OUTPUT, (byte) 0x00);
    }

    public void reset() {
        device.writeBlockData(I2C_ADDRESS, CMD_RESET, (byte) 0xff);
    }

    public void close() {
        device.close();
    }
}
