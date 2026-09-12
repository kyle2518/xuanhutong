package com.xuanhutong.clinic.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.clinic.dto.request.HerbInventoryCreateRequest;
import com.xuanhutong.clinic.dto.request.HerbInventoryUpdateRequest;
import com.xuanhutong.common.dto.response.ApiResponse;
import com.xuanhutong.clinic.service.HerbInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class HerbInventoryController {
    private final HerbInventoryService service;
    @GetMapping
    public ApiResponse<Page<Map<String, Object>>> list(Authentication a, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean low, @RequestParam(defaultValue = "1") int p, @RequestParam(defaultValue = "50") int s) {
        return ApiResponse.success(service.listInventory((Long)a.getPrincipal(), keyword, low, p, s));
    }
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> get(Authentication a, @PathVariable Long id) {
        return ApiResponse.success(service.getInventoryItem(id, (Long)a.getPrincipal()));
    }
    @PostMapping
    public ApiResponse<Map<String, Object>> create(Authentication a, @RequestBody HerbInventoryCreateRequest r) {
        return ApiResponse.success(service.createInventory((Long)a.getPrincipal(), r.getHerbId(), r.getStockGrams(), r.getMinStockAlert(), r.getUnitPrice()));
    }
    @GetMapping("/available-herbs")
    public ApiResponse<Page<Map<String, Object>>> availableHerbs(Authentication a, @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int p, @RequestParam(defaultValue = "20") int s) {
        return ApiResponse.success(service.getAvailableHerbs((Long)a.getPrincipal(), keyword, p, s));
    }
    @PutMapping("/{id}")
    public ApiResponse<Void> update(Authentication a, @PathVariable Long id, @RequestBody HerbInventoryUpdateRequest r) {
        service.updateStock(id, (Long)a.getPrincipal(), r); return ApiResponse.success();
    }
    @PostMapping("/batch-update")
    public ApiResponse<Void> batch(Authentication a, @RequestBody HerbInventoryUpdateRequest r) {
        service.batchUpdate((Long)a.getPrincipal(), r); return ApiResponse.success();
    }
}
