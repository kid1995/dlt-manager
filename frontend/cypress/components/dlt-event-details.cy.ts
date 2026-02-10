import { CommonModule, formatDate } from '@angular/common'
import { MatTooltipModule } from '@angular/material/tooltip'
import { provideRouter } from '@angular/router'
import { NGXLogger } from 'ngx-logger'
import { DltEventFullItemComponent } from '../../src/app/components/dlt-event-details/dlt-event-details.component'
import { DltManagerService } from '../../src/app/services/dlt-manager/dlt-manager.service'
import { DltEventFullItem } from '../../src/app/services/dlt-manager/model/DltEventFullItem'

import { SignalIdunaUiModule } from '@signal-iduna/ui-angular-proxy'
import ObjectUtils from '../../src/app/util/object-utils'
import { generateTestDltEventDetails } from '../helpers/fake-dlt-events'
import { routes } from '../../src/app/app.routes'
import { mount } from 'cypress/angular'
interface MockDltManagerServiceType {
  getDltEventFullItem: () => Promise<DltEventFullItem>
}

describe('DltEventFullItemComponent', () => {
  const SI_KEY_VALUE_COMPONENT_PREFIX = 'si-key-value-multitype'
  const SI_KEY_SLOT = 'div[slot="key"]'
  const SI_VALUE_SLOT = 'div[slot="value"]'
  const SI_EXPANDER_COMPONENT_PREFIX = 'si-expander'

  let mockDltService: MockDltManagerServiceType
  let testDltEventDetails: DltEventFullItem

  context('Test Dlt Event with all attributes', () => {
    beforeEach(() => {
      testDltEventDetails = generateTestDltEventDetails({
        allFields: true,
      })

      mockDltService = {
        getDltEventFullItem: () => Promise.resolve(testDltEventDetails),
      }

      mount(DltEventFullItemComponent, {
        imports: [CommonModule, MatTooltipModule, SignalIdunaUiModule],
        providers: [
          provideRouter(routes),
          { provide: DltManagerService, useValue: mockDltService },
          {
            provide: NGXLogger,
            useValue: { error: cy.stub(), debug: cy.stub() },
          },
        ],
      }).as('component')
    })

    it('should display all metadata fields with their values', () => {
      cy.get('@component').then((componentWrapper) => {
        const wrapper = componentWrapper as unknown as {
          component: DltEventFullItemComponent
        }
        const component = wrapper.component
        const metadataFieldsToTest = component.metadataFields
        metadataFieldsToTest.forEach(
          (field: { key: string; label: string; pipe?: string }) => {
            const expectedValue = ObjectUtils.extractFieldValue(
              testDltEventDetails,
              field.key,
            )
            cy.contains(
              `${SI_KEY_VALUE_COMPONENT_PREFIX} ${SI_KEY_SLOT}`,
              field.label,
            ).should('be.visible')
            cy.contains(
              `${SI_KEY_VALUE_COMPONENT_PREFIX} ${SI_KEY_SLOT}`,
              field.label,
            )
              .closest(SI_KEY_VALUE_COMPONENT_PREFIX)
              .find(SI_VALUE_SLOT)
              .should(
                'contain.text',
                field.pipe === 'date-short'
                  ? formatDate(expectedValue as Date, 'short', 'de-DE')
                  : expectedValue?.toString() || '',
              )
          },
        )
      })
    })

    it('should display all last admin action fields if admin action exists', () => {
      cy.get('@component').then((componentWrapper) => {
        const wrapper = componentWrapper as unknown as {
          component: DltEventFullItemComponent
        }
        const component = wrapper.component
        const lastAdminActionFieldsToTest = component.lastAdminActionFields
        if (testDltEventDetails.lastAdminAction) {
          lastAdminActionFieldsToTest.forEach(
            (field: { key: string; label: string }) => {
              const expectedValue = ObjectUtils.extractFieldValue(
                testDltEventDetails.lastAdminAction,
                field.key,
              )
              cy.contains(
                `${SI_KEY_VALUE_COMPONENT_PREFIX} ${SI_KEY_SLOT}`,
                field.label,
              ).should('be.visible')
              cy.contains(
                `${SI_KEY_VALUE_COMPONENT_PREFIX} ${SI_KEY_SLOT}`,
                field.label,
              )
                .closest(SI_KEY_VALUE_COMPONENT_PREFIX)
                .find(SI_VALUE_SLOT)
                .should('contain.text', expectedValue?.toString() || '')
            },
          )
        } else {
          cy.log(
            'Skipping admin action fields test as lastAdminAction is not present in mock data.',
          )
        }
      })
    })

    it('should display raw data in expander if available', () => {
      cy.get(
        `${SI_EXPANDER_COMPONENT_PREFIX}${getComponentAttributeTag('title', 'DLT Event (raw data)')}`,
      ).should('exist')
      cy.get(
        `${SI_EXPANDER_COMPONENT_PREFIX}${getComponentAttributeTag('title', 'DLT Event (raw data)')} pre`,
      ).should('contain.text', JSON.stringify(testDltEventDetails, null, 2))
    })

    it('should display the payload expander if available', () => {
      cy.get(
        `${SI_EXPANDER_COMPONENT_PREFIX}${getComponentAttributeTag('title', 'Payload')}`,
      ).should('exist')
      cy.get(
        `${SI_EXPANDER_COMPONENT_PREFIX}${getComponentAttributeTag('title', 'Payload')} pre`,
      ).should('contain.text', testDltEventDetails.payload)
    })

    it('should display the stack trace expander if available', () => {
      if (testDltEventDetails.stackTrace) {
        cy.get(
          `${SI_EXPANDER_COMPONENT_PREFIX}${getComponentAttributeTag('title', 'Stacktrace')}`,
        ).should('exist')
        cy.get(
          `${SI_EXPANDER_COMPONENT_PREFIX}${getComponentAttributeTag('title', 'Stacktrace')} pre`,
        ).should('contain.text', testDltEventDetails.stackTrace)
      } else {
        cy.get('si-expander[title="Stacktrace"]').should('not.exist')
      }
    })
  })
})

const getComponentAttributeTag = (attr: string, value: string) => {
  return `[${attr}="${value}"]`
}
