# Blackbox #

Blackbox is a server side application that acts as a bridge between Elasticsearch and client applications to simplify search experience that were before difficult to meet by the University of Bergen Library, department of Special Collections. It is developed using Object Oriented Programming best practices and uses Elasticsearch core API to communicate with a cluster.

Blackbox communicates with Elasticsearch cluster in round-robbin fashion through Transport client that has full priviledge as one of the Elasticsearch nodes in the cluster. 

It runs as a [tomcat application](http://kirishima.uib.no/blackbox) and all client applications can communicate with it through HTTP transport.

In this version, we are making it independent from the client implementations and hence can be queried separately. See f.eg http://kirishima.uib.no/blackbox/search?index=ska2&pretty=1

### Usage ###
Blackbox can be queried by specifying parameters in a respective endpoint.
[Search endpoint](http://kirishima.uib.no/blackbox/search) can take parameters such as index, type, q, date_from, date_to, pretty, size etc. See below for an individual parameters.

* index : The index that the search should be executed. It can be more than one indices, e.g
http://kirishima.uib.no/blackbox/search?index=ska2&index=admin-test. If not specified, all indices in the cluster will be considered.
* type : Same as index, See Elasticsearch type.
* q : A query string. 
* size : The size of the returned results. Default is 10
* date_from/date_to : Search for created date in the form of yyyy-MM-dd
* filter : A terms filter if you want to limit the search results. e.g 
```
filter=type.exact#Brev
```
, you will only search within Brev.
* pretty: Prettify results. It can hold explicit true values such as true, 1 and on. It is false by default.
* sort: You can sort by specifying the field that you want to sort and the sorting order seperated by colon. For example, **available:asc** will sort the results in **asc**ending order of the filed **available**

For example http://kirishima.uib.no/blackbox/search?q=knud+knudsen, this will perfom a search to all indices in the cluster for the string "knud knudsen".

Another endpoint, is [suggest endpoint](http://kirishima.uib.no/blackbox/suggest) which is used for auto suggestion. The result is an array of the suggested values. For example http://kirishima.uib.no/blackbox/suggest?q=marianne gives a list of suggested values for string "marianne".


We have also support for "exclude" API. This means if one wants to exclude a terms facet, one will have to write 
```
filter=-field#value
```
For example, this query exclude Fotografi in Marcus prod: http://marcus.uib.no/search/?filter=-type.exact%23Fotografi