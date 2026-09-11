"""ai_diagnoses 表结构（Python 服务启动时幂等自建，不碰 Java Flyway）。"""
from sqlalchemy import text

from app.db.engine import engine

AI_DIAGNOSES_DDL = """
CREATE TABLE IF NOT EXISTS `ai_diagnoses` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '所属医生',
    `patient_id` BIGINT NOT NULL COMMENT '病人',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, SIGNED, REJECTED',
    `chief_complaint` TEXT DEFAULT NULL COMMENT '主诉/输入',
    `draft_content` JSON DEFAULT NULL COMMENT '结构化草稿',
    `evidence` JSON DEFAULT NULL COMMENT '古籍依据/引文',
    `agent_trace` JSON DEFAULT NULL COMMENT 'Agent执行轨迹',
    `compatibility_warnings` JSON DEFAULT NULL COMMENT '配伍禁忌提示',
    `converted_prescription_id` BIGINT DEFAULT NULL COMMENT '转处方后的 prescriptions.id',
    `signed_at` DATETIME DEFAULT NULL,
    `signature_text` VARCHAR(100) DEFAULT NULL,
    `is_deleted` TINYINT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_ai_diag_user_id` (`user_id`),
    INDEX `idx_ai_diag_patient_id` (`patient_id`),
    INDEX `idx_ai_diag_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI辅助诊断表';
"""


def ensure_tables() -> None:
    with engine.begin() as conn:
        conn.execute(text(AI_DIAGNOSES_DDL))
