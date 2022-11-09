package com.java;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SlaveApplication {

    private static final String DEFAULT_GROUP_IP_ADDRESS = "230.0.0.0";
    private static final int DEFAULT_GROUP_PORT = 4321;

    public static void main(String[] args) {
        if(args.length != 6) {
            printHelpMessage();
            System.exit(1);
        }

        ProcessInfo slave = null;
        SoftwareClock softwareClock = null;
        // Setting up Slave
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
            slave = new ProcessInfo(pid, address, port, pTime, aDelay);
            System.out.println("[INFO] Processo slave inicializado");
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
            groupSocket = new MulticastSocket(DEFAULT_GROUP_PORT);
            groupAddress = new InetSocketAddress(groupIP, DEFAULT_GROUP_PORT);
            groupSocket.joinGroup(groupIP);
            System.out.printf("[GROUP] Entrei no group (%s)\n", groupIP.toString());

            SlaveClockSync slaveClockSync = new SlaveClockSync(slave, softwareClock, groupSocket, groupAddress);
            slaveClockSync.start();
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
                    System.out.println("[ERROR] Leave Group");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            groupSocket.close();
            System.out.println("[ERROR] Close Group Socket");
        }
    }

    public static void printHelpMessage() {
        System.out.println("\n\tUsage: java ServerApplication <pid> <ipAddress> <port> <timeOffset> <pTime> <aDeplay>\n");
    }
}