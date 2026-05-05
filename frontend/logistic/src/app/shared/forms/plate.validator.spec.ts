import { FormControl } from '@angular/forms';
import { plateValidator } from './plate.validator';

describe('plateValidator', () => {
  it('allows empty (use Validators.required separately)', () => {
    expect(plateValidator(new FormControl(''))).toBeNull();
  });

  it('allows typical plate without spaces', () => {
    expect(plateValidator(new FormControl('A123BC77'))).toBeNull();
  });

  it('normalizes spaces', () => {
    expect(plateValidator(new FormControl('A 123 BC 77'))).toBeNull();
  });

  it('rejects too short', () => {
    expect(plateValidator(new FormControl('AB1'))).toEqual({ plate: true });
  });
});
