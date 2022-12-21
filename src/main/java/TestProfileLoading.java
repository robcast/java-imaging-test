import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.FileImageInputStream;

public class TestProfileLoading {

    static Logger logger = Logger.getLogger(TestProfileLoading.class.getName());

    static {
        // set log level from property
        String lvlVal = System.getProperty("java.util.logging.ConsoleHandler.level");
        if (lvlVal != null) {
            Level level = Level.parse(lvlVal);
            for (Handler h : Logger.getLogger("").getHandlers())
                h.setLevel(level);
            logger.setLevel(level);
        }
    }

    private static BufferedImage loadImage(String mt, String fn, boolean destSrgb)
            throws Exception, FileNotFoundException, IOException {
        // let ImageIO choose Reader type
        ImageReader reader = null;
        logger.fine("getting ImageReader for type " + mt);
        Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(mt);
        if (readers.hasNext()) {
            reader = readers.next();
        } else {
            throw new Exception("Can't find Reader to load File with mime-type " + mt + "!");
        }
        // set input
        RandomAccessFile rf = new RandomAccessFile(fn, "r");
        FileImageInputStream istream = new FileImageInputStream(rf);
        reader.setInput(istream);
        // choose default readParam
        ImageReadParam readParam = reader.getDefaultReadParam();
        // show alternative imageTypes
        for (Iterator<ImageTypeSpecifier> i = reader.getImageTypes(0); i.hasNext();) {
            ImageTypeSpecifier type = (ImageTypeSpecifier) i.next();
            ColorModel cm = type.getColorModel();
            ColorSpace cs = cm.getColorSpace();
            logger.fine(
                    "  possible destination color model: " + cm + " color space: " + cs + " is sRGB=" + cs.isCS_sRGB());
            if (destSrgb && cs.isCS_sRGB()) {
                logger.fine("    selected as destination");
                readParam.setDestinationType(type);
            }
        }

        // read image
        logger.fine("Loading file: " + fn);
        logger.fine("Using reader: " + reader);
        BufferedImage img = reader.read(0, readParam);
        return img;
    }

    private static Map<String, Integer> getPixels(BufferedImage img) {
        HashMap<String, Integer> res = new HashMap<String, Integer>();

        // check upper left pixel (DCI-P3 outside sRGB)
        int x1 = 5;
        int y1 = 5;
        int pixel1 = img.getRGB(x1, y1);
        Color color1 = new Color(pixel1);
        res.put("px1-red-srgb", color1.getRed());
        logger.fine("pixel1 sRGB: " + color1);
        int[] pixel1r = img.getRaster().getPixel(x1, y1, new int[4]);
        res.put("px1-red-raw", pixel1r[0]);
        logger.fine("pixel1 raw: " + Arrays.toString(pixel1r));

        // check lower right pixel (DCI-P3 inside sRGB)
        int x2 = img.getWidth() - 5;
        int y2 = img.getHeight() - 5;
        int pixel2 = img.getRGB(x2, y2);
        Color color2 = new Color(pixel2);
        res.put("px2-red-srgb", color2.getRed());
        logger.fine("pixel2 sRGB: " + color2);
        int[] pixel2r = img.getRaster().getPixel(x2, y2, new int[4]);
        res.put("px2-red-raw", pixel2r[0]);
        logger.fine("pixel2 raw: " + Arrays.toString(pixel2r));

        return res;
    }

    private static void checkGamut(Map<String, Integer> res) {
        if (res.get("px1-red-raw").equals(res.get("px2-red-raw"))) {
            // raw values are the same
            logger.info("RESULT: color gamut is saturated sRGB (srgb-raw == dci-p3-raw)");
        } else if (!res.get("px1-red-srgb").equals(res.get("px2-red-srgb"))) {
            // raw values are different and srgb values are different
            logger.info(
                    "RESULT: input color profile was ignored or image gamut was translated! (srgb-raw != dci-p3-raw && srgb(srgb) != srgb(dci-p3))");
        } else {
            // raw values are different but srgb values are the same
            logger.info(
                    "RESULT: input color profile was preserved. (srgb-raw != dci-p3-raw && srgb(srgb) == srgb(dci-p3))");
        }
    }

    private static void checkColorspace(BufferedImage img) {
        ColorModel cm = img.getColorModel();
        ColorSpace cs = cm.getColorSpace();
        logger.info("Image colorspace: " + cs + " is sRGB=" + cs.isCS_sRGB());
    }

    public static void main(String[] args) throws Exception {
        /*
         * JPEG
         */
        logger.info("Loading JPEG");
        BufferedImage img = loadImage("image/jpeg", "data/dcip3-srgb-test-j8.jpg", false);
        checkColorspace(img);
        Map<String, Integer> res = getPixels(img);
        checkGamut(res);

        /*
         * PNG 8-bit depth
         */
        logger.info("Loading PNG 8-bit");
        img = loadImage("image/png", "data/dcip3-srgb-test-p8.png", false);
        checkColorspace(img);
        res = getPixels(img);
        checkGamut(res);

        /*
         * TIFF 8-bit depth
         */
        logger.info("Loading TIFF 8-bit");
        img = loadImage("image/tiff", "data/dcip3-srgb-test-t8.tiff", false);
        checkColorspace(img);
        res = getPixels(img);
        checkGamut(res);

        /*
         * TIFF 8-bit depth
         */
        logger.info("Loading TIFF 8-bit sRGB");
        img = loadImage("image/tiff", "data/dcip3-srgb-test-t8.tiff", true);
        checkColorspace(img);
        res = getPixels(img);
        checkGamut(res);

        /*
         * PNG 16-bit depth
         */
        logger.info("Loading PNG 16-bit");
        img = loadImage("image/png", "data/dcip3-srgb-test-p16.png", false);
        checkColorspace(img);
        res = getPixels(img);
        checkGamut(res);

        /*
         * TIFF 16-bit depth
         */
        logger.info("Loading TIFF 16-bit");
        img = loadImage("image/tiff", "data/dcip3-srgb-test-t16.tiff", false);
        checkColorspace(img);
        res = getPixels(img);
        checkGamut(res);

        /*
         * TIFF 16-bit depth
         */
        logger.info("Loading TIFF 16-bit sRGB");
        img = loadImage("image/tiff", "data/dcip3-srgb-test-t16.tiff", true);
        checkColorspace(img);
        res = getPixels(img);
        checkGamut(res);

    }

}