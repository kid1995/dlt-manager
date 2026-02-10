import { AdminActionHistoryItem } from "../model/AdminActionHistoryItem";
import { DltEventFullItem } from "../model/DltEventFullItem";
import { DltEventOverviewItem } from "../model/DltEventOverviewItem";

export class DltManagerModelConverter {
  public static convertRawDltEventOverviewItem(data: DltEventOverviewItem): DltEventOverviewItem {
    return {
      ...data,
      addToDltTimestamp: DltManagerModelConverter.asDate(data.addToDltTimestamp),
      lastAdminAction: DltManagerModelConverter.convertRawAdminActionHistoryItem(data.lastAdminAction)
    }
  }

  public static convertRawAdminActionHistoryItem(data?: AdminActionHistoryItem): AdminActionHistoryItem | undefined {
    if (data == null) {
      return undefined
    }
    return {
      ...data,
      timestamp: DltManagerModelConverter.asDate(data.timestamp)
    }
  }

  public static convertRawDltEventFullItem(data: DltEventFullItem): DltEventFullItem {
    if(data == null) {
      return data
    }
    let maybeFormattedPayload: string = data.payload
    try {
      const obj = JSON.parse(data.payload)
      maybeFormattedPayload = JSON.stringify(obj, null, 2)
    } finally { /* empty */ }
    return {
      ...data,
      addToDltTimestamp: DltManagerModelConverter.asDate(data.addToDltTimestamp),
      payload: maybeFormattedPayload,
      lastAdminAction: DltManagerModelConverter.convertRawAdminActionHistoryItem(data.lastAdminAction)
    }
  }

  private static asDate(maybeDate: unknown): Date {
    if (maybeDate instanceof Date) {
      return maybeDate
    }
    if (typeof maybeDate === 'string') {
      return new Date(maybeDate)
    }
    if (typeof maybeDate === 'number') {
      return new Date(maybeDate)
    }
    throw new Error(`failed to convert to Date: ${maybeDate}`)
  }

}