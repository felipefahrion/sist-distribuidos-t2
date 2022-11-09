package com.java;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class MasterClockSyncTask extends TimerTask {

    private ProcessInfo masterProcessInfo;
    private SoftwareClock masterSoftwareClock;
    private DatagramSocket socket;
    private DatagramPacket packet;
    private List<ProcessInfo> processes;
    private LocalDateTime scT1;
    private LocalDateTime scT4;
    private MulticastSocket groupSocket;
    private InetSocketAddress groupAddress;

    private final int DEFAULT_GROUP_REPLY_TIME_WINDOW = 5000; // 5s 
    private final int DEFAULT_SOCKET_TIMEOUT = Math.max(DEFAULT_GROUP_REPLY_TIME_WINDOW / 10, 200); // minimo de 200ms
    private final int DEFAULT_TRIM_TIME = 10000; // 10s de offset para poda

    private byte[] responseTimesBuffer = new byte[1024];
    private byte[] receiveTimesBuffer = new byte[1024];

    public MasterClockSyncTask(ProcessInfo master, SoftwareClock softwareClock, MulticastSocket groupSocket, InetSocketAddress groupAddress) throws SocketException {
        this.masterProcessInfo = master;
        this.masterSoftwareClock = softwareClock;
        this.groupSocket = groupSocket;
        this.groupAddress = groupAddress;
        this.socket = new DatagramSocket(master.getPort());
        this.socket.setSoTimeout(DEFAULT_SOCKET_TIMEOUT);
    }

    @Override
    public void run() {
        processes = new ArrayList<>();
        processes.add(masterProcessInfo);

        this.sendPollingMessageToGroup();
        if (!this.receiveSlavesTimes()) {
            System.out.println("[TIMEOUT] Nenhum processo escravo retornou seu tempo.");
            return;
        }

        long averageTime = this.calculateAverageTime();
        System.out.println("[INFO] Tempo medio = " + averageTime);
        this.adjustProcessesTimes(averageTime);
    }

    private void sendPollingMessageToGroup() {
        try {
            // T1 register
            this.scT1 = masterSoftwareClock.getInstantTime();
            masterProcessInfo.setTime(scT1);
            
            // T1 info
            System.out.printf("[INFO] [T1_TIME] = %s\n", scT1);

            String pollingMessage = String.join("|",
                "POLLING",
                masterProcessInfo.getTime().toString(),
                masterProcessInfo.getIpAddress().toString().split("\\/")[1],
                String.valueOf(masterProcessInfo.getPort()));

            byte[] masterTimeBuffer = pollingMessage.getBytes();

            DatagramPacket groupPacket = new DatagramPacket(masterTimeBuffer, masterTimeBuffer.length, groupAddress.getAddress(), groupAddress.getPort());

            // DEBUG
            System.out.printf("[DEBUG] Simulando a latencia da rede. [aDelay = %dms]\n", masterProcessInfo.getADelay());
            // Waiting for Master aDelay lapse
            Thread.sleep(this.masterProcessInfo.getADelay());

            this.groupSocket.send(groupPacket);
            System.out.printf("[DATA] Envio da mensagem \"%s\" ao grupo (%s:%s)\n", masterProcessInfo.getTime().toString(), groupAddress.getAddress().toString(), groupAddress.getPort());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean receiveSlavesTimes() {
        boolean result = false;
        long elapsedTime; // used in debug message
        while ((elapsedTime = ChronoUnit.MILLIS.between(this.scT1, this.masterSoftwareClock.getInstantTime())) < DEFAULT_GROUP_REPLY_TIME_WINDOW) {
            this.receiveTimesBuffer = new byte[1024];
            this.packet = new DatagramPacket(receiveTimesBuffer, receiveTimesBuffer.length);
            
            try {
                // DEBUG
                //System.out.println("[DEBUG] Aguardando receive. [TIME_ELAPSED] = " + elapsedTime);
                this.socket.receive(this.packet);

                // T4 register
                this.scT4 = masterSoftwareClock.getInstantTime();

                // DEBUG
                // System.out.println("[DEBUG] Apos receive. [TIME_ELAPSED] = " + ChronoUnit.MILLIS.between(this.scT1, this.scT4));
            } catch (SocketTimeoutException e) {
                // DEBUG
                // System.out.println("[TIMEOUT] Socket timeout.");
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            
            // DEBUG package
            String debug = "\n[DEBUG PACKAGE]\n"
                + "[TIME_RECEIVED] = " + scT4.toString() + "\n"
                // + "HOSTNAME> "+ packet.getAddress().getHostName() + "\n"
                + "ADDRESS> "+ packet.getAddress().getHostAddress() + "\n"
                + "PORT> "+ packet.getPort() + "\n"
                + "SOCKET_ADDRESS> "+ packet.getSocketAddress() + "\n"
                + "DATA_LENGHT> "+ packet.getData().length + "\n";
            System.out.println(debug);
            // End DEBUG
            
            // Decode and check package
            String content = new String(packet.getData(), 0, packet.getLength()).trim();
            String[] data = content.split("\\|");
            
            System.out.println("[DATA] " + content);
            if (!data[0].equals("POLLING_REPLY")) {
                System.out.println("[DATA] Pacote descartado.");
                continue;
            }

            // Creating SlaveProcessInfo
            Integer pid = Integer.parseInt(data[1]);
            long delta = Long.parseLong(data[2]);
            ProcessInfo slave = new ProcessInfo(pid, packet.getAddress(), packet.getPort());
            slave.setDelta(delta);
            slave.setRtt(ChronoUnit.MILLIS.between(scT1, scT4));
            System.out.printf("[DATA] Recebido o tempo do processo escravo %s\n", slave.toString());
                
            // T4 info
            System.out.printf("[INFO] [T4_TIME] = %s relativo ao escravo PID=%s\n", this.scT4, slave.getPid());
            
            // Delta T info (T4-T1) - total RTT
            System.out.printf("[INFO] [T4 - T1] [SC_RTT] = %d do escravo PID=%s\n", slave.getRtt(), slave.getPid());

            processes.add(slave);
            result = true;
        }

        // DEBUG
        System.out.println("[DEBUG] Fim da janela de receive.");
        return result;
    }

    private long calculateAverageTime() {
        List<Long> processDeltaTimes = this.processes.stream()
            .map(ProcessInfo::getDelta)
            .sorted()
            .collect(Collectors.toList());

        long medianTime;

        if ((processDeltaTimes.size() % 2 == 0)) {
            int middleIndex = processDeltaTimes.size() / 2;
            long middleLeft = processDeltaTimes.get(middleIndex - 1);
            long middleRight = processDeltaTimes.get(middleIndex);
            medianTime = (middleLeft + middleRight) / 2;
        } else {
            medianTime = processDeltaTimes.get((processDeltaTimes.size() / 2));
        }

        List<Long> trimmedProcessDeltaTimes = processDeltaTimes.stream()
                .filter(delta -> Math.abs(delta - medianTime) <= DEFAULT_TRIM_TIME)
                .collect(Collectors.toList());

        double average = trimmedProcessDeltaTimes.stream()
                .mapToDouble(delta -> delta)
                .average()
                .orElse(0.0);

        return (long) average;
    }

    private void adjustProcessesTimes(long averageTime) {
        LocalDateTime oldTime = masterSoftwareClock.getInstantTime();
        masterSoftwareClock.updateClock(averageTime);
        long newTimeDelta = ChronoUnit.MILLIS.between(oldTime, masterSoftwareClock.getInstantTime());
        
        System.out.println("[NEW CLOCK TIME] " + masterSoftwareClock.getInstantTime() + " [DELTA] " + newTimeDelta);

        processes.stream()
            .filter(process -> !process.equals(masterProcessInfo))
            .forEach(process -> {
                // TODO: TESTAR CALCULO
                long newTime = process.getAdjustedDelta(averageTime);

                String newTimeMessage = String.join("|",
                    "NEW_TIME",
                    String.valueOf(newTime));

                this.responseTimesBuffer = newTimeMessage.getBytes();
                
                this.packet = new DatagramPacket(this.responseTimesBuffer, this.responseTimesBuffer.length, process.getIpAddress(), process.getPort());
                                
                // DEBUG package
                String debug = "\n[DEBUG PACKAGE]\n"
                    + "STRING NEW_TIME> " + newTimeMessage + "\n"
                    + "BUFFER_LENGTH> " + responseTimesBuffer.length + "\n"
                    + "PACKET_LENGHT> " + this.packet.getLength() + "\n"
                    + "ADDRESS:PORT> " + this.packet.getSocketAddress().toString() + "\n"
                    + "CONTENT_STRING> " + new String(this.packet.getData(), 0, this.packet.getLength()) + "\n";
                System.out.println(debug);
                // End DEBUG

                try {
                    // DEBUG
                    System.out.printf("[DEBUG] Simulando a latencia da rede [aDelay = %dms]\n", masterProcessInfo.getADelay());
                    // Waiting for Master aDelay lapse
                    Thread.sleep(this.masterProcessInfo.getADelay());

                    this.socket.send(this.packet);
                    System.out.printf("[DATA] Envio do tempo delta=%s para o processo [%s:%s - PID: %d]\n", newTime, process.getIpAddress(), process.getPort(), process.getPid());
                } catch (IOException e) {
                    System.out.printf("[ERRO] Nao foi possivel enviar o pacote de correcao do relogio para o processo [%s:%s - PID: %d]\n", process.getIpAddress(), process.getPort(), process.getPid());
                } catch (InterruptedException e) {
                    System.err.println(this.getClass() + "[ERRO] Erro no Thread.sleep().");
                }
            });
    }
}