#### [Project Overview](../README.md) | [Mapping a document landscape](./mapping.md)
----

# Supported Solr-types

The framework supports the following Solr field types out-of-the-box:


| Suffix       | Solr-type       | Collection? | Audlang type |
|--------------|-----------------|-------------|--------------|
| **`_i`**     | SOLR_INTEGER    | **no**      | INTEGER      |
| **`_is`**    | SOLR_INTEGER    | **yes**     | INTEGER      |
| **`_l`**     | SOLR_LONG       | **no**      | INTEGER      |
| **`_ls`**    | SOLR_LONG       | **yes**     | INTEGER      |
| **`_f`**     | SOLR_FLOAT      | **no**      | DECIMAL      |
| **`_fs`**    | SOLR_FLOAT      | **yes**     | DECIMAL      |
| **`_d`**     | SOLR_DOUBLE     | **no**      | DECIMAL      |
| **`_ds`**    | SOLR_DOUBLE     | **yes**     | DECIMAL      |
| **`_b`**     | SOLR_BOOLEAN    | **no**      | BOOL         |
| **`_bs`**    | SOLR_BOOLEAN    | **yes**     | BOOL         |
| **`_dt`**    | SOLR_DATE (UTC) | **no**      | DATE         |
| **`_dts`**   | SOLR_DATE (UTC) | **yes**     | DATE         |
| **`_s`**     | SOLR_STRING     | **no**      | STRING       |
| **`_ss`**    | SOLR_STRING     | **yes**     | STRING       |

The base types are defined in **[DefaultAdlSolrType](../src/main/java/de/calamanari/adl/solr/DefaultAdlSolrType.java)**.

If you need further variations, you might be able to address this with type decoration (formatters). See [AdlSolrTypeDecorator](../src/main/java/de/calamanari/adl/solr/AdlSolrTypeDecorator.java). 

Native type casting is not supported due to the lack of any native way of surrounding a Solr-field in a query with an expression that adjusts its value dynamically.

## Further Reading
* [Audlang Core Type Support](https://github.com/KarlEilebrecht/audlang-java-core/blob/main/src/main/java/de/calamanari/adl/cnv/tps/README.md)
* [Audlang Spec §2: Type conventions](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#2-type-conventions)

