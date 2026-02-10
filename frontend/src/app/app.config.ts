import { registerLocaleData } from '@angular/common'
import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http'
import localeDe from '@angular/common/locales/de'
import {
  APP_INITIALIZER,
  ApplicationConfig,
  importProvidersFrom,
  inject,
  InjectionToken,
  LOCALE_ID,
  provideZoneChangeDetection,
} from '@angular/core'
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async'
import { provideRouter } from '@angular/router'
import { LoggerModule, NgxLoggerLevel } from 'ngx-logger'
import { environment } from '../environments/environment'
import { routes } from './app.routes'
import { DefaultOAuthInterceptor, OAuthModuleConfig, provideOAuthClient } from 'angular-oauth2-oidc'
import { AuthService } from './services/auth/auth.service'

registerLocaleData(localeDe)

export interface OidcConfig {
  issuer: string
  clientId: string
  showDebug: boolean
  identityConfig: boolean
}

export interface AppConfig {
  production: boolean
  backendUrl: string
  oidc: OidcConfig
}

export const APP_CONFIG = new InjectionToken<AppConfig>('app.config')
const BACKEND_URL = environment.backendUrl.replace(/\/$/, "");
const AUTH_CONFIG: OAuthModuleConfig ={
  resourceServer: {
    allowedUrls:[BACKEND_URL],
    sendAccessToken: true
  }
}

export const appConfig: ApplicationConfig = {
  providers: [
    {
      provide: APP_CONFIG,
      useValue: environment,
    },
    provideOAuthClient(AUTH_CONFIG),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: DefaultOAuthInterceptor, multi: true },
    AuthService,
    {
      provide: APP_INITIALIZER,
      multi: true,
      useFactory: () => {
        const authService = inject(AuthService);
        return () => authService.initAuth();
      }
    },
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    importProvidersFrom(LoggerModule.forRoot({ level: NgxLoggerLevel.DEBUG })),
    provideAnimationsAsync('noop'),
    { provide: LOCALE_ID, useValue: 'de-DE' }
    
  ],
}
