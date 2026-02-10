import { OAuthErrorEvent, OAuthEvent, OAuthService } from 'angular-oauth2-oidc'
import { AuthService } from './auth.service'
import { Subject } from 'rxjs'
import { TestBed } from '@angular/core/testing'
import { APP_CONFIG } from '../../app.config'
import { NGXLogger } from 'ngx-logger'

describe('AuthService Test', () => {
  let authService: AuthService;
  let oauthServiceMock: jest.Mocked<OAuthService>;
  const ngxLogger = jest.Mocked<NGXLogger>;
  let mockEventsSubject = new Subject<OAuthEvent>();
  const mockConfig: any = {    
    oidc: {
      issuer: 'test-link',
      clientId: 'test-client-id',
      showDebug: false,
      identityConfig: false,
    }
  }
  const createMockOauthServiceMock = () => {
    return {
      configure: jest.fn(),
      loadDiscoveryDocumentAndTryLogin: jest.fn().mockResolvedValue(undefined),
      tryLoginCodeFlow: jest.fn().mockResolvedValue(undefined),
      hasValidAccessToken: jest.fn().mockResolvedValue(undefined),
      initLoginFlow: jest.fn(),
      logOut: jest.fn(),
      getAccessToken: jest.fn().mockReturnValue('mock-token'),
      getIdentityClaims: jest.fn().mockReturnValue({}),
      events: mockEventsSubject.asObservable(),
    } as any
  }

  beforeAll(() => {
    oauthServiceMock = createMockOauthServiceMock()

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        { provide: OAuthService, useValue: oauthServiceMock },
        { provide: NGXLogger, useValue: ngxLogger},
        { provide: APP_CONFIG, useValue: mockConfig },
      ],
    })

    authService = TestBed.inject(AuthService);
    
  })

  beforeEach(() => {
    if(!mockEventsSubject.closed) mockEventsSubject.unsubscribe();
    mockEventsSubject = new Subject<OAuthEvent>();
    oauthServiceMock.events = mockEventsSubject.asObservable();
    authService.subscribeOAuthEvent()
    
  });

  afterEach(() => {
    if(!mockEventsSubject.closed) mockEventsSubject.unsubscribe();
  })

  
  it('should invoke all needed oidc flow config ', async () => {
    await authService.initAuth()
    expect(oauthServiceMock.configure).toHaveBeenCalled();
    expect(oauthServiceMock.loadDiscoveryDocumentAndTryLogin).toHaveBeenCalled();
    expect(oauthServiceMock.tryLoginCodeFlow).toHaveBeenCalled();
  });

  it('should set authenticated when receive token_received event', async () => {
    mockEventsSubject.next({ type: 'token_received' } as OAuthEvent)
    const isAuthenticatedSignal = authService.getAuthenticatedSignal()
    expect(isAuthenticatedSignal()).toBe(true)
  });

  it('should not authenticate when token expired', async () => {
    mockEventsSubject.next({ type: 'token_expires' } as OAuthEvent)
    const isAuthenticatedSignal = authService.getAuthenticatedSignal()
    expect(isAuthenticatedSignal()).toBe(false)
  });

  it('should not authenticate when receive error event', async () => {
    mockEventsSubject.next(new OAuthErrorEvent('jwks_load_error', {}))
    const isAuthenticatedSignal = authService.getAuthenticatedSignal()
    expect(isAuthenticatedSignal()).toBe(false)
  });
});
