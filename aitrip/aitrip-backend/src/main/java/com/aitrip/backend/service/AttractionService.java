package com.aitrip.backend.service;

import com.aitrip.backend.entity.TbAttraction;

import java.util.List;

/**
 * 景区服务接口
 */
public interface AttractionService {

    /**
     * 按城市 + 关键词搜索景区
     */
    List<TbAttraction> searchByCity(String city, String keyword);

    /**
     * 查询景区门票库存余量
     */
    int queryStock(Long attractionId);

    /**
     * 搜索景区（供 MCP Tool 调用，返回 JSON 字符串）
     */
    String searchAttractions(String city, Integer typeId, Integer pageSize);

    /**
     * 查询景区详情（返回 JSON 字符串）
     */
    String getAttractionDetail(Long attractionId);

    /**
     * 查询附近景区（返回 JSON 字符串）
     */
    String getNearbyAttractions(Double longitude, Double latitude, Double radiusKm);
}
