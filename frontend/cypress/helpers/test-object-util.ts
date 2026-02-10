export default class TestObjectUtils {
  static compare = (a: unknown, b: unknown): number => {
    if (a == null && b == null) return 0
    if (a == null) return -1
    if (b == null) return 1
    if (a instanceof Date && b instanceof Date) return a.getTime() - b.getTime()
    return a.toString().localeCompare(b.toString())
  }

  static format = (value: unknown): string => {
    return value instanceof Date ? new Intl.DateTimeFormat('en-US', {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(value) : value?.toString() ?? '';
  }
}
