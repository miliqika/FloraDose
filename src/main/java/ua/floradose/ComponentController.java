package ua.floradose;

final class ComponentController {
    private final Ctx c; private final InfoPanel info; private final SpecPanel spec; private final WaterPanel water; private final ModelController model;
    private String selId = "shell"; private int selIdx = -1; private boolean isolated;
    private static final java.util.Set<String> SLOT_IDS = new java.util.HashSet<>(java.util.Arrays.asList("slot", "electrode", "light", "ntc", "mic", "loadcell", "slotboard", "valve", "cable"));
    ComponentController(Ctx c, InfoPanel info, SpecPanel spec, WaterPanel water, ModelController model) { this.c = c; this.info = info; this.spec = spec; this.water = water; this.model = model; }
    String id() { return selId; } int idx() { return selIdx; } boolean isolated() { return isolated; }
    void select(String id, int idx) {
        selId = id; selIdx = Data.instanceVisible(id, idx < 0 ? 0 : idx, c.cfg()) ? idx : -1;
        if (isolated) c.scene().isolate(selId, selIdx);
        info.update(); info.selectInTree(selId, selIdx); spec.selectRow(selId); if (SLOT_IDS.contains(id) && selIdx >= 0) water.showSlot(selIdx);
        c.repaint();
    }
    void reselect() { selIdx = Data.instanceVisible(selId, selIdx < 0 ? 0 : selIdx, c.cfg()) ? selIdx : -1; info.update(); info.selectInTree(selId, selIdx); }
    void toggleIsolate() { isolated = !isolated; if (isolated) { Data.C d = Data.COMPS.get(selId); if (d != null && d.internal) model.showInternal(); c.scene().isolate(selId, selIdx); } else c.scene().isolate(null, -1); info.update(); c.repaint(); }
    void focusSelected() {
        Scene3D.Part p = c.scene().find(selId, selIdx); if (p == null) return;
        Data.C d = Data.COMPS.get(selId); if (d != null && d.internal) model.showInternal();
        c.repaint(); c.scene().focus(p);
    }
}
