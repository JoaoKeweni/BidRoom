package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class MainClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", 5000)) {
            System.out.println("Conectado ao servidor!");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Fase 4: Thread para OUVIR as mensagens do servidor sem travar o teclado
            Thread listenerThread = new Thread(() -> {
                try {
                    String resposta;
                    while ((resposta = in.readLine()) != null) {
                        // Sempre que receber algo, imprime na tela
                        System.out.println("\n[Servidor]: " + resposta);
                        System.out.print("> "); // Reimprime o cursor
                    }
                } catch (IOException e) {
                    System.out.println("Conexão com o servidor encerrada.");
                }
            });
            listenerThread.start();

            // Thread principal: responsável apenas por ENVIAR mensagens
            Scanner scanner = new Scanner(System.in);
            System.out.println("Você já pode digitar seus comandos (ex: LOGIN|SeuNome ou CHAT|SuaMensagem)");
            System.out.println("Para sair, digite 'sair'");
            
            while (true) {
                System.out.print("> ");
                String mensagem = scanner.nextLine();
                
                if (mensagem.equalsIgnoreCase("sair")) {
                    break;
                }
                
                out.println(mensagem);
            }
            
            System.out.println("Desconectando...");
            
        } catch (IOException e) {
            System.err.println("Erro ao conectar no servidor: " + e.getMessage());
        }
    }
}
