-- V2__add_signature_text.sql
-- 此前 Prescription 实体引用了 signatureText 字段，但 prescriptions 表缺少该列，
-- 导致查询/更新（预览 PDF、签署）报 Unknown column 'signature_text'。此处补上。

ALTER TABLE `prescriptions`
    ADD COLUMN `signature_text` VARCHAR(100) DEFAULT NULL COMMENT '签名文本' AFTER `signed_at`;
