package com.electrician.tracker.ui.util.navigation;

import com.electrician.tracker.domain.job.JobType;
import com.electrician.tracker.ui.controller.job.ServiceListController;
import com.electrician.tracker.ui.controller.job.SiteListController;
import org.springframework.stereotype.Component;

/**
 * Opens a job from anywhere (usage history, monthly report, dashboard, a
 * quote): a site on the "Şantiyeler" page with its pane expanded, a service
 * on the "Servisler" page with its row selected. A site can also be opened
 * on its materials with one material group chosen.
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

    /** A site with its "Malzemeler" tab open on {@code groupName} ({@code null} = "Kategorisiz"). */
    public void openSiteGroup(Long jobId, String groupName) {
        contentNavigator.<SiteListController>show(ViewPaths.SITES, page -> page.focusJobGroup(jobId, groupName));
    }

    public void showSites() {
        contentNavigator.show(ViewPaths.SITES);
    }

    public void showServices() {
        contentNavigator.show(ViewPaths.SERVICES);
    }
}
