# Guia Definitivo do BidRoom (Leilão Distribuído)

Este documento foi criado para ajudá-lo a entender a arquitetura do projeto de ponta a ponta e servir como roteiro na hora da sua apresentação acadêmica.

---

## 1. Como Rodar o Projeto

### Pré-requisitos
- JDK 21+ instalado.
- Computadores na mesma rede Wi-Fi (caso for apresentar em múltiplos PCs).

### Passo 1: Compilação
Abra o terminal na pasta raiz do projeto (`BidRoom/`) e rode o comando que compila todas as classes, jogando os binários na pasta `bin`:
```bash
javac -d bin src/models/*.java src/server/*.java src/client/*.java
```

### Passo 2: Iniciar o Servidor (O Banco Central)
O Servidor deve ser o primeiro a ligar. Ele abre a porta TCP 5000 e fica escutando.
```bash
java -cp bin server.MainServer
```

### Passo 3: Iniciar os Clientes
Abra novos terminais e rode as instâncias do cliente:
```bash
java -cp bin client.MainGUIClient
```
- **Login Leiloeiro:** Digite `admin1207`. O painel mutante de administração será ativado.
- **Login Comprador:** Digite qualquer nome.

---

## 2. Trechos de Códigos Indispensáveis

Estes são os pedaços de código que "garantem a nota" na disciplina de Sistemas Distribuídos. Se o professor pedir para ver o código, mostre essas áreas:

### A) Multi-threading: Aceitando Múltiplas Conexões
Como o Servidor atende vários clientes ao mesmo tempo sem travar?
*(Arquivo: `MainServer.java`)*
```java
while (true) {
    // Fica bloqueado até alguém conectar
    Socket socket = serverSocket.accept();
    
    // Cria um atendente (ClientHandler) exclusivo para esse socket
    ClientHandler clientHandler = new ClientHandler(socket, this, userManager);
    clients.add(clientHandler);
    
    // Dispara uma Thread paralela para ele. Assim o while não trava!
    Thread clientThread = new Thread(clientHandler);
    clientThread.start();
}
```

### B) Exclusão Mútua (O Coração do Leilão)
Garante que se dois clientes derem lance no exato mesmo milissegundo, os lances entrarão em fila (monitor), impedindo a "Condição de Corrida".
*(Arquivo: `AuctionManager.java`)*
```java
// A palavra 'synchronized' age como um cadeado (Mutex). Só uma Thread pode entrar aqui por vez.
public synchronized boolean processBid(String user, double amount) {
    if (amount <= currentAuction.getHighestBid()) return false;
    
    // Validação de saldo com o Banco
    User bidder = server.getUserManager().getUser(user);
    if (bidder.getBalance() < amount) return false;

    // Atualiza o líder
    currentAuction.setHighestBid(amount);
    currentAuction.setHighestBidder(user);
    
    // Sistema Anti-sniping: se der lance no fim, o tempo volta pra 10s
    if (timeLeft < 10) timeLeft = 10;
    
    return true;
}
```

### C) O Relógio Single Source of Truth
O cronômetro roda em uma Thread isolada no **Servidor** e dita a lei para os clientes. Os clientes não possuem lógica de relógio, apenas exibem o que o servidor manda.
*(Arquivo: `AuctionManager.java`)*
```java
private void startTimer() {
    new Thread(() -> {
        while (timeLeft > 0) {
            try {
                Thread.sleep(1000); // Aguarda 1 segundo exato
                timeLeft--;
                server.broadcast("TIME|" + timeLeft); // Avisa todo mundo
            } catch (InterruptedException e) { }
        }
        closeAuction(); // Quando chega a zero, liquida o pagamento e transfere o item
    }).start();
}
```

### D) Concorrência em UI: Atualização Assíncrona no Java Swing
Você não pode alterar a tela de um aplicativo Java (como um texto de JLabel) a partir da Thread que escuta a rede, senão a interface quebra. O Swing exige que atualizações visuais ocorram na *Event Dispatch Thread*.
*(Arquivo: `MainGUIClient.java`)*
```java
private void processarMensagemServidor(String mensagem) {
    // Enfileira a atualização visual na Thread principal da tela com segurança
    SwingUtilities.invokeLater(() -> {
        // ... (lógica do switch para mudar labels, adicionar imagens, etc)
        lblHighestBid.setText("Valor: R$ " + partes[2]);
    });
}
```

### E) Mídia Desacoplada (Baixando Arquivos na Rede)
Impedindo que a interface trave durante o download das fotos do leilão (Threads de I/O de Rede).
*(Arquivo: `MainGUIClient.java`)*
```java
private void carregarImagemDaWeb(String imageUrl) {
    // Abre uma nova Thread para que o Leilão continue rodando enquanto a foto é baixada
    new Thread(() -> {
        URL url = new URL(imageUrl);
        Image image = ImageIO.read(url);
        // Só injeta na UI quando o download do HTTP terminar completamente
        SwingUtilities.invokeLater(() -> lblImage.setIcon(new ImageIcon(image)));
    }).start();
}
```
