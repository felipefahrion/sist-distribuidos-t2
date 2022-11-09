package com.java;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class SoftwareClockMultithread extends SoftwareClock {

    public SoftwareClockMultithread() {
        super();
    }

    public SoftwareClockMultithread(long timeOffset) {
        super(timeOffset);
    }

    @Override
    public void run() {
        System.out.println("[CLOCK_START] " + this.clock.toString());

        Thread tickClock = new TickClock(this);
        Thread printClockTick = new PrintClock(this);
        tickClock.start();
        printClockTick.start();
    }

    private class TickClock extends Thread {
        private SoftwareClock softwareClock;

        public TickClock(SoftwareClock softwareClock) {
            this.softwareClock = softwareClock;
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(softwareClock.DEFAULT_TICK_RATE_MS);
                    softwareClock.updateClock(DEFAULT_TICK_RATE_MS);
                } catch (InterruptedException e) {
                    System.err.println(this.getClass() + "[ERRO] Erro no Thread.sleep() do TickClock.");
                }
            }
        }
    }

    private class PrintClock extends Thread {
        private SoftwareClock softwareClock;

        PrintClock(SoftwareClock softwareClock) {
            this.softwareClock = softwareClock;
        }

        public void run() {
            LocalDateTime currentSoftwareClock = softwareClock.clock;
            LocalDateTime softwareClockInstant;
            long currentSoftwareClockDelta = 0;

            long currentSystemTime = System.currentTimeMillis();
            long systemTimeInstant;
            long currentSystemDelta = 0;

            long clockLag = 0;

            while (true) {
                try {
                    Thread.sleep(softwareClock.DEFAULT_PRINT_RATE_MS);
                    
                    softwareClockInstant = softwareClock.clock;
                    currentSoftwareClockDelta = ChronoUnit.MILLIS.between(currentSoftwareClock, softwareClockInstant); 
                    currentSoftwareClock = softwareClockInstant;

                    systemTimeInstant = System.currentTimeMillis();
                    currentSystemDelta = systemTimeInstant - currentSystemTime; 
                    currentSystemTime = systemTimeInstant;

                    clockLag += currentSoftwareClockDelta - currentSystemDelta;
                    
                    System.out.println("[TICK] " + softwareClock.clock.toString() + " [DELTA] " + currentSoftwareClockDelta + " [RTC_DELTA] " + currentSystemDelta + " [CLOCK_TIME_LAG] " + clockLag);
                } catch (InterruptedException e) {
                    System.err.println(this.getClass() + "[ERRO] Erro no Thread.sleep() do PrintTick.");
                }
            }
        }
    }

    public void updateClock(long milliseconds) {
        this.clock = this.clock.plus(milliseconds, ChronoUnit.MILLIS);
    }

    public LocalDateTime getInstantTime() {
        return LocalDateTime.from(clock);
    }
}
