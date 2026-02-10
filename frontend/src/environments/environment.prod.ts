import { AppConfig } from "../app/app.config"

/**
 * This environment file is used for all environments. Environment specific values are set via a
 * replacement mechanism at runtime. When the `dltmanager-ui` Docker container is started,
 * placeholder values (like "NOT_SET_BACKEND_URL") are replaced by environment variables (see file Dockerfile).
 */
export const environment: AppConfig = {
  production: true,
  backendUrl: "NOT_SET_BACKEND_URL",
  oidc: {
    issuer: 'https://employee.login.int.signal-iduna.org/',
    clientId: '8d12476c2684592b12515daab4ca0ddb72007118-E',
    showDebug: true,
    identityConfig: true,
  }
}
