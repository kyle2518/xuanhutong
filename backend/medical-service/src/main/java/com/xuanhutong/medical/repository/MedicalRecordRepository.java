package com.xuanhutong.medical.repository;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanhutong.medical.entity.MedicalRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MedicalRecordRepository extends BaseMapper<MedicalRecord> {}
