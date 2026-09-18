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
    private StackPane contentPane;
    private StackPane loadingOverlay;
    private Scene mainScene;

    // UI Components
    private TextArea notesArea;
    private TextField notesField;
    private Label pdfStatusLabel;
    private ListView<Deck> deckListView;

    @Override
    public void start(Stage primaryStage) {
        rootPane = new StackPane();
        rootPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Background Glow Effect
        Region glow1 = new Region();
        glow1.getStyleClass().add("glow-bg");
        glow1.setPrefSize(600, 600);
        glow1.setTranslateX(-200);
        glow1.setTranslateY(-200);
        
        Region glow2 = new Region();
        glow2.getStyleClass().add("glow-bg");
        glow2.setPrefSize(500, 500);
        glow2.setTranslateX(300);
        glow2.setTranslateY(200);

        contentPane = new StackPane();

        // Loading Overlay
        loadingOverlay = new StackPane();
        loadingOverlay.getStyleClass().add("loading-overlay");
        loadingOverlay.setVisible(false);
        loadingOverlay.setOpacity(0);
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setStyle("-fx-accent: #3574f0;");
        Label loadingText = new Label("AI is processing...");
        loadingText.setTextFill(Color.WHITE);
        loadingText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        
        VBox loadingBox = new VBox(15, spinner, loadingText);
        loadingBox.setAlignment(Pos.CENTER);
        loadingOverlay.getChildren().add(loadingBox);

        rootPane.getChildren().addAll(glow1, glow2, contentPane, loadingOverlay);

        mainScene = new Scene(rootPane, 800, 600);
        primaryStage.setTitle("EliteLearn");
        primaryStage.setScene(mainScene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
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
        
        btn.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btn);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });
        btn.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), btn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        return btn;
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), loadingOverlay);
        ft.setToValue(show ? 1.0 : 0.0);
        ft.setOnFinished(e -> {
            if (!show) loadingOverlay.setVisible(false);
        });
        ft.play();
    }

    private void showToast(String message, boolean isError) {
        Label toastLabel = new Label(message);
        toastLabel.setTextFill(Color.WHITE);
        toastLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        toastLabel.setPadding(new Insets(10, 20, 10, 20));
        
        StackPane toastPane = new StackPane(toastLabel);
        String bgColor = isError ? "#ff5555" : "#2ea043";
        toastPane.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8;");
        
        rootPane.getChildren().add(toastPane);
        StackPane.setAlignment(toastPane, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toastPane, new Insets(0, 0, 40, 0));
        
        toastPane.setOpacity(0);
        toastPane.setTranslateY(30);
        
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
            slideOut.setToY(30);
            ParallelTransition pt = new ParallelTransition(fadeOut, slideOut);
            pt.setOnFinished(ev -> rootPane.getChildren().remove(toastPane));
            pt.play();
        });
        delay.play();
    }

    // ==========================================
    // MAIN MENU
    // ==========================================
    private void showMainMenu() {
        VBox mainContent = new VBox(20);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(40));
        // FIX: Force VBox to fill the StackPane so centering works perfectly
        mainContent.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Label title = new Label("EliteLearn");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 56));
        title.setTextFill(Color.WHITE);
        title.setMaxWidth(Double.MAX_VALUE);
        title.setTextAlignment(TextAlignment.CENTER);

        Label subtitle = new Label("Leitner-inspired digital spaced learning system");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
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
        contentPane.getChildren().setAll(mainContent);
    }

    // ==========================================
    // CREATE SCREEN
    // ==========================================
    private void showCreateScreen() {
        VBox screen = new VBox(20);
        screen.setPadding(new Insets(40));
        screen.setAlignment(Pos.TOP_CENTER);
        screen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Label title = new Label("Create New Deck or Test");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        // Notes Input (Single Line)
        notesField = new TextField();
        notesField.setPromptText("Click here to start typing or pasting notes...");
        notesField.setMaxWidth(600);
        notesField.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: #bbbbbb; -fx-border-color: #3c3f41; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Notes Input (Expanded Area)
        notesArea = new TextArea();
        notesArea.setWrapText(true);
        notesArea.setVisible(false);
        notesArea.setManaged(false);
        notesArea.setMaxWidth(600);
        // FIX: Match the dark background color
        notesArea.setStyle("-fx-background-color: #2b2b2b; -fx-control-inner-background: #2b2b2b; -fx-text-fill: #bbbbbb; -fx-border-color: #3574f0; -fx-border-radius: 6; -fx-background-radius: 6;");
        
        // FIX: Shrink when clicking outside (losing focus)
        notesArea.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                collapseNotesArea();
            }
        });

        VBox notesContainer = new VBox(10, notesField, notesArea);
        notesContainer.setAlignment(Pos.TOP_CENTER);
        notesContainer.setMaxWidth(600);

        notesField.setOnMouseClicked(e -> expandNotesArea());
        notesField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) expandNotesArea();
        });

        // Action Buttons Row
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setMaxWidth(600);

        Button importBtn = createGlassButton("Import Deck JSON", false);
        importBtn.setOnAction(e -> importDeck());

        Button cancelBtn = createGlassButton("Cancel", false);
        cancelBtn.setOnAction(e -> showMainMenu());

        Button generateBtn = createGlassButton("Generate Flashcards", false);
        generateBtn.setOnAction(e -> generateDeck());

        actionBox.getChildren().addAll(importBtn, cancelBtn, generateBtn);

        screen.getChildren().addAll(title, notesContainer, actionBox);
        contentPane.getChildren().setAll(screen);
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
                new KeyFrame(Duration.millis(300), new KeyValue(notesArea.prefRowCountProperty(), 8))
            );
            timeline.play();
        }
    }

    private void collapseNotesArea() {
        if (notesArea.isVisible()) {
            notesField.setText(notesArea.getText());
            notesArea.setVisible(false);
            notesArea.setManaged(false);
            notesField.setVisible(true);
            notesField.setManaged(true);
        }
    }

    private void generateDeck() {
        String notes = notesArea.isVisible() ? notesArea.getText() : notesField.getText();
        if (notes.trim().isEmpty()) {
            showToast("Please enter notes first.", true);
            return;
        }

        showLoading(true);
        Task<List<Flashcard>> task = new Task<>() {
            @Override
            protected List<Flashcard> call() throws Exception {
                return aiService.generateFlashcards(notes, 20, ApiKeyManager.getApiKey());
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

    // ==========================================
    // DECKS SCREEN
    // ==========================================
    private void showDecksScreen() {
        VBox screen = new VBox(20);
        screen.setPadding(new Insets(40));
        screen.setAlignment(Pos.TOP_CENTER);
        screen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setMaxWidth(Double.MAX_VALUE);
        
        Button backBtn = createGlassButton("< Back", false);
        backBtn.setOnAction(e -> showMainMenu());
        
        Label title = new Label("Your Decks");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        HBox.setHgrow(title, Priority.ALWAYS);
        
        topBar.getChildren().addAll(backBtn, title);

        deckListView = new ListView<>();
        deckListView.setMaxWidth(600);
        deckListView.setPrefHeight(400);
        // FIX: Remove the ugly white background
        deckListView.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        deckListView.setCellFactory(lv -> new DeckListCell(lv));
        refreshDeckList();

        screen.getChildren().addAll(topBar, deckListView);
        contentPane.getChildren().setAll(screen);
    }

    private void refreshDeckList() {
        deckListView.getItems().setAll(decks);
    }

    private class DeckListCell extends ListCell<Deck> {
        private final HBox content;
        private final Label nameLabel;
        private final Button studyBtn;
        private final Button deleteBtn;

        public DeckListCell(ListView<Deck> lv) {
            content = new HBox(15);
            content.setAlignment(Pos.CENTER_LEFT);
            content.getStyleClass().add("deck-cell");
            content.setPadding(new Insets(15));

            nameLabel = new Label();
            nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
            nameLabel.setTextFill(Color.web("#bbbbbb"));
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            studyBtn = createGlassButton("Study", false);
            studyBtn.setPadding(new Insets(5, 15, 5, 15));
            
            deleteBtn = createGlassButton("Delete", true);
            deleteBtn.setPadding(new Insets(5, 15, 5, 15));

            content.getChildren().addAll(nameLabel, studyBtn, deleteBtn);

            studyBtn.setOnAction(e -> {
                if (getItem() != null) startStudySession(getItem());
            });
            deleteBtn.setOnAction(e -> {
                if (getItem() != null) {
                    decks.remove(getItem());
                    refreshDeckList();
                }
            });

            // FIX: Clicking empty space clears selection
            setOnMouseClicked(e -> {
                if (isEmpty()) {
                    lv.getSelectionModel().clearSelection();
                }
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

    // ==========================================
    // STUDY SCREEN
    // ==========================================
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

        VBox screen = new VBox(20);
        screen.setPadding(new Insets(40));
        screen.setAlignment(Pos.CENTER);
        screen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setMaxWidth(Double.MAX_VALUE);
        Button backBtn = createGlassButton("< Back to Decks", false);
        backBtn.setOnAction(e -> showDecksScreen());
        topBar.getChildren().add(backBtn);

        Label questionLabel = new Label(currentStudyCard.getQuestion());
        questionLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32)); 
        questionLabel.setTextFill(Color.WHITE);
        questionLabel.setWrapText(true);
        questionLabel.setTextAlignment(TextAlignment.CENTER);
        questionLabel.setMaxWidth(700); 

        Label answerLabel = new Label(currentStudyCard.getAnswer());
        answerLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 24)); 
        answerLabel.setTextFill(Color.web("#5c94ff")); 
        answerLabel.setWrapText(true);
        answerLabel.setTextAlignment(TextAlignment.CENTER);
        answerLabel.setMaxWidth(700);
        answerLabel.setVisible(false);

        // AI Input Area
        TextField answerInputField = new TextField();
        answerInputField.setPromptText("Type your answer here...");
        answerInputField.setPrefWidth(400);
        answerInputField.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-border-color: #3574f0; -fx-border-radius: 6;");
        
        Button submitAnswerBtn = createGlassButton("Submit to AI", false);
        HBox inputBox = new HBox(10, answerInputField, submitAnswerBtn);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setVisible(false);

        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setTextFill(Color.WHITE);
        feedbackLabel.setFont(Font.font("Segoe UI", 14));
        feedbackLabel.setMaxWidth(700);
        feedbackLabel.setTextAlignment(TextAlignment.CENTER);
        feedbackLabel.setVisible(false);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button revealBtn = createGlassButton("Reveal Answer", false);
        Button typeAnswerBtn = createGlassButton("Type My Answer", false);
        Button correctBtn = createGlassButton("Correct", false);
        correctBtn.getStyleClass().add("green");
        Button incorrectBtn = createGlassButton("Incorrect", true);
        Button skipBtn = createGlassButton("Skip / Next", false);

        // --- Button Logic ---
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
            Task<AIService.GradeResult> task = new Task<>() {
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

            task.setOnSucceeded(ev -> {
                showLoading(false);
                AIService.GradeResult res = task.getValue();
                feedbackLabel.setText("Score: " + res.score + "/10 | Grade: " + res.grade + "\n" + res.explanation);
                
                if (res.score >= 9) feedbackLabel.setTextFill(Color.web("#2ea043"));
                else if (res.score <= 7) feedbackLabel.setTextFill(Color.web("#ff5555"));
                else feedbackLabel.setTextFill(Color.web("#f0b432"));
                
                feedbackLabel.setVisible(true);
                inputBox.setVisible(false);
                skipBtn.setVisible(true); // Show skip/next button to proceed

                if (res.score >= 9) currentStudyDeck.updateWeight(currentStudyCard, true);
                else if (res.score <= 7) currentStudyDeck.updateWeight(currentStudyCard, false);
            });

            task.setOnFailed(ev -> {
                showLoading(false);
                showToast("Error grading: " + task.getException().getMessage(), true);
            });

            new Thread(task).start();
        });

        skipBtn.setOnAction(e -> loadNextCard());

        buttonBox.getChildren().addAll(revealBtn, typeAnswerBtn, correctBtn, incorrectBtn, skipBtn);

        screen.getChildren().addAll(topBar, questionLabel, answerLabel, inputBox, feedbackLabel, buttonBox);
        contentPane.getChildren().setAll(screen);
    }

    public static void main(String[] args) {
        launch(args);
    }
}