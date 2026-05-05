import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { CatalogApiService } from '../../core/catalog-api.service';
import { PlaceInputComponent } from './place-input.component';

describe('PlaceInputComponent', () => {
  let fixture: ComponentFixture<PlaceInputComponent>;
  let catalog: { places: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    catalog = {
      places: vi.fn().mockReturnValue(
        of([
          {
            id: 1,
            address: 'Склад 1',
            contact: '+7999',
            placeType: 'LOAD' as const,
          },
        ]),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [PlaceInputComponent, ReactiveFormsModule],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        provideHttpClientTesting(),
        FormBuilder,
        { provide: CatalogApiService, useValue: catalog },
      ],
    }).compileComponents();

    const fb = TestBed.inject(FormBuilder);
    const form = fb.nonNullable.group({
      originAddress: [''],
      originContact: [''],
    });

    fixture = TestBed.createComponent(PlaceInputComponent);
    const cmp = fixture.componentInstance;
    cmp.form = form;
    cmp.addressKey = 'originAddress';
    cmp.contactKey = 'originContact';
    cmp.placeType = 'LOAD';
  });

  it('fills contact when selecting a saved place', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.onSelect({
      originalEvent: new Event('x'),
      value: {
        id: 1,
        address: 'Склад 1',
        contact: '+7111',
        placeType: 'LOAD',
      },
    });
    expect(cmp.form.get('originContact')?.value).toBe('+7111');
    expect(cmp.lastSelectedPlace?.id).toBe(1);
  });

  it('requests places with type filter on complete', () => {
    fixture.detectChanges();
    const cmp = fixture.componentInstance;
    cmp.complete({ originalEvent: new Event('input'), query: 'ск' });
    expect(catalog.places).toHaveBeenCalledWith('LOAD', 'ск');
  });
});
