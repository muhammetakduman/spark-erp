package com.electrician.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.electrician.tracker.domain.Customer;
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
import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.dto.AttendanceEntry;
import com.electrician.tracker.dto.AttendanceSaveResult;
import com.electrician.tracker.dto.DashboardFigures;
import com.electrician.tracker.dto.EmployeeAttendanceReport;
import com.electrician.tracker.dto.EmployeeWageSummary;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.MaterialSummaryLine;
import com.electrician.tracker.dto.MonthlyReport;
import com.electrician.tracker.dto.MonthlyReportRow;
import com.electrician.tracker.dto.ReportJobFilter;
import com.electrician.tracker.dto.ReportTotals;
import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.dto.VatBreakdown;
import com.electrician.tracker.dto.YearlyReport;
import com.electrician.tracker.service.AttendanceService;
import com.electrician.tracker.service.AylikRaporService;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.service.JobService;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.PaymentService;
import com.electrician.tracker.service.PriceHistoryService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.service.ServiceJobService;
import com.electrician.tracker.service.SessionService;
import com.electrician.tracker.service.exception.ReferencedEntityException;
import com.electrician.tracker.ui.util.Bicimlendirici;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The docs/LAST.md walkthrough, entered through the same services the screens
 * use, on its own SQLite database. Every expected value is worked out by hand
 * in the comments. Rule in force: attendance wages are recorded and shown but
 * never deducted from cost or profit.
 */
@SpringBootTest(classes = SpringConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AylikRaporEntegrasyonTest {

    private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);
    private static final YearMonth AUGUST = YearMonth.of(2026, 8);
    private static final YearMonth JULY = YearMonth.of(2026, 7);
    private static final YearMonth NOVEMBER = YearMonth.of(2026, 11);
    private static final int YEAR = 2026;
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
    private SessionService sessionService;
    @Autowired
    private PriceHistoryService priceHistoryService;
    @Autowired
    private AylikRaporService aylikRaporService;

    private Employee mehmet;
    private Employee ali;
    private Employee veli;
    private Product sigorta;
    private Product kablo;
    private Job siteA;
    private Job siteB;
    private Job siteC;
    private Job service1;
    private Job service2;
    private MaterialItem panoLine;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE + "?foreign_keys=on");
    }

    private static Path createDatabaseFile() {
        try {
            Path file = Files.createTempFile("aylik-rapor-test", ".db");
            // Deleted on exit: the cached Spring context keeps the SQLite file open.
            file.toFile().deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeAll
    void enterSampleData() {
        sessionService.start(new SessionUser(1L, "admin", "Test Admin", null, UserRole.ADMIN));
        mehmet = employeeService.create(new Employee("Mehmet", money("2500"), true, true));
        ali = employeeService.create(new Employee("Ali", money("1500"), false, true));
        veli = employeeService.create(new Employee("Veli", money("1500"), false, true));

        Product topraklama = productService.create(new Product("Topraklama çubuğu", ProductUnit.PIECE));
        kablo = productService.create(new Product("NYM 3x2,5 kablo", ProductUnit.METER));
        sigorta = productService.create(new Product("Sigorta 16A", ProductUnit.PIECE));
        Product pano = productService.create(new Product("Pano montaj bedeli", ProductUnit.PIECE));

        siteA = site("Şahin Yapı İnşaat A.Ş.", "Kadıköy Blok A", LocalDate.of(2026, 9, 12));
        LocalDate aDate = LocalDate.of(2026, 9, 12);
        addLine(siteA, topraklama, aDate, "10", "185", "240", 20, true);
        addLine(siteA, kablo, aDate, "200", "28", "38", 20, true);
        addLine(siteA, sigorta, aDate, "24", "45", "70", null, null);
        MaterialItem panoItem = new MaterialItem(siteA, pano, aDate, BigDecimal.ONE, null, null, null,
                PriceEntryType.TOTAL, money("3000"), 20, null);
        panoItem.setVatIncluded(false);
        panoLine = materialService.addItem(panoItem);
        attend(siteA, List.of(LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 13), LocalDate.of(2026, 9, 14)),
                new AttendanceEntry(mehmet.getId(), money("2500")), new AttendanceEntry(ali.getId(), money("1500")));
        attend(siteA, List.of(LocalDate.of(2026, 9, 15)),
                new AttendanceEntry(veli.getId(), money("1500"), new BigDecimal("0.5")));
        paymentService.addPayment(new Payment(siteA, LocalDate.of(2026, 9, 16), money("20000"),
                PaymentMethod.TRANSFER, null));

        siteB = site("Demir İnşaat", "Ataşehir Villa", LocalDate.of(2026, 9, 20));
        addLine(siteB, sigorta, LocalDate.of(2026, 9, 20), "10", "45", "70", null, null);
        attend(siteB, List.of(LocalDate.of(2026, 9, 20)), new AttendanceEntry(mehmet.getId(), money("2500")));

        MaterialItem serviceLine = new MaterialItem(null, sigorta, null, new BigDecimal("2"), money("45"), null,
                money("70"), PriceEntryType.UNIT, null, null, null);
        service1 = serviceJobService.save(null, service("Ahmet Bey", "Pano arızası", LocalDate.of(2026, 9, 23),
                "1000", "500", true), List.of(materialService.prepareDraft(serviceLine)));
        service2 = serviceJobService.save(null, service("Ayşe Hanım", "Priz değişimi", LocalDate.of(2026, 9, 24),
                "800", null, false), List.of());

        siteC = site("Kaya Yapı", "Beykoz", LocalDate.of(2026, 8, 5));
        addLine(siteC, sigorta, LocalDate.of(2026, 8, 5), "5", "45", "70", null, null);
        attend(siteC, List.of(LocalDate.of(2026, 8, 5)), new AttendanceEntry(ali.getId(), money("1500")));
    }

    // ---- A. Line calculations ------------------------------------------------------------

    @Test
    @Order(1)
    void a_lineCalculations() {
        JobSummary a = summary(siteA);
        // 1. Topraklama 10 × 240 = 2.400 incl. 20% → 2.000 excl. + 400 VAT (checked through the job's split below)
        // 2. Pano: TOTAL entry kept, 3.000 excl. + 20% = 3.600 in the grand total
        MaterialItem pano = materialService.findByJob(siteA.getId()).stream()
                .filter(item -> item.getId().equals(panoLine.getId())).findFirst().orElseThrow();
        assertThat(pano.getPriceEntryType()).isEqualTo(PriceEntryType.TOTAL);
        assertThat(pano.getSaleTotalAmount()).isEqualByComparingTo("3000");
        // 3. Exactly one line (Pano) without a purchase price
        assertThat(a.materialPurchasePriceMissingCount()).isEqualTo(1);
        // Topraklama alone: 2.400 incl. 20% → 2.000 + 400
        VatBreakdown topraklama = com.electrician.tracker.service.KdvHesaplayici.calculate(List.of(
                new com.electrician.tracker.service.KdvHesaplayici.Line(money("2400"), 20, true)));
        assertThat(topraklama.excludingVat()).isEqualByComparingTo("2000.00");
        assertThat(topraklama.vatTotal()).isEqualByComparingTo("400.00");
    }

    // ---- B. Attendance -------------------------------------------------------------------

    @Test
    @Order(2)
    void b_attendance() {
        JobSummary a = summary(siteA);
        // 4. 3 + 3 + 0,5 = 6,5 days, shown as "6,5 gün"
        assertThat(a.attendanceDayCount()).isEqualByComparingTo("6.5");
        assertThat(Bicimlendirici.daysWithUnit(a.attendanceDayCount())).isEqualTo("6,5 gün");
        // 5. 2.500×3 + 1.500×3 + 1.500×0,5 = 12.750
        assertThat(a.attendanceTotal()).isEqualByComparingTo("12750.00");

        // 6. Mehmet: Site A 3 days, Site B 1 day, 4 in total
        EmployeeAttendanceReport report = attendanceService.reportForEmployee(mehmet.getId(),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        assertThat(report.jobs()).hasSize(2);
        assertThat(report.jobs().get(0).dayCount()).isEqualByComparingTo("3");
        assertThat(report.jobs().get(1).dayCount()).isEqualByComparingTo("1");
        assertThat(report.dayCount()).isEqualByComparingTo("4");

        // 7. Same employee, same day on another job → a conflict the dialog warns about;
        //    same job, same day again → skipped silently.
        List<LocalDate> sept12 = List.of(LocalDate.of(2026, 9, 12));
        List<AttendanceEntry> mehmetEntry = List.of(new AttendanceEntry(mehmet.getId(), money("2500")));
        assertThat(attendanceService.findConflicts(siteB.getId(), sept12, mehmetEntry)).hasSize(1);
        AttendanceSaveResult again = attendanceService.saveBatch(siteA.getId(), sept12, mehmetEntry);
        assertThat(again.createdCount()).isZero();
        assertThat(again.skippedCount()).isEqualTo(1);
    }

    // ---- C. Site A summary ---------------------------------------------------------------

    @Test
    @Order(3)
    void c_siteASummary() {
        JobSummary a = summary(siteA);
        // 8. Sale excl.: 2.000 + 6.333,33 + 1.680 + 3.000 = 13.013,33
        //    VAT 20%: (10.000 incl. → 1.666,67) + (3.000 excl. → 600) = 2.266,67; grand total 15.280,00
        assertThat(a.saleExcludingVat()).isEqualByComparingTo("13013.33");
        assertThat(a.saleVat().rates()).hasSize(1);
        assertThat(a.saleVat().rates().get(0).rate()).isEqualTo(20);
        assertThat(a.saleVatAmount()).isEqualByComparingTo("2266.67");
        assertThat(a.saleIncludingVat()).isEqualByComparingTo("15280.00");
        // 9. Cost = material purchase 1.850 + 5.600 + 1.080 = 8.530 (wages are not a cost)
        assertThat(a.costExcludingVat()).isEqualByComparingTo("8530.00");
        // 10. Profit 13.013,33 − 8.530 = 4.483,33, estimated (1 line without purchase price)
        assertThat(a.profit()).isEqualByComparingTo("4483.33");
        assertThat(a.isProfitEstimated()).isTrue();
        // 11. Collected 20.000; 15.280 − 20.000 = −4.720 → overpaid, nothing owed
        assertThat(a.collectedTotal()).isEqualByComparingTo("20000.00");
        assertThat(a.remaining()).isEqualByComparingTo("-4720.00");
        assertThat(a.isFullyPaid()).isTrue();
    }

    // ---- D. Services ---------------------------------------------------------------------

    @Test
    @Order(4)
    void d_services() {
        // 12. 1.000 + 500 + 2 × 70 = 1.640, paid
        JobSummary s1 = summary(service1);
        assertThat(s1.saleIncludingVat()).isEqualByComparingTo("1640.00");
        assertThat(s1.paymentReceived()).isTrue();
        assertThat(s1.isFullyPaid()).isTrue();
        // 13. 800, not paid
        JobSummary s2 = summary(service2);
        assertThat(s2.saleIncludingVat()).isEqualByComparingTo("800.00");
        assertThat(s2.paymentReceived()).isFalse();
        assertThat(s2.remaining()).isEqualByComparingTo("800.00");
        // 14. Pending service payment on the main screen: 800
        DashboardFigures figures = jobSummaryService.loadDashboard(SEPTEMBER);
        assertThat(figures.pendingServicePayment()).isEqualByComparingTo("800.00");
    }

    // ---- E. Monthly report ---------------------------------------------------------------

    @Test
    @Order(5)
    void e_monthlyReport() {
        JobBoard board = aylikRaporService.loadBoard();
        MonthlyReport september = aylikRaporService.monthly(board, SEPTEMBER, ReportJobFilter.ALL);
        ReportTotals t = september.totals();
        // 15. Revenue 13.013,33 + 700 + 1.640 + 800 = 16.153,33; VAT (info) 2.266,67
        //     Cost 8.530 + 450 + 90 = 9.070; profit 7.083,33 (estimated, 1 line)
        //     Uncollected: A 0 (overpaid) + B 700 + S1 0 + S2 800 = 1.500; wages (info) 12.750 + 2.500 = 15.250
        assertThat(t.revenue()).isEqualByComparingTo("16153.33");
        assertThat(t.vatAmount()).isEqualByComparingTo("2266.67");
        assertThat(t.cost()).isEqualByComparingTo("9070.00");
        assertThat(t.profit()).isEqualByComparingTo("7083.33");
        assertThat(t.uncollected()).isEqualByComparingTo("1500.00");
        assertThat(t.wageTotal()).isEqualByComparingTo("15250.00");
        assertThat(t.missingPurchasePriceCount()).isEqualTo(1);
        // 16. Four rows, Site C (August) not among them
        assertThat(september.rows()).extracting(MonthlyReportRow::jobId)
                .containsExactly(siteA.getId(), siteB.getId(), service1.getId(), service2.getId());

        // 17. August: only Site C — revenue 350, cost 225, profit 125, uncollected 350, wage 1.500
        MonthlyReport august = aylikRaporService.monthly(board, AUGUST, ReportJobFilter.ALL);
        assertThat(august.rows()).extracting(MonthlyReportRow::jobId).containsExactly(siteC.getId());
        assertThat(august.totals().revenue()).isEqualByComparingTo("350.00");
        assertThat(august.totals().cost()).isEqualByComparingTo("225.00");
        assertThat(august.totals().profit()).isEqualByComparingTo("125.00");
        assertThat(august.totals().uncollected()).isEqualByComparingTo("350.00");
        assertThat(august.totals().wageTotal()).isEqualByComparingTo("1500.00");

        // 18. July: empty month, all zero, no error
        MonthlyReport july = aylikRaporService.monthly(board, JULY, ReportJobFilter.ALL);
        assertThat(july.rows()).isEmpty();
        assertThat(july.materials()).isEmpty();
        assertThat(july.wages()).isEmpty();
        assertThat(july.totals().revenue()).isZero();
        assertThat(july.totals().profit()).isZero();
        assertThat(july.totals().uncollected()).isZero();

        // 19. Services only: 2 rows; revenue 2.440, cost 90, profit 2.350, uncollected 800
        MonthlyReport services = aylikRaporService.monthly(board, SEPTEMBER, ReportJobFilter.SERVICES);
        assertThat(services.rows()).hasSize(2);
        assertThat(services.totals().revenue()).isEqualByComparingTo("2440.00");
        assertThat(services.totals().cost()).isEqualByComparingTo("90.00");
        assertThat(services.totals().profit()).isEqualByComparingTo("2350.00");
        assertThat(services.totals().uncollected()).isEqualByComparingTo("800.00");

        // 20. Sigorta 16A in September: 24 + 10 + 2 = 36 pieces; purchase 1.620, sale 2.520
        MaterialSummaryLine fuse = september.materials().stream()
                .filter(line -> line.productName().equals("Sigorta 16A")).findFirst().orElseThrow();
        assertThat(fuse.quantity()).isEqualByComparingTo("36");
        assertThat(fuse.purchaseTotal()).isEqualByComparingTo("1620.00");
        assertThat(fuse.saleTotal()).isEqualByComparingTo("2520.00");

        // 21. Wages: Mehmet 4 days (10.000), Ali 3 days (4.500), Veli 0,5 day (750)
        assertThat(september.wages())
                .extracting(EmployeeWageSummary::employeeName, w -> w.dayCount().stripTrailingZeros(),
                        EmployeeWageSummary::totalWage)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Mehmet", new BigDecimal("4"), new BigDecimal("10000.00")),
                        org.assertj.core.groups.Tuple.tuple("Ali", new BigDecimal("3"), new BigDecimal("4500.00")),
                        org.assertj.core.groups.Tuple.tuple("Veli", new BigDecimal("0.5"), new BigDecimal("750.00")));

        // 22. Year: only August and September filled; total = both months
        YearlyReport year = aylikRaporService.yearly(board, YEAR, ReportJobFilter.ALL);
        assertThat(year.months()).hasSize(12);
        year.months().forEach(month -> {
            boolean filled = month.month().equals(AUGUST) || month.month().equals(SEPTEMBER);
            assertThat(month.totals().revenue().signum() != 0).as(month.month().toString()).isEqualTo(filled);
        });
        assertThat(year.total().revenue()).isEqualByComparingTo("16503.33");
        assertThat(year.total().profit()).isEqualByComparingTo("7208.33");
        assertThat(year.total().uncollected()).isEqualByComparingTo("1850.00");
    }

    @Test
    @Order(6)
    void e_exportsMatchTheScreen() throws IOException {
        MonthlyReport september = aylikRaporService.monthly(SEPTEMBER, ReportJobFilter.ALL);
        // 23. Excel: 4 job rows + total row with the same figures
        Path excel = Files.createTempFile("aylik-rapor", ".xlsx");
        aylikRaporService.exportExcel(september, excel);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(excel.toFile()))) {
            Sheet jobs = workbook.getSheet("İşler");
            assertThat(jobs.getLastRowNum()).isEqualTo(5);
            Row total = jobs.getRow(5);
            assertThat(total.getCell(0).getStringCellValue()).isEqualTo("TOPLAM");
            assertThat(total.getCell(7).getNumericCellValue()).isEqualTo(16153.33);
            assertThat(total.getCell(10).getNumericCellValue()).isEqualTo(7083.33);
        }
        // PDF: opens and shows the same totals
        Path pdf = Files.createTempFile("aylik-rapor", ".pdf");
        aylikRaporService.exportPdf(september, pdf);
        try (PdfReader reader = new PdfReader(Files.readAllBytes(pdf))) {
            String text = new PdfTextExtractor(reader).getTextFromPage(1);
            assertThat(text).contains("16.153,33").contains("7.083,33").contains("1.500,00");
        }
        Files.deleteIfExists(excel);
        Files.deleteIfExists(pdf);
    }

    // ---- F. Filters and panels -----------------------------------------------------------

    @Test
    @Order(7)
    void f_usageHistoryAndSupplier() {
        // 25. Sigorta 16A given 4 times (A, B, C, Service 1)
        assertThat(priceHistoryService.usageReport(sigorta.getId(), null, null).rows()).hasSize(4);

        // 26. A new supplier is accepted and suggested at once; a case/letter variant is the same supplier
        Job siteD = site("Test Müşteri", "Kasım Şantiyesi", NOVEMBER.atDay(2));
        MaterialItem first = line(siteD, kablo, NOVEMBER.atDay(2), "5", "28", "38");
        first.setSupplierName("Elektrik Market");
        materialService.addItem(first);
        assertThat(materialService.findDistinctSupplierNames()).containsExactly("Elektrik Market");
        MaterialItem second = line(siteD, kablo, NOVEMBER.atDay(3), "5", "28", "38");
        second.setSupplierName("  elektrik   market ");
        assertThat(materialService.addItem(second).getSupplierName()).isEqualTo("Elektrik Market");
        assertThat(materialService.findDistinctSupplierNames()).containsExactly("Elektrik Market");
    }

    @Test
    @Order(8)
    void f_completingSiteWithBalance() {
        // 24. Site B still owes 700: the form warns (outstanding balance > 0) and, once completed,
        //     it is listed as "bitmiş ama tahsilatı eksik"
        assertThat(jobSummaryService.outstandingBalance(siteB.getId()).orElseThrow()).isEqualByComparingTo("700.00");
        jobService.changeStatus(siteB.getId(), JobStatus.COMPLETED);
        DashboardFigures figures = jobSummaryService.loadDashboard(SEPTEMBER);
        assertThat(figures.completedSitesWithBalance()).extracting(Job::getId).containsExactly(siteB.getId());
        assertThat(figures.activeSiteCount()).isEqualTo(3);
    }

    // ---- G. Editing and deleting ---------------------------------------------------------

    @Test
    @Order(9)
    void g_editingAndDeleting() {
        // 27. Changing a quantity changes the job summary and the monthly report
        MaterialItem fuseB = materialService.findByJob(siteB.getId()).get(0);
        MaterialItem changed = line(siteB, sigorta, fuseB.getItemDate(), "20", "45", "70");
        materialService.update(fuseB.getId(), changed);
        assertThat(summary(siteB).saleExcludingVat()).isEqualByComparingTo("1400.00");
        assertThat(aylikRaporService.monthly(SEPTEMBER, ReportJobFilter.ALL).totals().revenue())
                .isEqualByComparingTo("16853.33");

        // 28. Deleting Veli's half day: 6,5 → 6 days, 12.750 → 12.000
        Long veliRow = attendanceService.findByJob(siteA.getId()).stream()
                .filter(row -> row.getEmployee().getId().equals(veli.getId()))
                .findFirst().orElseThrow().getId();
        attendanceService.delete(veliRow);
        assertThat(summary(siteA).attendanceDayCount()).isEqualByComparingTo("6");
        assertThat(summary(siteA).attendanceTotal()).isEqualByComparingTo("12000.00");

        // 29. Mehmet has attendance: cannot be deleted (the screen then offers deactivation)
        assertThatThrownBy(() -> employeeService.delete(mehmet.getId()))
                .isInstanceOf(ReferencedEntityException.class);
    }

    // ---- Helpers -------------------------------------------------------------------------

    private JobSummary summary(Job job) {
        return jobSummaryService.summarize(job.getId());
    }

    private Job site(String customerName, String siteName, LocalDate start) {
        Customer customer = customerService.create(new Customer(customerName, null, null, null, null));
        return jobService.create(new Job(customer, JobType.SITE, siteName, null, start, null, JobStatus.ACTIVE,
                null, null, false, null));
    }

    private Job service(String customerName, String description, LocalDate date, String serviceFee,
            String laborFee, boolean paid) {
        Customer customer = customerService.create(new Customer(customerName, null, null, null, null));
        return new Job(customer, JobType.SERVICE, description, null, date, null, JobStatus.COMPLETED,
                money(serviceFee), laborFee == null ? null : money(laborFee), paid, null);
    }

    private void addLine(Job job, Product product, LocalDate date, String quantity, String purchase, String sale,
            Integer vatRate, Boolean vatIncluded) {
        MaterialItem item = new MaterialItem(job, product, date, new BigDecimal(quantity), money(purchase), null,
                money(sale), PriceEntryType.UNIT, null, vatRate, null);
        item.setVatIncluded(vatIncluded);
        materialService.addItem(item);
    }

    private MaterialItem line(Job job, Product product, LocalDate date, String quantity, String purchase,
            String sale) {
        return new MaterialItem(job, product, date, new BigDecimal(quantity), money(purchase), null, money(sale),
                PriceEntryType.UNIT, null, null, null);
    }

    private void attend(Job job, List<LocalDate> dates, AttendanceEntry... entries) {
        attendanceService.saveBatch(job.getId(), dates, List.of(entries));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
