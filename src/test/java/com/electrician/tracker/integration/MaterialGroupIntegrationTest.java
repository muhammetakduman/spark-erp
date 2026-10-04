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
import java.util.List;

import com.electrician.tracker.config.SpringConfig;
import com.electrician.tracker.domain.CurrencyCode;
import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.domain.MaterialItem;
import com.electrician.tracker.domain.PriceEntryType;
import com.electrician.tracker.domain.Product;
import com.electrician.tracker.domain.ProductUnit;
import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.dto.GroupJobBreakdown;
import com.electrician.tracker.dto.GroupJobRow;
import com.electrician.tracker.dto.JobDetail;
import com.electrician.tracker.dto.MaterialGroupBreakdown;
import com.electrician.tracker.dto.MaterialGroupTotal;
import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.service.AuthenticationService;
import com.electrician.tracker.service.CustomerService;
import com.electrician.tracker.service.ExchangeRateService;
import com.electrician.tracker.service.JobService;
import com.electrician.tracker.service.JobSummaryService;
import com.electrician.tracker.service.MaterialService;
import com.electrician.tracker.service.ProductCatalogService;
import com.electrician.tracker.service.ProductService;
import com.electrician.tracker.service.ReportService;
import com.electrician.tracker.service.ServiceJobService;
import com.electrician.tracker.service.SessionService;
import com.electrician.tracker.service.exception.AccessDeniedException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * docs/mini_features.md "Doğrulama": material groups per site on the real
 * Spring/JPA/SQLite stack with its own database (grouping, catalogue left
 * alone, totals, masking, outputs, "Gruba Taşı", the product screen's
 * spending per job).
 */
@SpringBootTest(classes = SpringConfig.class)
class MaterialGroupIntegrationTest {

    private static final Path DATABASE = createDatabaseFile();
    private static final LocalDate DAY = LocalDate.of(2026, 10, 1);

    @Autowired
    private SessionService sessionService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private ProductService productService;
    @Autowired
    private JobService jobService;
    @Autowired
    private ServiceJobService serviceJobService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private JobSummaryService jobSummaryService;
    @Autowired
    private ExchangeRateService exchangeRateService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ProductCatalogService productCatalogService;

    private static Long adminId;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE + "?foreign_keys=on");
    }

    private static Path createDatabaseFile() {
        try {
            Path file = Files.createTempFile("material-group-test", ".db");
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
        login(adminId, UserRole.ADMIN);
    }

    // 2, 3: the group belongs to the line; the catalogue and other sites are untouched

    @Test
    void cableMovedToDiafonKeepsItsCatalogueCategoryAndOtherSitesGetKablo() {
        Product cable = product("Grup Kablo 1", "Kablo");
        Job first = site("Grup Şantiye 1");
        MaterialItem line = line(first, cable, "80", "42", "58");
        line.setGroupName("Diafon");
        materialService.addItem(line);

        assertThat(groupNames(first)).containsExactly("Diafon");
        assertThat(productService.findById(cable.getId()).getCategory()).isEqualTo("Kablo");

        Job second = site("Grup Şantiye 2");
        MaterialItem blank = materialService.addItem(line(second, cable, "10", "42", "58"));
        assertThat(blank.getGroupName()).isEqualTo("Kablo");
        assertThat(materialService.findGroupSuggestions(second.getId()).get(0)).isEqualTo("Kablo");
    }

    // 4: "diafon" and "Diafon" are one group, stored with the spelling in use

    @Test
    void caseVariantsEndUpInOneGroup() {
        Product panel = product("Grup Panel 4", null);
        Job site = site("Grup Şantiye 4");
        MaterialItem first = line(site, panel, "1", "100", "150");
        first.setGroupName("Diafon");
        materialService.addItem(first);
        MaterialItem second = line(site, panel, "2", "100", "150");
        second.setGroupName("diafon");

        assertThat(materialService.addItem(second).getGroupName()).isEqualTo("Diafon");
        assertThat(breakdown(site).groups()).hasSize(1);
        assertThat(breakdown(site).groups().get(0).total().lineCount()).isEqualTo(2);
    }

    // 5, 6, 7: header totals, summary total = job material totals, missing purchase price

    @Test
    void groupTotalsMatchHandCalculationAndTheJobsMaterialTotal() {
        Job site = site("Grup Şantiye 5");
        addInGroup(site, product("Grup Ön panel 5", "Diafon"), "1", "8200", "11000", null);
        addInGroup(site, product("Grup Tuş paneli 5", "Diafon"), "6", "1150", "1600", null);
        addInGroup(site, product("Grup Pano 5", "Pano"), "1", "3000", "4500", null);
        MaterialItem vatLine = line(site, product("Grup Sigorta 5", "Pano"), "10", "120", "240");
        vatLine.setVatRate(20);
        vatLine.setVatIncluded(true);
        vatLine.setPurchaseVat(20, true);
        materialService.addItem(vatLine);
        addInGroup(site, product("Grup Klemens 5", "Pano"), "4", null, "25", null);

        MaterialGroupBreakdown breakdown = breakdown(site);
        MaterialGroupTotal diafon = total(breakdown, "Diafon");
        // Purchase 8.200 + 6.900 = 15.100, sale 11.000 + 9.600 = 20.600
        assertThat(diafon.purchaseTotal()).isEqualByComparingTo("15100.00");
        assertThat(diafon.saleTotal()).isEqualByComparingTo("20600.00");
        assertThat(diafon.profit()).isEqualByComparingTo("5500.00");
        MaterialGroupTotal pano = total(breakdown, "Pano");
        // Purchase 3.000 + 1.200 incl. 20% (1.000) = 4.000; sale 4.500 + 2.400 incl. (2.000) + 100 = 6.600
        assertThat(pano.purchaseTotal()).isEqualByComparingTo("4000.00");
        assertThat(pano.saleTotal()).isEqualByComparingTo("6600.00");
        assertThat(pano.purchasePriceMissingCount()).isEqualTo(1);
        assertThat(diafon.purchasePriceMissingCount()).isZero();
        assertThat(breakdown.summary()).extracting(MaterialGroupTotal::groupName).containsExactly("Diafon", "Pano");

        JobDetail detail = jobSummaryService.loadJobDetail(site.getId());
        assertThat(breakdown.total().saleTotal()).isEqualByComparingTo(detail.summary().materialSaleTotalExcludingVat());
        assertThat(breakdown.total().purchaseTotal()).isEqualByComparingTo(detail.summary().materialPurchaseTotal());
        assertThat(breakdown.total().lineCount()).isEqualTo(5);
    }

    // 8: a dollar purchase keeps its lira value when the rate changes later

    @Test
    void changingTheExchangeRateLeavesGroupCostAlone() {
        Job site = site("Grup Şantiye 8");
        MaterialItem dollars = line(site, product("Grup Kamera 8", "Kamera"), "2", "100", "6000");
        dollars.setPurchaseCurrency(CurrencyCode.USD, new BigDecimal("40"), null);
        materialService.addItem(dollars);
        BigDecimal before = total(breakdown(site), "Kamera").purchaseTotal();

        exchangeRateService.update(CurrencyCode.USD, new BigDecimal("55"));

        assertThat(before).isEqualByComparingTo("8000.00");
        assertThat(total(breakdown(site), "Kamera").purchaseTotal()).isEqualByComparingTo(before);
    }

    // 9, 10: a MANAGER never receives cost or profit, in the figures or in outputs

    @Test
    void managerGetsNoCostOrProfitAnywhere() throws IOException {
        Customer customer = customerService.create(new Customer("Grup Servis Müşteri", null, null, null, null));
        Product relay = product("Grup Röle 9", "Pano");
        MaterialItem draft = new MaterialItem(null, relay, null, new BigDecimal("3"), new BigDecimal("37.77"), null,
                new BigDecimal("90"), PriceEntryType.UNIT, null, null, null);
        Job service = serviceJobService.save(null, new Job(customer, JobType.SERVICE, "Grup arıza", null, DAY, null,
                JobStatus.COMPLETED, null, null, false, null), List.of(materialService.prepareDraft(draft)));
        List<MaterialItem> loadedByAdmin = jobSummaryService.loadJobDetail(service.getId()).materials();

        login(adminId + 1, UserRole.MANAGER);
        MaterialGroupBreakdown masked = materialService.groupBreakdown(
                jobSummaryService.loadJobDetail(service.getId()).materials());
        MaterialGroupBreakdown fromAdminList = materialService.groupBreakdown(loadedByAdmin);

        for (MaterialGroupBreakdown breakdown : List.of(masked, fromAdminList)) {
            assertThat(breakdown.hasFinancials()).isFalse();
            assertThat(breakdown.total().purchaseTotal()).isNull();
            assertThat(breakdown.total().profit()).isNull();
            assertThat(breakdown.summary()).allSatisfy(total -> {
                assertThat(total.purchaseTotal()).isNull();
                assertThat(total.profit()).isNull();
                assertThat(total.purchasePriceMissingCount()).isNull();
            });
            assertThat(breakdown.total().saleTotal()).isEqualByComparingTo("270.00");
        }
        Path pdf = Files.createTempFile("manager-service", ".pdf");
        reportService.generateJobPdf(service.getId(), pdf);
        assertThat(pdfText(pdf)).doesNotContain("37,77", "113,31").contains("270,00");
        Path excel = Files.createTempFile("manager-range", ".xlsx");
        assertThatThrownBy(() -> reportService.exportJobsByDateRange(DAY, DAY, excel))
                .isInstanceOf(AccessDeniedException.class);
    }

    // PDF and Excel of a site: grouped, with a subtotal per group

    @Test
    void sitePdfAndExcelAreGroupedWithSubtotals() throws IOException {
        LocalDate day = LocalDate.of(2026, 3, 14);
        Job site = jobService.create(new Job(customerService.create(new Customer("Grup Çıktı Müşteri", null, null,
                null, null)), JobType.SITE, "Grup Çıktı Şantiye", null, day, null, JobStatus.ACTIVE, null, null,
                false, null));
        addInGroup(site, product("Grup Diafon seti PDF", "Diafon"), "1", "8200", "11000", null);
        addInGroup(site, product("Grup Kablo PDF", "Kablo"), "80", "42", "58", "Diafon");
        addInGroup(site, product("Grup Röle PDF", null), "2", "300", "450", null);

        Path pdf = Files.createTempFile("site-grouped", ".pdf");
        reportService.generateJobPdf(site.getId(), pdf);
        String text = pdfText(pdf);
        assertThat(text).contains("Ara toplam – Diafon (KDV'siz)", "15.640,00", "Ara toplam – Kategorisiz (KDV'siz)",
                "900,00");
        assertThat(text.indexOf("Diafon")).isLessThan(text.indexOf("Kategorisiz"));

        Path excel = Files.createTempFile("site-grouped", ".xlsx");
        reportService.exportJobsByDateRange(day, day, excel);
        try (XSSFWorkbook workbook = workbook(excel)) {
            Sheet materials = workbook.getSheet("Malzemeler");
            assertThat(materials.getRow(0).getCell(3).getStringCellValue()).isEqualTo("Grup");
            assertThat(materials.getLastRowNum()).isEqualTo(5);
            assertThat(materials.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Diafon");
            Row diafonSubtotal = materials.getRow(3);
            assertThat(diafonSubtotal.getCell(3).getStringCellValue()).isEqualTo("Ara toplam – Diafon");
            assertThat(diafonSubtotal.getCell(13).getNumericCellValue()).isEqualTo(11560.0);
            assertThat(diafonSubtotal.getCell(14).getNumericCellValue()).isEqualTo(15640.0);
            assertThat(materials.getRow(5).getCell(3).getStringCellValue()).isEqualTo("Ara toplam – Kategorisiz");
        }
    }

    // 11: adding a line updates the totals

    @Test
    void addingALineUpdatesTheGroupTotals() {
        Job site = site("Grup Şantiye 11");
        Product panel = product("Grup Panel 11", "Diafon");
        addInGroup(site, panel, "1", "100", "200", null);
        addInGroup(site, panel, "2", "100", "200", null);

        assertThat(total(breakdown(site), "Diafon").lineCount()).isEqualTo(2);
        assertThat(total(breakdown(site), "Diafon").saleTotal()).isEqualByComparingTo("600.00");
    }

    // Product screen: a group's spending per job, by the line's group (not the product's category)

    @Test
    void groupDistributionFollowsTheLinesGroupAndAddsUpPerJob() {
        Product breaker = product("Dağılım Sigorta", "Dağılım Sigortası");
        Product cable = product("Dağılım Kablo", "Dağılım Kablosu");
        Job big = site("Dağılım Şantiye Büyük");
        Job small = site("Dağılım Şantiye Küçük");
        addInGroup(big, breaker, "10", "100", "150", null);
        addInGroup(big, breaker, "2", null, "150", null);
        addInGroup(small, breaker, "3", "100", "160", null);
        addInGroup(small, cable, "50", "10", "15", "Dağılım Sigortası");
        addInGroup(small, cable, "20", "10", "15", null);

        GroupJobBreakdown breakers = productCatalogService.groupDistribution("dağılım SİGORTASI", null, null);

        // Big: 1.000 cost (one line without a price), 1.800 sale; small: 300 + 500 = 800 cost, 480 + 750 sale
        assertThat(breakers.rows()).extracting(GroupJobRow::jobId).containsExactly(big.getId(), small.getId());
        GroupJobRow smallRow = breakers.rows().get(1);
        assertThat(smallRow.total().purchaseTotal()).isEqualByComparingTo("800.00");
        assertThat(smallRow.total().saleTotal()).isEqualByComparingTo("1230.00");
        assertThat(smallRow.total().lineCount()).isEqualTo(2);
        GroupJobRow bigRow = breakers.rows().get(0);
        assertThat(bigRow.total().purchaseTotal()).isEqualByComparingTo("1000.00");
        assertThat(bigRow.total().purchasePriceMissingCount()).isEqualTo(1);
        assertThat(breakers.total().purchaseTotal()).isEqualByComparingTo("1800.00");
        assertThat(breakers.total().saleTotal()).isEqualByComparingTo("3030.00");
        assertThat(breakers.total().profit()).isEqualByComparingTo("1230.00");
        assertThat(breakers.total().lineCount()).isEqualTo(4);
        assertThat(breakers.jobCount()).isEqualTo(2);
        assertThat(breakers.productIds()).containsExactlyInAnyOrder(breaker.getId(), cable.getId());

        // The cable moved to the breaker group counts there, not under its catalogue category
        GroupJobBreakdown cables = productCatalogService.groupDistribution("Dağılım Kablosu", null, null);
        assertThat(cables.total().lineCount()).isEqualTo(1);
        assertThat(cables.total().purchaseTotal()).isEqualByComparingTo("200.00");
        assertThat(productCatalogService.loadCatalog().groups()).contains("Dağılım Sigortası", "Dağılım Kablosu");
    }

    @Test
    void groupDistributionNarrowsToTheChosenDates() {
        Product lamp = product("Tarih Lamba", "Tarih Aydınlatma");
        Job site = site("Tarih Şantiye");
        MaterialItem september = line(site, lamp, "1", "100", "200");
        september.setItemDate(LocalDate.of(2026, 9, 10));
        materialService.addItem(september);
        MaterialItem october = line(site, lamp, "2", "100", "200");
        october.setItemDate(LocalDate.of(2026, 10, 10));
        materialService.addItem(october);

        GroupJobBreakdown all = productCatalogService.groupDistribution("Tarih Aydınlatma", null, null);
        GroupJobBreakdown octoberOnly = productCatalogService.groupDistribution("Tarih Aydınlatma",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(all.total().saleTotal()).isEqualByComparingTo("600.00");
        assertThat(octoberOnly.total().saleTotal()).isEqualByComparingTo("400.00");
        assertThat(octoberOnly.total().lineCount()).isEqualTo(1);
        assertThat(productCatalogService.groupDistribution("Tarih Aydınlatma", LocalDate.of(2027, 1, 1), null)
                .rows()).isEmpty();
    }

    @Test
    void managerSeesOnlyServicesWithoutCostInTheDistribution() {
        Product fan = product("Yetki Fan", "Yetki Grubu");
        addInGroup(site("Yetki Şantiye"), fan, "1", "100", "200", null);
        Customer customer = customerService.create(new Customer("Yetki Servis Müşteri", null, null, null, null));
        MaterialItem draft = new MaterialItem(null, fan, null, BigDecimal.ONE, new BigDecimal("100"), null,
                new BigDecimal("300"), PriceEntryType.UNIT, null, null, null);
        serviceJobService.save(null, new Job(customer, JobType.SERVICE, "Yetki arıza", null, DAY, null,
                JobStatus.COMPLETED, null, null, false, null), List.of(materialService.prepareDraft(draft)));

        login(adminId + 1, UserRole.MANAGER);
        GroupJobBreakdown breakdown = productCatalogService.groupDistribution("Yetki Grubu", null, null);

        assertThat(breakdown.hasFinancials()).isFalse();
        assertThat(breakdown.rows()).singleElement().satisfies(row -> {
            assertThat(row.jobType()).isEqualTo(JobType.SERVICE);
            assertThat(row.total().purchaseTotal()).isNull();
            assertThat(row.total().profit()).isNull();
        });
        assertThat(breakdown.total().saleTotal()).isEqualByComparingTo("300.00");
    }

    // 13: "Gruba Taşı" for five lines in one go

    @Test
    void fiveLinesMoveToPanoTogether() {
        Job site = site("Grup Şantiye 13");
        Product cable = product("Grup Kablo 13", "Kablo");
        List<Long> ids = java.util.stream.IntStream.range(0, 5)
                .mapToObj(i -> materialService.addItem(line(site, cable, "1", "10", "20")).getId())
                .toList();

        materialService.moveToGroup(ids, "Pano 13");
        materialService.moveToGroup(ids.subList(0, 3), "PANO  13");

        MaterialGroupBreakdown breakdown = breakdown(site);
        assertThat(groupNames(site)).containsExactly("Pano 13");
        assertThat(total(breakdown, "Pano 13").lineCount()).isEqualTo(5);
        assertThat(total(breakdown, "Pano 13").saleTotal()).isEqualByComparingTo("100.00");

        materialService.moveToGroup(ids.subList(0, 2), "  ");
        assertThat(groupNames(site)).containsExactly("Kablo", "Pano 13");
    }

    // 14, 15: Turkish order with "Kategorisiz" last; a site without any group

    @Test
    void groupsAreInTurkishOrderAndUngroupedSitesStillWork() {
        Job site = site("Grup Şantiye 14");
        Product item = product("Grup Malzeme 14", null);
        for (String group : new String[] { "Şalt", "Çatı", null, "Aydınlatma" }) {
            addInGroup(site, item, "1", "1", "2", group);
        }
        assertThat(groupNames(site)).containsExactly("Aydınlatma", "Çatı", "Şalt", null);

        Job plain = site("Grup Şantiye 15");
        addInGroup(plain, item, "1", "1", "2", null);
        addInGroup(plain, item, "3", "1", "2", null);
        assertThat(groupNames(plain)).containsExactly((String) null);
        assertThat(breakdown(plain).total().lineCount()).isEqualTo(2);
        assertThat(breakdown(site("Grup Şantiye boş")).groups()).isEmpty();
    }

    // ---- helpers --------------------------------------------------------------------------------------

    private void login(Long id, UserRole role) {
        sessionService.start(new SessionUser(id, "test", "Test", null, role));
    }

    private Product product(String name, String category) {
        return productService.create(new Product(name, ProductUnit.PIECE, null, category));
    }

    private Job site(String name) {
        Customer customer = customerService.create(new Customer(name + " Müşteri", null, null, null, null));
        return jobService.create(new Job(customer, JobType.SITE, name, null, DAY, null, JobStatus.ACTIVE, null, null,
                false, null));
    }

    private MaterialItem line(Job job, Product product, String quantity, String purchase, String sale) {
        return new MaterialItem(job, product, DAY, new BigDecimal(quantity),
                purchase == null ? null : new BigDecimal(purchase), null, new BigDecimal(sale), PriceEntryType.UNIT,
                null, null, null);
    }

    private void addInGroup(Job job, Product product, String quantity, String purchase, String sale, String group) {
        MaterialItem item = line(job, product, quantity, purchase, sale);
        item.setGroupName(group);
        materialService.addItem(item);
    }

    private MaterialGroupBreakdown breakdown(Job job) {
        return materialService.groupBreakdown(jobSummaryService.loadJobDetail(job.getId()).materials());
    }

    private List<String> groupNames(Job job) {
        return breakdown(job).groups().stream().map(group -> group.groupName()).toList();
    }

    private static MaterialGroupTotal total(MaterialGroupBreakdown breakdown, String groupName) {
        return breakdown.summary().stream()
                .filter(total -> groupName.equals(total.groupName()))
                .findFirst().orElseThrow();
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
}
