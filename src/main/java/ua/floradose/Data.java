package ua.floradose;

import java.util.LinkedHashMap;
import java.util.Map;

final class Data {
    static final class C {
        final String id, name, cat, model, mat, place, dim, purpose;
        final int qty;
        final boolean internal, assumed;
        C(String id, String name, int qty, String cat, boolean in, String model, String mat, String place, String dim, String purpose, boolean as) {
            this.id = id; this.name = name; this.qty = qty; this.cat = cat; this.internal = in; this.model = model;
            this.mat = mat; this.place = place; this.dim = dim; this.purpose = purpose; this.assumed = as;
        }
        String catName() { return cat.equals("water") ? "Вода" : cat.equals("el") ? "Електроніка" : "Конструкція"; }
    }

    static final double COL_W = 4.8, COL_D = 15.0, COL_H = 12.5, CH_L = 42, CH_D = 10.5, CH_H = 5.0;
    static final double TANK_ML = COL_W * COL_D * COL_H + CH_L * CH_D * CH_H;
    static final double TANK_L = TANK_ML / 1000.0;
    static double waterLevelCm(double pct) {
        double v = Math.max(0, Math.min(100, pct)) / 100.0 * TANK_ML, ac = COL_W * COL_D, ab = CH_L * CH_D;
        return v <= (ac + ab) * CH_H ? v / (ac + ab) : (v - ab * CH_H) / ac;
    }
    static final String TANK_DIM = String.format("≈ %.1f л: колонка %.0f×%.0f×%.0f мм + основа %.0f×%.0f×%.0f мм", TANK_L, COL_W * 10, COL_D * 10, COL_H * 10, CH_L * 10, CH_D * 10, CH_H * 10);

    static final class ErrorSpec {
        final String code, cause, message, reaction, result, action;
        ErrorSpec(String code, String cause, String message, String reaction, String result, String action) { this.code = code; this.cause = cause; this.message = message; this.reaction = reaction; this.result = result; this.action = action; }
    }
    static final Map<String, C> COMPS = new LinkedHashMap<>();

    private static void c(String id, String name, int q, String cat, boolean in, String model, String mat, String place, String dim, String pur, boolean as) {
        COMPS.put(id, new C(id, name, q, cat, in, model, mat, place, dim, pur, as));
    }

    static {
        c("shell", "Декоративна оболонка", 1, "ext", false, "—", "WPC / шпон / WPC з переробленого полімеру", "Зовнішній корпус (зона 1)", "580×160×140 мм (Д×Ш×В)", "Заокруглений моноліт із горизонтальним волокном; не несе навантаження й не перекриває сенсори.", false);
        c("service", "Знімна сервісна панель", 1, "ext", false, "—", "Як оболонка", "Задня стінка", "420×60×8 мм", "Магнітна панель для доступу до помпи, фільтра й плат (UC5, UC15).", false);
        c("window", "Світлова ніша (дифузне вікно)", 1, "ext", false, "—", "PMMA / PC дифузне", "Фронт, нижня частина", "320×44 мм, заглиблення 10 мм", "Широка напівпрозора смуга з розсіяним світлом LED; крізь неї працює датчик освітленості.", false);
        c("led", "RGBW LED-стрічка + драйвер", 1, "el", true, "RGBW 12 В, ШІМ ≥ 1 кГц", "—", "За дифузним вікном", "≈ 300 мм", "Ambient-підсвітка й індикація рівня води: білий ≥ 40 %, жовтий 15–40 %, червоний < 15 % (D4).", false);
        c("slot", "Вкладиш слота", 5, "ext", false, "—", "PP / rPP", "Верхня панель, крок 84 мм", "лоток 80×60 мм, борт 5 мм", "Знімний горщик-вкладиш; ґрунт, камінці, сенсорна стінка зі стабільною проникністю (PlantSlot).", false);
        c("tank", "Резервуар", 1, "water", false, "—", "Tritan / Tritan Renew", "Лівий торець + задній канал", TANK_DIM, "Запас води для 5 слотів; прозора колонка в торці слугує оглядовим вікном і зоною ємнісного рівнеміра.", false);
        c("lid", "Кришка резервуара й заливний отвір", 1, "ext", false, "—", "Як оболонка + силікон", "Верх лівого торця", "Ø22 мм", "Доливання води без розбирання (КВ-Ф1).", false);
        c("panel", "Сенсорна панель", 1, "el", false, "3,5″ IPS TFT, ємнісний дотик", "PMMA / загартоване скло", "Верх, правий торець, врівень зі стільницею", "126×148 мм", "Глянцева чорна панель: піктограми «вода» й «світло», статус і картки S1–S5 (TouchPanel).", true);
        c("level", "Ємнісний рівнемір", 1, "el", false, "FDC1004 + електроди-смужка", "Мідь на PCB", "Зовнішня стінка резервуара", "4×90 мм", "Вимірює рівень води крізь однорідну стінку (Reservoir.measureLevel).", false);
        c("light", "Датчик освітленості", 5, "el", false, "VEML7700-клас через TCA9548A", "—", "Обід слота (Standard — один спільний)", "5×5 мм", "Освітленість для режиму AUTO підсвітки й діагностики (LightSensor).", true);
        c("ntc", "NTC-термістор слота", 5, "el", false, "NTC 10 кОм", "—", "Обід кожного слота", "3×3 мм", "Температура слота (SlotSensorNode).", true);
        c("electrode", "Ємнісні електроди слота", 5, "el", false, "FDC1004 (2 канали на слот)", "Мідь на PCB вкладиша", "Стінка вкладиша", "20×180 мм", "Вологість/засоленість ґрунту крізь стінку; без електродів у ґрунті.", false);
        c("loadcell", "Тензодатчик ваги слота", 5, "el", true, "Балковий 3 кг + АЦП HX711", "Алюміній", "Під платформою слота, на рамі", "50×12×6 мм", "Вага горщика; підтвердження поливу зважуванням (ФВ-С1).", false);
        c("mic", "Ультразвуковий MEMS-мікрофон", 5, "el", false, "Knowles SPH0641LU4H-1 (PDM, до 80 кГц)", "—", "Обід слота, отвір із сіткою", "3×4 мм", "Акустична діагностика стресу (D3); лише Premium/Eco.", false);
        c("frame", "Несуча рама", 1, "ext", true, "—", "Оцинкована сталь", "Усередині корпусу", "390×80×40 мм", "Жорстка рама під тензодатчики; не залежить від оболонки (зона 2).", false);
        c("pump", "Мікропомпа", 1, "water", true, "12 В діафрагмова", "ABS / EPDM", "Біля резервуара", "Ø25×50 мм", "Подає воду з резервуара в колектор; блокується при мінімумі води.", false);
        c("filter", "Знімний фільтр води", 1, "water", true, "—", "PP + сітка", "Між резервуаром і помпою", "Ø14×40 мм", "Очищає воду; ресурс відстежується (UC5).", true);
        c("manifold", "Колектор", 1, "water", true, "—", "POM", "Уздовж корпусу", "Ø10×315 мм", "Розподіляє воду по п’яти лініях до клапанів.", false);
        c("tube", "Силіконові трубки", 6, "water", true, "Ø6 мм", "Силікон", "Резервуар→помпа, клапани→слоти", "70–200 мм", "Канали подавання води до слотів.", false);
        c("valve", "Мікроклапан 2/2 NC", 5, "water", true, "12 В, нормально закритий", "POM + EPDM", "Над колектором під кожним слотом", "20×20×30 мм", "Індивідуальне дозування (±5 мл); без напруги закритий (НВ-Ф2).", false);
        c("mux", "Мультиплексор I²C + FDC1004", 1, "el", true, "TCA9548A + 3×FDC1004", "FR-4", "Основна плата", "30×20 мм", "12 ємнісних каналів: 5 слотів × 2 + рівнемір × 2.", false);
        c("slotboard", "Плата слота", 5, "el", true, "SMT, вузол сенсорів + HX711", "FR-4 + лак", "Під кожним слотом", "50×30 мм", "Збирає дані вузла слота й передає на основну плату.", true);
        c("pcb", "Основна плата", 1, "el", true, "Власна SMT-плата", "FR-4 + захисний лак", "Сухий відсік, праворуч", "75×120 мм", "Мікроконтролер, драйвери, роз’єми; лак від вологи.", true);
        c("mcu", "Мікроконтролер", 1, "el", true, "ESP32-S3-WROOM-1 (Wi-Fi/BLE, PSRAM)", "—", "На основній платі", "18×26 мм", "SystemController: опитування, рішення про полив, дисплей, синхронізація.", false);
        c("drv", "Драйвери клапанів і помпи", 1, "el", true, "MOSFET-ключі + захисні діоди", "—", "На основній платі", "30×15 мм", "Комутують 12 В для 5 клапанів і помпи.", true);
        c("conn", "Роз’єми", 5, "el", true, "JST-XH", "Пластик / латунь", "Край основної плати", "20×14×12 мм", "Клапани, плати слотів, дисплей, LED, живлення.", true);
        c("psu", "Блок живлення", 1, "el", true, "Адаптер 12 В/3 А + DC-DC", "—", "Сухий відсік", "50×30×40 мм", "Мережевий адаптер, розподіл живлення (PowerUnit).", false);
        c("batt", "Резервний акумулятор", 1, "el", true, "Li-ion 1S, змінний тримач", "—", "Сухий відсік", "Ø18×65 мм", "Живить сенсори й дисплей при втраті мережі (switchToBackup).", false);
        c("bay", "Сухий електронний відсік", 1, "el", true, "—", "ABS UL 94 V-0 / PC/ABS V-0", "Правий торець під дисплеєм", "85×140×78 мм", "Захист IP44; ізольований від водного контуру (зона 3).", false);
        c("partition", "Перегородка відсіку", 1, "el", true, "—", "ABS", "Між водним контуром і відсіком", "140×78×4 мм", "Розділяє воду й електроніку.", false);
        c("cable", "Джгути: живлення й сигнали", 10, "el", true, "AWG22 (живлення), AWG26 (сигнали)", "Мідь, ПВХ", "Плата → клапани й плати слотів", "≈ 80–450 мм", "Оранжеві дроти — 12 В для клапанів, графітові — сигнали плат слотів; прокладені вздовж рами.", false);
    }

    static final class Plant {
        final String name, latin, form, tip; final int thr, dose, wetMax, tMin, tMax, luxMin, luxMax, rhMin, rhMax, soil; final double dry;
        Plant(String name, String latin, String form, int thr, int dose, double dry, int wetMax, int tMin, int tMax, int luxMin, int luxMax, int rhMin, int rhMax, int soil, String tip) {
            this.name = name; this.latin = latin; this.form = form; this.thr = thr; this.dose = dose; this.dry = dry; this.wetMax = wetMax; this.tMin = tMin; this.tMax = tMax;
            this.luxMin = luxMin; this.luxMax = luxMax; this.rhMin = rhMin; this.rhMax = rhMax; this.soil = soil; this.tip = tip;
        }
        String ranges() { return "ґрунт " + thr + "–" + wetMax + " %, " + tMin + "–" + tMax + " °C, " + luxMin + "–" + luxMax + " лк, повітря " + rhMin + "–" + rhMax + " %"; }
    }
    static final Plant[] PLANTS = {
        new Plant("Злакова (драцена)", "Dracaena", "гострий пучок вузького листя", 45, 70, 1.3, 85, 12, 30, 200, 800, 40, 60, 0, "Любить помірну вологість: поливайте, коли верхній шар підсох."),
        new Plant("Сукулент (ехеверія)", "Echeveria", "розетка м’ясистих листків", 25, 30, 0.45, 70, 8, 35, 450, 1000, 30, 50, 1, "Мало води й багато світла; надлишок вологи викликає гниль."),
        new Plant("Пеларгонія", "Pelargonium", "гіллясті стебла з лопатевим листям", 40, 60, 0.9, 80, 10, 30, 400, 1000, 40, 60, 0, "Потребує яскравого світла; не заливайте — ґрунт має трохи просихати."),
        new Plant("Сукулент дрібний", "Crassula", "пучок товстого листя", 25, 30, 0.5, 70, 8, 34, 400, 1000, 30, 50, 1, "Накопичує воду в листі; поливайте рідко й помірно."),
        new Plant("Широколиста (фікус)", "Ficus", "гілки з великими глянцевими листками", 50, 80, 1.5, 88, 15, 30, 250, 800, 50, 70, 3, "Любить рівномірну вологість і розсіяне світло; боїться протягів."),
    };
    static final class Soil {
        final String name, note; final double dryMul; final int initMoist;
        Soil(String name, double dryMul, int initMoist, String note) { this.name = name; this.dryMul = dryMul; this.initMoist = initMoist; this.note = note; }
    }
    static final Soil[] SOILS = {
        new Soil("Універсальний ґрунт", 1.0, 45, "Середня вологоємність."),
        new Soil("Для сукулентів (мінеральний)", 1.35, 25, "Швидко дренується й просихає."),
        new Soil("Торфовий (вологоємний)", 0.75, 60, "Довго тримає воду."),
        new Soil("Кокосовий субстрат", 0.9, 50, "Рівномірно вологий, добре аерується."),
    };
    static final String[] SPECIES = new String[PLANTS.length];
    static final int[] THR = new int[PLANTS.length], DOSE = new int[PLANTS.length];
    static final double[] DRY = new double[PLANTS.length];
    static { for (int i = 0; i < PLANTS.length; i++) { SPECIES[i] = PLANTS[i].name; THR[i] = PLANTS[i].thr; DOSE[i] = PLANTS[i].dose; DRY[i] = PLANTS[i].dry; } }
    static final String[] RISK = {"норма", "посуха", "перезволоження", "дефіцит світла", "перегрів", "переохолодження"};

    static final ErrorSpec[] ERR = {
        new ErrorSpec("E1", "Рівень води нижче мінімуму (< 5 %)", "Мало води, долийте", "Насос заблоковано, клапани закриті, LED червоний", "Полив скасовано; сповіщення про доливання", "Долийте воду в резервуар до позначки MAX; полив відновиться автоматично."),
        new ErrorSpec("E2", "Некоректні дані датчика (3 повтори)", "Помилка датчика, слот N", "Сенсор позначено несправним", "Слот пропущено, решта продовжують", "Перевірте кабель і очистьте електроди слота; за потреби замініть датчик."),
        new ErrorSpec("E3", "Клапан не відкрився / вага не змінилась", "Немає потоку, слот N", "Аварійне закриття клапана, зупинка помпи", "Слот не полито, сигнал на панель", "Перевірте трубку й клапан на засмічення, промийте фільтр і перезапустіть цикл."),
        new ErrorSpec("E4", "Втрата зв’язку з платою слота", "Втрата зв’язку, слот N", "Ізоляція компонента, клапан закритий", "Слот пропущено", "Перевірте роз’єм плати слота й перезавантажте пристрій."),
        new ErrorSpec("E5", "Переривання циклу поливу", "Полив перервано", "Клапан закрито, помпу зупинено, об’єм збережено", "Частковий полив, цикл завершено", "Повторіть цикл, коли буде зручно; перерваний цикл збережено в журналі."),
        new ErrorSpec("E6", "Втрата живлення", "Резервний акумулятор", "Помпа, клапани, LED вимкнено; стан збережено", "Мінімальне споживання; відновлення зі збереженого стану", "Відновіть мережеве живлення; резервний акумулятор тимчасово живить сенсори."),
        new ErrorSpec("E7", "Ризик перезволоження (TinyML)", "Потрібна увага, слот N", "Автополив слота призупинено (ФВ-С2)", "Слот не поливається, підсвічується", "Не поливайте слот; дайте ґрунту підсохнути й перевірте дренаж.")};

    static final String[][] CFG_ROWS = {
        {"Декоративна оболонка", "WPC (HDPE/PP) з деревним наповнювачем", "Шпон дуба/ясена FSC", "WPC з переробленого полімеру"},
        {"Сенсорна панель", "PMMA", "Загартоване скло", "PMMA"},
        {"Резервуар", "Tritan", "Tritan", "Tritan Renew"},
        {"Вкладиші слотів", "PP", "PP", "rPP (перероблений)"},
        {"Датчики освітленості", "1 спільний", "5 (на кожен слот)", "5 (на кожен слот)"},
        {"Мікрофони (акустична діагностика D3)", "немає", "5", "5"},
        {"Діагностика ризиків", "Пороги вологості, світла й температури", "+ акустичний аналіз (D3)", "+ акустичний аналіз (D3)"},
        {"Орієнтовна ціна", "≈ 129,5 $", "≈ 153,5 $", "≈ 149,5 $"}};
    static final double[] PRICE = {129.5, 153.5, 149.5};
    static final String[] CFG = {"Standard", "Premium", "Eco"};
    static final String[][] CFG_INFO = {
        {"WPC (HDPE/PP) з деревним наповнювачем", "Tritan", "PMMA", "≈ 129,5 $", "один спільний датчик освітленості, без мікрофонів"},
        {"Шпон дуба/ясена FSC на вологостійкій основі", "Tritan", "Загартоване скло", "≈ 153,5 $", "5 мікрофонів, повна діагностика (D3)"},
        {"WPC з переробленого полімеру", "Tritan Renew", "PMMA", "≈ 149,5 $", "вкладиші rPP, пакування FSC, 5 мікрофонів"}};

    static String mat(C c, int cfg) {
        switch (c.id) {
            case "shell": case "front": return CFG_INFO[cfg][0];
            case "tank": return CFG_INFO[cfg][1];
            case "panel": return CFG_INFO[cfg][2];
            case "slot": return cfg == 2 ? "rPP (перероблений PP)" : "PP";
            default: return c.mat;
        }
    }
    static boolean instanceVisible(String id, int idx, int cfg) {
        if (id.equals("mic")) return cfg != 0;
        if (id.equals("light")) return cfg != 0 || idx <= 0;
        return true;
    }
    static int qty(C c, int cfg) { return cfg == 0 ? (c.id.equals("mic") ? 0 : c.id.equals("light") ? 1 : c.qty) : c.qty; }
    private Data() {}
}
