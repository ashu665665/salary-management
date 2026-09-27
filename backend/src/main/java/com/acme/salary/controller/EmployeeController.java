package com.acme.salary.controller;

import com.acme.salary.dto.EmployeeCriteria;
import com.acme.salary.dto.CsvExport;
import com.acme.salary.dto.EmployeeDetail;
import com.acme.salary.dto.EmployeeSummary;
import com.acme.salary.dto.ExitRequest;
import com.acme.salary.dto.HireEmployeeRequest;
import com.acme.salary.dto.PageResponse;
import com.acme.salary.dto.PromoteRequest;
import com.acme.salary.dto.RecordRevisionRequest;
import com.acme.salary.dto.UpdateEmployeeRequest;
import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.JobLevel;
import com.acme.salary.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService service;

    @GetMapping
    public PageResponse<EmployeeSummary> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Country country,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) JobLevel jobLevel,
            @RequestParam(defaultValue = "false") boolean includeLeavers,
            @PageableDefault(size = 25, sort = "lastName") Pageable pageable) {

        EmployeeCriteria criteria = new EmployeeCriteria(search, country, department, jobLevel, includeLeavers);

        return toPageResponse(service.search(criteria, pageable));
    }

    /** The current list as a CSV download, filtered exactly as the screen is. */
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Country country,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) JobLevel jobLevel,
            @RequestParam(defaultValue = "false") boolean includeLeavers) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @GetMapping("/{id}")
    public EmployeeDetail get(@PathVariable Long id) {
        return service.findDetail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDetail hire(@Valid @RequestBody HireEmployeeRequest request) {
        return service.hire(request);
    }

    @PatchMapping("/{id}")
    public EmployeeDetail update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return service.updateDetails(id, request);
    }

    @PostMapping("/{id}/revisions")
    public EmployeeDetail recordRevision(@PathVariable Long id, @Valid @RequestBody RecordRevisionRequest request) {
        return service.recordRevision(id, request);
    }

    @PostMapping("/{id}/promotion")
    public EmployeeDetail promote(@PathVariable Long id, @Valid @RequestBody PromoteRequest request) {
        return service.promote(id, request);
    }

    @PostMapping("/{id}/exit")
    public EmployeeDetail markExit(@PathVariable Long id, @Valid @RequestBody ExitRequest request) {
        return service.markExit(id, request);
    }

    private static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
