package no.uib.marcus.search;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import no.uib.marcus.search.client.ClientFactory;
import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.base.Optional;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilderException;
import org.elasticsearch.search.sort.SortBuilder;

public class MarcusSearchService implements SearchService {

        private static final Logger logger = Logger.getLogger(MarcusSearchService.class);
        private @Nullable String[] indices; 
        private @Nullable String[] types;
        private String aggregations;
        private SortBuilder sort;
        private int from = -1;
        private int size = -1;
               
        public MarcusSearchService (){}
        
        public MarcusSearchService(String[] indices, String [] types, String aggregations, 
                int from, int size, SortBuilder sort) {
                this.indices = indices;
                this.types = types;
                this.aggregations = aggregations;
                this.from = from;
                this.size = size;
                this.sort = sort;
        }
        public String[] getIndices() {
                return indices;
        }

        public void setIndices(String[] indices) {
                this.indices = indices;
        }

        public String[] getTypes() {
                return types;
        }

        public void setTypes(String[] types) {
                this.types = types;
        }

        public String getAggregations() {
                return aggregations;
        }

        public void setAggregations(String aggregations) {
                this.aggregations = aggregations;
        }

        public int getFrom() {
                return from;
        }

        public void setFrom(int from) {
                this.from = from;
        }

        public int getSize() {
                return size;
        }

        public void setSize(int size) {
                this.size = size;
        }

        public SortBuilder getSort() {
                return sort;
        }

        public void setSort(SortBuilder sort) {
                this.sort = sort;
        }
        
        /** Get all documents**/
        @Override
        public SearchResponse getAllDocuments() {
                SearchResponse response = null;
                try {
                        //Prepare search request across all indices
                        response = ClientFactory
                                .getTransportClient()
                                .prepareSearch()
                                .setIndices(indices)
                                .setTypes(types)
                                .execute()
                                .actionGet();

                } catch (SearchSourceBuilderException se) {
                        logger.error("Exception on preparing the request: " + se.getDetailedMessage());
                } catch (Exception ex) {
                        logger.error("Exception: " + ex.getLocalizedMessage());
                }
                return response;

        }

        /**
         * Get all documents. 
         * <br />
         * @param queryStr - a query string, can be <code>NULL</code> which means match_all query.
         * 
         * @return a SearchResponse
         */
        @Override
        public SearchResponse getDocuments(@Nullable String queryStr) {
                SearchResponse response = null;
                SearchRequestBuilder searchRequest;
                QueryBuilder query;
                try {
                        //Prepare search request
                        searchRequest = ClientFactory
                                .getTransportClient()
                                .prepareSearch();
                        //Set indices
                        if (indices != null && indices.length > 0) {
                                searchRequest.setIndices(indices);
                        }
                        //Set types
                        if (types != null && types.length > 0) {
                                searchRequest.setTypes(types);
                        }
                        if (Strings.hasText(queryStr)) {
                                //Set query string
                                query = QueryBuilders.queryStringQuery(queryStr);
                        } else {
                                //Match all query
                                query = QueryBuilders.matchAllQuery();
                        }
                        //Set query
                        searchRequest.setQuery(query);
                        //from & size
                        searchRequest.setFrom(from);
                        searchRequest.setSize(size);
                        
                        if (sort != null) {
                                searchRequest.addSort(sort);
                        }
                        
                        if (Strings.hasText(aggregations)) {
                                //Append term aggregations to this request builder
                                this.addTermsAggregation(searchRequest);
                        }
                        //Show what has been sent to ES, for debugging..
                        logger.info(searchRequest.toString());
                        response = searchRequest
                                .execute()
                                .actionGet();
                } catch (SearchSourceBuilderException se) {
                        logger.error("Exception on preparing the request: " + se.getDetailedMessage());
                } catch (ElasticsearchException ex) {
                        logger.error("Exception: " + ex.getDetailedMessage());
                }
                return response;
        }

        /**
         * Get all documents. 
         * <br />
         *
         * @param queryStr - a query string, can be <code>NULL</code> which means match_all query.
         * @param filter  - a filter that will be embedded to the query
         * 
         * @return a SearchResponse
         */
        @Override
        public SearchResponse getDocuments(@Nullable String queryStr, FilterBuilder filter) {
                SearchResponse response = null;
                SearchRequestBuilder searchRequest;
                BoolFilterBuilder boolFilter = (BoolFilterBuilder) filter;
                QueryBuilder query;
                try {
                        //Prepare search request
                        searchRequest = ClientFactory
                                .getTransportClient()
                                .prepareSearch();

                        //Set indices
                        if (indices != null && indices.length > 0) {
                                searchRequest.setIndices(indices);
                        }
                        //Set types
                        if (types != null && types.length > 0) {
                                searchRequest.setTypes(types);
                        }

                        if (Strings.hasText(queryStr)) {
                                //Use query_string query
                                query = QueryBuilders.queryStringQuery(queryStr);
                        } else {
                                //Use match_all query
                                query = QueryBuilders.matchAllQuery();
                        }

                        searchRequest.setQuery(QueryBuilders
                                .filteredQuery(query, boolFilter));

                        searchRequest.setFrom(from);
                        searchRequest.setSize(size);

                        if (sort != null) {
                                searchRequest.addSort(sort);
                        }
                        
                        if (Strings.hasText(aggregations)) {
                                //Append term aggregations to this request builder
                               this.addTermsAggregation(searchRequest);
                        }
                        //Show Search builder for debugging purpose
                        logger.info(searchRequest.toString());

                        response = searchRequest
                                .execute()
                                .actionGet();
                } catch (SearchSourceBuilderException se) {
                        logger.error("Exception on preparing the request: "
                                + se.getDetailedMessage());
                } catch (ElasticsearchException ex) {
                        logger.error(ex.getDetailedMessage());
                }
                return response;
        }

        /**
         * A method to append terms aggregations to the search request builder.
         *
         * @param a search request
         *
         * @return the same search request where aggregations have been added to
         * it.
         */
        private SearchRequestBuilder addTermsAggregation(SearchRequestBuilder searchRequest) 
                throws ElasticsearchException {
                
                JsonElement jsonElement = new JsonParser().parse(aggregations);
                for (JsonElement facets : jsonElement.getAsJsonArray()) {
                        JsonObject currentFacet = facets.getAsJsonObject();
                        if (currentFacet.has("field")) {
                                String field = currentFacet.get("field").getAsString();
                                TermsBuilder termsBuilder = AggregationBuilders
                                        .terms(field)
                                        .field(field)
                                        .minDocCount(0);

                                //Set size
                                if (currentFacet.has("size")) {
                                        int size = currentFacet.get("size").getAsInt();
                                        termsBuilder.size(size);

                                }

                                //Set order
                                if (currentFacet.has("order")) {
                                        Order order = Order.count(false);
                                        if (currentFacet.get("order").getAsString().equalsIgnoreCase("count_asc")) {
                                                order = Order.count(true);
                                        } else if (currentFacet.get("order").getAsString().equalsIgnoreCase("term_asc")) {
                                                order = Order.term(true);
                                        } else if (currentFacet.get("order").getAsString().equalsIgnoreCase("term_desc")) {
                                                order = Order.term(false);
                                        }
                                        termsBuilder.order(order);
                                }

                                //Add aggregations to the search request builder
                                searchRequest.addAggregation(termsBuilder);
                        }

                }
                return searchRequest;
        }

        /**
         * A method to add extra field to every bucket in the terms
         * aggregations. The field value is queried independently from the
         * Elasticsearch.
         * <br />
         *
         * This method is in experimentation phase and maybe removed in the
         * future releases.
         *
         * @param a search response
         *
         * @return a JSON string of response where the extra field has been added to
         * each bucket aggregation.
         *
         */
        public String addExtraFieldToBucketsAggregation(SearchResponse response) {
               Gson gson = new GsonBuilder().setPrettyPrinting().create();
                if (response == null) {
                        throw new NullPointerException("Search response is NULL."
                                + " Cannot process aggregations. "
                                + "This means search response failed to execute");
                }
                JsonElement responseJson = new JsonParser().parse(response.toString());
                if (responseJson.getAsJsonObject().has("aggregations")) {
                        JsonObject aggs = responseJson.getAsJsonObject().get("aggregations").getAsJsonObject();
                        //Iterate throgh all the terms
                        for (Map.Entry<String, JsonElement> entry : aggs.entrySet()) {
                                //Get term 
                                String term = entry.getKey();
                                //Get value
                                JsonObject terms = entry.getValue().getAsJsonObject();
                                //Iterate throgh array of buckets of this term.
                                for (JsonElement element : terms.getAsJsonArray("buckets")) {
                                        JsonObject bucket = element.getAsJsonObject();
                                        String value = bucket.get("key").getAsString();

                                        /**
                                         * Query Elasticsearch independently for
                                         * count of the specified term.
                                         */
                                        CountRequestBuilder countRequestBuilder = ClientFactory
                                                .getTransportClient()
                                                .prepareCount();

                                        if (indices != null && indices.length > 0) {
                                                countRequestBuilder.setIndices(indices);
                                        }
                                        if (types != null && types.length > 0) {
                                                countRequestBuilder.setTypes(types);
                                        }
                                        /**
                                         * Build a count response*
                                         */
                                        CountResponse countResponse = countRequestBuilder
                                                .setQuery(QueryBuilders.termQuery(term, value))
                                                .execute()
                                                .actionGet();
                                        //Add this extra field "total_doc_count" to bucket aggregations
                                        bucket.addProperty("total_doc_count", countResponse.getCount());
                                }
                        }

                }

                return gson.toJson(responseJson);
        }

        /**
         * Print out properties of this instance as a JSON string
         **/
        public String toJsonString() throws IOException {
                XContentBuilder jsonObj = XContentFactory.jsonBuilder().prettyPrint()
                        .startObject()
                        .field("indices", this.getIndices()==null? Strings.EMPTY_ARRAY : getIndices())
                        .field("type", this.getTypes()==null? Strings.EMPTY_ARRAY : getTypes())
                        .field("from", this.getFrom())
                        .field("size", this.getSize())
                        .field("aggregations", this.getAggregations()==null? Strings.EMPTY_ARRAY : getAggregations())
                        .endObject();
                
                return jsonObj.string();
        }
        
        
        
        //Testing Aggregations
        public static void testAggRes(BoolFilterBuilder fb1) throws IOException, Exception {
                Map map = new HashMap();
                String json = "[{\"field\": \"status\", \"size\": 25},{\"field\" : \"assigned_to\"}]";
                //JsonElement el = new JsonParser().parse(json);

                map.put("status", "go_to_gate");
                BoolFilterBuilder fb = (BoolFilterBuilder) FilterBuilders
                        .boolFilter()
                        //.must(FilterBuilders.termFilter("status", "Draft"))
                        .should(FilterBuilders
                                .rangeFilter("created")
                                .from("1920")
                                .to(null)
                                .includeLower(true)
                                .includeUpper(true))
                        .should(FilterBuilders
                                .rangeFilter("madeafter")
                                .from("1920-01-01")
                                .to(null))
                        .should(FilterBuilders
                                .rangeFilter("madebefore")
                                .from(null)
                                .to("1920-01-01"));

                SearchRequestBuilder req = ClientFactory.getTransportClient()
                        .prepareSearch()
                        .setIndices("admin")
                        //.setTypes("invoice")
                        //.setQuery(QueryBuilders.matchAllQuery()) 
                        .setQuery(QueryBuilders.filteredQuery(QueryBuilders.queryStringQuery("*"), fb))
                        //.setPostFilter(fb1)
                        // .addAggregation(AggregationBuilders.global("_aggs")
                        .addAggregation((AggregationBuilders
                                .terms("status")
                                .field("status")))
                        //.subAggregation(AggregationBuilders.terms("by_assignee").field("assigned_to"))))               
                        .addAggregation(AggregationBuilders
                                .dateRange("created")
                                .field("created")
                                .addRange("created", "2010", null)
                        );

                SearchResponse res = req.execute().actionGet();

                // appendTermsAggregation(req, json);
                //.addAggregation(aggregation)
                //.addAggregation(AggregationBuilders.filter("filtered").filter(fb))
                //.addAggregation(AggregationBuilders.terms("status").field("status"))
                //.addAggregation(AggregationBuilders.terms("assigned_to").field("assigned_to"));
                //System.out.println("JSON Response: " +  gson.toJson(responseJson));
                System.out.println("Request: " + req.toString());
        }

        //Main method for easy debugging
        public static void main(String[] args) throws IOException, Exception {

                Object _from = Optional.fromNullable(null).or(10);

                //int _size = (int)Optional.fromNullable(Integer.parseInt(null)).or(10); 
                String s = "[{\"field\": \"status\", \"size\": 15, \"operator\" : \"AND\", \"order\": \"term_asc\"}]";
                // System.out.println("Map baby: " + facetMap.toString());
                //System.out.println(getAll("admin", null));
                //System.out.println(getDocuments("ma", "admin" , "invoice", null));
                //System.out.println("List of suggestion :" + Suggestion.getSuggestionsFor("m", "admin").toString());
                // String jsonString = gson.toJson(Suggestion.getSuggestions("m", "admin" , "suggest"));
                //System.out.println("List of suggestion :" + jsonString);
                //System.out.println("List of suggestion :" + Suggestion.getSuggestResponse("m", "admin" , "suggest"));
                //testAggRes(null);
                System.out.println((int) _from + " : ");
        }

}
