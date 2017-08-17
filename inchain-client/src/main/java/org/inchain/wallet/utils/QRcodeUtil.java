package org.inchain.wallet.utils;

import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class QRcodeUtil {

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    private QRcodeUtil() {
    }

    public static BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
            }
        }
        return image;
    }


    public static void writeToFile(BitMatrix matrix, String format, File file)
            throws IOException {
        BufferedImage image = toBufferedImage(matrix);
        if (!ImageIO.write(image, format, file)) {
            throw new IOException("Could not write an image of format "
                    + format + " to " + file);
        }
    }

    public static void writeToStream(BitMatrix matrix, String format,
                                     OutputStream stream) throws IOException {


        BufferedImage image = toBufferedImage(matrix);
        if (!ImageIO.write(image, format, stream)) {
            throw new IOException("Could not write an image of format "
                    + format);
        }
    }

    public static void genQrcode(String content, String path) {
        genQrcode(content, path, 198, 198);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void genQrcode(String content, String path, int width, int height) {
        try {
            Map hints = new HashMap();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;

            QRCode code = Encoder.encode(content, errorCorrectionLevel, hints);

            int quietZone = 4;
            if (hints != null) {
                ErrorCorrectionLevel requestedECLevel = (ErrorCorrectionLevel) hints.get(EncodeHintType.ERROR_CORRECTION);
                if (requestedECLevel != null) {
                    errorCorrectionLevel = requestedECLevel;
                }
                Integer quietZoneInt = (Integer) hints.get(EncodeHintType.MARGIN);
                if (quietZoneInt != null) {
                    quietZone = quietZoneInt;
                }
            }


            BitMatrix bitMatrix = renderResult(code, width, height, quietZone);
            File file1 = new File(path);
            File parentFile = file1.getParentFile();
            if(!parentFile.exists()) {
                parentFile.mkdirs();
            }

            QRcodeUtil.writeToFile(bitMatrix, "png", file1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void genQrcodeToStream(String content, OutputStream outputStream, int width, int height) {
        try {
            Map hints = new HashMap();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;

            QRCode code = Encoder.encode(content, errorCorrectionLevel, hints);

            int quietZone = 4;
            if (hints != null) {
                ErrorCorrectionLevel requestedECLevel = (ErrorCorrectionLevel) hints.get(EncodeHintType.ERROR_CORRECTION);
                if (requestedECLevel != null) {
                    errorCorrectionLevel = requestedECLevel;
                }
                Integer quietZoneInt = (Integer) hints.get(EncodeHintType.MARGIN);
                if (quietZoneInt != null) {
                    quietZone = quietZoneInt;
                }
            }


            BitMatrix bitMatrix = renderResult(code, width, height, quietZone);

            QRcodeUtil.writeToStream(bitMatrix, "png", outputStream);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void genQrcode(String content, String path, int width, int height,String ccbPath) {
        try {
            Map hints = new HashMap();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.L;

            QRCode code = Encoder.encode(content, errorCorrectionLevel, hints);

            int quietZone = 4;
            if (null != hints) {
                ErrorCorrectionLevel requestedECLevel = (ErrorCorrectionLevel) hints.get(EncodeHintType.ERROR_CORRECTION);
                if (requestedECLevel != null) {
                    errorCorrectionLevel = requestedECLevel;
                }

                Integer quietZoneInt = (Integer) hints.get(EncodeHintType.MARGIN);
                if (quietZoneInt != null) {
                    quietZone = quietZoneInt;
                }
            }

            BitMatrix bitMatrix = renderResult(code, width, height, quietZone);
            File file1 = new File(path);
            File parentFile = file1.getParentFile();
            if(!parentFile.exists()) {
                parentFile.mkdirs();
            }

            QRcodeUtil.writeToFile(bitMatrix, "png", file1,ccbPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage toBufferedImage(BitMatrix matrix,String ccbPath) throws IOException {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
            }
        }

        Image logo = ImageIO.read(new File(ccbPath));
        int widthLogo = logo.getWidth(null)>width*2/10?(width*2/10):logo.getWidth(null),
                heightLogo = logo.getHeight(null)>height*2/10?(height*2/10):logo.getWidth(null);

        int x = (width - widthLogo) / 2;
        int y = (height - heightLogo) / 2;
        Graphics2D gs = image.createGraphics();

        gs.setBackground(Color.WHITE);
        gs.drawImage(logo, x, y, widthLogo, heightLogo, null);
        gs.dispose();
        image.flush();
        return image;
    }

    public static void writeToFile(BitMatrix matrix, String format, File file,String ccbPath)
            throws IOException {
        BufferedImage image = toBufferedImage(matrix,ccbPath);
        if (!ImageIO.write(image, format, file)) {
            throw new IOException("Could not write an image of format "
                    + format + " to " + file);
        }
    }

    private static BitMatrix renderResult(QRCode code, int width, int height, int quietZone) {
        ByteMatrix input = code.getMatrix();
        if (input == null) {
            throw new IllegalStateException();
        }
        int inputWidth = input.getWidth();
        int inputHeight = input.getHeight();
        int qrWidth = inputWidth ;
        int qrHeight = inputHeight;
        int outputWidth = Math.max(width, qrWidth);
        int outputHeight = Math.max(height, qrHeight);

        int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);

        int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
        int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

        BitMatrix output = new BitMatrix(outputWidth, outputHeight);

        for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
            for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
                if (input.get(inputX, inputY) == 1) {
                    output.setRegion(outputX, outputY, multiple, multiple);
                }
            }
        }

        return output;
    }


}