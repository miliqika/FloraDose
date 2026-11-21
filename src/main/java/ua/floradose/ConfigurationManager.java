package ua.floradose;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import java.util.function.IntConsumer;

final class ConfigurationManager {
    private int cfg; private final ToggleGroup group = new ToggleGroup(); private final Label price = new Label(); private final Tooltip tip = new Tooltip(); private final IntConsumer onChange; private boolean setting;
    ConfigurationManager(IntConsumer onChange) { this.onChange = onChange; }
    int cfg() { return cfg; }
    Node node() {
        HBox seg = new HBox(); seg.getStyleClass().add("seg");
        for (int i = 0; i < 3; i++) { ToggleButton b = new ToggleButton(Data.CFG[i]); b.setToggleGroup(group); b.getStyleClass().add("seg-btn"); b.setUserData(i); b.setSelected(i == 0); seg.getChildren().add(b); }
        group.selectedToggleProperty().addListener((o, a, b) -> { if (b == null) a.setSelected(true); else if (!setting) { cfg = (Integer) b.getUserData(); refresh(); onChange.accept(cfg); } });
        price.getStyleClass().add("price"); Button info = Ui.button("", "info", () -> { }); info.getStyleClass().add("btn-ghost"); tip.setShowDelay(Duration.millis(100)); info.setTooltip(tip);
        refresh(); HBox h = new HBox(8, seg, price, info); h.setAlignment(Pos.CENTER_LEFT); return h;
    }
    void set(int i) { cfg = i; setting = true; for (Toggle t : group.getToggles()) if ((Integer) t.getUserData() == i) group.selectToggle(t); setting = false; refresh(); }
    private void refresh() {
        price.setText(Data.CFG_INFO[cfg][3]);
        StringBuilder b = new StringBuilder(); for (int i = 0; i < 3; i++) b.append(Data.CFG[i]).append(" — ").append(Data.CFG_INFO[i][0]).append("; панель: ").append(Data.CFG_INFO[i][2]).append("; ").append(Data.CFG_INFO[i][4]).append("; ").append(Data.CFG_INFO[i][3]).append(i < 2 ? "\n" : ""); tip.setText(b.toString());
    }
}
