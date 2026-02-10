import { fakerDE as faker } from '@faker-js/faker'
import { DltEventOverviewItem } from '../../src/app/services/dlt-manager/model/DltEventOverviewItem'
import { DltEventFullItem } from '../../src/app/services/dlt-manager/model/DltEventFullItem'

export function generateFakeItem(
  isRandomAvailableActions?: boolean,
): DltEventOverviewItem {
  return {
    dltEventId: faker.string.nanoid(),
    originalEventId: faker.string.nanoid(),
    serviceName: faker.company.name(),
    addToDltTimestamp: faker.date.recent(),
    topic: faker.lorem.word(),
    partition: faker.string.numeric(),
    traceId: faker.string.uuid(),
    payloadMediaType: 'application/json',
    error: faker.lorem.sentence(),
    lastAdminAction: {
      username: faker.internet.username(),
      timestamp: faker.date.recent(),
      actionName: faker.helpers.arrayElement(['RETRY', 'DELETE']),
      actionDetails: faker.lorem.words(),
      status: faker.helpers.arrayElement(['SUCCESS', 'FAILURE']),
      statusError: faker.lorem.words(),
    },
    availableActions: isRandomAvailableActions
      ? faker.helpers.arrayElement([
          [
            { name: 'Retry', description: 'Retry event' },
            { name: 'Delete', description: 'Delete event' },
          ],
          [{ name: 'Delete', description: 'Delete event' }],
          [{ name: 'Retry', description: 'Retry event' }],
        ])
      : [
          { name: 'Retry', description: 'Retry event' },
          { name: 'Delete', description: 'Delete event' },
        ],
  }
}

export function generateTestDltEventDetails(options: {
  allFields?: boolean
  missingOptionalFields?: boolean
}): DltEventFullItem {
  return {
    ...generateFakeItem(),
    payload: JSON.stringify({
      message: faker.lorem.paragraph(),
      data: { value: faker.number.int() },
    }),
    stackTrace:
      options?.allFields || !options?.missingOptionalFields
        ? faker.lorem.paragraphs(3)
        : undefined,
  } as DltEventFullItem
}
