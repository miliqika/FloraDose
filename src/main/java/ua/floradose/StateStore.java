package ua.floradose;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StateStore {
    static final int VERSION = 1;
    static final long MAX_BYTES = 1 << 20;
    static final class Result {
        final boolean found, loaded, recovered; final List<String> warnings;
        Result(boolean found, boolean loaded, boolean recovered, List<String> warnings) { this.found = found; this.loaded = loaded; this.recovered = recovered; this.warnings = warnings; }
    }
    private final Path file;
    StateStore(Path file) { this.file = file; }
    static Path defaultPath() { return Paths.get(System.getProperty("user.home", "."), ".floradose", "state.json"); }
    Path file() { return file; }

    static Map<String, Object> toMap(Sim s, boolean stamp) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("version", VERSION); if (stamp) m.put("savedAt", LocalDateTime.now().withNano(0).toString());
        List<Object> plants = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("species", s.species[i]); p.put("speciesName", Data.PLANTS[s.species[i]].name); p.put("soil", s.soil[i]); p.put("moisture", round(s.moist[i], 1));
            p.put("threshold", s.thrOv[i] < 0 ? null : (Object) s.thrOv[i]); p.put("dose", s.doseOv[i] < 0 ? null : (Object) s.doseOv[i]); p.put("inCycle", s.sel[i]); p.put("usedMl", round(s.waterUsed[i], 1));
            plants.add(p);
        }
        m.put("plants", plants); m.put("tankPercent", round(s.tank, 2)); m.put("lux", round(s.lux, 0)); m.put("temperature", round(s.tempAmb, 1)); m.put("doseCorrection", round(s.doseCorr, 0));
        m.put("timeScale", round(s.timeScale, 0)); m.put("autoWatering", s.autoStart); m.put("drying", s.drying); m.put("ledMode", s.ledMode); m.put("modelHours", round(s.hours, 2));
        List<Object> uh = new ArrayList<>(); for (double d : s.useHist) uh.add(round(d, 1)); m.put("useHistory", uh);
        return m;
    }
    static String toJson(Sim s, boolean stamp) { return Json.write(toMap(s, stamp)); }
    private static double round(double v, int d) { double k = Math.pow(10, d); return Math.round(v * k) / k; }

    private static double num(Object o, double def, String what, List<String> w) {
        if (o instanceof Double) return (Double) o;
        if (o != null) w.add(what + ": очікувалося число"); return def;
    }
    private static int intIn(Object o, int lo, int hi, int def, String what, List<String> w) {
        if (o == null) return def;
        if (!(o instanceof Double)) { w.add(what + ": очікувалося ціле число"); return def; }
        double d = (Double) o; if (d != Math.rint(d) || d < lo || d > hi) { w.add(what + ": значення " + d + " поза " + lo + ".." + hi + ", скинуто"); return def; }
        return (int) d;
    }
    private static boolean bool(Object o, boolean def, String what, List<String> w) { if (o instanceof Boolean) return (Boolean) o; if (o != null) w.add(what + ": очікувалося true/false"); return def; }
    private static double clampW(double v, double lo, double hi, double def, String what, List<String> w) {
        if (Double.isNaN(v) || v < lo || v > hi) { w.add(what + ": значення поза " + lo + ".." + hi + ", встановлено " + def); return def; }
        return v;
    }

    static List<String> apply(Object root, Sim s) {
        Map<String, Object> m = Json.obj(root); if (m == null) throw new IllegalArgumentException("корінь має бути JSON-об’єктом");
        Object v = m.get("version"); if (!(v instanceof Double)) throw new IllegalArgumentException("відсутнє поле version");
        int ver = (int) (double) (Double) v; if (ver < 1 || ver > VERSION) throw new IllegalArgumentException("непідтримувана версія формату: " + ver);
        List<String> w = new ArrayList<>(); List<Object> pl = Json.arr(m.get("plants"));
        if (pl == null) w.add("plants: відсутній масив слотів, слоти залишено типовими");
        else if (pl.size() != 5) w.add("plants: очікувалося 5 слотів, отримано " + pl.size());
        for (int i = 0; pl != null && i < Math.min(5, pl.size()); i++) {
            Map<String, Object> p = Json.obj(pl.get(i)); String t = "слот " + (i + 1);
            if (p == null) { w.add(t + ": очікувався об’єкт"); continue; }
            s.species[i] = intIn(p.get("species"), 0, Data.PLANTS.length - 1, i, t + ".species", w);
            s.soil[i] = intIn(p.get("soil"), 0, Data.SOILS.length - 1, Data.PLANTS[s.species[i]].soil, t + ".soil", w);
            s.moist[i] = clampW(num(p.get("moisture"), Sim.DEFAULT_MOIST[i], t + ".moisture", w), 0, 100, Sim.DEFAULT_MOIST[i], t + ".moisture", w);
            s.thrOv[i] = intIn(p.get("threshold"), 5, 95, -1, t + ".threshold", w); s.doseOv[i] = intIn(p.get("dose"), 10, 200, -1, t + ".dose", w);
            s.sel[i] = bool(p.get("inCycle"), true, t + ".inCycle", w);
            s.waterUsed[i] = clampW(num(p.get("usedMl"), 0, t + ".usedMl", w), 0, 1e7, 0, t + ".usedMl", w);
        }
        s.tank = clampW(num(m.get("tankPercent"), 85, "tankPercent", w), 0, 100, 85, "tankPercent", w);
        s.lux = clampW(num(m.get("lux"), 420, "lux", w), 0, 1000, 420, "lux", w);
        s.tempAmb = clampW(num(m.get("temperature"), 22, "temperature", w), 10, 40, 22, "temperature", w);
        s.doseCorr = clampW(num(m.get("doseCorrection"), 100, "doseCorrection", w), 50, 150, 100, "doseCorrection", w);
        s.timeScale = clampW(num(m.get("timeScale"), 600, "timeScale", w), 60, 3600, 600, "timeScale", w);
        s.autoStart = bool(m.get("autoWatering"), false, "autoWatering", w); s.drying = bool(m.get("drying"), false, "drying", w);
        s.ledMode = intIn(m.get("ledMode"), 0, 2, 0, "ledMode", w);
        s.hours = clampW(num(m.get("modelHours"), 0, "modelHours", w), 0, 1e7, 0, "modelHours", w);
        s.useHist.clear(); List<Object> uh = Json.arr(m.get("useHistory"));
        if (uh != null) for (Object o : uh) { if (s.useHist.size() >= 7) break; if (o instanceof Double && (Double) o >= 0 && (Double) o <= 1e5) s.useHist.add((Double) o); else w.add("useHistory: пропущено некоректний запис"); }
        w.addAll(s.sanitize());
        return w;
    }

    void save(Sim s) throws IOException { write(file, toJson(s, true)); }
    static void write(Path target, String text) throws IOException {
        Path dir = target.toAbsolutePath().getParent(); if (dir != null) Files.createDirectories(dir);
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, text.getBytes(StandardCharsets.UTF_8));
        try { Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING); }
    }
    Result load(Sim s) { return read(file, s, true); }
    static Result read(Path f, Sim s, boolean quarantine) {
        List<String> w = new ArrayList<>();
        if (!Files.exists(f)) return new Result(false, false, false, w);
        try {
            if (Files.size(f) > MAX_BYTES) throw new IllegalArgumentException("файл завеликий (> " + MAX_BYTES / 1024 + " КБ)");
            String text = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
            w.addAll(apply(Json.parse(text), s)); return new Result(true, true, false, w);
        } catch (IOException | IllegalArgumentException e) {
            w.add("не вдалося прочитати стан: " + e.getMessage());
            if (quarantine) {
                try { Path bad = f.resolveSibling(f.getFileName() + ".corrupt-" + System.currentTimeMillis()); Files.move(f, bad, StandardCopyOption.REPLACE_EXISTING); w.add("пошкоджений файл збережено як " + bad.getFileName()); }
                catch (IOException ex) { w.add("не вдалося відкласти пошкоджений файл: " + ex.getMessage()); }
            }
            return new Result(true, false, quarantine, w);
        }
    }
}
