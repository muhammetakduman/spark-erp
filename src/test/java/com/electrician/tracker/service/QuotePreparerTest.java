package com.electrician.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.electrician.tracker.domain.UserRole;
import com.electrician.tracker.dto.SessionUser;
import org.junit.jupiter.api.Test;

class QuotePreparerTest {

    private static final SessionUser USER = new SessionUser(1L, "ali", "Ali Yılmaz", "Elektrik Teknikeri",
            UserRole.ADMIN);

    @Test
    void typedTextsWinAndAreTrimmed() {
        QuotePreparer preparer = QuotePreparer.resolve("  Veli Demir ", " Saha Sorumlusu ", USER);

        assertThat(preparer.name()).isEqualTo("Veli Demir");
        assertThat(preparer.title()).isEqualTo("Saha Sorumlusu");
    }

    @Test
    void blankFieldsTakeTheLoggedInUser() {
        QuotePreparer preparer = QuotePreparer.resolve(" ", null, USER);

        assertThat(preparer.name()).isEqualTo("Ali Yılmaz");
        assertThat(preparer.title()).isEqualTo("Elektrik Teknikeri");
    }

    @Test
    void withoutUserOrWithBlankUserTheFieldsStayEmpty() {
        assertThat(QuotePreparer.resolve(null, null, null)).isEqualTo(new QuotePreparer(null, null));
        SessionUser nameless = new SessionUser(2L, "x", " ", null, UserRole.MANAGER);
        assertThat(QuotePreparer.resolve("", "", nameless)).isEqualTo(new QuotePreparer(null, null));
    }
}
