import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.util.Scanner;

public class Data {

	private static DatagramSocket clientSocket;
	private static Integer timeout = 3000;
	static int tentativas = 0;

	/**
	 * Inicia o socket atribuindo um limite de tempo (timeout).
	 */
	protected static void iniciarSocket(int porta) {
		try {
			clientSocket = new DatagramSocket(porta);
			clientSocket.setSoTimeout(timeout);
		} catch (SocketException e) {
			System.out.println("Erro ao iniciar socket");
			e.printStackTrace();
		}
	}

	/**
	 * Fecha socket.
	 */
	protected static void fecharSocket() {
		clientSocket.close();
	}

	/**
	 * Método que envia a mensagem para o client destino.
	 * 
	 * @param procSelecionado
	 */
	protected static void enviarMensagem(Mensagem mensagem, Processo procSelecionado) {
		try {
			clientSocket.connect(procSelecionado.host, procSelecionado.port);
			byte[] serialized = mensagemToByteArray(mensagem);
			DatagramPacket sendPacket = new DatagramPacket(serialized, serialized.length);
			clientSocket.send(sendPacket);
			clientSocket.disconnect();
			// receberAck();
		} catch (PortUnreachableException e) {
			System.out.println("Erro ao enviar mensagem. Processo Já terminou a execução");
			System.out.println("Encerrado Processo Local");
			comandoParaFechar();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Erro ao enviar mensagem.");
			System.out.println("Encerrado Processo");
			comandoParaFechar();
		}
	}

	private static void enviarAck(DatagramPacket receiveDatagram) throws Exception {
		try {
			byte[] serialized = new byte[] { 1, 0, 0 };
			clientSocket.connect(receiveDatagram.getAddress(), receiveDatagram.getPort());
			DatagramPacket sendPacket = new DatagramPacket(serialized, serialized.length);
			clientSocket.send(sendPacket);
			clientSocket.disconnect();
		} catch (Exception e) {
			throw new Exception("Erro, Ack Não enviado.");
		}
	}

	private static void receberAck() throws Exception {
		try {
			tentativas = 0;
			byte[] receiveData = new byte[1024];
			DatagramPacket receiveDatagram = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receiveDatagram);
			byte[] recBytes = receiveDatagram.getData();
			// ack Recebido...
			clientSocket.disconnect();
		} catch (Exception e) {
			tentativas++;
			if (tentativas == 3) {
				System.out.println("Erro, Ack Não recebido - timeout.");
				System.out.println("Erro ao enviar mensagem (Processo destino já terminou a execução.)");
				System.out.println("Encerrado Processo");
				comandoParaFechar();
			} else {
				receberAck();
			}
		}
	}

	/**
	 * Converte o objeto mensagem para um byteArray
	 */
	private static byte[] mensagemToByteArray(Mensagem mensagem) {
		try {
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			ObjectOutput oo = new ObjectOutputStream(bStream);
			oo.writeObject(mensagem);
			oo.close();
			return bStream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected static Mensagem receberMensagem() {
		try {
			byte[] receiveData = new byte[1024];
			DatagramPacket receiveDatagram = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receiveDatagram);
			// enviarAck(receiveDatagram);
			byte[] recBytes = receiveDatagram.getData();
			Mensagem mensagem = byteArrayToMensagem(recBytes);
			return mensagem;
		} catch (IOException e) {
			try {
				Thread.sleep(100l);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return receberMensagem();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Erro ao receber mensagem");
			System.out.println("Encerrado Processo");
			comandoParaFechar();
			return null;
		}
	}

	/**
	 * Converte de byteArray para o objeto mensagem.
	 */
	private static synchronized Mensagem byteArrayToMensagem(byte[] mensagem) {
		try {
			ByteArrayInputStream btr = new ByteArrayInputStream(mensagem);
			ObjectInputStream iStream = new ObjectInputStream(btr);
			Mensagem mensagemObj = (Mensagem) iStream.readObject();
			iStream.close();
			btr.close();
			return mensagemObj;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected static void comandoParaFechar() {
		fecharSocket();
		System.out.println("===Fim Eventos===");
		System.out.println("===Digite qualquer tecla para sair===");
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		System.exit(0);
	}

}
