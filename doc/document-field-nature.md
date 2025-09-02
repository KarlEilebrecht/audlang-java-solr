#### [Project Overview](../README.md)
----

# About the nature of documents and fields

The approach requires one central so-called *main document* for each individual with an identifier field. This plays well with Solr's concept of a unique document key. Always keep in mind that any id-selection or counting runs on these documents.

Additionally, it is possible to have sub-documents. These must be *joined* with the main document whenever any of their fields is involved in a query.

Conceptually, the behavior is similar to a central table and some additional tables in a relational database. A *foreign key* allows to join dependent entities with the main entity.

However, there are some important notes:
 * The central entity in form of the **main document is mandatory**. Other than with SQL where you could determine a super-set of ids by unioning all involved tables, in Solr queries we must always start with the main document. From there, we can join sub-documents.
 * While joining in SQL means *joining data* (making additional columns available), for Solr it means *switching the context to a sub-document*. This implies that you cannot deal with the fields of two different documents at the same time (within a pair of braces). Consequently, **it is impossible to compare fields sitting in different documents against each other**.
 * Other than SQL, Solr has built-in support for collection fields, means fields that can hold multiple values of a kind. Most of the time, this works transparantly and the semantics are compliant to [§7 Audlang Spec](https://github.com/KarlEilebrecht/audlang-spec/blob/main/doc/AudienceDefinitionLanguageSpecification.md#7-audlang-and-collection-attributes). However, **you cannot compare two collection fields against each other**, even if they sit on the same document.
 * **Fields in Solr are globally defined**, uniquely identified by their name. There is no concept of *qualifying* a field by its document. However, to ease configuration, it was decided to pretend their existence in the context of a particular document configuration. The validation rules of the document configuration ensure that you cannot create ambiguous settings regarding the same field.

## Document characteristics

All documents in Solr exist side-by-side, no matter if we are talking about the main document or any sub-document. Although called "nested" (and the loading from json seems to imply that), nested documents are not living inside the main document, they exist as separate documents. The difference compared to dependent documents is the way Solr indexes and joins these documents from the main document. There are performance and management advantages in either approach. For example, you can add *dependent* documents without re-indexing the main document. This does not work with *nested* documents. Nested documents on the other hand offer the advantage to easily query their data along with the main document, and joins are less expensive. Being dependent or nested has no impact on the available query feature set.

 All documents carry a **globally unique key** in the [**unique key field**](../src/main/java/de/calamanari/adl/solr/SolrFormatConstants.java) and a **node type** identifier in the [**node type field**](../src/main/java/de/calamanari/adl/solr/SolrFormatConstants.java) to distinguish different kinds of documents from each other.

 * **Main document**: One document exists for each individual in the base audience. If possible, put all attributes into this document as Solr-joins are costly.

 * **Nested document**: A document with extra data virtually nested in a main document. There can be multiple of these documents of a kind (node type) for the same main document. See also [Multi-doc](./multi-doc-concept.md).

 * **Dependent document**: A document with extra data linked to a main document by a **foreign key** in the [**main key field**](../src/main/java/de/calamanari/adl/solr/SolrFormatConstants.java). There can be multiple of these documents of a kind (node type) for the same main document. See also [Multi-doc](./multi-doc-concept.md).

 Additionally, every document can carry **document filters**, additional condition(s) in form of [FilterFields](../src/main/java/de/calamanari/adl/solr/config/FilterField.java). Whenever a query involves this document, the document filter(s) narrow the scope. Document filters are meant for scenarions where queries on a (sub-)document must be limited independently from the conditions of a given expression (e.g., an `is_active_b`-field).

## Field and assignment characteristics

 * **isCollection**: The field can hold multiple values. This impacts the ability to compare fields.
 * **isMultiDoc**: See [Multi-doc](./multi-doc-concept.md).

## NOT always translates to NOT ANY

This rule applies to every field assignment no matter if it is marked *multi-doc* or not. This is sometimes inconvenient for the user, but it is an **essential requirement**.

Given a condition `color = red` Audlang requires the two sets defined by `color=red` and `color != red` to be **disjoint**.

Would we allow testing a negative condition on a particular sub-document *instance* rather than *any*, the sets above could *overlap*. Let's assume the field `color` sits on a nested document *layout* and for the profile `4711` exist two layouts, one with `color=red` and the other with `color=blue`. Testing on document instance level would mean that the query `color=red` returns `4711` but also `NOT color=red`. This is not only a cosmetic issue, it potentially compromises the whole expression logic behind the scenes.

