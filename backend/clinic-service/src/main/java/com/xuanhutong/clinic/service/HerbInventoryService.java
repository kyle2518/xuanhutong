package com.xuanhutong.clinic.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.clinic.dto.request.HerbInventoryUpdateRequest;
import java.math.BigDecimal;
import java.util.Map;

public interface HerbInventoryService {
    Page<Map<String, Object>> listInventory(Long userId, String keyword, Boolean lowStock, int page, int size);
    Map<String, Object> getInventoryItem(Long id, Long userId);
    Map<String, Object> createInventory(Long userId, Long herbId, BigDecimal stockGrams, BigDecimal minStockAlert, BigDecimal unitPrice);
    Page<Map<String, Object>> getAvailableHerbs(Long userId, String keyword, int page, int size);
    void updateStock(Long id, Long userId, HerbInventoryUpdateRequest request);
    void batchUpdate(Long userId, HerbInventoryUpdateRequest request);
}
