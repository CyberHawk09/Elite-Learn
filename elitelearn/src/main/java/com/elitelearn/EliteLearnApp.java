package com.elitelearn;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
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
    private Scene mainScene;
    
    private TextArea notesArea;
    private TextField notesField;
    private Label pdfStatusLabel;
    private ListView<Deck> deckListView;

    @Override
    public void start(Stage primaryStage) {
        rootPane = new StackPane();
        rootPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
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
        
        rootPane.getChildren().addAll(glow1, glow2, contentPane, loadingOverlay);
        
        mainScene = new Scene(rootPane, 1920, 1080);
        
        // ==========================================
        // NEW: Click outside notesArea to collapse it
        // ==========================================
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

    private Button createGlassButton(String text, boolean isRed) {
        Button btn = new Button(text);
        btn.getStyleClass().add("glass-button");
        if (isRed) btn.getStyleClass().add("red");
        btn.setStyle(btn.getStyle() + "-fx-font-size: 34px; -fx-padding: 24 60 24 60;");
        
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
        return btn;
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), loadingOverlay);
        ft.setToValue(show ? 1.0 : 0.0);
        ft.setOnFinished(e -> { if (!show) loadingOverlay.setVisible(false); });
        ft.play();
    }

    private void showToast(String message, boolean isError) {
        Label toastLabel = new Label(message);
        toastLabel.setTextFill(Color.WHITE);
        toastLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        toastLabel.setPadding(new Insets(24, 48, 24, 48));
        toastLabel.setMaxWidth(1000);
        toastLabel.setWrapText(true);
        toastLabel.setTextAlignment(TextAlignment.CENTER);
        
        StackPane toastPane = new StackPane(toastLabel);
        String bgColor = isError ? "#ff5555" : "#2ea043";
        toastPane.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0, 0, 8);");
        
        rootPane.getChildren().add(toastPane);
        StackPane.setAlignment(toastPane, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toastPane, new Insets(0, 0, 80, 0)); 
        
        toastPane.setOpacity(0);
        toastPane.setTranslateY(40); 
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toastPane);
        fadeIn.setToValue(1.0);
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(300), toastPane);
        slideIn.setToY(0);
        new ParallelTransition(fadeIn, slideIn).play();
        
        PauseTransition delay = new PauseTransition(Duration.seconds(4));
        delay.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toastPane);
            fadeOut.setToValue(0.0);
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(300), toastPane);
            slideOut.setToY(40);
            ParallelTransition pt = new ParallelTransition(fadeOut, slideOut);
            pt.setOnFinished(ev -> rootPane.getChildren().remove(toastPane));
            pt.play();
        });
        delay.play();
    }

    private void showMainMenu() {
        VBox mainContent = new VBox(60); 
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(40));
        mainContent.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        Label title = new Label("EliteLearn");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 120));
        title.setTextFill(Color.WHITE);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setTextAlignment(TextAlignment.CENTER);
        
        Label subtitle = new Label("Leitner-inspired digital spaced learning system");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 40));
        subtitle.setTextFill(Color.web("#828282"));
        subtitle.setMaxWidth(Double.MAX_VALUE);
        subtitle.setTextAlignment(TextAlignment.CENTER);
        
        Button continueBtn = createGlassButton("Continue", false);
        continueBtn.setOnAction(e -> showDecksScreen());
        
        Button createBtn = createGlassButton("Create New", false);
        createBtn.setOnAction(e -> showCreateScreen());
        
        Button exitBtn = createGlassButton("Exit", true);
        exitBtn.setOnAction(e -> System.exit(0));
        
        mainContent.getChildren().addAll(title, subtitle, continueBtn, createBtn, exitBtn);
        contentPane.setCenter(mainContent);
    }

    private void showCreateScreen() {
        VBox screen = new VBox(30);
        screen.setPadding(new Insets(60));
        screen.setAlignment(Pos.TOP_CENTER);
        screen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        Label title = new Label("Create New Deck or Test");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 56));
        title.setTextFill(Color.WHITE);
        
        HBox modeBox = new HBox(20);
        modeBox.setAlignment(Pos.CENTER_LEFT);
        modeBox.setMaxWidth(1200);
        
        Label modeLabel = new Label("Create:");
        modeLabel.setTextFill(Color.web("#bbbbbb"));
        modeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        
        ComboBox<String> modeDropdown = new ComboBox<>();
        modeDropdown.getItems().addAll("Flashcard Deck", "Test (PDF)");
        modeDropdown.setValue("Flashcard Deck");
        modeDropdown.setStyle("-fx-font-size: 28px; -fx-background-color: #2b2b2b; -fx-text-fill: #bbbbbb; -fx-padding: 5;");
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
        
        Button generateBtn = createGlassButton("Generate", false);
        generateBtn.setOnAction(e -> {
            if ("Test (PDF)".equals(modeDropdown.getValue())) {
                generateTest(teacherField.getText());
            } else {
                generateDeck();
            }
        });
        
        actionBox.getChildren().addAll(importBtn, cancelBtn, generateBtn);
        screen.getChildren().addAll(title, modeBox, testOptionsPanel, notesContainer, pdfBox, actionBox);
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
        String notes = notesArea != null && notesArea.isVisible() ? notesArea.getText() : (notesField != null ? notesField.getText() : "");
        if (selectedPdfs.isEmpty() && notes.trim().isEmpty()) {
            showToast("Please enter notes or upload a PDF first.", true);
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
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Name Your Deck");
            dialog.setHeaderText("Enter a name for your new deck:");
            dialog.showAndWait().ifPresent(name -> {
                if (!name.trim().isEmpty()) {
                    decks.add(new Deck(name.trim(), cards));
                    showDecksScreen();
                }
            });
        });
        task.setOnFailed(e -> {
            showLoading(false);
            String errorMsg = task.getException().getMessage();
            if (errorMsg.contains("503")) errorMsg = "AI Service is temporarily busy (503). Please try again in a few seconds.";
            showToast(errorMsg, true);
        });
        new Thread(task).start();
    }

    private void generateTest(String teacherName) {
        String notes = notesArea != null && notesArea.isVisible() ? notesArea.getText() : (notesField != null ? notesField.getText() : "");
        if (selectedPdfs.isEmpty() && notes.trim().isEmpty()) {
            showToast("Please enter notes or upload a PDF first.", true);
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
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Name Your Test");
            dialog.setHeaderText("Enter a name for your test:");
            dialog.showAndWait().ifPresent(testName -> {
                if (!testName.trim().isEmpty()) {
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
                                showToast("Test and Answer Key generated!", false);
                            }
                        } catch (Exception ex) {
                            showToast("Error generating test: " + ex.getMessage(), true);
                        }
                    }
                }
            });
        });
        task.setOnFailed(e -> {
            showLoading(false);
            showToast("Error: " + task.getException().getMessage(), true);
        });
        new Thread(task).start();
    }

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
                    showToast("Deck '" + importedDeck.getName() + "' imported!", false);
                    showDecksScreen();
                }
            } catch (Exception e) {
                showToast("Error importing deck: " + e.getMessage(), true);
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
            showToast("This deck is empty!", true);
            return;
        }
        currentStudyDeck = deck;
        loadNextCard();
    }

    private void loadNextCard() {
        currentStudyCard = currentStudyDeck.pickNextCard();
        if (currentStudyCard == null) {
            showToast("Deck is empty!", true);
            showDecksScreen();
            return;
        }
        
        VBox screen = new VBox(40);
        screen.setPadding(new Insets(60));
        screen.setAlignment(Pos.CENTER);
        screen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setMaxWidth(Double.MAX_VALUE);
        
        Button backBtn = createGlassButton("< Back to Decks", false);
        backBtn.setOnAction(e -> showDecksScreen());
        topBar.getChildren().add(backBtn);
        
        Label questionLabel = new Label(currentStudyCard.getQuestion());
        questionLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 72));
        questionLabel.setTextFill(Color.WHITE);
        questionLabel.setWrapText(true);
        questionLabel.setTextAlignment(TextAlignment.CENTER);
        questionLabel.setMaxWidth(1400);
        
        Label answerLabel = new Label(currentStudyCard.getAnswer());
        answerLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 52));
        answerLabel.setTextFill(Color.web("#5c94ff"));
        answerLabel.setWrapText(true);
        answerLabel.setTextAlignment(TextAlignment.CENTER);
        answerLabel.setMaxWidth(1400);
        answerLabel.setVisible(false);
        
        TextField answerInputField = new TextField();
        answerInputField.setPromptText("Type your answer here...");
        answerInputField.setPrefWidth(800);
        answerInputField.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-border-color: #3574f0; -fx-border-radius: 8; -fx-font-size: 32px; -fx-padding: 15;");
        
        Button submitAnswerBtn = createSmallGlassButton("Submit to AI", false);
        
        // ==========================================
        // NEW: Cancel button for typing answer
        // ==========================================
        Button cancelAnswerBtn = createSmallGlassButton("Cancel", true);
        
        HBox inputBox = new HBox(15, answerInputField, submitAnswerBtn, cancelAnswerBtn);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setVisible(false);
        
        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setFont(Font.font("Segoe UI", 34));
        feedbackLabel.setMaxWidth(1400);
        feedbackLabel.setTextAlignment(TextAlignment.CENTER);
        feedbackLabel.setVisible(false);
        
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
        
        // ==========================================
        // NEW: Cancel action logic
        // ==========================================
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
                feedbackLabel.setText("Score: " + res.score + "/10 | Grade: " + res.grade + "\n\nCorrect Answer:\n" + currentStudyCard.getAnswer() + "\n\nAI Feedback:\n" + res.explanation);
                if (res.score >= 9) feedbackLabel.setTextFill(Color.web("#2ea043"));
                else if (res.score <= 7) feedbackLabel.setTextFill(Color.web("#ff5555"));
                else feedbackLabel.setTextFill(Color.web("#f0b432"));
                
                feedbackLabel.setVisible(true);
                inputBox.setVisible(false);
                skipBtn.setVisible(true);
                
                if (res.score >= 9) currentStudyDeck.updateWeight(currentStudyCard, true);
                else if (res.score <= 7) currentStudyDeck.updateWeight(currentStudyCard, false);
            });
            gradeTask.setOnFailed(ev -> {
                showLoading(false);
                showToast("Error grading: " + gradeTask.getException().getMessage(), true);
            });
            new Thread(gradeTask).start();
        });
        
        skipBtn.setOnAction(e -> loadNextCard());
        buttonBox.getChildren().addAll(revealBtn, typeAnswerBtn, correctBtn, incorrectBtn, skipBtn);
        screen.getChildren().addAll(topBar, questionLabel, answerLabel, inputBox, feedbackLabel, buttonBox);
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
                showToast("Deck exported successfully!", false);
            } catch (Exception e) {
                showToast("Error exporting: " + e.getMessage(), true);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}