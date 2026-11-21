package ua.floradose;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SimFeaturesTest {
    Sim s; List<Sim.Ev> events;
    @BeforeEach void init() { events = new ArrayList<>(); s = new Sim(new Sim.Hook() { public void event(Sim.Ev e) { events.add(e); } public void disp(String a, String b, Color c) { } }); }
    private void only(int slot, double moist) { Arrays.fill(s.sel, false); s.sel[slot] = true; s.moist[slot] = moist; }

    @Test void changingPlantResetsIndividualSettingsAndSoil() {
        s.setThreshold(1, 60); s.setDoseOverride(1, 150); s.setSoil(1, 2, 40); s.setSpecies(1, 4);
        assertEquals(4, s.species[1]); assertEquals(-1, s.thrOv[1]); assertEquals(-1, s.doseOv[1]); assertEquals(Data.PLANTS[4].soil, s.soil[1]); assertEquals(Data.PLANTS[4].thr, s.thr(1)); assertEquals(Data.PLANTS[4].dose, s.dose(1), 1e-9);
    }
    @Test void choosingTheSamePlantChangesNothingAndLogsNothing() { s.setThreshold(0, 70); int n = events.size(); s.setSpecies(0, s.species[0]); assertEquals(n, events.size()); assertEquals(70, s.thr(0)); }
    @Test void changesAreLogged() { s.setSpecies(2, 1); assertEquals(1, events.size()); assertEquals(3, events.get(0).slot); assertTrue(events.get(0).text.contains("рослину змінено")); }
    @Test void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> s.setSpecies(5, 0)); assertThrows(IllegalArgumentException.class, () -> s.setSpecies(-1, 0)); assertThrows(IllegalArgumentException.class, () -> s.setSpecies(0, 99));
        assertThrows(IllegalArgumentException.class, () -> s.setSoil(0, 9, 10)); assertThrows(IllegalArgumentException.class, () -> s.setThreshold(7, 10)); assertThrows(IllegalArgumentException.class, () -> s.setDoseOverride(-2, 10));
        assertThrows(IllegalArgumentException.class, () -> s.setLedMode(3)); assertThrows(IllegalArgumentException.class, () -> s.scenario(99)); assertThrows(IllegalArgumentException.class, () -> s.scenario(-1));
    }
    @Test void thresholdAndDoseAreClampedAndResettable() {
        s.setThreshold(0, 2); assertEquals(5, s.thr(0)); s.setThreshold(0, 500); assertEquals(95, s.thr(0)); s.setThreshold(0, -1); assertEquals(Data.PLANTS[0].thr, s.thr(0));
        s.setDoseOverride(0, 1); assertEquals(10, s.dose(0), 1e-9); s.setDoseOverride(0, 9999); assertEquals(200, s.dose(0), 1e-9); s.setDoseOverride(0, -5); assertEquals(Data.PLANTS[0].dose, s.dose(0), 1e-9);
        s.setDoseOverride(0, 100); s.doseCorr = 110; assertEquals(110, s.dose(0), 1e-9);
    }
    @Test void individualThresholdChangesTheWateringDecision() {
        only(0, 60); s.runToEnd(); assertEquals(0, s.actual[0], 1e-9, "60 % вище порогу профілю 45 %");
        init(); only(0, 60); s.setThreshold(0, 70); s.runToEnd(); assertTrue(s.actual[0] > 0, "60 % нижче індивідуального порогу 70 %");
    }
    @Test void individualDoseIsWhatTheCycleDelivers() { only(0, 10); s.setDoseOverride(0, 100); s.runToEnd(); assertEquals(100, s.actual[0], 1.0); }
    @Test void soilSetsInitialMoistureAndDryingSpeed() {
        s.setSoil(0, 2, -1); assertEquals(Data.SOILS[2].initMoist, s.moist[0], 1e-9); s.setSoil(0, 1, 33); assertEquals(33, s.moist[0], 1e-9); s.setSoil(0, 1, 250); assertEquals(100, s.moist[0], 1e-9);
        s.species[0] = s.species[1] = 0; s.soil[0] = 1; s.soil[1] = 2; assertTrue(s.dryRate(0) > s.dryRate(1), "мінеральний ґрунт сохне швидше за торфовий");
    }
    @Test void hotAndBrightAirDriesFaster() { double a = s.dryRate(0); s.tempAmb = 32; double b = s.dryRate(0); s.lux = 900; double c = s.dryRate(0); assertTrue(a < b && b < c); }

    @Test void refillRaisesLevelClearsBlockAndNeverLowers() {
        only(0, 10); s.tank = 3; s.runToEnd(); assertEquals(1, s.lastErr); assertTrue(s.pumpBlocked);
        double added = s.refill(100); assertEquals(100, s.tank, 1e-9); assertEquals((100 - 3) * s.capL * 10, added, 1e-6); assertFalse(s.pumpBlocked); assertEquals(0, s.lastErr);
        assertEquals(0, s.refill(50), 1e-9); assertEquals(100, s.tank, 1e-9); assertEquals(100, s.tank); s.refill(Double.NaN); assertEquals(100, s.tank, 1e-9);
    }
    @Test void smallTopUpDoesNotClearTheBlockBelowFivePercent() { only(0, 10); s.tank = 1; s.runToEnd(); assertTrue(s.pumpBlocked); s.refill(3); assertTrue(s.pumpBlocked); s.refill(20); assertFalse(s.pumpBlocked); }
    @Test void pumpFaultStopsWateringAndCanBeCleared() {
        only(0, 10); s.setPumpFault(true); double t = s.tank; s.runToEnd();
        assertEquals(3, s.lastErr); assertEquals(t, s.tank, 1e-9); assertEquals(0, s.actual[0], 1e-9); assertTrue(s.pumpBad); assertTrue(s.issues().contains("Помпа не створює тиск")); assertEquals(2, s.overall());
        s.setPumpFault(false); assertFalse(s.pumpBad); only(0, 10); s.runToEnd(); assertTrue(s.actual[0] > 0); assertEquals(0, s.lastErr);
    }
    @Test void pumpFaultBlocksAutoStart() { s.autoStart = true; only(0, 5); s.autoCool = 0; s.setPumpFault(true); only(0, 10); s.runToEnd(); s.autoCool = 0; assertFalse(s.autoDue(0.1)); }
    @Test void manualModeNeverStartsByItself() { s.autoStart = false; only(0, 5); s.autoCool = 0; assertFalse(s.autoDue(0.1)); s.autoStart = true; assertTrue(s.autoDue(0.1)); }

    @Test void ledModesCycleAndAffectColourAndLevel() {
        assertEquals(0, s.ledMode); assertEquals(1.0, s.ledLevel(), 1e-9); assertEquals(Sim.LED_MODES[0], s.ledModeName());
        s.tank = 10; assertEquals(s.led(), s.ledColor(), "АВТО: колір за рівнем води"); s.cycleLedMode(); assertEquals(1, s.ledMode); assertEquals(Color.web("#ffb274"), s.ledColor(), "УВІМК: тепле світло незалежно від води"); assertEquals(1.0, s.ledLevel(), 1e-9);
        s.cycleLedMode(); assertEquals(2, s.ledMode); assertEquals(0.0, s.ledLevel(), 1e-9); s.cycleLedMode(); assertEquals(0, s.ledMode);
    }
    @Test void ledStateFollowsTankInAutoMode() { s.tank = 85; assertEquals(Color.web("#ffb274"), s.ledColor()); s.tank = 30; assertEquals(Color.web("#ffd24a"), s.ledColor()); s.tank = 5; assertEquals(Color.web("#ff3b30"), s.ledColor()); }

    @Test void defaultsNeedWaterInThreeSlotsAndForecastPointsAtTheFirst() {
        assertEquals(3, s.needWater()); assertEquals(0, s.nextSlot()); assertTrue(s.nextWateringText().startsWith("S1")); assertTrue(s.nextWateringText().contains("зараз"));
    }
    @Test void forecastNamesTheSlotThatDriesFirst() {
        Arrays.fill(s.moist, 90); s.moist[3] = s.thr(3) + 1; assertEquals(0, s.needWater()); assertEquals(3, s.nextSlot()); assertTrue(s.nextWateringText().startsWith("S4")); assertTrue(s.nextWateringText().contains("через"));
        Arrays.fill(s.sel, false); assertEquals(-1, s.nextSlot()); assertEquals("немає слотів у циклі", s.nextWateringText());
    }
    @Test void usedTodayCoversOnlyTheLast24ModelHours() {
        only(0, 10); s.runToEnd(); assertTrue(s.usedToday() > 0); s.advance(150); assertEquals(0, s.usedToday(), 1e-9, "25 год модельного часу потому");
    }
    @Test void diagnosticsListEveryProblem() {
        Arrays.fill(s.moist, 50); assertTrue(s.issues().isEmpty(), s.issues().toString()); assertEquals("Усе в нормі", s.diagnosticsText());
        s.tank = 3; s.vs[2] = 2; s.bad[1] = 1; String t = s.diagnosticsText(); assertTrue(t.contains("порожній")); assertTrue(t.contains("V3")); assertTrue(t.contains("S2"));
        s.tank = 12; assertTrue(s.issues().contains("Низький рівень води — долийте воду"));
    }
    @Test void classificationUsesPlantProfile() {
        Arrays.fill(s.moist, 50); s.moist[1] = 85; assertEquals(2, s.classify(1), "ехеверія: перезволоження"); assertEquals(0, s.classify(4)); s.moist[4] = 5; assertEquals(1, s.classify(4), "фікус: посуха");
        Arrays.fill(s.moist, 50); s.tempAmb = 33; assertEquals(4, s.classify(0), "драцена: перегрів понад 30 °C"); s.tempAmb = 22; s.lux = 50; assertEquals(3, s.classify(1), "дефіцит світла");
    }

    @Test void scenariosSetTheExpectedConditions() {
        Arrays.fill(s.moist, 50); s.scenario(1); assertEquals(34, s.tempAmb, 1e-9); assertTrue(s.drying); for (int i = 0; i < 5; i++) assertTrue(s.moist[i] <= s.thr(i) + 8.0 + 1e-9);
        s.scenario(2); assertEquals(70, s.lux, 1e-9); s.scenario(3); assertEquals(12, s.tank, 1e-9); s.scenario(4); assertEquals(1, s.species[0]); assertEquals(2, s.classify(0));
    }
    @Test void normalScenarioRestoresConditionsAndClearsFaults() {
        s.scenario(1); s.scenario(3); s.fSensor = 2; s.fValve = 1; s.fComm = 0; s.setPumpFault(true); s.lastErr = 4; s.scenario(0);
        assertEquals(85, s.tank, 1e-9); assertEquals(420, s.lux, 1e-9); assertEquals(22, s.tempAmb, 1e-9); assertFalse(s.drying); assertEquals(-1, s.fSensor); assertEquals(-1, s.fValve); assertEquals(-1, s.fComm); assertFalse(s.fPump); assertEquals(0, s.lastErr);
        assertArrayEquals(Sim.DEFAULT_MOIST, s.moist, 1e-9);
    }
    @Test void everyScenarioHasAName() { assertEquals(5, Sim.SCENARIOS.length); for (String n : Sim.SCENARIOS) assertFalse(n.isEmpty()); }
    @Test void resetToDefaultsRestoresSlotsAndSettings() {
        s.setSpecies(0, 3); s.setThreshold(1, 80); s.setDoseOverride(2, 150); s.setSoil(3, 3, 10); s.sel[4] = false; s.autoStart = true; s.setLedMode(2); s.tank = 20; s.timeScale = 1800; s.drying = true; s.useHist.add(100.0);
        only(1, 10); s.runToEnd(); s.resetToDefaults();
        for (int i = 0; i < 5; i++) { assertEquals(i, s.species[i]); assertEquals(Data.PLANTS[i].soil, s.soil[i]); assertEquals(-1, s.thrOv[i]); assertEquals(-1, s.doseOv[i]); assertTrue(s.sel[i]); assertEquals(0, s.waterUsed[i], 1e-9); }
        assertFalse(s.autoStart); assertEquals(0, s.ledMode); assertEquals(85, s.tank, 1e-9); assertEquals(600, s.timeScale, 1e-9); assertFalse(s.drying); assertTrue(s.useHist.isEmpty() && s.waterings.isEmpty() && s.errors.isEmpty()); assertEquals(0, s.hours, 1e-9);
        assertArrayEquals(Sim.DEFAULT_MOIST, s.moist, 1e-9);
    }
    @Test void resetIsRefusedWhileACycleRuns() { only(0, 10); s.begin(); assertTrue(s.running()); assertThrows(IllegalStateException.class, s::resetToDefaults); }
    @Test void sanitizeRepairsEveryOutOfRangeValueAndIsIdempotent() {
        s.species[0] = 9; s.soil[1] = -3; s.thrOv[2] = 2; s.doseOv[3] = 500; s.moist[4] = Double.NaN; s.tank = 500; s.lux = -5; s.tempAmb = 99; s.doseCorr = 0; s.timeScale = 1; s.ledMode = 5;
        List<String> w = s.sanitize(); assertTrue(w.size() >= 6, w.toString());
        assertEquals(0, s.species[0]); assertEquals(Data.PLANTS[1].soil, s.soil[1]); assertEquals(-1, s.thrOv[2]); assertEquals(-1, s.doseOv[3]); assertEquals(Sim.DEFAULT_MOIST[4], s.moist[4], 1e-9);
        assertEquals(100, s.tank, 1e-9); assertEquals(0, s.lux, 1e-9); assertEquals(40, s.tempAmb, 1e-9); assertEquals(50, s.doseCorr, 1e-9); assertEquals(60, s.timeScale, 1e-9); assertEquals(0, s.ledMode);
        assertTrue(s.sanitize().isEmpty(), "повторна перевірка нічого не знаходить");
    }
    @Test void clampHandlesNonFiniteNumbers() { assertEquals(7, Sim.clamp(Double.NaN, 0, 10, 7)); assertEquals(7, Sim.clamp(Double.POSITIVE_INFINITY, 0, 10, 7)); assertEquals(10, Sim.clamp(11, 0, 10, 7)); assertEquals(0, Sim.clamp(-1, 0, 10, 7)); }
}
