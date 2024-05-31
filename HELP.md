# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.2.0-M2/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.2.0-M2/maven-plugin/reference/html/#build-image)
* [Spring Web](https://docs.spring.io/spring-boot/docs/3.2.0-M2/reference/htmlsingle/index.html#web)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)


```shell
keytool -genkeypair -alias http3.demo -keyalg RSA -keysize 2048 -storetype PKCS12 -storepass changeit -keystore keystore.p12 -validity 3650
keytool -genkey -alias http3.demo -storetype PKCS12 -keyalg RSA -keypass changeit -storepass changeit -keystore keystore.p12
keytool -export -alias http3.demo -storepass changeit -file server.cer -keystore keystore.jks
keytool  -importcert  -keystore trust.jks
```
