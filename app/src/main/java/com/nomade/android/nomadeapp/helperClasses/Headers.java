package com.nomade.android.nomadeapp.helperClasses;

import android.net.Uri;

import com.android.volley.VolleyLog;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Headers
 *
 * Defines the headers needed for the authentication with the server.
 */
public enum Headers {
    //According to RFC2617
    WWWAuthenticate("WWW-Authenticate", new String[]{"realm", "nonce", "opaque", "stale", "algorithm", "qop", "domain"}),
    Authorization(new String[]{"realm", "nonce", "opaque", "stale", "algorithm", "qop", "username", "uri", "cnonce", "nc", "response"}),
    AuthenticationInfo("Authentication-Info", new String[]{"qop", "nc", "digest", "nextnonce", "rspauth"});

    private String val;
    private String[] vals;

    Headers(String[] headerValues) {
        init(this.name(), headerValues);
    }

    Headers(String val, String[] headerValues) {
        init(val, headerValues);
    }

    private void init(String val, String[] headerValues) {
        this.val = val;
        Pattern regex;
        if (headerValues == null) {
            this.vals = new String[0];
            regex = Pattern.compile("");
        } else {
            this.vals = headerValues == null ? new String[0] : headerValues;
            regex = initPattern(this.vals);
        }
    }

    private Pattern initPattern(String[] vals) {
        StringBuilder concat = new StringBuilder();
        for (String v : vals) {
            if (concat.length() == 0)
                concat.append(val);
            else concat.append("|").append(val);
        }
        return Pattern.compile("(" + concat + ")=(\"?(\\w+)\"?)");
    }

    public String val() {
        return this.val;
    }

    public String[] headerValues() {
        return this.vals;
    }

    // Deprecated by updating to Volley 1.1.0
//    public Map<String, String> toMap(Header headerValue) {
//        LinkedHashMap<String, String> header = new LinkedHashMap<String,String>(this.vals.length);
//        for (HeaderElement he : headerValue.getElements()) {
//            for (String val : this.vals) {
//                if (he.getName().contains(val))
//                    header.put(val, he.getValue());
//            }
//        }
//        VolleyLog.d("Authorization values: %s", header);
//        return header;
//    }

    public String make(Map<String, String> values) {
        StringBuilder value = new StringBuilder("Digest ");
        boolean comma = false;
        for (String val : this.vals) {
            String hv = values.get(val);
            if (hv != null) {
                if (comma) value.append(", ");
                else comma = true;
                value.append(val).append("=");
                if ("nc".equals(val)) {
                    value.append(hv);
                } else {
                    value.append("\"").append(hv).append("\"");
                }
            }
        }
        VolleyLog.d("%s header: %s", this.val, value.toString());
        return value.toString();
    }

    public String make(Map<String, String> values, String currentUri) {
        StringBuilder value = new StringBuilder("Digest ");
        boolean comma = false;
        for (String val : this.vals) {
            if (val.equals("uri")){
                String hv = Uri.parse(currentUri).getPath();
                if (hv != null) {
                    if (comma) value.append(", ");
                    else comma = true;
                    value.append(val).append("=");
                    if ("nc".equals(val)) {
                        value.append(hv);
                    } else {
                        value.append("\"").append(hv).append("\"");
                    }
                }
            }
            else {
                String hv = values.get(val);
                if (hv != null) {
                    if (comma) value.append(", ");
                    else comma = true;
                    value.append(val).append("=");
                    if ("nc".equals(val)) {
                        value.append(hv);
                    } else {
                        value.append("\"").append(hv).append("\"");
                    }
                }
            }
        }
        VolleyLog.d("%s header: %s", this.val, value.toString());
        return value.toString();
    }
}
