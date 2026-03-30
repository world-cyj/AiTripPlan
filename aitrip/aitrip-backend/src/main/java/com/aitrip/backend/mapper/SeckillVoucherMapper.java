package com.aitrip.backend.mapper;

import com.aitrip.backend.entity.TbSeckillVoucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 秒杀券 Mapper
 */
@Mapper
public interface SeckillVoucherMapper extends BaseMapper<TbSeckillVoucher> {

    /**
     * CAS 扣减库存（乐观锁，stock > 0 才扣）
     */
    @Update("UPDATE tb_seckill_voucher SET stock = stock - 1, sold = sold + 1 " +
            "WHERE voucher_id = #{voucherId} AND stock > 0")
    int decreaseStock(@Param("voucherId") Long voucherId);
}
