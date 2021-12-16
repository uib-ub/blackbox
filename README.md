# Blackbox - query builder for Elasticsearch

![Alt text](src/main/webapp/images/blackbox.png?raw=true "Class diagrams")

## What is Blackbox?

Blackbox is an Elasticsearch query builder which takes client requests, process them and then send them to Elasticsearch. It is a server side application that acts as a bridge between Elasticsearch and client applications to simplify search experience. 

Blackbox uses Elasticsearch core API to communicate with Elasticsearch cluster in round-robbin 
fashion through built-in Elasticseach Transport client (transport client is deprecated in favor of HTTP client). Blackbox acts as one of the Elasticsearch nodes in the cluster. It runs as a [Web application](http://jambo.uib.no/blackbox) whereby clients communicate with it through HTTP.

## Why Blackbox?

The main reason is to simplify queries for search clients. This means search clients send simple queries to Blackbox,
and Blackbox transforms those queries into complex queries which Elasticsearch understands, and then send them to Elasticsearch.

### Example 

Marcus search client sends this query to Blackbox: https://marcus.uib.no/search/?q=nordnes&from_date=1800&filter=type.exact%23Fotografi . 
Blackbox takes the query, pre process it and transform it to complex Query DSL before sending it to Elasticsearch. 
See below on how the above query gets converted to. 

``` 
{
   "from":0,
   "size":10,
   "query":{
      "filtered":{
         "query":{
            "function_score":{
               "query":{
                  "query_string":{
                     "query":"nordnes",
                     "fields":[
                        "identifier",
                        "label^3.0",
                        "_all"
                     ],
                     "default_operator":"and",
                     "analyzer":"default"
                  }
               },
               "functions":[
                  {
                     "filter":{
                        "term":{
                           "type":"fotografi"
                        }
                     },
                     "weight":3.0
                  },
                  {
                     "filter":{
                        "term":{
                           "type":"bilde"
                        }
                     },
                     "weight":3.0
                  }
               ]
            }
         },
         "filter":{
            "bool":{
               "must":{
                  "term":{
                     "type.exact":"Fotografi"
                  }
               },
               "should":[
                  {
                     "range":{
                        "created":{
                           "from":"1800-01-01",
                           "to":null,
                           "include_lower":true,
                           "include_upper":true
                        }
                     }
                  },
                  {
                     "bool":{
                        "must":{
                           "range":{
                              "madeAfter":{
                                 "from":"1800-01-01",
                                 "to":null,
                                 "include_lower":true,
                                 "include_upper":true
                              }
                           }
                        }
                     }
                  },
                  {
                     "bool":{
                        "must":{
                           "range":{
                              "madeBefore":{
                                 "from":"1800-01-01",
                                 "to":null,
                                 "include_lower":true,
                                 "include_upper":true
                              }
                           }
                        }
                     }
                  }
               ]
            }
         }
      }
   }
}   
``` 

Nevertheless, letting search clients access Elasticsearch cluster directly may pose some security threats, 
and thus we decided that the search traffic should pass only through Blackbox which then supports only GET requests. 



## Usage and query parameters
Currently Blackbox supports 3 endpoints Search, Suggest and Discover.

- Search - for search and supports full range of query parameters  e.g https://jambo.uib.no/blackbox/search?q=marcus
- Suggest - for auto suggestions e.g https://jambo.uib.no/blackbox/suggest?q=marcus
- Discover - for experimental endpoint just for discovering data  e.g https://jambo.uib.no/blackbox/discover?q=marcus

 [Search endpoint](http://jambo.uib.no/blackbox/search) can take full range of parameters, such as the followings:-

* `index` : the index that the search should be executed. It can be more than one indices, e.g http://jambo.uib.no/blackbox/search?index=ska2&index=admin-test. If not specified, all indices in the cluster will be considered.
* `type` : same as index, See Elasticsearch type.
* `q` : a query string. For example, http://jambo.uib.no/blackbox/search?q=knud+knudsen will perform a search to all indices in the cluster for the search string "knud knudsen".
* `size` : the size of the returned results. Default is 10
* `date_from / date_to `: search for created date in the form of `yyyy-MM-dd` or  `yyyy-MM` or  `yyyy`  
* `filter` : a terms filter if you want to limit the search results e.g `filter=type.exact#Brev`, you will only search within type `Brev`.
* `aggs`: you can specify aggregations as parameter. Aggregations must be a valid JSON arrays. For example 

``` 
 facets :[
   {
      "field":"type",
      "size":30,
      "operator":"OR",
      "order":"count_desc",
      "min_doc_count":0
   },
   {
      "field":"hasZoom",
      "size":10,
      "operator":"AND",
      "order":"term_asc",
      "min_doc_count":0
   }
]
    
   ``` 
                                                                                                                                            
* `pretty`: prettify results. It can hold explicit true values such as `true`, `1` or `on`. It is `false` by default.
* `sort`: you can sort by specifying the field that you want to sort and the sorting order separated by colon. For example, `available:asc` will sort the results in **asc**ending order of the filed `available`. You will have to make sure that the field exists and it is not analyzed.
* `service`: a service parameter tells Blackbox to construct a query based on a particular service, currently we have WAB, MARCUS, MARCUS_ADMIN and SKA services. Default is MARCUS service. We introduced  `service` because we would like to build query based on which data set we are querying. For instance, you would want to boost document of type "Postkort" in Marcus and of type "Skeivopedia" in Skeivtarkiv. You don't have to create a new service for each Elasticsearch index, these are just for our internal use. 
* `index_boost`: sometimes you would want to boost documents that belong to a specific index if you are querying multiple indices at the same time. Here comes `index_boost` which takes index_name as it's value. 


Another endpoint is [suggest endpoint](http://jambo.uib.no/blackbox/suggest?=marcus) which is used for auto suggestion. The result is an array of the suggested values. For example `http://jambo.uib.no/blackbox/suggest?q=marianne` gives a list of suggested values for string "marianne".
Suggest endpoint can take parameters such as `index` , `q` and `size`.

We have also support for *exclude API*. This means if one wants to exclude a terms facet, one will have to write in form of
`filter=-field#value`. Note the minus sign in front of field name. This query exclude documents of type `Fotografi` in Marcus :  http://marcus.uib.no/search/?filter=-type.exact%23Fotografi


## Installation instructions

* Install tomcat
* Install maven
* Clone Blackbox from the master branch
* Copy the template file `config.template.example.json` from `/src/main/resources/` and give it a name `config.template.json`.  In the file `config.template.json`, you need to change `cluster.name` and `cluster.host` to the same names as your Elasticsearch cluster settings. 
* Go to the root directory of Blackbox and build a war file with Maven `mvn clean build`
* Copy `war` file (which should be in `target` folder) to a Tomcats `wabapps` folder and simply call it `blackbox.war`. 
* You should then be able to access the blackbox by server URL e.g `localhost/blackbox/`
