package com.acme.salary.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.dto.EmployeeSummary;
import com.acme.salary.dto.MoneyView;
import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.JobLevel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The export exists so HR can hand numbers to finance, which in practice means opening the file in
 * Excel. That is what these tests hold it to: a file Excel reads correctly, including the awkward
 * cases where a value contains the separator.
 */
class EmployeeCsvWriterTest {

    private final EmployeeCsvWriter writer = new EmployeeCsvWriter();

    private static EmployeeSummary row(String code, String name, MoneyView salary) {
        return new EmployeeSummary(
                1L, code, name, "someone@acme.example",
                Country.INDIA, Department.ENGINEERING, JobLevel.MID,
                LocalDate.of(2020, 1, 1), null, true,
                salary, salary == null ? null : LocalDate.of(2024, 4, 1));
    }

    private static MoneyView rupees(String amount) {
        return new MoneyView(new BigDecimal(amount), "INR");
    }

    private static List<String> linesOf(String csv) {
        return List.of(csv.replace("﻿", "").split("\r\n"));
    }

    @Test
    void starts_with_a_header_row() {
        String csv = writer.write(List.of());

        assertThat(linesOf(csv).get(0)).isEqualTo(
                "Employee Code,Name,Email,Country,Department,Level,Hire Date,Exit Date,Status,Salary,Currency,Salary Effective From");
    }

    @Test
    void writes_a_header_even_when_nobody_matched_the_filter() {
        assertThat(linesOf(writer.write(List.of()))).hasSize(1);
    }

    @Test
    void writes_one_line_per_employee() {
        String csv = writer.write(List.of(
                row("ACME-00001", "Priya Nair", rupees("1200000")),
                row("ACME-00002", "Rahul Desai", rupees("1400000"))));

        assertThat(linesOf(csv)).hasSize(3);
        assertThat(linesOf(csv).get(1)).startsWith("ACME-00001,Priya Nair,");
    }

    @Test
    void writes_the_salary_as_a_plain_number_so_excel_treats_it_as_one() {
        String csv = writer.write(List.of(row("ACME-00001", "Priya Nair", rupees("1200000.00"))));

        assertThat(linesOf(csv).get(1)).contains(",1200000.00,INR,");
    }

    @Test
    void leaves_the_salary_columns_empty_for_someone_with_no_pay_on_record() {
        String csv = writer.write(List.of(row("ACME-00001", "Priya Nair", null)));

        assertThat(linesOf(csv).get(1)).endsWith(",,,");
    }

    @Test
    void says_whether_someone_is_still_employed() {
        EmployeeSummary leaver = new EmployeeSummary(
                2L, "ACME-00009", "Sam Cole", "sam@acme.example",
                Country.CANADA, Department.LEGAL, JobLevel.SENIOR,
                LocalDate.of(2019, 3, 1), LocalDate.of(2024, 12, 31), false,
                new MoneyView(new BigDecimal("95000"), "CAD"), LocalDate.of(2023, 4, 1));

        String csv = writer.write(List.of(row("ACME-00001", "Priya Nair", rupees("1200000")), leaver));

        assertThat(linesOf(csv).get(1)).contains(",Active,");
        assertThat(linesOf(csv).get(2)).contains(",2024-12-31,Left,");
    }

    @Test
    void quotes_a_value_containing_the_separator() {
        String csv = writer.write(List.of(row("ACME-00001", "Nair, Priya", rupees("1200000"))));

        assertThat(linesOf(csv).get(1)).contains("\"Nair, Priya\"");
    }

    @Test
    void doubles_the_quotes_inside_a_quoted_value() {
        String csv = writer.write(List.of(row("ACME-00001", "Priya \"Pri\" Nair", rupees("1200000"))));

        assertThat(linesOf(csv).get(1)).contains("\"Priya \"\"Pri\"\" Nair\"");
    }

    @Test
    void keeps_a_value_containing_a_line_break_inside_one_field() {
        String csv = writer.write(List.of(row("ACME-00001", "Priya\nNair", rupees("1200000"))));

        assertThat(csv).contains("\"Priya\nNair\"");
    }

    @Test
    void separates_rows_the_way_excel_expects() {
        String csv = writer.write(List.of(row("ACME-00001", "Priya Nair", rupees("1200000"))));

        assertThat(csv).contains("\r\n");
    }

    @Test
    void starts_the_file_with_a_byte_order_mark_so_excel_reads_accented_names_correctly() {
        assertThat(writer.write(List.of())).startsWith("﻿");
    }
}
