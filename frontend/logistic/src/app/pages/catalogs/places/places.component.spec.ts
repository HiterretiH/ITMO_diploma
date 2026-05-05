import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfirmationService } from 'primeng/api';
import { of } from 'rxjs';
import { CatalogApiService } from '../../../core/catalog-api.service';
import { PlacesComponent } from './places.component';

describe('PlacesComponent', () => {
  let fixture: ComponentFixture<PlacesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlacesComponent],
      providers: [
        provideNoopAnimations(),
        provideHttpClient(),
        provideHttpClientTesting(),
        ConfirmationService,
        {
          provide: CatalogApiService,
          useValue: {
            places: () => of([]),
            createPlace: () => of({ id: 1, address: 'A', contact: null, placeType: 'LOAD' }),
            updatePlace: () => of({ id: 1, address: 'A', contact: null, placeType: 'LOAD' }),
            deletePlace: () => of(void 0),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PlacesComponent);
  });

  it('creates and loads list', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance.items).toEqual([]);
  });
});
