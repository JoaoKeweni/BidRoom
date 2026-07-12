# Perguntas Frequentes (Q&A de Defesa do Projeto)

Professores de Sistemas Distribuídos e Redes costumam focar em como o aluno lidou com concorrência, consistência de dados e as limitações do protocolo TCP. Este documento lista as armadilhas comuns e como você deve respondê-las.

---

### 1. "O que acontece se dois usuários clicarem em 'Dar Lance' de R$ 5.000 no exato mesmo milissegundo?"
**Sua Resposta:** 
> "O servidor não sofrerá de **Race Condition** (Condição de Corrida). A validação dos lances ocorre dentro do método `processBid`, que é protegido pela palavra reservada `synchronized`. Isso cria um monitor (Exclusão Mútua). A primeira Thread que chegar tranca o método, processa, verifica o saldo e atualiza o maior valor. A segunda Thread será forçada a esperar na fila e, quando conseguir entrar, o `highestBid` já estará em 5.000, fazendo com que o segundo lance seja rejeitado automaticamente."

### 2. "E se um espertinho alterar o relógio do computador dele ou usar um programa de Macro pra dar o lance no último milissegundo (Sniping)?"
**Sua Resposta:** 
> "Nós implementamos o padrão de arquitetura de **Single Source of Truth** (Única Fonte da Verdade). O cliente Java Swing não possui lógica de relógio interno; ele é apenas uma tela burra (`Thin Client`). O cronômetro roda em uma Thread isolada lá no Servidor e envia Broadcasts do tipo `TIME|x`. 
> Além disso, sobre o Macro/Robô: O Servidor possui uma trava de **Anti-Sniping**. Se alguém mandar um lance quando faltar menos de 10 segundos, o Servidor automaticamente reseta o tempo para 10 segundos, dando chance para os humanos cobrirem o lance."

### 3. "Como você garantiu que o download do Avatar e da Foto dos Itens não sobrecarregasse a nossa rede TCP?"
**Sua Resposta:** 
> "Nenhum arquivo binário trafega pelo nosso servidor TCP. O servidor de leilão transporta apenas Strings contendo URLs da internet. A responsabilidade do processamento de rede de mídia fica 100% isolada no Cliente. Quando o Cliente recebe a URL, ele abre uma nova conexão HTTP paralela em *background* para baixar a imagem direto da Wikipedia ou da API do DiceBear. Isso livra o tráfego do Servidor e impede que a Interface do usuário congele, já que é feito fora da Thread principal."

### 4. "Por que vocês escolheram construir um Protocolo Baseado em Texto com Pipes (`|`) ao invés de transmitir Objetos Java Serializados?"
**Sua Resposta:** 
> "Interoperabilidade. Se usássemos Objetos Serializados, o nosso Servidor ficaria amarrado exclusivamente ao Java para sempre. Passando mensagens em texto com um delimitador padrão (ex: `BID|1000`), nós garantimos que no futuro, se quisermos fazer um aplicativo mobile em Flutter, um site em Python ou um app em C#, eles poderão se conectar no nosso socket TCP puro e interpretar a string de texto perfeitamente."

### 5. "Como a transferência financeira é atômica? E se a luz cair no meio da cobrança?"
**Sua Resposta:** 
> "A liquidação acontece de uma vez só dentro do método `closeAuction()` do `AuctionManager`. O objeto `User` é recuperado diretamente da memória (Thread-safe `ConcurrentHashMap`), o seu saldo é subtraído com o método seguro e o item é adicionado à lista do Inventário. Como o controle das instâncias financeiras está engessado no Back-end, não dependemos de o cliente aprovar o pagamento. O servidor bate o martelo e liquida a dívida compulsoriamente."

### 6. "E se um cliente não estiver usando a sua Interface Gráfica e tentar forçar o comando de iniciar leilão pelo terminal (`START_AUCTION`)?"
**Sua Resposta:** 
> "O nosso sistema possui autorização (Roles). No momento do login, se a pessoa não for autenticada como o Admin Supremo (`admin1207`), a propriedade `isAdmin` no objeto dela fica cravada em `false`. O `ClientHandler` filtra os pacotes de rede; se ele interceptar comandos sensíveis vindos de uma conta comum, ele aborta a transação e devolve um `ERROR|Acesso negado`."
