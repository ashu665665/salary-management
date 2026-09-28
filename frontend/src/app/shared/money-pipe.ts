import { Pipe, PipeTransform } from '@angular/core';
import { MoneyView } from '../core/models/employee.model';

/**
 * Renders an amount with its currency: INR 1,925,000.
 *
 * <p>The code goes in front rather than a symbol, because the same screen shows ten currencies and
 * a bare symbol would leave it unclear which one a figure is in. Whole units only: nobody reads a
 * salary to the cent.
 */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  private readonly format = new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 });

  transform(money: MoneyView | null | undefined): string {
    if (!money) {
      return '';
    }
    return `${money.currency} ${this.format.format(money.amount)}`;
  }
}
