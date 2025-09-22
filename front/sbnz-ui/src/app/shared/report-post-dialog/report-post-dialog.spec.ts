import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportPostDialog } from './report-post-dialog';

describe('ReportPostDialog', () => {
  let component: ReportPostDialog;
  let fixture: ComponentFixture<ReportPostDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportPostDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReportPostDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
