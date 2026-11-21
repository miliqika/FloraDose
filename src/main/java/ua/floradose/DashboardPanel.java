package ua.floradose;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.Locale;

final class DashboardPanel {
    private static Label label(String text, String... styles) { Label l = new Label(text); l.getStyleClass().addAll(styles); return l; }
    private static void state(Label l, int sev) { l.getStyleClass().removeAll("st-ok", "st-warn", "st-err", "st-idle"); l.getStyleClass().add(sev == 0 ? "st-ok" : sev == 1 ? "st-warn" : "st-err"); }
    private static String days(double d) { return d >= 99 ? "> 99 діб" : String.format(Locale.ROOT, "≈ %.1f діб", d).replace('.', ','); }

    private static final class Tile {
        final VBox box; final Label value = label("", "tile-value"), sub = label("", "tile-sub"); final ProgressBar bar;
        Tile(String title, boolean withBar) {
            bar = withBar ? new ProgressBar(0) : null; if (bar != null) bar.setMaxWidth(Double.MAX_VALUE);
            sub.setWrapText(true); value.setWrapText(true); value.setMinHeight(Region.USE_PREF_SIZE); box = new VBox(4, label(title, "tile-title"), value, sub); if (bar != null) box.getChildren().add(2, bar);
            box.getStyleClass().add("tile"); box.setMaxWidth(Double.MAX_VALUE); GridPane.setHgrow(box, Priority.ALWAYS); GridPane.setFillWidth(box, true);
        }
    }

    Node build(Ctx c) {
        Sim s = c.sim();
        Label title = label("FloraDose", "dash-title"), sys = label("", "badge-big"); Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox head = new HBox(10, title, sp, sys); head.setAlignment(Pos.CENTER_LEFT);
        Tile water = new Tile("Рівень води", true), plants = new Tile("Рослин у пристрої", false), need = new Tile("Потребують поливу", false), used = new Tile("Витрачено за добу", false),
             reserve = new Tile("Запас води", false), diag = new Tile("Діагностика", false);
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8);
        ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(50); grid.getColumnConstraints().addAll(cc, cc);
        Tile[] tiles = {water, plants, need, used, reserve, diag}; for (int k = 0; k < tiles.length; k++) grid.add(tiles[k].box, k % 2, k / 2);
        ToggleButton auto = new ToggleButton(); auto.getStyleClass().add("switch"); auto.setOnAction(e -> { if (c.syncing()) return; s.autoStart = auto.isSelected(); s.ev("Система", 0, 0, "Автополив " + (s.autoStart ? "увімкнено" : "вимкнено")); c.repaint(); });
        HBox ledBox = new HBox(); ledBox.getStyleClass().add("seg"); ToggleGroup lg = new ToggleGroup(); ToggleButton[] lb = new ToggleButton[3];
        for (int k = 0; k < 3; k++) { final int m = k; ToggleButton b = new ToggleButton(Sim.LED_MODES[k]); b.setToggleGroup(lg); b.getStyleClass().add("seg-btn"); b.setOnAction(e -> { if (!c.syncing()) { s.setLedMode(m); c.repaint(); } }); lb[k] = b; ledBox.getChildren().add(b); }
        lg.selectedToggleProperty().addListener((o, a, b) -> { if (b == null && a != null) a.setSelected(true); });
        Button now = Ui.primary("Полити зараз", "water", c::startCycle), refill = Ui.button("Долити воду", "tank", () -> { s.refill(100); c.repaint(); });
        now.setTooltip(new Tooltip("Один цикл: полив лише слотів нижче порогу вологості"));
        GridPane ctl = new GridPane(); ctl.setHgap(10); ctl.setVgap(8); ColumnConstraints c0 = new ColumnConstraints(); c0.setHgrow(Priority.ALWAYS); ctl.getColumnConstraints().add(c0);
        ctl.add(label("Автополив", "ctl-name"), 0, 0); ctl.add(auto, 1, 0); ctl.add(label("Підсвічування", "ctl-name"), 0, 1); ctl.add(ledBox, 1, 1);
        FlowPane actions = new FlowPane(8, 8, now, refill);
        Label next = label("", "next-line"); next.setWrapText(true);
        VBox controls = new VBox(10, label("Керування", "pane-title"), ctl, actions, next); controls.getStyleClass().add("card"); controls.setPadding(new Insets(12));
        VBox slots = new VBox(6); slots.getStyleClass().add("card"); slots.setPadding(new Insets(12)); slots.getChildren().add(label("Мої рослини", "pane-title"));
        Label[] sname = new Label[5], sstat = new Label[5], snext = new Label[5]; ProgressBar[] sbar = new ProgressBar[5]; HBox[] rows = new HBox[5]; Button[] sgo = new Button[5];
        for (int i = 0; i < 5; i++) {
            final int k = i; sname[i] = label("", "slot-name"); sstat[i] = label("", "chip-state"); snext[i] = label("", "hint"); sbar[i] = new ProgressBar(0); sbar[i].getStyleClass().add("moist"); sbar[i].setMaxWidth(Double.MAX_VALUE); sbar[i].setPrefWidth(120);
            sgo[i] = Ui.button("Полити", "water", () -> c.startSlot(k)); sgo[i].getStyleClass().add("btn-compact");
            VBox mid = new VBox(3, new HBox(8, sname[i], sstat[i]), sbar[i], snext[i]); HBox.setHgrow(mid, Priority.ALWAYS); mid.setFillWidth(true);
            rows[i] = new HBox(10, mid, sgo[i]); rows[i].setAlignment(Pos.CENTER_LEFT); rows[i].getStyleClass().add("slot-row"); rows[i].setOnMouseClicked(e -> { if (e.getTarget() == rows[k] || !(e.getTarget() instanceof Button)) c.select("slot", k); });
            slots.getChildren().add(rows[i]);
        }
        c.addSyncer(() -> {
            int ov = s.overall(), nw = s.needWater(); boolean busy = s.running();
            sys.setText(s.onBattery ? "● Працює від акумулятора" : busy ? "● Іде полив…" : ov == 0 ? "● Система активна" : ov == 1 ? "● Потрібна увага" : "● Потрібне втручання"); state(sys, ov);
            water.value.setText((int) Math.round(s.tank) + " %"); water.bar.setProgress(s.tank / 100.0); water.bar.getStyleClass().removeAll("warn", "crit"); if (s.tank < 15) water.bar.getStyleClass().add("crit"); else if (s.tank < 40) water.bar.getStyleClass().add("warn"); water.sub.setText(String.format(Locale.ROOT, "≈ %.1f л із %.1f л", s.tankMl() / 1000, s.capL).replace('.', ','));
            plants.value.setText(String.valueOf(5)); plants.sub.setText("5 слотів · " + countSel(s) + " у циклі поливу");
            need.value.setText(String.valueOf(nw)); need.sub.setText(nw == 0 ? "усі рослини в нормі" : "вологість нижче порогу");
            used.value.setText((int) Math.round(s.usedToday()) + " мл"); used.sub.setText("за останні 24 год модельного часу");
            reserve.value.setText(days(s.daysLeft())); reserve.sub.setText(s.forecastBasis());
            java.util.List<String> is = s.issues(); diag.value.setText(is.isEmpty() ? "Усе гаразд" : String.valueOf(is.size())); diag.sub.setText(is.isEmpty() ? "помилок і попереджень немає" : (is.size() == 1 ? "зауваження:\n" : is.size() < 5 ? "зауваження:\n" : "зауважень:\n") + String.join("\n", is.subList(0, Math.min(3, is.size()))) + (is.size() > 3 ? "\n…" : ""));
            diag.value.getStyleClass().removeAll("tile-ok", "tile-bad"); diag.value.getStyleClass().add(is.isEmpty() ? "tile-ok" : "tile-bad");
            need.value.getStyleClass().removeAll("tile-ok", "tile-bad"); need.value.getStyleClass().add(nw == 0 ? "tile-ok" : "tile-bad");
            auto.setSelected(s.autoStart); auto.setText(s.autoStart ? "УВІМК" : "ВИМК"); lb[s.ledMode].setSelected(true); now.setDisable(busy);
            next.setText("Наступний полив: " + s.nextWateringText() + (s.autoStart ? "" : "  ·  автополив вимкнено"));
            for (int i = 0; i < 5; i++) {
                String st = s.bad[i] != 0 || s.vs[i] == 2 ? "Помилка" : s.moist[i] < s.thr(i) ? "Потрібен полив" : "Норма";
                sname[i].setText("S" + (i + 1) + " · " + Data.PLANTS[s.species[i]].name.split(" ")[0] + " · " + (int) s.moist[i] + " %"); sbar[i].setProgress(s.moist[i] / 100.0);
                sstat[i].setText(st); sstat[i].getStyleClass().removeAll("st-ok", "st-warn", "st-err"); sstat[i].getStyleClass().add(st.equals("Норма") ? "st-ok" : st.equals("Помилка") ? "st-err" : "st-warn");
                snext[i].setText("до поливу: " + WaterPanel.hours(s.hoursToNext(i))); sgo[i].setDisable(busy);
                rows[i].getStyleClass().remove("slot-row-sel"); if (c.selId().equals("slot") && c.selIdx() == i) rows[i].getStyleClass().add("slot-row-sel");
            }
        });
        Label hint = Ui.hint("Це простий режим: основні показники й швидкі дії. Подробиці, схеми й налаштування — в інженерному режимі (перемикач угорі).");
        VBox box = new VBox(10, head, grid, controls, slots, hint); box.setPadding(new Insets(8));
        ScrollPane sp2 = new ScrollPane(box); sp2.setFitToWidth(true); sp2.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp2.getStyleClass().add("side-pane");
        hint.prefWidthProperty().bind(sp2.widthProperty().subtract(46)); next.prefWidthProperty().bind(sp2.widthProperty().subtract(60));
        return sp2;
    }
    private static int countSel(Sim s) { int n = 0; for (boolean b : s.sel) if (b) n++; return n; }
}
