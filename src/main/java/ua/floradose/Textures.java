package ua.floradose;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.Random;

final class Textures {
    private Textures() { }

    private static Image snap(Canvas c, boolean transparent) { SnapshotParameters sp = new SnapshotParameters(); if (transparent) sp.setFill(Color.TRANSPARENT); return c.snapshot(sp, null); }
    private static void streaks(GraphicsContext g, Random r, int W, int H, int n, String dark, String light, double minLen, double maxLen, double ampMax) {
        for (int i = 0; i < n; i++) {
            double y = r.nextDouble() * H, w = 0.6 + r.nextDouble() * 1.8, amp = 0.5 + r.nextDouble() * ampMax, ph = r.nextDouble() * 6, fr = 0.002 + r.nextDouble() * 0.006;
            g.setStroke(r.nextInt(3) != 0 ? Color.web(dark, 0.06 + r.nextDouble() * 0.22) : Color.web(light, 0.08 + r.nextDouble() * 0.2)); g.setLineWidth(w);
            double x0 = r.nextDouble() * W * 0.7, len = minLen + r.nextDouble() * (maxLen - minLen); g.beginPath(); g.moveTo(x0, y);
            for (double x = x0; x < Math.min(W, x0 + len); x += 24) g.lineTo(x, y + Math.sin(x * fr + ph) * amp); g.stroke();
        }
    }

    static Image wood(int cfg) {
        int W = 1536, H = 288; Canvas c = new Canvas(W, H); GraphicsContext g = c.getGraphicsContext2D(); Random r = new Random(11 + cfg);
        String[] base = {"#dbc19b", "#e5cfa9", "#cdb38e"}; g.setFill(Color.web(base[cfg])); g.fillRect(0, 0, W, H);
        for (int b = 0; b < 40; b++) { g.setFill(Color.web(r.nextBoolean() ? "#b08a5e" : "#f3e1c0", 0.015 + r.nextDouble() * 0.03)); g.fillRect(0, r.nextDouble() * H, W, 2 + r.nextDouble() * 14); }
        streaks(g, r, W, H, 1500, "#9a7248", "#fbefd2", 200, 1100, 1.0);
        for (int i = 0; i < 3000; i++) { g.setFill(Color.web("#74502a", 0.04 + r.nextDouble() * 0.07)); g.fillRect(r.nextDouble() * W, r.nextDouble() * H, 4 + r.nextDouble() * 14, 1); }
        if (cfg != 1) for (int i = 0; i < 1200; i++) { g.setFill(cfg == 0 ? Color.web("#6b5a44", 0.1) : Color.web("#4f6b4a", 0.1)); g.fillRect(r.nextDouble() * W, r.nextDouble() * H, 2, 2); }
        return snap(c, false);
    }
    static Image table() {
        int W = 1024, H = 512; Canvas c = new Canvas(W, H); GraphicsContext g = c.getGraphicsContext2D(); Random r = new Random(5);
        g.setFill(Color.web("#e0c6a0")); g.fillRect(0, 0, W, H);
        for (int b = 0; b < 4; b++) { double y = b * H / 4.0; g.setFill(Color.web(b % 2 == 0 ? "#f1ddba" : "#c9a67a", 0.09)); g.fillRect(0, y, W, H / 4.0); g.setFill(Color.web("#8a6a45", 0.16)); g.fillRect(0, y, W, 1.2); g.setFill(Color.web("#fff0d8", 0.22)); g.fillRect(0, y + 1.2, W, 1); }
        streaks(g, r, W, H, 700, "#9a7248", "#fbeed4", 200, 900, 1.2);
        return snap(c, false);
    }
    static Image wall() {
        int W = 2048, H = 1024, rows = 26; Canvas c = new Canvas(W, H); GraphicsContext g = c.getGraphicsContext2D(); Random r = new Random(23);
        g.setFill(Color.web("#e4e0da")); g.fillRect(0, 0, W, H);
        double ph = H / (double) rows;
        for (int k = 0; k < rows; k++) {
            double y = k * ph; g.setFill(Color.web(r.nextBoolean() ? "#ffffff" : "#b9b2a6", 0.05 + r.nextDouble() * 0.09)); g.fillRect(0, y, W, ph);
            for (int i = 0; i < 40; i++) { double yy = y + 4 + r.nextDouble() * (ph - 8); g.setStroke(Color.web(r.nextBoolean() ? "#a89f92" : "#ffffff", 0.05 + r.nextDouble() * 0.1)); g.setLineWidth(0.6 + r.nextDouble()); g.strokeLine(r.nextDouble() * W * 0.6, yy, r.nextDouble() * W * 0.6 + W * 0.4, yy + (r.nextDouble() - 0.5) * 3); }
            g.setFill(Color.web("#8f877b", 0.55)); g.fillRect(0, y, W, 2.2); g.setFill(Color.web("#ffffff", 0.45)); g.fillRect(0, y + 2.2, W, 1.6);
            for (int j = 0; j < 2; j++) { g.setFill(Color.web("#8f877b", 0.3)); g.fillRect(r.nextDouble() * W, y, 2, ph); }
        }
        return snap(c, false);
    }
    static Image cloth() {
        int S = 512; Canvas c = new Canvas(S, S); GraphicsContext g = c.getGraphicsContext2D(); Random r = new Random(9);
        g.setFill(Color.web("#b4baa6")); g.fillRect(0, 0, S, S);
        for (int i = 0; i < S; i += 2) { g.setStroke(Color.web(r.nextBoolean() ? "#8c9480" : "#d3d8c6", 0.05 + r.nextDouble() * 0.06)); g.setLineWidth(1); g.strokeLine(0, i, S, i); g.strokeLine(i, 0, i, S); }
        return snap(c, false);
    }
    static Image moss() {
        int S = 256; Canvas c = new Canvas(S, S); GraphicsContext g = c.getGraphicsContext2D(); Random r = new Random(77);
        g.setFill(Color.web("#3f4a2b")); g.fillRect(0, 0, S, S);
        String[] cols = {"#5d7238", "#7b8f4a", "#35421f", "#4b5c2d", "#2b2218", "#a3ad74"};
        for (int i = 0; i < 90; i++) { g.setFill(Color.web(cols[r.nextInt(4)], 0.25)); double d = 14 + r.nextDouble() * 30; g.fillOval(r.nextDouble() * S, r.nextDouble() * S, d, d * 0.8); }
        for (int i = 0; i < 2200; i++) { g.setFill(Color.web(cols[r.nextInt(cols.length)], 0.35 + r.nextDouble() * 0.4)); double d = 1 + r.nextDouble() * 3; g.fillOval(r.nextDouble() * S, r.nextDouble() * S, d, d); }
        return snap(c, false);
    }
    static Image shadow() {
        Canvas c = new Canvas(256, 256); GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(new RadialGradient(0, 0, 128, 128, 128, false, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#000", 0.5)), new Stop(0.6, Color.web("#000", 0.25)), new Stop(1, Color.web("#000", 0))));
        g.fillRect(0, 0, 256, 256); return snap(c, true);
    }
    static Image logo() {
        Canvas c = new Canvas(900, 150); GraphicsContext g = c.getGraphicsContext2D();
        g.setTextAlign(TextAlignment.CENTER); g.setFill(Color.web("#6a4c34", 0.88)); g.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 104)); g.fillText("FloraDose", 450, 112); return snap(c, true);
    }
    static Image glowPlane() {
        Canvas c = new Canvas(256, 128); GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(new RadialGradient(0, 0, 128, 64, 128, false, CycleMethod.NO_CYCLE, new Stop(0, Color.color(1, 1, 1, 0.9)), new Stop(0.55, Color.color(1, 1, 1, 0.3)), new Stop(1, Color.color(1, 1, 1, 0))));
        g.save(); g.scale(1, 0.5); g.fillRect(0, 0, 256, 256); g.restore(); return snap(c, true);
    }
    static Image glow(Color base, double bright) {
        double W = 1024, H = 132, rad = 22; Canvas c = new Canvas(W, H); GraphicsContext g = c.getGraphicsContext2D();
        Color dull = Color.web("#c9bdb0"), lit = base.interpolate(Color.web("#ffd8b8"), 0.5), b = dull.interpolate(lit, bright), lt = b.interpolate(Color.WHITE, 0.3), dk = b.deriveColor(0, 1, 0.82, 1);
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, dk), new Stop(0.55, b), new Stop(1, lt)));
        g.fillRoundRect(2, 2, W - 4, H - 4, 2 * rad, 2 * rad);
        if (bright > 0.01) for (int i = 0; i < 8; i++) {
            double cx = 70 + i * (W - 140) / 7.0, cy = H * (0.56 + (i % 2 == 0 ? 0.04 : -0.04)), r1 = 95;
            g.setFill(new RadialGradient(0, 0, cx, cy, r1, false, CycleMethod.NO_CYCLE, new Stop(0, Color.color(1, 0.93, 0.78, 0.6 * bright)), new Stop(0.35, Color.color(lt.getRed(), lt.getGreen(), lt.getBlue(), 0.28 * bright)), new Stop(1, Color.color(b.getRed(), b.getGreen(), b.getBlue(), 0))));
            g.fillOval(cx - r1, cy - r1, 2 * r1, 2 * r1);
            g.setFill(new RadialGradient(0, 0, cx, cy, 16, false, CycleMethod.NO_CYCLE, new Stop(0, Color.color(1, 0.97, 0.9, 0.9 * bright)), new Stop(1, Color.color(1, 0.9, 0.7, 0))));
            g.fillOval(cx - 16, cy - 16, 32, 32);
        }
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#3a2410", 0.38)), new Stop(0.28, Color.web("#3a2410", 0))));
        g.fillRoundRect(2, 2, W - 4, H - 4, 2 * rad, 2 * rad); return snap(c, true);
    }
}
