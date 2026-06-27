package com.emia.archives.controller;

import com.emia.archives.dto.PersonnelDto;
import com.emia.archives.dto.request.CreatePersonnelRequest;
import com.emia.archives.dto.response.ApiResponse;
import com.emia.archives.dto.response.MemberPromotionDetail;
import com.emia.archives.enums.PersonnelCategorie;
import com.emia.archives.service.ExportService;
import com.emia.archives.service.PersonnelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/com-emia")
@RequiredArgsConstructor
public class ComEmiaController {

    private final PersonnelService personnelService;
    private final ExportService exportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonnelDto>>> getAll(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(personnelService.getAll(schoolId, PersonnelCategorie.COM_EMIA)));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable String schoolId) throws Exception {
        byte[] data = exportService.exportPersonnelExcel(schoolId, PersonnelCategorie.COM_EMIA);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=com-emia.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonnelDto>> getById(@PathVariable String schoolId,
                                                               @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(personnelService.getById(id, schoolId)));
    }

    @GetMapping("/{id}/promotions")
    public ResponseEntity<ApiResponse<MemberPromotionDetail>> getMemberPromotions(
            @PathVariable String schoolId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(personnelService.getMemberWithPromotions(id, schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PersonnelDto>> create(@PathVariable String schoolId,
                                                             @RequestBody CreatePersonnelRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                personnelService.create(schoolId, request, PersonnelCategorie.COM_EMIA),
                "Membre COM EMIA créé avec succès"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonnelDto>> update(@PathVariable String schoolId,
                                                             @PathVariable Long id,
                                                             @RequestBody CreatePersonnelRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                personnelService.update(id, schoolId, request),
                "Membre COM EMIA modifié avec succès"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId,
                                                     @PathVariable Long id) {
        personnelService.delete(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Membre COM EMIA supprimé avec succès"));
    }
}
