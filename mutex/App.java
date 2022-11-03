import java.io.*; 
import java.net.*; 

class App {    
	
	public static void main(String args[]) throws Exception{
		if (args.length != 5) {
			System.out.println("Usage: Client <\"name\"> <lock server ip> <lock port> <resource ip> <resource port>");
			
			return;
		}


		/* Permite leitura de dados do teclado */
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		/* DatagramSocket implementa socket UDP */
		DatagramSocket clientSocket = new DatagramSocket();
		
		/* Faz a tradução do nome do host para o seu IP (DNS). Também pode-se entrar com o IP no formato literal, exemplo 127.0.0.1.*/
		InetAddress lockIPAddress = InetAddress.getByName(args[1]);
		int lockPort = Integer.parseInt(args[2]);
		InetAddress resIPAddress = InetAddress.getByName(args[3]);
		int resPort = Integer.parseInt(args[4]);

		while (true) {
			/* Buffer reservado para envio dos dados */
			byte[] sendData = new byte[1024];
			
			/* operação lock() */
			String sentence = "lock()";
			sendData = sentence.getBytes();

			System.out.println(lockIPAddress.getHostAddress() + ":" + lockPort);
			/* Cria um novo pacote UDP, configurando com o IP e porta de destino.*/
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, lockIPAddress, lockPort);
			
			/* Envia o pacote UDP para obter o lock */
			clientSocket.send(sendPacket);
			
			/* Buffer para recepção de dados */
			byte[] receiveData = new byte[1024];

			/* DatagramPacket representa um pacote UDP, repare que ele esta associado ao buffer reservado para recepção */
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			/* Aguarda o recebimento de uma mensagem. O servidor fica aguardando neste ponto 
			 * até que uma mensagem seja recebida, ou desiste */
			clientSocket.setSoTimeout(1000);
			try {
				clientSocket.receive(receivePacket);
			} catch (Exception e) {
				System.out.println("lock() timed out!");
				continue;
			}
			
			/* Converte o buffer recebido para um objeto string. Isso é válido porque estamos aguardando mensagens textuais. */
			sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
			
			if (sentence.equals("grant()")) {
				/* Pega o IP do remetente */
				lockIPAddress = receivePacket.getAddress();
				
				/* Pega o número de porta do remetente */
				int port = receivePacket.getPort();
				
				/* Exibe, IP:port => msg */
				System.out.println(lockIPAddress.getHostAddress() + ":" + port + " => " + sentence);
				
				for (int i = 0; i < 50; i++) {
					/* envia primeira mensagem */
					sentence = "write(): " + args[0];
					sendData = sentence.getBytes();

					/* Cria um novo pacote UDP, configurando com o IP e porta de destino.*/
					sendPacket = new DatagramPacket(sendData, sendData.length, resIPAddress, resPort);
					
					/* Envia o pacote UDP */
					clientSocket.send(sendPacket);
					
					/* dorme um pouco */
					try {
						Thread.sleep(10);
					} catch (Exception e) {
					}
					
					/* envia segunda mensagem */
					sentence = "write(): " + args[0] + " oper " + Integer.toString(i);
					sendData = sentence.getBytes();

					/* Cria um novo pacote UDP, configurando com o IP e porta de destino.*/
					sendPacket = new DatagramPacket(sendData, sendData.length, resIPAddress, resPort);
					
					/* Envia o pacote UDP */
					clientSocket.send(sendPacket);
				}
				
				/* operação unlock() */
				sentence = "unlock()";
				sendData = sentence.getBytes();

				/* Cria um novo pacote UDP, configurando com o IP e porta de destino.*/
				sendPacket = new DatagramPacket(sendData, sendData.length, lockIPAddress, lockPort);
				
				/* Envia o pacote UDP para obter o lock */
				clientSocket.send(sendPacket);
			}
			/* dorme um pouco */
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
			}
		}
		
//		clientSocket.close();
	}
}
