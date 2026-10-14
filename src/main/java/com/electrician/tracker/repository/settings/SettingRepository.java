package com.electrician.tracker.repository.settings;

import com.electrician.tracker.domain.settings.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, String> {
}
