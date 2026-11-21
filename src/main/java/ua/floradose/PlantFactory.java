package ua.floradose;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

final class PlantFactory {
    static final int SPECIES = 5;
    static final double[] SCALE = {1.45, 1.3, 1.3, 1.3, 1.3};
    static final String[] FORM = {"гострий пучок вузького листя", "розетка м’ясистих листків", "гіллясті стебла з лопатевим листям", "пучок товстого листя", "гілки з широким листям"};

    static final class Group {
        final String color; final double specular, power; final MeshBuilder mesh = new MeshBuilder();
        Group(String color, double specular, double power) { this.color = color; this.specular = specular; this.power = power; }
    }
    private final Map<String, Group> groups = new LinkedHashMap<>(); private final double spec, power;
    private PlantFactory(double spec, double power) { this.spec = spec; this.power = power; }
    private MeshBuilder mesh(String color) { return groups.computeIfAbsent(color, c -> new Group(c, spec, power)).mesh; }

    static List<Group> build(int species, long seed, double x, double y, double z) {
        if (species < 0 || species >= SPECIES) throw new IllegalArgumentException("Невідомий вид рослини: " + species);
        double[][] sp = {{0.15, 16}, {0.22, 22}, {0.06, 10}, {0.35, 30}, {0.45, 36}};
        PlantFactory f = new PlantFactory(sp[species][0], sp[species][1]); Random r = new Random(seed * 7919 + species);
        switch (species) {
            case 0: f.dracaena(r, x, y, z); break;
            case 1: f.echeveria(r, x, y, z); break;
            case 2: f.pelargonium(r, x, y, z); break;
            case 3: f.crassula(r, x, y, z); break;
            default: f.broadleaf(r, x, y, z); break;
        }
        for (Group g : f.groups.values()) g.mesh.scaleAbout(SCALE[species], x, y, z);
        return new ArrayList<>(f.groups.values());
    }
    static double[] bounds(List<Group> gs) {
        double[] r = {1e18, 1e18, 1e18, -1e18, -1e18, -1e18};
        for (Group g : gs) { if (g.mesh.isEmpty()) continue; double[] b = g.mesh.bounds(); for (int k = 0; k < 3; k++) { r[k] = Math.min(r[k], b[k]); r[3 + k] = Math.max(r[3 + k], b[3 + k]); } }
        return r;
    }
    static int triangles(List<Group> gs) { int n = 0; for (Group g : gs) n += g.mesh.triangleCount(); return n; }

    private static double pick(Random r, double lo, double hi) { return lo + (hi - lo) * r.nextDouble(); }
    private static String choose(Random r, String... c) { return c[r.nextInt(c.length)]; }

    private void dracaena(Random r, double x, double y, double z) {
        int n = 54;
        for (int k = 0; k < n; k++) {
            boolean left = k % 5 < 2; double yaw = left ? Math.PI + pick(r, -0.6, 0.6) : r.nextDouble() * 2 * Math.PI;
            double pitch = left ? pick(r, 0.95, 1.3) : pick(r, 1.1, 1.5), len = pick(r, 0.8, 1.35) * (left ? 1.0 : 0.95);
            MeshBuilder.Leaf l = new MeshBuilder.Leaf().size(len, pick(r, 0.055, 0.075), 0.008).shape(0.3, 0.85).curl(0, 0.35, pick(r, 0.5, 1.0)); l.baseW = 0.4; l.segs = 8; l.cols = 3;
            String c = r.nextInt(6) == 0 ? "#7a8d56" : choose(r, "#26422f", "#31503a", "#3f6143", "#52744f");
            mesh(c).leaf(l, x + pick(r, -0.05, 0.05), y, z + pick(r, -0.04, 0.04), yaw, pitch, pick(r, -0.4, 0.4));
        }
    }

    private void echeveria(Random r, double x, double y, double z) {
        int[] cnt = {4, 6, 8, 11, 14, 17}; String[] col = {"#b9cdbf", "#a6c0b3", "#92b0a4", "#80a298", "#729488", "#6a8a82"};
        for (int ring = 0; ring < cnt.length; ring++) for (int k = 0; k < cnt[ring]; k++) {
            double a = k * 2 * Math.PI / cnt[ring] + ring * 0.55 + pick(r, -0.1, 0.1), t = ring / (double) (cnt.length - 1);
            double pitch = 1.38 - t * 1.2 + pick(r, -0.05, 0.05), len = 0.26 + t * 0.17 + pick(r, -0.02, 0.02);
            MeshBuilder.Leaf l = new MeshBuilder.Leaf().size(len, 0.13 + t * 0.07, 0.05 + t * 0.012).shape(0.58, 0.8).curl(0.3, 0.1, -0.15 + t * 0.55); l.segs = 6; l.cols = 5;
            String c = ring >= 4 && r.nextInt(10) == 0 ? "#8e93a0" : col[ring];
            double rad = 0.03 + 0.012 * ring; mesh(c).leaf(l, x + Math.cos(a) * rad, y + 0.075 - ring * 0.009, z + Math.sin(a) * rad, a, pitch, pick(r, -0.1, 0.1));
        }
    }

    private void pelargonium(Random r, double x, double y, double z) {
        int stems = 4;
        for (int b = 0; b < stems; b++) {
            double a = b * 1.57 + 0.5 + pick(r, -0.2, 0.2), h = pick(r, 0.85, 1.2) - (b == 3 ? 0.2 : 0), lean = pick(r, 0.12, 0.3);
            double[] p0 = {x + Math.cos(a) * 0.03, y, z + Math.sin(a) * 0.03}, p1 = {p0[0] + Math.cos(a) * lean * 0.3, y + h * 0.5, p0[2] + Math.sin(a) * lean * 0.3}, p2 = {p0[0] + Math.cos(a) * lean, y + h, p0[2] + Math.sin(a) * lean};
            mesh("#a3ae72").stem(new double[][]{p0, p1, p2}, 0.017, 0.009, 5);
            int nodes = b == 3 ? 4 : 5;
            for (int k = 0; k < nodes; k++) {
                double t = 0.3 + 0.7 * k / (nodes - 1.0), nx, ny, nz; if (t < 0.5) { double u = t / 0.5; nx = p0[0] + (p1[0] - p0[0]) * u; ny = p0[1] + (p1[1] - p0[1]) * u; nz = p0[2] + (p1[2] - p0[2]) * u; }
                else { double u = (t - 0.5) / 0.5; nx = p1[0] + (p2[0] - p1[0]) * u; ny = p1[1] + (p2[1] - p1[1]) * u; nz = p1[2] + (p2[2] - p1[2]) * u; }
                double ya = a + (k % 2 == 0 ? 1 : -1) * pick(r, 0.7, 1.5), pl = pick(r, 0.1, 0.19), up = k == nodes - 1 ? 0.75 : pick(r, 0.2, 0.55);
                double ex = nx + Math.cos(ya) * Math.cos(up) * pl, ey = ny + Math.sin(up) * pl, ez = nz + Math.sin(ya) * Math.cos(up) * pl;
                mesh("#a3ae72").tube(new double[]{nx, ny, nz}, new double[]{ex, ey, ez}, 0.008, 0.005, 4);
                double R = pick(r, 0.22, 0.3) - k * 0.006; String c = choose(r, "#5f8447", "#51733f", "#6a8f4c", "#476b3a");
                mesh(c).lobedLeaf(R, r.nextBoolean() ? 5 : 7, 0.52, pick(r, 0.1, 0.35), ex, ey, ez, ya, k == nodes - 1 ? 0.45 : pick(r, -0.25, 0.15), pick(r, -0.3, 0.3), 3, 36);
            }
        }
    }

    private void crassula(Random r, double x, double y, double z) {
        mesh("#7c7f52").stem(new double[][]{{x, y, z}, {x + 0.01, y + 0.1, z}, {x, y + 0.2, z - 0.01}}, 0.035, 0.03, 6);
        double[] tierY = {0.03, 0.13, 0.22}, pit = {0.35, 0.85, 1.25}; int[] cnt = {6, 5, 4};
        for (int t = 0; t < 3; t++) for (int k = 0; k < cnt[t]; k++) {
            double a = k * 2 * Math.PI / cnt[t] + t * 0.8 + pick(r, -0.2, 0.2), len = (t == 0 ? 0.4 : t == 1 ? 0.42 : 0.34) + pick(r, -0.04, 0.04);
            MeshBuilder.Leaf l = new MeshBuilder.Leaf().size(len, pick(r, 0.15, 0.2), 0.07).shape(0.62, 0.7).curl(0.22, 0, t == 0 ? 0.25 : pick(r, -0.15, 0.3)); l.segs = 6; l.cols = 5;
            mesh(choose(r, "#4f6e4b", "#5d7a55", "#456542", "#6a8560")).leaf(l, x + Math.cos(a) * 0.03, y + tierY[t], z + Math.sin(a) * 0.03, a, pit[t] + pick(r, -0.12, 0.12), pick(r, -0.15, 0.15));
        }
    }

    private void broadleaf(Random r, double x, double y, double z) {
        mesh("#6f8a45").stem(new double[][]{{x, y, z}, {x + 0.02, y + 0.25, z + 0.01}, {x, y + 0.5, z}}, 0.03, 0.018, 6);
        double[] ty = {0.12, 0.26, 0.38, 0.5}; double rot = pick(r, 0, 3);
        for (int t = 0; t < ty.length; t++) for (int k = 0; k < 2; k++) {
            double a = rot + t * 1.1 + k * Math.PI + pick(r, -0.25, 0.25), sc = t == 3 ? 0.62 : 1.0 - t * 0.08;
            double[] base = {x + (t > 0 ? 0.01 : 0), y + ty[t], z}, tip = {base[0] + Math.cos(a) * 0.07, base[1] + 0.04, base[2] + Math.sin(a) * 0.07};
            mesh("#7a9548").tube(base, tip, 0.012, 0.008, 4);
            MeshBuilder.Leaf l = new MeshBuilder.Leaf().size(pick(r, 0.36, 0.43) * sc, pick(r, 0.25, 0.3) * sc, 0.014).shape(0.4, 0.78).curl(0.36, 0.14, pick(r, 0.35, 0.7)); l.segs = 7; l.cols = 5;
            mesh(t == 3 ? "#78ad4d" : choose(r, "#4d8a3b", "#5b9a44", "#3f7633", "#549040")).leaf(l, tip[0], tip[1], tip[2], a, pick(r, 0.35, 0.95), pick(r, -0.3, 0.3));
        }
        for (int k = 0; k < 3; k++) { double a = rot + k * 2.1; MeshBuilder.Leaf l = new MeshBuilder.Leaf().size(0.17, 0.11, 0.01).shape(0.45, 0.8).curl(0.3, 0.1, 0.2); mesh("#8cbf55").leaf(l, x, y + 0.5, z, a, 1.2, 0); }
    }
}
