package com.electrician.tracker.ui.util;

import com.electrician.tracker.config.AppInfo;
import org.springframework.stereotype.Component;

/**
 * The developer's signature texts (version, developer, licence notice,
 * copyright), built from {@link AppInfo} and the Turkish message bundle so the
 * menu footer, About dialog and licence screen always say the same thing.
 */
@Component
public class AppSignature {

    private final AppInfo appInfo;

    public AppSignature(AppInfo appInfo) {
        this.appInfo = appInfo;
    }

    /** "Spark ERP 2.8.3 · Geliştirici: Muhammet Akduman" (menu footer, About). */
    public String versionLine() {
        return DialogUtil.message("signature.version", appInfo.name(), appInfo.version(), appInfo.developer());
    }

    /** "Bu program lisanslıdır; izinsiz kopyalanamaz ve üçüncü kişilerle paylaşılamaz." */
    public String noticeLine() {
        return DialogUtil.message("signature.notice");
    }

    /** "© 2026 Muhammet Akduman – Tüm hakları saklıdır." */
    public String copyrightLine() {
        // The year is passed as text: MessageFormat would print a number as "2.026".
        return DialogUtil.message("signature.copyright", String.valueOf(appInfo.copyrightYear()),
                appInfo.developer());
    }

    /** "Spark ERP 2.8.3" for the window title. */
    public String windowTitle() {
        return appInfo.nameAndVersion();
    }

    /** "Spark ERP": login brand band, header, installer. */
    public String appName() {
        return appInfo.name();
    }

    /** The one-sentence description of the program (About dialog). */
    public String description() {
        return appInfo.description();
    }

    /** "Sürüm 2.4.2" for the login screen. */
    public String versionOnly() {
        return DialogUtil.message("signature.versionOnly", appInfo.version());
    }

    public String developer() {
        return appInfo.developer();
    }
}
