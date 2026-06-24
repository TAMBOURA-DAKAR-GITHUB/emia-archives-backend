package com.emia.archives.controller;

import com.emia.archives.dto.SchoolDto;
import com.emia.archives.dto.response.ApiResponse;
import com.emia.archives.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SchoolDto>>> getAllSchools() {
        return ResponseEntity.ok(ApiResponse.ok(schoolService.getAllSchools()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SchoolDto>> getSchoolById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(schoolService.getSchoolById(id)));
    }
}
