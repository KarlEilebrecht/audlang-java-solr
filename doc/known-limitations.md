#### [Project Overview](../README.md)
----

# Known limitations

Although the converter supports the full feature set of the [Audience Definition Language Specification](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#audience-definition-language-specification), there are a few limitations related to the Solr feature set and its concepts.

 * You cannot compare two values from two different document instances (reference match):
   * **Case 1**: Let there be a date field `date1` in the main document and another date field `date2` in a nested oder dependent document. Then, any reference match (e.g., `date1 > @date2`) will be rejected.
   * **Case 2**: Let there be a field `color1` in a nested or dependent document and another date field `color2` in a different nested oder dependent document. Again, any reference match (e.g., `color1 = @color2`) will be rejected.
   * **Case 3**: Let there be two fields `count1` and `count2` located in the same nested or dependent document, *both* assignments marked **[multi-doc](./multi-doc-concept.md)**, then any reference match (e.g., `count1 <= @count2`) will be rejected because we cannot access the data of two different document *instances* at the same time.
 * Solr cannot compare two fields (reference match) if any of them is a collection. E.g., let there be two fields on the same document, `carColor` and `favoriteColors` (a collection) then Audlang defines `carColor = @favoriteColors` to be *true* if the car color is any of the favorite colors (see [Audlang Spec §7.1](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#71-equals-on-collection-attributes)). In case of Solr this query will be rejected.

:bulb: There is an exception to the above limitations. If both fields are boolean values the converter will *resolve* the reference match into a combination of value matches *beforehand* to create a valid query.

***Read also:***
 * **[Mapping a document landscape](./mapping.md)**


