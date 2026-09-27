create table employee (
    id            bigserial primary key,
    employee_code varchar(20)  not null unique,
    first_name    varchar(80)  not null,
    last_name     varchar(80)  not null,
    email         varchar(160) not null unique,
    country       varchar(40)  not null,
    department    varchar(40)  not null,
    job_level     varchar(40)  not null,
    hire_date     date         not null,
    exit_date     date,
    constraint employee_exit_not_before_hire check (exit_date is null or exit_date >= hire_date)
);

-- The employee list is always filtered by some combination of these three, and leavers are
-- excluded from current figures, so exit_date is part of the filter rather than an afterthought.
create index idx_employee_country on employee (country);
create index idx_employee_department on employee (department);
create index idx_employee_job_level on employee (job_level);
create index idx_employee_exit_date on employee (exit_date);

create table salary_revision (
    id             bigserial primary key,
    employee_id    bigint       not null references employee (id) on delete cascade,
    amount         numeric(15, 2) not null check (amount > 0),
    currency       varchar(3)   not null,
    effective_date date         not null,
    reason         varchar(30)  not null,
    recorded_at    timestamptz  not null,
    -- One change per employee per day: a same-day correction should replace the figure, not sit
    -- alongside it leaving two possible answers for that date.
    constraint uq_revision_per_employee_per_date unique (employee_id, effective_date)
);

-- Finding the revision in force on a date means looking for the newest one not after it, so the
-- index is ordered by effective_date descending within an employee.
create index idx_revision_employee_effective on salary_revision (employee_id, effective_date desc);
