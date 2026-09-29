package com.electrician.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.electrician.tracker.config.SpringConfig;
import com.electrician.tracker.domain.Company;
import com.electrician.tracker.domain.CurrencyCode;
import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.DailyJobPriority;
import com.electrician.tracker.domain.DailyJobStatus;
import com.electrician.tracker.domain.DiscountType;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.domain.QuoteStatus;
import com.electrician.tracker.domain.TemplateType;
import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.dto.DailyJobDraft;
import com.electrician.tracker.dto.DayPlan;
import com.electrician.tracker.dto.ItemSetApplication;
import com.electrician.tracker.dto.JobSummary;
import com.electrician.tracker.dto.ProductUsageReport;
import com.electrician.tracker.dto.QuoteDraft;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.dto.QuoteView;
import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.dto.TemplateView;
import com.electrician.tracker.service.AttendanceService;
import com.electrician.tracker.service.AuthenticationService;
import com.electrician.tracker.service.CompanyService;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.DailyJobService;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.service.ExchangeRateService;
import com.electrician.tracker.service.JobService;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.PriceHistoryService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.service.QuoteLineNumbering;
import com.electrician.tracker.service.QuoteService;
import com.electrician.tracker.service.SessionService;
import com.electrician.tracker.service.TemplateService;
import com.electrician.tracker.service.exception.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * docs/SANTIYE-TAKIP-GUNCELLEME-TURU-2.md "Doğrulama" steps that can be
 * checked without clicking through the screens, on the real
 * Spring/JPA/SQLite stack with its own database.
 */
@SpringBootTest(classes = SpringConfig.class)
class UpdateTour2IntegrationTest {

    private static final Path DATABASE = createDatabaseFile();
    private static final String TURKISH_SAMPLE = "Şişli Çağlayan Işık İnşaat Güç Öğe";

    @Autowired
    private SessionService sessionService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private ProductService productService;
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JobService jobService;
    @Autowired
    private JobSummaryService jobSummaryService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private PriceHistoryService priceHistoryService;
    @Autowired
    private ExchangeRateService exchangeRateService;
    @Autowired
    private QuoteService quoteService;
    @Autowired
    private TemplateService templateService;
    @Autowired
    private DailyJobService dailyJobService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private AuthenticationService authenticationService;

    private static Long adminId;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE + "?foreign_keys=on");
    }

    private static Path createDatabaseFile() {
        try {
            Path file = Files.createTempFile("tour2-test", ".db");
            file.toFile().deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A real ADMIN row, so entries that remember their creator satisfy the foreign key. */
    @BeforeEach
    void logInAsAdmin() {
        if (adminId == null) {
            adminId = authenticationService.setUpFirstAdmin("admin", "Test Admin", null, "gizli123", "gizli123")
                    .id();
        }
        login(UserRole.ADMIN);
    }

    // ---- 1–2: quote customer ---------------------------------------------------

    @Test
    void newCustomerIsAddedOnceAndLinkedWhateverTheCase() {
        int before = customerService.findAll().size();
        QuoteView first = quoteService.save(null, draft("Yılmaz Elektrik Ltd", true, List.of(freeLine("Kablo"))));

        Customer created = customerService.findById(first.draft().customerId());
        assertThat(created.getName()).isEqualTo("Yılmaz Elektrik Ltd");
        assertThat(created.getEmail()).isEqualTo("info@yilmaz.test");
        assertThat(created.getAddress()).isEqualTo("Kadıköy");

        QuoteView second = quoteService.save(null, draft("  YILMAZ elektrik LTD ", true, List.of(freeLine("Priz"))));
        assertThat(second.draft().customerId()).isEqualTo(first.draft().customerId());
        assertThat(customerService.findAll()).hasSize(before + 1);
    }

    @Test
    void uncheckedNewNameIsKeptWithoutCustomerRecord() {
        QuoteView saved = quoteService.save(null, draft("Kayıtsız Firma", false, List.of(freeLine("Kablo"))));

        assertThat(saved.draft().customerId()).isNull();
        assertThat(saved.draft().companyName()).isEqualTo("Kayıtsız Firma");
        assertThat(customerService.findByName("kayitsiz firma")).isEmpty();
    }

    // ---- 3–5: brand and numbering ----------------------------------------------

    @Test
    void quoteKeepsItsOwnBrandAndTheNumberOrder() {
        Product product = productService.create(new Product("Anahtar", ProductUnit.PIECE, "ÖZNUR", null));
        List<QuoteLine> lines = List.of(
                new QuoteLine(2, null, "Serbest", null, BigDecimal.ONE, ProductUnit.SET, BigDecimal.TEN, null),
                new QuoteLine(1, product.getId(), "Anahtar", "HES", BigDecimal.ONE, ProductUnit.PIECE,
                        BigDecimal.TEN, null),
                new QuoteLine(3, product.getId(), "Anahtar", "", BigDecimal.ONE, ProductUnit.PIECE, BigDecimal.TEN,
                        null));
        QuoteView saved = quoteService.save(null, draft("Marka Testi", false, lines));
        product.setBrand("DEĞİŞTİ");
        productService.update(product.getId(), product);

        List<QuoteLine> reloaded = quoteService.findById(saved.id()).draft().lines();
        assertThat(reloaded).extracting(QuoteLine::lineNo).containsExactly(1, 2, 3);
        assertThat(reloaded).extracting(QuoteLine::productName).containsExactly("Anahtar", "Serbest", "Anahtar");
        assertThat(reloaded).extracting(QuoteLine::brand).containsExactly("HES", null, null);
    }

    @Test
    void freeQuoteItemIsAddedToCatalogOnceAndGetsSupplierAndCost() {
        List<QuoteLine> lines = List.of(
                QuoteLine.unnumbered(null, "Sıva altı buat", "Viko", BigDecimal.ONE, ProductUnit.PIECE,
                        BigDecimal.TEN, null),
                QuoteLine.unnumbered(null, "  SIVA ALTI  buat ", "viko", BigDecimal.ONE, ProductUnit.PIECE,
                        BigDecimal.TEN, null));
        QuoteView saved = quoteService.save(null, draft("Katalog Testi", false, lines));

        List<QuoteLine> reloaded = quoteService.findById(saved.id()).draft().lines();
        assertThat(reloaded).allSatisfy(line -> assertThat(line.isFreeItem()).isFalse());
        assertThat(reloaded).extracting(QuoteLine::productId).containsOnly(reloaded.get(0).productId());

        Product product = productService.findById(reloaded.get(0).productId());
        product.setSupplierName("  Elektrik Market ");
        product.setPurchasePrice(new BigDecimal("42.50"));
        productService.update(product.getId(), product);
        Product edited = productService.findById(product.getId());
        assertThat(edited.getSupplierName()).isEqualTo("Elektrik Market");
        assertThat(edited.getPurchasePrice()).isEqualByComparingTo("42.50");
    }

    // ---- 9–10: one-page PDF with Turkish letters -------------------------------

    @Test
    void longQuotePdfFitsOnePageWithTurkishLetters() throws IOException {
        Company company = Company.empty();
        company.setName(TURKISH_SAMPLE);
        company.setAddress("Çamlıca Mah. Gül Sok. No: 5 Üsküdar / İstanbul");
        company.setPhone("0216 555 44 33");
        company.setLogo(logo());
        company.setBrandColor("#1F4E79");
        companyService.save(company);
        for (int lineCount : List.of(22, QuoteLineNumbering.MAX_LINES)) {
            List<QuoteLine> lines = new ArrayList<>();
            for (int i = 1; i <= lineCount; i++) {
                lines.add(new QuoteLine(i, null, "NYM 3x2,5 kablo çift izoleli şantiye tipi " + i, "ÖZNUR",
                        new BigDecimal("125.5"), ProductUnit.METER, new BigDecimal("48.75"), null));
            }
            QuoteDraft full = new QuoteDraft(quoteService.suggestNumber(LocalDate.now()), LocalDate.now(), 15, null,
                    "Ğ".repeat(10) + " İnşaat Çelik Şirketi", "a".repeat(120), "İlgili Kişi", "0532 000 00 00",
                    null, "x@y.test", "Konu: şantiye elektrik tesisatı", DiscountType.PERCENT, new BigDecimal("5"),
                    new BigDecimal("2500"), 20, "Ş".repeat(600), QuoteStatus.DRAFT, lines, false, null, null);
            QuoteView saved = quoteService.save(null, full);
            Path pdf = Files.createTempFile("teklif", ".pdf");
            pdf.toFile().deleteOnExit();

            quoteService.exportPdf(saved.id(), pdf);

            PdfReader reader = new PdfReader(pdf.toString());
            try {
                assertThat(reader.getNumberOfPages()).as("%d kalem tek sayfa", lineCount).isEqualTo(1);
                String text = new PdfTextExtractor(reader).getTextFromPage(1);
                assertThat(text).contains("TEKLİF FORMU", "GENEL TOPLAM", "Şişli", "Çağlayan", "Işık", "İnşaat");
            } finally {
                reader.close();
            }
        }
    }

    // ---- 11–12: a MANAGER cannot reach sites or the dashboard -------------------

    @Test
    void managerIsRefusedSitesDashboardAndBoardButGetsServices() {
        Customer customer = customerService.create(new Customer("Yetki Testi", null, null, null, null));
        Job site = jobService.create(new Job(customer, JobType.SITE, "Kilitli Şantiye", null, LocalDate.now(), null,
                JobStatus.ACTIVE, null, null, false, null));
        login(UserRole.MANAGER);

        assertThatThrownBy(() -> jobSummaryService.loadBoard()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> jobSummaryService.loadDashboard(java.time.YearMonth.now()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> jobSummaryService.loadJobDetail(site.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> jobService.findById(site.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> jobService.create(new Job(customer, JobType.SITE, "Yeni", null, null, null,
                JobStatus.ACTIVE, null, null, false, null))).isInstanceOf(AccessDeniedException.class);
        assertThat(jobSummaryService.loadServiceBoard().jobs()).allMatch(job -> job.getType() == JobType.SERVICE);
    }

    // ---- 13–15: foreign currency --------------------------------------------------

    @Test
    void dollarPurchaseIsFrozenInLiraAndUsedForProfitAndComparison() {
        Customer customer = customerService.create(new Customer("Döviz Testi", null, null, null, null));
        Job site = jobService.create(new Job(customer, JobType.SITE, "Döviz Şantiyesi", null, LocalDate.now(), null,
                JobStatus.ACTIVE, null, null, false, null));
        Product cable = productService.create(new Product("Döviz Kablo", ProductUnit.METER));
        // Lira line: 2 × 100 purchase, 2 × 150 sale. Dollar line: 1 × 120 $ @ 34,25 = 4.110 purchase, 5.000 sale.
        materialService.addItem(line(site, cable, "2", "100", CurrencyCode.TRY, null, "150", "Ucuzcu"));
        MaterialItem dollar = materialService.addItem(line(site, cable, "1", "120", CurrencyCode.USD, "34.25", "5000",
                "Pahalıcı"));

        assertThat(dollar.getPurchaseUnitPriceTl()).isEqualByComparingTo("4110.00");
        assertThat(exchangeRateService.find(CurrencyCode.USD).orElseThrow().rate()).isEqualByComparingTo("34.25");

        exchangeRateService.update(CurrencyCode.USD, new BigDecimal("40"));
        MaterialItem reloaded = materialService.findByJob(site.getId()).stream()
                .filter(MaterialItem::isForeignCurrencyPurchase).findFirst().orElseThrow();
        assertThat(reloaded.getPurchaseUnitPriceTl()).isEqualByComparingTo("4110.00");

        JobSummary summary = jobSummaryService.summarize(site.getId());
        // Sale 300 + 5.000 = 5.300; cost 200 + 4.110 = 4.310; profit 990.
        assertThat(summary.costExcludingVat()).isEqualByComparingTo("4310.00");
        assertThat(summary.profit()).isEqualByComparingTo("990.00");
        assertThat(summary.foreignCurrencyLineCount()).isEqualTo(1);

        ProductUsageReport usage = priceHistoryService.usageReport(cable.getId(), null, null);
        assertThat(usage.suppliers()).extracting(comparison -> comparison.supplierName())
                .containsExactly("Ucuzcu", "Pahalıcı");
        assertThat(usage.rows()).anySatisfy(row -> {
            assertThat(row.isForeignCurrencyPurchase()).isTrue();
            assertThat(row.purchaseUnitPrice()).isEqualByComparingTo("4110.00");
            assertThat(row.purchaseOriginalUnitPrice()).isEqualByComparingTo("120");
        });
    }

    // ---- 16–19: daily plan -----------------------------------------------------------

    @Test
    void dayCountersPostponementChainAttendanceAndPending() {
        LocalDate today = dailyJobService.today();
        Customer customer = customerService.create(new Customer("Plan Müşterisi", null, "Moda Cad.", null, null));
        Job site = jobService.create(new Job(customer, JobType.SITE, "Plan Şantiyesi", null, today.minusDays(10),
                null, JobStatus.ACTIVE, null, null, false, null));
        Employee usta = employeeService.create(new Employee("Plan Usta", new BigDecimal("2000"), true, true));

        DailyJobCard went = dailyJobService.save(null, dailyDraft(today, "Gidilen iş", site.getId(), usta));
        DailyJobCard missed = dailyJobService.save(null, dailyDraft(today, "Gidilemeyen iş", null, usta));
        dailyJobService.save(null, dailyDraft(today, "Bekleyen iş", null, usta));
        assertThat(dailyJobService.findOtherBookings(null, dailyDraft(today, "Dördüncü", null, usta)))
                .singleElement().satisfies(booking -> assertThat(booking.otherJobCount()).isEqualTo(3));

        int attendanceBefore = attendanceService.findByJob(site.getId()).size();
        dailyJobService.complete(went.id(), null, true);
        dailyJobService.markNotVisited(missed.id(), "Müşteri evde yoktu", true);

        DayPlan todayPlan = dailyJobService.dayPlan(today);
        assertThat(todayPlan.plannedCount()).isEqualTo(1);
        assertThat(todayPlan.completedCount()).isEqualTo(1);
        assertThat(todayPlan.notVisitedCount()).isEqualTo(1);
        assertThat(attendanceService.findByJob(site.getId())).hasSize(attendanceBefore + 1);

        DailyJobCard tomorrow = dailyJobService.dayPlan(today.plusDays(1)).cards().stream()
                .filter(card -> card.title().equals("Gidilemeyen iş")).findFirst().orElseThrow();
        assertThat(tomorrow.status()).isEqualTo(DailyJobStatus.PLANNED);
        assertThat(tomorrow.postponeCount()).isEqualTo(1);
        dailyJobService.markNotVisited(tomorrow.id(), null, true);
        DailyJobCard dayAfter = dailyJobService.dayPlan(today.plusDays(2)).cards().stream()
                .filter(card -> card.title().equals("Gidilemeyen iş")).findFirst().orElseThrow();
        assertThat(dayAfter.postponeCount()).isEqualTo(2);

        DailyJobCard forgotten = dailyJobService.save(null, dailyDraft(today.minusDays(1), "Unutulan iş", null, usta));
        assertThat(dailyJobService.pending()).extracting(DailyJobCard::id).contains(forgotten.id());
        assertThat(dailyJobService.pendingCount()).isPositive();
        dailyJobService.moveToToday(forgotten.id());
        assertThat(dailyJobService.pending()).extracting(DailyJobCard::id).doesNotContain(forgotten.id());
        assertThat(dailyJobService.dayPlan(today).cards()).extracting(DailyJobCard::title).contains("Unutulan iş");
    }

    @Test
    void unlinkedVisitBecomesServiceAndOnlyServicesAreLinkable() {
        LocalDate today = dailyJobService.today();
        Customer customer = customerService.create(new Customer("Link Müşterisi", null, null, null, null));
        jobService.create(new Job(customer, JobType.SITE, "Link Şantiyesi", null, today, null, JobStatus.ACTIVE,
                null, null, false, null));
        assertThat(dailyJobService.linkableJobs()).allSatisfy(option ->
                assertThat(option.type()).isEqualTo(JobType.SERVICE));

        Employee usta = employeeService.create(new Employee("Servis Usta", new BigDecimal("1500"), true, true));
        DailyJobDraft draft = new DailyJobDraft(today, null, "Priz arızası", null, "Yeni Servis Müşterisi", "Kadıköy",
                "0555", List.of(usta.getId()), DailyJobPriority.NORMAL, null, null);
        DailyJobCard visit = dailyJobService.save(null, draft);

        assertThat(dailyJobService.complete(visit.id(), null, true).serviceCreated()).isTrue();

        Customer created = customerService.findByName("Yeni Servis Müşterisi").orElseThrow();
        DailyJobCard done = dailyJobService.dayPlan(today).cards().stream()
                .filter(card -> card.id().equals(visit.id())).findFirst().orElseThrow();
        Job service = jobService.findById(done.jobId());
        assertThat(service.getType()).isEqualTo(JobType.SERVICE);
        assertThat(service.getCustomer().getId()).isEqualTo(created.getId());
        assertThat(service.getName()).isEqualTo("Priz arızası");
        assertThat(service.getServiceFee()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(attendanceService.findByJob(service.getId())).hasSize(1);
    }

    // ---- 26–27: templates ------------------------------------------------------------

    @Test
    void defaultNoteAndItemSetTemplates() {
        TemplateView note = templateService.saveText(null, "Standart şartlar", TemplateType.QUOTE_NOTE,
                "Ödeme peşin.");
        templateService.setDefault(note.id(), true);
        assertThat(quoteService.newQuote("yedek").draft().notes()).isEqualTo("Ödeme peşin.");

        Product kept = productService.create(new Product("Şablon Priz", ProductUnit.PIECE, "Viko", null));
        Product removed = productService.create(new Product("Silinecek Ürün", ProductUnit.PIECE));
        List<QuoteLine> five = QuoteLineNumbering.appendAll(List.of(), List.of(
                catalogLine(kept), freeLine("Sarf"), freeLine("İşçilik"), catalogLine(removed), freeLine("Nakliye")));
        TemplateView set = templateService.saveItemSet("Standart daire", five);
        productService.delete(removed.getId());

        List<QuoteLine> two = QuoteLineNumbering.appendAll(List.of(), List.of(freeLine("İlk"), freeLine("İkinci")));
        ItemSetApplication applied = templateService.applyItemSet(set.id(), two);

        assertThat(applied.skippedCount()).isEqualTo(1);
        assertThat(applied.lines()).extracting(QuoteLine::lineNo).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(applied.lines()).extracting(QuoteLine::productName)
                .containsExactly("İlk", "İkinci", "Şablon Priz", "Sarf", "İşçilik", "Nakliye");
    }

    // ---- helpers ----------------------------------------------------------------------

    private void login(UserRole role) {
        sessionService.start(new SessionUser(adminId, "test", "Test Kullanıcı", null, role));
    }

    private QuoteDraft draft(String companyName, boolean addToList, List<QuoteLine> lines) {
        return new QuoteDraft(quoteService.suggestNumber(LocalDate.now()), LocalDate.now(), 15, null, companyName,
                "Kadıköy", null, "0532 111 22 33", null, "info@yilmaz.test", "Tesisat", DiscountType.NONE, null, null,
                20, "Notlar", QuoteStatus.DRAFT, QuoteLineNumbering.normalize(lines), addToList, null, null);
    }

    private static QuoteLine freeLine(String name) {
        return QuoteLine.unnumbered(null, name, null, BigDecimal.ONE, ProductUnit.SET, new BigDecimal("100"), null);
    }

    private static QuoteLine catalogLine(Product product) {
        return QuoteLine.unnumbered(product.getId(), product.getName(), product.getBrand(), BigDecimal.ONE,
                product.getUnit(), new BigDecimal("50"), null);
    }

    private static MaterialItem line(Job job, Product product, String quantity, String purchase,
            CurrencyCode currency, String rate, String sale, String supplier) {
        MaterialItem item = new MaterialItem(job, product, LocalDate.now(), new BigDecimal(quantity),
                new BigDecimal(purchase), supplier, new BigDecimal(sale), PriceEntryType.UNIT, null, null, null);
        item.setPurchaseCurrency(currency, rate == null ? null : new BigDecimal(rate), null);
        return item;
    }

    private static DailyJobDraft dailyDraft(LocalDate date, String title, Long jobId, Employee employee) {
        return new DailyJobDraft(date, "9:30", title, null, "Ahmet Bey", "Moda", "0555", List.of(employee.getId()),
                DailyJobPriority.NORMAL, null, jobId);
    }

    private static byte[] logo() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(400, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB), "png",
                out);
        return out.toByteArray();
    }
}
