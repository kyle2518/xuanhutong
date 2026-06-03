package com.xuanhutong.rx.repository;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanhutong.rx.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface PrescriptionRepository extends BaseMapper<Prescription> {}
