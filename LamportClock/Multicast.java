import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Scanner;

public class Multicast {

    private static Boolean creator = false;
    private static Boolean receiver = false;
    private static int port;
    private static InetAddress group;
    private static Boolean condition = true;
    private static int numberOfMyProcess;

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length < 1) {
            System.out.println("Você precisa indicar qual a linha do arquivo de configuração é o seu processo");
            System.exit('1');
        }        
        numberOfMyProcess = Integer.parseInt(args[0]);        
        port = 3001;
        MulticastSocket socket = new MulticastSocket(port);
        socket.setSoTimeout(500);
        group = InetAddress.getByName("224.0.0.0");
        socket.joinGroup(group);
        String nick = "";
        if (args.length < 2) {
            nick = "";
        }
        else {
            nick = args[1];
        }

        if (nick.equalsIgnoreCase("server")) {
            creator = true;
        }

        while (condition) {
            if (creator)
                creator(socket);
            else
                receiver(socket);
        }

        socket.leaveGroup(group);
        socket.close();
    }

    public static void creator(MulticastSocket socket) throws InterruptedException {
        ReadConfigFile configFile = new ReadConfigFile();
        configFile.readConfigAddToList();        
        System.out.println("Estou pronto, vou esperar os outros");
        System.out.println("Digite 'verify' após inicializar todos os processos para a inicialização multicast");
        Scanner in = new Scanner(System.in);
        String verifyMessage = in.nextLine();
        try {
            int i = 0;
            if (verifyMessage.equalsIgnoreCase("verify")) {
                byte[] verify = new byte[1024];
                verify = verifyMessage.getBytes();
                DatagramPacket sendVerify = new DatagramPacket(verify, verify.length, group, port);
                socket.send(sendVerify);
            }
            while (i < configFile.getNOfProcess() - 1) {
                byte[] receivedOk = new byte[1024];
                DatagramPacket processReady = new DatagramPacket(receivedOk, receivedOk.length);
                try {
                    socket.receive(processReady);
                    i++;
                } catch (Exception e) {
                    condition = false;
                    break;
                }
                Thread.sleep(2000);
            }
            byte[] startLocalsClock = new byte[1024];
            startLocalsClock = "start".getBytes();
            DatagramPacket sendStartCommand = new DatagramPacket(startLocalsClock, startLocalsClock.length, group,
                    port);
            socket.send(sendStartCommand);
            condition = false;
            System.out.println("Iniciando relógio...");
            Lamport localClock = new Lamport(numberOfMyProcess);
            localClock.start();

        } catch (Exception e) {

        }
        in.close();
    }

    public static void receiver(MulticastSocket socket) throws InterruptedException {
        ReadConfigFile configFile = new ReadConfigFile();
        configFile.readConfigAddToList();        
        try {
            if (receiver) {
                System.out.println("Estou pronto");
                byte[] sendOk = new byte[1024];
                sendOk = "ready".getBytes();
                DatagramPacket sendReady = new DatagramPacket(sendOk, sendOk.length, group, port);
                socket.send(sendReady);
                receiver = false;
            }

            byte[] response = new byte[1024];
            DatagramPacket res = new DatagramPacket(response, response.length);
            socket.receive(res);

            String responseData = new String(res.getData(), 0, res.getLength());

            if (responseData.equals("verify")) {
                receiver = true;
            }

            if (responseData.equals("start")) {
                System.out.println("Iniciando relógio...");
                Lamport localClock = new Lamport(numberOfMyProcess);
                localClock.start();

            }

        } catch (Exception e) {

        }

    }

}
