import {
  Inject,
  inject,
  Injectable,
  OnDestroy,
  Signal,
  signal,
} from '@angular/core'
import {
  AuthConfig,
  OAuthErrorEvent,
  OAuthEvent,
  OAuthService,
} from 'angular-oauth2-oidc'
import { Router } from '@angular/router'
import LogsFormatter from '../../util/logs-formatter'
import { APP_CONFIG, AppConfig } from '../../app.config'
import { Subscription } from 'rxjs'
import { NGXLogger } from 'ngx-logger'

@Injectable({ providedIn: 'root' })
export class AuthService implements OnDestroy {
  private isAuthenticatedSignal = signal(false)

  private _oauthService = inject(OAuthService)
  private _router = inject(Router)
  private _logger = inject(NGXLogger)
  private _oauthEventSubscription: Subscription = Subscription.EMPTY

  constructor(@Inject(APP_CONFIG) private appConfig: AppConfig) {}

  ngOnDestroy(): void {
    this._oauthEventSubscription.unsubscribe()
  }

  async initAuth(): Promise<void> {
    this.configureOIDCClient()
    await this.startOIDCFlow()
    this.subscribeOAuthEvent()
  }

  login(url: string) {
    this._oauthService.initLoginFlow(url)
  }

  async logout() {
    this._oauthService.logOut()
    await this.handleIsNotAuthenticated()
  }

  getAuthenticatedSignal(): Signal<boolean> {
    return this.isAuthenticatedSignal.asReadonly()
  }

  get accessToken(): string | null {
    return this._oauthService.getAccessToken()
  }

  get userRoles(): string[] {
    const claims = this._oauthService.getIdentityClaims()
    if (!claims) return []
    return claims?.['groups'] || []
  }

  get name(): string | null {
    const claims = this._oauthService.getIdentityClaims()
    if (!claims) return null
    return claims['uid']
  }

  hasRole(role: string): boolean {
    return this.userRoles.includes(role)
  }

  private configureOIDCClient(): void {
    const authConfig: AuthConfig = {
      issuer: this.appConfig.oidc.issuer,
      clientId: this.appConfig.oidc.clientId,
      redirectUri: window.location.origin,
      responseType: 'code',
      scope: 'openid si_common',
      timeoutFactor: 0.75,
      showDebugInformation: this.appConfig.oidc.showDebug,
      postLogoutRedirectUri: window.location.origin,
    }

    this._oauthService.configure(authConfig)
  }

  private async startOIDCFlow(): Promise<void> {
    try {
      await this._oauthService.loadDiscoveryDocumentAndTryLogin()
      await this._oauthService.tryLoginCodeFlow()
      this.isAuthenticatedSignal.set(this._oauthService.hasValidAccessToken())
    } catch (error) {
      const errorMsg = LogsFormatter.formatErrorMsg({
        className: 'AuthService',
        fnName: 'startOIDCFlow',
        errorSrc: 'loadDiscoveryDocumentAndTryLogin or tryLoginCodeFlow',
        error,
      })
      this._logger.error(errorMsg)
      this.handleIsNotAuthenticated()
    }
  }

  subscribeOAuthEvent(): void {
    this._oauthEventSubscription = this._oauthService.events.subscribe(
      (event: OAuthEvent | OAuthErrorEvent) =>
        this.handleOAuthEvent(event).catch((navigateError: unknown) => {
          this._logger.error(navigateError)
        }),
    )
  }

  private async handleOAuthEvent(
    event: OAuthEvent | OAuthErrorEvent,
  ): Promise<boolean> {
    if (event instanceof OAuthErrorEvent) {
      return this.handleIsNotAuthenticated()
    }
    switch (event?.type) {
      case 'token_received':
        this.isAuthenticatedSignal.set(true)
        return this._router.navigateByUrl(this._oauthService.state as string)

      case 'token_expires':
        return this.handleIsNotAuthenticated()
      default:
        this._logger.warn('unknown event', event)
        return true
    }
  }

  private async handleIsNotAuthenticated(): Promise<boolean> {
    this.isAuthenticatedSignal.set(false)
    return this._router.navigateByUrl('/login')
  }
}
