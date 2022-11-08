import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Inicia {

	static int procProntos = 0;
	static int TEMPO = 0;
	static int NICK = 1;
	static int MSG = 2;
	static int portaMcast = 4444;
	static String processoId = "";

	/**
	 * Multicast que aguarda os 3 processos
	 * enviarem a mensagem avisando que estão prontos para iniciar os eventos.
	 */
	private static void aguardaTodosProcessos(String[] args) throws IOException, UnknownHostException {
		processoId = args[0];
		MulticastSocket socket = new MulticastSocket(portaMcast);
		InetAddress grupo = InetAddress.getByName("224.0.0.1");
		socket.joinGroup(grupo);
		
		Scanner scanner = new Scanner(System.in);
		System.out.println("Digite 'PRONTO' para iniciar os eventos");

		while (procProntos < 3) {
			try {

				byte[] entrada = new byte[1024];
				DatagramPacket pacote = new DatagramPacket(entrada, entrada.length);
				socket.setSoTimeout(100);
				socket.receive(pacote);
				String recebido = new String(pacote.getData(), 0, pacote.getLength());

				String vars[] = recebido.split("\\s");
				// vars[] -> [1601515714940, usuario, mensagem]

				if (vars[MSG].equalsIgnoreCase("PRONTO")) {
					System.out.println("Processo "+ vars[NICK] + " Já esta preparado!");
					procProntos++;
				}

			} catch (IOException e) {
				// Timeout....
			}
			
			if (System.in.available() > 0) {				
				String mens = scanner.nextLine();
				long time = System.currentTimeMillis();
				byte[] saida = new byte[1024];
				saida = (Long.toString(time) + " " + processoId + " " + mens).getBytes();
				DatagramPacket pacote = new DatagramPacket(saida, saida.length, grupo, portaMcast);
				socket.send(pacote);
			}
		}
		
		socket.leaveGroup(grupo);
		socket.close();
	}
	
	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.out.println("Usage: java Main <numero processo>\n");
			return;
		}

	//	aguardaTodosProcessos(args);
		Thread.sleep(1000l);
		// starta o processo...
		new Nodo(Integer.parseInt(args[0]));
	}

}
