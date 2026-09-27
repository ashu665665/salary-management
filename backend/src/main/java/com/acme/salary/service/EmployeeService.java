package com.acme.salary.service;

import com.acme.salary.dto.EmployeeCriteria;
import com.acme.salary.dto.EmployeeDetail;
import com.acme.salary.dto.EmployeeSummary;
import com.acme.salary.dto.ExitRequest;
import com.acme.salary.dto.HireEmployeeRequest;
import com.acme.salary.dto.PromoteRequest;
import com.acme.salary.dto.RecordRevisionRequest;
import com.acme.salary.dto.UpdateEmployeeRequest;
import com.acme.salary.repository.CurrentSalaryRow;
import com.acme.salary.repository.EmployeeRepository;
import com.acme.salary.repository.SalaryRevisionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * The operations the HR Manager can perform, in terms the UI can call.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employees;
    private final SalaryRevisionRepository revisions;
    private final Clock clock;

    public Page<EmployeeSummary> search(EmployeeCriteria criteria, Pageable pageable) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public EmployeeDetail findDetail(Long id) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public EmployeeDetail hire(HireEmployeeRequest request) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public EmployeeDetail updateDetails(Long id, UpdateEmployeeRequest request) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public EmployeeDetail recordRevision(Long id, RecordRevisionRequest request) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public EmployeeDetail promote(Long id, PromoteRequest request) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public EmployeeDetail markExit(Long id, ExitRequest request) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public List<CurrentSalaryRow> currentSalaries(List<Long> employeeIds, LocalDate asOf) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
