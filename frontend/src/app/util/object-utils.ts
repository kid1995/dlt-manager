import { HttpErrorResponse } from "@angular/common/http"

export default class ObjectUtils {

  public static compare(a: unknown, b: unknown): number {
    if (a != null && b == null) {
      return 1
    }
    if (a == null && b != null) {
      return -1
    }

    if (a == null && b == null) {
      return 0
    }

    if (a instanceof Date && b instanceof Date) {
      return ObjectUtils.compareNumber(a.getTime(), b.getTime())
    }
    if (typeof a === 'string' && typeof b === 'string') {
      return a.localeCompare(b)
    }
    if (typeof a === 'number' && typeof b === 'number') {
      return ObjectUtils.compareNumber(a, b)
    }
    if (typeof a === 'boolean' && typeof b === 'boolean') {
      return ObjectUtils.compareBoolean(a, b)
    }
    return -1
  }

  private static compareBoolean(a: boolean, b: boolean) {
    if (a === b) {
      return 0
    }
    if (!a) {
      return -1
    }
    return 1
  }

  private static compareNumber(a: number, b: number): number {
    if (a < b) {
      return -1
    }
    if (a > b) {
      return 1
    }
    return 0
  }

  public static extractFieldValue(data: unknown, fieldKey: string): Date | string | number | undefined {
    if (fieldKey == null || fieldKey.length === 0 || (typeof data !== 'object')) {
      return undefined
    }

    const fieldKeys = fieldKey.split(".")
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let tmpObj: any = data
    while (tmpObj !== null && fieldKeys.length > 0) {
      const key = fieldKeys[0]
      fieldKeys.shift() // removes the first element
      tmpObj = tmpObj[key]
      if (fieldKeys.length > 0 && typeof tmpObj !== 'object') {
        return undefined
      }
    }
    return tmpObj
  }

  public static errorAsString(error: object): string {
    if(!error) {
      return 'unknown error';
    }
    if (typeof error === 'string') {
      return error
    }
    if (error instanceof HttpErrorResponse) {
      return `${error.status} ${error.statusText}: ${error.error}`
    }
    if (error instanceof Error) {
      return `${error.message} ${error.name} ${error.cause}`
    }
    return JSON.stringify(error)
  }
}
