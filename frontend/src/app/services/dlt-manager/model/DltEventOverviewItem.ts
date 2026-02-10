import { AdminActionHistoryItem } from "./AdminActionHistoryItem"
import { DltEventAction } from "./DltEventAction"

export interface DltEventOverviewItem {
	dltEventId: string,
	originalEventId: string,
	serviceName: string,
	addToDltTimestamp: Date,
	topic: string,
	partition: string,
	traceId?: string,
	payloadMediaType: string,
	error: string,
	lastAdminAction?: AdminActionHistoryItem,
	availableActions: readonly DltEventAction[]
}
