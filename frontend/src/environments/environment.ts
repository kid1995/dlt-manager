import { AppConfig } from "../app/app.config"

//AppConfig for local development (used )
export const environment: AppConfig = {
  production: false,
  backendUrl: 'http://localhost:8080',
  oidc: {
    issuer: 'https://employee.login.int.signal-iduna.org/',
    clientId: '8d12476c2684592b12515daab4ca0ddb72007118-E',
    showDebug: true,
    identityConfig: true,
  }
}

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/plugins/zone-error';  // Included with Angular CLI.
