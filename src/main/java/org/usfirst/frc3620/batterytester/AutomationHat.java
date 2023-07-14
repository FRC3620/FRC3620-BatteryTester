package org.usfirst.frc3620.batterytester;

import com.diozero.api.AnalogInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.devices.Ads1x15;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomationHat implements AutoCloseable {
    Logger logger = LoggerFactory.getLogger(getClass());

    SN3218 sn3218;
    Ads1x15 ads;

    AnalogInputDevice[] analogInputDevices;
    DigitalOutputDevice[] relays = new DigitalOutputDevice[3];
    public final byte[] LED_RELAYS_NO = { 6, 8, 10 };
    public final byte[] LED_RELAYS_NC = { 7, 9, 11 };

    public final byte[] LED_ADC = { 0, 1, 2 };

    public final byte LED_POWER = 17;
    public final byte LED_COMMS = 16;
    public final byte LED_WARN = 15;

    private final byte LED_RAW_POWER_ON = 16;
    private final byte LED_RAW_POWER_OFF = 0;

    public AutomationHat (int controller, Ads1x15.PgaConfig pgaConfig, Ads1x15.Ads1015DataRate dataRate) {
        ads = new Ads1x15(controller, Ads1x15.Address.GND, pgaConfig, dataRate);
        finishSetup(controller);
    }

    public AutomationHat (int controller, Ads1x15.PgaConfig pgaConfig, Ads1x15.Ads1115DataRate dataRate) {
        ads = new Ads1x15(controller, Ads1x15.Address.GND, pgaConfig, dataRate);
        finishSetup(controller);
    }

    void finishSetup(int controller) {
        ads.setSingleMode(1);

        int numChannels = ads.getModel().getNumChannels();
        analogInputDevices = new AnalogInputDevice[numChannels];
        for (int channel = 0; channel < numChannels; channel++) {
            analogInputDevices[channel] = new AnalogInputDevice(ads, channel);
        }
        sn3218 = new SN3218(controller);

        sn3218.output_raw(LED_POWER, LED_RAW_POWER_ON);

        relays[0] = makeRelay(13);
        relays[1] = makeRelay(19);
        relays[2] = makeRelay(16);

        for (int i = 1; i <= 3; i++) {
            setRelay(i, false);
        }
    }

    DigitalOutputDevice makeRelay(int gpio) {
        return DigitalOutputDevice.Builder.builder(gpio).setInitialValue(false).build();
    }

    public void setRelay (int i, boolean b) {
        sn3218.output_raw(LED_COMMS, LED_RAW_POWER_ON);
        DigitalOutputDevice d = relays[i-1];
        d.setValue(b);
        logger.info ("setting GPIO {} to {}", d.getGpio(), b);
        if (b) {
            sn3218.output_raw(LED_RELAYS_NO[i-1], LED_RAW_POWER_ON);
            sn3218.output_raw(LED_RELAYS_NC[i-1], LED_RAW_POWER_OFF);
        } else {
            sn3218.output_raw(LED_RELAYS_NO[i-1], LED_RAW_POWER_OFF);
            sn3218.output_raw(LED_RELAYS_NC[i-1], LED_RAW_POWER_ON);
        }
        sn3218.output_raw(LED_COMMS, LED_RAW_POWER_OFF);
    }

    /**
     * read an AD
     * @param channel index of the AD. This is the number on the board (1..4), not
     *          the internal ADS1x15 ADC number.
     * @return voltage at the input
     */
    public double readAnalogInputVoltage (int channel) {
        int adsChannel = channel - 1;
        sn3218.output_raw(LED_COMMS, LED_RAW_POWER_ON);
        if (channel < 4) {
            sn3218.output_raw(LED_ADC[adsChannel], LED_RAW_POWER_ON);
        }

        AnalogInputDevice aid = analogInputDevices[adsChannel];
        float ad_voltage = aid.getScaledValue();
        float rv = ad_voltage * (float) ((channel == 4) ? 1.0 : (25.85 / 3.3));

        logger.debug ("ADS channel {} : {} {}", adsChannel, ad_voltage, rv);

        if (channel < 4) {
            sn3218.output_raw(LED_ADC[adsChannel], LED_RAW_POWER_OFF);
        }
        sn3218.output_raw(LED_COMMS, LED_RAW_POWER_OFF);
        return rv;
    }

    @Override
    public void close() throws Exception {
        for (var r : relays) {
            r.off();
        }
        sn3218.disable();
        sn3218.close();
    }
}
