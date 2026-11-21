package ua.floradose;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;

final class SchemePanel {
    private final Ctx c; private final Canvas cv = new Canvas(Schematic.CW, Schematic.CH); private String filter = "all"; private final Slider zoom = new Slider(0.5, 2.2, 1.0);
    SchemePanel(Ctx c) { this.c = c; }
    private ScrollPane sp; private boolean userZoom;
    private void fitZoom() { if (sp == null || sp.getWidth() < 100) return; userZoom = false; zoom.setValue(Math.max(0.5, Math.min(1.4, (sp.getWidth() - 24) / Schematic.CW))); }
    void redraw() { Schematic.draw(cv.getGraphicsContext2D(), c.selId(), filter); }
    Node build() {
        ToggleGroup g = new ToggleGroup(); HBox f = new HBox(6);
        for (String[] x : new String[][]{{"all", "Усі"}, {"w", "Вода"}, {"p", "Живлення"}, {"s", "Сигнали"}}) { ToggleButton b = new ToggleButton(x[1]); b.setToggleGroup(g); b.setSelected(x[0].equals("all")); b.setOnAction(e -> { filter = x[0]; redraw(); }); f.getChildren().add(b); }
        zoom.setPrefWidth(120); zoom.valueProperty().addListener((o, a, b) -> { cv.setScaleX(b.doubleValue()); cv.setScaleY(b.doubleValue()); });
        HBox zr = new HBox(8, new Label("Масштаб"), zoom, Ui.button("Вмістити", "fit", () -> fitZoom()));
        cv.setOnMouseClicked(e -> { String id = Schematic.hit(e.getX(), e.getY()); if (id != null) c.select(id, -1); });
        cv.setOnMouseMoved(e -> cv.setCursor(Schematic.hit(e.getX(), e.getY()) != null ? Cursor.HAND : Cursor.DEFAULT));
        Group gr = new Group(cv); ScrollPane sp = new ScrollPane(gr); sp.setPannable(true); sp.getStyleClass().add("scheme-scroll");
        sp.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> { if (e.isControlDown()) { zoom.setValue(Math.max(0.5, Math.min(2.2, zoom.getValue() + e.getDeltaY() / 400.0))); e.consume(); } });
        VBox v = new VBox(8, Ui.hint("Клік по вузлу виділяє компонент у моделі, списку й картці; підсвічуються його вхідні й вихідні зв’язки. Ctrl+колесо — масштаб, перетягування — переміщення."), f, zr, sp);
        v.setPadding(new Insets(6)); VBox.setVgrow(sp, Priority.ALWAYS); this.sp = sp; sp.widthProperty().addListener((o, a, b) -> { if (!userZoom) fitZoom(); });
        zoom.setOnMousePressed(e -> userZoom = true); redraw(); return v;
    }
}
