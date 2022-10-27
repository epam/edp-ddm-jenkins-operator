{{/*
Expand the name of the chart.
*/}}
{{- define "jenkins-operator.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "jenkins-operator.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "jenkins-operator.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "imageRegistry" -}}
{{- if .Values.global.imageRegistry -}}
{{- printf "%s/" .Values.global.imageRegistry -}}
{{- else -}}
{{- end -}}
{{- end }}

{{- define "keycloak.realm" -}}
{{- printf "%s-%s" .Release.Namespace .Values.keycloak.realm }}
{{- end -}}

{{/*
Define Keycloak URL
*/}}
{{- define "keycloak.url" -}}
{{- printf "%s%s" "https://" .Values.keycloak.host }}
{{- end -}}

{{/*
Define Jenkins URL
*/}}
{{- define "edp.hostnameSuffix" -}}
{{- printf "%s-%s.%s" .Values.cdPipelineName .Values.cdPipelineStageName .Values.dnsWildcard }}
{{- end }}

{{- define "admin-tools.hostname" -}}
{{- printf "admin-tools-%s" (include "edp.hostnameSuffix" .) }}
{{- end }}

{{- define "admin-tools.jenkinsUrl" -}}
{{- printf "%s%s/%s" "https://" (include "admin-tools.hostname" .) .Values.jenkins.basePath }}
{{- end }}

{{- define "jenkins.localUrl" -}}
{{- printf "%s:%s/%s" "https://jenkins" .Values.jenkins.port .Values.jenkins.basePath }}
{{- end }}

{{- define "admin-tools.gerritUrl" -}}
{{- printf "%s%s/%s" "https://" (include "admin-tools.hostname" .) .Values.gerrit.basePath }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "jenkins-operator.metaLabels" -}}
helm.sh/chart: {{ include "jenkins-operator.chart" . }}
{{ include "jenkins-operator.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "jenkins-operator.selectorLabels" -}}
app.kubernetes.io/name: {{ include "jenkins-operator.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "admin-routes.whitelist.cidr" -}}
{{- if .Values.global }}
{{- if .Values.global.whiteListIP }}
{{- .Values.global.whiteListIP.adminRoutes }}
{{- end }}
{{- end }}
{{- end -}}

{{- define "admin-routes.whitelist.annotation" -}}
haproxy.router.openshift.io/ip_whitelist: {{ (include "admin-routes.whitelist.cidr" . | default "0.0.0.0/0") | quote }}
{{- end -}}

{{/*
Redis
*/}}
{{- define "sentinel.host" -}}
{{- if .Values.sentinel.host }}
{{- .Values.sentinel.host }}
{{- else -}}
rfs-redis-sentinel.{{ .Values.namespace }}.svc
{{- end }}
{{- end }}

{{- define "sentinel.port" -}}
{{- if .Values.sentinel.port }}
{{- .Values.sentinel.port }}
{{- else -}}
26379
{{- end }}
{{- end }}
