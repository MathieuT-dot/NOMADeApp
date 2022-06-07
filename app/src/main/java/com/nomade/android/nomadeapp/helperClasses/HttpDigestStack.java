package com.nomade.android.nomadeapp.helperClasses;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.HurlStack;
import com.nomade.android.nomadeapp.activities.MainActivity;

import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

import static android.content.Context.MODE_PRIVATE;

/**
 * HttpDigestStack
 *
 * Http Digest Stack to handle authentication requests.
 */
class HttpDigestStack extends HurlStack {

    private static final String TAG = HttpDigestStack.class.getSimpleName();

    //    private static String cookie;
    private Proxy proxy;
    private DigestAuthenticator authenticator;

    private static SharedPreferences authSharedPreferences;
    private static SharedPreferences.Editor authEditor;

    public HttpDigestStack() {
        super();
        this.authenticator = new DigestAuthenticator() {
            @Override
            protected PasswordAuthentication requestPasswordAuthentication(String realm) {

//                MyLog.d(TAG, "Username: " + MainActivity.username + ", Password: " + new String(MainActivity.secret1) + ", Realm: " + realm);

                return new PasswordAuthentication(MainActivity.username, MainActivity.secret1);
            }

        };

        authSharedPreferences = AppController.getInstance().getSharedPreferences(Constants.AUTH_CACHE, MODE_PRIVATE);
        authEditor = authSharedPreferences.edit();
        authEditor.apply();
    }

    private HttpDigestStack(UrlRewriter urlRewriter) {
        super(urlRewriter);
    }

    private HttpDigestStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
        super(urlRewriter, sslSocketFactory);
    }

    private HttpDigestStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory, Proxy proxy) {
        super(urlRewriter, sslSocketFactory);
        this.proxy = proxy;
    }

    private HttpDigestStack(DigestAuthenticator auth, UrlRewriter urlRewriter) {
        super(urlRewriter);
        this.authenticator = auth;
    }

    private HttpDigestStack(DigestAuthenticator auth, UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
        super(urlRewriter, sslSocketFactory);
        this.authenticator = auth;
    }

    private HttpDigestStack(DigestAuthenticator auth, UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory, Proxy proxy) {
        super(urlRewriter, sslSocketFactory);
        this.proxy = proxy;
        this.authenticator = auth;
    }


    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {
        if (this.proxy == null)
            return super.createConnection(url);
        else return (HttpURLConnection) url.openConnection(proxy);
    }

    @Override
    public HttpResponse executeRequest (Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {

//        MyLog.d(TAG, "Initial");

//        MyLog.d(TAG,"Request Headers: " + request.getHeaders());

        // Uncomment for a list of all request headers
//        for (Map.Entry<String,String> entry: request.getHeaders().entrySet()){
//            MyLog.d(entry.getKey(), entry.getValue());
//        }

//        MyLog.d(TAG,"Additional Headers");
        Map<String, String> addHeaders = new LinkedHashMap<>();
        for (Map.Entry<String,String> entry: additionalHeaders.entrySet()){
            // Uncomment for a list of all additional headers
//            MyLog.d(entry.getKey(), entry.getValue());
            addHeaders.put(entry.getKey(), entry.getValue());
        }

        if (!request.getHeaders().containsKey(Headers.Authorization.val()) && !addHeaders.containsKey(Headers.Authorization.val())) {

//            MyLog.d(TAG, "Headers don't contain Authorization");

            authenticator.url = request.getUrl();
            authenticator.updateAuthToken(request);
            String auth = authenticator.getAuthToken();
            if (auth != null) {

//                MyLog.d(TAG, "Authenticator does contain auth");
//                MyLog.d(TAG, "auth: " + auth);

                addHeaders.put(Headers.Authorization.val(), auth);
//                additionalHeaders.put("Cookie", cookie);
            }
            else {

//                MyLog.d(TAG, "Authenticator doesn't contain auth");

                addHeaders.remove(Headers.Authorization.val());
                request.getHeaders().remove(Headers.Authorization.val());
            }
        }

        HttpResponse response = super.executeRequest(request, addHeaders);

        MyLog.d(TAG, "Response | Status code: " + response.getStatusCode() + ", URL: " + request.getUrl());

        List<Header> headersList = response.getHeaders();
        LinkedHashMap<String, String> headersMap = new LinkedHashMap<>();

        boolean authenticateHeaderPresent = false;

        for (Header header : headersList){
            // Uncomment for a list of all response headers
//            MyLog.d("Response headers", header.getName() + ": " + header.getValue());

            if (header.getName().toLowerCase().equals(Headers.WWWAuthenticate.val().toLowerCase())){
                headersMap.put(Headers.WWWAuthenticate.val(), header.getValue());
                authenticateHeaderPresent = true;
            }
            else {
                headersMap.put(header.getName(), header.getValue());
            }
        }

        switch (response.getStatusCode()){

            case 302: // Moved temporarily
                MyLog.d(TAG, "HttpStatus 302 (Moved temporarily)");
                break;

            case 400: // Bad Request
                MyLog.d(TAG, "HttpStatus 400 (Bad request)");

                MyLog.d(TAG, "UNAUTHORIZED FORCE LOG OUT");
                authenticator.invalidateAuthToken(Uri.parse(request.getUrl()).getHost());
                Intent intent1 = new Intent(AppController.getInstance(), MainActivity.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // TODO check clear task functionality
                intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent1.putExtra("log_out", true);
                AppController.getInstance().startActivity(intent1);
                return response;

            case 401: // Unauthorized
                MyLog.d(TAG, "HttpStatus 401 (Unauthorized)");

                boolean stale = false;
                if (authenticateHeaderPresent){
                    MyLog.d(TAG, "Stale: headers map contains www authenticate key");
                    Map<String, String> header = Utilities.parseHeaderToMap(headersMap.get(Headers.WWWAuthenticate.val()));
                    String staleString = header.get("stale");
                    if (staleString != null){
                        MyLog.d(TAG, "Stale: stale string is not null");
                        MyLog.d(TAG, "Stale: stale string: " + staleString);
                        stale = staleString.equals("true");
                    }
                    else {
                        MyLog.d(TAG, "Stale: stale string is null, stale = false");
                    }
                }
                else {
                    MyLog.d(TAG, "Stale: headers map doesn't contain www authenticate key, stale = false");
                }

                MyLog.d(TAG, "Stale value: " + stale);

                if (MainActivity.loggedIn && !stale){
                    MyLog.d(TAG, "UNAUTHORIZED FORCE LOG OUT");
                    authenticator.invalidateAuthToken(Uri.parse(request.getUrl()).getHost());
                    Intent intent2 = new Intent(AppController.getInstance(), MainActivity.class);
                    intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    // TODO check clear task functionality
                    intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent2.putExtra("log_out", true);
                    AppController.getInstance().startActivity(intent2);
                    return response;
                }
                else {
                    request.addMarker("auth-required");
                    if (authenticator.setAuthToken(request, response)) {

                        MyLog.d(TAG, "SetAuthToken successful");

                        request.addMarker("auth-set");

                        addHeaders.clear();

                        return executeRequest(request, addHeaders);
                    }
                }
                break;

            case 403: // Forbidden
                MyLog.d(TAG, "HttpStatus 403 (Forbidden)");
                return response;

            case 500: // Internal server error
                MyLog.d(TAG, "HttpStatus 500 (Internal server error)");
                break;
        }

        MyLog.d(TAG, "Return Response");

        return response;
    }


    static abstract class DigestAuthenticator implements com.android.volley.toolbox.Authenticator {

        private static final String SUB_TAG = DigestAuthenticator.class.getSimpleName();

        private Map<String, Map<String, String>> authCache = new HashMap<>();
        String url;
        private MessageDigest md;

        @Override
        public final String getAuthToken() throws AuthFailureError {
            Map<String, String> authValues = authCache.get(Uri.parse(url).getHost());
            if (authValues != null) {
                String qop = authValues.get("qop");
                if ("auth".equals(qop) || "auth-int".equals(qop)) {
                    String h1 = authValues.get("h1");
                    String h2 = authValues.get("h2");

                    MessageDigest md5 = null;
                    try {
                        md5 = getDigest(authValues.get("algorithm"));
                    } catch (NoSuchAlgorithmException e) {
                        throw new AuthFailureError("Unknown algorithm", e);
                    }
                    authValues.put("nc", incNonceCount(authValues.get("nc")));
                    authValues.put("response", digest(md5, h1, authValues.get("nonce"), authValues.get("nc"), authValues.get("cnonce"), qop, h2));
                }

                authEditor.putString(Uri.parse(url).getHost(), MapUtil.mapToString(authValues)).apply();

                return Headers.Authorization.make(authValues, url);
            }
            return null;
        }

        @Override
        public final void invalidateAuthToken(String authToken) {
            authCache.remove(authToken);
        }


        final MessageDigest getDigest(String algorithm) throws NoSuchAlgorithmException {
            if (md != null && md.getAlgorithm().equals(algorithm)) return this.md;
            if (algorithm == null)
                this.md = MessageDigest.getInstance("MD5");
            else
                this.md = MessageDigest.getInstance(algorithm);
            return this.md;
        }

        String incNonceCount(String nc) {
            if (nc == null || "".equals(nc)) return BigInteger.ONE.toString(16);
            else
                try {
                    BigInteger onc = new BigInteger(nc, 16);
                    return onc.add(BigInteger.ONE).toString(16);
                } catch (NumberFormatException e) {

                }
            return BigInteger.ONE.toString(16);
        }

        final String getCNonce(Map<String, String> header, String etag, String pwd) {
            if (!MainActivity.loggedIn) {
                try {
                    pwd = digest(getDigest("MD5"), pwd);
                }
                catch (NoSuchAlgorithmException e) {
                    MyLog.e(SUB_TAG,  "NoSuchAlgorithmException Error: " + e.toString() + ", " + e.getMessage());
                }
            }
            if (header.containsKey("cnonce")) return header.get("cnonce");

            String cnonce = "";
            if (etag != null && etag.length() > 0)
                cnonce += etag + ":";
            cnonce += Calendar.getInstance().getTimeInMillis() + ":";
            cnonce += pwd;
            MyLog.d(SUB_TAG, "Input cnonce: " + cnonce);
            cnonce = Base64.encodeToString(cnonce.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
            MyLog.d(SUB_TAG, "Full cnonce: " + cnonce);
            cnonce = cnonce.substring(10, 18);
            MyLog.d(SUB_TAG,"Substring cnonce: " + cnonce);
            return cnonce;
        }

        //if server supports auth and auth-int, use auth-int, if auth only, return auth and an empty string otherwise.
        private String getQopValue(String value) {
            if (value == null) return "";
            for (String s : value.split(",")) {
                if ("auth-int".equals(s.trim())) return s;
                if ("auth".equals(s.trim())) value = s;
            }
            return value;
        }

        private String digest(MessageDigest digest, String method, String uri, byte[] content) {
            digest.reset();
            digest.update(method.getBytes());
            digest.update(":".getBytes());
            digest.update(uri.getBytes());
            digest.update(":".getBytes());
            if (content != null)
                digest.update(content);
            return String.format("%032x", new BigInteger(1, digest.digest()));
        }

        private String digest(MessageDigest digest, String... s) {
            digest.reset();
            boolean nfirst = false;
            StringBuilder res = new StringBuilder();
            for (String str : s) {
                if (str != null) {
                    if (nfirst)
                        res.append(":");
                    else
                        nfirst = true;

                    res.append(str);
                }
            }
            MyLog.d(SUB_TAG, res.toString());
            res = new StringBuilder(String.format("%032x", new BigInteger(1, digest.digest(res.toString().getBytes()))));

            MyLog.d(SUB_TAG, res.toString());
            return res.toString();

        }

        final Boolean setAuthToken(Request request, HttpResponse httpResponse) throws AuthFailureError {
            try {
                List<Header> headersList = httpResponse.getHeaders();
                LinkedHashMap<String, String> headersMap = new LinkedHashMap<>();

                boolean authenticateHeaderPresent = false;

                for (Header header : headersList){

                    MyLog.d(SUB_TAG, header.getName() + ": " + header.getValue());

                    if (header.getName().toLowerCase().equals(Headers.WWWAuthenticate.val().toLowerCase())){
                        headersMap.put(Headers.WWWAuthenticate.val(), header.getValue());
                        authenticateHeaderPresent = true;
                    }
                    else {
                        headersMap.put(header.getName(), header.getValue());
                    }
                }

                if (!authenticateHeaderPresent) {
                    MyLog.w(SUB_TAG, "No WWW-Authenticate header found. Ignoring.");
                    return false;
                }
                URL url = new URL(request.getUrl());
                Map<String, String> header = Utilities.parseHeaderToMap(headersMap.get(Headers.WWWAuthenticate.val()));
                MyLog.d(SUB_TAG, "WWW-Authenticate header: " + headersMap.get(Headers.WWWAuthenticate.val()));
                String realm = header.get("realm");
                PasswordAuthentication auth = requestPasswordAuthentication(realm);
                if (auth != null) {
                    String algorithm = header.get("algorithm");
                    if (algorithm == null || "".equals(algorithm) || "MD5(-sess)*".matches(algorithm))
                        algorithm = "MD5";

                    MessageDigest md5 = getDigest(algorithm);

                    String pwd = new String(auth.getPassword());

                    String h1;

                    if (MainActivity.loggedIn){
                        h1 = new String(MainActivity.secret2);
                        MyLog.d(SUB_TAG,"h1: " + h1);
                    }
                    else {
                        h1 = digest(md5, auth.getUserName(), realm, pwd);
                        MyLog.d(SUB_TAG, "h1: " + h1 + ", user: " + auth.getUserName() + ", realm: " + realm + ", password: " + pwd);
                        if ("MD5-sess".equalsIgnoreCase(algorithm)) {//should be done only once by the spec
                            String hetag = null;
                            if (headersMap.containsKey("Etag")){
                                hetag = headersMap.get("Etag");
                            }
                            header.put("cnonce", getCNonce(header, hetag, pwd));
                            h1 = digest(md5, h1, header.get("nonce"), header.get("cnonce"));
                            MyLog.d(SUB_TAG, "h1: " + h1 + ", nonce: " + header.get("nonce") + ", cnonce: " + header.get("cnonce"));
                        }
                        MainActivity.secret2 = h1.toCharArray();
                    }

                    String uri = header.get("uri");
                    if (uri == null || "".equals(uri)) {
                        uri = request.getUrl();
                        uri = Uri.parse(uri).getPath();
                        header.put("uri", uri);
                    }

                    String qop = getQopValue(header.get("qop"));
                    MyLog.d(SUB_TAG, "Quality Of Protection: " + qop);
                    String h2;
                    if ("auth-int".equals(qop)) {
                        String method = requestMethod(request);
                        h2 = digest(md5, method, uri, request.getBody());
                        MyLog.d(SUB_TAG, "h2: " + h2 + ", method: " + method + ", uri: " + uri + ", body: " + request.getBody());
                    } else {
                        String method = requestMethod(request);
                        h2 = digest(md5, method, uri);
                        MyLog.d(SUB_TAG, "h2: " + h2 + ", method: " + method + ", uri: " + uri);
                    }
                    if ("auth".equals(qop) || "auth-int".equals(qop)) {
                        header.put("qop", qop);
                        String hetag = null;
                        if (headersMap.containsKey("Etag")){
                            hetag = headersMap.get("Etag");
                        }
                        header.put("cnonce", getCNonce(header, hetag, pwd));
                        header.put("h1", h1);
                        header.put("h2", h2);
                    } else {
                        header.put("response", digest(md5, h1, header.get("nonce"), h2));
                    }
                    String domain = header.remove("domain");
                    if (domain != null && domain.length() > 0) {
                        for (String uris : domain.split(" ")) {
                            authCache.put(uris, header);
                        }
                    }

//                    cookie = httpResponse.getFirstHeader("Set-Cookie").getValue();
//                    cookie = cookie.substring(0, cookie.indexOf("path") - 2);

                    header.put("username", auth.getUserName());
                    authCache.put(Uri.parse(request.getUrl()).getHost() , header);
                    authEditor.putString(Uri.parse(request.getUrl()).getHost(), MapUtil.mapToString(header)).apply();
                    MyLog.d(SUB_TAG, "SHAREDPREF PUT "  + Uri.parse(request.getUrl()).getHost() + ": " +  MapUtil.mapToString(header));
                    MyLog.d(SUB_TAG, "" + authCache);
                    MyLog.d(SUB_TAG,  "setAuthToken - " + Uri.parse(request.getUrl()).getHost() + ": " + authSharedPreferences.getString(Uri.parse(request.getUrl()).getHost(), "null"));
                    return true;
                } else return false;
            } catch (NoSuchAlgorithmException | MalformedURLException e) {
                throw new AuthFailureError("Cannot calculate header value", e);
            }
        }

        final Boolean updateAuthToken(Request request) throws AuthFailureError {
            try {
                MyLog.d(SUB_TAG, "host part of URL: " + Uri.parse(url).getHost());
                Map<String, String> authValues = authCache.get(Uri.parse(url).getHost());
                if (authValues == null){
                    String authValuesString = authSharedPreferences.getString(Uri.parse(request.getUrl()).getHost(), null);
                    if (authValuesString != null){
                        MyLog.d(SUB_TAG, "SHAREDPREF GET " + Uri.parse(request.getUrl()).getHost() + ": " + authValuesString);
                        authValues = MapUtil.stringToMap(authValuesString);
                        authCache.put(Uri.parse(url).getHost(), authValues);
                    }
                    else {
                        MyLog.w(SUB_TAG, "authValuesString == null (shared preferences are empty!");
                    }
                }
                if (authValues != null) {
                    String uri = authValues.get("uri");
                    uri = Uri.parse(uri).getPath();

                    String requestUrl  = request.getUrl();
                    requestUrl = Uri.parse(requestUrl).getPath();

//                    if (!uri.equals(requestUrl)){

                    authValues.put("uri", requestUrl);

                    String algorithm = authValues.get("algorithm");
                    if (algorithm == null || "".equals(algorithm) || "MD5(-sess)*".matches(algorithm))
                        algorithm = "MD5";

                    MessageDigest md5 = getDigest(algorithm);

                    String qop = getQopValue(authValues.get("qop"));
                    String h2;
                    if ("auth-int".equals(qop)) {
                        String method = requestMethod(request);
                        h2 = digest(md5, method, requestUrl, request.getBody());
                        MyLog.d(SUB_TAG, "h2: " + h2+ ", method: " + method + ", uri: " + requestUrl + ", body: " + request.getBody());
                    } else {
                        String method = requestMethod(request);
                        h2 = digest(md5, method, requestUrl);
                        MyLog.w(SUB_TAG, "h2: " + h2 + ", method: " + method + ", uri: " + requestUrl);
                    }
                    authValues.put("h2", h2);
                    return true;

//                    } else return false;

                } else {
                    MyLog.w(SUB_TAG, "authValues == null (shared preferences are empty!");
                    return false;
                }

            }
            catch (NoSuchAlgorithmException e){
                throw new AuthFailureError("Cannot calculate header value", e);
            }
        }

        private String requestMethod(Request request) {
            switch (request.getMethod()) {
                case Request.Method.GET:
                    return "GET";
                case Request.Method.POST:
                    return "POST";
                case Request.Method.PUT:
                    return "PUT";
                case Request.Method.DELETE:
                    return "DELETE";
                case Request.Method.HEAD:
                    return "HEAD";
                case Request.Method.OPTIONS:
                    return "OPTIONS";
                case Request.Method.TRACE:
                    return "TRACE";
                case Request.Method.PATCH:
                    return "PATCH";
                default:
                    return "GET";
            }

        }

        protected abstract PasswordAuthentication requestPasswordAuthentication(String realm);

    }

}
