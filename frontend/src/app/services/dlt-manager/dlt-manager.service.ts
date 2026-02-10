import { Inject, Injectable } from '@angular/core'
import { HttpClient } from '@angular/common/http'
import { firstValueFrom } from 'rxjs/internal/firstValueFrom'
import { map } from 'rxjs/operators'
import { DltEventOverviewItem } from './model/DltEventOverviewItem'
import { GetDltEventsResponse } from './model/GetDltEventsResponse'
import { APP_CONFIG, AppConfig } from '../../app.config'
import { DltManagerModelConverter } from './converter/dlt-manager-model-converter'
import { DltEventFullItem } from './model/DltEventFullItem'


@Injectable({
  providedIn: 'root'
})
export class DltManagerService {
  private readonly apiUrl: string

  constructor(private http: HttpClient, @Inject(APP_CONFIG) private appConfig: AppConfig) {
    this.apiUrl = appConfig.backendUrl.replace(/\/$/, "")
  }

  public getDltEventOverviewItems(): Promise<readonly DltEventOverviewItem[]> {
    
    const url = `${this.apiUrl}/api/events/overview`
    return firstValueFrom(
      this.http.get<GetDltEventsResponse>(url)
        .pipe(map(x => x.dltEventItems.map(DltManagerModelConverter.convertRawDltEventOverviewItem)))
    )
  }

  public getDltEventOverviewItem(dltEventId: string): Promise<DltEventOverviewItem | null> {
    
    const url = `${this.apiUrl}/api/events/overview/${dltEventId}`
    return firstValueFrom(
      this.http.get<DltEventOverviewItem>(url)
        .pipe(map(item => DltManagerModelConverter.convertRawDltEventOverviewItem(item)))
    )
  }

  public getDltEventFullItem(dltEventId: string): Promise<DltEventFullItem> {
    
    return firstValueFrom(
      this.http.get<DltEventFullItem>(`${this.apiUrl}/api/events/details/${dltEventId}`)
        .pipe(
          map(response => DltManagerModelConverter.convertRawDltEventFullItem(response))
        )
    )
  }

  public retryDltEntry(dltEventId: string): Promise<void> {
    
    return firstValueFrom(
      this.http.post<void>(`${this.apiUrl}/api/events/re-processing/${dltEventId}`, null)
    )
  }

  public deleteDltEntry(dltEventId: string): Promise<void> {
    
    return firstValueFrom(
      this.http.delete<void>(`${this.apiUrl}/api/events/${dltEventId}`)
    )
  }

  
}
