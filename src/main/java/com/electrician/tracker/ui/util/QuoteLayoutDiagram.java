package com.electrician.tracker.ui.util;

import java.util.EnumMap;
import java.util.Map;

import com.electrician.tracker.domain.TemplateType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A small box drawing of the quote PDF (letterhead, customer, item table,
 * totals, terms, signatures) with where each part comes from. The part a
 * template fills is highlighted in the corporate colour; for a template that
 * is not used in quotes the drawing fades and a note says so.
 */
public final class QuoteLayoutDiagram {

    private static final String ACTIVE = "diagram-part-active";
    private static final String FADED = "quote-diagram-faded";
    private static final double SPACING = 2;

    private enum Part {
        HEADER("template.diagram.header", "template.diagram.header.source", null),
        CUSTOMER("template.diagram.customer", "template.diagram.customer.source", null),
        ITEMS("template.diagram.items", "template.diagram.items.source", TemplateType.QUOTE_ITEM_SET),
        TOTALS("template.diagram.totals", "template.diagram.totals.source", null),
        TERMS("template.diagram.terms", "template.diagram.terms.source", TemplateType.QUOTE_NOTE),
        SIGNATURE("template.diagram.signature", "template.diagram.signature.source", null);

        private final String nameKey;
        private final String sourceKey;
        private final TemplateType filledBy;

        Part(String nameKey, String sourceKey, TemplateType filledBy) {
            this.nameKey = nameKey;
            this.sourceKey = sourceKey;
            this.filledBy = filledBy;
        }
    }

    private final VBox root = new VBox();
    private final Map<Part, VBox> boxes = new EnumMap<>(Part.class);
    private final Label note = new Label(DialogUtil.message("template.diagram.notInQuote"));

    public QuoteLayoutDiagram() {
        root.getStyleClass().add("quote-diagram");
        for (Part part : Part.values()) {
            Label name = new Label(DialogUtil.message(part.nameKey));
            name.getStyleClass().add("diagram-part-name");
            Label source = new Label(DialogUtil.message(part.sourceKey));
            source.getStyleClass().add("diagram-part-source");
            source.setWrapText(true);
            VBox box = new VBox(SPACING, name, source);
            box.getStyleClass().add("diagram-part");
            if (part == Part.ITEMS) {
                box.getStyleClass().add("diagram-part-table");
            }
            boxes.put(part, box);
            root.getChildren().add(box);
        }
        note.getStyleClass().add("diagram-note");
        note.setWrapText(true);
        note.setVisible(false);
        note.setManaged(false);
        root.getChildren().add(note);
    }

    public VBox node() {
        return root;
    }

    /** Highlights the part {@code type} fills; {@code null} clears the highlight. */
    public void highlight(TemplateType type) {
        boxes.forEach((part, box) -> {
            box.getStyleClass().remove(ACTIVE);
            if (type != null && part.filledBy == type) {
                box.getStyleClass().add(ACTIVE);
            }
        });
        boolean notInQuote = type == TemplateType.JOB_DESCRIPTION;
        root.getStyleClass().remove(FADED);
        if (notInQuote) {
            root.getStyleClass().add(FADED);
        }
        note.setVisible(notInQuote);
        note.setManaged(notInQuote);
    }
}
