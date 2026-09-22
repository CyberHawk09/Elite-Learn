package com.elitelearn;

import javafx.animation.*;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EliteLearnApp extends Application {
    private final List<Deck> decks = new ArrayList<>();
    private final AIService aiService = new AIService();
    private Deck currentStudyDeck;
    private Flashcard currentStudyCard;
    private final List<File> selectedPdfs = new ArrayList<>();
    
    private StackPane rootPane;
    private BorderPane contentPane; 
    private StackPane loadingOverlay;
    private StackPane statusOverlay;
    private Label statusLabel;
    
    private MediaPlayer backgroundMusicPlayer;
    private MediaPlayer clickSoundPlayer;
    
    private Scene mainScene;
    
    private TextArea notesArea;
    private TextField notesField;
    private TextField deckNameField; 
    private Label pdfStatusLabel;
    private ListView<Deck> deckListView;

    // ==========================================
    // FEYNMAN MODE STATE
    // ==========================================
    private String feynmanTitle;
    private String feynmanConcept;
    private String feynmanCurrentQuestion;
    private StringBuilder feynmanHistory;
    private int feynmanTurn = 1;
    private static final int MAX_FEYNMAN_TURNS = 3;
    
    private ScrollPane historyScroll;
    private TextArea feynmanInputArea;
    private Button submitBtn;
    private Button whiteFlagBtn;

    @Override
    public void start(Stage primaryStage) {
        rootPane = new StackPane();
        rootPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        initializeAudio();
        
        Region glow1 = new Region();
        glow1.getStyleClass().add("glow-bg");
        glow1.setPrefSize(1000, 1000);
        glow1.setTranslateX(-400);
        glow1.setTranslateY(-400);
        
        Region glow2 = new Region();
        glow2.getStyleClass().add("glow-bg");
        glow2.setPrefSize(900, 900);
        glow2.setTranslateX(500);
        glow2.setTranslateY(400);
        
        contentPane = new BorderPane();
        contentPane.setStyle("-fx-background-color: transparent;");
        
        loadingOverlay = new StackPane();
        loadingOverlay.getStyleClass().add("loading-overlay");
        loadingOverlay.setVisible(false);
        loadingOverlay.setOpacity(0);
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setStyle("-fx-accent: #3574f0; -fx-min-width: 100; -fx-min-height: 100;");
        
        Label loadingText = new Label("AI is processing...");
        loadingText.setTextFill(Color.WHITE);
        loadingText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        
        VBox loadingBox = new VBox(30, spinner, loadingText);
        loadingBox.setAlignment(Pos.CENTER);
        loadingOverlay.getChildren().add(loadingBox);
        
        statusOverlay = new StackPane();
        statusOverlay.setStyle("-fx-background-color: rgba(15, 15, 15, 0.92);");
        statusOverlay.setVisible(false);
        statusOverlay.setOpacity(0);
        statusOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        statusOverlay.setAlignment(Pos.CENTER);
        statusOverlay.prefWidthProperty().bind(rootPane.widthProperty());
        statusOverlay.prefHeightProperty().bind(rootPane.heightProperty());
        
        statusLabel = new Label();
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 48));
        statusLabel.setTextAlignment(TextAlignment.CENTER);
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(1400);
        statusLabel.setMaxHeight(Double.MAX_VALUE);
        statusLabel.setAlignment(Pos.CENTER);
        StackPane.setAlignment(statusLabel, Pos.CENTER);
        
        statusOverlay.getChildren().setAll(statusLabel);
        
        rootPane.getChildren().addAll(glow1, glow2, contentPane, loadingOverlay, statusOverlay);
        
        mainScene = new Scene(rootPane, 1920, 1080);
        
        mainScene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (notesArea != null && notesArea.isVisible()) {
                Point2D localPoint = notesArea.screenToLocal(event.getScreenX(), event.getScreenY());
                if (!notesArea.contains(localPoint)) {
                    collapseNotesArea();
                }
            }
        });
        
        primaryStage.setTitle("EliteLearn");
        primaryStage.setScene(mainScene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        primaryStage.setMaximized(true);
        primaryStage.show();
        
        showMainMenu();
        
        if (ApiKeyManager.getApiKey() == null) {
            promptForApiKey();
        }
    }

    private void promptForApiKey() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("API Key Required");
        dialog.setHeaderText("Welcome to EliteLearn!");
        dialog.setContentText("Please enter your Google Gemini API Key:");
        dialog.showAndWait().ifPresent(key -> {
            if (!key.trim().isEmpty()) {
                ApiKeyManager.saveApiKey(key.trim());
            }
        });
    }

    private void initializeAudio() {
        try {
            var musicUrl = getClass().getResource("/audio/calm-background.mp3");
            var clickUrl = getClass().getResource("/audio/bubble-click.mp3");
            if (musicUrl != null) {
                backgroundMusicPlayer = new MediaPlayer(new Media(musicUrl.toExternalForm()));
                backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                backgroundMusicPlayer.setVolume(0.18);
                backgroundMusicPlayer.play();
            }
            if (clickUrl != null) {
                clickSoundPlayer = new MediaPlayer(new Media(clickUrl.toExternalForm()));
                clickSoundPlayer.setVolume(0.35);
            }
        } catch (Exception ex) {
            System.err.println("Could not initialize audio: " + ex.getMessage());
        }
    }

    private void playClickSound() {
        if (clickSoundPlayer == null) return;
        clickSoundPlayer.stop();
        clickSoundPlayer.seek(Duration.ZERO);
        clickSoundPlayer.play();
    }

    private Button createGlassButton(String text, boolean isRed) {
        Button btn = new Button(text);
        btn.getStyleClass().add("glass-button");
        if (isRed) btn.getStyleClass().add("red");
        btn.setStyle(btn.getStyle() + "-fx-font-size: 34px; -fx-padding: 24 60 24 60;");
        btn.addEventFilter(ActionEvent.ACTION, e -> playClickSound());
        
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btn);
            st.setToX(1.05); st.setToY(1.05); st.play();
        });
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btn);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
        return btn;
    }

    private Button createSmallGlassButton(String text, boolean isRed) {
        Button btn = new Button(text);
        btn.getStyleClass().add("glass-button");
        if (isRed) btn.getStyleClass().add("red");
        btn.setStyle(btn.getStyle() + "-fx-font-size: 24px; -fx-padding: 16 36 16 36;");
        btn.addEventFilter(ActionEvent.ACTION, e -> playClickSound());
        return btn;
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), loadingOverlay);
        ft.setToValue(show ? 1.0 : 0.0);
        ft.setOnFinished(e -> { if (!show) loadingOverlay.setVisible(false); });
        ft.play();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setTextFill(isError ? Color.web("#ff5555") : Color.web("#2ea043"));
        
        statusOverlay.setVisible(true);
        FadeTransition ftIn = new FadeTransition(Duration.millis(300), statusOverlay);
        ftIn.setToValue(1.0);
        ftIn.play();
        
        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> {
            FadeTransition ftOut = new FadeTransition(Duration.millis(300), statusOverlay);
            ftOut.setToValue(0.0);
            ftOut.setOnFinished(ev -> statusOverlay.setVisible(false));
            ftOut.play();
        });
        delay.play();
    }

    private ScrollPane createTransparentScrollableLabel(String text, Color textColor, double fontSize) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setTextFill(textColor);
        label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, fontSize));
        label.setTextAlignment(TextAlignment.LEFT);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setPadding(new Insets(20));
        
        ScrollPane scrollPane = new ScrollPane(label);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        scrollPane.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-background: transparent; " +
            "-fx-border-color: transparent;"
        );
        
        scrollPane.lookupAll(".viewport").forEach(node ->
            node.setStyle("-fx-background-color: transparent;")
        );
        
        return scrollPane;
    }

    private void showMainMenu() {
        StackPane centerContainer = new StackPane();
        centerContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        VBox menuBox = new VBox(50);
        menuBox.setAlignment(Pos.CENTER);
        
        Label title = new Label("EliteLearn");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 110));
        title.setTextFill(Color.WHITE);
        title.setTextAlignment(TextAlignment.CENTER);
        
        Label subtitle = new Label("Leitner-inspired digital spaced learning system");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 36));
        subtitle.setTextFill(Color.web("#828282"));
        subtitle.setTextAlignment(TextAlignment.CENTER);
        
        Button continueBtn = createGlassButton("Continue", false);
        Button createBtn = createGlassButton("Create New", false);
        Button exitBtn = createGlassButton("Exit", true);
        
        double btnWidth = 450;
        continueBtn.setPrefWidth(btnWidth);
        createBtn.setPrefWidth(btnWidth);
        exitBtn.setPrefWidth(btnWidth);
        
        continueBtn.setOnAction(e -> showDecksScreen());
        createBtn.setOnAction(e -> showCreateScreen());
        exitBtn.setOnAction(e -> System.exit(0));
        
        VBox buttonStack = new VBox(20, continueBtn, createBtn, exitBtn);
        buttonStack.setAlignment(Pos.CENTER);
        
        menuBox.getChildren().addAll(title, subtitle, buttonStack);
        centerContainer.getChildren().add(menuBox);
        
        contentPane.setCenter(centerContainer);
    }

    private void showCreateScreen() {
        VBox screen = new VBox(30);
        screen.setPadding(new Insets(60));
        screen.setAlignment(Pos.TOP_CENTER);
        screen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        Label title = new Label("Create New Deck, Test, or Feynman Session");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 56));
        title.setTextFill(Color.WHITE);
        
        HBox nameBox = new HBox(20);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.setMaxWidth(1200);
        
        Label nameLabel = new Label("Title:");
        nameLabel.setTextFill(Color.web("#bbbbbb"));
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        
        deckNameField = new TextField();
        deckNameField.setPromptText("Enter Session Name...");
        deckNameField.setPrefWidth(600);
        deckNameField.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: #bbbbbb; -fx-border-color: #3c3f41; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 28px; -fx-padding: 10;");
        
        nameBox.getChildren().addAll(nameLabel, deckNameField);
        
        HBox modeBox = new HBox(20);
        modeBox.setAlignment(Pos.CENTER_LEFT);
        modeBox.setMaxWidth(1200);
        
        Label modeLabel = new Label("Mode:");
        modeLabel.setTextFill(Color.web("#bbbbbb"));
        modeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        
        ComboBox<String> modeDropdown = new ComboBox<>();
        modeDropdown.getItems().addAll("Flashcard Deck", "Test (PDF)", "Feynman Mode");
        modeDropdown.setValue("Flashcard Deck");
        modeDropdown.setStyle("-fx-font-size: 28px; -fx-background-color: #2b2b2b; -fx-padding: 5;");
        
        ListCell<String> buttonCell = new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTextFill(Color.WHITE);
                setFont(Font.font("Segoe UI", 28));
                setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white;");
            }
        };
        modeDropdown.setButtonCell(buttonCell);
        
        modeDropdown.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTextFill(Color.WHITE);
                setFont(Font.font("Segoe UI", 28));
                setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white;");
            }
        });
        
        modeBox.getChildren().addAll(modeLabel, modeDropdown);
        
        VBox testOptionsPanel = new VBox(15);
        testOptionsPanel.setMaxWidth(1200);
        testOptionsPanel.setVisible(false);
        testOptionsPanel.setManaged(false);
        
        HBox teacherBox = new HBox(20);
        teacherBox.setAlignment(Pos.CENTER_LEFT);
        
        Label teacherLabel = new Label("Teacher Name:");
        teacherLabel.setTextFill(Color.web("#bbbbbb"));
        teacherLabel.setFont(Font.font("Segoe UI", 32));
        
        TextField teacherField = new TextField();
        teacherField.setPrefWidth(500);
        teacherField.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: #bbbbbb; -fx-border-color: #3c3f41; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 28px; -fx-padding: 10;");
        teacherBox.getChildren().addAll(teacherLabel, teacherField);
        testOptionsPanel.getChildren().add(teacherBox);
        
        modeDropdown.setOnAction(e -> {
            boolean isTest = "Test (PDF)".equals(modeDropdown.getValue());
            testOptionsPanel.setVisible(isTest);
            testOptionsPanel.setManaged(isTest);
        });
        
        notesField = new TextField();
        notesField.setPromptText("Click here to start typing or pasting notes...");
        notesField.setMaxWidth(1200);
        notesField.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: #bbbbbb; -fx-border-color: #3c3f41; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 28px; -fx-padding: 16;");
        
        notesArea = new TextArea();
        notesArea.setWrapText(true);
        notesArea.setVisible(false);
        notesArea.setManaged(false);
        notesArea.setMaxWidth(1200);
        notesArea.setStyle("-fx-background-color: #2b2b2b; -fx-control-inner-background: #2b2b2b; -fx-text-fill: #bbbbbb; -fx-border-color: #3574f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 28px;");
        
        VBox notesContainer = new VBox(15, notesField, notesArea);
        notesContainer.setAlignment(Pos.TOP_CENTER);
        notesContainer.setMaxWidth(1200);
        
        notesField.setOnMouseClicked(e -> expandNotesArea());
        notesField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) expandNotesArea();
        });
        
        HBox pdfBox = new HBox(20);
        pdfBox.setAlignment(Pos.CENTER_LEFT);
        pdfBox.setMaxWidth(1200);
        
        Button uploadPdfBtn = createSmallGlassButton("Upload PDF(s)", false);
        uploadPdfBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            List<File> files = fc.showOpenMultipleDialog(null);
            if (files != null) {
                selectedPdfs.addAll(files);
                updatePdfStatus();
                collapseNotesArea();
                notesField.clear();
            }
        });
        
        Button clearPdfBtn = createSmallGlassButton("Clear PDFs", true);
        clearPdfBtn.setOnAction(e -> {
            selectedPdfs.clear();
            updatePdfStatus();
        });
        
        pdfStatusLabel = new Label("No PDFs selected");
        pdfStatusLabel.setTextFill(Color.web("#828282"));
        pdfStatusLabel.setFont(Font.font("Segoe UI", 26));
        pdfBox.getChildren().addAll(uploadPdfBtn, clearPdfBtn, pdfStatusLabel);
        
        HBox actionBox = new HBox(20);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setMaxWidth(1200);
        
        Button importBtn = createGlassButton("Import Deck JSON", false);
        importBtn.setOnAction(e -> importDeck());
        
        Button cancelBtn = createGlassButton("Cancel", false);
        cancelBtn.setOnAction(e -> showMainMenu());
        
        Button generateBtn = createGlassButton("Create", false);
        generateBtn.setOnAction(e -> {
            String mode = modeDropdown.getValue();
            if ("Test (PDF)".equals(mode)) {
                generateTest(teacherField.getText());
            } else if ("Feynman Mode".equals(mode)) {
                startFeynmanSession();
            } else {
                generateDeck();
            }
        });
        
        actionBox.getChildren().addAll(importBtn, cancelBtn, generateBtn);
        
        screen.getChildren().addAll(title, nameBox, modeBox, testOptionsPanel, notesContainer, pdfBox, actionBox);
        contentPane.setCenter(screen);
    }

    private void expandNotesArea() {
        if (!notesArea.isVisible()) {
            notesArea.setText(notesField.getText());
            notesArea.setVisible(true);
            notesArea.setManaged(true);
            notesField.setVisible(false);
            notesField.setManaged(false);
            notesArea.requestFocus();
            notesArea.setPrefRowCount(1);
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(300), new KeyValue(notesArea.prefRowCountProperty(), 10))
            );
            timeline.play();
        }
    }

    private void collapseNotesArea() {
        if (notesArea != null && notesArea.isVisible()) {
            notesField.setText(notesArea.getText());
            notesArea.setVisible(false);
            notesArea.setManaged(false);
            notesField.setVisible(true);
            notesField.setManaged(true);
        }
    }

    private void updatePdfStatus() {
        if (selectedPdfs.isEmpty()) {
            pdfStatusLabel.setText("No PDFs selected");
            pdfStatusLabel.setTextFill(Color.web("#828282"));
        } else {
            pdfStatusLabel.setText(selectedPdfs.size() + " PDF(s) selected");
            pdfStatusLabel.setTextFill(Color.web("#5c94ff"));
        }
    }

    private void generateDeck() {
        String deckName = deckNameField.getText().trim();
        if (deckName.isEmpty()) {
            showStatus("Please enter a title for your deck.", true);
            return;
        }

        String notes = notesArea != null && notesArea.isVisible() ? notesArea.getText() : (notesField != null ? notesField.getText() : "");
        if (selectedPdfs.isEmpty() && notes.trim().isEmpty()) {
            showStatus("Please enter notes or upload a PDF first.", true);
            return;
        }
        
        showLoading(true);
        Task<List<Flashcard>> task = new Task<>() {
            @Override
            protected List<Flashcard> call() throws Exception {
                String key = ApiKeyManager.getApiKey();
                if (!selectedPdfs.isEmpty()) {
                    return aiService.generateFlashcardsFromPdfs(selectedPdfs, 20, key);
                } else {
                    return aiService.generateFlashcards(notes, 20, key);
                }
            }
        };
        
        task.setOnSucceeded(e -> {
            showLoading(false);
            List<Flashcard> cards = task.getValue();
            decks.add(new Deck(deckName, cards));
            showStatus("Deck '" + deckName + "' created successfully!", false);
            showDecksScreen();
        });
        
        task.setOnFailed(e -> {
            showLoading(false);
            String errorMsg = task.getException().getMessage();
            if (errorMsg.contains("503")) errorMsg = "AI Service is temporarily busy (503). Please try again in a few seconds.";
            showStatus(errorMsg, true);
        });
        new Thread(task).start();
    }

    private void generateTest(String teacherName) {
        String testName = deckNameField.getText().trim();
        if (testName.isEmpty()) {
            showStatus("Please enter a title for your test.", true);
            return;
        }

        String notes = notesArea != null && notesArea.isVisible() ? notesArea.getText() : (notesField != null ? notesField.getText() : "");
        if (selectedPdfs.isEmpty() && notes.trim().isEmpty()) {
            showStatus("Please enter notes or upload a PDF first.", true);
            return;
        }
        
        showLoading(true);
        Task<List<Flashcard>> task = new Task<>() {
            @Override
            protected List<Flashcard> call() throws Exception {
                String key = ApiKeyManager.getApiKey();
                if (!selectedPdfs.isEmpty()) {
                    return aiService.generateFlashcardsFromPdfs(selectedPdfs, 20, key);
                } else {
                    return aiService.generateFlashcards(notes, 20, key);
                }
            }
        };
        
        task.setOnSucceeded(e -> {
            showLoading(false);
            List<Flashcard> questions = task.getValue();
            
            FileChooser testFC = new FileChooser();
            testFC.setInitialFileName(testName + "_Test.pdf");
            testFC.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File testFile = testFC.showSaveDialog(null);
            
            if (testFile != null) {
                try {
                    if (!testFile.getName().toLowerCase().endsWith(".pdf")) {
                        testFile = new File(testFile.getAbsolutePath() + ".pdf");
                    }
                    TestGenerator.generateTestPDF(testName, teacherName, questions, testFile);
                    
                    FileChooser keyFC = new FileChooser();
                    keyFC.setInitialFileName(testName + "_AnswerKey.pdf");
                    keyFC.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
                    File keyFile = keyFC.showSaveDialog(null);
                    
                    if (keyFile != null) {
                        if (!keyFile.getName().toLowerCase().endsWith(".pdf")) {
                            keyFile = new File(keyFile.getAbsolutePath() + ".pdf");
                        }
                        TestGenerator.generateAnswerKeyPDF(testName, teacherName, questions, keyFile);
                        showStatus("Test and Answer Key generated!", false);
                    }
                } catch (Exception ex) {
                    showStatus("Error generating test: " + ex.getMessage(), true);
                }
            }
        });
        
        task.setOnFailed(e -> {
            showLoading(false);
            showStatus("Error: " + task.getException().getMessage(), true);
        });
        new Thread(task).start();
    }

    // ==========================================
    // FEYNMAN MODE LOGIC
    // ==========================================

    private void startFeynmanSession() {
        feynmanTitle = deckNameField.getText().trim();
        if (feynmanTitle.isEmpty()) {
            showStatus("Please enter a title for your session.", true);
            return;
        }

        String notes = notesArea != null && notesArea.isVisible() ? notesArea.getText() : (notesField != null ? notesField.getText() : "");
        if (selectedPdfs.isEmpty() && notes.trim().isEmpty()) {
            showStatus("Please enter notes or upload a PDF to teach me!", true);
            return;
        }

        showLoading(true);
        Task<AIService.FeynmanInit> task = new Task<>() {
            @Override
            protected AIService.FeynmanInit call() throws Exception {
                // UPDATED: Now passing the actual PDF files to the AI
                return aiService.initiateFeynmanSession(notes, selectedPdfs, ApiKeyManager.getApiKey());
            }
        };

        task.setOnSucceeded(e -> {
            showLoading(false);
            AIService.FeynmanInit init = task.getValue();
            feynmanConcept = init.concept;
            feynmanCurrentQuestion = init.first_question;
            feynmanHistory = new StringBuilder("1. ").append(feynmanCurrentQuestion);
            feynmanTurn = 1;
            showFeynmanUI();
        });

        task.setOnFailed(e -> {
            showLoading(false);
            showStatus("Error starting Feynman session: " + task.getException().getMessage(), true);
        });
        new Thread(task).start();
    }

    private void showFeynmanUI() {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        HBox topBar = new HBox(30);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setMaxWidth(Double.MAX_VALUE);

        Button backBtn = createGlassButton("< Back to Menu", false);
        backBtn.setOnAction(e -> showMainMenu());

        Label titleLabel = new Label(feynmanTitle);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        titleLabel.setTextFill(Color.web("#f0b432"));
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        titleLabel.setAlignment(Pos.CENTER);

        topBar.getChildren().addAll(backBtn, titleLabel);

        historyScroll = createTransparentScrollableLabel(feynmanHistory.toString(), Color.WHITE, 32);
        VBox.setVgrow(historyScroll, Priority.ALWAYS);

        feynmanInputArea = new TextArea();
        feynmanInputArea.setPromptText("Explain the concept...");
        feynmanInputArea.setWrapText(true);
        feynmanInputArea.setPrefHeight(300);
        feynmanInputArea.setStyle("-fx-background-color: #2b2b2b; -fx-control-inner-background: #2b2b2b; -fx-text-fill: #bbbbbb; -fx-border-color: #3574f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-size: 28px; -fx-padding: 15;");

        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        submitBtn = createGlassButton("Submit Explanation", false);
        whiteFlagBtn = createGlassButton("White Flag", true);

        buttonBox.getChildren().addAll(submitBtn, whiteFlagBtn);

        submitBtn.setOnAction(e -> {
            String userExplanation = feynmanInputArea.getText().trim();
            if (userExplanation.isEmpty()) {
                showStatus("You need to explain something first!", true);
                return;
            }
            evaluateFeynmanExplanation(userExplanation);
        });

        whiteFlagBtn.setOnAction(e -> {
            resolveFeynmanQuestion();
        });

        mainLayout.getChildren().addAll(topBar, historyScroll, feynmanInputArea, buttonBox);
        contentPane.setCenter(mainLayout);
    }

    private void evaluateFeynmanExplanation(String userExplanation) {
        // 1. Disable UI to prevent double submission and indicate loading
        feynmanInputArea.setDisable(true);
        submitBtn.setDisable(true);
        whiteFlagBtn.setDisable(true);
        showLoading(true);

        Task<AIService.FeynmanEval> task = new Task<>() {
            @Override
            protected AIService.FeynmanEval call() throws Exception {
                return aiService.evaluateFeynmanExplanation(
                    feynmanConcept, feynmanCurrentQuestion, 
                    userExplanation, feynmanTurn, ApiKeyManager.getApiKey()
                );
            }
        };

        task.setOnSucceeded(e -> {
            showLoading(false);
            AIService.FeynmanEval eval = task.getValue();
            
            // 2. ONLY NOW do we move the text to history and clear the input
            feynmanHistory.append("\n\n").append(userExplanation);
            
            feynmanTurn++;
            if (feynmanTurn <= MAX_FEYNMAN_TURNS) {
                feynmanCurrentQuestion = eval.next_question;
                feynmanHistory.append("\n\n").append(feynmanTurn).append(". ").append(feynmanCurrentQuestion);
            } else {
                feynmanHistory.append("\n\nSession Complete!");
                showStatus("Feynman Session Complete! Great teaching!", false);
            }
            
            updateHistoryUI();
            feynmanInputArea.clear(); // Clear ONLY on success
            
            // Re-enable if not complete
            if (feynmanTurn <= MAX_FEYNMAN_TURNS) {
                feynmanInputArea.setDisable(false);
                submitBtn.setDisable(false);
                whiteFlagBtn.setDisable(false);
                feynmanInputArea.requestFocus();
            } else {
                feynmanInputArea.setDisable(true);
                submitBtn.setDisable(true);
                whiteFlagBtn.setDisable(true);
            }
        });

        task.setOnFailed(e -> {
            showLoading(false);
            // 3. Re-enable UI and KEEP the text so the user can retry
            feynmanInputArea.setDisable(false);
            submitBtn.setDisable(false);
            whiteFlagBtn.setDisable(false);
            feynmanInputArea.requestFocus();
            showStatus("Error evaluating explanation: " + task.getException().getMessage(), true);
        });
        new Thread(task).start();
    }

    private void resolveFeynmanQuestion() {
        feynmanHistory.append("\n\n🏳️ White Flag Raised!");
        updateHistoryUI();
        
        feynmanInputArea.setDisable(true);
        submitBtn.setDisable(true);
        whiteFlagBtn.setDisable(true);
        showLoading(true);

        Task<AIService.FeynmanResolve> task = new Task<>() {
            @Override
            protected AIService.FeynmanResolve call() throws Exception {
                return aiService.resolveFeynmanQuestion(feynmanConcept, feynmanCurrentQuestion, ApiKeyManager.getApiKey());
            }
        };

        task.setOnSucceeded(e -> {
            showLoading(false);
            AIService.FeynmanResolve resolve = task.getValue();
            
            feynmanHistory.append("\n\n").append(resolve.explanation);
            updateHistoryUI();
            
            showStatus("Don't worry, learning takes time! Session ended.", false);
        });

        task.setOnFailed(e -> {
            showLoading(false);
            // Re-enable on failure so they can try the white flag again or just go back
            feynmanInputArea.setDisable(false);
            submitBtn.setDisable(false);
            whiteFlagBtn.setDisable(false);
            showStatus("Error resolving question: " + task.getException().getMessage(), true);
        });
        new Thread(task).start();
    }

    private void updateHistoryUI() {
        Label historyLabel = (Label) historyScroll.getContent();
        historyLabel.setText(feynmanHistory.toString());
        historyScroll.setVvalue(1.0);
    }

    // ==========================================
    // STANDARD DECK UI LOGIC
    // ==========================================

    private void importDeck() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Deck Files", "*.json"));
        File file = fc.showOpenDialog(null);
        if (file != null) {
            try {
                String json = java.nio.file.Files.readString(file.toPath());
                Deck importedDeck = new com.google.gson.Gson().fromJson(json, Deck.class);
                if (importedDeck != null && importedDeck.getName() != null) {
                    decks.add(importedDeck);
                    showStatus("Deck '" + importedDeck.getName() + "' imported!", false);
                    showDecksScreen();
                }
            } catch (Exception e) {
                showStatus("Error importing deck: " + e.getMessage(), true);
            }
        }
    }

    private void showDecksScreen() {
        VBox screen = new VBox(30);
        screen.setPadding(new Insets(60));
        screen.setAlignment(Pos.TOP_CENTER);
        screen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        HBox topBar = new HBox(30);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setMaxWidth(Double.MAX_VALUE);
        
        Button backBtn = createGlassButton("< Back", false);
        backBtn.setOnAction(e -> showMainMenu());
        
        Label title = new Label("Your Decks");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 56));
        title.setTextFill(Color.WHITE);
        HBox.setHgrow(title, Priority.ALWAYS);
        topBar.getChildren().addAll(backBtn, title);
        
        deckListView = new ListView<>();
        deckListView.setMaxWidth(1400);
        deckListView.setPrefWidth(1400);
        deckListView.setPrefHeight(800);
        deckListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        deckListView.setCellFactory(lv -> new DeckListCell(lv));
        refreshDeckList();
        
        screen.getChildren().addAll(topBar, deckListView);
        contentPane.setCenter(screen);
    }

    private void refreshDeckList() {
        deckListView.getItems().setAll(decks);
    }

    private class DeckListCell extends ListCell<Deck> {
        private final HBox content;
        private final Label nameLabel;
        private final Button studyBtn;
        private final Button exportBtn;
        private final Button deleteBtn;

        public DeckListCell(ListView<Deck> lv) {
            content = new HBox(20);
            content.setAlignment(Pos.CENTER_LEFT);
            content.getStyleClass().add("deck-cell");
            content.setPadding(new Insets(50, 40, 50, 40)); 
            
            nameLabel = new Label();
            nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 38));
            nameLabel.setTextFill(Color.web("#bbbbbb"));
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            
            studyBtn = createSmallGlassButton("Study", false);
            exportBtn = createSmallGlassButton("Export", false);
            deleteBtn = createSmallGlassButton("Delete", true);
            
            HBox buttonBox = new HBox(15, studyBtn, exportBtn, deleteBtn);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            content.getChildren().addAll(nameLabel, buttonBox);
            
            studyBtn.setOnAction(e -> { if (getItem() != null) startStudySession(getItem()); });
            exportBtn.setOnAction(e -> { if (getItem() != null) exportDeck(getItem()); });
            deleteBtn.setOnAction(e -> {
                if (getItem() != null) {
                    decks.remove(getItem());
                    refreshDeckList();
                }
            });
            
            setOnMouseClicked(e -> {
                if (isEmpty()) lv.getSelectionModel().clearSelection();
            });
            
            content.setOnDragDetected(e -> {
                if (getItem() == null) return;
                Dragboard db = content.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(String.valueOf(getIndex()));
                db.setContent(cc);
                e.consume();
            });
            
            content.setOnDragOver(e -> {
                if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.MOVE);
                e.consume();
            });
            
            content.setOnDragDropped(e -> {
                Dragboard db = e.getDragboard();
                if (db.hasString()) {
                    int dragIndex = Integer.parseInt(db.getString());
                    int dropIndex = getIndex();
                    if (dragIndex != dropIndex && dropIndex >= 0) {
                        Deck dragged = decks.remove(dragIndex);
                        decks.add(dropIndex, dragged);
                        refreshDeckList();
                    }
                    e.setDropCompleted(true);
                }
                e.consume();
            });
        }

        @Override
        protected void updateItem(Deck deck, boolean empty) {
            super.updateItem(deck, empty);
            if (empty || deck == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(deck.getName() + "  (" + deck.size() + " cards)");
                setGraphic(content);
            }
        }
    }

    private void startStudySession(Deck deck) {
        if (deck.isEmpty()) {
            showStatus("This deck is empty!", true);
            return;
        }
        currentStudyDeck = deck;
        loadNextCard();
    }

    private void loadNextCard() {
        currentStudyCard = currentStudyDeck.pickNextCard();
        if (currentStudyCard == null) {
            showStatus("Deck is empty!", true);
            showDecksScreen();
            return;
        }
        
        VBox screen = new VBox(30);
        screen.setPadding(new Insets(50));
        screen.setAlignment(Pos.CENTER);
        screen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setMaxWidth(Double.MAX_VALUE);
        
        Button backBtn = createGlassButton("< Back to Decks", false);
        backBtn.setOnAction(e -> showDecksScreen());
        topBar.getChildren().add(backBtn);
        
        Label questionLabel = new Label(currentStudyCard.getQuestion());
        questionLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        questionLabel.setTextFill(Color.WHITE);
        questionLabel.setWrapText(true);
        questionLabel.setTextAlignment(TextAlignment.CENTER);
        questionLabel.setMaxWidth(1400);
        questionLabel.setMinHeight(165);
        questionLabel.setAlignment(Pos.CENTER);
        
        Label answerLabel = new Label(currentStudyCard.getAnswer());
        answerLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 32));
        answerLabel.setTextFill(Color.web("#5c94ff"));
        answerLabel.setWrapText(true);
        answerLabel.setTextAlignment(TextAlignment.CENTER);
        answerLabel.setMaxWidth(1400);
        answerLabel.setMinHeight(126);
        answerLabel.setAlignment(Pos.CENTER);
        answerLabel.setVisible(false);
        
        TextField answerInputField = new TextField();
        answerInputField.setPromptText("Type your answer here...");
        answerInputField.setPrefWidth(800);
        answerInputField.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-border-color: #3574f0; -fx-border-radius: 8; -fx-font-size: 32px; -fx-padding: 15;");
        
        Button submitAnswerBtn = createSmallGlassButton("Submit to AI", false);
        Button cancelAnswerBtn = createSmallGlassButton("Cancel", true);
        
        HBox inputBox = new HBox(15, answerInputField, submitAnswerBtn, cancelAnswerBtn);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setVisible(false);
        
        ScrollPane feedbackScroll = createTransparentScrollableLabel("", Color.WHITE, 32);
        feedbackScroll.setVisible(false);
        feedbackScroll.setMaxHeight(350);
        VBox.setMargin(feedbackScroll, new Insets(-40, 0, 0, 0));
        
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button revealBtn = createGlassButton("Reveal Answer", false);
        Button typeAnswerBtn = createGlassButton("Type My Answer", false);
        Button correctBtn = createGlassButton("Correct", false);
        correctBtn.getStyleClass().add("green");
        Button incorrectBtn = createGlassButton("Incorrect", true);
        Button skipBtn = createGlassButton("Skip / Next", false);
        
        revealBtn.setOnAction(e -> {
            answerLabel.setVisible(true);
            revealBtn.setVisible(false);
            typeAnswerBtn.setVisible(false);
            correctBtn.setVisible(true);
            incorrectBtn.setVisible(true);
        });
        
        typeAnswerBtn.setOnAction(e -> {
            inputBox.setVisible(true);
            answerInputField.requestFocus();
            typeAnswerBtn.setVisible(false);
            revealBtn.setVisible(false);
            skipBtn.setVisible(false);
        });
        
        cancelAnswerBtn.setOnAction(e -> {
            inputBox.setVisible(false);
            answerInputField.clear();
            typeAnswerBtn.setVisible(true);
            revealBtn.setVisible(true);
            skipBtn.setVisible(true);
        });
        
        correctBtn.setVisible(false);
        correctBtn.setOnAction(e -> {
            currentStudyDeck.updateWeight(currentStudyCard, true);
            loadNextCard();
        });
        
        incorrectBtn.setVisible(false);
        incorrectBtn.setOnAction(e -> {
            currentStudyDeck.updateWeight(currentStudyCard, false);
            loadNextCard();
        });
        
        submitAnswerBtn.setOnAction(e -> {
            String userAns = answerInputField.getText();
            if (userAns.trim().isEmpty()) return;
            showLoading(true);
            Task<AIService.GradeResult> gradeTask = new Task<>() {
                @Override
                protected AIService.GradeResult call() throws Exception {
                    return aiService.gradeAnswer(
                        currentStudyCard.getQuestion(),
                        currentStudyCard.getAnswer(),
                        userAns,
                        ApiKeyManager.getApiKey()
                    );
                }
            };
            gradeTask.setOnSucceeded(ev -> {
                showLoading(false);
                AIService.GradeResult res = gradeTask.getValue();
                
                Label fbLabel = (Label) feedbackScroll.getContent();
                fbLabel.setText("Score: " + res.score + "/10 | Grade: " + res.grade + "\n\nCorrect Answer:\n" + currentStudyCard.getAnswer() + "\n\nAI Feedback:\n" + res.explanation);
                
                if (res.score >= 9) fbLabel.setTextFill(Color.web("#2ea043"));
                else if (res.score <= 7) fbLabel.setTextFill(Color.web("#ff5555"));
                else fbLabel.setTextFill(Color.web("#f0b432"));
                
                feedbackScroll.setVisible(true);
                feedbackScroll.setTranslateY(-70);
                inputBox.setVisible(false);
                skipBtn.setVisible(true);
                
                if (res.score >= 9) currentStudyDeck.updateWeight(currentStudyCard, true);
                else if (res.score <= 7) currentStudyDeck.updateWeight(currentStudyCard, false);
            });
            gradeTask.setOnFailed(ev -> {
                showLoading(false);
                showStatus("Error grading: " + gradeTask.getException().getMessage(), true);
            });
            new Thread(gradeTask).start();
        });
        
        skipBtn.setOnAction(e -> loadNextCard());
        buttonBox.getChildren().addAll(revealBtn, typeAnswerBtn, correctBtn, incorrectBtn, skipBtn);
        screen.getChildren().addAll(topBar, questionLabel, answerLabel, inputBox, feedbackScroll, buttonBox);
        contentPane.setCenter(screen);
    }

    private void exportDeck(Deck deck) {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName(deck.getName() + ".json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fc.showSaveDialog(null);
        if (file != null) {
            try {
                java.nio.file.Files.writeString(file.toPath(), new com.google.gson.Gson().toJson(deck));
                showStatus("Deck exported successfully!", false);
            } catch (Exception e) {
                showStatus("Error exporting: " + e.getMessage(), true);
            }
        }
    }

    @Override
    public void stop() {
        if (backgroundMusicPlayer != null) backgroundMusicPlayer.stop();
        if (clickSoundPlayer != null) clickSoundPlayer.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}