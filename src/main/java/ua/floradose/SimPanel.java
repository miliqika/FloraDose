package ua.floradose;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

final class SimPanel {
    static final class SlotRow {
        final StringProperty slot = new SimpleStringProperty(), profile = new SimpleStringProperty(), plan = new SimpleStringProperty(), actual = new SimpleStringProperty(), moist = new SimpleStringProperty(), status = new SimpleStringProperty();
        int sev;
        void update(Sim s, int i) {
            slot.set("S" + (i + 1)); profile.set(Data.SPECIES[s.species[i]]); plan.set(s.plan[i] > 0 ? String.valueOf((int) s.plan[i]) : "—");
            actual.set(s.status[i] == null ? "—" : String.valueOf((int) s.actual[i])); moist.set(Double.isNaN(s.mBefore[i]) ? (int) s.moist[i] + " % (зараз)" : (int) s.mBefore[i] + " → " + (int) s.mAfter[i] + " %");
            status.set(s.status[i] == null ? "не перевірявся" : s.status[i]); sev = s.status[i] == null ? 0 : s.sev[i];
        }
    }
    final ObservableList<Sim.Ev> events = FXCollections.observableArrayList();
    private final Ctx c; private final CheckBox stepMode = new CheckBox("Покроково"); private Button pauseBtn; final TabPane tabs = new TabPane();
    SimPanel(Ctx c) { this.c = c; }

    Node build() {
        Sim s = c.sim();
        Button start = Ui.primary("Запуск", "sim", c::startCycle);
        pauseBtn = Ui.button("Пауза", "pause", c::togglePause);
        Button step = Ui.button("Крок", "step", c::stepOnce);
        Button stop = Ui.danger("Стоп", "stop", () -> { if (!s.running() || c.confirm("Зупинити поточний цикл? Клапани буде закрито.")) c.stopCycle("Зупинено користувачем"); });
        Button restart = Ui.danger("Перезапуск", "restart", () -> { if (!s.running() || c.confirm("Перервати цикл і почати заново?")) c.restartCycle(); });
        Button intr = Ui.danger("Перервати полив", "hand", () -> { if (!s.running()) return; if (c.confirm("Перервати поточний полив? Слот отримає лише частину дози.")) { s.interrupt = true; s.ev("Система", 0, 1, "Користувач перервав полив"); } });
        Button pw = Ui.danger("Втрата живлення", "bolt", () -> { if (c.confirm("Змоделювати втрату живлення? Цикл буде перервано, помпу й клапани вимкнено.")) { s.powerLoss = true; s.ev("Система", 0, 1, "Відключено мережеве живлення"); if (s.running() && !c.isPaused() && !stepMode.isSelected()) c.startCycleStepNow(); } });
        FlowPane btn = new FlowPane(6, 6, start, pauseBtn, step, stop, restart); Button rp = Ui.button("Відновити живлення", "bolt", () -> { c.sim().restorePower(); c.repaint(); }); FlowPane crit = new FlowPane(6, 6, intr, pw, rp);
        Slider sp = new Slider(0.25, 4, 1); sp.setPrefWidth(120); Label spl = new Label("×1,00"); sp.valueProperty().addListener((o, a, b) -> { c.setSpeed(b.doubleValue()); spl.setText("×" + String.format("%.2f", b.doubleValue())); });
        stepMode.setOnAction(e -> c.setStepMode(stepMode.isSelected()));
        HBox speed = new HBox(8, new Label("Швидкість"), sp, spl, stepMode); speed.setAlignment(Pos.CENTER_LEFT);
        HBox faults = new HBox(6, new Label("Збій датчика S"), Ui.faultBox(x -> s.fSensor = x), new Label("клапана V"), Ui.faultBox(x -> s.fValve = x), new Label("зв’язку"), Ui.faultBox(x -> s.fComm = x)); faults.setAlignment(Pos.CENTER_LEFT);
        CheckBox pumpFault = new CheckBox("Збій помпи (немає тиску в магістралі)"); pumpFault.setOnAction(e -> { if (!c.syncing()) { s.setPumpFault(pumpFault.isSelected()); c.repaint(); } });
        c.addSyncer(() -> pumpFault.setSelected(s.fPump));
        ChoiceBox<String> scen = new ChoiceBox<>(); scen.getItems().addAll(Sim.SCENARIOS); scen.setValue(Sim.SCENARIOS[0]);
        Button scenGo = Ui.button("Застосувати", null, () -> { int k = scen.getSelectionModel().getSelectedIndex(); if (k >= 0 && !s.running()) { s.scenario(k); c.repaint(); } });
        c.addSyncer(() -> scenGo.setDisable(s.running()));
        HBox scenRow = new HBox(6, scen, scenGo); scenRow.setAlignment(Pos.CENTER_LEFT);
        VBox control = new VBox(10, Ui.hint("Цикл за Лаб. №3: ініціалізація → збір даних → діагностика (правила-заглушка) → адаптивний полив зі зважуванням → моніторинг резервуара → збереження. Це візуальна симуляція."),
            Ui.title("Керування циклом"), btn, speed, Ui.title("Тестові збої"), faults, pumpFault, Ui.title("Готові сценарії"), Ui.hint("Швидко створюють типову ситуацію для демонстрації: спека, темрява, мало води, перезволоження. «Норма» повертає типові значення й знімає збої."), scenRow, Ui.title("Критичні ситуації"), crit, Ui.title("Стан пристрою"), StatusPanel.build(c));
        control.setPadding(new Insets(8)); ScrollPane cs = new ScrollPane(control); cs.setFitToWidth(true); cs.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); cs.getStyleClass().add("side-pane");

        ObservableList<SlotRow> rows = FXCollections.observableArrayList(); for (int i = 0; i < 5; i++) rows.add(new SlotRow());
        TableView<SlotRow> rt = new TableView<>(rows);
        rt.getColumns().add(col("Слот", 46, r -> r.slot)); rt.getColumns().add(col("Профіль", 130, r -> r.profile)); rt.getColumns().add(col("План, мл", 66, r -> r.plan));
        rt.getColumns().add(col("Факт, мл", 66, r -> r.actual)); rt.getColumns().add(col("Вологість", 110, r -> r.moist));
        TableColumn<SlotRow, String> stc = col("Статус", 240, r -> r.status);
        stc.setCellFactory(cl -> new TableCell<SlotRow, String>() { @Override protected void updateItem(String v, boolean e) { super.updateItem(v, e); setText(e ? null : v); getStyleClass().removeAll("ok", "warn", "err"); if (!e && getTableRow() != null && getTableRow().getItem() != null) { int sv = getTableRow().getItem().sev; if (sv == 2) getStyleClass().add("err"); else if (sv == 1) getStyleClass().add("warn"); } } });
        rt.getColumns().add(stc);
        c.addSyncer(() -> { for (int i = 0; i < 5; i++) rows.get(i).update(s, i); });
        VBox results = new VBox(8, Ui.hint("Планова й фактична доза, вологість до й після поливу та статус кожного слота за останній цикл."), rt); results.setPadding(new Insets(8)); VBox.setVgrow(rt, Priority.ALWAYS);

        FilteredList<Sim.Ev> fe = new FilteredList<>(events, e -> true); TableView<Sim.Ev> jt = new TableView<>(fe);
        jt.getColumns().add(Ui.<Sim.Ev>col("Час", 52, e -> Ui.time(e.t))); jt.getColumns().add(Ui.<Sim.Ev>col("Категорія", 92, e -> e.cat)); jt.getColumns().add(Ui.<Sim.Ev>col("Слот", 46, e -> e.slot > 0 ? "S" + e.slot : "—"));
        jt.getColumns().add(Ui.<Sim.Ev>col("Рівень", 70, e -> Ui.LEVEL[e.sev])); jt.getColumns().add(Ui.<Sim.Ev>col("Опис", 520, e -> e.text));
        jt.setRowFactory(t -> new TableRow<Sim.Ev>() { @Override protected void updateItem(Sim.Ev e, boolean empty) { super.updateItem(e, empty); getStyleClass().removeAll("row-warn", "row-err"); if (!empty && e != null && e.sev == 1) getStyleClass().add("row-warn"); if (!empty && e != null && e.sev == 2) getStyleClass().add("row-err"); } });
        events.addListener((ListChangeListener<Sim.Ev>) ch -> { if (!jt.getItems().isEmpty()) jt.scrollTo(jt.getItems().size() - 1); });
        ChoiceBox<String> lvl = new ChoiceBox<>(); lvl.getItems().addAll("Усі події", "Увага й помилки", "Лише помилки"); lvl.setValue("Усі події");
        lvl.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> fe.setPredicate(e -> e.sev >= b.intValue()));
        Button exp = Ui.button("CSV", "csv", () -> { List<String[]> r = new ArrayList<>(); r.add(new String[]{"Час", "Категорія", "Слот", "Рівень", "Опис"}); for (Sim.Ev e : events) r.add(new String[]{Ui.time(e.t), e.cat, e.slot > 0 ? "S" + e.slot : "", Ui.LEVEL[e.sev], e.text}); c.export().csv("FloraDose_journal.csv", r); });
        VBox journal = new VBox(8, new HBox(6, lvl, Ui.button("Очистити", null, events::clear), exp), jt); journal.setPadding(new Insets(8)); VBox.setVgrow(jt, Priority.ALWAYS);

        TableView<Data.ErrorSpec> et = new TableView<>(FXCollections.observableArrayList(Data.ERR));
        et.getColumns().add(Ui.<Data.ErrorSpec>col("Код", 40, e -> e.code)); et.getColumns().add(Ui.<Data.ErrorSpec>col("Причина", 170, e -> e.cause)); et.getColumns().add(Ui.<Data.ErrorSpec>col("Повідомлення", 140, e -> e.message));
        et.getColumns().add(Ui.<Data.ErrorSpec>col("Реакція системи", 220, e -> e.reaction)); et.getColumns().add(Ui.<Data.ErrorSpec>col("Результат", 200, e -> e.result)); et.getColumns().add(Ui.<Data.ErrorSpec>col("Що робити", 260, e -> e.action));
        c.addSyncer(() -> { if (s.lastErr > 0) et.getSelectionModel().select(s.lastErr - 1); });
        VBox ref = new VBox(8, Ui.hint("Довідник можливих помилок і реакцій. Фактичні помилки — у вкладці «Помилки», усі події — в «Журналі». Діагностика ризиків — програмні правила, а не навчена TinyML-модель."), et); ref.setPadding(new Insets(8)); VBox.setVgrow(et, Priority.ALWAYS);

        TableView<Sim.ErrRec> er = new TableView<>(FXCollections.observableArrayList());
        java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"), tf = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
        er.getColumns().add(Ui.<Sim.ErrRec>col("Дата", 82, e -> e.when.format(df))); er.getColumns().add(Ui.<Sim.ErrRec>col("Час", 66, e -> e.when.format(tf))); er.getColumns().add(Ui.<Sim.ErrRec>col("Код", 44, e -> e.code));
        er.getColumns().add(Ui.<Sim.ErrRec>col("Причина", 190, e -> e.cause)); er.getColumns().add(Ui.<Sim.ErrRec>col("Слот", 46, e -> e.slot > 0 ? "S" + e.slot : "—")); er.getColumns().add(Ui.<Sim.ErrRec>col("Рекомендована дія", 330, e -> e.action));
        c.addSyncer(() -> { if (er.getItems().size() != s.errors.size()) { er.getItems().setAll(s.errors); er.scrollTo(er.getItems().size() - 1); } });
        VBox errs = new VBox(8, Ui.hint("Журнал фактичних помилок поточного запуску: дата, час, код, причина, слот і рекомендована дія."), er, Ui.button("Журнал помилок CSV", "csv", () -> c.export().errorLog(s))); errs.setPadding(new Insets(8)); VBox.setVgrow(er, Priority.ALWAYS);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(new Tab("Керування", cs), new Tab("Результати", results), new Tab("Журнал", journal), new Tab("Помилки", errs), new Tab("Довідник", ref));
        return tabs;
    }
    void setPaused(boolean p) { if (pauseBtn != null) { pauseBtn.setText(p ? "Відновити" : "Пауза"); } }
    private static <T> TableColumn<T, String> col(String t, double w, java.util.function.Function<T, StringProperty> f) {
        TableColumn<T, String> c = new TableColumn<>(t); c.setPrefWidth(w); c.setCellValueFactory(d -> f.apply(d.getValue())); return c;
    }
}
