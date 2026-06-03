package com.xuanhutong.auth.repository;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuanhutong.common.entity.User;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface UserRepository extends BaseMapper<User> {}
