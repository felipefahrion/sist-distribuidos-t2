
Sistemas Distribuídos - Trabalho 2


O segundo trabalho da disciplina consiste em desenvolver um programa dis-
tribu ́ıdo que implemente rel ́ogios vetoriais para ordena ̧c ̃ao de eventos (com
ordem total). O programa deve receber como entrada um arquivo de con-
figura ̧c ̃ao e um n ́umero que identifica uma das linhas do arquivo de confi-
gura ̧c ̃ao. Todos os processos devem possuir uma c ́opia desse arquivo. Cada
linha do arquivo de configura ̧c ̃ao ter ́a o seguinte formato:
id host port chance events min_delay max_delay
• id  ́e um n ́umero inteiro que identifica o processo;
• host  ́e o hostname ou endere ̧co IP da m ́aquina (nodo) que executa o
processo;
• port  ́e o n ́umero da porta que o processo vai escutar;
• chance  ́e uma probabilidade (entre 0 e 1) da ocorrˆencia de um evento
de envio de mensagem. Por exemplo, o valor 0.2 significa 20% de
probabilidade de ser realizado um envio de mensagem, sendo os 80%
restantes eventos locais;
• events  ́e o n ́umero de eventos que ser ̃ao executados nesse nodo (recomen-
da-se aproximadamente 100 eventos);
• min delay  ́e o tempo m ́ınimo de intervalo entre eventos (recomenda-se
valores entre 100 e 300 ms);
• max delay  ́e o tempo m ́aximo de intervalo entre eventos (recomenda-se
valores entre 350 e 750 ms).
1
Execu ̧c ̃ao do programa e descri ̧c ̃ao do algoritmo:
 ́E importante que um mecanismo de sincroniza ̧c ̃ao inicial seja implemen-
tado para que todos os processos iniciem a execu ̧c ̃ao do algoritmo ao mesmo
tempo. Para isso, pode-se utilizar um grupo multicast. Cada nodo pode exe-
cutar um evento local ou enviar uma mensagem para outro nodo, de acordo
com a probabilidade estabelecida em sua configura ̧c ̃ao. Utilize datagramas
para o envio de mensagens.
• Para eventos locais, incremente o rel ́ogio local;
• Para envio de mensagens, escolha aleatoriamente entre um dos outros
nodos definidos no arquivo de configura ̧c ̃ao e envie uma mensagem con-
tendo tamb ́em o valor do rel ́ogio.
Devem ser gerados eventos com intervalo de min delay a max delay, e cada
nodo deve executar diversos desses eventos (de acordo com a configura ̧c ̃ao,
podendo ser um evento local ou envio de mensagem) e depois terminar sua
execu ̧c ̃ao. Pode ocorrer de um nodo tentar enviar uma mensagem para um
nodo que j ́a terminou sua execu ̧c ̃ao, neste caso trate o erro de tal forma que
interrompa a execu ̧c ̃ao do processo que tentou enviar.
Sa ́ıda do programa:
Cada processo deve individualmente produzir sua pr ́opria sa ́ıda. Use a
seguinte sintaxe para formatar a sa ́ıda:
• Evento local: i [c,c,c,c,...] L, onde i  ́e o ID do nodo local e [c,c,c,c,...]
 ́e o valor do rel ́ogio vetorial local;
• Envio de mensagem: i [c,c,c,c,...] S d, onde i  ́e o ID do nodo local,
[c,c,c,c,...]  ́e o valor do rel ́ogio vetorial enviado e d  ́e o ID do nodo
destinat ́ario da mensagem;
• Recebimento de mensagem: i [c,c,c,c,...] R s t, onde i  ́e o ID do nodo
local, [c,c,c,c,...]  ́e o valor do rel ́ogio vetorial depois do recebimento
da mensagem, s  ́e ID do nodo remetente da mensagem e t  ́e o valor do
rel ́ogio l ́ogico recebido com a mensagem.
2
