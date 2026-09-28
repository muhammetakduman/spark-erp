package com.electrician.tracker.report;

import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

/**
 * Writes "1/1" in the top right corner of every page. The total is only
 * known when the document closes, so it goes into a shared template that is
 * filled in at the end.
 */
final class PageNumberStamp extends PdfPageEventHelper {

    private static final Font FONT = PdfFonts.regular(8);
    private static final float TEMPLATE_WIDTH = 30;
    private static final float TEMPLATE_HEIGHT = 12;
    private static final float TOP_OFFSET = 18;
    private static final String SEPARATOR = "/";

    private PdfTemplate total;

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        total = writer.getDirectContent().createTemplate(TEMPLATE_WIDTH, TEMPLATE_HEIGHT);
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();
        float x = document.right() - TEMPLATE_WIDTH;
        float y = document.getPageSize().getHeight() - TOP_OFFSET;
        ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                new Phrase(writer.getPageNumber() + SEPARATOR, FONT), x, y, 0);
        canvas.addTemplate(total, x, y);
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                new Phrase(String.valueOf(writer.getPageNumber() - 1), FONT), 0, 0, 0);
    }
}
