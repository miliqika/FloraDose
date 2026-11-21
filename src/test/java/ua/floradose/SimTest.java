package ua.floradose;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimTest {
    Sim s;
    @BeforeEach void init() {
        s = new Sim(new Sim.Hook() { public void event(Sim.Ev e) { } public void disp(String a, String b, Color c) { } });
        java.util.Arrays.fill(s.moist, 80);
    }
    private void only(int slot, double moist) { java.util.Arrays.fill(s.sel, false); s.sel[slot] = true; s.moist[slot] = moist; }
    private void stepUntilPhase(int p) { int n = 0; while (s.phase != p && n++ < 500) s.step(); }

    @Test void configRulesMatchSpecificationCounts() {
        for (Data.C c : Data.COMPS.values()) if (c.qty == 5) for (int cfg = 0; cfg < 3; cfg++) {
            int visible = 0; for (int i = 0; i < 5; i++) if (Data.instanceVisible(c.id, i, cfg)) visible++;
            assertEquals(Data.qty(c, cfg), visible, c.id + " у конфігурації " + Data.CFG[cfg]);
        }
    }
    @Test void schematicNodesExistInSpecification() { for (String id : Schematic.N.keySet()) assertTrue(Data.COMPS.containsKey(id), id); for (String[] e : Schematic.E) { assertTrue(Schematic.N.containsKey(e[0]) && Schematic.N.containsKey(e[1])); assertTrue(Schematic.route(e[0], e[1], 0).size() >= 2); } }
    @Test void flowOnlyWithOpenValve() {
        only(0, 10); s.begin(); int n = 0; while (s.step() >= 0 && n++ < 400) for (int i = 0; i < 5; i++) if (s.flow[i]) assertEquals(1, s.vs[i], "потік лише при відкритому клапані");
    }
    @Test void emptyTankNeverWatersNormally() { only(0, 10); s.tank = 0; s.runToEnd(); assertEquals(0, s.actual[0], 1e-9); assertEquals(1, s.lastErr); }
    @Test void naturalDryingLowersMoistureWithProfileRates() {
        java.util.Arrays.fill(s.moist, 80); s.drying = true; s.species[0] = 4; s.species[1] = 1; s.tickDry(10); assertTrue(s.moist[0] < 80 && s.moist[1] < 80); assertTrue(80 - s.moist[0] > 80 - s.moist[1], "широколиста сохне швидше за сукулент");
        s.drying = false; double m = s.moist[2]; s.tickDry(10); assertEquals(m, s.moist[2], 1e-9);
    }
    @Test void autoStartChecksTankSensorAndValve() {
        s.autoStart = true; s.moist[0] = 5; s.species[0] = 0; java.util.Arrays.fill(s.sel, false); s.sel[0] = true; s.autoCool = 0; assertTrue(s.autoDue(0.1));
        init(); s.autoStart = true; s.moist[0] = 5; java.util.Arrays.fill(s.sel, false); s.sel[0] = true; s.tank = 2; s.autoCool = 0; assertFalse(s.autoDue(0.1));
        init(); s.autoStart = true; s.moist[0] = 5; java.util.Arrays.fill(s.sel, false); s.sel[0] = true; s.fSensor = 0; s.autoCool = 0; assertFalse(s.autoDue(0.1));
        init(); s.autoStart = true; s.moist[0] = 5; java.util.Arrays.fill(s.sel, false); s.sel[0] = true; s.vs[0] = 2; s.autoCool = 0; assertFalse(s.autoDue(0.1));
    }
    @Test void singleSlotCycleWatersOnlyThatSlotAndRestoresSelection() {
        java.util.Arrays.fill(s.moist, 10); boolean[] before = s.sel.clone(); s.beginSingle(2); int n = 0; while (s.step() >= 0 && n++ < 400) { }
        assertTrue(s.actual[2] > 0); for (int i : new int[]{0, 1, 3, 4}) assertEquals(0, s.actual[i], 1e-9); assertArrayEquals(before, s.sel);
    }
    @Test void errorLogCarriesAdviceAndWateringIsLogged() {
        only(0, 10); s.tank = 2; s.runToEnd(); assertFalse(s.errors.isEmpty()); assertEquals("E1", s.errors.get(0).code); assertTrue(s.errors.get(0).action.contains("воду"));
        init(); only(1, 10); s.runToEnd(); assertEquals(1, s.waterings.size()); assertTrue(s.waterUsed[1] > 0);
    }
    @Test void timeToNextWateringAndPowerRestore() {
        s.moist[0] = 80; s.species[0] = 0; double h = s.hoursToNext(0); assertTrue(h > 0); s.moist[0] = 10; assertEquals(0, s.hoursToNext(0), 1e-9);
        only(0, 10); s.begin(); stepUntilPhase(6); s.powerLoss = true; s.step(); assertTrue(s.onBattery); assertEquals(2, s.overall()); s.restorePower(); assertFalse(s.onBattery);
    }
    @Test void historyAndClockAdvance() { s.advance(2); assertTrue(s.hours > 0); s.sample(); s.sample(); assertEquals(2, s.history.size()); }
    @Test void typedErrorSpecs() { assertEquals(7, Data.ERR.length); assertEquals("E1", Data.ERR[0].code); assertFalse(Data.ERR[2].reaction.isEmpty()); }
    @Test void bodyDimensionsFollowConcept() { assertTrue(Data.COMPS.get("shell").dim.startsWith("580×160×140"), Data.COMPS.get("shell").dim); }
    @Test void tankVolumeWithinSpecification() { assertTrue(Data.TANK_L >= 3.0 && Data.TANK_L <= 5.0, "об’єм " + Data.TANK_L); }
    @Test void configurationQuantities() {
        Data.C light = Data.COMPS.get("light"), mic = Data.COMPS.get("mic");
        assertEquals(1, Data.qty(light, 0)); assertEquals(5, Data.qty(light, 1)); assertEquals(5, Data.qty(light, 2));
        assertEquals(0, Data.qty(mic, 0)); assertEquals(5, Data.qty(mic, 1));
        for (String id : new String[]{"shell", "tank", "panel", "slot"}) for (int c = 0; c < 3; c++) assertFalse(Data.mat(Data.COMPS.get(id), c).isEmpty());
        assertNotEquals(Data.mat(Data.COMPS.get("shell"), 0), Data.mat(Data.COMPS.get("shell"), 1));
    }
    @Test void speciesProfilesConsistent() { assertEquals(Data.SPECIES.length, Data.THR.length); assertEquals(Data.SPECIES.length, Data.DOSE.length); assertEquals(7, Data.ERR.length); }
    @Test void normalWateringUpdatesTankMoistureAndWeight() {
        only(0, 20); s.species[0] = 2; double tank0 = s.tank, w0 = s.weight(0), dose = s.dose(0); s.runToEnd();
        assertEquals(dose, s.actual[0], 1.0); assertEquals(20 + dose / Sim.ML_PER_PCT, s.moist[0], 0.5);
        assertEquals(tank0 - dose / (s.capL * 10), s.tank, 0.05); assertEquals(w0 + dose, s.weight(0), 3.0); assertEquals(0, s.vs[0]); assertEquals(0, s.lastErr);
    }
    @Test void noWateringWhenMoistureAboveThreshold() { only(1, 80); double t = s.tank; s.runToEnd(); assertEquals(0, s.actual[1], 1e-9); assertEquals(t, s.tank, 1e-9); }
    @Test void doseCorrectionScalesDose() { s.species[0] = 0; double d = s.dose(0); s.doseCorr = 150; assertEquals(d * 1.5, s.dose(0), 1e-9); }
    @Test void lowWaterBlocksPump() { only(0, 10); s.tank = 3; s.runToEnd(); assertEquals(1, s.lastErr); assertEquals(0, s.actual[0], 1e-9); assertTrue(s.pumpBlocked); }
    @Test void valveFaultStopsWithoutFlow() { only(2, 10); s.fValve = 2; double t = s.tank; s.runToEnd(); assertEquals(3, s.lastErr); assertEquals(2, s.vs[2]); assertEquals(t, s.tank, 1e-9); }
    @Test void sensorFaultAndCommLoss() {
        only(3, 10); s.fSensor = 3; s.runToEnd(); assertEquals(2, s.lastErr); assertEquals(0, s.actual[3], 1e-9);
        init(); only(4, 10); s.fComm = 4; s.runToEnd(); assertEquals(4, s.lastErr); assertEquals(0, s.actual[4], 1e-9);
    }
    @Test void interruptGivesPartialWatering() {
        only(0, 10); s.begin(); stepUntilPhase(6); for (int k = 0; k < 4; k++) s.step(); s.interrupt = true; int n = 0; while (s.step() >= 0 && n++ < 50) { }
        assertEquals(5, s.lastErr); assertTrue(s.actual[0] > 0 && s.actual[0] < s.dose(0)); assertEquals(0, s.vs[0]);
    }
    @Test void powerLossClosesEverything() {
        only(0, 10); s.begin(); stepUntilPhase(6); s.powerLoss = true; s.step();
        assertEquals(6, s.lastErr); assertFalse(s.running()); for (int i = 0; i < 5; i++) { assertFalse(s.flow[i]); assertNotEquals(1, s.vs[i]); }
    }
    @Test void overwateringPausesAutomaticWatering() { only(0, 95); s.runToEnd(); assertEquals(7, s.lastErr); assertEquals(0, s.actual[0], 1e-9); }
    @Test void ledThresholds() { s.tank = 40; assertEquals(0, s.ledState()); s.tank = 39; assertEquals(1, s.ledState()); s.tank = 15; assertEquals(1, s.ledState()); s.tank = 14; assertEquals(2, s.ledState()); }
    @Test void forecastUsesHistoryWhenAvailable() {
        java.util.Arrays.fill(s.sel, true); double byPlan = s.daysLeft(); assertEquals(s.tankMl() / s.dailyUse(), byPlan, 1e-9);
        s.useHist.add(100.0); s.useHist.add(300.0); assertEquals(200.0, s.dailyUse(), 1e-9); assertEquals(s.tankMl() / 200.0, s.daysLeft(), 1e-9);
    }
    @Test void manualFlowConsumesWater() { double t = s.tank; s.toggleValve(1); s.tickManual(2.0); assertEquals(t - 2 * Sim.RATE_ML_S / (s.capL * 10), s.tank, 1e-9); s.toggleValve(1); assertEquals(0, s.vs[1]); }
    @Test void restartResetsState() { only(0, 10); s.fValve = 0; s.runToEnd(); s.fValve = -1; s.moist[0] = 10; s.runToEnd(); assertEquals(0, s.lastErr); assertTrue(s.actual[0] > 0); }
}
