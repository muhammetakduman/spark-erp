package com.electrician.tracker.service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.domain.DailyJob;
import com.electrician.tracker.domain.DailyJobPriority;
import com.electrician.tracker.domain.DailyJobStatus;
import com.electrician.tracker.domain.Employee;
import com.electrician.tracker.domain.Job;
import com.electrician.tracker.domain.JobStatus;
import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.dto.AttendanceSaveResult;
import com.electrician.tracker.dto.DailyJobCard;
import com.electrician.tracker.dto.DailyJobCompletion;
import com.electrician.tracker.dto.DailyJobDraft;
import com.electrician.tracker.dto.DayPlan;
import com.electrician.tracker.dto.EmployeeBooking;
import com.electrician.tracker.dto.JobOption;
import com.electrician.tracker.dto.SessionUser;
import com.electrician.tracker.dto.WeekPlan;
import com.electrician.tracker.report.DailyPlanPdfGenerator;
import com.electrician.tracker.repository.DailyJobRepository;
import com.electrician.tracker.repository.EmployeeRepository;
import com.electrician.tracker.service.exception.NotFoundException;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "İş Takip / Günlük Program": what is planned for a day, whether the team
 * went, and moving what was not done to another day (the old entry stays as
 * POSTPONED and the new one points back to it). A completed entry that
 * belongs to a site or service can be written to the attendance in the same
 * step. An entry done without a link becomes a new service of its customer,
 * so every visit shows up in the services list. Entries are linked to
 * services only; a site keeps the link it was planned with.
 */
@Service
public class DailyJobService {

    private static final Set<DailyJobStatus> OVERDUE_STATUSES =
            Set.of(DailyJobStatus.PLANNED, DailyJobStatus.NOT_VISITED);
    private static final long NO_ENTRY = -1L;

    private final DailyJobRepository dailyJobRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerService customerService;
    private final JobService jobService;
    private final AttendanceService attendanceService;
    private final CompanyService companyService;
    private final DailyPlanPdfGenerator pdfGenerator;
    private final AccessControl accessControl;
    private final Clock clock;

    public DailyJobService(DailyJobRepository dailyJobRepository, EmployeeRepository employeeRepository,
            CustomerService customerService, JobService jobService, AttendanceService attendanceService,
            CompanyService companyService, DailyPlanPdfGenerator pdfGenerator, AccessControl accessControl,
            Clock clock) {
        this.dailyJobRepository = dailyJobRepository;
        this.employeeRepository = employeeRepository;
        this.customerService = customerService;
        this.jobService = jobService;
        this.attendanceService = attendanceService;
        this.companyService = companyService;
        this.pdfGenerator = pdfGenerator;
        this.accessControl = accessControl;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    @Transactional(readOnly = true)
    public DayPlan dayPlan(LocalDate date) {
        return DailyPlanCalculator.dayPlan(date, dailyJobRepository.findPlanBetween(date, date),
                dailyJobRepository.findSourceLinks());
    }

    @Transactional(readOnly = true)
    public WeekPlan weekPlan(LocalDate anyDay) {
        LocalDate monday = DailyPlanCalculator.weekStart(anyDay);
        return DailyPlanCalculator.weekPlan(monday, dailyJobRepository.findPlanBetween(monday, monday.plusDays(6)),
                dailyJobRepository.findSourceLinks(), employeeRepository.findByActiveTrue());
    }

    /** Entries before today still planned (forgotten) or not visited, oldest first. */
    @Transactional(readOnly = true)
    public List<DailyJobCard> pending() {
        List<DailyJobCard> cards = new ArrayList<>(DailyPlanCalculator.cards(
                dailyJobRepository.findOverdue(today(), OVERDUE_STATUSES), dailyJobRepository.findSourceLinks()));
        cards.sort(Comparator.comparing(DailyJobCard::date));
        return cards;
    }

    /** Number shown on the "İş Takip" menu badge. */
    @Transactional(readOnly = true)
    public long pendingCount() {
        return dailyJobRepository.countOverdue(today(), OVERDUE_STATUSES);
    }

    @Transactional(readOnly = true)
    public DailyJobDraft findDraft(Long id) {
        return DailyJobMapper.toDraft(findEntity(id));
    }

    /** Active services a daily job can be linked to. */
    @Transactional(readOnly = true)
    public List<JobOption> linkableJobs() {
        return jobService.findActiveAccessible().stream()
                .filter(job -> job.getType() == JobType.SERVICE)
                .map(DailyJobMapper::toOption)
                .sorted(Comparator.comparing(JobOption::label, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** The option of one job, for a site planned from its own screen or an entry already linked to it. */
    @Transactional(readOnly = true)
    public JobOption jobOption(Long jobId) {
        return DailyJobMapper.toOption(jobService.findById(jobId));
    }

    /**
     * Employees of the draft who already go to another planned entry that
     * day, with how many; the form asks before saving.
     */
    @Transactional(readOnly = true)
    public List<EmployeeBooking> findOtherBookings(Long id, DailyJobDraft draft) {
        if (draft.date() == null || draft.employeeIds() == null || draft.employeeIds().isEmpty()) {
            return List.of();
        }
        List<DailyJob> others = dailyJobRepository.findBookings(draft.date(), DailyJobStatus.PLANNED,
                draft.employeeIds(), id == null ? NO_ENTRY : id);
        Map<Long, Integer> counts = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        others.forEach(other -> other.getEmployees().forEach(employee -> {
            counts.merge(employee.getId(), 1, Integer::sum);
            names.put(employee.getId(), employee.getName());
        }));
        return counts.entrySet().stream()
                .map(entry -> new EmployeeBooking(names.get(entry.getKey()), draft.date(), entry.getValue()))
                .sorted(Comparator.comparing(EmployeeBooking::employeeName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Creates ({@code id == null}) or changes a daily job. */
    @Transactional
    public DailyJobCard save(Long id, DailyJobDraft draft) {
        DailyJobRules.validate(draft);
        DailyJob entry = id == null ? new DailyJob(draft.date(), draft.title().trim(), currentUserId(), now())
                : findEntity(id);
        entry.setJob(resolveJob(draft.jobId(), entry.getJob()));
        entry.setJobDate(draft.date());
        entry.setTitle(draft.title().trim());
        applyCustomer(entry, draft);
        entry.setAddress(blankToNull(draft.address()));
        entry.setPhone(blankToNull(draft.phone()));
        entry.setPriority(draft.priority() == null ? DailyJobPriority.NORMAL : draft.priority());
        entry.setTimeOfDay(DailyJobRules.normalizeTime(draft.timeOfDay()));
        entry.setNote(blankToNull(draft.note()));
        entry.replaceEmployees(employeeRepository.findAllById(
                draft.employeeIds() == null ? List.of() : draft.employeeIds()));
        return DailyJobMapper.toCard(dailyJobRepository.save(entry), 0);
    }

    /**
     * "Gidildi": done, with an optional note. An entry without a link that
     * has a customer becomes a new service (fees left empty, a typed customer
     * name is saved as a customer). For an entry of a site or service,
     * {@code writeAttendance} also records one full day for each employee who
     * went (at their default wage; days already recorded are skipped).
     */
    @Transactional
    public DailyJobCompletion complete(Long id, String note, boolean writeAttendance) {
        DailyJob entry = findOpenEntity(id);
        entry.complete(blankToNull(note));
        boolean serviceCreated = entry.getJob() == null && openServiceFor(entry);
        if (!writeAttendance || entry.getJob() == null || entry.getEmployees().isEmpty()) {
            return new DailyJobCompletion(new AttendanceSaveResult(0, 0), serviceCreated);
        }
        return new DailyJobCompletion(attendanceService.saveFullDays(entry.getJob().getId(), entry.getJobDate(),
                entry.getEmployees().stream().map(Employee::getId).toList()), serviceCreated);
    }

    /** Links the entry to a new completed service of its customer; false when it has no customer. */
    private boolean openServiceFor(DailyJob entry) {
        Customer customer = entry.getCustomer();
        if (customer == null && entry.getCustomerName() == null) {
            return false;
        }
        if (customer == null) {
            customer = customerService.findOrCreate(entry.getCustomerName(), entry.getPhone(), entry.getAddress(),
                    null).customer();
            entry.setCustomer(customer);
            entry.setCustomerName(null);
        }
        entry.setJob(jobService.create(new Job(customer, JobType.SERVICE, entry.getTitle(), entry.getAddress(),
                entry.getJobDate(), null, JobStatus.COMPLETED, BigDecimal.ZERO, BigDecimal.ZERO, false,
                entry.getNote())));
        return true;
    }

    /** Adds or changes the note of an entry that was already marked as done. */
    @Transactional
    public void updateCompletionNote(Long id, String note) {
        DailyJob entry = findEntity(id);
        if (entry.getStatus() != DailyJobStatus.COMPLETED) {
            throw new ValidationException("error.dailyJob.notCompleted");
        }
        entry.complete(blankToNull(note));
    }

    /**
     * "Gidilmedi": with {@code moveToNextDay} the entry becomes POSTPONED and
     * the same job is planned for the next day; otherwise it stays NOT_VISITED.
     */
    @Transactional
    public void markNotVisited(Long id, String reason, boolean moveToNextDay) {
        DailyJob entry = findOpenEntity(id);
        if (moveToNextDay) {
            postpone(entry, entry.getJobDate().plusDays(1), blankToNull(reason));
        } else {
            entry.markNotVisited(blankToNull(reason));
        }
    }

    /** "Bugüne aktar" for a forgotten or not visited entry of an earlier day. */
    @Transactional
    public void moveToToday(Long id) {
        DailyJob entry = findEntity(id);
        if (!OVERDUE_STATUSES.contains(entry.getStatus())) {
            throw new ValidationException("error.dailyJob.notOpen");
        }
        postpone(entry, today(), entry.getCompletionNote());
    }

    @Transactional
    public void cancel(Long id) {
        findOpenEntity(id).cancel();
    }

    @Transactional
    public void delete(Long id) {
        dailyJobRepository.delete(findEntity(id));
    }

    /** The printable "Günlük Program" of a day. */
    @Transactional(readOnly = true)
    public void exportDayPdf(LocalDate date, Path outputFile) {
        pdfGenerator.generate(dayPlan(date), companyService.get(), outputFile);
    }

    private void postpone(DailyJob entry, LocalDate date, String reason) {
        entry.postponeTo(date, reason);
        dailyJobRepository.save(entry.copyFor(date, currentUserId(), now()));
    }

    /** A changed link must be a job the user may open; an unchanged one is kept as it is. */
    private Job resolveJob(Long jobId, Job current) {
        if (jobId == null) {
            return null;
        }
        if (current != null && jobId.equals(current.getId())) {
            return current;
        }
        return jobService.findById(jobId);
    }

    /** A listed customer, else the typed name matched against the list, else the typed name alone. */
    private void applyCustomer(DailyJob entry, DailyJobDraft draft) {
        Customer customer = draft.customerId() != null ? customerService.findById(draft.customerId())
                : customerService.findByName(draft.customerName()).orElse(null);
        entry.setCustomer(customer);
        entry.setCustomerName(customer == null ? blankToNull(draft.customerName()) : null);
    }

    private DailyJob findOpenEntity(Long id) {
        DailyJob entry = findEntity(id);
        if (!entry.isPlanned()) {
            throw new ValidationException("error.dailyJob.notOpen");
        }
        return entry;
    }

    private DailyJob findEntity(Long id) {
        return dailyJobRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("error.dailyJob.notFound"));
    }

    private Long currentUserId() {
        return accessControl.currentUser().map(SessionUser::id).orElse(null);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
