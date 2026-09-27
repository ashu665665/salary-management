package com.acme.salary.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.salary.dto.EmployeeCriteria;
import com.acme.salary.dto.EmployeeDetail;
import com.acme.salary.dto.EmployeeSummary;
import com.acme.salary.dto.MoneyView;
import com.acme.salary.exception.EmployeeNotFoundException;
import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.JobLevel;
import com.acme.salary.service.EmployeeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService service;

    private static EmployeeSummary aSummary() {
        return new EmployeeSummary(
                7L, "ACME-1", "Priya Nair", "priya@acme.example",
                Country.INDIA, Department.ENGINEERING, JobLevel.MID,
                LocalDate.of(2020, 1, 1), null, true,
                new MoneyView(new BigDecimal("1600000"), "INR"), LocalDate.of(2024, 4, 1));
    }

    private static EmployeeDetail aDetail() {
        return new EmployeeDetail(
                7L, "ACME-1", "Priya", "Nair", "Priya Nair", "priya@acme.example",
                Country.INDIA, Department.ENGINEERING, JobLevel.MID,
                LocalDate.of(2020, 1, 1), null, true,
                new MoneyView(new BigDecimal("1600000"), "INR"), List.of());
    }

    @Nested
    class ListingEmployees {

        @Test
        void returns_a_page_of_employees() throws Exception {
            when(service.search(any(EmployeeCriteria.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(aSummary()), PageRequest.of(0, 25), 1));

            mockMvc.perform(get("/api/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].employeeCode").value("ACME-1"))
                    .andExpect(jsonPath("$.content[0].fullName").value("Priya Nair"))
                    .andExpect(jsonPath("$.content[0].currentSalary.amount").value(1600000))
                    .andExpect(jsonPath("$.content[0].currentSalary.currency").value("INR"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.page").value(0));
        }

        @Test
        void passes_the_filters_through_to_the_service() throws Exception {
            when(service.search(any(EmployeeCriteria.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 25), 0));

            mockMvc.perform(get("/api/employees")
                            .param("search", "nair")
                            .param("country", "INDIA")
                            .param("department", "ENGINEERING")
                            .param("jobLevel", "MID")
                            .param("includeLeavers", "true"))
                    .andExpect(status().isOk());

            ArgumentCaptor<EmployeeCriteria> criteria = ArgumentCaptor.forClass(EmployeeCriteria.class);
            verify(service).search(criteria.capture(), any(Pageable.class));
            assertThat(criteria.getValue().search()).isEqualTo("nair");
            assertThat(criteria.getValue().country()).isEqualTo(Country.INDIA);
            assertThat(criteria.getValue().department()).isEqualTo(Department.ENGINEERING);
            assertThat(criteria.getValue().jobLevel()).isEqualTo(JobLevel.MID);
            assertThat(criteria.getValue().includeLeavers()).isTrue();
        }

        @Test
        void rejects_an_unknown_country() throws Exception {
            mockMvc.perform(get("/api/employees").param("country", "ATLANTIS"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ReadingOne {

        @Test
        void returns_the_employee() throws Exception {
            when(service.findDetail(7L)).thenReturn(aDetail());

            mockMvc.perform(get("/api/employees/7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Priya Nair"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        void returns_404_when_there_is_no_such_employee() throws Exception {
            when(service.findDetail(404L)).thenThrow(new EmployeeNotFoundException(404L));

            mockMvc.perform(get("/api/employees/404"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value("No employee with id 404"));
        }
    }

    @Nested
    class HiringAnEmployee {

        private static final String VALID_BODY = """
                {
                  "employeeCode": "ACME-9",
                  "firstName": "Dana",
                  "lastName": "Brooks",
                  "email": "dana@acme.example",
                  "country": "UNITED_STATES",
                  "department": "SALES",
                  "jobLevel": "MANAGER",
                  "hireDate": "2025-01-06",
                  "salaryAmount": 150000,
                  "currency": "USD"
                }
                """;

        @Test
        void returns_201_with_the_created_employee() throws Exception {
            when(service.hire(any())).thenReturn(aDetail());

            mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employeeCode").value("ACME-1"));
        }

        @Test
        void rejects_a_missing_name() throws Exception {
            String body = VALID_BODY.replace("\"firstName\": \"Dana\"", "\"firstName\": \" \"");

            mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.firstName").exists());
        }

        @Test
        void rejects_an_invalid_email() throws Exception {
            String body = VALID_BODY.replace("dana@acme.example", "not-an-email");

            mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists());
        }

        @Test
        void rejects_a_salary_that_is_not_positive() throws Exception {
            String body = VALID_BODY.replace("\"salaryAmount\": 150000", "\"salaryAmount\": 0");

            mockMvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.salaryAmount").exists());
        }
    }

    @Nested
    class ChangingAnEmployee {

        @Test
        void records_a_salary_revision() throws Exception {
            when(service.recordRevision(eq(7L), any())).thenReturn(aDetail());

            mockMvc.perform(post("/api/employees/7/revisions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"amount": 1400000, "currency": "INR",
                                     "effectiveDate": "2024-04-01", "reason": "ANNUAL_RAISE"}
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        void turns_a_rejected_revision_into_400() throws Exception {
            when(service.recordRevision(eq(7L), any()))
                    .thenThrow(new IllegalArgumentException("effective date 2019-01-01 is before the hire date"));

            mockMvc.perform(post("/api/employees/7/revisions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"amount": 1400000, "currency": "INR",
                                     "effectiveDate": "2019-01-01", "reason": "ANNUAL_RAISE"}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("effective date 2019-01-01 is before the hire date"));
        }

        @Test
        void turns_leaving_twice_into_409() throws Exception {
            when(service.markExit(eq(7L), any()))
                    .thenThrow(new IllegalStateException("ACME-1 already left on 2024-12-31"));

            mockMvc.perform(post("/api/employees/7/exit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lastWorkingDay\": \"2025-05-31\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        void updates_the_editable_details() throws Exception {
            when(service.updateDetails(eq(7L), any())).thenReturn(aDetail());

            mockMvc.perform(patch("/api/employees/7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "Priya", "lastName": "Nair-Kumar",
                                     "email": "priya.kumar@acme.example", "department": "PRODUCT"}
                                    """))
                    .andExpect(status().isOk());
        }
    }
}
