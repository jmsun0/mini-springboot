package com.sjm.core.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

public class HttpUtil {
    public static HttpBuilder build() {
        return new HttpBuilder();
    }

    public static final class HttpBuilder {
        private String protocol;
        private String host;
        private int port;
        private HttpHeader header = HttpEngine.getDefaultHeader();
        private ByteData data;
        private String url;
        private boolean holdCookies = HttpEngine.getHoldCookies();
        private URI uri;

        public HttpBuilder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public HttpBuilder setHost(String host) {
            this.host = host;
            return this;
        }

        public HttpBuilder setPort(int port) {
            this.port = port;
            return this;
        }

        public HttpBuilder setMethod(String method) {
            header.setRequestMethod(method);
            return this;
        }

        public HttpBuilder setPath(String path) {
            header.setRequestPath(path);
            return this;
        }

        public HttpBuilder setVersion(String version) {
            header.setRequestVersion(version);
            return this;
        }

        public HttpBuilder setHeader(String key, String value) {
            header.setAttr(key, value);
            return this;
        }

        public HttpBuilder setHeader(Map<String, String> headers) {
            for (Map.Entry<String, String> e : headers.entrySet())
                setHeader(e.getKey(), e.getValue());
            return this;
        }

        public HttpBuilder setData(ByteData data) {
            this.data = data;
            return this;
        }

        public HttpBuilder setURL(String url) {
            this.url = url;
            return this;
        }

        public HttpBuilder setHoldCookies(boolean holdCookies) {
            this.holdCookies = holdCookies;
            return this;
        }

        private void checkArguments() {
            if (url != null) {
                try {
                    URL u = new URL(url);
                    protocol = u.getProtocol();
                    host = u.getHost();
                    port = u.getPort();
                    header.setRequestPath(u.getFile());
                } catch (Exception e) {
                    throw new RuntimeException("URL格式错误", e);
                }
            } else {
                if (protocol == null)
                    protocol = "http";
                if (host == null)
                    throw new RuntimeException("http请求缺少主机名");
                if (port == 0)
                    port = -1;
                if (header.getRequestPath() == null)
                    throw new RuntimeException("http请求缺少路径");
            }
            if (holdCookies) {
                String path = header.getRequestPath();
                int i = path.indexOf('?');
                try {
                    uri = new URI(protocol, null, host, port, i == -1 ? path : path.substring(0, i),
                            i == -1 ? null : path.substring(i + 1), null);
                } catch (URISyntaxException e) {
                    throw new RuntimeException("URI格式错误", e);
                }
                String cookies = HttpEngine.getCookieString(uri);
                if (!cookies.isEmpty())
                    header.setAttr("Cookie", cookies);
            }
            if (header.getRequestMethod() == null)
                header.setRequestMethod("GET");
            if (header.getRequestVersion() == null)
                header.setRequestVersion("HTTP/1.1");
        }

        public HttpResult execute() {
            checkArguments();
            HttpResult r;
            try {
                r = HttpEngine.getInstance().http(protocol, host, port, header, data);
            } catch (IOException e) {
                throw new RuntimeException("http访问失败", e);
            }
            if (holdCookies) {
                String cookies = r.getHeader().getAttr("Set-Cookie");
                if (cookies != null)
                    HttpEngine.setCookieString(uri, cookies);
            }
            return r;
        }
    }
    public static final class HttpHeader implements Cloneable {
        private String[] head;
        private TreeMap<String, String> attrs;

        public HttpHeader(String[] head) {
            this.head = head;
            attrs = new TreeMap<String, String>(Misc.STRING_NOCASE_COMPARATOR);
        }

        public HttpHeader() {
            this(new String[3]);
        }

        @SuppressWarnings("unchecked")
        @Override
        public HttpHeader clone() {
            try {
                HttpHeader instance = (HttpHeader) super.clone();
                instance.head = head.clone();
                instance.attrs = (TreeMap<String, String>) attrs.clone();
                return instance;
            } catch (CloneNotSupportedException e) {
                throw new Error(e);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(head[0]).append(' ').append(head[1]).append(' ').append(head[2]).append('\r')
                    .append('\n');
            for (Entry<String, String> e : attrs.entrySet())
                if (e.getKey() != null)
                    sb.append(e.getKey()).append(':').append(e.getValue()).append('\r')
                            .append('\n');
            sb.append('\r').append('\n');
            return sb.toString();
        }

        public Map<String, String> getAttrs() {
            return attrs;
        }

        public String getAttr(String key) {
            return attrs.get(key);
        }

        public void setAttr(String key, String value) {
            attrs.put(key, value);
        }

        public String getRequestMethod() {
            return head[0];
        }

        public String getRequestPath() {
            return head[1];
        }

        public String getRequestVersion() {
            return head[2];
        }

        public void setRequestMethod(String method) {
            head[0] = method;
        }

        public void setRequestPath(String path) {
            head[1] = path;
        }

        public void setRequestVersion(String version) {
            head[2] = version;
        }

        public String getResponseVersion() {
            return head[0];
        }

        public String getResponseCode() {
            return head[1];
        }

        public String getResponseMessage() {
            return head[2];
        }

        public void setResponseVersion(String version) {
            head[0] = version;
        }

        public void setResponseCode(String code) {
            head[1] = code;
        }

        public void setResponseMessage(String message) {
            head[2] = message;
        }
    }
    public static abstract class HttpResult implements Closeable {
        public abstract ByteData getData() throws IOException;

        public abstract HttpHeader getHeader();

        protected int code;

        public int getCode() {
            if (code == 0)
                code = Integer.parseInt(getHeader().getResponseCode());
            return code;
        }

        @Override
        public abstract void close() throws IOException;

        @Override
        public String toString() {
            try {
                return "header:\n" + getHeader() + "\ndata:\n" + getData().toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static final class MyCookieStore implements CookieStore {
        private List<HttpCookie> cookieJar = null;
        private Map<String, List<HttpCookie>> domainIndex = null;
        private Map<URI, List<HttpCookie>> uriIndex = null;
        private ReentrantLock lock = null;

        public MyCookieStore() {
            cookieJar = new ArrayList<HttpCookie>();
            domainIndex = new HashMap<String, List<HttpCookie>>();
            uriIndex = new HashMap<URI, List<HttpCookie>>();
            lock = new ReentrantLock(false);
        }

        @Override
        public void add(URI uri, HttpCookie cookie) {
            if (cookie == null)
                throw new NullPointerException("cookie is null");
            lock.lock();
            try {
                cookieJar.remove(cookie);
                if (cookie.getMaxAge() != 0) {
                    cookieJar.add(cookie);
                    if (cookie.getDomain() != null) {
                        addIndex(domainIndex, cookie.getDomain(), cookie);
                    }
                    if (uri != null) {
                        addIndex(uriIndex, getEffectiveURI(uri), cookie);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public List<HttpCookie> get(URI uri) {
            if (uri == null) {
                throw new NullPointerException("uri is null");
            }
            List<HttpCookie> cookies = new ArrayList<HttpCookie>();
            boolean secureLink = "https".equalsIgnoreCase(uri.getScheme());
            lock.lock();
            try {
                getInternal1(cookies, domainIndex, uri.getHost(), secureLink);
                getInternal2(cookies, uriIndex, getEffectiveURI(uri), secureLink);
            } finally {
                lock.unlock();
            }
            return cookies;
        }

        @Override
        public List<HttpCookie> getCookies() {
            List<HttpCookie> rt;
            lock.lock();
            try {
                Iterator<HttpCookie> it = cookieJar.iterator();
                while (it.hasNext()) {
                    if (it.next().hasExpired()) {
                        it.remove();
                    }
                }
            } finally {
                rt = Collections.unmodifiableList(cookieJar);
                lock.unlock();
            }

            return rt;
        }

        @Override
        public List<URI> getURIs() {
            List<URI> uris = new ArrayList<URI>();

            lock.lock();
            try {
                Iterator<URI> it = uriIndex.keySet().iterator();
                while (it.hasNext()) {
                    URI uri = it.next();
                    List<HttpCookie> cookies = uriIndex.get(uri);
                    if (cookies == null || cookies.size() == 0) {
                        it.remove();
                    }
                }
            } finally {
                uris.addAll(uriIndex.keySet());
                lock.unlock();
            }

            return uris;
        }

        @Override
        public boolean remove(URI uri, HttpCookie ck) {
            if (ck == null) {
                throw new NullPointerException("cookie is null");
            }
            boolean modified = false;
            lock.lock();
            try {
                modified = cookieJar.remove(ck);
            } finally {
                lock.unlock();
            }
            return modified;
        }

        @Override
        public boolean removeAll() {
            lock.lock();
            try {
                cookieJar.clear();
                domainIndex.clear();
                uriIndex.clear();
            } finally {
                lock.unlock();
            }

            return true;
        }

        private boolean netscapeDomainMatches(String domain, String host) {
            if (domain == null || host == null) {
                return false;
            }
            boolean isLocalDomain = ".local".equalsIgnoreCase(domain);
            int embeddedDotInDomain = domain.indexOf('.');
            if (embeddedDotInDomain == 0) {
                embeddedDotInDomain = domain.indexOf('.', 1);
            }
            if (!isLocalDomain
                    && (embeddedDotInDomain == -1 || embeddedDotInDomain == domain.length() - 1)) {
                return false;
            }
            int firstDotInHost = host.indexOf('.');
            if (firstDotInHost == -1 && isLocalDomain) {
                return true;
            }
            int domainLength = domain.length();
            int lengthDiff = host.length() - domainLength;
            if (lengthDiff == 0) {
                return host.equalsIgnoreCase(domain);
            } else if (lengthDiff > 0) {
                String D = host.substring(lengthDiff);
                return (D.equalsIgnoreCase(domain));
            } else if (lengthDiff == -1) {
                return (domain.charAt(0) == '.' && host.equalsIgnoreCase(domain.substring(1)));
            }
            return false;
        }

        private void getInternal1(List<HttpCookie> cookies,
                Map<String, List<HttpCookie>> cookieIndex, String host, boolean secureLink) {
            ArrayList<HttpCookie> toRemove = new ArrayList<HttpCookie>();
            for (Map.Entry<String, List<HttpCookie>> entry : cookieIndex.entrySet()) {
                String domain = entry.getKey();
                List<HttpCookie> lst = entry.getValue();
                for (HttpCookie c : lst) {
                    if ((c.getVersion() == 0 && netscapeDomainMatches(domain, host))
                            || (c.getVersion() == 1 && HttpCookie.domainMatches(domain, host))) {
                        if ((cookieJar.indexOf(c) != -1)) {
                            if (!c.hasExpired()) {
                                if ((secureLink || !c.getSecure()) && !cookies.contains(c)) {
                                    cookies.add(c);
                                }
                            } else {
                                toRemove.add(c);
                            }
                        } else {
                            toRemove.add(c);
                        }
                    }
                }
                for (HttpCookie c : toRemove) {
                    lst.remove(c);
                    cookieJar.remove(c);

                }
                toRemove.clear();
            }
        }

        private <T> void getInternal2(List<HttpCookie> cookies,
                Map<T, List<HttpCookie>> cookieIndex, Comparable<T> comparator,
                boolean secureLink) {
            for (T index : cookieIndex.keySet()) {
                if (comparator.compareTo(index) == 0) {
                    List<HttpCookie> indexedCookies = cookieIndex.get(index);
                    if (indexedCookies != null) {
                        Iterator<HttpCookie> it = indexedCookies.iterator();
                        while (it.hasNext()) {
                            HttpCookie ck = it.next();
                            if (cookieJar.indexOf(ck) != -1) {
                                if (!ck.hasExpired()) {
                                    if ((secureLink || !ck.getSecure()) && !cookies.contains(ck))
                                        cookies.add(ck);
                                } else {
                                    it.remove();
                                    cookieJar.remove(ck);
                                }
                            } else {
                                it.remove();
                            }
                        }
                    }
                }
            }
        }

        private <T> void addIndex(Map<T, List<HttpCookie>> indexStore, T index, HttpCookie cookie) {
            if (index != null) {
                List<HttpCookie> cookies = indexStore.get(index);
                if (cookies != null) {
                    cookies.remove(cookie);

                    cookies.add(cookie);
                } else {
                    cookies = new ArrayList<HttpCookie>();
                    cookies.add(cookie);
                    indexStore.put(index, cookies);
                }
            }
        }

        private URI getEffectiveURI(URI uri) {
            URI effectiveURI = null;
            try {
                effectiveURI = new URI("http", uri.getHost(), null, null, null);
            } catch (URISyntaxException ignored) {
                effectiveURI = uri;
            }
            return effectiveURI;
        }
    }
    public static final class HttpResponseCode {
        private static HashMap<Integer, String> codeMap = new HashMap<Integer, String>();
        static {
            codeMap.put(100, "Continue");// 继续。客户端应继续其请求
            codeMap.put(101, "Switching Protocols");// 切换协议。服务器根据客户端的请求切换协议。只能切换到更高级的协议，例如，切换到HTTP的新版本协议
            codeMap.put(200, "OK");// 请求成功。一般用于GET与POST请求
            codeMap.put(201, "Created");// 已创建。成功请求并创建了新的资源
            codeMap.put(202, "Accepted");// 已接受。已经接受请求，但未处理完成
            codeMap.put(203, "Non-Authoritative Information");// 非授权信息。请求成功。但返回的meta信息不在原始的服务器，而是一个副本
            codeMap.put(204, "No Content");// 无内容。服务器成功处理，但未返回内容。在未更新网页的情况下，可确保浏览器继续显示当前文档
            codeMap.put(205, "Reset Content");// 重置内容。服务器处理成功，用户终端（例如：浏览器）应重置文档视图。可通过此返回码清除浏览器的表单域
            codeMap.put(206, "Partial Content");// 部分内容。服务器成功处理了部分GET请求
            codeMap.put(300, "Multiple Choices");// 多种选择。请求的资源可包括多个位置，相应可返回一个资源特征与地址的列表用于用户终端（例如：浏览器）选择
            codeMap.put(301, "Moved Permanently");// 永久移动。请求的资源已被永久的移动到新URI，返回信息会包括新的URI，浏览器会自动定向到新URI。今后任何新的请求都应使用新的URI代替
            codeMap.put(302, "Found");// 临时移动。与301类似。但资源只是临时被移动。客户端应继续使用原有URI
            codeMap.put(303, "See Other");// 查看其它地址。与301类似。使用GET和POST请求查看
            codeMap.put(304, "Not Modified");// 未修改。所请求的资源未修改，服务器返回此状态码时，不会返回任何资源。客户端通常会缓存访问过的资源，通过提供一个头信息指出客户端希望只返回在指定日期之后修改的资源
            codeMap.put(305, "Use Proxy");// 使用代理。所请求的资源必须通过代理访问
            codeMap.put(306, "Unused");// 已经被废弃的HTTP状态码
            codeMap.put(307, "Temporary Redirect");// 临时重定向。与302类似。使用GET请求重定向
            codeMap.put(400, "Bad Request");// 客户端请求的语法错误，服务器无法理解
            codeMap.put(401, "Unauthorized");// 请求要求用户的身份认证
            codeMap.put(402, "Payment Required");// 保留，将来使用
            codeMap.put(403, "Forbidden");// 服务器理解请求客户端的请求，但是拒绝执行此请求
            codeMap.put(404, "Not Found");// 服务器无法根据客户端的请求找到资源（网页）。通过此代码，网站设计人员可设置"您所请求的资源无法找到"的个性页面
            codeMap.put(405, "Method Not Allowed");// 客户端请求中的方法被禁止
            codeMap.put(406, "Not Acceptable");// 服务器无法根据客户端请求的内容特性完成请求
            codeMap.put(407, "Proxy Authentication Required");// 请求要求代理的身份认证，与401类似，但请求者应当使用代理进行授权
            codeMap.put(408, "Request Time-out");// 服务器等待客户端发送的请求时间过长，超时
            codeMap.put(409, "Conflict");// 服务器完成客户端的PUT请求是可能返回此代码，服务器处理请求时发生了冲突
            codeMap.put(410, "Gone");// 客户端请求的资源已经不存在。410不同于404，如果资源以前有现在被永久删除了可使用410代码，网站设计人员可通过301代码指定资源的新位置
            codeMap.put(411, "Length Required");// 服务器无法处理客户端发送的不带Content-Length的请求信息
            codeMap.put(412, "Precondition Failed");// 客户端请求信息的先决条件错误
            codeMap.put(413, "Request Entity Too Large");// 由于请求的实体过大，服务器无法处理，因此拒绝请求。为防止客户端的连续请求，服务器可能会关闭连接。如果只是服务器暂时无法处理，则会包含一个Retry-After的响应信息
            codeMap.put(414, "Request-URI Too Large");// 请求的URI过长（URI通常为网址），服务器无法处理
            codeMap.put(415, "Unsupported Media Type");// 服务器无法处理请求附带的媒体格式
            codeMap.put(416, "Requested range not satisfiable");// 客户端请求的范围无效
            codeMap.put(417, "Expectation Failed");// 服务器无法满足Expect的请求头信息
            codeMap.put(500, "Internal Server Error");// 服务器内部错误，无法完成请求
            codeMap.put(501, "Not Implemented");// 服务器不支持请求的功能，无法完成请求
            codeMap.put(502, "Bad Gateway");// 充当网关或代理的服务器，从远端服务器接收到了一个无效的请求
            codeMap.put(503, "Service Unavailable");// 由于超载或系统维护，服务器暂时的无法处理客户端的请求。延时的长度可包含在服务器的Retry-After头信息中
            codeMap.put(504, "Gateway Time-out");// 充当网关或代理的服务器，未及时从远端服务器获取请求
            codeMap.put(505, "HTTP Version not supported");// 服务器不支持请求的HTTP协议的版本，无法完成处理
        }

        public static String getMessage(int code) {
            return codeMap.get(code);
        }
    }

    public static abstract class HttpEngine {
        public abstract HttpResult http(String protocol, String host, int port, HttpHeader header,
                ByteData data) throws IOException;

        private static HttpEngine engine;

        static {
            engine = new DefaultHttpEngine();
        }

        public static HttpEngine getInstance() {
            return engine;
        }

        public static void setInstance(HttpEngine engine) {
            HttpEngine.engine = engine;
        }

        private static int defaultTimeOut = 5000;

        public static void setDefaultTimeOut(int time) {
            defaultTimeOut = time;
        }

        public static int getDefaultTimeOut() {
            return defaultTimeOut;
        }

        private static HttpHeader defaultHeader = new HttpHeader(new String[3]);

        public static void setDefaultHeader(HttpHeader header) {
            defaultHeader = header;
        }

        public static HttpHeader getDefaultHeader() {
            return defaultHeader.clone();
        }

        private static CookieStore cookieStore = new MyCookieStore();

        public static void setCookieStore(CookieStore store) {
            cookieStore = store;
        }

        public static String getCookieString(URI uri) {
            return Strings.combine(cookieStore.get(uri), ";");
        }

        public static void setCookieString(URI uri, String cookie) {
            for (HttpCookie c : HttpCookie.parse(cookie))
                cookieStore.add(uri, c);
        }

        private static boolean holdCookies = true;

        public static void setHoldCookies(boolean hold) {
            holdCookies = hold;
        }

        public static boolean getHoldCookies() {
            return holdCookies;
        }
    }
    public static class DefaultHttpEngine extends HttpEngine {
        @Override
        public HttpResult http(String protocol, String host, int port, HttpHeader header,
                ByteData data) throws IOException {
            return runHttp(protocol, host, port, header, data);
        }

        public static HttpResult runHttp(String protocol, String host, int port, HttpHeader header,
                ByteData data) throws IOException {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(protocol, host, port, header.getRequestPath())
                        .openConnection();
                conn.setRequestMethod(header.getRequestMethod());
                conn.setConnectTimeout(HttpEngine.getDefaultTimeOut());
                for (Entry<String, String> e : header.getAttrs().entrySet())
                    conn.setRequestProperty(e.getKey(), e.getValue());
                conn.setDoInput(true);
                if (data != null) {
                    conn.setDoOutput(true);
                    OutputStream os = null;
                    try {
                        os = conn.getOutputStream();
                        data.write(os);
                    } finally {
                        Misc.close(data);
                        Misc.close(os);
                    }
                }
                conn.connect();
                return new DefaultHttpResult(conn);
            } catch (IOException e) {
                if (conn != null)
                    conn.disconnect();
                throw e;
            }
        }

        public static class DefaultHttpResult extends HttpResult {
            private HttpURLConnection conn;
            private HttpHeader header;

            public DefaultHttpResult(HttpURLConnection conn) {
                this.conn = conn;
                header = new HttpHeader();
                for (Entry<String, List<String>> e : conn.getHeaderFields().entrySet()) {
                    String key = e.getKey();
                    List<String> value = e.getValue();
                    if (key != null)
                        header.setAttr(key, value.get(value.size() - 1));
                    else {
                        String[] head = value.get(0).split(" ");
                        header.setResponseVersion(head[0]);
                        header.setResponseCode(head[1]);
                        header.setResponseMessage(head.length == 3 ? head[2]
                                : HttpResponseCode.getMessage(Integer.parseInt(head[1])));
                    }
                }
            }

            @Override
            public ByteData getData() throws IOException {
                return ByteData.valueOf(conn.getInputStream());
            }

            @Override
            public HttpHeader getHeader() {
                return header;
            }

            @Override
            public void close() {
                conn.disconnect();
            }
        }
    }
}
