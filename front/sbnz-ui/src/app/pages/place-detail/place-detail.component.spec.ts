import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlaceDetailComponent } from './place-detail.component';

describe('PlaceDetail', () => {
  let component: PlaceDetailComponent;
  let fixture: ComponentFixture<PlaceDetailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlaceDetailComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PlaceDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
