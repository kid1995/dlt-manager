import { DltEventOverviewItem } from "./DltEventOverviewItem";

export interface DltEventFullItem extends DltEventOverviewItem {
	payload: string,
	stackTrace?: string,
}
