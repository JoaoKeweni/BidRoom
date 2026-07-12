package server;

public class Protocol {
    
    // Agora o Protocolo retorna as partes separadas do comando.
    // Retorna um Array de Strings: [ACAO, ARGUMENTO1, ARGUMENTO2...]
    public static String[] parseCommand(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new String[]{"VAZIO"};
        }

        // O limite -1 evita que mensagens do chat sejam cortadas caso tenham o caracter "|" dentro delas
        String[] parts = message.split("\\|", -1);
        parts[0] = parts[0].toUpperCase(); // Padroniza a ação para maiúsculo (ex: login -> LOGIN)
        return parts;
    }
}
