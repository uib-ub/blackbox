package no.uib.marcus.search;

import com.google.gson.*;
import no.uib.marcus.search.client.ClientFactory;
import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilderException;
import org.elasticsearch.search.sort.SortBuilder;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;


public class MarcusSearchService implements SearchService, Serializable {

        private static final long serialVersionUID = 4L;

        private static final Logger logger = Logger.getLogger(MarcusSearchService.class);
        private
        @Nullable
        String[] indices;
        private
        @Nullable
        String[] types;
        private String aggregations;
        private SortBuilder sort;
        private int from = -1;
        private int size = -1;

        public MarcusSearchService() {
        }

        public MarcusSearchService(String[] indices, String[] types, String aggregations,
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

        public void setIndices(String... indices) {
                this.indices = indices;
        }

        public String[] getTypes() {
                return types;
        }

        public void setTypes(String... types) {
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

        /**
         * Get all documents.
         * <br />
         *
         * @return a SearchResponse
         */
        @Override
        public SearchResponse getAllDocuments() {
                return getDocuments(null);
        }

        /**
         * Get all documents that match a query string.
         * <br />
         *
         * @param queryStr a query string, can be <code>NULL</code> which means
         *                 match all documents.
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
                                //Use simple_query_string query
                                query = QueryBuilders.simpleQueryStringQuery(queryStr)
                                        .defaultOperator(SimpleQueryStringBuilder.Operator.AND);
                        } else {
                                //Match all documents query
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
                                //Append aggregations to this request builder
                                this.addAggregations(searchRequest);
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
         *
         * @param queryStr a query string, can be <code>NULL</code> which means
         *                 match all documents.
         * @param filter   a filter that will be embedded to the query
         * @return a SearchResponse
         */
        @Override
        public SearchResponse getDocuments(@Nullable String queryStr, FilterBuilder filter) {
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
                                //Use simple_query_string query
                                query = QueryBuilders.simpleQueryStringQuery(queryStr)
                                        .defaultOperator(SimpleQueryStringBuilder.Operator.AND);
                        } else {
                                //Use match_all query
                                query = QueryBuilders.matchAllQuery();
                        }
                        searchRequest.setQuery(QueryBuilders
                                .filteredQuery(query, filter));

                        searchRequest.setFrom(from);
                        searchRequest.setSize(size);

                        if (sort != null) {
                                searchRequest.addSort(sort);
                        }

                        if (Strings.hasText(aggregations)) {
                                //Append aggregations to this request builder
                                this.addAggregations(searchRequest);
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
         * @param searchRequest a search request
         * @return the same search request where aggregations have been added to
         * it.
         */
        private SearchRequestBuilder addAggregations(SearchRequestBuilder searchRequest)
                throws ElasticsearchException, JsonParseException {
                JsonElement jsonElement = new JsonParser().parse(aggregations);
                for (JsonElement facets : jsonElement.getAsJsonArray()) {
                        JsonObject currentFacet = facets.getAsJsonObject();
                        if (currentFacet.has("field")) {
                                //Add DateHistogram aggregations
                                if (currentFacet.has("type") && currentFacet.get("type").getAsString().equals("date_histogram")) {
                                        searchRequest.addAggregation(
                                                getDateHistogramAggregation(currentFacet)
                                        );
                                } else {
                                        //Add terms aggregations to the search request builder (this is default)
                                        searchRequest.addAggregation(
                                                getTermsAggregation(currentFacet)
                                        );
                                }
                        }

                }
                return searchRequest;
        }

        /**
         * A method to build a date histogram aggregation
         *
         * @param facet a JSON object
         * @return a date histogram builder
         */
        private DateHistogramBuilder getDateHistogramAggregation(JsonObject facet) {
                String field = facet.get("field").getAsString();
                //Create date histogram
                DateHistogramBuilder dateHistogram = AggregationBuilders
                        .dateHistogram(field)
                        .field(field);
                //Set date format
                if (facet.has("format")) {
                        dateHistogram.format(facet.get("format").getAsString());
                }
                //Set interval
                if (facet.has("interval")) {
                        dateHistogram.interval(new DateHistogram.Interval(facet.get("interval").getAsString()));
                }
                //Set number of minimum documents that should be returned
                if (facet.has("min_doc_count")) {
                        dateHistogram.minDocCount(facet.get("min_doc_count").getAsLong());
                }
                //Set order
                if (facet.has("order")) {
                        Histogram.Order order = Histogram.Order.KEY_ASC;
                        if (facet.get("order").getAsString().equalsIgnoreCase("count_asc")) {
                                order = Histogram.Order.COUNT_ASC;
                        } else if (facet.get("order").getAsString().equalsIgnoreCase("count_desc")) {
                                order = Histogram.Order.COUNT_DESC;
                        } else if (facet.get("order").getAsString().equalsIgnoreCase("key_desc")) {
                                order = Histogram.Order.KEY_DESC;
                        }
                        dateHistogram.order(order);
                }
                return dateHistogram;
        }

        /**
         * A method to build terms aggregations
         *
         * @param facet a JSON object
         * @return a term builder
         */
        private TermsBuilder getTermsAggregation(JsonObject facet){
                String field = facet.get("field").getAsString();
                TermsBuilder termsBuilder = AggregationBuilders
                        .terms(field)
                        .field(field);
                //Set size
                if (facet.has("size")) {
                        int size = facet.get("size").getAsInt();
                        termsBuilder.size(size);
                }
                //Set order
                if (facet.has("order")) {
                        Order order = Order.count(false);
                        if (facet.get("order").getAsString().equalsIgnoreCase("count_asc")) {
                                order = Order.count(true);
                        } else if (facet.get("order").getAsString().equalsIgnoreCase("term_asc")) {
                                order = Order.term(true);
                        } else if (facet.get("order").getAsString().equalsIgnoreCase("term_desc")) {
                                order = Order.term(false);
                        }
                        termsBuilder.order(order);
                }
                //Set number of minimum documents that should be returned
                if (facet.has("min_doc_count")) {
                        long minDocCount = facet.get("min_doc_count").getAsLong();
                        termsBuilder.minDocCount(minDocCount);
                }
                
                return termsBuilder;
        }
        /**
         * A method to add extra field to every bucket in the terms
         * aggregations. The field value is queried independently from the
         * Elasticsearch.
         * <p/>
         * This method is in experimentation phase and maybe removed in the
         * future releases.
         *
         * @param response a search response
         * @return a JSON string of response where the extra field has been
         * added to each bucket aggregation.
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
                        //Iterate through all the terms
                        for (Map.Entry<String, JsonElement> entry : aggs.entrySet()) {
                                //Get term
                                String term = entry.getKey();
                                //Get value
                                JsonObject terms = entry.getValue().getAsJsonObject();
                                //Iterate throgh array of buckets of this term.
                                for (JsonElement element : terms.getAsJsonArray("buckets")) {
                                        JsonObject bucket = element.getAsJsonObject();
                                        String value = bucket.get("key").getAsString();
                                        /*
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
                                        //Build a count response
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
         */
        public String toJsonString() throws IOException {
                XContentBuilder jsonObj = XContentFactory.jsonBuilder().prettyPrint()
                        .startObject()
                        .field("indices", indices == null ? Strings.EMPTY_ARRAY : indices)
                        .field("type", types == null ? Strings.EMPTY_ARRAY : types)
                        .field("from", from)
                        .field("size", size)
                        .field("aggregations", aggregations == null ? Strings.EMPTY_ARRAY : aggregations)
                        .endObject();

                return jsonObj.string();
        }

        //Testing Aggregations
        public static void testAggs() throws Exception {
                SearchRequestBuilder req = ClientFactory.getTransportClient()
                        .prepareSearch()
                        .setIndices("admin")
                        .setTypes("document")
                        .addAggregation(AggregationBuilders
                                .dateHistogram("created")
                                .field("created")
                                .format("yyyy")
                                .order(Histogram.Order.KEY_ASC)
                                //.extendedBounds("1997-01-01", "2016-01-01")
                                .interval(new DateHistogram.Interval("520w"))
                                .minDocCount(1)
                        );

                SearchResponse res = req.execute().actionGet();
                System.out.println("Request: " + req.toString());
                System.out.println("JSON Response: " + res.toString());

        }

        //Main method for easy debugging
        public static void main(String[] args) throws IOException, Exception {
                testAggs();
        }

}
