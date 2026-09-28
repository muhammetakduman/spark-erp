package com.electrician.tracker.ui.util;

import com.electrician.tracker.domain.JobType;
import com.electrician.tracker.ui.controller.ServiceListController;
import com.electrician.tracker.ui.controller.SiteListController;
import org.springframework.stereotype.Component;

/**
 * Opens a job from anywhere (usage history, monthly report, dashboard, a
 * quote): a site on the "Şantiyeler" page with its pane expanded, a service
 * on the "Servisler" page with its row selected.
 */
@Component
public class JobNavigator {

    private final ContentNavigator contentNavigator;

    public JobNavigator(ContentNavigator contentNavigator) {
        this.contentNavigator = contentNavigator;
    }

    public void open(Long jobId, JobType type) {
        if (type == JobType.SITE) {
            contentNavigator.<SiteListController>show(ViewPaths.SITES, page -> page.focusJob(jobId));
        } else {
            contentNavigator.<ServiceListController>show(ViewPaths.SERVICES, page -> page.focusJob(jobId));
        }
    }

    public void showSites() {
        contentNavigator.show(ViewPaths.SITES);
    }

    public void showServices() {
        contentNavigator.show(ViewPaths.SERVICES);
    }
}
