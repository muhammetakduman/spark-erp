package com.electrician.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import com.electrician.tracker.config.SpringConfig;
import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.dto.JobBoard;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.service.AttendanceService;
import com.electrician.tracker.service.BackupService;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.service.JobService;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.PaymentService;
import com.electrician.tracker.service.PriceHistoryService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.service.ReportService;
import com.electrician.tracker.service.SessionService;
import com.electrician.tracker.domain.Payment;
import com.electrician.tracker.domain.PaymentMethod;
import com.electrician.tracker.dto.AttendanceEntry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Exercises the real Spring/JPA/SQLite stack end to end for the main
 * screen's aggregated loading path ({@link JobSummaryService#loadBoard()}),
 * which cannot otherwise be verified without clicking through the UI.
 */
@SpringBootTest(classes = SpringConfig.class)
class JobBoardIntegrationTest {

    private static Path databaseFile;

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
    private JobSummaryService jobSummaryService;
    @Autowired
    private BackupService backupService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private PriceHistoryService priceHistoryService;
    @Autowired
    private SessionService sessionService;

    @BeforeEach
    void logInAsAdmin() {
        sessionService.start(new SessionUser(1L, "admin", "Test Admin", null, UserRole.ADMIN));
    }

    @BeforeAll
    static void createTempDatabase() throws IOException {
        databaseFile = Files.createTempFile("electrician-tracker-test", ".db");
        // Deleted on JVM exit rather than in @AfterAll: the Spring context (and
        // its pooled SQLite connection) is cached beyond this test class and
        // Windows refuses to delete a file that is still open.
        databaseFile.toFile().deleteOnExit();
        System.setProperty("spring.datasource.url", "jdbc:sqlite:" + databaseFile + "?foreign_keys=on");
    }

    @AfterAll
    static void clearDatasourceProperty() {
        System.clearProperty("spring.datasource.url");
    }

    @Test
    void loadsAggregatedBoardWithoutPerJobQueries() {
        Customer customer = customerService.create(new Customer("Test Müşteri", null, null, null, null));
        Product product = productService.create(new Product("Test Kablo", ProductUnit.METER));
        Employee employee = employeeService.create(new Employee("Test Usta", BigDecimal.valueOf(1000), true, true));

        Job site = jobService.create(new Job(customer, JobType.SITE, "Test Şantiye", null,
                LocalDate.now(), null, JobStatus.ACTIVE, null, null, false, null));
        materialService.addItem(new MaterialItem(site, product, LocalDate.now(), BigDecimal.TEN,
                BigDecimal.valueOf(5), null, BigDecimal.valueOf(8), PriceEntryType.UNIT, null, null, null));
        attendanceService.saveBatch(site.getId(), java.util.List.of(LocalDate.now()),
                java.util.List.of(new AttendanceEntry(employee.getId(), BigDecimal.valueOf(1000))));

        JobBoard board = jobSummaryService.loadBoard();

        assertThat(board.jobs()).extracting(Job::getId).contains(site.getId());
        JobSummary summary = board.summaries().get(site.getId());
        assertThat(summary).isNotNull();
        // 10 * 8 (sale) - 10 * 5 (purchase) = 30; the wage is recorded but not deducted
        assertThat(summary.profit()).isEqualByComparingTo("30.00");
        assertThat(board.materialsFor(site.getId())).hasSize(1);
        assertThat(board.attendancesFor(site.getId())).hasSize(1);
    }

    @Test
    void storesFreeTextSupplierNameAndSuggestsPreviouslyUsedNames() {
        Customer customer = customerService.create(new Customer("Tedarikçi Testi", null, null, null, null));
        Product product = productService.create(new Product("Test Priz", ProductUnit.PIECE));
        Job site = jobService.create(new Job(customer, JobType.SITE, "Tedarikçi Şantiyesi", null,
                LocalDate.now(), null, JobStatus.ACTIVE, null, null, false, null));

        MaterialItem saved = materialService.addItem(new MaterialItem(site, product, LocalDate.now(),
                BigDecimal.ONE, BigDecimal.valueOf(3), "Elektrik Pazarlama", BigDecimal.valueOf(5), PriceEntryType.UNIT, null, null, null));

        assertThat(saved.getSupplierName()).isEqualTo("Elektrik Pazarlama");
        assertThat(materialService.findDistinctSupplierNames()).contains("Elektrik Pazarlama");
        assertThat(priceHistoryService.suggestFor(product.getId()).lastSupplierName())
                .isEqualTo("Elektrik Pazarlama");
    }

    @Test
    void persistsTotalPriceEntryAndReusesProductByNormalizedName() {
        Customer customer = customerService.create(new Customer("Toplam Fiyat Testi", null, null, null, null));
        Product product = productService.create(new Product("Işıklı Anahtar", ProductUnit.PIECE));
        Job site = jobService.create(new Job(customer, JobType.SITE, "Toplam Şantiyesi", null,
                LocalDate.now(), null, JobStatus.ACTIVE, null, null, false, null));

        Product picked = productService.createOrReuse("  ISIKLI anahtar ", null, null, ProductUnit.SET).product();
        materialService.addItem(new MaterialItem(site, picked, LocalDate.now(), new BigDecimal("3"),
                null, null, null, PriceEntryType.TOTAL, new BigDecimal("100"), null, null));

        assertThat(picked.getId()).isEqualTo(product.getId());
        MaterialItem reloaded = materialService.findByJob(site.getId()).get(0);
        assertThat(reloaded.getPriceEntryType()).isEqualTo(PriceEntryType.TOTAL);
        assertThat(reloaded.getSaleTotalAmount()).isEqualByComparingTo("100");
        assertThat(reloaded.getSaleUnitPrice()).isEqualByComparingTo("33.33");
        JobSummary summary = jobSummaryService.loadBoard().summaries().get(site.getId());
        assertThat(summary.materialSaleTotalExcludingVat()).isEqualByComparingTo("100.00");
    }

    @Test
    void generatesJobPdfAndDateRangeExcelExport(@TempDir Path tempDir) throws IOException {
        Customer customer = customerService.create(new Customer("PDF Müşteri", null, null, null, null));
        Product product = productService.create(new Product("PDF Kablo", ProductUnit.METER));
        Job site = jobService.create(new Job(customer, JobType.SITE, "PDF Şantiye", null,
                LocalDate.now(), null, JobStatus.ACTIVE, BigDecimal.valueOf(200), BigDecimal.valueOf(300), false, null));
        MaterialItem vatItem = new MaterialItem(site, product, LocalDate.now(), BigDecimal.TEN,
                BigDecimal.valueOf(5), null, BigDecimal.valueOf(8), PriceEntryType.UNIT, null, 10, null);
        vatItem.setVatIncluded(false);
        materialService.addItem(vatItem);
        paymentService.addPayment(new Payment(site, LocalDate.now(), BigDecimal.valueOf(50), PaymentMethod.CASH, null));

        Path pdfFile = tempDir.resolve("report.pdf");
        reportService.generateJobPdf(site.getId(), pdfFile);
        assertThat(Files.exists(pdfFile)).isTrue();
        assertThat(Files.size(pdfFile)).isGreaterThan(0);

        Path excelFile = tempDir.resolve("export.xlsx");
        reportService.exportJobsByDateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), excelFile);
        assertThat(Files.exists(excelFile)).isTrue();
        assertThat(Files.size(excelFile)).isGreaterThan(0);
    }

    @Test
    void backupNowCreatesAFileInTheConfiguredFolder(@TempDir Path tempDir) {
        backupService.setBackupFolder(tempDir);
        Path backupFile = backupService.backupNow();
        assertThat(Files.exists(backupFile)).isTrue();
        assertThat(backupService.listBackups()).contains(backupFile);
    }
}
