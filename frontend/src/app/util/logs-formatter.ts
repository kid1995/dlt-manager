export interface LogInfo{
    className: string,
    fnName: string,
    errorSrc?: string,
    error?: unknown
}

export default class LogsFormatter {
    
    public static formatMsg(info: LogInfo){
        return `${info.className} ~ ${info.fnName} ~ ${info?.errorSrc}: `
    }
    
    public static formatErrorMsg(errorInfo: LogInfo){
        return this.formatMsg(errorInfo) + errorInfo?.error;
    }
}