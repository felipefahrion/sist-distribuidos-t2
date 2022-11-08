import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Nodo extends Data {

	public ArrayList<Processo> processos = new ArrayList<>();
	public int processoId;
	public int numeroEventos = 100;
	public int numProcessos;
	public static Thread tRecebimento = null;
	public boolean recebendo, enviando;

	public Nodo(int processo) throws Exception {
		this.processoId = processo;
		carregaProcessos();
		super.iniciarSocket(processos.get(processoId).port);
		iniciaRecebimentoDeMensagens();
		executaNodo();
	}

	private void executaNodo() throws Exception {
		Random rand = new Random();
		while (processos.get(processoId).eventos.size() < numeroEventos) {
			Thread.sleep((long) rand.nextInt(1000 - (500 - 1)) + 500); // evnts entre 0.5 e 1 seg
			executaEvento();
		}
		tRecebimento.stop();
		super.fecharSocket();
		System.out.println("===Fim Eventos===");
		System.out.println("===Digite qualquer tecla para sair===");
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
	}

	private void executaEvento() throws Exception {
		if (Math.random() < processos.get(processoId).chance) { // prob de chance
//			System.out.println("Enviando mensagem");
			executaEnvioDeMensagem();
		} else {
//			System.out.println("Evento local!");
			executaEventoLocal();
		}
	}

	private void executaEventoLocal() throws Exception {
		Processo proc = processos.get(processoId);
		this.adicionaEvento(proc, TipoEvento.LOCAL, null, null, null, null);
	}

	private synchronized void adicionaEvento(Processo proc, TipoEvento tipo, Integer vrRlgEnviado, Integer idNodoRemetente,
			Integer vlrRlgDaMensagem, Integer idNodoDestino) throws Exception {
		Evento novoEvento;
		Evento eventoAnterior;
		if (proc.eventos.isEmpty()) { // primeiro evento
			switch (tipo) {
			case LOCAL:
				novoEvento = new Evento(System.currentTimeMillis(), processoId, 1);
				proc.relogios[processoId] = proc.relogios[processoId] + 1;
				break;
			case ENVIO:
				novoEvento = new Evento(System.currentTimeMillis(), processoId, 1, idNodoDestino);
				proc.relogios[processoId] = proc.relogios[processoId] + 1;
				break;
			case RECEBIMENTO:
				novoEvento = new Evento(System.currentTimeMillis(), processoId, this.maxRelogios(proc.relogios[processoId], vlrRlgDaMensagem), idNodoRemetente, vlrRlgDaMensagem);
				proc.relogios[processoId] = this.maxRelogios(proc.relogios[processoId], vlrRlgDaMensagem);
				break;
			default:
				throw new Exception("Tipo do Evento não definido");
			}
		} else { // existem eventos anteriores (incrementar)
			switch (tipo) {
			case LOCAL:
				eventoAnterior = proc.eventos.get(proc.eventos.size() - 1);
				novoEvento = new Evento(System.currentTimeMillis(), processoId, eventoAnterior.c + 1);
				proc.relogios[processoId] = proc.relogios[processoId] + 1;
				break;
			case ENVIO:
				eventoAnterior = proc.eventos.get(proc.eventos.size() - 1);
				novoEvento = new Evento(System.currentTimeMillis(), processoId, eventoAnterior.c + 1, idNodoDestino);
				proc.relogios[processoId] = proc.relogios[processoId] + 1;
				break;
			case RECEBIMENTO:
				eventoAnterior = proc.eventos.get(proc.eventos.size() - 1);
				novoEvento = new Evento(System.currentTimeMillis(), processoId, this.maxRelogios(proc.relogios[processoId], vlrRlgDaMensagem), idNodoRemetente,
						vlrRlgDaMensagem);
				proc.relogios[processoId] = this.maxRelogios(proc.relogios[processoId], vlrRlgDaMensagem);
				break;
			default:
				throw new Exception("Tipo do Evento não definido");
			}

		}
		proc.eventos.add(novoEvento); // grava evento
		System.out.println(novoEvento.toString());
	}

	private int maxRelogios(int vlrRelogioLocal, Integer vlrRlgDaMensagem) {
		return Math.max(vlrRelogioLocal, vlrRlgDaMensagem) + 1;
	}

	private void executaEnvioDeMensagem() throws Exception {
		while (recebendo == true) {
			// Segura o fluxo e envio até terminar o fluxo de recebimento
		}
		enviando = true;
		Processo procSelecionado = selecionaProcessoAleatorio();
		this.adicionaEvento(processos.get(processoId), TipoEvento.ENVIO, processos.get(processoId).relogios[processoId],
				null, null, procSelecionado.id);
		super.enviarMensagem(new Mensagem(processoId, processos.get(processoId).relogios), procSelecionado);
		enviando = false;
	}

	private Processo selecionaProcessoAleatorio() {
		Random rand = new Random();
		int numProcessos = processos.size() - 1;
		int procSorteado = rand.nextInt(numProcessos - (0 - 1) + 0);
		while (procSorteado == processoId) {
			procSorteado = rand.nextInt(numProcessos - (0 - 1) + 0);
		}
		return processos.get(procSorteado);
	}

	private void iniciaRecebimentoDeMensagens() {
		tRecebimento = (new Thread() {
			@Override
			public void run() {
				while (true) {
					Mensagem mensagem = receberMensagem();
					while (enviando == true) {
						// Segura o fluxo e recebimento até terminar o fluxo de envio
					}
					recebendo = true;
					int processoOrigem = mensagem.processoOrigem;
					int[] relogioOrigem = mensagem.relogioOrigem;
					Processo processo = processos.get(processoId);
					try {
						adicionaEvento(processo, TipoEvento.RECEBIMENTO, null, processoOrigem,
								relogioOrigem[processoOrigem], null);
						recebendo = false;
					} catch (Exception e) {
						System.out.println("Erro ao adicionar Evento de recebimento de mensagem");
						recebendo = false;
						e.printStackTrace();
					}
				}
			}
		});
		tRecebimento.start();
	}

	private void carregaProcessos() throws NumberFormatException, UnknownHostException {
		try {
			Scanner in = new Scanner(new FileReader("conf.txt"));
			int qtdProcessos = -1;
			while (in.hasNextLine()) {
				String line = in.nextLine();
				String[] valores = line.split(" ");
				if (valores.length == 1) {
					qtdProcessos = Integer.parseInt(valores[0]);
				} else {
					Processo nodo = new Processo(Integer.parseInt(valores[0]), InetAddress.getByName(valores[1]),
							Integer.parseInt(valores[2]), Double.parseDouble(valores[3]), qtdProcessos);
					processos.add(nodo);
				}
			}
			numProcessos = processos.size();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Arquivo nao encontrado!");
		}
	}
}
