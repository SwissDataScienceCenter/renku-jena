
This project builds Docker image and a helm chart for chosen version of [Fuseki with UI](https://jena.apache.org/documentation/fuseki2/).

## Building docker image

The Docker image building process takes the following arguments:
`JENA_VERSION` defaulted to `4.9.0`
`OPENJDK_VERSION` defaulted to `17`
`ALPINE_VERSION` defaulted to `3.18.2`

To build the Docker image manually the following command can be used:
```
docker build --force-rm --build-arg JENA_VERSION=4.9.0 -t fuseki .
```

It's possible to build the image for `linux/amd64` and `linux/arm64` platforms.

## Running docker image

There are images built for `linux/amd64` and `linux/arm64` available on dockerhub. For available versions check [renku/renku-jena page](https://hub.docker.com/r/renku/renku-jena/tags).

There are certain environment variables that might be set before running the image:
* `JVM_ARGS` - environment variable with all additional JVM ARGs needed for the server to operate correctly. The default value is: `-Xmx2048m -Xms2048m`.
* `FUSEKI_BASE` - a folder where all the configurations and data are kept. By default it's set to `/fuseki`. It's mandatory to mount some volume which `FUSEKI_BASE` points to. The `fuseki` user has to have write privileges on that folder. The folder should be a persistent volume, otherwise, all the data and the configuration will be lost on the server shutdown. It's recommended not to change the value for `FUSEKI_BASE` but simply mount a volume to `fuseki`.

An example `docker run` command starting Fuseki server may look like follows:

```
docker run -i --rm -p "3030:3030" -e 'FUSEKI_BASE=/fuseki' --mount type=bind,src=/tmp/jena,dst=/fuseki -t fuseki
```

Additionally, other files might be mounted to the server to customise certain its aspects:
* **Security:** By default a simple security model is used where an `admin` user (with `admin` password) is configured. The user protects the admin endpoints only (all endpoints starting with `/$/`, except `/$/status`  and `/$/ping` which are left unprotected). A chosen security model might be used and configured in a custom `shiro.ini` file. The file has to be mounted on `${FUSEKI_BASE}/shiro.ini`. More info on how to prepare the file can be found [here](https://jena.apache.org/documentation/fuseki2/fuseki-security.html).
* **Logging:** By default all server log statements of severity `INFO` and higher will be logged to the console. The standard rules can be changed in a custom file and mounted on `${FUSEKI_BASE}/log4j2.properties`. More can be found [here](https://jena.apache.org/documentation/fuseki2/fuseki-logging.html).
* **Server properties:** default Fuseki server properties are stored in the `${FUSEKI_BASE}/config.ttl` file. A custom file can be prepared and mounted to replace the defaults.

## Configuring Helm chart

There's a set of values that have to be configured in order to make Fuseki working as intended:
* `persistence.size` - size of the persistence volume; defaulted to 1Gi;
* `additionalEnvironmentVariables.JVM_ARGS` - flags to be passed to the JVM in the server startup command; it's common to pass some memory related settings to JVM, e.g. `-Xmx2048m -Xms2048m`.
* `requests.memory` - amount of memory requested for the container; should be higher than 'Xmx' if set on `additionalEnvironmentVariables.JVM_ARGS`;

By default, the chart creates a Persistent Volume and mounts it at `$FUSEKI_BASE`. This folder is used by Fuseki for storing both the configuration and the data. It's crucial for `$FUSEKI_BASE` to be on a persistent volume, otherwise the configuration and the data will be lost on server shutdown. As mentioned above, it's recommended not to modify the mounting point but simply mount a volume to `/fuseki`.

There might be other files that had to be customised in order to achieve certain behaviour of Fuseki server. These files should be listed under `additionalVolumeMounts` and `additionalVolumes` in the `values.yaml` file. The most common customisations are:
* `shiro.ini` - definition of security aspects of the Fuseki server. In-depth info can be found [here](https://jena.apache.org/documentation/fuseki2/fuseki-security.html). As mentioned above, the default file specifies `admin` user (with `admin` password) that protects admin endpoints (starting with `/$/`) except `/$/status`  and `/$/ping`.
* `log4j2.properties` - logging settings; more info is available on the [Fuseki page](https://jena.apache.org/documentation/fuseki2/fuseki-logging.html)

An example configuration of a volume mount (for `shiro.ini` file specifically) may be done this way:
```
additionalVolumeMounts:
  - name: shiro-config
    mountPath: /fuseki/shiro.ini
    subPath: shiro.ini
    readOnly: true
```

An example configuration for a volume may look like follows:
```
additionalVolumes:
  - name: shiro-config
    configMap:
      name: jena-shiro-ini
      defaultMode: 0555
```

While `jena-shiro-ini` can be defined like this:
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: jena-shiro-ini
  labels:
    {{- include "renku-jena.labels" . | nindent 4 }}
data:
  shiro.ini: |-
    [main]
    # Development
    ssl.enabled = false

    plainMatcher=org.apache.shiro.authc.credential.SimpleCredentialsMatcher
    iniRealm.credentialsMatcher = $plainMatcher

    [users]
    admin={{ .Values.users.admin.password }}
    dsuser={{ .Values.users.user.password }}

    [roles]

    [urls]
    ## Control functions open to anyone
    /$/status = anon
    /$/ping   = anon

    ## and the rest are restricted
    /$/** = authcBasic,user[admin]
    /my-dataset/** = authcBasic,user[dsuser]
```

All of the mentioned mounts are optional and chart provides defaults for them (look for a relevant file in the root folder of the repository).

## Working with the server

Initially, the server starts with no datasets, except from the cases when there are datasets configured and created in the mounted `${FUSEKI_BASE}/configuration` and `${FUSEKI_BASE}/databases` folders. In such a case, the existing datasets will be served by the server straight after the start-up.

New datasets can be added in the following ways:
* through the admin API by POSTing a ttl config file (preferred way):
  ```
  curl -X POST -d @custom-ds.ttl -H 'Content-Type: text/turtle' http://fuseki-host/$/datasets
  ```
* through the Fuseki UI Console;
* through new ttl configuration file added to `${FUSEKI_BASE}/configuration` folder (server restart is needed to pick up the new configs);

**NOTICE:** the security model configured in the `shiro.ini` file will be used for the newly created datasets.

## Releasing

In order to release a new version of both the Docker image and the chart, the following steps needs to be done:
* update the `version` property in the `renku-jena/Chart.yaml` with a correct value;
* update the `image.tag` property in the `renku-jena/values.yaml` with a correct value;
* update the `JENA_VERSION` build argument in the `chartpress.yaml` with the relevant version of Jena;
* publish a new release in GitHub.
