```sparql
prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix lf-maint: <http://linkedfactory.org/vocab/maintenance#>

select ?detail ?ins ?text where {
    ?event lf-maint:nr "952"^^xsd:int;
        lf-maint:detail/lf-maint:next* ?detail .

    ?detail a lf-maint:Instruction; lf-maint:detail/lf-maint:next* ?ins .

    ?ins rdfs:label ?text .
    filter(lang(?text) = "de") 
}
```
