import { Pipe, PipeTransform } from '@angular/core';

/**
 * Turns the API's enum values into something readable: UNITED_STATES becomes United States.
 *
 * <p>One pipe rather than the same string juggling in every template, so the screens all label
 * a country or a department the same way.
 */
@Pipe({ name: 'enumLabel' })
export class EnumLabelPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }
    return value
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  }
}
