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
