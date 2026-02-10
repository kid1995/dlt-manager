import { CommonModule } from '@angular/common'
import { mount } from 'cypress/angular'
import { NGXLogger } from 'ngx-logger'
import {
  DltEventOverviewComponent,
  TableHeaderDef,
} from '../../src/app/components/dlt-event-overview/dlt-event-overview.component'
import { DltManagerService } from '../../src/app/services/dlt-manager/dlt-manager.service'
import { generateFakeItem } from '../helpers/fake-dlt-events'
import { SignalIdunaUiModule } from '@signal-iduna/ui-angular-proxy'
import { MatTooltipModule } from '@angular/material/tooltip'
import { provideRouter } from '@angular/router'
import ObjectUtils from '../../src/app/util/object-utils'
import { DltEventOverviewItem } from '../../src/app/services/dlt-manager/model/DltEventOverviewItem'
import TestObjectUtils from '../helpers/test-object-util'
import { TableCellSortOrder } from '@signal-iduna/ui'
import type { SinonStub } from 'sinon'

describe('DltEventOverviewComponent Test', () => {
  const NUMBER_OF_TEST_DATA = Cypress.env('numberOfTestData')
  const DEBUG_DELAY = Cypress.env('debugDelay')
  const testDataList = Array.from(
    { length: NUMBER_OF_TEST_DATA },
    generateFakeItem,
  )

  interface MockDltManagerServiceType {
    getDltEventOverviewItems: () => Promise<DltEventOverviewItem[]>
    retryDltEntry: SinonStub
    deleteDltEntry: SinonStub
  }

  let mockDltService: MockDltManagerServiceType

  beforeEach(() => {
    mockDltService = {
      getDltEventOverviewItems: () => Promise.resolve(testDataList),
      retryDltEntry: cy.stub().resolves() as SinonStub,
      deleteDltEntry: cy.stub().resolves() as SinonStub,
    }

    mount(DltEventOverviewComponent, {
      imports: [CommonModule, MatTooltipModule, SignalIdunaUiModule],
      providers: [
        provideRouter([]),
        { provide: DltManagerService, useValue: mockDltService }, // Use the mockDltService
        {
          provide: NGXLogger,
          useValue: { error: cy.stub(), debug: cy.stub() },
        }
      ],
    }).as('component')
  })

  it('should sort item base on column header, when column header was clicked', () => {
    cy.get('@component').then((dltEventOverviewWrapper) => {
      const wrapper = dltEventOverviewWrapper as unknown as {
        component: DltEventOverviewComponent
      }
      const component = wrapper.component
      const headerDefs = component.headerDefs.filter(
        (headerDef: TableHeaderDef) => headerDef.sortKey,
      )

      headerDefs.forEach((header, columnIndex) => {
        const headerLabel = header.label
        let currentSortOrder = header.sortOrder
        let testDirection: TableCellSortOrder = 'asc'
        cy.log(
          `Testing sort ${testDirection.toUpperCase()} for "${headerLabel}with ${testDirection}`
        )
        const numberOfClickTillAscSort = findNumberOfClick2DesireSortingState(
          currentSortOrder,
          testDirection,
        )
        for (let i = 0; i < numberOfClickTillAscSort; i++) {
          cy.get(`[data-cy="header-${headerLabel}"]`)
          .click(); 
          cy.wait(DEBUG_DELAY)
        }
        checkRenderedListAfterSort(header, columnIndex, testDataList, testDirection)
        currentSortOrder = testDirection as TableCellSortOrder
        testDirection = 'desc'
        cy.log(
          `Testing sort ${testDirection.toUpperCase()} for "${headerLabel}" with ${testDirection}`
        )
        const numberOfClickTillDescSort = findNumberOfClick2DesireSortingState(
          currentSortOrder,
          testDirection,
        )
        for (let i = 0; i < numberOfClickTillDescSort; i++) {
          cy.get(`[data-cy="header-${headerLabel}"]`).click()
          cy.wait(DEBUG_DELAY)
        }
        checkRenderedListAfterSort(header, columnIndex, testDataList, testDirection)
      })
    })
  })

  it('should trigger retry and delete methods via onTableRowActionClicked', () => {
    const { dltEventId } = testDataList[0]

    cy.get(`[data-cy="retry-dlt-${dltEventId}"]`).click()
    cy.wait(DEBUG_DELAY)
    cy.then(() => {
      expect(mockDltService.retryDltEntry).to.be.calledWith(dltEventId)
    })

    cy.get(`[data-cy="delete-dlt-${dltEventId}"]`).click()
    cy.wait(DEBUG_DELAY)
    cy.then(() => {
      expect(mockDltService.deleteDltEntry).to.be.calledWith(dltEventId)
    })
  })
})

const extractColumnTextFromRows = (
  rows: JQuery<HTMLElement>,
  columnIndex: number,
): string[] =>
  [...rows].map((row) =>
    row.querySelectorAll('si-table-cell')[columnIndex].textContent!.trim(),
  )

const findNumberOfClick2DesireSortingState = (
  currentOrder: TableCellSortOrder,
  desireOrder: TableCellSortOrder,
): number => {
  const sortStateList: TableCellSortOrder[] = [null, 'desc', 'asc']
  const currentIndex = sortStateList.indexOf(currentOrder)
  const desireOrderIndex = sortStateList.indexOf(desireOrder)
  return (
    (desireOrderIndex - currentIndex + sortStateList.length) %
    sortStateList.length
  )
}

const createExpectedSortedList = (
  inputList: readonly DltEventOverviewItem[],
  sortKey: string,
  direction: 'asc' | 'desc',
) => {
  return [...inputList]
    .sort((a, b) => {
      const aVal = ObjectUtils.extractFieldValue(a, sortKey)
      const bVal = ObjectUtils.extractFieldValue(b, sortKey)
      return (
        (direction === 'asc' ? 1 : -1) * TestObjectUtils.compare(aVal, bVal)
      )
    })
    .map((item) =>
      TestObjectUtils.format(ObjectUtils.extractFieldValue(item, sortKey)),
    )
}

const checkRenderedListAfterSort = (
  header: TableHeaderDef,
  columnIndex: number,
  testData: readonly DltEventOverviewItem[],
  direction: 'asc' | 'desc',
) => {
  const headerSortKey = header.sortKey as string
  // check result
  cy.get('si-table-body si-table-row').then((rows) => {
    const renderedValues = extractColumnTextFromRows(rows, columnIndex)

    const expectedSortedValues = createExpectedSortedList(
      testData,
      headerSortKey,
      direction,
    )
    expect(renderedValues).to.ordered.members(expectedSortedValues)
  })
}
