import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class App {

    private static String myIp;
    private static String myProcessId;
    private static String myChance;
    private static String myEvents;
    private static String myMinDelay;
    private static String myMaxDelay;
    private static List<String> otherHosts;
    private static int[] clock;
    private static float rndEvetChance;
    private static String fileName;

    /**
     * @param args
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException {

        set_up(args);     
        
        try {
            initialize(fileName);
        } catch (Exception e) {
            System.out.println(e); 
        }
        
        start_local_clock();
        
        // debug_method();


        // start_multicast();
    }

    public static void start_multicast() throws IOException {


    }

    public static void send_multicast() throws IOException {
		byte[] messabeByte = new byte[256];

		String mens = "Alô, mundo!";

		messabeByte = mens.getBytes();
        
		DatagramSocket socket = new DatagramSocket();
		InetAddress multicastGroup = InetAddress.getByName("230.0.0.1");
		DatagramPacket sentPackt = new DatagramPacket(messabeByte, messabeByte.length, multicastGroup,5000);
        
		socket.send(sentPackt);
		socket.close();
    }

    public static void receive_multcast() throws IOException {
        MulticastSocket multicastSocket = new MulticastSocket(5000);
		InetAddress multicastGroup = InetAddress.getByName("230.0.0.1");
		multicastSocket.joinGroup(multicastGroup);

		byte[] messageByte = new byte[256];

		DatagramPacket pkct = new DatagramPacket(messageByte, messageByte.length);
		multicastSocket.receive(pkct);

		String receivedString = new String(pkct.getData(),0,pkct.getLength());
		System.out.println("Received: " + receivedString);

		multicastSocket.leaveGroup(multicastGroup);
        
		multicastSocket.close();
    }

    public static void start_local_clock() {
        int totalProcess = otherHosts.size() + 1; //size of other process + my host
        
        clock = new int[totalProcess];
        Arrays.fill(clock, 0);
    }

    public static void set_up(String[] args) {
        fileName = args[0];
        myProcessId = args[1];
        otherHosts = new ArrayList<>();

        rndEvetChance = (float)( 0.1 + (Math.random() * 0.9));
    }

    public static void initialize(String fileName) throws IOException {
        
        File file = new File(fileName);
        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;

        br.readLine(); //skip firts line

        while ((st = br.readLine()) != null){
            String[] processLine = st.split(" ");

            if(processLine[0].equals(myProcessId)){
                myIp = processLine[1];
                myProcessId = processLine[2];
                myChance = processLine[3];
                myEvents = processLine[4];
                myMinDelay = processLine[5];
                myMaxDelay = processLine[6];

            } else {
                otherHosts.add(processLine[0]);
            }
        }

        br.close();    
    }

    public static void debug_method() {
        System.out.println("print other hosts");
        for (String i : otherHosts) {
            System.out.println(i);
        }

        System.out.println();

        System.out.println("clock");
        for (int i : clock) {
            System.out.print(i);
        }
    }
} 