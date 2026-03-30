package com.aitrip.backend.mapper;

import com.aitrip.backend.entity.TbOutbox;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OutboxMapper extends BaseMapper<TbOutbox> {

    @Select("SELECT * FROM tb_outbox WHERE status = 0 AND next_retry_time <= NOW() ORDER BY create_time ASC LIMIT #{limit}")
    List<TbOutbox> selectPendingMessages(@Param("limit") int limit);

    @Update("UPDATE tb_outbox SET status = 1, send_time = NOW() WHERE id = #{id}")
    int markDelivered(@Param("id") Long id);

    @Update("UPDATE tb_outbox SET status = 2, last_error = #{lastError} WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("lastError") String lastError);

    @Update("UPDATE tb_outbox SET retry_count = retry_count + 1, " +
            "next_retry_time = DATE_ADD(NOW(), INTERVAL #{delaySeconds} SECOND), " +
            "last_error = #{lastError} WHERE id = #{id}")
    int updateRetry(@Param("id") Long id,
                    @Param("delaySeconds") int delaySeconds,
                    @Param("lastError") String lastError);
}
