package server;

import java.util.concurrent.ConcurrentHashMap;
import models.User;

public class UserManager {
    
    // Guarda o nome do usuário como chave e o objeto User como valor (Thread-Safe)
    private ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    // Realiza o login: cria um novo ou retorna o existente
    public User login(String name) {
        // computeIfAbsent: se o 'name' não existir no mapa, executa a função (cria um novo User)
        // e coloca no mapa. Se já existir, simplesmente retorna o que está lá.
        // Isso é Thread-safe por padrão no ConcurrentHashMap e evita problemas de concorrência.
        return users.computeIfAbsent(name, key -> {
            System.out.println("Criando conta e saldo inicial para o usuário novo: " + name);
            return new User(name);
        });
    }

    // Método de uso futuro para verificar se um usuário existe
    public User getUser(String name) {
        return users.get(name);
    }
}
