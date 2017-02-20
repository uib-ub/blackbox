# Blackbox #

![Alt text](src/main/webapp/images/class_diagram.png?raw=true "Class diagrams")

Blackbox is a server side application that acts as a bridge between Elasticsearch and client applications to simplify search experience that were before difficult to meet. It is developed to meet the University of Bergen Library requirements and adheres to OOP best practices and uses Elasticsearch core API to communicate with a cluster.

Blackbox communicates with Elasticsearch cluster in round-robbin fashion through Transport client which acts as one of the Elasticsearch nodes in the cluster. 

It runs as a [tomcat application](http://kirishima.uib.no/blackbox) and clients can communicate with it through HTTP.

It is independent from the client implementations and hence can be queried separately. 

### Usage ###
Blackbox can be queried by specifying parameters in a respective endpoint.
[Search endpoint](http://kirishima.uib.no/blackbox/search) can take parameters such as the followings:-

*<code>index</code> : the index that the search should be executed. It can be more than one indices, e.g
<code>http://kirishima.uib.no/blackbox/search?index=ska2&index=admin-test</code>. If not specified, all indices in the cluster will be considered.
* <code>type</code> : same as index, See Elasticsearch type.
* <code>q</code> : a query string. For example, <code> http://kirishima.uib.no/blackbox/search?q=knud+knudsen</code> will perform a search to all indices in the cluster for the search string "knud knudsen".
* <code>size</code> : the size of the returned results. Default is 10
* <code> date_from / date_to </code>: search for created date in the form of <code>yyyy-MM-dd</code> or  <code>yyyy-MM</code> or  <code>yyyy</code>  
* <code>filter</code> : a terms filter if you want to limit the search results e.g <code>filter=type.exact#Brev</code>
, you will only search within type <code>Brev</code>.
* <code>aggs</code>: you can specify aggregations as paramater. Aggregations must be a valid JSON arrays. For example <code> facets: [
                                                                                                                                            {
                                                                                                                                                "field": "type",
                                                                                                                                                "size": 30,
                                                                                                                                                "operator": "OR",
                                                                                                                                                "order": "count_desc",
                                                                                                                                                "min_doc_count": 0
                                                                                                                                            },
                                                                                                                                            {
                                                                                                                                                "field": "hasZoom",
                                                                                                                                                "size": 10,
                                                                                                                                                "operator": "AND",
                                                                                                                                                "order": "term_asc",
                                                                                                                                                "min_doc_count": 0
                                                                                                                                            }]
                                                                                                                                            </code> 
* <code>pretty</code>: prettify results. It can hold explicit true values such as <code>true</code>, <code>1</code> or <code>on</code>. It is <code>false</code> by default.
* <code>sort</code>: you can sort by specifying the field that you want to sort and the sorting order separated by colon. For example, <code>available:asc</code> will sort the results in **asc**ending order of the filed <code>available</code>. You will have to make sure that the field exists and it is not analyzed.
* <code>service</code>: a service parameter tells Blackbox to construct a query based on a particular service, currently we have WAB, MARCUS and SKA services. Default is MARCUS service. We introduced  <code>service</code> because we would like to build query based on which data set we are querying. For instance, you would want to boost document of type "Postkort" in Marcus and of type "Skeivopedia" in Skeivtarkiv.  
* <code>index_boost</code>: sometimes you would want to boost documents that belong to a specific index if you are querying multiple indices at the same time. Here comes <code>index_boost</code> which takes index_name as it's value. 

Another endpoint is [suggest endpoint](http://kirishima.uib.no/blackbox/suggest?=marcus) which is used for auto suggestion. The result is an array of the suggested values. For example <code>http://kirishima.uib.no/blackbox/suggest?q=marianne</code> gives a list of suggested values for string "marianne".


We have also support for <code>"exclude" API </code>. This means if one wants to exclude a terms facet, one will have to write in form of
<code>filter=-field#value</code>. Note the minus sign in front of field name. This query exclude documents of type <code>Fotografi</code> in Marcus : <code> http://marcus.uib.no/search/?filter=-type.exact%23Fotografi</code>