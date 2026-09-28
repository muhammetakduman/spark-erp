package com.electrician.tracker.report;

import java.awt.Color;

import com.electrician.tracker.domain.Company;
import com.electrician.tracker.service.CompanyService;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;

/**
 * The one print style of every customer PDF (quote, site/service handout,
 * daily program): headings and table headers in the company's brand colour
 * (white bold text on it), black body text, very light zebra rows that do not
 * look dirty when printed, numbers right-aligned and a grand total set apart
 * with a thin line and a light grey fill. Print colours live here, not in
 * the screens' theme.
 */
final class PdfStyle {

    static final Color BLACK = Color.BLACK;
    static final Color WHITE = Color.WHITE;
    static final Color ZEBRA = new Color(0xF7, 0xF7, 0xF7);
    static final Color TOTAL_FILL = new Color(0xEC, 0xEC, 0xEC);
    static final Color RULE = new Color(0xBF, 0xBF, 0xBF);
    static final Color MUTED = new Color(0x59, 0x59, 0x59);
    private static final float HAIRLINE = 0.5f;
    private static final float TOTAL_RULE = 0.8f;

    private final Color brand;

    PdfStyle(Company company) {
        this.brand = Color.decode(CompanyService.brandColorOf(company));
    }

    Color brand() {
        return brand;
    }

    /** Section headings ("TEKLİF FORMU", "Müşteri Bilgileri", "Notlar"): brand colour, bold. */
    Font heading(float size) {
        return colored(PdfFonts.bold(size), brand);
    }

    static Font bold(float size) {
        return colored(PdfFonts.bold(size), BLACK);
    }

    static Font regular(float size) {
        return colored(PdfFonts.regular(size), BLACK);
    }

    static Font muted(float size) {
        return colored(PdfFonts.regular(size), MUTED);
    }

    Paragraph headingParagraph(String text, float size) {
        return new Paragraph(text, heading(size));
    }

    /** A table header cell: brand fill, white bold text. */
    PdfPCell headerCell(String text, float size, int alignment, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text, colored(PdfFonts.bold(size), WHITE)));
        cell.setBackgroundColor(brand);
        cell.setBorderColor(brand);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(padding);
        return cell;
    }

    /** A body cell of row {@code rowIndex} (0-based): every second row gets the zebra fill; text wraps. */
    static PdfPCell bodyCell(String text, Font font, int alignment, int rowIndex, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setNoWrap(false);
        cell.setPadding(padding);
        cell.setBorderColor(RULE);
        cell.setBorderWidth(HAIRLINE);
        if (rowIndex % 2 == 1) {
            cell.setBackgroundColor(ZEBRA);
        }
        return cell;
    }

    /** A plain, borderless cell (labels and values of info blocks, totals). */
    static PdfPCell plainCell(String text, Font font, int alignment, float padding) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(padding);
        return cell;
    }

    /** The grand total cell: thin line above, light grey fill. */
    static PdfPCell grandTotalCell(String text, Font font, int alignment, float padding) {
        PdfPCell cell = plainCell(text, font, alignment, padding);
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColorTop(BLACK);
        cell.setBorderWidthTop(TOTAL_RULE);
        cell.setBackgroundColor(TOTAL_FILL);
        return cell;
    }

    /** A box with a brand-coloured frame, for info blocks. */
    PdfPCell frame(PdfPCell cell) {
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(brand);
        cell.setBorderWidth(HAIRLINE);
        return cell;
    }

    private static Font colored(Font font, Color color) {
        font.setColor(color);
        return font;
    }
}
