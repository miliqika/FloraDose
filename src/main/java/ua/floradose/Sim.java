package ua.floradose;

import javafx.scene.paint.Color;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class Sim {
    static final class Ev {
        final double t; final String cat; final int slot, sev; final String text;
        Ev(double t, String cat, int slot, int sev, String text) { this.t = t; this.cat = cat; this.slot = slot; this.sev = sev; this.text = text; }
    }
    static final class Watering { final double hour; final int slot; final double ml, seconds; Watering(double h, int s, double ml, double sec) { hour = h; slot = s; this.ml = ml; seconds = sec; } }
    static final class ErrRec { final LocalDateTime when; final String code, cause, action; final int slot; ErrRec(String code, String cause, int slot, String action) { when = LocalDateTime.now(); this.code = code; this.cause = cause; this.slot = slot; this.action = action; } }
    static final class Sample { final double hour, tank, temp; final double[] moist; Sample(double h, double[] m, double t, double temp) { hour = h; moist = m.clone(); tank = t; this.temp = temp; } }
    interface Hook { void event(Ev e); void disp(String a, String b, Color c); }

    static final double RATE_ML_S = 20;
    static final double ML_PER_PCT = 3.0;

    final Hook h;
    double tank = 85, capL = Data.TANK_L, lux = 420, tempAmb = 22, doseCorr = 100, clock;
    final double[] moist = DEFAULT_MOIST.clone();
    final boolean[] sel = {true, true, true, true, true};
    static final double[] DEFAULT_MOIST = {30, 70, 25, 55, 20};
    final int[] species = {0, 1, 2, 3, 4};
    final int[] soil = {0, 1, 0, 1, 3}, thrOv = {-1, -1, -1, -1, -1}, doseOv = {-1, -1, -1, -1, -1};
    int ledMode; static final String[] LED_MODES = {"АВТО", "УВІМК", "ВИМК"};
    boolean fPump, pumpBad;
    final int[] vs = new int[5], bad = new int[5];
    final boolean[] flow = new boolean[5], man = new boolean[5], alert = new boolean[5];
    final double[] plan = new double[5], actual = new double[5], mBefore = new double[5], mAfter = new double[5];
    final String[] status = new String[5]; final int[] sev = new int[5];
    final List<Double> useHist = new ArrayList<>();
    int fSensor = -1, fValve = -1, fComm = -1, lastErr = 0, phase = 99, slot, pourK;
    boolean interrupt, powerLoss, pumpBlocked, wasPowerLoss;
    double m0, cycleUse, hours, battery = 100; boolean onBattery;
    final double[] waterUsed = new double[5]; final List<Watering> waterings = new ArrayList<>(); final List<ErrRec> errors = new ArrayList<>(); final List<Sample> history = new ArrayList<>();
    private boolean[] selBackup; private double pourStartHour;
    double slotTemp(int i) { return tempAmb + (i - 2) * 0.25; }
    double dryRate(int i) { return Data.DRY[species[i]] * Data.SOILS[soil[i]].dryMul * (1 + 0.04 * (tempAmb - 22)) * (0.6 + 0.8 * lux / 1000.0); }
    double hoursToNext(int i) { double r = dryRate(i); return moist[i] <= thr(i) ? 0 : (moist[i] - thr(i)) / Math.max(r, 0.01); }
    double targetMoisture(int i) { return Math.min(100, thr(i) + dose(i) / ML_PER_PCT); }
    void advance(double dtReal) { hours += dtReal * timeScale / 3600.0; if (onBattery) battery = Math.max(0, battery - 0.15 * dtReal); }
    void sample() { history.add(new Sample(hours, moist, tank, tempAmb)); if (history.size() > 600) history.remove(0); }
    void restorePower() { if (onBattery) { onBattery = false; powerLoss = false; ev("Система", 0, 0, "Мережеве живлення відновлено; стан відновлено зі збереженого"); } }
    int overall() { for (int v : vs) if (v == 2) return 2; if (onBattery || tank < 5 || pumpBad) return 2; for (int b : bad) if (b != 0) return 2; return tank < 15 || lastErr > 0 && running() ? 1 : 0; }
    String overallText() { int o = overall(); return o == 2 ? "Потрібне втручання" : o == 1 ? "Увага" : "Норма"; }
    void beginSingle(int i) { if (running()) return; selBackup = sel.clone(); Arrays.fill(sel, false); sel[i] = true; begin(); }
    void beginAll() { if (running()) return; Arrays.fill(sel, true); begin(); }
    String mcu = "вимк.", pump = "вимк.", phaseName = "очікування";

    Sim(Hook h) { this.h = h; }

    double weight(int i) { return 900 + moist[i] * 3; }
    int thr(int i) { return thrOv[i] >= 0 ? thrOv[i] : Data.THR[species[i]]; }
    double dose(int i) { return (doseOv[i] >= 0 ? doseOv[i] : Data.DOSE[species[i]]) * doseCorr / 100.0; }
    boolean running() { return phase < 99; }
    double tankMl() { return tank * capL * 10; }
    int ledState() { return tank >= 40 ? 0 : tank >= 15 ? 1 : 2; }
    String ledName() { return new String[]{"білий", "жовтий", "червоний"}[ledState()]; }
    Color led() { return new Color[]{Color.web("#ffb274"), Color.web("#ffd24a"), Color.web("#ff3b30")}[ledState()]; }
    String ledModeName() { return LED_MODES[ledMode]; }
    Color ledColor() { return ledMode == 1 ? Color.web("#ffb274") : led(); }
    double ledLevel() { return ledMode == 2 ? 0 : 1; }
    void setLedMode(int m) { if (m < 0 || m > 2) throw new IllegalArgumentException("Режим LED: 0..2, отримано " + m); if (m == ledMode) return; ledMode = m; ev("Система", 0, 0, "Підсвічування: " + LED_MODES[m]); }
    void cycleLedMode() { setLedMode((ledMode + 1) % 3); }
    double dailyUse() {
        if (!useHist.isEmpty()) { double s = 0; for (double d : useHist) s += d; return s / useHist.size(); }
        double s = 0; for (int i = 0; i < 5; i++) if (sel[i]) s += dose(i); return s;
    }
    double daysLeft() { return tankMl() / Math.max(dailyUse(), 1); }
    String forecastBasis() { return useHist.isEmpty() ? "за планом доз (1 цикл/добу)" : "за середнім із " + useHist.size() + " цикл(ів)"; }
    int classify(int i) {
        Data.Plant p = Data.PLANTS[species[i]]; double m = moist[i];
        if (m > p.wetMax) return 2; if (m < Math.max(8, thr(i) * 0.55)) return 1; if (lux < p.luxMin * 0.4) return 3; if (tempAmb > p.tMax) return 4; if (tempAmb < p.tMin) return 5; return 0;
    }
    private static void checkSlot(int i) { if (i < 0 || i > 4) throw new IllegalArgumentException("Слот 0..4, отримано " + i); }
    static double clamp(double v, double lo, double hi, double def) { return Double.isNaN(v) || Double.isInfinite(v) ? def : Math.max(lo, Math.min(hi, v)); }
    void setSpecies(int i, int sp) {
        checkSlot(i); if (sp < 0 || sp >= Data.PLANTS.length) throw new IllegalArgumentException("Невідомий вид рослини: " + sp);
        if (species[i] == sp) return; String old = Data.PLANTS[species[i]].name; species[i] = sp; thrOv[i] = -1; doseOv[i] = -1; soil[i] = Data.PLANTS[sp].soil;
        ev("Система", i + 1, 0, "Слот " + (i + 1) + ": рослину змінено «" + old + "» → «" + Data.PLANTS[sp].name + "»; поріг " + thr(i) + " %, доза " + (int) dose(i) + " мл, ґрунт «" + Data.SOILS[soil[i]].name + "»");
    }
    void setSoil(int i, int sl, double initialMoisture) {
        checkSlot(i); if (sl < 0 || sl >= Data.SOILS.length) throw new IllegalArgumentException("Невідомий тип ґрунту: " + sl);
        soil[i] = sl; moist[i] = initialMoisture < 0 ? Data.SOILS[sl].initMoist : clamp(initialMoisture, 0, 100, Data.SOILS[sl].initMoist);
        ev("Система", i + 1, 0, "Слот " + (i + 1) + ": ґрунт «" + Data.SOILS[sl].name + "», вологість " + (int) moist[i] + " %");
    }
    void setThreshold(int i, int v) { checkSlot(i); thrOv[i] = v < 0 ? -1 : (int) clamp(v, 5, 95, 40); ev("Система", i + 1, 0, "Слот " + (i + 1) + ": поріг поливу " + thr(i) + " %" + (thrOv[i] < 0 ? " (за профілем)" : " (індивідуальний)")); }
    void setDoseOverride(int i, int ml) { checkSlot(i); doseOv[i] = ml < 0 ? -1 : (int) clamp(ml, 10, 200, 60); ev("Система", i + 1, 0, "Слот " + (i + 1) + ": доза " + (int) dose(i) + " мл" + (doseOv[i] < 0 ? " (за профілем)" : " (індивідуальна)")); }
    double refill(double toPct) {
        double target = Math.max(tank, clamp(toPct, 0, 100, 100)), added = (target - tank) * capL * 10; tank = target;
        if (tank >= 5) { pumpBlocked = false; if (lastErr == 1) lastErr = 0; }
        ev("Резервуар", 0, 0, "Долито " + (int) added + " мл, рівень " + (int) tank + " % (UC11)"); return added;
    }
    void setPumpFault(boolean b) { fPump = b; if (!b) { pumpBad = false; if (!running()) pump = "ОК"; } ev("Система", 0, b ? 1 : 0, b ? "Тестовий збій: помпа не створює тиск" : "Тестовий збій помпи знято"); }
    int needWater() { int n = 0; for (int i = 0; i < 5; i++) if (moist[i] < thr(i)) n++; return n; }
    double usedToday() { double s = 0; for (Watering w : waterings) if (hours - w.hour < 24) s += w.ml; return s; }
    int nextSlot() { int b = -1; double bh = Double.MAX_VALUE; for (int i = 0; i < 5; i++) { if (!sel[i]) continue; double h = hoursToNext(i); if (h < bh) { bh = h; b = i; } } return b; }
    String nextWateringText() {
        int i = nextSlot(); if (i < 0) return "немає слотів у циклі"; double h = hoursToNext(i);
        String when = h <= 0 ? "зараз" : h < 1 ? "менше ніж за годину" : h < 48 ? String.format(Locale.ROOT, "через ≈ %.0f год", h) : String.format(Locale.ROOT, "через ≈ %.1f діб", h / 24);
        return "S" + (i + 1) + " · " + Data.PLANTS[species[i]].name.split(" ")[0] + " — " + when;
    }
    List<String> issues() {
        List<String> r = new ArrayList<>();
        if (onBattery) r.add("Живлення від резервного акумулятора"); if (tank < 5) r.add("Резервуар порожній — долийте воду"); else if (tank < 15) r.add("Низький рівень води — долийте воду");
        if (pumpBad || fPump) r.add("Помпа не створює тиск"); for (int i = 0; i < 5; i++) { if (vs[i] == 2) r.add("Клапан V" + (i + 1) + " несправний"); if (bad[i] != 0) r.add("Датчик слота S" + (i + 1) + " не відповідає"); }
        for (int i = 0; i < 5; i++) { int k = classify(i); if (k != 0 && bad[i] == 0) r.add("S" + (i + 1) + ": " + Data.RISK[k]); }
        return r;
    }
    String diagnosticsText() { List<String> r = issues(); return r.isEmpty() ? "Усе в нормі" : String.join("; ", r); }
    static final String[] SCENARIOS = {"Норма", "Спекотний сухий день", "Темна кімната", "Мало води", "Перезволоження слота 1"};
    void scenario(int k) {
        if (k < 0 || k >= SCENARIOS.length) throw new IllegalArgumentException("Сценарій 0.." + (SCENARIOS.length - 1));
        if (k == 0) { tank = 85; lux = 420; tempAmb = 22; doseCorr = 100; System.arraycopy(DEFAULT_MOIST, 0, moist, 0, 5); fSensor = fValve = fComm = -1; setPumpFault(false); drying = false; lastErr = 0; pumpBlocked = false; }
        else if (k == 1) { tempAmb = 34; lux = 900; drying = true; for (int i = 0; i < 5; i++) moist[i] = Math.min(moist[i], thr(i) + 8.0); }
        else if (k == 2) { lux = 70; }
        else if (k == 3) { tank = 12; }
        else { species[0] = 1; moist[0] = Math.max(moist[0], Data.PLANTS[1].wetMax + 8.0); }
        ev("Система", 0, 0, "Сценарій: " + SCENARIOS[k]);
    }
    void resetToDefaults() {
        if (running()) throw new IllegalStateException("Скидання неможливе під час виконання циклу");
        for (int i = 0; i < 5; i++) { species[i] = i; soil[i] = Data.PLANTS[i].soil; thrOv[i] = -1; doseOv[i] = -1; sel[i] = true; waterUsed[i] = 0; alert[i] = false; status[i] = null; sev[i] = 0; plan[i] = actual[i] = 0; mBefore[i] = mAfter[i] = Double.NaN; }
        scenario(0); autoStart = false; ledMode = 0; timeScale = 600; battery = 100; hours = 0; phaseName = "очікування";
        useHist.clear(); waterings.clear(); history.clear(); errors.clear();
        ev("Система", 0, 0, "Налаштування скинуто до типових");
    }
    List<String> sanitize() {
        List<String> w = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (species[i] < 0 || species[i] >= Data.PLANTS.length) { w.add("слот " + (i + 1) + ": невідомий вид, встановлено типовий"); species[i] = i; }
            if (soil[i] < 0 || soil[i] >= Data.SOILS.length) { w.add("слот " + (i + 1) + ": невідомий ґрунт, встановлено за профілем"); soil[i] = Data.PLANTS[species[i]].soil; }
            if (thrOv[i] != -1 && (thrOv[i] < 5 || thrOv[i] > 95)) { w.add("слот " + (i + 1) + ": поріг поза 5–95 %, скинуто до профілю"); thrOv[i] = -1; }
            if (doseOv[i] != -1 && (doseOv[i] < 10 || doseOv[i] > 200)) { w.add("слот " + (i + 1) + ": доза поза 10–200 мл, скинуто до профілю"); doseOv[i] = -1; }
            double m = clamp(moist[i], 0, 100, DEFAULT_MOIST[i]); if (m != moist[i]) { w.add("слот " + (i + 1) + ": некоректна вологість"); moist[i] = m; }
        }
        tank = clamp(tank, 0, 100, 85); lux = clamp(lux, 0, 1000, 420); tempAmb = clamp(tempAmb, 10, 40, 22); doseCorr = clamp(doseCorr, 50, 150, 100); timeScale = clamp(timeScale, 60, 3600, 600);
        if (ledMode < 0 || ledMode > 2) { w.add("режим LED некоректний, встановлено АВТО"); ledMode = 0; }
        return w;
    }

    void ev(String cat, int slot, int sev, String text) { h.event(new Ev(clock, cat, slot, sev, text)); }

    void begin() {
        Arrays.fill(status, null); Arrays.fill(sev, 0); Arrays.fill(bad, 0); Arrays.fill(vs, 0); Arrays.fill(flow, false); Arrays.fill(man, false); Arrays.fill(alert, false);
        Arrays.fill(plan, 0); Arrays.fill(actual, 0); Arrays.fill(mBefore, Double.NaN); Arrays.fill(mAfter, Double.NaN);
        interrupt = false; powerLoss = false; onBattery = false; pumpBlocked = false; phase = 0; slot = 0; lastErr = 0; cycleUse = 0;
    }
    void stop(String msg) { closeAll(); phase = 99; if (selBackup != null) { System.arraycopy(selBackup, 0, sel, 0, 5); selBackup = null; } mcu = "ОК"; pump = "ОК"; phaseName = "зупинено"; ev("Система", 0, 0, msg); h.disp("FloraDose", "Зупинено", Color.web("#ffd27f")); }
    private void closeAll() { for (int i = 0; i < 5; i++) { if (vs[i] == 1) vs[i] = 0; flow[i] = false; man[i] = false; } }

    private void err(int code, int i) {
        Data.ErrorSpec r = Data.ERR[code - 1]; String msg = r.message.replace("N", String.valueOf(i + 1));
        lastErr = code; errors.add(new ErrRec(r.code, r.cause, i + 1, r.action)); ev("Помилка", i + 1, 2, r.code + ": " + msg + " → " + r.reaction + ". Дія: " + r.action);
        h.disp("Помилка " + r.code, msg, Color.web("#ff8a8a"));
        if (i >= 0) { status[i] = r.code + ": " + r.result; sev[i] = 2; }
    }
    private void done(int i, String s, int sv) { status[i] = s; sev[i] = sv; }

    void toggleValve(int i) {
        if (vs[i] == 1) { vs[i] = 0; man[i] = false; flow[i] = false; ev("Полив", i + 1, 0, "Клапан V" + (i + 1) + " закрито вручну"); return; }
        if (tank < 5) { err(1, -1); return; }
        if (fValve == i) { vs[i] = 2; err(3, i); return; }
        if (fPump) { pumpBad = true; ev("Помилка", i + 1, 2, "Причина: помпа не створює тиск — потоку немає"); err(3, i); return; }
        vs[i] = 1; man[i] = true; flow[i] = true; ev("Полив", i + 1, 0, "Клапан V" + (i + 1) + " відкрито вручну (UC9)");
    }
    void tickManual(double dt) {
        int n = 0;
        for (int i = 0; i < 5; i++) if (man[i]) { n++; moist[i] = Math.min(100, moist[i] + RATE_ML_S * dt / ML_PER_PCT); }
        if (n > 0) {
            tank = Math.max(0, tank - RATE_ML_S * dt * n / (capL * 10));
            if (tank <= 0) { closeAll(); err(1, -1); }
        }
    }
    boolean anyManual() { for (boolean b : man) if (b) return true; return false; }

    boolean drying, autoStart; double timeScale = 600, autoCool;
    void tickDry(double dt) {
        if (!drying) return;
        for (int i = 0; i < 5; i++) {
            if (man[i] || vs[i] == 1) continue;
            moist[i] = Math.max(0, moist[i] - dryRate(i) * dt * timeScale / 3600.0);
        }
    }
    boolean autoDue(double dt) {
        if (!autoStart || running()) return false;
        autoCool -= dt; if (autoCool > 0) return false;
        for (int i = 0; i < 5; i++) {
            if (!sel[i] || moist[i] >= thr(i) - 2) continue;
            autoCool = 15;
            if (tank < 5) { ev("Резервуар", i + 1, 1, "Автополив не запущено: мало води"); return false; }
            if (fSensor == i || fComm == i) { ev("Помилка", i + 1, 1, "Автополив не запущено: датчик слота не відповідає"); return false; }
            if (vs[i] == 2) { ev("Помилка", i + 1, 1, "Автополив не запущено: клапан несправний"); return false; }
            if (pumpBad) { ev("Помилка", i + 1, 1, "Автополив не запущено: помпа несправна"); return false; }
            ev("Система", i + 1, 0, "Автозапуск циклу: вологість " + (int) moist[i] + " % нижче порогу " + thr(i) + " %");
            return true;
        }
        return false;
    }
    void runToEnd() { begin(); int n = 0; while (step() >= 0 && n++ < 2000) { } }

    long step() { long d = step0(); if (d >= 0) clock += d / 1000.0; if (!running() && selBackup != null) { System.arraycopy(selBackup, 0, sel, 0, 5); selBackup = null; } return d; }
    private long step0() {
        if (powerLoss && phase < 99) {
            closeAll(); pump = "вимк."; lastErr = 6; wasPowerLoss = true; onBattery = true; phase = 99; phaseName = "резервне живлення"; errors.add(new ErrRec("E6", Data.ERR[5].cause, 0, Data.ERR[5].action));
            ev("Помилка", 0, 2, "E6: втрата живлення → резервний акумулятор; помпа, клапани, LED вимкнено; стан збережено; режим мінімального споживання");
            h.disp("Резервний акумулятор", "Стан збережено", Color.web("#ff8a8a"));
            return -1;
        }
        switch (phase) {
            case 0:
                phaseName = "ініціалізація";
                ev("Система", 0, 0, "Цикл FloraDose" + (wasPowerLoss ? " (відновлення зі збереженого стану)" : "") + ": подано живлення"); wasPowerLoss = false;
                ev("Система", 0, 0, "Паралельно: самоперевірка сенсорів, помпи, клапанів, LED ‖ завантаження профілів і налаштувань");
                h.disp("FloraDose", "Ініціалізація…", Color.web("#9ff0c0")); mcu = "ініц."; phase = 1; return 900;
            case 1:
                ev("Система", 0, fPump ? 1 : 0, fPump ? "D1: самоперевірка: помпа не відповідає (тестовий збій). Тарування ваги, формування планів поливу" : "D1: самоперевірка успішна. Тарування ваги, формування планів поливу"); mcu = "ОК"; pump = fPump ? "несправна" : "ОК";
                for (int i = 0; i < 5; i++) { plan[i] = sel[i] ? dose(i) : 0; if (sel[i]) ev("Система", i + 1, 0, Data.SPECIES[species[i]] + ": поріг " + thr(i) + " %, доза " + (int) dose(i) + " мл"); }
                h.disp("Самоперевірка", "OK", Color.web("#9ff0c0")); phase = 2; return 900;
            case 2:
                phaseName = "збір даних";
                for (int i = 0; i < 5; i++) {
                    if (fComm == i) { bad[i] = 4; err(4, i); }
                    else if (fSensor == i) { ev("Помилка", i + 1, 1, "Некоректні дані датчика, повтор 1/3, 2/3, 3/3"); bad[i] = 2; err(2, i); }
                    else ev("Дані", i + 1, 0, "вологість " + (int) moist[i] + " %, вага " + (int) weight(i) + " г, " + (int) tempAmb + " °C");
                }
                ev("Дані", 0, 0, "Рівнемір " + (int) tank + " %, освітленість " + (int) lux + " лк");
                h.disp("Опитування сенсорів", "S1–S5, рівень, світло", Color.web("#9ff0c0")); phase = 3; return 1000;
            case 3:
                phaseName = "моніторинг резервуара";
                ev("Резервуар", 0, 0, "Запас ≈ " + String.format("%.1f", daysLeft()) + " діб (" + forecastBasis() + "); LED: " + ledName());
                if (tank < 15) ev("Резервуар", 0, 1, "Рівень низький: долийте воду (UC11)");
                if (tank < 5) { pumpBlocked = true; err(1, -1); }
                h.disp("Вода " + (int) tank + " %", "≈ " + String.format("%.1f", daysLeft()) + " діб, LED " + ledName(), Color.web("#9ff0c0"));
                phase = 4; slot = 0; return 900;
            case 4: {
                while (slot < 5 && !sel[slot]) slot++;
                if (slot >= 5) { phase = 7; return 300; }
                int i = slot, n = i + 1;
                if (bad[i] != 0) { slot++; return 300; }
                phaseName = "діагностика слота " + n;
                int k = classify(i); double m = moist[i];
                ev("Діагностика", n, k == 0 ? 0 : 1, "Правила-заглушка: " + Data.RISK[k]);
                if (m < 12 || m > 92) { alert[i] = true; ev("Діагностика", n, 2, "Потрібне негайне втручання — слот підсвічено червоним"); }
                h.disp("Слот " + n, Data.RISK[k], k == 0 ? Color.web("#9ff0c0") : Color.web("#ffd27f"));
                if (k == 2) { lastErr = 7; errors.add(new ErrRec("E7", Data.ERR[6].cause, n, Data.ERR[6].action)); ev("Помилка", n, 2, "E7: ризик перезволоження → автополив призупинено. Дія: " + Data.ERR[6].action); done(i, "E7: полив призупинено (перезволоження)", 2); slot++; return 800; }
                phase = 5; return 800;
            }
            case 5: {
                int i = slot, n = i + 1;
                phaseName = "рішення про полив, слот " + n;
                if (moist[i] >= thr(i)) { done(i, "полив не потрібен", 0); mBefore[i] = mAfter[i] = moist[i]; ev("Полив", n, 0, "D2: вологість " + (int) moist[i] + " % ≥ поріг " + thr(i) + " % — полив не потрібен"); h.disp("Слот " + n, "Норма " + (int) moist[i] + " %", Color.web("#9ff0c0")); slot++; phase = 4; return 600; }
                if (pumpBlocked || tank < 5) { err(1, i); slot++; phase = 4; return 400; }
                if (pumpBad && fPump) { ev("Полив", n, 1, "Слот " + n + " пропущено: помпа несправна"); done(i, "E3: помпа несправна", 2); slot++; phase = 4; return 300; }
                ev("Полив", n, 0, "D2: потрібен полив. Клапан V" + n + " відкрито, доза " + (int) dose(i) + " мл");
                vs[i] = 1; flow[i] = true; pump = "працює"; pourK = 0; m0 = moist[i]; pourStartHour = hours; mBefore[i] = m0; h.disp("Слот " + n, "Полив " + (int) dose(i) + " мл", Color.web("#7fd4ff")); phase = 6; return 700;
            }
            case 6: {
                int i = slot, n = i + 1; double d = dose(i);
                phaseName = "полив, слот " + n;
                if (interrupt) { vs[i] = 0; flow[i] = false; pump = "ОК"; actual[i] = (moist[i] - m0) * ML_PER_PCT; mAfter[i] = moist[i]; cycleUse += actual[i]; waterUsed[i] += actual[i]; waterings.add(new Watering(pourStartHour, i, actual[i], actual[i] / RATE_ML_S)); err(5, i); done(i, "E5: частковий полив " + (int) actual[i] + " мл", 1); phase = 7; return 500; }
                if (fValve == i || fPump) {
                    if (pourK >= 3) {
                        flow[i] = false;
                        if (fValve == i) { vs[i] = 2; pump = "ОК"; } else { vs[i] = 0; pumpBad = true; pump = "несправна"; ev("Помилка", n, 2, "Причина: помпа не створює тиск — потоку немає"); }
                        err(3, i); slot++; phase = 4; return 500;
                    }
                    pourK++; return 400;
                }
                if (tank <= 2) { vs[i] = 0; flow[i] = false; pump = "ОК"; actual[i] = (moist[i] - m0) * ML_PER_PCT; mAfter[i] = moist[i]; cycleUse += actual[i]; err(1, i); slot++; phase = 4; return 400; }
                tank = Math.max(0, tank - d / 8 / (capL * 10)); moist[i] = Math.min(100, moist[i] + d / 8 / ML_PER_PCT); pourK++;
                if (pourK >= 8) {
                    vs[i] = 0; flow[i] = false; pump = "ОК"; mAfter[i] = moist[i]; actual[i] = (moist[i] - m0) * ML_PER_PCT; cycleUse += actual[i]; waterUsed[i] += actual[i]; waterings.add(new Watering(pourStartHour, i, actual[i], actual[i] / RATE_ML_S));
                    boolean corr = Math.abs(actual[i] - d) > d * 0.1;
                    ev("Полив", n, corr ? 1 : 0, "Клапан V" + n + " закрито; Δ ваги " + (int) actual[i] + " г при плані " + (int) d + " мл" + (corr ? " → корекція плану" : ""));
                    done(i, "полито " + (int) actual[i] + " мл", 0); h.disp("Слот " + n, "Готово ✓", Color.web("#9ff0c0"));
                    slot++; phase = 4; return 700;
                }
                return 280;
            }
            case 7:
                phaseName = "збереження";
                useHist.add(cycleUse); if (useHist.size() > 7) useHist.remove(0);
                ev("Резервуар", 0, 0, "Рівень " + (int) tank + " %, LED " + ledName() + ", запас ≈ " + String.format("%.1f", daysLeft()) + " діб (" + forecastBasis() + ")");
                ev("Система", 0, 0, "Показники й події збережено; синхронізація (за наявності); цикл завершено. Використано " + (int) cycleUse + " мл");
                h.disp("Цикл завершено", "Стан оновлено ✓", Color.web("#9ff0c0")); phase = 99; phaseName = "очікування"; return -1;
            default: return -1;
        }
    }
}
