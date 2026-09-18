package com.elitelearn;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.concurrent.Task;
import javafx.scene.Node;
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
    private StackPane loadingOverlay;
    private StackPane contentPane;
    private Scene mainScene;

    // UI Components
    private TextArea notesArea;
    private TextField notesField;
    private VBox notesContainer;
    private Label pdfStatusLabel;
    private ListView<Deck> deckListView;

    @Override
    public void start(Stage primaryStage) {
        rootPane = new StackPane();
        rootPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        // Background Glow Effect (Created ONCE here)
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

        // FIX: Create a dedicated content pane for screen swapping
        contentPane = new StackPane();

        // Loading Overlay
        loadingOverlay = new StackPane();
        loadingOverlay.getStyleClass().add("loading-overlay");
        loadingOverlay.setVisible(false);
        loadingOverlay.setOpacity(0);
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setStyle("-fx-accent: #3574f0;");
        Label loadingText = new Label("AI is generating your flashcards...");
        loadingText.setTextFill(Color.WHITE);
        loadingText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        
        VBox loadingBox = new VBox(15, spinner, loadingText);
        loadingBox.setAlignment(Pos.CENTER);
        loadingOverlay.getChildren().add(loadingBox);

        // Add everything to the root pane in the correct z-order
        rootPane.getChildren().addAll(glow1, glow2, contentPane, loadingOverlay);

        mainScene = new Scene(rootPane, 800, 600);
        primaryStage.setTitle("EliteLearn");
        primaryStage.setScene(mainScene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        // Show the Main Menu initially
        showMainMenu();

        // Check for API Key on startup
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
        
        // Hover animation
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

    // ==========================================
    // CREATE SCREEN
    // ==========================================
    private void showCreateScreen() {
        VBox screen = new VBox(20);
        screen.setPadding(new Insets(40));
        screen.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Create New Deck or Test");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        // Animated Notes Input
        notesField = new TextField();
        notesField.setPromptText("Click here to start typing or pasting notes...");
        notesField.getStyleClass().add("custom-text-field");
        
        notesArea = new TextArea();
        notesArea.getStyleClass().add("custom-text-area");
        notesArea.setWrapText(true);
        notesArea.setVisible(false);
        notesArea.setManaged(false);

        notesContainer = new VBox(10, notesField, notesArea);
        notesContainer.setMaxWidth(600);

        notesField.setOnMouseClicked(e -> expandNotesArea());
        notesField.textProperty().addListener((obs, old, newVal) -> {
            if (!newVal.isEmpty()) expandNotesArea();
        });

        // PDF Upload
        HBox pdfBox = new HBox(15);
        pdfBox.setAlignment(Pos.CENTER_LEFT);
        Button uploadPdfBtn = createGlassButton("Upload PDF(s)", false);
        uploadPdfBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            List<File> files = fc.showOpenMultipleDialog(null);
            if (files != null) {
                selectedPdfs.addAll(files);
                updatePdfStatus();
                notesField.setText(""); // Clear text field if PDFs are uploaded
                notesArea.setVisible(false);
                notesArea.setManaged(false);
            }
        });

        Button clearPdfBtn = createGlassButton("Clear PDFs", true);
        clearPdfBtn.setOnAction(e -> {
            selectedPdfs.clear();
            updatePdfStatus();
        });

        pdfStatusLabel = new Label("No PDFs selected");
        pdfStatusLabel.setTextFill(Color.web("#828282"));

        pdfBox.getChildren().addAll(uploadPdfBtn, clearPdfBtn, pdfStatusLabel);

        // Action Buttons
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setMaxWidth(600);

        Button cancelBtn = createGlassButton("Cancel", false);
        cancelBtn.setOnAction(e -> showMainMenu());

        Button generateBtn = createGlassButton("Generate Flashcards", false);
        generateBtn.setOnAction(e -> generateDeck());

        actionBox.getChildren().addAll(cancelBtn, generateBtn);

        screen.getChildren().addAll(title, notesContainer, pdfBox, actionBox);
        
        Node[] glows = getGlowBackgrounds();
        contentPane.getChildren().setAll(screen);
    }

    private void expandNotesArea() {
        if (!notesArea.isVisible()) {
            notesArea.setVisible(true);
            notesArea.setManaged(true);
            notesArea.setText(notesField.getText());
            notesField.setVisible(false);
            notesField.setManaged(false);
            
            // Smooth height animation
            notesArea.setPrefRowCount(1);
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(300), new KeyValue(notesArea.prefRowCountProperty(), 10))
            );
            timeline.play();
        }
    }

    private void updatePdfStatus() {
        if (selectedPdfs.isEmpty()) {
            pdfStatusLabel.setText("No PDFs selected");
        } else {
            pdfStatusLabel.setText(selectedPdfs.size() + " PDF(s) selected");
        }
    }

    private void generateDeck() {
        String notes = notesArea.isVisible() ? notesArea.getText() : notesField.getText();
        if (selectedPdfs.isEmpty() && notes.trim().isEmpty()) {
            showAlert("Please enter notes or upload a PDF first.");
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
            showAlert("Error generating cards: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    // ==========================================
    // DECKS SCREEN
    // ==========================================
    private void showDecksScreen() {
        VBox screen = new VBox(20);
        screen.setPadding(new Insets(40));
        screen.setAlignment(Pos.TOP_CENTER);

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
        deckListView.setMaxHeight(400);
        deckListView.getStyleClass().add("deck-list");
        deckListView.setCellFactory(lv -> new DeckListCell());
        refreshDeckList();

        screen.getChildren().addAll(topBar, deckListView);
        
        Node[] glows = getGlowBackgrounds();
        contentPane.getChildren().setAll(screen);
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

        public DeckListCell() {
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
            
            exportBtn = createGlassButton("Export", false);
            exportBtn.setPadding(new Insets(5, 15, 5, 15));
            
            deleteBtn = createGlassButton("Delete", true);
            deleteBtn.setPadding(new Insets(5, 15, 5, 15));

            content.getChildren().addAll(nameLabel, studyBtn, exportBtn, deleteBtn);

            studyBtn.setOnAction(e -> {
                if (getItem() != null) startStudySession(getItem());
            });
            exportBtn.setOnAction(e -> {
                if (getItem() != null) exportDeck(getItem());
            });
            deleteBtn.setOnAction(e -> {
                if (getItem() != null) {
                    decks.remove(getItem());
                    refreshDeckList();
                }
            });

            // Drag and Drop Setup
            content.setOnDragDetected(e -> {
                if (getItem() == null) return;
                Dragboard db = content.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(String.valueOf(getIndex()));
                db.setContent(cc);
                e.consume();
            });

            content.setOnDragOver(e -> {
                if (e.getDragboard().hasString()) {
                    e.acceptTransferModes(TransferMode.MOVE);
                }
                e.consume();
            });

            content.setOnDragDropped(e -> {
                Dragboard db = e.getDragboard();
                if (db.hasString()) {
                    int dragIndex = Integer.parseInt(db.getString());
                    int dropIndex = getIndex();
                    
                    if (dragIndex != dropIndex && dragIndex != dropIndex - 1) {
                        Deck dragged = decks.remove(dragIndex);
                        if (dropIndex > dragIndex) dropIndex--;
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

    // ==========================================
    // STUDY SCREEN (Simplified for JavaFX)
    // ==========================================
    private void startStudySession(Deck deck) {
        if (deck.isEmpty()) {
            showAlert("This deck is empty!");
            return;
        }
        currentStudyDeck = deck;
        loadNextCard();
    }

    private void loadNextCard() {
        currentStudyCard = currentStudyDeck.pickNextCard();
        if (currentStudyCard == null) {
            showAlert("Deck is empty!");
            showDecksScreen();
            return;
        }

        VBox screen = new VBox(20);
        screen.setPadding(new Insets(40));
        screen.setAlignment(Pos.CENTER);

        Button backBtn = createGlassButton("< Back to Decks", false);
        backBtn.setAlignment(Pos.TOP_LEFT);
        backBtn.setOnAction(e -> showDecksScreen());
        VBox.setVgrow(backBtn, Priority.ALWAYS);

        Label questionLabel = new Label(currentStudyCard.getQuestion());
        questionLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 22));
        questionLabel.setTextFill(Color.WHITE);
        questionLabel.setWrapText(true);
        questionLabel.setAlignment(Pos.CENTER);

        Label answerLabel = new Label(currentStudyCard.getAnswer());
        answerLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 18));
        answerLabel.setTextFill(Color.web("#3574f0"));
        answerLabel.setWrapText(true);
        answerLabel.setAlignment(Pos.CENTER);
        answerLabel.setVisible(false);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button revealBtn = createGlassButton("Reveal Answer", false);
        revealBtn.setOnAction(e -> {
            answerLabel.setVisible(true);
            revealBtn.setVisible(false);
        });

        Button correctBtn = createGlassButton("Correct", false);
        correctBtn.getStyleClass().add("green");
        correctBtn.setVisible(false);
        correctBtn.setOnAction(e -> {
            currentStudyDeck.updateWeight(currentStudyCard, true);
            loadNextCard();
        });

        Button incorrectBtn = createGlassButton("Incorrect", true);
        incorrectBtn.setVisible(false);
        incorrectBtn.setOnAction(e -> {
            currentStudyDeck.updateWeight(currentStudyCard, false);
            loadNextCard();
        });

        Button skipBtn = createGlassButton("Skip", false);
        skipBtn.setOnAction(e -> loadNextCard());

        buttonBox.getChildren().addAll(revealBtn, correctBtn, incorrectBtn, skipBtn);

        screen.getChildren().addAll(backBtn, questionLabel, answerLabel, buttonBox);
        
        Node[] glows = getGlowBackgrounds();
        contentPane.getChildren().setAll(screen);
    }

    // ==========================================
    // UTILITIES
    // ==========================================
    private void showMainMenu() {
        VBox mainContent = new VBox(20);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(40));

        Label title = new Label("EliteLearn");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Leitner-inspired digital spaced learning system");
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        subtitle.setTextFill(Color.web("#828282"));

        Button continueBtn = createGlassButton("Continue", false);
        continueBtn.setOnAction(e -> showDecksScreen());

        Button createBtn = createGlassButton("Create New", false);
        createBtn.setOnAction(e -> showCreateScreen());

        Button exitBtn = createGlassButton("Exit", true);
        exitBtn.setOnAction(e -> System.exit(0));

        mainContent.getChildren().addAll(title, subtitle, continueBtn, createBtn, exitBtn);
        
        // FIX: Just swap the content in the center pane
        contentPane.getChildren().setAll(mainContent);
    }

    private Node[] getGlowBackgrounds() {
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
        
        // FIX: Return as Node[] instead of Pane[]
        return new Node[]{glow1, glow2};
    }


    private void exportDeck(Deck deck) {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName(deck.getName() + ".json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fc.showSaveDialog(null);
        if (file != null) {
            try {
                java.nio.file.Files.writeString(file.toPath(), new com.google.gson.Gson().toJson(deck));
                showAlert("Deck exported successfully!");
            } catch (Exception e) {
                showAlert("Error exporting: " + e.getMessage());
            }
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("EliteLearn");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}