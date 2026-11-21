package ua.floradose;

import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

final class Icons {
    private static final java.util.Map<String, String> P = new java.util.HashMap<>();
    static {
        P.put("model", "M12 3 L20 7.5 V16.5 L12 21 L4 16.5 V7.5 Z M4 7.5 L12 12 L20 7.5 M12 12 V21");
        P.put("inner", "M12 3 L21 8 L12 13 L3 8 Z M3 12 L12 17 L21 12 M3 16 L12 21 L21 16");
        P.put("el", "M7 7 H17 V17 H7 Z M10 3 V7 M14 3 V7 M10 17 V21 M14 17 V21 M3 10 H7 M3 14 H7 M17 10 H21 M17 14 H21");
        P.put("scheme", "M4 4 H10 V10 H4 Z M14 14 H20 V20 H14 Z M10 7 H17 V14");
        P.put("comp", "M8 6 H20 M8 12 H20 M8 18 H20 M4 6 H5 M4 12 H5 M4 18 H5");
        P.put("water", "M12 3 C12 3 5 11 5 15 A7 7 0 0 0 19 15 C19 11 12 3 12 3 Z");
        P.put("sim", "M12 3 A9 9 0 1 0 12.01 3 Z M10 8.5 L16 12 L10 15.5 Z");
        P.put("info", "M12 3 A9 9 0 1 0 12.01 3 Z M12 11 V17 M12 7.5 V7.6");
        P.put("config", "M4 7 H20 M4 12 H20 M4 17 H20 M9 5 V9 M15 10 V14 M8 15 V19");
        P.put("monitor", "M4 19 V5 M4 19 H20 M7 15 L11 11 L14 14 L19 8");
        P.put("demo", "M4 9 V4 H9 M15 4 H20 V9 M20 15 V20 H15 M9 20 H4 V15 M9 9 H15 V15 H9 Z");
        P.put("close", "M6 6 L18 18 M18 6 L6 18");
        P.put("minimize", "M5 18 H19");
        P.put("menu", "M4 7 H20 M4 12 H20 M4 17 H20");
        P.put("fit", "M4 9 V4 H9 M15 4 H20 V9 M20 15 V20 H15 M9 20 H4 V15");
        P.put("explode", "M12 9 V3 M9 5 L12 3 L15 5 M12 15 V21 M9 19 L12 21 L15 19 M9 12 H3 M5 9 L3 12 L5 15 M15 12 H21 M19 9 L21 12 L19 15");
        P.put("assemble", "M12 3 V9 M9 7 L12 9 L15 7 M12 21 V15 M9 17 L12 15 L15 17 M3 12 H9 M7 9 L9 12 L7 15 M21 12 H15 M17 9 L15 12 L17 15");
        P.put("stop", "M6 6 H18 V18 H6 Z");
        P.put("pause", "M8 5 V19 M16 5 V19");
        P.put("step", "M6 5 L14 12 L6 19 Z M18 5 V19");
        P.put("restart", "M4 12 A8 8 0 1 0 7 6 M4 4 V9 H9");
        P.put("hand", "M9 11 V5 A1.5 1.5 0 0 1 12 5 V10 M12 10 V4 A1.5 1.5 0 0 1 15 4 V11 M15 11 V6 A1.5 1.5 0 0 1 18 6 V14 A6 6 0 0 1 12 20 H11 A6 6 0 0 1 7 17 L5 13 A1.5 1.5 0 0 1 8 12 L9 14");
        P.put("bolt", "M13 3 L5 14 H11 L10 21 L19 9 H13 Z");
        P.put("save", "M5 4 H16 L19 7 V20 H5 Z M8 4 V9 H15 V4 M8 20 V14 H16 V20");
        P.put("tank", "M7 4 H17 V20 H7 Z M7 12 H17 M10 16 H14");
        P.put("sun", "M12 8 A4 4 0 1 0 12.01 8 Z M12 2 V4 M12 20 V22 M2 12 H4 M20 12 H22 M5 5 L6.5 6.5 M17.5 17.5 L19 19 M5 19 L6.5 17.5 M17.5 6.5 L19 5");
        P.put("user", "M12 4 A4 4 0 1 0 12.01 4 Z M4 20 C4 15 8 13 12 13 C16 13 20 15 20 20");
        P.put("csv", "M12 3 V15 M7 11 L12 16 L17 11 M5 20 H19");
    }
    static Node of(String name, double size) {
        SVGPath p = new SVGPath(); p.setContent(P.getOrDefault(name, P.get("info"))); p.getStyleClass().add("icon");
        p.setFill(null); p.setStrokeLineCap(StrokeLineCap.ROUND); p.setStrokeLineJoin(StrokeLineJoin.ROUND); p.setStrokeWidth(1.8);
        double sc = size / 24.0; p.setScaleX(sc); p.setScaleY(sc);
        javafx.scene.layout.Pane box = new javafx.scene.layout.Pane(p); box.setMinSize(size, size); box.setPrefSize(size, size); box.setMaxSize(size, size);
        p.setTranslateX(-(24 - size) / 2); p.setTranslateY(-(24 - size) / 2);
        return box;
    }
    private Icons() {}
}
