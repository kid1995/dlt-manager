import { DltEventFullItem } from "../model/DltEventFullItem"
import { DltEventOverviewItem } from "../model/DltEventOverviewItem"
import { DltManagerModelConverter } from "./dlt-manager-model-converter"

describe('DltManagerService::convertRawDltEventOverviewItem()', () => {

    it('should convert data strings to Date', () => {

        const rawData: DltEventOverviewItem = {
            dltEventId: "dltEventId",
            originalEventId: "originalEventId",
            serviceName: "serviceName",
            addToDltTimestamp: "2024-09-04T12:26:00Z" as unknown as Date,
            topic: "topic",
            partition: "partition",
            traceId: "traceId",
            payloadMediaType: "payloadMediaType",
            error: "error",
            lastAdminAction: {
                username: "username",
                timestamp: "2024-10-04T12:26:00Z" as unknown as Date,
                actionName: "actionName",
                actionDetails: "actionDetails",
                status: "status",
                statusError: "statusError"
            },
            availableActions: [
                {
                    name: "name",
                    description: "description"
                }
            ]
        }
        const expectedResult: DltEventOverviewItem = {
            dltEventId: "dltEventId",
            originalEventId: "originalEventId",
            serviceName: "serviceName",
            addToDltTimestamp: new Date("2024-09-04T12:26:00Z"),
            topic: "topic",
            partition: "partition",
            traceId: "traceId",
            payloadMediaType: "payloadMediaType",
            error: "error",
            lastAdminAction: {
                username: "username",
                timestamp: new Date("2024-10-04T12:26:00Z"),
                actionName: "actionName",
                actionDetails: "actionDetails",
                status: "status",
                statusError: "statusError"
            },
            availableActions: [
                {
                    name: "name",
                    description: "description"
                }
            ]
        }

        expect(DltManagerModelConverter.convertRawDltEventOverviewItem(rawData)).toEqual(expectedResult)
        expect(rawData.addToDltTimestamp as unknown).toEqual("2024-09-04T12:26:00Z")
        expect(rawData.lastAdminAction?.timestamp as unknown).toEqual("2024-10-04T12:26:00Z")
    })

})

describe('DltManagerService::convertRawDltEventFullItem()', () => {

    it('should convert data strings to Date', () => {
        const payloadObject = {
            key1: "value1",
            key2: "value2"
        }
        const payloadObjectFlatJsonString = JSON.stringify(payloadObject)

        const rawData: DltEventFullItem = {
            dltEventId: "dltEventId",
            originalEventId: "originalEventId",
            serviceName: "serviceName",
            addToDltTimestamp: "2024-09-04T12:26:00Z" as unknown as Date,
            topic: "topic",
            partition: "partition",
            traceId: "traceId",
            payload: payloadObjectFlatJsonString,
            payloadMediaType: "payloadMediaType",
            error: "error",
            stackTrace: "stackTrace",
            lastAdminAction: {
                username: "username",
                timestamp: new Date("2024-10-04T12:26:00Z"),
                actionName: "actionName",
                actionDetails: "actionDetails",
                status: "status",
                statusError: "statusError"
            },
            availableActions: [
                {
                    name: "name",
                    description: "description"
                }
            ]
        }

        const expectedResult: DltEventFullItem = {
            ...rawData,
            addToDltTimestamp: new Date("2024-09-04T12:26:00Z"),
            payload: JSON.stringify(payloadObject, null, 2), //pretty printed JSON representation
            lastAdminAction: {
                username: "username",
                timestamp: new Date("2024-10-04T12:26:00Z"),
                actionName: "actionName",
                actionDetails: "actionDetails",
                status: "status",
                statusError: "statusError"
            }
        }

        expect(DltManagerModelConverter.convertRawDltEventFullItem(rawData)).toEqual(expectedResult)
        expect(rawData.payload).toEqual(payloadObjectFlatJsonString)
    })
})