import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
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
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.FileImageInputStream;

public class TestColorConvertOp {
	
	static Logger logger = Logger.getLogger(TestColorConvertOp.class.getName());

	static {
		// set log level from property
		String lvlVal = System.getProperty("java.util.logging.ConsoleHandler.level");
		if (lvlVal != null) {
			Level level = Level.parse(lvlVal);
		    for(Handler h : Logger.getLogger("").getHandlers())    
		        h.setLevel(level);
		    logger.setLevel(level);
		}
	}
	
	private static BufferedImage loadImage(String mt, String fn, boolean destSrgb, int destIdx) throws Exception, FileNotFoundException, IOException {
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
        int idx = 1;
		for (Iterator<ImageTypeSpecifier> i = reader.getImageTypes(0); i.hasNext(); ++idx) {
			ImageTypeSpecifier type = (ImageTypeSpecifier) i.next();
			ColorModel cm = type.getColorModel();
			ColorSpace cs = cm.getColorSpace();
			logger.fine("  possible destination color model " + idx + ": " + cm + " color space: " + cs + " is sRGB=" + cs.isCS_sRGB());
			if (destIdx == 0 && destSrgb && cs.isCS_sRGB()) {
				logger.fine("    selected as destination");
				readParam.setDestinationType(type);
			}
			if (destIdx > 0 && idx == destIdx) {
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

	private static BufferedImage changeProfile(BufferedImage img, ICC_Profile profile) {
		ColorModel inCM = img.getColorModel();
		ColorSpace inCS = inCM.getColorSpace();
		ColorSpace outCS = new ICC_ColorSpace(profile);
		//ColorModel outCM = new ComponentColorModel(outCS, new int[] { 8, 8, 8, 8 }, inCM.hasAlpha(), inCM.isAlphaPremultiplied(), ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
		ColorModel outCM = new ComponentColorModel(outCS, true, false, ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
		ColorConvertOp inToOut = new ColorConvertOp(inCS, outCS, null);
		WritableRaster outRaster = outCM.createCompatibleWritableRaster(img.getWidth(), img.getHeight());
		inToOut.filter(img.getRaster(), outRaster);
		BufferedImage image = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		image.setData(outRaster);
		return image;
	}
	
	private static BufferedImage convertToProfile(BufferedImage img, ICC_Profile profile) {
		ColorConvertOp cco = new ColorConvertOp(new ICC_Profile[] {profile}, null);
		BufferedImage bi = cco.filter(img, null);
		return bi;
	}
	
	private static BufferedImage convertToSrgb(BufferedImage img) {
		ColorConvertOp cco = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null);
		BufferedImage bi = cco.filter(img, null);
		return bi;
	}
	
	private static BufferedImage convertToNonAlpha(BufferedImage img) {
		BufferedImage bi = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		ColorConvertOp cco = new ColorConvertOp(null);
		BufferedImage bo = cco.filter(img, bi);
		return bo;
	}
	
	private static ICC_Profile getProfile(BufferedImage img) {
		ICC_Profile prof = null;
		ColorModel cm = img.getColorModel();
		ColorSpace cs = cm.getColorSpace();
		if (cs instanceof ICC_ColorSpace) {
			prof = ((ICC_ColorSpace) cs).getProfile();
		}
		return prof;
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
		} else if (! res.get("px1-red-srgb").equals(res.get("px2-red-srgb"))) {
			// raw values are different and srgb values are different
			logger.info("RESULT: color gamut was translated! (srgb-raw != dci-p3-raw && srgb(srgb) != srgb(dci-p3))");
		} else {
			// raw values are different but srgb values are the same
			logger.info("RESULT: color profile was preserved. (srgb-raw != dci-p3-raw && srgb(srgb) == srgb(dci-p3))");		
		}
	}

	private static void checkColorspace(BufferedImage img) {
		ColorModel cm = img.getColorModel();
		ColorSpace cs = cm.getColorSpace();
		logger.info("Image colorspace: " + cs + " is sRGB=" + cs.isCS_sRGB() + " hasAlpha=" + cm.hasAlpha());
	}

	public static void main(String[] args) throws Exception {
		BufferedImage img = null;
		Map<String, Integer> res = null;
		
		/*
		 * PNG 8-bit depth
		 */
		logger.info("Loading PNG 8-bit =================================");
		img = loadImage("image/png", "data/dcip3-srgb-test-p8.png", false, 0);
		checkColorspace(img);
		res = getPixels(img);
		//checkGamut(res);
		logger.info("Converting to non-alpha");
		img = convertToNonAlpha(img);
		checkColorspace(img);
		res = getPixels(img);
		checkGamut(res);
		
		/*
		 * TIFF 8-bit depth
		 */
		logger.info("Loading TIFF 8-bit =================================");
		img = loadImage("image/tiff", "data/dcip3-srgb-test-t8.tiff", false, 0);
		checkColorspace(img);
		res = getPixels(img);
		logger.info("Converting to sRGB");
		img = convertToSrgb(img);
		checkColorspace(img);
		res = getPixels(img);
		checkGamut(res);

		/*
		 * TIFF 8-bit depth
		 */
		logger.info("Loading TIFF 8-bit =================================");
		img = loadImage("image/tiff", "data/dcip3-srgb-test-t8.tiff", false, 0);
		ICC_Profile profile = getProfile(img);
		checkColorspace(img);
		res = getPixels(img);
		logger.info("Converting to non-alpha");
		img = convertToNonAlpha(img);
		checkColorspace(img);
		res = getPixels(img);
		checkGamut(res);

		/*
		 * PNG 8-bit depth
		 */
		logger.info("Loading PNG 8-bit =================================");
		img = loadImage("image/png", "data/dcip3-srgb-test-p8.png", false, 0);
		checkColorspace(img);
		res = getPixels(img);
		logger.info("Converting to profile");
		img = changeProfile(img, profile);
		checkColorspace(img);
		res = getPixels(img);
		checkGamut(res);

		/*
		 * PNG 16-bit depth
		 */
		logger.info("Loading PNG 16-bit =================================");
		img = loadImage("image/png", "data/dcip3-srgb-test-p16.png", false, 0);
		checkColorspace(img);
		res = getPixels(img);
		checkGamut(res);
		
		/*
		 * TIFF 16-bit depth
		 */
		logger.info("Loading TIFF 16-bit =================================");
		img = loadImage("image/tiff", "data/dcip3-srgb-test-t16.tiff", false, 0);
		checkColorspace(img);
		res = getPixels(img);
		checkGamut(res);

		/*
		 * TIFF 16-bit depth
		 */
		logger.info("Loading TIFF 16-bit sRGB =================================");
		img = loadImage("image/tiff", "data/dcip3-srgb-test-t16.tiff", true, 0);
		checkColorspace(img);
		res = getPixels(img);
		checkGamut(res);

	}

}