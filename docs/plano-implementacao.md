# Plano de Implementação — BidRoom
### Leilão Multiplayer em Tempo Real com TCP Sockets em Java

> Este documento é ao mesmo tempo um **guia passo a passo** e um **material de estudo**. Cada fase explica *o que fazer*, *por que fazer* (o conceito de Sistemas Distribuídos por trás) e *como verificar* que funcionou antes de seguir adiante. Não pule a verificação — ela é o que te dá confiança para avançar.

---

## 1. Visão Geral

Você vai construir um sistema onde vários usuários, cada um no seu próprio programa cliente, se conectam a **um servidor central** para participar de leilões. As pessoas dão lances ao mesmo tempo, conversam num chat, e o servidor (perfile de adm) decide tudo: quem é o maior lance, quando o leilão acaba, quem ganhou, e transfere as moedas.

O ponto central do projeto — e o que o professor vai avaliar — não é "fazer um leilão bonito". É **demonstrar que você entende como sistemas distribuídos funcionam**: como programas separados conversam pela rede, como lidar com várias pessoas fazendo coisas ao mesmo tempo, e como manter todos vendo a mesma verdade.

**A regra de ouro do projeto:** *o cliente nunca decide nada importante. O servidor é a única fonte da verdade.* Guarde isso, porque ela vai orientar praticamente toda decisão de design.

---

## 2. Conceitos-Chave (leia antes de programar)

Estes são os assuntos que você vai *demonstrar na prática*. Entendê-los agora faz o código fazer sentido depois.

### 2.1 Arquitetura Cliente-Servidor
Existem dois tipos de programa:
- **Servidor**: fica rodando, esperando conexões. Guarda o estado do sistema (usuários, leilões, saldos). É a autoridade.
- **Cliente**: o programa de cada usuário. Ele **pede** coisas ("quero dar um lance de 500") e **mostra** o que o servidor responde. Ele não guarda a verdade — só uma cópia da tela.

*Analogia:* o servidor é o leiloeiro no palco com o martelo. Os clientes são as pessoas na plateia levantando a plaquinha. A plateia pede; o leiloeiro decide.

### 2.2 Socket TCP
Um **socket** é a "ponta de um cano" de comunicação entre dois programas pela rede. TCP garante que os dados chegam **em ordem e sem perda** (diferente de UDP). Em Java:
- `ServerSocket` (no servidor): fica "escutando" numa porta (ex.: 5000) esperando alguém conectar.
- `Socket` (nos dois lados): representa a conexão já estabelecida. É por ele que você lê e escreve mensagens.

*Analogia:* `ServerSocket` é o telefone da recepção que fica tocando. Quando você atende (`accept()`), nasce um `Socket` — a ligação específica com aquela pessoa.

### 2.3 Threads e Concorrência
O servidor precisa atender **vários clientes ao mesmo tempo**. Se ele atendesse um de cada vez, o segundo cliente ficaria travado esperando o primeiro terminar. A solução: **uma thread por cliente**. Thread é uma "linha de execução" independente — o Java roda várias em paralelo.

- **Concorrência** = várias coisas acontecendo "ao mesmo tempo".
- O problema que ela cria: **condição de corrida** (*race condition*) — dois clientes dão um lance no mesmo instante, as duas threads leem "maior lance = 500" ao mesmo tempo, e as duas acham que ganharam. O estado fica inconsistente.

### 2.4 Exclusão Mútua (Sincronização)
A solução para a condição de corrida: garantir que **só uma thread por vez** mexa na parte crítica (o lance atual). Em Java isso se faz com `synchronized` ou `ReentrantLock`. A ideia é criar uma "fila de uma pessoa só" na porta do dado compartilhado.

*Analogia:* o banheiro de um avião. Só entra um; os outros esperam. É a **seção crítica** protegida por uma tranca (*lock*).

Este é provavelmente o conceito mais importante para a nota — é onde a maioria dos trabalhos falha, porque "funciona nos testes" mas quebra sob concorrência real.

### 2.5 Protocolo de Aplicação
Cliente e servidor precisam **combinar uma língua**. Um protocolo é o conjunto de regras: "toda mensagem começa com um tipo (`LOGIN`, `BID`...), seguida dos dados nesse formato". Sem isso, o servidor recebe texto e não sabe o que fazer. Você vai **inventar seu próprio protocolo** — isso é parte da avaliação.

### 2.6 Broadcast
Quando algo muda (alguém deu um lance, entrou na sala, mandou mensagem), o servidor precisa avisar **todos os clientes conectados**. Isso é *broadcast*: percorrer a lista de conexões e enviar a mesma mensagem para cada uma. É o que mantém todo mundo com a tela sincronizada.

### 2.7 Sincronização de Estado e Consistência
Todos os clientes precisam ver a mesma coisa: o mesmo maior lance, o mesmo tempo restante. Como só o servidor decide e faz broadcast, os clientes convergem para o mesmo estado. **Consistência** = ninguém vê uma verdade diferente do outro.

### 2.8 Gerenciamento de Sessões e Desconexões
Clientes entram e saem (ou caem, sem avisar). O servidor precisa detectar isso, remover o usuário da lista, e não quebrar quando tentar enviar mensagem para alguém que sumiu. Isso é **gerência de sessão**.

---

## 3. Pré-requisitos

Antes da Fase 1, garanta que você tem:

1. **JDK 21 instalado.** Verifique no terminal:
   ```bash
   java -version
   javac -version
   ```
   Os dois devem responder com a versão 21 (ou a exigida pela disciplina).
2. **Uma IDE ou editor** (IntelliJ, VS Code com extensão Java, ou Eclipse). Recomendo IntelliJ Community para iniciante em Java — ele ajuda muito com erros.
3. **(Opcional) Gson**, se você optar por mensagens em JSON. É um `.jar` da Google que converte objetos Java ↔ JSON. Se preferir começar sem dependências, use o protocolo de texto com separador (mostrado adiante) e adicione JSON depois.
4. **Noção básica de terminal** — você vai rodar servidor e clientes em janelas separadas para testar.

---

## 4. Arquitetura (o todo antes das partes)

```
                    ┌─────────────────────────────┐
                    │          SERVIDOR           │
                    │                             │
   Cliente João ────┤  ServerSocket (porta 5000)  │
                    │        │                    │
   Cliente Maria ───┤        ├─ ClientHandler(João)  ← 1 thread
                    │        ├─ ClientHandler(Maria) ← 1 thread
   Cliente Pedro ───┤        └─ ClientHandler(Pedro) ← 1 thread
                    │                             │
                    │  AuctionManager (leilões)   │  ← estado
                    │  UserManager    (usuários)  │     compartilhado
                    │  Protocol       (a "língua")│     (protegido por locks)
                    └─────────────────────────────┘
```

**Como as peças conversam:**
- `MainServer` — liga o servidor, fica no `accept()` aceitando conexões. Para cada uma, cria um `ClientHandler` e dispara uma thread.
- `ClientHandler` — a thread dedicada a um cliente. Lê mensagens, pede ao `Protocol` para interpretar, chama o manager certo, e responde. Também guarda a referência para poder enviar broadcast.
- `AuctionManager` — cérebro do leilão. Inicia, valida lances (**com exclusão mútua**), controla o cronômetro, finaliza, escolhe vencedor.
- `UserManager` — cuida de login, saldos, inventários, desconexões.
- `Protocol` — traduz texto ↔ comando estruturado, nos dois sentidos.
- `models/` (`User`, `Item`, `Auction`, `Bid`) — as "coisas" do sistema, só dados.

**Por que essa separação?** Cada classe tem uma responsabilidade única. Isso deixa o código testável fase a fase e é exatamente o que dá "cara de trabalho de faculdade": você consegue apontar no código onde cada conceito de Sistemas Distribuídos foi implementado.

---

## 5. Passos de Implementação

> Cada fase = um marco (milestone) que você **testa isoladamente** antes de avançar. Se a fase não passa na verificação, não comece a próxima — você só vai empilhar bugs difíceis de achar.

---

### Fase 1 — Comunicação Básica (a fundação)

**O que fazer:**
Criar um servidor que aceita múltiplos clientes e um cliente mínimo que conecta. Sem interface, sem lógica de leilão. Só conectar, detectar conexão e desconexão, imprimir no console.

- No servidor: `ServerSocket` numa porta fixa, um laço infinito com `accept()`, e para cada conexão criar um `ClientHandler` rodando em sua própria `Thread`.
- No `ClientHandler`: um laço que lê linhas do cliente e imprime no console. Ao detectar fim da conexão, imprime que o cliente saiu.

**Por que:**
Esta é a base de *tudo*. Sem conexão confiável e múltiplas threads funcionando, nenhuma fase seguinte existe. Aqui você demonstra **comunicação TCP** e **uma thread por cliente** (concorrência). Fazer isso primeiro, isolado, garante que a fundação é sólida.

**Como verificar:**
1. Rode o servidor. Deve imprimir `Servidor iniciado na porta 5000`.
2. Conecte um cliente (pode ser até pelo `telnet localhost 5000` no início). Servidor imprime `Cliente conectado`.
3. Abra um **segundo** cliente ao mesmo tempo. Servidor imprime a segunda conexão **sem travar** a primeira. ← *isto prova que as threads funcionam.*
4. Feche um cliente. Servidor imprime `Cliente desconectado` e continua rodando.

---

### Fase 2 — Protocolo de Comunicação (a língua)

**O que fazer:**
Definir o formato das mensagens e criar a classe `Protocol` que faz o *parsing* (interpretar texto recebido) e a *serialização* (montar texto para enviar).

Escolha **um** formato e seja consistente:
- **Opção simples (recomendada para começar):** texto com separador.
  ```
  LOGIN|João
  BID|500
  CHAT|Olá pessoal
  ```
- **Opção JSON (com Gson):**
  ```json
  { "type": "LOGIN", "name": "João" }
  ```

Defina a lista de comandos: `LOGIN`, `JOIN`, `CHAT`, `CREATE_ITEM`, `START_AUCTION`, `BID`, `END_AUCTION` — e também as **respostas do servidor** (ex.: `LOGIN_OK`, `ERROR|saldo insuficiente`, `NEW_BID|João|500`).

**Por que:**
Sem protocolo, o servidor recebe texto solto e não sabe o que é. O protocolo é o **contrato** entre as pontas — um dos conceitos avaliados. Centralizar em `Protocol` evita espalhar `if (mensagem.startsWith(...))` pelo código todo. Definir as respostas do servidor agora (não só os pedidos) evita retrabalho depois.

**Como verificar:**
Escreva um pequeno teste (ou um `main` temporário) que passa `"BID|500"` para o `Protocol` e confirma que ele devolve algo como `tipo=BID, valor=500`. E o inverso: montar um comando e conferir o texto gerado. Envie um `LOGIN|João` de um cliente e veja o servidor reconhecer o comando (não só imprimir texto cru).

---

### Fase 3 — Sala e Lista de Clientes (estado compartilhado)

**O que fazer:**
O servidor mantém uma **lista de clientes conectados** (na prática, uma lista dos `ClientHandler` ativos). Quando alguém conecta e faz `LOGIN`, é adicionado à lista.

⚠️ Use uma coleção **thread-safe** (ex.: `CopyOnWriteArrayList` ou uma `List` normal protegida por `synchronized`), porque várias threads vão adicionar/remover ao mesmo tempo.

**Por que:**
Esta lista é o primeiro **estado compartilhado** entre threads — e o primeiro lugar onde a concorrência pode causar bug. É também a base do broadcast: para avisar "todos", você precisa saber quem são "todos". Aqui aparece a **gerência de sessão**.

**Como verificar:**
Conecte 3 clientes. Adicione um comando de debug que lista os conectados no console do servidor: deve mostrar os 3. Desconecte um: a lista cai para 2, sem erro.

---

### Fase 4 — Chat (primeiro broadcast)

**O que fazer:**
Cliente envia `CHAT|mensagem`. O servidor recebe e **reenvia para todos** os clientes da lista (broadcast), incluindo o nome de quem falou: `CHAT|João|Olá`.

**Por que:**
O chat é a forma mais simples de exercitar **broadcast** e **sincronização de estado** — se está fácil, você vê a mensagem aparecer em todas as janelas ao mesmo tempo. É um "leilão de mentira" para treinar o mecanismo que os lances vão usar depois. Melhor descobrir problemas de broadcast aqui, com chat, do que na Fase 8 com dinheiro envolvido.

**Como verificar:**
Com 3 clientes abertos, João envia uma mensagem. **Os 3** (inclusive o João) veem a mensagem. Maria responde; todos veem. Se um cliente não recebe, o bug está no broadcast — resolva antes de seguir.

---

### Fase 5 — Sistema de Usuários e Economia (modelo de dados + UserManager)

**O que fazer:**
Criar a classe `User` (id, nome, avatar, saldo, inventário) e o `UserManager`. No `LOGIN`, criar o usuário com **saldo inicial de 10.000 moedas** e inventário vazio. O `UserManager` cuida de login, busca por usuário e desconexão.

**Por que:**
Agora o sistema deixa de ser "conexões anônimas" e passa a ter **identidade e estado por usuário**. O saldo é o dado que os lances vão validar — precisa ser confiável e viver **só no servidor** (nunca confie no saldo que o cliente diz ter; regra de ouro).

**Como verificar:**
Faça login de 2 usuários. Peça (via comando de debug) o saldo de cada um: ambos com 10.000. Desconecte e reconecte — decida agora a regra: o usuário volta com o saldo anterior ou zera? (Para o escopo do trabalho, manter em memória e recriar do zero costuma bastar — documente essa escolha.)

---

### Fase 6 — Itens e Início do Leilão (Escopo Reduzido)

**O que fazer:**
Criar `Item` (id, nome, preçoInicial) e `Auction` (item, maiorLance, líder, status). O cliente NÃO cadastra itens. Criaremos um `AuctionManager` no servidor que já possui uma lista de itens pré-cadastrados no código.
O `MainServer` terá um console de administrador (`Scanner`). Quando o administrador digitar `iniciar`, o servidor pega o próximo item da fila, cria o leilão e faz broadcast `AUCTION_START|item|preço` para todos.

**Por que:**
Reduz a complexidade e foca no essencial: concorrência. O `AuctionManager` centraliza as decisões do leilão — coerente com a regra de ouro onde o cliente nunca dita o estado do servidor.

**Como verificar:**
Abra clientes e o servidor. No terminal do servidor, digite `iniciar`. Todos os clientes conectados devem receber a notificação do início do leilão.

---

### Fase 7 — Lances com Exclusão Mútua (o coração do projeto) ⭐

**O que fazer:**
Cliente envia `BID|valor`. O servidor, dentro de uma **seção crítica protegida** (`synchronized` no objeto do leilão ou um `ReentrantLock` no `AuctionManager`), valida **na ordem**:
1. O leilão está ABERTO? (não acabou o tempo)
2. O valor é **maior** que o maior lance atual?
3. O usuário **tem saldo** suficiente?

Se tudo passar: atualiza maiorLance e líder, registra um `Bid`, e faz broadcast `NEW_BID|usuário|valor`. Se algo falhar: responde só para aquele cliente `ERROR|motivo`.

**Por que — leia com atenção:**
Este é o ponto onde **exclusão mútua** deixa de ser teoria. Sem o *lock*, imagine: maior lance = 500. João e Maria mandam 600 no mesmo milissegundo. As duas threads leem "500", as duas acham "600 > 500 ✓", e as duas viram líder — o estado fica inconsistente e talvez você cobre os dois. Com a seção crítica, a thread da Maria **espera** a do João terminar; quando ela entra, o maior lance já é 600, e o 600 dela é rejeitado (ou ela precisa mandar 601). **É exatamente este trecho que prova, na prática, o conceito de concorrência controlada — destaque-o na apresentação.**

Detalhe importante sobre saldo: decida se o dinheiro é "reservado" no momento do lance ou só debitado do vencedor no fim. Para o escopo, o mais simples é **validar** o saldo no lance e **debitar só o vencedor** na finalização — mas documente essa decisão.

**Como verificar:**
1. Teste normal: João dá 600, todos veem, João é líder. Maria dá 700, vira líder.
2. Teste de rejeição: alguém tenta um lance menor que o atual → recebe `ERROR`. Alguém sem saldo tenta → recebe `ERROR`.
3. **Teste de concorrência (o que vale nota):** faça um cliente de teste que dispare vários lances em paralelo (várias threads mandando ao mesmo tempo). Ao final, deve existir **um único líder coerente** e o maior lance deve ser o maior valor válido — nunca dois líderes, nunca um valor "perdido". Rode várias vezes; o resultado deve ser sempre consistente.

---

### Fase 8 — Cronômetro Controlado pelo Servidor

**O que fazer:**
O servidor mantém o tempo restante do leilão (ex.: uma thread do `AuctionManager` que a cada segundo decrementa e faz broadcast `TIME|segundos`). O cliente **só exibe** o número que recebe. Regra extra: se chegar um lance válido nos últimos X segundos, o servidor **reinicia/estende** o cronômetro (anti-sniping).

**Por que:**
Se cada cliente contasse o próprio tempo, os relógios divergiriam (rede tem atraso) e o leilão acabaria em momentos diferentes para cada um — **inconsistência**. Centralizar o tempo no servidor é a aplicação direta da regra de ouro e de sincronização de estado. Cuidado de concorrência: o cronômetro (uma thread) e os lances (outras threads) mexem no mesmo leilão — o **mesmo lock da Fase 7** deve proteger a transição "tempo chegou a zero → fecha".

**Como verificar:**
Inicie um leilão de 20s. Os 3 clientes mostram a **mesma** contagem. Dê um lance faltando 2s → o tempo estende. Deixe zerar sem lances → o status vai para ENCERRADO em todos ao mesmo tempo.

---

### Fase 9 — Finalização e Transferência

**O que fazer:**
Quando o tempo zera, o servidor (na seção crítica): fecha o leilão, escolhe o **líder como vencedor**, **debita** as moedas do vencedor, **credita** o dono do item, **transfere o item** para o inventário do vencedor, e faz broadcast `AUCTION_END|vencedor|valor`. Se ninguém deu lance, o item volta/permanece com o dono.

**Por que:**
É a "transação" do sistema — várias mudanças de estado que precisam acontecer **juntas e sem interferência** de um lance atrasado. Por isso roda na seção crítica e só depois de o leilão estar fechado. Demonstra **consistência de dados** sob concorrência (nenhum lance pode "furar a fila" e entrar depois do fim).

**Como verificar:**
Rode um leilão completo. Confira que: saldo do vencedor caiu exatamente pelo valor, saldo do dono subiu o mesmo tanto (soma total de moedas no sistema **se conserva** — ótimo invariante para testar!), o item está no inventário do vencedor. Tente dar um lance depois do `AUCTION_END` → deve ser rejeitado.

---

### Fase 10 — Interface Gráfica (por último, de propósito)

**O que fazer:**
Só agora a interface web (HTML/CSS/JS), inspirada no Kahoot:
- **Centro:** produto, imagem, maior lance, tempo.
- **Inferior:** avatares pixel art dos participantes.
- **Direita:** chat.
- **Esquerda:** saldo, itens, histórico.

O JS conecta ao servidor. Como navegador não fala TCP puro, você tem duas opções: (a) um **WebSocket** no servidor Java (mais trabalho, mas é o "certo" para web), ou (b) manter o cliente em **Java** (Swing/JavaFX) e usar o HTML só como mock visual. **Confirme com o professor** qual caminho é aceito — isso muda bastante o esforço. Se o foco é sockets TCP puros, um cliente Java costuma ser o mais alinhado ao objetivo da disciplina.

**Por que por último:**
A interface é a parte que *menos* demonstra Sistemas Distribuídos e a que mais consome tempo com detalhe visual. Deixá-la por último garante que a lógica avaliada já está pronta e testada. Se o tempo apertar, você entrega um sistema funcional (mesmo que feio) em vez de uma tela bonita que não funciona.

**Como verificar:**
Com a interface, dois usuários reais conseguem: logar, ver o item, dar lances vendo o tempo e o maior lance atualizarem em tempo real, conversar no chat, e ver o resultado ao final — tudo sem tocar no console.

---

## 6. Ordem de Criação dos Arquivos (mapa rápido)

| Fase | Arquivos que você cria/edita |
|------|------------------------------|
| 1 | `MainServer.java`, `ClientHandler.java` |
| 2 | `Protocol.java` |
| 3 | `ClientHandler` (lista de clientes) |
| 4 | `ClientHandler`/`MainServer` (broadcast) |
| 5 | `models/User.java`, `UserManager.java` |
| 6 | `models/Item.java`, `models/Auction.java`, `AuctionManager.java` |
| 7 | `models/Bid.java`, `AuctionManager` (lock nos lances) |
| 8 | `AuctionManager` (thread do cronômetro) |
| 9 | `AuctionManager` + `UserManager` (transferência) |
| 10 | `client/index.html`, `style.css`, `app.js` |

---

## 7. Riscos e Pontos de Atenção

- **Esquecer o lock nos lances (Fase 8).** É o erro clássico. "Funciona quando testo sozinho" e quebra com concorrência real. Teste com lances paralelos de propósito.
- **Deixar o cliente decidir algo** (tempo, se o lance é válido, saldo). Sempre revalide no servidor. Regra de ouro.
- **Broadcast para um cliente que caiu.** Ao enviar, trate a exceção e remova o cliente morto da lista, senão uma desconexão derruba o broadcast dos outros.
- **Bloquear a thread do servidor.** Nunca faça `accept()` ou lógica pesada na thread errada travar o atendimento dos outros. Uma thread por cliente resolve isso — não centralize processamento numa só.
- **Estado só em memória.** Se o servidor cair, perde tudo. Para o escopo acadêmico geralmente tudo bem — mas **documente** essa limitação (mostra maturidade).
- **Deadlock ao usar vários locks.** Se você criar mais de um lock (ex.: um por usuário + um por leilão), cuidado com a ordem de aquisição. Comece com **um lock só** no leilão para não cair nessa armadilha.
- **Testar tudo no fim.** Não faça. Teste **fase a fase** — é a única forma de saber onde um bug foi introduzido.

---

## 8. Roteiro de Marcos para a Apresentação

Para dar "cara de trabalho de faculdade", apresente a **evolução**, mapeando cada marco ao conceito demonstrado:

1. Comunicação TCP servidor↔clientes → *sockets, arquitetura cliente-servidor*
2. Login e entrada na sala → *sessões, estado compartilhado*
3. Chat em tempo real → *broadcast, sincronização*
4. Usuários e economia virtual → *estado por usuário no servidor*
5. Itens e Leilão centralizado → *domínio central, centralização de decisões*
6. Lances → *concorrência e exclusão mútua* ⭐ (seu ponto alto)
7. Finalização automática + transferência → *consistência transacional*
8. Interface e animações → *experiência do usuário*

Na defesa, **abra o código da Fase 7** e mostre a seção crítica: é a prova concreta de que você entendeu concorrência, o conceito mais valorizado num trabalho de Sistemas Distribuídos.
