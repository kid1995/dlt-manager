import { Component, CUSTOM_ELEMENTS_SCHEMA, inject } from '@angular/core'
import { Router, RouterOutlet } from '@angular/router'
import '@signal-iduna/ui'
import { AuthService } from './services/auth/auth.service'
import { SignalIdunaUiModule } from "@signal-iduna/ui-angular-proxy"

@Component({
  selector: 'app-root',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  imports: [RouterOutlet,SignalIdunaUiModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'dltmanager-ui'
  private authService = inject(AuthService);
  private readonly isAuthenticatedSignal = this.authService.getAuthenticatedSignal()
  private router = inject(Router);

  isAuthenticated(): boolean {
    return this.isAuthenticatedSignal();
  }

  logout() {
    this.authService.logout()
    this.router.navigate(['/'])
  }
}
