package com.electrician.tracker.service.messaging;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Sends through Netgsm, the provider most Turkish small businesses already
 * have an account with.
 *
 * <p>The request is the plain HTTP form of their send service; the answer is a
 * short text, "00 &lt;jobid&gt;" when the message was accepted and a bare
 * two-digit code otherwise. The codes below are Netgsm's published ones; any
 * other answer is handed back whole rather than guessed at, so an endpoint
 * that changes shape shows itself instead of failing quietly.
 *
 * <p>The exact path lives in settings next to the credentials, so it can be
 * corrected without rebuilding. Settings also offers a test message, which is
 * the quickest way to confirm the whole chain against a real account.
 */
@Service
public class NetgsmSmsGateway implements SmsGateway {

    private static final String SEND_PATH = "/sms/send/get/";
    private static final String SUCCESS_CODE = "00";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    /** Netgsm's published refusals; anything else is reported as it arrived. */
    private static final Map<String, String> KNOWN_CODES = Map.of(
            "20", "error.sms.netgsm.20",
            "30", "error.sms.netgsm.30",
            "40", "error.sms.netgsm.40",
            "50", "error.sms.netgsm.50",
            "51", "error.sms.netgsm.51",
            "60", "error.sms.netgsm.60",
            "70", "error.sms.netgsm.70",
            "80", "error.sms.netgsm.80",
            "85", "error.sms.netgsm.85");

    private final SmsSettings settings;
    private final HttpClient httpClient;

    @Autowired
    public NetgsmSmsGateway(SmsSettings settings) {
        this(settings, HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    /** For tests, which supply a client that answers without a network. */
    NetgsmSmsGateway(SmsSettings settings, HttpClient httpClient) {
        this.settings = settings;
        this.httpClient = httpClient;
    }

    @Override
    public boolean isConfigured() {
        return settings.isConfigured();
    }

    @Override
    public SmsResult send(String phone, String message) {
        if (!isConfigured()) {
            return SmsResult.failed("error.sms.notConfigured", null);
        }
        try {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(sendUrl(phone, message))).timeout(TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return interpret(response.body());
        } catch (IOException e) {
            return SmsResult.failed("error.sms.network", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SmsResult.failed("error.sms.network", e.getMessage());
        } catch (RuntimeException e) {
            return SmsResult.failed("error.sms.network", e.getMessage());
        }
    }

    private String sendUrl(String phone, String message) {
        return settings.baseUrl() + SEND_PATH
                + "?usercode=" + encode(settings.username())
                + "&password=" + encode(settings.password())
                + "&gsmno=" + encode(phone)
                + "&message=" + encode(message)
                + "&msgheader=" + encode(settings.header())
                + "&dil=TR";
    }

    /**
     * "00 1234567" means accepted and carries the provider's job id; a known
     * two-digit code becomes a sentence the user can act on; anything else is
     * passed through untouched.
     */
    static SmsResult interpret(String body) {
        String answer = body == null ? "" : body.trim();
        String code = answer.split("\\s+")[0];
        if (SUCCESS_CODE.equals(code)) {
            return SmsResult.sent(answer.length() > code.length() ? answer.substring(code.length()).trim() : "");
        }
        String known = KNOWN_CODES.get(code);
        return known != null ? SmsResult.failed(known, null) : SmsResult.failed("error.sms.unknown", answer);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
