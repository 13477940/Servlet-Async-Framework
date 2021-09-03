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

package upload.internal;

import upload.PartOutput;
import upload.errors.PartSizeException;
import upload.errors.RequestSizeException;
import upload.interfaces.OnError;
import upload.interfaces.OnPartBegin;
import upload.interfaces.OnPartEnd;
import upload.interfaces.OnRequestComplete;
import upload.util.NullChannel;
import upload.util.OutputStreamBackedChannel;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardOpenOption.*;
import static java.util.Objects.requireNonNull;

/**
 * Base class for the parser implementations. This holds the common methods, like the more specific
 * validation and the calling of the user-supplied functions.
 */
public abstract class AbstractUploadParser implements MultipartParser.PartHandler {

    /**
     * The default size allocated for the buffers.
     */
    private static final int DEFAULT_USED_MEMORY = 4096;
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
    OnRequestComplete requestCallback;
    /**
     * The error callback, called when an error occurred.
     */
    OnError errorCallback;
    /**
     * The user object.
     */
    private Object userObject;
    /**
     * The number of bytes to be allocated for the buffers.
     */
    protected int maxBytesUsed = DEFAULT_USED_MEMORY;
    /**
     * The number of bytes that should be buffered before calling the part begin callback.
     */
    protected int sizeThreshold;
    /**
     * The maximum size permitted for the parts. By default it is unlimited.
     */
    private long maxPartSize = -1;
    /**
     * The maximum size permitted for the complete request. By default it is unlimited.
     */
    protected long maxRequestSize = -1;
    /**
     * The valid mime type.
     */
    protected static final String MULTIPART_FORM_DATA = "multipart/form-data";
    /**
     * The buffer that stores the first bytes of the current part.
     */
    protected ByteBuffer checkBuffer;
    /**
     * The channel to where the current part is written.
     */
    private WritableByteChannel writableChannel;
    /**
     * The known size of the request.
     */
    protected long requestSize;
    /**
     * The context instance.
     */
    protected UploadContextImpl context;
    /**
     * The reference to the multipart parser.
     */
    protected MultipartParser.ParseState parseState;
    /**
     * The buffer that stores the bytes which were read from the
     * servlet input stream or from a different source.
     */
    protected ByteBuffer dataBuffer;

    /**
     * Sets up the necessary objects to start the parsing. Depending upon
     * the environment the concrete implementations can be very different.
     * @param request The servlet request
     * @throws RequestSizeException If the supplied size is invalid
     */
    void init(final HttpServletRequest request) {

        // Fail fast mode
        if (maxRequestSize > -1) {
            final var requestSize = request.getContentLengthLong();
            if (requestSize > maxRequestSize) {
                throw new RequestSizeException("The size of the request (" + requestSize
                        + ") is greater than the allowed size (" + maxRequestSize + ")!", requestSize, maxRequestSize);
            }
        }

        checkBuffer = ByteBuffer.allocate(sizeThreshold);
        context = new UploadContextImpl(request, userObject);

        final var mimeType = request.getHeader(Headers.CONTENT_TYPE);
        if (mimeType != null && mimeType.startsWith(MULTIPART_FORM_DATA)) {
            final String boundary = Headers.extractBoundaryFromHeader(mimeType);
            if (boundary == null) {
                throw new IllegalArgumentException("Could not find boundary in multipart request with ContentType: "
                        + mimeType
                        + ", multipart data will not be available");
            }
            final var encodingHeader = request.getCharacterEncoding();
            final var charset = encodingHeader == null ? ISO_8859_1 : Charset.forName(encodingHeader);
            parseState = MultipartParser.beginParse(this, boundary.getBytes(charset), maxBytesUsed, charset);
        }
    }

    /**
     * Checks how many bytes have been read so far and stops the
     * parsing if a max size has been set and reached.
     * @param additional The amount to add, always non negative
     */
    void checkPartSize(final int additional) {
        final long partSize = context.incrementAndGetPartBytesRead(additional);
        if (maxPartSize > -1 && partSize > maxPartSize) {
            throw new PartSizeException("The size of the part ("
                    + partSize
                    + ") is greater than the allowed size ("
                    + maxPartSize
                    + ")!", partSize, maxPartSize);
        }
    }

    /**
     * Checks how many bytes have been read so far and stops the
     * parsing if a max size has been set and reached.
     * @param additional The amount to add, always non negative
     */
    void checkRequestSize(final int additional) {
        requestSize += additional;
        if (maxRequestSize > -1 && requestSize > maxRequestSize) {
            throw new RequestSizeException("The size of the request ("
                    + requestSize
                    + ") is greater than the allowed size ("
                    + maxRequestSize
                    + ")!", requestSize, maxRequestSize);
        }
    }

    @Override
    public void beginPart(final Headers headers) {
        final var disposition = headers.getHeader(Headers.CONTENT_DISPOSITION);
        if (disposition != null && disposition.startsWith("form-data")) {
            final var fieldName = Headers.extractQuotedValueFromHeader(disposition, "name");
            final var fileName = Headers.extractQuotedValueFromHeader(disposition, "filename");
            context.reset(new PartStreamImpl(fileName, fieldName, headers));
        }
    }

    @Override
    public void data(final ByteBuffer buffer) throws IOException {
        checkPartSize(buffer.remaining());
        copyBuffer(buffer);
        if (context.isBuffering() && context.getPartBytesRead() >= sizeThreshold) {
            validate(false);
        }
        if (!context.isBuffering()) {
            while (buffer.hasRemaining()) {
                writableChannel.write(buffer);
            }
        }
    }

    private void copyBuffer(final ByteBuffer buffer) {
        final var transferCount = Math.min(checkBuffer.remaining(), buffer.remaining());
        if (transferCount > 0) {
            checkBuffer.put(buffer.array(), buffer.arrayOffset() + buffer.position(), transferCount);
            buffer.position(buffer.position() + transferCount);
        }
    }

    private void validate(final boolean partFinished) throws IOException {
        context.finishBuffering();
        if (partFinished) {
            context.getCurrentPart().markAsFinished();
        }
        PartOutput output = null;
        checkBuffer.flip();
        if (partBeginCallback != null) {
            output = requireNonNull(partBeginCallback.onPartBegin(context, checkBuffer));
            if (output.safeToCast(WritableByteChannel.class)) {
                writableChannel = output.unwrap(WritableByteChannel.class);
            } else if (output.safeToCast(OutputStream.class)) {
                writableChannel = new OutputStreamBackedChannel(output.unwrap(OutputStream.class));
            } else if (output.safeToCast(Path.class)) {
                writableChannel = Files.newByteChannel(output.unwrap(Path.class), EnumSet.of(APPEND, CREATE, WRITE));
            } else {
                throw new IllegalArgumentException("Invalid output object!");
            }
        }
        if (output == null) {
            writableChannel = new NullChannel();
            output = PartOutput.from(writableChannel);
        }
        context.setOutput(output);
        checkBuffer.flip();
        while (checkBuffer.hasRemaining()) {
            writableChannel.write(checkBuffer);
        }
    }

    @Override
    public void endPart() throws IOException {
        if (context.isBuffering()) {
            validate(true);
        }
        context.getCurrentPart().markAsFinished();
        checkBuffer.clear();
        context.updatePartBytesRead();
        writableChannel.close();
        if (partEndCallback != null) {
            partEndCallback.onPartEnd(context);
        }
    }

    public void setPartBeginCallback(final OnPartBegin partBeginCallback) {
        this.partBeginCallback = partBeginCallback;
    }

    public void setPartEndCallback(final OnPartEnd partEndCallback) {
        this.partEndCallback = partEndCallback;
    }

    public void setRequestCallback(final OnRequestComplete requestCallback) {
        this.requestCallback = requestCallback;
    }

    public void setErrorCallback(final OnError errorCallback) {
        this.errorCallback = errorCallback;
    }

    public void setUserObject(final Object userObject) {
        this.userObject = userObject;
    }

    /**
     * Sets the amount of bytes to allocate. This is distributed
     * between the buffers used for raw parsing.
     * @param maxBytesUsed The amount to use
     */
    public void setMaxBytesUsed(final int maxBytesUsed) {
        // There are two byte buffers so each one gets half of the amount
        this.maxBytesUsed = maxBytesUsed / 2;
        this.dataBuffer = ByteBuffer.allocate(maxBytesUsed / 2);
    }

    public void setSizeThreshold(final int sizeThreshold) {
        this.sizeThreshold = sizeThreshold;
    }

    public void setMaxPartSize(final long maxPartSize) {
        this.maxPartSize = maxPartSize;
    }

    public void setMaxRequestSize(final long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }
}
