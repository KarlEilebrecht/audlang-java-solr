#### [Project Overview](../README.md) | [About the nature of documents and fields](./document-field-nature.md)
----

# About "multi-doc"

## Definition of "multi-doc"

The multi-doc-concept addresses an issue you see when mapping attributes to fields in a nested or dependent document. When queried in the wrong way the result set can be empty or incomplete.

:bulb: Only field assignments on *nested or dependent documents* can be *multi-doc* because by definition there can be only a single main document per individual.

It is easier to explain this with examples:

### Example: Data from multiple partners

Let's assume a scenario with the attribute *favorite color* `favColor`, mapped to a field `fav_color_s` and the attribute *car owner* `carOwner`, mapped to a field `car_owner_b` both sitting on the nested document of type `partner_data`. As long as there is either exactly one such nested document or no such document, you are safe. The behavior is exactly as if the data was part of the main document.

But now imagine there can be *multiple* nested documents of type `partner_data` (e.g., onboarded from multiple sources). It is reasonable to assume that not all dynamic fields exist in all of these documents.

This leads to a critical problem:

Let's assume a user wants to see all guys who love the color red and have a car.

```
favColor=red AND carOwner=1
```

Look at the Solr-query below:

```
node_type:node1
AND {!parent which="node_type:node1" v="node_type\:node2\
AND\ car_owner_b\:TRUE\
AND\ fav_color_s\:red"}

```

*Do you see the problem?* - The first condition `car_owner_b:TRUE` *pins* the document, so the second condition cannot be met anymore. This effect is called *accidental document-pinning*. Just like in this simple example similar problems can occur in more complicated ways.

This is an example where we must inform the query builder that the mapping is **multi-doc**, means: the data for the same individual may sit in more than one sub-document.

Now the converter solves the problem by creating two filter queries (joined with AND):

```
node_type:node1
AND {!parent which="node_type:node1" v="node_type\:node2\
    AND\ car_owner_b\:TRUE"}
```
*`and`*
```
node_type:node1
AND {!parent which="node_type:node1" v="node_type\:node2\
    AND\ fav_color_s\:red"}
```

:bulb: You can mark an entire nested or dependent document as *multi-doc* which tells the system that every field-assignment for this document shall be marked *multi-doc*.

## Disadvantages

First of all, marking an attribute mapping *multi-doc* most of the time complicates the query (more joins, existence checks, etc.).

But there is another potential problem: In a document with multiple fields a *multi-doc* attribute gets *logically detached* from its document-context.

Check the pos-data (point-of-sale) below:

| id    | purchase_date_dt | article_s | price_d |
|-------|------------------|-----------|---------|
| 19011 | 2024-01-13 | DROP LIGHT | 198.78 |
| 19017 | 2024-03-14 | RED WINE 0.75L | 5.75 |
| 19017 | 2024-03-15 | CORNFLAKES | 4.25 |
| 19017 | 2024-03-15 | CHEESE 36M 100GR |8.99 |
| 19017 | 2024-03-31 | WATERMELON EXTRACT 0.118L |199.99 |

You decided to mark the *purchaseDate* mapping as *multi-doc*,
so the user can easily find out if *anything* was bought at a given date.

The issue with this approach is that the *purchaseDate* is now logically decoupled from the remaining columns.

For record `19017` we know this guy bought *anything* on `2024-03-31`. And we know the same guy bought cornflakes on `2024-03-15`.

So, the query `purchaseDate=2024-03-31 AND article=CORNFLAKES` would return record `19017`. This may be what you want or not ...

```
node_type:node1
AND {!join from=main_id to=id v="node_type\:node3\
    AND\ article_s\:CORNFLAKES"}
```
*`and`*
```
node_type:node1
AND {!join from=main_id to=id v="node_type\:node3\
    AND\ purchase_date_dt\:\[2024\\\-03\\\-31T00\\\:00\\\:00Z\ TO\ 2024\\\-04\\\-01T00\\\:00\\\:00Z\}"}
```

A simple tweak would be mapping the `purchase_date_dt` field twice, first regularly to *purchaseDate* and a second time to *anyPurchaseDate*, this time marked *multi-row*.

The query `article=CORNFLAKES AND purchaseDate = 2024-03-15 AND anyPurchaseDate = 2024-03-31` translates to:

```
node_type:node1
AND {!join from=main_id to=id v="node_type\:node3\
    AND\ article_s\:CORNFLAKES\
    AND\ purchase_date_dt\:\[2024\\\-03\\\-15T00\\\:00\\\:00Z\ TO\ 2024\\\-03\\\-16T00\\\:00\\\:00Z\}"}
```
*`and`*
```
node_type:node1
AND {!join from=main_id to=id v="node_type\:node3\
    AND\ purchase_date_dt\:\[2024\\\-03\\\-31T00\\\:00\\\:00Z\ TO\ 2024\\\-04\\\-01T00\\\:00\\\:00Z\}"}
```
Whatever you decide, keep in mind that *multi-doc* does not play well with nested or dependent documents that carry *related attributes* of a record. 

