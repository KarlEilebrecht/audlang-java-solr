# Audlang Java Solr

This repository is part of project [Audlang](https://github.com/users/KarlEilebrecht/projects/1/views/1?pane=info) and provides an **[Apache Solr](https://solr.apache.org/)** converter implementation for the **[Audience Definition Language Specification (Audlang)](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#audience-definition-language-specification)** based
on the [Audlang Java Core Project](https://github.com/KarlEilebrecht/audlang-java-core). Latest build artifacts can be found on [Maven Central](https://central.sonatype.com/namespace/de.calamanari.adl).

```xml
		<dependency>
			<groupId>de.calamanari.adl</groupId>
			<artifactId>audlang-java-solr</artifactId>
			<version>1.0.0</version>
		</dependency>
```

There is a small set of further dependencies (e.g., SLF4j for logging, JUnit), please refer to this project's POM for details.

Primarily, this project includes a functional **ready-to-use implementation** to map various Solr document layouts and to generate Solr-queries for these setups from [CoreExpressions](https://github.com/KarlEilebrecht/audlang-java-core/blob/main/src/main/java/de/calamanari/adl/irl/README.md). The converter supports the full feature set of the [Audience Definition Language Specification](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#audience-definition-language-specification) with only very few [limitations](./doc/known-limitations.md).

The purpose of this framework is generating Solr-queries (textual expressions). Hence, apart from the tests included in this project, there are no library dependencies to the Apache Solr ecosystem. The project has been developed for Solr 9, and its tests run against this release. However, there is no dependency to any particular Solr version.

Significant parts of this project are dedicated to the definition and mapping of a Solr document landscape. Similar to my [sql-converter implementation](https://github.com/KarlEilebrecht/audlang-java-sql), a major goal was supporting different layouts. You can work with a single document, but there is also support for joining **nested or dependent documents**. 

A **fluent self-explaining API** helps simplify the setup and keep the configuration short and readable. Special aspects like **multi-tenancy** are covered by the built-in support for **document filters**. This way data fields can be kept separate from purely technical fields to limit or scope the data access. Although, best practice should be first setting up a logical data model in form of an [ArgMetaInfoLookup](https://github.com/KarlEilebrecht/audlang-java-core/tree/main/src/main/java/de/calamanari/adl/cnv/tps#readme) and mapping it to the documents, the framework also provides **rule-based auto-mapping**. This is especially interesting for initial experimenting and testing as it aligns perfectly with Solr's dynamic field concept.

Bridging the gap between a logical data model that conforms to the [Audlang Type Conventions](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#2-type-conventions) (resp. any untyped model) and a managed schema with Solr-types was a main challenge during the development. The framework addresses the problem most of the time with **[automatic type coalescence](./doc/type-coalescence.md)** for **[common Solr-types](./doc/solr-types.md)**. 

To get an idea how the configuration works, checkout this project and review the [managed schema](./src/test/resources/solr/configsets/audlang/conf/managed-schema.xml), [test data](./src/test/resources/solr/exampledocs/audlang-data-hybrid.json) and the [mapping](./src/test/java/de/calamanari/adl/solr/EmbeddedSolrServerUtils.java) related to the [embedded Solr tests](./src/test/java/de/calamanari/adl/solr/cnv/SolrExpressionConverterComplexTest.java).

*Give it a try, have fun!*

Karl Eilebrecht, August 2025

***Read next:***
 * **[Mapping a document landscape](./doc/mapping.md)**
 * [Package documentation](./src/main/java/de/calamanari/adl/solr/README.md)
 
----
<img align="right" src="https://sonarcloud.io/api/project_badges/measure?project=KarlEilebrecht_audlang-java-solr&metric=alert_status" />

[![SonarQube Cloud](https://sonarcloud.io/images/project_badges/sonarcloud-light.svg)](https://sonarcloud.io/summary/new_code?id=KarlEilebrecht_audlang-java-solr)


