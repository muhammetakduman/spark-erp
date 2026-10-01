package com.electrician.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.electrician.tracker.config.SpringConfig;
import com.electrician.tracker.domain.Company;
import com.electrician.tracker.domain.CurrencyCode;
import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.DailyJobPriority;
import com.electrician.tracker.domain.DiscountType;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.domain.PaymentMethod;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.domain.QuoteStatus;
import com.electrician.tracker.dto.AttendanceEntry;
import com.electrician.tracker.dto.DailyJobDraft;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.MaterialSummaryLine;
import com.electrician.tracker.dto.MonthlyReport;
import com.electrician.tracker.dto.QuoteDraft;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.dto.QuoteView;
import com.electrician.tracker.dto.ReportJobFilter;
import com.electrician.tracker.dto.ReportTotals;
import com.electrician.tracker.dto.YearlyReport;
import com.electrician.tracker.service.AttendanceService;
import com.electrician.tracker.service.AuthenticationService;
import com.electrician.tracker.service.AylikRaporService;
import com.electrician.tracker.service.CompanyService;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.DailyJobService;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.service.JobService;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.PaymentService;
import com.electrician.tracker.service.PriceHistoryService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.service.QuoteLineNumbering;
import com.electrician.tracker.service.QuoteService;
import com.electrician.tracker.service.ReportService;
import com.electrician.tracker.service.ServiceJobService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Release check: one realistic month entered through the services the screens
 * use, then every PDF and Excel output the program can produce, with the
 * figures worked out by hand in the comments. Mixed VAT rates (none, 10, 20,
 * included and excluded), a dollar purchase, a fractional quantity, a total
 * price entry, half days, partial payments and a percent discount.
 * <p>
 * The files are written to {@code -Drelease.check.dir=...} when given (to
 * look at them), otherwise to a temporary folder.
 */
@SpringBootTest(classes = SpringConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReleaseCheckIntegrationTest {

    private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);
    private static final LocalDate SITE_START = LocalDate.of(2026, 9, 3);
    private static final LocalDate SERVICE_DATE = LocalDate.of(2026, 9, 18);
    private static final Path DATABASE = createDatabaseFile();

    @Autowired
    private CustomerService customerService;
    @Autowired
    private ProductService productService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JobService jobService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ServiceJobService serviceJobService;
    @Autowired
    private JobSummaryService jobSummaryService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private PriceHistoryService priceHistoryService;
    @Autowired
    private AylikRaporService aylikRaporService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private QuoteService quoteService;
    @Autowired
    private DailyJobService dailyJobService;
    @Autowired
    private CompanyService companyService;

    private Path outputFolder;
    private Product cable;
    private Product fuse;
    private Product ledPanel;
    private Employee mehmet;
    private Employee omer;
    private Job site;
    private Job service;
    private Job octoberService;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE + "?foreign_keys=on");
    }

    private static Path createDatabaseFile() {
        try {
            Path file = Files.createTempFile("release-check", ".db");
            file.toFile().deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeAll
    void enterOneMonth() throws IOException {
        String folder = System.getProperty("release.check.dir");
        outputFolder = folder == null ? Files.createTempDirectory("release-check") : Files.createDirectories(Path.of(folder));
        authenticationService.setUpFirstAdmin("admin", "Muhammet Akduman", "Elektrik Mühendisi", "gizli123",
                "gizli123");

        Company company = Company.empty();
        company.setName("Işıklar Elektrik Taahhüt Ltd. Şti.");
        company.setAddress("Çağlayan Mah. Gül Sok. No: 5 Şişli / İstanbul");
        company.setPhone("0212 555 44 33");
        company.setTaxOffice("Şişli");
        company.setTaxNo("1234567890");
        companyService.save(company);

        mehmet = employeeService.create(new Employee("Mehmet Usta", money("2500"), true, true));
        omer = employeeService.create(new Employee("Ömer Çırak", money("1500"), false, true));
        cable = productService.create(new Product("NYM 3x2,5 kablo", ProductUnit.METER, "Öznur", "Kablo"));
        fuse = productService.create(new Product("Sigorta 16A", ProductUnit.PIECE, "Schneider", "Sigorta"));
        ledPanel = productService.create(new Product("LED Panel 60x60", ProductUnit.PIECE, null, "Aydınlatma"));
        Product rcd = productService.create(new Product("Kaçak akım rölesi 40A", ProductUnit.PIECE));

        Customer siteCustomer = customerService.create(new Customer("Işıklar Yapı İnşaat A.Ş.", "0532 111 22 33",
                "Çamlıca Mah. Üsküdar / İstanbul", "9876543210", null));
        Job newSite = new Job(siteCustomer, JobType.SITE, "Çağlayan Konutları B Blok", "Çağlayan / Kağıthane",
                SITE_START, null, JobStatus.ACTIVE, null, money("5000"), false, null);
        newSite.setLaborFeeVat(20, false);
        site = jobService.create(newSite);

        // Sale 12,5 × 38 = 475 excl. 20%; purchase 12,5 × 28 = 350 excl. 20%
        MaterialItem cableLine = line(cable, SITE_START, "12.5", "28", "38", 20, false);
        cableLine.setPurchaseVat(20, false);
        cableLine.setSupplierName("Öznur Kablo");
        materialService.addItem(cableLine);
        // Sale 24 × 70 = 1.680 incl. 20%; purchase 24 × 45 = 1.080 incl. 20% → 900 excl.
        MaterialItem fuseLine = line(fuse, LocalDate.of(2026, 9, 5), "24", "45", "70", 20, true);
        fuseLine.setPurchaseVat(20, true);
        fuseLine.setSupplierName("Elektrik Market");
        materialService.addItem(fuseLine);
        // Sale 8 × 900 = 7.200 excl. 10%; purchase 12,50 $ × 41,25 = 515,625 TL × 8 = 4.125 (no VAT)
        MaterialItem ledLine = line(ledPanel, LocalDate.of(2026, 9, 10), "8", "12.50", "900", 10, false);
        ledLine.setPurchaseCurrency(CurrencyCode.USD, new BigDecimal("41.25"), null);
        ledLine.setSupplierName("Işık Dünyası");
        materialService.addItem(ledLine);
        // Total entry: 3 pieces for 2.500, no VAT, no purchase price
        materialService.addItem(new MaterialItem(site, rcd, LocalDate.of(2026, 9, 12), new BigDecimal("3"), null,
                null, null, PriceEntryType.TOTAL, money("2500"), null, null));

        attendanceService.saveBatch(site.getId(), List.of(SITE_START, SITE_START.plusDays(1), SITE_START.plusDays(2)),
                List.of(new AttendanceEntry(mehmet.getId(), money("2500"))));
        attendanceService.saveBatch(site.getId(), List.of(SITE_START),
                List.of(new AttendanceEntry(omer.getId(), money("1500"))));
        attendanceService.saveBatch(site.getId(), List.of(SITE_START.plusDays(1)),
                List.of(new AttendanceEntry(omer.getId(), money("1500"), new BigDecimal("0.5"))));

        payment(site, LocalDate.of(2026, 9, 4), "5000", PaymentMethod.CASH);
        payment(site, LocalDate.of(2026, 9, 15), "7500", PaymentMethod.TRANSFER);
        payment(site, LocalDate.of(2026, 9, 25), "2000", PaymentMethod.CHECK);

        // Service: fee 1.500 incl. 20%, labor 750 no VAT, 2 fuses × 70 no VAT (purchase 2 × 45 incl. 20%)
        Customer serviceCustomer = customerService.create(new Customer("Şükrü Güneş", "0555 444 33 22",
                "Moda Cad. No: 12 Kadıköy", null, null));
        Job newService = new Job(serviceCustomer, JobType.SERVICE, "Pano arızası ve sigorta değişimi", null,
                SERVICE_DATE, null, JobStatus.COMPLETED, money("1500"), money("750"), false, null);
        newService.setServiceFeeVat(20, true);
        MaterialItem serviceFuse = new MaterialItem(null, fuse, null, new BigDecimal("2"), money("45"), null,
                money("70"), PriceEntryType.UNIT, null, null, null);
        serviceFuse.setPurchaseVat(20, true);
        service = serviceJobService.save(null, newService, List.of(materialService.prepareDraft(serviceFuse)));
        payment(service, SERVICE_DATE, "1000", PaymentMethod.CASH);

        Customer octoberCustomer = customerService.create(new Customer("Gülşen Hanım", null, null, null, null));
        octoberService = serviceJobService.save(null, new Job(octoberCustomer, JobType.SERVICE, "Priz değişimi",
                null, LocalDate.of(2026, 10, 2), null, JobStatus.COMPLETED, money("600"), null, true, null),
                List.of());
    }

    @Test
    void siteSummary() {
        JobSummary summary = jobSummaryService.summarize(site.getId());
        // 20%: 1.680 incl. → 1.400 + (475 + 5.000 labor) excl. = 6.875 net, VAT 1.375
        // 10%: 7.200 net, VAT 720; no VAT: 2.500
        assertThat(summary.saleExcludingVat()).isEqualByComparingTo("16575.00");
        assertThat(summary.saleVatAmount()).isEqualByComparingTo("2095.00");
        assertThat(summary.saleIncludingVat()).isEqualByComparingTo("18670.00");
        // Cost: 350 + 900 + 4.125 = 5.375; profit 11.200 (estimated: the relay has no purchase price)
        assertThat(summary.costExcludingVat()).isEqualByComparingTo("5375.00");
        assertThat(summary.profit()).isEqualByComparingTo("11200.00");
        assertThat(summary.materialPurchasePriceMissingCount()).isEqualTo(1);
        // Payments 5.000 + 7.500 + 2.000 = 14.500; remaining 4.170
        assertThat(summary.collectedTotal()).isEqualByComparingTo("14500.00");
        assertThat(summary.remaining()).isEqualByComparingTo("4170.00");
        // Days 3 + 1 + 0,5; wages 7.500 + 1.500 + 750
        assertThat(summary.attendanceDayCount()).isEqualByComparingTo("4.5");
        assertThat(summary.attendanceTotal()).isEqualByComparingTo("9750.00");
    }

    @Test
    void serviceSummary() {
        JobSummary summary = jobSummaryService.summarize(service.getId());
        // 1.500 incl. 20% → 1.250 + 250 VAT; 750 + 140 without VAT
        assertThat(summary.saleExcludingVat()).isEqualByComparingTo("2140.00");
        assertThat(summary.saleVatAmount()).isEqualByComparingTo("250.00");
        assertThat(summary.saleIncludingVat()).isEqualByComparingTo("2390.00");
        assertThat(summary.costExcludingVat()).isEqualByComparingTo("75.00");
        assertThat(summary.profit()).isEqualByComparingTo("2065.00");
        assertThat(summary.remaining()).isEqualByComparingTo("1390.00");
    }

    @Test
    void dashboardAndReports() {
        DashboardFigures dashboard = jobSummaryService.loadDashboard(SEPTEMBER);
        assertThat(dashboard.monthlyRevenue()).isEqualByComparingTo("18715.00");
        assertThat(dashboard.monthlyProfit()).isEqualByComparingTo("13265.00");

        MonthlyReport september = aylikRaporService.monthly(SEPTEMBER, ReportJobFilter.ALL);
        ReportTotals totals = september.totals();
        assertThat(september.rows()).hasSize(2);
        assertThat(totals.revenue()).isEqualByComparingTo("18715.00");
        assertThat(totals.vatAmount()).isEqualByComparingTo("2345.00");
        assertThat(totals.cost()).isEqualByComparingTo("5450.00");
        assertThat(totals.profit()).isEqualByComparingTo("13265.00");
        assertThat(totals.collected()).isEqualByComparingTo("15500.00");
        assertThat(totals.uncollected()).isEqualByComparingTo("5560.00");
        assertThat(totals.wageTotal()).isEqualByComparingTo("9750.00");
        // Fuses: 24 + 2 = 26; purchase (1.080 + 90) incl. 20% → 975; sale 1.680 incl. → 1.400 + 140 = 1.540
        MaterialSummaryLine fuses = september.materials().get(0);
        assertThat(fuses.productName()).isEqualTo("Sigorta 16A");
        assertThat(fuses.quantity()).isEqualByComparingTo("26");
        assertThat(fuses.purchaseTotal()).isEqualByComparingTo("975.00");
        assertThat(fuses.saleTotal()).isEqualByComparingTo("1540.00");

        YearlyReport year = aylikRaporService.yearly(2026, ReportJobFilter.ALL);
        assertThat(year.total().revenue()).isEqualByComparingTo("19315.00");
    }

    @Test
    void everyOutputFile() throws IOException {
        Path sitePdf = outputFolder.resolve("1-santiye.pdf");
        reportService.generateJobPdf(site.getId(), sitePdf);
        assertThat(pdfText(sitePdf)).contains("Işıklar Yapı İnşaat A.Ş.", "Çağlayan Konutları", "16.575,00",
                "18.670,00", "14.500,00", "4.170,00");

        Path servicePdf = outputFolder.resolve("2-servis.pdf");
        reportService.generateJobPdf(service.getId(), servicePdf);
        assertThat(pdfText(servicePdf)).contains("Şükrü Güneş", "2.390,00", "1.390,00");

        MonthlyReport september = aylikRaporService.monthly(SEPTEMBER, ReportJobFilter.ALL);
        Path monthlyPdf = outputFolder.resolve("3-aylik-rapor.pdf");
        aylikRaporService.exportPdf(september, monthlyPdf);
        assertThat(pdfText(monthlyPdf)).contains("18.715,00", "13.265,00", "5.560,00");

        Path monthlyExcel = outputFolder.resolve("4-aylik-rapor.xlsx");
        aylikRaporService.exportExcel(september, monthlyExcel);
        try (XSSFWorkbook workbook = workbook(monthlyExcel)) {
            Row total = workbook.getSheet("İşler").getRow(3);
            assertThat(total.getCell(0).getStringCellValue()).isEqualTo("TOPLAM");
            assertThat(total.getCell(7).getNumericCellValue()).isEqualTo(18715.0);
            assertThat(total.getCell(10).getNumericCellValue()).isEqualTo(13265.0);
            assertThat(total.getCell(12).getNumericCellValue()).isEqualTo(5560.0);
            assertThat(total.getCell(12).getCellStyle().getDataFormatString()).isEqualTo("#,##0.00");
        }

        Path rangeExcel = outputFolder.resolve("5-is-listesi.xlsx");
        reportService.exportJobsByDateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31), rangeExcel);
        try (XSSFWorkbook workbook = workbook(rangeExcel)) {
            Sheet jobs = workbook.getSheet("İşler");
            assertThat(jobs.getLastRowNum()).isEqualTo(3);
            Row siteRow = jobs.getRow(1);
            assertThat(siteRow.getCell(5).getNumericCellValue()).isEqualTo(16575.0);
            assertThat(siteRow.getCell(8).getNumericCellValue()).isEqualTo(18670.0);
            assertThat(siteRow.getCell(14).getNumericCellValue()).isEqualTo(4170.0);
            Sheet materials = workbook.getSheet("Malzemeler");
            assertThat(materials.getLastRowNum()).isEqualTo(5);
        }

        Path attendanceExcel = outputFolder.resolve("6-puantaj.xlsx");
        reportService.exportAttendanceMatrix(SEPTEMBER.atDay(1), SEPTEMBER.atEndOfMonth(), attendanceExcel);
        try (XSSFWorkbook workbook = workbook(attendanceExcel)) {
            Sheet sheet = workbook.getSheet("Puantaj");
            int totalColumn = SEPTEMBER.lengthOfMonth() + 1;
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
            assertThat(rowOf(sheet, "Mehmet Usta").getCell(totalColumn).getNumericCellValue()).isEqualTo(3.0);
            Row omerRow = rowOf(sheet, "Ömer Çırak");
            assertThat(omerRow.getCell(totalColumn).getNumericCellValue()).isEqualTo(1.5);
            assertThat(omerRow.getCell(SITE_START.getDayOfMonth() + 1).getStringCellValue()).contains("(½)");
        }

        Path usageExcel = outputFolder.resolve("7-urun-gecmisi.xlsx");
        priceHistoryService.exportUsage(ledPanel, null, null, usageExcel);
        try (XSSFWorkbook workbook = workbook(usageExcel)) {
            Row row = workbook.getSheetAt(0).getRow(1);
            assertThat(row.getCell(6).getStringCellValue()).isEqualTo("USD");
            assertThat(row.getCell(7).getNumericCellValue()).isEqualTo(12.5);
            assertThat(row.getCell(8).getNumericCellValue()).isEqualTo(41.25);
            assertThat(row.getCell(9).getNumericCellValue()).isEqualTo(515.625);
            assertThat(row.getCell(14).getNumericCellValue()).isEqualTo(7200.0);
        }

        // Quote: 100 × 38,50 + 20 × 72,25 + 10 × 950 = 14.795; 7,5% discount 1.109,63;
        // + 4.000 labor = 17.685,37; 20% VAT 3.537,07; grand total 21.222,44
        QuoteView quote = quoteService.save(null, quoteDraft());
        assertThat(quote.totals().grandTotal()).isEqualByComparingTo("21222.44");
        Path quotePdf = outputFolder.resolve("8-teklif.pdf");
        quoteService.exportPdf(quote.id(), quotePdf);
        assertThat(pdfText(quotePdf)).contains("14.795,00", "1.109,63", "17.685,37", "3.537,07", "21.222,44");

        dailyJobService.save(null, dailyJob("09:00", "Pano kontrolü", "Şükrü Güneş"));
        dailyJobService.save(null, dailyJob("14:30", "Aydınlatma arızası", "Gülşen Hanım"));
        Path dailyPdf = outputFolder.resolve("9-gunluk-plan.pdf");
        dailyJobService.exportDayPdf(SERVICE_DATE, dailyPdf);
        assertThat(pdfText(dailyPdf)).contains("Pano kontrolü", "Aydınlatma arızası", "Mehmet Usta");
    }

    private QuoteDraft quoteDraft() {
        List<QuoteLine> lines = List.of(
                QuoteLine.unnumbered(cable.getId(), cable.getName(), cable.getBrand(), new BigDecimal("100"),
                        ProductUnit.METER, new BigDecimal("38.50"), null),
                QuoteLine.unnumbered(fuse.getId(), fuse.getName(), fuse.getBrand(), new BigDecimal("20"),
                        ProductUnit.PIECE, new BigDecimal("72.25"), null),
                QuoteLine.unnumbered(ledPanel.getId(), ledPanel.getName(), null, new BigDecimal("10"),
                        ProductUnit.PIECE, new BigDecimal("950"), null));
        return new QuoteDraft(quoteService.suggestNumber(SERVICE_DATE), SERVICE_DATE, 15, null,
                "Işıklar Yapı İnşaat A.Ş.", "Çamlıca Mah. Üsküdar / İstanbul", "Gökhan Öztürk", "0532 111 22 33",
                null, "info@isiklar.test", "C Blok elektrik tesisatı", DiscountType.PERCENT, new BigDecimal("7.5"),
                new BigDecimal("4000"), 20, "Fiyatlarımıza nakliye dahildir.", QuoteStatus.SENT,
                QuoteLineNumbering.normalize(lines), false, null, null);
    }

    private DailyJobDraft dailyJob(String time, String title, String customerName) {
        return new DailyJobDraft(SERVICE_DATE, time, title, null, customerName, "Kadıköy", "0555 444 33 22",
                List.of(mehmet.getId(), omer.getId()), DailyJobPriority.NORMAL, null, null);
    }

    private MaterialItem line(Product product, LocalDate date, String quantity, String purchase, String sale,
            Integer vatRate, Boolean vatIncluded) {
        MaterialItem item = new MaterialItem(site, product, date, new BigDecimal(quantity), money(purchase), null,
                money(sale), PriceEntryType.UNIT, null, vatRate, null);
        item.setVatIncluded(vatIncluded);
        return item;
    }

    private void payment(Job job, LocalDate date, String amount, PaymentMethod method) {
        paymentService.addPayment(new Payment(job, date, money(amount), method, null));
    }

    private static Row rowOf(Sheet sheet, String employeeName) {
        for (Row row : sheet) {
            if (row.getCell(0).getStringCellValue().equals(employeeName)) {
                return row;
            }
        }
        throw new AssertionError("No row for " + employeeName);
    }

    private static XSSFWorkbook workbook(Path file) throws IOException {
        try (FileInputStream in = new FileInputStream(file.toFile())) {
            return new XSSFWorkbook(in);
        }
    }

    private static String pdfText(Path pdf) throws IOException {
        try (PdfReader reader = new PdfReader(Files.readAllBytes(pdf))) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        }
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
