package com.java;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;

public class MasterApplication {

    private static final long DEFAULT_POLLING_INTERVAL = 30000; // 30s
    private static final long DEFAULT_DELAY = 1000; // 1s para inicializacao do relogio
    private static final String DEFAULT_GROUP_IP_ADDRESS = "230.0.0.0";
    private static final int DEFAULT_GROUP_PORT = 4321;
    private static Timer timer = new Timer();

    public static void main(String[] args) { // pid, ipAddress, port, timeOffset, pTime, time
        if(args.length != 6) {
            printHelpMessage();
            System.exit(1);
        }

        ProcessInfo master = null; // startTime + processingTime
        SoftwareClock softwareClock = null;
        // Setting up Master
        try {
            int pid = Integer.parseInt(args[0]);
            InetAddress address = InetAddress.getByName(args[1]);
            int port = Integer.parseInt(args[2]);
            long timeOffset = Long.parseLong(args[3]);
            long pTime = Long.parseLong(args[4]);
            long aDelay = Long.parseLong(args[5]);
            // softwareClock = new SoftwareClock(timeOffset);
            // softwareClock = new SoftwareClockMultithread(timeOffset);
            softwareClock = new SoftwareClockMultithreadRTC(timeOffset);
            softwareClock.start();
            master = new ProcessInfo(pid, address, port, pTime, aDelay);
            System.out.println("[INFO] Processo mestre inicializado");
        } catch (UnknownHostException | NumberFormatException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        // Joining Multicast group
        MulticastSocket groupSocket = null;
        InetAddress groupIP = null;
        InetSocketAddress groupAddress;
        
        try {
            groupIP = InetAddress.getByName(DEFAULT_GROUP_IP_ADDRESS);
            groupAddress = new InetSocketAddress(groupIP, DEFAULT_GROUP_PORT);
            groupSocket = new MulticastSocket(DEFAULT_GROUP_PORT);
            //groupSocket = new MulticastSocket(groupAddress);
            groupSocket.joinGroup(groupIP);
            //System.out.printf(("[GROUP] Entrei no group (%s:%s)\n", groupSocket.getInetAddress().toString(), groupSocket.getPort());
            System.out.printf("[GROUP] Entrei no group (%s)\n", groupIP.toString());

            // Run polling thread
            timer.schedule(new MasterClockSyncTask(master, softwareClock, groupSocket, groupAddress), DEFAULT_DELAY, DEFAULT_POLLING_INTERVAL);
        } catch (UnknownHostException e) {
            closeSocket(groupSocket, groupIP);
            e.printStackTrace();
        } catch (SocketException e) {
            closeSocket(groupSocket, groupIP);
            e.printStackTrace();
        } catch (IOException e) {
            closeSocket(groupSocket, groupIP);
            e.printStackTrace();
        }
    }

    private static void closeSocket(MulticastSocket groupSocket, InetAddress groupIP) {
        if (groupSocket != null) {
            if (groupIP != null)
                try {
                    groupSocket.leaveGroup(groupIP);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            groupSocket.close();
        }
    }

    public static void printHelpMessage() {
        System.out.println("\n\tUsage: java MasterApplication <pid> <ipAddress> <port> <timeOffset> <pTime> <aDelay>\n");
    }
}
