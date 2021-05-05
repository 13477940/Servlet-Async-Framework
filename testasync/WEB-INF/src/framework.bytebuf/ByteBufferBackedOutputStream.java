package framework.bytebuf;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * https://stackoverflow.com/questions/4332264/wrapping-a-bytebuffer-with-an-inputstream
 */
public class ByteBufferBackedOutputStream extends OutputStream {

    private ByteBuffer backendBuffer;

    public ByteBufferBackedOutputStream(ByteBuffer backendBuffer) {
        Objects.requireNonNull(backendBuffer, "Given backend buffer can not be null!");
        this.backendBuffer = backendBuffer;
    }

    public void close() throws IOException {
        this.backendBuffer = null;
    }

    private void ensureStreamAvailable() throws IOException {
        if (this.backendBuffer == null) {
            throw new IOException("write on a closed OutputStream");
        }
    }

    @Override
    public void write(int b) throws IOException {
        this.ensureStreamAvailable();
        backendBuffer.put((byte) b);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        this.write(bytes, 0, bytes.length);
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        this.ensureStreamAvailable();
        Objects.requireNonNull(bytes, "Given buffer can not be null!");
        if ((off < 0) || (off > bytes.length) || (len < 0) ||
                ((off + len) > bytes.length) || ((off + len) < 0))
        {
            throw new IndexOutOfBoundsException();
        }
        else if (len == 0) {
            return;
        }

        backendBuffer.put(bytes, off, len);
    }

}
