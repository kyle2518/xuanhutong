package com.xuanhutong.medical.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.medical.dto.request.PatientCreateRequest;
import com.xuanhutong.medical.dto.request.PatientUpdateRequest;
import com.xuanhutong.medical.entity.Patient;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.medical.repository.PatientRepository;
import com.xuanhutong.medical.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {
    private final PatientRepository patientRepository;

    public Page<Patient> listPatients(Long userId, int page, int size, String keyword) {
        var w = new LambdaQueryWrapper<Patient>().eq(Patient::getUserId, userId);
        if (StringUtils.hasText(keyword)) w.and(q -> q.like(Patient::getName, keyword).or().like(Patient::getPhone, keyword));
        w.orderByDesc(Patient::getCreatedAt);
        return patientRepository.selectPage(new Page<>(page, size), w);
    }

    public Patient getPatient(Long id, Long userId) {
        var p = patientRepository.selectOne(new LambdaQueryWrapper<Patient>().eq(Patient::getId, id).eq(Patient::getUserId, userId));
        if (p == null) throw new BusinessException(ErrorCode.PATIENT_NOT_FOUND);
        return p;
    }

    public Patient createPatient(Long userId, PatientCreateRequest req) {
        Patient p = new Patient(); p.setUserId(userId); p.setName(req.getName());
        p.setPhone(req.getPhone()); p.setGender(req.getGender()); p.setAge(req.getAge());
        p.setBirthDate(req.getBirthDate()); p.setAddress(req.getAddress());
        p.setIdCard(req.getIdCard()); p.setChiefComplaint(req.getChiefComplaint());
        patientRepository.insert(p); return p;
    }

    public Patient updatePatient(Long id, Long userId, PatientUpdateRequest req) {
        Patient p = getPatient(id, userId);
        if (req.getName() != null) p.setName(req.getName());
        if (req.getPhone() != null) p.setPhone(req.getPhone());
        if (req.getGender() != null) p.setGender(req.getGender());
        if (req.getAge() != null) p.setAge(req.getAge());
        if (req.getBirthDate() != null) p.setBirthDate(req.getBirthDate());
        if (req.getAddress() != null) p.setAddress(req.getAddress());
        if (req.getIdCard() != null) p.setIdCard(req.getIdCard());
        if (req.getChiefComplaint() != null) p.setChiefComplaint(req.getChiefComplaint());
        patientRepository.updateById(p); return p;
    }

    public void deletePatient(Long id, Long userId) { getPatient(id, userId); patientRepository.deleteById(id); }
}
