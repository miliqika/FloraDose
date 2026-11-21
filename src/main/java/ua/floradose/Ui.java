package ua.floradose;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

final class Ui {
    static Button button(String t, String icon, Runnable r) { Button b = new Button(t); if (icon != null) { b.setGraphic(Icons.of(icon, 15)); b.setGraphicTextGap(6); } b.setOnAction(e -> r.run()); return b; }
    static Button primary(String t, String icon, Runnable r) { Button b = button(t, icon, r); b.getStyleClass().add("btn-primary"); return b; }
    static Button danger(String t, String icon, Runnable r) { Button b = button(t, icon, r); b.getStyleClass().add("btn-danger"); return b; }
    static Label hint(String t) { Label l = new Label(t); l.setWrapText(true); l.getStyleClass().add("hint"); return l; }
    static Label title(String t) { Label l = new Label(t); l.getStyleClass().add("pane-title"); return l; }
    static Node slider(Ctx c, String name, double min, double max, Supplier<Double> get, Consumer<Double> set, String unit) {
        Slider s = new Slider(min, max, get.get()); s.setPrefWidth(170); HBox.setHgrow(s, Priority.SOMETIMES);
        Label v = new Label(), n = new Label(name); n.setPrefWidth(135); n.setMinWidth(100); v.setPrefWidth(66);
        s.valueProperty().addListener((o, a, b) -> { if (!c.syncing()) { set.accept(b.doubleValue()); c.repaint(); } });
        c.addSyncer(() -> { s.setValue(get.get()); v.setText(String.format("%.0f", get.get()) + " " + unit); });
        HBox h = new HBox(8, n, s, v); h.setAlignment(Pos.CENTER_LEFT); return h;
    }
    static <T> TableColumn<T, String> col(String t, double w, Function<T, String> f) {
        TableColumn<T, String> c = new TableColumn<>(t); c.setPrefWidth(w); c.setCellValueFactory(d -> new SimpleStringProperty(f.apply(d.getValue()))); return c;
    }
    static ChoiceBox<String> faultBox(Consumer<Integer> set) {
        ChoiceBox<String> c = new ChoiceBox<>(); c.getItems().addAll("немає", "1", "2", "3", "4", "5"); c.setValue("немає");
        c.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> set.accept(b.intValue() - 1)); return c;
    }
    static String time(double t) { return String.format("%02d:%02d", (int) t / 60, (int) t % 60); }
    static final String[] LEVEL = {"Інфо", "Увага", "Помилка"};
    private Ui() {}
}
