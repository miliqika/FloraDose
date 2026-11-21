package ua.floradose;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    static final class ParseException extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        final int pos;
        ParseException(String msg, int pos) { super(msg + " (позиція " + pos + ")"); this.pos = pos; }
    }
    private static final int MAX_DEPTH = 48;
    private final String s; private int i;
    private Json(String s) { this.s = s; }

    static Object parse(String text) {
        if (text == null) throw new ParseException("порожній вхід", 0);
        Json j = new Json(text.startsWith("﻿") ? text.substring(1) : text); j.ws(); Object v = j.value(0); j.ws();
        if (j.i != j.s.length()) throw new ParseException("зайві символи після значення", j.i);
        return v;
    }

    private void ws() { while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\n' || s.charAt(i) == '\r' || s.charAt(i) == '\t')) i++; }
    private char peek() { if (i >= s.length()) throw new ParseException("несподіваний кінець тексту", i); return s.charAt(i); }
    private void expect(char c) { if (peek() != c) throw new ParseException("очікувалося «" + c + "»", i); i++; }

    private Object value(int depth) {
        if (depth > MAX_DEPTH) throw new ParseException("занадто глибока вкладеність", i);
        char c = peek();
        switch (c) {
            case '{': return object(depth);
            case '[': return array(depth);
            case '"': return string();
            case 't': return literal("true", Boolean.TRUE);
            case 'f': return literal("false", Boolean.FALSE);
            case 'n': return literal("null", null);
            default:
                if (c == '-' || (c >= '0' && c <= '9')) return number();
                throw new ParseException("неочікуваний символ «" + c + "»", i);
        }
    }
    private Object literal(String w, Object v) { if (!s.startsWith(w, i)) throw new ParseException("очікувалося " + w, i); i += w.length(); return v; }
    private Map<String, Object> object(int depth) {
        expect('{'); Map<String, Object> m = new LinkedHashMap<>(); ws();
        if (peek() == '}') { i++; return m; }
        while (true) {
            ws(); if (peek() != '"') throw new ParseException("очікувався ключ-рядок", i); String k = string(); ws(); expect(':'); ws();
            m.put(k, value(depth + 1)); ws();
            char c = peek(); i++;
            if (c == '}') return m; if (c != ',') throw new ParseException("очікувалося «,» або «}»", i - 1);
        }
    }
    private List<Object> array(int depth) {
        expect('['); List<Object> a = new ArrayList<>(); ws();
        if (peek() == ']') { i++; return a; }
        while (true) {
            ws(); a.add(value(depth + 1)); ws();
            char c = peek(); i++;
            if (c == ']') return a; if (c != ',') throw new ParseException("очікувалося «,» або «]»", i - 1);
        }
    }
    private String string() {
        expect('"'); StringBuilder b = new StringBuilder();
        while (true) {
            char c = peek(); i++;
            if (c == '"') return b.toString();
            if (c < 0x20) throw new ParseException("керівний символ у рядку", i - 1);
            if (c != '\\') { b.append(c); continue; }
            char e = peek(); i++;
            switch (e) {
                case '"': b.append('"'); break; case '\\': b.append('\\'); break; case '/': b.append('/'); break; case 'b': b.append('\b'); break;
                case 'f': b.append('\f'); break; case 'n': b.append('\n'); break; case 'r': b.append('\r'); break; case 't': b.append('\t'); break;
                case 'u':
                    if (i + 4 > s.length()) throw new ParseException("обірваний \\u-код", i);
                    try { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); } catch (NumberFormatException ex) { throw new ParseException("некоректний \\u-код", i); }
                    i += 4; break;
                default: throw new ParseException("некоректна escape-послідовність", i - 1);
            }
        }
    }
    private Double number() {
        int st = i; if (peek() == '-') i++;
        if (i >= s.length()) throw new ParseException("число обірване", st);
        if (s.charAt(i) == '0') i++; else if (s.charAt(i) >= '1' && s.charAt(i) <= '9') { while (i < s.length() && Character.isDigit(s.charAt(i))) i++; } else throw new ParseException("некоректне число", st);
        if (i < s.length() && s.charAt(i) == '.') { i++; int d = i; while (i < s.length() && Character.isDigit(s.charAt(i))) i++; if (d == i) throw new ParseException("некоректна дробова частина", st); }
        if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) { i++; if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++; int d = i; while (i < s.length() && Character.isDigit(s.charAt(i))) i++; if (d == i) throw new ParseException("некоректний порядок", st); }
        double v = Double.parseDouble(s.substring(st, i));
        if (Double.isInfinite(v)) throw new ParseException("число поза діапазоном", st);
        return v;
    }

    static String write(Object o) { StringBuilder b = new StringBuilder(); write(b, o, 0); return b.append('\n').toString(); }
    private static void indent(StringBuilder b, int n) { for (int k = 0; k < n; k++) b.append("  "); }
    @SuppressWarnings("unchecked")
    private static void write(StringBuilder b, Object o, int lvl) {
        if (o == null) b.append("null");
        else if (o instanceof Boolean) b.append(o);
        else if (o instanceof Number) {
            double d = ((Number) o).doubleValue(); if (Double.isNaN(d) || Double.isInfinite(d)) throw new IllegalArgumentException("число без JSON-представлення: " + d);
            if (d == Math.rint(d) && Math.abs(d) < 1e15) b.append((long) d); else b.append(d);
        }
        else if (o instanceof String) quote(b, (String) o);
        else if (o instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) o; if (m.isEmpty()) { b.append("{}"); return; }
            b.append("{\n"); int n = 0;
            for (Map.Entry<String, Object> e : m.entrySet()) { indent(b, lvl + 1); quote(b, e.getKey()); b.append(": "); write(b, e.getValue(), lvl + 1); b.append(++n < m.size() ? ",\n" : "\n"); }
            indent(b, lvl); b.append('}');
        }
        else if (o instanceof List) {
            List<Object> a = (List<Object>) o; if (a.isEmpty()) { b.append("[]"); return; }
            boolean flat = true; for (Object x : a) if (x instanceof Map || x instanceof List) flat = false;
            if (flat) { b.append('['); for (int k = 0; k < a.size(); k++) { if (k > 0) b.append(", "); write(b, a.get(k), lvl); } b.append(']'); return; }
            b.append("[\n"); for (int k = 0; k < a.size(); k++) { indent(b, lvl + 1); write(b, a.get(k), lvl + 1); b.append(k + 1 < a.size() ? ",\n" : "\n"); }
            indent(b, lvl); b.append(']');
        }
        else throw new IllegalArgumentException("непідтримуваний тип: " + o.getClass().getSimpleName());
    }
    private static void quote(StringBuilder b, String s) {
        b.append('"');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"': b.append("\\\""); break; case '\\': b.append("\\\\"); break; case '\n': b.append("\\n"); break; case '\r': b.append("\\r"); break; case '\t': b.append("\\t"); break;
                default: if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c);
            }
        }
        b.append('"');
    }

    static Map<String, Object> obj(Object o) { @SuppressWarnings("unchecked") Map<String, Object> m = o instanceof Map ? (Map<String, Object>) o : null; return m; }
    static List<Object> arr(Object o) { @SuppressWarnings("unchecked") List<Object> a = o instanceof List ? (List<Object>) o : null; return a; }
    private Json() { this(""); }
}
