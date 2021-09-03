/*
 * Copyright (C) 2016 Adam Forgacs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package upload;

import upload.errors.PartSizeException;
import upload.errors.RequestSizeException;
import upload.interfaces.*;
import upload.internal.AbstractUploadParser;
import upload.internal.AsyncUploadParser;
import upload.internal.BlockingUploadParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Locale;

/**
 * The main class for the library. Provides a fluent API for the users to
 * customize the parsing process.
 *
 * <p>The class is actually a builder class, it does not do the actual parsing. Instead,
 * when the user calls the {@link UploadParser#doBlockingParse(HttpServletRequest)} doBlockingParse}
 * or the {@link UploadParser#setupAsyncParse(HttpServletRequest)} setupAsyncParse} it creates
 * the actual parser object, determined by the configuration. This means that common
 * configuration can be kept in one place and the parser can be passed around and modified
 * freely. The servlet request object is not necessary before the actual parsing starts. In fact
 * the configured parser can be reused for each http request.</p>
 */
public final class UploadParser {

    /**
     * The default size allocated for the buffers.
     */
    private static final int DEFAULT_USED_MEMORY = 4096;

    /**
     * Part of HTTP content type header.
     */
    private static final String MULTIPART = "multipart/";

    /**
     * The part begin callback, called at the beginning of each part parsing.
     */
    private OnPartBegin partBeginCallback;

    /**
     * The part end callback, called at the end of each part parsing.
     */
    private OnPartEnd partEndCallback;

    /**
     * The request callback, called after every part has been processed.
     */
    private OnRequestComplete requestCallback;

    /**
     * The error callback, called when an error occurred.
     */
    private OnError errorCallback;

    /**
     * The user object.
     */
    private Object userObject;

    /**
     * The number of bytes to be allocated for the buffers.
     */
    private int maxBytesUsed = DEFAULT_USED_MEMORY;

    /**
     * The number of bytes that should be buffered before calling the part begin callback.
     */
    private int sizeThreshold;

    /**
     * The maximum size permitted for the parts. By default it is unlimited.
     */
    private long maxPartSize = -1;

    /**
     * The maximum size permitted for the complete request. By default it is unlimited.
     */
    private long maxRequestSize = -1;

    private UploadParser() {
        // No need to allow public access
    }

    /**
     * Sets a callback for each part, called at the beginning.
     * If you wish to skip the method invoke, pass a null
     * parameter or do not call this method.
     * @param partBeginCallback An object or lambda expression
     * @return The parser will return itself
     */
    public UploadParser onPartBegin(final OnPartBegin partBeginCallback) {
        this.partBeginCallback = partBeginCallback;
        return this;
    }

    /**
     * Sets a callback for each part, called at the end.
     * If you wish to skip the method invoke, pass a null
     * parameter or do not call this method.
     * @param partEndCallback An object or lambda expression
     * @return The parser will return itself
     */
    public UploadParser onPartEnd(final OnPartEnd partEndCallback) {
        this.partEndCallback = partEndCallback;
        return this;
    }

    /**
     * Sets a callback for the request, called after each part is processed.
     * If you wish to skip the method invoke, pass a null
     * parameter or do not call this method.
     * @param requestCallback An object or lambda expression
     * @return The parser will return itself
     */
    public UploadParser onRequestComplete(final OnRequestComplete requestCallback) {
        this.requestCallback = requestCallback;
        return this;
    }

    /**
     * Sets a callback for the errors, called if any error occurs.
     * If you wish to skip the method invoke, pass a null
     * parameter or do not call this method.
     * @param errorCallback An object or lambda expression
     * @return The parser will return itself
     */
    public UploadParser onError(final OnError errorCallback) {
        this.errorCallback = errorCallback;
        return this;
    }

    /**
     * Sets the user object, which can be anything. This is only
     * necessary to allow access to it during the stages of the
     * parsing. The most common use-case for this is passing
     * the servlet response object and setting the status code
     * on it depending on what happened. The object passed here can
     * be retrieved with {@link UploadContext#getUserObject(Class)} getUserObject}.
     *
     * <p>Note that if you have access to the object reference
     * where you declare the callback functions then this
     * method can be skipped and you can directly use that
     * reference, instead of retrieving it from the context.
     * @param userObject A custom user object
     * @return The parser will return itself
     */
    public UploadParser userObject(final Object userObject) {
        this.userObject = userObject;
        return this;
    }

    /**
     * Sets the amount of bytes to allocate for the parsing. A minimum
     * of 1024 is recommended. This amount does not include the size
     * specified in the {@link UploadParser#sizeThreshold(int)} sizeThreshold}
     * method.
     * @param maxBytesUsed The amount to use
     * @return The parser will return itself
     * @throws IllegalArgumentException If the parameter is negative
     */
    public UploadParser maxBytesUsed(final int maxBytesUsed) {
        if (maxBytesUsed < 2) {
            throw new IllegalArgumentException("The parameter value is too low, the parsing cannot work!");
        }
        this.maxBytesUsed = maxBytesUsed;
        return this;
    }

    /**
     * Sets the amount of bytes to buffer in the memory, before
     * calling the part end callback.
     * @param sizeThreshold The amount to use
     * @return The parser will return itself
     * @throws IllegalArgumentException If the parameter is negative
     */
    public UploadParser sizeThreshold(final int sizeThreshold) {
        if (sizeThreshold < 0) {
            throw new IllegalArgumentException("Invalid parameter value: " + sizeThreshold);
        }
        this.sizeThreshold = Math.max(sizeThreshold, 0);
        return this;
    }

    /**
     * Sets the maximum allowed size for each part. Exceeding this
     * will result in a {@link PartSizeException} exception.
     * @param maxPartSize The amount to use
     * @return The parser will return itself
     * @throws IllegalArgumentException If the parameter is negative
     */
    public UploadParser maxPartSize(final long maxPartSize) {
        if (maxPartSize < 0) {
            throw new IllegalArgumentException("Invalid parameter value: " + maxPartSize);
        }
        this.maxPartSize = maxPartSize;
        return this;
    }

    /**
     * Sets the maximum allowed size for the request. Exceeding this
     * will result in a {@link RequestSizeException} exception.
     * @param maxRequestSize The amount to use
     * @return The parser will return itself
     * @throws IllegalArgumentException If the parameter is negative
     */
    public UploadParser maxRequestSize(final long maxRequestSize) {
        if (maxRequestSize < 0) {
            throw new IllegalArgumentException("Invalid parameter value: " + maxRequestSize);
        }
        this.maxRequestSize = maxRequestSize;
        return this;
    }

    /**
     * Performs the necessary operations to setup the async parsing. The parser will
     * register itself to the request stream and the method will quickly return.
     * @param request The request object
     * @throws IOException If an error occurred with the request stream
     * @throws IllegalArgumentException If the request is not a multipart request
     */
    public void setupAsyncParse(final HttpServletRequest request) throws IOException {
        if (!isMultipart(request)) {
            throw new IllegalArgumentException("Not a multipart request!");
        }
        final var parser = new AsyncUploadParser(request);
        build(parser);
        parser.setupAsyncParse();
    }

    /**
     * The parser begins parsing the request stream. This is a blocking method,
     * the method will not finish until the upload process finished, either
     * successfully or not.
     * @param request The request object
     * @return The upload context
     * @throws IOException If an error occurred with the IO
     * @throws ServletException If an error occurred with the servlet stream
     * @throws IllegalArgumentException If the request is not a multipart request
     */
    public UploadContext doBlockingParse(final HttpServletRequest request) throws IOException, ServletException {
        if (!isMultipart(request)) {
            throw new IllegalArgumentException("Not a multipart request!");
        }
        final var parser = new BlockingUploadParser(request);
        build(parser);
        return parser.doBlockingParse();
    }

    /**
     * Passes the configuration parameters to the actual
     * parser implementation.
     * @param parser The parser implementation
     */
    private void build(final AbstractUploadParser parser) {
        parser.setPartBeginCallback(partBeginCallback);
        parser.setPartEndCallback(partEndCallback);
        parser.setRequestCallback(requestCallback);
        parser.setErrorCallback(errorCallback);
        parser.setUserObject(userObject);
        parser.setMaxBytesUsed(maxBytesUsed);
        parser.setSizeThreshold(sizeThreshold);
        parser.setMaxPartSize(maxPartSize);
        parser.setMaxRequestSize(maxRequestSize);
    }

    /**
     * Utility method which can be used to check whether the request
     * should be processed by this parser or not.
     * @param request The servlet request
     * @return Whether the request is a proper multipart request
     */
    public static boolean isMultipart(final HttpServletRequest request) {
        return request.getContentType() != null && request.getContentType().toLowerCase(Locale.ENGLISH).startsWith(MULTIPART);
    }

    /**
     * Returns a new parser, allowing the caller to set configuration.
     * @return A parser object
     */
    public static UploadParser newParser() {
        return new UploadParser();
    }
}
