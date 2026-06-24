package com.emia.archives.service;

import com.emia.archives.dto.SchoolDto;
import com.emia.archives.entity.School;
import com.emia.archives.exception.ResourceNotFoundException;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<SchoolDto> getAllSchools() {
        return schoolRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolDto getSchoolById(String id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("École", "id", id));
        return mapper.toDto(school);
    }

    public School getSchoolEntityById(String id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("École", "id", id));
    }
}
