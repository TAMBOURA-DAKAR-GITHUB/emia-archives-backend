package com.emia.archives.service;

import com.emia.archives.dto.GradeDto;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<GradeDto> getAllGrades() {
        return gradeRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }
}
