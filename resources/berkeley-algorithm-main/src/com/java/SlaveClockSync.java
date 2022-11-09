package com.java;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class SlaveClockSync extends Thread {

    private ProcessInfo slaveProcessInfo;
    private SoftwareClock slaveSoftwareClock;
    private DatagramSocket socket;
    private DatagramPacket packet;
    MulticastSocket groupSocket;
    private InetSocketAddress groupAddress;

    private final int DEFAULT_RECEIVE_MASTER_NEW_TIME_TIMEOUT = 10000; // 10s

    public SlaveClockSync(ProcessInfo slave, SoftwareClock softwareClock, MulticastSocket groupSocket, InetSocketAddress groupAddress) throws SocketException {
        this.slaveProcessInfo = slave;
        this.slaveSoftwareClock = softwareClock;
        this.groupSocket = groupSocket;
        this.groupAddress = groupAddress;
        this.socket = new DatagramSocket(slave.getPort());
        this.socket.setSoTimeout(DEFAULT_RECEIVE_MASTER_NEW_TIME_TIMEOUT);
    }

    @Override
    public void run() {
        InetAddress masterServerAddress;
        int masterServerPort;
        LocalDateTime masterTime;
        LocalDateTime scT2;
        long rtcT2;
        LocalDateTime scT3;
        long rtcT3;
        
        while (true) {
            try {
                byte[] receiveBuffer = new byte[1024];
                DatagramPacket groupPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                System.out.printf("[GROUP] Escutando multicast do group (%s:%s)\n", groupAddress.getAddress().toString(), groupAddress.getPort());
                this.groupSocket.receive(groupPacket);

                // T2 register
                scT2 = slaveSoftwareClock.getInstantTime();
                rtcT2 = System.currentTimeMillis();

                // DEBUG package
                String debug = "\n[DEBUG PACKAGE]\n"
                    + "[TIME_RECEIVED] = " + scT2.toString() + "\n"
                    // + "HOSTNAME> "+ groupPacket.getAddress().getHostName() + "\n"
                    + "ADDRESS> "+ groupPacket.getAddress().getHostAddress() + "\n"
                    + "PORT> "+ groupPacket.getPort() + "\n"
                    + "SOCKET_ADDRESS> "+ groupPacket.getSocketAddress() + "\n"
                    + "DATA_LENGHT> "+ groupPacket.getData().length + "\n";
                System.out.println(debug);
                // End DEBUG

                // Decode and check package
                String content = new String(groupPacket.getData(), 0, groupPacket.getLength()).trim();
                String data[] = content.split("\\|");
                System.out.println("[DATA] " + content);
                
                if (!data[0].equals("POLLING")) {
                    System.out.println("[DATA] Pacote descartado.");
                    continue;
                }

                // Register Master info
                masterTime = LocalDateTime.parse(data[1]);
                masterServerAddress = InetAddress.getByName(data[2]);
                masterServerPort = Integer.parseInt(data[3]);
                
                // Delta master-slave info (T2-T1)
                System.out.printf("[INFO] [SLAVE_DELTA] = %s\n", ChronoUnit.MILLIS.between(masterTime, scT2));

                // T2 info
                System.out.printf("[INFO] [T2_TIME] = %s\n", scT2);

                // DEBUG
                System.out.printf("[DEBUG] Aguardando o tempo de processamento. [pTime = %dms]\n", slaveProcessInfo.getPTime());
                // Waiting for pTime lapse
                Thread.sleep(slaveProcessInfo.getPTime());

                // T3 register
                scT3 = slaveSoftwareClock.getInstantTime();
                rtcT3 = System.currentTimeMillis();
                slaveProcessInfo.setTime(scT3);
                slaveProcessInfo.setDelta(ChronoUnit.MILLIS.between(masterTime, scT3));
                
                // T3 info
                System.out.printf("[INFO] [T3_TIME] = %s\n", scT3);
                
                // Delta T info (T3-T2) - actual pTime
                System.out.printf("[INFO] [T3 - T2] [SC_PTIME] = %d [RTC_PTIME] = %d\n", ChronoUnit.MILLIS.between(scT2, scT3), (rtcT3 - rtcT2));

                // Reply to master
                try {
                    String slaveTimeMessage = String.join("|",
                        "POLLING_REPLY",
                        String.valueOf(slaveProcessInfo.getPid()),
                        String.valueOf(slaveProcessInfo.getDelta()));
        
                    byte[] slaveTimeBuffer = slaveTimeMessage.getBytes();
        
                    DatagramPacket slavePacket = new DatagramPacket(slaveTimeBuffer, slaveTimeBuffer.length, masterServerAddress, masterServerPort);

                    // DEBUG
                    System.out.printf("[DEBUG] Simulando a latencia da rede. [aDelay = %dms]\n", slaveProcessInfo.getADelay());
                    // Waiting for aDelay lapse
                    Thread.sleep(slaveProcessInfo.getADelay());

                    this.socket.send(slavePacket);
                    System.out.printf("[DATA] Envio da mensagem \"%s\" ao mestre (%s:%s)\n", slaveTimeMessage, masterServerAddress.toString(), masterServerPort);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

                // Receive new time delta
                byte[] receiveNewTimeBuffer = new byte[1024];
                this.packet = new DatagramPacket(receiveNewTimeBuffer, receiveNewTimeBuffer.length);

                try {
                    System.out.printf("[INFO] Aguardando ajuste do relogio (%s:%s)\n", groupAddress.getAddress().toString(), groupAddress.getPort());
                    this.socket.receive(this.packet);
                } catch (SocketTimeoutException e) {
                    // DEBUG
                    System.out.println("[TIMEOUT] Sem resposta do master.");
                    continue;
                }

                String newTimeString = new String(this.packet.getData(), 0, this.packet.getLength()).trim();
                String newTimeData[] = newTimeString.split("\\|");
                System.out.println("[DATA] " + newTimeString);

                if (!newTimeData[0].equals("NEW_TIME")) {
                    System.out.println("[DATA] Pacote descartado.");
                    continue;
                }
                long newTime = Long.parseLong(newTimeData[1]);

                LocalDateTime oldTime = slaveSoftwareClock.getInstantTime();
                slaveSoftwareClock.updateClock(newTime);
                long newTimeDelta = ChronoUnit.MILLIS.between(oldTime, slaveSoftwareClock.getInstantTime());
                
                System.out.println("[NEW CLOCK TIME] " + slaveSoftwareClock.getInstantTime() + " [DELTA] " + newTimeDelta);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                continue;
            }
        }
    }
}
