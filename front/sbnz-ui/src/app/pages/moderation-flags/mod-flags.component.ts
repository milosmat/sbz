import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { AdminService, FlagDTO } from '../../core/admin.service';

@Component({
  standalone: true,
  selector: 'app-mod-flags',
  imports: [CommonModule, MatTableModule, DatePipe],
  templateUrl: './mod-flags.component.html',
  styleUrls: ['./mod-flags.component.css']
})
export class ModFlagsComponent implements OnInit {
  private api = inject(AdminService);
  rows: FlagDTO[] = [];
  cols = ['user','reason','until'];

  ngOnInit() {
    this.api.getFlags().subscribe(r => this.rows = r);
  }
}
