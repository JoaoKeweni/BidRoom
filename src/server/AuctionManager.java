package server;

import java.util.LinkedList;
import java.util.Queue;
import models.Auction;
import models.Item;

public class AuctionManager {
    
    private Queue<Item> itemPool = new LinkedList<>();
    private Auction currentAuction;
    private MainServer server;

    public AuctionManager(MainServer server) {
        this.server = server;
        // Popula a fila de itens que serão leiloados pelo servidor
        itemPool.add(new Item("1", "Placa de Vídeo RTX 4090", 5000.0));
        itemPool.add(new Item("2", "Notebook Gamer", 3500.0));
        itemPool.add(new Item("3", "Cadeira Ergonômica", 800.0));
    }

    // Método disparado pelo administrador no servidor
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

        // Instancia o leilão em memória
        currentAuction = new Auction(nextItem);
        System.out.println("Iniciando leilão para: " + nextItem.getName());
        
        // Fase 7: Broadcast de início do leilão
        server.broadcast("AUCTION_START|" + nextItem.getName() + "|" + nextItem.getStartPrice());
    }

    public Auction getCurrentAuction() {
        return currentAuction;
    }
}
