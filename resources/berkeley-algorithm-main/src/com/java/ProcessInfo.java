package com.java;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class ProcessInfo {

    private final InetAddress ipAddress;
    private final int port;
    private final int pid;
    private LocalDateTime time;
    private long pTime;
    private long aDelay;
    private long delta;
    private long rtt;

     public ProcessInfo(int pid, InetAddress ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.pid = pid;
    }

     public ProcessInfo(int pid, InetAddress ipAddress, int port, long pTime, long aDelay) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.pid = pid;
        this.pTime = pTime;
        this.aDelay = aDelay;
    }

    public long getRtt() {
        return rtt;
    }

    public void setRtt(long rtt) {
        this.rtt = rtt;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public int getPid() {
        return pid;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime newTime) {
        this.time = newTime;
    }

    public long getDelta() {
        return delta;
    }

    public void setDelta(long delta) {
        this.delta = delta;
    }

    public long getAdjustedDelta(long averageTime) {
        return averageTime - this.delta + (rtt / 2);
    }

    public long getPTime() {
        return pTime;
    }

    public long getADelay() {
        return aDelay;
    }

    @Override
    public String toString() {
        return "ProcessInfo[" +
                "ipAddress=" + ipAddress +
                ", port=" + port +
                ", pid=" + pid +
                ((time != null) ? ", time=" + time : "") +
                ", delta=" + delta +
                ", rtt=" + rtt +
                ']';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessInfo that = (ProcessInfo) o;
        return ipAddress.equals(that.ipAddress) && port == that.port && pid == that.pid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, port, pid);
    }
}
