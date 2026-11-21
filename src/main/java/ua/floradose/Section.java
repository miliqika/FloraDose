package ua.floradose;

enum Section {
    MODEL("model", "3D-модель", "МОДЕЛЬ", Scene3D.Mode.MODEL), INNER("inner", "Внутрішня конструкція", null, Scene3D.Mode.INNER), EL("el", "Електроніка", null, Scene3D.Mode.EL),
    SCHEMES("scheme", "Схеми", "ДОКУМЕНТАЦІЯ", Scene3D.Mode.INNER), COMP("comp", "Компоненти", null, Scene3D.Mode.INNER), CONFIG("config", "Конфігурації", null, Scene3D.Mode.MODEL),
    WATER("water", "Система поливу", "РОБОТА СИСТЕМИ", Scene3D.Mode.WATER), SIM("sim", "Симуляція", null, Scene3D.Mode.INNER), MONITOR("monitor", "Моніторинг", null, Scene3D.Mode.WATER),
    INFO("info", "Інформація", "ДОВІДКА", Scene3D.Mode.MODEL);
    final String icon, title, group; final Scene3D.Mode mode;
    Section(String i, String t, String g, Scene3D.Mode m) { icon = i; title = t; group = g; mode = m; }
}
