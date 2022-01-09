package RestAPIs;

import java.io.*;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;

public class RestAPIs {

    private String prepareConnect(String httpUrl, String method, String data, String param){
        HttpURLConnection httpURLConnection = null;
        try{
            HttpConfig httpConfig = new HttpConfig(httpUrl, method);
            httpURLConnection = httpConfig.getHttpURLConnection();
            httpURLConnection.connect();
            if (method.toLowerCase().equals("post")) {
                this.addParam(httpURLConnection, param);
            }

            if (param != null) {
                this.addParam(httpURLConnection, param);
            }

            if (data != null) {
                this.addData(httpURLConnection, data);
            }
            int code = httpURLConnection.getResponseCode();
            String msg = "";
            if (code == 200){
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null){
                    msg += line + "\n";
                }
                reader.close();
            }
            System.out.println("response: " + msg);
            return Integer.toString(code);
        }catch (IOException e){
            System.out.println(e);
        }finally {
            if (null != httpURLConnection){
                try {
                    httpURLConnection.disconnect();
                } catch (Exception e){
                    System.out.println("disconnect error: " + e);
                }
            }
        }
        return "error";
    }

    private String prepareConnect(String httpUrl, String method){
        return this.prepareConnect(httpUrl, method, null, null);
    }

    private String prepareConnect(String httpUrl, String method, String data){
        return this.prepareConnect(httpUrl, method, data, null);
    }

//    private String prepareConnect(String httpUrl, String method, String param){
//        return this.prepareConnect(httpUrl, method, null, param);
//    }

    private HttpURLConnection addHeader(HttpURLConnection httpURLConnection, Map<String, String> header){
        Header headerSet = new Header();
        if (null != header) {
            for (String key: header.keySet()){
                headerSet.setHeader(key, header.get(key));
            }
        }

        for (String key: headerSet.getHeader().keySet()){
            httpURLConnection.setRequestProperty(key, headerSet.getHeader().get(key));
        }
        return httpURLConnection;
    }

    private HttpURLConnection addHeader(HttpURLConnection httpURLConnection){
        return this.addHeader(httpURLConnection, null);
    }

    private HttpURLConnection addParam(HttpURLConnection httpURLConnection, String param){
        try {
            DataOutputStream dos = new DataOutputStream(httpURLConnection.getOutputStream());
            dos.writeBytes(param);
            dos.flush();
            dos.close();
        } catch (IOException e) {
            System.out.println("set param failed: " + e);
        }
        return httpURLConnection;
    }

    private HttpURLConnection addParam(HttpURLConnection httpURLConnection) {
        return this.addParam(httpURLConnection, null);
    }

    private HttpURLConnection addData(HttpURLConnection httpURLConnection, String data) throws IOException{
        byte[] postData = data.getBytes(StandardCharsets.UTF_8);
        DataOutputStream dos = new DataOutputStream(httpURLConnection.getOutputStream());
        dos.write(postData);
        dos.flush();
        dos.close();
        return httpURLConnection;
    }

    private HttpURLConnection addData(HttpURLConnection httpURLConnection){
        return httpURLConnection;
    }

    public String doGet(String httpUrl){
        return this.prepareConnect(httpUrl, "GET");
    }

    public String doPost(String httpUrl){
        return this.prepareConnect(httpUrl, "POST");
    }
}

class Header {
    private Map<String, String> header = new HashMap<>();

    public void Header(){
        Map<String, String> map = new HashMap<>();
        map.put("content-type", "application/json");
        map.put("origin", "https://www.bilibili.com");
        map.put("referer", "https://www.bilibili.com/");
        map.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36");
        this.header = map;
    }

    public Map<String, String> getHeader(){
        return this.header;
    }

    public void setHeader(String key, String value){
        this.header.put(key, value);
    }
}

class HttpConfig{
    private HttpURLConnection httpURLConnection = null;
    private String url = null;
    private String method = "GET";

    public HttpConfig(String url, String method){
        this.url = url;
        if (null != method && null != method.strip() ){
            this.method = method;
        }
    }

//    public void setHttpURLConnection(HttpURLConnection httpURLConnection) {
//        this.httpURLConnection = httpURLConnection;
//    }

    public HttpURLConnection getHttpURLConnection() throws IOException{
        URL url = new URL(this.url);
        if(this.httpURLConnection == null){
            this.httpURLConnection = (HttpURLConnection) url.openConnection();
            this.httpURLConnection.setDoOutput(true);
            this.httpURLConnection.setDoInput(true);
            this.httpURLConnection.setRequestMethod(this.method);
            this.httpURLConnection.setUseCaches(true);
            this.httpURLConnection.setInstanceFollowRedirects(true);
            this.httpURLConnection.setConnectTimeout(3000);
        }
        return this.httpURLConnection;
    }
}
