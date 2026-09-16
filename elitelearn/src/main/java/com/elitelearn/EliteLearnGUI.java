package com.elitelearn;

import com.google.gson.Gson;
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
import java.nio.file.Files;
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
    private static final Color ACCENT_GREEN = new Color(46, 160, 67);
    private static final Font FONT_MAIN = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.ITALIC, 14);

    // --- App State ---
    private final List<Deck> decks = new java.util.ArrayList<>();
    private String apiKey = "";
    private final AIService aiService = new AIService();
    private final Gson gson = new Gson();
    private Deck currentStudyDeck;
    private Flashcard currentStudyCard;
    private File selectedPdf = null;

    // --- UI Components ---
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private DefaultListModel<Deck> deckListModel;
    private JPasswordField apiKeyField;
    private JTextArea notesArea;
    private JLabel studyQuestionLabel;
    private JLabel studyAnswerLabel;
    private JSpinner numCardsSpinner;
    private JLabel pdfStatusLabel;

    // --- Study Screen Specific Components ---
    private JPanel inputPanel;
    private JTextField userInputField;
    private JButton submitAnswerBtn;
    private JPanel feedbackPanel;
    private JTextArea feedbackArea;
    
    private JButton revealBtn;
    private JButton typeAnswerBtn;
    private JButton skipBtn;
    private JButton correctBtn;
    private JButton incorrectBtn;
    private JButton cancelInputBtn;
    private JButton nextCardBtn;

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

        // --- CONFIRM BUTTON ---
        JButton confirmBtn = createStyledButton("Confirm", ACCENT_BLUE, false);
        confirmBtn.addActionListener(e -> {
            // 1. ONLY update the variable when Confirm is clicked
            apiKey = new String(apiKeyField.getPassword());
            // 2. Clear the field for security
            apiKeyField.setText(""); 
            cardLayout.show(contentPanel, "MAIN");
        });

        // --- CANCEL BUTTON ---
        JButton cancelBtn = createStyledButton("Cancel", ACCENT_BLUE, false);
        cancelBtn.addActionListener(e -> {
            // 1. DO NOT touch the apiKey variable.
            // 2. Just clear the text field so it doesn't linger on screen
            apiKeyField.setText(""); 
            cardLayout.show(contentPanel, "MAIN");
        });

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
                        int rightEdge = cellBounds.x + cellBounds.width;
                        if (e.getX() > rightEdge - 190) {
                            if (e.getX() > rightEdge - 90) {
                                decks.remove(index);
                                refreshDeckList();
                            } else {
                                exportDeck(deckListModel.get(index));
                            }
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
            pdfStatusLabel.setForeground(TEXT_SECONDARY);
            notesArea.setEnabled(true);
            cardLayout.show(contentPanel, "NEW_DECK");
        });

        bottomBar.add(addBtn);
        mainPanel.add(bottomBar, BorderLayout.SOUTH);

        return mainPanel;
    }

    // ==========================================
    // 4. NEW DECK SCREEN
    // ==========================================
    private JPanel buildNewDeckScreen() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG_MAIN);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 10));
        headerPanel.setBackground(BG_MAIN);

        JLabel title = new JLabel("Create New Deck");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        headerPanel.add(title, BorderLayout.NORTH);

        JPanel optionsContainer = new JPanel(new BorderLayout(0, 10));
        optionsContainer.setBackground(BG_MAIN);

        JPanel importRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        importRow.setBackground(BG_MAIN);
        JButton importBtn = createStyledButton("Import Deck from File", ACCENT_BLUE, false);
        importBtn.addActionListener(e -> importDeck());
        importRow.add(importBtn);
        optionsContainer.add(importRow, BorderLayout.NORTH);

        JPanel topOptions = new JPanel(new BorderLayout(15, 0));
        topOptions.setBackground(BG_MAIN);

        JPanel leftControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftControls.setBackground(BG_MAIN);

        JLabel numLabel = new JLabel("Cards (1-100):");
        numLabel.setForeground(TEXT_PRIMARY);
        leftControls.add(numLabel);

        numCardsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1));
        JComponent editor = numCardsSpinner.getEditor();
        ((JSpinner.DefaultEditor) editor).getTextField().setBackground(BG_INPUT);
        ((JSpinner.DefaultEditor) editor).getTextField().setForeground(TEXT_PRIMARY);
        ((JSpinner.DefaultEditor) editor).getTextField().setCaretColor(TEXT_PRIMARY);
        leftControls.add(numCardsSpinner);

        JButton uploadPdfBtn = createStyledButton("Upload PDF", ACCENT_BLUE, false);
        uploadPdfBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedPdf = fc.getSelectedFile();
                SwingUtilities.invokeLater(() -> {
                    pdfStatusLabel.setText("Selected: " + selectedPdf.getName());
                    pdfStatusLabel.setForeground(ACCENT_BLUE);
                    pdfStatusLabel.revalidate();
                    pdfStatusLabel.repaint();
                    topOptions.revalidate();
                    topOptions.repaint();
                    notesArea.setEnabled(false);
                    notesArea.setText("");
                });
            }
        });
        leftControls.add(uploadPdfBtn);

        JButton clearPdfBtn = createStyledButton("Clear PDF", TEXT_SECONDARY, false);
        clearPdfBtn.addActionListener(e -> {
            selectedPdf = null;
            SwingUtilities.invokeLater(() -> {
                pdfStatusLabel.setText("No PDF selected (Using text area)");
                pdfStatusLabel.setForeground(TEXT_SECONDARY);
                pdfStatusLabel.revalidate();
                pdfStatusLabel.repaint();
                topOptions.revalidate();
                topOptions.repaint();
                notesArea.setEnabled(true);
            });
        });
        leftControls.add(clearPdfBtn);

        topOptions.add(leftControls, BorderLayout.WEST);

        pdfStatusLabel = new JLabel("No PDF selected (Using text area)");
        pdfStatusLabel.setForeground(TEXT_SECONDARY);
        topOptions.add(pdfStatusLabel, BorderLayout.CENTER);

        optionsContainer.add(topOptions, BorderLayout.SOUTH);
        headerPanel.add(optionsContainer, BorderLayout.CENTER);

        panel.add(headerPanel, BorderLayout.NORTH);

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
    // 5. STUDY SCREEN (Added Skip & AI Grading)
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

        // 1. Standard Answer Reveal Label
        studyAnswerLabel = new JLabel("<html><center>Answer goes here</center></html>");
        studyAnswerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        studyAnswerLabel.setForeground(ACCENT_BLUE);
        studyAnswerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        studyAnswerLabel.setVisible(false);
        studyAnswerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        southPanel.add(studyAnswerLabel);
        southPanel.add(Box.createVerticalStrut(10));

        // 2. AI Grading Input Panel (Hidden by default)
        inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);
        inputPanel.setVisible(false);
        inputPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        userInputField = new JTextField();
        userInputField.setFont(FONT_MAIN);
        userInputField.setBackground(BG_INPUT);
        userInputField.setForeground(TEXT_PRIMARY);
        userInputField.setCaretColor(TEXT_PRIMARY);
        userInputField.setBorder(new LineBorder(BORDER_COLOR, 1));
        userInputField.setMargin(new Insets(8, 10, 8, 10));
        
        submitAnswerBtn = createStyledButton("Submit to AI", ACCENT_BLUE, false);
        submitAnswerBtn.addActionListener(e -> submitAnswerToAI());
        
        inputPanel.add(userInputField, BorderLayout.CENTER);
        inputPanel.add(submitAnswerBtn, BorderLayout.EAST);
        southPanel.add(inputPanel);
        southPanel.add(Box.createVerticalStrut(10));

        // 3. AI Feedback Panel (Hidden by default)
        feedbackPanel = new JPanel(new BorderLayout());
        feedbackPanel.setOpaque(false);
        feedbackPanel.setVisible(false);
        feedbackPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        feedbackArea = new JTextArea(4, 40);
        feedbackArea.setEditable(false);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setFont(FONT_MAIN);
        feedbackArea.setBackground(BG_CARD);
        feedbackArea.setForeground(TEXT_PRIMARY);
        feedbackArea.setBorder(new LineBorder(BORDER_COLOR, 1));
        feedbackArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane feedbackScroll = new JScrollPane(feedbackArea);
        feedbackScroll.setPreferredSize(new Dimension(600, 100));
        feedbackPanel.add(feedbackScroll, BorderLayout.CENTER);
        southPanel.add(feedbackPanel);
        southPanel.add(Box.createVerticalStrut(15));

        // 4. Button Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        bottomBar.setOpaque(false);

        // State 1 Buttons (Question Only)
        revealBtn = createStyledButton("Reveal Answer", ACCENT_BLUE, false);
        revealBtn.addActionListener(e -> {
            studyAnswerLabel.setVisible(true);
            toggleStudyButtons("REVEALED");
        });

        typeAnswerBtn = createStyledButton("Type My Answer", ACCENT_BLUE, false);
        typeAnswerBtn.addActionListener(e -> {
            inputPanel.setVisible(true);
            userInputField.requestFocus();
            toggleStudyButtons("TYPING");
        });

        skipBtn = createStyledButton("Skip", TEXT_SECONDARY, false);
        skipBtn.addActionListener(e -> loadNextCard()); // Skip doesn't update weight!

        // State 2 Buttons (Revealed)
        correctBtn = createStyledButton("Correct", ACCENT_GREEN, false);
        correctBtn.setVisible(false);
        correctBtn.addActionListener(e -> {
            currentStudyDeck.updateWeight(currentStudyCard, true);
            loadNextCard();
        });

        incorrectBtn = createStyledButton("Incorrect", ACCENT_RED, false);
        incorrectBtn.setVisible(false);
        incorrectBtn.addActionListener(e -> {
            currentStudyDeck.updateWeight(currentStudyCard, false);
            loadNextCard();
        });

        // State 3 Buttons (Typing)
        cancelInputBtn = createStyledButton("Cancel", TEXT_SECONDARY, false);
        cancelInputBtn.setVisible(false);
        cancelInputBtn.addActionListener(e -> {
            inputPanel.setVisible(false);
            userInputField.setText("");
            toggleStudyButtons("INITIAL");
        });

        // State 4 Buttons (Graded)
        nextCardBtn = createStyledButton("Next Card", ACCENT_BLUE, false);
        nextCardBtn.setVisible(false);
        nextCardBtn.addActionListener(e -> loadNextCard());

        bottomBar.add(revealBtn);
        bottomBar.add(typeAnswerBtn);
        bottomBar.add(skipBtn);
        bottomBar.add(correctBtn);
        bottomBar.add(incorrectBtn);
        bottomBar.add(cancelInputBtn);
        bottomBar.add(nextCardBtn);

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

    private void toggleStudyButtons(String state) {
        // Hide all first
        revealBtn.setVisible(false);
        typeAnswerBtn.setVisible(false);
        skipBtn.setVisible(false);
        correctBtn.setVisible(false);
        incorrectBtn.setVisible(false);
        cancelInputBtn.setVisible(false);
        nextCardBtn.setVisible(false);

        switch (state) {
            case "INITIAL":
                revealBtn.setVisible(true);
                typeAnswerBtn.setVisible(true);
                skipBtn.setVisible(true);
                break;
            case "REVEALED":
                correctBtn.setVisible(true);
                incorrectBtn.setVisible(true);
                skipBtn.setVisible(true);
                break;
            case "TYPING":
                cancelInputBtn.setVisible(true);
                skipBtn.setVisible(true);
                break;
            case "GRADED":
                nextCardBtn.setVisible(true);
                break;
        }
    }

    private void submitAnswerToAI() {
        String userAnswer = userInputField.getText().trim();
        if (userAnswer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type an answer first.");
            return;
        }

        // Disable input while waiting for AI
        userInputField.setEnabled(false);
        submitAnswerBtn.setEnabled(false);
        submitAnswerBtn.setText("Grading...");

        SwingWorker<AIService.GradeResult, Void> worker = new SwingWorker<>() {
            @Override
            protected AIService.GradeResult doInBackground() throws Exception {
                return aiService.gradeAnswer(
                        currentStudyCard.getQuestion(), 
                        currentStudyCard.getAnswer(), 
                        userAnswer, 
                        apiKey
                );
            }

            @Override
            protected void done() {
                try {
                    AIService.GradeResult result = get();
                    
                    // Format the feedback with both Score and Grade
                    String feedbackText = String.format(
                        "Score: %d/10 | Grade: %s\n\n" +
                        "Correct Answer:\n%s\n\n" +
                        "AI Feedback:\n%s", 
                        result.score, result.grade, 
                        currentStudyCard.getAnswer(), 
                        result.explanation
                    );
                    feedbackArea.setText(feedbackText);
                    
                    // Color-code the feedback based on the grade
                    if (result.grade != null && result.grade.equalsIgnoreCase("CORRECT")) {
                        feedbackArea.setForeground(new Color(46, 160, 67)); // Green
                    } else if (result.grade != null && result.grade.equalsIgnoreCase("PARTIAL")) {
                        feedbackArea.setForeground(new Color(240, 180, 50)); // Yellow/Orange
                    } else {
                        feedbackArea.setForeground(ACCENT_RED); // Red
                    }

                    feedbackPanel.setVisible(true);
                    inputPanel.setVisible(false);
                    
                    // 3. Update weight strictly based on the numerical score
                    if (result.score >= 9) {
                        // 9-10: Marked correct (decrease weight)
                        currentStudyDeck.updateWeight(currentStudyCard, true);
                    } else if (result.score <= 7) {
                        // 0-7: Marked incorrect (increase weight)
                        currentStudyDeck.updateWeight(currentStudyCard, false);
                    }
                    // If score == 8: Do nothing, weight remains unchanged!
                    
                    toggleStudyButtons("GRADED");
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(EliteLearnGUI.this, "Error grading answer: " + e.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE);
                    // Re-enable input on failure
                    userInputField.setEnabled(true);
                    submitAnswerBtn.setEnabled(true);
                    submitAnswerBtn.setText("Submit to AI");
                }
            }
        };
        worker.execute();
    }

    private void refreshDeckList() {
        deckListModel.clear();
        for (Deck d : decks) {
            deckListModel.addElement(d);
        }
    }

    private void generateDeck() {
        int numCards = (int) numCardsSpinner.getValue();
        if (selectedPdf == null && notesArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter notes or upload a PDF first.");
            return;
        }
        if (apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please set your API key first.");
            return;
        }

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

    private void exportDeck(Deck deck) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Deck");
        fc.setSelectedFile(new File(deck.getName() + ".json"));
        fc.setFileFilter(new FileNameExtensionFilter("JSON Deck File", "json"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".json")) {
                file = new File(file.getAbsolutePath() + ".json");
            }
            try {
                String json = gson.toJson(deck);
                Files.writeString(file.toPath(), json);
                JOptionPane.showMessageDialog(this, "Deck exported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error exporting deck: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importDeck() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import Deck");
        fc.setFileFilter(new FileNameExtensionFilter("JSON Deck File", "json"));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                String json = Files.readString(file.toPath());
                Deck importedDeck = gson.fromJson(json, Deck.class);

                if (importedDeck != null && importedDeck.getName() != null) {
                    decks.add(importedDeck);
                    refreshDeckList();
                    cardLayout.show(contentPanel, "DECKS");
                    JOptionPane.showMessageDialog(this, "Deck '" + importedDeck.getName() + "' imported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid deck file format.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error importing deck: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
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
        
        // Reset all UI states for the new card
        studyAnswerLabel.setVisible(false);
        inputPanel.setVisible(false);
        feedbackPanel.setVisible(false);
        userInputField.setText("");
        userInputField.setEnabled(true);
        submitAnswerBtn.setEnabled(true);
        submitAnswerBtn.setText("Submit to AI");
        
        toggleStudyButtons("INITIAL");
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
        btn.setMargin(new Insets(10, 25, 10, 25)); 

        return btn;
    }

    // --- Drag and Drop & List Rendering ---

    private class DeckCellRenderer extends JPanel implements ListCellRenderer<Deck> {
        private JLabel titleLabel = new JLabel();
        private JButton exportBtn = new JButton("Export");
        private JButton deleteBtn = new JButton("Delete");

        public DeckCellRenderer() {
            setLayout(new BorderLayout(10, 0));
            setBackground(BG_CARD);
            setBorder(new LineBorder(BORDER_COLOR, 1));

            titleLabel.setForeground(TEXT_PRIMARY);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setOpaque(false);

            exportBtn.setForeground(ACCENT_BLUE);
            exportBtn.setBackground(BG_CARD);
            exportBtn.setBorder(new LineBorder(ACCENT_BLUE, 1));
            exportBtn.setFocusPainted(false);
            exportBtn.setPreferredSize(new Dimension(80, 30));

            deleteBtn.setForeground(ACCENT_RED);
            deleteBtn.setBackground(BG_CARD);
            deleteBtn.setBorder(new LineBorder(ACCENT_RED, 1));
            deleteBtn.setFocusPainted(false);
            deleteBtn.setPreferredSize(new Dimension(80, 30));

            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightPanel.setOpaque(false);
            rightPanel.add(exportBtn);
            rightPanel.add(deleteBtn);

            add(titleLabel, BorderLayout.CENTER);
            add(rightPanel, BorderLayout.EAST);
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