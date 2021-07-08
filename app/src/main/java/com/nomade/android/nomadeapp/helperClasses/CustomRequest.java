package com.nomade.android.nomadeapp.helperClasses;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * CustomRequest
 *
 * Extension of the Volley Request class used to login.
 */
public class CustomRequest extends Request<Map<String,String>> {
    private final Response.Listener<Map<String,String>> mListener;

    public CustomRequest(int method, String url, Response.Listener<Map<String,String>> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
    }

    public CustomRequest(String url, Response.Listener<Map<String,String>> listener, Response.ErrorListener errorListener) {
        this(Method.GET, url, listener, errorListener);
    }

    @Override
    protected void deliverResponse(Map<String,String> response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<Map<String,String>> parseNetworkResponse(NetworkResponse response) {
        Map<String,String> responseHeaders = response.headers;

        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        responseHeaders.put("string_data", parsed);

        return Response.success(responseHeaders, HttpHeaderParser.parseCacheHeaders(response));
    }
}
