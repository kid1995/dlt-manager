import { CommonModule } from '@angular/common'
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  CUSTOM_ELEMENTS_SCHEMA, inject,
  OnInit,
} from '@angular/core'
import { MatTooltipModule } from '@angular/material/tooltip'
import '@signal-iduna/ui'
import { TableCellSortOrder } from '@signal-iduna/ui'
import { SignalIdunaUiModule } from '@signal-iduna/ui-angular-proxy'
import { NGXLogger } from 'ngx-logger'
import { DltManagerService } from '../../services/dlt-manager/dlt-manager.service'
import { DltEventOverviewItem } from '../../services/dlt-manager/model/DltEventOverviewItem'
import ObjectUtils from '../../util/object-utils'
import {Router} from "@angular/router";

@Component({
  selector: 'app-admin',
  standalone: true,
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  imports: [CommonModule, MatTooltipModule, SignalIdunaUiModule],
  templateUrl: './dlt-event-overview.component.html',
  styleUrl: './dlt-event-overview.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DltEventOverviewComponent implements OnInit {
  private readonly router = inject(Router);
  dltEventItems: readonly DltEventOverviewItem[] = []
  errorMessage?: string

  constructor(
    private readonly dltManagerService: DltManagerService,
    private readonly changeDetector: ChangeDetectorRef,
    private readonly logger: NGXLogger,
  ) {}

  public headerDefs: readonly TableHeaderDef[] = [
    {
      label: 'Date',
      sortKey: 'addToDltTimestamp',
      sortOrder: 'desc',
    },
    {
      label: 'Service',
      sortKey: 'serviceName',
      sortOrder: null,
    },
    {
      label: 'DLT Event ID',
      sortKey: 'originalEventId',
      sortOrder: null,
    },
    {
      label: 'Message',
      sortKey: 'error',
      sortOrder: null,
    },
    {
      label: 'Last action',
      sortKey: 'lastAdminAction.actionName',
      sortOrder: null,
    },
    {
      label: 'Last action date',
      sortKey: 'lastAdminAction.timestamp',
      sortOrder: null,
    },
    {
      label: 'Last action status',
      sortKey: 'lastAdminAction.status',
      sortOrder: null,
    },
    {
      label: 'Action',
      sortKey: null,
      sortOrder: null,
    },
  ]

  ngOnInit() {
    this.loadData()
  }

  loadData() {
    this.errorMessage = undefined
    this.dltManagerService
      .getDltEventOverviewItems()
      .then((data) => {
        const sortByColumn = this.headerDefs.find(
          (hd) => hd.sortKey != null && hd.sortOrder != null,
        )
        if (sortByColumn != null) {
          this.dltEventItems = DltEventOverviewComponent.sortRowData(
            data,
            sortByColumn.sortKey!,
            sortByColumn.sortOrder,
          )
        } else {
          this.dltEventItems = data
        }
      })
      .catch((error) => {
        this.errorMessage = ObjectUtils.errorAsString(error)
        this.logger.error(
          `DltEventOverviewComponent::refresh() failed: ${this.errorMessage}`,
        )
      })
      .finally(() => this.changeDetector.markForCheck())
  }

  public onTableRowActionClicked(action: string, dltEventId: string) {
    this.logger.debug(
      `DltEventOverviewComponent::onTableRowActionClicked(action:${action}, dltEventId:${dltEventId})`,
    )
    switch (action) {
      case 'Retry':
        this.retryDltEntry(dltEventId)
        break
      case 'Delete':
        this.deleteDltEntry(dltEventId)
        break
      default:
        this.logger.error(
          `DltEventOverviewComponent::onTableRowActionClicked() - unexpected action: '${action}'`,
        )
        break
    }
  }

  retryDltEntry(dltEventId: string) {
    this.logger.debug(`DltEventOverviewComponent::retryDltEntry()`)
    this.errorMessage = undefined
    this.dltManagerService
      .retryDltEntry(dltEventId)
      .then(() => {
        this.loadData()
      })
      .catch((error) => {
        this.logger.error(
          `DltEventOverviewComponent::retryDltEntry() failed: ${ObjectUtils.errorAsString(error)}`,
        )
        this.errorMessage = ObjectUtils.errorAsString(error)
        this.changeDetector.markForCheck()
      })
  }

  deleteDltEntry(dltEventId: string) {
    this.logger.debug(`DltEventOverviewComponent::deleteDltEntry()`)
    this.errorMessage = undefined
    this.dltManagerService
      .deleteDltEntry(dltEventId)
      .then(() => {
        this.loadData()
      })
      .catch((error) => {
        this.logger.error(
          `DltEventOverviewComponent::deleteDltEntry() failed: ${ObjectUtils.errorAsString(error)}`,
        )
        this.errorMessage = ObjectUtils.errorAsString(error)
        this.changeDetector.markForCheck()
      })
      .finally(() => this.changeDetector.markForCheck())
  }

  navigateToDetail(dltEventId: string): Promise<boolean> {
    return this.router.navigate(['/', 'dlt-event-details', dltEventId])
  }

  private readonly sortCycleMap: Map<string | null, TableCellSortOrder> =
    new Map<string | null, TableCellSortOrder>([
      [null, 'asc'],
      ['asc', 'desc'],
      ['desc', null],
    ])

  public onSort(event: CustomEvent) {
    const sortKey: string = event.detail.sortKey

    const heading: TableHeaderDef | undefined = this.headerDefs.find(
      (heading) => heading?.sortKey === sortKey,
    )
    if (!heading) return

    const newSortOrder: TableCellSortOrder =
      this.sortCycleMap.get(heading.sortOrder) ?? null

    // Set the new sortKey & reset other headerDefs
    this.headerDefs = this.headerDefs.map((headerDef) => {
      const updatedHeaderDef: TableHeaderDef = { ...headerDef }
      if (headerDef.sortKey !== sortKey) {
        updatedHeaderDef.sortOrder = null
      } else {
        updatedHeaderDef.sortOrder = newSortOrder
      }
      return updatedHeaderDef
    })

    // Sort the data
    this.dltEventItems = DltEventOverviewComponent.sortRowData(
      this.dltEventItems,
      sortKey,
      newSortOrder,
    )
  }

  private static sortRowData(
    toSort: readonly DltEventOverviewItem[],
    sortKey: string,
    sortOrder: TableCellSortOrder,
  ): readonly DltEventOverviewItem[] {
    const sortedItems = toSort.slice()
    sortedItems.sort(
      (itemA: DltEventOverviewItem, itemB: DltEventOverviewItem) => {
        const a: unknown = ObjectUtils.extractFieldValue(
          itemA as unknown,
          sortKey,
        )
        const b: unknown = ObjectUtils.extractFieldValue(
          itemB as unknown,
          sortKey,
        )
        const compareResult: number = ObjectUtils.compare(a, b)
        return sortOrder === 'desc' ? -compareResult : compareResult
      },
    )
    return sortedItems
  }
}

export interface TableHeaderDef {
  label: string
  sortKey?: string | null
  sortOrder: TableCellSortOrder
}
