#!/bin/sh
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

if [[ ! -n "$ADMIN_PASSWORD" ]] ; then
  echo -e '\n**** No ADMIN_PASSWORD env variable set ****\n'
  exit 1
fi
cp ${FUSEKI_HOME}/run/shiro.ini ${FUSEKI_BASE}/shiro.ini
sed -i 's/${ADMIN_PASSWORD}/'${ADMIN_PASSWORD}'/g' ${FUSEKI_BASE}/shiro.ini

cp ${FUSEKI_HOME}/log4j2.properties ${FUSEKI_BASE}/log4j2.properties

if [[ ! -n "$FUSEKI_COMMAND" ]] ; then
  export FUSEKI_COMMAND="--conf=${FUSEKI_BASE}/config.ttl"
fi

exec "${FUSEKI_HOME}/fuseki-server" "$FUSEKI_COMMAND"
