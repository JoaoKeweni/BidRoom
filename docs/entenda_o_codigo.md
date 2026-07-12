# Entenda o Código: Concorrência e Sistemas Distribuídos na Prática

Este não é um simples aplicativo de chat. O **BidRoom** foi projetado para lidar com problemas reais da engenharia de software: **Múltiplos usuários tentando alterar a mesma informação ao mesmo tempo.** 

Se você for apresentar o código, precisa dominar os conceitos abaixo. Eles conectam a teoria pura da disciplina de Sistemas Distribuídos com as linhas de código que escrevemos.

---

## 1. O Fantasma da Concorrência (Race Conditions)

O maior desafio de um Leilão Distribuído é a **Condição de Corrida** (*Race Condition*). Imagine este cenário trágico:
- O leilão de um carro está em R$ 10.000.
- O **João** e a **Maria** decidem cobrir o lance no exato mesmo milissegundo. 
- O João envia "Dou 15 mil". A Maria envia "Dou 12 mil".
- Como a internet não é perfeita, os pacotes chegam juntos ao Servidor. Se o servidor ler a memória ao mesmo tempo para os dois, ele pode validar o lance do João, mas em seguida sobrescrever com o lance da Maria (12 mil), mesmo sendo menor! O sistema colapsaria e o banco perderia dinheiro.

### Como pulamos o obstáculo da Concorrência?
A solução está no arquivo `AuctionManager.java`, no coração do processamento de lances:

```java
public synchronized boolean processBid(String user, double amount) { ... }
```

A palavra mágica aqui é **`synchronized`**. Em Sistemas Distribuídos, isso implementa o que chamamos de **Exclusão Mútua (Mutex) ou Monitor**.
Quando colocamos essa trava no método, nós transformamos a entrada em uma catraca de estádio. Mesmo que 10.000 pessoas mandem um lance no mesmo milissegundo, a Máquina Virtual do Java (JVM) bloqueia a porta. Apenas a **Thread 1** entra, lê o valor atual, valida o saldo, atualiza o lance e sai. Só depois a **Thread 2** é liberada para entrar. Como a Thread 2 entrou depois, ela já vai enxergar o valor atualizado e o lance atrasado/menor será sumariamente rejeitado. **Garantimos a integridade dos dados!**

---

## 2. A Orquestra de Threads (Por que precisamos de tantas?)

Um sistema de rede puramente síncrono é inútil: ele atende uma pessoa e deixa todas as outras esperando (congeladas). Para criar um ambiente tempo-real (Real-time), nós dividimos o trabalho do nosso processador em **Threads** (Linhas de execução paralelas).

O nosso ecossistema roda sustentado por 4 tipos de Threads diferentes:

### A) A Thread Porteira (`MainServer.java`)
Quando o servidor liga, ele para na linha `serverSocket.accept();`. Isso é uma instrução de bloqueio (*Blocking I/O*). O código congela ali esperando alguém se conectar na porta 5000.
Se o nosso sistema não tivesse Threads, assim que o João conectasse, o servidor ficaria preso conversando com o João e a porta 5000 ficaria fechada para a Maria.
**O que nós fizemos:** Assim que o João conecta, nós "jogamos" a conexão dele para uma nova Thread paralela e o porteiro volta rapidamente para a porta 5000 para esperar o próximo cliente.

### B) As Threads Garçons (`ClientHandler.java`)
Para CADA usuário conectado, o servidor abre uma Thread exclusiva. Se tivermos 50 alunos na sala do leilão, teremos 50 Threads ativas no servidor. O trabalho do *ClientHandler* é ficar em um `while(true)` infinito, com um "copo na parede", apenas escutando os *bytes* que estão chegando pelo cano de rede daquele usuário específico. Graças a esse paralelismo, o João pode digitar no chat sem interferir na ação do Leiloeiro.

### C) A Thread do Relógio ("Single Source of Truth")
O relógio do leilão (30, 29, 28...) **NÃO** roda nos clientes. Se rodasse, os usuários poderiam usar *Cheat Engine*, fraudar a memória RAM deles ou alterar a hora do Windows para burlar o leilão.
Nós adotamos o padrão **Única Fonte da Verdade**. No `AuctionManager.java`, nós criamos uma Thread que faz um loop de `Thread.sleep(1000)` (dorme 1 segundo exato) e tira 1 do tempo. Essa Thread é a dona suprema do tempo. A cada 1 segundo, ela espalha o tempo para todos pela rede. Os clientes são apenas "telas burras" (Thin Clients) que exibem o que o servidor manda.

### D) As Threads de Concorrência de UI (Cliente Java Swing)
No cliente, temos um grande inimigo: A "Thread de Desenho da Tela" (*Event Dispatch Thread - EDT*). Se você mandar ela fazer trabalho pesado (como baixar uma foto de 2 Megabytes da internet), a janela do seu aplicativo congela e aparece a temida mensagem "(Não está respondendo)".
**Como resolvemos:** Toda vez que chega o comando para baixar a imagem da internet (ou gerar o Avatar no DiceBear), nós instanciamos uma *Thread Assíncrona de Background*. A Thread vai lá no servidor do Google/DiceBear, baixa a foto sem ninguém ver e, só quando a foto está pronta, ela manda um sinal `SwingUtilities.invokeLater()`. Esse sinal diz para a interface: *"Ei, toma a foto pronta, atualize a tela"*. Tudo flui magicamente sem travamentos!

---

## 3. Resumo da Ópera para a Banca

Se o professor perguntar: *"Qual foi a maior dificuldade técnica desse projeto?"*

Você responde:
> *"Foi garantir o sincronismo de estado entre máquinas diferentes. Nós optamos por uma arquitetura Cliente-Servidor Estrela via TCP puro para garantir que a ordem dos pacotes de lance não se perdesse. O Servidor centraliza toda a lógica de negócios e o Banco de Dados (Saldo/Inventário), enquanto protege a validação de lances usando Exclusão Mútua (`synchronized`). Do lado dos clientes, o desafio foi a programação assíncrona, usando Threads separadas para não bloquear a interface gráfica enquanto escutamos a rede ou baixamos as imagens da Web."*
