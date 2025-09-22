import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FriendsSearchComponent } from './friends-search.component';

describe('FriendsSearch', () => {
  let component: FriendsSearchComponent;
  let fixture: ComponentFixture<FriendsSearchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FriendsSearchComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FriendsSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
