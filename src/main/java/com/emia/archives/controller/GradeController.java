package com.emia.archives.controller;

import com.emia.archives.dto.GradeDto;
import com.emia.archives.dto.response.ApiResponse;
import com.emia.archives.mapper.EntityMapper;
import com.emia.archives.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeRepository gradeRepository;
    private final EntityMapper mapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GradeDto>>> getAllGrades() {
        List<GradeDto> grades = gradeRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(grades));
    }
}
