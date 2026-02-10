import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
} from '@angular/router'
import { AuthService } from '../services/auth/auth.service'
import { authGuard } from './auth.guard'
import { createEnvironmentInjector, runInInjectionContext, signal } from '@angular/core'
import { TestBed } from '@angular/core/testing'

describe('authGuard Fn Test', () => {
  let authService: jest.Mocked<AuthService>;
  let router: jest.Mocked<Router>;
  let activeRouterSnapShot: jest.Mocked<ActivatedRouteSnapshot>;
  let routerStateSnapShot: RouterStateSnapshot;
  const mockSignal = signal(true);
  let injector: ReturnType<typeof createEnvironmentInjector>;
  
  beforeEach(() => {
    authService = {
      getAuthenticatedSignal: jest.fn(),
    } as any
    router = {
      navigate: jest.fn(),
    } as any
    activeRouterSnapShot = { url: '' } as any
    routerStateSnapShot = { url: '' } as any
    injector = TestBed.configureTestingModule({
        providers: [
            {provide: AuthService, useValue: authService},
            {provide: Router, useValue: router},
        ]
    }) as any;
  })

  it('should allow navigation if authenticated', () => {
    mockSignal.set(true)
    authService.getAuthenticatedSignal.mockReturnValue(mockSignal.asReadonly())
    const result = runInInjectionContext(injector, () => {
        return authGuard(activeRouterSnapShot, routerStateSnapShot)
    });
    expect(result).toBe(true)
    expect(router.navigate).not.toHaveBeenCalled()
  })

  it('should block navigation if not authenticated', () => {
    mockSignal.set(false)
    authService.getAuthenticatedSignal.mockReturnValue(mockSignal.asReadonly())
    const result = runInInjectionContext(injector, () => {
        return authGuard(activeRouterSnapShot, routerStateSnapShot)
    });
    expect(result).toBe(false)
    expect(router.navigate).toHaveBeenCalledWith(['/login'])
  })
})
