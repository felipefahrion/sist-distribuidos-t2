
/**
 * Evento local: m i c l
 * Envio de mensagem: m i c s d
 * Recebimento de mensagem: m i c r s t 
 * m = tempo nodo do local (ms) 
 * i = ID nodo local 
 * c = valor do relogio (enviado) + id (concatenado) 
 * d = ID do nodo destino da msg s = envio de  mensagem (envio de msg) ou ID do nodo remetente da mensagem (quando recebimento) 
 * r = recebimento de mensagem 
 * l = mensagem local 
 * t = valor do relógio lógico recebido com a mensagem
 */
public class Evento {

	public long m;
	public int i;
	public int c;
	public int d;
	public TipoEvento tipo;
	public int t;
	public int s;

	// Evento Local
	/**
	 * Evento local: m i c l, onde m é o tempo do computador local em milissegundos,
	 * i é o ID do nodo local e c é o valor do relógio lógico local, concatenado com
	 * o ID do nodo;
	 */
	public Evento(long m, int i, int c) {
		this.m = m;
		this.i = i;
		this.c = c;
		this.tipo = TipoEvento.LOCAL;
	}

	// Evento envio de mensagem
	/**
	 * Envio de mensagem: m i c s d, onde m é o tempo do computador local em
	 * milissegundos, i é o ID do nodo local, c é o valor do relógio lógico enviado
	 * (relógio concatenado com o ID), d é o ID do nodo destinatário da mensagem;
	 */
	public Evento(long m, int i, int c, int d) {
		this.m = m;
		this.i = i;
		this.c = c;
		this.tipo = TipoEvento.ENVIO;
		this.d = d;
	}

	// Evento recebimento de mensagem
	/**
	 * Recebimento de mensagem: m i c r s t, onde onde m é o tempo do computador
	 * local em milissegundos, i é o ID do nodo local, c é o valor do relógio lógico
	 * depois do recebimento da mensagem, s é ID do nodo remetente da mensagem, t é
	 * o valor do relógio lógico recebido com a mensagem.
	 * 
	 */
	public Evento(long m, int i, int c, int s, int t) {
		this.m = m;
		this.i = i;
		this.c = c;
		this.tipo = TipoEvento.RECEBIMENTO;
		this.s = s;
		this.t = t;
	}

	@Override
	public String toString() {
		switch (tipo) {
		case LOCAL:
			return m + " " + i + " " + c + "-" + i + " " + tipo.getValue();
		case ENVIO:
			return m + " " + i + " " + c + "-" + i + " " + tipo.getValue() + " " + d;
		case RECEBIMENTO:
			return m + " " + i + " " + c + "-" + i + " " + tipo.getValue() + " " + s + " " + t;
		default:
			return "Tipo de evento não informado.";
		}
	}

}
