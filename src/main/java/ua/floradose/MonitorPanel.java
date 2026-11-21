package ua.floradose;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;

final class MonitorPanel {
    private int lastKey = -1;
    Node build(Ctx c) {
        Sim s = c.sim();
        LineChart<Number, Number> moist = chart("Вологість ґрунту, %", 0, 100), tank = chart("Рівень води в резервуарі, %", 0, 100), temp = chart("Температура, °C", 10, 40);
        XYChart.Series<Number, Number>[] ms = series(moist, 5, "S"); XYChart.Series<Number, Number>[] ts = series(tank, 1, "Вода"); XYChart.Series<Number, Number>[] tt = series(temp, 1, "Повітря");
        CategoryAxis cx = new CategoryAxis(); NumberAxis ny = new NumberAxis(); ny.setLabel("мл"); BarChart<String, Number> used = new BarChart<>(cx, ny); used.setTitle("Використано води за слотами, мл"); used.setAnimated(false); used.setLegendVisible(false); used.setPrefHeight(190);
        XYChart.Series<String, Number> us = new XYChart.Series<>(); used.getData().add(us); for (int i = 0; i < 5; i++) us.getData().add(new XYChart.Data<>("S" + (i + 1), 0));
        TableView<Sim.Watering> wt = new TableView<>(FXCollections.observableArrayList()); wt.setPrefHeight(190);
        wt.getColumns().add(Ui.<Sim.Watering>col("Час, год", 80, w -> String.format("%.2f", w.hour))); wt.getColumns().add(Ui.<Sim.Watering>col("Слот", 60, w -> "S" + (w.slot + 1)));
        wt.getColumns().add(Ui.<Sim.Watering>col("Об’єм, мл", 90, w -> String.valueOf((int) w.ml))); wt.getColumns().add(Ui.<Sim.Watering>col("Тривалість, с", 110, w -> String.format("%.1f", w.seconds)));
        Button exp = Ui.button("Журнал поливів CSV", "csv", () -> c.export().waterLog(s));
        Label sum = new Label(); sum.setWrapText(true); sum.getStyleClass().add("next-line");
        TableView<Integer> fc = new TableView<>(FXCollections.observableArrayList(0, 1, 2, 3, 4)); fc.setPrefHeight(190); fc.setFixedCellSize(28); fc.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fc.getColumns().add(Ui.<Integer>col("Слот", 50, i -> "S" + (i + 1))); fc.getColumns().add(Ui.<Integer>col("Рослина", 150, i -> Data.PLANTS[s.species[i]].latin));
        fc.getColumns().add(Ui.<Integer>col("Ґрунт", 150, i -> Data.SOILS[s.soil[i]].name.replace("Для сукулентів", "Мінеральний").replaceAll(" (ґрунт|субстрат)$", ""))); fc.getColumns().add(Ui.<Integer>col("Вологість", 80, i -> (int) s.moist[i] + " %"));
        fc.getColumns().add(Ui.<Integer>col("Поріг", 60, i -> s.thr(i) + " %" + (s.thrOv[i] >= 0 ? " *" : ""))); fc.getColumns().add(Ui.<Integer>col("Доза", 70, i -> (int) s.dose(i) + " мл"));
        fc.getColumns().add(Ui.<Integer>col("До поливу", 110, i -> WaterPanel.hours(s.hoursToNext(i))));
        String[] fkey = {""};
        c.addSyncer(() -> {
            sum.setText("Наступний полив: " + s.nextWateringText() + "\nСередня витрата за цикл: " + (int) s.dailyUse() + " мл · використано за 24 год: " + (int) s.usedToday() + " мл · запас води: " + (s.daysLeft() >= 99 ? "> 99" : String.format("%.1f", s.daysLeft())) + " діб (" + s.forecastBasis() + ")");
            StringBuilder k = new StringBuilder(); for (int i = 0; i < 5; i++) k.append((int) s.moist[i]).append(',').append(s.thr(i)).append(',').append(s.species[i]).append(',').append(s.soil[i]).append(',').append((int) s.dose(i)).append(';');
            if (!k.toString().equals(fkey[0])) { fkey[0] = k.toString(); fc.refresh(); }
        });
        c.addSyncer(() -> {
            int key = s.history.size() * 31 + s.waterings.size(); if (key == lastKey) return; lastKey = key;
            List<Sim.Sample> h = s.history; int stride = Math.max(1, h.size() / 160);
            for (int j = 0; j < 5; j++) { ms[j].getData().clear(); } ts[0].getData().clear(); tt[0].getData().clear();
            for (int k = 0; k < h.size(); k += stride) { Sim.Sample p = h.get(k); for (int j = 0; j < 5; j++) ms[j].getData().add(new XYChart.Data<>(p.hour, p.moist[j])); ts[0].getData().add(new XYChart.Data<>(p.hour, p.tank)); tt[0].getData().add(new XYChart.Data<>(p.hour, p.temp)); }
            for (int i = 0; i < 5; i++) us.getData().get(i).setYValue(s.waterUsed[i]);
            wt.getItems().setAll(s.waterings);
        });
        VBox v = new VBox(10, Ui.hint("Графіки будуються за модельним часом (швидкість — у «Системі поливу»). Дані демонстраційні, не є вимірюваннями реального пристрою."), Ui.title("Прогноз і витрата"), sum, fc, Ui.hint("* — індивідуальний поріг слота."), moist, tank, temp, used, Ui.title("Журнал поливів (час, слот, об’єм, тривалість)"), wt, exp);
        v.setPadding(new Insets(6)); ScrollPane sp = new ScrollPane(v); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); sp.getStyleClass().add("side-pane"); return sp;
    }
    private LineChart<Number, Number> chart(String title, double lo, double hi) {
        NumberAxis x = new NumberAxis(); x.setLabel("Модельний час, год"); x.setForceZeroInRange(false); NumberAxis y = new NumberAxis(lo, hi, (hi - lo) / 5); LineChart<Number, Number> ch = new LineChart<>(x, y);
        ch.setTitle(title); ch.setCreateSymbols(false); ch.setAnimated(false); ch.setPrefHeight(200); return ch;
    }
    @SuppressWarnings("unchecked")
    private XYChart.Series<Number, Number>[] series(LineChart<Number, Number> ch, int n, String name) {
        XYChart.Series<Number, Number>[] a = new XYChart.Series[n];
        for (int i = 0; i < n; i++) { a[i] = new XYChart.Series<>(); a[i].setName(n > 1 ? name + (i + 1) : name); ch.getData().add(a[i]); }
        return a;
    }
}
