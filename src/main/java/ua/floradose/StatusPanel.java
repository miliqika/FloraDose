package ua.floradose;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;

final class StatusPanel {
    static Node build(Ctx c) {
        Sim s = c.sim();
        Label overall = new Label(); overall.getStyleClass().add("badge-big");
        Label banner = new Label(); banner.setWrapText(true); banner.managedProperty().bind(banner.visibleProperty());
        ProgressBar wbar = new ProgressBar(); wbar.setMaxWidth(Double.MAX_VALUE);
        Label wl = new Label(), pw = new Label(), pump = new Label(), light = new Label(), phase = new Label(), fore = new Label(); fore.setWrapText(true);
        HBox valves = new HBox(6); Label[] vl = new Label[5];
        for (int i = 0; i < 5; i++) { vl[i] = new Label(); vl[i].getStyleClass().add("chip-state"); valves.getChildren().add(vl[i]); }
        GridPane slots = new GridPane(); slots.setHgap(6); slots.setVgap(6);
        Label[][] sl = new Label[5][3]; ProgressBar[] mb = new ProgressBar[5];
        for (int i = 0; i < 5; i++) {
            sl[i][0] = new Label(); sl[i][1] = new Label(); sl[i][2] = new Label(); mb[i] = new ProgressBar(); mb[i].getStyleClass().add("moist"); mb[i].setMaxWidth(Double.MAX_VALUE);
            VBox card = new VBox(2, sl[i][0], mb[i], sl[i][1], sl[i][2]); card.getStyleClass().add("slot-card"); card.setMaxWidth(Double.MAX_VALUE); final int k = i; card.setOnMouseClicked(e -> c.select("slot", k));
            slots.add(card, i % 3, i / 3); GridPane.setHgrow(card, Priority.ALWAYS);
        }
        for (int i = 0; i < 3; i++) { ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(33.3); slots.getColumnConstraints().add(cc); }
        c.addSyncer(() -> {
            int o = s.overall(); overall.setText("● " + s.overallText()); overall.getStyleClass().removeAll("st-ok", "st-warn", "st-err"); overall.getStyleClass().add(o == 0 ? "st-ok" : o == 1 ? "st-warn" : "st-err");
            boolean low = s.tank < 15, err = s.lastErr > 0 && (s.lastErr == 1 || s.lastErr == 3 || s.lastErr == 6 || s.lastErr == 2 || s.lastErr == 4);
            banner.getStyleClass().removeAll("banner-err", "banner-warn", "banner");
            if (s.onBattery) { banner.setText("⚠ Живлення від резервного акумулятора (" + (int) s.battery + " %). Що робити: " + Data.ERR[5].action); banner.getStyleClass().addAll("banner", "banner-err"); banner.setVisible(true); }
            else if (err) { Data.ErrorSpec e = Data.ERR[s.lastErr - 1]; banner.setText("⚠ " + e.code + ": " + e.cause + ". Що робити: " + e.action + (s.lastErr == 1 ? " Полив заблоковано за правилами безпеки." : "")); banner.getStyleClass().addAll("banner", "banner-err"); banner.setVisible(true); }
            else if (low) { banner.setText("Рівень води низький (" + (int) s.tank + " %). Що робити: " + Data.ERR[0].action); banner.getStyleClass().addAll("banner", "banner-warn"); banner.setVisible(true); }
            else banner.setVisible(false);
            wbar.setProgress(s.tank / 100.0); wbar.getStyleClass().removeAll("warn", "crit"); if (s.tank < 15) wbar.getStyleClass().add("crit"); else if (s.tank < 40) wbar.getStyleClass().add("warn");
            wl.setText(String.format("Вода: %.2f л (%d %%) з %.1f л · LED %s", s.tankMl() / 1000, (int) s.tank, s.capL, s.ledName()));
            pw.setText(s.onBattery ? "Живлення: акумулятор " + (int) s.battery + " %" : "Живлення: мережа 12 В · акумулятор " + (int) s.battery + " %");
            pump.setText("MCU: " + s.mcu + " · Помпа: " + s.pump); light.setText("Освітлення: " + (int) s.lux + " лк · підсвітка " + s.ledName()); phase.setText("Фаза: " + s.phaseName);
            fore.setText(String.format("Запас ≈ %.1f діб (%s)", s.daysLeft(), s.forecastBasis()));
            for (int i = 0; i < 5; i++) {
                vl[i].setText("V" + (i + 1) + (s.vs[i] == 1 ? " ВІДКР." : s.vs[i] == 2 ? " ЗБІЙ" : " закр.")); vl[i].getStyleClass().removeAll("st-ok", "st-err", "st-idle"); vl[i].getStyleClass().add(s.vs[i] == 1 ? "st-ok" : s.vs[i] == 2 ? "st-err" : "st-idle");
                sl[i][0].setText("S" + (i + 1) + " · " + Data.SPECIES[s.species[i]].split(" ")[0]); mb[i].setProgress(s.moist[i] / 100.0);
                sl[i][1].setText((int) s.moist[i] + " % · " + String.format("%.1f", s.slotTemp(i)) + " °C"); boolean bad = s.bad[i] != 0 || s.fSensor == i || s.fComm == i;
                sl[i][2].setText(bad ? "датчик: збій" : "датчик: ОК"); sl[i][2].getStyleClass().removeAll("ok", "err"); sl[i][2].getStyleClass().add(bad ? "err" : "ok");
            }
        });
        VBox v = new VBox(6, overall, banner, wl, wbar, pw, light, pump, phase, fore, new Label("Клапани"), valves, new Label("Слоти: вологість, температура, датчик"), slots); v.getStyleClass().add("card"); v.setPadding(new Insets(10)); return v;
    }
    private StatusPanel() {}
}
