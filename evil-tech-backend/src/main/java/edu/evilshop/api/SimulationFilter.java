package edu.evilshop.api;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounds JSON bodies, including chunked input, before deserialization. */
@Component
public class SimulationFilter extends OncePerRequestFilter {
    private static final int MAX_BODY=32768;
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        response.setHeader("X-Simulation","university-demo");
        response.setHeader("Cache-Control","no-store");
        response.setHeader("X-Content-Type-Options","nosniff");
        if (request.getContentLengthLong()>MAX_BODY) { tooLarge(response); return; }
        if (request.getMethod().equals("POST") || request.getMethod().equals("PUT")) {
            byte[] body=request.getInputStream().readNBytes(MAX_BODY+1);
            if (body.length>MAX_BODY) { tooLarge(response); return; }
            HttpServletRequestWrapper wrapped=new HttpServletRequestWrapper(request) {
                @Override public ServletInputStream getInputStream() {
                    ByteArrayInputStream bytes=new ByteArrayInputStream(body);
                    return new ServletInputStream() {
                        public int read() { return bytes.read(); }
                        public int read(byte[] b,int off,int len) { return bytes.read(b,off,len); }
                        public boolean isFinished() { return bytes.available()==0; }
                        public boolean isReady() { return true; }
                        public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Synchronous demo API"); }
                    };
                }
                @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8)); }
            };
            chain.doFilter(wrapped,response);
        } else chain.doFilter(request,response);
    }
    private void tooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(413); response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"BODY_TOO_LARGE\",\"message\":\"Maximum request body is 32768 bytes.\",\"simulated\":true}");
    }
}
