import { inject } from '@angular/core'
import {  CanActivateFn, Router } from '@angular/router'
import { AuthService } from '../services/auth/auth.service'


export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject( Router);
  const isAuthenticatedSignal = auth.getAuthenticatedSignal();
  const isAuthenticated = isAuthenticatedSignal();
  
  if(!isAuthenticated){
    router.navigate(['/login']);
    return false;
  }
  return true;
}

