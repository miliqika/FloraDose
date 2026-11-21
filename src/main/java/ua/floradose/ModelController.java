package ua.floradose;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.*;

final class ModelController {
    private static final String[][] CALL = {{"tank", "-1", "Резервуар", "w"}, {"filter", "-1", "Фільтр", "w"}, {"pump", "-1", "Помпа", "w"}, {"manifold", "-1", "Колектор", "w"}, {"valve", "0", "V1", "w"}, {"valve", "1", "V2", "w"}, {"valve", "2", "V3", "w"},
        {"valve", "3", "V4", "w"}, {"valve", "4", "V5", "w"}, {"pcb", "-1", "Основна плата", "e"}, {"bay", "-1", "Електронний відсік IP44", "e"}, {"mcu", "-1", "MCU", "e"}, {"loadcell", "0", "Вага S1", "s"}, {"loadcell", "1", "Вага S2", "s"},
        {"loadcell", "2", "Вага S3", "s"}, {"loadcell", "3", "Вага S4", "s"}, {"loadcell", "4", "Вага S5", "s"}, {"level", "-1", "Рівнемір", "s"}};
    private final Ctx c; private final Scene3D sc; final Set<String> groupHidden = new HashSet<>(); private final Map<String, ToggleButton> groupBtns = new LinkedHashMap<>();
    private Scene3D.Mode override; private double exA; private Slider exSlider; private final ToggleGroup modeGroup = new ToggleGroup(); private final Label hover = new Label(), hud = new Label();
    private final Pane callLayer = new Pane(); private final List<Object[]> callouts = new ArrayList<>(); private HBox legend; private Section section = Section.MODEL;
    private String savedView; private Set<String> savedGroups = new HashSet<>(), savedHidden = new HashSet<>(); private double savedEx;
    private ChoiceBox<String> bgBox; private ToggleButton decorBtn; private final Runnable onDemo; private ChoiceBox<String> viewBox;

    ModelController(Ctx c, Runnable onDemo) { this.c = c; this.sc = c.scene(); this.onDemo = onDemo; }
    Scene3D.Mode effMode() { return override != null ? override : section.mode; }
    double exA() { return exA; }
    void setHover(String t) { hover.setText(t); }
    private boolean compact;
    void setCompact(boolean on) { compact = on; if (legend != null) { legend.setManaged(!on); } }
    void setHud(String t) { hud.setText(t); }

    private static Button tip(Button b, String t) { b.setTooltip(new Tooltip(t)); return b; }
    private Node leg(String color, String text) { Line l = new Line(0, 0, 22, 0); l.setStroke(Color.web(color)); l.setStrokeWidth(4); HBox h = new HBox(6, l, new Label(text)); h.setAlignment(Pos.CENTER_LEFT); return h; }

    Node build() {
        hover.getStyleClass().add("hover-label"); hover.setMouseTransparent(true); hover.visibleProperty().bind(hover.textProperty().isNotEmpty()); hover.managedProperty().bind(hover.visibleProperty()); hud.getStyleClass().add("hud"); hud.setMouseTransparent(true); hud.visibleProperty().bind(hud.textProperty().isNotEmpty());
        HBox modeSeg = new HBox(); modeSeg.getStyleClass().add("seg");
        Object[][] modes = {{"Корпус", Scene3D.Mode.MODEL}, {"Все", Scene3D.Mode.INNER}, {"Вода", Scene3D.Mode.WATER}, {"Плати", Scene3D.Mode.EL}, {"Датчики", Scene3D.Mode.SENS}};
        for (Object[] m : modes) { ToggleButton b = new ToggleButton((String) m[0]); b.setToggleGroup(modeGroup); b.getStyleClass().add("seg-btn"); b.setUserData(m[1]); b.setMinWidth(Region.USE_PREF_SIZE); modeSeg.getChildren().add(b); }
        modeGroup.selectedToggleProperty().addListener((o, a, b) -> { if (b == null) { if (a != null) a.setSelected(true); } else { override = (Scene3D.Mode) b.getUserData(); c.repaint(); } });
        HBox groups = new HBox(6); groups.getStyleClass().add("overlay-bar"); groups.setAlignment(Pos.CENTER_RIGHT); groups.getChildren().add(new Label("Показ"));
        for (String[] g : new String[][]{{"shell", "Оболонка"}, {"water", "Вода"}, {"el", "Плати"}, {"plants", "Рослини"}}) {
            ToggleButton t = new ToggleButton(g[1]); t.setSelected(true); t.getStyleClass().add("chip-toggle"); t.setMinWidth(Region.USE_PREF_SIZE); t.setOnAction(e -> { if (t.isSelected()) groupHidden.remove(g[0]); else groupHidden.add(g[0]); c.repaint(); }); groupBtns.put(g[0], t); groups.getChildren().add(t);
        }
        FlowPane topRow = new FlowPane(8, 6, modeSeg, groups);
        HBox hudBox = new HBox(hud); hudBox.setAlignment(Pos.TOP_CENTER); hudBox.setMouseTransparent(true); hudBox.setPickOnBounds(false);
        for (String[] cl : CALL) { Label l = new Label(cl[2]); l.getStyleClass().add("callout"); l.setVisible(false); l.setMouseTransparent(true); callLayer.getChildren().add(l); callouts.add(new Object[]{cl[0], Integer.parseInt(cl[1]), l, cl[3]}); }
        callLayer.setMouseTransparent(true); callLayer.setPickOnBounds(false);
        legend = new HBox(12, leg("#6fc3f5", "блакитні трубки — вода"), leg("#e8821e", "оранжеві дроти — 12 В"), leg("#33363b", "графітові — сигнали"), leg("#1f6b3a", "зелені — плати")); legend.getStyleClass().add("legend"); legend.setVisible(false);
        viewBox = new ChoiceBox<>(); viewBox.getItems().addAll("Загальний", "Спереду", "Зверху", "Ззаду", "Справа"); viewBox.setValue("Загальний"); viewBox.setTooltip(new Tooltip("Ракурс"));
        String[] vk = {"iso", "front", "top", "back", "right"}; viewBox.getSelectionModel().selectedIndexProperty().addListener((o, x, y) -> sc.setView(vk[y.intValue()]));
        exSlider = new Slider(0, 1, 0); exSlider.setPrefWidth(100); exSlider.setTooltip(new Tooltip("Розбирання моделі")); exSlider.valueProperty().addListener((o, x, y) -> { exA = y.doubleValue(); c.repaint(); });
        bgBox = new ChoiceBox<>(); bgBox.getItems().addAll("Студійний", "Домашній", "Нейтральний"); bgBox.setValue("Домашній"); bgBox.setTooltip(new Tooltip("Фон сцени")); bgBox.getSelectionModel().selectedIndexProperty().addListener((o, x, y) -> sc.setBackground(y.intValue()));
        decorBtn = new ToggleButton("Декор"); decorBtn.setSelected(true); decorBtn.setTooltip(new Tooltip("Показати/приховати стіл-декор: вазу, тканину")); decorBtn.setOnAction(e -> sc.setDecor(decorBtn.isSelected()));
        FlowPane bar = new FlowPane(6, 6, viewBox, new Separator(Orientation.VERTICAL), tip(Ui.button("", "explode", () -> animateEx(1)), "Розібрати модель"), tip(Ui.button("", "assemble", () -> animateEx(0)), "Зібрати модель"), exSlider, new Separator(Orientation.VERTICAL),
            tip(Ui.button("", "fit", () -> { sc.autoFit = true; sc.fit(); }), "Кадрувати модель: центрувати й масштабувати"), tip(Ui.button("", "save", this::saveView), "Зберегти ракурс і видимість"), tip(Ui.button("↺", null, this::restoreView), "Відновити збережений вигляд"),
            new Separator(Orientation.VERTICAL), bgBox, decorBtn, tip(Ui.button("", "demo", onDemo), "Повноекранна демонстрація моделі (Esc — вихід)"));
        bar.getStyleClass().add("overlay-bar"); bar.setAlignment(Pos.CENTER_LEFT); bar.setColumnHalignment(javafx.geometry.HPos.LEFT);
        BorderPane ov = new BorderPane(); ov.setPickOnBounds(false); ov.setPadding(new Insets(8)); VBox topBox = new VBox(6, topRow, hudBox, hover); topBox.setPickOnBounds(false); ov.setTop(topBox);
        VBox bottom = new VBox(6, legend, bar); bottom.setPickOnBounds(false); bottom.setAlignment(Pos.BOTTOM_LEFT); ov.setBottom(bottom);
        StackPane left = new StackPane(sc.sub, callLayer, ov); left.setMinSize(420, 0); bar.prefWrapLengthProperty().bind(left.widthProperty().subtract(36)); topRow.prefWrapLengthProperty().bind(left.widthProperty().subtract(36));
        sc.sub.widthProperty().bind(left.widthProperty()); sc.sub.heightProperty().bind(left.heightProperty());
        Rectangle clip = new Rectangle(); clip.widthProperty().bind(left.widthProperty()); clip.heightProperty().bind(left.heightProperty()); left.setClip(clip);
        return left;
    }
    void sectionChanged(Section s) {
        section = s; override = null; for (Toggle t : modeGroup.getToggles()) if (t.getUserData() == s.mode) { modeGroup.selectToggle(t); override = null; }
    }
    void showInternal() { if (effMode() == Scene3D.Mode.MODEL) for (Toggle t : modeGroup.getToggles()) if (t.getUserData() == Scene3D.Mode.INNER) modeGroup.selectToggle(t); }
    private void animateEx(double to) { new Timeline(new KeyFrame(Duration.seconds(2.6), new KeyValue(exSlider.valueProperty(), to, Interpolator.EASE_BOTH))).play(); }

    void updateCallouts() {
        Scene3D.Mode m = effMode(); boolean on = m != Scene3D.Mode.MODEL && exA < 0.5; legend.setVisible(m != Scene3D.Mode.MODEL && !compact);
        List<double[]> placed = new ArrayList<>();
        for (Object[] cl : callouts) {
            Label l = (Label) cl[2]; String kind = (String) cl[3]; boolean show = on && (m == Scene3D.Mode.INNER ? !kind.equals("s") : m == Scene3D.Mode.WATER ? kind.equals("w") : m == Scene3D.Mode.EL ? kind.equals("e") : kind.equals("s"));
            Scene3D.Part p = show ? sc.find((String) cl[0], (Integer) cl[1]) : null; if (p != null && !p.n.isVisible()) p = null;
            Point2D q = p == null ? null : sc.project(p); l.setVisible(q != null); if (q == null) continue;
            double x = q.getX() + 6, y = q.getY() - 26, w = Math.max(30, l.getText().length() * 6.5 + 14);
            for (int it = 0; it < 10; it++) { boolean hit = false; for (double[] r : placed) if (x < r[0] + r[2] && x + w > r[0] && y < r[1] + 18 && y + 18 > r[1]) { y = r[1] + 19; hit = true; } if (!hit) break; }
            placed.add(new double[]{x, y, w}); l.relocate(x, y);
        }
    }
    void saveView() { savedView = sc.viewState(); savedGroups = new HashSet<>(groupHidden); savedHidden = new HashSet<>(c.hidden()); savedEx = exA; }
    void restoreView() { if (savedView == null) return; apply(savedView, savedGroups, savedHidden, savedEx); }
    void apply(String view, Set<String> groups, Set<String> hid, double ex) {
        if (view != null && !view.isEmpty()) sc.restoreView(view);
        groupHidden.clear(); groupHidden.addAll(groups); c.hidden().clear(); c.hidden().addAll(hid);
        for (Map.Entry<String, ToggleButton> e : groupBtns.entrySet()) e.getValue().setSelected(!groupHidden.contains(e.getKey()));
        exSlider.setValue(ex); c.repaint();
    }
    void fillState(Properties p) {
        p.setProperty("view", sc.viewState()); p.setProperty("groups", String.join(",", groupHidden)); p.setProperty("hidden", String.join(",", c.hidden())); p.setProperty("bg", String.valueOf(bgBox.getSelectionModel().getSelectedIndex())); p.setProperty("decor", String.valueOf(decorBtn.isSelected()));
    }
    void applyState(Properties p) {
        Set<String> g = new HashSet<>(), h = new HashSet<>(); for (String x : p.getProperty("groups", "").split(",")) if (!x.isEmpty()) g.add(x); for (String x : p.getProperty("hidden", "").split(",")) if (!x.isEmpty()) h.add(x);
        apply(p.getProperty("view", ""), g, h, 0); try { bgBox.getSelectionModel().select(Integer.parseInt(p.getProperty("bg", "1"))); } catch (Exception ignored) { } decorBtn.setSelected(Boolean.parseBoolean(p.getProperty("decor", "true"))); sc.setDecor(decorBtn.isSelected());
    }
}
