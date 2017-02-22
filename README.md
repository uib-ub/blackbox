# Blackbox - a bridge between search clients and Elasticsearch cluster

![Alt text](src/main/webapp/images/class_diagram.png?raw=true "Class diagrams")

##Preamble

Blackbox is a server side application that acts as a bridge between Elasticsearch and client applications to simplify search experience that were before difficult to meet. It is developed to meet the University of Bergen Library requirements and adheres to OOP best practices and uses Elasticsearch core API to communicate with a cluster.

Blackbox communicates with Elasticsearch cluster in round-robbin fashion through Transport client which acts as one of the Elasticsearch nodes in the cluster. It runs as a [Web application](http://kirishima.uib.no/blackbox) whereby clients communicate with it through HTTP and it is independent from the client implementation and hence can be queried separately. 

## Usage 
Blackbox can be queried by specifying query parameters in a respective endpoint.
[Search endpoint](http://kirishima.uib.no/blackbox/search) can take parameters such as the followings:-

* `index` : the index that the search should be executed. It can be more than one indices, e.g http://kirishima.uib.no/blackbox/search?index=ska2&index=admin-test. If not specified, all indices in the cluster will be considered.
* `type` : same as index, See Elasticsearch type.
* `q` : a query string. For example, http://kirishima.uib.no/blackbox/search?q=knud+knudsen will perform a search to all indices in the cluster for the search string "knud knudsen".
* `size` : the size of the returned results. Default is 10
* `date_from / date_to `: search for created date in the form of `yyyy-MM-dd` or  `yyyy-MM` or  `yyyy`  
* `filter` : a terms filter if you want to limit the search results e.g `filter=type.exact#Brev`, you will only search within type `Brev`.
* `aggs`: you can specify aggregations as parameter. Aggregations must be a valid JSON arrays. For example 

    ``` 
    facets: [
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
                                                                                                                                            
    ``` 
                                                                                                                                            
* `pretty`: prettify results. It can hold explicit true values such as `true`, `1` or `on`. It is `false` by default.
* `sort`: you can sort by specifying the field that you want to sort and the sorting order separated by colon. For example, `available:asc` will sort the results in **asc**ending order of the filed `available`. You will have to make sure that the field exists and it is not analyzed.
* `service`: a service parameter tells Blackbox to construct a query based on a particular service, currently we have WAB, MARCUS, MARCUS_ADMIN and SKA services. Default is MARCUS service. We introduced  `service` because we would like to build query based on which data set we are querying. For instance, you would want to boost document of type "Postkort" in Marcus and of type "Skeivopedia" in Skeivtarkiv. You don't have to create a new service for each Elasticsearch index, these are just for our internal use. 
* `index_boost`: sometimes you would want to boost documents that belong to a specific index if you are querying multiple indices at the same time. Here comes `index_boost` which takes index_name as it's value. 


Another endpoint is [suggest endpoint](http://kirishima.uib.no/blackbox/suggest?=marcus) which is used for auto suggestion. The result is an array of the suggested values. For example `http://kirishima.uib.no/blackbox/suggest?q=marianne` gives a list of suggested values for string "marianne".
Suggest endpoint can take parameters such as `index` , `q` and `size`.

We have also support for *exclude API*. This means if one wants to exclude a terms facet, one will have to write in form of
`filter=-field#value`. Note the minus sign in front of field name. This query exclude documents of type `Fotografi` in Marcus :  http://marcus.uib.no/search/?filter=-type.exact%23Fotografi


## Installation 

* Clone Blackbox from the master branch 
* Add your cluster properties to `config-template.json` in the resource folder. Blackbox will look for these settings when initializing. See `config-template-test.json` in the resource folder for example settings.
* Build a war file with Maven `mvn clean build`
* Copy `war` file to a Tomcats `wabapps` folder