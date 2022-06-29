This project is to build Fuseki image and a helm chart for it.

### The process

The process of building new Fuseki image is automatic and done by the CI job. Manual intervention is needed only in the case of a new `jena-fuseki-docker` tools. In such a case the following steps needs to be done:
 
* wget 'https://repo1.maven.org/maven2/org/apache/jena/jena-fuseki-docker/4.5.0/jena-fuseki-docker-4.5.0.zip'
* unzip -j -q jena-fuseki-docker-4.5.0.zip
* rm -rf jena-fuseki-docker-4.5.0.zip

**NOTICE:** It's important to check if the newly imported files does provide the same functionality as the previous ones.
