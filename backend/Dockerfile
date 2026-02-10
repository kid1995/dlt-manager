# USE_LATEST
FROM prod.docker.system.local/baseimages/sda-jre21-alpine:8

COPY ./build/install/* /java/

ENV TZ Europe/Berlin
ENV LANG de_DE.UTF-8
ENV LANGUAGE de_DE.UTF-8
ENV LC_ALL de_DE.UTF-8

RUN /tmp/setPermissions.sh

USER java

ENTRYPOINT ["/java/bin/backend"]
