import { Component } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  barWidths(): number[] {
    throw new Error('not implemented yet');
  }

  onDimensionChange(groupBy: string): void {
    throw new Error('not implemented yet');
  }
}
