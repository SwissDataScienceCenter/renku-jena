#!/bin/sh
## Licensed under the terms of http://www.apache.org/licenses/LICENSE-2.0

if [ ! -f "$FUSEKI_BASE/config.ttl" ]; then
  echo "###################################"
  echo "No server $FUSEKI_BASE/config.ttl -> copying default..."
  echo "###################################"
  cp "$FUSEKI_HOME/run/config.ttl" "$FUSEKI_BASE/config.ttl"
fi

if [ ! -f "$FUSEKI_BASE/shiro.ini" ]; then
  echo "###################################"
  echo "No $FUSEKI_BASE/shiro.ini -> copying default..."
  echo "###################################"
  cp "$FUSEKI_HOME/run/shiro.ini" "$FUSEKI_BASE/shiro.ini"
fi

cp "${FUSEKI_HOME}/run/log4j2.properties" "${FUSEKI_BASE}/log4j2.properties"

if [ -z "$FUSEKI_COMMAND" ]; then
  export FUSEKI_COMMAND="--conf=${FUSEKI_BASE}/config.ttl"
fi

if [ -f "$FUSEKI_BASE/system/tdb.lock" ]; then
  echo "###################################"
  echo "Found an existing tdb.lock at startup at $FUSEKI_BASE/system, will delete it before startup."
  echo "###################################"
  rm -f "$FUSEKI_BASE/system/tdb.lock"
fi

exec /usr/bin/compact-jena &
"${FUSEKI_HOME}/fuseki-server" "$FUSEKI_COMMAND"
