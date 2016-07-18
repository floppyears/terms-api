package edu.oregonstate.mist.termsapi.dao

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import edu.oregonstate.mist.api.jsonapi.ResourceObject
import edu.oregonstate.mist.termsapi.core.Attributes
import org.apache.http.HttpEntity
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.util.EntityUtils

class TermsDAO {
    private UtilHttp utilHttp
    private HttpClient httpClient
    private ObjectMapper mapper = new ObjectMapper()

    TermsDAO(UtilHttp utilHttp, HttpClient httpClient) {
        this.httpClient = httpClient
        this.utilHttp = utilHttp
    }

    /**
     * Performs class search and returns results in jsonapi format
     *
     * @param term
     * @param subject
     * @param courseNumber
     * @param q
     * @param pageNumber
     * @param pageSize
     * @return
     */
    public def getTerms(Integer pageNumber, Integer pageSize) {
        CloseableHttpResponse response
        def data = new ArrayList<ResourceObject>()
        def sourcePagination = [totalCount: 0, pageOffset: 0, pageMaxSize: 0]

        try {
            def query = getQueryMap(pageNumber, pageSize)
            response = utilHttp.sendGet(query, httpClient)

            HttpEntity entity = response.getEntity()
            def entityString = EntityUtils.toString(entity)

            def sourceData = this.mapper.readValue(entityString,
                    new TypeReference<List<HashMap>>() {
                })

            sourcePagination = getSourcePagination(response.getAllHeaders())
            EntityUtils.consume(entity)

            sourceData.each {
                data += getTermResourceObject(it)
            }
        } finally {
            response?.close()
        }

        [data: data, sourcePagination: sourcePagination]
    }

    /**
     * Performs class search and returns results in jsonapi format
     *
     * @param term
     * @param subject
     * @param courseNumber
     * @param q
     * @param pageNumber
     * @param pageSize
     * @return
     */
    public def getTerm(String term) {
        CloseableHttpResponse response
        def sourceData = []

        try {
            def query = [:]
            String backendPath = utilHttp.getBackendPath()
            utilHttp.setBackendPath(backendPath + "/${term}")

            response = utilHttp.sendGet(query, httpClient)

            HttpEntity entity = response.getEntity()
            def entityString = EntityUtils.toString(entity)

            sourceData = this.mapper.readValue(entityString,
                    new TypeReference<HashMap>() {
                })
            EntityUtils.consume(entity)
        } finally {
            response?.close()
        }

        getTermResourceObject(sourceData)
    }

    private static getSourcePagination(headers) {
        def headerMap = [:]
        headers.each {
            headerMap[it.name] = it.value
        }

        [
            totalCount: headerMap['X-hedtech-totalCount']?.toInteger(),
            pageOffset: headerMap['X-hedtech-pageOffset']?.toInteger(),
            pageMaxSize: headerMap['X-hedtech-pageMaxSize']?.toInteger()
        ]
    }

    /**
     * Constructs a resource object and associated attributes from map
     *
     * @param it
     */
    private static getTermResourceObject(it) {
        Attributes attributes = new Attributes(
                code:               it.code,
                description:        it.description,
                startDate:          it.startDate,
                endDate:            it.endDate,
                financialAidYear:   it.financialAidProcessingYear,
                housingStartDate:   it.housingStartDate,
                housingEndDate:     it.housingEndDate
        )

        new ResourceObject(id: it.code, type: 'term', attributes: attributes)
    }

    /**
     * Parses out the parameters and adds them to a map if they are not empty
     *
     * @param term
     * @param subject
     * @param courseNumber
     * @param q
     * @param pageNumber
     * @param pageSize
     * @return
     */
    private LinkedHashMap getQueryMap(Integer pageNumber, Integer pageSize) {
        def query = [offset:0]

        if (pageNumber && pageNumber > 1) {
            query['offset'] = pageSize * pageNumber
        }
        if (pageSize) {
            query['max'] = pageSize
        }
        query
    }
}
