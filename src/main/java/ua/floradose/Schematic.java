package ua.floradose;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import java.util.*;

final class Schematic {
    static final double W = 108, H = 44, GX = 132, GY = 98, OX = 18, OY = 18, CW = 940, CH = 430;
    static final Map<String, int[]> N = new LinkedHashMap<>();
    static final String[][] E = {
        {"tank", "filter", "w", ""}, {"filter", "pump", "w", ""}, {"pump", "manifold", "w", ""}, {"manifold", "valve", "w", "5 ліній"}, {"valve", "slot", "w", "трубки"},
        {"level", "mux", "s", "I²C"}, {"electrode", "mux", "s", "I²C"}, {"mux", "mcu", "s", "I²C"}, {"loadcell", "slotboard", "s", "HX711"}, {"light", "slotboard", "s", "I²C"},
        {"mic", "slotboard", "s", "PDM"}, {"ntc", "slotboard", "s", "АЦП"}, {"slotboard", "mcu", "s", "шина"}, {"mcu", "drv", "s", "GPIO"}, {"drv", "valve", "s", "12 В ключі"},
        {"drv", "pump", "s", "12 В ключ"}, {"mcu", "panel", "s", "SPI / дотик"}, {"mcu", "led", "s", "ШІМ"},
        {"psu", "drv", "p", "12 В"}, {"psu", "led", "p", "12 В"}, {"psu", "mcu", "p", "3,3 В"}, {"psu", "panel", "p", "5 В"}, {"batt", "psu", "p", "резерв"}};
    static {
        Object[][] g = {{"tank", 0, 0}, {"filter", 1, 0}, {"pump", 2, 0}, {"manifold", 3, 0}, {"valve", 4, 0}, {"slot", 5, 0}, {"panel", 0, 1}, {"mcu", 1, 1}, {"drv", 2, 1}, {"led", 3, 1}, {"psu", 4, 1}, {"batt", 5, 1},
            {"level", 0, 2}, {"electrode", 2, 2}, {"loadcell", 3, 2}, {"light", 4, 2}, {"mic", 5, 2}, {"ntc", 6, 2}, {"mux", 1, 3}, {"slotboard", 4, 3}};
        for (Object[] o : g) N.put((String) o[0], new int[]{(Integer) o[1], (Integer) o[2]});
    }
    static Color col(String t) { return t.equals("w") ? Color.web("#2b7de9") : t.equals("p") ? Color.web("#d9822b") : Color.web("#2a9d6f"); }
    private static double nx(String id) { return OX + N.get(id)[0] * GX; }
    private static double ny(String id) { return OY + N.get(id)[1] * GY; }

    static List<double[]> route(String a, String b, int lane) {
        int ca = N.get(a)[0], ra = N.get(a)[1], cb = N.get(b)[0], rb = N.get(b)[1]; List<double[]> p = new ArrayList<>();
        double ax = nx(a) + W / 2, ay = ny(a) + H / 2, bx = nx(b) + W / 2, by = ny(b) + H / 2, gap = (GY - H) / 2;
        if (ra == rb && Math.abs(ca - cb) == 1) { double s = cb > ca ? 1 : -1; p.add(new double[]{ax + s * W / 2, ay}); p.add(new double[]{bx - s * W / 2, by}); return p; }
        if (ca == cb) { double s = rb > ra ? 1 : -1; p.add(new double[]{ax, ay + s * H / 2}); p.add(new double[]{bx, by - s * H / 2}); return p; }
        double off = (lane % 4 - 1.5) * 8;
        if (ra == rb) { double chy = ny(a) + H + gap + lane * 5; p.add(new double[]{ax + off, ay + H / 2}); p.add(new double[]{ax + off, chy}); p.add(new double[]{bx + off, chy}); p.add(new double[]{bx + off, by + H / 2}); return p; }
        double dir = rb > ra ? 1 : -1, chy = dir > 0 ? ny(a) + H + gap + lane * 5 : ny(a) - gap + lane * 5;
        p.add(new double[]{ax + off, ay + dir * H / 2}); p.add(new double[]{ax + off, chy}); p.add(new double[]{bx + off, chy}); p.add(new double[]{bx + off, by - dir * H / 2});
        return p;
    }
    static boolean touches(String[] e, String id) { return id != null && (e[0].equals(id) || e[1].equals(id)); }

    static void draw(GraphicsContext g, String sel, String filter) {
        g.setFill(Color.web("#FAF8F4")); g.fillRect(0, 0, CW, CH);
        g.setFill(Color.web("#E4DDD0"));
        for (double x = 8; x < CW; x += 22) for (double y = 8; y < CH; y += 22) g.fillOval(x, y, 1.6, 1.6);
        Set<String> act = new HashSet<>(); Map<String, Integer> lanes = new HashMap<>(); boolean hasSel = sel != null && N.containsKey(sel);
        g.setFont(Font.font(10)); g.setTextAlign(TextAlignment.CENTER);
        for (String[] e : E) {
            if (!filter.equals("all") && !e[2].equals(filter)) continue;
            act.add(e[0]); act.add(e[1]);
            String key = N.get(e[0])[1] + ":" + (N.get(e[1])[1] >= N.get(e[0])[1]); int lane = lanes.merge(key, 1, Integer::sum) - 1;
            List<double[]> r = route(e[0], e[1], lane); boolean hot = touches(e, sel); double alpha = !hasSel || hot ? 1 : 0.28;
            Color c = col(e[2]); g.setStroke(Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha)); g.setFill(g.getStroke()); g.setLineWidth(hot ? 3.4 : 2);
            for (int i = 0; i < r.size() - 1; i++) g.strokeLine(r.get(i)[0], r.get(i)[1], r.get(i + 1)[0], r.get(i + 1)[1]);
            double[] q = r.get(r.size() - 1), q0 = r.get(r.size() - 2); double an = Math.atan2(q[1] - q0[1], q[0] - q0[0]), ah = hot ? 12 : 9;
            g.fillPolygon(new double[]{q[0], q[0] - ah * Math.cos(an - 0.45), q[0] - ah * Math.cos(an + 0.45)}, new double[]{q[1], q[1] - ah * Math.sin(an - 0.45), q[1] - ah * Math.sin(an + 0.45)}, 3);
            if (!e[3].isEmpty()) { int li = 0; double best = 0; for (int i = 0; i < r.size() - 1; i++) { double d = Math.hypot(r.get(i + 1)[0] - r.get(i)[0], r.get(i + 1)[1] - r.get(i)[1]); if (d > best) { best = d; li = i; } }
                double mx = (r.get(li)[0] + r.get(li + 1)[0]) / 2, my = (r.get(li)[1] + r.get(li + 1)[1]) / 2; g.setFill(Color.web("#FAF8F4")); g.fillRect(mx - e[3].length() * 2.8, my - 8, e[3].length() * 5.6, 12);
                g.setFill(Color.color(0.2, 0.2, 0.2, alpha)); g.fillText(e[3], mx, my + 2); }
        }
        g.setFont(Font.font(11));
        for (Map.Entry<String, int[]> en : N.entrySet()) {
            String id = en.getKey(); double x = nx(id), y = ny(id); boolean on = filter.equals("all") || act.contains(id), s = id.equals(sel);
            boolean rel = false; if (hasSel) for (String[] e : E) if (touches(e, sel) && touches(e, id)) rel = true;
            g.setFill(s ? Color.web("#EBD7B3") : rel ? Color.web("#F5ECDA") : on ? Color.WHITE : Color.web("#F0EDE6"));
            g.fillRoundRect(x, y, W, H, 9, 9); g.setStroke(s ? Color.web("#8B7355") : rel ? Color.web("#B5986A") : Color.web("#B9B2A5")); g.setLineWidth(s ? 3 : rel ? 2 : 1); g.strokeRoundRect(x, y, W, H, 9, 9);
            Data.C c = Data.COMPS.get(id); String t = c.name; g.setFill(on ? Color.web("#2B2620") : Color.web("#A39B8B"));
            if (t.length() > 17) { int cut = t.lastIndexOf(' ', 17); if (cut < 6) cut = 17; g.fillText(t.substring(0, cut), x + W / 2, y + 18); String t2 = t.substring(cut).trim(); g.fillText((t2.length() > 18 ? t2.substring(0, 17) + "…" : t2) + (c.qty > 1 ? " ×" + c.qty : ""), x + W / 2, y + 32); }
            else g.fillText(t + (c.qty > 1 ? " ×" + c.qty : ""), x + W / 2, y + H / 2 + 4);
        }
        g.setTextAlign(TextAlignment.LEFT); g.setFill(Color.web("#6B6458")); g.setFont(Font.font(11));
        g.fillText("Стрілки: напрямок потоку. Синій — вода · оранжевий — живлення · зелений — сигнали й керування.", 18, CH - 10);
    }
    static String hit(double x, double y) {
        for (String id : N.keySet()) if (x >= nx(id) && x <= nx(id) + W && y >= ny(id) && y <= ny(id) + H) return id;
        return null;
    }
    private Schematic() {}
}
