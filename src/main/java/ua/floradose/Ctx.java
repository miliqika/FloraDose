package ua.floradose;

import javafx.stage.Stage;
import java.util.Set;

interface Ctx {
    Sim sim();
    int cfg();
    String selId();
    int selIdx();
    void select(String id, int idx);
    void repaint();
    void addSyncer(Runnable r);
    Stage stage();
    Set<String> hidden();
    boolean confirm(String text);
    ExportService export();
    Scene3D scene();
    boolean syncing();
    void setConfig(int i);
    void showSection(Section s);
    void startCycle();
    void startCycleStepNow();
    void startSlot(int i);
    void startAll();
    void restartCycle();
    void stopCycle(String msg);
    void togglePause();
    void stepOnce();
    void setSpeed(double v);
    void setStepMode(boolean b);
    boolean isPaused();
    void focusSelected();
    void toggleIsolate();
    boolean isolated();
}
