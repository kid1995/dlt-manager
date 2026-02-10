import { CommonModule } from '@angular/common'
import { ChangeDetectionStrategy, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core'
import { FormsModule, ReactiveFormsModule } from '@angular/forms'
import '@signal-iduna/ui'
import { SignalIdunaUiModule } from '@signal-iduna/ui-angular-proxy'
import { AuthService } from '../../services/auth/auth.service'

@Component({
  selector: 'app-login',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, SignalIdunaUiModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  
  constructor(private readonly authService: AuthService) {
  }

  onSubmit() {
    this.authService.login(window.location.search)
  }
}
