import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * App
 */
public class App {

    public static void main(String[] args) throws FileNotFoundException {

        List<Process>processes = new ArrayList<>(); 


        float random = (float)( 0.1 + (Math.random() * 0.9));

        int[] clock = new int[processes.size()];
        Arrays.fill(clock, 0);

        String processId = args[1];

        start_multicast();

    }

    public static void start_multicast() {
        MulticastSocket socket = new MulticastSocket(5000);
		InetAddress grupo = InetAddress.getByName("230.0.0.1");
		socket.joinGroup(grupo);
		byte[] entrada = new byte[256];
		DatagramPacket pacote = new DatagramPacket(entrada,entrada.length);
		socket.receive(pacote);
		String recebido = new String(pacote.getData(),0,pacote.getLength());
		System.out.println("Received: "+recebido);
		socket.leaveGroup(grupo);
		socket.close();
    }

    public static void initialize() {
        
        File file = new File("config_sample.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;
        while ((st = br.readLine()) != null){
            String[] processLine = st.split(" ");

            if(processLine[0].equals(processId)){
                
            }

        }


 
            // Print the string
            System.out.println(st);
    }
} 