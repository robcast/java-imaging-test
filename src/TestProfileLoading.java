import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

public class TestProfileLoading {
	
	static Logger logger = Logger.getLogger(TestProfileLoading.class.getName());

	private static void checkPixels(BufferedImage img) {
		ColorModel cm = img.getColorModel();
		ColorSpace cs = cm.getColorSpace();
		logger.info("Colorspace: " + cs + " is sRGB=" + cs.isCS_sRGB());
		// check pixel values
		int x1 = 5;
		int y1 = 5;
		int pixel1 = img.getRGB(x1, y1);
		logger.info("pixel1 sRGB: " + new Color(pixel1));
		int[] pixel1r = img.getRaster().getPixel(x1, y1, new int[4]);
		logger.info("pixel1 raw: " + Arrays.toString(pixel1r));
		int x2 = img.getWidth() - 5;
		int y2 = img.getHeight() - 5;
		int pixel2 = img.getRGB(x2, y2);
		logger.info("pixel2 sRGB: " + new Color(pixel2));
		int[] pixel2r = img.getRaster().getPixel(x2, y2, new int[4]);
		logger.info("pixel2 raw: " + Arrays.toString(pixel2r));
	}
	
	private static BufferedImage loadImage(String mt, String fn) throws Exception, FileNotFoundException, IOException {
		// let ImageIO choose Reader type
		ImageReader reader = null;
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
		logger.info("Loading file: " + fn);
		logger.info("Using reader: " + reader);
		BufferedImage img = reader.read(0);
		return img;
	}

	public static void main(String[] args) throws Exception {
		/*
		 * JPEG
		 */
		BufferedImage img = loadImage("image/jpeg", "data/dcip3-srgb-test-j8.jpg");
		checkPixels(img);

		/*
		 * PNG 8-bit depth
		 */
		img = loadImage("image/png", "data/dcip3-srgb-test-p8.png");
		checkPixels(img);
	}

}