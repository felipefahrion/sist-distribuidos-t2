import java.io.*; 
import java.net.*; 

class LockServer {    
	public static void main(String args[]) throws Exception{
		/* DatagramSocket implementa socket UDP */
		DatagramSocket serverSocket = new DatagramSocket(9876);
		
		/* Servidor fica recebendo mensagens por tempo indeterminado */
		while (true) {
			/* Buffer para recepção de dados */
			byte[] receiveData = new byte[1024];

			/* DatagramPacket representa um pacote UDP, repare que ele esta associado ao buffer reservado para recepção */
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
			/* Aguarda o recebimento de uma mensagem. O servidor fica aguardando neste ponto 
			 * até que uma mensagem seja recebida */
			serverSocket.setSoTimeout(0);
			serverSocket.receive(receivePacket);
			
			/* Converte o buffer recebido para um objeto string. Isso é válido porque estamos aguardando mensagens textuais. */
			String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
			
			System.out.println(sentence);
		
			if (sentence.equals("lock()")) {
				
				/* Buffer reservado para envio dos dados */
				byte[] sendData = new byte[1024];
				
				/* Pega o IP do remetente */
				InetAddress IPAddress = receivePacket.getAddress();
				
				/* Pega o número de porta do remetente */
				int port = receivePacket.getPort();
				
				/* Exibe, IP:port => msg */
				System.out.println(IPAddress.getHostAddress() + ":" + port + " => " + sentence);
				
				sentence = "grant()";
				sendData = sentence.getBytes();
				
				/* Cria um novo pacote UDP, configurando com o IP e porta de destino.*/
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				
				try {
					/* Envia o pacote UDP */
					serverSocket.send(sendPacket);
					System.out.println("Response sent. Waiting for unlock()...");
				} catch (Exception e) {
				}
				
				while (true) {
					/* DatagramPacket representa um pacote UDP, repare que ele esta associado ao buffer reservado para recepção */
					receivePacket = new DatagramPacket(receiveData, receiveData.length);
				
					/* Aguarda o recebimento de uma mensagem. O servidor fica aguardando neste ponto 
					* até que uma mensagem seja recebida */
					serverSocket.setSoTimeout(1000);
					try {
						serverSocket.receive(receivePacket);
					
					
						/* Converte o buffer recebido para um objeto string. Isso é válido porque estamos aguardando mensagens textuais. */
						sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
					
						if (sentence.equals("unlock()")) {
							System.out.println("unlock()...");
							break;
						}
					} catch (Exception e) {
						break;
					}
				}
			}
		}
	}
}
