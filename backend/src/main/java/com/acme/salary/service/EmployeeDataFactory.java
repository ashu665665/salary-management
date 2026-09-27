package com.acme.salary.service;

import com.acme.salary.model.Country;
import com.acme.salary.model.Department;
import com.acme.salary.model.Employee;
import com.acme.salary.model.JobLevel;
import com.acme.salary.model.Money;
import com.acme.salary.model.RevisionReason;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Builds a plausible organisation of a given size.
 *
 * <p>Plausible matters: a dashboard over uniformly random salaries shows nothing worth looking at.
 * So the generated org has a level pyramid, pay that differs by country and seniority, annual
 * review rounds, occasional promotions and a few leavers.
 *
 * <p>Everything comes from one seeded {@link Random}, so the same seed and date always produce the
 * same organisation. The employees are built through {@link Employee} rather than assembled as
 * rows, which means the seed data has to satisfy every rule the application enforces.
 */
@Component
public class EmployeeDataFactory {

    /** ACME runs its pay review on the first of April. */
    private static final Month REVIEW_MONTH = Month.APRIL;

    private static final int LONGEST_SERVICE_YEARS = 12;

    /**
     * How much the pay bands themselves move each year.
     *
     * <p>This is what creates people who are underpaid without anyone deciding to underpay them:
     * someone hired eight years ago started on the band of that year, and if their raises since
     * have not kept up with the band, they now sit below people doing the same job. That drift is
     * the main thing an HR Manager wants this software to surface, so the data has to contain it.
     */
    private static final double BAND_GROWTH_PER_YEAR = 0.04;

    /** Roughly how many of every hundred people have left. */
    private static final int LEAVER_PERCENTAGE = 8;

    /**
     * A typical annual salary at each level, in US dollars, before the country adjustment below.
     */
    private static final Map<JobLevel, Integer> LEVEL_PAY_USD = Map.of(
            JobLevel.JUNIOR, 48_000,
            JobLevel.MID, 72_000,
            JobLevel.SENIOR, 102_000,
            JobLevel.LEAD, 128_000,
            JobLevel.MANAGER, 150_000,
            JobLevel.DIRECTOR, 205_000,
            JobLevel.EXECUTIVE, 320_000);

    /**
     * What a country pays relative to the US, and what its currency is worth against the dollar.
     *
     * <p>The rates are illustrative and fixed on purpose, so the numbers are reproducible. When the
     * exchange rate table arrives for reporting, these should be read from it rather than repeated.
     */
    private static final Map<Country, PayScale> PAY_SCALES = Map.of(
            Country.UNITED_STATES, new PayScale(1.00, 1.0),
            Country.UNITED_KINGDOM, new PayScale(0.88, 0.79),
            Country.GERMANY, new PayScale(0.86, 0.92),
            Country.CANADA, new PayScale(0.84, 1.36),
            Country.AUSTRALIA, new PayScale(0.87, 1.52),
            Country.SINGAPORE, new PayScale(0.82, 1.35),
            Country.JAPAN, new PayScale(0.74, 157.0),
            Country.POLAND, new PayScale(0.46, 4.00),
            Country.BRAZIL, new PayScale(0.36, 5.40),
            Country.INDIA, new PayScale(0.29, 83.0));

    /** How the headcount splits across countries, as shares out of 100. */
    private static final List<Weighted<Country>> COUNTRY_MIX = List.of(
            new Weighted<>(Country.INDIA, 34),
            new Weighted<>(Country.UNITED_STATES, 19),
            new Weighted<>(Country.POLAND, 9),
            new Weighted<>(Country.GERMANY, 8),
            new Weighted<>(Country.UNITED_KINGDOM, 8),
            new Weighted<>(Country.BRAZIL, 6),
            new Weighted<>(Country.CANADA, 5),
            new Weighted<>(Country.SINGAPORE, 4),
            new Weighted<>(Country.AUSTRALIA, 4),
            new Weighted<>(Country.JAPAN, 3));

    private static final List<Weighted<Department>> DEPARTMENT_MIX = List.of(
            new Weighted<>(Department.ENGINEERING, 38),
            new Weighted<>(Department.CUSTOMER_SUPPORT, 12),
            new Weighted<>(Department.SALES, 11),
            new Weighted<>(Department.OPERATIONS, 9),
            new Weighted<>(Department.PRODUCT, 8),
            new Weighted<>(Department.MARKETING, 7),
            new Weighted<>(Department.FINANCE, 6),
            new Weighted<>(Department.HR, 5),
            new Weighted<>(Department.LEGAL, 4));

    /** A pyramid: plenty of juniors, very few executives. */
    private static final List<Weighted<JobLevel>> LEVEL_MIX = List.of(
            new Weighted<>(JobLevel.JUNIOR, 26),
            new Weighted<>(JobLevel.MID, 30),
            new Weighted<>(JobLevel.SENIOR, 21),
            new Weighted<>(JobLevel.LEAD, 10),
            new Weighted<>(JobLevel.MANAGER, 8),
            new Weighted<>(JobLevel.DIRECTOR, 4),
            new Weighted<>(JobLevel.EXECUTIVE, 1));

    public List<Employee> generate(int count, LocalDate asOf, long randomSeed) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative: " + count);
        }
        Random random = new Random(randomSeed);
        List<Employee> employees = new ArrayList<>(count);
        for (int number = 1; number <= count; number++) {
            employees.add(generateOne(number, asOf, random));
        }
        return employees;
    }

    private Employee generateOne(int number, LocalDate asOf, Random random) {
        Country country = pick(COUNTRY_MIX, random);
        Department department = pick(DEPARTMENT_MIX, random);
        JobLevel currentLevel = pick(LEVEL_MIX, random);
        LocalDate hireDate = randomHireDate(asOf, random);

        // People are promoted into their level rather than hired at the top of it, so work out
        // where they started and climb back up through their pay history.
        int promotions = plannedPromotions(currentLevel, hireDate, asOf, random);
        JobLevel startingLevel = levelBelow(currentLevel, promotions);

        Names names = namesFor(country, random);
        Employee employee = Employee.hire(
                "ACME-%05d".formatted(number),
                names.first(),
                names.last(),
                "%s.%s.%d@acme.example".formatted(names.first().toLowerCase(), names.last().toLowerCase(), number),
                country,
                department,
                startingLevel,
                hireDate,
                salaryFor(country, startingLevel, random, hireDate, asOf));

        LocalDate exitDate = maybeExitDate(hireDate, asOf, random);
        buildPayHistory(employee, currentLevel, promotions, hireDate,
                exitDate == null ? asOf : exitDate, asOf, random);
        if (exitDate != null) {
            employee.markExit(exitDate);
        }
        return employee;
    }

    /**
     * Walks the review rounds from the year after joining to the last one that has happened,
     * raising pay each year and promoting at the rounds chosen for it.
     *
     * <p>The promotion rounds are picked up front rather than spaced by a formula. An earlier
     * version promoted whoever had not caught up on their final review, which quietly bunched
     * promotions into the recent past and pushed the reported year-on-year increase to 15%.
     */
    private void buildPayHistory(Employee employee, JobLevel targetLevel, int promotions,
            LocalDate hireDate, LocalDate until, LocalDate asOf, Random random) {

        List<LocalDate> reviews = reviewDates(hireDate, until);
        if (reviews.isEmpty()) {
            return;
        }
        Set<Integer> promotionRounds = pickRounds(reviews.size(), Math.min(promotions, reviews.size()), random);

        for (int index = 0; index < reviews.size(); index++) {
            LocalDate reviewDate = reviews.get(index);
            Money current = employee.latestRevision().getSalary();

            if (promotionRounds.contains(index) && targetLevel.isAbove(employee.getJobLevel())) {
                JobLevel nextLevel = levelAbove(employee.getJobLevel());
                employee.promoteTo(nextLevel,
                        promotionSalary(employee, nextLevel, current, random, reviewDate, asOf), reviewDate);
            } else if (random.nextInt(100) < 7) {
                employee.recordRevision(
                        raised(current, employee, random, 0.02, 0.06), reviewDate, RevisionReason.MARKET_CORRECTION);
            } else {
                employee.recordRevision(
                        raised(current, employee, random, 0.02, 0.07), reviewDate, RevisionReason.ANNUAL_RAISE);
            }
        }
    }

    /** Which review rounds a promotion falls on, spread across a career rather than bunched. */
    private Set<Integer> pickRounds(int totalRounds, int howMany, Random random) {
        Set<Integer> chosen = new HashSet<>();
        while (chosen.size() < howMany) {
            chosen.add(random.nextInt(totalRounds));
        }
        return chosen;
    }

    /** Every first of April between joining and the cut-off. */
    private List<LocalDate> reviewDates(LocalDate hireDate, LocalDate until) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate review = LocalDate.of(hireDate.getYear() + 1, REVIEW_MONTH, 1);
        while (!review.isAfter(until)) {
            if (review.isAfter(hireDate)) {
                dates.add(review);
            }
            review = review.plusYears(1);
        }
        return dates;
    }

    private int plannedPromotions(JobLevel currentLevel, LocalDate hireDate, LocalDate asOf, Random random) {
        int yearsOfService = Math.max(0, asOf.getYear() - hireDate.getYear());
        int possibleByTenure = yearsOfService / 3;
        int possibleByLevel = currentLevel.getRank() - JobLevel.JUNIOR.getRank();
        int possible = Math.min(possibleByTenure, possibleByLevel);
        return possible <= 0 ? 0 : random.nextInt(possible + 1);
    }

    private LocalDate randomHireDate(LocalDate asOf, Random random) {
        // Square the fraction so recent years are more crowded than old ones, the way a growing
        // company actually looks.
        double fraction = Math.pow(random.nextDouble(), 2);
        long daysAgo = (long) (fraction * LONGEST_SERVICE_YEARS * 365) + 30;
        return asOf.minusDays(daysAgo);
    }

    private LocalDate maybeExitDate(LocalDate hireDate, LocalDate asOf, Random random) {
        if (random.nextInt(100) >= LEAVER_PERCENTAGE) {
            return null;
        }
        long daysEmployed = hireDate.until(asOf).toTotalMonths() * 30L;
        if (daysEmployed < 400) {
            return null;
        }
        long stayedFor = 365 + random.nextInt((int) Math.max(1, daysEmployed - 365));
        return hireDate.plusDays(stayedFor);
    }

    /**
     * What this level paid in this country on a given date: today's band, wound back by however
     * many years ago that was.
     */
    private Money salaryFor(Country country, JobLevel level, Random random, LocalDate on, LocalDate asOf) {
        PayScale scale = PAY_SCALES.get(country);
        double yearsAgo = Math.max(0, asOf.getYear() - on.getYear());
        double bandThen = LEVEL_PAY_USD.get(level) / Math.pow(1 + BAND_GROWTH_PER_YEAR, yearsAgo);
        double usd = bandThen * scale.countryMultiplier();
        // Two people at the same level in the same country do not earn exactly the same.
        double withSpread = usd * (0.88 + random.nextDouble() * 0.24);
        BigDecimal local = BigDecimal.valueOf(withSpread * scale.unitsPerUsd());
        return Money.of(roundSensibly(local, country), country.getPayCurrency());
    }

    /** Nobody is paid 1,234,567. Salaries are round numbers. */
    private BigDecimal roundSensibly(BigDecimal amount, Country country) {
        BigDecimal unit = switch (country) {
            case INDIA, JAPAN -> BigDecimal.valueOf(5000);
            case BRAZIL, POLAND -> BigDecimal.valueOf(500);
            default -> BigDecimal.valueOf(250);
        };
        return amount.divide(unit, 0, RoundingMode.HALF_UP).multiply(unit);
    }

    /**
     * A promotion puts someone on the band for the level they are moving into, never less than a
     * small step up from what they were on. Bumping by a flat percentage instead would leave the
     * senior bands full of people paid like the level below.
     */
    private Money promotionSalary(Employee employee, JobLevel newLevel, Money current, Random random,
            LocalDate effectiveDate, LocalDate asOf) {
        Money band = salaryFor(employee.getCountry(), newLevel, random, effectiveDate, asOf);
        Money floor = raised(current, employee, random, 0.05, 0.09);
        return band.isGreaterThan(floor) ? band : floor;
    }

    /** A raise, rounded the same way a starting salary is: payroll deals in round numbers. */
    private Money raised(Money current, Employee employee, Random random, double minimum, double maximum) {
        Money increased = current.multipliedBy(raiseFactor(random, minimum, maximum));
        return Money.of(roundSensibly(increased.getAmount(), employee.getCountry()), increased.getCurrency());
    }

    private BigDecimal raiseFactor(Random random, double minimum, double maximum) {
        return BigDecimal.valueOf(1 + minimum + random.nextDouble() * (maximum - minimum));
    }

    private JobLevel levelAbove(JobLevel level) {
        return byRank(level.getRank() + 1);
    }

    private JobLevel levelBelow(JobLevel level, int steps) {
        return byRank(Math.max(JobLevel.JUNIOR.getRank(), level.getRank() - steps));
    }

    private JobLevel byRank(int rank) {
        for (JobLevel level : JobLevel.values()) {
            if (level.getRank() == rank) {
                return level;
            }
        }
        throw new IllegalArgumentException("no job level with rank " + rank);
    }

    private <T> T pick(List<Weighted<T>> options, Random random) {
        int total = options.stream().mapToInt(Weighted::weight).sum();
        int roll = random.nextInt(total);
        int running = 0;
        for (Weighted<T> option : options) {
            running += option.weight();
            if (roll < running) {
                return option.value();
            }
        }
        return options.get(options.size() - 1).value();
    }

    private Names namesFor(Country country, Random random) {
        NamePool pool = NAME_POOLS.getOrDefault(country, DEFAULT_NAMES);
        return new Names(
                pool.firstNames().get(random.nextInt(pool.firstNames().size())),
                pool.lastNames().get(random.nextInt(pool.lastNames().size())));
    }

    private record PayScale(double countryMultiplier, double unitsPerUsd) {
    }

    private record Weighted<T>(T value, int weight) {
    }

    private record Names(String first, String last) {
    }

    private record NamePool(List<String> firstNames, List<String> lastNames) {
    }

    private static final NamePool DEFAULT_NAMES = new NamePool(
            List.of("Dana", "Sam", "Mia", "Noah", "Olivia", "Ethan", "Grace", "Liam", "Chloe", "Owen",
                    "Ava", "Caleb", "Isla", "Mason", "Ruby", "Felix"),
            List.of("Brooks", "Cole", "Tan", "Bennett", "Clarke", "Foster", "Hayes", "Reed",
                    "Wallace", "Barnes", "Ellis", "Griffin"));

    private static final Map<Country, NamePool> NAME_POOLS = Map.of(
            Country.INDIA, new NamePool(
                    List.of("Priya", "Rahul", "Ananya", "Vikram", "Meera", "Arjun", "Kavya", "Rohan",
                            "Divya", "Aditya", "Nisha", "Karthik"),
                    List.of("Nair", "Sharma", "Iyer", "Patel", "Reddy", "Gupta", "Menon", "Desai",
                            "Rao", "Kulkarni", "Bose", "Joshi")),
            Country.JAPAN, new NamePool(
                    List.of("Haruto", "Yuna", "Sora", "Aoi", "Ren", "Mio", "Hina", "Kaito"),
                    List.of("Sato", "Suzuki", "Tanaka", "Watanabe", "Ito", "Yamamoto", "Nakamura")),
            Country.GERMANY, new NamePool(
                    List.of("Jonas", "Lena", "Lukas", "Greta", "Felix", "Anna", "Niklas", "Sophie"),
                    List.of("Weber", "Schmidt", "Fischer", "Becker", "Hoffmann", "Wagner", "Keller")),
            Country.POLAND, new NamePool(
                    List.of("Mateusz", "Zofia", "Piotr", "Julia", "Jakub", "Maja", "Tomasz", "Hanna"),
                    List.of("Kowalski", "Nowak", "Wojcik", "Kaminski", "Lewandowski", "Zielinski")),
            Country.BRAZIL, new NamePool(
                    List.of("Ana", "Lucas", "Beatriz", "Thiago", "Camila", "Rafael", "Julia", "Pedro"),
                    List.of("Silva", "Souza", "Oliveira", "Costa", "Pereira", "Almeida", "Rocha")));
}
