#### [Project Overview](../README.md) | [Mapping a document landscape](./mapping.md)
----

# About type-coalescence

The Audience Definition Language (ADL) itself is *type-agnostic* but comes with a set of **[type conventions](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#2-type-conventions)**. Solr on the other hand has a couple of standard types. This framework supports the Solr-types STRING, INTEGER, LONG, FLOAT, DOUBLE, DATE (with time UTC), BOOLEAN defined in [DefaultAdlSolrType](../src/main/java/de/calamanari/adl/solr/DefaultAdlSolrType.java). 


## Implementation goals

 * Mapping should be made simple. If a type combination logically works, then the framework should just do the job, roughly following the idea of [duck typing](https://en.wikipedia.org/wiki/Duck_typing).
 * If there is no logical model (means all user-entered information is string), then the ADL's type conventions should be used to make the values compatible to the mapped field's type if possible.

## Basic value adjustments

The auto-adjustment supports many common scenarios, for example:
 * STRINGs are compatible to *every* underlying data type as long as their format meets the [§2 Type conventions](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#2-type-conventions)).
 * An INTEGER is also compatible to every supported Solr-types, given that the value can be mapped (e.g., `0=false`, `1=true`).
 * A DECIMAL is compatible to all Solr-types, except for BOOLEAN.
 
 The compatibility rules can be found in [DefaultAdlSolrType#**isCompatibleWith(AdlType type)**](../src/main/java/de/calamanari/adl/solr/DefaultAdlSolrType.java). The value adjustments happen as a part of the formatting, see [DefaultSolrFormatter](../src/main/java/de/calamanari/adl/solr/DefaultSolrFormatter.java).

 ## Special case: Date

 By intention the ADL does not deal with *time* (see [§2.3 Date Values](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#23-date-values) and [Dealing with date values](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#dealing-with-date-values)).

 This leads to problems when it comes to mapping a Solr-date which carries a UTC time portion. We must be prepared for date information with 00:00:00 time portion or any arbitrary time portion. Or maybe the date is stored as an [EPOCH value](https://en.wikipedia.org/wiki/Epoch_(computing)) within a Solr-field of type LONG.

 The advanced type coalescence automatically detects if an ADL-date is being mapped onto a field with a *finer resolution*, e.g., Solr DATE and performs special adjustments.

 *Examples:*
  * Let's assume the field `field_a` mapped to attribute `a` contains a DATE `2024-07-31T17:29:21Z` and the query would be `a = 2024-07-31`. Obviously, any direct equals-comparison would fail. Thus, the query generator will turn the query into a proper range query (everything between `2024-07-31T00:00:00Z` (incl.) and `2024-08-01T00:00:00Z` (excl.)).
  * A similar adjustment will be made for *greater than* queries. If a user queries `a > 2024-07-31` then it would be surprising to *include* `2024-07-31T17:29:21Z`. Instead the query builder will adjust the condition to `>= 2024-08-01T00:00:00Z`.

Automatic date alignment happens for Solr DATE, INTEGER and LONG (see [SolrFormatUtils#**shouldAlignDate(...)**](../src/main/java/de/calamanari/adl/solr/SolrFormatUtils.java)). The adjustments can be found in [DefaultMatchFilterFactory](../src/main/java/de/calamanari/adl/solr/cnv/DefaultMatchFilterFactory.java).

:point_right: When comparing two Solr DATE fields against eachother in a reference match the two affected DATE values will be automatically aligned to the start of that day.

:bulb: To disable the advanced date handling you can configure the flag **[DISABLE_DATE_TIME_ALIGNMENT](../src/main/java/de/calamanari/adl/solr/cnv/SolrConversionDirective.java)**.

