package com.electrician.tracker.report;

import com.electrician.tracker.config.AppInfo;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/**
 * "Spark ERP 2.8.3" centred at the bottom of every page of every PDF the
 * program prints. The text comes from {@link AppInfo}, so it follows pom.xml.
 */
@Component
public class PdfFooter {

    private static final float FONT_SIZE = 7;

    private final String text;

    public PdfFooter(AppInfo appInfo) {
        this.text = appInfo.nameAndVersion();
    }

    /** Registers the footer on a writer before the document is opened. */
    void attachTo(PdfWriter writer) {
        writer.setPageEvent(new Stamp(text));
    }

    String text() {
        return text;
    }

    private static final class Stamp extends PdfPageEventHelper {

        private static final Font FONT = PdfStyle.muted(FONT_SIZE);
        private static final float HALF = 2;

        private final String text;

        Stamp(String text) {
            this.text = text;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float x = (document.left() + document.right()) / HALF;
            float y = document.bottomMargin() / HALF;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(text, FONT), x, y, 0);
        }
    }
}
