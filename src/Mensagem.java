import java.io.Serializable;

public class Mensagem implements Serializable {

	static final long serialVersionUID = 1L;
	int processoOrigem;
	public int[] relogioOrigem;

	public Mensagem(int processoOrigem, int[] relogioOrigem) {
		super();
		this.processoOrigem = processoOrigem;
		this.relogioOrigem = relogioOrigem;
	}

}
