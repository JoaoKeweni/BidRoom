package server;

import java.util.LinkedList;
import java.util.Queue;
import models.Auction;
import models.Item;
import models.User;

public class AuctionManager {

    private final Queue<Item> itemPool = new LinkedList<>();
    private Auction currentAuction;
    private final MainServer server;

    // Fase 8: Controle de Tempo
    private int timeLeft;

    public AuctionManager(MainServer server) {
        this.server = server;
        // Removidos os itens fixos (hardcoded). Agora o admin vai cadastrar!
    }

    // FASE 13: O Leiloeiro cadastra os itens pela rede com IMAGEM
    public synchronized void addItem(String name, double startPrice, String imageUrl) {
        Item novoItem = new Item(String.valueOf(itemPool.size() + 1), name, startPrice, imageUrl);
        itemPool.add(novoItem);
        System.out.println("Leiloeiro adicionou um item à fila: " + name);
        server.broadcast("INFO|O Leiloeiro adicionou o item [" + name + "] à fila de leilões!");
    }

    // FASE 12: Funções em tempo real do Leiloeiro
    public synchronized void addTime(int seconds) {
        if (currentAuction != null && currentAuction.isOpen() && timeLeft > 0) {
            timeLeft += seconds;
            System.out.println("Leiloeiro adicionou +" + seconds + "s ao tempo.");
            server.broadcast("INFO|O Leiloeiro esticou o tempo em " + seconds + " segundos!");
            server.broadcast("TIME|" + timeLeft); // Força atualização instantânea visual
        }
    }

    public synchronized void forceCloseAuction() {
        if (currentAuction != null && currentAuction.isOpen() && timeLeft > 0) {
            timeLeft = 0; // O laço da thread do relógio vai pegar isso no próximo segundo e fechar sozinho
            System.out.println("Leiloeiro forçou o fim do leilão!");
            server.broadcast("INFO|O Leiloeiro bateu o martelo! Encerrando...");
        }
    }

    public void startNextAuction() {
        if (currentAuction != null && currentAuction.isOpen()) {
            System.out.println("Já existe um leilão em andamento!");
            return;
        }

        Item nextItem = itemPool.poll();
        if (nextItem == null) {
            System.out.println("Não há mais itens para leiloar.");
            server.broadcast("INFO|O leilão acabou! Todos os itens foram vendidos.");
            return;
        }

        currentAuction = new Auction(nextItem);
        timeLeft = 30; // O leilão durará 30 segundos

        System.out.println("Iniciando leilão para: " + nextItem.getName());
        // Envia o BROADCAST com todos os detalhes + a URL da imagem
        server.broadcast("AUCTION_START|" + nextItem.getName() + "|" + nextItem.getStartPrice() + "|" + nextItem.getImageUrl());

        // Fase 8: Dispara o relógio
        startTimer();
    }

    // Fase 8: Thread do Cronômetro
    private void startTimer() {
        Thread timerThread = new Thread(() -> {
            while (true) {
                int currentTime;

                // Lê o tempo atual de forma segura contra concorrência
                synchronized (this) {
                    currentTime = timeLeft;
                }

                if (currentTime <= 0) {
                    break; // Sai do loop infinito
                }

                // Envia o tempo restante para todos os clientes
                server.broadcast("TIME|" + currentTime);

                try {
                    Thread.sleep(1000); // Dorme por 1 segundo
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Decrementa o tempo (com segurança)
                synchronized (this) {
                    timeLeft--;
                }
            }

            // Quando chega a zero, chama a finalização
            closeAuction();
        });
        timerThread.start();
    }

    // FASE 9: A Grande Finalização e Transação Econômica
    private synchronized void closeAuction() {
        if (currentAuction != null && currentAuction.isOpen()) {
            currentAuction.closeAuction();
            System.out.println("O tempo acabou. Fechando o leilão...");
            
            User winner = currentAuction.getLeader();
            
            if (winner != null) {
                // Efetua a transação financeira segura (deduz saldo, entrega o item)
                winner.subtractBalance(currentAuction.getHighestBid());
                winner.addItem(currentAuction.getItem());
                
                String mensagemVitoria = "VENCEDOR: " + winner.getName() + " arrematou [" + 
                                         currentAuction.getItem().getName() + "] por " + 
                                         currentAuction.getHighestBid() + " moedas!";
                                         
                System.out.println(mensagemVitoria);
                server.broadcast("AUCTION_END|" + mensagemVitoria);
            } else {
                // Ninguém deu lance
                System.out.println("Leilão encerrado sem vencedores.");
                server.broadcast("AUCTION_END|O item [" + currentAuction.getItem().getName() + "] não recebeu nenhum lance.");
            }
        }
    }

    public Auction getCurrentAuction() {
        return currentAuction;
    }

    public synchronized boolean processBid(User user, double bidAmount, ClientHandler handler) {
        if (currentAuction == null || !currentAuction.isOpen()) {
            handler.sendMessage("ERROR|Não há nenhum leilão aberto no momento.");
            return false;
        }

        if (bidAmount <= currentAuction.getHighestBid()) {
            handler.sendMessage("ERROR|O lance deve ser estritamente maior que o valor atual ("
                    + currentAuction.getHighestBid() + ").");
            return false;
        }

        if (user.getBalance() < bidAmount) {
            handler.sendMessage("ERROR|Saldo insuficiente. Você tem apenas " + user.getBalance() + " moedas.");
            return false;
        }

        currentAuction.setHighestBid(bidAmount);
        currentAuction.setLeader(user);

        // Fase 8: Anti-sniping
        // Se derem um lance nos últimos 10 segundos, o tempo sobe para 10!
        if (timeLeft < 10) {
            System.out.println("Anti-sniping ativado! Tempo reiniciado para 10s.");
            timeLeft = 10;
        }

        System.out.println("NOVO LIDER: " + user.getName() + " ofereceu " + bidAmount + " pelo item "
                + currentAuction.getItem().getName());

        server.broadcast("NEW_BID|" + user.getName() + "|" + bidAmount);
        return true;
    }
}
