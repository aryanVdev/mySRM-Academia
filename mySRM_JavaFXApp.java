import javafx.scene.web.WebView;
import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.regex.*;  // <-- add this line


import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

// JDBC imports added
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

public class mySRM_JavaFXApp extends Application {

    private BorderPane root;
    private VBox sidebar;
    private StackPane mainContent;
    private Scene loginScene, dashboardScene;

    private final Map<String, String> credentials = new HashMap<>();
    private final Map<String, String> names = new HashMap<>();
    private String loggedEmail = null;

    // SQL file path provided by you
    private static final String SQL_SCRIPT_PATH =
            "C:\\Users\\Srihari Srivathsan\\OneDrive\\Documents\\JAVA PROGRAMS\\APP_PROJECT_DATABASE.sql";

    // external HTML file path (relative to working directory)
    private static final String MAPS_EMBED_PATH = "html/maps_embed.html";

    @Override
    public void start(Stage stage) {
        loadCredentials();

        stage.setTitle("mySRM \u2013 Login");

        Stop[] stops = new Stop[]{
                new Stop(0, Color.web("#0f2027")),
                new Stop(1, Color.web("#203a43"))
        };
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops);

        StackPane loginRoot = new StackPane();
        loginRoot.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));

        Circle orb = new Circle(200, Color.web("#00c6ff15"));
        orb.setEffect(new GaussianBlur(50));
        loginRoot.getChildren().add(orb);

        Timeline orbAnim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(orb.translateXProperty(), -300),
                        new KeyValue(orb.translateYProperty(), -150)),
                new KeyFrame(Duration.seconds(10),
                        new KeyValue(orb.translateXProperty(), 300),
                        new KeyValue(orb.translateYProperty(), 250))
        );
        orbAnim.setAutoReverse(true);
        orbAnim.setCycleCount(Animation.INDEFINITE);
        orbAnim.play();

        VBox loginCard = new VBox(20);
        loginCard.setAlignment(Pos.CENTER);
        loginCard.setPadding(new Insets(40));
        loginCard.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 20; "
                + "-fx-border-color: rgba(0,198,255,0.5); -fx-border-radius: 20;");
        loginCard.setEffect(new DropShadow(15, Color.web("#00c6ff40")));

        Label title = new Label("Login to mySRM");
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 26));
        title.setTextFill(Color.WHITE);

        TextField emailField = new TextField();
        emailField.setPromptText("Enter your SRMIST Email ID");
        emailField.setStyle("-fx-background-color: rgba(255,255,255,0.08); "
                + "-fx-text-fill: white; -fx-background-radius: 10; -fx-prompt-text-fill: #bbbbbb;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your Password");
        passwordField.setStyle("-fx-background-color: rgba(255,255,255,0.08); "
                + "-fx-text-fill: white; -fx-background-radius: 10; -fx-prompt-text-fill: #bbbbbb;");

        Button loginBtn = new Button("Login \u27A3");
        loginBtn.setStyle("-fx-background-color: linear-gradient(to right,#00c6ff,#0072ff); "
                + "-fx-text-fill: white; -fx-font-family: 'Poppins'; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 25;");
        loginBtn.setOnMouseEntered(e -> loginBtn.setScaleX(1.05));
        loginBtn.setOnMouseExited(e -> loginBtn.setScaleX(1));

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.web("#ff6b6b"));
        errorLabel.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 13));

        // 2. When Enter is pressed in the email field, move focus to password field.
        emailField.setOnAction(e -> passwordField.requestFocus());
        // Also allow Enter in password field to trigger login.
        passwordField.setOnAction(e -> loginBtn.fire());

        loginBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            String pass = passwordField.getText().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                errorLabel.setText("\u26A0 Please fill in all fields");
            } else if (!email.endsWith("@srmist.edu.in")) {
                errorLabel.setText("\u26A0 Use your SRMIST Email ID (@srmist.edu.in)");
            } else if (!credentials.containsKey(email)) {
                errorLabel.setText("\u26A0 Email not found");
            } else if (!credentials.get(email).equals(pass)) {
                errorLabel.setText("\u26A0 Incorrect password");
            } else {
                errorLabel.setText("");
                loggedEmail = email;
                FadeTransition fade = new FadeTransition(Duration.millis(500), loginRoot);
                fade.setFromValue(1);
                fade.setToValue(0);
                fade.setOnFinished(ev -> {
                    if (dashboardScene == null) dashboardScene = buildDashboard(stage);
                    stage.setScene(dashboardScene);
                });
                fade.play();
            }
        });

        loginCard.getChildren().addAll(title, emailField, passwordField, loginBtn, errorLabel);
        loginRoot.getChildren().add(loginCard);

        loginScene = new Scene(loginRoot, 1150, 700);
        stage.setScene(loginScene);
        stage.show();
    }

    // helper to read external HTML files
    private String readHtmlFile(String path) {
        try {
            return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("⚠ Unable to read HTML file " + path + ": " + e.getMessage());
            return "<html><body><iframe width='100%' height='100%' frameborder='0' style='border:0' src='https://www.google.com/maps' allowfullscreen></iframe></body></html>";
        }
    }

    /**
     * loadCredentials()
     *
     * New behavior:
     *  1. Try to connect to local MySQL (jdbc:mysql://localhost:3306) using provided credentials.
     *  2. If connection succeeds, attempt to execute the SQL script at SQL_SCRIPT_PATH.
     *  3. Query APP_PROJECT_DATABASE.Student_Login_Details and populate `credentials` and `names`.
     *  4. If anything fails, log the error (no CSV fallback).
     */
    private void loadCredentials() {
        // DB connection settings (as provided)
        final String DB_URL = "jdbc:mysql://localhost:3306/?serverTimezone=UTC";
        final String DB_USER = "root";
        final String DB_PASS = "ciqpyv_zeCjeb_2subbo";

        // Attempt DB operations
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement()) {

            // 1) Read SQL script file content
            String sql = Files.readString(Paths.get(SQL_SCRIPT_PATH), StandardCharsets.UTF_8);

            // 2) Split into individual statements by semicolon and execute each
            //    (simple split - assumes the SQL script uses ';' to terminate statements)
            String[] statements = sql.split(";");
            for (String s : statements) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        stmt.execute(trimmed);
                    } catch (SQLException exExec) {
                        // Log and continue: some statements (like CREATE DATABASE if exists) may produce warnings
                        System.err.println("SQL execution warning/err: " + exExec.getMessage());
                    }
                }
            }

            // 3) Query the Student_Login_Details table in the APP_PROJECT_DATABASE
            String query = "SELECT Name, SRM_Email_ID, Password FROM APP_PROJECT_DATABASE.Student_Login_Details";
            try (ResultSet rs = stmt.executeQuery(query)) {
                int count = 0;
                while (rs.next()) {
                    String name = rs.getString("Name");
                    String email = rs.getString("SRM_Email_ID");
                    String pass = rs.getString("Password");
                    if (email != null) {
                        credentials.put(email.trim(), pass == null ? "" : pass.trim());
                        names.put(email.trim(), name == null ? "" : name.trim());
                        count++;
                    }
                }
                if (count > 0) {
                    System.out.println("Loaded " + count + " login rows from database.");
                } else {
                    System.out.println("No rows found in Student_Login_Details table.");
                }
            } catch (SQLException qex) {
                System.err.println("Query failed: " + qex.getMessage());
            }

        } catch (Exception dbEx) {
            // connection or execution failed -> log and continue
            System.err.println("⚠ Database attempt failed: " + dbEx.getMessage());
        }
    }

    private Scene buildDashboard(Stage stage) {
        root = new BorderPane();

        Stop[] stops = new Stop[]{
                new Stop(0, Color.web("#141E30")),
                new Stop(1, Color.web("#243B55"))
        };
        LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops);
        root.setBackground(new Background(new BackgroundFill(gradient, CornerRadii.EMPTY, Insets.EMPTY)));

        HBox topBar = new HBox();
        topBar.setPadding(new Insets(20, 30, 20, 30));
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setSpacing(15);

        Label header = new Label("mySRM \u2013 Student Companion");
        header.setFont(Font.font("Poppins", FontWeight.EXTRA_BOLD, 28));
        header.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button profileBtn = new Button("\uD83D\uDC64 Profile");
        Button logoutBtn = new Button("\uD83D\uDEAA Logout");
        for (Button b : new Button[]{profileBtn, logoutBtn}) {
            b.setStyle("-fx-background-color: linear-gradient(to right,#00c6ff,#0072ff); "
                    + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 6 15;");
            b.setOnMouseEntered(e -> b.setScaleX(1.1));
            b.setOnMouseExited(e -> b.setScaleX(1));
        }

        logoutBtn.setOnAction(e -> {
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to log out?", ButtonType.YES, ButtonType.NO);
    confirm.showAndWait().ifPresent(res -> {
        if (res == ButtonType.YES) {
            // 🧹 Reset login screen before showing it again
            loginScene.getRoot().setOpacity(1); 
            stage.setScene(loginScene);
        }
    });
});


        profileBtn.setOnAction(e -> {
            String name = names.getOrDefault(loggedEmail, "Unknown");
            VBox profilePanel = new VBox(15);
            profilePanel.setAlignment(Pos.CENTER);
            profilePanel.getChildren().add(livelyCard("\uD83D\uDC64 Profile Info",
                    "Name: " + name + "\nEmail: " + loggedEmail + "\nReg No: RA21123456789\nBranch: CSE \u2013 AI"));
            mainContent.getChildren().setAll(profilePanel);
        });

        topBar.getChildren().addAll(header, spacer, profileBtn, logoutBtn);
        root.setTop(topBar);

        // --- REPLACEMENT START ---
sidebar = new VBox(20);
sidebar.setPadding(new Insets(30));
sidebar.setAlignment(Pos.TOP_CENTER);
sidebar.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-background-radius: 25;");
sidebar.setEffect(new DropShadow(20, Color.web("#000000")));
sidebar.setPrefWidth(220);

String[] menuItems = {
    "🏠 Dashboard", "🏆 Marks", "📅 Timetable", "📢 Notices",
    "🗺 Campus Map", "🍽 Dining", "📊 Student Attendance",
    "💬 Feedback", "📚 Study Materials", "🗓 Calendar", "🤖 AI Assistance"
};

for (String item : menuItems) {
    Label btn = new Label(item);
    btn.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 16));
    btn.setTextFill(Color.WHITE);
    btn.setPadding(new Insets(12, 25, 12, 25));
    btn.setStyle("-fx-background-radius: 20; -fx-cursor: hand;");
    btn.setOnMouseEntered(ev -> animateHover(btn, true));
    btn.setOnMouseExited(ev -> animateHover(btn, false));
    btn.setOnMouseClicked(ev -> switchPanel(item));
    sidebar.getChildren().add(btn);
}

// Wrap the sidebar VBox in a ScrollPane so it scrolls when items overflow
ScrollPane sidebarScroll = new ScrollPane(sidebar);
sidebarScroll.setFitToWidth(true); // make the VBox width match the viewport
sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
sidebarScroll.setPannable(true);
sidebarScroll.setPrefViewportWidth(240);
sidebarScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");

// Put the scroll pane into a container so its rounded background stays visible
StackPane sidebarHolder = new StackPane(sidebarScroll);
sidebarHolder.setPadding(new Insets(10)); // small padding so rounded corners show
sidebarHolder.setMaxWidth(240);

// keep the same left slot but set the holder (with scroll) instead of raw VBox
root.setLeft(sidebarHolder);
// --- REPLACEMENT END ---


        mainContent = new StackPane();
mainContent.setPadding(new Insets(25));
mainContent.setAlignment(Pos.CENTER);

// ✅ Wrap mainContent in a ScrollPane so the whole area scrolls when content is large
ScrollPane scrollPane = new ScrollPane(mainContent);
scrollPane.setFitToWidth(true);
scrollPane.setFitToHeight(true);
scrollPane.setPannable(true);
scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

root.setCenter(scrollPane);

switchPanel("🏠 Dashboard");


        return new Scene(root, 1150, 700);
    }

    private void animateHover(Label btn, boolean enter) {
        Color targetColor = enter ? Color.web("#00c6ff40") : Color.TRANSPARENT;
        Timeline t = new Timeline(
                new KeyFrame(Duration.millis(250),
                        new KeyValue(btn.backgroundProperty(),
                                new Background(new BackgroundFill(targetColor, new CornerRadii(20), Insets.EMPTY)))),
                new KeyFrame(Duration.millis(200),
                        new KeyValue(btn.scaleXProperty(), enter ? 1.08 : 1),
                        new KeyValue(btn.scaleYProperty(), enter ? 1.08 : 1))
        );
        t.play();
    }

    private VBox livelyCard(String title, String content) {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-background-radius: 20; "
                + "-fx-border-color: rgba(0,198,255,0.5); -fx-border-width: 1.5; -fx-border-radius: 20;");
        card.setEffect(new DropShadow(10, Color.web("#00c6ff40")));

        Label lblTitle = new Label(title);
        lblTitle.setFont(Font.font("Poppins", FontWeight.BOLD, 20));
        lblTitle.setTextFill(Color.WHITE);

        Label lblContent = new Label(content);
        lblContent.setFont(Font.font("Inter", 15));
        lblContent.setTextFill(Color.LIGHTGRAY);
        lblContent.setWrapText(true);
        lblContent.setAlignment(Pos.CENTER);

        card.getChildren().addAll(lblTitle, lblContent);

        card.setTranslateY(50);
        card.setOpacity(0);
        Timeline entrance = new Timeline(
                new KeyFrame(Duration.millis(600),
                        new KeyValue(card.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(card.translateYProperty(), 0, Interpolator.EASE_OUT))
        );
        entrance.play();

        ScaleTransition pulse = new ScaleTransition(Duration.millis(800), card);
        pulse.setFromX(0.95);
        pulse.setToX(1);
        pulse.setFromY(0.95);
        pulse.setToY(1);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();

        return card;
    }

    private void switchPanel(String name) {
        VBox panel = new VBox(25);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(30));

        Label title = new Label(name);
        title.setFont(Font.font("Poppins", FontWeight.BOLD, 26));
        title.setTextFill(Color.WHITE);
        panel.getChildren().add(title);

        switch (name) {
            case "🏠 Dashboard" -> {
                panel.getChildren().add(livelyCard("🎓 Welcome", "Your Campus. Your Companion."));
                panel.getChildren().add(livelyCard("💡 Quick Tip", "Use the sidebar to explore modules."));
            }

case "🤖 AI Assistance" -> {
    // Main AI assistance box
    VBox aiBox = new VBox(20);
    aiBox.setAlignment(Pos.CENTER);
    aiBox.setPadding(new Insets(25));
    aiBox.setStyle("-fx-background-color: rgba(0,0,0,0.35); "
            + "-fx-background-radius: 20; -fx-border-color: rgba(0,198,255,0.4); "
            + "-fx-border-width: 1.5; -fx-border-radius: 20;");
    aiBox.setEffect(new DropShadow(15, Color.web("#00c6ff40")));

    // Header
    Label header = new Label("Say Hi to Arivu");
    header.setFont(Font.font("Poppins", FontWeight.BOLD, 28));
    header.setTextFill(Color.WHITE);

    Label subtitle = new Label("Your Campus Assistant (powered by Llama2)");
    subtitle.setFont(Font.font("Inter", 14));
    subtitle.setTextFill(Color.LIGHTGRAY);

    // Chat container
    VBox chatContainer = new VBox(10);
    chatContainer.setPadding(new Insets(15));
    chatContainer.setAlignment(Pos.TOP_LEFT);
    chatContainer.setPrefHeight(420);
    chatContainer.setFillWidth(true);

    ScrollPane chatScroll = new ScrollPane(chatContainer);
    chatScroll.setFitToWidth(true);
    chatScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
    chatScroll.setPrefViewportHeight(420);
    chatScroll.setVvalue(1.0);

    // Input row
    HBox inputRow = new HBox(10);
    inputRow.setAlignment(Pos.CENTER);
    inputRow.setPadding(new Insets(10, 0, 0, 0));

    TextField userInput = new TextField();
    userInput.setPromptText("Ask something about SRM, academics, or campus life...");
    userInput.setPrefWidth(650);
    userInput.setStyle("-fx-background-color: rgba(255,255,255,0.08); "
            + "-fx-text-fill: white; -fx-prompt-text-fill: #cccccc; "
            + "-fx-background-radius: 20; -fx-border-radius: 20; -fx-font-size: 14; "
            + "-fx-border-color: rgba(0,198,255,0.4);");

    Button sendBtn = new Button("Send 🚀");
    sendBtn.setStyle("-fx-background-color: linear-gradient(to right,#00c6ff,#0072ff); "
            + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 8 25;");
    sendBtn.setOnMouseEntered(e -> sendBtn.setScaleX(1.05));
    sendBtn.setOnMouseExited(e -> sendBtn.setScaleX(1));

    inputRow.getChildren().addAll(userInput, sendBtn);

    aiBox.getChildren().addAll(header, subtitle, chatScroll, inputRow);
    panel.getChildren().add(aiBox);

    // Bubble styling
    final String USER_COLOR = "#00c6ff55";
    final String AI_COLOR = "#ffffff10";

    java.util.function.BiConsumer<String, Boolean> addMessage = (text, isUser) -> {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(580);
        msg.setFont(Font.font("Inter", 15));
        msg.setPadding(new Insets(10, 15, 10, 15));
        msg.setStyle("-fx-background-color: " + (isUser ? USER_COLOR : AI_COLOR)
                + "; -fx-background-radius: 18; -fx-border-radius: 18; "
                + "-fx-border-color: rgba(0,198,255,0.3); -fx-border-width: 1; -fx-text-fill: white;");

        HBox wrapper = new HBox(msg);
        wrapper.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(5, 5, 5, 5));

        msg.setOpacity(0);
        chatContainer.getChildren().add(wrapper);

        FadeTransition fade = new FadeTransition(Duration.millis(400), msg);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        chatScroll.setVvalue(1.0);
    };

    // Function to send prompt to Ollama
    Runnable sendPrompt = () -> {
        String prompt = userInput.getText().trim();
        if (prompt.isEmpty()) return;

        addMessage.accept(prompt, true);
        userInput.clear();
        addMessage.accept("💭 Thinking...", false);

        new Thread(() -> {
            try {
                String modelName = "llama2"; // adjust if using another model
                String jsonPayload = "{\"model\":\"" + modelName + "\",\"messages\":[{\"role\":\"user\",\"content\":\""
                        + prompt.replace("\"", "\\\"") + "\"}],\"max_tokens\":512}";

                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("http://127.0.0.1:11434/v1/chat/completions"))
                        .header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                java.net.http.HttpResponse<String> resp =
                        client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

                String aiResponse = extractResponseFromJson(resp.body());

                javafx.application.Platform.runLater(() -> {
                    chatContainer.getChildren().remove(chatContainer.getChildren().size() - 1);
                    addMessage.accept(aiResponse, false);
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    chatContainer.getChildren().remove(chatContainer.getChildren().size() - 1);
                    addMessage.accept("⚠️ Error: " + ex.getMessage(), false);
                });
            }
        }).start();
    };

    // Button click or Enter key triggers send
    sendBtn.setOnAction(e -> sendPrompt.run());
    userInput.setOnAction(e -> sendPrompt.run());
}



           // --------------------------- 📊 MARKS PANEL (Fixed Duplication) ---------------------------
case "🏆 Marks" -> {
    panel.getChildren().clear();

    VBox marksBox = livelyCard("📊 Marks Overview", "Your Internal Marks (out of 60)");
    marksBox.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-background-radius: 20; "
            + "-fx-border-color: rgba(0,198,255,0.5); -fx-border-width: 1.5; -fx-border-radius: 20;");

    TableView<Map<String, Object>> marksTable = new TableView<>();

    TableColumn<Map<String, Object>, String> subjCol = new TableColumn<>("Subject");
    subjCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("Subject")));

    TableColumn<Map<String, Object>, String> marksCol = new TableColumn<>("Internal Marks");
    marksCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("Marks"))));

    marksTable.getColumns().addAll(subjCol, marksCol);
    marksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    marksTable.setPrefHeight(350);

    String dbUrl = "jdbc:mysql://localhost:3306/APP_PROJECT_DATABASE?serverTimezone=UTC";
    String dbUser = "root";
    String dbPass = "ciqpyv_zeCjeb_2subbo";

    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
         Statement stmt = conn.createStatement()) {

        String sql = "SELECT * FROM Student_Login_Details WHERE SRM_Email_ID = '" + loggedEmail + "' LIMIT 1";
        ResultSet rs = stmt.executeQuery(sql);

        List<Map<String, Object>> rows = new ArrayList<>();

        if (rs.next()) {
            Map<String, Integer> subjectColumns = new LinkedHashMap<>();
            subjectColumns.put("UHV-II", rs.getInt("UHV_II_Marks"));
            subjectColumns.put("COMPUTER ORGANIZATION AND ARCHITECTURE", rs.getInt("COA_Marks"));
            subjectColumns.put("ADVANCED PROGRAMMING PRACTICE", rs.getInt("ADV_PROG_Marks"));
            subjectColumns.put("DATA STRUCTURES AND ALGORITHMS", rs.getInt("DSA_Marks"));
            subjectColumns.put("DATA STRUCTURES AND ALGORITHMS LAB", rs.getInt("DSA_LAB_Marks"));
            subjectColumns.put("PROFESSIONAL ETHICS", rs.getInt("PROF_ETH_Marks"));
            subjectColumns.put("OBJECT ORIENTED DESIGN AND PROGRAMMING", rs.getInt("OODP_Marks"));
            subjectColumns.put("RASPBERRY PI FUNDAMENTALS", rs.getInt("RPI_Marks"));

            for (Map.Entry<String, Integer> e : subjectColumns.entrySet()) {
                Map<String, Object> row = new HashMap<>();
                row.put("Subject", e.getKey());
                // show blank if DB value is NULL (getInt returns 0 for NULL, so check wasNull)
                int val = e.getValue() == 0 ? 0 : e.getValue();
                // Better detect actual NULL using ResultSet: re-query value per column instead of using map above
                // but since we already collected, use simple display: empty string if zero
                row.put("Marks", (val == 0 ? "" : val));
                rows.add(row);
            }
        }

        marksTable.getItems().setAll(rows);

        if (rows.isEmpty()) {
            Label noData = new Label("⚠️ No marks found for this account.");
            noData.setTextFill(Color.LIGHTGRAY);
            marksBox.getChildren().add(noData);
        }

    } catch (SQLException ex) {
        Label error = new Label("\u26A0 Database error: " + ex.getMessage());
        error.setTextFill(Color.RED);
        marksBox.getChildren().add(error);
    }

    marksBox.getChildren().add(marksTable);
    panel.getChildren().add(marksBox);
}


            // ✅ Updated Timetable section with embedded image + improved zoom & movement
            case "📅 Timetable" -> {
                // Make the timetable card darker and allow freer movement + central alignment
                VBox timetableBox = livelyCard("📅 Weekly Timetable", "Here\u2019s your class schedule for the semester:");
                timetableBox.setStyle("-fx-background-color: rgba(0,0,0,0.28); -fx-background-radius: 20; "
                        + "-fx-border-color: rgba(0,198,255,0.5); -fx-border-width: 1.5; -fx-border-radius: 20;");

                ImageView timetableImage = new ImageView(new Image("file:/C:/Users/Srihari%20Srivathsan/OneDrive/Documents/JAVA%20PROGRAMS/Time_Table.png"));
                timetableImage.setPreserveRatio(true);
                timetableImage.setFitWidth(900);

                // Put the image inside a resizable Pane so it can be freely moved (dragged)
                Pane dragPane = new Pane();
                dragPane.setPrefSize(1400, 1000); // larger virtual area to give freedom of movement
                // center the image initially
                timetableImage.setLayoutX((dragPane.getPrefWidth() - timetableImage.getFitWidth()) / 2.0);
                timetableImage.setLayoutY(40);

                dragPane.getChildren().add(timetableImage);

                // Allow dragging the image for free movement
                final double[] dragOffset = new double[2];
                timetableImage.setOnMousePressed(ev -> {
                    dragOffset[0] = ev.getSceneX() - timetableImage.getTranslateX();
                    dragOffset[1] = ev.getSceneY() - timetableImage.getTranslateY();
                });
                timetableImage.setOnMouseDragged(ev -> {
                    timetableImage.setTranslateX(ev.getSceneX() - dragOffset[0]);
                    timetableImage.setTranslateY(ev.getSceneY() - dragOffset[1]);
                });

                // Zoom controls (slider + +/-) already present — keep that and bind scale
                Slider zoomSlider = new Slider(0.5, 2.0, 1.0);
                zoomSlider.setMajorTickUnit(0.5);
                zoomSlider.setShowTickMarks(false);
                zoomSlider.setShowTickLabels(false);
                zoomSlider.setPrefWidth(300);

                Button zoomIn = new Button("+");
                Button zoomOut = new Button("−");
                zoomIn.setStyle("-fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 10;");
                zoomOut.setStyle("-fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 10;");

                timetableImage.scaleXProperty().bind(zoomSlider.valueProperty());
                timetableImage.scaleYProperty().bind(zoomSlider.valueProperty());

                zoomIn.setOnAction(e -> {
                    double v = Math.min(zoomSlider.getMax(), zoomSlider.getValue() + 0.1);
                    zoomSlider.setValue(Math.round(v * 100.0) / 100.0);
                });
                zoomOut.setOnAction(e -> {
                    double v = Math.max(zoomSlider.getMin(), zoomSlider.getValue() - 0.1);
                    zoomSlider.setValue(Math.round(v * 100.0) / 100.0);
                });

                HBox zoomControls = new HBox(10, zoomOut, zoomSlider, zoomIn);
                zoomControls.setAlignment(Pos.CENTER);

                // Wrap the large dragPane in a ScrollPane so the user can pan the larger area when zoomed
                StackPane centerHolder = new StackPane();
                centerHolder.setAlignment(Pos.CENTER);
                centerHolder.getChildren().add(dragPane); // keeps it centered inside scroll viewport

                ScrollPane imagePane = new ScrollPane(centerHolder);
                imagePane.setPrefViewportWidth(920);
                imagePane.setPrefViewportHeight(520);
                imagePane.setFitToWidth(false); // don't force-fit so centering works
                imagePane.setFitToHeight(false);
                imagePane.setPannable(true);

                timetableBox.getChildren().addAll(imagePane, zoomControls);
                panel.getChildren().add(timetableBox);
            }


            case "📢 Notices" -> panel.getChildren().add(livelyCard("📢 Announcements",
                    "- Midterm next week\n- Hackathon reg closes soon"));
            case "🗺 Campus Map" -> {
    WebView mapView = new WebView();
    mapView.setPrefSize(1000, 600);

    final javafx.scene.web.WebEngine webEngine = mapView.getEngine();
    // Use a desktop user agent so Google serves the full maps page where possible
    webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117 Safari/537.36");

    // Full Google Maps URL (centered on SRM + reasonable zoom)
    String mapsUrl = "https://www.google.com/maps/place/SRM+Institute+of+Science+and+Technology/@12.8236035,80.0422723,17z";

    // Load the full maps page directly into the WebView so the larger view is shown by default
    webEngine.load(mapsUrl);

    // Intercept popups (target="_blank") and open them in the system browser
    webEngine.setCreatePopupHandler(pf -> {
        WebView popup = new WebView();
        popup.getEngine().getLoadWorker().stateProperty().addListener((obs, oldS, newS) -> {
            if (newS == javafx.concurrent.Worker.State.SUCCEEDED) {
                String popupUrl = popup.getEngine().getLocation();
                if (popupUrl != null && popupUrl.startsWith("http")) {
                    try { java.awt.Desktop.getDesktop().browse(new java.net.URI(popupUrl)); }
                    catch (Exception ex) { /* ignore or log */ }
                }
            }
        });
        return popup.getEngine();
    });

    // If the WebView fails to load the full map (common due to web engine limitations),
    // open the system browser as a fallback and show a small message.
    webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldEx, newEx) -> {
        if (newEx != null) {
            // we're already on the FX thread in this listener, so it's safe
            new Alert(Alert.AlertType.WARNING,
                    "Map may not load fully inside the app. The full map will open in your browser.")
                    .showAndWait();
            try { java.awt.Desktop.getDesktop().browse(new java.net.URI(mapsUrl)); }
            catch (Exception ex) { /* ignore */ }

            // load embed HTML from external file instead of inline HTML
            String embedHTML = readHtmlFile(MAPS_EMBED_PATH);
            webEngine.loadContent(embedHTML);
        }
    });

    VBox mapCard = livelyCard("🧭 Campus Map", "Large map view embedded below:");
    mapCard.getChildren().add(mapView);

    // Keep a handy explicit fallback button (optional) for users who want the native browser.
    Button openLargeMap = new Button("Open in Browser");
    openLargeMap.setStyle("-fx-background-color: linear-gradient(to right,#00c6ff,#0072ff); "
            + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 18;");
    openLargeMap.setOnAction(ev -> {
        try { java.awt.Desktop.getDesktop().browse(new java.net.URI(mapsUrl)); }
        catch (Exception ex) { new Alert(Alert.AlertType.ERROR, "⚠ Unable to open larger map!").showAndWait(); }
    });

    mapCard.getChildren().add(openLargeMap);
    panel.getChildren().add(mapCard);
}

            // 🍽 Dining — replaced to include image with same zoom & drag behavior
            case "🍽 Dining" -> {
                VBox diningBox = livelyCard("🍱 Mess Menu", "");
                diningBox.setStyle("-fx-background-color: rgba(0,0,0,0.28); -fx-background-radius: 20; "
                        + "-fx-border-color: rgba(0,198,255,0.5); -fx-border-width: 1.5; -fx-border-radius: 20;");

                // Image (use percent-encoding for spaces like other images in your code)
                ImageView diningImage = new ImageView(new Image("file:/C:/Users/Srihari%20Srivathsan/OneDrive/Documents/JAVA%20PROGRAMS/Dining_Menu.jpg"));
                diningImage.setPreserveRatio(true);
                diningImage.setFitWidth(900);

                // Drag pane to allow freer movement
                Pane diningDragPane = new Pane();
                diningDragPane.setPrefSize(1400, 1000);
                diningImage.setLayoutX((diningDragPane.getPrefWidth() - diningImage.getFitWidth()) / 2.0);
                diningImage.setLayoutY(40);
                diningDragPane.getChildren().add(diningImage);

                final double[] diningDragOffset = new double[2];
                diningImage.setOnMousePressed(ev -> {
                    diningDragOffset[0] = ev.getSceneX() - diningImage.getTranslateX();
                    diningDragOffset[1] = ev.getSceneY() - diningImage.getTranslateY();
                });
                diningImage.setOnMouseDragged(ev -> {
                    diningImage.setTranslateX(ev.getSceneX() - diningDragOffset[0]);
                    diningImage.setTranslateY(ev.getSceneY() - diningDragOffset[1]);
                });

                // Zoom controls
                Slider diningZoomSlider = new Slider(0.5, 2.0, 1.0);
                diningZoomSlider.setMajorTickUnit(0.5);
                diningZoomSlider.setShowTickMarks(false);
                diningZoomSlider.setShowTickLabels(false);
                diningZoomSlider.setPrefWidth(300);

                Button diningZoomIn = new Button("+");
                Button diningZoomOut = new Button("−");
                diningZoomIn.setStyle("-fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 10;");
                diningZoomOut.setStyle("-fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 10;");

                diningImage.scaleXProperty().bind(diningZoomSlider.valueProperty());
                diningImage.scaleYProperty().bind(diningZoomSlider.valueProperty());

                diningZoomIn.setOnAction(e -> {
                    double v = Math.min(diningZoomSlider.getMax(), diningZoomSlider.getValue() + 0.1);
                    diningZoomSlider.setValue(Math.round(v * 100.0) / 100.0);
                });
                diningZoomOut.setOnAction(e -> {
                    double v = Math.max(diningZoomSlider.getMin(), diningZoomSlider.getValue() - 0.1);
                    diningZoomSlider.setValue(Math.round(v * 100.0) / 100.0);
                });

                HBox diningZoomControls = new HBox(10, diningZoomOut, diningZoomSlider, diningZoomIn);
                diningZoomControls.setAlignment(Pos.CENTER);

                StackPane diningCenterHolder = new StackPane();
                diningCenterHolder.setAlignment(Pos.CENTER);
                diningCenterHolder.getChildren().add(diningDragPane);

                ScrollPane diningPane = new ScrollPane(diningCenterHolder);
                diningPane.setPrefViewportWidth(920);
                diningPane.setPrefViewportHeight(520);
                diningPane.setFitToWidth(false);
                diningPane.setFitToHeight(false);
                diningPane.setPannable(true);

                diningBox.getChildren().addAll(diningPane, diningZoomControls);
                panel.getChildren().add(diningBox);
            }
// --------------------------- 📈 ATTENDANCE PANEL (Fixed Duplication) ---------------------------
case "📊 Student Attendance" -> {
    panel.getChildren().clear();

    VBox attendanceBox = livelyCard("📈 Student Attendance", "Attendance Summary (in %)");
    attendanceBox.setStyle("-fx-background-color: rgba(0,0,0,0.35); -fx-background-radius: 20; "
            + "-fx-border-color: rgba(0,198,255,0.5); -fx-border-width: 1.5; -fx-border-radius: 20;");

    TableView<Map<String, Object>> attendanceTable = new TableView<>();

    TableColumn<Map<String, Object>, String> subjCol = new TableColumn<>("Subject");
    subjCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty((String) data.getValue().get("Subject")));

    TableColumn<Map<String, Object>, String> attCol = new TableColumn<>("Attendance (%)");
    attCol.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().get("Attendance"))));

    attendanceTable.getColumns().addAll(subjCol, attCol);
    attendanceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    attendanceTable.setPrefHeight(350);

    String dbUrl = "jdbc:mysql://localhost:3306/APP_PROJECT_DATABASE?serverTimezone=UTC";
    String dbUser = "root";
    String dbPass = "ciqpyv_zeCjeb_2subbo";

    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
         Statement stmt = conn.createStatement()) {

        String sql = "SELECT * FROM Student_Login_Details WHERE SRM_Email_ID = '" + loggedEmail + "' LIMIT 1";
        ResultSet rs = stmt.executeQuery(sql);

        List<Map<String, Object>> rows = new ArrayList<>();

        if (rs.next()) {
            Map<String, Double> subjectAttendance = new LinkedHashMap<>();
            subjectAttendance.put("UHV-II", rs.getDouble("UHV_II_Attendance"));
            subjectAttendance.put("COMPUTER ORGANIZATION AND ARCHITECTURE", rs.getDouble("COA_Attendance"));
            subjectAttendance.put("ADVANCED PROGRAMMING PRACTICE", rs.getDouble("ADV_PROG_Attendance"));
            subjectAttendance.put("DATA STRUCTURES AND ALGORITHMS", rs.getDouble("DSA_Attendance"));
            subjectAttendance.put("DATA STRUCTURES AND ALGORITHMS LAB", rs.getDouble("DSA_LAB_Attendance"));
            subjectAttendance.put("PROFESSIONAL ETHICS", rs.getDouble("PROF_ETH_Attendance"));
            subjectAttendance.put("OBJECT ORIENTED DESIGN AND PROGRAMMING", rs.getDouble("OODP_Attendance"));
            subjectAttendance.put("RASPBERRY PI FUNDAMENTALS", rs.getDouble("RPI_Attendance"));

            for (Map.Entry<String, Double> e : subjectAttendance.entrySet()) {
                Map<String, Object> row = new HashMap<>();
                row.put("Subject", e.getKey());
                // Show blank if value is 0.0 (or absent)
                double v = e.getValue() == 0.0 ? 0.0 : e.getValue();
                String display = (v == 0.0 ? "" : String.format("%.2f", v));
                row.put("Attendance", display);
                rows.add(row);
            }
        }

        attendanceTable.getItems().setAll(rows);

        if (rows.isEmpty()) {
            Label noData = new Label("⚠️ No attendance records found.");
            noData.setTextFill(Color.LIGHTGRAY);
            attendanceBox.getChildren().add(noData);
        }

    } catch (SQLException ex) {
        Label error = new Label("\u26A0 Database Error: " + ex.getMessage());
        error.setTextFill(Color.RED);
        attendanceBox.getChildren().add(error);
    }

    attendanceBox.getChildren().add(attendanceTable);
    panel.getChildren().add(attendanceBox);
}



            case "💬 Feedback" -> {
    // Root feedback card styled similarly to AI Assistance
    VBox feedbackBox = new VBox(15);
    feedbackBox.setAlignment(Pos.CENTER);
    feedbackBox.setPadding(new Insets(25));
    feedbackBox.setStyle("-fx-background-color: rgba(0,0,0,0.35); "
            + "-fx-background-radius: 20; -fx-border-color: rgba(0,198,255,0.4); "
            + "-fx-border-width: 1.5; -fx-border-radius: 20;");
    feedbackBox.setEffect(new DropShadow(15, Color.web("#00c6ff40")));

    Label header = new Label("💬 Feedback");
    header.setFont(Font.font("Poppins", FontWeight.BOLD, 26));
    header.setTextFill(Color.WHITE);

    // A small subtitle / instruction
    Label sub = new Label("Share suggestions, issues or praise — we read every message.");
    sub.setFont(Font.font("Inter", 13));
    sub.setTextFill(Color.LIGHTGRAY);

    // Feedback feed (shows previous feedback or live session messages)
    VBox feedContainer = new VBox(8);
    feedContainer.setPadding(new Insets(12));
    feedContainer.setAlignment(Pos.TOP_LEFT);

    ScrollPane feedScroll = new ScrollPane(feedContainer);
    feedScroll.setFitToWidth(true);
    feedScroll.setPrefViewportHeight(260);
    feedScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

    // Helper to create message bubbles (reused inside this block)
    java.util.function.BiConsumer<String, Boolean> addFeedMessage = (text, isUser) -> {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(580);
        msg.setFont(Font.font("Inter", 14));
        msg.setPadding(new Insets(10, 14, 10, 14));
        msg.setStyle("-fx-background-color: " + (isUser ? "#00c6ff55" : "#ffffff10")
                + "; -fx-background-radius: 16; -fx-border-radius: 16; "
                + "-fx-border-color: rgba(0,198,255,0.18); -fx-border-width: 1; -fx-text-fill: white;");
        HBox wrapper = new HBox(msg);
        wrapper.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(6, 6, 6, 6));
        msg.setOpacity(0);
        feedContainer.getChildren().add(wrapper);
        FadeTransition ft = new FadeTransition(Duration.millis(360), msg);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
        feedScroll.setVvalue(1.0);
    };

    // Seed with an example system message
    addFeedMessage.accept("We'd love your feedback — try telling us what to improve or what you liked.", false);

    // Rating control (stars)
    HBox ratingRow = new HBox(8);
    ratingRow.setAlignment(Pos.CENTER_LEFT);
    Label rateLabel = new Label("Rate:");
    rateLabel.setFont(Font.font("Inter", 14));
    rateLabel.setTextFill(Color.LIGHTGRAY);

    ToggleGroup starsGroup = new ToggleGroup();
    HBox starsBox = new HBox(6);
    for (int i = 1; i <= 5; i++) {
        ToggleButton star = new ToggleButton("★");
        star.setUserData(i);
        star.setFont(Font.font("Inter", 18));
        star.setStyle("-fx-background-color: transparent; -fx-text-fill: #666666;");
        star.setToggleGroup(starsGroup);
        star.setOnMouseEntered(e -> star.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffd166;"));
        star.setOnMouseExited(e -> {
            if (!star.isSelected()) star.setStyle("-fx-background-color: transparent; -fx-text-fill: #666666;");
        });
        star.selectedProperty().addListener((obs, was, now) -> {
            // update styles across stars when selection changes
            for (Toggle t : starsGroup.getToggles()) {
                ToggleButton b = (ToggleButton) t;
                if (b.isSelected()) b.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffd166;");
                else b.setStyle("-fx-background-color: transparent; -fx-text-fill: #666666;");
            }
        });
        starsBox.getChildren().add(star);
    }
    ratingRow.getChildren().addAll(rateLabel, starsBox);

    // Input area (large TextArea like before but styled)
    TextArea fbInput = new TextArea();
    fbInput.setPromptText("Share your thoughts... (bug, suggestion, feature request, praise)");
    fbInput.setPrefHeight(120);
    fbInput.setWrapText(true);
    fbInput.setStyle("-fx-background-radius: 15; -fx-font-size: 14; -fx-control-inner-background: rgba(255,255,255,0.06); -fx-text-fill: white; -fx-prompt-text-fill: #bbbbbb;");

    // Smart quick-reply chips
    HBox chips = new HBox(8);
    chips.setAlignment(Pos.CENTER_LEFT);
    String[] quick = {"Bug", "UI", "Performance", "Feature request", "Other"};
    for (String q : quick) {
        Button chip = new Button(q);
        chip.setStyle("-fx-background-radius: 16; -fx-border-radius: 16; -fx-background-color: rgba(255,255,255,0.04); -fx-text-fill: white; -fx-padding: 6 12;");
        chip.setOnAction(e -> {
            fbInput.setText(q + ": ");
            fbInput.requestFocus();
            fbInput.positionCaret(fbInput.getText().length());
        });
        chips.getChildren().add(chip);
    }

    // Submit button
    Button submit = new Button("Submit ✉");
    submit.setStyle("-fx-background-color: linear-gradient(to right,#00c6ff,#0072ff); "
            + "-fx-text-fill: white; -fx-background-radius: 25; -fx-font-weight: bold; -fx-padding: 8 20;");
    submit.setOnMouseEntered(e -> submit.setScaleX(1.04));
    submit.setOnMouseExited(e -> submit.setScaleX(1));

    // Local helper to display a small success check and animated confirmation
    Runnable showSuccess = () -> {
        Label ok = new Label("✅ Feedback submitted — thanks!");
        ok.setFont(Font.font("Inter", 14));
        ok.setTextFill(Color.web("#b7ffef"));
        ok.setStyle("-fx-background-color: rgba(0,0,0,0.28); -fx-background-radius: 12; -fx-padding: 8 12;");
        ok.setOpacity(0);
        feedbackBox.getChildren().add(ok);
        FadeTransition f = new FadeTransition(Duration.millis(450), ok);
        f.setFromValue(0);
        f.setToValue(1);
        f.play();
        PauseTransition p = new PauseTransition(Duration.seconds(2.2));
        p.setOnFinished(ev -> {
            FadeTransition hide = new FadeTransition(Duration.millis(400), ok);
            hide.setFromValue(1);
            hide.setToValue(0);
            hide.setOnFinished(ev2 -> feedbackBox.getChildren().remove(ok));
            hide.play();
        });
        p.play();
    };

    // Submit handler: validate, add to feed, clear input, show success
    submit.setOnAction(e -> {
        String text = fbInput.getText().trim();
        if (text.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please enter feedback before submitting.").showAndWait();
            return;
        }
        // gather rating
        int rating = 0;
        if (starsGroup.getSelectedToggle() != null) rating = (int) starsGroup.getSelectedToggle().getUserData();
        String summary = (rating > 0 ? "Rating: " + rating + "/5\n" : "") + text;
        addFeedMessage.accept(summary, true);

        // Optionally: here you could call a method to persist feedback to DB or send to server.
        // For now we just show success UI and clear the input.
        fbInput.clear();
        starsGroup.selectToggle(null);
        showSuccess.run();
    });

    // Put everything together in feedbackBox
    HBox submitRow = new HBox(12, submit);
    submitRow.setAlignment(Pos.CENTER_RIGHT);

    feedbackBox.getChildren().addAll(header, sub, feedScroll, ratingRow, chips, fbInput, submitRow);
    panel.getChildren().add(feedbackBox);
}

            case "📚 Study Materials" -> {
                Button openFolder = new Button("📂 Open 'Year I' Folder");
                openFolder.setStyle("-fx-background-color: linear-gradient(to right,#00c6ff,#0072ff); "
                        + "-fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 25; -fx-padding: 10 20;");
                openFolder.setOnAction(e -> {
                    try {
                        String driveUrl = "https://drive.google.com/drive/folders/1-5YamFyjnLR6I5RyVEOluonRCBdVVzPA?usp=sharing";
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(driveUrl));
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "⚠ Unable to open Google Drive link!").showAndWait();
                    }
                });

                VBox studyBox = livelyCard("📚 Study Materials",
                        "Access all your study materials and notes directly from Google Drive.\n\nClick below to open your Year I folder:");
                studyBox.getChildren().add(openFolder);
                panel.getChildren().add(studyBox);
            }

            case "🗓 Calendar" -> {
    VBox calBox = livelyCard("🗓 Academic Calendar", "Here\u2019s your academic schedule for 2025\u20132026:");
    // make the calendar panel darker to improve contrast
    calBox.setStyle("-fx-background-color: rgba(0,0,0,0.28); -fx-background-radius: 20; "
            + "-fx-border-color: rgba(0,198,255,0.5); -fx-border-width: 1.5; -fx-border-radius: 20;");

    ImageView calendarImg = new ImageView(
        new Image("file:/C:/Users/Srihari%20Srivathsan/OneDrive/Documents/JAVA%20PROGRAMS/2025-2026%20ODD%20CALENDAR.jpg")
    );
    calendarImg.setPreserveRatio(true);
    calendarImg.setFitWidth(900);

    // Put calendar inside a larger Pane to allow freer movement and central alignment
    Pane calDragPane = new Pane();
    calDragPane.setPrefSize(1400, 1000);
    calendarImg.setLayoutX((calDragPane.getPrefWidth() - calendarImg.getFitWidth()) / 2.0);
    calendarImg.setLayoutY(40);
    calDragPane.getChildren().add(calendarImg);

    final double[] calDragOffset = new double[2];
    calendarImg.setOnMousePressed(ev -> {
        calDragOffset[0] = ev.getSceneX() - calendarImg.getTranslateX();
        calDragOffset[1] = ev.getSceneY() - calendarImg.getTranslateY();
    });
    calendarImg.setOnMouseDragged(ev -> {
        calendarImg.setTranslateX(ev.getSceneX() - calDragOffset[0]);
        calendarImg.setTranslateY(ev.getSceneY() - calDragOffset[1]);
    });

    Slider calZoomSlider = new Slider(0.5, 2.0, 1.0);
    calZoomSlider.setMajorTickUnit(0.5);
    calZoomSlider.setShowTickMarks(false);
    calZoomSlider.setShowTickLabels(false);
    calZoomSlider.setPrefWidth(300);

    Button calZoomIn = new Button("+");
    Button calZoomOut = new Button("−");
    calZoomIn.setStyle("-fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 10;");
    calZoomOut.setStyle("-fx-background-radius: 8; -fx-font-weight: bold; -fx-padding: 6 10;");

    calendarImg.scaleXProperty().bind(calZoomSlider.valueProperty());
    calendarImg.scaleYProperty().bind(calZoomSlider.valueProperty());

    calZoomIn.setOnAction(e -> {
        double v = Math.min(calZoomSlider.getMax(), calZoomSlider.getValue() + 0.1);
        calZoomSlider.setValue(Math.round(v * 100.0) / 100.0);
    });
    calZoomOut.setOnAction(e -> {
        double v = Math.max(calZoomSlider.getMin(), calZoomSlider.getValue() - 0.1);
        calZoomSlider.setValue(Math.round(v * 100.0) / 100.0);
    });

    HBox calZoomControls = new HBox(10, calZoomOut, calZoomSlider, calZoomIn);
    calZoomControls.setAlignment(Pos.CENTER);

    StackPane calCenterHolder = new StackPane();
    calCenterHolder.setAlignment(Pos.CENTER);
    calCenterHolder.getChildren().add(calDragPane);

    ScrollPane calPane = new ScrollPane(calCenterHolder);
    calPane.setPrefViewportWidth(920);
    calPane.setPrefViewportHeight(520);
    calPane.setFitToWidth(false);
    calPane.setFitToHeight(false);
    calPane.setPannable(true);

    calBox.getChildren().addAll(calPane, calZoomControls);
    panel.getChildren().add(calBox);
}

        }

        mainContent.getChildren().setAll(panel);
    }

private String queryOllama(String prompt) throws IOException {
    String model = "llama2"; // or change to "llama3", "mistral", etc. if installed

    String json = "{ \"model\": \"" + model + "\", \"prompt\": \"" + prompt.replace("\"", "\\\"") + "\" }";
    java.net.URL url = new java.net.URL("http://localhost:11434/api/generate");
    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);

    try (java.io.OutputStream os = conn.getOutputStream()) {
        os.write(json.getBytes());
    }

    StringBuilder response = new StringBuilder();
    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()))) {
        String line;
        while ((line = br.readLine()) != null) {
            if (line.contains("\"response\"")) {
                int start = line.indexOf(":") + 2;
                int end = line.lastIndexOf("\"");
                if (start > 0 && end > start)
                    response.append(line.substring(start, end));
            }
        }
    }
    return response.toString().trim();
}
// 🆕 UPDATED: robust extractor that supports both /v1/chat/completions and /api/generate
    private String extractResponseFromJson(String json) {
        if (json == null || json.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        // 1) Prefer choices[].message.content (OpenAI-compatible chat API)
        Pattern p = Pattern.compile(
            "\"message\"\\s*:\\s*\\{[^}]*\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",
            Pattern.CASE_INSENSITIVE
        );
        Matcher m = p.matcher(json);
        while (m.find()) {
            sb.append(unescapeJsonString(m.group(1)));
        }
        if (sb.length() > 0) return sb.toString().trim();

        // 2) Try any "content": "..." fields (some Ollama variants place it directly)
        p = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.CASE_INSENSITIVE);
        m = p.matcher(json);
        while (m.find()) {
            String chunk = m.group(1);
            if (chunk != null && !chunk.isEmpty()) sb.append(unescapeJsonString(chunk)).append("\n");
        }
        if (sb.length() > 0) return sb.toString().trim();

        // 3) Fallback to streaming /api/generate chunked "response": "..."
        p = Pattern.compile("\"response\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.CASE_INSENSITIVE);
        m = p.matcher(json);
        while (m.find()) {
            sb.append(unescapeJsonString(m.group(1)));
        }
        if (sb.length() > 0) return sb.toString().trim();

        // 4) If nothing matched, return a truncated copy for debugging
        return json.length() > 1000 ? json.substring(0, 1000) + "\n...[truncated]" : json;
    }

    private String unescapeJsonString(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i++);
            if (c == '\\' && i < s.length()) {
                char esc = s.charAt(i++);
                switch (esc) {
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case '"': sb.append('"'); break;
                    case 'u':
                        if (i + 3 < s.length()) {
                            String hex = s.substring(i, i + 4);
                            try {
                                int code = Integer.parseInt(hex, 16);
                                sb.append((char) code);
                                i += 4;
                            } catch (NumberFormatException ex) {
                                sb.append("\\u").append(hex);
                                i += 4;
                            }
                        } else {
                            sb.append("\\u");
                        }
                        break;
                    default:
                        sb.append(esc);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        launch();
    }
}


