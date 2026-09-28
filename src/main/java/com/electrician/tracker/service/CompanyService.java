package com.electrician.tracker.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

import com.electrician.tracker.domain.Company;
import com.electrician.tracker.repository.CompanyRepository;
import com.electrician.tracker.service.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The user's own company details (Ayarlar → Firma bilgileri), printed as the
 * letterhead of quote and job PDFs, with the brand colour of their headings.
 * Everybody may read them; only an ADMIN changes them.
 */
@Service
public class CompanyService {

    /** Dark blue used when no brand colour was chosen. */
    public static final String DEFAULT_BRAND_COLOR = "#1F4E79";
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9A-Fa-f]{6}");

    private final CompanyRepository companyRepository;
    private final AccessControl accessControl;
    private final LogoProcessor logoProcessor;

    public CompanyService(CompanyRepository companyRepository, AccessControl accessControl,
            LogoProcessor logoProcessor) {
        this.companyRepository = companyRepository;
        this.accessControl = accessControl;
        this.logoProcessor = logoProcessor;
    }

    /** The saved company, or an empty one before anything was entered. */
    @Transactional(readOnly = true)
    public Company get() {
        return companyRepository.findById(Company.SINGLE_ROW_ID).orElseGet(Company::empty);
    }

    @Transactional
    public Company save(Company changes) {
        accessControl.requireAdmin();
        Company company = companyRepository.findById(Company.SINGLE_ROW_ID).orElseGet(Company::empty);
        company.setName(blankToNull(changes.getName()));
        company.setSlogan(blankToNull(changes.getSlogan()));
        company.setAddress(blankToNull(changes.getAddress()));
        company.setPhone(blankToNull(changes.getPhone()));
        company.setEmail(blankToNull(changes.getEmail()));
        company.setWeb(blankToNull(changes.getWeb()));
        company.setTaxOffice(blankToNull(changes.getTaxOffice()));
        company.setTaxNo(blankToNull(changes.getTaxNo()));
        company.setLogo(changes.hasLogo() ? changes.getLogo() : null);
        company.setBrandColor(normalizeColor(changes.getBrandColor()));
        return companyRepository.save(company);
    }

    /** Whether a logo file of this kind can be used (PNG, JPG, SVG). */
    public boolean isAcceptedLogo(String fileName) {
        return logoProcessor.isAccepted(fileName);
    }

    /** Reads a logo file and returns the small PNG that will be stored (longest edge ≤ 400 px). */
    public byte[] prepareLogo(Path file) {
        accessControl.requireAdmin();
        if (!logoProcessor.isAccepted(file.getFileName().toString())) {
            throw new ValidationException("error.company.logo.type");
        }
        try {
            if (Files.size(file) > LogoProcessor.MAX_UPLOAD_BYTES) {
                throw new ValidationException("error.company.logo.tooLarge");
            }
            return logoProcessor.prepare(Files.readAllBytes(file), file.getFileName().toString());
        } catch (IOException e) {
            throw new ValidationException("error.company.logo.unreadable");
        }
    }

    /** The company's colour, or the default dark blue. */
    public static String brandColorOf(Company company) {
        return company == null || company.getBrandColor() == null ? DEFAULT_BRAND_COLOR : company.getBrandColor();
    }

    private static String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        String trimmed = color.trim();
        if (!HEX_COLOR.matcher(trimmed).matches()) {
            throw new ValidationException("error.company.color.invalid");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
