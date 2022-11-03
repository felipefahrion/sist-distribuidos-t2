

public enum TipoEvento {
	LOCAL("l", "Local"), ENVIO("s", "Envio"), RECEBIMENTO("r", "Recebimento");

	public final String valor;
	public final String descricao;

	private TipoEvento(String valor, String descricao) {
		this.valor = valor;
		this.descricao = descricao;
	}

	public String getValue() {
		return valor;
	}
}

