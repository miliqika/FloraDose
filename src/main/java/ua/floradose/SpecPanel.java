package ua.floradose;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

final class SpecPanel {
    private final Ctx c; final TableView<Data.C> table = new TableView<>(); private boolean selecting;
    SpecPanel(Ctx c) { this.c = c; }
    void refresh() { table.refresh(); }
    void selectRow(String id) { Data.C d = Data.COMPS.get(id); selecting = true; table.getSelectionModel().select(d); table.scrollTo(d); selecting = false; }
    Node build() {
        TableColumn<Data.C, Boolean> vis = new TableColumn<>("Показ"); vis.setPrefWidth(52); vis.setCellValueFactory(d -> new SimpleBooleanProperty(true));
        vis.setCellFactory(cc -> new TableCell<Data.C, Boolean>() {
            final CheckBox cb = new CheckBox();
            { cb.setOnAction(e -> { Data.C it = (Data.C) getTableRow().getItem(); if (it == null) return; if (cb.isSelected()) c.hidden().remove(it.id); else c.hidden().add(it.id); c.repaint(); }); }
            @Override protected void updateItem(Boolean v, boolean empty) { super.updateItem(v, empty); if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; } cb.setSelected(!c.hidden().contains(((Data.C) getTableRow().getItem()).id)); setGraphic(cb); }
        });
        table.getColumns().add(vis);
        table.getColumns().add(Ui.<Data.C>col("Назва", 190, d -> d.name + (d.assumed ? " ⚠" : ""))); table.getColumns().add(Ui.<Data.C>col("К-сть", 48, d -> String.valueOf(Data.qty(d, c.cfg()))));
        table.getColumns().add(Ui.<Data.C>col("Модель", 180, d -> d.model)); table.getColumns().add(Ui.<Data.C>col("Матеріал", 180, d -> Data.mat(d, c.cfg())));
        table.getColumns().add(Ui.<Data.C>col("Місце", 190, d -> d.place)); table.getColumns().add(Ui.<Data.C>col("Розміри", 210, d -> d.dim));
        table.getColumns().add(Ui.<Data.C>col("Призначення", 400, d -> d.purpose)); table.getColumns().add(Ui.<Data.C>col("Тип", 90, Data.C::catName));
        FilteredList<Data.C> fl = new FilteredList<>(FXCollections.observableArrayList(Data.COMPS.values()), d -> true); table.setItems(fl);
        TextField q = new TextField(); q.setPromptText("Пошук: назва, матеріал, місце, модель…"); HBox.setHgrow(q, Priority.ALWAYS);
        ChoiceBox<String> cat = new ChoiceBox<>(); cat.getItems().addAll("Усі типи", "Конструкція", "Вода", "Електроніка"); cat.setValue("Усі типи");
        Runnable flt = () -> { String t = q.getText() == null ? "" : q.getText().toLowerCase(); String k = cat.getValue();
            fl.setPredicate(d -> (k.equals("Усі типи") || d.catName().equals(k)) && (t.isEmpty() || (d.name + " " + d.place + " " + d.model + " " + Data.mat(d, c.cfg()) + " " + d.purpose).toLowerCase().contains(t))); };
        q.textProperty().addListener((o, a, b) -> flt.run()); cat.valueProperty().addListener((o, a, b) -> flt.run());
        Button exp = Ui.button("CSV", "csv", () -> {
            List<String[]> rows = new ArrayList<>(); rows.add(new String[]{"Назва", "К-сть", "Модель", "Матеріал", "Місце", "Розміри", "Призначення", "Тип", "Припущення"});
            for (Data.C d : table.getItems()) rows.add(new String[]{d.name, String.valueOf(Data.qty(d, c.cfg())), d.model, Data.mat(d, c.cfg()), d.place, d.dim, d.purpose, d.catName(), d.assumed ? "так" : ""});
            c.export().csv("FloraDose_BOM_" + Data.CFG[c.cfg()] + ".csv", rows);
        });
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> { if (b != null && !selecting) c.select(b.id, -1); });
        VBox v = new VBox(8, Ui.hint("Специфікація для обраної конфігурації; кількості, матеріали й датчики збігаються з 3D-моделлю та схемою. ⚠ — проєктне припущення."), new HBox(6, q, cat, exp), table);
        v.setPadding(new Insets(6)); VBox.setVgrow(table, Priority.ALWAYS); return v;
    }
}
