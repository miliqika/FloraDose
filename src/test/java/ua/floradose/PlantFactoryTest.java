package ua.floradose;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class PlantFactoryTest {
    private static List<PlantFactory.Group> make(int sp, long seed) { return PlantFactory.build(sp, seed, 0, 0, 0); }

    @Test void everySpeciesProducesFiniteNonEmptyGeometry() {
        for (int sp = 0; sp < Data.PLANTS.length; sp++) {
            List<PlantFactory.Group> g = make(sp, 100 + sp); assertFalse(g.isEmpty(), "вид " + sp); assertTrue(PlantFactory.triangles(g) > 500, "достатньо деталей: вид " + sp);
            for (PlantFactory.Group x : g) { assertTrue(x.mesh.allFinite(), "NaN/∞ у виді " + sp); assertNotNull(x.color); }
        }
    }
    @Test void facesReferenceExistingVertices() {
        for (int sp = 0; sp < Data.PLANTS.length; sp++) for (PlantFactory.Group x : make(sp, 7)) {
            if (x.mesh.isEmpty()) continue; int nv = x.mesh.vertexCount(), nf = x.mesh.triangleCount();
            int[] f = x.mesh.fxFaces(); assertEquals(6 * nf, f.length); for (int k = 0; k < f.length; k += 2) assertTrue(f[k] >= 0 && f[k] < nv, "індекс вершини");
            assertEquals(3 * nv, x.mesh.fxPoints().length); assertEquals(nf, x.mesh.smoothing().length);
        }
    }
    @Test void sameSeedGivesSameShapeAndOtherSeedVariesIt() {
        for (int sp = 0; sp < Data.PLANTS.length; sp++) {
            double[] a = PlantFactory.bounds(make(sp, 11)), b = PlantFactory.bounds(make(sp, 11)), c = PlantFactory.bounds(make(sp, 12345)); assertArrayEquals(a, b, 0, "детермінованість, вид " + sp); assertFalse(Arrays.equals(a, c), "інше зерно — інша форма, вид " + sp);
        }
    }
    @Test void speciesAreVisuallyDistinct() {
        Set<Integer> tris = new HashSet<>(); Set<String> colours = new HashSet<>();
        for (int sp = 0; sp < Data.PLANTS.length; sp++) { List<PlantFactory.Group> g = make(sp, 3); tris.add(PlantFactory.triangles(g)); StringBuilder k = new StringBuilder(); for (PlantFactory.Group x : g) k.append(x.color); colours.add(k.toString()); }
        assertEquals(Data.PLANTS.length, tris.size(), "різна кількість трикутників"); assertEquals(Data.PLANTS.length, colours.size(), "різні палітри");
    }
    @Test void plantsStandOnTheirBaseAndFitTheSlot() {
        double slotR = 1.45;
        for (int sp = 0; sp < Data.PLANTS.length; sp++) {
            double[] b = PlantFactory.bounds(make(sp, 100 + sp));
            assertEquals(0, b[1], 0.05, "основа рослини на рівні вставки, вид " + sp); assertTrue(b[4] > 0.4 && b[4] < 2.2, "висота " + b[4] + ", вид " + sp);
            assertTrue(b[0] > -slotR && b[3] < slotR && b[2] > -slotR && b[5] < slotR, "ширина в межах слота, вид " + sp);
        }
        double[] d = PlantFactory.bounds(make(0, 1)), e = PlantFactory.bounds(make(1, 1)); assertTrue(d[4] > 2 * e[4], "драцена значно вища за розетку ехеверії");
    }
    @Test void positionOffsetsTheWholePlant() {
        double[] o = PlantFactory.bounds(make(2, 5)), m = PlantFactory.bounds(PlantFactory.build(2, 5, 1.5, 2.0, -0.5));
        assertEquals(o[0] + 1.5, m[0], 1e-6); assertEquals(o[1] + 2.0, m[1], 1e-6); assertEquals(o[5] - 0.5, m[5], 1e-6);
    }
    @Test void scaleAboutKeepsPivotFixed() {
        MeshBuilder mb = new MeshBuilder(); int a = mb.vertex(1, 0, 0), b = mb.vertex(2, 1, 0), c = mb.vertex(1, 1, 1); mb.tri(a, b, c, 0, 0, 0, 1);
        mb.scaleAbout(2, 1, 0, 0); assertArrayEquals(new double[]{1, 0, 0}, mb.vertexAt(a), 1e-12); assertArrayEquals(new double[]{3, 2, 0}, mb.vertexAt(b), 1e-12); assertArrayEquals(new double[]{1, 2, 2}, mb.vertexAt(c), 1e-12);
    }
    @Test void triangleOrientationFollowsHint() {
        MeshBuilder mb = new MeshBuilder(); int a = mb.vertex(0, 0, 0), b = mb.vertex(1, 0, 0), c = mb.vertex(0, 0, 1);
        mb.tri(a, b, c, 0, 0, 1, 0); int[] up = mb.fxFaces(); MeshBuilder m2 = new MeshBuilder(); a = m2.vertex(0, 0, 0); b = m2.vertex(1, 0, 0); c = m2.vertex(0, 0, 1); m2.tri(a, b, c, 0, 0, -1, 0); int[] down = m2.fxFaces();
        assertEquals(up[2], down[4], "протилежна підказка міняє порядок вершин"); assertEquals(up[4], down[2]);
    }
}
