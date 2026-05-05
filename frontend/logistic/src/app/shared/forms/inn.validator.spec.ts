import { FormControl } from '@angular/forms';
import { innValidator } from './inn.validator';

describe('innValidator', () => {
  it('allows empty', () => {
    expect(innValidator(new FormControl(''))).toBeNull();
    expect(innValidator(new FormControl('  '))).toBeNull();
  });

  it('allows 10 or 12 digits', () => {
    expect(innValidator(new FormControl('1234567890'))).toBeNull();
    expect(innValidator(new FormControl('123456789012'))).toBeNull();
  });

  it('rejects invalid length', () => {
    expect(innValidator(new FormControl('123'))).toEqual({ inn: true });
  });
});
