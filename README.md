# Employee Salary Management

Salary management for an organisation of 10,000 employees across several countries, built for an
HR Manager who currently does this in spreadsheets.

It does two jobs: keep salary data safe and easy to change, and answer questions about how the
organisation pays people.

## Start with the requirements document

**[docs/requirements.md](docs/requirements.md)** is the one-page requirements document: the goal,
the scope and features, and what is deliberately left out with the reasoning for each omission. It
was written before any code and is the yardstick everything here was built against.

It is the best place to start reading, because most of what follows is a consequence of decisions
recorded there — why a salary is a history rather than a number, why figures convert through a
fixed rate set, and why CSV import, authentication and payroll processing are not here.

The other documents:

| Document | What it covers |
| --- | --- |
| [docs/requirements.md](docs/requirements.md) | Goal, scope, and what was deliberately left out |
| [docs/architecture.md](docs/architecture.md) | How it is built, the domain model, performance, trade-offs |
| [docs/ai-workflow.md](docs/ai-workflow.md) | How AI was used, and the defects it introduced |

## Run it

One command. Docker is the only thing you need installed.

```bash
docker compose up
```

Then open **http://localhost:8080**. The first start creates the database, runs the migrations and
fills it with a small demo organisation. It takes about a minute to build and ten seconds to start.

The container holds Postgres, the API and the UI together, so there is nothing else to set up. The
database lives in a Docker volume, so data survives `docker compose down` and a rebuild.

To start again from scratch:

```bash
docker compose down -v      # -v also deletes the database volume
docker compose up --build
```

## What you can do

| Screen | What it answers |
| --- | --- |
| **Dashboard** | What does payroll cost, split by country, department or level? What does a typical person earn? How fast is pay moving? Who is paid outside the range for their job? |
| **Employees** | Where is the person I am looking for, out of 10,000? Filter by country, department, level; include or exclude leavers; export what is on screen to CSV. |
| **Employee** | What is this person paid, what were they paid before, and why did it change? Record a raise or a promotion, correct their details, record them as a leaver. |

## Configuration

Everything has a working default; these are the ones worth knowing.

| Variable | Default | What it does |
| --- | --- | --- |
| `SEED_ENABLED` | `true` | Fill an empty database with demo data. Never touches data that is already there. |
| `SEED_EMPLOYEE_COUNT` | `25` | How many employees to generate. Set it to `10000` for a full-sized organisation. |
| `SEED_RESET` | `false` | Delete every employee and salary record, then seed again. Off for a reason. |
| `SEED_RANDOM_SEED` | `20250601` | The same seed always produces the same organisation. |
| `DATABASE_URL` | local Postgres | Point the application at a database outside the container. |
| `PORT` | `8080` | Port the application listens on. |

A larger organisation is worth knowing about: the dashboard compares people against others at the
same level in the same country, and peer groups of fewer than five are not reported on. At 25
employees almost every peer group is one person, so the outlier panel is empty by design. Seed a
few hundred to see that part of the dashboard do its job:

```bash
SEED_EMPLOYEE_COUNT=300 docker compose up
```

## Running it after a code change

The single container is for running the system, not for working in it: every change would mean a
full image rebuild. Three ways to run it, depending on what you touched.

Requires **JDK 21** and **Node 22.12+**. All three need the database, so start it once and leave
it: it keeps its data between runs.

```bash
docker compose -f docker-compose.dev.yml up -d    # Postgres on 5432
```

### After a backend change

Run the API from source. It seeds an empty database on startup, so the first run gives you the
demo organisation to work against.

```bash
cd backend
mvn spring-boot:run          # http://localhost:8080
```

Stop with Ctrl-C and run it again to pick up a change. The API is usable on its own while you work
on it:

```bash
curl "http://localhost:8080/api/employees?size=5"
curl "http://localhost:8080/api/analytics/overview"
```

Run `mvn test` before you commit — it needs no database and takes a few seconds.

### After a frontend change

Leave the backend running as above, then start the dev server in a second terminal. It rebuilds on
save, and proxies `/api` to the backend on 8080, so the two halves behave as though they were
served together.

```bash
cd frontend
npm install                  # first time only
npm start                    # http://localhost:4200
```

Open **4200**, not 8080 — 8080 is the API while you are working this way. If the dev server fails
to start its worker pool on a machine short of memory, use `NG_BUILD_MAX_WORKERS=1 npm start`.

### Both together, the way it is deployed

When you want to check the real thing — Spring serving the compiled Angular from one container,
which is what production runs — build the image and run it:

```bash
docker compose up --build    # http://localhost:8080
```

**`--build` is the part that matters after a code change.** Without it Docker reuses the last
image and you will be looking at your previous version, which is a confusing five minutes. The
build compiles Angular, packages the jar with the UI inside it, and starts the container: a few
minutes the first time, less afterwards because the dependency layers are cached.

Worth doing before you push anything that touches the build, the static assets or the routing,
since those only behave differently once both halves are served from the same origin.

Stop the dev database when you are done with it:

```bash
docker compose -f docker-compose.dev.yml down     # add -v to delete its data too
```

## Testing

Two kinds, run separately, because they cost different amounts.

```bash
cd backend
mvn test        # 118 unit tests, no database, a few seconds
mvn verify      # adds 46 integration tests against a real Postgres via Testcontainers

cd frontend
npm test        # 83 component and service tests, jsdom, no browser
```

Unit tests are the loop you work in. The integration tests start a real Postgres and run the actual
migrations, because SQL and entity mappings are only genuinely exercised against a real database —
`ddl-auto: validate` means an entity that drifts from the schema fails the build.

On a machine with little free memory, the frontend test runner can have its workers killed. If you
see *"Worker exited unexpectedly"*, run it with one worker:

```bash
NODE_OPTIONS=--max-old-space-size=768 VITEST_MAX_FORKS=1 npm test
```

## How it is put together

```
backend/          Java 21, Spring Boot 3.5, Postgres, Flyway
  controller/     HTTP endpoints
  service/        what the HR Manager can do; seeding and CSV
  repository/     queries, including the reporting SQL
  model/          Employee, SalaryRevision, Money — where the rules live
  dto/            the shapes the API speaks in
  config/         clock, seeding and reporting settings

frontend/         Angular 21, standalone components and signals, Angular Material
  core/           typed API clients and models
  features/       dashboard, employee list, employee detail, dialogs
  shared/         formatting pipes

docker/           entrypoint for the combined container
docs/             requirements, architecture, and how AI was used
```

## The decisions that shaped it

The reasoning behind these is in [docs/architecture.md](docs/architecture.md); the scope they serve
is in [docs/requirements.md](docs/requirements.md).

- **A salary is a history, not a number.** Every change is a dated record with a reason, and
  nothing overwrites what came before. "What do they earn now", "what did they earn last April" and
  "how much has pay moved" become the same question asked with a different date.
- **Money carries its currency.** Adding rupees to euros throws rather than quietly producing a
  wrong total, and amounts take the scale of their currency — so JPY has no decimal places.
- **Totals convert through a named, fixed rate set.** A payroll figure that changes because an
  exchange rate moved overnight is not a number anyone can take to a meeting.
- **Reporting is SQL.** Medians and quartiles over ten thousand rows are the database's job.
- **The list is two queries, never N+1.** One for the page of employees, one for their current
  salaries.

## Known limitations

- **One container means one replica.** The database lives inside it, so this cannot be scaled
  horizontally, and the data is only as safe as the volume. That is the trade for a single command,
  and it is the first thing to change for real production use.
- **No authentication.** One HR Manager persona, confirmed as out of scope for this exercise.
- **Exchange rates are fixed, not live.** Deliberate: reproducible figures beat current ones here.
- **The export builds the file in memory.** Fine at this size; an export covering far more than
  10,000 people would need to stream.
