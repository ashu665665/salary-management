import { MoneyPipe } from './money-pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('puts the currency code in front of the amount, with room to breathe', () => {
    expect(pipe.transform({ amount: 76500, currency: 'USD' })).toBe('USD 76,500');
  });

  it('groups thousands so a large salary can be read at a glance', () => {
    expect(pipe.transform({ amount: 1925000, currency: 'INR' })).toBe('INR 1,925,000');
  });

  it('shows whole units, because nobody reads salaries to the cent', () => {
    expect(pipe.transform({ amount: 76500.49, currency: 'USD' })).toBe('USD 76,500');
  });

  it('shows nothing when there is no salary on record', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
