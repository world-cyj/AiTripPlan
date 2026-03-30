package com.aitrip.backend.mapper;

import com.aitrip.backend.entity.TbIdempotent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

@Mapper
public interface IdempotentMapper extends BaseMapper<TbIdempotent> {

    /** 检查是否已成功消费 */
    @Select("SELECT COUNT(1) FROM tb_idempotent WHERE message_id = #{messageId} AND topic = #{topic} AND status = 1")
    int existsSuccessRecord(@Param("messageId") String messageId, @Param("topic") String topic);

    /** INSERT IGNORE 防并发重复插入 */
    @Insert("INSERT IGNORE INTO tb_idempotent(message_id, topic, status, create_time, expire_time) " +
            "VALUES(#{messageId}, #{topic}, #{status}, NOW(), #{expireTime})")
    int insertOrIgnore(TbIdempotent idempotent);

    /** 更新消费状态 */
    @Update("UPDATE tb_idempotent SET status = #{status} WHERE message_id = #{messageId} AND topic = #{topic}")
    int updateStatus(@Param("messageId") String messageId,
                     @Param("topic") String topic,
                     @Param("status") int status);
}
