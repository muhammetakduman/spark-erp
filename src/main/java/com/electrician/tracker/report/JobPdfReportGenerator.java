package com.electrician.tracker.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.VatBreakdown;
import com.electrician.tracker.service.MaterialPriceCalculator;
import com.electrician.tracker.ui.util.Bicimlendirici;
import com.electrician.tracker.ui.util.EnumLabels;
import com.electrician.tracker.ui.util.VatLabels;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/**
 * Builds the customer-facing PDF handout for a site or service job: customer
 * details, material list, labor/service fees, VAT-excl./incl. totals and
 * payments (a site's payment list and remaining balance, a service's paid
 * flag). Purchase prices, suppliers and profit are internal figures and
 * are deliberately left out of this document.
 */
@Component
public class JobPdfReportGenerator {

    private static final Font TITLE_FONT = PdfFonts.bold(16);
    private static final Font SECTION_FONT = PdfFonts.bold(12);
    private static final Font NORMAL_FONT = PdfFonts.regular(10);
    private static final Font HEADER_CELL_FONT = PdfFonts.bold(10);

    public void generate(Job job, JobSummary summary, List<MaterialItem> materials, List<Payment> payments,
            Path outputFile) {
        Document document = new Document();
        try (FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, job);
            addMaterialsTable(document, materials);
            addTotalsSection(document, job, summary);
            if (job.getType() == JobType.SITE || !payments.isEmpty()) {
                addPaymentsTable(document, payments, summary);
            } else {
                addServicePaymentStatus(document, job);
            }

            document.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not generate PDF report", e);
        }
    }

    private void addHeader(Document document, Job job) throws DocumentException {
        boolean site = job.getType() == JobType.SITE;
        Customer customer = job.getCustomer();
        document.add(new Paragraph(site ? "Şantiye Dökümü" : "Servis Dökümü", TITLE_FONT));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Müşteri: " + customer.getName(), NORMAL_FONT));
        addOptionalLine(document, "Müşteri Adresi", customer.getAddress());
        addOptionalLine(document, "Vergi No", customer.getTaxNo());
        addOptionalLine(document, site ? "Şantiye" : "İş", job.getName());
        addOptionalLine(document, "Şantiye Adresi", job.getAddress());
        if (job.getStartDate() != null) {
            document.add(new Paragraph((site ? "Başlangıç: " : "Tarih: ") + Bicimlendirici.date(job.getStartDate()),
                    NORMAL_FONT));
        }
        document.add(new Paragraph(" "));
    }

    private void addMaterialsTable(Document document, List<MaterialItem> materials) throws DocumentException {
        document.add(new Paragraph("Malzeme Listesi", SECTION_FONT));
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addHeaderCell(table, "Ürün");
        addHeaderCell(table, "Miktar");
        addHeaderCell(table, "Birim Fiyat");
        addHeaderCell(table, "KDV");
        addHeaderCell(table, "Toplam");

        for (MaterialItem item : materials) {
            table.addCell(new Phrase(item.getProduct().getName(), NORMAL_FONT));
            table.addCell(new Phrase(Bicimlendirici.quantity(item.getQuantity()) + " " + EnumLabels.label(item.getProduct().getUnit()), NORMAL_FONT));
            table.addCell(new Phrase(Bicimlendirici.money(item.getSaleUnitPrice()), NORMAL_FONT));
            table.addCell(new Phrase(VatLabels.describe(item.getVatRate(), item.getVatIncluded()), NORMAL_FONT));
            table.addCell(new Phrase(Bicimlendirici.money(MaterialPriceCalculator.saleTotal(item)), NORMAL_FONT));
        }
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addTotalsSection(Document document, Job job, JobSummary summary) throws DocumentException {
        if (summary.attendanceDayCount().signum() > 0) {
            // Day count is the sum of day factors; wages are internal cost and stay out of the customer's copy.
            document.add(new Paragraph("İşçilik: " + Bicimlendirici.days(summary.attendanceDayCount()) + " gün",
                    NORMAL_FONT));
        }
        addFeeLine(document, "Servis Ücreti", job.getServiceFee(), job.getServiceFeeVatRate(), job.getServiceFeeVatIncluded());
        addFeeLine(document, "İşçilik Ücreti", job.getLaborFee(), job.getLaborFeeVatRate(), job.getLaborFeeVatIncluded());
        VatBreakdown vat = summary.saleVat();
        if (vat.hasVat()) {
            document.add(new Paragraph("Ara Toplam (KDV'siz): " + Bicimlendirici.money(vat.excludingVat()), NORMAL_FONT));
            for (VatBreakdown.VatRateAmount rate : vat.rates()) {
                document.add(new Paragraph("KDV (%" + rate.rate() + ") — matrah "
                        + Bicimlendirici.money(rate.excludingVat()) + ": " + Bicimlendirici.money(rate.vatAmount()),
                        NORMAL_FONT));
            }
        }
        document.add(new Paragraph("Genel Toplam: " + Bicimlendirici.money(vat.includingVat()), SECTION_FONT));
        document.add(new Paragraph(" "));
    }

    private void addFeeLine(Document document, String caption, BigDecimal fee, Integer vatRate, Boolean vatIncluded)
            throws DocumentException {
        if (fee == null || fee.signum() == 0) {
            return;
        }
        String vatText = vatRate == null ? "" : " (KDV " + VatLabels.describe(vatRate, vatIncluded) + ")";
        document.add(new Paragraph(caption + ": " + Bicimlendirici.money(fee) + vatText, NORMAL_FONT));
    }

    private void addPaymentsTable(Document document, List<Payment> payments, JobSummary summary) throws DocumentException {
        document.add(new Paragraph("Tahsilat", SECTION_FONT));
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        addHeaderCell(table, "Tarih");
        addHeaderCell(table, "Tutar");
        addHeaderCell(table, "Yöntem");

        for (Payment payment : payments) {
            table.addCell(new Phrase(payment.getPaymentDate().format(Bicimlendirici.DATE), NORMAL_FONT));
            table.addCell(new Phrase(Bicimlendirici.money(payment.getAmount()), NORMAL_FONT));
            table.addCell(new Phrase(EnumLabels.label(payment.getMethod()), NORMAL_FONT));
        }
        document.add(table);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Tahsil Edilen: " + Bicimlendirici.money(summary.collectedTotal()), NORMAL_FONT));
        document.add(new Paragraph("Kalan: " + Bicimlendirici.money(summary.remaining()), SECTION_FONT));
    }

    private void addOptionalLine(Document document, String caption, String value) throws DocumentException {
        if (value != null && !value.isBlank()) {
            document.add(new Paragraph(caption + ": " + value, NORMAL_FONT));
        }
    }

    private void addServicePaymentStatus(Document document, Job job) throws DocumentException {
        document.add(new Paragraph("Ödeme: " + (job.isPaymentReceived() ? "Alındı" : "Alınmadı"), SECTION_FONT));
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_CELL_FONT));
        table.addCell(cell);
    }
}
