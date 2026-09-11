package com.xuanhutong.rx.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.rx.dto.request.HerbRequest;
import com.xuanhutong.rx.entity.Herb;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.rx.repository.HerbRepository;
import com.xuanhutong.rx.service.HerbService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

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

    public Herb createHerb(HerbRequest r) {
        if (herbRepo.selectCount(new LambdaQueryWrapper<Herb>().eq(Herb::getChineseName, r.getChineseName())) > 0) {
            throw new BusinessException(400, "药材「" + r.getChineseName() + "」已存在");
        }
        Herb h = new Herb();
        applyRequest(h, r);
        herbRepo.insert(h);
        return h;
    }

    public Herb updateHerb(Long id, HerbRequest r) {
        Herb h = getHerb(id);
        applyRequest(h, r);
        herbRepo.updateById(h);
        return h;
    }

    public void deleteHerb(Long id) {
        getHerb(id);
        herbRepo.deleteById(id); // 逻辑删除（@TableLogic）
    }

    public int batchCreateHerbs(List<HerbRequest> requests) {
        int n = 0;
        for (var r : requests) {
            if (!StringUtils.hasText(r.getChineseName()) || !StringUtils.hasText(r.getPinyinName())) continue;
            if (herbRepo.selectCount(new LambdaQueryWrapper<Herb>().eq(Herb::getChineseName, r.getChineseName())) > 0) continue; // 跳过已存在
            Herb h = new Herb();
            applyRequest(h, r);
            herbRepo.insert(h);
            n++;
        }
        return n;
    }

    private void applyRequest(Herb h, HerbRequest r) {
        h.setPinyinName(r.getPinyinName());
        h.setChineseName(r.getChineseName());
        h.setLatinName(r.getLatinName());
        h.setCategory(r.getCategory());
        h.setProperties(r.getProperties());
        h.setMeridianTropism(r.getMeridianTropism());
        h.setEfficacy(r.getEfficacy());
        h.setIndications(r.getIndications());
        h.setDosageRange(r.getDosageRange());
        h.setContraindications(r.getContraindications());
    }
}
