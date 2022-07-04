
This project builds (Fuseki with UI)[https://jena.apache.org/documentation/fuseki2/] Docker image and a helm chart for it.

## Building docker image

The Docker image building process takes the following arguments:
`JENA_VERSION` defaulted to `4.5.0`
`OPENJDK_VERSION` defaulted to `17`
`ALPINE_VERSION` defaulted to `3.15.0`

To build the Docker image manually the following command can be used:
```
docker build --force-rm --build-arg JENA_VERSION=4.5.0 -t fuseki
```

## Running docker image

There are certain environment variables that might be set before running the image:
* `FUSEKI_BASE` - a folder where all the configurations and data are kept. By default the `FUSEKI_BASE` is set to `/fuseki`. It's mandatory to mount some volume which `FUSEKI_BASE` points to. The `fuseki` user has to have write privileges on that folder. The folder should be a persistent volume, otherwise all the data and configuration will be lost on the server shutdown.
* `JVM_ARGS` - environment variable with all additional JVM ARGs. By default it's set to `-Xmx2048m -Xms2048m`.

An example `docker run` command starting Fuseki server may look like follows:

```
docker run -i --rm -p "3030:3030" -e 'FUSEKI_BASE=/fuseki' --mount type=bind,src=/tmp/jena,dst=/fuseki -t fuseki
```

Additionally, other files might be mounted to the running sever to customise certain its aspects:
* Security: by default a simple security model is used where an `admin` user (with `admin` password) is configured. The user protects the admin endpoints only (all endpoints starting with `/$/` except `/$/status`  and `/$/ping` which are left unprotected). A chosen security model might be used and configured in a custom `shiro.ini` file. The file has to be mounted on `${FUSEKI_BASE}/shiro.ini`. More info on how to prepare the file can be found (here)[https://jena.apache.org/documentation/fuseki2/fuseki-security.html].
* Logging: by default all server log statements of severity `INFO` and higher will be logged to the Console. The standard rules can be changed in a custom file and mounted on `${FUSEKI_BASE}/log4j2.properties`. More can be found (here)[https://jena.apache.org/documentation/fuseki2/fuseki-logging.html].
* Server properties: default Fuseki server properties are stored in the `${FUSEKI_BASE}/config.ttl` file. A custom file can be prepared and mounted to replace the defaults.

## Configuring Helm chart

## Working with the server

Initially, the server starts with no datasets, except from the cases when there are datasets configured and created in the mounted `${FUSEKI_BASE}/configuration` and `${FUSEKI_BASE}/databases` folders. In such a case, the existing datasets will be served by the server straight after the start-up.

New datasets can be added in the following ways:
* through the Fuseki UI Console;
* through new ttl configuration file added to `${FUSEKI_BASE}/configuration` folder (server restart is needed to pick up the config);
* through the admin API by POSTing a ttl config file:
```
curl -X POST -d @custom-ds.ttl -H 'Content-Type: text/turtle' http://fuseki-host/$/datasets
```

*NOTICE:* the security model configured in the `shiro.ini` file will be used for the newly created datasets.
