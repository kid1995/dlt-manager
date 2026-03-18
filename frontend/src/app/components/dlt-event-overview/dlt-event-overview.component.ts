import { CommonModule } from '@angular/common'
import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  inject,
  OnInit,
} from '@angular/core'
import { MatTooltipModule } from '@angular/material/tooltip'
import {
  SiTableCellSortOrder,
  TableCellSortEventBody,
} from '@signal-iduna/ui-angular'
import {
  SiButtonNg,
  SiIconNg,
  SiTableNg,
  SiTableHeaderNg,
  SiTableBodyNg,
  SiTableRowNg,
  SiTableCellNg,
} from '@signal-iduna/ui-angular'
import { NGXLogger } from 'ngx-logger'
import { DltManagerService } from '../../services/dlt-manager/dlt-manager.service'
import { DltEventOverviewItem } from '../../services/dlt-manager/model/DltEventOverviewItem'
import ObjectUtils from '../../util/object-utils'
import { Router } from '@angular/router'

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    MatTooltipModule,
    SiButtonNg,
    SiIconNg,
    SiTableNg,
    SiTableHeaderNg,
    SiTableBodyNg,
    SiTableRowNg,
    SiTableCellNg,
  ],
  templateUrl: './dlt-event-overview.component.html',
  styleUrl: './dlt-event-overview.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DltEventOverviewComponent implements OnInit {
  private readonly router = inject(Router)
  dltEventItems: readonly DltEventOverviewItem[] = []
  errorMessage?: string

  private readonly dltManagerService = inject(DltManagerService)
  private readonly changeDetector = inject(ChangeDetectorRef)
  private readonly logger = inject(NGXLogger)

  public headerDefs: readonly TableHeaderDef[] = [
    {
      label: 'Date',
      sortKey: 'addToDltTimestamp',
      sortOrder: 'descending',
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

  private readonly sortCycleMap: Map<
    SiTableCellSortOrder,
    SiTableCellSortOrder
  > = new Map<SiTableCellSortOrder, SiTableCellSortOrder>([
    [null, 'ascending'],
    ['ascending', 'descending'],
    ['descending', null],
  ])

  public onSort(event: TableCellSortEventBody<Record<string, unknown>>) {
    // Angular proxy types this as TableCellSortEventBody but at runtime
    // it's a CustomEvent with data in event.detail
    const rawEvent = event as unknown as CustomEvent<
      TableCellSortEventBody<Record<string, unknown>>
    >
    const sortKey: string = rawEvent.detail.sortKey as string

    const heading: TableHeaderDef | undefined = this.headerDefs.find(
      (heading) => heading?.sortKey === sortKey,
    )
    if (!heading) return

    const newSortOrder: SiTableCellSortOrder =
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
    sortOrder: SiTableCellSortOrder,
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
        return sortOrder === 'descending' ? -compareResult : compareResult
      },
    )
    return sortedItems
  }
}

export interface TableHeaderDef {
  label: string
  sortKey?: string
  sortOrder: SiTableCellSortOrder
}
