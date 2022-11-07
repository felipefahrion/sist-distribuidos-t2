import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.DatagramSocket;

public class Lamport {
    private Process myProcess;
    private int numberOfMyProcess;
    private int localTimeStamp = 0;
    private int counter = 0;
    private DatagramSocket socket;
    ReadConfigFile readFile = new ReadConfigFile();

    public Lamport(int numberOfMyProcess) {
        this.numberOfMyProcess = numberOfMyProcess;
        readFile.readConfigAddToList();
    }

    Thread createEvent = new Thread(new Runnable() {
        @Override
        public void run() {
            try {

                while (counter < 100) {
                    int random = (int) (Math.random() * (1000 - 500 + 1) + 500);
                    Thread.sleep(random);
                    chooseTypeOfEvent();
                    counter++;
                }
                System.exit(1);

            } catch (Exception e) {
            }
        }
    });

    public void calcLamport(String[] data) {
        int receivedTimeStamp = Integer.parseInt(data[2]);
        int receivedId = Integer.parseInt(data[1]);
        localTimeStamp = Math.max(localTimeStamp, receivedTimeStamp);
        localTimeStamp += 1;
        String concatLocalTimeStamp = "" + localTimeStamp + myProcess.id;
        System.out.println(System.currentTimeMillis() + " " + myProcess.id + " " + concatLocalTimeStamp + " r " + receivedId
                + " " + receivedTimeStamp);
        // System.out.println("RECEIVE EVENT");
    }

    Thread listener = new Thread(new Runnable() {
        @Override
        public void run() {
            try {

                DatagramPacket packet;
                byte[] data = new byte[1024];
                while (true) {
                    try {
                        packet = new DatagramPacket(data, data.length);
                        socket.setSoTimeout(500);
                        socket.receive(packet);
                        String response = new String(packet.getData(), 0, packet.getLength());
                        String[] msgAndClock = response.split(":");
                        calcLamport(msgAndClock);
                    } catch (Exception e) {
                    }
                }

            } catch (Exception e) {
            }
        }
    });

    public void chooseTypeOfEvent() {
        int random = 1 + (int) (Math.random() * ((100 - 1) + 1));
        double chance = myProcess.chance * 100;
        if (random > chance) {
            localEvent();
        } else {
            try {
                sendEvent();
            } catch (Exception e) {

            }
        }

    }

    public void localEvent() {
        localTimeStamp += 1;
        String concatLocalTimeStamp = localTimeStamp + myProcess.id + "";
        System.out.println(System.currentTimeMillis() + " " + myProcess.id + " " + concatLocalTimeStamp + " l");
        // System.out.println("LOCAL EVENT");
        return;

    }

    public void sendEvent() {
        try {
            byte[] sendMessageAndLocalClock = new byte[1024];
            String messageAndClock = "Olá:" + myProcess.id + ":" + localTimeStamp;
            sendMessageAndLocalClock = messageAndClock.getBytes();
            Process otherProcess = readFile.getRandomProcess(numberOfMyProcess);
            InetAddress host = InetAddress.getByName(otherProcess.host);
            DatagramPacket sendData = new DatagramPacket(sendMessageAndLocalClock, sendMessageAndLocalClock.length,
                    host, otherProcess.port);

            socket.send(sendData);
            String concatLocalTimeStamp = localTimeStamp + myProcess.id + "";
            System.out.println(System.currentTimeMillis() + " " + myProcess.id + " " + concatLocalTimeStamp + " "
                  + " s "  + otherProcess.id);

        } catch (Exception e) {
            System.out.println("Erro \n\n\n");
            e.printStackTrace();
        }

        // System.out.println("SEND EVENT");
        return;

    }

    public void getMyProcess() {
        myProcess = readFile.getIndexListOfProcess(numberOfMyProcess);
        System.out.println("My PROCEES ID:  " + myProcess.id);
        try {
            socket = new DatagramSocket(myProcess.port);
        } catch (SocketException e) {
            System.out.println("Unable to start Server");
            System.exit(1);
        }
        listener.start();
        createEvent.start();
        while (true) {
            listener.run();
            createEvent.run();
        }
    }

    public void start() {
        System.out.println(
                "---------------------------------------------------------------------------------------------------");
        this.getMyProcess();
    }

}
