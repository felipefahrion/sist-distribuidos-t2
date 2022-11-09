package com.java;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class SoftwareClock extends Thread {
    protected long DEFAULT_TICK_RATE_MS = 20; // 1-100ms
    protected long DEFAULT_PRINT_RATE_MS = TimeUnit.SECONDS.toMillis(1); // 1s
    protected LocalDateTime clock;

    public SoftwareClock() {
        this.clock = LocalDateTime.now();
    }

    public SoftwareClock(long timeOffset) {
        this.clock = LocalDateTime.now().plus(timeOffset, ChronoUnit.MILLIS);
    }

    public void run() {
        System.out.println("[CLOCK_START] " + this.clock.toString());

        long printCheck = 0;

        long currentSystemTime = System.currentTimeMillis();
        long systemTimeInstant;
        long currentSystemDelta = 0;

        LocalDateTime currentSoftwareClock = this.clock;
        LocalDateTime softwareClockInstant;
        long currentSoftwareClockDelta = 0;

        long clockLag = 0;

        while (true) {
            try {
                Thread.sleep(DEFAULT_TICK_RATE_MS);
                this.updateClock(DEFAULT_TICK_RATE_MS);
                
                printCheck += DEFAULT_TICK_RATE_MS;
                if (printCheck == DEFAULT_PRINT_RATE_MS) {
                    softwareClockInstant = clock;
                    currentSoftwareClockDelta = ChronoUnit.MILLIS.between(currentSoftwareClock, softwareClockInstant); 
                    currentSoftwareClock = softwareClockInstant;

                    systemTimeInstant = System.currentTimeMillis();
                    currentSystemDelta = systemTimeInstant - currentSystemTime; 
                    currentSystemTime = systemTimeInstant;
                    
                    clockLag += currentSoftwareClockDelta - currentSystemDelta;

                    System.out.println("[TICK] " + clock.toString() + " [DELTA] " + currentSoftwareClockDelta + " [RTC_DELTA] " + currentSystemDelta + " [CLOCK_TIME_LAG] " + clockLag);

                    printCheck = 0;
                }
            } catch (InterruptedException e) {
                System.err.println(this.getClass() + "[ERRO] Erro no Thread.sleep().");
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
