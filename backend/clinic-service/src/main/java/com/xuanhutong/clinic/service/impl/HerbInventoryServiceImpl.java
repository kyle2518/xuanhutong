package com.xuanhutong.clinic.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.clinic.dto.request.HerbInventoryUpdateRequest;
import com.xuanhutong.clinic.entity.Herb;
import com.xuanhutong.clinic.entity.HerbInventory;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.clinic.repository.HerbInventoryRepository;
import com.xuanhutong.clinic.repository.HerbRepository;
import com.xuanhutong.clinic.service.HerbInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor
public class HerbInventoryServiceImpl implements HerbInventoryService {
    private final HerbInventoryRepository invRepo; private final HerbRepository herbRepo;

    public Page<Map<String, Object>> listInventory(Long userId, String keyword, Boolean lowStock, int page, int size) {
        var w = new LambdaQueryWrapper<HerbInventory>().eq(HerbInventory::getUserId, userId);
        var pr = invRepo.selectPage(new Page<>(page, size), w);
        Page<Map<String, Object>> rp = new Page<>(page, size, pr.getTotal());
        rp.setRecords(pr.getRecords().stream().map(inv -> {
            Map<String, Object> m = new HashMap<>(); Herb h = herbRepo.selectById(inv.getHerbId());
            m.put("id", inv.getId()); m.put("herbId", inv.getHerbId()); m.put("herbName", h != null ? h.getChineseName() : "?");
            m.put("pinyinName", h != null ? h.getPinyinName() : ""); m.put("category", h != null ? h.getCategory() : "");
            m.put("efficacy", h != null ? h.getEfficacy() : ""); m.put("stockGrams", inv.getStockGrams());
            m.put("minStockAlert", inv.getMinStockAlert()); m.put("unitPrice", inv.getUnitPrice());
            m.put("lastRestockDate", inv.getLastRestockDate());
            m.put("isLowStock", inv.getMinStockAlert() != null && inv.getStockGrams().compareTo(inv.getMinStockAlert()) <= 0);
            return m;
        }).toList()); return rp;
    }

    public Map<String, Object> getInventoryItem(Long id, Long userId) {
        var inv = invRepo.selectOne(new LambdaQueryWrapper<HerbInventory>().eq(HerbInventory::getId, id).eq(HerbInventory::getUserId, userId));
        if (inv == null) throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND);
        Herb h = herbRepo.selectById(inv.getHerbId());
        Map<String, Object> m = new HashMap<>(); m.put("id", inv.getId()); m.put("herbId", inv.getHerbId());
        m.put("herbName", h != null ? h.getChineseName() : ""); m.put("stockGrams", inv.getStockGrams());
        m.put("minStockAlert", inv.getMinStockAlert()); m.put("unitPrice", inv.getUnitPrice());
        m.put("lastRestockDate", inv.getLastRestockDate()); return m;
    }

    public Map<String, Object> createInventory(Long userId, Long herbId) {
        var exist = invRepo.selectOne(new LambdaQueryWrapper<HerbInventory>().eq(HerbInventory::getUserId, userId).eq(HerbInventory::getHerbId, herbId));
        if (exist != null) return getInventoryItem(exist.getId(), userId);
        Herb herb = herbRepo.selectById(herbId); if (herb == null) throw new BusinessException(ErrorCode.HERB_NOT_FOUND);
        HerbInventory inv = new HerbInventory(); inv.setUserId(userId); inv.setHerbId(herbId);
        inv.setStockGrams(BigDecimal.ZERO); inv.setLastRestockDate(LocalDate.now()); invRepo.insert(inv);
        return getInventoryItem(inv.getId(), userId);
    }

    public Page<Map<String, Object>> getAvailableHerbs(Long userId, String keyword, int page, int size) {
        var ids = invRepo.selectList(new LambdaQueryWrapper<HerbInventory>().eq(HerbInventory::getUserId, userId)).stream().map(HerbInventory::getHerbId).toList();
        var w = new LambdaQueryWrapper<Herb>();
        if (StringUtils.hasText(keyword)) w.and(q -> q.like(Herb::getChineseName, keyword).or().like(Herb::getPinyinName, keyword));
        if (!ids.isEmpty()) w.notIn(Herb::getId, ids);
        w.orderByAsc(Herb::getPinyinName);
        var hp = herbRepo.selectPage(new Page<>(page, size), w);
        Page<Map<String, Object>> rp = new Page<>(page, size, hp.getTotal());
        rp.setRecords(hp.getRecords().stream().map(h -> {
            Map<String, Object> m = new HashMap<>(); m.put("herbId", h.getId()); m.put("herbName", h.getChineseName());
            m.put("pinyinName", h.getPinyinName()); m.put("category", h.getCategory()); m.put("efficacy", h.getEfficacy());
            return m;
        }).toList()); return rp;
    }

    public void updateStock(Long id, Long userId, HerbInventoryUpdateRequest r) {
        var inv = invRepo.selectOne(new LambdaQueryWrapper<HerbInventory>().eq(HerbInventory::getId, id).eq(HerbInventory::getUserId, userId));
        if (inv == null) throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND);
        if (r.getStockGrams() != null) inv.setStockGrams(r.getStockGrams());
        if (r.getMinStockAlert() != null) inv.setMinStockAlert(r.getMinStockAlert());
        if (r.getUnitPrice() != null) inv.setUnitPrice(r.getUnitPrice());
        inv.setLastRestockDate(LocalDate.now()); invRepo.updateById(inv);
    }

    @Transactional
    public void batchUpdate(Long userId, HerbInventoryUpdateRequest r) {
        if (r.getItems() == null || r.getItems().isEmpty()) return;
        for (var item : r.getItems()) {
            var inv = invRepo.selectOne(new LambdaQueryWrapper<HerbInventory>().eq(HerbInventory::getUserId, userId).eq(HerbInventory::getHerbId, item.getHerbId()));
            if (inv != null) { inv.setStockGrams(item.getStockGrams()); inv.setLastRestockDate(LocalDate.now()); invRepo.updateById(inv); }
        }
    }
}
