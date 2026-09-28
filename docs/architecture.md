# Architecture

How the system is put together, and why. The scope and the deliberate omissions are in
[requirements.md](requirements.md).

## Shape

```
Browser ──► Angular 21 (served by Spring from /static)
              │  /api/**
              ▼
         Spring Boot 3.5  ──►  Postgres 16
         controller → service → repository → model
```

One container holds all three. In development they run separately, with the dev server proxying
`/api` to the backend.

## The layers, and what belongs in each

| Layer | Holds | Does not hold |
| --- | --- | --- |
| `controller` | HTTP mapping, request validation | business rules, "what is today" |
| `service` | what the HR Manager can do, loading the right things, deciding the date | the rules about what a valid pay change is |
| `repository` | queries, including the reporting SQL | decisions about which rate set or date applies |
| `model` | the rules: what makes a salary revision valid | anything about HTTP or SQL |

The most useful line here is the one between `service` and `model`. `EmployeeService` decides what
to load and what to say when something is missing. `Employee` decides whether a pay change is
allowed at all. That is why every rule has a fast unit test and none of them need a database.

## The domain

```
Employee ──1:n──► SalaryRevision
  code, name, email                amount + currency (Money)
  country, department, level       effective date
  hire date, exit date             reason: hire / annual raise
                                           / promotion / market correction
```

**An employee has no salary field.** Pay is derived from the revisions as at a date:
`salaryOn(date)` returns the latest revision not after that date. This is the decision the rest of
the system leans on.

- It makes history free. "What changed this year" is the same query as "what do they earn", with a
  different date.
- It stops a future-dated raise showing as current pay.
- It means a correction is a new record, not an edit. `SalaryRevision` has getters and no setters.

**Revisions can only be created through `Employee`.** The constructor is package-private, because
the rules — not before the hire date, not after the exit date, one per date — only make sense with
the whole history in view. There is no way to write a revision that breaks them.

**`Money` is a value object** holding an amount and its currency. Combining different currencies
throws rather than producing a quietly wrong total, and the scale follows the currency, so JPY has
no decimal places. Equality compares by value rather than by `BigDecimal.equals`, where `100` and
`100.00` are different numbers.

**Dates are always passed in, never read from the clock inside the domain.** `Clock` is injected,
so "as at today" is a decision the service makes and a test can fix.

## Currency

Salaries are stored in the currency the employee is actually paid in. That is the truth HR enters
and checks, and converting on the way in would lose it.

Reporting needs one currency, so `exchange_rate` holds a **named rate set** (`2025-baseline`) and
every aggregate divides by the rate for that currency. The set is named rather than implied so a
figure can always be traced back to the rates that produced it, and a later set can be added
without changing any number already reported.

Year-on-year change is the exception: it is computed in the employee's **own** currency. A
percentage does not need converting, and using the base currency would fold exchange-rate movement
into a figure that is supposed to measure pay decisions. Anyone whose currency changed is left out
for the same reason.

## Reporting

All of it is SQL. Medians and quartiles over ten thousand rows are what a database is for; pulling
every salary into the application to sort it there would move a lot of data to compute one number.

One CTE defines *who counts and what they earn* — employed on the date, with the revision then in
force, converted through the rate set — and every report builds on it, so the definition cannot
drift between screens.

Two details worth knowing:

- **`distinct on (employee_id)`** gives the current salary per person in one pass, using the
  `(employee_id, effective_date desc)` index. Postgres-specific, and the right tool.
- **Outlier severity is measured in quartile widths**, the way a boxplot does it. The first version
  divided by the quartile itself, which structurally flatters the overpaid: a salary can sit three
  times above the upper quartile but never more than once below the lower one, so the underpaid —
  the people the report exists to find — were pushed off the list.

Peers are same country *and* same level. Comparing an Indian junior with an American one would flag
the entire India office, which is a fact about currencies, not about pay.

## Performance

The numbers that mattered, measured rather than assumed:

| Operation | Time |
| --- | --- |
| Seed 10,000 employees and 47,000 revisions | 2.4s |
| Payroll overview across 10,000 | 74ms |
| Breakdown by country | 27ms |
| Outliers | 51ms |

Three things make that work:

**The employee list is two queries, never N+1.** One for the page, one for the current salaries of
just those rows. The obvious approach — fetching revisions alongside the page — makes Hibernate
paginate *in memory*, loading all 10,000 rows to show 25. There is a comment in
`EmployeeRepository` saying so, because it looks like an easy "improvement" otherwise.

**Bulk writes bypass JPA.** `GenerationType.IDENTITY` is right for ordinary writes but stops
Hibernate batching inserts, since it needs a round trip per generated id. `EmployeeBatchWriter`
takes 10,000 ids from the sequence in one query, then sends two batches.

**Nothing is cached.** 10,000 employees with a few revisions each is about 47,000 rows, which is
small. Aggregates run in SQL with indexes on the filtered columns. Adding caching or materialised
views would be complexity defending against load that does not exist.

## Testing

| Kind | Count | Needs | Runs in |
| --- | --- | --- | --- |
| Backend unit | 118 | nothing | ~10s |
| Backend integration | 46 | Docker | ~20s |
| Frontend | 83 | nothing | ~35s |

Integration tests use Testcontainers, run the real migrations, and keep `ddl-auto: validate` on, so
an entity that drifts from the schema fails the build. That is not theoretical: the first time the
application was started against Postgres, validation caught `currency char(3)` where Hibernate
expected `varchar(3)` — and `char` pads with trailing spaces, which would have broken currency
comparison quietly.

The suite is split by name (`*Test` unit, `*IT` integration) so the fast loop stays fast.

## Trade-offs

| Decision | Why | What it costs |
| --- | --- | --- |
| Everything in one container | one command, works anywhere with Docker | one replica only; data safe only as the volume is |
| Salary as history | makes trends and corrections possible at all | more modelling than a single figure |
| Fixed exchange rates | figures are reproducible and testable | not current |
| Reporting in SQL | fast, and the right tool for percentiles | ties reporting to Postgres |
| Layer-first packages | immediately familiar to a Spring reviewer | each feature spreads across five folders as it grows |
| No caching | no complexity defending against load that does not exist | would need revisiting well beyond 10,000 |
| Enums for country and department | type-safe, simple, no join | adding one is a code change, not data |

## If this went further

Bonus and total compensation, so "what does this person cost" is answerable. CSV import with a
validated preview. A raise-cycle planner to model an increase across a group before committing it.
Pay bands per level and country, so outliers are measured against policy rather than only against
peers. And, before any real use, a database that lives outside the application container.
