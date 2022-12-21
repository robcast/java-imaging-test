# java-imaging-test

Test cases for problems with Java imaging APIs.

Run e.g. the `TestProfileLoading` tests as

```
mvn exec:java -Dexec.mainClass=TestProfileLoading
```

or with shorter logging messages

```
mvn exec:java -Dexec.mainClass=TestProfileLoading -Djava.util.logging.SimpleFormatter.format='%5$s%6$s%n'
```

or with a more verbose log level

```
mvn exec:java -Dexec.mainClass=TestProfileLoading -Djava.util.logging.SimpleFormatter.format='%5$s%6$s%n' -Djava.util.logging.ConsoleHandler.level=FINE
```

## Test images

The test images in the `data` directory are the same image in different formats. All images have a DCI-P3 color profile. The images are two hues of red divided roughly by the diagonal from lower left to upper right. 

The upper left part is the maximum red color possible in the DCI-P3 colorspace (RGB=255,0,0 in DCI-P3) while the lower right part is the maximum red color possible in the sRGB colorspace (RGB=241,0,0 in DCI-P3). 

When you run the test code with verbose logging it shows the raw RGB value and java.awt.Color value (translated in sRGB) for a DCI-P3-red pixel and a sRGB-red pixel.

The images were inspired by the Webkit project blog post and test images on color gamut https://webkit.org/blog-files/color-gamut/
