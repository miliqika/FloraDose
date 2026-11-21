package ua.floradose;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

final class ExportService {
    private final Stage stage;
    ExportService(Stage stage) { this.stage = stage; }
    private File ask(String name, String desc, String ext, boolean save) {
        FileChooser fc = new FileChooser(); fc.setInitialFileName(name); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, ext));
        return save ? fc.showSaveDialog(stage) : fc.showOpenDialog(stage);
    }
    void csv(String name, List<String[]> rows) {
        File f = ask(name, "CSV", "*.csv", true); if (f == null) return;
        StringBuilder sb = new StringBuilder("\uFEFF");
        for (String[] r : rows) { for (int k = 0; k < r.length; k++) sb.append(k > 0 ? ";" : "").append('"').append(r[k].replace("\"", "\"\"")).append('"'); sb.append("\r\n"); }
        try { Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8)); } catch (Exception ex) { error(ex); }
    }
    private void error(Exception ex) { new Alert(Alert.AlertType.ERROR, "Не вдалося виконати операцію: " + ex.getMessage()).showAndWait(); }

    void bomReport(int cfg) {
        List<String[]> rows = new ArrayList<>(); rows.add(new String[]{"FloraDose — склад пристрою", "Конфігурація: " + Data.CFG[cfg], Data.CFG_INFO[cfg][3], "Об’єм резервуара: " + String.format("%.1f л", Data.TANK_L)});
        rows.add(new String[]{"Назва", "К-сть", "Модель", "Матеріал", "Місце", "Розміри", "Тип", "Припущення"});
        int total = 0; for (Data.C d : Data.COMPS.values()) { int q = Data.qty(d, cfg); total += q; if (q > 0) rows.add(new String[]{d.name, String.valueOf(q), d.model, Data.mat(d, cfg), d.place, d.dim, d.catName(), d.assumed ? "так" : ""}); }
        rows.add(new String[]{"Разом одиниць", String.valueOf(total), "", "", "", "", "", ""});
        rows.add(new String[]{"Орієнтовна ціна конфігурації", String.valueOf(Data.PRICE[cfg]) + " $", "ціни окремих деталей у лабораторних не вказано", "", "", "", "", ""});
        csv("FloraDose_report_" + Data.CFG[cfg] + ".csv", rows);
    }
    void waterLog(Sim s) {
        List<String[]> rows = new ArrayList<>(); rows.add(new String[]{"Модельний час, год", "Слот", "Профіль", "Об’єм, мл", "Тривалість, с"});
        for (Sim.Watering w : s.waterings) rows.add(new String[]{String.format("%.2f", w.hour), "S" + (w.slot + 1), Data.SPECIES[s.species[w.slot]], String.valueOf((int) w.ml), String.format("%.1f", w.seconds)});
        csv("FloraDose_watering_log.csv", rows);
    }
    void errorLog(Sim s) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy"), tf = DateTimeFormatter.ofPattern("HH:mm:ss");
        List<String[]> rows = new ArrayList<>(); rows.add(new String[]{"Дата", "Час", "Код", "Причина", "Слот", "Рекомендована дія"});
        for (Sim.ErrRec e : s.errors) rows.add(new String[]{e.when.format(df), e.when.format(tf), e.code, e.cause, e.slot > 0 ? "S" + e.slot : "—", e.action});
        csv("FloraDose_error_log.csv", rows);
    }
    void saveState(Properties p) {
        File f = ask("FloraDose_state.properties", "Стан FloraDose", "*.properties", true); if (f == null) return;
        try (FileOutputStream o = new FileOutputStream(f)) { p.store(o, "FloraDose state"); } catch (Exception ex) { error(ex); }
    }
    boolean saveText(String name, String desc, String ext, String text) {
        File f = ask(name, desc, ext, true); if (f == null) return false;
        try { StateStore.write(f.toPath(), text); return true; } catch (Exception ex) { error(ex); return false; }
    }
    String loadText(String desc, String ext) {
        File f = ask("", desc, ext, false); if (f == null) return null;
        try { if (f.length() > StateStore.MAX_BYTES) throw new java.io.IOException("файл завеликий"); return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); } catch (Exception ex) { error(ex); return null; }
    }
    Properties loadState() {
        File f = ask("", "Стан FloraDose", "*.properties", false); if (f == null) return null;
        try (FileInputStream i = new FileInputStream(f)) { Properties p = new Properties(); p.load(i); return p; } catch (Exception ex) { error(ex); return null; }
    }
}
