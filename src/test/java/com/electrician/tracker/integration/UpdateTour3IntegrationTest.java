package com.electrician.tracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.electrician.tracker.config.SpringConfig;
import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.DailyJobPriority;
import com.electrician.tracker.domain.DailyJobStatus;
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
import com.electrician.tracker.domain.TemplateType;
import com.electrician.tracker.domain.ThemeMode;
import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.dto.BulkDeletionResult;
import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.dto.DailyJobDraft;
import com.electrician.tracker.dto.QuoteDraft;
import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.dto.QuoteView;
import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.dto.TemplateView;
import com.electrician.tracker.service.AttendanceService;
import com.electrician.tracker.service.AuthenticationService;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.DailyJobService;
import com.electrician.tracker.service.EmployeeService;
import com.electrician.tracker.service.JobService;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.PaymentService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.service.QuoteService;
import com.electrician.tracker.service.SessionService;
import com.electrician.tracker.service.TemplateService;
import com.electrician.tracker.service.UserPreferenceService;
import com.electrician.tracker.service.exception.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * docs/SPARK-ERP-GUNCELLEME-TURU-3.md "Doğrulama" steps that can be checked
 * without clicking through the screens (bulk delete, quote preparer, pending
 * reminder, template usage, per-user preferences), on the real
 * Spring/JPA/SQLite stack with its own database.
 */
@SpringBootTest(classes = SpringConfig.class)
class UpdateTour3IntegrationTest {

    private static final Path DATABASE = createDatabaseFile();
    private static final int BULK_SIZE = 12;
    private static final int USED_PRODUCTS = 4;

    @Autowired
    private SessionService sessionService;
    @Autowired
    private AuthenticationService authenticationService;
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
    private QuoteService quoteService;
    @Autowired
    private TemplateService templateService;
    @Autowired
    private DailyJobService dailyJobService;
    @Autowired
    private UserPreferenceService preferences;

    private static Long adminId;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE + "?foreign_keys=on");
    }

    private static Path createDatabaseFile() {
        try {
            Path file = Files.createTempFile("tour3-test", ".db");
            file.toFile().deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeEach
    void logInAsAdmin() {
        if (adminId == null) {
            adminId = authenticationService.setUpFirstAdmin("admin", "Test Admin", "Teknisyen", "gizli123",
                    "gizli123").id();
        }
        login(adminId, "Test Admin", "Teknisyen", UserRole.ADMIN);
    }

    // ---- 5: bulk delete of sites with everything that belongs to them ----------------------

    @Test
    void twelveSitesAreCountedAndDeletedWithTheirRecords() {
        Customer customer = customerService.create(new Customer("Toplu Müşteri", null, null, null, null));
        Product cable = productService.create(new Product("Toplu Kablo", ProductUnit.METER));
        Employee usta = employeeService.create(new Employee("Toplu Usta", new BigDecimal("1000"), true, true));
        List<Long> siteIds = new ArrayList<>();
        for (int i = 0; i < BULK_SIZE; i++) {
            Job site = jobService.create(new Job(customer, JobType.SITE, "Toplu Şantiye " + i, null,
                    LocalDate.now(), null, JobStatus.ACTIVE, null, null, false, null));
            materialService.addItem(new MaterialItem(site, cable, LocalDate.now(), BigDecimal.ONE, null, null,
                    BigDecimal.TEN, PriceEntryType.UNIT, null, null, null));
            materialService.addItem(new MaterialItem(site, cable, LocalDate.now(), BigDecimal.ONE, null, null,
                    BigDecimal.TEN, PriceEntryType.UNIT, null, null, null));
            attendanceService.saveFullDays(site.getId(), LocalDate.now(), List.of(usta.getId()));
            paymentService.addPayment(new Payment(site, LocalDate.now(), BigDecimal.TEN, PaymentMethod.CASH, null));
            siteIds.add(site.getId());
        }

        Map<String, Long> impact = jobService.bulkDeletionImpact(siteIds);
        assertThat(impact).containsEntry("delete.count.materials", 24L)
                .containsEntry("delete.count.attendance", 12L)
                .containsEntry("delete.count.payments", 12L);

        BulkDeletionResult result = jobService.deleteAll(siteIds);
        assertThat(result.deletedCount()).isEqualTo(BULK_SIZE);
        assertThat(result.skipped()).isEmpty();
        assertThat(jobService.bulkDeletionImpact(siteIds)).isEmpty();
        assertThat(jobService.findActiveAccessible()).extracting(Job::getId).doesNotContainAnyElementsOf(siteIds);
    }

    // ---- 6: products with references are skipped, then made inactive ------------------------

    @Test
    void usedProductsAreSkippedAndCanBeMadeInactive() {
        Customer customer = customerService.create(new Customer("Ürün Müşterisi", null, null, null, null));
        Job site = jobService.create(new Job(customer, JobType.SITE, "Ürün Şantiyesi", null, LocalDate.now(), null,
                JobStatus.ACTIVE, null, null, false, null));
        List<Long> productIds = new ArrayList<>();
        for (int i = 0; i < BULK_SIZE; i++) {
            Product product = productService.create(new Product("Toplu Ürün " + i, ProductUnit.PIECE));
            if (i < USED_PRODUCTS) {
                materialService.addItem(new MaterialItem(site, product, LocalDate.now(), BigDecimal.ONE, null, null,
                        BigDecimal.TEN, PriceEntryType.UNIT, null, null, null));
            }
            productIds.add(product.getId());
        }

        BulkDeletionResult result = productService.deleteAll(productIds);
        assertThat(result.deletedCount()).isEqualTo(BULK_SIZE - USED_PRODUCTS);
        assertThat(result.skipped()).hasSize(USED_PRODUCTS)
                .allSatisfy(skipped -> {
                    assertThat(skipped.reasonKey()).isEqualTo("error.product.delete.hasMaterialItems");
                    assertThat(skipped.count()).isEqualTo(1);
                });
        assertThat(result.skippedIds()).containsExactlyInAnyOrderElementsOf(productIds.subList(0, USED_PRODUCTS));

        productService.deactivateAll(result.skippedIds());
        assertThat(productService.findAllActive()).extracting(Product::getId)
                .doesNotContainAnyElementsOf(productIds);
        assertThat(productService.findAll()).extracting(Product::getId)
                .containsAll(productIds.subList(0, USED_PRODUCTS));
    }

    @Test
    void customersWithJobsAndEmployeesWithAttendanceAreSkipped() {
        Customer busy = customerService.create(new Customer("Meşgul Müşteri", null, null, null, null));
        Customer free = customerService.create(new Customer("Boş Müşteri", null, null, null, null));
        Job site = jobService.create(new Job(busy, JobType.SITE, "Meşgul Şantiye", null, LocalDate.now(), null,
                JobStatus.ACTIVE, null, null, false, null));
        BulkDeletionResult customers = customerService.deleteAll(List.of(busy.getId(), free.getId()));
        assertThat(customers.deletedCount()).isEqualTo(1);
        assertThat(customers.skippedIds()).containsExactly(busy.getId());
        customerService.deactivateAll(customers.skippedIds());
        assertThat(customerService.findAllActive()).extracting(Customer::getId).doesNotContain(busy.getId());

        Employee working = employeeService.create(new Employee("Çalışan Usta", new BigDecimal("900"), false, true));
        Employee idle = employeeService.create(new Employee("Boştaki Usta", new BigDecimal("900"), false, true));
        attendanceService.saveFullDays(site.getId(), LocalDate.now(), List.of(working.getId()));
        BulkDeletionResult employees = employeeService.deleteAll(List.of(working.getId(), idle.getId()));
        assertThat(employees.deletedCount()).isEqualTo(1);
        assertThat(employees.skipped()).singleElement().satisfies(skipped ->
                assertThat(skipped.reasonKey()).isEqualTo("error.employee.delete.hasAttendance"));
    }

    @Test
    void onlyAnAdminMayDeleteInBulk() {
        Product product = productService.create(new Product("Yetki Ürünü", ProductUnit.PIECE));
        login(adminId, "Müdür", null, UserRole.MANAGER);
        assertThatThrownBy(() -> productService.deleteAll(List.of(product.getId())))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> templateService.deleteAll(List.of(1L))).isInstanceOf(AccessDeniedException.class);
    }

    // ---- 11–12: "Hazırlayan" of a quote ------------------------------------------------------

    @Test
    void typedPreparerIsKeptAndSuggestedForTheNextQuote() {
        QuoteView saved = quoteService.save(null, quoteDraft("Ali Yılmaz", "Saha Sorumlusu"));
        assertThat(saved.preparedByName()).isEqualTo("Ali Yılmaz");
        assertThat(saved.preparedByTitle()).isEqualTo("Saha Sorumlusu");

        QuoteView next = quoteService.newQuote("");
        assertThat(next.draft().preparedByName()).isEqualTo("Ali Yılmaz");
        assertThat(next.draft().preparedByTitle()).isEqualTo("Saha Sorumlusu");
    }

    @Test
    void blankPreparerBecomesTheLoggedInUser() {
        QuoteView saved = quoteService.save(null, quoteDraft("  ", null));
        assertThat(saved.preparedByName()).isEqualTo("Test Admin");
        assertThat(saved.preparedByTitle()).isEqualTo("Teknisyen");
    }

    // ---- 13–16: pending reminder ----------------------------------------------------------------

    @Test
    void reminderListsForgottenJobsAndSettlesThem() {
        LocalDate today = dailyJobService.today();
        DailyJobCard first = dailyJobService.save(null, dailyDraft(today.minusDays(1), "Dünkü iş 1"));
        DailyJobCard second = dailyJobService.save(null, dailyDraft(today.minusDays(1), "Dünkü iş 2"));
        DailyJobCard third = dailyJobService.save(null, dailyDraft(today.minusDays(2), "Gidilmeyen iş"));
        dailyJobService.markNotVisited(third.id(), "Kapalıydı", false);

        assertThat(dailyJobService.reminderItems()).extracting(DailyJobCard::id)
                .contains(first.id(), second.id(), third.id());

        dailyJobService.moveToToday(first.id());
        List<DailyJobCard> afterMove = dailyJobService.reminderItems();
        assertThat(afterMove).extracting(DailyJobCard::id).doesNotContain(first.id());
        assertThat(afterMove).filteredOn(card -> card.title().equals("Dünkü iş 1"))
                .singleElement().satisfies(card -> assertThat(card.date()).isEqualTo(today));

        dailyJobService.completePending(third.id());
        dailyJobService.cancelPending(second.id());
        assertThat(dailyJobService.pending()).extracting(DailyJobCard::id)
                .doesNotContain(first.id(), second.id(), third.id());
        assertThat(dailyJobService.dayPlan(today.minusDays(2)).cards()).filteredOn(card -> card.id().equals(third.id()))
                .singleElement().satisfies(card -> assertThat(card.status()).isEqualTo(DailyJobStatus.COMPLETED));
    }

    @Test
    void moveAllToTodayMovesEveryEarlierJobInOneStep() {
        LocalDate today = dailyJobService.today();
        DailyJobCard a = dailyJobService.save(null, dailyDraft(today.minusDays(3), "Toplu taşı A"));
        DailyJobCard b = dailyJobService.save(null, dailyDraft(today.minusDays(4), "Toplu taşı B"));

        assertThat(dailyJobService.moveAllToToday(List.of(a.id(), b.id()))).isEqualTo(2);
        assertThat(dailyJobService.dayPlan(today).cards()).extracting(DailyJobCard::title)
                .contains("Toplu taşı A", "Toplu taşı B");
        assertThat(dailyJobService.pending()).extracting(DailyJobCard::id).doesNotContain(a.id(), b.id());
    }

    @Test
    void reminderIsShownOncePerUserAndDayAndCanBeSwitchedOff() {
        LocalDate today = LocalDate.of(2026, 9, 29);
        Long otherUser = adminId + 1000;
        assertThat(preferences.isReminderDue(today)).isTrue();
        preferences.markReminderShown(today);
        assertThat(preferences.isReminderDue(today)).isFalse();
        assertThat(preferences.isReminderDue(today.plusDays(1))).isTrue();

        login(otherUser, "Başka", null, UserRole.ADMIN);
        assertThat(preferences.isReminderDue(today)).isTrue();
        preferences.setReminderEnabled(false);
        assertThat(preferences.isReminderDue(today)).isFalse();
    }

    // ---- 21: theme per user ------------------------------------------------------------------------

    @Test
    void themeChoiceIsKeptPerUser() {
        assertThat(preferences.themeMode()).isEqualTo(ThemeMode.SYSTEM);
        preferences.setThemeMode(ThemeMode.DARK);
        assertThat(preferences.themeMode()).isEqualTo(ThemeMode.DARK);

        login(adminId + 2000, "İkinci", null, UserRole.ADMIN);
        assertThat(preferences.themeMode()).isEqualTo(ThemeMode.SYSTEM);
        preferences.setThemeMode(ThemeMode.LIGHT);
        login(adminId, "Test Admin", "Teknisyen", UserRole.ADMIN);
        assertThat(preferences.themeMode()).isEqualTo(ThemeMode.DARK);
        preferences.setThemeMode(ThemeMode.SYSTEM);
    }

    // ---- 5: template usage and the item list editor -------------------------------------------------

    @Test
    void templateUseIsCountedAndItemListsCanBeEdited() {
        TemplateView set = templateService.saveItemSet("Kullanım Seti", List.of(freeLine("Buat"), freeLine("Priz")));
        assertThat(set.useCount()).isZero();
        assertThat(set.lastUsedAt()).isNull();

        templateService.applyItemSet(set.id(), List.of());
        templateService.applyItemSet(set.id(), List.of());
        TemplateView used = findTemplate(set.id());
        assertThat(used.useCount()).isEqualTo(2);
        assertThat(used.lastUsedAt()).isNotNull();

        List<QuoteLine> lines = new ArrayList<>(templateService.itemSetLines(set.id()));
        lines.remove(0);
        TemplateView edited = templateService.updateItemSet(set.id(), "Kullanım Seti 2", lines);
        assertThat(edited.name()).isEqualTo("Kullanım Seti 2");
        assertThat(edited.lineCount()).isEqualTo(1);
        assertThat(templateService.itemSetLines(set.id())).extracting(QuoteLine::productName).containsExactly("Priz");

        TemplateView job = templateService.saveText(null, "Priz arızası", TemplateType.JOB_DESCRIPTION,
                "Priz arızası");
        dailyJobService.save(null, dailyDraft(dailyJobService.today(), "priz ARIZASI"));
        assertThat(findTemplate(job.id()).useCount()).isEqualTo(1);
    }

    // ---- helpers --------------------------------------------------------------------------------------

    private void login(Long id, String fullName, String title, UserRole role) {
        sessionService.start(new SessionUser(id, "test", fullName, title, role));
    }

    private TemplateView findTemplate(Long id) {
        return templateService.findAll().stream().filter(template -> template.id().equals(id)).findFirst()
                .orElseThrow();
    }

    private QuoteDraft quoteDraft(String preparedBy, String title) {
        return new QuoteDraft(quoteService.suggestNumber(LocalDate.now()), LocalDate.now(), 15, null, "Hazırlayan Ltd",
                null, null, null, null, null, null, DiscountType.NONE, null, null, 20, "Notlar", QuoteStatus.DRAFT,
                List.of(freeLine("Kablo").withLineNo(1)), false, preparedBy, title);
    }

    private static QuoteLine freeLine(String name) {
        return QuoteLine.unnumbered(null, name, null, BigDecimal.ONE, ProductUnit.SET, new BigDecimal("100"), null);
    }

    private static DailyJobDraft dailyDraft(LocalDate date, String title) {
        return new DailyJobDraft(date, null, title, null, null, null, null, List.of(), DailyJobPriority.NORMAL, null,
                null);
    }
}
