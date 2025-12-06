package com.ithra.library.service;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VideoProcessingService {

    @Value("${app.frame.extraction.interval:30}")
    private int frameInterval;

    public List<VideoFrame> extractFrames(String videoPath) {
        List<VideoFrame> frames = new ArrayList<>();

        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath);
            grabber.start();

            double frameRate = grabber.getFrameRate();
            int totalFrames = grabber.getLengthInFrames();

            log.info("Processing video: {} frames at {} fps", totalFrames, frameRate);

            Java2DFrameConverter converter = new Java2DFrameConverter();
            int frameCount = 0;

            Frame frame;
            while ((frame = grabber.grabImage()) != null) {
                if (frameCount % frameInterval == 0) {
                    BufferedImage bufferedImage = converter.convert(frame);

                    if (bufferedImage != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "jpg", baos);
                        byte[] imageBytes = baos.toByteArray();

                        double timestamp = frameCount / frameRate;

                        VideoFrame videoFrame = new VideoFrame();
                        videoFrame.setFrameNumber(frameCount);
                        videoFrame.setTimestamp(timestamp);
                        videoFrame.setImageBytes(imageBytes);

                        frames.add(videoFrame);

                        log.info("Extracted frame {} at timestamp {}", frameCount, timestamp);
                    }
                }
                frameCount++;
            }

            grabber.stop();
            grabber.release();

            log.info("Extracted {} frames from video", frames.size());

        } catch (Exception e) {
            log.error("Error extracting frames from video", e);
        }

        return frames;
    }

    public static class VideoFrame {
        private int frameNumber;
        private double timestamp;
        private byte[] imageBytes;

        public int getFrameNumber() { return frameNumber; }
        public void setFrameNumber(int frameNumber) { this.frameNumber = frameNumber; }

        public double getTimestamp() { return timestamp; }
        public void setTimestamp(double timestamp) { this.timestamp = timestamp; }

        public byte[] getImageBytes() { return imageBytes; }
        public void setImageBytes(byte[] imageBytes) { this.imageBytes = imageBytes; }
    }
}