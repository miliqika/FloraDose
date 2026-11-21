package ua.floradose;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.SnapshotParameters;
import java.util.*;
import java.util.function.BiConsumer;

final class Scene3D {
    static final double S = 100;
    enum Mode { MODEL, INNER, EL, WATER, SENS }
    static final double[] XS = {-2.30, -1.46, -0.62, 0.22, 1.06};
    static final double[] HX = {-2.0, XS[1], XS[2], XS[3], XS[4]};
    static final double TZ = 0.10, ZH = 0.8, TOP = 1.40, ZW = -0.12, Y0 = 0.05;
    static final double TRAY_W = 0.80, TRAY_D = 0.66;
    private static final Set<String> SHELL = new HashSet<>(Arrays.asList("shell", "service", "window", "lid"));
    private static final Set<String> GHOSTED = new HashSet<>(Arrays.asList("shell", "service", "window"));
    private static final Set<String> SENSORS = new HashSet<>(Arrays.asList("electrode", "light", "ntc", "mic", "loadcell", "level", "slotboard"));

    static final class Part {
        final Shape3D n; final String id; final int idx; final double[] base, ex; PhongMaterial mat;
        boolean liner, plant, wood, glass, soil; int sp = -1; double delay, ph; PhongMaterial alt;
        Part(Shape3D n, String id, int idx, double[] base, double[] ex, PhongMaterial mat) { this.n = n; this.id = id; this.idx = idx; this.base = base; this.ex = ex; this.mat = mat; }
    }
    static final class Niche { final double x0, y0, x1, y1, r, depth, soft; Niche(double x0, double y0, double x1, double y1, double r, double depth, double soft) { this.x0 = x0; this.y0 = y0; this.x1 = x1; this.y1 = y1; this.r = r; this.depth = depth; this.soft = soft; } }

    final List<Part> parts = new ArrayList<>();
    final Group world = new Group();
    final SubScene sub;
    final Rotate rx = new Rotate(22, Rotate.X_AXIS), ry = new Rotate(28, Rotate.Y_AXIS);
    final PerspectiveCamera cam = new PerspectiveCamera(true);
    BiConsumer<String, Integer> onPick = (id, i) -> { }, onHover = (id, i) -> { };
    boolean autoFit = true; private double fitW = 690, fitH = 420;
    private final List<Node> late = new ArrayList<>();
    private final Sphere[][] drops = new Sphere[5][7];
    private final Point3D[][] paths = new Point3D[5][];
    private Part colWater, baseWater, colMeniscus;
    private final PhongMaterial[] woodM = new PhongMaterial[3], glassM = new PhongMaterial[3], soilM = new PhongMaterial[5];
    private final double[] soilShown = {-99, -99, -99, -99, -99};
    private int lastCfg = -1;
    java.util.function.IntConsumer onPanelTap = k -> { };
    private double ledLevel = 1; private PointLight warmL;
    static final double[][] ICON = {{0.30, 0.64}, {0.56, 0.64}};
    private final PhongMaterial hl = pm(Color.web("#2fbf8a")), hlGhost = tp("#2fbf8a", 0.4), ghost = tp("#caa070", 0.1), dim = tp("#8a8a8a", 0.2),
            red = pm(Color.web("#e03030")), open = pm(Color.web("#2f7cf0")), glowM = new PhongMaterial(Color.gray(0.42, 0.97)), panelM = new PhongMaterial(Color.WHITE);
    private static final int DW = 640, DH = 734;
    private final Canvas dispCanvas = new Canvas(DW, DH);
    private final WritableImage dispImg = new WritableImage(DW, DH);
    private String dispKey = ""; private boolean dispBound;
    private double mx, my;
    private final Map<String, PhongMaterial> mc = new HashMap<>();
    private Box tableBox, floorBox, floorRoom, wallBox, glowPlane; private final Group decor = new Group(); private PhongMaterial wallTexM, wallPlainM, floorM, floorRoomM, glowPlaneM;
    private Color ledCur = Color.web("#ffb36b"), ledTarget = ledCur; private int glowKey = -1, bgMode = 1, lastHash = 0; private boolean decorOn = true, dirty = true;
    private String isolId; private int isolIdx = -1; private final boolean[][] plantBuilt = new boolean[5][5];
    PhongMaterial pmc(String hex) { return mc.computeIfAbsent(hex, h -> pm(Color.web(h))); }

    static PhongMaterial tp(String hex, double a) { Color c = Color.web(hex); PhongMaterial m = new PhongMaterial(Color.color(c.getRed() * a, c.getGreen() * a, c.getBlue() * a, a)); m.setSpecularColor(Color.BLACK); return m; }
    static PhongMaterial pm(Color c) { PhongMaterial m = new PhongMaterial(c); m.setSpecularColor(Color.gray(0.3)); m.setSpecularPower(24); return m; }

    Scene3D() {
        PointLight key = new PointLight(Color.gray(0.52)); key.setTranslateX(-500); key.setTranslateY(-850); key.setTranslateZ(-800);
        PointLight fill = new PointLight(Color.gray(0.2)); fill.setTranslateX(700); fill.setTranslateY(-450); fill.setTranslateZ(-900);
        warmL = new PointLight(Color.web("#3a2410")); warmL.setTranslateY(-30); warmL.setTranslateZ(-300);
        Group root = new Group(world, new AmbientLight(Color.gray(0.38)), key, fill, warmL);
        sub = new SubScene(root, 800, 700, true, SceneAntialiasing.BALANCED);
        sub.setFill(Color.web("#dedad4"));
        cam.setNearClip(1); cam.setFarClip(12000); cam.setFieldOfView(30); cam.setTranslateZ(-1250); cam.setTranslateY(-90);
        sub.setCamera(cam);
        rx.setPivotY(-70); ry.setPivotY(-70);
        world.getTransforms().addAll(rx, ry);
        sub.widthProperty().addListener((o, a, b) -> { if (autoFit) fit(); });
        sub.heightProperty().addListener((o, a, b) -> { if (autoFit) fit(); });
        sub.setOnMousePressed(e -> { mx = e.getSceneX(); my = e.getSceneY(); });
        sub.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - mx, dy = e.getSceneY() - my; mx = e.getSceneX(); my = e.getSceneY();
            if (e.isSecondaryButtonDown()) { cam.setTranslateX(cam.getTranslateX() - dx); cam.setTranslateY(cam.getTranslateY() - dy); autoFit = false; }
            else { ry.setAngle(ry.getAngle() + dx * 0.4); rx.setAngle(Math.max(-5, Math.min(89, rx.getAngle() + dy * 0.4))); }
        });
        sub.setOnScroll(e -> { autoFit = false; cam.setTranslateZ(Math.max(-4500, Math.min(-300, cam.getTranslateZ() + e.getDeltaY() * 1.4))); });
    }
    void fit() {
        double w = sub.getWidth(), h = sub.getHeight(); if (w < 60 || h < 60) return;
        double t = Math.tan(Math.toRadians(cam.getFieldOfView() / 2)), asp = w / h;
        double dW = fitW / (2 * t * asp * 0.86), dH = fitH / (2 * t * 0.74);
        cam.setTranslateX(0); cam.setTranslateY(-105); cam.setTranslateZ(-Math.max(dW, dH));
    }
    void setView(String v) {
        cam.setFieldOfView(v.equals("photo") ? 44 : 30); fitW = v.equals("photo") ? 640 : 690;
        if (v.equals("iso")) { rx.setAngle(22); ry.setAngle(28); }
        else if (v.equals("front")) { rx.setAngle(4); ry.setAngle(0); }
        else if (v.equals("top")) { rx.setAngle(88); ry.setAngle(0); }
        else if (v.equals("back")) { rx.setAngle(15); ry.setAngle(180); }
        else if (v.equals("right")) { rx.setAngle(25); ry.setAngle(-50); }
        else if (v.equals("photo")) { rx.setAngle(31); ry.setAngle(25); }
        autoFit = true; fit();
    }
    String viewState() { return rx.getAngle() + ";" + ry.getAngle() + ";" + cam.getTranslateX() + ";" + cam.getTranslateY() + ";" + cam.getTranslateZ() + ";" + autoFit + ";" + cam.getFieldOfView(); }
    void restoreView(String s) {
        if (s == null || s.isEmpty()) return; String[] p = s.split(";");
        rx.setAngle(Double.parseDouble(p[0])); ry.setAngle(Double.parseDouble(p[1])); cam.setTranslateX(Double.parseDouble(p[2])); cam.setTranslateY(Double.parseDouble(p[3])); cam.setTranslateZ(Double.parseDouble(p[4])); autoFit = Boolean.parseBoolean(p[5]); if (p.length > 6) cam.setFieldOfView(Double.parseDouble(p[6]));
    }
    Part find(String id, int idx) { for (Part p : parts) if (p.id.equals(id) && (idx < 0 || p.idx == idx || p.idx < 0)) return p; return null; }
    Point2D project(Part p) {
        Point3D a = p.n.localToScene(Point3D.ZERO); double zc = a.getZ() - cam.getTranslateZ(); if (zc < 1) return null;
        double f = (sub.getHeight() / 2) / Math.tan(Math.toRadians(cam.getFieldOfView() / 2));
        return new Point2D(sub.getWidth() / 2 + (a.getX() - cam.getTranslateX()) * f / zc, sub.getHeight() / 2 + (a.getY() - cam.getTranslateY()) * f / zc);
    }

    private static Point3D pt(double[] a) { return new Point3D(a[0] * S, -a[1] * S, -a[2] * S); }
    private Part reg(Shape3D n, String id, int idx, double[] ex, PhongMaterial m, boolean lateFlag) {
        n.setMaterial(m);
        if (n instanceof MeshView) ((MeshView) n).setCullFace(CullFace.NONE);
        Part p = new Part(n, id, idx, new double[]{n.getTranslateX(), n.getTranslateY(), n.getTranslateZ()}, ex == null ? new double[3] : new double[]{ex[0] * S, -ex[1] * S, -ex[2] * S}, m);
        p.delay = delayOf(id); parts.add(p);
        n.setOnMouseClicked(e -> { if (e.isStillSincePress()) onPick.accept(id, idx); });
        n.setOnMouseEntered(e -> onHover.accept(id, idx)); n.setOnMouseExited(e -> onHover.accept(null, -1));
        if (lateFlag) late.add(n); else world.getChildren().add(n);
        return p;
    }
    private static double delayOf(String id) {
        switch (id) {
            case "shell": case "service": case "lid": return 0;
            case "panel": return 0.05; case "window": return 0.1; case "slot": return 0.05; case "tank": return 0.15;
            case "loadcell": return 0.3; case "frame": return 0.35; case "slotboard": return 0.4;
            default: Data.C c = Data.COMPS.get(id); return c != null && c.cat.equals("el") && c.internal ? 0.6 : 0.45;
        }
    }
    private <T extends Shape3D> T at(T n, double x, double y, double z) { n.setTranslateX(x * S); n.setTranslateY(-y * S); n.setTranslateZ(-z * S); return n; }
    private Box box(double w, double h, double d) { return new Box(w * S, h * S, d * S); }
    private Cylinder cyl(double r, double h) { return new Cylinder(r * S, h * S); }
    private Cylinder between(double[] a, double[] b, double r) {
        Point3D A = pt(a), B = pt(b), d = B.subtract(A); double len = d.magnitude();
        Cylinder c = new Cylinder(r * S, len);
        Point3D axis = new Point3D(0, 1, 0).crossProduct(d);
        if (axis.magnitude() > 1e-6) c.getTransforms().add(new Rotate(Math.toDegrees(Math.acos(d.getY() / len)), axis));
        Point3D m = A.midpoint(B); c.setTranslateX(m.getX()); c.setTranslateY(m.getY()); c.setTranslateZ(m.getZ());
        return c;
    }
    private void wire(String id, int idx, double[][] pts, double r, PhongMaterial m, double[] ex) {
        for (int i = 0; i < pts.length - 1; i++) reg(between(pts[i], pts[i + 1], r), id, idx, ex, m, false);
        for (int i = 1; i < pts.length - 1; i++) { Sphere s = new Sphere(r * S, 8); reg(at(s, pts[i][0], pts[i][1], pts[i][2]), id, idx, ex, m, false); }
    }
    private static void tri(List<Integer> f, float[] P, int a, int b, int c, float cx, float cy, float cz, int grp, List<Integer> g) {
        float ux = P[b * 3] - P[a * 3], uy = P[b * 3 + 1] - P[a * 3 + 1], uz = P[b * 3 + 2] - P[a * 3 + 2], vx = P[c * 3] - P[a * 3], vy = P[c * 3 + 1] - P[a * 3 + 1], vz = P[c * 3 + 2] - P[a * 3 + 2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float tx = (P[a * 3] + P[b * 3] + P[c * 3]) / 3 - cx, ty = (P[a * 3 + 1] + P[b * 3 + 1] + P[c * 3 + 1]) / 3 - cy, tz = (P[a * 3 + 2] + P[b * 3 + 2] + P[c * 3 + 2]) / 3 - cz;
        if (nx * tx + ny * ty + nz * tz < 0) { int t = b; b = c; c = t; }
        f.add(a); f.add(b); f.add(c); g.add(grp);
    }
    private static MeshView mesh(float[] P, float[] T, List<Integer> F, List<Integer> G) {
        TriangleMesh m = new TriangleMesh(); m.getPoints().setAll(P); m.getTexCoords().setAll(T);
        int[] faces = new int[F.size() * 2]; for (int i = 0; i < F.size(); i++) { faces[2 * i] = F.get(i); faces[2 * i + 1] = F.get(i); }
        m.getFaces().setAll(faces); int[] g = new int[G.size()]; for (int i = 0; i < g.length; i++) g[i] = G.get(i); m.getFaceSmoothingGroups().setAll(g);
        return new MeshView(m);
    }
    static MeshView ell(double a, double b, double c) { return ell(a, b, c, 7, 10); }
    static MeshView ell(double a, double b, double c, int st, int sl) {
        int np = (st + 1) * (sl + 1); float[] P = new float[np * 3], T = new float[np * 2]; List<Integer> F = new ArrayList<>(), G = new ArrayList<>();
        for (int i = 0; i <= st; i++) for (int j = 0; j <= sl; j++) { int k = i * (sl + 1) + j; double ph = Math.PI * i / st, th = 2 * Math.PI * j / sl;
            P[k * 3] = (float) (a * S * Math.sin(ph) * Math.cos(th)); P[k * 3 + 1] = (float) (b * S * Math.cos(ph)); P[k * 3 + 2] = (float) (c * S * Math.sin(ph) * Math.sin(th)); T[k * 2] = (float) j / sl; T[k * 2 + 1] = (float) i / st; }
        for (int i = 0; i < st; i++) for (int j = 0; j < sl; j++) { int p00 = i * (sl + 1) + j, p01 = p00 + 1, p10 = (i + 1) * (sl + 1) + j, p11 = p10 + 1; tri(F, P, p00, p10, p11, 0, 0, 0, 1, G); tri(F, P, p00, p11, p01, 0, 0, 0, 1, G); }
        return mesh(P, T, F, G);
    }
    private Affine basis(double[] pos, double[] dir, double len, double roll) {
        Point3D P = pt(pos), L = new Point3D(dir[0], -dir[1], -dir[2]).normalize(), up = new Point3D(0, -1, 0);
        Point3D T = up.subtract(L.multiply(up.dotProduct(L)));
        if (T.magnitude() < 0.15) T = new Point3D(1, 0, 0).subtract(L.multiply(L.getX()));
        T = T.normalize(); Point3D W = L.crossProduct(T);
        if (roll != 0) { Point3D T2 = T.multiply(Math.cos(roll)).add(W.multiply(Math.sin(roll))); T = T2.normalize(); W = L.crossProduct(T); }
        Point3D C = P.add(L.multiply(len * S / 2));
        return new Affine(W.getX(), L.getX(), T.getX(), C.getX(), W.getY(), L.getY(), T.getY(), C.getY(), W.getZ(), L.getZ(), T.getZ(), C.getZ());
    }
    private Part leaf(String id, int idx, double[] ex, PhongMaterial m, double[] pos, double[] dir, double len, double wid, double th, double roll, int sp) {
        MeshView v = ell(wid / 2, len / 2, th / 2); v.getTransforms().add(basis(pos, dir, len, roll));
        Part p = reg(v, id, idx, ex, m, false); p.plant = sp >= 0; p.sp = sp; return p;
    }
    private static double[] dirOf(double yaw, double tilt) { return new double[]{Math.cos(yaw) * Math.sin(tilt), Math.cos(tilt), Math.sin(yaw) * Math.sin(tilt)}; }

    private static void arc(List<double[]> p, double cx, double cz, double r, double a0, int seg) { for (int i = 0; i <= seg; i++) { double a = Math.toRadians(a0 + 90.0 * i / seg); p.add(new double[]{cx + r * Math.cos(a), cz + r * Math.sin(a)}); } }
    private static List<double[]> plan(double x0, double x1, double z0, double z1, double[] r, double in, int seg, int frontSub, int[] frontRange, double[] ts) {
        List<double[]> p = new ArrayList<>();
        double[][] cs = {{x0 + in, z0 + in, 1, 1, 180}, {x1 - in, z0 + in, -1, 1, 270}, {x1 - in, z1 - in, -1, -1, 0}, {x0 + in, z1 - in, 1, -1, 90}};
        for (int k = 0; k < 4; k++) {
            double rr = Math.max(0.02, r[k] - in), cx = cs[k][0] + cs[k][2] * rr, cz = cs[k][1] + cs[k][3] * rr;
            arc(p, cx, cz, rr, cs[k][4], seg);
            if (k == 2 && frontSub > 0) {
                double[] a = p.get(p.size() - 1); double xe = x0 + in + Math.max(0.02, r[3] - in);
                if (frontRange != null) frontRange[0] = p.size();
                for (int i = 1; i <= frontSub; i++) { double t = ts != null ? ts[i - 1] : (double) i / (frontSub + 1); p.add(new double[]{a[0] + (xe - a[0]) * t, a[1]}); }
                if (frontRange != null) frontRange[1] = p.size() - 1;
            }
        }
        return p;
    }
    private static double sdRR(double px, double py, double cx, double cy, double hx, double hy, double r) {
        double qx = Math.abs(px - cx) - (hx - r), qy = Math.abs(py - cy) - (hy - r);
        return Math.hypot(Math.max(qx, 0), Math.max(qy, 0)) + Math.min(Math.max(qx, qy), 0) - r;
    }
    static MeshView loft(double x0, double x1, double z0, double z1, double H, double[] r, double radBot, double radTop, Niche ni) { return loft(x0, x1, z0, z1, H, r, radBot, radTop, ni, 0, H); }
    static MeshView loft(double x0, double x1, double z0, double z1, double H, double[] r, double radBot, double radTop, Niche ni, double uvY0, double uvH) {
        int seg = 9, K = 6; List<double[]> prof = new ArrayList<>(); double[] ts = null;
        if (ni != null) {
            List<Double> tl = new ArrayList<>(); for (int i = 1; i <= 110; i++) tl.add(i / 111.0);
            double a0 = x1 - r[2], span = a0 - (x0 + r[3]), st = ni.soft / 8 / span, e5 = 0.005 / span, tR = (a0 - ni.x1) / span, tL = (a0 - ni.x0) / span;
            for (double t = tR - e5; t <= tR + ni.soft / span + e5; t += st) tl.add(t);
            for (double t = tL - ni.soft / span - e5; t <= tL + e5; t += st) tl.add(t);
            Collections.sort(tl); List<Double> td = new ArrayList<>(); double lt = 0; for (double t : tl) if (t > 1e-4 && t < 1 - 1e-4 && t > lt + 2e-4) { td.add(t); lt = t; }
            ts = new double[td.size()]; for (int i = 0; i < ts.length; i++) ts[i] = td.get(i);
        }
        int fsub = ts == null ? 0 : ts.length;
        if (radBot > 0) for (int k = 0; k <= K; k++) { double a = Math.toRadians(90.0 * k / K); prof.add(new double[]{radBot * (1 - Math.sin(a)), radBot * (1 - Math.cos(a))}); } else prof.add(new double[]{0, 0});
        double ya = radBot, yb = H - radTop; int mid = Math.max(1, (int) Math.round((yb - ya) / 0.04)); List<Double> ys = new ArrayList<>();
        for (int i = 1; i < mid; i++) ys.add(ya + (yb - ya) * i / mid);
        if (ni != null) { double st = ni.soft / 8; for (double y = ni.y0 - 0.005; y <= ni.y0 + ni.soft + 0.005; y += st) ys.add(y); for (double y = ni.y1 - ni.soft - 0.005; y <= ni.y1 + 0.005; y += st) ys.add(y); }
        Collections.sort(ys); double lastY = ya; for (double y : ys) if (y > ya + 1e-4 && y < yb - 1e-4 && y > lastY + 1e-4) { prof.add(new double[]{0, y}); lastY = y; }
        if (radTop > 0) for (int k = K; k >= 0; k--) { double a = Math.toRadians(90.0 * k / K); prof.add(new double[]{radTop * (1 - Math.sin(a)), H - radTop * (1 - Math.cos(a))}); } else prof.add(new double[]{0, H});
        int R = prof.size(), N = 0; List<List<double[]>> rings = new ArrayList<>(); int[] fr = {0, -1};
        for (int ri = 0; ri < R; ri++) rings.add(plan(x0, x1, z0, z1, r, prof.get(ri)[0], seg, fsub, ri == 0 ? fr : null, ts));
        N = rings.get(0).size(); int stride = N + 1, capBase = R * stride;
        float[] P0 = new float[(R * stride + 2 * (N + 1)) * 3], P1 = new float[P0.length], T = new float[(R * stride + 2 * (N + 1)) * 2];
        for (int ri = 0; ri < R; ri++) {
            List<double[]> poly = rings.get(ri); double y = prof.get(ri)[1], tot = 0;
            for (int j = 0; j < N; j++) { double[] a = poly.get(j), b = poly.get((j + 1) % N); tot += Math.hypot(a[0] - b[0], a[1] - b[1]); }
            double acc = 0;
            for (int j = 0; j <= N; j++) {
                int k = ri * stride + j; double[] a = poly.get(j % N), b = poly.get((j + 1) % N); double zz = a[1];
                P0[k * 3] = (float) (a[0] * S); P0[k * 3 + 1] = (float) (-y * S); P0[k * 3 + 2] = (float) (-zz * S);
                if (ni != null && fr[1] >= 0 && j % N >= fr[0] && j % N <= fr[1]) {
                    double sd = sdRR(a[0], y, (ni.x0 + ni.x1) / 2, (ni.y0 + ni.y1) / 2, (ni.x1 - ni.x0) / 2, (ni.y1 - ni.y0) / 2, ni.r);
                    double t = Math.max(0, Math.min(1, -sd / ni.soft)); zz -= ni.depth * t * t * (3 - 2 * t);
                }
                P1[k * 3] = P0[k * 3]; P1[k * 3 + 1] = P0[k * 3 + 1]; P1[k * 3 + 2] = (float) (-zz * S);
                T[k * 2] = (float) (acc / tot); T[k * 2 + 1] = (float) (1 - (y + uvY0) / uvH);
                if (j < N) acc += Math.hypot(a[0] - b[0], a[1] - b[1]);
            }
        }
        float cx = (float) ((x0 + x1) / 2 * S), cy = (float) (-H / 2 * S), cz = (float) (-(z0 + z1) / 2 * S); List<Integer> F = new ArrayList<>(), G = new ArrayList<>();
        for (int ri = 0; ri < R - 1; ri++) for (int j = 0; j < N; j++) { int a = ri * stride + j, b = a + 1, c = (ri + 1) * stride + j, d = c + 1; tri(F, P0, a, b, d, cx, cy, cz, 1, G); tri(F, P0, a, d, c, cx, cy, cz, 1, G); }
        for (int cap = 0; cap < 2; cap++) {
            int rb = (cap == 0 ? 0 : R - 1) * stride; int cb = capBase + cap * (N + 1); double y = cap == 0 ? 0 : H;
            P0[cb * 3] = cx; P0[cb * 3 + 1] = (float) (-y * S); P0[cb * 3 + 2] = cz; P1[cb * 3] = cx; P1[cb * 3 + 1] = P0[cb * 3 + 1]; P1[cb * 3 + 2] = cz; T[cb * 2] = 0.5f; T[cb * 2 + 1] = 0.5f;
            for (int j = 0; j < N; j++) { double[] a = rings.get(cap == 0 ? 0 : R - 1).get(j); int k = cb + 1 + j;
                P0[k * 3] = (float) (a[0] * S); P0[k * 3 + 1] = (float) (-y * S); P0[k * 3 + 2] = (float) (-a[1] * S); P1[k * 3] = P0[k * 3]; P1[k * 3 + 1] = P0[k * 3 + 1]; P1[k * 3 + 2] = P0[k * 3 + 2];
                T[k * 2] = (float) ((a[0] - x0) / (x1 - x0)); T[k * 2 + 1] = (float) ((a[1] - z0) / (z1 - z0)); }
            for (int j = 0; j < N; j++) { int a = cb, b = cb + 1 + j, c = cb + 1 + (j + 1) % N; tri(F, P0, a, b, c, cx, cy, cz, 2 + cap, G); }
        }
        return mesh(P1, T, F, G);
    }
    static MeshView flatRing(double x0, double x1, double z0, double z1, double r, double wall, double y0, double y1) {
        List<double[]> o = plan(x0, x1, z0, z1, new double[]{r, r, r, r}, 0, 6, 0, null, null), in = plan(x0 + wall, x1 - wall, z0 + wall, z1 - wall, new double[]{Math.max(.05, r - wall), Math.max(.05, r - wall), Math.max(.05, r - wall), Math.max(.05, r - wall)}, 0, 6, 0, null, null);
        int n = o.size(); float[] P = new float[4 * n * 3], T = new float[4 * n * 2]; List<Integer> F = new ArrayList<>(), G = new ArrayList<>();
        double cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
        for (int i = 0; i < n; i++) {
            double[][] pp = {{o.get(i)[0], y1, o.get(i)[1]}, {in.get(i)[0], y1, in.get(i)[1]}, {o.get(i)[0], y0, o.get(i)[1]}, {in.get(i)[0], y0, in.get(i)[1]}};
            for (int k = 0; k < 4; k++) { int q = k * n + i; P[q * 3] = (float) (pp[k][0] * S); P[q * 3 + 1] = (float) (-pp[k][1] * S); P[q * 3 + 2] = (float) (-pp[k][2] * S); T[q * 2] = (float) (i / (double) n); T[q * 2 + 1] = k / 3f; }
        }
        for (int i = 0; i < n; i++) { int j = (i + 1) % n; int oT = i, oT2 = j, iT = n + i, iT2 = n + j, oB = 2 * n + i, oB2 = 2 * n + j, iB = 3 * n + i, iB2 = 3 * n + j;
            triN(F, G, P, oT, oT2, iT2, 0, -1, 0, 1); triN(F, G, P, oT, iT2, iT, 0, -1, 0, 1);
            double mx = (o.get(i)[0] + o.get(j)[0]) / 2 - cx, mz = (o.get(i)[1] + o.get(j)[1]) / 2 - cz;
            triN(F, G, P, oT, oT2, oB2, (float) mx, 0, (float) -mz, 2); triN(F, G, P, oT, oB2, oB, (float) mx, 0, (float) -mz, 2);
            double ix = (in.get(i)[0] + in.get(j)[0]) / 2 - cx, iz = (in.get(i)[1] + in.get(j)[1]) / 2 - cz;
            triN(F, G, P, iT, iT2, iB2, (float) -ix, 0, (float) iz, 3); triN(F, G, P, iT, iB2, iB, (float) -ix, 0, (float) iz, 3); }
        return mesh(P, T, F, G);
    }
    private static void triN(List<Integer> f, List<Integer> g, float[] P, int a, int b, int c, float ex, float ey, float ez, int grp) {
        float ux = P[b * 3] - P[a * 3], uy = P[b * 3 + 1] - P[a * 3 + 1], uz = P[b * 3 + 2] - P[a * 3 + 2], vx = P[c * 3] - P[a * 3], vy = P[c * 3 + 1] - P[a * 3 + 1], vz = P[c * 3 + 2] - P[a * 3 + 2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        if (nx * ex + ny * ey + nz * ez < 0) { int t = b; b = c; c = t; }
        f.add(a); f.add(b); f.add(c); g.add(grp);
    }
    static List<double[]> rectR(double x0, double y0, double x1, double y1, double[] r) {
        List<double[]> p = new ArrayList<>(); double[][] cs = {{x0 + r[0], y0 + r[0], 180, r[0]}, {x1 - r[1], y0 + r[1], 270, r[1]}, {x1 - r[2], y1 - r[2], 0, r[2]}, {x0 + r[3], y1 - r[3], 90, r[3]}};
        for (double[] c : cs) for (int i = 0; i <= 6; i++) { double a = Math.toRadians(c[2] + i * 15); p.add(new double[]{c[0] + c[3] * Math.cos(a), c[1] + c[3] * Math.sin(a)}); }
        return p;
    }
    static List<double[]> rect(double x0, double y0, double x1, double y1, double r) { return rectR(x0, y0, x1, y1, new double[]{r, r, r, r}); }
    static MeshView prism(List<double[]> poly, double depth, boolean flipV) {
        int n = poly.size(); double minx = 1e9, maxx = -1e9, miny = 1e9, maxy = -1e9;
        for (double[] q : poly) { minx = Math.min(minx, q[0]); maxx = Math.max(maxx, q[0]); miny = Math.min(miny, q[1]); maxy = Math.max(maxy, q[1]); }
        float z = (float) (depth * S / 2); float[] P = new float[2 * n * 3], T = new float[2 * n * 2]; List<Integer> F = new ArrayList<>(), G = new ArrayList<>();
        for (int k = 0; k < 2; k++) for (int i = 0; i < n; i++) { double[] q = poly.get(i); int h = k * n + i; P[h * 3] = (float) (q[0] * S); P[h * 3 + 1] = (float) (-q[1] * S); P[h * 3 + 2] = k == 0 ? -z : z;
            T[h * 2] = (float) ((q[0] - minx) / (maxx - minx + 1e-9)); T[h * 2 + 1] = (float) (flipV ? (q[1] - miny) / (maxy - miny + 1e-9) : (maxy - q[1]) / (maxy - miny + 1e-9)); }
        float cx = (float) ((minx + maxx) / 2 * S), cy = (float) (-(miny + maxy) / 2 * S);
        for (int i = 1; i < n - 1; i++) { tri(F, P, 0, i, i + 1, cx, cy, 0, 1, G); tri(F, P, n, n + i, n + i + 1, cx, cy, 0, 2, G); }
        for (int i = 0; i < n; i++) { int j = (i + 1) % n; tri(F, P, i, j, j + n, cx, cy, 0, 3, G); tri(F, P, i, j + n, i + n, cx, cy, 0, 3, G); }
        return mesh(P, T, F, G);
    }

    static MeshView plate(List<double[]> poly, double thick) {
        int n = poly.size(); double minx = 1e9, maxx = -1e9, minz = 1e9, maxz = -1e9;
        for (double[] q : poly) { minx = Math.min(minx, q[0]); maxx = Math.max(maxx, q[0]); minz = Math.min(minz, q[1]); maxz = Math.max(maxz, q[1]); }
        int tot = 1 + 2 * n; float[] P = new float[tot * 3], T = new float[tot * 2]; List<Integer> F = new ArrayList<>(), G = new ArrayList<>();
        P[0] = (float) ((minx + maxx) / 2 * S); P[1] = 0; P[2] = (float) (-(minz + maxz) / 2 * S); T[0] = 0.5f; T[1] = 0.5f;
        for (int i = 0; i < n; i++) {
            double[] q = poly.get(i); float u = (float) ((q[0] - minx) / (maxx - minx)), v = (float) ((q[1] - minz) / (maxz - minz)); int a = 1 + i, b = 1 + n + i;
            P[a * 3] = (float) (q[0] * S); P[a * 3 + 1] = 0; P[a * 3 + 2] = (float) (-q[1] * S); T[a * 2] = u; T[a * 2 + 1] = v;
            P[b * 3] = P[a * 3]; P[b * 3 + 1] = (float) (thick * S); P[b * 3 + 2] = P[a * 3 + 2]; T[b * 2] = u; T[b * 2 + 1] = v;
        }
        float hx = P[0], hy = (float) (thick * S * 4), hz = P[2];
        for (int i = 0; i < n; i++) { int j = (i + 1) % n; tri(F, P, 0, 1 + i, 1 + j, hx, hy, hz, 1, G); tri(F, P, 1 + i, 1 + j, 1 + n + j, hx, hy, hz, 2, G); tri(F, P, 1 + i, 1 + n + j, 1 + n + i, hx, hy, hz, 2, G); }
        return mesh(P, T, F, G);
    }
    private static double domeH(double b, double rho, double th, double p1, double p2) {
        return b * Math.pow(Math.max(0, 1 - rho * rho), 0.55) + 0.014 * (1 - rho) * Math.sin(5.0 * rho * Math.cos(th) + p1) * Math.cos(4.0 * rho * Math.sin(th) + p2);
    }
    static MeshView dome(double a, double b, double c, int rings, int seg, long seed) {
        Random r = new Random(seed); double p1 = r.nextDouble() * 6, p2 = r.nextDouble() * 6;
        int np = 1 + rings * seg; float[] P = new float[np * 3], T = new float[np * 2]; List<Integer> F = new ArrayList<>(), G = new ArrayList<>();
        P[1] = (float) (-domeH(b, 0, 0, p1, p2) * S); T[0] = 0.5f; T[1] = 0.5f;
        for (int i = 1; i <= rings; i++) for (int j = 0; j < seg; j++) {
            double rho = (double) i / rings, th = 2 * Math.PI * j / seg; int k = 1 + (i - 1) * seg + j;
            P[k * 3] = (float) (a * rho * Math.cos(th) * S); P[k * 3 + 1] = (float) (-domeH(b, rho, th, p1, p2) * S); P[k * 3 + 2] = (float) (-c * rho * Math.sin(th) * S);
            T[k * 2] = (float) (0.5 + 0.5 * rho * Math.cos(th)); T[k * 2 + 1] = (float) (0.5 + 0.5 * rho * Math.sin(th));
        }
        float hy = (float) (S * 0.6);
        for (int j = 0; j < seg; j++) tri(F, P, 0, 1 + j, 1 + (j + 1) % seg, 0, hy, 0, 1, G);
        for (int i = 1; i < rings; i++) for (int j = 0; j < seg; j++) {
            int a0 = 1 + (i - 1) * seg + j, a1 = 1 + (i - 1) * seg + (j + 1) % seg, b0 = 1 + i * seg + j, b1 = 1 + i * seg + (j + 1) % seg;
            tri(F, P, a0, a1, b1, 0, hy, 0, 1, G); tri(F, P, a0, b1, b0, 0, hy, 0, 1, G);
        }
        return mesh(P, T, F, G);
    }
    static MeshView lathe(double[][] prof, int seg) {
        int R = prof.length; float[] P = new float[R * (seg + 1) * 3], T = new float[R * (seg + 1) * 2]; List<Integer> F = new ArrayList<>(), G = new ArrayList<>();
        for (int i = 0; i < R; i++) for (int j = 0; j <= seg; j++) { int k = i * (seg + 1) + j; double th = 2 * Math.PI * j / seg;
            P[k * 3] = (float) (prof[i][0] * Math.cos(th) * S); P[k * 3 + 1] = (float) (-prof[i][1] * S); P[k * 3 + 2] = (float) (prof[i][0] * Math.sin(th) * S); T[k * 2] = (float) j / seg; T[k * 2 + 1] = (float) i / (R - 1); }
        float hy = (float) (-prof[R - 1][1] / 2 * S);
        for (int i = 0; i < R - 1; i++) for (int j = 0; j < seg; j++) { int a = i * (seg + 1) + j, b = a + 1, c = (i + 1) * (seg + 1) + j, d = c + 1; tri(F, P, a, b, d, 0, hy, 0, 1, G); tri(F, P, a, d, c, 0, hy, 0, 1, G); }
        return mesh(P, T, F, G);
    }
    private static MeshView clothMesh(double x0, double w, double z0, double zE, double drop, double top) {
        int U = 30, V = 90; double rad = 0.08, lay = zE - z0 - rad, arc = Math.PI / 2 * rad, L = lay + arc + drop;
        float[] P = new float[(U + 1) * (V + 1) * 3], T = new float[(U + 1) * (V + 1) * 2]; List<Integer> F = new ArrayList<>(), G = new ArrayList<>();
        for (int v = 0; v <= V; v++) {
            double s = L * v / V, z, y, hang = 0;
            if (s <= lay) { z = z0 + s; y = top; } else if (s <= lay + arc) { double ph = (s - lay) / rad; z = zE - rad + rad * Math.sin(ph); y = top - rad + rad * Math.cos(ph); hang = 0.2; } else { z = zE; y = top - rad - (s - lay - arc); hang = 1; }
            for (int u = 0; u <= U; u++) {
                double uu = (double) u / U, x = x0 + w * (uu + 0.06 * (v / (double) V));
                double rip = 0.016 * Math.sin(uu * 11 + s * 1.9) + 0.012 * Math.sin(uu * 23 - s * 2.7);
                double yy = y, zz = z;
                if (hang < 0.1) yy += 0.012 + Math.max(0, rip) + 0.02 * Math.max(0, Math.sin(uu * 5.5 + 1.2) * Math.sin(s * 1.3)); else zz += 0.04 + (0.045 * Math.sin(uu * 13) + 0.02 * Math.sin(uu * 29 + 1) + 0.065) * Math.min(1, (s - lay) / 0.5);
                int idx = v * (U + 1) + u; P[idx * 3] = (float) (x * S); P[idx * 3 + 1] = (float) (-yy * S); P[idx * 3 + 2] = (float) (-zz * S); T[idx * 2] = (float) uu; T[idx * 2 + 1] = (float) v / V;
            }
        }
        for (int v = 0; v < V; v++) for (int u = 0; u < U; u++) {
            int a = v * (U + 1) + u, b = a + 1, c = (v + 1) * (U + 1) + u, d = c + 1; double sm = L * (v + 0.5) / V; boolean lying = sm <= lay + arc * 0.5;
            float cx = P[a * 3], cy = P[a * 3 + 1], cz = P[a * 3 + 2];
            float hx = cx, hy = lying ? cy + 80 : cy, hz = lying ? cz : cz + 80;
            tri(F, P, a, b, d, hx, hy, hz, 1, G); tri(F, P, a, d, c, hx, hy, hz, 1, G);
        }
        return mesh(P, T, F, G);
    }

    private static String cut(String s, int n) { return s.length() > n ? s.substring(0, n) : s; }
    void showScreen(String a, String b, Color col) {
        String key = a + "|" + b + "|" + col; if (key.equals(dispKey)) return; dispKey = key;
        GraphicsContext g = dispCanvas.getGraphicsContext2D(); g.clearRect(0, 0, DW, DH);
        g.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#2b2d32")), new Stop(0.45, Color.web("#0d0e11")), new Stop(1, Color.web("#040405")))); g.fillRect(0, 0, DW, DH);
        g.setFill(Color.web("#ffffff", 0.06)); g.fillPolygon(new double[]{0, 380, 220, 0}, new double[]{0, 0, DH, DH}, 4);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Color.web("#e6eaed", 0.5)); g.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 34)); g.fillText(cut(b, 14), DW / 2.0, 150);
        g.setFill(Color.web("#8a949b", 0.5)); g.setFont(Font.font("Segoe UI", 24)); g.fillText(cut(a, 22), DW / 2.0, 192);
        g.setLineCap(StrokeLineCap.ROUND); g.setLineJoin(StrokeLineJoin.ROUND);
        Color dropC = Color.web("#f6fbff").interpolate(col, 0.3), sunC = Color.web("#fff4dc");
        for (int k = 0; k < 2; k++) {
            double cx = ICON[k][0] * DW, cy = ICON[k][1] * DH; Color c = k == 0 ? dropC : sunC;
            g.setFill(new RadialGradient(0, 0, cx, cy, 110, false, CycleMethod.NO_CYCLE, new Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.16)), new Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0)))); g.fillOval(cx - 110, cy - 110, 220, 220);
            g.setStroke(c); g.setLineWidth(6);
            if (k == 0) { g.beginPath(); g.moveTo(cx, cy - 46); g.bezierCurveTo(cx - 8, cy - 26, cx - 34, cy - 8, cx - 34, cy + 14); g.bezierCurveTo(cx - 34, cy + 36, cx - 18, cy + 46, cx, cy + 46); g.bezierCurveTo(cx + 18, cy + 46, cx + 34, cy + 36, cx + 34, cy + 14); g.bezierCurveTo(cx + 34, cy - 8, cx + 8, cy - 26, cx, cy - 46); g.stroke(); g.setLineWidth(4.5); g.beginPath(); g.moveTo(cx - 20, cy + 18); g.bezierCurveTo(cx - 20, cy + 28, cx - 14, cy + 33, cx - 8, cy + 34); g.stroke(); }
            else { g.strokeOval(cx - 19, cy - 19, 38, 38); for (int q = 0; q < 8; q++) { double an = q * Math.PI / 4; g.strokeLine(cx + Math.cos(an) * 31, cy + Math.sin(an) * 31, cx + Math.cos(an) * 45, cy + Math.sin(an) * 45); } }
        }
        SnapshotParameters sp = new SnapshotParameters(); sp.setFill(Color.BLACK); dispCanvas.snapshot(sp, dispImg);
        if (!dispBound) { dispBound = true; panelM.setDiffuseMap(dispImg); panelM.setSelfIlluminationMap(dispImg); }
    }

    void setBackground(int m) {
        bgMode = m; boolean home = m == 1; Color c = home ? Color.web("#e0dcd6") : m == 0 ? Color.web("#e8e3d9") : Color.web("#cfd2d4");
        wallPlainM.setDiffuseColor(c); wallBox.setMaterial(home ? wallTexM : wallPlainM); sub.setFill(c);
        floorM.setDiffuseColor(m == 0 ? Color.web("#e4dfd5") : Color.web("#c9ccce"));
        floorBox.setVisible(!home); floorRoom.setVisible(home); tableBox.setVisible(home); decor.setVisible(home && decorOn);
    }
    void setDecor(boolean on) { decorOn = on; decor.setVisible(bgMode == 1 && on); }
    void isolate(String id, int idx) { isolId = id; isolIdx = idx; dirty = true; }
    String isolated() { return isolId; }
    void focus(Part p) {
        Point3D a = p.n.localToScene(Point3D.ZERO); autoFit = false; cam.setTranslateX(a.getX()); cam.setTranslateY(a.getY()); cam.setTranslateZ(a.getZ() - 520);
    }
    void setLedLevel(double v) { ledLevel = Math.max(0, Math.min(1, v)); }
    private static PhongMaterial glass(String hex, double a) { PhongMaterial m = tp(hex, a); m.setSpecularColor(Color.gray(0.75)); m.setSpecularPower(55); return m; }

    private static final double[] EXS = {0, 1.3, 0};
    void build() {
        for (int k = 0; k < 3; k++) { PhongMaterial w = new PhongMaterial(Color.WHITE); w.setDiffuseMap(Textures.wood(k)); w.setSpecularColor(Color.gray(k == 1 ? 0.2 : 0.13)); w.setSpecularPower(14); woodM[k] = w; }
        glassM[0] = glass("#dce9ec", 0.17); glassM[1] = glass("#e2eef0", 0.15); glassM[2] = glass("#d6eadb", 0.2);
        panelM.setSpecularColor(Color.gray(0.85)); panelM.setSpecularPower(110); glowM.setSpecularColor(Color.BLACK);
        Image mossImg = Textures.moss();
        for (int i = 0; i < 5; i++) { soilM[i] = new PhongMaterial(Color.WHITE); soilM[i].setDiffuseMap(mossImg); soilM[i].setSpecularColor(Color.gray(0.04)); soilM[i].setSpecularPower(8); }
        PhongMaterial wat = glass("#7fbfe6", 0.42); wat.setSpecularColor(Color.gray(0.5)); wat.setSpecularPower(40);
        PhongMaterial steel = pm(Color.web("#b9bec4")), alu = pm(Color.web("#d8dadd")), pom = pm(Color.web("#f4f4f2")), pcb = pm(Color.web("#1f6b3a")),
                tube = tp("#6fc3f5", 0.75), copper = pm(Color.web("#b87333")), grey = pm(Color.web("#7b7f84")), abs = pm(Color.web("#3a3d42")), chip = pm(Color.web("#222222")),
                batt = pm(Color.web("#2f6fb0")), psuM = pm(Color.web("#2c4a6a")), orange = pm(Color.web("#e8821e")), graphite = pm(Color.web("#33363b")), clip = pm(Color.web("#9aa0a6")),
                trayM = pm(Color.web("#8a857e")), darkM = pm(Color.web("#1a1511")), grooveM = pm(Color.web("#5b534a")), dropM = pm(Color.web("#3aa0ff"));
        trayM.setSpecularColor(Color.gray(0.4)); trayM.setSpecularPower(30); darkM.setSpecularColor(Color.gray(0.05)); grooveM.setSpecularColor(Color.BLACK);
        wallPlainM = new PhongMaterial(Color.web("#e0dcd6")); wallPlainM.setSpecularColor(Color.BLACK);
        wallTexM = new PhongMaterial(Color.WHITE); wallTexM.setDiffuseMap(Textures.wall()); wallTexM.setSpecularColor(Color.BLACK);
        wallBox = at(box(28, 12, 0.1), 0, 4.0, -2.4); wallBox.setMaterial(wallTexM); wallBox.setMouseTransparent(true); world.getChildren().add(wallBox);
        PhongMaterial tm = new PhongMaterial(Color.WHITE); tm.setDiffuseMap(Textures.table()); tm.setSpecularColor(Color.gray(0.1)); tm.setSpecularPower(16);
        tableBox = at(box(26, 0.5, 4.25), 0, -0.25, -0.225); tableBox.setMaterial(tm); tableBox.setMouseTransparent(true); world.getChildren().add(tableBox);
        floorM = new PhongMaterial(Color.web("#e2ddd2")); floorM.setSpecularColor(Color.BLACK);
        floorBox = at(box(60, 0.4, 40), 0, -0.2, 0); floorBox.setMaterial(floorM); floorBox.setMouseTransparent(true); world.getChildren().add(floorBox);
        floorRoomM = new PhongMaterial(Color.web("#9d99a3")); floorRoomM.setSpecularColor(Color.BLACK);
        floorRoom = at(box(80, 0.4, 60), 0, -7.6, 0); floorRoom.setMaterial(floorRoomM); floorRoom.setMouseTransparent(true); world.getChildren().add(floorRoom);
        PhongMaterial sm = new PhongMaterial(Color.WHITE); sm.setDiffuseMap(Textures.shadow()); sm.setSpecularColor(Color.BLACK);
        Box shadow = at(box(7.4, 0.002, 2.8), 0.05, 0.003, 0.05); shadow.setMaterial(sm); shadow.setMouseTransparent(true); world.getChildren().add(shadow);
        Image gp = Textures.glowPlane(); glowPlaneM = new PhongMaterial(Color.gray(0.3)); glowPlaneM.setDiffuseMap(gp); glowPlaneM.setSelfIlluminationMap(gp); glowPlaneM.setSpecularColor(Color.BLACK);
        glowPlane = at(box(4.6, 0.002, 1.7), -0.25, 0.004, 1.65); glowPlane.setMaterial(glowPlaneM); glowPlane.setMouseTransparent(true); world.getChildren().add(glowPlane);
        PhongMaterial clothM = new PhongMaterial(Color.WHITE); clothM.setDiffuseMap(Textures.cloth()); clothM.setSpecularColor(Color.gray(0.03));
        MeshView cloth = clothMesh(1.7, 2.6, 0.3, 1.94, 1.5, 0.0); cloth.setMaterial(clothM); cloth.setCullFace(CullFace.NONE); cloth.setMouseTransparent(true); decor.getChildren().add(cloth);
        PhongMaterial vaseM = pm(Color.web("#eee9e0")); vaseM.setSpecularColor(Color.gray(0.2)); vaseM.setSpecularPower(18);
        MeshView vase = lathe(new double[][]{{0, 0}, {0.30, 0}, {0.45, 0.14}, {0.54, 0.45}, {0.5, 0.78}, {0.38, 1.06}, {0.24, 1.26}, {0.2, 1.4}, {0.23, 1.5}, {0.2, 1.54}, {0.12, 1.5}, {0, 1.45}}, 36);
        vase.setMaterial(vaseM); vase.getTransforms().add(new Scale(0.9, 0.9, 0.9)); at(vase, 3.05, 0, -1.1); vase.setMouseTransparent(true); decor.getChildren().add(vase);
        double[] v0 = {3.05, 1.38, -1.1}, v1 = {2.0, 4.0, -1.1};
        Cylinder stem = between(v0, v1, 0.014); stem.setMaterial(pmc("#6b5a46")); stem.setMouseTransparent(true); decor.getChildren().add(stem);
        for (int k = 0; k < 13; k++) {
            double t = 0.16 + k * 0.066; double[] p = {v0[0] + (v1[0] - v0[0]) * t, v0[1] + (v1[1] - v0[1]) * t, v0[2]};
            double side = (k % 2 == 0 ? 1 : -1); double sz = 0.24 - k * 0.006;
            MeshView lf = ell(sz * 0.78, sz * 0.95, 0.02, 8, 14); lf.getTransforms().add(basis(p, new double[]{side * 0.7, 0.55, 0.0}, sz * 2, 0));
            lf.setMaterial(pmc(k % 3 == 0 ? "#b0a487" : k % 3 == 1 ? "#8a7f66" : "#756e58")); lf.setMouseTransparent(true); decor.getChildren().add(lf);
        }
        world.getChildren().add(decor); setBackground(1);
        double H = TOP - 0.10, RR = 0.32, RL = 0.20;
        double[] rBody = {0.02, RR, RR, 0.02}, rSlab = {RL, RR, RR, RL};
        Niche ni = new Niche(-1.85, 0.16, 1.46, 0.62, 0.18, 0.10, 0.05);
        Part body = reg(loft(-2.35, 2.9, -ZH, ZH, H, rBody, 0.07, 0, ni, 0, TOP), "shell", -1, new double[]{0.4, 0, 0}, woodM[0], true); body.wood = true;
        Part slab = reg(at(loft(-2.9, 2.9, -ZH, ZH, 0.10, rSlab, 0, 0.05, null, H, TOP), 0, H, 0), "shell", -1, new double[]{0, 0.9, 0}, woodM[0], true); slab.wood = true;
        Part foot = reg(loft(-2.9, -2.35, -ZH, ZH, Y0, rSlab, 0.02, 0, null, 0, TOP), "shell", -1, new double[]{-1.0, -0.3, 0}, woodM[0], true); foot.wood = true;
        Part svc = reg(at(box(4.2, 0.7, 0.012), 0, 0.6, -ZH - 0.007), "service", -1, new double[]{0, 0, -1.2}, woodM[0], true); svc.wood = true;
        reg(at(prism(rect(-1.85, 0.16, 1.46, 0.62, 0.18), 0.004, false), 0, 0, ZH - 0.10 + 0.006), "window", -1, new double[]{0, 0, 1.4}, glowM, true);
        PhongMaterial lm = new PhongMaterial(Color.WHITE); lm.setDiffuseMap(Textures.logo()); lm.setSpecularColor(Color.BLACK);
        reg(at(prism(rect(-0.50, -0.083, 0.50, 0.083, 0.01), 0.004, false), -0.25, 0.86, ZH + 0.003), "shell", -1, new double[]{0.4, 0, 0}, lm, true);
        reg(at(box(3.0, 0.03, 0.03), -0.25, 0.38, 0.62), "led", -1, new double[]{0, 0, 0.8}, glowM, false);
        double colH = Data.COL_H / 10, colW = Data.COL_W / 10, colD = Data.COL_D / 10, chL = Data.CH_L / 10, chD = Data.CH_D / 10, chH = Data.CH_H / 10, cwX = (-2.9 - 2.35) / 2;
        colWater = reg(at(box(colW, colH, colD), cwX, Y0 + colH / 2, 0), "tank", -1, new double[]{-1.3, 0, 0}, wat, true);
        baseWater = reg(at(box(chL, chH, chD), -0.2, Y0 + chH / 2, -0.1), "tank", -1, new double[]{0, -0.2, -0.6}, wat, true);
        PhongMaterial men = tp("#e8f6ff", 0.8); men.setSpecularColor(Color.gray(0.6));
        colMeniscus = reg(at(box(colW - 0.01, 0.012, colD - 0.01), cwX, Y0, 0), "tank", -1, new double[]{-1.3, 0, 0}, men, true);
        Part tc = reg(at(loft(-2.9, -2.35, -ZH, ZH, colH, new double[]{RL, 0.02, 0.02, RL}, 0, 0, null), 0, Y0, 0), "tank", -1, new double[]{-1.3, 0, 0}, glassM[0], true); tc.glass = true;
        Part tb = reg(at(box(chL + 0.04, chH + 0.04, chD + 0.04), -0.2, Y0 + chH / 2, -0.1), "tank", -1, new double[]{0, -0.2, -0.6}, glassM[0], true); tb.glass = true;
        PhongMaterial edge = tp("#ffffff", 0.5);
        reg(at(box(0.008, colH, 0.008), -2.352, Y0 + colH / 2, ZH - 0.004), "tank", -1, new double[]{-1.3, 0, 0}, edge, true);
        reg(at(flatRing(-0.15, 0.15, -0.15, 0.15, 0.15, 0.026, 0, 0.004), -2.62, TOP + 0.002, 0.56), "lid", -1, new double[]{-1.3, 1.0, 0}, grooveM, false);
        Part plug = reg(at(cyl(0.124, 0.01), -2.62, TOP + 0.004, 0.56), "lid", -1, new double[]{-1.3, 1.0, 0}, woodM[0], false); plug.wood = true;
        reg(at(box(0.03, colH * 0.9, 0.008), cwX, Y0 + colH / 2, -ZH - 0.004), "level", -1, new double[]{-1.3, 0, -0.5}, copper, false);
        reg(at(plate(rectR(1.50, -0.76, 2.86, 0.76, new double[]{0.1, 0.28, 0.28, 0.1}), 0.002), 0, TOP + 0.002, 0), "panel", -1, new double[]{0.5, 1.0, 0}, darkM, false);
        Part panel = reg(at(plate(rectR(1.52, -0.74, 2.84, 0.74, new double[]{0.08, 0.26, 0.26, 0.08}), 0.04), 0, TOP + 0.014, 0), "panel", -1, new double[]{0.5, 1.0, 0}, panelM, false);
        panel.n.setOnMouseClicked(e -> {
            if (!e.isStillSincePress()) return; Point2D uv = e.getPickResult().getIntersectedTexCoord();
            if (uv != null) for (int k = 0; k < 2; k++) if (Math.hypot((uv.getX() - ICON[k][0]) * DW, (uv.getY() - ICON[k][1]) * DH) < 80) { onPanelTap.accept(k); return; }
            onPick.accept("panel", -1);
        });
        showScreen("FloraDose", "Готовий", Color.web("#9ff0c0"));
        reg(at(box(3.6, 0.03, 0.07), -0.45, 1.12, TZ), "frame", -1, null, steel, false);
        for (int k = 0; k < 4; k++) reg(at(box(0.05, 0.56, 0.05), -2.0 + k * 1.1, 0.84, 0.12), "frame", -1, null, steel, false);
        reg(at(box(0.12, 0.03, 0.42), 1.84, 1.12, 0.0), "frame", -1, null, steel, false);
        for (int i = 0; i < 5; i++) {
            double x = XS[i], hx = HX[i]; double[] e = EXS;
            reg(at(flatRing(x - 0.415, x + 0.415, TZ - 0.35, TZ + 0.35, 0.31, 0.022, 0, 0.004), 0, TOP + 0.001, 0), "slot", i, e, grooveM, false);
            Part rp = reg(at(flatRing(x - TRAY_W / 2, x + TRAY_W / 2, TZ - TRAY_D / 2, TZ + TRAY_D / 2, 0.28, 0.07, 0, 0.05), 0, TOP, 0), "slot", i, e, trayM, false); rp.liner = true;
            MeshView soil = dome(0.325, 0.075, 0.255, 7, 28, 40 + i);
            Part sp = reg(at(soil, x, TOP + 0.02, TZ), "slot", i, e, soilM[i], false); sp.soil = true;
            Random pr = new Random(40 + i); int np = 3 + pr.nextInt(4);
            for (int k = 0; k < np; k++) {
                double th = (0.12 + 0.76 * pr.nextDouble()) * Math.PI, rho = 0.5 + 0.38 * pr.nextDouble(), sz = 0.03 + pr.nextDouble() * 0.035;
                double px = x + 0.325 * rho * Math.cos(th), pz = TZ + 0.255 * rho * Math.sin(th), py = TOP + 0.02 + 0.075 * Math.pow(Math.max(0, 1 - rho * rho), 0.55) + sz * 0.3;
                MeshView pb = ell(sz * (1 + pr.nextDouble() * 0.5), sz * 0.66, sz * (1 + pr.nextDouble() * 0.35), 6, 10); pb.getTransforms().add(new Rotate(pr.nextDouble() * 180, Rotate.Y_AXIS));
                reg(at(pb, px, py, pz), "slot", i, e, pmc(new String[]{"#b9ab95", "#9c8e79", "#cdbfa7", "#8b7c68"}[k % 4]), false);
            }
            reg(at(box(0.02, 0.1, 0.2), hx - 0.28, 1.25, TZ), "electrode", i, e, copper, false);
            reg(at(box(0.04, 0.008, 0.04), x - 0.14, TOP + 0.002, TZ + 0.42), "light", i, e, pm(Color.web("#3d4146")), false);
            reg(at(box(0.03, 0.008, 0.03), x + 0.14, TOP + 0.002, TZ + 0.42), "ntc", i, e, pm(Color.web("#4a3d34")), false);
            reg(at(box(0.04, 0.008, 0.03), x, TOP + 0.002, TZ + 0.42), "mic", i, e, pm(Color.web("#33373b")), false);
            reg(at(box(0.46, 0.045, 0.1), hx, 1.22, TZ), "loadcell", i, new double[]{0, 0.4, 0}, alu, false);
            reg(at(box(0.4, 0.015, 0.25), x, 1.05, 0.38), "slotboard", i, new double[]{0, 0.3, 0.4}, pcb, false);
        }
        double[] A = {-2.6, 0.30, 0.52}, B = {-2.22, 0.30, 0.52}, Cc = {-2.22, 0.74, 0.52}, D = {-2.22, 0.74, ZW}, E2 = {-2.06, 0.74, ZW};
        reg(between(A, B, 0.03), "tube", -1, null, tube, false);
        reg(between(B, Cc, 0.06), "filter", -1, new double[]{0, 0.1, 0.2}, pom, false);
        reg(between(Cc, D, 0.03), "tube", -1, null, tube, false);
        reg(between(D, E2, 0.08), "pump", -1, new double[]{0, 0.1, 0}, grey, false);
        reg(between(E2, new double[]{1.2, 0.74, ZW}, 0.05), "manifold", -1, new double[]{0, 0.1, 0}, pom, false);
        for (int i = 0; i < 5; i++) {
            double hx = HX[i];
            reg(at(box(0.16, 0.16, 0.16), hx, 1.0, ZW), "valve", i, new double[]{0, 0.3, 0}, pm(Color.web("#2a2a2a")), false);
            reg(at(cyl(0.06, 0.07), hx, 1.085, ZW), "valve", i, new double[]{0, 0.3, 0}, copper, false);
            reg(between(new double[]{hx, 0.74, ZW}, new double[]{hx, 0.92, ZW}, 0.03), "tube", -1, new double[]{0, 0.3, 0}, tube, false);
            reg(between(new double[]{hx, 1.12, ZW}, new double[]{hx, TOP + 0.03, ZW}, 0.03), "tube", -1, new double[]{0, 0.3, 0}, tube, false);
            paths[i] = new Point3D[]{pt(A), pt(B), pt(Cc), pt(D), pt(E2), pt(new double[]{hx, 0.74, ZW}), pt(new double[]{hx, TOP + 0.04, ZW})};
            for (int k = 0; k < 7; k++) { Sphere s = new Sphere(0.04 * S, 8); s.setMaterial(dropM); s.setVisible(false); s.setMouseTransparent(true); drops[i][k] = s; world.getChildren().add(s); }
            wire("cable", i, new double[][]{{hx + 0.1, 0.97, 0.55}, {hx + 0.1, 0.97, ZW}, {hx + 0.1, 1.10, ZW}}, 0.012, orange, null);
            wire("cable", i, new double[][]{{XS[i], 1.05, 0.52}, {XS[i], 1.05, 0.58}, {XS[i], 0.97, 0.58}}, 0.007, graphite, null);
        }
        wire("cable", 0, new double[][]{{1.9, 0.97, 0.0}, {1.9, 0.97, 0.55}, {HX[0] + 0.1, 0.97, 0.55}}, 0.02, orange, null);
        wire("cable", 0, new double[][]{{1.9, 1.01, 0.1}, {1.85, 1.01, 0.58}, {XS[0], 1.01, 0.58}}, 0.01, graphite, null);
        for (int k = 0; k < 5; k++) reg(at(box(0.04, 0.05, 0.05), -1.7 + k * 0.72, 0.97, 0.55), "frame", -1, null, clip, false);
        double[] pe = {0.5, -0.1, 0.5}, pz = {0.5, 0.1, 0.5};
        reg(at(box(0.78, 0.025, 1.2), 2.4, 0.14, 0), "pcb", -1, pe, pcb, false);
        reg(at(box(0.3, 0.045, 0.3), 2.3, 0.18, -0.3), "mcu", -1, pz, chip, false);
        reg(at(box(0.32, 0.045, 0.12), 2.3, 0.18, 0.3), "drv", -1, pz, abs, false);
        reg(at(box(0.22, 0.04, 0.16), 2.62, 0.18, -0.3), "mux", -1, pz, chip, false);
        for (int k = 0; k < 5; k++) reg(at(box(0.1, 0.07, 0.08), 2.05, 0.18, -0.5 + k * 0.25), "conn", -1, pz, pm(Color.web("#f2efe6")), false);
        reg(at(box(0.4, 0.22, 0.3), 2.5, 0.31, 0.45), "psu", -1, new double[]{0.5, 0.2, 0.3}, psuM, false);
        reg(between(new double[]{2.1, 0.75, -0.3}, new double[]{2.75, 0.75, -0.3}, 0.09), "batt", -1, new double[]{0.5, 0.4, 0.3}, batt, false);
        reg(at(box(0.04, 1.18, 1.4), 1.96, 0.66, 0), "partition", -1, new double[]{-0.2, 0, 0}, abs, false);
        reg(at(box(0.88, 1.16, 1.44), 2.42, 0.66, 0), "bay", -1, new double[]{0.7, 0, 0}, tp("#9aa0a8", 0.14), true);
        world.getChildren().addAll(late);
    }

    private void ensurePlants(int[] species) {
        for (int i = 0; i < 5; i++) { int sp = species[i]; if (!plantBuilt[i][sp]) { plantBuilt[i][sp] = true; addPlant(i, sp); dirty = true; } }
    }
    private void addPlant(int i, int sp) {
        List<PlantFactory.Group> gs = PlantFactory.build(sp, 100 + i * 7 + sp, XS[i], TOP + 0.07, TZ);
        int at = late.isEmpty() ? world.getChildren().size() : world.getChildren().indexOf(late.get(0));
        for (PlantFactory.Group g : gs) {
            if (g.mesh.isEmpty()) continue;
            TriangleMesh tm = new TriangleMesh(VertexFormat.POINT_TEXCOORD); tm.getPoints().setAll(g.mesh.fxPoints()); tm.getTexCoords().setAll(0, 0); tm.getFaces().setAll(g.mesh.fxFaces()); tm.getFaceSmoothingGroups().setAll(g.mesh.smoothing());
            Color c = Color.web(g.color); PhongMaterial m = new PhongMaterial(c); m.setSpecularColor(Color.gray(g.specular)); m.setSpecularPower(g.power);
            PhongMaterial dry = new PhongMaterial(c.interpolate(Color.web("#b9a45c"), 0.38)); dry.setSpecularColor(Color.gray(g.specular * 0.5)); dry.setSpecularPower(g.power);
            MeshView v = new MeshView(tm); Part p = reg(v, "slot", i, EXS, m, true); v.setCullFace(CullFace.BACK); p.plant = true; p.sp = sp; p.alt = dry;
            world.getChildren().remove(v); world.getChildren().add(Math.min(at, world.getChildren().size()), v); late.remove(v); at++;
        }
    }

    private void placeWater() {
        for (Part p : new Part[]{colWater, baseWater, colMeniscus}) p.n.setTranslateY(p.base[1] + p.ex[1] * p.ph);
    }
    void refresh(String sel, int selIdx, Mode mode, Set<String> hid, Set<String> grp, int cfg, int[] species, int[] vs, boolean[] alert, double[] moist, double tank, int ledState, Color led, double exA) {
        ensurePlants(species); ledTarget = led;
        if (cfg != lastCfg) { lastCfg = cfg; dirty = true; panelM.setSpecularColor(Color.gray(cfg == 1 ? 1.0 : 0.85)); panelM.setSpecularPower(cfg == 1 ? 140 : 110); }
        for (int i = 0; i < 5; i++) if (Math.abs(moist[i] - soilShown[i]) > 2) { soilShown[i] = moist[i]; double w = Math.max(0, Math.min(1, moist[i] / 100.0)); soilM[i].setDiffuseColor(Color.WHITE.interpolate(Color.web("#5b5148"), w * 0.85)); soilM[i].setSpecularColor(Color.gray(0.04 + 0.14 * w)); }
        double lvl = Data.waterLevelCm(tank) / 10.0, hc = Math.max(0.003, Math.min(lvl, Data.COL_H / 10)), hb = Math.max(0.003, Math.min(lvl, Data.CH_H / 10));
        colWater.n.setScaleY(hc / (Data.COL_H / 10)); colWater.base[1] = -(Y0 + hc / 2) * S; baseWater.n.setScaleY(hb / (Data.CH_H / 10)); baseWater.base[1] = -(Y0 + hb / 2) * S;
        colMeniscus.base[1] = -(Y0 + hc) * S; colMeniscus.n.setOpacity(tank < 0.5 ? 0 : 1);
        int h = Objects.hash(sel, selIdx, mode, hid.hashCode(), grp.hashCode(), cfg, Arrays.hashCode(species), Arrays.hashCode(vs), Arrays.hashCode(alert), Math.round(exA * 200), isolId, isolIdx);
        if (!dirty && h == lastHash) { placeWater(); return; }
        lastHash = h; dirty = false;
        for (Part p : parts) {
            Data.C c = Data.COMPS.get(p.id);
            boolean vis = !hid.contains(p.id) && !(mode == Mode.MODEL && c.internal && exA < 0.12);
            if (p.plant && species[p.idx] != p.sp) vis = false;
            if (p.plant && grp.contains("plants")) vis = false;
            if (SHELL.contains(p.id) && grp.contains("shell")) vis = false;
            if (!p.plant && c.cat.equals("water") && grp.contains("water")) vis = false;
            if (c.cat.equals("el") && grp.contains("el")) vis = false;
            if (p.idx >= 0 && !Data.instanceVisible(p.id, p.idx, cfg)) vis = false;
            if (isolId != null && !(p.id.equals(isolId) && (isolIdx < 0 || p.idx == isolIdx))) vis = false;
            p.n.setVisible(vis);
            boolean gh = GHOSTED.contains(p.id) && mode != Mode.MODEL;
            p.n.setMouseTransparent(gh);
            PhongMaterial m = p.mat;
            if (p.wood) m = woodM[cfg];
            if (p.glass) m = glassM[cfg];
            if (p.id.equals("valve") && p.n instanceof Box) m = vs[p.idx] == 1 ? open : vs[p.idx] == 2 ? red : p.mat;
            if (p.liner && alert[p.idx]) m = red;
            if (p.plant && alert[p.idx] && p.alt != null) m = p.alt;
            if (gh) m = ghost;
            else if (c.internal && ((mode == Mode.EL && c.cat.equals("water")) || (mode == Mode.WATER && c.cat.equals("el")) || (mode == Mode.SENS && !SENSORS.contains(p.id)))) m = dim;
            boolean selected = p.id.equals(sel) && (selIdx < 0 || p.idx == selIdx);
            if (p.id.equals("shell") || p.id.equals("service") || p.plant) selected = false;
            if (selected) m = gh ? hlGhost : hl;
            if (p.id.equals("led") && !selected) m = glowM;
            p.n.setMaterial(m);
            double ph = Math.max(0, Math.min(1, (exA - p.delay) / (1 - p.delay + 1e-9))); ph = ph * ph * (3 - 2 * ph); p.ph = ph;
            p.n.setTranslateX(p.base[0] + p.ex[0] * ph); p.n.setTranslateY(p.base[1] + p.ex[1] * ph); p.n.setTranslateZ(p.base[2] + p.ex[2] * ph);
        }
    }
    void tick(double t, boolean[] flow) {
        double dr = ledTarget.getRed() - ledCur.getRed(), dg = ledTarget.getGreen() - ledCur.getGreen(), db = ledTarget.getBlue() - ledCur.getBlue();
        if (Math.abs(dr) + Math.abs(dg) + Math.abs(db) > 0.004) ledCur = Color.color(ledCur.getRed() + dr * 0.08, ledCur.getGreen() + dg * 0.08, ledCur.getBlue() + db * 0.08); else ledCur = ledTarget;
        int lv = (int) Math.round(ledLevel * 3);
        int key = ((int) (ledCur.getRed() * 15) << 8) | ((int) (ledCur.getGreen() * 15) << 4) | (int) (ledCur.getBlue() * 15) | (lv << 12);
        if (key != glowKey) {
            glowKey = key; Image im = Textures.glow(ledCur, lv / 3.0);
            if (lv == 0) { glowM.setDiffuseColor(Color.WHITE); glowM.setDiffuseMap(im); glowM.setSelfIlluminationMap(null); }
            else { glowM.setDiffuseMap(null); glowM.setDiffuseColor(Color.BLACK); glowM.setSelfIlluminationMap(im); }

            double k = lv / 3.0; glowPlaneM.setDiffuseColor(Color.color(ledCur.getRed() * 0.5 * k, ledCur.getGreen() * 0.5 * k, ledCur.getBlue() * 0.5 * k));
            warmL.setColor(Color.color(ledCur.getRed() * 0.3 * k, ledCur.getGreen() * 0.3 * k, ledCur.getBlue() * 0.3 * k));
        }
        wallBox.setVisible(Math.cos(Math.toRadians(ry.getAngle())) > -0.35 || bgMode != 1);
        for (int i = 0; i < 5; i++) for (int k = 0; k < 7; k++) {
            Sphere s = drops[i][k]; s.setVisible(flow[i]);
            if (!flow[i]) continue;
            double u = (t * 0.6 + k / 7.0) % 1.0, tot = 0; Point3D[] P = paths[i];
            double[] L = new double[P.length - 1];
            for (int j = 0; j < L.length; j++) { L[j] = P[j].distance(P[j + 1]); tot += L[j]; }
            double d = u * tot;
            for (int j = 0; j < L.length; j++) {
                if (d <= L[j]) { Point3D q = P[j].add(P[j + 1].subtract(P[j]).multiply(d / L[j])); s.setTranslateX(q.getX()); s.setTranslateY(q.getY()); s.setTranslateZ(q.getZ()); break; }
                d -= L[j];
            }
        }
    }
}
