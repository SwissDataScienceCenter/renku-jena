This project is to build Fuseki image and a helm chart for it.


JENA_VERSION defaulted to 4.5.0
OPENJDK_VERSION defaulted to 17
ALPINE_VERSION defaulted to 3.15.0

There are two folders:
* FUSEKI_HOME with all the binaries and defaults
* FUSEKI_BASE with all configuration and data
There may/should be volume mounted to FUSEKI_BASE which is `/fuseki` to prevent data and configuration being removed.

The required `ADMIN_PASSWORD` is a required environment variable and it protects access to the admin API (and the Fuseki UI console).

The `/fuseki/shiro.ini` is a file protecting the API. By default the `admin=$ADMIN_PASSWORD` entry is set there on start-up to secure the admin endpoints (all the endpoints starting with `/$/` except `/$/status`  and `/$/ping` which are left unprotected).

Dataset(s) endpoint security is achieved by setting relevant entries in the `/fuseki/password.file` (a custom file should be mounted to replace the empty file). By default the Basic auth is configured so the `password.file` has to have entries in the `username: password` format. Optionally, the Digest auth might be used but it would require replacing the `/fuseki/config.ttl` with one having relevant changes. Additionally, to secure a chosen dataset(s), a relevant entry has to be set on the Dataset configuration ttl file (e.g. `fuseki:allowedUsers "admin", "renku" ;`; more info can be found at https://jena.apache.org/documentation/fuseki2/fuseki-data-access-control.html).

There's also `JAVA_OPTIONS` environment variable defaulted to `-Xmx2048m -Xms2048m` which also might be modified if needed.
