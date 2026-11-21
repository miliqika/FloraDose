package ua.floradose;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.util.List;

final class WaterPanel {
    private int slot = 0;
    private final Ctx c;
    WaterPanel(Ctx c) { this.c = c; }
    void showSlot(int i) { if (i >= 0 && i < 5) { slot = i; c.repaint(); } }

    String statusOf(int i) {
        Sim s = c.sim();
        if (s.bad[i] != 0 || s.vs[i] == 2 || (s.status[i] != null && s.sev[i] == 2)) return "Помилка";
        return s.moist[i] < s.thr(i) ? "Потрібен полив" : "Норма";
    }
    static String hours(double h) { return h <= 0 ? "зараз" : h < 1 ? "< 1 год" : h < 48 ? String.format("≈ %.0f год", h) : String.format("≈ %.1f діб", h / 24); }

    Node build() {
        Sim s = c.sim();
        HBox cards = new HBox(6);
        for (int i = 0; i < 5; i++) {
            final int k = i; Label n = new Label(), st = new Label(), v = new Label(), nx = new Label(); st.setWrapText(true); st.setMaxWidth(Double.MAX_VALUE);
            ProgressBar mb = new ProgressBar(); mb.getStyleClass().add("moist"); mb.setMaxWidth(Double.MAX_VALUE);
            VBox card = new VBox(3, n, mb, v, nx, st); card.getStyleClass().add("slot-card"); HBox.setHgrow(card, Priority.ALWAYS); card.setMaxWidth(Double.MAX_VALUE); card.setMinWidth(70);
            card.setOnMouseClicked(e -> { slot = k; c.select("slot", k); });
            c.addSyncer(() -> {
                n.setText("S" + (k + 1) + " · " + Data.SPECIES[s.species[k]].split(" ")[0]); mb.setProgress(s.moist[k] / 100.0); v.setText((int) s.moist[k] + " % · V" + (k + 1) + (s.vs[k] == 1 ? " ВІДКР." : s.vs[k] == 2 ? " ЗБІЙ" : ""));
                nx.setText("до поливу: " + hours(s.hoursToNext(k))); nx.getStyleClass().add("hint");
                String t = statusOf(k); st.setText(t); st.getStyleClass().removeAll("ok", "warn", "err"); st.getStyleClass().add(t.equals("Норма") ? "ok" : t.equals("Помилка") ? "err" : "warn");
                card.getStyleClass().remove("slot-card-sel"); if (slot == k) card.getStyleClass().add("slot-card-sel");
            });
            cards.getChildren().add(card);
        }
        Button all = Ui.primary("Полити всі слоти (авто)", "water", c::startAll);
        Label head = new Label(); head.getStyleClass().add("card-title");
        ChoiceBox<String> prof = new ChoiceBox<>(); prof.getItems().addAll(Data.SPECIES);
        prof.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> { if (!c.syncing() && b.intValue() >= 0) { s.setSpecies(slot, b.intValue()); c.repaint(); c.select("slot", slot); } });
        Label moistL = new Label(), target = new Label(), got = new Label(), next = new Label(), info = new Label(); for (Label l : new Label[]{moistL, target, got, next, info}) l.setWrapText(true);
        Slider ms = new Slider(0, 100, 30); ms.setPrefWidth(200); Label mv = new Label();
        ms.valueProperty().addListener((o, a, b) -> { if (!c.syncing()) { s.moist[slot] = b.doubleValue(); c.repaint(); } });
        Canvas spark = new Canvas(380, 70);
        Button valve = new Button(); valve.setOnAction(e -> { s.toggleValve(slot); c.repaint(); });
        Button one = Ui.primary("Полити цей слот", "water", () -> c.startSlot(slot));
        CheckBox use = new CheckBox("Брати участь у циклі"); use.setOnAction(e -> s.sel[slot] = use.isSelected());
        Button pick = Ui.button("Виділити на моделі", "model", () -> c.select("slot", slot));
        c.addSyncer(() -> {
            head.setText("Слот S" + (slot + 1) + " · " + Data.SPECIES[s.species[slot]] + " · " + statusOf(slot)); prof.getSelectionModel().select(s.species[slot]); ms.setValue(s.moist[slot]); mv.setText((int) s.moist[slot] + " %");
            moistL.setText("Поточна вологість: " + (int) s.moist[slot] + " % · температура слота " + String.format("%.1f", s.slotTemp(slot)) + " °C");
            target.setText("Потрібний рівень для виду: поріг поливу " + s.thr(slot) + " %, після дози " + (int) s.dose(slot) + " мл — до ≈ " + (int) s.targetMoisture(slot) + " %");
            got.setText("Отримано води за сеанс: " + (int) s.waterUsed[slot] + " мл" + (s.status[slot] == null ? "" : " · останній цикл: " + s.status[slot]));
            next.setText("Час до наступного поливу: " + hours(s.hoursToNext(slot)) + " (швидкість висихання " + String.format("%.2f", s.dryRate(slot)) + " %/год, демо-модель)");
            valve.setText(s.vs[slot] == 1 ? "Закрити клапан V" + (slot + 1) : "Відкрити клапан V" + (slot + 1)); use.setSelected(s.sel[slot]); one.setDisable(s.running());
            drawSpark(spark.getGraphicsContext2D(), s, slot);
        });
        ChoiceBox<String> soilBox = new ChoiceBox<>(); for (Data.Soil so : Data.SOILS) soilBox.getItems().add(so.name);
        soilBox.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> { if (!c.syncing() && b.intValue() >= 0) { s.setSoil(slot, b.intValue(), s.moist[slot]); c.repaint(); } });
        CheckBox indThr = new CheckBox("власний"), indDose = new CheckBox("власна"); Slider thrS = new Slider(5, 95, 40), doseS = new Slider(10, 200, 60); Label thrV = new Label(), doseV = new Label(); thrS.setPrefWidth(150); doseS.setPrefWidth(150);
        thrS.setBlockIncrement(1); doseS.setBlockIncrement(5);
        indThr.setOnAction(e -> { if (!c.syncing()) { s.setThreshold(slot, indThr.isSelected() ? (int) thrS.getValue() : -1); c.repaint(); } });
        indDose.setOnAction(e -> { if (!c.syncing()) { s.setDoseOverride(slot, indDose.isSelected() ? (int) doseS.getValue() : -1); c.repaint(); } });
        thrS.valueProperty().addListener((o, a, b) -> { if (!c.syncing() && indThr.isSelected()) { s.thrOv[slot] = (int) Math.round(b.doubleValue()); c.repaint(); } });
        doseS.valueProperty().addListener((o, a, b) -> { if (!c.syncing() && indDose.isSelected()) { s.doseOv[slot] = (int) Math.round(b.doubleValue()); c.repaint(); } });
        thrS.valueChangingProperty().addListener((o, a, ch) -> { if (!ch && indThr.isSelected()) s.setThreshold(slot, (int) Math.round(thrS.getValue())); });
        doseS.valueChangingProperty().addListener((o, a, ch) -> { if (!ch && indDose.isSelected()) s.setDoseOverride(slot, (int) Math.round(doseS.getValue())); });
        Label plantInfo = new Label(), climate = new Label(); plantInfo.setWrapText(true); climate.setWrapText(true); plantInfo.getStyleClass().add("hint");
        GridPane g = new GridPane(); g.setHgap(8); g.setVgap(6); g.add(new Label("Профіль рослини"), 0, 0); g.add(prof, 1, 0, 2, 1); g.add(new Label("Тип ґрунту"), 0, 1); g.add(soilBox, 1, 1, 2, 1);
        g.add(new Label("Поч. вологість"), 0, 2); g.add(new HBox(8, ms, mv), 1, 2, 2, 1);
        g.add(new Label("Поріг поливу"), 0, 3); g.add(new HBox(8, thrS, thrV), 1, 3); g.add(indThr, 2, 3); g.add(new Label("Доза поливу"), 0, 4); g.add(new HBox(8, doseS, doseV), 1, 4); g.add(indDose, 2, 4);
        ColumnConstraints g0 = new ColumnConstraints(), g1 = new ColumnConstraints(), g2 = new ColumnConstraints(); g0.setMinWidth(Region.USE_PREF_SIZE); g1.setHgrow(Priority.ALWAYS); g2.setMinWidth(Region.USE_PREF_SIZE); g.getColumnConstraints().addAll(g0, g1, g2);
        for (Slider sl : new Slider[]{thrS, doseS, ms}) { sl.setPrefWidth(110); sl.setMinWidth(60); HBox.setHgrow(sl, Priority.ALWAYS); } for (Label l : new Label[]{thrV, doseV, mv}) l.setMinWidth(Region.USE_PREF_SIZE); indThr.setMinWidth(Region.USE_PREF_SIZE); indDose.setMinWidth(Region.USE_PREF_SIZE);
        c.addSyncer(() -> {
            soilBox.getSelectionModel().select(s.soil[slot]); indThr.setSelected(s.thrOv[slot] >= 0); indDose.setSelected(s.doseOv[slot] >= 0);
            thrS.setValue(s.thr(slot)); thrS.setDisable(!indThr.isSelected()); thrV.setText(s.thr(slot) + " %" + (indThr.isSelected() ? "" : " (профіль)"));
            doseS.setValue(s.doseOv[slot] >= 0 ? s.doseOv[slot] : Data.DOSE[s.species[slot]]); doseS.setDisable(!indDose.isSelected()); doseV.setText((int) s.dose(slot) + " мл" + (indDose.isSelected() ? "" : " (профіль)"));
            Data.Plant pl = Data.PLANTS[s.species[slot]]; plantInfo.setText(pl.latin + " — " + pl.form + ". Комфорт: " + pl.ranges() + ". " + pl.tip + " Ґрунт: " + Data.SOILS[s.soil[slot]].note);
            int k = s.classify(slot); climate.setText("Умови слота: " + (k == 0 ? "у межах норми для виду" : "увага — " + Data.RISK[k]) + " (температура " + String.format("%.0f", s.slotTemp(slot)) + " °C, світло " + (int) s.lux + " лк)");
            climate.getStyleClass().removeAll("ok", "warn"); climate.getStyleClass().add(k == 0 ? "ok" : "warn");
        });
        VBox detail = new VBox(8, head, g, plantInfo, climate, moistL, target, got, next, Ui.title("Вологість за добу модельного часу"), spark, new HBox(8, one, valve), new HBox(8, pick, use)); detail.getStyleClass().add("card"); detail.setPadding(new Insets(10));
        VBox gen = new VBox(6, Ui.slider(c, "Рівень води", 0, 100, () -> s.tank, x -> s.tank = x, "%"), Ui.slider(c, "Корекція дози", 50, 150, () -> s.doseCorr, x -> s.doseCorr = x, "%"),
            Ui.slider(c, "Освітленість", 0, 1000, () -> s.lux, x -> s.lux = x, "лк"), Ui.slider(c, "Температура", 10, 40, () -> s.tempAmb, x -> s.tempAmb = x, "°C"));
        CheckBox dry = new CheckBox("Природне висихання ґрунту"); dry.setOnAction(e -> { if (!c.syncing()) s.drying = dry.isSelected(); }); c.addSyncer(() -> dry.setSelected(s.drying));
        CheckBox auto = new CheckBox("Автоматичний режим: автозапуск циклу за порогом вологості"); auto.setOnAction(e -> { if (!c.syncing()) { s.autoStart = auto.isSelected(); s.ev("Система", 0, 0, "Режим: " + (s.autoStart ? "автоматичний" : "ручний")); c.repaint(); } });
        Label modeL = new Label(); modeL.getStyleClass().add("hint"); c.addSyncer(() -> { auto.setSelected(s.autoStart); modeL.setText(s.autoStart ? "Режим автоматичний: цикл запускається сам, коли вологість нижче порогу." : "Режим ручний: полив — кнопками «Полити» або вручну клапанами."); });
        HBox ledBox = new HBox(); ledBox.getStyleClass().add("seg"); ToggleGroup lg = new ToggleGroup(); ToggleButton[] lb = new ToggleButton[3];
        for (int k = 0; k < 3; k++) { final int m = k; ToggleButton b = new ToggleButton(Sim.LED_MODES[k]); b.setToggleGroup(lg); b.getStyleClass().add("seg-btn"); b.setOnAction(e -> { if (!c.syncing()) { s.setLedMode(m); c.repaint(); } }); lb[k] = b; ledBox.getChildren().add(b); }
        lg.selectedToggleProperty().addListener((o, a, b) -> { if (b == null && a != null) a.setSelected(true); }); c.addSyncer(() -> lb[s.ledMode].setSelected(true));
        Slider ts = new Slider(60, 3600, 600); ts.setPrefWidth(140); Label tl = new Label("1 с = 10 хв");
        ts.valueProperty().addListener((o, a, b) -> { s.timeScale = b.doubleValue(); tl.setText("1 с = " + (int) (b.doubleValue() / 60) + " хв"); });
        gen.getChildren().addAll(dry, auto, modeL, new HBox(8, new Label("Підсвічування"), ledBox), new HBox(8, new Label("Швидкість часу"), ts, tl), Ui.hint("Коефіцієнти висихання — параметри демонстраційної моделі, не відкалібровані за реальними вимірюваннями."));
        TitledPane general = new TitledPane("Загальні параметри", gen); general.setExpanded(false);
        Node circuit = circuit(c);
        Label hint = Ui.hint("Виберіть слот: керування й показники — лише для нього. Кожен слот має власний клапан, датчики й профіль рослини.");
        VBox box = new VBox(10, hint, cards, all, circuit, detail, general); box.setPadding(new Insets(6));
        ScrollPane sp = new ScrollPane(box); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.getStyleClass().add("side-pane");
        hint.prefWidthProperty().bind(sp.widthProperty().subtract(46)); for (Label l : new Label[]{moistL, target, got, next}) l.prefWidthProperty().bind(sp.widthProperty().subtract(60));
        return sp;
    }
    private Node circuit(Ctx c) {
        Sim s = c.sim(); final double W = 400, H = 150; Canvas cv = new Canvas(W, H); GraphicsContext g = cv.getGraphicsContext2D();
        Label info = new Label(); info.getStyleClass().add("hint"); info.setWrapText(true);
        Button top = Ui.button("Долити до 100 %", "tank", () -> { s.refill(100); c.repaint(); }), plus = Ui.button("+ 500 мл", null, () -> { s.refill(s.tank + 500.0 / (s.capL * 10)); c.repaint(); });
        c.addSyncer(() -> {
            g.setFill(Color.web("#FFFBF4")); g.fillRect(0, 0, W, H); g.setStroke(Color.web("#DCCFB8")); g.strokeRect(0.5, 0.5, W - 1, H - 1);
            boolean anyFlow = false; for (boolean f : s.flow) anyFlow |= f; Color pipe = anyFlow ? Color.web("#4A8FC9") : Color.web("#B9B2A4");
            g.setStroke(Color.web("#8FB4C8")); g.setLineWidth(2); g.strokeRoundRect(14, 16, 70, 118, 10, 10);
            double lv = Math.min(1, Data.waterLevelCm(s.tank) / (Data.COL_H)), hh = 114 * lv; g.setFill(Color.web("#4A8FC9", 0.55)); g.fillRoundRect(16, 132 - hh, 66, hh, 8, 8);
            g.setTextAlign(javafx.scene.text.TextAlignment.CENTER); g.setFont(javafx.scene.text.Font.font(null, javafx.scene.text.FontWeight.BOLD, 13)); g.setFill(Color.web("#3A2D22")); g.fillText((int) s.tank + " %", 49, 76); g.setFont(javafx.scene.text.Font.font(10)); g.setFill(Color.web("#5C4E3E")); g.fillText("≈ " + String.format("%.1f", s.tankMl() / 1000) + " л", 49, 92); g.setTextAlign(javafx.scene.text.TextAlignment.LEFT); g.setFont(javafx.scene.text.Font.getDefault());
            g.setStroke(pipe); g.setLineWidth(4); g.strokeLine(84, 75, 106, 75);
            boolean pb = s.pumpBad || s.fPump; g.setFill(pb ? Color.web("#F6D8D2") : "працює".equals(s.pump) ? Color.web("#CFE6F7") : Color.web("#EFE6D5")); g.setStroke(pb ? Color.web("#B8382B") : Color.web("#8B6340")); g.setLineWidth(2); g.fillOval(106, 59, 32, 32); g.strokeOval(106, 59, 32, 32);
            g.setFill(Color.web("#3A2D22")); g.fillText("П", 118, 79); g.setStroke(pipe); g.setLineWidth(4); g.strokeLine(138, 75, 384, 75);
            for (int i = 0; i < 5; i++) {
                double x = 180 + i * 50; boolean open = s.vs[i] == 1, bad = s.vs[i] == 2; g.setStroke(open ? Color.web("#4A8FC9") : Color.web("#B9B2A4")); g.setLineWidth(4); g.strokeLine(x, 75, x, 92);
                g.setFill(bad ? Color.web("#B8382B") : open ? Color.web("#4A8FC9") : Color.web("#F2EADB")); g.setStroke(bad ? Color.web("#B8382B") : Color.web("#8B6340")); g.setLineWidth(2); g.fillRoundRect(x - 14, 92, 28, 26, 6, 6); g.strokeRoundRect(x - 14, 92, 28, 26, 6, 6);
                g.setTextAlign(javafx.scene.text.TextAlignment.CENTER); g.setFont(javafx.scene.text.Font.font(11)); g.setFill(open || bad ? Color.WHITE : Color.web("#3A2D22")); g.fillText("V" + (i + 1), x, 109); g.setFont(javafx.scene.text.Font.font(10)); g.setFill(Color.web("#7C6E5E")); String pn = Data.PLANTS[s.species[i]].name.split(" ")[0]; g.fillText(pn.length() > 6 ? pn.substring(0, 5) + "." : pn, x, 134); g.setTextAlign(javafx.scene.text.TextAlignment.LEFT); g.setFont(javafx.scene.text.Font.getDefault());
}
            info.setText("Клапани: клік по V1–V5 відкриває/закриває вручну (потік — блакитний). Помпа: " + s.pump + (pb ? " · НЕСПРАВНА" : "") + ". Запас води " + String.format("%.1f", s.daysLeft()) + " діб.");
        });
        cv.setOnMouseClicked(e -> { for (int i = 0; i < 5; i++) { double x = 180 + i * 50; if (Math.abs(e.getX() - x) <= 16 && e.getY() >= 90 && e.getY() <= 120) { s.toggleValve(i); c.select("valve", i); c.repaint(); return; } } if (e.getX() < 90) c.select("tank", -1); });
        cv.setCursor(javafx.scene.Cursor.HAND);
        VBox v = new VBox(6, Ui.title("Резервуар і клапани"), cv, info, new HBox(8, top, plus)); v.getStyleClass().add("card"); v.setPadding(new Insets(10)); return v;
    }
    private void drawSpark(GraphicsContext g, Sim s, int k) {
        double W = 380, H = 70; g.setFill(Color.web("#FFFBF4")); g.fillRect(0, 0, W, H); g.setStroke(Color.web("#DCCFB8")); g.strokeRect(0.5, 0.5, W - 1, H - 1);
        double thr = s.thr(k); g.setStroke(Color.web("#D9A021", 0.7)); g.setLineDashes(4, 4); double ty = H - thr / 100.0 * H; g.strokeLine(0, ty, W, ty); g.setLineDashes((double[]) null);
        List<Sim.Sample> h = s.history; if (h.size() < 2) { g.setFill(Color.web("#7A7266")); g.fillText("накопичення даних…", 10, 38); return; }
        double t1 = h.get(h.size() - 1).hour, t0 = Math.max(h.get(0).hour, t1 - 24); g.setStroke(Color.web("#5F7F55")); g.setLineWidth(2); g.beginPath(); boolean first = true;
        for (Sim.Sample p : h) { if (p.hour < t0) continue; double x = (p.hour - t0) / Math.max(0.01, t1 - t0) * W, y = H - p.moist[k] / 100.0 * H; if (first) { g.moveTo(x, y); first = false; } else g.lineTo(x, y); }
        g.stroke(); g.setLineWidth(1);
    }
}
