package c.y.queuing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import c.y.queuing.entity.SystemSetting;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
    Optional<SystemSetting> findBySettingKey(String settingKey);
}