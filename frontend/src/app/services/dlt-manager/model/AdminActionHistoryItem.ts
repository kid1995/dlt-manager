export interface AdminActionHistoryItem {
	username: string,
	timestamp: Date,
	actionName: string,
	actionDetails: string,
	status: string,
	statusError?: string
}
