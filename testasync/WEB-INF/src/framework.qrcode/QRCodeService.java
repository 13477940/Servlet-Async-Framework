package framework.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

/**
 * 實作 QRCode 圖形產生器
 *
 * require jar
 * https://mvnrepository.com/artifact/com.google.zxing/core
 * https://mvnrepository.com/artifact/com.google.zxing/javase/3.4.1
 *
 * QRCode Image Generator
 */
public class QRCodeService {

    private final String exten = "png"; // use png for transparent
    private final HashMap<EncodeHintType, Object> hints = new HashMap<>();

    private QRCodeService(String character, ErrorCorrectionLevel level, Integer margin) {
        hints.put(EncodeHintType.CHARACTER_SET, character);
        hints.put(EncodeHintType.ERROR_CORRECTION, level);
        hints.put(EncodeHintType.MARGIN, margin);
    }

    /**
     * QRCode image to base64 string
     * base64 header -> data:image/png;base64,[base64string]
     * 主要用於傳遞到前端藉由 base64 資源路徑圖片呈現
     */
    public String toBase64String(String content, int width, int height) {
        String result = null;
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height);
            BufferedImage bi = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ImageIO.write(bi, exten, baos);
            result = Base64.getEncoder().withoutPadding().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * QRCode image to output stream
     * 使用時要注意 HTTP Response 的 Header 要是 "Content-Type":"image/png;charset=UTF-8"
     */
    public void writeToStream(String content, OutputStream stream, int width, int height, Handler handler) throws WriterException, IOException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        MatrixToImageWriter.writeToStream(bitMatrix, exten, stream);
        {
            if(null != handler) {
                Bundle b = new Bundle();
                b.putString("status", "done");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
        }
    }

    /**
     * QRCode image to buffered image
     */
    public BufferedImage toBufferedImage(String content, int width, int height) throws WriterException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * QRCode image to disk file
     */
    public void createImageFile(String content, String path, int width, int height) throws WriterException, IOException {
        BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
        // toPath() required jdk 1.7+
        MatrixToImageWriter.writeToPath(bitMatrix, exten, new File(path).toPath());
    }

    public static class Builder {

        private final HashMap<EncodeHintType, Object> hints = new HashMap<>();

        public QRCodeService.Builder setCharacter(String character) {
            if(null != character) hints.put(EncodeHintType.CHARACTER_SET, character);
            return this;
        }

        public QRCodeService.Builder setErrorCorrection(ErrorCorrectionLevel level) {
            if(null != level) hints.put(EncodeHintType.ERROR_CORRECTION, level);
            return this;
        }

        public QRCodeService.Builder setMargin(Integer pixel) {
            if(null != pixel) hints.put(EncodeHintType.MARGIN, pixel);
            return this;
        }

        public QRCodeService build() {
            if(!hints.containsKey(EncodeHintType.CHARACTER_SET)) hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            if(!hints.containsKey(EncodeHintType.ERROR_CORRECTION)) hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            if(!hints.containsKey(EncodeHintType.MARGIN)) hints.put(EncodeHintType.MARGIN, 0);
            return new QRCodeService(
                    (String) hints.get(EncodeHintType.CHARACTER_SET),
                    (ErrorCorrectionLevel) hints.get(EncodeHintType.ERROR_CORRECTION),
                    (Integer) hints.get(EncodeHintType.MARGIN)
            );
        }

    }

}
