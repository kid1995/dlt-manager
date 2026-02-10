import { Routes } from '@angular/router'
import { LoginComponent } from './components/login/login.component'
import { authGuard } from './guards/auth.guard'

export const routes: Routes = [
    {
        path: '', redirectTo: '/dlt-event-overview', pathMatch: 'full'
    },
    {
        path: 'login', component: LoginComponent
    },
    {
        path: 'dlt-event-overview',
        loadComponent: () => import('./components/dlt-event-overview/dlt-event-overview.component').then(m =>m.DltEventOverviewComponent),
        canActivate:[authGuard]
    },
    {
        path: 'dlt-event-details/:dltEventId',
        loadComponent: () => import('./components/dlt-event-details/dlt-event-details.component').then(m =>m.DltEventFullItemComponent),
        canActivate:[authGuard]
    },
    // otherwise redirect to home
    { path: '**', redirectTo: '' }
]
