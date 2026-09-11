package com.xuanhutong.rx.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.rx.dto.request.HerbRequest;
import com.xuanhutong.rx.entity.Herb;
import java.util.List;

public interface HerbService {
    Page<Herb> searchHerbs(String keyword, String category, int page, int size);
    Herb getHerb(Long id);
    String getHerbTooltip(Long id);
    Herb createHerb(HerbRequest request);
    Herb updateHerb(Long id, HerbRequest request);
    void deleteHerb(Long id);
    int batchCreateHerbs(List<HerbRequest> requests);
}
