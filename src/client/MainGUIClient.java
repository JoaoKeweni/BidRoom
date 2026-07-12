package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import javax.imageio.ImageIO;

public class MainGUIClient extends JFrame {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Componentes da Aba Principal (Leilão)
    private JTextArea chatArea;
    private JTextField inputField;
    private JLabel lblItemName;
    private JLabel lblHighestBid;
    private JLabel lblTimeLeft;
    private JLabel lblImage;
    private JTextField bidField;
    private JButton btnBid;

    // Painéis Mutantes
    private JPanel bottomArea;
    private JPanel bidPanel;
    private JPanel adminPanel;
    private JTextField txtItemName;
    private JTextField txtItemPrice;
    private JTextField txtItemUrl;

    // FASE 14: Componentes da Aba Perfil
    private JLabel lblAvatar;
    private JLabel lblProfileName;
    private JLabel lblProfileBalance;
    private DefaultListModel<String> listModelItems;
    private JList<String> listItems;

    public MainGUIClient() {
        setTitle("BidRoom - Leilão Distribuído");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // ==========================================
        // CONSTRUÇÃO DA ABA 1: SALÃO PRINCIPAL
        // ==========================================
        JPanel auctionPanel = new JPanel(new BorderLayout());
        auctionPanel.setBorder(BorderFactory.createTitledBorder("Mesa do Leiloeiro"));
        auctionPanel.setBackground(new Color(240, 248, 255));

        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.setOpaque(false);
        lblItemName = new JLabel("Aguardando leilão começar...", SwingConstants.CENTER);
        lblItemName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblItemName.setForeground(new Color(25, 25, 112));

        lblHighestBid = new JLabel("Maior Lance: R$ 0.00", SwingConstants.CENTER);
        lblHighestBid.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        lblTimeLeft = new JLabel("Tempo: --", SwingConstants.CENTER);
        lblTimeLeft.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTimeLeft.setForeground(new Color(178, 34, 34));

        infoPanel.add(lblItemName);
        infoPanel.add(lblHighestBid);
        infoPanel.add(lblTimeLeft);

        lblImage = new JLabel("Nenhuma Foto", SwingConstants.CENTER);
        lblImage.setPreferredSize(new Dimension(200, 200));
        lblImage.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        auctionPanel.add(infoPanel, BorderLayout.CENTER);
        auctionPanel.add(lblImage, BorderLayout.EAST);

        bottomArea = new JPanel(new BorderLayout());
        bottomArea.setOpaque(false);

        // Área do Comprador
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

        // Área do Admin
        adminPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        adminPanel.setOpaque(false);
        JPanel adminRow1 = new JPanel(new FlowLayout());
        adminRow1.setOpaque(false);
        txtItemName = new JTextField(10);
        txtItemPrice = new JTextField(5);
        txtItemUrl = new JTextField(12);
        JButton btnAddItem = new JButton("Cadastrar");
        adminRow1.add(new JLabel("Nome:"));
        adminRow1.add(txtItemName);
        adminRow1.add(new JLabel("R$:"));
        adminRow1.add(txtItemPrice);
        adminRow1.add(new JLabel("URL Foto:"));
        adminRow1.add(txtItemUrl);
        adminRow1.add(btnAddItem);

        JPanel adminRow2 = new JPanel(new FlowLayout());
        adminRow2.setOpaque(false);
        JButton btnStartAuction = new JButton("▶ Iniciar Próximo");
        JButton btnAddTime = new JButton("⏳ +5s");
        JButton btnForceEnd = new JButton("🛑 Encerrar");
        adminRow2.add(btnStartAuction);
        adminRow2.add(btnAddTime);
        adminRow2.add(btnForceEnd);

        adminPanel.add(adminRow1);
        adminPanel.add(adminRow2);

        bottomArea.add(bidPanel, BorderLayout.CENTER);
        auctionPanel.add(bottomArea, BorderLayout.SOUTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        chatArea.setBackground(new Color(30, 30, 30));
        chatArea.setForeground(new Color(220, 220, 220));
        JScrollPane scrollChat = new JScrollPane(chatArea);
        scrollChat.setBorder(BorderFactory.createTitledBorder("Chat & Eventos"));

        JPanel chatInputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JButton btnSend = new JButton("Enviar Chat");
        chatInputPanel.add(inputField, BorderLayout.CENTER);
        chatInputPanel.add(btnSend, BorderLayout.EAST);

        // Agrupa tudo do leilão em um painel único
        JPanel mainTab = new JPanel(new BorderLayout());
        mainTab.add(auctionPanel, BorderLayout.NORTH);
        mainTab.add(scrollChat, BorderLayout.CENTER);
        mainTab.add(chatInputPanel, BorderLayout.SOUTH);

        // ==========================================
        // CONSTRUÇÃO DA ABA 2: MEU PERFIL (FASE 14)
        // ==========================================
        JPanel profileTab = new JPanel(new BorderLayout());
        profileTab.setBackground(new Color(45, 45, 45)); // Fundo escuro premium

        JPanel profileHeader = new JPanel(new BorderLayout());
        profileHeader.setOpaque(false);
        profileHeader.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        lblAvatar = new JLabel("Buscando Avatar...", SwingConstants.CENTER);
        lblAvatar.setPreferredSize(new Dimension(200, 200));
        lblAvatar.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));

        JPanel statsPanel = new JPanel(new GridLayout(2, 1));
        statsPanel.setOpaque(false);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(50, 40, 50, 40));
        lblProfileName = new JLabel("Usuário", SwingConstants.LEFT);
        lblProfileName.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblProfileName.setForeground(Color.WHITE);
        lblProfileBalance = new JLabel("Carteira: R$ --", SwingConstants.LEFT);
        lblProfileBalance.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblProfileBalance.setForeground(new Color(50, 205, 50)); // LimeGreen

        statsPanel.add(lblProfileName);
        statsPanel.add(lblProfileBalance);

        profileHeader.add(lblAvatar, BorderLayout.WEST);
        profileHeader.add(statsPanel, BorderLayout.CENTER);

        listModelItems = new DefaultListModel<>();
        listModelItems.addElement("Você ainda não possui itens. Ganhe um leilão primeiro!");
        listItems = new JList<>(listModelItems);
        listItems.setFont(new Font("Segoe UI", Font.BOLD, 18));
        listItems.setBackground(new Color(60, 60, 60));
        listItems.setForeground(new Color(255, 215, 0)); // Dourado pros itens
        listItems.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollInventory = new JScrollPane(listItems);
        scrollInventory.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY),
                "Meu Cofre de Riquezas", 0, 0, new Font("Segoe UI", Font.BOLD, 16), Color.WHITE));

        JButton btnRefreshProfile = new JButton("🔄 Sincronizar com Servidor");
        btnRefreshProfile.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnRefreshProfile.setBackground(new Color(70, 130, 180));
        btnRefreshProfile.setForeground(Color.WHITE);

        profileTab.add(profileHeader, BorderLayout.NORTH);
        profileTab.add(scrollInventory, BorderLayout.CENTER);
        profileTab.add(btnRefreshProfile, BorderLayout.SOUTH);

        // ==========================================
        // ADICIONAR TUDO NO JTABBEDPANE
        // ==========================================
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.addTab("Salão Principal", mainTab);
        tabbedPane.addTab("Meu Perfil e Inventário", profileTab);

        add(tabbedPane, BorderLayout.CENTER);

        // --- EVENTOS DE CLIQUES ---
        btnBid.addActionListener(e -> enviarLance());
        btnSend.addActionListener(e -> enviarChat());
        inputField.addActionListener(e -> enviarChat());
        btnRefreshProfile.addActionListener(e -> out.println("PERFIL")); // Ao clicar no refresh, pede pro servidor o
                                                                         // perfil atualizado

        btnAddItem.addActionListener(e -> {
            String name = txtItemName.getText().trim();
            String price = txtItemPrice.getText().trim();
            String url = txtItemUrl.getText().trim();
            if (!name.isEmpty() && !price.isEmpty() && !url.isEmpty()) {
                out.println("ADD_ITEM|" + name + "|" + price + "|" + url);
                txtItemName.setText("");
                txtItemPrice.setText("");
                txtItemUrl.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Preencha Nome, Preço e URL!");
            }
        });

        btnStartAuction.addActionListener(e -> out.println("START_AUCTION"));
        btnAddTime.addActionListener(e -> out.println("ADD_TIME|5"));
        btnForceEnd.addActionListener(e -> out.println("FORCE_END_AUCTION"));
    }

    private void conectar() {
        String ip = JOptionPane.showInputDialog(this,
                "Para jogar em rede, digite o IP do Servidor (Ex: 192.168.X.X)\nOu deixe em branco para jogar apenas no seu computador:",
                "127.0.0.1");
        if (ip == null)
            System.exit(0);
        if (ip.trim().isEmpty())
            ip = "127.0.0.1";

        String nome = JOptionPane.showInputDialog(this, "Bem-vindo ao BidRoom!\nDigite seu nome de usuário:", "Login",
                JOptionPane.PLAIN_MESSAGE);
        if (nome == null || nome.trim().isEmpty())
            System.exit(0);

        // Configura a aba de perfil imediatamente
        lblProfileName.setText(nome);
        carregarAvatarAssincrono(nome);

        if (nome.equals("admin1207")) {
            bottomArea.remove(bidPanel);
            bottomArea.add(adminPanel, BorderLayout.CENTER);
            bottomArea.revalidate();
            bottomArea.repaint();
            setTitle("BidRoom - PAINEL DO LEILOEIRO");
            appendChat("[SISTEMA] Autenticado como Leiloeiro Supremo!");
        }

        try {
            socket = new Socket(ip, 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("LOGIN|" + nome);

            // Pede o inventário inicial assim que loga
            out.println("PERFIL");

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
            JOptionPane.showMessageDialog(this, "Não foi possível conectar ao servidor.");
            System.exit(0);
        }
    }

    // FASE 14: Busca o Avatar no DiceBear usando o nome como Semente
    private void carregarAvatarAssincrono(String nome) {
        new Thread(() -> {
            try {
                // A API gera um pixel art baseado exclusivamente na string do nome
                URL url = new URL("https://api.dicebear.com/7.x/avataaars/png?seed=" + nome + "&size=200");
                Image image = ImageIO.read(url);
                if (image != null) {
                    ImageIcon icon = new ImageIcon(image);
                    SwingUtilities.invokeLater(() -> {
                        lblAvatar.setText(null);
                        lblAvatar.setIcon(icon);
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> lblAvatar.setText("Offline"));
            }
        }).start();
    }

    private void carregarImagemDaWeb(String imageUrl) {
        lblImage.setIcon(null);
        lblImage.setText("Carregando foto...");
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                Image image = ImageIO.read(url);
                if (image != null) {
                    Image scaled = image.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(scaled);
                    SwingUtilities.invokeLater(() -> {
                        lblImage.setText(null);
                        lblImage.setIcon(icon);
                    });
                } else {
                    SwingUtilities.invokeLater(() -> lblImage.setText("Imagem Inválida"));
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> lblImage.setText("Falha ao carregar"));
            }
        }).start();
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
                    if (partes.length >= 4)
                        carregarImagemDaWeb(partes[3]);
                    break;
                case "TIME":
                    lblTimeLeft.setText("Tempo restante: " + partes[1] + "s");
                    if (Integer.parseInt(partes[1]) <= 5)
                        lblTimeLeft.setForeground(Color.RED);
                    else
                        lblTimeLeft.setForeground(new Color(178, 34, 34));
                    break;
                case "NEW_BID":
                    lblHighestBid.setText("Líder: " + partes[1] + " | Valor: R$ " + partes[2]);
                    appendChat("💰 " + partes[1] + " assumiu a liderança com R$ " + partes[2]);
                    break;
                case "AUCTION_END":
                    lblTimeLeft.setText("TEMPO ENCERRADO");
                    appendChat("🛑 [FIM] " + partes[1]);
                    lblImage.setIcon(null);
                    lblImage.setText("Aguardando próximo...");
                    // Ao acabar o leilão, nós automaticamente sincronizamos a conta para o vencedor
                    // ver o item dele caindo no inventário!
                    out.println("PERFIL");
                    break;
                case "PROFILE_DATA":
                    // FASE 14: Recebe e injeta os dados na Aba de Perfil
                    lblProfileBalance.setText("Carteira: R$ " + partes[1]);
                    listModelItems.clear();
                    if (partes.length > 2 && !partes[2].isEmpty()) {
                        String[] items = partes[2].split(",");
                        for (String it : items) {
                            listModelItems.addElement(" - " + it);
                        }
                    } else {
                        listModelItems.addElement("Você ainda não possui itens no cofre.");
                    }
                    appendChat("[SISTEMA] Inventário e Saldo sincronizados com o Banco Central.");
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
            out.println("CHAT|" + texto);
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
