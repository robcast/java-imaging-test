import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;

public class TestProfileLoading {
	
	static Logger logger = Logger.getLogger(TestProfileLoading.class.getName());

	private static void checkPixels(BufferedImage img) {
		// check pixel values
		int x1 = 5;
		int y1 = 5;
		int pixel1 = img.getRGB(x1, y1);
		logger.info("pixel1 sRGB: " + new Color(pixel1));
		int[] pixel1r = img.getRaster().getPixel(x1, y1, new int[4]);
		logger.info("pixel1 raw: " + pixel1r);
		int x2 = img.getWidth() - 5;
		int y2 = img.getHeight() - 5;
		int pixel2 = img.getRGB(x2, y2);
		logger.info("pixel2 sRGB: " + new Color(pixel2));
		int[] pixel2r = img.getRaster().getPixel(x2, y2, new int[4]);
		logger.info("pixel2 raw: " + pixel2r);
	}
	
	public static void main() throws Exception {
		String mt = "image/png";
		// let ImageIO choose Reader
		ImageReader reader = null;
		Iterator<ImageReader> readers = ImageIO.getImageReadersByMIMEType(mt);
		if (readers.hasNext()) {
			reader = readers.next();
		} else {
			throw new Exception("Can't find Reader to load File with mime-type " + mt + "!");
		}
        ImageReadParam readParam = null;
		BufferedImage img = reader.read(0, readParam );
		checkPixels(img);
	}
}