{{/*
Generate resource name: <stage>-<chart>[-ui]
*/}}
{{- define "copsi.name" -}}
{{- $base := printf "%s-%s" .Values.stage .Chart.Name -}}
{{- if eq .Values.component "frontend" -}}
{{- printf "%s-ui" $base | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $base | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Chart label
*/}}
{{- define "copsi.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "copsi.labels" -}}
helm.sh/chart: {{ include "copsi.chart" . }}
{{ include "copsi.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Selector labels
*/}}
{{- define "copsi.selectorLabels" -}}
app: {{ include "copsi.name" . }}
app.kubernetes.io/name: {{ .Chart.Name }}
environment: {{ .Values.stage }}
istio-external: "true"
{{- end -}}
