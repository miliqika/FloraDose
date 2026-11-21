package ua.floradose;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class JsonTest {
    @Test void parsesScalarsObjectsAndArrays() {
        Map<String, Object> m = Json.obj(Json.parse("{\"a\": 1, \"b\": [true, false, null, \"x\"], \"c\": {\"d\": -2.5e1}}"));
        assertEquals(1.0, m.get("a")); List<Object> b = Json.arr(m.get("b")); assertEquals(4, b.size()); assertEquals(Boolean.TRUE, b.get(0)); assertNull(b.get(2)); assertEquals("x", b.get(3));
        assertEquals(-25.0, Json.obj(m.get("c")).get("d"));
    }
    @Test void keepsKeyOrder() { assertEquals("[z, a, m]", Json.obj(Json.parse("{\"z\":1,\"a\":2,\"m\":3}")).keySet().toString()); }
    @Test void parsesEscapesAndUnicode() {
        assertEquals("a\"b\\c/d\n\t\u0416", Json.parse("\"a\\\"b\\\\c\\/d\\n\\t\\u0416\""));
        assertEquals("Полив 70 мл", Json.parse("\"Полив 70 мл\""));
    }
    @Test void skipsBomAndWhitespace() { assertEquals(7.0, Json.parse("\uFEFF \n\t 7 \r\n")); }
    @Test void emptyContainers() { assertTrue(Json.obj(Json.parse("{}")).isEmpty()); assertTrue(Json.arr(Json.parse("[ ]")).isEmpty()); }
    @Test void rejectsMalformedInput() {
        String[] bad = {"", "   ", "{", "[1,", "[1,]", "{\"a\":1,}", "{'a':1}", "{\"a\" 1}", "[1 2]", "01", "1.", "-", ".5", "+1", "NaN", "Infinity", "tru", "nul", "\"abc", "\"a\nb\"", "\"\\x\"", "\"\\u12\"", "\"\\uZZZZ\"", "1 2", "{} {}", "{\"a\":1}x", "1e999"};
        for (String t : bad) assertThrows(Json.ParseException.class, () -> Json.parse(t), "має бути відхилено: «" + t + "»");
        assertThrows(Json.ParseException.class, () -> Json.parse(null));
    }
    @Test void limitsNestingDepth() {
        StringBuilder ok = new StringBuilder(), deep = new StringBuilder(); for (int i = 0; i < 40; i++) ok.append('['); for (int i = 0; i < 40; i++) ok.append(']');
        assertNotNull(Json.parse(ok.toString()));
        for (int i = 0; i < 5000; i++) deep.append('['); assertThrows(Json.ParseException.class, () -> Json.parse(deep.toString()));
    }
    @Test void parseErrorCarriesPosition() { Json.ParseException e = assertThrows(Json.ParseException.class, () -> Json.parse("[1, 2, x]")); assertEquals(7, e.pos); assertTrue(e.getMessage().contains("7")); }
    @Test void roundTripPreservesStructure() {
        String src = "{\"name\":\"Фікус «S5»\",\"n\":[1,2.5,-3],\"nested\":{\"ok\":true,\"none\":null,\"list\":[{\"k\":1},{\"k\":2}]},\"empty\":{},\"e\":[]}";
        Object first = Json.parse(src); Object again = Json.parse(Json.write(first)); assertEquals(first, again);
    }
    @Test void writesIntegersWithoutFraction() { assertEquals("{\n  \"a\": 3,\n  \"b\": 2.5\n}\n", Json.write(new java.util.LinkedHashMap<String, Object>() {{ put("a", 3.0); put("b", 2.5); }})); }
    @Test void writesEscapedStrings() { String t = Json.write("a\"b\\c\n\u0001"); assertEquals("\"a\\\"b\\\\c\\n\\u0001\"\n", t); assertEquals("a\"b\\c\n\u0001", Json.parse(t)); }
    @Test void refusesNonFiniteNumbersAndUnknownTypes() {
        assertThrows(IllegalArgumentException.class, () -> Json.write(Double.NaN)); assertThrows(IllegalArgumentException.class, () -> Json.write(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Json.write(new Object()));
    }
    @Test void safeAccessorsReturnNullForWrongTypes() { assertNull(Json.obj("x")); assertNull(Json.arr(1.0)); assertNull(Json.obj(null)); }
}
