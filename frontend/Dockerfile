FROM prod.docker.system.local/baseimages/sda-nginx-1-alpine:69

COPY ./dist/dltmanager-ui/browser /usr/share/nginx/html/

USER root
RUN /tmp/setPermissions.sh && \
    /tmp/changeConfiguration.sh Configuration sub_filter BACKEND_URL NOT_SET_BACKEND_URL  && \
    /tmp/changeConfiguration.sh Configuration sub_filter AUTH_URL NOT_SET_AUTH_URL  && \
    /tmp/changeConfiguration.sh Header Content-Security-Policy default-src self && \
    /tmp/changeConfiguration.sh Header Content-Security-Policy font-src "'self' https://infonet.signal-iduna.de *.system.local *.system-a.local https://*.signal-iduna.de https://*.signal-iduna.org localhost:* https://tagmanager.google.com www.googletagmanager.com https://www.google-analytics.com http://tagmanager.google.com https://fonts.googleapis.com https://fonts.gstatic.com data: 'unsafe-inline' 'unsafe-eval'" && \
    /tmp/changeConfiguration.sh Header Content-Security-Policy connect-src "https://*.system.local https://*.signal-iduna.org" && \
    /tmp/changeConfiguration.sh Header Content-Security-Policy frame-ancestors none && \
    /tmp/changeConfiguration.sh Header Content-Security-Policy sandbox allow-same-origin && \
    /tmp/changeConfiguration.sh Header Content-Security-Policy sandbox allow-scripts && \
    /tmp/changeConfiguration.sh Header Content-Security-Policy upgrade-insecure-requests

USER nginx
