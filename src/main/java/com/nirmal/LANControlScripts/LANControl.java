package com.nirmal.LANControlScripts;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LANControl {
    private static final String DEVICE_IP = "{YOUR_DEVICE_IP}";
    private static final int THRESHOLD = 25; // MODIFIABLE: THRESHOLD SET TO 25 BY DEFAULT!

    private static volatile int[] prevColor = {0, 0, 0};
    private static volatile int[] newColor = {0, 0, 0};
    private static volatile boolean colorChanged = false;

    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2); // 1 thread for checking color and 1 for sending requests

    private DatagramSocket socket;
    private InetAddress deviceAddress;
    private int port;

    // Initializes the socket and IP address
    public LANControl(String deviceIp, int port) throws Exception {
        this.deviceAddress = InetAddress.getByName(deviceIp);
        this.port = port;
        this.socket = new DatagramSocket();
    }

    public void changeColor(int r, int g, int b) throws Exception {
        String colorCommand = "{ \"msg\":{ \"cmd\":\"colorwc\", \"data\":{ \"color\":{ \"r\":" + r + ", \"g\":" + g + ", \"b\":" + b + " }, \"colorTemInKelvin\":" + 0 + " } } }";
        byte[] commandBytes = colorCommand.getBytes();

        DatagramPacket packet = new DatagramPacket(commandBytes, commandBytes.length, deviceAddress, port);
        socket.send(packet);

        // System.out.println("Color changed to " + r + " " + g + " " + b); // Optional: Print the color change every time
    }

    private static int[] averageColor() throws AWTException {
        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage screenShot = robot.createScreenCapture(screenRect);

        long sumRed = 0, sumGreen = 0, sumBlue = 0;
        int width = screenShot.getWidth();
        int height = screenShot.getHeight();
        int numPixels = (width * height)/4;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x+=4) {
                int rgb = screenShot.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                sumRed += red;
                sumGreen += green;
                sumBlue += blue;
            }
        }

        int avgRed = (int) (sumRed / numPixels);
        int avgGreen = (int) (sumGreen / numPixels);
        int avgBlue = (int) (sumBlue / numPixels);

        return new int[]{avgRed, avgGreen, avgBlue};
    }

    private static boolean isThresholdSurpassed() {
        return Math.abs(newColor[0] - prevColor[0]) > THRESHOLD ||
               Math.abs(newColor[1] - prevColor[1]) > THRESHOLD ||
               Math.abs(newColor[2] - prevColor[2]) > THRESHOLD;
    }

    public static void main(String[] args) throws Exception {

        LANControl controller = new LANControl(DEVICE_IP, 4003);

        // Thread 1: Color calculation, running every 50ms by default
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int[] calculatedColor = averageColor();  // Compute the new color

                // Update shared newColor array
                synchronized (newColor) {
                    newColor[0] = (int) ((1 - 0.1) * calculatedColor[0] + 0.1 * prevColor[0]);
                    newColor[1] = (int) ((1 - 0.1) * calculatedColor[1] + 0.1 * prevColor[1]);
                    newColor[2] = (int) ((1 - 0.1) * calculatedColor[2] + 0.1 * prevColor[2]);
                    colorChanged = true;  // Indicate that the color has been updated
                }

                
            } catch (AWTException ex) {
                ex.printStackTrace();
            }
        }, 0, 50, TimeUnit.MILLISECONDS); // MODIFIABLE: INCREASE OR DECREASE TIME BASED ON COMPUTER PERFORMANCE (Recommend keeping thread 2's time lower than thread 1)

        // Thread 2: Threshold checker, running every 25ms by default
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (newColor) {
                // Check if the threshold is surpassed and color changed
                if (colorChanged && isThresholdSurpassed()) {
                    try {
                        
                        controller.changeColor(newColor[0], newColor[1], newColor[2]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Update previous color to new color
                    prevColor[0] = newColor[0];
                    prevColor[1] = newColor[1];
                    prevColor[2] = newColor[2];

                    // Reset colorChanged flag
                    colorChanged = false;
                }
            }
        }, 0, 25, TimeUnit.MILLISECONDS); // MODIFIABLE: INCREASE OR DECREASE TIME BASED ON COMPUTER PERFORMANCE (Recommend keeping thread 2's time lower than thread 1)
    }
}