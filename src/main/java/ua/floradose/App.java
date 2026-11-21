package ua.floradose;

import javafx.application.Application;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;
import java.util.prefs.Preferences;

public class App extends Application implements Ctx {
    private final Scene3D sc = new Scene3D();
    private Sim sim; private Stage stage; private ExportService exporter;
    private InfoPanel info; private WaterPanel water; private SimPanel simP; private SpecPanel spec; private SchemePanel scheme;
    private ConfigurationManager cfgMgr; private NavigationController navCtl; private ModelController model; private ComponentController comp; private SimulationController simCtl;
    private Section section = Section.MODEL; private final Set<String> hidden = new HashSet<>(); private final List<Runnable> syncers = new ArrayList<>();
    private boolean sync, demo, autoCollapsed, autoHidden; private ToggleButton panelBtn; private Label cfgLabel; private Button fullBtn; private final Map<Section, Node> panes = new EnumMap<>(Section.class); private final Map<Section, Double> divs = new EnumMap<>(Section.class);
    private final StackPane content = new StackPane(); private final Label secTitle = new Label(), exploreHint = new Label(); private Button bayBtn, pauseBtn;
    private SplitPane split; private VBox right; private Node nav, header; private BorderPane root; private String dispText = "Готовий";
    private static final Preferences PREFS = Preferences.userNodeForPackage(App.class);
    private StateStore store; private Node dash, cfgNode; private ToggleButton userBtn, engBtn; private Timeline saveTl;
    private boolean userMode, wantUser = true, modeReady, saveWarned; private Section prevSection = Section.MODEL;
    private String lastFp = "", idleKey = ""; private long holdUntil;

    public static void main(String[] args) { launch(args); }

    @Override public Sim sim() { return sim; } @Override public int cfg() { return cfgMgr.cfg(); } @Override public String selId() { return comp.id(); } @Override public int selIdx() { return comp.idx(); }
    @Override public void addSyncer(Runnable r) { syncers.add(r); } @Override public Stage stage() { return stage; } @Override public Set<String> hidden() { return hidden; } @Override public ExportService export() { return exporter; }
    @Override public Scene3D scene() { return sc; } @Override public boolean syncing() { return sync; } @Override public boolean isPaused() { return simCtl.paused(); }
    @Override public void select(String id, int idx) { comp.select(id, idx); }
    @Override public void setSpeed(double v) { simCtl.setSpeed(v); } @Override public void setStepMode(boolean b) { simCtl.setStepMode(b); }
    @Override public void startCycle() { simCtl.start(); } @Override public void startCycleStepNow() { simCtl.startNow(); } @Override public void startSlot(int i) { simCtl.startSlot(i); } @Override public void startAll() { simCtl.startAll(); }
    @Override public void restartCycle() { simCtl.restart(); } @Override public void stopCycle(String m) { simCtl.stop(m); } @Override public void togglePause() { simCtl.togglePause(); } @Override public void stepOnce() { simCtl.stepOnce(); }
    @Override public void focusSelected() { comp.focusSelected(); } @Override public void toggleIsolate() { comp.toggleIsolate(); } @Override public boolean isolated() { return comp.isolated(); }
    @Override public boolean confirm(String text) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL); a.initOwner(stage); a.setHeaderText(null); a.setTitle("Підтвердження");
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    @Override
    public void start(Stage st) {
        stage = st; sc.build(); exporter = new ExportService(st);
        sc.onPick = (id, i) -> comp.select(id, i);
        sc.onHover = (id, i) -> model.setHover(id == null ? "" : Data.COMPS.get(id).name + (i >= 0 && Data.COMPS.get(id).qty > 1 ? " · №" + (i + 1) : "") + " — " + Data.COMPS.get(id).place);
        simP = new SimPanel(this);
        sim = new Sim(new Sim.Hook() {
            public void event(Sim.Ev e) { simP.events.add(e); }
            public void disp(String a, String b, Color col) { sc.showScreen(a, b, col); dispText = a + (b.isEmpty() ? "" : " · " + b); holdUntil = System.nanoTime() + 5_000_000_000L; idleKey = ""; }
        });
        store = new StateStore(StateStore.defaultPath()); loadStored();
        sc.onPanelTap = k -> { if (k == 0) { if (sim.running()) sim.ev("Система", 0, 1, "Екран: цикл уже виконується"); else simCtl.start(); } else sim.cycleLedMode(); repaint(); };
        cfgMgr = new ConfigurationManager(i -> { onConfigChanged(); });
        info = new InfoPanel(this); water = new WaterPanel(this); spec = new SpecPanel(this); scheme = new SchemePanel(this);
        model = new ModelController(this, this::toggleDemo); comp = new ComponentController(this, info, spec, water, model);
        simCtl = new SimulationController(this, sim, () -> simP.setPaused(simCtl.paused()), () -> model.updateCallouts());
        navCtl = new NavigationController(this::showSectionInternal);

        Label logo = new Label("FloraDose"); logo.getStyleClass().add("logo"); secTitle.getStyleClass().add("section-title");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        panelBtn = new ToggleButton("Панель"); panelBtn.setGraphic(Icons.of("comp", 15)); panelBtn.setSelected(true); panelBtn.setTooltip(new Tooltip("Показати/приховати інформаційну панель")); panelBtn.setOnAction(e -> setRightVisible(panelBtn.isSelected()));
        cfgLabel = new Label("Конфігурація");
        Button minB = Ui.button("", "minimize", () -> stage.setIconified(true)); minB.setTooltip(new Tooltip("Згорнути вікно")); minB.getStyleClass().add("btn-win");
        fullBtn = Ui.button("", "demo", () -> stage.setFullScreen(!stage.isFullScreen())); fullBtn.setTooltip(new Tooltip("Повний екран (F11)")); fullBtn.getStyleClass().add("btn-win");
        Button closeB = Ui.button("", "close", this::requestClose); closeB.setTooltip(new Tooltip("Закрити програму (Ctrl+Q)")); closeB.getStyleClass().addAll("btn-win", "btn-close");
        userBtn = new ToggleButton("Користувач"); engBtn = new ToggleButton("Інженер"); ToggleGroup modeTg = new ToggleGroup();
        for (ToggleButton b : new ToggleButton[]{userBtn, engBtn}) { b.setToggleGroup(modeTg); b.getStyleClass().add("seg-btn"); }
        userBtn.setTooltip(new Tooltip("Простий екран: стан, вода, рослини, автополив")); engBtn.setTooltip(new Tooltip("Усі розділи: схеми, компоненти, симуляція, моніторинг"));
        modeTg.selectedToggleProperty().addListener((o, a, b) -> { if (b == null) { if (a != null) a.setSelected(true); } else if (modeReady) setUserMode(b == userBtn); });
        HBox modeSeg = new HBox(userBtn, engBtn); modeSeg.getStyleClass().add("seg");
        cfgNode = cfgMgr.node();
        HBox hd = new HBox(14, logo, secTitle, sp, modeSeg, cfgLabel, cfgNode, panelBtn, new Separator(javafx.geometry.Orientation.VERTICAL), minB, fullBtn, closeB); hd.getStyleClass().add("header"); hd.setAlignment(Pos.CENTER_LEFT); header = hd;
        nav = navCtl.build();

        exploreHint.setWrapText(true); exploreHint.getStyleClass().add("hint");
        bayBtn = Ui.button("Показати сухий відсік", "el", () -> { sc.setView("right"); comp.select("pcb", -1); }); bayBtn.managedProperty().bind(bayBtn.visibleProperty());
        VBox explore = new VBox(8, exploreHint, bayBtn, info.treeBox); VBox.setVgrow(info.treeBox, Priority.ALWAYS); explore.setPadding(new Insets(4));
        panes.put(Section.MODEL, explore); panes.put(Section.INNER, explore); panes.put(Section.EL, explore); panes.put(Section.SCHEMES, scheme.build()); panes.put(Section.COMP, spec.build()); panes.put(Section.CONFIG, new ConfigPanel().build(this));
        panes.put(Section.WATER, water.build()); panes.put(Section.SIM, simP.build()); panes.put(Section.MONITOR, new MonitorPanel().build(this)); panes.put(Section.INFO, infoPane());
        dash = new DashboardPanel().build(this); dash.setVisible(false);
        for (Node n : new LinkedHashSet<>(panes.values())) content.getChildren().add(n);
        content.getChildren().add(dash);
        right = new VBox(8, info.card, content); right.setPadding(new Insets(10)); right.getStyleClass().add("side-pane"); right.setMinWidth(340); VBox.setVgrow(content, Priority.ALWAYS);
        split = new SplitPane(model.build(), right); split.setDividerPositions(0.6);
        root = new BorderPane(split); root.setTop(header); root.setLeft(nav);
        Rectangle2D vb = Screen.getPrimary().getVisualBounds(); double sw = Math.min(1560, vb.getWidth() * 0.94), shh = Math.min(920, vb.getHeight() * 0.94);
        Scene scene = new Scene(root, Math.max(760, sw), Math.max(520, shh)); scene.getStylesheets().add(App.class.getResource("/style.css").toExternalForm());
        st.setTitle("FloraDose — інтерактивний 3D-прототип"); st.setMinWidth(720); st.setMinHeight(480); st.setScene(scene);
        st.widthProperty().addListener((o, a, b) -> adapt(b.doubleValue(), st.getHeight())); st.heightProperty().addListener((o, a, b) -> adapt(st.getWidth(), b.doubleValue()));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN), this::requestClose); scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F11), () -> st.setFullScreen(!st.isFullScreen()));
        st.fullScreenProperty().addListener((o, a, b) -> { if (!b && demo) toggleDemo(); });
        st.setOnCloseRequest(e -> { if (!confirmClose()) e.consume(); else shutdown(); });
        st.show(); loadPrefs();
        navCtl.select(section); comp.select("shell", -1); sc.fit(); syncers.add(this::updateHud); syncers.add(this::updateIdleDisplay);
        modeReady = true; (wantUser ? userBtn : engBtn).setSelected(true); if (!wantUser) setUserMode(false);
        repaint(); adapt(st.getWidth(), st.getHeight());
        simCtl.startTimer();
        saveTl = new Timeline(new KeyFrame(Duration.seconds(3), e -> autosave(false))); saveTl.setCycleCount(Timeline.INDEFINITE); saveTl.play();
    }
    private void loadStored() {
        StateStore.Result r = store.load(sim); if (!r.found) return;
        if (r.loaded) sim.ev("Система", 0, r.warnings.isEmpty() ? 0 : 1, "Стан відновлено з файлу " + store.file() + (r.warnings.isEmpty() ? "" : " (виправлень: " + r.warnings.size() + ")"));
        for (String w : r.warnings) sim.ev("Система", 0, 1, "Стан: " + w);
        if (!r.loaded) sim.ev("Система", 0, 1, "Запуск із типовими налаштуваннями");
    }
    private void autosave(boolean force) {
        if (sim == null || store == null) return;
        try {
            java.util.Map<String, Object> m = StateStore.toMap(sim, false); m.remove("modelHours"); String fp = Json.write(m);
            if (!force && fp.equals(lastFp)) return; store.save(sim); lastFp = fp; saveWarned = false;
        } catch (Exception e) { if (!saveWarned) { saveWarned = true; sim.ev("Система", 0, 1, "Не вдалося зберегти стан: " + e.getMessage()); } }
    }
    private void updateIdleDisplay() {
        if (sim.running() || sim.onBattery || System.nanoTime() < holdUntil) return;
        int ov = sim.overall(), nw = sim.needWater(); java.util.List<String> is = sim.issues(); String a, b; Color col;
        if (ov == 2) { a = "Потрібне втручання"; b = is.isEmpty() ? "" : is.get(0); col = Color.web("#ff8a8a"); }
        else if (sim.tank < 15) { a = "Мало води"; b = "Вода " + (int) sim.tank + " % · долийте"; col = Color.web("#ffd27f"); }
        else if (nw > 0) { a = "Потрібен полив"; b = nw + " з 5 слотів · вода " + (int) sim.tank + " %"; col = Color.web("#ffd27f"); }
        else { a = "FloraDose"; b = "Усе гаразд · вода " + (int) sim.tank + " %"; col = Color.web("#9ff0c0"); }
        String key = a + "|" + b; if (key.equals(idleKey)) return; idleKey = key; sc.showScreen(a, b, col); dispText = a + (b.isEmpty() ? "" : " · " + b);
    }
    private void updateHud() {
        String d = dispText; boolean hasWater = d.toLowerCase().contains("вод");
        model.setHud((hasWater ? "" : "Вода " + (int) sim.tank + " %  ·  ") + (sim.running() ? sim.phaseName + "  ·  " : "") + d + (sim.lastErr > 0 ? "   ⚠ " + Data.ERR[sim.lastErr - 1].code : ""));
    }

    private void adapt(double w, double h) {
        root.setStyle("-fx-font-size:" + (w < 1000 ? 11 : w < 1250 ? 12 : 13) + "px;");
        if (w < 1200 && !navCtl.collapsed() && !demo) { navCtl.collapse(true); autoCollapsed = true; } else if (w > 1350 && autoCollapsed) { navCtl.collapse(false); autoCollapsed = false; }
        if (w < 980 && !demo && !userMode && split.getItems().contains(right)) { setRightVisible(false); panelBtn.setSelected(false); autoHidden = true; }
        else if (w >= 1100 && autoHidden && !demo && !userMode) { setRightVisible(true); panelBtn.setSelected(true); autoHidden = false; }
        boolean st = w >= 1100 && !userMode, cl = w >= 1150 && !userMode; secTitle.setVisible(st); secTitle.setManaged(st); cfgLabel.setVisible(cl); cfgLabel.setManaged(cl);
        model.setCompact(h < 700 || w < 1000);
    }
    void requestClose() { if (confirmClose()) { shutdown(); stage.close(); Platform.exit(); } }
    private boolean confirmClose() { return !sim.running() || confirm("Цикл симуляції ще виконується. Закрити програму?"); }
    private boolean closed; private void shutdown() { if (closed) return; closed = true; saveState(PREFS); if (saveTl != null) saveTl.stop(); autosave(true); simCtl.shutdown(); }
    private void setUserMode(boolean u) {
        if (u == userMode) return;
        userMode = u; PREFS.put("ui.mode", u ? "user" : "engineer");
        if (u) prevSection = section;
        if (!demo) root.setLeft(u ? null : nav);
        for (Node n : new Node[]{cfgNode, panelBtn}) { n.setVisible(!u); n.setManaged(!u); }
        info.card.setVisible(!u); info.card.setManaged(!u);
        if (u && !split.getItems().contains(right) && !demo) { setRightVisible(true); panelBtn.setSelected(true); }
        Section target = u ? Section.MODEL : prevSection; navCtl.select(target); showSectionInternal(target);
        if (stage != null) adapt(stage.getWidth(), stage.getHeight());
    }

    private void setRightVisible(boolean v) { if (v && !split.getItems().contains(right)) { split.getItems().add(right); split.setDividerPositions(0.6); } else if (!v) split.getItems().remove(right); }
    private void toggleDemo() {
        demo = !demo; root.setTop(demo ? null : header); root.setLeft(demo || userMode ? null : nav); if (demo) split.getItems().remove(right); else if (!split.getItems().contains(right)) { split.getItems().add(right); split.setDividerPositions(0.6); }
        stage.setFullScreen(demo); sc.autoFit = true; sc.fit(); if (!demo) adapt(stage.getWidth(), stage.getHeight());
    }

    @Override public void showSection(Section s) { navCtl.select(s); }
    void showSectionInternal(Section s) {
        if (split != null && split.getDividerPositions().length > 0) divs.put(section, split.getDividerPositions()[0]);
        section = s; if (split != null && split.getItems().contains(right)) split.setDividerPositions(divs.getOrDefault(s, s == Section.SCHEMES || s == Section.COMP || s == Section.CONFIG || s == Section.MONITOR || s == Section.SIM ? 0.5 : 0.6));
        secTitle.setText("·  " + s.title); for (Node n : new LinkedHashSet<>(panes.values())) n.setVisible(false); dash.setVisible(false); (userMode ? dash : panes.get(s)).setVisible(true);
        info.card.setExpanded(!userMode && (s == Section.MODEL || s == Section.INNER || s == Section.EL));
        exploreHint.setText(s == Section.MODEL ? "Клік по деталі в моделі або по вузлу дерева виділяє її: слот — лише обраний, клапан — лише обраний." : s == Section.INNER ? "Корпус прозорий: видно раму, водний контур, плати й сухий відсік. Режими перегляду — над моделлю." : "Електроніка: основна плата, мікроконтролер, драйвери, датчики й живлення. ⚠ — проєктні припущення.");
        bayBtn.setVisible(s == Section.EL); model.sectionChanged(s); repaint();
    }
    private void onConfigChanged() { info.rebuildTree(); spec.refresh(); comp.reselect(); repaint(); }
    @Override public void setConfig(int i) { cfgMgr.set(i); onConfigChanged(); }
    @Override public void repaint() {
        if (sim == null || model == null) return;
        sc.refresh(comp.id(), comp.idx(), model.effMode(), hidden, model.groupHidden, cfgMgr.cfg(), sim.species, sim.vs, sim.alert, sim.moist, sim.tank, sim.ledState(), sim.ledColor(), model.exA());
        sc.setLedLevel(sim.ledLevel());
        sync = true; for (Runnable r : syncers) r.run(); sync = false;
        if (section == Section.SCHEMES) scheme.redraw();
    }
    private Node infoPane() {
        Button saveB = Ui.button("Зберегти стан у файл", "save", () -> { Properties p = new Properties(); saveStateTo(p); exporter.saveState(p); }), loadB = Ui.button("Завантажити стан", null, () -> { Properties p = exporter.loadState(); if (p != null) applyState(p); });
        Button repB = Ui.button("Звіт про склад пристрою (CSV)", "csv", () -> exporter.bomReport(cfgMgr.cfg()));
        Button jsonOut = Ui.button("Експорт налаштувань (JSON)", "save", this::exportJson), jsonIn = Ui.button("Імпорт налаштувань (JSON)", null, this::importJson), reset = Ui.danger("Скинути до типових", "restart", this::resetDefaults);
        jsonOut.setTooltip(new Tooltip("Рослини, ґрунт, пороги, вода, світло, автополив, режим підсвічування")); Label stateFile = Ui.hint("Файл автозбереження: " + store.file());
        VBox v = new VBox(8, Ui.title("Про програму"), Ui.hint("FloraDose — інтерактивний 3D-прототип настільного пристрою автоматичного поливу з п’ятьма слотами та спільним резервуаром. Складено за Лаб. №1–6."),
            Ui.title("Збереження"), Ui.hint("Остання конфігурація, видимість компонентів і ракурс зберігаються автоматично при закритті програми й відновлюються при запуску."), new HBox(6, saveB, loadB), repB,
            Ui.title("Налаштування симуляції (JSON)"), Ui.hint("Рослини слотів, тип ґрунту, індивідуальні пороги й дози, рівень води, освітленість, температура, автополив і режим підсвічування зберігаються між запусками автоматично (кожні кілька секунд і при закритті). Некоректні значення у файлі виправляються з попередженням у журналі, пошкоджений файл відкладається як *.corrupt-…"), stateFile, new FlowPane(6, 6, jsonOut, jsonIn, reset),
            Ui.title("Габарити"), Ui.hint("Корпус ≈ 58 × 16 × 14 см (Д × Ш × В): довжина й ширина за Лаб. №6, висота збільшена під пропорції з фото-референсу (у Лаб. №6 — 9,5 см); кутовий радіус ≈ 3 см. Об’єм резервуара (" + String.format("%.1f", Data.TANK_L) + " л) розраховано з геометрії: колонка в лівому торці + плоска основа під лотками; він у межах 3–5 л."),
            Ui.title("Візуальна симуляція й електроніка"), Ui.hint("Усе в програмі — візуальна симуляція. Електроніка апаратно не перевірялася. ⚠ — проєктне припущення: моделі плат, розташування, драйвери, роз’єми, дисплей."),
            Ui.title("Діагностика ризиків"), Ui.hint("Діагностика імітується програмними правилами з порогами вологості, освітленості й температури. Це не навчена TinyML-модель; у виробі її замінить RiskClassifier на мікроконтролері."),
            Ui.title("Допущення моделі"), Ui.hint("Витрата помпи 20 мл/с; 3 мл води піднімають вологість слота на 1 %; 1 цикл на добу для прогнозу. Коефіцієнти висихання й температури слотів — демонстраційні, не відкалібровані."));
        v.setPadding(new Insets(6)); ScrollPane s = new ScrollPane(v); s.setFitToWidth(true); s.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); s.getStyleClass().add("side-pane"); return s;
    }

    private void exportJson() { if (exporter.saveText("FloraDose_settings.json", "Налаштування FloraDose (JSON)", "*.json", StateStore.toJson(sim, true))) sim.ev("Система", 0, 0, "Налаштування експортовано у JSON"); }
    private void importJson() {
        if (sim.running()) { new Alert(Alert.AlertType.INFORMATION, "Імпорт неможливий під час виконання циклу. Зупиніть цикл і повторіть.").showAndWait(); return; }
        String text = exporter.loadText("Налаштування FloraDose (JSON)", "*.json"); if (text == null) return;
        try {
            java.util.List<String> w = StateStore.apply(Json.parse(text), sim);
            sim.ev("Система", 0, w.isEmpty() ? 0 : 1, "Налаштування імпортовано з JSON" + (w.isEmpty() ? "" : " (виправлень: " + w.size() + ")")); for (String x : w) sim.ev("Система", 0, 1, "Імпорт: " + x);
            spec.refresh(); repaint();
        } catch (IllegalArgumentException ex) { new Alert(Alert.AlertType.ERROR, "Файл не схожий на налаштування FloraDose: " + ex.getMessage() + "\nПоточні налаштування не змінено.").showAndWait(); }
    }
    private void resetDefaults() {
        if (sim.running()) { new Alert(Alert.AlertType.INFORMATION, "Скидання неможливе під час виконання циклу.").showAndWait(); return; }
        if (confirm("Скинути рослини, ґрунт, пороги, воду й лічильники до типових значень?")) { sim.resetToDefaults(); spec.refresh(); repaint(); }
    }

    private void saveStateTo(Properties p) { p.setProperty("cfg", String.valueOf(cfgMgr.cfg())); p.setProperty("section", (userMode ? prevSection : section).name()); p.setProperty("ui.mode", userMode ? "user" : "engineer"); model.fillState(p); }
    private void saveState(Preferences pr) { Properties p = new Properties(); saveStateTo(p); for (String k : p.stringPropertyNames()) pr.put(k, p.getProperty(k)); }
    private void loadPrefs() { Properties p = new Properties(); try { for (String k : PREFS.keys()) p.setProperty(k, PREFS.get(k, "")); } catch (Exception ignored) { } if (!p.isEmpty()) applyState(p); }
    private void applyState(Properties p) {
        try { int c = Integer.parseInt(p.getProperty("cfg", "0")); if (c >= 0 && c < 3) { cfgMgr.set(c); onConfigChanged(); } } catch (Exception ignored) { }
        try { Section s = Section.valueOf(p.getProperty("section", "MODEL")); if (userMode) prevSection = s; else section = s; } catch (Exception ignored) { }
        String um = p.getProperty("ui.mode"); if (um != null) { boolean u = "user".equals(um); if (modeReady) (u ? userBtn : engBtn).setSelected(true); else wantUser = u; }
        model.applyState(p); spec.refresh(); repaint();
    }
}
