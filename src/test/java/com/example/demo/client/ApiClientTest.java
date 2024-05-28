package com.example.demo.client;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ApiClientTest {
    private static final Logger log = LoggerFactory.getLogger(ApiClientTest.class);


    @Test
    void test() {
        ApiClient apiClient = new ApiClient();
        IntStream.range(0, 800)
                .forEach(i -> {
                    File image = apiClient.getImage();
                    log.info("saved - i: {} path: {}", i, image);
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    void testResize() throws IOException {
        File[] files = Path.of("target/test-classes/input/").toFile().listFiles();
        for(File f : List.of(files)){
            resize(f.getAbsolutePath(), f.getAbsolutePath() + "_resized.png", 200, 200);
        }


    }

    public void resize(String inputImagePath,
                       String outputImagePath, int scaledWidth, int scaledHeight)
            throws IOException {
        // reads input image
        File inputFile = new File(inputImagePath);
        BufferedImage inputImage = ImageIO.read(inputFile);

        // creates output image
        BufferedImage outputImage = new BufferedImage(scaledWidth,
                scaledHeight, inputImage.getType());

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // extracts extension of output file
        String formatName = outputImagePath.substring(outputImagePath
                .lastIndexOf(".") + 1);

        // writes to output file
        ImageIO.write(outputImage, formatName, new File(outputImagePath));
    }

}
