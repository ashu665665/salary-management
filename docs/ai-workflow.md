# How I used AI on this

Built with **Claude Code**. This is an honest account of how, including the things it got wrong,
because "used AI to write it" is not an interesting claim on its own — how the work was directed
and checked is the part worth reading.

## How the work was structured

The system was built in thin vertical slices, each one small enough to verify before moving on:
domain model, then repository and API, then seeding, then currency and reporting, then each screen.
The commit history follows that shape, and shows the tests for a slice landing before the code that
satisfies them.

Those test commits contain the type skeletons — interfaces, records, methods that throw — alongside
the tests. A commit that does not compile is not useful evidence of anything; a commit that
compiles and fails with `UnsupportedOperationException` is a real red step.

**Everything was verified by running it, not by the suite going green.** Each slice was exercised
against a seeded database, the screens were rendered in a headless browser and read, and the whole
system was driven through the container. Most of the defects below were found that way.

## What it got wrong, and how it was caught

This is the useful part. Six of the eight were found by running the thing, not by tests passing.

**Salary bands were compressed.** Generated executives earned barely more than directors ($185k vs
$176k) because promotions applied a flat 14–22% bump while the gap between levels is nearer 50%.
Found by reading the dashboard against real data. Promotions now move someone onto the band for
their new level.

**Nobody could ever be underpaid.** Every seeded employee started on *today's* band, so raises
could only push people up — the outlier report was 25 out of 25 ABOVE_RANGE. Adding band inflation
(4% a year) means someone hired eight years ago started on that year's band, which is exactly how
real underpayment happens. Found by noticing the report only ever pointed one way.

**Year-on-year pay movement read 15%.** Real organisations run 4–8%. Traced to a "catch-up" loop
that forced any undelivered promotions onto the most recent review round, bunching them into the
last year. Replacing it with promotion rounds chosen up front gave 6.8%.

**Outlier ranking structurally favoured the overpaid.** Severity was measured as a fraction of the
quartile, and a salary can sit three times above the upper quartile but never more than once below
the lower one. Switched to quartile widths, the way a boxplot measures it.

**`char(3)` instead of `varchar(3)`.** Caught on the first boot against Postgres by
`ddl-auto: validate`. Beyond the mismatch, `char` pads with trailing spaces, which would have
broken currency comparison silently.

**An O(n²) loop in the bulk writer.** `employees.indexOf(employee)` inside the batch insert
callback — wrong as well as slow, since two equal objects would collide. Caught reading the code
before running it.

**"Hr" and "USD76,500".** The label pipe lowercased acronyms, and the currency code ran into the
number. Both obvious in a screenshot and invisible to every test that existed.

**The entire backend test tree was moved into `src/main/resources/`.** Maven looks in
`src/test/java`, so on that commit the project had effectively zero tests while still reporting a
successful build. Caught reading `git status` before trusting the summary, fixed in the next commit.

## Two places where a test changed the design

**A slice test exposed a smell.** `@WebMvcTest` failed because the controller wanted a `Clock` —
and the service already had one. Two clocks is a bad answer to "what is today", so rather than
importing config into the test, the query type was split: the web layer passes on what was asked
for, and the service decides the date it is as at.

**Mockito's strict stubs found a redundant load.** An `UnnecessaryStubbing` failure showed that a
promotion sent to the raise endpoint is rejected *before* the employee is loaded. The test now
documents that rather than stubbing something the code never calls.

## What I did not accept

- **`@Data` and `@ToString` on JPA entities**, and Lombok-generated equality on `Money` —
  `BigDecimal.equals` compares scale, so `100` and `100.00` would be different salaries.
- **A generated getter for the revisions collection**, which would have handed out the live list
  and let callers bypass every rule in `recordRevision`.
- **`provideAnimationsAsync()`**, added out of habit from older Angular and not needed by Material
  21 — the build failed and the assumption went.
- **Seeding 10,000 rows by default.** The generator supports it, but carrying that data everywhere
  buys nothing; it is one environment variable.

## Where it was most and least useful

**Most:** the mechanical middle of the work — DTOs, mappers, Angular templates, the shape of a
test suite once the first few were written. And as a fast second reader of SQL.

**Least:** anything where "looks plausible" and "is right" diverge. The seeded data compiled,
passed every test, and told a false story about how the organisation paid people — three times.
That is the failure mode to watch for, and the only defence is running the thing and reading the
output like you would read a colleague's.
