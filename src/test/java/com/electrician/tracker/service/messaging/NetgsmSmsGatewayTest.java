package com.electrician.tracker.service.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * The provider answers with a short line rather than a status code, so reading
 * it correctly is the whole of the integration.
 */
class NetgsmSmsGatewayTest {

    @Test
    void anAcceptedMessageCarriesTheProvidersJobId() {
        SmsResult result = NetgsmSmsGateway.interpret("00 1234567");

        assertThat(result.success()).isTrue();
        assertThat(result.detail()).isEqualTo("1234567");
    }

    @Test
    void acceptanceWithoutAJobIdIsStillAcceptance() {
        assertThat(NetgsmSmsGateway.interpret("00").success()).isTrue();
    }

    @Test
    void aPublishedRefusalBecomesASentenceTheUserCanActOn() {
        SmsResult wrongPassword = NetgsmSmsGateway.interpret("30");

        assertThat(wrongPassword.success()).isFalse();
        assertThat(wrongPassword.code()).isEqualTo("error.sms.netgsm.30");
    }

    @Test
    void anUnapprovedSenderTitleIsRecognisedToo() {
        assertThat(NetgsmSmsGateway.interpret("40").code()).isEqualTo("error.sms.netgsm.40");
    }

    @Test
    void anAnswerThisProgramDoesNotKnowIsPassedThroughWhole() {
        // Guessing here would hide an endpoint that changed shape.
        SmsResult result = NetgsmSmsGateway.interpret("<html>Service moved</html>");

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("error.sms.unknown");
        assertThat(result.detail()).isEqualTo("<html>Service moved</html>");
    }

    @Test
    void anEmptyAnswerIsAFailureRatherThanASilentSuccess() {
        assertThat(NetgsmSmsGateway.interpret("").success()).isFalse();
        assertThat(NetgsmSmsGateway.interpret(null).success()).isFalse();
    }

    @Test
    void surroundingWhitespaceDoesNotChangeTheReading() {
        assertThat(NetgsmSmsGateway.interpret("  00 987  ").success()).isTrue();
        assertThat(NetgsmSmsGateway.interpret("\n30\n").code()).isEqualTo("error.sms.netgsm.30");
    }
}
