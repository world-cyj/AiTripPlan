package com.aitrip.backend.mapper;

import com.aitrip.backend.entity.TbVoucherOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 优惠券订单 Mapper
 */
@Mapper
public interface VoucherOrderMapper extends BaseMapper<TbVoucherOrder> {

    /**
     * 幂等检查：根据用户ID + 优惠券ID 查询是否已有订单
     */
    @Select("SELECT COUNT(1) FROM tb_voucher_order WHERE user_id = #{userId} AND voucher_id = #{voucherId} AND deleted = 0")
    int countByUserAndVoucher(@Param("userId") Long userId, @Param("voucherId") Long voucherId);
}
