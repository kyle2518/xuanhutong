package com.xuanhutong.rx.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.rx.entity.Herb;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.rx.repository.HerbRepository;
import com.xuanhutong.rx.service.HerbService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service @RequiredArgsConstructor
public class HerbServiceImpl implements HerbService {
    private final HerbRepository herbRepo;

    public Page<Herb> searchHerbs(String keyword, String category, int page, int size) {
        var w = new LambdaQueryWrapper<Herb>();
        if (StringUtils.hasText(keyword)) w.and(q -> q.like(Herb::getChineseName, keyword).or().like(Herb::getPinyinName, keyword));
        if (StringUtils.hasText(category)) w.eq(Herb::getCategory, category);
        w.orderByAsc(Herb::getPinyinName); return herbRepo.selectPage(new Page<>(page, size), w);
    }

    public Herb getHerb(Long id) { Herb h = herbRepo.selectById(id); if (h == null) throw new BusinessException(ErrorCode.HERB_NOT_FOUND); return h; }

    public String getHerbTooltip(Long id) {
        Herb h = getHerb(id); StringBuilder sb = new StringBuilder();
        sb.append(h.getChineseName()).append("（").append(h.getCategory()).append("）\n");
        sb.append("性味：").append(h.getProperties()).append("\n");
        sb.append("归经：").append(h.getMeridianTropism()).append("\n");
        sb.append("功效：").append(h.getEfficacy());
        if (StringUtils.hasText(h.getDosageRange())) sb.append("\n常用剂量：").append(h.getDosageRange());
        return sb.toString();
    }
}
