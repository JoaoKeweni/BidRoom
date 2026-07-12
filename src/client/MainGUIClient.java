package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class MainGUIClient extends JFrame {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Componentes da UI
    private JTextArea chatArea;
    private JTextField inputField;
    private JLabel lblItemName;
    private JLabel lblHighestBid;
    private JLabel lblTimeLeft;
    private JTextField bidField;
    private JButton btnBid;
    
    // Painéis
    private JPanel bottomArea;
    private JPanel bidPanel;
    private JPanel adminPanel;
    private JTextField txtItemName;
    private JTextField txtItemPrice;
    
    public MainGUIClient() {
        setTitle("BidRoom - Leilão Distribuído");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // --- PAINEL SUPERIOR (LEILÃO) ---
        JPanel auctionPanel = new JPanel(new GridLayout(4, 1));
        auctionPanel.setBorder(BorderFactory.createTitledBorder("Mesa do Leiloeiro"));
        auctionPanel.setBackground(new Color(240, 248, 255));
        
        lblItemName = new JLabel("Aguardando leilão começar...", SwingConstants.CENTER);
        lblItemName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblItemName.setForeground(new Color(25, 25, 112));
        
        lblHighestBid = new JLabel("Maior Lance: R$ 0.00", SwingConstants.CENTER);
        lblHighestBid.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        
        lblTimeLeft = new JLabel("Tempo: --", SwingConstants.CENTER);
        lblTimeLeft.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTimeLeft.setForeground(new Color(178, 34, 34));
        
        bottomArea = new JPanel(new BorderLayout());
        bottomArea.setOpaque(false);
        
        // Área de dar lance (Comprador)
        bidPanel = new JPanel(new FlowLayout());
        bidPanel.setOpaque(false);
        bidField = new JTextField(10);
        btnBid = new JButton("Dar Lance!");
        btnBid.setBackground(new Color(46, 139, 87));
        btnBid.setForeground(Color.WHITE);
        btnBid.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        bidPanel.add(new JLabel("Seu valor (R$):"));
        bidPanel.add(bidField);
        bidPanel.add(btnBid);
        
        // FASE 12: Área do Leiloeiro com GridLayout em 2 linhas
        adminPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        adminPanel.setOpaque(false);
        
        // Linha 1: Cadastro de itens
        JPanel adminRow1 = new JPanel(new FlowLayout());
        adminRow1.setOpaque(false);
        txtItemName = new JTextField(10);
        txtItemPrice = new JTextField(5);
        JButton btnAddItem = new JButton("Cadastrar Item");
        adminRow1.add(new JLabel("Nome:"));
        adminRow1.add(txtItemName);
        adminRow1.add(new JLabel("Preço:"));
        adminRow1.add(txtItemPrice);
        adminRow1.add(btnAddItem);

        // Linha 2: Controles de Tempo Real
        JPanel adminRow2 = new JPanel(new FlowLayout());
        adminRow2.setOpaque(false);
        JButton btnStartAuction = new JButton("▶ Iniciar Próximo Leilão");
        JButton btnAddTime = new JButton("⏳ +5s");
        JButton btnForceEnd = new JButton("🛑 Encerrar Leilão");
        adminRow2.add(btnStartAuction);
        adminRow2.add(btnAddTime);
        adminRow2.add(btnForceEnd);
        
        adminPanel.add(adminRow1);
        adminPanel.add(adminRow2);
        
        // Por padrão, mostra visão de comprador
        bottomArea.add(bidPanel, BorderLayout.CENTER);
        
        auctionPanel.add(lblItemName);
        auctionPanel.add(lblHighestBid);
        auctionPanel.add(lblTimeLeft);
        auctionPanel.add(bottomArea);

        // --- CENTRO (CHAT) ---
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        chatArea.setBackground(new Color(30, 30, 30));
        chatArea.setForeground(new Color(220, 220, 220));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Chat & Eventos do Servidor"));

        // --- PAINEL INFERIOR (ENVIAR) ---
        JPanel chatInputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton btnSend = new JButton("Enviar Chat");
        
        chatInputPanel.add(inputField, BorderLayout.CENTER);
        chatInputPanel.add(btnSend, BorderLayout.EAST);

        add(auctionPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(chatInputPanel, BorderLayout.SOUTH);

        // --- EVENTOS DE CLIQUE ---
        btnBid.addActionListener(e -> enviarLance());
        btnSend.addActionListener(e -> enviarChat());
        inputField.addActionListener(e -> enviarChat()); 
        
        // Botões do Admin
        btnAddItem.addActionListener(e -> {
            String name = txtItemName.getText().trim();
            String price = txtItemPrice.getText().trim();
            if(!name.isEmpty() && !price.isEmpty()) {
                out.println("ADD_ITEM|" + name + "|" + price);
                txtItemName.setText("");
                txtItemPrice.setText("");
            }
        });
        
        btnStartAuction.addActionListener(e -> out.println("START_AUCTION"));
        
        // FASE 12: Eventos dos novos controles ao vivo
        btnAddTime.addActionListener(e -> out.println("ADD_TIME|5"));
        btnForceEnd.addActionListener(e -> out.println("FORCE_END_AUCTION"));
    }

    private void conectar() {
        String nome = JOptionPane.showInputDialog(this, "Bem-vindo ao BidRoom!\nDigite seu nome de usuário:", "Login", JOptionPane.PLAIN_MESSAGE);
        if (nome == null || nome.trim().isEmpty()) {
            System.exit(0);
        }
        
        // MUTAÇÃO: Tela do Leiloeiro Supremo
        if (nome.equals("admin1207")) {
            bottomArea.remove(bidPanel);
            bottomArea.add(adminPanel, BorderLayout.CENTER);
            bottomArea.revalidate();
            bottomArea.repaint();
            setTitle("BidRoom - PAINEL DO LEILOEIRO");
            appendChat("[SISTEMA] Autenticado como Leiloeiro Supremo!");
        }

        try {
            socket = new Socket("127.0.0.1", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("LOGIN|" + nome);

            new Thread(() -> {
                try {
                    String mensagem;
                    while ((mensagem = in.readLine()) != null) {
                        processarMensagemServidor(mensagem);
                    }
                } catch (IOException e) {
                    appendChat("[Erro] Conexão perdida: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Não foi possível conectar ao servidor na porta 5000.\nGaranta que o MainServer está rodando!", "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void processarMensagemServidor(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            String[] partes = mensagem.split("\\|", -1);
            String acao = partes[0];

            switch (acao) {
                case "AUCTION_START":
                    lblItemName.setText("Em leilão: " + partes[1]);
                    lblHighestBid.setText("Maior Lance Inicial: R$ " + partes[2]);
                    appendChat("🔔 O LEILÃO COMEÇOU! Item: " + partes[1]);
                    break;
                case "TIME":
                    lblTimeLeft.setText("Tempo restante: " + partes[1] + "s");
                    if (Integer.parseInt(partes[1]) <= 5) {
                        lblTimeLeft.setForeground(Color.RED);
                    } else {
                        lblTimeLeft.setForeground(new Color(178, 34, 34));
                    }
                    break;
                case "NEW_BID":
                    lblHighestBid.setText("Líder: " + partes[1] + " | Valor: R$ " + partes[2]);
                    appendChat("💰 " + partes[1] + " assumiu a liderança com R$ " + partes[2]);
                    break;
                case "AUCTION_END":
                    lblTimeLeft.setText("TEMPO ENCERRADO");
                    appendChat("🛑 [FIM] " + partes[1]);
                    break;
                case "CHAT":
                    appendChat("🗣️ [" + partes[1] + "]: " + partes[2]);
                    break;
                case "LOGIN_OK":
                case "INFO":
                case "ERROR":
                    String texto = mensagem.substring(acao.length() + 1);
                    appendChat("[" + acao + "] " + texto);
                    break;
                default:
                    appendChat("[Desconhecido] " + mensagem);
                    break;
            }
        });
    }

    private void enviarLance() {
        String valor = bidField.getText().trim();
        if (!valor.isEmpty()) {
            out.println("BID|" + valor);
            bidField.setText("");
        }
    }

    private void enviarChat() {
        String texto = inputField.getText().trim();
        if (!texto.isEmpty()) {
            if (texto.equalsIgnoreCase("PERFIL")) {
                out.println("PERFIL");
            } else {
                out.println("CHAT|" + texto);
            }
            inputField.setText("");
        }
    }

    private void appendChat(String texto) {
        chatArea.append(texto + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainGUIClient client = new MainGUIClient();
            client.setVisible(true);
            client.conectar();
        });
    }
}
