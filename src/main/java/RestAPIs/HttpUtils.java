package RestAPIs;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class HttpUtils {

    private CloseableHttpAsyncClient closeableHttpAsyncClient;
    private final static Timeout CONNECTION_TIMEOUT = Timeout.ofSeconds(5);
    private Map<String, String> params;
    private Map<String, String> headers;
    private String basicURL;

    public URI uriFactory() throws URISyntaxException {
        URIBuilder uri = new URIBuilder(basicURL);
        uri.setScheme("https")
                .addParameters(paramsKV(params));
        return uri.build();
    }

    public HttpUtils(String requestsJson) {
        // todo: 解析json字符串内容
        HttpAsyncClientBuilder httpClientBuilder = HttpAsyncClients.custom()
                .setDefaultHeaders(headersKV(headers))
                .setDefaultRequestConfig(configRequest());
        this.closeableHttpAsyncClient = httpClientBuilder.build();
        this.closeableHttpAsyncClient.start();
    }

    void doRequest(String method) throws URISyntaxException, ExecutionException, InterruptedException {
        SimpleHttpRequest request;
        // https://hc.apache.org/httpcomponents-client-5.1.x/migration-guide/migration-to-async-simple.html
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper objectMapper = new ObjectMapper(jsonFactory);

        if (method.toLowerCase().equals("get")) {
            request = SimpleHttpRequest.create(Method.GET.name(), uriFactory());
        } else if (method.toLowerCase().equals("post")) {
            request = SimpleHttpRequest.create(Method.POST.name(), uriFactory());
        } else {
            request = null;
        }
        if (request != null) {
            final Future<?> future =  this.closeableHttpAsyncClient.execute(
                    SimpleRequestProducer.create(request),
                    SimpleResponseConsumer.create(),
                    new FutureCallback<SimpleHttpResponse>() {

                @Override
                public void completed(SimpleHttpResponse response) {
                    try {
                        JsonNode responseData = objectMapper.readTree(response.getBodyText());
                        int status = new StatusLine(response).getStatusCode();
                        System.out.println("status: " + status);
                        System.out.println(responseData);
                    } catch (IOException ex) {
                        System.out.println("Error processing jSON content: " + ex.getMessage());
                    }
                }

                @Override
                public void failed(Exception ex) {
                    System.out.println("Error executing HTTP request: " + ex.getMessage());
                }

                @Override
                public void cancelled() {
                    System.out.println("HTTP request execution cancelled");
                }

            });
            future.get();
            this.closeableHttpAsyncClient.close(CloseMode.GRACEFUL);
        }
    }

    RequestConfig configRequest() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .build();
    }

    //header
    List<Header> headersKV(Map<String, String> headerMap) {
        List<Header> headers = new ArrayList<Header>();
        //TODO: default header...
        headers.add(new BasicHeader("", ""));
        if (headerMap == null) {
            return headers;
        }
        for (Map.Entry<String, String> entry: headerMap.entrySet()) {
            headers.add(new BasicHeader(entry.getKey(), entry.getValue()));
        }
        return headers;
    }

    List<Header> headersKV(String key, String value, List<Header> headers) {
        headers.add(new BasicHeader(key, value));
        return headers;
    }

    //params
    BasicNameValuePair paramsKV(String key, String value) {
        if (key == null || value == null || key.equals("") || value.equals("")) {
            return null;
        }
         return new BasicNameValuePair(key, value);
    }

    List<NameValuePair> paramsKV(Map<String, String> paramMap) {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        for (Map.Entry<String, String> entry: paramMap.entrySet()) {
            nameValuePairs.add(paramsKV(entry.getKey(), entry.getValue()));
        }
        return nameValuePairs;
    }

    // form params
    UrlEncodedFormEntity formEntity(String key, String value) {
        List<NameValuePair> formParams = new ArrayList<NameValuePair>();
        formParams.add(new BasicNameValuePair(key, value));
        return new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8);
    }

    // form payload
    StringEntity payloadEntity(String str) {
        return new StringEntity(str);
    }

    // post upload file
    HttpEntity fileUploadEntity(List<String> files) {
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        for (String file: files) {
            multipartEntityBuilder.addBinaryBody("file", new File(file));
        }
        return  multipartEntityBuilder.build();
    }

    //post multipart/form-data
    HttpEntity fileUploadEntity(Map<String, String> formData,List<String> files) {
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        for (Map.Entry<String, String> entry: formData.entrySet()) {
            multipartEntityBuilder.addTextBody(entry.getKey(), entry.getValue());
        }
        for (String file: files) {
            multipartEntityBuilder.addBinaryBody("file", new File(file));
        }
        return  multipartEntityBuilder.build();
    }
}
