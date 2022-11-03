import java.net.InetAddress;
import java.util.ArrayList;

public class Processo {
	public int id;
	public InetAddress host;
	public Integer port;
	public Double chance;
	public ArrayList<Evento> eventos = new ArrayList<>();
	public int[] relogios;

	public Processo(int id, InetAddress host, Integer port, Double chance, int numRelogios) {
		super();
		this.id = id;
		this.host = host;
		this.port = port;
		this.chance = chance;
		relogios = new int[numRelogios];
	}
}
