package ua.floradose;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class ConfigPanel {
    Node build(Ctx c) {
        HBox cards = new HBox(8);
        for (int i = 0; i < 3; i++) {
            final int k = i; Label t = new Label(Data.CFG[i]); t.getStyleClass().add("card-title"); Label p = new Label(Data.CFG_INFO[i][3]); p.getStyleClass().add("price-dark"); Label n = new Label(Data.CFG_INFO[i][4]); n.setWrapText(true); n.getStyleClass().add("hint");
            Button b = Ui.primary("Обрати", null, () -> c.setConfig(k)); VBox card = new VBox(4, t, p, n, b); card.getStyleClass().add("card"); HBox.setHgrow(card, Priority.ALWAYS); card.setMaxWidth(Double.MAX_VALUE);
            c.addSyncer(() -> { card.getStyleClass().remove("card-sel"); if (c.cfg() == k) card.getStyleClass().add("card-sel"); b.setDisable(c.cfg() == k); });
            cards.getChildren().add(card);
        }
        TableView<String[]> tv = new TableView<>(FXCollections.observableArrayList(Data.CFG_ROWS)); tv.setPrefHeight(300);
        tv.getColumns().add(Ui.<String[]>col("Параметр", 190, r -> r[0]));
        for (int i = 0; i < 3; i++) { final int k = i; TableColumn<String[], String> col = Ui.<String[]>col(Data.CFG[i], 170, r -> r[k + 1]);
            col.setCellFactory(cl -> new TableCell<String[], String>() { @Override protected void updateItem(String v, boolean e) { super.updateItem(v, e); setText(e ? null : v); getStyleClass().remove("cfg-sel"); if (!e && c.cfg() == k) getStyleClass().add("cfg-sel"); } });
            tv.getColumns().add(col); }
        Label diff = new Label(); diff.setWrapText(true);
        Label cost = new Label("Як формується вартість: базова комплектація Standard ≈ 129,5 $ (BOM Лаб. №6). Premium ≈ 153,5 $: шпон FSC на вологостійкій основі, загартоване скло панелі, 4 додаткові датчики освітленості й 5 акустичних мікрофонів. Eco ≈ 149,5 $: перероблений WPC, Tritan Renew, вкладиші з rPP, ті самі датчики й мікрофони. ⚠ Розбивку надбавки за окремими позиціями в лабораторних не наведено — ціни конфігурацій взято з Лаб. №6."); cost.setWrapText(true); cost.getStyleClass().add("hint");
        c.addSyncer(() -> {
            tv.refresh(); StringBuilder b = new StringBuilder(); int cfg = c.cfg();
            if (cfg == 0) b.append("Обрано базову конфігурацію Standard: один спільний датчик освітленості, мікрофонів немає.");
            else for (Data.C d : Data.COMPS.values()) { int dq = Data.qty(d, cfg) - Data.qty(d, 0); if (dq != 0) b.append(dq > 0 ? "+ " : "− ").append(d.name).append(": ").append(Math.abs(dq) > 0 ? (dq > 0 ? "з’являється ×" : "зникає ×") + Math.abs(dq) : "").append(" (разом ").append(Data.qty(d, cfg)).append(")\n"); }
            b.append("\nМатеріали: оболонка — ").append(Data.CFG_INFO[cfg][0]).append("; панель — ").append(Data.CFG_INFO[cfg][2]).append("; резервуар — ").append(Data.CFG_INFO[cfg][1]).append(".");
            diff.setText("Відмінності від Standard для обраної конфігурації (" + Data.CFG[cfg] + "):\n" + b);
        });
        Button rep = Ui.button("Звіт про склад пристрою (CSV)", "csv", () -> c.export().bomReport(c.cfg()));
        VBox v = new VBox(10, Ui.hint("Порівняйте конфігурації й оберіть одну: зміни одразу відображаються в моделі, специфікації та схемі."), cards, tv, Ui.title("Що змінюється"), diff, Ui.title("Вартість"), cost, rep);
        v.setPadding(new Insets(6)); ScrollPane sp = new ScrollPane(v); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.getStyleClass().add("side-pane"); return sp;
    }
}
