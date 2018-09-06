#### :warning:This project is now part of the EE4J initiative. This repository has been archived as all activities are now happening in the [corresponding Eclipse repository](https://github.com/eclipse-ee4j/orb-gmbal-commons). See [here](https://www.eclipse.org/ee4j/status.php) for the overall EE4J transition status.
 
---
# Glassfish Common APIs

This is the [gmbal-commons project](https://javaee.github.io/gmbal-commons/).
 
## Releasing

* Make sure `gpg-agent` is running.
* Execute `mvn -B release:prepare release:perform`

For publishing the site do the following:

```
cd target/checkout
mvn verify site site:stage scm-publish:publish-scm
```
