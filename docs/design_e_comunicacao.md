# Arquitetura de Design e Comunicação (Front-end vs Back-end)

Este documento foi criado para detalhar o funcionamento exclusivo do **Cliente Java Swing** (`MainGUIClient.java`), explicando como a tela ganha vida e como ela conversa de forma eficiente com o servidor. 

---

## 1. O Padrão "Tela Burra" (Thin Client)

A maior armadilha ao criar aplicações de rede com Interface Gráfica (UI) é misturar Lógica de Negócios com a Tela. No **BidRoom**, a arquitetura adotada é a do **Thin Client (Cliente Fino)**. 

O que isso significa?
- **O Java Swing não faz cálculos financeiros.** Ele nunca subtrai moedas e não possui nenhuma variável `meuDinheiroAtual = 1000`.
- **Ele não mede o tempo.** Não existe nenhuma Thread rodando um contador ou cronômetro interno no cliente.
- A tela existe única e exclusivamente para **duas tarefas**: 
  1. Enviar eventos de cliques para a rede.
  2. "Reagir" ao que a rede responde.

---

## 2. O Dicionário de Comunicação (O Protocolo)

Para que o Front-end e o Back-end conversem com extrema velocidade, não usamos um protocolo pesado de XML ou serialização do Java. Nós criamos nosso próprio "Protocolo em Texto Puro" usando *Pipes* (`|`) como delimitadores.

### Como a Tela MANDA algo para o Back-end?
O Front-end tem acesso apenas a um "tubo de envio" chamado `PrintWriter (out)`. 
Quando o usuário clica no botão verde **"Dar Lance!"**, o Java Swing varre o que está na caixa de texto e simplesmente cospe para a rede a String: `"BID|1000"`. Só isso. A tela apaga a caixa de texto e volta a dormir.

### Como a Tela RECEBE algo do Back-end?
O Front-end tem uma Thread isolada que fica rodando em laço infinito "ouvindo" um `BufferedReader (in)`.
Se o Servidor mandar a resposta mágica `"TIME|23"`, a nossa Thread divide a String usando o pipe `|` e percebe que o prefixo é `TIME`.
A partir daí, ela invoca o método de atualização segura do Java Swing:
```java
lblTimeLeft.setText("Tempo restante: " + partes[1] + "s");
```

---

## 3. Painéis Mutantes e a Arquitetura em Abas (JTabbedPane)

Para não sobrecarregar o usuário com múltiplas janelinhas soltas, todo o design foi feito dentro de uma única Tela Base (Frame), usando layouts inteligentes.

### A) A Mutação por Papel (Role-Based Rendering)
O aplicativo se comporta como um *camaleão*. Se o usuário digita um nome normal, ele recebe o **Painel do Comprador** (Apenas uma caixinha simples para dar um lance).
Se o usuário logar secretamente com a senha do Admin (`admin1207`), a Interface Gráfica sofre uma "mutação ao vivo":
```java
if (nome.equals("admin1207")) {
    bottomArea.remove(bidPanel); // Arranca a área de comprador
    bottomArea.add(adminPanel);  // Injeta uma gigantesca mesa de controle (Cadastrar Item, Adicionar +5s, Fechar Leilão)
}
```

### B) O JTabbedPane (Isolamento Visual)
Criamos a sensação de "Game" ou "Rede Social" dividindo a interface em duas grandes abas:
- **Aba "Salão Principal":** Abriga as ferramentas urgentes (Cronômetro, Maior Lance, Chat e Botão de Lance). Tudo o que precisa ser lido em milissegundos.
- **Aba "Meu Perfil e Inventário":** É um local "preguiçoso". Ele não é renderizado toda hora. A lista contendo "A Coroa do Rei" e as "Armaduras" só é gerada através da mensagem especial `PROFILE_DATA` enviada pelo servidor.

---

## 4. O Sistema de Download de "Mídia Assíncrona"

Leilões precisam de apelo visual (Fotos da Mona Lisa, Carros, Avatares, etc.). 
No entanto, o Swing possui uma fraqueza brutal conhecida: A ***Event Dispatch Thread (EDT)***. 

Se mandarmos a interface baixar a imagem da internet na linha principal do código, toda a tela do leilão (os lances, o chat, o botão) **congela** até os pixels da foto terminarem de ser baixados, quebrando a experiência de quem está comprando no desespero dos últimos segundos.

### A Genialidade do Design no `carregarImagemDaWeb()`
Quando o Front-end escuta do servidor que um leilão começou e recebe a URL de uma Ferrari (`AUCTION_START|Ferrari|100|http://link...`), nós mandamos a nossa interface gráfica abrir uma *Sub-Thread (Background Task)* isolada.
```java
new Thread(() -> {
    URL url = new URL(imageUrl);
    Image image = ImageIO.read(url); // <-- Isso pode demorar, mas ocorre no escuro, sem travar nada!
    
    // Quando terminar, aí sim avisa a Interface com o invokeLater!
    SwingUtilities.invokeLater(() -> lblImage.setIcon(icon));
}).start();
```
É por causa disso que a tela do nosso leilão flui lindamente sem dar um único travamento, seja desenhando os avatares (*DiceBear*) ou carregando fotos 4K direto do Google Imagens na tela do Leilão!
