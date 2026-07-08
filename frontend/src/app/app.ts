import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Sidebar } from './shared/sidebar/sidebar';
import { ToastHost } from './shared/toast/toast-host';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Sidebar, ToastHost],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
