# Salary Management — Requirements

**User:** the HR Manager at ACME, who looks after ~10,000 employees across several countries.
**Today:** it all lives in spreadsheets. Updating is slow and risky, and a question like "what do we pay engineers in India vs Germany" costs an afternoon of pivot tables.

## Goal

Get the HR Manager out of spreadsheets. Today the data is in Excel, and that causes five specific problems:

1. **Finding one person means scrolling 10,000 rows.** So the app has search and filters by country, department and level.
2. **Editing a salary wipes the old one.** You can't see what someone used to earn, or why it changed. So the app keeps every salary change as its own record, with a date and a reason.
3. **Each country has its own sheet and its own currency**, so nothing adds up across the org. So the app stores the local currency and converts to USD for any total or comparison.
4. **Every question needs a new pivot table.** So the app answers the common ones on a dashboard: total cost, cost by country, department and level, and how pay has moved this year.
5. **One wrong cell breaks a formula and nobody notices.** So the app validates what's entered and keeps the numbers in a database instead of in formulas.

## Scope & Features

**Employee records.** Searchable, filterable, sortable list, paged on the server. Add and edit employees. Mark a leaver with an exit date — they drop out of current cost and peer stats but keep their history.

**Salary history, not a single number.** Each employee has a series of revisions: amount, currency, effective date, reason (hire, annual raise, promotion, market correction). A new revision never overwrites the last one. This is the fix for pain point 2 — without history you can't see what someone used to earn or how pay has moved.

**Currencies.** Stored in the employee's local currency, which is what HR enters and checks. A fixed exchange-rate table in the seed data converts to USD for any total or comparison — fixed rather than live so numbers are reproducible and tests are predictable.

**Compensation view.** One employee's salary history, plus where they sit against the median, p25 and p75 for their level and country.

**Dashboard.** Headcount and annual payroll cost by country, department and level; median and quartiles per group; average year-on-year increase; who is below p25 or above p75 for their peer group. Medians and quartiles rather than averages, because a few senior salaries skew an average enough to make it useless for a pay decision.

**CSV export** of the current list. HR always needs to hand numbers to finance, and export is cheap and read-only.

**Seed script** for 10,000 employees with a realistic spread of countries, departments, levels and currencies, and a few revisions each so the dashboard shows something real.

**Stack.** Java 21 + Spring Boot, Angular, Postgres, Docker for local setup, deployed to a public URL.

## Deliberately Left Out

**Login and roles** — one persona, and auth isn't required. The time is better spent on the salary domain.

**A separate audit log** — the salary history already records what changed, when and why. With a single user, recording *who* adds nothing.

**CSV/Excel import and bulk updates** — the expensive one: column mapping, per-row validation, a preview step, and a story for half-failed uploads. A half-built import is more dangerous than the spreadsheet it replaces, so I'd rather get the data model and reporting right.

**Payroll processing** (tax, deductions, payslips, payment runs) — a different product, with rules that change per country. This app reports on pay; it doesn't pay anyone.

**Approval flows and notifications** — these need a second person in the loop, like a finance approver, to mean anything.

**Gender pay gap analysis** — valuable in real life, but it needs gender data that isn't in this brief. Inventing it in a seed script to render a chart would give a misleading answer to a question that deserves a real one.

**Bonus, equity and benefits** — annual gross base only. Each extra component adds currency and aggregation work without making the problem more interesting.

**Cost-of-living adjustment** — whether a Pune salary is comparable to a Berlin one is a judgment the HR Manager should make, not something buried inside a number.

**Hourly and part-time** — everyone seeded is full-time salaried. Prorating is arithmetic, not a design question.

**Self-service, multiple orgs, translations, mobile-first, caching** — nobody in this brief needs them. On caching specifically: 10,000 employees at a few revisions each is ~40,000 rows, which is small for Postgres. Aggregates run in SQL with indexes on the filtered columns; there's no load here that justifies more.

## What changed after this was written

This document was written before any code. Three things moved, and it is more useful to record that
than to quietly edit the original.

**CSV export survived; CSV import stayed out.** As planned. Export is one endpoint and cannot
corrupt anything; import needs column mapping, per-row validation and a preview to be safe.

**The seed defaults to 25 employees, not 10,000.** The generator supports any size and 10,000 takes
2.4 seconds, but carrying that much demo data through local, test and live environments buys
nothing. It is one environment variable (`SEED_EMPLOYEE_COUNT`). The trade is visible on the
dashboard: peer groups of fewer than five are not compared, so at 25 employees the outlier panel is
empty by design.

**The whole system ships as one container.** Database, API and UI together, started with
`docker compose up`. Not how a production system should be arranged — a database belongs outside
the application it serves — but it makes the system runnable anywhere with one command, which
matters more here. The cost is recorded in the README and in the architecture notes.

**Salary history proved to be the right call.** It was the one place the plan spent extra modelling
effort, and everything interesting on the dashboard — year-on-year movement, the reason a salary
changed, the story behind a number — comes from it.
