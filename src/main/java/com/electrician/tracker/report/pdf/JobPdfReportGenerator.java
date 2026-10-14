package com.electrician.tracker.report.pdf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import com.electrician.tracker.domain.settings.Company;
import com.electrician.tracker.domain.customer.Customer;
import com.electrician.tracker.domain.job.Job;
import com.electrician.tracker.domain.job.JobType;
import com.electrician.tracker.domain.material.MaterialItem;
import com.electrician.tracker.domain.job.Payment;
import com.electrician.tracker.dto.job.JobSummary;
import com.electrician.tracker.dto.material.MaterialGroup;
import com.electrician.tracker.dto.common.VatBreakdown;
import com.electrician.tracker.service.material.MaterialPriceCalculator;
import com.electrician.tracker.ui.util.format.Bicimlendirici;
import com.electrician.tracker.ui.util.dialog.DialogUtil;
import com.electrician.tracker.ui.util.format.EnumLabels;
import com.electrician.tracker.ui.util.format.VatLabels;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/**
 * Builds the customer-facing PDF handout for a site or service job in the
 * same print style as the quote ({@link PdfStyle}): customer details,
 * material list, labor/service fees, VAT-excl./incl. totals and payments (a
 * site's payment list and remaining balance, a service's paid flag), under
 * the company letterhead. A site's materials are printed by group (group
 * name in the brand colour, a sale subtotal under each group). Purchase prices, suppliers and profit are internal
 * figures and are deliberately left out of this document; the payment
 * section is left out too when {@code includePayments} is false (a MANAGER
 * does not see receivables).
 */
@Component
public class JobPdfReportGenerator {

    private static final float TITLE_SIZE = 14;
    private static final float SECTION_SIZE = 10;
    private static final float TEXT_SIZE = 9;
    private static final float GRAND_TOTAL_SIZE = 11;
    private static final float MARGIN = 32;
    private static final float GAP = 6;
    private static final float PADDING = 3;
    private static final float[] INFO_WIDTHS = { 1, 3 };
    private static final float[] MATERIAL_WIDTHS = { 40, 14, 15, 13, 18 };
    private static final float[] PAYMENT_WIDTHS = { 1, 1, 1 };
    private static final float[] TOTALS_WIDTHS = { 3, 2 };
    private static final float TOTALS_WIDTH_PERCENT = 50;

    private final PdfFooter pdfFooter;

    public JobPdfReportGenerator(PdfFooter pdfFooter) {
        this.pdfFooter = pdfFooter;
    }

    /** {@code groups} empty prints {@code materials} as one flat list. */
    public void generate(Job job, JobSummary summary, List<MaterialItem> materials, List<MaterialGroup> groups,
            List<Payment> payments, Company company, boolean includePayments, Path outputFile) {
        PdfStyle style = new PdfStyle(company);
        Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        try (FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumberStamp());
            pdfFooter.attachTo(writer);
            document.open();
            PdfCompanyHeader.add(document, company, style);
            addHeader(document, job, style);
            addMaterialsTable(document, materials, groups, style);
            addFees(document, job, summary);
            addTotals(document, summary);
            if (includePayments && (job.getType() == JobType.SITE || !payments.isEmpty())) {
                addPaymentsTable(document, payments, summary, style);
            } else if (job.getType() == JobType.SERVICE) {
                addServicePaymentStatus(document, job, style);
            }
            document.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate PDF report", e);
        }
    }

    private void addHeader(Document document, Job job, PdfStyle style) throws DocumentException {
        boolean site = job.getType() == JobType.SITE;
        Paragraph title = style.headingParagraph(DialogUtil.message(site ? "jobPdf.title.site" : "jobPdf.title.service"),
                TITLE_SIZE);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(GAP);
        document.add(title);
        Customer customer = job.getCustomer();
        PdfPTable info = new PdfPTable(INFO_WIDTHS);
        info.setWidthPercentage(100);
        info.setSpacingAfter(GAP);
        PdfPCell blockTitle = PdfStyle.plainCell(DialogUtil.message("jobPdf.customerBlock"),
                style.heading(SECTION_SIZE), Element.ALIGN_LEFT, PADDING);
        blockTitle.setColspan(INFO_WIDTHS.length);
        info.addCell(blockTitle);
        addInfoRow(info, "jobPdf.customer", customer.getName());
        addInfoRow(info, "jobPdf.customerAddress", customer.getAddress());
        addInfoRow(info, "jobPdf.taxNo", customer.getTaxNo());
        addInfoRow(info, site ? "jobPdf.site" : "jobPdf.job", job.getName());
        addInfoRow(info, "jobPdf.siteAddress", job.getAddress());
        if (job.getStartDate() != null) {
            addInfoRow(info, site ? "jobPdf.startDate" : "jobPdf.date", Bicimlendirici.date(job.getStartDate()));
        }
        PdfPTable frame = new PdfPTable(1);
        frame.setWidthPercentage(100);
        frame.setSpacingAfter(GAP);
        frame.addCell(style.frame(new PdfPCell(info)));
        document.add(frame);
    }

    private static void addInfoRow(PdfPTable table, String labelKey, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        table.addCell(PdfStyle.plainCell(DialogUtil.message(labelKey), PdfStyle.bold(TEXT_SIZE), Element.ALIGN_LEFT,
                PADDING));
        table.addCell(PdfStyle.plainCell(value, PdfStyle.regular(TEXT_SIZE), Element.ALIGN_LEFT, PADDING));
    }

    /** Product bold; quantity and prices right-aligned; line totals bold; zebra rows; groups when given. */
    private void addMaterialsTable(Document document, List<MaterialItem> materials, List<MaterialGroup> groups,
            PdfStyle style) throws DocumentException {
        document.add(style.headingParagraph(DialogUtil.message("jobPdf.materials"), SECTION_SIZE));
        PdfPTable table = new PdfPTable(MATERIAL_WIDTHS);
        table.setWidthPercentage(100);
        table.setSpacingBefore(GAP / 2);
        table.setSpacingAfter(GAP);
        table.setHeaderRows(1);
        table.addCell(style.headerCell(DialogUtil.message("jobPdf.column.product"), TEXT_SIZE, Element.ALIGN_LEFT,
                PADDING));
        table.addCell(style.headerCell(DialogUtil.message("jobPdf.column.quantity"), TEXT_SIZE, Element.ALIGN_RIGHT,
                PADDING));
        table.addCell(style.headerCell(DialogUtil.message("jobPdf.column.unitPrice"), TEXT_SIZE, Element.ALIGN_RIGHT,
                PADDING));
        table.addCell(style.headerCell(DialogUtil.message("jobPdf.column.vat"), TEXT_SIZE, Element.ALIGN_LEFT,
                PADDING));
        table.addCell(style.headerCell(DialogUtil.message("jobPdf.column.total"), TEXT_SIZE, Element.ALIGN_RIGHT,
                PADDING));
        if (groups.isEmpty()) {
            addMaterialRows(table, materials);
        } else {
            for (MaterialGroup group : groups) {
                addGroupRows(table, group, style);
            }
        }
        document.add(table);
    }

    /** Group name across the table, its lines, then "Ara toplam – Diafon (KDV'siz)". */
    private static void addGroupRows(PdfPTable table, MaterialGroup group, PdfStyle style) {
        String name = group.total().isUncategorized() ? DialogUtil.message("material.group.uncategorized")
                : group.groupName();
        PdfPCell title = PdfStyle.plainCell(name, style.heading(TEXT_SIZE), Element.ALIGN_LEFT, PADDING);
        title.setColspan(MATERIAL_WIDTHS.length);
        title.setPaddingTop(PADDING * 2);
        table.addCell(title);
        addMaterialRows(table, group.lines());
        PdfPCell caption = PdfStyle.plainCell(DialogUtil.message("jobPdf.groupSubtotal", name),
                PdfStyle.bold(TEXT_SIZE), Element.ALIGN_RIGHT, PADDING);
        caption.setColspan(MATERIAL_WIDTHS.length - 1);
        table.addCell(caption);
        table.addCell(PdfStyle.plainCell(Bicimlendirici.money(group.total().saleTotal()), PdfStyle.bold(TEXT_SIZE),
                Element.ALIGN_RIGHT, PADDING));
    }

    private static void addMaterialRows(PdfPTable table, List<MaterialItem> materials) {
        Font bold = PdfStyle.bold(TEXT_SIZE);
        Font regular = PdfStyle.regular(TEXT_SIZE);
        int rowIndex = 0;
        for (MaterialItem item : materials) {
            table.addCell(PdfStyle.bodyCell(item.getProduct().getDisplayName(), bold, Element.ALIGN_LEFT, rowIndex,
                    PADDING));
            table.addCell(PdfStyle.bodyCell(Bicimlendirici.quantity(item.getQuantity()) + " "
                    + EnumLabels.label(item.getProduct().getUnit()), regular, Element.ALIGN_RIGHT, rowIndex, PADDING));
            table.addCell(PdfStyle.bodyCell(Bicimlendirici.money(item.getSaleUnitPrice()), regular,
                    Element.ALIGN_RIGHT, rowIndex, PADDING));
            table.addCell(PdfStyle.bodyCell(VatLabels.describe(item.getVatRate(), item.getVatIncluded()), regular,
                    Element.ALIGN_LEFT, rowIndex, PADDING));
            table.addCell(PdfStyle.bodyCell(Bicimlendirici.money(MaterialPriceCalculator.saleTotal(item)), bold,
                    Element.ALIGN_RIGHT, rowIndex, PADDING));
            rowIndex++;
        }
    }

    private void addFees(Document document, Job job, JobSummary summary) throws DocumentException {
        Font text = PdfStyle.regular(TEXT_SIZE);
        if (summary.attendanceDayCount().signum() > 0) {
            // Day count is the sum of day factors; wages are internal cost and stay out of the customer's copy.
            document.add(new Paragraph(DialogUtil.message("jobPdf.laborDays",
                    Bicimlendirici.days(summary.attendanceDayCount())), text));
        }
        addFeeLine(document, "jobPdf.serviceFee", job.getServiceFee(), job.getServiceFeeVatRate(),
                job.getServiceFeeVatIncluded());
        addFeeLine(document, "jobPdf.laborFee", job.getLaborFee(), job.getLaborFeeVatRate(),
                job.getLaborFeeVatIncluded());
    }

    private void addFeeLine(Document document, String captionKey, BigDecimal fee, Integer vatRate,
            Boolean vatIncluded) throws DocumentException {
        if (fee == null || fee.signum() == 0) {
            return;
        }
        String vatText = vatRate == null ? "" : DialogUtil.message("jobPdf.feeVat", VatLabels.describe(vatRate,
                vatIncluded));
        document.add(new Paragraph(DialogUtil.message(captionKey, Bicimlendirici.money(fee)) + vatText,
                PdfStyle.regular(TEXT_SIZE)));
    }

    /** Subtotal and VAT lines in regular black; the grand total largest and bold on a grey band. */
    private void addTotals(Document document, JobSummary summary) throws DocumentException {
        VatBreakdown vat = summary.saleVat();
        PdfPTable table = new PdfPTable(TOTALS_WIDTHS);
        table.setWidthPercentage(TOTALS_WIDTH_PERCENT);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(GAP);
        table.setSpacingAfter(GAP);
        if (vat.hasVat()) {
            addTotalRow(table, DialogUtil.message("jobPdf.subtotal"), vat.excludingVat());
            for (VatBreakdown.VatRateAmount rate : vat.rates()) {
                addTotalRow(table, DialogUtil.message("jobPdf.vatLine", rate.rate(),
                        Bicimlendirici.money(rate.excludingVat())), rate.vatAmount());
            }
        }
        Font grand = PdfStyle.bold(GRAND_TOTAL_SIZE);
        table.addCell(PdfStyle.grandTotalCell(DialogUtil.message("jobPdf.grandTotal"), grand, Element.ALIGN_LEFT,
                PADDING * 2));
        table.addCell(PdfStyle.grandTotalCell(Bicimlendirici.money(vat.includingVat()), grand, Element.ALIGN_RIGHT,
                PADDING * 2));
        document.add(table);
    }

    private static void addTotalRow(PdfPTable table, String caption, BigDecimal amount) {
        table.addCell(PdfStyle.plainCell(caption, PdfStyle.regular(TEXT_SIZE), Element.ALIGN_LEFT, PADDING));
        table.addCell(PdfStyle.plainCell(Bicimlendirici.money(amount), PdfStyle.regular(TEXT_SIZE),
                Element.ALIGN_RIGHT, PADDING));
    }

    private void addPaymentsTable(Document document, List<Payment> payments, JobSummary summary, PdfStyle style)
            throws DocumentException {
        document.add(style.headingParagraph(DialogUtil.message("jobPdf.payments"), SECTION_SIZE));
        PdfPTable table = new PdfPTable(PAYMENT_WIDTHS);
        table.setWidthPercentage(100);
        table.setSpacingBefore(GAP / 2);
        table.setHeaderRows(1);
        table.addCell(style.headerCell(DialogUtil.message("jobPdf.column.date"), TEXT_SIZE, Element.ALIGN_LEFT,
                PADDING));
        table.addCell(style.headerCell(DialogUtil.message("jobPdf.column.amount"), TEXT_SIZE, Element.ALIGN_RIGHT,
                PADDING));
        table.addCell(style.headerCell(DialogUtil.message("jobPdf.column.method"), TEXT_SIZE, Element.ALIGN_LEFT,
                PADDING));
        Font regular = PdfStyle.regular(TEXT_SIZE);
        int rowIndex = 0;
        for (Payment payment : payments) {
            table.addCell(PdfStyle.bodyCell(Bicimlendirici.date(payment.getPaymentDate()), regular,
                    Element.ALIGN_LEFT, rowIndex, PADDING));
            table.addCell(PdfStyle.bodyCell(Bicimlendirici.money(payment.getAmount()), PdfStyle.bold(TEXT_SIZE),
                    Element.ALIGN_RIGHT, rowIndex, PADDING));
            table.addCell(PdfStyle.bodyCell(EnumLabels.label(payment.getMethod()), regular, Element.ALIGN_LEFT,
                    rowIndex, PADDING));
            rowIndex++;
        }
        document.add(table);
        PdfPTable totals = new PdfPTable(TOTALS_WIDTHS);
        totals.setWidthPercentage(TOTALS_WIDTH_PERCENT);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setSpacingBefore(GAP);
        addTotalRow(totals, DialogUtil.message("jobPdf.collected"), summary.collectedTotal());
        Font grand = PdfStyle.bold(GRAND_TOTAL_SIZE);
        totals.addCell(PdfStyle.grandTotalCell(DialogUtil.message("jobPdf.remaining"), grand, Element.ALIGN_LEFT,
                PADDING * 2));
        totals.addCell(PdfStyle.grandTotalCell(Bicimlendirici.money(summary.remaining()), grand, Element.ALIGN_RIGHT,
                PADDING * 2));
        document.add(totals);
    }

    private void addServicePaymentStatus(Document document, Job job, PdfStyle style) throws DocumentException {
        document.add(style.headingParagraph(DialogUtil.message(job.isPaymentReceived() ? "jobPdf.payment.received"
                : "jobPdf.payment.pending"), SECTION_SIZE));
    }
}
