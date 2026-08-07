package com.emia.archives.controller;

import com.emia.archives.dto.PersonnelDto;
import com.emia.archives.dto.request.CreatePersonnelRequest;
import com.emia.archives.dto.response.ApiResponse;
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
@RequestMapping("/api/schools/{schoolId}/ip")
@RequiredArgsConstructor
public class IpController {

    private final PersonnelService personnelService;
    private final ExportService exportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonnelDto>>> getAll(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(personnelService.getAll(schoolId, PersonnelCategorie.IP)));
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable String schoolId) throws Exception {
        byte[] data = exportService.exportPersonnelExcel(schoolId, PersonnelCategorie.IP);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ip.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable String schoolId) throws Exception {
        byte[] data = exportService.exportPersonnelPdf(schoolId, PersonnelCategorie.IP);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ip.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonnelDto>> getById(@PathVariable String schoolId,
                                                             @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(personnelService.getById(id, schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PersonnelDto>> create(@PathVariable String schoolId,
                                                            @RequestBody CreatePersonnelRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                personnelService.create(schoolId, request, PersonnelCategorie.IP),
                "Instructeur Permanent créé avec succès"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonnelDto>> update(@PathVariable String schoolId,
                                                            @PathVariable Long id,
                                                            @RequestBody CreatePersonnelRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                personnelService.update(id, schoolId, request),
                "Instructeur Permanent modifié avec succès"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId,
                                                    @PathVariable Long id) {
        personnelService.delete(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Instructeur Permanent supprimé avec succès"));
    }
}
