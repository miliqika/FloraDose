package ua.floradose;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

final class SimulationController {
    private final Ctx c; private final Sim sim; private final PauseTransition pt = new PauseTransition(Duration.millis(500));
    private boolean paused, stepMode; private double speed = 1, lastDelay = 500, histAcc, repAcc; private final Runnable pauseUi; private AnimationTimer timer; private final Runnable perFrame;
    SimulationController(Ctx c, Sim sim, Runnable pauseUi, Runnable perFrame) { this.c = c; this.sim = sim; this.pauseUi = pauseUi; this.perFrame = perFrame; pt.setOnFinished(e -> runStep()); }
    void setSpeed(double v) { speed = v; } void setStepMode(boolean b) { stepMode = b; } boolean paused() { return paused; }
    void start() { if (sim.running()) return; paused = false; pauseUi.run(); sim.begin(); runStep(); }
    void startNow() { pt.stop(); runStep(); }
    void startSlot(int i) { if (sim.running()) return; paused = false; pauseUi.run(); sim.beginSingle(i); runStep(); }
    void startAll() { if (sim.running()) return; paused = false; pauseUi.run(); sim.beginAll(); runStep(); }
    void restart() { pt.stop(); paused = false; pauseUi.run(); sim.stop("Перезапуск"); sim.begin(); runStep(); }
    void stop(String msg) { pt.stop(); paused = false; pauseUi.run(); sim.stop(msg); c.repaint(); }
    void stepOnce() { if (!sim.running()) sim.begin(); long d = sim.step(); if (d >= 0) lastDelay = d; c.repaint(); }
    void togglePause() {
        if (!sim.running()) return; paused = !paused; pauseUi.run();
        if (paused) pt.stop(); else { pt.setDuration(Duration.millis(Math.max(30, lastDelay / speed))); pt.playFromStart(); }
    }
    private void runStep() {
        long d = sim.step(); if (d >= 0) lastDelay = d; c.repaint();
        if (d < 0 || stepMode || paused) return;
        pt.setDuration(Duration.millis(Math.max(30, d / speed))); pt.playFromStart();
    }
    void startTimer() {
        timer = new AnimationTimer() {
            long last;
            @Override public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = Math.min(0.1, (now - last) / 1e9); last = now;
                sim.advance(dt); sim.tickDry(dt); if (sim.autoDue(dt)) start(); sim.tickManual(dt); c.scene().tick(now / 1e9, sim.flow); perFrame.run();
                histAcc += dt; if (histAcc > 0.5) { histAcc = 0; sim.sample(); }
                repAcc += dt; boolean active = sim.anyManual() || sim.running() || sim.drying || sim.onBattery; if ((active && repAcc > 0.25) || repAcc > 1.0) { repAcc = 0; c.repaint(); }
            }
        };
        timer.start();
    }
    void shutdown() { pt.stop(); if (timer != null) timer.stop(); }
}
