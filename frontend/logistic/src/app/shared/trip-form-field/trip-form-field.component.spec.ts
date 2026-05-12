import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { TripFormFieldComponent } from './trip-form-field.component';

describe('TripFormFieldComponent', () => {
  let fixture: ComponentFixture<TripFormFieldComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripFormFieldComponent, ReactiveFormsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TripFormFieldComponent);
    fixture.componentRef.setInput('control', new FormControl('', Validators.required));
    fixture.componentRef.setInput('label', 'Test');
    fixture.componentRef.setInput('htmlFor', 'tf-test');
    fixture.componentRef.setInput('errorDomId', 'tf-test-err');
    fixture.componentRef.setInput('errorText', 'Required');
    fixture.detectChanges();
  });

  it('shows error with stable id when control is touched and invalid', () => {
    const ctrl = fixture.componentInstance.control();
    ctrl.markAsTouched();
    fixture.detectChanges();

    const host = fixture.nativeElement as HTMLElement;
    const err = host.querySelector('#tf-test-err');
    expect(err).not.toBeNull();
    expect(err?.textContent?.trim()).toBe('Required');
    expect(err?.getAttribute('role')).toBe('alert');
  });
});

describe('TripFormFieldComponent projection', () => {
  @Component({
    standalone: true,
    imports: [ReactiveFormsModule, TripFormFieldComponent],
    template: `
      <app-trip-form-field
        [control]="c"
        label="L"
        htmlFor="x"
        errorDomId="e"
      >
        <input id="x" [formControl]="c" />
        <div class="trip-field-actions"><span class="act">A</span></div>
      </app-trip-form-field>
    `,
  })
  class HostStub {
    c = new FormControl('');
  }

  it('projects default slot and .trip-field-actions', async () => {
    await TestBed.configureTestingModule({
      imports: [HostStub],
    }).compileComponents();
    const wrap = TestBed.createComponent(HostStub);
    wrap.detectChanges();
    const h = wrap.nativeElement as HTMLElement;
    expect(h.querySelector('input#x')).not.toBeNull();
    expect(h.querySelector('.trip-field-actions .act')?.textContent).toBe('A');
  });
});
