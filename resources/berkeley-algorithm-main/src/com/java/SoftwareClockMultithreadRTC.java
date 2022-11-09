package com.java;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class SoftwareClockMultithreadRTC extends SoftwareClock {

    public SoftwareClockMultithreadRTC() {
        super();
    }

    public SoftwareClockMultithreadRTC(long timeOffset) {
        super(timeOffset);
    }

    @Override
    public void run() {
        System.out.println("[CLOCK_START] " + this.clock.toString());

        // Clock Tick Thread
        new Thread(() -> {
            long currentSystemTime = System.currentTimeMillis();
            long systemTimeInstant;
            long currentSystemDelta = 0;

            while (true) {
                try {
                    Thread.sleep(this.DEFAULT_TICK_RATE_MS);
                    systemTimeInstant = System.currentTimeMillis();
                    currentSystemDelta = systemTimeInstant - currentSystemTime;
                    currentSystemTime = systemTimeInstant;

                    this.updateClock(currentSystemDelta);
                } catch (InterruptedException e) {
                    System.err.println(this.getClass() + "[ERRO] Erro no Thread.sleep() do TickClock.");
                }
            }
        }).start();

        // Print Clock Tick Thread
        new Thread(() -> {
            LocalDateTime currentSoftwareClock = this.clock;
            LocalDateTime softwareClockInstant;
            long currentSoftwareClockDelta = 0;

            long currentSystemTime = System.currentTimeMillis();
            long systemTimeInstant;
            long currentSystemDelta = 0;

            long clockLag = 0;

            while (true) {
                try {
                    Thread.sleep(this.DEFAULT_PRINT_RATE_MS);
                    
                    softwareClockInstant = this.clock;
                    currentSoftwareClockDelta = ChronoUnit.MILLIS.between(currentSoftwareClock, softwareClockInstant); 
                    currentSoftwareClock = softwareClockInstant;

                    systemTimeInstant = System.currentTimeMillis();
                    currentSystemDelta = systemTimeInstant - currentSystemTime; 
                    currentSystemTime = systemTimeInstant;

                    clockLag += currentSoftwareClockDelta - currentSystemDelta;
                    
                    System.out.println("[TICK] " + this.clock.toString() + " [DELTA] " + currentSoftwareClockDelta + " [RTC_DELTA] " + currentSystemDelta + " [CLOCK_TIME_LAG] " + clockLag);
                } catch (InterruptedException e) {
                    System.err.println(this.getClass() + "[ERRO] Erro no Thread.sleep() do PrintTick.");
                }
            }
        }).start();
    }

    public void updateClock(long milliseconds) {
        this.clock = this.clock.plus(milliseconds, ChronoUnit.MILLIS);
    }

    public LocalDateTime getInstantTime() {
        return LocalDateTime.from(clock);
    }
}
