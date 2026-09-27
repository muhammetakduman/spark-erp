package com.electrician.tracker.service;

import java.util.Optional;

import com.electrician.tracker.domain.Setting;
import com.electrician.tracker.repository.SettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingService {

    private final SettingRepository settingRepository;

    public SettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional(readOnly = true)
    public Optional<String> getValue(String key) {
        return settingRepository.findById(key).map(Setting::getValue);
    }

    @Transactional
    public void setValue(String key, String value) {
        settingRepository.findById(key)
                .ifPresentOrElse(
                        setting -> setting.setValue(value),
                        () -> settingRepository.save(new Setting(key, value)));
    }
}
