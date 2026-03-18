import { Component, inject } from '@angular/core'
import { Router, RouterOutlet } from '@angular/router'
import { AuthService } from './services/auth/auth.service'
import {
  SiHeaderNg,
  SiHeaderLogoNg,
  SiHeaderMenuItemNg,
  SiIconNg,
} from '@signal-iduna/ui-angular'

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    SiHeaderNg,
    SiHeaderLogoNg,
    SiHeaderMenuItemNg,
    SiIconNg,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  title = 'dltmanager-ui'
  private authService = inject(AuthService)
  private readonly isAuthenticatedSignal =
    this.authService.getAuthenticatedSignal()
  private router = inject(Router)

  isAuthenticated(): boolean {
    return this.isAuthenticatedSignal()
  }

  logout() {
    this.authService.logout()
    this.router.navigate(['/'])
  }
}
