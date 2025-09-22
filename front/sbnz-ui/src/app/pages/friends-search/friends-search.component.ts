import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, switchMap } from 'rxjs/operators';
import { FriendService } from '../../core/friend.service';
import { MatListModule } from '@angular/material/list';
import { MatButtonModule } from '@angular/material/button';

@Component({
  standalone: true,
  selector: 'app-friends-search',
  imports: [CommonModule, ReactiveFormsModule, MatListModule, MatButtonModule],
  templateUrl: './friends-search.component.html',
  styleUrls: ['./friends-search.component.css']
})
export class FriendsSearchComponent {
  q = new FormControl('');
  results: any[] = [];

  constructor(private friends: FriendService) {
    this.q.valueChanges.pipe(
      debounceTime(250),
      switchMap(v => this.friends.search(v ?? ''))
    ).subscribe(res => this.results = res);
  }

  add(id: string)   { this.friends.add(id).subscribe(); }
  block(id: string) { this.friends.block(id).subscribe(); }
}
