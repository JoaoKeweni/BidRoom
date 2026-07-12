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
        itemPool.add(new Item("1", "Placa de Vídeo RTX 4090", 5000.0));
        itemPool.add(new Item("2", "Notebook Gamer", 3500.0));
        itemPool.add(new Item("3", "Cadeira Ergonômica", 800.0));
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
        server.broadcast("AUCTION_START|" + nextItem.getName() + "|" + nextItem.getStartPrice());

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
