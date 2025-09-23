import { Component, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import {AdminService, FlagDTO} from '../../core/admin.service';
import { Flagged } from '../../core/models/moderation';
import {MatTableModule} from '@angular/material/table';

@Component({
  standalone: true,
  selector: 'app-admin-moderation',
  imports: [CommonModule, MatTableModule, DatePipe],
  templateUrl: './admin-moderation.component.html',
  styleUrls: ['./admin-moderation.component.css']
})
export class AdminModerationComponent {
  private api = inject(AdminService);
  rows: FlagDTO[] = [];
  cols = ['user','reason','until'];

  ngOnInit() {
    this.api.getFlags().subscribe(r => this.rows = r);
  }
}
