import { CommonModule } from '@angular/common'
import {
  ChangeDetectorRef,
  Component,
  CUSTOM_ELEMENTS_SCHEMA,
  inject,
  OnInit,
} from '@angular/core'
import { MatTooltipModule } from '@angular/material/tooltip'
import { ActivatedRoute } from '@angular/router'
import '@signal-iduna/ui'
import { SignalIdunaUiModule } from '@signal-iduna/ui-angular-proxy'
import { NGXLogger } from 'ngx-logger'
import { DltManagerService } from '../../services/dlt-manager/dlt-manager.service'
import { DltEventFullItem } from '../../services/dlt-manager/model/DltEventFullItem'
import ObjectUtils from '../../util/object-utils'

@Component({
  selector: 'app-dlt-event-details',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  imports: [CommonModule, MatTooltipModule, SignalIdunaUiModule],
  templateUrl: './dlt-event-details.component.html',
  styleUrl: './dlt-event-details.component.scss',
})
export class DltEventFullItemComponent implements OnInit {
  private route: ActivatedRoute = inject(ActivatedRoute)
  public dltEvent?: DltEventFullItem
  public dltEventRawJson?: string

  dltEventId!: string

  readonly metadataFields: { key: string; label: string; pipe?: string }[] = [
    { key: 'dltEventId', label: 'DLT Event ID' },
    { key: 'originalEventId', label: 'Original Event ID' },
    { key: 'serviceName', label: 'Service' },
    { key: 'addToDltTimestamp', label: 'Time', pipe: "date-short" },
    { key: 'traceId', label: 'Trace ID' },
    { key: 'error', label: 'Error' },
  ]

  readonly lastAdminActionFields: { key: string; label: string }[] = [
    { key: 'actionName', label: 'Action type' },
    { key: 'actionDetails', label: 'Description' },
    { key: 'username', label: 'Triggered by user' },
    { key: 'timestamp', label: 'Date' },
    { key: 'status', label: 'Status' },
    { key: 'statusError', label: 'Status detail' },
  ]

  constructor(
    private readonly dltManagerService: DltManagerService,
    private readonly changeDetector: ChangeDetectorRef,
    private readonly logger: NGXLogger,
  ) {
    if (this.route) {
      this.dltEventId = this.route.snapshot.params['dltEventId']
    }
  }

  ngOnInit() {
    this.refresh()
  }

  refresh() {
    this.dltManagerService
      .getDltEventFullItem(this.dltEventId)
      .then((data: DltEventFullItem) => {
        this.logger.debug(
          `DltEventFullItemComponent::refresh() - got DltEventFullItem data for ${this.dltEventId}`,
        )
        this.dltEvent = data
        this.dltEventRawJson = JSON.stringify(this.dltEvent, null, 2)
      })
      .catch((error) =>
        this.logger.error(
          `DltEventFullItemComponent::refresh() - getDltEventFullItem(${this.dltEventId}) failed: ${ObjectUtils.errorAsString(error)}`,
        ),
      )
      .finally(() => this.changeDetector.markForCheck())
  }

  extractValue(obj: unknown, key: string): Date | string | number | undefined {
    return ObjectUtils.extractFieldValue(obj, key)
  }
}
