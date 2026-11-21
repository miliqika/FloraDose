package ua.floradose;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DataTest {
    @Test void tankLevelStartsAtBottomAndEndsAtColumnTop() {
        assertEquals(0, Data.waterLevelCm(0), 1e-9); assertEquals(Data.COL_H, Data.waterLevelCm(100), 1e-9);
        assertEquals(0, Data.waterLevelCm(-20), 1e-9); assertEquals(Data.COL_H, Data.waterLevelCm(250), 1e-9);
    }
    @Test void tankLevelIsMonotonicAndContinuous() {
        double prev = -1; for (int p = 0; p <= 100; p++) { double h = Data.waterLevelCm(p); assertTrue(h >= prev - 1e-12, "спадає на " + p + " %"); if (p > 0) assertTrue(h - prev < 0.6, "стрибок на " + p + " %"); prev = h; }
        double ac = Data.COL_W * Data.COL_D, ab = Data.CH_L * Data.CH_D, pctAtBase = (ac + ab) * Data.CH_H / Data.TANK_ML * 100;
        assertEquals(Data.CH_H, Data.waterLevelCm(pctAtBase), 1e-9, "рівень рівно на висоті основи, коли вона повна");
        assertTrue(Data.waterLevelCm(85) > Data.CH_H && Data.waterLevelCm(85) < Data.COL_H);
    }
    @Test void tankVolumeIsConsistent() { assertEquals(Data.COL_W * Data.COL_D * Data.COL_H + Data.CH_L * Data.CH_D * Data.CH_H, Data.TANK_ML, 1e-9); assertEquals(Data.TANK_ML / 1000, Data.TANK_L, 1e-12); assertTrue(Data.TANK_DIM.contains("л")); }
    @Test void plantProfilesAreSelfConsistent() {
        assertEquals(5, Data.PLANTS.length);
        for (Data.Plant p : Data.PLANTS) {
            String n = p.name; assertFalse(n.isEmpty()); assertTrue(p.thr >= 5 && p.thr <= 95, n); assertTrue(p.dose >= 10 && p.dose <= 200, n); assertTrue(p.dry > 0, n);
            assertTrue(p.wetMax > p.thr && p.wetMax <= 100, n + ": верхня межа вологості вища за поріг"); assertTrue(p.tMin < p.tMax, n); assertTrue(p.luxMin < p.luxMax, n);
            assertTrue(p.soil >= 0 && p.soil < Data.SOILS.length, n); assertFalse(p.latin.isEmpty()); assertFalse(p.ranges().isEmpty()); assertFalse(p.tip.isEmpty());
        }
    }
    @Test void derivedArraysMatchProfiles() {
        assertEquals(Data.PLANTS.length, Data.SPECIES.length); assertEquals(Data.PLANTS.length, Data.THR.length); assertEquals(Data.PLANTS.length, Data.DOSE.length); assertEquals(Data.PLANTS.length, Data.DRY.length);
        for (int i = 0; i < Data.PLANTS.length; i++) { assertEquals(Data.PLANTS[i].name, Data.SPECIES[i]); assertEquals(Data.PLANTS[i].thr, Data.THR[i]); assertEquals(Data.PLANTS[i].dose, Data.DOSE[i], 1e-9); assertEquals(Data.PLANTS[i].dry, Data.DRY[i], 1e-9); }
    }
    @Test void succulentsNeedLessWaterThanLeafyPlants() {
        double echeveria = Data.PLANTS[1].thr, ficus = Data.PLANTS[4].thr; assertTrue(echeveria < ficus); assertTrue(Data.PLANTS[1].dose < Data.PLANTS[4].dose); assertTrue(Data.PLANTS[1].dry < Data.PLANTS[4].dry);
    }
    @Test void soilProfiles() {
        assertEquals(4, Data.SOILS.length); for (Data.Soil so : Data.SOILS) { assertTrue(so.dryMul > 0.3 && so.dryMul < 3, so.name); assertTrue(so.initMoist >= 0 && so.initMoist <= 100, so.name); assertFalse(so.note.isEmpty()); }
        assertTrue(Data.SOILS[1].dryMul > Data.SOILS[0].dryMul, "мінеральний ґрунт для сукулентів сохне швидше за універсальний"); assertTrue(Data.SOILS[2].dryMul < Data.SOILS[0].dryMul, "торфовий тримає воду довше");
    }
    @Test void riskAndErrorReferencesAreComplete() { assertEquals(6, Data.RISK.length); for (String r : Data.RISK) assertNotNull(r); assertEquals(7, Data.ERR.length); for (Data.ErrorSpec e : Data.ERR) { assertFalse(e.code.isEmpty()); assertFalse(e.action.isEmpty()); } }
    @Test void everyComponentHasThreeConfigurations() { assertEquals(3, Data.CFG.length); for (Data.C c : Data.COMPS.values()) for (int cfg = 0; cfg < 3; cfg++) { assertTrue(Data.qty(c, cfg) >= 0, c.id); assertNotNull(Data.mat(c, cfg), c.id); } }
}
