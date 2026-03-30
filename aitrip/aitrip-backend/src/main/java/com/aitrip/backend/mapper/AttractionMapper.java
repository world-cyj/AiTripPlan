package com.aitrip.backend.mapper;

import com.aitrip.backend.entity.TbAttraction;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AttractionMapper extends BaseMapper<TbAttraction> {

    @Select("SELECT id FROM tb_attraction WHERE deleted = 0")
    List<Long> selectAllValidIds();

    @Select("<script>" +
            "SELECT * FROM tb_attraction WHERE deleted = 0" +
            "<if test=\"city != null and city != ''\"> AND city = #{city}</if>" +
            "<if test=\"keyword != null and keyword != ''\">" +
            " AND (name LIKE CONCAT('%',#{keyword},'%') OR description LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            " ORDER BY score DESC" +
            "</script>")
    List<TbAttraction> searchByCity(@Param("city") String city, @Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT * FROM tb_attraction WHERE deleted = 0" +
            "<if test=\"city != null and city != ''\"> AND city = #{city}</if>" +
            "<if test=\"typeId != null and typeId != 0\"> AND type_id = #{typeId}</if>" +
            " ORDER BY score DESC LIMIT #{pageSize}" +
            "</script>")
    List<TbAttraction> searchAttractions(@Param("city") String city,
                                        @Param("typeId") Integer typeId,
                                        @Param("pageSize") Integer pageSize);
}
