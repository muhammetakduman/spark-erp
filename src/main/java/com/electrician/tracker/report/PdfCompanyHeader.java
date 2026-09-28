package com.electrician.tracker.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.electrician.tracker.domain.Company;
import com.electrician.tracker.ui.util.DialogUtil;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

/**
 * The letterhead of customer PDFs, from Ayarlar → Firma bilgileri: logo on
 * the left; name (in the brand colour), slogan, address and contact line on
 * the right. Without a logo the text takes the full width, leaving no gap.
 * Nothing is printed while no company name has been entered.
 */
final class PdfCompanyHeader {

    private static final float NAME_SIZE = 13;
    private static final float TEXT_SIZE = 8;
    private static final float LOGO_MAX_WIDTH = 100;
    private static final float LOGO_MAX_HEIGHT = 50;
    private static final float[] WITH_LOGO_WIDTHS = { 1, 4 };
    private static final float BOTTOM_GAP = 6;
    private static final String CONTACT_SEPARATOR = "  ·  ";

    private PdfCompanyHeader() {
    }

    static void add(Document document, Company company, PdfStyle style) throws DocumentException {
        if (company == null || company.getName() == null) {
            return;
        }
        PdfPTable table = company.hasLogo() ? new PdfPTable(WITH_LOGO_WIDTHS) : new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(BOTTOM_GAP);
        if (company.hasLogo()) {
            table.addCell(logoCell(company.getLogo(), style));
        }
        table.addCell(textCell(company, style));
        document.add(table);
    }

    private static PdfPCell logoCell(byte[] logo, PdfStyle style) throws DocumentException {
        try {
            Image image = Image.getInstance(logo);
            image.scaleToFit(LOGO_MAX_WIDTH, LOGO_MAX_HEIGHT);
            PdfPCell cell = new PdfPCell(image, false);
            underline(cell, style);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            return cell;
        } catch (IOException e) {
            // An unreadable logo must not stop the document: leave the cell empty.
            PdfPCell empty = new PdfPCell(new Phrase(""));
            underline(empty, style);
            return empty;
        }
    }

    private static PdfPCell textCell(Company company, PdfStyle style) {
        PdfPCell cell = new PdfPCell();
        underline(cell, style);
        cell.addElement(new Paragraph(company.getName(), style.heading(NAME_SIZE)));
        Font text = PdfStyle.regular(TEXT_SIZE);
        for (String line : textLines(company)) {
            cell.addElement(new Paragraph(line, text));
        }
        return cell;
    }

    private static void underline(PdfPCell cell, PdfStyle style) {
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColorBottom(style.brand());
    }

    private static List<String> textLines(Company company) {
        List<String> lines = new ArrayList<>();
        addIfPresent(lines, company.getSlogan());
        addIfPresent(lines, company.getAddress());
        List<String> contacts = new ArrayList<>();
        addIfPresent(contacts, labeled("pdf.company.phone", company.getPhone()));
        addIfPresent(contacts, labeled("pdf.company.email", company.getEmail()));
        addIfPresent(contacts, company.getWeb());
        if (!contacts.isEmpty()) {
            lines.add(String.join(CONTACT_SEPARATOR, contacts));
        }
        addIfPresent(lines, taxLine(company));
        return lines;
    }

    private static String taxLine(Company company) {
        if (company.getTaxOffice() == null && company.getTaxNo() == null) {
            return null;
        }
        return DialogUtil.message("pdf.company.tax", Objects.requireNonNullElse(company.getTaxOffice(), ""),
                Objects.requireNonNullElse(company.getTaxNo(), ""));
    }

    private static String labeled(String key, String value) {
        return value == null ? null : DialogUtil.message(key, value);
    }

    private static void addIfPresent(List<String> lines, String value) {
        if (value != null && !value.isBlank()) {
            lines.add(value);
        }
    }
}
