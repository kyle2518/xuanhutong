package com.xuanhutong.rx.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.rx.entity.Herb;

public interface HerbService {
    Page<Herb> searchHerbs(String keyword, String category, int page, int size);
    Herb getHerb(Long id);
    String getHerbTooltip(Long id);
}
