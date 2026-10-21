package com.mindspace.common.web;

import com.mindspace.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects oversized request bodies: by Content-Length up front, and by counting bytes for chunked bodies
 * that declare no length.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxBytes;

    public RequestSizeLimitFilter(AppProperties props) {
        this.maxBytes = props.request().maxBodyBytes();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long declared = request.getContentLengthLong();
        if (declared > maxBytes) {
            reject(response);
            return;
        }
        if (declared < 0 && request.getHeader("Transfer-Encoding") != null) {
            chain.doFilter(new LimitedRequest(request, maxBytes), response);
            return;
        }
        chain.doFilter(request, response);
    }

    private static void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"type\":\"urn:mindspace:problem:payload-too-large\","
                + "\"title\":\"Request body too large\",\"status\":413}");
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {
        private final long limit;

        LimitedRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream delegate = super.getInputStream();
            return new ServletInputStream() {
                private long count;

                @Override
                public int read() throws IOException {
                    int b = delegate.read();
                    if (b >= 0 && ++count > limit) {
                        throw new IOException("Request body too large");
                    }
                    return b;
                }

                @Override
                public int read(byte[] buffer, int off, int len) throws IOException {
                    int n = delegate.read(buffer, off, len);
                    if (n > 0) {
                        count += n;
                        if (count > limit) {
                            throw new IOException("Request body too large");
                        }
                    }
                    return n;
                }

                @Override
                public boolean isFinished() {
                    return delegate.isFinished();
                }

                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    delegate.setReadListener(listener);
                }
            };
        }
    }
}
