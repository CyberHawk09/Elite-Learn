package com.elitelearn;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

public class EliteLearnGUI extends JFrame {

    // --- JetBrains Darcula Theme Colors ---
    private static final Color BG_MAIN = new Color(30, 31, 34);
    private static final Color BG_CARD = new Color(43, 43, 43);
    private static final Color BG_INPUT = new Color(30, 31, 34);
    private static final Color BORDER_COLOR = new Color(60, 63, 65);
    private static final Color TEXT_PRIMARY = new Color(187, 187, 187);
    private static final Color TEXT_SECONDARY = new Color(130, 130, 130);
    private static final Color ACCENT_BLUE = new Color(53, 116, 240);
    private static final Color ACCENT_RED = new Color(255, 85, 85);
    private static final Font FONT_MAIN = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.ITALIC, 14);

    // --- App State ---
    private final List<Deck> decks = new java.util.ArrayList<>();
    private String apiKey = "";
    private final AIService aiService = new AIService();
    private Deck currentStudyDeck;
    private Flashcard currentStudyCard;
    
    // New State for PDF & Quantity
    private File selectedPdf = null;

    // --- UI Components ---
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private DefaultListModel<Deck> deckListModel;
    private JPasswordField apiKeyField;
    private JTextArea notesArea;
    private JLabel studyQuestionLabel;
    private JLabel studyAnswerLabel;
    private JButton revealBtn;
    
    // New UI Components
    private JSpinner numCardsSpinner;
    private JLabel pdfStatusLabel;

    public EliteLearnGUI() {
        setTitle("EliteLearn");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_MAIN);
        setFont(FONT_MAIN);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG_MAIN);

        contentPanel.add(buildMainMenu(), "MAIN");
        contentPanel.add(buildApiKeyScreen(), "API");
        contentPanel.add(buildDecksScreen(), "DECKS");
        contentPanel.add(buildNewDeckScreen(), "NEW_DECK");
        contentPanel.add(buildStudyScreen(), "STUDY");

        add(contentPanel);
    }

    // ==========================================
    // 1. MAIN MENU
    // ==========================================
    private JPanel buildMainMenu() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_MAIN);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 10, 0);

        JLabel title = new JLabel("EliteLearn");
        title.setFont(FONT_TITLE);
        title.setForeground(Color.WHITE);
        panel.add(title, gbc);

        JLabel subtitle = new JLabel("Leitner-inspired digital spaced learning system");
        subtitle.setFont(FONT_SUBTITLE);
        subtitle.setForeground(TEXT_SECONDARY);
        panel.add(subtitle, gbc);

        gbc.insets = new Insets(30, 0, 10, 0);

        JButton apiKeyBtn = createStyledButton("API Key", ACCENT_BLUE, false);
        apiKeyBtn.addActionListener(e -> cardLayout.show(contentPanel, "API"));
        panel.add(apiKeyBtn, gbc);

        JButton decksBtn = createStyledButton("Decks", ACCENT_BLUE, false);
        decksBtn.addActionListener(e -> {
            refreshDeckList();
            cardLayout.show(contentPanel, "DECKS");
        });
        panel.add(decksBtn, gbc);

        JButton exitBtn = createStyledButton("Exit", ACCENT_RED, false);
        exitBtn.addActionListener(e -> System.exit(0));
        panel.add(exitBtn, gbc);

        return panel;
    }

    // ==========================================
    // 2. API KEY SCREEN
    // ==========================================
    private JPanel buildApiKeyScreen() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_MAIN);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 10, 0);

        JLabel label = new JLabel("Enter your Google Gemini API Key:");
        label.setForeground(TEXT_PRIMARY);
        panel.add(label, gbc);

        apiKeyField = new JPasswordField(30);
        apiKeyField.setFont(FONT_MAIN);
        apiKeyField.setBackground(BG_INPUT);
        apiKeyField.setForeground(TEXT_PRIMARY);
        apiKeyField.setCaretColor(TEXT_PRIMARY);
        apiKeyField.setBorder(new LineBorder(BORDER_COLOR, 1));
        apiKeyField.setMargin(new Insets(10, 10, 10, 10));
        panel.add(apiKeyField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        btnPanel.setBackground(BG_MAIN);

        JButton confirmBtn = createStyledButton("Confirm", ACCENT_BLUE, false);
        confirmBtn.addActionListener(e -> {
            apiKey = new String(apiKeyField.getPassword());
            cardLayout.show(contentPanel, "MAIN");
        });

        JButton cancelBtn = createStyledButton("Cancel", ACCENT_BLUE, false);
        cancelBtn.addActionListener(e -> cardLayout.show(contentPanel, "MAIN"));

        btnPanel.add(confirmBtn);
        btnPanel.add(cancelBtn);

        gbc.insets = new Insets(30, 0, 0, 0);
        panel.add(btnPanel, gbc);

        return panel;
    }

    // ==========================================
    // 3. DECKS SCREEN
    // ==========================================
    private JPanel buildDecksScreen() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_MAIN);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(BG_MAIN);
        JButton backBtn = createStyledButton("< Back to Main Menu", TEXT_SECONDARY, false);
        backBtn.addActionListener(e -> cardLayout.show(contentPanel, "MAIN"));
        topBar.add(backBtn);
        mainPanel.add(topBar, BorderLayout.NORTH);

        deckListModel = new DefaultListModel<>();
        JList<Deck> deckList = new JList<>(deckListModel);
        deckList.setCellRenderer(new DeckCellRenderer());
        deckList.setFixedCellHeight(60);
        deckList.setBackground(BG_MAIN);
        deckList.setSelectionBackground(new Color(53, 116, 240, 50));

        deckList.setDragEnabled(true);
        deckList.setDropMode(DropMode.INSERT);
        deckList.setTransferHandler(new DeckTransferHandler());

        deckList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int index = deckList.locationToIndex(e.getPoint());
                if (index != -1) {
                    Rectangle cellBounds = deckList.getCellBounds(index, index);
                    if (cellBounds != null && cellBounds.contains(e.getPoint())) {
                        if (e.getX() > cellBounds.x + cellBounds.width - 90) {
                            decks.remove(index);
                            refreshDeckList();
                        } else {
                            startStudySession(deckListModel.get(index));
                        }
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(deckList);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_MAIN);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomBar.setBackground(BG_CARD);
        bottomBar.setBorder(new LineBorder(BORDER_COLOR, 1));
        bottomBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 60));

        JButton addBtn = new JButton("+");
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 24));
        addBtn.setForeground(ACCENT_BLUE);
        addBtn.setBackground(BG_CARD);
        addBtn.setBorder(new LineBorder(ACCENT_BLUE, 1));
        addBtn.setPreferredSize(new Dimension(50, 40));
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> {
            notesArea.setText("");
            selectedPdf = null;
            pdfStatusLabel.setText("No PDF selected (Using text area)");
            notesArea.setEnabled(true);
            cardLayout.show(contentPanel, "NEW_DECK");
        });

        bottomBar.add(addBtn);
        mainPanel.add(bottomBar, BorderLayout.SOUTH);

        return mainPanel;
    }

    // ==========================================
    // 4. NEW DECK SCREEN (Added Spinner & PDF Upload)
    // ==========================================
    private JPanel buildNewDeckScreen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- Top Options Panel ---
        JPanel topOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        topOptions.setBackground(BG_MAIN);
        
        JLabel numLabel = new JLabel("Cards (1-100):");
        numLabel.setForeground(TEXT_PRIMARY);
        topOptions.add(numLabel);

        numCardsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1));
        JComponent editor = numCardsSpinner.getEditor();
        ((JSpinner.DefaultEditor) editor).getTextField().setBackground(BG_INPUT);
        ((JSpinner.DefaultEditor) editor).getTextField().setForeground(TEXT_PRIMARY);
        ((JSpinner.DefaultEditor) editor).getTextField().setCaretColor(TEXT_PRIMARY);
        topOptions.add(numCardsSpinner);

        JButton uploadPdfBtn = createStyledButton("Upload PDF", ACCENT_BLUE, false);
        uploadPdfBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedPdf = fc.getSelectedFile();
                pdfStatusLabel.setText("Selected: " + selectedPdf.getName());
                pdfStatusLabel.setForeground(ACCENT_BLUE);
                notesArea.setEnabled(false);
                notesArea.setText("");
            }
        });
        topOptions.add(uploadPdfBtn);

        JButton clearPdfBtn = createStyledButton("Clear PDF", TEXT_SECONDARY, false);
        clearPdfBtn.addActionListener(e -> {
            selectedPdf = null;
            pdfStatusLabel.setText("No PDF selected (Using text area)");
            pdfStatusLabel.setForeground(TEXT_SECONDARY);
            notesArea.setEnabled(true);
        });
        topOptions.add(clearPdfBtn);

        pdfStatusLabel = new JLabel("No PDF selected (Using text area)");
        pdfStatusLabel.setForeground(TEXT_SECONDARY);
        topOptions.add(pdfStatusLabel);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel title = new JLabel("Create New Deck");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(topOptions, BorderLayout.SOUTH);
        
        panel.add(headerPanel, BorderLayout.NORTH);

        // --- Center Text Area ---
        notesArea = new JTextArea();
        notesArea.setFont(FONT_MAIN);
        notesArea.setBackground(BG_INPUT);
        notesArea.setForeground(TEXT_PRIMARY);
        notesArea.setCaretColor(TEXT_PRIMARY);
        notesArea.setBorder(new LineBorder(BORDER_COLOR, 1));
        notesArea.setMargin(new Insets(10, 10, 10, 10));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(notesArea);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- Bottom Buttons ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(BG_MAIN);

        JButton cancelBtn = createStyledButton("Cancel", ACCENT_BLUE, false);
        cancelBtn.addActionListener(e -> cardLayout.show(contentPanel, "DECKS"));

        JButton generateBtn = createStyledButton("Generate Flashcards", ACCENT_BLUE, false);
        generateBtn.addActionListener(e -> generateDeck());

        btnPanel.add(cancelBtn);
        btnPanel.add(generateBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ==========================================
    // 5. STUDY SCREEN
    // ==========================================
    private JPanel buildStudyScreen() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        studyQuestionLabel = new JLabel("<html><center>Question goes here</center></html>");
        studyQuestionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        studyQuestionLabel.setForeground(Color.WHITE);
        studyQuestionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(studyQuestionLabel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.setOpaque(false);

        studyAnswerLabel = new JLabel("<html><center>Answer goes here</center></html>");
        studyAnswerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        studyAnswerLabel.setForeground(ACCENT_BLUE);
        studyAnswerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        studyAnswerLabel.setVisible(false);
        studyAnswerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        southPanel.add(studyAnswerLabel);
        southPanel.add(Box.createVerticalStrut(15));

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        bottomBar.setOpaque(false);

        revealBtn = createStyledButton("Reveal Answer", ACCENT_BLUE, false);
        revealBtn.addActionListener(e -> {
            boolean isCurrentlyVisible = studyAnswerLabel.isVisible();
            studyAnswerLabel.setVisible(!isCurrentlyVisible);
            revealBtn.setText(isCurrentlyVisible ? "Reveal Answer" : "Hide Answer");
        });

        JButton correctBtn = createStyledButton("Correct", new Color(46, 160, 67), false);
        correctBtn.addActionListener(e -> handleStudyResult(true));

        JButton incorrectBtn = createStyledButton("Incorrect", ACCENT_RED, false);
        incorrectBtn.addActionListener(e -> handleStudyResult(false));

        bottomBar.add(revealBtn);
        bottomBar.add(correctBtn);
        bottomBar.add(incorrectBtn);

        southPanel.add(bottomBar);
        panel.add(southPanel, BorderLayout.SOUTH);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.setBackground(BG_MAIN);
        JButton backBtn = createStyledButton("< Back to Decks", TEXT_SECONDARY, false);
        backBtn.addActionListener(e -> cardLayout.show(contentPanel, "DECKS"));
        topBar.add(backBtn);
        panel.add(topBar, BorderLayout.NORTH);

        return panel;
    }

    // ==========================================
    // LOGIC & HELPERS
    // ==========================================

    private void refreshDeckList() {
        deckListModel.clear();
        for (Deck d : decks) {
            deckListModel.addElement(d);
        }
    }

    private void generateDeck() {
        int numCards = (int) numCardsSpinner.getValue();
        
        // Validate inputs
        if (selectedPdf == null && notesArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter notes or upload a PDF first.");
            return;
        }
        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please set your API key first.");
            return;
        }

        // Run AI in background thread
        SwingWorker<List<Flashcard>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Flashcard> doInBackground() throws Exception {
                if (selectedPdf != null) {
                    return aiService.generateFlashcardsFromPdf(selectedPdf, numCards, apiKey);
                } else {
                    return aiService.generateFlashcards(notesArea.getText(), numCards, apiKey);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Flashcard> cards = get();
                    String deckName = JOptionPane.showInputDialog(EliteLearnGUI.this, "Enter a name for your new deck:", "New Deck", JOptionPane.PLAIN_MESSAGE);
                    if (deckName != null && !deckName.trim().isEmpty()) {
                        Deck newDeck = new Deck(deckName, cards);
                        decks.add(newDeck);
                        refreshDeckList();
                        cardLayout.show(contentPanel, "DECKS");
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(EliteLearnGUI.this, "Error generating cards: " + e.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void startStudySession(Deck deck) {
        if (deck.isEmpty()) {
            JOptionPane.showMessageDialog(this, "This deck is empty!");
            return;
        }
        currentStudyDeck = deck;
        loadNextCard();
        cardLayout.show(contentPanel, "STUDY");
    }

    private void loadNextCard() {
        currentStudyCard = currentStudyDeck.pickNextCard();
        if (currentStudyCard == null) {
            JOptionPane.showMessageDialog(this, "Deck is empty!");
            cardLayout.show(contentPanel, "DECKS");
            return;
        }
        studyQuestionLabel.setText("<html><center>" + currentStudyCard.getQuestion() + "</center></html>");
        studyAnswerLabel.setText("<html><center>" + currentStudyCard.getAnswer() + "</center></html>");
        studyAnswerLabel.setVisible(false);
        revealBtn.setText("Reveal Answer"); 
        revealBtn.setEnabled(true);
    }

    private void handleStudyResult(boolean isCorrect) {
        currentStudyDeck.updateWeight(currentStudyCard, isCorrect);
        loadNextCard();
    }

    // --- Custom UI Helpers ---

    private JButton createStyledButton(String text, Color themeColor, boolean isSolid) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MAIN);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.CENTER);

        if (isSolid) {
            btn.setBackground(themeColor);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(BG_MAIN);
            btn.setForeground(themeColor);
        }
        btn.setBorder(new LineBorder(themeColor, 1, true));
        btn.setMargin(new Insets(12, 35, 12, 35)); 

        return btn;
    }

    // --- Drag and Drop & List Rendering ---

    private class DeckCellRenderer extends JPanel implements ListCellRenderer<Deck> {
        private JLabel titleLabel = new JLabel();
        private JButton deleteBtn = new JButton("Delete");

        public DeckCellRenderer() {
            setLayout(new BorderLayout(10, 0));
            setBackground(BG_CARD);
            setBorder(new LineBorder(BORDER_COLOR, 1));

            titleLabel.setForeground(TEXT_PRIMARY);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setOpaque(false);

            deleteBtn.setForeground(ACCENT_RED);
            deleteBtn.setBackground(BG_CARD);
            deleteBtn.setBorder(new LineBorder(ACCENT_RED, 1));
            deleteBtn.setFocusPainted(false);
            deleteBtn.setPreferredSize(new Dimension(80, 30));

            add(titleLabel, BorderLayout.CENTER);
            add(deleteBtn, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Deck> list, Deck value, int index, boolean isSelected, boolean cellHasFocus) {
            titleLabel.setText(value.getName() + "  (" + value.size() + " cards)");
            setBackground(isSelected ? new Color(53, 116, 240, 40) : BG_CARD);
            return this;
        }
    }

    @SuppressWarnings("unchecked")
    private class DeckTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) { return MOVE; }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList<Deck> list = (JList<Deck>) c;
            int index = list.getSelectedIndex();
            if (index < 0) return null;
            return new StringSelection(String.valueOf(index));
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
            int dropIndex = dl.getIndex();

            try {
                String data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                int dragIndex = Integer.parseInt(data);

                if (dragIndex < 0 || dragIndex >= decks.size()) return false;
                if (dropIndex < 0) dropIndex = decks.size();
                if (dropIndex > decks.size()) dropIndex = decks.size();
                if (dragIndex == dropIndex || dragIndex == dropIndex - 1) return false;

                Deck draggedDeck = decks.remove(dragIndex);
                if (dropIndex > dragIndex) dropIndex--;
                decks.add(dropIndex, draggedDeck);

                refreshDeckList();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) { 
                e.printStackTrace();
            }
            new EliteLearnGUI().setVisible(true);
        });
    }
}