import ObjectUtils from "./object-utils"


describe('ObjectUtils::compare()', () => {

    it('should work for numbers', () => {
        expect(ObjectUtils.compare(1, 2)).toBe(-1)
        expect(ObjectUtils.compare(1, 1)).toBe(0)
        expect(ObjectUtils.compare(2, 2)).toBe(0)
        expect(ObjectUtils.compare(2, 1)).toBe(1)

        expect(ObjectUtils.compare(null, 1)).toBe(-1)
        expect(ObjectUtils.compare(undefined, 1)).toBe(-1)
        expect(ObjectUtils.compare(1, null)).toBe(1)
        expect(ObjectUtils.compare(1, undefined)).toBe(1)
        expect(ObjectUtils.compare(null, null)).toBe(0)
        expect(ObjectUtils.compare(undefined, undefined)).toBe(0)
    })

    it('should work for strings', () => {
        expect(ObjectUtils.compare("1", "2")).toBe(-1)
        expect(ObjectUtils.compare("1", "1")).toBe(0)
        expect(ObjectUtils.compare("2", "2")).toBe(0)
        expect(ObjectUtils.compare("2", "1")).toBe(1)

        expect(ObjectUtils.compare(null, "1")).toBe(-1)
        expect(ObjectUtils.compare(undefined, "1")).toBe(-1)
        expect(ObjectUtils.compare("1", null)).toBe(1)
        expect(ObjectUtils.compare("1", undefined)).toBe(1)
    })

    it('should work for booleans', () => {
        expect(ObjectUtils.compare(false, true)).toBe(-1)
        expect(ObjectUtils.compare(false, false)).toBe(0)
        expect(ObjectUtils.compare(true, true)).toBe(0)
        expect(ObjectUtils.compare(true, false)).toBe(1)

        expect(ObjectUtils.compare(null, false)).toBe(-1)
        expect(ObjectUtils.compare(null, true)).toBe(-1)
        expect(ObjectUtils.compare(undefined, false)).toBe(-1)
        expect(ObjectUtils.compare(undefined, true)).toBe(-1)
        expect(ObjectUtils.compare(false, null)).toBe(1)
        expect(ObjectUtils.compare(true, null)).toBe(1)
        expect(ObjectUtils.compare(false, undefined)).toBe(1)
        expect(ObjectUtils.compare(true, undefined)).toBe(1)
    })

    it('should work for Dates', () => {
        const earlyDate: Date = new Date(Date.parse("2019-01-01T00:00:00.000Z"))
        const lateDate: Date = new Date(Date.parse("2029-01-01T00:00:00.000Z"))

        expect(ObjectUtils.compare(earlyDate, lateDate)).toBe(-1)
        expect(ObjectUtils.compare(earlyDate, earlyDate)).toBe(0)
        expect(ObjectUtils.compare(lateDate, lateDate)).toBe(0)
        expect(ObjectUtils.compare(lateDate, earlyDate)).toBe(1)

        expect(ObjectUtils.compare(null, earlyDate)).toBe(-1)
        expect(ObjectUtils.compare(null, lateDate)).toBe(-1)
        expect(ObjectUtils.compare(undefined, earlyDate)).toBe(-1)
        expect(ObjectUtils.compare(undefined, lateDate)).toBe(-1)
        expect(ObjectUtils.compare(earlyDate, null)).toBe(1)
        expect(ObjectUtils.compare(lateDate, null)).toBe(1)
        expect(ObjectUtils.compare(earlyDate, undefined)).toBe(1)
        expect(ObjectUtils.compare(lateDate, undefined)).toBe(1)
    })

})