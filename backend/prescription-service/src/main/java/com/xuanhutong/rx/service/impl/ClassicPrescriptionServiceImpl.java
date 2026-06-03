package com.xuanhutong.rx.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.rx.dto.request.ClassicPrescriptionRequest;
import com.xuanhutong.rx.entity.ClassicPrescription;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.rx.repository.ClassicPrescriptionRepository;
import com.xuanhutong.rx.service.ClassicPrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service @RequiredArgsConstructor
public class ClassicPrescriptionServiceImpl implements ClassicPrescriptionService {
    private final ClassicPrescriptionRepository repo;

    public Page<ClassicPrescription> listPrescriptions(String keyword, String source, int page, int size) {
        var w = new LambdaQueryWrapper<ClassicPrescription>();
        if (StringUtils.hasText(keyword)) w.and(q -> q.like(ClassicPrescription::getName, keyword).or().like(ClassicPrescription::getEfficacy, keyword));
        if (StringUtils.hasText(source)) w.like(ClassicPrescription::getSource, source);
        w.orderByAsc(ClassicPrescription::getSortOrder); return repo.selectPage(new Page<>(page, size), w);
    }

    public ClassicPrescription getPrescription(Long id) {
        var cp = repo.selectById(id); if (cp == null) throw new BusinessException(ErrorCode.NOT_FOUND); return cp;
    }

    public ClassicPrescription createPrescription(ClassicPrescriptionRequest r) {
        var cp = new ClassicPrescription(); cp.setName(r.getName()); cp.setSource(r.getSource());
        cp.setCategory(r.getCategory()); cp.setComposition(r.getComposition()); cp.setEfficacy(r.getEfficacy());
        cp.setIndications(r.getIndications()); cp.setUsageMethod(r.getUsageMethod());
        cp.setSourceText(r.getSourceText()); cp.setNotes(r.getNotes()); repo.insert(cp); return cp;
    }

    public ClassicPrescription updatePrescription(Long id, ClassicPrescriptionRequest r) {
        var cp = getPrescription(id); cp.setName(r.getName()); cp.setSource(r.getSource());
        cp.setCategory(r.getCategory()); cp.setComposition(r.getComposition()); cp.setEfficacy(r.getEfficacy());
        cp.setIndications(r.getIndications()); cp.setUsageMethod(r.getUsageMethod());
        cp.setSourceText(r.getSourceText()); cp.setNotes(r.getNotes()); repo.updateById(cp); return cp;
    }

    public void deletePrescription(Long id) { getPrescription(id); repo.deleteById(id); }
}
