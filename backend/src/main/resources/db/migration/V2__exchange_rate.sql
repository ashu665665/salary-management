-- Salaries are held in local currency, but a total across countries only means something in one
-- currency. These are the rates that conversion uses.
--
-- They are fixed rather than fetched: a payroll total that changes because a rate moved overnight
-- is not a number an HR Manager can take to a meeting. The rate set is named so that a figure can
-- always be traced back to the rates that produced it, and a later set can be added without
-- changing any number already reported.
create table exchange_rate (
    id            bigserial primary key,
    rate_set      varchar(40)    not null,
    currency      varchar(3)     not null,
    units_per_usd numeric(18, 6) not null check (units_per_usd > 0),
    constraint uq_exchange_rate_per_set unique (rate_set, currency)
);

create index idx_exchange_rate_set on exchange_rate (rate_set);

-- Illustrative mid-market rates, one unit of USD expressed in each currency.
insert into exchange_rate (rate_set, currency, units_per_usd) values
    ('2025-baseline', 'USD', 1.000000),
    ('2025-baseline', 'GBP', 0.790000),
    ('2025-baseline', 'EUR', 0.920000),
    ('2025-baseline', 'CAD', 1.360000),
    ('2025-baseline', 'AUD', 1.520000),
    ('2025-baseline', 'SGD', 1.350000),
    ('2025-baseline', 'JPY', 157.000000),
    ('2025-baseline', 'PLN', 4.000000),
    ('2025-baseline', 'BRL', 5.400000),
    ('2025-baseline', 'INR', 83.000000);
