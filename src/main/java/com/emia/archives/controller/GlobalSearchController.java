package com.emia.archives.controller;

import com.emia.archives.dto.response.SearchResultDto;
import com.emia.archives.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService searchService;

    @GetMapping
    public ResponseEntity<List<SearchResultDto>> search(
            @RequestParam String schoolId,
            @RequestParam String q) {
        return ResponseEntity.ok(searchService.search(schoolId, q));
    }
}
