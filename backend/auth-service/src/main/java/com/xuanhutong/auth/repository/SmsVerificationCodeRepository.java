package com.xuanhutong.auth.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanhutong.auth.entity.SmsVerificationCode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SmsVerificationCodeRepository extends BaseMapper<SmsVerificationCode> {
}
