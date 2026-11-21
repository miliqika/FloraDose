package ua.floradose;

import java.util.Arrays;

final class MeshBuilder {
    static final double UNIT = 100;
    static final int G_TOP = 1, G_BOTTOM = 2, G_STEM = 4, G_BODY = 8;

    private double[] p = new double[3 * 256];
    private int[] f = new int[3 * 256], g = new int[256];
    private int np, nf;

    static final class Leaf {
        double len = 0.3, wid = 0.1, thick = 0.01;
        double peak = 0.5;
        double full = 1.0;
        double baseW = 0;
        double cup = 0, fold = 0;
        double arch = 0;
        double twist = 0;
        int segs = 6, cols = 5;
        Leaf size(double len, double wid, double thick) { this.len = len; this.wid = wid; this.thick = thick; return this; }
        Leaf shape(double peak, double full) { this.peak = peak; this.full = full; return this; }
        Leaf curl(double cup, double fold, double arch) { this.cup = cup; this.fold = fold; this.arch = arch; return this; }
    }

    int vertexCount() { return np; }
    int triangleCount() { return nf; }
    boolean isEmpty() { return nf == 0; }

    int vertex(double x, double y, double z) {
        if (3 * np + 3 > p.length) p = Arrays.copyOf(p, p.length * 2);
        p[3 * np] = x; p[3 * np + 1] = y; p[3 * np + 2] = z;
        return np++;
    }
    double[] vertexAt(int i) { return new double[]{p[3 * i], p[3 * i + 1], p[3 * i + 2]}; }

    void tri(int a, int b, int c, int group, double hx, double hy, double hz) {
        double ux = p[3 * b] - p[3 * a], uy = p[3 * b + 1] - p[3 * a + 1], uz = p[3 * b + 2] - p[3 * a + 2];
        double vx = p[3 * c] - p[3 * a], vy = p[3 * c + 1] - p[3 * a + 1], vz = p[3 * c + 2] - p[3 * a + 2];
        double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        if (nx * nx + ny * ny + nz * nz < 1e-18) return;
        if (nx * hx + ny * hy + nz * hz < 0) { int t = b; b = c; c = t; }
        if (3 * nf + 3 > f.length) f = Arrays.copyOf(f, f.length * 2);
        if (nf + 1 > g.length) g = Arrays.copyOf(g, g.length * 2);
        f[3 * nf] = a; f[3 * nf + 1] = b; f[3 * nf + 2] = c; g[nf] = group; nf++;
    }

    double[] bounds() {
        double[] r = {1e18, 1e18, 1e18, -1e18, -1e18, -1e18};
        for (int i = 0; i < np; i++) for (int k = 0; k < 3; k++) { double v = p[3 * i + k]; r[k] = Math.min(r[k], v); r[3 + k] = Math.max(r[3 + k], v); }
        return r;
    }
    void scaleAbout(double k, double cx, double cy, double cz) { for (int i = 0; i < np; i++) { p[3 * i] = cx + (p[3 * i] - cx) * k; p[3 * i + 1] = cy + (p[3 * i + 1] - cy) * k; p[3 * i + 2] = cz + (p[3 * i + 2] - cz) * k; } }
    boolean allFinite() { for (int i = 0; i < 3 * np; i++) if (Double.isNaN(p[i]) || Double.isInfinite(p[i])) return false; return true; }

    float[] fxPoints() { float[] r = new float[3 * np]; for (int i = 0; i < np; i++) { r[3 * i] = (float) (p[3 * i] * UNIT); r[3 * i + 1] = (float) (-p[3 * i + 1] * UNIT); r[3 * i + 2] = (float) (-p[3 * i + 2] * UNIT); } return r; }
    int[] fxFaces() { int[] r = new int[6 * nf]; for (int i = 0; i < nf; i++) for (int k = 0; k < 3; k++) { r[6 * i + 2 * k] = f[3 * i + k]; r[6 * i + 2 * k + 1] = 0; } return r; }
    int[] smoothing() { return Arrays.copyOf(g, nf); }

    void ellipsoid(double cx, double cy, double cz, double a, double b, double c, double yaw, int stacks, int slices) {
        int[][] id = new int[stacks + 1][slices + 1]; double cy0 = Math.cos(yaw), sy = Math.sin(yaw);
        for (int i = 0; i <= stacks; i++) for (int j = 0; j <= slices; j++) {
            double ph = Math.PI * i / stacks, th = 2 * Math.PI * j / slices;
            double lx = a * Math.sin(ph) * Math.cos(th), ly = b * Math.cos(ph), lz = c * Math.sin(ph) * Math.sin(th);
            id[i][j] = vertex(cx + lx * cy0 + lz * sy, cy + ly, cz - lx * sy + lz * cy0);
        }
        for (int i = 0; i < stacks; i++) for (int j = 0; j < slices; j++) {
            int[] q = {id[i][j], id[i + 1][j], id[i + 1][j + 1], id[i][j + 1]};
            double[] m = centroid(q); double hx = m[0] - cx, hy = m[1] - cy, hz = m[2] - cz;
            tri(q[0], q[1], q[2], G_BODY, hx, hy, hz); tri(q[0], q[2], q[3], G_BODY, hx, hy, hz);
        }
    }
    private double[] centroid(int[] q) { double x = 0, y = 0, z = 0; for (int i : q) { x += p[3 * i]; y += p[3 * i + 1]; z += p[3 * i + 2]; } return new double[]{x / q.length, y / q.length, z / q.length}; }

    void tube(double[] a, double[] b, double r0, double r1, int sides) {
        double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2], len = Math.sqrt(dx * dx + dy * dy + dz * dz); if (len < 1e-9) return;
        dx /= len; dy /= len; dz /= len;
        double[] ref = Math.abs(dy) < 0.9 ? new double[]{0, 1, 0} : new double[]{1, 0, 0};
        double ux = dy * ref[2] - dz * ref[1], uy = dz * ref[0] - dx * ref[2], uz = dx * ref[1] - dy * ref[0], ul = Math.sqrt(ux * ux + uy * uy + uz * uz); ux /= ul; uy /= ul; uz /= ul;
        double wx = dy * uz - dz * uy, wy = dz * ux - dx * uz, wz = dx * uy - dy * ux;
        int[] lo = new int[sides], hi = new int[sides]; double[][] dir = new double[sides][];
        for (int k = 0; k < sides; k++) {
            double an = 2 * Math.PI * k / sides, c = Math.cos(an), s = Math.sin(an); double ox = ux * c + wx * s, oy = uy * c + wy * s, oz = uz * c + wz * s; dir[k] = new double[]{ox, oy, oz};
            lo[k] = vertex(a[0] + ox * r0, a[1] + oy * r0, a[2] + oz * r0); hi[k] = vertex(b[0] + ox * r1, b[1] + oy * r1, b[2] + oz * r1);
        }
        for (int k = 0; k < sides; k++) { int n = (k + 1) % sides; double[] h = dir[k];
            tri(lo[k], lo[n], hi[n], G_STEM, h[0], h[1], h[2]); tri(lo[k], hi[n], hi[k], G_STEM, h[0], h[1], h[2]); }
    }
    void stem(double[][] pts, double r0, double r1, int sides) {
        int n = pts.length - 1; for (int i = 0; i < n; i++) tube(pts[i], pts[i + 1], r0 + (r1 - r0) * i / n, r0 + (r1 - r0) * (i + 1) / n, sides);
    }

    private static double width(Leaf s, double t) {
        double k = Math.log(0.5) / Math.log(Math.max(0.05, Math.min(0.95, s.peak)));
        double v = Math.pow(Math.max(0, Math.sin(Math.PI * Math.pow(t, k))), s.full);
        return s.wid * Math.max(v, s.baseW * (1 - t));
    }
    void leaf(Leaf s, double bx, double by, double bz, double yaw, double pitch, double roll) {
        int NS = s.segs, NC = s.cols % 2 == 0 ? s.cols + 1 : s.cols;
        double hx = Math.cos(yaw), hz = Math.sin(yaw), sx = -Math.sin(yaw), sz = Math.cos(yaw), dt = s.len / NS;
        int[][] top = new int[NS + 1][NC], bot = new int[NS + 1][NC];
        double cx = bx, cy = by, cz = bz;
        for (int i = 0; i <= NS; i++) {
            double t = (double) i / NS, phi = pitch - s.arch * t;
            double tx = Math.cos(phi) * hx, ty = Math.sin(phi), tz = Math.cos(phi) * hz;
            double nx = -Math.sin(phi) * hx, ny = Math.cos(phi), nz = -Math.sin(phi) * hz;
            double rr = roll + s.twist * t, cr = Math.cos(rr), sr = Math.sin(rr);
            double n2x = nx * cr + sx * sr, n2y = ny * cr, n2z = nz * cr + sz * sr, b2x = -nx * sr + sx * cr, b2y = -ny * sr, b2z = -nz * sr + sz * cr;
            double w = width(s, t), th = s.thick * Math.sin(Math.PI * Math.min(1, Math.max(0, t * 0.9 + 0.05)));
            for (int j = 0; j < NC; j++) {
                double u = -1 + 2.0 * j / (NC - 1), lift = s.cup * w * u * u + s.fold * w * 0.5 * Math.abs(u), half = th / 2 * Math.sqrt(Math.max(0, 1 - u * u));
                double qx = cx + b2x * u * w / 2, qy = cy + b2y * u * w / 2, qz = cz + b2z * u * w / 2;
                top[i][j] = vertex(qx + n2x * (lift + half), qy + n2y * (lift + half), qz + n2z * (lift + half));
                bot[i][j] = vertex(qx + n2x * (lift - half), qy + n2y * (lift - half), qz + n2z * (lift - half));
            }
            cx += tx * dt; cy += ty * dt; cz += tz * dt;
            if (i == 0) { }
        }
        for (int i = 0; i < NS; i++) for (int j = 0; j < NC - 1; j++) {
            double[] a = vertexAt(top[i][j]), b = vertexAt(bot[i][j]); double hxx = a[0] - b[0], hyy = a[1] - b[1], hzz = a[2] - b[2];
            if (hxx * hxx + hyy * hyy + hzz * hzz < 1e-12) { hxx = -Math.sin(pitch) * hx; hyy = Math.cos(pitch); hzz = -Math.sin(pitch) * hz; }
            tri(top[i][j], top[i][j + 1], top[i + 1][j + 1], G_TOP, hxx, hyy, hzz); tri(top[i][j], top[i + 1][j + 1], top[i + 1][j], G_TOP, hxx, hyy, hzz);
            tri(bot[i][j], bot[i][j + 1], bot[i + 1][j + 1], G_BOTTOM, -hxx, -hyy, -hzz); tri(bot[i][j], bot[i + 1][j + 1], bot[i + 1][j], G_BOTTOM, -hxx, -hyy, -hzz);
        }
    }

    void lobedLeaf(double R, int lobes, double notch, double cup, double bx, double by, double bz, double yaw, double pitch, double roll, int rings, int pts) {
        double hx = Math.cos(yaw), hz = Math.sin(yaw), sx = -Math.sin(yaw), sz = Math.cos(yaw);
        double tx = Math.cos(pitch) * hx, ty = Math.sin(pitch), tz = Math.cos(pitch) * hz, nx = -Math.sin(pitch) * hx, ny = Math.cos(pitch), nz = -Math.sin(pitch) * hz;
        double cr = Math.cos(roll), sr = Math.sin(roll); double n2x = nx * cr + sx * sr, n2y = ny * cr, n2z = nz * cr + sz * sr, b2x = -nx * sr + sx * cr, b2y = -ny * sr, b2z = -nz * sr + sz * cr;
        double ccx = bx + tx * R * 0.82, ccy = by + ty * R * 0.82, ccz = bz + tz * R * 0.82, gap = 0.42;
        int[][] top = new int[rings + 1][pts + 1], bot = new int[rings + 1][pts + 1]; int ct = vertex(ccx + n2x * 0.004, ccy + n2y * 0.004, ccz + n2z * 0.004), cb = vertex(ccx - n2x * 0.004, ccy - n2y * 0.004, ccz - n2z * 0.004);
        for (int k = 1; k <= rings; k++) for (int j = 0; j <= pts; j++) {
            double al = -Math.PI + gap + (2 * Math.PI - 2 * gap) * j / pts, lobe = Math.pow(0.5 + 0.5 * Math.cos(lobes * al), 0.7), serr = 1 + 0.045 * Math.cos(lobes * al * 3);
            double rho = (double) k / rings * R * (notch + (1 - notch) * lobe) * serr, ca = Math.cos(al), sa = Math.sin(al), lift = cup * (rho / R) * (rho / R) * R * 0.45;
            double qx = ccx + tx * rho * ca + b2x * rho * sa, qy = ccy + ty * rho * ca + b2y * rho * sa, qz = ccz + tz * rho * ca + b2z * rho * sa;
            top[k][j] = vertex(qx + n2x * (lift + 0.004), qy + n2y * (lift + 0.004), qz + n2z * (lift + 0.004)); bot[k][j] = vertex(qx + n2x * (lift - 0.004), qy + n2y * (lift - 0.004), qz + n2z * (lift - 0.004));
        }
        for (int j = 0; j < pts; j++) {
            tri(ct, top[1][j], top[1][j + 1], G_TOP, n2x, n2y, n2z); tri(cb, bot[1][j], bot[1][j + 1], G_BOTTOM, -n2x, -n2y, -n2z);
            for (int k = 1; k < rings; k++) {
                tri(top[k][j], top[k + 1][j], top[k + 1][j + 1], G_TOP, n2x, n2y, n2z); tri(top[k][j], top[k + 1][j + 1], top[k][j + 1], G_TOP, n2x, n2y, n2z);
                tri(bot[k][j], bot[k + 1][j], bot[k + 1][j + 1], G_BOTTOM, -n2x, -n2y, -n2z); tri(bot[k][j], bot[k + 1][j + 1], bot[k][j + 1], G_BOTTOM, -n2x, -n2y, -n2z);
            }
        }
    }
}
