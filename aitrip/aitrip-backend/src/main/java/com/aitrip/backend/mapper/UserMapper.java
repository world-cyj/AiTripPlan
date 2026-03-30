package com.aitrip.backend.mapper;

import com.aitrip.backend.entity.TbUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<TbUser> {

    @Select("SELECT * FROM tb_user WHERE phone = #{phone} LIMIT 1")
    TbUser selectByPhone(String phone);
}
