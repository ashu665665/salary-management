import { EnumLabelPipe } from './enum-label-pipe';

describe('EnumLabelPipe', () => {
  const pipe = new EnumLabelPipe();

  it('turns a single word into a readable label', () => {
    expect(pipe.transform('ENGINEERING')).toBe('Engineering');
  });

  it('turns an underscored name into words', () => {
    expect(pipe.transform('UNITED_STATES')).toBe('United States');
    expect(pipe.transform('CUSTOMER_SUPPORT')).toBe('Customer Support');
  });

  it('leaves nothing behind for a missing value', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
