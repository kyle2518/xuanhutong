package com.xuanhutong.medical.repository;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanhutong.medical.entity.Patient;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PatientRepository extends BaseMapper<Patient> {}
