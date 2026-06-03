-- V1__init_schema.sql
-- 悬壶通 (XuanHuTong) Database Schema

CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `phone` VARCHAR(20) NOT NULL,
    `password` VARCHAR(255) DEFAULT NULL,
    `name` VARCHAR(50) NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'DOCTOR' COMMENT 'DOCTOR, ADMIN',
    `avatar_url` VARCHAR(500) DEFAULT NULL,
    `signature_image_url` VARCHAR(500) DEFAULT NULL,
    `clinic_name` VARCHAR(100) DEFAULT NULL,
    `status` TINYINT DEFAULT 1 COMMENT '1=active, 0=disabled',
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_users_phone` (`phone`),
    INDEX `idx_users_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS `patients` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '创建者/所属医生',
    `name` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `gender` TINYINT DEFAULT NULL COMMENT '0=male, 1=female',
    `age` INT DEFAULT NULL,
    `birth_date` DATE DEFAULT NULL,
    `address` VARCHAR(255) DEFAULT NULL,
    `id_card` VARCHAR(18) DEFAULT NULL,
    `chief_complaint` TEXT DEFAULT NULL,
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_patients_user_id` (`user_id`),
    INDEX `idx_patients_phone` (`phone`),
    INDEX `idx_patients_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病人信息表';

CREATE TABLE IF NOT EXISTS `medical_records` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `patient_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL COMMENT '就诊医生',
    `visit_date` DATETIME NOT NULL,
    `diagnosis` TEXT DEFAULT NULL COMMENT '中医诊断',
    `symptoms` TEXT DEFAULT NULL,
    `treatment_method` TEXT DEFAULT NULL,
    `notes` TEXT DEFAULT NULL,
    `report_file_url` VARCHAR(500) DEFAULT NULL,
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_records_patient_id` (`patient_id`),
    INDEX `idx_records_user_id` (`user_id`),
    INDEX `idx_records_visit_date` (`visit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='病历记录表';

CREATE TABLE IF NOT EXISTS `prescriptions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `patient_id` BIGINT NOT NULL,
    `medical_record_id` BIGINT DEFAULT NULL,
    `user_id` BIGINT NOT NULL COMMENT '开方医生',
    `diagnosis` TEXT DEFAULT NULL,
    `notes` TEXT DEFAULT NULL COMMENT '用法说明',
    `total_doses` INT DEFAULT 1,
    `pdf_url` VARCHAR(500) DEFAULT NULL,
    `is_signed` TINYINT DEFAULT 0,
    `signed_at` DATETIME DEFAULT NULL,
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_prescriptions_patient_id` (`patient_id`),
    INDEX `idx_prescriptions_user_id` (`user_id`),
    INDEX `idx_prescriptions_medical_record_id` (`medical_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药方表';

CREATE TABLE IF NOT EXISTS `prescription_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `prescription_id` BIGINT NOT NULL,
    `herb_id` BIGINT NOT NULL,
    `herb_name` VARCHAR(100) NOT NULL,
    `dosage_grams` DECIMAL(10,2) NOT NULL COMMENT '克数',
    `notes` VARCHAR(255) DEFAULT NULL COMMENT '特殊用法',
    `sort_order` INT DEFAULT 0,
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_items_prescription_id` (`prescription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药方药材明细表';

CREATE TABLE IF NOT EXISTS `herbs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `pinyin_name` VARCHAR(100) NOT NULL,
    `chinese_name` VARCHAR(50) NOT NULL,
    `latin_name` VARCHAR(100) DEFAULT NULL,
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `properties` VARCHAR(100) DEFAULT NULL COMMENT '性味',
    `meridian_tropism` VARCHAR(100) DEFAULT NULL COMMENT '归经',
    `efficacy` TEXT NOT NULL COMMENT '功效',
    `indications` TEXT DEFAULT NULL COMMENT '主治',
    `dosage_range` VARCHAR(50) DEFAULT NULL COMMENT '常用剂量',
    `contraindications` TEXT DEFAULT NULL COMMENT '禁忌',
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_herbs_pinyin` (`pinyin_name`),
    INDEX `idx_herbs_chinese` (`chinese_name`),
    INDEX `idx_herbs_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='中药材表';

CREATE TABLE IF NOT EXISTS `herb_inventory` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `herb_id` BIGINT NOT NULL,
    `stock_grams` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '库存克数',
    `min_stock_alert` DECIMAL(12,2) DEFAULT NULL COMMENT '预警值',
    `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '单价(元/克)',
    `last_restock_date` DATE DEFAULT NULL,
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_inventory_user_herb` (`user_id`, `herb_id`),
    INDEX `idx_inventory_user_id` (`user_id`),
    INDEX `idx_inventory_herb_id` (`herb_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药材库存表';

CREATE TABLE IF NOT EXISTS `appointments` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `patient_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL COMMENT '预约医生',
    `appointment_time` DATETIME NOT NULL,
    `duration_minutes` INT DEFAULT 30,
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING, CONFIRMED, CANCELLED, COMPLETED',
    `patient_name` VARCHAR(50) DEFAULT NULL COMMENT '预约人姓名',
    `patient_phone` VARCHAR(20) DEFAULT NULL COMMENT '预约人电话',
    `notes` TEXT DEFAULT NULL COMMENT '备注',
    `created_from` VARCHAR(20) DEFAULT 'WECHAT' COMMENT 'WECHAT, SYSTEM, PHONE',
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_appointments_user_id` (`user_id`),
    INDEX `idx_appointments_patient_id` (`patient_id`),
    INDEX `idx_appointments_time` (`appointment_time`),
    INDEX `idx_appointments_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约表';

CREATE TABLE IF NOT EXISTS `classic_prescriptions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL COMMENT '方名',
    `source` VARCHAR(100) NOT NULL COMMENT '出处',
    `category` VARCHAR(100) DEFAULT NULL COMMENT '分类',
    `composition` TEXT NOT NULL COMMENT '组成(JSON)',
    `efficacy` TEXT NOT NULL COMMENT '功效',
    `indications` TEXT DEFAULT NULL COMMENT '主治',
    `usage_method` TEXT DEFAULT NULL COMMENT '用法',
    `source_text` TEXT DEFAULT NULL COMMENT '原文',
    `notes` TEXT DEFAULT NULL COMMENT '备注',
    `sort_order` INT DEFAULT 0,
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_classic_rx_name` (`name`),
    INDEX `idx_classic_rx_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='经典药方表';

CREATE TABLE IF NOT EXISTS `sms_verification_codes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `phone` VARCHAR(20) NOT NULL,
    `code` VARCHAR(10) NOT NULL,
    `expires_at` DATETIME NOT NULL,
    `used` TINYINT DEFAULT 0,
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_sms_phone` (`phone`, `used`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='短信验证码表';
