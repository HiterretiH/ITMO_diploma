import {
  Component,
  input,
} from '@angular/core';
import { AbstractControl } from '@angular/forms';

@Component({
  selector: 'app-trip-form-field',
  standalone: true,
  imports: [],
  templateUrl: './trip-form-field.component.html',
  styleUrl: './trip-form-field.component.css',
})
export class TripFormFieldComponent {
  readonly control = input.required<AbstractControl>();
  readonly label = input.required<string>();
  readonly htmlFor = input.required<string>();
  /** Stable DOM id for the error `<small>` (use with `[attr.aria-describedby]` on the control). */
  readonly errorDomId = input.required<string>();
  /** Visible validation message when `control` is touched and invalid. */
  readonly errorText = input<string>('Обязательное поле');

  showError(): boolean {
    const c = this.control();
    return c.touched && c.invalid;
  }
}
