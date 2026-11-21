package ua.floradose;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import java.util.*;

final class InfoPanel {
    static final class TN { final String id; final int idx; final String label; TN(String id, int idx, String label) { this.id = id; this.idx = idx; this.label = label; } @Override public String toString() { return label; } }
    private static final String[][] GROUPS = {
        {"Конструкція", "shell", "window", "lid", "slot", "service", "frame"}, {"Водний контур", "tank", "filter", "pump", "manifold", "tube", "valve"},
        {"Електроніка", "panel", "led", "mcu", "pcb", "mux", "drv", "conn", "psu", "batt", "bay", "partition", "slotboard", "cable"}, {"Датчики", "level", "light", "ntc", "electrode", "loadcell", "mic"}};
    private static final Set<String> INST = new HashSet<>(Arrays.asList("slot", "valve", "slotboard", "light", "ntc", "electrode", "loadcell", "mic"));
    final TitledPane card = new TitledPane(); final TreeView<TN> tree = new TreeView<>(); final VBox treeBox;
    private final Ctx c; private boolean syncing; private final TabPane tabs = new TabPane();
    private final VBox desc = new VBox(6), params = new VBox(4), links = new VBox(4);
    private final Label name = new Label(), stateLbl = new Label(); private final Circle dot = new Circle(6); private final StackPane thumb = new StackPane(); private ToggleButton isoBtn;
    private final TextField search = new TextField(); private final ChoiceBox<String> catBox = new ChoiceBox<>();

    InfoPanel(Ctx c) {
        this.c = c;
        card.setText("Вибраний компонент"); card.setCollapsible(true); card.setExpanded(true);
        name.getStyleClass().add("card-title"); name.setWrapText(true); stateLbl.setWrapText(true); stateLbl.getStyleClass().add("state-line");
        thumb.setPrefSize(54, 54); thumb.setMinSize(54, 54); thumb.getStyleClass().add("thumb");
        Button show = Ui.button("Показати на моделі", "fit", c::focusSelected); isoBtn = new ToggleButton("Ізолювати"); isoBtn.setGraphic(Icons.of("model", 15)); isoBtn.setOnAction(e -> c.toggleIsolate());
        HBox acts = new HBox(6, show, isoBtn);
        VBox head = new VBox(3, name, new HBox(6, dot, stateLbl)); ((HBox) head.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);
        HBox top = new HBox(10, thumb, head); top.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(head, Priority.ALWAYS);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE); tabs.setPrefHeight(140); tabs.setMinHeight(100);
        tabs.getTabs().addAll(new Tab("Опис", wrap(desc)), new Tab("Параметри", wrap(params)), new Tab("Зв’язки", wrap(links)));
        VBox box = new VBox(8, top, acts, tabs); box.setPadding(new Insets(6)); card.setContent(box);
        search.setPromptText("Пошук компонента…"); HBox.setHgrow(search, Priority.ALWAYS);
        catBox.getItems().addAll("Усі категорії", "Конструкція", "Водний контур", "Електроніка", "Датчики"); catBox.setValue("Усі категорії");
        search.textProperty().addListener((o, a, b) -> rebuildTree()); catBox.valueProperty().addListener((o, a, b) -> rebuildTree());
        Button collapse = Ui.button("Згорнути", null, () -> setExpandedAll(false)), expand = Ui.button("Розгорнути", null, () -> setExpandedAll(true));
        tree.setShowRoot(false); tree.getStyleClass().add("comp-tree");
        tree.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> { if (!syncing && b != null && b.getValue().id != null) c.select(b.getValue().id, b.getValue().idx); });
        treeBox = new VBox(6, new HBox(6, search, catBox), new HBox(6, collapse, expand), tree); VBox.setVgrow(tree, Priority.ALWAYS);
        rebuildTree(); c.addSyncer(this::updateState);
    }
    private ScrollPane wrap(Node n) { ScrollPane s = new ScrollPane(n); s.setFitToWidth(true); s.setPadding(new Insets(4)); s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); return s; }
    private void setExpandedAll(boolean b) { if (tree.getRoot() != null) for (TreeItem<TN> g : tree.getRoot().getChildren()) g.setExpanded(b); }

    void rebuildTree() {
        int cfg = c.cfg(); String q = search.getText() == null ? "" : search.getText().toLowerCase().trim(); String cat = catBox.getValue() == null ? "Усі категорії" : catBox.getValue();
        TreeItem<TN> root = new TreeItem<>(new TN(null, -1, "")); root.setExpanded(true);
        for (String[] g : GROUPS) {
            if (!cat.equals("Усі категорії") && !cat.equals(g[0])) continue;
            int count = 0; for (int k = 1; k < g.length; k++) { Data.C d = Data.COMPS.get(g[k]); int qn = Data.qty(d, cfg); if (qn > 0 && (q.isEmpty() || (d.name + " " + d.model + " " + d.place + " " + Data.mat(d, cfg)).toLowerCase().contains(q))) count += qn; }
            TreeItem<TN> gi = rebuildGroup(g, cfg, q, count); if (!gi.getChildren().isEmpty()) root.getChildren().add(gi);
        }
        tree.setRoot(root);
    }
    private TreeItem<TN> rebuildGroup(String[] g, int cfg, String q, int count) {
        TreeItem<TN> gi = new TreeItem<>(new TN(null, -1, g[0] + "  (" + count + ")")); gi.setExpanded(true);
        for (int k = 1; k < g.length; k++) {
            Data.C d = Data.COMPS.get(g[k]); int qn = Data.qty(d, cfg); if (qn == 0) continue;
            if (!q.isEmpty() && !(d.name + " " + d.model + " " + d.place + " " + Data.mat(d, cfg)).toLowerCase().contains(q)) continue;
            TreeItem<TN> it = new TreeItem<>(new TN(d.id, -1, d.name + (qn > 1 ? "  ×" + qn : "")));
            if (INST.contains(d.id) && qn > 1) for (int i = 0; i < qn; i++) it.getChildren().add(new TreeItem<>(new TN(d.id, i, (d.id.equals("valve") ? "V" : "S") + (i + 1) + " — " + d.name)));
            gi.getChildren().add(it);
        }
        return gi;
    }
    void selectInTree(String id, int idx) {
        if (tree.getRoot() == null) return; syncing = true;
        for (TreeItem<TN> g : tree.getRoot().getChildren()) for (TreeItem<TN> it : g.getChildren()) {
            if (it.getValue().id.equals(id)) {
                TreeItem<TN> target = it; g.setExpanded(true);
                if (idx >= 0) for (TreeItem<TN> ch : it.getChildren()) if (ch.getValue().idx == idx) { target = ch; it.setExpanded(true); }
                tree.getSelectionModel().select(target); int row = tree.getRow(target); if (row >= 0) tree.scrollTo(row);
                syncing = false; return;
            }
        }
        syncing = false;
    }
    Object[] stateOf(String id, int idx) {
        Sim s = c.sim(); int i = idx;
        switch (id) {
            case "valve": if (i < 0) return new Object[]{"V1–V5: " + states(s), s.overall() == 2 ? 2 : 0};
                return new Object[]{"V" + (i + 1) + ": " + new String[]{"закритий", "ВІДКРИТИЙ — іде полив", "несправний"}[s.vs[i]], s.vs[i] == 2 ? 2 : s.vs[i] == 1 ? 0 : -1};
            case "slot": case "electrode": case "loadcell": case "slotboard": case "light": case "ntc": case "mic":
                if (i < 0) return new Object[]{"див. картки слотів", -1};
                boolean bad = s.bad[i] != 0 || s.fSensor == i || s.fComm == i; return new Object[]{"S" + (i + 1) + ": " + Data.SPECIES[s.species[i]] + ", вологість " + (int) s.moist[i] + " % (поріг " + s.thr(i) + " %), вага ≈ " + (int) s.weight(i) + " г, " + String.format("%.1f", s.slotTemp(i)) + " °C", bad ? 2 : s.moist[i] < s.thr(i) ? 1 : 0};
            case "tank": case "level": return new Object[]{String.format("Рівень %d %% ≈ %.2f л з %.1f л", (int) s.tank, s.tankMl() / 1000, s.capL), s.tank < 5 ? 2 : s.tank < 15 ? 1 : 0};
            case "pump": return new Object[]{"Помпа: " + s.pump, s.pump.equals("працює") ? 0 : -1}; case "mcu": return new Object[]{"MCU: " + s.mcu, s.mcu.equals("ОК") ? 0 : -1};
            case "led": return new Object[]{"Колір підсвітки: " + s.ledName(), s.ledState()}; case "batt": case "psu": return new Object[]{s.onBattery ? "Акумулятор " + (int) s.battery + " %" : "Мережа 12 В, акумулятор " + (int) s.battery + " %", s.onBattery ? 2 : 0};
            case "panel": return new Object[]{"Фаза: " + s.phaseName, -1};
            default: return new Object[]{"—", -1};
        }
    }
    private String states(Sim s) { StringBuilder b = new StringBuilder(); for (int i = 0; i < 5; i++) b.append(new String[]{"закр.", "ВІДКР.", "збій"}[s.vs[i]]).append(i < 4 ? ", " : ""); return b.toString(); }

    void update() {
        Data.C d = Data.COMPS.get(c.selId()); if (d == null) return; int idx = c.selIdx(), cfg = c.cfg(); boolean inst = idx >= 0 && d.qty > 1;
        card.setText("Вибраний компонент"); name.setText(d.name + (inst ? " · №" + (idx + 1) : ""));
        thumb.getChildren().setAll(Icons.of(d.cat.equals("water") ? "water" : d.cat.equals("el") ? "el" : "model", 26));
        thumb.setStyle("-fx-background-color:" + (d.cat.equals("water") ? "#DCEBF5" : d.cat.equals("el") ? "#DDEBDD" : "#EFE2CA") + ";-fx-background-radius:12;");
        isoBtn.setSelected(c.isolated());
        Label p = new Label(d.purpose); p.setWrapText(true); desc.getChildren().setAll(p);
        if (d.assumed) { Label w = new Label("⚠ Проєктне припущення: точних даних у лабораторних немає, модель і розташування — попереднє рішення."); w.setWrapText(true); w.getStyleClass().add("hint"); desc.getChildren().add(w); }
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(4); int r = 0;
        String[][] kv = {{"Тип", d.catName()}, {"Кількість", Data.qty(d, cfg) + (d.id.equals("light") && cfg == 0 ? " (спільний)" : "")}, {"Модель", d.model}, {"Матеріал", Data.mat(d, cfg)}, {"Розміри", d.dim}, {"Розташування", d.place}};
        for (String[] e : kv) { Label a = new Label(e[0]); a.getStyleClass().add("kv-key"); a.setMinWidth(92); Label b = new Label(e[1]); b.setWrapText(true); b.setMaxWidth(320); g.add(a, 0, r); g.add(b, 1, r++); }
        params.getChildren().setAll(g); links.getChildren().clear(); boolean any = false;
        for (String[] e : Schematic.E) if (Schematic.touches(e, d.id)) {
            any = true; boolean out = e[0].equals(d.id); Data.C o = Data.COMPS.get(out ? e[1] : e[0]);
            Label l = new Label((out ? "→  " : "←  ") + o.name + "   · " + (e[2].equals("w") ? "вода" : e[2].equals("p") ? "живлення" : "сигнали") + (e[3].isEmpty() ? "" : " (" + e[3] + ")")); l.setWrapText(true);
            l.setOnMouseClicked(ev -> c.select(o.id, -1)); l.getStyleClass().add("link-row"); links.getChildren().add(l);
        }
        if (!any) links.getChildren().add(Ui.hint("Окремих електричних або гідравлічних зв’язків у схемі немає (конструктивний елемент)."));
        updateState();
    }
    private void updateState() {
        if (c.selId() == null) return; Object[] st = stateOf(c.selId(), c.selIdx()); stateLbl.setText((String) st[0]); int lv = (Integer) st[1];
        dot.setFill(lv == 2 ? Color.web("#B8382B") : lv == 1 ? Color.web("#D9A021") : lv == 0 ? Color.web("#5F7F55") : Color.web("#B9AE9B"));
    }
}
