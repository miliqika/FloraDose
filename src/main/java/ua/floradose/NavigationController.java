package ua.floradose;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class NavigationController {
    private final VBox nav = new VBox(2); private final List<ToggleButton> btns = new ArrayList<>(); private final List<Label> groups = new ArrayList<>(); private final ToggleGroup tg = new ToggleGroup();
    private final SimpleDoubleProperty width = new SimpleDoubleProperty(224); private boolean collapsed; private final Consumer<Section> onSelect;
    NavigationController(Consumer<Section> onSelect) { this.onSelect = onSelect; }
    Node build() {
        nav.getStyleClass().add("nav"); Button burger = Ui.button("", "menu", () -> collapse(!collapsed)); burger.getStyleClass().add("btn-ghost"); burger.setTooltip(new Tooltip("Згорнути/розгорнути меню")); nav.getChildren().add(burger);
        for (Section s : Section.values()) {
            if (s.group != null) { Label g = new Label(s.group); g.getStyleClass().add("nav-group"); nav.getChildren().add(g); groups.add(g); }
            ToggleButton b = new ToggleButton(s.title); b.setGraphic(Icons.of(s.icon, 18)); b.setGraphicTextGap(10); b.getStyleClass().add("nav-btn"); b.setToggleGroup(tg); b.setUserData(s); b.setMaxWidth(Double.MAX_VALUE);
            b.prefWidthProperty().bind(width); b.minWidthProperty().bind(width); b.setTooltip(new Tooltip(s.title)); nav.getChildren().add(b); btns.add(b);
        }
        width.addListener((o, a, b) -> { boolean wide = b.doubleValue() > 120; for (ToggleButton t : btns) t.setText(wide ? ((Section) t.getUserData()).title : ""); for (Label l : groups) { l.setVisible(wide); l.setManaged(wide); } });
        tg.selectedToggleProperty().addListener((o, a, b) -> { if (b == null) a.setSelected(true); else onSelect.accept((Section) b.getUserData()); });
        return nav;
    }
    void select(Section s) { for (ToggleButton b : btns) if (b.getUserData() == s) b.setSelected(true); }
    boolean collapsed() { return collapsed; }
    void collapse(boolean c) {
        if (c == collapsed) return; collapsed = c;
        new Timeline(new KeyFrame(Duration.millis(220), new KeyValue(width, c ? 46 : 224, Interpolator.EASE_BOTH))).play();
    }
}
