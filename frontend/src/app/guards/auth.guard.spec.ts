import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { authGuard } from './auth.guard';
import { signal } from '@angular/core';
import { AuthService } from '../services/auth/auth.service';

describe('authGuard', () => {
  let authServiceMock: any;
  let routerMock: any;
  // Create a signal to mock the return of getAuthenticatedSignal()
  const isAuthenticatedMock = signal(false);

  beforeEach(() => {
    authServiceMock = {
      getAuthenticatedSignal: jest.fn(() => isAuthenticatedMock)
    };
    routerMock = {
      navigate: jest.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    });
  });

  it('should allow navigation if authenticated', () => {
    isAuthenticatedMock.set(true); // Set state to authenticated
    
    // Use TestBed.runInInjectionContext to wrap the guard call
    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    
    expect(result).toBe(true);
  });

  it('should block navigation and redirect if not authenticated', () => {
    isAuthenticatedMock.set(false); // Set state to unauthenticated
    
    const result = TestBed.runInInjectionContext(() => authGuard({} as any, {} as any));
    
    expect(result).toBe(false);
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });
});


