import java.awt.Color;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBuffer;
import java.awt.image.Kernel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
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
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

public class Test16BitColor {

	static Logger logger = Logger.getLogger(Test16BitColor.class.getName());

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

	private static BufferedImage loadImage(String mt, String fn)
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
		// read image
		logger.fine("Loading file: " + fn);
		logger.fine("Using reader: " + reader);
		BufferedImage img = reader.read(0);
		return img;
	}

    private static void writeImage(BufferedImage img, String format, String filename) {
        try {
            File file = new File(filename);
            logger.info("Writing image in format " + format + " as " + filename);
            ImageIO.write(img, format, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

	private static BufferedImage changeProfile(BufferedImage inBi, ICC_Profile profile) {
        // method suggested by Harald K in https://stackoverflow.com/a/74873159/4912 
		ColorModel inCM = inBi.getColorModel();
		boolean hasAlpha = inCM.hasAlpha();
		boolean isAlphaPre = inCM.isAlphaPremultiplied();
		int[] bits = inCM.getComponentSize();
		int transferType = inCM.getTransferType();
		ColorSpace outCS = new ICC_ColorSpace(profile);
		ColorModel cm = new ComponentColorModel(outCS, bits, hasAlpha, isAlphaPre, Transparency.OPAQUE,
				transferType);
		BufferedImage outBi = new BufferedImage(cm, inBi.getRaster(), isAlphaPre, null);
		return outBi;
	}

	private static BufferedImage convertToSrgb8Bit(BufferedImage img) {
		ColorConvertOp cco = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null);
		BufferedImage bi = cco.filter(img, null);
		return bi;
	}

	private static void changeRasterToSrgb(BufferedImage img, ICC_Profile realProfile) {
	    // method suggested by Harald K in https://stackoverflow.com/a/74873159/4912 
		ICC_Profile srgbProf = ICC_Profile.getInstance(ColorSpace.CS_sRGB);
		ColorConvertOp cco = new ColorConvertOp(new ICC_Profile[] {realProfile, srgbProf}, null);
		WritableRaster colorRaster;
		if (img.getColorModel().hasAlpha()) {
			// use subraster with only color components 
			colorRaster = img.getRaster().createWritableChild(0, 0, img.getWidth(), img.getHeight(), 0, 0, new int[] {0, 1, 2});
		} else {
			colorRaster = img.getRaster();
		}
		cco.filter(colorRaster, colorRaster);
	}

	private static BufferedImage convertToNonAlpha(BufferedImage img) {
		BufferedImage bi = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
		ColorConvertOp cco = new ColorConvertOp(null);
		BufferedImage bo = cco.filter(img, bi);
		return bo;
	}

    private static BufferedImage changeTo8BitDepth1(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean hasAlpha = cm.hasAlpha();
        boolean isAlphaPre = cm.isAlphaPremultiplied();
        int transferType = DataBuffer.TYPE_BYTE;
        int transparency = cm.getTransparency();
        ColorSpace cs = cm.getColorSpace();
        ColorModel newCm = new ComponentColorModel(cs, hasAlpha, isAlphaPre, transparency, transferType);
        WritableRaster newRaster = newCm.createCompatibleWritableRaster(bi.getWidth(), bi.getHeight());
        BufferedImage newBi = new BufferedImage(newCm, newRaster, isAlphaPre, null);
        // convert using setData
        newBi.setData(bi.getRaster());
        return newBi;
    }
    
    private static BufferedImage changeTo8BitDepth2(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean hasAlpha = cm.hasAlpha();
        boolean isAlphaPre = cm.isAlphaPremultiplied();
        int transferType = DataBuffer.TYPE_BYTE;
        int transparency = cm.getTransparency();
        ColorSpace cs = cm.getColorSpace();
        ColorModel newCm = new ComponentColorModel(cs, hasAlpha, isAlphaPre, transparency, transferType);
        WritableRaster newRaster = newCm.createCompatibleWritableRaster(bi.getWidth(), bi.getHeight());
        BufferedImage newBi = new BufferedImage(newCm, newRaster, isAlphaPre, null);
        // convert using drawImage
        newBi.createGraphics().drawImage(bi, null, 0, 0);
        return newBi;
    }

    private static BufferedImage changeTo8BitDepth(BufferedImage original) {
        // method suggested by Harald K in https://stackoverflow.com/a/74995441/4912
        
        ColorModel cm = original.getColorModel();

        // Create 8 bit color model
        ColorModel newCM = new ComponentColorModel(cm.getColorSpace(), cm.hasAlpha(), cm.isAlphaPremultiplied(),
                cm.getTransparency(), DataBuffer.TYPE_BYTE);
        WritableRaster newRaster = newCM.createCompatibleWritableRaster(original.getWidth(), original.getHeight());
        BufferedImage newImage = new BufferedImage(newCM, newRaster, newCM.isAlphaPremultiplied(), null);

        // convert using setData
        // newImage.setData(as8BitRaster(original.getRaster())); // Works
        newRaster.setDataElements(0, 0, as8BitRaster(original.getRaster())); // Faster, requires less conversion

        return newImage;
    }

    private static Raster as8BitRaster(WritableRaster raster) {
        // Assumption: Raster is TYPE_USHORT (16 bit) and has
        // PixelInterleavedSampleModel
        PixelInterleavedSampleModel sampleModel = (PixelInterleavedSampleModel) raster.getSampleModel();

        // We'll create a custom data buffer, that delegates to the original 16 bit
        // buffer
        final DataBuffer buffer = raster.getDataBuffer();

        return Raster.createInterleavedRaster(new DataBuffer(DataBuffer.TYPE_BYTE, buffer.getSize()) {
            @Override
            public int getElem(int bank, int i) {
                return buffer.getElem(bank, i) >>> 8; // We only need the upper 8 bits of the 16 bit sample
            }

            @Override
            public void setElem(int bank, int i, int val) {
                throw new UnsupportedOperationException("Raster is read only!");
            }
        }, raster.getWidth(), raster.getHeight(), sampleModel.getScanlineStride(), sampleModel.getPixelStride(),
                sampleModel.getBandOffsets(), new Point());
    }
    
    private static BufferedImage scale(BufferedImage img, float scaleX, float scaleY, boolean interpol) {
        RenderingHints renderHint = new RenderingHints(null);
        if (interpol) {
            renderHint.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        }
        //renderHint.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        AffineTransformOp scaleOp = new AffineTransformOp(
                AffineTransform.getScaleInstance(scaleX, scaleY), renderHint);
        img = scaleOp.filter(img, null);
        return img;
    }

    private static BufferedImage convolve(BufferedImage img) {
        Kernel kernel = new Kernel(2, 2, new float[] { 0.25f, 0.25f, 0.25f, 0.25f });
        //RenderingHints renderHint = new RenderingHints(RenderingHints.KEY_DITHERING,
        //        RenderingHints.VALUE_DITHER_DISABLE);
        ConvolveOp blurOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        img = blurOp.filter(img, null);
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
         * DCI-P3 TIFF 16-bit depth to 8-bit TIFF with profile 
         */
        logger.info("Loading dci-p3 TIFF 16-bit =================================");
        img = loadImage("image/tiff", "data/dcip3-srgb-test-t16.tiff");
        checkColorspace(img);
        res = getPixels(img);
        checkGamut(res);
        writeImage(img, "TIFF", "output-from-dcip3-tiff16.tiff");
        logger.info("Converting to 8 bit");
        img = changeTo8BitDepth(img);
        checkColorspace(img);
        res = getPixels(img);
        checkGamut(res);
        writeImage(img, "TIFF", "output-from-dcip3-tiff16-8bit.tiff");

        /*
         * DCI-P3 TIFF 16-bit depth to sRGB to 8-bit TIFF
         */
        logger.info("Loading dci-p3 TIFF 16-bit =================================");
        img = loadImage("image/tiff", "data/dcip3-srgb-test-t16.tiff");
        checkColorspace(img);
        res = getPixels(img);
        logger.info("Converting to sRGB 8bit");
        img = convertToSrgb8Bit(img);
        checkColorspace(img);
        res = getPixels(img);
        checkGamut(res);
        writeImage(img, "TIFF", "output-from-dcip3-tiff16-srgb8.tiff");

        /*
         * DCI-P3 TIFF 16-bit depth to scaled TIFF with profile 
         */
        logger.info("Loading dci-p3 TIFF 16-bit =================================");
        img = loadImage("image/tiff", "data/dcip3-srgb-test-t16.tiff");
        checkColorspace(img);
        res = getPixels(img);
        checkGamut(res);
        logger.info("Scaling by 2x without interpolation");
        BufferedImage img2 = scale(img, 2f, 2f, false);
        checkColorspace(img2);
        res = getPixels(img2);
        checkGamut(res);
        writeImage(img2, "TIFF", "output-from-dcip3-tiff16-scale2x.tiff");
        logger.info("Scaling by 2x with interpolation");
        img2 = scale(img, 2f, 2f, true);
        checkColorspace(img2);
        res = getPixels(img2);
        checkGamut(res);
        writeImage(img2, "TIFF", "output-from-dcip3-tiff16-scale2x-interp.tiff");

        /*
         * PhotoRGB TIFF 16-bit depth to 8-bit TIFF with profile 
         */
        logger.info("Loading dci-p3 TIFF 16-bit =================================");
        img = loadImage("image/tiff", "data/photorgb-sample-t16.tiff");
        checkColorspace(img);
        res = getPixels(img);
        //checkGamut(res);
        writeImage(img, "TIFF", "output-from-photorgb-tiff16.tiff");
        logger.info("Changing to 8 bit");
        img2 = changeTo8BitDepth(img);
        checkColorspace(img2);
        res = getPixels(img2);
        //checkGamut(res);
        writeImage(img2, "TIFF", "output-from-photorgb-tiff16-8bit.tiff");
        logger.info("Converting to sRGB 8 bit");
        img2 = convertToSrgb8Bit(img);
        checkColorspace(img2);
        res = getPixels(img2);
        //checkGamut(res);
        writeImage(img2, "TIFF", "output-from-photorgb-tiff16-srgb8.tiff");

        /*
         * PhotoRGB TIFF 16-bit depth to scaled TIFF with profile 
         */
        logger.info("Loading photorgb TIFF 16-bit =================================");
        img = loadImage("image/tiff", "data/photorgb-sample-t16.tiff");
        checkColorspace(img);
        res = getPixels(img);
        //checkGamut(res);
        logger.info("Scaling by 2x without interpolation");
        img2 = scale(img, 2f, 2f, false);
        checkColorspace(img2);
        res = getPixels(img2);
        //checkGamut(res);
        writeImage(img2, "TIFF", "output-from-photorgb-tiff16-scale2x.tiff");
        logger.info("Scaling by 2x with interpolation");
        img2 = scale(img, 2f, 2f, true);
        checkColorspace(img2);
        res = getPixels(img2);
        //checkGamut(res);
        writeImage(img2, "TIFF", "output-from-photorgb-tiff16-scale2x-interp.tiff");

        /*
         * PhotoRGB TIFF 16-bit depth to blurred TIFF with profile 
         */
        logger.info("Loading photorgb TIFF 16-bit =================================");
        img = loadImage("image/tiff", "data/photorgb-sample-t16.tiff");
        checkColorspace(img);
        res = getPixels(img);
        //checkGamut(res);
        logger.info("Blurring with convolve");
        img = convolve(img);
        checkColorspace(img);
        res = getPixels(img);
        //checkGamut(res);
        writeImage(img, "TIFF", "output-from-photorgb-tiff16-blur.tiff");

        /*
         * PhotoRGB TIFF 8-bit depth to scaled TIFF with profile 
         */
        logger.info("Loading photorgb TIFF 8-bit =================================");
        img = loadImage("image/tiff", "data/photorgb-sample-t8.tiff");
        checkColorspace(img);
        res = getPixels(img);
        //checkGamut(res);
        writeImage(img, "TIFF", "output-from-photorgb-tiff8.tiff");
        logger.info("Scaling by 2x without interpolation");
        img2 = scale(img, 2f, 2f, false);
        checkColorspace(img2);
        res = getPixels(img2);
        //checkGamut(res);
        writeImage(img2, "TIFF", "output-from-photorgb-tiff8-scale2x.tiff");
        logger.info("Scaling by 2x with interpolation");
        img2 = scale(img, 2f, 2f, true);
        checkColorspace(img2);
        res = getPixels(img2);
        //checkGamut(res);
        writeImage(img2, "TIFF", "output-from-photorgb-tiff8-scale2x-interp.tiff");

        /*
         * PhotoRGB TIFF 8-bit depth to blurred TIFF with profile 
         */
        logger.info("Loading photorgb TIFF 8-bit =================================");
        img = loadImage("image/tiff", "data/photorgb-sample-t8.tiff");
        checkColorspace(img);
        res = getPixels(img);
        //checkGamut(res);
        logger.info("Blurring with convolve");
        img = convolve(img);
        checkColorspace(img);
        res = getPixels(img);
        //checkGamut(res);
        writeImage(img, "TIFF", "output-from-photorgb-tiff8-blur.tiff");

        /*
         * sRGB TIFF 16-bit depth to blurred TIFF
         */
        logger.info("Loading srgb TIFF 16-bit =================================");
        img = loadImage("image/tiff", "data/srgb-sample-t16.tiff");
        checkColorspace(img);
        res = getPixels(img);
        //checkGamut(res);
        writeImage(img, "TIFF", "output-from-srgb-tiff16.tiff");
        logger.info("Blurring with convolve");
        img = convolve(img);
        checkColorspace(img);
        res = getPixels(img);
        //checkGamut(res);
        writeImage(img, "TIFF", "output-from-srgb-tiff16-blur.tiff");

        /*
         * sRGB TIFF 16-bit depth to scaled TIFF with profile 
         */
        logger.info("Loading srgb TIFF 16-bit =================================");
        img = loadImage("image/tiff", "data/srgb-sample-t16.tiff");
        checkColorspace(img);
        res = getPixels(img);
        //checkGamut(res);
        logger.info("Scaling by 2x without interpolation");
        img2 = scale(img, 2f, 2f, false);
        checkColorspace(img2);
        res = getPixels(img2);
        //checkGamut(res);
        writeImage(img2, "TIFF", "output-from-srgb-tiff16-scale2x.tiff");
        logger.info("Scaling by 2x with interpolation");
        img2 = scale(img, 2f, 2f, true);
        checkColorspace(img2);
        res = getPixels(img2);
        //checkGamut(res);
        writeImage(img2, "TIFF", "output-from-srgb-tiff16-scale2x-interp.tiff");

        logger.info("Java version: " + System.getProperty("java.version"));
	}

}