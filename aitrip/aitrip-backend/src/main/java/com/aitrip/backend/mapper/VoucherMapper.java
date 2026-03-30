package com.aitrip.backend.mapper;

import com.aitrip.backend.entity.TbVoucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 优惠券 Mapper
 */
@Mapper
public interface VoucherMapper extends BaseMapper<TbVoucher> {

    /**
     * 查询优惠券及其秒杀扩展信息
     */
    @Select("SELECT v.*, sv.stock, sv.begin_time, sv.end_time " +
            "FROM tb_voucher v LEFT JOIN tb_seckill_voucher sv ON v.id = sv.voucher_id " +
            "WHERE v.id = #{id}")
    TbVoucher selectWithSeckill(Long id);
}
