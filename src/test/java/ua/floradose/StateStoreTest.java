package ua.floradose;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class StateStoreTest {
    @TempDir Path dir;
    Sim s;
    static Sim newSim() { return new Sim(new Sim.Hook() { public void event(Sim.Ev e) { } public void disp(String a, String b, Color c) { } }); }
    @BeforeEach void init() { s = newSim(); }

    private void customise(Sim x) {
        x.setSpecies(1, 4); x.setSoil(1, 2, 61.5); x.setThreshold(0, 52); x.setDoseOverride(3, 95); x.sel[2] = false; x.tank = 63.25; x.lux = 780; x.tempAmb = 27.5; x.doseCorr = 120; x.timeScale = 1200;
        x.autoStart = true; x.drying = true; x.setLedMode(2); x.useHist.add(150.0); x.useHist.add(210.5); x.waterUsed[4] = 333.3;
    }
    private static Map<String, Object> parse(String t) { return Json.obj(Json.parse(t)); }
    private String json(String mutateKey, Object v) { Map<String, Object> m = StateStore.toMap(s, false); m.put(mutateKey, v); return Json.write(m); }

    @Test void roundTripRestoresEverything() {
        customise(s); String text = StateStore.toJson(s, true); Sim t = newSim(); List<String> w = StateStore.apply(Json.parse(text), t);
        assertTrue(w.isEmpty(), w.toString());
        assertArrayEquals(s.species, t.species); assertArrayEquals(s.soil, t.soil); assertArrayEquals(s.thrOv, t.thrOv); assertArrayEquals(s.doseOv, t.doseOv); assertArrayEquals(s.sel, t.sel);
        for (int i = 0; i < 5; i++) { assertEquals(s.moist[i], t.moist[i], 0.051); assertEquals(s.waterUsed[i], t.waterUsed[i], 0.051); }
        assertEquals(63.25, t.tank, 1e-9); assertEquals(780, t.lux, 1e-9); assertEquals(27.5, t.tempAmb, 1e-9); assertEquals(120, t.doseCorr, 1e-9); assertEquals(1200, t.timeScale, 1e-9);
        assertTrue(t.autoStart); assertTrue(t.drying); assertEquals(2, t.ledMode); assertEquals(s.useHist, t.useHist);
    }
    @Test void stampIsOptionalAndFormatIsVersioned() {
        assertFalse(StateStore.toMap(s, false).containsKey("savedAt")); assertTrue(StateStore.toMap(s, true).containsKey("savedAt")); assertEquals(StateStore.VERSION, StateStore.toMap(s, false).get("version"));
        assertEquals(5, Json.arr(StateStore.toMap(s, false).get("plants")).size());
    }
    @Test void savedFileLoadsBack() throws IOException {
        customise(s); StateStore st = new StateStore(dir.resolve("sub/state.json")); st.save(s);
        Sim t = newSim(); StateStore.Result r = st.load(t); assertTrue(r.found && r.loaded && !r.recovered); assertTrue(r.warnings.isEmpty(), r.warnings.toString()); assertEquals(63.25, t.tank, 1e-9); assertEquals(95, t.doseOv[3]);
    }
    @Test void writeIsAtomicAndLeavesNoTempFile() throws IOException {
        Path f = dir.resolve("state.json"); StateStore.write(f, "{\"v\":1}"); StateStore.write(f, "{\"v\":2}");
        assertEquals("{\"v\":2}", new String(Files.readAllBytes(f), StandardCharsets.UTF_8));
        try (Stream<Path> l = Files.list(dir)) { assertEquals(List.of("state.json"), l.map(p -> p.getFileName().toString()).collect(Collectors.toList())); }
    }
    @Test void missingFileIsNotAnError() { StateStore.Result r = new StateStore(dir.resolve("none.json")).load(s); assertFalse(r.found); assertFalse(r.loaded); assertFalse(r.recovered); assertTrue(r.warnings.isEmpty()); }
    @Test void corruptFileIsQuarantinedAndSimKeepsDefaults() throws IOException {
        Path f = dir.resolve("state.json"); Files.write(f, "{\"version\": 1, \"plants\": [".getBytes(StandardCharsets.UTF_8));
        double tank = s.tank; StateStore.Result r = new StateStore(f).load(s);
        assertTrue(r.found); assertFalse(r.loaded); assertTrue(r.recovered); assertFalse(r.warnings.isEmpty()); assertFalse(Files.exists(f)); assertEquals(tank, s.tank, 1e-9);
        try (Stream<Path> l = Files.list(dir)) { assertTrue(l.anyMatch(p -> p.getFileName().toString().startsWith("state.json.corrupt-"))); }
    }
    @Test void readWithoutQuarantineLeavesFile() throws IOException {
        Path f = dir.resolve("state.json"); Files.write(f, "не json".getBytes(StandardCharsets.UTF_8)); StateStore.Result r = StateStore.read(f, s, false);
        assertFalse(r.loaded); assertFalse(r.recovered); assertTrue(Files.exists(f));
    }
    @Test void oversizedFileIsRejected() throws IOException {
        Path f = dir.resolve("big.json"); Files.write(f, new byte[(int) StateStore.MAX_BYTES + 10]); StateStore.Result r = StateStore.read(f, s, false); assertFalse(r.loaded); assertTrue(r.warnings.get(0).contains("завеликий"));
    }
    @Test void wrongRootOrVersionIsRejectedBeforeAnyChange() {
        customise(s); double tank = s.tank;
        assertThrows(IllegalArgumentException.class, () -> StateStore.apply(Json.parse("[1,2]"), s));
        assertThrows(IllegalArgumentException.class, () -> StateStore.apply(Json.parse("{\"plants\": []}"), s));
        assertThrows(IllegalArgumentException.class, () -> StateStore.apply(Json.parse(json("version", 99.0)), s));
        assertThrows(IllegalArgumentException.class, () -> StateStore.apply(Json.parse(json("version", 0.0)), s));
        assertThrows(IllegalArgumentException.class, () -> StateStore.apply(Json.parse(json("version", "1")), s));
        assertEquals(tank, s.tank, 1e-9); assertEquals(4, s.species[1]);
    }
    @Test void invalidFieldsFallBackWithWarnings() {
        Map<String, Object> m = StateStore.toMap(s, false); List<Object> pl = Json.arr(m.get("plants"));
        Map<String, Object> p0 = Json.obj(pl.get(0)), p1 = Json.obj(pl.get(1)), p2 = Json.obj(pl.get(2));
        p0.put("species", 99.0); p1.put("moisture", 150.0); p1.put("threshold", 3.0); p2.put("dose", "багато"); p2.put("inCycle", "так"); m.put("tankPercent", "повний"); m.put("ledMode", 7.0); m.put("temperature", -50.0);
        Sim t = newSim(); List<String> w = StateStore.apply(Json.parse(Json.write(m)), t);
        assertEquals(0, t.species[0]); assertEquals(Sim.DEFAULT_MOIST[1], t.moist[1], 1e-9); assertEquals(-1, t.thrOv[1]); assertEquals(-1, t.doseOv[2]); assertTrue(t.sel[2]);
        assertEquals(85, t.tank, 1e-9); assertEquals(0, t.ledMode); assertEquals(22, t.tempAmb, 1e-9);
        String all = String.join("|", w); for (String k : new String[]{"slot 1".replace("slot", "слот"), "tankPercent", "ledMode", "temperature", "moisture", "threshold", "dose", "inCycle"}) assertTrue(all.contains(k), k + " ∉ " + all);
    }
    @Test void fractionalAndNonNumericIndexesAreRejected() {
        Map<String, Object> m = StateStore.toMap(s, false); Json.obj(Json.arr(m.get("plants")).get(3)).put("species", 2.5); Json.obj(Json.arr(m.get("plants")).get(4)).put("soil", "x");
        Sim t = newSim(); List<String> w = StateStore.apply(Json.parse(Json.write(m)), t); assertEquals(3, t.species[3]); assertEquals(Data.PLANTS[4].soil, t.soil[4]); assertEquals(2, w.size(), w.toString());
    }
    @Test void wrongSlotCountAndMissingPlantsAreReported() {
        Map<String, Object> m = StateStore.toMap(s, false); Json.arr(m.get("plants")).remove(4); Sim t = newSim(); t.setSpecies(4, 1);
        List<String> w = StateStore.apply(Json.parse(Json.write(m)), t); assertTrue(w.get(0).contains("5 слотів")); assertEquals(1, t.species[4], "слот поза файлом не змінюється");
        m.remove("plants"); assertTrue(StateStore.apply(Json.parse(Json.write(m)), newSim()).get(0).contains("plants"));
    }
    @Test void unknownKeysAreIgnoredAndHistoryIsFiltered() {
        Map<String, Object> m = StateStore.toMap(s, false); m.put("futureFeature", "ok"); List<Object> uh = new java.util.ArrayList<>(List.of(10.0, -5.0, "x", 20.0)); m.put("useHistory", uh);
        Sim t = newSim(); List<String> w = StateStore.apply(Json.parse(Json.write(m)), t); assertEquals(List.of(10.0, 20.0), t.useHist); assertEquals(2, w.size(), w.toString());
    }
    @Test void historyIsLimitedToSevenEntries() {
        Map<String, Object> m = StateStore.toMap(s, false); List<Object> uh = new java.util.ArrayList<>(); for (int i = 0; i < 20; i++) uh.add(100.0 + i); m.put("useHistory", uh);
        Sim t = newSim(); StateStore.apply(Json.parse(Json.write(m)), t); assertEquals(7, t.useHist.size());
    }
    @Test void minimalFileUsesDefaults() { Sim t = newSim(); t.tank = 10; List<String> w = StateStore.apply(Json.parse("{\"version\":1,\"plants\":[]}"), t); assertEquals(85, t.tank, 1e-9); assertTrue(w.get(0).contains("5 слотів") || w.size() >= 1); }
    @Test void loadedValuesDriveBehaviour() {
        customise(s); Sim t = newSim(); StateStore.apply(Json.parse(StateStore.toJson(s, false)), t);
        assertEquals(52, t.thr(0)); assertEquals(95 * 1.2, t.dose(3), 1e-9); assertEquals(Data.PLANTS[4].name, Data.PLANTS[t.species[1]].name);
    }
}
