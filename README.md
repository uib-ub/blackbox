# README #

Blackbox is an application that acts as a bridge between Elasticsearch and client applications. It is developed using OOP best practices and uses Elasticsearch core API to communicate with the cluster.

Blackbox communicates with our Elasticsearch cluster in round-robbin fashion through Transport client. 

It runs as a tomcat application at http://kirishima.uib.no/blackbox/ and all client applications will be communicating with it here through HTTP transport. In this version, we are making it independent from the client implementations and can be queried separately. See f.eg http://kirishima.uib.no/blackbox/search?pretty=1